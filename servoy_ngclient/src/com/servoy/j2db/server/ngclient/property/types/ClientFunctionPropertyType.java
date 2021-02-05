/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGApplication;

/**
 * @author jcompagner
 * @since 2021.03
 */
@SuppressWarnings("nls")
public class ClientFunctionPropertyType extends TagStringPropertyType
{
	public static final ClientFunctionPropertyType CLIENT_FUNCTION_INSTANCE = new ClientFunctionPropertyType();
	public static final String CLIENT_FUNCTION_TYPE_NAME = "clientfunction";

	@Override
	public String getName()
	{
		return CLIENT_FUNCTION_TYPE_NAME;
	}

	@Override
	public boolean valueInTemplate(String formElementVal, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}

	@Override
	public BasicTagStringTypeSabloValue fromJSON(Object newValue, BasicTagStringTypeSabloValue previousValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// we don't allow any changes from the client.
		return previousValue;
	}


	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, BasicTagStringTypeSabloValue object, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (object != null)
		{
			INGApplication application = object.getDataAdapterList().getApplication();
			if (application.getRuntimeProperties().containsKey("NG2"))
			{
				clientConversion.convert("clientfunction");
				JSONUtils.addKeyIfPresent(writer, key);
				String uuid = application.registerClientFunction(object.getTagReplacedValue());
				writer.value(uuid);
				return writer;
			}
		}
		return super.toJSON(writer, key, object, pd, clientConversion, dataConverterContext);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		// should not be called, to template is not supported
		return writer;
	}
}
