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
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sablo.WebComponent;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.ConversionLocation;
import org.sablo.websocket.IForJsonConverter;
import org.sablo.websocket.IWebsocketEndpoint;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;


/**
 * Handles a websocket session based on a NGClient.
 *
 * @author rgansevles
 *
 */
public class NGClientWebsocketSession extends BaseWebsocketSession implements INGClientWebsocketSession
{
	private static ThreadLocal<String> currentWindowName = new ThreadLocal<>();

	private NGClient client;

	private final AtomicInteger handlingEvent = new AtomicInteger(0);

	private final ConcurrentMap<IWebsocketEndpoint, ConcurrentMap<String, String>> endpointForms = new ConcurrentHashMap<>();
	private boolean proccessChanges;

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
	public boolean isValid()
	{
		return !client.isShutDown();
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

		J2DBGlobals.setServiceProvider(client);
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
						client.getRuntimeWindowManager().getCurrentWindow().setController(currentForm);
						sendSolutionCSSURL(client.getSolution());
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
					}
				}
			});
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			J2DBGlobals.setServiceProvider(null);
		}
	}

	/**
	 * @param message
	 */
	public void handleMessage(final JSONObject obj)
	{
		startHandlingEvent();
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
								if (form instanceof WebGridFormUI && obj.has("rowId") && !((WebGridFormUI)form).setEditingRowByPkHash(obj.getString("rowId")))
								{
									// don't go on if the right row couldn't be selected.
									error = "Could not select record by rowId " + obj.getString("rowId");
								}
								else
								{
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
								}
								if (obj.has("cmsgid")) // client wants response
								{
									WebsocketEndpoint.get().sendResponse(obj.get("cmsgid"), error == null ? result : error, error == null,
										getForJsonConverter());
								}
							}
							catch (JSONException | IOException e)
							{
								Debug.error(e);
							}
						}
					});

					break;
				}
				case "valuelistfilter" :
				{
					String formName = obj.getString("formname");
					IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
					WebFormComponent webComponent = form.getWebComponent(obj.getString("beanname"));

					Object property = webComponent.getProperty(obj.getString("property"));
					LookupListModel lstModel = null;
					if (property instanceof CustomValueList)
					{
						lstModel = new LookupListModel(client, (CustomValueList)property);
						webComponent.setProperty(obj.getString("property"), lstModel, ConversionLocation.BROWSER_UPDATE);
					}
					else if (property instanceof LookupValueList)
					{
						lstModel = new LookupListModel(client, (LookupValueList)property);
						webComponent.setProperty(obj.getString("property"), lstModel, ConversionLocation.BROWSER_UPDATE);
					}
					else if (property instanceof LookupListModel)
					{
						lstModel = (LookupListModel)property;
					}

					if (lstModel != null)
					{
						// TODO what is the dataprovider? record could be given through the DataAdapterList..
						lstModel.fill(null, null, obj.getString("filter"), false);
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			stopHandlingEvent();
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

					if (form instanceof WebGridFormUI)
					{
						JSONObject changes = obj.getJSONObject("changes");
						if (changes.has("rowId"))
						{
							String pkHash = changes.getString("rowId");
							changes.remove("rowId");
							if (!((WebGridFormUI)form).setEditingRowByPkHash(pkHash))
							{
								// don't push changes, record couldn't be set
								return;
							}
						}
					}

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
		ConcurrentMap<String, String> formsOnClient = endpointForms.get(WebsocketEndpoint.get());
		String formUrl = formsOnClient.get(formName);
		synchronized (formUrl)
		{
			formUrl.notifyAll();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.websocket.BaseWebsocketSession#registerEndpoint(org.sablo.websocket.IWebsocketEndpoint)
	 */
	@Override
	public void registerEndpoint(IWebsocketEndpoint endpoint)
	{
		super.registerEndpoint(endpoint);
		endpointForms.put(endpoint, new ConcurrentHashMap<String, String>());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.websocket.BaseWebsocketSession#deregisterEndpoint(org.sablo.websocket.IWebsocketEndpoint)
	 */
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
		ConcurrentMap<String, String> formsOnClient = endpointForms.get(WebsocketEndpoint.get());
		String formName = realInstanceName == null ? form.getName() : realInstanceName;
		String formUrl = "solutions/" + form.getSolution().getName() + "/forms/" + formName + ".html";
		if (formsOnClient.putIfAbsent(formName, formUrl) == null)
		{
			// form is not yet on the client, send over the controller
			updateController(form, formName, formUrl, !async);
			if (!async)
			{
				synchronized (formUrl)
				{
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
	private void updateController(Form form, String realFormName, String formUrl, boolean forceLoad)
	{
		try
		{
			String realUrl = formUrl;
			FlattenedSolution fs = client.getFlattenedSolution();
			Solution sc = fs.getSolutionCopy(false);
			boolean copy = false;
			if (sc != null && sc.getChild(form.getUUID()) != null)
			{
				realUrl = realUrl + "?lm:" + form.getLastModified() + "&uuid=" + getUuid();
				copy = true;
			}
			else if (!form.getName().endsWith(realFormName))
			{
				realUrl = realUrl + "?lm:" + form.getLastModified() + "&uuid=" + getUuid();
			}
			StringWriter sw = new StringWriter(512);
			if (copy || !Boolean.valueOf(System.getProperty("servoy.generateformscripts", "false")).booleanValue())
			{
				boolean tableview = (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED);
				String view = (tableview ? "tableview" : "recordview");
				new FormTemplateGenerator(new ServoyDataConverterContext(client), true).generate(form, realFormName, "form_" + view + "_js.ftl", sw);
			}
			if (client.isEventDispatchThread())
			{
				executeServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "updateController",
					new Object[] { realFormName, sw.toString(), realUrl, Boolean.valueOf(forceLoad) });
			}
			else
			{
				executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "updateController",
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
				JSONStringer stringer = new JSONStringer();
				try
				{
					stringer.object().key("styleSheetPath").value(
						"resources/" + MediaResourcesServlet.FLATTENED_SOLUTION_ACCESS + "/" + solution.getName() + "/" + styleSheetMedia.getName());
					WebsocketEndpoint.get().sendMessage(stringer.endObject().toString());
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else
			{
				Debug.error("Cannot find solution styleSheet in media lib.");
			}
		}
	}

	public void startHandlingEvent()
	{
		handlingEvent.incrementAndGet();
	}

	public void stopHandlingEvent()
	{
		handlingEvent.decrementAndGet();
		valueChanged();
	}

	@Override
	public void valueChanged()
	{
		// if there is an incoming message or an NGEvent running on event thread, postpone sending until it's done; else push it.
		if (!proccessChanges && WebsocketEndpoint.get().hasSession() && client != null && handlingEvent.get() == 0)
		{
			try
			{
				proccessChanges = true;
				sendChanges(WebsocketEndpoint.get().getAllComponentsChanges());
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
			finally
			{
				proccessChanges = false;
			}
		}
	}

	public void sendChanges(Map<String, Map<String, Map<String, Object>>> properties) throws IOException
	{
		WebsocketEndpoint.get().sendMessage(properties.size() == 0 ? null : Collections.singletonMap("forms", properties), true, getForJsonConverter());
	}

	@Override
	public void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		super.executeAsyncServiceCall(serviceName, functionName, arguments);
		valueChanged();
	}

	@Override
	protected Object invokeApi(WebComponent receiver, WebComponentApiDefinition apiFunction, Object[] arguments, Map<String, Object> callContributions)
	{
		try
		{
			Map<String, Object> call = new HashMap<>();
			if (callContributions != null) call.putAll(callContributions);

			IWebFormController form = client.getFormManager().getForm(receiver.getParent().getName());
			touchForm(form.getForm(), form.getName(), false);
			if (form.getFormUI() instanceof WebGridFormUI)
			{
				call.put("viewIndex", Integer.valueOf(((WebGridFormUI)form.getFormUI()).getSelectedViewIndex()));
			}
			Object ret = super.invokeApi(receiver, apiFunction, arguments, call);
			return NGClientForJsonConverter.toJavaObject(ret, apiFunction.getReturnType(), new ServoyDataConverterContext(getClient()),
				ConversionLocation.BROWSER_UPDATE, null); // TODO should JSONUtils.toJavaObject  use PropertyDescription instead of propertyType
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public IForJsonConverter getForJsonConverter()
	{
		return NGClientForJsonConverter.INSTANCE;
	}
}
