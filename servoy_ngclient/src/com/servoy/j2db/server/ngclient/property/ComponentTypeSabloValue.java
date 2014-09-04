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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.ComponentContext;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.Debug;

/**
 * Value used at runtime in Sablo component.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ComponentTypeSabloValue implements ISmartPropertyValue
{

	protected WebFormComponent childComponent;

	protected boolean componentIsCreated = false;

	protected WebFormComponent parentComponent;
	protected PropertyChangeListener forFoundsetListener;
	protected IChangeListener monitor;
	protected PropertyDescription componentPropertyDescription;
	protected String forFoundsetTypedPropertyName;

	protected final ComponentTypeFormElementValue formElementValue;

	public ComponentTypeSabloValue(ComponentTypeFormElementValue formElementValue, PropertyDescription componentPropertyDescription,
		String forFoundsetTypedPropertyName)
	{
		this.formElementValue = formElementValue;
		this.forFoundsetTypedPropertyName = forFoundsetTypedPropertyName;
		this.componentPropertyDescription = componentPropertyDescription;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, org.sablo.BaseWebObject parentComponent)
	{
		componentIsCreated = false;
		this.parentComponent = (WebFormComponent)parentComponent;
		this.monitor = changeMonitor;

		if (childComponent != null)
		{
			childComponent.dispose();
		}
		createComponentsIfNeededAndPossible();
		if (forFoundsetTypedPropertyName != null)
		{
			this.parentComponent.addPropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetListener = new PropertyChangeListener()
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
		if (forFoundsetListener != null) parentComponent.removePropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetListener);
	}

	private FoundsetTypeSabloValue getFoundsetValue()
	{
		if (parentComponent != null)
		{
			String foundsetPropName = forFoundsetTypedPropertyName;
			if (foundsetPropName != null)
			{
				return (FoundsetTypeSabloValue)parentComponent.getProperty(foundsetPropName);
			}
		}
		return null;
	}

	protected void createComponentsIfNeededAndPossible()
	{
		// this method should get called only after init() got called on all properties from this component (including this one)
		// so now we should be able to find a potentially linked foundset property value
		if (componentIsCreated || parentComponent == null) return;

		FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
		if (foundsetPropValue == null) return; // Cannot find linked foundset property; it is possible that that property was not yet attached to the component; we can wait for that to happen before creating components; see foundsetPropertyReady()

		componentIsCreated = true;
		IWebFormUI formUI = parentComponent.findParent(IWebFormUI.class);
		IDataAdapterList dal = (foundsetPropValue != null ? foundsetPropValue.getDataAdapterList() : formUI.getDataAdapterList());

		childComponent = ComponentFactory.createComponent(dal.getApplication(), dal, formElementValue.element, parentComponent);
		childComponent.addPropertyChangeListener(null, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				monitor.valueChanged();
			}
		});
		childComponent.setComponentContext(new ComponentContext(formElementValue.propertyPath));
		formUI.contributeComponentToElementsScope(formElementValue.element, formElementValue.element.getWebComponentSpec(), childComponent);
		for (String handler : childComponent.getFormElement().getHandlers())
		{
			Object value = childComponent.getFormElement().getPropertyValue(handler);
			if (value != null)
			{
				childComponent.add(handler, (Integer)value);
			}
		}

		registerDataProvidersWithFoundset(foundsetPropValue);
	}

	/**
	 * Let linked foundset property know which dataprovider/tagstrings it should send client-side.
	 */
	protected void registerDataProvidersWithFoundset(FoundsetTypeSabloValue foundsetPropValue)
	{
		if (foundsetPropValue != null)
		{
			HashSet<String> allDataProviders = new HashSet<String>();
			allDataProviders.addAll(formElementValue.dataLinks.values());
			foundsetPropValue.includeDataProviders(allDataProviders);
		}
	}

	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers, ComponentPropertyType componentPropertyType)
		throws JSONException
	{
		if (conversionMarkers != null) conversionMarkers.convert(ComponentPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		// TODO if the components property type is not linked to a foundset then somehow the dataproviders/tagstring must also be sent when needed
		// but if it is linked to a foundset those should only be sent through the foundset!
		if (childComponent != null)
		{
			TypedData<Map<String, Object>> changes = childComponent.getChanges();
			if (changes.content.size() > 0)
			{
				destinationJSON.object();
				destinationJSON.key(ComponentPropertyType.PROPERTY_UPDATES);
				destinationJSON.object();
				JSONUtils.writeDataWithConversions(destinationJSON, changes.content, changes.contentType);
				destinationJSON.endObject();
				destinationJSON.endObject();
			}
			else
			{
				// TODO send all for now - when the separate tagging interface for granular updates vs full updates is added we can send NO_OP again or send nothing
				componentPropertyType.toTemplateJSONValue(destinationJSON, null, formElementValue, componentPropertyDescription, conversionMarkers);
			}
		}
		return destinationJSON;
	}

	public void browserUpdatesReceived(Object jsonValue)
	{
		if (childComponent == null) return;

		try
		{
			JSONArray updates = (JSONArray)jsonValue;
			for (int i = 0; i < updates.length(); i++)
			{
				JSONObject update = (JSONObject)updates.get(i);
				if (update.has("handlerExec"))
				{
					// { handlerExec: {
					// 		eventType: ...,
					// 		args: ...,
					// 		rowId : ...
					// }});
					update = update.getJSONObject("handlerExec");
					if (update.has("eventType"))
					{
						boolean selectionOk = true;
						if (update.has("rowId"))
						{
							String rowId = update.getString("rowId");
							FoundsetTypeSabloValue foundsetValue = getFoundsetValue();
							if (foundsetValue != null)
							{
								if (!foundsetValue.setEditingRowByPkHash(rowId))
								{
									Debug.error("Cannot select row when event was fired; row identifier: " + rowId);
									selectionOk = false;
								}
							}
						}
						if (selectionOk)
						{
							String eventType = update.getString("eventType");
//						String beanName = update.getString("beanName");
							JSONArray jsargs = update.getJSONArray("args");
							Object[] args = new Object[jsargs == null ? 0 : jsargs.length()];
							for (int j = 0; jsargs != null && j < jsargs.length(); j++)
							{
								args[j] = jsargs.get(j);
							}

							childComponent.executeEvent(eventType, args); // TODO
						}
					}
				}
				else if (update.has("propertyChanges"))
				{
					// { propertyChanges : {
					//	 	beanIndex: ...,
					// 		changes: ...
					// }}
					JSONObject changes = update.getJSONObject("propertyChanges");

					Iterator<String> keys = changes.keys();
					while (keys.hasNext())
					{
						String key = keys.next();
						Object object = changes.get(key);
						childComponent.putBrowserProperty(key, object);
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
