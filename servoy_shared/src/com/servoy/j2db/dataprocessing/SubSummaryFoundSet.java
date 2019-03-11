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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * This class is used by printing to handle subsum results and convert those to states, which can be rendered to the screen or paper
 *
 * @author jblok
 */
public class SubSummaryFoundSet implements IFoundSetInternal
{
	private final IDataSet data;
	private final SQLSheet sheet;
	private final IFoundSetInternal relatedInfoLookup;
	protected RowManager rowManager;
	private final IFoundSetManagerInternal fsm;

	public SubSummaryFoundSet(IFoundSetManagerInternal fsm, IFoundSetInternal set, SortColumn[] groupByFields, List<AggregateVariable> aggregates,
		IDataSet data, Table table) throws ServoyException
	{
		this.fsm = fsm;
		relatedInfoLookup = set;

		this.data = data;

		rowManager = ((FoundSetManager)fsm).getRowManager(fsm.getDataSource(table));
//		rowManager.register(this); not needed in printing foundset

		sheet = ((FoundSetManager)fsm).getSQLGenerator().getNewTableSQLSheet(fsm.getDataSource(table));
		HashMap<String, Integer> columnIndexes = new HashMap<String, Integer>();
		for (int i = 0; i < groupByFields.length; i++)
		{
			SortColumn s = groupByFields[i];
			columnIndexes.put(s.getName(), new Integer(i));
		}
		for (int i = 0; i < aggregates.size(); i++)
		{
			AggregateVariable ag = aggregates.get(i);
			columnIndexes.put(ag.getName(), new Integer(i + groupByFields.length));
		}
		sheet.setDataProviderIDsColumnMap(columnIndexes);
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFireCollectable#completeFire(java.util.List)
	 */
	public void completeFire(Map<IRecord, List<String>> entries)
	{
	}

	public IFoundSetManagerInternal getFoundSetManager()
	{
		return fsm;
	}

	public String getRelationName()
	{
		return null;
	}

	public IFoundSetInternal copy(boolean unrelate) throws ServoyException
	{
		return this;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getSize()
	 */
	public int getSize()
	{
		return data.getRowCount();
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getState(int)
	 */
	public IRecordInternal getRecord(int row)
	{
		return new PrintState(this, new Row(rowManager, data.getRow(row), sheet.getAllUnstoredCalculationNamesWithNoValue(), false)
		{
			@Override
			protected void handleCalculationDependencies(Column column, String dataProviderID)
			{
				// do nothing - as this requires a pk hashkey and doesn't make sense for a subsummary record; it would also produce ArrayIndexOutOfBoundsException
				// because column data & sheet column indexes used in this row are not in sync
			}
		});
	}

	public IRecordInternal[] getRecords(int startrow, int count)
	{
		List<IRecordInternal> retval = new ArrayList<IRecordInternal>();
		for (int i = startrow; i < Math.min(startrow + count, getSize()); i++)
		{
			retval.add(getRecord(i));
		}
		return retval.toArray(new IRecordInternal[retval.size()]);
	}

	public IRecordInternal getRecord(Object[] pk)
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getParentState()
	 */
	public IRecordInternal getParentRecord()
	{
		return null;
	}

	public int getRecordIndex(IRecord record)
	{
		return -1;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getSheet()
	 */
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

	public void loadAllRecords() throws ServoyException
	{
		// ignore
	}

	@Deprecated
	public void browseAll() throws ServoyException
	{
		loadAllRecords();
	}

	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#deleteState(int)
	 */
	public void deleteRecord(int row) throws ServoyException
	{
		// ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#deleteRecord(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	public void deleteRecord(IRecordInternal record) throws ServoyException
	{
		// ignore
	}

	@Deprecated
	public void deleteAll() throws ServoyException
	{
		deleteAllRecords();
	}

	public void deleteAllRecords() throws ServoyException
	{
		// ignore
	}

	public void deleteAllInternal() throws ServoyException
	{
		// ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#newRecord()
	 */
	public int newRecord(boolean addOnTop) throws ServoyException
	{
		return 0;
	}

	public int newRecord(boolean addOnTop, boolean changeSelection) throws ServoyException
	{
		return 0;
	}

	public int newRecord(int indexToAdd, boolean changeSelection) throws ServoyException
	{
		return 0;
	}

	public int newRecord() throws ServoyException
	{
		return 0;
	}

	public void sort(List<SortColumn> sortColumns, boolean defer) throws ServoyException
	{
	}

	public void sort(List<SortColumn> sortColumns) throws ServoyException
	{
	}

	public void setSort(String sortString) throws ServoyException
	{
	}

	public String getSort()
	{
		return null;
	}

	public int duplicateRecord(int row, boolean addOnTop) throws ServoyException
	{
		return 0;
	}

	public int duplicateRecord(int recordIndex, int indexToAdd) throws ServoyException
	{
		return 0;
	}

	public boolean isValidRelation(String name)
	{
		return relatedInfoLookup.isValidRelation(name);
	}

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	public IFoundSetInternal getRelatedFoundSet(IRecordInternal state, String relationName, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		if (delegate != null && delegate.getSize() > 0)
		{
			return delegate.getRecord(0).getRelatedFoundSet(relationName, defaultSortColumns);
		}
		else
		{
			return null;//impossible to query because the relation RH keys does never exists
		}
	}

	public Object getCalculationValue(IRecordInternal state, String dataProviderID, Object[] vargs, UsedDataProviderTracker usedDataProviderTracker)
	{
//		return relatedFoundSetLookup.getCalculationValue(state, dataProviderID);
//		return null;//it is just impossible to get calcs values from^ a printState
		if (delegate != null)
		{
			return delegate.getCalculationValue(delegate.getRecord(0), dataProviderID, vargs, null /* do not manage calc dependencies */);
		}
		return null;
	}

	private IFoundSetInternal delegate;

	void setDelegate(IFoundSetInternal d)
	{
		this.delegate = d;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#containsDataProvider(String)
	 */
	public boolean containsDataProvider(String dataProviderID)
	{
		if (delegate != null)
		{
			return delegate.containsDataProvider(dataProviderID);
		}
		return false;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getDataProviderValue(String, boolean)
	 */
	public Object getDataProviderValue(String dataProviderID)
	{
		if (delegate != null)
		{
			Object retval = delegate.getDataProviderValue(dataProviderID);
			return retval;
		}
		return null;
	}

	public Object setDataProviderValue(String dataProviderID, Object value) //used by globals
	{
		//ignore
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#setStateToStringDataProviderID(String)
	 */
	public void setRecordToStringDataProviderID(String dataProviderID)
	{
		// ignore
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getStateToStringDataProviderID()
	 */
	public String getRecordToStringDataProviderID()
	{
		return null;
	}

	public int getSelectedIndex()
	{
		return -1;
	}

	/**
	 * Sets the selectedRow.
	 *
	 * @param selectedRow The selectedRow to set
	 */
	public void setSelectedIndex(int selectedRow)
	{
		// ignore
	}

	@Override
	public int getRecordIndex(String pkHash, int hintStart)
	{
		// ignore
		return -1;
	}

	public boolean isRecordEditable(int rowIndex)
	{
		return false;
	}

	public boolean isInFindMode()
	{
		return false;
	}

	public boolean find()
	{
		return false;
	}

	@Override
	public int search() throws Exception
	{
		return 0;
	}

	public static class PrintState extends Record
	{
		PrintState(IFoundSetInternal parent, Row columndata)
		{
			super(parent, columndata);
		}

		private IFoundSetInternal delegate;

		public void setDelegate(IFoundSetInternal d)
		{
			this.delegate = d;
		}

		/*
		 * _____________________________________________________________ Related states implementation,instead of normal state cache and prevent lookup for
		 * printing (==needed)
		 */
		private final Map<String, IFoundSetInternal> relatedStates = new HashMap<String, IFoundSetInternal>(3); //relationID -> subState

		/**
		 * Get related foundset, relationName may be multiple-levels deep
		 */
		@Override
		public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
		{
			if (relationName == null) return null;

			((SubSummaryFoundSet)parent).setDelegate(delegate);

			IFoundSetInternal sub = relatedStates.get(relationName);
			if (sub == null)
			{
				try
				{
					sub = parent.getRelatedFoundSet(this, relationName, defaultSortColumns);
					if (sub != null)
					{
						relatedStates.put(relationName, sub);
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
			return sub;
		}

		//prevent any value to be set causes index out of bounds (sets occurs if calc value not up-to-date)
		@Override
		public Object setValue(String dataProviderID, Object value)
		{
			return null;//ignore
		}

		//prevent any value to be set causes index out of bounds (sets occurs if calc value not up-to-date)
		@Override
		public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
		{
			return null;
		}

		private final Map<String, Object> dataproviderValueCache = new HashMap<String, Object>();//especially to cache aggregate results!

		@Override
		public Object getValue(String dataProviderID, boolean converted)
		{
			((SubSummaryFoundSet)parent).setDelegate(delegate);

			Object retval = dataproviderValueCache.get(dataProviderID);
			if (retval == null && !dataproviderValueCache.containsKey(dataProviderID))
			{
				int columnIndex = parent.getSQLSheet().getColumnIndex(dataProviderID);
				if (columnIndex != -1)
				{
					//subsumfoundset has different sqlsheet with different column indexes
					retval = getRawData().getRawValue(columnIndex, true); // does not use a converter
				}
				else
				{
					retval = super.getValue(dataProviderID, converted); // I think this could be directly replaced same as below - delegate.getRecord(0).getValue(dataProviderID); - except for hardcoded dataprovider strings as seen in Record.getValue() and then we can get rid of all the method overrides that want to avoid ArrayIndexOutOfBounds exceptions
				}
				if (retval == null) retval = delegate.getRecord(0).getValue(dataProviderID, converted);
				dataproviderValueCache.put(dataProviderID, retval);
			}
			return retval;
		}

		public void setDelegate(IRecordInternal rec)//to make calcs work
		{
			parent = rec.getParentFoundSet();
		}

		/**
		 *
		 */
		public void doAggregatesLookup()
		{
			((SubSummaryFoundSet)parent).setDelegate(delegate);

			String[] aggregates = parent.getSQLSheet().getAggregateNames();
			for (String element : aggregates)
			{
				getValue(element);//cache them all in this printstate
			}
		}

		@Override
		public String toString()
		{
			return "PrintState " + super.toString();
		}
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getTable()
	 */
	public ITable getTable()
	{
		return sheet.getTable();
	}

	public void addParent(IRecordInternal record)
	{
		// no aggregates support in printing...??
	}

	public void fireAggregateChangeWithEvents(IRecordInternal record)
	{
		// no aggregates support in printing...??
	}

	public List<SortColumn> getSortColumns()
	{
		return null;
	}

	public PrototypeState getPrototypeState()
	{
		return new PrototypeState(this);
	}

	public void clear()
	{
		// ignore
	}

	@Deprecated
	public void makeEmpty()
	{
		clear();
	}

	public void addFoundSetEventListener(IFoundSetEventListener l)
	{
		// ignore
	}

	public void removeFoundSetEventListener(IFoundSetEventListener l)
	{
		// ignore
	}

	public void addAggregateModificationListener(IModificationListener listener)
	{
		// ignore
	}

	public void removeAggregateModificationListener(IModificationListener listener)
	{
		// ignore
	}

	public String[] getDataProviderNames(int type)
	{
		// ignore
		return null;
	}

	public String getDataSource()
	{
		return DataSourceUtils.createDBTableDataSource(sheet.getServerName(), sheet.getTable().getName());
	}

	public boolean hadMoreRows()
	{
		return data.hadMoreRows();
	}

	public void sort(Comparator<Object[]> recordPKComparator)
	{
	}

	public boolean loadByQuery(IQueryBuilder query) throws ServoyException
	{
		return false;
	}

	public IQueryBuilder getQuery()
	{
		return null;
	}

	@Override
	public Object forEach(IRecordCallback callback)
	{
		return null;
	}

	@Override
	public Iterator<IRecord> iterator()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSet#setMultiSelect(boolean)
	 */
	@Override
	public void setMultiSelect(boolean multiSelect)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isMultiSelect()
	{
		return false;
	}

	@Override
	public void setSelectedIndexes(int[] indexes)
	{
		// ignore
	}

	@Override
	public int[] getSelectedIndexes()
	{
		return new int[0];
	}

	@Override
	public int getID()
	{
		return 0;
	}

	@Override
	public int getRawSize()
	{
		return getSize();
	}

	@Override
	public boolean isInitialized()
	{
		return true;
	}

	@Override
	public void fireFoundSetChanged()
	{
	}

	@Override
	public QuerySelect getCurrentStateQuery(boolean reduceSearch, boolean clone) throws ServoyException
	{
		return null;
	}

	@Override
	public boolean dispose()
	{
		return true;
	}
}
