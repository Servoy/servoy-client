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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.websocket.BaseWebsocketSession#getForm(java.lang.String)
	 */
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
	public void onOpen(final String solutionName)
	{
		super.onOpen(solutionName);

		if (Utils.stringIsEmpty(solutionName))
		{
			WebsocketEndpoint.get().cancelSession("Invalid solution name");
			return;
		}

		if (!client.isEventDispatchThread()) J2DBGlobals.setServiceProvider(client);
		try
		{
			if (client.getSolution() != null)
			{
				if (!client.getSolution().getName().equals(solutionName))
				{
					client.closeSolution(true, null);
				}
				else
				{
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
							sendSolutionCSSURL(client.getSolution());
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

	/**
	 * @param message
	 */
	public void handleMessage(final JSONObject obj)
	{
		if (client != null) J2DBGlobals.setServiceProvider(client);
		try
		{
			// TODO: move these commands to services
			// always just try to set the current window name
			String event = obj.getString("cmd");
			switch (event)
			{
				case "datapush" :
				{
					pushChanges(obj, false);
					break;
				}
				case "svypush" :
				{
					pushChanges(obj, true);
					break;
				}
				case "event" :
				{
					getEventDispatcher().addEvent(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								JSONArray jsargs = obj.getJSONArray("args");
								IWebFormUI form = client.getFormManager().getFormAndSetCurrentWindow(obj.getString("formname")).getFormUI();
								WebFormComponent webComponent = form.getWebComponent(obj.getString("beanname"));
								String eventType = obj.getString("event");
								Object[] args = new Object[jsargs == null ? 0 : jsargs.length()];
								for (int i = 0; jsargs != null && i < jsargs.length(); i++)
								{
									args[i] = jsargs.get(i);
								}

								String error = null;
								Object result = null;
								pushChanges(obj);
								try
								{
									result = webComponent.executeEvent(eventType, args);
								}
								catch (Exception e)
								{
									Debug.error(e);
									error = e.getMessage();
								}
								if (obj.has("cmsgid")) // client wants response
								{
									WebsocketEndpoint.get().sendResponse(obj.get("cmsgid"), error == null ? result : error, null,
										FullValueToJSONConverter.INSTANCE, error == null);
								}
							}
							catch (JSONException | IOException e)
							{
								Debug.error(e);
								sendInternalError(e);
							}
						}
					});

					break;
				}
				case "formloaded" :
					formCreated(obj.getString("formname"));
					break;
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
			sendInternalError(e);
		}
		finally
		{
			J2DBGlobals.setServiceProvider(null);
		}
	}

	private void pushChanges(final JSONObject obj, final boolean apply)
	{
		getEventDispatcher().addEvent(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					IWebFormUI form = client.getFormManager().getFormAndSetCurrentWindow(obj.getString("formname")).getFormUI();

					pushChanges(obj);
					if (apply)
					{
						WebFormComponent webComponent = form.getWebComponent(obj.getString("beanname"));
						form.getDataAdapterList().pushChanges(webComponent, obj.getString("property"));
					}
				}
				catch (JSONException e)
				{
					Debug.error(e);
					sendInternalError(e);
				}
			}
		});
	}

	/**
	 * @param obj
	 * @throws JSONException
	 */
	private void pushChanges(JSONObject obj) throws JSONException
	{
		JSONObject changes = obj.getJSONObject("changes");
		if (changes.length() > 0)
		{
			String formName = obj.getString("formname");
			IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
			String beanName = obj.optString("beanname");

			WebComponent webComponent = beanName.length() > 0 ? form.getWebComponent(beanName) : (WebComponent)form;
			Iterator<String> keys = changes.keys();
			while (keys.hasNext())
			{
				String key = keys.next();
				Object object = changes.get(key);
				webComponent.putBrowserProperty(key, object);
			}
		}
	}

	@Override
	public void formCreated(String formName)
	{
		ConcurrentMap<String, Pair<String, Boolean>> formsOnClient = endpointForms.get(WebsocketEndpoint.get());
		String formUrl = formsOnClient.get(formName).getLeft();
		synchronized (formUrl)
		{
			formsOnClient.get(formName).setRight(Boolean.TRUE);
			formUrl.notifyAll();
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
			synchronized (formUrl)
			{
				if (!formsOnClient.get(formName).getRight().booleanValue())
				{
					// really send the changes
					sendChanges();
					try
					{
						formUrl.wait(); // wait for the 'formloaded' event from client
					}
					catch (InterruptedException ex)
					{
						Debug.error(ex);
					}
				}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.websocket.BaseWebsocketSession#valueChanged()
	 */
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
		if (client.getWebsocketSession() != null)
		{
			IWebsocketEndpoint current = WebsocketEndpoint.set(new WebsocketSessionEndpoints(client.getWebsocketSession()));
			try
			{
				Map<String, Object> detail = new HashMap<>();
				String htmlfilePath = Settings.getInstance().getProperty("servoy.webclient.pageexpired.page");
				if (htmlfilePath != null) detail.put("viewUrl", htmlfilePath);
				getService("$sessionService").executeServiceCall("expireSession", new Object[] { detail });
			}
			catch (IOException e)
			{
				Debug.log(e);
			}
			finally
			{
				WebsocketEndpoint.set(current);
			}
		}
		super.closeSession();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.websocket.BaseWebsocketSession#createClientService(java.lang.String)
	 */
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
