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


import static com.servoy.j2db.dataprocessing.IDataServer.RAW_QUERY;
import static com.servoy.j2db.dataprocessing.RowManager.createPKHashKey;
import static com.servoy.j2db.dataprocessing.SQLGenerator.isDistinctAllowed;
import static com.servoy.j2db.persistence.ColumnInfo.DATABASE_IDENTITY;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONAFTERCREATEMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONAFTERFINDMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONAFTERSEARCHMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONCREATEMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONFINDMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID;
import static com.servoy.j2db.persistence.StaticContentSpecLoader.PROPERTY_ONSEARCHMETHODID;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.query.ColumnType.getColumnTypes;
import static com.servoy.j2db.util.Debug.isDebugEnabled;
import static com.servoy.j2db.util.Utils.equalObjects;
import static com.servoy.j2db.util.Utils.iterate;
import static com.servoy.j2db.util.Utils.stream;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.SymbolScriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.FoundSetManager.GlobalFoundSetEventListener;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.CustomCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryCustomJoin;
import com.servoy.j2db.query.QueryCustomSelect;
import com.servoy.j2db.query.QueryCustomSort;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryFilter;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.LazyCompilationScope;
import com.servoy.j2db.scripting.TableScope;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * The foundset of a form, also handles the locking with the AppServer based on tablepks, and is the formmodel itself!
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSFoundSet", scriptingName = "JSFoundSet")
public abstract class FoundSet implements IFoundSetInternal, IFoundSetScriptMethods, IRowListener, Scriptable, SymbolScriptable, Cloneable, IJSFoundSet
{
	public static final String JS_FOUNDSET = "JSFoundSet"; //$NON-NLS-1$

	public static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(FoundSet.class);

	protected final FoundSetManager fsm;
	protected final RowManager rowManager;
	protected boolean findMode = false;
	private List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<IFoundSetEventListener>();
	private List<IModificationListener> aggregateModificationListeners = new ArrayList<IModificationListener>();

	private String serializedQuery;

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

	protected boolean disposed = false;

	private final List<WeakReference<IRecordInternal>> allParents = new ArrayList<WeakReference<IRecordInternal>>(6);

	private PrototypeState proto = null;

	protected boolean mustQueryForUpdates;

	private MethodCallState nextChunkLoadState = MethodCallState.READY;

	// forms might force their foundset to remain at a certain multiselect value
	// if a form 'pinned' multiselect, multiSelect should not be changeable by foundset JS access
	// if more then 1 form wishes to pin multiselect at a time, the form with lowest elementid wins
	private String multiSelectPinnedForm = null;
	private int multiSelectPinLevel;

	private int foundsetID = 0;

	private static Callable symbol_iterator = (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
		return new IterableES6Iterator(scope, ((Iterable)thisObj));
	};

	public PrototypeState getPrototypeState()
	{
		if (proto == null)
		{
			proto = new PrototypeState(this);
		}
		return proto;
	}

	//must be used by subclasses
	protected FoundSet(IFoundSetManagerInternal app, String relation_name, SQLSheet sheet, QuerySelect pkSelect,
		List<SortColumn> defaultSortColumns) throws ServoyException
	{
		fsm = (FoundSetManager)app;
		if (sheet == null)
		{
			throw new IllegalArgumentException(app.getApplication().getI18NMessage("servoy.foundSet.error.sqlsheet")); //$NON-NLS-1$
		}
		pksAndRecords = new PksAndRecordsHolder(this, fsm.config.chunkSize(), fsm.config.optimizedChangeFires());
		relationName = relation_name;
		this.sheet = sheet;

		rowManager = fsm.getRowManager(fsm.getDataSource(sheet.getTable()));
		// null default sort columns means: use sort columns from query
		defaultSort = defaultSortColumns;
		lastSortColumns = defaultSort;

		if (sheet.getTable() != null && pkSelect == null)
		{
			creationSqlSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), null, null, true, null, lastSortColumns, false);
		}
		else
		{
			creationSqlSelect = deepClone(pkSelect);
		}

		pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, deepClone(creationSqlSelect));
		aggregateCache = new HashMap<String, Object>(6);
		findMode = false;
	}

	@Override
	public void configure(IRecordInternal parent)
	{
		if (rowManager != null && !(parent instanceof FindState)) rowManager.register(this);
	}

	public String getRelationName()
	{
		return relationName;
	}

	public SQLSheet getSQLSheet()
	{
		return sheet;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#containsAggregate(java.lang.String)
	 */
	@Override
	public boolean containsAggregate(String name)
	{
		return sheet.containsAggregate(name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#containsCalculation(java.lang.String)
	 */
	@Override
	public boolean containsCalculation(String dataProviderID)
	{
		return sheet.containsCalculation(dataProviderID);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getColumnIndex(java.lang.String)
	 */
	@Override
	public int getColumnIndex(String dataProviderID)
	{
		return sheet.getColumnIndex(dataProviderID);
	}

	/**
	 * Gets the unique id of the foundset. The foundset does not have an id until this method is called the first time on it.
	 */
	@Override
	public int getID()
	{
		// we do not automatically assign an id to each foundset so that ng client client side code cannot send in random ints to target any foundset;
		// in this way, only foundsets that were sent to client already (getID() was called on them in various property types to send it to client) can be targeted
		if (foundsetID == 0) foundsetID = fsm.getNextFoundSetID();
		return foundsetID;
	}


	/**
	 * Same as {@link #getID()} but it will not assign an id if it wasn't set before. If it does not have an ID it will return 0.
	 */
	public int getIDInternal()
	{
		return foundsetID;
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
		clearOmit(null);
		browseAllInternal();
	}

	/**
	 * Check for a condition, either in the query or in the filters
	 * @return
	 */
	protected boolean currentQueryHasAnyCondition()
	{
		QuerySelect query = pksAndRecords.getQuerySelectForReading();
		return query.hasAnyCondition() || (sheet != null && fsm.getTableFilterParams(sheet.getServerName(), query) != null);
	}

	public void browseAllInternal() throws ServoyException
	{
		if (sheet == null || sheet.getTable() == null) return;

		// do get the sql select with the omitted pks, else a find that didn't get anything will not
		// just display the records without the omitted pks (when clear omit is false)
		refreshFromDBInternal(
			fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), creationSqlSelect, null, true, omittedPKs, lastSortColumns, true), false,
			fsm.config.pkChunkSize(), false, false);
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
			refreshFromDB(false, false);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
	}

	/**
	 * browse all part which can be used by subclasses this also acts as refresh and performs the pk query (again) can be called on any thread
	 *
	 * @param dropSort
	 * @param skipStopEdit
	 */
	void refreshFromDB(boolean dropSort, boolean skipStopEdit) throws ServoyException
	{
		refreshFromDBInternal(null, dropSort, fsm.config.pkChunkSize(), false, skipStopEdit);
	}

	/**
	 * browse all part which can be used by subclasses this also acts as refresh and performs the pk query (again) can be called on any thread
	 *
	 * @param querySelect will not be modified, null for the current active query
	 * @param flushRelatedFS
	 * @param skipStopEdit
	 */
	protected void refreshFromDBInternal(QuerySelect sqlSelect, boolean dropSort, int rowsToRetrieveHint, boolean keepPkOrder, boolean skipStopEdit)
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

		Object[][] selectedPKs = getSelectedPksForKeepingSelection(pks, true);

		Map<Integer, IRecordInternal> newRecords = new HashMap<>();
		EditRecordList editRecordList = getFoundSetManager().getEditRecordList();
		IRecordInternal[] array = editRecordList.getEditedRecords(this);
		for (IRecordInternal editingRecord : array)
		{
			if (!editingRecord.existInDataSource())
			{
				synchronized (pksAndRecords)
				{
					int newRecordIndex = cachedRecords.indexOf(editingRecord);
					if (newRecordIndex == -1) newRecordIndex = 0;// in case some has called startEdit before new/duplicateRecords was completed.
					newRecords.put(Integer.valueOf(newRecordIndex), editingRecord);
					cachedRecords.set(newRecordIndex, null);
				}
			}
			else
			{
				// TODO check.. call stop edit? Records will be only referenced in the foundset manager:
				if (!skipStopEdit) editingRecord.stopEditing();
			}
		}
		int oldSize = getSize();
		if (oldSize > 1)
		{
			fireSelectionAdjusting();
		}

		IDataSet oldPKs = pks;

		int rowsToRetrieve = calculateRowsToRetrieve(rowsToRetrieveHint, selectedPKs);

		IFoundSetChanges changes = null;
		String transaction_id = fsm.getTransactionID(sheet);
		long time = System.currentTimeMillis();
		try
		{
			QuerySelect theQuery = (sqlSelect == null) ? pksAndRecords.getQuerySelectForReading() : sqlSelect;
			if (theQuery == null)
			{
				// query has been cleared
				pks = new BufferedDataSet();
			}
			else
			{
				pks = performQuery(transaction_id, theQuery, getRowIdentColumnTypes(), 0, rowsToRetrieve, IDataServer.FOUNDSET_LOAD_QUERY);
			}
			synchronized (pksAndRecords)
			{
				// optimistic locking, if the query has been changed in the mean time forget about the refresh
				if (sqlSelect != null || theQuery == null || theQuery == pksAndRecords.getQuerySelectForReading())
				{
					changes = pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), theQuery);
					cachedRecords = pksAndRecords.getCachedRecords();
				}
				else
				{
					Debug.log("refreshFromDBInternal: query was changed during refresh, not resetting old query"); //$NON-NLS-1$
				}
			}
			if (Debug.tracing())
			{
				Debug.trace(Thread.currentThread().getName() + ": RefreshFrom DB time: " + (System.currentTimeMillis() - time) + " pks: " + pks.getRowCount() + //$NON-NLS-1$//$NON-NLS-2$
					", SQL: " + theQuery); //$NON-NLS-1$
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}

		initialized = true;

		clearInternalState(true);

		if (dropSort) lastSortColumns = defaultSort;

		int selectedIndex = -1;
		synchronized (pksAndRecords)
		{
			if (cachedRecords == pksAndRecords.getCachedRecords())
			{
				pks = pksAndRecords.getPks();
				for (Entry<Integer, IRecordInternal> entry : newRecords.entrySet())
				{
					int newRecordIndex = entry.getKey().intValue();
					IRecordInternal newRecord = entry.getValue();
					if (newRecordIndex == 0)
					{
						cachedRecords.add(0, newRecord);
						pks.addRow(0, newRecord.getPK());
						selectedIndex = 0;
						changes = null;
					}
					else if (newRecordIndex > 0)
					{
						newRecordIndex = pks.getRowCount();
						cachedRecords.add(newRecordIndex, newRecord);
						pks.addRow(newRecordIndex, newRecord.getPK());
						selectedIndex = newRecordIndex;
						changes = null;
					}
				}

				if (keepPkOrder)
				{
					pksAndRecords.reorder(oldPKs);
					changes = null;
				}
			}
			else
			{
				Debug.log("refreshFromDBInternal: cached records were changed during refresh, not reading editing records (would be duplicated)"); //$NON-NLS-1$
			}

			IRecordInternal[] currentEditedRecords = editRecordList.getEditedRecords(this);
			outer : for (IRecordInternal record : currentEditedRecords)
			{
				Object[] pk = record.getPK();
				pks = pksAndRecords.getPks();
				int i = 0;
				while (true)
				{
					for (; i < pks.getRowCount(); i++)
					{
						if (equalObjects(pks.getRow(i), pk))
						{
							pksAndRecords.getCachedRecords().set(i, record);
							continue outer;
						}
					}
					if (getSize() < oldSize && pks.hadMoreRows())
					{
						int hint = ((getSize() / fsm.config.pkChunkSize()) + 2) * fsm.config.pkChunkSize();
						queryForMorePKs(pksAndRecords, pks.getRowCount(), hint, true);
						changes = null;
					}
					else
					{
						break;
					}
				}

			}
		}

		// let the List know the model changed
		int newSize = getSize();
		fireDifference(oldSize, newSize, changes);

		// move to correct position if we know
		if (selectedIndex == -1)
		{
			trySelectingPks(selectedPKs, newSize, false);
		}
		else
		{
			setSelectedIndex(selectedIndex);
		}
	}

	public boolean hasAccess(int access)
	{
		return fsm.getEditRecordList().hasAccess(getSQLSheet().getTable(), access);
	}

	/**
	 * Clears the foundset.
	 *
	 * @deprecated As of release 3.1, replaced by {@link #clear()}.
	 */
	@Deprecated
	public void js_clearFoundSet()
	{
		js_clear();
	}

	/**
	 * Clear the foundset.
	 *
	 * This will set a special condition in the query that makes the query not return any results.
	 *
	 * But if new Records are added to this foundset, then those records become the query pk set.
	 * So it will then behave the same as loadRecord(pkset) of a pkset of those new records.
	 *
	 * You can query for this state in the isCleared() call so you can call loadRecords() to remove that cleared state if needed.
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
		fireDifference(size, getSize(), null);
	}

	/**
	 * Returns a boolean if this foundset is in a cleared state (has the clear condition that is added by a clear() call)
	 *
	 * @return boolean true if this foundset is cleared
	 *
	 * @since 2023.09
	 */
	public boolean js_isCleared()
	{
		return pksAndRecords.getQuerySelectForReading().getCondition(SQLGenerator.CONDITION_CLEAR) != null;
	}

	abstract boolean canDispose();

	/**
	 * Dispose a foundset from memory when foundset is no longer needed. Should be used to destroy separate foundsets (is an optimization for memory management).
	 * A related foundset or a foundset which is linked to visible forms/components cannot be disposed. Returns whether foundset was disposed or not.
	 *
	 * @sample
	 * %%prefix%%foundset.dispose();
	 *
	 * @return boolean foundset was disposed
	 */
	@SuppressWarnings("nls")
	@JSFunction
	public boolean dispose()
	{
		if (getRelationName() != null)
		{
			Debug.warn("Cannot dispose the related foundset:  " + getRelationName() + ", fs: " + this);
			return false;
		}
		if (foundSetEventListeners.size() != 0 && !(foundSetEventListeners.size() == 1 && foundSetEventListeners.get(0) instanceof GlobalFoundSetEventListener))
		{
			Debug.warn("Cannot dispose foundset, still linked to component, fs: " + this + ", listeners: " + foundSetEventListeners);
			return false;
		}
		if (!canDispose())
		{
			Debug.warn("Cannot dispose foundset, still linked to form UI, fs: " + this);
			return false;
		}
		rowManager.unregister(this);
		clear();
		getFoundSetManager().removeFoundSet(this);
		disposed = true;
		return true;
	}

	/**
	 * Check if the foundset was disposed.
	 * <br/><br/>
	 * When the foundset.dispose() method was called successfully, a foundset can no longer be used.
	 *
	 * @sample
	 * // check if the foundset was disposed.
	 * if (%%prefix%%foundset.isDisposed())
	 * {
	 *    // foundset cannot be used anymore
	 * }
	 */
	@JSFunction
	public boolean isDisposed()
	{
		return disposed;
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * Use clear(), reloadWithFilters(), loadRecords() or loadAllRecords() to make the filter effective.
	 * Multiple filters can be added to the same dataprovider, they will all be applied.
	 *
	 * @sampleas js_addFoundSetFilterParam(String, String, Object, String)
	 *
	 * @param dataprovider String column to filter on.
	 *
	 * @param operator String operator: =, <, >, >=, <=, !=, (NOT) LIKE, (NOT) IN, (NOT) BETWEEN and IS (NOT) NULL optionally
	 * augmented with modifiers "#" (ignore case) or "^||" (or-is-null), prefix with "sql:" to allow the value to be interpreted as a custom query.
	 *
	 * @param value Object filter value (for in array and between an array with 2 elements)
	 *
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value) throws ServoyException
	{
		return addFilterParam(null, dataprovider, operator, value);
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * Use clear(), reloadWithFilters(), loadRecords() or loadAllRecords() to make the filter effective.
	 * The filter is removed again using removeFoundSetFilterParam(name).
	 *
	 * @sample
	 * var success = foundset.addFoundSetFilterParam('customerid', '=', 'BLONP', 'custFilter'); // possible to add multiple
	 * // Named filters can be removed using foundset.removeFoundSetFilterParam(filterName)
	 *
	 * // you can use modifiers in the operator as well, filter on companies where companyname is null or equals-ignore-case 'servoy'
	 * var ok = foundset.addFoundSetFilterParam('companyname', '#^||=', 'servoy')
	 *
	 * // Filters with in-conditions can be used with arrays or with custom queries:
	 * success = foundset.addFoundSetFilterParam("productcode", "in", [120, 144, 200]);
	 * success = foundset.addFoundSetFilterParam("city", "in", ["London", "Paris"]);
	 * // use "sql:in" in stead of "in" to allow the value to be interpreted as a custom query
	 * success = foundset.addFoundSetFilterParam("countrycode", "sql:in", "select country code from countries where region in ('Europe', 'Asia')");
	 *
	 * foundset.loadAllRecords(); // to make param(s) effective
	 *
	 * // see https://wiki.servoy.com/display/DOCS/Using+Table+Filters
	 *
	 * @param dataprovider String column to filter on.
	 *
	 * @param operator String operator: =, <, >, >=, <=, !=, (NOT) LIKE, (NOT) IN, (NOT) BETWEEN and IS (NOT) NULL optionally
	 * augmented with modifiers "#" (ignore case) or "^||" (or-is-null), prefix with "sql:" to allow the value to be interpreted as a custom query.
	 *
	 * @param value Object filter value (for in array and between an array with 2 elements)
	 *
	 * @param name String name, used to remove the filter again.
	 *
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value, String name) throws ServoyException
	{
		return addFilterParam(name, dataprovider, operator, value);
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 *
	 * Filters on tables touched in the query will not be applied to the query filter.
	 * For example, when a table filter exists on the order_details table,
	 * a query filter with a join from orders to order_details will be applied to the foundset,
	 * but the filter condition on the orders_details table will not be included.
	 *
	 * Use clear(), reloadWithFilters(), loadRecords() or loadAllRecords() to make the filter effective.
	 * Multiple filters can be added to the same dataprovider, they will all be applied.
	 *
	 * @sampleas js_addFoundSetFilterParam(QBSelect, String)
	 *
	 * @param query condition to filter on.
	 *
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public boolean js_addFoundSetFilterParam(QBSelect query)
	{
		return js_addFoundSetFilterParam(query, null);
	}

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 *
	 * Filters on tables touched in the query will not be applied to the query filter.
	 * For example, when a table filter exists on the order_details table,
	 * a query filter with a join from orders to order_details will be applied to the foundset,
	 * but the filter condition on the orders_details table will not be included.
	 *
	 * Use clear(), reloadWithFilters(), loadRecords() or loadAllRecords() to make the filter effective.
	 * The filter is removed again using removeFoundSetFilterParam(name).
	 *
	 * The table of the query has to be the same as the foundset table.
	 *
	 * @sample
	 *
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(
	 *    query.or.add(
	 *             query.columns.shipcity.eq('Amersfoort'))
	 *    .add(    query.columns.shipcity.eq('Amsterdam')));
	 *
	 * var success = foundset.addFoundSetFilterParam(query, 'cityFilter'); // possible to add multiple
	 * // Named filters can be removed using foundset.removeFoundSetFilterParam(filterName)
	 *
	 * foundset.loadAllRecords(); // to make param(s) effective
	 *
	 * @param query condition to filter on.
	 *
	 * @param name String name, used to remove the filter again.
	 *
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public boolean js_addFoundSetFilterParam(QBSelect query, String filterName)
	{
		if (query == null || sheet.getTable() == null || !sheet.getTable().getDataSource().equals(query.getDataSource()))
		{
			return false;
		}

		QuerySelect queryClone = query.build(); // makes a clone
		// the make sure the main table of the query is linked to the table of the foundset
		queryClone.relinkTable(queryClone.getTable(), creationSqlSelect.getTable());
		return addFilterParam(filterName, new QueryTableFilterdefinition(queryClone));
	}

	/**
	 * Set multiple foundset filters at the same time.
	 * After all filters have been applied / updated, the foundset will re reloaded immediately with the new filters applied.
	 *
	 * The filters that have been applied with the same filter name will be removed and replaced with the new set of filters (which may be empty).
	 *
	 * @sample
	 *
	 * // Create a number of filters
	 * var filter1_10 = foundset.createTableFilterParam('customerid', '=', 10);
	 *
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipcity.eq('Amersfoort'));
	 * var filter2 = databaseManager.createTableFilterParam(query);
	 *
	 * // apply multiple filters at the same time, previous filters with the same name are removed:
	 * var success = foundset.setFoundSetFilters('myfilters', [filter1_10, filter2])
	 *
	 * // update one of the filters:
	 * var filter1_11 = foundset.createTableFilterParam('customerid', '=', 11);
	 *
	 * var success = foundset.setFoundSetFilters('myfilters', [filter1_11, filter2])
	 *
	 * // filters can be removed by setting them to an empty list:
	 * var success = databaseManager.setTableFilters('myfilters', [])
	 *
	 * @param filterName The name of the filter that should be set.
	 * @param tableFilters list of filters to be applied.
	 *
	 * @return true if the table filters could be applied.
	 */
	public boolean js_setFoundSetFilters(String filterName, JSTableFilter[] tableFilters) throws ServoyException
	{
		if (sheet.getTable() == null)
		{
			return false;
		}

		for (var tf : iterate(tableFilters))
		{
			if (Objects.isNull(tf))
			{
				Debug.warn("setFoundSetFilters: invalid argument: null filter");
				return false;
			}
			if (!sheet.getTable().getDataSource().equals(tf.getTable().getDataSource()))
			{
				Debug.warn("setFoundSetFilters: invalid argument: datasource not for this foundset: " + tf.getTable().getDataSource());
				return false;
			}
		}

		EditRecordList editRecordList = fsm.getEditRecordList();
		if (editRecordList.stopIfEditing(this) != ISaveConstants.STOPPED)
		{
			Debug.log("Couldn't add foundset filter param because foundset had edited records"); //$NON-NLS-1$
			return false;
		}

		List<TableFilter> originalFilters = foundSetFilters == null ? null : new ArrayList<>(foundSetFilters);

		removeFilterParamInternal(filterName);
		if (tableFilters != null && tableFilters.length > 0)
		{
			if (foundSetFilters == null)
			{
				foundSetFilters = new ArrayList<>();
			}

			stream(tableFilters)
				.map(tf -> new TableFilter(filterName, sheet.getServerName(), sheet.getTable().getName(), sheet.getTable().getSQLName(),
					tf.getTableFilterdefinition(), false))
				.forEach(foundSetFilters::add);
		}

		if (!Objects.equals(originalFilters, foundSetFilters))
		{
			resetFiltercondition(originalFilters);
			browseAllInternal();
		}

		return true;
	}

	/**
	 * Create a table filter that can be applied to the foundset.
	 * Multiple filters can be applied at the same time using foundset.setTableFilters().
	 *
	 * @sample
	 * // filter on messages table where messagesid>10
	 * var filter = foundset.createTableFilterParam('messagesid', '>', 10)
	 *
	 * // some filters with in-conditions
	 * var filter = foundset.createTableFilterParam('productcode', 'in', [120, 144, 200])
	 * // use "sql:in" in stead of "in" to allow the value to be interpreted as a custom query
	 * var filter = foundset.createTableFilterParam('countrycode', 'sql:in', 'select country code from countries where region = "Europe"')
	 *
	 * // you can use modifiers in the operator as well, filter on companies where companyname is null or equals-ignore-case 'servoy'
	 * var filter = foundset.createTableFilterParam('companyname', '#^||=', 'servoy')
	 *
	 * // the value may be null, this will result in 'column is null' sql condition.
	 * var filter = foundset.createTableFilterParam('verified', '=', null)
	 *
	 * // apply multiple filters at the same time, previous filters with the same name are removed:
	 * var success = foundset.setTableFilters('myfilters', [filter1, filter2])
	 *
	 * @param dataprovider A specified dataprovider column name.
	 * @param operator One of "=, <, >, >=, <=, !=, LIKE, or IN" optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null), prefix with "sql:" to allow the value to be interpreted as a custom query.
	 * @param value The specified filter value.
	 *
	 * @return table filter or null when no filter could be created.
	 */
	public JSTableFilter js_createTableFilterParam(String dataprovider, String operator, Object value)
	{
		if (sheet.getTable() == null)
		{
			return null;
		}

		try
		{
			DataproviderTableFilterdefinition dataproviderTableFilterdefinition = fsm.createDataproviderTableFilterdefinition(
				sheet.getTable(), dataprovider, operator, value);
			if (dataproviderTableFilterdefinition != null)
			{
				return new JSTableFilter(sheet.getTable().getServerName(), sheet.getTable(), dataproviderTableFilterdefinition);
			}

			fsm.getApplication().reportJSError(
				"Foundset filter not created, column not found in table or operator invalid, serverName = '" + sheet.getTable().getServerName() + "'" +
					", tableName = '" + sheet.getTable().getName() + ", dataprovider = '" + dataprovider + "', operator = '" + operator + "'",
				null);
		}
		catch (ServoyException ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * Remove a named foundset filter.
	 * Use clear(), reloadWithFilters(), loadRecords() or loadAllRecords() to make the filter effective.
	 *
	 * @sample
	 * var success = foundset.removeFoundSetFilterParam('custFilter');// removes all filters with this name
	 * foundset.loadAllRecords(); // to make param(s) effective
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
	 * Get a previously defined foundset filter, using its given name.
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
	 * @param filterName name of the filter to retrieve.
	 *
	 * @return Array of filter definitions.
	 */
	public Object[][] js_getFoundSetFilterParams(String filterName)
	{
		return getFoundSetFilterParams(filterName);
	}

	/**
	 * Get the list of previously defined foundset filters.
	 *
	 * For column-based table filters, a row of 5 fields per filter are returned.
	 * The "columns" of a row from this array are: tablename, dataprovider, operator, value, filtername
	 *
	 * For query-based filters, a row of 2 fields per filter are returned.
	 * The "columns" of a row from this array are: query, filtername
	 *
	 * @sample
	 * var params = foundset.getFoundSetFilterParams()
	 * for (var i = 0; params != null && i < params.length; i++)
	 * {
	 *  if (params[i].length() == 5) {
	 * 		application.output('FoundSet filter on table ' + params[i][0] + ': '+ params[i][1] + ' '+params[i][2] + ' '+params[i][3] + (params[i][4] == null ? ' [no name]' : ' ['+params[i][4]+']'))
	 * 	}
	 *  if (params[i].length() == 2) {
	 * 		application.output('FoundSet filter with query ' + params[i][0]+ ': ' + (params[i][1] == null ? ' [no name]' : ' ['+params[i][1]+']'))
	 * 	}
	 * }
	 *
	 * @return Array of filter definitions.
	 */
	public Object[][] js_getFoundSetFilterParams()
	{
		return getFoundSetFilterParams(null);
	}

	public Object[][] getFoundSetFilterParams(String filterName)
	{
		List<Object[]> result = new ArrayList<Object[]>();
		for (TableFilter f : iterate(foundSetFilters))
		{
			if (filterName == null || filterName.equals(f.getName()))
			{
				if (f.getTableFilterdefinition() instanceof DataproviderTableFilterdefinition)
				{
					DataproviderTableFilterdefinition tableFilterdefinition = (DataproviderTableFilterdefinition)f.getTableFilterdefinition();
					result.add(new Object[] { f.getTableName(), tableFilterdefinition.getDataprovider(), RelationItem.getOperatorAsString(
						tableFilterdefinition.getOperator()), tableFilterdefinition.getValue(), f.getName() });
				}
				if (f.getTableFilterdefinition() instanceof QueryTableFilterdefinition)
				{
					QuerySelect querySelect = ((QueryTableFilterdefinition)f.getTableFilterdefinition()).getQuerySelect();
					result.add(new Object[] { new QBSelect(fsm, fsm.getScopesScopeProvider(), fsm.getApplication().getFlattenedSolution(),
						fsm.getApplication().getScriptEngine().getSolutionScope(), querySelect.getTable().getDataSource(), null,
						deepClone(querySelect, true)), f.getName() });
				}
			}
		}

		return result.toArray(new Object[result.size()][]);
	}

	/**
	 * Get a duplicate of the foundset. This is a full copy of the foundset (cached pks,records, relation, filters, search criteria, omitted records, selection).
	 *
	 * @sample
	 * var dupFoundset = %%prefix%%foundset.duplicateFoundSet();
	 * %%prefix%%foundset.find();
	 * //search some fields
	 * var count = %%prefix%%foundset.search();
	 * if (count == 0)
	 * {
	 * 	plugins.dialogs.showWarningDialog('Alert', 'No records found','OK');
	 * 	%%prefix%%foundset.loadRecords(dupFoundset);
	 * }
	 *
	 * @return foundset duplicate.
	 */
	public FoundSet js_duplicateFoundSet() throws ServoyException//can be used by loadRecords Again
	{
		return (FoundSet)copy(false);
	}

	/**
	 * Set the foundset in find mode. (Start a find request), use the "search" function to perform/exit the find.
	 *
	 * Before going into find mode, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off), the foundset will not go into find mode.
	 * Make sure the operator and the data (value) are part of the string passed to dataprovider (included inside a pair of quotation marks).
	 * Note: always make sure to check the result of the find() method.
	 *
	 * When in find mode, columns can be assigned string expressions (including operators) that are evaluated as:
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
	 * Arrays can be used for searching a number of values, this will result in an 'IN' condition that will be used in the search.
	 * The values are not restricted to strings but can be any type that matches the column type.
	 * For example, "record.department_id = [1, 33, 99]"
	 *
	 * Multiple Find Records for Logical OR:
	 * It's important to note that when in Find Mode, a foundset will initially contain one record object.
	 * However, multiple record objects may be used to articulate search criteria.
	 * This has the effect that the criteria described in each record are concatenated by a SQL OR.
	 *
	 * @sample
	 * if (%%prefix%%foundset.find()) //find will fail if autosave is disabled and there are unsaved records
	 * {
	 * 	columnTextDataProvider = 'a search value'
	 * 	// for numbers you have to make sure to format it correctly so that the decimal point is in your locales notation (. or ,)
	 * 	columnNumberDataProvider = '>' + utils.numberFormat(anumber, '####.00');
	 * 	columnDateDataProvider = '31-12-2010|dd-MM-yyyy'
	 * 	%%prefix%%foundset.search()
	 * }
	 *
	 * // Multiple Find Records for Logical OR - example
	 * // Find customers in the city of Berlin AND in the postal code 12209...
	 * // OR customers in the city of San Francisco AND in the postal code 94117
	 * if (%%prefix%%foundset.find()) // Enter find mode    city = 'Berlin';
	 * {
	 * 	city = 'Berlin';   // Assign a search criteria
	 * 	postalcode = '12209';
	 * 	%%prefix%%foundset.newRecord();   // Create a new search record
	 * 	city = 'San Francisco';
	 *  postalcode = '94117';
	 * 	%%prefix%%foundset.search();      // Execute the query and load the records
	 * }
	 *
	 * @return true if the foundset is now in find mode, false otherwise.
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_search(Boolean, Boolean)
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_setAutoSave(boolean)
	 * @see com.servoy.j2db.BasicFormController$JSForm#js_find()
	 * @see com.servoy.j2db.BasicFormController$JSForm#js_search(Boolean, Boolean)
	 */
	@JSFunction
	public boolean find()
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
	@JSFunction
	public boolean isInFind()
	{
		return isInFindMode();
	}

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 * Clear results from previous searches.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 *
	 * @sampleas js_search(Boolean, Boolean)
	 *
	 * @return the recordCount
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#find()
	 */
	@JSFunction
	public int search() throws ServoyException
	{
		return js_search(Boolean.TRUE, Boolean.TRUE);
	}

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 * Reduce results from previous searches.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 *
	 * @sampleas js_search(Boolean, Boolean)
	 *
	 * @param clearLastResults boolean, clear previous search, default true
	 *
	 * @return the recordCount
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#find()
	 */
	public int js_search(Boolean clearLastResults) throws ServoyException
	{
		return js_search(clearLastResults, Boolean.TRUE);
	}

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 *
	 * @sample
	 * var recordCount = %%prefix%%foundset.search();
	 * //var recordCount = %%prefix%%foundset.search(false,false); //to extend foundset
	 *
	 * @param clearLastResults boolean, clear previous search, default true
	 * @param reduceSearch boolean, reduce (true) or extend (false) previous search results, default true
	 *
	 * @return the recordCount
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#find()
	 */
	public int js_search(Boolean clearLastResults, Boolean reduceSearch) throws ServoyException
	{
		boolean _clearLastResults = getBooleanAsbool(clearLastResults, true);
		boolean _reduceSearch = getBooleanAsbool(reduceSearch, true);

		if (isInFindMode())
		{
			int nfound = performFind(_clearLastResults, _reduceSearch, true, false, null);
			return nfound < 0 ? /* blocked */0 : nfound;
		}
		return 0;
	}

	/** Check wether the foundset has any conditions from a previous find action.
	 *
	 * @sample
	 * if (%%prefix%%foundset.hasConditions())
	 * {
	 * 		// foundset had find actions
	 * }
	 *
	 * @return wether the foundset has find-conditions
	 */
	public boolean js_hasConditions()
	{
		QuerySelect query = pksAndRecords.getQuerySelectForReading();
		return query != null &&
			stream(query.getConditions(SQLGenerator.CONDITION_SEARCH)).anyMatch(condition -> !SQLGenerator.isDynamicPKSetCondition(condition));
	}

	/**
	 * Gets the name of the table used.
	 *
	 * @deprecated As of release 5.0, replaced by {@link #getDataSource()}.
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
	 * @deprecated As of release 5.0, replaced by {@link #getDataSource()}.
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
	 * Gets the parent records when called on a related foundset. (empty array if not a related foundset)
	 * Depending on the cardinality of the relation, this method returns either 1 or more records.
	 * This can be useful when creating a new record in an empty related foundset and some data from the parent record(s) is needed.
	 * <br/><br/>
	 * Be aware that if datasources.xxx.getFoundset() is called multiple times on the same datasource it creates multiple foundset instances
	 * then the related foundset can have multiple references to the same parent record but in different foundsets.
	 * (different instances of the record for the same row in the database)
	 * In that case, this method will return the record from the first foundset.
	 *
	 * @sample var parents = relatedFoundset.getParentRecords();
	 *
	 * @return an array of records
	 */
	@JSFunction
	public IRecordInternal[] getParentRecords()
	{
		if (this.allParents.isEmpty()) return new IRecordInternal[0];
		Map<String, IRecordInternal> uniqueRecords = new HashMap<String, IRecordInternal>();
		for (IRecordInternal record : getParents())
		{
			uniqueRecords.putIfAbsent(record.getPKHashKey(), record);
		}
		return uniqueRecords.values().toArray(new IRecordInternal[0]);
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
			// get out of find mode, no need to do a find query here, loadAllRecords() will do that anyway
			clearInternalState(true);
			int oldSize = getSize();
			pksAndRecords.setPks(null, 0);
			fireDifference(oldSize, 0, null);
		}

		loadAllRecords();
		return true;
	}

	/**
	 * Reloads all last records again with the filters applied.
	 *
	 * @sample
	 *  %%prefix%%foundset.reloadWithFilters();
	 *
	 */
	public void js_reloadWithFilters() throws ServoyException
	{
		refreshFromDBInternal(
			fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), pksAndRecords.getTempQuery(), null, true, null, lastSortColumns, false),
			false, fsm.config.pkChunkSize(), false, false);
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
	 * Returns a JSDataSet with the PKs omitted on this foundset
	 * If no PKs have been omitted, an empty JSDataSet will be returned
	 *
	 * @sample %%prefix%%foundset.getOmittedPKs();
	 *
	 * @return a JSDataSet
	 */
	@JSFunction
	public JSDataSet getOmittedPKs()
	{
		BufferedDataSet set = omittedPKs != null ? (BufferedDataSet)omittedPKs.clone() : new BufferedDataSet();
		set.setColumnNames(sheet.getPKColumnDataProvidersAsArray());
		return new JSDataSet(set);
	}

	protected boolean checkLoadRecordsAllowed(boolean allowRelated, boolean allowInFind)
	{
		if (sheet.getTable() == null)
		{
			throw new IllegalStateException("couldn't load dataset on a foundset that has no table: " + this); //$NON-NLS-1$
		}

		if (!allowRelated && relationName != null) // on related foundset, only allow loadRecords without arguments
		{
			throw new IllegalStateException("Can't load data/records in a related foundset: " + this); //$NON-NLS-1$
		}

		if (isInFindMode() && allowInFind)
		{
			pksAndRecords.setPks(null, 0);
			setSelectedIndex(-1);
			clearInternalState(true); // goes out of find mode
		}

		if (isInFindMode())
		{
			Debug.log("couldn't load new records on a foundset that is in find mode"); //$NON-NLS-1$
			fsm.getApplication().reportJSError("couldn't load dataset on a foundset that is in find mode", null); //$NON-NLS-1$
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

		return true;
	}

	public boolean cancelFind() throws ServoyException
	{
		// revert to foundset as before find mode
		performFind(false, false, true, true, null); // cancel find mode
		return !findMode;
	}

	/**
	 * Reloads all last (related) records again, if, for example, after search in tabpanel.
	 * When in find mode, this will reload the records from before the find() call.
	 *
	 * @sample
	 *  //to reload all last (related) records again, if for example when searched in tabpanel
	 *  %%prefix%%foundset.loadRecords();
	 *
	 * @return true if successful
	 */
	public boolean js_loadRecords() throws ServoyException
	{
		if (isInFindMode())
		{
			return cancelFind();
		}
		if (!checkLoadRecordsAllowed(true, false))
		{
			return false;
		}
		loadAllRecords();
		return true;
	}

	/**
	 * @clonedesc com.servoy.j2db.BasicFormController$JSForm#js_loadRecords(IDataSet)
	 *
	 * @sample
	 * // loads a primary key dataset, will remove related sort!
	 * //var dataset = databaseManager.getDataSetByQuery(...);
	 * // dataset must match the table primary key columns (alphabetically ordered)
	 * %%prefix%%foundset.loadRecords(dataset);
	 *
	 * @param dataset pkdataset
	 *
	 * @return true if successful
	 */
	public boolean js_loadRecords(IDataSet dataset) throws ServoyException
	{
		return checkLoadRecordsAllowed(false, false) && loadExternalPKList(dataset, true);
	}

	/**
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 *
	 * @param dataset The dataset to load records from
	 * @param ignored true for ignoring the argument array
	 *
	 * @deprecated use loadRecords(JSDataSet)
	 */
	@Deprecated
	public boolean js_loadRecords(IDataSet dataset, Object ignored) throws ServoyException
	{
		return js_loadRecords(dataset);
	}

	/**
	 * Copies foundset data from another foundset.
	 * This will alter the foundset state to the state of the foundset that is given.
	 * If you really just want to use the given foundset on the form itself, then you need to use controller.loadRecords(foundset)
	 * that will change the instance of the foundset that is used for this form. Not just update an existing forms foundset.
	 * <br/><br/>
	 * If you copy over a relation into this foundset, then this foundset will not be a related foundset, it will not automatically update its state
	 * of records are updated or added that belong to that relation. It will only be a snapshot of that related foundsets state.
	 * <br/><br/>
	 * Foundset filter params are copied over from the original/source foundset and are merged with the existing filters on this foundset.
	 * So if the original foundset had filters and the given foundset has filters then the resulting foundset will have all the filters of both,
	 * If you don't want this and you really want only the state of the given foundset, use controller.loadRecords(fs) instead of foundset.loadRecords(fs)
	 *
	 * @sample
	 * //Copies foundset data from another foundset
	 * %%prefix%%foundset.loadRecords(fs);
	 *
	 * @param foundset The foundset to load records from
	 *
	 * @return true if successful
	 */
	public boolean js_loadRecords(FoundSet foundset)
	{
		return checkLoadRecordsAllowed(false, false) && copyFrom(foundset);
	}

	/**
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 *
	 * @param foundset The foundset to load record from
	 * @param ignored true for ignoring the argument array
	 *
	 * @deprecated use loadRecords(FoundSet)
	 */
	@Deprecated
	public boolean js_loadRecords(FoundSet foundset, Object ignored)
	{
		return js_loadRecords(foundset);
	}

	/**
	 * @clonedesc com.servoy.j2db.BasicFormController$JSForm#js_loadRecords(String)
	 * @sample
	 * //loads records in to the foundset based on a query (also known as 'Form by query')
	 * %%prefix%%foundset.loadRecords(sqlstring,parameters);
	 *
	 * @param queryString select statement
	 * @param argumentsArray arguments to query
	 *
	 * @return true if successful
	 */
	public boolean js_loadRecords(String queryString, Object[] argumentsArray) throws ServoyException
	{
		return checkLoadRecordsAllowed(false, false) && loadByQuery(queryString, argumentsArray);
	}

	/**
	 * @clonedesc com.servoy.j2db.BasicFormController$JSForm#js_loadRecords(String)
	 * @sample
	 * //loads records in to the foundset based on a query (also known as 'Form by query')
	 * %%prefix%%foundset.loadRecords(sqlstring);
	 *
	 * @param queryString select statement
	 *
	 * @return true if successful
	 */
	public boolean js_loadRecords(String queryString) throws ServoyException
	{
		return js_loadRecords(queryString, null);
	}

	/**
	 * @clonedesc com.servoy.j2db.BasicFormController$JSForm#js_loadRecords(Number)
	 * @sample
	 * //Loads a single record by primary key, will remove related sort!
	 * %%prefix%%foundset.loadRecords(123);
	 *
	 * @param numberpk single-column pk value
	 *
	 * @return true if successful
	 */
	public boolean js_loadRecords(Number numberpk) throws ServoyException
	{
		return loadRecordsBySinglePK(numberpk);
	}

	/**
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 *
	 * @param numberpk single-column pk value
	 * @param ignored true to ignore arguments array
	 *
	 * @deprecated use loadRecords(Number)
	 */
	@Deprecated
	public boolean js_loadRecords(Number numberpk, Object ignored) throws ServoyException
	{
		return js_loadRecords(numberpk);
	}

	/**
	 * @clonedesc com.servoy.j2db.BasicFormController$JSForm#js_loadRecords(UUID)
	 * @sample
	 * //Loads a single record by primary key, will remove related sort!
	 * %%prefix%%foundset.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
	 *
	 * @param uuidpk single-column pk value
	 * @return true if successful
	 */
	public boolean js_loadRecords(UUID uuidpk) throws ServoyException
	{
		return loadRecordsBySinglePK(uuidpk);
	}

	/**
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 * @param uuidpk single-column pk value
	 * @param ignored true to ignore argument array
	 *
	 * @deprecated use loadRecords(UUID)
	 */
	@Deprecated
	public boolean js_loadRecords(UUID uuidpk, Object ignored) throws ServoyException
	{
		return js_loadRecords(uuidpk);
	}

	protected boolean loadRecordsBySinglePK(Object pk) throws ServoyException
	{
		if (!checkLoadRecordsAllowed(false, false))
		{
			return false;
		}

		List<Column> pkColumns = sheet.getTable() == null ? null : sheet.getTable().getRowIdentColumns();
		if (pkColumns != null && pkColumns.size() == 1)
		{
			return loadExternalPKList(new BufferedDataSet(new String[] { pkColumns.get(0).getName() }, Collections.singletonList(new Object[] { pk })), true);
		}

		return false;
	}

	/**
	 * Loads records into form foundset based on a query builder object (also known as 'Form by query').
	 * When the foundset is in find mode, the find states are discarded, the foundset will go out of find mode and the foundset will be loaded using the query.
	 * If the foundset is related, the relation-condition will be added to the query.
	 * Tries to preserve selection based on primary key, otherwise first record is selected.
	 *
	 * The query of the QBSelect that is given is added as a "search" condition to the existing base query of the foundset.
	 * This does mean that loadAllRecords() will revert this, because that will clear the search condition and go back to the base query of the foundset.
	 * Some hold true for clear() that will remove the search condition and because of that the given query will also be removed.
	 *
	 * If you want to create more a "view" on your database that will always be kept by this foundset, so loadAllRecords() (with our withou first calliing clear()) will always
	 * revert back to this set of data (and you can also search inside this data with find/search or adding another query on top of it.
	 * Then have a look at datasources.db.server.table.getFoundset(query) because that will generate a foundset with the given query as the base query.
	 *
	 * @sample
	 * var qb = datasources.db.example_data.orders.createSelect();
	 * qb.result.addPk();
	 * qb.where.add(qb.columns.product_id.eq(1))
	 * %%prefix%%foundset.loadRecords(qb);
	 *
	 * @param querybuilder the query builder
	 * @return true if successful
	 */
	public boolean js_loadRecords(QBSelect querybuilder) throws ServoyException
	{
		return checkLoadRecordsAllowed(true, true) && loadByQuery(querybuilder);
	}

	/**
	 * Method to handle old foundset loadRecords calls.
	 * Deprecated method to handle pre-6.1 calls to varargs function foundset.loadRecords([1]), this was called with vargs=[1] in stead of vargs=[[1]].
	 *
	 * @param vargs the arguments
	 *
	 * @deprecated use loadRecords with single typed argument
	 */
	@Deprecated
	public boolean js_loadRecords(Object[] vargs) throws ServoyException
	{
		if (vargs == null || vargs.length != 1)
		{
			throw new IllegalArgumentException("Cannot find function loadRecords for " + (vargs == null ? "no" : String.valueOf(vargs.length)) + " args"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}

		Object data = vargs[0];

		if (data instanceof Wrapper)
		{
			data = ((Wrapper)data).unwrap();
		}

		if (data instanceof IDataSet)
		{
			return js_loadRecords((IDataSet)data);
		}
		if (data instanceof FoundSet)
		{
			return js_loadRecords((FoundSet)data);
		}
		if (data instanceof String)
		{
			return js_loadRecords((String)data);
		}
		if (data instanceof Number)
		{
			return js_loadRecords((Number)data);
		}
		if (data instanceof UUID)
		{
			return js_loadRecords((UUID)data);
		}
		if (data == null)
		{
			// legacy v6 behaviour
			loadAllRecords();
			return true;
		}

		throw new IllegalArgumentException("Cannot find function loadRecords for argument " + data.getClass().getName()); //$NON-NLS-1$
	}

	/**
	 * Perform a relookup for the record under the given index
	 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
	 *
	 * @sample %%prefix%%foundset.relookup(1);
	 * @param index record index (1-based)
	 */
	public void js_relookup(Number index)
	{
		if (index == null) return;
		int _index = index.intValue();
		if (isInitialized() && _index > 0 && _index <= getSize())
		{
			processCopyValues(_index - 1);
		}
	}

	/**
	 * Perform a relookup for the currently selected records
	 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
	 *
	 * @sample %%prefix%%foundset.relookup(1);
	 */
	public void js_relookup()
	{
		if (isInitialized())
		{
			for (int i : getSelectedIndexes())
			{
				processCopyValues(i);
			}
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

	private int lastReturnedRecordIndex = 0;

	protected int getRecordIndex(Object[] pk)
	{
		IDataSet pks = pksAndRecords.getPks();
		if (pk != null && pk.length != 0 && pks != null && pks.getColumnCount() == pk.length)
		{
			int rowCount = pks.getRowCount();
			// we try to optimize this search assuming next index is located near the last one
			int startIndex = Math.min(lastReturnedRecordIndex - 10, rowCount);
			if (startIndex < 0) startIndex = 0;
			for (int r = startIndex; r < rowCount; r++)
			{
				int matchingIndex = getMatchingRecordIndex(pk, pks, r);
				if (matchingIndex >= 0)
				{
					return matchingIndex;
				}
			}
			for (int r = 0; r < startIndex; r++)
			{
				int matchingIndex = getMatchingRecordIndex(pk, pks, r);
				if (matchingIndex >= 0)
				{
					return matchingIndex;
				}
			}
		}
		return -1;
	}

	private int getMatchingRecordIndex(Object[] pk, IDataSet pks, int r)
	{
		Object[] pkrow = pks.getRow(r);
		if (pkrow != null)
		{
			boolean match = pkrow.length == pk.length;
			for (int c = 0; c < pkrow.length; c++)
			{
				match = match && equalObjects(pk[c], pkrow[c]);
			}
			if (match)
			{
				lastReturnedRecordIndex = r;
				return r;
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
		int index = getRecordIndex(pk);
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
				{
					if (index == selectedIdx)
					{
						indexAlreadySelected = true;
						continue;
					}
					newSelectedIndexes[i++] = selectedIdx;
				}
				if (indexAlreadySelected)
				{
					if (selectedIndexes.length > 1) // only deselect if there are at least 2 selected, so we always have a selection
					{
						int[] newSelectedIndexesTrimed = new int[newSelectedIndexes.length - 2];
						System.arraycopy(newSelectedIndexes, 0, newSelectedIndexesTrimed, 0, newSelectedIndexes.length - 2);
						newSelectedIndexes = newSelectedIndexesTrimed;
					}
					else
					{
						return true;
					}
				}
				else
				{
					newSelectedIndexes[i] = index;
				}

				setSelectedIndexes(newSelectedIndexes);
			}
			else
			{
				setSelectedIndex(index);
			}
			return true;
		}
		return false;
	}

	protected boolean selectRecords(Object[][] pks)
	{
		if (pks != null)
		{
			int[] selectedIndexes = new int[pks.length];
			for (int i = 0; i < pks.length; i++)
			{
				selectedIndexes[i] = getRecordIndex(pks[i]);
				if (selectedIndexes[i] == -1) return false;
			}

			setSelectedIndexes(selectedIndexes);
			return true;
		}

		return false;
	}

	public boolean loadByQuery(IQueryBuilder query) throws ServoyException
	{
		// check if this query is on our base table
		if (!Utils.stringSafeEquals(getDataSource(), query.getDataSource()))
		{
			throw new RepositoryException(
				"Cannot load foundset with query based on another table (" + getDataSource() + " != " + query.getDataSource() + ')').setContext( //$NON-NLS-1$//$NON-NLS-2$
					this.toString());
		}

		QuerySelect sqlSelect = ((QBSelect)query).build(); // makes a clone
		// The table of the creationSqlSelect and the query in pksAndRecords must always be the same object
		BaseQueryTable originalTable = sqlSelect.getTable();
		sqlSelect.relinkTable(sqlSelect.getTable(), creationSqlSelect.getTable());
		if (!creationSqlSelect.getTable().equals(originalTable))
		{
			// The tables are on the same datasource, but not equal (for example, originalTable has an alias that
			// is needed in a custom query part), in that case add a join to the original table
			sqlSelect.addJoin(SQLGenerator.createSelfJoin(getTable(), sqlSelect.getTable(), originalTable));
		}

		if (sqlSelect.getColumns() == null)
		{
			// no columns, add pk
			// note that QBSelect.build() already returns a clone
			Iterator<Column> pkIt = ((Table)getTable()).getRowIdentColumns().iterator();
			if (!pkIt.hasNext())
			{
				throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { getTable().getName() }).setContext(
					this.toString());
			}

			while (pkIt.hasNext())
			{
				Column c = pkIt.next();
				sqlSelect.addColumn(c.queryColumn(sqlSelect.getTable()));
			}
		}

		Placeholder dynamicPKplaceholder = sqlSelect.getPlaceholder(new TablePlaceholderKey(sqlSelect.getTable(), SQLGenerator.PLACEHOLDER_FOUNDSET_PKS));
		if (dynamicPKplaceholder != null && dynamicPKplaceholder.isSet() && dynamicPKplaceholder.getValue() instanceof Object[])
		{
			// loading from saved query, dynamic pk was replaced by array in serialization, make dynamic again
			dynamicPKplaceholder.setValue(new DynamicPkValuesArray(getSQLSheet().getTable().getRowIdentColumns(),
				SQLGenerator.createPKValuesDataSet(getSQLSheet().getTable().getRowIdentColumns(), (Object[][])dynamicPKplaceholder.getValue())));
		}

		if (sqlSelect.getSorts() == null)
		{
			// query does not define sort, use last sorts
			fsm.getSQLGenerator().addSorts(sqlSelect, sqlSelect.getTable(), this, sheet.getTable(), lastSortColumns == null ? defaultSort : lastSortColumns,
				true, false);
		}
		else
		{
			// try to determine the SortColumns from the query-sort
			lastSortColumns = determineSortColumns(sqlSelect);
		}

		return loadByQuery(addFilterconditions(sqlSelect, foundSetFilters));
	}

	/**
	 * @param sqlSelect
	 * @return
	 * @throws RepositoryException
	 */
	private List<SortColumn> determineSortColumns(QuerySelect sqlSelect) throws RepositoryException
	{
		List<SortColumn> sortColumns = null;
		for (IQuerySort qsort : iterate(sqlSelect.getSorts()))
		{
			if (qsort instanceof QuerySort)
			{
				IQuerySelectValue qcolumn = ((QuerySort)qsort).getColumn();
				if (qcolumn instanceof QueryColumn)
				{
					ColumnWrapper columnWrapper = findColumnWrapperForColumn(sqlSelect, (QueryColumn)qcolumn);
					if (columnWrapper != null)
					{
						if (sortColumns == null)
						{
							sortColumns = new ArrayList<>();
						}
						SortColumn sortColumn = new SortColumn(columnWrapper);
						sortColumn.setSortOrder(((QuerySort)qsort).isAscending() ? SortColumn.ASCENDING : SortColumn.DESCENDING);
						sortColumns.add(sortColumn);

						continue; // otherwise stop searching
					}
				}
			}
		}

		// stop searching when no match could be made to a valid column, return the ones found or null when nothing matched
		return sortColumns;
	}

	private ColumnWrapper findColumnWrapperForColumn(QuerySelect sqlSelect, QueryColumn qcolumn) throws RepositoryException
	{
		List<Relation> relationSequence = findQueryRelationSequence(sqlSelect, qcolumn.getTable());
		if (relationSequence == null)
		{
			return null;
		}

		// found the relations, match the column (by sqlName)
		ITable table = fsm.getTable(qcolumn.getTable().getDataSource());
		if (table != null)
		{
			IColumn column = table.getColumnBySqlname(qcolumn.getName());
			if (column != null)
			{
				return new ColumnWrapper(column, relationSequence.toArray(new Relation[relationSequence.size()]));
			}
		}

		return null;
	}

	private List<Relation> findQueryRelationSequence(QuerySelect sqlSelect, BaseQueryTable qTable)
	{
		if (sqlSelect.getTable() == qTable)
		{
			// column on base table, not related
			return Collections.emptyList();
		}

		// find the join to this table
		for (ISQLJoin join : iterate(sqlSelect.getJoins()))
		{
			if (join.getRelationName() != null && join instanceof QueryJoin && ((QueryJoin)join).getForeignTable() == qTable)
			{
				Relation relation = fsm.getRelation(join.getRelationName());
				if (relation != null && relation.getForeignDataSource().equals(qTable.getDataSource()))
				{
					List<Relation> subRelated = findQueryRelationSequence(sqlSelect, ((QueryJoin)join).getPrimaryTable());
					if (subRelated != null)
					{
						// found matching relation sequence
						List<Relation> relationSequence = new ArrayList<>(subRelated);
						relationSequence.add(relation);
						return relationSequence;
					}
				}
			}
		}

		// not found
		return null;
	}

	/**
	 * Get the query that the foundset is currently using (as a clone; modifying this QBSelect will not automatically change the foundset).
	 * When the foundset is in find mode, the find conditions are included in the resulting query.
	 * So the query that would be used when just calling search() (or search(true,true)) is returned.
	 * Note that foundset filters are optionally included and table filters are not included in the query.
	 *
	 * @sample
	 * var q = foundset.getQuery()
	 * q.where.add(q.columns.x.eq(100))
	 * foundset.loadRecords(q);
	 *
	 * @param includeFilters include the foundset filters, default true.
	 *
	 * @return query.
	 */
	@JSFunction
	public QBSelect getQuery(boolean includeFilters)
	{
		QuerySelect query;
		try
		{
			query = getCurrentStateQuery(true, true);
			if (!includeFilters)
			{
				query.clearCondition(SQLGenerator.CONDITION_FILTER);
				removeFilterJoins(query, foundSetFilters);
			}
		}
		catch (ServoyException e)
		{
			Debug.error(e);
			throw new RuntimeException(e.getMessage());
		}
		return new QBSelect(getFoundSetManager(), getFoundSetManager().getScopesScopeProvider(), getFoundSetManager().getApplication().getFlattenedSolution(),
			getFoundSetManager().getApplication().getScriptEngine().getSolutionScope(), getDataSource(), null, query);
	}

	/**
	 * @clonedesc getQuery(boolean)
	 * @sampleas getQuery(boolean)
	 *
	 * @return query.
	 */
	@JSFunction
	public QBSelect getQuery()
	{
		return getQuery(true);
	}

	/**
	 * Get foundset name. If foundset is not named foundset or related foundset will return null.
	 *
	 * @sample
	 * var name = foundset.getName()
	 *
	 * @return name.
	 */
	@JSFunction
	public String getName()
	{
		String name = getRelationName();
		if (name == null)
		{
			name = fsm.getFoundSetName(this);
		}
		return name;
	}

	private boolean loadByQuery(QuerySelect sqlSelect) throws ServoyException
	{
		if (initialized && (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED))
		{
			fsm.getApplication().reportJSError("couldn't load dataset because foundset had edited records but couldn't save it: " + this + //$NON-NLS-1$
				", edited record(s): " + Utils.stringJoin(getFoundSetManager().getEditRecordList().getEditedRecords(this), '.') + ", failed record(s): " +
				Utils.stringJoin(getFoundSetManager().getEditRecordList().getFailedRecords(this), '.'), null);
			return false;
		}

		IDataSet pks = pksAndRecords.getPks();

		Object[][] selectedPKs = getSelectedPksForKeepingSelection(pks, fsm.isAlwaysFollowPkSelection());

		int sizeBefore = getSize();
		if (sizeBefore > 1)
		{
			fireSelectionAdjusting();
		}

		clearOmit(sqlSelect);

		if (relationName != null)
		{
			ISQLCondition fsRelatedCondition = pksAndRecords.getQuerySelectForReading().getCondition(SQLGenerator.CONDITION_RELATION);
			ISQLCondition selectRelatedCondition = sqlSelect.getCondition(SQLGenerator.CONDITION_RELATION);
			if (selectRelatedCondition != null && !selectRelatedCondition.equals(fsRelatedCondition))
			{
				// add the different relation condition as search
				sqlSelect.addCondition(SQLGenerator.CONDITION_SEARCH, selectRelatedCondition);
			}

			// Make sure the relation condition of this related foundset is left untouched
			sqlSelect.setCondition(SQLGenerator.CONDITION_RELATION,
				AbstractBaseQuery.relinkTable(pksAndRecords.getQuerySelectForReading().getTable(), sqlSelect.getTable(), fsRelatedCondition));
		}

		initialized = true;

		int rowsToRetrieve = calculateRowsToRetrieve(fsm.config.pkChunkSize(), selectedPKs);

		// do query with sqlSelect
		String transaction_id = fsm.getTransactionID(sheet);
		IDataSet pk_data;
		try
		{
			pk_data = performQuery(transaction_id, sqlSelect, getRowIdentColumnTypes(), 0, rowsToRetrieve, IDataServer.CUSTOM_QUERY);
		}
		catch (RemoteException e)
		{
			clear();
			throw new RepositoryException(e);
		}

		if (pk_data.getRowCount() > 0 && pk_data.getColumnCount() != sheet.getPKIndexes().length)
			throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.query.error.incorrectNumberOfPKS")); //$NON-NLS-1$

		IFoundSetChanges changes = pksAndRecords.setPksAndQuery(pk_data, pk_data.getRowCount(), sqlSelect);
		clearInternalState(true);

		int newSize = getSize();
		fireDifference(sizeBefore, newSize, changes);

		// try to preserve selection after load by query; if not possible select first record
		trySelectingPks(selectedPKs, newSize, true);

		return true;
	}

	private ColumnType[] getRowIdentColumnTypes()
	{
		return getTable().getRowIdentColumns().stream().map(Column::getColumnType).toArray(ColumnType[]::new);
	}

	public boolean loadByQuery(String query, Object[] args) throws ServoyException
	{
		if (query == null || sheet.getTable() == null) return false;

		int from_index = -1;
		int order_by_index;

		//check requirements
		if (!SQLGenerator.isSelectQuery(query))
		{
			throw new IllegalArgumentException(SQLGenerator.SQL_QUERY_VALIDATION_MESSAGE + ':' + query);
		}
		String sql_lowercase = Utils.toEnglishLocaleLowerCase(query);

		order_by_index = sql_lowercase.lastIndexOf("order by"); //$NON-NLS-1$
		boolean analyse_query_parts = (order_by_index != -1);
		if (analyse_query_parts)
		{
			// if the query cannot be parsed according to the old methods, we just use the entire sql as
			// subquery. NOTE: this means that the ordering defined in the order-by part is lost.
			if (((from_index = sql_lowercase.indexOf("from")) == -1) //$NON-NLS-1$
				|| (sql_lowercase.indexOf(Utils.toEnglishLocaleLowerCase(sheet.getTable().getSQLName())) == -1) || (sql_lowercase.indexOf("group by") != -1) //$NON-NLS-1$
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
			fsm.getApplication().reportJSError("couldn't load dataset because foundset had edited records but couldn't save it: " + this + //$NON-NLS-1$
				", edited record(s): " + Utils.stringJoin(getFoundSetManager().getEditRecordList().getEditedRecords(this), '.') + ", failed record(s): " +
				Utils.stringJoin(getFoundSetManager().getEditRecordList().getFailedRecords(this), '.'), null);
			return false;
		}

		QuerySelect originalQuery = pksAndRecords.getQuerySelectForReading();

		QuerySelect sqlSelect = deepClone(creationSqlSelect);
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
				throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { getTable().getName() }).setContext(
					this.toString());
			}

			List<QueryColumn> pkQueryColumns = new ArrayList<QueryColumn>();
			while (pkIt.hasNext())
			{
				Column c = pkIt.next();
				pkQueryColumns.add(c.queryColumn(sqlSelect.getTable()));
			}

			// must strip of the order-by part because not all databases (Oracle, who else) like order-by in subselect
			String customQuery = query;
			if (order_by_index > 0)
			{
				// query contains order-by clause, find the next a closing bracket if it exists.
				// order-by has to be removed because some dbs do not allow that inside subselect.
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
				// if level is 0, then the order-by clause was within a sub-part of the query and not at the end,
				// in that case we can leave the ordering in place because it it not the sorting of the top-level query.
				if (level == 1)
				{
					// order-by clause was at the end
					customQuery = query.substring(0, order_by_index);
				}
			}
			sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, new SetCondition(IBaseSQLCondition.IN_OPERATOR,
				pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]), new QueryCustomSelect(customQuery, whereArgs), true));

			// set the previous sort, add all joins that are needed for this sort
			List<IQuerySort> origSorts = originalQuery.getSorts();
			if (origSorts != null)
			{
				ArrayList<IQuerySort> sorts = new ArrayList<IQuerySort>();

				// find which sorts we will use and which tables are needed for that
				Set<BaseQueryTable> sortTables = new HashSet<BaseQueryTable>();
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
				List<BaseQueryTable> tablesToResolve = new ArrayList<BaseQueryTable>();
				tablesToResolve.addAll(sortTables);

				List<BaseQueryTable> resolvedTables = new ArrayList<BaseQueryTable>();
				resolvedTables.add(sqlSelect.getTable());

				ArrayList<ISQLJoin> requiredJoins = new ArrayList<ISQLJoin>();

				boolean found = true;
				while (found && tablesToResolve.size() > 0)
				{
					BaseQueryTable table = tablesToResolve.remove(0);
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
							if (!found && ijoin instanceof ISQLTableJoin)
							{
								ISQLTableJoin join = (ISQLTableJoin)ijoin;
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
			sqlSelect.setDistinct(false); // not needed when you have no joins and may conflict with order by

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
				sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, new CustomCondition(query.substring(where_index + 5, order_by_index).trim(), whereArgs));
			}

			// pick the foundset main table from the tables in the query (does not have to be the first one, we generate sql ourselves
			// that puts the main table at the end, see QueryGenerator)
			boolean foundTable = false;
			String mainTable = sheet.getTable().getName();
			StringBuilder otherTables = new StringBuilder();
			StringTokenizer tok = new StringTokenizer(tables, ","); //$NON-NLS-1$
			String mainTableAlias = mainTable; // default alias to table name
			String whitespace = "\\s+"; //$NON-NLS-1$
			while (tok.hasMoreElements())
			{
				String tableName = tok.nextToken().trim();
				String[] lcTableName = tableName.toLowerCase().split(whitespace);
				if (matchesMainTablename(lcTableName[0]))
				{
					foundTable = true;
					// either 'tabname', 'tabname aliasname' or 'tabname AS aliasname', when no alias is given, use table name as alias
					mainTableAlias = tableName.split(whitespace)[lcTableName.length - 1];
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
			BaseQueryTable qTable = sqlSelect.getTable();
			sqlSelect.relinkTable(sqlSelect.getTable(),
				new QueryTable(qTable.getName(), qTable.getDataSource(), qTable.getCatalogName(), qTable.getSchemaName(), mainTableAlias));

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

		return loadByQuery(sqlSelect);
	}

	/** Check if the tableName matches the main table.
	 *
	 * For example: "THECAT"."THESCHEMA"."THETABLE" matches thetable.
	 */
	private boolean matchesMainTablename(String tableName)
	{
		Table table = sheet.getTable();
		if (table.getName().equalsIgnoreCase(tableName) || table.getSQLName().equalsIgnoreCase(tableName))
		{
			return true;
		}

		String[] catschemtab = tableName.split("\\.");
		if (catschemtab.length == 2)
		{
			// catalogorschema.table
			String catschem = dequote(catschemtab[0]);
			String tab = dequote(catschemtab[1]);
			if ((catschem.equalsIgnoreCase(table.getCatalog()) || catschem.equalsIgnoreCase(table.getSchema())) &&
				(table.getName().equalsIgnoreCase(tab) || table.getSQLName().equalsIgnoreCase(tab)))
			{
				return true;
			}
		}
		else if (catschemtab.length == 3)
		{
			// catalog.schema.table
			String cat = dequote(catschemtab[0]);
			String schem = dequote(catschemtab[1]);
			String tab = dequote(catschemtab[2]);
			if (cat.equalsIgnoreCase(table.getCatalog()) && schem.equalsIgnoreCase(table.getSchema()) &&
				(table.getName().equalsIgnoreCase(tab) || table.getSQLName().equalsIgnoreCase(tab)))
			{
				return true;
			}
		}

		// no match
		return false;
	}

	/**
	 * @param string
	 * @return
	 */
	private static String dequote(String name)
	{
		if (name.length() > 2 && isquote(name.charAt(0)) && isquote(name.charAt(name.length() - 1)))
		{
			return name.substring(1, name.length() - 1);
		}
		return name;
	}

	private static boolean isquote(char c)
	{
		return c == '"' || c == '\'';
	}

	public boolean loadExternalPKList(IDataSet ds) throws ServoyException
	{
		return loadExternalPKList(ds, true);
	}

	public boolean loadExternalPKList(IDataSet ds, boolean dropSort) throws ServoyException
	{
		if (sheet.getTable() == null)
		{
			fsm.getApplication().reportJSError("couldn't load dataset on a foundset that has no table", null); //$NON-NLS-1$
			return false;
		}

		if (disposed)
		{
			fsm.getApplication().reportJSError("couldn't load dataset on a disposed foundset", null); //$NON-NLS-1$
			return false;
		}

		if (initialized && (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED))
		{
			fsm.getApplication().reportJSError("couldn't load dataset because foundset had edited records but couldn't save it: " + this + //$NON-NLS-1$
				", edited record(s): " + Utils.stringJoin(getFoundSetManager().getEditRecordList().getEditedRecords(this), '.') + ", failed record(s): " +
				Utils.stringJoin(getFoundSetManager().getEditRecordList().getFailedRecords(this), '.'), null);
			return false;
		}

		IDataSet pks = pksAndRecords.getPks();

		Object[][] selectedPKs = getSelectedPksForKeepingSelection(pks, fsm.isAlwaysFollowPkSelection());

		int sizeBefore = getSize();
		if (sizeBefore > 1)
		{
			fireSelectionAdjusting();
		}

		IDataSet set;
		if (ds != null && ds.getRowCount() > 0)
		{
			List<Column> pkColumns = sheet.getTable().getRowIdentColumns();

			if (ds.getColumnCount() < pkColumns.size())
			{
				throw new RuntimeException("Dataset column count (" + ds.getColumnCount() + ") does not match table pk size (" + pkColumns.size() + ')'); //$NON-NLS-1$ //$NON-NLS-2$
			}

			Set<String> pkhashes = new HashSet<String>(ds.getRowCount());
			List<Object[]> pkRows = new ArrayList<Object[]>(ds.getRowCount());
			for (int i = 0; i < ds.getRowCount(); i++)
			{
				Object[] row = ds.getRow(i);
				Object[] pkrow = new Object[pkColumns.size()];
				for (int j = 0; j < pkColumns.size(); j++)
				{
					pkrow[j] = pkColumns.get(j).getAsRightType(row[j], true, false);
				}
				if (pkhashes.add(createPKHashKey(pkrow))) // check for duplicate pks
				{
					pkRows.add(pkrow);
				}
			}

			set = new BufferedDataSet(null, pkRows);
		}
		else
		{
			set = new BufferedDataSet(null);
		}

		// Load with pk set: remove all conditions (except foundset filters) and set pks as search-condition
		QuerySelect sqlSelect = deepClone(creationSqlSelect);

		// Set a dynamic pk condition, when pks are added, these are added to the condition automatically.
		sqlSelect.setCondition(SQLGenerator.CONDITION_SEARCH, SQLGenerator.createDynamicPKSetConditionForFoundset(this, sqlSelect.getTable(), set));

		//not possible to keep related, can limit the just supplied pkset, which would awkward
		fsm.getSQLGenerator().addSorts(sqlSelect, sqlSelect.getTable(), this, sheet.getTable(), lastSortColumns, false, false);
		clearOmit(sqlSelect);
		int sizeAfter = set.getRowCount();
		IFoundSetChanges changes = pksAndRecords.setPksAndQuery(set, sizeAfter, sqlSelect);
		clearInternalState(true);

		if (((fsm.config.verifyPKDatasetAgainstTableFilters() && fsm.getTableFilterParams(sheet.getServerName(), sqlSelect) != null) ||
			sqlSelect.getCondition(SQLGenerator.CONDITION_FILTER) != null) && set.getRowCount() > 0)
		{
			fireDifference(sizeBefore, sizeAfter, changes);
			refreshFromDBInternal(null, dropSort, set.getRowCount(), true, false); // some PKs in the set may not be valid for the current filters
		}
		else
		{
			initialized = true;

			if (pksAndRecords.getPks().getRowCount() > 0) getRecord(0);

			fireDifference(sizeBefore, sizeAfter, changes);

			// try to preserve selection after load pk list; if not possible select first record
			trySelectingPks(selectedPKs, getSize(), true);
		}
		return true;
	}

	/**
	 * Get a reference to the current sql. NOTE: this is not a copy, make no change to the query.
	 */
	@Override
	public QuerySelect getQuerySelectForReading()
	{
		return pksAndRecords.getQuerySelectForReading();
	}

	public QuerySelect getCreationSqlSelect()
	{
		return creationSqlSelect;
	}

	public void queryForAllPKs()
	{
		PksAndRecordsHolder pksAndRecordsCopy;
		int rowCount;
		synchronized (pksAndRecords)
		{
			pksAndRecordsCopy = pksAndRecords.shallowCopy();
			IDataSet pks = pksAndRecordsCopy.getPks();
			rowCount = pks == null ? 0 : pks.getRowCount();
		}
		queryForMorePKs(pksAndRecordsCopy, rowCount, -1, true);
	}

	/**
	 * Fill the pks from pksAndRecordsCopy starting at originalPKRowcount.
	 *
	 * @return the number of records added
	 */
	protected int queryForMorePKs(PksAndRecordsHolder pksAndRecordsCopy, int originalPKRowcount, int maxResult, boolean fireChanges)
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
			if (pks != null && dbIndexLastPk > 0 && originalPKRowcount > 0)
			{
				correctedMaxResult = maxResult > 0 ? (maxResult + dbIndexLastPk - originalPKRowcount) : maxResult;
				lastPkHash = createPKHashKey(pks.getRow(originalPKRowcount - 1));
				// re-query the last pk
				startRow = dbIndexLastPk - 1;
			}
			else
			{
				correctedMaxResult = maxResult;
				startRow = originalPKRowcount;
				lastPkHash = null;
			}
			int size = getSize();
			long time = System.currentTimeMillis();
			IDataSet newpks = performQuery(transaction_id, sqlSelect, getRowIdentColumnTypes(), startRow, correctedMaxResult, IDataServer.FOUNDSET_LOAD_QUERY);

			if (Debug.tracing())
			{
				Debug.trace("Query for PKs, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + " SQL: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					sqlSelect.toString());
			}

			int offset = 0;
			if (lastPkHash != null)
			{
				for (int i = 0; offset == 0 && i < newpks.getRowCount(); i++)
				{
					if (lastPkHash.equals(createPKHashKey(newpks.getRow(i))))
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
					Debug.warn("Could not connect next foundset chunk (" + startRow + "," + correctedMaxResult + "), re-loading entire PK set of datasource: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						getDataSource());
					pks.createPKCache(); // out-of-sync detected, this also flags that new PKS need to be matched against existing ones
					startRow = 0;
					time = System.currentTimeMillis();
					newpks = performQuery(transaction_id, sqlSelect, getRowIdentColumnTypes(), startRow, correctedMaxResult, IDataServer.FOUNDSET_LOAD_QUERY);

					if (Debug.tracing())
					{
						Debug.trace("RE-query for PKs, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + //$NON-NLS-1$//$NON-NLS-2$
							" SQL: " + sqlSelect.toString()); //$NON-NLS-1$
					}
				}
			}
			if (pks != null)
			{
				synchronized (pks)
				{
					int addIndex = originalPKRowcount;
					for (int i = offset; i < newpks.getRowCount(); i++)
					{
						// check for duplicates
						Object[] newpk = newpks.getRow(i);
						if (!pks.hasPKCache() /* only check for duplicates if foundset could not be connected */ || !pks.containsPk(newpk))
						{
							pks.setRow(addIndex++, newpk, false);
							dbIndexLastPk = startRow + 1 + i; // keep index in db of last added pk to correct maxresult in next chunk
						}
					}

					if (!newpks.hadMoreRows())
					{
						pks.clearHadMoreRows();
					}
				}
			}
			pksAndRecordsCopy.setDbIndexLastPk(dbIndexLastPk);

			int newSize = getCorrectedSizeForFires();
			if (fireChanges && newpks.getRowCount() != 0)
			{
				fireFoundSetEvent(size, newSize, FoundSetEvent.CHANGE_INSERT);
			}
			return newSize - size;
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
			creationSqlSelect = deepClone(sqlSelect);
		}
		IFoundSetChanges changes = null;
		//cache pks
		String transaction_id = fsm.getTransactionID(sheet);
		long time = System.currentTimeMillis();
		try
		{
			IDataSet pks = performQuery(transaction_id, sqlSelect, getRowIdentColumnTypes(), 0, fsm.config.pkChunkSize(), IDataServer.FOUNDSET_LOAD_QUERY);

			changes = pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), sqlSelect);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		if (Debug.tracing())
		{
			Debug.trace(
				"BrowseAll time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + ", SQL: " + sqlSelect.toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		initialized = true;
		omittedPKs = null;
		clearInternalState(true);

		//let the List know the model changed
		fireDifference(oldSize, getSize(), changes);
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
			// should never happen when not related to printing; when printing/closing print preview, this does happen
			Debug.log("Flushing foundset with no selection (after printing?): " + this); //$NON-NLS-1$
			return; // safety
		}

		synchronized (pksAndRecords)
		{
			SafeArrayList<IRecordInternal> cachedRecords = pksAndRecords.getCachedRecords();
			for (int i = cachedRecords.size() - 1; i >= 0; i--)
			{
				if (row >= 0 && i < row + fsm.config.chunkSize())//leave every thing close to the selection
				{
					i = row - fsm.config.chunkSize();
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
		return getRecord(row, fsm.config.chunkSize());
	}

	private IRecordInternal getRecord(int row, int sizeHint)
	{
		if (getSize() == 0 || row < 0) return null;

		PksAndRecordsHolder pksAndRecordsCopy;
		IDataSet pks;
		int rowCount;
		boolean hadMoreRows;
		synchronized (pksAndRecords)
		{
			pksAndRecordsCopy = pksAndRecords.shallowCopy();
			pks = pksAndRecordsCopy.getPks();

			if (pks == null) return null;
			rowCount = pks.getRowCount();
			hadMoreRows = pks.hadMoreRows();
		}

		if (row >= rowCount - 1)
		{
			int hint = ((row / fsm.config.pkChunkSize()) + 2) * fsm.config.pkChunkSize();
			int nQueried = 0;
			if (hadMoreRows)
			{
				nQueried = queryForMorePKs(pksAndRecordsCopy, rowCount, hint, true);
			}

			String dataSource = getDataSource();
			String inmemDataSourceName = DataSourceUtils.getInmemDataSourceName(dataSource);
			if (nQueried <= 0 //
				&& inmemDataSourceName != null //
				&& nextChunkLoadState != MethodCallState.DONE // When all chunks are loaded switch to queries (for example after sort)
				&& fsm.hasFoundsetTrigger(dataSource, PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID))
			{
				// Inmem datasource with an OnNextChunkMethod
				int total = 0;
				try
				{
					int newPksCount;
					do
					{
						newPksCount = loadNextChunkFromMethod(inmemDataSourceName, hint - rowCount - total);
						total += newPksCount;
					}
					while (newPksCount > 0 && row >= rowCount + total - 1);
				}
				catch (ServoyException e)
				{
					fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.loadingRecord"), e);
				}

				if (total > 0)
				{
					fireFoundSetEvent(rowCount, rowCount + total - 1, FoundSetEvent.CHANGE_INSERT);
				}
			}
		}

		IRecordInternal state = pksAndRecordsCopy.getCachedRecords().get(row);
		if (state == null && !findMode)
		{
			state = createRecord(row, sizeHint, pks, pksAndRecordsCopy.getCachedRecords());
		}
		// if state is still null (invalid pk?) then return prototype state
		// so that in scripting and in ui everything does format (and global relations are able display)
		if (state == null && row < Math.max(rowCount, getSize()))
		{
			state = getPrototypeState();
		}
		return state;
	}

	/**
	 * Call the OnFoundsetNextChunkMethod.
	 */
	private int loadNextChunkFromMethod(String inmemDataSourceName, int numberRequested) throws ServoyException
	{
		if (nextChunkLoadState == MethodCallState.READY)
		{
			nextChunkLoadState = MethodCallState.CALLING;
			try
			{
				Object nextChunk = executeFoundsetTriggerReturnFirst(new Object[] { inmemDataSourceName, Integer.valueOf(numberRequested) },
					PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID, true);

				if (nextChunk instanceof IDataSet && ((IDataSet)nextChunk).getRowCount() > 0)
				{
					IDataSet nextChunkDataset = (IDataSet)nextChunk;

					List<Object[]> newPks = null;
					int[] pkIndexesInDataset;
					boolean hasIdentityColumn = getTable().getRowIdentColumns().stream().mapToInt(Column::getSequenceType)
						.anyMatch(sequenceType -> sequenceType == DATABASE_IDENTITY);
					if (hasIdentityColumn)
					{
						// we assume that he identity column is the single pk.
						// this may be the internal hidden identity column (_sv_id) or a user-defined one.
						pkIndexesInDataset = new int[] { 0 };
					}
					else
					{
						// extract pks from dataset and add them to the foundset pks
						String[] pkColumnnames = stream(getTable().getRowIdentColumnNames()).toArray(String[]::new);
						List<String> pkColumnnamesList = asList(pkColumnnames);

						pkIndexesInDataset = stream(nextChunkDataset.getColumnNames())
							.mapToInt(pkColumnnamesList::indexOf).filter(idx -> idx >= 0)
							.toArray();

						if (pkIndexesInDataset.length != pkColumnnames.length)
						{
							List<String> missingPks = stream(pkColumnnamesList).filter(pkName -> !asList(nextChunkDataset.getColumnNames()).contains(pkName))
								.collect(toList());
							if (!missingPks.isEmpty())
							{
								throw new IllegalArgumentException(
									"onFoundsetNextChunkMethod: Not all pk columns are in the dataset, missing dataproviders: " + missingPks);
							}
						}

						newPks = nextChunkDataset.getRows();
					}

					Object[] generatedPks = fsm.insertToDataSource(inmemDataSourceName, nextChunkDataset, null, null, false, true);

					if (hasIdentityColumn && generatedPks != null)
					{
						// dbidentity values for inserted records
						newPks = stream(generatedPks).map(o -> new Object[] { o }).collect(toList());
					}

					synchronized (pksAndRecords)
					{
						PKDataSet pks = pksAndRecords.getPks();

						// get the pkvalues from the nextChunkDataset based on pkIndexesInDataset
						newPks.stream()
							.map(r -> stream(pkIndexesInDataset).mapToObj(idx -> r[idx]).toArray())
							.forEach(newpk -> pks.setRow(pks.getRowCount(), newpk, false));
					}

					return newPks.size();
				}
				else
				{
					nextChunkLoadState = MethodCallState.DONE;
				}
			}
			finally
			{
				// In case of exception, try again next time
				if (nextChunkLoadState != MethodCallState.DONE)
				{
					nextChunkLoadState = MethodCallState.READY;
				}
			}
		}

		return 0;
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

	@Override
	public Iterator<IRecord> iterator()
	{
		return new FoundSetIterator();
	}

	/**
	 * Delete record with the given index.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord(4);
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @param index The index of the record to delete.
	 *
	 * @return boolean true if record could be deleted.
	 */
	public boolean js_deleteRecord(Number index) throws ServoyException
	{
		if (index == null) return false;
		checkInitialized();
		return deleteRecord(new int[] { index.intValue() - 1 });
	}

	/**
	 * Delete record from foundset.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord(rec);
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @param record The record to delete from the foundset.
	 *
	 * @return boolean true if record could be deleted.
	 */
	public boolean js_deleteRecord(IJSRecord record) throws ServoyException
	{
		checkInitialized();

		deleteRecord((IRecordInternal)record);
		return true;
	}

	/**
	 * Delete currently selected record(s).
	 * If the foundset is in multiselect mode, all selected records are deleted.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord();
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @return boolean true if all records could be deleted.
	 */
	public boolean js_deleteRecord() throws ServoyException
	{
		checkInitialized();
		return deleteRecord(getSelectedIndexes());
	}

	private boolean deleteRecord(int[] deleteRecIdx) throws ServoyException
	{
		boolean success = true;
		for (int i = deleteRecIdx.length - 1; i > -1; i--)
		{
			if (deleteRecIdx[i] >= 0 && deleteRecIdx[i] < getSize()) deleteRecord(deleteRecIdx[i]);
			else success = false;
		}
		return success;
	}

	/**
	 * Omit record under the given index (add it to omit records list), to be shown with loadOmittedRecords. If index is null it behaves just like omitRecord().
	 * This operation returns false when index is invalid (should be between 1 and foundset size) or foundset is in bad state (its table not accessible)
	 * or the record is in an edit state and can't be saved (autosave is false). Any retrievable record can be ommitted.
	 *
	 * Note: The omitted records list is discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invertRecords()
	 *
	 * @sampleas js_omitRecord()
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadOmittedRecords()
	 *
	 * @param index The index of the record to omit, starting with 1 .
	 *
	 * @return boolean true if all records could be omitted.
	 */
	public boolean js_omitRecord(Number index)
	{
		int _index;
		if (index == null)
		{
			return isInitialized() && omitState(getSelectedIndexes());
		}
		else
		{
			_index = index.intValue();
			return isInitialized() && omitState(new int[] { _index - 1 });
		}
	}

	/**
	 * Omit selected record(s) (add it to omit records list), to be shown with loadOmittedRecords.
	 * This operation returns false only when foundset is in bad state (table not accessible or not having a valid selected record)
	 * or the record is in an edit state and can't be saved (autosave is false).
	 *
	 * Note: The omitted records list is discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invertRecords()
	 *
	 * @sample var success = %%prefix%%foundset.omitRecord();
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadOmittedRecords()
	 *
	 * @return boolean true if all selected record(s) could be omitted.
	 */
	public boolean js_omitRecord()
	{
		return isInitialized() && omitState(getSelectedIndexes());
	}

	/**
	 * Omit record sent as parameter (add it to omit records list), to be shown with loadOmittedRecords.
	 * This operation returns false only when foundset is in bad state (table not accessible or not having a valid selected record)
	 * or the record is in an edit state and can't be saved (autosave is false) or record not present in foundset.
	 *
	 * Note: The omitted records list is discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invertRecords()
	 *
	 * @sample var success = %%prefix%%foundset.omitRecord(record);
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadOmittedRecords()
	 *
	 * @return boolean true if record could be omitted.
	 */
	@JSFunction
	public boolean omitRecord(IJSRecord record)
	{
		if (isInitialized())
		{
			int _index = getRecordIndex((IRecord)record);
			if (_index >= 0)
			{
				return omitState(new int[] { _index });
			}
		}
		return false;
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
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_sort(String)
	 *
	 * @return String sort columns
	 */
	public String js_getCurrentSort()
	{
		return FoundSetManager.getSortColumnsAsString(lastSortColumns);
	}

	/**
	 * Sorts the foundset based on the given sort string.
	 * Tries to preserve selection based on primary key. If first record is selected or cannot select old record it will select first record after sort.
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order.
	 *
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 */
	public void js_sort(String sortString) throws ServoyException
	{
		js_sort(sortString, Boolean.FALSE);
	}

	/**
	 * Sorts the foundset based on the given sort string.
	 * Tries to preserve selection based on primary key. If first record is selected or cannot select old record it will select first record after sort.
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order.
	 *
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 * @param defer boolean when true, the "sortString" will be just stored, without performing a query on the database (the actual sorting will be deferred until the next data loading action).
	 */
	public void js_sort(String sortString, Boolean defer) throws ServoyException
	{
		sort(((FoundSetManager)getFoundSetManager()).getSortColumns(getTable(), sortString), getBooleanAsbool(defer, false));
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
			final IExecutingEnviroment scriptEngine = fsm.getApplication().getScriptEngine();
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
	 * Duplicate record at index in the foundset, change selection to new record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param onTop when true the new record is added as the topmost record.
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Number index, Boolean onTop) throws ServoyException
	{
		return js_duplicateRecord(index, onTop, Boolean.TRUE);
	}

	/**
	 * Duplicate record at index in the foundset, change selection to new record, place on top.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Number index) throws ServoyException
	{
		return js_duplicateRecord(index, Boolean.TRUE, Boolean.TRUE);
	}

	/**
	 * Duplicate selected record, change selection to new record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @param onTop when true the new record is added as the topmost record.
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Boolean onTop) throws ServoyException
	{
		return js_duplicateRecord(Integer.valueOf(getSelectedIndex() + 1), onTop, Boolean.TRUE);
	}

	/**
	 * Duplicate selected record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @param onTop when true the new record is added as the topmost record.
	 * @param changeSelection when true the selection is changed to the duplicated record.
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Boolean onTop, Boolean changeSelection) throws ServoyException
	{
		return js_duplicateRecord(Integer.valueOf(getSelectedIndex() + 1), onTop, changeSelection);
	}

	/**
	 * Duplicate current record, change selection to new record, place on top.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord() throws ServoyException
	{
		return js_duplicateRecord(Integer.valueOf(getSelectedIndex() + 1), Boolean.TRUE, Boolean.TRUE);
	}

	/**
	 * Duplicate record at index in the foundset.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param onTop when true the new record is added as the topmost record.
	 * @param changeSelection when true the selection is changed to the duplicated record.
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Number index, Boolean onTop, Boolean changeSelection) throws ServoyException
	{
		int _index = getNumberAsInt(index, getSelectedIndex());
		boolean _onTop = getBooleanAsbool(onTop, true);
		boolean _changeSelection = getBooleanAsbool(changeSelection, true);
		return duplicateRecord(_index - 1, _onTop ? 0 : Integer.MAX_VALUE, _changeSelection) + 1;
	}

	/**
	 * Duplicate record at index in the foundset, change selection to new record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 *
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param location the new record is added at specified index
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Number index, Number location) throws ServoyException
	{
		return js_duplicateRecord(index, location, Boolean.TRUE);
	}

	/**
	 * Duplicate record at index in the foundset.
	 *
	 * @sample
	 * %%prefix%%foundset.duplicateRecord();
	 * %%prefix%%foundset.duplicateRecord(false); //duplicate the current record, adds at bottom
	 * %%prefix%%foundset.duplicateRecord(1,2); //duplicate the first record as second record
	 * //duplicates the record (record index 3), adds on top and selects the record
	 * %%prefix%%foundset.duplicateRecord(3,true,true);
	 *
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param location the new record is added at specified index
	 * @param changeSelection when true the selection is changed to the duplicated record.
	 *
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public int js_duplicateRecord(Number index, Number location, Boolean changeSelection) throws ServoyException
	{
		int _index = getNumberAsInt(index, getSelectedIndex());
		int _location = getNumberAsInt(location, 1);
		boolean _changeSelection = getBooleanAsbool(changeSelection, true);
		return duplicateRecord(_index - 1, _location - 1, _changeSelection) + 1;
	}

	/**
	 * Create a new record in the foundset and change selection to it. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param index the new record is added at specified index.
	 *
	 * @return int index of new record.
	 */
	public int js_newRecord(Number index) throws Exception
	{
		return js_newRecord(index, Boolean.TRUE);
	}

	/**
	 * Create a new record in the foundset. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param index the new record is added at specified index.
	 * @param changeSelection boolean when true the selection is changed to the new record.
	 *
	 * @return int index of new record.
	 */
	public int js_newRecord(Number index, Boolean changeSelection) throws Exception
	{
		int _index = getNumberAsInt(index, 1);
		boolean _changeSelection = getBooleanAsbool(changeSelection, true);
		if (_index > 0)
		{
			return newRecord(null, _index - 1, _changeSelection) + 1;//javascript index is plus one
		}
		return -1;
	}

	/**
	 * Create a new record in the foundset and change selection to it. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param onTop when true the new record is added as the topmost record.
	 *
	 * @return int index of new record.
	 */
	public int js_newRecord(Boolean onTop) throws ServoyException
	{
		return js_newRecord(onTop, Boolean.TRUE);
	}

	/**
	 * Create a new record in the foundset. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param onTop when true the new record is added as the topmost record; when false
	 * the record is added to the end, if all records are loaded, otherwise it will be added to the top
	 * @param changeSelection boolean when true the selection is changed to the new record.
	 *
	 * @return int index of new record.
	 */
	public int js_newRecord(Boolean onTop, Boolean changeSelection) throws ServoyException
	{
		boolean _onTop = getBooleanAsbool(onTop, true);
		boolean _changeSelection = getBooleanAsbool(changeSelection, true);
		return newRecord(null, _onTop ? 0 : Integer.MAX_VALUE, _changeSelection) + 1;//javascript index is plus one
	}

	/**
	 * Create a new record on top of the foundset and change selection to it. Returns -1 if the record can't be made.
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
	 * @return int index of new record.
	 */
	public int js_newRecord() throws Exception
	{
		return js_newRecord(Integer.valueOf(1), Boolean.TRUE);
	}

	/**
	 * Create a new record in the foundset and change selection to it at specified index. Returns the new record or null if the record can't be made.
	 *
	 * @sample
	 * var rec = %%prefix%%foundset.createRecord(1); // add as first record, do change selection
	 *
	 * @param index the new record is added at specified index (1-based).
	 *
	 * @return IJSRecord of new record.
	 */
	@JSFunction
	public IJSRecord createRecord(Number index) throws Exception
	{
		return createRecord(index, Boolean.TRUE);
	}

	/**
	 * Create a new record in the foundset. Returns the new record or null if the record can't be made.
	 *
	 * @sample
	 *  var rec = %%prefix%%foundset.createRecord(1, false); // add as first record, do not change selection
	 *
	 * @param index the new record is added at specified index (1-based).
	 * @param changeSelection boolean when true the selection is changed to the new record.
	 *
	 * @return  IJSRecord of new record.
	 */
	@JSFunction
	public IJSRecord createRecord(Number index, Boolean changeSelection) throws Exception
	{
		int _index = getNumberAsInt(index, 1);
		boolean _changeSelection = getBooleanAsbool(changeSelection, true);
		if (_index > 0)
		{
			return (IJSRecord)getRecord(newRecord(null, _index - 1, _changeSelection));
		}
		return null;
	}

	/**
	 * Create a new record in the foundset. Returns the new record or null if the record can't be made.
	 *
	 * @sample
	 * var rec = %%prefix%%foundset.createRecord(false); // add as last record, do change selection
	 *
	 * @param onTop when true the new record is added as the topmost record.
	 *
	 * @return IJSRecord of new record.
	 */
	@JSFunction
	public IJSRecord createRecord(Boolean onTop) throws ServoyException
	{
		return createRecord(onTop, Boolean.TRUE);
	}

	/**
	 * Create a new record in the foundset. Returns the new record or null if the record can't be made.
	 *
	 * @sample
	 * var rec = %%prefix%%foundset.createRecord(false, false); // add as last record, do not change selection
	 *
	 * @param onTop when true the new record is added as the topmost record; when false
	 * the record is added to the end, if all records are loaded, otherwise it will be added to the top
	 * @param changeSelection boolean when true the selection is changed to the new record.
	 *
	 * @return  IJSRecord of new record.
	 */
	@JSFunction
	public IJSRecord createRecord(Boolean onTop, Boolean changeSelection) throws ServoyException
	{
		boolean _onTop = getBooleanAsbool(onTop, true);
		boolean _changeSelection = getBooleanAsbool(changeSelection, true);
		return (IJSRecord)getRecord(newRecord(null, _onTop ? 0 : Integer.MAX_VALUE, _changeSelection));//javascript index is plus one
	}

	/**
	 * Create a new record on top of the foundset and change selection to it. Returns the new record or null if record was not created.
	 *
	 * @sample
	 * var rec = %%prefix%%foundset.createRecord(); // add as first record
	 *
	 * @return IJSRecord the new record
	 */
	@JSFunction
	public IJSRecord createRecord() throws Exception
	{
		return createRecord(Integer.valueOf(1), Boolean.TRUE);
	}

	@Override
	public int jsFunction_getSelectedIndex()
	{
		checkSelection();
		return IFoundSetScriptMethods.super.jsFunction_getSelectedIndex();
	}

	@Override
	public void jsFunction_setSelectedIndex(int index)
	{
		IFoundSetScriptMethods.super.jsFunction_setSelectedIndex(index);
	}

	@Override
	public Number[] jsFunction_getSelectedIndexes()
	{
		checkSelection();
		return IFoundSetScriptMethods.super.jsFunction_getSelectedIndexes();
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
	 * @deprecated As of release 3.1, replaced by {@link #getSize()}.
	 */
	@Deprecated
	public int js_getMaxRecordIndex()
	{
		return getSize();
	}

	/**
	 * Get the record object at the given index.
	 * Argument "index" is 1 based (so first record is 1).
	 *
	 * @sample var record = %%prefix%%foundset.getRecord(index);
	 *
	 * @param index int record index (1 based).
	 *
	 * @return Record record.
	 */
	public IJSRecord js_getRecord(int index)
	{
		return (IJSRecord)getRecord(index - 1); // index is row + 1, so we substract 1 here.
	}

	/**
	 * Get the loaded record based on its  the primary key values
	 *
	 * This will only return a record that is already loaded in this foundset, this will not try to load anything from the database.
	 *
	 * @sample var record = %%prefix%%foundset.getRecordByPk(1); // or getRecordByPk(1,2) or ([1,2]) for multicolumn pk
	 *
	 * @param pk pk values as array
	 *
	 * @return Record record.
	 */
	public IJSRecord js_getRecordByPk(Object... pk)
	{
		return (IJSRecord)getRecord(pk);
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
	public int js_getRecordIndex(IJSRecord record)
	{
		int recordIndex = getRecordIndex((IRecord)record);
		if (recordIndex == -1) return -1;
		return recordIndex + 1;
	}

	/**
	 * Get the selected record.
	 *
	 * @sample var selectedRecord = %%prefix%%foundset.getSelectedRecord();
	 * @return Record record.
	 */
	@JSFunction
	public IJSRecord getSelectedRecord()
	{
		checkSelection();
		IJSRecord record = (IJSRecord)getRecord(getSelectedIndex());
		return record == getPrototypeState() ? null : record; // safety, do not return proto
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
		checkSelection();
		int[] selectedIndexes = getSelectedIndexes();
		List<IRecordInternal> selectedRecords = new ArrayList<IRecordInternal>(selectedIndexes.length);
		for (int index : selectedIndexes)
		{
			IRecordInternal record = getRecord(index);
			if (record != null && record != getPrototypeState()) // safety, do not return proto
			{
				selectedRecords.add(record);
			}
		}

		return selectedRecords.toArray(new IRecordInternal[selectedRecords.size()]);
	}

	/**
	 *
	 */
	@SuppressWarnings("nls")
	private void checkSelection()
	{
		if (getSize() > 0 && getSelectedIndex() == -1)
		{
			Debug.error("No selection set on foundset with size " + getSize() + " fs: " + this, new RuntimeException());
			setSelectedIndex(0);
		}
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

	public void setMultiSelect(boolean multiSelect)
	{
		if (multiSelectPinnedForm == null) setMultiSelectInternal(multiSelect); // if a form is currently overriding this, ignore js call
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

	/**
	 * As a guideline, only the one who pinned the multiSelect should unpin it.
	 */
	public void unpinMultiSelectIfNeeded(String formName)
	{
		if (multiSelectPinnedForm == formName)
		{
			multiSelectPinnedForm = null;
			fireSelectionModeChange(); // this allows any other forms that might be currently using this foundset to apply their own selectionMode to it
		}
	}

	protected abstract void setMultiSelectInternal(boolean isMultiSelect);

	public abstract boolean isMultiSelect();

	private int lastRecordCreatedIndex = -1;

	//do real query for state
	// is already synched by caller around the PksAndRecordsHolder instance
	private Record createRecord(int row, int sz, IDataSet pks, SafeArrayList<IRecordInternal> cachedRecords)
	{
		int a_sizeHint = (sz > fsm.config.pkChunkSize()) ? fsm.config.pkChunkSize() : sz; //safety, SQL in limit

		if (Math.abs(row - lastRecordCreatedIndex) > 30 && cachedRecords.get(row - 1) == null && cachedRecords.get(row + 1) == null)
		{
			synchronized (pksAndRecords)
			{
				removeRecords(row, false, cachedRecords);
			}
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
						startRow = row - fsm.config.chunkSize();
					}
					else
					{
						startRow = row - fsm.config.chunkSize() / 2;
					}
					startRow = Math.max(startRow, 0);
				}
			}
			else
			{
				sizeHint = ((cachedRecords.get(row + 1) != null || pks.getRowCount() == 1) ? 1 : a_sizeHint);
			}
			int oldSize = pks.getRowCount();
			List<Row> rows = rowManager.getRows(pks, startRow, sizeHint, false);
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
				rows = rowManager.getRows(pks, row, 1, false);
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
				fireDifference(oldSize, newSize, null);
			}
			synchronized (pksAndRecords)
			{
				removeRecords(row, true, cachedRecords);
			}
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
				Row data = rowManager.createNotYetExistInDBRowObject(sheet.getNewRowData(fsm.getApplication(), this), false);
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
		int cacheSize = fsm.config.chunkSize() * 3;
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
		UsedDataProviderTracker current = null;
		try
		{
			Object obj;
			TableScope tableScope = (TableScope)fsm.getScriptEngine().getTableScope(sheet.getTable());
			tableScope.setArguments(vargs);
			current = tableScope.setUsedDataProviderTracker(usedDataProviderTracker);
			Scriptable previous = tableScope.getPrototype();
			try
			{
				tableScope.setPrototype((Scriptable)state);//make sure its set correctly
				obj = tableScope.getCalculationValue(dataProviderID, tableScope);
				if (obj instanceof Byte)//fix for postgress
				{
					obj = Integer.valueOf(((Byte)obj).intValue());
				}
				else
				{
					Column column = sheet.getTable().getColumn(dataProviderID);
					if (column != null)
					{
						// stored calculation
						// TODO: check case with stored calc on column with column converter
						if (column.getScale() > 0 && column.getDataProviderType() == IColumnTypes.NUMBER && obj != null)
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
				tableScope.setUsedDataProviderTracker(current);
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
		if ("selectedIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;
		}
		if ("maxRecordIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;
		}
		if ("serverURL".equals(dataProviderID)) //$NON-NLS-1$
		{
			return true;
		}

		try
		{
			// have to test for a global prefix. because setDataProviderId does check for this.
			Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
			if (scope.getLeft() != null)
			{
				GlobalScope gs = fsm.getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft());
				return gs != null && gs.has(scope.getRight(), gs);
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
			return Integer.valueOf(getSelectedIndex() + 1);//deprecated
		}
		if ("selectedIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return Integer.valueOf(getSelectedIndex() + 1);
		}
		if ("maxRecordIndex".equals(dataProviderID) || "lazyMaxRecordIndex".equals(dataProviderID)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return Integer.valueOf(getSize());
		}
		if ("serverURL".equals(dataProviderID)) //$NON-NLS-1$
		{
			return getFoundSetManager().getApplication().getScriptEngine().getSystemConstant("serverURL"); //$NON-NLS-1$
		}

		try
		{
			if (ScopesUtils.getVariableScope(dataProviderID).getLeft() != null)
			{
				return fsm.getScopesScopeProvider().getDataProviderValue(dataProviderID);
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

			Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
			if (scope.getLeft() != null)
			{
				GlobalScope gscope = fsm.getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft());
				if (gscope != null && gscope.has(scope.getRight(), gscope))
				{
					Object oldVal = gscope.put(scope.getRight(), value);
					if (!equalObjects(oldVal, value))
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
					if (!equalObjects(currentValue, value))
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
			IDataSet ds = performQuery(transaction_id, select, null, 0, 1, IDataServer.AGGREGATE_QUERY);

			if (Debug.tracing())
			{
				Debug.trace("Aggregate query, time: " + (System.currentTimeMillis() - time) + ", thread: " + Thread.currentThread().getName() + ", SQL: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					select.toString());
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
		Relation[] relationSequence = fsm.getApplication().getFlattenedSolution().getRelationSequence(name);
		if (relationSequence != null && relationSequence.length > 0 && !relationSequence[0].isGlobal() &&
			!relationSequence[0].getPrimaryDataSource().equals(getDataSource()))
		{
			fsm.getApplication().reportJSWarning("An incorrect child relation (" + relationSequence[0].getName() +
				") was accessed through a foundset (or a record of foundset) with datasource '" + getDataSource() + "'. The accessed relation actually has '" +
				relationSequence[0].getPrimaryDataSource() +
				"' as primary datasource. It will resolve for legacy reasons but please fix it as it is error prone.", new ServoyException());
		}
		return relationSequence != null;
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
	 * for (var i = 1; i <= %%prefix%%foundset.getSize(); i++)
	 * {
	 * 	var rec = %%prefix%%foundset.getRecord(i);
	 * }
	 *
	 * @return int current size.
	 */
	@JSFunction
	public int getSize()
	{
		return getRawSize();
	}

	// get the size without the danger of firing a query
	public final int getRawSize()
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
		int cacheIndex = pksAndRecords.getCachedRecords().indexOf(record);
		if (cacheIndex >= 0 || isInFindMode())
		{
			return cacheIndex;
		}
		return record == null ? -1 : getRecordIndex(record.getPK());
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

		if (disposed)
		{
			fsm.getApplication().reportJSError("cannot delete records for a disposed foundset", null); //$NON-NLS-1$
			throw new ServoyException(ServoyException.INVALID_INPUT);
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
				throw new ApplicationException(ServoyException.NO_DELETE_ACCESS, new Object[] { table.getName() });
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

				IDataSet deletePKs;
				boolean allFoundsetRecordsLoaded = currentPKs != null && pksAndRecords.getCachedRecords().size() == getSize() && !hadMoreRows();
				if (allFoundsetRecordsLoaded)
				{
					// clone because this will be used in a separate thread by performUpdates while it will be altered in this one (deletes all records at the end of the method)
					deletePKs = currentPKs.clone();
				}
				else
				{
					deletePKs = new BufferedDataSet();
					deletePKs.addRow(new Object[] { ValueFactory.createTableFlushValue() });
				}
				String tid = fsm.getTransactionID(table.getServerName());
				SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, table.getServerName(), table.getName(), deletePKs,
					tid, delete_sql, fsm.getTableFilterParams(table.getServerName(), delete_sql));
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
					Debug.log("Could not delete all records in 1 statement (a record may be locked), trying per-record"); //$NON-NLS-1$
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
			PksAndRecordsHolder pksAndRecordsCopy;
			int rowCount;
			synchronized (pksAndRecords)
			{
				pksAndRecordsCopy = pksAndRecords.shallowCopy();
				IDataSet pks = pksAndRecordsCopy.getPks();
				rowCount = pks == null ? 0 : pks.getRowCount();
			}
			queryForMorePKs(pksAndRecordsCopy, rowCount, -1, false);
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
		deleteRecord(getRecord(row), row);
	}

	public void deleteRecord(IRecordInternal state, int row) throws ServoyException
	{
		if (state == null)
		{
			return;
		}

		((FoundSetManager)getFoundSetManager()).clearAllDeleteSets();
		try
		{
			deleteRecord(state, row, false);
		}
		finally
		{
			((FoundSetManager)getFoundSetManager()).clearAllDeleteSets();
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#deleteRecord(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	public void deleteRecord(IRecordInternal record) throws ServoyException
	{
		if (record.getParentFoundSet() != this)
			throw new ApplicationException(ServoyException.INVALID_INPUT, new RuntimeException("Record not from this foundset")); //$NON-NLS-1$
		int recordIndex = getRecordIndex(record);
		if (recordIndex == -1) throw new ApplicationException(ServoyException.INVALID_INPUT, new RuntimeException("Record pk not found in this foundset")); //$NON-NLS-1$
		deleteRecord(record, recordIndex);
	}

	// part of bigger delete == sql foundset delete is already done for this row (see deleteAll)
	private void deleteRecord(int row, boolean partOfBiggerDelete) throws ServoyException
	{
		IRecordInternal state;
		if (partOfBiggerDelete)
		{
			state = pksAndRecords.getCachedRecords().get(row);
		}
		else
		{
			state = getRecord(row);
		}

		deleteRecord(state, row, partOfBiggerDelete);
	}

	private void deleteRecord(IRecordInternal state, int row, boolean partOfBiggerDelete) throws ServoyException
	{
		if (sheet.getTable() == null)
		{
			return;
		}
		if (!hasAccess(IRepository.DELETE) && (state == null || state.existInDataSource()))
		{
			throw new ApplicationException(ServoyException.NO_DELETE_ACCESS, new Object[] { sheet.getTable().getName() });
		}

		if (state != null && !(state instanceof PrototypeState) && !findMode)
		{
			if (!fsm.getRowManager(fsm.getDataSource(sheet.getTable())).addRowToDeleteSet(state.getPKHashKey()))
			{
				// already being deleted in recursion
				return;
			}

			if (!partOfBiggerDelete)
			{
				try
				{
					// see EditRecordList.stopEditing
					if (state.existInDataSource() &&
						!executeFoundsetTriggerBreakOnFalse(new Object[] { state }, PROPERTY_ONDELETEMETHODID, true))
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


				// check for related data
				FlattenedSolution flattenedSolution = fsm.getApplication().getFlattenedSolution();
				for (Relation relation : iterate(flattenedSolution.getRelations(sheet.getTable(), true, false)))
				{
					if (Relation.isValid(relation, flattenedSolution) && !relation.getAllowParentDeleteWhenHavingRelatedRecords() &&
						!relation.isExactPKRef(flattenedSolution) && !relation.isGlobal())
					{
						boolean hasRelatedRecords;
						if (state.isRelatedFoundSetLoaded(relation.getName(), null))
						{
							IFoundSetInternal set = state.getRelatedFoundSet(relation.getName(), null);
							hasRelatedRecords = set != null && set.getSize() > 0;
						}
						else
						{
							// foundset not loaded, do an optimized query to find out if there are related records
							QuerySelect relatedQuery = fsm.getRelatedFoundSetQuery(state, relation);
							if (relatedQuery == null)
							{
								// This can happen when the relation has null columns
								if (isDebugEnabled())
								{
									Debug.debug(
										"Foundset not loaded but relation args are null, falling back to loading the relation to check existence of related records for relation '" +
											relation.getName() + "' from record '" + state.getPKHashKey() + "' in datasource '" + getDataSource() + "'");
								}
								IFoundSetInternal set = state.getRelatedFoundSet(relation.getName());
								hasRelatedRecords = set != null && set.getSize() > 0;
							}
							else
							{
								relatedQuery.setColumns(new ArrayList<IQuerySelectValue>(asList(new QueryColumnValue(Integer.valueOf(1), null, true))));

								String transaction_id = fsm.getTransactionID(sheet);
								try
								{
									// rowsToRetrieve=0, because we add 1 extra for the actual query, so we can rely on ds.hadMoreRows()
									IDataSet ds = performQuery(transaction_id, relatedQuery, getColumnTypes(IColumnTypes.INTEGER), 0, 0, RAW_QUERY);
									hasRelatedRecords = ds.hadMoreRows();
								}
								catch (RemoteException | ServoyException e)
								{
									hasRelatedRecords = true; // just to be safe
									Debug.error(e);
								}
							}
						}
						if (hasRelatedRecords)
						{
							fsm.getApplication().reportJSError(
								"Delete not granted due to AllowParentDeleteWhenHavingRelatedRecords and existing related records from record with PK: " +
									state.getPKHashKey() + " index in foundset: " + row + " blocked by relation: " + relation.getName(),
								null);
							throw new ApplicationException(ServoyException.NO_PARENT_DELETE_WITH_RELATED_RECORDS, new Object[] { relation.getName() })
								.setContext(this.toString());
						}
					}
				}

				//if this is a new record, this delete operation may be the result of a rollback due to a duplicated pk
				// so only delete related data if the record is already in the db
				if (state.existInDataSource())
				{
					// delete the related data
					for (Relation relation : iterate(flattenedSolution.getRelations(sheet.getTable(), true, false)))
					{
						if (Relation.isValid(relation, flattenedSolution) && relation.getDeleteRelatedRecords() && !relation.isGlobal())//if completely global never delete do cascade delete
						{
							IFoundSetInternal set = state.getRelatedFoundSet(relation.getName());
							if (set != null)
							{
								Debug.trace("******************************* delete related set size: " + set.getSize() + " from record with PK: " + //$NON-NLS-1$//$NON-NLS-2$
									state.getPKHashKey() + " index in foundset: " + row); //$NON-NLS-1$
								set.deleteAllInternal();
							}
						}
					}
				}
			}

			if (state.existInDataSource())
			{
				Row data = state.getRawData();
				rowManager.deleteRow(this, data, hasAccess(IRepository.TRACKING), partOfBiggerDelete);

				executeFoundsetTrigger(new Object[] { state }, PROPERTY_ONAFTERDELETEMETHODID, false);

				GlobalTransaction gt = fsm.getGlobalTransaction();
				if (gt != null)
				{
					gt.addDeletedRecord(state);
				}

				// really remove the state from the edited records, can't be saved at all anymore after delete.
				fsm.getEditRecordList().removeEditedRecord(state);
			}
			else
			{
				rowManager.clearRow(state.getRawData());
			}
		}
		if (!(state instanceof PrototypeState))
		{
			removeRecordInternalEx(state, row);
			// delete the row data so it won't be updated by other foundsets also having records to this rowdata.
			if (state != null && state.getRawData() != null)
			{
				state.getRawData().flagExistInDB();
			}

		}
	}

	/**
	 * Execute the foundset trigger for specified TableNode property.
	 * When multiple tiggers exist, stop when 1 returns false.
	 *
	 * @param args
	 * @param property TableNode property
	 * @return
	 * @throws ServoyException
	 */
	boolean executeFoundsetTriggerBreakOnFalse(Object[] args, TypedProperty<Integer> property, boolean throwException) throws ServoyException
	{
		return fsm.executeFoundsetTriggerBreakOnFalse(getTable(), args, property, throwException, this);
	}

	/**
	 * Execute the foundset trigger for specified TableNode property.
	 *
	 * @param args
	 * @param property TableNode property
	 * @return
	 * @throws ServoyException
	 */
	void executeFoundsetTrigger(Object[] args, TypedProperty<Integer> property, boolean throwException) throws ServoyException
	{
		fsm.executeFoundsetTrigger(getTable(), args, property, throwException, this);
	}

	Object executeFoundsetTriggerReturnFirst(Object[] args, TypedProperty<Integer> property, boolean throwException) throws ServoyException
	{
		return fsm.executeFoundsetTriggerReturnFirst(getTable(), args, property, throwException, this);
	}

	private boolean tableHasOnDeleteMethods()
	{
		try
		{
			FlattenedSolution solutionRoot = fsm.getApplication().getFlattenedSolution();
			Iterator<TableNode> tableNodes = solutionRoot.getTableNodes(getTable());
			List<ScriptMethod> foundsetMethods = solutionRoot.getFoundsetMethods(getTable(), false);
			while (tableNodes.hasNext())
			{
				TableNode node = tableNodes.next();
				int methodId = node.getOnDeleteMethodID();
				if (methodId > 0 && solutionRoot.getScriptMethod(methodId) != null || AbstractBase.selectById(foundsetMethods.iterator(), methodId) != null)
				{
					return true;
				}
				methodId = node.getOnAfterDeleteMethodID();
				if (methodId > 0 && solutionRoot.getScriptMethod(methodId) != null || AbstractBase.selectById(foundsetMethods.iterator(), methodId) != null)
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

		int toDelete = row;
		synchronized (pksAndRecords)
		{
			if (state != null)
			{
				// check if the index is still the right one
				IRecordInternal current = pksAndRecords.getCachedRecords().get(toDelete);
				if (current != state)
				{
					// if not try to find the to remove state.
					toDelete = pksAndRecords.getCachedRecords().indexOf(state);

					if (toDelete == -1)
					{
						// cached record was removed/cleared, find by pk
						toDelete = getRecordIndex(state.getPK());
					}
				}
			}
			pksAndRecords.getCachedRecords().remove(toDelete);
			if (!findMode)
			{
				IDataSet pks = pksAndRecords.getPks();
				if (pks != null && pks.getRowCount() > toDelete)
				{
					if (toDelete != -1) pks.removeRow(toDelete);
					int dbIndexLastPk = pksAndRecords.getDbIndexLastPk();
					if (dbIndexLastPk > 0)
					{
						pksAndRecords.setDbIndexLastPk(dbIndexLastPk - 1);
					}
				}
			}
		}

		if (getSize() == 0) setSelectedIndex(-1);

		if (toDelete >= 0) fireFoundSetEvent(toDelete, toDelete, FoundSetEvent.CHANGE_DELETE);

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
		boolean b = loadExternalPKList(omittedPKs, false);
		omittedPKs = null;
		return b;
	}

	public void invert() throws ServoyException
	{
		int sizeBefore;
		QuerySelect sqlSelect;
		ArrayList<String> invertConditionNames = new ArrayList<String>();
		IFoundSetChanges changes = null;
		synchronized (pksAndRecords)
		{
			sizeBefore = getSize();
			sqlSelect = pksAndRecords.getQuerySelectForReading();

			for (String conditionName : sqlSelect.getConditionNames())
			{
				if (conditionName != null &&
					((conditionName.equals(SQLGenerator.CONDITION_SEARCH) || conditionName.equals(SQLGenerator.CONDITION_CLEAR)) ||
						!conditionName.startsWith(SQLGenerator.SERVOY_CONDITION_PREFIX)))
				{
					invertConditionNames.add(conditionName);
				}
			}

			if (invertConditionNames.size() == 0)
			{
				changes = pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, sqlSelect);
			}
			else
			{
				sqlSelect = pksAndRecords.getQuerySelectForModification();
				for (String inverConditionName : invertConditionNames)
				{
					sqlSelect.setCondition(inverConditionName, sqlSelect.getCondition(inverConditionName).negate());
				}
				clearOmit(sqlSelect);
				// set pks here in case a refresh comes along
				changes = pksAndRecords.setPksAndQuery(pksAndRecords.getPks(), pksAndRecords.getDbIndexLastPk(), sqlSelect);
			}
		}

		if (invertConditionNames.size() > 0)
		{
			//cache pks
			String transaction_id = fsm.getTransactionID(sheet);
			try
			{
				IDataSet pks = performQuery(transaction_id, sqlSelect, getRowIdentColumnTypes(), 0, fsm.config.pkChunkSize(), IDataServer.FOUNDSET_LOAD_QUERY);

				synchronized (pksAndRecords)
				{
					// optimistic locking, if the query has been changed in the mean time forget about the refresh
					if (sqlSelect != pksAndRecords.getQuerySelectForReading())
					{
						Debug.log("invert: query was changed during refresh, not resetting old query"); //$NON-NLS-1$
						return;
					}
					changes = pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), sqlSelect);
				}
			}
			catch (RemoteException e)
			{
				changes = pksAndRecords.setPksAndQuery(new BufferedDataSet(), 0, sqlSelect);
				throw new RepositoryException(e);
			}
		}

		if (aggregateCache.size() > 0)
		{
			fireAggregateChangeWithEvents(null);
		}

		fireDifference(sizeBefore, getSize(), changes);
	}


	public boolean omitState(int[] rows)
	{
		if (sheet.getTable() == null)
		{
			getFoundSetManager().getApplication().reportJSWarning("ommit fails because of an invalid table");
			return false;
		}

		List<IRecordInternal> recordsToOmit = new ArrayList<IRecordInternal>();
		boolean success = true;
		for (int row : rows)
		{
			if (row < 0 || row >= getSize())
			{
				success = false;
				getFoundSetManager().getApplication().reportJSWarning("ommit fails because of an invalid index " + row);
				continue;
			}
			IRecordInternal state = getRecord(row);
			if (state != null && fsm.getEditRecordList().stopEditing(false, state) == ISaveConstants.STOPPED)
			{
				recordsToOmit.add(state);
			}
			else
			{
				getFoundSetManager().getApplication().reportJSWarning(
					"Record not omitted for index " + row + " because could not stop the edit on it " + state);
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
			replaceOmitCondition();

			for (IRecordInternal dsState : recordsToOmit)
			{
				removeRecordInternalEx(dsState, pksAndRecords.getCachedRecords().indexOf(dsState));
			}
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

	private int newRecord(Row rowData, int indexToAdd, boolean changeSelection) throws ServoyException
	{
		return newRecord(rowData, indexToAdd, changeSelection, true);
	}

	protected int newRecord(Row rowData, int indexToAdd, boolean changeSelection, boolean javascriptRecord) throws ServoyException
	{
		IRecordInternal newRecord = createRecord(rowData, javascriptRecord);
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

		if (changeSelection && fsm.getEditRecordList().isEditing(getRecord(getSelectedIndex())))
		{
			fsm.getEditRecordList().stopEditing(false, getRecord(getSelectedIndex()));
		}

		int size = getSize();
		int indexToAdd = (idx < 0) ? 0 : (idx > size) ? size : idx;
		if (indexToAdd == size && hadMoreRows())
		{
			Debug.trace("Cannot add new record to end of foundset because foundset is not fully loaded yet, adding at begin of foundset"); //$NON-NLS-1$
			indexToAdd = 0;
		}

		synchronized (pksAndRecords)
		{
			SafeArrayList<IRecordInternal> cachedRecords = null;
			PKDataSet pks = pksAndRecords.getPks();
			if (pks == null)
			{
				pksAndRecords.setPks(new BufferedDataSet(), 0);
				cachedRecords = pksAndRecords.getCachedRecords();
				pks = pksAndRecords.getPks();
			}
			else
			{
				cachedRecords = pksAndRecords.getCachedRecords();
				if (pks.hadMoreRows())
				{
					// When a record was added to a not-fully loaded foundset, create the pk cache so that duplicates
					// are checked in queryForMorePKs(), the new pk may be part of one of the other chunks
					pks.createPKCache();
				}
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

	private IRecordInternal createRecord(Row rowData, boolean javascriptRecord) throws ApplicationException
	{
		if (findMode)
		{
			if (pksAndRecords.getCachedRecords().size() > fsm.config.pkChunkSize()) return null;//limit to 200
			return new FindState(this);
		}

		if (javascriptRecord && !hasAccess(IRepository.INSERT))
		{
			throw new ApplicationException(ServoyException.NO_CREATE_ACCESS, new Object[] { getTable().getName() });
		}

		if (rowData == null && relationName != null)
		{
			Relation relation = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
			if (relation != null)
			{
				Placeholder ph = SQLGenerator.getRelationPlaceholder(creationSqlSelect, relation.getName());
				if (ph == null || !ph.isSet() || ph.getValue() == null || ((Object[])ph.getValue()).length == 0)
				{
					Debug.trace(
						"New record failed because related foundset had no parameters, or trying to make a new findstate when it is nested more then 2 deep"); //$NON-NLS-1$
					return null;
				}
				if (!relation.getAllowCreationRelatedRecords())
				{
					ApplicationException applicationException = new ApplicationException(ServoyException.NO_RELATED_CREATE_ACCESS,
						new Object[] { relation.getName() });
					applicationException.setContext(this.toString());
					throw applicationException;
				}
			}
		}

		try
		{
			if (javascriptRecord && !executeFoundsetTriggerBreakOnFalse(null, PROPERTY_ONCREATEMETHODID, false))
			{
				Debug.trace("New record creation was denied by onCreateRecord method"); //$NON-NLS-1$
				return null;
			}
		}
		catch (ServoyException e)
		{
			Debug.error(e);
			return null;
		}
		IRecordInternal newRecord = null;
		if (rowData == null)
		{
			Object[] data = sheet.getNewRowData(fsm.getApplication(), this);
			newRecord = new Record(this, rowManager.createNotYetExistInDBRowObject(data, true));
			sheet.processCopyValues(newRecord);
		}
		else
		{
			newRecord = new Record(this, rowData);
		}
		try
		{
			if (javascriptRecord)
			{
				executeFoundsetTrigger(new Object[] { newRecord }, PROPERTY_ONAFTERCREATEMETHODID, false);
			}
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
		return newRecord;
	}

	public void processCopyValues(int row)
	{
		IRecordInternal state = getRecord(row);
		if (state != null)
		{
			sheet.processCopyValues(state);
			fireFoundSetEvent(row, row, FoundSetEvent.CHANGE_UPDATE);
		}
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

		try
		{
			if (!executeFoundsetTriggerBreakOnFalse(null, PROPERTY_ONFINDMETHODID, false))
			{
				Debug.trace("Find mode switch was denied by onFind method"); //$NON-NLS-1$
				return;
			}
		}
		catch (ServoyException e)
		{
			Debug.error(e);
			return;
		}

		fireSelectionAdjusting();

		int oldSize = getSize();
		pksAndRecords.setPks(new BufferedDataSet(), 0);//return to 0

		//let the List know the model changed
		fireDifference(oldSize, 0, null);
//		selectionModel.setSelectedRow(-1);

		boolean oldFindMode = findMode;
		clearInternalState(false);
		findMode = true;
		if (oldFindMode == false) fireFindModeChange();


		try
		{
			newRecord(null, Integer.MAX_VALUE, true);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		try
		{
			executeFoundsetTrigger(null, PROPERTY_ONAFTERFINDMETHODID, false);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
	}

	/**
	 * Get the query for the current state.
	 * When in find mode, include the findState conditions
	 *
	 * @param reduceSearch
	 * @return
	 * @throws ServoyException
	 */
	public QuerySelect getCurrentStateQuery(boolean reduceSearch, boolean clone) throws ServoyException
	{
		QuerySelect currentQuery = pksAndRecords.getQuerySelectForReading();
		if (isInFindMode())
		{
			// Generate findstate query
			return fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), currentQuery, pksAndRecords.getCachedRecords(), reduceSearch, omittedPKs,
				lastSortColumns, true);
		}
		return clone ? deepClone(currentQuery, true) : currentQuery;
	}

	/**
	 * Execute the find sql, returns the number of records found, returns -1 when the call was blocked by a trigger
	 * @param clearLastResult
	 * @param reduceSearch
	 * @param clearIfZero
	 * @param cancelFind
	 * @param returnInvalidRangeConditions
	 * @return
	 * @throws ServoyException
	 */
	public int performFind(boolean clearLastResult, boolean reduceSearch, boolean clearIfZero, boolean cancelFind, List<String> returnInvalidRangeConditions)
		throws ServoyException//perform the find
	{
		int numberOfFindStates = getSize();
		if (cancelFind)
		{
			// ignore find states
			pksAndRecords.setPks(null, 0);
			setSelectedIndex(-1);
		}
		else
		{
			try
			{
				if (!executeFoundsetTriggerBreakOnFalse(new Object[] { Boolean.valueOf(clearLastResult), Boolean.valueOf(reduceSearch) },
					PROPERTY_ONSEARCHMETHODID, true))
				{
					Debug.trace("Foundset search was denied by onSearchFoundset method"); //$NON-NLS-1$
					return -1; // blocked
				}
			}
			catch (ServoyException e)
			{
				Debug.error(e);
				return -1; // blocked
			}

			if (clearLastResult) removeLastFound();

			setSelectedIndex(numberOfFindStates > 0 ? 0 : -1);
		}

		try
		{
			QuerySelect findSqlSelect = getCurrentStateQuery(reduceSearch, false);

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
				findPKs = performQuery(transaction_id, findSqlSelect, getRowIdentColumnTypes(), 0, fsm.config.pkChunkSize(), IDataServer.FIND_BROWSER_QUERY);
			}
			catch (RemoteException e)
			{
				throw new RepositoryException(e);
			}
			if (Debug.tracing())
			{
				Debug.trace("Find executed, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + ", sql: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					findSqlSelect.toString());
			}

			IFoundSetChanges changes = null;
			if (findPKs.getRowCount() == 0)
			{
				if (clearIfZero)
				{
					changes = pksAndRecords.setPksAndQuery(null, 0, findSqlSelect);
					clearInternalState(true);
					setSelectedIndex(-1);
				}
			}
			else
			{
				fireSelectionAdjusting();
				changes = pksAndRecords.setPksAndQuery(findPKs, findPKs.getRowCount(), findSqlSelect);

				clearInternalState(true);
				fireAggregateChangeWithEvents(null); //notify about aggregate change,because the find has cleared them all.
			}
			initialized = true;

			fireDifference(numberOfFindStates, getSize(), changes);

			if (getSelectedIndex() == -1 && getSize() > 0)
			{
				setSelectedIndex(0);
			}

			int nfound = findPKs.getRowCount();
			try
			{
				executeFoundsetTrigger(null, PROPERTY_ONAFTERSEARCHMETHODID, false);
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}

			return nfound;
		}
		catch (ServoyException e)
		{
			pksAndRecords.setPks(null, 0);
			clearInternalState(true);
			setSelectedIndex(-1);
			fireDifference(numberOfFindStates, 0, null);

			throw e;
		}
	}

	String getSerializedQuery()
	{
		QuerySelect currentQuery = pksAndRecords.getQuerySelectForReading();
		if (currentQuery != null)
		{ // this could be find mode
			String serverName = DataSourceUtils.getDataSourceServerName(getDataSource());
			ArrayList<TableFilter> tableFilterParams = fsm.getTableFilterParams(serverName, currentQuery);
			try
			{
				QuerySet qs = fsm.getDataServer().getSQLQuerySet(serverName, currentQuery, tableFilterParams, 0, -1, true, true);
				return qs.getSelect().getSql();
			}
			catch (RepositoryException | RemoteException e)
			{
				Debug.error("Can't get a serialized state from " + currentQuery, e); //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Fire difference based on real size (not corrected for fires!)
	 *
	 * @param oldSize
	 * @param newSize
	 */
	protected void fireDifference(int oldSize, int newSize, IFoundSetChanges changes)
	{
		if (serializedQuery != null)
		{
			String currentQuery = getSerializedQuery();
			if (!currentQuery.equals(serializedQuery))
			{
				this.serializedQuery = currentQuery;
				fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.FOUNDSET_DEFINITION_CHANGE, FoundSetEvent.CHANGE_UPDATE));
			}
		}
		if (changes != null)
		{
			changes.getChanges().forEach(change -> fireFoundSetEvent(change.getFirstRow(), change.getLastRow(), change.getType()));
			return;
		}
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
		synchronized (pksAndRecords)
		{
			pksAndRecords.setPksAndQuery(pksAndRecords.getPks(), pksAndRecords.getDbIndexLastPk(), deepClone(creationSqlSelect), true);
		}
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
		if (!defer && (getFoundSetManager().getEditRecordList().stopIfEditing(this) != ISaveConstants.STOPPED))
		{
			fsm.getApplication().reportJSError("Couldn't do a sort because there are edited records on this foundset: " + this, null); //$NON-NLS-1$
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
		synchronized (pksAndRecords)
		{
			sqlSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(this, sheet.getTable(), pksAndRecords.getQuerySelectForReading(), null, true, null,
				lastSortColumns, true);
			IDataSet pks = pksAndRecords.getPks();
			// set the current select with the new sort in case a refreshFromDBInternal comes along or when defer is set
			pksAndRecords.setPksAndQuery(pks, pks == null ? 0 : pks.getRowCount(), sqlSelect, true);

			if (defer)
			{
				if (pks != null) pks.clearHadMoreRows(); //make sure we don't do query for more pks with a new sort!
				return;
			}
		}

		if (!initialized)
		{
			fsm.getApplication().reportJSWarning(
				"Performing sort on an uninitialized foundset, please use defer=true or load records first (filter params may be discarded if not applied yet): " +
					this);
		}

		reloadWithCurrentQuery(fsm.config.pkChunkSize(), true);
	}

	/**
	 * @throws ServoyException
	 * @throws RepositoryException
	 */
	protected boolean reloadWithCurrentQuery(int rowsToRetrieveHint, boolean isSorting) throws ServoyException
	{
		QuerySelect sqlSelect;
		IDataSet pks;
		synchronized (pksAndRecords)
		{
			sqlSelect = getPksAndRecords().getQuerySelectForReading();
			pks = pksAndRecords.getPks();
		}

		// try to keep selection when reloading
		Object[][] selectedPKs = getSelectedPksForKeepingSelection(pks, !isSorting && fsm.isAlwaysFollowPkSelection());

		IFoundSetChanges changes = null;
		int oldSize = getRawSize();

		int rowsToRetrieve = calculateRowsToRetrieve(rowsToRetrieveHint, selectedPKs);

		String transaction_id = fsm.getTransactionID(sheet);
		try
		{
			pks = performQuery(transaction_id, sqlSelect, getRowIdentColumnTypes(), 0, rowsToRetrieve, IDataServer.FOUNDSET_LOAD_QUERY);

			synchronized (pksAndRecords)
			{
				// optimistic locking, if the query has been changed in the mean time forget about the refresh
				if (sqlSelect != pksAndRecords.getQuerySelectForReading())
				{
					Debug.log("sort: query was changed during refresh, not resetting old query"); //$NON-NLS-1$
					return false;
				}
				changes = pksAndRecords.setPksAndQuery(pks, pks.getRowCount(), sqlSelect, isSorting);
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}

		initialized = true;
		mustQueryForUpdates = false;

		if (!isSorting)
		{
			//do kind of browseAll/refresh from db.
			// differences: selected record isn't tried to keep in sync with pk, new records are handled different.
			// boolean must be false before clear (that fires a aggregate change so again a getRecord())
			clearInternalState(true);
		}

		int newSize = getRawSize();
		fireDifference(oldSize, newSize, changes);

		trySelectingPks(selectedPKs, newSize, false);

		return true;
	}

	/*
	 * Select by these pks if possible, otherwise select keep the current index or the first record
	 */
	private void trySelectingPks(Object[][] selectedPKs, int newSize, boolean index0WhenNotFound)
	{
		boolean selectedPKsRecPresent = false;
		if (selectedPKs != null)
		{
			selectedPKsRecPresent = selectedPKs.length == 1 ? selectRecord(selectedPKs[0]) : selectRecords(selectedPKs);
		}

		if (!selectedPKsRecPresent && (index0WhenNotFound || !(getSelectedIndex() >= 0 && getSelectedIndex() < newSize)))
		{
			if (fsm.getApplication().isEventDispatchThread())
			{
				setSelectedIndex(newSize > 0 ? 0 : -1);
			}
			else
			{
				fsm.getApplication().invokeLater(() -> setSelectedIndex(newSize > 0 ? 0 : -1));
			}
		}
	}

	/*
	 * if we want to keep the selected pk, query some more to make the change bigger that the pk is loaded again
	 */
	private int calculateRowsToRetrieve(int rowsToRetrieveHint, Object[][] selectedPKs)
	{
		if (selectedPKs != null)
		{
			// round up to next 200 (selection + 50)
			return (1 + (getSelectedIndex() + rowsToRetrieveHint / 4) / rowsToRetrieveHint) * rowsToRetrieveHint;
		}
		return rowsToRetrieveHint;
	}

	private Object[][] getSelectedPksForKeepingSelection(IDataSet pks, boolean alwaysFollowPkSelection)
	{
		if (pks != null)
		{
			int[] selectedIndexes = getSelectedIndexes();
			if (selectedIndexes != null && selectedIndexes.length > 0 && shouldKeepSelectionForPK(selectedIndexes, alwaysFollowPkSelection))
			{
				return stream(selectedIndexes).mapToObj(pks::getRow).toArray(Object[][]::new);
			}
		}
		return null;
	}

	/**
	 * if single selected and first record is selected we ignore selection, unless configured via databasemanager.alwaysFollowPkSelection = false
	 *
	 * Note: for a refresh generated by the UI (sorting), the databasemanager.alwaysFollowPkSelection should be ignored (allowConfigOverride = false)
	 */
	private boolean shouldKeepSelectionForPK(int[] selectedIndexes, boolean alwaysFollowPkSelection)
	{
		if (alwaysFollowPkSelection)
		{
			return true;
		}

		// if the selection is on the first record only we do not want to select the same pk again, just select first index again
		return selectedIndexes.length > 1 || (selectedIndexes.length == 1 && selectedIndexes[0] > 0);
	}

	public void sort(Comparator<Object[]> recordPKComparator)
	{
		if (findMode) return;
		PksAndRecordsHolder pksAndRecordsHolderCopy;
		IDataSet pks;
		int rowCount = 0;
		Object[][] selectedPKs = null;
		synchronized (pksAndRecords)
		{
			pksAndRecordsHolderCopy = pksAndRecords.shallowCopy();
			pks = pksAndRecords.getPks();
			if (pks != null)
			{
				rowCount = pks.getRowCount();

				// if single selected and first record is selected we ignore selection
				selectedPKs = getSelectedPksForKeepingSelection(pks, false);
			}
		}
		int oldSize = getSize();

		PKDataSet pks2 = null;
		if (pksAndRecordsHolderCopy.getPks() != null && pksAndRecordsHolderCopy.getPks().hadMoreRows())
		{
			queryForMorePKs(pksAndRecordsHolderCopy, rowCount, -1, false);
			pks2 = pksAndRecordsHolderCopy.getPks();
		}
		else
		{
			pks2 = pksAndRecordsHolderCopy.getPksClone();
		}
		if (pks != null)
		{
			pks2.sort(recordPKComparator);
			IFoundSetChanges changes = null;
			synchronized (pksAndRecords)
			{
				changes = pksAndRecords.setPksAndQuery(pks2, pksAndRecordsHolderCopy.getDbIndexLastPk(), pksAndRecords.getQuerySelectForReading(), true);
			}

			int newSize = getSize();
			fireDifference(oldSize, newSize, changes);

			trySelectingPks(selectedPKs, newSize, true);
		}
	}

	public boolean isRecordEditable(int rowIndex)
	{
		if (findMode && hasAccess(IRepository.READ))
		{
			return true;//findmode is always editable, when having read permission
		}
		if (hasAccess(IRepository.UPDATE))
		{
			IRecordInternal rec = getRecord(rowIndex);
			if (rec != null)
			{
				boolean locked = rec.isLocked();
				if (locked)
				{
					fsm.getApplication().reportWarningI18NMessage("servoy.foundSet.recordLocked");
				}
				return !locked;
			}
			fsm.getApplication().reportWarningI18NMessage("servoy.foundSet.noRecord");
		}
		else if (hasAccess(IRepository.INSERT))
		{
			IRecordInternal rec = getRecord(rowIndex);
			if (rec != null)
			{
				return !rec.existInDataSource();
			}
			fsm.getApplication().reportWarningI18NMessage("servoy.foundSet.noRecord");
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
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.FIND_MODE_CHANGE, FoundSetEvent.CHANGE_UPDATE));
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
							if (e.getType() == FoundSetEvent.FOUNDSET_DEFINITION_CHANGE)
								array = foundSetEventListeners.stream().filter(listener -> listener.wantsFoundSetDefinitionChanges())
									.toArray(IFoundSetEventListener[]::new);
							else array = foundSetEventListeners.toArray(new IFoundSetEventListener[foundSetEventListeners.size()]);
						}

						// make sure that all the foundset events are completely handled
						// and then the events that are triggered by those are done
						FireCollector fireCollector = FireCollector.getFireCollector();
						try
						{
							for (IFoundSetEventListener element : array)
							{
								element.foundSetChanged(e);
							}
						}
						finally
						{
							fireCollector.done();
						}
					}
				}
			};
			if (fsm.getApplication().isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				fsm.getApplication().invokeLater(run);
			}
		}
	}

	public void fireFoundSetChanged()
	{
		int size = getSize();
		fireDifference(size, size, null);
	}

	public abstract int getSelectedIndex();

	public abstract void setSelectedIndex(int i);

	public abstract void setSelectedIndexes(int[] indexes);

	public abstract int[] getSelectedIndexes();

	@Override
	public int getRecordIndex(String pkHash, int startHint)
	{
		int hintStart = Math.min(startHint + 5, getSize());

		SafeArrayList<IRecordInternal> cachedRecords = getPksAndRecords().getCachedRecords();
		PKDataSet pks = getPksAndRecords().getPks();

		// an effort to find the record faster follows:
		// first look directly at hint (which is the record's index when it was sent to browser initially; meanwhile it might have changed)
		if (checkThatRecordPKHashMatches(pkHash, cachedRecords, pks, startHint)) return startHint;

		// then start a bit above hint and go to 0
		int start = (hintStart < 0 || hintStart > pks.getRowCount()) ? 0 : hintStart;
		for (int i = start; --i >= 0;)
			if (i != startHint && checkThatRecordPKHashMatches(pkHash, cachedRecords, pks, i)) return i;

		// check the rest of them
		for (int i = start; i < pks.getRowCount(); i++)
			if (checkThatRecordPKHashMatches(pkHash, cachedRecords, pks, i)) return i;

		return -1;
	}

	private boolean checkThatRecordPKHashMatches(String pkHash, SafeArrayList<IRecordInternal> cachedRecords, PKDataSet pks, int i)
	{
		String recordPkHash = null;
		IRecordInternal record = cachedRecords.get(i);
		if (record != null) recordPkHash = record.getPKHashKey();
		else recordPkHash = createPKHashKey(pks.getRow(i));

		return pkHash.equals(recordPkHash);
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFireCollectable#completeFire(java.util.List)
	 */
	public void completeFire(Map<IRecord, List<String>> entries)
	{
		int start = Integer.MAX_VALUE;
		int end = -1;
		List<String> dataproviders = null;
		for (IRecord record : entries.keySet())
		{
			int index = getRecordIndex(record);
			if (index != -1 && start > index)
			{
				start = index;
			}
			if (end < index)
			{
				end = index;
			}
			dataproviders = entries.get(record);
		}
		if (start != Integer.MAX_VALUE && end != -1)
		{
			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE, dataproviders);
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

				IDataSet pks;
				synchronized (pksAndRecords)
				{
					pks = pksAndRecords.getPks();
				}

				// ROW CAN BE NULL
				Row row = e.getRow();

				switch (e.getType())
				{
					case RowEvent.INSERT :
						Debug.trace("Row inserted notify"); //$NON-NLS-1$
						if (!currentQueryHasAnyCondition() && pks != null && !pks.hadMoreRows() && row != null)//does show all records, if so show record .. if not we whould have to go to the database to verify if the record does match our SQL
						{
							Object[] pk = row.getPK();
							//check if the new record's pk is already present in this foundset
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
											equal = equalObjects(pk[p], pkval);
										}
										if (equal)
										{
											return; // do nothing; this foundset (probably it was already refreshed by coincidence, or same foundset that generated the event)
										}
									}
								}

								size = getSize();
								pks.addRow(pk);

							}
							clearAggregates();
							fireFoundSetEvent(size, size, FoundSetEvent.CHANGE_INSERT);
						}
						else
						{
							mustQueryForUpdates = true;
							clearAggregates();
						}
						break;

					case RowEvent.UPDATE :
					case RowEvent.DELETE :
						if (pks != null && row != null)
						{
							String pkHash = row.getPKHashKey();
							for (int i = pks.getRowCount() - 1; i >= 0; i--)
							{
								Object[] pk = pks.getRow(i);
								if (createPKHashKey(pk).equals(pkHash))
								{
									if (e.getType() == RowEvent.UPDATE)
									{
										clearAggregates();
										fireFoundSetEvent(i, i, FoundSetEvent.CHANGE_UPDATE);
									}
									else if (e.getType() == RowEvent.DELETE)
									{
										removeRecordInternal(i); // does fireIntervalRemoved(this,i,i);
									}
									break;
								}
							}
							if (e.getType() == RowEvent.DELETE && omittedPKs != null && omittedPKs.getRowCount() > 0)
							{
								for (int i = 0; i < omittedPKs.getRowCount(); i++)
								{
									if (equalObjects(pkHash, createPKHashKey(omittedPKs.getRow(i))))
									{
										omittedPKs.removeRow(i);
										replaceOmitCondition();
										break;
									}
								}
							}
						}
						else if (row == null && getSize() > 0 && (!e.isAggregateChange() || aggregateCache.size() > 0))
						{
							clearAggregates();
							Object[] changedColumnNames = e.getChangedColumnNames();
							if (changedColumnNames instanceof String[])
							{
								fireFoundSetEvent(0, getSize() - 1, FoundSetEvent.CHANGE_UPDATE, asList((String[])changedColumnNames));
							}
							else
							{
								fireFoundSetEvent(0, getSize() - 1, FoundSetEvent.CHANGE_UPDATE);
							}
						}
						break;

					case RowEvent.PK_UPDATED :
						// row pk updated, adjust pksAndRecords admin
						if (e.getOldPkHash() != null)
						{
							// oldPkHash is only set when row was updated by this client
							pksAndRecords.rowPkUpdated(e.getOldPkHash(), row);
						}
				}
			}
			finally
			{
				isInNotify = false;
			}
		}
	}

	private void replaceOmitCondition()
	{
		QuerySelect sqlSelect = pksAndRecords.getQuerySelectForModification();
		if (omittedPKs.getRowCount() > 0)
		{
			List<IQuerySelectValue> pkQueryColumns = sqlSelect.getColumns();
			sqlSelect.setCondition(SQLGenerator.CONDITION_OMIT,
				SQLGenerator.createSetConditionFromPKs(IBaseSQLCondition.NOT_OPERATOR,
					pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]), sheet.getTable().getRowIdentColumns(),
					omittedPKs));
		}
		else
		{
			sqlSelect.clearCondition(SQLGenerator.CONDITION_OMIT);
		}
		synchronized (pksAndRecords)
		{
			pksAndRecords.setPksAndQuery(pksAndRecords.getPks(), pksAndRecords.getDbIndexLastPk(), sqlSelect, true);
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
			aggregateCache.keySet().removeAll(aggregatesToRemove);
			for (String aggregate : aggregatesToRemove)
			{
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
		// if it is already a full flush (invalidate_foundset) don't do anything.
		if (indexen != null && indexen[0] < 0 && indexen[1] < 0) return;

		for (Record record : records)
		{
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
			else if (Debug.tracing())
			{
				Debug.trace("record index -1 for the record " + record + " already out of the foundset: " + this); //$NON-NLS-1$ //$NON-NLS-2$
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

	/**
	 * Get all dataproviders of the foundset.
	 *
	 * @sample
	 * var dataprovidersNames = %%prefix%%alldataproviders;
	 * application.output("This foundset has " + dataprovidersNames.length + " data providers.")
	 * for (var i=0; i<dataprovidersNames.length; i++)
	 * 	application.output(dataprovidersNames[i]);
	 *
	 * @special
	 */
	@JSReadonlyProperty
	public String[] alldataproviders()
	{
		List<String> al = new ArrayList<>();
		Table table = (Table)getTable();
		if (table != null)
		{
			try
			{
				for (Column column : iterate(table.getColumnsSortedByName()))
				{
					if (!column.hasFlag(IBaseColumn.EXCLUDED_COLUMN))
					{
						al.add(column.getDataProviderID());
					}
				}

				FlattenedSolution flattenedSolution = fsm.getApplication().getFlattenedSolution();
				for (AggregateVariable av : iterate(flattenedSolution.getAggregateVariables(table, true)))
				{
					al.add(av.getDataProviderID());
				}

				for (ScriptCalculation sc : iterate(flattenedSolution.getScriptCalculations(table, true)))
				{
					if (al.contains(sc.getDataProviderID()))
					{
						al.remove(sc.getDataProviderID());
					}
					al.add(sc.getDataProviderID());
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		return al.toArray(new String[al.size()]);
	}

	public Object get(String name, Scriptable start)
	{
		if (isToplevelKeyword(name)) return Scriptable.NOT_FOUND;
		if (name.equals("multiSelect")) //$NON-NLS-1$
		{
			return Boolean.valueOf(isMultiSelect());
		}
		if ("alldataproviders".equals(name)) //$NON-NLS-1$
		{
			return new NativeJavaArray(this, alldataproviders());
		}

		Object mobj = jsFunctions.get(name);
		if (mobj != null)
		{
			ScriptRuntime.setFunctionProtoAndParent((BaseFunction)mobj, start);
			return mobj;
		}

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

		if (name.equals("_records_")) //$NON-NLS-1$
		{
			int maxRows = getSize();
			if (hadMoreRows())
			{
				maxRows--;
			}
			Scriptable records = Context.getCurrentContext().newArray(this, maxRows);
			for (int i = 0; i < maxRows; i++)
			{
				IRecordInternal record = pksAndRecords.getCachedRecords().get(i);
				records.put(i, records, record);
			}
			return records;
		}
		if (name.equals("_selection_")) //$NON-NLS-1$
		{
			int[] selection = getSelectedIndexes();
			if (selection.length == 0)
			{
				return Integer.valueOf(0);
			}
			if (selection.length == 1)
			{
				return Integer.valueOf(selection[0] + 1);
			}
			StringBuilder buf = new StringBuilder();
			buf.append('[');
			for (int i = 0; i < selection.length; i++)
			{
				if (i > 0) buf.append(", "); //$NON-NLS-1$
				buf.append(selection[i] + 1);
			}
			buf.append(']');
			return buf.toString();
		}
		return Scriptable.NOT_FOUND;
	}

	/**
	 * @param scriptMethod
	 */
	public void reloadFoundsetMethod(IScriptProvider scriptMethod)
	{
		if (prototypeScope instanceof LazyCompilationScope)
		{
			((LazyCompilationScope)prototypeScope).remove(scriptMethod);
			((LazyCompilationScope)prototypeScope).put(scriptMethod, scriptMethod);
		}
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

		Pair<String, String> scope = ScopesUtils.getVariableScope(name);
		if (scope.getLeft() != null)
		{
			fsm.getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft()).put(scope.getRight(), start, value);
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
		if (prototypeScope == null)
		{
			LazyCompilationScope scope = new LazyCompilationScope(this, fsm.getApplication().getScriptEngine(), new ISupportScriptProviders()
			{
				public Iterator< ? extends IScriptProvider> getScriptMethods(boolean sort)
				{
					List<ScriptMethod> methods = null;
					Iterator<TableNode> tableNodes = fsm.getApplication().getFlattenedSolution().getTableNodes(getTable());
					while (tableNodes.hasNext())
					{
						TableNode tn = tableNodes.next();
						Iterator<ScriptMethod> fsMethods = tn.getFoundsetMethods(sort);
						if (methods == null)
						{
							if (!tableNodes.hasNext())
							{
								// just 1
								return fsMethods;
							}
							methods = new ArrayList<ScriptMethod>();
						}
						while (fsMethods.hasNext())
						{
							methods.add(fsMethods.next());
						}
					}
					return methods == null ? Collections.<ScriptMethod> emptyList().iterator() : methods.iterator();
				}

				public Iterator<ScriptVariable> getScriptVariables(boolean b)
				{
					return Collections.<ScriptVariable> emptyList().iterator();
				}

				public ScriptMethod getScriptMethod(int methodId)
				{
					return null; // not called by LCS
				}
			})
			{

				@Override
				public String getClassName()
				{
					return "FoundSetScope"; //$NON-NLS-1$
				}

				@Override
				public String getScopeName()
				{
					return getDataSource();
				}

			};
			scope.setFunctionParentScriptable(this); // make sure functions like getSize cannot be overridden
			prototypeScope = scope;
		}

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

		al.add("_records_"); //$NON-NLS-1$
		al.add("_selection_"); //$NON-NLS-1$

		al.addAll(jsFunctions.keySet());
		return al.toArray();
	}

	public Object getDefaultValue(Class< ? > hint)
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
		for (IRecordInternal cachedRecord : cachedRecords)
		{
			if (cachedRecord != null)
			{
				counter++;
			}

		}
		StringBuilder sb = new StringBuilder();
		sb.append("FoundSet[Table:"); //$NON-NLS-1$
		sb.append(getTable() == null ? "<none>" : getTable().getName()); //$NON-NLS-1$
		sb.append(",Size: "); //$NON-NLS-1$
		sb.append(getRawSize());
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

	@Override
	protected Object clone() throws CloneNotSupportedException
	{
		FoundSet obj = (FoundSet)super.clone();
		obj.pksAndRecords = new PksAndRecordsHolder(obj, fsm.config.chunkSize(), fsm.config.optimizedChangeFires());
		synchronized (pksAndRecords)
		{
			obj.pksAndRecords.setPksAndQuery(new BufferedDataSet(pksAndRecords.getPks()), pksAndRecords.getDbIndexLastPk(),
				pksAndRecords.getQuerySelectForModification());
		}
		obj.findMode = false;
		obj.creationSqlSelect = deepClone(creationSqlSelect);
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

				fs.creationSqlSelect = deepClone(fs_sqlSelect);//reset the creation! because we just changed the sqlSelect
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
						if (!fs_sqlSelect.setPlaceholderValue(new TablePlaceholderKey(fs_sqlSelect.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY), pk))
						{
							Debug.error(new RuntimeException(
								"Could not set placeholder " + new TablePlaceholderKey(fs_sqlSelect.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY) + //$NON-NLS-1$
									" in query " + fs_sqlSelect + "-- continuing")); //$NON-NLS-1$ //$NON-NLS-2$
						}
						fs.pksAndRecords.getPks().setRow(0, pk);
						fs.setSelectedIndex(0);
					}
					else
					{
						fs.newRecord(row, 0, true, false);
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

		FoundSet fs = (FoundSet)fsm.getNewFoundSet(sheet.getTable(), creationSqlSelect, lastSortColumns);
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
		if (omittedPKs != null) fs.omittedPKs = new BufferedDataSet(omittedPKs.getColumnNames(), omittedPKs.getColumnTypes(), omittedPKs.getRows());

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

		fs.setMultiSelectInternal(isMultiSelect());
		if (isMultiSelect())
		{
			int[] selectedIndexes = getSelectedIndexes();
			if (selectedIndexes != null && selectedIndexes.length > 0) fs.setSelectedIndexes(selectedIndexes);
			else fs.setSelectedIndex(getSelectedIndex());
		}
		else fs.setSelectedIndex(getSelectedIndex());

		return fs;
	}

	public boolean copyFrom(FoundSet fs)
	{
		if (fs == null || fs.getTable() == null || fs == this) return false;
		if (!fs.getTable().equals(getTable())) return false;
		if (relationName != null) return false;

		int oldNumberOfRows = getSize();
		fireSelectionAdjusting();

		omittedPKs = null;

		List<TableFilter> myOwnFilters = null;
		// look for filters in this foundset that have not been applied to the other foundset
		for (TableFilter filter : iterate(foundSetFilters))
		{
			if (fs.foundSetFilters == null || !fs.foundSetFilters.contains(filter))
			{
				if (myOwnFilters == null) myOwnFilters = new ArrayList<TableFilter>(foundSetFilters.size());
				myOwnFilters.add(filter);
			}
		}
		sheet = fs.sheet;
		IFoundSetChanges changes = pksAndRecords.setPksAndQuery(new BufferedDataSet(fs.pksAndRecords.getPks()), fs.pksAndRecords.getDbIndexLastPk(),
			addFilterconditions(fs.pksAndRecords.getQuerySelectForModification(), myOwnFilters));
		if (fs.foundSetFilters != null)
		{
			// copy over the foundset filters from the other fs, merged with the filters this foundset had
			foundSetFilters = new ArrayList<>();
			fs.foundSetFilters.forEach(filter -> {
				if (filter.getTableFilterdefinition() instanceof QueryTableFilterdefinition)
				{
					QuerySelect select = deepClone(((QueryTableFilterdefinition)filter.getTableFilterdefinition()).getQuerySelect());
					select.relinkTable(select.getTable(), creationSqlSelect.getTable());
					foundSetFilters.add(new TableFilter(filter.getName(), sheet.getServerName(), sheet.getTable().getName(), sheet.getTable().getSQLName(),
						new QueryTableFilterdefinition(select), false));
				}
				else
				{
					foundSetFilters.add(filter);
				}
			});
			if (myOwnFilters != null)
			{
				foundSetFilters.addAll(myOwnFilters);
			}
			resetFiltercondition(foundSetFilters);
		}
		initialized = fs.initialized;

		clearInternalState(true);

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
		fireDifference(oldNumberOfRows, getSize(), changes);

		setMultiSelect(fs.isMultiSelect());
		if (fs.isMultiSelect())
		{
			int[] selectedIndexes = fs.getSelectedIndexes();
			if (selectedIndexes != null && selectedIndexes.length > 0) setSelectedIndexes(selectedIndexes);
			else setSelectedIndex(fs.getSelectedIndex());
		}
		else setSelectedIndex(fs.getSelectedIndex());

		if (myOwnFilters != null)
		{
			// my own filters have been added, have to refresh
			refresh();
		}
		return true;
	}

	public void setSQLSelect(QuerySelect select) throws Exception
	{
		refreshFromDBInternal(select, false, fsm.config.pkChunkSize(), false, false);
	}

	public boolean addFilterParam(String filterName, String dataprovider, String operator, Object value) throws ServoyException
	{
		DataproviderTableFilterdefinition dataproviderTableFilterdefinition = fsm.createDataproviderTableFilterdefinition(sheet.getTable(), dataprovider,
			operator, value);

		if (dataproviderTableFilterdefinition == null)
		{
			fsm.getApplication().reportJSError("Table filter not created, column not found in table or operator invalid, filterName = '" + filterName +
				"', serverName = '" + sheet.getTable().getServerName() + "', table = '" + sheet.getTable().getName() + "', dataprovider = '" + dataprovider +
				"', operator = '" + operator + "'", null);
			return false;
		}

		// create condition to check filter
		QueryFilter filtercondition = SQLGenerator.createTableFiltercondition(creationSqlSelect.getTable(), sheet.getTable(),
			dataproviderTableFilterdefinition);
		if (filtercondition == null)
		{
			return false;
		}

		return addFilterParam(filterName, dataproviderTableFilterdefinition);
	}

	protected boolean addFilterParam(String filterName, TableFilterdefinition tableFilterdefinition)
	{
		if (sheet.getTable() == null)
		{
			return false;
		}
		EditRecordList editRecordList = fsm.getEditRecordList();
		if (editRecordList.stopIfEditing(this) != ISaveConstants.STOPPED)
		{
			Debug.log("Couldn't add foundset filter param because foundset had edited records"); //$NON-NLS-1$
			return false;
		}

		TableFilter filter = new TableFilter(filterName, sheet.getServerName(), sheet.getTable().getName(), sheet.getTable().getSQLName(),
			tableFilterdefinition, false);

		if (filter.isContainedIn(foundSetFilters))
		{
			// do not add the same filter, will add same AND-condition anyway
			return true;
		}

		if (foundSetFilters == null)
		{
			foundSetFilters = new ArrayList<>();
		}
		foundSetFilters.add(filter);

		resetFiltercondition(foundSetFilters);
		// Do not set initialized to false, this would trigger a load if the form happens to be touched anywhere
		if (fsm.config.uninitializedFoundsetWhenFiltersAreAdded())
		{
			initialized = false;
		}
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

		List<TableFilter> originalFilters = foundSetFilters == null ? null : new ArrayList<>(foundSetFilters);
		boolean found = removeFilterParamInternal(filterName);

		if (found)
		{
			resetFiltercondition(originalFilters);
			// Do not set initialized to false, this would trigger a load if the form happens to be touched anywhere
			if (fsm.config.uninitializedFoundsetWhenFiltersAreAdded())
			{
				initialized = false;
			}
		}
		return found;
	}

	private boolean removeFilterParamInternal(String filterName)
	{
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
			if (foundSetFilters.isEmpty())
			{
				foundSetFilters = null;
			}
		}
		return found;
	}

	public void clearFilterParams()
	{
		List<TableFilter> originalFilters = foundSetFilters;
		this.foundSetFilters = null;
		resetFiltercondition(originalFilters);
	}

	private void resetFiltercondition(List<TableFilter> originalFilters)
	{
		synchronized (pksAndRecords)
		{
			creationSqlSelect.clearCondition(SQLGenerator.CONDITION_FILTER);
			// remove joins made for filter conditions
			removeFilterJoins(creationSqlSelect, originalFilters);
			addFilterconditions(creationSqlSelect, foundSetFilters);

			// update temp query with the current filters so it can be used with js_reloadWithFilters
			QuerySelect tempQuery = pksAndRecords.getTempQuery();
			tempQuery.clearCondition(SQLGenerator.CONDITION_FILTER);
			// remove joins made for filter conditions
			removeFilterJoins(tempQuery, originalFilters);
			addFilterconditions(tempQuery, foundSetFilters);
		}
	}

	private QuerySelect removeFilterJoins(QuerySelect select, List<TableFilter> filters)
	{
		for (TableFilter tf : iterate(filters))
		{
			select.removeJoinsWithOrigin(tf);
		}
		return select;
	}

	private QuerySelect addFilterconditions(QuerySelect select, List<TableFilter> filters)
	{
		for (TableFilter tf : iterate(filters))
		{
			QueryFilter filtercondition = SQLGenerator.createTableFiltercondition(select.getTable(), sheet.getTable(), tf);
			select.addCondition(SQLGenerator.CONDITION_FILTER, filtercondition.getCondition());
			for (ISQLJoin join : iterate(filtercondition.getJoins()))
			{
				join.setOrigin(tf);
				select.addJoin(join);
			}
		}
		return select;
	}

	public boolean addFoundSetFilterParam(QBSelect query, String filterName)
	{
		return this.js_addFoundSetFilterParam(query, filterName);
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

	@Deprecated
	public void makeEmpty()
	{
		clear();
	}

	//used if left hand side null in relation
	public void clear()
	{
		omittedPKs = null;
		QuerySelect sqlSelect = deepClone(creationSqlSelect);
		BufferedDataSet emptyPks = new BufferedDataSet();
		if (sqlSelect != null)
		{
			sqlSelect.setCondition(SQLGenerator.CONDITION_CLEAR, SQLGenerator.createDynamicPKSetConditionForFoundset(this, sqlSelect.getTable(), emptyPks));
			sqlSelect.clearCondition(SQLGenerator.CONDITION_RELATION);
		}
		pksAndRecords.setPksAndQuery(emptyPks, 0, sqlSelect);

		if (rowManager != null) rowManager.clearAndCheckCache();
//		if (rowManager.getRowCount() > 5000)
//		{
//			//if explicitly cleared, and significant size is presnt, hint for flush
//			rowManager.flushAllCachedRows();
//		}
		initialized = true;
		clearInternalState(true);
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
			int index = getRecordIndex(pk);
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
				if (serializedQuery == null && l.wantsFoundSetDefinitionChanges())
				{
					// if this new listener wants to have a defintion change, generate the serialized query
					serializedQuery = getSerializedQuery();
				}
			}
		}
	}

	public void removeFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			foundSetEventListeners.remove(l);
			// did we remove a listener that wants a definition change and we do have a serializedQuery?
			if (serializedQuery != null && l.wantsFoundSetDefinitionChanges())
			{
				// then check if there are others, if not set it to null
				if (!foundSetEventListeners.stream().anyMatch(listener -> listener.wantsFoundSetDefinitionChanges()))
				{
					serializedQuery = null;
				}
			}
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
			case IRepository.AGGREGATEVARIABLES :
				return getSQLSheet().getAggregateNames();

			case IRepository.SCRIPTCALCULATIONS :
				return getSQLSheet().getCalculationNames();

			case IRepository.COLUMNS :
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


	protected IDataSet performQuery(String transaction_id, QuerySelect query, ColumnType[] resultTypes, int startRow, int rowsToRetrieve, int type)
		throws RemoteException, ServoyException
	{
		if (disposed)
		{
			fsm.getApplication().reportJSError("cannot query for a disposed foundset", null); //$NON-NLS-1$
			throw new ServoyException(ServoyException.INVALID_INPUT);
		}

		if (!hasAccess(IRepository.READ))
		{
			int rawSize = getRawSize();
			clear();
			fireDifference(rawSize, 0, null);
			throw new ApplicationException(ServoyException.NO_ACCESS, new Object[] { getSQLSheet().getTable().getName() });
		}

		QuerySelect theQuery = query;

		IDataSet dataSet = SQLGenerator.getEmptyDataSetForDummyQuery(theQuery);
		if (dataSet != null)
		{
			// just return empty data set, prevent 'where 1=2' queries
			if (Debug.tracing())
			{
				Debug.trace("Skipping reload query on cleared foundset for data source " + getDataSource());
			}
			return dataSet;
		}

		try
		{
			boolean distinctInMemory;

			if (type == RAW_QUERY)
			{
				// a raw query, just run it 'as is'
				distinctInMemory = false;
			}
			else
			{
				distinctInMemory = !theQuery.isUnique();

				if (!theQuery.isDistinct() && !distinctInMemory && hasTablefilterWithJoins(theQuery))
				{
					// table filters adds joins that cause duplicate results
					if (theQuery.getColumns().size() == 1 && isDistinctAllowed(theQuery.getColumns(), theQuery.getSorts()))
					{
						// we can set distinct
						QuerySelect copy = (QuerySelect)theQuery.shallowClone();
						copy.setDistinct(true);
						theQuery = copy;
					}
					else
					{
						// cannot set distinct, make sure we filter out duplicate records
						distinctInMemory = true;
					}
				}
			}

			return fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, theQuery, resultTypes,
				fsm.getTableFilterParams(sheet.getServerName(), theQuery), distinctInMemory, startRow, rowsToRetrieve, type);
		}
		catch (ServoyException e)
		{
			e.setContext(this.toString());
			throw e;
		}
		catch (CloneNotSupportedException e)
		{
			// should not happen
			throw new RuntimeException(e);
		}
	}

	private boolean hasTablefilterWithJoins(QuerySelect theQuery)
	{
		ArrayList<TableFilter> tableFilterParams = fsm.getTableFilterParams(sheet.getServerName(), theQuery);
		return tableFilterParams != null && tableFilterParams.stream()
			.map(TableFilter::getTableFilterdefinition)
			.filter(QueryTableFilterdefinition.class::isInstance)
			.map(QueryTableFilterdefinition.class::cast)
			.map(QueryTableFilterdefinition::getQuerySelect)
			.map(QuerySelect::getJoins)
			.anyMatch(Objects::nonNull);
	}

	/**
	 * helper function to get int value of java.lang.Numeber object or default value in case of null
	 * @param numberObject
	 * @param defaultValue
	 * @return
	 */
	private int getNumberAsInt(Number numberObject, int defaultValue)
	{
		return numberObject == null ? defaultValue : numberObject.intValue();
	}

	/**
	 * helper function to get boolean value of java.lang.Boolean object or default value in case of null
	 * @param booleanObject
	 * @param defaultValue
	 * @return
	 */
	private boolean getBooleanAsbool(Boolean booleanObject, boolean defaultValue)
	{
		return booleanObject == null ? defaultValue : booleanObject.booleanValue();
	}

	private class FoundSetIterator implements Iterator<IRecord>
	{
		private int currentIndex = -1;
		private Object[] currentPK = null;
		private final List<Object[]> processedPKS = new ArrayList<Object[]>();
		private PKDataSet pks = null;
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
			PKDataSet newPKs = getPksAndRecords().getPks();
			if (pks != null && newPKs != pks)
			{
				// pks set has changed, a foundset operation (like sort) occurred, cannot iterate anymore
				throw new RuntimeException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.invalidIteration")); //$NON-NLS-1$
			}
			pks = newPKs;
			int nextIndex = currentIndex + 1;
			IRecord nextRecord = FoundSet.this.getRecord(nextIndex);
			while (nextRecord instanceof PrototypeState)
			{
				nextIndex = currentIndex + 1;
				nextRecord = FoundSet.this.getRecord(nextIndex);
			}
			if (currentIndex >= 0)
			{
				IRecord tmpCurrentRecord = FoundSet.this.getRecord(currentIndex);
				if (tmpCurrentRecord == null || !equalObjects(tmpCurrentRecord.getPK(), currentPK))
				{
					// something is changed in the foundset, recalculate
					if (tmpCurrentRecord == null)
					{
						int size = FoundSet.this.getRawSize();
						if (size == 0)
						{
							return null;
						}
						currentIndex = size - 1;
						tmpCurrentRecord = FoundSet.this.getRecord(currentIndex);
					}
					if (!listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
					{
						// substract current index
						while (tmpCurrentRecord != null && !listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex - 1;
							tmpCurrentRecord = FoundSet.this.getRecord(currentIndex);
						}
						nextIndex = currentIndex + 1;
						nextRecord = FoundSet.this.getRecord(nextIndex);
					}
					else
					{
						// increment current index
						while (tmpCurrentRecord != null && listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex + 1;
							tmpCurrentRecord = FoundSet.this.getRecord(currentIndex);
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
					if (equalObjects(array, value))
					{
						return true;
					}
				}
			}
			return false;
		}

	}

	enum MethodCallState
	{
		READY, CALLING, DONE;
	}
}
