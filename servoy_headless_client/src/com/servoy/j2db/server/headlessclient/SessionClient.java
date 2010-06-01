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


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.print.PageFormat;
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
import java.util.List;
import java.util.Locale;
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
import javax.swing.JLabel;
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
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.ILAFManager;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ISessionClient;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.ModeManager;
import com.servoy.j2db.cmd.ICmd;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.RelatedValueList;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.InfoChannel;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.PluginManager;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScriptEngine;
import com.servoy.j2db.scripting.StartupArgumentsScope;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRendererFactory;
import com.servoy.j2db.server.headlessclient.dataui.WebItemFactory;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.smart.dataui.DataRendererFactory;
import com.servoy.j2db.smart.dataui.SwingItemFactory;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.LocalhostRMIRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.TaskThreadPool;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.toolbar.IToolbarPanel;
import com.servoy.j2db.util.toolbar.Toolbar;

/**
 * A client which can be used in a jsp page or inside the org.apache.wicket framework as webclient
 * 
 * @author Jan Blok
 */
public class SessionClient extends ClientState implements ISessionClient
{
	private static final String USER = "user."; //$NON-NLS-1$

	private final HashMap<Locale, Properties> messages = new HashMap<Locale, Properties>();

	protected String username;
	protected String password;
	protected Locale locale;

	protected ItemFactory itemFactory;
	protected IDataRendererFactory<org.apache.wicket.Component> dataRendererFactory;

	private TaskThreadPool taskThreadPool;

	//just for the cases there is no org.apache.wicket running
	private static WebClientsApplication wicket_app = new WebClientsApplication();
	private static Session wicket_session = null;

	private ResourceBundle localeJarMessages;
	private HttpSession session;

	private InfoChannel outputChannel;

	protected SessionClient(ServletRequest req, String name, String pass, String method, Object[] methodArgs, String solution) throws Exception
	{
		super();
		if (req instanceof HttpServletRequest)
		{
			session = ((HttpServletRequest)req).getSession();
		}
		getClientInfo().setApplicationType(getApplicationType());
		getClientInfo().setDontBlockDuringMaintenance(SolutionMetaData.isImportHook(solution));

		boolean reset = testThreadLocals();
		try
		{
			settings = Settings.getInstance();

			username = name;
			password = pass;

			this.preferedSolutionMethodNameToCall = method;
			this.preferedSolutionMethodArguments = methodArgs;
			if (req == null)
			{
				locale = Locale.getDefault();
			}
			else
			{
				locale = req.getLocale();
			}

			setAjaxUsage(solution);

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
			if (reset) unsetThreadLocals();
		}
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
		return super.getSolutionTypeFilter() | SolutionMetaData.MODULE | SolutionMetaData.SMART_CLIENT_ONLY | SolutionMetaData.WEB_CLIENT_ONLY;
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{
		boolean reset = testThreadLocals();
		try
		{
			loadSolutionsAndModules(solutionMeta);
			setAjaxUsage(solutionMeta.getName());
			getScriptEngine();
		}
		finally
		{
			if (reset) unsetThreadLocals();
		}

		if (getSolution() == null)
		{
			throw new IllegalArgumentException(Messages.getString("servoy.exception.solutionNotFound", new Object[] { solutionMeta.getName() })); //$NON-NLS-1$
		}
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
			registered = super.registerClient(uc);
			if (!registered)
			{
				//TODO: trail mode
			}
		}
		catch (final ApplicationException e)
		{
			if ((e.getErrorCode() == ServoyException.NO_LICENSE) || (e.getErrorCode() == ServoyException.MAINTENANCE_MODE))
			{
				shutDown(true);
				throw e;
			}
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
		boolean reset = testThreadLocals();
		try
		{
			super.shutDown(force);
			if (taskThreadPool != null)
			{
				taskThreadPool.stop();
				taskThreadPool = null;
			}

			if (scheduledExecutorService != null)
			{
				scheduledExecutorService.shutdownNow();
				scheduledExecutorService = null;
			}

		}
		finally
		{
			if (reset)
			{
				unsetThreadLocals();
			}
			shuttingDown = false;
		}
	}

	public boolean isShuttingDown()
	{
		return shuttingDown;
	}

	/**
	 * This method the currently set service provider. Will return true if the thread locals must be reset to null.
	 * 
	 * @return true if not found and set (then it should be reset).
	 */
	protected boolean testThreadLocals()
	{
		IServiceProvider provider = J2DBGlobals.getServiceProvider();
		if (provider != this)
		{
			// if this happens it is a webclient in developer..
			// and the provider is not set for this web client. so it must be set.
			provider = null;
		}
		if (provider == null)
		{
			setThreadLocals(this);
		}
		return provider == null;
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

	/*
	 * @see com.servoy.j2db.smart.J2DBClient#refreshI18NMessages()
	 */
	@Override
	protected void refreshI18NMessages()
	{
		messages.clear();
	}

	public void reportInfo(Component parentComponent, String message, String title)
	{
		Debug.log(message);
	}

	public void reportInfo(String message)
	{
		Debug.log(message);
	}

	public void reportError(Component parentComponent, String message, Object detail)
	{
		Debug.error(detail);
	}


	@Override
	protected void checkForActiveTransactions(boolean force)
	{
		if (foundSetManager != null)
		{
			foundSetManager.rollbackTransaction(false);
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
		J2DBGlobals.firePropertyChange(this, "solution", null, getSolution()); //$NON-NLS-1$
	}

	protected TimeZone timeZone;

	private ScheduledExecutorService scheduledExecutorService;

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		return new ScriptEngine(this);
	}

	@Override
	protected void createFoundSetManager()
	{
		foundSetManager = new FoundSetManager(this, null, new SwingFoundSetFactory());
		foundSetManager.init();
	}

	@Override
	protected IFormManager createFormManager()
	{
		WebFormManager fm = new WebFormManager(this, new DummyMainContainer());
		return fm;
	}

	@Override
	public void showDefaultLogin() throws ServoyException
	{
		if (username != null && password != null)
		{
			authenticate(null, null, new Object[] { username, password });
		}
		if (getClientInfo().getUserUid() == null)
		{
			throw new ApplicationException(ServoyException.INCORRECT_LOGIN);
		}
		handleClientUserUidChanged(null, getClientInfo().getUserUid());
	}

	//overridden ssl-rmi seems not to work localy
	@Override
	protected boolean startApplicationServer()
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
		((FoundSetManager)getFoundSetManager()).setColumnManangers(pluginManager.getColumnValidatorManager(), pluginManager.getColumnConverterManager());
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
		setThreadLocals(this);
		try
		{
			String formName = visibleFormName;
			if (formName == null)
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
				throw new IllegalArgumentException("Form " + formName + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
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
			unsetThreadLocals();
		}
		return retval;
	}

	private static void setThreadLocals(IServiceProvider provider)
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
		J2DBGlobals.setServiceProvider(provider);
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

	protected void unsetThreadLocals()
	{
		if (Application.get() == wicket_app)
		{
			Application.unset();
		}
		if (Session.get() == wicket_session)
		{
			Session.unset();
		}
		J2DBGlobals.setServiceProvider(null);
	}

	public synchronized Object getDataProviderValue(String contextName, String dataProviderID)
	{
		if (dataProviderID == null) return null;
		setThreadLocals(this);
		try
		{
			Object value = null;
			if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				String restName = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
				GlobalScope gs = getScriptEngine().getSolutionScope().getGlobalScope();
				value = gs.get(restName);
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
			unsetThreadLocals();
		}
	}

	public synchronized void saveData()
	{
		setThreadLocals(this);
		try
		{
			getFoundSetManager().getEditRecordList().stopEditing(false);
		}
		finally
		{
			unsetThreadLocals();
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
					dataContext = token;
				}
			}

			if (visibleFormName == null)
			{
				visibleFormName = ((FormManager)getFormManager()).getCurrentForm().getName();
			}

			if (visibleFormName != null)
			{
				FormController fp = ((FormManager)getFormManager()).leaseFormPanel(visibleFormName);
				if (!fp.isShowingData()) fp.loadAllRecords();
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
							IFoundSetInternal rfs = r.getRelatedFoundSet(dataContext, null);
							r = rfs.getRecord(rfs.getSelectedIndex());
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
		setThreadLocals(this);
		try
		{
			Pair<IRecordInternal, FormScope> p = getContext(contextName);
			return setDataProviderValue(p, dataprovider, value);
		}
		finally
		{
			unsetThreadLocals();
		}
	}

	private Object setDataProviderValue(Pair<IRecordInternal, FormScope> p, String dataProviderID, Object obj)
	{
		Object prevValue = null;
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String restName = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
			GlobalScope gs = getScriptEngine().getSolutionScope().getGlobalScope();
			prevValue = gs.put(restName, obj);
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
		setThreadLocals(this);
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
			unsetThreadLocals();
		}
	}

	public synchronized boolean setMainForm(String formName)
	{
		setThreadLocals(this);
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
			unsetThreadLocals();
		}
		return false;
	}

	public void setI18NMessagesFilter(String columnname, String value)
	{
		Properties properties = new Properties();
		Messages.loadMessagesFromDatabase(null, getClientInfo().getClientId(), getSettings(), getDataServer(), getRepository(), properties, locale,
			Messages.ALL_LOCALES, null, null, columnname, value);
		Messages.loadMessagesFromDatabase(getSolution(), getClientInfo().getClientId(), getSettings(), getDataServer(), getRepository(), properties, locale,
			Messages.ALL_LOCALES, null, null, columnname, value);
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
			if (properties == null)
			{
				properties = new Properties();
				Messages.loadMessagesFromDatabase(null, getClientInfo().getClientId(), getSettings(), getDataServer(), getRepository(), properties, loc);
				if (getSolution() != null) //must be sure that solution is loaded, app might retrieve system messages, before solution loaded!
				{
					Messages.loadMessagesFromDatabase(getSolution(), getClientInfo().getClientId(), getSettings(), getDataServer(), getRepository(),
						properties, loc);
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

	public void setTimeZone(TimeZone timeZone)
	{
		this.timeZone = timeZone;
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
		setThreadLocals(this);
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
			unsetThreadLocals();
		}
		return null;
	}

	public boolean isValid()
	{
		return !isShuttingDown() && getClientInfo() != null;
	}

	/*
	 * _______________________________________________________________________________
	 */

	public boolean isEventDispatchThread()
	{
		return true;
	}

	private final ReentrantLock executing = new ReentrantLock();

	// invoke later can't add it to a runnable or something. It is not the same thing as invokelater on 
	// swing utilities where it still happens on the event thread but a bit later which can't be done in a web client. 
	public void invokeLater(Runnable r)
	{
		invokeAndWait(r);
	}

	public void invokeAndWait(Runnable r)
	{
		boolean reset = testThreadLocals();
		// We test here for printing, WebForm.processFppInAWTEventQueue(..) will call SwingUtilities.invokeAndWait() to print in awt thread.
		if (!SwingUtilities.isEventDispatchThread()) executing.lock();
		try
		{
			r.run();
		}
		finally
		{
			if (!SwingUtilities.isEventDispatchThread()) executing.unlock();
			if (reset)
			{
				unsetThreadLocals();
			}
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

	public int getClientPlatform()
	{
		// unknown client platform, overridden in WebClient
		return Utils.PLATFORM_OTHER;
	}

	@Override
	public ITaskExecuter getThreadPool()
	{
		if (taskThreadPool == null)
		{
			synchronized (J2DBGlobals.class)
			{
				if (taskThreadPool == null)
				{
					taskThreadPool = new TaskThreadPool(new Runnable()
					{
						public void run()
						{
							setThreadLocals(SessionClient.this);
						}
					}, 3);
				}
			}
		}
		return taskThreadPool;
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
						@Override
						protected void beforeExecute(Thread t, Runnable r)
						{
							super.beforeExecute(t, r);
							setThreadLocals(SessionClient.this);
						}

						@Override
						protected void afterExecute(Runnable r, Throwable t)
						{
							super.afterExecute(r, t);
							unsetThreadLocals();
						}
					};
				}
			}
		}
		return scheduledExecutorService;
	}

	private IBeanManager beanManager;

	public IBeanManager getBeanManager()
	{
		if (beanManager == null)
		{
			beanManager = ApplicationServerSingleton.get().getBeanManager();
		}
		return beanManager;
	}

	private ICmdManager cmdManager;

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

	private IToolbarPanel toolbarPanel;

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


	public void updateInsertMode(IDisplay display)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void logout(Object[] solution_to_open_args)
	{
		if (getClientInfo().getUserUid() != null)
		{
			if (getSolution() != null && getSolution().getMustAuthenticate())
			{
				if (closeSolution(false, solution_to_open_args) && !closing) // don't shutdown if already closing; wait for the first closeSolution to finish
				{
					shutDown(false);//no way to enter username password so shutdown
				}
			}
			else
			{
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
		if (session != null)
		{
			List<String> retval = new ArrayList<String>();
			Enumeration< ? > it = session.getAttributeNames();
			while (it.hasMoreElements())
			{
				String key = (String)it.nextElement();
				if (key.startsWith(USER))
				{
					retval.add(key);
				}
			}
			return retval.toArray(new String[retval.size()]);
		}
		Debug.error("User properties not possible for non http Headless client!"); //$NON-NLS-1$
		return new String[0];
	}

	public void setUserProperty(String a_name, String value)
	{
		if (a_name == null) return;
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

	public boolean setUIProperty(Object name, Object val)
	{
		return false;
	}

	public Object getUIProperty(Object name)
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
	public void activateSolutionMethod(String globalMethodName, StartupArgumentsScope argumentsScope)
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

	private Container printingRendererParent;

	public Container getPrintingRendererParent()
	{
		if (printingRendererParent == null)
		{
			printingRendererParent = new JLabel();
			printingRendererParent.addNotify();
			printingRendererParent.setVisible(true);
			printingRendererParent.setSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
			printingRendererParent.doLayout();
		}
		return printingRendererParent;
	}

	public void setPageFormat(PageFormat pf)
	{
		pageFormat = pf;
	}

	private PageFormat pageFormat = new PageFormat();

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

	public boolean showURL(String url, String target, String target_options, int timeout_ms)
	{
		//ignore
		return false;
	}

	public Rectangle getWindowBounds(String windowName)
	{
		return new Rectangle();
	}

	public void setOutputChannel(InfoChannel channel)
	{
		this.outputChannel = channel;
	}
}
