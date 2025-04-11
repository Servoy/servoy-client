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

package com.servoy.base.scripting.api;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * @author jcompagner
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IJSController
{
	/**
	 * Gets or sets the enabled state of a form; also known as "grayed-out".
	 *
	 * Notes:
	 * -A disabled element(s) cannot be selected by clicking the form.
	 *
	 * @sample
	 * //gets the enabled state of the form
	 * var state = %%prefix%%controller.enabled;
	 * //enables the form for input
	 * %%prefix%%controller.enabled = true;
	 */
	public boolean getEnabled();

	public void setEnabled(boolean b);

	/**
	 * Gets the current record index of the current foundset.
	 *
	 * @sample
	 * //gets the current record index in the current foundset
	 * var current = %%prefix%%controller.getSelectedIndex();
	 * //sets the next record in the foundset, will be reflected in UI
	 * %%prefix%%controller.setSelectedIndex(current+1);
	 * @return the index
	 */
	public int getSelectedIndex();

	/**
	 * Sets the current record index of the current foundset.
	 *
	 * @sample
	 * //gets the current record index in the current foundset
	 * var current = %%prefix%%controller.getSelectedIndex();
	 * //sets the next record in the foundset, will be reflected in UI
	 * %%prefix%%controller.setSelectedIndex(current+1);
	 *
	 * @param index the index to select
	 */
	public void setSelectedIndex(int index);

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
	public void showRecords(IJSFoundSet foundset) throws Exception;

	/**
	 * Get the used datasource.
	 *
	 * @sample var dataSource = %%prefix%%controller.getDataSource();
	 * @return the datasource
	 */
	public String getDataSource();


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
	public void show() throws Exception;

	/**
	 * Get the name of this form.
	 *
	 * @sample var formName = %%prefix%%controller.getName();
	 * @return the name
	 */
	public String getName();
}
