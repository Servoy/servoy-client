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
import org.mozilla.javascript.Function;
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
public class FunctionRefType extends UUIDReferencePropertyType<Function>
	implements IPropertyConverterForBrowser<Function>, IClassPropertyType<Function>
{

	private static final String FUNCTION_HASH = "functionhash"; //$NON-NLS-1$

	public static final FunctionRefType INSTANCE = new FunctionRefType();
	public static final String TYPE_NAME = "NativeFunction"; //$NON-NLS-1$

	private FunctionRefType()
	{
	}

	@Override
	public Function fromJSON(Object newJSONValue, Function previousSabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext context, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof JSONObject)
		{
			JSONObject jsonFunction = (JSONObject)newJSONValue;
			return getReference(jsonFunction.optString(FUNCTION_HASH));
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Function sabloValue, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();
		writer.key(FUNCTION_HASH).value(addReference(sabloValue));
		writer.key("svyType").value(getName()); // TODO is this "svyType" used anywhere? can it be removed?
		writer.endObject();
		return writer;
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Class<Function> getTypeClass()
	{
		return Function.class;
	}

}
