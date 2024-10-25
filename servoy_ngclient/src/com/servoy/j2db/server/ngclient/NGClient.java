package com.servoy.j2db.server.ngclient;

import static com.servoy.j2db.util.UUID.randomUUID;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.WebComponent;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.eventthread.WebsocketSessionWindows;
import org.sablo.specification.IFunctionParameters;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebObjectApiFunctionDefinition;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.WebsocketSessionManager;
import org.sablo.websocket.impl.ClientService;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.ClientStub;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDataRendererFactory;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.IClient;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.SwingFoundSetFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSBlobLoaderBuilder;
import com.servoy.j2db.scripting.PluginScope;
import com.servoy.j2db.scripting.info.NGCONSTANTS;
import com.servoy.j2db.server.headlessclient.AbstractApplication;
import com.servoy.j2db.server.headlessclient.util.HCUtils;
import com.servoy.j2db.server.ngclient.INGClientWindow.IFormHTMLAndJSGenerator;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet.MediaInfo;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.property.BrowserFunction;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
import com.servoy.j2db.server.ngclient.scripting.WebServiceFunction;
import com.servoy.j2db.server.ngclient.scripting.WebServiceScriptable;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IPerformanceDataProvider;
import com.servoy.j2db.server.shared.IPerformanceRegistry;
import com.servoy.j2db.server.shared.PerformanceData;
import com.servoy.j2db.server.shared.PerformanceTiming;
import com.servoy.j2db.server.shared.PerformanceTimingAggregate;
import com.servoy.j2db.server.shared.WebCredentials;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.Ad;
import com.servoy.j2db.util.AppendingStringBuffer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IGetLastAccessed;
import com.servoy.j2db.util.IGetStatusLine;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.RendererParentWrapper;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyScheduledExecutor;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

@SuppressWarnings("nls")
public class NGClient extends AbstractApplication
	implements INGApplication, IChangeListener, IServerService, IGetStatusLine, IGetLastAccessed, IPerformanceDataProvider
{

	private static final long serialVersionUID = 1L;

	public static final String APPLICATION_SERVICE = "$applicationService";
	public static final String APPLICATION_SERVER_SERVICE = "applicationServerService";
	private static final String SABLO_LOADING_INDICATOR = "$sabloLoadingIndicator";
	private static final String CLIENTUTILS_SERVICE = "clientutils";

	private final INGClientWebsocketSession wsSession;

	private transient volatile ServoyScheduledExecutor scheduledExecutorService;

	private volatile NGRuntimeWindowManager runtimeWindowManager;

	private Map<Object, Object> uiProperties;

	private final Map<String, String> overrideStyleSheets = new HashMap<String, String>(3);

	private final Map<String, String> clientFunctions = new HashMap<String, String>(3);

	private IMediaUploadCallback mediaUploadCallback;

	private boolean reloadClientFunctionsSend;

	private HashMap<String, String> properties = null;

	private final Set<Pair<Form, String>> toRecreate = new HashSet<>();

	private PerformanceData performanceData;

	private boolean registered = false;

	private volatile long lastAccessed;

	private URL serverURL;

	private final IDesignerCallback designerCallback;

	public NGClient(INGClientWebsocketSession wsSession, IDesignerCallback designerCallback) throws Exception
	{
		super(new WebCredentials());

		this.designerCallback = designerCallback;
		this.wsSession = wsSession;
		getWebsocketSession().registerServerService(APPLICATION_SERVER_SERVICE, this);
		getWebsocketSession().registerServerService(I18NService.NAME, new I18NService(this));
		getWebsocketSession().registerServerService(ClientDesignService.NAME, new ClientDesignService(this));
		getClientInfo().setApplicationType(getApplicationType());
		applicationSetup();
		applicationInit();
		applicationServerInit();
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
	public void refreshI18NMessages(boolean clearCustomMessages)
	{
		super.refreshI18NMessages(true);
		if (!isClosing)
		{
			refreshI18n();
		}
	}

	@Override
	public void setLocale(Locale l)
	{
		boolean send = locale != null && !locale.equals(l);
		super.setLocale(l);
		if (send)
		{
			refreshI18n();
		}
	}

	private void refreshI18n()
	{
		if (locale == null)
		{
			return;
		}

		if (!("".equals(locale.getLanguage()) && "".equals(locale.getCountry())))
		{
			getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setLocale",
				new Object[] { locale.getLanguage(), locale.getCountry() });
		}

		// flush the valuelist cache so that all valuelist are recreated with the new locale keys
		ComponentFactory.flushAllValueLists(this, false);

		List<IFormController> allControllers = getFormManager().getCachedFormControllers();
		for (IFormController fc : allControllers)
		{
			IWebFormUI formUI = (IWebFormUI)fc.getFormUI();
			Collection<WebComponent> components = formUI.getComponents();
			for (WebComponent component : components)
			{
				if (component instanceof WebFormComponent)
				{
					NGUtils.resetI18NProperties((WebFormComponent)component, component.getSpecification());
				}
			}
		}
	}

	@Override
	protected IExecutingEnviroment createScriptEngine()
	{
		IExecutingEnviroment scriptEngine = super.createScriptEngine();
		WebObjectSpecification[] serviceSpecifications = WebServiceSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
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
		if (designerCallback != null)
		{
			designerCallback.addScriptObjects(null, scriptEngine.getSolutionScope());
		}
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
		ClientInfo clientInfo = getClientInfo();
		if (clientInfo != null)
		{
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
					clientInfo.addInfo("useragent:" + userAgent);
				}
				String platform = jsonObject.optString("platform");
				if (platform != null)
				{
					clientInfo.addInfo("platform:" + platform);
				}
				String remote_ipaddress = jsonObject.optString("remote_ipaddress");
				if (remote_ipaddress != null)
				{
					clientInfo.setHostAddress(remote_ipaddress);
				}
				String remote_host = jsonObject.optString("remote_host");
				if (remote_host != null)
				{
					clientInfo.setHostName(remote_host);
				}
				if (timeZone == null)
				{
					String clientTimeZone = jsonObject.optString("timeZone");
					if (clientTimeZone != null && clientTimeZone.length() > 0)
					{
						timeZone = TimeZone.getTimeZone(clientTimeZone);
					}
					else
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
						clientInfo.addInfo("locale:" + locale);
					}
				}
			}
			if (timeZone != null)
			{
				clientInfo.setTimeZone(timeZone);
			}

			clientInfo.addInfo("session key: " + getWebsocketSession().getSessionKey());

			try
			{
				getClientHost().pushClientInfo(clientInfo.getClientId(), clientInfo);
			}
			catch (RemoteException e)
			{
				Debug.error(e);
			}
		}
	}

	public void loadSolution(String solutionName) throws RepositoryException
	{
		try
		{
			SolutionMetaData solutionMetaData = getApplicationServer().getSolutionDefinition(solutionName, getSolutionTypeFilter());
			if (solutionMetaData == null)
			{
				throw new IllegalArgumentException(
					Messages.getString("servoy.exception.solutionNotFound", new Object[] { solutionName, IApplication.getApplicationTypeAsString(
						getClientInfo().getApplicationType()), SolutionMetaData.getSolutionNamesByFilter(getSolutionTypeFilter()) }));
			}
			loadSolution(solutionMetaData);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	@Override
	public PerformanceData getPerformanceData()
	{
		String solutionName = getSolutionName();
		if (performanceData == null && solutionName != null)
		{
			try
			{
				IPerformanceRegistry registry = (getApplicationServerAccess() != null ? getApplicationServerAccess().getFunctionPerfomanceRegistry() : null);
				if (registry != null)
				{
					performanceData = registry.getPerformanceData(solutionName);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return performanceData;
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
		IEventDispatcher eventDispatcher = wsSession.getEventDispatcher(false);
		return eventDispatcher != null && eventDispatcher.isEventDispatchThread();
	}

	@Override
	public void invokeAndWait(Runnable r)
	{
		try
		{
			invokeAndWait(r, -1);
		}
		catch (TimeoutException e)
		{
			// can be ignored never happens when -1 is passed in.
		}
	}

	public void invokeAndWait(Runnable r, long timeoutInMinutes) throws TimeoutException
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
				if (timeoutInMinutes > 0)
				{
					future.get(timeoutInMinutes, TimeUnit.MINUTES);
				}
				else future.get(); // blocking
			}
			catch (InterruptedException | RuntimeException e) // RuntimeException includes CancellationException as well
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

	protected void runWhileShowingLoadingIndicator(Runnable r)
	{
		IClientService s = getWebsocketSession().getClientService(SABLO_LOADING_INDICATOR);
		s.executeAsyncNowServiceCall("showLoading", null);
		r.run();
		s.executeAsyncNowServiceCall("hideLoading", null);
	}

	@Override
	protected void loadSolution(SolutionMetaData solutionMeta) throws RepositoryException
	{
		performanceData = null; // make sure a new one in started if a new solution is loaded
		runWhileShowingLoadingIndicator(() -> {
			if (loadSolutionsAndModules(solutionMeta))
			{
				J2DBGlobals.firePropertyChange(this, "solution", null, getSolution()); //$NON-NLS-1$
			}
		});
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
				WebObjectSpecification[] serviceSpecifications = specProviderState.getAllWebObjectSpecifications();
				for (WebObjectSpecification serviceSpecification : serviceSpecifications)
				{
					WebObjectFunctionDefinition apiFunction = serviceSpecification.getApiFunction("cleanup");
					if (apiFunction == null)
					{
						apiFunction = serviceSpecification.getInternalApiFunction("cleanup");
					}
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
			getClientFunctions().clear();
			mediaInfos.values().stream().forEach(info -> info.destroy());
			mediaInfos.clear();
			overrideStyleSheets.clear();
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

					}
					if (a != null && a.length > 0 && a[0] != null)
					{
						url.append(m != null ? "&" : "?");
						url.append("a=").append(a[0]);
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
		foundSetManager = new NGFoundSetManager(this, getFoundSetManagerConfig(), new SwingFoundSetFactory());
		foundSetManager.init();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ClientState#createUserClient()
	 */
	@Override
	protected void createUserClient()
	{
		userClient = new ClientStub(this)
		{
			@Override
			public void shutDown()
			{
				// if shutdown from the server, just force interrupt the event thread.
				IEventDispatcher eventDispatcher = getWebsocketSession().getEventDispatcher(false);
				if (eventDispatcher != null) eventDispatcher.interruptEventThread();

				invokeLater(() -> NGClient.this.shutDown(true));

			}
		};
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
					scheduledExecutorService = new ServoyScheduledExecutor(16, 1, "NGClient-Pool-" + getClientID())
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
					return HCUtils
						.getOSName(userAgent);
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

	public Map<String, Object> getClientSideUIProperties()
	{
		Map<String, Object> clientSideUIProperties = new HashMap<String, Object>();
		if (uiProperties != null)
		{
			for (Map.Entry<Object, Object> e : uiProperties.entrySet())
			{
				if (e.getKey() instanceof String && (e.getValue() instanceof String || e.getValue() instanceof Number || e.getValue() instanceof Boolean))
				{
					clientSideUIProperties.put((String)e.getKey(), e.getValue());
				}
			}
		}
		return clientSideUIProperties;
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

		getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setUserProperty",
			new Object[] { name, value });

	}

	@Override
	public void removeUserProperty(String name)
	{
		getDefaultUserProperties().remove(name);
		Map<String, String> userProperties = getUserProperties();

		if (userProperties == null) return;

		userProperties.remove(name);

		getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("removeUserProperty",
			new Object[] { name });

	}

	@Override
	public void removeAllUserProperties()
	{
		Map<String, String> userProperties = getUserProperties();
		List<String> userPropertiesToDelete = new ArrayList<>();

		if (userProperties == null) return;


		for (Map.Entry<String, String> entry : userProperties.entrySet())
		{
			userPropertiesToDelete.add(entry.getKey());
		}

		if (userPropertiesToDelete.size() > 0)
		{
			for (String prop : userPropertiesToDelete)
			{
				userProperties.remove(prop);
			}
		}

		getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("removeAllUserProperties",
			new Object[] { userProperties });

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

	private volatile ShowUrl showUrl = null;

	@Override
	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame)
	{
		// 2 calls to show url? Just send this one.
		if (showUrl != null)
		{
			this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("showUrl",
				new Object[] { showUrl.url, showUrl.target, showUrl.target_options, Integer.valueOf(showUrl.timeout) });
		}
		showUrl = new ShowUrl(url, target, target_options, timeout, onRootFrame);
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

	protected static final Logger SHUTDOWNLOGGER = LoggerFactory.getLogger("SHUTDOWNLOGGER"); //$NON-NLS-1$

	@Override
	public synchronized void shutDown(boolean force)
	{
		if (SHUTDOWNLOGGER.isDebugEnabled()) SHUTDOWNLOGGER.debug("In shutdown for client: " + getWebsocketSession().getSessionKey()); //$NON-NLS-1$
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
			if (showUrl == null) getWebsocketSession().sendRedirect(null);
			WebsocketSessionManager.removeSession(getWebsocketSession().getSessionKey());
		}
	}

	private transient Object[] adsInfo = null;//cache to expensive to get each time

	@Override
	protected boolean registerClient(IClient uc) throws Exception
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

	private final ConcurrentMap<String, MediaInfo> mediaInfos = new ConcurrentHashMap<>(3);

	public MediaInfo createMediaInfo(byte[] mediaBytes, String fileName, String contentType, String contentDisposition)
	{
		MediaInfo mediaInfo = new MediaInfo(randomUUID().toString(), fileName, contentType == null ? MimeTypes.getContentType(mediaBytes, null) : contentType,
			contentDisposition, mediaBytes);
		mediaInfos.put(mediaInfo.getName(), mediaInfo);
		return mediaInfo;
	}

	public MediaInfo createMediaInfo(byte[] mediaBytes)
	{
		return createMediaInfo(mediaBytes, null, null, null);
	}

	@Override
	public MediaInfo getMedia(String dynamicID)
	{
		return mediaInfos.get(dynamicID);
	}

	public String serveResource(String filename, byte[] bs, String mimetype, String contentDisposition)
	{
		MediaInfo mediaInfo = createMediaInfo(bs, filename, mimetype, contentDisposition);
		return mediaInfo.getURL(getWebsocketSession().getSessionKey().getClientnr());
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
					credentials.setPassword(args.optBoolean("encrypted") ? SecuritySupport.decrypt(args.optString("password"))
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
							r.put("password", SecuritySupport.encrypt(credentials.getPassword()));
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
				IFoundSetManagerInternal fsm = getFoundSetManager();
				if (fsm != null)
				{
					fsm.getEditRecordList().stopEditing(false);
				}
				else
				{
					Debug.warn("autosave with no foundSetManager.");
				}
				break;

			case "callServerSideApi" :
			{
				if (getScriptEngine() == null)
				{
					return null;
				}
				String serviceScriptingName = args.getString("service");
				PluginScope scope = (PluginScope)getScriptEngine().getSolutionScope().get("plugins", getScriptEngine().getSolutionScope());
				Object service = scope.get(serviceScriptingName, scope);

				if (service instanceof WebServiceScriptable webServiceScriptable)
				{
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
						Debug.warn("callServerSideApi for unknown function '" + serviceMethodName + "' of service '" + serviceScriptingName + "'.");
						throw new RuntimeException("trying to call a function '" + serviceMethodName + "' that does not exist in .spec of service: " +
							serviceSpec.getName());
					}
					else
					{
						IFunctionParameters argumentPDs = (functionSpec != null ? functionSpec.getParameters() : null);

						// apply conversion
						Object[] arrayOfJavaConvertedMethodArgs = new Object[methodArguments.length()];
						for (int i = 0; i < methodArguments.length(); i++)
						{
							arrayOfJavaConvertedMethodArgs[i] = JSONUtils.fromJSON(null, methodArguments.get(i),
								(argumentPDs != null && argumentPDs.getDefinedArgsCount() > i) ? argumentPDs.getParameterDefinition(i) : null,
								new BrowserConverterContext(serviceWebObject, PushToServerEnum.allow), new ValueReference<Boolean>(false));
						}

						Object retVal = webServiceScriptable.executeScopeFunction(functionSpec, arrayOfJavaConvertedMethodArgs);
						if (functionSpec != null && functionSpec.getReturnType() != null)
						{
							EmbeddableJSONWriter w = new EmbeddableJSONWriter(true);
							FullValueToJSONConverter.INSTANCE.toJSONValue(w, null, retVal, functionSpec.getReturnType(),
								new BrowserConverterContext(serviceWebObject, PushToServerEnum.reject));
							retVal = w;
						}
						return retVal;
					}
				}
				else
				{
					Debug.warn("callServerSideApi for unknown service '" + serviceScriptingName + "'");
					throw new RuntimeException("callServerSideApi for unknown service '" + serviceScriptingName + "'; called method: " +
						args.getString("methodName"));
				}
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
				HttpSession httpSession = getWebsocketSession().getHttpSession();
				if (httpSession != null)
				{
					httpSession.removeAttribute(StatelessLoginHandler.ID_TOKEN);
				}
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
		reloadClientFunctionsSend = false;
		if (toRecreate.size() > 0)
		{
			NGClientWebsocketSessionWindows allWindowsProxy = new NGClientWebsocketSessionWindows(getWebsocketSession());
			for (Pair<Form, String> pair : toRecreate)
			{
				IFormHTMLAndJSGenerator generator = getWebsocketSession().getFormHTMLAndJSGenerator(pair.getLeft(), pair.getRight());
				allWindowsProxy.updateForm(pair.getLeft(), pair.getRight(), generator);
			}
			toRecreate.clear();
		}

		if (showUrl != null)
		{
			this.getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeAsyncNowServiceCall("showUrl",
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
	public Pair<Long, Long> onStartSubAction(String serviceName, String functionName, WebObjectApiFunctionDefinition apiFunction, Object[] arguments)
	{
		if (performanceData != null) return performanceData.startSubAction(serviceName + "." + functionName, System.currentTimeMillis(),
			(apiFunction == null || apiFunction.getBlockEventProcessing()) ? IDataServer.METHOD_CALL : IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT,
			getClientID(), getSolutionName());
		return null;
	}

	@Override
	public void onStopSubAction(Pair<Long, Long> perfId)
	{
		if (perfId != null)
		{
			if (performanceData != null) performanceData.endSubAction(perfId, getClientID());
		}
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


	@Override
	public String registerClientFunction(String code)
	{
		String uuid = clientFunctions.get(code);
		if (uuid == null)
		{
			uuid = UUID.randomUUID().toString();
			clientFunctions.put(code, uuid);
			if (!reloadClientFunctionsSend)
			{
				reloadClientFunctionsSend = true;
				this.getWebsocketSession().getClientService(NGClientWebsocketSession.CLIENT_FUNCTION_SERVICE).executeAsyncServiceCall("reloadClientFunctions",
					null);
			}
		}
		return uuid;
	}

	@Override
	public Map<String, String> getClientFunctions()
	{
		return this.clientFunctions;
	}

	@Override
	public Object generateBrowserFunction(String functionString)
	{
		return new BrowserFunction(functionString, this);
	}

	@Override
	public JSBlobLoaderBuilder createUrlBlobloaderBuilder(String dataprovider)
	{
		return new JSBlobLoaderBuilder(this, dataprovider, getWebsocketSession().getSessionKey().getClientnr());
	}


	/**
	 * @author jcompagner
	 *
	 */
	private static final class DummyPerformanceRegistry implements IPerformanceRegistry
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

	@Override
	public void setClipboardContent(String content)
	{
		try
		{
			getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("setClipboardContent",
				new Object[] { Utils.stringReplace(content, "\r", "") });
		}
		catch (IOException e)
		{
			Debug.error("Error setting the clipboard content", e);
		}
	}

	@Override
	public String getClipboardContent()
	{
		try
		{
			return (String)getWebsocketSession().getClientService(NGClient.APPLICATION_SERVICE).executeServiceCall("getClipboardContent", null);
		}
		catch (IOException e)
		{
			Debug.error("Error getting the clipboard content", e);
		}

		return null;
	}


	@Override
	public JSONObject getBounds(String webComponentID, String subselector)
	{
		try
		{
			return (JSONObject)this.getWebsocketSession().getClientService(NGClient.CLIENTUTILS_SERVICE).executeServiceCall("getBounds",
				new Object[] { webComponentID, subselector });
		}
		catch (IOException e)
		{
			Debug.error("Error getting component bounds", e);
		}
		return null;
	}

	@Override
	public void setFirstDayOfTheWeek(int weekday)
	{
		putClientProperty(IApplication.FIRST_DAY_OF_WEEK, Integer.valueOf(weekday));
	}

	@Override
	public String getMediaURL(String mediaName)
	{
		return MediaPropertyType.getMediaUrl(mediaName, getFlattenedSolution(), this);
	}

}
