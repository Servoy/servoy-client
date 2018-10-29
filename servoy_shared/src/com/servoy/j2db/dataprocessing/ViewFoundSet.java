/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.impl.QBJoin;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.querybuilder.impl.QBTableClause;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * @author jcompagner
 * @since 8.4
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ViewFoundSet", scriptingName = "ViewFoundSet")
public class ViewFoundSet extends AbstractTableModel implements ISwingFoundSet, IConstantsObject
{
	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen also for column changes of the given table/datasource.
	 * Like order_lines.productid that has a join to orders and is displaying the productname.
	 * If a change in such a join condition (like order_lines.productid in the sample above) is seen then the query will befired again to detect changes.
	 */
	public static final int MONITOR_JOIN_CONDITIONS = 1;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen also for column changes of the given table/datasource that are used in the where statement.
	 * Like order_lines.unit_price > 100. If a change is seen on that datasource on such a column used in the where a full query will be fired again to detect changes.
	 */
	public static final int MONITOR_WHERE_CONDITIONS = 2;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for inserts on the given table/datasource.
	 * This will always result in a full query to detect changes whenever an insert on that table happens.
	 */
	public static final int MONITOR_INSERT = 4;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for deletes on the given table/datasource.
	 * This will always result in a full query to detect changes whenever an insert on that table happens.
	 */
	public static final int MONITOR_DELETES = 8;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for deletes on the given table/datasource which should be the primairy/main table of this query.
	 * If a delete comes in for this table, then we will only remove the records from the ViewFoundSet that do have this primairy key in its value. So no need to do a full query.
	 * So this will only work if the query shows order_lines for the order_lines table, not for the products table that is joined to get the product_name.
	 * Only 1 of the 2 monitors for deletes should be registered for a table/datasource.
	 */
	public static final int MONITOR_DELETES_FOR_PRIMAIRY_TABLE = 16;

	protected transient AlwaysRowSelectedSelectionModel selectionModel;
	private transient TableAndListEventDelegate tableAndListEventDelegate;

	private final String datasource;
	private final IFoundSetManagerInternal manager;

	private final List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<IFoundSetEventListener>(3);
	// this is just a list to keep hard references to the RowListeners we give the RowManager (that is kept weak in there)
	private final List<IRowListener> rowListeners = new ArrayList<IRowListener>(3);

	private List<ViewRecord> records = new ArrayList<>();

	private final List<WeakReference<IRecordInternal>> allParents = new ArrayList<WeakReference<IRecordInternal>>(6);

	private final Map<IQuerySelectValue[], Map<String, List<Integer>>> pkByDatasourceCache = new HashMap<>();

	private final Map<IQuerySelectValue, String> columnNames = new LinkedHashMap<>();

	private QuerySelect select;

	private int foundsetID = 0;

	private boolean hasMore = false;
	private boolean refresh = true;
	private int currentChunkSize;
	private final int chunkSize;

	// forms might force their foundset to remain at a certain multiselect value
	// if a form 'pinned' multiselect, multiSelect should not be changeable by foundset JS access
	// if more then 1 form wishes to pin multiselect at a time, the form with lowest elementid wins
	private int multiSelectPinnedTo = -1;
	private int multiSelectPinLevel;
	private final Map<BaseQueryTable, List<IQuerySelectValue>> pkColumnsForTable;
	private final Map<BaseQueryTable, List<IQuerySelectValue>> columnsForTable;

	public ViewFoundSet(String datasource, QuerySelect select, IFoundSetManagerInternal manager, int chunkSize)
	{
		this.datasource = datasource;
		this.select = select;
		this.manager = manager;
		this.chunkSize = chunkSize;
		this.currentChunkSize = chunkSize;
		createSelectionModel();

		BaseQueryTable baseTable = select.getTable();
		final Map<String, IQuerySelectValue> nameToSelect = new HashMap<>();
		final Map<IQuerySelectValue, String> selectToName = new HashMap<>();
		pkColumnsForTable = new IdentityHashMap<>();
		columnsForTable = new IdentityHashMap<>();
		for (IQuerySelectValue selectValue : select.getColumns())
		{
			QueryColumn column = selectValue.getColumn();
			String name = selectValue.getAlias() != null ? selectValue.getAlias() : column.getName();
			IQuerySelectValue duplicate = nameToSelect.get(name);
			if (duplicate != null)
			{
				if (duplicate.getColumn().getTable() == baseTable)
				{
					BaseQueryTable colTable = column.getTable();
					name = (colTable.getAlias() != null ? colTable.getAlias() : colTable.getName()) + '.' + name;
					nameToSelect.put(name, selectValue);
					selectToName.put(selectValue, name);
				}
				else
				{
					nameToSelect.put(name, selectValue);
					selectToName.put(selectValue, name);
					BaseQueryTable colTable = duplicate.getColumn().getTable();
					name = (colTable.getAlias() != null ? colTable.getAlias() : colTable.getName()) + '.' + name;
					nameToSelect.put(name, selectValue);
					selectToName.put(selectValue, name);
				}
			}
			else
			{
				nameToSelect.put(name, selectValue);
				selectToName.put(selectValue, name);
			}
			BaseQueryTable table = column.getTable();
			if ((column.getFlags() & IBaseColumn.IDENT_COLUMNS) != 0)
			{
				List<IQuerySelectValue> list = pkColumnsForTable.get(table);
				if (list == null)
				{
					list = new ArrayList<>();
					pkColumnsForTable.put(table, list);
				}
				list.add(column);
			}
			else
			{
				List<IQuerySelectValue> list = columnsForTable.get(table);
				if (list == null)
				{
					list = new ArrayList<>();
					columnsForTable.put(table, list);
				}
				list.add(column);
			}
		}

		for (IQuerySelectValue selectValue : select.getColumns())
		{
			columnNames.put(selectValue, selectToName.get(selectValue));
		}
	}

	private void addQuerySelectToMap(final Map<BaseQueryTable, List<QueryColumn>> columnsInJoinsPerTable, QueryColumn column)
	{
		List<QueryColumn> list = columnsInJoinsPerTable.get(column.getTable());
		if (list == null)
		{
			list = new ArrayList<>();
			columnsInJoinsPerTable.put(column.getTable(), list);
		}
		list.add(column);
	}

	/**
	 * @param queryPks
	 * @param realOrderedPks
	 * @return
	 */
	private IQuerySelectValue[] getOrderedPkColumns(List<IQuerySelectValue> queryPks, String[] realOrderedPks)
	{
		if (queryPks.size() != realOrderedPks.length) return null;
		IQuerySelectValue[] retValue = new IQuerySelectValue[realOrderedPks.length];
		for (int i = 0; i < realOrderedPks.length; i++)
		{
			for (IQuerySelectValue selectValue : queryPks)
			{
				if (selectValue.getColumn().getName().equals(realOrderedPks[i]))
				{
					retValue[i] = selectValue;
					break;
				}
			}
			if (retValue[i] == null) return null;
		}
		return retValue;
	}

	@Override
	public boolean isInitialized()
	{
		return !refresh;
	}

	@Override
	public String[] getDataProviderNames(int type)
	{
		return null;
	}

	@Override
	@JSFunction
	public String getDataSource()
	{
		return datasource;
	}

	@Override
	public String getRelationName()
	{
		return null;
	}

	@Override
	@JSFunction
	public int getSize()
	{
		if (refresh)
		{
			try
			{
				loadAllRecords();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
		return records.size();
	}

	@Override
	public int getRawSize()
	{
		return records.size();
	}

	@Override
	public void addFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			if (!foundSetEventListeners.contains(l))
			{
				foundSetEventListeners.add(l);
			}
		}
	}

	@Override
	public void removeFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			foundSetEventListeners.remove(l);
		}
	}

	@Override
	public Object forEach(IRecordCallback callback)
	{
		FoundSetIterator foundsetIterator = new FoundSetIterator();
		while (foundsetIterator.hasNext())
		{
			IRecord currentRecord = foundsetIterator.next();
			Object returnValue = callback.handleRecord(currentRecord, foundsetIterator.currentIndex, this);
			if (returnValue != null && returnValue != Undefined.instance)
			{
				return returnValue;
			}
		}
		return null;
	}

	/**
	 * Iterates over the records of a foundset taking into account inserts and deletes that may happen at the same time.
	 * It will dynamically load all records in the foundset (using Servoy lazy loading mechanism). If callback function returns a non null value the traversal will be stopped and that value is returned.
	 * If no value is returned all records of the foundset will be traversed. Foundset modifications( like sort, omit...) cannot be performed in the callback function.
	 * If foundset is modified an exception will be thrown. This exception will also happen if a refresh happens because of a rollback call for records on this datasource when iterating.
	 * When an exception is thrown from the callback function, the iteraion over the foundset will be stopped.
	 *
	 * @sample
	 *  foundset.forEach(function(record,recordIndex,foundset) {
	 *  	//handle the record here
	 *  });
	 *
	 * @param callback The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.
	 *
	 * @return Object the return value of the callback
	 *
	 */
	public Object js_forEach(Function callback)
	{
		return forEach(new CallJavaScriptCallBack(callback, manager.getApplication().getScriptEngine(), null));
	}

	/**
	 * @clonedesc js_forEach(Function)
	 *
	 * @sampleas js_forEach(Function)
	 *
	 * @param callback The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.
	 * @param thisObject What the this object should be in the callback function (default it is the foundset)
	 *
	 * @return Object the return value of the callback
	 *
	 */
	public Object js_forEach(Function callback, Scriptable thisObject)
	{
		return forEach(new CallJavaScriptCallBack(callback, manager.getApplication().getScriptEngine(), thisObject));
	}


	/**
	 * Databroadcast can be enabled per select table of a query, the select table can be the main QBSelect or on of it QBJoins
	 * By default this monitors only the column values that are in the result of the QBSelect, you can only enable databroadcast for a table if for that table also the PK is selected in the results.
	 *
	 * you can use {@link #enableDatabroadcastFor(QBTableClause, int)} to specify what should be monitored more besides pure column values per pk.
	 * Those have impact on performance because for the most part if we see a hit then a full query is done to see if there are changes.
	 *
	 * @sample
	 *  var select = datasources.db.example_data.order_details.createSelect();
	 *  var join = select.joins.add("db:/example_data/products");
	 *  join.on.add(select.columns.productid.eq(join.columns.productid));
	 *  select.result.add(); // add colums of the select or join
	 *  var vf = databaseManager.getViewFoundSet("myorders",select)
	 *  vf.enableDatabroadcastFor(select);
	 *  vf.enableDatabroadcastFor(join);
	 *
	 * @param queryTable The QBSelect or QBJoin of a full query where this foundset should listenn for data changes.
	 */
	@JSFunction
	public void enableDatabroadcastFor(QBTableClause queryTable)
	{
		enableDatabroadcastFor(queryTable, 0);
	}

	/**
	 * Enable the databroadcast for a specific table of the QBSelect or QBJoin with extra flags for looking for join or where criteria or deletes/inserts.
	 * These extra flags can be a performance hit because the query needs to be executed again to see if there are changes.
	 * You need to have pk selected in the results for the table/datasource that you are enabling databroadcast on.
	 *
	 * @sample
	 *  var select = datasources.db.example_data.order_details.createSelect();
	 *  var join = select.joins.add("db:/example_data/products");
	 *  join.on.add(select.columns.productid.eq(join.columns.productid));
	 *  select.result.add(); // add colums of the select or join
	 *  var vf = databaseManager.getViewFoundSet("myorders",select)
	 *  // monitor for the main table the join conditions (orders->product, when product id changes in the orders table) and requery the table on insert events, delete directly the record if a pk delete happens.
	 *  vf.enableDatabroadcastFor(select,,ViewFoundSet.MONITOR_JOIN_CONDITIONS | ViewFoundSet.MONITOR_INSERT | ViewFoundSet.MONITOR_DELETES_FOR_PRIMAIRY_TABLE);
	 *  vf.enableDatabroadcastFor(join);
	 *
	 * @param queryTable The QBSelect or QBJoin of a full query where this foundset should listenn for data changes.
	 * @param flags One or more of the ViewFoundSet.XXX flags added to each other.
	 */
	@JSFunction
	public void enableDatabroadcastFor(QBTableClause queryTable, int flags)
	{
		BaseQueryTable table = null;
		if (queryTable instanceof QBSelect)
		{
			table = ((QBSelect)queryTable).getQuery().getTable();
		}
		else if (queryTable instanceof QBJoin)
		{
			table = ((QBJoin)queryTable).getQueryTable();
		}
		if (table != null)
		{
			// touch the row manager for the given table
			List<IQuerySelectValue> list = pkColumnsForTable.get(table);
			if (list != null)
			{
				try
				{
					RowManager rowManager = manager.getRowManager(table.getDataSource());
					if (rowManager != null)
					{
						String[] realOrderedPks = rowManager.getSQLSheet().getPKColumnDataProvidersAsArray();
						IQuerySelectValue[] queryPks = getOrderedPkColumns(list, realOrderedPks);
						if (queryPks != null)
						{
							final Map<BaseQueryTable, List<QueryColumn>> columnsInJoinsPerTable = new IdentityHashMap<>();

							IVisitor visitor = (object) -> {
								if (object instanceof QueryColumn)
								{
									addQuerySelectToMap(columnsInJoinsPerTable, (QueryColumn)object);
								}
								return object;
							};
							if ((flags & MONITOR_JOIN_CONDITIONS) == MONITOR_JOIN_CONDITIONS) AbstractBaseQuery.acceptVisitor(select.getJoins(), visitor);
							if ((flags & MONITOR_WHERE_CONDITIONS) == MONITOR_WHERE_CONDITIONS) AbstractBaseQuery.acceptVisitor(select.getWhere(), visitor);

							boolean monitorInserts = (flags & MONITOR_INSERT) == MONITOR_INSERT;
							boolean monitorIDeletes = (flags & MONITOR_DELETES) == MONITOR_DELETES;
							boolean monitorIDeletesForMain = (flags & MONITOR_DELETES_FOR_PRIMAIRY_TABLE) == MONITOR_DELETES_FOR_PRIMAIRY_TABLE;

							RowListener rl = new RowListener(table.getDataSource(), queryPks, columnsForTable.get(table), columnsInJoinsPerTable.get(table),
								monitorInserts, monitorIDeletes, monitorIDeletesForMain);
							// keep a hard reference so as long as this ViewFoundSet lives the listener is kept in RowManager
							rowListeners.add(rl);
							rowManager.register(rl);
						}
						else
						{
							throw new RuntimeException(
								"ViewFoundSets did get pks '" + list + "' for datasource " + table + " but they should be " + Arrays.toString(realOrderedPks));
						}
					}
				}
				catch (ServoyException e)
				{
					Debug.error(e);
				}
			}
			else
			{
				throw new RuntimeException("ViewFoundSet based on select: " + this.select + " does not have pk's selected from " + table.getDataSource() +
					" to enable databroadcast for that datasource");
			}

		}
	}


	@Override
	public int getRecordIndex(IRecord record)
	{
		return records.indexOf(record);
	}

	@Override
	public boolean isRecordEditable(int row)
	{
		return true;
	}

	@Override
	public boolean isInFindMode()
	{
		return false;
	}

	@Override
	public boolean find()
	{
		return false;
	}

	@Override
	public int search() throws Exception
	{
		return 0;
	}

	@Override
	@JSFunction
	public void loadAllRecords() throws ServoyException
	{
		String serverName = DataSourceUtils.getDataSourceServerName(select.getTable().getDataSource());
		String transaction_id = manager.getTransactionID(serverName);
		try
		{
			IDataSet ds = manager.getApplication().getDataServer().performQuery(manager.getApplication().getClientID(), serverName, transaction_id, select,
				manager.getTableFilterParams(serverName, select), select.isUnique(), 0, currentChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
			refresh = false;
			ArrayList<IQuerySelectValue> cols = select.getColumns();
			int currentSize = records.size();
			List<ViewRecord> old = records;
			records = new ArrayList<>(ds.getRowCount());
			pkByDatasourceCache.clear();

			String[] colNames = columnNames.values().toArray(new String[columnNames.size()]);

			for (int i = 0; i < ds.getRowCount(); i++)
			{
				Object[] rowData = ds.getRow(i);
				if (i < currentSize)
				{
					ViewRecord current = old.get(i);
					current.updateValues(colNames, rowData);
					records.add(current);
				}
				else
				{
					records.add(new ViewRecord(colNames, rowData, i, this));
				}
			}
			hasMore = ds.hadMoreRows();
			fireDifference(currentSize, records.size());
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	@Override
	public void clear()
	{
		int prevIndex = records.size();
		records.clear();
		refresh = false;
		hasMore = false;
		currentChunkSize = chunkSize;
		fireFoundSetEvent(0, prevIndex - 1, FoundSetEvent.CHANGE_DELETE);
	}

	@Override
	public void deleteRecord(int row) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public void deleteAllRecords() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(int indexToAdd, boolean changeSelection) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int duplicateRecord(int recordIndex, int indexToAdd) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	private void createSelectionModel()
	{
		if (selectionModel == null)
		{
			selectionModel = new AlwaysRowSelectedSelectionModel(this);
			addListDataListener(selectionModel);
		}
	}

	@Override
	public int getSelectedIndex()
	{
		if (selectionModel == null) createSelectionModel();

		return selectionModel.getSelectedRow();
	}

	@Override
	public void setSelectedIndex(int selectedRow)
	{
		if (selectionModel == null) createSelectionModel();
		selectionModel.setSelectedRow(selectedRow);
	}

	protected void setMultiSelectInternal(boolean isMultiSelect)
	{
		if (selectionModel == null) createSelectionModel();
		if (isMultiSelect != isMultiSelect())
		{
			selectionModel.setSelectionMode(isMultiSelect ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
		}
	}

	/**
	 * @param pinId lower id has priority over higher id when using the same pinLevel. (refers to form element id)
	 * @param pinLevel lower level has priority in pinning over higher level. (refers to visible/invisible forms)
	 */
	public void pinMultiSelectIfNeeded(boolean multiSelect, int pinId, int pinLevel)
	{
		if (multiSelectPinnedTo == -1)
		{
			// no current pinning; just pin
			multiSelectPinLevel = pinLevel;
			multiSelectPinnedTo = pinId;
			setMultiSelectInternal(multiSelect);
		}
		else if (pinLevel < multiSelectPinLevel)
		{
			// current pin was for hidden form, this is a visible form
			multiSelectPinLevel = pinLevel;
			if (multiSelectPinnedTo != pinId)
			{
				multiSelectPinnedTo = pinId;
				setMultiSelectInternal(multiSelect);
			}
		}
		else if (pinLevel == multiSelectPinLevel)
		{
			// same pin level, different forms; always choose one with lowest id
			if (pinId < multiSelectPinnedTo)
			{
				multiSelectPinnedTo = pinId;
				setMultiSelectInternal(multiSelect);
			}
		}
		else if (pinId == multiSelectPinnedTo) // && (pinLevel > multiSelectPinLevel) implied
		{
			// pinlevel is higher then current; if this is the current pinned form, update the pin level
			// maybe other visible forms using this foundset want to pin selection mode in this case (visible pinning form became hidden)
			multiSelectPinLevel = pinLevel;
			fireSelectionModeChange();
		}
	}

	@Override
	@JSGetter
	public void setMultiSelect(boolean multiSelect)
	{
		if (multiSelectPinnedTo == -1) setMultiSelectInternal(multiSelect); // if a form is currently overriding this, ignore js call
	}

	@Override
	@JSGetter
	public boolean isMultiSelect()
	{
		if (selectionModel == null) createSelectionModel();
		return selectionModel.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
	}

	@Override
	public void setSelectedIndexes(int[] indexes)
	{
		if (selectionModel == null) createSelectionModel();
		selectionModel.setSelectedRows(indexes);
	}

	@Override
	public int[] getSelectedIndexes()
	{
		if (selectionModel == null) createSelectionModel();
		return selectionModel.getSelectedRows();
	}

	@JSFunction
	public ViewRecord getSelectedRecord()
	{
		int selectedIndex = getSelectedIndex();
		if (selectedIndex >= 0 && selectedIndex < records.size()) return records.get(selectedIndex);
		return null;
	}

	@Override
	public String getSort()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSort(String sortString) throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Get the query that the foundset is currently using (as a clone; modifying this QBSelect will not automatically change the foundset).
	 * When the founset is in find mode, the find conditions are included in the resulting query.
	 * So the query that would be used when just calling search() (or search(true,true)) is returned.
	 * Note that foundset filters are included and table filters are not included in the query.
	 *
	 * @sample
	 * var q = foundset.getQuery()
	 * q.where.add(q.columns.x.eq(100))
	 * foundset.loadRecords(q);
	 *
	 * @return query.
	 */
	@JSFunction
	public QBSelect getQuery()
	{
		QuerySelect query = AbstractBaseQuery.deepClone(this.select, true);
		IApplication application = manager.getApplication();
		return new QBSelect(manager, manager.getScopesScopeProvider(), application.getFlattenedSolution(), application.getScriptEngine().getSolutionScope(),
			select.getTable().getDataSource(), null, query);
	}

	@Override
	public boolean loadByQuery(IQueryBuilder query) throws ServoyException
	{
		this.select = ((QBSelect)query).build(); // makes a clone
		loadAllRecords();
		return true;
	}

	@Override
	public void setRecordToStringDataProviderID(String dataProviderID)
	{
	}

	@Override
	public String getRecordToStringDataProviderID()
	{
		return null;
	}

	@Override
	public void sort(List<SortColumn> sortColumns) throws ServoyException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void makeEmpty()
	{
		clear();
	}

	@Override
	public void browseAll() throws ServoyException
	{
		loadAllRecords();
	}

	@Override
	public void deleteAll() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(boolean addOnTop) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(boolean addOnTop, boolean changeSelection) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int duplicateRecord(int row, boolean addOnTop) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public boolean containsDataProvider(String dataProviderID)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getDataProviderValue(String dataProviderID)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object setDataProviderValue(String dataProviderID, Object value)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<IRecord> iterator()
	{
		return new FoundSetIterator();
	}

	@Override
	public void completeFire(Map<IRecord, List<String>> entries)
	{
		int start = Integer.MAX_VALUE;
		int end = -1;
		List<String> dataproviders = null;
		for (Entry<IRecord, List<String>> entry : entries.entrySet())
		{
			int index = getRecordIndex(entry.getKey());
			if (index != -1 && start > index)
			{
				start = index;
			}
			if (end < index)
			{
				end = index;
			}
			if (dataproviders == null) dataproviders = entry.getValue();
			else dataproviders.addAll(entry.getValue());
		}
		if (start != Integer.MAX_VALUE && end != -1)
		{
			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE, dataproviders);
		}
	}

	@Override
	public SQLSheet getSQLSheet()
	{
		return null;
	}

	@Override
	public PrototypeState getPrototypeState()
	{
		return new PrototypeState(this);
	}

	@Override
	public void addParent(IRecordInternal record)
	{
		if (record != null && record.getParentFoundSet() != this)
		{
			synchronized (allParents)
			{
				for (int i = allParents.size(); --i >= 0;)
				{
					WeakReference<IRecordInternal> wr = allParents.get(i);
					IRecordInternal rcd = wr.get();
					if (rcd == null)
					{
						allParents.remove(i);
					}
					else
					{
						if (rcd.equals(record))
						{
							return;
						}
					}
				}
				allParents.add(new WeakReference<IRecordInternal>(record));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#fireAggregateChangeWithEvents(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	@Override
	public void fireAggregateChangeWithEvents(IRecordInternal record)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#isValidRelation(java.lang.String)
	 */
	@Override
	public boolean isValidRelation(String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getRelatedFoundSet(com.servoy.j2db.dataprocessing.IRecordInternal, java.lang.String,
	 * java.util.List)
	 */
	@Override
	public IFoundSetInternal getRelatedFoundSet(IRecordInternal record, String relationName, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getCalculationValue(com.servoy.j2db.dataprocessing.IRecordInternal, java.lang.String,
	 * java.lang.Object[], com.servoy.j2db.scripting.UsedDataProviderTracker)
	 */
	@Override
	public Object getCalculationValue(IRecordInternal record, String dataProviderID, Object[] vargs, UsedDataProviderTracker usedDataProviderTracker)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sort(List<SortColumn> sortColumns, boolean defer) throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void sort(Comparator<Object[]> recordPKComparator)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public List<SortColumn> getSortColumns()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Sorts the foundset based on the given record comparator function.
	 * Tries to preserve selection based on primary key. If first record is selected or cannot select old record it will select first record after sort.
	 * The comparator function is called to compare
	 * two records, that are passed as arguments, and
	 * it will return -1/0/1 if the first record is less/equal/greater
	 * then the second record.
	 *
	 * The function based sorting does not work with printing.
	 * It is just a temporary in-memory sort.
	 *
	 * NOTE: starting with 7.2 release this function doesn't save the data anymore
	 *
	 * @sample
	 * %%prefix%%foundset.sort(mySortFunction);
	 *
	 * function mySortFunction(r1, r2)
	 * {
	 *	var o = 0;
	 *	if(r1.id < r2.id)
	 *	{
	 *		o = -1;
	 *	}
	 *	else if(r1.id > r2.id)
	 *	{
	 *		o = 1;
	 *	}
	 *	return o;
	 * }
	 *
	 * @param recordComparisonFunction record comparator function
	 */
	@JSFunction
	@JSSignature(arguments = { Function.class })
	public void sort(Object recordComparisonFunction)
	{
		if (recordComparisonFunction instanceof Function)
		{
			final Function func = (Function)recordComparisonFunction;
			final IExecutingEnviroment scriptEngine = manager.getApplication().getScriptEngine();
			final Scriptable recordComparatorScope = func.getParentScope();
			sort(new Comparator<Object[]>()
			{
				public int compare(Object[] o1, Object[] o2)
				{
					try
					{
						Object compareResult = scriptEngine.executeFunction(func, recordComparatorScope, recordComparatorScope,
							new Object[] { getRecord(o1), getRecord(o2) }, false, true);
						double cmp = Utils.getAsDouble(compareResult, true);
						return cmp < 0 ? -1 : cmp > 0 ? 1 : 0;
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
					return 0;
				}
			});
		}
	}

	@Override
	public IFoundSetManagerInternal getFoundSetManager()
	{
		return manager;
	}

	@Override
	public ITable getTable()
	{
		try
		{
			return manager.getTable(select.getTable().getDataSource());
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create a copy from the  current record of a View Foundset of datasource " + this.datasource);
	}

	@Override
	public IFoundSetInternal copy(boolean unrelate) throws ServoyException
	{
		return new ViewFoundSet(datasource, AbstractBaseQuery.deepClone(this.select, true), manager, chunkSize);
	}

	public ViewRecord js_getRecord(int row)
	{
		return getRecord(row - 1);
	}

	@Override
	public ViewRecord getRecord(int row)
	{
		testForLoadMore(row);
		if (row < 0 || row >= records.size()) return null;
		return records.get(row);
	}

	private void testForLoadMore(int maxIndex)
	{
		boolean queryForMore = hasMore && (maxIndex == records.size() - 1);
		if (refresh || queryForMore)
		{
			try
			{
				if (queryForMore) currentChunkSize += chunkSize;
				loadAllRecords();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
	}

	@Override
	public IRecordInternal[] getRecords(int startrow, int count)
	{
		int toIndex = startrow + count;
		testForLoadMore(toIndex);
		toIndex = records.size() < toIndex ? records.size() : toIndex;
		List<ViewRecord> subList = records.subList(startrow, toIndex);
		return subList.toArray(new IRecordInternal[subList.size()]);
	}

	@Override
	public void deleteAllInternal() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public void addAggregateModificationListener(IModificationListener listener)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAggregateModificationListener(IModificationListener listener)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hadMoreRows()
	{
		return hasMore;
	}

	@Override
	public void deleteRecord(IRecordInternal record) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public IRecordInternal getRecord(Object[] pk)
	{
		for (IRecordInternal record : records)
		{
			if (Utils.equalObjects(record.getPK(), pk)) return record;
		}
		return null;
	}

	@Override
	public int getRecordIndex(String pkHash, int startHint)
	{
		int hintStart = Math.min(startHint + 5, getSize());

		int start = (hintStart < 0 || hintStart > records.size()) ? 0 : hintStart;

		for (int i = start; --i >= 0;)
		{
			String recordPkHash = null;
			IRecordInternal record = records.get(i);
			recordPkHash = record.getPKHashKey();
			if (pkHash.equals(recordPkHash))
			{
				return i;
			}
		}
		for (int i = start; i < records.size(); i++)
		{
			String recordPkHash = null;
			IRecordInternal record = records.get(i);
			recordPkHash = record.getPKHashKey();
			if (pkHash.equals(recordPkHash))
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Gets the unique id of the foundset. The foundset does not have an id until this method is called the first time on it.
	 */
	@Override
	public int getID()
	{
		// we do not automatically assign an id to each foundset so that ng client client side code cannot send in random ints to target any foundset;
		// in this way, only foundsets that were sent to client already (getID() was called on them in various property types to send it to client) can be targeted
		if (foundsetID == 0) foundsetID = this.manager.getNextFoundSetID();
		return foundsetID;
	}


	/**
	 * Same as {@link #getID()} but it will not assign an id if it wasn't set before.
	 */
	int getIDInternal()
	{
		return foundsetID;
	}

	@Override
	public void fireFoundSetChanged()
	{
		fireFoundSetEvent(0, records.size() - 1, FoundSetEvent.CHANGE_UPDATE);
	}

	/**
	 * Fire difference based on real size (not corrected for fires!)
	 *
	 * @param oldSize
	 * @param newSize
	 */
	protected void fireDifference(int oldSize, int newSize)
	{
		if (newSize == 0 && oldSize == 0 || oldSize == newSize) return;

		//let the List know the model changed,the new states
		if (newSize < oldSize)
		{
			fireFoundSetEvent(newSize, oldSize - 1, FoundSetEvent.CHANGE_DELETE);
		}
		else
		{
			if (newSize > oldSize)
			{
				fireFoundSetEvent(oldSize, newSize - 1, FoundSetEvent.CHANGE_INSERT);
			}
		}
	}

	public void fireSelectionModeChange()
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.SELECTION_MODE_CHANGE, FoundSetEvent.CHANGE_UPDATE));
	}

	protected final void fireFoundSetEvent(int firstRow, int lastRow, int changeType)
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType, firstRow, lastRow));
	}

	protected final void fireFoundSetEvent(int firstRow, int lastRow, int changeType, List<String> dataproviders)
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType, firstRow, lastRow, dataproviders));
	}


	protected void fireFoundSetEvent(final FoundSetEvent e)
	{
		if (foundSetEventListeners.size() > 0)
		{
			Runnable run = new Runnable()
			{
				@Override
				public void run()
				{
					if (foundSetEventListeners.size() > 0)
					{
						final IFoundSetEventListener[] array;
						synchronized (foundSetEventListeners)
						{
							array = foundSetEventListeners.toArray(new IFoundSetEventListener[foundSetEventListeners.size()]);
						}

						for (IFoundSetEventListener element : array)
						{
							element.foundSetChanged(e);
						}
					}
				}
			};
			if (this.manager.getApplication().isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				this.manager.getApplication().invokeLater(run);
			}
		}
		int type = e.getChangeType();
		int firstRow = e.getFirstRow();
		int lastRow = e.getLastRow();
		// always fire also if there are no listeners (because of always-first-selection rule)
		if (type == FoundSetEvent.CHANGE_INSERT || type == FoundSetEvent.CHANGE_DELETE)
		{
			// if for example both a record view and a table view listen for this event and the record view changes the selection before the table view tries to adjust it due to the insert (on the same selectionModel)
			// selection might become wrong (selection = 165 when only 164 records are available); so selectionModel needs to know; similar situations might happen for delete also
			boolean before = selectionModel.setFoundsetIsFiringSizeChangeTableAndListEvent(true);
			try
			{
				getTableAndListEventDelegate().fireTableAndListEvent(getFoundSetManager().getApplication(), firstRow, lastRow, type);
			}
			finally
			{
				selectionModel.setFoundsetIsFiringSizeChangeTableAndListEvent(before);
			}
		}
		else if (e.getType() == FoundSetEvent.CONTENTS_CHANGED)
		{
			getTableAndListEventDelegate().fireTableAndListEvent(getFoundSetManager().getApplication(), firstRow, lastRow, type);
		}
	}


	@Override
	public int getRowCount()
	{
		return getSize();
	}

	@Override
	public int getColumnCount()
	{
		//do nothing handled in CellAdapter
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		//do nothing handled in CellAdapter
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex)
	{
		return isRecordEditable(rowIndex);
	}

	@Override
	public void setElementAt(Object aValue, int rowIndex)
	{
		//not needed
	}

	@Override
	public Object getElementAt(int index)
	{
		return getRecord(index);
	}

	@Override
	public AlwaysRowSelectedSelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	protected TableAndListEventDelegate getTableAndListEventDelegate()
	{
		if (tableAndListEventDelegate == null) tableAndListEventDelegate = new TableAndListEventDelegate(this);
		return tableAndListEventDelegate;
	}

	@Override
	public void addListDataListener(ListDataListener l)
	{
		getTableAndListEventDelegate().addListDataListener(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.removeListDataListener(l);
		}
	}

	@Override
	public void fireTableModelEvent(int firstRow, int lastRow, int column, int type)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.fireTableModelEvent(firstRow, lastRow, column, type);
		}
	}

	private Map<String, List<Integer>> getPkCacheByDatasource(IQuerySelectValue[] pkColumns)
	{
		Map<String, List<Integer>> cache = pkByDatasourceCache.get(pkColumns);
		if (cache == null)
		{
			cache = new HashMap<>();
			Object[] pks = new Object[pkColumns.length];
			for (int j = records.size(); --j >= 0;)
			{
				ViewRecord record = records.get(j);
				for (int i = pkColumns.length; --i >= 0;)
				{
					pks[i] = record.getValue(columnNames.get(pkColumns[i]));
				}
				String pkHashKey = RowManager.createPKHashKey(pks);
				List<Integer> list = cache.get(pkHashKey);
				if (list == null)
				{
					cache.put(pkHashKey, Collections.singletonList(Integer.valueOf(j)));
				}
				else if (list.size() == 1)
				{
					List<Integer> copy = new ArrayList<>(list);
					copy.add(Integer.valueOf(j));
					cache.put(pkHashKey, copy);
				}
				else
				{
					list.add(Integer.valueOf(j));
				}
			}
			pkByDatasourceCache.put(pkColumns, cache);
		}
		return cache;
	}

	@Override
	public String toString()
	{
		return "ViewFoundset[size:" + records.size() + ", must refresh:" + refresh + ",hadMoreRows:" + hasMore + "]";
	}

	private class FoundSetIterator implements Iterator<IRecord>
	{
		private int currentIndex = -1;
		private Object[] currentPK = null;
		private final List<Object[]> processedPKS = new ArrayList<Object[]>();
		private IRecord currentRecord = null;

		public FoundSetIterator()
		{
		}

		@Override
		public boolean hasNext()
		{
			if (currentRecord == null)
			{
				currentRecord = getNextRecord();
			}
			return currentRecord != null;
		}

		@Override
		public IRecord next()
		{
			if (currentRecord == null)
			{
				currentRecord = getNextRecord();
			}
			IRecord returnRecord = currentRecord;
			currentRecord = null;
			return returnRecord;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		private IRecord getNextRecord()
		{

			int nextIndex = currentIndex + 1;
			IRecord nextRecord = getRecord(nextIndex);
			while (nextRecord instanceof PrototypeState)
			{
				nextIndex = currentIndex + 1;
				nextRecord = getRecord(nextIndex);
			}
			if (currentIndex >= 0)
			{
				IRecord tmpCurrentRecord = getRecord(currentIndex);
				if (tmpCurrentRecord == null || !Utils.equalObjects(tmpCurrentRecord.getPK(), currentPK))
				{
					// something is changed in the foundset, recalculate
					if (tmpCurrentRecord == null)
					{
						int size = records.size();
						if (size == 0)
						{
							return null;
						}
						currentIndex = size - 1;
						tmpCurrentRecord = getRecord(currentIndex);
					}
					if (!listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
					{
						// substract current index
						while (tmpCurrentRecord != null && !listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex - 1;
							tmpCurrentRecord = getRecord(currentIndex);
						}
						nextIndex = currentIndex + 1;
						nextRecord = getRecord(nextIndex);
					}
					else
					{
						// increment current index
						while (tmpCurrentRecord != null && listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex + 1;
							tmpCurrentRecord = getRecord(currentIndex);
						}
						nextIndex = currentIndex;
						nextRecord = tmpCurrentRecord;
					}
					if (nextRecord == null)
					{
						return null;
					}
				}
			}
			if (nextRecord != null)
			{
				currentPK = nextRecord.getPK();
			}
			currentIndex = nextIndex;
			processedPKS.add(currentPK);
			return nextRecord;
		}

		private boolean listContainsArray(List<Object[]> list, Object[] value)
		{
			if (list != null)
			{
				for (Object[] array : list)
				{
					if (Utils.equalObjects(array, value))
					{
						return true;
					}
				}
			}
			return false;
		}
	}

	private class RowListener implements IRowListener
	{
		private final String ds;
		private final IQuerySelectValue[] pkColumns;
		private final IQuerySelectValue[] columns;
		private final Set<String> columnInJoins;
		private final boolean monitorInserts;
		private final boolean monitorIDeletes;
		private final boolean monitorIDeletesForMain;

		public RowListener(String datasource, IQuerySelectValue[] queryPks, List<IQuerySelectValue> list, List<QueryColumn> columnInJoins,
			boolean monitorInserts, boolean monitorIDeletes, boolean monitorIDeletesForMain)
		{
			this.ds = datasource;
			this.pkColumns = queryPks;
			this.monitorInserts = monitorInserts;
			this.monitorIDeletes = monitorIDeletes;
			this.monitorIDeletesForMain = monitorIDeletesForMain;
			this.columns = list.toArray(new IQuerySelectValue[list.size()]);
			this.columnInJoins = columnInJoins != null ? columnInJoins.stream().map(QueryColumn::getName).collect(Collectors.toSet()) : Collections.emptySet();
		}

		@Override
		public void notifyChange(RowEvent e)
		{
			if (e.getPkHashKey() == null || refresh) return;

			boolean fullRefresh = false;
			if (e.getType() == RowEvent.UPDATE)
			{
				if (e.getChangedColumnNames() != null & this.columnInJoins.size() > 0)
				{
					if (Arrays.asList(e.getChangedColumnNames()).stream().anyMatch(colname -> this.columnInJoins.contains(colname)))
					{
						fullRefresh = true;
						// join or where condition hit, reload the foundset.
						doRefresh();
					}
				}
				if (!fullRefresh)
				{
					Map<String, List<Integer>> cacheByRow = getPkCacheByDatasource(pkColumns);
					List<Integer> rowIndexes = cacheByRow.get(e.getPkHashKey());
					if (rowIndexes != null)
					{
						// get the values directly from the row
						FireCollector fireCollector = FireCollector.getFireCollector();
						try
						{
							Row row = e.getRow();
							if (row != null)
							{
								for (IQuerySelectValue column : columns)
								{
									Object rowValue = row.getValue(column.getColumn().getName());
									for (Integer rowIndex : rowIndexes)
									{
										ViewRecord viewRecord = records.get(rowIndex.intValue());
										viewRecord.setValue(columnNames.get(column), rowValue);
									}
								}
							}
							else
							{
								// query for the values if needed.
								IQuerySelectValue[] columnsToQuery = this.columns;
								if (e.getChangedColumnNames() != null)
								{
									List<IQuerySelectValue> changed = new ArrayList<>(e.getChangedColumnNames().length);
									for (Object changedColumn : e.getChangedColumnNames())
									{
										for (IQuerySelectValue selectValue : columns)
										{
											if (selectValue.getColumn().getName().equals(changedColumn))
											{
												changed.add(selectValue);
												break;
											}
										}
									}
									columnsToQuery = new IQuerySelectValue[changed.size()];
									if (changed.size() > 0) columnsToQuery = changed.toArray(columnsToQuery);
								}
								if (columnsToQuery.length > 0)
								{
									try
									{
										IQueryBuilder queryBuilder = manager.getQueryFactory().createSelect(ds);
										for (IQuerySelectValue column : columnsToQuery)
										{
											queryBuilder.result().add(queryBuilder.getColumn(column.getColumn().getName()));
										}
										for (IQuerySelectValue pkColumn : pkColumns)
										{
											// just get the pk value from the first record (should be the same for all, because those records all have the same pkhash)
											queryBuilder.where().add(
												queryBuilder.getColumn(pkColumn.getColumn().getName()).eq(records.get(0).getValue(columnNames.get(pkColumn))));
										}
										ISQLSelect updateSelect = queryBuilder.build();
										String serverName = DataSourceUtils.getDataSourceServerName(select.getTable().getDataSource());
										String transaction_id = manager.getTransactionID(serverName);
										IDataSet updatedDS = manager.getApplication().getDataServer().performQuery(manager.getApplication().getClientID(),
											serverName, transaction_id, updateSelect, manager.getTableFilterParams(serverName, updateSelect), true, 0,
											currentChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
										if (updatedDS.getRowCount() > 0)
										{
											// should be just 1 row for a pk query...
											Object[] updateData = updatedDS.getRow(0);
											for (int i = columnsToQuery.length; --i >= 0;)
											{
												IQuerySelectValue column = columnsToQuery[i];
												Object rowValue = updateData[i];
												for (Integer rowIndex : rowIndexes)
												{
													ViewRecord viewRecord = records.get(rowIndex.intValue());
													viewRecord.setValue(columnNames.get(column), rowValue);
												}
											}
										}
									}
									catch (Exception e1)
									{
										Debug.error(e1);
									}
								}
							}
						}
						finally
						{
							fireCollector.done();
						}
					}
				}
			}
			else if ((e.getType() == RowEvent.DELETE && monitorIDeletes) || (e.getType() == RowEvent.INSERT && monitorInserts))
			{
				doRefresh();
			}
			else if (e.getType() == RowEvent.DELETE && monitorIDeletesForMain)
			{
				Map<String, List<Integer>> cacheByRow = getPkCacheByDatasource(pkColumns);
				List<Integer> rowIndexes = cacheByRow.get(e.getPkHashKey());
				if (rowIndexes != null)
				{
					rowIndexes.forEach((value) -> {
						records.remove(value.intValue());
						// this could be maybe done in 1 accumulated fire, but this should be only 1 (main table delete)
						fireFoundSetEvent(value.intValue(), value.intValue(), FoundSetEvent.CHANGE_DELETE);
					});
				}
			}
		}

		private void doRefresh()
		{
			if (foundSetEventListeners.size() > 0)
			{
				FireCollector fireCollector = FireCollector.getFireCollector();
				try
				{
					loadAllRecords();
				}
				catch (ServoyException e1)
				{
					Debug.error(e1);
				}
				finally
				{
					fireCollector.done();
				}
			}
			else
			{
				refresh = true;
			}
		}
	}
}
