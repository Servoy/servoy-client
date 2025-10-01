/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.scripting.JSApplication;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.property.BrowserFunction;

/**
 * A type for string representation of JS code set on the server that is meant to run directly on client/inside the browser.
 *
 * It is similar to {@link ClientFunctionPropertyType} but it operates with BrowserFunction instances that are created via
 * {@link JSApplication#generateBrowserFunction(String)}. Because it is an {@link IClassPropertyType}, it is able to convert
 * properly when sending to client even when nested inside plain 'object' types.
 *
 * @author jcompanger
 * @since 2022.06
 */
public class DynamicClientFunctionPropertyType implements IClassPropertyType<BrowserFunction>, IPropertyWithClientSideConversions<BrowserFunction>
{

	public static final String CLIENT_SIDE_TYPE_NAME = "clientfunction"; //$NON-NLS-1$
	public static final DynamicClientFunctionPropertyType INSTANCE = new DynamicClientFunctionPropertyType();

	@Override
	public String getName()
	{
		return "dynamicclientfunction"; //$NON-NLS-1$
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		return null;
	}

	@Override
	public BrowserFunction defaultValue(PropertyDescription pd)
	{
		return null;
	}

	@Override
	public boolean isBuiltinType()
	{
		return false;
	}

	@Override
	public boolean isProtecting()
	{
		return false;
	}

	@Override
	public BrowserFunction fromJSON(Object newJSONValue, BrowserFunction previousSabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// can't be called from the clientside
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, BrowserFunction sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			// if making changes to this impl also check the MapPropertyType
			INGApplication application = sabloValue.getApplication();
			JSONUtils.addKeyIfPresent(writer, key);
			if (application.getRuntimeProperties().containsKey("NG2")) //$NON-NLS-1$
			{
				String uuid = application.registerClientFunction(sabloValue.getFunctionString());
				writer.value(uuid);
			}
			else
			{
				writer.value(sabloValue.getFunctionString());
			}
		}
		return writer;
	}

	@Override
	public Class<BrowserFunction> getTypeClass()
	{
		return BrowserFunction.class;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(CLIENT_SIDE_TYPE_NAME);
		return true;
	}

}
