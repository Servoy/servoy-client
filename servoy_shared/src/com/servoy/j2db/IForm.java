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
package com.servoy.j2db;


import java.awt.print.PrinterJob;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.ServoyException;

/**
 * Interface for manipulating a form
 *
 * @author jblok
 */
public interface IForm
{

	/**
	 * Constant used for form selectionMode property. It means that the foundset's multiSelect property is used.
	 */
	public static final int SELECTION_MODE_DEFAULT = ContentSpec.ZERO.intValue();

	/**
	 * Constant used for form selectionMode property. It means that the form will force multiSelect to false on the foundset it uses.
	 */
	public static final int SELECTION_MODE_SINGLE = 1;

	/**
	 * Constant used for form selectionMode property. It means that the form will force multiSelect to true on the foundset it uses.
	 */
	public static final int SELECTION_MODE_MULTI = 2;

	/**
	 * Constant for method setView(...) to show in recordview.
	 */
	public static final int RECORD_VIEW = IFormConstants.VIEW_TYPE_RECORD;

	/**
	 * Constant for locked record view.
	 */
	public static final int LOCKED_RECORD_VIEW = IFormConstants.VIEW_TYPE_RECORD_LOCKED;

	/**
	 * Constant for method setView(...) to show in listview.
	 */
	public static final int LIST_VIEW = IFormConstants.VIEW_TYPE_LIST;

	/**
	 * Eval a javascript
	 *
	 * @since Servoy 3.5
	 */
	public Object eval(String javascript);

	/**
	 * When using a form as an external component, this flag has to be set and cleared when done with it.
	 * If you call this in the webclient then the returning component will be a FormUI wicket component with the wicket:id of "webform"
	 * So you have to add a placeholder in your bean html file like: &lt;div wicket:id="webform"&gt;&lt;/div&gt;
	 *
	 * @param visibleExternal boolean if it will be showing external or not
	 *
	 * @since Servoy 2.2
	 */
	public Object setUsingAsExternalComponent(boolean visibleExternal) throws ServoyException;

	/**
	 * Show all records from table.
	 */
	public void loadAllRecords();

	/**
	 * Method to check if this form had record data.
	 *
	 * @return boolean normally returs true, if not use loadAllRecords().
	 */
	public boolean isShowingData();

	/**
	 * Delete current record.
	 */
	public boolean deleteRecord();

	/**
	 * Delete all records in foundset.
	 */
	public boolean deleteAllRecords();

	/**
	 * Make new record.
	 */
	public void newRecord();

	/**
	 * Duplicate current record.
	 */
	public void duplicateRecord();

	/**
	 * Enter the find.
	 */
	public void find();

	/**
	 * Check if in find.
	 */
	public boolean isInFindMode();

	/**
	 * Prevents always doing a find in find.
	 */
	public void removeLastFound();

	/**
	 * Perform a find (called search() in javascript).
	 *
	 * @param reduce true if find in find, otherwise it is expand search
	 * @param showDialogOnNoResults true shows dialog
	 */
	public int performFind(boolean clear, boolean reduce, boolean showDialogOnNoResults);

	/**
	 * Omit a record.
	 */
	public void omitRecord();

	/**
	 * Show all omitted records as foundset.
	 */
	public void showOmittedRecords();

	/**
	 * Invert the foundset.
	 */
	public void invertRecords();

	/**
	 * Show the sort dialog.
	 *
	 * @throws Exception
	 */
	public void showSortDialog() throws Exception;

	/**
	 * do reLookupValues on current record.
	 */
	public void reLookupValues();

	/**
	 * Store changes in DB.
	 *
	 * @deprecated
	 */
	@Deprecated
	public void saveData();

	/**
	 * preview current records.
	 */
	public void printPreview();

	/**
	 * Print records use.
	 */
	public void print(boolean showManyRecordsDialog, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob);

	/**
	 * get the current view (see constants).
	 *
	 * @return the view type
	 */
	public int getView();

	/**
	 * Set the current view (see constants).
	 *
	 * @param v
	 */
	public void setView(int v);

	@Deprecated
	public ITable getTable();

	/**
	 * @since Servoy 5.0
	 */
	public String getDataSource();

	/**
	 * Get the form name
	 *
	 * @since 3.5
	 */
	public String getName();

	/**
	 * @return the current foundset
	 * @since 5.0
	 */
	public IFoundSet getFoundSet();


	/**
	 * Gets the read-only state of a form; also known as "editable".
	 * Note: The field(s) in a form set as read-only can be selected and the field data can be copied to clipboard.
	 *
	 * @since 6.1
	 */
	public boolean isReadOnly();

	/**
	 * @return the ui component of this form.
	 *
	 * @since 6.1
	 */
	public IFormUI getFormUI();

	/**
	 * @return the named elements of this form.
	 *
	 * @since 6.1
	 */
	public IRuntimeComponent[] getElements();

	/**
	 * @return Load the controller with the foundset.
	 *
	 * @param foundSet foundset to load
	 *
	 * @since 6.1
	 */
	public boolean loadRecords(IFoundSet foundSet);

	/**
	 * @param key the design time property name
	 *
	 * @return Get a design-time property of a form.
	 *
	 * @since 6.1
	 */
	public Object getDesignTimeProperty(String key);
}
