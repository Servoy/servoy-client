package com.servoy.j2db.server.ngclient;

import java.util.Collection;

import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

public interface INGWebObject
{

	Object getProperty(String name);

	Object getRawPropertyValue(String name, boolean b);

	PropertyDescription getPropertyDescription(String name);

	BaseWebObject getUnderlyingWebObject();

	Collection<PropertyDescription> getProperties(IPropertyType< ? > type);
}
