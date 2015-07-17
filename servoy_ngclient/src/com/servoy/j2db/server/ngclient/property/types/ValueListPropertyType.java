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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.ColumnBasedValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
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
	IRhinoToSabloComponent<ValueListTypeSabloValue>, ISabloComponentToRhino<ValueListTypeSabloValue>
{

	public static final ValueListPropertyType INSTANCE = new ValueListPropertyType();
	public static final String TYPE_NAME = "valuelist";

	private ValueListPropertyType()
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
		boolean canOptimize = true;
		if (json != null)
		{
			dataprovider = json.optString("for");
			def = json.optString("default");
			canOptimize = json.optBoolean("canOptimize", true);
		}
		return new ValueListConfig(dataprovider, def, canOptimize);
	}

	@Override
	public boolean valueInTemplate(Object object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return false;
	}

	@Override
	public ValueListTypeSabloValue fromJSON(Object newJSONValue, ValueListTypeSabloValue previousSabloValue, IDataConverterContext dataConverterContext)
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
	public JSONWriter toJSON(JSONWriter writer, String key, ValueListTypeSabloValue sabloValue, DataConversion clientConversion,
		IDataConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			boolean checkIfChanged = false;
			// TODO we should have type info here to send instead of null for real/display values
			if (dataConverterContext != null)
			{
				BaseWebObject webObject = dataConverterContext.getWebObject();
				// if not writing properties, then it means the valuelist is already on the client, so
				// send it only if it is changed
				if (webObject instanceof WebFormComponent)
				{
					WebFormComponent wfc = (WebFormComponent)webObject;
					int formViewType = wfc.getFormElement().getForm().getView();
					if (sabloValue.config.canOptimize() &&
						(formViewType == IFormConstants.VIEW_TYPE_RECORD || formViewType == IFormConstants.VIEW_TYPE_RECORD_LOCKED) &&
						!wfc.isWritingComponentProperties())
					{
						checkIfChanged = true;
					}
				}
			}
			sabloValue.toJSON(writer, key, clientConversion, checkIfChanged);
		}
		return writer;
	}

	@Override
	public ValueListTypeSabloValue toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		ValueList val = null;
		IValueList valueList = null;

		int valuelistID = Utils.getAsInteger(formElementValue);
		INGApplication application = dataAdapterList.getApplication();
		if (valuelistID > 0)
		{
			val = application.getFlattenedSolution().getValueList(valuelistID);
		}
		else
		{
			UUID uuid = Utils.getAsUUID(formElementValue, false);
			if (uuid != null) val = (ValueList)application.getFlattenedSolution().searchPersist(uuid);
		}

		ValueListConfig config = (ValueListConfig)pd.getConfig();
		String dataproviderID = (pd.getConfig() != null ? (String)formElement.getPropertyValue(config.getFor()) : null);

		if (val != null)
		{
			String format = null;
			if (dataproviderID != null)
			{
				Collection<PropertyDescription> properties = formElement.getProperties(FormatPropertyType.INSTANCE);
				for (PropertyDescription formatPd : properties)
				{
					// compare whether format and valuelist property are for same property (dataprovider) or if format is used for valuelist property itself
					if (formatPd.getConfig() instanceof String[] && ((String[])formatPd.getConfig()).length > 0 &&
						(config.getFor().equals(((String[])formatPd.getConfig())[0]) || pd.getName().equals(((String[])formatPd.getConfig())[0])))
					{
						format = (String)formElement.getPropertyValue(formatPd.getName());
						break;
					}
				}
			}
			ComponentFormat fieldFormat = ComponentFormat.getComponentFormat(format, dataproviderID,
				application.getFlattenedSolution().getDataproviderLookup(application.getFoundSetManager(), dataAdapterList.getForm().getForm()), application);
			valueList = com.servoy.j2db.component.ComponentFactory.getRealValueList(application, val, true, fieldFormat.dpType, fieldFormat.parsedFormat,
				dataproviderID);
		}
		else
		{
			if ("autoVL".equals(config.getDefaultValue()))
			{
				String dp = (String)formElement.getPropertyValue(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName());
				IWebFormUI formUI = component.findParent(WebFormUI.class);
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

		return valueList != null ? new ValueListTypeSabloValue(valueList, dataAdapterList, config, dataproviderID, pd) : null;
	}

	@Override
	public TargetDataLinks getDataLinks(Object formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement)
	{
		if (formElementValue instanceof IValueList)
		{
			return (((IValueList)formElementValue).isRecordLinked()) ? TargetDataLinks.LINKED_TO_ALL : TargetDataLinks.NOT_LINKED_TO_DATA;
		}
		return null;
	}

	@Override
	public ValueListTypeSabloValue toSabloComponentValue(Object rhinoValue, ValueListTypeSabloValue previousComponentValue, PropertyDescription pd,
		BaseWebObject componentOrService)
	{
		Object vl = componentOrService.getProperty(pd.getName());
		if (vl != null)
		{
			ValueListTypeSabloValue value = (ValueListTypeSabloValue)vl;
			INGApplication application = value.dataAdapterList.getApplication();
			IValueList list = value.getValueList();
			IValueList newVl = null;
			if (list != null && list instanceof CustomValueList && (rhinoValue instanceof JSDataSet || rhinoValue instanceof IDataSet))
			{
				String name = list.getName();
				ValueList valuelist = application.getFlattenedSolution().getValueList(name);
				if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
				{
					ParsedFormat format = ((CustomValueList)list).getFormat();
					int type = ((CustomValueList)list).getValueType();
					newVl = ValueListFactory.fillRealValueList(application, valuelist, IValueListConstants.CUSTOM_VALUES, format, type, rhinoValue);
				}
			}

			ValueListConfig config = (ValueListConfig)pd.getConfig();
			String dataproviderID = (componentOrService.getProperty(config.getFor()) != null
				? ((DataproviderTypeSabloValue)componentOrService.getProperty(config.getFor())).getDataProviderID() : null);
			return newVl != null ? new ValueListTypeSabloValue(newVl, value.dataAdapterList, config, dataproviderID, pd) : previousComponentValue;
		}
		return null;
	}

	@Override
	public boolean isValueAvailableInRhino(ValueListTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return webComponentValue != null;
	}

	@Override
	public Object toRhinoValue(ValueListTypeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		if (webComponentValue != null)
		{
			try
			{
				INGApplication application = webComponentValue.dataAdapterList.getApplication();
				if (webComponentValue.valueList != null)
				{
					List<Object[]> rows = new ArrayList<Object[]>();
					for (int i = 0; i < webComponentValue.valueList.getSize(); i++)
					{
						rows.add(new Object[] { webComponentValue.valueList.getElementAt(i), webComponentValue.valueList.getRealElementAt(i) });
					}
					return new JSDataSet(application, new BufferedDataSet(new String[] { "displayValue", "realValue" }, //$NON-NLS-1$ //$NON-NLS-2$
						rows));
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

}
