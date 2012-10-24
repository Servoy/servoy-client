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
package com.servoy.j2db.scripting.api;

import com.servoy.j2db.scripting.annotations.ServoyMobile;

/**
 * 
 * @author jcompagner
 * @since 7.0
 */
@ServoyMobile
public interface IJSFoundSet
{
	/**
	 * Set the current record index.
	 *
	 * @sampleas jsFunction_getSelectedIndex()
	 *
	 * @param index int index to set (1-based)
	 */
	public void jsFunction_setSelectedIndex(int index);

	/**
	 * Get the current record index of the foundset.
	 *
	 * @sample
	 * //gets the current record index in the current foundset
	 * var current = %%prefix%%foundset.getSelectedIndex();
	 * //sets the next record in the foundset
	 * %%prefix%%foundset.setSelectedIndex(current+1);	
	 * @return int current index (1-based)
	 */
	public int jsFunction_getSelectedIndex();

	/**
	 * Get the record object at the index. 
	 *
	 * @sample var record = %%prefix%%foundset.getRecord(index);
	 *
	 * @param index int record index
	 * 
	 * @return Record record. 
	 */
	public IJSRecord js_getRecord(int index);

	/**
	 * Get the number of records in this foundset.
	 * This is the number of records loaded, note that when looping over a foundset, size() may
	 * increase as more records are loaded.
	 * 
	 * @sample 
	 * var nrRecords = %%prefix%%foundset.getSize()
	 * 
	 * // to loop over foundset, recalculate size for each record
	 * for (var i = 1; i <= %%prefix%%foundset.getSize(); i++)
	 * {
	 * 	var rec = %%prefix%%foundset.getRecord(i);
	 * }
	 * 
	 * @return int current size.
	 */
	public int js_getSize();


	/**
	 * Get the selected record.
	 *
	 * @sample var selectedRecord = %%prefix%%foundset.getSelectedRecord();
	 * @return Record record. 
	 */
	public IJSRecord getSelectedRecord();

	/**
	 * Create a new record on top of the foundset and change selection to it. Returns -1 if the record can't be made.
	 *
	 * @sample
	 * // foreign key data is only filled in for equals (=) relation items 
	 * var idx = %%prefix%%foundset.newRecord(false); // add as last record
	 * // %%prefix%%foundset.newRecord(); // adds as first record
	 * // %%prefix%%foundset.newRecord(2); //adds as second record
	 * if (idx >= 0) // returned index is -1 in case of failure 
	 * {
	 * 	%%prefix%%foundset.some_column = "some text";
	 * 	application.output("added on position " + idx);
	 * 	// when adding at the end of the foundset, the returned index
	 * 	// corresponds with the size of the foundset
	 * }
	 *
	 * @return int index of new record.
	 */
	public int js_newRecord() throws Exception;
}
