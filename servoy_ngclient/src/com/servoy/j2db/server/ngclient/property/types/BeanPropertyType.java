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
package com.servoy.j2db.server.ngclient.property.types;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.DefaultPropertyType;

import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;

/**
 * @author jcompagner
 */
public class BeanPropertyType extends DefaultPropertyType<String> implements IFormElementToSabloComponent<String, String>
{

	public static final BeanPropertyType INSTANCE = new BeanPropertyType();
	public static final String TYPE_NAME = "bean";

	protected BeanPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public String toSabloComponentValue(String formElementValue, PropertyDescription pd, FormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		if (formElementValue != null)
		{
			return ComponentFactory.getMarkupId(formElement.getName(), formElementValue);
		}
		return null;
	}
}
