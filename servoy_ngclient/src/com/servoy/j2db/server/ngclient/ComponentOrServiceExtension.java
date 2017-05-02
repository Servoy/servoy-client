package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareMap;
import org.sablo.specification.property.IPropertyType;

public class ComponentOrServiceExtension<SabloT, SabloWT> implements INGWebObject
{
	private final PropertyDescription customJSONTypeDefinition;
	private final INGWebObject underlyingWebObject;
	private ChangeAwareMap<SabloT, SabloWT> rhinoMap;

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

	public void setPropertyValues(ChangeAwareMap<SabloT, SabloWT> map)
	{
		this.rhinoMap = map;
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
		PropertyDescription customPD = customJSONTypeDefinition.getProperty(name);
		if (customPD != null)
		{
			if (rhinoMap.getBaseMap().containsKey(name))
			{
				return rhinoMap.getBaseMap().get(name);
			}
			return BaseWebObject.getDefaultFromPD(getDefaultFromSpecAsWellIfNeeded, customPD);
		}

		return underlyingWebObject.getRawPropertyValue(name, getDefaultFromSpecAsWellIfNeeded);
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
