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
	public int getSize();


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
	 * @param index the index on which place the record should go
	 * @param changeSelection wheter or not the selection should change.
	 *
	 * @return int index of new record.
	 */
	public int js_newRecord(Number index, Boolean changeSelection) throws Exception;


	/**
	 * Sorts the foundset based on the given record comparator function.
	 * The comparator function is called to compare
	 * two records, that are passed as arguments, and
	 * it will return -1/0/1 if the first record is less/equal/greater
	 * then the second record.
	 *
	 * The function based sorting does not work with printing.
	 * It is just a temporary in-memory sort.
	 *
	 * @sample
	 * %%prefix%%foundset.sort(mySortFunction);
	 *
	 * function mySortFunction(r1, r2)
	 * {
	 *	var o = 0;
	 *	if(r1.id < r2.id)
	 *	{
	 *		o = -1;
	 *	}
	 *	else if(r1.id > r2.id)
	 *	{
	 *		o = 1;
	 *	}
	 *	return o;
	 * }
	 *
	 * @param recordComparisonFunction record comparator function
	 */
	public void sort(final Object recordComparisonFunction);

	/**
	 * Delete record with the given index.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord(4);
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @param index The index of the record to delete.
	 *
	 * @return boolean true if record could be deleted.
	 */
	public boolean js_deleteRecord(Number index) throws Exception;

	/**
	 * Delete record from foundset.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord(rec);
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @param record The record to delete from the foundset.
	 *
	 * @return boolean true if record could be deleted.
	 */
	public boolean js_deleteRecord(IJSRecord record) throws Exception;

	/**
	 * Delete currently selected record(s).
	 * If the foundset is in multiselect mode, all selected records are deleted.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteRecord();
	 * //can return false incase of related foundset having records and orphans records are not allowed by the relation
	 *
	 * @return boolean true if all records could be deleted.
	 */
	public boolean js_deleteRecord() throws Exception;

	/**
	 * Delete all records in foundset, resulting in empty foundset.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteAllRecords();
	 * @return boolean true if all records could be deleted.
	 */
	public boolean js_deleteAllRecords() throws Exception;

	/**
	 * Set the foundset in find mode. (Start a find request), use the "search" function to perform/exit the find.
	 *
	 * Before going into find mode, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off), the foundset will not go into find mode.
	 * Make sure the operator and the data (value) are part of the string passed to dataprovider (included inside a pair of quotation marks).
	 * Note: always make sure to check the result of the find() method.
	 *
	 * When in find mode, columns can be assigned string expressions (including operators) that are evaluated as:
	 * General:
	 *       c1||c2    (condition1 or condition2)
	 *       c|format  (apply format on condition like 'x|dd-MM-yyyy')
	 *       !c        (not condition)
	 *       #c        (modify condition, depends on column type)
	 *       ^         (is null)
	 *       ^=        (is null or empty)
	 *       &lt;x     (less than value x)
	 *       &gt;x     (greater than value x)
	 *       &lt;=x    (less than or equals value x)
	 *       &gt;=x    (greater than or equals value x)
	 *       x...y     (between values x and y, including values)
	 *       x         (equals value x)
	 *
	 *  Number fields:
	 *       =x       (equals value x)
	 *       ^=       (is null or zero)
	 *
	 *  Date fields:
	 *       #c       (equals value x, entire day)
	 *       now      (equals now, date and or time)
	 *       //       (equals today)
	 *       today    (equals today)
	 *
	 *  Text fields:
	 *       #c	        (case insensitive condition)
	 *       = x      (equals a space and 'x')
	 *       ^=       (is null or empty)
	 *       %x%      (contains 'x')
	 *       %x_y%    (contains 'x' followed by any char and 'y')
	 *       \%      (contains char '%')
	 *       \_      (contains char '_')
	 *
	 * Related columns can be assigned, they will result in related searches.
	 * For example, "employees_to_department.location_id = headoffice" finds all employees in the specified location).
	 *
	 * Searching on related aggregates is supported.
	 * For example, "orders_to_details.total_amount = '&gt;1000'" finds all orders with total order details amount more than 1000.
	 *
	 * Arrays can be used for searching a number of values, this will result in an 'IN' condition that will be used in the search.
	 * The values are not restricted to strings but can be any type that matches the column type.
	 * For example, "record.department_id = [1, 33, 99]"
	 *
	 * @sample
	 * if (%%prefix%%foundset.find()) //find will fail if autosave is disabled and there are unsaved records
	 * {
	 * 	columnTextDataProvider = 'a search value'
	 * 	// for numbers you have to make sure to format it correctly so that the decimal point is in your locales notation (. or ,)
	 * 	columnNumberDataProvider = '>' + utils.numberFormat(anumber, '####.00');
	 * 	columnDateDataProvider = '31-12-2010|dd-MM-yyyy'
	 * 	%%prefix%%foundset.search()
	 * }
	 *
	 * @return true if the foundset is now in find mode, false otherwise.
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_search(Boolean, Boolean)
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_setAutoSave(boolean)
	 * @see com.servoy.j2db.BasicFormController$JSForm#js_find()
	 * @see com.servoy.j2db.BasicFormController$JSForm#js_search(Boolean, Boolean)
	 */
	public boolean find();

	/**
	 * Check if this foundset is in find mode.
	 *
	 * @sample
	 * //Returns true when find was called on this foundset and search has not been called yet
	 * %%prefix%%foundset.isInFind();
	 *
	 * @return boolean is in find mode.
	 */
	public boolean isInFind();

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 * Clear results from previous searches.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 *
	 * @return the recordCount
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#find()
	 */
	public int search() throws Exception;

	/**
	 * Loads all accessible records from the datasource into the foundset.
	 * Filters on the foundset are applied.
	 *
	 * Before loading the records, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off),
	 * records will not be loaded,
	 *
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_addFoundSetFilterParam(String, String, Object, String)
	 *
	 * @return true if records are loaded, false otherwise.
	 */
	public boolean js_loadAllRecords() throws Exception;

	/**
	 * Get the record index. Will return -1 if the record can't be found.
	 *
	 * @sample var index = %%prefix%%foundset.getRecordIndex(record);
	 *
	 * @param record Record
	 *
	 * @return int index.
	 */
	int js_getRecordIndex(IJSRecord record);

	/**
	 * Get all dataproviders of the foundset.
	 *
	 * @sample
	 * var dataprovidersNames = %%prefix%%alldataproviders;
	 * application.output("This foundset has " + dataprovidersNames.length + " data providers.")
	 * for (var i=0; i<dataprovidersNames.length; i++)
	 * 	application.output(dataprovidersNames[i]);
	 *
	 * @special
	 */
	String[] alldataproviders();

}
