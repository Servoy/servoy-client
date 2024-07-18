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

import static com.servoy.j2db.dataprocessing.FireCollector.getFireCollector;
import static com.servoy.j2db.query.AbstractBaseQuery.acceptVisitor;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.util.DataSourceUtils.createDBTableDataSource;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;
import static com.servoy.j2db.util.Utils.stream;
import static java.util.Arrays.asList;

import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.SymbolScriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.DerivedTable;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.impl.QBJoin;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.querybuilder.impl.QBTableClause;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.TypePredicate;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;
import com.servoy.j2db.util.visitor.SearchVisitor;

/**
 * @author jcompagner
 * @since 8.4
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ViewFoundSet", scriptingName = "ViewFoundSet")
public class ViewFoundSet extends AbstractTableModel implements ISwingFoundSet, IFoundSetScriptMethods, IConstantsObject, SymbolScriptable
{

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen also for column
	 * changes of the given table/datasource. This is used by default if you just use enableDatabroadcastFor()
	 * without flags. If you use the one with the flags you need to give this one if you just want to listen to
	 * column changes that are in the result for a given datasource and pk.
	 *
	 * This constants needs to have the pk's selected for the given datasource (should be in the results).
	 */
	public static final int MONITOR_COLUMNS = 1;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen also for column
	 * changes of the given table/datasource in the join statement - like order_lines.productid that has a join
	 * to orders and is displaying the productname. If a change in such a join condition (like
	 * order_lines.productid in the sample above) is seen then the query will be fired again to detect changes.
	 */
	public static final int MONITOR_JOIN_CONDITIONS = 2;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen also for column
	 * changes of the given table/datasource that are used in the where statement - like
	 * order_lines.unit_price > 100. If a change is seen on that datasource on such a column used in the where
	 * a full query will be fired again to detect changes.
	 */
	public static final int MONITOR_WHERE_CONDITIONS = 4;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for inserts on the
	 * given table/datasource. This will always result in a full query to detect changes whenever an insert on
	 * that table happens.
	 */
	public static final int MONITOR_INSERT = 8;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for deletes on the
	 * given table/datasource. This will always result in a full query to detect changes whenever an delete on
	 * that table happens.
	 */
	public static final int MONITOR_DELETES = 16;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for deletes on the
	 * given table/datasource which should be the primary/main table of this query. If a delete comes in for this
	 * table, then we will only remove the records from the ViewFoundSet that do have this primary key in its
	 * value. So no need to do a full query. So this will only work if the query shows order_lines for the
	 * order_lines table, not for the products table that is joined to get the product_name. Only 1 of the 2
	 * monitors for deletes should be registered for a table/datasource.
	 *
	 * This constants needs to have the pk's selected for the given datasource (should be in the results)
	 */
	public static final int MONITOR_DELETES_FOR_PRIMARY_TABLE = 32;

	/**
	 * Constant for the flags in {@link #enableDatabroadcastFor(QBTableClause, int)} to listen for changes in
	 * columns (selected) of the given datasource in the query that can affect aggregates. This means that when
	 * there are deletes, inserts or updates on columns selected from that datasource, a full re-query will
	 * happen to refresh the aggregates.
	 *
	 * IMPORTANT: in general, this flag should be set on (possible multiple) datasources from the query that
	 * have group by on their columns, and the columns don't contain the pk, or that have the actual aggregates
	 * on their columns (because all those could influence the value of aggregates).
	 *
	 * For example (ignoring the fact that in a real-life situation these fields might not change), a view
	 * foundset based on this query:
	 *
	 * SELECT orders.customerid, orders.orderdate, SUM(order_details.unitprice) FROM orders
	 *    LEFT OUTER JOIN order_details ON orders.orderid = order_details.orderid
	 *    GROUP BY orders.customerid, orders.orderdate
	 *	  ORDER BY orders.customerid asc, orders.orderdate desc
	 *
	 * will want to enable databroadcast flag MONITOR_AGGREGATES on both "orders" (because if "orderdate" or
	 * "customerid" - that are used in GROUP BY - change/are corrected on a row, that row could move from one
	 * group to the other, affecting the SUM(order_details.unitprice) for the groups involved) and "order_details"
	 * (because if "unitprice" changes/is corrected, the aggregate will be affected).
	 *
	 * But if the above query would also select the orders.odersid (and also group by that) then the orders row
	 * that you select for that sum will always be unique and only {@link #MONITOR_COLUMNS} has to be used for
	 * those - if needed.
	 */
	public static final int MONITOR_AGGREGATES = 64;

	public static final String VIEW_FOUNDSET = "ViewFoundSet";

	private static Callable symbol_iterator = (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
		return new IterableES6Iterator(scope, ((ViewFoundSet)thisObj));
	};

	protected transient AlwaysRowSelectedSelectionModel selectionModel;
	private transient TableAndListEventDelegate tableAndListEventDelegate;

	private final String datasource;
	private final IFoundSetManagerInternal manager;

	private final List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<>(3);
	// this is just a list to keep hard references to the RowListeners we give the RowManager (that is kept weak in there)
	private final List<IRowListener> rowListeners = new ArrayList<>(3);

	private List<ViewRecord> records = new ArrayList<>();
	private final List<ViewRecord> editedRecords = new ArrayList<>();
	private final List<ViewRecord> failedRecords = new ArrayList<ViewRecord>(2);

	private final List<WeakReference<IRecordInternal>> allParents = new ArrayList<>(6);

	private final Map<IQuerySelectValue[], Map<String, List<Integer>>> pkByDatasourceCache = new HashMap<>();

	private final Map<IQuerySelectValue, String> columnNames = new LinkedHashMap<>();


	private final QuerySelect select;

	private int foundsetID = 0;

	private boolean hasMore = false;
	private boolean refresh = true;
	private int currentChunkSize;
	private final int chunkSize;

	// forms might force their foundset to remain at a certain multiselect value
	// if a form 'pinned' multiselect, multiSelect should not be changeable by foundset JS access
	// if more then 1 form wishes to pin multiselect at a time, the form with lowest elementid wins
	private String multiSelectPinnedForm = null;
	private int multiSelectPinLevel;
	private final Map<BaseQueryTable, List<IQuerySelectValue>> pkColumnsForTable;
	private final Map<BaseQueryTable, List<IQuerySelectValue>> columnsForTable;

	private ITable table;

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
			String name = selectValue.getAliasOrName();
			IQuerySelectValue duplicate = nameToSelect.get(name);
			if (column != null && duplicate != null)
			{
				if (duplicate.getColumn().getTable() == baseTable)
				{
					BaseQueryTable colTable = column.getTable();
					name = (colTable.getAlias() != null ? colTable.getAlias() : colTable.getName()) + '_' + name;
					nameToSelect.put(name, selectValue);
					selectToName.put(selectValue, name);
				}
				else
				{
					nameToSelect.put(name, selectValue);
					selectToName.put(selectValue, name);
					BaseQueryTable colTable = duplicate.getColumn().getTable();
					name = (colTable.getAlias() != null ? colTable.getAlias() : colTable.getName()) + '_' + name;
					nameToSelect.put(name, selectValue);
					selectToName.put(selectValue, name);
				}
			}
			else
			{
				nameToSelect.put(name, selectValue);
				selectToName.put(selectValue, name);
			}

			select.getRealColumn(column).ifPresent(realColumn -> {
				Map<BaseQueryTable, List<IQuerySelectValue>> columnsMap;
				if ((realColumn.getFlags() & IBaseColumn.IDENT_COLUMNS) != 0)
				{
					columnsMap = pkColumnsForTable;
				}
				else
				{
					columnsMap = columnsForTable;
				}
				BaseQueryTable realcolumnTable = realColumn.getTable();
				List<IQuerySelectValue> list = columnsMap.get(realcolumnTable);
				if (list == null)
				{
					list = new ArrayList<>();
					columnsMap.put(realcolumnTable, list);
				}
				list.add(realColumn);
			});
		}

		for (IQuerySelectValue selectValue : select.getColumns())
		{
			IQuerySelectValue col;
			Optional<QueryColumn> realCol = select.getRealColumn(selectValue);
			if (realCol.isPresent())
			{
				col = realCol.get();
			}
			else
			{
				col = selectValue;
			}
			columnNames.put(col, selectToName.get(selectValue));
		}
	}

	private void addRealColumnToTableMap(Map<BaseQueryTable, List<QueryColumn>> columnsInJoinsPerTable, QueryColumn column)
	{
		select.getRealColumn(column).ifPresent(realColumn -> {
			BaseQueryTable realcolumnTable = realColumn.getTable();
			List<QueryColumn> list = columnsInJoinsPerTable.get(realcolumnTable);
			if (list == null)
			{
				list = new ArrayList<>();
				columnsInJoinsPerTable.put(realcolumnTable, list);
			}
			list.add(realColumn);
		});
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
				if (Objects.equals(realOrderedPks[i], selectValue.getColumnName()))
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
		return !shouldRefresh();
	}

	@Override
	public String[] getDataProviderNames(int type)
	{
		return null;
	}

	/**
	 * Returns the datasource (view:name) for this ViewFoundSet.
	 *
	 * @sample
	 * solutionModel.getForm("x").dataSource  = viewFoundSet.getDataSource();
	 */
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

	/**
	 * Get the number of records in this viewfoundset.
	 * This is the number of records loaded, note that when looping over a foundset, size() may
	 * increase as more records are loaded.
	 *
	 * @sample
	 * var nrRecords = %%prefix%%vfs.getSize()
	 *
	 * // to loop over foundset, recalculate size for each record
	 * for (var i = 1; i <= %%prefix%%foundset.getSize(); i++)
	 * {
	 * 	var rec = %%prefix%%vfs.getRecord(i);
	 * }
	 *
	 * @return int current size.
	 */
	@Override
	@JSFunction
	public int getSize()
	{
		if (shouldRefresh())
		{
			try
			{
				loadAllRecordsImpl();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
		return records.size();
	}

	/**
	 * Dispose and unregisters a view foundset from memory when is no longer needed.
	 * Returns whether foundset was disposed.
	 * If linked to visible form or component, view foundset cannot be disposed.
	 *
	 * Normally ViewFoundSets are not hold on to by the system, so if you only use this inside a method it will be disposed by itself.
	 * This method is then just helps by also calling clear()
	 *
	 * For ViewFoundSets that are also registered  by using true as the last argument in the call: databaseMananager.getViewFoundSet(name, query, boolean register)
	 * are hold on to by the system and Forms can use it for there foundset. Calling dispose on those will remove it from the system, so it is not usable anymore in forms.
	 *
	 * @sample
	 * 	%%prefix%%vfs.dispose();
	 *
	 *  @return boolean foundset was disposed
	 */
	@JSFunction
	public boolean dispose()
	{
		if (foundSetEventListeners.size() != 0)
		{
			Debug.warn("Cannot dispose view foundset, still linked to component, fs: " + this + ", listeners: " + foundSetEventListeners);
			return false;
		}
		if (tableAndListEventDelegate != null && !tableAndListEventDelegate.canDispose())
		{
			Debug.warn("Cannot dispose foundset, still linked to form UI, fs: " + this);
			return false;
		}
		clear();
		return getFoundSetManager().unregisterViewFoundSet(datasource);
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
		Scriptable scriptableFoundset = null;
		try
		{
			Context.enter();
			scriptableFoundset = (Scriptable)Context.javaToJS(this, this.getFoundSetManager().getApplication().getScriptEngine().getSolutionScope());
		}
		finally
		{
			Context.exit();
		}
		while (foundsetIterator.hasNext())
		{
			IRecord currentRecord = foundsetIterator.next();
			Object returnValue = callback.handleRecord(currentRecord, foundsetIterator.currentIndex, scriptableFoundset);
			if (returnValue != null && returnValue != Undefined.instance)
			{
				return returnValue;
			}
		}
		return null;
	}

	/**
	 * Databroadcast can be enabled per select table of a query, the select table can be the main QBSelect or on of it QBJoins
	 * By default this monitors only the column values that are in the result of the QBSelect, you can only enable this default monitoring for a table if for that table also the PK is selected in the results.
	 *
	 * you can use {@link #enableDatabroadcastFor(QBTableClause, int)} to specify what should be monitored more besides pure column values per pk.
	 * Those have impact on performance because for the most part if we see a hit then a full query is done to see if there are changes.
	 *
	 * @sample
	 *  var select = datasources.db.example_data.order_details.createSelect();
	 *  var join = select.joins.add("db:/example_data/products");
	 *  join.on.add(select.columns.productid.eq(join.columns.productid));
	 *  select.result.add(); // add columns of the select or join
	 *  var vf = databaseManager.getViewFoundSet("myorders",select)
	 *  vf.enableDatabroadcastFor(select);
	 *  vf.enableDatabroadcastFor(join);
	 *
	 * @param queryTable The QBSelect or QBJoin of a full query where this foundset should listen for data changes.
	 */
	@JSFunction
	public void enableDatabroadcastFor(QBTableClause queryTable)
	{
		enableDatabroadcastFor(queryTable, MONITOR_COLUMNS);
	}

	/**
	 * Enable the databroadcast for a specific table of the QBSelect or QBJoin with  flags for looking for join or where criteria or deletes/inserts.
	 * These  flags can be a performance hit because the query needs to be executed again to see if there are any changes.
	 * For certain flags {@link #MONITOR_COLUMNS} and {@link #MONITOR_DELETES_FOR_PRIMARY_TABLE} the pk for that table must be in the results.
	 *
	 * @sample
	 *  var select = datasources.db.example_data.order_details.createSelect();
	 *  var join = select.joins.add("db:/example_data/products");
	 *  join.on.add(select.columns.productid.eq(join.columns.productid));
	 *  select.result.add(); // add columns of the select or join
	 *  var vf = databaseManager.getViewFoundSet("myorders",select)
	 *  // monitor for the main table the join conditions (orders->product, when product id changes in the orders table) and requery the table on insert events, delete directly the record if a pk delete happens.
	 *  vf.enableDatabroadcastFor(select, ViewFoundSet.MONITOR_JOIN_CONDITIONS | ViewFoundSet.MONITOR_INSERT | ViewFoundSet.MONITOR_DELETES_FOR_PRIMARY_TABLE);
	 *  vf.enableDatabroadcastFor(join);
	 *
	 * @param queryTableclause The QBSelect or QBJoin of a full query where this foundset should listen for data changes.
	 * @param flags One or more of the ViewFoundSet.XXX flags added to each other.
	 */
	@JSFunction
	public void enableDatabroadcastFor(QBTableClause queryTableclause, int flags)
	{
		// dont do anything if there is nothing todo.
		if (flags == 0) return;


		SearchVisitor<QueryColumn> searchColumns = new SearchVisitor<>(new TypePredicate<>(QueryColumn.class));
		if ((flags & MONITOR_JOIN_CONDITIONS) == MONITOR_JOIN_CONDITIONS) acceptVisitor(select.getJoins(), searchColumns);
		if ((flags & MONITOR_WHERE_CONDITIONS) == MONITOR_WHERE_CONDITIONS) acceptVisitor(select.getWhere(), searchColumns);

		Map<BaseQueryTable, List<QueryColumn>> columnsInJoinsPerTable = new IdentityHashMap<>();
		searchColumns.getFound().forEach(column -> addRealColumnToTableMap(columnsInJoinsPerTable, column));

		boolean monitorInserts = (flags & MONITOR_INSERT) == MONITOR_INSERT;
		boolean monitorDeletes = (flags & MONITOR_DELETES) == MONITOR_DELETES;

		boolean monitorDeletesForMain = (flags & MONITOR_DELETES_FOR_PRIMARY_TABLE) == MONITOR_DELETES_FOR_PRIMARY_TABLE;
		boolean monitorColumns = (flags & MONITOR_COLUMNS) == MONITOR_COLUMNS;

		boolean monitorAggregates = (flags & MONITOR_AGGREGATES) == MONITOR_AGGREGATES;

		Collection<BaseQueryTable> tables = new ArrayList<>();
		if (queryTableclause instanceof QBSelect)
		{
			tables = asList(((QBSelect)queryTableclause).getQuery().getTable());
		}
		else if (queryTableclause instanceof QBJoin)
		{
			BaseQueryTable table = ((QBJoin)queryTableclause).getQueryTable();
			if (table.getDataSource() == null)
			{
				// derived table, get all tables used
				DerivedTable dt = (DerivedTable)((QBJoin)queryTableclause).getJoin().getForeignTableReference();
				tables = dt.getQuery().getColumns().stream() //
					.map(select::getRealColumn) //
					.filter(Optional::isPresent).map(Optional::get) //
					.map(QueryColumn::getTable)
					// reduce to unique identity hashmap
					.reduce(new IdentityHashMap<BaseQueryTable, BaseQueryTable>(), (map, col) -> {
						map.put(col, col);
						return map;
					}, (d, e) -> {
						e.putAll(d);
						return e;
					}).values();
			}
			else
			{
				tables = asList(table);
			}
		}

		tables.forEach(table -> {
			try
			{
				enableDatabroadcastFor(table, columnsInJoinsPerTable, monitorDeletesForMain, monitorColumns, monitorAggregates, monitorInserts, monitorDeletes);
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		});

	}

	private void enableDatabroadcastFor(BaseQueryTable table, Map<BaseQueryTable, List<QueryColumn>> columnsInJoinsPerTable, boolean monitorDeletesForMain,
		boolean monitorColumns, boolean monitorAggregates, boolean monitorInserts, boolean monitorDeletes) throws ServoyException
	{
		RowManager rowManager = manager.getRowManager(table.getDataSource());
		if (rowManager != null)
		{
			IQuerySelectValue[] queryPks = null;

			// for normal column watches or deletes of the main table we need to have the pk's in the select for the datasource.
			if (monitorDeletesForMain || monitorColumns)
			{
				List<IQuerySelectValue> list = pkColumnsForTable.get(table);
				if (list != null)
				{
					String[] realOrderedPks = rowManager.getSQLSheet().getPKColumnDataProvidersAsArray();
					queryPks = getOrderedPkColumns(list, realOrderedPks);
					if (queryPks == null)
					{
						throw new RuntimeException(
							"ViewFoundSets did get pks '" + list + "' for datasource " + table + " but they should be " + Arrays.toString(realOrderedPks));
					}
				}
				else
				{
					throw new RuntimeException("ViewFoundSet based on select: " + this.select + " does not have pk's selected from " + table.getDataSource() +
						" to enable databroadcast for that datasource");
				}
			}

			RowListener rl = new RowListener(table.getDataSource(), queryPks, (monitorColumns || monitorAggregates) ? columnsForTable.get(table) : null,
				columnsInJoinsPerTable.get(table), monitorInserts, monitorDeletes, monitorDeletesForMain, monitorAggregates);
			// keep a hard reference so as long as this ViewFoundSet lives the listener is kept in RowManager
			rowListeners.add(rl);
			rowManager.register(rl);

		}
	}

	void addEditedRecord(ViewRecord record)
	{
		failedRecords.remove(record);
		editedRecords.add(record);
	}

	/**
	 * Get the edited records of this view foundset.
	 * @sample
	 * var editedRecords = foundset.getEditedRecords();
	 * for (var i = 0; i < editedRecords.length; i++)
	 * {
	 *    application.output(editedRecords[i]);
	 * }
	 * @return an array of edited records
	 */
	@JSFunction
	public ViewRecord[] getEditedRecords()
	{
		return editedRecords.toArray(new ViewRecord[editedRecords.size()]);
	}


	/**
	 * Saves all records in the view foundset that have changes.
	 * You can only save columns from a table if the pks of that table are also selected by the view foundset's query.
	 *
	 * @return true if the save was successfull, false if not and then the record will hav the exception set.
	 */
	@JSFunction
	public boolean save()
	{
		return doSave(null) == ISaveConstants.STOPPED;
	}

	/**
	 * Validates the given record, it runs first the method that is attached to the entity event "onValidate".
	 * Those methods do get a parameter JSRecordMarkers where the problems can be reported against.
	 * All columns are then also null/empty checked and if they are and the Column is marked as "not null" an error will be
	 * added with the message key "servoy.record.error.null.not.allowed" for that column.
	 *
	 * An extra state object can be given that will also be passed around if you want to have more state in the validation objects
	 * (like giving some ui state so the entity methods know where you come from)
	 *
	 * It will return a JSRecordMarkers when the record had validation problems
	 *
	 * @param record
	 *
	 * @return Returns a JSRecordMarkers if the record has validation problems
	 */
	@JSFunction
	public JSRecordMarkers validate(ViewRecord record)
	{
		return validate(record, null);
	}

	/**
	 * @clonedesc validate(ViewRecord)
	 *
	 * @sampleas validate(ViewRecord)
	 * 	 *
	 * @param record The ViewRecord to validate
	 * @param customObject An extra customObject to give to the validate method.
	 */
	@JSFunction
	public JSRecordMarkers validate(ViewRecord record, Object customObject)
	{
		return manager.validateRecord(record, customObject);
	}

	/**
	 * Saved a specific record of this foundset.
	 * You can only save columns from a table if also the pk is selected of that table
	 *
	 * @return true if the save was successfull, false if not and then the record will hav the exception set.
	 *
	 * @param record
	 */
	@JSFunction
	public boolean save(ViewRecord record)
	{
		if (record != null && record.getParentFoundSet() != this) return false;
		return doSave(record) == ISaveConstants.STOPPED;
	}

	/**
	 * Sorts the foundset based on the given sort string.
	 * Column in sort string must already exist in ViewFoundset.
	 *
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 */
	@JSFunction
	public void sort(String sortString) throws ServoyException
	{
		sort(getFoundSetManager().getSortColumns(getTable(), sortString), false);
	}

	/**
	 * Sorts the foundset based on the given sort string.
	 * Column in sort string must already exist in ViewFoundset.
	 *
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 * @param defer boolean when true, the "sortString" will be just stored, without performing a query on the database (the actual sorting will be deferred until the next data loading action).
	 */
	@JSFunction
	public void sort(String sortString, Boolean defer) throws ServoyException
	{
		sort(getFoundSetManager().getSortColumns(getTable(), sortString), defer == null ? false : defer.booleanValue());
	}

	/**
	 * Get the last sort columns that were set using viewfoundset sort api.s
	 *
	 * @sample
	 * //reverse the current sort
	 *
	 * //the original sort "companyName asc, companyContact desc"
	 * //the inversed sort "companyName desc, companyContact asc"
	 * var foundsetSort = foundset.getCurrentSort()
	 * var sortColumns = foundsetSort.split(',')
	 * var newFoundsetSort = ''
	 * for(var i=0; i<sortColumns.length; i++)
	 * {
	 * 	var currentSort = sortColumns[i]
	 * 	var sortType = currentSort.substring(currentSort.length-3)
	 * 	if(sortType.equalsIgnoreCase('asc'))
	 * 	{
	 * 		newFoundsetSort += currentSort.replace(' asc', ' desc')
	 * 	}
	 * 	else
	 * 	{
	 * 		newFoundsetSort += currentSort.replace(' desc', ' asc')
	 * 	}
	 * 	if(i != sortColumns.length - 1)
	 * 	{
	 * 		newFoundsetSort += ','
	 * 	}
	 * }
	 * foundset.sort(newFoundsetSort)
	 *
	 * @return String sort columns
	 */
	@JSFunction
	public String getCurrentSort()
	{
		return getSort();
	}

	int doSave(ViewRecord record)
	{
		int retCode = ISaveConstants.STOPPED;
		List<ViewRecord> toSave = new ArrayList<>();
		if (record == null)
		{
			toSave.addAll(editedRecords);
		}
		else
		{
			if (record.isEditing()) toSave.add(record);
		}

		if (toSave.size() > 0)
		{
			ArrayList<ViewRecord> processedRecords = new ArrayList<ViewRecord>();
			try
			{
				boolean previousRefresh = refresh;
				String serverName = getDataSourceServerName(select.getTable().getDataSource());
				String transaction_id = manager.getTransactionID(serverName);

				HashMap<SQLStatement, ViewRecord> statementToRecord = new HashMap<>();
				List<SQLStatement> statements = new ArrayList<>();

				for (ViewRecord rec : toSave)
				{
					Map<String, Object> changes = rec.getChanges();
					// directly just remove it from the edited records if we try to save it.
					editedRecords.remove(rec);
					if (changes == null) continue;

					Map<BaseQueryTable, Map<QueryColumn, Object>> tableToChanges = new IdentityHashMap<>();
					columnNames.forEach((selectValue, name) -> {
						if (changes.containsKey(name))
						{
							QueryColumn realColumn = select.getRealColumn(selectValue).orElseThrow(() -> {
								RuntimeException ex = new RuntimeException(
									"Can't save " + rec + " for changed values " + changes + " because table for column '" + name + "' cannot be found");
								rec.setLastException(ex);
								if (!failedRecords.contains(rec)) failedRecords.add(rec);
								return ex;
							});
							BaseQueryTable table = realColumn.getTable();
							Map<QueryColumn, Object> map = tableToChanges.get(table);
							if (map == null)
							{
								map = new HashMap<>();
								tableToChanges.put(table, map);
							}
							map.put(realColumn, rec.getValue(name));
						}
					});
					tableToChanges.forEach((table, changesMap) -> {
						List<IQuerySelectValue> pkColumns = pkColumnsForTable.get(table);
						if (pkColumns == null)
						{
							RuntimeException ex = new RuntimeException("Can't save " + rec + " for changed values " + changes +
								" because there are no pk's found for table with changes " + table.getAlias() != null ? table.getAlias() : table.getName());
							rec.setLastException(ex);
							if (!failedRecords.contains(rec)) failedRecords.add(rec);
							throw ex;
						}

						int counter = 0;
						Object[] pk = new Object[pkColumns.size()];
						QueryUpdate update = new QueryUpdate(table);

						IQuerySelectValue[] queryPks = null;
						try
						{
							RowManager rowManager = manager.getRowManager(table.getDataSource());
							if (rowManager != null)
							{
								queryPks = getOrderedPkColumns(pkColumns, rowManager.getSQLSheet().getPKColumnDataProvidersAsArray());
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
						if (queryPks == null) queryPks = pkColumns.toArray(new IQuerySelectValue[0]);
						for (IQuerySelectValue pkColumn : queryPks)
						{
							Object pkValue = rec.getValue(columnNames.get(pkColumn));
							update.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, pkColumn, pkValue));
							pk[counter++] = pkValue;
						}

						IDataSet pks = new BufferedDataSet();
						pks.addRow(pk);

						counter = 0;
						String[] changedColumns = new String[changes.size()];
						for (Entry<QueryColumn, Object> entry : changesMap.entrySet())
						{
							QueryColumn column = entry.getKey();
							update.addValue(column, entry.getValue());
							changedColumns[counter++] = column.getName();
						}

						SQLStatement statement = new SQLStatement(ISQLActionTypes.UPDATE_ACTION, serverName, table.getName(), pks, transaction_id, update,
							manager.getTableFilterParams(serverName, update));
						statement.setChangedColumns(changedColumns);
						statement.setExpectedUpdateCount(1);
						statements.add(statement);
						statementToRecord.put(statement, rec);
					});
					JSRecordMarkers validateObject = validate(rec);
					if (validateObject != null && validateObject.isHasErrors())
					{
						Object[] genericExceptions = validateObject.getGenericExceptions();
						if (genericExceptions.length > 0)
						{
							rec.setLastException((Exception)genericExceptions[0]);
						}
						if (!failedRecords.contains(rec))
						{
							failedRecords.add(rec);
							retCode = ISaveConstants.SAVE_FAILED;
						}
					}

					if (!failedRecords.contains(rec))
					{
						processedRecords.add(rec);
					}
				}

				if (toSave.size() > 1 && failedRecords.isEmpty() || //if this is a save all call we don't save if we have failed records
					toSave.size() == 1 && !failedRecords.contains(record))//if this is a single record save, we just check if it is failed or not
				{

					Object[] updateResult = manager.getApplication().getDataServer().performUpdates(manager.getApplication().getClientID(),
						statements.toArray(new SQLStatement[statements.size()]));
					for (int i = 0; i < updateResult.length; i++)
					{
						ViewRecord rec = statementToRecord.remove(statements.get(i)); // i of the updateResults should be the same for the statements;
						Object o = updateResult[i];
						if (o instanceof Exception)
						{
							// something went wrong
							failedRecords.add(rec);
							rec.setLastException((Exception)o);
							retCode = ISaveConstants.SAVE_FAILED;
						}
						else if (!statementToRecord.values().contains(rec) && !failedRecords.contains(rec))
						{
							rec.clearChanges();
						}
					}

					// TODO what happens if the save failed for some? add the changes back in?

					for (SQLStatement statement : statements)
					{
						manager.notifyDataChange(createDBTableDataSource(statement.getServerName(), statement.getTableName()),
							statement.getPKsRow(0), ISQLActionTypes.UPDATE_ACTION, statement.getChangedColumns());
					}

					// if we should have refreshed before this save and it is still in refresh mode (refresh is true and no editted records anymore)
					// do a load but only if there are listeners
					if (previousRefresh && shouldRefresh() && foundSetEventListeners.size() > 0)
					{
						loadAllRecordsImpl();
					}
				}
			}
			catch (ServoyException | RemoteException e)
			{
				Debug.error(e);
			}
			finally
			{
				if (!failedRecords.isEmpty())
				{
					processedRecords.stream().forEachOrdered(editedRecords::add);
				}
			}
		}
		return retCode;
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

	/**
	 * This will reload the current set of ViewRecords in this foundset, resetting the chunk size back to the start (default 200).
	 * All edited records will be discarded! So this can be seen as a full clean up of this ViewFoundSet.
	 */
	@Override
	@JSFunction
	public void loadAllRecords() throws ServoyException
	{
		currentChunkSize = chunkSize;
		if (editedRecords.size() > 0)
		{
			// if there are editing records and load all is called, then just remove all changes
			editedRecords.stream().forEach(edited -> edited.clearChanges());
			editedRecords.clear();
		}
		if (failedRecords.size() > 0)
		{
			// if there are failed records and load all is called, then just remove all changes
			failedRecords.stream().forEach(failed -> failed.clearChanges());
			failedRecords.clear();
		}
		loadAllRecordsImpl();
	}

	private void loadAllRecordsImpl() throws ServoyException
	{
		String serverName = getDataSourceServerName(select.getTable().getDataSource());
		String transaction_id = manager.getTransactionID(serverName);
		try
		{
			IDataSet ds = manager.getApplication().getDataServer().performQuery(manager.getApplication().getClientID(), serverName, transaction_id, select,
				null, manager.getTableFilterParams(serverName, select), select.isUnique(), 0, currentChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
			refresh = false;
			int currentSize = records.size();
			List<ViewRecord> old = records;
			records = new ArrayList<>(ds.getRowCount());
			pkByDatasourceCache.clear();

			String[] colNames = columnNames.values().toArray(new String[columnNames.size()]);

			try (FireCollector fireCollector = getFireCollector())
			{
				for (int i = 0; i < ds.getRowCount(); i++)
				{
					Object[] rowData = ds.getRow(i);
					if (i < currentSize)
					{
						ViewRecord current = old.get(i);
						records.add(current);
						current.updateValues(colNames, rowData);
					}
					else
					{
						records.add(new ViewRecord(colNames, rowData, this));
					}
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
	public void pinMultiSelectIfNeeded(boolean multiSelect, String formName, int pinLevel)
	{
		if (multiSelectPinnedForm == null)
		{
			// no current pinning; just pin
			multiSelectPinLevel = pinLevel;
			multiSelectPinnedForm = formName;
			setMultiSelectInternal(multiSelect);
		}
		else if (pinLevel < multiSelectPinLevel)
		{
			// current pin was for hidden form, this is a visible form
			multiSelectPinLevel = pinLevel;
			if (multiSelectPinnedForm != formName)
			{
				multiSelectPinnedForm = formName;
				setMultiSelectInternal(multiSelect);
			}
		}
		else if (pinLevel == multiSelectPinLevel)
		{
			// same pin level, different forms; always choose one with lowest "name"
			if (formName.compareTo(multiSelectPinnedForm) < 0)
			{
				multiSelectPinnedForm = formName;
				setMultiSelectInternal(multiSelect);
			}
		}
		else if (formName == multiSelectPinnedForm) // && (pinLevel > multiSelectPinLevel) implied
		{
			// pinlevel is higher then current; if this is the current pinned form, update the pin level
			// maybe other visible forms using this foundset want to pin selection mode in this case (visible pinning form became hidden)
			multiSelectPinLevel = pinLevel;
			fireSelectionModeChange();
		}
	}

	public void unpinMultiSelectIfNeeded(String formName)
	{
		if (multiSelectPinnedForm == formName)
		{
			multiSelectPinnedForm = null;
			fireSelectionModeChange(); // this allows any other forms that might be currently using this foundset to apply their own selectionMode to it
		}
	}

	@Override
	@JSGetter
	public void setMultiSelect(boolean multiSelect)
	{
		if (multiSelectPinnedForm == null) setMultiSelectInternal(multiSelect); // if a form is currently overriding this, ignore js call
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

	/**
	 * Get the selected records.
	 * When the viewfoundset is in multiSelect mode (see property multiSelect), selection can be a more than 1 record.
	 *
	 * @sample var selectedRecords = %%prefix%%foundset.getSelectedRecords();
	 * @return Array current records.
	 */
	@JSFunction
	public ViewRecord[] getSelectedRecords()
	{
		int[] selectedIndexes = getSelectedIndexes();
		List<ViewRecord> selectedRecords = new ArrayList<ViewRecord>(selectedIndexes.length);
		for (int index : selectedIndexes)
		{
			ViewRecord record = getRecord(index);
			if (record != null)
			{
				selectedRecords.add(record);
			}
		}

		return selectedRecords.toArray(new ViewRecord[selectedRecords.size()]);
	}


	@Override
	public String getSort()
	{
		return FoundSetManager.getSortColumnsAsString(determineSortColumns());
	}

	@Override
	public void setSort(String sortString) throws ServoyException
	{
		sort(getFoundSetManager().getSortColumns(getTable(), sortString), false);
	}

	/**
	 * Get the cloned query that created this ViewFoundSset (modifying this QBSelect will not change the foundset).
	 * The ViewFoundSets main query can't be altered after creation; you need to make a new ViewFoundSet for that (it can have the same datasource name).
	 *
	 * @sample
	 * var q = foundset.getQuery()
	 * q.where.add(q.columns.x.eq(100))
	 * var newVF = databaseManager.getViewFoundset("name", q);
	 *
	 * @return query.
	 */
	@JSFunction
	public QBSelect getQuery()
	{
		QuerySelect query = deepClone(this.select, true);
		IApplication application = manager.getApplication();
		String serverName = getDataSourceServerName(select.getTable().getDataSource());
		// Use the server from the original query but leave the table null. The QueryBuilder will use columns from the query.result in that case.
		String queryDataSource = createDBTableDataSource(serverName, null);

		return new QBSelect(manager, manager.getScopesScopeProvider(), application.getFlattenedSolution(), application.getScriptEngine().getSolutionScope(),
			queryDataSource, null, query);
	}

	@Override
	public ISQLSelect getQuerySelectForReading()
	{
		return select;
	}

	@Override
	public QuerySelect getCurrentStateQuery(boolean reduceSearch, boolean clone) throws ServoyException
	{
		return clone ? deepClone(this.select, true) : this.select;
	}

	@Override
	public boolean loadByQuery(IQueryBuilder query) throws ServoyException
	{
		return false;
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
		sort(sortColumns, false);
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


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#containsAggregate(java.lang.String)
	 */
	@Override
	public boolean containsAggregate(String name)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#containsCalculation(java.lang.String)
	 */
	@Override
	public boolean containsCalculation(String dataProviderID)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getColumnIndex(java.lang.String)
	 */
	@Override
	public int getColumnIndex(String dataProviderID)
	{
		int index = 0;
		for (String columnName : columnNames.values())
		{
			if (columnName.equals(dataProviderID))
			{
				return index;
			}
			index++;
		}
		return -1;
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
		Relation[] relationSequence = getFoundSetManager().getApplication().getFlattenedSolution().getRelationSequence(name);
		if (relationSequence != null && relationSequence.length > 0 && !relationSequence[0].isGlobal() &&
			!relationSequence[0].getPrimaryDataSource().equals(getDataSource()))
		{
			getFoundSetManager().getApplication().reportJSWarning("An incorrect child relation (" + relationSequence[0].getName() +
				") was accessed through a foundset (or a record of foundset) with datasource '" + getDataSource() + "'. The accessed relation actually has '" +
				relationSequence[0].getPrimaryDataSource() +
				"' as primary datasource. It will resolve for legacy reasons but please fix it as it is error prone.", new ServoyException());
		}
		return relationSequence != null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getRelatedFoundSet(com.servoy.j2db.dataprocessing.IRecordInternal, java.lang.String,
	 * java.util.List)
	 */
	@Override
	public IFoundSetInternal getRelatedFoundSet(IRecordInternal record, String fullRelationName, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (fullRelationName == null)
		{
			return null;
		}
		IFoundSetInternal retval = null;
		IRecordInternal currentRecord = record;
		String[] parts = fullRelationName.split("\\."); //$NON-NLS-1$
		for (int i = 0; i < parts.length; i++)
		{
			// if this is a findstate and that is not the source record then leave the relation lookup to the findstate itself.
			if (currentRecord instanceof FindState && i != 0)
			{
				String leftPart = parts[i];
				for (int k = i + 1; k < parts.length; k++)
				{
					leftPart += "." + parts[k]; //$NON-NLS-1$
				}
				return currentRecord.getRelatedFoundSet(leftPart);
			}

			RowManager rowManager = manager.getRowManager(getTable().getDataSource());
			SQLSheet relatedSheet = rowManager == null ? null
				: rowManager.getSQLSheet().getRelatedSheet(getFoundSetManager().getApplication().getFlattenedSolution().getRelation(parts[i]),
					getFoundSetManager().getSQLGenerator());
			if (relatedSheet == null)
			{
				retval = getFoundSetManager().getGlobalRelatedFoundSet(parts[i]);
			}
			else
			{
				retval = ((FoundSetManager)getFoundSetManager()).getRelatedFoundSet(currentRecord, relatedSheet, parts[i], defaultSortColumns);
				if (retval != null)
				{
					if (retval.getSize() == 0 && !currentRecord.existInDataSource())
					{
						Relation r = getFoundSetManager().getApplication().getFlattenedSolution().getRelation(parts[i]);
						if (r != null && r.isExactPKRef(getFoundSetManager().getApplication().getFlattenedSolution()))//TODO add unique column test instead of pk requirement
						{
							((FoundSet)retval).newRecord(record.getRawData(), 0, true, false);
						}
					}
					retval.addParent(currentRecord);
				}
			}
			if (retval == null)
			{
				return null;
			}
			if (i < parts.length - 1)
			{
				currentRecord = retval.getRecord(retval.getSelectedIndex());
				if (currentRecord == null)
				{
					return null;
				}
			}
		}
		return retval;
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
		if (!defer && doSave(null) != ISaveConstants.STOPPED)
		{
			manager.getApplication().reportJSError("Couldn't do a sort because there are edited records on this foundset: " + this, null); //$NON-NLS-1$
			return;
		}
		this.select.clearSorts();
		if (sortColumns != null) this.select.setSorts((ArrayList< ? extends IQuerySort>)sortColumns.stream()
			.map(sort -> new QuerySort(((Column)sort.getColumn()).queryColumn(this.select.getTable()), sort.getSortOrder() == SortColumn.ASCENDING,
				manager.getSortOptions(sort.getColumn())))
			.collect(Collectors.toList()));
		if (!defer) this.loadAllRecordsImpl();
		else hasMore = false;

	}

	@Override
	public void sort(Comparator<Object[]> recordPKComparator)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public List<SortColumn> getSortColumns()
	{
		return determineSortColumns();
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

	private List<SortColumn> determineSortColumns()
	{
		List<IQuerySort> sorts = select.getSorts();
		if (sorts != null)
		{
			return sorts.stream().filter(QuerySort.class::isInstance).map(QuerySort.class::cast)
				.map(sort -> {
					String name = sort.getColumn().getAliasOrName();
					IColumn column = getTable().getColumnBySqlname(name);
					if (column != null)
					{
						return new SortColumn(column, sort.isAscending() ? SortColumn.ASCENDING : SortColumn.DESCENDING);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}
		return null;
	}

	@Override
	public IFoundSetManagerInternal getFoundSetManager()
	{
		return manager;
	}

	@Override
	public ITable getTable()
	{
		if (table == null)
		{
			try
			{
				table = manager.getTable(getDataSource());
				if (table == null)
				{
					table = new Table(IServer.VIEW_SERVER, DataSourceUtils.getViewDataSourceName(getDataSource()), true, ITable.VIEW, null, null);
					((Table)table).setDataSource(getDataSource());
					for (IQuerySelectValue col : select.getColumns())
					{
						Column newCol = null;

						QueryColumn qCol = col.getColumn();
						if (qCol != null && qCol.getTable() != null)
						{
							ITable colTable = manager.getTable(qCol.getTable().getDataSource());
							if (colTable != null)
							{
								Column column = colTable.getColumn(qCol.getName());
								if (column != null)
								{
									String colname = getColunmName(col, qCol);
									newCol = table.createNewColumn(DummyValidator.INSTANCE, colname, column.getType(), column.getLength(), column.getScale(),
										column.getAllowNull());
									if (column.getColumnInfo() != null)
									{
										DatabaseUtils.createNewColumnInfo(
											manager.getApplication().getFlattenedSolution().getPersistFactory().getNewElementID(null), newCol, false);
										newCol.getColumnInfo().copyFrom(column.getColumnInfo());
									}
								}
							}
						}

						if (newCol == null)
						{
							// existing database column not found, create column on the fly
							BaseColumnType columnType = col.getColumnType();
							if (columnType == null)
							{
								columnType = ColumnType.getColumnType(IColumnTypes.TEXT);
							}

							String colname = getColunmName(col, qCol);

							table.createNewColumn(DummyValidator.INSTANCE, colname, columnType.getSqlType(), columnType.getLength(), columnType.getScale(),
								true);
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		return table;
	}

	/**
	 * @param col
	 * @param qCol
	 * @return
	 */
	private String getColunmName(IQuerySelectValue col, QueryColumn qCol)
	{
		String colname = columnNames.get(qCol != null ? qCol : col);
		if (colname == null) colname = columnNames.get(col);
		if (colname == null) colname = col.getAliasOrName();
		return colname;
	}

	@Override
	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create a copy from the  current record of a View Foundset of datasource " + this.datasource);
	}

	@Override
	public IFoundSetInternal copy(boolean unrelate) throws ServoyException
	{
		ViewFoundSet viewFoundSetCopy = new ViewFoundSet(datasource, deepClone(this.select, true), manager, chunkSize);
		manager.registerViewFoundSet(viewFoundSetCopy, true);
		return viewFoundSetCopy;
	}

	/**
	 * Get the ViewRecord object at the given index.
	 * Argument "index" is 1 based (so first record is 1).
	 *
	 * @sample var record = %%prefix%%vfs.getRecord(index);
	 *
	 * @param index int record index (1 based).
	 *
	 * @return ViewRecord record.
	 */
	public ViewRecord js_getRecord(int row)
	{
		return getRecord(row - 1);
	}

	/**
	 * Get the ViewRecord from the primary key values.
	 *
	 * @sample var record = %%prefix%%vfs.getRecordByPk(1);  // or getRecordByPk(1,2) or ([1,2]) for multicolumn pk
	 *
	 * @param pk pk values as array
	 *
	 * @return ViewRecord record.
	 */
	public ViewRecord js_getRecordByPk(Object... pk)
	{
		return (ViewRecord)getRecord(pk);
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
		// never query for more if there are edited records.
		boolean queryForMore = hasMore && (maxIndex == records.size() - 1) && editedRecords.size() == 0;
		if (shouldRefresh() || queryForMore)
		{
			try
			{
				if (queryForMore) currentChunkSize += chunkSize;
				loadAllRecordsImpl();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
	}

	private boolean shouldRefresh()
	{
		return refresh && editedRecords.size() == 0 && failedRecords.size() == 0;
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
	public int getIDInternal()
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
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return isRecordEditable(rowIndex);
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
				for (int i = 0; i < pkColumns.length; i++)
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
		return "ViewFoundset[size:" + records.size() + ", must refresh:" + refresh + ", has editted records:" + editedRecords.size() + ", hadMoreRows:" +
			hasMore + "]";
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
		private final boolean monitorDeletes;
		private final boolean monitorDeletesForMain;
		private final boolean monitorAggregates;

		public RowListener(String datasource, IQuerySelectValue[] queryPks, List<IQuerySelectValue> list, List<QueryColumn> columnInJoins,
			boolean monitorInserts, boolean monitorDeletes, boolean monitorDeletesForMain, boolean monitorAggregates)
		{
			this.ds = datasource;
			this.pkColumns = queryPks;
			this.monitorInserts = monitorInserts;
			this.monitorDeletes = monitorDeletes;
			this.monitorDeletesForMain = monitorDeletesForMain;
			this.monitorAggregates = monitorAggregates && list != null && list.size() > 0;
			this.columns = list != null ? list.toArray(new IQuerySelectValue[list.size()]) : null;
			this.columnInJoins = columnInJoins != null ? columnInJoins.stream().map(QueryColumn::getName).collect(Collectors.toSet()) : Collections.emptySet();
		}

		@Override
		public void notifyChange(RowEvent e)
		{
			if (e.getPkHashKey() == null || refresh) return;

			boolean fullRefresh = false;
			if (e.getType() == RowEvent.UPDATE)
			{
				if (e.getChangedColumnNames() != null)
				{
					if (stream(e.getChangedColumnNames()).anyMatch(this.columnInJoins::contains))
					{
						fullRefresh = doRefresh();
					}
					if (!fullRefresh && monitorAggregates)
					{
						List<Object> names = asList(e.getChangedColumnNames());
						if (stream(this.columns).map(IQuerySelectValue::getColumnName).anyMatch(names::contains))
						{
							fullRefresh = doRefresh();
						}
					}
				}
				if (!fullRefresh && pkColumns != null && columns != null)
				{
					Map<String, List<Integer>> cacheByRow = getPkCacheByDatasource(pkColumns);
					List<Integer> rowIndexes = cacheByRow.get(e.getPkHashKey());
					if (rowIndexes != null)
					{
						// get the values directly from the row
						try (FireCollector fireCollector = getFireCollector())
						{
							Row row = e.getRow();
							if (row != null)
							{
								for (IQuerySelectValue column : columns)
								{
									Object rowValue = row.getValue(column.getColumnName());
									for (Integer rowIndex : rowIndexes)
									{
										ViewRecord viewRecord = records.get(rowIndex.intValue());
										viewRecord.setValueImpl(columnNames.get(column), rowValue);
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
											if (Objects.equals(changedColumn, selectValue.getColumnName()))
											{
												changed.add(selectValue);
												break;
											}
										}
									}
									columnsToQuery = changed.toArray(new IQuerySelectValue[changed.size()]);
								}
								if (columnsToQuery.length > 0)
								{
									try
									{
										IQueryBuilder queryBuilder = manager.getQueryFactory().createSelect(ds);
										for (IQuerySelectValue column : columnsToQuery)
										{
											queryBuilder.result().add(queryBuilder.getColumn(column.getColumnName()));
										}
										for (IQuerySelectValue pkColumn : pkColumns)
										{
											// just get the pk value from the first record (should be the same for all, because those records all have the same pkhash)
											queryBuilder.where().add(queryBuilder.getColumn(pkColumn.getColumnName()).eq(
												records.get(rowIndexes.get(0).intValue()).getValue(columnNames.get(pkColumn))));
										}
										ISQLSelect updateSelect = queryBuilder.build();
										String serverName = getDataSourceServerName(select.getTable().getDataSource());
										String transaction_id = manager.getTransactionID(serverName);
										IDataSet updatedDS = manager.getApplication().getDataServer().performQuery(manager.getApplication().getClientID(),
											serverName, transaction_id, updateSelect, null, manager.getTableFilterParams(serverName, updateSelect), true, 0,
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
													viewRecord.setValueImpl(columnNames.get(column), rowValue);
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
					}
				}
			}
			else if ((e.getType() == RowEvent.DELETE && (monitorDeletes || monitorAggregates)) ||
				(e.getType() == RowEvent.INSERT && (monitorInserts || monitorAggregates)))
			{
				doRefresh();
			}
			else if (e.getType() == RowEvent.DELETE && monitorDeletesForMain)
			{
				// if monitor for deletes is enabled then pkColumns should be there.
				Map<String, List<Integer>> cacheByRow = getPkCacheByDatasource(pkColumns);
				List<Integer> rowIndexes = cacheByRow.get(e.getPkHashKey());
				if (rowIndexes != null)
				{
					for (int i = rowIndexes.size(); i-- > 0;)
					{
						Integer value = rowIndexes.get(i);
						records.remove(value.intValue());
						// this could be maybe done in 1 accumulated fire, but this should be only 1 (main table delete)
						fireFoundSetEvent(value.intValue(), value.intValue(), FoundSetEvent.CHANGE_DELETE);
					}
				}
			}
		}

		private boolean doRefresh()
		{
			if (foundSetEventListeners.size() > 0)
			{
				if (editedRecords.size() > 0)
				{
					// if there are edited records then don't do a load-all but just set the refresh to true.
					// return false so that it isn't seen as a full refresh and we try to update it otherwise.
					refresh = true;
					return false;
				}
				try (FireCollector fireCollector = getFireCollector())
				{
					loadAllRecordsImpl();
				}
				catch (ServoyException e1)
				{
					Debug.error(e1);
				}
			}
			else
			{
				refresh = true;
			}
			return true;
		}
	}

	/**
	 * Revert changes of the provided view records.
	 * @param rec an array of view records
	 */
	@JSFunction
	public void revertEditedRecords(ViewRecord[] rec)
	{
		if (rec != null)
		{
			Arrays.stream(rec).forEach(r -> editedRecords.remove(r.revertChangesImpl()));
		}
	}

	/**
	 * Revert changes of all unsaved view records of the view foundset.
	 */
	@JSFunction
	public void revertEditedRecords()
	{
		editedRecords.stream().forEach(r -> r.revertChangesImpl());
		editedRecords.clear();
	}

	/**
	 * Returns true if the viewfoundset has records.
	 * @return true if the viewfoundset has records.
	 */
	@JSFunction
	public boolean hasRecords()
	{
		return getSize() > 0;
	}

	/**
	 * Get the records which could not be saved.
	 * @return an array of failed records
	 */
	@JSFunction
	public ViewRecord[] getFailedRecords()
	{
		return failedRecords.toArray(new ViewRecord[failedRecords.size()]);
	}

	/**
	 * Get a duplicate of the viewfoundset. This is a full copy of the view foundset.
	 *
	 * @sample
	 * var dupFoundset = %%prefix%%foundset.duplicateFoundSet();
	 *
	 * @return foundset duplicate.
	 */
	@JSFunction
	public ViewFoundSet duplicateFoundSet() throws ServoyException
	{
		return (ViewFoundSet)copy(false);
	}

	/**
	 * Get foundset name. If foundset is not named foundset will return null.
	 *
	 * @sample
	 * var name = foundset.getName()
	 *
	 * @return name.
	 */
	@JSFunction
	public String getName()
	{
		String name = this.datasource;
		if (name != null)
		{
			name = DataSourceUtils.getViewDataSourceName(datasource);
		}
		return name;
	}

	/**
	 * Get the record index. Will return -1 if the record can't be found.
	 *
	 * @sample var index = %%prefix%%foundset.getRecordIndex(record);
	 *
	 * @param record Record
	 *
	 * @return int index.
	 */
	@JSFunction
	public int getRecordIndex(ViewRecord record)
	{
		int recordIndex = getRecordIndex((IRecord)record);
		if (recordIndex == -1) return -1;
		return recordIndex + 1;
	}

	/**
	 * Check whether the foundset has record changes.
	 * @return true if the foundset has any edited records, false otherwise
	 */
	@JSFunction
	public boolean hasRecordChanges()
	{
		return !editedRecords.isEmpty();
	}


	public Class< ? >[] getAllReturnedTypes()
	{
		return new Class< ? >[] { ViewRecord.class };
	}

	/**
	 * Check if validation or db exceptions ocurred on a previous attempt to saving this record.
	 * @param viewRecord
	 * @return true if the record is failed, false otherwise
	 */
	boolean isFailedRecord(ViewRecord viewRecord)
	{
		return failedRecords.contains(viewRecord);
	}

	public Object get(Symbol key, Scriptable start)
	{
		if (SymbolKey.ITERATOR.equals(key))
		{
			return symbol_iterator;
		}
		return Scriptable.NOT_FOUND;
	}


	public boolean has(Symbol key, Scriptable start)
	{
		return (SymbolKey.ITERATOR.equals(key));
	}

	public void put(Symbol key, Scriptable start, Object value)
	{

	}


	public void delete(Symbol key)
	{

	}
}
