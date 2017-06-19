/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.scripting.WebBaseFunction;
import com.servoy.j2db.util.Pair;

/**
 * This property type is meant as a way to send records from client side scripting to server side scripting of webcomponents or services.
 * From the rowId that is available in foundset property types - in their viewport, on the server side it can find the record if it's given the server side foundset.
 *
 * @author acostescu
 */
public class RowReferencePropertyType extends DefaultPropertyType<String> implements IPropertyConverterForBrowser<String>, ISabloComponentToRhino<String>
{

	public static final RowReferencePropertyType INSTANCE = new RowReferencePropertyType();
	public static final String TYPE_NAME = "rowRef";

	private RowReferencePropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public String fromJSON(Object newValue, String previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof String)
		{
			return (String)newValue;
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, String value, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.value(value);
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(String webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(final String webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		// TODO change this to another function class as it's not a call-to-client function in it's javadoc maybe? this is actually all on server
		return new WebBaseFunction(null)
		{

			@Override
			public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
			{
				IRecordInternal record = null;
				if (args != null && args.length == 1 && args[0] instanceof IFoundSetInternal)
				{
					Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(webComponentValue);
					int recordIndex = ((IFoundSetInternal)args[0]).getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());
					if (recordIndex != -1) record = ((IFoundSetInternal)args[0]).getRecord(recordIndex);
				}
				return record;
			}

		};
	}

}
