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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;

/**
 * TODO is format really mapped on a special object (like ParsedFormat)
 * maybe this should go into Servoy
 * @author jcompagner
 *
 */
public class FormatPropertyType extends DefaultPropertyType<Object> implements IConvertedPropertyType<Object>/* <ComponentFormat> */,
	ISupportTemplateValue<Object>, IFormElementDefaultValueToSabloComponent<Object, Object>
{

	private static final Logger log = LoggerFactory.getLogger(FormatPropertyType.class.getCanonicalName());

	public static final FormatPropertyType INSTANCE = new FormatPropertyType();
	public static final String TYPE_NAME = "format";
	private static Object DESIGN_DEFAULT = new Object();

	private FormatPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{

		if (json != null && json.has("for"))
		{
			try
			{
				return json.getString("for");
			}
			catch (JSONException e)
			{
				log.error("JSONException", e);
			}
		}
		return "";
	}

	@Override
	public Object/* ComponentFormat */defaultValue()
	{
		return DESIGN_DEFAULT;
	}

	@Override
	public Object/* ComponentFormat */fromJSON(Object newValue, Object/* ComponentFormat */previousValue, IDataConverterContext dataConverterContext)
	{
		// TODO remove when these types are design-aware and we know exactly how to deal with FormElement values (a refactor is to be done soon)
		return newValue;

		// ?
//		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object/* ComponentFormat */formatValue, DataConversion clientConversion,
		IDataConverterContext dataConverterContext) throws JSONException
	{
		ComponentFormat format;
		if (formatValue == null || formatValue instanceof String)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			return writer.value(null);
		}
		format = (ComponentFormat)formatValue;

		Map<String, Object> map = new HashMap<>();
		String type = Column.getDisplayTypeString(format.uiType);
		if (type.equals("INTEGER")) type = "NUMBER";
		map.put("type", type);

		boolean isMask = format.parsedFormat.isMask();
		String mask = format.parsedFormat.getEditFormat();
		if (isMask && type.equals("DATETIME"))
		{
			mask = format.parsedFormat.getDateMask();
		}
		else if (format.parsedFormat.getDisplayFormat() != null && type.equals("TEXT"))
		{
			isMask = true;
			mask = format.parsedFormat.getDisplayFormat();
		}
		String placeHolder = null;
		if (format.parsedFormat.getPlaceHolderString() != null) placeHolder = format.parsedFormat.getPlaceHolderString();
		else if (format.parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(format.parsedFormat.getPlaceHolderCharacter());
		map.put("isMask", Boolean.valueOf(isMask));
		map.put("edit", mask);
		map.put("placeHolder", placeHolder);
		map.put("allowedCharacters", format.parsedFormat.getAllowedCharacters());
		map.put("display", format.parsedFormat.getDisplayFormat());

		return JSONUtils.toBrowserJSONFullValue(writer, key, map, null, clientConversion, null);
	}

	@Override
	public boolean valueInTemplate(Object object)
	{
		return false;
	}

	@Override
	public Object toSabloComponentValue(Object formElementValue, PropertyDescription pd, FormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return getSabloValue(formElementValue, pd, formElement, dataAdapterList);
	}

	@Override
	public Object toSabloComponentDefaultValue(PropertyDescription pd, FormElement formElement, WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return getSabloValue(null, pd, formElement, dataAdapterList);
	}

	private Object getSabloValue(Object formElementValue, PropertyDescription pd, FormElement formElement, DataAdapterList dataAdapterList)
	{
		if (formElementValue == NGConversions.IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER || formElementValue == DESIGN_DEFAULT)
		{
			formElementValue = null;
		}

		// for now ignore format for elements with valuelist, as those may have different display value type
		if (formElement.getPropertyValue("valuelistID") == null && (formElementValue instanceof String || formElementValue == null))
		{
			// get dataproviderId
			String dataproviderId = (String)formElement.getPropertyValue((String)pd.getConfig());
			ComponentFormat format = ComponentFormat.getComponentFormat(
				(String)formElementValue,
				dataproviderId,
				dataAdapterList.getApplication().getFlattenedSolution().getDataproviderLookup(dataAdapterList.getApplication().getFoundSetManager(),
					dataAdapterList.getForm().getForm()), dataAdapterList.getApplication());
			return format;
		}
		return formElementValue;
	}

	// TODO implement RHINO conversion interfaces for JS access to the property on server; recreate ComponentFormat object (it has quite a lot of dependencies, application, pesist  etc)
	// see case SVY-7527

}
