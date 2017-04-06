package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

public class ComponentOrServiceExtension<SabloT, SabloWT> implements INGWebObject
{
	private final PropertyDescription customJSONTypeDefinition;
	private final INGWebObject underlyingWebObject;
	private Map<SabloT, SabloWT> rhinoMap;

	public ComponentOrServiceExtension(PropertyDescription customJSONTypeDefinition, INGWebObject componentOrService)
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
		return underlyingWebObject instanceof BaseWebObject ? (BaseWebObject)underlyingWebObject : underlyingWebObject.getUnderlyingWebObject();
	}

	public void setPropertyValues(Map<SabloT, SabloWT> rhinoMap)
	{
		this.rhinoMap = rhinoMap;
	}

	@Override
	public PropertyDescription getPropertyDescription(String name)
	{
		return customJSONTypeDefinition.getProperty(name) != null ? customJSONTypeDefinition.getProperty(name)
			: underlyingWebObject.getPropertyDescription(name);
	}

	@Override
	public Object getRawPropertyValue(String name, boolean getDefault)
	{
		if (rhinoMap.containsKey(name))
		{
			return rhinoMap.get(name);
		}
		if (getPropertyDescription(name).hasDefault())
		{
			return getPropertyDescription(name).getDefaultValue();
		}
		return null;
	}

	@Override
	public Collection<PropertyDescription> getProperties(IPropertyType< ? > type)
	{
		List<PropertyDescription> properties = new ArrayList<PropertyDescription>();
		properties.addAll(customJSONTypeDefinition.getProperties(type));
		properties.addAll(underlyingWebObject.getProperties(type));
		return properties;
	}
}
