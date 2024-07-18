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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IPropertyDescriptionProvider;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.specification.property.ISupportsGranularUpdates;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.solutionmodel.JSValueList;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.ICanBeLinkedToFoundset;
import com.servoy.j2db.server.ngclient.property.NGComponentDALContext;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignerDefaultWriter;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IRhinoDesignConverter;

/**
 * Property type that handles valuelist typed properties.
 *
 * @author acostescu
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class ValueListPropertyType extends DefaultPropertyType<ValueListTypeSabloValue>
	implements IConvertedPropertyType<ValueListTypeSabloValue>, IFormElementToSabloComponent<Object, ValueListTypeSabloValue>, ISupportTemplateValue<Object>,
	IDataLinkedType<Object, ValueListTypeSabloValue>, IRhinoToSabloComponent<ValueListTypeSabloValue>, ISabloComponentToRhino<ValueListTypeSabloValue>,
	IPushToServerSpecialType, IRhinoDesignConverter, II18NPropertyType<ValueListTypeSabloValue>, ICanBeLinkedToFoundset<Object, ValueListTypeSabloValue>,
	ISupportsGranularUpdates<ValueListTypeSabloValue>, IPropertyWithClientSideConversions<ValueListTypeSabloValue>, IDesignerDefaultWriter
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
		boolean lazyLoading = false;
		String configPropertyName = null;
		if (json != null)
		{
			dataprovider = json.optString("for");
			def = json.optString("default");
			if (json.has("max")) max = json.optInt("max");
			if (json.has("lazyLoading")) lazyLoading = json.optBoolean("lazyLoading");
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
			configPropertyName = json.optString("config", null);
		}
		return new ValueListConfig(dataprovider, def, max, logMax, lazyLoading, configPropertyName);
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
		if (previousSabloValue != null && (newJSONValue instanceof JSONObject))
		{
			// currently the only thing that can come from client is a filter request...
			previousSabloValue.fromJSON((JSONObject)newJSONValue);
		}
		else Debug.error("Got a client update for valuelist property, but valuelist is null or value can't be interpreted: " + newJSONValue + ".");

		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ValueListTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			sabloValue.toJSON(writer, key, dataConverterContext);
		}
		return writer;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter writer, String key, ValueListTypeSabloValue sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			sabloValue.changesToJSON(writer, key, dataConverterContext);
		}
		return writer;
	}

	@Override
	public ValueListTypeSabloValue toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		if (formElementValue != null)
		{
			if (formElement != null && formElement.getPersistIfAvailable() instanceof Field && "0".equals(formElementValue.toString()))
			{
				// legacy behavior, we cannot set null in repository, we set 0 for no value (if different than default)
				return null;
			}
			ValuelistPropertyDependencies propertyDependencies = getDependenciesToOtherProperties(pd, formElement);
			return new ValueListTypeSabloValue(formElementValue, pd, propertyDependencies,
				propertyDependencies.dataproviderPropertyName != null && formElement.getPropertyValue(propertyDependencies.dataproviderPropertyName) != null,
				propertyDependencies.formatPropertyName != null && formElement.getPropertyValue(propertyDependencies.formatPropertyName) != null,
				dataAdapterList);
		}
		return null;
	}

	protected ValuelistPropertyDependencies getDependenciesToOtherProperties(PropertyDescription pd, IPropertyDescriptionProvider propertyDescriptionProvider)
	{
		ValueListConfig config = (ValueListConfig)pd.getConfig();
		String dataproviderPropertyName = config.getFor();

		String foundsetPropertyName = null;
		String formatPropertyName = null; // this is really only used I think when you have a custom valuelist with date values (without separate display values) - to convert the String defined dates in the custom valuelist into actual Date values
		boolean dataproviderResolveValuelist = false;
		if (dataproviderPropertyName != null)
		{
			PropertyDescription dpPropertyDef = propertyDescriptionProvider.getPropertyDescription(dataproviderPropertyName);
			Object dpConfig = null;
			if (dpPropertyDef != null)
			{
				dpConfig = dpPropertyDef.getConfig();
			}
			if (dpPropertyDef != null && (dpPropertyDef.getType() instanceof FoundsetLinkedPropertyType))
			{
				foundsetPropertyName = ((FoundsetLinkedConfig)dpPropertyDef.getConfig()).getForFoundsetName();
				dpConfig = ((FoundsetLinkedConfig)dpPropertyDef.getConfig()).getWrappedPropertyDescription().getConfig();
			}
			if (dpConfig instanceof DataproviderConfig && ((DataproviderConfig)dpConfig).shouldResolveValuelist())
			{
				dataproviderResolveValuelist = true;
			}
		}

		Collection<PropertyDescription> properties = propertyDescriptionProvider.getProperties(FormatPropertyType.INSTANCE);
		for (PropertyDescription formatPd : properties)
		{
			// compare whether format and valueList property are for same property (dataprovider) or if format is used for valuelist property itself
			if (formatPd.getConfig() instanceof String[] && ((String[])formatPd.getConfig()).length > 0)
			{
				for (String formatForClauseEntry : ((String[])formatPd.getConfig()))
				{
					if (dataproviderPropertyName.equals(formatForClauseEntry) || pd.getName().equals(formatForClauseEntry))
					{
						formatPropertyName = formatPd.getName();
						break;
					}
				}
				if (formatPropertyName != null) break; // there can/should be only one format property for a specific valuelist; we found it
			}
		}

		return new ValuelistPropertyDependencies(dataproviderPropertyName, foundsetPropertyName, formatPropertyName, dataproviderResolveValuelist,
			config.getConfigPropertyName());
	}

	@Override
	public TargetDataLinks getDataLinks(Object formElementValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement)
	{
		return TargetDataLinks.NOT_LINKED_TO_DATA; // we don't have the IValueList yet (that is a runtime thing, not a form element thing); so for now say "not linked to data"; at runtime when valuelist sablo value might add
		// itself as a listener to the DataAdapterList, any ComponentTypeSabloValue or FoundsetLinkedSabloValue using that should/will update their state
	}

	@Override
	public ValueListTypeSabloValue toSabloComponentValue(Object rhinoValue, ValueListTypeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext webObjectContext)
	{
		if (rhinoValue == null || RhinoConversion.isUndefinedOrNotFound(rhinoValue)) return null;

		// we expect that the toString of the scriptable rhino value is always the valuelist name
		Object value = rhinoValue instanceof DefaultScope ? rhinoValue.toString() : rhinoValue;

		if (previousComponentValue == null)
		{
			return value instanceof String ? createValuelistSabloValueByNameFromRhino((String)value, pd, webObjectContext) : null;
		}

		if (!previousComponentValue.isInitialized())
		{
			if (value instanceof String)
			{
				// weird; but we are going to create a new value anyway so it doesn't matter much
				return createValuelistSabloValueByNameFromRhino((String)value, pd, webObjectContext);
			}
			else
			{
				// we cannot set values from a dataset if the previous value is not ready for it
				Debug.error(
					"Trying to make changes (assignment) to an uninitialized valuelist property (this is not allowed): " + pd + " of " + webObjectContext,
					new RuntimeException());
				return previousComponentValue;
			}
		}

		ParsedFormat format = null;
		int type = -1;
		IValueList list = previousComponentValue.getValueList();

		if (list != null && list.getName() != null && list.getName().equals(value))
		{
			// no need to create a new value if we have the same valuelist name
			return previousComponentValue;
		}

		ValueListTypeSabloValue newValue;
		IValueList newVl = null;

		// see if it's a component.setValuelistItems (legacy) equivalent
		if (list != null && list instanceof CustomValueList && (value instanceof JSDataSet || value instanceof IDataSet))
		{
			// here we create a NEW, separate (runtime) custom valuelist instance for this component only (no longer the 'global' custom valuelist with that name that can be affected by application.setValuelistItems(...))
			INGApplication application = previousComponentValue.getDataAdapterList().getApplication();
			ValueList valuelist = application.getFlattenedSolution().getValueList(list.getName());
			if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
			{
				format = ((CustomValueList)list).getFormat();
				type = ((CustomValueList)list).getValueType();
				newVl = ValueListFactory.fillRealValueList(application, valuelist, IValueListConstants.CUSTOM_VALUES, format, type, value);

				if (newVl != null)
				{
					previousComponentValue.setNewCustomValuelistInstance(newVl, value);
					newValue = previousComponentValue;
				}
				else
				{
					// should never happen; ValueListFactory.fillRealValueList seems to always return non-null
					Debug.error("Assignment to Valuelist typed property '" + pd.getName() + "' of component '" + webObjectContext +
						"' failed for an unknown reason; dataset: " + value, new RuntimeException());
					newValue = previousComponentValue; // just keep old value
				}
			}
			else
			{
				Debug.error("Assignment to Valuelist typed property '" + pd.getName() + "' of component '" + webObjectContext +
					"' failed. Assigning a dataset is ONLY allowed for custom valuelists; dataset: " + value, new RuntimeException());
				newValue = previousComponentValue;
			}
		}
		else if (value instanceof String)
		{
			// the Rhino value is a different valuelist name; create a full new one
			newValue = createValuelistSabloValueByNameFromRhino((String)value, pd, webObjectContext);
		}
		else
		{
			Debug.error("Assignment to Valuelist typed property '" + pd.getName() + "' of component '" + webObjectContext +
				"' failed. Assigning this value is not supported: " + value, new RuntimeException());
			newValue = previousComponentValue; // whatever was set here is not supported; so keep the previous value
		}

		return newValue;

	}

	private ValueListTypeSabloValue createValuelistSabloValueByNameFromRhino(String valuelistId, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		if (valuelistId == null) return null;
		ValuelistPropertyDependencies propertyDependencies = getDependenciesToOtherProperties(pd, webObjectContext);

		return new ValueListTypeSabloValue(valuelistId, pd, propertyDependencies, false, false, NGComponentDALContext.getDataAdapterList(webObjectContext));
		// above both waitForDataproviderIfNull and waitForFormatIfNull are false because while in 'from Rhino' conversion, at this point, even if one would for example
		// set a full custom object that contains this property with dependencies to other properties in that custom object - we don't know the order in which the other properties are set
		// so we can check them; but that is not a problem because nothing is attached yet in this case (those flags are only used in attachToBaseObject) and when attach will be called, all
		// rhino-to-sablo conversions on that custom object are already done (so at attach time the values are already there)
	}

	@Override
	public boolean isValueAvailableInRhino(ValueListTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return webComponentValue != null;
	}

	@Override
	public Object toRhinoValue(ValueListTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext,
		Scriptable startScriptable)
	{
		if (webComponentValue == null) return null;
		return new DefaultScope(startScriptable)
		{
			private static final String NAME = "name";
			private static final String DATASET = "dataset";

			@Override
			public Object get(String name, Scriptable start)
			{
				if (webComponentValue != null && webComponentValue.getValueList() != null)
				{
					if (NAME.equals(name))
					{
						return webComponentValue.getValueList().getName();
					}
					if (DATASET.equals(name))
					{
						JSDataSet dataset = new JSDataSet();
						dataset.js_addColumn("display_values");
						dataset.js_addColumn("real_values");
						IValueList valueList = webComponentValue.getValueList();
						for (int i = 0; i < valueList.getSize(); i++)
						{
							dataset.js_addRow(new Object[] { valueList.getElementAt(i), valueList.getRealElementAt(i) });
						}
						return dataset;
					}
				}
				return super.get(name, start);
			}

			@Override
			public void put(String name, Scriptable start, Object value)
			{
				if (NAME.equals(name) || DATASET.equals(name))
				{
					toSabloComponentValue(value, webComponentValue, pd, webObjectContext);
					return;
				}
				super.put(name, start, value);
			}

			@Override
			// This is depended on that the toString() will return the valuelist name if that is set!
			// the ValueListPropertyType expect this but also in scripting.
			public String toString()
			{
				if (webComponentValue == null || webComponentValue.getValueList() == null) return null;
				return webComponentValue.getValueList().getName();
			}

			@Override
			public Object getDefaultValue(Class< ? > typeHint)
			{
				if (String.class.equals(typeHint) || typeHint == null)
				{
					return toString();
				}
				return super.getDefaultValue(typeHint);
			}
		};
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
		if (value != null)
		{
			list = application.getFlattenedSolution().getValueList(value.toString());
			if (list == null)
			{
				list = (ValueList)application.getFlattenedSolution().searchPersist(value.toString());
			}
		}
		if (list != null)
		{
			return application.getScriptEngine().getSolutionModifier().getValueList(list.getName());
		}
		return null;
	}

	@Override
	public ValueListTypeSabloValue resetI18nValue(ValueListTypeSabloValue propertyValue, PropertyDescription pd, WebFormComponent component)
	{
		// have to test if a real valuelist is there because a "autoVL" valuelist doesn't have an actual valuelist but is based on the column itself.
		if (propertyValue != null)
		{
			propertyValue.resetI18nValue();
		}
		return propertyValue;
	}

	/**
	 * Just a container for keeping names of properties that a valuelist property can depend on based on the for/forFoundset clauses in the .spec file.
	 *
	 * @author acostescu
	 */
	protected static class ValuelistPropertyDependencies
	{

		public final String dataproviderPropertyName;
		public final String foundsetPropertyName;
		public final String formatPropertyName;
		public final boolean dataproviderResolveValuelist;
		public final String configPropertyName;

		public ValuelistPropertyDependencies(String dataproviderPropertyName, String foundsetPropertyName, String formatPropertyName,
			boolean dataproviderResolveValuelist, String configPropertyName)
		{
			this.dataproviderPropertyName = dataproviderPropertyName;
			this.foundsetPropertyName = foundsetPropertyName;
			this.formatPropertyName = formatPropertyName;
			this.dataproviderResolveValuelist = dataproviderResolveValuelist;
			this.configPropertyName = configPropertyName;
		}
	}

	@Override
	public JSONWriter toDesignerDefaultJSONValue(JSONWriter writer, String key) throws JSONException
	{
		writer.key(key);
		writer.object();
		writer.key("hasRealValues");
		writer.value(true);
		writer.key("values");
		List<Map<String, Object>> array = new ArrayList<>(3);
		for (int i = 0; i < 3; i++)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("realValue", Integer.valueOf(i + 1));
			map.put("displayValue", "Item" + (i + 1));
			array.add(map);
		}
		JSONUtils.toBrowserJSONFullValue(writer, null, array, null, null);
		writer.endObject();
		return writer;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);

		w.value(TYPE_NAME);
		return true;
	}

}
