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
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;

/**
 * A type for string representation of JS code set on the server that is meant to run directly on client/inside the browser.
 *
 * In NG1 it remains to operate just like a TagStringPropertyType - and client will call JS eval on the property's string value.</br>
 * But in NG2 this is smarter and avoids the need to an eval client-side; it just sends an UUID and generates a script file (identified based on that UUID on client)
 * from the actual function string (prop. value).
 *
 * @author jcompagner
 * @since 2021.03
 */
@SuppressWarnings("nls")
public class ClientFunctionPropertyType extends TagStringPropertyType implements IPropertyWithClientSideConversions<BasicTagStringTypeSabloValue>
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
	public JSONWriter toJSON(JSONWriter writer, String key, BasicTagStringTypeSabloValue object, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (object != null)
		{
			INGApplication application = null;

			if (object.getDataAdapterList() != null) application = object.getDataAdapterList().getApplication();
			else application = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication();

			if (application.getRuntimeProperties().containsKey("NG2"))
			{
				JSONUtils.addKeyIfPresent(writer, key);
				String uuid = application.registerClientFunction(object.getTagReplacedValueForClient(dataConverterContext.getComputedPushToServerValue()));
				writer.value(uuid);
				return writer;
			}
		}
		return super.toJSON(writer, key, object, pd, dataConverterContext);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		// should not be called, to template is not supported
		return writer;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		// we don't (currently) have here the application instance, so we can't not send the type for NG1; but NG1 has a dummy impl. of client side type
		// that will not do anything with the value, just return it unconverted as it comes from the server;
		// NG2 does need the client-side conversion
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(CLIENT_FUNCTION_TYPE_NAME);
		return true;
	}

}
