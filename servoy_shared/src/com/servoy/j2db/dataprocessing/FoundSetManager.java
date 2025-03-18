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


import static com.servoy.base.util.DataSourceUtilsBase.getDBServernameTablename;
import static com.servoy.j2db.Messages.isI18NTable;
import static com.servoy.j2db.dataprocessing.FoundSetManager.TriggerExecutionMode.BreakOnFalse;
import static com.servoy.j2db.dataprocessing.FoundSetManager.TriggerExecutionMode.ExecuteEach;
import static com.servoy.j2db.dataprocessing.FoundSetManager.TriggerExecutionMode.ReturnFirst;
import static com.servoy.j2db.dataprocessing.SQLGenerator.createRelationKeyPlaceholderKey;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.util.DataSourceUtils.createDBTableDataSource;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceServerName;
import static com.servoy.j2db.util.DataSourceUtils.getDataSourceTableName;
import static com.servoy.j2db.util.DataSourceUtils.getViewDataSourceName;
import static com.servoy.j2db.util.Utils.arrayMerge;
import static com.servoy.j2db.util.Utils.iterate;
import static com.servoy.j2db.util.Utils.parseJSExpressions;
import static com.servoy.j2db.util.Utils.removeFromCollection;
import static com.servoy.j2db.util.Utils.stream;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.Types;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.jabsorb.serializer.MarshallException;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.google.common.cache.CacheBuilder;
import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.Messages;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDataServer.InsertResult;
import com.servoy.j2db.dataprocessing.SQLSheet.ConverterInfo;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnName;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SortingNullprecedence;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.IQueryElement;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.SortOptions;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;
import com.servoy.j2db.querybuilder.impl.QBFactory;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.WrappedObjectReference;
import com.servoy.j2db.util.serialize.JSONSerializerWrapper;
import com.servoy.j2db.util.visitor.IVisitor;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Manager for foundsets
 * @author jblok
 */
public class FoundSetManager implements IFoundSetManagerInternal
{
	private final IApplication application;
	private ConcurrentMap<IFoundSetListener, IFoundSetInternal> separateFoundSets; // FoundSetListener -> FoundSet ... 1 foundset per listener
	private Map<String, IFoundSetInternal> sharedDataSourceFoundSet; // dataSource -> FoundSet ... 1 foundset per data source
	private Map<ViewFoundSet, Object> noneRegisteredVFS;
	private Map<String, ViewFoundSet> viewFoundSets; // dataSource -> FoundSet ... 1 foundset per data source
	private Map<String, MenuFoundSet> menuFoundSets; // dataSource -> FoundSet ... 1 foundset per data source
	private List<MenuFoundSet> relatedMenuFoundSets; // dataSource -> FoundSet ... 1 foundset per data source
	private ConcurrentMap<IFoundSetInternal, Boolean> foundSets;
	private ConcurrentMap<Pair<String, String>, IFoundSetInternal> namedFoundSets;
	private WeakReference<IFoundSetInternal> noTableFoundSet;
	private Map<String, RowManager> rowManagers; // dataSource -> RowManager... 1 per table
	private Map<ITable, CopyOnWriteArrayList<ITableChangeListener>> tableListeners; // table -> ArrayList(tableListeners)
	private SQLGenerator sqlGenerator;
	private GlobalTransaction globalTransaction;
	private IInfoListener infoListener; // we allow only one
	private final IFoundSetFactory foundsetfactory;
	private boolean createEmptyFoundsets = false;
	private Map<String, List<TableFilter>> tableFilterParams; // server -> ArrayList(TableFilter)
	private Map<String, ITable> inMemDataSources; // dataSourceUri -> temp table
	private Map<String, ITable> viewDataSources;
	protected Map<String, ConcurrentMap<String, RelatedFoundSet>> cachedSubStates; // Map based on guava soft values cache
	protected Map<String, List<RelatedHashedArguments>> dbIdentArguments;
	protected List<String> locks = new SortedList<String>(StringComparator.INSTANCE);

	private final GlobalFoundSetEventListener globalFoundSetEventListener = new GlobalFoundSetEventListener();

	private final EditRecordList editRecordList;

	public final FoundSetManagerConfig config;

	private final List<Runnable> fireRunabbles = new ArrayList<>();

	// tracking info used for logging
	private final HashMap<String, Object> trackingInfoMap = new HashMap<>();
	private int foundsetCounter = 1;

	private static final String TENANT_FILTER = "_svy_tenant_id_table_filter"; //$NON-NLS-1$

	public FoundSetManager(IApplication app, FoundSetManagerConfig config, IFoundSetFactory factory)
	{
		application = app;
		this.config = config;
		initMembers();
		editRecordList = new EditRecordList(this);
		foundsetfactory = factory;
	}

	@Override
	public SortOptions getSortOptions(IColumn column)
	{
		boolean ignoreCase = false;
		SortingNullprecedence sortingNullprecedence = SortingNullprecedence.databaseDefault;
		if (column != null)
		{
			try
			{
				// First defined at server level
				IServer server = application.getSolution().getServer(column.getTable().getServerName());
				if (server != null)
				{
					ignoreCase = server.getSettings().isSortIgnorecase();
					sortingNullprecedence = server.getSettings().getSortingNullprecedence();
				}
			}
			catch (RepositoryException | RemoteException e)
			{
				Debug.error("Exception getting server settings", e);
			}

			ColumnInfo columnInfo = column.getColumnInfo();
			if (columnInfo != null)
			{
				// Can be overridden at column level
				if (columnInfo.getSortIgnorecase() != null)
				{
					ignoreCase = columnInfo.getSortIgnorecase().booleanValue();
				}
				if (columnInfo.getSortingNullprecedence() != null && columnInfo.getSortingNullprecedence() != SortingNullprecedence.databaseDefault)
				{
					sortingNullprecedence = columnInfo.getSortingNullprecedence();
				}
			}
		}

		return SortOptions.NONE.withIgnoreCase(ignoreCase).withNullprecedence(sortingNullprecedence);
	}

	/**
	 * @return the cachedSubStates
	 */
	private Map<String, ConcurrentMap<String, RelatedFoundSet>> getCachedSubStates()
	{
		return cachedSubStates;
	}

	private Runnable createFlushAction(final String dataSource)
	{
		return new Runnable()
		{
			public void run()
			{
				if (dataSource == null)
				{
					for (RowManager element : rowManagers.values())
					{
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
						refreshFoundSetsFromDB(dataSource, null, false, false);
						fireTableEvent(element.getSQLSheet().getTable());
					}
				}
			}
		};
	}

	// triggered by server call
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

	private final List<Runnable> runOnEditOrTransactionStoppedActions = Collections.synchronizedList(new ArrayList<>(3));
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
		refreshFoundSetsFromDB(null, null, false, false);
	}

	/**
	 *
	 * @param table
	 * @param dataSource
	 * @param columnName when not null, only return true if the table has the column
	 */
	private boolean mustRefresh(ITable table, String dataSource, List<TableFilterdefinition> tableFilterdefinitions)
	{
		if (tableFilterdefinitions != null && tableFilterdefinitions.stream().noneMatch(tableFilterdefinition -> tableFilterdefinition.affects(table)))
		{
			// table not affected
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
			return table.getServerName().equals(dsTable.getServerName()) && table.getName().equals(dsTable.getName());
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
	private Collection<ITable> refreshFoundSetsFromDB(String dataSource, List<TableFilterdefinition> tableFilterdefinitions, boolean dropSort,
		boolean skipStopEdit)
	{
		Set<ITable> affectedTables = new HashSet<ITable>();

		asList(
			sharedDataSourceFoundSet.values(),
			separateFoundSets.values(),
			foundSets.keySet(),
			namedFoundSets.values())
				.stream().flatMap(Collection::stream)
				.filter(FoundSet.class::isInstance)
				.map(FoundSet.class::cast)
				.forEach(foundset -> refreshFoundSet(foundset, dataSource, tableFilterdefinitions, dropSort, skipStopEdit, affectedTables));

		// Can't just clear substates!! if used in portal then everything is out of sync
//		if(server_name == null && table_name == null)
//		{
//			cachedSubStates.clear();
//		}
//		else
		for (ConcurrentMap<String, RelatedFoundSet> map : getCachedSubStates().values())
		{
			for (RelatedFoundSet relatedFoundSet : map.values())
			{
				try
				{
					if (mustRefresh(relatedFoundSet.getTable(), dataSource, tableFilterdefinitions))
					{
						//element.refreshFromDB(false);
						// this call is somewhat different then a complete refresh from db.
						// The selection isn't tried to keep on the same pk
						// new records are really being flushed..
						relatedFoundSet.invalidateFoundset();
						affectedTables.add(relatedFoundSet.getTable());
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		getEditRecordList().fireEvents();

		return affectedTables;
	}

	private void refreshFoundSet(FoundSet fs, String dataSource, List<TableFilterdefinition> tableFilterdefinitions, boolean dropSort, boolean skipStopEdit,
		Set<ITable> affectedTables)
	{
		try
		{
			if (mustRefresh(fs.getTable(), dataSource, tableFilterdefinitions))
			{
				if (fs.isInitialized())
				{
					fs.getPksAndRecords().setSkipOptimizeChangeFires(true);
					try
					{
						fs.refreshFromDB(dropSort, skipStopEdit);
					}
					finally
					{
						fs.getPksAndRecords().setSkipOptimizeChangeFires(false);
					}
				}
				affectedTables.add(fs.getTable());
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	private Collection<ITable> refreshFoundSetsFromDBforFilterAndGetAffectedTables(String dataSource, List<TableFilterdefinition> tableFilterdefinitions)
	{
		Collection<ITable> affectedtableList = refreshFoundSetsFromDB(dataSource, tableFilterdefinitions, false, false);

		// also add tables that have listeners but no foundsets
		for (ITable tableKey : tableListeners.keySet())
		{
			if (!affectedtableList.contains(tableKey) && mustRefresh(tableKey, dataSource, tableFilterdefinitions))
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

		getAllFoundsetsStream()
			.filter(FoundSet.class::isInstance).map(FoundSet.class::cast)
			.filter(rfs -> dataSource.equals(rfs.getDataSource()))
			.forEach(rfs -> {
				try
				{
					rfs.reloadFoundsetMethod(scriptMethod);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			});
	}

	public void init()
	{
		editRecordList.init();
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
		RelatedHashedArgumentsWithState relatedArguments = calculateFKHash(state, relation, false);
		if (relatedArguments == null)
		{
			return null;
		}

		RelatedFoundSet rfs = getCachedRelatedFoundset(relation.getName(), relatedArguments);
		if (rfs != null && !rfs.mustQueryForUpdates() && !rfs.mustAggregatesBeLoaded())
		{
			return rfs;
		}
		return null;
	}

	//query for a substate
	protected IFoundSetInternal getRelatedFoundSet(IRecordInternal state, SQLSheet childSheet, String relationName, List<SortColumn> defaultSortColumns)
		throws ServoyException
	{
		Relation relation = application.getFlattenedSolution().getRelation(relationName);
		if (!Relation.isValid(relation, application.getFlattenedSolution()))
		{
			return null;
		}

		if (relation.isParentRef())
		{
			return state.getParentFoundSet();
		}

		RelatedHashedArgumentsWithState relatedArguments = calculateFKHash(state, relation, false);
		if (relatedArguments == null)
		{
			return null;
		}

		IFoundSetInternal retval = getCachedRelatedFoundset(relation.getName(), relatedArguments);

		List<RelatedHashedArgumentsWithState> toFetch = null;
		if (retval == null)
		{
			String lockString = relationName + relatedArguments.hashedArguments.hash;
			ConcurrentMap<String, RelatedFoundSet> rfs;
			synchronized (locks)
			{
				rfs = getCachedSubStates().get(relationName);
				if (rfs == null)
				{
					rfs = CacheBuilder.newBuilder().softValues().<String, RelatedFoundSet> build().asMap();
					getCachedSubStates().put(relationName, rfs);
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
				retval = rfs.get(relatedArguments.hashedArguments.hash);
				if (retval == null)
				{
					locks.add(lockString);

					// pre-fetch a number of sibling related found sets
					toFetch = new ArrayList<>();
					toFetch.add(relatedArguments); // first to fetch is the one currently requested

					IFoundSetInternal parent = state.getParentFoundSet();
					int currIndex = parent.getRecordIndex(state);
					if (!disableRelatedSiblingsPrefetch && currIndex >= 0 && parent instanceof FoundSet)
					{
						int relatedChunkSize = config.chunkSize() / 3;
						Object[] siblingRecords = ((FoundSet)parent).getPksAndRecords().getCachedRecords().toArray(); // take a snapshot of cachedRecords
						for (int s = currIndex + 1; s < siblingRecords.length && toFetch.size() < relatedChunkSize; s++)
						{
							IRecordInternal sibling = (IRecordInternal)siblingRecords[s];
							if (sibling != null)
							{
								RelatedHashedArgumentsWithState extra = calculateFKHash(sibling, relation, true);
								if (extra != null && !rfs.containsKey(extra.hashedArguments.hash) /* already cached */)
								{
									String extraLockString = relationName + extra.hashedArguments.hash;
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
						RelatedHashedArgumentsWithState relargs = toFetch.get(f);
						states[f] = relargs.state;
						whereArgsList[f] = relargs.hashedArguments.whereArgs;
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
							RelatedHashedArgumentsWithState relargs = toFetch.get(f);
							if (retvals != null)
							{
								rfs.put(relargs.hashedArguments.hash, retvals[f]);
								if (relargs.hashedArguments.isDBIdentity())
								{
									List<RelatedHashedArguments> storedDBIdentArguments = dbIdentArguments.get(relationName);
									if (storedDBIdentArguments == null)
									{
										storedDBIdentArguments = Collections.synchronizedList(new ArrayList<>());
										dbIdentArguments.put(relationName, storedDBIdentArguments);
									}
									storedDBIdentArguments.add(relargs.hashedArguments);
								}
							}
							locks.remove(relationName + relargs.hashedArguments.hash);
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

	private RelatedFoundSet getCachedRelatedFoundset(String relationName, RelatedHashedArgumentsWithState relatedArguments)
	{
		ConcurrentMap<String, RelatedFoundSet> rfsCache = getCachedSubStates().get(relationName);
		RelatedFoundSet rfs = null;
		if (rfsCache != null)
		{
			rfs = rfsCache.get(relatedArguments.hashedArguments.hash);
			if (rfs == null)
			{
				rfs = checkDbIdentMap(relationName, relatedArguments.hashedArguments, rfsCache);
			}
		}
		return rfs;
	}

	/**
	 * @param relationName
	 * @param retval
	 * @param relatedArguments
	 * @param rfs
	 * @return
	 */
	private RelatedFoundSet checkDbIdentMap(String relationName, RelatedHashedArguments relatedArguments, ConcurrentMap<String, RelatedFoundSet> rfs)
	{
		RelatedFoundSet retval = null;
		List<RelatedHashedArguments> identArguments = dbIdentArguments.get(relationName);
		int index = -1;
		if (identArguments != null && (index = identArguments.indexOf(relatedArguments)) >= 0)
		{
			RelatedHashedArguments oldDBIdentArguments = identArguments.get(index);
			retval = rfs.get(oldDBIdentArguments.hash);
			if (retval != null)
			{
				// adjust the related cache
				rfs.put(relatedArguments.hash, retval);
				rfs.remove(oldDBIdentArguments.hash);
				// test if last entry, if so remove the complete relation key
				synchronized (identArguments)
				{
					identArguments.remove(oldDBIdentArguments);
					if (identArguments.size() == 0)
					{
						dbIdentArguments.remove(relationName);
					}
				}
				if (retval != null)
				{
					unwrapDbIdentValue(retval.getCreationSqlSelect(), relationName);
					unwrapDbIdentValue(retval.getQuerySelectForReading(), relationName);
				}
			}
		}
		return retval;
	}

	private static void unwrapDbIdentValue(QuerySelect querySelect, String relationName)
	{
		Placeholder whereArgsPlaceholder = SQLGenerator.getRelationPlaceholder(querySelect, relationName);
		if (whereArgsPlaceholder != null && whereArgsPlaceholder.getValue() instanceof Object[][])
		{
			Object[][] createWhereArgs = (Object[][])whereArgsPlaceholder.getValue();
			for (int i = 0; i < createWhereArgs.length; i++)
			{
				if (createWhereArgs[i][0] instanceof DbIdentValue && ((DbIdentValue)createWhereArgs[i][0]).getPkValue() != null)
				{
					createWhereArgs[i][0] = ((DbIdentValue)createWhereArgs[i][0]).getPkValue();
				}
			}
		}
	}

	private RelatedHashedArgumentsWithState calculateFKHash(IRecordInternal state, Relation r, boolean testForCalcs) throws RepositoryException
	{
		Object[] whereArgs = getRelationWhereArgs(state, r, testForCalcs);
		if (whereArgs == null)
		{
			return null;
		}

		return new RelatedHashedArgumentsWithState(state, new RelatedHashedArguments(whereArgs, RowManager.createPKHashKey(whereArgs)));
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


	/**
	 *  Get query for the relation from a record
	 */
	public QuerySelect getRelatedFoundSetQuery(IRecordInternal record, Relation relation) throws ServoyException
	{
		SQLSheet sheet = getSQLGenerator().getCachedTableSQLSheet(relation.getPrimaryDataSource());
		// this returns quickly if it already has a sheet for that relation, but optimize further?
		getSQLGenerator().makeRelatedSQL(sheet, relation);

		Object[] relationWhereArgs = getRelationWhereArgs(record, relation, false);
		if (relationWhereArgs == null)
		{
			return null;
		}

		QuerySelect relationSelect = deepClone((QuerySelect)sheet.getRelatedSQLDescription(relation.getName()).getSQLQuery());

		TablePlaceholderKey placeHolderKey = createRelationKeyPlaceholderKey(relationSelect.getTable(), relation.getName());
		if (!relationSelect.setPlaceholderValue(placeHolderKey, relationWhereArgs))
		{
			Debug.error(new RuntimeException("Could not set relation placeholder " + placeHolderKey + " in query " + relationSelect));
			return null;
		}

		return relationSelect;
	}

	public void handleUserLoggedin()
	{
		// sqlGenerator may have calculations loaded based on the login flattened solution
		sqlGenerator = null;
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
		sharedDataSourceFoundSet = new ConcurrentHashMap<>(64);
		separateFoundSets = CacheBuilder.newBuilder().weakKeys().initialCapacity(32).<IFoundSetListener, IFoundSetInternal> build().asMap();
		foundSets = CacheBuilder.newBuilder().weakKeys().initialCapacity(64).<IFoundSetInternal, Boolean> build().asMap();
		namedFoundSets = CacheBuilder.newBuilder().weakValues().initialCapacity(32).<Pair<String, String>, IFoundSetInternal> build().asMap();
		viewFoundSets = new ConcurrentHashMap<>(16);
		menuFoundSets = new ConcurrentHashMap<>(5);
		relatedMenuFoundSets = new ArrayList<MenuFoundSet>();
		noneRegisteredVFS = CacheBuilder.newBuilder().weakKeys().initialCapacity(8).<ViewFoundSet, Object> build().asMap();
		noTableFoundSet = null;

		rowManagers = new ConcurrentHashMap<>(64);
		tableListeners = new ConcurrentHashMap<>(16);
		tableFilterParams = new ConcurrentHashMap<String, List<TableFilter>>();

		cachedSubStates = new ConcurrentHashMap<>(128);
		dbIdentArguments = new ConcurrentHashMap<>();
		inMemDataSources = new ConcurrentHashMap<>();
		viewDataSources = new ConcurrentHashMap<>();
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
			sqlGenerator = new SQLGenerator(application, config.setRelationNameComment());
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
				// first time this client uses this table
				getDataServer().addClientAsTableUser(application.getClientID(), t.getServerName(), t.getName());
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
		ITable table = dataSource.startsWith(DataSourceUtils.VIEW_DATASOURCE_SCHEME_COLON) ? viewDataSources.get(dataSource) : inMemDataSources.get(dataSource);
		if (table == null)
		{
			// when it is a db:/server/table data source, note that the table is optional in the datasource string
			String[] servernameTablename = getDBServernameTablename(dataSource);
			if (servernameTablename != null && servernameTablename[0] != null && servernameTablename[1] != null)
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
			else if (getDataSourceServerName(dataSource) == IServer.INMEM_SERVER)
			{
				if (!inMemDataSources.containsKey(dataSource) && dataSourceExists(dataSource))
				{
					try
					{
						insertToDataSource(getDataSourceTableName(dataSource), new BufferedDataSet(), null, null, true, false,
							IServer.INMEM_SERVER);
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				return inMemDataSources.get(dataSource);
			}
			else if (getDataSourceServerName(dataSource) == IServer.VIEW_SERVER)
			{
				Optional<ServoyJSONObject> columnDefintion;
				if (!viewDataSources.containsKey(dataSource) && (columnDefintion = getColumnDefintion(dataSource)).isPresent())
				{
					Table tbl = new Table(IServer.VIEW_SERVER, getViewDataSourceName(dataSource), true, ITable.VIEW, null, null);
					tbl.setDataSource(dataSource);
					DatabaseUtils.deserializeInMemoryTable(application.getFlattenedSolution().getPersistFactory(), tbl, columnDefintion.get());
					tbl.setExistInDB(true);
					tbl.setInitialized(true);
					viewDataSources.put(dataSource, tbl);

					try
					{
						executeFoundsetTriggerReturnFirst(tbl, new Object[] { getViewDataSourceName(dataSource) },
							StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID, false, null); // can't entity methods, not supported on view foundsets
					}
					catch (ServoyException e)
					{
						Debug.error("Error executing foundset method for datasource:  " + dataSource, e);
					}

				}
				return viewDataSources.get(dataSource);
			}
		}
		return table;
	}

	public boolean dataSourceExists(String dataSource) throws RepositoryException
	{
		if (getDataSourceServerName(dataSource) == IServer.INMEM_SERVER)
		{
			if (inMemDataSources.containsKey(dataSource))
			{
				return true;
			}

			return getColumnDefintion(dataSource).isPresent();
		}

		if (getDataSourceServerName(dataSource) == IServer.VIEW_SERVER)
		{
			if (viewDataSources.containsKey(dataSource))
			{
				return true;
			}

			return getColumnDefintion(dataSource).isPresent();
		}


		return getTable(dataSource) != null;
	}

	/**
	 * @param dataSource
	 * @return
	 */
	private Optional<ServoyJSONObject> getColumnDefintion(String dataSource)
	{
		return stream(application.getFlattenedSolution().getTableNodes(dataSource))
			.map(TableNode::getColumns)
			.filter(Objects::nonNull)
			.findFirst();
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

	public DataproviderTableFilterdefinition createDataproviderTableFilterdefinition(ITable table, String dataprovider, String operator, Object val)
		throws ServoyException
	{
		if (dataprovider == null || operator == null)
		{
			return null;
		}

		int op = RelationItem.getValidOperator(operator.trim(), IBaseSQLCondition.ALL_DEFINED_OPERATORS, IBaseSQLCondition.ALL_MODIFIERS);
		if (op == -1)
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
		return new DataproviderTableFilterdefinition(dataprovider, op, value);
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

	public void setTableFilters(String filterName, String serverName, List<TableFilterRequest> tableFilterRequests, boolean removeOld, boolean fire)
	{
		boolean refreshI18NMessages = false;
		Set<Pair<String, TableFilterdefinition>> toRefresh = new HashSet<>();

		List<TableFilter> existingParams = tableFilterParams.get(serverName);
		boolean hasBroadcastFilter = stream(existingParams).anyMatch(TableFilter::isBroadcastFilter);

		// get the new filters if requested (tableFilterRequests can be null)
		List<TableFilter> newParams = null;
		if (tableFilterRequests != null)
		{
			newParams = new ArrayList<>(tableFilterRequests.size());
			for (TableFilterRequest tableFilterRequest : tableFilterRequests)
			{
				String tableName = tableFilterRequest.table == null ? null : tableFilterRequest.table.getName();
				TableFilter filter = new TableFilter(filterName, serverName, tableName,
					tableFilterRequest.table == null ? null : tableFilterRequest.table.getSQLName(), tableFilterRequest.tableFilterdefinition,
					tableFilterRequest.broadcastFilter);

				newParams.add(filter);
				if (existingParams == null || !existingParams.contains(filter))
				{
					toRefresh.add(new Pair<>(createDBTableDataSource(serverName, tableName), tableFilterRequest.tableFilterdefinition));
					refreshI18NMessages |= isI18NTable(serverName, filter.getTableName(), application);
				}
				// else filter is not changed
			}
		}

		// remove old filters by filterName
		if (removeOld && filterName != null && existingParams != null)
		{
			if (newParams != null)
			{ // new filters must not be removed
				existingParams.removeAll(newParams);
			}

			for (Iterator<TableFilter> oldParamsIt = existingParams.iterator(); oldParamsIt.hasNext();)
			{
				TableFilter oldFilter = oldParamsIt.next();
				if (filterName.equals(oldFilter.getName()))
				{
					oldParamsIt.remove();

					toRefresh.add(new Pair<>(createDBTableDataSource(serverName, oldFilter.getTableName()), oldFilter.getTableFilterdefinition()));
					refreshI18NMessages |= isI18NTable(serverName, oldFilter.getTableName(), application);
				}
			}
			if (existingParams.isEmpty())
			{
				tableFilterParams.remove(serverName);
			}
		}

		if (newParams != null)
		{
			hasBroadcastFilter |= stream(newParams).anyMatch(TableFilter::isBroadcastFilter);

			if (existingParams == null || existingParams.isEmpty())
			{
				tableFilterParams.put(serverName, newParams);
			}
			else
			{
				existingParams.addAll(newParams);
			}
		}

		if (hasBroadcastFilter)
		{
			// update the broadcast filters for the client
			BroadcastFilter[] broadcastFilters = stream(tableFilterParams.get(serverName))
				.filter(TableFilter::isBroadcastFilter)
				.map(tableFilter -> {
					BroadcastFilter broadcastFilter = tableFilter.createBroadcastFilter();
					if (broadcastFilter == null)
					{
						Debug.warn("Table filter " + tableFilter.getName() +
							"is not supported for dataBroadcast, filter is used in the client, but not used in dataBroadcast");
					}
					return broadcastFilter;
				})
				.filter(Objects::nonNull)
				.toArray(BroadcastFilter[]::new);
			getDataServer().setBroadcastFilters(application.getClientID(), serverName, broadcastFilters);
		}

		if (fire)
		{
			// fire events after all filters are adjusted
			Set<ITable> firedTables = new HashSet<>();
			toRefresh.stream().collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toList())))
				.forEach((dataSource, tableFilterdefinitions) -> {
					for (ITable affectedtable : refreshFoundSetsFromDBforFilterAndGetAffectedTables(dataSource, tableFilterdefinitions))
					{
						if (firedTables.add(affectedtable))
						{
							fireTableEvent(affectedtable);
						}
					}
				});

			if (refreshI18NMessages)
			{
				((ClientState)application).refreshI18NMessages(false);
			}
		}
	}

	public boolean updateTableFilterParam(String serverName, String filterName, ITable table, TableFilterdefinition tableFilterdefinition)
	{
		if (filterName != null)
		{
			for (TableFilter f : iterate(tableFilterParams.get(serverName)))
			{
				if (filterName.equals(f.getName()))
				{
					f.setTableFilterDefinition(tableFilterdefinition);

					for (ITable affectedtable : refreshFoundSetsFromDBforFilterAndGetAffectedTables(getDataSource(table), asList(tableFilterdefinition)))
					{
						fireTableEvent(affectedtable);
					}

					if (isI18NTable(serverName, table != null ? table.getName() : null, application))
					{
						((ClientState)application).refreshI18NMessages(false);
					}

					return true;
				}
			}
		}
		return false;
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
					dataSource = createDBTableDataSource(filter.getServerName(), filter.getTableName());
				}

				for (ITable affectedtable : refreshFoundSetsFromDBforFilterAndGetAffectedTables(dataSource, asList(filter.getTableFilterdefinition())))
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

	@Override
	public IFoundSetInternal[] getAllLoadedFoundsets(String dataSource, boolean includeViewFoundsets)
	{
		return getAllFoundsetsStream()
			.filter(fs -> includeViewFoundsets || fs instanceof FoundSet)
			.filter(fs -> dataSource == null || dataSource.equals(fs.getDataSource()))
			.toArray(IFoundSetInternal[]::new);
	}

	private Stream<IFoundSetInternal> getAllFoundsetsStream()
	{
		return Stream.concat(relatedMenuFoundSets.stream(), Stream.concat(menuFoundSets.values().stream(), Stream.concat(separateFoundSets.values().stream(), //
			Stream.concat(sharedDataSourceFoundSet.values().stream(), //
				Stream.concat(viewFoundSets.values().stream(), //
					Stream.concat(noneRegisteredVFS.keySet().stream(), //
						Stream.concat(foundSets.keySet().stream(), //
							Stream.concat(namedFoundSets.values().stream(), //
								getCachedSubStates().values()
									.stream() //
									.map(ConcurrentMap::values)
									.flatMap(Collection::stream) //
							))))))));
	}

	public Object[][] getTableFilterParams(String serverName, String filterName)
	{
		List<Object[]> result = new ArrayList<Object[]>();
		for (TableFilter f : iterate(tableFilterParams.get(serverName)))
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
					result.add(new Object[] { new QBSelect(this, getScopesScopeProvider(), getApplication().getFlattenedSolution(),
						getApplication().getScriptEngine().getSolutionScope(), querySelect.getTable().getDataSource(), null,
						deepClone(querySelect, true)), f.getName() });
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
		return getTableFilterParams(serverName, sql, emptyList());
	}

	public ArrayList<TableFilter> getTableFilterParams(String serverName, IQueryElement sql, List<String> filtersToIgnore)
	{
		List<TableFilter> serverFilters = stream(tableFilterParams.get(serverName))
			.filter(tf -> !filtersToIgnore.contains(tf.getName()))
			.collect(toList());
		if (serverFilters.isEmpty())
		{
			return null;
		}

		// get the sql table names in the query
		Set<String> tableSqlNames = new HashSet<>();
		// find the filters for the tables found in the query
		ArrayList<TableFilter>[] filters = new ArrayList[] { null };
		sql.acceptVisitor(o -> {
			try
			{
				if (o instanceof QueryTable && ((QueryTable)o).getDataSource() != null && ((QueryTable)o).getName() != null &&
					tableSqlNames.add(((QueryTable)o).getName()))
				{
					QueryTable qTable = (QueryTable)o;
					Table table = (Table)getTable(qTable.getDataSource());
					if (table == null)
					{
						// should never happen
						throw new RuntimeException("Could not find table '" + qTable.getDataSource() + "' for table filters");
					}

					for (TableFilter filter : iterate(serverFilters))
					{
						TableFilterdefinition tableFilterdefinition = filter.getTableFilterdefinition();
						if (filter.getTableName() == null && tableFilterdefinition instanceof DataproviderTableFilterdefinition)
						{
							DataproviderTableFilterdefinition dataproviderTableFilterdefinition = (DataproviderTableFilterdefinition)tableFilterdefinition;
							// filter is on all tables with specified dataProvider as column
							Column column = table.getColumn(dataproviderTableFilterdefinition.getDataprovider());
							if (column != null)
							{
								// Use filter with table name filled in.
								// When table was null value was not yet converted, convert now.
								Object value = convertFilterValue(table, column, dataproviderTableFilterdefinition.getValue());
								TableFilter useFilter = new TableFilter(filter.getName(), filter.getServerName(), table.getName(), table.getSQLName(),
									dataproviderTableFilterdefinition.getDataprovider(), dataproviderTableFilterdefinition.getOperator(), value);
								addFilter(filters, useFilter);
							}
						}
						else if (filter.getTableSQLName().equals(qTable.getName()))
						{
							addFilter(filters, filter);
						}
					}
				}
			}
			catch (Exception e)
			{
				// big trouble, this is security filtering, so bail out on error
				throw new RuntimeException(e);
			}
			return o;
		});

		return filters[0];
	}

	private static void addFilter(ArrayList<TableFilter>[] filters, TableFilter filter)
	{
		if (filters[0] == null)
		{
			filters[0] = new ArrayList<TableFilter>();
		}
		filters[0].add(filter);
	}

	/**
	 * Check if table filters for the query are defined that have joins.
	 *
	 * @param serverName
	 * @param sql
	 * @return
	 */
	public boolean hasTableFiltersWithJoins(String serverName, IQueryElement sql)
	{
		final List<TableFilter> serverFilters = tableFilterParams.get(serverName);
		if (serverFilters == null)
		{
			return false;
		}

		// get the sql table names in the query
		// find the filters for the tables found in the query
		final AtomicBoolean hasTableFiltersWithJoins = new AtomicBoolean(false);
		sql.acceptVisitor(new IVisitor()
		{
			private final Set<String> tableSqlNames = new HashSet<String>();

			public Object visit(Object o)
			{
				if (o instanceof QueryTable && ((QueryTable)o).getName() != null && tableSqlNames.add(((QueryTable)o).getName()))
				{
					QueryTable qTable = (QueryTable)o;
					for (TableFilter filter : serverFilters)
					{
						if (Utils.stringSafeEquals(filter.getTableSQLName(), qTable.getName()))
						{
							if (filter.getTableFilterdefinition() instanceof QueryTableFilterdefinition)
							{
								List<ISQLJoin> joins = ((QueryTableFilterdefinition)filter.getTableFilterdefinition()).getQuerySelect().getJoins();
								if (joins != null && !joins.isEmpty())
								{
									hasTableFiltersWithJoins.set(true);
									return new VisitorResult(o, false);
								}
							}
						}
					}
				}
				return o;
			}
		});

		return hasTableFiltersWithJoins.get();
	}

	/**
	 * Checks if the specified table has filter defined.
	 *
	 * @param serverName
	 * @param tableName
	 *
	 * @return true if there is a filter defined for the table, otherwise false
	 */
	public boolean hasTableFilter(String serverName, String tableName)
	{
		return !getTableFilters(serverName, tableName).isEmpty();
	}

	/**
	 * Get the table filters for the table.
	 *
	 * @param serverName
	 * @param tableName
	 *
	 * @return tableFilters list of table filters, empty list if none are defined
	 */
	public List<TableFilter> getTableFilters(String serverName, String tableName)
	{
		if (serverName == null || tableName == null) return emptyList();
		List<TableFilter> serverFilters = tableFilterParams.get(serverName);
		if (serverFilters == null) return emptyList();

		return serverFilters.stream()
			.filter(tableFilter -> tableName.equals(tableFilter.getTableName()))
			.toList();
	}

	/**
	 * Get the table filters with the given name.
	 *
	 * @param serverName
	 * @param tableName
	 *
	 * @return tableFilters list of table filters, empty list if none are defined
	 */
	@Override
	public List<TableFilter> getTableFilters(String filterName)
	{
		return tableFilterParams.values().stream().flatMap(List::stream)
			.filter(tf -> filterName.equals(tf.getName()))
			.collect(toList());
	}

	public IFoundSetInternal getSeparateFoundSet(IFoundSetListener l, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		String dataSource = l.getDataSource();
		if (dataSource == null)
		{
			return getNoTableFoundSet();
		}

		// make sure inmem table is created
		getTable(dataSource);

		// if it is a view foundset then just return the view foundset datasource, its always 1 per datasource
		if (getViewDataSourceName(dataSource) != null)
		{
			ViewFoundSet vfs = viewFoundSets.get(dataSource);
			if (vfs == null) throw new IllegalStateException("The view datasource " + dataSource +
				" is not registered yet on the form manager, please use databaseManager.getViewFoundSet(name, query, register)  with the register boolean true, or get at design time view through datasources.view.xxx.getFoundSet() first before showing a form");
			return vfs;
		}

		if (DataSourceUtils.getMenuDataSourceName(dataSource) != null)
		{
			// always one instance
			return getAndCreateMenuFoundset(dataSource);
		}

		if (l.getSharedFoundsetName() != null)
		{
			return getNamedFoundSet(l.getSharedFoundsetName(), dataSource);
		}

		IFoundSetInternal foundset = separateFoundSets.get(l);
		if (foundset == null)
		{
			foundset = createFoundset(dataSource, null, defaultSortColumns);
			separateFoundSets.put(l, foundset);
		}

		return foundset;
	}

	private IFoundSetInternal createFoundset(String datasource, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		SQLSheet sheet = getSQLGenerator().getCachedTableSQLSheet(datasource);
		IFoundSetInternal foundset = foundsetfactory.createFoundSet(this, sheet, pkSelect, defaultSortColumns);
		if (createEmptyFoundsets) foundset.clear();
		// inform global foundset event listeners that a new foundset has been created
		globalFoundSetEventListener.foundSetCreated(foundset);
		return foundset;
	}

	@Override
	public IFoundSetInternal findFoundset(int id)
	{
		return getAllFoundsetsStream()
			.filter(fs -> id == fs.getIDInternal())
			.findAny().orElse(null);
	}

	@Override
	public synchronized int getNextFoundSetID()
	{
		if (foundsetCounter == 0) foundsetCounter = 1; // MAX_INT ++ => negative and then gradually goes back to 0 (0 means id not initialized in Foundset class; so we skip it)
		return foundsetCounter++;
	}

	@Override
	public IFoundSetInternal getNamedFoundSet(String name, String datasourceFromContext) throws ServoyException
	{
		if (name == null) throw new RuntimeException("can't ask for a named foundset with a null name");

		Optional<Form> form = stream(application.getFlattenedSolution().getForms(false))
			.filter(frm -> name.equals(frm.getSharedFoundsetName()))
			.findAny();

		String datasource = form.map(Form::getDataSource).orElse(datasourceFromContext);
		if (datasource == null)
		{
			// datasourceFromContext was null and form was not found
			Debug.warn("Named foundset '" + name + "' not found");
			return null;
		}

		if (datasourceFromContext != null && !datasourceFromContext.equals(datasource))
		{
			Debug.warn("Named foundset '" + name + "' found for datasource '" + datasource + "' but requested datasource was '" + datasourceFromContext + "'");
			return null;
		}

		String initialSort = form.map(Form::getInitialSort).orElse(null); // use from form, if found
		Pair<String, String> key = new Pair<>(name, datasource);
		IFoundSetInternal foundset = namedFoundSets.get(key);
		if (foundset == null)
		{
			foundset = createFoundset(datasource, null, getSortColumns(datasource, initialSort));
			namedFoundSets.put(key, foundset);
		}

		return foundset;
	}

	public String getFoundSetName(IFoundSet foundset)
	{
		return namedFoundSets.entrySet().stream().filter(entry -> entry.getValue() == foundset).findAny().map(Entry::getKey).map(Pair::getLeft).orElse(null);
	}

	public IFoundSetInternal getSharedFoundSet(String dataSource, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (dataSource == null || !application.getFlattenedSolution().isMainSolutionLoaded())
		{
			return getNoTableFoundSet();
		}

		// make sure inmem table is created
		getTable(dataSource);

		if (getViewDataSourceName(dataSource) != null)
		{
			ViewFoundSet vfs = viewFoundSets.get(dataSource);
			if (vfs == null) throw new IllegalStateException("The view datasource " + dataSource +
				" is not registered yet on the form manager, please use databaseManager.getViewFoundSet(name, query, register)  with the register boolean true, or get at design time view through datasources.view.xxx.getFoundSet() first before showing a form");
			return vfs;
		}

		if (DataSourceUtils.getMenuDataSourceName(dataSource) != null)
		{
			return getAndCreateMenuFoundset(dataSource);
		}

		IFoundSetInternal foundset = sharedDataSourceFoundSet.get(dataSource);
		if (foundset == null)
		{
			foundset = createFoundset(dataSource, null, defaultSortColumns);
			sharedDataSourceFoundSet.put(dataSource, foundset);
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

		IFoundSetInternal foundset = createFoundset(dataSource, pkSelect, defaultSortColumns);
		foundSets.put(foundset, TRUE);
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

		return foundSets.containsKey(set);
	}

	@Override
	public void removeFoundSet(FoundSet foundset)
	{
		// TODO should we also add a FOUNDSET_DISPOSED to the globalFoundSetEventListener? it is able to trigger a NEW_FOUNDSET event for example
		foundset.removeFoundSetEventListener(globalFoundSetEventListener);
		foundSets.remove(foundset);
		if (sharedDataSourceFoundSet.get(foundset.getDataSource()) == foundset)
		{
			sharedDataSourceFoundSet.remove(foundset.getDataSource());
		}
		removeFromCollection(separateFoundSets.values(), foundset);
		removeFromCollection(namedFoundSets.values(), foundset);
	}

	private MenuFoundSet getAndCreateMenuFoundset(String dataSource)
	{
		MenuFoundSet menuFoundset = menuFoundSets.get(dataSource);
		if (menuFoundset == null)
		{
			JSMenu jsMenu = application.getMenuManager().getMenu(DataSourceUtils.getMenuDataSourceName(dataSource));
			if (jsMenu != null)
			{
				menuFoundset = new MenuFoundSet(jsMenu, this);
				menuFoundSets.put(dataSource, menuFoundset);
			}
		}
		return menuFoundset;
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
						else
						{
							Debug.warn("Invalid sort column: " + columnName + " on table " + t.getDataSource() + ". Will be ignored.");
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
				if (DataSourceUtils.getViewDataSourceName(t.getDataSource()) != null)
				{
					Debug.debug(e);
				}
				else
				{
					Debug.error(e);
				}
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
	// index == -1 is (current) selected record,< -1 is all records
	public boolean acquireLock(IFoundSet fs, int index, String lockName)
	{
		if (fs instanceof IFoundSetInternal)
		{
			IFoundSetInternal foundSet = (IFoundSetInternal)fs;
			if (foundSet.getSQLSheet() == null || foundSet.getSQLSheet().getTable() == null)
			{
				return false;
			}
			Map<String, Object[]> pkhashkeys = new HashMap<>();
			if (index == -1)
			{
				int idx = foundSet.getSelectedIndex();
				if (idx >= 0 && idx < foundSet.getSize())
				{
					IRecordInternal rec = foundSet.getRecord(idx);
					if (rec == null || rec.getRawData() == null) return false; // just for safety
					if (!rec.getRawData().lockedByMyself()) pkhashkeys.put(rec.getPKHashKey(), rec.getPK());
				}
				else
				{
					return false; // wrong index
				}
			}
			else if (index < -1)
			{
				for (int i = 0; i < foundSet.getSize(); i++)
				{
					IRecordInternal rec = foundSet.getRecord(i);
					if (rec == null || rec.getRawData() == null) return false; // just for safety
					if (!rec.getRawData().lockedByMyself()) pkhashkeys.put(rec.getPKHashKey(), rec.getPK());
				}
			}
			else if (index >= 0)
			{
				if (index < foundSet.getSize())
				{
					IRecordInternal rec = foundSet.getRecord(index);
					if (rec == null || rec.getRawData() == null) return false; // just for safety
					if (!rec.getRawData().lockedByMyself()) pkhashkeys.put(rec.getPKHashKey(), rec.getPK());
				}
				else
				{
					return false; // wrong index
				}
			}
			else
			{
				return false; // unknown index
			}

			RowManager rm = rowManagers.get(foundSet.getDataSource());
			if (rm != null)
			{
				QuerySelect tableSelectQuery = (QuerySelect)rm.getSQLSheet().getSQL(SQLSheet.SELECT);
				QuerySelect lockSelect = SQLGenerator.createUpdateLockSelect(tableSelectQuery, pkhashkeys.values(),
					hasTransaction() && parseBoolean(application.getSettings().getProperty("servoy.record.lock.lockInDB", "false"))); //$NON-NLS-1$ //$NON-NLS-2$
				if (lockSelect == null)
				{
					// no pks
					return true;
				}

				Set<Object> ids = new HashSet<>(pkhashkeys.keySet()); // make copy because it is not serialized in developer and set is emptied
				if (rm.acquireLock(application.getClientID(), lockSelect, lockName, ids))
				{
					if (infoListener != null)
					{
						infoListener.showLocksStatus(true);
					}
					// success
					return true;
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
					if (getDataServer().releaseLocks(application.getClientID(), rm.getSQLSheet().getServerName(), rm.getSQLSheet().getTable().getName(),
						pkhashkeys))
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
				QuerySelect sqlString = foundset.getQuerySelectForReading();

				QuerySelect selectCountSQLString = sqlString.getSelectCount("n", true); //$NON-NLS-1$
				IDataSet set = ds.performQuery(application.getClientID(), t.getServerName(), transaction_id, selectCountSQLString, null,
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

				QuerySelect countSelect = new QuerySelect(table.queryTable());
				countSelect.addColumn(new QueryAggregate(QueryAggregate.COUNT, new QueryColumnValue(Integer.valueOf(1), "n", true), null)); //$NON-NLS-1$

				IDataSet set = ds.performQuery(application.getClientID(), table.getServerName(), transaction_id, countSelect, null,
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
				refreshFoundSetsFromDB(dataSource, null, false, true);
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
		long time = System.currentTimeMillis();
		IDataSet set = ds.performCustomQuery(application.getClientID(), serverName, "<user_query>", transaction_id, sqlSelect,
			includeFilters ? getTableFilterParams(serverName, sqlSelect) : null, 0, maxNumberOfRowsToRetrieve);
		if (Debug.tracing())
		{
			Debug.trace(
				"Custom query, time: " + (System.currentTimeMillis() - time) + " thread: " + Thread.currentThread().getName() + " SQL: " + sqlSelect); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
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

		String queryTid = getTransactionID(serverName);

		String dataSource = DataSourceUtils.createInmemDataSource(name);
		ITable table = inMemDataSources.get(dataSource);
		GlobalTransaction gt = getGlobalTransaction();
		String targetTid = null;
		String targetServerName = table == null ? IServer.INMEM_SERVER : table.getServerName();
		if (gt != null)
		{
			targetTid = gt.getTransactionID(targetServerName);
		}
		if (table != null)
		{
			table = deleteAndCleanupInmemoryDatasource(name, dataSource, table, targetTid);
		}

		table = application.getDataServer()
			.insertQueryResult(application.getClientID(), serverName, queryTid, sqlSelect,
				useTableFilters ? getTableFilterParams(serverName, sqlSelect) : null, false, 0, maxNumberOfRowsToRetrieve, IDataServer.CUSTOM_QUERY,
				dataSource,
				table == null ? IServer.INMEM_SERVER : table.getServerName(), table == null ? null : table.getName() /* create temp table when null */,
				targetTid, ColumnType.getColumnTypes(types), pkNames);
		if (table != null)
		{
			inMemDataSources.put(dataSource, table);
			fireTableEvent(table);
			refreshFoundSetsFromDB(dataSource, null, true, false);
			return dataSource;
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
			// changed by other calls don't notify the table change for every row, call it once now.
			notifyChange(rm.getSQLSheet().getTable());
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
								application.getScriptEngine()
									.getScopesScope()
									.executeGlobalFunction(sm.getScopeName(), sm.getName(),
										arrayMerge(
											new Object[] { ds, new Integer(action), new JSDataSet(application, fnewPks), Boolean.valueOf(didHaveDataCached) },
											parseJSExpressions(solution.getFlattenedMethodArguments("onDataBroadcastMethodID"))), //$NON-NLS-1$
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
		for (RowManager element : rowManagers.values())
		{
			element.clearDeleteSet();
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

	public static class TableFilterRequest
	{
		private final ITable table;
		private final TableFilterdefinition tableFilterdefinition;
		private final boolean broadcastFilter;

		public TableFilterRequest(ITable table, TableFilterdefinition tableFilterdefinition, boolean broadcastFilter)
		{
			this.table = table;
			this.tableFilterdefinition = tableFilterdefinition;
			this.broadcastFilter = broadcastFilter;
		}
	}

	/**
	 * Container class for related foundset arguments and the hash
	 *
	 * @author rgansevles
	 *
	 */
	private static class RelatedHashedArgumentsWithState
	{
		final IRecordInternal state;
		final RelatedHashedArguments hashedArguments;

		public RelatedHashedArgumentsWithState(IRecordInternal state, RelatedHashedArguments hashedArguments)
		{
			this.state = state;
			this.hashedArguments = hashedArguments;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof RelatedHashedArgumentsWithState))
			{
				return false;
			}
			RelatedHashedArgumentsWithState other = ((RelatedHashedArgumentsWithState)obj);
			return Utils.equalObjects(state, other.state) && Utils.equalObjects(hashedArguments, other.hashedArguments);
		}

		@Override
		public String toString()
		{
			return hashedArguments.toString();
		}
	}

	private static class RelatedHashedArguments
	{
		final Object[] whereArgs;
		final String hash;
		boolean isDBIdentiy;

		public RelatedHashedArguments(Object[] whereArgs, String hash)
		{
			this.whereArgs = whereArgs;
			this.hash = hash;
			if (whereArgs != null)
			{
				for (Object arg : whereArgs)
				{
					if (arg instanceof DbIdentValue)
					{
						isDBIdentiy = true;
						break;
					}
				}
			}
		}

		public boolean isDBIdentity()
		{
			return isDBIdentiy;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof RelatedHashedArguments))
			{
				return false;
			}
			RelatedHashedArguments other = ((RelatedHashedArguments)obj);

			if (Utils.equalObjects(hash, other.hash))
			{
				return true;
			}
			if (isDBIdentiy || other.isDBIdentiy)
			{
				// check for special scenario with dbidentity
				if (whereArgs != null && other.whereArgs != null && whereArgs.length == other.whereArgs.length)
				{
					boolean argsEqual = true;
					for (int i = 0; i < whereArgs.length; i++)
					{
						if (!Utils.equalObjects(whereArgs[i], other.whereArgs[i]))
						{
							argsEqual = false;
							if (whereArgs[i] instanceof DbIdentValue && Utils.equalObjects(((DbIdentValue)whereArgs[i]).getPkValue(), other.whereArgs[i]))
							{
								argsEqual = true;
							}
							if (other.whereArgs[i] instanceof DbIdentValue &&
								Utils.equalObjects(((DbIdentValue)other.whereArgs[i]).getPkValue(), whereArgs[i]))
							{
								argsEqual = true;
							}
							if (!argsEqual) break;
						}
					}
					return argsEqual;
				}
			}
			return false;
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

	private boolean alwaysFollowPkSelection = false;

	public boolean isAlwaysFollowPkSelection()
	{
		return alwaysFollowPkSelection;
	}

	public void setAlwaysFollowPkSelection(boolean alwaysFollowPkSelection)
	{
		this.alwaysFollowPkSelection = alwaysFollowPkSelection;
	}

	private boolean disableRelatedSiblingsPrefetch = true;

	public boolean isDisableRelatedSiblingsPrefetch()
	{
		return disableRelatedSiblingsPrefetch;
	}

	public void setDisableRelatedSiblingsPrefetch(boolean disableRelatedSiblingsPrefetch)
	{
		this.disableRelatedSiblingsPrefetch = disableRelatedSiblingsPrefetch;
	}

	public Object[] insertToDataSource(String name, IDataSet dataSet, ColumnType[] columnTypes, WrappedObjectReference<String[]> pkNames, boolean create,
		boolean skipOnLoad)
		throws ServoyException
	{
		return insertToDataSource(name, dataSet, columnTypes, pkNames, create, skipOnLoad, null);
	}

	public Object[] insertToDataSource(String name, IDataSet dataSet, ColumnType[] columnTypes, WrappedObjectReference<String[]> pkNames, boolean create,
		boolean skipOnLoad, String server)
		throws ServoyException
	{
		if (name == null)
		{
			return null;
		}
		WrappedObjectReference<String[]> actualPkNames = pkNames;
		if (actualPkNames == null) actualPkNames = new WrappedObjectReference<String[]>(null);

		String dataSource = IServer.VIEW_SERVER.equals(server) ? DataSourceUtils.createViewDataSource(name) : DataSourceUtils.createInmemDataSource(name);

		// initial dataset to use, but can also be set later to a 0 row dataset that is created to match columnNames and columnTypes with an in-mem
		// table definition, if that is available and columns do not match with the initial dataset
		IDataSet fixedDataSet = dataSet;

		List<ColumnType> fixedColumnTypes;
		if (columnTypes == null)
		{
			ColumnType[] dataSetTypes = BufferedDataSetInternal.getColumnTypeInfo(dataSet);
			fixedColumnTypes = dataSetTypes == null ? null : asList(dataSetTypes);
		}
		else
		{
			fixedColumnTypes = asList(columnTypes);
		}

		// get column def from the first in-mem datasource found
		ServoyJSONObject columnsDef = null;
		Iterator<TableNode> tblIte = application.getFlattenedSolution().getTableNodes(dataSource);
		int onLoadMethodId = -1;
		while (tblIte.hasNext())
		{
			TableNode tn = tblIte.next();
			if (columnsDef == null) columnsDef = tn.getColumns();
			if (onLoadMethodId == -1) onLoadMethodId = tn.getOnFoundSetLoadMethodID();
		}

		HashMap<String, ColumnInfoDef> columnInfoDefinitions = null;
		Set<Integer> columnsThatNeedToStringSerialize = null;
		if (columnsDef == null)
		{
			// if we have array columns, convert values using StringSerializer
			if (containsArrayType(fixedColumnTypes))
			{
				columnsThatNeedToStringSerialize = new HashSet<>();
				for (int i = 0; i < fixedColumnTypes.size(); i++)
				{
					ColumnType columnType = fixedColumnTypes.get(i);
					if (columnType.getSqlType() == Types.ARRAY)
					{
						fixedColumnTypes.set(i, ColumnType.getColumnType(IColumnTypes.TEXT));
						columnsThatNeedToStringSerialize.add(Integer.valueOf(i));
					}
				}
			}
		}
		else
		{
			TableDef tableInfo = DatabaseUtils.deserializeTableInfo(columnsDef);
			List<String> inmemColumnNamesThatCanSetData = new ArrayList<>(); // column names that are not SEQUENCE_AUTO_ENTER/DATABASE_IDENTITY from table node (columnsDef)
			List<ColumnType> inmemColumnTypesForColumnsThatCanSetData = new ArrayList<>(); // column types of columns that are not SEQUENCE_AUTO_ENTER/DATABASE_IDENTITY from table node (columnsDef)
			columnInfoDefinitions = new HashMap<String, ColumnInfoDef>(); // ALL column defs from design time table node (columnsDef)
			List<String> inmemPKs = new ArrayList<>(); // pk/rowid column names from design time table node (columnsDef)

			for (int j = 0; j < tableInfo.columnInfoDefSet.size(); j++)
			{
				ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(j);
				if (cid.autoEnterType != ColumnInfo.SEQUENCE_AUTO_ENTER || cid.autoEnterSubType != ColumnInfo.DATABASE_IDENTITY)
				{
					// we only support auto-enter for in-mem tables based on dbident (that is then handled by hsql)
					// that is why we only check for that here; for example, if one would define at design time a
					// uuid generator pk column on an in-mem table, that would not work; it would try to insert null into a non-nullable column
					inmemColumnNamesThatCanSetData.add(cid.name);
					inmemColumnTypesForColumnsThatCanSetData.add(cid.columnType);
				}
				if ((cid.flags & IBaseColumn.IDENT_COLUMNS) != 0)
				{
					inmemPKs.add(cid.name);
				}
				columnInfoDefinitions.put(cid.name, cid);

				// apply stringserializer on designed datasources
				if (JSONSerializerWrapper.STRING_SERIALIZER_NAME.equals(cid.converterName))
				{
					if (columnsThatNeedToStringSerialize == null)
					{
						columnsThatNeedToStringSerialize = new HashSet<>();
					}
					columnsThatNeedToStringSerialize.add(Integer.valueOf(j));
				}
			}

			if (actualPkNames.o == null && inmemPKs.size() > 0)
			{
				actualPkNames.o = inmemPKs.toArray(new String[inmemPKs.size()]);
			}

			List<String> dataSetColumnNames = asList(dataSet.getColumnNames());
			if (dataSetColumnNames.size() > 0)
			{
				// sort inmemColumnNamesThatCanSetData (and inmemColumnTypesForColumnsThatCanSetData) to the same order as  dataSet.getColumnNames()
				List<String> sortedInmemColumnNamesThatCanSetData = new ArrayList<>();
				List<ColumnType> sortedInmemColumnTypesForColumnsThatCanSetData = new ArrayList<>();

				for (int i = 0; i < dataSetColumnNames.size(); i++)
				{
					int idx = inmemColumnNamesThatCanSetData.indexOf(dataSetColumnNames.get(i));
					if (idx != -1)
					{
						sortedInmemColumnNamesThatCanSetData.add(inmemColumnNamesThatCanSetData.remove(idx));
						sortedInmemColumnTypesForColumnsThatCanSetData.add(inmemColumnTypesForColumnsThatCanSetData.remove(idx));
					}
				}
				sortedInmemColumnNamesThatCanSetData.addAll(inmemColumnNamesThatCanSetData);
				sortedInmemColumnTypesForColumnsThatCanSetData.addAll(inmemColumnTypesForColumnsThatCanSetData);
				inmemColumnNamesThatCanSetData = sortedInmemColumnNamesThatCanSetData;
				inmemColumnTypesForColumnsThatCanSetData = sortedInmemColumnTypesForColumnsThatCanSetData;
			}

			if (!dataSetColumnNames.equals(inmemColumnNamesThatCanSetData) ||
				!compareColumnTypes(fixedColumnTypes, inmemColumnTypesForColumnsThatCanSetData))
			{
				if (dataSet.getColumnCount() > 0 /*
													 * do not generate warning if this is just the initial load of a design time inmem table that adds 0 rows
													 * and doesn't care about columns
													 */
					&& !dataSetColumnNames.equals(inmemColumnNamesThatCanSetData))
				{
					Debug.warn(
						"Dataset column names definition does not match inmem table definition for datasource: " + dataSource + " columns of dataset: " + //$NON-NLS-1$ //$NON-NLS-2$
							Arrays.toString(dataSet.getColumnNames()) + ", columns of in mem definition: " + inmemColumnNamesThatCanSetData + //$NON-NLS-1$
							". The table definition will be used."); //$NON-NLS-1$
				}
				if (fixedColumnTypes != null && !compareColumnTypes(fixedColumnTypes, inmemColumnTypesForColumnsThatCanSetData))
				{
					Debug.warn("Dataset column types definition does not match inmem table definition for datasource: " + dataSource + //$NON-NLS-1$
						" types of dataset (type, length, scale): " + //$NON-NLS-1$
						fixedColumnTypes + ", types of in mem definition: " + inmemColumnTypesForColumnsThatCanSetData + //$NON-NLS-1$
						". The table definition will be used."); //$NON-NLS-1$
				}
				fixedColumnTypes = inmemColumnTypesForColumnsThatCanSetData;
				fixedDataSet = BufferedDataSetInternal.createBufferedDataSet(
					inmemColumnNamesThatCanSetData.toArray(new String[inmemColumnNamesThatCanSetData.size()]),
					fixedColumnTypes.toArray(new ColumnType[fixedColumnTypes.size()]), new ArrayList<Object[]>(), false);
			}
		}

		// check if column count of the actual data corresponds to the count of needed columns (fixedColumnTypes.size()) that are
		//   - in the if branch above (not defined as a design-time inmem table (table node))
		//       - taken from columnTypes arg or from dataset arg column type info - if available
		//       - if fixedColumnTypes is null then this is an in-mem that is not defined at design time nor does it have column types available so we will just use the columns that the dataset has
		//   - in the else branch above (already defined at design time), columns that are not auto sequences or dbidents; see inmemColumnNames/inmemColumnTypes(which is at this point the same as fixedColumnTypes)
		if (fixedColumnTypes != null && dataSet.getRowCount() > 0 && dataSet.getRow(0).length != fixedColumnTypes.size())
		{
			throw new RepositoryException("Data set rows do not match column count"); //$NON-NLS-1$
		}

		if (columnsThatNeedToStringSerialize != null)
		{
			replaceValuesWithSerializedString(dataSet, columnsThatNeedToStringSerialize);
		}

		ITable table = IServer.VIEW_SERVER.equals(server) ? viewDataSources.get(dataSource) : inMemDataSources.get(dataSource);

		if (table == null && !create)
		{
			throw new RepositoryException("Appending to non-existing datasource: " + dataSource);
		}

		GlobalTransaction gt = getGlobalTransaction();
		String tid = null;
		String serverName = server == null ? (table == null ? IServer.INMEM_SERVER : table.getServerName()) : server;
		if (gt != null)
		{
			tid = gt.getTransactionID(serverName);
		}

		if (create && table != null)
		{
			table = deleteAndCleanupInmemoryDatasource(name, dataSource, table, tid);
		}

		InsertResult insertResult = application.getDataServer()
			.insertDataSet(application.getClientID(), fixedDataSet, dataSource,
				table == null ? IServer.INMEM_SERVER : table.getServerName(), table == null ? null : table.getName() /* create temp table when null */, tid,
				fixedColumnTypes == null ? null : fixedColumnTypes.toArray(new ColumnType[fixedColumnTypes.size()]) /* inferred from dataset when null */,
				actualPkNames.o, columnInfoDefinitions);
		if (insertResult != null)
		{
			table = insertResult.getTable();
			// if the given dataset is not the dataset that is "fixed" (columns/typing fixed to the in mem design definition) and it has rows,
			// then call insertDataSet again so the data is inserted with the columns defined in the the dataset

			// we did check above that the dataSet column count matches the settable columns the foundset is supposed to have,
			// but they could be in a different order
			// TODO then the FoundsetDataSet that is created based on the foundset will not work correctly with indexes inside the foundset...
			// do we want to fix that somehow or the caller should just make sure it calls it with the correct column order?
			if (dataSet != fixedDataSet && dataSet.getRowCount() > 0)
			{
				insertResult = application.getDataServer()
					.insertDataSet(application.getClientID(), dataSet, dataSource, table.getServerName(), table.getName(), tid,
						columnTypes/* will be inferred by called method from dataset if null */,
						actualPkNames.o, columnInfoDefinitions);
			}
			if (IServer.INMEM_SERVER.equals(serverName))
			{
				inMemDataSources.put(dataSource, table);
			}
			else
			{
				viewDataSources.put(dataSource, table);
			}
			fireTableEvent(table);
			if (!skipOnLoad && dataSet.getRowCount() == 0 && onLoadMethodId > 0)
			{
				IFoundSetInternal sharedFoundSet = getSharedFoundSet(dataSource);
				executeFoundsetTriggerReturnFirst(sharedFoundSet.getTable(), new Object[] { DataSourceUtils.getInmemDataSourceName(dataSource) },
					StaticContentSpecLoader.PROPERTY_ONFOUNDSETLOADMETHODID, false, (Scriptable)sharedFoundSet);
			}
			if (create)
			{
				// only refresh when it is a new full load, when adding data to an existing table, it is only applicable to the (shared) foundset
				refreshFoundSetsFromDB(dataSource, null, false, false);
			}
			return insertResult.getGeneratedPks();
		}
		return null;
	}

	/**
	 * @param name
	 * @param dataSource
	 * @param table
	 * @param tid
	 * @return
	 * @throws ServoyException
	 */
	protected ITable deleteAndCleanupInmemoryDatasource(String name, String dataSource, ITable table, String tid) throws ServoyException
	{
		// temp table was used before, delete all data in it
		// first remove all edits for this datasource.
		if (getEditRecordList().removeRecords(dataSource))
		{
			Debug.warn("createDataSource was called while there were edited records under datasource with same name: " + name +
				". All old records that where in edit state were removed.");
		}

		FoundSet foundSet = (FoundSet)getSharedFoundSet(dataSource);
		foundSet.removeLastFound();
		try
		{
			QueryDelete delete = new QueryDelete(
				new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema(), true));
			SQLStatement deleteStatement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, table.getServerName(), table.getName(), null, tid, delete,
				null);
			application.getDataServer().performUpdates(application.getClientID(), new ISQLStatement[] { deleteStatement });
		}
		catch (Exception e)
		{
			Debug.log(e);
			table = null;
		}
		RowManager element = rowManagers.get(dataSource);
		if (element != null)
		{
			element.flushAllCachedRows();
		}
		return table;
	}

	private static void replaceValuesWithSerializedString(IDataSet dataSet, Set<Integer> columnsThatNeedToStringSerialize)
	{
		Integer[] colIndexesThatNeedToStringSerialize = columnsThatNeedToStringSerialize.toArray(new Integer[columnsThatNeedToStringSerialize.size()]);

		// run stringserializer over the rows
		JSONSerializerWrapper stringserializer = new JSONSerializerWrapper(false);
		for (int r = 0; r < dataSet.getRowCount(); r++)
		{
			Object[] row = dataSet.getRow(r).clone();
			for (Integer colIdxAsInteger : colIndexesThatNeedToStringSerialize)
			{
				int colIdx = colIdxAsInteger.intValue();
				if (row[colIdx] != null && !(row[colIdx] instanceof String))
				{
					// the data was not a string (so not pre-serialized from js), run it through the stringserializer.
					try
					{
						row[colIdx] = stringserializer.toJSON(row[colIdx]).toString();
					}
					catch (MarshallException e)
					{
						Debug.warn(e);
					}
				}
			}
			dataSet.setRow(r, row);
		}
	}

	private static boolean containsArrayType(List<ColumnType> columnTypes)
	{
		if (columnTypes != null)
		{
			for (ColumnType columnType : columnTypes)
			{
				if (columnType.getSqlType() == Types.ARRAY)
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean removeDataSource(String uri) throws RepositoryException
	{
		if (uri == null)
		{
			return false;
		}
		ITable table = inMemDataSources.remove(uri);
		if (table != null)
		{
			sharedDataSourceFoundSet.remove(uri);
			application.getDataServer().dropTemporaryTable(application.getClientID(), table.getServerName(), table.getName());
			getSQLGenerator().removeCache(uri);
			return true;
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
			if (serverName == null || serverName.equals(getDataSourceServerName(datasource)))
			{
				ITable t = getTable(datasource);
				if (Debug.tracing())
				{
					Debug.trace("Registering table '" + t.getServerName() + ". " + t.getName() + "' for client '" + application.getClientID() + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
				}
				getDataServer().addClientAsTableUser(application.getClientID(), t.getServerName(), t.getName());
			}
		}
	}

	@SuppressWarnings("nls")
	@Override
	public JSRecordMarkers validateRecord(IRecordInternal record, Object state)
	{
		if (record == null) return null;
		// always reset the validation object
		record.setRecordMarkers(null);
		// first check for a validation entity method
		ITable table = record.getParentFoundSet().getTable();
		JSRecordMarkers recordMarkers = new JSRecordMarkers(record, application, state);
		Object[] args = new Object[] { record, recordMarkers, state };
		Scriptable scope = record.getParentFoundSet() instanceof Scriptable ? (Scriptable)record.getParentFoundSet() : null;
		try
		{
			executeFoundsetTrigger(table, args, StaticContentSpecLoader.PROPERTY_ONVALIDATEMETHODID, true,
				scope);
		}
		catch (ServoyException e)
		{
			recordMarkers.addGenericException(e);
		}

		if (record.existInDataSource())
		{
			if (record.isFlaggedForDeletion())
			{
				try
				{
					// if the first returns false it will stop the rest (in line with what we had)
					if (!executeFoundsetTriggerBreakOnFalse(table, args, StaticContentSpecLoader.PROPERTY_ONDELETEMETHODID, true, scope))
					{
						recordMarkers.setOnBeforeUpdateFailed();
					}
				}
				catch (ServoyException e)
				{
					recordMarkers.addGenericException(e);
				}
			}
			else
			{
				try
				{
					// if the first returns false it will stop the rest (in line with what we had)
					if (!executeFoundsetTriggerBreakOnFalse(table, args, StaticContentSpecLoader.PROPERTY_ONUPDATEMETHODID, true, scope))
					{
						recordMarkers.setOnBeforeUpdateFailed();
					}
				}
				catch (ServoyException e)
				{
					recordMarkers.addGenericException(e);
				}
			}
		}
		else
		{
			try
			{
				// if the first returns false it will stop the rest (in line with what we had)
				if (!executeFoundsetTriggerBreakOnFalse(table, args, StaticContentSpecLoader.PROPERTY_ONINSERTMETHODID, true, scope))
				{
					recordMarkers.setOnBeforeInsertFailed();
				}
			}
			catch (ServoyException e)
			{
				recordMarkers.addGenericException(e);
			}
		}


		//check for null and length and validators
		SQLSheet sqlSheet = record.getParentFoundSet().getSQLSheet();
		record.getParentFoundSet().getTable().getColumns().forEach(column -> {
			// null
			Object rawValue = record instanceof ViewRecord ? record.getValue(column.getDataProviderID())
				: record.getRawData().getRawValue(column.getDataProviderID());
			if (isNullColumnValidatorEnabled() && !column.getAllowNull() && column.getDatabaseDefaultValue() == null &&
				(column.getColumnInfo() == null || !column.getColumnInfo().isDBManaged()) &&
				(rawValue == null || ("".equals(rawValue) && Column.mapToDefaultType(column.getType()) == IColumnTypes.TEXT)))
			{
				recordMarkers.report("i18n:servoy.record.error.null.not.allowed", column.getDataProviderID(), ILogLevel.ERROR, state,
					new Object[] { column.getDataProviderID() });
				// this would result normally in an Record.exception so for now also set that
				if (!(record instanceof ViewRecord))
				{
					record.getRawData().setLastException(
						new DataException("Column " + column.getDataProviderID() + " can't be null", ServoyException.DATA_INTEGRITY_VIOLATION));
				}
			}

			// validators only for changed columns (based on the raw, "unconverted" value)
			Object oldRawValue = record instanceof ViewRecord ? ((ViewRecord)record).getOldVaue(column.getDataProviderID())
				: record.existInDataSource() ? record.getRawData().getOldRawValue(column.getDataProviderID()) : null;
			if (!(rawValue instanceof DbIdentValue) && !Utils.equalObjects(rawValue, oldRawValue))
			{
				// the length check
				int valueLen = Column.getObjectSize(rawValue, column.getType());
				if (valueLen > 0 && column.getLength() > 0 && valueLen > column.getLength()) // insufficient space to save value
				{
					recordMarkers.report("i18n:servoy.record.error.columnSizeTooSmall", column.getDataProviderID(), ILogLevel.ERROR, state,
						new Object[] { column.getDataProviderID(), Integer.valueOf(column.getLength()), rawValue });
				}
				if (sqlSheet != null) // for ViewRecords this is null, we don't have the actual sheet here for the underlying column
				{
					Pair<String, Map<String, String>> validatorInfo = sqlSheet.getColumnValidatorInfo(sqlSheet.getColumnIndex(column.getDataProviderID()));
					if (validatorInfo != null)
					{
						IColumnValidator validator = columnValidatorManager.getValidator(validatorInfo.getLeft());
						if (validator == null)
						{
							Debug.error("Column '" + column.getDataProviderID() +
								"' does have column validator  information, but either the validator '" + validatorInfo.getLeft() +
								"'  is not available, is the validator installed? (default default_validators.jar in the plugins) or the validator information is incorrect.");

							recordMarkers.report("i18n:servoy.error.validatorNotFound", column.getDataProviderID(), ILogLevel.ERROR, state,
								new Object[] { validatorInfo.getLeft() });
						}
						else
						{
							try
							{
								if (validator instanceof IColumnValidator2)
								{
									((IColumnValidator2)validator).validate(validatorInfo.getRight(), rawValue, column.getDataProviderID(), recordMarkers,
										state);
								}
								else
								{
									validator.validate(validatorInfo.getRight(), rawValue);
								}
							}
							catch (IllegalArgumentException e)
							{
								recordMarkers.report("i18n:servoy.record.error.validation", column.getDataProviderID(), ILogLevel.ERROR, state,
									new Object[] { column.getDataProviderID(), rawValue, e.getMessage() });
							}
						}
					}
				}
			}
		});


		if (recordMarkers.isInvalid())
		{
			record.setRecordMarkers(recordMarkers);
			return recordMarkers;
		}
		return null;
	}

	public int saveData()
	{
		return editRecordList.stopEditing(false);
	}

	public int saveData(List<IRecord> recordsToSave)
	{
		return editRecordList.stopEditing(true, null, recordsToSave.stream().map(IRecordInternal.class::cast).toList());
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
		// return a copy so that modifications after this doesn't affect the one that is returned
		// also the caller can't suddenly change it.
		return new HashMap<>(trackingInfoMap);
	}

	public IQueryBuilderFactory getQueryFactory()
	{
		return new QBFactory(this, getScopesScopeProvider(), getApplication().getFlattenedSolution(), getApplication().getScriptEngine().getSolutionScope());
	}

	public IFoundSetInternal getFoundSet(String dataSource) throws ServoyException
	{
		if (dataSource.startsWith(DataSourceUtils.VIEW_DATASOURCE_SCHEME_COLON))
		{
			ViewFoundSet viewFoundSet = viewFoundSets.get(dataSource);
			if (viewFoundSet == null)
			{
				// trigger the load if it is not there yet.
				getTable(dataSource);
				viewFoundSet = viewFoundSets.get(dataSource);
				if (viewFoundSet == null)
				{
					Debug.warn("The ViewFoundSet for datasource " + dataSource +
						" is not registered, use the entity onload method to register through 'datasources.view.xxx.getViewFoundset(select)' or 'databaseManager.getViewFoundSet(memOrViewName,sql,true)'");
				}
			}
			return viewFoundSet;
		}
		if (DataSourceUtils.getMenuDataSourceName(dataSource) != null)
		{
			return getAndCreateMenuFoundset(dataSource);
		}

		if (dataSource.startsWith(DataSourceUtils.INMEM_DATASOURCE) &&
			hasFoundsetTrigger(dataSource, StaticContentSpecLoader.PROPERTY_ONFOUNDSETNEXTCHUNKMETHODID))
		{
			return getSharedFoundSet(dataSource);
		}

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

	@SuppressWarnings("nls")
	@Override
	public ViewFoundSet getViewFoundSet(String name, QBSelect query, boolean register)
	{
		if (query.getQuery().getColumns() == null || query.getQuery().getColumns().size() == 0)
		{
			throw new RuntimeException("Can't create a ViewFoundset with name: " + name + " and query  " + query + " that has no columns");
		}
		String dataSource = DataSourceUtils.createViewDataSource(name);
		ViewFoundSet vfs = new ViewFoundSet(dataSource, query.build(), application.getFoundSetManager(), config.pkChunkSize());

		// if this datasource defintion is created already in the developer then we need to check if the query columns are correctly matching it.
		ServoyJSONObject columnsDef = null;
		Iterator<TableNode> tblIte = application.getFlattenedSolution().getTableNodes(dataSource);
		while (tblIte.hasNext() && columnsDef == null)
		{
			TableNode tn = tblIte.next();
			columnsDef = tn.getColumns();
			if (columnsDef != null)
			{
				TableDef def = DatabaseUtils.deserializeTableInfo(columnsDef);
				for (ColumnInfoDef col : def.columnInfoDefSet)
				{
					IQuerySelectValue selectValue = getSelectvalue(query, col.name);
					if (selectValue == null)
					{
						Debug.error("Column " + col.name + " of type " + col.columnType.toString() + " defined in view datasource '" + dataSource +
							"' was not found in the provided query.");
						return null;
					}

					BaseColumnType columnType = selectValue.getColumnType();
					// relax the mapping on default Servoy types
					if (columnType != null &&
						Column.mapToDefaultType(columnType.getSqlType()) != Column.mapToDefaultType(col.columnType.getSqlType()))
					{
						Debug.error(
							"Column type for column '" + col.name + " of type " + col.columnType.toString() + "' defined in view datasource '" + dataSource +
								"' does not match the one " + columnType + " provided in the query.");
						return null;
					}
				}
			}
		}

		registerViewFoundSet(vfs, !register);

		return vfs;
	}

	private IQuerySelectValue getSelectvalue(QBSelect query, String name)
	{
		return query.getQuery()
			.getColumns()
			.stream() //
			.filter(column -> name.equals(column.getAliasOrName())) //
			.findAny()
			.orElse(null);
	}

	@Override
	public void registerRelatedMenuFoundSet(MenuFoundSet foundset)
	{
		relatedMenuFoundSets.add(foundset);
	}

	@Override
	public void unregisterRelatedMenuFoundSet(MenuFoundSet foundset)
	{
		relatedMenuFoundSets.remove(foundset);
	}

	@Override
	public boolean registerViewFoundSet(ViewFoundSet foundset, boolean onlyWeak)
	{
		if (foundset == null) return false;
		if (onlyWeak)
		{
			noneRegisteredVFS.put(foundset, TRUE);
		}
		else
		{
			ViewFoundSet oldValue = viewFoundSets.put(foundset.getDataSource(), foundset);
			ITable table = foundset.getTable();
			if (!viewDataSources.containsKey(foundset.getDataSource()))
				viewDataSources.put(foundset.getDataSource(), table);
			if (oldValue != null)
			{
				for (IFormController controller : application.getFormManager().getCachedFormControllers())
				{
					if (controller.getFormModel() == oldValue)
					{
						controller.loadAllRecords();
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean unregisterViewFoundSet(String datasource)
	{
		boolean unregistered = viewFoundSets.remove(datasource) != null;
		if (unregistered)
		{
			viewDataSources.remove(datasource);
		}
		return unregistered;
	}

	@Override
	public ViewFoundSet getRegisteredViewFoundSet(String name)
	{
		if (name == null) return null;
		if (name.startsWith(DataSourceUtils.VIEW_DATASOURCE_SCHEME_COLON))
		{
			return viewFoundSets.get(name);
		}
		return viewFoundSets.get(DataSourceUtils.createViewDataSource(name));
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

		String serverName = getDataSourceServerName(select.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { select.getDataSource() }));

		return getDataSetByQuery(serverName, select.build(), useTableFilters, max_returned_rows);
	}

	private static boolean compareColumnTypes(List<ColumnType> types1, List<ColumnType> types2)
	{
		if (types1 == types2) return true;
		if (types1 == null || types2 == null) return false;

		int length = types1.size();
		if (types2.size() != length) return false;

		for (int i = 0; i < length; i++)
		{
			ColumnType type1 = types1.get(i);
			ColumnType type2 = types2.get(i);

			if (!Column.isColumnInfoCompatible(type1, type2, false))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public int getConvertedTypeForColumn(IColumn column, boolean mapToDefaultType)
	{
		int type = mapToDefaultType ? column.getDataProviderType() : (column instanceof Column ? ((Column)column).getType() : column.getDataProviderType());
		ColumnInfo ci = column.getColumnInfo();
		if (ci != null && ci.getConverterName() != null && ci.getConverterName().trim().length() != 0)
		{
			IColumnConverter columnConverter = ((FoundSetManager)application.getFoundSetManager()).getColumnConverterManager()
				.getConverter(
					ci.getConverterName());
			if (columnConverter instanceof ITypedColumnConverter)
			{
				try
				{
					int convType = ((ITypedColumnConverter)columnConverter).getToObjectType(
						ComponentFactory.<String> parseJSonProperties(ci.getConverterProperties()));
					if (convType != Integer.MAX_VALUE)
					{
						type = Column.mapToDefaultType(convType);
					}
				}
				catch (IOException e)
				{
					Debug.error("Exception loading properties for converter " + columnConverter.getName() + ", properties: " + ci.getConverterProperties(), e);
				}
			}
		}
		return type;
	}

	void clearFlushActions()
	{
		runOnEditOrTransactionStoppedActions.clear();
	}

	@Override
	public Collection<String> getViewFoundsetDataSourceNames()
	{
		List<String> viewFoundsetDataSourceNames = new ArrayList<>(viewDataSources.size());
		for (String dataSource : viewDataSources.keySet())
		{
			viewFoundsetDataSourceNames.add(getViewDataSourceName(dataSource));
		}
		return viewFoundsetDataSourceNames;
	}


	boolean executeFoundsetTriggerBreakOnFalse(ITable table, Object[] args, TypedProperty<Integer> property, boolean throwException, Scriptable foundsetScope)
		throws ServoyException
	{
		return Boolean.TRUE.equals(executeFoundsetTriggerInternal(table, args, property, BreakOnFalse, throwException, foundsetScope));
	}

	Object executeFoundsetTriggerReturnFirst(ITable table, Object[] args, TypedProperty<Integer> property, boolean throwException, Scriptable foundsetScope)
		throws ServoyException
	{
		return executeFoundsetTriggerInternal(table, args, property, ReturnFirst, throwException, foundsetScope);
	}

	void executeFoundsetTrigger(ITable table, Object[] args, TypedProperty<Integer> property, boolean throwException, Scriptable foundsetScope)
		throws ServoyException
	{
		executeFoundsetTriggerInternal(table, args, property, ExecuteEach, throwException, foundsetScope);
	}

	private List<TriggerFunction> getTriggerFunctions(ITable table, TypedProperty<Integer> property, Scriptable foundsetScope)
	{
		FlattenedSolution solutionRoot = getApplication().getFlattenedSolution();
		IExecutingEnviroment scriptEngine = getApplication().getScriptEngine();

		return stream(solutionRoot.getTableNodes(table)).map(tableNode -> {
			Object function = null;
			Scriptable scope = null;
			ScriptMethod scriptMethod = solutionRoot.getScriptMethod(((Integer)tableNode.getProperty(property.getPropertyName())).intValue());
			if (scriptMethod != null)
			{
				if (scriptMethod.getParent() instanceof Solution)
				{
					// global method
					GlobalScope gs = scriptEngine.getScopesScope().getGlobalScope(scriptMethod.getScopeName());
					if (gs != null)
					{
						scope = gs;
						function = gs.get(scriptMethod.getName());
					}
				}
				else if (foundsetScope != null)
				{
					// foundset method
					scope = foundsetScope;
					function = scope.getPrototype().get(scriptMethod.getName(), scope);
				}
			}
			if (function instanceof Function)
			{
				return new TriggerFunction((Function)function, scope, tableNode);
			}
			return null;
		}).filter(Objects::nonNull).collect(toList());
	}

	private Object executeFoundsetTriggerInternal(ITable table, Object[] args, TypedProperty<Integer> property, TriggerExecutionMode executionMode,
		boolean throwException, Scriptable foundsetScope) throws ServoyException
	{
		IExecutingEnviroment scriptEngine = getApplication().getScriptEngine();

		List<TriggerFunction> functions = getTriggerFunctions(table, property, foundsetScope);
		if (executionMode == ReturnFirst && functions.size() > 1)
		{
			Debug.warn("Multiple event handlers found for event " + property.getPropertyName() + ", only executing one");
		}

		for (TriggerFunction function : functions)
		{
			try
			{
				Object returnValue = scriptEngine.executeFunction(function.getFunction(), function.getScope(), function.getScope(),
					arrayMerge(args, parseJSExpressions(function.getTableNode().getFlattenedMethodArguments(property.getPropertyName()))), false,
					throwException);
				if (executionMode == ReturnFirst || (executionMode == BreakOnFalse && Boolean.FALSE.equals(returnValue)))
				{
					// return first value or break on false return, do not execute remaining triggers.
					return returnValue;
				}
			}
			catch (JavaScriptException e)
			{
				// update or insert method threw exception.
				throw new DataException(ServoyException.RECORD_VALIDATION_FAILED, e.getValue(), e).setContext(this.toString());
			}
			catch (EcmaError e)
			{
				throw new ApplicationException(ServoyException.SAVE_FAILED, e).setContext(this.toString());
			}
			catch (Exception e)
			{
				Debug.error(e);
				throw new ServoyException(ServoyException.SAVE_FAILED, new Object[] { e.getMessage() }).setContext(this.toString());
			}
		}

		return Boolean.TRUE;
	}

	enum TriggerExecutionMode
	{
		ReturnFirst, BreakOnFalse, ExecuteEach
	}

	private static class TriggerFunction
	{
		private final Function function;
		private final Scriptable scope;
		private final TableNode tableNode;

		public TriggerFunction(Function function, Scriptable scope, TableNode tableNode)
		{
			this.function = function;
			this.scope = scope;
			this.tableNode = tableNode;
		}

		public Scriptable getScope()
		{
			return scope;
		}

		public Function getFunction()
		{
			return function;
		}

		public TableNode getTableNode()
		{
			return tableNode;
		}
	}

	public boolean hasFoundsetTrigger(String dataSource, TypedProperty<Integer> property)
	{
		return stream(application.getFlattenedSolution().getTableNodes(dataSource))
			.mapToInt(tableNode -> tableNode.getTypedProperty(property))
			.anyMatch(methodId -> methodId > 0);
	}

	public QuerySet getQuerySet(QuerySelect select, boolean includeFilters) throws RepositoryException
	{
		if (select.getColumns() == null || !includeFilters)
		{
			// Do not modify the input
			select = deepClone(select);
		}

		String serverName = getDataSourceServerName(select.getTable().getDataSource());
		ArrayList<TableFilter> tfParams = null;
		if (includeFilters)
		{
			tfParams = getTableFilterParams(serverName, select);
		}
		else
		{
			// get the sql without any filters
			select.clearCondition(SQLGenerator.CONDITION_FILTER);
			select.removeUnusedJoins(false, true);
		}

		if (select.getColumns() == null)
		{
			// no columns, add pk
			BaseQueryTable qTable = select.getTable();
			ITable table = getTable(qTable.getDataSource());
			List<Column> pks = table.getRowIdentColumns();
			if (pks.isEmpty())
			{
				throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { table.getName() });
			}

			pks.stream()
				.map(col -> col.queryColumn(qTable))
				.forEach(select::addColumn);
		}

		return application.getDataServer().getSQLQuerySet(serverName, select, tfParams, 0, -1, true, true);
	}

	@Override
	public void setTenantValue(Solution solution, Object value)
	{
		int count = 0;
		try
		{
			// get tenant columns
			for (IServer server : solution.getServerProxies().values())
			{
				List<TableFilterRequest> tableFilterRequests = null;
				for (ColumnName tenantColumn : server.getTenantColumns())
				{
					count++;
					ITable table = server.getTable(tenantColumn.getTableName());

					if (tableFilterRequests == null) tableFilterRequests = new ArrayList<>();
					if (value != null)
					{
						tableFilterRequests.add(new TableFilterRequest(table,
							createDataproviderTableFilterdefinition(table, tenantColumn.getColumnName(), "=", value), //$NON-NLS-1$
							true));
					}
					// else filter will be removed if it exists for this server
				}
				if (tableFilterRequests != null)
				{
					setTableFilters(TENANT_FILTER, server.getName(), tableFilterRequests, true, true);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		if (count == 0)
		{
			Debug.warn("setTenantValue: No tenant columns found, value is ignored!");
		}
		else
		{
			Debug.debug("setTenantValue: A tenant value was " + (value == null ? "cleared" : "set") + " for " + count + " tenant columns.");
		}
	}

	@Override
	public Object[] getTenantValue()
	{
		return getTableFilters(TENANT_FILTER).stream()
			.map(TableFilter::createBroadcastFilter)
			.filter(Objects::nonNull)
			.map(bf -> bf.getFilterValue())
			.findAny().orElse(null);
	}

}
