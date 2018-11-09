/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.documentation.ServoyDocumented;


/**
 * Special interface in javascript only there in the developer that bridges between the runtime client and the developers workspace
 * @author emera
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "servoyDeveloper", scriptingName = "servoyDeveloper")
@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
public interface IJSDeveloperSolutionModel
{

	/**
	 * Saves all changes made through the solution model into the workspace.
	 * Please note that this method only saves the new in memory datasources,
	 * if you would like to override the existing ones use servoyDeveloper.save(true).
	 */
	void js_save();

	/**
	 * Saves all changes made through the solution model into the workspace.
	 *
	 * @param override Override existing in memory tables.
	 */
	void js_save(boolean override);

	/**
	 * Saves just the given form, valuelist, relation or in memory datasource into the developers workspace.
	 * This must be a solution created or altered form/in memory datasource.
	 *
	 * @param obj The formname, JSForm, JSValueList, JSRelation, datasource name or JSDataSource object to save.
	 */
	void js_save(Object obj);

	/**
	 * Saves just the given form, valuelist, relation or in memory datasource into the developers workspace.
	 * This must be a solution created or altered form/in memory datasource.
	 *
	 * @param obj The formname, JSForm, JSValueList, JSRelation, datasource name or JSDataSource object to save.
	 * @param override Override an existing in memory table.
	 */
	void js_save(Object obj, boolean override);

	/**
	 * Saves just the given form, valuelist, relation or in memory datasource into the developers workspace.
	 * This must be a solution created form/in memory datasource.
	 * NOTE: The current method can only be used for new objects.
	 * For existing objects, please use the save method with the override flag set to true.
	 * It is not needed to specify the solution, because the object to be updated will be saved in the right solution.
	 *
	 * @param obj The formname, JSForm, JSValueList, JSRelation, datasource name or JSDataSource object to save.
	 * @param solutionName The destination solution, a module of the active solution.
	 */
	void js_save(Object obj, String solutionName);

	/**
	 * Updates the given in memory datasource and saves it into the developers workspace.
	 *
	 * @param dataSource datasource name or JSDataSource object to save.
	 * @param dataset the dataset with the update columns
	 * @param types array of the update columns types
	 */
	void js_updateInMemDataSource(Object dataSource, JSDataSet dataSet, Object types);

	/**
	 * Opens the form FormEditor in the developer.
	 *
	 * @param form The form name or JSForm object to open in an editor.
	 */
	void js_openForm(Object form);

}