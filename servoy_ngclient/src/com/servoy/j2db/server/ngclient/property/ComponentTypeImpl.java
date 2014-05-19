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

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONObject;
import org.sablo.specification.property.IComplexTypeImpl;
import org.sablo.specification.property.IDesignJSONToJavaPropertyConverter;
import org.sablo.specification.property.IJSONToJavaPropertyConverter;
import org.sablo.specification.property.IPropertyConfigurationParser;
import org.sablo.specification.property.IServerObjToJavaPropertyConverter;

import com.servoy.j2db.util.Debug;

/**
 * Implementation for the complex custom type "component". Can be used to nest components using properties.
 * 
 * @author acostescu
 */
public class ComponentTypeImpl implements IComplexTypeImpl<ComponentTypeConfig, ComponentTypeValue>
{

	public final static String TYPE_NAME_KEY = "typeName"; //$NON-NLS-1$
	public final static String DEFINITION_KEY = "definition"; //$NON-NLS-1$
	public final static String API_CALL_TYPES_KEY = "apiCallTypes"; //$NON-NLS-1$

	public final static String CALL_ON_KEY = "callOn"; //$NON-NLS-1$
	public final static int CALL_ON_SELECTED_RECORD = 0;
	public final static int CALL_ON_ALL_RECORDS = 1;

	public final static String FUNCTION_NAME_KEY = "functionName"; //$NON-NLS-1$

	@Override
	public IPropertyConfigurationParser<ComponentTypeConfig> getPropertyConfigurationParser()
	{
		return new IPropertyConfigurationParser<ComponentTypeConfig>()
		{

			@Override
			public ComponentTypeConfig parseProperyConfiguration(JSONObject configObject)
			{
				String tmp = configObject.optString("forFoundsetTypedProperty"); //$NON-NLS-1$
				return tmp == null ? null : new ComponentTypeConfig(tmp);
			}

		};
	}

	@Override
	public IJSONToJavaPropertyConverter<ComponentTypeConfig, ComponentTypeValue> getJSONToJavaPropertyConverter(final boolean isArray)
	{
		if (isArray)
		{
			return new IJSONToJavaPropertyConverter<ComponentTypeConfig, ComponentTypeValue>()
			{

				@Override
				public ComponentTypeValue jsonToJava(Object jsonValue, ComponentTypeValue oldJavaObject, ComponentTypeConfig config)
				{
					if (oldJavaObject == null) Debug.error("Somehow oldValue was null when getting browser updates (for array of components type property)...");
					else
					{
						oldJavaObject.browserUpdatesReceived(jsonValue);
					}
					return oldJavaObject;
				}

			};
		}
		else
		{
			return null;
			// TODO support only one as well?
		}
	}

	@Override
	public IDesignJSONToJavaPropertyConverter<ComponentTypeConfig, ComponentTypeValue> getDesignJSONToJavaPropertyConverter(boolean isArray)
	{
		if (isArray)
		{
			return new IDesignJSONToJavaPropertyConverter<ComponentTypeConfig, ComponentTypeValue>()
			{

				@Override
				public ComponentTypeValue designJSONToJava(Object jsonValue, ComponentTypeConfig config)
				{
					return new ComponentTypeValue(jsonValue, config);
				}

			};
		}
		else
		{
			return null;
			// TODO support only one as well?
		}
	}

	@Override
	public IServerObjToJavaPropertyConverter<ComponentTypeConfig, ComponentTypeValue> getServerObjectToJavaPropertyConverter(boolean isArray)
	{
		// TODO implement more here if we want this type of properties accessible in scripting
		return new IServerObjToJavaPropertyConverter<ComponentTypeConfig, ComponentTypeValue>()
		{

			@Override
			public boolean usesServerObjRepresentation()
			{
				return false;
			}

			@Override
			public ComponentTypeValue serverObjToJava(Object jsonValue, ComponentTypeConfig config, ComponentTypeValue oldValue)
			{
				return oldValue;
			}

		};
	}
}
