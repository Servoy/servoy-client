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


import java.util.ArrayList;
import java.util.List;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Table based valuelist
 *
 * @author jblok
 */
public class DBValueList extends CustomValueList implements ITableChangeListener
{
	protected final int maxValuelistRows;
	public static final String NAME_COLUMN = "valuelist_name"; //$NON-NLS-1$

	protected List<SortColumn> defaultSort = null;
	private Table table;
	protected boolean containsCalculation = false;
	protected boolean registered = false;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public DBValueList(IServiceProvider app, ValueList vl)
	{
		super(app, vl);
		int maxRowsSetting = (app instanceof IApplication) ? Utils.getAsInteger(((IApplication)app).getClientProperty(IApplication.VALUELIST_MAX_ROWS)) : 0;
		maxValuelistRows = (maxRowsSetting > 0 && maxRowsSetting <= 1000) ? maxRowsSetting : 500;

		if (vl.getAddEmptyValue() == IValueListConstants.EMPTY_VALUE_ALWAYS)
		{
			allowEmptySelection = true;
		}

		realValues = new SafeArrayList<Object>();
		if (vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
		{
			try
			{
				IServer s = application.getSolution().getServer(vl.getServerName());
				if (s != null)
				{
					table = (Table)s.getTable(vl.getTableName());
					setContainsCalculationFlag(table);

					//if changes are performed on the data refresh this list.
					if (!registered)
					{
						((FoundSetManager)application.getFoundSetManager()).addTableListener(table, this);
						registered = true;
					}

					defaultSort = application.getFoundSetManager().getSortColumns(table, vl.getSortOptions());
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			fill();
		}
	}

	@Override
	public void deregister()
	{
		//Can't deregister DB Valuelist in this method because this valuelist can be reused!
	}

/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */
	@Override
	public Object getRealElementAt(int row)//real value, getElementAt is display value
	{
		if (row < -1 && fallbackValueList != null)
		{
			return fallbackValueList.getRealElementAt((row * -1) - 2);
		}
		return realValues.get(row);
	}

	/*
	 * @see com.servoy.j2db.dataprocessing.CustomValueList#hasRealValues()
	 */
	@Override
	public boolean hasRealValues()
	{
		return valueList.getReturnDataProviders() != valueList.getShowDataProviders();
	}

	@Override
	public int realValueIndexOf(Object value)
	{
		int i = realValues.indexOf(value);
		if (i == -1 && fallbackValueList != null)
		{
			i = fallbackValueList.realValueIndexOf(value);
			if (i != -1)
			{
				i = (i + 2) * -1; // all values range from -2 > N
			}
		}
		return i;
	}

	private String[] getDisplayFormat()
	{
		if (table != null && hasRealValues())
		{
			String[] displayFormats = new String[3];
			Column col1 = table.getColumn(valueList.getDataProviderID1());
			if (col1 != null && col1.getColumnInfo() != null) displayFormats[0] = col1.getColumnInfo().getDefaultFormat();
			Column col2 = table.getColumn(valueList.getDataProviderID2());
			if (col2 != null && col2.getColumnInfo() != null) displayFormats[1] = col2.getColumnInfo().getDefaultFormat();
			Column col3 = table.getColumn(valueList.getDataProviderID3());
			if (col3 != null && col3.getColumnInfo() != null) displayFormats[2] = col3.getColumnInfo().getDefaultFormat();
			return displayFormats;
		}
		return null;
	}

	//update the list, contents may have changed, can this implemented more effective?
	public void tableChange(TableEvent e)
	{
		try
		{
			int size = getSize();
			stopBundlingEvents(); // to be on the safe side
			realValues = new SafeArrayList<Object>();
			removeAllElements();
			if (size > 0)
			{
				fireIntervalRemoved(this, 0, size - 1);
			}
			isLoaded = false;
			if (valueList.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
			{
				fill();
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);//due to buggy swing ui on macosx 131 this is needed, thows nullp when filled and not selected
		}
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */

	protected boolean isLoaded = false;

	@Override
	public void fill(IRecordInternal parentState)
	{
		super.fill(parentState);
		if (!isLoaded && !(parentState instanceof PrototypeState) && parentState != null &&
			valueList.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
		{
			fill();
			stopBundlingEvents(); // to be on the safe side
			int size = getSize();
			if (size > 0) fireContentsChanged(this, 0, size - 1);
		}
	}

	//also called by universal field valueChanged
	@SuppressWarnings("nls")
	private void fill()
	{
		try
		{
			if (table == null) return;

			FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
			List<SortColumn> sortColumns = foundSetManager.getSortColumns(table, valueList.getSortOptions());
			FoundSet fs = (FoundSet)foundSetManager.getNewFoundSet(table, null, sortColumns);
			if (fs == null)
			{
				return;
			}

			if (valueList.getUseTableFilter())//apply name as filter on column valuelist_name
			{
				fs.addFilterParam("valueList.nameColumn", NAME_COLUMN, "=", valueList.getName()); //$NON-NLS-1$
			}

			fs.browseAll(false);//we do nothing with related foundsets so don't touch these

			// browse all could trigger also a fill
			if (isLoaded) return;
			isLoaded = true;

			int showValues = valueList.getShowDataProviders();
			int returnValues = valueList.getReturnDataProviders();
			int total = (showValues | returnValues);

			//more than one value -> concat
			boolean concatShowValues = willConcat(showValues);
			boolean concatReturnValues = willConcat(returnValues);

			boolean singleColumn = (total & 7) == 1 || (total & 7) == 2 || (total & 7) == 4;

			try
			{
				startBundlingEvents();
				//add empty row
				if (valueList.getAddEmptyValue() == IValueListConstants.EMPTY_VALUE_ALWAYS)
				{
					addElement(""); //$NON-NLS-1$
					realValues.add(null);
				}

				QuerySelect creationSQLParts = null;
				if (singleColumn && fs.getSize() >= ((FoundSetManager)application.getFoundSetManager()).pkChunkSize && !containsCalculation)
				{
					creationSQLParts = createValuelistQuery(application, valueList, table);
				}
				if (creationSQLParts != null && creationSQLParts.isDistinct() &&
					fs.getSize() >= ((FoundSetManager)application.getFoundSetManager()).pkChunkSize && !containsCalculation)
				{
					ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), creationSQLParts);
					if (valueList.getUseTableFilter()) //apply name as filter on column valuelist_name in creationSQLParts
					{
						if (tableFilterParams == null)
						{
							tableFilterParams = new ArrayList<TableFilter>();
						}
						tableFilterParams.add(new TableFilter("dbValueList.nameFilter", table.getServerName(), table.getName(), table.getSQLName(), NAME_COLUMN, //$NON-NLS-1$
							IBaseSQLCondition.EQUALS_OPERATOR, valueList.getName()));
					}
					String transaction_id = foundSetManager.getTransactionID(table.getServerName());
					SQLStatement trackingInfo = null;
					if (foundSetManager.getEditRecordList().hasAccess(table, IRepository.TRACKING_VIEWS))
					{
						trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, table.getServerName(), table.getName(), null, null);
						trackingInfo.setTrackingData(creationSQLParts.getColumnNames(), new Object[][] { }, new Object[][] { }, application.getUserUID(),
							foundSetManager.getTrackingInfo(), application.getClientID());
					}
					IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, creationSQLParts,
						tableFilterParams, !creationSQLParts.isUnique(), 0, maxValuelistRows, IDataServer.VALUELIST_QUERY, trackingInfo);
					if (set.getRowCount() >= maxValuelistRows)
					{
						if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.client.report.max.valuelist.items", "true")))
						{
							if (application instanceof IApplication)
							{
								((IApplication)application).reportJSWarning(
									"Valuelist " + getName() + " fully loaded with " + maxValuelistRows + " rows, more rows are discarded!!");
							}
							else
							{
								application.reportJSError(
									"Valuelist " + getName() + " fully loaded with " + maxValuelistRows + " rows, more rows are discarded!!", null);
							}
						}
					}

					String[] displayFormat = getDisplayFormat();
					for (int i = 0; i < set.getRowCount(); i++)
					{
						Object[] row = CustomValueList.processRow(set.getRow(i), showValues, returnValues);
						Object displayValue = null;
						if (displayFormat != null)
						{
							displayValue = handleDisplayData(valueList, displayFormat, concatShowValues, showValues, row, application);
						}
						else
						{
							displayValue = handleRowData(valueList, concatShowValues, showValues, row, application);
						}
						addElement(displayValue != null ? displayValue.toString() : displayValue);
						realValues.add(handleRowData(valueList, concatReturnValues, returnValues, row, application));
					}
				}
				else
				{
					IRecordInternal[] array = fs.getRecords(0, maxValuelistRows);
					String[] displayFormat = getDisplayFormat();
					for (IRecordInternal r : array)
					{
						if (r != null)
						{
							Object val = handleRowData(valueList, displayFormat, concatShowValues, showValues, r, application);
							Object rval = handleRowData(valueList, null, concatReturnValues, returnValues, r, application);
							int index = indexOf(val);
							if (index == -1 || !Utils.equalObjects(getRealElementAt(index), rval))
							{
								addElement(val);
								realValues.add(rval);
							}
						}
					}
					if (fs.getSize() >= maxValuelistRows)
					{
						if (Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.client.report.max.valuelist.items", "true")))
						{
							if (application instanceof IApplication)
							{
								((IApplication)application).reportJSWarning(
									"Valuelist " + getName() + " fully loaded with " + maxValuelistRows + " rows, more rows are discarded!!");
							}
							else
							{
								application.reportJSError(
									"Valuelist " + getName() + " fully loaded with " + maxValuelistRows + " rows, more rows are discarded!!", null);
							}
						}
					}

				}
			}
			finally
			{
				stopBundlingEvents();
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	public static boolean willConcat(int selectedColumnValuesBitmask)
	{
		boolean concatShowValues = false;
		if ((selectedColumnValuesBitmask != 1) && (selectedColumnValuesBitmask != 2) && (selectedColumnValuesBitmask != 4))
		{
			concatShowValues = true;
		} // else it's a combination; it can't be 0
		return concatShowValues;
	}

	protected void setContainsCalculationFlag(ITable t)
	{
		if (valueList != null && application != null && application.getFlattenedSolution() != null)
		{
			containsCalculation = (checkIfCalc(valueList.getDataProviderID1(), t) || checkIfCalc(valueList.getDataProviderID2(), t) ||
				checkIfCalc(valueList.getDataProviderID3(), t));
		}
	}

	private boolean checkIfCalc(String dp, ITable t)
	{
		return dp != null && application.getFlattenedSolution().getScriptCalculation(dp, t) != null;
	}

	public static IQuerySelectValue getQuerySelectValue(ITable table, BaseQueryTable queryTable, String dataprovider)
	{
		if (dataprovider != null && table != null)
		{
			Column c = table.getColumn(dataprovider);
			if (c != null)
			{
				return new QueryColumn(queryTable, c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale(), c.getFlags());
			}
		}
		// should never happen
		throw new IllegalStateException("Cannot find column " + dataprovider + " in table " + table);
	}

	public ITable getTable()
	{
		return table;
	}

	public static QuerySelect createValuelistQuery(IServiceProvider application, ValueList valueList, Table table)
	{
		if (table == null) return null;

		FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
		// do not add the default pk-sort, only add real configured sort columns on value list
		List<SortColumn> sortColumns = valueList.getSortOptions() == null ? null : foundSetManager.getSortColumns(table, valueList.getSortOptions());

		int showValues = valueList.getShowDataProviders();
		int returnValues = valueList.getReturnDataProviders();
		int total = (showValues | returnValues);

		QuerySelect select = new QuerySelect(new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema()));

		ArrayList<IQuerySort> orderColumns = new ArrayList<IQuerySort>();
		ArrayList<IQuerySelectValue> columns = new ArrayList<IQuerySelectValue>();

		boolean useDefinedSort = sortColumns != null && sortColumns.size() > 0;
		if (useDefinedSort)
		{
			for (SortColumn sc : sortColumns)
			{
				orderColumns.add(
					new QuerySort(getQuerySelectValue(table, select.getTable(), sc.getDataProviderID()), sc.getSortOrder() == SortColumn.ASCENDING));
			}
		}

		if ((total & 1) != 0)
		{
			IQuerySelectValue cSQLName = getQuerySelectValue(table, select.getTable(), valueList.getDataProviderID1());
			columns.add(cSQLName);
			if ((showValues & 1) != 0 && !useDefinedSort)
			{
				orderColumns.add(new QuerySort(cSQLName, true));
			}
		}
		if ((total & 2) != 0)
		{
			IQuerySelectValue cSQLName = getQuerySelectValue(table, select.getTable(), valueList.getDataProviderID2());
			columns.add(cSQLName);
			if ((showValues & 2) != 0 && !useDefinedSort)
			{
				orderColumns.add(new QuerySort(cSQLName, true));
			}
		}
		if ((total & 4) != 0)
		{
			IQuerySelectValue cSQLName = getQuerySelectValue(table, select.getTable(), valueList.getDataProviderID3());
			columns.add(cSQLName);
			if ((showValues & 4) != 0 && !useDefinedSort)
			{
				orderColumns.add(new QuerySort(cSQLName, true));
			}
		}

		// check if we can still use distinct
		select.setDistinct(SQLGenerator.isDistinctAllowed(columns, orderColumns));
		select.setColumns(columns);
		select.setSorts(orderColumns);

		return select;
	}

	public static List<String> getShowDataproviders(ValueList valueList, Table callingTable, String dataProviderID, IFoundSetManagerInternal foundSetManager)
		throws RepositoryException
	{
		if (valueList == null)
		{
			return null;
		}

		FlattenedSolution flattenedSolution = foundSetManager.getApplication().getFlattenedSolution();

		// Find destination table in case dataProviderID is related
		String[] split = dataProviderID.split("\\.");
		String dataSource = callingTable.getDataSource();
		for (int i = 0; i < split.length - 1; i++) // first parts are relation names, last part is column name
		{
			Relation relation = flattenedSolution.getRelation(split[i]);
			if (relation == null || !relation.getPrimaryDataSource().equals(dataSource))
			{
				return null;
			}
			dataSource = relation.getForeignDataSource();
		}

		Table table = (Table)foundSetManager.getTable(dataSource);
		String columnName = split[split.length - 1];
		String prefix = dataProviderID.substring(0, dataProviderID.length() - columnName.length());

		// first try fallback value list,
		ValueList usedValueList = flattenedSolution.getValueList(valueList.getFallbackValueListID());
		Relation valuelistSortRelation = flattenedSolution.getValuelistSortRelation(usedValueList, table, columnName, foundSetManager);
		if (valuelistSortRelation == null)
		{
			// then try regular value list
			usedValueList = valueList;
			valuelistSortRelation = flattenedSolution.getValuelistSortRelation(usedValueList, table, columnName, foundSetManager);
		}

		if (valuelistSortRelation == null)
		{
			return null;
		}

		List<String> showDataproviders = new ArrayList<String>(3);
		int showValues = usedValueList.getShowDataProviders();
		if ((showValues & 1) != 0)
		{
			showDataproviders.add(prefix + valuelistSortRelation.getName() + '.' + usedValueList.getDataProviderID1());
		}
		if ((showValues & 2) != 0)
		{
			showDataproviders.add(prefix + valuelistSortRelation.getName() + '.' + usedValueList.getDataProviderID2());
		}
		if ((showValues & 4) != 0)
		{
			showDataproviders.add(prefix + valuelistSortRelation.getName() + '.' + usedValueList.getDataProviderID3());
		}
		return showDataproviders;
	}

	@Override
	public IDataProvider[] getDependedDataProviders()
	{
		return null;
	}
}
