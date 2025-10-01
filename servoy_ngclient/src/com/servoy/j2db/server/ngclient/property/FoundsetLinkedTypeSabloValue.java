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
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IPushToServerSpecialType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.ISupportTemplateValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.util.Debug;
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

	protected static final String ID_FOR_FOUNDSET = "idForFoundset";

	/**
	 * Constant given to viewport change monitor and row data provider as column name.
	 * It doesn't matter much because there is only one "cell" to write in case of foundset-linked properties (unlike
	 * foundset props. with columns and component props. with properties which can have multiple cells in one row).
	 */
	protected static final String DUMMY_COL_NAME = "";

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

	private final Set<String> foundsetLinkedDPs = new HashSet<>();
	private boolean foundsetLinkedUsesAllDPs = false;

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
		public YT initialize(IChangeListener monitor);
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

		public YT initialize(IChangeListener monitor)
		{
			YT sabloVal = (YT)NGConversions.INSTANCE.convertFormElementToSabloComponentValue(formElementValue, wrappedPropertyDescription, formElement,
				(WebFormComponent)webObjectContext.getUnderlyingWebObject(), (DataAdapterList)wrappedComponentContext.getDataAdapterList());

			// if the wrapped type didn't want to be in template value, we have to notify a change on this prop. to tell
			// the parent container to move this from template runtime values to runtime values - in order for it to be sent to browser;
			// this is needed because valueInTemplate(...) call for foundset linked type always returns true (wants to be in template) while
			// wrapped would generally return false (for example dataproviders want to be just runtime values) and expect to be sent to browser
			// after being attached and calculating their initial value; without this if, a foundset linked dataprovider prop in the root of a
			// component would not send it's value to browser initially if developer assigned to it at design time a form variable for example
			IPropertyType< ? > wrappedType = wrappedPropertyDescription.getType();
			if ((wrappedType instanceof ISupportTemplateValue) && // types that do not implement ISupportTemplateValue are considered to be in template
				!((ISupportTemplateValue<YF>)wrappedType).valueInTemplate(formElementValue, wrappedPropertyDescription,
					new FormElementContext(formElement.getRootFormElement())))
			{
				monitor.valueChanged();
			}

			return sabloVal;
		}

	}

	private class RhinoInitializingState implements InitializingState<YT>
	{

		private final Object rhinoValue;

		public RhinoInitializingState(Object rhinoValue)
		{
			this.rhinoValue = rhinoValue;
		}

		public YT initialize(IChangeListener monitor)
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

	public int getRecordIndexHint()
	{
		if (webObjectContext != null)
		{
			Object property = webObjectContext.getProperty(forFoundsetPropertyName);
			if (property instanceof FoundsetTypeSabloValue)
			{
				return ((FoundsetTypeSabloValue)property).getRecordIndexHint();
			}
		}
		return 0; // we no longer send index hint to client; if we don't have a FoundsetTypeSabloValue use 0 as hint, but this should not happen
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
				attachToBaseObject(monitor, webObjectCntxt); // it does "detach" first at the beginning of attachToBaseObject anyway, that is why we don't call detach here
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
		foundsetLinkedUsesAllDPs = false;

		dal.addDataLinkedPropertyRegistrationListener(getDataLinkedPropertyRegistrationListener(changeMonitor, foundsetPropValue));

		wrappedComponentContext = new NGComponentDALContext(foundsetPropValue.getDataAdapterList(), webObjectContext);

		if (!wrappedValueInitialized)
		{
			wrappedValueInitialized = true;

			// the following conversion also adds dal data listeners when needed and will trigger dataLinkedPropertyRegistrationListener above which updates the record-linked or not state
			// can already be non-null if it was a default value or if for some reason the value was detached and re-attached to a component
			if (wrappedSabloValue == null)
			{
				setWrappedSabloValue(initializingState.initialize(monitor));
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
						actualWrappedValueChangeHandlerForFoundsetLinked.valueChangedInFSLinkedUnderlyingValue(DUMMY_COL_NAME, viewPortChangeMonitor);
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
		if (webObjectContext == null) return; // it is already detached

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
				if (dal != null) dal.removeDataLinkedPropertyRegistrationListener(dataLinkedPropertyRegistrationListener);
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
					viewPortChangeMonitor = new FoundsetLinkedTypeViewportDataChangeMonitor<>(chMonitor, new FoundsetLinkedViewportRowDataProvider<YF, YT>(
						foundsetPropValue.getDataAdapterList(), wrappedPropertyDescription, FoundsetLinkedTypeSabloValue.this),
						FoundsetLinkedTypeSabloValue.this);
					foundsetPropValue.addViewportDataChangeMonitor(viewPortChangeMonitor);
					if (old != null) viewPortChangeMonitor.viewPortCompletelyChanged = old.viewPortCompletelyChanged;

					if (targetDataLinks.dataProviderIDs != null &&
						targetDataLinks.dataProviderIDs.length > 0)
					{
						// this condition above should always be true, except when it's TargetDataLinks.LINKED_TO_ALL
						// because there is a check above for targetDataLinks.recordLinked == true
						if (idForFoundset == null)
						{
							// register the first dataprovider used by the wrapped property to the foundset for sorting
							idForFoundset = Utils.calculateMD5HashBase16(targetDataLinks.dataProviderIDs[0]);
							idForFoundsetChanged = true;
							foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(idForFoundset, targetDataLinks.dataProviderIDs[0]);
						}
						foundsetLinkedDPs.addAll(Arrays.asList(targetDataLinks.dataProviderIDs));
						foundsetLinkedUsesAllDPs = false;
					}
					else foundsetLinkedUsesAllDPs = true;

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
					foundsetPropValue.removeViewportDataChangeMonitor(viewPortChangeMonitor);
					if (idForFoundset != null)
					{
						foundsetPropValue.setRecordDataLinkedPropertyIDToColumnDP(idForFoundset, null);
						idForFoundset = null;
						idForFoundsetChanged = true;
						foundsetLinkedDPs.clear();
					}
					foundsetLinkedUsesAllDPs = false;
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

				if (changeMonitor != null)
				{
					if (wrappedSabloValue instanceof IDataLinkedPropertyValue)
						((IDataLinkedPropertyValue)wrappedSabloValue).attachToBaseObject(changeMonitor, wrappedComponentContext);

					changeMonitor.valueChanged();
				}
			}
		}
		else
		{
			// should we treat this as well? I don't think this should ever happen under normal conditions
			Debug.error("An attempt to set foundset linked property '" + wrappedPD + "' from scripting on " + webObjectCntxt +
				" failed because the (existing) foundset linked prop. was not yet initialized...", new RuntimeException());
		}
	}

	public JSONWriter fullToJSON(JSONWriter writer, String key, PropertyDescription wrappedPropertyDescription, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		if (!wrappedValueInitialized)
		{
			Debug.warn("Trying to get full value from an uninitialized foundset linked property: " + wrappedPropertyDescription);
			return writer;
		}

		JSONUtils.addKeyIfPresent(writer, key);

		writer.object();
		writer.key(FoundsetLinkedPropertyType.FOR_FOUNDSET_PROPERTY_NAME).value(forFoundsetPropertyName);
		if (idForFoundset != null) writer.key(ID_FOR_FOUNDSET).value(idForFoundset);
		idForFoundsetChanged = false;

		if (viewPortChangeMonitor == null)
		{
			// single value; not record dependent
			if (getFoundsetValue() != null)
			{
				// normal situation
				IJSONStringWithClientSideType wrappedJSONValue = JSONUtils.FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(wrappedSabloValue,
					wrappedPropertyDescription,
					dataConverterContext, false);

				writer.key(FoundsetLinkedPropertyType.SINGLE_VALUE).value(wrappedJSONValue); // write it even if it's null (the prop doesn't want to write itself); we need a value to be generated on client in the viewport
				if (wrappedJSONValue.getClientSideType() != null) writer.key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(wrappedJSONValue.getClientSideType());
			}
			else
			{
				// if the foundset property value that this prop. is supposed to use is set to null, we have nothing more to do then send null (it will generate a 0 sized array on the client anyway);
				// in attachToBaseObject we didn't even call attach for the wrapped sablo value so, to avoid exceptions, just send null single value to client
				writer.key(FoundsetLinkedPropertyType.SINGLE_VALUE).value(JSONObject.NULL);
			}
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


		destinationJSON.key(FoundsetLinkedPropertyType.VIEWPORT_VALUE);
		ViewportClientSideTypes clientSideTypesForViewport = viewPortChangeMonitor.getRowDataProvider().writeRowData(foundsetPropertyViewPort.getStartIndex(),
			foundsetPropertyViewPort.getStartIndex() + foundsetPropertyViewPort.getSize() - 1, null, getFoundsetValue().getFoundset(), destinationJSON,
			wrappedSabloValue);

		// conversion info for websocket traffic (for example Date objects will turn into long or String to be usable in JSON)
		if (clientSideTypesForViewport != null) clientSideTypesForViewport.writeClientSideTypes(destinationJSON, JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY);
	}

	public JSONWriter changesToJSON(JSONWriter writer, String key, PropertyDescription wrappedPropertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (!wrappedValueInitialized)
		{
			Debug.warn("Trying to get changes from an uninitialized foundset linked property: " + wrappedPropertyDescription);
			return writer;
		}

		// if the foundset property value that this prop. is supposed to use is set to null, we have nothing more to do then send null (it will generate a 0 sized array on the client anyway);
		// in attachToBaseObject we didn't even call then attach for the wrapped sablo value so, to avoid exceptions, fullToJSON will just send null single value to client
		if (getFoundsetValue() == null) return fullToJSON(writer, key, wrappedPropertyDescription, dataConverterContext);

		boolean somethingWasWritten = false;

		if (idForFoundsetChanged)
		{
			somethingWasWritten = startContentWithObjectIfNeeded(writer, key, somethingWasWritten);

			writer.key(ID_FOR_FOUNDSET).value(idForFoundset == null ? JSONObject.NULL : idForFoundset);
			idForFoundsetChanged = false;
		}

		if (viewPortChangeMonitor == null)
		{
			IJSONStringWithClientSideType wrappedJSONValue = JSONUtils.ChangesToJSONConverter.INSTANCE.getConvertedValueWithClientType(wrappedSabloValue,
				wrappedPropertyDescription,
				dataConverterContext, false);

			if (wrappedJSONValue != null)
			{
				somethingWasWritten = startContentWithObjectIfNeeded(writer, key, somethingWasWritten);
				writer.key(FoundsetLinkedPropertyType.SINGLE_VALUE_UPDATE).value(wrappedJSONValue);
				if (wrappedJSONValue.getClientSideType() != null) writer.key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(wrappedJSONValue.getClientSideType());
			}
		}
		else
		{
			viewPortChangeMonitor.getRowDataProvider().initializeIfNeeded(dataConverterContext);

			if (viewPortChangeMonitor.shouldSendWholeViewport())
			{
				somethingWasWritten = startContentWithObjectIfNeeded(writer, key, somethingWasWritten);
				viewPortChangeMonitor.clearChanges();
				writeWholeViewportToJSON(writer);
			}
			else if (viewPortChangeMonitor.hasViewportChanges())
			{
				somethingWasWritten = startContentWithObjectIfNeeded(writer, key, somethingWasWritten);
				writer.key(FoundsetLinkedPropertyType.VIEWPORT_VALUE_UPDATE);

				ArrayOperation[] viewPortChanges = viewPortChangeMonitor.getViewPortChanges();
				viewPortChangeMonitor.clearChanges();

				writer.array();
				for (ArrayOperation viewPortChange : viewPortChanges)
				{
					FoundsetPropertyType.writeViewportOperationToJSON(viewPortChange, viewPortChangeMonitor.getRowDataProvider(), getFoundset(),
						getFoundsetValue().getViewPort().getStartIndex(), writer, null, wrappedSabloValue);
				}
				writer.endArray();
			}
			else viewPortChangeMonitor.clearChanges(); // else there is no change to send but clear it anyway!

			viewPortChangeMonitor.doneWritingChanges();
		}
		if (somethingWasWritten) writer.endObject();

		return writer;
	}

	private boolean startContentWithObjectIfNeeded(JSONWriter writer, String key, boolean somethingWasWritten)
	{
		if (!somethingWasWritten)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.object();
		}
		return true;
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
			if (viewPortChangeMonitor != null) viewPortChangeMonitor.viewPortCompletelyChanged();
			else changeMonitor.valueChanged();
		}
	}

	protected void updatePropertyValueForRecord(FoundsetTypeSabloValue foundsetPropertyValue, String rowIDValue, Object value,
		PropertyDescription wrappedPropertyDescription, IBrowserConverterContext dataConverterContext)
	{
		IFoundSetInternal foundset = foundsetPropertyValue.getFoundset();

		if (foundset != null)
		{
			int recordIndex = foundset.getRecordIndex(rowIDValue, foundsetPropertyValue.getRecordIndexHint());

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
					if (!viewPortChangeMonitor.hasViewportChanges() && !viewPortChangeMonitor.shouldSendWholeViewport() &&
						(actualWrappedValueChangeHandlerForFoundsetLinked == null ||
							!actualWrappedValueChangeHandlerForFoundsetLinked.willRestoreSelectedRecordToFoundsetDALLater()))
					{
						foundsetPropertyValue.setDataAdapterListToSelectedRecord();
						// if .hasViewportChanges() || .viewPortChangeMonitor.shouldSendWholeViewport(), the selected record will be restored later in toJSON
						// via viewPortChangeMonitor.doneWritingChanges() and
						// in some cases - for example if the viewport has updates due to a valuelist.filter() that just happened on another
						// record then the selected one in the updatePropertyValueForRecord() above, we must not restore selection here, as that
						// changesToJSON that will follow will want to send the result of that filter and not a full valuelist value that might
						// result due to a restore of selected record in the FoundsetDataAdapterList followed by a switch to the
						// record that .filter() was called on when writing changes toJSON...

						// !isApplyingDPValueFromClient() is for the situation (that we have in unit tests and that I guess can happen although the DAL push will update the selection anyway later during the push)
						// when a svyPush/apply comes for an not-yet-selected record; and we don't want to revert the record to selected one until after the value
						// was also pushed to the not-yet-selected record via DAL; we call setDataAdapterListToSelectedRecord() later in that case,
						// when setApplyingDPValueFromClient(false) gets called
					}
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

	protected boolean isLinkedToRecordDP(String columnDPName)
	{
		// FoundsetTypeChangeMonitor.recordsUpdated can end up notifying changes for actual dataproviders - we must check if they affect or not the foundset-linked-value
		return foundsetLinkedUsesAllDPs || foundsetLinkedDPs.contains(columnDPName);
	}

	public void setApplyingDPValueFromClient(final boolean applyInProgress)
	{
		if (actualWrappedValueChangeHandlerForFoundsetLinked != null)
			actualWrappedValueChangeHandlerForFoundsetLinked.setApplyingDPValueFromClient(applyInProgress);
	}

}
