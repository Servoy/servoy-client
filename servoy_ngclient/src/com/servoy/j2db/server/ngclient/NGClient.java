package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpSession;

import org.apache.wicket.util.string.AppendingStringBuffer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.WebComponent;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.WebsocketSessionManager;
import org.sablo.websocket.impl.ClientService;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.PluginScope;
import com.servoy.j2db.scripting.info.NGCONSTANTS;
import com.servoy.j2db.server.headlessclient.AbstractApplication;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet.MediaInfo;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.scripting.WebServiceFunction;
import com.servoy.j2db.server.ngclient.scripting.WebServiceScriptable;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IPerfomanceRegistry;
import com.servoy.j2db.server.shared.PerformanceData;
import com.servoy.j2db.server.shared.PerformanceTiming;
import com.servoy.j2db.server.shared.PerformanceTimingAggregate;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Ad;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IGetLastAccessed;
import com.servoy.j2db.util.IGetStatusLine;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

// TODO we should add a subclass between ClientState and SessionClient, (remove all "session" and wicket related stuff out of SessionClient)
// then we can extend that one.
@SuppressWarnings("nls")
public class NGClient extends AbstractApplication implements INGApplication, IChangeListener, IServerService, IGetStatusLine, IGetLastAccessed
{
	private static final long serialVersionUID = 1L;

	public static final String APPLICATION_SERVICE = "$applicationService";
	public static final String APPLICATION_SERVER_SERVICE = "applicationServerService";
	public static final String HTTP_SESSION_COUNTER = "httpSessionCounter";

	private final INGClientWebsocketSession wsSession;

	private transient volatile ServoyScheduledExecutor scheduledExecutorService;

	private volatile NGRuntimeWindowManager runtimeWindowManager;

	private Map<Object, Object> uiProperties;

	private final Map<String, String> overrideStyleSheets = new HashMap<String, String>();

	private HashMap<String, String> properties = null;

	private final IPerfomanceRegistry perfRegistry;

	private boolean registered = false;

	private volatile long lastAccessed;

	private URL serverURL;

	public NGClient(INGClientWebsocketSession wsSession) throws Exception
	{
		super(new WebCredentials());

		this.wsSession = wsSession;
		getWebsocketSession().registerServerService(APPLICATION_SERVER_SERVICE, this);
		getWebsocketSession().registerServerService(I18NService.NAME, new I18NService(this));
		getWebsocketSession().registerServerService(ClientDesignService.NAME, new ClientDesignService(this));
		getClientInfo().setApplicationType(getApplicationType());
		applicationSetup();
		applicationInit();
		applicationServerInit();
		IPerfomanceRegistry registry = (getApplicationServerAccess() != null ? getApplicationServerAccess().getFunctionPerfomanceRegistry() : null);
		if (registry == null)
		{
			registry = new DummyPerformanceRegistry();
		}
		perfRegistry = registry;
	}

	@Override
	public void selectAndOpenSolution()
	{
		try
		{
			try
			{
				getWebsocketSession().getClientService("$sabloLoadingIndicator").executeServiceCall("showLoading", null);
			}
			catch (IOException e)
			{
			}
			super.selectAndOpenSolution();
		}
		finally
		{
			try
			{
				getWebsocketSession().getClientService("$sabloLoadingIndicator").executeServiceCall("hideLoading", null);
			}
			catch (IOException e)
			{
			}
		}
	}

	@Override
	protected SolutionMetaData selectSolutionToLoad() throws RepositoryException
	{
		// don't return here the current solution, that should only be loaded really through
		// a request == websocket endpoint
		return null;
	}

	@Override
	public void reportInfo(final String message)
	{
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("showMessage",
						new Object[] { Utils.stringReplace(message, "\r", "") });
				}
				catch (IOException e)
				{
					Debug.error("Error sending message to client", e);
				}
			}
		};
		// make sure we report this on all windows.
		if (CurrentWindow.exists() && CurrentWindow.get() instanceof WebsocketSessionWindows)
		{
			runnable.run();
		}
		else
		{
			CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows(getWebsocketSession()), runnable);
		}
	}

	@Override
	public void overrideStyleSheet(String oldStyleSheet, String newStyleSheet)
	{
		overrideStyleSheets.put(oldStyleSheet, newStyleSheet);
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				getWebsocketSession().sendStyleSheet();
			}
		};
		// make sure we report this on all windows.
		if (CurrentWindow.exists() && CurrentWindow.get() instanceof WebsocketSessionWindows)
		{
			runnable.run();
		}
		else
		{
			CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows(getWebsocketSession()), runnable);
		}
	}

	/**
	 * @return the styleSheet
	 */
	public Map<String, String> getOverrideStyleSheets()
	{
		return overrideStyleSheets;
	}

	@Override
	public void setLocale(Locale l)
	{
		boolean send = locale != null && !locale.equals(l);
		super.setLocale(l);
		if (send && !("".equals(l.getLanguage()) && "".equals(l.getCountry())))
			getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setLocale",
				new Object[] { l.getLanguage(), l.getCountry() });
		// flush the valuelist cache so that all valuelist are recreated with the new locale keys
		Map< ? , ? > cachedValueList = (Map< ? , ? >)getRuntimeProperties().get(IServiceProvider.RT_VALUELIST_CACHE);
		if (cachedValueList != null) cachedValueList.clear();
		List<IFormController> allControllers = getFormManager().getCachedFormControllers();
		for (IFormController fc : allControllers)
		{
			IWebFormUI formUI = (IWebFormUI)fc.getFormUI();
			Collection<WebComponent> components = formUI.getComponents();
			for (WebComponent component : components)
			{
				if (component instanceof WebFormComponent) NGUtils.resetI18NProperties((WebFormComponent)component, component.getSpecification());
			}
		}
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		IExecutingEnviroment scriptEngine = super.createScriptEngine();
		WebObjectSpecification[] serviceSpecifications = WebServiceSpecProvider.getSpecProviderState().getAllWebComponentSpecifications();
		PluginScope scope = (PluginScope)scriptEngine.getSolutionScope().get("plugins", scriptEngine.getSolutionScope());
		scope.setLocked(false);
		for (WebObjectSpecification serviceSpecification : serviceSpecifications)
		{
			if (serviceSpecification.getApiFunctions().size() != 0 || serviceSpecification.getAllPropertiesNames().size() != 0)
			{
				scope.put(ClientService.convertToJSName(serviceSpecification.getName()), scope,
					new WebServiceScriptable(this, serviceSpecification, scriptEngine.getSolutionScope()));
			}
		}
		scope.setLocked(true);
		return scriptEngine;
	}


	@Override
	public Locale getLocale()
	{
		if (locale == null) initFromClientBrowserinformation();
		return super.getLocale();
	}

	@Override
	public TimeZone getTimeZone()
	{
		if (timeZone == null) initFromClientBrowserinformation();
		return super.getTimeZone();
	}

	private HashMap<String, String> getUserProperties()
	{
		if (properties != null)
		{
			return properties;
		}
		else
		{
			try
			{
				JSONObject data = (JSONObject)getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("getUserProperties",
					null);

				if (data != null)
				{
					properties = new HashMap<>();

					data.keys().forEachRemaining(key -> {
						properties.put(key, data.getString(key));
					});
				}
				return properties;
			}
			catch (IOException e)
			{
				Debug.error("Error retrieving user properties from client browser ", e);
				return null;
			}


		}
	}

	private void initFromClientBrowserinformation()
	{
		Object retValue;

		try
		{
			retValue = this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("getClientBrowserInformation", null);
		}
		catch (IOException e)
		{
			Debug.warn(e);
			return;
		}
		if (retValue instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)retValue;
			String url = jsonObject.optString("serverURL");
			if (url != null)
			{
				try
				{
					serverURL = new URL(url);
				}
				catch (MalformedURLException e)
				{
					Debug.error(e);
				}
			}
			String userAgent = jsonObject.optString("userAgent");
			if (userAgent != null)
			{
				getClientInfo().addInfo("useragent:" + userAgent);
			}
			String platform = jsonObject.optString("platform");
			if (platform != null)
			{
				getClientInfo().addInfo("platform:" + platform);
			}
			String remote_ipaddress = jsonObject.optString("remote_ipaddress");
			if (remote_ipaddress != null)
			{
				getClientInfo().setHostAddress(remote_ipaddress);
			}
			String remote_host = jsonObject.optString("remote_host");
			if (remote_host != null)
			{
				getClientInfo().setHostName(remote_host);
			}
			if (timeZone == null)
			{
				String utc = jsonObject.optString("utcOffset");
				if (utc != null)
				{
					// apparently it is platform dependent on whether you get the
					// offset in a decimal form or not. This parses the decimal
					// form of the UTC offset, taking into account several
					// possibilities
					// such as getting the format in +2.5 or -1.2

					int dotPos = utc.indexOf('.');
					if (dotPos >= 0)
					{
						String hours = utc.substring(0, dotPos);
						String hourPart = utc.substring(dotPos + 1);

						if (hours.startsWith("+"))
						{
							hours = hours.substring(1);
						}
						int offsetHours = Integer.parseInt(hours);
						int offsetMins = (int)(Double.parseDouble(hourPart) * 6);

						// construct a GMT timezone offset string from the retrieved
						// offset which can be parsed by the TimeZone class.

						AppendingStringBuffer sb = new AppendingStringBuffer("GMT");
						sb.append(offsetHours > 0 ? "+" : "-");
						sb.append(Math.abs(offsetHours));
						sb.append(":");
						if (offsetMins < 10)
						{
							sb.append("0");
						}
						sb.append(offsetMins);
						timeZone = TimeZone.getTimeZone(sb.toString());
					}
					else
					{
						int offset = Integer.parseInt(utc);
						if (offset < 0)
						{
							utc = utc.substring(1);
						}
						timeZone = TimeZone.getTimeZone("GMT" + ((offset > 0) ? "+" : "-") + utc);
					}

					String dstOffset = jsonObject.optString("utcDstOffset");
					if (timeZone != null && dstOffset != null)
					{
						TimeZone dstTimeZone = null;
						dotPos = dstOffset.indexOf('.');
						if (dotPos >= 0)
						{
							String hours = dstOffset.substring(0, dotPos);
							String hourPart = dstOffset.substring(dotPos + 1);

							if (hours.startsWith("+"))
							{
								hours = hours.substring(1);
							}
							int offsetHours = Integer.parseInt(hours);
							int offsetMins = (int)(Double.parseDouble(hourPart) * 6);

							// construct a GMT timezone offset string from the
							// retrieved
							// offset which can be parsed by the TimeZone class.

							AppendingStringBuffer sb = new AppendingStringBuffer("GMT");
							sb.append(offsetHours > 0 ? "+" : "-");
							sb.append(Math.abs(offsetHours));
							sb.append(":");
							if (offsetMins < 10)
							{
								sb.append("0");
							}
							sb.append(offsetMins);
							dstTimeZone = TimeZone.getTimeZone(sb.toString());
						}
						else
						{
							int offset = Integer.parseInt(dstOffset);
							if (offset < 0)
							{
								dstOffset = dstOffset.substring(1);
							}
							dstTimeZone = TimeZone.getTimeZone("GMT" + ((offset > 0) ? "+" : "-") + dstOffset);
						}
						// if the dstTimezone (1 July) has a different offset then
						// the real time zone (1 January) try to combine the 2.
						if (dstTimeZone != null && dstTimeZone.getRawOffset() != timeZone.getRawOffset())
						{
							int dstSaving = dstTimeZone.getRawOffset() - timeZone.getRawOffset();
							String[] availableIDs = TimeZone.getAvailableIDs(timeZone.getRawOffset());
							for (String availableID : availableIDs)
							{
								TimeZone zone = TimeZone.getTimeZone(availableID);
								if (zone.getDSTSavings() == dstSaving)
								{
									// this is a best guess... still the start and end of the DST should
									// be needed to know to be completely correct, or better yet
									// not just the GMT offset but the TimeZone ID should be transfered
									// from the browser.
									timeZone = zone;
									break;
								}
							}
						}
						// if the timezone is really just the default of the server just use that one.
						TimeZone dftZone = TimeZone.getDefault();
						if (timeZone.getRawOffset() == dftZone.getRawOffset() && timeZone.getDSTSavings() == dftZone.getDSTSavings())
						{
							timeZone = dftZone;
						}
					}
				}
			}
			if (locale == null)
			{
				String browserLocale = jsonObject.optString("locale");
				if (browserLocale != null)
				{
					String[] languageAndCountry = browserLocale.split("-");
					if (languageAndCountry.length == 1)
					{
						locale = new Locale(languageAndCountry[0]);
					}
					else if (languageAndCountry.length == 2)
					{
						locale = new Locale(languageAndCountry[0], languageAndCountry[1]);
					}
					getClientInfo().addInfo("locale:" + locale);
				}
			}
		}
		if (timeZone != null)
		{
			getClientInfo().setTimeZone(timeZone);
		}

		getClientInfo().addInfo("session uuid: " + getWebsocketSession().getUuid());

		try
		{
			getClientHost().pushClientInfo(getClientInfo().getClientId(), getClientInfo());
		}
		catch (RemoteException e)
		{
			Debug.error(e);
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

	@Override
	protected int getSolutionTypeFilter()
	{
		return super.getSolutionTypeFilter() | SolutionMetaData.NG_CLIENT_ONLY;
	}

	@Override
	public INGFormManager getFormManager()
	{
		return (INGFormManager)super.getFormManager();
	}

//	public synchronized Map<String, Map<String, Map<String, Object>>> getAllComponentsChanges()
//	{
//		Map<String, Map<String, Map<String, Object>>> changes = new HashMap<>(8);
//		if (isShutDown()) return changes;
//		for (IFormController fc : getFormManager().getCachedFormControllers())
//		{
//			if (fc.isFormVisible())
//			{
//				Map<String, Map<String, Object>> formChanges = ((WebFormUI)fc.getFormUI()).getAllComponentsChanges();
//				if (formChanges.size() > 0)
//				{
//					changes.put(fc.getName(), formChanges);
//				}
//			}
//		}
//		return changes;
//	}

	@Override
	protected void solutionLoaded(Solution s)
	{
		super.solutionLoaded(s);
		getWebsocketSession().solutionLoaded(s);
	}

	@Override
	public INGClientWebsocketSession getWebsocketSession()
	{
		return wsSession;
	}

	@Override
	public void valueChanged()
	{
		getWebsocketSession().valueChanged();
	}

	@Override
	protected void doInvokeLater(Runnable r)
	{
		wsSession.getEventDispatcher().postEvent(r);
	}

	@Override
	public boolean isEventDispatchThread()
	{
		return wsSession.getEventDispatcher().isEventDispatchThread();
	}

	@Override
	public void invokeAndWait(Runnable r)
	{
		if (wsSession.getEventDispatcher().isEventDispatchThread())
		{
			r.run();
		}
		else
		{
			FutureTask<Object> future = new FutureTask<Object>(r, null);
			wsSession.getEventDispatcher().addEvent(future);
			try
			{
				future.get(); // blocking
			}
			catch (InterruptedException e)
			{
				Debug.trace(e);
			}
			catch (ExecutionException e)
			{
				e.getCause().printStackTrace();
				Debug.error(e.getCause());
			}
		}
	}

	@Override
	protected boolean startApplicationServerConnection()
	{
		try
		{
			applicationServer = ApplicationServerRegistry.getService(IApplicationServer.class);
			return true;
		}
		catch (Exception ex)
		{
			reportError(Messages.getString("servoy.client.error.finding.dataservice"), ex); //$NON-NLS-1$
			return false;
		}
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{
		if (loadSolutionsAndModules(solutionMeta))
		{
			J2DBGlobals.firePropertyChange(this, "solution", null, getSolution()); //$NON-NLS-1$
		}
	}

	@Override
	protected boolean callCloseSolutionMethod(final boolean force)
	{
		final boolean[] retValue = new boolean[1];
		Runnable run = new Runnable()
		{
			@Override
			public void run()
			{
				boolean isSet = J2DBGlobals.getServiceProvider() == NGClient.this;
				try
				{
					if (!isSet) J2DBGlobals.setServiceProvider(NGClient.this);
					retValue[0] = doCallCloseSolutionMethod(force);
				}
				finally
				{
					if (!isSet) J2DBGlobals.setServiceProvider(null);
				}
			}
		};
		if (CurrentWindow.exists()) run.run();
		else CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows(getWebsocketSession()), run);
		return retValue[0];
	}

	private boolean doCallCloseSolutionMethod(boolean force)
	{
		boolean canClose = super.callCloseSolutionMethod(force);
		//cleanup here before script engine is destroyed
		if (canClose || force)
		{
			SpecProviderState specProviderState = WebServiceSpecProvider.getSpecProviderState();
			if (specProviderState != null)
			{
				WebObjectSpecification[] serviceSpecifications = specProviderState.getAllWebComponentSpecifications();
				for (WebObjectSpecification serviceSpecification : serviceSpecifications)
				{
					WebObjectFunctionDefinition apiFunction = serviceSpecification.getApiFunction("cleanup");
					if (apiFunction != null && getScriptEngine() != null)
					{
						final PluginScope scope = (PluginScope)getScriptEngine().getSolutionScope().get("plugins", getScriptEngine().getSolutionScope());
						if (scope != null)
						{
							final Scriptable service = (Scriptable)scope.get(serviceSpecification.getScriptingName(), null);
							final Object api = service.get(apiFunction.getName(), null);
							if (api instanceof Function)
							{
								Runnable r = new Runnable()
								{
									@Override
									public void run()
									{
										Context context = Context.enter();
										try
										{
											((Function)api).call(context, scope, service, ScriptRuntime.emptyArgs);
										}
										catch (Exception ex)
										{
											Debug.trace(ex);
										}
										finally
										{
											Context.exit();
										}
									}
								};
								if (api instanceof WebServiceFunction)
								{
									invokeAndWait(r);
								}
								else
								{
									r.run();
								}
							}
						}
					}
				}
			}
		}
		return canClose;
	}

	@Override
	public boolean closeSolution(boolean force, Object[] args)
	{
		String currentSolution = isSolutionLoaded() ? getSolutionName() : null;
		boolean isCloseSolution = super.closeSolution(force, args);
		if (isCloseSolution)
		{
			getRuntimeProperties().put(IServiceProvider.RT_VALUELIST_CACHE, null);

			if (args == null || args.length < 1)
			{
				if (!force && showUrl == null)
				{
					CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows(getWebsocketSession()), new Runnable()
					{
						public void run()
						{
							getWebsocketSession().getClientService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("reload", new Object[0]);
						}
					});
				}
			}
			else
			{
				String openSolution = getPreferedSolutionNameToLoadOnInit();
				if (openSolution == null) openSolution = currentSolution;
				if (openSolution != null)
				{
					String m = getPreferedSolutionMethodNameToCall();
					Object[] a = getPreferedSolutionMethodArguments();

					StringBuilder url = new StringBuilder("solutions/").append(openSolution).append("/index.html");
					if (m != null)
					{
						url.append("?m=").append(m);
						if (a != null && a.length > 0) url.append("&a=").append(a[0]);
					}

					showURL(url.toString(), "_self", null, 0, true);
				}
			}
		}
		return isCloseSolution;
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		return new NGFormManager(this);
	}

	@Override
	public IChangeListener getChangeListener()
	{
		return this;
	}

	@Override
	protected void createFoundSetManager()
	{
		foundSetManager = new NGFoundSetManager(this, new SwingFoundSetFactory());
		foundSetManager.init();
	}

	@Override
	public ScheduledExecutorService getScheduledExecutor()
	{
		if (scheduledExecutorService == null && !isShutDown())
		{
			synchronized (this)
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
							prev = J2DBGlobals.getServiceProvider();
							if (prev != NGClient.this)
							{
								// if this happens it is a webclient in developer..
								// and the provider is not set for this web client. so it must be set.
								J2DBGlobals.setServiceProvider(NGClient.this);
							}
						}

						@Override
						protected void afterExecute(Runnable r, Throwable t)
						{
							super.afterExecute(r, t);
							J2DBGlobals.setServiceProvider(prev);
						}
					};
				}
			}
		}
		return scheduledExecutorService;
	}

	@SuppressWarnings("nls")
	@Override
	public Dimension getScreenSize()
	{
		try
		{
			Object retValue = this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("getScreenSize", null);
			if (retValue instanceof JSONObject)
			{
				int orientation = ((JSONObject)retValue).optInt("orientation", 0);
				int width = ((JSONObject)retValue).optInt("width", -1);
				int height = ((JSONObject)retValue).optInt("height", -1);
				if (orientation == 90 || orientation == -90)
				{
					return new Dimension(height, width);
				}
				return new Dimension(width, height);
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public URL getServerURL()
	{
		if (serverURL == null)
		{
			initFromClientBrowserinformation();
		}
		return serverURL;
	}

	@Override
	public int getApplicationType()
	{
		return IApplication.NG_CLIENT;
	}

	@Override
	public String getClientOSName()
	{
		try
		{
			Object retValue = this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("getUserAgentAndPlatform", null);
			if (retValue instanceof JSONObject)
			{
				String userAgent = ((JSONObject)retValue).optString("userAgent");
				if (userAgent != null)
				{
					if (userAgent.indexOf("NT 6.1") != -1) return "Windows 7";
					if (userAgent.indexOf("NT 6.0") != -1) return "Windows Vista";
					if (userAgent.indexOf("NT 5.1") != -1 || userAgent.indexOf("Windows XP") != -1) return "Windows XP";
					if (userAgent.indexOf("Linux") != -1) return "Linux";
					if (userAgent.indexOf("Mac") != -1) return "Mac OS";
				}
				String platform = ((JSONObject)retValue).optString("platform");
				if (platform != null) return platform;
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return super.getClientOSName();
	}

	@Override
	public int getClientPlatform()
	{
		try
		{
			Object retValue = this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("getUserAgentAndPlatform", null);
			if (retValue instanceof JSONObject)
			{
				String platform = ((JSONObject)retValue).optString("platform");
				if (platform != null)
				{
					return Utils.getPlatform(platform);
				}
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
		return super.getClientPlatform();
	}

	@Override
	public String getApplicationName()
	{
		return "Servoy NGClient";
	}

	@Override
	public boolean putClientProperty(Object name, Object val)
	{
		if (NGCONSTANTS.WINDOW_TIMEOUT.equals(name))
		{
			if (val != null && (!(val instanceof Number) || ((Number)val).longValue() <= 0))
			{
				return false;
			}

			getWebsocketSession().setSessionWindowTimeout(val == null ? null : Long.valueOf(((Number)val).longValue()));
			return true;
		}

		if (val == null || val instanceof Boolean || val instanceof Number || val instanceof String)
		{
			getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setUIProperty", new Object[] { name, val });
		}
		if (uiProperties == null)
		{
			uiProperties = new HashMap<Object, Object>();
		}
		uiProperties.put(name, val);
		return true;
	}

	@Override
	public Object getClientProperty(Object name)
	{
		if (NGCONSTANTS.WINDOW_TIMEOUT.equals(name))
		{
			return Long.valueOf(getWebsocketSession().getWindowTimeout());
		}

		return uiProperties == null ? null : uiProperties.get(name);
	}

	@Override
	public void setTitle(String title)
	{
		getRuntimeWindowManager().getCurrentWindow().setTitle(title);
	}

	@Override
	public ItemFactory getItemFactory()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public IDataRendererFactory getDataRenderFactory()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public RendererParentWrapper getPrintingRendererParent()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public PageFormat getPageFormat()
	{
		// Not used in NGClient
		return null;
	}

	@Override
	public void setPageFormat(PageFormat currentPageFormat)
	{
		// Not used in NGClient
	}

	@Override
	public String getUserProperty(String name)
	{
		String defaultUserProperty = getDefaultUserProperties().get(name);
		if (defaultUserProperty != null) return defaultUserProperty;

		Map<String, String> userProperties = getUserProperties();

		if (userProperties == null)
		{
			return null;
		}

		if (userProperties.containsKey(name))
		{
			return userProperties.get(name);
		}
		else
		{
			return null;
		}

	}


	@Override
	public void setUserProperty(String name, String value)
	{
		getDefaultUserProperties().remove(name);
		Map<String, String> userProperties = getUserProperties();

		if (userProperties == null) return;

		if (value == null)
		{
			userProperties.remove(name);
		}
		else
		{
			userProperties.put(name, value);
		}

		getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setUserProperty", new Object[] { name, value });

	}

	@SuppressWarnings("nls")
	@Override
	public String[] getUserPropertyNames()
	{
		Map<String, String> userProperties = getUserProperties();

		List<String> names = new ArrayList<String>();
		if (userProperties != null)
		{
			userProperties.keySet().forEach(key -> names.add(key));

		}
		for (String defaultUserPropertyKey : getDefaultUserProperties().keySet())
		{
			if (names.indexOf(defaultUserPropertyKey) == -1)
			{
				names.add(defaultUserPropertyKey);
			}
		}
		return names.toArray(new String[0]);

	}

	@Override
	public void looseFocus()
	{
		// TODO call request focus on a div in a client?
	}

	private ShowUrl showUrl = null;

	@Override
	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		String newUrl = url;

		if (target != null && !target.equals("_self") && !target.equals("_top") && url.contains("/solutions/"))
		{
			try
			{
				StringBuilder sb = new StringBuilder(newUrl);
				URL newSolutionUrl = new URL(url);
				if (newSolutionUrl.getQuery() != null)
				{
					sb.append("&").append(IWebsocketEndpoint.CLEAR_SESSION_PARAM).append("=true");
				}
				else sb.append("?").append(IWebsocketEndpoint.CLEAR_SESSION_PARAM).append("=true");
				newUrl = sb.toString();
			}
			catch (MalformedURLException e)
			{
				e.printStackTrace();
			}
		}
		// 2 calls to show url? Just send this one.
		if (showUrl != null)
		{
			this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("showUrl",
				new Object[] { showUrl.url, showUrl.target, showUrl.target_options, Integer.valueOf(showUrl.timeout) });
		}
		showUrl = new ShowUrl(newUrl, target, target_options, timeout, onRootFrame);
		return true;
	}

	@Override
	public void setStatusText(String text, String tooltip)
	{
		this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setStatusText", new Object[] { text });
	}

	@Override
	public NGRuntimeWindowManager getRuntimeWindowManager()
	{
		if (runtimeWindowManager == null)
		{
			synchronized (this)
			{
				if (runtimeWindowManager == null) runtimeWindowManager = new NGRuntimeWindowManager(this);
			}
		}
		return runtimeWindowManager;
	}

	@Override
	public synchronized void shutDown(boolean force)
	{
		super.shutDown(force);
		if (isShutDown())
		{
			if (scheduledExecutorService != null)
			{
				scheduledExecutorService.shutdownNow();
				try
				{
					scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e)
				{
				}
				scheduledExecutorService = null;

			}
			if (httpSession != null)
			{
				try
				{
					AtomicInteger sessionCounter = (AtomicInteger)httpSession.getAttribute(HTTP_SESSION_COUNTER);
					if (sessionCounter.decrementAndGet() == 0)
					{
						httpSession.invalidate();
					}
				}
				catch (Exception ignore)
				{
					// http session can already be invalid..
				}
				httpSession = null;
			}
			if (showUrl == null) getWebsocketSession().sendRedirect(null);
			WebsocketSessionManager.removeSession(getWebsocketSession().getUuid());
		}
	}

	private transient Object[] adsInfo = null;//cache to expensive to get each time

	@Override
	protected boolean registerClient(IUserClient uc) throws Exception
	{
		registered = false;
		try
		{
			registered = super.registerClient(uc);
			ApplicationServerRegistry.get().setServerProcess(getClientID());
		}
		catch (final ApplicationException e)
		{
			((NGClientWebsocketSession)wsSession).setClient(this);
			throw e;
		}
		return registered;
	}

	protected void showInfoPanel()
	{
		if (!registered)
		{
			((NGClientWebsocketSession)wsSession).setClient(this);
			invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					if (adsInfo == null) adsInfo = Ad.getAdInfo();
					final int w = Utils.getAsInteger(adsInfo[1]);
					final int h = Utils.getAsInteger(adsInfo[2]);
					if (w > 50 && h > 50)
					{
						final URL url = (URL)adsInfo[0];
						final int t = Utils.getAsInteger(adsInfo[3]);
						getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("showInfoPanel",
							new Object[] { url.toString(), w, h, t, getI18NMessage("servoy.button.close") });
					}
				}
			});
		}
	}

	@Override
	public void setValueListItems(String name, Object[] displayValues, Object[] realValues, boolean autoconvert)
	{
		ValueList vl = getFlattenedSolution().getValueList(name);
		if (vl != null && vl.getValueListType() == IValueListConstants.CUSTOM_VALUES)
		{
			int guessedType = Types.OTHER;
			if (autoconvert && realValues != null)
			{
				guessedType = guessValuelistType(realValues);
			}
			else if (autoconvert && displayValues != null)
			{
				guessedType = guessValuelistType(displayValues);
			}
			IValueList valueList = com.servoy.j2db.component.ComponentFactory.getRealValueList(this, vl, true, Types.OTHER, null, null);
			if (valueList instanceof CustomValueList)
			{
				((CustomValueList)valueList).setValueType(guessedType);
				((CustomValueList)valueList).fillWithArrayValues(displayValues, realValues);
			}
		}
	}

	@Override
	public void showDefaultLogin() throws ServoyException
	{
		try
		{
			getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("showDefaultLogin", null);
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
	}

	private IMediaUploadCallback mediaUploadCallback;

	public IMediaUploadCallback getMediaUploadCallback()
	{
		return mediaUploadCallback;
	}

	public void showFileOpenDialog(IMediaUploadCallback callback, boolean multiSelect, String acceptFilter, String dialogTitle)
	{
		try
		{
			mediaUploadCallback = callback;
			String key = multiSelect ? "servoy.filechooser.upload.addFiles" : "servoy.filechooser.upload.addFile";
			getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("showFileOpenDialog",
				new Object[] { dialogTitle == null ? getI18NMessage(key) : dialogTitle, Boolean.valueOf(multiSelect), acceptFilter });
		}
		catch (IOException ex)
		{
			Debug.error(ex);
		}
	}

	public String serveResource(String filename, byte[] bs, String mimetype, String contentDisposition)
	{
		MediaInfo mediaInfo = MediaResourcesServlet.createMediaInfo(bs, filename, mimetype, contentDisposition);
		return "resources/" + MediaResourcesServlet.DYNAMIC_DATA_ACCESS + "/" + mediaInfo.getName();
	}

	/*
	 * @see org.sablo.websocket.IServerService#executeMethod(java.lang.String, org.json.JSONObject)
	 */
	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		switch (methodName)
		{
			case "login" :
				try
				{
					credentials.setUserName(args.optString("username"));
					credentials.setPassword(args.optBoolean("encrypted") ? SecuritySupport.decrypt(Settings.getInstance(), args.optString("password"))
						: args.optString("password"));
					authenticate(null, null, new Object[] { credentials.getUserName(), credentials.getPassword() });
					if (getClientInfo().getUserUid() != null)
					{
						wsSession.getEventDispatcher().postEvent(new Runnable()
						{
							public void run()
							{
								try
								{
									loadSolution(getSolution().getName());
								}
								catch (RepositoryException ex)
								{
									Debug.error(ex);
								}
							}
						});
						if (args.optBoolean("remember"))
						{
							JSONObject r = new JSONObject();
							r.put("username", credentials.getUserName());
							r.put("password", SecuritySupport.encrypt(Settings.getInstance(), credentials.getPassword()));
							return r;
						}
						else return Boolean.TRUE;
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
				return Boolean.FALSE;
			case "autosave" :
				getFoundSetManager().getEditRecordList().stopEditing(false);
				break;
			case "callServerSideApi" :
			{
				String serviceScriptingName = args.getString("service");
				PluginScope scope = (PluginScope)getScriptEngine().getSolutionScope().get("plugins", getScriptEngine().getSolutionScope());
				Object service = scope.get(serviceScriptingName, scope);

				if (service instanceof WebServiceScriptable)
				{
					WebServiceScriptable webServiceScriptable = (WebServiceScriptable)service;
					JSONArray methodArguments = args.getJSONArray("args");
					String serviceMethodName = args.getString("methodName");

					// apply browser to sablo java value conversion - using server-side-API definition from the service's spec file if available (otherwise use default conversion)
					// the call to webServiceScriptable.executeScopeFunction will do the java to Rhino one

					// find spec for method
					WebObjectSpecification serviceSpec = webServiceScriptable.getServiceSpecification(); // get specification from plugins scope (which uses getScriptName() of service, then use the getClientService using the real name, to make sure client service is created if needed)
					BaseWebObject serviceWebObject = (BaseWebObject)getWebsocketSession().getClientService(serviceSpec.getName());

					WebObjectFunctionDefinition functionSpec = (serviceSpec != null ? serviceSpec.getInternalApiFunction(serviceMethodName) : null);
					if (functionSpec == null)
					{
						functionSpec = (serviceSpec != null ? serviceSpec.getApiFunction(serviceMethodName) : null);
					}
					List<PropertyDescription> argumentPDs = (functionSpec != null ? functionSpec.getParameters() : null);

					// apply conversion
					Object[] arrayOfJavaConvertedMethodArgs = new Object[methodArguments.length()];
					for (int i = 0; i < methodArguments.length(); i++)
					{
						arrayOfJavaConvertedMethodArgs[i] = JSONUtils.fromJSON(null, methodArguments.get(i),
							(argumentPDs != null && argumentPDs.size() > i) ? argumentPDs.get(i) : null,
							new BrowserConverterContext(serviceWebObject, PushToServerEnum.allow), new ValueReference<Boolean>(false));
					}

					Object retVal = webServiceScriptable.executeScopeFunction(functionSpec, arrayOfJavaConvertedMethodArgs);
					if (functionSpec != null && functionSpec.getReturnType() != null)
					{
						retVal = new TypedData<Object>(retVal, functionSpec.getReturnType()); // this means that when this return value is sent to client it will be converted to browser JSON correctly - if we give it the type
					}
					return retVal;
				}
				else
				{
					Debug.warn("callServerSideApi for unknown service '" + serviceScriptingName + "'");
				}
				break;
			}
		}

		return null;
	}

	@Override
	protected IClientPluginAccess createClientPluginAccess()
	{
		return new NGClientPluginAccessProvider(this);
	}

	@Override
	public void logout(final Object[] solution_to_open_args)
	{
		if (getClientInfo().getUserUid() != null)
		{
			boolean doLogoutAndClearUserInfo = false;
			if (getSolution() != null)
			{
				boolean doLogOut = getClientInfo().getUserUid() != null;
				if (getSolution() != null)
				{
					doLogOut = closeSolution(false, solution_to_open_args);
				}
				doLogoutAndClearUserInfo = doLogOut && getSolution() == null;
			}
			else
			{
				doLogoutAndClearUserInfo = true;
			}

			if (doLogoutAndClearUserInfo)
			{
				if (getApplicationServerAccess() != null && getClientID() != null)
				{
					try
					{
						getApplicationServerAccess().logout(getClientID());
					}
					catch (Exception ex)
					{
						Debug.log("Error during logout", ex);
					}
				}
				credentials.clear();
				getClientInfo().clearUserInfo();

				try
				{
					getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("clearDefaultLoginCredentials", null);
				}
				catch (Exception ex)
				{
					Debug.log("Error calling client side logout", ex);
				}
			}
		}
	}

	@Override
	public void updateUI(int time)
	{
		try
		{
			CurrentWindow.get().sendChanges();
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	private final Set<Pair<Form, String>> toRecreate = new HashSet<>();

	private HttpSession httpSession;

	@Override
	public void recreateForm(Form form, String name)
	{
		toRecreate.add(new Pair<Form, String>(form, name));
	}

	@Override
	public void flushRecreatedForm(Form form, String formName)
	{
		toRecreate.remove(new Pair<Form, String>(form, formName));
	}

	@Override
	public void changesWillBeSend()
	{
		if (toRecreate.size() > 0)
		{
			NGClientWebsocketSessionWindows allWindowsProxy = new NGClientWebsocketSessionWindows(getWebsocketSession());
			for (Pair<Form, String> pair : toRecreate)
			{
				allWindowsProxy.updateForm(pair.getLeft(), pair.getRight(), new FormHTMLAndJSGenerator(this, pair.getLeft(), pair.getRight()));
			}
			toRecreate.clear();
		}

		if (showUrl != null)
		{
			this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("showUrl",
				new Object[] { showUrl.url, showUrl.target, showUrl.target_options, Integer.valueOf(showUrl.timeout) });
			showUrl = null;
		}
	}

	/**
	 * Get a status line to be displayed on the admin page.
	 */
	public String getStatusLine()
	{
		long lastAccessed = getWebsocketSession().getLastAccessed();

		if (lastAccessed == Long.MIN_VALUE)
		{
			// this should normally not happen
			return "No websockets";
		}

		long lastPingTime = getWebsocketSession().getLastPingTime();
		if (lastPingTime > 0)
		{
			// a window is in use, there is a last ping time
			return "Websocket connected, last ping time: " + new SimpleDateFormat("EEE HH:mm:ss").format(new Date(lastPingTime));
		}

		return "Websockets disconnected since " + new SimpleDateFormat("EEE HH:mm:ss").format(new Date(lastAccessed));
	}


	@Override
	public Pair<UUID, UUID> onStartSubAction(String serviceName, String functionName, WebObjectFunctionDefinition apiFunction, Object[] arguments)
	{
		PerformanceData performanceData = perfRegistry.getPerformanceData(getSolutionName());
		if (performanceData != null) return performanceData.startSubAction(serviceName + "." + functionName, System.currentTimeMillis(),
			(apiFunction == null || apiFunction.getBlockEventProcessing()) ? IDataServer.METHOD_CALL : IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT,
			getClientID());
		return null;
	}

	@Override
	public void onStopSubAction(Pair<UUID, UUID> perfId)
	{
		PerformanceData performanceData = perfRegistry.getPerformanceData(getSolutionName());
		if (performanceData != null) performanceData.endSubAction(perfId);

	}

	@Override
	public void updateLastAccessed()
	{
		lastAccessed = System.currentTimeMillis();

	}

	@Override
	public long getLastAccessedTime()
	{
		return lastAccessed;
	}


	/**
	 * @author jcompagner
	 *
	 */
	private static final class DummyPerformanceRegistry implements IPerfomanceRegistry
	{
		@Override
		public void setMaxNumberOfEntriesPerContext(int maxNumberOfEntriesPerContext)
		{
		}

		@Override
		public String[] getPerformanceTimingContexts()
		{
			return null;
		}

		@Override
		public PerformanceTimingAggregate[] getPerformanceTiming(String string)
		{
			return null;
		}

		@Override
		public PerformanceData getPerformanceData(String context)
		{
			return null;
		}

		@Override
		public int getMaxNumberOfEntriesPerContext()
		{
			return 0;
		}

		@Override
		public Date getLastCleared(String context)
		{
			return null;
		}

		@Override
		public String getId()
		{
			return null;
		}

		@Override
		public Map<String, PerformanceTiming[]> getActiveTimings()
		{
			return null;
		}

		@Override
		public void clearPerformanceData(String context)
		{
		}
	}

	private class ShowUrl
	{
		private final String url;
		private final String target;
		private final String target_options;
		private final int timeout;
		private final boolean onRootFrame;

		/**
		 * @param url
		 * @param target
		 * @param target_options
		 * @param timeout
		 * @param onRootFrame
		 */
		public ShowUrl(String url, String target, String target_options, int timeout, boolean onRootFrame)
		{
			this.url = url;
			this.target = target;
			this.target_options = target_options;
			this.timeout = timeout;
			this.onRootFrame = onRootFrame;
		}

	}

	@Override
	public void setTimeZone(TimeZone zone)
	{
		Debug.warn("Setting TimeZone on NG client is not allowed");
	}

	/**
	 * @param httpSession
	 */
	public void setHttpSession(HttpSession httpSession)
	{
		if (this.httpSession == null && httpSession != null)
		{
			this.httpSession = httpSession;
			httpSession.setMaxInactiveInterval(0);
			AtomicInteger sessionCounter;
			synchronized (httpSession)
			{
				sessionCounter = (AtomicInteger)httpSession.getAttribute(HTTP_SESSION_COUNTER);
				if (sessionCounter == null)
				{
					sessionCounter = new AtomicInteger();
					httpSession.setAttribute(HTTP_SESSION_COUNTER, sessionCounter);
				}
			}
			getClientInfo().addInfo("httpsession:" + httpSession.getId());
			sessionCounter.incrementAndGet();
		}
	}
}
