/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.component.WebComponentApiDefinition;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.utils.JSONUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The websocket endpoint for communication between the WebClient instanceof the server and the browser.
 * This class creates if needed an instance.
 * It should generated a uuid when a new instance is created that is then send to the client to have session support
 * so that when the browser refreshes or we show a dialog in another tab/iframe that websocket does map on the same instance.
 * 
 * @author jcompagner
 */
@SuppressWarnings("nls")
@ServerEndpoint(value = "/websocket")
public class NGClientEndpoint implements INGClientEndpoint
{
	private static Map<String, NGClient> clients = new HashMap<>();

	private static Map<String, Long> noneActiveClients = new HashMap<>();

	private static final long SESSION_TIMEOUT = 1 * 60 * 1000;

	private static IClientCreator clientCreator;

	private Session session;

	private NGClient client;
	private String uuid;
	private String windowName;

	// modal dialogs are just divs, so they share the same endpoint, we need to store the actual one.
	private String currentWindowName;

	private final AtomicInteger handlingEvent = new AtomicInteger(0);
	private final AtomicInteger nextMessageId = new AtomicInteger(0);
	private final Map<Integer, List<Object>> pendingMessages = new HashMap<>();

	private final List<Map<String, Object>> serviceCalls = new ArrayList<>();

	private final ConcurrentMap<String, String> formsOnClient = new ConcurrentHashMap<>();


	public NGClientEndpoint()
	{
	}

	public static INGApplication getClient(String uuid)
	{
		return clients.get(uuid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGClientEndpoint#touchForm(java.lang.String)
	 */
	@Override
	public void touchForm(Form form)
	{
		if (form == null) return;
		String formUrl = (String)JSONUtils.toStringObject(form, PropertyType.form);
		if (formsOnClient.putIfAbsent(formUrl, formUrl) == null)
		{
			// form is not yet on the client, send over the controller
			updateController(form, formUrl);
		}

	}

	/**
	 * @param formUrl
	 * @param fs
	 * @param form
	 */
	private void updateController(Form form, String formUrl)
	{
		try
		{
			String realUrl = formUrl;
			FlattenedSolution fs = client.getFlattenedSolution();
			Solution sc = fs.getSolutionCopy(false);
			if (sc != null && sc.getChild(form.getUUID()) != null)
			{
				realUrl = realUrl + "?lm:" + form.getLastModified() + "&uuid=" + uuid;
			}

			boolean tableview = (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED);
			String view = (tableview ? "tableview" : "recordview");
			StringWriter sw = new StringWriter(512);
			new FormTemplateGenerator(fs).generate(form, "form_" + view + "_js.ftl", sw);
			if (client.isEventDispatchThread())
			{
				executeDirectServiceCall(NGRuntimeWindowMananger.WINDOW_SERVICE, "updateController",
					new Object[] { form.getName(), sw.toString(), formUrl, realUrl });
			}
			else
			{
				executeServiceCall(NGRuntimeWindowMananger.WINDOW_SERVICE, "updateController", new Object[] { form.getName(), sw.toString(), formUrl, realUrl });
			}
		}
		catch (IOException e)
		{
			Debug.error(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGClientEndpoint#updateFormUrl(com.servoy.j2db.persistence.Form)
	 */
	@Override
	public void updateForm(Form form)
	{
		String formUrl = (String)JSONUtils.toStringObject(form, PropertyType.form);
		if (formsOnClient.containsKey(formUrl))
		{
			updateController(form, formUrl);
		}
	}

	@OnOpen
	public void start(Session session, EndpointConfig config)
	{
		this.session = session;
		// TODO how to get the solution name, here by the request uri or param, or below in onmessage, sending it through the init.
		// String solutionname = session.getRequestParameterMap().get("solution").get(0);
		// String solutionname = session.getRequestURI().getPath().substring(x,y);
	}

	@OnError
	public void onError(Throwable t)
	{
		if (t instanceof IOException) Debug.error(t.getMessage());
		else Debug.error(t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGClientEndpoint#closeSession()
	 */
	@Override
	public void closeSession()
	{
		if (session != null)
		{
			try
			{
				System.err.println("calling close on " + session);
				session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Application Server shutdown!!!!!!!"));
			}
			catch (IOException e)
			{
			}
			session = null;
		}
		client = null;
	}

	@OnClose
	public void onClose()
	{
		synchronized (clients)
		{
			long currentTime = System.currentTimeMillis();
			Iterator<Entry<String, Long>> iterator = noneActiveClients.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<String, Long> entry = iterator.next();
				if (currentTime - entry.getValue().longValue() > SESSION_TIMEOUT)
				{
					iterator.remove();
				}
			}
			noneActiveClients.put(uuid, new Long(currentTime));
		}
		session = null;
		client = null;
	}

	private final StringBuilder incommingPartialMessage = new StringBuilder();

	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		String message = msg;
		if (!lastPart)
		{
			incommingPartialMessage.append(message);
			return;
		}
		if (incommingPartialMessage.length() > 0)
		{
			incommingPartialMessage.append(message);
			message = incommingPartialMessage.toString();
			incommingPartialMessage.setLength(0);
		}
		startHandlingEvent();
		if (client != null) J2DBGlobals.setServiceProvider(client);
		try
		{
			final JSONObject obj = new JSONObject(message);
			// always just try to set the current window name
			currentWindowName = obj.optString("windowname");
			String event = obj.getString("cmd");
			switch (event)
			{
				case "init" :
				{
					formsOnClient.clear();
					uuid = obj.optString("srvuuid");
					synchronized (clients)
					{
						if (uuid != null && uuid.length() > 0)
						{
							client = clients.get(uuid);
						}
						if (client != null && !client.isShutDown())
						{
							noneActiveClients.remove(uuid);
							J2DBGlobals.setServiceProvider(client);
							client.setActiveWebSocketClientEndpoint(this);
							// TODO now we just get the current form that is was last current and set it back
							// is there a better way? (the url doesn't have any info)
							IWebFormController currentForm = this.client.getFormManager().getCurrentForm();
							windowName = this.client.getRuntimeWindowManager().createMainWindow();
							this.client.getRuntimeWindowManager().getCurrentWindow().setController(currentForm);
						}
						else
						{
							String solutionName = obj.optString("solutionName");
							if (Utils.stringIsEmpty(solutionName)) solutionName = "InvalidSolutionNameInURL";
							client = getClientCreator().createClient(this);
							windowName = this.client.getRuntimeWindowManager().createMainWindow();
							uuid = UUID.randomUUID().toString();
							clients.put(uuid, client);
							J2DBGlobals.setServiceProvider(client); // set before loadSolution call, scriptengine contextlistener depends on this
							// TODO should just load not go into the invokeLater (solution load method and so on are executed as is the onload/onshow of the first form)
							client.loadSolution(solutionName);
						}
					}
					JSONStringer stringer = new JSONStringer();
					stringer.object().key("srvuuid").value(uuid).key("windowName").value(windowName);
					sendText(stringer.endObject().toString());
					break;
				}
				case "requestdata" :
				{
					String formName = obj.getString("formname");
					IWebFormUI form = client.getFormManager().getForm(formName).getFormUI();
					if (form instanceof WebGridFormUI && obj.has("currentPage")) ((WebGridFormUI)form).setCurrentPage(obj.getInt("currentPage"));
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
								if (form instanceof WebGridFormUI && obj.has("svy_pk") && !((WebGridFormUI)form).setEditingRowByPkHash(obj.getString("svy_pk")))
								{
									// don't go on if the right row couldn't be selected.
									error = "Could not select record by pk " + obj.getString("svy_pk");
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
										e.printStackTrace();
										Debug.error(e);
										error = e.getMessage();
									}
								}
								if (obj.has("cmsgid")) // client wants response
								{
									Map<String, Object> data = new HashMap<>();
									data.put("cmsgid", obj.get("cmsgid"));
									if (error == null)
									{
										data.put("ret", result);
									}
									else
									{
										data.put("exception", error);
									}
									sendText(writeDataWithConversions(data));
								}
							}
							catch (JSONException e)
							{
								Debug.error(e);
								e.printStackTrace();
							}
						}
					});

					break;
				}
				case "response" :
				{
					// {cmd:'response', smsgid: obj.smsgid, ret: value }
					synchronized (pendingMessages)
					{
						List<Object> ret = pendingMessages.get(new Integer(obj.getInt("smsgid")));
						if (ret != null) ret.add(obj.opt("ret"));
						pendingMessages.notifyAll();
					}
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
							JSONWriter response = null;
							try
							{
								response = new JSONStringer().object().key("cmsgid").value(obj.get("cmsgid"));
								IWebFormController form = parseForm(obj);
								List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
								boolean ok = form.notifyVisible(obj.getBoolean("visible"), invokeLaterRunnables);
								Utils.invokeLater(client, invokeLaterRunnables);
								if (ok && obj.has("relation"))
								{
									IWebFormController parentForm = client.getFormManager().getForm(obj.getString("parentForm"));
									FoundSet parentFs = parentForm.getFormModel();
									IRecordInternal selectedRecord = (IRecordInternal)parentFs.getSelectedRecord();
									form.loadRecords(selectedRecord.getRelatedFoundSet(obj.getString("relation")));
								}
								response.key("ret").value(ok);
							}
							catch (Exception e)
							{
								try
								{
									if (response != null) response.key("exception").value(e.getMessage());
								}
								catch (JSONException e1)
								{
								}
							}
							finally
							{
								try
								{
									if (response != null) sendText(response.endObject().toString());
								}
								catch (JSONException e)
								{
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
				case "service" :
				{
					client.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							String serviceName = obj.optString("servicename");
							String methodName = obj.optString("methodname");
							IService service = client.getService(serviceName);
							if (service != null)
							{
								Object result = null;
								String error = null;
								try
								{
									result = service.executeMethod(methodName, obj.optJSONObject("args"));
								}
								catch (Exception e)
								{
									e.printStackTrace();
									Debug.error(e);
									error = e.getMessage();
								}
								if (obj.has("cmsgid")) // client wants response
								{
									Map<String, Object> data = new HashMap<>();
									data.put("cmsgid", obj.opt("cmsgid"));
									if (error == null)
									{
										data.put("ret", result);
									}
									else
									{
										data.put("exception", error);
									}
									try
									{
										sendText(writeDataWithConversions(data));
									}
									catch (JSONException e)
									{
										Debug.error(e);
									}

								}
							}
						}
					});
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
		// if there is an incoming event, ignore this change else push it.
		if (session != null && client != null && handlingEvent.get() == 0)
		{
			try
			{
				sendChanges(client.getChanges());
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
		}
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

	private void sendChanges(Map<String, Map<String, Map<String, Object>>> properties) throws JSONException
	{
		if (properties.size() == 0 && serviceCalls.size() == 0) return;

		if (serviceCalls.size() > 0)
		{
			Map<String, Object> map = new HashMap<>();
			if (properties.size() > 0) map.put("forms", properties);
			map.put("services", serviceCalls);
			sendText(writeDataWithConversions(map));
			serviceCalls.clear();
		}
		else
		{
			sendText(writeDataWithConversions(Collections.singletonMap("forms", properties)));
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
						if (changes.has("svy_pk"))
						{
							String pkHash = changes.getString("svy_pk");
							changes.remove("svy_pk");
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
					e.printStackTrace();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGClientEndpoint#executeServiceCall(java.lang.String, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void executeServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		// {"services":[{name:serviceName,call:functionName,args:argumentsArray}]}
		Map<String, Object> serviceCall = new HashMap<>();
		serviceCall.put("name", serviceName);
		serviceCall.put("call", functionName);
		serviceCall.put("args", arguments);
		serviceCalls.add(serviceCall);
		valueChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.INGClientEndpoint#executeDirectServiceCall(java.lang.String, java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object executeDirectServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		// {"services":[{name:serviceName,call:functionName,args:argumentsArray}]}
		Map<String, Object> serviceCall = new HashMap<>();
		serviceCall.put("name", serviceName);
		serviceCall.put("call", functionName);
		serviceCall.put("args", arguments);
		Integer messageId = addMessageId(serviceCall);

		Map<String, Object> map = new HashMap<>();
		map.put("services", Collections.singletonList(serviceCall));

		try
		{
			return sendMessage(writeDataWithConversions(map), messageId);
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return null;
	}


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
			Integer messageId = apiDefinition.getReturnType() == null ? null : addMessageId(data); // this means we wait

			Object ret = sendMessage(writeDataWithConversions(data), messageId);
			// convert back
			if (ret instanceof Long && apiDefinition.getReturnType().getType() == PropertyType.date)
			{
				return new Date(((Long)ret).longValue());
			}
			return JSONUtils.toJavaObject(ret, apiDefinition.getReturnType() != null ? apiDefinition.getReturnType().getType() : null); // TODO should JSONUtils.toJavaObject  use PropertyDescription instead of propertyType
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}

		return null;
	}

	private void writeConversions(JSONWriter object, Map<String, Object> map) throws JSONException
	{
		for (Entry<String, Object> entry : map.entrySet())
		{
			if (entry.getValue() instanceof Map)
			{
				writeConversions(object.key(entry.getKey()).object(), (Map<String, Object>)entry.getValue());
				object.endObject();
			}
			else
			{
				object.key(entry.getKey()).value(entry.getValue());
			}
		}
	}

	protected String writeDataWithConversions(Map<String, ? > data) throws JSONException
	{
		JSONWriter writer = new JSONStringer().object();
		ClientConversion clientConversion = new ClientConversion();
		for (Entry<String, ? > entry : data.entrySet())
		{
			clientConversion.pushNode(entry.getKey());
			writer.key(entry.getKey());
			JSONUtils.toJSONValue(writer, entry.getValue(), clientConversion);
			clientConversion.popNode();
		}

		if (clientConversion.getConversions().size() > 0)
		{
			writer.key("conversions").object();
			writeConversions(writer, clientConversion.getConversions());
			writer.endObject();
		}

		return writer.endObject().toString();
	}

	protected Integer addMessageId(Map<String, Object> object)
	{
		Integer nextMessageid = new Integer(nextMessageId.incrementAndGet());
		object.put("smsgid", nextMessageid);
		return nextMessageid;
	}

	/**
	 * Send a message to the client.
	 * When messageId is not null, wait for the response.
	 */
	protected Object sendMessage(String message, Integer messageId)
	{
		sendText(message);
		if (messageId != null)
		{
			List<Object> ret = new ArrayList<>(1);
			synchronized (pendingMessages)
			{
				pendingMessages.put(messageId, ret);
				while (ret.size() == 0)
				{
					try
					{
						pendingMessages.wait();
					}
					catch (InterruptedException e)
					{
						// ignore
					}
				}
				pendingMessages.remove(messageId);
				return ret.get(0);
			}
		}

		return null;
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

	private synchronized void sendText(String txt)
	{
		try
		{
			if (session != null) session.getBasicRemote().sendText(txt);
		}
		catch (IOException e)
		{
			Debug.error(e);
			e.printStackTrace();
		}
	}

	/**
	 * @return the clientCreator
	 */
	public static IClientCreator getClientCreator()
	{
		if ((clientCreator == null))
		{
			clientCreator = new IClientCreator()
			{
				@Override
				public NGClient createClient(INGClientEndpoint endpoint)
				{
					return new NGClient(endpoint);
				}
			};
		}
		return clientCreator;
	}

	/**
	 * @param clientCreator the clientCreator to set
	 */
	public static void setClientCreator(IClientCreator clientCreator)
	{
		NGClientEndpoint.clientCreator = clientCreator;
	}
}
