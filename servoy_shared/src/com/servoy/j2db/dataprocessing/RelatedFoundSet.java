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


import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.visitor.PackVisitor;

/**
 * This class is normally found as related state from another state and therefore holds related data
 *
 * @author jblok
 */
public abstract class RelatedFoundSet extends FoundSet
{

	private static NativeJavaMethod maxRecord;

	static
	{
		try
		{
			maxRecord = new NativeJavaMethod(FoundSet.class.getMethod("js_getMaxRecordIndex", (Class[])null), "getMaxRecordIndex"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	protected RelatedFoundSet(IDataSet data, QuerySelect select, IFoundSetManagerInternal app, IRecordInternal parent, String relationName, SQLSheet sheet,
		List<SortColumn> defaultSortColumns, QuerySelect aggregateSelect, IDataSet aggregateData) throws ServoyException
	{
		super(app, parent, relationName, sheet, null, defaultSortColumns);

		if (data == null)
		{
			getPksAndRecords().setPksAndQuery(new BufferedDataSet(), 0, select);
		}
		else
		{
			IDataSet pks = createPKDataSet(sheet, data);

			SafeArrayList<IRecordInternal> cachedRecords = getPksAndRecords().setPksAndQuery(pks, pks.getRowCount(), select);
			for (int row = 0; row < pks.getRowCount(); row++)
			{
				Object[] rowArray = data.getRow(row);
				// fire delayed so that this constructor will end before fires will touch this relatedfoundset again.
				List<Runnable> fireRunnables = new ArrayList<Runnable>(1);
				Row rowData = rowManager.getRowBasedonPKFromEntireColumnArray(rowArray, fireRunnables);
				fsm.registerFireRunnables(fireRunnables);
				Record state = new Record(this, rowData);
				cachedRecords.set(row, state);
			}

			if (pks != null && pks.getRowCount() > 0)
			{
				setSelectedIndex(0);
			}
		}

		//fix the select from related_SQL to relatedPK_SQL
		ArrayList<IQuerySelectValue> pkColumns = new ArrayList<IQuerySelectValue>();
		Iterator<Column> pkIt = sheet.getTable().getRowIdentColumns().iterator();
		while (pkIt.hasNext())
		{
			Column column = pkIt.next();
			pkColumns.add(new QueryColumn(select.getTable(), column.getID(), column.getSQLName(), column.getType(), column.getLength(), column.getScale(),
				column.getFlags()));
		}
		select.setColumns(pkColumns);
		creationSqlSelect = AbstractBaseQuery.deepClone(select);

		if (aggregateData != null)
		{
			fillAggregates(aggregateSelect, aggregateData);
		}

		initialized = true;
	}


	//can only used by findState
	protected RelatedFoundSet(IFoundSetManagerInternal app, IRecordInternal parent, String relationName, SQLSheet sheet) throws ServoyException
	{
		super(app, parent, relationName, sheet, null, null);
		getPksAndRecords().setPksAndQuery(new BufferedDataSet(), 0, null);
		initialized = true;
	}

	/**
	 * Create multiple related foundsets in one call to the data server.
	 *
	 * @param factory
	 * @param app
	 * @param parents same length as whereArsgLists
	 * @param relation
	 * @param sheet
	 * @param whereArsgLists
	 * @param defaultSortColumns
	 * @return
	 * @throws ServoyException
	 */
	public static IFoundSetInternal[] createRelatedFoundSets(IFoundSetFactory factory, IFoundSetManagerInternal app, IRecordInternal[] parents,
		Relation relation, SQLSheet sheet, Object[][] whereArsgLists, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (sheet == null)
		{
			throw new IllegalArgumentException(app.getApplication().getI18NMessage("servoy.foundSet.error.sqlsheet")); //$NON-NLS-1$
		}

		FoundSetManager fsm = (FoundSetManager)app;

		List<SortColumn> sortColumns;
		if (defaultSortColumns == null || defaultSortColumns.size() == 0)
		{
			sortColumns = sheet.getDefaultPKSort();
		}
		else
		{
			sortColumns = defaultSortColumns;
		}

		QuerySelect cleanSelect = fsm.getSQLGenerator().getPKSelectSqlSelect(fsm.getScopesScopeProvider(), sheet.getTable(), null, null, true, null,
			sortColumns, false);

		QuerySelect relationSelect = (QuerySelect)sheet.getRelatedSQLDescription(relation.getName()).getSQLQuery();
		//don't select all columns in pk select
		cleanSelect.setColumns(AbstractBaseQuery.relinkTable(relationSelect.getTable(), cleanSelect.getTable(), relationSelect.getColumnsClone()));

		//copy the where (is foreign where)
		cleanSelect.setCondition(SQLGenerator.CONDITION_RELATION, AbstractBaseQuery.relinkTable(relationSelect.getTable(), cleanSelect.getTable(),
			relationSelect.getConditionClone(SQLGenerator.CONDITION_RELATION)));

		TablePlaceholderKey placeHolderKey = SQLGenerator.createRelationKeyPlaceholderKey(cleanSelect.getTable(), relation.getName());

		QuerySelect[] sqlSelects = new QuerySelect[whereArsgLists.length]; // all queries
		QuerySelect[] aggregateSelects = new QuerySelect[whereArsgLists.length]; // all aggregates
		List<Integer> queryIndex = new ArrayList<Integer>(whereArsgLists.length);
		Map<Integer, Row> cachedRows = new HashMap<Integer, Row>();
		List<QueryData> queryDatas = new ArrayList<QueryData>(whereArsgLists.length);

		String transactionID = fsm.getTransactionID(sheet);
		String clientID = fsm.getApplication().getClientID();
		ArrayList<TableFilter> sqlFilters = fsm.getTableFilterParams(sheet.getServerName(), cleanSelect);

		for (int i = 0; i < whereArsgLists.length; i++)
		{
			Object[] whereArgs = whereArsgLists[i];
			if (whereArgs == null || whereArgs.length == 0)
			{
				throw new IllegalArgumentException(app.getApplication().getI18NMessage("servoy.relatedfoundset.error.noFK") + relation.getName()); //$NON-NLS-1$
			}

			QuerySelect sqlSelect;
			if (i == whereArsgLists.length - 1)
			{
				sqlSelect = cleanSelect; // the last one, use the template, no clone needed
			}
			else
			{
				sqlSelect = AbstractBaseQuery.deepClone(cleanSelect);
			}

			if (!sqlSelect.setPlaceholderValue(placeHolderKey, whereArgs))
			{
				Debug.error(new RuntimeException("Could not set placeholder " + placeHolderKey + " in query " + sqlSelect + "-- continuing")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}
			sqlSelects[i] = sqlSelect;

			// Check for non-empty where-arguments, joins on null-conditions are not allowed (similar to FK constraints in databases)
			if (!whereArgsIsEmpty(whereArgs))
			{
				Row cachedRow = null;
				if (relation.isFKPKRef(fsm.getApplication().getFlattenedSolution()))
				{
					// optimize for FK->PK relation, if the data is already cached, do not query
					RowManager rowManager = fsm.getRowManager(relation.getForeignDataSource());
					if (rowManager != null)
					{
						cachedRow = rowManager.getCachedRow(whereArgs).getLeft();
					}
				}
				if (cachedRow != null)
				{
					if (Debug.tracing())
					{
						Debug.trace(Thread.currentThread().getName() + ": Found cached FK record"); //$NON-NLS-1$
					}
					cachedRows.put(Integer.valueOf(i), cachedRow);
				}
				else
				{
					ISQLSelect selectStatement = AbstractBaseQuery.deepClone((ISQLSelect)sqlSelect);
					// Note: put a clone of sqlSelect in the queryDatas list, we will compress later over multiple queries using pack().
					// Clone is needed because packed queries may not be save to manipulate.
					SQLStatement trackingInfo = null;
					if (fsm.getEditRecordList().hasAccess(sheet.getTable(), IRepository.TRACKING_VIEWS))
					{
						trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, sheet.getServerName(), sheet.getTable().getName(), null, null);
						trackingInfo.setTrackingData(sheet.getColumnNames(), new Object[][] { }, new Object[][] { }, fsm.getApplication().getUserUID(),
							fsm.getTrackingInfo(), fsm.getApplication().getClientID());
					}
					queryDatas.add(new QueryData(selectStatement, sqlFilters, !sqlSelect.isUnique(), 0, fsm.initialRelatedChunkSize, IDataServer.RELATION_QUERY,
						trackingInfo));
					queryIndex.add(Integer.valueOf(i));

					QuerySelect aggregateSelect = FoundSet.getAggregateSelect(sheet, sqlSelect);
					if (aggregateSelect != null)
					{
						// Note: see note about clone above.
						queryDatas.add(new QueryData(AbstractBaseQuery.deepClone((ISQLSelect)aggregateSelect),
							fsm.getTableFilterParams(sheet.getServerName(), aggregateSelect), false, 0, 1, IDataServer.AGGREGATE_QUERY, null));
						queryIndex.add(Integer.valueOf(i)); // same index for aggregates
						aggregateSelects[i] = aggregateSelect;
					}
				}
			}
		}

		IDataSet[] dataSets = null;
		if (queryDatas.size() > 0)
		{
			try
			{
				// pack is safe here because queryDatas contains only cloned ISQLSelect objects
				QueryData[] qDatas = queryDatas.toArray(new QueryData[queryDatas.size()]);
				AbstractBaseQuery.acceptVisitor(qDatas, new PackVisitor());

				int size = 0;
				if (Debug.tracing()) // trace the message size
				{
					try
					{
						ByteArrayOutputStream bs = new ByteArrayOutputStream();
						ObjectOutputStream os = new ObjectOutputStream(bs);
						os.writeObject(qDatas);
						os.close();
						size = bs.size();
					}
					catch (Exception e)
					{
						Debug.trace(e);
					}
				}

				long time = System.currentTimeMillis();
				dataSets = fsm.getDataServer().performQuery(clientID, sheet.getServerName(), transactionID, qDatas);
				if (Debug.tracing())
				{
					Debug.trace(Thread.currentThread().getName() + ": Relation query: " + relation.getName() + " with: " + qDatas.length + //$NON-NLS-1$ //$NON-NLS-2$
						" queries,query size: " + size + ",time: " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			catch (RepositoryException re)
			{
				testException(clientID, re);
				throw re;
			}
			catch (RemoteException e)
			{
				testException(clientID, e.getCause());
				throw new RepositoryException(e);
			}
		}

		IFoundSetInternal[] foundsets = new RelatedFoundSet[whereArsgLists.length];
		int d = 0;
		for (int i = 0; i < whereArsgLists.length; i++)
		{
			IDataSet data;
			IDataSet aggregateData = null;
			int index = (d >= queryIndex.size()) ? -1 : queryIndex.get(d).intValue();
			if (index == i)
			{
				// regular query
				data = dataSets[d++];

				// optionally followed by aggregate
				index = (d >= queryIndex.size()) ? -1 : queryIndex.get(d).intValue();
				if (index == i)
				{
					// aggregate
					aggregateData = dataSets[d++];
				}
			}
			else
			{
				data = new BufferedDataSet();
				Row row = cachedRows.get(Integer.valueOf(i));
				if (row != null)
				{
					// cached
					data.addRow(row.getRawColumnData());
				}
				// else whereArgsIsEmpty
			}

			foundsets[i] = factory.createRelatedFoundSet(data, sqlSelects[i], app, parents[i], relation.getName(), sheet, sortColumns, aggregateSelects[i],
				aggregateData);
			if (aggregateData != null && foundsets[i] instanceof FoundSet)
			{
				((FoundSet)foundsets[i]).fillAggregates(aggregateSelects[i], aggregateData);
			}
		}

		if (d != queryIndex.size())
		{
			// should never happen!
			throw new RepositoryException("Related query parameters out of sync " + d + '/' + queryIndex.size()); //$NON-NLS-1$
		}

		return foundsets;
	}

	private static void testException(String clientID, Throwable t)
	{
		if (Debug.tracing() && t instanceof RepositoryException)
		{
			RepositoryException re = (RepositoryException)t;
			if (re.getErrorCode() == ServoyException.InternalCodes.CLIENT_NOT_REGISTERED)
			{
				Debug.trace("Client not registered on server, expecting a reconnect: " + clientID, re); //$NON-NLS-1$
			}
			else
			{
				Debug.trace("Error getting related foundsets for clientID: " + clientID, re); //$NON-NLS-1$
			}
		}
	}

	//related foundsets based on null cannot exist by SQL definition
	private static boolean whereArgsIsEmpty(Object[] whereArgs)
	{
		boolean empty = true;
		for (Object element : whereArgs)
		{
			if (element instanceof DbIdentValue && ((DbIdentValue)element).getPkValue() == null) return true;
			if (element != null) empty = false;
		}
		return empty;//only return true when all null (in case of multi key)
	}

	@Override
	protected int newRecord(Row rowData, int indexToAdd, boolean changeSelection, boolean javascriptRecord) throws ServoyException
	{
		checkQueryForUpdates();
		return super.newRecord(rowData, indexToAdd, changeSelection, javascriptRecord);
	}

	@Override
	protected int getRecordIndex(Object[] pk)
	{
		checkQueryForUpdates(); // make foundset is loaded, the query may insert records before record parameter.
		return super.getRecordIndex(pk);
	}

	@Override
	public void loadAllRecords() throws ServoyException
	{
		// Also clear omit in browse all/refresh from db
		// don't do it in refreshFromDb because then
		// the omits can be cleared if there is a refresh
		// from db coming from outside or a search that has no results.
		clearOmit(null);

		refreshFromDBInternal(AbstractBaseQuery.deepClone(creationSqlSelect), true, false, fsm.pkChunkSize, false, false);
	}

	@Override
	public int getSize()
	{
		checkQueryForUpdates();
		return super.getSize();
	}


	/**
	 * Get the arguments hash this related foundSet was created with. Hash is based on values converted using column converters.
	 * @return
	 */
	public String getWhereArgsHash()
	{
		Placeholder ph = creationSqlSelect.getPlaceholder(SQLGenerator.createRelationKeyPlaceholderKey(creationSqlSelect.getTable(), getRelationName()));
		if (ph == null || !ph.isSet())
		{
			if (!findMode)
			{
				Debug.error("RelatedFoundset, creation args not found\nplaceholder=" + ph + "\nrelation=" + getRelationName() + "\ncreationSqlSelect=" + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					creationSqlSelect, new RuntimeException("RelatedFoundset, creation args not found!!"));
			}
			return null; // how can this happen (other then in find mode) ??
		}

		Relation relation = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
		if (relation == null)
		{
			throw new IllegalStateException("Relation not found for related foundset: " + relationName); //$NON-NLS-1$
		}

		Object[][] foreignData = (Object[][])ph.getValue();
		Column[] columns;
		try
		{
			columns = relation.getForeignColumns(fsm.getApplication().getFlattenedSolution());
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			throw new IllegalStateException("Relation columns not found for related foundset: " + relationName); //$NON-NLS-1$
		}

		if (columns.length != foreignData.length)
		{
			// safety check, should not happen
			throw new IllegalStateException("Relation where-args inconsistent with columns for relation" + relationName); //$NON-NLS-1$
		}

		Object[] whereArgs = new Object[foreignData.length];
		for (int i = 0; i < foreignData.length; i++)
		{
			// Use converted value for hash
			int colindex = getSQLSheet().getColumnIndex(columns[i].getDataProviderID());
			whereArgs[i] = getSQLSheet().convertValueToObject(foreignData[i][0], colindex, fsm.getColumnConverterManager());
		}

		return RowManager.createPKHashKey(whereArgs);
	}

	@Override
	public boolean addFilterParam(String filterName, String dataprovider, String operator, Object value)
	{
		// don't do anything, can't add parameters to related foundset
		fsm.getApplication().reportJSError("Cannot addFoundSetFilterParam to related foundset", null); //$NON-NLS-1$
		return false;//would also cause problem due to sharing related foundsets for different records
	}

	@Override
	@ServoyClientSupport(mc = false, wc = false, sc = false)
	public void js_clear()
	{
		// don't do anything, can't clear related data
		fsm.getApplication().reportJSError("Clearing a relatedfoundset is not possible", null); //$NON-NLS-1$
	}

	private static IDataSet createPKDataSet(SQLSheet sheet, IDataSet data)//extract primary columns from all the related data
	{
		if (data.getRowCount() == 0)//performance enhancement
		{
			return new BufferedDataSet();
		}

		int[] columns = sheet.getPKIndexes();// new int[old.size()];
		return new BufferedDataSet(data, columns);
	}

	@Override
	public Object get(java.lang.String name, Scriptable start)
	{
		if ("recordIndex".equals(name)) //$NON-NLS-1$
		{
			return Integer.valueOf(getSelectedIndex() + 1);
		}
		else if ("getMaxRecordIndex".equals(name)) //adding a method here must also  be added in Ident.java //$NON-NLS-1$
		{
			return maxRecord;
		}
		return super.get(name, start);
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		boolean b = super.has(name, start);
		if (!b)
		{
			b = "getMaxRecordIndex".equals(name); //$NON-NLS-1$
		}
		return b;
	}

	@Override
	public void put(java.lang.String name, Scriptable start, java.lang.Object value)
	{
		if ("recordIndex".equals(name)) //$NON-NLS-1$
		{
			int i = Utils.getAsInteger(value) - 1;
			if (i >= 0 && i < getSize())
			{
				setSelectedIndex(i);
			}
			return;
		}
		else if ("getMaxRecordIndex".equals(name)) //$NON-NLS-1$
		{
			// don't set
			return;
		}
		super.put(name, start, value);
	}

	/*
	 * _____________________________________________________________ Methods from AbstractEditListModel for use if portal is using a JEditList
	 */

	@Override
	public IRecordInternal getRecord(int row)
	{
		checkQueryForUpdates();
		return super.getRecord(row);//for new/dup records (ontop records are handled above)
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.FoundSet#recordsUpdated(java.util.List, java.util.List, java.util.HashMap)
	 */
	@Override
	protected void recordsUpdated(List<Record> records, List<String> aggregatesToRemove)
	{
		super.recordsUpdated(records, aggregatesToRemove);
		for (int i = 0; i < records.size(); i++)
		{
			IRecordInternal record = records.get(i);
			notifyChange_checkForUpdate(record.getRawData(), false);
		}
	}

	private boolean isInNotify = false;

	@Override
	public void notifyChange(RowEvent e) //this method is only called if I'm not the source of the event
	{
		if (!isInNotify)//prevent circle calling
		{
			try
			{
				isInNotify = true;

				// ROW CAN BE NULL ON UPDATE
				Row r = e.getRow();
				if (e.getType() == RowEvent.INSERT)
				{
					notifyChange_checkForNewRow(r);
				}
				else
				{
					if ((e.getType() == RowEvent.UPDATE //
					|| (e.getType() == RowEvent.PK_UPDATED && e.getOldPkHash() == null)) // pk was updated by another client (oldpkhash is filled when updated by self)
					&& getPksAndRecords().getPks() != null)
					{
						if (r == null || (e.getType() == RowEvent.PK_UPDATED && e.getOldPkHash() == null))
						{
							// cached row was not found, check if a column was updated that the relation depends on
							if (e.getChangedColumnNames() != null)
							{
								if (hasForeignColumn(e.getChangedColumnNames()))
								{
									invalidateFoundset();
									getFoundSetManager().getEditRecordList().fireEvents();
								}
								return;//make sure processing stops here
							}
						}
						else
						{
							boolean mustLookForMore = notifyChange_checkForUpdate(r, true);
							if (mustLookForMore)//this code is entered when a foreign column is changed and row falls into this foundset
							{
								notifyChange_checkForNewRow(r);
							}
							return;//make sure processing stops here
						}
					}
					super.notifyChange(e);
				}
			}
			finally
			{
				isInNotify = false;
			}
		}
	}

	private boolean hasForeignColumn(Object[] changedColumnNames)
	{
		if (changedColumnNames != null)
		{
			Relation relation = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
			if (relation == null)
			{
				throw new IllegalStateException("Relation not found for related foundset: " + relationName); //$NON-NLS-1$
			}

			try
			{
				Column[] foreignColumns = relation.getForeignColumns(fsm.getApplication().getFlattenedSolution());
				for (Column column : foreignColumns)
				{
					for (Object columnName : changedColumnNames)
					{
						if (column.getName().equals(columnName))
						{
							return true;
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		return false;
	}

	private boolean notifyChange_checkForUpdate(Row r, boolean updateTest)
	{
		// if already in state for new query then don't test anything.
		if (mustQueryForUpdates)
		{
			return false;
		}
		boolean retval = true;
		String pkHash = r.getPKHashKey();
		Relation relation = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
		if (relation == null)
		{
			throw new IllegalStateException("Relation not found for related foundset: " + relationName); //$NON-NLS-1$
		}

		Placeholder whereArgsPlaceholder = creationSqlSelect.getPlaceholder(
			SQLGenerator.createRelationKeyPlaceholderKey(creationSqlSelect.getTable(), relation.getName()));
		if (whereArgsPlaceholder == null || !whereArgsPlaceholder.isSet())
		{
			Debug.error("creationSqlSelect = " + creationSqlSelect); //$NON-NLS-1$
			Debug.error("PlaceholderKey = " + SQLGenerator.createRelationKeyPlaceholderKey(creationSqlSelect.getTable(), relation.getName())); //$NON-NLS-1$
			Debug.error("RelatedFoundSet = " + this); //$NON-NLS-1$
			Debug.error("Row = " + r); //$NON-NLS-1$
			Debug.error("whereArgsPlaceholder = " + whereArgsPlaceholder); //$NON-NLS-1$

			// log on server as well
			try
			{
				fsm.getApplication().getDataServer().logMessage("creationSqlSelect = " + creationSqlSelect + '\n' + "PlaceholderKey = " + //$NON-NLS-1$ //$NON-NLS-2$
					SQLGenerator.createRelationKeyPlaceholderKey(creationSqlSelect.getTable(), relation.getName()) + '\n' + "RelatedFoundSet = " + this + '\n' + //$NON-NLS-1$
					"Row = " + r + '\n' + "whereArgsPlaceholder = " + whereArgsPlaceholder); //$NON-NLS-1$//$NON-NLS-2$
			}
			catch (Exception e)
			{
				Debug.error("Failed to log on server", e); //$NON-NLS-1$
			}

			Debug.error("Relation values not set for related foundset " + relation.getName()); //$NON-NLS-1$
			return false;
		}
		// createWhereArgs is a matrix as wide as the relation keys and 1 deep
		Object[][] createWhereArgs = (Object[][])whereArgsPlaceholder.getValue(); // Really get only the where params not all of them (like table filter)

		SQLSheet.SQLDescription sqlDesc = sheet.getRelatedSQLDescription(relationName);
		List<String> req = sqlDesc.getRequiredDataProviderIDs();//foreign key names
		if (createWhereArgs.length != req.size())
		{
			Debug.error("RelatedFoundset check for updated row, creation args and relation args are not the same!!"); //$NON-NLS-1$
			return false;//how can this happen??
		}

		IDataSet pks = getPksAndRecords().getPks();

		if (pks != null)
		{
			int[] operators = relation.getOperators();
			for (int i = 0; i < req.size(); i++)
			{
				String foreignKeyName = req.get(i);
				// createWhereArgs is a matrix as wide as the relation keys and 1 deep, compare unconverted values
				boolean remove = !checkForeignKeyValue(r.getRawValue(foreignKeyName), createWhereArgs[i][0], operators[i]);

				if (remove)
				{
					retval = false;
					//row is not longer part of this related foundset, so remove in myself
					for (int ii = pks.getRowCount() - 1; ii >= 0; ii--)
					{
						Object[] pk = pks.getRow(ii);
						if (RowManager.createPKHashKey(pk).equals(pkHash))
						{
							removeRecordInternal(ii);//does fireIntervalRemoved(this,ii,ii);
							break;
						}
					}
					break;
				}
			}
			if (retval && updateTest)
			{
				for (int ii = pks.getRowCount() - 1; ii >= 0; ii--)
				{
					Object[] pk = pks.getRow(ii);
					if (RowManager.createPKHashKey(pk).equals(pkHash))
					{
						fireAggregateChangeWithEvents(getRecord(ii));
						retval = false;
						break;
					}
				}
			}
		}
		return retval;
	}

	private boolean checkForeignKeyValue(Object obj, Object whereArg, int operator)
	{
		int maskedOperator = operator & IBaseSQLCondition.OPERATOR_MASK;
		if (Utils.equalObjects(whereArg, obj, true))
		{
			return (maskedOperator == IBaseSQLCondition.EQUALS_OPERATOR || maskedOperator == IBaseSQLCondition.GTE_OPERATOR ||
				maskedOperator == IBaseSQLCondition.LTE_OPERATOR || maskedOperator == IBaseSQLCondition.LIKE_OPERATOR);
		}

		if ((operator & IBaseSQLCondition.ORNULL_MODIFIER) != 0 && Utils.equalObjects(obj, null))
		{
			return true;
		}

		if (maskedOperator == IBaseSQLCondition.NOT_OPERATOR)
		{
			return true;
		}

		boolean equal = false;
		if (maskedOperator == IBaseSQLCondition.LIKE_OPERATOR)
		{
			if (obj == null) return false;
			// For LIKE we make case-insensitive comparison.
			String arg = whereArg.toString().toUpperCase();
			String objString = obj.toString().toUpperCase();
			StringTokenizer st = new StringTokenizer(arg, "%", true); //$NON-NLS-1$
			List<String> al = new ArrayList<String>();
			while (st.hasMoreTokens())
			{
				al.add(st.nextToken());
			}
			boolean startsWidth = true;
			int prevIndex = 0;
			for (int i = 0; i < al.size(); i++)
			{
				String tokenOrString = al.get(i);
				if ("%".equals(tokenOrString)) //$NON-NLS-1$
				{
					startsWidth = false;
					// if only % (left) then everything is equal
					equal = true;
				}
				else
				{
					if (startsWidth)
					{
						equal = objString.startsWith(tokenOrString);
					}
					else
					{
						if (al.size() > i + 1)
						{
							prevIndex = objString.indexOf(tokenOrString, prevIndex);
							equal = prevIndex != -1;
						}
						else
						{
							equal = objString.endsWith(tokenOrString);
						}
					}
					if (!equal)
					{
						break;
					}
				}
			}
		}
		else if (maskedOperator != IBaseSQLCondition.EQUALS_OPERATOR)
		{
			// Now test the GT(E) and LT(E) if possible (Comparable)
			if (obj instanceof Comparable && whereArg instanceof Comparable)
			{
				int compare = 0;
				if (whereArg instanceof String)
				{
					compare = ((String)whereArg).compareToIgnoreCase((String)obj);
				}
				else if (obj instanceof Number && whereArg instanceof Number && obj.getClass() != whereArg.getClass())
				{
					compare = (int)(((Number)whereArg).doubleValue() - ((Number)obj).doubleValue());
				}
				else
				{
					compare = ((Comparable)whereArg).compareTo(obj);
				}
				if (maskedOperator == IBaseSQLCondition.GT_OPERATOR || maskedOperator == IBaseSQLCondition.GTE_OPERATOR)
				{
					equal = compare >= 0;
				}
				else if (maskedOperator == IBaseSQLCondition.LT_OPERATOR || maskedOperator == IBaseSQLCondition.LTE_OPERATOR)
				{
					equal = compare <= 0;
				}
			}
		}
		return equal;
	}

	private void notifyChange_checkForNewRow(Row row)
	{
		// if already in state for new query then don't test anything.
		if (mustQueryForUpdates)
		{
			return;
		}

		// check if sql where is still the same, if there is search in it do nothing
		AndOrCondition createCondition = creationSqlSelect.getCondition(SQLGenerator.CONDITION_RELATION);
		AndOrCondition condition = getPksAndRecords().getQuerySelectForReading().getCondition(SQLGenerator.CONDITION_RELATION);
		if ((createCondition == null && condition == null) || createCondition != null && createCondition.equals(condition)) // does not include placeholder values in comparison
		{
			try
			{
				boolean doCheck = true;

				Relation relation = fsm.getApplication().getFlattenedSolution().getRelation(relationName);
				if (relation == null)
				{
					// this may happen when the relation was removed using solution model
					return;
				}
				//check the foreign key if they match, if so it will fall in this foundset
				Placeholder ph = creationSqlSelect.getPlaceholder(
					SQLGenerator.createRelationKeyPlaceholderKey(creationSqlSelect.getTable(), relation.getName()));
				if (ph == null || !ph.isSet())
				{
					Column[] cols = relation.getForeignColumns(fsm.getApplication().getFlattenedSolution());
					StringBuilder columns = new StringBuilder();
					columns.append("(");
					if (cols != null && cols.length > 0)
					{
						for (Column col : cols)
						{
							columns.append(col.getName());
							columns.append(",");
						}
						columns.setLength(columns.length() - 1);
					}
					columns.append(")");
					Debug.error("RelatedFoundset check for relation:" + relationName + " for a new row, creation args " + columns + "not found!!"); //$NON-NLS-1$
					return;//how can this happen??
				}
				// foreignData is a matrix as wide as the relation keys and 1 deep
				Object[][] foreignData = (Object[][])ph.getValue(); // Really get only the where params not all of them (like table filter)
				Column[] cols = relation.getForeignColumns(fsm.getApplication().getFlattenedSolution());
				if (foreignData.length != cols.length)
				{
					StringBuilder columns = new StringBuilder();
					columns.append("(");
					if (cols.length > 0)
					{
						for (Column col : cols)
						{
							columns.append(col.getName());
							columns.append(",");
						}
						columns.setLength(columns.length() - 1);
					}
					columns.append(")");

					StringBuilder data = new StringBuilder();
					data.append("(");
					if (foreignData.length > 0)
					{
						for (Object[] d : foreignData)
						{
							data.append("[");
							if (d.length > 0)
							{
								for (Object object : d)
								{
									data.append(object);
									data.append(",");
								}
								data.setLength(data.length() - 1);
							}
							data.append("]");
						}
						data.setLength(data.length() - 1);
					}
					data.append(")");

					Debug.error("RelatedFoundset check for relation:" + relationName + " for new row, creation args " + columns + " and relation args " + data + //$NON-NLS-1$
						"  are not the same!!");
					return;//how can this happen??
				}

				int[] operators = relation.getOperators();
				for (int i = 0; i < cols.length; i++)
				{
					Object obj = row.getRawValue(cols[i].getDataProviderID()); // compare unconverted values
					if (!checkForeignKeyValue(obj, foreignData[i][0], operators[i]))
					{
						doCheck = false;
						break;
					}
				}

				if (doCheck)
				{
					invalidateFoundset();
					getFoundSetManager().getEditRecordList().fireEvents();
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	protected void invalidateFoundset()
	{
		if (!mustQueryForUpdates)
		{
			int size = getCorrectedSizeForFires();
			IRecordInternal record = getRecord(getSelectedIndex());
			mustQueryForUpdates = true;
			Map<FoundSet, int[]> parentToIndexen = getFoundSetManager().getEditRecordList().getFoundsetEventMap();
//			if (size >= 0)
//			{
//				parentToIndexen.put(this, new int[] { 0, size });
//			}
//			else
			{
				// when foundset was empty (size = -1) this will be fired as an foundset-invalidated event, see EditRecordList.fireEvents()
				parentToIndexen.put(this, new int[] { -1, -1 });
			}
			fireAggregateChange(record);
		}
	}

	private void checkQueryForUpdates()
	{
		// Only do a new query if the foundset is in a mustQuery state
		// AND if it doesn't have edited records. These records should first be saved before
		// doing a query for pks. Else those records could be removed from this related foundset
		// and never return again (or the next mustQuery comes around)
		if (mustQueryForUpdates && !fsm.getEditRecordList().hasEditedRecords(this, false))
		{
			try
			{
				long time = System.currentTimeMillis();

				reloadWithCurrentQuery(Math.max(getSelectedIndex(), fsm.chunkSize) + fsm.chunkSize, false, true);

				if (Debug.tracing())
				{
					Debug.trace(Thread.currentThread().getName() + ": Related CheckForUpdate DB time: " + (System.currentTimeMillis() - time) + " pks: " + //$NON-NLS-1$//$NON-NLS-2$
						getPksAndRecords().getPks().getRowCount() + ", SQL: " + getPksAndRecords().getQuerySelectForReading().toString()); //$NON-NLS-1$
				}

				mustQueryForUpdates = false;
			}
			catch (ServoyException ex)
			{
				fsm.getApplication().reportError("Error quering for new pks in relatedfoundset", ex); //$NON-NLS-1$
				throw new RuntimeException("Error quering for new pks in relatedfoundset", ex); //$NON-NLS-1$
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				throw new RuntimeException("Error quering for new pks in relatedfoundset", ex); //$NON-NLS-1$
			}
		}
		else if (mustQueryForUpdates)
		{
			Debug.trace("checkQueryForUpdates: skipping because there were edited records"); //$NON-NLS-1$
		}
	}

	@Override
	@ServoyClientSupport(mc = false, wc = false, sc = false)
	public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value) throws ServoyException
	{
		return super.js_addFoundSetFilterParam(dataprovider, operator, value);
	}

	@Override
	@ServoyClientSupport(mc = false, wc = false, sc = false)
	public boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value, String name) throws ServoyException
	{
		return super.js_addFoundSetFilterParam(dataprovider, operator, value, name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_removeFoundSetFilterParam(java.lang.String)
	 */
	@Override
	@ServoyClientSupport(mc = false, wc = false, sc = false)
	public boolean js_removeFoundSetFilterParam(String name)
	{
		return super.js_removeFoundSetFilterParam(name);
	}

	@Override
	public String toString()
	{
		String str = super.toString();
		return "Related" + str.substring(0, str.length() - 1) + ", Args: " + getWhereArgsHash() + ']'; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
