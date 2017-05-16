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

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.SQLSheet.VariableInfo;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;

/**
 * Scriptable Foundsetupdater object
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSFoundSetUpdater implements IReturnedTypesProvider, IJavaScriptType
{
	private IApplication application;
	private FoundSet foundset;
	private int rowsToUpdate = -1;
	private List<Pair<String, Object>> list = new ArrayList<Pair<String, Object>>();
	private int iteratorIndex = 0;

	/*
	 * Don't use purely for method editor
	 */
	public JSFoundSetUpdater()
	{
	}

	JSFoundSetUpdater(IApplication app, FoundSet fs)
	{
		application = app;
		foundset = fs;
		iteratorIndex = foundset.getSelectedIndex();
	}

	private void clear()
	{
		//clear, so it cannot be done again with those values
		list = new ArrayList<Pair<String, Object>>();
		rowsToUpdate = -1;
		currentRecord = null;
	}

	/**
	 * Set the column value to update, returns true if successful.
	 *
	 * @sampleas js_performUpdate()
	 *
	 * @param name The name of the column to update.
	 *
	 * @param value The new value (can be an array with data for x number of rows) to be stored in the specified column.
	 *
	 * @return true if succeeded, false if failed.
	 */
	public boolean js_setColumn(String name, Object value)
	{
		Object val = value;
		if (foundset.getSQLSheet().getColumnIndex(name) >= 0)//only allow columns
		{
			if (currentRecord == null)
			{
				if (val instanceof Object[])
				{
					rowsToUpdate = Math.max(rowsToUpdate, ((Object[])val).length);
				}
				list.add(new Pair<String, Object>(name, val));
				return true;
			}
			else
			{
				try
				{
					if (val instanceof Object[] && ((Object[])val).length != 0)
					{
						val = ((Object[])val)[0];
					}
					if (currentRecord.startEditing())
					{
						currentRecord.setValue(name, val);
						currentRecord.stopEditing();
						return true;
					}
				}
				catch (Exception ex)
				{
					application.reportError(application.getI18NMessage("servoy.foundsetupdater.error.setColumnValue"), ex); //$NON-NLS-1$
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * Do the actual update in the database, returns true if successful. It will first try to save all editing records (from all foundsets), if cannot save will return false before doing the update.
	 * There are 3 types of possible use with the foundset updater
	 * 1) update entire foundset by a single sql statement; that is not possible when the table of the foundset has tracking enabled then it will loop over the whole foundset.
	 *    When a single sql statement is done, modification columns will not be updated and associated Table Events won't be triggered, because it does the update directly in the database, without getting the records.
	 *   NOTE: this mode will refresh all foundsets based on same datasource
	 * 2) update part of foundset, for example the first 4 row (starts with selected row)
	 * 3) safely loop through foundset (starts with selected row)
	 *
	 * after the perform update call there are no records in edit mode, that where not already in edit mode, because all of them are saved directly to the database,
	 * or in mode 1 the records are not touched at all and the database is updated directly.
	 *
	 * @sample
	 * //1) update entire foundset
	 * var fsUpdater = databaseManager.getFoundSetUpdater(foundset)
	 * fsUpdater.setColumn('customer_type',1)
	 * fsUpdater.setColumn('my_flag',0)
	 * fsUpdater.performUpdate()
	 *
	 * //2) update part of foundset, for example the first 4 row (starts with selected row)
	 * var fsUpdater = databaseManager.getFoundSetUpdater(foundset)
	 * fsUpdater.setColumn('customer_type',new Array(1,2,3,4))
	 * fsUpdater.setColumn('my_flag',new Array(1,0,1,0))
	 * fsUpdater.performUpdate()
	 *
	 * //3) safely loop through foundset (starts with selected row)
	 * controller.setSelectedIndex(1)
	 * var count = 0
	 * var fsUpdater = databaseManager.getFoundSetUpdater(foundset)
	 * while(fsUpdater.next())
	 * {
	 * 	fsUpdater.setColumn('my_flag',count++)
	 * }
	 *
	 * @return true if succeeded, false if failed.
	 */
	public boolean js_performUpdate() throws ServoyException
	{
		if (list.size() == 0 || foundset.getTable() == null)
		{
			return false;
		}

		if (!foundset.hasAccess(IRepository.UPDATE))
		{
			throw new ApplicationException(ServoyException.NO_MODIFY_ACCESS, new Object[] { foundset.getTable().getName() });
		}
		// first stop all edits, 'force' stop the edit by saying that it is a javascript stop
		if (application.getFoundSetManager().getEditRecordList().stopEditing(true) != ISaveConstants.STOPPED) return false;

		try
		{
			QuerySelect sqlParts;
			IDataSet currentPKs;
			synchronized (foundset.getPksAndRecords())
			{
				sqlParts = foundset.getPksAndRecords().getQuerySelectForReading();
				currentPKs = foundset.getPksAndRecords().getPks();
			}
			if (rowsToUpdate == -1 && !foundset.hasAccess(IRepository.TRACKING) && sqlParts.getJoins() == null)//does not have join to other table
			{
				//all rows at once, via sql
				Table table = (Table)foundset.getTable();
				SQLSheet sheet = foundset.getSQLSheet();

				QueryUpdate sqlUpdate = new QueryUpdate(sqlParts.getTable());
				for (int j = 0; j < list.size(); j++)
				{
					Pair<String, Object> p = list.get(j);
					String name = p.getLeft();
					Object val = p.getRight();
					int columnIndex = sheet.getColumnIndex(name);
					VariableInfo variableInfo = sheet.getCalculationOrColumnVariableInfo(name, columnIndex);
					if (val != null && !("".equals(val) && Column.mapToDefaultType(variableInfo.type) == IColumnTypes.TEXT))//do not convert null to 0 incase of numbers, this means the calcs the value whould change each time //$NON-NLS-1$
					{
						val = sheet.convertObjectToValue(name, val, foundset.getFoundSetManager().getColumnConverterManager(),
							foundset.getFoundSetManager().getColumnValidatorManager());
					}
					Column c = table.getColumn(name);
					if (val == null)
					{
						val = ValueFactory.createNullValue(c.getType());
					}

					sqlUpdate.addValue(new QueryColumn(sqlParts.getTable(), c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale(), c.getFlags()),
						val);
				}
				sqlUpdate.setCondition(sqlParts.getWhereClone());

				FoundSetManager fsm = (FoundSetManager)application.getFoundSetManager();
				IDataSet pks;
				boolean allFoundsetRecordsLoaded = currentPKs != null && currentPKs.getRowCount() <= fsm.pkChunkSize && !currentPKs.hadMoreRows();
				if (allFoundsetRecordsLoaded)
				{
					pks = currentPKs;
				}
				else
				{
					pks = new BufferedDataSet();
					pks.addRow(new Object[] { ValueFactory.createTableFlushValue() });
				}

				String transaction_id = fsm.getTransactionID(foundset.getSQLSheet());
				try
				{
					SQLStatement statement = new SQLStatement(ISQLActionTypes.UPDATE_ACTION, table.getServerName(), table.getName(), pks, transaction_id,
						sqlUpdate, fsm.getTableFilterParams(table.getServerName(), sqlUpdate));
					if (allFoundsetRecordsLoaded)
					{
						statement.setExpectedUpdateCount(pks.getRowCount());
					}
					Object[] results = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), new ISQLStatement[] { statement });

					for (int i = 0; results != null && i < results.length; i++)
					{
						if (results[i] instanceof ServoyException)
						{
							if (((ServoyException)results[i]).getErrorCode() == ServoyException.UNEXPECTED_UPDATE_COUNT)
							{
								performLoopUpdate();
								clear();
								return true;
							}
							fsm.flushCachedDatabaseData(fsm.getDataSource(table));
							throw (ServoyException)results[i];
						}
					}

					fsm.flushCachedDatabaseData(fsm.getDataSource(table));
					clear();
					return true;
				}
				catch (ApplicationException aex)
				{
					if (allFoundsetRecordsLoaded || aex.getErrorCode() != ServoyException.RECORD_LOCKED)
					{
						throw aex;
					}
					// a record was locked by another client, try per-record
					Debug.log("foundsetUpdater could not update all records in 1 statement (a record may be locked), trying per-record");
				}
			}

			performLoopUpdate();
		}
		catch (Exception ex)
		{
			application.handleException(application.getI18NMessage("servoy.foundsetupdater.updateFailed"), //$NON-NLS-1$
				new ApplicationException(ServoyException.SAVE_FAILED, ex));
			return false;
		}

		clear();
		return true;
	}

	/**
	 *
	 */
	private void performLoopUpdate()
	{
		//update via loop
		final int[] arrayIndex = new int[] { 0 };
		final int[] startRow = new int[] { rowsToUpdate >= 0 ? foundset.getSelectedIndex() : 0 };
		foundset.forEach(new IRecordCallback()
		{
			@Override
			public Object handleRecord(IRecord record, int recordIndex, IFoundSet foundset)
			{
				if (arrayIndex[0] == rowsToUpdate)
				{
					return Boolean.FALSE;
				}
				if (recordIndex >= startRow[0])
				{
					startRow[0] = 0;
					boolean wasEditing = ((IRecordInternal)record).isEditing();
					if (record.startEditing())
					{
						//update the fields
						for (int j = 0; j < list.size(); j++)
						{
							Pair<String, Object> p = list.get(j);
							String name = p.getLeft();
							Object val = p.getRight();
							if (val instanceof Object[])
							{
								int indx = arrayIndex[0];
								if (arrayIndex[0] >= ((Object[])val).length) indx = ((Object[])val).length - 1;//incase there is a length difference between arrays, repeat last value
								val = ((Object[])val)[indx];
							}
							record.setValue(name, val);
						}
					}
					if (!wasEditing)
					{
						((IRecordInternal)record).stopEditing();
					}
					arrayIndex[0]++;
				}
				return null;
			}
		});
	}

	private IRecordInternal currentRecord;

	/**
	 * Start over with this iterator 'next' function (at the foundset selected record).
	 *
	 * @sample
	 * controller.setSelectedIndex(1)
	 * var count = 0
	 * var fsUpdater = databaseManager.getFoundSetUpdater(foundset)
	 * while(fsUpdater.next())
	 * {
	 * 	fsUpdater.setColumn('my_flag',++count)
	 * }
	 * fsUpdater.resetIterator()
	 * while(fsUpdater.next())
	 * {
	 * 	fsUpdater.setColumn('max_flag',count)
	 * }
	 */
	public void js_resetIterator()
	{
		clear();
		iteratorIndex = foundset.getSelectedIndex();
		currentRecord = null;
	}

	/**
	 * Go to next record in this updater, returns true if successful.
	 * NOTE: this method doesn't take into account deletes and inserts that may happen at same time. For more reliable iterator see foundset.forEach
	 *
	 * @sample
	 * controller.setSelectedIndex(1)
	 * var count = 0
	 * var fsUpdater = databaseManager.getFoundSetUpdater(foundset)
	 * while(fsUpdater.next())
	 * {
	 * 	fsUpdater.setColumn('my_flag',count++)
	 * }
	 *
	 * @return true if proceeded to next record, false otherwise
	 */
	public boolean js_next()
	{
		if (currentRecord != null)
		{
			// not first iteration
			if (iteratorIndex > foundset.getSize() - 2) return false; // is already at the end

			if (currentRecord.getParentFoundSet() != null)//if old record is not longer part of foundset we do not update counter, due to update relation field incase of related foundset or search field
			{
				iteratorIndex++;
			}
		}
		else
		{
			// first iteration; do not increase the iteratorIndex - it must point to the first element after next() is called
			if (foundset.getSize() == 0) return false; // if there are no elements in the foundset => no first iterations
		}
		currentRecord = foundset.getRecord(iteratorIndex);
		return true;
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}
}
