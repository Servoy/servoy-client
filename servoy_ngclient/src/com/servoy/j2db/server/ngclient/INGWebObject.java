package com.servoy.j2db.server.ngclient;

import java.util.Map;

import org.sablo.BaseWebObject;
import org.sablo.websocket.TypedData;

public interface INGWebObject
{

	Object getProperty(String name);

	TypedData<Map<String, Object>> getProperties();

	BaseWebObject getUnderlyingWebObject();
}
