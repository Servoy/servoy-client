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
package com.servoy.base.scripting.api;

import com.servoy.base.scripting.annotations.ServoyClientSupport;

/**
 *
 * @author jcompagner
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public interface IJSRecord
{


	/**
	 * Returns true or false if the record is being edited or not.
	 * This does not have the mean that it has changes, the record is just in edit mode, accepting changes.
	 *
	 * @sample
	 * var isEditing = foundset.getSelectedRecord().isEditing() // also foundset.getRecord can be used
	 *
	 * @return a boolean when in edit.
	 */
	public boolean isEditing();

	/**
	 * Returns true if the current record is a new record or false otherwise.
	 *
	 * @sample
	 * var isNew = foundset.getSelectedRecord().isNew();
	 *
	 * @return true if the current record is a new record, false otherwise;
	 */
	public boolean isNew();

	/**
	 * Returns an array with the primary key values of the record.
	 *
	 * @sample
	 * var pks = foundset.getSelectedRecord().getPKs() // also foundset.getRecord can be used
	 *
	 * @return an Array with the pk values.
	 */
	public Object[] getPKs();

	/**
	 * Reverts the in memory outstanding (not saved) changes of the record.
	 *
	 *
	 * @sample
	 * var record= %%prefix%%foundset.getSelectedRecord();
	 * record.revertChanges();
	 */
	public void revertChanges();

	/**
	 * Returns a JSDataSet with outstanding (not saved) changed data of this record.
	 * column1 is the column name, colum2 is the old data and column3 is the new data.
	 *
	 * NOTE: To return an array of records with outstanding changed data, see the function databaseManager.getEditedRecords().
	 *
	 * @sample
	 * /** @type {JSDataSet} *&#47;
	 * var dataset = record.getChangedData()
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	application.output(dataset.getValue(i,1) +' '+ dataset.getValue(i,2) +' '+ dataset.getValue(i,3));
	 * }
	 *
	 * @return a JSDataSet with the changed data of this record.
	 */
//	public IJSDataSet getChangedData();

	/**
	 * Returns true if the current record has outstanding/changed data.
	 *
	 * @sample
	 * var hasChanged = record.hasChangedData();
	 *
	 * @return true if the current record has outstanding/changed data.
	 */
//	public boolean hasChangedData();

	/**
	 * Returns last occurred exception on this record (or null).
	 *
	 * @sample
	 * var exception = record.exception;
	 *
	 * @return The occurred exception.
	 */
//	public Exception getException();

	/**
	 * Returns the records datasource string.
	 *
	 * @sample
	 * var ds = record.getDataSource();
	 *
	 * @return The datasource string of this record.
	 */
	public String getDataSource();


	/**
	 * Returns parent foundset of the record.
	 *
	 * @sample
	 * var parent = record.foundset;
	 *
	 * @return The parent foundset of the record.
	 */
	public IJSFoundSet getFoundset();
}
