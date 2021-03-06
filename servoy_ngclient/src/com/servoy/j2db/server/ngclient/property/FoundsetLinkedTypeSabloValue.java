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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.ChangesToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Property value for {@link FoundsetLinkedPropertyType}.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FoundsetLinkedTypeSabloValue<YF, YT> implements IDataLinkedPropertyValue, IHasUnderlyingState
{

	/**
	 * Non-record linked property change received from client...
	 */
	protected static final String PROPERTY_CHANGE = "propertyChange";

	protected static final String PUSH_TO_SERVER = "w";

	protected static final String ID_FOR_FOUNDSET = "idForFoundset";

	/**
	 * When non-null then the wrapped property is not yet initialized - waiting for forFoundset property's DAL to be available
	 */
	protected InitializingState<YT> initializingState;
	protected boolean wrappedValueInitialized;

	protected YT wrappedSabloValue;
	protected NGComponentDALContext wrappedComponentContext;

	protected IWebObjectContext webObjectContext;
	protected final String forFoundsetPropertyName;
	protected final PropertyDescription wrappedPropertyDescription;
	protected PropertyChangeListener forFoundsetPropertyListener;
	protected IChangeListener foundsetStateChangeListener;
	protected IDataLinkedPropertyRegistrationListener dataLinkedPropertyRegistrationListener;
	protected IChangeListener changeMonitor;
	protected String idForFoundset;

	private FoundsetLinkedValueChangeHandler actualWrappedValueChangeHandlerForFoundsetLinked;
	private IChangeListener wrappedUnderlyingStateListener;

	protected Set<String> foundsetLinkedDPs = new HashSet<>();

	protected boolean idForFoundsetChanged = false;

	protected ViewportDataChangeMonitor<FoundsetLinkedViewportRowDataProvider<YF, YT>> viewPortChangeMonitor;

	protected List<IChangeListener> underlyingValueChangeListeners = new ArrayList<>();

	/**
	 * Called when we already know the wrapped value (probably default value...)
	 */
	public FoundsetLinkedTypeSabloValue(YT wrappedSabloValue, String forFoundsetPropertyName, PropertyDescription wrappedPropertyDescription)
	{
		this(forFoundsetPropertyName, wrappedPropertyDescription, true);
		setWrappedSabloValue(wrappedSabloValue);
		// initializingState = null; // it is null by default, just mentioning it
	}

	public FoundsetLinkedTypeSabloValue(String forFoundsetPropertyName, YF formElementValue, PropertyDescription wrappedPropertyDescription,
		INGFormElement formElement)
	{
		this(forFoundsetPropertyName, wrappedPropertyDescription, false);
		initializingState = new FormElementInitializingState(formElementValue, formElement);
	}

	public FoundsetLinkedTypeSabloValue(String forFoundsetPropertyName, Object rhinoValue, PropertyDescription wrappedPropertyDescription)
	{
		this(forFoundsetPropertyName, wrappedPropertyDescription, false);
		initializingState = new RhinoInitializingState(rhinoValue);
	}

	private FoundsetLinkedTypeSabloValue(String forFoundsetPropertyName, PropertyDescription wrappedPropertyDescription, boolean wrappedValueInitialized)
	{
		this.forFoundsetPropertyName = forFoundsetPropertyName;
		this.wrappedPropertyDescription = wrappedPropertyDescription;

		this.wrappedValueInitialized = wrappedValueInitialized;
	}

	private interface InitializingState<YT>
	{
		public YT initialize();
	}

	private class FormElementInitializingState implements InitializingState<YT>
	{

		protected final YF formElementValue;
		protected final INGFormElement formElement;

		public FormElementInitializingState(YF formElementValue, INGFormElement formElement)
		{
			this.formElementValue = formElementValue;
			this.formElement = formElement;
		}

		public YT initialize()
		{
			return (YT)NGConversions.INSTANCE.convertFormElementToSabloComponentValue(formElementValue, wrappedPropertyDescription, formElement,
				(WebFormComponent)webObjectContext.getUnderlyingWebObject(), (DataAdapterList)wrappedComponentContext.getDataAdapterList());
		}

	}

	private class RhinoInitializingState implements InitializingState<YT>
	{

		private final Object rhinoValue;

		public RhinoInitializingState(Object rhinoValue)
		{
			this.rhinoValue = rhinoValue;
		}

		public YT initialize()
		{
			// convert rhino to sablo using wrapped type - but give this conversion the correct IWebObjectContext (using the foundset property's DAL)
			return NGConversions.INSTANCE.convertRhinoToSabloComponentValue(rhinoValue, null, wrappedPropertyDescription, wrappedComponentContext);
		}

	}

	private FoundsetTypeSabloValue getFoundsetValue()
	{
		if (webObjectContext != null)
		{
			return (FoundsetTypeSabloValue)webObjectContext.getProperty(forFoundsetPropertyName);
		}
		return null;
	}

	public IFoundSetInternal getFoundset()
	{
		if (webObjectContext != null)
		{
			Object property = webObjectContext.getProperty(forFoundsetPropertyName);
			if (property instanceof FoundsetTypeSabloValue)
			{
				return ((FoundsetTypeSabloValue)property).getFoundset();
			}
		}
		return null;
	}

	@Override
	public void attachToBaseObject(final IChangeListener monitor, final IWebObjectContext webObjectCntxt)
	{
		// detach first if needed so that we don't add listeners twice (the method can be called as well when for example the linked foundset value changes or if value is in arrays/custom objects and those get completely replaced with browser sent value)
		// see below; attach is also called from within "propertyChange"; it is also possible that due to some unexpected situation attach is called twice from the outside world without a detach between them...
		detach();

		this.webObjectContext = webObjectCntxt;
		this.changeMonitor = monitor;

		// in case the linked foundSet property is not yet initialized or it changes, we must call attach again to prepare what's needed
		this.webObjectContext.addPropertyChangeListener(forFoundsetPropertyName, forFoundsetPropertyListener = new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				attachToBaseObject(monitor, webObjectCntxt);
			}
		});

		final FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();

		if (foundsetPropValue == null) return;

		foundsetPropValue.addStateChangeListener(foundsetStateChangeListener = new IChangeListener()
		{
			@Override
			public void valueChanged()
			{
				attachToBaseObject(monitor, webObjectCntxt);
			}
		});
		FoundsetDataAdapterList dal = foundsetPropValue.getDataAdapterList();

		if (dal == null) return; // foundset property val. is not yet attached to component it seems

		idForFoundset = null;
		foundsetLinkedDPs.clear();
		dal.addDataLinkedPropertyRegistrationListener(getDataLinkedPropertyRegistrationListener(changeMonitor, foundsetPropValue));

		wrappedComponentContext = new NGComponentDALContext(foundsetPropValue.getDataAdapterList(), webObjectContext);

		if (!wrappedValueInitialized)
		{
			wrappedValueInitialized = true;

			// the following conversion also adds dal data listeners when needed and will trigger dataLinkedPropertyRegistrationListener above which updates the record-linked or not state
			// can already be non-null if it was a default value or if for some reason the value was detached and re-attached to a component
			if (wrappedSabloValue == null)
			{
				setWrappedSabloValue(initializingState.initialize());
			}
			else if (wrappedSabloValue instanceof IHasUnderlyingState)
				((IHasUnderlyingState)wrappedSabloValue).addStateChangeListener(getWrappedUnderlyingStateListener());
		}
		else if (wrappedSabloValue instanceof IHasUnderlyingState)
			((IHasUnderlyingState)wrappedSabloValue).addStateChangeListener(getWrappedUnderlyingStateListener());

		if (wrappedSabloValue instanceof IDataLinkedPropertyValue)
			((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(getWrappedPropChangeMonitor(monitor), wrappedComponentContext);
	}

	private void setWrappedSabloValue(YT newValue)
	{
		if (wrappedSabloValue instanceof IHasUnderlyingState)
			((IHasUnderlyingState)wrappedSabloValue).removeStateChangeListener(getWrappedUnderlyingStateListener());

		wrappedSabloValue = newValue;
		fireUnderlyingPropertyChangeListeners();

		if (wrappedSabloValue instanceof IHasUnderlyingState)
			((IHasUnderlyingState)wrappedSabloValue).addStateChangeListener(getWrappedUnderlyingStateListener());
	}

	private IChangeListener getWrappedUnderlyingStateListener()
	{
		if (wrappedUnderlyingStateListener == null) wrappedUnderlyingStateListener = new IChangeListener()
		{
			@Override
			public void valueChanged()
			{
				fireUnderlyingPropertyChangeListeners();
			}
		};
		return wrappedUnderlyingStateListener;
	}

	private IChangeListener getWrappedPropChangeMonitor(final IChangeListener monitor)
	{
		// if the wrapped property is really linked to foundset data, the change monitor given to the wrapped property needs to mark a certain row in the viewportChangeMonitor as dirty
		// if it is called - as that means that the wrapped prop.'s value has changed for the current record
		// for example if a foundset linked DP value which is set to a related dataprovider changes value on the current FoundsetDataAdapterList record we want to send that change for that record to client
		return new IChangeListener()
		{

			@Override
			public void valueChanged()
			{
				if (actualWrappedValueChangeHandlerForFoundsetLinked != null)
				{
					FoundsetTypeSabloValue foundsetValue = getFoundsetValue();
					if (foundsetValue != null && !foundsetValue.getDataAdapterList().isQuietRecordChangeInProgress())
					{
						// here for column name we give the name of the first foundset linked DP that the wrapped sablo val. attached to the dal, or if not available then the actual prop. name;
						// the idea is that when this will call a queueCellChange on the viewPortChangeMonitor, it should pass the columnName check (from #usesFoundsetLinkedDataprovider() that gets
						// called from FoundsetLinkedViewportRowDataProvider.containsColumn(String))
						// TODO maybe we should give a generated UUID or something instead of wrappedPropertyDescription.getName() if there are no foundsetLinkedDPs (linked-to-all) and add the same
						// UUID to foundsetLinkedDPs to avoid overlaps between wrapped PD and foundset given column names that could be checked in usesFoundsetLinkedDataprovider; but it's not a big
						// deal as worst it could happen I think is sending the foundset linked value to client a bit more often then needed in case of such an overlap and changes happening in
						// foundset on the column with the same name;
						actualWrappedValueChangeHandlerForFoundsetLinked.valueChangedInFSLinkedUnderlyingValue(
							foundsetLinkedDPs.size() > 0 ? foundsetLinkedDPs.iterator().next() : wrappedPropertyDescription.getName(), viewPortChangeMonitor);
					}
				}
				else
				{
					monitor.valueChanged();
				}
			}
		};
	}

	@Override
	public void detach()
	{
		// this wrapped detach() should normally trigger unregister idForFoundset and remove viewPortChangeMonitor as well if needed - in dataLinkedPropertyUnregistered() below
		if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
		if (wrappedSabloValue instanceof IHasUnderlyingState)
			((IHasUnderlyingState)wrappedSabloValue).removeStateChangeListener(getWrappedUnderlyingStateListener());

		FoundsetTypeSabloValue foundsetPropValue = getFoundsetValue();

		if (forFoundsetPropertyListener != null && webObjectContext != null)
		{
			webObjectContext.removePropertyChangeListener(forFoundsetPropertyName, forFoundsetPropertyListener);
			forFoundsetPropertyListener = null;
		}

		if (foundsetPropValue != null)
		{
			if (foundsetStateChangeListener != null)
			{
				foundsetPropValue.removeStateChangeListener(foundsetStateChangeListener);
				foundsetStateChangeListener = null;
			}

			FoundsetDataAdapterList dal = foundsetPropValue.getDataAdapterList();

			if (dataLinkedPropertyRegistrationListener != null)
			{
				dal.removeDataLinkedPropertyRegistrationListener(dataLinkedPropertyRegistrationListener);
				dataLinkedPropertyRegistrationListener = null;
			}
		}

		webObjectContext = null;
		changeMonitor = null;
	}

	protected IDataLinkedPropertyRegistrationListener getDataLinkedPropertyRegistrationListener(final IChangeListener chMonitor,
		final FoundsetTypeSabloValue foundsetPropValue)
	{
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

					if (viewPortChangeMonitor != null) foundsetPropValue.removeViewportDataChangeMonitor(viewPortChangeMonitor);

					ViewportDataChangeMonitor< ? > old = viewPortChangeMonitor;
					actualWrappedValueChangeHandlerForFoundsetLinked = new FoundsetLinkedValueChangeHandler(foundsetPropValue);
					viewPortChangeMonitor = new ViewportDataChangeMonitor<>(chMonitor, new FoundsetLinkedViewportRowDataProvider<YF, YT>(
						foundsetPropValue.getDataAdapterList(), wrappedPropertyDescription, FoundsetLinkedTypeSabloValue.this));
					foundsetPropValue.addViewportDataChangeMonitor(viewPortChangeMonitor);
					if (old != null) viewPortChangeMonitor.viewPortCompletelyChanged = old.viewPortCompletelyChanged;

					if (targetDataLinks.dataProviderIDs != null &&
						targetDataLinks.dataProviderIDs.length > 0) /* this condition should always be true, except when it's TargetDataLinks.LINKED_TO_ALL */
					{
						if (idForFoundset == null)
						{
							// register the first dataprovider used by the wrapped property to the foundset for sorting
							idForFoundset = Utils.calculateMD5HashBase16(targetDataLinks.dataProviderIDs[0]);
							idForFoundsetChanged = true;
							foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(idForFoundset, targetDataLinks.dataProviderIDs[0]);
						}
						foundsetLinkedDPs.addAll(Arrays.asList(targetDataLinks.dataProviderIDs));
					}

					if (changed)
					{
						chMonitor.valueChanged();
						viewPortChangeMonitor.viewPortCompletelyChanged();
					}
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
					if (idForFoundset != null)
					{
						foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(idForFoundset, null);
						idForFoundset = null;
						idForFoundsetChanged = true;
						foundsetLinkedDPs.clear();
					}
					viewPortChangeMonitor = null;
					actualWrappedValueChangeHandlerForFoundsetLinked = null;
					chMonitor.valueChanged();
				}
			}

		};
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (wrappedSabloValue instanceof IDataLinkedPropertyValue)
			((IDataLinkedPropertyValue)wrappedSabloValue).dataProviderOrRecordChanged(record, dataProvider, isFormDP, isGlobalDP, fireChangeEvent);
	}

	public YT getWrappedValue()
	{
		return wrappedSabloValue;
	}

	public static Object unwrapIfNeeded(Object sabloValue)
	{
		if (sabloValue instanceof FoundsetLinkedTypeSabloValue< ? , ? >) return ((FoundsetLinkedTypeSabloValue)sabloValue).getWrappedValue();
		else return sabloValue;
	}

	public void rhinoToSablo(Object rhinoValue, PropertyDescription wrappedPD, IWebObjectContext webObjectCntxt)
	{
		if (wrappedValueInitialized)
		{
			YT newWrappedVal;
			newWrappedVal = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(rhinoValue, wrappedSabloValue, wrappedPD, wrappedComponentContext);

			if (newWrappedVal != wrappedSabloValue)
			{
				// do what component would do when a property changed
				// TODO should we make current method return a completely new instance instead and leave component code do the rest?
				if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();

				setWrappedSabloValue(newWrappedVal);

				if (wrappedSabloValue instanceof IDataLinkedPropertyValue)
					((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(changeMonitor, wrappedComponentContext);
			}

//			changeMonitor.valueChanged();
		}
		else
		{
			// should we treat this as well? I don't think this should ever happen under normal conditions
			Debug.error("An attempt to set foundset linked property '" + wrappedPD + "' from scripting on " + webObjectCntxt +
				" failed because the (existing) foundset linked prop. was not yet initialized...", new RuntimeException());
		}
	}

	public JSONWriter fullToJSON(JSONWriter writer, String key, DataConversion clientConversion, PropertyDescription wrappedPropertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (!wrappedValueInitialized)
		{
			Debug.warn("Trying to get full value from an uninitialized foundset linked property: " + wrappedPropertyDescription);
			return writer;
		}

		clientConversion.convert(FoundsetLinkedPropertyType.CONVERSION_NAME);
		JSONUtils.addKeyIfPresent(writer, key);

		writer.object();
		writer.key(FoundsetLinkedPropertyType.FOR_FOUNDSET_PROPERTY_NAME).value(forFoundsetPropertyName);
		if (idForFoundset != null) writer.key(ID_FOR_FOUNDSET).value(idForFoundset == null ? JSONObject.NULL : idForFoundset);
		idForFoundsetChanged = false;

		PushToServerEnum pushToServer = BrowserConverterContext.getPushToServerValue(dataConverterContext);
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
			viewPortChangeMonitor.getRowDataProvider().initializeIfNeeded(dataConverterContext);
			viewPortChangeMonitor.clearChanges();
			// record dependent; viewport value
			writeWholeViewportToJSON(writer);
			viewPortChangeMonitor.doneWritingChanges();
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
			foundsetPropertyViewPort.getStartIndex() + foundsetPropertyViewPort.getSize() - 1, null, getFoundsetValue().getFoundset(), destinationJSON,
			clientConversionInfo, wrappedSabloValue);
		clientConversionInfo.popNode();

		// conversion info for websocket traffic (for example Date objects will turn into long)
		JSONUtils.writeClientConversions(destinationJSON, clientConversionInfo);
	}

	public JSONWriter changesToJSON(JSONWriter writer, String key, DataConversion clientConversion, PropertyDescription wrappedPropertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (!wrappedValueInitialized)
		{
			Debug.warn("Trying to get changes from an uninitialized foundset linked property: " + wrappedPropertyDescription);
			return writer;
		}

		clientConversion.convert(FoundsetLinkedPropertyType.CONVERSION_NAME);
		JSONUtils.addKeyIfPresent(writer, key);

		writer.object();
		if (idForFoundsetChanged)
		{
			writer.key(ID_FOR_FOUNDSET).value(idForFoundset == null ? JSONObject.NULL : idForFoundset);
			idForFoundsetChanged = false;
		}

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
			viewPortChangeMonitor.getRowDataProvider().initializeIfNeeded(dataConverterContext);

			if (viewPortChangeMonitor.shouldSendWholeViewport())
			{
				viewPortChangeMonitor.clearChanges();
				writeWholeViewportToJSON(writer);
			}
			else if (viewPortChangeMonitor.hasViewportChanges())
			{
				DataConversion clientConversionInfo = new DataConversion();
				writer.key(FoundsetLinkedPropertyType.VIEWPORT_VALUE_UPDATE);
				clientConversionInfo.pushNode(FoundsetLinkedPropertyType.VIEWPORT_VALUE_UPDATE);

				ArrayOperation[] viewPortChanges = viewPortChangeMonitor.getViewPortChanges();
				viewPortChangeMonitor.clearChanges();

				writer.array();
				for (int i = 0; i < viewPortChanges.length; i++)
				{
					clientConversionInfo.pushNode(String.valueOf(i));
					FoundsetPropertyType.writeViewportOperationToJSON(viewPortChanges[i], viewPortChangeMonitor.getRowDataProvider(), getFoundset(),
						getFoundsetValue().getViewPort().getStartIndex(), writer, null, clientConversionInfo, wrappedSabloValue);
					clientConversionInfo.popNode();
				}
				writer.endArray();
				clientConversionInfo.popNode();

				// conversion info for websocket traffic (for example Date objects will turn into long)
				JSONUtils.writeClientConversions(writer, clientConversionInfo);
			}
			else viewPortChangeMonitor.clearChanges(); // else there is no change to send but clear it anyway!

			viewPortChangeMonitor.doneWritingChanges();
		}
		writer.endObject();

		return writer;
	}

	public void browserUpdatesReceived(Object newJSONValue, PropertyDescription wrappedPropertyDescription, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		PushToServerEnum pushToServer = BrowserConverterContext.getPushToServerValue(dataConverterContext);

		if (!wrappedValueInitialized)
		{
			Debug.error("Trying to update state for an uninitialized foundset linked property: " + wrappedPropertyDescription + " | " + webObjectContext);
			return;
		}
		if (!(newJSONValue instanceof JSONArray))
		{
			// is a data push
			if (wrappedSabloValue instanceof DataproviderTypeSabloValue)
			{
				((DataproviderTypeSabloValue)wrappedSabloValue).browserUpdateReceived(newJSONValue, dataConverterContext);
			}
			return;
		}
		if ((wrappedPropertyDescription instanceof IPushToServerSpecialType &&
			((IPushToServerSpecialType)wrappedPropertyDescription).shouldAlwaysAllowIncommingJSON()) || PushToServerEnum.allow.compareTo(pushToServer) <= 0)
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
								" | " + webObjectContext);
							return;
						}

						Object object = update.get(PROPERTY_CHANGE);
						YT newWrappedValue = (YT)JSONUtils.fromJSONUnwrapped(wrappedSabloValue, object, wrappedPropertyDescription, dataConverterContext,
							returnValueAdjustedIncommingValue);

						if (newWrappedValue != wrappedSabloValue)
						{
							// do what component would do when a property changed
							// TODO should we make current method return a completely new instance instead and leave component code do the rest?
							if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
							setWrappedSabloValue(newWrappedValue);

							if (wrappedSabloValue instanceof IDataLinkedPropertyValue)
								((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(changeMonitor, wrappedComponentContext);
						}
					}
					else if (update.has(ViewportDataChangeMonitor.VIEWPORT_CHANGED))
					{
						if (viewPortChangeMonitor == null)
						{
							Debug.error("Trying to update some record value for a foundset linked non-record dependent property: " +
								wrappedPropertyDescription + " | " + webObjectContext);
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
					// as svyApply/data push for DPs does not go through here but through NGFormServiceHandler.executeMethod(String, JSONObject), the
					// "setApplyingDPValueFromClient" will be called from there not here...
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

				try
				{
					ValueReference<Boolean> returnValueAdjustedIncommingValueForRow = new ValueReference<Boolean>(Boolean.FALSE);
					YT newWrappedValue = (YT)JSONUtils.fromJSONUnwrapped(wrappedSabloValue, value, wrappedPropertyDescription, dataConverterContext,
						returnValueAdjustedIncommingValueForRow);

					if (newWrappedValue != wrappedSabloValue)
					{
						// do what component would do when a property changed
						// TODO should we make current method return a completely new instance instead and leave component code do the rest?
						if (wrappedSabloValue instanceof IDataLinkedPropertyValue) ((IDataLinkedPropertyValue)wrappedSabloValue).detach();
						setWrappedSabloValue(newWrappedValue);

						if (wrappedSabloValue instanceof IDataLinkedPropertyValue)
							((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(changeMonitor, wrappedComponentContext);

						// the full value has changed; the whole viewport might be affected
						viewPortChangeMonitor.viewPortCompletelyChanged();
					}
					else if (returnValueAdjustedIncommingValueForRow.value.booleanValue())
					{
						FoundsetTypeViewport viewPort = foundsetPropertyValue.getViewPort();
						int firstViewPortIndex = Math.max(viewPort.getStartIndex(), recordIndex);
						int lastViewPortIndex = Math.min(viewPort.getStartIndex() + viewPort.getSize() - 1, recordIndex);
						if (firstViewPortIndex <= lastViewPortIndex)
						{
							viewPortChangeMonitor.queueOperation(firstViewPortIndex - viewPort.getStartIndex(), lastViewPortIndex - viewPort.getStartIndex(),
								viewPort.getSize(), ArrayOperation.CHANGE);
						}
					}
				}
				catch (JSONException e)
				{
					Debug.error("Setting value for record dependent property '" + wrappedPropertyDescription + "' in foundset linked component to value: " +
						value + " failed.", e);
				}
				finally
				{
					foundsetPropertyValue.setDataAdapterListToSelectedRecord();
				}
			}
			else
			{
				Debug.error("Cannot set foundset linked record dependent property for (" + rowIDValue + ") property '" + wrappedPropertyDescription +
					"' to value '" + value + "' of component: " + webObjectContext + ". Record not found.", new RuntimeException());
			}
		}
		else
		{
			Debug.error("Cannot set foundset linked record dependent property for (" + rowIDValue + ") property '" + wrappedPropertyDescription +
				"' to value '" + value + "' of component: " + webObjectContext + ". Foundset is null.", new RuntimeException());
		}
	}

	@Override
	public String toString()
	{
		return "foundsetLinked(" + wrappedSabloValue + ")";
	}

	@Override
	public void addStateChangeListener(IChangeListener valueChangeListener)
	{
		if (!underlyingValueChangeListeners.contains(valueChangeListener)) underlyingValueChangeListeners.add(valueChangeListener);
	}

	@Override
	public void removeStateChangeListener(IChangeListener valueChangeListener)
	{
		underlyingValueChangeListeners.remove(valueChangeListener);
	}

	protected void fireUnderlyingPropertyChangeListeners()
	{
		if (underlyingValueChangeListeners.size() > 0)
		{
			// just in case any listeners will end up trying to alter underlyingValueChangeListeners - avoid a ConcurrentModificationException
			IChangeListener[] copyOfListeners = underlyingValueChangeListeners.toArray(new IChangeListener[underlyingValueChangeListeners.size()]);
			for (IChangeListener l : copyOfListeners)
			{
				l.valueChanged();
			}
		}
	}

	protected IWebObjectContext getDALWebObjectContext()
	{
		return webObjectContext;
	}

	protected boolean usesFoundsetLinkedDataprovider(String dataProviderID)
	{
		// see #getWrappedPropChangeMonitor() impl. to understand why the wrappedPropertyDescription.getName() is also considered a "used dataprovider"
		return foundsetLinkedDPs.contains(dataProviderID) || Utils.stringSafeEquals(wrappedPropertyDescription.getName(), dataProviderID);
	}

	public void setApplyingDPValueFromClient(boolean applyInProgress)
	{
		if (actualWrappedValueChangeHandlerForFoundsetLinked != null)
			actualWrappedValueChangeHandlerForFoundsetLinked.setApplyingDPValueFromClient(applyInProgress);
	}


}
