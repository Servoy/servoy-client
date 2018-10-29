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

import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.util.ServoyException;


/**
 * The public foundset interface
 *
 * <p>
 * NOTE: do not implement this interface, it can change with new Servoy versions if new functionality is needed.
 * Foundset instances should be get through {@link IDatabaseManager}
 * </p>
 *
 * @author jblok
 * @since Servoy 5.0
 */
public interface IFoundSet extends IFoundSetDeprecated, IGlobalValueEntry, Iterable<IRecord>
{
	// don't add costants here, so that subclasses can have constants of there own exposed into scripting
//	public int COLUMNS = IRepository.COLUMNS;
//	public int AGGREGATEVARIABLES = IRepository.AGGREGATEVARIABLES;
//	public int SCRIPTCALCULATIONS = IRepository.SCRIPTCALCULATIONS;

	/**
	 * The dataprovider names
	 *
	 * @param type of dataprovider, see constants in this interface
	 * @return the array of dataproviders
	 */
	public String[] getDataProviderNames(int type);

	/**
	 * The datasource this foundset is build on
	 *
	 * @return the datasource
	 */
	public String getDataSource();

	/**
	 * The relation name if this foundset is a related foundset
	 *
	 * @return the relation name, null if not related
	 */
	public String getRelationName();

	/**
	 * Get the size of present cached row identifiers (primary keys)
	 *
	 * @return the number of rows which can be received via getRecord
	 */
	public int getSize();

	/**
	 * Add a foundset listener
	 *
	 * @param l the listener
	 */
	public void addFoundSetEventListener(IFoundSetEventListener l);

	/**
	 * remove a foundset listener
	 *
	 * @param l the listener
	 */
	public void removeFoundSetEventListener(IFoundSetEventListener l);

	/**
	 * Get a record object, getting the last of current size might trigger a load of more records if more present
	 *
	 * @param row the index to retrieve
	 * @return the record Object
	 */
	public IRecord getRecord(int row);

	/**
	 * Get multiple record Objects at once
	 *
	 * @param startrow the index to start from
	 * @param count the number of records object to get
	 * @return array of records objects
	 */
	public IRecord[] getRecords(int startrow, int count);

	/**
	 * Iterates over all records of the foundset. The callback method will be called for each record.
	 * This method takes into account inserts and deletes that may occur at same time. It will dynamically load records in the foundset.
	 * If callback returns a value, the traversal will be stopped and function will return that value.
	 * During traversal, foundset modifications (like sort, load records...) cannot be performed, otherwise it will throw an exception.
	 * The same exception will happen if a rollback happens on the foundsets datasource. Because that will also generate a full refesh/reload of the foundset.
	 *
	 * @param callback Callback interface to be called for each record.
	 *
	 * @return Object will return what callback returns
	 */
	public Object forEach(IRecordCallback callback);

	/**
	 * Get the index of a record object inside a foundset
	 *
	 * @param record the records object
	 * @return the index or -1 if not present (anymore)
	 */
	public int getRecordIndex(IRecord record);

	/**
	 * Check if a records is editable
	 *
	 * @param row the index
	 * @return true if editable
	 */
	public boolean isRecordEditable(int row);

	/**
	 * Returns true if this foundset is in find mode and false otherwise.
	 *
	 * @return foundset's find mode.
	 */
	public boolean isInFindMode();

	/**
	 *  Set the foundset in find mode. (Start a find request), use the "search" function to perform/exit the find.
	 *
	 * @return false if this foundset didn't go into find mode.
	 */
	public boolean find();

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 * Clear results from previous searches.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 *
	 * @return the recordCount
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSet#find()
	 */
	public int search() throws Exception;

	/**
	 * Load the initial records inside (back into) this foundset from datasource
	 */
	public void loadAllRecords() throws ServoyException;

	/**
	 * Remove the records from memory
	 */
	public void clear();

	/**
	 * Delete a record from foundset and datasource
	 *
	 * @param row
	 * @throws ServoyException
	 */
	public void deleteRecord(int row) throws ServoyException;

	/**
	 * Delete all records from foundset and datasource
	 *
	 * @throws ServoyException
	 */
	public void deleteAllRecords() throws ServoyException;

	/**
	 * Create a new Record
	 *
	 * @param indexToAdd the index to place the record, Integer.MAX_VALUE means bottom
	 * @param changeSelection change the selection to this new record
	 * @return the index of the new record
	 * @throws ServoyException
	 */
	public int newRecord(int indexToAdd, boolean changeSelection) throws ServoyException;

	/**
	 * Duplicate a record
	 *
	 * @param recordIndex to duplicate
	 * @param indexToAdd the index to place the record, Integer.MAX_VALUE means bottom
	 * @return the index of the new record
	 * @throws ServoyException
	 */
	public int duplicateRecord(int recordIndex, int indexToAdd) throws ServoyException;

	/**
	 * Get the selected index
	 *
	 * @return the index
	 */
	public int getSelectedIndex();

	/**
	 * Set the selected index
	 *
	 * @param selectedRow the index
	 */
	public void setSelectedIndex(int selectedRow);

	/**
	 * Puts this foundset in multi-select or single-select mode. If this foundset is shown in a form, this call can be
	 * ignored as the form decides the foundset's multiselect.
	 * @param multiSelect sets multiselect mode if true and single select mode if false
	 */
	public void setMultiSelect(boolean multiSelect);

	/**
	 * Returns true if this foundset is in multiselect mode and false if it's in single-select mode.
	 * @return true if this foundset is in multiselect mode and false if it's in single-select mode.
	 */
	public boolean isMultiSelect();

	/**
	 * Set the selected indexes of this foundset (in case multiselect is used).
	 * @param indexes the indexes that will be selected in this foundset.
	 */
	public void setSelectedIndexes(int[] indexes);

	/**
	 * Returns the selected indexes of this foundset (in case multiselect is used).
	 * @return the selected indexes of this foundset (in case multiselect is used).
	 */
	public int[] getSelectedIndexes();

	/**
	 * Makes a copy/clone of the foundset
	 *
	 * @param unrelate If true then it wil unrelated this foundset, detach it from its relation
	 *
	 * @return The cloned foundset.
	 *
	 * @throws ServoyException
	 */
	public IFoundSet copy(boolean unrelate) throws ServoyException;


	/**
	 * Returns the current sort string of this Foundset like "column1 asc, column2 desc"
	 *
	 * @return The sort string.
	 */
	public String getSort();

	/**
	 * Sets the sort string of the Foundset can be something like "column 1 asc, column2 desc"
	 * Will sort the Foundset immediately when the Foundset is not in find mode.
	 *
	 * @param sortString
	 */
	public void setSort(String sortString) throws ServoyException;

	/**
	 * Get the query for this foundset.
	 * @since 6.1
	 */
	public IQueryBuilder getQuery();

	/**
	 * Set the PK query for this foundset.
	 * @since 6.1
	 */
	public boolean loadByQuery(IQueryBuilder query) throws ServoyException;
}