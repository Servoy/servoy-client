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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.AggregatedPropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebGridFormUI;
import com.servoy.j2db.server.ngclient.WebGridFormUI.RowData;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Value used at runtime as foundset type value proxy for multiple interested parties (browser, designtime, scripting).
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FoundsetTypeSabloValue implements IServoyAwarePropertyValue
{

	/**
	 * Column that is always automatically sent for each record in a foundset's viewport. It's value
	 * uniquely identifies that record.
	 */
	public static final String ROW_ID_COL_KEY = "_svyRowId";

	// START keys and values used in JSON
	public static final String UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them

	public static final String SERVER_SIZE = "serverSize";
	public static final String SELECTED_ROW_INDEXES = "selectedRowIndexes";
	public static final String MULTI_SELECT = "multiSelect";
	public static final String VIEW_PORT = "viewPort";
	public static final String START_INDEX = "startIndex";
	public static final String SIZE = "size";
	public static final String ROWS = "rows";
	public static final String NO_OP = "noOP";

	public static final String CONVERSIONS = "conversions";
	// END keys and values used in JSON

	protected FoundsetTypeViewport viewPort;
	protected IFoundSetInternal foundset;
	protected final Object designJSONValue;
	protected BaseWebObject webObject; // (the component)
	protected Set<String> dataProviders = new HashSet<>();
	protected String foundsetSelector;
	protected IDataAdapterList dataAdapterList;
	protected String propertyName;

	protected FoundsetTypeChangeMonitor changeMonitor;
	protected FoundsetPropertySelectionListener listSelectionListener;

	public FoundsetTypeSabloValue(Object designJSONValue, String propertyName)
	{
		this.designJSONValue = designJSONValue;
		this.propertyName = propertyName;

		changeMonitor = new FoundsetTypeChangeMonitor(this);
		viewPort = new FoundsetTypeViewport(changeMonitor);
		// nothing to do here; foundset is not initialized until it's attached to a component
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, BaseWebObject webObject)
	{
		this.webObject = webObject;
		dataAdapterList = null;
		changeMonitor.setChangeNotifier(changeNotifier);

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
			if ("".equals(foundsetSelector))
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
				// if we want to use this type on services as well we need extra code here to get the application
				newFoundset = (IFoundSetInternal)((WebComponent)webObject).findParent(IWebFormUI.class).getDataConverterContext().getApplication().getFoundSetManager().getFoundSet(
					foundsetSelector);
			}
			catch (ServoyException e)
			{
				if (record != null) Debug.trace(e);
			}
		}

		if (newFoundset != foundset)
		{
			if (foundset instanceof ISwingFoundSet) ((ISwingFoundSet)foundset).getSelectionModel().removeListSelectionListener(getListSelectionListener());
			foundset = newFoundset;
			viewPort.setFoundset(foundset);
			changeMonitor.newFoundsetInstance();
			if (foundset instanceof ISwingFoundSet) ((ISwingFoundSet)foundset).getSelectionModel().addListSelectionListener(getListSelectionListener());

			return true;
		}
		return false;
	}

	protected FoundsetPropertySelectionListener getListSelectionListener()
	{
		if (listSelectionListener == null)
		{
			listSelectionListener = new FoundsetPropertySelectionListener(changeMonitor);
		}
		return listSelectionListener;
	}

	@Override
	public boolean pushRecord(IRecordInternal record)
	{
		return updateFoundset(record);
	}

	@Override
	public void detach()
	{
		viewPort.dispose();
		if (foundset instanceof ISwingFoundSet) ((ISwingFoundSet)foundset).getSelectionModel().removeListSelectionListener(getListSelectionListener());
	}

	public JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO conversion markers should never be null I think, but it did happen (due to JSONUtils.toJSONValue(JSONWriter writer, Object value, IForJsonConverter forJsonConverter, ConversionLocation toDestinationType); will create a case for that
		if (conversionMarkers != null) conversionMarkers.convert(FoundsetPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

		destinationJSON.object();
		destinationJSON.key(SERVER_SIZE).value(foundset != null ? foundset.getSize() : 0);
		destinationJSON.key(SELECTED_ROW_INDEXES);
		addSelectedIndexes(destinationJSON);
		destinationJSON.key(MULTI_SELECT).value(foundset != null ? foundset.isMultiSelect() : false); // TODO listener and granular changes for this as well?

		// viewPort
		destinationJSON.key(VIEW_PORT);
		addViewPort(destinationJSON);
		// end viewPort

		destinationJSON.endObject();
		changeMonitor.clearChanges();
		return destinationJSON;
	}

	protected void addViewPort(JSONWriter destinationJSON) throws JSONException
	{
		destinationJSON.object();
		addViewPortBounds(destinationJSON);
//		rows: [
//	         	{ _svyRowId: 'someRowIdHASH1', nameColumn: "Bubu" },
//	         	{ _svyRowId: 'someRowIdHASH2', nameColumn: "Yogy" },
//				(...)
//	    ]
		if (foundset != null)
		{
			Map<String, Object> rows = new HashMap<>();
			PropertyDescription rowTypes = null;
			Map<String, Object>[] rowsArray = new Map[viewPort.getSize()];
			rows.put(ROWS, rowsArray);

			PropertyDescription rowArrayTypes = AggregatedPropertyType.newAggregatedProperty();
			for (int i = viewPort.getStartIndex() + viewPort.getSize() - 1; i >= viewPort.getStartIndex(); i--)
			{
				TypedData<Map<String, Object>> rowTypedData = getRowData(i);
				rowsArray[i - viewPort.getStartIndex()] = rowTypedData.content;
				if (rowTypedData.contentType != null) rowArrayTypes.putProperty(String.valueOf(i), rowTypedData.contentType);
			}

			if (rowArrayTypes.hasChildProperties())
			{
				rowTypes = AggregatedPropertyType.newAggregatedProperty();
				rowTypes.putProperty(ROWS, rowArrayTypes);
			}
			// convert for websocket traffic (for example Date objects will turn into long)
			JSONUtils.writeDataWithConversions(destinationJSON, rows, rowTypes);
		}
		else
		{
			destinationJSON.key(ROWS).array().endArray();
		}
		destinationJSON.endObject();
	}

	/**
	 * Dumps selected indexes to JSON.
	 */
	protected void addSelectedIndexes(JSONWriter destinationJSON) throws JSONException
	{
		destinationJSON.array();
		if (foundset != null)
		{
			for (int idx : foundset.getSelectedIndexes())
			{
				destinationJSON.value(idx);
			}
		}
		destinationJSON.endArray();
	}

	protected void addViewPortBounds(JSONWriter destinationJSON) throws JSONException
	{
		destinationJSON.key(START_INDEX).value(viewPort.getStartIndex()).key(SIZE).value(viewPort.getSize());

	}

	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		if (changeMonitor.shouldSendAll()) return toJSON(destinationJSON, conversionMarkers);
		else
		{
			if (conversionMarkers != null) conversionMarkers.convert(FoundsetPropertyType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets

			boolean somethingChanged = false;
			// change monitor already takes care not to report duplicates here (like whole viewport + viewport bounds)
			if (changeMonitor.shouldSendFoundsetSize())
			{
				destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + SERVER_SIZE).value(foundset != null ? foundset.getSize() : 0);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendSelectedIndexes())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + SELECTED_ROW_INDEXES);
				addSelectedIndexes(destinationJSON);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendWholeViewPort())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + VIEW_PORT);
				addViewPort(destinationJSON);
				somethingChanged = true;
			}
			else
			{
				boolean viewPortUpdateAdded = false;
				if (changeMonitor.shouldSendViewPortBounds())
				{
					if (!somethingChanged) destinationJSON.object();
					destinationJSON.key(UPDATE_PREFIX + VIEW_PORT).object();
					viewPortUpdateAdded = true;
					addViewPortBounds(destinationJSON);
					somethingChanged = true;
				}
				List<RowData> viewPortChanges = changeMonitor.getViewPortChanges();
				if (viewPortChanges.size() > 0)
				{
					if (!somethingChanged) destinationJSON.object();
					if (!viewPortUpdateAdded)
					{
						destinationJSON.key(UPDATE_PREFIX + VIEW_PORT).object();
						viewPortUpdateAdded = true;
					}
					Map<String, Object> changes = new HashMap<>();
					PropertyDescription changeTypes = null;
					Map<String, Object>[] changesArray = new Map[viewPortChanges.size()];

					changes.put(UPDATE_PREFIX + ROWS, changesArray);

					PropertyDescription changeArrayTypes = AggregatedPropertyType.newAggregatedProperty();
					for (int i = viewPortChanges.size() - 1; i >= 0; i--)
					{
						TypedData<Map<String, Object>> rowTypedData = viewPortChanges.get(i).toMap();
						changesArray[i] = rowTypedData.content;
						if (rowTypedData.contentType != null) changeArrayTypes.putProperty(String.valueOf(i), rowTypedData.contentType);
					}

					if (changeArrayTypes.hasChildProperties())
					{
						changeTypes = AggregatedPropertyType.newAggregatedProperty();
						changeTypes.putProperty(UPDATE_PREFIX + ROWS, changeArrayTypes);
					}

					// convert for websocket traffic (for example Date objects will turn into long)
					JSONUtils.writeDataWithConversions(destinationJSON, changes, changeTypes);
					somethingChanged = true;
				}
				if (viewPortUpdateAdded) destinationJSON.endObject();
			}

			if (somethingChanged) destinationJSON.endObject();
			else
			{
				// no change yet we are still asked to send changes; we could send all or just nothing useful
//				destinationJSON.key(NO_OP).value(0);
				// TODO send all for now - when the separate tagging interface for granular updates vs full updates is added we can send NO_OP again
				toJSON(destinationJSON, conversionMarkers);
			}

			changeMonitor.clearChanges();
			return destinationJSON;
		}
	}

	protected TypedData<Map<String, Object>> getRowData(int foundsetIndex)
	{
		Map<String, Object> data = new HashMap<>();
		PropertyDescription dataTypes = AggregatedPropertyType.newAggregatedProperty();

		// write viewport row contents
		IRecordInternal record = foundset.getRecord(foundsetIndex);
		data.put(ROW_ID_COL_KEY, record.getPKHashKey() + "_" + foundsetIndex); // TODO do we really need the "i"?
		IWebFormUI formUI = ((WebComponent)webObject).findParent(IWebFormUI.class);
		Iterator<String> it = dataProviders.iterator();
		while (it.hasNext())
		{
			String dataProvider = it.next();

			// TODO currently we also send globals/form variables through foundset; in the future it should be enough to get it from the record only, not through DataAdapterList.getValueObject!
			Object value = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, formUI.getController().getFormScope(), dataProvider);
			data.put(dataProvider, value);
			PropertyDescription pd = NGUtils.getDataProviderPropertyDescription(dataProvider, foundset.getTable());
			if (pd == null) pd = NGUtils.getDataProviderPropertyDescription(dataProvider,
				formUI.getDataConverterContext().getApplication().getFlattenedSolution(), formUI.getController().getForm(), foundset.getTable()); // TODO remove this when component[] properly implements it's dataproviders - when there's no need for foundset to send over globals/form variables
			if (pd != null)
			{
				dataTypes.putProperty(dataProvider, pd);
			}
		}
		if (!dataTypes.hasChildProperties()) dataTypes = null;

		return new TypedData<Map<String, Object>>(data, dataTypes);
	}

	public void browserUpdatesReceived(Object jsonValue)
	{
		if (foundset == null) return;
		try
		{
			if (jsonValue instanceof JSONArray)
			{
				JSONArray arr = (JSONArray)jsonValue;
				for (int i = 0; i < arr.length(); i++)
				{
					JSONObject update = (JSONObject)arr.get(i);
					// {newViewPort: {startIndex : startIndex, size : size}}
					if (update.has("newViewPort"))
					{
						JSONObject newViewport = update.getJSONObject("newViewPort");
						viewPort.setBounds(newViewport.getInt(START_INDEX), newViewport.getInt(SIZE));
					}
					// {loadExtraRecords: negativeOrPositiveCount}
					else if (update.has("loadExtraRecords"))
					{
						viewPort.loadExtraRecords(update.getInt("loadExtraRecords"));
					}
					// {newClientSelection: newSelectedIndexesArray}
					else if (update.has("newClientSelection"))
					{
						JSONArray jsonSelectedIndexes = update.getJSONArray("newClientSelection");
						int[] newSelectedIndexes = new int[jsonSelectedIndexes.length()];
						for (int j = newSelectedIndexes.length - 1; j >= 0; j--)
						{
							newSelectedIndexes[j] = jsonSelectedIndexes.getInt(j);
						}

						// this !Arrays.equals check in conjunction with pause()/resume() is needed to avoid an effect on the client that server always sends back changed selection in which case
						// if the user quickly changes selection multiple times and the connection is slow, selection will jump all over
						// the place until it stabilizes correctly
						getListSelectionListener().pause();
						try
						{
							foundset.setSelectedIndexes(newSelectedIndexes);
						}
						finally
						{
							getListSelectionListener().resume();
							// if server denies the new selection as invalid and doesn't change selection, send it to the client so that it doesn't keep invalid selection
							if (!Arrays.equals(foundset.getSelectedIndexes(), newSelectedIndexes))
							{
								changeMonitor.selectionChanged();
							}
						}
					}
					else if (update.has("dataChanged"))
					{
						// {dataChanged: { ROW_ID_COL_KEY: rowIDValue, dataproviderName: value }}
						JSONObject dataChangeJSON = (JSONObject)update.get("dataChanged");
						String rowIDValue = dataChangeJSON.getString(ROW_ID_COL_KEY);
						String dataProviderName = dataChangeJSON.getString("dp");
						Object value = dataChangeJSON.get("value");

						if (foundset != null)
						{
							Pair<String, Integer> splitHashAndIndex = WebGridFormUI.splitPKHashAndIndex(rowIDValue);
							int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

							if (recordIndex != -1)
							{
								IWebFormUI formUI = ((WebComponent)webObject).findParent(IWebFormUI.class); // this will no longer be needed once 'component' type handles the global/form variables
								IRecordInternal record = foundset.getRecord(recordIndex);
								// convert Dates where it's needed

								PropertyDescription dataProviderPropDesc = NGUtils.getDataProviderPropertyDescription(dataProviderName, foundset.getTable()); // this should be enough for when only foundset dataproviders are used
								if (dataProviderPropDesc == null)
								{
									dataProviderPropDesc = NGUtils.getDataProviderPropertyDescription(dataProviderName,
										formUI.getDataConverterContext().getApplication().getFlattenedSolution(), formUI.getController().getForm(),
										foundset.getTable());
								}

								value = JSONUtils.fromJSONUnwrapped(null, value, dataProviderPropDesc, null);

								viewPort.pauseRowUpdateListener(splitHashAndIndex.getLeft());
								try
								{
									if (foundset.getTable().getColumnType(dataProviderName) != 0)
									{
										record.startEditing(); // we could have used here JS put but that method is not in the interface
										record.setValue(dataProviderName, value);
									}
									else
									{
										// TODO currently we also send globals/form variables through foundset;
										// in the future it should be enough to set it in the record only!
										// not through DataAdapterList
										com.servoy.j2db.dataprocessing.DataAdapterList.setValueObject(record, formUI.getController().getFormScope(),
											dataProviderName, value);
									}
								}
								finally
								{
									viewPort.resumeRowUpdateListener();
									// if server denies the new selection as invalid and doesn't change selection, send it to the client so that it doesn't keep invalid selection
									// TODO use here record directly instead of dataAdapterList when we no longer work with variables, just foundset data in this property (when that is implemented in components property)
									if (!Utils.equalObjects(com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record,
										formUI.getController().getFormScope(), dataProviderName), value))
									{
										changeMonitor.recordsUpdated(recordIndex, recordIndex, foundset.getSize(), viewPort);
									}
								}
							}
							else
							{
								Debug.error("Cannot set foundset record (" + rowIDValue + ") dataprovider '" + dataProviderName + "' to value '" + value +
									". Record not found.");
							}
						}
					}
				}
			}
		}
		catch (JSONException e)
		{
			Debug.error("Error when getting browser updates for property (" + this.toString() + ")", e);
		}
	}

	/**
	 * When this foundset is used in combination with child "components" properties, those properties will need
	 * a dataAdapterList that is being fed records from this foundset.
	 */
	public IDataAdapterList getDataAdapterList()
	{
		// TODO remove this or replace it with something else that can feed records to component properties
		// this method gets called by linked component type property/properties
		if (dataAdapterList == null)
		{
			dataAdapterList = new DataAdapterList(((WebComponent)webObject).findParent(IWebFormUI.class).getController());
		}
		return dataAdapterList;
	}

	/**
	 * Register a list of dataproviders that is needed client-side.
	 * @param dataProvidersToSend a list of dataproviders that will be sent to the browser as part of the foundset property's viewport data.
	 */
	public void includeDataProviders(Set<String> dataProvidersToSend)
	{
		if (dataProviders.addAll(dataProvidersToSend)) changeMonitor.dataProvidersChanged();
	}

	public boolean setEditingRowByPkHash(String pkHashAndIndex)
	{
		return WebGridFormUI.setEditingRowByPkHash(foundset, pkHashAndIndex);
	}

	@Override
	public String toString()
	{
		return "'" + propertyName + "' foundset type property on component " + (webObject != null ? webObject.getName() : "- not yet attached -");
	}

}
