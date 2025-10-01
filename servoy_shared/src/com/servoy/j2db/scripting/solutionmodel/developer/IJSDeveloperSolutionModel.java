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

package com.servoy.j2db.scripting.solutionmodel.developer;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.documentation.ServoyDocumented;


/**
 * ONLY AVAILABLE when running a client from Servoy Developer. Do not try to use this when running clients from a normal server/war deployment.<br/>
 * It is meant to be used primarily from developer's 'Interactive Console' view (so you will not get it suggested in code completion of a scope/form script editor).<br/><br/>
 *
 * It provides utility methods for interacting with the developer's environment from a debug Servoy client.<br/>
 * It offers a bridge between the runtime client and the developer's workspace - allowing for changes done at runtime via solution model to be persisted into the workspace.<br/><br/>
 *
 * Look at this as a way to automate various tasks that create solution content or alter big solutions.
 *
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
	@JSFunction
	void save();

	/**
	 * Saves all changes made through the solution model into the workspace.
	 *
	 * @param override Override existing in memory tables.
	 */
	@JSFunction
	void save(boolean override);

	/**
	 * Saves just the given form, valuelist, relation or in memory datasource into the developers workspace.
	 * This must be a solution created or altered form/in memory datasource.
	 *
	 * @param obj The formname, JSForm, JSValueList, JSRelation, datasource name or JSDataSource object to save.
	 */
	@JSFunction
	void save(Object obj);

	/**
	 * Saves just the given form, valuelist, relation or in memory datasource into the developers workspace.
	 * This must be a solution created or altered form/in memory datasource.
	 *
	 * @param obj The formname, JSForm, JSValueList, JSRelation, datasource name or JSDataSource object to save.
	 * @param override Override an existing in memory table.
	 */
	@JSFunction
	void save(Object obj, boolean override);

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
	@JSFunction
	void save(Object obj, String solutionName);

	/**
	 * Updates the given in memory datasource and saves it into the developers workspace.
	 *
	 * @param dataSource datasource name or JSDataSource object to save.
	 * @param dataset the dataset with the update columns
	 * @param types array of the update columns types
	 */
	@JSFunction
	void updateInMemDataSource(Object dataSource, JSDataSet dataSet, Object types);

//	JSONArray js_getExistingVariants(String variantCategoryName);
//
//	void js_setVariantsFor(String variantCategoryName, String jsonArrayString);
}