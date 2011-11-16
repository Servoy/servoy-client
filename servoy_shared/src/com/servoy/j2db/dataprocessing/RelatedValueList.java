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


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.PlaceholderKey;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;

/**
 * Valuelist based on values from a relation (related foundset)
 * 
 * @author jblok
 */
public class RelatedValueList extends DBValueList implements IFoundSetEventListener
{
	private final boolean concatShowValues;
	private final boolean concatReturnValues;

	private IRecordInternal parentState;
	private final List<IFoundSetInternal> related = new ArrayList<IFoundSetInternal>();

	public RelatedValueList(IServiceProvider app, ValueList vl)
	{
		super(app, vl);

		int showValues = vl.getShowDataProviders();
		int returnValues = vl.getReturnDataProviders();

		//more than one value -> concat
		concatShowValues = (showValues & 7) != 1 && (showValues & 7) != 2 && (showValues & 7) != 4;
		concatReturnValues = (returnValues & 7) != 1 && (returnValues & 7) != 2 && (returnValues & 7) != 4;
		fill(null);
	}

	@Override
	public void deregister()
	{
		if (registered && valueList.getDatabaseValuesType() == ValueList.RELATED_VALUES)
		{
			try
			{
				Relation[] relations = application.getFlattenedSolution().getRelationSequence(valueList.getRelationName());
				if (relations != null)
				{
					for (Relation relation : relations)
					{
						if (!relation.isGlobal())
						{
							//if changes are performed on the data refresh this list.
							((FoundSetManager)application.getFoundSetManager()).removeTableListener(relation.getForeignTable(), this);
						}
					}
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
				valueList = null;
			}
		}
		registered = false;
	}

	@Override
	public String getRelationName()
	{
		return valueList.getRelationName();
	}

	@Override
	public void tableChange(TableEvent e)
	{
		refill();
	}

	public void foundSetChanged(FoundSetEvent e)
	{
		if (e.getType() == FoundSetEvent.CONTENTS_CHANGED) refill();
	}

	public void refill()
	{
		if (parentState != null && valueList != null)
		{
			for (IFoundSetInternal fs : related)
			{
				fs.removeFoundSetEventListener(this);
			}
			related.clear(); //to pass performance check in fill
			fill(parentState);
		}
	}

	@Override
	public void fill(IRecordInternal ps)//to create all the rows
	{
		this.parentState = ps;

		Relation[] relations = application.getFlattenedSolution().getRelationSequence(valueList.getRelationName());
		if (relations != null)
		{
			try
			{
				defaultSort = ((FoundSetManager)application.getFoundSetManager()).getSortColumns(relations[relations.length - 1].getForeignDataSource(),
					valueList.getSortOptions());

				// check if all relations go to the same server
				boolean sameServer = true;
				String serverName = relations[0].getForeignServerName();
				for (Relation relation : relations)
				{
					//if changes are performed on the data refresh this list.
					if (!relation.isGlobal())
					{
						if (!registered)
						{
							((FoundSetManager)application.getFoundSetManager()).addTableListener(relation.getForeignTable(), this);
						}
						if (sameServer)
						{
							sameServer = serverName.equals(relation.getForeignServerName());
						}
					}
				}
				registered = true;

				int size = super.getSize();

				stopBundlingEvents(); // to be on the safe side
				removeAllElements();
				for (IFoundSetInternal fs : related)
				{
					fs.removeFoundSetEventListener(this);
				}
				related.clear();
				realValues = new SafeArrayList<Object>();
				fireIntervalRemoved(this, 0, size);

				if (sameServer && relations.length > 1)
				{
					// multiple-level relation on same server, use query
					fillWithQuery(relations);
				}
				else
				{
					// 1-level relation or different servers, use foundsets
					fillFromFoundset(relations);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);//due to buggy swing ui on macosx 131 this is needed, throws nullp when filled and not selected
			}
		}

		super.fill(ps);
		stopBundlingEvents(); // to be on the safe side
		fireContentsChanged(this, -1, -1);
	}

	/**
	 * Fill from foundset in case of one-level relation or multiple servers
	 * 
	 * @throws ServoyException
	 */
	@SuppressWarnings("nls")
	public void fillFromFoundset(Relation[] relations) throws ServoyException
	{
		try
		{
			IFoundSetInternal relatedFoundSet;
			if (parentState == null)
			{
				relatedFoundSet = application.getFoundSetManager().getGlobalRelatedFoundSet(relations[0].getName());
			}
			else if (parentState instanceof FindState)
			{
				relatedFoundSet = parentState.getParentFoundSet().getRelatedFoundSet(parentState, relations[0].getName(), null);
			}
			else
			{
				relatedFoundSet = parentState.getRelatedFoundSet(relations[0].getName(), null);
			}

			for (IFoundSetInternal fs : related)
			{
				fs.removeFoundSetEventListener(this);
			}
			List<IFoundSetInternal> oldrelated = new ArrayList<IFoundSetInternal>(related);
			related.clear();

			if (relatedFoundSet != null)
			{
				int showValues = valueList.getShowDataProviders();
				int returnValues = valueList.getReturnDataProviders();
				boolean showAndReturnAreSame = showValues == returnValues;

				try
				{
					List<IRecordInternal> allRecords = new ArrayList<IRecordInternal>();
					getRelatedRecords(allRecords, related, relatedFoundSet, relations, 1);

					startBundlingEvents();
					//add empty row
					if (valueList.getAddEmptyValue() == ValueList.EMPTY_VALUE_ALWAYS)
					{
						addElement(""); //$NON-NLS-1$
						realValues.add(null);
					}

					if (related.equals(oldrelated))
					{
						return; //performance check
					}

					IRecordInternal[] records = allRecords.toArray(new IRecordInternal[allRecords.size()]);
					if (defaultSort != null) Arrays.sort(records, new RecordComparator(defaultSort));

					if (records.length >= MAX_VALUELIST_ROWS)
					{
						application.reportJSError("Valuelist " + getName() + " fully loaded with 500 rows, more rows are discarded!!", null);
					}

					for (IRecordInternal state : records)
					{
						if (state == null || state.getParentFoundSet() == null) continue;

						Object element = CustomValueList.handleRowData(valueList, concatShowValues, showValues, state, application);
						if (showAndReturnAreSame && indexOf(element) > -1) continue;

						addElement(element);
						realValues.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, state, application));
					}
				}
				finally
				{
					stopBundlingEvents();
				}

				isLoaded = true;
			}
		}
		finally
		{
			for (IFoundSetInternal fs : related)
			{
				fs.addFoundSetEventListener(this);
			}
		}
	}

	private static void getRelatedRecords(List<IRecordInternal> relatedRecords, List<IFoundSetInternal> visitedFoundsets, IFoundSetInternal foundSet,
		Relation[] relations, int depth)
	{
		if (foundSet != null)
		{
			visitedFoundsets.add(foundSet);
			IRecordInternal[] records = foundSet.getRecords(0, MAX_VALUELIST_ROWS);

			for (int i = 0; relatedRecords.size() < MAX_VALUELIST_ROWS && i < records.length; i++)
			{
				if (depth == relations.length)
				{
					if (!relatedRecords.contains(records[i]))
					{
						relatedRecords.add(records[i]);
					}
				}
				else
				{
					// go one level deeper
					getRelatedRecords(relatedRecords, visitedFoundsets, ((Record)records[i]).getRelatedFoundSet(relations[depth].getName()), relations,
						depth + 1);
				}
			}
		}

	}

	/**
	 * Fill using query in case of multi-level relation
	 * 
	 * @param relations
	 * @throws ServoyException
	 * @throws RemoteException
	 */
	@SuppressWarnings("nls")
	protected void fillWithQuery(Relation[] relations) throws RemoteException, ServoyException
	{
		FoundSetManager foundSetManager = (FoundSetManager)application.getFoundSetManager();

		Pair<QuerySelect, QueryTable> pair = createRelatedValuelistQuery(application, valueList, relations, parentState);
		if (pair == null)
		{
			return;
		}
		QuerySelect select = pair.getLeft();

		int showValues = valueList.getShowDataProviders();
		int returnValues = valueList.getReturnDataProviders();
		boolean showAndReturnAreSame = showValues == returnValues;

		// perform the query
		String serverName = relations[0].getForeignServerName();
		SQLStatement trackingInfo = null;
		if (foundSetManager.getEditRecordList().hasAccess(relations[relations.length - 1].getForeignTable(), IRepository.TRACKING_VIEWS))
		{
			trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, serverName, pair.getRight().getName(), null, null);
			trackingInfo.setTrackingData(select.getColumnNames(), new Object[][] { }, new Object[][] { }, application.getUserUID(),
				foundSetManager.getTrackingInfo(), application.getClientID());
		}
		IDataSet dataSet = application.getDataServer().performQuery(application.getClientID(), serverName, foundSetManager.getTransactionID(serverName),
			select, foundSetManager.getTableFilterParams(serverName, select), !select.isUnique(), 0, DBValueList.MAX_VALUELIST_ROWS,
			IDataServer.VALUELIST_QUERY, trackingInfo);
		try
		{
			startBundlingEvents();
			//add empty row
			if (valueList.getAddEmptyValue() == ValueList.EMPTY_VALUE_ALWAYS)
			{
				addElement(""); //$NON-NLS-1$
				realValues.add(null);
			}
			if (dataSet.getRowCount() >= MAX_VALUELIST_ROWS)
			{
				application.reportJSError("Valuelist " + getName() + " fully loaded with 500 rows, more rows are discarded!!", null);
			}
			for (int i = 0; i < dataSet.getRowCount(); i++)
			{
				Object[] row = CustomValueList.processRow(dataSet.getRow(i), showValues, returnValues);

				Object element = CustomValueList.handleRowData(valueList, concatShowValues, showValues, row, application);
				if (showAndReturnAreSame && indexOf(element) != -1) continue;

				addElement(element);
				realValues.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, row, application));
			}
		}
		finally
		{
			stopBundlingEvents();
		}

		isLoaded = true;
	}

	public static Pair<QuerySelect, QueryTable> createRelatedValuelistQuery(IServiceProvider application, ValueList valueList, Relation[] relations,
		IRecordInternal parentState) throws ServoyException
	{
		if (parentState == null)
		{
			return null;
		}

		FoundSetManager foundSetManager = (FoundSetManager)application.getFoundSetManager();
		SQLGenerator sqlGenerator = foundSetManager.getSQLGenerator();
		IGlobalValueEntry globalScopeProvider = foundSetManager.getGlobalScopeProvider();

		SQLSheet childSheet = sqlGenerator.getCachedTableSQLSheet(relations[0].getPrimaryDataSource());
		// this returns quickly if it already has a sheet for that relation, but optimize further?
		sqlGenerator.makeRelatedSQL(childSheet, relations[0]);

		QuerySelect select = AbstractBaseQuery.deepClone((QuerySelect)childSheet.getRelatedSQLDescription(relations[0].getName()).getSQLQuery());

		Object[] relationWhereArgs = foundSetManager.getRelationWhereArgs(parentState, relations[0], false);
		if (relationWhereArgs == null)
		{
			return null;
		}
		PlaceholderKey placeHolderKey = SQLGenerator.createRelationKeyPlaceholderKey(select.getTable(), relations[0].getName());
		if (!select.setPlaceholderValue(placeHolderKey, relationWhereArgs))
		{
			Debug.error(new RuntimeException("Could not set relation placeholder " + placeHolderKey + " in query " + select)); //$NON-NLS-1$//$NON-NLS-2$ 
			return null;
		}

		QueryTable lastTable = select.getTable();
		Table foreignTable = relations[0].getForeignTable();
		for (int i = 1; i < relations.length; i++)
		{
			foreignTable = relations[i].getForeignTable();
			QueryJoin join = SQLGenerator.createJoin(application.getFlattenedSolution(), relations[i], lastTable, new QueryTable(foreignTable.getSQLName(),
				foreignTable.getCatalog(), foreignTable.getSchema()), globalScopeProvider);
			select.addJoin(join);
			lastTable = join.getForeignTable();
		}

		List<SortColumn> defaultSort = foundSetManager.getSortColumns(relations[relations.length - 1].getForeignDataSource(), valueList.getSortOptions());
		SQLGenerator.addSorts(application, select, lastTable, globalScopeProvider, foreignTable, defaultSort, true);

		int showValues = valueList.getShowDataProviders();
		int returnValues = valueList.getReturnDataProviders();
		int total = (showValues | returnValues);

		ArrayList<IQuerySelectValue> columns = new ArrayList<IQuerySelectValue>();
		if ((total & 1) != 0)
		{
			columns.add(getQuerySelectValue(foreignTable, lastTable, valueList.getDataProviderID1()));
		}
		if ((total & 2) != 0)
		{
			columns.add(getQuerySelectValue(foreignTable, lastTable, valueList.getDataProviderID2()));
		}
		if ((total & 4) != 0)
		{
			columns.add(getQuerySelectValue(foreignTable, lastTable, valueList.getDataProviderID3()));
		}
		select.setColumns(columns);
		select.setDistinct(false); // not allowed in all situations

		return new Pair<QuerySelect, QueryTable>(select, lastTable);
	}

}
