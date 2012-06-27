/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.debug;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.RequestCycle;
import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.eclipse.dltk.rhino.dbgp.DBGPDebugger.ITerminationListener;
import org.eclipse.dltk.rhino.dbgp.DBGPStackManager;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.IWebClientApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.scripting.LazyCompilationScope;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ExtendableURLClassLoader;

/**
 * @author jcompagner
 * 
 */
public class RemoteDebugScriptEngine extends ScriptEngine implements ITerminationListener
{
	private static Socket socket;
	private static volatile DBGPDebugger debugger;
	private static volatile ServerSocket ss;
	private static final ConcurrentHashMap<IApplication, List<Context>> contexts = new ConcurrentHashMap<IApplication, List<Context>>();

	private static final ContextFactory.Listener contextListener = new ContextFactory.Listener()
	{
		private final ThreadLocal<DBGPStackManager> manager = new ThreadLocal<DBGPStackManager>();

		public void contextReleased(Context cx)
		{
			IServiceProvider sp = J2DBGlobals.getServiceProvider();
			if (sp instanceof IApplication && sp instanceof IDebugClient)
			{
				IApplication application = (IApplication)sp;
				if (manager.get() != null && application.isEventDispatchThread() && !(Thread.currentThread() instanceof ServoyDebugger))
				{
					if (debugger != null) debugger.setStackManager(manager.get());
					manager.remove();
				}
				List<Context> list = contexts.get(application);
				if (list != null) list.remove(cx);
			}
		}

		public void contextCreated(Context cx)
		{
			IServiceProvider sp = J2DBGlobals.getServiceProvider();
			if (sp instanceof IApplication && sp instanceof IDebugClient)
			{
				IApplication application = (IApplication)sp;
				if (debugger != null && debugger.isInited)
				{
					// executing can be done multiply in a thread (calc)
					// only allow the event threads (AWT and web client request thread) to debug.
					boolean isDispatchThread = application.isEventDispatchThread() && !(Thread.currentThread() instanceof ServoyDebugger);
					if (isDispatchThread && application instanceof IWebClientApplication)
					{
						isDispatchThread = RequestCycle.get() != null; // for web client test extra if this is a Request thread.
					}
					if (isDispatchThread)
					{
						cx.setApplicationClassLoader(application.getBeanManager().getClassLoader());
						manager.set(debugger.getStackManager());
						debugger.setContext(cx);
						cx.setDebugger(debugger, null);
						cx.setGeneratingDebug(true);
						cx.setOptimizationLevel(-1);
					}
					else if (!(cx.getApplicationClassLoader() instanceof ExtendableURLClassLoader))
					{
						cx.setApplicationClassLoader(application.getBeanManager().getClassLoader());
					}
				}
				else
				{
					manager.remove();
				}

				// context for this client
				List<Context> list = contexts.get(application);
				if (list == null)
				{
					list = Collections.synchronizedList(new ArrayList<Context>());
					contexts.put(application, list);
				}
				list.add(cx);
			}
		}

	};

	static
	{
		ContextFactory.getGlobal().addListener(contextListener);
	}

	private static final List<IProfileListener> profileListeners = new ArrayList<IProfileListener>();


	public static int startupDebugger()
	{
		if (ss != null) return ss.getLocalPort();
		try
		{
			ss = new ServerSocket(0);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return -1;
		}
		Runnable debugThread = new Runnable()
		{

			public void run()
			{
				while (ss != null)
				{
					try
					{
						socket = ss.accept();
						socket.setKeepAlive(true);
						if (Debug.tracing())
						{
							Debug.trace("Socket " + socket + " Accepted, staring connect thread"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						new Thread(new Runnable()
						{
							public void run()
							{
								try
								{
									connect("globals.js", "remote:" + ss.getLocalPort()); //$NON-NLS-1$ //$NON-NLS-2$
								}
								catch (Exception e)
								{
									Debug.error("Error connectiong to a debug", e); //$NON-NLS-1$
								}
							}
						}, "ScriptDebug Connector").start(); //$NON-NLS-1$
					}
					catch (Exception e)
					{
						Debug.error("Error accepting debug connections", e); //$NON-NLS-1$
						try
						{
							ss.close();
						}
						catch (IOException e1)
						{
						}
						ss = null;
					}
				}
			}

		};
		new Thread(debugThread, "Script Debug accept thread").start(); //$NON-NLS-1$
		return ss.getLocalPort();
	}

	private boolean listenerAdded;
	private final AtomicInteger executingFunction = new AtomicInteger(0);


	/**
	 * @param app
	 */
	public RemoteDebugScriptEngine(IApplication app)
	{
		super(app);
	}

	public static boolean isConnected()
	{
		return isConnected(5);
	}

	public static boolean isConnected(int maxWaits)
	{
		int i = 0;
		while (ss == null && i++ < maxWaits)
		{
			if (Debug.tracing())
			{
				Debug.trace("Waiting for Server socket " + i + " of " + maxWaits + " tries"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try
			{
				Thread.sleep(1000); // wait for a bit until socket is there.
			}
			catch (InterruptedException e)
			{
				// ignore
			}
		}
		if (ss == null) return false;
		i = 0;
		while (debugger == null && i++ < 10 * maxWaits)
		{
			// just wait a few seconds if a debugger has to be created.
			if (Debug.tracing())
			{
				Debug.trace("Waiting for debugger to be created" + i + " of 50 tries"); //$NON-NLS-1$ //$NON-NLS-2$ 
			}
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				// ignore
			}
		}
		int counter = 0;
		while (debugger != null && socket != null && !debugger.isInited && socket.isConnected() && !socket.isClosed() && socket.isBound())
		{
			if (counter++ > 5)
			{
				Debug.trace("Debug Socket still not connected after 5 tries, " + socket); //$NON-NLS-1$
				return false;
			}
			synchronized (debugger)
			{
				try
				{
					debugger.wait(1000);
				}
				catch (InterruptedException e)
				{
					Debug.error(e);
				}
			}
		}
		boolean returnValue = false;
		if (debugger != null && socket != null && socket.isConnected() && !socket.isClosed() && socket.isBound() && debugger.isInited)
		{
			debugger.outputStdOut(""); //$NON-NLS-1$
			returnValue = !socket.isClosed() && socket.isConnected() && socket.isBound();
		}
		if (!returnValue && socket != null)
		{
			debugger = null;
			socket = null;
		}
		return returnValue;
	}

	/**
	 * @see com.servoy.j2db.scripting.ScriptEngine#compileFunction(com.servoy.j2db.persistence.IScriptProvider, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Function compileFunction(IScriptProvider sp, Scriptable scope) throws Exception
	{
		String sourceName = sp.getDataProviderID();
		AbstractBase base = (AbstractBase)sp;
		String filename = base.getSerializableRuntimeProperty(IScriptProvider.FILENAME);
		if (filename != null)
		{
			sourceName = filename;
		}
		Context cx = Context.enter();

		try
		{
			cx.setGeneratingDebug(true);
			cx.setOptimizationLevel(-1);
			return compileScriptProvider(sp, scope, cx, sourceName);
		}
		catch (EcmaError ee)
		{
			application.reportJSError("Compilation failed for method: " + sp.getDataProviderID() + ", " + ee.getMessage(), ee);
			throw ee;
		}
		catch (Exception e)
		{
			Debug.error("Compilation failed for method: " + sp.getDataProviderID());
			throw e;
		}
		finally
		{

			Context.exit();
		}
	}

	public boolean recompileScriptCalculation(ScriptCalculation sc)
	{

		try
		{
			Scriptable tableScope = getExistingTableScrope(sc.getTable());
			if (tableScope instanceof LazyCompilationScope)
			{
				try
				{
					((LazyCompilationScope)tableScope).put(sc, sc);
					return true;
				}
				catch (Exception ex)
				{
					application.reportJSError("compile failed: " + sc.getDataProviderID(), ex);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * @param file
	 * @param sessionId
	 * @return
	 * @throws IOException
	 */
	private static boolean connect(String file, String sessionId) throws IOException
	{
		Context cx = Context.enter();
		try
		{
			debugger = new ServoyDebugger(socket, file, sessionId, cx, profileListeners);
			if (Debug.tracing())
			{
				Debug.trace("Created Servoy Debugger on socket " + socket + ", starting the debugger command thread."); //$NON-NLS-1$ //$NON-NLS-2$
			}

			debugger.start();
			cx.setDebugger(debugger, null);
			cx.setGeneratingDebug(true);
			cx.setOptimizationLevel(-1);
		}
		finally
		{
			Context.exit();
		}
		if (!socket.isConnected())
		{
			socket = null;
			debugger = null;
			return false;
		}
		return true;
	}

	@Override
	public Object executeFunction(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException)
		throws Exception
	{
		if (debugger != null && !listenerAdded)
		{
			listenerAdded = true;
			debugger.addTerminationListener(RemoteDebugScriptEngine.this);
		}
		try
		{
			executingFunction.incrementAndGet();
			return super.executeFunction(f, scope, thisObject, args, focusEvent, throwException);
		}
		finally
		{
			executingFunction.decrementAndGet();
		}
	}

	/**
	 * 
	 */
	public DBGPDebugger getDebugger()
	{
		if (debugger != null && isConnected())
		{
			return debugger;
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.scripting.ScriptEngine#destroy()
	 */
	@Override
	public void destroy()
	{
		if (debugger != null)
		{
			List<Context> list = contexts.remove(application);
			if (list != null && list.size() > 0)
			{
				Context[] array;
				synchronized (list)
				{
					array = list.toArray(new Context[list.size()]);
					list.clear();
				}

				for (Context cx : array)
				{
					DBGPStackManager manager = DBGPStackManager.removeManager(cx);
					if (manager != null)
					{
						manager.stop();
					}
				}
			}

			debugger.removeTerminationListener(this);
		}
		super.destroy();
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.DBGPDebugger.ITerminationListener#debuggerTerminated()
	 */
	public void debuggerTerminated()
	{
		socket = null;
		debugger = null;
		((ClientState)application).invokeLater(new Runnable()
		{
			public void run()
			{
				((ClientState)application).shutDown(true);
			}
		});
	}

	@Override
	public boolean isAWTSuspendedRunningScript()
	{
		// actually this returns true if the AWT thread is suspended in debugger (not any thread)
		if (executingFunction.get() > 0 && debugger != null)
		{
			DBGPStackManager sm = debugger.getStackManager();
			return sm != null && sm.isSuspended();
		}
		return false;
	}

	/**
	 * @param profileListener
	 */
	public static void registerProfileListener(IProfileListener profileListener)
	{
		profileListeners.add(profileListener);
	}

	public static void deregisterProfileListener(IProfileListener profileListener)
	{
		profileListeners.remove(profileListener);
	}
}
