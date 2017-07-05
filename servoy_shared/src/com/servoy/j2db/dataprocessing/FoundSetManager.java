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


import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.SQLSheet.ConverterInfo;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.IQueryElement;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;
import com.servoy.j2db.querybuilder.impl.QBFactory;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.WeakHashSet;
import com.servoy.j2db.util.visitor.IVisitor;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Manager for foundsets
 * @author jblok
 */
public class FoundSetManager implements IFoundSetManagerInternal
{
	@SuppressWarnings("unchecked")
	private static final Entry<String, SoftReference<RelatedFoundSet>>[] EMPTY_ENTRY_ARRAY = new Map.Entry[0];

	private final IApplication application;
	private Map<Object, FoundSet> separateFoundSets; //FoundSetListener -> FoundSet ... 1 foundset per listener
	private Map<String, FoundSet> sharedDataSourceFoundSet; //dataSource -> FoundSet ... 1 foundset per data source
	private Set<FoundSet> foundSets;
	private WeakReference<IFoundSetInternal> noTableFoundSet;

	private Map<String, RowManager> rowManagers; //dataSource -> RowManager... 1 per table
	private Map<ITable, CopyOnWriteArrayList<ITableChangeListener>> tableListeners; //table -> ArrayList(tableListeners)
	protected SQLGenerator sqlGenerator;
	private GlobalTransaction globalTransaction;
	private IInfoListener infoListener;//we allow only one
	private final IFoundSetFactory foundsetfactory;
	private boolean createEmptyFoundsets = false;
	private Map<String, List<TableFilter>> tableFilterParams;//server -> ArrayList(TableFilter)
	private Map<String, ITable> inMemDataSources; // dataSourceUri -> temp table

	protected Map<String, ConcurrentMap<String, SoftReference<RelatedFoundSet>>> cachedSubStates;
	protected List<String> locks = new SortedList<String>(StringComparator.INSTANCE);

	private final GlobalFoundSetEventListener globalFoundSetEventListener = new GlobalFoundSetEventListener();

	private final EditRecordList editRecordList;

	public final int pkChunkSize;
	public final int chunkSize;
	public final int initialRelatedChunkSize;

	private final List<Runnable> fireRunabbles = new ArrayList<Runnable>();

	// tracking info used for logging
	private final HashMap<String, Object> trackingInfoMap = new HashMap<String, Object>();

	public FoundSetManager(IApplication app, IFoundSetFactory factory)
	{
		application = app;
		initMembers();
		editRecordList = new EditRecordList(this);

		foundsetfactory = factory;

		pkChunkSize = Utils.getAsInteger(app.getSettings().getProperty("servoy.foundset.pkChunkSize", Integer.toString(200)));//primarykeys to be get in one roundtrip //$NON-NLS-1$
		chunkSize = Utils.getAsInteger(app.getSettings().getProperty("servoy.foundset.chunkSize", Integer.toString(30)));//records to be get in one roundtrip //$NON-NLS-1$
		initialRelatedChunkSize = Utils.getAsInteger(app.getSettings().getProperty("servoy.foundset.initialRelatedChunkSize", Integer.toString(chunkSize * 2))); //initial related records to get in one roundtrip//$NON-NLS-1$
	}

	private Runnable createFlushAction(final String dataSource)
	{
		return new Runnable()
		{
			public void run()
			{
				if (dataSource == null)
				{
					Iterator<RowManager> it = rowManagers.values().iterator();
					while (it.hasNext())
					{
						RowManager element = it.next();
						element.flushAllCachedRows();
						fireTableEvent(element.getSQLSheet().getTable());
					}
					refreshFoundSetsFromDB();
				}
				else
				{
					RowManager element = rowManagers.get(dataSource);
					if (element != null)
					{
						element.flushAllCachedRows();
						refreshFoundSetsFromDB(dataSource, null, false);
						fireTableEvent(element.getSQLSheet().getTable());
					}
				}
			}
		};
	}

	//triggered by server call
	public void flushCachedDatabaseDataFromRemote(String dataSource)
	{
		runOnEditOrTransactionStoppedActions.add(createFlushAction(dataSource));
		performActionIfRequired();
	}

	public void flushCachedDatabaseData(String dataSource)
	{
		Runnable action = createFlushAction(dataSource);
		action.run();
	}

	/*
	 * Flush locally in the current client, for client plugins
	 *
	 * @see com.servoy.j2db.dataprocessing.IDatabaseManager#notifyDataChange(java.lang.String, com.servoy.j2db.dataprocessing.IDataSet, int)
	 */
	public boolean notifyDataChange(String dataSource, IDataSet pks, int action)
	{
		if (pks == null)
		{
			flushCachedDatabaseData(dataSource);
		}
		else
		{
			notifyDataChange(dataSource, pks, action, null);
		}
		return true;
	}

	private final List<Runnable> runOnEditOrTransactionStoppedActions = Collections.synchronizedList(new ArrayList<Runnable>(3));
	private final AtomicBoolean isBusy = new AtomicBoolean(false);

	void performActionIfRequired()
	{
		if (isBusy.getAndSet(true)) return;
		try
		{
			while (runOnEditOrTransactionStoppedActions.size() > 0 && !hasTransaction() && !editRecordList.isEditing())
			{
				try
				{
					runOnEditOrTransactionStoppedActions.remove(0).run();
				}
				catch (Exception e)
				{
					Debug.error("Exception calling remote flush action", e); //$NON-NLS-1$
				}
			}
		}
		finally
		{
			isBusy.set(false);
		}
	}

	public void refreshFoundSetsFromDB()
	{
		refreshFoundSetsFromDB(null, null, false);
	}

	/**
	 *
	 * @param table
	 * @param dataSource
	 * @param columnName when not null, only return true if the table has the column
	 */
	private boolean mustRefresh(ITable table, String dataSource, String columnName)
	{
		if (columnName != null && table instanceof Table && ((Table)table).getColumn(columnName) == null)
		{
			// does not have specified column not
			return false;
		}

		ITable dsTable = null;
		try
		{
			dsTable = getTable(dataSource);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}

		if (table != null && dsTable != null)
		{
			return (table.getServerName().equals(dsTable.getServerName()) && table.getName().equals(dsTable.getName()));
		}
		return true;
	}

	/**
	 * Used by rollback and flush table/all
	 * @param dataSource the datasource the foundsets must be build on to refresh (null then all)
	 * @param columnName when not null, only refresh foundsets on tables that have this column
	 * @param skipStopEdit If true then stop edit will not be called
	 * @return affected tables
	 */
	private Collection<ITable> refreshFoundSetsFromDB(String dataSource, String columnName, boolean skipStopEdit)
	{
		Set<ITable> affectedTables = new HashSet<ITable>();

		List<FoundSet> fslist = new ArrayList<FoundSet>(sharedDataSourceFoundSet.size() + separateFoundSets.size() + foundSets.size());
		fslist.addAll(sharedDataSourceFoundSet.values());
		fslist.addAll(separateFoundSets.values());
		fslist.addAll(foundSets);

		for (FoundSet fs : fslist)
		{
			try
			{
				if (mustRefresh(fs.getTable(), dataSource, columnName))
				{
					if (fs.isInitialized())
					{
						fs.refreshFromDB(false, skipStopEdit);
					}
					affectedTables.add(fs.getTable());
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		// Can't just clear substates!! if used in portal then everything is out of sync
//		if(server_name == null && table_name == null)
//		{
//			cachedSubStates.clear();
//		}
//		else
		for (ConcurrentMap<String, SoftReference<RelatedFoundSet>> map : cachedSubStates.values())
		{
			Map.Entry<String, SoftReference<RelatedFoundSet>>[] array = map.entrySet().toArray(EMPTY_ENTRY_ARRAY);
			for (Map.Entry<String, SoftReference<RelatedFoundSet>> entry : array)
			{
				SoftReference<RelatedFoundSet> sr = entry.getValue();
				RelatedFoundSet element = sr.get();
				if (element != null)
				{
					try
					{
						if (mustRefresh(element.getTable(), dataSource, columnName))
						{
							//element.refreshFromDB(false);
							// this call is somewhat different then a complete refresh from db.
							// The selection isn't tried to keep on the same pk
							// new records are really being flushed..
							element.invalidateFoundset();
							affectedTables.add(element.getTable());
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				else
				{
					map.remove(entry.getKey(), sr);
				}
			}
		}
		getEditRecordList().fireEvents();

		return affectedTables;
	}

	private Collection<ITable> getFilterUpdateAffectedTables(String dataSource, String columnName)
	{
		Collection<ITable> affectedtableList = refreshFoundSetsFromDB(dataSource, columnName, false);

		// also add tables that have listeners but no foundsets
		Iterator<ITable> tableListeneresIte = tableListeners.keySet().iterator();
		ITable tableKey;
		while (tableListeneresIte.hasNext())
		{
			tableKey = tableListeneresIte.next();
			if (!affectedtableList.contains(tableKey) && mustRefresh(tableKey, dataSource, columnName))
			{
				affectedtableList.add(tableKey);
			}
		}

		return affectedtableList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetManagerInternal#reloadFoundsetMethod(java.lang.String, com.servoy.j2db.persistence.IScriptProvider)
	 */
	public void reloadFoundsetMethod(String dataSource, IScriptProvider scriptMethod)
	{
		if (dataSource == null)
		{
			return;
		}

		for (Object element : sharedDataSourceFoundSet.values().toArray())
		{
			FoundSet fs = (FoundSet)element;
			try
			{
				if (dataSource.equals(fs.getDataSource()))
				{
					fs.reloadFoundsetMethod(scriptMethod);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		for (Object element : separateFoundSets.values().toArray())
		{
			FoundSet fs = (FoundSet)element;
			try
			{
				if (dataSource.equals(fs.getDataSource()))
				{
					fs.reloadFoundsetMethod(scriptMethod);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		for (Object element : foundSets.toArray())
		{
			FoundSet fs = (FoundSet)element;
			try
			{
				if (dataSource.equals(fs.getDataSource()))
				{
					fs.reloadFoundsetMethod(scriptMethod);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		for (ConcurrentMap<String, SoftReference<RelatedFoundSet>> map : cachedSubStates.values())
		{
			Map.Entry<String, SoftReference<RelatedFoundSet>>[] array = map.entrySet().toArray(EMPTY_ENTRY_ARRAY);
			for (Map.Entry<String, SoftReference<RelatedFoundSet>> entry : array)
			{
				SoftReference<RelatedFoundSet> sr = entry.getValue();
				RelatedFoundSet element = sr.get();
				if (element != null)
				{
					try
					{
						if (dataSource.equals(element.getDataSource()))
						{
							element.reloadFoundsetMethod(scriptMethod);
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				else
				{
					map.remove(entry.getKey(), sr);
				}
			}
		}
	}


	public void init()
	{
		editRecordList.init();
	}

	/**
	 * flushes/refreshes only related foundsets with a changed sql.
	 *
	 * @param caller
	 * @param relationName
	 * @param parentToIndexen
	 */
	public void flushRelatedFoundSet(IFoundSetInternal caller, String relationName)
	{
		ConcurrentMap<String, SoftReference<RelatedFoundSet>> hm = cachedSubStates.get(relationName);
		if (hm != null)
		{
			Map.Entry<String, SoftReference<RelatedFoundSet>>[] array = hm.entrySet().toArray(EMPTY_ENTRY_ARRAY);
			for (Map.Entry<String, SoftReference<RelatedFoundSet>> entry : array)
			{
				SoftReference<RelatedFoundSet> sr = entry.getValue();
				RelatedFoundSet element = sr.get();
				if (element != null)
				{
					//prevent callbacks by called test
					if (element != caller)
					{
						try
						{
							if (!element.creationSqlSelect.equals(element.getSqlSelect()))
							{
								element.invalidateFoundset();
							}
						}
						catch (Exception e)
						{
							Debug.error(e);
						}
					}
				}
				else
				{
					hm.remove(entry.getKey(), sr);
				}
			}
		}
	}

	public IFoundSetInternal getGlobalRelatedFoundSet(String name) throws RepositoryException, ServoyException
	{
		Relation relation = application.getFlattenedSolution().getRelation(name);
		if (relation != null && relation.isGlobal())
		{
			SQLSheet childSheet = getSQLGenerator().getCachedTableSQLSheet(relation.getForeignDataSource());
			// this returns quickly if it already has a sheet for that relation, but optimize further?
			getSQLGenerator().makeRelatedSQL(childSheet, relation);
			return getRelatedFoundSet(new PrototypeState(getSharedFoundSet(relation.getForeignDataSource())), childSheet, name,
				getDefaultPKSortColumns(relation.getForeignDataSource()));
		}
		return null;
	}

	/**
	 * Check if related foundset is loaded, relationName may be multiple-levels deep.
	 */
	protected boolean isRelatedFoundSetLoaded(IRecordInternal state, String relationName)
	{
		try
		{
			IRecordInternal rec = state;
			Relation[] relationSequence = application.getFlattenedSolution().getRelationSequence(relationName);
			for (int i = 0; relationSequence != null && i < relationSequence.length; i++)
			{
				IFoundSetInternal rfs = getRelatedFoundSetWhenLoaded(rec, relationSequence[i]);
				if (rfs == null)
				{
					return false;
				}
				if (i == relationSequence.length - 1 || rfs.getSize() == 0)
				{
					// no more relations to be loaded because rfs is empty, or at end of relation sequence
					return true;
				}
				rec = rfs.getRecord(rfs.getSelectedIndex());
			}
		}
		catch (Exception e)
		{
			Debug.error("is related foundset check failed", e); //$NON-NLS-1$
		}
		return false;
	}

	private RelatedFoundSet getRelatedFoundSetWhenLoaded(IRecordInternal state, Relation relation) throws RepositoryException
	{
		if (state == null || !Relation.isValid(relation, application.getFlattenedSolution()))
		{
			return null;
		}
		RelatedHashedArguments relatedArguments = calculateFKHash(state, relation, false);
		if (relatedArguments == null)
		{
			return null;
		}

		Map<String, SoftReference<RelatedFoundSet>> rfsMap = cachedSubStates.get(relation.getName());
		if (rfsMap != null)
		{
			SoftReference<RelatedFoundSet> sr = rfsMap.get(relatedArguments.hash);
			if (sr != null)
			{
				RelatedFoundSet rfs = sr.get();
				if (rfs != null && !rfs.mustQueryForUpdates() && !rfs.mustAggregatesBeLoaded())
				{
					return rfs;
				}
			}
		}
		return null;
	}

	//query for a substate
	protected IFoundSetInternal getRelatedFoundSet(IRecordInternal state, SQLSheet childSheet, String relationName, List<SortColumn> defaultSortColumns)
		throws ServoyException
	{
		IFoundSetInternal retval = null;
		Relation relation = application.getFlattenedSolution().getRelation(relationName);
		if (!Relation.isValid(relation, application.getFlattenedSolution()))
		{
			return null;
		}

		if (relation.isParentRef())
		{
			return state.getParentFoundSet();
		}

		RelatedHashedArguments relatedArguments = calculateFKHash(state, relation, false);
		if (relatedArguments == null)
		{
			return null;
		}

		ConcurrentMap<String, SoftReference<RelatedFoundSet>> rfs = cachedSubStates.get(relationName);
		if (rfs != null)
		{
			SoftReference<RelatedFoundSet> sr = rfs.get(relatedArguments.hash);
			if (sr != null)
			{
				retval = sr.get();
				if (retval == null && Debug.tracing())
					Debug.trace("-----------CacheMiss for related founset " + relationName + " for keys " + relatedArguments.hash); //$NON-NLS-1$ //$NON-NLS-2$
				//else Debug.trace("-----------CacheHit!! for related foundset " + relID + " for keys " + calcPKHashKey);
			}
		}

		List<RelatedHashedArguments> toFetch = null;
		if (retval == null)
		{
			String lockString = relationName + relatedArguments.hash;
			synchronized (locks)
			{
				rfs = cachedSubStates.get(relationName);
				if (rfs == null)
				{
					rfs = new ConcurrentHashMap<String, SoftReference<RelatedFoundSet>>();
					cachedSubStates.put(relationName, rfs);
				}
				while (locks.contains(lockString))
				{
					try
					{
						locks.wait();
					}
					catch (InterruptedException e)
					{
						Debug.error(e);
					}
				}
				SoftReference<RelatedFoundSet> sr = rfs.get(relatedArguments.hash);
				if (sr != null)
				{
					retval = sr.get();
				}


				if (retval == null)
				{
					locks.add(lockString);

					// pre-fetch a number of sibling related found sets
					toFetch = new ArrayList<RelatedHashedArguments>();
					toFetch.add(relatedArguments); // first to fetch is the one currently requested

					IFoundSetInternal parent = state.getParentFoundSet();
					int currIndex = parent.getRecordIndex(state);
					if (currIndex >= 0 && parent instanceof FoundSet)
					{
						int relatedChunkSize = chunkSize / 3;
						Object[] siblingRecords = ((FoundSet)parent).getPksAndRecords().getCachedRecords().toArray(); // take a snapshot of cachedRecords
						for (int s = currIndex + 1; s < siblingRecords.length && toFetch.size() < relatedChunkSize; s++)
						{
							IRecordInternal sibling = (IRecordInternal)siblingRecords[s];
							if (sibling != null)
							{
								RelatedHashedArguments extra = calculateFKHash(sibling, relation, true);
								if (extra != null)
								{
									SoftReference<RelatedFoundSet> srSibling = rfs.get(extra.hash);

									if (srSibling != null && srSibling.get() != null)
									{
										// already cached
										continue;
									}

									String extraLockString = relationName + extra.hash;
									if (!locks.contains(extraLockString))
									{
										locks.add(extraLockString);
										toFetch.add(extra);
									}
								}
							}
						}
					}
				}
			}

			if (retval == null)
			{
				RelatedFoundSet[] retvals = null;
				try
				{
					IRecordInternal[] states = new IRecordInternal[toFetch.size()];
					Object[][] whereArgsList = new Object[toFetch.size()][];
					for (int f = 0; f < toFetch.size(); f++)
					{
						RelatedHashedArguments relargs = toFetch.get(f);
						states[f] = relargs.state;
						whereArgsList[f] = relargs.whereArgs;
					}
					if (relation.getInitialSort() != null)
					{
						defaultSortColumns = getSortColumns(relation.getForeignDataSource(), relation.getInitialSort());
					}
					retvals = (RelatedFoundSet[])RelatedFoundSet.createRelatedFoundSets(foundsetfactory, this, states, relation, childSheet, whereArgsList,
						defaultSortColumns);
					retval = retvals[0];// the first query is the one requested now, the rest is pre-fetch
				}
				finally
				{
					synchronized (locks)
					{
						for (int f = 0; f < toFetch.size(); f++)
						{
							RelatedHashedArguments relargs = toFetch.get(f);
							if (retvals != null)
							{
								rfs.put(relargs.hash, new SoftReference<RelatedFoundSet>(retvals[f]));
							}
							locks.remove(relationName + relargs.hash);
						}

						locks.notifyAll();
					}
				}

				// inform global foundset event listeners that a new foundset has been created
				globalFoundSetEventListener.foundSetsCreated(retvals);

				// run runnables for firing events after foundsets have been created
				if (fireRunabbles.size() > 0)
				{
					Runnable[] runnables;
					synchronized (fireRunabbles)
					{
						runnables = new ArrayList<Runnable>(fireRunabbles).toArray(new Runnable[fireRunabbles.size()]);
						fireRunabbles.clear();
					}
					for (Runnable runnable : runnables)
					{
						runnable.run();
					}
				}
			}
		}
		return retval;
	}

	private RelatedHashedArguments calculateFKHash(IRecordInternal state, Relation r, boolean testForCalcs) throws RepositoryException
	{
		Object[] whereArgs = getRelationWhereArgs(state, r, testForCalcs);
		if (whereArgs == null)
		{
			return null;
		}

		return new RelatedHashedArguments(state, whereArgs, RowManager.createPKHashKey(whereArgs));
	}

	/**
	 * Get relation where-args, not using column converters
	 * @param state
	 * @param relation
	 * @param testForCalcs
	 * @return
	 * @throws RepositoryException
	 */
	public Object[] getRelationWhereArgs(IRecordInternal state, Relation relation, boolean testForCalcs) throws RepositoryException
	{
		boolean isNull = true;
		IDataProvider[] args = relation.getPrimaryDataProviders(application.getFlattenedSolution());
		Column[] columns = relation.getForeignColumns(application.getFlattenedSolution());
		Object[] array = new Object[args.length];
		for (int i = 0; i < args.length; i++)
		{
			Object value = null;
			if (args[i] instanceof LiteralDataprovider)
			{
				value = ((LiteralDataprovider)args[i]).getValue();
			}
			else if (args[i] instanceof EnumDataProvider)
			{
				value = getScopesScopeProvider().getDataProviderValue(args[i].getDataProviderID());
			}
			else
			{
				String dataProviderID = args[i].getDataProviderID();
				if (testForCalcs && state.getRawData().containsCalculation(dataProviderID) && state.getRawData().mustRecalculate(dataProviderID, true))
				{
					// just return null if a calc is found that also have to be recalculated.
					// else this can just cascade through..
					return null;
				}
				value = state.getValue(dataProviderID, false); // unconverted (todb value)
			}
			if (value != Scriptable.NOT_FOUND)
			{
				array[i] = columns[i].getAsRightType(value);
			}
			if (array[i] != null)
			{
				isNull = false;
			}
			else
			{
				// If it is a column that is null then always set isNull on true and break.
				// Because null columns can't have a relation.
				if (args[i] instanceof IColumn)
				{
					return null;
				}
				if (isNull)
				{
					isNull = !(args[i] instanceof ScriptVariable);
				}
			}
		}

		if (isNull) return null; //optimize for null keys (multiple all null!) but not empty pk (db ident)

		return array;
	}

	public void flushCachedItems()
	{
		trackingInfoMap.clear();
		//just to make sure
		rollbackTransaction(true, true, true);
		releaseAllLocks(null);

		createEmptyFoundsets = false;
		for (RowManager rm : rowManagers.values())
		{
			rm.dispose();
		}
		initMembers();
		sqlGenerator = null;
		scopesScopeProvider = null;
		editRecordList.init();
	}

	private void initMembers()
	{
		sharedDataSourceFoundSet = new ConcurrentHashMap<String, FoundSet>(64);
		separateFoundSets = Collections.synchronizedMap(new WeakHashMap<Object, FoundSet>(32));
		foundSets = Collections.synchronizedSet(new WeakHashSet<FoundSet>(64));
		noTableFoundSet = null;

		rowManagers = new ConcurrentHashMap<String, RowManager>(64);
		tableListeners = new ConcurrentHashMap<ITable, CopyOnWriteArrayList<ITableChangeListener>>(16);
		tableFilterParams = new ConcurrentHashMap<String, List<TableFilter>>();

		cachedSubStates = new ConcurrentHashMap<String, ConcurrentMap<String, SoftReference<RelatedFoundSet>>>(128);
		inMemDataSources = new ConcurrentHashMap<String, ITable>();
	}

	/**
	 * This calls flush on the sql sheet of that table only used when developing the solution.
	 */
	public void flushSQLSheet(String dataSource)
	{
		try
		{
			SQLSheet cachedTableSQLSheet = getSQLGenerator().getCachedTableSQLSheet(dataSource);
			if (cachedTableSQLSheet != null) cachedTableSQLSheet.flush(application, null);
		}
		catch (ServoyException e)
		{
			Debug.error(e);
		}
	}

	/**
	 * This calls flush on the sql sheet of that table only used when developing the solution.
	 */
	public void flushSQLSheet(Relation relation)
	{
		try
		{
			SQLSheet cachedTableSQLSheet = getSQLGenerator().getCachedTableSQLSheet(relation.getPrimaryDataSource());
			if (cachedTableSQLSheet != null) cachedTableSQLSheet.flush(application, relation);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private ScopesScopeProvider scopesScopeProvider;

	public IGlobalValueEntry getScopesScopeProvider()
	{
		if (scopesScopeProvider == null)
		{
			scopesScopeProvider = new ScopesScopeProvider(application.getScriptEngine().getScopesScope());
		}
		return scopesScopeProvider;
	}

	public SQLGenerator getSQLGenerator()
	{
		if (sqlGenerator == null)
		{
			sqlGenerator = new SQLGenerator(application);
		}
		return sqlGenerator;
	}

	public IDataServer getDataServer()
	{
		return application.getDataServer();
	}

	public IApplication getApplication()
	{
		return application;
	}

	public IExecutingEnviroment getScriptEngine()
	{
		return application.getScriptEngine();
	}

	public synchronized RowManager getRowManager(String dataSource) throws ServoyException
	{
		if (getDataServer() == null)
		{
			// no data access yet
			return null;
		}
		ITable t = getTable(dataSource);
		if (t != null)
		{
			RowManager rm = rowManagers.get(dataSource);
			if (rm == null)
			{
				try
				{
					// first time this client uses this table
					getDataServer().addClientAsTableUser(application.getClientID(), t.getServerName(), t.getName());
				}
				catch (RemoteException e)
				{
					throw new RepositoryException(e);
				}
				rm = new RowManager(this, getSQLGenerator().getCachedTableSQLSheet(dataSource));
				rowManagers.put(dataSource, rm);
			}
			return rm;
		}
		return null;
	}

	ITable getTable(IFoundSetListener l)
	{
		String dataSource = l.getDataSource();
		if (dataSource == null)
		{
			return null;
		}
		ITable table = inMemDataSources.get(dataSource);
		if (table == null)
		{
			return l.getTable();
		}
		return table;
	}

	@SuppressWarnings("nls")
	public ITable getTable(String dataSource) throws RepositoryException
	{
		if (dataSource == null)
		{
			return null;
		}
		if (application.getSolution() == null)
		{
			if (Debug.tracing())
			{
				Debug.trace("Trying to get a table for a datasource: " + dataSource + " on an already closed solution", new RuntimeException());
			}
			return null;
		}
		ITable table = inMemDataSources.get(dataSource);
		if (table == null)
		{
			// when it is a db:/server/table data source
			String[] servernameTablename = DataSourceUtilsBase.getDBServernameTablename(dataSource);
			if (servernameTablename != null && servernameTablename[0] != null)
			{
				try
				{
					IServer server = application.getSolution().getServer(servernameTablename[0]);
					if (server == null)
					{
						throw new RepositoryException(Messages.getString("servoy.exception.serverNotFound", new Object[] { servernameTablename[0] })); //$NON-NLS-1$
					}

					table = server.getTable(servernameTablename[1]);
				}
				catch (RemoteException e)
				{
					throw new RepositoryException(e);
				}
			}
			else if (DataSourceUtils.getDataSourceServerName(dataSource) == IServer.INMEM_SERVER)
			{
				if (!inMemDataSources.containsKey(dataSource) && dataSourceExists(dataSource))
				{
					try
					{
						createDataSourceFromDataSet(DataSourceUtils.getDataSourceTableName(dataSource), new BufferedDataSet(), null, null, false);
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				return inMemDataSources.get(dataSource);
			}
		}
		return table;
	}

	public boolean dataSourceExists(String dataSource) throws RepositoryException
	{
		if (DataSourceUtils.getDataSourceServerName(dataSource) == IServer.INMEM_SERVER)
		{
			if (inMemDataSources.containsKey(dataSource))
			{
				return true;
			}
			ServoyJSONObject columnsDef = null;
			Iterator<TableNode> tblIte = application.getFlattenedSolution().getTableNodes(dataSource);
			while (tblIte.hasNext() && columnsDef == null)
			{
				TableNode tn = tblIte.next();
				columnsDef = tn.getColumns();
			}

			if (columnsDef != null)
			{
				return true;
			}
			return false;
		}
		else
		{
			return getTable(dataSource) != null;
		}
	}

	public Collection<String> getInMemDataSourceNames()
	{
		List<String> inMemDataSourceNames = new ArrayList<>(inMemDataSources.size());
		for (String dataSource : inMemDataSources.keySet())
		{
			inMemDataSourceNames.add(DataSourceUtils.getInmemDataSourceName(dataSource));
		}
		return inMemDataSourceNames;
	}

	public String getDataSource(ITable table)
	{
		if (table == null)
		{
			return null;
		}

		return table.getDataSource();
	}

	public Relation getRelation(String relationName)
	{
		return getApplication().getFlattenedSolution().getRelation(relationName);
	}

	/**
	 * Find the data source of the table with given sql name in same server as serverDataSource
	 */
	public String resolveDataSource(String serverDataSource, String tableSQLName)
	{
		ITable serverTable = null;
		try
		{
			serverTable = getTable(serverDataSource);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		if (serverTable == null)
		{
			return null;
		}

		ITable table = null;
		try
		{
			IServer server = application.getSolution().getServer(serverTable.getServerName());
			if (server != null)
			{
				table = server.getTableBySqlname(tableSQLName);
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}

		if (table == null)
		{
			return null;
		}
		return table.getDataSource();
	}

	public void addFoundSetListener(IFoundSetListener l) throws ServoyException
	{
		giveMeFoundSet(l);
	}

	public TableFilter createTableFilter(String name, String serverName, ITable table, String dataprovider, String operator, Object value)
		throws ServoyException
	{
		if (dataprovider == null || operator == null) return null;

		int op = RelationItem.getValidOperator(operator.trim(), IBaseSQLCondition.ALL_DEFINED_OPERATORS, IBaseSQLCondition.ALL_MODIFIERS);
		if (op == -1)
		{
			return null;
		}

		return createTableFilter(name, serverName, table, dataprovider.trim(), op, value);
	}

	public TableFilter createTableFilter(String name, String serverName, ITable table, String dataprovider, int operator, Object val) throws ServoyException
	{
		if (table != null && ((Table)table).getColumn(dataprovider) == null)
		{
			return null;
		}

		Object value = val;
		if (value instanceof Wrapper)
		{
			value = ((Wrapper)value).unwrap();
		}

		if (table != null)
		{
			Column column = ((Table)table).getColumn(dataprovider);
			if (column == null)
			{
				return null;
			}
			value = convertFilterValue(table, column, value);
		}
		return new TableFilter(name, serverName, table == null ? null : table.getName(), table == null ? null : table.getSQLName(), dataprovider, operator,
			value);
	}

	public Object convertFilterValue(ITable table, Column column, Object value) throws ServoyException
	{
		ConverterInfo columnConverterInfo = getSQLGenerator().getCachedTableSQLSheet(table.getDataSource()).getColumnConverterInfo(column.getDataProviderID());
		if (columnConverterInfo != null)
		{
			IColumnConverter columnConverter = application.getFoundSetManager().getColumnConverterManager().getConverter(columnConverterInfo.converterName);
			if (columnConverter != null)
			{
				Object[] array = null;
				if (value instanceof List< ? >)
				{
					array = ((List< ? >)value).toArray();
				}
				else if (value != null && value.getClass().isArray())
				{
					array = ((Object[])value).clone();
				}

				if (array != null)
				{
					for (int i = 0; i < array.length; i++)
					{
						array[i] = SQLGenerator.convertFromObject(application, columnConverter, columnConverterInfo, column.getDataProviderID(),
							column.getDataProviderType(), array[i], false);
					}
					return array;
				}

				if (value == null || !SQLGenerator.isSelectQuery(value.toString()))
				{
					return SQLGenerator.convertFromObject(application, columnConverter, columnConverterInfo, column.getDataProviderID(),
						column.getDataProviderType(), value, false);
				}
				// else add as subquery
			}
		}
		return value;
	}

	public boolean addTableFilterParam(String filterName, String serverName, ITable table, String dataprovider, String operator, Object value)
		throws ServoyException
	{
		TableFilter filter = createTableFilter(filterName, serverName, table, dataprovider, operator, value);
		if (filter == null)
		{
			application.reportJSError("Table filter not created, column not found in table or operator invalid, filterName = '" + filterName +
				"', serverName = '" + serverName + "', table = '" + table + "', dataprovider = '" + dataprovider + "', operator = '" + operator + "'", null);
			return false;
		}

		List<TableFilter> params = tableFilterParams.get(serverName);
		if (params == null)
		{
			tableFilterParams.put(serverName, params = new ArrayList<TableFilter>());
		}

		if (!filter.isContainedIn(params)) // do not add the same filter, will add same AND-condition anyway
		{
			params.add(filter);

			for (ITable affectedtable : getFilterUpdateAffectedTables(getDataSource(table), filter.getDataprovider()))
			{
				fireTableEvent(affectedtable);
			}
		}

		if (Messages.isI18NTable(serverName, table != null ? table.getName() : null, application))
		{
			((ClientState)application).refreshI18NMessages();
		}

		return true;
	}

	public boolean removeTableFilterParam(String serverName, String filterName)
	{
		List<TableFilter> params = tableFilterParams.get(serverName);
		List<TableFilter> removedFilters = new ArrayList<TableFilter>();
		if (params != null)
		{
			Iterator<TableFilter> iterator = params.iterator();
			while (iterator.hasNext())
			{
				TableFilter f = iterator.next();
				if (filterName.equals(f.getName()))
				{
					iterator.remove();
					removedFilters.add(f);
				}
			}

			Set<ITable> firedTables = new HashSet<ITable>();
			for (TableFilter filter : removedFilters)
			{
				String dataSource;
				if (filter.getTableName() == null)
				{
					dataSource = null;
				}
				else
				{
					dataSource = DataSourceUtils.createDBTableDataSource(filter.getServerName(), filter.getTableName());
				}

				for (ITable affectedtable : getFilterUpdateAffectedTables(dataSource, filter.getDataprovider()))
				{
					if (firedTables.add(affectedtable))
					{
						fireTableEvent(affectedtable);
					}
				}
			}
		}

		return removedFilters.size() > 0;
	}

	public Object[][] getTableFilterParams(String serverName, String filterName)
	{
		List<TableFilter> params = tableFilterParams.get(serverName);
		List<Object[]> result = new ArrayList<Object[]>();
		if (params != null)
		{
			Iterator<TableFilter> iterator = params.iterator();
			while (iterator.hasNext())
			{
				TableFilter f = iterator.next();
				if (filterName == null || filterName.equals(f.getName()))
				{
					result.add(
						new Object[] { f.getTableName(), f.getDataprovider(), RelationItem.getOperatorAsString(f.getOperator()), f.getValue(), f.getName() });
				}
			}
		}

		return result.toArray(new Object[result.size()][]);
	}

	/**
	 * Get the table filters that are applicable on the sql for the server. Returns an array of table filters, the resulting array may be modified by the
	 * caller.
	 *
	 * @param serverName
	 * @param sql
	 * @return
	 */
	public ArrayList<TableFilter> getTableFilterParams(String serverName, IQueryElement sql)
	{
		final List<TableFilter> serverFilters = tableFilterParams.get(serverName);
		if (serverFilters == null)
		{
			return null;
		}

		// get the sql table names in the query
		final Set<String> tableSqlNames = new HashSet<String>();
		// find the filters for the tables found in the query
		final ArrayList<TableFilter>[] filters = new ArrayList[] { null };
		sql.acceptVisitor(new IVisitor()
		{
			public Object visit(Object o)
			{
				if (o instanceof QueryTable && tableSqlNames.add(((QueryTable)o).getName()))
				{
					QueryTable qTable = (QueryTable)o;
					for (TableFilter filter : serverFilters)
					{
						TableFilter useFilter = null;
						if (filter.getTableName() == null)
						{
							// filter is on all tables with specified dataProvider as column
							try
							{
								if (qTable.getDataSource() != null)
								{
									Table table = (Table)getTable(qTable.getDataSource());
									if (table == null)
									{
										// should never happen
										throw new RuntimeException("Could not find table '" + qTable.getDataSource() + "' for table filters");
									}
									Column column = table.getColumn(filter.getDataprovider());
									if (column != null)
									{
										// Use filter with table name filled in.
										// When table was null value was not yet converted, convert now.
										Object value = convertFilterValue(table, column, filter.getValue());
										useFilter = new TableFilter(filter.getName(), filter.getServerName(), table.getName(), table.getSQLName(),
											filter.getDataprovider(), filter.getOperator(), value);
									}
								}
							}
							catch (Exception e)
							{
								// big trouble, this is security filtering, so bail out on error
								throw new RuntimeException(e);
							}
						}
						else if (filter.getTableSQLName().equals(qTable.getName()))
						{
							useFilter = filter;
						}

						if (useFilter != null)
						{
							if (filters[0] == null)
							{
								filters[0] = new ArrayList<TableFilter>();
							}
							filters[0].add(useFilter);
						}
					}
				}
				return o;
			}
		});

		return filters[0];
	}

	/**
	 * Checks if the specified table has filter defined
	 *
	 * @param serverName
	 * @param tableName
	 * @return true if there is a filter defined for the table, otherwise false
	 */
	public boolean hasTableFilter(String serverName, String tableName)
	{
		if (serverName == null || tableName == null) return false;
		List<TableFilter> serverFilters = tableFilterParams.get(serverName);
		if (serverFilters == null) return false;

		Iterator<TableFilter> serverFiltersIte = serverFilters.iterator();
		TableFilter tableFilter;
		while (serverFiltersIte.hasNext())
		{
			tableFilter = serverFiltersIte.next();
			if (tableName.equals(tableFilter.getTableName())) return true;
		}

		return false;
	}

	public IFoundSetInternal getSeparateFoundSet(IFoundSetListener l, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (l.getDataSource() == null)
		{
			return getNoTableFoundSet();
		}
		else
		{
			// make sure inmem table is created
			getTable(l.getDataSource());
		}

		FoundSet foundset = l.getSharedFoundsetName() != null ? separateFoundSets.get(l.getSharedFoundsetName()) : separateFoundSets.get(l);
		if (foundset == null)
		{
			foundset = createSeparateFoundset(l.getDataSource(), l.getSharedFoundsetName() != null ? l.getSharedFoundsetName() : l, defaultSortColumns);
		}
		return foundset;
	}

	private FoundSet createSeparateFoundset(String datasource, Object key, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		SQLSheet sheet = getSQLGenerator().getCachedTableSQLSheet(datasource);
		FoundSet foundset = (FoundSet)foundsetfactory.createFoundSet(this, sheet, null, defaultSortColumns);
		if (createEmptyFoundsets) foundset.clear();
		separateFoundSets.put(key, foundset);
		// inform global foundset event listeners that a new foundset has been created
		globalFoundSetEventListener.foundSetCreated(foundset);
		return foundset;
	}

	@Override
	public IFoundSet getNamedFoundSet(String name) throws ServoyException
	{
		if (name == null) throw new RuntimeException("can't ask for a named foundset with a null name");
		IFoundSet foundset = separateFoundSets.get(name);
		if (foundset == null)
		{
			Iterator<Form> forms = application.getFlattenedSolution().getForms(false);
			while (forms.hasNext())
			{
				Form form = forms.next();
				if (name.equals(form.getSharedFoundsetName()))
				{
					foundset = createSeparateFoundset(form.getDataSource(), name, getSortColumns(form.getDataSource(), form.getInitialSort()));
					break;
				}
			}
		}
		return foundset;
	}

	public IFoundSetInternal getSharedFoundSet(String dataSource, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (dataSource == null || !application.getFlattenedSolution().isMainSolutionLoaded())
		{
			return getNoTableFoundSet();
		}
		else
		{
			// make sure inmem table is created
			getTable(dataSource);
		}

		FoundSet foundset = sharedDataSourceFoundSet.get(dataSource);
		if (foundset == null)
		{
			SQLSheet sheet = getSQLGenerator().getCachedTableSQLSheet(dataSource);
			foundset = (FoundSet)foundsetfactory.createFoundSet(this, sheet, null, defaultSortColumns);
			if (createEmptyFoundsets) foundset.clear();
			sharedDataSourceFoundSet.put(dataSource, foundset);
			// inform global foundset event listeners that a new foundset has been created
			globalFoundSetEventListener.foundSetCreated(foundset);
		}
		return foundset;
	}

	private IFoundSetInternal getNoTableFoundSet() throws ServoyException
	{
		IFoundSetInternal foundSet = noTableFoundSet == null ? null : noTableFoundSet.get();
		if (foundSet == null)
		{
			SQLSheet sheet = getSQLGenerator().getCachedTableSQLSheet(null);
			foundSet = foundsetfactory.createFoundSet(this, sheet, null, null);
			noTableFoundSet = new WeakReference<IFoundSetInternal>(foundSet);
		}
		return foundSet;
	}

	public IFoundSetInternal getSharedFoundSet(String dataSource) throws ServoyException
	{
		return getSharedFoundSet(dataSource, getDefaultPKSortColumns(dataSource));
	}

	public IFoundSet getNewFoundSet(String dataSource) throws ServoyException
	{
		return getNewFoundSet(dataSource, null, getDefaultPKSortColumns(dataSource));
	}

	public IFoundSetInternal getNewFoundSet(ITable table, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		return getNewFoundSet(getDataSource(table), pkSelect, defaultSortColumns);
	}

	/**
	 * @param defaultSortColumns: when null: use sorting defined in query
	 */
	public IFoundSetInternal getNewFoundSet(String dataSource, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (dataSource == null)
		{
			return getNoTableFoundSet();
		}
		SQLSheet sheet = getSQLGenerator().getCachedTableSQLSheet(dataSource);
		FoundSet foundset = (FoundSet)foundsetfactory.createFoundSet(this, sheet, pkSelect, defaultSortColumns);
		if (createEmptyFoundsets) foundset.clear();
		foundSets.add(foundset);
		// inform global foundset event listeners that a new foundset has been created
		globalFoundSetEventListener.foundSetCreated(foundset);
		return foundset;
	}

	public void giveMeFoundSet(IFoundSetListener l) throws ServoyException
	{
		IFoundSetInternal set = null;
		if (l.getDataSource() == null || l.wantSharedFoundSet())
		{
			String wantedGlobalRelationName = l.getGlobalRelationNamedFoundset(); // form is set on using a global relation through namedFoundset property
			if (wantedGlobalRelationName != null)
			{
				set = getGlobalRelatedFoundSet(wantedGlobalRelationName);
				if (set == null || !Solution.areDataSourcesCompatible(application.getRepository(), set.getDataSource(), l.getDataSource()))
				{
					throw new RepositoryException("Cannot create global relation namedFoundset '" + wantedGlobalRelationName + //$NON-NLS-1$
						"' - please check relation"); //$NON-NLS-1$
				}
			}
			else
			{
				set = getSharedFoundSet(l.getDataSource(), l.getDefaultSortColumns());
			}
		}
		else
		{
			set = getSeparateFoundSet(l, l.getDefaultSortColumns());
		}
		l.newValue(new FoundSetEvent(set, FoundSetEvent.NEW_FOUNDSET, FoundSetEvent.CHANGE_UPDATE));
	}

	public void removeFoundSetListener(IFoundSetListener l)
	{
	}

	public boolean isShared(IFoundSet set)
	{
		if (set == null) return false;
		if (set instanceof RelatedFoundSet) return false;

		SQLSheet sheet = ((IFoundSetInternal)set).getSQLSheet();
		if (sheet != null)
		{
			Table table = sheet.getTable();
			if (table != null)
			{
				return set.equals(sharedDataSourceFoundSet.get(getDataSource(table)));
			}
		}
		return false;
	}

	public boolean isNew(IFoundSet set)
	{
		if (set == null) return false;
		if (set instanceof RelatedFoundSet) return false;

		return foundSets.contains(set);
	}

/*
 * _____________________________________________________________ Methods for informing valuelists about table contents changes
 */
	public void addTableListener(ITable table, ITableChangeListener l)
	{
		CopyOnWriteArrayList<ITableChangeListener> list = tableListeners.get(table);
		if (list == null)
		{
			list = new CopyOnWriteArrayList<ITableChangeListener>();
			tableListeners.put(table, list);
		}
		list.add(l);
	}

	public void removeTableListener(ITable table, ITableChangeListener l)
	{
		CopyOnWriteArrayList<ITableChangeListener> list = tableListeners.get(table);
		if (list != null)
		{
			list.remove(l);
		}
	}

	void notifyChange(Table table)
	{
		fireTableEvent(table);
	}

	private void fireTableEvent(ITable table)
	{
		final CopyOnWriteArrayList<ITableChangeListener> list = tableListeners.get(table);
		if (list != null && list.size() > 0)
		{
			Runnable runnable = new Runnable()
			{
				public void run()
				{
					TableEvent e = new TableEvent(this);
					for (ITableChangeListener l : list)
					{
						l.tableChange(e);
					}
				}
			};
			if (application.isEventDispatchThread())
			{
				runnable.run();
			}
			else
			{
				application.invokeLater(runnable);
			}
		}
	}

	public static String getSortColumnsAsString(List<SortColumn> list)
	{
		StringBuilder sb = new StringBuilder();
		if (list != null)
		{
			for (int i = 0; i < list.size(); i++)
			{
				SortColumn sc = list.get(i);
				sb.append(sc.toString());
				if (i < list.size() - 1) sb.append(", "); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	public List<SortColumn> getSortColumns(ITable t, String options)
	{
		List<SortColumn> list = new ArrayList<SortColumn>(3);
		if (t == null) return list;
		if (options != null)
		{
			try
			{
				StringTokenizer tk = new StringTokenizer(options, ","); //$NON-NLS-1$
				while (tk.hasMoreTokens())
				{
					String columnName = null;
					String order = null;
					String def = tk.nextToken().trim();
					int index = def.indexOf(" "); //$NON-NLS-1$
					if (index != -1)
					{
						columnName = def.substring(0, index);
						order = def.substring(index + 1);
					}
					else
					{
						columnName = def;
					}
					if (columnName != null)
					{
						SortColumn sc = getSortColumn(t, Utils.toEnglishLocaleLowerCase(columnName), true);
						if (sc != null)
						{
							if (order != null && order.trim().toLowerCase().startsWith("desc")) //$NON-NLS-1$
							{
								sc.setSortOrder(SortColumn.DESCENDING);
							}
							list.add(sc);
						}
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		if (list.size() == 0)
		{
			// default pk sort
			try
			{
				return getDefaultPKSortColumns(t.getDataSource());
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
		return list;
	}

	public List<SortColumn> getSortColumns(String dataSource, String options) throws RepositoryException
	{
		return getSortColumns(getTable(dataSource), options);
	}


	public SortColumn getSortColumn(ITable table, String dataProviderID, boolean logIfCannotBeResolved) throws RepositoryException
	{
		if (table == null || dataProviderID == null) return null;

		Table lastTable = (Table)table;
		List<Relation> relations = new ArrayList<Relation>();
		String[] split = dataProviderID.split("\\."); //$NON-NLS-1$
		for (int i = 0; i < split.length - 1; i++)
		{
			Relation r = application.getFlattenedSolution().getRelation(split[i]);
			String reason = null;
			if (r == null)
			{
				reason = "relation '" + split[i] + "' not found";
			}
			else if (!Relation.isValid(r, application.getFlattenedSolution()) || !r.isUsableInSort())
			{
				if (!Relation.isValid(r, application.getFlattenedSolution())) reason = "relation '" + split[i] + "' not valid";
				else if (r.isMultiServer()) reason = "relation '" + split[i] + "' is cross server, sorting is not supported";
				else if (r.isGlobal()) reason = "relation '" + split[i] + "' is global, sorting is not supported";
				else reason = "relation '" + split[i] + "' is outer join with or null modifier, sorting is not supported";
			}
			else if (!lastTable.equals(getTable(r.getPrimaryDataSource())))
			{
				reason = "table '" + lastTable.getName() + "' does not match with relation '" + split[i] + "'primary table";
			}
			if (reason != null)
			{
				if (logIfCannotBeResolved) Debug.log("Cannot sort on dataprovider " + dataProviderID + ", " + reason, new Exception(split[i]));
				return null;
			}
			relations.add(r);
			lastTable = (Table)getTable(r.getForeignDataSource());
		}

		String colName = split[split.length - 1];
		IColumn c = lastTable.getColumn(colName);
		if (c == null)
		{
			// check for aggregate
			c = AbstractBase.selectByName(application.getFlattenedSolution().getAggregateVariables(lastTable, false), colName);
		}
		if (c != null)
		{
			return new SortColumn(c, relations.size() == 0 ? null : relations.toArray(new Relation[relations.size()]));
		}
		return null;
	}

/*
 * _____________________________________________________________ locking methods
 */
	//index == -1 is (current) selected record,< -1 is all records
	public boolean acquireLock(IFoundSet fs, int index, String lockName)
	{
		if (fs instanceof IFoundSetInternal)
		{
			IFoundSetInternal foundSet = (IFoundSetInternal)fs;
			if (foundSet.getSQLSheet().getTable() == null)
			{
				return false;
			}
			Map<Object, Object[]> pkhashkeys = new HashMap<Object, Object[]>();
			if (index == -1)
			{
				int idx = foundSet.getSelectedIndex();
				if (idx >= 0 && idx < foundSet.getSize())
				{
					IRecordInternal rec = foundSet.getRecord(idx);
					if (rec == null || rec.getRawData() == null) return false;//just for safety
					if (!rec.getRawData().lockedByMyself()) pkhashkeys.put(rec.getPKHashKey(), rec.getPK());
				}
				else
				{
					return false;//wrong index
				}
			}
			else if (index < -1)
			{
				for (int i = 0; i < foundSet.getSize(); i++)
				{
					IRecordInternal rec = foundSet.getRecord(i);
					if (rec == null || rec.getRawData() == null) return false;//just for safety
					if (!rec.getRawData().lockedByMyself()) pkhashkeys.put(rec.getPKHashKey(), rec.getPK());
				}
			}
			else if (index >= 0)
			{
				if (index < foundSet.getSize())
				{
					IRecordInternal rec = foundSet.getRecord(index);
					if (rec == null || rec.getRawData() == null) return false;//just for safety
					if (!rec.getRawData().lockedByMyself()) pkhashkeys.put(rec.getPKHashKey(), rec.getPK());
				}
				else
				{
					return false;//wrong index
				}
			}
			else
			{
				return false;//unknown index
			}

			if (pkhashkeys.size() == 0) //optimize
			{
				return true;
			}

			Table table = (Table)foundSet.getTable();
			if (table != null)
			{
				String server_name = foundSet.getSQLSheet().getServerName();
				String table_name = foundSet.getSQLSheet().getTable().getName();
				RowManager rm = rowManagers.get(DataSourceUtils.createDBTableDataSource(server_name, table_name));
				//process
				Set<Object> keySet = pkhashkeys.keySet();
				Set<Object> ids = new HashSet<Object>(keySet);//make copy because it is not serialized in developer and set is emptied
				QuerySelect lockSelect = SQLGenerator.createUpdateLockSelect(table, pkhashkeys.values().toArray(new Object[pkhashkeys.size()][]),
					hasTransaction() && Boolean.parseBoolean(application.getSettings().getProperty("servoy.record.lock.lockInDB", "false"))); //$NON-NLS-1$ //$NON-NLS-2$
				if (rm != null)
				{
					if (rm.acquireLock(application.getClientID(), lockSelect, lockName, ids))
					{
						if (infoListener != null) infoListener.showLocksStatus(true);
						// success
						return true;
					}
				}
			}

		}
		return false;
	}

	public boolean releaseAllLocks(String lockName)
	{
		boolean allReleased = true;
		boolean hasLocks = false;
		for (RowManager rm : rowManagers.values())
		{
			Set<Object> pkhashkeys = rm.getOwnLocks(lockName);
			if (pkhashkeys.size() != 0)
			{
				try
				{
					if (((ILockServer)getDataServer()).releaseLocks(application.getClientID(), rm.getSQLSheet().getServerName(),
						rm.getSQLSheet().getTable().getName(), pkhashkeys))
					{
						rm.removeLocks(pkhashkeys);
					}
					else
					{
						allReleased = false;
						hasLocks = true;
					}
				}
				catch (RepositoryException e)
				{
					// Will not happen
					Debug.error(e);
					return false;
				}
				catch (RemoteException e)
				{
					Debug.error(e);//TODO:put error code in app
					allReleased = false;
					hasLocks = true;
				}
			}
			if (infoListener != null && !hasLocks && lockName != null)
			{
				// check if some other locks are remaining
				hasLocks = rm.hasOwnLocks(null);
			}
		}
		if (infoListener != null) infoListener.showLocksStatus(hasLocks);
		return allReleased;
	}

	public boolean hasLocks(String lockName)
	{
		for (RowManager rm : rowManagers.values())
		{
			if (rm.hasOwnLocks(lockName))
			{
				return true;
			}
		}
		return false;
	}

/*
 * _____________________________________________________________ transaction methods
 */
	public void startTransaction()
	{
		if (globalTransaction == null)
		{
			globalTransaction = new GlobalTransaction(getDataServer(), application.getClientID());
			if (infoListener != null) infoListener.showTransactionStatus(true);
		}
	}

	public boolean hasTransaction()
	{
		return (globalTransaction != null);
	}

	public String[] getTableNames(String serverName)
	{
		try
		{
			IServer server = application.getSolution().getServer(serverName);
			if (server != null)
			{
				List<String> list = server.getTableNames(false);
				return list.toArray(new String[list.size()]);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public String[] getViewNames(String serverName)
	{
		try
		{
			IServer server = application.getSolution().getServer(serverName);
			if (server != null)
			{
				List<String> list = server.getViewNames(false);
				return list.toArray(new String[list.size()]);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public int getFoundSetCount(IFoundSetInternal fs)
	{
		if (fs instanceof FoundSet && fs.getTable() != null)
		{
			FoundSet foundset = (FoundSet)fs;
			try
			{
				//optimize
				if (foundset.isInitialized() && !foundset.hadMoreRows())
				{
					return foundset.getSize();
				}
				long time = System.currentTimeMillis();
				IDataServer ds = application.getDataServer();
				Table t = (Table)foundset.getTable();
				String transaction_id = getTransactionID(t.getServerName());
				QuerySelect sqlString = foundset.getSqlSelect();

				QuerySelect selectCountSQLString = sqlString.getSelectCount("n", true); //$NON-NLS-1$
				IDataSet set = ds.performQuery(application.getClientID(), t.getServerName(), transaction_id, selectCountSQLString,
					getTableFilterParams(t.getServerName(), selectCountSQLString), false, 0, 10, IDataServer.FOUNDSET_LOAD_QUERY);
				if (Debug.tracing())
				{
					Debug.trace("Foundset count time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + ", SQL: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						selectCountSQLString.toString());
				}

				if (set.getRowCount() > 0)
				{
					Object[] row = set.getRow(0);
					if (row.length > 0)
					{
						return Utils.getAsInteger(row[0]);
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return -1;
	}


	public int getTableCount(ITable table)
	{
		if (table != null)
		{
			try
			{
				long time = System.currentTimeMillis();
				IDataServer ds = application.getDataServer();
				String transaction_id = getTransactionID(table.getServerName());

				QuerySelect countSelect = new QuerySelect(new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema()));
				countSelect.addColumn(new QueryAggregate(QueryAggregate.COUNT, new QueryColumnValue(Integer.valueOf(1), "n", true), null)); //$NON-NLS-1$

				IDataSet set = ds.performQuery(application.getClientID(), table.getServerName(), transaction_id, countSelect,
					getTableFilterParams(table.getServerName(), countSelect), false, 0, 10, IDataServer.FOUNDSET_LOAD_QUERY);
				if (Debug.tracing())
				{
					Debug.trace("Table count time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + ", SQL: " + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						countSelect.toString());
				}
				if (set.getRowCount() > 0)
				{
					Object[] row = set.getRow(0);
					if (row.length > 0)
					{
						return Utils.getAsInteger(row[0]);
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return -1;
	}

	public boolean commitTransaction()
	{
		return commitTransaction(true, true);
	}

	public boolean commitTransaction(boolean saveFirst, boolean revertSavedRecords)
	{
		// first stop all edits, 'force' stop the edit by saying that it is a javascript stop
		if (globalTransaction != null && (!saveFirst || getEditRecordList().stopEditing(true) == ISaveConstants.STOPPED))
		{
			GlobalTransaction gt = globalTransaction;
			globalTransaction = null;
			Collection<String> dataSourcesToRefresh = gt.commit(revertSavedRecords);
			if (infoListener != null) infoListener.showTransactionStatus(false);
			performActionIfRequired();
			if (dataSourcesToRefresh != null)
			{
				refreshFoundsetsWithoutEditedRecords(dataSourcesToRefresh);
				return false;
			}
			return true;
		}
		return false;
	}

	private void refreshFoundsetsWithoutEditedRecords(Collection<String> dataSourcesToRefresh)
	{
		try
		{
			getEditRecordList().ignoreSave(true);
			for (String dataSource : dataSourcesToRefresh)
			{
				refreshFoundSetsFromDB(dataSource, null, true);
			}
		}
		finally
		{
			getEditRecordList().ignoreSave(false);
		}
	}

	public void rollbackTransaction()
	{
		rollbackTransaction(true, true, true);
	}

	public void rollbackTransaction(boolean rollbackEdited, boolean queryForNewData, boolean revertSavedRecords)
	{
		if (globalTransaction != null)
		{
			// first delete all edits, don't bother saving them they will be rolled back anyway.
			// Note that rows that have never been saved in the db will not be seen in GlobalTransaction.rollback()
			// because they never went through GlobalTransaction.addRow(), EditRecordList.rollbackRecords() will rollback in-memory.
			if (rollbackEdited)
			{
				getEditRecordList().rollbackRecords();
			}

			GlobalTransaction gt = globalTransaction;
			globalTransaction = null;
			Collection<String> dataSourcesToRefresh = gt.rollback(queryForNewData, revertSavedRecords);
			if (infoListener != null) infoListener.showTransactionStatus(false);
			performActionIfRequired();
			// refresh foundsets only if rollbackEdited is true, else the foundsets will even save/stopedit the record they where editing..
			if (dataSourcesToRefresh != null)
			{
				refreshFoundsetsWithoutEditedRecords(dataSourcesToRefresh);
			}
		}
	}

	/**
	 * Returns the globalTransaction.
	 *
	 * @return GlobalTransaction
	 */
	public GlobalTransaction getGlobalTransaction()
	{
		return globalTransaction;
	}

	public String getTransactionID(SQLSheet sheet) throws ServoyException
	{
		return getTransactionID(sheet.getServerName());
	}

	public String getTransactionID(String serverName) throws ServoyException
	{
		if (globalTransaction != null)
		{
			return globalTransaction.getTransactionID(serverName);
		}
		return null;
	}

	public String getOriginalServerName(String serverName)
	{
		return getOriginalServerNames(serverName).iterator().next();
	}

	@Override
	public Collection<String> getOriginalServerNames(String serverName)
	{
		IDataServer dataServer = application.getDataServer();
		if (dataServer instanceof DataServerProxy)
		{
			return ((DataServerProxy)dataServer).getReverseMappedServerNames(serverName);
		}
		return Collections.singleton(serverName);
	}

	public String getSwitchedToServerName(String serverName)
	{
		IDataServer dataServer = application.getDataServer();
		if (dataServer instanceof DataServerProxy)
		{
			return ((DataServerProxy)dataServer).getMappedServerName(serverName);
		}
		return serverName;
	}

	public IDataSet getDataSetByQuery(String serverName, ISQLSelect sqlSelect, boolean includeFilters, int maxNumberOfRowsToRetrieve) throws ServoyException
	{
		IDataServer ds = application.getDataServer();
		String transaction_id = getTransactionID(serverName);
		IDataSet set = null;
		try
		{
			long time = System.currentTimeMillis();
			set = ds.performCustomQuery(application.getClientID(), serverName, "<user_query>", transaction_id, sqlSelect,
				includeFilters ? getTableFilterParams(serverName, sqlSelect) : null, 0, maxNumberOfRowsToRetrieve);
			if (Debug.tracing())
			{
				Debug.trace(
					"Custom query, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + " SQL: " + sqlSelect); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		return set;
	}

	public String createDataSourceFromQuery(String name, String serverName, ISQLSelect sqlSelect, boolean useTableFilters, int maxNumberOfRowsToRetrieve,
		int[] types, String[] pkNames) throws ServoyException
	{
		if (name == null)
		{
			return null;
		}

		try
		{
			String queryTid = getTransactionID(serverName);

			String dataSource = DataSourceUtils.createInmemDataSource(name);
			ITable table = inMemDataSources.get(dataSource);
			if (table != null)
			{
				// temp table was used before, delete all data in it
				FoundSet foundSet = (FoundSet)getSharedFoundSet(dataSource);
				foundSet.removeLastFound();
				try
				{
					foundSet.deleteAllRecords();
				}
				catch (Exception e)
				{
					Debug.log(e);
					table = null;
				}
			}
			GlobalTransaction gt = getGlobalTransaction();
			String targetTid = null;
			if (gt != null)
			{
				targetTid = gt.getTransactionID(table == null ? IServer.INMEM_SERVER : table.getServerName());
			}

			table = application.getDataServer().insertQueryResult(application.getClientID(), serverName, queryTid, sqlSelect,
				useTableFilters ? getTableFilterParams(serverName, sqlSelect) : null, false, 0, maxNumberOfRowsToRetrieve, IDataServer.CUSTOM_QUERY, dataSource,
				table == null ? IServer.INMEM_SERVER : table.getServerName(), table == null ? null : table.getName() /* create temp table when null */,
				targetTid, ColumnType.getColumnTypes(types), pkNames);
			if (table != null)
			{
				inMemDataSources.put(dataSource, table);
				fireTableEvent(table);
				refreshFoundSetsFromDB(dataSource, null, false);
				return dataSource;
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		return null;
	}

	@Override
	public String toString()
	{
		return "FoundSetManager"; //$NON-NLS-1$
	}

/*
 * _____________________________________________________________ dataNotification
 */
	public void notifyDataChange(final String ds, IDataSet pks, final int action, Object[] insertColumnData)
	{
		RowManager rm = rowManagers.get(ds);
		if (rm != null)
		{
			List<Row> insertedRows = null;
			if (action == ISQLActionTypes.INSERT_ACTION && insertColumnData == null)
			{
				// in this case the insert notification is probably triggered by rawSQL; so we need to read the new rows from DB to get correct newly inserted content
				try
				{
					insertedRows = rm.getRows(pks, 0, pks.getRowCount(), false);
					if (insertedRows.size() != pks.getRowCount())
					{
						insertedRows = rm.getRows(pks, 0, pks.getRowCount(), true);
					}
				}
				catch (ServoyException e)
				{
					Debug.error("Cannot get newly inserted rows.", e);
				}
			}
			boolean didHaveRowAndIsUpdated = false;

			IDataSet newPks = pks;
			try
			{
				// Convert the pk dataset to the column type of the pk columns
				newPks = BufferedDataSetInternal.convertPksToRightType(pks, getTable(ds));
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
			final IDataSet fnewPks = newPks;

			for (int i = 0; i < fnewPks.getRowCount(); i++)
			{
				boolean b = rm.changeByOther(RowManager.createPKHashKey(fnewPks.getRow(i)), action, insertColumnData,
					insertedRows == null ? null : insertedRows.get(i));
				didHaveRowAndIsUpdated = (didHaveRowAndIsUpdated || b);
			}
			final boolean didHaveDataCached = didHaveRowAndIsUpdated;
			Runnable r = new Runnable()
			{
				public void run()
				{
					Solution solution = application.getSolution();
					if (solution != null)
					{
						ScriptMethod sm = application.getFlattenedSolution().getScriptMethod(solution.getOnDataBroadcastMethodID());
						if (sm != null)
						{
							try
							{
								application.getScriptEngine().getScopesScope().executeGlobalFunction(sm.getScopeName(), sm.getName(),
									Utils.arrayMerge(
										new Object[] { ds, new Integer(action), new JSDataSet(application, fnewPks), Boolean.valueOf(didHaveDataCached) },
										Utils.parseJSExpressions(solution.getFlattenedMethodArguments("onDataBroadcastMethodID"))), //$NON-NLS-1$
									false, false);
							}
							catch (Exception e1)
							{
								application.reportError(
									Messages.getString("servoy.foundsetManager.error.ExecutingDataBroadcastMethod", new Object[] { sm.getName() }), e1); //$NON-NLS-1$
							}
						}
					}
				}
			};
			application.invokeLater(r);

			if (didHaveRowAndIsUpdated)
			{
				if (infoListener != null) infoListener.showDataChange();
			}
			else
			// TODO if(action == INSERT) This is called to often now.
			{
				fireTableEvent(rm.getSQLSheet().getTable());
			}
		}
	}

	public FoundSet getEmptyFoundSet(IFoundSetListener panel) throws ServoyException
	{
		FoundSet set = (FoundSet)getNewFoundSet(getTable(panel), null, panel.getDefaultSortColumns());
		set.clear();
		return set;
	}

	public IFoundSetInternal createRelatedFindFoundSet(IRecordInternal parentRecord, String relationName, SQLSheet childSheet) throws ServoyException
	{
		return foundsetfactory.createRelatedFindFoundSet(this, parentRecord, relationName, childSheet);
	}


	public void flushSecuritySettings()
	{
		editRecordList.clearSecuritySettings();
	}

	public IInfoListener getInfoListener()
	{
		return infoListener;
	}

	public void setInfoListener(IInfoListener listener)
	{
		infoListener = listener;
	}

	public EditRecordList getEditRecordList()
	{
		return editRecordList;
	}

	public void createEmptyFormFoundsets()
	{
		createEmptyFoundsets = true;
	}

	/**
	 * Clear the delete sets of all row managers.
	 */
	public void clearAllDeleteSets()
	{
		Iterator<RowManager> it = rowManagers.values().iterator();
		while (it.hasNext())
		{
			it.next().clearDeleteSet();
		}

	}

	public void addGlobalFoundsetEventListener(IFoundSetEventListener foundSetEventListener)
	{
		globalFoundSetEventListener.addFoundSetEventListener(foundSetEventListener);
	}

	public void removeGlobalFoundsetEventListener(IFoundSetEventListener foundSetEventListener)
	{
		globalFoundSetEventListener.removeFoundSetEventListener(foundSetEventListener);
	}

	/**
	 * Manager for listeners interested in foundst events on any foundset.
	 * They will also be informed of newly created foundsets.
	 * @author rgansevles
	 *
	 */
	public static class GlobalFoundSetEventListener implements IFoundSetEventListener
	{
		private final List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<IFoundSetEventListener>();

		public void foundSetsCreated(IFoundSet[] foundSets)
		{
			if (foundSets != null)
			{
				for (IFoundSet fs : foundSets)
				{
					foundSetCreated(fs);
				}
			}
		}

		public void foundSetCreated(IFoundSet foundSet)
		{
			if (foundSet != null)
			{
				foundSet.addFoundSetEventListener(this);
				if (foundSetEventListeners.size() > 0)
				{
					foundSetChanged(new FoundSetEvent(foundSet, FoundSetEvent.NEW_FOUNDSET, FoundSetEvent.CHANGE_UPDATE));
				}
			}
		}

		public void foundSetChanged(FoundSetEvent e)
		{
			IFoundSetEventListener[] array;
			synchronized (this)
			{
				array = foundSetEventListeners.toArray(new IFoundSetEventListener[foundSetEventListeners.size()]);
			}

			for (IFoundSetEventListener element : array)
			{
				element.foundSetChanged(e);
			}
		}

		public synchronized void addFoundSetEventListener(IFoundSetEventListener l)
		{
			if (!foundSetEventListeners.contains(l))
			{
				foundSetEventListeners.add(l);
			}
		}

		public synchronized void removeFoundSetEventListener(IFoundSetEventListener l)
		{
			foundSetEventListeners.remove(l);
		}
	}


	/**
	 * Container class for related foundset arguments and the hash
	 *
	 * @author rgansevles
	 *
	 */
	private static class RelatedHashedArguments
	{
		final IRecordInternal state;
		final Object[] whereArgs;
		final String hash;

		public RelatedHashedArguments(IRecordInternal state, Object[] whereArgs, String hash)
		{
			this.state = state;
			this.whereArgs = whereArgs;
			this.hash = hash;
		}

		@Override
		public String toString()
		{
			return "RelatedHashedArguments " + hash + " " + Arrays.toString(whereArgs);
		}
	}

	public void setColumnManangers(IColumnValidatorManager columnValidatorManager, IConverterManager<IColumnConverter> columnConverterManager,
		IConverterManager<IUIConverter> uiConverterManager)
	{
		this.columnValidatorManager = columnValidatorManager;
		this.columnConverterManager = columnConverterManager;
		this.uiConverterManager = uiConverterManager;
	}

	private IColumnValidatorManager columnValidatorManager;
	private IConverterManager<IColumnConverter> columnConverterManager;
	private IConverterManager<IUIConverter> uiConverterManager;

	public IColumnValidatorManager getColumnValidatorManager()
	{
		return columnValidatorManager;
	}

	public IConverterManager<IColumnConverter> getColumnConverterManager()
	{
		return columnConverterManager;
	}

	public IConverterManager<IUIConverter> getUIConverterManager()
	{
		return uiConverterManager;
	}

	private boolean nullColumnValidatorEnabled = true;

	public boolean isNullColumnValidatorEnabled()
	{
		return nullColumnValidatorEnabled;
	}

	public void setNullColumnValidatorEnabled(boolean enable)
	{
		nullColumnValidatorEnabled = enable;
	}

	public String createDataSourceFromDataSet(String name, IDataSet dataSet, ColumnType[] columnTypes, String[] pkNames, boolean skipOnLoad)
		throws ServoyException
	{
		if (name == null)
		{
			return null;
		}

		String dataSource = DataSourceUtils.createInmemDataSource(name);
		FlattenedSolution s = application.getFlattenedSolution();

		IDataSet fixedDataSet = dataSet;
		ColumnType[] fixedColumnTypes = columnTypes;
		if (fixedColumnTypes == null) fixedColumnTypes = BufferedDataSetInternal.getColumnTypeInfo(dataSet);

		// get column def from the first in-mem datasource found
		ServoyJSONObject columnsDef = null;
		Iterator<TableNode> tblIte = s.getTableNodes(dataSource);
		int onLoadMethodId = -1;
		while (tblIte.hasNext())
		{
			TableNode tn = tblIte.next();
			if (columnsDef == null) columnsDef = tn.getColumns();
			if (onLoadMethodId == -1) onLoadMethodId = tn.getOnFoundSetLoadMethodID();
		}

		HashMap<String, ColumnInfoDef> columnInfoDefinitions = null;
		if (columnsDef != null)
		{
			TableDef tableInfo = DatabaseUtils.deserializeTableInfo(columnsDef);
			columnInfoDefinitions = new HashMap<String, ColumnInfoDef>();
			String[] inmemColumnNames = new String[tableInfo.columnInfoDefSet.size()];
			List<String> inmemPKs = new ArrayList<String>();
			ColumnType[] inmemColumnTypes = new ColumnType[tableInfo.columnInfoDefSet.size()];
			for (int j = 0; j < tableInfo.columnInfoDefSet.size(); j++)
			{
				ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(j);
				inmemColumnNames[j] = cid.name;
				inmemColumnTypes[j] = cid.columnType;
				if ((cid.flags & IBaseColumn.IDENT_COLUMNS) != 0)
				{
					inmemPKs.add(cid.name);
				}
				columnInfoDefinitions.put(cid.name, cid);
			}
			if (pkNames == null && inmemPKs.size() > 0)
			{
				pkNames = inmemPKs.toArray(new String[inmemPKs.size()]);
			}

			if (!Arrays.equals(dataSet.getColumnNames(), inmemColumnNames) || !Arrays.equals(fixedColumnTypes, inmemColumnTypes))
			{
				fixedColumnTypes = inmemColumnTypes;
				fixedDataSet = BufferedDataSetInternal.createBufferedDataSet(inmemColumnNames, fixedColumnTypes, dataSet.getRows(), dataSet.hadMoreRows());
				if (dataSet.getColumnCount() > 0 && !Arrays.equals(dataSet.getColumnNames(), inmemColumnNames))
				{
					Debug.warn("Dataset column names definition does not match inmem table definition for datasource : " + dataSource);
				}
				if (columnTypes != null && !Arrays.equals(columnTypes, inmemColumnTypes))
				{
					Debug.warn("Dataset column types definition does not match inmem table definition for datasource : " + dataSource);
				}
			}
		}

		// check if column names width matches rows
		if (fixedDataSet.getRowCount() > 0 && fixedDataSet.getRow(0).length != fixedDataSet.getColumnCount())
		{
			throw new RepositoryException("Data set rows do not match column count"); //$NON-NLS-1$
		}

		try
		{
			ITable table = inMemDataSources.get(dataSource);
			if (table != null)
			{
				// temp table was used before, delete all data in it
				FoundSet foundSet = (FoundSet)getSharedFoundSet(dataSource);
				foundSet.removeLastFound();
				try
				{
					foundSet.deleteAllRecords();
				}
				catch (Exception e)
				{
					Debug.log(e);
					table = null;
				}
			}

			GlobalTransaction gt = getGlobalTransaction();
			String tid = null;
			if (gt != null)
			{
				tid = gt.getTransactionID(table == null ? IServer.INMEM_SERVER : table.getServerName());
			}


			table = application.getDataServer().insertDataSet(application.getClientID(), fixedDataSet, dataSource,
				table == null ? IServer.INMEM_SERVER : table.getServerName(), table == null ? null : table.getName() /* create temp table when null */, tid,
				fixedColumnTypes /* inferred from dataset when null */, pkNames, columnInfoDefinitions);
			if (table != null)
			{
				inMemDataSources.put(dataSource, table);
				fireTableEvent(table);
				if (!skipOnLoad && fixedDataSet.getRowCount() == 0 && onLoadMethodId > 0)
				{
					IFoundSetInternal sharedFoundSet = getSharedFoundSet(dataSource);
					((FoundSet)sharedFoundSet).executeFoundsetTrigger(new Object[] { DataSourceUtils.getInmemDataSourceName(dataSource) },
						StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID, false);
				}
				refreshFoundSetsFromDB(dataSource, null, false);
				return dataSource;
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		return null;
	}

	public boolean removeDataSource(String uri) throws RepositoryException
	{
		if (uri == null)
		{
			return false;
		}
		try
		{
			ITable table = inMemDataSources.remove(uri);
			if (table != null)
			{
				sharedDataSourceFoundSet.remove(uri);
				application.getDataServer().dropTemporaryTable(application.getClientID(), table.getServerName(), table.getName());
				return true;
			}
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
		return false;
	}

	/**
	 * Test validity of data sources that are for this client.
	 *
	 * @return
	 */
	public boolean hasClientDataSources()
	{
		// in-memory data sources
		return inMemDataSources.size() > 0;
	}

	/**
	 *  Register this client as table user for all used tables.
	 *
	 * @param serverName when non-null limit to server name.
	 *
	 * @throws ServoyException
	 */
	public void registerClientTables(String serverName) throws ServoyException
	{
		for (String datasource : rowManagers.keySet())
		{
			if (serverName == null || serverName.equals(DataSourceUtils.getDataSourceServerName(datasource)))
			{
				ITable t = getTable(datasource);
				try
				{
					if (Debug.tracing())
					{
						Debug.trace("Registering table '" + t.getServerName() + ". " + t.getName() + "' for client '" + application.getClientID() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
					}
					getDataServer().addClientAsTableUser(application.getClientID(), t.getServerName(), t.getName());
				}
				catch (RemoteException e)
				{
					throw new RepositoryException(e);
				}
			}
		}
	}

	public int saveData()
	{
		return editRecordList.stopEditing(false);
	}

	public int saveData(List<IRecord> recordsToSave)
	{
		return editRecordList.stopEditing(true, recordsToSave);
	}

	@Override
	public IRecord[] getFailedRecords()
	{
		return editRecordList.getFailedRecords();
	}

	/** register runnables that contain fire calls, should be done after foundsets are created.
	 * @param runnable
	 */
	public void registerFireRunnables(List<Runnable> runnables)
	{
		synchronized (fireRunabbles)
		{
			fireRunabbles.addAll(runnables);
		}
	}

	/**
	 * @see com.servoy.j2db.IServiceProvider#setTrackingInfo(com.servoy.j2db.util.Pair)
	 */
	public void addTrackingInfo(String columnName, Object value)
	{
		if (columnName != null)
		{
			if (value == null) trackingInfoMap.remove(columnName);
			else trackingInfoMap.put(columnName, value);
		}
	}

	/**
	 * @see com.servoy.j2db.IServiceProvider#getTrackingInfo()
	 */
	public HashMap<String, Object> getTrackingInfo()
	{
		return trackingInfoMap;
	}

	public IQueryBuilderFactory getQueryFactory()
	{
		return new QBFactory(this, getScopesScopeProvider(), getApplication().getFlattenedSolution(), getApplication().getScriptEngine().getSolutionScope());
	}

	public IFoundSetInternal getFoundSet(String dataSource) throws ServoyException
	{
		IFoundSetInternal fs = getNewFoundSet(dataSource, null, getDefaultPKSortColumns(dataSource));
		fs.clear();//have to deliver a initialized foundset, user might call new record as next call on this one
		return fs;
	}

	public List<SortColumn> getDefaultPKSortColumns(String dataSource) throws ServoyException
	{
		return getSQLGenerator().getCachedTableSQLSheet(dataSource).getDefaultPKSort();
	}

	public IFoundSet getFoundSet(IQueryBuilder query) throws ServoyException
	{
		QBSelect select = (QBSelect)query;
		IFoundSet fs = getNewFoundSet(select.getDataSource(), select.getQuery(), null /* use sorting defined in query */);
		fs.loadAllRecords();
		return fs;
	}

	public IDataSet getDataSetByQuery(IQueryBuilder query, int max_returned_rows) throws ServoyException
	{
		return getDataSetByQuery(query, true, max_returned_rows);
	}

	public IDataSet getDataSetByQuery(IQueryBuilder query, boolean useTableFilters, int max_returned_rows) throws ServoyException
	{
		if (!application.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			return null;
		}

		QBSelect select = (QBSelect)query;

		String serverName = DataSourceUtils.getDataSourceServerName(select.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { select.getDataSource() }));

		return getDataSetByQuery(serverName, select.build(), useTableFilters, max_returned_rows);
	}
}
