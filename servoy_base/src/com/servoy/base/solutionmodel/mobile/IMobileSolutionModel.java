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

package com.servoy.base.solutionmodel.mobile;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.scripting.solutionhelper.IBaseSHList;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSolutionModel;


/**
 * Solution model interface for mobile clients.
 * 
 * @author acostescu
 *
 * @since 7.2
 */
@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
public interface IMobileSolutionModel extends IBaseSolutionModel
{

	/** 
	 * Creates a new JSForm Object.
	 * 
	 * NOTE: See the JSForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/my_server/my_table')
	 * //now you can add stuff to the form (under JSForm node)
	 * //add a label
	 * myForm.newLabel('Name', 1)
	 * //add a "normal" text entry field
	 * myForm.newTextField('dataProviderNameHere', 2)
	 *
	 * @param name the specified name of the form
	 *
	 * @param dataSource the specified name of the datasource for the specified table
	 *
	 * @return a new JSForm object
	 */
	public IBaseSMForm newForm(String name, String dataSource);

	/**
	 * Creates a new list form, similar to an inset list but without the inset list's header and relation.
	 * The result will be an independent form which behaves like a mobile list.
	 * 
	 * @param formName the new form's name.
	 * @param dataSource the list will be populated based on this datasource.
	 * @param textDataProviderID can be null; it's a convenience argument for setting the dataprovider that will be used to populate the main text area of the list's items.
	 * @return the newly created list form.
	 * 
	 * @sample
	 * var f = solutionModel.newForm("created_by_sm_1","udm","contacts",null,false,100,380);
	 * // create a button to go to it on the main form
	 * b = f.newButton("Show created list form",0,9,10,10,
	 * 	f.newMethod("function showListForm() { forms.created_by_sm_2.controller.show(); }"));
	 * // create the actual list form
	 * var list = f.createListForm('created_by_sm_2', databaseManager.getDataSource("udm","contacts"),"name_first");
	 * list.onAction = solutionModel.getForm('created_by_sm_2').newMethod("function goBack() { history.back(); }");
	 */
	public IBaseSHList newListForm(String formName, String dataSource, String textDataProviderID);

	/**
	 * Returns an existing list form.
	 * 
	 * @param formName the form's name.
	 * @return the existing list form, or null if it does not exist.
	 * 
	 * @sample
	 * var list = solutionModel.getListForm('created_by_sm_2');
	 */
	public IBaseSHList getListForm(String name);

	/**
	 * Get an array of all list-forms.
	 *
	 * @sample
	 * var forms = solutionModel.getListForms()
	 * for (var i in forms)
	 * 	application.output(forms[i].name)
	 *
	 * @return an array of IBaseSHList type elements
	 */
	public IBaseSHList[] getListForms();

}
