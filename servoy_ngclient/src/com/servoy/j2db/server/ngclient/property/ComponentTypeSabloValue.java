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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.specification.property.types.AggregatedPropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.server.ngclient.ComponentContext;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeChangeMonitor.RowData;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

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

	protected String forFoundsetTypedPropertyName;
	protected PropertyChangeListener forFoundsetPropertyListener;
	protected ViewportDataChangeMonitor viewPortChangeMonitor;

	protected WebFormComponent parentComponent;
	protected IChangeListener monitor;
	protected PropertyDescription componentPropertyDescription;

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
		createComponentIfNeededAndPossible();
		if (forFoundsetTypedPropertyName != null)
		{
			this.parentComponent.addPropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetPropertyListener = new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					if (evt.getNewValue() != null) createComponentIfNeededAndPossible();
				}
			});
		}
	}

	@Override
	public void detach()
	{
		if (forFoundsetPropertyListener != null)
		{
			parentComponent.removePropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetPropertyListener);

			FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
			if (foundsetPropValue != null && viewPortChangeMonitor != null)
			{
				foundsetPropValue.removeViewportDataChangeMonitor(viewPortChangeMonitor);
			}
		}
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

	protected void createComponentIfNeededAndPossible()
	{
		// this method should get called only after init() got called on all properties from this component (including this one)
		// so now we should be able to find a potentially linked foundset property value
		if (componentIsCreated || parentComponent == null) return;

		FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();

		componentIsCreated = true;
		IWebFormUI formUI = parentComponent.findParent(IWebFormUI.class);
		IDataAdapterList dal = (foundsetPropValue != null ? foundsetPropValue.getDataAdapterList() : formUI.getDataAdapterList());

		childComponent = ComponentFactory.createComponent(dal.getApplication(), dal, formElementValue.element, parentComponent);
		childComponent.addPropertyChangeListener(null, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				// if some property changed that is not record-based
				if (forFoundsetTypedPropertyName == null || !formElementValue.recordBasedProperties.contains(evt.getPropertyName()))
				{
					monitor.valueChanged();
				}
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

		if (foundsetPropValue != null)
		{
			viewPortChangeMonitor = new ViewportDataChangeMonitor(monitor, new ComponentViewportRowDataProvider(dal, childComponent,
				formElementValue.recordBasedProperties));
			foundsetPropValue.addViewportDataChangeMonitor(viewPortChangeMonitor);
		}
		if (childComponent.hasChanges()) monitor.valueChanged();
	}

	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers, ComponentPropertyType componentPropertyType)
		throws JSONException
	{
		if (conversionMarkers != null) conversionMarkers.convert(ComponentPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		if (childComponent != null)
		{
			TypedData<Map<String, Object>> changes = childComponent.getChanges();

			// if the components property type is not linked to a foundset then the dataproviders/tagstring must also be sent when needed
			// but if it is linked to a foundset those should only be sent through the viewport
			if (forFoundsetTypedPropertyName != null)
			{
				// remove properties that are per record basis from the "per all model"
				for (String propertyName : formElementValue.recordBasedProperties)
				{
					changes.content.remove(propertyName);
					if (changes.contentType != null) changes.contentType.putProperty(propertyName, null);
				}
			}

			boolean modelChanged = (changes.content.size() > 0);
			boolean viewPortChanged = (forFoundsetTypedPropertyName != null && (viewPortChangeMonitor.shouldSendWholeViewport() || viewPortChangeMonitor.getViewPortChanges().size() > 0));
			boolean nothingChanged = !(modelChanged || viewPortChanged);

//			if (modelChanged || viewPortChanged)
//			{
			try
			{

				destinationJSON.object();
			}
			catch (JSONException e)
			{
				System.out.println("a");
			}
			destinationJSON.key(ComponentPropertyType.PROPERTY_UPDATES_KEY);
			destinationJSON.object();
//			}

			if (modelChanged || nothingChanged)
			{
				destinationJSON.key(ComponentPropertyType.MODEL_KEY);
				destinationJSON.object();
				// send component model (when linked to foundset only props that are not record related)
				JSONUtils.writeDataWithConversions(destinationJSON, changes.content, changes.contentType);
				destinationJSON.endObject();
			}

			if (viewPortChanged || nothingChanged)
			{
				// something in the viewport containing per-record component property values changed - send updates
				if (viewPortChangeMonitor.shouldSendWholeViewport() || nothingChanged)
				{

					FoundsetTypeViewport foundsetPropertyViewPort = getFoundsetValue().getViewPort();

					TypedData<List<Map<String, Object>>> rowsArray = viewPortChangeMonitor.getRowDataProvider().getRowData(
						foundsetPropertyViewPort.getStartIndex(), foundsetPropertyViewPort.getStartIndex() + foundsetPropertyViewPort.getSize() - 1,
						getFoundsetValue().getFoundset());

					Map<String, Object> viewPort = new HashMap<>();
					PropertyDescription viewPortTypes = null;
					viewPort.put(ComponentPropertyType.MODEL_VIEWPORT_KEY, rowsArray.content);
					if (rowsArray.contentType != null && rowsArray.contentType.hasChildProperties())
					{
						viewPortTypes = AggregatedPropertyType.newAggregatedProperty();
						viewPortTypes.putProperty(ComponentPropertyType.MODEL_VIEWPORT_KEY, rowsArray.contentType);
					}
					// convert for websocket traffic (for example Date objects will turn into long)
					JSONUtils.writeDataWithConversions(destinationJSON, viewPort, viewPortTypes);
				}
				else
				// viewPortChanges.size() > 0
				{
					List<RowData> viewPortChanges = viewPortChangeMonitor.getViewPortChanges();
					Map<String, Object> vpChanges = new HashMap<>();
					PropertyDescription vpChangeTypes = null;
					Map<String, Object>[] changesArray = new Map[viewPortChanges.size()];

					vpChanges.put(ComponentPropertyType.MODEL_VIEWPORT_CHANGES_KEY, changesArray);

					PropertyDescription changeArrayTypes = AggregatedPropertyType.newAggregatedProperty();
					for (int i = viewPortChanges.size() - 1; i >= 0; i--)
					{
						TypedData<Map<String, Object>> rowTypedData = viewPortChanges.get(i).toMap();
						changesArray[i] = rowTypedData.content;
						if (rowTypedData.contentType != null) changeArrayTypes.putProperty(String.valueOf(i), rowTypedData.contentType);
					}

					if (changeArrayTypes.hasChildProperties())
					{
						vpChangeTypes = AggregatedPropertyType.newAggregatedProperty();
						vpChangeTypes.putProperty(ComponentPropertyType.MODEL_VIEWPORT_CHANGES_KEY, changeArrayTypes);
					}

					// convert for websocket traffic (for example Date objects will turn into long)
					JSONUtils.writeDataWithConversions(destinationJSON, vpChanges, vpChangeTypes);

				}
				viewPortChangeMonitor.clearChanges();
			}

//			if (modelChanged || viewPortChanged)
//			{
			destinationJSON.endObject();
			destinationJSON.endObject();
//			}
//			else
//			{
//				// TODO send all for now - when the separate tagging interface for granular updates vs full updates is added we can send NO_OP again or send nothing
//				// TODO HERE WE need to make a distinction between initial request data (which should send all WebFormComponent properties as well as viewport)
//				// and full to JSON (which is needed in some cases for custom json objects and custom json arrays of components) - which should send both template values and form component values and viewport somehow...
//				componentPropertyType.toTemplateJSONValue(destinationJSON, null, formElementValue, componentPropertyDescription, conversionMarkers, null);
//			}
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
					// 		prop1: ...,
					// 		prop2: ...
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
				else if (update.has("viewportDataChanged"))
				{
					// component is linked to a foundset and the value of a property that depends on the record changed client side;
					// in this case update DataAdapterList with the correct record and then set the value on the component
					FoundsetTypeSabloValue foundsetPropertyValue = getFoundsetValue();
					if (foundsetPropertyValue != null && foundsetPropertyValue.getFoundset() != null)
					{
						IFoundSetInternal foundset = foundsetPropertyValue.getFoundset();
						JSONObject change = update.getJSONObject("viewportDataChanged");

						String rowIDValue = change.getString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
						String propertyName = change.getString(FoundsetTypeSabloValue.DATAPROVIDER_KEY);
						Object value = change.get(FoundsetTypeSabloValue.VALUE_KEY);

						Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowIDValue);
						int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

						if (recordIndex != -1)
						{
							foundsetPropertyValue.getDataAdapterList().setRecord(foundset.getRecord(recordIndex), false);

							viewPortChangeMonitor.pauseRowUpdateListener(splitHashAndIndex.getLeft());
							try
							{
								childComponent.putBrowserProperty(propertyName, value);
							}
							catch (JSONException e)
							{
								Debug.error("Setting value for record dependent property '" + propertyName + "' in foundset linked component to value: " +
									value + " failed.", e);
							}
							finally
							{
								viewPortChangeMonitor.resumeRowUpdateListener();
							}
						}
						else
						{
							Debug.error("Cannot set foundset linked record dependent component property for (" + rowIDValue + ") property '" + propertyName +
								"' to value '" + value + ". Record not found.");
						}
					}
					else
					{
						Debug.error("Component updates received for record linked property, but component is not linked to a foundset: " +
							update.get("viewportDataChanged"));
					}
				}
//				var r = {};
//				r[$foundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][$foundsetTypeConstants.ROW_ID_COL_KEY];
//				r.dp = dataprovider;
//				r.value = newData;
//
//				// convert new data if necessary
//				var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][r[$foundsetTypeConstants.ROW_ID_COL_KEY]] : undefined;
//				if (conversionInfo && conversionInfo[dataprovider]) r.value = $sabloConverters.convertFromClientToServer(r.value, conversionInfo[dataprovider], oldData);
//				else r.value = $sabloUtils.convertClientObject(r.value);
//
//				internalState.requests.push({viewportDataChanged: r});

			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}
}
