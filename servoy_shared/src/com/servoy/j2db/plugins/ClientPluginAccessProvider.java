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
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.JMenu;

import org.mozilla.javascript.Function;

import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDatabaseManager;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.solutionmodel.ISolutionModel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.ThreadingRemoteInvocationHandler;
import com.servoy.j2db.util.toolbar.IToolbarPanel;

/**
 * Interface Impl.
 *
 * @author jblok
 */
public class ClientPluginAccessProvider implements IClientPluginAccess
{
	private final IApplication application;
	private final boolean useThreadingInvocationHandler;

	public ClientPluginAccessProvider(IApplication app)
	{
		application = app;
		useThreadingInvocationHandler = Boolean.parseBoolean(application.getSettings().getProperty("servoy.plugins.services.threaded", "true")); //$NON-NLS-1$ //$NON-NLS-2$
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

	public Map<Object, Object> getRuntimeProperties()
	{
		return application.getRuntimeProperties();
	}

	/**
	 * @deprecated use getUserUID
	 */
	@Deprecated
	public Object getUserID()
	{
		return getUserUID();
	}

	public Object getUserUID()
	{
		return application.getUserUID();
	}

	public String getClientID()
	{
		return application.getClientID();
	}

	@Deprecated
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

	public ISolutionModel getSolutionModel()
	{
		return application.getScriptEngine().getSolutionModifier();
	}

	public String getApplicationName()
	{
		return application.getApplicationName();
	}

	public Properties getSettings()
	{
		return application.getSettings();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public JMenu getImportMenu()
	{
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public JMenu getExportMenu()
	{
		return null;
	}

	/**
	 * @deprecated use getCurrentWindow if possible (gives the real current window)
	 */
	@Deprecated
	public JFrame getMainApplicationFrame()
	{
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public Window getCurrentWindow()
	{
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public Window getWindow(String windowName)
	{
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public IToolbarPanel getToolbarPanel()
	{
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public ITaskExecuter getThreadPool()
	{
		return (ServoyScheduledExecutor)application.getScheduledExecutor();
	}

	public ScheduledExecutorService getExecutor()
	{
		return application.getScheduledExecutor();
	}

	public void reportError(Component parentComponent, String msg, Object detail)
	{
		application.reportError(msg, detail);
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
	 * @deprecated
	 */
	@Deprecated
	public void registerURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		//nop
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

	@Deprecated
	public Remote getServerService(String name) throws Exception
	{
		return getRemoteService(name);
	}

	public Remote getRemoteService(String name) throws Exception
	{
		Remote remote = application.getServerService(name);
		if (remote == null || !useThreadingInvocationHandler || application.isRunningRemote())
		{
			return remote;
		}
		return ThreadingRemoteInvocationHandler.createThreadingRemoteInvocationHandler(remote);
	}

	/*
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#setStatusText(java.lang.String)
	 */
	public void setStatusText(String txt)
	{
		application.setStatusText(txt, null);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void exportObject(Remote object) throws RemoteException
	{
		//nop
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
	public final void executeMethod(final String formname, final String methodname, final Object[] arguments)
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
		return executeMethod(context, methodname, arguments, async, true);
	}

	public Object executeMethod(String context, String methodname, Object[] arguments, final boolean async, final boolean stopUIEditing) throws Exception
	{
		if (application.isSolutionLoaded())
		{
			final MethodExecutor method = new MethodExecutor(context, methodname, arguments, async, stopUIEditing);

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
				// This happens when the webclient uses the headless client plugin to call a method in the HC in the server-side of the plugin,
				// since this is all server-side code the HC call is executed in the same thread.
				boolean eventThread = false;
				try
				{
					eventThread = application.isEventDispatchThread();
				}
				catch (Exception ex)
				{
					//ignore
				}
				if (eventThread && !async && application == J2DBGlobals.getServiceProvider())
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
									((IScriptSupport)application.getScriptEngine()).isAWTSuspendedRunningScript())
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
		private final boolean stopUIEditing;

		public MethodExecutor(String context, String methodname, Object[] arguments, boolean async, boolean stopUIEditing)
		{
			if (methodname == null)
			{
				throw new IllegalArgumentException("null methodname");
			}
			if (context == null)
			{
				// derive context from name (scopes.myscope.mymethod)
				Pair<String, String> variableScope = ScopesUtils.getVariableScope(methodname);
				this.context = ScriptVariable.SCOPES_DOT_PREFIX + (variableScope.getLeft() == null ? ScriptVariable.GLOBAL_SCOPE : variableScope.getLeft());
				this.methodname = variableScope.getRight();
			}
			else
			{
				this.context = context;
				this.methodname = methodname;
			}
			this.arguments = arguments;
			this.async = async;
			this.stopUIEditing = stopUIEditing;
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
				if (application.isSolutionLoaded())
				{
					if (context.startsWith(ScriptVariable.SCOPES_DOT_PREFIX))
					{
						String scopename = context.substring(ScriptVariable.SCOPES_DOT_PREFIX.length());
						GlobalScope gs = application.getScriptEngine().getScopesScope().getGlobalScope(scopename);
						Object function = gs == null ? null : gs.get(methodname);
						if (function instanceof Function)
						{
							try
							{
								if (stopUIEditing) application.getFoundSetManager().getEditRecordList().prepareForSave(false); // push the updated elements data in the Record.
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
							IFormController fp = application.getFormManager().leaseFormPanel(context);
							if (fp != null)
							{
								fp.initForJSUsage();
								fp.setView(fp.getView());
								fp.executeOnLoadMethod();
								try
								{
									// make sure the foundset of this form is in an initialized state.
									if (!fp.isShowingData()) fp.loadAllRecordsImpl(true);
								}
								catch (Exception ex)
								{
									Debug.error(ex);
									application.handleException(application.getI18NMessage("servoy.formPanel.error.formData"), ex); //$NON-NLS-1$
								}
								retval = fp.executeFunction(methodname, arguments, stopUIEditing, null, true, null, true, false, true);
							}
							else
							{
								retval = new IllegalArgumentException("form: " + context + " didnt resolve to a form in solution " + getSolutionName()); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						catch (Exception e)
						{
							retval = e;
							if (async) application.handleException("Exception calling form method '" + methodname + "' with arguments " +
								Arrays.toString(arguments) + " on form '" + context + "' in async mode on solution " + getSolutionName(), e);

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
		//nop
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#getStyleSheet(java.lang.String)
	 */
	public IStyleSheet getStyleSheet(String name)
	{
		FlattenedSolution flattenedSolution = application.getFlattenedSolution();
		if (flattenedSolution == null)
		{
			return null; // when called before flattened solution is set.
		}
		String style_name = ComponentFactory.getOverriddenStyleName(application, name);
		Style s = flattenedSolution.getStyle(style_name);
		return ComponentFactory.getCSSStyle(application, s);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#getRuntimeWindow(java.lang.String)
	 */
	public IRuntimeWindow getRuntimeWindow(String name)
	{
		return application.getRuntimeWindowManager().getWindow(name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#getCurrentRuntimeWindow()
	 */
	public IRuntimeWindow getCurrentRuntimeWindow()
	{
		RuntimeWindow windowImpl = application.getRuntimeWindowManager().getCurrentWindow();
		if (windowImpl != null && windowImpl.getWrappedObject() != null)
		{
			return windowImpl;
		}
		return application.getRuntimeWindowManager().getWindow(null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#setValueListItems(java.lang.String, java.lang.Object[], java.lang.Object[])
	 */
	public void setValueListItems(String name, Object[] displayValues, Object[] realValues)
	{
		application.setValueListItems(name, displayValues, realValues, true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#setValueListItems(java.lang.String, java.lang.Object[])
	 */
	public void setValueListItems(String name, Object[] displayValues)
	{
		application.setValueListItems(name, displayValues, null, true);
	}

	@Override
	public Map<String, String> getUserProperties()
	{
		Map<String, String> map = new HashMap<String, String>();
		String[] userPropNames = application.getUserPropertyNames();
		if (userPropNames != null)
		{
			for (String propName : userPropNames)
			{
				map.put(propName, application.getUserProperty(propName));
			}
		}
		return map;
	}


	@Override
	public void setUserProperties(Map<String, String> properties)
	{
		if (properties != null)
		{
			for (String propName : properties.keySet())
			{
				application.setUserProperty(propName, properties.get(propName));
			}
		}
	}
}
