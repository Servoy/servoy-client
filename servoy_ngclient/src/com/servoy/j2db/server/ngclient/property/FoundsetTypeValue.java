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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebGridFormUI.RowData;
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
	protected WebFormComponent component;
	protected Set<String> dataProviders = new HashSet<>();
	protected String foundsetSelector;
	protected IDataAdapterList dataAdapterList;
	protected String propertyName;

	protected FoundsetTypeChangeMonitor changeMonitor;
	protected ListSelectionListener listSelectionListener;

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
		changeMonitor = new FoundsetTypeChangeMonitor(this);
		viewPort = new FoundsetTypeViewport(changeMonitor);
	}

	@Override
	public void attachToComponent(IChangeListener changeNotifier, WebComponent component)
	{
		this.component = (WebFormComponent)component;
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
			if (foundset instanceof ISwingFoundSet) ((ISwingFoundSet)foundset).getSelectionModel().removeListSelectionListener(getListSelectionListener());
			foundset = newFoundset;
			viewPort.setFoundset(foundset);
			changeMonitor.newFoundsetInstance();
			if (foundset instanceof ISwingFoundSet) ((ISwingFoundSet)foundset).getSelectionModel().addListSelectionListener(getListSelectionListener());

			return true;
		}
		return false;
	}

	protected ListSelectionListener getListSelectionListener()
	{
		if (listSelectionListener == null)
		{
			listSelectionListener = new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					if (!e.getValueIsAdjusting())
					{
						changeMonitor.selectionChanged();
					}
				}
			};
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

	@Override
	public JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		// TODO conversion markers should never be null I think, but it did happen (due to JSONUtils.toJSONValue(JSONWriter writer, Object value, IForJsonConverter forJsonConverter, ConversionLocation toDestinationType); will create a case for that
		if (conversionMarkers != null) conversionMarkers.convert(FoundsetTypeImpl.TYPE_ID); // so that the client knows it must use the custom client side JS for what JSON it gets

		destinationJSON.object();
		destinationJSON.key(SERVER_SIZE).value(foundset != null ? foundset.getSize() : 0);
		destinationJSON.key(SELECTED_ROW_INDEXES);
		addSelectedIndexes(destinationJSON);
		destinationJSON.key(MULTI_SELECT).value(foundset != null ? foundset.isMultiSelect() : false); // TODO listener and granular changes for this as well?

		// viewPort
		destinationJSON.key(VIEW_PORT);
		addViewPort(destinationJSON, false);
		// end viewPort

		destinationJSON.endObject();
		changeMonitor.clearChanges();
		return destinationJSON;
	}

	protected void addViewPort(JSONWriter destinationJSON, boolean update) throws JSONException
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
			Map<String, Object>[] rowsArray = new Map[viewPort.getSize()];
			rows.put(ROWS, rowsArray);

			for (int i = viewPort.getStartIndex() + viewPort.getSize() - 1; i >= viewPort.getStartIndex(); i--)
			{
				rowsArray[i - viewPort.getStartIndex()] = getRowData(i);
			}

			// convert for websocket traffic (for example Date objects will turn into long)
			JSONUtils.writeDataWithConversions(destinationJSON, rows,
				getFormUI().getDataConverterContext().getApplication().getWebsocketSession().getForJsonConverter(), update ? ConversionLocation.BROWSER_UPDATE
					: ConversionLocation.BROWSER);
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

	@Override
	public JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException
	{
		if (changeMonitor.shouldSendAll()) return toJSON(destinationJSON, conversionMarkers);
		else
		{
			if (conversionMarkers != null) conversionMarkers.convert(FoundsetTypeImpl.TYPE_ID); // so that the client knows it must use the custom client side JS for what JSON it gets

			boolean somethingChanged = false;
			destinationJSON.object();
			// change monitor already takes care not to report duplicates here (like whole viewport + viewport bounds)
			if (changeMonitor.shouldSendFoundsetSize())
			{
				destinationJSON.key(UPDATE_PREFIX + SERVER_SIZE).value(foundset != null ? foundset.getSize() : 0);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendSelectedIndexes())
			{
				destinationJSON.key(UPDATE_PREFIX + SELECTED_ROW_INDEXES);
				addSelectedIndexes(destinationJSON);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendWholeViewPort())
			{
				destinationJSON.key(UPDATE_PREFIX + VIEW_PORT);
				addViewPort(destinationJSON, true);
				somethingChanged = true;
			}
			else
			{
				boolean viewPortUpdateAdded = false;
				if (changeMonitor.shouldSendViewPortBounds())
				{
					destinationJSON.key(UPDATE_PREFIX + VIEW_PORT).object();
					viewPortUpdateAdded = true;
					addViewPortBounds(destinationJSON);
					somethingChanged = true;
				}
				List<RowData> viewPortChanges = changeMonitor.getViewPortChanges();
				if (viewPortChanges.size() > 0)
				{
					if (!viewPortUpdateAdded)
					{
						destinationJSON.key(UPDATE_PREFIX + VIEW_PORT).object();
						viewPortUpdateAdded = true;
					}
					Map<String, Object> changes = new HashMap<>();
					Map<String, Object>[] changesArray = new Map[viewPortChanges.size()];
					changes.put(UPDATE_PREFIX + ROWS, changesArray);

					for (int i = viewPortChanges.size() - 1; i >= 0; i--)
					{
						changesArray[i] = viewPortChanges.get(i).toMap();
					}

					// convert for websocket traffic (for example Date objects will turn into long)
					JSONUtils.writeDataWithConversions(destinationJSON, changes,
						getFormUI().getDataConverterContext().getApplication().getWebsocketSession().getForJsonConverter(), ConversionLocation.BROWSER_UPDATE);
					somethingChanged = true;
				}
				if (viewPortUpdateAdded) destinationJSON.endObject();
			}

			if (!somethingChanged)
			{
				// no change yet we are still asked to send changes; we could send all or just nothing useful
				destinationJSON.key(NO_OP).value(0);
			}

			destinationJSON.endObject();
			changeMonitor.clearChanges();
			return destinationJSON;
		}
	}

	protected Map<String, Object> getRowData(int foundsetIndex)
	{
		Map<String, Object> data = new HashMap<>();
		// write viewport row contents
		IRecordInternal record = foundset.getRecord(foundsetIndex);
		data.put("_svyRowId", record.getPKHashKey() + "_" + foundsetIndex); // TODO do we really need the "i"?

		Iterator<String> it = dataProviders.iterator();
		while (it.hasNext())
		{
			String dataProvider = it.next();

			// TODO currently we also send globals/form variables through foundset; in the future it should be enough to get it from the record only, not through DataAdapterList.getValueObject!
			Object value = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, getFormUI().getController().getFormScope(), dataProvider);
			data.put(dataProvider, value);
		}
		return data;
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
					viewPort.setBounds(newViewport.getInt(START_INDEX), newViewport.getInt(SIZE));
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
		if (dataProviders.addAll(dataProvidersToSend)) changeMonitor.dataProvidersChanged();
	}

	@Override
	public String toString()
	{
		return "'" + propertyName + "' foundset type property on component " + (component != null ? component.getName() : "- not yet attached -");
	}

}
