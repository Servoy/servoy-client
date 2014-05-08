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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IService;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.component.WebComponentApiDefinition;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.server.ngclient.property.PropertyType;
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
	private NGClient client;
	private String windowName;

	// modal dialogs are just divs, so they share the same endpoint, we need to store the actual one.
	private String currentWindowName;

	private final AtomicInteger handlingEvent = new AtomicInteger(0);

	private final ConcurrentMap<String, String> formsOnClient = new ConcurrentHashMap<>();

	public NGClientWebsocketSession()
	{
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
	public void onOpen(final String solutionName)
	{
		super.onOpen(solutionName);

		if (Utils.stringIsEmpty(solutionName))
		{
			getActiveWebsocketEndpoint().cancelSession("Invalid solution name");
			return;
		}

		formsOnClient.clear();

		IWebFormController currentForm = client.getFormManager().getCurrentForm();

		windowName = client.getRuntimeWindowManager().createMainWindow();
		try
		{
			getActiveWebsocketEndpoint().sendMessage(
				new JSONStringer().object().key("srvuuid").value(getUuid()).key("windowName").value(windowName).endObject().toString());
		}
		catch (IOException | JSONException e)
		{
			Debug.error(e);
		}

		J2DBGlobals.setServiceProvider(client);
		if (currentForm != null)
		{
			// TODO now we just get the current form that is was last current and set it back
			// is there a better way? (the url doesn't have any info)
			client.getRuntimeWindowManager().getCurrentWindow().setController(currentForm);
			sendSolutionCSSURL(client.getSolution());
		}
		else
		{
			// TODO should just load not go into the invokeLater (solution load method and so on are executed as is the onload/onshow of the first form)
			client.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						client.loadSolution(solutionName);
					}
					catch (RepositoryException e)
					{
						Debug.error("Failed to load the solution: " + solutionName, e);
					}
				}
			});
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
			currentWindowName = obj.optString("windowname");
			String event = obj.getString("cmd");
			switch (event)
			{
				case "requestdata" :
				{
					String formName = obj.getString("formname");
					IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
					if (form instanceof WebGridFormUI)
					{
						WebGridFormUI grid = (WebGridFormUI)form;
						if (obj.has("currentPage")) grid.setCurrentPage(obj.getInt("currentPage"));
						if (obj.has("pageSize")) grid.setPageSize(obj.getInt("pageSize"));
					}
					Map<String, Map<String, Object>> properties = form.getAllProperties();
					Map<String, Map<String, Map<String, Object>>> formData = new HashMap<String, Map<String, Map<String, Object>>>();
					formData.put(formName, properties);
					sendChanges(formData);
					break;
				}
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
					client.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								String formName = obj.getString("formname");
								JSONArray jsargs = obj.getJSONArray("args");
								IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
								WebComponent webComponent = form.getWebComponent(obj.getString("beanname"));
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
										result = webComponent.execute(eventType, args);
									}
									catch (Exception e)
									{
										Debug.error(e);
										error = e.getMessage();
									}
								}
								if (obj.has("cmsgid")) // client wants response
								{
									getActiveWebsocketEndpoint().sendResponse(obj.get("cmsgid"), error == null ? result : error, error == null);
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
				case "formreadOnly" :
				{
					IWebFormController form = parseForm(obj);
					if (form != null)
					{
						((WebFormController)form).setReadOnly(obj.getBoolean("readOnly"));
					}
					break;
				}
				case "formenabled" :
				{
					IWebFormController form = parseForm(obj);
					if (form != null)
					{
						((WebFormController)form).setComponentEnabled(obj.getBoolean("enabled"));
					}
					break;
				}
				case "formvisibility" :
				{
					client.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								IWebFormController form = parseForm(obj);
								List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
								boolean isVisible = obj.getBoolean("visible");
								boolean ok = form.notifyVisible(isVisible, invokeLaterRunnables);
								if (ok && obj.has("parentForm") && !obj.isNull("parentForm"))
								{
									IWebFormController parentForm = client.getFormManager().getForm(obj.getString("parentForm"));
									WebComponent containerComponent = parentForm.getFormUI().getWebComponent(obj.getString("bean"));
									if (containerComponent != null)
									{
										containerComponent.updateVisibleForm(form.getFormUI(), isVisible, obj.optInt("formIndex"));
									}
									if (obj.has("relation") && !obj.isNull("relation"))
									{
										String relation = obj.getString("relation");
										FoundSet parentFs = parentForm.getFormModel();
										IRecordInternal selectedRecord = (IRecordInternal)parentFs.getSelectedRecord();
										form.loadRecords(selectedRecord.getRelatedFoundSet(relation));
										parentForm.getFormUI().getDataAdapterList().addRelatedForm(form, relation);
									}
								}
								Utils.invokeLater(client, invokeLaterRunnables);
								getActiveWebsocketEndpoint().sendResponse(obj.get("cmsgid"), Boolean.valueOf(ok), true);
							}
							catch (Exception e)
							{
								Debug.error(e);
								try
								{
									getActiveWebsocketEndpoint().sendResponse(obj.get("cmsgid"), e.getMessage(), false);
								}
								catch (IOException | JSONException e1)
								{
									Debug.error(e1);
								}
							}
						}
					});
					break;
				}
				case "valuelistfilter" :
				{
					String formName = obj.getString("formname");
					IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
					WebComponent webComponent = form.getWebComponent(obj.getString("beanname"));

					Object property = webComponent.getProperty(obj.getString("property"));
					LookupListModel lstModel = null;
					if (property instanceof CustomValueList)
					{
						lstModel = new LookupListModel(client, (CustomValueList)property);
						webComponent.putProperty(obj.getString("property"), lstModel);
					}
					else if (property instanceof LookupValueList)
					{
						lstModel = new LookupListModel(client, (LookupValueList)property);
						webComponent.putProperty(obj.getString("property"), lstModel);
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

	@Override
	public void callService(String serviceName, final String methodName, final JSONObject args, final Object msgId)
	{
		final IService service = getService(serviceName);
		if (service != null)
		{
			client.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					doCallService(service, methodName, args, msgId);
				}

			});
		}
		else
		{
			Debug.warn("Unknown servie called: " + serviceName);
		}
	}

	private IWebFormController parseForm(JSONObject obj)
	{
		try
		{
			String formName = obj.getString("form");
			if (formName.endsWith(".html"))
			{
				int index = formName.lastIndexOf('/');
				formName = formName.substring(index + 1, formName.length() - 5);
			}
			return client.getFormManager().getForm(formName);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
	}

	private void pushChanges(final JSONObject obj, final boolean apply)
	{
		client.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					String formName = obj.getString("formname");
					IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();

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
						WebComponent webComponent = form.getWebComponent(obj.getString("beanname"));
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
		String formName = obj.getString("formname");
		IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
		String beanName = obj.optString("beanname");

		WebComponent webComponent = beanName.length() > 0 ? form.getWebComponent(beanName) : (WebComponent)form;
		JSONObject changes = obj.getJSONObject("changes");
		Iterator<String> keys = changes.keys();
		while (keys.hasNext())
		{
			String key = keys.next();
			Object object = changes.get(key);
			webComponent.putBrowserProperty(key, object);
		}
	}

	@Override
	public void touchForm(Form form, String realInstanceName)
	{
		if (form == null) return;
		String formName = realInstanceName == null ? form.getName() : realInstanceName;
		String formUrl = "solutions/" + form.getSolution().getName() + "/forms/" + formName + ".html";
		if (formsOnClient.putIfAbsent(formName, formUrl) == null)
		{
			// form is not yet on the client, send over the controller
			updateController(form, formName, formUrl);
		}
	}

	/**
	 * @param formUrl
	 * @param fs
	 * @param form
	 */
	private void updateController(Form form, String realFormName, String formUrl)
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
				new FormTemplateGenerator(new DataConverterContext(client), true).generate(form, realFormName, "form_" + view + "_js.ftl", sw);
			}
			if (client.isEventDispatchThread())
			{
				executeServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "updateController", new Object[] { realFormName, sw.toString(), realUrl });
			}
			else
			{
				executeAsyncServiceCall(NGRuntimeWindowManager.WINDOW_SERVICE, "updateController", new Object[] { realFormName, sw.toString(), realUrl });
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
		updateController(form, name, formUrl);
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
					getActiveWebsocketEndpoint().sendMessage(stringer.endObject().toString());
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
		if (getActiveWebsocketEndpoint().hasSession() && client != null && handlingEvent.get() == 0)
		{
			try
			{
				sendChanges(client.getChanges());
			}
			catch (IOException e)
			{
				Debug.error(e);
			}
		}
	}

	private void sendChanges(Map<String, Map<String, Map<String, Object>>> properties) throws IOException
	{
		getActiveWebsocketEndpoint().sendMessage(properties.size() == 0 ? null : Collections.singletonMap("forms", properties), true);
	}

	/**
	 * @return the currentWindowName
	 */
	public String getCurrentWindowName()
	{
		if (!Utils.stringIsEmpty(currentWindowName))
		{
			return currentWindowName;
		}
		return windowName;
	}


	@Override
	public Object executeApi(WebComponentApiDefinition apiDefinition, String formName, String beanName, Object[] arguments)
	{
		// {"call":{"form":"product","bean":"datatextfield1","api":"requestFocus","args":[arg1, arg2]}}
		try
		{
			Map<String, Map<String, Map<String, Object>>> changes = client.getChanges();
			Map<String, Object> data = new HashMap<>();
			data.put("forms", changes);

			Map<String, Object> call = new HashMap<>();
			call.put("form", formName);
			call.put("bean", beanName);
			call.put("api", apiDefinition.getName());

			IWebFormController form = client.getFormManager().getForm(formName);
			if (form.getFormUI() instanceof WebGridFormUI)
			{
				call.put("viewIndex", Integer.valueOf(((WebGridFormUI)form.getFormUI()).getSelectedViewIndex()));
			}
			if (arguments != null && arguments.length > 0)
			{
				call.put("args", arguments);
			}
			data.put("call", call);

			Object ret = getActiveWebsocketEndpoint().sendMessage(data, false);
			// convert back
			if (ret instanceof Long && apiDefinition.getReturnType().getType() == PropertyType.date)
			{
				return new Date(((Long)ret).longValue());
			}
			return JSONUtils.toJavaObject(ret, apiDefinition.getReturnType(), new DataConverterContext(getClient())); // TODO should JSONUtils.toJavaObject  use PropertyDescription instead of propertyType
		}
		catch (JSONException | IOException e)
		{
			Debug.error(e);
		}

		return null;
	}

	@Override
	public void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		super.executeAsyncServiceCall(serviceName, functionName, arguments);
		valueChanged();
	}
}
