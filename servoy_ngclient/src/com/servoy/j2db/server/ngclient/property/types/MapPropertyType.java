/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.util.Text;

/**
 * @author gboros
 *
 */
public class MapPropertyType extends DefaultPropertyType<JSONObject>
	implements IConvertedPropertyType<JSONObject>, IFormElementToTemplateJSON<JSONObject, JSONObject>, IRhinoToSabloComponent<JSONObject>
{

	public static final MapPropertyType INSTANCE = new MapPropertyType();
	public static final String TYPE_NAME = "map"; //$NON-NLS-1$

	private MapPropertyType()
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyConverter#fromJSON(java.lang.Object, java.lang.Object, org.sablo.specification.PropertyDescription,
	 * java.lang.Object, org.sablo.util.ValueReference)
	 */
	@Override
	public JSONObject fromJSON(Object newJSONValue, JSONObject previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return newJSONValue instanceof JSONObject ? (JSONObject)newJSONValue : null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyConverter#toJSON(org.json.JSONWriter, java.lang.String, java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.websocket.utils.DataConversion, java.lang.Object)
	 */
	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, JSONObject sabloValue, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			writer.key(key);
			writer.value(fixJSONObjectValueTypesAndI18N(sabloValue));
		}
		return writer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON#toTemplateJSONValue(org.json.JSONWriter, java.lang.String,
	 * java.lang.Object, org.sablo.specification.PropertyDescription, org.sablo.websocket.utils.DataConversion,
	 * com.servoy.j2db.server.ngclient.FormElementContext)
	 */
	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, JSONObject formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		if (formElementValue != null)
		{
			writer.key(key);
			writer.value(fixJSONObjectValueTypesAndI18N(formElementValue));
		}
		return writer;
	}

	private JSONObject fixJSONObjectValueTypesAndI18N(JSONObject jsonObject)
	{
		JSONObject fixedJSONObject = new JSONObject();
		for (String jsonKey : jsonObject.keySet())
		{
			Object v = jsonObject.get(jsonKey);
			if (v instanceof String)
			{
				String sV = (String)v;
				if (sV.length() > 1 && sV.startsWith("'") && sV.endsWith("'"))
				{
					v = sV.substring(1, sV.length() - 1);
				}
				else if (sV.toLowerCase().equals("true") || sV.toLowerCase().equals("false"))
				{
					fixedJSONObject.put(jsonKey, Boolean.parseBoolean(sV));
					continue;
				}
				else
				{
					try
					{
						long lV = Long.parseLong(sV);
						fixedJSONObject.put(jsonKey, lV);
						continue;
					}
					catch (NumberFormatException ex)
					{
						try
						{
							double dV = Double.parseDouble(sV);
							fixedJSONObject.put(jsonKey, dV);
							continue;
						}
						catch (NumberFormatException ex1)
						{
						}
					}
				}

				v = Text.processTags((String)v, null);
			}
			fixedJSONObject.put(jsonKey, v);
		}
		return fixedJSONObject;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent#toSabloComponentValue(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.IWebObjectContext)
	 */
	@Override
	public JSONObject toSabloComponentValue(Object rhinoValue, JSONObject previousComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		JSONObject sabloValue = null;

		if (rhinoValue instanceof Map)
		{
			sabloValue = new JSONObject();
			for (Map.Entry< ? , ? > entry : ((Map< ? , ? >)rhinoValue).entrySet())
			{
				sabloValue.put(entry.getKey().toString(), entry.getValue());
			}
		}

		return sabloValue;
	}
}