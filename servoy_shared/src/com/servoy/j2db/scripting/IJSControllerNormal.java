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

package com.servoy.j2db.scripting;

import java.awt.print.PrinterJob;

import org.mozilla.javascript.Function;

import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.api.IJSController;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;


/**
 * @author jcompagner
 *
 */
public interface IJSControllerNormal extends IJSController
{

	/**
	 * Returns the JSWindow that the form is shown in, or null if the form is not currently showing in a window.
	 * 
	 * @sample
	 * var currentWindow = controller.getWindow();
	 * if (currentWindow != null) {
	 * 	currentWindow.title = 'We have a new title';
	 * } else {
	 * 	currentWindow = application.createWindow("Window Name", JSWindow.WINDOW, null);
	 * 	currentWindow(650, 700, 450, 350);
	 * 	currentWindow = "Window Title";
	 * 	controller.show(currentWindow);
	 * }
	 * 
	 * @return the JSWindow that the form is shown in, or null if the form is not currently showing in a window.
	 */
	public JSWindow js_getWindow();

	/**
	 * Shows the form (makes the form visible)
	 * This function does not affect the form foundset in any way.
	 * 
	 * @sample
	 * // show the form in the current window/dialog
	 * %%prefix%%controller.show();
	 * // show the form in newly created named modal dialog
	 * var w = application.createWindow("mydialog", JSWindow.MODAL_DIALOG);
	 * %%prefix%%controller.show(w);
	 * // show the form in an existing window/dialog
	 * var w = application.getWindow("mydialog"); // use null name for main app. window
	 * %%prefix%%controller.show(w);
	 * // or %%prefix%%controller.show("mydialog");
	 * //show the form in the main window
	 * //%%prefix%%controller.show(null);
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 *
	 */
	// Deprecated implementation:
	// Shows the form (makes the form visible), optionally shown in the specified (modal or not) dialog.
	// @sample
	// //show the form in the current window/dialog
	// %%prefix%%controller.show();
	// //show the form in the named modal dialog 
	// //%%prefix%%controller.show('mydialog',true);
	//
	// @param dialogName optional the dialog/window name
	// @param modal optional boolean indicating modality for dialogs; default value is false
	public void js_show() throws ServoyException;

	/**
	 * @clonedesc js_show()
	 * @sampleas js_show()
	 * 
	 * @param window the window in which this form should be shown, specified by the name of an existing window
	 * 
	 * @throws ServoyException
	 */
	public void js_show(String window) throws ServoyException;

	/**
	 * @clonedesc js_show()
	 * @sampleas js_show()
	 * 
	 * @param window the window in which this form should be shown, given as a window object
	 * 
	 * @throws ServoyException
	 */
	public void js_show(JSWindow window) throws ServoyException;

	/**
	 * Load data into the form and shows the form, is a shortcut for the functions 'loadRecords' and 'show'.
	 *
	 * @sample
	 * %%prefix%%controller.showRecords(foundset);
	 * // load foundset & show the form in newly created named modal dialog
	 * var w = application.createWindow("mydialog", JSWindow.MODAL_DIALOG);
	 * %%prefix%%controller.showRecords(foundset, w);
	 * // load foundset & show the form in an existing window/dialog
	 * var w = application.getWindow("mydialog"); // use null name for main app. window
	 * %%prefix%%controller.showRecords(foundset, w);
	 * //%%prefix%%controller.showRecords(foundset, "mydialog");
	 * 
	 * @param foundset the foundset to load before showing the form.
	 */
	// Deprecated implementation:
	// //show the form in the named modal dialog 
	// //%%prefix%%controller.show(foundset, 'mydialog', true);
	// @param data the foundset/pkdataset/singleNumber_pk/UUIDpk to load before showing the form
	// @param dialogName optional the dialog name
	// @param modal optional boolean indicating modality for dialogs; default value is false
	//
	public void js_showRecords(FoundSet foundset) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(FoundSet)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 * 
	 * @param foundset the foundset to load before showing the form.
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(FoundSet foundset, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(FoundSet)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 * 
	 * @param foundset the foundset to load before showing the form.
	 * @param window the window in which this form should be shown, given as a window object.
	 */
	public void js_showRecords(FoundSet foundset, JSWindow window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @param pkdataset the pkdataset to load before showing the form.
	 */
	public void js_showRecords(JSDataSet pkdataset) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(JSDataSet)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param pkdataset the pkdataset to load before showing the form.
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(JSDataSet pkdataset, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(JSDataSet)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param pkdataset the pkdataset to load before showing the form.
	 * @param window the window in which this form should be shown, given as a window object.
	 */
	public void js_showRecords(JSDataSet pkdataset, JSWindow window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 */
	public void js_showRecords(QBSelect query) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(QBSelect query, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 * @param window the window in which this form should be shown, given as a window object.
	 */
	public void js_showRecords(QBSelect query, JSWindow window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @param singleNumber_pk the singleNumber_pk to load before showing the form.
	 */
	public void js_showRecords(Number singleNumber_pk) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(Number)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param singleNumber_pk the singleNumber_pk to load before showing the form.
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(Number singleNumber_pk, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(Number)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param singleNumber_pk the singleNumber_pk to load before showing the form.
	 * @param window the window in which this form should be shown, given as a window object
	 */
	public void js_showRecords(Number singleNumber_pk, JSWindow window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @param query the query to load before showing the form.
	 */
	public void js_showRecords(String query) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @param query the query to load before showing the form.
	 * @param argumentsArray the array of arguments for the query
	 */
	public void js_showRecords(String query, Object[] argumentsArray) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(String query, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 * @param argumentsArray the array of arguments for the query
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(String query, Object[] argumentsArray, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 * @param window the window in which this form should be shown, given as a window object
	 */
	public void js_showRecords(String query, JSWindow window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object)
	 *  
	 * @param query the query to load before showing the form.
	 * @param argumentsArray the array of arguments for the query
	 * @param window the window in which this form should be shown, given as a window object
	 */
	public void js_showRecords(String query, Object[] argumentsArray, JSWindow window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @param UUIDpk the UUIDpk to load before showing the form.
	 */
	public void js_showRecords(UUID UUIDpk) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(UUID)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object) 
	 * 
	 * @param UUIDpk the UUIDpk to load before showing the form.
	 * @param window the window in which this form should be shown, specified by the name of an existing window.
	 */
	public void js_showRecords(UUID UUIDpk, String window) throws ServoyException;

	/**
	 * @clonedesc js_showRecords(FoundSet)
	 * @sampleas js_showRecords(FoundSet)
	 * 
	 * @see com.servoy.j2db.scripting.JSApplication#js_createWindow(String, int)
	 * @see com.servoy.j2db.scripting.JSApplication#js_getWindow(String)
	 * @see com.servoy.j2db.FormController$JSForm#js_loadRecords(UUID)
	 * @see com.servoy.j2db.FormController$JSForm#js_show(Object) 
	 * 
	 * @param UUIDpk the UUIDpk to load before showing the form.
	 * @param window the window in which this form should be shown, given as a window object.
	 */
	public void js_showRecords(UUID UUIDpk, JSWindow window) throws ServoyException;

	/**
	 * Loads all accessible records from the datasource into the form foundset.
	 * When the form contains a related foundset it will be replaced by a default foundset on same datasource.
	 * 
	 * Notes: 
	 * -the default foundset is always limited by filters, if databaseManager.addFoundSetFilterParam function is used.
	 * -typical use is loading the normal foundset again after form usage in a related tabpanel
	 *
	 * @sample %%prefix%%controller.loadAllRecords();
	 * 
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_addTableFilterParam(String, String, String, String, Object, String)
	 * @return true if successful
	 */
	public boolean js_loadAllRecords() throws ServoyException;

	/**
	 * Loads all accessible records from the datasource into the form foundset. Typical usage is loading records after search in related tabpanel. 
	 * The difference to loadAllRecords() is that related foundset will load related records.
	 * 
	 * @sample
	 * //to reload all last (related) records again, if for example after a search in related tabpanel
	 * %%prefix%%controller.loadRecords();
	 * 
	 * @return true if successful
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_loadRecords()
	 */
	public boolean js_loadRecords() throws ServoyException;

	/**
	 * Loads a (related) foundset into the form.
	 * The form will no longer share the default foundset with forms of the same datasource, use loadAllRecords to restore the default foundset.
	 * 
	 * @sample
	 * //to load a (related)foundset into the form.
	 * //the form will no longer share the default foundset with forms of the same datasource, use loadAllRecords to restore the default foundset 
	 * %%prefix%%controller.loadRecords(order_to_orderdetails);
	 * 
	 * @param foundset to load
	 * @return true if successful
	 */
	public boolean js_loadRecords(FoundSet foundset);

	/**
	 * Loads a primary key dataset, will remove related sort.
	 * 
	 * @sample
	 * //to load a primary key dataset, will remove related sort
	 * //var dataset = databaseManager.getDataSetByQuery(...);
	 * // dataset must match the table primary key columns (alphabetically ordered)
	 * %%prefix%%controller.loadRecords(dataset);
	 * 
	 * @param pkdataset to load
	 * @return true if successful
	 */
	public boolean js_loadRecords(IDataSet pkdataset);

	/**
	 * Loads a single record by primary key, will remove related sort.
	 * 
	 * @sample
	 * %%prefix%%controller.loadRecords(123);
	 * 
	 * @param singlenNmber_pk to load
	 * @return true if successful
	 */
	public boolean js_loadRecords(Number singlenNmber_pk);

	/**
	 * @clonedesc js_loadRecords(Number)
	 * 
	 * @sample
	 * %%prefix%%controller.loadRecords(application.getUUID('6b5e2f5d-047e-45b3-80ee-3a32267b1f20'));
	 * 
	 * @param UUIDpk to load
	 * @return true if successful
	 */
	public boolean js_loadRecords(UUID UUIDpk);

	/**
	 * Loads records into form foundset based on a query (also known as 'Form by query'). The query must be a valid sql select.
	 * 
	 * @sample
	 * %%prefix%%controller.loadRecords(sqlstring);
	 * 
	 * @param queryString to load
	 * @return true if successful
	 */
	public boolean js_loadRecords(String queryString);

	/**
	 * @clonedesc js_loadRecords(String)
	 * 
	 * @sample
	 * %%prefix%%controller.loadRecords(sqlstring,parameters);
	 * 
	 * @param queryString to load
	 * @param queryArgumentsArray the arguments to replace the questions marks in the queryString
	 * @return true if successful
	 */
	public boolean js_loadRecords(String queryString, Object[] queryArgumentsArray);

	/**
	 * Returns the current cached record count of the current foundset. 
	 * To return the full foundset count, use: databaseManager.getFoundSetCount(...) 
	 * Tip: get the the table count of all rows in a table, use: databaseManager.getTableCount(...) 
	 *
	 * @sample
	 * for ( var i = 1 ; i <= %%prefix%%controller.getMaxRecordIndex() ; i++ )
	 * {
	 * 	%%prefix%%controller.setSelectedIndex(i);
	 * 	//do some action per record
	 * }
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getFoundSetCount(Object)
	 * @return the max record index
	 */
	public int js_getMaxRecordIndex();

	/**
	 * Get the name of this form.
	 *
	 * @sample var formName = %%prefix%%controller.getName();
	 * @return the name
	 */
	public String js_getName();

	/**
	 * Get the used datasource.
	 *
	 * @sample var dataSource = %%prefix%%controller.getDataSource();
	 * @return the datasource
	 */
	public String js_getDataSource();

	/**
	 * Returns the maximum length allowed in the specified dataprovider. 
	 *
	 * @sample %%prefix%%controller.getDataProviderMaxLength('name');
	 * @param name the dataprovider name
	 * @return the length
	 */
	public int js_getDataProviderMaxLength(String name) throws ServoyException;

	/**
	 * Sets focus to the first field of the form; based on tab order sequence.
	 *
	 * @sample %%prefix%%controller.focusFirstField();
	 * 
	 * @see focusField
	 */
	public void js_focusFirstField();

	/**
	 * Sets focus to a field specified by its name. 
	 * If the second parameter is set to true, then readonly fields will be skipped 
	 * (the focus will be set to the first non-readonly field located after the field with the specified name; the tab sequence is respected when searching for the non-readonly field).
	 *
	 * @sample
	 * var tabseq = %%prefix%%controller.getTabSequence();
	 * if (tabseq.length > 1) {
	 * 	// If there is more than one field in the tab sequence, 
	 * 	// focus the second one and skip over readonly fields.
	 * 	%%prefix%%controller.focusField(tabseq[1], true);
	 * }
	 * else {
	 * 	// If there is at most one field in the tab sequence, then focus
	 * 	// whatever field is first, and don't bother to skip over readonly fields.
	 * 	%%prefix%%controller.focusField(null, false);
	 * }
	 * @param fieldName the name of the field to be focussed
	 * @param skipReadonly boolean indication to skip read only fields, if the named field happens to be read only
	 */
	public void js_focusField(String fieldName, boolean skipReadonly);

	/**
	 * Recreates the forms UI components, to reflect the latest solution model.
	 * Use this after altering the elements via solutionModel at the JSForm of this form.
	 * 
	 * @sample
	 * // get the solution model JSForm 
	 * var form = solutionModel.getForm("myForm");
	 * // get the JSField of the form 
	 * var field = form.getField("myField");
	 * // alter the field
	 * field.x = field.x + 10;
	 * // recreate the runtime forms ui to reflect the changes.
	 * %%prefix%%controller.recreateUI();
	 * 
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm
	 * 
	 * @return true if successful
	 */
	public boolean js_recreateUI();

	/**
	 * Set the tab order sequence programatically, by passing the elements references in a javascript array.
	 *
	 * @sample %%prefix%%controller.setTabSequence([%%prefix%%elements.fld_order_id, %%prefix%%elements.fld_order_amount]);
	 * @param arrayOfElements array containing the element references
	 */
	public void js_setTabSequence(Object[] arrayOfElements);

	/**
	 * Get an array with the names of the components that are part of the tab sequence. 
	 * The order of the names respects the order of the tab sequence. 
	 * Components that are not named will not appear in the returned array, although they may be in the tab sequence.
	 *
	 * @sample
	 * var tabseq = %%prefix%%controller.getTabSequence();
	 * if (tabseq.length > 1) {
	 * 	// If there is more than one field in the tab sequence, 
	 * 	// focus the second one and skip over readonly fields.
	 * 	%%prefix%%controller.focusField(tabseq[1], true);
	 * }
	 * else {
	 * 	// If there is at most one field in the tab sequence, then focus
	 * 	// whatever field is first, and don't bother to skip over readonly fields.
	 * 	%%prefix%%controller.focusField(null, false);
	 * }
	 * @return array of names
	 */
	public String[] jsFunction_getTabSequence();

	/**
	 * Gets or sets the read-only state of a form; also known as "editable"
	 * 
	 * Note: The field(s) in a form set as read-only can be selected and the field data can be copied to clipboard. 
	 *
	 * @sample
	 * //gets the read-only state of the form
	 * var state = %%prefix%%controller.readOnly;
	 * //sets the read-only state of the form
	 * %%prefix%%controller.readOnly = true
	 */
	public boolean js_getReadOnly();

	public void js_setReadOnly(boolean b);

	/**
	 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_find()
	 * @sample
	 * if (%%prefix%%controller.find()) //find will fail if autosave is disabled and there are unsaved records
	 * {
	 * 	columnTextDataProvider = 'a search value'
	 * 	columnNumberDataProvider = '>10'
	 * 	columnDateDataProvider = '31-12-2010|dd-MM-yyyy'
	 * 	%%prefix%%controller.search()
	 * }
	 */
	public boolean js_find() throws ServoyException;

	/**
	 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_search()
	 * @sample
	 * var recordCount = %%prefix%%controller.search();
	 * //var recordCount = %%prefix%%controller.search(false,false); //to extend foundset
	 */
	public int js_search() throws ServoyException;

	/**
	 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_search(Boolean)
	 * 
	 * @param clearLastResults boolean, clear previous search, default true  
	 * 
	 * @return the recordCount
	 */
	public int js_search(boolean clearLastResults) throws ServoyException;

	/**
	 * @sameas com.servoy.j2db.dataprocessing.FoundSet#js_search(Boolean,Boolean)
	 * 
	 * @param clearLastResults boolean, clear previous search, default true  
	 * @param reduceSearch boolean, reduce (true) or extend (false) previous search results, default true
	 * 
	 * @return the recordCount
	 */
	public int js_search(boolean clearLastResults, boolean reduceSearch) throws ServoyException;

	/**
	 * Show this form in print preview.
	 *
	 * @sample
	 * //shows this form (with foundset records) in print preview
	 * %%prefix%%controller.showPrintPreview();
	 * //to print preview current record only
	 * //%%prefix%%controller.showPrintPreview(true);
	 * //to print preview current record only with 125% zoom factor; 
	 * //%%prefix%%controller.showPrintPreview(true, null, 125);
	 *
	 */
	public void js_showPrintPreview();

	/**
	 * @clonedesc js_showPrintPreview()
	 * @sampleas js_showPrintPreview()
	 * @param printCurrentRecordOnly to print the current record only
	 */
	public void js_showPrintPreview(boolean printCurrentRecordOnly);

	/**
	 * @clonedesc js_showPrintPreview()
	 * @sampleas js_showPrintPreview()
	 *
	 * @param printCurrentRecordOnly to print the current record only
	 * @param printerJob print to plugin printer job, see pdf printer plugin for example (incase print is used from printpreview)
	 * 
	 */
	public void js_showPrintPreview(boolean printCurrentRecordOnly, PrinterJob printerJob);

	/**
	 * @clonedesc js_showPrintPreview()
	 * @sampleas js_showPrintPreview()
	 *
	 * @param printCurrentRecordOnly to print the current record only
	 * @param printerJob print to plugin printer job, see pdf printer plugin for example (incase print is used from printpreview)
	 * @param zoomFactor a specified number value from 10-400
	 */
	public void js_showPrintPreview(boolean printCurrentRecordOnly, PrinterJob printerJob, int zoomFactor);

	/**
	 * Print this form with current foundset, without preview.
	 *
	 * @sample
	 * //print this form (with foundset records)
	 * %%prefix%%controller.print();
	 * //print only current record (no printerSelectDialog) to pdf plugin printer
	 * //%%prefix%%controller.print(true,false,plugins.pdf_output.getPDFPrinter('c:/temp/out.pdf'));
	 *
	 */
	public void js_print();

	/**
	 * @clonedesc js_print()
	 * @sampleas js_print()
	 * @param printCurrentRecordOnly to print the current record only
	 */
	public void js_print(boolean printCurrentRecordOnly);

	/**
	 * @clonedesc js_print()
	 * @sampleas js_print()
	 * @param printCurrentRecordOnly to print the current record only
	 * @param showPrinterSelectDialog boolean to show the printer select dialog (default printer is normally used)  
	 */
	public void js_print(boolean printCurrentRecordOnly, boolean showPrinterSelectDialog);

	/**
	 * @clonedesc js_print()
	 * @sampleas js_print()
	 * @param printCurrentRecordOnly to print the current record only
	 * @param showPrinterSelectDialog boolean to show the printer select dialog (default printer is normally used)  
	 * @param printerJob print to plugin printer job, see pdf printer plugin for example
	 */
	public void js_print(boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob);

	/**
	 * Print this form with current foundset records to xml format.
	 *
	 * @sample
	 * //TIP: see also plugins.file.writeXMLFile(...)
	 * var xml = %%prefix%%controller.printXML();
	 * //print only current record 
	 * //var xml = %%prefix%%controller.printXML(true);
	 *
	 * @return the XML 
	 */
	public String js_printXML();

	/**
	 * @clonedesc js_printXML()
	 * @sampleas js_printXML()
	 *
	 * @param printCurrentRecordOnly to print the current record only
	 * @return the XML 
	 */
	public String js_printXML(boolean printCurrentRecordOnly);

	/**
	 * Delete current selected record, deletes mulitple selected records incase the foundset is using multiselect.
	 *
	 * @sample
	 * var success = %%prefix%%controller.deleteRecord();
	 * @return false incase of related foundset having records and orphans records are not allowed by the relation
	 */
	public boolean js_deleteRecord() throws ServoyException;

	/**
	 * Deletes all records in foundset, resulting in empty foundset.
	 *
	 * @sample
	 * var success = %%prefix%%controller.deleteAllRecords();
	 * @return false incase of related foundset having records and orphans records are not allowed by the relation
	 */
	public boolean js_deleteAllRecords() throws ServoyException;

	/**
	 * Sets this form in designmode with one or more callback methods. 
	 *
	 * @sampleas jsFunction_setDesignMode(boolean)
	 *
	 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
	 */
	public void jsFunction_setDesignMode(Function onDrag);

	/**
	 * Sets this form in designmode with one or more callback methods. 
	 *
	 * @sampleas jsFunction_setDesignMode(boolean)
	 *
	 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
	 * @param ondrop org.mozilla.javascript.Function onDrop method reference 
	 */
	public void jsFunction_setDesignMode(Function onDrag, Function onDrop);

	/**
	 * Sets this form in designmode with one or more callback methods. 
	 *
	 * @sampleas jsFunction_setDesignMode(boolean)
	 *
	 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
	 * @param ondrop org.mozilla.javascript.Function onDrop method reference 
	 * @param onselect org.mozilla.javascript.Function onSelect method reference
	 */
	public void jsFunction_setDesignMode(Function onDrag, Function onDrop, Function onSelect);

	/**
	 * Sets this form in designmode with one or more callback methods. 
	 *
	 * @sampleas jsFunction_setDesignMode(boolean)
	 *
	 * @param ondrag org.mozilla.javascript.Function onDrag method reference 
	 * @param ondrop org.mozilla.javascript.Function onDrop method reference 
	 * @param onselect org.mozilla.javascript.Function onSelect method reference
	 * @param onresize org.mozilla.javascript.Function onResize method reference
	 */
	public void jsFunction_setDesignMode(Function onDrag, Function onDrop, Function onSelect, Function onResize);

	/**
	 * Sets this form in designmode with param true, false will return to normal browse/edit mode.
	 *
	 * @sample
	 * var form = forms["selectedFormName"];
	 * if (!form.controller.getDesignMode())
	 * {
	 * 	// Set the current form in designmode with no callbacks
	 * 	form.controller.setDesignMode(true);
	 * 	// Set the current form in designmode with callbacks
	 * 	// where onDrag, onDrop, onSelect, onResize are names of form methods (not from "selectedFormName" form)
	 * 	// form.controller.setDesignMode(onDrag, onDrop, onSelect, onResize);
	 * }
	 * //Set the current form out of designmode (to normal browse)
	 * //form.controller.setDesignMode(false);
	 *
	 * @param designMode boolean sets form in design mode if true, false ends design mode.  
	 */
	public void jsFunction_setDesignMode(boolean designMode);

	/**
	 * Returns the state of this form designmode.
	 *
	 * @sample
	 * var success = %%prefix%%controller.getDesignMode();
	 * 
	 * @return the design mode state (true/fase)
	 */
	public boolean jsFunction_getDesignMode();

	/**
	 * Create a new record in the form foundset.
	 *
	 * @sample
	 * // foreign key data is only filled in for equals (=) relation items 
	 * %%prefix%%controller.newRecord();//default adds on top
	 * //%%prefix%%controller.newRecord(false); //adds at bottom
	 * //%%prefix%%controller.newRecord(2); //adds as second record
	 * 
	 * @return true if succesful
	 */
	public boolean js_newRecord() throws ServoyException;

	/**
	 * @clonedesc js_newRecord()
	 * @sampleas js_newRecord()
	 * @param insertOnTop boolean true adds the new record as the topmost record
	 * @return true if successful
	 */
	public boolean js_newRecord(boolean insertOnTop) throws ServoyException;

	/**
	 * @clonedesc js_newRecord()
	 * @sampleas js_newRecord()
	 * @param location adds at specified index
	 * @return true if successful
	 */
	public boolean js_newRecord(int location) throws ServoyException;

	/**
	 * Duplicate current record or record at index in the form foundset.
	 *
	 * @sample
	 * %%prefix%%controller.duplicateRecord(); //duplicate the current record, adds on top
	 * //%%prefix%%controller.duplicateRecord(false); //duplicate the current record, adds at bottom
	 * //%%prefix%%controller.duplicateRecord(1,2); //duplicate the first record as second record
	 * 
	 * @return true if succesful
	 */
	public boolean js_duplicateRecord() throws ServoyException;

	/**
	 * @clonedesc js_duplicateRecord()
	 * @sampleas js_duplicateRecord()
	 * @param location boolean true adds the new record as the topmost record
	 * @return true if successful
	 */
	public boolean js_duplicateRecord(boolean location) throws ServoyException;

	/**
	 * @clonedesc js_duplicateRecord()
	 * @sampleas js_duplicateRecord()
	 * @param location adds at specified index
	 * @return true if successful
	 */
	public boolean js_duplicateRecord(int location) throws ServoyException;

	public void js_setView(int i);

	/**
	 * Get/Set the current type of view of this form.
	 *
	 * @sample
	 * //gets the type of view for this form
	 * var view = %%prefix%%controller.view;
	 * //sets the form to Record view
	 * %%prefix%%controller.view = 0;//RECORD_VIEW
	 * //sets the form to List view
	 * %%prefix%%controller.view = 1;//LIST_VIEW
	 */
	public int js_getView();

	/**
	 * Gets the forms context where it resides, returns a dataset of its structure to the main controller.
	 * Note: can't be called in onload, because no context is yet available at this time.
	 *
	 * @sample
	 * //dataset columns: [containername(1),formname(2),tabpanel or beanname(3),tabname(4),tabindex(5)]
	 * //dataset rows: mainform(1) -> parent(2)  -> current form(3) (when 3 forms deep)
	 * /** @type {JSDataSet} *&#47;
	 * var dataset = %%prefix%%controller.getFormContext();
	 * if (dataset.getMaxRowIndex() > 1) 
	 * {
	 * 	// form is in a tabpanel
	 * 	var parentFormName = dataset.getValue(1,2)
	 * }
	 * @return the dataset with form context
	 * @see com.servoy.j2db.dataprocessing.JSDataSet 
	 */
	public JSDataSet js_getFormContext();

	/**
	 * Omit current record in form foundset, to be shown with loadOmittedRecords.
	 * 
	 * Note: The omitted records are discarded when these functions are executed: loadAllRecords, loadRecords(dataset), loadRecords(sqlstring), invert 
	 *
	 * @sample var success = %%prefix%%controller.omitRecord();
	 * @return true if successful
	 * 
	 * @see com.servoy.j2db.FormController$JSForm#js_loadOmittedRecords()
	 */
	public boolean js_omitRecord() throws ServoyException;

	/**
	 * Loads the records that are currently omitted in the form foundset.
	 *
	 * @sample %%prefix%%controller.loadOmittedRecords();
	 * @return true if successful
	 */
	public boolean js_loadOmittedRecords() throws ServoyException;

	/**
	 * Inverts the current foundset against all rows of the current table; all records that are not in the foundset will become the current foundset.
	 *
	 * @sample %%prefix%%controller.invertRecords();
	 * @return true if successful
	 */
	public boolean js_invertRecords() throws ServoyException;

	/**
	 * Show the sort dialog to the user a preselection sortString can be passed, to sort the form foundset.
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
	 * 
	 * @sample %%prefix%%controller.sortDialog('columnA desc,columnB asc'); 
	 */
	public void js_sortDialog();

	/**
	 * @clonedes js_sortDialog()
	 * @sampleas js_sortDialog()
	 *
	 * @param sortString the specified columns (and sort order) 
	 */
	public void js_sortDialog(String sortString);

	/**
	 * Sorts the form foundset based on the given sort string. 
	 * TIP: You can use the Copy button in the developer Select Sorting Fields dialog to get the needed syntax string for the desired sort fields/order. 
	 *
	 * @sample %%prefix%%controller.sort('columnA desc,columnB asc');
	 *
	 * @param sortString the specified columns (and sort order)
	 */
	public void js_sort(String sortString);

	/**
	 * @clonedesc js_sort(String)
	 * @sampleas js_sort(String)
	 *
	 * @param sortString the specified columns (and sort order)
	 * @param defer the "sortString" will be just stored, without performing a query on the database (the actual sorting will be deferred until the next data loading action).
	 */
	public void js_sort(String sortString, boolean defer);

	/**
	 * Performs a relookup for the current foundset record dataproviders.
	 * Lookups are defined in the dataprovider (columns) auto-enter setting and are normally performed over a relation upon record creation.
	 *
	 * @sample %%prefix%%controller.relookup();
	 */
	public void js_relookup();

	/**
	 * Gets a value based on the specified dataprovider name. 
	 *
	 * @sample var val = %%prefix%%controller.getDataProviderValue('contact_name');
	 *
	 * @param dataProvider the dataprovider name to retieve the value for
	 * @return the dataprovider value (null if unknown dataprovider)
	 */
	public Object jsFunction_getDataProviderValue(String dataProvider);

	/**
	 * Sets the value based on a specified dataprovider name.
	 *
	 * @sample %%prefix%%controller.setDataProviderValue('contact_name','mycompany');
	 *
	 * @param dataprovider the dataprovider name to set the value for 
	 * @param value the value to set in the dataprovider 
	 */
	public void jsFunction_setDataProviderValue(String dataprovider, Object value);

	/**
	 * Set the preferred printer name to use when printing.
	 *
	 * @sample %%prefix%%controller.setPreferredPrinter('HP Laser 2200');
	 *
	 * @param printerName The name of the printer to be used when printing. 
	 */
	public void jsFunction_setPreferredPrinter(String printerName);

	/**
	 * Set the page format to use when printing.
	 *
	 * Orientation values:
	 * 0 - Landscape mode
	 * 1 - Portrait mode
	 * 
	 * Units values:
	 * 0 - millimeters
	 * 1 - inches
	 * 2 - pixels
	 * 
	 * Note: The unit specified for width, height and all margins MUST be the same.
	 *  
	 * @sample
	 * //Set page format to a custom size of 100x200 pixels with 10 pixel margins on all sides in portrait mode
	 * %%prefix%%controller.setPageFormat(100, 200, 10, 10, 10, 10);
	 * 
	 * //Set page format to a custom size of 100x200 pixels with 10 pixel margins on all sides in landscape mode
	 * %%prefix%%controller.setPageFormat(100, 200, 10, 10, 10, 10, SM_ORIENTATION.LANDSCAPE);
	 * 
	 * //Set page format to a custom size of 100x200 mm in landscape mode
	 * %%prefix%%controller.setPageFormat(100, 200, 0, 0, 0, 0, SM_ORIENTATION.LANDSCAPE, SM_UNITS.MM);
	 * 
	 * //Set page format to a custom size of 100x200 inch in portrait mode
	 * %%prefix%%controller.setPageFormat(100, 200, 0, 0, 0, 0, SM_ORIENTATION.PORTRAIT, SM_UNITS.INCH);
	 *
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed.
	 */
	public void jsFunction_setPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin);

	/**
	 * @clonedesc jsFunction_setPageFormat(double, double, double, double, double, double)
	 * @sampleas jsFunction_setPageFormat(double, double, double, double, double, double)
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed. 
	 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
	 */
	public void jsFunction_setPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin,
		int orientation);

	/**
	 * @clonedesc jsFunction_setPageFormat(double, double, double, double, double, double)
	 * @sampleas jsFunction_setPageFormat(double, double, double, double, double, double)
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed. 
	 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
	 * @param units the specified units for the width and height of the page to be printed; the default is pixels
	 */
	public void jsFunction_setPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin,
		int orientation, int units);

	/**
	 * Gets the form width in pixels. 
	 *
	 * @sample var width = %%prefix%%controller.getFormWidth();
	 *
	 * @return the width in pixels
	 */
	public int js_getFormWidth();

	/**
	 * Gets the part height in pixels. 
	 *
	 * @sample var height = %%prefix%%controller.getPartHeight(JSPart.BODY);
	 *
	 * @param partType The type of the part whose height will be returned.
	 *
	 * @return the part height in pixels
	 */
	public int js_getPartHeight(int partType);

	/**
	 * Returns the Y offset of a given part of the form. 
	 *
	 * @sample
	 * var offset = %%prefix%%controller.getPartYOffset(JSPart.BODY);
	 *
	 * @param partType The type of the part whose Y offset will be returned.
	 *
	 * @return A number holding the Y offset of the specified form part.
	 */
	public int js_getPartYOffset(int partType);

	/** Get a design-time property of a form.
	 *
	 * @sample 
	 * var prop = forms.orders.controller.getDesignTimeProperty('myprop')	
	 */
	public Object js_getDesignTimeProperty(String key);

}