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
package com.servoy.j2db.scripting.solutionmodel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSValueList implements IConstantsObject
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
	 * vlist.serverName = 'example_data';
	 * vlist.tableName = 'parent_table';
	 * vlist.setDisplayDataProviderIds('parent_table_text');
	 * vlist.setReturnDataProviderIds('parent_table_text', 'parent_table_id');
	 * vlist.separator = ' ## ';
	 * vlist.sortOptions = 'parent_table_text desc';
	 */
	public static final int DATABASE_VALUES = ValueList.DATABASE_VALUES;

	/**
	 * @sameas CUSTOM_VALUES
	 */
//	public static final int GLOBAL_METHOD_VALUES = ValueList.GLOBAL_METHOD_VALUES;

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


	private ValueList valuelist;
	private final IApplication application;
	private boolean isCopy;

	/**
	 * 
	 */
	public JSValueList(ValueList valuelist, IApplication application, boolean isNew)
	{
		this.valuelist = valuelist;
		this.application = application;
		this.isCopy = isNew;
	}

	void checkModification()
	{
		// TODO already in use check???

		// make copy if needed
		if (!isCopy)
		{
			valuelist = application.getFlattenedSolution().createPersistCopy(valuelist);
			isCopy = true;
		}
	}

	/**
	 * @return
	 */
	ValueList getValueList()
	{
		return valuelist;
	}

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
	public int js_getAddEmptyValue()
	{
		return valuelist.getAddEmptyValue();
	}

	/**
	 * Set the display dataproviders. There can be at most 3 of them, combined with the return dataproviders.
	 * The values taken from these dataproviders, in order, separated by the separator, will be displayed
	 * by the valuelist.
	 *
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 *
	 * @param dataprovider2 optional The second display dataprovider.
	 *
	 * @param dataprovider3 optional The third display dataprovider.
	 */
	public void jsFunction_setDisplayDataProviderIds(Object[] ids)
	{
		checkModification();
		if (ids.length > 3) throw new IllegalArgumentException("max 3 ids allowed in the display dataproviders ids"); //$NON-NLS-1$
		clearUnusedDataProviders(valuelist.getReturnDataProviders());
		if (ids == null || ids.length == 0)
		{
			valuelist.setShowDataProviders(0);
		}
		else
		{
			int showId = 0;
			for (int i = 0; i < ids.length; i++)
			{
				String id = (String)ids[i];
				String currentId = valuelist.getDataProviderID1();
				if (i < 1 && testId(id, currentId))
				{
					showId += 1;
					valuelist.setDataProviderID1(id);
				}
				else
				{
					// dataprovider1 is used as a return value test dataprovider2
					currentId = valuelist.getDataProviderID2();
					if (i < 2 && testId(id, currentId))
					{
						showId += 2;
						valuelist.setDataProviderID2(id);
					}
					else
					{
						// dataprovider2 is used as a return value test dataprovider3
						currentId = valuelist.getDataProviderID3();
						if (i < 3 && testId(id, currentId))
						{
							showId += 4;
							valuelist.setDataProviderID3(id);
						}
						else
						{
							throw new RuntimeException("Cant set display values all slots are used by return dataproviders"); //$NON-NLS-1$
						}
					}
				}
			}
			valuelist.setShowDataProviders(showId);
		}
	}

	private void clearUnusedDataProviders(int valueToKeep)
	{
		if (valuelist != null)
		{
			if ((valueToKeep & 1) != 1)
			{
				valuelist.setDataProviderID1(null);
			}
			if ((valueToKeep & 2) != 2)
			{
				valuelist.setDataProviderID2(null);
			}
			if ((valueToKeep & 4) != 4)
			{
				valuelist.setDataProviderID3(null);
			}
		}
	}

	private boolean testId(String wantedId, String currentId)
	{
		return wantedId.equals(currentId) || currentId == null;
	}

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
	public Object[] jsFunction_getDisplayDataProviderIds()
	{
		return getDataProviders(valuelist.getShowDataProviders());
	}

	/**
	 * Set the return dataprovers. There can be at most 3 of them, combined with the display dataproviders.
	 * The values taken from these dataproviders, in order, separated by the separator, will be returned
	 * by the valuelist.
	 *
	 * @sampleas DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 *
	 * @param dataprovider2 optional The second return dataprovider. 
	 *
	 * @param dataprovider3 optional The third return dataprovider.
	 */
	public void jsFunction_setReturnDataProviderIds(Object[] ids)
	{
		checkModification();
		if (ids.length > 3) throw new IllegalArgumentException("max 3 ids allowed in the display dataproviders ids"); //$NON-NLS-1$
		clearUnusedDataProviders(valuelist.getShowDataProviders());
		if (ids == null || ids.length == 0)
		{
			valuelist.setReturnDataProviders(0);
		}
		else
		{
			int returnId = 0;
			for (int i = 0; i < ids.length; i++)
			{
				String id = (String)ids[i];
				String currentId = valuelist.getDataProviderID1();
				if (i < 1 && testId(id, currentId))
				{
					returnId += 1;
					valuelist.setDataProviderID1(id);
				}
				else
				{
					// dataprovider1 is used as a return value test dataprovider2
					currentId = valuelist.getDataProviderID2();
					if (i < 2 && testId(id, currentId))
					{
						returnId += 2;
						valuelist.setDataProviderID2(id);
					}
					else
					{
						// dataprovider2 is used as a return value test dataprovider3
						currentId = valuelist.getDataProviderID3();
						if (i < 3 && testId(id, currentId))
						{
							returnId += 4;
							valuelist.setDataProviderID3(id);
						}
						else
						{
							throw new RuntimeException("Cant set display values all slots are used by other dataproviders (3 max combined)"); //$NON-NLS-1$
						}
					}
				}
			}
			valuelist.setReturnDataProviders(returnId);
		}
	}


	/**
	 * Returns an array of the dataproviders that will be used to define the valuelist value that is saved.
	 *
	 * @sampleas jsFunction_getDisplayDataProviderIds() 
	 *
	 * @return An array of Strings representing the names of the return dataprovider.
	 */
	public Object[] jsFunction_getReturnDataProviderIds()
	{
		return getDataProviders(valuelist.getReturnDataProviders());
	}

	/**
	 * @param selection
	 * @return
	 */
	private String[] getDataProviders(int selection)
	{
		ArrayList<String> dataproviders = new ArrayList<String>();
		if ((selection & 1) == 1 && valuelist.getDataProviderID1() != null)
		{
			dataproviders.add(valuelist.getDataProviderID1());
		}
		if ((selection & 2) == 2 && valuelist.getDataProviderID2() != null)
		{
			dataproviders.add(valuelist.getDataProviderID2());
		}
		if ((selection & 4) == 4 && valuelist.getDataProviderID3() != null)
		{
			dataproviders.add(valuelist.getDataProviderID3());
		}
		return dataproviders.toArray(new String[dataproviders.size()]);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getCustomValues()
	 *
	 * @sample
	 * var vl1 = solutionModel.newValueList("customtext",JSValueList.CUSTOM_VALUES);
	 * vl1.customValues = "customvalue1\ncustomvalue2";
	 * var vl2 = solutionModel.newValueList("customid",JSValueList.CUSTOM_VALUES);
	 * vl2.customValues = "customvalue1|1\ncustomvalue2|2";
	 * var form = solutionModel.newForm("customvaluelistform",controller.getDataSource(),null,true,300,300);
	 * var combo1 = form.newComboBox("globals.text",10,10,120,20);
	 * combo1.valuelist = vl1;
	 * var combo2 = form.newComboBox("globals.id",10,60,120,20);
	 * combo2.valuelist = vl2;
	 */
	public String js_getCustomValues()
	{
		return valuelist.getCustomValues();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getName()
	 * 
	 * @sampleas js_getUseTableFilter()
	 */
	public String js_getName()
	{
		return valuelist.getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getRelationName()
	 * 
	 * @sample
	 * var rel = solutionModel.newRelation('parent_to_child', 'example_data', 'parent_table', 'example_data', 'child_table', JSRelation.INNER_JOIN);
	 * rel.newRelationItem('parent_table_id', '=', 'child_table_parent_id');
	 *
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.serverName = 'example_data';
	 * vlist.tableName = 'parent_table';
	 * vlist.relationName = 'parent_to_child';
	 * vlist.setDisplayDataProviderIds('child_table_text');
	 * vlist.setReturnDataProviderIds('child_table_text');
	 */
	public String js_getRelationName()
	{
		return valuelist.getRelationName();
	}

	@Deprecated
	public String js_getRelationNMName()
	{
		return valuelist.getRelationNMName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getSeparator()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String js_getSeparator()
	{
		return valuelist.getSeparator();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getServerName()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String js_getServerName()
	{
		return valuelist.getServerName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getSortOptions()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String js_getSortOptions()
	{
		return valuelist.getSortOptions();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getTableName()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public String js_getTableName()
	{
		return valuelist.getTableName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getDataSource()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.dataSource = 'db:/example_data/parent_table';
	 * vlist.setDisplayDataProviderIds('parent_table_text');
	 * vlist.setReturnDataProviderIds('parent_table_text');
	 */
	public String js_getDataSource()
	{
		return valuelist.getDataSource();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getUseTableFilter()
	 * 
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.DATABASE_VALUES);
	 * vlist.serverName = 'example_data';
	 * vlist.tableName = 'valuelists';
	 * vlist.setDisplayDataProviderIds('valuelist_data');
	 * vlist.setReturnDataProviderIds('valuelist_data');
	 * vlist.useTableFilter = true;
	 * vlist.name = 'two';
	 */
	public boolean js_getUseTableFilter()
	{
		return valuelist.getUseTableFilter();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getValueListType()
	 * 
	 * @sampleas DATABASE_VALUES
	 */
	public int js_getValueListType()
	{
		return valuelist.getValueListType();
	}

	/**
	 * Property if an empty value must be shown, set one of the JSValueList.EMPTY_VALUE_ALWAYS or JSValueList.EMPTY_VALUE_NEVER constants
	 *
	 * @sample 
	 */
	public void js_setAddEmptyValue(int arg)
	{
		checkModification();
		valuelist.setAddEmptyValue(arg);
	}

	public void js_setCustomValues(String arg)
	{
		checkModification();
		valuelist.setCustomValues(arg);
	}

	/**
	 * Sets the global method for this valuelist, this method will then be called to get the values for this valuelist.
	 * It should return a JSDataSet with 2 columns: display|real
	 * 
	 * @sample
	 */
	public void js_setGlobalMethod(JSMethod method)
	{
		checkModification();
		if (method == null)
		{
			valuelist.setCustomValues(null);
			valuelist.setValueListType(ValueList.CUSTOM_VALUES);
		}
		else
		{
			ScriptMethod scriptMethod = method.getScriptMethod();
			if (scriptMethod.getParent() instanceof Solution)
			{
				valuelist.setCustomValues(ScriptVariable.GLOBAL_DOT_PREFIX + scriptMethod.getName());
				valuelist.setValueListType(ValueList.GLOBAL_METHOD_VALUES);
			}
			else
			{
				throw new RuntimeException("Only global methods are supported for a valuelist"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * A global method that provides the data for the valuelist. The global method must provided the data 
	 * as a JSDataSet.
	 * 
	 * It is called when the valuelist needs data, it has 3 modes.
	 * real and display params both null: return the whole list
	 * only display is specified, called by a typeahead, return a filtered list
	 * only real value is specified, called when the list doesnt contain the real value for the give record value, this will insert this value into the existing list
	 * 
	 * @sample
	 * var listProvider = solutionModel.newGlobalMethod('function getDataSetForValueList(displayValue, realValue, record, valueListName) {' +
	 *		'	' +
	 *		'if (displayValue == null && realValue == null) {' +
	 *		'  // TODO think about caching this result. can be called often!' +
	 *		'  // return the complete list' +
	 *		'  return databaseManager.getDataSetByQuery("example_data", "select firstname + ' ' + lastname, employeeid from employees", null, 100);' +
	 *		'} else if (displayValue != null) {' +
	 *		'  // TYPE_AHEAD filter call, return a filtered list' +
	 *		'  var args = [displayValue + "%", displayValue + "%"]' +
	 *		'  return databaseManager.getDataSetByQuery("example_data", "select firstname + ' ' + lastname, employeeid from employees where firstname like ? or lastname like ?", args, 100);' +
	 *		'} else if (realValue != null) {' +
	 *		'  // TODO think about caching this result. can be called often!' +
	 *		'  // real object not found in the current list, return 1 row with display,realvalue that will be added to the current list' +
	 *		'  // dont return a complete list in this mode because that will be added to the list that is already there' +
	 *		'  var args = [realValue];' +
	 *		'  return databaseManager.getDataSetByQuery("example_data", "select firstname + ' ' + lastname, employeeid from employees where employeeid = ?", args, 1);' +
	 *		'}' +
	 *	'}');
	 * var vlist = solutionModel.newValueList('vlist', JSValueList.CUSTOM_VALUES);
	 * vlist.globalMethod = listProvider;
	 */
	public JSMethod js_getGlobalMethod()
	{
		String values = valuelist.getCustomValues();
		if (values != null && values.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			ScriptMethod scriptMethod = application.getFlattenedSolution().getScriptMethod(values.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length()));
			if (scriptMethod != null)
			{
				return new JSMethod(application, scriptMethod, true);
			}
		}
		return null;
	}


	public void js_setName(String arg)
	{
		checkModification();
		try
		{
			valuelist.updateName(new ScriptNameValidator(application.getFlattenedSolution()), arg);
		}
		catch (RepositoryException e)
		{
			Debug.error("Failed to update name of valuelist to '" + arg + "'.", e);
		}
	}

	public void js_setRelationName(String arg)
	{
		checkModification();
		valuelist.setRelationName(arg);
	}

	@Deprecated
	public void js_setRelationNMName(String arg)
	{
		checkModification();
		valuelist.setRelationNMName(arg);
	}

	public void js_setSeparator(String arg)
	{
		checkModification();
		valuelist.setSeparator(arg);
	}

	public void js_setServerName(String arg)
	{
		checkModification();
		valuelist.setServerName(arg);
	}

	public void js_setSortOptions(String arg)
	{
		checkModification();
		valuelist.setSortOptions(arg);
	}

	public void js_setTableName(String arg)
	{
		checkModification();
		valuelist.setTableName(arg);
	}

	public void js_setDataSource(String arg)
	{
		// check syntax, do not accept invalid URIs
		if (arg != null)
		{
			try
			{
				new URI(arg);
			}
			catch (URISyntaxException e)
			{
				throw new RuntimeException("Invalid dataSourc1e URI: '" + arg + "' :" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		checkModification();
		valuelist.setDataSource(arg);
	}

	public void js_setUseTableFilter(boolean arg)
	{
		checkModification();
		valuelist.setUseTableFilter(arg);
	}

	public void js_setValueListType(int arg)
	{
		checkModification();
		valuelist.setValueListType(arg);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Valuelist: " + valuelist.getName(); //$NON-NLS-1$
	}
}
