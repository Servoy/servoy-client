/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.scripting.api;

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 * @author jcompagner
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IJSDatabaseManager
{
	/**
	 * Saves all outstanding (unsaved) data and exits the current record. 
	 * Optionally, by specifying a record or foundset, can save a single record or all reacords from foundset instead of all the data.
	 * 
	 * NOTE: The fields focus may be lost in user interface in order to determine the edits. 
	 * 
	 * @sample
	 * databaseManager.saveData();
	 * //databaseManager.saveData(foundset.getRecord(1));//save specific record
	 * //databaseManager.saveData(foundset);//save all records from foundset
	 *
	 * // when creating many records in a loop do a batch save on an interval as every 10 records (to save on memory and roundtrips)
	 * // for (var recordIndex = 1; recordIndex <= 5000; recordIndex++) 
	 * // {
	 * //		currentcontroller.newRecord();
	 * //		someColumn = recordIndex;
	 * //		anotherColumn = "Index is: " + recordIndex;
	 * //		if (recordIndex % 10 == 0) databaseManager.saveData();
	 * // }
	 * 
	 * @return true if the save was done without an error.
	 */
	public boolean saveData() throws Exception;


	/**
	 * Set autosave, if false then no saves will happen by the ui (not including deletes!). 
	 * Until you call databaseManager.saveData() or setAutoSave(true)
	 * 
	 * If you also want to be able to rollback deletes then you have to use databaseManager.startTransaction().
	 * Because even if autosave is false deletes of records will be done. 
	 *
	 * @sample
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 * 
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * databaseManager.setAutoSave(true)
	 *
	 * @param autoSave Boolean to enable or disable autosave.
	 * 
	 * @return false if the current edited record could not be saved.
	 */
//	public boolean setAutoSave(boolean autoSave);

	/**
	 * Returns true or false if autosave is enabled or disabled.
	 *
	 * @sample
	 * //Set autosave, if false then no saves will happen by the ui (not including deletes!). Until you call saveData or setAutoSave(true)
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 * 
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * databaseManager.setAutoSave(true)
	 * 
	 * @return true if autosave if enabled.
	 */
//	public boolean getAutoSave();

	/**
	 * Returns the table name from the datasource, or null if not a database datasource.
	 *
	 * @sample var tablename = databaseManager.getDataSourceTableName(datasource);
	 *
	 * @param dataSource The datasource string to get the tablename from.
	 * 
	 * @return The tablename of the datasource.
	 */
	public String getDataSourceTableName(String dataSource);

	/**
	 * Returns the server name from the datasource, or null if not a database datasource.
	 *
	 * @sample var servername = databaseManager.getDataSourceServerName(datasource);
	 *
	 * @param dataSource The datasource string to get the server name from.
	 * 
	 * @return The servername of the datasource.
	 */
	public String getDataSourceServerName(String dataSource);

	/**
	 * Returns the datasource corresponding to the given server/table.
	 *
	 * @sample var datasource = databaseManager.getDataSource('example_data', 'categories');
	 *
	 * @param serverName The name of the table's server.
	 * @param tableName The table's name.
	 * 
	 * @return The datasource of the given table/server.
	 */
	public String getDataSource(String serverName, String tableName);


	/**
	 * Returns true if the (related)foundset exists and has records.
	 *
	 * @sample
	 * if (%%elementName%%.hasRecords(orders_to_orderitems))
	 * {
	 * 	//do work on relatedFoundSet
	 * }
	 * //if (%%elementName%%.hasRecords(foundset.getSelectedRecord(),'orders_to_orderitems.orderitems_to_products'))
	 * //{
	 * //	//do work on deeper relatedFoundSet
	 * //}
	 *
	 * @param foundset A JSFoundset to test. 
	 * 
	 * @return true if the foundset/relation has records.
	 */
	public boolean hasRecords(IJSFoundSet foundset);

	/**
	 * @clonedesc hasRecords(IJSFoundSet)
	 * 
	 * @sampleas hasRecords(IJSFoundSet)
	 * 
	 * @param record A JSRecord to test.
	 * @param relationString The relation name.
	 *
	 * @return true if the foundset/relation has records.
	 */
	public boolean hasRecords(IJSRecord record, String relationString);

	/**
	 * Returns a foundset object for a specified datasource or server and tablename. 
	 *
	 * @sample
	 * // type the foundset returned from the call with JSDoc, fill in the right server/tablename
	 * /** @type {JSFoundset<db:/servername/tablename>} *&#47;
	 * var fs = databaseManager.getFoundSet(controller.getDataSource())
	 * var ridx = fs.newRecord()
	 * var record = fs.getRecord(ridx)
	 * record.emp_name = 'John'
	 * databaseManager.saveData()
	 *
	 * @param dataSource The datasource to get a JSFoundset for.
	 * 
	 * @return A new JSFoundset for that datasource.
	 */
	public IJSFoundSet getFoundSet(String dataSource) throws Exception;
}
