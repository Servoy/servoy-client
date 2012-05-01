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
import java.util.Map;
import java.util.WeakHashMap;

import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.SQLSheet.VariableInfo;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.UUID;
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
	private Object getRawValue(String id)
	{
		int columnIndex = parent.getSQLSheet().getColumnIndex(id);
		if (columnIndex != -1)
		{
			return getValue(columnIndex, false);
		}
		return unstoredCalcCache.get(id);
	}

	public boolean existInDB()
	{
		return existInDB;
	}

	public boolean containsCalculation(String id)
	{
		return parent.getSQLSheet().containsCalculation(id);
	}

	Object getValue(int columnIndex)
	{
		// call this with false, else things like using a dbident in javascript or creating related records are going wrong.
		Object value = getValue(columnIndex, false);
		return parent.getSQLSheet().convertValueToObject(value, columnIndex, parent.getFoundsetManager().getColumnConverterManager());
	}

	Object getValue(int columnIndex, boolean unwrapDbIdent)
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
			if (o instanceof DbIdentValue)
			{
				return o;
			}
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
			if (columnIndex >= 0)
			{
				Pair<String, Map<String, String>> converterInfo = sheet.getColumnConverterInfo(columnIndex);
				if (converterInfo != null)
				{
					IColumnConverter conv = parent.getFoundsetManager().getColumnConverterManager().getConverter(converterInfo.getLeft());
					if (conv == null)
					{
						throw new IllegalStateException(Messages.getString("servoy.error.converterNotFound", new Object[] { converterInfo.getLeft() })); //$NON-NLS-1$
					}
					try
					{
						convertedValue = conv.convertFromObject(converterInfo.getRight(), variableInfo.type, convertedValue);
					}
					catch (Exception e)
					{
						throw new IllegalArgumentException(
							Messages.getString(
								"servoy.record.error.settingDataprovider", new Object[] { dataProviderID, Column.getDisplayTypeString(variableInfo.type), convertedValue }), e); //$NON-NLS-1$
					}

					int valueLen = Column.getObjectSize(convertedValue, variableInfo.type);
					if (valueLen > 0 && variableInfo.length > 0 && valueLen > variableInfo.length) // insufficient space to save value
					{
						throw new IllegalArgumentException(
							Messages.getString(
								"servoy.record.error.columnSizeTooSmall", new Object[] { dataProviderID, Column.getDisplayTypeString(variableInfo.type), convertedValue })); //$NON-NLS-1$						
					}
				}

				Pair<String, Map<String, String>> validatorInfo = sheet.getColumnValidatorInfo(columnIndex);
				if (validatorInfo != null)
				{
					IColumnValidator validator = parent.getFoundsetManager().getColumnValidatorManager().getValidator(validatorInfo.getLeft());
					if (validator == null)
					{
						throw new IllegalStateException(Messages.getString("servoy.error.validatorNotFound", new Object[] { validatorInfo.getLeft() })); //$NON-NLS-1$
					}

					try
					{
						validator.validate(validatorInfo.getRight(), convertedValue);
					}
					catch (IllegalArgumentException e)
					{
						String msg = Messages.getString("servoy.record.error.validation", new Object[] { dataProviderID, convertedValue }); //$NON-NLS-1$
						if (e.getMessage() != null && e.getMessage().length() != 0) msg += ' ' + e.getMessage();
						throw new IllegalArgumentException(msg);
					}
				}

				if ((variableInfo.flags & Column.UUID_COLUMN) != 0)
				{
					// this is a UUID column, convert from UUID
					UUID uuid = Utils.getAsUUID(convertedValue, false);
					if (uuid != null)
					{
						switch (Column.mapToDefaultType(variableInfo.type))
						{
							case IColumnTypes.TEXT :
								convertedValue = uuid.toString();
								break;
							case IColumnTypes.MEDIA :
								convertedValue = uuid.toBytes();
								break;
						}
					}
				}
			}

			if (variableInfo.type != IColumnTypes.MEDIA || (variableInfo.flags & Column.UUID_COLUMN) != 0)
			{
				try
				{
					convertedValue = Column.getAsRightType(variableInfo.type, variableInfo.flags, convertedValue, null, variableInfo.length, null, true); // dont use timezone here, should only be done in ui related stuff
				}
				catch (Exception e)
				{
					Debug.error(e);
					throw new IllegalArgumentException(
						Messages.getString(
							"servoy.record.error.settingDataprovider", new Object[] { dataProviderID, Column.getDisplayTypeString(variableInfo.type), convertedValue })); //$NON-NLS-1$
				}
			}
		}
		else if (parent.getFoundsetManager().getNullColumnValidatorEnabled())
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

		//if we receive NULL from the db for Empty strings in Servoy calcs, return value
		if (o == null && "".equals(convertedValue) && containsCalculation(dataProviderID)) return convertedValue; //$NON-NLS-1$
		if (!Utils.equalObjects(o, convertedValue))
		{
			boolean mustStop = !parent.getFoundsetManager().getEditRecordList().isEditing();
			if (src != null && existInDB && !wasUNINITIALIZED) //if not yet existInDB, leave startEdit to Foundset new/duplicateRecord code!
			{
				src.startEditing(false);
			}

			if (columnIndex != -1 && columnIndex < columndata.length)
			{
				createOldValuesIfNeeded();
				columndata[columnIndex] = convertedValue;
			}
			else if (containsCalculation(dataProviderID))
			{
				unstoredCalcCache.put(dataProviderID, convertedValue);
			}
			lastException = null;

			handleCalculationDependencies(sheet.getTable().getColumn(dataProviderID), dataProviderID);

			FireCollector collector = new FireCollector();
			fireNotifyChange(dataProviderID, convertedValue, collector);
			collector.done();

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
		return convertedValue;//is same so return
	}

	protected void handleCalculationDependencies(Column column, String dataProviderID)
	{
		if (column != null && (column.getFlags() & Column.IDENT_COLUMNS) != 0)
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
		existInDB = true;
		synchronized (this)
		{
			oldValues = null;//dump any old shit
		}
		softReferenceAllByteArrays();
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
				if (pks[i] instanceof DbIdentValue)
				{
					Object identValue = ((DbIdentValue)pks[i]).getPkValue();
					if (identValue == null)
					{
						pks[i] = "_svdbi" + pks[i].hashCode(); // DbIdentValue.hashCode() must be stable, i.e. not change when value is set //$NON-NLS-1$
					}
					else
					{
						pks[i] = identValue;
					}
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
		return (oldValues != null || !existInDB);
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
		synchronized (this)
		{
			if (mode == ROLLBACK_MODE.OVERWRITE_CHANGES || oldValues == null)
			{
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
						}
					}
				}
				oldValues = array;
			}
		}
		getRowManager().fireDependingCalcs(getPKHashKey(), null, null);
	}

	void rollbackFromOldValues()
	{
		boolean fire = false;
		synchronized (this)
		{
			if (oldValues != null)
			{
				columndata = oldValues;
				oldValues = null;
				fire = true;
			}
		}
		if (fire)
		{
			parent.fireDependingCalcs(getPKHashKey(), null, null);
			parent.fireNotifyChange(null, this, null, RowEvent.UPDATE);
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

}
