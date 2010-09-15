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
package com.servoy.j2db.plugins;


import java.awt.Component;
import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.JMenu;

import org.mozilla.javascript.Function;

import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDatabaseManager;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FileChooserUtils;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.toolbar.IToolbarPanel;

/**
 * Interface Impl.
 * 
 * @author jblok
 */
public class ClientPluginAccessProvider implements IClientPluginAccess
{
	protected IApplication application;

	public ClientPluginAccessProvider(IApplication app)
	{
		application = app;
	}

	public IDataServer getDataServer()
	{
		return application.getDataServer();
	}

	public void output(Object msg)
	{
		application.output(msg, ILogLevel.INFO);
	}

	public void output(Object msg, int level)
	{
		application.output(msg, level);
	}

	public Map getRuntimeProperties()
	{
		return application.getRuntimeProperties();
	}

	/**
	 * @deprecated use getUserUID
	 */
	@Deprecated
	public Object getUserID()
	{
		return application.getUserUID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IPluginAccess#getUserID()
	 */
	public Object getUserUID()
	{
		return application.getUserUID();
	}

	public String getClientID()
	{
		return application.getClientID();
	}

	public String getTransactionID(String serverName)
	{
		try
		{
			return application.getFoundSetManager().getTransactionID(serverName);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public String getSolutionName()
	{
		if (application.getSolution() != null)
		{
			return application.getSolution().getName();
		}
		return null;
	}

	public IFormManager getFormManager()
	{
		return application.getFormManager();
	}

	public IDatabaseManager getDatabaseManager()
	{
		return application.getFoundSetManager();
	}

	public ICmdManager getCmdManager()
	{
		return application.getCmdManager();
	}

	public IBeanManager getBeanManager()
	{
		return application.getBeanManager();
	}

	public String getApplicationName()
	{
		return application.getApplicationName();
	}

	public Properties getSettings()
	{
		return application.getSettings();
	}

	public JMenu getImportMenu()
	{
		return application.getImportMenu();
	}

	public JMenu getExportMenu()
	{
		return application.getExportMenu();
	}

	/**
	 * @deprecated use getCurrentWindow if possible (gives the real current window)
	 */
	@Deprecated
	public JFrame getMainApplicationFrame()
	{
		return application.getMainApplicationFrame();
	}

	public Window getCurrentWindow()
	{
		Object window = application.getJSWindowManager().getCurrentWindowWrappedObject();
		if ((window instanceof Window) && ((Window)window).isVisible())
		{
			return (Window)window;
		}
		else
		{
			return application.getMainApplicationFrame();
		}
	}

	public Window getWindow(String windowName)
	{
		if (windowName == null) return application.getMainApplicationFrame();

		Object window = application.getJSWindowManager().getCurrentWindowWrappedObject();
		if ((window instanceof Window) && ((Window)window).isVisible())
		{
			return (Window)window;
		}
		else
		{
			return null;
		}
	}

	public IToolbarPanel getToolbarPanel()
	{
		return application.getToolbarPanel();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public ITaskExecuter getThreadPool()
	{
		return application.getThreadPool();
	}

	public ScheduledExecutorService getExecutor()
	{
		return application.getScheduledExecutor();
	}

	public void reportError(Component parentComponent, String msg, Object detail)
	{
		application.reportError(parentComponent, msg, detail);
	}

	public void handleException(String msg, Exception e)
	{
		application.handleException(msg, e);
	}

	public void reportError(String msg, Object detail)
	{
		application.reportError(msg, detail);
	}

	public void reportWarningInStatus(String s)
	{
		application.reportWarningInStatus(s);

	}

	public void blockGUI(String reason)
	{
		application.blockGUI(reason);
	}

	public void releaseGUI()
	{
		application.releaseGUI();
	}

	/**
	 * Register a URLStreamHandler for a protocol
	 * 
	 * @param protocolName
	 * @param handler
	 */
	public void registerURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		application.addURLStreamHandler(protocolName, handler);
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#getMediaURLStreamHandler()
	 */
	public URLStreamHandler getMediaURLStreamHandler()
	{
		return new MediaURLStreamHandler(application);
	}

	/**
	 * Get a server interface by name.
	 * 
	 * @param name of the server
	 * @return IServer
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public IServer getServer(String name) throws RemoteException, RepositoryException
	{
		return application.getSolution().getServer(name);
	}

	/**
	 * Get all the defined server interfaces.
	 * 
	 * @return IServer[] all the servers
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public String[] getServerNames() throws RemoteException, RepositoryException
	{
		return application.getRepository().getServerNames(true);
	}

	/**
	 * @return IApplication
	 */
	public IApplication getApplication()
	{
		return application;
	}

	public URL getServerURL()
	{
		return application.getServerURL();
	}

	public Remote getServerService(String name) throws Exception
	{
		return application.getServerService(name);
	}

	/*
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#setStatusText(java.lang.String)
	 */
	public void setStatusText(String txt)
	{
		application.setStatusText(txt, null);
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#exportObject(java.rmi.Remote)
	 */
	@Deprecated
	public void exportObject(Remote object) throws RemoteException
	{
		if (application instanceof ISmartClientApplication)
		{
			((ISmartClientApplication)application).exportObject(object);
		}

	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public String getMessage(String i18nKey)
	{
		return application.getI18NMessage(i18nKey);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public String getMessage(String i18nKey, Object[] arguments)
	{
		return getI18NMessage(i18nKey, arguments);
	}

	public String getI18NMessage(String i18nKey, Object[] arguments)
	{
		return application.getI18NMessage(i18nKey, arguments);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void executeMethod(final String formname, final String methodname, final Object[] arguments)
	{
		try
		{
			executeMethod(formname, methodname, arguments, true);
		}
		catch (Exception e)
		{
			application.reportError("Error executing global method " + methodname, e); //$NON-NLS-1$
			Debug.error(e);
		}
	}

	public Object executeMethod(String context, String methodname, Object[] arguments, final boolean async) throws Exception
	{
		if (application.getSolution() != null)
		{
			final MethodExecutor method = new MethodExecutor(context, methodname, arguments, async);

			// We first added to a thread. then called invokeLater in it.
			// else you can't have batch processors that add the same (one time run) job at the end of the run. 
			// see case: 61486
			// this doesn't matter to much for session client. Because the current thread is the schedulers thread 
			// and not the request thread.
			// maybe invokeLater should fix this. But for a session client this is not possible if that call
			// happens in the request method.
			synchronized (method)
			{
				// When application != J2DBGlobals.getServiceProvider() the method is called from client a to another (headless) client b.
				// Method execution has to be done in a separate thread to prevent mixing thread locals from client a and b.
				// This happens when the weblient uses the headless client plugin to call a method in the HC in the server-side of the plugin,
				// since this is all server-side code the HC call is executed in the same thread.
				if (application.isEventDispatchThread() && !async && application == J2DBGlobals.getServiceProvider())
				{
					application.invokeAndWait(method);
					Object retval = method.getRetval();
					if (retval instanceof Exception)
					{
						throw (Exception)retval;
					}
					return retval;
				}
				else
				{
					application.getScheduledExecutor().execute(new Runnable()
					{
						public void run()
						{
							if (!async && !application.isEventDispatchThread())
							{
								// if not async and not event dispatch thread 
								// and the debugger is enabled, execute this directly. 
								if (application.getScriptEngine() instanceof IScriptSupport &&
									((IScriptSupport)application.getScriptEngine()).isAlreadyExecutingFunctionInDebug())
								{
									method.run();
									return;
								}
							}

							if (async)
							{
								application.invokeLater(method);
							}
							else
							{
								application.invokeAndWait(method);
							}
						}
					});

					if (!async)
					{
						Object retval;
						try
						{
							method.wait();
							retval = method.getRetval();
						}
						catch (InterruptedException e)
						{
							retval = e;
							Debug.error(e);
						}
						if (retval instanceof Exception)
						{
							throw (Exception)retval;
						}
						return retval;
					}
				}
			}
		}
		return null;
	}


	private class MethodExecutor implements Runnable
	{
		private final String context;
		private final String methodname;
		private final Object[] arguments;
		private Object retval;
		private final boolean async;

		public MethodExecutor(String context, String methodname, Object[] arguments, boolean async)
		{
			this.context = context;
			this.methodname = methodname;
			this.arguments = arguments;
			this.async = async;
		}

		public Object getRetval()
		{
			return retval;
		}

		@SuppressWarnings("nls")
		public void run()
		{
			try
			{
				//solution can be closed in the mean time.
				if (application.getSolution() != null)
				{
					if (context == null)
					{
						GlobalScope gs = application.getScriptEngine().getSolutionScope().getGlobalScope();
						Object function = gs.get(methodname);
						if (function instanceof Function)
						{
							try
							{
								retval = application.getScriptEngine().executeFunction((Function)function, gs, gs, arguments, true, true);
							}
							catch (Exception e)
							{
								retval = e;
								if (async) application.handleException("Exception calling global method '" + methodname + "' with arguments " +
									Arrays.toString(arguments) + " in async mode on solution " + getSolutionName(), e);

							}
						}
						else
						{
							retval = new IllegalArgumentException(
								"global methodname: " + methodname + " didnt resolve to a method in solution " + getSolutionName()); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					else
					{
						try
						{
							FormController fp = ((FormManager)application.getFormManager()).leaseFormPanel(context);
							if (fp != null)
							{
								fp.initForJSUsage();
								fp.setView(fp.getView());
								fp.executeOnLoadMethod();
								retval = fp.executeFunction(methodname, arguments, false, null, true, null, false, true);
							}
						}
						catch (Exception e)
						{
							retval = e;
							if (async) application.handleException("Exception calling form method '" + methodname + "' with arguments " +
								Arrays.toString(arguments) + " on form '" + context + "'in async mode on solution " + getSolutionName(), e);

						}
					}
				}
			}
			finally
			{
				synchronized (this)
				{
					notify();
				}
			}
		}
	}

	public int getApplicationType()
	{
		return application.getApplicationType();
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#isInDeveloper()
	 */
	public boolean isInDeveloper()
	{
		return application.isInDeveloper();
	}

	public int getPlatform()
	{
		return application.getClientPlatform();
	}

	public String getVersion()
	{
		return ClientVersion.getVersion();
	}

	public int getReleaseNumber()
	{
		return ClientVersion.getReleaseNumber();
	}

	public ResourceBundle getResourceBundle(Locale locale)
	{
		return application.getResourceBundle(locale);
	}

	public IPluginManager getPluginManager()
	{
		return application.getPluginManager();
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#showFileOpenDialog(com.servoy.j2db.plugins.IMediaUploadCallback, java.lang.String, boolean, java.lang.String[])
	 */
	public void showFileOpenDialog(IMediaUploadCallback callback, String fileNameHint, boolean multiSelect, String[] filter, int selection, String dialogTitle)
	{
		File file = null;
		if (fileNameHint != null) file = new File(fileNameHint);
		if (multiSelect)
		{
			File[] files = FileChooserUtils.getFiles(getCurrentWindow(), file, selection, filter, dialogTitle);
			if (files != null && files.length > 0)
			{
				IUploadData[] data = new FileUploadData[files.length];
				for (int i = 0; i < files.length; i++)
				{
					data[i] = new FileUploadData(files[i]);
				}
				callback.uploadComplete(data);
			}

		}
		else
		{
			final File f = FileChooserUtils.getAReadFile(getCurrentWindow(), file, selection, filter, dialogTitle);
			if (f != null)
			{
				IUploadData data = new FileUploadData(f);
				callback.uploadComplete(new IUploadData[] { data });
			}
		}
	}

	/**
	 * @author jcompagner
	 *
	 */
	private static final class FileUploadData implements IUploadData
	{
		/**
		 * 
		 */
		private final File f;

		/**
		 * @param f
		 */
		private FileUploadData(File f)
		{
			this.f = f;
		}

		public File getFile()
		{
			return f;
		}

		public String getName()
		{
			return f.getAbsolutePath();
		}

		public String getContentType()
		{
			try
			{
				return ImageLoader.getContentType(FileChooserUtils.readFile(f, 32), f.getName());
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		public byte[] getBytes()
		{
			try
			{
				return FileChooserUtils.readFile(f);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.plugins.IUploadData#getInputStream()
		 */
		public InputStream getInputStream() throws IOException
		{
			return new BufferedInputStream(new FileInputStream(f));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#getStyleSheet(java.lang.String)
	 */
	public IStyleSheet getStyleSheet(String name)
	{
		String style_name = ComponentFactory.getOverriddenStyleName(application, name);
		Style s = application.getFlattenedSolution().getStyle(style_name);
		return ComponentFactory.getCSSStyle(s);
	}
}
