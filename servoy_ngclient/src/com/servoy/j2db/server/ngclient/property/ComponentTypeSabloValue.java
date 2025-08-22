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
import org.json.JSONString;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IDirtyPropertyListener;
import org.sablo.IWebObjectContext;
import org.sablo.IllegalChangeFromClientException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.TypedDataWithChangeInfo;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.ChangesToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.ngclient.ComponentContext;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.component.RuntimeWebComponent;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType.IModelWriter;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.II18NPropertyType;
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
 * This is meant to be used as a property in a parent component that will handle the showing of this as a child component. (For example table view (portal) or list form component components do this - use child components as properties...)
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ComponentTypeSabloValue implements ISmartPropertyValue
{

	private static final String TAG_MAIN = "main";

	public static final Logger log = LoggerFactory.getLogger(ComponentTypeSabloValue.class.getCanonicalName());

	public static final String TAG_ADD_TO_ELEMENTS_SCOPE = "addToElementsScope";

	public static final String NO_OP = "n";

	protected WebFormComponent childComponent;

	protected boolean componentIsCreated = false;

	protected String forFoundsetTypedPropertyName;
	protected PropertyChangeListener forFoundsetPropertyListener, readonlyPropertyListener;

	protected ViewportDataChangeMonitor<ComponentViewportRowDataProvider> viewPortChangeMonitor;

	protected ComponentDataLinkedPropertyListener dataLinkedPropertyRegistrationListener; // only used in case component is foundset-linked

	/**
	 * Initially equal to recordBasedProperties of the form element. It will always return a copy and keep the original intact if any
	 * methods that can mutate it's state are about to do that.
	 *
	 * This will be null if the component is not foundset-linked and will always be non-null (no matter if it has record based props or
	 * not in the component) if the component is foundset linked.
	 */
	private RecordBasedProperties recordBasedProperties;

	private IWebObjectContext webObjectContext;
	private IChangeListener foundsetStateChangeListener;
	protected IChangeListener monitor;
	protected PropertyDescription componentPropertyDescription;

	protected final ComponentTypeFormElementValue formElementValue;

	private FoundsetLinkedValueChangeHandler foundsetLinkedPropOfComponentValueChangeHandler;

	public ComponentTypeSabloValue(ComponentTypeFormElementValue formElementValue, PropertyDescription componentPropertyDescription,
		String forFoundsetTypedPropertyName)
	{
		this.formElementValue = formElementValue;
		this.forFoundsetTypedPropertyName = forFoundsetTypedPropertyName;
		this.componentPropertyDescription = componentPropertyDescription;
		initRecordBasedProperties();
	}

	private void initRecordBasedProperties()
	{
		this.recordBasedProperties = forFoundsetTypedPropertyName != null ? formElementValue.recordBasedProperties : null;
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
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectCtxt)
	{
		// the code below that clears component should not be needed as detach will do these and it should never happen that attach is called twice in a row without a detach in-between
		// but due to some code in ChangeAwareList.detach(int idx, WT el, boolean remove) that might skip the actual detach to improve operation of Rhino side .splice ... it can happen that the value gets attached twice
		if (componentIsCreated) detach();

		this.webObjectContext = webObjectCtxt;
		this.monitor = changeMonitor;

		if (forFoundsetTypedPropertyName != null)
		{
			this.webObjectContext.addPropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetPropertyListener = new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					if (evt.getNewValue() != null) createComponentIfNeededAndPossible();
				}
			});
		}
		createComponentIfNeededAndPossible();
	}

	private void setDataproviderNameToFoundset()
	{
		FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
		Collection<PropertyDescription> dp = childComponent.getSpecification().getProperties(DataproviderPropertyType.INSTANCE);
		if (dp.size() > 0)
		{
			// get the 'main' or first dataprovider property if no 'main' is set
			String dataprovider = null;
			for (PropertyDescription propertyDesc : dp)
			{
				Object propertyValue = childComponent.getProperty(propertyDesc.getName());
				if (propertyValue != null && (dataprovider == null || Boolean.TRUE.equals(propertyDesc.getTag(TAG_MAIN))))
				{
					dataprovider = ((DataproviderTypeSabloValue)propertyValue).getDataProviderID();
				}
			}
			if (dataprovider != null) foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(childComponent.getName(), dataprovider);
		}
	}

	@Override
	public void detach()
	{
		if (webObjectContext == null) return; // already detached or nothing to clean up.

		if (forFoundsetPropertyListener != null)
		{
			webObjectContext.removePropertyChangeListener(forFoundsetTypedPropertyName, forFoundsetPropertyListener);

			FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
			if (foundsetPropValue != null)
			{
				if (foundsetStateChangeListener != null)
				{
					foundsetPropValue.removeStateChangeListener(foundsetStateChangeListener);
					foundsetStateChangeListener = null;
				}
				if (viewPortChangeMonitor != null)
				{
					foundsetPropValue.removeViewportDataChangeMonitor(viewPortChangeMonitor);
					viewPortChangeMonitor = null;
				}
				if (dataLinkedPropertyRegistrationListener != null)
				{
					FoundsetDataAdapterList dal = foundsetPropValue.getDataAdapterList();
					if (dal != null) dal.removeDataLinkedPropertyRegistrationListener(dataLinkedPropertyRegistrationListener);
				}
				foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(childComponent.getName(), null);
			}
		}
		if (readonlyPropertyListener != null) webObjectContext.removePropertyChangeListener(WebFormUI.READONLY, readonlyPropertyListener);

		// unregister this component from formcontroller "elements" scope if needed
		WebFormComponent parentComponent = getParentComponent();
		IWebFormUI formUI = parentComponent != null ? parentComponent.findParent(IWebFormUI.class) : null;
		if (formUI != null && componentPropertyDescription != null && Utils.getAsBoolean(componentPropertyDescription.getTag(TAG_ADD_TO_ELEMENTS_SCOPE)))
		{
			formUI.removeComponentFromElementsScope(formElementValue.element, formElementValue.element.getWebComponentSpec(), childComponent);
		}

		if (childComponent != null)
		{
			childComponent.dispose();
			childComponent = null;
		}
		componentIsCreated = false;

		initRecordBasedProperties(); // reset to form element's record based properties or null

		this.monitor = null;
		this.webObjectContext = null;
	}

	private FoundsetTypeSabloValue getFoundsetValue()
	{
		if (webObjectContext != null)
		{
			if (forFoundsetTypedPropertyName != null)
			{
				return (FoundsetTypeSabloValue)webObjectContext.getProperty(forFoundsetTypedPropertyName);
			}
		}
		return null;
	}

	private WebFormComponent getParentComponent()
	{
		return webObjectContext != null ? (WebFormComponent)webObjectContext.getUnderlyingWebObject() : null;
	}

	protected void createComponentIfNeededAndPossible()
	{
		// this method should get called only after init() got called on all properties from this component (including this one)
		// so now we should be able to find a potentially linked foundset property value
		if (componentIsCreated || webObjectContext == null) return;

		final FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
		if (foundsetPropValue != null) foundsetPropValue.addStateChangeListener(getFoundsetStateChangeListener());

		if ((foundsetPropValue == null || foundsetPropValue.getDataAdapterList() == null) && forFoundsetTypedPropertyName != null) return; // foundset property value is not yet set or not yet attached to component

		componentIsCreated = true;
		IWebFormUI formUI = getParentComponent().findParent(IWebFormUI.class);
		final IDataAdapterList dal = (foundsetPropValue != null ? foundsetPropValue.getDataAdapterList() : formUI.getDataAdapterList());

		if (foundsetPropValue != null)
		{
			// do this before creating the component so that any attach() methods of it's properties that register data links get caught
			((FoundsetDataAdapterList)dal).addDataLinkedPropertyRegistrationListener(createDataLinkedPropertyRegistrationListener());

			foundsetLinkedPropOfComponentValueChangeHandler = new FoundsetLinkedValueChangeHandler(foundsetPropValue);
		}

		childComponent = ComponentFactory.createComponent(dal.getApplication(), dal, formElementValue.element, getParentComponent(),
			formUI.getController().getForm());

		if (foundsetPropValue != null)
		{
			dataLinkedPropertyRegistrationListener.componentIsNowAvailable();
		}

		childComponent.setDirtyPropertyListener(new IDirtyPropertyListener()
		{

			@Override
			public void propertyFlaggedAsDirty(String propertyName, boolean dirty, boolean granularUpdate)
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
							foundsetLinkedPropOfComponentValueChangeHandler.valueChangedInFSLinkedUnderlyingValue(propertyName, viewPortChangeMonitor);
						}
						else
						{
							// else this change was probably determined by the fact that we reuse components, changing the record in the DAL to get data for a specific row;
							// so we need to clear component changes for this property because we do not notify the parent here (we want to ignore the change) so
							// we shouldn't keep the property marked as dirty - thus blocking future property changes to generate a valueChanged on parent's monitor
							childComponent.clearChangedStatusForProperty(propertyName);
						}
					}
					else
					{
						// non-record related prop. changed...
						monitor.valueChanged();
					}
				}
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
		if (componentPropertyDescription != null && Utils.getAsBoolean(componentPropertyDescription.getTag(TAG_ADD_TO_ELEMENTS_SCOPE)))
		{
			formUI.contributeComponentToElementsScope(formElementValue.element, formElementValue.element.getWebComponentSpec(), childComponent);
		}
		for (String handler : childComponent.getFormElement().getHandlers())
		{
			Object value = childComponent.getFormElement().getPropertyValue(handler);
			if (value instanceof String && Utils.getAsUUID(value, false) != null)
			{
				IPersist function = formUI.getController().getApplication().getFlattenedSolution().searchPersist((String)value);
				Form form = formUI.getController().getForm();
				if (function == null)
				{
					Debug.warn("Script Method of value '" + value + "' not found trying just the form " + form);

					IPersist child = form.getChild(UUID.fromString((String)value));
					if (child != null)
					{
						Debug.warn("Script Method " + child + " on the form " + form + " with uuid " + child.getUUID());
						function = child;
					}
				}
				if (function != null)
				{
					childComponent.add(handler, function.getUUID().toString());
				}
				else
				{
					Debug.warn("Event handler for " + handler + " with value '" + value + "' not found (form " + form + ", form element " +
						childComponent.getFormElement().getName() + ")");
				}
			}
		}

		if (foundsetPropValue != null)
		{
			viewPortChangeMonitor = new ComponentTypeViewportDataChangeMonitor(monitor,
				new ComponentViewportRowDataProvider((FoundsetDataAdapterList)dal, childComponent, this), this);
			foundsetPropValue.addViewportDataChangeMonitor(viewPortChangeMonitor);
			setDataproviderNameToFoundset();
		}

		addPropagatingPropertyChangeListener(WebFormUI.READONLY, webObjectContext.getProperty(WebFormUI.READONLY));
		addPropagatingPropertyChangeListener("editable", webObjectContext.getProperty("editable"));

		if (childComponent.hasChanges()) monitor.valueChanged();
	}

	protected RecordBasedProperties getRecordBasedProperties()
	{
		return recordBasedProperties;
	}

	private IChangeListener getFoundsetStateChangeListener()
	{
		if (foundsetStateChangeListener == null) foundsetStateChangeListener = new IChangeListener()
		{
			@Override
			public void valueChanged()
			{
				createComponentIfNeededAndPossible();
			}
		};

		return foundsetStateChangeListener;
	}

	private void addPropagatingPropertyChangeListener(final String property, Object initialValue)
	{
		if (webObjectContext.getPropertyDescription(property) != null && childComponent.getPropertyDescription(property) != null)
		{
			PropertyDescription propertyDescChild = childComponent.getPropertyDescription(property);
			if (childComponent.getProperty(property) == null || !propertyDescChild.hasDefault() ||
				childComponent.getProperty(property).equals(propertyDescChild.getDefaultValue()))
			{
				setChildProperty(property, initialValue);
				this.webObjectContext.addPropertyChangeListener(property, readonlyPropertyListener = new PropertyChangeListener()
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
			if (tmp != null) nestedPropertyFound(propertyValueToFind, tmp.getLeft(), tmp.getRight());
		}

		return false;
	}

	/**
	 * Writes a diff update between the value it has in the template and the initial data requested after runtime components were created or during a page refresh.
	 * @param componentPropertyType
	 */
	public JSONWriter initialToJSON(JSONWriter destinationJSON, ComponentPropertyType componentPropertyType) throws JSONException
	{
		if (forFoundsetTypedPropertyName != null && recordBasedProperties.areRecordBasedPropertiesChangedComparedToTemplate())
			return fullToJSON(destinationJSON, componentPropertyType);

		destinationJSON.object();
		destinationJSON.key(ComponentPropertyType.PROPERTY_UPDATES_KEY);
		destinationJSON.object();

		// model content
		TypedData<Map<String, Object>> allProps = childComponent.getProperties();
		childComponent.clearChanges();
		removeRecordDependentProperties(allProps);

		destinationJSON.key(ComponentPropertyType.MODEL_KEY);
		destinationJSON.object();

		// send component model (when linked to foundset only props that are not record related)
		childComponent.writeProperties(InitialToJSONConverter.INSTANCE, null, destinationJSON, allProps);

		destinationJSON.endObject();

		// viewport content
		writeWholeViewportToJSON(destinationJSON);

		destinationJSON.endObject();
		destinationJSON.endObject();

		return destinationJSON;
	}

	public JSONWriter changesToJSON(JSONWriter destinationJSON, ComponentPropertyType componentPropertyType) throws JSONException
	{
		if (forFoundsetTypedPropertyName != null && recordBasedProperties.areRecordBasedPropertiesChanged())
		{
			// just send over the whole thing - viewport and model properties are not the same as they used to be
			return fullToJSON(destinationJSON, componentPropertyType);
		}

		TypedDataWithChangeInfo changes = childComponent.getAndClearChanges();
		removeRecordDependentProperties(changes);

		boolean modelChanged = (changes.content.size() > 0);
		boolean viewPortChanged = (forFoundsetTypedPropertyName != null &&
			(viewPortChangeMonitor.shouldSendWholeViewport() || viewPortChangeMonitor.hasViewportChanges()));

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

			// send component model (when linked to foundset only props that are not record related)
			childComponent.writeProperties(ChangesToJSONConverter.INSTANCE, FullValueToJSONConverter.INSTANCE, destinationJSON, changes);

			destinationJSON.endObject();
		}

		if (viewPortChanged)
		{
			// something in the viewport containing per-record component property values changed - send updates
			if (viewPortChangeMonitor.shouldSendWholeViewport())
			{
				viewPortChangeMonitor.clearChanges();
				writeWholeViewportToJSON(destinationJSON);
			}
			else
			// viewPortChanges.size() > 0
			{
				ArrayOperation[] viewPortChanges = viewPortChangeMonitor.getViewPortChanges();
				viewPortChangeMonitor.clearChanges();

				destinationJSON.key(ComponentPropertyType.MODEL_VIEWPORT_CHANGES_KEY).array();

				FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
				for (ArrayOperation viewPortChange : viewPortChanges)
				{
					FoundsetPropertyType.writeViewportOperationToJSON(viewPortChange, viewPortChangeMonitor.getRowDataProvider(),
						foundsetPropValue.getFoundset(),
						foundsetPropValue.getViewPort().getStartIndex(), destinationJSON, null, null);
				}
				destinationJSON.endArray();
			}
			viewPortChangeMonitor.doneWritingChanges();
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

			viewPortChangeMonitor.clearChanges();

			destinationJSON.key(ComponentPropertyType.MODEL_VIEWPORT_KEY);

			// types implementing IPropertyConverterForBrowserWithDynamicClientType that wrote a dynamic type will be returned by following call;
			// static IPropertyWithClientSideConversions values are just written without including the type; this is because client already knows the IPropertyWithClientSideConversions client-side (sent via ClientSideTypesState)
			ViewportClientSideTypes dynamicClientSideTypes = viewPortChangeMonitor.getRowDataProvider().writeRowData(foundsetPropertyViewPort.getStartIndex(),
				foundsetPropertyViewPort.getStartIndex() + foundsetPropertyViewPort.getSize() - 1, getFoundsetValue().getFoundset(), destinationJSON);

			// conversion info for websocket traffic (for example Date objects will turn into long or String to be usable in JSON and client-side needs to know about this)
			if (dynamicClientSideTypes != null) dynamicClientSideTypes.writeClientSideTypes(destinationJSON, JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY);

			viewPortChangeMonitor.doneWritingChanges();
		}
	}

	/**
	 * Writes the entire value of this property as JSON. This includes the template values, not just the runtime component properties.
	 * This is currently needed and can get called if the property is nested inside other complex properties (json object/array) that sometimes
	 * might want/need to send again the entire content.
	 */
	public JSONWriter fullToJSON(final JSONWriter writer, ComponentPropertyType componentPropertyType) throws JSONException
	{
		// create children of component as specified by this property
		final FormElement fe = formElementValue.element;

		writer.object();

		// get template model values
		final TypedData<Map<String, Object>> formElementProperties = fe.propertiesForTemplateJSON();

		// we'll need to update them with runtime values
		final TypedData<Map<String, Object>> runtimeProperties = childComponent.getProperties();
		childComponent.clearChanges();

		// add to useful properties only those formElement properties that didn't get overridden at runtime (so form element value is still used)
		boolean templateValuesRemoved = false;
		for (Entry<String, Object> fePropEntry : formElementProperties.content.entrySet())
		{
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

		IWebFormUI parent = childComponent.findParent(IWebFormUI.class);
		final FormElementContext formElementContext = new FormElementContext(fe, new ServoyDataConverterContext(parent.getController()), null);
		componentPropertyType.writeTemplateJSONContent(writer, formElementValue, forFoundsetTypedPropertyName, formElementContext, new IModelWriter()
		{

			@Override
			public void writeComponentModel() throws JSONException
			{
				writer.object();
				JSONUtils.writeData(FormElementToJSON.INSTANCE, writer, formElementProperties.content, formElementProperties.contentType, formElementContext);
				// always use full to JSON converter here; second arg. is null due to that
				childComponent.writeProperties(JSONUtils.FullValueToJSONConverter.INSTANCE, null, writer, runtimeProperties);
				writer.endObject();
			}

		}, recordBasedProperties, false);
		if (forFoundsetTypedPropertyName != null) recordBasedProperties.clearChanged();

		writeWholeViewportToJSON(writer);

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
			recordBasedProperties.forEach((propertyName) -> {
				try
				{
					changes.content.remove(propertyName);
				}
				catch (UnsupportedOperationException e)
				{
					changes.content = new HashMap<String, Object>(changes.content);
					changes.content.remove(propertyName);
				}
				if (changes.contentType != null)
				{
					Map<String, PropertyDescription> properties = new HashMap(changes.contentType.getProperties());
					properties.remove(propertyName);
					changes.contentType = new PropertyDescriptionBuilder().withName(changes.contentType.getName()).withType(
						changes.contentType.getType()).withConfig(changes.contentType.getConfig()).withProperties(properties).withDefaultValue(
							changes.contentType.getDefaultValue())
						.withInitialValue(changes.contentType.getInitialValue()).withHasDefault(
							changes.contentType.hasDefault())
						.withValues(changes.contentType.getValues()).withPushToServer(
							changes.contentType.getPushToServer())
						.withOptional(changes.contentType.isOptional()).withDeprecated(
							changes.contentType.getDeprecated())
						.build();
				}
			});
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
							String rowId = update.optString("rowId", null);
							if (rowId != null)
							{
								FoundsetTypeSabloValue foundsetValue = getFoundsetValue();
								if (foundsetValue != null)
								{
									if (!foundsetValue.setEditingRowByPkHash(rowId))
									{
										Debug.error("Cannot select row when component event was fired; row identifier: " + rowId + ", forFoundset: '" +
											foundsetValue + "', component: " + (childComponent != null ? childComponent : getName()));
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

							JSONString result = null;
							String error = null;
							try
							{
								result = childComponent.executeEvent(eventType, args); // FIXME here we know it's comming from client json/sablo/java and returning to client json; see SVY-18096
							}
							catch (ParseException pe)
							{
								log.warn("Warning: " + pe.getMessage(), pe);
							}
							catch (IllegalChangeFromClientException ilcae)
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
									CurrentWindow.get().getSession().getSabloService().resolveDeferedEvent(cmsid, true, result, null);
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

						if (!viewPortChangeMonitor.hasViewportChanges() && !viewPortChangeMonitor.shouldSendWholeViewport())
							foundsetPropertyValue.setDataAdapterListToSelectedRecord();
						// else the selected record will be restored later in toJSON via viewPortChangeMonitor.doneWritingChanges() and
						// in some cases - for example if the viewport has updates due to a valuelist.filter() that just happened on another
						// record then the selected one in the updatePropertyValueForRecord() above, we must not restore selection here, as that
						// changesToJSON that will follow will want to send the result of that filter and not a full valuelist value that might
						// result due to a restore of selected record in the FoundsetDataAdapterList followed by a switch to the
						// record that .filter() was called on when writing changes toJSON...
					}
					else
					{
						Debug.error("Component updates received for record linked property, but component is not linked to a foundset: " +
							update.get(ViewportDataChangeMonitor.VIEWPORT_CHANGED));
					}
				}
				else if (update.has("svyApply"))
				{
					// svyApply: {
					//     _svyRowId?: string;
					//     _svyRowIdOfProp?: string;
					//     pn: string;
					//     v: any;
					// }
					JSONObject changeAndApply = update.getJSONObject("svyApply");

					String propertyName = changeAndApply.getString(ComponentPropertyType.PROPERTY_NAME_KEY);
					Object value = changeAndApply.get(ComponentPropertyType.VALUE_KEY);
					String rowIDOfPropInsideComponent = changeAndApply.optString(ComponentPropertyType.ROW_ID_OF_PROP_INSIDE_COMPONENT, null);

					try
					{
						if (forFoundsetTypedPropertyName != null && recordBasedProperties.contains(propertyName))
						{
							// changes component record and sets value
							String rowIDValue = changeAndApply.getString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
							if (foundsetLinkedPropOfComponentValueChangeHandler != null)
								foundsetLinkedPropOfComponentValueChangeHandler.setApplyingDPValueFromClient(true);
							FoundsetTypeSabloValue foundsetValue = getFoundsetValue();
							updatePropertyValueForRecord(foundsetValue, rowIDValue, propertyName, value);

							// apply change to record/dp
							foundsetValue.getDataAdapterList().pushChanges(childComponent, propertyName, rowIDOfPropInsideComponent);

							foundsetValue.setDataAdapterListToSelectedRecord();
						}
						else
						{
							childComponent.putBrowserProperty(propertyName, value);
							IWebFormUI formUI = getParentComponent().findParent(IWebFormUI.class);

							// apply change to record/dp
							formUI.getDataAdapterList().pushChanges(childComponent, propertyName, rowIDOfPropInsideComponent);
						}


						if (forFoundsetTypedPropertyName != null && !recordBasedProperties.contains(propertyName))
						{
							// a global or form var that in case of a foundset linked component will apply the value on the child component but, as it knows it is comming from the browser,
							// the child component will not notify it as a changed value; we need that though as we need to resend that value for all rows back to client, not just currently selected one
							childComponent.markPropertyAsChangedByRef(propertyName);
						}
					}
					finally
					{
						if (foundsetLinkedPropOfComponentValueChangeHandler != null)
							foundsetLinkedPropOfComponentValueChangeHandler.setApplyingDPValueFromClient(false);
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
						FoundsetTypeSabloValue foundsetTypeSabloValue = getFoundsetValue();
						IFoundSetInternal foundset = foundsetTypeSabloValue.getFoundset();
						dal = foundsetTypeSabloValue.getDataAdapterList();

						if (foundset != null)
						{
							int recordIndex = foundset.getRecordIndex(rowIDValue, foundsetTypeSabloValue.getRecordIndexHint());

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
						IWebFormUI formUI = getParentComponent().findParent(IWebFormUI.class);
						dal = formUI.getDataAdapterList();
					}

					// the following line will also change the foundset selection to DAL's record
					dal.startEdit(childComponent, propertyName, null); // TODO last arg should be here the foundsetLinked row Id in case the property is itself a foundset-linked DP; this should be done as part of case SVY-10500
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

		if (foundset != null)
		{
			int recordIndex = foundset.getRecordIndex(rowIDValue, foundsetPropertyValue.getRecordIndexHint());

			if (recordIndex != -1)
			{
				// ok we found the record; check that it is still available in the viewport of this foundset linked component (as it might have been excluded from viewport prior to this)
				FoundsetTypeViewport vp = foundsetPropertyValue.getViewPort();
				if (vp.getStartIndex() <= recordIndex && recordIndex < (vp.getStartIndex() + vp.getSize()))
				{
					foundsetPropertyValue.getDataAdapterList().setRecordQuietly(foundset.getRecord(recordIndex));

					try
					{
						childComponent.putBrowserProperty(propertyName, value);
					}
					catch (JSONException e)
					{
						Debug.error(
							"Setting value for record dependent property '" + propertyName + "' in foundset linked component to value: " + value + " failed.",
							e);
					}
				}
				else
				{
					// normally when this happens it is a strange sequence of requests from the client side component (updates are sent for components that are no longer inside the used viewport)
					// for example a list form component that uses a form component that contains an extra table (related foundset)
					// had a bad behavior that, after it received 50 records initially, it would only need 9 and would request loadLessRecords -41/41 twice on the main foundset (=> 0 size viewport server side)
					// but at the same time the extra-table that corresponds to the first record had many related rows and it also sent update from browser to loadLessRecords then initially...
					// so the list form components viewport was size 0 on server already but an update came for a child component - a record that does exist, but is no longer in viewport;
					// and that resulted in a valid loadLess on the extra table but which was marked as handled correctly (handleID) but it was never sent back to client as it no longer existed on client
					// and when list form component corrected it's viewport again to be 0-9, the nested foundset property type of the extra table still had that "handledID" set
					// so basically it sent a handledID to client for a previous value "undefined" (as viewport was 0) => exception on client
					//
					// I think we can safely ignore these type of requests - as they are for obsolete records
					Debug.debug("Cannot set foundset linked record dependent component property for (" + rowIDValue + ") property '" + propertyName +
						"' to value '" + value + "' of component: " + childComponent + ". Record found, but out of current viewport.");
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

	/**
	 * @return the childComponent
	 */
	public WebFormComponent getChildComponent()
	{
		return childComponent;
	}

	protected final class ComponentDataLinkedPropertyListener implements IDataLinkedPropertyRegistrationListener
	{
		private final Map<IDataLinkedPropertyValue, String> oldDataLinkedValuesToRootPropertyName = new HashMap<IDataLinkedPropertyValue, String>(3);
		private final List<Pair<IDataLinkedPropertyValue, String[]>> initiallyAddedValuesWhileComponentIsNull = new ArrayList<>(3);

		@Override
		public void dataLinkedPropertyRegistered(IDataLinkedPropertyValue propertyValue, TargetDataLinks targetDataLinks)
		{
			if (targetDataLinks != TargetDataLinks.NOT_LINKED_TO_DATA && targetDataLinks.recordLinked)
			{
				if (childComponent != null)
				{
					recordLinkedPropAdded(propertyValue, targetDataLinks.dataProviderIDs);
				}
				else
				{
					initiallyAddedValuesWhileComponentIsNull.add(new Pair<>(propertyValue, targetDataLinks.dataProviderIDs));
				}
			}
		}

		protected void recordLinkedPropAdded(IDataLinkedPropertyValue propertyValue, String[] dataProviderIDs)
		{
			String propertyName = findComponentPropertyName(propertyValue);

			if (propertyName != null)
			{
				oldDataLinkedValuesToRootPropertyName.put(propertyValue, propertyName);
				recordBasedProperties = recordBasedProperties.addRecordBasedProperty(propertyName, dataProviderIDs, monitor);
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

				if (propertyName != null)
				{
					recordBasedProperties = recordBasedProperties.removeRecordBasedProperty(propertyName, monitor);
				}
			}
			else
			{
				initiallyAddedValuesWhileComponentIsNull.removeIf(item -> (item.getLeft() == propertyValue));
			}
		}

		protected void componentIsNowAvailable()
		{
			for (Pair<IDataLinkedPropertyValue, String[]> e : initiallyAddedValuesWhileComponentIsNull)
				recordLinkedPropAdded(e.getLeft(), e.getRight());
			initiallyAddedValuesWhileComponentIsNull.clear();
		}

	}

	@Override
	public String toString()
	{
		return "Child component value: " + (childComponent != null ? childComponent : "not yet attached");
	}


	public void resetI18nValue()
	{
		for (Entry<String, PropertyDescription> p : childComponent.getSpecification().getProperties().entrySet())
		{
			if (p.getValue().getType() instanceof II18NPropertyType)
			{
				childComponent.setProperty(p.getKey(),
					((II18NPropertyType)p.getValue().getType()).resetI18nValue(childComponent.getProperty(p.getKey()), p.getValue(), childComponent));
			}
		}
	}

}
