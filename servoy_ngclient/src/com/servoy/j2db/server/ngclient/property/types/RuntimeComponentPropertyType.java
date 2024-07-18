/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;

/**
 * @author lvostinar
 *
 */
public class RuntimeComponentPropertyType extends DefaultPropertyType<RuntimeWebComponent> implements IConvertedPropertyType<RuntimeWebComponent>
{
	public static final RuntimeComponentPropertyType INSTANCE = new RuntimeComponentPropertyType();
	public static final String TYPE_NAME = "runtimecomponent";

	private RuntimeComponentPropertyType()
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
	public RuntimeWebComponent fromJSON(Object newJSONValue, RuntimeWebComponent previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		//do we need this one also?
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, RuntimeWebComponent sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (key != null)
		{
			writer.key(key);
		}
		Object id = sabloValue.get("svyMarkupId", sabloValue);
		WebObjectSpecification spec = sabloValue.getComponent().getParent().getSpecification();
		if (spec != null)
		{
			Collection<PropertyDescription> formComponentProperties = spec
				.getProperties(FormComponentPropertyType.INSTANCE);
			if (formComponentProperties != null)
			{
				for (PropertyDescription property : formComponentProperties)
				{
					if (property.getConfig() instanceof ComponentTypeConfig &&
						((ComponentTypeConfig)property.getConfig()).forFoundset != null)
					{
						FoundsetTypeSabloValue foundsetPropertyValue = (FoundsetTypeSabloValue)sabloValue.getComponent()
							.getParent().getProperty(((ComponentTypeConfig)property.getConfig()).forFoundset);
						if (foundsetPropertyValue != null)
						{
							IFoundSetInternal foundset = foundsetPropertyValue.getFoundset();

							id += "_" + foundset.getSelectedIndex();
						}
						break;
					}

				}
			}
		}

		writer.value(id);
		return writer;
	}
}
