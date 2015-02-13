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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.eventthread.WebsocketSessionEndpoints;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
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
	private NGClient client;

	private final ConcurrentMap<IWebsocketEndpoint, ConcurrentMap<String, Pair<String, Boolean>>> endpointForms = new ConcurrentHashMap<>();

	public NGClientWebsocketSession(String uuid)
	{
		super(uuid);
	}

	public void setClient(NGClient client)
	{
		this.client = client;
	}

	public NGClient getClient()
	{
		return client;
	}

	@Override
	public Container getForm(String formName)
	{
		return (Container)client.getFormManager().getForm(formName).getFormUI();
	}

	@Override
	public boolean isValid()
	{
		return client != null && !client.isShutDown();
	}

	@Override
	protected IEventDispatcher createDispatcher()
	{
		return new NGEventDispatcher(client);
	}

	@Override
	public void onOpen(final Map<String, List<String>> requestParams)
	{
		super.onOpen(requestParams);

		if (requestParams == null)
		{
			WebsocketEndpoint.get().cancelSession("Solution name is required");
			return;
		}

		final StartupArguments args = new StartupArguments(requestParams);
		final String solutionName = args.getSolutionName();

		if (Utils.stringIsEmpty(solutionName))
		{
			WebsocketEndpoint.get().cancelSession("Invalid solution name");
			return;
		}

		if (!client.isEventDispatchThread()) J2DBGlobals.setServiceProvider(client);
		try
		{
			Solution solution = client.getSolution();
			if (solution != null)
			{
				if (!solution.getName().equals(solutionName))
				{
					client.closeSolution(true, null);
				}
				else
				{

					String method = args.getMethodName();
					String firstArgument = args.getFirstArgument();
					if (method != null)
					{
						try
						{
							client.getScriptEngine().getScopesScope().executeGlobalFunction(null, method,
								(firstArgument == null ? null : new Object[] { firstArgument, args.toJSMap() }), false, false);
						}
						catch (Exception e1)
						{
							client.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { method }), e1); //$NON-NLS-1$
						}
					}

					String windowId = WebsocketEndpoint.get().getWindowId();
					if (windowId == null || client.getRuntimeWindowManager().getWindow(windowId) == null)
					{
						// TODO can this happen? What is now the current form?
						WebsocketEndpoint.get().setWindowId(client.getRuntimeWindowManager().createMainWindow());
						// make sure a form is set?
					}
					else
					{
						client.getRuntimeWindowManager().setCurrentWindowName(windowId);
					}
					IWebFormController currentForm = client.getFormManager().getCurrentForm();
					if (currentForm != null)
					{
						// we have to call setcontroller again so that switchForm is called and the form is loaded into the reloaded/new window.
						startHandlingEvent();
						try
						{
							client.getRuntimeWindowManager().getCurrentWindow().setController(currentForm);
							sendSolutionCSSURL(solution);
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
						// the solution was not loaded or another was loaded, now create a main window and load the solution.
						WebsocketEndpoint.get().setWindowId(client.getRuntimeWindowManager().createMainWindow());


						List<String> arguments = new ArrayList<String>();

						if (args.getSolutionName() != null) arguments.add(StartupArguments.PARAM_KEY_SOLUTION + StartupArguments.PARAM_KEY_VALUE_SEPARATOR +
							args.getSolutionName());
						if (args.getFirstArgument() != null) arguments.add(StartupArguments.PARAM_KEY_ARGUMENT + StartupArguments.PARAM_KEY_VALUE_SEPARATOR +
							args.getFirstArgument());
						if (args.getMethodName() != null) arguments.add(StartupArguments.PARAM_KEY_METHOD + StartupArguments.PARAM_KEY_VALUE_SEPARATOR +
							args.getMethodName());
						client.handleArguments(arguments.toArray(new String[arguments.size()]), args);

						client.loadSolution(solutionName);

					}
					catch (RepositoryException e)
					{
						Debug.error("Failed to load the solution: " + solutionName, e);
						sendInternalError(e);
					}
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

	@Override
	protected IServerService createFormService()
	{
		return new NGFormServiceHandler(this);
	}

//	public void handleMessage(final JSONObject obj)
//	{
//		if (client != null) J2DBGlobals.setServiceProvider(client);
//		try
//		{
//		}
//		catch (Exception e)
//		{
//			Debug.error(e);
//			sendInternalError(e);
//		}
//		finally
//		{
//			J2DBGlobals.setServiceProvider(null);
//		}
//	}

	@Override
	public void formCreated(String formName)
	{
		ConcurrentMap<String, Pair<String, Boolean>> formsOnClient = endpointForms.get(WebsocketEndpoint.get());
		if (formsOnClient.containsKey(formName))
		{
			String formUrl = formsOnClient.get(formName).getLeft();
			synchronized (formUrl)
			{
				formsOnClient.get(formName).setRight(Boolean.TRUE);
				getEventDispatcher().resume(formUrl);
			}
		}
	}

	@Override
	public void registerEndpoint(IWebsocketEndpoint endpoint)
	{
		super.registerEndpoint(endpoint);
		endpointForms.put(endpoint, new ConcurrentHashMap<String, Pair<String, Boolean>>());
	}

	@Override
	public void deregisterEndpoint(IWebsocketEndpoint endpoint)
	{
		super.deregisterEndpoint(endpoint);
		endpointForms.remove(endpoint);
	}

	@Override
	public void touchForm(Form form, String realInstanceName, boolean async)
	{
		if (form == null) return;
		ConcurrentMap<String, Pair<String, Boolean>> formsOnClient = endpointForms.get(WebsocketEndpoint.get());
		if (formsOnClient == null) return; // endpoint is not registered for forms (ex: there is a api call from a scheduler, that will want to touch the form, but there are no forms for that endpoint)
		String formName = realInstanceName == null ? form.getName() : realInstanceName;
		String formUrl = "solutions/" + form.getSolution().getName() + "/forms/" + formName + ".html";
		if (formsOnClient.putIfAbsent(formName, new Pair<String, Boolean>(formUrl, Boolean.FALSE)) == null)
		{
			// form is not yet on the client, send over the controller
			updateController(form, formName, formUrl, !async);
		}
		else
		{
			formUrl = formsOnClient.get(formName).getLeft();
		}

		// if sync wait until we got response from client as it is loaded
		if (!async)
		{
			if (!formsOnClient.get(formName).getRight().booleanValue())
			{
				// really send the changes
				sendChanges();
				getEventDispatcher().suspend(formUrl, IWebsocketEndpoint.EVENT_LEVEL_SYNC_API_CALL);
			}
		}
	}

	/**
	 * @param formUrl
	 * @param fs
	 * @param form
	 */
	protected void updateController(Form form, String realFormName, String formUrl, boolean forceLoad)
	{
		try
		{
			String realUrl = formUrl;
			FlattenedSolution fs = client.getFlattenedSolution();
			Solution sc = fs.getSolutionCopy(false);
			boolean copy = false;
			if (sc != null && sc.getChild(form.getUUID()) != null)
			{
				realUrl = realUrl + "?lm:" + form.getLastModified() + "&sessionId=" + getUuid();
				copy = true;
			}
			else if (!form.getName().endsWith(realFormName))
			{
				realUrl = realUrl + "?lm:" + form.getLastModified() + "&sessionId=" + getUuid();
			}
			else
			{
				realUrl = realUrl + "?sessionId=" + getUuid();
			}
			StringWriter sw = new StringWriter(512);
			if (copy || !Boolean.valueOf(System.getProperty("servoy.generateformscripts", "false")).booleanValue())
			{
				new FormTemplateGenerator(new ServoyDataConverterContext(client), true, false).generate(form, realFormName, "form_recordview_js.ftl", sw);
			}
			if (client.isEventDispatchThread() && forceLoad)
			{
				getService(NGRuntimeWindowManager.WINDOW_SERVICE).executeServiceCall("updateController",
					new Object[] { realFormName, sw.toString(), realUrl, Boolean.valueOf(forceLoad) });
			}
			else
			{
				getService(NGRuntimeWindowManager.WINDOW_SERVICE).executeAsyncServiceCall("updateController",
					new Object[] { realFormName, sw.toString(), realUrl, Boolean.valueOf(forceLoad) });
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public void updateForm(Form form, String name)
	{
		String formUrl = "solutions/" + form.getSolution().getName() + "/forms/" + name + ".html";
		updateController(form, name, formUrl, false);
	}

	@Override
	public void solutionLoaded(Solution solution)
	{
		sendSolutionCSSURL(solution);
	}

	protected void sendSolutionCSSURL(Solution solution)
	{
		int styleSheetID = solution.getStyleSheetID();
		if (styleSheetID > 0)
		{
			Media styleSheetMedia = solution.getMedia(styleSheetID);
			if (styleSheetMedia != null)
			{
				String path = "resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solution.getName() + "/" + styleSheetMedia.getName();
				getService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setStyleSheet", new Object[] { path });
			}
			else
			{
				Debug.error("Cannot find solution styleSheet in media lib.");
			}
		}
		else
		{
			getService(NGClient.APPLICATION_SERVICE).executeAsyncServiceCall("setStyleSheet", new Object[] { });
		}
	}

	@Override
	protected Object invokeApi(WebComponent receiver, WebComponentApiDefinition apiFunction, Object[] arguments, PropertyDescription argumentTypes,
		Map<String, Object> callContributions)
	{
		Map<String, Object> call = new HashMap<>();
		if (callContributions != null) call.putAll(callContributions);

		IWebFormController form = client.getFormManager().getForm(receiver.findParent(IWebFormUI.class).getName());
		touchForm(form.getForm(), form.getName(), false);
		if (receiver instanceof WebFormComponent && ((WebFormComponent)receiver).getComponentContext() != null)
		{
			ComponentContext componentContext = ((WebFormComponent)receiver).getComponentContext();
			call.put("propertyPath", componentContext.getPropertyPath());
		}
		return super.invokeApi(receiver, apiFunction, arguments, argumentTypes, call);
	}

	@Override
	public void valueChanged()
	{
		if (client != null)
		{
			super.valueChanged();
		}
	}


	@Override
	public void closeSession()
	{
		this.closeSession(null);
	}

	public void closeSession(String redirectUrl)
	{
		if (client.getWebsocketSession() != null)
		{
			IWebsocketEndpoint current = WebsocketEndpoint.set(new WebsocketSessionEndpoints(client.getWebsocketSession()));
			try
			{
				Map<String, Object> detail = new HashMap<>();
				String htmlfilePath = Settings.getInstance().getProperty("servoy.webclient.pageexpired.page");
				if (htmlfilePath != null) detail.put("viewUrl", htmlfilePath);
				if (redirectUrl != null) detail.put("redirectUrl", redirectUrl);
				getService("$sessionService").executeAsyncServiceCall("expireSession", new Object[] { detail });
			}
			finally
			{
				WebsocketEndpoint.set(current);
			}
		}
		super.closeSession();
	}

	@Override
	protected IClientService createClientService(String name)
	{
		WebComponentSpecification spec = WebServiceSpecProvider.getInstance().getWebServiceSpecification(name);
		if (spec == null) spec = new WebComponentSpecification(name, "", name, null, null, "", null);
		return new ServoyClientService(name, spec, this);
	}

	/**
	 * Sets an internalServerError object on the client side which shows the internal server error page.
	 * If it is run from the developer it also adds the stack trace
	 * @param e
	 */
	public static void sendInternalError(Exception e)
	{
		Map<String, Object> internalError = new HashMap<>();
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();
		if (ApplicationServerRegistry.get().isDeveloperStartup()) internalError.put("stack", stackTrace);
		String htmlView = Settings.getInstance().getProperty("servoy.webclient.error.page");
		if (htmlView != null) internalError.put("viewUrl", htmlView);
		WebsocketEndpoint.get().getWebsocketSession().getService("$sessionService").executeAsyncServiceCall("setInternalServerError",
			new Object[] { internalError });
	}
}
