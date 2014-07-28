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

package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.specification.property.IComplexTypeImpl;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.ConversionLocation;

/**
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class NGClientForJsonConverter
{

	/**
	 * Converts a JSON value / primitive (if jsonSource is DESIGN or BROWSER) or custom server Java object (if jsonSource is SERVER, assuming
	 * that a custom complex type knows how to interpret thes custom server side set objects) to a Java value representing that property based on bean spec type.<br>
	 *
	 * @param oldJavaObject the object that currently represents this JSON's property
	 * @param propertyValue can be a JSONObject or array or primitive. (so something deserialized from a JSON string)
	 * @param component can be null in case for example return values are converted
	 * @param jsonSource hints about where the object to be converted originated from.
	 * @return the corresponding Java object based on bean spec.
	 */
	public static Object toJavaObject(Object sourceValue, PropertyDescription componentSpecType, IServoyDataConverterContext converterContext,
		ConversionLocation jsonSource, Object oldJavaObject) throws JSONException
	{
		Object propertyValue = sourceValue;
		if (sourceValue != null && componentSpecType != null)
		{
			IPropertyType type = componentSpecType.getType();

			IComplexTypeImpl complexType = type instanceof IComplexTypeImpl ? (IComplexTypeImpl)type : null;
			if (propertyValue instanceof IComplexPropertyValue)
			{
				// FormElement and WebComponent both do conversions on init so you end up
				// being asked to convert an already converted value; leave it as it is then
			}
			// ConversionLocation.BROWSER_UPDATE is now handled directly in sablo BaseWebObject
			// ConversionLocation.SERVER is now handled directly in RhinoConverter
			else if (complexType != null && jsonSource == ConversionLocation.DESIGN &&
				complexType.getDesignJSONToJavaPropertyConverter(componentSpecType.isArray()) != null)
			{
				propertyValue = complexType.getDesignJSONToJavaPropertyConverter(componentSpecType.isArray()).designJSONToJava(propertyValue,
					componentSpecType.getConfig());
			}
			else if (componentSpecType.isArray() && propertyValue instanceof JSONArray)
			{
				JSONArray arr = ((JSONArray)propertyValue);
				List oldList = (oldJavaObject instanceof List ? (List)oldJavaObject : null);
				List<Object> list = new ArrayList<>();
				for (int i = 0; i < arr.length(); i++)
				{
					list.add(toJavaObject(arr.get(i), componentSpecType.asArrayElement(), converterContext, jsonSource, (oldList != null && oldList.size() > i)
						? oldList.get(i) : null));
				}
				return list;
			}
		}

		return propertyValue;
	}


}