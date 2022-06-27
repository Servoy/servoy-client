/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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
import java.util.List;

import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.ui.IFormUI;

/**
 * @author jcompagner
 *
 */
public interface IBasicFormUI extends IFormUI
{

	/**
	 * @param application
	 * @param dlgMessage
	 * @param string
	 * @return
	 */
	boolean showYesNoQuestionDialog(IApplication application, String dlgMessage, String string);

	/**
	 * @return
	 */
	String getContainerName();

	/**
	 * @param showDialogs
	 * @param printCurrentRecordOnly
	 * @param zoomFactor
	 * @param printerJob
	 */
	void printPreview(boolean showDialogs, boolean printCurrentRecordOnly, int zoomFactor, PrinterJob printerJob);

	/**
	 * @param showDialogs
	 * @param printCurrentRecordOnly
	 * @param showPrinterSelectDialog
	 * @param printerJob
	 */
	void print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob);

	/**
	 * @param printCurrentRecordOnly
	 * @return
	 */
	String printXML(boolean printCurrentRecordOnly);

	/**
	 * @param application
	 * @param options
	 */
	void showSortDialog(IApplication application, String options);

	/**
	 * @return
	 */
	int getFormWidth();

	/**
	 * @param partType
	 * @return
	 */
	int getPartHeight(int partType);

	/**
	 * @return
	 */
	JSDataSet getFormContext();

	/**
	 * @param invokeLaterRunnables
	 */
	void changeFocusIfInvalid(List<Runnable> invokeLaterRunnables);

	/**
	 * @param looseFocus
	 */
	void prepareForSave(boolean looseFocus);

	/**
	 *  Called by the Form Controller when the form wants to hide itself.
	 *  The ui should go over all the components and ask if they can be hidden
	 *  FormContainer componet should relay this to the visible nested form.
	 */
	default boolean executeOnBeforeHide()
	{
		return true;
	}

}
