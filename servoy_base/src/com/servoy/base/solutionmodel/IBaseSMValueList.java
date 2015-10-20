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

package com.servoy.base.solutionmodel;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Solution model value list object.
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMValueList
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
	public static final int CUSTOM_VALUES = IValueListConstants.CUSTOM_VALUES;
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
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int DATABASE_VALUES = IValueListConstants.DATABASE_VALUES;

	/**
	 * A string with the elements in the valuelist. The elements 
	 * can be separated by linefeeds (custom1
	 * custom2), optional with realvalues ((custom1|1
	 * custom2|2)).
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

	public void setCustomValues(String arg);

	/**
	 * The name of the value list.
	 * 
	 * It is relevant when the "useTableFilter" property is set.
	 */
	public String getName();

}