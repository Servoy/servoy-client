package com.servoy.j2db.server.ngclient;

import java.util.Map;

import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;

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
		return rhinoMap.containsKey(name) ? rhinoMap.get(name) : underlyingWebObject.getProperty(name);
	}

	@Override
	public BaseWebObject getUnderlyingWebObject()
	{
		return underlyingWebObject;
	}

	public void setPropertyValues(Map<SabloT, SabloWT> rhinoMap)
	{
		this.rhinoMap = rhinoMap;
	}

	@Override
	public PropertyDescription getPropertyDescription(String name)
	{
		return customJSONTypeDefinition.getProperty(name);
	}
}
