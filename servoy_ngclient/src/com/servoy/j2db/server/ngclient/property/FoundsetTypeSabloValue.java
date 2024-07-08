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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.IllegalChangeFromClientException;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.types.EnabledPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.ServoyClientService;
import com.servoy.j2db.server.ngclient.property.ChainedRelatedFoundsetSelectionMonitor.IRelatedFoundsetChainSelectionChangeListener;
import com.servoy.j2db.server.ngclient.property.types.FormatPropertyType;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.utils.NGUtils;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;

/**
 * Value used at runtime as foundset type value proxy for multiple interested parties (browser, designtime, scripting).
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FoundsetTypeSabloValue implements IDataLinkedPropertyValue, TableModelListener, IHasUnderlyingState
{

	public static final String FORM_FOUNDSET_SELECTOR = "";

	protected static final Logger log = LoggerFactory.getLogger(FoundsetPropertyType.class.getCanonicalName());

	/**
	 * Column that is always automatically sent for each record in a foundset's viewport. It's value
	 * uniquely identifies that record.
	 */
	public static final String ROW_ID_COL_KEY = "_svyRowId";

	/**
	 * This key/column should be stored as $foundsetTypeConstants.ROW_ID_COL_KEY in the actual client side row, but this key is sent from server when the foundset property is sending
	 * just a partial update, but some of the columns that did change are also pks so they do affect the pk hash; client uses this to distiguish between a full
	 * update of a row and a partial update of a row; so if update has $foundsetTypeConstants.ROW_ID_COL_KEY it will consider it to be a full update,
	 * and if it has either ROW_ID_COL_KEY_PARTIAL_UPDATE or no rowID then it is a partial update of a row (only some of the columns in that row have changed).
	 */
	public static final String ROW_ID_COL_KEY_PARTIAL_UPDATE = "_svyRowId_p";


	public static final String DATAPROVIDER_KEY = "dp";
	public static final String VALUE_KEY = "value";

	// START keys and values used in JSON
	public static final String UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them

	public static final String SERVER_SIZE = "serverSize";
	public static final String SORT = "sortColumns";
	public static final String SELECTED_ROW_INDEXES = "selectedRowIndexes";
	public static final String FOUNDSET_ID = "foundsetId";
	public static final String USER_SET_SELECTION = "userSetSelection";
	public static final String FOUNDSET_DEFINITION = "foundsetDefinition";

	public static final String HANDLED_CLIENT_REQUESTS = "handledClientReqIds";
	public static final String ID_KEY = "id";

	public static final String MULTI_SELECT = "multiSelect";
	public static final String FIND_MODE = "findMode";
	public static final String VIEW_PORT = "viewPort";
	public static final String COLUMN_FORMATS = "columnFormats";
	public static final String HAS_MORE_ROWS = "hasMoreRows";
	public static final String START_INDEX = "startIndex";
	public static final String SIZE = "size";
	public static final String PREFERRED_VIEWPORT_SIZE = "preferredViewportSize";
	public static final String INITIAL_SELECTION_VIEWPORT_CENTERED = "initialSelectionViewportCentered";
	public static final String ROWS = "rows";
	public static final String NO_OP = "n";

	// END keys and values used in JSON

	protected FoundsetTypeViewport viewPort;
	private IFoundSetInternal foundset;
	protected final Object designJSONValue;

	protected Map<String, String> dataproviders = new HashMap<>();
	protected Map<String, ComponentFormat> columnFormats;

	protected String foundsetSelector;
	protected FoundsetDataAdapterList dataAdapterList;
	protected String propertyName;

	protected FoundsetTypeChangeMonitor changeMonitor;
	protected FoundsetPropertySelectionListener listSelectionListener;
	protected ChainedRelatedFoundsetSelectionMonitor chainedRelatedFoundsetSelectionMonitor;

	protected FoundsetTypeRowDataProvider rowDataProvider;

	// child components can be foundset linked (forFoundset: ...); in this case foundset prop. API can sort by child component name if it's told
	// which child component maps to which foundset column... the same goes for dataprovider properties linked to foundsets for example
	protected final Map<String, String> recordDataLinkedPropertyIDToColumnDP;

	protected final DataAdapterList parentDAL;

	protected IWebObjectContext webObjectContext;
	private final List<IChangeListener> underlyingStateListeners = new ArrayList<>();

	protected final FoundsetPropertyTypeConfig specConfig;
	private String lastSortString;

	public FoundsetTypeSabloValue(Object designJSONValue, String propertyName, DataAdapterList parentDAL, FoundsetPropertyTypeConfig specConfig)
	{
		this.designJSONValue = designJSONValue;
		this.propertyName = propertyName;
		this.parentDAL = parentDAL;
		this.specConfig = specConfig;

		rowDataProvider = new FoundsetTypeRowDataProvider(this);
		changeMonitor = new FoundsetTypeChangeMonitor(this, rowDataProvider);
		viewPort = new FoundsetTypeViewport(changeMonitor, specConfig, log);
		// nothing to do here; foundset is not initialized until it's attached to a component
		recordDataLinkedPropertyIDToColumnDP = new HashMap<String, String>();
		// foundsetSelector as defined in component design json
		if (designJSONValue != null)
		{
			// IMPORTANT if any changes are made in the way we store the form element json (designJSONValue) please change code in FormatPropertyType that uses it as well!
			foundsetSelector = ((JSONObject)designJSONValue).optString(FoundsetPropertyType.FOUNDSET_SELECTOR, null);
			initializeDataproviders(((JSONObject)designJSONValue).optJSONObject(FoundsetPropertyType.DATAPROVIDERS_KEY_FOR_DESIGN));
		}
	}

	public void initializeDataproviders(JSONObject dataProvidersJSON)
	{
		if (dataProvidersJSON != null)
		{
			Iterator keys = dataProvidersJSON.keys();
			if (keys.hasNext()) dataproviders.clear();
			while (keys.hasNext())
			{
				String key = (String)keys.next();
				dataproviders.put(key, ServoyJSONObject.optString(key, dataProvidersJSON, null));
			}
		}
	}

	protected void notifyDataProvidersUpdated()
	{
		refreshColumnFormats();

		if (getFoundset() != null)
		{
			if (viewPort.getSize() > 0) changeMonitor.viewPortCompletelyChanged();
		}
	}

	protected void refreshColumnFormats()
	{
		boolean formatsChanged = (columnFormats != null);
		columnFormats = null;
		formatsChanged = updateColumnFormatsIfNeeded() || formatsChanged;
		if (formatsChanged) changeMonitor.columnFormatsUpdated();
	}

	public FoundsetTypeViewport getViewPort()
	{
		return viewPort;
	}

	public IFoundSetInternal getFoundset()
	{
		return foundset;
	}

	public String getFoundsetSelector()
	{
		return foundsetSelector;
	}

	protected boolean isOneOfTheFollowingAPk(Set<String> columnNames)
	{
		if (columnNames == null) return false;

		if (foundset != null && foundset.getSQLSheet() != null)
		{
			String[] pkIDs = foundset.getSQLSheet().getPKColumnDataProvidersAsArray();
			for (String pkID : pkIDs)
				if (columnNames.contains(pkID)) return true;
		}
		return false;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, IWebObjectContext webObjectCntxt)
	{
		this.webObjectContext = webObjectCntxt;
		dataAdapterList = null;
		changeMonitor.setChangeNotifier(changeNotifier);

		// get the foundset identifier, then the foundset itself
//		foundset: {
//			foundsetSelector: 'string',
//			dataProviders: 'dataprovider[]'
//		}
		updateFoundset((IRecordInternal)null);

		// register parent record changed listener
		if (parentDAL != null)
		{
			TargetDataLinks dataLinks = TargetDataLinks.LINKED_TO_ALL;
			if (foundsetSelector != null && !FORM_FOUNDSET_SELECTOR.equals(foundsetSelector) && !DataSourceUtils.isDatasourceUri(foundsetSelector))
			{
				// it is a relation then, not a datasource (separate or named foundset)
				int lastIndex = foundsetSelector.lastIndexOf('.');
				if (lastIndex > 0)
				{
					// if this is a nested relation the parent dal needs to know this. so it can monitor the parent relations.
					Relation[] relations = getApplication().getFlattenedSolution().getRelationSequence(foundsetSelector.substring(0, lastIndex));
					if (relations != null && relations.length > 0)
					{
						dataLinks = new TargetDataLinks(null, true, relations);
					}
				}
			}
			parentDAL.addDataLinkedProperty(this, dataLinks);
		}

		fireUnderlyingStateChangedListeners(); // we now have a webObjectContext so getDataAdapterList() might return non-null now; in some cases this is all other properties need, they don't need the foundset itself
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
	protected void updateFoundset(IRecordInternal record)
	{
		// IMPORTANT if the following code regarding foundsetSelector is changed please update code in getTableBasedOfFoundsetPropertyFromFoundsetIdentifier(...) below as well
		if (foundsetSelector == null) return; // this foundset is only meant to be set from Rhino scripting; do not automatically set it and do not automatically clear it once it's set from Rhino

		IFoundSetInternal newFoundset = null;

		if (FORM_FOUNDSET_SELECTOR.equals(foundsetSelector))
		{
			// it is the form's foundset then
			if (record != null)
			{
				newFoundset = record.getParentFoundSet();
			}
			else if (parentDAL != null && parentDAL.getForm() != null)
			{
				// when formUI is initialized / attachToBaseObject() is called,
				// the formModel (foundset) is not yet initialized in the WebFormController
				// so it will return null, but later, after form manager calls init() on it
				// it will have it; this attempt to get it from the form directly is only for access from Rhino to this foundset
				// property's "foundset" before the form is visible, so before dataProviderOrRecordChanged()
				// is called with an actual record from which we can get the foundset
				newFoundset = parentDAL.getForm().getFoundSet();
			}
		}
		else if (!DataSourceUtils.isDatasourceUri(foundsetSelector))
		{
			// it is a relation then or a shared named foundset (set somewhere on a form in designer)
			if (record != null)
			{
				Object o = record.getValue(foundsetSelector);
				if (o instanceof IFoundSetInternal)
				{
					// it is a related foundset then if we were able to get it from current record
					newFoundset = (IFoundSetInternal)o;
					if (chainedRelatedFoundsetSelectionMonitor == null)
					{
						chainedRelatedFoundsetSelectionMonitor = new ChainedRelatedFoundsetSelectionMonitor(new IRelatedFoundsetChainSelectionChangeListener()
						{
							@Override
							public void selectionChanged(IRecordInternal rootRecord, String nestedRelationNames)
							{
								updateFoundset(rootRecord);
							}
						});
					}
					chainedRelatedFoundsetSelectionMonitor.update(newFoundset, record, foundsetSelector);
				}
				else if (parentDAL == null || parentDAL.getApplication().getFlattenedSolution().getRelationSequence(foundsetSelector) == null)
				{
					// if it is not a related foundset it must be a shared/named foundset
					try
					{
						newFoundset = (IFoundSetInternal)getFoundSetManager().getNamedFoundSet(foundsetSelector);
					}
					catch (ServoyException e)
					{
						Debug.error(e);
					}
				}
			}
		}
		else // DataSourceUtils.isDatasourceUri(foundsetSelector)
		{
			// if this is a separate foundset selector; don't replace the foundset which is already inside it because
			// that will only reinitialize constantly this FoundsetType/Table with a new foundset - on every dataprovider change.
			if (foundset != null) newFoundset = foundset;
			else
			{
				try
				{
					// if we want to use this type on services as well we need extra code here to get the application
					newFoundset = (IFoundSetInternal)getFoundSetManager().getFoundSet(foundsetSelector);
					if (((JSONObject)designJSONValue).optBoolean(FoundsetPropertyType.LOAD_ALL_RECORDS_FOR_SEPARATE, false))
					{
						newFoundset.loadAllRecords();
					}
				}
				catch (ServoyException e)
				{
					if (record != null && !(record instanceof PrototypeState)) Debug.error(e);
				}
			}
		}
		updateFoundset(newFoundset);
	}

	public static ITable getTableBasedOfFoundsetPropertyFromFoundsetIdentifier(String foundsetId, INGApplication application, Form form)
	{
		// IMPORTANT if the following code regarding foundsetSelector is changed please update code in updateFoundset(IRecordInternal record) above as well
		ITable table = null;

		if (FoundsetTypeSabloValue.FORM_FOUNDSET_SELECTOR.equals(foundsetId))
		{
			// it is the form's foundset then
			IFoundSetManagerInternal foundSetManager = application.getFoundSetManager();
			if (foundSetManager == null)
			{
				table = application.getFlattenedSolution().getTable(form.getDataSource());
			}
			else
			{
				try
				{
					table = foundSetManager.getTable(form.getDataSource());
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
			}
		}
		else if (!DataSourceUtils.isDatasourceUri(foundsetId))
		{
			// it is a relation then, not a datasource (separate or named foundset)
			Relation[] relations = application.getFlattenedSolution().getRelationSequence(foundsetId);
			if (relations != null && relations.length > 0)
			{
				table = application.getFlattenedSolution().getTable(relations[relations.length - 1].getForeignDataSource());
			}
		}
		else // DataSourceUtils.isDatasourceUri(foundsetName)
		{
			// if this is a separate or named foundset selector or it is a foundset value that was set from Rhino
			table = application.getFlattenedSolution().getTable(foundsetId);
		}
		return table;
	}

	protected IFoundSetManagerInternal getFoundSetManager()
	{
		return getApplication().getFoundSetManager();
	}

	protected INGApplication getApplication()
	{
		if (webObjectContext.getUnderlyingWebObject() instanceof WebComponent) return getFormUI().getDataConverterContext().getApplication();
		else if (webObjectContext.getUnderlyingWebObject() instanceof ServoyClientService)
		{
			// foundset type is not designed to work in services but currently it is used by old foundset manager service for dbtreeview component;
			// in new components, a combination of foundset, foundset ref/record types & server-side component scripting allow components to get as many foundsets as they need, so
			// there should be no more need for the foundset manager service
			return ((ServoyClientService)webObjectContext.getUnderlyingWebObject()).getDataConverterContext().getApplication();
		}
		return null;
	}

	public void updateFoundset(IFoundSetInternal newFoundset)
	{
		if (newFoundset != null && newFoundset.getDataSource() == null) newFoundset = null;
		if (newFoundset != foundset)
		{
			int oldServerSize = (foundset != null ? foundset.getSize() : 0);
			int newServerSize = (newFoundset != null ? newFoundset.getSize() : 0);
			boolean oldMultiselect = (foundset != null ? foundset.isMultiSelect() : false);
			boolean newMultiselect = (newFoundset != null ? newFoundset.isMultiSelect() : false);
			boolean oldFindMode = (foundset != null ? foundset.isInFindMode() : false);
			boolean newFindMode = (newFoundset != null ? newFoundset.isInFindMode() : false);

			if (foundset instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)foundset).getSelectionModel().removeListSelectionListener(getListSelectionListener());
				((ISwingFoundSet)foundset).removeTableModelListener(this);
			}
			foundset = newFoundset;
			viewPort.setFoundset(foundset);
			if (oldServerSize != newServerSize) changeMonitor.newFoundsetSize();
			changeMonitor.selectionChanged(false);
			changeMonitor.checkHadMoreRows();
			if (oldMultiselect != newMultiselect) changeMonitor.multiSelectChanged();
			if (oldFindMode != newFindMode) changeMonitor.findModeChanged(newFindMode);
			if (updateColumnFormatsIfNeeded()) changeMonitor.columnFormatsUpdated();
			changeMonitor.foundsetIDChanged();

			if (foundset instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)foundset).getSelectionModel().addListSelectionListener(getListSelectionListener());
				((ISwingFoundSet)foundset).addTableModelListener(this);
			}
			if (chainedRelatedFoundsetSelectionMonitor != null && chainedRelatedFoundsetSelectionMonitor.getRelatedFoundset() != foundset)
			{
				// maybe a set of the foundset came from Rhino - in which case we no longer need to monitor selection changes in the chain of related foundsets
				chainedRelatedFoundsetSelectionMonitor.unregisterListeners();
				chainedRelatedFoundsetSelectionMonitor = null;
			}

			FoundsetDataAdapterList fsDAL = getDataAdapterList();
			if (fsDAL != null) fsDAL.setRecordQuietly(null, true); // avoid the DAL listening to changes in obsolete Records from the previous foundset
			if (foundset != null && fsDAL != null) fsDAL.setFindMode(foundset.isInFindMode());

			fireUnderlyingStateChangedListeners(); // some listening properties might be interested in the new underlying foundset itself
		}
	}

	public void setDataAdapterListToSelectedRecord()
	{
		// we want to keep the foundset DAL always on the selected record - that way updates to related dataproviders in foundset linked components or foundset linked properties
		// that use this DAL are seen in the UI
		// TODO make related DP updates also work with non-selected records in those cases...

		if (dataAdapterList != null)
		{
			if (foundset != null && foundset.getSize() > 0)
			{
				IRecord selectedRecord = foundset.getRecord(foundset.getSelectedIndex());
				dataAdapterList.setRecordQuietly(selectedRecord, true);
			}
			else
			{
				dataAdapterList.setRecordQuietly(null, true); // make sure DAL is not listening to records that are no longer there in the foundset
			}
		}
	}

	protected boolean updateColumnFormatsIfNeeded()
	{
		if (specConfig.sendDefaultFormats && columnFormats == null && getFoundset() != null && webObjectContext != null)
		{
			columnFormats = new HashMap<>();
			for (Entry<String, String> dp : dataproviders.entrySet())
			{
				columnFormats.put(dp.getKey(),
					ComponentFormat.getComponentFormat(null, ((Table)getFoundset().getTable()).getColumn(dp.getValue()), getApplication(), true));
			}

			return true;
		}
		return false;
	}

	protected FoundsetPropertySelectionListener getListSelectionListener()
	{
		if (listSelectionListener == null)
		{
			listSelectionListener = new FoundsetPropertySelectionListener(changeMonitor, viewPort);
		}
		return listSelectionListener;
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		updateFoundset(record);
	}

	@Override
	public void detach()
	{
		viewPort.dispose();
		if (foundset instanceof ISwingFoundSet)
		{
			((ISwingFoundSet)foundset).getSelectionModel().removeListSelectionListener(getListSelectionListener());
			((ISwingFoundSet)foundset).removeTableModelListener(this);
		}
		if (chainedRelatedFoundsetSelectionMonitor != null)
		{
			chainedRelatedFoundsetSelectionMonitor.unregisterListeners();
			chainedRelatedFoundsetSelectionMonitor = null;
		}
	}

	public JSONWriter toJSON(JSONWriter destinationJSON, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		rowDataProvider.initializeIfNeeded(dataConverterContext);

		destinationJSON.object();

		PushToServerEnum pushToServer = BrowserConverterContext.getPushToServerValue(dataConverterContext);

		destinationJSON.key(SERVER_SIZE).value(getFoundset() != null ? getFoundset().getSize() : 0);
		if (getFoundset() != null) destinationJSON.key(FOUNDSET_ID).value(getFoundset().getID());
		destinationJSON.key(SORT).value(getSortStringAsNames());
		destinationJSON.key(SELECTED_ROW_INDEXES);
		addSelectedIndexes(destinationJSON);
		destinationJSON.key(MULTI_SELECT).value(getFoundset() != null ? getFoundset().isMultiSelect() : false);
		destinationJSON.key(FIND_MODE).value(getFoundset() != null ? getFoundset().isInFindMode() : false);
		destinationJSON.key(HAS_MORE_ROWS).value(getFoundset() != null ? getFoundset().hadMoreRows() : false);

		writeColumnFormatsIfNeededAndAvailable(destinationJSON, dataConverterContext, false);

		addHandledClientRequestIdsIfNeeded(destinationJSON, true);

		// viewPort
		destinationJSON.key(VIEW_PORT);
		changeMonitor.clearChanges();
		addViewPort(destinationJSON);
		// end viewPort

		destinationJSON.endObject();
		return destinationJSON;
	}

	protected void writeColumnFormatsIfNeededAndAvailable(JSONWriter destinationJSON, IBrowserConverterContext dataConverterContext, boolean update)
		throws JSONException
	{
		if (specConfig.sendDefaultFormats)
		{
			destinationJSON.key((update ? UPDATE_PREFIX : "") + COLUMN_FORMATS).object();
			if (columnFormats != null)
			{
				FormatPropertyType formatPropertyType = (FormatPropertyType)TypesRegistry.getType(FormatPropertyType.TYPE_NAME); // just get it nicely in case it's overridden in designer for example

				for (Entry<String, ComponentFormat> columnFormat : columnFormats.entrySet())
				{
					formatPropertyType.writeComponentFormatToJSON(destinationJSON, columnFormat.getKey(), columnFormat.getValue(), dataConverterContext);
				}
			} // else just an empty object if fine (but we do write it because when changing dataproviders from scripting it could change from something to null and the client should know about it)
			destinationJSON.endObject();
		}
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
		if (getFoundset() != null)
		{

			destinationJSON.key(ROWS);
			ViewportClientSideTypes clientSideTypesForViewport = rowDataProvider.writeRowData(viewPort.getStartIndex(),
				viewPort.getStartIndex() + viewPort.getSize() - 1, getFoundset(), destinationJSON);

			// conversion info for websocket traffic (for example Date objects will turn into long or String to be usable in JSON and client-side needs to know about this)
			if (clientSideTypesForViewport != null) clientSideTypesForViewport.writeClientSideTypes(destinationJSON, JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY);
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
		if (getFoundset() != null)
		{
			for (int idx : getFoundset().getSelectedIndexes())
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

	public JSONWriter changesToJSON(JSONWriter destinationJSON, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (changeMonitor.shouldSendAll()) return toJSON(destinationJSON, dataConverterContext);
		else
		{
			rowDataProvider.initializeIfNeeded(dataConverterContext);

			boolean somethingChanged = false;
			// change monitor already takes care not to report duplicates here (like whole viewport + viewport bounds)
			if (changeMonitor.shouldSendFoundsetSize())
			{
				destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + SERVER_SIZE).value(getFoundset() != null ? getFoundset().getSize() : 0);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendFoundsetID())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + FOUNDSET_ID).value(getFoundset() != null ? getFoundset().getID() : 0);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendFoundsetDefinitionChange())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + FOUNDSET_DEFINITION).value(true);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendFoundsetSort())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + SORT).value(getSortStringAsNames());
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendHadMoreRows())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + HAS_MORE_ROWS).value(getFoundset() != null ? getFoundset().hadMoreRows() : false);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendMultiSelect())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + MULTI_SELECT).value(getFoundset() != null ? getFoundset().isMultiSelect() : false);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendSelectedIndexes())
			{
				if (!somethingChanged) destinationJSON.object();
				destinationJSON.key(UPDATE_PREFIX + SELECTED_ROW_INDEXES);
				addSelectedIndexes(destinationJSON);
				if (changeMonitor.shouldSendUserSetSelection())
				{
					destinationJSON.key(UPDATE_PREFIX + USER_SET_SELECTION).value(true);
				}
				somethingChanged = true;
			}
			somethingChanged = addHandledClientRequestIdsIfNeeded(destinationJSON, somethingChanged);
			if (changeMonitor.shouldSendColumnFormats())
			{
				if (!somethingChanged) destinationJSON.object();
				writeColumnFormatsIfNeededAndAvailable(destinationJSON, dataConverterContext, true);
				somethingChanged = true;
			}
			if (changeMonitor.shouldSendWholeViewPort())
			{
				changeMonitor.clearChanges();
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

				if (changeMonitor.hasViewportChanges())
				{
					ArrayOperation[] viewPortChanges = changeMonitor.getViewPortChanges();
					changeMonitor.clearChanges();
					if (!somethingChanged) destinationJSON.object();
					if (!viewPortUpdateAdded)
					{
						destinationJSON.key(UPDATE_PREFIX + VIEW_PORT).object();
						viewPortUpdateAdded = true;
					}

					destinationJSON.key(UPDATE_PREFIX + ROWS).array();

					for (ArrayOperation viewPortChange : viewPortChanges)
					{
						FoundsetPropertyType.writeViewportOperationToJSON(viewPortChange, rowDataProvider, foundset, viewPort.getStartIndex(),
							destinationJSON, null, null);
					}
					destinationJSON.endArray();

					somethingChanged = true;
				}
				else changeMonitor.clearChanges(); // changes have to be cleared anyway

				if (viewPortUpdateAdded) destinationJSON.endObject();
			}

			if (somethingChanged) destinationJSON.endObject();
			else
			{
				// no change yet we are still asked to send changes (so not full value); send a dummy NO_OP
				destinationJSON.object().key(NO_OP).value(true).endObject();
			}

			return destinationJSON;
		}
	}

	private boolean addHandledClientRequestIdsIfNeeded(JSONWriter destinationJSON, boolean somethingChanged)
	{
		List<Pair<Integer, Boolean>> handledClientRequests = changeMonitor.getHandledRequestIds();
		if (handledClientRequests.size() > 0)
		{
			if (!somethingChanged) destinationJSON.object();
			destinationJSON.key(HANDLED_CLIENT_REQUESTS).array();
			for (Pair<Integer, Boolean> x : handledClientRequests)
			{
				destinationJSON.object().key(ID_KEY).value(x.getLeft().intValue()).key(VALUE_KEY).value(x.getRight().booleanValue()).endObject();
			}
			destinationJSON.endArray();
			somethingChanged = true;
		}
		return somethingChanged;
	}

	protected String getClientIDForColumnName(String columnName, boolean searchInRecordDataLinkedPropertyIDsAsWell)
	{
		String clientID = getKeyForValue(dataproviders, columnName);
		if (clientID == null && searchInRecordDataLinkedPropertyIDsAsWell)
		{
			clientID = getKeyForValue(recordDataLinkedPropertyIDToColumnDP, columnName);
		}
		return clientID;
	}

	private <KT, VT> KT getKeyForValue(Map<KT, VT> map, VT value)
	{
		for (Entry<KT, VT> entry : map.entrySet())
		{
			if (Utils.equalObjects(value, entry.getValue()))
			{
				return entry.getKey();
			}
		}
		return null;
	}

	protected void populateRowData(IRecordInternal record, Set<String> columnNames, JSONWriter w, IBrowserConverterContext browserConverterContext,
		ViewportClientSideTypes types) throws JSONException
	{
		List<Pair<String/* forColumn */, JSONString/* type */>> typesOfColumns = null;
		JSONString clientSideTypeOfCol;

		for (Entry<String, String> entry : dataproviders.entrySet())
		{
			String dataProvider = entry.getValue();
			if (columnNames == null || columnNames.contains(dataProvider))
			{
				Object value = (dataProvider != null ? record.getValue(dataProvider) : null);
				if (value == Scriptable.NOT_FOUND) value = null; // if the given DP is invalid, then record.getValue(dataProvider) can return Rhino Scriptable.NOT_FOUND; we must handle that as that can't be sent to client conversion directly

				PropertyDescription pd = getDataProviderPropertyDescription(dataProvider);

				// currently all that NGUtils.getDataProviderPropertyDescription can return is IConvertedProperty type or default types; so we don't need any special value pre-processing (like IWrapperType or IServoyAwareValue or others would need)
				//			if (pd != null)
				//			{
				//				if (pd.getType() instanceof IWrapperType< ? , ? >) value = ((IWrapperType)pd.getType()).wrap(value, null, new DataConverterContext(pd,
				//					webObject));
				//			}

				if (value instanceof DbIdentValue)
				{
					value = ((DbIdentValue)value).getPkValue();
				}

				w.key(entry.getKey());
				IJSONStringWithClientSideType jsonValueRepresentationForWrappedValue = JSONUtils.FullValueToJSONConverter.INSTANCE
					.getConvertedValueWithClientType(value, pd,
						browserConverterContext, false);

				w.value(jsonValueRepresentationForWrappedValue); // write it even if it is null
				clientSideTypeOfCol = (jsonValueRepresentationForWrappedValue != null ? jsonValueRepresentationForWrappedValue.getClientSideType() : null);
				Pair<String/* forColumn */, JSONString/* type */> cellType = new Pair<>(entry.getKey(),
					clientSideTypeOfCol);
				if (typesOfColumns == null) typesOfColumns = new ArrayList<>();
				typesOfColumns.add(cellType);
			}
		}
		types.registerClientSideType(typesOfColumns);
	}

	private PropertyDescription getDataProviderPropertyDescription(String dataProvider)
	{
		if (parentDAL != null)
		{
			return NGUtils.getDataProviderPropertyDescription(dataProvider, parentDAL.getApplication(), parentDAL.getForm().getForm(), foundset.getTable(),
				false, false);
		}
		else
		{
			IDataAdapterList dl = NGComponentDALContext.getDataAdapterList(webObjectContext);
			if (dl != null)
				return NGUtils.getDataProviderPropertyDescription(dataProvider, dl.getApplication(), dl.getForm().getForm(), foundset.getTable(), false, false);
		}
		return NGUtils.getDataProviderPropertyDescription(dataProvider, foundset.getTable(), webObjectContext != null ? getApplication() : null, false, false);
	}

	public void browserUpdatesReceived(Object jsonValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
	{
		PushToServerEnum pushToServer = BrowserConverterContext.getPushToServerValue(dataConverterContext);

		if (getFoundset() == null) return;
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
						int requestID = update.getInt(ID_KEY);

						viewPort.clearSendingInitialPreferredViewport();
						viewPort.setBounds(newViewport.getInt(START_INDEX), newViewport.getInt(SIZE));

						changeMonitor.requestIdHandled(requestID, true);
					}
					if (update.has(PREFERRED_VIEWPORT_SIZE))
					{
						viewPort.setPreferredViewportSize(update.getInt(PREFERRED_VIEWPORT_SIZE));
						if (update.has(FoundsetPropertyTypeConfig.SEND_SELECTION_VIEWPORT_INITIALLY))
							viewPort.setSendSelectionViewportInitially(update.getBoolean(FoundsetPropertyTypeConfig.SEND_SELECTION_VIEWPORT_INITIALLY));
						if (update.has(INITIAL_SELECTION_VIEWPORT_CENTERED))
							viewPort.setInitialSelectionViewportCentered(update.getBoolean(INITIAL_SELECTION_VIEWPORT_CENTERED));
					}
					// {loadExtraRecords: negativeOrPositiveCount}
					else if (update.has("loadExtraRecords"))
					{
						int requestID = update.getInt(ID_KEY);
						viewPort.clearSendingInitialPreferredViewport();
						viewPort.loadExtraRecords(update.getInt("loadExtraRecords"));
						changeMonitor.requestIdHandled(requestID, true);
					}
					// {loadLessRecords: negativeOrPositiveCount}
					else if (update.has("loadLessRecords"))
					{
						int requestID = update.getInt(ID_KEY);
						viewPort.clearSendingInitialPreferredViewport();
						viewPort.loadLessRecords(update.getInt("loadLessRecords"));
						changeMonitor.requestIdHandled(requestID, true);
					}
					else if (update.has("sort"))
					{
						int requestID = update.getInt(ID_KEY);
						JSONArray columns = update.getJSONArray("sort");
						StringBuilder sort = new StringBuilder();
						Map<String, String> dp = dataproviders.size() > 0 ? dataproviders : recordDataLinkedPropertyIDToColumnDP;
						String dataProviderID = null;
						boolean sortAscending = true;
						for (int j = 0; j < columns.length(); j++)
						{
							JSONObject sortColumn = columns.getJSONObject(j);
							String name = sortColumn.getString("name");
							if (dp.containsKey(name))
							{
								sort.append(dp.get(name));
								sort.append(" " + sortColumn.getString("direction"));
								if (dataProviderID == null)
								{
									dataProviderID = dp.get(name);
									sortAscending = "asc".equalsIgnoreCase(sortColumn.getString("direction"));
								}
								if (j < columns.length() - 1) sort.append(",");
							}
						}
						IWebFormUI formUI = getFormUI();
						IWebFormController fc = (formUI != null ? formUI.getController() : null);
						if (fc != null && fc.getForm().getOnSortCmdMethodID() > 0 && dataProviderID != null)
						{
							// our api only supports one dataproviderid sort at a time
							JSEvent event = new JSEvent();
							event.setFormName(fc.getName());
							fc.executeFunction(String.valueOf(fc.getForm().getOnSortCmdMethodID()),
								Utils.arrayMerge((new Object[] { dataProviderID, Boolean.valueOf(sortAscending), event }),
									Utils.parseJSExpressions(fc.getForm().getFlattenedMethodArguments("onSortCmdMethodID"))), //$NON-NLS-1$
								true, null, false, "onSortCmdMethodID"); //$NON-NLS-1$
						}
						else
						{
							try
							{
								String currentSort = foundset.getSort();
								String newSort = sort.toString();
								foundset.setSort(newSort);
								if (!Utils.equalObjects(currentSort, newSort) || // really a new sort
									!Utils.equalObjects(foundset.getSort(), newSort)) // not sorted, send back to client
								{
									changeMonitor.foundsetSortChanged();
								}
							}
							catch (ServoyException e)
							{
								Debug.error("Cannot sort foundset by " + sort.toString(), e);
							}
						}
						changeMonitor.requestIdHandled(requestID, true);
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
							if (newSelectedIndexes.length == 1)
							{
								foundset.setSelectedIndex(newSelectedIndexes[0]);
							}
							else
							{
								foundset.setSelectedIndexes(newSelectedIndexes);
							}
						}
						finally
						{
							getListSelectionListener().resume();
							// if server denies the new selection as invalid and doesn't change selection, send it to the client so that it doesn't keep invalid selection
							if (!Arrays.equals(foundset.getSelectedIndexes(), newSelectedIndexes))
							{
								changeMonitor.selectionChanged(false);
							}
						}
					}
					// {newClientSelectionRequest: newSelectedIndexesArray}
					else if (update.has("newClientSelectionRequest"))
					{
						int requestID = update.getInt(ID_KEY);
						JSONArray jsonSelectedIndexes = update.getJSONArray("newClientSelectionRequest");
						int[] newSelectedIndexes = new int[jsonSelectedIndexes.length()];
						for (int j = newSelectedIndexes.length - 1; j >= 0; j--)
						{
							newSelectedIndexes[j] = jsonSelectedIndexes.getInt(j);
						}

						int[] oldSelection = foundset.getSelectedIndexes();
						// this !Arrays.equals check in conjunction with pause()/resume() is needed to avoid an effect on the client that server always sends back changed selection in which case
						// if the user quickly changes selection multiple times and the connection is slow, selection will jump all over
						// the place until it stabilizes correctly
						getListSelectionListener().pause();
						try
						{
							if (newSelectedIndexes.length == 1)
							{
								foundset.setSelectedIndex(newSelectedIndexes[0]);
							}
							else
							{
								foundset.setSelectedIndexes(newSelectedIndexes);
							}
						}
						finally
						{
							getListSelectionListener().resume();

							if (!Arrays.equals(oldSelection, foundset.getSelectedIndexes()))
							{// if the selection is changed, send it back to the client so that its model is also updated
								changeMonitor.selectionChanged(false);
								changeMonitor.requestIdHandled(requestID, true);
							}
							else
							{
								if (!Arrays.equals(oldSelection, newSelectedIndexes))
								{ // it was supposed to change but the server did not allow it
									changeMonitor.requestIdHandled(requestID, false);

								}
								else changeMonitor.requestIdHandled(requestID, true);
							}
						}
					}
					else if (update.has(ViewportDataChangeMonitor.VIEWPORT_CHANGED))
					{
						boolean success = false;
						if (PushToServerEnum.allow.compareTo(pushToServer) <= 0)
						{
							// {dataChanged: { ROW_ID_COL_KEY: rowIDValue, dataproviderName: value }}
							JSONObject dataChangeJSON = (JSONObject)update.get(ViewportDataChangeMonitor.VIEWPORT_CHANGED);
							String rowIDValue = dataChangeJSON.getString(ROW_ID_COL_KEY);
							String dpKey = dataChangeJSON.getString(DATAPROVIDER_KEY);
							String dataProviderName;
							if (dataproviders.containsKey(dpKey))
							{
								dataProviderName = dataproviders.get(dpKey);
							}
							else
							{
								dataProviderName = recordDataLinkedPropertyIDToColumnDP.get(dpKey);
							}
							Object value = dataChangeJSON.get(VALUE_KEY);

							if (foundset != null)
							{
								Pair<String, Integer> splitHashAndIndex = splitPKHashAndIndex(rowIDValue);
								int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

								if (recordIndex != -1)
								{
									IRecordInternal record = foundset.getRecord(recordIndex);
									// convert Dates where it's needed

									PropertyDescription dataProviderPropDesc = getDataProviderPropertyDescription(dataProviderName); // this should be enough for when only foundset dataproviders are used
									ValueReference<Boolean> returnValueAdjustedIncommingValueForRow = new ValueReference<Boolean>(Boolean.FALSE);
									value = JSONUtils.fromJSONUnwrapped(null, value, dataProviderPropDesc, dataConverterContext,
										returnValueAdjustedIncommingValueForRow);

									try
									{
										if (record.startEditing())
										{
											try
											{
												record.setValue(dataProviderName, value);
												success = true;
											}
											catch (IllegalArgumentException e)
											{
												// TODO handle the validaton errors.
												IWebFormUI formUI = getFormUI();
												formUI.getController().getApplication().reportError(
													"Validation for " + dataProviderName + " for value: " + value + " failed.", e);
											}
										}
										// else cannot start editing; finally block will deal with it (send old value back to client as new one can't be pushed)
									}
									finally
									{
										// if server denies the new value as invalid and doesn't change it, send it to the client so that it doesn't keep invalid value; the same if for example a double was rounded to an int
										if (!Utils.equalObjects(record.getValue(dataProviderName), value) || // TODO I think we can also use here !success instead of equalObjects(...)
											returnValueAdjustedIncommingValueForRow.value.booleanValue())
										{
											changeMonitor.recordsUpdated(recordIndex, recordIndex, foundset.getSize(), viewPort,
												Arrays.asList(new String[] { dataProviderName }));
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
						else
						{
							log.error("Property (" + pd +
								") that doesn't define a suitable pushToServer value (allow/shallow/deep) tried to modify foundset dataprovider value serverside. Denying and sending back full viewport!");
							changeMonitor.viewPortCompletelyChanged();
						}

						if (update.has(ID_KEY))
						{
							// it was called from client side public API "updateViewportRecord" that now returns a promise just like many of the other API calls; resolve/reject that
							changeMonitor.requestIdHandled(update.getInt(ID_KEY), success);
						} // else it was called from an angular watch - it doesn't have any defers client-side so no ID_KEY for message; no ID to send back then
					}
				}
			}
		}
		catch (JSONException e)
		{
			Debug.error("Error when getting browser updates for property (" + this.toString() + ") for " + jsonValue, e);
		}
	}

	/**
	 * When this foundset is used in combination with child "components" properties, those properties will need
	 * a dataAdapterList that is being fed records from this foundset.
	 */
	public FoundsetDataAdapterList getDataAdapterList()
	{
		// this method gets called by linked component type property/properties
		// that means here we are working with components, not with services - so we can cast webObject and create a new data adapter list
		if (dataAdapterList == null && webObjectContext != null && webObjectContext.getUnderlyingWebObject() instanceof WebComponent)
		{
			dataAdapterList = new FoundsetDataAdapterList(getFormUI().getController(), this);
			if (foundset != null) dataAdapterList.setFindMode(foundset.isInFindMode());
		}
		return dataAdapterList;
	}

	protected IWebFormUI getFormUI()
	{
		return webObjectContext.getUnderlyingWebObject() instanceof WebComponent
			? ((WebComponent)webObjectContext.getUnderlyingWebObject()).findParent(IWebFormUI.class) : null;
	}

	public static Pair<String, Integer> splitPKHashAndIndex(String pkHashAndIndex)
	{
		int index = pkHashAndIndex.lastIndexOf("_");
		int recordIndex = Integer.parseInt(pkHashAndIndex.substring(index + 1));
		String pkHash = pkHashAndIndex.substring(0, index);
		return new Pair<>(pkHash, Integer.valueOf(recordIndex));
	}

	public void addViewportDataChangeMonitor(ViewportDataChangeMonitor viewPortChangeMonitor)
	{
		changeMonitor.addViewportDataChangeMonitor(viewPortChangeMonitor);
	}

	public void removeViewportDataChangeMonitor(ViewportDataChangeMonitor viewPortChangeMonitor)
	{
		changeMonitor.removeViewportDataChangeMonitor(viewPortChangeMonitor);
	}

	public boolean setEditingRowByPkHash(String pkHashAndIndex)
	{
		Pair<String, Integer> splitHashAndIndex = splitPKHashAndIndex(pkHashAndIndex);
		int recordIndex = splitHashAndIndex.getRight().intValue();
		IRecordInternal recordByIndexHint = foundset.getRecord(recordIndex);
		String pkHash = splitHashAndIndex.getLeft();
		if (recordByIndexHint == null || !pkHash.equals(recordByIndexHint.getPKHashKey()))
		{
			recordIndex = foundset.getRecordIndex(pkHash, recordIndex);
			if (recordIndex != -1)
			{
				foundset.setSelectedIndex(recordIndex);
				return true;
			}
			else return false;
		}
		else foundset.setSelectedIndex(recordIndex);
		return true;
	}

	@Override
	public String toString()
	{
		return "FoundsetPT:  '" + (foundset != null ? foundset.getDataSource() : null) + " on property '" + propertyName +
			"': foundset type property on component " +
			(webObjectContext != null ? webObjectContext.getUnderlyingWebObject().getName() : "- not yet attached -");
	}

	public void setRecordDataLinkedPropertyIDToColumnDP(String id, String dataprovider)
	{
		if (dataprovider == null) recordDataLinkedPropertyIDToColumnDP.remove(id);
		else recordDataLinkedPropertyIDToColumnDP.put(id, dataprovider);
	}

	private String getSortStringAsNames()
	{
		String sortString = "";
		if (getFoundset() != null)
		{
			List<SortColumn> sortColumns = getFoundset().getSortColumns();
			if (sortColumns != null)
			{
				for (int j = 0; j < sortColumns.size(); j++)
				{
					String clientIDForColumnName = getClientIDForColumnName(sortColumns.get(j).getDataProviderID(), true);
					if (clientIDForColumnName != null)
					{
						sortString += clientIDForColumnName + " " + ((sortColumns.get(j).getSortOrder() == SortColumn.DESCENDING) ? "desc" : "asc");
						if (j < sortColumns.size() - 1)
						{
							sortString += ",";
						}
					}
					else
					{
						// not found, stop
						if (sortString.endsWith(","))
						{
							sortString = sortString.substring(0, sortString.length() - 1);
						}
						break;
					}
				}
			}
		}

		lastSortString = sortString;
		return sortString;
	}

	public void tableChanged(TableModelEvent e)
	{
		// We try to detect when a sort has been done on the foundset, and we update the arrows in the header accordingly.
		// This is just an heuristic for filtering out the sort event from all table changed events that are raised.
		if (getFoundset() != null && e.getColumn() == TableModelEvent.ALL_COLUMNS && e.getFirstRow() == 0)
		{
			if (!Utils.equalObjects(lastSortString, getSortStringAsNames())) changeMonitor.foundsetSortChanged();
		}
	}


	@Override
	public void addStateChangeListener(IChangeListener valueChangeListener)
	{
		if (!underlyingStateListeners.contains(valueChangeListener)) underlyingStateListeners.add(valueChangeListener);
	}

	@Override
	public void removeStateChangeListener(IChangeListener valueChangeListener)
	{
		underlyingStateListeners.remove(valueChangeListener);
	}

	protected void fireUnderlyingStateChangedListeners()
	{
		if (underlyingStateListeners.size() > 0)
		{
			// just in case any listeners will end up trying to alter underlyingValueChangeListeners - avoid a ConcurrentModificationException
			IChangeListener[] copyOfListeners = underlyingStateListeners.toArray(new IChangeListener[underlyingStateListeners.size()]);
			for (IChangeListener l : copyOfListeners)
			{
				l.valueChanged();
			}
		}
	}

	public boolean allowPush(Object jsonValue, IllegalChangeFromClientException e)
	{
		if (jsonValue instanceof JSONArray)
		{
			List<String> propertiesToCheck;
			boolean blockAllowPushIfContainsIs;
			JSONArray arr = (JSONArray)jsonValue;
			// for allow access on enabled, allow everything except data push
			if (EnabledPropertyType.TYPE_NAME.equals(e.getBlockedByProperty()) && hasAllowAccessFor(EnabledPropertyType.TYPE_NAME))
			{
				propertiesToCheck = Arrays.asList(ViewportDataChangeMonitor.VIEWPORT_CHANGED);
				blockAllowPushIfContainsIs = true;
			}
			else
			{
				// by default only allow viewport size update
				propertiesToCheck = Arrays.asList(PREFERRED_VIEWPORT_SIZE);
				blockAllowPushIfContainsIs = false;
			}

			for (int i = 0; i < arr.length(); i++)
			{
				JSONObject update = (JSONObject)arr.get(i);
				for (String key : update.keySet())
				{
					if (blockAllowPushIfContainsIs == propertiesToCheck.contains(key))
					{
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	private boolean hasAllowAccessFor(String propertyTypeName)
	{
		boolean hasAllowAccessForEnabled = false;
		PropertyDescription pd = this.webObjectContext.getPropertyDescription(this.propertyName);
		Object allowEditTag = pd.getTag(WebObjectSpecification.ALLOW_ACCESS);
		// allowEditTag is either a String or an array of Strings representing 'blocked by' property name(s) that should not block the given property (the spec makes specific exceptions in the property itself for the other props. that should not block it)
		if (allowEditTag instanceof JSONArray)
		{
			for (Object element : ((JSONArray)allowEditTag))
			{
				if (element.equals(propertyTypeName))
				{
					hasAllowAccessForEnabled = true;
					break;
				}
			}
		}
		else if (allowEditTag instanceof String && allowEditTag.equals(propertyTypeName))
		{
			hasAllowAccessForEnabled = true;
		}

		return hasAllowAccessForEnabled;
	}

	public IFoundSetInternal checkForExistingFormFoundsetBeforeFormIsVisibleAndGetIt()
	{
		// if this is supposed to work with a form foundset, and controler.init()
		// was called (so form might have a foundset) but form is not yet visible
		// so dataProviderOrRecordChanged() was not yet called with a record, do
		// check form foundset again
		if (foundset == null && FORM_FOUNDSET_SELECTOR.equals(foundsetSelector)) updateFoundset((IRecordInternal)null);
		return foundset;
	}

	/**
	 * @return the dataproviders
	 */
	public Map<String, String> getDataproviders()
	{
		return dataproviders;
	}
}
