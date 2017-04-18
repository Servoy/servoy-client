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
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList.DisplayString;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * {@link AbstractListModel} implementation for Lookup/Typeahead fields.
 *
 * @author jcompagner
 */
public class LookupListModel extends AbstractListModel
{
	private IValueList lookup;

	private boolean hadMoreRows;

	private int showValues;
	private int returnValues;
	private boolean concatShowValues;
	private boolean concatReturnValues;

	private Table table;
	private QuerySelect creationSQLParts;

	private final IApplication application;

	List<Object> alReal = new ArrayList<Object>();
	List<Object> alDisplay = new ArrayList<Object>();

	private List<SortColumn> defaultSort;

	private TableFilter nameFilter;

	private int secondColRedirectIndex = -1;
	private int thirdColRedirectIndex = -1;


	public LookupListModel(IApplication application, LookupValueList lookup)
	{
		this.application = application;
		this.lookup = lookup;
		ValueList vl = lookup.getValueList();

		showValues = vl.getShowDataProviders();
		returnValues = vl.getReturnDataProviders();
		int total = (showValues | returnValues);

		//more than one value -> concat
		concatShowValues = false;
		if ((showValues != 1) && (showValues != 2) && (showValues != 4))
		{
			concatShowValues = true;
		}
		concatReturnValues = false;
		if ((returnValues != 1) && (returnValues != 2) && (returnValues != 4))
		{
			concatReturnValues = true;
		}

		String serverName = null;
		String tableName = null;
		try
		{
			if (vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
			{
				serverName = vl.getServerName();
				tableName = vl.getTableName();
			}
			else
			{
				Relation[] relations = application.getFlattenedSolution().getRelationSequence(vl.getRelationName());
				if (relations != null)
				{
					Relation r = relations[relations.length - 1];
					serverName = r.getForeignServerName();
					tableName = r.getForeignTableName();
				}
			}

			IServer s = application.getSolution().getServer(serverName);
			if (s == null)
			{
				Debug.error("Could not find server " + serverName + " for lookup list " + lookup.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}

			table = (Table)s.getTable(tableName);
			if (table == null)
			{
				Debug.error("Could not find table " + tableName + " in server " + serverName + " for lookup list " + lookup.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return;
			}

			creationSQLParts = new QuerySelect(new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema()));
			creationSQLParts.setDistinct(true);

			ArrayList<IQuerySelectValue> columns = new ArrayList<IQuerySelectValue>();
			ArrayList<IQuerySort> orderColumns = new ArrayList<IQuerySort>();
			if ((total & 1) != 0)
			{
				if (table.getColumn(vl.getDataProviderID1()) == null)
				{
					String msg = "Lookup values with non-column data providers (like unstored calculations) are not supported, could not find column '" +
						vl.getDataProviderID1() + "' in table '" + table + "'";
					application.reportJSError(msg, null);
					throw new RuntimeException(msg);
				}
				IQuerySelectValue cSQLName = DBValueList.getQuerySelectValue(table, creationSQLParts.getTable(), vl.getDataProviderID1());
				columns.add(cSQLName);
				if ((showValues & 1) != 0)
				{
					orderColumns.add(new QuerySort(cSQLName, true));
				}
			}
			if ((total & 2) != 0)
			{
				if (table.getColumn(vl.getDataProviderID2()) == null)
				{
					String msg = "Lookup values with non-column data providers (like unstored calculations) are not supported, could not find column '" +
						vl.getDataProviderID2() + "' in table '" + table + "'";
					application.reportJSError(msg, null);
					throw new RuntimeException(msg);
				}
				IQuerySelectValue cSQLName = DBValueList.getQuerySelectValue(table, creationSQLParts.getTable(), vl.getDataProviderID2());
				if ((secondColRedirectIndex = columns.indexOf(cSQLName)) < 0)
				{
					columns.add(cSQLName);
				}
				if ((showValues & 2) != 0)
				{
					orderColumns.add(new QuerySort(cSQLName, true));
				}
			}
			if ((total & 4) != 0)
			{
				if (table.getColumn(vl.getDataProviderID3()) == null)
				{
					String msg = "Lookup values with non-column data providers (like unstored calculations) are not supported, could not find column '" +
						vl.getDataProviderID3() + "' in table '" + table + "'";
					application.reportJSError(msg, null);
					throw new RuntimeException(msg);
				}
				IQuerySelectValue cSQLName = DBValueList.getQuerySelectValue(table, creationSQLParts.getTable(), vl.getDataProviderID3());
				if ((thirdColRedirectIndex = columns.indexOf(cSQLName)) < 0)
				{
					columns.add(cSQLName);
				}
				if ((showValues & 4) != 0)
				{
					orderColumns.add(new QuerySort(cSQLName, true));
				}
			}

			creationSQLParts.setColumns(columns);
			creationSQLParts.setSorts(orderColumns);

			if (vl.getUseTableFilter())//apply name as filter on column valuelist_name
			{
				nameFilter = new TableFilter("lookupValuelist.nameFilter", table.getServerName(), table.getName(), table.getSQLName(), DBValueList.NAME_COLUMN, //$NON-NLS-1$
					IBaseSQLCondition.EQUALS_OPERATOR, vl.getName());
			}

			defaultSort = application.getFoundSetManager().getSortColumns(table, vl.getSortOptions());
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * @param application
	 * @param serverName
	 * @param tableName
	 * @param dataProviderID
	 */
	public LookupListModel(IApplication application, String serverName, String tableName, String dataProviderID)
	{
		this.application = application;
		try
		{
			IServer s = application.getSolution().getServer(serverName);
			if (s == null)
			{
				Debug.error("Could not find server " + serverName + " for lookup list on table " + tableName); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}

			table = (Table)s.getTable(tableName);
			if (table == null)
			{
				Debug.error("Could not find table " + tableName + " in server " + serverName + " for lookup list"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return;
			}

			creationSQLParts = new QuerySelect(new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema()));
			creationSQLParts.setDistinct(true);

			ArrayList<IQuerySelectValue> columns = new ArrayList<IQuerySelectValue>();
			ArrayList<IQuerySort> orderColumns = new ArrayList<IQuerySort>();
			IQuerySelectValue cSQLName = DBValueList.getQuerySelectValue(table, creationSQLParts.getTable(), dataProviderID);
			columns.add(cSQLName);
			orderColumns.add(new QuerySort(cSQLName, true));

			creationSQLParts.setColumns(columns);
			creationSQLParts.setSorts(orderColumns);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	/**
	 * @param application
	 * @param list
	 */
	public LookupListModel(IApplication application, CustomValueList list)
	{
		this.application = application;
		this.lookup = list;
	}

	/**
	 * @return the lookup
	 */
	public IValueList getValueList()
	{
		return lookup;
	}

	private ArrayList<IQuerySort> getSortColumnsForQuery(QuerySelect query)
	{
		ArrayList<IQuerySort> sortColumnsForQuery = null;

		if (defaultSort != null && defaultSort.size() > 0)
		{
			sortColumnsForQuery = new ArrayList<IQuerySort>();
			ArrayList<IQuerySelectValue> queryColumns = query.getColumns();
			for (SortColumn sortColumn : defaultSort)
			{
				for (IQuerySelectValue column : queryColumns)
				{
					if (sortColumn.getName().trim().equalsIgnoreCase(column.getColumn().getName().trim()))
					{
						sortColumnsForQuery.add(new QuerySort(column, sortColumn.getSortOrder() == SortColumn.ASCENDING));
						break;
					}
				}
			}
			if (sortColumnsForQuery.size() == 0) sortColumnsForQuery = null;
		}

		return sortColumnsForQuery;
	}

	public int getSize()
	{
		return alReal.size();
	}

	public void filter(String filter)
	{
		String txt = filter.toLowerCase();
		int prevSize = alReal.size();
		List<Object> tmp = alDisplay;
		if (tmp.size() == 0) tmp = alReal;
		for (int i = tmp.size(); --i >= 0;)
		{
			Object obj = tmp.get(i);
			if (obj instanceof DisplayString)
			{
				if (!((DisplayString)obj).startsWith(txt))
				{
					if (alDisplay.size() > 0) alDisplay.remove(i);
					alReal.remove(i);
				}
			}
			else if (!obj.toString().toLowerCase().startsWith(txt))
			{
				if (alDisplay.size() > 0) alDisplay.remove(i);
				alReal.remove(i);
			}
		}
		fireChanges(prevSize);
	}

	/**
	 * @param firstTime
	 * @param lookup
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public void fill(IRecordInternal parentState, String dataProviderID, String filter, boolean firstTime) throws ServoyException
	{
		int prevSize = alReal.size();

		IRecordInternal realState = parentState;

		alReal.clear();
		alDisplay.clear();

		String txt = (filter == null || firstTime) ? "" : filter.toLowerCase(); //$NON-NLS-1$

		if (dataProviderID != null && !ScopesUtils.isVariableScope(dataProviderID))
		{
			int index = dataProviderID.lastIndexOf('.');
			if (index != -1 && realState != null)
			{
				IFoundSetInternal relatedFoundSet = realState.getRelatedFoundSet(dataProviderID.substring(0, index));
				if (relatedFoundSet != null && relatedFoundSet.getSize() != 0)
				{
					realState = relatedFoundSet.getRecord(relatedFoundSet.getSelectedIndex());
				}
			}
		}
		if (lookup instanceof GlobalMethodValueList)
		{
			GlobalMethodValueList clist = (GlobalMethodValueList)lookup;
			clist.fill(realState, filter == null ? "" : filter, null);
			for (int i = 0; i < clist.getSize(); i++)
			{
				Object display = clist.getElementAt(i);
				if (display == null) continue;
				alDisplay.add(display);
				alReal.add(clist.getRealElementAt(i));
			}
			hadMoreRows = true;
		}
		else if (lookup instanceof CustomValueList)
		{
			CustomValueList clist = (CustomValueList)lookup;
			for (int i = 0; i < clist.getSize(); i++)
			{
				Object display = clist.getElementAt(i);
				if (display == null) continue;
				if (txt == "" || display.toString().toLowerCase().startsWith(txt)) //$NON-NLS-1$
				{
					alDisplay.add(display);
					alReal.add(clist.getRealElementAt(i));
				}
			}
		}
		else if (lookup instanceof LookupValueList)
		{
			if ("".equals(txt) && lookup.getAllowEmptySelection()) //$NON-NLS-1$
			{
				alReal.add(null);
				alDisplay.add(""); //$NON-NLS-1$
			}
			if (((LookupValueList)lookup).getValueList().getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
			{
				fillDBValueListValues(txt);
			}
			else
			{
				fillRelatedValueListValues(realState, txt);
			}
		}
		else
		{
			fillDBColumnValues(dataProviderID, txt);
		}
		fireChanges(prevSize);
	}

	/**
	 * @param txt
	 * @throws RemoteException
	 * @throws Exception
	 */
	private void fillRelatedValueListValues(IRecordInternal parentState, String filter) throws ServoyException
	{
		if (parentState == null) return;

		String txt = filter;
		ValueList valueList = ((LookupValueList)lookup).getValueList();

		Relation[] relations = application.getFlattenedSolution().getRelationSequence(valueList.getRelationName());
		Pair<QuerySelect, BaseQueryTable> pair = RelatedValueList.createRelatedValuelistQuery(application, valueList, relations, parentState);
		if (pair == null)
		{
			return;
		}
		QuerySelect select = pair.getLeft();
		BaseQueryTable qTable = pair.getRight();

		generateWherePart(txt, valueList, select, qTable);

		try
		{
			FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
			String transaction_id = foundSetManager.getTransactionID(table.getServerName());
			ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), select);
			if (nameFilter != null) //apply name as filter on column valuelist_name in creationSQLParts
			{
				if (tableFilterParams == null)
				{
					tableFilterParams = new ArrayList<TableFilter>();
				}
				tableFilterParams.add(nameFilter);
			}

			SQLStatement trackingInfo = null;
			if (foundSetManager.getEditRecordList().hasAccess(table, IRepository.TRACKING_VIEWS))
			{
				trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, table.getServerName(), qTable.getName(), null, null);
				trackingInfo.setTrackingData(select.getColumnNames(), new Object[][] { }, new Object[][] { }, application.getUserUID(),
					foundSetManager.getTrackingInfo(), application.getClientID());
			}
			IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, select, tableFilterParams,
				true, 0, 100, IDataServer.VALUELIST_QUERY, trackingInfo);
			String[] displayFormat = (lookup instanceof LookupValueList) ? ((LookupValueList)lookup).getDisplayFormat() : null;
			for (int i = 0; i < set.getRowCount(); i++)
			{
				Object[] row = processRow(set.getRow(i));
				DisplayString display = CustomValueList.handleDisplayData(valueList, displayFormat, concatShowValues, showValues, row, application);
				if (display != null)
				{
					alDisplay.add(display);
					alReal.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, row, application));
				}
			}
			hadMoreRows = set.hadMoreRows();
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	/**
	 *  In cases where the user selected the same column in 2 or 3 'Return in dataprovider' or 'Show in field' for a valuelist
	 *  the query  only does a select with one column name .After the data is received  it reconstructs the rows with the missing duplicate columns
	 * */
	public Object[] processRow(Object[] row)
	{
		Object[] ret = row;
		if (secondColRedirectIndex >= 0 || thirdColRedirectIndex >= 0)
		{
			boolean hasFirstDp = (showValues & 1) != 0 || (returnValues & 1) != 0;
			boolean hasSecondDp = (showValues & 2) != 0 || (returnValues & 2) != 0;
			boolean hasThirdDp = (showValues & 4) != 0 || (returnValues & 4) != 0;

			ArrayList<Object> arr = new ArrayList<Object>();
			int srcIdx = 0;

			if (hasFirstDp) arr.add(row[srcIdx++]);

			if (secondColRedirectIndex >= 0)
			{
				arr.add(row[secondColRedirectIndex]);
			}
			else if (hasSecondDp)
			{
				arr.add(row[srcIdx++]);
			}

			if (thirdColRedirectIndex >= 0)
			{
				arr.add(row[thirdColRedirectIndex]);
			}
			else if (hasThirdDp)
			{
				arr.add(row[srcIdx++]);
			}

			ret = arr.toArray();
		}
		return CustomValueList.processRow(ret, showValues, returnValues);
	}

	/**
	 * @param txt
	 * @param valueList
	 * @param select
	 * @param qTable
	 */
	private boolean generateWherePart(String txt, ValueList valueList, QuerySelect select, BaseQueryTable qTable)
	{
		if (txt != null && !txt.equals("")) //$NON-NLS-1$
		{
			String[] displayValues = null;
			String separator = valueList.getSeparator();
			if (separator != null && !separator.equals("")) //$NON-NLS-1$
			{
				if (showValues != 1 && showValues != 2 && showValues != 4)
				{
					// its a combination
					displayValues = Utils.stringSplit(txt, separator);
					if (displayValues.length == 1 && displayValues[0].equals(txt.toUpperCase()))
					{
						displayValues = null;
					}
					else
					{
						ArrayList<String> lst = new ArrayList<String>();
						for (int i = 0; i < displayValues.length; i++)
						{
							if (!displayValues[i].trim().equals("")) //$NON-NLS-1$
							{
								lst.add(displayValues[i].toUpperCase() + '%');
							}
						}
						displayValues = lst.toArray(new String[lst.size()]);
					}
				}
			}
			String likeValue = txt.toUpperCase() + '%';
			OrCondition overallOr = new OrCondition();
			if ((showValues & 1) != 0)
			{
				addOrCondition(valueList.getDataProviderID1(), qTable, likeValue, displayValues, overallOr);
			}
			if ((showValues & 2) != 0)
			{
				addOrCondition(valueList.getDataProviderID2(), qTable, likeValue, displayValues, overallOr);
			}
			if ((showValues & 4) != 0)
			{
				addOrCondition(valueList.getDataProviderID3(), qTable, likeValue, displayValues, overallOr);
			}
			select.addCondition(SQLGenerator.CONDITION_SEARCH, overallOr);
			return true;
		}
		return false;
	}

	protected void addOrCondition(String dataProviderId, BaseQueryTable qTable, String likeValue, String[] displayValues, OrCondition overallOr)
	{
		IQuerySelectValue querySelect = DBValueList.getQuerySelectValue(table, qTable, dataProviderId);
		if (displayValues != null)
		{
			for (String displayValue : displayValues)
			{
				overallOr.addCondition(SQLGenerator.createLikeCompareCondition(querySelect, table.getColumnType(dataProviderId), displayValue));
			}
		}
		// also just add the complete value, for the possibility that it was a value with a separator.
		overallOr.addCondition(SQLGenerator.createLikeCompareCondition(querySelect, table.getColumnType(dataProviderId), likeValue));
	}

	/**
	 * @param txt
	 * @throws Exception
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	private void fillDBValueListValues(String filter) throws ServoyException
	{
		ValueList valueList = ((LookupValueList)lookup).getValueList();
		QuerySelect sqlParts = AbstractBaseQuery.deepClone(creationSQLParts);
		if (!generateWherePart(filter, valueList, sqlParts, sqlParts.getTable()))
		{
			ArrayList<IQuerySort> sorts = getSortColumnsForQuery(sqlParts);
			if (sorts != null) sqlParts.setSorts(sorts);
		}

		try
		{
			FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
			String transaction_id = foundSetManager.getTransactionID(table.getServerName());
			ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), sqlParts);
			if (nameFilter != null) //apply name as filter on column valuelist_name in creationSQLParts
			{
				if (tableFilterParams == null)
				{
					tableFilterParams = new ArrayList<TableFilter>();
				}
				tableFilterParams.add(nameFilter);
			}
			SQLStatement trackingInfo = null;
			if (foundSetManager.getEditRecordList().hasAccess(table, IRepository.TRACKING_VIEWS))
			{
				trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, table.getServerName(), table.getName(), null, null);
				trackingInfo.setTrackingData(sqlParts.getColumnNames(), new Object[][] { }, new Object[][] { }, application.getUserUID(),
					foundSetManager.getTrackingInfo(), application.getClientID());
			}
			IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, sqlParts,
				tableFilterParams, !sqlParts.isUnique(), 0, 100, IDataServer.VALUELIST_QUERY, trackingInfo);
			String[] displayFormat = (lookup instanceof LookupValueList) ? ((LookupValueList)lookup).getDisplayFormat() : null;
			for (int i = 0; i < set.getRowCount(); i++)
			{
				Object[] row = processRow(set.getRow(i));
				DisplayString display = CustomValueList.handleDisplayData(valueList, displayFormat, concatShowValues, showValues, row, application);
				if (display != null)
				{
					alDisplay.add(display);
					alReal.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, row, application));
				}
			}
			hadMoreRows = set.hadMoreRows();
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	/**
	 * @param txt
	 * @throws Exception
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	private void fillDBColumnValues(String dataProviderID, String txt) throws ServoyException
	{
		QuerySelect sqlParts = AbstractBaseQuery.deepClone(creationSQLParts);

		sqlParts.clearCondition(SQLGenerator.CONDITION_SEARCH);
		if (!"".equals(txt)) //$NON-NLS-1$
		{
			sqlParts.setCondition(SQLGenerator.CONDITION_SEARCH,
				new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR,
					new QueryFunction(QueryFunctionType.upper, DBValueList.getQuerySelectValue(table, sqlParts.getTable(), dataProviderID), dataProviderID),
					txt.toUpperCase() + '%'));
		}
		else
		{
			sqlParts.clearCondition(SQLGenerator.CONDITION_SEARCH);
		}

		try
		{
			FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
			String transaction_id = foundSetManager.getTransactionID(table.getServerName());
			ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), sqlParts);
			if (nameFilter != null) //apply name as filter on column valuelist_name in creationSQLParts
			{
				if (tableFilterParams == null)
				{
					tableFilterParams = new ArrayList<TableFilter>();
				}
				tableFilterParams.add(nameFilter);
			}

			SQLStatement trackingInfo = null;
			if (foundSetManager.getEditRecordList().hasAccess(table, IRepository.TRACKING_VIEWS))
			{
				trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, table.getServerName(), table.getName(), null, null);
				trackingInfo.setTrackingData(sqlParts.getColumnNames(), new Object[][] { }, new Object[][] { }, application.getUserUID(),
					foundSetManager.getTrackingInfo(), application.getClientID());
			}
			IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, sqlParts,
				tableFilterParams, !sqlParts.isUnique(), 0, 100, IDataServer.VALUELIST_QUERY, trackingInfo);
			for (int i = 0; i < set.getRowCount(); i++)
			{
				Object[] row = set.getRow(i);
				if (row[0] != null && !"".equals(row[0])) //$NON-NLS-1$
				{
					alReal.add(row[0]);
				}
			}
			hadMoreRows = set.hadMoreRows();
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	/**
	 * @param prevSize
	 */
	private void fireChanges(final int prevSize)
	{

		if (application.isEventDispatchThread())
		{
			fireChangesImpl(prevSize);
		}
		else
		{
			Runnable run = new Runnable()
			{
				public void run()
				{
					fireChangesImpl(prevSize);
				}
			};
			application.invokeAndWait(run);
		}
	}


	/**
	 * @param prevSize
	 */
	private void fireChangesImpl(final int prevSize)
	{
		if (prevSize == 0)
		{
			if (alReal.size() > 0)
			{
				fireIntervalAdded(this, 0, alReal.size() - 1);
			}
		}
		else if (prevSize == alReal.size())
		{
			fireContentsChanged(this, 0, prevSize - 1);
		}
		else
		{
			if (prevSize < alReal.size())
			{
				fireIntervalAdded(this, prevSize, (alReal.size() - prevSize - 1));
				fireContentsChanged(this, 0, prevSize - 1);
			}
			else
			{
				fireIntervalRemoved(this, alReal.size(), prevSize - 1);
				fireContentsChanged(this, 0, alReal.size() - 1);
			}
		}
	}

	/*
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	public Object getElementAt(int index)
	{
		if (alDisplay.size() > 0) return alDisplay.get(index);
		else return alReal.get(index);
	}

	public Object getRealElementAt(int index)
	{
		return alReal.get(index);
	}

	public int realValueIndexOf(Object element)
	{
		return alReal.indexOf(element);
	}

	public Iterator<Object> iterator()
	{
		if (alDisplay.size() > 0)
		{
			return alDisplay.iterator();
		}
		return alReal.iterator();
	}

	/**
	 * @return
	 */
	public int isShowValues()
	{
		return showValues;
	}

	/**
	 * @return
	 */
	public int isReturnValues()
	{
		return returnValues;
	}

	/**
	 * @return
	 */
	public List<SortColumn> getDefaultSort()
	{
		return defaultSort;
	}

	/**
	 * @return
	 */
	public boolean hasMoreRows()
	{
		return hadMoreRows;
	}

}