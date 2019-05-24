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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.SQLSheet.VariableInfo;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;


/**
 * Represents one row (containing all columns) from a table
 *
 * @author jblok
 */
public class Row
{
	public enum ROLLBACK_MODE
	{
		OVERWRITE_CHANGES, UPDATE_CHANGES, KEEP_CHANGES

	}

	public static final Object UNINITIALIZED = new Object();

	private Exception lastException;
	private final RowManager parent;
	private volatile Object[] columndata;//actual columndata
	private volatile Object[] oldValues;

	private final Map<String, Object> unstoredCalcCache; // dataProviderID -> Value
	private boolean existInDB;
	private String pkHashKey;
	private final WeakHashMap<IRowChangeListener, Object> listeners;

	private static Object dummy = new Object();

	private final ConcurrentMap<String, Thread> calculatingThreads = new ConcurrentHashMap<String, Thread>(4);

	void register(IRowChangeListener r)
	{
		synchronized (listeners)
		{
			if (!listeners.containsKey(r)) listeners.put(r, dummy);
		}
	}

	public boolean hasListeners()
	{
		synchronized (listeners)
		{
			return listeners.size() != 0;
		}
	}

	void fireNotifyChange(String name, Object value, FireCollector collector)
	{
		ModificationEvent e = new ModificationEvent(name, value, this);
		Object[] array;
		synchronized (listeners)
		{
			array = listeners.keySet().toArray();
		}

		for (Object element2 : array)
		{
			IRowChangeListener element = (IRowChangeListener)element2;
			element.notifyChange(e, collector);
		}
	}

	Row(RowManager parent, Object[] columndata, Map<String, Object> cc, boolean existInDB)
	{
		this.parent = parent;
		this.columndata = columndata;
		this.existInDB = existInDB;
		unstoredCalcCache = cc;
		listeners = new WeakHashMap<IRowChangeListener, Object>();

		// walk over the column data's to see if there is a dbident
		// if it doesnt have a 'belong to' row yet that this is a
		// db ident for its pk. If it is set then that dbident comes
		// from another parent related record.
		for (Object element : columndata)
		{
			if (element instanceof DbIdentValue)
			{
				DbIdentValue value = (DbIdentValue)element;
				if (value.getRow() == null)
				{
					value.setRow(this);
				}
			}
		}
	}

	//if something is defined as calc it will return the calc value
	public Object getValue(String id)
	{
		Object obj;
		int columnIndex = parent.getSQLSheet().getColumnIndex(id);
		if (columnIndex != -1)
		{
			obj = getValue(columnIndex);
		}
		else
		{
			obj = unstoredCalcCache.get(id);
		}
		if (obj == UNINITIALIZED)
		{
			obj = null;
		}
		return obj;
	}

	/*
	 * Get value unconverted
	 */
	public Object getRawValue(String id)
	{
		Object obj;
		int columnIndex = parent.getSQLSheet().getColumnIndex(id);
		if (columnIndex != -1)
		{
			obj = getRawValue(columnIndex, false);
		}
		else
		{
			obj = unstoredCalcCache.get(id);
		}
		if (obj == UNINITIALIZED)
		{
			obj = null;
		}
		return obj;
	}

	public boolean existInDB()
	{
		return existInDB;
	}

	public boolean containsCalculation(String id)
	{
		return parent.getSQLSheet().containsCalculation(id);
	}

	/**
	 * Get row value, converted using column converter
	 * @param columnIndex
	 * @return
	 */
	Object getValue(int columnIndex)
	{
		// call this with false, else things like using a dbident in javascript or creating related records are going wrong.
		Object value = getRawValue(columnIndex, false);
		return parent.getSQLSheet().convertValueToObject(value, columnIndex, parent.getFoundsetManager().getColumnConverterManager());
	}

	/**
	 * Get row value, do not use column converter.
	 * @param columnIndex
	 * @param unwrapDbIdent
	 * @return
	 */
	Object getRawValue(int columnIndex, boolean unwrapDbIdent)
	{
		if (columnIndex < 0 || columnIndex >= columndata.length) return null;

		Object obj = columndata[columnIndex];
		if (obj instanceof ValueFactory.BlobMarkerValue)
		{
			obj = ((ValueFactory.BlobMarkerValue)obj).getCachedData();//see if this is soft cached
			if (obj == null)
			{
				try
				{
					Blob b = parent.getBlob(this, columnIndex);
					if (b != null && b.getBlobData() != null)
					{
						obj = b.getBlobData();
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}

				if (obj != null && ((byte[])obj).length > 50000)
				{
					columndata[columnIndex] = ValueFactory.createBlobMarkerValue((byte[])obj);
					if (oldValues != null)
					{
						oldValues[columnIndex] = ValueFactory.createBlobMarkerValue((byte[])obj);
					}
				}
				else
				{
					columndata[columnIndex] = obj;
					if (oldValues != null)
					{
						oldValues[columnIndex] = obj;
					}
				}
			}
		}
		else if (obj instanceof DbIdentValue)
		{
			if (((DbIdentValue)obj).getPkValue() != null)
			{
				// If the pk value is there just replace it
				obj = ((DbIdentValue)obj).getPkValue();
				columndata[columnIndex] = obj;
			}
			else if (unwrapDbIdent)
			{
				obj = null;
			}
		}

		return obj;
	}

	void setDbIdentValue(Object value)
	{
		int identindex = parent.getSQLSheet().getIdentIndex();
		if (identindex >= 0)
		{
			Object o = columndata[identindex];
			// it should always be a db ident value??
			if (o instanceof DbIdentValue)
			{
				((DbIdentValue)o).setPkValue(value);
			}
			columndata[identindex] = value;
			FireCollector collector = FireCollector.getFireCollector();
			try
			{
				String dataProviderID = parent.getSQLSheet().getColumnNames()[identindex];
				fireNotifyChange(dataProviderID, value, collector);
			}
			catch (Exception e)
			{
				Debug.error("error notifying the system of a db Ident value change of " + value + " of row: " + this, e);
			}
			finally
			{
				collector.done();
			}
		}
	}

	Object getDbIdentValue()
	{
		int identindex = parent.getSQLSheet().getIdentIndex();
		if (identindex >= 0)
		{
			Object o = columndata[identindex];
			if (o == null)
			{
				o = ValueFactory.createDbIdentValue().setRow(this);
				columndata[identindex] = o;
			}
			// just return the value, it may already have its value replaced, we have support for setting db ident values in query generator
//			if (o instanceof DbIdentValue)
//			{
			return o;
//			}
		}
		Debug.error("No DbIdent for this row: " + this); //$NON-NLS-1$
		return null;
	}


	/**
	 * @param dataProviderID
	 * @return
	 */
	boolean containsDataprovider(String dataProviderID)
	{
		return (containsCalculation(dataProviderID) || parent.getSQLSheet().getColumnIndex(dataProviderID) != -1);
	}

	//returns the oldvalue, or value if no change
	public Object setValue(IRowChangeListener src, String dataProviderID, Object value)
	{
		Object o = getRawValue(dataProviderID);
		if (o instanceof DbIdentValue) return o; // this column is controlled by the database - so do not allow sets until the database chose a value
		Object convertedValue = value;

		SQLSheet sheet = parent.getSQLSheet();
		int columnIndex = sheet.getColumnIndex(dataProviderID);
		VariableInfo variableInfo = sheet.getCalculationOrColumnVariableInfo(dataProviderID, columnIndex);

		if (convertedValue != null && !("".equals(convertedValue) && Column.mapToDefaultType(variableInfo.type) == IColumnTypes.TEXT))//do not convert null to 0 incase of numbers, this means the calcs the value whould change each time //$NON-NLS-1$
		{
			convertedValue = sheet.convertObjectToValue(dataProviderID, convertedValue, parent.getFoundsetManager().getColumnConverterManager(),
				parent.getFoundsetManager().getColumnValidatorManager());
		}
		else if (parent.getFoundsetManager().isNullColumnValidatorEnabled())
		{
			//check for not null constraint
			Column c = null;
			try
			{
				c = sheet.getTable().getColumn(dataProviderID);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			if (c != null && !c.getAllowNull())
			{
				throw new IllegalArgumentException(Messages.getString("servoy.record.error.validation", new Object[] { dataProviderID, convertedValue })); //$NON-NLS-1$
			}
		}

		boolean wasUNINITIALIZED = false;
		if (o == UNINITIALIZED)
		{
			o = null;
			wasUNINITIALIZED = true;
		}

		boolean isCalculation = containsCalculation(dataProviderID);
		//if we receive NULL from the db for Empty strings in Servoy calcs, return value
		if (o == null && "".equals(convertedValue) && isCalculation) //$NON-NLS-1$
		{
			mustRecalculate(dataProviderID, false);
			return convertedValue;
		}
		if (!Utils.equalObjects(o, convertedValue))
		{
			boolean mustStop = false;
			if (columnIndex != -1 && columnIndex < columndata.length)
			{
				mustStop = !parent.getFoundsetManager().getEditRecordList().isEditing();
				if (src != null && existInDB && !wasUNINITIALIZED) //if not yet existInDB, leave startEdit to Foundset new/duplicateRecord code!
				{
					src.startEditing(false);
				}

				createOldValuesIfNeeded();
				columndata[columnIndex] = convertedValue;
			}
			else if (isCalculation)
			{
				unstoredCalcCache.put(dataProviderID, convertedValue);
			}
			lastException = null;

			// Reset the mustRecalculate here, before setValue fires events, so if it is an every time changing calculation it will not be calculated again and again
			if (isCalculation)
			{
				mustRecalculate(dataProviderID, false);
				threadCalculationComplete(dataProviderID);
			}

			handleCalculationDependencies(sheet.getTable().getColumn(dataProviderID), dataProviderID);

			FireCollector collector = FireCollector.getFireCollector();
			try
			{
				fireNotifyChange(dataProviderID, convertedValue, collector);
			}
			finally
			{
				collector.done();
			}

			if (src != null && mustStop && existInDB && !wasUNINITIALIZED)
			{
				try
				{
					src.stopEditing();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			return o;
		}
		else if (isCalculation)
		{
			// Reset the mustRecalculate here, before setValue fires events, so if it is an every time changing calculation it will not be calculated again and again
			mustRecalculate(dataProviderID, false);
		}
		return convertedValue;//is same so return
	}

	/*
	 * Don't call this method if you want to update a value in this row that is completely tracked. Because this method does not create the oldValues backup
	 * array. It just modifies directly the actual values. If isChanged() call is false before the call to this method then after this the isChanged() call is
	 * still false..
	 */
	void setRawValue(String dataProviderID, Object value)
	{
		SQLSheet sheet = parent.getSQLSheet();
		int columnIndex = sheet.getColumnIndex(dataProviderID);
		if (columnIndex >= 0 && columnIndex < columndata.length)
		{
			columndata[columnIndex] = value;
		}
		handleCalculationDependencies(sheet.getTable().getColumn(dataProviderID), dataProviderID);
	}

	protected void handleCalculationDependencies(Column column, String dataProviderID)
	{
		if (column != null && (column.getFlags() & IBaseColumn.IDENT_COLUMNS) != 0)
		{
			// PK update, recalc hash, update calculation dependencies and fire depending calcs
			getRowManager().fireDependingCalcsForPKUpdate(this, getPKHashKey());
		}
		else
		{
			getRowManager().fireDependingCalcs(getPKHashKey(), dataProviderID, null);
		}
	}

	public Object getOldRequiredValue(String dataProviderID)// incase a primary key is changed
	{
		if (oldValues != null && oldValues.length != 0)
		{
			SQLSheet.SQLDescription desc = parent.getSQLSheet().getSQLDescription(SQLSheet.UPDATE);
			List<String> list = desc.getOldRequiredDataProviderIDs();
			for (int i = 0; i < list.size(); i++)
			{
				String array_element = list.get(i);
				if (dataProviderID.equals(array_element))
				{
					int columnIndex = parent.getSQLSheet().getColumnIndex(dataProviderID);
					if (columnIndex >= 0)
					{
						if (oldValues[columnIndex] != null)
						{
							return oldValues[columnIndex];
						}
						break;
					}
				}
			}
		}
		return null;//getValue(dataProviderID, true);
	}

	synchronized void createOldValuesIfNeeded()
	{
		if (oldValues == null && existInDB)
		{
			//clone
			oldValues = new Object[columndata.length];
			System.arraycopy(columndata, 0, oldValues, 0, columndata.length);
			//Note: what we assume here is that in the 'columndata' the primarey keys are infront and in same order as requiredDataProviderIDs(==primarey keys)
			//the SQLGenerator does this currently!
		}
		return;
	}

	//this makes it possible to validate the state before it is processed again due to some listner being fired
	void flagExistInDB()
	{
		if (!isRemoving)
		{
			existInDB = true;
			synchronized (this)
			{
				oldValues = null;//dump any old shit
			}
			softReferenceAllByteArrays();
		}
	}

	void clearExistInDB()
	{
		existInDB = false;
	}

	private void softReferenceAllByteArrays()
	{
		for (int i = 0; i < columndata.length; i++)
		{
			if (columndata[i] instanceof byte[] && ((byte[])columndata[i]).length > 50000)
			{
				columndata[i] = ValueFactory.createBlobMarkerValue((byte[])columndata[i]);
			}
		}
	}

	String recalcPKHashKey()
	{
		pkHashKey = null;
		return getPKHashKey();
	}

	//See ALSO RowManager.createPKHashKey
	public String getPKHashKey()
	{
		if (pkHashKey == null)
		{
			SQLSheet sheet = parent.getSQLSheet();
			int[] pkpos = sheet.getPKIndexes();
			Object[] pks = new Object[pkpos.length];
			for (int i = 0; i < pkpos.length; i++)
			{
				if (oldValues != null)
				{
					pks[i] = oldValues[pkpos[i]];
				}
				else
				{
					pks[i] = columndata[pkpos[i]];
				}
			}
			pkHashKey = RowManager.createPKHashKey(pks);
		}
		return pkHashKey;
	}

	public Object[] getPK()
	{
		int[] pkpos = parent.getSQLSheet().getPKIndexes();
		Object[] retval = new Object[pkpos.length];
		for (int i = 0; i < pkpos.length; i++)
		{
			Object val = null;
			if (oldValues != null)
			{
				val = oldValues[pkpos[i]];
			}
			else
			{
				val = columndata[pkpos[i]];
			}
			if (val instanceof DbIdentValue && ((DbIdentValue)val).getPkValue() != null)
			{
				val = ((DbIdentValue)val).getPkValue();
			}
			retval[i] = val;
		}
		return retval;
	}

	public boolean isChanged()
	{
		if (!existInDB) return true;
		if (oldValues != null)
		{
			for (int i = 0; i < oldValues.length; i++)
			{
				if (!Utils.equalObjects(oldValues[i], columndata[i])) return true;
			}
			oldValues = null;
		}
		return false;
	}

	public Object[] getRawColumnData()
	{
		return columndata;
	}

	public Object[] getRawOldColumnData()
	{
		return oldValues;
	}

	public RowManager getRowManager()
	{
		return parent;
	}

	boolean lockedByMyself()
	{
		return parent.lockedByMyself(this);
	}

	void rollbackFromDB() throws Exception
	{
		parent.rollbackFromDB(this, true, ROLLBACK_MODE.OVERWRITE_CHANGES);
	}

	void rollbackFromDB(ROLLBACK_MODE mode) throws Exception
	{
		parent.rollbackFromDB(this, true, mode);
	}

	void setRollbackData(Object[] array, ROLLBACK_MODE mode)
	{
		Map<String, Object> changedColumns = new HashMap<String, Object>();
		String[] columnNames = getRowManager().getSQLSheet().getColumnNames();
		synchronized (this)
		{
			if (mode == ROLLBACK_MODE.OVERWRITE_CHANGES || oldValues == null)
			{
				if (columnNames != null && array.length == columnNames.length)
				{
					for (int i = 0; i < array.length; i++)
					{
						if (!Utils.equalObjects(array[i], columndata[i]))
						{
							changedColumns.put(columnNames[i], array[i]);
						}
					}
				}
				columndata = array;
				oldValues = null;
			}
			else
			{
				if (mode != ROLLBACK_MODE.KEEP_CHANGES)
				{
					for (int i = 0; i < oldValues.length; i++)
					{
						if (!Utils.equalObjects(oldValues[i], array[i]))
						{
							columndata[i] = array[i];
							changedColumns.put(columnNames[i], array[i]);
						}
					}
				}
				oldValues = array;
			}
		}
		fireChanges(changedColumns);
	}

	void rollbackFromOldValues()
	{
		Map<String, Object> changedColumns = new HashMap<String, Object>();
		String[] columnNames = getRowManager().getSQLSheet().getColumnNames();
		synchronized (this)
		{
			if (oldValues != null)
			{
				if (columnNames != null && oldValues.length == columnNames.length)
				{
					for (int i = 0; i < oldValues.length; i++)
					{
						if (!Utils.equalObjects(oldValues[i], columndata[i]))
						{
							changedColumns.put(columnNames[i], oldValues[i]);
						}
					}
				}
				columndata = oldValues;
				oldValues = null;
			}
		}
		// maybe is new record, just clear exception
		lastException = null;
		fireChanges(changedColumns);
	}

	/**
	 * @param changedColumns
	 */
	private void fireChanges(Map<String, Object> changedColumns)
	{
		if (changedColumns.size() > 0)
		{
			for (String dataProviderID : changedColumns.keySet())
			{
				parent.fireDependingCalcs(getPKHashKey(), dataProviderID, null);
			}
			parent.fireNotifyChange(null, this, this.getPKHashKey(), changedColumns.keySet().toArray(), RowEvent.UPDATE);
			FireCollector collector = FireCollector.getFireCollector();
			try
			{
				for (String dataProviderID : changedColumns.keySet())
				{
					fireNotifyChange(dataProviderID, changedColumns.get(dataProviderID), collector);
				}
			}
			finally
			{
				collector.done();
			}
		}
	}

	void setLastException(Exception lastException)
	{
		this.lastException = lastException;
	}

	Exception getLastException()
	{
		return lastException;
	}

	@Override
	public String toString()
	{
		String[] columnNames = this.parent.getSQLSheet().getColumnNames();
		StringBuilder sb = new StringBuilder();
		sb.append("Row("); //$NON-NLS-1$
		sb.append(parent.getFoundsetManager().getDataSource(parent.getSQLSheet().getTable()));
		sb.append(")[DATA:"); //$NON-NLS-1$
		for (int i = 0; i < columndata.length; i++)
		{
			sb.append(columnNames[i]);
			sb.append('=');
			sb.append(columndata[i]);
			sb.append(',');
		}
		sb.append("  CALCULATIONS: "); //$NON-NLS-1$
		sb.append(unstoredCalcCache);
		sb.append(']');
		return sb.toString();
	}

	private final SortedList<String> calcsUptodate = new SortedList<String>(StringComparator.INSTANCE, 3);

	/** Should never be called directly, always use RowManager.
	 * @see RowManager.flagRowCalcForRecalculation
	 * @param dp
	 */
	boolean internalFlagCalcForRecalculation(String dp)
	{
		synchronized (calcsUptodate)
		{
			return calcsUptodate.remove(dp);
		}
	}

	protected List<String> getCalcsUptodate()
	{
		synchronized (calcsUptodate)
		{
			return new ArrayList<String>(calcsUptodate);
		}
	}

	/**
	 * @param dataProviderID
	 * @return
	 */
	public boolean mustRecalculate(String dataProviderID, boolean justTesting)
	{
		synchronized (calcsUptodate)
		{
			if (!calcsUptodate.contains(dataProviderID))
			{
				if (!justTesting) calcsUptodate.add(dataProviderID);
				return true;
			}
		}
		return false;
	}

	/**
	 * Synchronization for not calculating the same calculation on multiple threads simultaneously...<br>
	 * After calling this method, YOU MUST call in a finally block {@link #threadCalculationComplete()} method.
	 * @param dataProviderID the calculation.
	 */
	public void threadWillExecuteCalculation(String dataProviderID)
	{
		Thread currentThread = Thread.currentThread();
		Thread previous = calculatingThreads.putIfAbsent(dataProviderID, currentThread);
		if (previous != null && previous != currentThread)
		{
			long time = System.currentTimeMillis();
			try
			{
				previous = calculatingThreads.putIfAbsent(dataProviderID, currentThread);
				while (previous != null && previous != currentThread && System.currentTimeMillis() < (time + 5000))
				{
					synchronized (calculatingThreads)
					{
						calculatingThreads.wait(1000);
					}
					previous = calculatingThreads.putIfAbsent(dataProviderID, currentThread);
				}
			}
			catch (InterruptedException e)
			{
				//ignore
			}
			if (previous != null && previous != currentThread)
			{
				try
				{
					StackTraceElement[] stackTrace = previous.getStackTrace();
					StringBuilder sb = new StringBuilder();
					sb.append("Calc '" + dataProviderID + "' did time out for thread: " + currentThread.getName() + " still waiting for: " +
						previous.getName() + ", stack:");
					for (StackTraceElement stackTraceElement : stackTrace)
					{
						sb.append("\n");
						sb.append(stackTraceElement.toString());
					}
					Debug.error(sb.toString(), new RuntimeException("calc timeout"));
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	/**
	 * Tell the row that current thread has finished calculating this calculation.
	 * @param dataProviderID the calculation.
	 */
	public void threadCalculationComplete(String dataProviderID)
	{
		calculatingThreads.remove(dataProviderID, Thread.currentThread());
		synchronized (calculatingThreads)
		{
			calculatingThreads.notifyAll();
		}
	}

	private boolean isRemoving = false;

	public void remove()
	{
		isRemoving = true;
		Object[] array;
		synchronized (listeners)
		{
			array = listeners.keySet().toArray();
		}

		for (Object element2 : array)
		{
			IRowChangeListener element = (IRowChangeListener)element2;
			element.rowRemoved();
		}
	}
}
