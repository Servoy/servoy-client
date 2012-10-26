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

package com.servoy.j2db.scripting.api;

import com.servoy.j2db.scripting.annotations.ServoyMobile;

/**
 * @author jcompagner
 *
 */
@ServoyMobile
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
	 * @clonedesc saveData()
	 * 
	 * @sampleas saveData()
	 *  
	 * @param foundset The JSFoundset to save.
	
	 * @return true if the save was done without an error.
	 */
	public boolean saveData(IJSFoundSet foundset) throws Exception;

	/**
	 * @clonedesc saveData()
	 * 
	 * @sampleas saveData()
	 *  
	 * @param record The JSRecord to save.
	
	 * @return true if the save was done without an error.
	 */
	public boolean saveData(IJSRecord record) throws Exception;
}
