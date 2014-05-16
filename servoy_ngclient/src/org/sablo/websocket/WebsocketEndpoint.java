/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sablo.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.NGClientForJsonConverter.ConversionLocation;
import com.servoy.j2db.util.Debug;

/**
 * The websocket endpoint for communication between the WebSocketSession instance on the server and the browser.
 * This class handles:
 * <ul>
 * <li>creating of websocket sessions and rebinding after reconnect
 * <li>messages protocol with request/response
 * <li>messages protocol with data conversion (currently only date)
 * <li>service calls (both server to client and client to server)
 * </ul>
 *
 * @author jcompagner, rgansevles
 */
@SuppressWarnings("nls")
@ServerEndpoint(value = "/websocket/{endpointType}/{id}/{argument}")
public class WebsocketEndpoint implements IWebsocketEndpoint
{
	private static ThreadLocal<IWebsocketEndpoint> currentInstance = new ThreadLocal<>();

	public static IWebsocketEndpoint get()
	{
		IWebsocketEndpoint websocketEndpoint = currentInstance.get();
		if (websocketEndpoint == null)
		{
			throw new IllegalStateException("no current websocket endpoint set");
		}
		return websocketEndpoint;
	}

	public static boolean exists()
	{
		return currentInstance.get() != null;
	}

	public static IWebsocketEndpoint set(IWebsocketEndpoint endpoint)
	{
		IWebsocketEndpoint websocketEndpoint = currentInstance.get();
		currentInstance.set(endpoint);
		return websocketEndpoint;
	}

	/*
	 * connection with browser
	 */
	private Session session;

	/*
	 * user session alike http session space
	 */
	private IWebsocketSession wsSession;

	private final AtomicInteger nextMessageId = new AtomicInteger(0);
	private final Map<Integer, List<Object>> pendingMessages = new HashMap<>();
	private final List<Map<String, Object>> serviceCalls = new ArrayList<>();

	public WebsocketEndpoint()
	{
	}

	@OnOpen
	public void start(Session newSession, @PathParam("endpointType")
	final String endpointType, @PathParam("id")
	String id, @PathParam("argument")
	final String arg) throws Exception
	{
		session = newSession;

		String uuid = "NULL".equals(id) ? null : id;
		String argument = "NULL".equals(arg) ? null : arg;

		wsSession = WebsocketSessionManager.getOrCreateSession(endpointType, uuid, true);
		try
		{
			currentInstance.set(this);
			wsSession.registerEndpoint(this);
			wsSession.onOpen(argument);
		}
		finally
		{
			currentInstance.remove();
		}
	}

	@OnError
	public void onError(Throwable t)
	{
		if (t instanceof IOException) Debug.error(t.getMessage());
		else Debug.error(t);
	}

	@Override
	public void closeSession()
	{
		closeSession(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Application Server shutdown!!!!!!!"));
	}

	@Override
	public void cancelSession(String reason)
	{
		closeSession(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
	}

	private void closeSession(CloseReason closeReason)
	{
		if (session != null)
		{
			try
			{
				session.close(closeReason);
			}
			catch (IOException e)
			{
			}
			session = null;
		}
		if (wsSession != null)
		{
			wsSession.deregisterEndpoint(this);
			wsSession = null;
		}
	}

	@OnClose
	public void onClose(@PathParam("endpointType")
	final String endpointType)
	{
		if (wsSession != null)
		{
			wsSession.deregisterEndpoint(this);
			WebsocketSessionManager.closeSession(endpointType, wsSession.getUuid());
		}
		session = null;
		wsSession = null;
	}

	private final StringBuilder incomingPartialMessage = new StringBuilder();

	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		String message = msg;
		if (!lastPart)
		{
			incomingPartialMessage.append(message);
			return;
		}
		if (incomingPartialMessage.length() > 0)
		{
			incomingPartialMessage.append(message);
			message = incomingPartialMessage.toString();
			incomingPartialMessage.setLength(0);
		}

		JSONObject obj;
		try
		{
			currentInstance.set(this);
			obj = new JSONObject(message);
			if (obj.has("smsgid"))
			{
				// response message
				synchronized (pendingMessages)
				{
					List<Object> ret = pendingMessages.get(new Integer(obj.getInt("smsgid")));
					if (ret != null) ret.add(obj.opt("ret"));
					pendingMessages.notifyAll();
				}
				return;
			}

			if (obj.has("service"))
			{
				// service call
				String serviceName = obj.optString("service");
				String methodName = obj.optString("methodname");

				wsSession.callService(serviceName, methodName, obj.optJSONObject("args"), obj.opt("cmsgid"));
				return;
			}
			wsSession.handleMessage(obj);
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return;
		}
		finally
		{
			currentInstance.remove();
		}

	}

	private void addServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		// {"services":[{name:serviceName,call:functionName,args:argumentsArray}]}
		Map<String, Object> serviceCall = new HashMap<>();
		serviceCall.put("name", serviceName);
		serviceCall.put("call", functionName);
		serviceCall.put("args", arguments);
		serviceCalls.add(serviceCall);
	}

	@Override
	public void executeAsyncServiceCall(String serviceName, String functionName, Object[] arguments)
	{
		addServiceCall(serviceName, functionName, arguments);
	}

	@Override
	public Object executeServiceCall(String serviceName, String functionName, Object[] arguments) throws IOException
	{
		addServiceCall(serviceName, functionName, arguments);
		return sendMessage(null, false, wsSession.getForJsonConverter()); // will return response from last service call
	}

	@SuppressWarnings("unchecked")
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

	public String writeDataWithConversions(Map<String, ? > data, IForJsonConverter forJsonConverter) throws JSONException
	{
		JSONWriter writer = new JSONStringer().object();
		DataConversion dataConversion = new DataConversion();
		for (Entry<String, ? > entry : data.entrySet())
		{
			dataConversion.pushNode(entry.getKey());
			writer.key(entry.getKey());
			JSONUtils.toJSONValue(writer, entry.getValue(), dataConversion, forJsonConverter, ConversionLocation.BROWSER_UPDATE);
			dataConversion.popNode();
		}

		if (dataConversion.getConversions().size() > 0)
		{
			writer.key("conversions").object();
			writeConversions(writer, dataConversion.getConversions());
			writer.endObject();
		}

		return writer.endObject().toString();
	}


	public Object sendMessage(Map<String, ? > data, boolean async, IForJsonConverter forJsonConverter) throws IOException
	{
		if ((data == null || data.size() == 0) && serviceCalls.size() == 0) return null;

		Map<String, Object> message = new HashMap<>();
		if (data != null && data.size() > 0)
		{
			message.put("msg", data);
		}
		if (serviceCalls.size() > 0)
		{
			message.put("services", serviceCalls);
		}

		Integer messageId = null;
		if (!async)
		{
			message.put("smsgid", messageId = new Integer(nextMessageId.incrementAndGet()));
		}

		try
		{
			sendText(writeDataWithConversions(message, forJsonConverter));
		}
		catch (JSONException e)
		{
			throw new IOException(e);
		}

		serviceCalls.clear();

		return (messageId == null) ? null : waitResponse(messageId);
	}

	public void sendMessage(String txt) throws IOException
	{
		sendText("{\"msg\":" + txt + '}');
	}

	@Override
	public void sendResponse(Object msgId, Object object, boolean success, IForJsonConverter forJsonConverter) throws IOException
	{
		Map<String, Object> data = new HashMap<>();
		data.put("cmsgid", msgId);
		data.put(success ? "ret" : "exception", object);
		try
		{
			sendText(writeDataWithConversions(data, forJsonConverter));
		}
		catch (JSONException e)
		{
			throw new IOException(e);
		}
	}

	private synchronized void sendText(String txt) throws IOException
	{
		if (session == null)
		{
			throw new IOException("No session");
		}
		session.getBasicRemote().sendText(txt);
	}


	/** Wait for a response message with given messsageId.
	 * @throws IOException
	 */
	protected Object waitResponse(Integer messageId) throws IOException
	{
		List<Object> ret = new ArrayList<>(1);
		synchronized (pendingMessages)
		{
			pendingMessages.put(messageId, ret);
			while (ret.size() == 0) // TODO are fail-safes/timeouts needed here in case client browser gets closed or confused?
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

	public boolean hasSession()
	{
		return session != null;
	}

}
