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
import org.sablo.specification.IYieldingType;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * TODO is format really mapped on a special object (like ParsedFormat)
 * maybe this should go into Servoy
 * @author jcompagner
 *
 */
public class FormatPropertyType extends DefaultPropertyType<Object> implements IConvertedPropertyType<Object>/* <ComponentFormat> */,
	ISupportTemplateValue<Object>, IFormElementDefaultValueToSabloComponent<Object, Object>, ISabloComponentToRhino<Object> /* <ComponentFormat */,
	IRhinoToSabloComponent<Object> /* <ComponentFormat */
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

	@SuppressWarnings("nls")
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
	public Object/* ComponentFormat */defaultValue(PropertyDescription pd)
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
		map.put("type", type);

		boolean isMask = format.parsedFormat.isMask();
		boolean isAllUppercase = format.parsedFormat.isAllUpperCase();
		boolean isAllLowercase = format.parsedFormat.isAllLowerCase();
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
			map.put("decimalSeparator", String.valueOf(dfs.getDecimalSeparator()));
			map.put("groupingSeparator", String.valueOf(dfs.getGroupingSeparator()));
			map.put("currencySymbol", dfs.getCurrencySymbol());
			map.put("percent", String.valueOf(dfs.getPercent()));
			map.put("maxLength", format.parsedFormat.getMaxLength() == null ? new Integer(-1) : format.parsedFormat.getMaxLength());
		}

		if (isAllUppercase) map.put("uppercase", Boolean.valueOf(isAllUppercase));
		else if (isAllLowercase) map.put("lowercase", Boolean.valueOf(isAllLowercase));

		return JSONUtils.toBrowserJSONFullValue(writer, key, map, null, clientConversion, null);
	}

	@Override
	public boolean valueInTemplate(Object object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}

	@Override
	public Object toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return getSabloValue(formElementValue, formElement, pd, component);
	}

	@Override
	public Object toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return getSabloValue(null, formElement, pd, component);
	}

	private Object getSabloValue(Object formElementValue, INGFormElement formElement, PropertyDescription pd, WebFormComponent component)
	{
		IApplication application = component.getDataConverterContext().getApplication();
		if (formElementValue == NGConversions.IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER || formElementValue == DESIGN_DEFAULT)
		{
			formElementValue = null;
		}

		if (formElementValue instanceof String || formElementValue == null)
		{
			String dataproviderId = null;
			if (pd.getConfig() instanceof String[])
			{
				for (String element : (String[])pd.getConfig())
				{
					PropertyDescription forProperty = formElement.getProperty(element);
					if (forProperty != null)
					{
						IPropertyType< ? > type = forProperty.getType();
						if (type instanceof IYieldingType< ? , ? >)
						{
							type = ((IYieldingType)type).getPossibleYieldType();
						}
						if (type instanceof DataproviderPropertyType)
						{
							dataproviderId = (String)formElement.getPropertyValue(element);
							break;
						}
						else if (type instanceof ValueListPropertyType)
						{
							Object id = formElement.getPropertyValue(element);
							int valuelistID = Utils.getAsInteger(id);
							ValueList val = null;
							if (valuelistID > 0)
							{
								val = application.getFlattenedSolution().getValueList(valuelistID);
							}
							else
							{
								UUID uuid = Utils.getAsUUID(id, false);
								if (uuid != null) val = (ValueList)application.getFlattenedSolution().searchPersist(uuid);
							}
							if (val != null)
							{
								int dpType = IColumnTypes.TEXT;
								IDataProvider dataProvider = null;
								ITable table;
								try
								{
									if (val.getRelationName() != null)
									{
										Relation[] relations = application.getFlattenedSolution().getRelationSequence(val.getRelationName());
										table = relations[relations.length - 1].getForeignTable();
									}
									else
									{
										table = val.getTable();
									}

									if (table != null)
									{
										String dp = null;
										int showDataProviders = val.getShowDataProviders();
										if (showDataProviders == 1)
										{
											dp = val.getDataProviderID1();
										}
										else if (showDataProviders == 2)
										{
											dp = val.getDataProviderID2();
										}
										else if (showDataProviders == 4)
										{
											dp = val.getDataProviderID3();
										}

										if (dp != null)
										{
											dataProvider = application.getFlattenedSolution().getDataProviderForTable((Table)table, dp);
										}
										if (dataProvider != null)
										{
											dpType = dataProvider.getDataProviderType();
										}
										return ComponentFormat.getComponentFormat((String)formElementValue, dpType, application);
									}
								}
								catch (Exception ex)
								{
									Debug.error(ex);
								}
							}
						}
					}
				}
			}
			ComponentFormat format = ComponentFormat.getComponentFormat(
				(String)formElementValue,
				dataproviderId,
				application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(),
					component.getDataConverterContext().getForm().getForm()), application);
			return format;
		}
		return formElementValue;
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		if (webComponentValue instanceof ComponentFormat)
		{
			return ((ComponentFormat)webComponentValue).parsedFormat.getFormatString();
		}
		return null;
	}

	@Override
	public Object toSabloComponentValue(Object rhinoValue, Object previousComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return getSabloValue(rhinoValue, ((WebFormComponent)componentOrService).getFormElement(), pd, (WebFormComponent)componentOrService);
	}
}
