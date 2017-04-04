package com.servoy.j2db.server.ngclient;

import java.util.HashMap;
import java.util.Map;

import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.websocket.TypedData;

public class ComponentOrServiceExtension<SabloT, SabloWT> implements INGWebObject
{
	private final PropertyDescription customJSONTypeDefinition;
	private final BaseWebObject underlyingWebObject;
	private Map<SabloT, SabloWT> rhinoMap;

	public ComponentOrServiceExtension(PropertyDescription customJSONTypeDefinition, BaseWebObject componentOrService)
	{
		this.customJSONTypeDefinition = customJSONTypeDefinition;
		this.underlyingWebObject = componentOrService;
	}

	@Override
	public Object getProperty(String name)
	{
		return rhinoMap.get(name);
	}

	@Override
	public BaseWebObject getUnderlyingWebObject()
	{
		return underlyingWebObject;
	}

	@Override
	public TypedData<Map<String, Object>> getProperties()
	{
		return new TypedData<Map<String, Object>>(new HashMap<String, Object>(customJSONTypeDefinition.getCustomJSONProperties()), customJSONTypeDefinition);
	}

	public void setPropertyValues(Map<SabloT, SabloWT> rhinoMap)
	{
		this.rhinoMap = rhinoMap;
	}
}
