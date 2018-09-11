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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMValueList;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.solutionmodel.ISMValueList;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSValueList implements IConstantsObject, ISMValueList
{
	private ValueList valuelist;
	private final IApplication application;
	private boolean isCopy;

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
	@JSGetter
	public int getAddEmptyValue()
	{
		return valuelist.getAddEmptyValue();
	}

	@JSSetter
	public void setAddEmptyValue(int arg)
	{
		checkModification();
		valuelist.setAddEmptyValue(arg);
	}

	protected void setDisplayDataProviderIdsInternal(String[] ids)
	{
		checkModification();
		if (ids != null && ids.length > 3) throw new IllegalArgumentException("max 3 ids allowed in the display dataproviders ids"); //$NON-NLS-1$
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
				String id = ids[i];
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

	/**
	 * @clonedesc setDisplayDataProviderIds(String, String, String)
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 */
	@JSFunction
	public void setDisplayDataProviderIds()
	{
		setDisplayDataProviderIdsInternal(null);
	}

	/**
	 * @clonedesc setDisplayDataProviderIds(String, String, String)
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 */
	@JSFunction
	public void setDisplayDataProviderIds(String dataprovider1)
	{
		setDisplayDataProviderIdsInternal(new String[] { dataprovider1 });
	}

	/**
	 * @clonedesc setDisplayDataProviderIds(String, String, String)
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 *
	 * @param dataprovider2 The second display dataprovider.
	 */
	@JSFunction
	public void setDisplayDataProviderIds(String dataprovider1, String dataprovider2)
	{
		setDisplayDataProviderIdsInternal(new String[] { dataprovider1, dataprovider2 });
	}

	/**
	 * Set the display dataproviders. There can be at most 3 of them, combined with the return dataproviders.
	 * The values taken from these dataproviders, in order, separated by the separator, will be displayed
	 * by the valuelist.
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 * @param dataprovider1 The first display dataprovider.
	 *
	 * @param dataprovider2 The second display dataprovider.
	 *
	 * @param dataprovider3 The third display dataprovider.
	 */
	@JSFunction
	public void setDisplayDataProviderIds(String dataprovider1, String dataprovider2, String dataprovider3)
	{
		setDisplayDataProviderIdsInternal(new String[] { dataprovider1, dataprovider2, dataprovider3 });
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
	@JSFunction
	public Object[] getDisplayDataProviderIds()
	{
		return getDataProviders(valuelist.getShowDataProviders());
	}

	protected void setReturnDataProviderIdsInternal(String[] ids)
	{
		checkModification();
		if (ids != null && ids.length > 3) throw new IllegalArgumentException("max 3 ids allowed in the display dataproviders ids"); //$NON-NLS-1$
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
				String id = ids[i];
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
	 * @clonedesc setReturnDataProviderIds(String, String, String)
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 */
	@JSFunction
	public void setReturnDataProviderIds()
	{
		setReturnDataProviderIdsInternal(null);
	}

	/**
	 * @clonedesc setReturnDataProviderIds(String, String, String)
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 */
	@JSFunction
	public void setReturnDataProviderIds(String dataprovider1)
	{
		setReturnDataProviderIdsInternal(new String[] { dataprovider1 });
	}

	/**
	 * @clonedesc setReturnDataProviderIds(String, String, String)
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 *
	 * @param dataprovider2 The second return dataprovider.
	 */
	@JSFunction
	public void setReturnDataProviderIds(String dataprovider1, String dataprovider2)
	{
		setReturnDataProviderIdsInternal(new String[] { dataprovider1, dataprovider2 });
	}

	/**
	 * Set the return dataproviders. There can be at most 3 of them, combined with the display dataproviders.
	 * The values taken from these dataproviders, in order, separated by the separator, will be returned
	 * by the valuelist.
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 *
	 * @param dataprovider1 The first return dataprovider.
	 *
	 * @param dataprovider2 The second return dataprovider.
	 *
	 * @param dataprovider3 The third return dataprovider.
	 */
	@JSFunction
	public void setReturnDataProviderIds(String dataprovider1, String dataprovider2, String dataprovider3)
	{
		setReturnDataProviderIdsInternal(new String[] { dataprovider1, dataprovider2, dataprovider3 });
	}

	/**
	 * Returns an array of the dataproviders that will be used to define the valuelist value that is saved.
	 *
	 * @sampleas getDisplayDataProviderIds()
	 *
	 * @return An array of Strings representing the names of the return dataprovider.
	 */
	@JSFunction
	public Object[] getReturnDataProviderIds()
	{
		return getDataProviders(valuelist.getReturnDataProviders());
	}

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
	 * var combo1 = form.newComboBox("scopes.globals.text",10,10,120,20);
	 * combo1.valuelist = vl1;
	 * var combo2 = form.newComboBox("scopes.globals.id",10,60,120,20);
	 * combo2.valuelist = vl2;
	 */
	@JSGetter
	public String getCustomValues()
	{
		return valuelist.getCustomValues();
	}

	@JSSetter
	public void setCustomValues(String arg)
	{
		checkModification();
		valuelist.setCustomValues(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getName()
	 *
	 * @sampleas getUseTableFilter()
	 */
	@JSGetter
	public String getName()
	{
		return valuelist.getName();
	}

	@JSSetter
	public void setName(String arg)
	{
		checkModification();
		try
		{
			valuelist.updateName(new ScriptNameValidator(application.getFlattenedSolution()), arg);
		}
		catch (RepositoryException e)
		{
			Debug.error("Failed to update name of valuelist to '" + arg + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

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
	@JSGetter
	public String getRelationName()
	{
		return valuelist.getRelationName();
	}

	@JSSetter
	public void setRelationName(String arg)
	{
		checkModification();
		valuelist.setRelationName(arg);
	}

	/**
	 * @deprecated relationName supports multiple levels relations
	 */
	@Deprecated
	public String js_getRelationNMName()
	{
		return valuelist.getRelationNMName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getSeparator()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 */
	@JSGetter
	public String getSeparator()
	{
		return valuelist.getSeparator();
	}

	@JSSetter
	public void setSeparator(String arg)
	{
		checkModification();
		valuelist.setSeparator(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getServerName()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 */
	@JSGetter
	public String getServerName()
	{
		return valuelist.getServerName();
	}

	@JSSetter
	public void setServerName(String arg)
	{
		checkModification();
		valuelist.setServerName(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getSortOptions()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 */
	@JSGetter
	public String getSortOptions()
	{
		return valuelist.getSortOptions();
	}

	@JSSetter
	public void setSortOptions(String arg)
	{
		checkModification();
		valuelist.setSortOptions(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getTableName()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 */
	@JSGetter
	public String getTableName()
	{
		return valuelist.getTableName();
	}

	@JSSetter
	public void setTableName(String arg)
	{
		checkModification();
		valuelist.setTableName(arg);
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
	@JSGetter
	public String getDataSource()
	{
		return valuelist.getDataSource();
	}

	@JSSetter
	public void setDataSource(String arg)
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
	@JSGetter
	public boolean getUseTableFilter()
	{
		return valuelist.getUseTableFilter();
	}

	@JSSetter
	public void setUseTableFilter(boolean arg)
	{
		checkModification();
		valuelist.setUseTableFilter(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ValueList#getValueListType()
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMValueList#DATABASE_VALUES
	 */
	@JSGetter
	public int getValueListType()
	{
		return valuelist.getValueListType();
	}

	@JSSetter
	public void setValueListType(int arg)
	{
		checkModification();
		valuelist.setValueListType(arg);
	}

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
	 * The last argument is rawDisplayValue which contains the same text as displayValue but without converting it to lowercase.
	 *
	 * @sample
	 * var listProvider = solutionModel.newGlobalMethod('globals', 'function getDataSetForValueList(displayValue, realValue, record, valueListName, findMode, rawDisplayValue) {' +
	 * 		'	' +
	 * 		'var args = null;' +
	 * 		'var query = datasources.db.example_data.employees.createSelect();' +
	 * 		'/** @type  {JSDataSet} *&#47;' +
	 * 		'var result = null;' +
	 * 		'if (displayValue == null && realValue == null) {' +
	 * 		'  // TODO think about caching this result. can be called often!' +
	 * 		'  // return the complete list' +
	 * 		'  query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname)).add(query.columns.employeeid);' +
	 * 		'  result = databaseManager.getDataSetByQuery(query,100);' +
	 * 		'} else if (displayValue != null) {' +
	 * 		'  // TYPE_AHEAD filter call, return a filtered list' +
	 * 		'  args = [displayValue + "%", displayValue + "%"]' +
	 * 		'  query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname)).add(query.columns.employeeid).' +
	 * 		'  root.where.add(query.or.add(query.columns.firstname.lower.like(args[0] + '%')).add(query.columns.lastname.lower.like(args[1] + '%')));' +
	 * 		'  result = databaseManager.getDataSetByQuery(query,100);' +
	 * 		'} else if (realValue != null) {' +
	 * 		'  // TODO think about caching this result. can be called often!' +
	 * 		'  // real object not found in the current list, return 1 row with display,realvalue that will be added to the current list' +
	 * 		'  // dont return a complete list in this mode because that will be added to the list that is already there' +
	 * 		'  args = [realValue];' +
	 * 		'  query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname)).add(query.columns.employeeid).' +
	 * 		'  root.where.add(query.columns.employeeid.eq(args[0]));' +
	 * 		'  result = databaseManager.getDataSetByQuery(query,1);' +
	 * 		'}' +
	 * 		'return result;' +
	 * 		'}');
	 * var vlist = solutionModel.newValueList('vlist', JSValueList.CUSTOM_VALUES);
	 * vlist.globalMethod = listProvider;
	 */
	@JSGetter
	public JSMethod getGlobalMethod()
	{
		String uuid = valuelist.getCustomValues();
		ScriptMethod scriptMethod = application.getFlattenedSolution().getScriptMethod(uuid);
		if (scriptMethod != null)
		{
			return new JSMethod(scriptMethod, application, true);
		}
		return null;
	}

	@JSSetter
	public void setGlobalMethod(IBaseSMMethod method)
	{
		checkModification();
		if (method == null)
		{
			valuelist.setCustomValues(null);
			valuelist.setValueListType(IValueListConstants.CUSTOM_VALUES);
		}
		else
		{
			ScriptMethod scriptMethod = ((JSMethod)method).getScriptMethod();
			if (scriptMethod.getParent() instanceof Solution)
			{
				valuelist.setCustomValues(scriptMethod.getUUID().toString());
				valuelist.setValueListType(IValueListConstants.GLOBAL_METHOD_VALUES);
			}
			else
			{
				throw new RuntimeException("Only global methods are supported for a valuelist"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Gets or sets the fallback valuelist.
	 *
	 * @sample
	 * var myValueList = solutionModel.getValueList('myValueListHere')
	 *  //get fallback value list
	 * var fallbackValueList = myValueList.fallbackValueList
	 */
	@JSGetter
	public JSValueList getFallbackValueList()
	{

		FlattenedSolution fs = application.getFlattenedSolution();
		int fallbackVLID = valuelist.getFallbackValueListID();
		ValueList fallbackVL = fs.getValueList(fallbackVLID);
		if (fallbackVL != null)
		{
			return new JSValueList(fallbackVL, application, false);
		}
		else
		{
			return null;
		}
	}

	@JSGetter
	public void setFallbackValueList(IBaseSMValueList vl)
	{
		checkModification();
		if (vl == null)
		{
			valuelist.setFallbackValueListID(0);
		}
		else
		{
			valuelist.setFallbackValueListID(((JSValueList)vl).getValueList().getID());
		}
	}

	@Deprecated
	public void js_setRelationNMName(String arg)
	{
		checkModification();
		valuelist.setRelationNMName(arg);
	}

	/**
	 * Returns the UUID of the value list
	 *
	 * @sample
	 * var vlist = solutionModel.newValueList('options', JSValueList.CUSTOM_VALUES);
	 * application.output(vlist.getUUID().toString());
	 */
	@JSFunction
	public UUID getUUID()
	{
		return valuelist.getUUID();
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		int type = valuelist.getValueListType();
		String typeString = "";
		switch (type)
		{
			case IValueListConstants.CUSTOM_VALUES :
				typeString = "Custom";
				break;
			case IValueListConstants.GLOBAL_METHOD_VALUES :
				ScriptMethod globalMethod = application.getFlattenedSolution().getScriptMethod(valuelist.getCustomValues());
				typeString = "GlobalMethod:" + globalMethod != null ? globalMethod.getPrefixedName() : valuelist.getCustomValues();
				break;
			case IValueListConstants.TABLE_VALUES :
				typeString = valuelist.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES ? "Table:" + valuelist.getDataSource()
					: "Related:" + valuelist.getRelationName();
		}
		return "JSValueList[name:" + valuelist.getName() + ',' + typeString + ']';
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((valuelist == null) ? 0 : valuelist.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSValueList other = (JSValueList)obj;
		if (valuelist == null)
		{
			if (other.valuelist != null) return false;
		}
		else if (!valuelist.getUUID().equals(other.valuelist.getUUID())) return false;
		return true;
	}

	/**
	 * Gets or sets the display value type with one of the types
	 * defined on JSVariable
	 *
	 * @sample
	 * var myValueList = solutionModel.getValueList('myValueListHere')
	 *  //set the display value type
	 * myValueList.displayValueType = JSVariable.TEXT
	 */
	@JSSetter
	@Override
	public void setDisplayValueType(int arg)
	{
		valuelist.setDisplayValueType(arg);
	}

	@JSGetter
	@Override
	public int getDisplayValueType()
	{
		return valuelist.getDisplayValueType();
	}

	/**
	 * Gets or sets the real value type with one of the types
	 * defined on JSVariable
	 *
	 * @sample
	 * var myValueList = solutionModel.getValueList('myValueListHere')
	 *  // set the real value type
	 * myValueList.realValueType = JSVariable.NUMBER
	 */
	@JSSetter
	@Override
	public void setRealValueType(int arg)
	{
		valuelist.setRealValueType(arg);
	}

	@JSGetter
	@Override
	public int getRealValueType()
	{
		return valuelist.getRealValueType();
	}
}
