/*

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
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.ChangesToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeChangeMonitor.RowData;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

/**
 * Property value for {@link FoundsetLinkedPropertyType}.
 *
 * @author acostescu
 */
public class FoundsetLinkedTypeSabloValue<YF, YT> implements IDataLinkedPropertyValue
{

	/**
	 * Non-record linked property change received from client...
	 */
	protected static final String PROPERTY_CHANGE = "propertyChange";

	protected static final String PUSH_TO_SERVER = "w";

	/**
	 * When non-null then the wrapped property is not yet initialized - waiting for forFoundset property's DAL to be available
	 */
	protected InitializingState initializingState;

	protected YT wrappedSabloValue;
	protected WebFormComponent component;
	protected IBrowserConverterContext browserConverterContext;
	protected final String forFoundsetPropertyName;
	protected PropertyChangeListener forFoundsetPropertyListener;
	protected IDataLinkedPropertyRegistrationListener dataLinkedPropertyRegistrationListener;
	protected IChangeListener changeMonitor;

	protected ViewportDataChangeMonitor viewPortChangeMonitor;

	protected class InitializingState
	{

		protected final YF formElementValue;
		protected final INGFormElement formElement;
		protected final PropertyDescription wrappedPropertyDescription;

		public InitializingState(PropertyDescription wrappedPropertyDescription, YF formElementValue, INGFormElement formElement)
		{
			this.wrappedPropertyDescription = wrappedPropertyDescription;
			this.formElementValue = formElementValue;
			this.formElement = formElement;
		}

	}

	/**
	 * Called when we already know the wrapped value (probably default value...)
	 */
	public FoundsetLinkedTypeSabloValue(YT wrappedSabloValue, String forFoundsetPropertyName)
	{
		this.wrappedSabloValue = wrappedSabloValue;
		this.forFoundsetPropertyName = forFoundsetPropertyName;
		// initializingState = null; // it is null by default, just mentioning it
	}

	public FoundsetLinkedTypeSabloValue(String forFoundsetPropertyName, YF formElementValue, PropertyDescription wrappedPropertyDescription,
		INGFormElement formElement, WebFormComponent component)
	{
		initializingState = new InitializingState(wrappedPropertyDescription, formElementValue, formElement);
		this.component = component;
		browserConverterContext = new BrowserConverterContext(component, PushToServerEnum.allow);
		this.forFoundsetPropertyName = forFoundsetPropertyName;
		// this.wrappedSabloValue = null; // for now; waiting for foundset property availability
	}

	private FoundsetTypeSabloValue getFoundsetValue()
	{
		if (component != null)
		{
			return (FoundsetTypeSabloValue)component.getProperty(forFoundsetPropertyName);
		}
		return null;
	}

	protected void createWrappedSabloValueNeededAndPossible()
	{
		if (initializingState == null) return;

		final FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();

		if (foundsetPropValue == null) return;

		FoundsetDataAdapterList dal = foundsetPropValue.getDataAdapterList();
		dal.addDataLinkedPropertyRegistrationListener(getDataLinkedPropertyRegistrationListener(changeMonitor, foundsetPropValue));

		this.wrappedSabloValue = (YT)NGConversions.INSTANCE.convertFormElementToSabloComponentValue(initializingState.formElementValue,
			initializingState.wrappedPropertyDescription, initializingState.formElement, component, dal); // this conversion also adds dal data listeners when needed and will trigger dataLinkedPropertyRegistrationListener above which updates the record-linked or not state
		if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(changeMonitor, component);

		initializingState = null;
	}

	@Override
	public void attachToBaseObject(final IChangeListener monitor, @SuppressWarnings("hiding") BaseWebObject component)
	{
		this.component = (WebFormComponent)component;
		browserConverterContext = new BrowserConverterContext(component, PushToServerEnum.allow);
		this.changeMonitor = monitor;

		createWrappedSabloValueNeededAndPossible();
		this.component.addPropertyChangeListener(forFoundsetPropertyName, forFoundsetPropertyListener = new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getNewValue() != null) createWrappedSabloValueNeededAndPossible();
			}
		});
	}

	@Override
	public void detach()
	{
		if (forFoundsetPropertyListener != null)
		{
			component.removePropertyChangeListener(forFoundsetPropertyName, forFoundsetPropertyListener);

			FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();
			if (foundsetPropValue != null && viewPortChangeMonitor != null)
			{
				foundsetPropValue.removeViewportDataChangeMonitor(viewPortChangeMonitor);
			}
		}
		if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
	}

	protected IDataLinkedPropertyRegistrationListener getDataLinkedPropertyRegistrationListener(final IChangeListener changeMonitor,
		final FoundsetTypeSabloValue foundsetPropValue)
	{
		final PropertyDescription pd = initializingState.wrappedPropertyDescription;
		return dataLinkedPropertyRegistrationListener = new IDataLinkedPropertyRegistrationListener()
		{

			@Override
			public void dataLinkedPropertyRegistered(IDataLinkedPropertyValue propertyValue, TargetDataLinks targetDataLinks)
			{
				if (wrappedSabloValue == propertyValue && targetDataLinks != TargetDataLinks.NOT_LINKED_TO_DATA && targetDataLinks.recordLinked)
				{
					// wrapped property is record linked so we need to send viewports to client
					// this could be the result of initialization or it could for example get changed from Rhino
					boolean changed = (viewPortChangeMonitor == null);

					viewPortChangeMonitor = new ViewportDataChangeMonitor(changeMonitor, new FoundsetLinkedViewportRowDataProvider<YF, YT>(
						foundsetPropValue.getDataAdapterList(), pd, FoundsetLinkedTypeSabloValue.this));
					foundsetPropValue.addViewportDataChangeMonitor(viewPortChangeMonitor);

					if (changed) changeMonitor.valueChanged();
				} // else we will send single value to client as it is not record dependent and the client can just duplicate that to match foundset viewport size
			}

			@Override
			public void dataLinkedPropertyUnregistered(IDataLinkedPropertyValue propertyValue)
			{
				if (wrappedSabloValue == propertyValue && viewPortChangeMonitor != null)
				{
					// wrapped property is now no longer record linked so we only send one value to be duplicated
					// this could be the result of initialization or it could for example get changed from Rhino
					getFoundsetValue().removeViewportDataChangeMonitor(viewPortChangeMonitor);
					viewPortChangeMonitor = null;
					changeMonitor.valueChanged();
				}
			}

		};
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).dataProviderOrRecordChanged(record,
			dataProvider, isFormDP, isGlobalDP, fireChangeEvent);
	}

	protected YT getWrappedValue()
	{
		return wrappedSabloValue;
	}

	protected IBrowserConverterContext getBrowserConverterContextForToJSON()
	{
		return browserConverterContext;
	}

	public void rhinoToSablo(Object rhinoValue, PropertyDescription wrappedPropertyDescription, BaseWebObject componentOrService)
	{
		YT newWrappedVal;
		newWrappedVal = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(rhinoValue, wrappedSabloValue, wrappedPropertyDescription, componentOrService);

		if (newWrappedVal != wrappedSabloValue)
		{
			// do what component would do when a property changed
			// TODO should we make current method return a completely new instance instead and leave component code do the rest?
			if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
			wrappedSabloValue = newWrappedVal;
			if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(changeMonitor,
				componentOrService);
		}
	}

	public JSONWriter fullToJSON(JSONWriter writer, String key, DataConversion clientConversion, PropertyDescription wrappedPropertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (initializingState != null)
		{
			Debug.warn("Trying to get full value from an uninitialized foundset linked property: " + wrappedPropertyDescription);
			return writer;
		}

		clientConversion.convert(FoundsetLinkedPropertyType.CONVERSION_NAME);
		JSONUtils.addKeyIfPresent(writer, key);

		writer.object();
		writer.key(FoundsetLinkedPropertyType.FOR_FOUNDSET_PROPERTY_NAME).value(forFoundsetPropertyName);

		PushToServerEnum pushToServer = dataConverterContext.getParentPropertyPushToServerValue();
		if (pushToServer == PushToServerEnum.shallow || pushToServer == PushToServerEnum.deep)
		{
			writer.key(PUSH_TO_SERVER).value(pushToServer == PushToServerEnum.shallow ? false : true);
		}

		if (viewPortChangeMonitor == null)
		{
			// single value; not record dependent
			DataConversion dataConversions = new DataConversion();
			dataConversions.pushNode(FoundsetLinkedPropertyType.SINGLE_VALUE);
			FullValueToJSONConverter.INSTANCE.toJSONValue(writer, FoundsetLinkedPropertyType.SINGLE_VALUE, wrappedSabloValue, wrappedPropertyDescription,
				dataConversions, dataConverterContext);
			JSONUtils.writeClientConversions(writer, dataConversions);
		}
		else
		{
			// record dependent; viewport value
			writeWholeViewportToJSON(writer);
			viewPortChangeMonitor.clearChanges();
		}
		writer.endObject();
		return writer;
	}

	protected void writeWholeViewportToJSON(JSONWriter destinationJSON) throws JSONException
	{
		FoundsetTypeViewport foundsetPropertyViewPort = getFoundsetValue().getViewPort();

		DataConversion clientConversionInfo = new DataConversion();

		destinationJSON.key(FoundsetLinkedPropertyType.VIEWPORT_VALUE);
		clientConversionInfo.pushNode(FoundsetLinkedPropertyType.VIEWPORT_VALUE);
		viewPortChangeMonitor.getRowDataProvider().writeRowData(foundsetPropertyViewPort.getStartIndex(),
			foundsetPropertyViewPort.getStartIndex() + foundsetPropertyViewPort.getSize() - 1, getFoundsetValue().getFoundset(), destinationJSON,
			clientConversionInfo);
		clientConversionInfo.popNode();

		// conversion info for websocket traffic (for example Date objects will turn into long)
		JSONUtils.writeClientConversions(destinationJSON, clientConversionInfo);
	}

	public JSONWriter changesToJSON(JSONWriter writer, String key, DataConversion clientConversion, PropertyDescription wrappedPropertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (initializingState != null)
		{
			Debug.warn("Trying to get changes from an uninitialized foundset linked property: " + wrappedPropertyDescription);
			return writer;
		}

		clientConversion.convert(FoundsetLinkedPropertyType.CONVERSION_NAME);
		JSONUtils.addKeyIfPresent(writer, key);

		writer.object();
		if (viewPortChangeMonitor == null)
		{
			// single value; just send it's changes
			DataConversion dataConversions = new DataConversion();
			dataConversions.pushNode(FoundsetLinkedPropertyType.SINGLE_VALUE_UPDATE);
			ChangesToJSONConverter.INSTANCE.toJSONValue(writer, FoundsetLinkedPropertyType.SINGLE_VALUE_UPDATE, wrappedSabloValue, wrappedPropertyDescription,
				dataConversions, dataConverterContext);
			JSONUtils.writeClientConversions(writer, dataConversions);
		}
		else
		{
			if (viewPortChangeMonitor.shouldSendWholeViewport())
			{
				writeWholeViewportToJSON(writer);
			}
			else if (viewPortChangeMonitor.getViewPortChanges().size() > 0)
			{
				DataConversion clientConversionInfo = new DataConversion();
				writer.key(FoundsetLinkedPropertyType.VIEWPORT_VALUE_UPDATE);
				clientConversionInfo.pushNode(FoundsetLinkedPropertyType.VIEWPORT_VALUE_UPDATE);


				List<RowData> viewPortChanges = viewPortChangeMonitor.getViewPortChanges();
				Map<String, Object>[] changesArray = new Map[viewPortChanges.size()];

				writer.array();
				for (int i = 0; i < viewPortChanges.size(); i++)
				{
					clientConversionInfo.pushNode(String.valueOf(i));
					viewPortChanges.get(i).writeJSONContent(writer, null, FullValueToJSONConverter.INSTANCE, clientConversionInfo);
					clientConversionInfo.popNode();
				}
				writer.endArray();
				clientConversionInfo.popNode();

				// conversion info for websocket traffic (for example Date objects will turn into long)
				JSONUtils.writeClientConversions(writer, clientConversionInfo);
			} // else there is no change to send!

			viewPortChangeMonitor.clearChanges();
		}
		writer.endObject();

		return writer;
	}

	public void browserUpdatesReceived(Object newJSONValue, PropertyDescription wrappedPropertyDescription, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext)
	{
		PushToServerEnum pushToServer = dataConverterContext.getParentPropertyPushToServerValue();

		if (initializingState != null)
		{
			Debug.error("Trying to update state for an uninitialized foundset linked property: " + wrappedPropertyDescription + " | " + component);
			return;
		}

		if ((wrappedPropertyDescription instanceof IPushToServerSpecialType && ((IPushToServerSpecialType)wrappedPropertyDescription).shouldAlwaysAllowIncommingJSON()) ||
			PushToServerEnum.allow.compareTo(pushToServer) <= 0)
		{
			try
			{
				JSONArray updates = (JSONArray)newJSONValue;
				for (int i = 0; i < updates.length(); i++)
				{
					JSONObject update = (JSONObject)updates.get(i);
					if (update.has(PROPERTY_CHANGE))
					{
						// for when property is not record dependent
						// { propertyChange : propValue }

						if (viewPortChangeMonitor != null)
						{
							Debug.error("Trying to update single state value for a foundset linked record dependent property: " + wrappedPropertyDescription +
								" | " + component);
							return;
						}

						Object object = update.get(PROPERTY_CHANGE);
						YT newWrappedValue = (YT)JSONUtils.fromJSONUnwrapped(wrappedSabloValue, object, wrappedPropertyDescription, dataConverterContext);

						if (newWrappedValue != wrappedSabloValue)
						{
							// do what component would do when a property changed
							// TODO should we make current method return a completely new instance instead and leave component code do the rest?
							if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
							wrappedSabloValue = newWrappedValue;
							if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(
								changeMonitor, component);
						}
					}
					else if (update.has(ViewportDataChangeMonitor.VIEWPORT_CHANGED))
					{
						if (viewPortChangeMonitor == null)
						{
							Debug.error("Trying to update some record value for a foundset linked non-record dependent property: " +
								wrappedPropertyDescription + " | " + component);
							return;
						}

						// property is linked to a foundset and the value of a property that depends on the record changed client side;
						// in this case update DataAdapterList with the correct record and then set the value on the wrapped property
						FoundsetTypeSabloValue foundsetPropertyValue = getFoundsetValue();
						if (foundsetPropertyValue != null && foundsetPropertyValue.getFoundset() != null)
						{
							JSONObject change = update.getJSONObject(ViewportDataChangeMonitor.VIEWPORT_CHANGED);

							String rowIDValue = change.getString(FoundsetTypeSabloValue.ROW_ID_COL_KEY);
							Object value = change.get(FoundsetTypeSabloValue.VALUE_KEY);

							updatePropertyValueForRecord(foundsetPropertyValue, rowIDValue, value, wrappedPropertyDescription, dataConverterContext);
						}
						else
						{
							Debug.error("Component updates received for record linked property, but component is not linked to a foundset: " +
								update.get(ViewportDataChangeMonitor.VIEWPORT_CHANGED));
						}
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Debug.error("Foundset linked property (" + pd +
				") that doesn't define a suitable pushToServer value (allow/shallow/deep) tried to update proxied value(s) serverside. Denying and sending back server value!");
			if (viewPortChangeMonitor != null) viewPortChangeMonitor.shouldSendWholeViewport();
			else changeMonitor.valueChanged();
		}
	}

	protected void updatePropertyValueForRecord(FoundsetTypeSabloValue foundsetPropertyValue, String rowIDValue, Object value,
		PropertyDescription wrappedPropertyDescription, IBrowserConverterContext dataConverterContext)
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
					YT newWrappedValue = (YT)JSONUtils.fromJSONUnwrapped(wrappedSabloValue, value, wrappedPropertyDescription, dataConverterContext);

					if (newWrappedValue != wrappedSabloValue)
					{
						// do what component would do when a property changed
						// TODO should we make current method return a completely new instance instead and leave component code do the rest?
						if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
						wrappedSabloValue = newWrappedValue;
						if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(
							changeMonitor, component);

						// the full value has changed; the whole viewport might be affected
						viewPortChangeMonitor.viewPortCompletelyChanged();
					}
				}
				catch (JSONException e)
				{
					Debug.error("Setting value for record dependent property '" + wrappedPropertyDescription + "' in foundset linked component to value: " +
						value + " failed.", e);
				}
				finally
				{
					viewPortChangeMonitor.resumeRowUpdateListener();
				}
			}
			else
			{
				Debug.error("Cannot set foundset linked record dependent property for (" + rowIDValue + ") property '" + wrappedPropertyDescription +
					"' to value '" + value + "' of component: " + component + ". Record not found.", new RuntimeException());
			}
		}
		else
		{
			Debug.error("Cannot set foundset linked record dependent property for (" + rowIDValue + ") property '" + wrappedPropertyDescription +
				"' to value '" + value + "' of component: " + component + ". Foundset is null.", new RuntimeException());
		}
	}

}
