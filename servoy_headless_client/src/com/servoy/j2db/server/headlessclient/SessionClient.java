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
package com.servoy.j2db.server.headlessclient;


import java.awt.Dimension;
import java.awt.Window;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.RemoteException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISessionClient;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.ModeManager;
import com.servoy.j2db.RuntimeWindowManager;
import com.servoy.j2db.cmd.ICmd;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.RelatedValueList;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.InfoChannel;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRendererFactory;
import com.servoy.j2db.server.headlessclient.dataui.WebItemFactory;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.smart.dataui.DataRendererFactory;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.smart.plugins.PluginManager;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.LocalhostRMIRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.toolbar.IToolbarPanel;
import com.servoy.j2db.util.toolbar.Toolbar;

/**
 * A client which can be used in a jsp page or inside the org.apache.wicket framework as webclient
 * 
 * @author jblok
 */
public class SessionClient extends ClientState implements ISessionClient
{
	private static final String USER = "user."; //$NON-NLS-1$

	private final HashMap<Locale, Properties> messages = new HashMap<Locale, Properties>();

	protected final WebCredentials credentials;

	protected Locale locale;

	protected transient IDataRendererFactory<org.apache.wicket.Component> dataRendererFactory;
	protected transient ItemFactory itemFactory;

	//just for the cases there is no org.apache.wicket running
	private static WebClientsApplication wicket_app = new WebClientsApplication();
	private static Session wicket_session = null;

	private transient ResourceBundle localeJarMessages;
	protected transient HttpSession session;

	private transient InfoChannel outputChannel;
	private RuntimeWindowManager jsWindowManager;

	private final HashMap<String, String> defaultUserProperties = new HashMap<String, String>();

	protected SessionClient(ServletRequest req, String uname, String pass, String method, Object[] methodArgs, String solution) throws Exception
	{
		this(req, new WebCredentials(uname, pass), method, methodArgs, solution);
	}

	protected SessionClient(ServletRequest req, WebCredentials credentials, String method, Object[] methodArgs, String solution) throws Exception
	{
		super();
		if (req instanceof HttpServletRequest)
		{
			session = ((HttpServletRequest)req).getSession();
		}
		getClientInfo().setApplicationType(getApplicationType());
		getClientInfo().setSolutionIntendedToBeLoaded(solution);

		IServiceProvider prev = testThreadLocals();
		try
		{
			settings = Settings.getInstance();
			loadDefaultUserProperties();
			this.credentials = credentials;

			this.preferredSolutionMethodNameToCall = method;
			this.preferredSolutionMethodArguments = methodArgs;

			setAjaxUsage(solution);
			enableAnchors(solution);

			if (req == null)
			{
				String str = getSettings().getProperty("locale.default"); //$NON-NLS-1$
				Locale loc = PersistHelper.createLocale(str);
				if (loc != null)
				{
					locale = loc;
				}
				else
				{
					locale = Locale.getDefault();
				}
			}
			else
			{
				locale = req.getLocale();
			}

			applicationSetup();
			applicationInit();
			applicationServerInit();
			serverInit();

		}
		catch (Exception e)
		{
			// if exception directly do a shutdown, so that this client doesn't hang.
			shutDown(true);
			throw e;
		}
		finally
		{
			unsetThreadLocals(prev);
		}
	}

	@Override
	public void clearLoginForm()
	{
		super.clearLoginForm();
		loggedIn();
	}

	@Override
	protected void loggedIn()
	{
		credentials.setPassword(""); //$NON-NLS-1$
		credentials.setUserName(getClientInfo().getUserUid());
	}

	@Override
	protected void applicationSetup()
	{
		super.applicationSetup();
		jsWindowManager = createJSWindowManager();
	}

	protected RuntimeWindowManager createJSWindowManager()
	{
		return new DummyRuntimeWindowManager(this);
	}

	public RuntimeWindowManager getRuntimeWindowManager()
	{
		return jsWindowManager;
	}

	public void loadSolution(String solutionName) throws RepositoryException
	{
		try
		{
			SolutionMetaData solutionMetaData = getApplicationServer().getSolutionDefinition(solutionName, getSolutionTypeFilter());
			if (solutionMetaData == null)
			{
				throw new IllegalArgumentException(Messages.getString("servoy.exception.solutionNotFound", new Object[] { solutionName })); //$NON-NLS-1$
			}
			loadSolution(solutionMetaData);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	/**
	 * We can define this here to allow all server based client to run every solution type, 
	 * while WebClient as exception uses SolutionLoader logic to load a SOLUTION|WEB_CLIENT_ONLY
	 */
	@Override
	protected int getSolutionTypeFilter()
	{
		return super.getSolutionTypeFilter() | SolutionMetaData.MODULE | SolutionMetaData.SMART_CLIENT_ONLY | SolutionMetaData.WEB_CLIENT_ONLY |
			SolutionMetaData.PRE_IMPORT_HOOK | SolutionMetaData.POST_IMPORT_HOOK;
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{
		IServiceProvider prev = testThreadLocals();
		try
		{
			loadSolutionsAndModules(solutionMeta);
			setAjaxUsage(solutionMeta.getName());
			enableAnchors(solutionMeta.getName());
			getScriptEngine();
		}
		finally
		{
			unsetThreadLocals(prev);
		}

		// Note that getSolution() may return null at this point if the security.closeSolution() or security.logout() was called in onSolutionOpen
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IApplication#updateUI(int)
	 */
	public void updateUI(int time)
	{
		// no use for session/webclients/headless clients
	}


	@Override
	protected SolutionMetaData showSolutionSelection(SolutionMetaData[] solutions)
	{
		return null;
	}

	@Override
	protected boolean registerClient(IUserClient uc) throws Exception
	{
		boolean registered = false;
		try
		{
			registered = super.registerClient(uc); // when registered is false, client is registered but with a trial license
			// access the server directly to mark the client as local
			ApplicationServerSingleton.get().setServerProcess(getClientID());
		}
		catch (final ApplicationException e)
		{
			if ((e.getErrorCode() == ServoyException.NO_LICENSE) || (e.getErrorCode() == ServoyException.MAINTENANCE_MODE))
			{
				shutDown(true);
			}
			throw e;
		}
		return registered;
	}

	private boolean shuttingDown = false;

	/**
	 * @see com.servoy.j2db.ClientState#shutDown(boolean)
	 */
	@Override
	public void shutDown(boolean force)
	{
		shuttingDown = true;
		IServiceProvider prev = testThreadLocals();
		try
		{
			super.shutDown(force);

			if (scheduledExecutorService != null)
			{
				scheduledExecutorService.shutdownNow();
				scheduledExecutorService = null;
			}

		}
		finally
		{
			unsetThreadLocals(prev);
			shuttingDown = false;
		}
	}

	@Override
	public boolean isShutDown()
	{
		return shuttingDown || super.isShutDown();
	}

	/**
	 * This method sets the service provider to this if needed. Will return the previous provider that should be set back later.
	 * 
	 * @return previously set service provider.
	 */
	protected IServiceProvider testThreadLocals()
	{
		if (!Application.exists())
		{
			Application.set(wicket_app);
		}
		if (!Session.exists())
		{
			synchronized (wicket_app)
			{
				if (wicket_session == null)
				{
					wicket_app.fakeInit();
					wicket_session = wicket_app.newSession(new EmptyRequest(), null);
				}
			}
			Session.set(wicket_session);
		}

		IServiceProvider provider = J2DBGlobals.getServiceProvider();
		if (provider != this)
		{
			// if this happens it is a webclient in developer..
			// and the provider is not set for this web client. so it must be set.
			J2DBGlobals.setServiceProvider(this);
		}

		return provider;
	}

	/**
	 * @param solutionName
	 */
	private void setAjaxUsage(String solutionName)
	{
		boolean ajaxEnabledOnServer = Utils.getAsBoolean(settings.getProperty("servoy.webclient.useAjax", "true")); //$NON-NLS-1$ //$NON-NLS-2$
		if (ajaxEnabledOnServer)
		{
			ajaxEnabledOnServer = Utils.getAsBoolean(settings.getProperty("servoy.webclient.useAjax." + solutionName, "true")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		boolean supportsAjax = true;//TODO: disable when simple (pda) broser is detected
		getRuntimeProperties().put("useAJAX", Boolean.toString(ajaxEnabledOnServer && supportsAjax)); //$NON-NLS-1$
	}

	private void enableAnchors(String solutionName)
	{
		boolean anchorsEnabledOnServer = Utils.getAsBoolean(settings.getProperty("servoy.webclient.enableAnchors", "true")); //$NON-NLS-1$ //$NON-NLS-2$
		if (anchorsEnabledOnServer)
		{
			anchorsEnabledOnServer = Utils.getAsBoolean(settings.getProperty("servoy.webclient.enableAnchors." + solutionName, "true")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		getRuntimeProperties().put("enableAnchors", Boolean.toString(anchorsEnabledOnServer)); //$NON-NLS-1$
	}

	/*
	 * @see com.servoy.j2db.smart.J2DBClient#refreshI18NMessages()
	 */
	@Override
	protected void refreshI18NMessages()
	{
		messages.clear();
	}

	public void reportInfo(String message)
	{
		Debug.log(message);
	}

	@Override
	protected void checkForActiveTransactions(boolean force)
	{
		if (foundSetManager != null)
		{
			foundSetManager.rollbackTransaction(true, false, true);
		}
	}

	public void valueBound(HttpSessionBindingEvent e)
	{
	}

	public void valueUnbound(HttpSessionBindingEvent e)
	{
		try
		{
			shutDown(true);
		}
		catch (Exception e1)
		{
			Debug.error(e1);
		}
	}

	@Override
	public boolean isRunningRemote()
	{
		return false;
	}

	@Override
	public URL getServerURL()
	{
		try
		{
			return new URL("http://localhost:" + ApplicationServerSingleton.get().getWebServerPort()); //$NON-NLS-1$
		}
		catch (MalformedURLException e)
		{
			Debug.error(e);
			return null;
		}
	}

	protected ICmdManager createCmdManager()
	{
		return null;
	}

	@Override
	protected IModeManager createModeManager()
	{
		return new ModeManager(this);
	}

	@Override
	public boolean saveSolution()
	{
		return true;//not needed here
	}

	@Override
	protected void solutionLoaded(Solution s)
	{
		super.solutionLoaded(s);
		J2DBGlobals.firePropertyChange(this, "solution", null, getSolution()); //$NON-NLS-1$
	}

	protected TimeZone timeZone;

	private transient ServoyScheduledExecutor scheduledExecutorService;

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new ScriptEngine(this);
	}

	@Override
	protected void createFoundSetManager()
	{
		foundSetManager = new FoundSetManager(this, new SwingFoundSetFactory());
		foundSetManager.init();
	}

	@Override
	protected IFormManager createFormManager()
	{
		WebFormManager fm = new WebFormManager(this, new DummyMainContainer(this));
		return fm;
	}

	@Override
	public void showDefaultLogin() throws ServoyException
	{
		if (credentials.getUserName() != null && credentials.getPassword() != null)
		{
			authenticate(null, null, new Object[] { credentials.getUserName(), credentials.getPassword() });
		}
		if (getClientInfo().getUserUid() == null)
		{
			shutDown(true);
			throw new ApplicationException(ServoyException.INCORRECT_LOGIN);
		}
	}

	//overridden ssl-rmi seems not to work localy
	@Override
	protected boolean startApplicationServerConnection()
	{
		try
		{
			applicationServer = (IApplicationServer)LocalhostRMIRegistry.getService(IApplicationServer.NAME);
			return true;
		}
		catch (Exception ex)
		{
			reportError(Messages.getString("servoy.client.error.finding.dataservice"), ex); //$NON-NLS-1$
			return false;
		}
	}

	public IClientPluginAccess createClientPluginAccess()
	{
		return new ClientPluginAccessProvider(this);
	}


	@Override
	protected void createPluginManager()
	{
		pluginManager = ApplicationServerSingleton.get().getPluginManager().createEfficientCopy(this);
		pluginManager.init();
		((PluginManager)pluginManager).initClientPlugins(this, (IClientPluginAccess)(pluginAccess = createClientPluginAccess()));
		((FoundSetManager)getFoundSetManager()).setColumnManangers(pluginManager.getColumnValidatorManager(), pluginManager.getColumnConverterManager(),
			pluginManager.getUIConverterManager());
	}

	@Override
	protected void saveSettings()
	{
		//do nothing
	}

	protected ILAFManager createLAFManager()
	{
		return ApplicationServerSingleton.get().getLafManager();
	}

	protected IBeanManager createBeanManager()
	{
		return ApplicationServerSingleton.get().getBeanManager();
	}

	/*
	 * _______________________________________________________________________________
	 */

	public synchronized Object executeMethod(String visibleFormName, String methodName, Object[] arguments) throws Exception
	{
		Object retval = null;
		IServiceProvider prev = testThreadLocals();
		try
		{
			String formName = visibleFormName;
			if (formName == null && ((FormManager)getFormManager()).getCurrentForm() != null)
			{
				formName = ((FormManager)getFormManager()).getCurrentForm().getName();
			}

			if (formName != null)
			{
				FormController fp = ((FormManager)getFormManager()).leaseFormPanel(formName);
				if (fp != null && fp.isFormVisible())
				{
					return fp.executeFunction(methodName, arguments, true, null, false, null);
				}
				else
				{
					throw new IllegalStateException("Cannot call method on non visible form " + formName); //$NON-NLS-1$
				}
			}
			else
			{
				throw new IllegalArgumentException("No current visible form specified"); //$NON-NLS-1$
			}
		}
		catch (IllegalStateException e1)
		{
			throw e1;
		}
		catch (IllegalArgumentException e2)
		{
			throw e2;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			unsetThreadLocals(prev);
		}
		return retval;
	}

	public WebClientsApplication getFakeApplication()
	{
		synchronized (wicket_app)
		{
			if (wicket_session == null)
			{
				wicket_app.fakeInit();
				wicket_session = wicket_app.newSession(new EmptyRequest(), null);
			}
		}
		return wicket_app;
	}

	protected void unsetThreadLocals(IServiceProvider prev)
	{
		if (J2DBGlobals.getServiceProvider() != prev)
		{
			if (Application.get() == wicket_app)
			{
				Application.unset();
			}
			if (Session.exists() && Session.get() == wicket_session)
			{
				Session.unset();
			}
			J2DBGlobals.setServiceProvider(prev);
		}
	}

	public synchronized Object getDataProviderValue(String contextName, String dataProviderID)
	{
		if (dataProviderID == null) return null;
		IServiceProvider prev = testThreadLocals();
		try
		{
			Object value = null;
			if (ScopesUtils.isVariableScope(dataProviderID))
			{
				value = getScriptEngine().getSolutionScope().getScopesScope().get(null, dataProviderID);
			}
			else
			{
				Pair<IRecordInternal, FormScope> p = getContext(contextName);
				if (p != null)
				{
					FormScope fs = p.getRight();
					IRecordInternal record = p.getLeft();
					if (fs != null && fs.has(dataProviderID, fs)) // how can fs be null.
					{
						value = fs.get(dataProviderID);
					}
					else if (record != null)
					{
						value = record.getValue(dataProviderID);
					}
					if (value == Scriptable.NOT_FOUND) value = ""; //$NON-NLS-1$
				}
			}
			return value;
		}
		finally
		{
			unsetThreadLocals(prev);
		}
	}

	public synchronized void saveData()
	{
		IServiceProvider prev = testThreadLocals();
		try
		{
			getFoundSetManager().getEditRecordList().stopEditing(false);
		}
		finally
		{
			unsetThreadLocals(prev);
		}
	}

	private Pair<IRecordInternal, FormScope> getContext(String contextName)
	{
		try
		{
			String visibleFormName = contextName;
			String dataContext = null;

			if (contextName != null)
			{
				StringTokenizer tk = new StringTokenizer(contextName, "."); //$NON-NLS-1$
				String token = tk.nextToken();
				if (token.equals("forms") && tk.hasMoreTokens()) //$NON-NLS-1$
				{
					visibleFormName = tk.nextToken();
					if (tk.hasMoreTokens()) token = tk.nextToken();
				}
				if (!token.equals("foundset")) //$NON-NLS-1$
				{
					// todo why is this always also assigned to the datacontext?
					// shouldnt the above if be: if (token.equals("foundset") && st.hasMoreTokes()) dataContext == st.nextToken(); 
					// because now this data context will just be set to a form name if the contextName is just a form. (which in many cases it is defined like that)
					dataContext = token;
				}
			}

			if (visibleFormName == null)
			{
				IForm tmp = ((FormManager)getFormManager()).getCurrentForm();
				if (tmp != null) visibleFormName = tmp.getName();
			}

			if (visibleFormName != null)
			{
				// just overwrite the above assignment again if the datacontext is really also the form, so it wont be used later on.
				if (Utils.stringSafeEquals(visibleFormName, dataContext))
				{
					dataContext = null;
				}
				FormController fp = ((FormManager)getFormManager()).leaseFormPanel(visibleFormName);
				if (!fp.isShowingData())
				{
					if (fp.wantEmptyFoundSet())
					{
						if (fp.getFormModel() != null) fp.getFormModel().clear();
					}
					else
					{
						fp.loadAllRecords();
					}
				}
				IFoundSetInternal fs = fp.getFoundSet();
				if (fs != null)
				{
					int idx = fs.getSelectedIndex();
					if (idx < 0) idx = 0;
					IRecordInternal r = fs.getRecord(idx);
					if (r != null)
					{
						if (dataContext != null)
						{
							IFoundSetInternal rfs = r.getRelatedFoundSet(dataContext);
							// rfs can be null because dataContext can just be a anything see above
							if (rfs != null)
							{
								r = rfs.getRecord(rfs.getSelectedIndex());
							}
						}
						return new Pair<IRecordInternal, FormScope>(r, fp.getFormScope());
					}
				}
				return new Pair<IRecordInternal, FormScope>(null, fp.getFormScope());
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public synchronized Object setDataProviderValue(String contextName, String dataprovider, Object value)
	{
		IServiceProvider prev = testThreadLocals();
		try
		{
			Pair<IRecordInternal, FormScope> p = getContext(contextName);
			return setDataProviderValue(p, dataprovider, value);
		}
		finally
		{
			unsetThreadLocals(prev);
		}
	}

	private Object setDataProviderValue(Pair<IRecordInternal, FormScope> p, String dataProviderID, Object obj)
	{
		Object prevValue = null;
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			getScriptEngine().getScopesScope().getOrCreateGlobalScope(scope.getLeft()).put(scope.getRight(), obj);
		}
		else if (p != null)
		{
			IRecordInternal record = p.getLeft();
			FormScope fs = p.getRight();
			if (fs.has(dataProviderID, fs))
			{
				prevValue = fs.get(dataProviderID);
				fs.put(dataProviderID, obj);
			}
			else if (record != null && record.startEditing())
			{
				try
				{
					prevValue = record.getValue(dataProviderID);
					record.setValue(dataProviderID, obj);
				}
				catch (IllegalArgumentException e)
				{
					Debug.trace(e);
				}
			}
		}
		return prevValue;
	}

	public synchronized int setDataProviderValues(String contextName, HttpServletRequest request_data)
	{
		int retval = 0;
		if (request_data.getCharacterEncoding() == null)
		{
			try
			{
				request_data.setCharacterEncoding(wicket_app.getRequestCycleSettings().getResponseRequestEncoding());
			}
			catch (UnsupportedEncodingException e)
			{
				Debug.log(e);
			}
		}
		IServiceProvider prev = testThreadLocals();
		try
		{
			Pair<IRecordInternal, FormScope> p = getContext(contextName);
			Enumeration< ? > e = request_data.getParameterNames();
			while (e.hasMoreElements())
			{
				String param = (String)e.nextElement();
				Object val = request_data.getParameter(param);
				Object oldVal = setDataProviderValue(p, param, val);
				if (!(Utils.equalObjects(oldVal, val))) retval++;
			}
			return retval;
		}
		finally
		{
			unsetThreadLocals(prev);
		}
	}

	public synchronized boolean setMainForm(String formName)
	{
		IServiceProvider prev = testThreadLocals();
		try
		{
			FormController fp = ((FormManager)getFormManager()).showFormInMainPanel(formName);
			if (fp != null && fp.getName().equals(formName))
			{
				return true;
			}
			else
			{
				Debug.trace("Form panel " + fp + " is (still) current main form"); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IllegalArgumentException("Form " + formName + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (IllegalArgumentException e1)
		{
			throw e1;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			unsetThreadLocals(prev);
		}
		return false;
	}

	public void setI18NMessagesFilter(String columnname, String[] value)
	{
		Properties properties = new Properties();
		Messages.loadMessagesFromDatabaseInternal(null, getClientInfo().getClientId(), getSettings(), getDataServer(), getRepository(), properties, locale,
			Messages.ALL_LOCALES, null, null, columnname, value);
		Solution solution = getSolution();
		Messages.loadMessagesFromDatabaseInternal(solution != null ? solution.getI18nDataSource() : null, getClientInfo().getClientId(), getSettings(),
			getDataServer(), getRepository(), properties, locale, Messages.ALL_LOCALES, null, null, columnname, value);
		synchronized (messages)
		{
			messages.put(locale, properties);
		}
	}

	public ResourceBundle getResourceBundle(Locale lc)
	{
		final Locale loc = lc != null ? lc : locale != null ? locale : Locale.getDefault();
		final ResourceBundle jarMessages = ResourceBundle.getBundle(Messages.BUNDLE_NAME, loc);
		final Properties msg = getMessages(loc);
		return new ResourceBundle()
		{
			@Override
			protected Object handleGetObject(String key)
			{
				return getI18NMessage(key, null, msg, jarMessages, loc);
			}

			@Override
			public Locale getLocale()
			{
				return loc;
			}

			@Override
			public Enumeration<String> getKeys()
			{
				return new Enumeration<String>()
				{
					private Enumeration< ? > solutionKeys = msg.keys();
					private final Enumeration< ? > jarKeys = jarMessages.getKeys();

					public String nextElement()
					{
						if (solutionKeys != null) return solutionKeys.nextElement().toString();
						else return jarKeys.nextElement().toString();
					}

					public boolean hasMoreElements()
					{
						if (solutionKeys != null && solutionKeys.hasMoreElements())
						{
							return true;
						}
						solutionKeys = null;
						return jarKeys.hasMoreElements();
					}
				};
			}
		};
	}

	public String getI18NMessage(String key, Object[] args)
	{
		if (key == null || key.length() == 0) return key;

		Properties properties = getMessages(getLocale());
		return getI18NMessage(key, args, properties, localeJarMessages, getLocale());
	}

	public String getI18NMessage(String key)
	{
		if (key == null || key.length() == 0) return key;

		Properties properties = getMessages(getLocale());
		return getI18NMessage(key, null, properties, localeJarMessages, getLocale());
	}

	public void setI18NMessage(String key, String value)
	{
		if (key != null)
		{
			Properties properties = getMessages(getLocale());
			if (value == null)
			{
				properties.remove(key);
				refreshI18NMessages();
			}
			else
			{
				properties.setProperty(key, value);
			}

		}
	}

	private static String getI18NMessage(String key, Object[] args, Properties msg, ResourceBundle jar, Locale loc)
	{
		String realKey = key;
		if (realKey.startsWith("i18n:")) //$NON-NLS-1$
		{
			realKey = realKey.substring(5);
		}
		String message = null;
		try
		{
			if (jar != null)
			{
				try
				{
					message = jar.getString(realKey);
				}
				catch (Exception e)
				{
				}
			}
			if (message != null && msg.getProperty(realKey) == null)
			{
				return message;
			}
			message = msg.getProperty(realKey);
			if (message == null) return '!' + realKey + '!';
			message = Utils.stringReplace(message, "'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
			MessageFormat mf = new MessageFormat(message);
			mf.setLocale(loc);
			return mf.format(args);
		}
		catch (MissingResourceException e)
		{
			return '!' + realKey + '!';
		}
		catch (Exception e)
		{
			return '!' + realKey + "!,txt:" + message + ", error:" + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}


	private Properties getMessages(Locale loc)
	{
		Properties properties = null;
		synchronized (messages)
		{
			properties = messages.get(loc);
			if (properties == null && getClientInfo() != null)
			{
				properties = new Properties();
				Messages.invalidConnection = false;
				Messages.loadMessagesFromDatabaseInternal(null, ApplicationServerSingleton.get().getClientId(), getSettings(), getDataServer(),
					getRepository(), properties, loc);
				if (getSolution() != null) //must be sure that solution is loaded, app might retrieve system messages, before solution loaded!
				{
					Messages.loadMessagesFromDatabaseInternal(getSolution().getI18nDataSource(), ApplicationServerSingleton.get().getClientId(), getSettings(),
						getDataServer(), getRepository(), properties, loc);
					messages.put(loc, properties);
				}
			}
			// also test here for the local jar message
			if (localeJarMessages == null && loc.equals(getLocale()))
			{
				localeJarMessages = ResourceBundle.getBundle(Messages.BUNDLE_NAME, loc);
			}
		}

		return properties == null ? new Properties() : properties;
	}

	/*
	 * @see IServiceProvider#getI18NMessageIfPrefixed(String,Object[])
	 */
	public String getI18NMessageIfPrefixed(String key)
	{
		if (key != null && key.startsWith("i18n:")) //$NON-NLS-1$
		{
			return getI18NMessage(key.substring(5), null);
		}
		return key;
	}

	public synchronized void setLocale(Locale l)
	{
		if (locale != null && locale.equals(l)) return;
		Locale old = locale;
		locale = l;
		localeJarMessages = null;
		J2DBGlobals.firePropertyChange(this, "locale", old, locale); //$NON-NLS-1$
	}

	public Locale getLocale()
	{
		return locale == null ? Locale.getDefault() : locale;
	}

	public TimeZone getTimeZone()
	{
		return timeZone == null ? TimeZone.getDefault() : timeZone;
	}

	public synchronized void setTimeZone(TimeZone zone)
	{
		if (timeZone != null && timeZone.equals(zone)) return;
		TimeZone old = timeZone;
		timeZone = zone;
		J2DBGlobals.firePropertyChange(this, "timeZone", old, timeZone); //$NON-NLS-1$

		ClientInfo clientInfo = getClientInfo();
		clientInfo.setTimeZone(timeZone);
		try
		{
			getClientHost().pushClientInfo(clientInfo.getClientId(), clientInfo);
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
	}

	public synchronized IDataSet getValueListItems(String contextName, String valuelistName)
	{
		IServiceProvider prev = testThreadLocals();
		try
		{
			ValueList vl = getFlattenedSolution().getValueList(valuelistName);
			if (vl != null)
			{
				// TODO should getValueListItems not specify type and format??
				IValueList valuelist = ComponentFactory.getRealValueList(this, vl, true, Types.OTHER, null, null);
				if (valuelist instanceof RelatedValueList)
				{
					Pair<IRecordInternal, FormScope> p = getContext(contextName);
					if (p != null)
					{
						IRecordInternal r = p.getLeft();
						if (r != null)
						{
							valuelist.fill(r);
						}
					}
				}
				if (valuelist != null)
				{
					ArrayList<Object[]> rows = new ArrayList<Object[]>();
					for (int i = 0; i < valuelist.getSize(); i++)
					{
						rows.add(new Object[] { valuelist.getElementAt(i), valuelist.getRealElementAt(i) });
					}
					return new BufferedDataSet(new String[] { "displayValue", "realValue" }, rows); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			else
			{
				throw new IllegalArgumentException("Valuelist with name " + valuelistName + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			unsetThreadLocals(prev);
		}
		return null;
	}

	public boolean isValid()
	{
		return !isShutDown() && getClientInfo() != null;
	}

	/*
	 * _______________________________________________________________________________
	 */

	public boolean isEventDispatchThread()
	{
		return true;
	}

	private final ReentrantLock executing = new ReentrantLock();

	protected boolean isExecutionLocked()
	{
		return executing.isLocked();
	}

	// invoke later can't add it to a runnable or something. It is not the same thing as invokelater on 
	// swing utilities where it still happens on the event thread but a bit later which can't be done in a web client. 
	public void invokeLater(Runnable r)
	{
		invokeAndWait(r);
	}

	public void invokeAndWait(Runnable r)
	{
		IServiceProvider prev = testThreadLocals();
		// We test here for printing, WebForm.processFppInAWTEventQueue(..) will call SwingUtilities.invokeAndWait() to print in awt thread.
		if (!SwingUtilities.isEventDispatchThread()) executing.lock();
		try
		{
			r.run();
		}
		finally
		{
			if (!SwingUtilities.isEventDispatchThread()) executing.unlock();
			unsetThreadLocals(prev);
		}
	}

	public void addURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		//ignore
	}

	public String getApplicationName()
	{
		return "Servoy Headless Client"; //$NON-NLS-1$
	}

	public int getApplicationType()
	{
		return HEADLESS_CLIENT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IApplication#getOSName()
	 */
	public String getClientOSName()
	{
		return System.getProperty("os.name"); //$NON-NLS-1$
	}

	public int getClientPlatform()
	{
		// unknown client platform, overridden in WebClient
		return Utils.PLATFORM_OTHER;
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor()
	{
		if (scheduledExecutorService == null)
		{
			synchronized (J2DBGlobals.class)
			{
				if (scheduledExecutorService == null)
				{
					scheduledExecutorService = new ServoyScheduledExecutor(1, 4, 1)
					{
						private IServiceProvider prev;

						@Override
						protected void beforeExecute(Thread t, Runnable r)
						{
							super.beforeExecute(t, r);
							prev = testThreadLocals();
						}

						@Override
						protected void afterExecute(Runnable r, Throwable t)
						{
							super.afterExecute(r, t);
							unsetThreadLocals(prev);
						}
					};
				}
			}
		}
		return scheduledExecutorService;
	}

	private transient IBeanManager beanManager;

	public IBeanManager getBeanManager()
	{
		if (beanManager == null)
		{
			beanManager = ApplicationServerSingleton.get().getBeanManager();
		}
		return beanManager;
	}

	private transient ICmdManager cmdManager;

	public ICmdManager getCmdManager()
	{
		if (cmdManager == null)
		{
			cmdManager = new ICmdManager()
			{
				public void executeRegisteredAction(String name)
				{
				}

				public void registerAction(String name, Action a)
				{
				}

				public Action getRegisteredAction(String name)
				{
					return null;
				}

				public void executeCmd(ICmd c, EventObject ie)
				{
				}

				public void init()
				{
				}

				public void flushCachedItems()
				{
				}
			};
		}
		return cmdManager;
	}

	public JMenu getExportMenu()
	{
		return null;
	}

	public JMenu getImportMenu()
	{
		return null;
	}

	public ILAFManager getLAFManager()
	{
		return null;
	}

	private transient IToolbarPanel toolbarPanel;

	public IToolbarPanel getToolbarPanel()
	{
		if (toolbarPanel == null)
		{
			toolbarPanel = new IToolbarPanel()
			{
				public String[] getToolBarNames()
				{
					return new String[0];
				}

				public Toolbar createToolbar(String name, String displayName)
				{
					return null;
				}

				public Toolbar createToolbar(String name, String displayName, int wantedRow)
				{
					return null;
				}

				public void removeToolBar(String name)
				{
				}

				public Toolbar getToolBar(String name)
				{
					return null;
				}

				public void setToolbarVisible(String name, boolean visible)
				{
				}
			};
		}
		return toolbarPanel;
	}

	@Override
	public void logout(Object[] solution_to_open_args)
	{
		if (getClientInfo().getUserUid() != null)
		{
			if (getSolution() != null && getSolution().requireAuthentication())
			{
				if (closeSolution(false, solution_to_open_args)) // don't shutdown if already closing; wait for the first closeSolution to finish
				{
					if (!isClosing)
					{
						shutDown(false);//no way to enter username password so shutdown
					}
					else
					{
						credentials.clear();
						getClientInfo().clearUserInfo();
					}
				}
			}
			else
			{
				credentials.clear();
				getClientInfo().clearUserInfo();
			}
		}
	}

	public void output(Object msg, int level)
	{
		if (level == DEBUG)
		{
			Debug.debug(msg);
		}
		else if (level == WARNING)
		{
			Debug.warn(msg);
		}
		else if (level == ERROR)
		{
			Debug.error(msg);
		}
		else if (level == FATAL)
		{
			Debug.fatal(msg);
		}
		else
		{
			Debug.log(msg);
		}
		if (outputChannel != null) outputChannel.info(msg != null ? msg.toString() : "NULL", level); //$NON-NLS-1$
	}

	public void registerWindow(String name, Window d)
	{
	}

	public Window getWindow(String name)
	{
		return null;
	}

	public void setStatusProgress(int progress)
	{
	}

	public void setStatusText(String text, String tooltip)
	{
	}

	public void setTitle(String title)
	{
	}

	public String getUserProperty(String a_name)
	{
		if (a_name == null) return null;
		String defaultUserProperty = getDefaultUserProperties().get(a_name);
		if (defaultUserProperty != null) return defaultUserProperty;
		CharSequence name = Utils.stringLimitLenght(a_name, 255);
		if (session != null)
		{
			return (String)session.getAttribute(USER + name);
		}
		else
		{
			return settings.getProperty(USER + name);
		}
	}

	public String[] getUserPropertyNames()
	{
		List<String> retval = new ArrayList<String>();
		if (session != null)
		{
			Enumeration< ? > it = session.getAttributeNames();
			while (it.hasMoreElements())
			{
				String key = (String)it.nextElement();
				if (key.startsWith(USER))
				{
					retval.add(key);
				}
			}

		}
		for (String defaultUserPropertyKey : getDefaultUserProperties().keySet())
		{
			if (retval.indexOf(defaultUserPropertyKey) == -1)
			{
				retval.add(defaultUserPropertyKey);
			}
		}

		return retval.toArray(new String[retval.size()]);
	}

	public void setUserProperty(String a_name, String value)
	{
		if (a_name == null) return;
		getDefaultUserProperties().remove(a_name); // clear
		CharSequence name = Utils.stringLimitLenght(a_name, 255);
		if (session != null)
		{
			if (value == null)
			{
				session.removeAttribute(USER + name);
			}
			else
			{
				session.setAttribute(USER + name, Utils.stringLimitLenght(value, 255));
			}
		}
		else
		{
			if (value == null)
			{
				settings.remove(USER + name);
			}
			else
			{
				settings.setProperty(USER + name, Utils.stringLimitLenght(value, 255).toString());
			}
		}
	}

	private void loadDefaultUserProperties()
	{
		Iterator<Object> it = getSettings().keySet().iterator();
		while (it.hasNext())
		{
			String key = (String)it.next();
			if (key.startsWith(USER))
			{
				defaultUserProperties.put(key.substring(USER.length()), getSettings().getProperty(key));
			}
		}
	}

	public Map<String, String> getDefaultUserProperties()
	{
		return defaultUserProperties;
	}

	public boolean putClientProperty(Object name, Object val)
	{
		return false;
	}

	public Object getClientProperty(Object name)
	{
		return null;
	}

	public JFrame getMainApplicationFrame()
	{
		return null;
	}

	public ImageIcon loadImage(String name)
	{
		return null;
	}

	public void reportWarningInStatus(String s)
	{
		reportWarning(s);
	}

	@Override
	public void blockGUI(String reason)
	{
	}

	@Override
	public void releaseGUI()
	{
	}

	@Override
	protected void bindUserClient()
	{
		//not needed in headless
	}

	/**
	 * @see com.servoy.j2db.ClientState#testClientRegistered(Object)
	 */
	@Override
	protected boolean testClientRegistered(Object exception)
	{
		if (exception instanceof ServoyException && ((ServoyException)exception).getErrorCode() == ServoyException.InternalCodes.CLIENT_NOT_REGISTERED)
		{
			if (session != null)
			{
				Debug.log("Client was not registered, invalidating the http session"); //$NON-NLS-1$
				try
				{
					shutDown(true);
				}
				catch (Exception e)
				{
					Debug.trace("error calling shutdown in a client is not registered call", e); //$NON-NLS-1$
				}
				try
				{
					session.invalidate();
				}
				catch (Exception e)
				{
					Debug.trace("error calling session invalidate in a client is not registered call", e); //$NON-NLS-1$
				}
			}
			return false;
		}
		return true;
	}

	@Override
	protected void unBindUserClient() throws Exception
	{
		//not needed in headless
	}

	@Override
	public void activateSolutionMethod(String globalMethodName, StartupArguments argumentsScope)
	{
		//not needed cannot push to client
	}

	public ItemFactory getItemFactory()
	{
		if (Utils.getAsBoolean(getRuntimeProperties().get("isPrinting"))) //$NON-NLS-1$
		{
			return new SwingItemFactory(this);//needed to be able to print
		}
		if (itemFactory == null)
		{
			itemFactory = new WebItemFactory(this);
		}
		return itemFactory;
	}

	public IDataRendererFactory< ? > getDataRenderFactory()
	{
		if (Utils.getAsBoolean(getRuntimeProperties().get("isPrinting"))) //$NON-NLS-1$
		{
			return new DataRendererFactory();//needed to be able to print
		}
		if (dataRendererFactory == null)
		{
			dataRendererFactory = new WebDataRendererFactory();
		}
		return dataRendererFactory;
	}

	private transient RendererParentWrapper printingRendererParent;

	public RendererParentWrapper getPrintingRendererParent()
	{
		if (printingRendererParent == null)
		{
			printingRendererParent = new RendererParentWrapper();
		}
		return printingRendererParent;
	}

	public void setPageFormat(PageFormat pf)
	{
		pageFormat = pf;
	}

	private transient PageFormat pageFormat = new PageFormat();

	public PageFormat getPageFormat()
	{
		return pageFormat;
	}

	@Override
	public IClientPluginAccess getPluginAccess()
	{
		return (IClientPluginAccess)super.getPluginAccess();
	}

	public Dimension getScreenSize()
	{
		return new Dimension(-1, -1);
	}

	public boolean showURL(String url, String target, String target_options, int timeout_ms, boolean onRootFrame)
	{
		//ignore
		return false;
	}

	public void setOutputChannel(InfoChannel channel)
	{
		this.outputChannel = channel;
	}

	public void looseFocus()
	{
		//nop
	}

	private void writeObject(ObjectOutputStream stream) throws IOException
	{
		//serialize is not implemented
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		//serialize is not implemented
	}

	private boolean isFormElementsEditableInFindMode = true;

	/*
	 * @see com.servoy.j2db.IApplication#setFormElementsEditableInFindMode(boolean)
	 */
	public void setFormElementsEditableInFindMode(boolean editable)
	{
		isFormElementsEditableInFindMode = editable;
	}

	/*
	 * @see com.servoy.j2db.IApplication#isFormElementsEditableInFindMode()
	 */
	public boolean isFormElementsEditableInFindMode()
	{
		return isFormElementsEditableInFindMode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.IApplication#onSolutionOpen()
	 */
	public void onSolutionOpen()
	{
		//nop
	}
}
