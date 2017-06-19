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
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;

/**
 * This is meant to replace FoundsetReferencePropertyTypeOld as that sends too much server side information to the client - and it's not always needed. (not sure it is ever really needed)
 * The idea is that you can send an unique string hash to the client representing a foundset value (that will probably be stored as a value in the service/component
 * model model as well as a FoundsetPropertyType) and then you can send that back to server through the same property type (for example as an argument to server side
 * scripting of that service/component) and on the server you get the real foundset it represents.
 *
 * @author acostescu
 */
public class FoundsetReferencePropertyType extends ReferencePropertyType<IFoundSetInternal> implements IPropertyConverterForBrowser<IFoundSetInternal>,
	IClassPropertyType<IFoundSetInternal>, IRhinoToSabloComponent<IFoundSetInternal>, ISabloComponentToRhino<IFoundSetInternal>
{

	public static final FoundsetReferencePropertyType INSTANCE = new FoundsetReferencePropertyType();
	public static final String TYPE_NAME = "foundsetRef";

	private FoundsetReferencePropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public IFoundSetInternal fromJSON(Object newValue, IFoundSetInternal previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof String)
		{
			return getReference((String)newValue);
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, IFoundSetInternal value, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.value(addReference(value));
		return writer;
	}

	@Override
	public Class<IFoundSetInternal> getTypeClass()
	{
		return IFoundSetInternal.class;
	}

	@Override
	public boolean isValueAvailableInRhino(IFoundSetInternal webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(IFoundSetInternal webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		return webComponentValue;
	}

	@Override
	public IFoundSetInternal toSabloComponentValue(Object rhinoValue, IFoundSetInternal previousComponentValue, PropertyDescription pd,
		IWebObjectContext componentOrService)
	{
		if (rhinoValue instanceof IFoundSetInternal)
		{
			return (IFoundSetInternal)rhinoValue;
		}
		return null;
	}

}
