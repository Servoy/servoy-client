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
import org.json.JSONString;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeJavaObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowserWithDynamicClientType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;

/**
 * @author gboros
 *
 */
public class JSNativeJavaObjectType extends DefaultPropertyType<NativeJavaObject>
	implements IClassPropertyType<NativeJavaObject>, IPropertyConverterForBrowserWithDynamicClientType<NativeJavaObject>
{

	public static final JSNativeJavaObjectType INSTANCE = new JSNativeJavaObjectType();
	public static final String TYPE_NAME = "jsnativejavaobject"; //$NON-NLS-1$

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public NativeJavaObject fromJSON(Object newJSONValue, NativeJavaObject previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// not supported
		return null;
	}

	@Override
	public JSONString toJSONWithDynamicClientSideType(JSONWriter writer, NativeJavaObject sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null) // should always be != null as long as this class is used only as a class type found based on sabloValue.getClass()
		{
			IJSONStringWithClientSideType valueAndClientSideType = FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(sabloValue.unwrap(),
				propertyDescription, dataConverterContext, false);
			if (valueAndClientSideType != null)
			{
				writer.value(valueAndClientSideType);
				return valueAndClientSideType.getClientSideType();
			} // else wrapped value doesn't want to write anything to the client
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, NativeJavaObject sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		if (sabloValue != null) // should always be != null as long as this class is used only as a class type found based on sabloValue.getClass()
		{
			return FullValueToJSONConverter.INSTANCE.toJSONValue(writer, key, sabloValue.unwrap(), pd, dataConverterContext);
		}
		return null;
	}

	@Override
	public NativeJavaObject defaultValue(PropertyDescription pd)
	{
		return null;
	}

	@Override
	public Class<NativeJavaObject> getTypeClass()
	{
		return NativeJavaObject.class;
	}

}
