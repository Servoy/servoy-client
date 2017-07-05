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

package com.servoy.j2db.server.ngclient.property.types;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IPropertyDescriptionProvider;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormAndTableDataProviderLookup;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.ColumnBasedValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.IHasUnderlyingState;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType.ValuelistPropertyDependencies;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link ValueListPropertyType}.
 * Handles any needed listeners and deals with to and from browser communications, filtering, ....
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ValueListTypeSabloValue implements IDataLinkedPropertyValue, ListDataListener, PropertyChangeListener, IChangeListener
{

	private boolean initialized;

	// values that we have from the start or before initialization
	private final Object valuelistIdentifier;
	private final PropertyDescription vlPD;
	private final IDataAdapterList dataAdapterListToUse; // can be component DAL or, if we have a for: dataprovider in spec and that dataprovider is for: foundsetProperty it will be the DAL of that foundset property (through the courtesy of FoundsetLinkedPropertyType as future case SVY-11204)
	private IWebObjectContext webObjectContext;

	private final ValuelistPropertyDependencies propertyDependencies;
	private boolean waitForDataproviderIfNull;
	private boolean waitForFormatIfNull;

	private IChangeListener changeMonitor;

	// values available after the ValueListTypeSabloValue initialization was completed
	private IValueList valueList;
	private String dataproviderID;
	private ComponentFormat format;
	private String formatParsedString;
	private FoundsetTypeSabloValue foundsetPropertySabloValue;
	private ITable foundsetPropertyTable;

	private IRecordInternal previousRecord;
	private LookupListModel filteredValuelist;
	private String filterStringForResponse; // when a filter(...) is requested, we must include the filter string that was applied to client (so that it can resolve the correct promise in case multiple filter calls are done quickly)

	/**
	 * Creates a new ValueListTypeSabloValue that is not ready yet for operation.<br/>
	 * It will be initialized once it has everything it needs.
	 *
	 * @param valuelistIdentifier the id or uuid of the valuelist persist or the valuelist's name.
	 * @param propertyDependencies if the spec declares a for: dataproviderProperty or if the spec declares a for: valuelist for format properties that point to this valuelist type, or the forDPPropertyName
	 *                             uses a foundset property then all these dependencies to other properties are given using this param. Format dependency is really only used I think when you have
	 *                             a custom valuelist with date values (without separate display values) - to convert the String defined dates in the
	 *                             custom valuelist into actual Date values. The DP or foundset property dependencies are relevant to be able to find the correct DP type to use.
	 * @param waitForDataproviderIfNull can only be true when initialized based on FormElement values; if we know that this valuelist is for a dataprovider property and that property is not supposed
	 *                                  to be null (form element value for that is not null), this is then true; this is useful to wait for a non-null
	 *                                  runtime dataprovider value before initializing the valuelist value, because initially the runtime values are
	 *                                  set into and 'attached' to the component one by one; and we want to avoid initializing the valuelist twice (once with null DP
	 *                                  and then once with the correct DP)
	 * @param waitForFormatIfNull similar to "waitForDataproviderIfNull"; set when we need to wait for the format property to be non-null before init
	 * @param pd the PropertyDescription for this valuelist property.
	 * @param dataAdapterListToUse the DAL of the component; this is only used if we DO NOT have a for: dataproviderProperty that is linked to a
	 *                                 foundset property
	 */
	public ValueListTypeSabloValue(Object valuelistIdentifier, PropertyDescription vlPD, ValuelistPropertyDependencies propertyDependencies,
		boolean waitForDataproviderIfNull, boolean waitForFormatIfNull, IDataAdapterList dataAdapterListToUse)
	{
		this.valuelistIdentifier = valuelistIdentifier;
		this.vlPD = vlPD;
		this.propertyDependencies = propertyDependencies;
		this.waitForDataproviderIfNull = waitForDataproviderIfNull;
		this.waitForFormatIfNull = waitForFormatIfNull;
		this.dataAdapterListToUse = dataAdapterListToUse;

		this.initialized = false;
	}

	/**
	 * Returns either the id or uuid or the name of the valuelist (persist).
	 */
	public Object getValuelistIdentifier()
	{
		return valuelistIdentifier;
	}

	@Override
	public void attachToBaseObject(IChangeListener monitor, IWebObjectContext webObjectCntxt)
	{
		this.changeMonitor = monitor;
		this.webObjectContext = webObjectCntxt;

		if (propertyDependencies.dataproviderPropertyName != null)
			webObjectContext.addPropertyChangeListener(propertyDependencies.dataproviderPropertyName, this);
		if (propertyDependencies.foundsetPropertyName != null) webObjectContext.addPropertyChangeListener(propertyDependencies.foundsetPropertyName, this);
		if (propertyDependencies.formatPropertyName != null) webObjectContext.addPropertyChangeListener(propertyDependencies.formatPropertyName, this);

		initializeIfPossibleAndNeeded(); // adds more listeners if needed (for example for underlying sablo value of a foundset linked value)
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// a sablo value that we are interested in changed
		initializeIfPossibleAndNeeded();
	}

	@Override
	public void valueChanged()
	{
		// the underlying sablo value of a foundset linked value that we are interested in changed
		initializeIfPossibleAndNeeded();
	}

	private void initializeIfPossibleAndNeeded()
	{
		// some dependent property has changed

		// get the new values
		String newDataproviderID = null;
		String newFormatString = null;
		FoundsetTypeSabloValue newFoundsetPropertySabloValue = null;
		ITable newFoundsetPropertyTable = null;

		if (propertyDependencies.foundsetPropertyName != null)
		{
			newFoundsetPropertySabloValue = (FoundsetTypeSabloValue)webObjectContext.getProperty(propertyDependencies.foundsetPropertyName);

			if (newFoundsetPropertySabloValue != null)
			{
				newFoundsetPropertySabloValue.addStateChangeListener(this); // this won't add it twice if it's already added (see javadoc of this call)
				if (newFoundsetPropertySabloValue.getFoundset() != null)
				{
					newFoundsetPropertyTable = newFoundsetPropertySabloValue.getFoundset().getTable();
				}
				else
				{
					newFoundsetPropertyTable = FoundsetTypeSabloValue.getTableBasedOfFoundsetPropertyFromFoundsetIdentifier(
						newFoundsetPropertySabloValue.getFoundsetSelector(), dataAdapterListToUse.getApplication(),
						((IContextProvider)webObjectContext.getUnderlyingWebObject()).getDataConverterContext().getForm().getForm());
				}
			}
		}

		if (propertyDependencies.formatPropertyName != null)
		{
			FormatTypeSabloValue formatSabloValue = ((FormatTypeSabloValue)webObjectContext.getProperty(propertyDependencies.formatPropertyName));
			ComponentFormat componentFormat = (formatSabloValue != null ? formatSabloValue.getComponentFormat() : null);
			newFormatString = ((componentFormat != null && componentFormat.parsedFormat != null) ? componentFormat.parsedFormat.getFormatString() : null);

			if (formatSabloValue != null) formatSabloValue.addStateChangeListener(this); // this won't add it twice if it's already added (see javadoc of this call)
		}
		if (propertyDependencies.dataproviderPropertyName != null)
		{
			Object dataproviderValue = webObjectContext.getProperty(propertyDependencies.dataproviderPropertyName);
			if (dataproviderValue instanceof IHasUnderlyingState) // if it's foundset linked; otherwise this will be false
			{
				((IHasUnderlyingState)dataproviderValue).addStateChangeListener(this); // this won't add it twice if it's already added (see javadoc of this call)
			}

			newDataproviderID = DataAdapterList.getDataProviderID(dataproviderValue); // this will only return non-null if dataproviderValue != null && it is initialized (so foundset is already operational)
		}

		// see if anything we are interested in changed, of if it's not yet initialized (a detach + attach could happen where everything is still equal, but the detach did clear the vl/format and set initialized to false; for example a table column remove and then add back)
		if (!Utils.stringSafeEquals(newDataproviderID, dataproviderID) || !Utils.stringSafeEquals(newFormatString, formatParsedString) ||
			newFoundsetPropertySabloValue != foundsetPropertySabloValue || !Utils.safeEquals(foundsetPropertyTable, newFoundsetPropertyTable) || !initialized)
		{
			// so something did change
			dataproviderID = newDataproviderID;
			foundsetPropertySabloValue = newFoundsetPropertySabloValue;
			foundsetPropertyTable = newFoundsetPropertyTable;
			formatParsedString = newFormatString;

			if ((!waitForDataproviderIfNull || dataproviderID != null) && (!waitForFormatIfNull || newFormatString != null) &&
				(propertyDependencies.foundsetPropertyName == null || (newFoundsetPropertySabloValue != null && newFoundsetPropertyTable != null)))
			{
				// see if all we need is here

				// we don't have a "waitForFoundsetIfNull" because if we really have a foundset-linked-dataprovider, then that one is not initialized until the foundset != null anyway; so we won't get to this place becauuse the dataprovider property would not be ready

				// in case we previously already had an operational valuelist, clear it up as we have new dependency values
				clearUpRuntimeValuelistAndFormat();

				// initialize now
				initializeValuelistAndFormat();

				if (valueList != null)
				{
					valueList.addListDataListener(this);

					// register data link and find mode listeners as needed
					TargetDataLinks dataLinks = getDataLinks();
					dataAdapterListToUse.addDataLinkedProperty(this, dataLinks);

					// reset the initial wait for flags as we have the initial value; any other change in dependent properties has to be treated right away without additional waiting (even if they change to null)
					waitForDataproviderIfNull = false;
					waitForFormatIfNull = false;

					initialized = true;
				}
				else
				{
					Debug.error("Cannot instantiate valuelist (does it exist in the solution?) '" + valuelistIdentifier + "' for property " + vlPD + " of " +
						webObjectContext, new RuntimeException());
					clearUpRuntimeValuelistAndFormat();
				}
				if (changeMonitor != null) changeMonitor.valueChanged();
			}
			else if (initialized)
			{
				// so we don't have yet all we need
				// make sure value is cleared/uninitialized (just in case something became unavailable that was available before)
				clearUpRuntimeValuelistAndFormat();
				if (changeMonitor != null) changeMonitor.valueChanged();
			}
		}
	}

	private ValueListConfig getConfig()
	{
		return (ValueListConfig)vlPD.getConfig();
	}

	public IValueList getValueList()
	{
		return valueList;
	}

	public TargetDataLinks getDataLinks()
	{
		IDataProvider[] dependedDataProviders = valueList.getDependedDataProviders();
		if (dependedDataProviders == null) return TargetDataLinks.NOT_LINKED_TO_DATA;
		if (dependedDataProviders.length == 0) return TargetDataLinks.LINKED_TO_ALL;

		boolean recordLinked = false;
		String[] dataproviders = new String[dependedDataProviders.length];
		for (int i = 0; i < dataproviders.length; i++)
		{
			dataproviders[i] = dependedDataProviders[i].getDataProviderID();
			recordLinked = recordLinked || (dependedDataProviders[i] instanceof IColumn || dependedDataProviders[i] instanceof ColumnWrapper);
		}
		return new TargetDataLinks(dataproviders, recordLinked);
	}

	public void setNewCustomValuelistInstance(IValueList vl)
	{
		// this gets called when items are set specifically for this component (so comp.setValuelistItems(...) in case of legacy or comp.myValuelistProp = someJSDataSet or IDataSet)
		if (valueList != null) valueList.removeListDataListener(this); // remove listener from old custom vl
		this.valueList = vl;
		if (valueList != null) valueList.addListDataListener(this);

		filteredValuelist = null;
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

	protected List<Map<String, Object>> getJavaValueForJSON() // TODO this should return TypedData<List<Map<String, Object>>> instead
	{
		List<Map<String, Object>> jsonValue = null;

		int vlSize = (filteredValuelist != null) ? filteredValuelist.getSize() : valueList.getSize();
		int size = Math.min(getConfig().getMaxCount(), vlSize);
		Object dpRealValue = null;
		Object dpDisplayValue = null;
		boolean containsDpValue = false;
		if (vlSize > getConfig().getMaxCount() && dataproviderID != null && previousRecord != null)
		{
			Object dpvalue = dataAdapterListToUse.getValueObject(previousRecord, dataproviderID);
			int dpindex = (filteredValuelist != null) ? filteredValuelist.realValueIndexOf(dpvalue) : valueList.realValueIndexOf(dpvalue);
			if (dpindex != -1)
			{
				dpRealValue = (filteredValuelist != null) ? filteredValuelist.getRealElementAt(dpindex) : valueList.getRealElementAt(dpindex);
				dpDisplayValue = (filteredValuelist != null) ? filteredValuelist.getElementAt(dpindex) : valueList.getElementAt(dpindex);
			}
		}
		List<Map<String, Object>> array = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			Object realValue = (filteredValuelist != null) ? filteredValuelist.getRealElementAt(i) : valueList.getRealElementAt(i);
			map.put("realValue", realValue);
			if (Utils.equalObjects(realValue, dpRealValue)) containsDpValue = true;
			Object displayValue = (filteredValuelist != null) ? filteredValuelist.getElementAt(i) : valueList.getElementAt(i);
			if (displayValue instanceof Timestamp)
			{
				map.put("displayValue", displayValue);
			}
			else
			{
				map.put("displayValue", displayValue != null ? dataAdapterListToUse.getApplication().getI18NMessageIfPrefixed(displayValue.toString()) : "");
			}
			array.add(map);
		}
		if (!containsDpValue && dpRealValue != null)
		{
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("realValue", dpRealValue);
			if (dpDisplayValue instanceof Timestamp)
			{
				map.put("displayValue", dpDisplayValue);
			}
			else
			{
				map.put("displayValue",
					dpDisplayValue != null ? dataAdapterListToUse.getApplication().getI18NMessageIfPrefixed(dpDisplayValue.toString()) : "");
			}
			array.add(map);
		}
		logMaxSizeExceptionIfNecessary(valueList.getName(), vlSize);
		jsonValue = array;

		return jsonValue;
	}

	/**
	 * @param valuelistSize
	 *
	 */
	private void logMaxSizeExceptionIfNecessary(String valueListName, int valuelistSize)
	{
		if (getConfig().getMaxCount() < valuelistSize && getConfig().shouldLogWhenOverMax()) dataAdapterListToUse.getApplication().reportJSError(
			"Valuelist " + valueListName + " fully loaded with " + getConfig().getMaxCount() + " rows, more rows are discarded!!", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	}

	protected FlattenedSolution getFlattenedSolution()
	{
		return webObjectContext != null ? ((WebFormComponent)webObjectContext.getUnderlyingWebObject()).getFormElement().getFlattendSolution() : null; // we could also find formUI and get the flattened solution from there but I think it should be the same one
	}

	private void clearUpRuntimeValuelistAndFormat()
	{
		if (valueList != null)
		{
			if (filteredValuelist != null)
			{
				filteredValuelist.removeListDataListener(this);
			}
			else
			{
				valueList.removeListDataListener(this);
			}

			dataAdapterListToUse.removeDataLinkedProperty(this);

		}
		valueList = null;
		format = null;

		initialized = false;
	}

	@Override
	public void detach()
	{
		clearUpRuntimeValuelistAndFormat();

		if (propertyDependencies.dataproviderPropertyName != null)
		{
			webObjectContext.removePropertyChangeListener(propertyDependencies.dataproviderPropertyName, this);

			Object dataproviderValue = webObjectContext.getProperty(propertyDependencies.dataproviderPropertyName);
			if (dataproviderValue instanceof IHasUnderlyingState) ((IHasUnderlyingState)dataproviderValue).removeStateChangeListener(this);
		}
		if (propertyDependencies.foundsetPropertyName != null)
		{
			webObjectContext.removePropertyChangeListener(propertyDependencies.foundsetPropertyName, this);

			Object foundsetValue = webObjectContext.getProperty(propertyDependencies.foundsetPropertyName);
			if (foundsetValue instanceof IHasUnderlyingState) ((IHasUnderlyingState)foundsetPropertySabloValue).removeStateChangeListener(this);
		}
		if (propertyDependencies.formatPropertyName != null)
		{
			webObjectContext.removePropertyChangeListener(propertyDependencies.formatPropertyName, this);
			Object formatPropertyValue = webObjectContext.getProperty(propertyDependencies.formatPropertyName);
			if (formatPropertyValue instanceof IHasUnderlyingState) ((IHasUnderlyingState)formatPropertyValue).removeStateChangeListener(this);
		}

		this.changeMonitor = null;
		webObjectContext = null;
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (valueList.getValueList().getLazyLoading() && valueList.getSize() == 0 && getConfig().getLazyLoading() && filteredValuelist == null)
		{
			// lazy load, wait for initial filter to load the valuelist
			return;
		}
		if ((previousRecord != null && !previousRecord.equals(record)) || Utils.equalObjects(dataProvider, dataproviderID))
		{
			revertFilter();
		}

		if (filteredValuelist == null && !fireChangeEvent) valueList.removeListDataListener(this);
		try
		{
			valueList.fill(record);
		}
		finally
		{
			if (filteredValuelist == null && !fireChangeEvent) valueList.addListDataListener(this);
		}
		previousRecord = record;
	}

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion) throws IllegalArgumentException, JSONException
	{
		if (!initialized)
		{
			// this is not expected; we should already have all that is needed to initialize the value before the first toJSON executes
			Debug.warn("Trying to send to client an uninitialized valuelist property: " + vlPD + " of " + webObjectContext + ". Will send null for now.");

			// we are still waiting for some dependency before we can initialize the valuelist? when that will be ready we will send the appropriate value to client
			if (key != null) writer.key(key);
			writer.value(null);
			return;
		}
		List<Map<String, Object>> newJavaValueForJSON = getJavaValueForJSON();
		if (clientConversion != null) clientConversion.convert(ValueListPropertyType.TYPE_NAME);
		DataConversion clientConversionsInsideValuelist = new DataConversion();
		if (key != null) writer.key(key);
		writer.object();
		if (filterStringForResponse != null)
		{
			writer.key("filter");
			writer.value(filterStringForResponse);
			filterStringForResponse = null;
		}
		if (valueList != null && valueList.getValueList() != null)
		{
			writer.key("valuelistid");
			writer.value(valueList.getValueList().getID());
		}
		writer.key("values");
		JSONUtils.toBrowserJSONFullValue(writer, null, newJavaValueForJSON, null, clientConversionsInsideValuelist, null);
		writer.endObject();
	}

	private void revertFilter()
	{
		if (filteredValuelist != null)
		{
			filteredValuelist.removeListDataListener(this);
			valueList.addListDataListener(this);
			filteredValuelist = null;
			if (changeMonitor != null) changeMonitor.valueChanged();
		}
	}

	/**
	 * Filters the values of the valuelist for type-ahead-like usage.
	 */
	public void filterValuelist(String filterString)
	{
		if (!initialized)
		{
			Debug.warn("Trying to send to client an uninitialized valuelist property: " + vlPD + " of " + webObjectContext);
			return;
		}

		this.filterStringForResponse = filterString;
		if (filteredValuelist == null)
		{
			if (valueList instanceof DBValueList)
			{
				try
				{
					filteredValuelist = new LookupListModel(dataAdapterListToUse.getApplication(),
						new LookupValueList(valueList.getValueList(), dataAdapterListToUse.getApplication(),
							ComponentFactory.getFallbackValueList(dataAdapterListToUse.getApplication(), dataproviderID, format != null ? format.uiType : 0,
								format != null ? format.parsedFormat : null, valueList.getValueList()),
							format != null && format.parsedFormat != null ? format.parsedFormat.getDisplayFormat() : null));
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
			else if (valueList instanceof CustomValueList)
			{
				filteredValuelist = new LookupListModel(dataAdapterListToUse.getApplication(), (CustomValueList)valueList);
			}
			else if (valueList instanceof LookupValueList)
			{
				filteredValuelist = new LookupListModel(dataAdapterListToUse.getApplication(), (LookupValueList)valueList);
			}
			else if (valueList instanceof ColumnBasedValueList)
			{
				filteredValuelist = ((ColumnBasedValueList)valueList).getListModel();
			}
			else if (valueList instanceof LookupListModel)
			{
				filteredValuelist = (LookupListModel)valueList;
			}

			if (filteredValuelist != null)
			{
				valueList.removeListDataListener(this);
				filteredValuelist.addListDataListener(this);
			}
		}

		if (filteredValuelist != null)
		{
			try
			{
				filteredValuelist.fill(dataAdapterListToUse.getRecord(), dataproviderID, filterString, false);
				if (changeMonitor != null) changeMonitor.valueChanged();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}

	}

	@Override
	public void intervalAdded(ListDataEvent e)
	{
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

	@Override
	public void intervalRemoved(ListDataEvent e)
	{
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

	@Override
	public void contentsChanged(ListDataEvent e)
	{
		if (changeMonitor != null) changeMonitor.valueChanged();
	}


	private void initializeValuelistAndFormat()
	{
		INGApplication application = dataAdapterListToUse.getApplication();
		ValueList valuelistPersist = getValuelistPersist(valuelistIdentifier, application);

		format = getComponentFormat(vlPD, dataAdapterListToUse, getConfig(), dataproviderID, webObjectContext);
		if (valuelistPersist != null)
		{
			valueList = getRealValueList(application, valuelistPersist, format, dataproviderID);
		}
		else
		{
			if ("autoVL".equals(getConfig().getDefaultValue()))
			{
				ITable table = getTableForDp();
				if (dataproviderID != null && table != null && table.getColumnType(dataproviderID) != 0)
				{
					valueList = new ColumnBasedValueList(application, table.getServerName(), table.getName(), dataproviderID);
				}
				else
				{
					// not supported empty valuelist (based on relations) just return an empty valuelist
					valueList = new CustomValueList(application, null, "", false, IColumnTypes.TEXT, null);
				}
			}
		}
	}

	public static ValueList getValuelistPersist(Object valuelistId, IApplication application)
	{
		ValueList valuelistPersist = null;

		int valuelistID = Utils.getAsInteger(valuelistId);
		if (valuelistID > 0)
		{
			valuelistPersist = application.getFlattenedSolution().getValueList(valuelistID);
		}
		else
		{
			UUID uuid = Utils.getAsUUID(valuelistId, false);
			if (uuid != null) valuelistPersist = (ValueList)application.getFlattenedSolution().searchPersist(uuid);
			else if (valuelistId instanceof String) valuelistPersist = application.getFlattenedSolution().getValueList(valuelistId.toString());
		}
		return valuelistPersist;
	}

	/**
	 * If this valuelist is for a dataprovider and that dataprovider is for a foundset (or even from form's foundset) then this method gives the correct table to search in for that DP.
	 * The DP might not be from this table though, it could still be a global variable or form variable.
	 *
	 * @return the table to search DP in
	 */
	private ITable getTableForDp()
	{
		ITable table;
		if (foundsetPropertySabloValue != null) table = foundsetPropertyTable;
		else
		{
			IWebFormUI formUI = ((WebFormComponent)webObjectContext.getUnderlyingWebObject()).findParent(WebFormUI.class);
			table = formUI.getController().getTable();
		}
		return table;
	}

	private ComponentFormat getComponentFormat(PropertyDescription vlPD, IDataAdapterList dataAdapterList, ValueListConfig config, String dpID,
		IPropertyDescriptionProvider comp)
	{
		INGApplication application = dataAdapterList.getApplication();

		return ComponentFormat.getComponentFormat(formatParsedString, dpID,
			new FormAndTableDataProviderLookup(application.getFlattenedSolution(), dataAdapterList.getForm().getForm(), getTableForDp()), application);
	}

	private IValueList getRealValueList(INGApplication application, ValueList val, ComponentFormat format, String dpID)
	{
		return com.servoy.j2db.component.ComponentFactory.getRealValueList(application, val, true, format.dpType, format.parsedFormat, dpID);
	}

	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterListToUse;
	}

	protected boolean isInitialized()
	{
		return initialized;
	}

	protected void resetI18nValue()
	{
		// probably client language has changed - destroy this object (clean everything up) and return a new wrapper
		clearUpRuntimeValuelistAndFormat();
		initializeIfPossibleAndNeeded();
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

}