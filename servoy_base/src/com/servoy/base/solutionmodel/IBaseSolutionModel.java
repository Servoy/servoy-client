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

package com.servoy.base.solutionmodel;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Solution model base interface (for mobile as well as other clients).
 * For more information have a look at ISolutionModel.
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(ng = true, mc = true, wc = true, sc = true)
public interface IBaseSolutionModel
{

	/**
	 * Creates a new IBaseSMForm Object.
	 * 
	 * NOTE: See the IBaseSMForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'my_server', 'my_table', 'myStyleName', false, 800, 600)
	 * //With only a datasource:
	 * //var myForm = solutionModel.newForm('newForm', datasource, 'myStyleName', false, 800, 600)
	 * //now you can add stuff to the form (under IBaseSMForm node)
	 * //add a label
	 * myForm.newLabel('Name', 20, 20, 120, 30)
	 * //add a "normal" text entry field
	 * myForm.newTextField('dataProviderNameHere', 140, 20, 140,20)
	 *
	 * @param name the specified name of the form
	 *
	 * @param serverName the specified name of the server for the specified table
	 *
	 * @param tableName the specified name of the table
	 *
	 * @param styleName the specified style  
	 *
	 * @param show_in_menu if true show the name of the new form in the menu; or false for not showing
	 *
	 * @param width the width of the form in pixels
	 *
	 * @param height the height of the form in pixels
	 * 
	 * @return a new IBaseSMForm object
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = true, sc = true)
	public IBaseSMForm newForm(String name, String serverName, String tableName, String styleName, boolean show_in_menu, int width, int height);

	/**
	 * Creates a new IBaseSMForm Object.
	 * 
	 * NOTE: See the IBaseSMForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/my_server/my_table', 'myStyleName', false, 800, 600)
	 * //now you can add stuff to the form (under IBaseSMForm node)
	 * //add a label
	 * myForm.newLabel('Name', 20, 20, 120, 30)
	 * //add a "normal" text entry field
	 * myForm.newTextField('dataProviderNameHere', 140, 20, 140,20)
	 *
	 * @param name the specified name of the form
	 *
	 * @param dataSource the specified name of the datasource for the specified table
	 *
	 * @param styleName the specified style  
	 *
	 * @param show_in_menu if true show the name of the new form in the menu; or false for not showing
	 *
	 * @param width the width of the form in pixels
	 *
	 * @param height the height of the form in pixels
	 * 
	 * @return a new ISMForm object
	 */
	@ServoyClientSupport(ng = true, mc = false, wc = true, sc = true)
	public IBaseSMForm newForm(String name, String dataSource, String styleName, boolean show_in_menu, int width, int height);

	/**
	 * Reverts the specified form to the original (blueprint) version of the form; will result in an exception error if the form is not an original form.
	 * 
	 * NOTE: Make sure you call history.remove first in your Servoy method (script) or call form.controller.recreateUI() before the script ends.
	 *
	 * @sample
	 * // revert the form to the original solution form, removing any changes done to it through the solution model.
	 * var revertedForm = solutionModel.revertForm('myForm')
	 * // add a label on a random place.
	 * revertedForm.newLabel("MyLabel",Math.random()*100,Math.random()*100,80,20);
	 * // make sure that the ui is up to date.
	 * forms.myForm.controller.recreateUI();
	 *
	 * @param name the specified name of the form to revert
	 * 
	 * @return an IBaseSMForm object
	 */
	public IBaseSMForm revertForm(String name);

	/**
	 * Removes the specified form during the persistent connected client session.
	 * 
	 * NOTE: Make sure you call history.remove first in your Servoy method (script).
	 *
	 * @sample
	 * //first remove it from the current history, to destroy any active form instance
	 * var success = history.removeForm('myForm')
	 * //removes the named form from this session, please make sure you called history.remove() first
	 * if(success)
	 * {
	 * 	solutionModel.removeForm('myForm')
	 * }
	 *
	 * @param name the specified name of the form to remove
	 * 
	 * @return true is form has been removed, false if form could not be removed
	 */
	public boolean removeForm(String name);


	/**
	 * Removes the specified global method.
	 * 
	 * @sample
	 * var m1 = solutionModel.newGlobalMethod('globals', 'function myglobalmethod1(){application.output("Global Method 1");}');
	 * var m2 = solutionModel.newGlobalMethod('globals', 'function myglobalmethod2(){application.output("Global Method 2");}');
	 * 
	 * var success = solutionModel.removeGlobalMethod('globals', 'myglobalmethod1');
	 * if (success == false) application.output('!!! myglobalmethod1 could not be removed !!!');
	 * 
	 * var list = solutionModel.getGlobalMethods('globals');
	 * for (var i = 0; i < list.length; i++) { 
	 * 	application.output(list[i].code);
	 * }
	 * 
	 * @param scopeName the scope in which the method is declared
	 * @param name the name of the global method to be removed
	 * @return true if the removal was successful, false otherwise
	 */
	public boolean removeGlobalMethod(String scopeName, String name);

	/**
	 * Removes the specified global variable.
	 * 
	 * @sample
	 * var v1 = solutionModel.newGlobalVariable('globals', 'globalVar1', IBaseSMVariable.INTEGER);
	 * var v2 = solutionModel.newGlobalVariable('globals', 'globalVar2', IBaseSMVariable.TEXT);
	 * 
	 * var success = solutionModel.removeGlobalVariable('globals', 'globalVar1');
	 * if (success == false) application.output('!!! globalVar1 could not be removed !!!');
	 * 
	 * var list = solutionModel.getGlobalVariables('globals');
	 * for (var i = 0; i < list.length; i++) {
	 * 	application.output(list[i].name + '[ ' + list[i].variableType + ']: ' + list[i].variableType);
	 * }
	 * 
	 * @param scopeName the scope in which the variable is declared
	 * @param name the name of the global variable to be removed 
	 * @return true if the removal was successful, false otherwise
	 */
	public boolean removeGlobalVariable(String scopeName, String name);

	/**
	 * Gets the specified form object and returns information about the form (see IBaseSMForm node).
	 *
	 * @sample
	 * var myForm = solutionModel.getForm('existingFormName');
	 * //get the style of the form (for all other properties see IBaseSMForm node)
	 * var scrollBars = myForm.scrollbars;
	 *
	 * @param name the specified name of the form
	 * 
	 * @return a IBaseSMForm
	 */
	public IBaseSMForm getForm(String name);

	/**
	 * Get an array of forms, that are all based on datasource/servername.
	 *
	 * @sample
	 * var forms = solutionModel.getForms(datasource)
	 * for (var i in forms)
	 * 	application.output(forms[i].name)
	 *
	 * @param datasource the datasource or servername 
	 * 
	 * @return an array of IBaseSMForm type elements
	 */
	public IBaseSMForm[] getForms(String datasource);

	/**
	 * Get an array of forms, that are all based on datasource/servername and tablename.
	 *
	 * @sample
	 * var forms = solutionModel.getForms(datasource,tablename)
	 * for (var i in forms)
	 * 	application.output(forms[i].name)
	 *
	 * @param server the datasource or servername 
	 * 
	 * @param tablename the tablename
	 * 
	 * @return an array of IBaseSMForm type elements
	 */
	public IBaseSMForm[] getForms(String server, String tablename);

	/**
	 * Get an array of all forms.
	 *
	 * @sample
	 * var forms = solutionModel.getForms()
	 * for (var i in forms)
	 * 	application.output(forms[i].name)
	 *
	 * @return an array of IBaseSMForm type elements
	 */
	public IBaseSMForm[] getForms();

	/**
	 * Gets an existing valuelist by the specified name and returns a IBaseSMValueList Object that can be assigned to a field.
	 *
	 * @sample
	 * var myValueList = solutionModel.getValueList('myValueListHere')
	 * //now set the valueList property of your field
	 * //myField.valuelist = myValueList
	 *
	 * @param name the specified name of the valuelist
	 * 
	 * @return a IBaseSMValueList object
	 */
	public IBaseSMValueList getValueList(String name);

	/**
	 * Gets an array of all valuelists for the currently active solution.
	 *
	 * @sample 
	 * var valueLists = solutionModel.getValueLists();
	 * if (valueLists != null && valueLists.length != 0)
	 * 	for (var i in valueLists)
	 * 		application.output(valueLists[i].name); 
	 * 
	 * @return an array of IBaseSMValueList objects
	 */
	public IBaseSMValueList[] getValueLists();

	/**
	 * Creates a new global variable with the specified name and number type.
	 * 
	 * NOTE: The global variable number type is based on the value assigned from the SolutionModel-IBaseSMVariable node; for example: IBaseSMVariable.INTEGER.
	 *
	 * @sample 
	 * var myGlobalVariable = solutionModel.newGlobalVariable('globals', 'newGlobalVariable', IBaseSMVariable.INTEGER); 
	 * myGlobalVariable.defaultValue = 12;
	 *	//myGlobalVariable.defaultValue = "{a:'First letter',b:'Second letter'}"
	 *
	 * @param scopeName the scope in which the variable is created
	 * @param name the specified name for the global variable 
	 *
	 * @param type the specified number type for the global variable
	 * 
	 * @return a IBaseSMVariable object
	 */
	public IBaseSMVariable newGlobalVariable(String scopeName, String name, int type);

	/**
	 * Gets an existing global variable by the specified name.
	 *
	 * @sample 
	 * var globalVariable = solutionModel.getGlobalVariable('globals', 'globalVariableName');
	 * application.output(globalVariable.name + " has the default value of " + globalVariable.defaultValue);
	 * 
	 * @param scopeName the scope in which the variable is searched
	 * @param name the specified name of the global variable
	 * 
	 * @return a IBaseSMVariable 
	 */
	public IBaseSMVariable getGlobalVariable(String scopeName, String name);

	/**
	 * Gets an array of all scope names used.
	 * 
	 * @sample
	 * var scopeNames = solutionModel.getScopeNames();
	 * for (var name in scopeNames)
	 * 	application.output(name);
	 * 
	 * @return an array of String scope names
	 */
	public String[] getScopeNames();

	/**
	 * Gets an array of all global variables.
	 * 
	 * @sample
	 * var globalVariables = solutionModel.getGlobalVariables('globals');
	 * for (var i in globalVariables)
	 * 	application.output(globalVariables[i].name + " has the default value of " + globalVariables[i].defaultValue);
	 * 
	 * @return an array of IBaseSMVariable type elements
	 * 
	 */
	public IBaseSMVariable[] getGlobalVariables();

	/**
	 * @clonedesc getGlobalVariables()
	 * @see #getGlobalVariables()
	 * @sampleas getGlobalVariables()
	 * @param scopeName limit to global vars of specified scope name
	 * 
	 * @return an array of IBaseSMVariable type elements
	 */
	public IBaseSMVariable[] getGlobalVariables(String scopeName);


	/**
	 * Creates a new global method with the specified code in a scope.
	 *
	 * @sample 
	 * var method = solutionModel.newGlobalMethod('globals', 'function myglobalmethod(){foundset.newRecord()}')
	 *
	 * @param scopeName the scope in which the method is created
	 * @param code the specified code for the global method
	 * 
	 * @return a IBaseSMMethod object
	 */
	public IBaseSMMethod newGlobalMethod(String scopeName, String code);

	/**
	 * Gets an existing global method by the specified name.
	 *
	 * @sample 
	 * var method = solutionModel.getGlobalMethod('globals', 'nameOfGlobalMethod'); 
	 * if (method != null) application.output(method.code);
	 * 
	 * @param scopeName the scope in which the method is searched
	 * @param name the name of the specified global method
	 * 
	 * @return a IBaseSMMethod
	 */
	public IBaseSMMethod getGlobalMethod(String scopeName, String name);

	/**
	 * The list of all global methods.
	 * 
	 * @sample
	 * var methods = solutionModel.getGlobalMethods('globals'); 
	 * for (var x in methods) 
	 * 	application.output(methods[x].getName());
	 * 
	 * @return an array of IBaseSMMethod type elements
	 * 
	 */
	public IBaseSMMethod[] getGlobalMethods();

	/**
	 * @clonedesc getGlobalMethods()
	 * @sampleas getGlobalMethods()
	 * @see #getGlobalMethods()
	 * @param scopeName limit to global methods of specified scope name
	 * @return an array of IBaseSMMethod type elements
	 */
	public IBaseSMMethod[] getGlobalMethods(String scopeName);

	/**
	 * Creates a new valuelist with the specified name and number type.
	 *
	 * @sample
	 * var vl1 = solutionModel.newValueList("customText",JSValueList.CUSTOM_VALUES);
	 * vl1.customValues = "customvalue1\ncustomvalue2";
	 * var vl2 = solutionModel.newValueList("customid",JSValueList.CUSTOM_VALUES);
	 * vl2.customValues = "customvalue1|1\ncustomvalue2|2";
	 * var form = solutionModel.newForm("customValueListForm",controller.getDataSource(),null,true,300,300);
	 * var combo1 = form.newComboBox("scopes.globals.text",10,10,120,20);
	 * combo1.valuelist = vl1;
	 * var combo2 = form.newComboBox("scopes.globals.id",10,60,120,20);
	 * combo2.valuelist = vl2;
	 *
	 * @param name the specified name for the valuelist
	 *
	 * @param type the specified number type for the valuelist; may be JSValueList.CUSTOM_VALUES, JSValueList.DATABASE_VALUES, JSValueList.EMPTY_VALUE_ALWAYS, JSValueList.EMPTY_VALUE_NEVER
	 * 
	 * @return a JSValueList object
	 */
	public IBaseSMValueList newValueList(String name, int type);

	/**
	 * Removes the specified valuelist.
	 * 
	 * @sample
	 * var vlName = "customValueList";
	 * var vl = solutionModel.newValueList(vlName,JSValueList.CUSTOM_VALUES);
	 * vl.customValues = "customvalue1\ncustomvalue2";
	 * 
	 * var status = solutionModel.removeValueList(vlName);
	 * if (status) application.output("Removal has been done.");
	 * else application.output("ValueList not removed.");
	 * 
	 * var vls = solutionModel.getValueLists();
	 * if (vls != null) {
	 * 	for (var i = 0; i < vls.length; i++) {
	 * 		application.output(vls[i]);
	 * 	}
	 * 	application.output("");
	 * }
	 * 
	 * 
	 * @param name name of the valuelist to be removed
	 * 
	 * @return true if the removal was successful, false otherwise
	 */
	public boolean removeValueList(String name);

}