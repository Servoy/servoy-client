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
import org.sablo.WebComponent;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IChangeListener;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebGridFormUI;
import com.servoy.j2db.util.Debug;

/**
 * Value used at runtime as component type value proxy for multiple interested parties (browser, designtime, scripting).
 * 
 * @author acostescu
 */
public class ComponentTypeValue implements IComplexPropertyValue
{

	private final Object designJSONValue;
	private final ComponentTypeConfig config;

//	private IChangeListener changeMonitor;

	// these arrays represent the array of elements (it can be refactored into an array of a new custom class)
	private FormElement[] elements;
	private WebFormComponent[] childComponents;
	private List<String>[] apisOnAll; // here are the api's that should be called on all records, not only selected one when called on a foundset linked component
	private Map<String, String>[] dataLinks;

	private boolean componentsAreCreated = false;

	private WebFormComponent component;

	// this class currently always works with arrays of Component values (see how it is instantiated)
	public ComponentTypeValue(Object designJSONValue, ComponentTypeConfig config)
	{
		this.config = config;
		this.designJSONValue = designJSONValue;
		// TODO ac Auto-generated constructor stub
	}

	public String forFoundsetTypedPropertyName()
	{
		return config != null ? config.forFoundsetTypedProperty : null;
	}

	@Override
	public void init(IChangeListener monitor, WebComponent c, String propertyName)
	{
//		this.changeMonitor = monitor;
		this.component = (WebFormComponent)c;

		// if this elements propety uses a forFoundsetTypedProperty, we don't know at this point if that property is initialized or not
		// so search for it later, when we really need to create components; fow not just parse what we can
		try
		{
			if (designJSONValue instanceof JSONArray)
			{
				JSONArray arrayOfElementSpecs = (JSONArray)designJSONValue;

				elements = new FormElement[arrayOfElementSpecs.length()];
				apisOnAll = new ArrayList[arrayOfElementSpecs.length()];
				dataLinks = new HashMap[arrayOfElementSpecs.length()];
				childComponents = new WebFormComponent[arrayOfElementSpecs.length()];

				for (int i = 0; i < elements.length; i++)
				{
					JSONObject elementSpec = (JSONObject)arrayOfElementSpecs.get(i);
					elements[i] = new FormElement((String)elementSpec.get(ComponentTypeImpl.TYPE_NAME_KEY),
						(JSONObject)elementSpec.get(ComponentTypeImpl.DEFINITION_KEY), component.getFormElement().getForm(), component.getName() +
							propertyName + "_" + i, component.getParent().getDataConverterContext()); //$NON-NLS-1$
					JSONArray callTypes = elementSpec.optJSONArray(ComponentTypeImpl.API_CALL_TYPES_KEY);
					if (callTypes == null) apisOnAll[i] = findCallTypesInApiSpecDefinition(elements[i].getWebComponentSpec().getApis());
					else
					{
						apisOnAll[i] = new ArrayList<String>();
						for (int j = 0; j < callTypes.length(); j++)
						{
							JSONObject o = callTypes.getJSONObject(j);
							if (o.getInt(ComponentTypeImpl.CALL_ON_KEY) == ComponentTypeImpl.CALL_ON_ALL_RECORDS) apisOnAll[i].add(o.getString(ComponentTypeImpl.FUNCTION_NAME_KEY));
						}
					}
					dataLinks[i] = findDataLinks(elements[i]);
				}
			}
			else elements = new FormElement[0];
		}
		catch (JSONException e)
		{
			elements = new FormElement[0];
			Debug.error(e);
		}
	}

	private void createComponentsIfNeeded()
	{
		// this method should get called only after init() got called on all properties from this component (including this one)
		// so now we should be able to find a potentially linked foundset property value
		if (componentsAreCreated) return;
		componentsAreCreated = true;

		FoundsetTypeValue foundsetPropValue = null;
		String foundsetPropName = forFoundsetTypedPropertyName();
		if (foundsetPropName != null)
		{
			foundsetPropValue = (FoundsetTypeValue)component.getProperty(foundsetPropName);
			if (foundsetPropValue == null) Debug.error("Cannot find linked foundset property '" + foundsetPropName + "' for an elements property."); //$NON-NLS-1$//$NON-NLS-2$
		}

		IDataAdapterList dal = (foundsetPropValue != null ? foundsetPropValue.getDataAdapterList() : component.getParent().getDataAdapterList());

		for (int i = 0; i < elements.length; i++)
		{
			childComponents[i] = ComponentFactory.createComponent(dal.getApplication(), dal, elements[i], component.getParent());
			component.getParent().contributeComponentToElementsScope(elements[i], elements[i].getWebComponentSpec(), childComponents[i]);
		}
		elements = null; // we now have components instantiated; don't need this one any more
	}

	private Map<String, String> findDataLinks(FormElement formElement)
	{
		Map<String, String> m = new HashMap<>();
		List<String> tagstrings = WebGridFormUI.getWebComponentPropertyType(formElement.getWebComponentSpec(), IPropertyType.Default.tagstring.getType());
		for (String tagstringPropID : tagstrings)
		{
			m.put(tagstringPropID, (String)formElement.getProperty(tagstringPropID));
		}

		List<String> dataproviders = WebGridFormUI.getWebComponentPropertyType(formElement.getWebComponentSpec(), IPropertyType.Default.dataprovider.getType());
		for (String dataproviderID : dataproviders)
		{
			m.put(dataproviderID, (String)formElement.getProperty(dataproviderID));
		}
		return m.size() > 0 ? m : null;
	}

	private List<String> findCallTypesInApiSpecDefinition(Map<String, WebComponentApiDefinition> apis)
	{
		List<String> arr = null;
		if (apis != null)
		{
			arr = new ArrayList<String>();
			for (Entry<String, WebComponentApiDefinition> apiMethod : apis.entrySet())
			{
				JSONObject apiConfigOptions = apiMethod.getValue().getCustomConfigOptions();
				if (apiConfigOptions != null &&
					apiConfigOptions.optInt(ComponentTypeImpl.CALL_ON_KEY, ComponentTypeImpl.CALL_ON_SELECTED_RECORD) == ComponentTypeImpl.CALL_ON_ALL_RECORDS)
				{
					arr.add(apiMethod.getKey());
				}
			}
			if (arr.size() == 0) arr = null;
		}
		return arr;
	}

	// TODO ac somewhere - link it with the foundset property...

	@Override
	@SuppressWarnings("nls")
	public JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// create children of component as specified by this property
		destinationJSON.array();
		if (childComponents != null)
		{
			createComponentsIfNeeded(); // currently we only support design-time elements (can be enhanced if needed)

			for (int i = 0; i < childComponents.length; i++)
			{
				FormElement fe = childComponents[i].getFormElement();
				destinationJSON.object();

				destinationJSON.key("componentDirectiveName").value(fe.getTypeName());
				destinationJSON.key("name").value(fe.getName());
				destinationJSON.key("model").value(fe.propertiesAsJSON(destinationJSON)); // full to json always uses design values
				destinationJSON.key("handlers").array();
				for (String handleMethodName : fe.getHandlers())
				{
					destinationJSON.value(handleMethodName);
				}
				destinationJSON.endArray();

				destinationJSON.key("forFoundset").object();
				if (dataLinks != null)
				{
					destinationJSON.key("dataLinks").array();
					for (Entry<String, String> dl : dataLinks[i].entrySet())
					{
						destinationJSON.object().key("propertyName").value(dl.getKey());
						destinationJSON.key("dataprovider").value(dl.getValue()).endObject();
					}
					destinationJSON.endArray();
				}
				if (apisOnAll[i] != null)
				{
					destinationJSON.key("apiCallTypes").array();
					for (String methodName : apisOnAll[i])
					{
						destinationJSON.object().key(methodName).value(ComponentTypeImpl.CALL_ON_ALL_RECORDS).endObject();
					}
					destinationJSON.endArray();
				}
				destinationJSON.endObject();
			}
		}
		destinationJSON.endArray();

		return destinationJSON;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO if the components property type is not linked to a foundset then somehow the dataproviders/tagstring must also be sent when needed
		// but if it is linked to a foundset those should only be sent through the foundset!
		// TODO ac send component properties that changed
		return toJSON(destinationJSON, conversionMarkers);
	}

	@Override
	public JSONWriter toDesignJSON(JSONWriter writer) throws JSONException
	{
		return writer.value(designJSONValue);
	}

	@Override
	public Object toServerObj()
	{
		// TODO implement more here if we want this type of properties accessible in scripting
		return null;
	}

	public void browserUpdatesReceived(Object jsonValue)
	{
		// TODO ac when some properties change reflect it for scripting?
	}

}
