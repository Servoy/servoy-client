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
package com.servoy.j2db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;

import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.persistence.ClientMethodTemplatesLoader;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManagerInternal;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.StartupArgumentsScope;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerAccess;
import com.servoy.j2db.server.shared.IClientManager;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.serialize.JSONConverter;

/**
 * Bare bone state and abstract base class for all client process instance
 * 
 * @author jblok
 */
public abstract class ClientState extends ClientVersion implements IServiceProvider, Serializable
{
	public static String READY = Messages.getString("servoy.general.status.ready"); //$NON-NLS-1$

	/**
	 * Fields
	 */
	//provides basic server interaction
	protected transient IApplicationServer applicationServer;

	//provides extended server access after authentication
	protected transient IApplicationServerAccess applicationServerAccess;

	//local reference to repository
	protected transient IRepository repository = null;

	//local reference to dataserver
	private transient IDataServer dataServer;

	//local reference to client host
	private transient IClientHost clientHost;

	//the script engine
	private volatile IExecutingEnviroment scriptEngine;

	//holding the application setting
	protected Properties settings;

	//preferred solution (args) 
	protected String preferredSolutionNameToLoadOnInit = null;
	protected String preferredSolutionMethodNameToCall = null;
	protected Object[] preferredSolutionMethodArguments = null;

	//the main solution, also called root
	protected final FlattenedSolution solutionRoot = new FlattenedSolution();

	/**
	 * Managers
	 */
	//form manager handling the forms
	protected IFormManager formManager;

	//mode manager handling the application mode
	protected transient IModeManager modeManager;

	//foundset manager handling the foundsets
	protected transient volatile IFoundSetManagerInternal foundSetManager;

	//plugin manager handling the (scriptable plugins)
	protected transient IPluginManagerInternal pluginManager;

	//user manager, giving access to (other)user info 
	private transient volatile IUserManager userManager;

	// does this client use the login solution when configured?
	private boolean useLoginSolution = true;

	protected ClientState()
	{
		//security check:all subclasses should be signed by same certificateS, this way unsigned subclasses are impossible to run
		if (!Utils.equalObjects(this.getClass().getSigners(), ClientState.class.getSigners()))
		{
			throw new IllegalStateException();
		}
		clientInfo = new ClientInfo();

		// firing some form events needs to know the position of JSEvent argument
		ClientMethodTemplatesLoader.loadClientMethodTemplatesIfNeeded();
	}

	protected void logStartUp()
	{
		Debug.log("Starting Servoy from " + System.getProperty("user.dir")); //$NON-NLS-1$//$NON-NLS-2$
		Debug.log("Servoy " + getVersion() + " build-" + getReleaseNumber() + " on " + System.getProperty("os.name") + " using Java " + System.getProperty("java.version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
//		Debug.log("file.encoding " + System.getProperty("file.encoding"));
	}

	private void appendArgumentsScopeToPreferedSolutionMethodArguments(StartupArgumentsScope argumentsScope)
	{
		if (preferredSolutionMethodArguments != null && preferredSolutionMethodArguments.length > 0)
		{
			Object[] new_preferedSolutionMethodArguments = new Object[preferredSolutionMethodArguments.length + 1];
			System.arraycopy(preferredSolutionMethodArguments, 0, new_preferedSolutionMethodArguments, 0, preferredSolutionMethodArguments.length);
			new_preferedSolutionMethodArguments[preferredSolutionMethodArguments.length] = argumentsScope;
			preferredSolutionMethodArguments = new_preferedSolutionMethodArguments;
		}
	}

	public void handleArguments(String args[], StartupArgumentsScope argumentsScope)
	{
		handleArguments(args);
		appendArgumentsScopeToPreferedSolutionMethodArguments(argumentsScope);
	}

	public void handleArguments(String[] args)
	{

		clientInfo.setSpecialClientIndentifier(null);
		if (args == null || args.length == 0)
		{
			//clear, do not clear method and arguments (clear method when it is called, we want to access the arguments during the app livespan)
			preferredSolutionNameToLoadOnInit = null;
		}
		else
		{
			StartupArgumentsScope argumentsScope = new StartupArgumentsScope(args);

			if (argumentsScope.getSolutionName() == null && argumentsScope.getMethodName() == null && argumentsScope.getFirstArgument() == null &&
				argumentsScope.getClientIdentifier() == null)
			{
				preferredSolutionNameToLoadOnInit = args[0];
				if (args.length >= 2)
				{
					if (args[1] != null && args[1].startsWith("CI:")) //$NON-NLS-1$
					{
						clientInfo.setSpecialClientIndentifier(args[1].substring(3));
					}
					else
					{
						preferredSolutionMethodNameToCall = args[1];
					}
					preferredSolutionMethodArguments = null;
					if (args.length >= 3)
					{
						if (args[2] != null && args[2].startsWith("CI:")) //$NON-NLS-1$
						{
							clientInfo.setSpecialClientIndentifier(args[2].substring(3));
						}
						else
						{
							preferredSolutionMethodArguments = new Object[] { args[2] };
						}
						if (args.length >= 4 && args[3] != null && args[3].startsWith("CI:")) //$NON-NLS-1$
						{
							clientInfo.setSpecialClientIndentifier(args[3].substring(3));
						}
					}
				}
			}
			else
			{
				preferredSolutionNameToLoadOnInit = argumentsScope.getSolutionName();
				preferredSolutionMethodNameToCall = argumentsScope.getMethodName();
				preferredSolutionMethodArguments = argumentsScope.getFirstArgument() != null ? new Object[] { argumentsScope.getFirstArgument() } : null;
				if (argumentsScope.getClientIdentifier() != null) clientInfo.setSpecialClientIndentifier(argumentsScope.getClientIdentifier());

				appendArgumentsScopeToPreferedSolutionMethodArguments(argumentsScope);
			}
		}
	}

	public Object[] getPreferedSolutionMethodArguments()
	{
		return preferredSolutionMethodArguments;
	}

	public String getPreferedSolutionMethodNameToCall()
	{
		return preferredSolutionMethodNameToCall;
	}

	public String getPreferedSolutionNameToLoadOnInit()
	{
		return preferredSolutionNameToLoadOnInit;
	}

	public void resetPreferedSolutionMethodNameToCall()
	{
		preferredSolutionMethodNameToCall = null;
	}

	protected void applicationSetup()
	{
		refreshI18NMessages();

		TimeZone defaultTimeZone = TimeZone.getDefault();
		if (defaultTimeZone != null) //can this happen?
		{
			String str = getSettings().getProperty("timezone.default", defaultTimeZone.getID()); //$NON-NLS-1$
			TimeZone tz = TimeZone.getTimeZone(str);
			if (tz != null)
			{
				getClientInfo().setTimeZone(tz);
			}
		}

		// create modemanager
		modeManager = createModeManager();

		// create formmanager
		formManager = createFormManager();

		// Runtime.getRuntime().addShutdownHook(new Thread()
		// {
		// public void run()
		// {
		// shutDown(true,false);//last arg is false because we are exiting
		// }
		// });
	}

	protected boolean applicationInit()
	{
		registerListeners();

		createUserClient();

		//Loading plugins
		//setStatusText(Messages.getString("servoy.client.status.load.plugins")); //$NON-NLS-1$

		return true;
	}

	protected boolean applicationServerInit() throws Exception
	{
		boolean b = startApplicationServer();
		bindUserClient();
		registerClient(userClient);

		createPluginManager();
		return b;
	}

	//likely called on non awt thread
	protected boolean serverInit()
	{
		return true;
	}

	public IUserManager getUserManager()
	{
		if (userManager == null)
		{
			synchronized (this)
			{
				if (userManager == null)
				{
					userManager = createUserManager();
				}
			}
		}
		return userManager;
	}

	@SuppressWarnings("nls")
	protected IUserManager createUserManager()
	{
		IApplicationServerAccess asa = getApplicationServerAccess();
		if (asa == null)
		{
			return null;
		}
		try
		{
			return asa.getUserManager(getClientID());
		}
		catch (RemoteException e)
		{
			Debug.error("Cannot get user manager", e);
		}
		return null;
	}

	/**
	 * @return success
	 */
	protected abstract boolean startApplicationServer();

	protected abstract void bindUserClient();

	public void unRegisterClient(String client_id) throws RemoteException
	{
		IClientHost ch = getClientHost();
		if (ch != null)
		{
			ch.unregister(client_id);
		}
	}

	/**
	 * Handle client login/logout.
	 * 
	 * @param userUidBefore
	 * @param userUidAfter
	 */
	@SuppressWarnings("nls")
	public void handleClientUserUidChanged(String userUidBefore, String userUidAfter)
	{
		if (userUidBefore == null && userUidAfter == null) return;
		if (isShutDown() || isClosing)
		{
			return;
		}

		if (solutionRoot.isMainSolutionLoaded() && (userUidAfter != null || !solutionRoot.getSolution().requireAuthentication()))

		{
			// no need to load main solution, user already logged in or does not have to log-in
			return;
		}

		if (userUidAfter == null)
		{
			// user logged out, close solution
			closeSolution(true, null);
		}
		else if (!userUidAfter.equals(userUidBefore))
		{
			// user logged in or switched user
			try
			{
				solutionRoot.clearLoginSolution(getActiveSolutionHandler());
			}
			catch (IOException e)
			{
				Debug.error("Error clearing login solution", e);
			}
		}

		// open the solution previously selected or show solution selection
		selectAndOpenSolution();
	}

	public void selectAndOpenSolution()
	{
		if (isShutDown() || isClosing)
		{
			return;
		}

		SolutionMetaData solutionMetaData = null;
		try
		{
			solutionMetaData = solutionRoot.getMainSolutionMetaData();
			if (solutionMetaData == null)
			{
				solutionMetaData = selectSolutionToLoad();
			}
			// else user is logged in, main solution should be loaded now

			if (solutionMetaData != null)
			{
				loadSolution(solutionMetaData);
			}
		}
		catch (RepositoryException e)
		{
			Debug.error("Could not load solution " + (solutionMetaData == null ? "<none>" : solutionMetaData.getName()), e);
			reportError(
				Messages.getString("servoy.client.error.loadingsolution", new Object[] { solutionMetaData == null ? "<none>" : solutionMetaData.getName() }), e);
		}
	}

	protected SolutionMetaData selectSolutionToLoad() throws RepositoryException
	{
		// get a list from the server to choose from
		int solutionTypeFilter = getSolutionTypeFilter();

		if (getPreferedSolutionNameToLoadOnInit() != null)
		{
			try
			{
				SolutionMetaData startSolution = applicationServer.getSolutionDefinition(getPreferedSolutionNameToLoadOnInit(), solutionTypeFilter);
				if (startSolution != null) return startSolution;
			}
			catch (RemoteException e)
			{
				throw new RepositoryException(e);
			}
		}

		SolutionMetaData[] solutions;
		try
		{
			solutions = applicationServer.getSolutionDefinitions(solutionTypeFilter);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		if (solutions == null || solutions.length == 0)
		{
			throw new RuntimeException(Messages.getString("servoy.client.error.opensolution")); //$NON-NLS-1$
		}
		if (solutions.length == 1)
		{
			return solutions[0];
		}

		// show a dialog
		return showSolutionSelection(solutions);
	}

	abstract protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException;

	abstract protected SolutionMetaData showSolutionSelection(SolutionMetaData[] solutions);

	protected int getSolutionTypeFilter()
	{
		return SolutionMetaData.SOLUTION;
	}

	public Object authenticate(String authenticator_solution, String method, Object[] credentials)
	{
		String jscredentials;
		try
		{
			JSONConverter jsonConverter = new JSONConverter();
			jscredentials = jsonConverter.convertToJSON(credentials);
			String jsReturn = authenticate(new Credentials(clientInfo.getClientId(), authenticator_solution, method, jscredentials));
			return jsonConverter.convertFromJSON(jsReturn);
		}
		catch (Exception e)
		{
			Debug.error("Could not convert credentials object to json", e); //$NON-NLS-1$
			return null;
		}
	}

	public String authenticate(Credentials credentials) throws RepositoryException
	{
		try
		{
			ClientLogin login = applicationServer.login(credentials);
			if (login == null)
			{
				clientInfo.setUserUid(null);
				return null;
			}
			clientInfo.setClientId(login.getClientId());

			clientInfo.setUserName(login.getUserName());
			clientInfo.setUserUid(login.getUserUid());
			clientInfo.setUserGroups(login.getUserGroups());
			if (login.getUserUid() == null)
			{
				clientInfo.setLastAuthentication(null, null, null);
			}
			else
			{
				// keep last successful authentication result for reconnect
				clientInfo.setLastAuthentication(credentials.getAuthenticatorType(), credentials.getMethod(), credentials.getJscredentials());
				loggedIn();
			}

			return login.getJsReturn();
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	protected void loggedIn()
	{
		// do nothing here
	}

	public void logout(@SuppressWarnings("unused") Object[] solution_to_open_args)
	{
		String userUid = null;
		try
		{
			if (clientInfo.getClientId() != null)
			{
				userUid = clientInfo.getUserUid();
				try
				{
					IApplicationServerAccess asa = getApplicationServerAccess();
					if (asa != null)
					{
						asa.logout(clientInfo.getClientId());
					}
					// else not logged in
				}
				catch (Exception e)
				{
					Debug.error("Error during logout", e); //$NON-NLS-1$
				}
			}
		}
		finally
		{
			clientInfo.clearUserInfo();
			dataServer = null;
			repository = null;
			userManager = null;
			applicationServerAccess = null; // will be recreated at new login
		}

		handleClientUserUidChanged(userUid, null);
	}


	protected boolean registerClient(IUserClient uc) throws Exception
	{
		String prevClientId = clientInfo.getClientId();
		long t1 = System.currentTimeMillis();
		int counter = 0;
		IClientHost mClientHost = null;
		Object[] retval = null;
		while (counter++ < 10)
		{
			try
			{
				mClientHost = getClientHost();
				if (mClientHost != null)
				{
					retval = mClientHost.register(uc);
				}
				break;
			}
			catch (Exception e)
			{
				if (counter == 10)
				{
					if (e instanceof RemoteException)
					{
						throw (RemoteException)e;
					}
					if (e instanceof RuntimeException)
					{
						throw (RuntimeException)e;
					}
					throw new RuntimeException(e.getMessage());
				}

				try
				{
					Thread.sleep(100 * counter);
				}
				catch (InterruptedException e1)
				{
				}
			}
		}
		boolean registered = false;
		if (retval != null)
		{
			registered = ((Integer)retval[1]).intValue() == IClientManager.REGISTER_OK;
			clientInfo.setClientId((String)retval[0]);
			if (Debug.tracing())
			{
				Debug.trace("Client (re)registered with id " + clientInfo.getClientId() + ", previous: " + prevClientId); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		if (clientInfo.getClientId() == null)
		{
			if (mClientHost == null)
			{
				throw new ApplicationException(ServoyException.InternalCodes.INVALID_RMI_SERVER_CONNECTION);
			}
			else
			{
				if (retval != null && ((Integer)retval[1]).intValue() == IClientManager.REGISTER_FAILED_MAINTENANCE_MODE) throw new ApplicationException(
					ServoyException.MAINTENANCE_MODE);
				else throw new ApplicationException(ServoyException.NO_LICENSE);
			}
		}
		else
		{
			clientInfo.setLoginTimestamp(System.currentTimeMillis());
		}
		long t2 = System.currentTimeMillis();
		Debug.trace("Leave registerClient registered:" + registered + " in " + (t2 - t1) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return registered;
	}

	protected abstract void unBindUserClient() throws Exception;

	public Properties getSettings()
	{
		return settings;
	}

	private final Map<Object, Object> runtimeProperties = new HashMap<Object, Object>();

	public Map<Object, Object> getRuntimeProperties()
	{
		return runtimeProperties;
	}

	public Solution getSolution()
	{
		return solutionRoot.getSolution();
	}

	public String getSolutionName()
	{
		// return name of main solution, also when user is in login solution
		return solutionRoot.getMainSolutionMetaData().getName();
	}

	/**
	 * @see com.servoy.j2db.IServiceProvider#getFlattenedSolution()
	 */
	public FlattenedSolution getFlattenedSolution()
	{
		return solutionRoot;
	}

	public IApplicationServer getApplicationServer()
	{
		return applicationServer;
	}

	@SuppressWarnings("nls")
	public IRepository getRepository()
	{
		if (repository == null)
		{
			try
			{
				repository = createRepository();
				if (repository != null) J2DBGlobals.firePropertyChange(this, "repository", null, repository);
			}
			catch (Exception ex)
			{
				reportError("Cannot find repository, it may not be running on server", ex);
			}
		}
		return repository;
	}

	protected IRepository createRepository() throws RemoteException
	{
		IApplicationServerAccess asa = getApplicationServerAccess();
		if (asa == null)
		{
			return null;
		}
		return asa.getRepository();
	}

	public final IDataServer getDataServer()
	{
		if (dataServer == null)
		{
			try
			{
				if (dataServer == null)
				{
					dataServer = createDataServer();
				}
			}
			catch (Exception ex)
			{
				reportError("Cannot find dataservice, it may not be running on server", ex); //$NON-NLS-1$ 
			}
		}
		return dataServer;
	}

	public IClientHost getClientHost()
	{
		if (clientHost == null)
		{
			try
			{
				if (clientHost == null)
				{
					clientHost = createClientHost();
				}
			}
			catch (Exception ex)
			{
				reportError("Cannot find client host, it may not be running on server", ex); //$NON-NLS-1$ 
			}
		}
		return clientHost;
	}

	public IApplicationServerAccess getApplicationServerAccess()
	{
		if (applicationServerAccess == null && getClientInfo().getClientId() != null)
		{
			applicationServerAccess = createApplicationServerAccess();
		}
		return applicationServerAccess;
	}

	public boolean haveRepositoryAccess()
	{
		return getRepository() != null;
	}


	protected IApplicationServerAccess createApplicationServerAccess()
	{
		if (getClientInfo().getClientId() != null)
		{
			try
			{
				return applicationServer.getApplicationServerAccess(getClientInfo().getClientId());
			}
			catch (RemoteException e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	protected IDataServer createDataServer()
	{
		try
		{
			IApplicationServerAccess asa = getApplicationServerAccess();
			if (asa != null)
			{
				return asa.getDataServer();
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
			reportError(getI18NMessage("servoy.client.error.finding.repository"), e); //$NON-NLS-1$
		}
		return null;
	}

	protected IClientHost createClientHost()
	{
		try
		{
			IApplicationServer as = getApplicationServer();
			if (as != null)
			{
				return as.getClientHost();
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public Remote getServerService(String name)
	{
		try
		{
			return applicationServer.getRegisteredService(name);
		}
		catch (RemoteException e)
		{
			Debug.error("Error getting the service " + name, e); //$NON-NLS-1$ 
		}
		return null;
	}


	public IExecutingEnviroment getScriptEngine()
	{
		if (scriptEngine == null && solutionRoot.getSolution() != null)
		{
			synchronized (this)
			{
				if (scriptEngine == null && solutionRoot.getSolution() != null)
				{
					scriptEngine = createScriptEngine();
					scriptEngine.getGlobalScope().createVars();
				}
			}
		}
		return scriptEngine;
	}

	protected abstract IExecutingEnviroment createScriptEngine();

	public IModeManager getModeManager()
	{
		return modeManager;
	}

	protected abstract IModeManager createModeManager();

	public IFormManager getFormManager()
	{
		return formManager;
	}

	protected abstract IFormManager createFormManager();

	public IFoundSetManagerInternal getFoundSetManager()
	{
		if (foundSetManager == null)
		{
			synchronized (this)
			{
				if (foundSetManager == null)
				{
					createFoundSetManager();
				}
			}
		}
		return foundSetManager;
	}

	protected abstract void createFoundSetManager();


	public String getClientID()
	{
		if (clientInfo == null)
		{
			return null;
		}
		return clientInfo.getClientId();
	}

	public String getUserUID()
	{
		if (clientInfo == null)
		{
			return null;
		}
		return clientInfo.getUserUid();
	}

	public String getUserName()
	{
		if (clientInfo == null)
		{
			return null;
		}
		return clientInfo.getUserName();
	}

	protected String currentWindowName = null;

	public String getCurrentWindowName()
	{
		return currentWindowName;
	}

	public void setCurrentWindowName(String currentWindowName)
	{
		String oldName = this.currentWindowName;
		this.currentWindowName = currentWindowName;
		J2DBGlobals.firePropertyChange(this, "currentWindow", oldName, currentWindowName); //$NON-NLS-1$
	}

	public void reportError(String msg, Object detail)
	{
		Debug.error(msg);
		Debug.error(detail);
	}

	public void reportJSError(String msg, Object detail)
	{
		Debug.error(msg);
		Debug.error(detail);
	}

	public void reportWarning(String s)
	{
		Debug.log(s);
	}

	@Deprecated
	public abstract ITaskExecuter getThreadPool();


	public abstract ScheduledExecutorService getScheduledExecutor();

	public abstract boolean isRunningRemote();

	public abstract URL getServerURL();

	protected transient IUserClient userClient;

	protected void createUserClient()
	{
		userClient = new ClientStub(this);
	}

	private ClientInfo clientInfo;

	private transient boolean isShutdown;

	public ClientInfo getClientInfo()
	{
		return clientInfo;
	}

	public void addClientInfo(String info)
	{
		clientInfo.addInfo(info);
		// try to push the new client info
		try
		{
			getClientHost().pushClientInfo(clientInfo.getClientId(), clientInfo);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public void removeAllClientInfo()
	{
		clientInfo.removeAllInfo();
		// try to push the change
		try
		{
			getClientHost().pushClientInfo(clientInfo.getClientId(), clientInfo);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public boolean isShutDown()
	{
		return isShutdown;
	}

	public void shutDown(boolean force)
	{
		Debug.trace("shutDown"); //$NON-NLS-1$		
		try
		{
			if (solutionRoot.getSolution() != null)
			{
				// shutdown should not try to reopen preferred solution
				if (!closeSolution(force, new String[] { }) && !force) return;
//				solutionRoot.setSolution(null);
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		isShutdown = true;

		unRegisterListeners();

		try
		{
			if (pluginManager != null)
			{
				pluginManager.flushCachedItems();
				pluginManager = null;
			}
		}
		catch (Exception e1)
		{
			Debug.error("Error flushing plugins"); //$NON-NLS-1$
		}
		try
		{
			if (formManager != null)
			{
				formManager.flushCachedItems();
				formManager = null;
			}
		}
		catch (Exception e1)
		{
			Debug.error(e1);
		}
		try
		{
			if (foundSetManager != null)
			{
				foundSetManager.flushCachedItems();
				foundSetManager = null;
			}
		}
		catch (Exception e1)
		{
			Debug.error(e1);
		}

		saveSettings();

		//de register myself
		try
		{
			if (clientInfo != null)
			{
				unRegisterClient(clientInfo.getClientId());
				unBindUserClient();
				clientInfo = null;
			}
		}
		catch (Exception e)
		{
			Debug.error(e);// incase server is dead
		}
	}

	protected abstract void saveSettings();

	protected transient boolean isClosing = false;

	public boolean closeSolution(boolean force, Object[] args)
	{
		if (solutionRoot.getSolution() == null || isClosing) return true;
		try
		{
			isClosing = true;
			String[] s_args = null;
			// we dont want to open anything again if this was a force close
			if (!force && args != null)
			{
				s_args = new String[args.length];
				for (int i = 0; i < args.length; i++)
				{
					s_args[i] = (args[i] != null ? args[i].toString() : null);
				}
			}
			else if (!force && args == null && getPreferedSolutionNameToLoadOnInit() != null)
			{
				s_args = new String[] { getPreferedSolutionNameToLoadOnInit() };
			}

			boolean autoSaveBlocked = false;
			if (foundSetManager != null && foundSetManager.getEditRecordList().isEditing())
			{
				// close solution is not a javaScript stop (do not save edited records if autoSave is off)
				int stopEditing = foundSetManager.getEditRecordList().stopEditing(false);
				if (stopEditing != ISaveConstants.AUTO_SAVE_BLOCKED || foundSetManager.getEditRecordList().getAutoSave() == true)
				{
					// so the stopEditing was not blocked because autoSave is off
					if (stopEditing != ISaveConstants.STOPPED)
					{
						if (force)
						{
							// just clean everything when in force mode
							foundSetManager.getEditRecordList().init();
						}
						else
						{
							return false;
						}
					}
				}
				else
				{
					// stopEditing was blocked because autoSave is off; this means unsaved changes will not be saved automatically
					// when this solution closes; however we must give the user the opportunity to save unsaved data on his solution close
					// handler - so we will clear edited records only after that method is called - and if the closing of the solution continues
					autoSaveBlocked = true;
				}
			}

			handleArguments(s_args);
			if (!callCloseSolutionMethod(force) && !force)
			{
				return false;
			}

			if (autoSaveBlocked && foundSetManager != null)
			{
				// clear edited records so that they will not be auto-saved by the operations that follow
				foundSetManager.getEditRecordList().init();
			}

			checkForActiveTransactions(force);

			// formmanager does a savedata on visible form
			J2DBGlobals.firePropertyChange(this, "solution", solutionRoot.getSolution(), null); //$NON-NLS-1$
			solutionRoot.clearSecurityAccess();
			saveSolution();// do save after firePropertyChange because that may flush some changes (from undoable cmds)

			try
			{
				solutionRoot.close(getActiveSolutionHandler());
			}
			catch (Exception e)
			{
				// ignore any error
				Debug.trace(e);
			}

			// Notify server!
			IClientHost ch = getClientHost();
			if (ch != null)// can be null if failed to init
			{
				try
				{
					if (clientInfo != null)
					{
						clientInfo.setOpenSolutionId(-1);
						ch.pushClientInfo(clientInfo.getClientId(), clientInfo);
					}
				}
				catch (Exception e1)
				{
					Debug.error(e1);// incase connection to server is dead
				}
			}

			if (foundSetManager != null)
			{
				foundSetManager.flushCachedItems();// delete any foundsets
				foundSetManager.init();
			}
			// inform messages of the closed solution
			refreshI18NMessages();

			if (scriptEngine != null)
			{
				scriptEngine.destroy();
				scriptEngine = null;// delete current script engine
			}
			// drop any temp tables for this client
			IDataServer ds = getDataServer();
			if (ds != null)
			{
				ds.dropTemporaryTable(getClientID(), null, null);
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		finally
		{
			isClosing = false;
			if (dataServer instanceof DataServerProxy) dataServer = ((DataServerProxy)dataServer).getEnclosingDataServer();
		}
		return true;
	}

	/**
	 * Call the on-close0solution method (if defined)
	 * 
	 * @param force passed onto method
	 * @return
	 */
	protected boolean callCloseSolutionMethod(boolean force)
	{
		ScriptMethod sm = null;
		try
		{
			sm = getFlattenedSolution().getScriptMethod(getSolution().getOnCloseMethodID());
		}
		catch (Exception e)
		{
			// ignore any error
			Debug.error(e);
		}

		if (sm != null)
		{
			GlobalScope gscope = getScriptEngine().getSolutionScope().getGlobalScope();
			Function function = gscope.getFunctionByName(sm.getName());
			if (function != null)
			{
				try
				{
					return !Boolean.FALSE.equals(getScriptEngine().executeFunction(
						function,
						gscope,
						gscope,
						Utils.arrayMerge((new Object[] { new Boolean(force) }),
							Utils.parseJSExpressions(getSolution().getInstanceMethodArguments("onCloseMethodID"))), false, false)); //$NON-NLS-1$

				}
				catch (Exception e1)
				{
					reportError(Messages.getString("servoy.client.error.executing.method", new Object[] { sm.getName() }), e1); //$NON-NLS-1$
				}
			}
		}
		return true;
	}

	protected abstract void checkForActiveTransactions(boolean force);

	public abstract boolean saveSolution();

	public IPluginManagerInternal getPluginManager()
	{
		return pluginManager;
	}

	protected abstract void createPluginManager();

	protected transient IPluginAccess pluginAccess;

	public IPluginAccess getPluginAccess()
	{
		return pluginAccess;
	}

	protected abstract void refreshI18NMessages();

	protected void registerListeners()
	{
		J2DBGlobals.addPropertyChangeListener(this, formManager);
		J2DBGlobals.addPropertyChangeListener(modeManager, formManager);
	}

	protected void unRegisterListeners()
	{
		J2DBGlobals.removeAllPropertyChangeListeners(this);
		J2DBGlobals.removeAllPropertyChangeListeners(modeManager);
	}

	private void writeObject(ObjectOutputStream stream) throws IOException
	{
		//serialize is not implemented
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		//serialize is not implemented
	}

	protected transient volatile IActiveSolutionHandler activeSolutionHandler;

	public IActiveSolutionHandler getActiveSolutionHandler()
	{
		if (activeSolutionHandler == null)
		{
			synchronized (this)
			{
				if (activeSolutionHandler == null)
				{
					activeSolutionHandler = createActiveSolutionHandler();
				}
			}
		}
		return activeSolutionHandler;
	}

	/**
	 * This method is intended to be overridden by clients that access the server over rmi
	 */
	protected IActiveSolutionHandler createActiveSolutionHandler()
	{
		return new LocalActiveSolutionHandler(this);
	}

	public void clearLoginForm()
	{
		try
		{
			// try to push the new client info
			getClientHost().pushClientInfo(clientInfo.getClientId(), clientInfo);

			loadSecuritySettings(solutionRoot);
			getFormManager().clearLoginForm();
		}
		catch (Exception ex)
		{
			Solution s = solutionRoot.getSolution();
			reportError(Messages.getString("servoy.client.error.loadingsolution", new Object[] { (s != null ? s.getName() : "<unknown>") }), ex); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * @param useLoginSolution the useLoginSolution to set
	 */
	public void setUseLoginSolution(boolean useLoginSolution)
	{
		this.useLoginSolution = useLoginSolution;
	}

	public boolean loadSolutionsAndModules(SolutionMetaData solutionMetaData)
	{
		if (solutionRoot.getSolution() != null && !solutionRoot.getSolution().getName().equals(solutionMetaData.getName())) return false; // SHOULD BE NULL!
		try
		{
			if (solutionMetaData != null)
			{
				boolean loadLoginSolution = useLoginSolution && clientInfo.getUserUid() == null;
				solutionRoot.setSolution(solutionMetaData, loadLoginSolution, !loadLoginSolution, getActiveSolutionHandler());// assign only here and not earlier

				if (solutionRoot.getSolution() == null && clientInfo.getUserUid() == null)
				{
					if (haveRepositoryAccess())
					{
						// Have repository access, don't need authorised access
						solutionRoot.setSolution(solutionMetaData, false, true, getActiveSolutionHandler());
						if (solutionMetaData.getMustAuthenticate() && clientInfo.getUserUid() == null && solutionRoot.getSolution() != null &&
							solutionRoot.getSolution().getLoginFormID() == 0)
						{
							// must login the old fashioned way
							showDefaultLogin();
							if (clientInfo.getUserUid() == null)
							{
								return false;
							}
						}
					}
					else
					{
						// no login solution, use default servoy login
						showDefaultLogin();
						if (clientInfo.getUserUid() == null)
						{
							return false;
						}
					}

				}

				if (solutionRoot.getSolution() == null)
				{
					reportError(Messages.getString("servoy.client.error.loadingsolution", new Object[] { (solutionMetaData.getName()) }), null); //$NON-NLS-1$ 
					return false;
				}

				solutionLoaded(getSolution());
			}
			return true;
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			reportError(Messages.getString(
				"servoy.client.error.loadingsolution", new Object[] { (solutionMetaData != null ? solutionMetaData.getName() : "<unknown>") }), ex); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
	}


	protected abstract void showDefaultLogin() throws ServoyException;

	public void loadSecuritySettings(FlattenedSolution root) throws ServoyException, RemoteException
	{
		if (clientInfo.getUserUid() != null)
		{
			Solution sol = root.getSolution();
			String[] groups = clientInfo.getUserGroups();
			if (groups == null) //fall back on retrieval of groups based on user_uid
			{
				groups = getUserManager().getUserGroups(clientInfo.getClientId(), clientInfo.getUserUid());
			}
			root.clearSecurityAccess();

			int[] sids = new int[] { sol.getSolutionID() };
			int[] srns = new int[] { sol.getReleaseNumber() };

			Solution[] modules = root.getModules();
			if (modules != null)
			{
				sids = new int[modules.length + 1];
				sids[0] = sol.getSolutionID();
				srns = new int[modules.length + 1];
				srns[0] = sol.getReleaseNumber();
				for (int i = 0; i < modules.length; i++)
				{
					Solution module = modules[i];
					sids[i + 1] = module.getSolutionID();
					srns[i + 1] = module.getReleaseNumber();
				}
			}

			Map<Object, Integer> securityAccess = getUserManager().getSecurityAccess(clientInfo.getClientId(), sids, srns, groups);
			root.addSecurityAccess(securityAccess);

			if (foundSetManager != null)
			{
				((FoundSetManager)foundSetManager).flushSecuritySettings();
			}
		}
	}

	protected void solutionLoaded(Solution s)
	{
		try
		{
			loadSecuritySettings(solutionRoot);

			refreshI18NMessages();

			getScriptEngine().getGlobalScope().reloadVariablesAndScripts(); // add variables for new solution

			// These lines must be before other solutionLoaded call implementations, because a long running process 
			// (solution startup method) will never update the status.
			getClientInfo().setOpenSolutionId(s.getSolutionMetaData().getRootObjectId());
			getClientInfo().setOpenSolutionTimestamp(System.currentTimeMillis());
			getClientHost().pushClientInfo(getClientInfo().getClientId(), getClientInfo());
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
	}

	//server-to-desktop activation
	public abstract void activateSolutionMethod(String globalMethodName, StartupArgumentsScope argumentsScope);

	public DataServerProxy proxyDataServer()
	{
		IDataServer ds = getDataServer();
		if (ds != null && !(ds instanceof DataServerProxy))
		{
			dataServer = new DataServerProxy(ds);
			ds = dataServer;
		}
		return (DataServerProxy)ds;
	}

	private transient boolean isHandlingError = false;

	public void handleException(String servoyMsg, final Exception e)
	{
		// Ignore the ExitScriptException completely.
		if (e instanceof ExitScriptException || e.getCause() instanceof ExitScriptException ||
			(e instanceof JavaScriptException && ((JavaScriptException)e).getValue() instanceof ExitScriptException)) return;

		// If the given message is null then let it be the localized message of the deepest cause (the real cause)
		String msg = servoyMsg;
		if (msg == null)
		{
			Throwable t = e;
			while (t.getCause() != null)
			{
				t = t.getCause();
			}
			msg = t.getLocalizedMessage();
		}

		Solution s = getSolution();
		if (s != null)// && (e instanceof ApplicationException || e instanceof DataException || e instanceof JavaScriptException))
		{
			Exception scriptException = e;
			//verify whether e is not caused by a ServoyException (at runtime, exceptions thrown are wrapped in WrappedException, 
			// so we need to look for a ServoyException into the chain)
			// first check for a javascript exception with its value
			if (scriptException instanceof JavaScriptException && ((JavaScriptException)scriptException).getValue() instanceof Exception)
			{
				scriptException = (Exception)((JavaScriptException)scriptException).getValue();
			}
			// then check if it is RhinoException and skip that one by default.
			else if (scriptException instanceof RhinoException && scriptException.getCause() instanceof Exception)
			{
				scriptException = (Exception)scriptException.getCause();
			}
			// Now search for a ServoyException in the chain.
			Throwable cause = scriptException;
			while (cause != null && !(cause instanceof ServoyException))
			{
				cause = cause.getCause();
				if (cause instanceof ServoyException)
				{
					scriptException = (ServoyException)cause;
				}
			}

			if (!testClientRegistered(scriptException))
			{
				return;
			}
			ScriptMethod sm = null;
			int mid = s.getOnErrorMethodID();
			if (mid > 0)
			{
				sm = getFlattenedSolution().getScriptMethod(mid);
			}

			if (sm == null || isHandlingError)//check for error handler, or when a error ocurs in error handler
			{
				reportError(msg, e);
			}
			else
			{
				try
				{
					isHandlingError = true;
					GlobalScope gscope = getScriptEngine().getSolutionScope().getGlobalScope();
					Object function = gscope.get(sm.getName());
					if (function instanceof Function)
					{
						Object retval = getScriptEngine().executeFunction(((Function)function), gscope, gscope,
							Utils.arrayMerge((new Object[] { scriptException }), Utils.parseJSExpressions(s.getInstanceMethodArguments("onErrorMethodID"))), //$NON-NLS-1$
							false, false);
						if (Utils.getAsBoolean(retval))
						{
							reportError(msg, e);//error handler cannot handle this error
						}
					}
				}
				catch (Exception e1)
				{
					reportError(msg, e);
				}
				finally
				{
					isHandlingError = false;
				}
			}
		}
		else
		//no solution
		{
			reportError(msg, e);
		}
	}

	/**
	 * @param exception The exception that should be tested.
	 * @return true if the client is still just registered, false if the exception reports an unregistered client
	 * 
	 */
	protected boolean testClientRegistered(Object exception)
	{
		return true;
	}

	//returns url,w,h,time_ms
	public Object[] getAdInfo()
	{
		Object[] retval = new Object[4];
		try
		{
			URL url = new URL("http://www.servoy.com/client/ad"); //$NON-NLS-1$
			InputStream is = url.openConnection().getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			Utils.closeReader(br);
			String[] size_array = line.split(","); //$NON-NLS-1$
			retval[0] = url;
			retval[1] = Utils.findNumber(size_array[0]);
			retval[2] = Utils.findNumber(size_array[1]);
			retval[3] = Utils.findNumber(size_array[2]);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return retval;
	}

	public boolean isInDeveloper()
	{
		return false;
	}

	public abstract void blockGUI(String reason);

	public abstract void releaseGUI();

	public void invokeLater(Runnable r, boolean immediate)
	{
		invokeLater(r);
	}
}
