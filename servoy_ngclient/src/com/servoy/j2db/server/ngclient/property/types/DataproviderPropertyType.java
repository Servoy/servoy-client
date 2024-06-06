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
import org.json.JSONString;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowserWithDynamicClientType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.ICanBeLinkedToFoundset;
import com.servoy.j2db.server.ngclient.property.NGComponentDALContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.ScopesUtils;

/**
 * @author jcompagner
 *
 */
public class DataproviderPropertyType extends DefaultPropertyType<DataproviderTypeSabloValue>
	implements IFormElementToSabloComponent<String, DataproviderTypeSabloValue>, IConvertedPropertyType<DataproviderTypeSabloValue>,
	ISupportTemplateValue<String>, ISabloComponentToRhino<DataproviderTypeSabloValue>, IRhinoToSabloComponent<DataproviderTypeSabloValue>,
	IDataLinkedType<String, DataproviderTypeSabloValue>, IFindModeAwareType<String, DataproviderTypeSabloValue>,
	ICanBeLinkedToFoundset<String, DataproviderTypeSabloValue>, IPropertyConverterForBrowserWithDynamicClientType<DataproviderTypeSabloValue>
{

	public static final DataproviderPropertyType INSTANCE = new DataproviderPropertyType();
	public static final String TYPE_NAME = "dataprovider"; //$NON-NLS-1$

	protected DataproviderPropertyType()
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
		// String forFoundSet = null; // see FoundsetLinkedPropertyType for how dataproviders linked to foundsets work
		boolean hasParseHtml = false;

		String displayTagsPropertyName = null;
		boolean displayTags = false;
		boolean resolveValuelist = false;
		if (json != null)
		{
			JSONObject onDataChangeObj = json.optJSONObject("ondatachange");
			if (onDataChangeObj != null)
			{
				onDataChange = onDataChangeObj.optString("onchange", null);
				onDataChangeCallback = onDataChangeObj.optString("callback", null);
			}
			hasParseHtml = json.has(HTMLStringPropertyType.CONFIG_OPTION_PARSEHTML) ? json.optBoolean(HTMLStringPropertyType.CONFIG_OPTION_PARSEHTML) : false;
			displayTagsPropertyName = json.optString(DataproviderConfig.DISPLAY_TAGS_PROPERTY_NAME_CONFIG_OPT, null);
			displayTags = json.has(DataproviderConfig.DISPLAY_TAGS_CONFIG_OPT) ? json.optBoolean(DataproviderConfig.DISPLAY_TAGS_CONFIG_OPT, false) : false;
			resolveValuelist = json.has(DataproviderConfig.RESOLVE_VALUELIST_CONFIG_OPT)
				? json.optBoolean(DataproviderConfig.RESOLVE_VALUELIST_CONFIG_OPT, false) : false;
		}

		return new DataproviderConfig(onDataChange, onDataChangeCallback, hasParseHtml, displayTagsPropertyName, displayTags, resolveValuelist);
	}

	@Override
	public TargetDataLinks getDataLinks(String formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement)
	{
		return getDataLinks(formElementValue, formElement.getForm(), flattenedSolution);
	}

	protected TargetDataLinks getDataLinks(String formElementValue, Form form, FlattenedSolution flattenedSolution)
	{
		// formElementValue is the data provider id string here
		if (formElementValue == null) return TargetDataLinks.NOT_LINKED_TO_DATA;

		// not linked for globals or form variables; linked for the rest - the rest should mean record based dataprovider
		boolean recordDP = !ScopesUtils.isVariableScope(formElementValue) && (form == null || form.getScriptVariable(formElementValue) == null);
		Relation[] relationSequence = null;
		if (recordDP)
		{
			// check if this is a related
			int index = formElementValue.lastIndexOf('.');
			if (index > 0 && index < formElementValue.length() - 1) //check if is related value request
			{
				String partName = formElementValue.substring(0, index);
				relationSequence = flattenedSolution.getRelationSequence(partName);
			}
		}
		// TODO - if it's global relation only, then record based constructor param should be false
		return new TargetDataLinks(new String[] { formElementValue }, recordDP, relationSequence);
	}


	@Override
	public boolean valueInTemplate(String object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false; // we have no value until the client is created for dataproviders
	}

	@Override
	public DataproviderTypeSabloValue toSabloComponentValue(String formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return formElementValue != null ? new DataproviderTypeSabloValue(formElementValue, dataAdapterList, component.getDataConverterContext(), pd) : null;
	}

	@Override
	public DataproviderTypeSabloValue fromJSON(Object newJSONValue, DataproviderTypeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdateReceived(newJSONValue, dataConverterContext);
		}
		return previousSabloValue;
	}

	@Override
	public JSONString toJSONWithDynamicClientSideType(JSONWriter writer, DataproviderTypeSabloValue sabloValue,
		PropertyDescription propertyDescription, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			IJSONStringWithClientSideType jsonValue = sabloValue.toJSON(dataConverterContext);

			writer.value(jsonValue);
			return jsonValue.getClientSideType();
		}
		else
		{
			writer.value(null);
			return null;
		}
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, DataproviderTypeSabloValue sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);

			IJSONStringWithClientSideType jsonValue = sabloValue.toJSON(dataConverterContext);

			if (jsonValue.getClientSideType() != null)
			{
				JSONUtils.writeConvertedValueWithClientType(writer, null, jsonValue.getClientSideType(), () -> {
					writer.value(jsonValue);
					return null;
				});
			}
			else writer.value(jsonValue);
		}
		else
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.value(null);
		}
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(DataproviderTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public DataproviderTypeSabloValue toSabloComponentValue(Object rhinoValue, DataproviderTypeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext webObjectContext)
	{
		if (rhinoValue == null || RhinoConversion.isUndefinedOrNotFound(rhinoValue)) return null;

		IDataAdapterList dal = NGComponentDALContext.getDataAdapterList(webObjectContext);
		if (rhinoValue instanceof String && !(previousComponentValue != null && rhinoValue.equals(previousComponentValue.getDataProviderID())) &&
			(previousComponentValue != null || dal != null))
		{
			// so it is a DPid string, not the same one that we had before and we have a place to take the dataAdapterList from; create a new value
			return new DataproviderTypeSabloValue((String)rhinoValue, dal != null ? dal : previousComponentValue.dataAdapterList,
				((WebFormComponent)webObjectContext.getUnderlyingWebObject()).getDataConverterContext(), pd);
		}
		return previousComponentValue;
	}

	@Override
	public Object toRhinoValue(DataproviderTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService,
		Scriptable startScriptable)
	{
		return webComponentValue != null ? webComponentValue.getDataProviderID() : null;
	}

	@Override
	public boolean isFindModeAware(String formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		return isFindModeAware(formElementValue, formElement.getForm(), flattenedSolution);
	}

	protected boolean isFindModeAware(String formElementValue, Form form, FlattenedSolution flattenedSolution)
	{
		if (formElementValue == null) return false;

		TargetDataLinks dataLinks = getDataLinks(formElementValue, form, flattenedSolution);
		return dataLinks.recordLinked;
	}

}
