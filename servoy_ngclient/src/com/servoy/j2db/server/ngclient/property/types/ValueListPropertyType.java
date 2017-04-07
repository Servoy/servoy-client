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

import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.solutionmodel.JSValueList;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.ColumnBasedValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.INGWebObject;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Property type that handles valuelist typed properties.
 *
 * @author acostescu
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class ValueListPropertyType extends DefaultPropertyType<ValueListTypeSabloValue> implements IConvertedPropertyType<ValueListTypeSabloValue>,
	IFormElementToSabloComponent<Object, ValueListTypeSabloValue>, ISupportTemplateValue<Object>, IDataLinkedType<Object, ValueListTypeSabloValue>,
	IRhinoToSabloComponent<ValueListTypeSabloValue>, ISabloComponentToRhino<ValueListTypeSabloValue>, IPushToServerSpecialType, IRhinoDesignConverter
{

	public static final ValueListPropertyType INSTANCE = new ValueListPropertyType();
	public static final String TYPE_NAME = "valuelist";

	protected ValueListPropertyType()
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
		String dataprovider = "";
		String def = null;
		int max = Integer.MAX_VALUE;
		boolean logMax = true;
		if (json != null)
		{
			dataprovider = json.optString("for");
			def = json.optString("default");
			if (json.has("max")) max = json.optInt("max");
			if (json.has("tags"))
			{
				try
				{
					JSONObject tags = json.getJSONObject("tags");
					if (tags.has("logWhenOverMax")) logMax = tags.getBoolean("logWhenOverMax");
				}
				catch (JSONException e)
				{
					Debug.log(e);
				}
			}
		}
		return new ValueListConfig(dataprovider, def, max, logMax);
	}

	@Override
	public boolean valueInTemplate(Object object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}

	@Override
	public ValueListTypeSabloValue fromJSON(Object newJSONValue, ValueListTypeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// handle any valuelist specific websocket incomming traffic
		if (previousSabloValue != null && newJSONValue instanceof String)
		{
			// currently the only thing that can come from client is a filter request...
			previousSabloValue.filterValuelist((String)newJSONValue);
		}
		else Debug.error("Got a client update for valuelist property, but valuelist is null or value can't be interpreted: " + newJSONValue + ".");

		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ValueListTypeSabloValue sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			sabloValue.toJSON(writer, key, clientConversion);
		}
		return writer;
	}

	@Override
	public ValueListTypeSabloValue toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		ValueList val = null;
		IValueList valueList = null;
		ValueListConfig config = (ValueListConfig)pd.getConfig();
		String dataproviderID = (pd.getConfig() != null ? (String)formElement.getPropertyValue(config.getFor()) : null);

		valueList = getIValueList(formElementValue, pd, formElement, component, dataAdapterList, val, valueList, config, dataproviderID);

		return valueList != null ? new ValueListTypeSabloValue(valueList, dataAdapterList, config, dataproviderID, pd,
			getComponentFormat(pd, dataAdapterList, formElement, config, dataproviderID, component)) : null;
	}

	/**
	 * @param value
	 * @param pd
	 * @param formElement
	 * @param componentOrService
	 * @param dataAdapterList
	 * @param val
	 * @param valueList
	 * @param config
	 * @param dataproviderID
	 * @return
	 */
	protected IValueList getIValueList(Object value, PropertyDescription pd, INGFormElement formElement, INGWebObject componentOrService,
		DataAdapterList dataAdapterList, ValueList val, IValueList valueList, ValueListConfig config, String dataproviderID)
	{
		int valuelistID = Utils.getAsInteger(value);
		INGApplication application = dataAdapterList.getApplication();
		if (valuelistID > 0)
		{
			val = application.getFlattenedSolution().getValueList(valuelistID);
		}
		else
		{
			UUID uuid = Utils.getAsUUID(value, false);
			if (uuid != null) val = (ValueList)application.getFlattenedSolution().searchPersist(uuid);
			else if (value instanceof String) val = application.getFlattenedSolution().getValueList(value.toString());
		}


		if (val != null)
		{
			ComponentFormat fieldFormat = getComponentFormat(pd, dataAdapterList, formElement, config, dataproviderID, componentOrService);
			valueList = getRealValueList(application, val, fieldFormat, dataproviderID);
		}
		else
		{
			if ("autoVL".equals(config.getDefaultValue()))
			{
				String dp = (String)formElement.getPropertyValue(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName());
				IWebFormUI formUI = ((WebFormComponent)componentOrService.getUnderlyingWebObject()).findParent(WebFormUI.class);
				if (dp != null && formUI.getController().getTable() != null && formUI.getController().getTable().getColumnType(dp) != 0)
				{
					valueList = new ColumnBasedValueList(application, formElement.getForm().getServerName(), formElement.getForm().getTableName(),
						(String)formElement.getPropertyValue(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName()));
				}
				else
				{
					// not supported empty valuelist (based on relations) just return an empty valuelist
					valueList = new CustomValueList(application, null, "", false, IColumnTypes.TEXT, null);
				}
			}
		}
		return valueList;
	}

	protected ComponentFormat getComponentFormat(PropertyDescription pd, DataAdapterList dataAdapterList, INGFormElement formElement, ValueListConfig config,
		String dataproviderID, INGWebObject component)
	{
		ComponentFormat formatValue = null;
		INGApplication application = dataAdapterList.getApplication();
		if (dataproviderID != null)
		{
			Collection<PropertyDescription> properties = formElement != null ? formElement.getProperties(FormatPropertyType.INSTANCE)
				: component.getProperties(FormatPropertyType.INSTANCE);
			for (PropertyDescription formatPd : properties)
			{
				// compare whether format and valuelist property are for same property (dataprovider) or if format is used for valuelist property itself
				if (formatPd.getConfig() instanceof String[] && ((String[])formatPd.getConfig()).length > 0 &&
					(config.getFor().equals(((String[])formatPd.getConfig())[0]) || pd.getName().equals(((String[])formatPd.getConfig())[0])))
				{
					if (formElement != null)
					{
						Object v = formElement.getPropertyValue(formatPd.getName()); // form element value for format is a String
						if (v != IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER)
						{
							formatValue = ComponentFormat.getComponentFormat((String)v, dataproviderID,
								application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), dataAdapterList.getForm().getForm()),
								application);
							break;
						}
					}
					else
					{
						formatValue = (ComponentFormat)component.getProperty(formatPd.getName()); // here format is either null or already a ComponentFormat
						if (formatValue != null) break;
					}
				}
			}
		}

		if (formatValue == null)
		{
			// we didn't find any format; just create a new one (for default formatting I guess)
			formatValue = ComponentFormat.getComponentFormat(null, dataproviderID,
				application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), dataAdapterList.getForm().getForm()), application);
		}

		return formatValue;
	}

	protected IValueList getRealValueList(INGApplication application, ValueList val, ComponentFormat fieldFormat, String dataproviderID)
	{
		return com.servoy.j2db.component.ComponentFactory.getRealValueList(application, val, true, fieldFormat.dpType, fieldFormat.parsedFormat,
			dataproviderID);
	}

	@Override
	public TargetDataLinks getDataLinks(Object formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		if (formElementValue instanceof IValueList)
		{
			IDataProvider[] dependedDataProviders = ((IValueList)formElementValue).getDependedDataProviders();
			if (dependedDataProviders == null) return TargetDataLinks.NOT_LINKED_TO_DATA;
			if (dependedDataProviders.length == 0) return TargetDataLinks.LINKED_TO_ALL;

			boolean recordLinked = false;
			String[] dataproviders = new String[dependedDataProviders.length];
			for (int i = 0; i < dataproviders.length; i++)
			{
				dataproviders[i] = dependedDataProviders[i].getDataProviderID();
				recordLinked = recordLinked || (dependedDataProviders[i] instanceof IColumn || dependedDataProviders[i] instanceof ColumnWrapper);
			}
			return new TargetDataLinks(dataproviders, recordLinked);
		}
		return null;
	}

	@Override
	public ValueListTypeSabloValue toSabloComponentValue(Object rhinoValue, ValueListTypeSabloValue previousComponentValue, PropertyDescription pd,
		INGWebObject componentOrService)
	{
		if (previousComponentValue == null)
		{
			return rhinoValue instanceof String ? createValuelistByName(rhinoValue, pd, componentOrService) : null;
		}

		ValueListTypeSabloValue newValue = previousComponentValue;
		ParsedFormat format = null;
		int type = -1;
		INGApplication application = previousComponentValue.dataAdapterList.getApplication();
		IValueList list = previousComponentValue.getValueList();

		if (list.getName().equals(rhinoValue))
		{
			//no need to create a new value if we have the same valuelist name
			return previousComponentValue;
		}

		IValueList newVl = null;
		// see if it's a setValuelistItems equivalent
		if (list != null && list instanceof CustomValueList && (rhinoValue instanceof JSDataSet || rhinoValue instanceof IDataSet))
		{
			ValueList valuelist = application.getFlattenedSolution().getValueList(list.getName());
			if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
			{
				format = ((CustomValueList)list).getFormat();
				type = ((CustomValueList)list).getValueType();
				newVl = ValueListFactory.fillRealValueList(application, valuelist, IValueListConstants.CUSTOM_VALUES, format, type, rhinoValue);

				if (newVl != null)
				{
					ValueListConfig config = (ValueListConfig)pd.getConfig();
					Object dpPropertyValue = componentOrService.getProperty(config.getFor());
					String dataproviderID = DataAdapterList.getDataProviderID(dpPropertyValue);

					newValue = new ValueListTypeSabloValue(newVl, previousComponentValue.dataAdapterList, config, dataproviderID, pd,
						new ComponentFormat(format, type, type));
				}
			}
		}
		else if (rhinoValue instanceof String)
		{
			//the rhino value is a different valuelist name 
			newValue = createValuelistByName(rhinoValue, pd, componentOrService);
		}

		return newValue;

	}

	/**
	 * @param rhinoValue
	 * @param pd
	 * @param componentOrService
	 * @return
	 */
	private ValueListTypeSabloValue createValuelistByName(Object rhinoValue, PropertyDescription pd, INGWebObject componentOrService)
	{
		WebFormComponent component = (WebFormComponent)componentOrService.getUnderlyingWebObject();
		ValueList val = null;
		IValueList valueList = null;
		ValueListConfig config = (ValueListConfig)pd.getConfig();
		DataAdapterList dataAdapterList = (DataAdapterList)component.getDataAdapterList();
		String dataproviderID = (pd.getConfig() != null ? DataAdapterList.getDataProviderID(componentOrService.getProperty(config.getFor())) : null);
		valueList = getIValueList(rhinoValue, pd, null, componentOrService, dataAdapterList, val, valueList, config, dataproviderID);
		return valueList != null ? new ValueListTypeSabloValue(valueList, dataAdapterList, config, dataproviderID, pd,
			getComponentFormat(pd, dataAdapterList, null, config, dataproviderID, componentOrService)) : null;
	}

	@Override
	public boolean isValueAvailableInRhino(ValueListTypeSabloValue webComponentValue, PropertyDescription pd, INGWebObject componentOrService)
	{
		return webComponentValue != null;
	}

	@Override
	public Object toRhinoValue(ValueListTypeSabloValue webComponentValue, PropertyDescription pd, INGWebObject componentOrService, Scriptable startScriptable)
	{
		if (webComponentValue != null)
		{
			if (webComponentValue.valueList != null)
			{
				return webComponentValue.valueList.getName();
			}
		}
		return null;
	}

	@Override
	public boolean shouldAlwaysAllowIncommingJSON()
	{
		return true; // fromJSON can only filter stuff so it shouldn't be a problem (it's not setting anything from client to server)
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof String)
		{
			ValueList vl = application.getFlattenedSolution().getValueList((String)value);
			if (vl != null) return vl.getUUID().toString();
		}
		else if (value instanceof JSValueList)
		{
			return ((JSValueList)value).getUUID().toString();
		}
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		ValueList list = null;
		UUID uuid = Utils.getAsUUID(value, false);
		if (uuid != null)
		{
			list = (ValueList)application.getFlattenedSolution().searchPersist(uuid);
		}
		if (value instanceof String && list == null)
		{
			list = application.getFlattenedSolution().getValueList((String)value);
		}
		if (list != null)
		{
			return application.getScriptEngine().getSolutionModifier().getValueList(list.getName());
		}
		return null;
	}

}
