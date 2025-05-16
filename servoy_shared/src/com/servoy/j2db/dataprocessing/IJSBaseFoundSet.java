/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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


import java.util.ArrayList;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

/**
 * <p><code>JSBaseFoundSet</code> provides a unified interface for working with foundsets, exposing
 * shared functionalities for regular and view foundsets. It supports key operations such as
 * iteration, selection, and data source retrieval. The multi-select mode allows multiple records
 * to be handled simultaneously. This abstraction ensures consistency across various foundset
 * types.</p>
 *
 * <h2>Features</h2>
 * <p><code>JSBaseFoundSet</code> allows iteration over records with the <code>forEach</code>
 * method, which handles dynamic loading while respecting Servoy’s lazy-loading mechanism. Callback
 * functions can interact with records while accommodating changes like inserts and deletions.
 * Exceptions ensure stability when the foundset is modified during iteration.</p>
 *
 * <p>The class provides methods to retrieve details about the data source and selected records.
 * Methods like <code>getDataSource</code> return the underlying data source, while
 * <code>getSelectedIndex</code> and <code>getSelectedIndexes</code> provide details about
 * currently selected records. The size of the cached row identifiers can be retrieved using
 * <code>getSize</code>.</p>
 *
 * <h2>Selection Management</h2>
 * <p><code>JSBaseFoundSet</code> includes methods for managing record selection. Developers can
 * set or retrieve the currently selected record index with <code>setSelectedIndex</code> and
 * <code>getSelectedIndex</code>. For multi-select scenarios, <code>setSelectedIndexes</code> and
 * <code>getSelectedIndexes</code> enable handling multiple selected records.</p>
 *
 * <p>For more information, refer to the <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/jsfoundset">FoundSet</a> documentation.</p>
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSBaseFoundSet", scriptingName = "JSBaseFoundSet")
public interface IJSBaseFoundSet extends IFoundSetInternal
{
	@JSSetter
	void setMultiSelect(boolean multiSelect);

	/**
	 * @return true if the foundset is in multi-select mode; false otherwise.
	 */
	@JSGetter
	boolean isMultiSelect();

	/**
	 * @return the data source of the foundset as a string.
	 */
	@JSFunction
	String getDataSource();

	/**
	 * @return the name of the foundset.
	 */
	@JSFunction
	String getName();

	/**
	 * Get the record index. Will return -1 if the record can't be found.
	 *
	 * @sample var index = %%prefix%%foundset.getRecordIndex(record);
	 *
	 * @param record Record
	 *
	 * @return int index.
	 */
	default public int jsFunction_getRecordIndex(IJSBaseRecord record)
	{
		int recordIndex = getRecordIndex((IRecord)record);
		if (recordIndex == -1) return -1;
		return recordIndex + 1;
	}


	/**
	 * @return an array of currently selected records in the foundset.
	 */
	@JSFunction
	public IJSBaseRecord[] getSelectedRecords();

	/**
	 * @return the number of records in the foundset.
	 */
	@JSFunction
	public int getSize();

	/**
	 * Iterates over the records of a foundset taking into account inserts and deletes that may happen at the same time.
	 * It will dynamically load all records in the foundset (using Servoy lazy loading mechanism). If callback function returns a non null value the traversal will be stopped and that value is returned.
	 * If no value is returned all records of the foundset will be traversed. Foundset modifications( like sort, omit...) cannot be performed in the callback function.
	 * If foundset is modified an exception will be thrown. This exception will also happen if a refresh happens because of a rollback call for records on this datasource when iterating.
	 * When an exception is thrown from the callback function, the iteration over the foundset will be stopped.
	 *
	 * @sample
	 *  foundset.forEach(function(record,recordIndex,foundset) {
	 *  	//handle the record here
	 *  });
	 *
	 * @param callback The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.
	 *
	 * @return Object the return value of the callback
	 *
	 */
	@JSFunction
	default Object forEach(Function callback)
	{
		return forEach(new CallJavaScriptCallBack(callback, getFoundSetManager().getApplication().getScriptEngine(), null));
	}

	/**
	 * @clonedesc forEach(Function)
	 *
	 * @sampleas forEach(Function)
	 *
	 * @param callback The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.
	 * @param thisObject What the this object should be in the callback function (default it is the foundset)
	 *
	 * @return Object the return value of the callback
	 *
	 */
	@JSFunction
	default Object forEach(Function callback, Scriptable thisObject)
	{
		return forEach(new CallJavaScriptCallBack(callback, getFoundSetManager().getApplication().getScriptEngine(), thisObject));
	}

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
	default int jsFunction_getSelectedIndex()
	{
		return getSelectedIndex() + 1;
	}

	/**
	 * Set the current record index.
	 *
	 * @sampleas jsFunction_getSelectedIndex()
	 *
	 * @param index int index to set (1-based)
	 */
	default void jsFunction_setSelectedIndex(int index)
	{
		if (index >= 1 && index <= getSize())
		{
			setSelectedIndex(index - 1);
		}
	}

	/**
	 * Get the indexes of the selected records.
	 * When the foundset is in multiSelect mode (see property multiSelect), a selection can consist of more than one index.
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
	default Number[] jsFunction_getSelectedIndexes()
	{
		Number[] selected = null;
		int[] selectedIndexes = getSelectedIndexes();
		if (selectedIndexes != null && selectedIndexes.length > 0)
		{
			selected = new Number[selectedIndexes.length];
			for (int i = 0; i < selectedIndexes.length; i++)
			{
				selected[i] = Integer.valueOf(selectedIndexes[i] + 1);
			}
		}

		return selected;
	}

	/**
	 * Set the selected records indexes.
	 *
	 * @sampleas jsFunction_getSelectedIndexes()
	 *
	 * @param indexes An array with indexes to set.
	 */
	default void jsFunction_setSelectedIndexes(Number[] indexes)
	{
		if (indexes == null || indexes.length == 0) return;
		ArrayList<Integer> selectedIndexes = new ArrayList<Integer>();

		Integer i;
		for (Object index : indexes)
		{
			i = Integer.valueOf(Utils.getAsInteger(index));
			if (selectedIndexes.indexOf(i) == -1) selectedIndexes.add(i);
		}
		int[] iSelectedIndexes = new int[selectedIndexes.size()];
		for (int j = 0; j < selectedIndexes.size(); j++)
		{
			iSelectedIndexes[j] = selectedIndexes.get(j).intValue() - 1;
		}
		setSelectedIndexes(iSelectedIndexes);
	}

}