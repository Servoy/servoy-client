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
	private Session session;
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
		wsSession.setActiveWebsocketEndpoint(this);

		wsSession.onOpen(argument);
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
				System.err.println("calling close on " + session);
				session.close(closeReason);
			}
			catch (IOException e)
			{
			}
			session = null;
		}
		wsSession = null;
	}

	@OnClose
	public void onClose(@PathParam("endpointType")
	final String endpointType)
	{
		if (wsSession != null)
		{
			WebsocketSessionManager.close(endpointType, wsSession.getUuid());
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
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return;
		}

		wsSession.handleMessage(obj);
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
		return sendMessage(null, false); // will return response from last service call
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

	public String writeDataWithConversions(Map<String, ? > data) throws JSONException
	{
		JSONWriter writer = new JSONStringer().object();
		DataConversion dataConversion = new DataConversion();
		for (Entry<String, ? > entry : data.entrySet())
		{
			dataConversion.pushNode(entry.getKey());
			writer.key(entry.getKey());
			JSONUtils.toJSONValue(writer, entry.getValue(), dataConversion);
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


	public Object sendMessage(Map<String, ? > data, boolean async) throws IOException
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
			sendText(writeDataWithConversions(message));
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
	public void sendResponse(Object msgId, Object object, boolean success) throws IOException
	{
		Map<String, Object> data = new HashMap<>();
		data.put("cmsgid", msgId);
		data.put(success ? "ret" : "exception", object);
		try
		{
			sendText(writeDataWithConversions(data));
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
