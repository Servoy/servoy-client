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

package com.servoy.j2db.dataprocessing;

import org.mozilla.javascript.Function;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.api.IJSFoundSet;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;


/**
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSFoundSet")
public interface IJSFoundSetNormal extends IJSFoundSet
{

	/**
	 * Clears the foundset.
	 *
	 * @deprecated As of release 3.1, replaced by {@link #clear()}.
	 */
	@Deprecated
	public abstract void js_clearFoundSet();

	/**
	 * Clear the foundset.
	 *
	 * @sample
	 * //Clear the foundset, including searches that may be on it
	 * %%prefix%%foundset.clear();
	 */
	public abstract void js_clear();

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * Use clear() or loadAllRecords() to make the filter effective.
	 * Multiple filters can be added to the same dataprovider, they will all be applied.
	 *
	 * @sample
	 * // Filter a fondset on a dataprovider value.
	 * // Note that multiple filters can be added to the same dataprovider, they will all be applied.
	 * 
	 * var success = %%prefix%%foundset.addFoundSetFilterParam('customerid', '=', 'BLONP', 'custFilter');//possible to add multiple
	 * %%prefix%%foundset.loadAllRecords();//to make param(s) effective
	 * // Named filters can be removed using %%prefix%%foundset.removeFoundSetFilterParam(filterName)
	 *
	 * @param dataprovider String column to filter on.
	 *
	 * @param operator String operator: =, <, >, >=, <=, !=, (NOT) LIKE, (NOT) IN, (NOT) BETWEEN and IS (NOT) NULL optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null).
	 *
	 * @param value Object filter value (for in array and between an array with 2 elements)
	 *
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public abstract boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value);

	/**
	 * Add a filter parameter that is permanent per user session to limit a specified foundset of records.
	 * Use clear() or loadAllRecords() to make the filter effective.
	 * The filter is removed again using removeFoundSetFilterParam(name).
	 *
	 * @sample
	 * var success = %%prefix%%foundset.addFoundSetFilterParam('customerid', '=', 'BLONP', 'custFilter');//possible to add multiple
	 * // Named filters can be removed using %%prefix%%foundset.removeFoundSetFilterParam(filterName)
	 *
	 * // you can use modifiers in the operator as well, filter on companies where companyname is null or equals-ignore-case 'servoy'
	 * var ok = %%prefix%%foundset.addFoundSetFilterParam('companyname', '#^||=', 'servoy')
	 *
	 * %%prefix%%foundset.loadAllRecords();//to make param(s) effective
	 *
	 * @param dataprovider String column to filter on.
	 *
	 * @param operator String operator: =, <, >, >=, <=, !=, (NOT) LIKE, (NOT) IN, (NOT) BETWEEN and IS (NOT) NULL optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null). 
	 *
	 * @param value Object filter value (for in array and between an array with 2 elements)
	 *
	 * @param name String name, used to remove the filter again.
	 * 
	 * @return true if adding the filter succeeded, false otherwise.
	 */
	public abstract boolean js_addFoundSetFilterParam(String dataprovider, String operator, Object value, String name);

	/**
	 * Remove a named foundset filter.
	 * Use clear() or loadAllRecords() to make the filter effective.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.removeFoundSetFilterParam('custFilter');// removes all filters with this name
	 * %%prefix%%foundset.loadAllRecords();//to make param(s) effective
	 *
	 * @param name String filter name.
	 * 
	 * @return true if removing the filter succeeded, false otherwise.
	 */
	public abstract boolean js_removeFoundSetFilterParam(String name);

	/**
	 * Get a previously defined foundset filter, using its given name.
	 * The result is an array of:
	 *  [ tableName, dataprovider, operator, value, name ]
	 *
	 * @sample
	 * var params = foundset.getFoundSetFilterParams()
	 * for (var i = 0; params != null && i < params.length; i++)
	 * {
	 * 	application.output('FoundSet filter on table ' + params[i][0]+ ': '+ params[i][1]+ ' '+params[i][2]+ ' '+params[i][3] +(params[i][4] == null ? ' [no name]' : ' ['+params[i][4]+']'))
	 * }
	 *
	 * @param filterName name of the filter to retrieve.
	 * 
	 * @return Array of filter definitions.
	 */
	public abstract Object[][] js_getFoundSetFilterParams(String filterName);

	/**
	 * Get the list of previously defined foundset filters.
	 * The result is an array of:
	 *  [ tableName, dataprovider, operator, value, name ]
	 *
	 * @sample
	 * var params = foundset.getFoundSetFilterParams()
	 * for (var i = 0; params != null && i < params.length; i++)
	 * {
	 * 	application.output('FoundSet filter on table ' + params[i][0]+ ': '+ params[i][1]+ ' '+params[i][2]+ ' '+params[i][3] +(params[i][4] == null ? ' [no name]' : ' ['+params[i][4]+']'))
	 * }
	 *
	 * @return Array of filter definitions.
	 */
	public abstract Object[][] js_getFoundSetFilterParams();

	/**
		 * Get a duplicate of the foundset.
		 *
		 * @sample
		 * var dupFoundset = %%prefix%%foundset.duplicateFoundSet();
		 * %%prefix%%foundset.find();
		 * //search some fields
		 * var count = %%prefix%%foundset.search();
		 * if (count == 0)
		 * {
		 * 	plugins.dialogs.showWarningDialog('Alert', 'No records found','OK');
		 * 	%%prefix%%foundset.loadRecords(dupFoundset);
		 * }
		 * 
		 * @return foundset duplicate.
		 */
	public abstract FoundSet js_duplicateFoundSet() throws ServoyException//can be used by loadRecords Again
	;

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
	 * @see com.servoy.j2db.FormController$JSForm#js_find()
	 * @see com.servoy.j2db.FormController$JSForm#js_search(Boolean, Boolean)
	 */
	public abstract boolean js_find();

	/**
	 * Check if this foundset is in find mode.
	 *
	 * @sample
	 * //Returns true when find was called on this foundset and search has not been called yet
	 * %%prefix%%foundset.isInFind();
	 * 
	 * @return boolean is in find mode.
	 */
	public abstract boolean js_isInFind();

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 * Clear results from previous searches.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 * 
	 * @sampleas js_search(Boolean, Boolean)
	 *
	 * @return the recordCount
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_find()
	 */
	public abstract int js_search() throws ServoyException;

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 * Reduce results from previous searches.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 * 
	 * @sampleas js_search(Boolean, Boolean)
	 *
	 * @param clearLastResults boolean, clear previous search, default true  
	 * 
	 * @return the recordCount
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_find()
	 */
	public abstract int js_search(Boolean clearLastResults) throws ServoyException;

	/**
	 * Start the database search and use the results, returns the number of records, make sure you did "find" function first.
	 *
	 * Note: Omitted records are automatically excluded when performing a search - meaning that the foundset result by default will not include omitted records.
	 * 
	 * @sample
	 * var recordCount = %%prefix%%foundset.search();
	 * //var recordCount = %%prefix%%foundset.search(false,false); //to extend foundset
	 *
	 * @param clearLastResults boolean, clear previous search, default true  
	 * @param reduceSearch boolean, reduce (true) or extend (false) previous search results, default true
	 * 
	 * @return the recordCount
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_find()
	 */
	public abstract int js_search(Boolean clearLastResults, Boolean reduceSearch) throws ServoyException;

	/** Check wether the foundset has any conditions from a previous find action.
	 * 
	 * @sample
	 * if (%%prefix%%foundset.hasConditions())
	 * {
	 * 		// foundset had find actions
	 * }
	 *
	 * @return wether the foundset has find-conditions
	 */
	public abstract boolean js_hasConditions();

	/**
	 * Gets the name of the table used.
	 *
	 * @deprecated As of release 5.0, replaced by {@link #getDataSource()}.
	 */
	@Deprecated
	public abstract String js_getTableName();

	/**
	 * Gets the name of the server used.
	 *
	 * @deprecated As of release 5.0, replaced by {@link #getDataSource()}.
	 */
	@Deprecated
	public abstract String js_getServerName();

	/**
	 * Get the datasource used.
	 * The datasource is an url that describes the data source.
	 * 
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getDataSourceServerName(String)
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getDataSourceTableName(String)
	 *
	 * @sample var dataSource = %%prefix%%foundset.getDataSource();
	 * 
	 * @return String data source.
	 */
	public abstract String js_getDataSource();

	/**
	 * Gets the relation name (null if not a related foundset).
	 *
	 * @sample var relName = %%prefix%%foundset.getRelationName();
	 * 
	 * @return String relation name when related.
	 */
	public abstract String js_getRelationName();

	/**
	 * Invert the foundset against all rows of the current table.
	 * All records that are not in the foundset will become the current foundset.
	 *
	 * @sample %%prefix%%foundset.invertRecords();
	 */
	public abstract void js_invertRecords() throws ServoyException;

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
	 * @sample %%prefix%%foundset.loadAllRecords();
	 * 
	 * @return true if records are loaded, false otherwise.
	 */
	public abstract boolean js_loadAllRecords() throws ServoyException;

	/**
	 * Loads the records that are currently omitted as a foundset.
	 * 
	 * Before loading the omitted records, all unsaved records will be saved in the database.
	 * If this fails (due to validation failures or sql errors) or is not allowed (autosave off), 
	 * omitted records will not be loaded,
	 *
	 * @sample %%prefix%%foundset.loadOmittedRecords();
	 * 
	 * @return true if records are loaded, false otherwise.
	 */
	public abstract boolean js_loadOmittedRecords() throws ServoyException;

	/**
	 * Reloads all last (related) records again, if, for example, after search in tabpanel.
	 * When in find mode, this will reload the records from before the find() call.
	 * 
	 * @sample
	 *  //to reload all last (related) records again, if for example when searched in tabpanel
	 *  %%prefix%%foundset.loadRecords();
	 *
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords() throws ServoyException;

	/**
	 * @clonedesc com.servoy.j2db.FormController$JSForm#js_loadRecords(IDataSet)
	 * 
	 * @sample
	 * // loads a primary key dataset, will remove related sort!
	 * //var dataset = databaseManager.getDataSetByQuery(...);
	 * // dataset must match the table primary key columns (alphabetically ordered)
	 * %%prefix%%foundset.loadRecords(dataset);
	 *
	 * @param dataset pkdataset
	 * 
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(IDataSet dataset) throws ServoyException;

	/** 
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 * 
	 * @param dataset The dataset to load records from
	 * @param ignored true for ignoring the argument array
	 * 
	 * @deprecated use loadRecords(JSDataSet)
	 */
	@Deprecated
	public abstract boolean js_loadRecords(IDataSet dataset, Object ignored) throws ServoyException;

	/**
	 * Copies foundset data from another foundset.
	 * 
	 * @sample
	 * //Copies foundset data from another foundset
	 * %%prefix%%foundset.loadRecords(fs);
	 *
	 * @param foundset The foundset to load records from
	 * 
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(FoundSet foundset);

	/** 
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 * 
	 * @param foundset The foundset to load record from
	 * @param ignored true for ignoring the argument array
	 * 
	 * @deprecated use loadRecords(FoundSet)
	 */
	@Deprecated
	public abstract boolean js_loadRecords(FoundSet foundset, Object ignored);

	/**
	 * @clonedesc com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @sample
	 * //loads records in to the foundset based on a query (also known as 'Form by query')
	 * %%prefix%%foundset.loadRecords(sqlstring,parameters);
	 * 
	 * @param queryString select statement
	 * @param argumentsArray arguments to query
	 * 
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(String queryString, Object[] argumentsArray) throws ServoyException;

	/**
	 * @clonedesc com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @sample
	 * //loads records in to the foundset based on a query (also known as 'Form by query')
	 * %%prefix%%foundset.loadRecords(sqlstring);
	 * 
	 * @param queryString select statement
	 * 
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(String queryString) throws ServoyException;

	/**
	 * @clonedesc com.servoy.j2db.FormController$JSForm#js_loadRecords(Number)
	 * @sample
	 * //Loads a single record by primary key, will remove related sort!
	 * %%prefix%%foundset.loadRecords(123);
	 *
	 * @param numberpk single-column pk value
	 * 
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(Number numberpk) throws ServoyException;

	/** 
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 * 
	 * @param numberpk single-column pk value
	 * @param ignored true to ignore arguments array
	 * 
	 * @deprecated use loadRecords(Number)
	 */
	@Deprecated
	public abstract boolean js_loadRecords(Number numberpk, Object ignored) throws ServoyException;

	/**
	 * @clonedesc com.servoy.j2db.FormController$JSForm#js_loadRecords(UUID)
	 * @sample
	 * //Loads a single record by primary key, will remove related sort!
	 * %%prefix%%foundset.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
	 *
	 * @param uuidpk single-column pk value
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(UUID uuidpk) throws ServoyException;

	/** 
	 * Method to handle old loadRecords calls with ignored argumentsaray.
	 * @param uuidpk single-column pk value
	 * @param ignored true to ignore argument array
	 * 
	 * @deprecated use loadRecords(UUID)
	 */
	@Deprecated
	public abstract boolean js_loadRecords(UUID uuidpk, Object ignored) throws ServoyException;

	/**
	 * @clonedesc com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * 
	 * @sample
	 * %%prefix%%foundset.loadRecords(qbselect);
	 * 
	 * @param querybuilder the query builder
	 * @return true if successful
	 */
	public abstract boolean js_loadRecords(QBSelect querybuilder) throws ServoyException;

	/** 
	 * Method to handle old foundset loadRecords calls.
	 * Deprecated method to handle pre-6.1 calls to varargs function foundset.loadRecords([1]), this was called with vargs=[1] in stead of vargs=[[1]].
	 * 
	 * @param vargs the arguments
	 * 
	 * @deprecated use loadRecords with single typed argument
	 */
	@Deprecated
	public abstract boolean js_loadRecords(Object[] vargs) throws ServoyException;

	/**
	 * Perform a relookup for the record under the given index
	 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
	 *
	 * @sample %%prefix%%foundset.relookup(1);
	 * @param index record index (1-based) 
	 */
	public abstract void js_relookup(Number index);

	/**
	 * Perform a relookup for the currently selected records
	 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
	 *
	 * @sample %%prefix%%foundset.relookup(1);
	 */
	public abstract void js_relookup();

	/**
	 * Get a value based on a dataprovider name.
	 *
	 * @sample var val = %%prefix%%foundset.getDataProviderValue('contact_name');
	 *
	 * @param dataProviderID data provider name
	 * 
	 * @return Object value
	 */
	public abstract Object js_getDataProviderValue(String dataProviderID);

	/**
	 * Set a value based on a dataprovider name.
	 *
	 * @sample %%prefix%%foundset.setDataProviderValue('contact_name','mycompany');
	 *
	 * @param dataProviderID data provider name
	 *
	 * @param value value to set
	 */
	public abstract void js_setDataProviderValue(String dataProviderID, Object value);

	/**
	 * Create a new unrelated foundset that is a copy of the current foundset.
	 * If the current foundset is not related, no copy will made.
	 *
	 * @sample %%prefix%%foundset.unrelate();
	 * 
	 * @return FoundSet unrelated foundset.
	 */
	public abstract IFoundSetInternal js_unrelate();

	/**
	 * Select the record based on pk data.
	 * Note that if the foundset has not loaded the record with the pk, selectrecord will fail.
	 * 
	 * In case of a table with a composite key, the pk sequence must match the alphabetical 
	 * ordering of the pk column names.
	 *
	 * @sample %%prefix%%foundset.selectRecord(pkid1,pkid2,pkidn);//pks must be alphabetically set! It is also possible to use an array as parameter.
	 *
	 * @param pkid1 primary key
	 *
	 * @param pkid2 optional second primary key (in case of composite primary key)
	 *
	 * @param pkidn optional nth primary key
	 * 
	 * @return true if succeeded.
	 */
	public abstract boolean js_selectRecord(Object[] vargs);

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
	public abstract boolean js_deleteRecord(Number index) throws ServoyException;

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
	public abstract boolean js_deleteRecord(IRecord record) throws ServoyException;

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
	public abstract boolean js_deleteRecord() throws ServoyException;

	/**
	 * Omit record under the given index, to be shown with loadOmittedRecords.
	 * If the foundset is in multiselect mode, all selected records are omitted (when no index parameter is used).
	
	 * Note: The omitted records list is discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invertRecords()
	 *
	 * @sampleas js_omitRecord()
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadOmittedRecords()
	 * 
	 * @param index The index of the record to omit.
	 * 
	 * @return boolean true if all records could be omitted.
	 */
	public abstract boolean js_omitRecord(Number index) throws ServoyException;

	/**
	 * Omit current record, to be shown with loadOmittedRecords.
	 * If the foundset is in multiselect mode, all selected records are omitted (when no index parameter is used).
	
	 * Note: The omitted records list is discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invertRecords()
	 *
	 * @sample var success = %%prefix%%foundset.omitRecord();
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadOmittedRecords()
	 * 
	 * @return boolean true if all records could be omitted.
	 */
	public abstract boolean js_omitRecord() throws ServoyException;

	/**
	 * Get the current sort columns.
	 *
	 * @sample
	 * //reverse the current sort
	 * 
	 * //the original sort "companyName asc, companyContact desc"
	 * //the inversed sort "companyName desc, companyContact asc"
	 * var foundsetSort = foundset.getCurrentSort()
	 * var sortColumns = foundsetSort.split(',')
	 * var newFoundsetSort = ''
	 * for(var i=0; i<sortColumns.length; i++)
	 * {
	 * 	var currentSort = sortColumns[i]
	 * 	var sortType = currentSort.substring(currentSort.length-3)
	 * 	if(sortType.equalsIgnoreCase('asc'))
	 * 	{
	 * 		newFoundsetSort += currentSort.replace(' asc', ' desc')
	 * 	}
	 * 	else
	 * 	{
	 * 		newFoundsetSort += currentSort.replace(' desc', ' asc')
	 * 	}
	 * 	if(i != sortColumns.length - 1)
	 * 	{
	 * 		newFoundsetSort += ','
	 * 	}
	 * }
	 * foundset.sort(newFoundsetSort)
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_sort(String)
	 * 
	 * @return String sort columns
	 */
	public abstract String js_getCurrentSort();

	/**
	 * Sorts the foundset based on the given sort string.
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
	 * 
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 */
	public abstract void js_sort(String sortString) throws ServoyException;

	/**
	 * Sorts the foundset based on the given sort string.
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
	 * 
	 * @sample %%prefix%%foundset.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 * @param defer boolean when true, the "sortString" will be just stored, without performing a query on the database (the actual sorting will be deferred until the next data loading action).
	 */
	public abstract void js_sort(String sortString, Boolean defer) throws ServoyException;

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
	 * @param comparator record comparator function
	 */
	public abstract void js_sort(Function comparator);

	/**
	 * Delete all records in foundset, resulting in empty foundset.
	 *
	 * @sample
	 * var success = %%prefix%%foundset.deleteAllRecords();
	 * @return boolean true if all records could be deleted.
	 */
	public abstract boolean js_deleteAllRecords() throws ServoyException;

	/**
	 * Duplicate record at index in the foundset, change selection to new record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param onTop when true the new record is added as the topmost record.
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Number index, Boolean onTop) throws ServoyException;

	/**
	 * Duplicate record at index in the foundset, change selection to new record, place on top.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Number index) throws ServoyException;

	/**
	 * Duplicate selected record, change selection to new record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @param onTop when true the new record is added as the topmost record.
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Boolean onTop) throws ServoyException;

	/**
	 * Duplicate selected record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @param onTop when true the new record is added as the topmost record.
	 * @param changeSelection when true the selection is changed to the duplicated record.
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Boolean onTop, Boolean changeSelection) throws ServoyException;

	/**
	 * Duplicate current record, change selection to new record, place on top.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord() throws ServoyException;

	/**
	 * Duplicate record at index in the foundset.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param onTop when true the new record is added as the topmost record.
	 * @param changeSelection when true the selection is changed to the duplicated record.
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Number index, Boolean onTop, Boolean changeSelection) throws ServoyException;

	/**
	 * Duplicate record at index in the foundset, change selection to new record.
	 *
	 * @sampleas js_duplicateRecord(Number, Number, Boolean)
	 * 
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param location the new record is added at specified index
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Number index, Number location) throws ServoyException;

	/**
	 * Duplicate record at index in the foundset.
	 *
	 * @sample
	 * %%prefix%%foundset.duplicateRecord();
	 * %%prefix%%foundset.duplicateRecord(false); //duplicate the current record, adds at bottom
	 * %%prefix%%foundset.duplicateRecord(1,2); //duplicate the first record as second record
	 * //duplicates the record (record index 3), adds on top and selects the record
	 * %%prefix%%foundset.duplicateRecord(3,true,true);
	 * 
	 * @param index The index of the record to duplicate; defaults to currently selected index. Ignored if first given parameter is a boolean value.
	 * @param location the new record is added at specified index
	 * @param changeSelection when true the selection is changed to the duplicated record.
	 *  
	 * @return 0 if record was not created or the record index if it was created.
	 */
	public abstract int js_duplicateRecord(Number index, Number location, Boolean changeSelection) throws ServoyException;

	/**
	 * Create a new record in the foundset and change selection to it. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param index the new record is added at specified index.
	 * 
	 * @return int index of new record.
	 */
	public abstract int js_newRecord(Number index) throws ServoyException;

	/**
	 * Create a new record in the foundset. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param index the new record is added at specified index.
	 * @param changeSelection boolean when true the selection is changed to the new record.
	 * 
	 * @return int index of new record.
	 */
	public abstract int js_newRecord(Number index, Boolean changeSelection) throws ServoyException;

	/**
	 * Create a new record in the foundset and change selection to it. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param onTop when true the new record is added as the topmost record.
	 * 
	 * @return int index of new record.
	 */
	public abstract int js_newRecord(Boolean onTop) throws ServoyException;

	/**
	 * Create a new record in the foundset. Returns -1 if the record can't be made.
	 *
	 * @sampleas js_newRecord()
	 *
	 * @param onTop when true the new record is added as the topmost record.
	 * @param changeSelection boolean when true the selection is changed to the new record.
	 * 
	 * @return int index of new record.
	 */
	public abstract int js_newRecord(Boolean onTop, Boolean changeSelection) throws ServoyException;

	/**
	 * Get the indexes of the selected records.
	 * When the founset is in multiSelect mode (see property multiSelect), a selection can consist of more than one index.
	 *
	 * @sample
	 * // modify selection to the first selected item and the following row only
	 * var current = %%prefix%%foundset.getSelectedIndexes();
	 * if (current.length > 1)
	 * {
	 * 	var newSelection = new Array();
	 * 	newSelection[0] = current[0]; // first current selection
	 * 	newSelection[1] = current[0] + 1; // and the next row
	 * 	%%prefix%%foundset.setSelectedIndexes(newSelection);
	 * }
	 * @return Array current indexes (1-based)
	 */
	public abstract Number[] jsFunction_getSelectedIndexes();

	/**
	 * Set the selected records indexes.
	 *
	 * @sampleas jsFunction_getSelectedIndexes()
	 * 
	 * @param indexes An array with indexes to set.
	 */
	public abstract void jsFunction_setSelectedIndexes(Number[] indexes);

	/**
	 * Get the number of records in this foundset.
	 *
	 * @sample
	 * for ( var i = 1 ; i <= %%prefix%%foundset.getMaxRecordIndex() ; i++ )
	 * {
	 * 	%%prefix%%foundset.setSelectedIndex(i);
	 * 	//do some action per record
	 * }
	 *
	 * @deprecated As of release 3.1, replaced by {@link #getSize()}.
	 */
	@Deprecated
	public abstract int js_getMaxRecordIndex();

	/**
	 * Get the record index. Will return -1 if the record can't be found.
	 *
	 * @sample var index = %%prefix%%foundset.getRecordIndex(record);
	 *
	 * @param record Record
	 * 
	 * @return int index. 
	 */
	public abstract int js_getRecordIndex(IRecordInternal record);

	/**
	 * Get the selected records.
	 * When the founset is in multiSelect mode (see property multiSelect), selection can be a more than 1 record.
	 *
	 * @sample var selectedRecords = %%prefix%%foundset.getSelectedRecords();
	 * @return Array current records.
	 */
	public abstract IRecordInternal[] js_getSelectedRecords();

	/**
	 * Get or set the multiSelect flag of the foundset.
	 *
	 * @sample
	 * // allow user to select multiple rows.
	 * %%prefix%%foundset.multiSelect = true;
	 */
	public abstract boolean js_isMultiSelect();

	public abstract void js_setMultiSelect(boolean multiSelect);

	public abstract QBSelect getQuery();

}