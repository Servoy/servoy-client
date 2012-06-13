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

package com.servoy.j2db.solutionmodel;

import com.servoy.j2db.persistence.ValueList;


/**
 * Solution model value list object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMValueList extends ISMHasUUID
{

	/**
	 * Constant to set the valueListType of a JSValueList.
	 * Sets the value list to use a custom list of values.
	 * Also used in solutionModel.newValueList(...) to create new valuelists
	 *
	 * @sample 
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.valueListType = JSValueList.CUSTOM_VALUES; // Change the type to custom values.
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 */
	public static final int CUSTOM_VALUES = ValueList.CUSTOM_VALUES;
	/**
	 * Constant to set the valueListType of a JSValueList.
	 * Sets the value list to use values loaded from a database.
	 * Also used in solutionModel.newValueList(...) to create new valuelists
	 *
	 * @sample 
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.valueListType = JSValueList.DATABASE_VALUES; // Change the type to database values.
	 * vlist.dataSource = 'db:/example_data/parent_table';
	 * vlist.setDisplayDataProviderIds('parent_table_text');
	 * vlist.setReturnDataProviderIds('parent_table_text', 'parent_table_id');
	 * vlist.separator = ' ## ';
	 * vlist.sortOptions = 'parent_table_text desc';
	 */
	public static final int DATABASE_VALUES = ValueList.DATABASE_VALUES;
	/**
	 * Constant to set/get the addEmptyValue property of a JSValueList.
	 *
	 * @sample 
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * vlist.addEmptyValue = JSValueList.EMPTY_VALUE_ALWAYS;
	 * var cmb = form.newComboBox('my_table_text', 10, 10, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public static final int EMPTY_VALUE_ALWAYS = ValueList.EMPTY_VALUE_ALWAYS;
	/**
	 * @clonedesc EMPTY_VALUE_ALWAYS
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * vlist.addEmptyValue = JSValueList.EMPTY_VALUE_NEVER;
	 * var cmb = form.newComboBox('my_table_text', 10, 10, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public static final int EMPTY_VALUE_NEVER = ValueList.EMPTY_VALUE_NEVER;

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getAddEmptyValue()
	 *
	 * @sample 
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "one\ntwo\nthree\nfour";
	 * vlist.addEmptyValue = JSValueList.EMPTY_VALUE_NEVER;
	 * var cmb = form.newComboBox('my_table_text', 10, 10, 100, 20);
	 * cmb.valuelist = vlist;
	 */
	public int getAddEmptyValue();

	/**
	 * @clonedesc setDisplayDataProviderIds(String, String, String)
	 * @sampleas DATABASE_VALUES
	 *
	 */
	public void setDisplayDataProviderIds();

	/**
	 * @clonedesc setDisplayDataProviderIds(String, String, String)
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 */
	public void setDisplayDataProviderIds(String dataprovider1);

	/**
	 * @clonedesc setDisplayDataProviderIds(String, String, String)
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 *
	 * @param dataprovider2 The second display dataprovider.
	 */
	public void setDisplayDataProviderIds(String dataprovider1, String dataprovider2);

	/**
	 * Set the display dataproviders. There can be at most 3 of them, combined with the return dataproviders.
	 * The values taken from these dataproviders, in order, separated by the separator, will be displayed
	 * by the valuelist.
	 *
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 *
	 * @param dataprovider2 The second display dataprovider.
	 *
	 * @param dataprovider3 The third display dataprovider.
	 */
	public void setDisplayDataProviderIds(String dataprovider1, String dataprovider2, String dataprovider3);

	/**
	 * Returns an array of the dataproviders that will be used to display the valuelist value.
	 *
	 * @sample 
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.dataSource = 'db:/example_data/parent_table';
	 * vlist.setDisplayDataProviderIds('parent_table_text', 'parent_table_id');
	 * vlist.setReturnDataProviderIds('parent_table_text');
	 * var dispDP = vlist.getDisplayDataProviderIds();
	 * for (var i=0; i<dispDP.length; i++)
	 * 	application.output(dispDP[i]);
	 * var retDP = vlist.getReturnDataProviderIds();
	 * for (var i=0; i<retDP.length; i++)
	 *	application.output(retDP[i]);
	 *
	 * @return An array of Strings representing the names of the display dataproviders.
	 */
	public Object[] getDisplayDataProviderIds();

	/**
	 * @clonedesc setReturnDataProviderIds(String, String, String)
	 * @sampleas DATABASE_VALUES
	 */
	public void setReturnDataProviderIds();

	/**
	 * @clonedesc setReturnDataProviderIds(String, String, String)
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 */
	public void setReturnDataProviderIds(String dataprovider1);

	/**
	 * @clonedesc setReturnDataProviderIds(String, String, String)
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 *
	 * @param dataprovider2 The second return dataprovider. 
	 */
	public void setReturnDataProviderIds(String dataprovider1, String dataprovider2);

	/**
	 * Set the return dataproviders. There can be at most 3 of them, combined with the display dataproviders.
	 * The values taken from these dataproviders, in order, separated by the separator, will be returned
	 * by the valuelist.
	 *
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 *
	 * @param dataprovider2 The second return dataprovider. 
	 *
	 * @param dataprovider3 The third return dataprovider.
	 */
	public void setReturnDataProviderIds(String dataprovider1, String dataprovider2, String dataprovider3);

	/**
	 * Returns an array of the dataproviders that will be used to define the valuelist value that is saved.
	 *
	 * @sampleas getDisplayDataProviderIds() 
	 *
	 * @return An array of Strings representing the names of the return dataprovider.
	 */
	public Object[] getReturnDataProviderIds();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getCustomValues()
	 *
	 * @sample
	 * var vl1 = solutionModel.newValueList("customtext",JSValueList.CUSTOM_VALUES);
	 * vl1.customValues = "customvalue1\ncustomvalue2";
	 * var vl2 = solutionModel.newValueList("customid",JSValueList.CUSTOM_VALUES);
	 * vl2.customValues = "customvalue1|1\ncustomvalue2|2";
	 * var form = solutionModel.newForm("customvaluelistform",controller.getDataSource(),null,true,300,300);
	 * var combo1 = form.newComboBox("scopes.globals.text",10,10,120,20);
	 * combo1.valuelist = vl1;
	 * var combo2 = form.newComboBox("scopes.globals.id",10,60,120,20);
	 * combo2.valuelist = vl2;
	 */
	public String getCustomValues();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getName()
	 * 
	 * @sampleas getUseTableFilter()
	 */
	public String getName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getRelationName()
	 * 
	 * @sample
	 * var rel = solutionModel.newRelation('parent_to_child', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * rel.newRelationItem('parent_table_id', '=', 'child_table_parent_id');
	 *
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.dataSource = 'db:/example_data/parent_table';
	 * vlist.relationName = 'parent_to_child';
	 * vlist.setDisplayDataProviderIds('child_table_text');
	 * vlist.setReturnDataProviderIds('child_table_text');
	 */
	public String getRelationName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getSeparator()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String getSeparator();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getServerName()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String getServerName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getSortOptions()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String getSortOptions();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getTableName()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String getTableName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getDataSource()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.dataSource = 'db:/example_data/parent_table';
	 * vlist.setDisplayDataProviderIds('parent_table_text');
	 * vlist.setReturnDataProviderIds('parent_table_text');
	 */
	public String getDataSource();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getUseTableFilter()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.dataSource = 'db:/example_data/valuelists';
	 * vlist.setDisplayDataProviderIds('valuelist_data');
	 * vlist.setReturnDataProviderIds('valuelist_data');
	 * vlist.useTableFilter = true;
	 * vlist.name = 'two';
	 */
	public boolean getUseTableFilter();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getValueListType()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public int getValueListType();

	/**
	 * Property if an empty value must be shown, set one of the JSValueList.EMPTY_VALUE_ALWAYS or JSValueList.EMPTY_VALUE_NEVER constants
	 */
	public void setAddEmptyValue(int arg);

	public void setCustomValues(String arg);

	/**
	 * Sets the global method for this valuelist, this method will then be called to get the values for this valuelist.
	 * It should return a JSDataSet with 2 columns: display|real
	 */
	public void setGlobalMethod(ISMMethod method);

	/**
	 * A global method that provides the data for the valuelist. The global method must provided the data 
	 * as a JSDataSet.
	 * 
	 * It is called when the valuelist needs data, it has 3 modes.
	 * real and display params both null: return the whole list
	 * only display is specified, called by a typeahead, return a filtered list
	 * only real value is specified, called when the list doesnt contain the real value for the give record value, this will insert this value into the existing list.
	 * 
	 * In find mode the record with be the FindRecord which is just like a normal JSRecord (DataRecord) it has the same properties (column/dataproviders) but doesnt have its methods (like isEditing())
	 * 
	 * @sample
	 * var listProvider = solutionModel.newGlobalMethod('globals', 'function getDataSetForValueList(displayValue, realValue, record, valueListName, findMode) {' +
	 * 		'	' +
	 * 		'if (displayValue == null && realValue == null) {' +
	 * 		'  // TODO think about caching this result. can be called often!' +
	 * 		'  // return the complete list' +
	 * 		'  return databaseManager.getDataSetByQuery("example_data", "select firstname || \' \' || lastname, employeeid from employees", null, 100);' +
	 * 		'} else if (displayValue != null) {' +
	 * 		'  // TYPE_AHEAD filter call, return a filtered list' +
	 * 		'  var args = [displayValue + "%", displayValue + "%"]' +
	 * 		'  return databaseManager.getDataSetByQuery("example_data", "select firstname || \' \' || lastname, employeeid from employees where firstname like ? or lastname like ?", args, 100);' +
	 * 		'} else if (realValue != null) {' +
	 * 		'  // TODO think about caching this result. can be called often!' +
	 * 		'  // real object not found in the current list, return 1 row with display,realvalue that will be added to the current list' +
	 * 		'  // dont return a complete list in this mode because that will be added to the list that is already there' +
	 * 		'  args = [realValue];' +
	 * 		'  return databaseManager.getDataSetByQuery("example_data", "select firstname || \' \' || lastname, employeeid from employees where employeeid = ?", args, 1);' +
	 * 		'}' +
	 * 		'}');
	 * var vlist = solutionModel.newValueList('vlist', JSValueList.CUSTOM_VALUES);
	 * vlist.globalMethod = listProvider;
	 */
	public ISMMethod getGlobalMethod();

	public void setName(String arg);

	public void setRelationName(String arg);

	public void setSeparator(String arg);

	public void setServerName(String arg);

	public void setSortOptions(String arg);

	public void setTableName(String arg);

	public void setDataSource(String arg);

	public void setUseTableFilter(boolean arg);

	public void setValueListType(int arg);
}