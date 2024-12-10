/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.services.client.TypesRegistryService;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebObjectApiFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecificationBuilder;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IMessageLogger;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.IWindow;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.ngclient.INGClientWindow.IFormHTMLAndJSGenerator;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * Handles a websocket session based on a NGClient.
 *
 * @author rgansevles
 *
 */
public class NGClientWebsocketSession extends BaseWebsocketSession implements INGClientWebsocketSession
{
	public static final String CLIENT_FUNCTION_SERVICE = "clientFunctionService"; //$NON-NLS-1$

	private int clientType = 1;

	IDesignerCallback designerCallback;

	private static final class WindowServiceSpecification extends WebObjectSpecification
	{
		private WindowServiceSpecification()
		{
			super(NGRuntimeWindowManager.WINDOW_SERVICE, "", IPackageReader.WEB_SERVICE, "", null, null, null, null, "", null, null, null);

			WebObjectApiFunctionDefinition createFormController = new WebObjectApiFunctionDefinition("updateController");
			// realFormName, jsTemplate, realUrl, Boolean.valueOf(forceLoad), htmlTemplate
			createFormController
				.addParameter(new PropertyDescriptionBuilder().withName("realFormName").withType(TypesRegistry.getType(StringPropertyType.TYPE_NAME)).build());
			createFormController
				.addParameter(new PropertyDescriptionBuilder().withName("jsTemplate").withType(TypesRegistry.getType(StringPropertyType.TYPE_NAME)).build());
			createFormController
				.addParameter(new PropertyDescriptionBuilder().withName("realUrl").withType(TypesRegistry.getType(StringPropertyType.TYPE_NAME)).build());
			createFormController.addParameter(new PropertyDescriptionBuilder().withName("forceLoad")
				.withType(TypesRegistry.getType(BooleanPropertyType.TYPE_NAME)).withOptional(true).build()); // Titanium client doesn't use this
			createFormController.addParameter(new PropertyDescriptionBuilder().withName("htmlTemplate")
				.withType(TypesRegistry.getType(StringPropertyType.TYPE_NAME)).withOptional(true).build()); // Titanium client doesn't use this //
			// createFormController.setAsync(true); // sync / async for this method is given explicitly by the java code that calls it (it calls either sync method or async method)
			createFormController.setPreDataServiceCall(true); // make sure the client state (FormCache) is created first on client when that message arrives, before any updates for this same form (from potentially the same message) want to apply themselves on that form's state
			addApiFunction(createFormController);

			WebObjectApiFunctionDefinition destroyFormController = new WebObjectApiFunctionDefinition("destroyController");
			destroyFormController
				.addParameter(new PropertyDescriptionBuilder().withName("realFormName").withType(TypesRegistry.getType(StringPropertyType.TYPE_NAME)).build());
			destroyFormController.setAsync(true);
			destroyFormController.setPreDataServiceCall(true);
			addApiFunction(destroyFormController);
		}
	}


	private static final class ClientFunctionsServiceSpecification extends WebObjectSpecification
	{
		private ClientFunctionsServiceSpecification()
		{
			super(CLIENT_FUNCTION_SERVICE, "", IPackageReader.WEB_SERVICE, "", null, null, null, null, "", null, null, null);
			WebObjectApiFunctionDefinition reload = new WebObjectApiFunctionDefinition("reloadClientFunctions");
			reload.setAsync(true);
			reload.setPreDataServiceCall(true);
			addApiFunction(reload);
		}
	}

	private static final ClientFunctionsServiceSpecification CLIENT_FUNCTIONS_SERVICE_SPEC = new ClientFunctionsServiceSpecification();
	private static final WindowServiceSpecification WINDOWS_SERVICE_SPEC = new WindowServiceSpecification();

	private NGClient client;

	public NGClientWebsocketSession(WebsocketSessionKey sessionKey, IDesignerCallback designerCallback)
	{
		super(sessionKey);
		registerClientService(new ServoyClientService(NGRuntimeWindowManager.WINDOW_SERVICE, WINDOWS_SERVICE_SPEC, this, false));
		registerClientService(
			new ServoyClientService(TypesRegistryService.TYPES_REGISTRY_SERVICE, TypesRegistryService.TYPES_REGISTRY_SERVICE_SPEC, this, false));
		registerClientService(new ServoyClientService(CLIENT_FUNCTION_SERVICE, CLIENT_FUNCTIONS_SERVICE_SPEC, this, false));
	}

	@Override
	public void init(Map<String, List<String>> requestParams) throws Exception
	{
		if (client == null)
		{
			setClient(new NGClient(this, designerCallback));
		}
	}

	public void setClient(NGClient client)
	{
		this.client = client;
	}

	public NGClient getClient()
	{
		return client;
	}

	/**
	 * @return the clientType
	 */
	public IFormHTMLAndJSGenerator getFormHTMLAndJSGenerator(Form form, String realFormName)
	{
		if (clientType != 2)
		{
			return new FormHTMLAndJSGenerator(client, form, realFormName);
		}
		else
		{
			return new AngularFormGenerator(client, form, realFormName, false);
		}
	}

	@Override
	public INGClientWindow createWindow(int windowNr, String windowName)
	{
		return new NGClientWindow(this, windowNr, windowName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<INGClientWindow> getWindows()
	{
		return (Collection<INGClientWindow>)super.getWindows();
	}

	@Override
	public boolean isValid()
	{
		return client != null && !client.isShutDown();
	}

	@Override
	protected IEventDispatcher createEventDispatcher()
	{
		return client == null ? null : new NGEventDispatcher(client);
	}

	@Override
	protected String getDispatcherThreadName()
	{
		return super.getDispatcherThreadName() + ", clientid: " + getClient().getClientID(); //$NON-NLS-1$
	}

	@SuppressWarnings("nls")
	@Override
	public void onOpen(final Map<String, List<String>> requestParams)
	{
		super.onOpen(requestParams);
		if (requestParams == null)
		{
			CurrentWindow.get().cancelSession("Solution name is required");
			return;
		}
		if (requestParams.containsKey("clienttype"))
		{
			clientType = Utils.getAsInteger(requestParams.get("clienttype").get(0), 1);
			if (clientType == 2) client.getRuntimeProperties().put("NG2", Boolean.TRUE);
			else client.getRuntimeProperties().remove("NG2");
		}
		else
		{
			clientType = 1;
			client.getRuntimeProperties().remove("NG2");
		}

		lastSentStyleSheets = null;

		final StartupArguments args = new StartupArguments(requestParams);
		final String solutionName = args.getSolutionName();

		if (Utils.stringIsEmpty(solutionName))
		{
			CurrentWindow.get().cancelSession("Invalid solution name");
			return;
		}

		if (!client.isEventDispatchThread()) J2DBGlobals.setServiceProvider(client);
		try
		{
			FlattenedSolution solution = client.getFlattenedSolution();
			if (solution != null)
			{
				// test for the main solution meta data else a login solution will constantly be closed even if it is for the right main solution.
				if (solution.getSolution() != null && !solutionName.equals(solution.getMainSolutionMetaData().getName()))
				{
					client.closeSolution(true, null);
				}
				else
				{
					if (solution.isMainSolutionLoaded() ||
						solution.getSolution() != null && solution.getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION)
					{
						//this is needed for the situation when the solution is already loaded and the deeplink url was changed (different arg values for instance)
						String method = args.getMethodName();
						String firstArgument = args.getFirstArgument();
						if (method != null)
						{
							try
							{
								client.getScriptEngine().getScopesScope().executeDeeplink(method,
									(args.toJSMap().isEmpty() ? null : new Object[] { firstArgument, args.toJSMap() }));
							}
							catch (Exception e1)
							{
								client.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { method }), e1); //$NON-NLS-1$
							}
						}
					}

					client.getRuntimeWindowManager().setCurrentWindowName(String.valueOf(CurrentWindow.get().getNr()));
					IWebFormController currentForm = client.getFormManager().getCurrentForm();
					if (currentForm != null)
					{
						// we have to call setcontroller again so that switchForm is called and the form is loaded into the reloaded/new window.
						startHandlingEvent();
						try
						{
							client.getClientFunctions().clear();
							sendUIProperties();
							if (client.getFormManager().isCurrentTheMainContainer())
							{
								client.getRuntimeWindowManager().getCurrentWindow().setController(currentForm);
							}
							else
							{
								// browser refresh while modal dialog is open
								RuntimeWindow mainWindow = client.getRuntimeWindowManager().getMainApplicationWindow();
								IFormController controller = mainWindow.getController();
								if (controller != null)
								{
									((NGRuntimeWindow)mainWindow).setController(controller);
								}
							}
							sendSolutionCSSURL(solution.getSolution());
						}
						finally
						{
							stopHandlingEvent();
						}
						return;
					}
				}
			}

			getEventDispatcher().addEvent(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						sendUIProperties();

						// the solution was not loaded or another was loaded, now create a main window and load the solution.
						client.getRuntimeWindowManager().createMainWindow(CurrentWindow.get().getNr());
						client.handleArguments(
							args.getFirstArgument() != null ? new String[] { args.getSolutionName(), args.getMethodName(), args.getFirstArgument() }
								: new String[] { args.getSolutionName(), args.getMethodName() },
							args);

						if (getHttpSession().getAttribute(StatelessLoginHandler.ID_TOKEN) != null)
						{
							setUserId();
						}
						client.loadSolution(solutionName);
						client.showInfoPanel();

					}
					catch (RepositoryException e)
					{
						Debug.error("Failed to load the solution: " + solutionName, e);
						sendInternalError(e);
					}
				}

				public void setUserId()
				{
					String id_token = (String)getHttpSession().getAttribute(StatelessLoginHandler.ID_TOKEN);
					String[] chunks = id_token.split("\\.");
					Base64.Decoder decoder = Base64.getUrlDecoder();
					String payload = new String(decoder.decode(chunks[1]));
					JSONObject token = new JSONObject(payload);
					String userID = token.getString(StatelessLoginHandler.UID);

					ClientInfo ci = client.getClientInfo();
					ci.setUserUid(userID);
					ci.setUserName(token.getString(StatelessLoginHandler.USERNAME));
					if (token.has(StatelessLoginHandler.PERMISSIONS))
					{
						JSONArray groups = token.getJSONArray(StatelessLoginHandler.PERMISSIONS);
						String[] gr = new String[groups.length()];
						for (int i = 0; i < groups.length(); i++)
						{
							gr[i] = groups.getString(i);
						}
						ci.setUserGroups(gr);
					}
					Object[] tenants = null;
					if (token.has(StatelessLoginHandler.TENANTS) && token.get(StatelessLoginHandler.TENANTS) instanceof JSONArray arr && arr.length() > 0)
					{
						tenants = new Object[arr.length()];
						for (int i = 0; i < arr.length(); i++)
						{
							try
							{
								tenants[i] = arr.get(i);
							}
							catch (JSONException e)
							{
								Debug.error("Cannot set the tenants value", e);
							}
						}
					}
					client.getFormManager().setTenantValue(tenants);
					if (token.optBoolean("remember", false))
					{
						JSONObject obj = new JSONObject();
						obj.put(StatelessLoginHandler.USERNAME, token.get(StatelessLoginHandler.USERNAME));
						obj.put(StatelessLoginHandler.ID_TOKEN, id_token);
						getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("rememberUser",
							new Object[] { obj });
					}
					//remove the id token of the oauth provider from the url
					getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("replaceUrlState",
						new Object[] { "/" + StatelessLoginHandler.SVYLOGIN_PATH });
				}
			});
		}
		catch (Exception e)
		{
			Debug.error(e);
			sendInternalError(e);
		}
		finally
		{
			if (!client.isEventDispatchThread()) J2DBGlobals.setServiceProvider(null);
		}
	}

	private void sendUIProperties()
	{
		Map<String, Object> clientProperties = client.getClientSideUIProperties();
		if (!clientProperties.containsKey(IApplication.TRUST_DATA_AS_HTML))
		{
			// set default trustDataAsHtml based on system setting
			clientProperties.put(IApplication.TRUST_DATA_AS_HTML,
				Boolean.valueOf(Settings.getInstance().getProperty(Settings.TRUST_DATA_AS_HTML_SETTING, Boolean.FALSE.toString())));
		}
		clientProperties.put(Settings.TESTING_MODE,
			Boolean.valueOf(Settings.getInstance().getProperty(Settings.TESTING_MODE, Boolean.FALSE.toString())));
		getClientService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setUIProperties", new Object[] { new JSONObject(clientProperties) });
	}

	@Override
	protected IServerService createFormService()
	{
		return new NGFormServiceHandler(this);
	}

	@Override
	protected IServerService createConsoleLoggerService()
	{
		return new NGConsoleLoggerServiceHandler(this);
	}

	@Override
	public void solutionLoaded(Solution solution)
	{
		sendSolutionCSSURL(solution);
	}

	public void sendStyleSheet()
	{
		if (client != null) sendSolutionCSSURL(client.getSolution());
	}

	private List<String> lastSentStyleSheets;

	@SuppressWarnings("nls")
	protected void sendSolutionCSSURL(Solution solution)
	{
		Map<String, String> overrideStyleSheets = client != null ? client.getOverrideStyleSheets() : null;
		List<String> styleSheets = PersistHelper.getOrderedStyleSheets(client.getFlattenedSolution());
		if (styleSheets != null && styleSheets.size() > 0)
		{
			if (overrideStyleSheets != null)
			{
				for (String oldStyleSheet : overrideStyleSheets.keySet())
				{
					if (styleSheets.contains(oldStyleSheet))
					{
						styleSheets.set(styleSheets.indexOf(oldStyleSheet), overrideStyleSheets.get(oldStyleSheet));
					}
				}
			}
			Collections.reverse(styleSheets);
			boolean ng2 = client.getRuntimeProperties().containsKey("NG2");
			for (int i = 0; i < styleSheets.size(); i++)
			{
				long timestamp = 0;
				Media media = null;
				String stylesheetName = styleSheets.get(i);
				if (ng2)
				{
					int lastPoint = stylesheetName.lastIndexOf('.');
					String ng2StylesheetName = stylesheetName.substring(0, lastPoint) + "_ng2" + stylesheetName.substring(lastPoint);
					media = client.getFlattenedSolution().getMedia(ng2StylesheetName);
					if (media == null) media = client.getFlattenedSolution().getMedia(stylesheetName);
					else stylesheetName = ng2StylesheetName;
				}
				else
					media = client.getFlattenedSolution().getMedia(stylesheetName);
				if (media != null && media.getLastModifiedTime() > 0)
				{
					timestamp = media.getLastModifiedTime();
					List<Media> references = media.getRuntimeProperty(Media.REFERENCES);
					if (references != null)
					{
						Long refLM = references.stream().collect(Collectors.summingLong(Media::getLastModifiedTime));
						timestamp += refLM.longValue();
					}
				}
				styleSheets.set(i, "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solution.getName() + "/" +
					stylesheetName.replace(".less", ".css") + "?t=" +
					Long.toHexString(timestamp == 0 ? client.getSolution().getLastModifiedTime() : timestamp) +
					"&clientnr=" + getSessionKey().getClientnr() + (ng2 ? "&ng2" : ""));
			}
			if (compareList(lastSentStyleSheets, styleSheets)) return;
			lastSentStyleSheets = new ArrayList<String>(styleSheets);
			getClientService(NGClient.APPLICATION_SERVICE).executeAsyncNowServiceCall("setStyleSheets",
				new Object[] { styleSheets.toArray(new String[0]) });
		}
		else
		{
			if (lastSentStyleSheets != null && lastSentStyleSheets.size() > 0)
			{
				getClientService(NGClient.APPLICATION_SERVICE).executeAsyncNowServiceCall("setStyleSheets", new Object[] { });
			}
			lastSentStyleSheets = null;
		}
	}

	private boolean compareList(List<String> list1, List<String> list2)
	{
		if (list1 == null) return list2 == null || list2.size() == 0;
		if (list2 == null) return list1 == null || list1.size() == 0;
		if (list1.size() == list2.size())
		{
			return list1.containsAll(list2) && list2.containsAll(list1);
		}
		return false;
	}

	@Override
	public void valueChanged()
	{
		if (client != null)
		{
			super.valueChanged();
		}
	}

	public void sendRedirect(final String redirectUrl)
	{
		IWindow curr = CurrentWindow.safeGet();
		CurrentWindow.runForWindow(curr != null && redirectUrl != null ? curr : new NGClientWebsocketSessionWindows(client.getWebsocketSession()),
			new Runnable()
			{
				@Override
				public void run()
				{
					Map<String, Object> detail = new HashMap<>();
					String htmlfilePath = Settings.getInstance().getProperty("servoy.webclient.pageexpired.page");
					if (htmlfilePath != null) detail.put("viewUrl", htmlfilePath);
					if (redirectUrl != null) detail.put("redirectUrl", redirectUrl);
					getClientService("$sessionService").executeAsyncServiceCall("expireSession", new Object[] { detail });
				}
			});
	}

	@Override
	protected IClientService createClientService(String name)
	{
		SpecProviderState specProviderState = WebServiceSpecProvider.getSpecProviderState();
		WebObjectSpecification spec = specProviderState == null ? null : specProviderState.getWebObjectSpecification(name);
		if (spec == null) spec = new WebObjectSpecificationBuilder().withName(name).withPackageType(IPackageReader.WEB_SERVICE).build();

		return new ServoyClientService(name, spec, this, true);
	}

	/*
	 * All windows are now closed. We shutdown the client in order to free up the license/resources for the next NGClient instantiation.
	 *
	 * @see org.sablo.websocket.BaseWebsocketSession#sessionExpired()
	 */
	@SuppressWarnings("nls")
	@Override
	public void sessionExpired()
	{
		if (!getClient().isShutDown()) try
		{
			if (SHUTDOWNLOGGER.isDebugEnabled()) SHUTDOWNLOGGER.debug("[SessionExpired] Shutting down client with id " + getSessionKey());
			getClient().invokeAndWait(() -> {
				getClient().shutDown(true);
			}, 5);
			if (SHUTDOWNLOGGER.isDebugEnabled()) SHUTDOWNLOGGER.debug("[SessionExpired] Client shutdown client with id " + getSessionKey());
		}
		catch (TimeoutException e)
		{
			if (SHUTDOWNLOGGER.isDebugEnabled()) SHUTDOWNLOGGER.debug("[SessionExpired] Timeout happend for shutdown client with id " + getSessionKey());
			if (!getClient().isShutDown())
			{
				// client shutdown timeout, maybe long running tasks.
				IEventDispatcher dispatcher = executor;
				if (dispatcher != null)
				{
					// just try to interrupt the event thread is that is still alive to force an exception.
					String stack = dispatcher.interruptEventThread();
					if (SHUTDOWNLOGGER.isDebugEnabled()) SHUTDOWNLOGGER
						.debug("[SessionExpired] dispatch thread interrupted; calling shutdown again (but later) for client with id " + getSessionKey() +
							" stack: \n" + stack);
					// now try again but don't wait for it.
					getClient().invokeLater(() -> {
						getClient().shutDown(true);
					});
				}
				else
				{
					if (SHUTDOWNLOGGER.isDebugEnabled())
						SHUTDOWNLOGGER.debug("[SessionExpired] no dispatch thread anymore for client with id " + getSessionKey());
				}
			}
			else
			{
				if (SHUTDOWNLOGGER.isDebugEnabled())
					SHUTDOWNLOGGER.debug("[SessionExpired] Client shutdown will not be called again on client with id " + getSessionKey() +
						" because it was already shut down.");
			}
		}
		super.sessionExpired();
	}


	/**
	 * Sets an internalServerError object on the client side which shows the internal server error page.
	 * If it is run from the developer it also adds the stack trace
	 * @param e
	 */
	public static void sendInternalError(Throwable e)
	{
		if (CurrentWindow.exists() && CurrentWindow.get().getEndpoint().hasSession())
		{
			Map<String, String> internalError = new HashMap<>();
			if (ApplicationServerRegistry.get().isDeveloperStartup())
			{
				StringWriter stackTrace = new StringWriter();
				e.printStackTrace(new PrintWriter(stackTrace));
				internalError.put("stack", stackTrace.toString());
			}
			String htmlView = Settings.getInstance().getProperty("servoy.webclient.error.page");
			if (htmlView != null) internalError.put("viewUrl", htmlView);
			CurrentWindow.get().getSession().getClientService("$sessionService").executeAsyncServiceCall("setInternalServerError",
				new Object[] { internalError });
		}
	}

	@Override
	public void updateLastAccessed(IWindow window)
	{
		super.updateLastAccessed(window);

		// see that the app server is still running
		IApplicationServerSingleton as = ApplicationServerRegistry.get();
		ScheduledExecutorService ee = (as != null ? as.getExecutor() : null);

		// check for window activity each time a window is closed, after the timeout period
		if (ee != null) ee.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				WebsocketSessionManager.closeInactiveSessions();
			}
		}, getWindowTimeout() * 1000 + 10, TimeUnit.MILLISECONDS);
	}

	@Override
	public Locale getLocale()
	{
		return client.getLocale();
	}

	@Override
	public IMessageLogger getMessageLogger(IWindow window)
	{
		if (MessageLogger.doLog)
		{
			return new MessageLogger(this, window.getNr());
		}
		return null;
	}

	@SuppressWarnings("nls")
	@Override
	public String getLogInformation()
	{
		if (client != null)
		{
			return "clientid: " + client.getClientID() +
				", httpsessionid: " + (getHttpSession() == null ? "<NONE>" : getHttpSession().getId()) +
				", serveruuid: " + ApplicationServerRegistry.get().getServerUUID();
		}
		return "";
	}
}
