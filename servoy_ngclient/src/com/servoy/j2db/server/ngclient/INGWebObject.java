package com.servoy.j2db.server.ngclient;

import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;

public interface INGWebObject
{

	Object getProperty(String name);

	Object getRawPropertyValue(String name, boolean b);

	PropertyDescription getPropertyDescription(String name);

	BaseWebObject getUnderlyingWebObject();
}
