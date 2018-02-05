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
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.persistence.CSSPosition;

/**
 * @author lvostinar
 *
 */
public class CSSPositionPropertyType extends DefaultPropertyType<CSSPosition> implements IClassPropertyType<CSSPosition>
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
			return new CSSPosition(json.optInt("top"), json.optInt("left"), json.optInt("bottom"), json.optInt("right"), json.optInt("width"),
				json.optInt("height"));
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, CSSPosition object, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		return writer.object().key("top").value(object.top).key("left").value(object.left).key("bottom").value(object.bottom).key("right").value(
			object.right).key("width").value(object.width).key("height").value(object.height).endObject();
	}

	@Override
	public CSSPosition defaultValue(PropertyDescription pd)
	{
		return new CSSPosition(0, 0, -1, -1, 80, 20);
	}

	@Override
	public Class<CSSPosition> getTypeClass()
	{
		return CSSPosition.class;
	}
}
