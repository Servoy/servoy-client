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


import java.lang.ref.ReferenceQueue;
import java.rmi.RemoteException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.dataprocessing.RowManager.RowFireNotifyChange.CalculationDependencyData;
import com.servoy.j2db.dataprocessing.ValueFactory.BlobMarkerValue;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryInsert;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.SoftReferenceWithData;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Manager for rows from one table
 *
 * @author jblok
 */
public class RowManager implements IModificationListener, IFoundSetEventListener
{
	private final FoundSetManager fsm;
	private final ReferenceQueue<Row> referenceQueue;
	private final Map<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>> pkRowMap; // pkString -> SoftReference(Row)
	private final SQLSheet sheet;
	private final WeakHashMap<IRowListener, Object> listeners;
	private final Set<NamedLock> lockedRowPKs;
	private final Map<String, Set<String>> globalCalcDependencies = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> relationsUsedInCalcs = new HashMap<String, Set<String>>();
	private final Map<String, Map<String, Set<String>>> aggregateCalcDependencies = new HashMap<String, Map<String, Set<String>>>();

	private Set<String> deleteSet;

	RowManager(FoundSetManager fsm, SQLSheet sheet)
	{
		this.fsm = fsm;
		this.sheet = sheet;
		pkRowMap = new ConcurrentHashMap<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>>(64);
		referenceQueue = new ReferenceQueue<Row>();
		listeners = new WeakHashMap<IRowListener, Object>(10);
		lockedRowPKs = Collections.synchronizedSet(new HashSet<NamedLock>());//my locks
	}

	public void dispose()
	{
		fsm.removeGlobalFoundsetEventListener(this);
	}

	private static Object dummy = new Object();

	void register(IRowListener fs)
	{
		synchronized (listeners)
		{
			listeners.put(fs, dummy);
		}
	}

	FoundSetManager getFoundsetManager()
	{
		return fsm;
	}

	//See ALSO Row.getPKHashKey
	public static String createPKHashKey(Object[] pk)
	{
		StringBuilder sb = new StringBuilder();
		if (pk != null)
		{
			for (Object val : pk)
			{
				if (val instanceof DbIdentValue)
				{
					Object identValue = ((DbIdentValue)val).getPkValue();
					if (identValue == null)
					{
						val = "_svdbi" + val.hashCode(); // DbIdentValue.hashCode() must be stable, i.e. not change when value is set //$NON-NLS-1$
					}
					else
					{
						val = identValue;
					}
				}
				String str;
				if (val instanceof byte[])
				{
					str = Utils.encodeBASE64((byte[])val); // UUID
				}
				else if (val instanceof UUID)
				{
					// make sure UUID PKs are matched regardless of casing
					str = val.toString().toLowerCase();
				}
				else if (val instanceof String && ((String)val).length() == 36 && ((String)val).split("-").length == 5) //$NON-NLS-1$
				{
					// make sure UUID PKs are matched regardless of casing (MSQ Sqlserver returns uppercase UUID strings for uniqueidentifier columns)
					str = ((String)val).toLowerCase();
				}
				else if (val instanceof Date)
				{
					str = Long.toString(((Date)val).getTime());
				}
				else if (val instanceof Float && ((Float)val).longValue() == ((Float)val).floatValue())
				{
					str = Long.toString(((Float)val).longValue());
				}
				else if (val instanceof Double && ((Double)val).longValue() == ((Double)val).doubleValue())
				{
					str = Long.toString(((Double)val).longValue());
				}
				else
				{
					str = Utils.convertToString(val);
				}
				if (val != null)
				{
					sb.append(str.length());
				}
				sb.append('.');
				sb.append(str);
				sb.append(';');
			}
		}
		return sb.toString();
	}

	Pair<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> getCachedRow(Object[] pk)
	{
		return getCachedRow(createPKHashKey(pk));
	}

	private Pair<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> getCachedRow(String pkhashKey)
	{
		Row rowData = null;
		Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = null;
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkhashKey);
		if (sr != null)
		{
			data = sr.getData();
			rowData = sr.get();
			if (rowData == null)
			{
				Debug.trace("-----------CacheMiss"); //$NON-NLS-1$
				if (canRemove(sr))
				{
					removeRowReferences(pkhashKey, null);
					pkRowMap.remove(pkhashKey);
					data = null;
				}
			}
		}
		return new Pair<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>(rowData, data);
	}

	Row getRowBasedonPKFromEntireColumnArray(Object[] columndata)
	{
		return getRowBasedonPKFromEntireColumnArrayEx(columndata, null /* row notifies are fired immediately */);
	}

	/*
	 * Called from RelatedFoundSet constructor, do not fire the row notify changes, but leave them to caller
	 */
	Row getRowBasedonPKFromEntireColumnArray(Object[] columndata, List<Runnable> fireRunnables)
	{
		final List<RowFireNotifyChange> fires = new ArrayList<RowFireNotifyChange>();
		Row row = getRowBasedonPKFromEntireColumnArrayEx(columndata, fires);
		if (fires.size() > 0)
		{
			fireRunnables.add(new Runnable()
			{
				public void run()
				{
					fireRowNotifyChanges(fires);
				}
			});
		}
		return row;
	}

	//creates if not existent
	private Row getRowBasedonPKFromEntireColumnArrayEx(Object[] columndata, List<RowFireNotifyChange> fires)
	{
		int[] pkpos = sheet.getPKIndexes();
		Object[] pk = new Object[pkpos.length];
		for (int i = 0; i < pkpos.length; i++)
		{
			Object val = columndata[pkpos[i]];
			pk[i] = val;
		}
		Row rowData = null;
		String pkHashKey = createPKHashKey(pk);
		boolean fireCalcs = false;
		synchronized (this)
		{
			Pair<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> cachedRow = getCachedRow(pkHashKey);
			rowData = cachedRow.getLeft();
			Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = cachedRow.getRight();
			if (rowData == null)
			{
				rowData = createExistInDBRowObject(columndata);
				SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = new SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>(
					rowData, referenceQueue);
				pkRowMap.put(rowData.getPKHashKey(), sr);
				if (data != null)
				{
					// use existing dependencies if row was GD'd before
					sr.setData(data);
					fireCalcs = true;
				}
				clearAndCheckCache();
			}
		}
		if (fireCalcs)
		{
			fireDependingCalcs(pkHashKey, null, fires);
		}
		return rowData;
	}

	Row createNotYetExistInDBRowObject(Object[] data, boolean addToMap)
	{
		return createRowObject(data, false, addToMap);
	}

	Row createExistInDBRowObject(Object[] data)
	{
		return createRowObject(data, true, false);
	}

	private Row createRowObject(Object[] data, boolean existInDB, boolean addToMap)
	{
		Row row = new Row(this, data, sheet.getAllUnstoredCalculationNamesWithNoValue(), existInDB);
		if (addToMap)
		{
			pkRowMap.put(row.getPKHashKey(), new SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>(row,
				referenceQueue));
			clearAndCheckCache();
		}
		return row;
	}

	/**
	 * Rollback data from db, return whether the data was found
	 * @param row
	 * @param doFires
	 * @param mode
	 * @return
	 * @throws ServoyException
	 */
	boolean rollbackFromDB(Row row, boolean doFires, Row.ROLLBACK_MODE mode) throws ServoyException
	{
		if (!row.existInDB())
		{
			return false;
		}
		Object[] pk = row.getPK();
		QuerySelect select = (QuerySelect)AbstractBaseQuery.deepClone(sheet.getSQL(SQLSheet.SELECT));
		if (!select.setPlaceholderValue(new TablePlaceholderKey(select.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY), pk))
		{
			Debug.error(new RuntimeException("Could not set placeholder " + new TablePlaceholderKey(select.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY) + //$NON-NLS-1$
				" in query " + select + "-- continuing")); //$NON-NLS-1$//$NON-NLS-2$
		}
		IDataSet formdata;
		try
		{
			String transaction_id = null;
			GlobalTransaction gt = fsm.getGlobalTransaction();
			if (gt != null)
			{
				transaction_id = gt.getTransactionID(sheet.getServerName());
			}
			formdata = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, select,
				fsm.getTableFilterParams(sheet.getServerName(), select), false, 0, 1, false);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}

		// construct Rows
		boolean found = formdata.getRowCount() >= 1;
		if (found)
		{
			row.setRollbackData(formdata.getRow(0), mode);
		}
		// else // whoa, row is deleted or pk is updated!

		if (doFires) fireNotifyChange(null, row, null, found ? RowEvent.UPDATE : RowEvent.DELETE);
		return found;
	}

	private final ThreadLocal<String> adjustingForChangeByOtherPKHashKey = new ThreadLocal<String>();

	//return true if i had row and did update
	@SuppressWarnings("nls")
	boolean changeByOther(String pkHashKey, int action, Object[] insertColumnDataOrChangedColumns, Row insertedRow)
	{
		Pair<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> cachedRow;
		synchronized (this)
		{
			cachedRow = getCachedRow(pkHashKey);
		}
		Row rowData = cachedRow.getLeft();
		if (rowData != null && action != ISQLActionTypes.INSERT_ACTION) // in case of rawSQL insert & notify, insertColumnDataOrChangedColumns is null, so the row corresponding to the pk was taken from DB and cached
		{
			if (action == ISQLActionTypes.DELETE_ACTION)
			{
				fireNotifyChange(null, rowData, null, RowEvent.DELETE);
			}
			if (rowData.hasListeners() && action == ISQLActionTypes.UPDATE_ACTION)
			{
				if (lockedByMyself(rowData))
				{
					Debug.error("the row with pk: " + rowData.getPKHashKey() + " from  datasource " + sheet.getServerName() + "/" + sheet.getTable().getName() +
						" was updating by another client when this client had a lock on it, this shouldn't be possible, the other client should have tried to get the lock first.");
				}
				try
				{
					adjustingForChangeByOtherPKHashKey.set(rowData.getPKHashKey());
					boolean found = rollbackFromDB(rowData, false, Row.ROLLBACK_MODE.UPDATE_CHANGES);
					int eventType = RowEvent.UPDATE;
					if (!found && rowData.existInDB())
					{
						// row was not found, check if the pk was updated
						eventType = RowEvent.DELETE;
						if (insertColumnDataOrChangedColumns != null)
						{
							List<String> pkdps = Arrays.asList(sheet.getPKColumnDataProvidersAsArray());
							for (Object changedColumnName : insertColumnDataOrChangedColumns)
							{
								if (pkdps.contains(changedColumnName))
								{
									eventType = RowEvent.PK_UPDATED;
									break;
								}
							}
						}
					}
					fireNotifyChange(null, rowData, insertColumnDataOrChangedColumns, eventType);
				}
				catch (Exception e)
				{
					Debug.error(e);//what can we do here
				}
				finally
				{
					adjustingForChangeByOtherPKHashKey.remove();
				}
				return true;
			}
			else
			{
				// the row is in memory but not longer referenced from any record or it was deleted.
				// do remove it so that it will be re queried when needed (when it was not deleted)
				removeRowReferences(pkHashKey, null);
				pkRowMap.remove(pkHashKey);
			}
			return false;
		}

		if (action == ISQLActionTypes.INSERT_ACTION)
		{
			if (insertedRow != null)
			{
				fireNotifyChange(null, insertedRow, null, RowEvent.INSERT);
				if (!insertedRow.hasListeners() && canRemove(pkRowMap.get(pkHashKey))) //new row is not in use
				{
					removeRowReferences(pkHashKey, null);
					pkRowMap.remove(pkHashKey);
					return false;
				}
				return true;
			}
			else if (insertColumnDataOrChangedColumns != null && insertColumnDataOrChangedColumns.length == sheet.getColumnNames().length) //last test is just to make sure
			{
				rowData = createExistInDBRowObject(insertColumnDataOrChangedColumns);
				fireNotifyChange(null, rowData, null, RowEvent.INSERT);
				if (rowData.hasListeners())//new row is in use
				{
					boolean fireCalcs = false;
					synchronized (this)
					{
						SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = new SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>(
							rowData, referenceQueue);
						if (cachedRow.getRight() != null)
						{
							sr.setData(cachedRow.getRight());
							fireCalcs = true;
						}
						pkRowMap.put(rowData.getPKHashKey(), sr);
					}
					if (fireCalcs)
					{
						fireDependingCalcs(pkHashKey, null, null);
					}
					return true;
				}
			}
		}
		else if (action == ISQLActionTypes.UPDATE_ACTION && insertColumnDataOrChangedColumns != null)
		{
			// update of row that is not cached by this client
			fireNotifyChange(null, null, insertColumnDataOrChangedColumns, RowEvent.UPDATE);
		}

		//we did not have row cached in memory no update is need, we will get the change if we query for row when needed
		return false;
	}

	synchronized List<Row> getRows(IDataSet pks, int row, int sizeHint, boolean queryAll) throws ServoyException
	{
		List<Row> retval = new SafeArrayList<Row>();
		if (row >= pks.getRowCount()) return retval;

		Object[] pk = pks.getRow(row);
		Row rowData = queryAll ? null : getCachedRow(pk).getLeft();
		if (rowData == null)
		{
			String transaction_id = null;
			GlobalTransaction gt = fsm.getGlobalTransaction();
			if (gt != null)
			{
				transaction_id = gt.getTransactionID(sheet.getServerName());
			}

			IDataSet formdata = null;
			QuerySelect select = (QuerySelect)sheet.getSQL(SQLSheet.SELECT);
			int maxRow = Math.min(row + sizeHint, pks.getRowCount());
			// get the PK array
			int ncols = pks.getColumnCount();
			int nvals = 0;
			@SuppressWarnings("unchecked")
			List<Object>[] valueLists = new List[ncols];
			for (int c = 0; c < ncols; c++)
			{
				valueLists[c] = new ArrayList<Object>();
			}
			for (int i = 0; i < maxRow - row; i++)
			{
				Object[] data = pks.getRow(row + i);
				if (data != null)
				{
					if (data.length != ncols)
					{
						throw new RuntimeException("Inconsistent PK set width"); //$NON-NLS-1$
					}
					boolean add = true;
					for (int c = 0; add && c < ncols; c++)
					{
						add = !(data[c] instanceof DbIdentValue);
					}
					if (add)
					{
						nvals++;
						for (int c = 0; c < ncols; c++)
						{
							valueLists[c].add(data[c]);
						}
					}
				}
			}

			Object[][] values = new Object[ncols][];
			for (int c = 0; c < ncols; c++)
			{
				values[c] = valueLists[c].toArray();
			}

			if (!select.setPlaceholderValue(new TablePlaceholderKey(select.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY), values))
			{
				Debug.error(new RuntimeException(
					"Could not set placeholder " + new TablePlaceholderKey(select.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY) + //$NON-NLS-1$
						" in query " + select + "-- continuing")); //$NON-NLS-1$//$NON-NLS-2$
			}

			long time = System.currentTimeMillis();
			try
			{
				SQLStatement trackingInfo = null;
				if (fsm.getEditRecordList().hasAccess(sheet.getTable(), IRepository.TRACKING_VIEWS))
				{
					trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, sheet.getServerName(), sheet.getTable().getName(), pks, null);
					trackingInfo.setTrackingData(sheet.getColumnNames(), new Object[][] { }, new Object[][] { }, fsm.getApplication().getUserUID(),
						fsm.getTrackingInfo(), fsm.getApplication().getClientID());
				}
				formdata = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), transaction_id, select,
					fsm.getTableFilterParams(sheet.getServerName(), select), false, 0, nvals, IDataServer.FOUNDSET_LOAD_QUERY, trackingInfo);
				if (Debug.tracing())
				{
					Debug.trace(Thread.currentThread().getName() + ": getting RowData time: " + (System.currentTimeMillis() - time) + ", SQL: " + //$NON-NLS-1$ //$NON-NLS-2$
						select.toString());
				}
			}
			catch (RemoteException e)
			{
				throw new RepositoryException(e);
			}

			//construct Rows
			for (int k = row; k < maxRow; k++)
			{
				String pkHash = createPKHashKey(pks.getRow(k));
				//reorder based on pk in mem,cannot do related sort icw SELECT_IN
				for (int r = 0; r < formdata.getRowCount(); r++)
				{
					Object[] columndata = formdata.getRow(r);
					rowData = getRowBasedonPKFromEntireColumnArray(columndata);
					if (pkHash.equals(createPKHashKey(rowData.getPK())))
					{
						retval.set(k - row, rowData);
						break;
					}
				}
			}
			if (retval.size() < maxRow - row)
			{
				retval.set(maxRow - row - 1, null);
			}

		}
		else
		{
			retval.add(rowData);
			if (sizeHint > 1)
			{
				int maxRow = Math.min(row + fsm.chunkSize, pks.getRowCount());
				for (int r = row + 1; r < maxRow; r++)
				{
					Object[] data = pks.getRow(r);
					Row r2 = getCachedRow(data).getLeft();
					if (r2 == null)
					{
						// if there is no row te be found, just break and return the currently found retval.
						break;
					}
					retval.add(r2);
				}
			}
		}
		return retval;
	}

	void fireNotifyChange(IRowListener skip, Row r, Object[] changedColumns, int eventType)
	{
		if (listeners.size() > 0)
		{
			RowEvent e = new RowEvent(this, r, eventType, changedColumns);

			// First copy it to a array list for concurrent mod...
			Object[] array = null;
			synchronized (listeners)
			{
				array = listeners.keySet().toArray();
			}

			for (Object element2 : array)
			{
				IRowListener element = (IRowListener)element2;
				if (element != skip) element.notifyChange(e);
			}
		}
		fsm.notifyChange(sheet.getTable());
	}

	void firePKUpdated(Row row, String oldKeyHash)
	{
		if (listeners.size() > 0)
		{
			RowEvent e = new RowEvent(this, row, RowEvent.PK_UPDATED, oldKeyHash);

			// First copy it to a array list for concurrent mod...
			Object[] array = null;
			synchronized (listeners)
			{
				array = listeners.keySet().toArray();
			}

			for (Object listener : array)
			{
				((IRowListener)listener).notifyChange(e);
			}
		}
	}

	RowUpdateInfo getRowUpdateInfo(Row row, boolean tracking) throws ServoyException
	{
		try
		{
			if (row.getRowManager() != this)
			{
				throw new IllegalArgumentException("I'm not the row manager from row"); //$NON-NLS-1$
			}

			if (adjustingForChangeByOtherPKHashKey.get() != null && adjustingForChangeByOtherPKHashKey.get().equals(row.getPKHashKey()))
			{
				row.flagExistInDB();
				//we ignore changes here because stored calc with time element are always changed,resulting in endlessloop between clients
				return null;
			}

			if (row.getLastException() instanceof DataException)
			{
				//cannot update an row which is not changed (which clears the dataexception)
				return null;
			}

			if (!row.isChanged()) return null;

			boolean mustRequeryRow = false;

			List<Column> dbPKReturnValues = new ArrayList<Column>();
			SQLSheet.SQLDescription sqlDesc = null;
			int statement_action;
			ISQLUpdate sqlUpdate = null;
			IServer server = fsm.getApplication().getSolution().getServer(sheet.getServerName());
			boolean oracleServer = SQLSheet.isOracleServer(server);
			boolean usesLobs = false;
			Table table = sheet.getTable();
			boolean doesExistInDB = row.existInDB();
			List<String> aggregatesToRemove = new ArrayList<String>(8);
			List<String> changedColumns = null;
			if (doesExistInDB)
			{
				statement_action = ISQLActionTypes.UPDATE_ACTION;
				sqlDesc = sheet.getSQLDescription(SQLSheet.UPDATE);
				sqlUpdate = (QueryUpdate)AbstractBaseQuery.deepClone(sqlDesc.getSQLQuery());
				List<String> req = sqlDesc.getRequiredDataProviderIDs();
				List<String> old = sqlDesc.getOldRequiredDataProviderIDs();

				Object[] olddata = row.getRawOldColumnData();
				if (olddata == null)//for safety only, nothing changed
				{
					return null;
				}
				Object[] newdata = row.getRawColumnData();
				for (int i = 0; i < olddata.length; i++)
				{
					String dataProviderID = req.get(i);
					Column c = table.getColumn(dataProviderID);
					ColumnInfo ci = c.getColumnInfo();
					if (ci != null && ci.isDBManaged())
					{
						mustRequeryRow = true;
					}
					else
					{
						Object modificationValue = c.getModificationValue(fsm.getApplication());
						if (modificationValue != null)
						{
							row.setRawValue(dataProviderID, modificationValue);
						}
						if (newdata[i] instanceof BlobMarkerValue)
						{
							// if it is a blob marker then it isn't something that is changed
							// because that would be a byte[]
							continue;
						}
						if ((olddata[i] != null && !olddata[i].equals(newdata[i])) || (newdata[i] != null && !newdata[i].equals(olddata[i])))
						{
							if (sheet.isUsedByAggregate(dataProviderID))
							{
								aggregatesToRemove.addAll(sheet.getAggregateName(dataProviderID));
							}
							Object robj = c.getAsRightType(newdata[i]);
							if (robj == null) robj = ValueFactory.createNullValue(c.getType());
							((QueryUpdate)sqlUpdate).addValue(
								new QueryColumn(((QueryUpdate)sqlUpdate).getTable(), c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale(),
									c.getFlags()), robj);
							if (changedColumns == null)
							{
								changedColumns = new ArrayList<String>(olddata.length - i);
							}
							changedColumns.add(c.getName());
							if (oracleServer && !usesLobs)
							{
								int type = c.getType();
								if (type == Types.BLOB && robj instanceof byte[] && ((byte[])robj).length > 4000)
								{
									usesLobs = true;
								}
								else if (type == Types.CLOB && robj instanceof String && ((String)robj).length() > 4000)
								{
									usesLobs = true;
								}
							}
						}
					}
				}

				if (changedColumns == null)//nothing changed after all
				{
					// clear the old data now else it will be kept and in a changed state.
					row.flagExistInDB();
					return null;
				}

				//add PK
				Object[] pkValues = new Object[old.size()];
				for (int j = 0; j < old.size(); j++)
				{
					String dataProviderID = old.get(j);
					pkValues[j] = row.getOldRequiredValue(dataProviderID);
				}

				// TODO: ckeck for success
				AbstractBaseQuery.setPlaceholderValue(sqlUpdate, new TablePlaceholderKey(((QueryUpdate)sqlUpdate).getTable(),
					SQLGenerator.PLACEHOLDER_PRIMARY_KEY), pkValues);
			}
			else
			{
				List<Object> argsArray = new ArrayList<Object>();
				statement_action = ISQLActionTypes.INSERT_ACTION;
				sqlDesc = sheet.getSQLDescription(SQLSheet.INSERT);
				sqlUpdate = (ISQLUpdate)AbstractBaseQuery.deepClone(sqlDesc.getSQLQuery());
				List<String> req = sqlDesc.getRequiredDataProviderIDs();
				if (Debug.tracing()) Debug.trace(sqlUpdate.toString());
				for (int i = 0; i < req.size(); i++)
				{
					String dataProviderID = req.get(i);
					if (sheet.isUsedByAggregate(dataProviderID))
					{
						aggregatesToRemove.addAll(sheet.getAggregateName(dataProviderID));
					}
					Column c = table.getColumn(dataProviderID);
					QueryColumn queryColumn = new QueryColumn(((QueryInsert)sqlUpdate).getTable(), c.getID(), c.getSQLName(), c.getType(), c.getLength(),
						c.getScale(), c.getFlags());
					ColumnInfo ci = c.getColumnInfo();
					if (c.isDBIdentity())
					{
						dbPKReturnValues.add(c);
						argsArray.add(row.getDbIdentValue());
					}
					else if (ci != null && ci.isDBManaged())
					{
						mustRequeryRow = true;
					}
					else
					{
						int columnIndex = getSQLSheet().getColumnIndex(dataProviderID);
						// HACK: DIRTY way, should use some kind of identifier preferably
						if (c.getDatabaseDefaultValue() != null && row.getRawValue(columnIndex, false) == null && c.getRowIdentType() == Column.NORMAL_COLUMN)
						{
							// The database has a default value, and the value is null, and this is an insert...
							// Remove the column from the query entirely and make sure the default value is requeried from the db.
							mustRequeryRow = true;
							((QueryInsert)sqlUpdate).removeColumn(queryColumn);
						}
						else
						{
							Object robj = c.getAsRightType(row.getRawValue(columnIndex, false));

							if (robj == null) robj = ValueFactory.createNullValue(c.getType());
							argsArray.add(robj);

							if (oracleServer && !usesLobs)
							{
								int type = c.getType();
								if (type == Types.BLOB && robj instanceof byte[] && ((byte[])robj).length > 4000)
								{
									usesLobs = true;
								}
								else if (type == Types.CLOB && robj instanceof String && ((String)robj).length() > 4000)
								{
									usesLobs = true;
								}
							}
						}
					}
				}
				AbstractBaseQuery.setPlaceholderValue(sqlUpdate, new TablePlaceholderKey(((QueryInsert)sqlUpdate).getTable(),
					SQLGenerator.PLACEHOLDER_INSERT_KEY), argsArray.toArray());
			}
			Object[] pk = row.getPK();
			IDataSet pks = new BufferedDataSet();
			pks.addRow(pk);
			String tid = null;
			GlobalTransaction gt = fsm.getGlobalTransaction();
			if (gt != null)
			{
				tid = gt.getTransactionID(sheet.getServerName());
			}
			QuerySelect requerySelect = null;
			if (mustRequeryRow)
			{
				requerySelect = (QuerySelect)AbstractBaseQuery.deepClone(sheet.getSQL(SQLSheet.SELECT));
				if (!requerySelect.setPlaceholderValue(new TablePlaceholderKey(requerySelect.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY), pk))
				{
					Debug.error(new RuntimeException(
						"Could not set placeholder " + new TablePlaceholderKey(requerySelect.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY) + //$NON-NLS-1$
							" in query " + requerySelect + "-- continuing")); //$NON-NLS-1$//$NON-NLS-2$
				}
			}
			SQLStatement statement = new SQLStatement(statement_action, sheet.getServerName(), table.getName(), pks, tid, sqlUpdate, fsm.getTableFilterParams(
				sheet.getServerName(), sqlUpdate), requerySelect);
			if (doesExistInDB) statement.setExpectedUpdateCount(1); // check that the row is updated (skip check for inert)
			if (changedColumns != null)
			{
				statement.setChangedColumns(changedColumns.toArray(new String[changedColumns.size()]));
			}
			statement.setOracleFixTrackingData(usesLobs && !tracking);
			statement.setIdentityColumn(dbPKReturnValues.size() == 0 ? null : dbPKReturnValues.get(0));
			if (tracking || usesLobs)
			{
				statement.setTrackingData(sheet.getColumnNames(), row.getRawOldColumnData() != null ? new Object[][] { row.getRawOldColumnData() } : null,
					row.getRawColumnData() != null ? new Object[][] { row.getRawColumnData() } : null, fsm.getApplication().getUserUID(),
					fsm.getTrackingInfo(), fsm.getApplication().getClientID());
			}
			return new RowUpdateInfo(row, statement, dbPKReturnValues, aggregatesToRemove);
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	void rowUpdated(final Row row, final String oldKeyHash, final IRowListener src, List<Runnable> runnables)
	{
		final boolean doesExistInDB = row.existInDB();
		row.flagExistInDB();//always needed flushes stuff

		String newKeyHash = row.recalcPKHashKey();
		if (!oldKeyHash.equals(newKeyHash))
		{
			// fire pk updated to IRowListeners
			runnables.add(new Runnable()
			{
				public void run()
				{
					firePKUpdated(row, oldKeyHash);
				}
			});
		}

		// run fires later (add this runnable here first because the runnables in EditRecordList are processed in reverse order)
		runnables.add(new Runnable()
		{
			public void run()
			{
				fireNotifyChange(src, row, null, doesExistInDB ? RowEvent.UPDATE : RowEvent.INSERT);
			}
		});

		// may add fires for depending calcs
		fireDependingCalcsForPKUpdate(row, oldKeyHash, runnables);
	}

	void fireDependingCalcsForPKUpdate(Row row, String oldKeyHash)
	{
		List<Runnable> runnables = new ArrayList<Runnable>(1);
		fireDependingCalcsForPKUpdate(row, oldKeyHash, runnables);
		for (Runnable runnable : runnables)
		{
			runnable.run();
		}
	}

	/**
	 * PK of a row may have been updated, the row may not have been saved yet.
	 * @param row
	 * @param oldKeyHash pkhash
	 * @param runnables
	 */
	synchronized void fireDependingCalcsForPKUpdate(final Row row, final String oldKeyHash, List<Runnable> runnables)
	{
		// do recalcPKHashKey incase its called before and pk did not yet exist
		String newKeyHash = row.recalcPKHashKey();
		if (!oldKeyHash.equals(newKeyHash))
		{
			final SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> srOld = pkRowMap.get(oldKeyHash);
			pkRowMap.put(newKeyHash, new SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>(row,
				referenceQueue));// (over)write new
			if (srOld != null)
			{
				// run fires later
				runnables.add(new Runnable()
				{
					public void run()
					{
						// calcs depending on old pk are invalid
						fireDependingCalcs(srOld, null, null);
						pkRowMap.remove(oldKeyHash);//remove old
					}
				});
			}
		}
		else
		{
			if (!pkRowMap.containsKey(newKeyHash))
			{
				pkRowMap.put(newKeyHash, new SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>(row,
					referenceQueue));
				clearAndCheckCache();
			}
		}
	}

	void clearAndCheckCache()
	{
		if (referenceQueue.poll() != null)
		{
			// quicly clear the whole queue, so that it is empty for the next time.
			while (referenceQueue.poll() != null)
			{
			}

			// test the hashmap for empty  Softreferences
			Iterator<Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>>> it = pkRowMap.entrySet().iterator();
			while (it.hasNext())
			{
				Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>> entry = it.next();
				SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> value = entry.getValue();
				if (value == null || (value.get() == null && canRemove(value)))
				{
					removeRowReferences(entry.getKey(), null);
					it.remove();
				}
			}
		}
	}

	int getRowCount()
	{
		return pkRowMap.size();
	}

	/**
	 * Returns the sheet.
	 *
	 * @return SQLSheet
	 */
	SQLSheet getSQLSheet()
	{
		return sheet;
	}

	void deleteRow(IRowListener src, Row r, boolean tracking, boolean partOfBiggerDelete) throws ServoyException
	{
		if (r.getRowManager() != this) throw new IllegalArgumentException("I'm not the row manager from row"); //$NON-NLS-1$

		r.flagExistInDB();//prevent it processed by any update, changed is false now
		if (!partOfBiggerDelete)
		{
			QueryDelete sqlDelete = AbstractBaseQuery.deepClone((QueryDelete)sheet.getSQLDescription(SQLSheet.DELETE).getSQLQuery());
			Object[] pk = r.getPK();
			if (!sqlDelete.setPlaceholderValue(new TablePlaceholderKey(sqlDelete.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY), pk))
			{
				Debug.error(new RuntimeException(
					"Could not set placeholder " + new TablePlaceholderKey(sqlDelete.getTable(), SQLGenerator.PLACEHOLDER_PRIMARY_KEY) + //$NON-NLS-1$
						" in query " + sqlDelete + "-- continuing")); //$NON-NLS-1$ //$NON-NLS-2$
			}

			IDataSet pks = new BufferedDataSet();
			pks.addRow(pk);
			ISQLStatement[] stats_a = new ISQLStatement[1];
			String tid = null;
			GlobalTransaction gt = fsm.getGlobalTransaction();
			if (gt != null)
			{
				tid = gt.getTransactionID(sheet.getServerName());
			}

			SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, sheet.getServerName(), sheet.getTable().getName(), pks, tid, sqlDelete,
				fsm.getTableFilterParams(sheet.getServerName(), sqlDelete));
			statement.setExpectedUpdateCount(1); // check that 1 record is deleted
			stats_a[0] = statement;
			if (tracking)
			{
				statement.setTrackingData(sheet.getColumnNames(), r.getRawColumnData() != null ? new Object[][] { r.getRawColumnData() } : null, null,
					fsm.getApplication().getUserUID(), fsm.getTrackingInfo(), fsm.getApplication().getClientID());
			}

			try
			{
				Object[] results = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), stats_a);
				for (int i = 0; results != null && i < results.length; i++)
				{
					if (results[i] instanceof ServoyException)
					{
						throw (ServoyException)results[i];
					}
				}
			}
			catch (RemoteException e)
			{
				throw new RepositoryException(e);
			}

			SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> removed;
			synchronized (this)
			{
				removed = pkRowMap.remove(r.getPKHashKey());
			}
			fireDependingCalcs(removed, null, null);
		}
		else
		{
			synchronized (this)
			{
				pkRowMap.remove(r.getPKHashKey());
			}
		}
		fireNotifyChange(src, r, null, RowEvent.DELETE);
	}

	void clearRow(Row r)
	{
		if (r != null)
		{
			synchronized (this)
			{
				pkRowMap.remove(r.getPKHashKey());
			}
		}
	}

	boolean lockedByMyself(Row r)
	{
		return lockedRowPKs.contains(new NamedLock(r.getPKHashKey(), null)); // searches on lock, not on name
	}

	public void addLocks(Set<Object> pkhashkeys, String lockName)
	{
		for (Object lock : pkhashkeys)
		{
			lockedRowPKs.add(new NamedLock(lock, lockName));
		}
	}

	public boolean acquireLock(String client_id, QuerySelect lockSelect, String lockName, Set<Object> ids)
	{
		try
		{
			String transaction_id = null;
			GlobalTransaction gt = fsm.getGlobalTransaction();
			if (gt != null)
			{
				transaction_id = gt.getTransactionID(sheet.getServerName());
			}
			IDataSet dataset = fsm.getApplication().getDataServer().acquireLocks(client_id, sheet.getServerName(), sheet.getTable().getName(), ids, lockSelect,
				transaction_id, getFoundsetManager().getTableFilterParams(sheet.getServerName(), lockSelect), getFoundsetManager().chunkSize);
			if (dataset != null)
			{
				addLocks(ids, lockName);
				for (int i = 0; i < dataset.getRowCount(); i++)
				{
					Object[] data = dataset.getRow(i);
					Row rowData = getRowBasedonPKFromEntireColumnArray(data);
					Object[] currentData = rowData.getRawColumnData();
					if (!Utils.equalObjects(data, currentData))
					{
						rowData.setRollbackData(data, Row.ROLLBACK_MODE.UPDATE_CHANGES);
						fireNotifyChange(null, rowData, null, RowEvent.UPDATE);
					}
				}
				return true;
			}
		}
		catch (Exception e)
		{
			Debug.error(e);//TODO:put error code in app
		}
		return false;
	}

	//return true if was forced remove
	public void removeLocks(Set<Object> pkhashkeys)
	{
		for (Object lock : pkhashkeys)
		{
			lockedRowPKs.remove(new NamedLock(lock, null));// searches on lock, not on name
		}
	}

	public Set<Object> getOwnLocks(String lockName)
	{
		Set<Object> locks = new HashSet<Object>(lockedRowPKs.size());
		for (NamedLock namedLock : lockedRowPKs)
		{
			if (lockName == null || lockName.equals(namedLock.name))
			{
				locks.add(namedLock.lock);
			}
		}
		return locks;
	}

	public boolean hasOwnLocks(String lockName)
	{
		for (NamedLock namedLock : lockedRowPKs)
		{
			if (lockName == null || lockName.equals(namedLock.name))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Flushes all cached rows which are not edited.
	 */
	synchronized void flushAllCachedRows()
	{
		//expensive but safe
		@SuppressWarnings("unchecked")
		Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>>[] array = pkRowMap.entrySet().toArray(
			new Entry[pkRowMap.size()]);
		for (Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>> entry : array)
		{
			SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> ref = entry.getValue();
			Row row = ref.get();
			if (row == null || !row.isChanged())
			{
				if (canRemove(ref))
				{
					removeRowReferences(entry.getKey(), null);
					pkRowMap.remove(entry.getKey());
				}
				else
				{
					ref.clear();
				}
			}
		}
	}

	/**
	 * @param row
	 * @return
	 */
	Blob getBlob(Row row, int columnIndex) throws Exception
	{
		QuerySelect blobSelect = new QuerySelect(new QueryTable(sheet.getTable().getSQLName(), sheet.getTable().getDataSource(), sheet.getTable().getCatalog(),
			sheet.getTable().getSchema()));

		String blobColumnName = sheet.getColumnNames()[columnIndex];
		Column blobColumn = sheet.getTable().getColumn(blobColumnName);
		blobSelect.addColumn(new QueryColumn(blobSelect.getTable(), blobColumn.getID(), blobColumn.getSQLName(), blobColumn.getType(), blobColumn.getLength(),
			blobColumn.getScale(), blobColumn.getFlags(), false));

		String[] pkColumnNames = sheet.getPKColumnDataProvidersAsArray();
		IQuerySelectValue[] pkQuerycolumns = new IQuerySelectValue[pkColumnNames.length];

		Object[][] pkValues = new Object[pkColumnNames.length][];
		Object[] pk = row.getPK();

		for (int k = 0; k < pkValues.length; k++)
		{
			Column pkcolumn = sheet.getTable().getColumn(pkColumnNames[k]);
			pkQuerycolumns[k] = new QueryColumn(blobSelect.getTable(), pkcolumn.getID(), pkcolumn.getSQLName(), pkcolumn.getType(), pkcolumn.getLength(),
				pkcolumn.getScale(), pkcolumn.getFlags(), false);
			pkValues[k] = new Object[] { pk[k] };
		}

		blobSelect.addCondition("blobselect", new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, pkQuerycolumns, pkValues, true)); //$NON-NLS-1$

		String serverName = sheet.getServerName();
		String transaction_id = null;
		GlobalTransaction gt = fsm.getGlobalTransaction();
		if (gt != null)
		{
			transaction_id = gt.getTransactionID(sheet.getServerName());
		}
		return fsm.getApplication().getDataServer().getBlob(fsm.getApplication().getClientID(), serverName, blobSelect,
			fsm.getTableFilterParams(sheet.getServerName(), blobSelect), transaction_id);
	}

	/**
	 * Value changed, clear depending calcs
	 */
	public void valueChanged(ModificationEvent e)
	{
		Collection<String> calcs = null;
		if (e.getSource() instanceof GlobalScope)
		{
			// global changed
			synchronized (globalCalcDependencies)
			{
				Set<String> calcSet = globalCalcDependencies.get(e.getName());
				if (calcSet != null)
				{
					calcs = new ArrayList<String>(calcSet);
				}
			}
		}
		else if (e.getSource() instanceof IFoundSet)
		{
			// aggregate changed
			synchronized (aggregateCalcDependencies)
			{
				Map<String, Set<String>> dependingAggregateCalcs = aggregateCalcDependencies.get(((IFoundSet)e.getSource()).getDataSource());
				if (dependingAggregateCalcs != null)
				{
					if (e.getName() == null)
					{
						// all aggregates
						Set<String> dependingCalcs = new HashSet<String>();
						for (Set<String> set : dependingAggregateCalcs.values())
						{
							dependingCalcs.addAll(set);
						}
						calcs = dependingCalcs;
					}
					else
					{
						// named aggregate
						Set<String> dependingCalcs = dependingAggregateCalcs.get(e.getName());
						if (dependingCalcs != null)
						{
							calcs = new ArrayList<String>(dependingCalcs);
						}
					}
				}
			}
		}

		clearCalcs(null, calcs);
	}

	/*
	 * Called from FoundsetManager GlobalFoundSetEventListener
	 */
	public void foundSetChanged(FoundSetEvent e)
	{
		IFoundSet sourceFoundset = e.getSourceFoundset();

		// only act on new foundsets or size changes for related foundsets
		if (e.getType() == FoundSetEvent.NEW_FOUNDSET ||
			(e.getType() == FoundSetEvent.CONTENTS_CHANGED && (e.getChangeType() == FoundSetEvent.CHANGE_INSERT ||
			e.getChangeType() == FoundSetEvent.FOUNDSET_INVALIDATED || e.getChangeType() == FoundSetEvent.CHANGE_DELETE)))
		{
			if (sourceFoundset instanceof RelatedFoundSet && !sourceFoundset.isInFindMode())
			{
				String relationName = sourceFoundset.getRelationName();
				// related foundset changed
				List<String> calcs = null;

				// first test if there are calcs that depend on this relation, to filter out relations that are never used in calcs
				synchronized (relationsUsedInCalcs)
				{
					Set<String> calcSet = relationsUsedInCalcs.get(relationName);
					if (calcSet != null)
					{
						calcs = new ArrayList<String>(calcSet);
					}
				}

				if (calcs != null)
				{
					// some calcs depend on a related foundset with this name, search by whereArgs
					String whereArgsHash = ((RelatedFoundSet)sourceFoundset).getWhereArgsHash();
					List<CalculationDependency> calculationDependencies = new ArrayList<CalculationDependency>();

					// go over each row to see if there are calcs depending on the RFS(whereArgs)
					Iterator<Map.Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>>> it = pkRowMap.entrySet().iterator();
					while (it.hasNext())
					{
						Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>> entry = it.next();
						String pkHash = entry.getKey();
						SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = entry.getValue();
						synchronized (sr)
						{
							Row row = sr.get();
							if (row != null)
							{
								Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
								if (data != null)
								{
									CalculationDependencyData calcRowrefs = data.getRight();
									if (calcRowrefs != null)
									{
										for (String calc : calcs)
										{
											List<RelationDependency> deps = calcRowrefs.getRelationDependencies(calc);
											if (deps != null)
											{
												for (RelationDependency dep : deps)
												{
													if (relationName.equals(dep.relationName) && whereArgsHash.equals(dep.whereArgsHash))
													{
														// the calc depends on this related foundset
														calculationDependencies.add(new CalculationDependency(sheet.getTable().getDataSource(), pkHash, calc));
													}
												}
											}
										}
									}
								}
							}
						}
					}

					if (calculationDependencies.size() > 0)
					{
						List<RowFireNotifyChange> fires = new ArrayList<RowFireNotifyChange>();
						for (CalculationDependency dep : calculationDependencies)
						{
							fireCalculationFlagged(dep.pkHashKey, dep.calc, fires);
						}
						if (fires.size() > 0)
						{
							fireRowNotifyChanges(fires);
							fireNotifyChange(null, null, null, RowEvent.UPDATE);
						}
					}
				}
			}
		}
	}

	/**
	 * @param calcs
	 */
	boolean clearingCalcs = false;

	public void clearCalcs(IRowListener source, Collection<String> calcs)
	{
		if (!clearingCalcs && calcs != null && calcs.size() > 0)
		{
			try
			{
				clearingCalcs = true;
				List<RowFireNotifyChange> fires = new ArrayList<RowFireNotifyChange>();
				for (String calc : calcs)
				{
					clearCalc(calc, fires);
				}
				if (fires.size() > 0)
				{
					fireRowNotifyChanges(fires);
					fireNotifyChange(source, null, null, RowEvent.UPDATE);
				}
			}
			finally
			{
				clearingCalcs = false;
			}
		}
	}

	private static void fireRowNotifyChanges(List<RowFireNotifyChange> fires)
	{
		if (fires != null && fires.size() > 0)
		{
			FireCollector collector = FireCollector.getFireCollector();
			try
			{
				Set<RowFireNotifyChange> fired = new HashSet<RowFireNotifyChange>();
				for (RowFireNotifyChange fire : fires)
				{
					if (fired.add(fire))
					{
						fire.row.fireNotifyChange(fire.name, fire.value, collector);
					}
				}
			}
			finally
			{
				collector.done();
			}
		}
	}

	/**
	 * @param dp
	 */
	private synchronized boolean clearCalc(String dp, List<RowFireNotifyChange> fires)
	{
		boolean changed = false;
		Iterator<Map.Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>>> it = pkRowMap.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<String, SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>>> entry = it.next();
			SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = entry.getValue();
			Row row = sr.get();
			if (row != null)
			{
				if (fireCalculationFlagged(row.getPKHashKey(), dp, fires) && !changed) changed = true;
			}
			else if (canRemove(sr))
			{
				removeRowReferences(entry.getKey(), dp);
				it.remove();//was empty remove while we are here anyway...
			}
		}
		return changed;
	}

	public void flagAllRowCalcsForRecalculation(String pkHashKey)
	{
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
		if (sr != null)
		{
			List<String> calcsUptodate = null;
			synchronized (sr)
			{
				Row row = sr.get();
				if (row != null)
				{
					calcsUptodate = row.getCalcsUptodate();
				}
			}
			flagRowCalcsForRecalculation(pkHashKey, calcsUptodate);
		}
	}

	public void flagRowCalcsForRecalculation(String pkHashKey, List<String> calcs)
	{
		if (calcs != null && calcs.size() > 0)
		{
			SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
			if (sr != null)
			{
				for (String calc : calcs)
				{
					flagRowCalcForRecalculation(pkHashKey, calc);
				}
			}
		}
	}

	public boolean flagRowCalcForRecalculation(String pkHashKey, String calc)
	{
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
		if (sr != null)
		{
			Row row = sr.get();
			if (row != null && row.internalFlagCalcForRecalculation(calc))
			{
				// check the calculation dependencies registered for the calc
				removeRowReferences(pkHashKey, calc);
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove references in other RowManagers to this row.
	 * @param pkHashKey
	 * @param calc, null for all
	 */
	public void removeRowReferences(String pkHashKey, String calc)
	{
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
		if (sr != null)
		{
			CalculationDependencyData rowRefs = null;
			synchronized (sr)
			{
				// check the calculation dependencies registered for the calc
				Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
				if (data != null)
				{
					CalculationDependencyData calcRowrefs = data.getRight();
					if (calcRowrefs != null)
					{
						// remove both row refs and relation dependencies
						if (calc == null)
						{
							rowRefs = calcRowrefs;
							data.setRight(null);
						}
						else
						{
							List<RowReference> refs = calcRowrefs.removeReferences(calc);
							if (refs != null)
							{
								rowRefs = new CalculationDependencyData();
								rowRefs.putRowReferences(calc, refs);
							}
						}
					}
				}
			}
			if (rowRefs != null)
			{
				String dataSource = fsm.getDataSource(sheet.getTable());
				for (Entry<String, List<RowReference>> entry : rowRefs.getRowReferencesEntrySet())
				{
					for (RowReference reference : entry.getValue())
					{
						try
						{
							getFoundsetManager().getRowManager(reference.dataSource).removeCalculationDependency(reference.pkHashKey, reference.dataproviderId,
								dataSource, pkHashKey, entry.getKey());
						}
						catch (ServoyException e)
						{
							Debug.log(e);
						}
					}
				}
			}
		}
	}

	public void clearDeleteSet()
	{
		deleteSet = null;
	}

	public boolean addRowToDeleteSet(String pkHashKey)
	{
		if (deleteSet == null)
		{
			deleteSet = new HashSet<String>();
		}
		return deleteSet.add(pkHashKey);
	}

	/**
	 * Wrapper for lock object with name. <br>
	 * Note: equality is only based on lock object so that locks with different names can be found in a set of NamedLocks.
	 */
	private static class NamedLock
	{
		public final Object lock;
		public final String name;

		public NamedLock(Object lock, String name)
		{
			this.lock = lock;
			this.name = name;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((lock == null) ? 0 : lock.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final NamedLock other = (NamedLock)obj;
			if (lock == null)
			{
				if (other.lock != null) return false;
			}
			else if (!lock.equals(other.lock)) return false;
			return true;
		}
	}

	public void addCalculationGlobalDependency(String global, String calc)
	{
		boolean first;
		synchronized (globalCalcDependencies)
		{
			first = globalCalcDependencies.size() == 0;
			Set<String> dependingCalcs = globalCalcDependencies.get(global);
			if (dependingCalcs == null)
			{
				dependingCalcs = new HashSet<String>();
				globalCalcDependencies.put(global, dependingCalcs);
			}
			dependingCalcs.add(calc);
		}
		if (first)
		{
			fsm.getApplication().getScriptEngine().getScopesScope().getModificationSubject().addModificationListener(this);
		}
	}

	public void addCalculationAggregateDependency(IFoundSetInternal foundSet, String aggregateName, String calc)
	{
		foundSet.addAggregateModificationListener(this);
		synchronized (aggregateCalcDependencies)
		{
			String dataSource = foundSet.getDataSource();
			Map<String, Set<String>> dependingAggregateCalcs = aggregateCalcDependencies.get(dataSource);
			if (dependingAggregateCalcs == null)
			{
				dependingAggregateCalcs = new HashMap<String, Set<String>>();
				aggregateCalcDependencies.put(dataSource, dependingAggregateCalcs);
			}
			Set<String> dependingCalcs = dependingAggregateCalcs.get(aggregateName);
			if (dependingCalcs == null)
			{
				dependingCalcs = new HashSet<String>();
				dependingAggregateCalcs.put(aggregateName, dependingCalcs);
			}
			dependingCalcs.add(calc);
		}
	}

	private boolean canRemove(SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr)
	{
		if (sr != null)
		{
			List<CalculationDependency> dependencies = new ArrayList<CalculationDependency>();
			Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = null;
			synchronized (sr)
			{
				data = sr.getData();
				if (data != null)
				{
					Map<String, List<CalculationDependency>> dependencyMap = data.getLeft();
					if (dependencyMap != null)
					{
						for (List<CalculationDependency> lst : dependencyMap.values())
						{
							dependencies.addAll(lst);
						}
					}
					sr.setData(null);
				}
			}
			try
			{
				for (CalculationDependency dep : dependencies)
				{
					RowManager rm;
					try
					{
						rm = fsm.getRowManager(dep.dataSource);
					}
					catch (ServoyException e)
					{
						Debug.error(e);
						return false;
					}
					if (rm != null && rm.getCachedRow(dep.pkHashKey).getLeft() != null)
					{
						return false;
					}
				}
			}
			finally
			{
				synchronized (sr)
				{
					sr.setData(data);
				}
			}
		}
		return true;
	}

	/**
	 * Fire calcs depending on dataproviders.
	 *
	 * @param pkHashKey
	 * @param dataProviderId, null for all
	 */
	public void fireDependingCalcs(String pkHashKey, String dataProviderId, List<RowFireNotifyChange> fires)
	{
		fireDependingCalcs(pkRowMap.get(pkHashKey), dataProviderId, fires);
	}

	/**
	 * Fire calcs depending on dataproviders.
	 *
	 * @param pkHashKey
	 * @param dataProviderId, null for all
	 */
	protected void fireDependingCalcs(SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr,
		String dataProviderId, List<RowFireNotifyChange> fires)
	{
		List<CalculationDependency> deps = null;
		if (sr != null)
		{
			synchronized (sr)
			{
				Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
				if (data != null)
				{
					Map<String, List<CalculationDependency>> dependencyMap = data.getLeft();
					if (dependencyMap != null)
					{
						if (dataProviderId == null)
						{
							deps = new ArrayList<CalculationDependency>();
							for (List<CalculationDependency> lst : dependencyMap.values())
							{
								deps.addAll(lst);
							}
							dependencyMap.clear();
						}
						else
						{
							deps = dependencyMap.remove(dataProviderId);
						}
					}
				}
			}
		}
		if (deps != null && deps.size() > 0)
		{
			List<RowFireNotifyChange> myFires;
			if (fires == null)
			{
				myFires = new ArrayList<RowFireNotifyChange>();
			}
			else
			{
				myFires = fires;
			}
			for (CalculationDependency dep : deps)
			{
				RowManager rm;
				try
				{
					rm = fsm.getRowManager(dep.dataSource);
					if (rm != null)
					{
						rm.fireCalculationFlagged(dep.pkHashKey, dep.calc, myFires);
					}
				}
				catch (ServoyException e)
				{
					Debug.error(e);
				}
			}
			if (fires == null)
			{
				// my own fires list
				fireRowNotifyChanges(myFires);
			}
			// else caller handles fires
		}
	}

	/**
	 * Fire through that the calc has flagged, fire calcs depending on this calc
	 * @param pkHashKey
	 * @param calc
	 * @param fires
	 * @return
	 */
	private boolean fireCalculationFlagged(String pkHashKey, String calc, List<RowFireNotifyChange> fires)
	{
		if (flagRowCalcForRecalculation(pkHashKey, calc))
		{
			Row row = getCachedRow(pkHashKey).getLeft();
			if (row != null)
			{
				List<RowFireNotifyChange> myFires;
				if (fires == null)
				{
					myFires = new ArrayList<RowFireNotifyChange>();
				}
				else
				{
					myFires = fires;
				}
				row.getRowManager().fireDependingCalcs(row.getPKHashKey(), calc, myFires);
				myFires.add(new RowFireNotifyChange(row, null, calc, null));

				if (fires == null)
				{
					// fire now
					fireRowNotifyChanges(myFires);
				}
				// else caller will fire
			}
			return true;
		}
		return false;
	}

	/**
	 * Register a calculation dependency, dependingCalc in row(dependingDataSource, dependingPkHashKey) was calculated using row(pkHashKey) in this datasource.
	 * @param pkHashKey
	 * @param dataproviderId
	 * @param dependingDataSource
	 * @param dependingPkHashKey
	 * @param dependingCalc
	 */
	public void addCalculationDependency(String pkHashKey, String dataproviderId, String dependingDataSource, String dependingPkHashKey, String dependingCalc)
	{
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
		if (sr != null)
		{
			synchronized (sr)
			{
				Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
				if (data == null)
				{
					data = new Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>(null, null);
					sr.setData(data);
				}
				Map<String, List<CalculationDependency>> dependencyMap = data.getLeft();
				if (dependencyMap == null)
				{
					dependencyMap = new HashMap<String, List<CalculationDependency>>();
					data.setLeft(dependencyMap);
				}
				List<CalculationDependency> dependencies = dependencyMap.get(dataproviderId);
				if (dependencies == null)
				{
					dependencies = new ArrayList<CalculationDependency>();
					dependencyMap.put(dataproviderId, dependencies);
				}
				dependencies.add(new CalculationDependency(dependingDataSource, dependingPkHashKey, dependingCalc));
			}

			// inform depending rowmanager as well (including its own)
			try
			{
				getFoundsetManager().getRowManager(dependingDataSource).addCalculationDependencyBackReference(
					new RowReference(fsm.getDataSource(sheet.getTable()), dataproviderId, pkHashKey), dependingPkHashKey, dependingCalc);
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
	}

	/**
	 * Remove a calculation dependency, dependingCalc in row(dependingDataSource, dependingPkHashKey) was calculated using row(pkHashKey) in this datasource.
	 * @param pkHashKey
	 * @param dataproviderId
	 * @param dependingDataSource
	 * @param dependingPkHashKey
	 * @param dependingCalc
	 */
	public void removeCalculationDependency(String pkHashKey, String dataproviderId, String dependingDataSource, String dependingPkHashKey, String dependingCalc)
	{
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
		if (sr != null)
		{
			synchronized (sr)
			{
				Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
				if (data != null)
				{
					Map<String, List<CalculationDependency>> deps = data.getLeft();
					if (deps != null)
					{
						List<CalculationDependency> list = deps.get(dataproviderId);
						if (list != null)
						{
							Iterator<CalculationDependency> iterator = list.iterator();
							while (iterator.hasNext())
							{
								CalculationDependency dep = iterator.next();
								if (dep.pkHashKey.equals(dependingPkHashKey) && dep.calc.equals(dependingCalc))
								{
									iterator.remove();
								}
							}
							if (list.size() == 0)
							{
								deps.remove(dataproviderId);
							}
						}
						if (deps.size() == 0)
						{
							data.setLeft(null);
						}
					}
				}
			}
		}
	}

	/**
	 * Rowmanager(dataSource) keeps a calculation dependency for my calc in row(pkHashKey).
	 * @param dataSource
	 * @param pkHashKey
	 * @param calc
	 */
	private void addCalculationDependencyBackReference(RowReference rowReference, String pkHashKey, String calc)
	{
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(pkHashKey);
		if (sr != null)
		{
			synchronized (sr)
			{
				Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
				if (data == null)
				{
					data = new Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>(null, null);
					sr.setData(data);
				}
				CalculationDependencyData rowRefs = data.getRight();
				if (rowRefs == null)
				{
					rowRefs = new CalculationDependencyData();
					data.setRight(rowRefs);
				}
				rowRefs.addRowReference(calc, rowReference);
			}
		}
	}

	/**
	 * Calculation depends on relation identified by whereArgsHash.
	 * @param whereArgsHash
	 * @param relationName
	 * @param dependingDataSource
	 * @param dependingPkHashKey
	 * @param dependingCalc
	 */
	public void addCalculationRelationDependency(String whereArgsHash, String relationName, String dependingDataSource, String dependingPkHashKey,
		String dependingCalc)
	{
		// keep a global list of relations that some calcs depend on
		boolean first;
		synchronized (relationsUsedInCalcs)
		{
			first = relationsUsedInCalcs.size() == 0;
			Set<String> dependingCalcs = relationsUsedInCalcs.get(relationName);
			if (dependingCalcs == null)
			{
				dependingCalcs = new HashSet<String>();
				relationsUsedInCalcs.put(relationName, dependingCalcs);
			}
			dependingCalcs.add(dependingCalc);
		}

		// add a relation dependency for the calc
		SoftReferenceWithData<Row, Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>> sr = pkRowMap.get(dependingPkHashKey);
		if (sr != null)
		{
			synchronized (sr)
			{
				Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData> data = sr.getData();
				if (data == null)
				{
					data = new Pair<Map<String, List<CalculationDependency>>, CalculationDependencyData>(null, null);
					sr.setData(data);
				}
				CalculationDependencyData rowRefs = data.getRight();
				if (rowRefs == null)
				{
					rowRefs = new CalculationDependencyData();
					data.setRight(rowRefs);
				}
				rowRefs.addRelationDependency(dependingCalc, new RelationDependency(relationName, whereArgsHash));
			}
		}

		if (first)
		{
			// listen for foundset events to all foundsets
			fsm.addGlobalFoundsetEventListener(this);
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "RowManager:" + sheet.getTable().getDataSource(); //$NON-NLS-1$
	}

	public static class CalculationDependency
	{
		public final String dataSource;
		public final String pkHashKey;
		public final String calc;

		public CalculationDependency(String dataSource, String pkHashKey, String calc)
		{
			this.dataSource = dataSource;
			this.pkHashKey = pkHashKey;
			this.calc = calc;
		}

		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("CalculationDependency [calc=").append(calc).append(", dataSource=").append(dataSource).append(", pkHashKey=").append(pkHashKey).append( //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				']');
			return builder.toString();
		}

	}

	public static class RelationDependency
	{
		public final String relationName;
		public final String whereArgsHash;

		public RelationDependency(String relationName, String whereArgsHash)
		{
			this.relationName = relationName;
			this.whereArgsHash = whereArgsHash;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((relationName == null) ? 0 : relationName.hashCode());
			result = prime * result + ((whereArgsHash == null) ? 0 : whereArgsHash.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			RelationDependency other = (RelationDependency)obj;
			if (relationName == null)
			{
				if (other.relationName != null) return false;
			}
			else if (!relationName.equals(other.relationName)) return false;
			if (whereArgsHash == null)
			{
				if (other.whereArgsHash != null) return false;
			}
			else if (!whereArgsHash.equals(other.whereArgsHash)) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "RelationDependency [relationName=" + relationName + ", whereArgsHash=" + whereArgsHash + ']'; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static class RowReference
	{
		public final String dataSource;
		public final String dataproviderId;
		public final String pkHashKey;

		public RowReference(String dataSource, String dataproviderId, String pkHashKey)
		{
			this.dataSource = dataSource;
			this.dataproviderId = dataproviderId;
			this.pkHashKey = pkHashKey;
		}

		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("RowReference [dataSource=").append(dataSource).append(", dataproviderId=").append(dataproviderId).append(", pkHashKey=").append( //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				pkHashKey).append(']');
			return builder.toString();
		}
	}

	public static class RowFireNotifyChange
	{
		public final Row row;
		public final IRowChangeListener skip;
		public final String name;
		public final Object value;

		public RowFireNotifyChange(Row row, IRowChangeListener skip, String name, Object value)
		{
			this.row = row;
			this.skip = skip;
			this.name = name;
			this.value = value;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((row == null) ? 0 : row.hashCode());
			result = prime * result + ((skip == null) ? 0 : skip.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			RowFireNotifyChange other = (RowFireNotifyChange)obj;
			if (name == null)
			{
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			if (row == null)
			{
				if (other.row != null) return false;
			}
			else if (!row.equals(other.row)) return false;
			if (skip == null)
			{
				if (other.skip != null) return false;
			}
			else if (!skip.equals(other.skip)) return false;
			if (value == null)
			{
				if (other.value != null) return false;
			}
			else if (!value.equals(other.value)) return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "RowFireNotifyChange [name=" + name + ", row=" + row + ", skip=" + skip + ", value=" + value + ']'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		}

		/**
		 * Container for what a calculation depends on: which dataproviders in which row (RowReference) and which relations with which whereargs (RelationDependency).
		 * @author rgansevles
		 *
		 */
		public static class CalculationDependencyData
		{
			private Map<String, List<RowReference>> rowReferences;
			private Map<String, List<RelationDependency>> relationDependencies;

			public void addRowReference(String calc, RowReference rowReference)
			{
				if (rowReferences == null)
				{
					rowReferences = new HashMap<String, List<RowReference>>();
				}
				List<RowReference> list = rowReferences.get(calc);
				if (list == null)
				{
					list = new ArrayList<RowReference>();
					rowReferences.put(calc, list);
				}
				list.add(rowReference);
			}

			public void addRelationDependency(String calc, RelationDependency relationDependency)
			{
				if (relationDependencies == null)
				{
					relationDependencies = new HashMap<String, List<RelationDependency>>();
				}
				List<RelationDependency> list = relationDependencies.get(calc);
				if (list == null)
				{
					list = new ArrayList<RelationDependency>();
					relationDependencies.put(calc, list);
				}
				if (!list.contains(relationDependency))
				{
					list.add(relationDependency);
				}
			}

			public void putRowReferences(String calc, List<RowReference> calcRowRefs)
			{
				if (rowReferences == null)
				{
					rowReferences = new HashMap<String, List<RowReference>>();
				}
				rowReferences.put(calc, calcRowRefs);
			}

			/**
			 * Remove both rowReferences and relationDependencies for the calc, return the removed rowReferences
			 * @param calc
			 * @return
			 */
			public List<RowReference> removeReferences(String calc)
			{
				if (relationDependencies != null)
				{
					relationDependencies.remove(calc);
				}
				if (rowReferences == null) return null;
				return rowReferences.remove(calc);
			}

			public Set<Entry<String, List<RowReference>>> getRowReferencesEntrySet()
			{
				if (rowReferences == null) return Collections.<Entry<String, List<RowReference>>> emptySet();
				return rowReferences.entrySet();
			}

			public List<RelationDependency> getRelationDependencies(String calc)
			{
				if (relationDependencies == null) return null;
				return relationDependencies.get(calc);
			}
		}
	}
}
