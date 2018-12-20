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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Debug;

/**
 * @author lvostinar
 *
 */
public class CSSPositionPropertyType extends DefaultPropertyType<CSSPosition> implements IClassPropertyType<CSSPosition>,
	IFormElementToTemplateJSON<CSSPosition, CSSPosition>, IDesignToFormElement<Object, CSSPosition, CSSPosition>, IDesignValueConverter<CSSPosition>
{
	public static final CSSPositionPropertyType INSTANCE = new CSSPositionPropertyType();
	public static final String TYPE_NAME = "cssPosition";

	protected CSSPositionPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public CSSPosition fromJSON(Object newValue, CSSPosition previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof JSONObject)
		{
			JSONObject json = (JSONObject)newValue;
			return new CSSPosition(json.optString("top"), json.optString("right"), json.optString("bottom"), json.optString("left"), json.optString("width"),
				json.optString("height"));
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, CSSPosition object, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		if (object != null)
		{
			writer.key("top").value(object.top).key("right").value(object.right).key("bottom").value(object.bottom).key("left").value(object.left).key(
				"width").value(object.width).key("height").value(object.height);
		}
		writer.endObject();
		return writer;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, CSSPosition formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}

	@Override
	public CSSPosition toFormElementValue(Object designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		if (designValue instanceof JSONObject)
		{
			return fromJSON(designValue, null, pd, null, null);
		}
		if (!(designValue instanceof CSSPosition))
		{
			Debug.error("Wrong design value for css position:" + designValue);
			return defaultValue(pd);
		}
		return (CSSPosition)designValue;
	}

	@Override
	public CSSPosition defaultValue(PropertyDescription pd)
	{
		return new CSSPosition("0", "-1", "-1", "0", "80", "20");
	}

	@Override
	public Class<CSSPosition> getTypeClass()
	{
		return CSSPosition.class;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDesignValueConverter#fromDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public CSSPosition fromDesignValue(Object designValue, PropertyDescription propertyDescription)
	{
		try
		{
			return fromJSON((designValue instanceof String && ((String)designValue).startsWith("{")) ? new JSONObject((String)designValue) : designValue, null,
				propertyDescription, null, null);
		}
		catch (Exception e)
		{
			Debug.error("can't parse '" + designValue + "' to the real type for property converter: " + propertyDescription.getType(), e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDesignValueConverter#toDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription)
	 */
	@Override
	public Object toDesignValue(Object javaValue, PropertyDescription pd)
	{
		if (javaValue instanceof CSSPosition)
		{
			JSONStringer writer = new JSONStringer();
			toJSON(writer, null, (CSSPosition)javaValue, pd, null, null);
			return new JSONObject(writer.toString());
		}
		return javaValue;
	}
}
