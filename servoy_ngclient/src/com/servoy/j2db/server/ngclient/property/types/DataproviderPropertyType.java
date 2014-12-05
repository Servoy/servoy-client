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
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.ScopesUtils;

/**
 * @author jcompagner
 *
 */
public class DataproviderPropertyType implements IFormElementToSabloComponent<String, DataproviderTypeSabloValue>,
	IConvertedPropertyType<DataproviderTypeSabloValue>, ISupportTemplateValue<String>, IDataLinkedType<String, DataproviderTypeSabloValue>,
	ISabloComponentToRhino<DataproviderTypeSabloValue>, IRhinoToSabloComponent<DataproviderTypeSabloValue>
{

	public static final DataproviderPropertyType INSTANCE = new DataproviderPropertyType();
	public static final String TYPE_NAME = "dataprovider"; //$NON-NLS-1$

	private DataproviderPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	@SuppressWarnings("nls")
	public Object parseConfig(JSONObject json)
	{
		String onDataChange = null;
		String onDataChangeCallback = null;
		String forFoundSet = null;
		boolean hasParseHtml = false;
		if (json != null)
		{
			JSONObject onDataChangeObj = json.optJSONObject("ondatachange");
			if (onDataChangeObj != null)
			{
				onDataChange = onDataChangeObj.optString("onchange", null);
				onDataChangeCallback = onDataChangeObj.optString("callback", null);
			}
			hasParseHtml = json.optBoolean(HTMLStringPropertyType.CONFIG_OPTION_PARSEHTML);
			forFoundSet = json.optString("forFoundSet");
		}

		return new DataproviderConfig(onDataChange, onDataChangeCallback, forFoundSet, hasParseHtml);
	}

	@Override
	public TargetDataLinks getDataLinks(String formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		// formElementValue is the data provider id string here
		if (formElementValue == null) return TargetDataLinks.NOT_LINKED_TO_DATA;

		// with '.' it means related record access through dataprovider (because for now I see that we don't support JS variables with dots in dataproviders - or we always treat them as related so that won't work)
		// TODO - if it's global relation only, then record based constructor param should be false
		if (formElementValue.contains(".")) return new TargetDataLinks(new String[] { formElementValue }, true /* TODO or false */); //$NON-NLS-1$

		// not linked for globals or form variables; linked for the rest - the rest should mean record based dataprovider
		boolean recordDP = !ScopesUtils.isVariableScope(formElementValue) && formElement.getForm().getScriptVariable(formElementValue) == null;
		return new TargetDataLinks(new String[] { formElementValue }, recordDP);
	}

	@Override
	public boolean valueInTemplate(String object)
	{
		return false; // we have no value until the client is created for dataproviders
	}

	@Override
	public DataproviderTypeSabloValue toSabloComponentValue(String formElementValue, PropertyDescription pd, FormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return formElementValue != null ? new DataproviderTypeSabloValue(formElementValue, dataAdapterList, component, (DataproviderConfig)pd.getConfig())
			: null;
	}

	@Override
	public DataproviderTypeSabloValue fromJSON(Object newJSONValue, DataproviderTypeSabloValue previousSabloValue, IDataConverterContext dataConverterContext)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdateReceived(newJSONValue, dataConverterContext);
		}
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, DataproviderTypeSabloValue sabloValue, DataConversion clientConversion,
		IDataConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			sabloValue.toJSON(writer, key, clientConversion, dataConverterContext);
		}
		else
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.value(null);
		}
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(DataproviderTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public DataproviderTypeSabloValue toSabloComponentValue(Object rhinoValue, DataproviderTypeSabloValue previousComponentValue, PropertyDescription pd,
		BaseWebObject componentOrService)
	{
		return previousComponentValue; // the property is read-only in Rhino
	}

	@Override
	public Object toRhinoValue(DataproviderTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService,
		Scriptable startScriptable)
	{
		return webComponentValue != null ? webComponentValue.getDataProviderID() : null;
	}

	@Override
	public DataproviderTypeSabloValue defaultValue()
	{
		return null;
	}

}
