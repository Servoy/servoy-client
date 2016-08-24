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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IllegalComponentAccessException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.ChangesToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.ngclient.ComponentContext;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IDirtyPropertyListener;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType.IModelWriter;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeChangeMonitor.RowData;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.IWrapperDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.FormElementToJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.InitialToJSONConverter;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONArrayType;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;
import com.servoy.j2db.server.ngclient.property.types.ReadonlyPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ReadonlySabloValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Value used at runtime in Sablo component.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ComponentTypeSabloValue implements ISmartPropertyValue
{
	public static final Logger log = LoggerFactory.getLogger(ComponentTypeSabloValue.class.getCanonicalName());

	public static final String NO_OP = "n";

	protected WebFormComponent childComponent;

	protected boolean componentIsCreated = false;

	protected String forFoundsetTypedPropertyName;
	protected PropertyChangeListener forFoundsetPropertyListener, readonlyPropertyListener;

	protected boolean recordBasedPropertiesChanged = false;
	protected boolean recordBasedPropertiesChangedComparedToTemplate = false;
	protected ViewportDataChangeMonitor<ComponentViewportRowDataProvider> viewPortChangeMonitor;
	protected List<Runnable> changesWhileUpdatingFoundsetBasedDPFromClient;

	protected ComponentDataLinkedPropertyListener dataLinkedPropertyRegistrationListener; // only used in case component is foundset-linked
	protected final List<String> recordBasedProperties;

	protected WebFormComponent parentComponent;
	protected IChangeListener monitor;
	protected PropertyDescription componentPropertyDescription;

	protected final ComponentTypeFormElementValue formElementValue;

	public ComponentTypeSabloValue(ComponentTypeFormElementValue formElementValue, PropertyDescription componentPropertyDescription,
		String forFoundsetTypedPropertyName)
	{
		this.formElementValue = formElementValue;
		this.forFoundsetTypedPropertyName = forFoundsetTypedPropertyName;
		this.recordBasedProperties = forFoundsetTypedPropertyName != null ? new ArrayList<>(formElementValue.recordBasedProperties) : null;
		this.componentPropertyDescription = componentPropertyDescription;
	}

	public String getName()
	{
		if (formElementValue != null && formElementValue.element != null)
		{
			return formElementValue.element.getName();
		}
		return null;
	}

	public PropertyDescription getComponentPropertyDescription()
	{
		return componentPropertyDescription;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, org.sablo.BaseWebObject parentComp)
	{
		componentIsCreated = false;
		this.parentComponent = (WebFormComponent)parentComp;
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

	private void setDataproviderNameToFoundset()
	{
		FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
		Collection<PropertyDescription> dp = childComponent.getSpecification().getProperties(DataproviderPropertyType.INSTANCE);
		if (dp.size() > 0)
		{
			//get the first dataprovider property for now
			PropertyDescription propertyDesc = dp.iterator().next();
			Object propertyValue = childComponent.getProperty(propertyDesc.getName());
			if (propertyValue != null)
			{
				String dataprovider = ((DataproviderTypeSabloValue)propertyValue).getDataProviderID();
				foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(childComponent.getName(), dataprovider);
			}
		}
	}

	@Override
	public void detach()
	{
		if (forFoundsetPropertyListener != null)
		{
			parentComponent.removePropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetPropertyListener);

			FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
			if (foundsetPropValue != null)
			{
				if (viewPortChangeMonitor != null) foundsetPropValue.removeViewportDataChangeMonitor(viewPortChangeMonitor);
				if (dataLinkedPropertyRegistrationListener != null)
				{
					FoundsetDataAdapterList dal = foundsetPropValue.getDataAdapterList();
					if (dal != null) dal.removeDataLinkedPropertyRegistrationListener(dataLinkedPropertyRegistrationListener);
				}
			}
		}
		if (readonlyPropertyListener != null) parentComponent.removePropertyChangeListener(WebFormUI.READONLY, readonlyPropertyListener);
	}

	private FoundsetTypeSabloValue getFoundsetValue()
	{
		if (parentComponent != null)
		{
			if (forFoundsetTypedPropertyName != null)
			{
				return (FoundsetTypeSabloValue)parentComponent.getProperty(forFoundsetTypedPropertyName);
			}
		}
		return null;
	}

	protected void createComponentIfNeededAndPossible()
	{
		// this method should get called only after init() got called on all properties from this component (including this one)
		// so now we should be able to find a potentially linked foundset property value
		if (componentIsCreated || parentComponent == null) return;

		final FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();

		if (foundsetPropValue == null && forFoundsetTypedPropertyName != null) return;

		componentIsCreated = true;
		IWebFormUI formUI = parentComponent.findParent(IWebFormUI.class);
		final IDataAdapterList dal = (foundsetPropValue != null ? foundsetPropValue.getDataAdapterList() : formUI.getDataAdapterList());

		if (foundsetPropValue != null)
		{
			// do this before creating the component so that any attach() methods of it's properties that register data links get caught
			((FoundsetDataAdapterList)dal).addDataLinkedPropertyRegistrationListener(createDataLinkedPropertyRegistrationListener());
		}

		childComponent = ComponentFactory.createComponent(dal.getApplication(), dal, formElementValue.element, parentComponent,
			formUI.getController().getForm());

		if (foundsetPropValue != null)
		{
			dataLinkedPropertyRegistrationListener.componentIsNowAvailable();
		}
		childComponent.setDirtyPropertyListener(new IDirtyPropertyListener()
		{

			@Override
			public void propertyFlaggedAsDirty(String propertyName, boolean dirty)
			{
				if (dirty)
				{
					// this gets called whenever a property is flagged as dirty/changed/to be sent to browser
					if (forFoundsetTypedPropertyName != null && recordBasedProperties.contains(propertyName))
					{
						if (!((FoundsetDataAdapterList)dal).isQuietRecordChangeInProgress() && foundsetPropValue.getFoundset() != null &&
							!foundsetPropValue.getFoundset().isInFindMode()) // if forFoundsetTypedPropertyName != null we are using a foundset DAL, so just cast
						{
							// for example valuelist properties can get filtered based on client sent filter in which case the property does change without
							// any actual change in the record; in this case we need to mark it correctly in viewport as a change
							IRecordInternal record = dal.getRecord();
							Runnable queueChangeRunnable = queueCellChangeOnRecord(propertyName, record);

							if (changesWhileUpdatingFoundsetBasedDPFromClient != null)
							{
								// if for example a dataprovider property change does in its fromJSON a monitor.valueChanged() (for example an integer DP getting client update of 1.15 would want to send back 1.00)
								// it will end up here; we do want to send that back to the client but as the new value is not
								// yet pushed to the record, we don't want the new value to be reverted by a DAL.setRecord() that happens when queuing changes for a specific record index
								// so we need to handle this change at a later time
								changesWhileUpdatingFoundsetBasedDPFromClient.add(queueChangeRunnable);
							}
							else
							{
								queueChangeRunnable.run();
							}
						}
						else
						{
							// else this change was probably determined by the fact that we reuse components, changing the record in the DAL to get data for a specific row;
							// so we need to clear component changes for this property because we do not notify the parent here (we want to ignore the change) so
							// we shouldn't keep the property marked as dirty - thus blocking future property changes to generate a valueChanged on parent's monitor
							childComponent.flagPropertyAsDirty(propertyName, false);
						}
					}
					else
					{
						// non-record related prop. changed...
						monitor.valueChanged();
					}
				}
			}

			private Runnable queueCellChangeOnRecord(final String propertyName, final IRecordInternal record)
			{
				return new Runnable()
				{

					@Override
					public void run()
					{
						int idx = foundsetPropValue.getFoundset().getRecordIndex(record);
						if (idx >= 0)
						{
							FoundsetTypeViewport viewPort = foundsetPropValue.getViewPort();
							int relativeIdx = idx - viewPort.getStartIndex();
							if (relativeIdx >= 0 && relativeIdx < viewPort.getStartIndex() + viewPort.getSize())
							{
								viewPortChangeMonitor.queueCellChange(relativeIdx, idx, propertyName, foundsetPropValue.getFoundset());
							}
						}
					}
				};
			}
		});

		for (String initialChangedProperty : childComponent.getProperties().content.keySet())
		{
			if (forFoundsetTypedPropertyName == null || !recordBasedProperties.contains(initialChangedProperty))
			{
				// non-record related prop. initially changed...
				monitor.valueChanged();
			}
		}

		childComponent.setComponentContext(new ComponentContext(formElementValue.propertyPath));
		if (componentPropertyDescription != null && Utils.getAsBoolean(componentPropertyDescription.getTag("addToElementsScope")))
		{
			formUI.contributeComponentToElementsScope(formElementValue.element, formElementValue.element.getWebComponentSpec(), childComponent);
		}
		for (String handler : childComponent.getFormElement().getHandlers())
		{
			Object value = childComponent.getFormElement().getPropertyValue(handler);
			if (value instanceof String)
			{
				UUID uuid = UUID.fromString((String)value);
				IPersist function = formUI.getController().getApplication().getFlattenedSolution().searchPersist(uuid);
				if (function != null)
				{
					childComponent.add(handler, function.getID());
				}
				else
				{
					Debug.warn("Event handler for " + handler + " not found (form " + formUI.getController().getName() + ", form element " +
						childComponent.getFormElement().getName() + ")");
				}
			}
			else if (value instanceof Number && ((Number)value).intValue() > 0)
			{
				childComponent.add(handler, ((Number)value).intValue());
			}
		}

		if (foundsetPropValue != null)
		{
			viewPortChangeMonitor = new ViewportDataChangeMonitor<>(monitor,
				new ComponentViewportRowDataProvider((FoundsetDataAdapterList)dal, childComponent, recordBasedProperties, this));
			foundsetPropValue.addViewportDataChangeMonitor(viewPortChangeMonitor);
			setDataproviderNameToFoundset();
		}

		addPropertyChangeListener(WebFormUI.READONLY, parentComponent.getProperty(WebFormUI.READONLY));


		if (childComponent.hasChanges()) monitor.valueChanged();
	}

	private void addPropertyChangeListener(final String property, Object initialValue)
	{
		if (parentComponent.getSpecification().getProperty(property) != null && childComponent.getSpecification().getProperty(property) != null)
		{
			PropertyDescription propertyDescChild = childComponent.getSpecification().getProperty(property);
			if (childComponent.getProperty(property) == null || !propertyDescChild.hasDefault() ||
				childComponent.getProperty(property).equals(propertyDescChild.getDefaultValue()))
			{
				setChildProperty(property, initialValue);
				this.parentComponent.addPropertyChangeListener(property, readonlyPropertyListener = new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						if (evt.getNewValue() != null)
						{
							setChildProperty(property, evt.getNewValue());
						}
					}
				});
			}
		}
	}

	private void setChildProperty(String propertyName, Object value)
	{
		Object val = value instanceof ReadonlySabloValue ? ((ReadonlySabloValue)value).getValue() : value;
		if (childComponent.getProperty(propertyName) == null || !childComponent.getProperty(propertyName).equals(val)) //check if the values are different
		{
			if (WebFormUI.READONLY.equals(propertyName))
			{
				PropertyDescription propertyDescChild = childComponent.getSpecification().getProperty(WebFormUI.READONLY);
				if (propertyDescChild.getType() instanceof ReadonlyPropertyType)
				{
					val = ReadonlyPropertyType.INSTANCE.toSabloComponentValue(val, (ReadonlySabloValue)childComponent.getProperty(WebFormUI.READONLY),
						propertyDescChild, childComponent);
				}
			}

			childComponent.setProperty(propertyName, val);
		}
	}

	protected IDataLinkedPropertyRegistrationListener createDataLinkedPropertyRegistrationListener()
	{
		return dataLinkedPropertyRegistrationListener = new ComponentDataLinkedPropertyListener();
	}

	protected String findComponentPropertyName(IDataLinkedPropertyValue propertyValueToFind)
	{
		Set<String> allPropNames = childComponent.getAllPropertyNames(true);
		WebObjectSpecification spec = childComponent.getSpecification();
		for (String n : allPropNames)
			if (nestedPropertyFound(propertyValueToFind, childComponent.getProperty(n), spec.getProperty(n))) return n;
		return null;
	}

	/**
	 * Searches for a nested property value "propertyValueToFind" inside a possibly nested object/array "propertyValue"
	 * TODO this is a hackish chunk of code; we should find a cleaner way to identify rootPropertyNames of data linked values!
	 */
	protected boolean nestedPropertyFound(IDataLinkedPropertyValue propertyValueToFind, Object propertyValue, PropertyDescription propertyDescription)
	{
		if (propertyValue == propertyValueToFind) return true;
		if (propertyValue == null || propertyDescription == null) return false;

		if (propertyDescription.getType() instanceof NGCustomJSONObjectType && propertyValue instanceof Map)
		{
			PropertyDescription nestedPDs = ((NGCustomJSONObjectType)propertyDescription.getType()).getCustomJSONTypeDefinition();
			for (Entry<String, Object> e : ((Map<String, Object>)propertyValue).entrySet())
			{
				if (nestedPropertyFound(propertyValueToFind, e.getValue(), nestedPDs.getProperty(e.getKey()))) return true;
			}
		}
		else if (propertyDescription.getType() instanceof NGCustomJSONArrayType && propertyValue instanceof List)
		{
			PropertyDescription nestedPD = ((NGCustomJSONArrayType)propertyDescription.getType()).getCustomJSONTypeDefinition();
			for (Object e : (List)propertyValue)
			{
				if (nestedPropertyFound(propertyValueToFind, e, nestedPD)) return true;
			}
		}
		else if (propertyDescription.getType() instanceof IWrapperDataLinkedType)
		{
			Pair<IDataLinkedPropertyValue, PropertyDescription> tmp = ((IWrapperDataLinkedType)propertyDescription.getType()).getWrappedDataLinkedValue(
				propertyValue, propertyDescription);
			return nestedPropertyFound(propertyValueToFind, tmp.getLeft(), tmp.getRight());
		}

		return false;
	}

	/**
	 * Writes a diff update between the value it has in the template and the initial data requested after runtime components were created or during a page refresh.
	 * @param componentPropertyType
	 */
	public JSONWriter initialToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers, ComponentPropertyType componentPropertyType)
		throws JSONException
	{
		if (recordBasedPropertiesChangedComparedToTemplate) return fullToJSON(destinationJSON, conversionMarkers, componentPropertyType);

		if (conversionMarkers != null) conversionMarkers.convert(ComponentPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		destinationJSON.object();
		destinationJSON.key(ComponentPropertyType.PROPERTY_UPDATES_KEY);
		destinationJSON.object();

		// model content
		TypedData<Map<String, Object>> allProps = childComponent.getProperties();
		childComponent.getAndClearChanges(); // just for clear
		removeRecordDependentProperties(allProps);

		destinationJSON.key(ComponentPropertyType.MODEL_KEY);
		destinationJSON.object();

		DataConversion conversions = new DataConversion();
		// send component model (when linked to foundset only props that are not record related)
		childComponent.writeProperties(InitialToJSONConverter.INSTANCE, destinationJSON, allProps.content, allProps.contentType, conversions);
		JSONUtils.writeClientConversions(destinationJSON, conversions);

		destinationJSON.endObject();

		// viewport content
		writeWholeViewportToJSON(destinationJSON);

		destinationJSON.endObject();
		destinationJSON.endObject();

		return destinationJSON;
	}

	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers, ComponentPropertyType componentPropertyType)
		throws JSONException
	{
		if (recordBasedPropertiesChanged)
		{
			// just send over the whole thing - viewport and model properties are not the same as they used to be
			return fullToJSON(destinationJSON, conversionMarkers, componentPropertyType);
		}

		if (conversionMarkers != null) conversionMarkers.convert(ComponentPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		TypedData<Map<String, Object>> changes = childComponent.getAndClearChanges();
		removeRecordDependentProperties(changes);

		boolean modelChanged = (changes.content.size() > 0);
		boolean viewPortChanged = (forFoundsetTypedPropertyName != null &&
			(viewPortChangeMonitor.shouldSendWholeViewport() || viewPortChangeMonitor.getViewPortChanges().size() > 0));

		destinationJSON.object();
		if (modelChanged || viewPortChanged)
		{
			destinationJSON.key(ComponentPropertyType.PROPERTY_UPDATES_KEY);
			destinationJSON.object();
		}

		if (modelChanged)
		{
			destinationJSON.key(ComponentPropertyType.MODEL_KEY);
			destinationJSON.object();

			DataConversion conversions = new DataConversion();
			// send component model (when linked to foundset only props that are not record related)
			childComponent.writeProperties(ChangesToJSONConverter.INSTANCE, destinationJSON, changes.content, changes.contentType, conversions);
			JSONUtils.writeClientConversions(destinationJSON, conversions);

			destinationJSON.endObject();
		}

		if (viewPortChanged)
		{
			// something in the viewport containing per-record component property values changed - send updates
			if (viewPortChangeMonitor.shouldSendWholeViewport())
			{
				writeWholeViewportToJSON(destinationJSON);
			}
			else
			// viewPortChanges.size() > 0
			{
				List<RowData> viewPortChanges = viewPortChangeMonitor.getViewPortChanges();
				DataConversion clientConversionInfo = new DataConversion();

				clientConversionInfo.pushNode(ComponentPropertyType.MODEL_VIEWPORT_CHANGES_KEY);
				destinationJSON.key(ComponentPropertyType.MODEL_VIEWPORT_CHANGES_KEY).array();

				for (int i = 0; i < viewPortChanges.size(); i++)
				{
					clientConversionInfo.pushNode(String.valueOf(i));
					viewPortChanges.get(i).writeJSONContent(destinationJSON, null, FullValueToJSONConverter.INSTANCE, clientConversionInfo);
					clientConversionInfo.popNode();
				}
				clientConversionInfo.popNode();
				destinationJSON.endArray();

				// conversion info for websocket traffic (for example Date objects will turn into long)
				JSONUtils.writeClientConversions(destinationJSON, clientConversionInfo);

			}
			viewPortChangeMonitor.clearChanges();
		}

		if (modelChanged || viewPortChanged)
		{
			destinationJSON.endObject();
		}
		else
		{
			// no change yet we are still asked to send changes (so not full value); send a dummy NO_OP
			destinationJSON.key(NO_OP).value(true);
		}
		destinationJSON.endObject();

		return destinationJSON;
	}

	protected void writeWholeViewportToJSON(JSONWriter destinationJSON) throws JSONException
	{
		if (forFoundsetTypedPropertyName != null)
		{
			FoundsetTypeViewport foundsetPropertyViewPort = getFoundsetValue().getViewPort();

			DataConversion clientConversionInfo = new DataConversion();

			destinationJSON.key(ComponentPropertyType.MODEL_VIEWPORT_KEY);
			clientConversionInfo.pushNode(ComponentPropertyType.MODEL_VIEWPORT_KEY);
			viewPortChangeMonitor.getRowDataProvider().writeRowData(foundsetPropertyViewPort.getStartIndex(),
				foundsetPropertyViewPort.getStartIndex() + foundsetPropertyViewPort.getSize() - 1, getFoundsetValue().getFoundset(), destinationJSON,
				clientConversionInfo);
			clientConversionInfo.popNode();

			viewPortChangeMonitor.clearChanges();

			// conversion info for websocket traffic (for example Date objects will turn into long)
			JSONUtils.writeClientConversions(destinationJSON, clientConversionInfo);
		}
	}

	/**
	 * Writes the entire value of this property as JSON. This includes the template values, not just the runtime component properties.
	 * This is currently needed and can get called if the property is nested inside other complex properties (json object/array) that sometimes
	 * might want/need to send again the entire content.
	 */
	public JSONWriter fullToJSON(final JSONWriter writer, DataConversion conversionMarkers, ComponentPropertyType componentPropertyType) throws JSONException
	{
		if (conversionMarkers != null) conversionMarkers.convert(ComponentPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		// create children of component as specified by this property
		final FormElement fe = formElementValue.element;

		writer.object();

		// get template model values
		final TypedData<Map<String, Object>> formElementProperties = fe.propertiesForTemplateJSON();

		// we'll need to update them with runtime values
		final TypedData<Map<String, Object>> runtimeProperties = childComponent.getProperties();
		childComponent.getAndClearChanges(); // just for clear

		// add to useful properties only those formElement properties that didn't get overriden at runtime (so form element value is still used)
		boolean templateValuesRemoved = false;
		Iterator<Entry<String, Object>> formElementPropertyIterator = formElementProperties.content.entrySet().iterator();
		while (formElementPropertyIterator.hasNext())
		{
			Entry<String, Object> fePropEntry = formElementPropertyIterator.next();
			if (runtimeProperties.content.containsKey(fePropEntry.getKey()))
			{
				// it has a non-default runtime value; so template value will be ignored/not sent
				if (!templateValuesRemoved)
				{
					formElementProperties.content = new HashMap<String, Object>(formElementProperties.content); // otherwise it's unmodifiable
					templateValuesRemoved = true;
				}
				formElementProperties.content.remove(fePropEntry.getKey());
			}
		}

		removeRecordDependentProperties(runtimeProperties);
		removeRecordDependentProperties(formElementProperties);

		final FormElementContext formElementContext = new FormElementContext(fe);
		componentPropertyType.writeTemplateJSONContent(writer, formElementValue, forFoundsetTypedPropertyName, formElementContext, new IModelWriter()
		{

			@Override
			public void writeComponentModel() throws JSONException
			{
				writer.object();
				DataConversion dataConversion = new DataConversion();
				JSONUtils.writeData(FormElementToJSON.INSTANCE, writer, formElementProperties.content, formElementProperties.contentType, dataConversion,
					formElementContext);
				childComponent.writeProperties(JSONUtils.FullValueToJSONConverter.INSTANCE, writer, runtimeProperties.content, runtimeProperties.contentType,
					dataConversion);
				JSONUtils.writeClientConversions(writer, dataConversion);
				writer.endObject();
			}

		}, recordBasedProperties, false);
		recordBasedPropertiesChanged = false;

		writeWholeViewportToJSON(writer);
		if (viewPortChangeMonitor != null) viewPortChangeMonitor.clearChanges();

		writer.endObject();

		return writer;
	}

	protected void removeRecordDependentProperties(TypedData<Map<String, Object>> changes)
	{
		// if the components property type is not linked to a foundset then the dataproviders/tagstring must also be sent when needed
		// but if it is linked to a foundset those should only be sent through the viewport
		if (forFoundsetTypedPropertyName != null)
		{
			// remove properties that are per record basis from the "per all model"
			for (String propertyName : recordBasedProperties)
			{
				try
				{
					changes.content.remove(propertyName);
				}
				catch (UnsupportedOperationException e)
				{
					changes.content = new HashMap<String, Object>(changes.content);
					changes.content.remove(propertyName);
				}
				if (changes.contentType != null) changes.contentType.putProperty(propertyName, null);
			}
		}
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
							String rowId = update.optString("rowId");
							if (rowId != null)
							{
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

							Object result = null;
							String error = null;
							try
							{
								result = childComponent.executeEvent(eventType, args);
							}
							catch (ParseException pe)
							{
								log.warn("Warning: " + pe.getMessage(), pe);
							}
							catch (IllegalComponentAccessException ilcae)
							{
								log.warn("Warning: " + ilcae.getMessage());
							}
							catch (Exception e)
							{
								error = "Error: " + e.getMessage();
								log.error(error, e);
							}

							int cmsid = update.optInt("defid", -1);
							if (cmsid != -1)
							{
								if (error == null)
								{
									Object resultObject = result;
									PropertyDescription objectType = null;
									if (result instanceof TypedData)
									{
										resultObject = ((TypedData< ? >)result).content;
										objectType = ((TypedData< ? >)result).contentType;
									}
									CurrentWindow.get().getSession().getSabloService().resolveDeferedEvent(cmsid, true, resultObject, objectType);
								}
								else
								{
									CurrentWindow.get().getSession().getSabloService().resolveDeferedEvent(cmsid, false, error, null);
								}
							}
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
				else if (update.has(ViewportDataChangeMonitor.VIEWPORT_CHANGED))
				{
					// component is linked to a foundset and the value of a property that depends on the record changed client side;
					// in this case update DataAdapterList with the correct record and then set the value on the component
					FoundsetTypeSabloValue foundsetPropertyValue = getFoundsetValue();
					if (foundsetPropertyValue != null && foundsetPropertyValue.getFoundset() != null)
					{
						JSONObject change = update.getJSONObject(ViewportDataChangeMonitor.VIEWPORT_CHANGED);

						String rowIDValue = change.getString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
						String propertyName = change.getString(FoundsetTypeSabloValue.DATAPROVIDER_KEY);
						Object value = change.get(FoundsetTypeSabloValue.VALUE_KEY);

						updatePropertyValueForRecord(foundsetPropertyValue, rowIDValue, propertyName, value);
					}
					else
					{
						Debug.error("Component updates received for record linked property, but component is not linked to a foundset: " +
							update.get(ViewportDataChangeMonitor.VIEWPORT_CHANGED));
					}
				}
				else if (update.has("svyApply"))
				{
					// { svyApply: {
					// 		rowId: rowId, // only when linked to foundset
					// 		propertyName: property,
					// 		propertyValue: propertyValue
					// }}
					JSONObject changeAndApply = update.getJSONObject("svyApply");

					String propertyName = changeAndApply.getString(ComponentPropertyType.PROPERTY_NAME_KEY);
					Object value = changeAndApply.get(ComponentPropertyType.VALUE_KEY);

					IDataAdapterList dal;
					try
					{
						if (forFoundsetTypedPropertyName != null && recordBasedProperties.contains(propertyName))
						{
							// changes component record and sets value
							String rowIDValue = changeAndApply.getString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
							changesWhileUpdatingFoundsetBasedDPFromClient = new ArrayList<>(); // we prevent a fromJSON on the dataprovider value that triggers valueChanged (so propertyFlaggedAsDirty) to re-apply (old) record values to DPs (effectively reverting the new value)
							// this can happen for example with integer DPs that get a double value from the browser and they round/trunc thus need to resend the value to client
							// we will execute the propertyFlaggedAsDirty code later, after DP value was applied
							// TODO shouldn't we apply in one go? so apply directly the value to record instead of setting it first in the component DP property?
							updatePropertyValueForRecord(getFoundsetValue(), rowIDValue, propertyName, value);
							dal = getFoundsetValue().getDataAdapterList();
						}
						else
						{
							childComponent.putBrowserProperty(propertyName, value);
							IWebFormUI formUI = parentComponent.findParent(IWebFormUI.class);
							dal = formUI.getDataAdapterList();
						}
						if (forFoundsetTypedPropertyName != null && !recordBasedProperties.contains(propertyName))
						{
							childComponent.flagPropertyAsDirty(propertyName, true);
						}
						// apply change to record/dp
						dal.pushChanges(childComponent, propertyName);
					}
					finally
					{
						if (changesWhileUpdatingFoundsetBasedDPFromClient != null)
						{
							for (Runnable r : changesWhileUpdatingFoundsetBasedDPFromClient)
								r.run();
							changesWhileUpdatingFoundsetBasedDPFromClient = null;
						}
					}
				}
				else if (update.has("svyStartEdit"))
				{
					// { svyStartEdit: {
					//   rowId: rowId, // only if linked to foundset
					//   propertyName: property
					// }}
					JSONObject startEditData = update.getJSONObject("svyStartEdit");

					String propertyName = startEditData.getString(ComponentPropertyType.PROPERTY_NAME_KEY);

					IDataAdapterList dal;
					if (forFoundsetTypedPropertyName != null && recordBasedProperties.contains(propertyName))
					{
						String rowIDValue = startEditData.getString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
						IFoundSetInternal foundset = getFoundsetValue().getFoundset();
						dal = getFoundsetValue().getDataAdapterList();

						Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowIDValue);
						if (foundset != null)
						{
							int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

							if (recordIndex != -1)
							{
								((FoundsetDataAdapterList)dal).setRecordQuietly(foundset.getRecord(recordIndex));
							}
							else
							{
								Debug.error("Cannot find record for foundset linked record dependent component property - startEdit (" + rowIDValue +
									"); property '" + propertyName, new RuntimeException());
							}
						}
						else
						{
							Debug.error("Foundset is null while trying to startEdit for foundset linked record dependent component property (" + rowIDValue +
								"); property '" + propertyName, new RuntimeException());
						}
					}
					else
					{
						IWebFormUI formUI = parentComponent.findParent(IWebFormUI.class);
						dal = formUI.getDataAdapterList();
					}

					dal.startEdit(childComponent, propertyName);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	protected void updatePropertyValueForRecord(FoundsetTypeSabloValue foundsetPropertyValue, String rowIDValue, String propertyName, Object value)
	{
		IFoundSetInternal foundset = foundsetPropertyValue.getFoundset();

		Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowIDValue);
		if (foundset != null)
		{
			int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

			if (recordIndex != -1)
			{
				foundsetPropertyValue.getDataAdapterList().setRecordQuietly(foundset.getRecord(recordIndex));

				viewPortChangeMonitor.pauseRowUpdateListener(splitHashAndIndex.getLeft());
				try
				{
					childComponent.putBrowserProperty(propertyName, value);
				}
				catch (JSONException e)
				{
					Debug.error(
						"Setting value for record dependent property '" + propertyName + "' in foundset linked component to value: " + value + " failed.", e);
				}
				finally
				{
					viewPortChangeMonitor.resumeRowUpdateListener();
				}
			}
			else
			{
				Debug.error("Cannot set foundset linked record dependent component property for (" + rowIDValue + ") property '" + propertyName +
					"' to value '" + value + "' of component: " + childComponent + ". Record not found.", new RuntimeException());
			}
		}
		else
		{
			Debug.error("Cannot set foundset linked record dependent component property for (" + rowIDValue + ") property '" + propertyName + "' to value '" +
				value + "' of component: " + childComponent + ". Foundset is null.", new RuntimeException());
		}
	}

	public Object getRuntimeComponent()
	{
		if (childComponent != null)
		{
			return new RuntimeWebComponent(childComponent, childComponent.getSpecification());
		}
		return null;
	}

	protected final class ComponentDataLinkedPropertyListener implements IDataLinkedPropertyRegistrationListener
	{
		private final Map<IDataLinkedPropertyValue, String> oldDataLinkedValuesToRootPropertyName = new HashMap<IDataLinkedPropertyValue, String>();
		private List<IDataLinkedPropertyValue> initiallyAddedValuesWhileComponentIsNull = new ArrayList<IDataLinkedPropertyValue>();

		@Override
		public void dataLinkedPropertyRegistered(IDataLinkedPropertyValue propertyValue, TargetDataLinks targetDataLinks)
		{
			if (targetDataLinks != TargetDataLinks.NOT_LINKED_TO_DATA && targetDataLinks.recordLinked)
			{
				if (childComponent != null)
				{
					recordLinkedPropAdded(propertyValue);
				}
				else
				{
					initiallyAddedValuesWhileComponentIsNull.add(propertyValue);
				}
			}
		}

		protected void recordLinkedPropAdded(IDataLinkedPropertyValue propertyValue)
		{
			String propertyName = findComponentPropertyName(propertyValue);

			if (propertyName != null)
			{
				oldDataLinkedValuesToRootPropertyName.put(propertyValue, propertyName);

				if (!recordBasedProperties.contains(propertyName))
				{
					recordBasedProperties.add(propertyName);

					recordBasedPropertiesChanged = true;
					recordBasedPropertiesChangedComparedToTemplate = true;
					monitor.valueChanged();
				}
			}
		}

		@Override
		public void dataLinkedPropertyUnregistered(IDataLinkedPropertyValue propertyValue)
		{
			if (childComponent != null)
			{
				// when this gets called the component property value is probably already changed
				// as usually data linked property values unregister themselves in detach();
				// so we use this map to find the rootPropertyName if it's a value of this child component
				String propertyName = oldDataLinkedValuesToRootPropertyName.remove(propertyValue);

				if (propertyName != null && recordBasedProperties.remove(propertyName))
				{
					recordBasedPropertiesChanged = true;
					recordBasedPropertiesChangedComparedToTemplate = true;
					monitor.valueChanged();
				}
			}
			else
			{
				initiallyAddedValuesWhileComponentIsNull.remove(propertyValue);
			}
		}

		protected void componentIsNowAvailable()
		{
			for (IDataLinkedPropertyValue v : initiallyAddedValuesWhileComponentIsNull)
				recordLinkedPropAdded(v);
			initiallyAddedValuesWhileComponentIsNull = null;
		}

	}
}
