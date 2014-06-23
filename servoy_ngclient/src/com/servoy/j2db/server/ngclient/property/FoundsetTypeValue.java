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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebComponentInitializer;
import org.sablo.WebComponent;
import org.sablo.websocket.ConversionLocation;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * Value used at runtime as foundset type value proxy for multiple interested parties (browser, designtime, scripting).
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FoundsetTypeValue implements IServoyAwarePropertyValue
{

	// START keys and values used in JSON
	public static final String SERVER_SIZE = "serverSize";
	public static final String SELECTED_ROW_INDEXES = "selectedRowIndexes";
	public static final String MULTI_SELECT = "multiSelect";
	public static final String VIEW_PORT = "viewPort";
	public static final String START_INDEX = "startIndex";
	public static final String SIZE = "size";
	public static final String ROWS = "rows";
	public static final String CONVERSIONS = "conversions";
	// END keys and values used in JSON

	protected FoundsetTypeViewport viewPort = new FoundsetTypeViewport();
	protected IFoundSetInternal foundset;
	protected final Object designJSONValue;
	protected WebFormComponent component;
	protected IChangeListener changeMonitor;
	protected Set<String> dataProviders = new HashSet<>();
	protected String foundsetSelector;
	protected IFoundSetEventListener foundsetEventListener;
	protected IDataAdapterList dataAdapterList;
	protected String propertyName;

	public FoundsetTypeValue(Object designJSONValue, Object config)
	{
		this.designJSONValue = designJSONValue; // maybe we should parse it and not keep it as JSON (it can be reconstructed afterwards from parseed content if needed)
		// TODO ac Auto-generated constructor stub
	}

	@Override
	public void initialize(IWebComponentInitializer fe, String propertyName, Object defaultValue)
	{
		// nothing to do here; foundset is not initialized until it's attached to a component
		this.propertyName = propertyName;
	}

	@Override
	public void attachToComponent(IChangeListener changeMonitor, WebComponent component)
	{
		this.component = (WebFormComponent)component;
		this.changeMonitor = changeMonitor;

		// get the foundset identifier, then the foundset itself
//		foundset: {
//			foundsetSelector: 'string',
//			dataProviders: 'dataprovider[]'
//		}
		try
		{
			JSONObject spec = (JSONObject)designJSONValue;

			// foundsetSelector as defined in component design XML.
			foundsetSelector = spec.optString("foundsetSelector");
			updateFoundset(null);

			JSONArray dataProvidersJSON = spec.optJSONArray("dataProviders");
			if (dataProvidersJSON != null)
			{
				Set<String> dataProvidersSet = new HashSet<>(dataProvidersJSON.length());
				for (int i = 0; i < dataProvidersJSON.length(); i++)
				{
					dataProvidersSet.add(dataProvidersJSON.getString(i));
				}
				includeDataProviders(dataProvidersSet);
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	/**
	 * Find the foundset to be used based on the design value of "foundsetSelector".\n
	 * It can be either:
	 * <ul>
	 * 	<li>a related foundset based on the component's current record (as one would access it in scripting). Example: "customers_to_orders";</li>
	 * 	<li>the component's foundset (as if in scripting you would say 'foundset') - if foundsetSelector is not specified at design time or null;</li>
	 * 	<li>a new foundset based on the given datasource (as if you would do DatabaseManager.getFoundset(datasource) in scripting). Example: "db:/example_data/customers".</li>
	 * </ul>
	 * 
	 * @param record the record this component is attached to; can be null. (form not linked to table or no records for example) 
	 * 
	 * @return true if the foundset was update, false otherwise.
	 */
	protected boolean updateFoundset(IRecordInternal record)
	{
		IFoundSetInternal newFoundset = null;
		if (record != null)
		{
			if (foundsetSelector == null)
			{
				newFoundset = record.getParentFoundSet();
			}
			else
			{
				Object o = record.getValue(foundsetSelector);
				if (o instanceof IFoundSetInternal)
				{
					newFoundset = (IFoundSetInternal)o;
				}
			}
		}

		if (newFoundset == null && foundsetSelector != null)
		{
			try
			{
				newFoundset = (IFoundSetInternal)getFormUI().getDataConverterContext().getApplication().getFoundSetManager().getFoundSet(foundsetSelector);
			}
			catch (ServoyException e)
			{
				if (record != null) Debug.trace(e);
			}
		}

		if (newFoundset != foundset)
		{
			if (foundset != null) foundset.removeFoundSetEventListener(getFoundsetEventListener());
			foundset = newFoundset;
			if (newFoundset != null) newFoundset.addFoundSetEventListener(getFoundsetEventListener());
			changeMonitor.valueChanged(); // TODO this is a bit doubled here - return value changed + change monitor; can we drop one?
			return true;
		}
		return false;
	}

	protected IFoundSetEventListener getFoundsetEventListener()
	{
		if (foundsetEventListener == null)
		{
			foundsetEventListener = new IFoundSetEventListener()
			{
				@Override
				public void foundSetChanged(FoundSetEvent e)
				{
					// TODO ac Auto-generated method stub
//					if (allChanged) return;
//					if (event.getType() == FoundSetEvent.FIND_MODE_CHANGE || event.getType() == FoundSetEvent.FOUNDSET_INVALIDATED)
//					{
//						// fully changed push everything
//						setAllChanged();
//					}
//					else if (event.getType() == FoundSetEvent.CONTENTS_CHANGED)
//					{
//						// partial change only push the changes.
//						if (event.getChangeType() == FoundSetEvent.CHANGE_DELETE)
//						{
//							int startIdx = (currentPage - 1) * getPageSize();
//							int endIdx = currentPage * getPageSize();
//							if ((startIdx <= event.getFirstRow() && event.getFirstRow() < endIdx) ||
//								(startIdx <= event.getLastRow() && event.getLastRow() < endIdx))
//							{
//								// delete already happened so foundset size is changed
//
//								// first row to be deleted inside current page
//								int startRow = Math.max(startIdx, event.getFirstRow());
//								// number of deletes from current page
//								int numberOfDeletes = Math.min(event.getLastRow() + 1, endIdx) - startRow;
//
//								// we need to replace same amount of records in current page; append rows if available
//								RowData data = getRows(Math.max(event.getLastRow() + 1, endIdx), Math.max(event.getLastRow() + 1, endIdx) + numberOfDeletes);
//
//								rowChanges.add(new RowData(data.rows, startRow - startIdx, startRow + numberOfDeletes - startIdx, RowData.DELETE));
//							}
//						}
//						else if (event.getChangeType() == FoundSetEvent.CHANGE_INSERT)
//						{
//							int startIdx = (currentPage - 1) * getPageSize();
//							int endIdx = currentPage * getPageSize();
//							if (endIdx > currentFoundset.getSize()) endIdx = currentFoundset.getSize();
//							if ((startIdx <= event.getFirstRow() && event.getFirstRow() < endIdx) ||
//								(startIdx <= event.getLastRow() && event.getLastRow() < endIdx))
//							{
//								int startRow = Math.max(startIdx, event.getFirstRow());
//								// number of inserts from current page
//								int numberOfInserts = Math.min(event.getLastRow() + 1, endIdx) - startRow;
//
//								// add records that fit current page
//								RowData rows = getRows(startRow, startRow + numberOfInserts);
//								rows.setType(RowData.INSERT);
//								rowChanges.add(rows);
//							}
//						}
//						else if (event.getChangeType() == FoundSetEvent.CHANGE_UPDATE)
//						{
//							if (currentFoundset != null && event.getFirstRow() == 0 && event.getLastRow() == currentFoundset.getSize() - 1)
//							{
//								// if all the rows were changed, do not add to rows as it could add same thing multiple times
//								allChanged = true;
//							}
//							// get the rows that are changed.
//							RowData rows = getRows(event.getFirstRow(), event.getLastRow() + 1);
//							if (rows != RowData.EMPTY)
//							{
//								rowChanges.add(rows);
//							}
//						}
//					}
//					getApplication().getChangeListener().valueChanged();
				}
			};
		}
		return foundsetEventListener;
	}

	@Override
	public boolean pushRecord(IRecordInternal record)
	{
		return updateFoundset(record);
	}

	@Override
	public void detach()
	{
		if (foundset != null) foundset.removeFoundSetEventListener(getFoundsetEventListener());
	}

	@Override
	public JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO conversion markers should never be null I think, but it did happen (due to JSONUtils.toJSONValue(JSONWriter writer, Object value, IForJsonConverter forJsonConverter, ConversionLocation toDestinationType); will create a case for that
		if (conversionMarkers != null) conversionMarkers.convert(FoundsetTypeImpl.TYPE_ID); // so that the client knows it must use the custom client side JS for what JSON it gets

		destinationJSON.object();
		destinationJSON.key(SERVER_SIZE).value(foundset != null ? foundset.getSize() : 0);
		destinationJSON.key(SELECTED_ROW_INDEXES).array();
		if (foundset != null)
		{
			for (int idx : foundset.getSelectedIndexes())
			{
				destinationJSON.value(idx);
			}
		}
		destinationJSON.endArray();
		destinationJSON.key(MULTI_SELECT).value(foundset != null ? foundset.isMultiSelect() : false);

		// viewPort
		destinationJSON.key(VIEW_PORT).object();
		correctViewportBoundsIfNeeded();
		destinationJSON.key(START_INDEX).value(viewPort.startIndex).key(SIZE).value(viewPort.size);
//		rows: [
//	         	{ _svyRowId: 'someRowIdHASH1', nameColumn: "Bubu" },
//	         	{ _svyRowId: 'someRowIdHASH2', nameColumn: "Yogy" },
//				(...)
//	    ]
		if (foundset != null)
		{
			Map<String, Object> rows = new HashMap<>();
			Map<String, Object>[] rowsArray = new Map[viewPort.size];
			rows.put(ROWS, rowsArray);

			for (int i = viewPort.startIndex + viewPort.size - 1; i >= viewPort.startIndex; i--)
			{
				rowsArray[i - viewPort.startIndex] = new HashMap<>();
				// write viewport row contents
				IRecordInternal record = foundset.getRecord(i);
				rowsArray[i - viewPort.startIndex].put("_svyRowId", record.getPKHashKey() + "_" + i); // TODO do we really need the "i"?

				Iterator<String> it = dataProviders.iterator();
				while (it.hasNext())
				{
					String dataProvider = it.next();

					// TODO currently we also send globals/form variables through foundset; in the future it should be enough to get it from the record only, not through DataAdapterList.getValueObject!
					Object value = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, getFormUI().getController().getFormScope(),
						dataProvider);
					rowsArray[i - viewPort.startIndex].put(dataProvider, value);
				}
			}

			// convert for websocket traffic (for example Date objects will turn into long)
			JSONUtils.writeDataWithConversions(destinationJSON, rows,
				getFormUI().getDataConverterContext().getApplication().getWebsocketSession().getForJsonConverter(), ConversionLocation.BROWSER_UPDATE);
		}
		else
		{
			destinationJSON.key(ROWS).array().endArray();
		}
		destinationJSON.endObject();
		// end viewPort

		destinationJSON.endObject();
		return destinationJSON;
	}

	@Override
	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO ac Auto-generated method stub
//		if (changed)
		return toJSON(destinationJSON, conversionMarkers);
//		else destinationJSON.value(null);
//		return destinationJSON;
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
		if (foundset == null) return;
		// TODO ac Auto-generated method stub
		try
		{
			if (jsonValue instanceof JSONObject)
			{
				JSONObject update = (JSONObject)jsonValue;
				// {newViewPort: {startIndex : startIndex, size : size}}
				if (update.has("newViewPort"))
				{
					JSONObject newViewport = update.getJSONObject("newViewPort");
					int oldStartIndex = viewPort.startIndex;
					int oldSize = viewPort.size;
					viewPort.startIndex = newViewport.getInt(START_INDEX);
					viewPort.size = newViewport.getInt(SIZE);
					correctViewportBoundsIfNeeded();
					if (oldStartIndex != viewPort.startIndex || oldSize != viewPort.size) changeMonitor.valueChanged();
				}
			}
		}
		catch (JSONException e)
		{
			Debug.error("Error when getting browser updates for property (" + this.toString() + ")", e);
		}
	}

	/**
	 * If client requested invalid bounds or due to foundset changes the previous bounds
	 * are no longer valid, correct them.
	 */
	protected void correctViewportBoundsIfNeeded()
	{
		if (foundset != null)
		{
			viewPort.startIndex = Math.max(0, Math.min(viewPort.startIndex, foundset.getSize() - 1));
			viewPort.size = Math.max(0, Math.min(viewPort.size, foundset.getSize() - viewPort.startIndex));
		}
		else
		{
			viewPort.startIndex = 0;
			viewPort.size = 0;
		}
	}

	/**
	 * When this foundset is used in combination with child "components" properties, those properties will need
	 * a dataAdapterList that is being fed records from this foundset.
	 */
	public IDataAdapterList getDataAdapterList()
	{
		// this method gets called by linked component type property/properties
		if (dataAdapterList == null)
		{
			dataAdapterList = new DataAdapterList(getFormUI().getController());
		}
		return dataAdapterList;
	}

	private IWebFormUI getFormUI()
	{
		return (IWebFormUI)component.getParent();
	}

	/**
	 * Register a list of dataproviders that is needed client-side.
	 * @param dataProvidersToSend a list of dataproviders that will be sent to the browser as part of the foundset property's viewport data.
	 */
	public void includeDataProviders(Set<String> dataProvidersToSend)
	{
		dataProviders.addAll(dataProvidersToSend);

		if (changeMonitor != null && dataProvidersToSend.size() > 0) changeMonitor.valueChanged();
	}

	@Override
	public String toString()
	{
		return "'" + propertyName + "' foundset type property on component " + (component != null ? component.getName() : "- not yet attached -");
	}

}
