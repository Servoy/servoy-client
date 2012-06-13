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

package com.servoy.j2db.solutionmodel;

import com.servoy.j2db.persistence.BaseComponent;


/**
 * Solution model java interface.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISolutionModel
{
	/**
	 * Creates a new ISMForm Object.
	 * 
	 * NOTE: See the ISMForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'my_server', 'my_table', 'myStyleName', false, 800, 600)
	 * //With only a datasource:
	 * //var myForm = solutionModel.newForm('newForm', datasource, 'myStyleName', false, 800, 600)
	 * //now you can add stuff to the form (under ISMForm node)
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
	 * @return a new ISMForm object
	 */
	public ISMForm newForm(String name, String serverName, String tableName, String styleName, boolean show_in_menu, int width, int height);

	/**
	 * Creates a new ISMForm Object.
	 * 
	 * NOTE: See the ISMForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/my_server/my_table', 'myStyleName', false, 800, 600)
	 * //now you can add stuff to the form (under ISMForm node)
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
	public ISMForm newForm(String name, String dataSource, String styleName, boolean show_in_menu, int width, int height);

	/**
	 * Creates a new form with the given ISMForm as its super form.
	 * 
	 * @sample
	 * //creates 2 forms with elements on them; shows the parent form, waits 2 seconds and shows the child form
	 * var mySuperForm = solutionModel.newForm('mySuperForm', 'db:/my_server/my_table', null, false, 800, 600);
	 * var label1 = mySuperForm.newLabel('LabelName', 20, 20, 120, 30);
	 * label1.text = 'DataProvider';
	 * label1.background = 'red';
	 * mySuperForm.newTextField('myDataProvider', 140, 20, 140,20);
	 * forms['mySuperForm'].controller.show();
	 * application.sleep(2000);
	 * var mySubForm = solutionModel.newForm('mySubForm', mySuperForm);
	 * var label2 = mySuperForm.newLabel('SubForm Label', 20, 120, 120, 30);
	 * label2.background = 'green';
	 * forms['mySuperForm'].controller.recreateUI();
	 * forms['mySubForm'].controller.show();
	 * 	
	 * @param name The name of the new form
	 * @param superForm the super form that will extended from, see ISMForm.setExtendsForm();
	 * @return a new ISMForm object
	 */
	public ISMForm newForm(String name, ISMForm superForm);

	/**
	 * Gets the style specified by the given name.
	 * 
	 * @sample
	 * var style = solutionModel.getStyle('my_existing_style')
	 * style.content = 'combobox { color: #0000ff;font: italic 10pt "Verdana";}'
	 * 
	 * @param name the specified name of the style
	 * 
	 * @return a ISMStyle
	 */
	public ISMStyle getStyle(String name);

	/**
	 * Creates a new style with the given css content string under the given name.
	 * 
	 * NOTE: Will throw an exception if a style with that name already exists.  
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm','db:/my_server/my_table',null,true,1000,800);
	 * if (form.transparent == false)
	 * {
	 * 	var style = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * 	style.text = style.text + 'field { background-color: blue; }';
	 * 	form.styleName = 'myStyle';
	 * }
	 * var field = form.newField('columnTextDataProvider',JSField.TEXT_FIELD,100,100,100,50);
	 * forms['myForm'].controller.show();
	 *
	 * @param name the name of the new style
	 * 
	 * @param content the css content of the new style
	 * 
	 * @return a ISMStyle object
	 */
	public ISMStyle newStyle(String name, String content);

	/**
	 * Makes an exact copy of the given form and gives it the new name.
	 *
	 * @sample 
	 * // get an existing form
	 * var form = solutionModel.getForm("existingForm")
	 * // make a clone/copy from it
	 * var clone = solutionModel.cloneForm("clonedForm", form)
	 * // add a new label to the clone
	 * clone.newLabel("added label",50,50,80,20);
	 * // show it
	 * forms["clonedForm"].controller.show();
	 *
	 * @param newName the new name for the form clone
	 *
	 * @param ISMForm the form to be cloned 
	 * 
	 * @return a ISMForm
	 */
	public ISMForm cloneForm(String newName, ISMForm ISMForm);

	/**
	 * Makes an exact copy of the given component (ISMComponent/JSField/JSLabel) and gives it a new name.
	 *
	 * @sample
	 * // get an existing field to clone.
	 * var field = solutionModel.getForm("formWithField").getField("fieldName");
	 * // make a clone/copy of the field
	 * var clone = solutionModel.cloneComponent("clonedField",field);
	 * 
	 * @param newName the new name of the cloned component
	 *
	 * @param component the component to clone
	 *
	 * @return the exact copy of the given component
	 */
	public <T extends BaseComponent> ISMComponent cloneComponent(String newName, ISMComponent component);


	/**
	 * Makes an exact copy of the given component (ISMComponent/JSField/JSLabel), gives it a new name and moves it to a new parent form, specified as a parameter.
	 *
	 * @sample
	 * // get an existing field to clone.
	 * var field = solutionModel.getForm("formWithField").getField("fieldName");
	 * // get the target form for the copied/cloned field
	 * var form = solutionModel.getForm("targetForm");
	 * // make a clone/copy of the field and re parent it to the target form.
	 * var clone = solutionModel.cloneComponent("clonedField",field,form);
	 * // show it
	 * forms["targetForm"].controller.show();
	 * 
	 * @param newName the new name of the cloned component
	 *
	 * @param component the component to clone
	 *
	 * @param newParentForm the new parent form 
	 * 
	 * @return the exact copy of the given component
	 */
	public <T extends BaseComponent> ISMComponent cloneComponent(String newName, ISMComponent component, ISMForm newParentForm);

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
	 * Removes the relation specified by name.
	 * 
	 * @sample
	 * var success = solutionModel.removeRelation('myRelation');
	 * if (success) { application.output("Relation has been removed");}
	 * else {application.output("Relation could not be removed");}
	 * 
	 * @param name the name of the relation to be removed
	 * 
	 * @return true if the removal was successful, false otherwise
	 */
	public boolean removeRelation(String name);

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
	 * var v1 = solutionModel.newGlobalVariable('globals', 'globalVar1', ISMVariable.INTEGER);
	 * var v2 = solutionModel.newGlobalVariable('globals', 'globalVar2', ISMVariable.TEXT);
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
	 * Removes the media item specified by name.
	 * 
	 * @sample
	 * var bytes1 = plugins.file.readFile('D:/Imgs/image1.png');
	 * var image1 = solutionModel.newMedia('image1.png', bytes1);
	 * var bytes2 = plugins.file.readFile('D:/Imgs/image2.jpg');
	 * var image2 = solutionModel.newMedia('image2.jpg',bytes2);
	 * var bytes3 = plugins.file.readFile('D:/Imgs/image3.jpg');
	 * var image3 = solutionModel.newMedia('image3.jpg',bytes3);
	 * 
	 * var f = solutionModel.newForm("newForm",currentcontroller.getDataSource(),null,false,500,350);
	 * var l = f.newLabel('', 20, 70, 300, 200);
	 * l.imageMedia = image1;
	 * l.borderType =  solutionModel.createLineBorder(4,'#ff0000');
	 * forms["newForm"].controller.show();
	 * 
	 * var status = solutionModel.removeMedia('image1.jpg');
	 * if (status) application.output("image1.png has been removed");
	 * else application.output("image1.png has not been removed");
	 * 
	 * var mediaList = solutionModel.getMediaList();
	 * for (var i = 0; i < mediaList.length; i++) {
	 * 	application.output(mediaList[i].getName() + ":" + mediaList[i].mimeType);
	 * }
	 * 
	 * @param name the name of the media item to be removed
	 * 
	 * @return true if the removal was successful, false otherwise
	 */
	public boolean removeMedia(String name);

	/**
	 * Removes the specified style.
	 * 
	 * @sample
	 * var s = solutionModel.newStyle("smStyle1",'form { background-color: yellow; }');
	 * var status = solutionModel.removeStyle("smStyle1");
	 * if (status == false) application.output("Could not remove style.");
	 * else application.output("Style removed.");
	 * 
	 * @param name the name of the style to be removed
	 * 
	 * @return true if the removal was successful, false otherwise
	 */
	public boolean removeStyle(String name);

	/**
	 * Removes the specified valuelist.
	 * 
	 * @sample
	 * var vlName = "customValueList";
	 * var vl = solutionModel.newValueList(vlName,ISMValueList.CUSTOM_VALUES);
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
	 * @return a ISMForm object
	 */
	public ISMForm revertForm(String name);

	/**
	 * Gets the specified form object and returns information about the form (see ISMForm node).
	 *
	 * @sample
	 * var myForm = solutionModel.getForm('existingFormName');
	 * //get the style of the form (for all other properties see ISMForm node)
	 * var styleName = myForm.styleName;
	 *
	 * @param name the specified name of the form
	 * 
	 * @return a ISMForm
	 */
	public ISMForm getForm(String name);

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
	 * @return an array of ISMForm type elements
	 */
	public ISMForm[] getForms(String datasource);

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
	 * @return an array of ISMForm type elements
	 */
	public ISMForm[] getForms(String server, String tablename);

	/**
	 * Get an array of all forms.
	 *
	 * @sample
	 * var forms = solutionModel.getForms()
	 * for (var i in forms)
	 * 	application.output(forms[i].name)
	 *
	 * @return an array of ISMForm type elements
	 */
	public ISMForm[] getForms();


	/**
	 * Gets the specified data source node and returns information about the form (see ISMDataSourceNode node).
	 * The ISMDataSourceNode holds all calculations and foundset methods.
	 *
	 * @sample
	 * var dsnode = solutionModel.getDataSourceNode('db:/example_data/customers');
	 * var c = dsnode.getCalculation("myCalculation");
	 * application.output("Name: " + c.getName() + ", Stored: " + c.isStored());
	 *
	 * @param dataSource table data source
	 * 
	 * @return a ISMDataSourceNode
	 */
	public ISMDataSourceNode getDataSourceNode(String dataSource);


	/**
	 * Gets the specified media object; can be assigned to a button/label.
	 *
	 * @sample
	 * var myMedia = solutionModel.getMedia('button01.gif')
	 * //now set the imageMedia property of your label or button
	 * //myButton.imageMedia = myMedia
	 * // OR
	 * //myLabel.imageMedia = myMedia
	 *
	 * @param name the specified name of the media object
	 * 
	 * @return a ISMMedia element
	 */
	public ISMMedia getMedia(String name);

	/**
	 * Creates a new media object that can be assigned to a label or a button.
	 *
	 * @sample
	 * var myMedia = solutionModel.newMedia('button01.gif',bytes)
	 * //now set the imageMedia property of your label or button
	 * //myButton.imageMedia = myMedia
	 * // OR
	 * //myLabel.imageMedia = myMedia
	 *
	 * @param name The name of the new media
	 * 
	 * @param bytes The content
	 * 
	 * @return a ISMMedia object
	 *  
	 */
	public ISMMedia newMedia(String name, byte[] bytes);

	/**
	 * Gets the list of all media objects.
	 * 
	 * @sample
	 * var mediaList = solutionModel.getMediaList();
	 * if (mediaList.length != 0 && mediaList != null) {
	 * 	for (var x in mediaList) {
	 * 		application.output(mediaList[x]);
	 * 	}
	 * }
	 * 
	 * 	@return a list with all the media objects.
	 * 	
	 */
	public ISMMedia[] getMediaList();

	/**
	 * Gets an existing valuelist by the specified name and returns a ISMValueList Object that can be assigned to a field.
	 *
	 * @sample
	 * var myValueList = solutionModel.getValueList('myValueListHere')
	 * //now set the valueList property of your field
	 * //myField.valuelist = myValueList
	 *
	 * @param name the specified name of the valuelist
	 * 
	 * @return a ISMValueList object
	 */
	public ISMValueList getValueList(String name);

	/**
	 * Gets an array of all valuelists for the currently active solution.
	 *
	 * @sample 
	 * var valueLists = solutionModel.getValueLists();
	 * if (valueLists != null && valueLists.length != 0)
	 * 	for (var i in valueLists)
	 * 		application.output(valueLists[i].name); 
	 * 
	 * @return an array of ISMValueList objects
	 */
	public ISMValueList[] getValueLists();


	/**
	 * Creates a new valuelist with the specified name and number type.
	 *
	 * @sample
	 * var vl1 = solutionModel.newValueList("customText",ISMValueList.CUSTOM_VALUES);
	 * vl1.customValues = "customvalue1\ncustomvalue2";
	 * var vl2 = solutionModel.newValueList("customid",ISMValueList.CUSTOM_VALUES);
	 * vl2.customValues = "customvalue1|1\ncustomvalue2|2";
	 * var form = solutionModel.newForm("customValueListForm",controller.getDataSource(),null,true,300,300);
	 * var combo1 = form.newComboBox("scopes.globals.text",10,10,120,20);
	 * combo1.valuelist = vl1;
	 * var combo2 = form.newComboBox("scopes.globals.id",10,60,120,20);
	 * combo2.valuelist = vl2;
	 *
	 * @param name the specified name for the valuelist
	 *
	 * @param type the specified number type for the valuelist; may be ISMValueList.CUSTOM_VALUES, ISMValueList.DATABASE_VALUES, ISMValueList.EMPTY_VALUE_ALWAYS, ISMValueList.EMPTY_VALUE_NEVER
	 * 
	 * @return a ISMValueList object
	 */
	public ISMValueList newValueList(String name, int type);

	/**
	 * Creates a new global variable with the specified name and number type.
	 * 
	 * NOTE: The global variable number type is based on the value assigned from the SolutionModel-ISMVariable node; for example: ISMVariable.INTEGER.
	 *
	 * @sample 
	 * var myGlobalVariable = solutionModel.newGlobalVariable('globals', 'newGlobalVariable', ISMVariable.INTEGER); 
	 * myGlobalVariable.defaultValue = 12;
	 *	//myGlobalVariable.defaultValue = "{a:'First letter',b:'Second letter'}"
	 *
	 * @param scopeName the scope in which the variable is created
	 * @param name the specified name for the global variable 
	 *
	 * @param type the specified number type for the global variable
	 * 
	 * @return a ISMVariable object
	 */
	public ISMVariable newGlobalVariable(String scopeName, String name, int type);

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
	 * @return a ISMVariable 
	 */
	public ISMVariable getGlobalVariable(String scopeName, String name);

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
	 * @return an array of ISMVariable type elements
	 * 
	 */
	public ISMVariable[] getGlobalVariables();

	/**
	 * @clonedesc getGlobalVariables()
	 * @sampleas getGlobalVariables()
	 * @param scopeName limit to global vars of specified scope name
	 * 
	 * @return an array of ISMVariable type elements
	 */
	public ISMVariable[] getGlobalVariables(String scopeName);


	/**
	 * Creates a new global method with the specified code in a scope.
	 *
	 * @sample 
	 * var method = solutionModel.newGlobalMethod('globals', 'function myglobalmethod(){currentcontroller.newRecord()}')
	 *
	 * @param scopeName the scope in which the method is created
	 * @param code the specified code for the global method
	 * 
	 * @return a ISMMethod object
	 */
	public ISMMethod newGlobalMethod(String scopeName, String code);

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
	 * @return a ISMMethod
	 */
	public ISMMethod getGlobalMethod(String scopeName, String name);

	/**
	 * Get a ISMMethod instance with arguments to be assigned to an event.
	 *
	 * @sample 
	 * var str = "John's Bookstore"
	 * var form = solutionModel.getForm('orders')
	 * var button = form.getButton('abutton')
	 * var method = form.getFormMethod('doit') // has 4 arguments: event (fixed), boolean, number and string
	 * // string arguments have to be quoted, they are interpreted before the method is called
	 * var quotedString = "'"+utils.stringReplace(str, "'", "\\'")+"'"
	 * // list all arguments the method has, use nulls for fixed arguments (like event)
	 * button.onAction = solutionModel.wrapMethodWithArguments(method, [null, true, 42, quotedString])
	 * 
	 * @param method ISMMethod to be assigned to an event
	 * 
	 * @param args positional arguments
	 * 
	 * @return a ISMMethod
	 */
	public ISMMethod wrapMethodWithArguments(ISMMethod method, Object... args);

	/**
	 * The list of all global methods.
	 * 
	 * @sample
	 * var methods = solutionModel.getGlobalMethods('globals'); 
	 * for (var x in methods) 
	 * 	application.output(methods[x].getName());
	 * 
	 * @return an array of ISMMethod type elements
	 * 
	 */
	public ISMMethod[] getGlobalMethods();

	/**
	 * @clonedesc getGlobalMethods()
	 * @sampleas getGlobalMethods()
	 * @param scopeName limit to global methods of specified scope name
	 * @return an array of ISMMethod type elements
	 */
	public ISMMethod[] getGlobalMethods(String scopeName);

	/**
	 * Creates a new ISMRelation Object with a specified name; includes the primary datasource, foreign datasource and the type of join for the new relation.
	 *
	 * @sample 
	 * var rel = solutionModel.newRelation('myRelation', myPrimaryDataSource, myForeignDataSource, ISMRelation.INNER_JOIN);
	 * application.output(rel.getRelationItems()); 
	 *
	 * @param name the specified name of the new relation
	 *
	 * @param primaryDataSource the specified name of the primary datasource
	 *
	 * @param foreignDataSource the specified name of the foreign datasource
	 *
	 * @param joinType the type of join for the new relation; ISMRelation.INNER_JOIN, ISMRelation.LEFT_OUTER_JOIN
	 * 
	 * @return a ISMRelation object
	 */
	public ISMRelation newRelation(String name, String primaryDataSource, String foreignDataSource, int joinType);

	/**
	 * Gets an existing relation by the specified name and returns a ISMRelation Object.
	 * 
	 * @sample 
	 * var relation = solutionModel.getRelation('name');
	 * application.output("The primary server name is " + relation.primaryServerName);
	 * application.output("The primary table name is " + relation.primaryTableName); 
	 * application.output("The foreign table name is " + relation.foreignTableName); 
	 * application.output("The relation items are " + relation.getRelationItems());
	 * 
	 * @param name the specified name of the relation
	 * 
	 * @return a ISMRelation
	 */
	public ISMRelation getRelation(String name);

	/**
	 * Gets an array of all relations; or an array of all global relations if the specified table is NULL.
	 *
	 * @sample 
	 * var relations = solutionModel.getRelations('server_name','table_name');
	 * if (relations.length != 0)
	 * 	for (var i in relations)
	 * 		application.output(relations[i].name);
	 *
	 * @param datasource the specified name of the datasource for the specified table
	 * 
	 * @return an array of all relations (all elements in the array are of type ISMRelation)
	 */

	public ISMRelation[] getRelations(String datasource);

	/**
	 * @clonedesc getRelations(String)
	 * @sampleas getRelations(String)
	 * @param servername the specified name of the server for the specified table
	 * @param tablename the specified name of the table
	 * 
	 * @return an array of all relations (all elements in the array are of type ISMRelation)
	 */
	public ISMRelation[] getRelations(String servername, String tablename);

	/**
	 * Create a page format string.
	 *
	 * Note: The unit specified for width, height and all margins MUST be the same.
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.defaultPageFormat = solutionModel.createPageFormat(612,792,72,72,72,72,SM_ORIENTATION.PORTRAIT,SM_UNITS.PIXELS);
	 * 
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed.
	 */

	public String createPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin);

	/**
	 * clonedesc createPageFormat(double, double, double, double, double, double)
	 * @sampleas createPageFormat(double, double, double, double, double, double)
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed.
	 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
	 */
	public String createPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin, int orientation);

	/**
	 * clonedesc createPageFormat(double, double, double, double, double, double)
	 * @sampleas createPageFormat(double, double, double, double, double, double)
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed.
	 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
	 * @param units the specified units for the width and height of the page to be printed; the default is pixels
	 */
	public String createPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin, int orientation,
		int units);

	/**
	 * Create a font string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * var component = form.getComponent("someComponent")
	 * component.fontType = solutionModel.createFont('Arial',SM_FONTSTYLE.BOLD,14);
	 * 
	 * @param name the name of the font
	 * @param style the style of the font (PLAIN, BOLD, ITALIC or BOLD+ITALIC)
	 * @param size the font size
	 * 
	 */
	public String createFont(String name, int style, int size);

	/**
	 * Create an empty border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createEmptyBorder(1,1,1,1);
	 *  
	 * @param top_width top width of empty border in pixels
	 * @param right_width right width of empty border in pixels
	 * @param bottom_width bottom width of empty border in pixels
	 * @param left_width left width of empty border in pixels
	 * 
	 */
	public String createEmptyBorder(int top_width, int right_width, int bottom_width, int left_width);

	/**
	 * Create an etched border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createEtchedBorder(SM_BEVELTYPE.RAISED,'#ff0000','#00ff00');
	 * 
	 * @param bevel_type bevel border type
	 * @param highlight_color bevel border highlight color
	 * @param shadow_color bevel border shadow color
	 * 
	 */
	public String createEtchedBorder(int bevel_type, String highlight_color, String shadow_color);

	/**
	 * Create a bevel border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createBevelBorder(SM_BEVELTYPE.RAISED,'#ff0000','#00ff00','#ff0000','#00ff00');
	 * 
	 * @param bevel_type bevel border type (SM_BEVELTYPE.RAISED or SM_BEVELTYPE.LOWERED)
	 * @param highlight_outer_color bevel border highlight outer color
	 * @param highlight_inner_color bevel border highlight inner color
	 * @param shadow_outer_color bevel border shadow outer color
	 * @param shadow_inner_color bevel border shadow outer color
	 */
	public String createBevelBorder(int bevel_type, String highlight_outer_color, String highlight_inner_color, String shadow_outer_color,
		String shadow_inner_color);

	/**
	 * Create a line border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createLineBorder(1,'#ff0000');
	 *  
	 * @param thick border thickness in pixels
	 * @param color color of the line border
	 * 
	 */
	public String createLineBorder(int thick, String color);

	/**
	 * Create a titled border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createTitledBorder('Test',solutionModel.createFont('Arial',SM_FONTSTYLE.PLAIN,10),'#ff0000',SM_TITLEJUSTIFICATION.CENTER,SM_TITLEPOSITION.TOP);
	 * 
	 * @param title_text the text from border
	 * @param font title text font string
	 * @param color border color
	 * @param title_justification title text justification
	 * @param title_position bevel title text position
	 * 
	 */
	public String createTitledBorder(String title_text, String font, String color, int title_justification, int title_position);

	/**
	 * Create a matte border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * form.borderType = solutionModel.createMatteBorder(1,1,1,1,"#00ff00");
	 *  
	 * @param top_width top width of matte border in pixels
	 * @param right_width right width of matte border in pixels
	 * @param bottom_width bottom width of matte border in pixels
	 * @param left_width left width of matte border in pixels
	 * @param color border color
	 * 
	 */
	public String createMatteBorder(int top_width, int right_width, int bottom_width, int left_width, String color);

	/**
	 * Create a special matte border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * // create a rectangle border (no rounded corners) and continous line
	 * form.borderType = solutionModel.createSpecialMatteBorder(1,1,1,1,"#00ff00","#00ff00","#00ff00","#00ff00",0,null);
	 * // create a border with rounded corners and dashed line (25 pixels drawn, then 25 pixels skipped)
	 * // form.borderType = solutionModel.createSpecialMatteBorder(1,1,1,1,"#00ff00","#00ff00","#00ff00","#00ff00",10,new Array(25,25));
	 * 
	 * @param top_width top width of matte border in pixels
	 * @param right_width right width of matte border in pixels
	 * @param bottom_width bottom width of matte border in pixels
	 * @param left_width left width of matte border in pixels
	 * @param top_color top border color
	 * @param right_color right border color
	 * @param bottom_color bottom border color
	 * @param left_color left border color
	 * @param rounding_radius width of the arc to round the corners
	 * @param dash_pattern the dash pattern of border stroke
	 */
	public String createSpecialMatteBorder(int top_width, int right_width, int bottom_width, int left_width, String top_color, String right_color,
		String bottom_color, String left_color, float rounding_radius, float[] dash_pattern);

	/**
	 * Create a special matte border string.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * // create a rectangle border (no rounded corners) and continous line
	 * form.borderType = solutionModel.createSpecialMatteBorder(1,1,1,1,"#00ff00","#00ff00","#00ff00","#00ff00",0,null);
	 * // create a border with rounded corners and dashed line (25 pixels drawn, then 25 pixels skipped)
	 * // rounding_radius is an array of up to 8 numbers, order is: top-left,top-right,bottom-right,bottom-left (repetead twice - for width and height)
	 * // form.borderType = solutionModel.createSpecialMatteBorder(1,1,1,1,"#00ff00","#00ff00","#00ff00","#00ff00",new Array(10,10,10,10),new Array(25,25));
	 * 
	 * @param top_width top width of matte border in pixels
	 * @param right_width right width of matte border in pixels
	 * @param bottom_width bottom width of matte border in pixels
	 * @param left_width left width of matte border in pixels
	 * @param top_color top border color
	 * @param right_color right border color
	 * @param bottom_color bottom border color
	 * @param left_color left border color
	 * @param rounding_radius array with width/height of the arc to round the corners
	 * @param border_style the border styles for the four margins(top/left/bottom/left)
	 */
	public String createRoundedBorder(int top_width, int right_width, int bottom_width, int left_width, String top_color, String right_color,
		String bottom_color, String left_color, float[] rounding_radius, String[] border_style);
}
