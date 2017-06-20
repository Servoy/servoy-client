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

import java.util.List;

import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.scripting.CreationalPrototype;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.util.ServoyException;

/**
 * @author lvostinar
 *
 */
public interface IFormController extends IForm
{
	IApplication getApplication();

	String getName();

	Form getForm();

	FoundSet getFormModel();

	void touch();

	ITable getTable();

	FormScope getFormScope();

	boolean recreateUI();

	/**
	 * @param creationalPrototype
	 */
	JSForm initForJSUsage(CreationalPrototype creationalPrototype);

	boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables);

	boolean stopUIEditing(boolean looseFocus);

	/**
	 *
	 */
	void destroy();

	boolean isDestroyed();

	/**
	 * @return
	 */
	JSForm initForJSUsage();

	/**
	 * @return
	 */
	boolean isFormVisible();

	/**
	 *
	 */
	void init();

	/**
	 * @return
	 */
	boolean isFormExecutingFunction();

	public void executeOnLoadMethod();

	public void notifyResized();

	/**
	 * @param cmd
	 * @param args
	 * @param saveData
	 * @param src
	 * @param focusEvent
	 * @param methodKey
	 */
	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey);

	/**
	 * @param cmd
	 * @param args
	 * @param saveData
	 * @param src
	 * @param focusEvent
	 * @param methodKey
	 * @param allowFoundsetMethods
	 * @param executeWhenFieldValidationFailed
	 * @param throwException
	 * @return
	 */
	public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey, boolean allowFoundsetMethods,
		boolean executeWhenFieldValidationFailed, boolean throwException) throws Exception;

	/**
	 * @return
	 */
	boolean wantEmptyFoundSet();

	/**
	 * @param b
	 */
	boolean loadAllRecordsImpl(boolean b) throws ServoyException;

	/**
	 *
	 */
	void refreshView();

	boolean setMode(int mode);

	boolean hasParentForm();
}
