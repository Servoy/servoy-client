/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import org.mozilla.javascript.NativeFunction;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

/**
 * @author gboros
 *
 */
public class NativeFunctionType extends UUIDReferencePropertyType<NativeFunction>
	implements IPropertyConverterForBrowser<NativeFunction>, IClassPropertyType<NativeFunction>
{
	public static final NativeFunctionType INSTANCE = new NativeFunctionType();
	public static final String TYPE_NAME = "NativeFunction"; //$NON-NLS-1$

	private NativeFunctionType()
	{
	}

	/*
	 * @see org.sablo.specification.property.IPropertyConverter#fromJSON(java.lang.Object, java.lang.Object, org.sablo.specification.PropertyDescription,
	 * java.lang.Object, org.sablo.util.ValueReference)
	 */
	@Override
	public NativeFunction fromJSON(Object newJSONValue, NativeFunction previousSabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext context, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonNativeFunction = (JSONObject)newJSONValue;
			return getReference(jsonNativeFunction.optString("functionhash"));
		}
		return null;
	}

	/*
	 * @see org.sablo.specification.property.IPropertyConverter#toJSON(org.json.JSONWriter, java.lang.String, java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.websocket.utils.DataConversion, java.lang.Object)
	 */
	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, NativeFunction sabloValue, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key("functionhash").value(addReference(sabloValue));
		writer.key("svyType").value(getName());
		writer.endObject();
		return writer;
	}

	/*
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	/*
	 * @see org.sablo.specification.property.IClassPropertyType#getTypeClass()
	 */
	@Override
	public Class<NativeFunction> getTypeClass()
	{
		return NativeFunction.class;
	}
}
