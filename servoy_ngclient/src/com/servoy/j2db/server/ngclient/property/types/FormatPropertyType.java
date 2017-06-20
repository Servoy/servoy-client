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

import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IPropertyDescriptionProvider;
import org.sablo.IWebObjectContext;
import org.sablo.specification.IYieldingType;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;

/**
 * TODO is format really mapped on a special object (like ParsedFormat)
 * maybe this should go into Servoy
 *
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class FormatPropertyType extends DefaultPropertyType<FormatTypeSabloValue> implements IConvertedPropertyType<FormatTypeSabloValue>,
	ISupportTemplateValue<String>, IFormElementDefaultValueToSabloComponent<String, FormatTypeSabloValue>, ISabloComponentToRhino<FormatTypeSabloValue>,
	IRhinoToSabloComponent<FormatTypeSabloValue>, II18NPropertyType<FormatTypeSabloValue>
{

	private static final Logger log = LoggerFactory.getLogger(FormatPropertyType.class.getCanonicalName());

	public static final FormatPropertyType INSTANCE = new FormatPropertyType();
	public static final String TYPE_NAME = "format";

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
				Object object = json.get("for");
				if (object instanceof JSONArray)
				{
					JSONArray arr = (JSONArray)object;
					String[] retValue = new String[arr.length()];
					for (int i = 0; i < arr.length(); i++)
					{
						retValue[i] = arr.getString(i);
					}
					return retValue;
				}
				else if (object instanceof String)
				{
					return new String[] { (String)object };
				}
				return null;
			}
			catch (JSONException e)
			{
				log.error("JSONException", e);
			}
		}
		return "";
	}

	@Override
	public FormatTypeSabloValue defaultValue(PropertyDescription pd)
	{
		return null; // toSabloComponentDefaultValue will be used instead
	}

	@Override
	public FormatTypeSabloValue fromJSON(Object newValue, FormatTypeSabloValue previousValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// format property types cannot be changed from client
		return previousValue;
	}

	@Override
	// @formatter:off
	public JSONWriter toJSON(JSONWriter writer, String key, FormatTypeSabloValue formatValue,
		PropertyDescription pdd/* if this arg is needed in the future please also handle it in foundset property type! that uses null */,
		DataConversion cc/* if this arg is needed in the future please also handle it in foundset property type! then that will need to handle client side conversions as well */,
		IBrowserConverterContext dataConverterContext) throws JSONException
	// @formatter:on
	{
		if (formatValue == null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			return writer.value(null);
		}
		if (!formatValue.isInitialized())
		{
			Debug.warn("Trying to send to client an uninitialized format property: " + formatValue + " of " + dataConverterContext.getWebObject());
			return writer;
		}

		return writeComponentFormatToJSON(writer, key, formatValue.getComponentFormat(), cc, dataConverterContext);
	}

	public JSONWriter writeComponentFormatToJSON(JSONWriter writer, String key, ComponentFormat format, DataConversion dataConversions,
		IBrowserConverterContext dataConverterContext)
	{
		Map<String, Object> map = new HashMap<>();
		String type = Column.getDisplayTypeString(format.uiType);
		map.put("type", type);

		boolean isMask = format.parsedFormat.isMask();
		boolean isAllUppercase = format.parsedFormat.isAllUpperCase();
		boolean isAllLowercase = format.parsedFormat.isAllLowerCase();
		String placeHolder = null;
		if (format.parsedFormat.getPlaceHolderString() != null) placeHolder = format.parsedFormat.getPlaceHolderString();
		else if (format.parsedFormat.getPlaceHolderCharacter() != 0) placeHolder = Character.toString(format.parsedFormat.getPlaceHolderCharacter());
		String mask = format.parsedFormat.getEditFormat();
		if (isMask && type.equals("DATETIME"))
		{
			mask = format.parsedFormat.getDateMask();
			if (placeHolder == null) placeHolder = format.parsedFormat.getDisplayFormat();
		}
		else if (format.parsedFormat.getDisplayFormat() != null && type.equals("TEXT"))
		{
			isMask = true;
			mask = format.parsedFormat.getDisplayFormat();
		}
		map.put("isMask", Boolean.valueOf(isMask));
		map.put("edit", mask);
		map.put("placeHolder", placeHolder);
		map.put("allowedCharacters", format.parsedFormat.getAllowedCharacters());
		map.put("display", format.parsedFormat.getDisplayFormat());
		map.put("isNumberValidator", Boolean.valueOf(format.parsedFormat.isNumberValidator()));

		if (type.equals("NUMBER") || type.equals("INTEGER") || format.parsedFormat.isNumberValidator())
		{
			BaseWebObject webObject = dataConverterContext.getWebObject();
			Locale clientLocale;
			if (webObject instanceof IContextProvider)
			{
				clientLocale = ((IContextProvider)webObject).getDataConverterContext().getApplication().getLocale();
			}
			else
			{
				Debug.warn("Cannot get client locale for : " + webObject.toString() + " , using system default");
				clientLocale = Locale.getDefault();
			}
			DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(clientLocale);

			// the commented out values are already available client-side in numeral.languageData(); they are taken from there now
//			map.put("decimalSeparator", String.valueOf(dfs.getDecimalSeparator()));
//			map.put("groupingSeparator", String.valueOf(dfs.getGroupingSeparator()));
//			map.put("currencySymbol", dfs.getCurrencySymbol());

			map.put("percent", String.valueOf(dfs.getPercent()));
		}
		if (format.parsedFormat.getMaxLength() != null)
		{
			map.put("maxLength", format.parsedFormat.getMaxLength());
		}
		if (isAllUppercase) map.put("uppercase", Boolean.valueOf(isAllUppercase));
		else if (isAllLowercase) map.put("lowercase", Boolean.valueOf(isAllLowercase));

		return JSONUtils.toBrowserJSONFullValue(writer, key, map, null, dataConversions, null); // here dataConversions is not used I think, could be null
	}

	@Override
	public boolean valueInTemplate(String formElementValue, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}

	@Override
	public FormatTypeSabloValue toSabloComponentValue(String formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		// we have all the info we need in the form element; we don't need here the runtime values for valuelist, dp or foundset properties if they are specified in .spec
		FormatPropertyDependencies propertyDependencies = getPropertyDependencies(pd, formElement);
		String dataproviderID = null;
		Object valuelistID = null;
		String foundsetID = null;

		if (propertyDependencies.dataproviderPropertyName != null)
		{
			dataproviderID = (String)formElement.getPropertyValue(propertyDependencies.dataproviderPropertyName);

			// if it is a dataprovider type. look if it is foundset linked
			if (propertyDependencies.foundsetPropertyName != null)
			{
				JSONObject formElementValOfFoundset = (JSONObject)formElement.getPropertyValue(propertyDependencies.foundsetPropertyName);
				if (formElementValOfFoundset != null) foundsetID = formElementValOfFoundset.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
			}
		}

		if (propertyDependencies.valueListPropertyName != null) valuelistID = formElement.getPropertyValue(propertyDependencies.valueListPropertyName);

		return new FormatTypeSabloValue(formElementValue, propertyDependencies, dataproviderID, valuelistID, foundsetID, component);
	}

	@Override
	public FormatTypeSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return toSabloComponentValue(null, pd, formElement, component, dataAdapterList);
	}

	private FormatPropertyDependencies getPropertyDependencies(PropertyDescription pd, IPropertyDescriptionProvider pdProvider)
	{
		String forDataproviderPropertyName = null;
		String forFoundsetPropertyName = null;
		String forValuelistPropertyName = null;

		if (pd.getConfig() instanceof String[])
		{
			for (String dependency : (String[])pd.getConfig())
			{
				// IMPORTANT: here we iterate over the for: configs to identify any dataprovider or valuelist properties that this format is meant for
				//
				// if you have for: [valuelist, dataprovider] then 2 things can happen:
				// - valuelist if it has both real and display values - forces the type; it is either TEXT (custom vl., global method vl.) or the 'display' column type in case it's a DB valuelist
				// - valuelist if not real/display but only one kind of values: here it is required in docs in the spec file that the valuelist property also defines "for": dataprovider if format
				//   defines both "for" valuelist and "for" dataprovider => valuelist doesn't force the type and then the dataprovider will decide the type
				//
				// if you have just for: dataprovider the the dataprovider property determines the type
				// if you have just for: valuelist (TODO) - this is currently not properly supported - as here we should get the type always from the VL (for both display and real values) - as we don't have a dataprovider to fall back on

				PropertyDescription forProperty = pdProvider.getPropertyDescription(dependency);
				if (forProperty != null)
				{
					IPropertyType< ? > type = forProperty.getType();
					if (type instanceof IYieldingType< ? , ? >)
					{
						type = ((IYieldingType)type).getPossibleYieldType();
					}
					if (type instanceof DataproviderPropertyType)
					{
						if (forDataproviderPropertyName == null)
						{
							forDataproviderPropertyName = dependency;

							// see if it's foundset linked
							Object config = forProperty.getConfig();
							if (config instanceof FoundsetLinkedConfig && ((FoundsetLinkedConfig)config).getForFoundsetName() != null)
							{
								forFoundsetPropertyName = ((FoundsetLinkedConfig)config).getForFoundsetName();
							}
						}
						else Debug.warn(
							"Format property '" + pd + " declares in .spec file to be for more then one dataprovider property; this is incorrect. (" +
								forDataproviderPropertyName + "," + dependency + ")");
					}
					else if (type instanceof ValueListPropertyType)
					{
						if (forValuelistPropertyName == null) forValuelistPropertyName = dependency;
						else Debug.warn("Format property '" + pd + " declares in .spec file to be for more then one valuelist property; this is incorrect. (" +
							forDataproviderPropertyName + "," + dependency + ")");
					}
				}
			}
		}

		return new FormatPropertyDependencies(forDataproviderPropertyName, forFoundsetPropertyName, forValuelistPropertyName);
	}

	@Override
	public boolean isValueAvailableInRhino(FormatTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(FormatTypeSabloValue formatValue, PropertyDescription pd, IWebObjectContext webObjectContext, Scriptable startScriptable)
	{
		if (formatValue != null)
		{
			if (!formatValue.isInitialized())
			{
				Debug.warn("Trying to send to client an uninitialized format property: " + formatValue + "(" + pd + ") of " + webObjectContext);
			}
			else return formatValue.getComponentFormat().parsedFormat.getFormatString();
		}
		return null;
	}

	@Override
	public FormatTypeSabloValue toSabloComponentValue(Object rhinoValue, FormatTypeSabloValue previousComponentValue, PropertyDescription formatPD,
		IWebObjectContext webObjectContext)
	{
		// we only accept strings or nulls from Rhino
		if (rhinoValue != null && !(rhinoValue instanceof String))
			throw new IllegalArgumentException("You can only assing a string as format for format property types: " + rhinoValue + " - " + formatPD.getName() +
				" - " + webObjectContext.getUnderlyingWebObject().getName());

		return new FormatTypeSabloValue((String)rhinoValue, getPropertyDependencies(formatPD, webObjectContext));
	}


	/**
	 * Just a container for keeping names of properties that a format property can depend on based on the .spec file.
	 *
	 * @author acostescu
	 */
	protected static class FormatPropertyDependencies
	{

		public final String dataproviderPropertyName;
		public final String foundsetPropertyName;
		public final String valueListPropertyName;

		public FormatPropertyDependencies(String dataproviderPropertyName, String foundsetPropertyName, String valueListPropertyName)
		{
			this.dataproviderPropertyName = dataproviderPropertyName;
			this.valueListPropertyName = valueListPropertyName;
			this.foundsetPropertyName = foundsetPropertyName;
		}
	}

	@Override
	public FormatTypeSabloValue resetI18nValue(FormatTypeSabloValue value, PropertyDescription pd, WebFormComponent component)
	{
		if (value != null)
		{
			value.resetI18nValue();
		}
		return value;
	}

}
