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

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author gganea
 *
 */
public class FindModePropertyType extends DefaultPropertyType<FindModeSabloValue>
	implements IConvertedPropertyType<FindModeSabloValue>, IFormElementDefaultValueToSabloComponent<JSONObject, FindModeSabloValue>,
	ISabloComponentToRhino<FindModeSabloValue>, IRhinoToSabloComponent<FindModeSabloValue>, IFormElementToTemplateJSON<String, FindModeSabloValue>
{

	public static final FindModePropertyType INSTANCE = new FindModePropertyType();
	public static final String TYPE_NAME = "findmode";

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		HashMap<String, Object> forEntities = new HashMap<String, Object>();
		try
		{
			JSONObject forProperty = (JSONObject)config.opt("for"); //$NON-NLS-1$
			String[] names = ServoyJSONObject.getNames(forProperty);
			for (String propertyName : names)
			{
				forEntities.put(propertyName, forProperty.get(propertyName));
			}
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		return new FindModeConfig(forEntities);
	}

	@Override
	public FindModeSabloValue defaultValue(PropertyDescription pd)
	{
		return null; // toSabloComponentDefaultValue will be used instead
	}

	@Override
	public FindModeSabloValue fromJSON(Object newJSONValue, FindModeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		//we do not allow changes coming in from the client
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FindModeSabloValue sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		return sabloValue.toJSON(writer);
	}

	@Override
	public FindModeSabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new FindModeSabloValue((FindModeConfig)pd.getConfig(), dataAdapterList);
	}

	@Override
	public FindModeSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new FindModeSabloValue((FindModeConfig)pd.getConfig(), dataAdapterList);
	}


	@Override
	public boolean isValueAvailableInRhino(FindModeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return false;
	}

	@Override
	public Object toRhinoValue(FindModeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public FindModeSabloValue toSabloComponentValue(Object rhinoValue, FindModeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext componentOrService)
	{
		return previousComponentValue;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.value(Boolean.FALSE);
		return writer;
	}
}
