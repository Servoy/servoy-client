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

import static com.servoy.j2db.dataprocessing.CustomValueList.processRow;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.util.Utils.iterate;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.event.ListDataListener;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList.DisplayString;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
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

	private ITable table;
	private QuerySelect creationSQLParts;

	private final IApplication application;

	List<Object> alReal = new ArrayList<Object>();
	List<Object> alDisplay = new ArrayList<Object>();

	private List<SortColumn> defaultSort;

	private TableFilter nameFilter;

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

		String datasource = null;
		try
		{
			if (vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
			{
				datasource = vl.getDataSource();
			}
			else
			{
				Relation[] relations = application.getFlattenedSolution().getRelationSequence(vl.getRelationName());
				if (relations != null)
				{
					Relation r = relations[relations.length - 1];
					datasource = r.getForeignDataSource();
				}
			}

			table = application.getFoundSetManager().getTable(datasource);
			if (table == null)
			{
				Debug.error("Could not find table of datasource " + datasource + " for lookup list " + lookup.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}

			creationSQLParts = DBValueList.createValuelistQuery(application, vl, table);

			if (vl.getUseTableFilter()) // apply name as filter on column valuelist_name
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

			table = s.getTable(tableName);
			if (table == null)
			{
				Debug.error("Could not find table " + tableName + " in server " + serverName + " for lookup list"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return;
			}

			creationSQLParts = new QuerySelect(table.queryTable());
			creationSQLParts.setDistinct(true);

			ArrayList<IQuerySelectValue> columns = new ArrayList<>();
			ArrayList<IQuerySort> orderColumns = new ArrayList<>();
			IQuerySelectValue cSQLName = DBValueList.getQueryColumn(table, creationSQLParts.getTable(), dataProviderID);
			columns.add(cSQLName);
			orderColumns.add(new QuerySort(cSQLName, true, application.getFoundSetManager().getSortOptions(table.getColumn(dataProviderID))));

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

	public void fill(IRecordInternal parentState, String dataProviderID, String filter, boolean firstTime) throws ServoyException
	{
		fill(parentState, dataProviderID, filter, null, firstTime, false);
	}

	/**
	 * @param firstTime
	 * @param lookup
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public void fill(IRecordInternal parentState, String dataProviderID, String filter, Object realValue, boolean firstTime, boolean alsoFilterOnRealValues)
		throws ServoyException
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
		if (lookup instanceof GlobalMethodValueList globalMethodValueList)
		{
			String fixedFilter = filter == null ? "" : filter;
			globalMethodValueList.fill(realState, fixedFilter, null);
			if ("".equals(fixedFilter) && globalMethodValueList.isEmpty())
			{
				globalMethodValueList.fill(realState, fixedFilter, alsoFilterOnRealValues ? fixedFilter : realValue);
			}
			for (int i = 0; i < globalMethodValueList.getSize(); i++)
			{
				Object display = globalMethodValueList.getElementAt(i);
				if (display == null) continue;
				alDisplay.add(display);
				alReal.add(globalMethodValueList.getRealElementAt(i));
			}
			hadMoreRows = true;
		}
		else if (lookup instanceof CustomValueList customValueList)
		{
			boolean procentStart = false;
			if (txt.startsWith("%"))
			{
				procentStart = true;
				txt = txt.substring(1, txt.length());
			}
			if (txt.endsWith("%")) txt = txt.substring(0, txt.length() - 1); //$NON-NLS-1$
			for (int i = 0; i < customValueList.getSize(); i++)
			{
				Object display = customValueList.getElementAt(i);
				if (display == null) continue;
				if (txt == "" || (procentStart && display.toString().toLowerCase().contains(txt)) ||
					(!procentStart && display.toString().toLowerCase().startsWith(txt)))
				{
					alDisplay.add(display);
					alReal.add(customValueList.getRealElementAt(i));
				}
				else if (alsoFilterOnRealValues)
				{
					Object real = customValueList.getRealElementAt(i);
					if (real != null && ((procentStart && real.toString().toLowerCase().contains(txt)) ||
						(!procentStart && real.toString().toLowerCase().startsWith(txt))))
					{
						alDisplay.add(display);
						alReal.add(real);
					}
				}
			}
		}
		else if (lookup instanceof LookupValueList lookupValueList)
		{
			if ("".equals(txt) && lookupValueList.getAllowEmptySelection()) //$NON-NLS-1$
			{
				alReal.add(null);
				alDisplay.add(""); //$NON-NLS-1$
			}
			if (lookupValueList.getValueList().getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
			{
				fillDBValueListValues(lookupValueList.getValueList(), txt, alsoFilterOnRealValues, lookupValueList.getDisplayFormat());
			}
			else
			{
				fillRelatedValueListValues(lookupValueList.getValueList(), realState, txt, alsoFilterOnRealValues, lookupValueList.getDisplayFormat());
			}
		}
		else
		{
			fillDBColumnValues(dataProviderID, txt);
		}
		fireChanges(prevSize);
	}

	private void fillRelatedValueListValues(ValueList valueList, IRecordInternal parentState, String filter, boolean alsoFilterOnRealValues,
		String[] displayFormat) throws ServoyException
	{
		if (parentState == null) return;

		Relation[] relations = application.getFlattenedSolution().getRelationSequence(valueList.getRelationName());
		Pair<QuerySelect, BaseQueryTable> pair = RelatedValueList.createRelatedValuelistQuery(application, valueList, relations, parentState);
		if (pair == null)
		{
			return;
		}
		QuerySelect select = pair.getLeft();
		BaseQueryTable qTable = pair.getRight();

		addSearchCondition(select, filter, valueList, qTable, alsoFilterOnRealValues);

		IFoundSetManagerInternal foundSetManager = application.getFoundSetManager();
		String transaction_id = foundSetManager.getTransactionID(table.getServerName());
		ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), select);
		if (nameFilter != null) // apply name as filter on column valuelist_name in creationSQLParts
		{
			if (tableFilterParams == null)
			{
				tableFilterParams = new ArrayList<>();
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
		IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, select, null,
			tableFilterParams, true, 0, 100, IDataServer.VALUELIST_QUERY, trackingInfo);
		for (int i = 0; i < set.getRowCount(); i++)
		{
			Object[] row = processRow(set.getRow(i), showValues, returnValues);
			DisplayString display = CustomValueList.handleDisplayData(valueList, displayFormat, concatShowValues, showValues, row, application);
			if (display != null)
			{
				alDisplay.add(display);
				alReal.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, row, application));
			}
		}
		hadMoreRows = set.hadMoreRows();
	}

	private void addSearchCondition(QuerySelect select, String txt, ValueList valueList, BaseQueryTable qTable, boolean alsoFilterOnRealValues)
	{
		if (txt == null || txt.equals(""))
		{
			return;
		}

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
					ArrayList<String> lst = new ArrayList<>();
					for (String displayValue : displayValues)
					{
						if (!displayValue.trim().equals("")) //$NON-NLS-1$
						{
							lst.add(displayValue.toUpperCase() + '%');
						}
					}
					displayValues = lst.toArray(new String[lst.size()]);
				}
			}
		}
		String likeValue = txt.toUpperCase() + '%';
		OrCondition overallOr = new OrCondition();
		if ((showValues & 1) != 0 || (alsoFilterOnRealValues && (returnValues & 1) != 0))
		{
			addOrCondition(valueList.getDataProviderID1(), qTable, likeValue, displayValues, overallOr);
		}
		if ((showValues & 2) != 0 || (alsoFilterOnRealValues && (returnValues & 2) != 0))
		{
			addOrCondition(valueList.getDataProviderID2(), qTable, likeValue, displayValues, overallOr);
		}
		if ((showValues & 4) != 0 || (alsoFilterOnRealValues && (returnValues & 4) != 0))
		{
			addOrCondition(valueList.getDataProviderID3(), qTable, likeValue, displayValues, overallOr);
		}

		select.addCondition(SQLGenerator.CONDITION_SEARCH, overallOr);
	}

	protected void addOrCondition(String dataProviderId, BaseQueryTable qTable, String likeValue, String[] displayValues, OrCondition overallOr)
	{
		IQuerySelectValue querySelect = DBValueList.getQueryColumn(table, qTable, dataProviderId);
		for (String displayValue : iterate(displayValues))
		{
			overallOr.addCondition(SQLGenerator.createLikeCompareCondition(querySelect, table.getColumnType(dataProviderId), displayValue));
		}
		// also just add the complete value, for the possibility that it was a value with a separator.
		overallOr.addCondition(SQLGenerator.createLikeCompareCondition(querySelect, table.getColumnType(dataProviderId), likeValue));
	}

	private void fillDBValueListValues(ValueList valueList, String filter, boolean alsoFilterOnRealValues, String[] displayFormat) throws ServoyException
	{
		if (creationSQLParts == null)
		{
			// not initialized properly
			return;
		}

		QuerySelect sqlParts = deepClone(creationSQLParts);
		addSearchCondition(sqlParts, filter, valueList, sqlParts.getTable(), alsoFilterOnRealValues);

		FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
		String transaction_id = foundSetManager.getTransactionID(table.getServerName());
		ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), sqlParts);
		if (nameFilter != null) //apply name as filter on column valuelist_name in creationSQLParts
		{
			if (tableFilterParams == null)
			{
				tableFilterParams = new ArrayList<>();
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
		IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, sqlParts, null,
			tableFilterParams, !sqlParts.isUnique(), 0, 100, IDataServer.VALUELIST_QUERY, trackingInfo);
		for (int i = 0; i < set.getRowCount(); i++)
		{
			Object[] row = processRow(set.getRow(i), showValues, returnValues);
			DisplayString display = CustomValueList.handleDisplayData(valueList, displayFormat, concatShowValues, showValues, row, application);
			if (display != null)
			{
				alDisplay.add(display);
				alReal.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, row, application));
			}
		}
		hadMoreRows = set.hadMoreRows();
	}

	/**
	 * @param txt
	 * @throws Exception
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	private void fillDBColumnValues(String dataProviderID, String txt) throws ServoyException
	{
		if (creationSQLParts == null)
		{
			// not initialized properly
			return;
		}
		QuerySelect sqlParts = deepClone(creationSQLParts);

		sqlParts.clearCondition(SQLGenerator.CONDITION_SEARCH);
		if (!"".equals(txt)) //$NON-NLS-1$
		{
			sqlParts.setCondition(SQLGenerator.CONDITION_SEARCH,
				new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR,
					new QueryFunction(QueryFunctionType.upper, DBValueList.getQueryColumn(table, sqlParts.getTable(), dataProviderID), dataProviderID),
					txt.toUpperCase() + '%'));
		}
		else
		{
			sqlParts.clearCondition(SQLGenerator.CONDITION_SEARCH);
		}

		IFoundSetManagerInternal foundSetManager = application.getFoundSetManager();
		String transaction_id = foundSetManager.getTransactionID(table.getServerName());
		ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), sqlParts);
		if (nameFilter != null) //apply name as filter on column valuelist_name in creationSQLParts
		{
			if (tableFilterParams == null)
			{
				tableFilterParams = new ArrayList<>();
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
		IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, sqlParts, null,
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

	public boolean removeListDataListenerIfNeeded(ListDataListener l)
	{
		int oldSize = getListDataListeners().length;
		removeListDataListener(l);
		int newSize = getListDataListeners().length;
		return oldSize != newSize;
	}
}