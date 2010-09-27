/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.BooleanCondition;
import com.servoy.j2db.query.CustomCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.PlaceholderKey;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryCustomJoin;
import com.servoy.j2db.query.QueryCustomSelect;
import com.servoy.j2db.query.QueryCustomSort;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.TableScope;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * The foundset of a form, also handles the locking with the AppServer based on tablepks, and is the formmodel itself!
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSFoundset")
public abstract class FoundSet implements IFoundSetInternal, IRowListener, Scriptable, Cloneable //, Wrapper
{
	public static final String JS_FOUNDSET = "JSFoundset"; //$NON-NLS-1$

	/*
	 * _____________________________________________________________ JavaScript stuff
	 */
	private static Map<String, NativeJavaMethod> jsFunctions = new HashMap<String, NativeJavaMethod>();
	static
	{
		try
		{
			Method[] methods = FoundSet.class.getMethods();
			for (Method m : methods)
			{
				if (m.getName().startsWith("js_")) //$NON-NLS-1$
				{
					String name = m.getName().substring(3);
					NativeJavaMethod nativeJavaMethod = jsFunctions.get(name);
					if (nativeJavaMethod == null)
					{
						nativeJavaMethod = new NativeJavaMethod(m, name);
					}
					else
					{
						nativeJavaMethod = new NativeJavaMethod(Utils.arrayAdd(nativeJavaMethod.getMethods(), new MemberBox(m), true), name);
					}
					jsFunctions.put(name, nativeJavaMethod);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public final String RECORD_IS_LOCKED;
	public final String NO_RECORD;
	public final String NO_ACCESS;

	protected FoundSetManager fsm;
	protected RowManager rowManager;
	protected boolean findMode = false;
	private List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<IFoundSetEventListener>();
	private List<IModificationListener> aggregateModificationListeners = new ArrayList<IModificationListener>();

	protected SQLSheet sheet; //SQL statements to perform on certain actions

	private volatile PksAndRecordsHolder pksAndRecords;

	protected QuerySelect creationSqlSelect;
	private List<TableFilter> foundSetFilters;

	protected transient Map<String, Object> aggregateCache;
	protected transient IDataSet omittedPKs;

	protected List<SortColumn> lastSortColumns;
	protected List<SortColumn> defaultSort;//as defined on a form

	private String stateToStringdataProviderID;
	protected String relationName; //mainly used by aggregates and substates and browseAll(Foundset)

	protected boolean initialized = false; //tells if the foundset has done one query and is ready to use

	private final List<WeakReference<IRecordInternal>> allParents = new ArrayList<WeakReference<IRecordInternal>>(6);

	private PrototypeState proto = null;

	protected boolean mustQueryForUpdates;

	public PrototypeState getPrototypeState()
	{
		if (proto == null)
		{
			proto = new PrototypeState(this);
		}
		return proto;
	}

	//must be used by subclasses
	protected FoundSet(IFoundSetManagerInternal app, IRecordInternal a_parent, String relation_name, SQLSheet sheet, List<SortColumn> defaultSortColumns)
		throws ServoyException
	{
		if (sheet == null)
		{
			throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.sqlsheet")); //$NON-NLS-1$
		}
		fsm = (FoundSetManager)app;
		pksAndRecords = new PksAndRecordsHolder(fsm.chunkSize);
		relationName = relation_name;
		this.sheet = sheet;

		RECORD_IS_LOCKED = fsm.getApplication().getI18NMessage("servoy.foundSet.recordLocked"); //$NON-NLS-1$
		NO_RECORD = fsm.getApplication().getI18NMessage("servoy.foundSet.noRecord"); //$NON-NLS-1$
		NO_ACCESS = fsm.getApplication().getI18NMessage("servoy.foundSet.error.noModifyAccess"); //$NON-NLS-1$

		rowManager = fsm.getRowManager(fsm.getDataSource(sheet.getTable()));
		if (rowManager != null && !(a_parent instanceof FindState)) rowManager.register(this);
		if (defaultSortColumns == null || defaultSortColumns.size() == 0)
		{
			defaultSort = sheet.getDefaultPKSort();
		}
		else
		{
			defaultSort = defaultSortColumns;
		}
		lastSortColumns = defaultSort;

		if (sheet.getTable() != null)
		{
			creationSqlSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), null, null, true, null, lastSortColumns, false);
		}

		pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, AbstractBaseQuery.deepClone(creationSqlSelect));
		aggregateCache = new HashMap<String, Object>(6);
		findMode = false;
	}

	public String getRelationName()
	{
		return relationName;
	}

	public SQLSheet getSQLSheet()
	{
		return sheet;
	}

	/**
	 * @return the pksAndRecords
	 */
	public final PksAndRecordsHolder getPksAndRecords()
	{
		return pksAndRecords;
	}

	@Deprecated
	public void browseAll() throws ServoyException
	{
		loadAllRecords();
	}

	/**
	 * Do PK keys query, and initialize the object further
	 */
	public void loadAllRecords() throws ServoyException
	{
		// Also clear omit in browse all/refresh from db
		// don't do it in refreshFromDb because then
		// the omits can be cleared if there is a refresh
		// from db coming from outside or a search that has no results.
		browseAll(initialized, true);
	}

	public void browseAll(boolean flushRelatedFS) throws ServoyException
	{
		browseAll(flushRelatedFS, false);
	}

	public void browseAll(boolean flushRelatedFS, boolean clearOmit) throws ServoyException
	{
		if (!findMode && initialized && !mustQueryForUpdates && !pksAndRecords.getQuerySelectForReading().hasAnyCondition() && getSize() > 0)
		{
			return;//optimize
		}

		if (sheet == null || sheet.getTable() == null) return;

		if (clearOmit)
		{
			clearOmit(null);
		}

		// do get the sql select with the omitted pks, else a find that didn't get anything will not 
		// just display the records without the omitted pks (when clear omit is false)
		refreshFromDBInternal(
			fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), creationSqlSelect, null, true, omittedPKs, lastSortColumns, true),
			flushRelatedFS, false, fsm.pkChunkSize, false);
	}

	protected void clearOmit(QuerySelect sqlSelect)
	{
		if (sqlSelect != null)
		{
			sqlSelect.clearCondition(SQLGenerator.CONDITION_OMIT);
		}
		omittedPKs = null;
	}

	/*
	 * called in developer to just requery and regenerate the records.
	 */
	public final void refresh()
	{
		if (getTable() == null)
		{
			return;
		}
		try
		{
			refreshFromDB(true);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
	}

	/**
	 * browse all part which can be used by subclasses this also acts as refresh and performs the pk query (again) can be called on any thread
	 * 
	 * @param flushRelatedFS
	 */
	void refreshFromDB(boolean flushRelatedFS) throws ServoyException
	{
		refreshFromDBInternal(null, flushRelatedFS, true, fsm.pkChunkSize, false);
	}

	/**
	 * browse all part which can be used by subclasses this also acts as refresh and performs the pk query (again) can be called on any thread
	 * 
	 * @param querySelect will not be modified, null for the current active query
	 * @param flushRelatedFS
	 */
	protected void refreshFromDBInternal(QuerySelect sqlSelect, boolean flushRelatedFS, boolean dropSort, int rowsToRetrieve, boolean keepPkOrder)
		throws ServoyException
	{
		if (fsm.getDataServer() == null)
		{
			// no data access yet
			return;
		}
		SafeArrayList<IRecordInternal> cachedRecords;
		IDataSet pks;

		synchronized (pksAndRecords)
		{
			cachedRecords = pksAndRecords.getCachedRecords();
			pks = pksAndRecords.getPks();
		}

		Map<Integer, IRecordInternal> newRecords = new HashMap<Integer, IRecordInternal>();
		EditRecordList editRecordList = getFoundSetManager().getEditRecordList();
		IRecordInternal[] array = editRecordList.getEditedRecords(this);
		for (IRecordInternal editingRecord : array)
		{
			if (!editingRecord.existInDataSource())
			{
				synchronized (pksAndRecords)
				{
					int newRecordIndex = cachedRecords.indexOf(editingRecord);
					if (newRecordIndex == -1) newRecordIndex = 0;//incase some has called startEdit before new/duplicateRecords was completed.
					newRecords.put(new Integer(newRecordIndex), editingRecord);
					cachedRecords.set(newRecordIndex, null);
				}
			}
			else
			{
				// TODO check.. call stop edit? Records will be only referenced in the foundset manager:
				editingRecord.stopEditing();
			}
		}
		int oldSize = getSize();
		if (oldSize > 1)
		{
			fireSelectionAdjusting();
		}

		Object[] selectedPK = null;
		if (pks != null && getSelectedIndex() >= 0 && getSelectedIndex() < pks.getRowCount()) selectedPK = pks.getRow(getSelectedIndex());

		IDataSet oldPKs = pks;

		if (!hasAccess(IRepository.READ))
		{
			fireDifference(oldSize, 0);
			throw new ApplicationException(ServoyException.NO_ACCESS);
		}

		//cache pks
		String transaction_id = fsm.getTransactionID(sheet);
		long time = System.currentTimeMillis();
		try
		{
			QuerySelect theQuery = (sqlSelect == null) ? pksAndRecords.getQuerySelectForReading() : sqlSelect;
			int type = initialized ? IDataServer.FIND_BROWSER_QUERY : IDataServer.FOUNDSET_LOAD_QUERY;
			pks = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, theQuery,
				fsm.getTableFilterParams(sheet.getServerName(), theQuery), !theQuery.isUnique(), 0, rowsToRetrieve, type);
			synchronized (pksAndRecords)
			{
				// optimistic locking, if the query has been changed in the mean time forget about the refresh
				if (sqlSelect != null || theQuery == pksAndRecords.getQuerySelectForReading())
				{
					cachedRecords = pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), theQuery);
				}
				else
				{
					Debug.log("refreshFromDBInternal: query was changed during refresh, not resetting old query"); //$NON-NLS-1$
				}
			}
			if (Debug.tracing())
			{
				Debug.trace(Thread.currentThread().getName() +
					": RefreshFrom DB time: " + (System.currentTimeMillis() - time) + " pks: " + pks.getRowCount() + ", SQL: " + theQuery.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}

		initialized = true;

		if (flushRelatedFS)
		{
			Iterator<Relation> it = fsm.getApplication().getFlattenedSolution().getRelations(sheet.getTable(), true, false);
			while (it.hasNext())
			{
				Relation r = it.next();
				fsm.flushRelatedFoundSet(this, r.getName());
			}
			editRecordList.getFoundsetEventMap().remove(this);
			editRecordList.fireEvents();
		}

		clearInternalState(true);

		if (dropSort) lastSortColumns = defaultSort;

		int selectedIndex = -1;
		synchronized (pksAndRecords)
		{
			if (cachedRecords == pksAndRecords.getCachedRecords())
			{
				pks = pksAndRecords.getPks();
				Iterator<Map.Entry<Integer, IRecordInternal>> it = newRecords.entrySet().iterator();
				while (it.hasNext())
				{
					Map.Entry<Integer, IRecordInternal> entry = it.next();
					int newRecordIndex = entry.getKey().intValue();
					IRecordInternal newRecord = entry.getValue();
					if (newRecordIndex == 0)
					{
						cachedRecords.add(0, newRecord);
						pks.addRow(0, newRecord.getPK());
						selectedIndex = 0;
					}
					else if (newRecordIndex > 0)
					{
						newRecordIndex = pks.getRowCount();
						cachedRecords.add(newRecordIndex, newRecord);
						pks.addRow(newRecordIndex, newRecord.getPK());
						selectedIndex = newRecordIndex;
					}
				}

				if (keepPkOrder)
				{
					pksAndRecords.reorder(oldPKs);
				}
			}
			else
			{
				Debug.log("refreshFromDBInternal: cached records were changed during refresh, not reading editing records (would be duplicated)"); //$NON-NLS-1$
			}
		}

		//let the List know the model changed
		fireDifference(oldSize, getSize());

		//move to correct position if we know
		if (selectedIndex != -1 || !selectRecord(selectedPK))
		{
			//move to front if unknown
			setSelectedIndex((pks != null && pks.getRowCount() > 0 && selectedIndex == -1) ? 0 : selectedIndex);
		}
	}


	public boolean hasAccess(int access)
	{
		return fsm.getEditRecordList().hasAccess(getSQLSheet().getTable(), access);
	}

	/**
	 * Clears the foundset.
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_clear()
	 */
	@Deprecated
	public void js_clearFoundSet()
	{
		js_clear();
	}

	/**
	 * Clear the foundset.
	 *
	 * @sample
	 * //Clear the foundset, including searches that may be on it
	 * %%prefix%%foundset.clear();
	 */
	public void js_clear()
	{
		int size = getSize();
		if (size > 1)
		{
			fireSelectionAdjusting();
		}
		clear();
		fireDifference(size, getSize());
	}

	public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value)
	{
		return addFilterParam(null, dataprovider, operator, value);
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * Use clear() or loadAllRecords() to make the filter effective.
	 * When given a name, the filter can be removed again using removeFoundSetFilterParam(name).
	 *
	 * @sample
	 * var success = %%prefix%%foundset.addFoundSetFilterParam('customerid', '=', 'BLONP', 'custFilter');//possible to add multiple
	 * %%prefix%%foundset.loadAllRecords();//to make param(s) effective
	 * // Named filters can be removed using %%prefix%%foundset.removeFoundSetFilterParam(filterName)
	 *
	 * @param dataprovider String column to filter on.
	 *
	 * @param operator String operator: =, <, >, >=, <=, !=, (NOT) LIKE, (NOT) IN, (NOT) BETWEEN and IS (NOT) NULL 
	 *
	 * @param value Object filter value (for in array and between an array with 2 elements)
	 *
	 * @param name optional String name, can be used to remove the filter again.
	 * 
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value, String name)
	{
		return addFilterParam(name, dataprovider, operator, value);
	}

	/**
	 * Remove a named foundset filter.
	 * Use clear() or loadAllRecords() to make the filter effective.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.removeFoundSetFilterParam('custFilter');// removes all filters with this name
	 * %%prefix%%foundset.loadAllRecords();//to make param(s) effective
	 *
	 * @param name String filter name.
	 * 
	 * @return true if removing the filter succeeded, false otherwise.
	 */
	public boolean js_removeFoundSetFilterParam(String name)
	{
		return removeFilterParam(name);
	}

	/**
	 * Get the list of previously defined foundset filters.
	 * The result is an array of:
	 *  [ tableName, dataprovider, operator, value, name ]
	 *
	 * @sample
	 * var params = foundset.getFoundSetFilterParams()
	 * for (var i = 0; params != null && i < params.length; i++)
	 * {
	 * 	application.output('FoundSet filter on table ' + params[i][0]+ ': '+ params[i][1]+ ' '+params[i][2]+ ' '+params[i][3] +(params[i][4] == null ? ' [no name]' : ' ['+params[i][4]+']'))
	 * }
	 *
	 * @param filterName optional name of the filters to retrieve, get all if not specified.
	 * 
	 * @return Array of filter definitions.
	 */
	public Object[][] js_getFoundSetFilterParams(String filterName)
	{
		return getFoundSetFilterParams(filterName);
	}

	public Object[][] js_getFoundSetFilterParams()
	{
		return getFoundSetFilterParams(null);
	}

	public Object[][] getFoundSetFilterParams(String filterName)
	{
		List<Object[]> result = new ArrayList<Object[]>();
		if (foundSetFilters != null)
		{
			Iterator<TableFilter> iterator = foundSetFilters.iterator();
			while (iterator.hasNext())
			{
				TableFilter f = iterator.next();
				if (filterName == null || filterName.equals(f.getName()))
				{
					result.add(new Object[] { f.getTableName(), f.getDataprovider(), RelationItem.getOperatorAsString(f.getOperator()), f.getValue(), f.getName() });
				}
			}
		}

		return result.toArray(new Object[result.size()][]);
	}

	/**
	 * Get a duplicate of the foundset.
	 *
	 * @sample
	 * var dupFoundset = %%prefix%%foundset.duplicateFoundSet();
	 * %%prefix%%foundset.find();
	 * //search some fields
	 * var count = %%prefix%%foundset.search();
	 * if (count == 0)
	 * {
	 * 	plugins.dialogs.showWarningDialog('Alert', 'No records found','OK');
	 * 	controller.loadRecords(dupFoundset);
	 * }
	 * 
	 * @return foundset duplicate.
	 */
	public FoundSet js_duplicateFoundSet() throws ServoyException//can be used by loadRecords Again
	{
		return (FoundSet)copy(false);
	}

	/**
	 * Set the foundset in find mode.
	 * 
	 * Before going into find mode, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off), 
	 * the foundset will not go into find mode.
	 * Note: always make sure to check the result of the find() method.
	 * 
	 * When in find mode, columns can be assigned string expressions that are evaluated as:
	 * General:
	 *       c1||c2    (condition1 or condition2)
	 *       c|format  (apply format on condition like 'x|dd-MM-yyyy')
	 *       !c        (not condition)
	 *       #c        (modify condition, depends on column type)
	 *       ^         (is null)
	 *       ^=        (is null or empty)
	 *       &lt;x     (less than value x)
	 *       &gt;x     (greater than value x)
	 *       &lt;=x    (less than or equals value x)
	 *       &gt;=x    (greater than or equals value x)
	 *       x...y     (between values x and y, including values)
	 *       x         (equals value x)
	 *
	 *  Number fields:
	 *       =x       (equals value x)
	 *       ^=       (is null or zero)
	 *
	 *  Date fields:
	 *       #c       (equals value x, entire day)
	 *       now      (equals now, date and or time)
	 *       //       (equals today)
	 *       today    (equals today)
	 *
	 *  Text fields:
	 *       #c	        (case insensitive condition)
	 *       = x      (equals a space and 'x')
	 *       ^=       (is null or empty)
	 *       %x%      (contains 'x')
	 *       %x_y%    (contains 'x' followed by any char and 'y')
	 *       \%      (contains char '%')
	 *       \_      (contains char '_')
	 *
	 * Related columns can be assigned, they will result in related searches.
	 * For example, "employees_to_department.location_id = headoffice" finds all employees in the specified location).
	 * 
	 * Searching on related aggregates is supported.
	 * For example, "orders_to_details.total_amount = '&gt;1000'" finds all orders with total order details amount more than 1000.
	 *  
	 * @sample
	 * if (%%prefix%%foundset.find()) //find will fail if autosave is disabled and there are unsaved records
	 * {
	 * 	columnTextDataProvider = 'a search value'
	 * 	columnNumberDataProvider = '>10'
	 * 	%%prefix%%foundset.search()
	 * }
	 * @return true if the foundset is now in find mode, false otherwise.
	 */
	public boolean js_find()
	{
		if (!isInFindMode())
		{
			if (fsm.getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED)
			{
				return false;
			}
			setFindMode();
		}
		return isInFindMode();
	}

	/**
	 * Check if this foundset is in find mode.
	 *
	 * @sample
	 * //Returns true when find was called on this foundset and search has not been called yet
	 * %%prefix%%foundset.isInFind();
	 * 
	 * @return boolean is in find mode.
	 */
	public boolean js_isInFind()
	{
		return isInFindMode();
	}

	/**
	 * Perform a search and show the results.
	 * Must be in find mode when running search (see find()).
	 *
	 * @sample
	 * var recordCount = %%prefix%%foundset.search();
	 * //var recordCount = %%prefix%%foundset.search(false,false);//to extend foundset
	 *
	 * @param clearLastResults optional boolean, clear previous search, default true  
	 *
	 * @param reduceSearch optional boolean, reduce (true) or extend (false) previous search results, default true
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_find()
	 * 
	 * @return int number of rows returned
	 */
	public int js_search(Object[] vargs) throws ServoyException
	{
		if (isInFindMode())
		{
			boolean clearLastResults = true;
			boolean reduceSearch = true;
			if (vargs != null && vargs.length >= 1 && vargs[0] instanceof Boolean)
			{
				clearLastResults = ((Boolean)vargs[0]).booleanValue();
			}
			if (vargs != null && vargs.length >= 2 && vargs[1] instanceof Boolean)
			{
				reduceSearch = ((Boolean)vargs[1]).booleanValue();
			}
			return performFind(clearLastResults, reduceSearch, true);
		}
		return 0;
	}

	/**
	 * Gets the name of the table used.
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getDataSource()
	 */
	@Deprecated
	public String js_getTableName()
	{
		ITable table = getTable();
		return table == null ? null : table.getName();
	}

	/**
	 * Gets the name of the server used.
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getDataSource()
	 */
	@Deprecated
	public String js_getServerName()
	{
		ITable table = getTable();
		return table == null ? null : table.getServerName();
	}

	/**
	 * Get the datasource used.
	 * The datasource is an url that describes the data source.
	 * 
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getDataSourceServerName(String)
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getDataSourceTableName(String)
	 *
	 * @sample var dataSource = %%prefix%%foundset.getDataSource();
	 * 
	 * @return String data source.
	 */
	public String js_getDataSource()
	{
		return getDataSource();
	}

	/**
	 * Gets the relation name (null if not a related foundset).
	 *
	 * @sample var relName = %%prefix%%foundset.getRelationName();
	 * 
	 * @return String relation name when related.
	 */
	public String js_getRelationName()
	{
		return relationName;
	}


	/**
	 * Invert the foundset against all rows of the current table.
	 * All records that are not in the foundset will become the current foundset.
	 *
	 * @sample %%prefix%%foundset.invertRecords();
	 */
	public void js_invertRecords() throws ServoyException
	{
		checkInitialized();

		if (fsm.getEditRecordList().stopIfEditing(this) == ISaveConstants.STOPPED)
		{
			invert();
		}
	}

	/**
	 * Loads all accessible records from the datasource into the foundset.
	 * Filters on the foundset are applied.
	 * 
	 * Before loading the records, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off), 
	 * records will not be loaded,
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_addFoundSetFilterParam(String, String, Object, String)
	 *
	 * @sample %%prefix%%foundset.loadAllRecords();
	 * 
	 * @return true if records are loaded, false otherwise.
	 */
	public boolean js_loadAllRecords() throws ServoyException
	{
		if (isInitialized())
		{
			int stopped = fsm.getEditRecordList().stopIfEditing(this);
			if (stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED)
			{
				return false;
			}
		}

		if (isInFindMode())
		{
			performFind(false, true, true);
		}

		loadAllRecords();
		return true;
	}

	/**
	 * Loads the records that are currently omitted as a foundset.
	 * 
	 * Before loading the omitted records, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off), 
	 * omitted records will not be loaded,
	 *
	 * @sample %%prefix%%foundset.loadOmittedRecords();
	 * 
	 * @return true if records are loaded, false otherwise.
	 */
	public boolean js_loadOmittedRecords() throws ServoyException
	{
		checkInitialized();

		if (fsm.getEditRecordList().stopIfEditing(this) == ISaveConstants.STOPPED)
		{
			return showOmitted();
		}
		return false;
	}

	/**
	 * Load records with primary key (dataset/number/uuid) or query.
	 *
	 * Load records can be used in 5 different ways
	 * 1) to copy foundset data from another foundset
	 * foundset.loadRecords(fs);

	 * 2) to load a primary key dataset, will remove related sort!
	 * var dataset = databaseManager.getDataSetByQuery(...);
	 * foundset.loadRecords(dataset);
	 * 
	 * 3) to load a single record by primary key, will remove related sort! (pk should be a number or UUID)
	 * foundset.loadRecords(123);
	 * foundset.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
	 * 
	 * 4) to reload all last related records again, if for example when searched in tabpanel
	 * foundset.loadRecords();
	 * 
	 * 5) to load records in to the form based on a query (also known as 'Form by query')
	 * foundset.loadRecords(sqlstring,parameters);
	 * limitations/requirements for sqlstring are:
	 * -must start with 'select'
	 * -the selected columns must be the (Servoy Form) table primary key columns (alphabetically ordered like 'select a_id, b_id,c_id ...')
	 * -can contain '?' which are replaced with values from the array supplied to parameters argument
	 * if the sqlstring contains an 'order by' clause, the records will be sorted accordingly and additional constraints apply:
	 * -must contain 'from' keyword
	 * -the 'from' must be a comma separated list of table names
	 * -must at least select from the table used in Servoy Form
	 * -cannot contain 'group by', 'having' or 'union'
	 * -all columns must be fully qualified like 'orders.order_id'
	 * 
	 * @sample
	 * //Load records can be used in 5 different ways
	 * //1) to copy foundset data from another foundset
	 * //%%prefix%%foundset.loadRecords(fs);

	 * //2) to load a primary key dataset, will remove related sort!
	 * //var dataset = databaseManager.getDataSetByQuery(...);
	 * // dataset must match the table primary key columns (alphabetically ordered)
	 * //%%prefix%%foundset.loadRecords(dataset);
	 * 
	 * //3) to load a single record by primary key, will remove related sort! (pk should be a number or UUID)
	 * //%%prefix%%foundset.loadRecords(123);
	 * //%%prefix%%foundset.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
	 * 
	 * //4) to reload all last related records again, if for example when searched in tabpanel
	 * //%%prefix%%foundset.loadRecords();
	 * 
	 * //5) to load records in to the form based on a query (also known as 'Form by query')
	 * //%%prefix%%foundset.loadRecords(sqlstring,parameters);
	 * //limitations/requirements for sqlstring are:
	 * //-must start with 'select'
	 * //-the selected columns must be the (Servoy Form) table primary key columns (alphabetically ordered like 'select a_id, b_id,c_id ...')
	 * //-can contain '?' which are replaced with values from the array supplied to parameters argument
	 * //if the sqlstring contains an 'order by' clause, the records will be sorted accordingly and additional constraints apply:
	 * //-must contain 'from' keyword
	 * //-the 'from' must be a comma separated list of table names
	 * //-must at least select from the table used in Servoy Form
	 * //-cannot contain 'group by', 'having' or 'union'
	 * //-all columns must be fully qualified like 'orders.order_id'
	 *
	 * @param input optional foundset/pkdataset/single_pk/query
	 * @param queryArgumentsArray optional used when input is a query
	 * @return true if successful
	 */
	public boolean js_loadRecords(Object[] vargs) throws ServoyException
	{
		if (isInFindMode() || sheet.getTable() == null)
		{
			return false;
		}

		if (vargs == null || vargs.length == 0)
		{
			loadAllRecords();
			return true;
		}

		if (relationName != null) // on related foundset, only allow loadRecords without arguments
		{
			return false;
		}

		if (isInitialized())
		{
			int stopped = fsm.getEditRecordList().stopIfEditing(this);
			if (stopped != ISaveConstants.STOPPED && stopped != ISaveConstants.AUTO_SAVE_BLOCKED)
			{
				return false;
			}
		}

		Object[] args = null;
		Object data = vargs[0];
		if (vargs.length >= 2)
		{
			if (vargs[1] instanceof Object[])
			{
				args = (Object[])vargs[1];
			}
		}

		if (data instanceof Wrapper)
		{
			data = ((Wrapper)data).unwrap();
		}

		if (data instanceof IDataSet)
		{
			return loadExternalPKList((IDataSet)data);
		}
		if (data instanceof FoundSet)
		{
			return copyFrom((FoundSet)data);
		}
		if (data instanceof String)
		{
			return loadByQuery((String)data, args);
		}
		if (data instanceof Number || data instanceof UUID)
		{
			List<Column> pkColumns = sheet.getTable() == null ? null : sheet.getTable().getRowIdentColumns();
			if (pkColumns != null && pkColumns.size() == 1)
			{
				List<Object[]> rows = new ArrayList<Object[]>();
				rows.add(new Object[] { data });
				BufferedDataSet dataset = new BufferedDataSet(new String[] { pkColumns.get(0).getName() }, rows);
				return loadExternalPKList(dataset);
			}
			return false;
		}
		loadAllRecords();
		return true;
	}

	/**
	 * Perform a relookup for the current record or the record under the given index
	 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
	 *
	 * @sample %%prefix%%foundset.relookup(1);
	 * @param index optional record index (1-based) 
	 */
	public void js_relookup(Object[] args)
	{
		int i;
		if (args == null || args.length != 1)
		{
			i = getSelectedIndex();
		}
		else
		{
			i = Utils.getAsInteger(args[0], true) - 1;
		}
		if (isInitialized() && i >= 0 && i < getSize())
		{
			processCopyValues(i);
		}
	}

	/**
	 * Get a value based on a dataprovider name.
	 *
	 * @sample var val = %%prefix%%foundset.getDataProviderValue('contact_name');
	 *
	 * @param dataProviderID data provider name
	 * 
	 * @return Object value
	 */
	public Object js_getDataProviderValue(String dataProviderID)
	{
		if (has(dataProviderID, this))
		{
			return get(dataProviderID, this);
		}
		return null;
	}

	/**
	 * Set a value based on a dataprovider name.
	 *
	 * @sample %%prefix%%foundset.setDataProviderValue('contact_name','mycompany');
	 *
	 * @param dataProviderID data provider name
	 *
	 * @param value value to set
	 */
	public void js_setDataProviderValue(String dataProviderID, Object value)
	{
		put(dataProviderID, this, value);
	}

	/**
	 * Create a new unrelated foundset that is a copy of the current foundset.
	 * If the current foundset is not related, no copy will made.
	 *
	 * @sample %%prefix%%foundset.unrelate();
	 * 
	 * @return FoundSet unrelated foundset.
	 */
	public IFoundSetInternal js_unrelate()
	{
		if (relationName != null)
		{
			try
			{
				return copy(true);
			}
			catch (ServoyException e)
			{
				Debug.error("Unrelated of relatedfoundset: " + this + " failed", e); //$NON-NLS-1$//$NON-NLS-2$
				return null;
			}
		}
		return this;
	}

	/**
	 * Select the record based on pk data.
	 * Note that if the foundset has not loaded the record with the pk, selectrecord will fail.
	 * 
	 * In case of a table with a composite key, the pk sequence must match the alphabetical 
	 * ordering of the pk column names.
	 *
	 * @sample %%prefix%%foundset.selectRecord(pkid1,pkid2,pkidn);//pks must be alphabetically set! It is also possible to use an array as parameter.
	 *
	 * @param pkid1 primary key
	 *
	 * @param pkid2 optional second primary key (in case of composite primary key)
	 *
	 * @param pkidn optional nth primary key
	 * 
	 * @return true if succeeded.
	 */
	public boolean js_selectRecord(Object[] vargs)
	{
		if (sheet.getTable() == null)
		{
			return false;
		}
		List<Object> args = new ArrayList<Object>();
		if (vargs != null && vargs.length > 0)
		{
			List<Column> cols = sheet.getTable().getRowIdentColumns();
			for (int i = 0; i < Math.min(vargs.length, cols.size()); i++)
			{
				Column c = cols.get(i);
				args.add(c.getAsRightType(vargs[i]));
			}
			return selectRecord(args.toArray(), true);
		}
		return false;
	}

	protected int getRecordIndex(Object[] pk, int start)
	{
		IDataSet pks = pksAndRecords.getPks();
		if (pk != null && pk.length != 0 && pks != null && pks.getColumnCount() == pk.length)
		{
			for (int r = start; r < pks.getRowCount(); r++)
			{
				Object[] pkrow = pks.getRow(r);
				boolean match = pkrow.length == pk.length;
				for (int c = 0; c < pkrow.length; c++)
				{
					match = match && Utils.equalObjects(pk[c], pkrow[c]);
				}
				if (match)
				{
					return r;
				}
			}
		}
		return -1;
	}

	protected boolean selectRecord(Object[] pk)
	{
		return selectRecord(pk, false);
	}

	protected boolean selectRecord(Object[] pk, boolean toggleInMultiselect)
	{
		int index = getRecordIndex(pk, 0);
		if (index != -1)
		{
			if (toggleInMultiselect && isMultiSelect())
			{
				// toggle selected record
				boolean indexAlreadySelected = false;
				int[] selectedIndexes = getSelectedIndexes();
				int[] newSelectedIndexes = new int[selectedIndexes.length + 1];
				int i = 0;
				for (int selectedIdx : selectedIndexes)
					if (index == selectedIdx)
					{
						indexAlreadySelected = true;
						continue;
					}
					else newSelectedIndexes[i++] = selectedIdx;
				if (indexAlreadySelected)
				{
					if (selectedIndexes.length > 1) // only deselect if there are at least 2 selected, so we always have a selection
					{
						int[] newSelectedIndexesTrimed = new int[newSelectedIndexes.length - 2];
						System.arraycopy(newSelectedIndexes, 0, newSelectedIndexesTrimed, 0, newSelectedIndexes.length - 2);
						newSelectedIndexes = newSelectedIndexesTrimed;
					}
					else return true;
				}
				else newSelectedIndexes[i] = index;

				setSelectedIndexes(newSelectedIndexes);
			}
			else setSelectedIndex(index);
			return true;
		}
		return false;
	}

	public boolean loadByQuery(String query, Object[] args) throws ServoyException
	{
		if (query == null || sheet.getTable() == null) return false;

		int from_index = -1;
		int order_by_index;

		//check requirements
		String sql_lowercase = Utils.toEnglishLocaleLowerCase(query);
		if (!sql_lowercase.startsWith("select")) throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.query.error.startWithSelect")); //$NON-NLS-1$ //$NON-NLS-2$

		order_by_index = sql_lowercase.lastIndexOf("order by"); //$NON-NLS-1$
		boolean analyse_query_parts = (order_by_index != -1);
		if (analyse_query_parts)
		{
			// if the query cannot be parsed according to the old methods, we just use the entire sql as
			// subquery. NOTE: this means that the ordering defined in the order-by part is lost.
			if (((from_index = sql_lowercase.indexOf("from")) == -1) //$NON-NLS-1$
				||
				(sql_lowercase.indexOf(Utils.toEnglishLocaleLowerCase(sheet.getTable().getSQLName())) == -1) || (sql_lowercase.indexOf("group by") != -1) //$NON-NLS-1$
				|| (sql_lowercase.indexOf("having") != -1) //$NON-NLS-1$
				|| (sql_lowercase.indexOf("union") != -1) //$NON-NLS-1$
				|| (sql_lowercase.indexOf("join") != -1) //$NON-NLS-1$
				|| (sql_lowercase.indexOf(".") == -1)) //$NON-NLS-1$
			{
				analyse_query_parts = false;
			}
		}
		if (initialized && (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED))
		{
			Debug.log("couldn't load dataset because foundset had editted records but couldn't save it"); //$NON-NLS-1$
			fsm.getApplication().reportJSError("couldn't load dataset because foundset had editted records but couldn't save it", null); //$NON-NLS-1$
			return false;
		}

		int sizeBefore = getSize();
		if (sizeBefore > 1)
		{
			fireSelectionAdjusting();
		}

		QuerySelect originalQuery = pksAndRecords.getQuerySelectForReading();

		QuerySelect sqlSelect = AbstractBaseQuery.deepClone(creationSqlSelect);
		sqlSelect.clearCondition(SQLGenerator.CONDITION_RELATION);
		sqlSelect.clearCondition(SQLGenerator.CONDITION_OMIT);

		if (rowManager != null) rowManager.clearAndCheckCache();
		initialized = true;

		Object[] whereArgs = null;
		if (args != null)
		{
			whereArgs = new Object[args.length];
			for (int i = 0; i < args.length; i++)
			{
				Object o = args[i];
				if (o != null && o.getClass().equals(Date.class))
				{
					o = new Timestamp(((Date)o).getTime());
				}
				whereArgs[i] = o;
			}
		}

		// the SQL is seen as a search condition, not as a foundset filter (V3.1 behavior).
		// Store the sql in the SQLGenerator.CONDITION_SEARCH part of the query. This means that the sql is inverted in
		// invertRecords and OR-ed in extended search.
		// for instance, loadRecords(SQL) followed by extended search (S) and invertrecords executes query 'NOT(SQL OR S)'
		if (!analyse_query_parts)
		{ // do not analyze the parts of the query, just create a set-condition that compares the pk columns with the result of the subquery

			Iterator<Column> pkIt = ((Table)getTable()).getRowIdentColumns().iterator();
			if (!pkIt.hasNext())
			{
				throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { getTable().getName() });
			}

			List<QueryColumn> pkQueryColumns = new ArrayList<QueryColumn>();
			while (pkIt.hasNext())
			{
				Column c = pkIt.next();
				pkQueryColumns.add(new QueryColumn(sqlSelect.getTable(), c.getID(), c.getSQLName(), c.getType(), c.getLength()));
			}

			// must strip of the order-by part because not all databases (Oracle, who else) like order-by in subselect
			String customQuery = query;
			while (order_by_index > 0)
			{
				// query contains order-by clause, remove it until a closing bracket or end-of-string.
				// order-by has to be removed because some dbs do not allow that inside subselect
				char[] chars = query.toCharArray();
				int level = 1;
				int i;
				for (i = order_by_index; level > 0 && i < chars.length; i++)
				{
					switch (chars[i])
					{
						case ')' :
							level--;
							break;
						case '(' :
							level++;
							break;
					}
				}
				customQuery = query.substring(0, order_by_index) + ((level > 0) ? "" : query.substring(i - 1)); //$NON-NLS-1$
				order_by_index = customQuery.toLowerCase().lastIndexOf("order by"); //$NON-NLS-1$
			}
			sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH,
				new SetCondition(ISQLCondition.EQUALS_OPERATOR, pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]), new QueryCustomSelect(
					customQuery, whereArgs), true));

			// set the previous sort, add all joins that are needed for this sort
			List<IQuerySort> origSorts = originalQuery.getSorts();
			if (origSorts != null)
			{
				ArrayList<IQuerySort> sorts = new ArrayList<IQuerySort>();

				// find which sorts we will use and which tables are needed for that
				Set<QueryTable> sortTables = new HashSet<QueryTable>();
				for (IQuerySort isort : origSorts)
				{
					if (isort instanceof QuerySort)
					{
						QuerySort sort = (QuerySort)isort;
						IQuerySelectValue icolumn = sort.getColumn();
						if (icolumn instanceof QueryColumn)
						{
							QueryColumn column = (QueryColumn)icolumn;
							sortTables.add(column.getTable());
							sorts.add(sort);
						}
					}
					// ignore custom sorts and sorts on other things than columns
				}

				// try to find the joins that are needed to satisfy tablesToResolve
				List<QueryTable> tablesToResolve = new ArrayList<QueryTable>();
				tablesToResolve.addAll(sortTables);

				List<QueryTable> resolvedTables = new ArrayList<QueryTable>();
				resolvedTables.add(sqlSelect.getTable());

				ArrayList<ISQLJoin> requiredJoins = new ArrayList<ISQLJoin>();

				boolean found = true;
				while (found && tablesToResolve.size() > 0)
				{
					QueryTable table = tablesToResolve.remove(0);
					if (resolvedTables.contains(table))
					{
						continue;
					}
					found = false;
					ArrayList<ISQLJoin> joins = originalQuery.getJoins();
					if (joins != null)
					{
						for (ISQLJoin ijoin : joins)
						{
							if (!found && ijoin instanceof QueryJoin)
							{
								QueryJoin join = (QueryJoin)ijoin;
								if (table.equals(join.getForeignTable()))
								{
									// have to add this join
									tablesToResolve.add(join.getPrimaryTable());
									resolvedTables.add(table);
									requiredJoins.add(join);
									found = true;
								}
							}
						}
					}
				}

				if (found)
				{
					sqlSelect.setJoins(requiredJoins);
					sqlSelect.setSorts(sorts);
				}
				else
				{
					Debug.log("Could not restore order by in loadRecords(): couild not find all tables for sorting in " + originalQuery); //$NON-NLS-1$
				}
			}
		}
		else
		{
			// create a query with the different parts as custom elements
			sqlSelect.clearJoins();

			String tables;
			int where_index = sql_lowercase.indexOf("where"); //$NON-NLS-1$
			if (where_index == -1)
			{
				tables = query.substring(from_index + 4, order_by_index);
				// no where-clause, remove the search condition (was set to FALSE in clear()
				sqlSelect.clearCondition(SQLGenerator.CONDITION_SEARCH);
			}
			else
			{
				tables = query.substring(from_index + 4, where_index);
				sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, new CustomCondition(query.substring(where_index + 5, order_by_index), whereArgs));
			}

			// pick the foundset main table from the tables in the query (does not have to be the first one, we generate sql ourselves
			// that puts the main table at the end, see QueryGenerator)
			boolean foundTable = false;
			String mainTable = sheet.getTable().getName();
			StringBuilder otherTables = new StringBuilder();
			StringTokenizer tok = new StringTokenizer(tables, ","); //$NON-NLS-1$
			String mainTableAlias = null;
			String whitespace = "\\s+"; //$NON-NLS-1$
			while (tok.hasMoreElements())
			{
				String tableName = tok.nextToken().trim();
				String[] lcTableName = tableName.toLowerCase().split(whitespace);
				if (lcTableName[0].equals(mainTable))
				{
					foundTable = true;
					if (lcTableName.length > 1) // alias
					{
						mainTableAlias = tableName.split(whitespace)[1];
					}
				}
				else
				{
					if (otherTables.length() > 0)
					{
						otherTables.append(", "); //$NON-NLS-1$
					}
					otherTables.append(tableName);
				}
			}

			// set table alias or unalias table when no alias was used
			QueryTable qTable = sqlSelect.getTable();
			sqlSelect.relinkTable(sqlSelect.getTable(), new QueryTable(qTable.getName(), qTable.getCatalogName(), qTable.getSchemaName(), mainTableAlias));

			if (otherTables.length() > 0)
			{
				if (!foundTable) throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.query.error.firstTable")); //$NON-NLS-1$
				sqlSelect.addJoin(new QueryCustomJoin("foundset.loadbyquery", sqlSelect.getTable(), otherTables.toString())); //$NON-NLS-1$
			}

			ArrayList<IQuerySort> sorts = new ArrayList<IQuerySort>();
			Enumeration<Object> sortParts = new StringTokenizer(query.substring(order_by_index + 8), ","); //$NON-NLS-1$
			while (sortParts.hasMoreElements())
			{
				sorts.add(new QueryCustomSort(((String)sortParts.nextElement()).trim()));
			}
			sqlSelect.setSorts(sorts);
		}

		clearOmit(sqlSelect);

		//do query with sqlSelect
		String transaction_id = fsm.getTransactionID(sheet);
		IDataSet pk_data;
		try
		{
			pk_data = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, sqlSelect,
				fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), !sqlSelect.isUnique(), 0, fsm.pkChunkSize, IDataServer.CUSTOM_QUERY);
		}
		catch (RemoteException e)
		{
			clear();
			throw new RepositoryException(e);
		}

		if (pk_data.getRowCount() > 0 && pk_data.getColumnCount() != sheet.getPKIndexes().length) throw new IllegalArgumentException(
			fsm.getApplication().getI18NMessage("servoy.foundSet.query.error.incorrectNumberOfPKS")); //$NON-NLS-1$

		pksAndRecords.setPksAndQuery(pk_data, pk_data.getRowCount(), sqlSelect);
		clearInternalState(true);

		fireDifference(sizeBefore, getSize());
		return true;
	}

	public boolean loadExternalPKList(IDataSet ds) throws ServoyException
	{
		if (sheet.getTable() == null)
		{
			return false;
		}

		if (initialized && (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED))
		{
			Debug.log("couldn't load dataset because foundset had editted records but couldn't save it"); //$NON-NLS-1$
			fsm.getApplication().reportJSError("couldn't load dataset because foundset had editted records but couldn't save it", null); //$NON-NLS-1$
			return false;
		}

		int sizeBefore = getSize();
		if (sizeBefore > 1)
		{
			fireSelectionAdjusting();
		}

		IDataSet set = ds;
		if (set != null && set.getRowCount() > 0)
		{
			List<Column> pkColumns = sheet.getTable().getRowIdentColumns();

			if (set.getColumnCount() > pkColumns.size())
			{
				int[] columns = new int[pkColumns.size()];
				for (int i = 0; i < pkColumns.size(); i++)
				{
					columns[i] = i;
				}
				set = new BufferedDataSet(set, columns);
			}

			for (int i = 0; i < set.getRowCount(); i++)
			{
				Object[] row = set.getRow(i);
				for (int j = 0; j < pkColumns.size(); j++)
				{
					row[j] = pkColumns.get(j).getAsRightType(row[j], true);
				}
				set.setRow(i, row);
			}
		}

		QuerySelect sqlSelect = pksAndRecords.getQuerySelectForModification();
		if (set != null && set.getRowCount() > 0)
		{
			// only generate the sql select when there is really data.
			// else it is just clear()
			sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, fsm.getSQLGenerator().createPKConditionFromDataset(sheet, sqlSelect.getTable(), set));
		}
		else
		{
			sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, BooleanCondition.FALSE_CONDITION);
		}
		sqlSelect.clearJoins();
		sqlSelect.clearSorts();
		//not possible to keep related, can limit the just supplied pkset, which would awkward
		SQLGenerator.addSorts(fsm.getApplication(), sqlSelect, sqlSelect.getTable(), this, sheet.getTable(), lastSortColumns, false);
		clearOmit(sqlSelect);
		int sizeAfter = (set == null) ? 0 : set.getRowCount();
		pksAndRecords.setPksAndQuery((set == null ? new BufferedDataSet(set) : set), sizeAfter, sqlSelect);
		clearInternalState(true);

		if (fsm.getTableFilterParams(sheet.getServerName(), sqlSelect) != null && set != null && set.getRowCount() > 0)
		{
			refreshFromDBInternal(null, false, true, set.getRowCount(), true); // some PKs in the set may not be valid for the current filters
		}
		else
		{
			if (pksAndRecords.getPks().getRowCount() > 0) getRecord(0);

			fireDifference(sizeBefore, sizeAfter);
		}
		return true;
	}

	/**
	 * Get a reference to the current sql. NOTE: this is not a copy, make no change to the query.
	 */
	public QuerySelect getSqlSelect()
	{
		return pksAndRecords.getQuerySelectForReading();
	}

	public QuerySelect getCreationSqlSelect()
	{
		return creationSqlSelect;
	}

	protected boolean queryForMorePKs(boolean fireChanges)
	{
		PksAndRecordsHolder pksAndRecordsCopy = pksAndRecords.shallowCopy();
		return queryForMorePKs(pksAndRecordsCopy, pksAndRecordsCopy.getPks().getRowCount() + fsm.pkChunkSize, fireChanges);
	}

	public boolean queryForAllPKs(int estimateCount)
	{
		return queryForMorePKs(pksAndRecords.shallowCopy(), estimateCount + fsm.pkChunkSize, true);
	}

	protected boolean queryForMorePKs(PksAndRecordsHolder pksAndRecordsCopy, int maxResult, boolean fireChanges)
	{
		try
		{
			String transaction_id = fsm.getTransactionID(sheet);
			QuerySelect sqlSelect = pksAndRecordsCopy.getQuerySelectForReading();
			PKDataSet pks = pksAndRecordsCopy.getPks();
			int dbIndexLastPk = pksAndRecordsCopy.getDbIndexLastPk();
			int startRow;
			String lastPkHash;
			int correctedMaxResult; // corrected against added or removed PKs in db since first chunk select
			if (dbIndexLastPk > 0 && pks.getRowCount() > 0)
			{
				correctedMaxResult = maxResult > 0 ? (maxResult + dbIndexLastPk - pks.getRowCount()) : maxResult;
				lastPkHash = RowManager.createPKHashKey(pks.getRow(pks.getRowCount() - 1));
				// re-query the last pk
				startRow = dbIndexLastPk - 1;
			}
			else
			{
				correctedMaxResult = maxResult;
				startRow = pks.getRowCount();
				lastPkHash = null;
			}
			int size = getCorrectedSizeForFires();
			long time = System.currentTimeMillis();
			IDataSet newpks = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, sqlSelect,
				fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), !sqlSelect.isUnique(), startRow, correctedMaxResult,
				IDataServer.FOUNDSET_LOAD_QUERY);
			if (Debug.tracing())
			{
				Debug.trace("Query for PKs, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + " SQL: " + sqlSelect.toString()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}

			int offset = 0;
			if (lastPkHash != null)
			{
				for (int i = 0; offset == 0 && i < newpks.getRowCount(); i++)
				{
					if (lastPkHash.equals(RowManager.createPKHashKey(newpks.getRow(i))))
					{
						// found the last pk from the previous set
						if (i != 0)
						{
							// out-of-sync
							Debug.warn("Data has been added in the database since first select of foundSet, new data is ignored"); //$NON-NLS-1$
						}
						// ignore PKs before the last pk of previous set, they have been added since last chunk select
						offset = i + 1;
					}
				}
				if (offset == 0 && startRow > 0)
				{
					// not found, reselect from start
					Debug.warn("Could not connect next foundset chunk, re-loading entire PK set"); //$NON-NLS-1$
					pks.createPKCache(); // out-of-sync detected, this also flags that new PKS need to be matched against existing ones
					startRow = 0;
					time = System.currentTimeMillis();
					newpks = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, sqlSelect,
						fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), !sqlSelect.isUnique(), startRow, correctedMaxResult,
						IDataServer.FOUNDSET_LOAD_QUERY);
					if (Debug.tracing())
					{
						Debug.trace("RE-query for PKs, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + " SQL: " + sqlSelect.toString()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}
				}
			}
			for (int i = offset; i < newpks.getRowCount(); i++)
			{
				// check for duplicates
				Object[] newpk = newpks.getRow(i);
				if (!pks.hasPKCache() /* only check for duplicates if foundset could not be connected */|| !pks.containsPk(newpk))
				{
					pks.addRow(newpk);
					dbIndexLastPk = startRow + 1 + i; // keep index in db of last added pk to correct maxresult in next chunk
				}
			}

			if (!newpks.hadMoreRows())
			{
				pks.clearHadMoreRows();
			}
			pksAndRecordsCopy.setDbIndexLastPk(dbIndexLastPk);

			int newSize = getCorrectedSizeForFires();
			if (newpks.getRowCount() != 0)
			{
				if (fireChanges) fireFoundSetEvent(size, newSize, FoundSetEvent.CHANGE_INSERT);
				return true;
			}
			return false;
		}
		catch (ServoyException ex)
		{
			fsm.getApplication().reportError(fsm.getApplication().getI18NMessage("servoy.foundSet.error.retrievingMoreData"), ex); //$NON-NLS-1$
			throw new RuntimeException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.retrievingMoreData"), ex); //$NON-NLS-1$
		}
		catch (Exception ex)
		{
			throw new RuntimeException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.retrievingMoreData"), ex); //$NON-NLS-1$
		}
	}

	public List<SortColumn> getLastSortColumns()
	{
		return lastSortColumns;
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	public void checkInitialized()
	{
		if (!isInitialized())
		{
			throw new RuntimeException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.formNotInitialized")); //$NON-NLS-1$
		}
	}

	/**
	 * WARNING: this method is also used by cloning, change with care!
	 */
	protected void clearInternalState(boolean fireModeChangeIfNecessary)
	{
		if (findMode)
		{
			findMode = false;
			if (fireModeChangeIfNecessary) fireFindModeChange();
		}
		if (aggregateCache.size() > 0)
		{
			fireAggregateChangeWithEvents(null);
		}
		mustQueryForUpdates = false;
	}

	public void browseAll(QuerySelect otherSQLSelect) throws ServoyException //ONLY used by printing
	{
		if (sheet == null || sheet.getTable() == null) return;

		int oldSize = getSize();
		if (oldSize > 0)
		{
			fireSelectionAdjusting();
		}

		lastSortColumns = defaultSort;
		QuerySelect sqlSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), otherSQLSelect, null, true, null, lastSortColumns, true);
		if (!initialized)
		{
			creationSqlSelect = AbstractBaseQuery.deepClone(sqlSelect);
		}

		if (!hasAccess(IRepository.READ))
		{
			throw new ApplicationException(ServoyException.NO_ACCESS);
		}

		//cache pks
		String transaction_id = fsm.getTransactionID(sheet);
		long time = System.currentTimeMillis();
		try
		{
			IDataSet pks = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, sqlSelect,
				fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), !sqlSelect.isUnique(), 0, fsm.pkChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
			pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), sqlSelect);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		if (Debug.tracing())
		{
			Debug.trace("BrowseAll time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + ", SQL: " + sqlSelect.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		initialized = true;
		omittedPKs = null;
		clearInternalState(true);

		//let the List know the model changed
		fireDifference(oldSize, getSize());
	}

	/**
	 * When used in normal JList set the toString column on all created states
	 */
	public void setRecordToStringDataProviderID(String dataProviderID)
	{
		stateToStringdataProviderID = dataProviderID;// Index sheet.getColumnIndex(dataProviderID);
	}

	public String getRecordToStringDataProviderID()
	{
		return stateToStringdataProviderID;
	}

	/**
	 * Make sure all data is GC'ed
	 */
	public void flushAllCachedItems()
	{
		if (findMode)
		{
			// no flushing in find mode
			return;
		}

		int row = getSelectedIndex();
		if (row == -1 && getSize() > 0)
		{
			// should never happen
			Debug.log("Flushing foundset with no selection: " + this, new RuntimeException("Flushing foundset with no selection")); //$NON-NLS-1$ //$NON-NLS-2$
			return; // safety
		}

		synchronized (pksAndRecords)
		{
			SafeArrayList<IRecordInternal> cachedRecords = pksAndRecords.getCachedRecords();
			for (int i = cachedRecords.size() - 1; i >= 0; i--)
			{
				if (row >= 0 && i < row + fsm.chunkSize)//leave every thing close to the selection
				{
					i = row - fsm.chunkSize;
					row = -1; // so this test isn't needed anymore
					continue;
				}

				IRecordInternal s = cachedRecords.get(i);
				if (s != null && s.existInDataSource())
				{
					cachedRecords.set(i, null);//clear
				}
			}
		}
	}

	/**
	 * Get a state for a certain (cached primary key) row
	 * 
	 * @throws RemoteException
	 */
	public IRecordInternal getRecord(int row)
	{
		return getRecord(row, fsm.chunkSize);
	}

	private IRecordInternal getRecord(int row, int sizeHint)
	{
		PksAndRecordsHolder pksAndRecordsCopy = pksAndRecords.shallowCopy();
		IDataSet pks = pksAndRecordsCopy.getPks();

		if (getSize() == 0 || pks == null || row < 0) return null;

		if (row >= pks.getRowCount() - 1 && pks.hadMoreRows())
		{
			int hint = ((row / fsm.pkChunkSize) + 2) * fsm.pkChunkSize;
			queryForMorePKs(pksAndRecordsCopy, hint, true);
		}
		IRecordInternal state = pksAndRecordsCopy.getCachedRecords().get(row);
		if (state == null && !findMode)
		{
			state = createRecord(row, sizeHint, pks, pksAndRecordsCopy.getCachedRecords());
		}
		// if state is still null (invalid pk?) then return prototype state 
		// so that in scripting and in ui everything does format (and global relations are able display)
		if (state == null)
		{
			state = proto;
		}
		return state;
	}

	public IRecordInternal[] getRecords(int startrow, int count)
	{
		if (count <= 0) return new IRecordInternal[0];

		List<IRecordInternal> retval = new ArrayList<IRecordInternal>();
		for (int i = startrow; i < Math.min(startrow + count, getSize()); i++)
		{
			retval.add(getRecord(i, count));
		}
		return retval.toArray(new IRecordInternal[retval.size()]);
	}

	/**
	 * Delete current/parameter record or the record under the given index.
	 * If the foundset is in multiselect mode, all selected records are deleted (when no parameter is used).
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord();
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @param index/record optional index of record to delete or record itself.
	 * 
	 * @return boolean true if all records could be deleted.
	 */
	public boolean js_deleteRecord(Object[] args) throws Exception
	{
		checkInitialized();

		int[] deleteRecIdx;
		if (args == null || args.length != 1)
		{
			deleteRecIdx = getSelectedIndexes();
		}
		else
		{
			if (args[0] instanceof IRecord)
			{
				deleteRecIdx = new int[] { getRecordIndex((IRecord)args[0]) };
			}
			else
			{
				deleteRecIdx = new int[] { Utils.getAsInteger(args[0], true) - 1 };
			}
		}

		boolean success = true;
		for (int i = deleteRecIdx.length - 1; i > -1; i--)
		{
			if (deleteRecIdx[i] >= 0 && deleteRecIdx[i] < getSize()) deleteRecord(deleteRecIdx[i]);
			else success = false;
		}
		return success;
	}

	/**
	 * Omit current record or the record under the given index, to be shown with loadOmittedRecords.
	 * If the foundset is in multiselect mode, all selected records are omitted (when no index parameter is used).

	 * Note: The omitted records list is discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invertRecords()
	 *
	 * @sample var success = %%prefix%%foundset.omitRecord();
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadOmittedRecords()
	 * 
	 * @param index optional index of record to omit.
	 * 
	 * @return boolean true if all records could be omitted.
	 */
	public boolean js_omitRecord(Object[] args) throws ServoyException
	{
		if (!isInitialized()) return false;

		int[] omitRecIdx;
		if (args == null || args.length != 1)
		{
			omitRecIdx = getSelectedIndexes();
		}
		else
		{
			omitRecIdx = new int[] { Utils.getAsInteger(args[0], true) - 1 };
		}

		return omitState(omitRecIdx);
	}

	/**
	 * Get the current sort columns.
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
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_sort(Object[])
	 * 
	 * @return String sort columns
	 */
	public String js_getCurrentSort()
	{
		return FoundSetManager.getSortColumnsAsString(lastSortColumns);
	}

	/**
	 * Sorts the foundset based on the given sort string or record comparator function.
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
	 * The comparator function is called to compare
	 * two records, that are passed as arguments, and
	 * it will return -1/0/1 if the first record is less/equal/greater
	 * then the second record.
	 * 
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
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
	 * @param sortString/recordComparator the specified columns (and sort order) or record comparator function
	 * @param defer optional boolean when true, the "sortString" will be just stored, without performing a query on the database (the actual sorting will be deferred until the next data loading action).
	 */
	public void js_sort(Object[] vargs) throws ServoyException
	{
		if (vargs.length == 0) return;
		if (vargs[0] instanceof Function)
		{
			sort((Function)vargs[0]);
		}
		else
		{
			String options = (String)vargs[0];
			boolean defer = vargs.length < 2 ? false : Utils.getAsBoolean(vargs[1]);
			sort(((FoundSetManager)getFoundSetManager()).getSortColumns(getTable(), options), defer);
		}
	}

	/**
	 * Delete all records in foundset, resulting in empty foundset.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteAllRecords();
	 * @return boolean true if all records could be deleted.
	 */
	public boolean js_deleteAllRecords() throws ServoyException
	{
		deleteAllRecords(); // will throw exception on error
		return true;
	}

	/**
	 * Duplicate current record or record at index in the foundset.
	 *
	 * @sample
	 * %%prefix%%foundset.duplicateRecord();
	 * %%prefix%%foundset.duplicateRecord(false); //duplicate the current record, adds at bottom
	 * %%prefix%%foundset.duplicateRecord(1,2); //duplicate the first record as second record
	 * //duplicates the record (record index 3), adds on top and selects the record
	 * %%prefix%%foundset.duplicateRecord(3,true,true);
	 * 
	 * @param index optional index of record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param location optional a boolean or number when true the new record is added as the topmost record, when a number, the new record is added at specified index ; defaults to 1.
	 * @param changeSelection optional boolean when true the selection is changed to the duplicated record; defaults to true.
	 *  
	 * @return true if succesful
	 */
	public int js_duplicateRecord(Object[] vargs) throws ServoyException
	{
		int selectedIndex = getSelectedIndex();
		boolean changeSelection = true;
		int indexToAdd = 0;
		if (vargs != null)
		{
			switch (vargs.length)
			{
				case 0 : // no args
					break;

				case 1 : // boolean (addOnTop) OR int (selectedIndex)
					if (vargs[0] instanceof Boolean)
					{
						indexToAdd = (Utils.getAsBoolean(vargs[0]) ? 0 : Integer.MAX_VALUE);
					}
					else if (vargs[0] instanceof Number)
					{
						selectedIndex = Utils.getAsInteger(vargs[0], true) - 1;
					}
					break;

				case 2 : // boolean (addOnTop) and boolean (changeSelection)
					// OR
					// int (selectedIndex) and boolean (addOnTop)
					// OR
					// int (selectedIndex) and int (indexToAdd)
					if (vargs[0] instanceof Boolean)
					{
						indexToAdd = (Utils.getAsBoolean(vargs[0]) ? 0 : Integer.MAX_VALUE);
						if (vargs[1] instanceof Boolean)
						{
							changeSelection = ((Boolean)vargs[1]).booleanValue();
						}
					}
					else if (vargs[0] instanceof Number)
					{
						selectedIndex = Utils.getAsInteger(vargs[0], true) - 1;
						if (vargs[1] instanceof Boolean)
						{
							indexToAdd = (Utils.getAsBoolean(vargs[1]) ? 0 : Integer.MAX_VALUE);
						}
						else if (vargs[1] instanceof Number)
						{
							indexToAdd = Utils.getAsInteger(vargs[1], true) - 1;
						}
					}
					break;

				default /* >= 3 */: // int (selectedIndex) and boolean (addOnTop) and boolean (changeSelection)
					// OR
					// int (selectedIndex) and int (indexToAdd) and boolean (changeSelection)
					if (vargs[0] instanceof Number)
					{
						selectedIndex = Utils.getAsInteger(vargs[0], true) - 1;
					}
					if (vargs[1] instanceof Boolean)
					{
						indexToAdd = (Utils.getAsBoolean(vargs[1]) ? 0 : Integer.MAX_VALUE);
					}
					else if (vargs[1] instanceof Number)
					{
						indexToAdd = Utils.getAsInteger(vargs[1], true) - 1;
					}
					if (vargs[2] instanceof Boolean)
					{
						changeSelection = ((Boolean)vargs[2]).booleanValue();
					}
					break;
			}
		}
		return duplicateRecord(selectedIndex, indexToAdd, changeSelection) + 1;
	}

	/**
	 * Create a new record in the foundset.
	 *
	 * @sample
	 * // foreign key data is only filled in for equals (=) relation items 
	 * var idx = %%prefix%%foundset.newRecord(false); // add as last record
	 * // %%prefix%%foundset.newRecord(); // adds as first record
	 * // %%prefix%%foundset.newRecord(2); //adds as second record
	 * if (idx >= 0) // returned index is -1 in case of failure 
	 * {
	 * 	%%prefix%%foundset.some_column = "some text";
	 * 	application.output("added on position " + idx);
	 * 	// when adding at the end of the foundset, the returned index
	 * 	// corresponds with the size of the foundset
	 * }
	 *
	 * @param location optional a boolean or number when true the new record is added as the topmost record, when a number, the new record is added at specified index ; defaults to 1.
	 * @param changeSelection optional boolean when true the selection is changed to the new record; defaults to true.
	 * 
	 * @return int index of new record.
	 */
	public int js_newRecord(Object[] vargs) throws ServoyException
	{
		boolean changeSelection = true;
		int indexToAdd = 0;
		if (vargs != null && vargs.length > 0)
		{
			if (vargs.length >= 1 && vargs[0] instanceof Boolean)
			{
				indexToAdd = (Utils.getAsBoolean(vargs[0]) ? 0 : Integer.MAX_VALUE);
			}
			else if (vargs.length >= 1 && vargs[0] instanceof Number)
			{
				indexToAdd = Utils.getAsInteger(vargs[0], true) - 1;
			}

			if (vargs.length >= 2 && vargs[1] instanceof Boolean)
			{
				changeSelection = ((Boolean)vargs[1]).booleanValue();
			}
		}

		if (indexToAdd >= 0)
		{
			return newRecord(null, indexToAdd, changeSelection) + 1;//javascript index is plus one
		}
		return -1;
	}

	/**
	 * Get the current record index of the foundset.
	 *
	 * @sample
	 * //gets the current record index in the current foundset
	 * var current = %%prefix%%foundset.getSelectedIndex();
	 * //sets the next record in the foundset
	 * %%prefix%%foundset.setSelectedIndex(current+1);	
	 * @return int current index (1-based)
	 */
	public int js_getSelectedIndex()
	{
		return getSelectedIndex() + 1;
	}

	/**
	 * Set the current record index.
	 *
	 * @sampleas js_getSelectedIndex()
	 *
	 * @param index int index to set (1-based)
	 */
	public void js_setSelectedIndex(Object[] args)
	{
		if (args == null || args.length == 0) return;
		int i = Utils.getAsInteger(args[0]);
		if (i >= 1 && i <= getSize())
		{
			setSelectedIndex(i - 1);
		}
	}

	/**
	 * Get the selected records indexes.
	 * When the founset is in multiSelect mode (see property multiSelect), selection can be a more than 1 index.
	 *
	 * @sample
	 * var current = %%prefix%%foundset.getSelectedIndexes();
	 * var newSelection = new Array();
	 * newSelection[0] = current[0];
	 * %%prefix%%foundset.setSelectedIndexes(newSelection);
	 * @return Array current indexes (1-based)
	 */
	public int[] js_getSelectedIndexes()
	{
		int[] selectedIndexes = getSelectedIndexes();
		if (selectedIndexes != null && selectedIndexes.length > 0)
		{
			for (int i = 0; i < selectedIndexes.length; i++)
			{
				selectedIndexes[i] += 1;
			}
		}

		return selectedIndexes;
	}

	/**
	 * Set the selected records indexes.
	 *
	 * @sampleas js_getSelectedIndexes()
	 * 
	 * @param indexes An array with indexes to set.
	 */
	public void js_setSelectedIndexes(Object[] indexes)
	{
		if (indexes == null || indexes.length == 0) return;
		ArrayList<Integer> selectedIndexes = new ArrayList<Integer>();

		Integer i;
		for (Object index : indexes)
		{
			i = new Integer(Utils.getAsInteger(index));
			if (selectedIndexes.indexOf(i) == -1) selectedIndexes.add(i);
		}
		int[] iSelectedIndexes = new int[selectedIndexes.size()];
		for (int j = 0; j < selectedIndexes.size(); j++)
		{
			iSelectedIndexes[j] = selectedIndexes.get(j).intValue() - 1;
		}
		setSelectedIndexes(iSelectedIndexes);
	}

	/**
	 * Get the number of records in this foundset.
	 *
	 * @sample
	 * for ( var i = 1 ; i <= %%prefix%%foundset.getMaxRecordIndex() ; i++ )
	 * {
	 * 	%%prefix%%foundset.setSelectedIndex(i);
	 * 	//do some action per record
	 * }
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getSize()
	 */
	@Deprecated
	public int js_getMaxRecordIndex()
	{
		return js_getSize();
	}

	/**
	 * Get the number of records in this foundset.
	 * This is the number of records loaded, note that when looping over a foundset, size() may
	 * increase as more records are loaded.
	 * 
	 * @sample 
	 * var nrRecords = %%prefix%%foundset.getSize()
	 * 
	 * // to loop over foundset, recalculate size for each record
	 * 	for (var i = 1; i <= %%prefix%%foundset.getSize(); i++)
	 * 	{
	 * 		var rec = %%prefix%%foundset.getRecord(i);
	 * 	}
	 * @return int current size.
	 */
	public int js_getSize()
	{
		return getSize();
	}

	/**
	 * Get the record object at the index.
	 *
	 * @sample var record = %%prefix%%foundset.getRecord(index);
	 *
	 * @param index int record index
	 * 
	 * @return Record record. 
	 */
	public IRecordInternal js_getRecord(int index)
	{
		return getRecord(index - 1); // index is row + 1, so we substract 1 here.
	}


	/**
	 * Get the record index.
	 *
	 * @sample var index = %%prefix%%foundset.getRecordIndex(record);
	 *
	 * @param record Record
	 * 
	 * @return int index. 
	 */
	public int js_getRecordIndex(IRecordInternal record)
	{
		return getRecordIndex(record) + 1;
	}

	/**
	 * Get the selected record.
	 *
	 * @sample var selectedRecord = %%prefix%%foundset.getSelectedRecord();
	 * @return Record record. 
	 */
	public IRecordInternal js_getSelectedRecord()
	{
		return getRecord(getSelectedIndex());
	}

	/**
	 * Get the selected records.
	 * When the founset is in multiSelect mode (see property multiSelect), selection can be a more than 1 record.
	 *
	 * @sample var selectedRecords = %%prefix%%foundset.getSelectedRecords();
	 * @return Array current records.
	 */
	public IRecordInternal[] js_getSelectedRecords()
	{
		int[] selectedIndexes = getSelectedIndexes();
		IRecordInternal[] selectedRecords = new IRecordInternal[selectedIndexes.length];
		for (int i = 0; i < selectedIndexes.length; i++)
		{
			selectedRecords[i] = getRecord(selectedIndexes[i]);
		}

		return selectedRecords;
	}

	/**
	 * Get or set the multiSelect flag of the foundset.
	 *
	 * @sample
	 * // allow user to select multiple rows.
	 * %%prefix%%foundset.multiSelect = true;
	 */
	public boolean js_isMultiSelect()
	{
		return isMultiSelect();
	}

	public void js_setMultiSelect(boolean multiSelect)
	{
		setMultiSelect(multiSelect);
	}

	public abstract void setMultiSelect(boolean isMultiSelect);

	public abstract boolean isMultiSelect();

	private int lastRecordCreatedIndex = -1;

	//do real query for state
	// is already synched by caller around the PksAndRecordsHolder instance
	private Record createRecord(int row, int sz, IDataSet pks, SafeArrayList<IRecordInternal> cachedRecords)
	{
		int a_sizeHint = (sz > fsm.pkChunkSize) ? fsm.pkChunkSize : sz; //safety, SQL in limit

		if (Math.abs(row - lastRecordCreatedIndex) > 30 && cachedRecords.get(row - 1) == null && cachedRecords.get(row + 1) == null)
		{
			removeRecords(row, false, cachedRecords);
		}
		lastRecordCreatedIndex = row;
		Record retval = null;
		try
		{
			int startRow = row;
			int sizeHint = a_sizeHint;
			if (row > 0)
			{
				if (cachedRecords.get(row - 1) == null)
				{
					if (cachedRecords.get(row + 1) != null)
					{
						startRow = row - fsm.chunkSize;
					}
					else
					{
						startRow = row - fsm.chunkSize / 2;
					}
					startRow = Math.max(startRow, 0);
				}
			}
			else
			{
				sizeHint = ((cachedRecords.get(row + 1) != null || pks.getRowCount() == 1) ? 1 : a_sizeHint);
			}
			int oldSize = pks.getRowCount();
			List<Row> rows = rowManager.getRows(pks, startRow, sizeHint);
			//construct States
			for (int r = rows.size(); --r >= 0;)
			{
				if (cachedRecords.get(startRow + r) == null)
				{
					Row rowData = rows.get(r);
					if (rowData != null)
					{
						Record state = new Record(this, rowData);
						cachedRecords.set(startRow + r, state);
					}
					else
					{
						pks.removeRow(startRow + r);
						cachedRecords.remove(startRow + r);
					}
				}
			}
			retval = (Record)cachedRecords.get(row);
			if (retval == null)
			{
				rows = rowManager.getRows(pks, row, 1);
				if (rows.size() == 1)
				{
					Row rowData = rows.get(0);
					if (rowData != null)
					{
						Record state = new Record(this, rowData);
						cachedRecords.set(row, state);
						retval = state;
					}
				}
				else
				{
					if (row < pks.getRowCount())
					{
						pks.removeRow(row);
						cachedRecords.remove(row);
					}
				}
			}
			int newSize = pks.getRowCount();
			if (oldSize != newSize)
			{
				fireDifference(oldSize, newSize);
			}
			removeRecords(row, true, cachedRecords);
		}
		catch (ServoyException ex)
		{
			if (ex.getErrorCode() == ServoyException.InternalCodes.CLIENT_NOT_REGISTERED)
			{
				fsm.getApplication().reportError(fsm.getApplication().getI18NMessage("servoy.foundSet.error.loadingRecord"), ex); //$NON-NLS-1$
				throw new RuntimeException(ex);
			}
			retval = (Record)cachedRecords.get(row);
			if (retval == null)
			{
				// make empty row so that it wont be an infinite loop of record lookups!!
				Row data = rowManager.createNotYetExistInDBRowObject(sheet.getNewRecordData(fsm.getApplication(), this), false);
				data.flagExistInDB();
				retval = new Record(this, data);
				cachedRecords.set(row, retval);
			}
			fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.loadingRecord"), ex); //$NON-NLS-1$
		}
		return retval;
	}

	//sliding window cache for selectedindex
	// caller already synced on PksAndRecordsHolder
	private void removeRecords(int row, boolean breakOnNull, SafeArrayList<IRecordInternal> cachedRecords)
	{
		int cacheSize = fsm.chunkSize * 3;
		int selected = getSelectedIndex();
		if (row > cacheSize)
		{
			int counter = row - cacheSize;
			while (counter != -1)
			{
				// Don't remove the selected
				if (counter == selected)
				{
					counter--;
					continue;
				}
				Object tmp = cachedRecords.set(counter, null);
				if (tmp instanceof Record)
				{
					Record record = ((Record)tmp);
					if (record.isEditing() || !record.existInDataSource())
					{
						cachedRecords.set(counter, record);
					}
				}
				else
				{
					// there is no prev record so already cleaned up. Break 
					if (breakOnNull) break;
				}
				counter--;
			}
		}
		int counter = row + cacheSize;
		while (cachedRecords.size() > counter)
		{
			// Don't remove the selected
			if (counter == selected)
			{
				counter++;
				continue;
			}
			Object tmp = cachedRecords.set(counter, null);
			if (tmp instanceof Record)
			{
				Record record = ((Record)tmp);
				if (record.isEditing() || !record.existInDataSource())
				{
					cachedRecords.set(counter, record);
				}
			}
			else
			{
				// there is no prev record so already cleaned up. Break 
				if (breakOnNull) break;
			}
			counter++;
		}
	}

	public IRecordInternal[] getEditingRecords()
	{
		return getFoundSetManager().getEditRecordList().getEditedRecords(this);
	}

	/**
	 * Found set is using scriptengine to recalculate the specified calculation,check first with containsCalculation before calling
	 */
	public Object getCalculationValue(IRecordInternal state, String dataProviderID, Object[] vargs, UsedDataProviderTracker usedDataProviderTracker)
	{
		try
		{
			Object obj;
			TableScope tableScope = (TableScope)fsm.getScriptEngine().getTableScope(sheet.getTable());
			tableScope.setArguments(vargs);
			tableScope.setUsedDataProviderTracker(usedDataProviderTracker);
			Scriptable previous = tableScope.getPrototype();
			try
			{
				tableScope.setPrototype((Scriptable)state);//make sure its set correctly
				obj = tableScope.getCalculationValue(dataProviderID, tableScope);
				if (obj instanceof Byte)//fix for postgress
				{
					obj = new Integer(((Byte)obj).intValue());
				}
				else
				{
					Column column = sheet.getTable().getColumn(dataProviderID);
					if (column != null)
					{
						// stored calculation
						if (column.getScale() > 0 && Column.mapToDefaultType(column.getType()) == IColumnTypes.NUMBER && obj != null)
						{
							// round the calculation to the column scale.
							// if rounding results in the old value we do not have to save.
							try
							{
								obj = Utils.roundNumber(obj, column.getLength(), true);
							}
							catch (Exception e)
							{
								Debug.error("Could not round stored calculation '" + dataProviderID + '\'', e); //$NON-NLS-1$
							}
						}
					}
				}
				//TODO: in developer we must check if the return type matches the one specified on a calculation otherwise relations will not work in some cases
			}
			finally
			{
				tableScope.setPrototype(previous);
			}
			return obj;
		}
		catch (Exception ex)
		{
			//fsm.getApplication().reportJSError(Messages.getString("servoy.error.executingCalculation",new Object[] {dataProviderID,getTable().getName(),ex.getMessage()}),ex) ;	 //$NON-NLS-1$
			fsm.getApplication().reportJSError(ex.getMessage(), ex);
			Debug.error("error executing calc: " + dataProviderID, ex); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Check if this Foundset contains the specified global or aggregate dataprovider
	 * 
	 * @param dataProviderID the dataprovider to check
	 */
	public boolean containsDataProvider(String dataProviderID) //as shared (global or aggregate)
	{
		if ("recordIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;//deprecated
		}
		else if ("selectedIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;
		}
		else if ("maxRecordIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;
		}
		else if ("serverURL".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;
		}

		try
		{
			// have to test for a global prefix. because setDataProviderId does check for this.
			if (dataProviderID.length() > ScriptVariable.GLOBAL_DOT_PREFIX.length() && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				Scriptable sol_scope = fsm.getScriptEngine().getSolutionScope().getGlobalScope();
				return sol_scope.has(dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length()), sol_scope);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		// in find mode aggregates are not queried, they may be stored in find state
		return !findMode && sheet.containsAggregate(dataProviderID);
	}

	/**
	 * Get the value from the specified global or aggregate dataprovider, always check first
	 * 
	 * @param dataProviderID the dataprovider
	 * @return the value
	 */
	public Object getDataProviderValue(String dataProviderID) //as shared (global or aggregate) value
	{
		if ("recordIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return new Integer(getSelectedIndex() + 1);//deprecated
		}
		else if ("selectedIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return new Integer(getSelectedIndex() + 1);
		}
		else if ("maxRecordIndex".equals(dataProviderID) || "lazyMaxRecordIndex".equals(dataProviderID)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return new Integer(getSize());
		}
		else if ("serverURL".equals(dataProviderID)) //$NON-NLS-1$
		{
			return getFoundSetManager().getApplication().getScriptEngine().getSystemConstant("serverURL"); //$NON-NLS-1$
		}

		try
		{
			if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				String global = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
				GlobalScope g_scope = fsm.getScriptEngine().getSolutionScope().getGlobalScope();
				if (g_scope.has(global, g_scope))
				{
					return g_scope.get(global);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}

		Object obj = null;
		int size = getSize(); // get size outside sync block
		synchronized (aggregateCache)
		{
			if (aggregateCache.containsKey(dataProviderID))
			{
				obj = aggregateCache.get(dataProviderID);
			}
			else
			{
				if (sheet.containsAggregate(dataProviderID))
				{
					if (size == 0)
					{
						// No need to query for the aggregate, value will always be null anyway.
						// Still need to have the aggregate in the cache in case calcs depend on them (must fire when cache is cleared)
						aggregateCache.put(dataProviderID, null);
					}
					else
					{
						obj = getAggregateValue(dataProviderID);
					}
				}
			}
		}
		return obj;
	}

	private Object getAggregateValue(String dataProviderID)
	{
		queryForAggregate(getAggregateSelect(sheet, pksAndRecords.getQuerySelectForReading()));
		return aggregateCache.get(dataProviderID);
	}

	public static QuerySelect getAggregateSelect(SQLSheet sheet, QuerySelect sqlSelect)
	{
		Map<String, QuerySelect> aggregate = sheet.getAggregates();
		if (aggregate == null || aggregate.size() == 0) return null;

		return SQLGenerator.createAggregateSelect(sqlSelect, sheet.getAggregates().values(), sheet.getTable().getRowIdentColumns());
	}

	//Used by globals, aggregates(are skipped) and related field creation  
	public Object setDataProviderValue(String dataProviderID, Object value)
	{
		try
		{
			if ("recordIndex".equals(dataProviderID) || "selectedIndex".equals(dataProviderID) || "maxRecordIndex".equals(dataProviderID) || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"lazyMaxRecordIndex".equals(dataProviderID) || "serverURL".equals(dataProviderID)) //$NON-NLS-1$ //$NON-NLS-2$
			{
				return getDataProviderValue(dataProviderID);
			}

			if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				GlobalScope gscope = fsm.getScriptEngine().getSolutionScope().getGlobalScope();
				String global = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
				if (gscope.has(global, gscope))
				{
					Object oldVal = gscope.put(global, value);
					if (!Utils.equalObjects(oldVal, value))
					{
						fireFoundSetEvent(0, getSize() - 1, FoundSetEvent.CHANGE_UPDATE);
					}
					return oldVal;
				}
			}
			else if (findMode || !sheet.containsAggregate(dataProviderID)) // in find mode aggregates are not queried but stored in find state
			{
				IRecordInternal state = getRecord(getSelectedIndex());
				if (state != null)
				{
					Object currentValue = state.getValue(dataProviderID);
					if (!Utils.equalObjects(currentValue, value))
					{
						boolean editStarted = false;
						if (!state.isEditing())
						{
							editStarted = state.startEditing();
							// if record couldn't be started return the value (locked??)
							if (!editStarted) return value;
						}
						// first set the value and get the old value
						return state.setValue(dataProviderID, value);
					}
					return currentValue;
				}

				//try to create a record if allowed
				Relation r = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
				if (r != null && r.getAllowCreationRelatedRecords())
				{
					try
					{
						int i = newRecord(true);
						IRecordInternal s = getRecord(i);
						if (s != null)
						{
							return s.setValue(dataProviderID, value);
						}
					}
					catch (ServoyException se)
					{
						fsm.getApplication().reportError(se.getLocalizedMessage(), se);
					}
				}
			}
		}
		catch (IllegalArgumentException iae)
		{
			throw iae;
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	private void queryForAggregate(QuerySelect select)
	{
		try
		{
			String transaction_id = fsm.getTransactionID(sheet);
			long time = System.currentTimeMillis();
			IDataSet ds = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, select,
				fsm.getTableFilterParams(sheet.getServerName(), select), false, 0, 1, IDataServer.AGGREGATE_QUERY);
			if (Debug.tracing())
			{
				Debug.trace("Aggregate query, time: " + (System.currentTimeMillis() - time) + ", thread: " + Thread.currentThread().getName() + ", SQL: " + select.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			fillAggregates(select, ds);
		}
		catch (Exception ex)
		{
			fsm.getApplication().reportJSError(fsm.getApplication().getI18NMessage("servoy.foundSet.error.aggregate"), ex); //$NON-NLS-1$
		}
	}

	/**
	 * @param select
	 * @param ds
	 */
	protected final void fillAggregates(QuerySelect select, IDataSet ds)
	{
		if (ds.getRowCount() != 0)
		{
			List<IQuerySelectValue> columns = select.getColumns();
			Object[] row = ds.getRow(0);
			if (row.length == columns.size())
			{
				for (int i = 0; i < row.length; i++)
				{
					aggregateCache.put((columns.get(i)).getAlias(), row[i]);
				}
			}
		}
	}

	public boolean isValidRelation(String name)
	{
		return fsm.getApplication().getFlattenedSolution().getRelationSequence(name) != null;
	}

	public int getSize()
	{
		int retval = 0;
		if (findMode)
		{
			retval = pksAndRecords.getCachedRecords().size();
		}
		else
		{
			IDataSet pks = pksAndRecords.getPks();
			if (pks != null)
			{
				retval = pks.getRowCount();
			}
		}
		return retval;
	}

	public int getRecordIndex(IRecord record)
	{
		return pksAndRecords.getCachedRecords().indexOf(record);
	}

	@Deprecated
	public void deleteAll() throws ServoyException
	{
		deleteAllRecords();
	}

	public void deleteAllRecords() throws ServoyException
	{
		try
		{
			deleteAllInternal();
		}
		finally
		{
			fsm.clearAllDeleteSets();
		}
	}

	public void deleteAllInternal() throws ServoyException
	{
		Table table = sheet.getTable();
		if (table == null)
		{
			return;
		}

		fireSelectionAdjusting();

		boolean partOfBiggerDelete = false;
		//does have access, does not have join to other table and doesn't have a on delete method
		QuerySelect sqlSelect;
		IDataSet currentPKs;
		synchronized (pksAndRecords)
		{
			sqlSelect = pksAndRecords.getQuerySelectForReading();
			currentPKs = pksAndRecords.getPks();
		}
		if (!hasAccess(IRepository.TRACKING) && sqlSelect.getJoins() == null && !tableHasOnDeleteMethods())
		{
			if (!hasAccess(IRepository.DELETE))
			{
				throw new ApplicationException(ServoyException.NO_DELETE_ACCESS);
			}

			boolean hasRelationsWithDelete = false;
			Iterator<Relation> it = fsm.getApplication().getFlattenedSolution().getRelations(table, true, false);
			while (it.hasNext())
			{
				Relation element = it.next();
				if ((element.getDeleteRelatedRecords() || !element.getAllowParentDeleteWhenHavingRelatedRecords()) && !element.isGlobal())
				{
					Debug.trace("Foundset deleted per-record because relation '" + element.getName() + "' requires some checks"); //$NON-NLS-1$ //$NON-NLS-2$ 
					hasRelationsWithDelete = true;
					break;
				}
			}

			if (!hasRelationsWithDelete)
			{
				getFoundSetManager().getEditRecordList().removeEditedRecords(this);

				//do sql delete all at once
				QueryDelete delete_sql = new QueryDelete(sqlSelect.getTable());
				delete_sql.setCondition(sqlSelect.getWhereClone());

				IDataSet deletePKs = null;
				boolean allFoundsetRecordsLoaded = (pksAndRecords.getCachedRecords().size() == getSize() && !hadMoreRows());
				if (allFoundsetRecordsLoaded)
				{
					deletePKs = currentPKs;
				}
				else
				{
					deletePKs = new BufferedDataSet();
					deletePKs.addRow(new Object[] { ValueFactory.createTableFlushValue() });
				}
				String tid = fsm.getTransactionID(table.getServerName());
				SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, table.getServerName(), table.getName(), deletePKs, tid, delete_sql,
					fsm.getTableFilterParams(table.getServerName(), delete_sql));
				try
				{
					Object[] results = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), new ISQLStatement[] { statement });
					for (int i = 0; results != null && i < results.length; i++)
					{
						if (results[i] instanceof ServoyException)
						{
							throw (ServoyException)results[i];
						}
					}

					if (!allFoundsetRecordsLoaded)
					{
						fsm.flushCachedDatabaseData(fsm.getDataSource(table));
					}

					partOfBiggerDelete = true;
				}
				catch (ApplicationException aex)
				{
					if (allFoundsetRecordsLoaded || aex.getErrorCode() != ServoyException.RECORD_LOCKED)
					{
						throw aex;
					}
					// a record was locked by another client, try per-record
					Debug.log("Could not delete all records in 1 statement (a record may be locked), trying per-record");
				}
				catch (RemoteException e)
				{
					throw new RepositoryException(e);
				}
			}
		}

		// Need to get all the PKs, recursive delete may not actually remove the PK from the list because it is already being deleted.
		if (!partOfBiggerDelete)
		{
			queryForMorePKs(pksAndRecords.shallowCopy(), -1, false);
		}

		try
		{
			for (int i = getSize() - 1; i >= 0; i--)
			{
				deleteRecord(i, partOfBiggerDelete);
			}
		}
		finally
		{
			int correctedSize = getCorrectedSizeForFires();
			if (correctedSize > -1) fireFoundSetEvent(0, correctedSize, FoundSetEvent.CHANGE_DELETE);
		}
	}

	public void deleteRecord(int row) throws ServoyException
	{
		((FoundSetManager)getFoundSetManager()).clearAllDeleteSets();
		try
		{
			deleteRecord(row, false);
		}
		finally
		{
			((FoundSetManager)getFoundSetManager()).clearAllDeleteSets();
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#deleteRecord(com.servoy.j2db.dataprocessing.Record)
	 */
	public void deleteRecord(Record record) throws ServoyException
	{
		if (record.getParentFoundSet() != this) throw new ApplicationException(ServoyException.INVALID_INPUT, new RuntimeException(
			"Record not from this foundset")); //$NON-NLS-1$
		int recordIndex = getRecordIndex(record);
		if (recordIndex == -1) recordIndex = getRecordIndex(record.getPK(), 0);
		if (recordIndex == -1) throw new ApplicationException(ServoyException.INVALID_INPUT, new RuntimeException("Record pk not found in this foundset")); //$NON-NLS-1$
		deleteRecord(recordIndex);
	}

	// part of bigger delete == sql foundset delete is already done for this row (see deleteAll)
	private void deleteRecord(int row, boolean partOfBiggerDelete) throws ServoyException
	{
		if (sheet.getTable() == null)
		{
			return;
		}
		if (!hasAccess(IRepository.DELETE))
		{
			throw new ApplicationException(ServoyException.NO_DELETE_ACCESS);
		}

		IRecordInternal state = null;
		if (partOfBiggerDelete)
		{
			state = pksAndRecords.getCachedRecords().get(row);
		}
		else
		{
			state = getRecord(row);
		}

		if (!findMode && state != null)
		{
			if (!fsm.getRowManager(fsm.getDataSource(sheet.getTable())).addRowToDeleteSet(state.getPKHashKey()))
			{
				// already being deleted in recursion
				return;
			}

			if (!partOfBiggerDelete)
			{
				// check for related data
				Iterator<Relation> it = fsm.getApplication().getFlattenedSolution().getRelations(sheet.getTable(), true, false);
				while (it.hasNext())
				{
					Relation rel = it.next();
					if (!rel.getAllowParentDeleteWhenHavingRelatedRecords() && !rel.isExactPKRef(fsm.getApplication().getFlattenedSolution()) &&
						!rel.isGlobal())
					{
						IFoundSetInternal set = state.getRelatedFoundSet(rel.getName(), null);
						if (set != null && set.getSize() > 0)
						{
							Debug.log("Delete not granted due to AllowParentDeleteWhenHavingRelatedRecords size: " + set.getSize() + " from record with PK: " + state.getPKHashKey() + " index in foundset: " + row + " blocked by relation: " + rel.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							throw new ApplicationException(ServoyException.NO_PARENT_DELETE_WITH_RELATED_RECORDS, new Object[] { rel.getName() });
						}
					}
				}

				// delete the related data
				it = fsm.getApplication().getFlattenedSolution().getRelations(sheet.getTable(), true, false);
				while (it.hasNext())
				{
					Relation rel = it.next();
					if (rel.getDeleteRelatedRecords() && !rel.isGlobal())//if completely global never delete do cascade delete
					{
						IFoundSetInternal set = state.getRelatedFoundSet(rel.getName(), null);
						if (set != null)
						{
							Debug.trace("******************************* delete related set size: " + set.getSize() + " from record with PK: " + state.getPKHashKey() + " index in foundset: " + row); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							set.deleteAllInternal();
						}
					}
				}
			}

			if (state.existInDataSource())
			{
				if (!partOfBiggerDelete)
				{
					try
					{
						// see EditRecordList.stopEditing
						if (!testTableEvents(state))
						{
							// trigger returned false
							Debug.log("Delete not granted for the table " + getTable()); //$NON-NLS-1$
							throw new ApplicationException(ServoyException.DELETE_NOT_GRANTED);
						}
					}
					catch (DataException e)
					{
						// trigger threw exception
						state.getRawData().setLastException(e);
						getFoundSetManager().getEditRecordList().markRecordAsFailed(state);
						Debug.log("Delete not granted for the table " + getTable() + ", pre-delete trigger threw exception"); //$NON-NLS-1$ //$NON-NLS-2$
						throw new ApplicationException(ServoyException.DELETE_NOT_GRANTED);
					}
				}
				Row data = state.getRawData();
				rowManager.deleteRow(this, data, hasAccess(IRepository.TRACKING), partOfBiggerDelete);

				executeAfterDeleteTrigger(state);
			}
		}

		removeRecordInternalEx(state, row);
	}

	/**
	 * @param record
	 * @return
	 */
	private boolean testTableEvents(IRecordInternal record) throws ServoyException
	{
		FlattenedSolution solutionRoot = fsm.getApplication().getFlattenedSolution();
		Iterator<TableNode> tableNodes = solutionRoot.getTableNodes(getTable());
		while (tableNodes.hasNext())
		{
			TableNode tn = tableNodes.next();
			int methodId = tn.getOnDeleteMethodID();
			if (methodId > 0)
			{
				ScriptMethod globalScriptMethod = solutionRoot.getScriptMethod(methodId);
				if (globalScriptMethod != null)
				{
					IExecutingEnviroment scriptEngine = fsm.getApplication().getScriptEngine();
					GlobalScope gscope = scriptEngine.getSolutionScope().getGlobalScope();
					Object function = gscope.get(globalScriptMethod.getName());
					if (function instanceof Function)
					{
						try
						{
							Object retval = scriptEngine.executeFunction(
								((Function)function),
								gscope,
								gscope,
								Utils.arrayMerge((new Object[] { record }), Utils.parseJSExpressions(tn.getInstanceMethodArguments("onDeleteMethodID"))), false, true); //$NON-NLS-1$
							if (Boolean.FALSE.equals(retval))
							{
								// delete method returned false. should block the delete.
								return false;
							}
						}
						catch (JavaScriptException e)
						{
							// delete method threw exception. should block the delete.
							throw new DataException(ServoyException.RECORD_VALIDATION_FAILED, e.getValue());
						}
						catch (Exception e)
						{
							Debug.error(e);
							throw new ServoyException(ServoyException.SAVE_FAILED, new Object[] { e.getMessage() });
						}
					}
				}
			}
		}
		return true;
	}

	private void executeAfterDeleteTrigger(IRecordInternal record) throws ServoyException
	{
		FlattenedSolution solutionRoot = fsm.getApplication().getFlattenedSolution();
		Iterator<TableNode> tableNodes = solutionRoot.getTableNodes(getTable());
		while (tableNodes.hasNext())
		{
			TableNode tn = tableNodes.next();
			int methodId = tn.getOnAfterDeleteMethodID();
			if (methodId > 0)
			{
				ScriptMethod globalScriptMethod = solutionRoot.getScriptMethod(methodId);
				if (globalScriptMethod != null)
				{
					IExecutingEnviroment scriptEngine = fsm.getApplication().getScriptEngine();
					GlobalScope gscope = scriptEngine.getSolutionScope().getGlobalScope();
					Object function = gscope.get(globalScriptMethod.getName());
					if (function instanceof Function)
					{
						try
						{
							scriptEngine.executeFunction(
								((Function)function),
								gscope,
								gscope,
								Utils.arrayMerge((new Object[] { record }), Utils.parseJSExpressions(tn.getInstanceMethodArguments("onAfterDeleteMethodID"))), false, false); //$NON-NLS-1$
						}
						catch (Exception e)
						{
							Debug.error(e);
							throw new ServoyException(ServoyException.SAVE_FAILED, new Object[] { e.getMessage() });
						}
					}
				}
			}
		}
	}

	private boolean tableHasOnDeleteMethods()
	{
		try
		{
			FlattenedSolution solutionRoot = fsm.getApplication().getFlattenedSolution();
			Iterator<TableNode> tableNodes = solutionRoot.getTableNodes(getTable());
			while (tableNodes.hasNext())
			{
				TableNode node = tableNodes.next();
				int methodId = node.getOnDeleteMethodID();
				if (methodId > 0 && solutionRoot.getScriptMethod(methodId) != null)
				{
					return true;
				}
				methodId = node.getOnAfterDeleteMethodID();
				if (methodId > 0 && solutionRoot.getScriptMethod(methodId) != null)
				{
					return true;
				}
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return false;
	}


	protected void removeRecordInternal(int row)
	{
		IRecordInternal state = pksAndRecords.getCachedRecords().get(row); // if state was not cached no need to query for it here
		removeRecordInternalEx(state, row);
	}

	private void removeRecordInternalEx(IRecordInternal state, int row)
	{
		//state can be null in case the row is already deleted in the database, but the pk is present in this foundset

		EditRecordList editRecordList = getFoundSetManager().getEditRecordList();
		if (state != null)
		{
			boolean existInDataSource = state.existInDataSource();
			try
			{
				// we NEVER should loose states which are editing, because the global editing state will stay otherwise! 
				if (state.isEditing())
				{
					if (existInDataSource)
					{
						state.stopEditing();
					}
					else
					{
						editRecordList.removeEditedRecord(state);
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		synchronized (pksAndRecords)
		{
			pksAndRecords.getCachedRecords().remove(row);
			if (!findMode)
			{
				IDataSet pks = pksAndRecords.getPks();
				if (pks.getRowCount() > row)
				{
					pks.removeRow(row);
					int dbIndexLastPk = pksAndRecords.getDbIndexLastPk();
					if (dbIndexLastPk > 0)
					{
						pksAndRecords.setDbIndexLastPk(dbIndexLastPk - 1);
					}
				}
			}
		}

		// delete the row data so it won't be updated by other foundsets also having records to this rowdata.
		if (state != null && state.getRawData() != null) //the state can be a prototype state(without row) if the pk is notified but the record is not yet lookedup before notify delete
		{
			state.getRawData().flagExistInDB();
		}

		if (getSize() == 0) setSelectedIndex(-1);

		fireFoundSetEvent(row, row, FoundSetEvent.CHANGE_DELETE);

		if (aggregateCache.size() > 0)
		{
			fireAggregateChangeWithEvents(null);
		}
		else
		{
			walkParents(editRecordList.getFoundsetEventMap());
			editRecordList.fireEvents();
		}
	}

	public boolean showOmitted() throws ServoyException
	{
		if (omittedPKs == null) omittedPKs = new BufferedDataSet();
		boolean b = loadExternalPKList(omittedPKs);
		omittedPKs = null;
		return b;
	}

	public void invert() throws ServoyException
	{
		int sizeBefore;
		QuerySelect sqlSelect;
		ISQLCondition where;
		synchronized (pksAndRecords)
		{
			sizeBefore = getSize();
			sqlSelect = pksAndRecords.getQuerySelectForReading();
			where = sqlSelect.getCondition(SQLGenerator.CONDITION_SEARCH);
			if (where == null)
			{
				pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, sqlSelect);
			}
			else
			{
				sqlSelect = pksAndRecords.getQuerySelectForModification();
				sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, where.negate());
				clearOmit(sqlSelect);
				// set pks here in case a refresh comes along
				pksAndRecords.setPksAndQuery(pksAndRecords.getPks(), pksAndRecords.getDbIndexLastPk(), sqlSelect);
			}
		}

		if (where != null)
		{
			//cache pks
			String transaction_id = fsm.getTransactionID(sheet);
			try
			{
				IDataSet pks = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, sqlSelect,
					fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), !sqlSelect.isUnique(), 0, fsm.pkChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
				synchronized (pksAndRecords)
				{
					// optimistic locking, if the query has been changed in the mean time forget about the refresh
					if (sqlSelect != pksAndRecords.getQuerySelectForReading())
					{
						Debug.log("invert: query was changed during refresh, not resetting old query"); //$NON-NLS-1$
						return;
					}
					pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), sqlSelect);
				}
			}
			catch (RemoteException e)
			{
				pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, sqlSelect);
				throw new RepositoryException(e);
			}
		}

		if (aggregateCache.size() > 0)
		{
			fireAggregateChangeWithEvents(null);
		}

		fireDifference(sizeBefore, getSize());
	}


	public boolean omitState(int[] rows) throws ServoyException
	{
		if (sheet.getTable() == null)
		{
			return false;
		}

		List<IRecordInternal> recordsToOmit = new ArrayList<IRecordInternal>();
		boolean success = true;
		for (int row : rows)
		{
			if (row < 0 || row >= getSize())
			{
				success = false;
				continue;
			}
			IRecordInternal state = getRecord(row);
			if (state != null && fsm.getEditRecordList().stopEditing(false, state) == ISaveConstants.STOPPED)
			{
				recordsToOmit.add(state);
			}
			else
			{
				success = false;
			}
		}

		if (recordsToOmit.size() > 0)
		{
			if (omittedPKs == null) omittedPKs = new BufferedDataSet();

			for (IRecordInternal dsState : recordsToOmit)
			{
				omittedPKs.addRow(dsState.getPK());
			}

			QuerySelect sqlSelect = pksAndRecords.getQuerySelectForModification();

			// replace the OMIT condition, keep sort (could be custom sort, different from lastSortColumns)
			List<IQuerySelectValue> pkQueryColumns = sqlSelect.getColumns();
			sqlSelect.setCondition(SQLGenerator.CONDITION_OMIT, SQLGenerator.createSetConditionFromPKs(ISQLCondition.NOT_OPERATOR,
				pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]), sheet.getTable().getRowIdentColumns(), omittedPKs));

			refreshFromDBInternal(sqlSelect, false, false, fsm.pkChunkSize, true);
		}

		return success;
	}

	public String getAsTabSeparated(int row)
	{
		if (row < 0)
		{
			//all
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < getSize(); i++)
			{
				IRecordInternal s = getRecord(i);
				if (s != null)
				{
					sb.append(s.getAsTabSeparated());
					if (i < getSize() - 1) sb.append("\n"); //$NON-NLS-1$
				}
			}
			return sb.toString();
		}
		IRecordInternal s = getRecord(row);
		if (s != null)
		{
			return s.getAsTabSeparated();
		}
		return ""; //$NON-NLS-1$
	}

	public int newRecord() throws ServoyException
	{
		return newRecord(null, 0, true);
	}

	@Deprecated
	public int newRecord(boolean addOnTop) throws ServoyException
	{
		return newRecord(null, (addOnTop ? 0 : Integer.MAX_VALUE), true);
	}

	public int newRecord(int indexToAdd) throws ServoyException
	{
		return newRecord(null, indexToAdd, true);
	}

	@Deprecated
	public int newRecord(boolean addOnTop, boolean changeSelection) throws ServoyException
	{
		return newRecord(null, (addOnTop ? 0 : Integer.MAX_VALUE), changeSelection);
	}

	public int newRecord(int indexToAdd, boolean changeSelection) throws ServoyException
	{
		return newRecord(null, indexToAdd, changeSelection);
	}

	private IRecordInternal getNewRecord(Row rowData) throws ApplicationException
	{
		IRecordInternal newRecord;
		if (findMode)
		{
			if (pksAndRecords.getCachedRecords().size() > fsm.pkChunkSize) return null;//limit to 200
			newRecord = new FindState(this);
		}
		else if (rowData == null)
		{
			newRecord = createRecord();
		}
		else
		{
			newRecord = new Record(this, rowData);
		}
		return newRecord;
	}

	protected int newRecord(Row rowData, int indexToAdd, boolean changeSelection) throws ServoyException
	{
		IRecordInternal newRecord = getNewRecord(rowData);
		if (newRecord == null) return -1;
		return addRecord(newRecord, indexToAdd, changeSelection);
	}

	private int addRecord(IRecordInternal newRecord, int idx, boolean changeSelection) throws ServoyException
	{
		if (newRecord == null)
		{
			return -1;
		}

		if (newRecord instanceof FindState)
		{
			if ((fsm.getEditRecordList().stopIfEditing(this) & (ISaveConstants.VALIDATION_FAILED + ISaveConstants.SAVE_FAILED)) != 0)
			{
				//we cannot allow finds when there are editting records...it possible to start (related!)find on table which whould possible not include editing records
				if (Debug.tracing())
				{
					Debug.trace("new record failed because there where records in edit mode and auto save is false"); //$NON-NLS-1$
				}
				return -1;
			}
		}

		if (!initialized && !findMode)
		{
			loadAllRecords();
		}

		int size = getSize();
		int indexToAdd = (idx < 0) ? 0 : (idx > size) ? size : idx;

		synchronized (pksAndRecords)
		{
			SafeArrayList<IRecordInternal> cachedRecords = null;
			IDataSet pks = pksAndRecords.getPks();
			if (pks == null)
			{
				cachedRecords = pksAndRecords.setPks(pks = new BufferedDataSet(), 0);
			}
			else
			{
				cachedRecords = pksAndRecords.getCachedRecords();
			}
			cachedRecords.add(indexToAdd, newRecord);
			if (indexToAdd % 40 == 0) removeRecords(indexToAdd, true, cachedRecords);

			pks.addRow(indexToAdd, newRecord.getPK());
		}


		fireFoundSetEvent(indexToAdd, indexToAdd, FoundSetEvent.CHANGE_INSERT);//always let know about new record

		if (changeSelection)
		{
			setSelectedIndex(indexToAdd);
		}

		// Can't start edit earlier (before selection model changes)
		// Else the selection model will call saveData on the just created
		// record again because the selection changes from the last to the new record
		if (newRecord.startEditing())
		{
			return indexToAdd;
		}
		//edit start can fail when in onRecordSelect for example a sort is performed cousing this record fall out of the cached records
		Debug.trace("New record failed because couldn't start editing for that record (record can already fall out of the foundset again by a save)"); //$NON-NLS-1$
		return -1;
	}

	private IRecordInternal createRecord() throws ApplicationException
	{
		if (findMode)
		{
			if (pksAndRecords.getCachedRecords().size() > fsm.pkChunkSize) return null;//limit to 200
			return new FindState(this);
		}

		if (!hasAccess(IRepository.INSERT))
		{
			throw new ApplicationException(ServoyException.NO_CREATE_ACCESS);
		}

		if (relationName != null)
		{
			Relation relation = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
			if (relation != null)
			{
				Placeholder ph = creationSqlSelect.getPlaceholder(SQLGenerator.createRelationKeyPlaceholderKey(creationSqlSelect.getTable(), relation.getName()));
				if (ph == null || !ph.isSet() || ph.getValue() == null || ((Object[])ph.getValue()).length == 0)
				{
					Debug.trace("New record failed because related foundset had no parameters, or trying to make a new findstate when it is nested more then 2 deep"); //$NON-NLS-1$
					return null;
				}
				if (!relation.getAllowCreationRelatedRecords())
				{
					throw new ApplicationException(ServoyException.NO_RELATED_CREATE_ACCESS, new Object[] { relation.getName() });
				}
			}
		}

		Object[] data = sheet.getNewRecordData(fsm.getApplication(), this);
		IRecordInternal newRecord = new Record(this, rowManager.createNotYetExistInDBRowObject(data, true));
		sheet.processCopyValues(newRecord);
		return newRecord;
	}

	public void processCopyValues(int row)
	{
		IRecordInternal state = getRecord(row);
		sheet.processCopyValues(state);
		fireFoundSetEvent(row, row, FoundSetEvent.CHANGE_UPDATE);
	}

	@Deprecated
	public int duplicateRecord(int row, boolean addOnTop) throws ServoyException
	{
		return duplicateRecord(row, (addOnTop ? 0 : Integer.MAX_VALUE), true);
	}

	public int duplicateRecord(int row, int indexToAdd) throws ServoyException
	{
		return duplicateRecord(row, indexToAdd, true);
	}

	int duplicateRecord(int row, int indexToAdd, boolean changeSelection) throws ServoyException
	{
		if (row >= 0 && fsm.getEditRecordList().prepareForSave(true) == ISaveConstants.STOPPED)
		{
			IRecordInternal state = getRecord(row);
			if (state != null)
			{
				if (findMode)
				{
					return addRecord(((FindState)state).duplicate(), indexToAdd, changeSelection);
				}

				Row currentRow = state.getRawData();
				Object[] data = sheet.getDuplicateRecordData(fsm.getApplication(), currentRow);
				Row newRow = rowManager.createNotYetExistInDBRowObject(data, true);
				return newRecord(newRow, indexToAdd, changeSelection);
			}
		}
		return -1;
	}

	public boolean isInFindMode()
	{
		return findMode;
	}

	public void setFindMode()
	{
		if (sheet.getTable() == null)
		{
			return;
		}
		fireSelectionAdjusting();

		int oldSize = getSize();
		pksAndRecords.setPks(new BufferedDataSet(), 0);//return to 0
		boolean oldFindMode = findMode;
		clearInternalState(false);
		findMode = true;
		if (oldFindMode == false) fireFindModeChange();

		//let the List know the model changed
		fireDifference(oldSize, 0);
//		selectionModel.setSelectedRow(-1);

		try
		{
			newRecord(false);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public int performFind(boolean clearLastResult, boolean reduceSearch, boolean clearIfZero) throws ServoyException//perform the find
	{
		return performFind(clearLastResult, reduceSearch, clearIfZero, null);
	}

	public int performFind(boolean clearLastResult, boolean reduceSearch, boolean clearIfZero, List<String> returnInvalidRangeConditions)
		throws ServoyException//perform the find
	{
		if (clearLastResult) removeLastFound();

		int numberOfFindStates = getSize();
		setSelectedIndex(numberOfFindStates > 0 ? 0 : -1);

		try
		{
			QuerySelect findSqlSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), pksAndRecords.getQuerySelectForReading(),
				pksAndRecords.getCachedRecords(), reduceSearch, omittedPKs, lastSortColumns, true);

			if (returnInvalidRangeConditions != null)
			{
				ISQLCondition sqlCondition = findSqlSelect.getCondition(SQLGenerator.CONDITION_SEARCH);
				returnInvalidRangeConditions.addAll(AbstractBaseQuery.getInvalidRangeConditions(sqlCondition));
			}

			//cache pks
			String transaction_id = fsm.getTransactionID(sheet);
			long time = System.currentTimeMillis();
			IDataSet findPKs = null;
			try
			{
				findPKs = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, findSqlSelect,
					fsm.getTableFilterParams(sheet.getServerName(), findSqlSelect), !findSqlSelect.isUnique(), 0, fsm.pkChunkSize,
					IDataServer.FIND_BROWSER_QUERY);
			}
			catch (RemoteException e)
			{
				throw new RepositoryException(e);
			}
			if (Debug.tracing())
			{
				Debug.trace("Find executed, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + ", sql: " + findSqlSelect.toString()); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
			}

			if (findPKs.getRowCount() == 0)
			{
				if (clearIfZero)
				{
					pksAndRecords.setPksAndQuery(null, 0, findSqlSelect);
					clearInternalState(true);
					setSelectedIndex(-1);
				}
			}
			else
			{
				fireSelectionAdjusting();
				pksAndRecords.setPksAndQuery(findPKs, findPKs.getRowCount(), findSqlSelect);
				initialized = true;

				clearInternalState(true);
				fireAggregateChangeWithEvents(null); //notify about aggregate change,because the find has cleared them all.
			}

			fireDifference(numberOfFindStates, getSize());
			return findPKs.getRowCount();
		}
		catch (ServoyException e)
		{
			pksAndRecords.setPks(null, 0);
			clearInternalState(true);
			setSelectedIndex(-1);
			fireDifference(numberOfFindStates, 0);

			throw e;
		}
	}

	/**
	 * Fire difference based on real size (not corrected for fires!)
	 * 
	 * @param oldSize
	 * @param newSize
	 */
	protected void fireDifference(int oldSize, int newSize)
	{
		if (newSize == 0 && oldSize == 0) return;

		//let the List know the model changed,the new states
		if (newSize < oldSize)
		{
			fireFoundSetEvent(newSize, oldSize - 1, FoundSetEvent.CHANGE_DELETE);
			if (newSize > 0) fireFoundSetEvent(0, newSize - 1, FoundSetEvent.CHANGE_UPDATE);
		}
		else
		{
			if (oldSize > 0)
			{
				fireFoundSetEvent(0, oldSize - 1, FoundSetEvent.CHANGE_UPDATE);
			}
			if (newSize > oldSize)
			{
				fireFoundSetEvent(oldSize, newSize - 1, FoundSetEvent.CHANGE_INSERT);
			}
		}
	}

	protected int getCorrectedSizeForFires()
	{
		return getSize() - 1;
	}

	//prevent always doing a search in search
	public void removeLastFound()
	{
		pksAndRecords.setPksAndQuery(pksAndRecords.getPks(), pksAndRecords.getDbIndexLastPk(), AbstractBaseQuery.deepClone(creationSqlSelect), true);
	}

	public void setSort(String sortString) throws ServoyException
	{
		sort(((FoundSetManager)getFoundSetManager()).getSortColumns(getTable(), sortString), false);
	}

	public String getSort()
	{
		return FoundSetManager.getSortColumnsAsString(lastSortColumns);
	}

	public List<SortColumn> getSortColumns()
	{
		return lastSortColumns;
	}

	public void sort(List<SortColumn> sortColumns) throws ServoyException
	{
		sort(sortColumns, false);
	}

	public void sort(List<SortColumn> sortColumns, boolean defer) throws ServoyException
	{
		if (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED)
		{
			fsm.getApplication().reportJSError("Couldn't do a sort because there where edited records on this foundset", null); //$NON-NLS-1$
			return;
		}
		lastSortColumns = sortColumns;

		if (findMode || sheet.getTable() == null) return;

		// The selection model must no fire a event (with adjusting) so that formpanels can execute: onRecordSave()
		if (!defer)
		{
			fireSelectionAdjusting();
		}

		QuerySelect sqlSelect;
		IDataSet pks;
		synchronized (pksAndRecords)
		{
			sqlSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), pksAndRecords.getQuerySelectForReading(), null, true, null,
				lastSortColumns, true);
			pks = pksAndRecords.getPks();
			// set the current select with the new sort in case a refreshFromDBInternal comes along or when defer is set
			pksAndRecords.setPksAndQuery(pks, pks == null ? 0 : pks.getRowCount(), sqlSelect, true);
		}

		if (defer)
		{
			if (pks != null) pks.clearHadMoreRows(); //make sure we don't do query for more pks with a new sort!
			return;
		}


		Object[] selectedPK = null;
		if (pks != null && getSelectedIndex() >= 0) selectedPK = pks.getRow(getSelectedIndex()); //always keep selection when sorting

		int oldSize = getSize();
		//cache pks
		String transaction_id = fsm.getTransactionID(sheet);
		try
		{
			pks = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, sqlSelect,
				fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), !sqlSelect.isUnique(), 0, fsm.pkChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
			synchronized (pksAndRecords)
			{
				// optimistic locking, if the query has been changed in the mean time forget about the refresh
				if (sqlSelect != pksAndRecords.getQuerySelectForReading())
				{
					Debug.log("sort: query was changed during refresh, not resetting old query"); //$NON-NLS-1$
					return;
				}
				pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), sqlSelect, true);
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}

		initialized = true;

		int newSize = getSize();
		fireDifference(oldSize, newSize);

		if (selectedPK == null || !selectRecord(selectedPK))
		{
			setSelectedIndex(newSize > 0 ? 0 : -1);
		}
	}

	private void sort(final Function recordComparator)
	{
		if (recordComparator != null)
		{
			final IExecutingEnviroment scriptEngine = fsm.getApplication().getScriptEngine();
			final Scriptable recordComparatorScope = recordComparator.getParentScope();
			sort(new Comparator<Object[]>()
			{
				public int compare(Object[] o1, Object[] o2)
				{
					try
					{
						Object compareResult = scriptEngine.executeFunction(recordComparator, recordComparatorScope, recordComparatorScope,
							new Object[] { getRecord(o1), getRecord(o2) }, false, true);
						return Utils.getAsInteger(compareResult, true);
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

	public void sort(Comparator<Object[]> recordPKComparator)
	{
		if (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED)
		{
			fsm.getApplication().reportJSError("Couldn't do a sort because there where edited records on this foundset", null); //$NON-NLS-1$
			return;
		}

		if (findMode) return;
		IDataSet pks = pksAndRecords.getPks();
		Object[] selectedPK = null;
		if (pks != null && getSelectedIndex() > 0) selectedPK = pks.getRow(getSelectedIndex()); //if first record is selected we ignore selection, is much faster
		int oldSize = getSize();

		PksAndRecordsHolder pksAndRecordsHolderCopy = pksAndRecords.shallowCopy();
		queryForMorePKs(pksAndRecordsHolderCopy, -1, false);
		synchronized (pksAndRecords)
		{
			pksAndRecordsHolderCopy.getPks().sort(recordPKComparator);
			pksAndRecords.setPks(pksAndRecordsHolderCopy.getPks(), pksAndRecordsHolderCopy.getDbIndexLastPk());
		}

		int newSize = getSize();
		fireDifference(oldSize, newSize);

		if (selectedPK == null || !selectRecord(selectedPK))
		{
			setSelectedIndex(newSize > 0 ? 0 : -1);
		}
	}

	public boolean isRecordEditable(int rowIndex)
	{
		if (findMode && hasAccess(IRepository.READ))
		{
			return true;//findmode is always editable, when having read permission
		}
		else if (hasAccess(IRepository.UPDATE))
		{
			IRecordInternal rec = getRecord(rowIndex);
			if (rec != null)
			{
				boolean locked = rec.isLocked();
				if (locked)
				{
					fsm.getApplication().reportWarning(RECORD_IS_LOCKED);
				}
				return !locked;
			}
			fsm.getApplication().reportWarning(NO_RECORD);
		}
		else if (hasAccess(IRepository.INSERT))
		{
			IRecordInternal rec = getRecord(rowIndex);
			if (rec != null)
			{
				return !rec.existInDataSource();
			}
			fsm.getApplication().reportWarning(NO_RECORD);
		}
		return false;
	}

	public IFoundSetManagerInternal getFoundSetManager()
	{
		return fsm;
	}

	protected void fireSelectionAdjusting()
	{
	}

	protected void fireFindModeChange()
	{
		if (foundSetEventListeners.size() > 0)
		{
			IFoundSetEventListener[] array;
			synchronized (foundSetEventListeners)
			{
				array = foundSetEventListeners.toArray(new IFoundSetEventListener[foundSetEventListeners.size()]);
			}

			FoundSetEvent e = new FoundSetEvent(this, FoundSetEvent.FIND_MODE_CHANGE, FoundSetEvent.CHANGE_UPDATE);
			for (IFoundSetEventListener element : array)
			{
				element.foundSetChanged(e);
			}
		}
	}

	protected void fireFoundSetEvent(@SuppressWarnings("unused") int firstRow, @SuppressWarnings("unused") int lastRow, int changeType)
	{
		if (foundSetEventListeners.size() > 0)
		{
			FoundSetEvent e = new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType);
			IFoundSetEventListener[] array;
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

	public void fireFoundSetChanged()
	{
		int size = getSize();
		fireDifference(size, size);
	}

	public abstract int getSelectedIndex();

	public abstract void setSelectedIndex(int i);

	public abstract void setSelectedIndexes(int[] indexes);

	public abstract int[] getSelectedIndexes();


	/**
	 * @see com.servoy.j2db.dataprocessing.IFireCollectable#completeFire(java.util.List)
	 */
	public void completeFire(List<Object> entries)
	{
		int start = Integer.MAX_VALUE;
		int end = -1;
		for (Object object : entries)
		{
			int index = getRecordIndex((IRecord)object);
			if (index != -1 && start > index)
			{
				start = index;
			}
			if (end < index)
			{
				end = index;
			}
		}
		if (start != Integer.MAX_VALUE && end != -1)
		{
			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE);
		}
	}

	private boolean isInNotify = false;

	public void notifyChange(RowEvent e) //this method is only called if I'm not the source of the event
	{
		if (!isInNotify) //prevent circle calling
		{
			try
			{
				isInNotify = true;

				SafeArrayList<IRecordInternal> cachedRecords;
				IDataSet pks;

				synchronized (pksAndRecords)
				{
					cachedRecords = pksAndRecords.getCachedRecords();
					pks = pksAndRecords.getPks();
				}

				// ROW CAN BE NULL
				Row r = e.getRow();

				if (e.getType() == RowEvent.INSERT)
				{
					Debug.trace("Row inserted notify"); //$NON-NLS-1$
					if (!pksAndRecords.getQuerySelectForReading().hasAnyCondition() && pks != null && !pks.hadMoreRows() && r != null &&
						fsm.getTableFilterParams(sheet.getServerName(), pksAndRecords.getQuerySelectForReading()) == null)//does show all records, if so show record .. if not we whould have to go to the database to verify if the record does match our SQL
					{
						Object[] pk = r.getPK();
						//check if im in new record
						int newRec = -1;
						int size;
						synchronized (pksAndRecords)
						{
							for (int i = 0; i < pks.getRowCount(); i++)
							{
								Object[] pksRow = pks.getRow(i);
								if (pk != null && pksRow != null && pk.length == pksRow.length)
								{
									boolean equal = true;
									for (int p = 0; equal && p < pk.length; p++)
									{
										Object pkval = pksRow[p];
										if (pkval instanceof DbIdentValue)
										{
											pkval = ((DbIdentValue)pkval).getPkValue();
											if (pkval != null)
											{
												// update ident value in pksAndRecords
												pksRow[p] = pkval;
											}
										}
										equal = Utils.equalObjects(pk[p], pkval);
									}
									if (equal)
									{
										newRec = i;
										break;
									}
								}
							}
							if (newRec != -1 && cachedRecords != null)
							{
								IRecordInternal rec = cachedRecords.get(newRec);
								if (rec != null && rec.getRawData() == r)
								{
									return;//do nothing is my own row via self join used in lookup 
								}
							}

							pks.addRow(pk);
							size = getCorrectedSizeForFires();

						}
						clearAggregates();
						fireFoundSetEvent(size, size, FoundSetEvent.CHANGE_INSERT);
					}
					else
					{
						mustQueryForUpdates = true;
						clearAggregates();
					}
				}
				else
				{
					if (pks != null && r != null)
					{
						String pkHash = r.getPKHashKey();
						for (int i = pks.getRowCount() - 1; i >= 0; i--)
						{
							Object[] pk = pks.getRow(i);
							if (RowManager.createPKHashKey(pk).equals(pkHash))
							{
								if (e.getType() == RowEvent.UPDATE)
								{
									clearAggregates();
									fireFoundSetEvent(i, i, FoundSetEvent.CHANGE_UPDATE);
								}
								else if (e.getType() == RowEvent.DELETE)
								{
									removeRecordInternal(i);//does fireIntervalRemoved(this,i,i);
								}
								break;
							}
						}
					}
					else if (r == null && getSize() > 0)
					{
						clearAggregates();
						fireFoundSetEvent(0, getSize() - 1, FoundSetEvent.CHANGE_UPDATE);
					}
				}
			}
			finally
			{
				isInNotify = false;
			}
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getTable()
	 */
	public ITable getTable()
	{
		return sheet.getTable();
	}

	protected void updatePk(Record state)
	{
		synchronized (pksAndRecords)
		{
			SafeArrayList<IRecordInternal> cachedRecords = pksAndRecords.getCachedRecords();
			IDataSet pks = pksAndRecords.getPks();
			int index = cachedRecords.indexOf(state);
			if (index != -1) //deleted without an update being done
			{
				Object[] pk = state.getRawData().getPK();
				if (pk == null) pk = new Object[] { "invalid" };//prevent crashing pks (must stay in sync)  //$NON-NLS-1$
				pks.setRow(index, pk);
			}
		}
	}

	public void fireAggregateChangeWithEvents(IRecordInternal record)
	{
		fireAggregateChange(record);
		getFoundSetManager().getEditRecordList().fireEvents();
	}

	public void fireAggregateChange(IRecordInternal record)
	{
		//clear aggregates before getting the record (if related foundset is in getNewPKS()
		clearAggregates();
		List<Record> records = new ArrayList<Record>(1);
		if (record == null)
		{
			// if record is null and mustQueryForUpdates is true
			// then make sure that mustQuery is false for a little while so that getRecord/getSize
			// in this call will not do the query to the database. because that should be done after 
			// fireAggregate method  
			boolean tmp = mustQueryForUpdates;
			mustQueryForUpdates = false;
			try
			{
				IRecordInternal r = getRecord(getSelectedIndex());
				if (r instanceof Record)
				{
					records.add((Record)r);
				}
			}
			finally
			{
				mustQueryForUpdates = tmp;
			}
		}
		else if (record instanceof Record)
		{
			records.add((Record)record);
		}
		recordsUpdated(records);
	}

	protected void clearAggregates()
	{
		if (aggregateCache.size() > 0)
		{
			aggregateCache.clear();
			fireAggregateModificationEvent(null, null);
		}
	}

	protected void recordsUpdated(List<Record> records, List<String> aggregatesToRemove)
	{
		if (aggregatesToRemove.size() > 0)
		{
			for (int i = 0; i < aggregatesToRemove.size(); i++)
			{
				String aggregate = aggregatesToRemove.get(i);
				aggregateCache.remove(aggregate);
				fireAggregateModificationEvent(aggregate, null);
			}
		}
		recordsUpdated(records);
	}

	private void recordsUpdated(List<Record> records)
	{
		Map<FoundSet, int[]> parentToIndexen = getFoundSetManager().getEditRecordList().getFoundsetEventMap();
		// first go to the parents
		walkParents(parentToIndexen);

		int[] indexen = parentToIndexen.get(this);

		for (int i = 0; i < records.size(); i++)
		{
			Record record = records.get(i);

			int recordIndex = pksAndRecords.getCachedRecords().indexOf(record);
			if (recordIndex != -1)
			{
				if (indexen == null)
				{
					indexen = new int[] { recordIndex, recordIndex };
					parentToIndexen.put(this, indexen);
				}
				else
				{
					indexen[0] = Math.min(indexen[0], recordIndex);
					indexen[1] = Math.max(indexen[1], recordIndex);
				}
			}
			else
			{
				if (Debug.tracing())
				{
					Debug.trace("record index -1 for the record " + record + " already out of the foundset: " + this); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	protected void fireAggregateModificationEvent(String name, Object value)
	{
		if (aggregateModificationListeners.size() > 0)
		{
			ModificationEvent e = new ModificationEvent(name, value, this);
			IModificationListener[] array;
			synchronized (aggregateModificationListeners)
			{
				array = aggregateModificationListeners.toArray(new IModificationListener[aggregateModificationListeners.size()]);
			}

			for (IModificationListener element : array)
			{
				element.valueChanged(e);
			}
		}
	}


	/**
	 * 
	 */
	private void walkParents(Map<FoundSet, int[]> parentsToIndexen)
	{
		List<IRecordInternal> parents = getParents();
		for (int i = parents.size(); --i >= 0;)
		{
			IRecordInternal parent = parents.get(i);
			Object parentFS = parent.getParentFoundSet();
			if (parentFS instanceof FoundSet)
			{
				FoundSet fs = (FoundSet)parentFS;
				int recordIndex = fs.pksAndRecords.getCachedRecords().indexOf(parent);
				if (recordIndex == -1) continue;

				int[] parentIndex = parentsToIndexen.get(fs);
				if (parentIndex == null)
				{
					parentIndex = new int[] { recordIndex, recordIndex };
					parentsToIndexen.put(fs, parentIndex);
					// First time for this foundset get its parents.
					fs.clearAggregates();
					fs.walkParents(parentsToIndexen);
				}
				else
				{
					parentIndex[0] = Math.min(parentIndex[0], recordIndex);
					parentIndex[1] = Math.max(parentIndex[1], recordIndex);
				}
			}
		}
	}

	public String getClassName()
	{
		return "FoundSet"; //$NON-NLS-1$
	}

	public static boolean isToplevelKeyword(String name)
	{
		for (String element : IExecutingEnviroment.TOPLEVEL_KEYWORDS)
		{
			if (element.equals(name))
			{
				return true;
			}
		}
		return false;
	}

	public Object get(String name, Scriptable start)
	{
		if (isToplevelKeyword(name)) return Scriptable.NOT_FOUND;
		if (name.equals("multiSelect")) //$NON-NLS-1$
		{
			return new Boolean(isMultiSelect());
		}
		if ("alldataproviders".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();
			Table table = (Table)getTable();
			if (table != null)
			{
				try
				{
					Iterator<Column> columnsIt = table.getColumnsSortedByName();
					while (columnsIt.hasNext())
					{
						Column c = columnsIt.next();
						al.add(c.getDataProviderID());
					}
					Iterator<AggregateVariable> aggIt = fsm.getApplication().getFlattenedSolution().getAggregateVariables(table, true);
					while (aggIt.hasNext())
					{
						AggregateVariable av = aggIt.next();
						al.add(av.getDataProviderID());
					}
					Iterator<ScriptCalculation> scriptIt = fsm.getApplication().getFlattenedSolution().getScriptCalculations(table, true);
					while (scriptIt.hasNext())
					{
						ScriptCalculation sc = scriptIt.next();
						al.add(sc.getDataProviderID());
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		Object mobj = jsFunctions.get(name);
		if (mobj != null) return mobj;

		if (getSize() == 0)
		{
			try
			{
				if (isValidRelation(name))
				{
					return getPrototypeState().getValue(name);
				}
				if (containsDataProvider(name))
				{
					return getDataProviderValue(name);
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else if (containsDataProvider(name))
		{
			return getDataProviderValue(name);
		}
		else if (has(name, start))
		{
			int row = getSelectedIndex();
			// test if in printing where the selected record is set to -1.
			// but if a value is being get it should always try to get a value.
			if (row == -1 && getSize() > 0) row = 0;
			if (row != -1)
			{
				IRecordInternal state = getRecord(row);
				if (state instanceof Scriptable)
				{
					return ((Scriptable)state).get(name, start);
				}
			}
		}
		if (name.startsWith("record_")) //$NON-NLS-1$
		{
			int recordIndex = Integer.parseInt(name.substring("record_".length())); //$NON-NLS-1$
			IRecordInternal record = pksAndRecords.getCachedRecords().get(recordIndex - 1);
			if (record == null) return null; //return "<record " + recordIndex + " not loaded>"; 
			return record;
		}
		return Scriptable.NOT_FOUND;
	}

	public Object get(int index, Scriptable start)
	{
		return Scriptable.NOT_FOUND;
	}

	public boolean has(String name, Scriptable start)
	{
		if (name == null) return false;

		if ("foundset".equals(name) || jsFunctions.containsKey(name)) return true; //$NON-NLS-1$ 

		if (name.equals("multiSelect")) return true;//$NON-NLS-1$

		if (isToplevelKeyword(name)) return false;

		if (containsDataProvider(name)) return true;
		if (isValidRelation(name)) return true;

		int row = getSelectedIndex();
		// test if in printing where the selected record is set to -1.
		// but if a value is being get it should always try to get a value.
		if (row == -1 && getSize() > 0) row = 0;

		if (row != -1)
		{
			IRecordInternal state = getRecord(row);
			if (state instanceof Scriptable)
			{
				return ((Scriptable)state).has(name, start);
			}
		}
		return false;
	}

	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	public void put(String name, Scriptable start, Object value)
	{
		if (name == null) return;

		if (name.equals("multiSelect") && value instanceof Boolean) //$NON-NLS-1$ 
		{
			setMultiSelect(((Boolean)value).booleanValue());
			return;
		}

		if (jsFunctions.containsKey(name)) return;//dont allow to set  

		if (name.length() > ScriptVariable.GLOBAL_DOT_PREFIX.length() && name.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String global = name.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
			Scriptable sol_scope = fsm.getScriptEngine().getSolutionScope().getGlobalScope();
			sol_scope.put(global, start, value);
		}
		else
		{
			int row = getSelectedIndex();
			if (row != -1)
			{
				IRecordInternal state = getRecord(row);
				if (state instanceof Scriptable)
				{
					((Scriptable)state).put(name, start, value);
				}
			}
		}
	}

	boolean mustAggregatesBeLoaded()
	{
		Map<String, QuerySelect> aggregate = sheet.getAggregates();
		if (aggregate == null || aggregate.size() == 0) return false;
		return (aggregateCache.size() == 0 && getSize() > 0);
	}

	public void put(int index, Scriptable start, Object value)
	{
		// ignore
	}

	public void delete(String name)
	{
		// ignore
	}

	public void delete(int index)
	{
		// ignore
	}

	private Scriptable prototypeScope;

	public Scriptable getPrototype()
	{
		return prototypeScope;
	}

	public void setPrototype(Scriptable prototype)
	{
		prototypeScope = prototype;
	}

	private Scriptable parentScope;

	public Scriptable getParentScope()
	{
		if (parentScope == null)
		{
			return fsm.getApplication().getScriptEngine().getSolutionScope();
		}
		return parentScope;
	}

	public void setParentScope(Scriptable parent)
	{
		parentScope = parent;
	}

	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<String>();
		al.add("alldataproviders"); //$NON-NLS-1$

		al.add("multiSelect"); //$NON-NLS-1$

//		al.add("recordIndex"); //$NON-NLS-1$
//		al.add("selectedIndex"); //$NON-NLS-1$
//		al.add("maxRecordIndex"); //$NON-NLS-1$

		Map<String, QuerySelect> aggregates = sheet.getAggregates();
		if (aggregates != null)
		{
			for (String aggregate : aggregates.keySet())
			{
				al.add(aggregate);
			}
		}

		int maxRows = getSize() - 1;
		String format = maxRows < 10 ? "0" : maxRows < 100 ? "00" : "000"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		DecimalFormat df = new DecimalFormat(format);

		for (int i = 0; i < maxRows; i++)
		{
			al.add("record_" + df.format(i + 1)); //$NON-NLS-1$
		}
		return al.toArray();
	}

	public Object getDefaultValue(Class hint)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Math.min(10, getSize()); i++)
		{
			IRecordInternal record = getRecord(i);
			sb.append(record.getPKHashKey());
			sb.append("\n"); //$NON-NLS-1$
		}
		if (getSize() > 10)
		{
			sb.append("..."); //$NON-NLS-1$
		}
		return sb.toString();
	}

	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

//	public Object unwrap()
//	{
//		return this;
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		int counter = 0;
		SafeArrayList<IRecordInternal> cachedRecords = pksAndRecords.getCachedRecords();
		for (int i = 0; i < cachedRecords.size(); i++)
		{
			if (cachedRecords.get(i) != null)
			{
				counter++;
			}

		}
		StringBuilder sb = new StringBuilder();
		sb.append("FoundSet[Table:"); //$NON-NLS-1$
		sb.append(getTable() == null ? "<none>" : getTable().getName()); //$NON-NLS-1$
		sb.append(",Size: "); //$NON-NLS-1$
		sb.append(getSize());
		if (relationName != null)
		{
			sb.append(",Relation: "); //$NON-NLS-1$
			sb.append(relationName);
		}
		if (foundSetFilters != null && foundSetFilters.size() > 0)
		{
			sb.append(",Filters: { "); //$NON-NLS-1$
			for (TableFilter tf : foundSetFilters)
			{
				sb.append(tf.toString());
				sb.append(' ');
			}
			sb.append('}');
		}
		sb.append(",CachedRecords: "); //$NON-NLS-1$
		sb.append(counter);
		sb.append(",SELECTED INDEX: "); //$NON-NLS-1$
		sb.append(getSelectedIndex());
//		int selected = getSelectedIndex();
//		IRecord record = null;
//		if (selected >= 0)
//		{
//			//record = getRecord(selected);
//		}
//		sb.append(record);
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
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
			SQLSheet relatedSheet = sheet.getRelatedSheet(fsm.getApplication().getFlattenedSolution().getRelation(parts[i]), fsm.getSQLGenerator());
			if (relatedSheet == null)
			{
				retval = fsm.getGlobalRelatedFoundSet(parts[i]);
			}
			else
			{
				retval = fsm.getRelatedFoundSet(currentRecord, relatedSheet, parts[i], defaultSortColumns);
				if (retval != null)
				{
					if (retval.getSize() == 0 && !currentRecord.existInDataSource())
					{
						Relation r = fsm.getApplication().getFlattenedSolution().getRelation(parts[i]);
						if (r != null && r.isExactPKRef(fsm.getApplication().getFlattenedSolution()))//TODO add unique column test instead of pk requirement 
						{
							((FoundSet)retval).newRecord(record.getRawData(), 0, true);
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

	@Override
	protected Object clone() throws CloneNotSupportedException
	{
		FoundSet obj = (FoundSet)super.clone();
		obj.pksAndRecords = new PksAndRecordsHolder(fsm.chunkSize);
		synchronized (pksAndRecords)
		{
			obj.pksAndRecords.setPksAndQuery(new BufferedDataSet(pksAndRecords.getPks()), pksAndRecords.getDbIndexLastPk(),
				pksAndRecords.getQuerySelectForModification());
		}
		obj.findMode = false;
		obj.creationSqlSelect = AbstractBaseQuery.deepClone(creationSqlSelect);
		if (foundSetFilters != null)
		{
			obj.foundSetFilters = new ArrayList<TableFilter>(foundSetFilters);
		}
		obj.foundSetEventListeners = new ArrayList<IFoundSetEventListener>();
		obj.aggregateModificationListeners = new ArrayList<IModificationListener>();
		return obj;
	}

	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException//used for printing current record
	{
		try
		{
			FoundSet fs = (FoundSet)clone();
			QuerySelect fs_sqlSelect = fs.pksAndRecords.getQuerySelectForReading(); // no need for clone, just made one
			SQLSheet.SQLDescription select_desc = sheet.getSQLDescription(SQLSheet.SELECT);
			if (select_desc != null)
			{
				QuerySelect select = (QuerySelect)select_desc.getSQLQuery();
				fs_sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, select.getConditionClone(SQLGenerator.CONDITION_SEARCH));
				// Leave CONDITION_RELATION and CONDITION_FILTER as is in fs (when it is a related fs)
				fs_sqlSelect.clearJoins();
				fs_sqlSelect.clearSorts();
				fs_sqlSelect.clearCondition(SQLGenerator.CONDITION_OMIT);//clear, 1 row needs no sort etc.

				// make sure the references to the tables from the copies are correct
				fs_sqlSelect.relinkTable(select.getTable(), fs_sqlSelect.getTable());

				fs.creationSqlSelect = AbstractBaseQuery.deepClone(fs_sqlSelect);//reset the creation! because we just changed the sqlSelect
				fs.lastSortColumns = null;
				if (fs.rowManager != null) fs.rowManager.register(fs);
				fs.aggregateCache = new HashMap<String, Object>(6);
				fs.pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, fs_sqlSelect);
			}

			int selRow = getSelectedIndex();
			if (selRow >= 0)
			{
				IRecordInternal selRec = getRecord(selRow);
				if (selRec != null)
				{
					Row row = selRec.getRawData();
					if (row != null && row.existInDB())
					{
						Object[] pk = row.getPK();
						if (!fs_sqlSelect.setPlaceholderValue(new PlaceholderKey(fs_sqlSelect.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY), pk))
						{
							Debug.error(new RuntimeException(
								"Could not set placeholder " + new PlaceholderKey(fs_sqlSelect.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY) + " in query " + fs_sqlSelect + "-- continuing")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						}
						fs.pksAndRecords.getPks().setRow(0, pk);
						fs.setSelectedIndex(0);
					}
					else
					{
						fs.newRecord(row, 0, true);
					}
				}
			}
			return fs;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public IFoundSetInternal copy(boolean unrelate) throws ServoyException
	{
		if (findMode) throw new RuntimeException("Foundsets in findmode can't be duplicated"); //$NON-NLS-1$

		if (sheet.getTable() == null) return this;

		FoundSet fs = (FoundSet)fsm.getNewFoundSet(sheet.getTable(), lastSortColumns);
		fs.creationSqlSelect = AbstractBaseQuery.deepClone(creationSqlSelect);
		if (foundSetFilters != null)
		{
			fs.foundSetFilters = new ArrayList<TableFilter>(foundSetFilters);
		}
		synchronized (pksAndRecords)
		{
			QuerySelect fs_sqlSelect = pksAndRecords.getQuerySelectForModification();
			if (unrelate)
			{
				// clear the relation condition, new finds should also retrieve records outside the relation
				ISQLCondition relationCondition = fs_sqlSelect.getCondition(SQLGenerator.CONDITION_RELATION);
				fs_sqlSelect.clearCondition(SQLGenerator.CONDITION_RELATION);
				fs.creationSqlSelect.clearCondition(SQLGenerator.CONDITION_RELATION);
				if (relationCondition != null)
				{
					// store old relation condition as search for extend/reduce-search
					fs_sqlSelect.addCondition(SQLGenerator.CONDITION_SEARCH, relationCondition);
				}
			}
			else
			{
				fs.sheet = sheet;
				fs.relationName = relationName;
			}
			fs.pksAndRecords.setPksAndQuery(new BufferedDataSet(pksAndRecords.getPks()), pksAndRecords.getDbIndexLastPk(), fs_sqlSelect);
		}
		fs.initialized = initialized;

		SafeArrayList<IRecordInternal> cachedRecords = pksAndRecords.getCachedRecords();
		SafeArrayList<IRecordInternal> fsCachedRecords = fs.pksAndRecords.getCachedRecords();
		for (int i = 0; i < cachedRecords.size(); i++)
		{
			IRecordInternal record = cachedRecords.get(i);
			if (record != null && !record.existInDataSource())
			{
				fsCachedRecords.set(i, new Record(fs, record.getRawData()));
			}
		}

		fs.setSelectedIndex(getSelectedIndex());

		return fs;
	}

	public boolean copyFrom(FoundSet fs)
	{
		if (fs == null || fs.getTable() == null) return false;
		if (!fs.getTable().equals(getTable())) return false;
		if (relationName != null) return false;

		int oldNumberOfRows = getSize();
		fireSelectionAdjusting();

		omittedPKs = null;
		clearInternalState(true);

		creationSqlSelect = AbstractBaseQuery.deepClone(fs.creationSqlSelect);
		if (fs.foundSetFilters != null)
		{
			foundSetFilters = new ArrayList<TableFilter>(fs.foundSetFilters);
		}
		sheet = fs.sheet;
		pksAndRecords.setPksAndQuery(new BufferedDataSet(fs.pksAndRecords.getPks()), fs.pksAndRecords.getDbIndexLastPk(),
			fs.pksAndRecords.getQuerySelectForModification());
		initialized = fs.initialized;

		SafeArrayList<IRecordInternal> fsCachedRecords = fs.pksAndRecords.getCachedRecords();
		synchronized (fsCachedRecords)
		{
			SafeArrayList<IRecordInternal> cachedRecords = pksAndRecords.getCachedRecords();
			for (int i = 0; i < fsCachedRecords.size(); i++)
			{
				IRecordInternal record = fsCachedRecords.get(i);
				if (record != null && !record.existInDataSource())
				{
					cachedRecords.set(i, new Record(this, record.getRawData()));
				}
			}
		}
		lastSortColumns = ((FoundSetManager)getFoundSetManager()).getSortColumns(getTable(), fs.getSort());
		fireDifference(oldNumberOfRows, getSize());

		setSelectedIndex(fs.getSelectedIndex());
		return true;
	}

	public void setSQLSelect(QuerySelect select) throws Exception
	{
		refreshFromDBInternal(select, false, false, fsm.pkChunkSize, false);
	}

	public boolean addFilterParam(String filterName, String dataprovider, String operator, Object value)
	{
		EditRecordList editRecordList = fsm.getEditRecordList();
		if (editRecordList.stopIfEditing(this) != ISaveConstants.STOPPED)
		{
			Debug.log("Couldn't add foundset filter param because foundset had edited records"); //$NON-NLS-1$
			return false;
		}

		TableFilter filter = FoundSetManager.createTableFilter(filterName, sheet.getTable(), dataprovider, operator, value);
		if (filter == null)
		{
			return false;
		}

		// create condition to check filter
		ISQLCondition cond = SQLGenerator.createTableFilterCondition(creationSqlSelect.getTable(), sheet.getTable(), filter);
		if (cond == null)
		{
			return false;
		}

		if (foundSetFilters == null)
		{
			foundSetFilters = new ArrayList<TableFilter>();
		}
		foundSetFilters.add(filter);

		resetFilterCondition();
		initialized = false;//to enforce browse all
		return true;
	}

	public boolean removeFilterParam(String filterName)
	{
		EditRecordList editRecordList = fsm.getEditRecordList();
		if (editRecordList.stopIfEditing(this) != ISaveConstants.STOPPED)
		{
			Debug.log("Couldn't remove foundset filter param because foundset had edited records"); //$NON-NLS-1$
			return false;
		}

		boolean found = false;
		if (foundSetFilters != null && filterName != null)
		{
			Iterator<TableFilter> filters = foundSetFilters.iterator();
			while (filters.hasNext())
			{
				TableFilter filter = filters.next();
				if (filterName.equals(filter.getName()))
				{
					filters.remove();
					found = true;
				}
			}
		}

		if (found)
		{
			resetFilterCondition();
			initialized = false;//to enforce browse all
		}
		return found;
	}

	private void resetFilterCondition()
	{
		synchronized (pksAndRecords)
		{
			creationSqlSelect.clearCondition(SQLGenerator.CONDITION_FILTER);
			if (foundSetFilters != null)
			{
				for (TableFilter tf : foundSetFilters)
				{
					creationSqlSelect.addCondition(SQLGenerator.CONDITION_FILTER,
						SQLGenerator.createTableFilterCondition(creationSqlSelect.getTable(), sheet.getTable(), tf));
				}
			}
		}

	}

	public boolean hadMoreRows()
	{
		IDataSet pks = pksAndRecords.getPks();
		if (pks != null)
		{
			return pks.hadMoreRows();
		}
		return false;
	}

	public IDataSet getCurrentPKs()
	{
		return pksAndRecords.getPks();
	}

	@Deprecated
	public void makeEmpty()
	{
		clear();
	}

	//used if left hand side null in relation
	public void clear()
	{
		clearInternalState(true);
		omittedPKs = null;
		QuerySelect sqlSelect = AbstractBaseQuery.deepClone(creationSqlSelect);
		sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, BooleanCondition.FALSE_CONDITION);
		sqlSelect.clearCondition(SQLGenerator.CONDITION_RELATION);
		pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, sqlSelect);

		if (rowManager != null) rowManager.clearAndCheckCache();
//		if (rowManager.getRowCount() > 5000) 
//		{
//			//if explicitly cleared, and significant size is presnt, hint for flush
//			rowManager.flushAllCachedRows();
//		}
		initialized = true;
	}

	public List<IRecordInternal> getParents()
	{
		List<IRecordInternal> al = new ArrayList<IRecordInternal>(allParents.size());
		synchronized (allParents)
		{
			for (int i = allParents.size(); --i >= 0;)
			{
				WeakReference<IRecordInternal> wr = allParents.get(i);
				IRecordInternal rcrd = wr.get();
				if (rcrd != null)
				{
					al.add(rcrd);
				}
				else
				{
					allParents.remove(i);
				}
			}
		}
		return al;
	}

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


	public IRecordInternal getRecord(Object[] pk)
	{
		// if this foundset is now in find mode there is now record anymore.
		if (!isInFindMode())
		{
			int index = getRecordIndex(pk, 0);
			if (index != -1)
			{
				return getRecord(index);
			}
		}
		return null;
	}

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

	public void removeFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			foundSetEventListeners.remove(l);
		}
	}

	public void addAggregateModificationListener(IModificationListener l)
	{
		synchronized (aggregateModificationListeners)
		{
			if (!aggregateModificationListeners.contains(l))
			{
				aggregateModificationListeners.add(l);
			}
		}
	}

	public void removeAggregateModificationListener(IModificationListener l)
	{
		synchronized (aggregateModificationListeners)
		{
			aggregateModificationListeners.remove(l);
		}
	}

	public String[] getDataProviderNames(int type)
	{
		switch (type)
		{
			case IFoundSet.AGGREGATEVARIABLES :
				return getSQLSheet().getAggregateNames();

			case IFoundSet.SCRIPTCALCULATIONS :
				return getSQLSheet().getCalculationNames();

			case IFoundSet.COLUMNS :
				return getSQLSheet().getColumnNames();

			default :
		}
		return null;
	}

	public String getDataSource()
	{
		return fsm.getDataSource(this.getTable());
	}

	boolean mustQueryForUpdates()
	{
		return mustQueryForUpdates;
	}
}