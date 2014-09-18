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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebGridFormUI;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
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
public class ComponentPropertyType extends CustomJSONPropertyType<ComponentTypeSabloValue> implements
	IDesignToFormElement<JSONObject, ComponentTypeFormElementValue, ComponentTypeSabloValue>,
	IFormElementToTemplateJSON<ComponentTypeFormElementValue, ComponentTypeSabloValue>,
	IFormElementToSabloComponent<ComponentTypeFormElementValue, ComponentTypeSabloValue>, IConvertedPropertyType<ComponentTypeSabloValue>,
	ISabloComponentToRhino<ComponentTypeSabloValue>
{

	public static final ComponentPropertyType INSTANCE = new ComponentPropertyType(null);

	public static final String TYPE_NAME = "component";

	// START keys and values used in JSON
	public final static String TYPE_NAME_KEY = "typeName";
	public final static String DEFINITION_KEY = "definition";
	public final static String API_CALL_TYPES_KEY = "apiCallTypes";
	public final static String FUNCTION_NAME_KEY = "functionName";

	public final static String CALL_ON_KEY = "callOn";
	public final static int CALL_ON_SELECTED_RECORD = 0;
	public final static int CALL_ON_ALL_RECORDS = 1;

	protected static final String PROPERTY_UPDATES = "propertyUpdates";
	// END keys and values used in JSON

	protected int uniqueId = 1;

	public ComponentPropertyType(PropertyDescription definition)
	{
		super(TYPE_NAME, definition);
	}

	@Override
	public ComponentTypeFormElementValue toFormElementValue(JSONObject designValue, PropertyDescription pd, FlattenedSolution flattenedSolution,
		FormElement fe, PropertyPath propertyPath)
	{
		try
		{
			FormElement element = new FormElement((String)designValue.get(TYPE_NAME_KEY), (JSONObject)designValue.get(DEFINITION_KEY), fe.getForm(),
				fe.getName() + (uniqueId++), fe.getDataConverterContext(), propertyPath);

			return getFormElementValue(designValue.optJSONArray(API_CALL_TYPES_KEY), pd, propertyPath, element);
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return null;
		}
	}

	public ComponentTypeFormElementValue getFormElementValue(JSONArray callTypes, PropertyDescription pd, PropertyPath propertyPath, FormElement element)
		throws JSONException
	{
		List<String> apisOnAll = null;
		Map<String, String> dataLinks = null;
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
			dataLinks = findDataLinks(element);
		} // else dataLinks and apisOnAll are not relevant
		return new ComponentTypeFormElementValue(element, apisOnAll, dataLinks, propertyPath.currentPathCopy());
	}

	public String forFoundsetTypedPropertyName(PropertyDescription pd)
	{
		return pd.getConfig() instanceof ComponentTypeConfig ? ((ComponentTypeConfig)pd.getConfig()).forFoundsetTypedProperty : null;
	}

	protected Map<String, String> findDataLinks(FormElement formElement)
	{
		Map<String, String> m = new HashMap<>();

		// I guess tagstrings, valuelists, tab seq, ... must be implemented separately and provided as a viewport containing these values as part of 'components'
		// property, not as part of foundset property
//		List<String> tagstrings = WebGridFormUI.getWebComponentPropertyType(formElement.getWebComponentSpec(), TagStringPropertyType.INSTANCE);
//		for (String tagstringPropID : tagstrings)
//		{
//			m.put(tagstringPropID, (String)formElement.getProperty(tagstringPropID));
//		}

		List<String> dataproviders = WebGridFormUI.getWebComponentPropertyType(formElement.getWebComponentSpec(), DataproviderPropertyType.INSTANCE);
		for (String dataproviderID : dataproviders)
		{
			String dataproviderIDValue = (String)formElement.getPropertyValue(dataproviderID);
			if (dataproviderIDValue != null) m.put(dataproviderID, dataproviderIDValue); // TODO if dataprovider type changes to store something else in form element this has to be updated as well
		}
		return m;
	}

	protected List<String> findCallTypesInApiSpecDefinition(Map<String, WebComponentApiDefinition> apis)
	{
		List<String> arr = null;
		if (apis != null)
		{
			arr = new ArrayList<String>();
			for (Entry<String, WebComponentApiDefinition> apiMethod : apis.entrySet())
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
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, ComponentTypeFormElementValue formElementValue, PropertyDescription pd,
		DataConversion conversionMarkers) throws JSONException
	{
		if (conversionMarkers != null) conversionMarkers.convert(ComponentPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		// create children of component as specified by this property
		FormElement fe = formElementValue.element;
		JSONUtils.addKeyIfPresent(writer, key);
		writer.object();

		writer.key("componentDirectiveName").value(fe.getTypeName());
		writer.key("name").value(fe.getName());
		writer.key("model");
		fe.propertiesAsTemplateJSON(writer); // full to json always uses design values
		writer.key("handlers").object();
		for (String handleMethodName : fe.getHandlers())
		{
			writer.key(handleMethodName);
			JSONObject handlerInfo = new JSONObject();
			handlerInfo.put("formName", fe.getForm().getName());
			handlerInfo.put("beanName", fe.getName());
			writer.value(handlerInfo);
		}
		writer.endObject();

		if (forFoundsetTypedPropertyName(pd) != null)
		{
			writer.key("forFoundset").object();
			if (formElementValue.dataLinks != null)
			{
				writer.key("dataLinks").array();
				for (Entry<String, String> dl : formElementValue.dataLinks.entrySet())
				{
					writer.object().key("propertyName").value(dl.getKey());
					writer.key("dataprovider").value(dl.getValue()).endObject();
				}
				writer.endArray();
			}
			if (formElementValue.apisOnAll != null)
			{
				writer.key("apiCallTypes").array();
				for (String methodName : formElementValue.apisOnAll)
				{
					writer.object().key(methodName).value(CALL_ON_ALL_RECORDS).endObject();
				}
				writer.endArray();
			}
			writer.endObject();
		}

		writer.endObject();

		return writer;
	}

	@Override
	public ComponentTypeSabloValue toSabloComponentValue(ComponentTypeFormElementValue formElementValue, PropertyDescription pd, FormElement formElement,
		WebFormComponent component)
	{
		return new ComponentTypeSabloValue(formElementValue, pd, forFoundsetTypedPropertyName(pd));
	}

	@Override
	public ComponentTypeSabloValue fromJSON(Object newJSONValue, ComponentTypeSabloValue previousSabloValue, IDataConverterContext dataConverterContext)
	{
		if (previousSabloValue != null)
		{
			previousSabloValue.browserUpdatesReceived(newJSONValue);
		}
		// else there's nothing to do here / this type can't receive browser updates when server has no value for it

		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ComponentTypeSabloValue sabloValue, DataConversion clientConversion) throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			sabloValue.changesToJSON(writer, clientConversion, this);
		}
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(ComponentTypeSabloValue webComponentValue, PropertyDescription pd, WebFormComponent component)
	{
		return false;
	}

	@Override
	public Object toRhinoValue(ComponentTypeSabloValue webComponentValue, PropertyDescription pd, WebFormComponent component)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		String tmp = config.optString("forFoundsetTypedProperty");
		return tmp == null ? null : new ComponentTypeConfig(tmp);
	}

}
