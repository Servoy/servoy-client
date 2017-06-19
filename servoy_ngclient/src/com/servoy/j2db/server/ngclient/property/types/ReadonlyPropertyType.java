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

/**
 * @author gganea
 *
 */
public class ReadonlyPropertyType extends DefaultPropertyType<ReadonlySabloValue>
	implements IConvertedPropertyType<ReadonlySabloValue>, IFormElementDefaultValueToSabloComponent<JSONObject, ReadonlySabloValue>,
	ISabloComponentToRhino<ReadonlySabloValue>, IRhinoToSabloComponent<ReadonlySabloValue>, IFormElementToTemplateJSON<String, ReadonlySabloValue>
{

	public static final ReadonlyPropertyType INSTANCE = new ReadonlyPropertyType();
	public static final String TYPE_NAME = "readOnly";

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public ReadonlyConfig parseConfig(JSONObject json)
	{
		ReadonlyConfig config = ReadonlyConfig.parse(json);
		if (config.getOppositeOf() == null)
		{
			throw new RuntimeException("Readonly property must also provide the 'oppositeOf' value. Please use type 'protected' instead.");
		}
		return config;
	}

	@Override
	public ReadonlySabloValue fromJSON(Object newJSONValue, ReadonlySabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ReadonlySabloValue sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		return sabloValue.toJSON(writer);
	}

	@Override
	public ReadonlySabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new ReadonlySabloValue((ReadonlyConfig)pd.getConfig(), !(Boolean)formElement.getPropertyValue(((ReadonlyConfig)pd.getConfig()).getOppositeOf()));
	}

	@Override
	public ReadonlySabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new ReadonlySabloValue((ReadonlyConfig)pd.getConfig(), !(Boolean)formElement.getPropertyValue(((ReadonlyConfig)pd.getConfig()).getOppositeOf()));
	}

	@Override
	public ReadonlySabloValue defaultValue(PropertyDescription pd)
	{
		return null; // toSabloComponentDefaultValue will be used instead
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException

	{
		JSONUtils.addKeyIfPresent(writer, key);
		Boolean propertyValue = (Boolean)formElementContext.getFormElement().getPropertyValue(((ReadonlyConfig)pd.getConfig()).getOppositeOf());
		writer.value(!propertyValue);
		return writer;
	}

	@SuppressWarnings("boxing")
	@Override
	public ReadonlySabloValue toSabloComponentValue(Object rhinoValue, ReadonlySabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext componentOrService)
	{
		return new ReadonlySabloValue((ReadonlyConfig)pd.getConfig(), (Boolean)rhinoValue, previousComponentValue.getOldOppositeOfValue());
	}

	@Override
	public boolean isValueAvailableInRhino(ReadonlySabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@SuppressWarnings("boxing")
	@Override
	public Object toRhinoValue(ReadonlySabloValue webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService, Scriptable startScriptable)
	{
		return webComponentValue.getValue();//
	}

}
