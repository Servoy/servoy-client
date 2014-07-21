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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebComponentInitializer;
import org.sablo.WebComponent;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.specification.property.IComplexTypeImpl;
import org.sablo.websocket.ConversionLocation;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.ComponentContext;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebGridFormUI;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.util.Debug;

/**
 * Value used at runtime as component type value proxy for multiple interested parties (browser, designtime, scripting).
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ComponentTypeValue implements IComplexPropertyValue
{

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

	protected final Object designJSONValue;
	protected final ComponentTypeConfig config;

	// these arrays represent the array of elements (it can be refactored into an array of a new custom class)
	protected FormElement[] elements;
	protected WebFormComponent[] childComponents;
	protected List<String>[] apisOnAll; // here are the api's that should be called on all records, not only selected one when called on a foundset linked component
	protected Map<String, String>[] dataLinks;

	protected boolean componentsAreCreated = false;

	protected WebFormComponent component;
	protected PropertyChangeListener forFoundsetListener;
	private IChangeListener monitor;
	private String propertyName;

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

	public void initialize(IWebComponentInitializer formElement, String propertyName, Object defaultValue)
	{
		FormElement fe = (FormElement)formElement;
		this.propertyName = propertyName;
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
					elements[i] = new FormElement((String)elementSpec.get(TYPE_NAME_KEY), (JSONObject)elementSpec.get(DEFINITION_KEY), fe.getForm(),
						fe.getName() + propertyName + "_" + i, fe.getDataConverterContext()); //$NON-NLS-1$

					if (forFoundsetTypedPropertyName() != null)
					{
						JSONArray callTypes = elementSpec.optJSONArray(API_CALL_TYPES_KEY);
						if (callTypes == null) apisOnAll[i] = findCallTypesInApiSpecDefinition(elements[i].getWebComponentSpec().getApiFunctions());
						else
						{
							apisOnAll[i] = new ArrayList<String>();
							for (int j = 0; j < callTypes.length(); j++)
							{
								JSONObject o = callTypes.getJSONObject(j);
								if (o.getInt(CALL_ON_KEY) == CALL_ON_ALL_RECORDS) apisOnAll[i].add(o.getString(FUNCTION_NAME_KEY));
							}
						}
						dataLinks[i] = findDataLinks(elements[i]);
					} // else dataLinks and apisOnAll are not relevant
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

	@Override
	public void attachToComponent(IChangeListener monitor, WebComponent c)
	{
		componentsAreCreated = false;
		this.component = (WebFormComponent)c;
		this.monitor = monitor;

		if (elements != null)
		{
			for (int i = 0; i < elements.length; i++)
			{
				if (childComponents[i] != null)
				{
					childComponents[i].dispose();
				}
			}
		}
		createComponentsIfNeededAndPossible();
		if (forFoundsetTypedPropertyName() != null)
		{
			component.addPropertyChangeListener(forFoundsetTypedPropertyName(), forFoundsetListener = new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					if (evt.getNewValue() != null) createComponentsIfNeededAndPossible();
				}
			});
		}
	}

	@Override
	public void detach()
	{
		if (forFoundsetListener != null) component.removePropertyChangeListener(forFoundsetTypedPropertyName(), forFoundsetListener);
	}

	private FoundsetTypeValue getFoundsetValue()
	{
		if (component != null)
		{
			String foundsetPropName = forFoundsetTypedPropertyName();
			if (foundsetPropName != null)
			{
				return (FoundsetTypeValue)component.getProperty(foundsetPropName);
			}
		}
		return null;
	}

	protected void createComponentsIfNeededAndPossible()
	{
		// this method should get called only after init() got called on all properties from this component (including this one)
		// so now we should be able to find a potentially linked foundset property value
		if (componentsAreCreated || component == null || elements == null) return;

		FoundsetTypeValue foundsetPropValue = getFoundsetValue();
		if (foundsetPropValue == null) return; // Cannot find linked foundset property; it is possible that that property was not yet attached to the component; we can wait for that to happen before creating components; see foundsetPropertyReady()

		componentsAreCreated = true;

		IDataAdapterList dal = (foundsetPropValue != null ? foundsetPropValue.getDataAdapterList() : ((IWebFormUI)component.getParent()).getDataAdapterList());

		for (int i = 0; i < elements.length; i++)
		{
			childComponents[i] = ComponentFactory.createComponent(dal.getApplication(), dal, elements[i], (IWebFormUI)component.getParent());
			childComponents[i].addPropertyChangeListener(null, new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					monitor.valueChanged();
				}
			});
			childComponents[i].setParent(component.getParent());
			childComponents[i].setComponentContext(new ComponentContext(component.getName(), propertyName, i));
			((IWebFormUI)component.getParent()).contributeComponentToElementsScope(elements[i], elements[i].getWebComponentSpec(), childComponents[i]);
			for (String handler : childComponents[i].getFormElement().getHandlers())
			{
				Object value = childComponents[i].getFormElement().getProperty(handler);
				if (value != null)
				{
					childComponents[i].add(handler, (Integer)value);
				}
			}
		}

		registerDataProvidersWithFoundset(foundsetPropValue);
	}

	/**
	 * Let linked foundset property know which dataprovider/tagstrings it should send client-side.
	 */
	protected void registerDataProvidersWithFoundset(FoundsetTypeValue foundsetPropValue)
	{
		if (foundsetPropValue != null)
		{
			HashSet<String> allDataProviders = new HashSet<String>();
			for (Map<String, String> dl : dataLinks)
			{
				allDataProviders.addAll(dl.values());
			}
			foundsetPropValue.includeDataProviders(allDataProviders);
		}
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
			m.put(dataproviderID, (String)formElement.getProperty(dataproviderID));
		}
		return m.size() > 0 ? m : null;
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

	// TODO ac somewhere - link it with the foundset property...

	@Override
	@SuppressWarnings("nls")
	public JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		createComponentsIfNeededAndPossible(); // currently we only support design-time elements (can be enhanced if needed)

		// TODO conversion markers should never be null I think, but it did happen (due to JSONUtils.toJSONValue(JSONWriter writer, Object value, IForJsonConverter forJsonConverter, ConversionLocation toDestinationType); will create a case for that
		if (conversionMarkers != null) conversionMarkers.convert(ComponentTypeImpl.TYPE_ID + IComplexTypeImpl.ARRAY); // so that the client knows it must use the custom client side JS for what JSON it gets

		// create children of component as specified by this property
		destinationJSON.array();
		if (elements != null)
		{

			for (int i = 0; i < elements.length; i++)
			{
				FormElement fe = elements[i];
				destinationJSON.object();

				destinationJSON.key("componentDirectiveName").value(fe.getTypeName());
				destinationJSON.key("name").value(fe.getName());
				destinationJSON.key("model");
				fe.propertiesAsJSON(destinationJSON); // full to json always uses design values
				destinationJSON.key("handlers").object();
				for (String handleMethodName : fe.getHandlers())
				{
					destinationJSON.key(handleMethodName);
					JSONObject handlerInfo = new JSONObject();
					handlerInfo.put("formName", fe.getForm().getName());
					handlerInfo.put("beanName", fe.getName());
					destinationJSON.value(handlerInfo);
				}
				destinationJSON.endObject();

				if (forFoundsetTypedPropertyName() != null)
				{
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
							destinationJSON.object().key(methodName).value(CALL_ON_ALL_RECORDS).endObject();
						}
						destinationJSON.endArray();
					}
					destinationJSON.endObject();
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
		if (conversionMarkers != null) conversionMarkers.convert(ComponentTypeImpl.TYPE_ID + IComplexTypeImpl.ARRAY); // so that the client knows it must use the custom client side JS for what JSON it gets

		// TODO if the components property type is not linked to a foundset then somehow the dataproviders/tagstring must also be sent when needed
		// but if it is linked to a foundset those should only be sent through the foundset!
		destinationJSON.object();
		if (childComponents != null)
		{
			Map<String, Map<String, Object>> componentChanges = new HashMap<String, Map<String, Object>>();
			for (int i = childComponents.length - 1; i >= 0; i--)
			{
				WebFormComponent childComponent = childComponents[i];
				if (childComponent != null)
				{
					Map<String, Object> changes = childComponent.getChanges();
					if (changes.size() > 0)
					{
						componentChanges.put(String.valueOf(i), changes);
					}
				}
			}
			if (componentChanges.size() > 0)
			{
				destinationJSON.key(PROPERTY_UPDATES);
				destinationJSON.object();
				JSONUtils.writeDataWithConversions(destinationJSON, componentChanges,
					((IWebFormUI)component.getParent()).getDataConverterContext().getApplication().getWebsocketSession().getForJsonConverter(),
					ConversionLocation.BROWSER_UPDATE);
				destinationJSON.endObject();
			}
		}
		destinationJSON.endObject();
		return destinationJSON;
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
		if (childComponents == null) return;

		if (jsonValue instanceof JSONArray)
		{
			try
			{
				JSONArray arr = (JSONArray)jsonValue;
				for (int i = 0; i < arr.length(); i++)
				{
					JSONObject update = (JSONObject)arr.get(i);
					if (update.has("handlerExec"))
					{
						// { handlerExec: {
						// 		beanName: ...,
						// 		eventType: ...,
						// 		args: ...,
						// 		rowId : ...
						// }});
						update = update.getJSONObject("handlerExec");
						if (update.has("eventType"))
						{
							if (update.has("rowId"))
							{
								String rowId = update.getString("rowId");
								FoundsetTypeValue foundsetValue = getFoundsetValue();
								if (foundsetValue != null)
								{
									foundsetValue.setEditingRowByPkHash(rowId);
								}
							}
							String eventType = update.getString("eventType");
							String beanName = update.getString("beanName");
							JSONArray jsargs = update.getJSONArray("args");
							Object[] args = new Object[jsargs == null ? 0 : jsargs.length()];
							for (int j = 0; jsargs != null && j < jsargs.length(); j++)
							{
								args[j] = jsargs.get(j);
							}

							for (WebFormComponent c : childComponents)
							{
								if (beanName.equals(c.getName()))
								{
									c.executeEvent(eventType, args);
									break;
								}
							}
						}
					}
					else if (update.has("propertyChanges"))
					{
						// { propertyChanges : {
						//	 	beanIndex: ...,
						// 		changes: ...
						// }}
						update = update.getJSONObject("propertyChanges");
						WebFormComponent c = childComponents[update.getInt("beanIndex")];
						JSONObject changes = update.getJSONObject("changes");

						Iterator<String> keys = changes.keys();
						while (keys.hasNext())
						{
							String key = keys.next();
							Object object = changes.get(key);
							c.putBrowserProperty(key, object);
						}
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}

	}
}
