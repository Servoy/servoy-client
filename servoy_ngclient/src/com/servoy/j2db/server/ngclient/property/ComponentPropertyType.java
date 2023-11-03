/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectApiFunctionDefinition;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.ISupportsGranularUpdates;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ChildWebComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.AngularFormGenerator;
import com.servoy.j2db.server.ngclient.ChildrenJSONGenerator;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.II18NPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ITemplateValueUpdaterType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.FormElementToJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.util.Debug;

/**
 * Implementation for the complex custom type "component".
 *
 * @author acostescu
 */
public class ComponentPropertyType extends DefaultPropertyType<ComponentTypeSabloValue>
	implements IDesignToFormElement<JSONObject, ComponentTypeFormElementValue, ComponentTypeSabloValue>,
	IFormElementToTemplateJSON<ComponentTypeFormElementValue, ComponentTypeSabloValue>,
	IFormElementToSabloComponent<ComponentTypeFormElementValue, ComponentTypeSabloValue>, IConvertedPropertyType<ComponentTypeSabloValue>,
	ISabloComponentToRhino<ComponentTypeSabloValue>, ISupportsGranularUpdates<ComponentTypeSabloValue>, ITemplateValueUpdaterType<ComponentTypeSabloValue>,
	II18NPropertyType<ComponentTypeSabloValue>, IPropertyWithClientSideConversions<ComponentTypeSabloValue>
{


	public static final ComponentPropertyType INSTANCE = new ComponentPropertyType();

	public static final String TYPE_NAME = ChildWebComponent.COMPONENT_PROPERTY_TYPE_NAME;

	// START keys and values used in JSON
	public final static String API_CALL_TYPES_KEY = "apiCallTypes";
	public final static String FUNCTION_NAME_KEY = "functionName";

	public final static String CALL_ON_KEY = "callOn";
	public final static int CALL_ON_SELECTED_RECORD = 0;
	public final static int CALL_ON_ALL_RECORDS = 1;

	protected static final String PROPERTY_UPDATES_KEY = "propertyUpdates";
	protected static final String MODEL_KEY = "model";
	protected static final String MODEL_VIEWPORT_KEY = "model_vp";
	protected static final String MODEL_VIEWPORT_CHANGES_KEY = "model_vp_ch";
	protected static final String FOUNDSET_CONFIG_PROPERTY_NAME = "foundsetConfig";
	protected static final String RECORD_BASED_PROPERTIES = "recordBasedProperties";

	public static final String PROPERTY_NAME_KEY = "pn";
	public static final String VALUE_KEY = "v";

	/**
	 * Used for an update that comes from the browser.
	 *
	 * Key for a rowId that identifies the row of a property inside the child component for the foundset linked DP of the
	 * component (that is linked to another property of type foundset inside the child component itself).<br/></br>
	 *
	 * So for example you have a list form component that has in each cell an editable table component (which could work for example on a related foundset).
	 * Then this key identifies the row in the related foundset for the column in the editable table that was changed on client.
	 */
	protected static final String ROW_ID_OF_PROP_INSIDE_COMPONENT = "_svyRowIdOfProp";

	private static final String COMPONENT_SPEC_NAME = "componentDirectiveName";
	// END keys and values used in JSON

	protected long uniqueId = 1;

	public ComponentPropertyType()
	{
		super();
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public ComponentTypeFormElementValue toFormElementValue(JSONObject designValue, PropertyDescription pd, FlattenedSolution flattenedSolution,
		INGFormElement fe, PropertyPath propertyPath)
	{
		try
		{
			FormElement element = new FormElement((String)designValue.get(ChildWebComponent.TYPE_NAME_KEY),
				(JSONObject)designValue.get(ChildWebComponent.DEFINITION_KEY), fe.getForm(), fe.getName() + (uniqueId++), flattenedSolution, propertyPath,
				fe.getDesignId() != null);

			return getFormElementValue(designValue.optJSONArray(API_CALL_TYPES_KEY), pd, propertyPath, element, flattenedSolution);
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return null;
		}
	}

	public ComponentTypeFormElementValue getFormElementValue(JSONArray callTypes, PropertyDescription pd, PropertyPath propertyPath, FormElement element,
		FlattenedSolution flattenedSolution) throws JSONException
	{
		List<String> apisOnAll = null;
		RecordBasedProperties recordBasedProperties = null;
		if (forFoundsetTypedPropertyName(pd) != null)
		{
			if (callTypes == null) apisOnAll = findCallTypesInApiSpecDefinition(element.getWebComponentSpec().getApiFunctions());
			else
			{
				apisOnAll = new ArrayList<String>();
				for (int j = 0; j < callTypes.length(); j++)
				{
					JSONObject o = callTypes.getJSONObject(j);
					if (o.getInt(CALL_ON_KEY) == CALL_ON_ALL_RECORDS) apisOnAll.add(o.getString(FUNCTION_NAME_KEY));
				}
			}
			recordBasedProperties = findRecordAwareRootProperties(element, flattenedSolution);
		} // else viewPortData and apisOnAll are not relevant
		return new ComponentTypeFormElementValue(element, apisOnAll, recordBasedProperties, propertyPath.currentPathCopy());
	}

	public String forFoundsetTypedPropertyName(PropertyDescription pd)
	{
		return pd.getConfig() instanceof ComponentTypeConfig ? ((ComponentTypeConfig)pd.getConfig()).forFoundset : null;
	}

	protected RecordBasedProperties findRecordAwareRootProperties(FormElement formElement, FlattenedSolution flattenedSolution)
	{
		RecordBasedProperties m = new RecordBasedProperties();

		// tagstrings, valuelists, tab seq, ... must be implemented separately and provided as a
		// viewport containing these values as part of 'components' property
		Set<Entry<String, PropertyDescription>> propertyDescriptors = formElement.getWebComponentSpec().getProperties().entrySet();
		for (Entry<String, PropertyDescription> propertyDescriptorEntry : propertyDescriptors)
		{
			PropertyDescription pd = propertyDescriptorEntry.getValue();
			IPropertyType< ? > type = pd.getType();
			if (type instanceof IDataLinkedType)
			{
				// as these are root-level component properties, their TargetDataLinks will always be cached (only array element values are not cached)
				TargetDataLinks dataLinks = ((IDataLinkedType)type).getDataLinks(formElement.getPropertyValue(pd.getName()), propertyDescriptorEntry.getValue(),
					flattenedSolution, formElement);
				if (dataLinks != TargetDataLinks.NOT_LINKED_TO_DATA && dataLinks.recordLinked)
				{
					m.addRecordBasedProperty(propertyDescriptorEntry.getKey(), dataLinks.dataProviderIDs, null);
				}
			}
		}

		return m;
	}

	protected List<String> findCallTypesInApiSpecDefinition(Map<String, WebObjectApiFunctionDefinition> apis)
	{
		List<String> arr = null;
		if (apis != null)
		{
			arr = new ArrayList<String>();
			for (Entry<String, WebObjectApiFunctionDefinition> apiMethod : apis.entrySet())
			{
				JSONObject apiConfigOptions = apiMethod.getValue().getCustomConfigOptions();
				if (apiConfigOptions != null && apiConfigOptions.optInt(CALL_ON_KEY, CALL_ON_SELECTED_RECORD) == CALL_ON_ALL_RECORDS)
				{
					arr.add(apiMethod.getKey());
				}
			}
			if (arr.size() == 0) arr = null;
		}
		return arr;
	}

	@Override
	public JSONWriter toTemplateJSONValue(final JSONWriter writer, String key, ComponentTypeFormElementValue formElementValue, PropertyDescription pd,
		final FormElementContext formElementContext) throws JSONException
	{
		if (formElementValue == null) return writer;

		FlattenedSolution clientFlattenedSolution = (formElementContext != null && formElementContext.getContext() != null)
			? formElementContext.getContext().getSolution() : null;
		if (!formElementValue.isSecurityViewable(clientFlattenedSolution))
		{
			return writer;
		}

		// create children of component as specified by this property
		final FormElementContext feContext = new FormElementContext(formElementValue.element, formElementContext.getContext(), null);
		JSONUtils.addKeyIfPresent(writer, key);

		writer.object();

		writeTemplateJSONContent(writer, formElementValue, forFoundsetTypedPropertyName(pd), feContext, new IModelWriter()
		{

			@Override
			public void writeComponentModel() throws JSONException
			{
				// TODO here we could remove record based props from fe.propertiesForTemplateJSON(); but normally record based props will not write any value in template anyway
				TypedData<Map<String, Object>> modelProperties = feContext.getFormElement().propertiesForTemplateJSON();
				writer.object();
				JSONUtils.writeData(FormElementToJSON.INSTANCE, writer, modelProperties.content, modelProperties.contentType, feContext);
				writer.endObject();
			}

		}, formElementValue.recordBasedProperties, true);

		writer.endObject();

		return writer;
	}

	protected <ContextT> void writeTemplateJSONContent(JSONWriter writer, ComponentTypeFormElementValue formElementValue, String forFoundsetPropertyType,
		FormElementContext componentFormElementContext, IModelWriter modelWriter, RecordBasedProperties recordBasedProperties,
		boolean writeViewportIfFoundsetBased)
		throws JSONException
	{
		FormElement fe = componentFormElementContext.getFormElement();
		if (forFoundsetPropertyType != null) writer.key(FoundsetLinkedPropertyType.FOR_FOUNDSET_PROPERTY_NAME).value(forFoundsetPropertyType);

		writer.key(COMPONENT_SPEC_NAME); // this is currently the spec.getName() which can also be used client-side to identify the client-side-types for this component
		IPersist persist = formElementValue.element.getPersistIfAvailable();
		if (persist instanceof TabPanel)
		{
			// special case for default/legacy tab panels (same persist can mean 1 of 2 spec files and 1 of 4 client side element types)
			ChildrenJSONGenerator.handleTabpanelSpecNameAndElementName(writer, persist);
		}
		else writer.value(fe.getTypeName());

		writer.key("name").value(componentFormElementContext.getName());

		if (fe.getPropertyValue("componentIndex") != null)
		{
			writer.key("componentIndex").value(fe.getPropertyValue("componentIndex"));
		}
		if (fe.getPropertyValue("headerIndex") != null)
		{
			writer.key("headerIndex").value(fe.getPropertyValue("headerIndex"));
		}
		AngularFormGenerator.writePosition(writer, persist,
			componentFormElementContext.getContext().getForm().getForm(), null, formElementValue.element.isInDesigner());

		writer.key("model");
		try
		{
			modelWriter.writeComponentModel();
		}
		catch (JSONException | IllegalArgumentException e)
		{
			Debug.error("Problem detected when handling a component's (" + fe.getTagname() + ") properties / events.", e);
			throw e;
		}

		writer.key("handlers").object();
		for (String handleMethodName : fe.getHandlers())
		{
			writer.key(handleMethodName);
			JSONObject handlerInfo = new JSONObject();
			handlerInfo.put("formName", fe.getForm().getName());
			handlerInfo.put("beanName", componentFormElementContext.getName());
			writer.value(handlerInfo);
		}
		writer.endObject();

		if (forFoundsetPropertyType != null)
		{
			if (writeViewportIfFoundsetBased) writer.key(MODEL_VIEWPORT_KEY).array().endArray(); // this will contain record based properties for the foundset's viewPort
			writer.key(FOUNDSET_CONFIG_PROPERTY_NAME).object();
			if (recordBasedProperties != null)
			{
				writer.key(RECORD_BASED_PROPERTIES).array();
				recordBasedProperties.forEach(propertyName -> writer.value(propertyName));
				writer.endArray();
			}
			if (formElementValue.apisOnAll != null)
			{
				writer.key(API_CALL_TYPES_KEY).array();
				for (String methodName : formElementValue.apisOnAll)
				{
					writer.object().key(methodName).value(CALL_ON_ALL_RECORDS).endObject();
				}
				writer.endArray();
			}
			writer.endObject();
		}
	}

	@Override
	public ComponentTypeSabloValue toSabloComponentValue(ComponentTypeFormElementValue formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dal)
	{
		if (!formElementValue.isSecurityViewable(dal.getApplication().getFlattenedSolution()))
		{
			return null;
		}
		return new ComponentTypeSabloValue(formElementValue, pd, forFoundsetTypedPropertyName(pd));
	}

	@Override
	public ComponentTypeSabloValue fromJSON(Object newJSONValue, ComponentTypeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdatesReceived(newJSONValue);
		}
		// else there's nothing to do here / this type can't receive browser updates when server has no value for it

		return previousSabloValue;
	}

	@Override
	public JSONWriter initialToJSON(JSONWriter writer, String key, ComponentTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		// this sends a diff update between the value it has in the template and the initial data requested after runtime components were created or during a page refresh.
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.initialToJSON(writer, this);
		}
		return writer;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter writer, String key, ComponentTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.changesToJSON(writer, this);
		}
		return writer;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ComponentTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.fullToJSON(writer, this);
		}
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(ComponentTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return webComponentValue != null && webComponentValue.getChildComponent() != null;
	}

	@Override
	public Object toRhinoValue(ComponentTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext componentOrService,
		Scriptable startScriptable)
	{
		if (webComponentValue != null)
		{
			return webComponentValue.getRuntimeComponent();
		}
		return null;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		if (config == null) return null;

		String tmp = config.optString("forFoundset");
		return tmp == null || tmp.length() == 0 ? null : new ComponentTypeConfig(tmp);
	}


	protected interface IModelWriter
	{
		void writeComponentModel() throws JSONException;
	}


	@Override
	public ComponentTypeSabloValue resetI18nValue(ComponentTypeSabloValue currentValue, PropertyDescription description, WebFormComponent component)
	{
		if (currentValue != null) currentValue.resetI18nValue();
		return currentValue;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(TYPE_NAME);
		return true;
	}

}
