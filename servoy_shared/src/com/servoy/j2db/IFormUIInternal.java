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
import java.util.Map;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.util.IFocusCycleRoot;

public interface IFormUIInternal<T> extends IFormUI, IFocusCycleRoot<T>
{
	public IView initView(IApplication app, FormController fp, int viewType);

	public ControllerUndoManager getUndoManager();

	public ElementScope makeElementsScriptObject(Scriptable fs, Map<String, Object[]> hmChildrenJavaMembers, IDataRenderer[] dataRenderers, IView view);

	public void destroy();

	public void setReadOnly(boolean b);

	/* overrides IFormUI.getController() */
	public FormController getController();

	/**
	 * @param options
	 */
	public void showSortDialog(IApplication application, String options);

	/**
	 * @param showDialogs
	 * @param printCurrentRecordOnly
	 * @param showPrinterSelectDialog
	 * @param printerJob
	 */
	public void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob);

	/**
	 * @param showDialogs
	 * @param printCurrentRecordOnly
	 * @param printerJob
	 */
	public void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomfactor, PrinterJob printerJob);

	/**
	 * @param printCurrentRecordOnly
	 * @return
	 */
	public String printXML(boolean printCurrentRecordOnly);

	public void updateFormUI();

	public boolean isFormInDialog();

	/**
	 * Returns the name of the window/dialog in which this form is showing.
	 * @return the name of the window/dialog in which this form is showing. If there is no such dialog/window or the form is showing in main app. frame, returns null.
	 */
	public String getContainerName();

	/**
	 * Shows an Yes/No question dialog.
	 * 
	 * @param application
	 * @param dlgMessage
	 * @param string
	 * @return true if user chose yes, false if user chose No or closed the dialog.
	 */
	public boolean showYesNoQuestionDialog(IApplication application, String dlgMessage, String string);

	public void focusField(T field);

	public JSDataSet getFormContext();

	/**
	 * @param mode
	 * @param callback
	 */
	public void setDesignMode(DesignModeCallbacks callback);

	public boolean isDesignMode();

	public void uiRecreated();

	public int getFormWidth();

	public int getPartHeight(int partType);

	public void prepareForSave(boolean looseFocus);

}
