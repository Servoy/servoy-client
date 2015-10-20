/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS

 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.base.solutionmodel;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Solution model form object, common to mobile and other clients.
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseSMForm extends IBaseSMListContainer
{

	/**
	 * The constants to set or get the view property of a JSForm. 
	 * They are as follows: JSForm.LIST_VIEW, JSForm.LOCKED_LIST_VIEW, JSForm.LOCKED_RECORD_VIEW, JSForm.LOCKED_TABLE_VIEW, JSForm.RECORD_VIEW.
	 *
	 * @sample 
	 * var myListViewForm = solutionModel.newForm('newForm1', myDatasource, myStyleName, false, 800, 600);
	 * myListViewForm.view = JSForm.LIST_VIEW;
	 * 
	 * var myLockedListViewForm = solutionModel.newForm('newForm2', myDatasource, myStyleName, false, 800, 600);	
	 * myLockedListViewForm.view = JSForm.LOCKED_LIST_VIEW;
	 * 
	 * var myLockedRecordViewForm = solutionModel.newForm('newForm3', myDatasource, myStyleName, false, 800, 600);
	 * myLockedRecordViewForm.view = JSForm.LOCKED_RECORD_VIEW;
	 * 
	 * var myLockedTableViewForm = solutionModel.newForm('newForm4', myDatasource, myStyleName, false, 800, 600);
	 * myLockedTableViewForm.view = JSForm.LOCKED_TABLE_VIEW;
	 * 
	 * var myRecordViewForm = solutionModel.newForm('newForm5', myDatasource, myStyleName, false, 800, 600);
	 * myRecordViewForm.view = JSForm.RECORD_VIEW;
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int LIST_VIEW = IFormConstants.VIEW_TYPE_LIST;

	/**
	 * @sameas LIST_VIEW
	 * @see #LIST_VIEW
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int LOCKED_TABLE_VIEW = IFormConstants.VIEW_TYPE_TABLE_LOCKED;

	/**
	 * @sameas LIST_VIEW
	 * @see #LIST_VIEW
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public static final int RECORD_VIEW = IFormConstants.VIEW_TYPE_RECORD;

	/**
	 * Creates a new form JSVariable - based on the name of the variable object and the number type, uses the SolutionModel JSVariable constants.
	 *
	 * @sampleas newVariable(String,int,String)
	 * @see #newVariable(String,int,String)
	 *
	 * @param name the specified name of the variable
	 *
	 * @param type the specified type of the variable (see Solution Model -> JSVariable node constants)
	 * 
	 * @return a JSVariable object
	 */
	public IBaseSMVariable newVariable(String name, int type);

	/**
	 * Creates a new form JSVariable - based on the name of the variable object , the  type  and it's default value , uses the SolutionModel JSVariable constants.
	 *
	 *<p>
	 * <b>This method does not require the form to be destroyed and recreated. Use this method if you want to change the form's model without destroying the runtime form</b>
	 *</p>
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var variable = form.newVariable('myVar', JSVariable.TEXT , "'This is a default value (with triple quotes)!'");
	 * //or variable = form.newVariable('myVar', JSVariable.TEXT)
	 * //variable.defaultValue = "'This is a default value (with triple quotes)!'" // setting the default value after the variable is created requires form recreation
	 * //variable.defaultValue = "{a:'First letter',b:'Second letter'}"   
	 * var field = form.newField(variable, JSField.TEXT_FIELD, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param name the specified name of the variable
	 *
	 * @param type the specified type of the variable (see Solution Model -> JSVariable node constants)
	 * 
	 * @param defaultValue the default value as a javascript expression string
	 * 
	 * @return a JSVariable object
	 */
	public IBaseSMVariable newVariable(String name, int type, String defaultValue);

	/**
	 * Gets an existing form variable for the given name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var fvariable = frm.getVariable("myVarName");
	 * 	application.output(fvariable.name + " has the default value of " + fvariable.defaultValue);
	 * 
	 * @param name the specified name of the variable
	 * 
	 * @return a IBaseSMVariable object
	 */
	public IBaseSMVariable getVariable(String name);

	/**
	 * An array consisting of all form variables for this form.
	 * 
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var variables = frm.getVariables();
	 * for (var i in variables)
	 * 	application.output(variables[i].name);
	 * 
	 * @return an array of all variables on this form
	 * 
	 */
	public IBaseSMVariable[] getVariables();

	/**
	 * Creates a new form IBaseSMMethod - based on the specified code. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var method = form.newMethod('function aMethod(event){application.output("Hello world!");}');
	 * var button = myListViewForm.newButton('Show message!',50,50,100,30,method);
	 * forms['newForm1'].controller.show();
	 *
	 * @param code the specified code for the new method
	 * 
	 * @return a new IBaseSMMethod object for this form
	 */
	public IBaseSMMethod newMethod(String code);

	/**
	 * Gets an existing form method for the given name.
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var method = frm.getMethod("myMethod");
	 * application.output(method.code);
	 * 
	 * @param name the specified name of the method
	 * 
	 * @return a IBaseSMMethod object (or null if the method with the specified name does not exist)
	 */
	public IBaseSMMethod getMethod(String name);

	/**
	 * Returns all existing form methods for this form.
	 * 
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var methods = frm.getMethods();
	 * for (var m in methods)
	 * 	application.output(methods[m].getName());
	 * 
	 * @return all form methods for the form 
	 */
	public IBaseSMMethod[] getMethods();

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getNavigatorID()
	 * 
	 * @sample-mc
	 * var aForm = solutionModel.newForm('newForm1', myDatasource);
	 * // you can also use SM_DEFAULTS.INGORE to just reuse the navigator that is already set.
	 * // here we assign an other new form as the navigator.
	 * var aNavigator = solutionModel.newForm('navForm', myDatasource);
	 * aForm.navigator = aNavigator;
	 * 
	 * @sample 
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * // you can also use SM_DEFAULTS.INGORE to just reuse the navigator that is already set, or SM_DEFAULTS.DEFAULT to have the default servoy navigator.
	 * // here we assign an other new form as the navigator.
	 * var aNavigator = solutionModel.newForm('navForm', myDatasource, null, false, 800, 600);
	 * // set the navigators navigator to NONE
	 * aNavigator.navigator = SM_DEFAULTS.NONE; // Hide the navigator on the form. 
	 * myListViewForm.navigator = aNavigator;
	 * application.output(myListViewForm.navigator.name);
	 * 
	 */
	public Object getNavigator();

	/**
	 * Creates a new IBaseSMField object on the form - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var variable = form.newVariable('myVar', IBaseSMVariable.TEXT);
	 * variable.defaultValue = "'This is a default value (with triple quotes)!'";
	 * var field = form.newField(variable, IBaseSMField.TEXT_FIELD, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();  	
	 *
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 *
	 * @param type the display type of the IBaseSMField object (see the Solution Model -> IBaseSMField node for display types)
	 *
	 * @param x the horizontal "x" position of the IBaseSMField object in pixels
	 *
	 * @param y the vertical "y" position of the IBaseSMField object in pixels
	 *
	 * @param width the width of the IBaseSMField object in pixels
	 *
	 * @param height the height of the IBaseSMField object in pixels
	 * 
	 * @return a new IBaseSMField object (of the specified display type) 
	 */
	public IBaseSMField newField(Object dataprovider, int type, int x, int y, int width, int height);

	/**
	 * Creates a new IBaseSMField object on the form with the displayType of TEXT_FIELD - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600); 
	 * //choose the dataprovider or IBaseSMVariable you want for the Text Field
	 * var x = null;
	 * //global IBaseSMVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',IBaseSMVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form IBaseSMVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',IBaseSMVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var textField = form.newTextField(x,100,100,200,50);
	 * //or a column data provider as the dataprovider
	 * //textField.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 * @param x the horizontal "x" position of the IBaseSMField object in pixels
	 * @param y the vertical "y" position of the IBaseSMField object in pixels
	 * @param width the width of the IBaseSMField object in pixels
	 * @param height the height of the IBaseSMField object in pixels
	 * 
	 * @return a IBaseSMField object with the displayType of TEXT_FIELD
	 */
	public IBaseSMField newTextField(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new IBaseSMField object on the form with the displayType of TEXT_AREA - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var globalVar = solutionModel.newGlobalVariable('globals', 'myGlobal',IBaseSMVariable.TEXT);
	 * globalVar.defaultValue = "'Type your text in here'";
	 * var textArea = form.newTextArea(globalVar,100,100,300,150);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 * @param x the horizontal "x" position of the IBaseSMTabPanel object in pixels
	 * @param y the vertical "y" position of the IBaseSMTabPanel object in pixels
	 * @param width the width of the IBaseSMTabPanel object in pixels
	 * @param height the height of the IBaseSMTabPanel object in pixels
	 * 
	 * @return a IBaseSMField object with the displayType of TEXT_AREA
	 */
	public IBaseSMField newTextArea(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new IBaseSMField object on the form with the displayType of COMBOBOX - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newComboBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 * @param x the horizontal "x" position of the IBaseSMField object in pixels
	 * @param y the vertical "y" position of the IBaseSMField object in pixels
	 * @param width the width of the IBaseSMField object in pixels
	 * @param height the height of the IBaseSMField object in pixels
	 * 
	 * @return a new IBaseSMField object on the form with the displayType of COMBOBOX
	 */
	public IBaseSMField newComboBox(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new IBaseSMField object on the form with the displayType of RADIOS (radio buttons) - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 * 
	 * @sample	
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES); 
	 * vlist.customValues = "value1\nvalue2\nvalue3"; 
	 * var radios = form.newRadios('columnDataProvider',100,100,200,200);
	 * radios.valuelist = vlist;
	 * 
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 * @param x the horizontal "x" position of the IBaseSMField object in pixels
	 * @param y the vertical "y" position of the IBaseSMField object in pixels
	 * @param width the width of the IBaseSMField object in pixels
	 * @param height the height of the IBaseSMField object in pixels
	 * 
	 * @return a IBaseSMField object with the displayType of RADIOS (radio buttons)
	 */
	public IBaseSMField newRadios(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new IBaseSMField object on the form with the displayType of CHECK (checkbox) - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 * 
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newCheck(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 * @param x the horizontal "x" position of the IBaseSMField object in pixels
	 * @param y the vertical "y" position of the IBaseSMField object in pixels
	 * @param width the width of the IBaseSMField object in pixels
	 * @param height the height of the IBaseSMField object in pixels
	 * 
	 * @return a new IBaseSMField object on the form with the displayType of CHECK (checkbox)
	 */
	public IBaseSMField newCheck(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new IBaseSMField object on the form with the displayType of PASSWORD - including the dataprovider/IBaseSMVariable of the IBaseSMField object, the "x" and "y" position of the IBaseSMField object in pixels, as well as the width and height of the IBaseSMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var pass = form.newPassword(scopes.globals.aVariable, 100, 100, 70, 30);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/IBaseSMVariable of the IBaseSMField object
	 * @param x the horizontal "x" position of the IBaseSMField object in pixels
	 * @param y the vertical "y" position of the IBaseSMField object in pixels
	 * @param width the width of the IBaseSMField object in pixels
	 * @param height the height of the IBaseSMField object in pixels
	 * 
	 * @return a new IBaseSMField object on the form with the displayType of PASSWORD
	 */
	public IBaseSMField newPassword(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new button on the form with the given text, place, size and IBaseSMMethod as the onAction event triggered action.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var method = form.newMethod('function onAction(event) { application.output("onAction intercepted on " + event.getFormName()); }');
	 * var button = form.newButton('myButton', 10, 10, 100, 30, method);
	 * application.output("The new button: " + button.name + " has the following onAction event handling method assigned " + button.onAction.getName());
	 *
	 * @param txt the text on the button
	 *
	 * @param x the x coordinate of the button location on the form
	 *
	 * @param y the y coordinate of the button location on the form
	 *
	 * @param width the width of the button
	 * 
	 * @param height the height of the button 
	 *
	 * @param action the method assigned to handle an onAction event
	 * 
	 * @return a new IBaseSMButton object
	 */
	public IBaseSMButton newButton(String txt, int x, int y, int width, int height, Object action);

	/**
	 * Creates a new IBaseSMLabel object on the form - including the text of the label, the "x" and "y" position of the label object in pixels, the width and height of the label object in pixels.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var label = form.newLabel('The text on the label', 140, 140, 50, 20);
	 * forms['newForm1'].controller.show(); 
	 *
	 * @param txt the specified text of the label object
	 *
	 * @param x the horizontal "x" position of the label object in pixels
	 *
	 * @param y the vertical "y" position of the label object in pixels
	 *
	 * @param width the width of the label object in pixels
	 *
	 * @param height the height of the label object in pixels
	 * 
	 * @return a IBaseSMLabel object
	 */
	public IBaseSMLabel newLabel(String txt, int x, int y, int width, int height);

	/**
	 * Creates a new ISMPortal object on the form - including the name of the ISMPortal object; the relation the ISMPortal object is based on, the "x" and "y" position of the ISMPortal object in pixels, as well as the width and height of the ISMPortal object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/table1','db:/server2/table2',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('another_parent_table_id', '=', 'another_child_table_parent_id');
	 * var portal = form.newPortal('portal',relation,200,200,300,300);
	 * portal.newField('someColumn',ISMField.TEXT_FIELD,200,200,120);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param name the specified name of the ISMPortal object
	 * @param relation the relation of the ISMPortal object
	 * @param x the horizontal "x" position of the ISMPortal object in pixels
	 * @param y the vertical "y" position of the ISMPortal object in pixels
	 * @param width the width of the ISMPortal object in pixels
	 * @param height the height of the ISMPortal object in pixels
	 * 
	 * @return a ISMPortal object 
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMPortal newPortal(String name, Object relation, int x, int y, int width, int height);

	/**
	 * Returns a ISMPortal that has the given name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var portal = frm.getPortal("myPortal");
	 * portal.initialSort = 'my_table_text desc';
	 *
	 * @param name the specified name of the portal
	 * 
	 * @return a ISMPortal object
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMPortal getPortal(String name);

	/**
	 * Removes a ISMPortal that has the given name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,800,600);
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/myTable','db:/server1/myOtherTable',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * var ISMPortal = form.newPortal('jsp',relation,100,400,300,300);
	 * ISMPortal.newField('child_table_id',ISMField.TEXT_FIELD,200,200,120);
	 * var ISMMethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jsp') == true) application.output('Portal removed ok'); else application.output('Portal could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the portal',450,500,250,50,method);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 * 
	 * @param name the specified name of the ISMPortal to be removed
	 * 
	 * @return true if the ISMPortal has successfully been removed; false otherwise
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public boolean removePortal(String name);

	/**
	 * Returns all ISMPortal objects of this form (not including the ones from the parent form), including the ones without a name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var portals = frm.getPortals();
	 * for (var i in portals)
	 * {
	 * 	var p = portals[i];
	 * 	if (p.name != null)
	 * 		application.output(p.name);
	 * 	else
	 * 		application.output("unnamed portal detected");
	 * }
	 * 
	 * @return an array of all ISMPortal objects on this form
	 *
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMPortal[] getPortals();

	/**
	 * Creates a new IBaseSMTabPanel object on the form - including the name of the IBaseSMTabPanel object, the "x" and "y" position of the IBaseSMTabPanel object in pixels, as well as the width and height of the IBaseSMTabPanel object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm','db:/server1/parent_table',null,false,640,480); 
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_text', IBaseSMField.TEXT_FIELD,10,10,100,20); 
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * var childTwo = solutionModel.newForm('childTwo','db:/server1/my_table',null,false,400,300);
	 * childTwo.newField('my_table_image', IBaseSMField.IMAGE_MEDIA,10,10,100,100); 
	 * var tabPanel = form.newTabPanel('tabs',10,10,620,460);
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * forms['parentForm'].controller.show();
	 * 
	 * @param name the specified name of the IBaseSMTabPanel object
	 * @param x the horizontal "x" position of the IBaseSMTabPanel object in pixels
	 * @param y the vertical "y" position of the IBaseSMTabPanel object in pixels
	 * @param width the width of the IBaseSMTabPanel object in pixels
	 * @param height the height of the IBaseSMTabPanel object in pixels
	 * 
	 * @return a IBaseSMTabPanel object	
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMTabPanel newTabPanel(String name, int x, int y, int width, int height);

	/**
	 * Returns a IBaseSMTabPanel that has the given name.
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var tabPanel = frm.getTabPanel("myTabPanel"); 
	 * var tabs = tabPanel.getTabs(); 
	 * for (var i=0; i<tabs.length; i++) 
	 *	application.output("Tab " + i + " has text " + tabs[i].text);	 
	 * 
	 * @param name the specified name of the tabpanel
	 * 
	 * @return a IBaseSMTabPanel object
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMTabPanel getTabPanel(String name);

	/**
	 * Removes a IBaseSMTabPanel that has the given name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX','db:/server1/parent_table',null,false,800,600);
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_text', IBaseSMField.TEXT_FIELD,10,10,100,20); 
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_id');
	 * var childTwo = solutionModel.newForm('childTwo','db:/server1/another_table',null,false,400,300);
	 * childTwo.newField('columnDataProvider', IBaseSMField.TEXT_FIELD,10,10,100,100);
	 * var tabPanel = form.newTabPanel('jst',10,10,620,460);
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * var method = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jst') == true)\n application.output('TabPanel has been removed ok');\n else\n application.output('TabPanel could not be deleted');\n forms['newFormX'].controller.recreateUI();\n}");
	 * var removerButton = form.newButton('Click here to remove the tab panel',450,500,250,50,method);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show(); 
	 *
	 * @param name the specified name of the IBaseSMTabPanel to be removed
	 * 
	 * @return true is the IBaseSMTabPanel has been successfully removed, false otherwise
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public boolean removeTabPanel(String name);

	/**
	 * Returns all IBaseSMTabPanels of this form (not including the ones from the parent form), including the ones without a name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var tabPanels = frm.getTabPanels();
	 * for (var i in tabPanels)
	 * {
	 *	var tp = tabPanels[i];
	 *	if (tp.name != null)
	 *		application.output("Tab " + tp.name + " has text " + tp.text);
	 *	else
	 *		application.output("Tab with text " + tp.text + " has no name");
	 * }
	 *
	 * @return an array of all IBaseSMTabPanel objects on this form			
	 *		
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public IBaseSMTabPanel[] getTabPanels();

	/**
	 * The field with the specified name.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var field = form.getField("myField");
	 * application.output(field.dataProviderID);
	 * 
	 * @param name the specified name of the field
	 * 
	 * @return a IBaseSMField object
	 */
	public IBaseSMField getField(String name);

	/**
	 * Removes a IBaseSMField that has the given name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,800,600);
	 * var IBaseSMField = form.newField(scopes.globals.myGlobalVariable,IBaseSMField.TEXT_FIELD,100,300,200,50);
	 * IBaseSMField.name = 'jsf';
	 * var method = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jsf') == true) application.output('Field has been removed ok'); else application.output('Field could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the field',450,500,250,50,method);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the IBaseSMField to remove 
	 * 
	 * @return true is the IBaseSMField has been successfully removed; false otherwise
	 */
	public boolean removeField(String name);

	/**
	 * Returns all IBaseSMField objects of this form, including the ones without a name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var fields = frm.getFields();
	 * for (var f in fields)
	 * {
	 * 	var fname = fields[f].name;
	 * 	if (fname != null)
	 * 		application.output(fname);
	 * }
	 * 
	 * @return all IBaseSMField objects of this form
	 *
	 */
	public IBaseSMField[] getFields();

	/**
	 * Returns a IBaseSMButton that has the given name.
	 *
	 * @sample 
	 * var btn = myForm.getButton("hello");
	 * application.output(btn.text);
	 *
	 * @param name the specified name of the button
	 * 
	 * @return a IBaseSMButton object 
	 */
	public IBaseSMButton getButton(String name);

	/**
	 * Removes a IBaseSMButton that has the specified name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,800,600);
	 * var b1 = form.newButton('This is button1',100,100,200,50,null);
	 * b1.name = 'b1';
	 * var method = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX'); if (form.removeButton('b1') == true) application.output('Button has been removed ok'); else application.output('Button could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var b2 = form.newButton('Click here to remove button1',100,230,200,50,method);
	 * b2.name = 'b2';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the IBaseSMButton to be removed 
	 * 
	 * @return true if the IBaseSMButton has been removed; false otherwise
	 */
	public boolean removeButton(String name);

	/**
	 * Returns all IBaseSMButtons of this form, including the ones without a name.
	 *
	 * @sample 
	 * var buttons = myForm.getButtons();
	 * for (var b in buttons)
	 * {
	 * 	if (buttons[b].name != null) 
	 * 		application.output(buttons[b].name);
	 * 	else
	 * 		application.output(buttons[b].text + " has no name ");
	 * }
	 * 
	 * @return the list of all IBaseSMButtons on this forms
	 *
	 */
	public IBaseSMButton[] getButtons();

	/**
	 * Returns a IBaseSMComponent that has the given name; if found it will be a IBaseSMField, IBaseSMLabel, IBaseSMButton, IBaseSMPortal, IBaseSMBean or IBaseSMTabPanel.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var cmp = frm.getComponent("componentName");
	 * application.output("Component type and name: " + cmp);
	 *
	 * @param name the specified name of the component 
	 * 
	 * @return a IBaseSMComponent object (might be a IBaseSMField, IBaseSMLabel, IBaseSMButton, IBaseSMPortal, IBaseSMBean or IBaseSMTabPanel)
	 */
	public IBaseSMComponent getComponent(String name);

	/**
	 * Removes a component (IBaseSMLabel, IBaseSMButton, IBaseSMField, IBaseSMPortal, IBaseSMBean, IBaseSMTabPanel) that has the given name. It is the same as calling "if(!removeLabel(name) &amp;&amp; !removeButton(name) ....)".
	 * Returns true if removal was successful, false otherwise.  
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX','db:/server1/parent_table',null,true,1000,750);
	 * var IBaseSMButton = form.newButton('IBaseSMButton to delete',100,100,200,50,null);
	 * IBaseSMButton.name = 'jsb';
	 * var IBaseSMLabel = form.newLabel('IBaseSMLabel to delete',100,200,200,50,null);
	 * IBaseSMLabel.name = 'jsl';
	 * IBaseSMLabel.transparent = false;
	 * IBaseSMLabel.background = 'green';
	 * var IBaseSMField = form.newField('scopes.globals.myGlobalVariable',IBaseSMField.TEXT_FIELD,100,300,200,50);
	 * IBaseSMField.name = 'jsf';
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * var IBaseSMPortal = form.newPortal('jsp',relation,100,400,300,300);
	 * IBaseSMPortal.newField('child_table_id',IBaseSMField.TEXT_FIELD,200,200,120);
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_id', IBaseSMField.TEXT_FIELD,10,10,100,20);
	 * var childTwo = solutionModel.newForm('childTwo','server1','other_table',null,false,400,300);
	 * childTwo.newField('some_table_id', IBaseSMField.TEXT_FIELD,10,10,100,100); 
	 * var IBaseSMTabPanel = form.newTabPanel('jst',450,30,620,460);
	 * IBaseSMTabPanel.newTab('tab1','Child One',childOne,relation);
	 * IBaseSMTabPanel.newTab('tab2','Child Two',childTwo);
	 * var method = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if ((form.removeComponent('jsb') == true) && (form.removeComponent('jsl') == true) && (form.removeComponent('jsf') == true) && (form.removeComponent('jsp') == true) & (form.removeComponent('jst') == true)) application.output('Components removed ok'); else application.output('Some component(s) could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove form components',450,500,250,50,method);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the component to be deleted
	 * 
	 * @return true if component has been successfully deleted; false otherwise
	 */
	public boolean removeComponent(String name);

	/**
	 * Returns a array of all the IBaseSMComponents that a form has; they are of type IBaseSMField,IBaseSMLabel,IBaseSMButton,IBaseSMPortal,IBaseSMBean or IBaseSMTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]); 
	 * 
	 * @return an array of all the IBaseSMComponents on the form.
	 */
	public IBaseSMComponent[] getComponents();

	/**
	 * Returns a IBaseSMLabel that has the given name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var label = frm.getLabel("myLabel");
	 * application.output(label.text); 
	 *
	 * @param name the specified name of the label
	 * 
	 * @return a IBaseSMLabel object (or null if the label with the specified name does not exist)
	 */
	public IBaseSMLabel getLabel(String name);

	/**
	 * Removes a IBaseSMLabel that has the given name. Returns true if removal successful, false otherwise  
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,1000,750);
	 * var IBaseSMLabel = form.newLabel('IBaseSMLabel to delete',100,200,200,50,null);
	 * IBaseSMLabel.name = 'jsl';
	 * IBaseSMLabel.transparent = false;
	 * IBaseSMLabel.background = 'green';
	 * var method = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX'); if (form.removeComponent('jsl') == true) application.output('Label has been removed'); else application.output('Label could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the green label',450,500,250,50,method);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 * 
	 * @param name the specified name of the IBaseSMLabel to be removed
	 * 
	 * @return true if the IBaseSMLabel with the given name has successfully been removed; false otherwise
	 */
	public boolean removeLabel(String name);

	/**
	 * Returns all IBaseSMLabels of this form (not including its super form), including the ones without a name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var labels = frm.getLabels();
	 * for (var i in labels)
	 * {
	 * 	var lname = labels[i].name;
	 * 	if (lname != null)
	 * 		application.output(lname);
	 * }
	 * 
	 * @return all IBaseSMLabels on this form
	 *
	 */
	public IBaseSMLabel[] getLabels();

	/**
	 * The name of the form.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * var formName = form.name;
	 * application.output(formName);
	 * 
	 */
	public String getName();

	/**
	 * Get the server name used by this form.
	 * 
	 * @sample 
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * form.serverName = 'anotherServerName';
	 * var theServerName = form.getServerName();
	 * application.output(theServerName);
	 */
	public String getServerName();

	/**
	 * The [name of the table/SQL view].[the name of the database server connection] the form is based on.
	 * 
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * aForm.tableName = 'anotherTableOfMine'
	 * if (forms['newForm1'].controller.find())
	 * {
	 * 	columnTextDataProvider = '=aSearchedValue'
	 * 	columnNumberDataProvider = '>10';
	 * 	forms['newForm1'].controller.search()
	 * }
	 */
	public String getTableName();

	/**
	 * The names of the database server and table that this form is linked to.
	 * 
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/a_server/a_table', 'aStyleName', false, 800, 600)
	 * myForm.dataSource = 'db:/anotherServerName/anotherTableName'
	 * 
	 */
	public String getDataSource();

	public void setServerName(String arg);

	public void setTableName(String arg);

	public void setDataSource(String arg);

	/**
	 * Removes a form JSVariable - based on the name of the variable object.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', null, null, true, 800, 600);
	 * var variable = form.newVariable('myVar', JSVariable.TEXT);
	 * variable.defaultValue = "'This is a default value (with triple quotes)!'";
	 * //variable.defaultValue = "{a:'First letter',b:'Second letter'}"
	 * var field = form.newField(variable, JSField.TEXT_FIELD, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * variable = form.removeVariable('myVar');
	 * application.sleep(4000);
	 * forms['newForm1'].controller.recreateUI();
	 *
	 * @param name the specified name of the variable
	 * 
	 * @return true if removed, false otherwise (ex: no var with that name)
	 */
	public boolean removeVariable(String name);

	/**
	 * Removes a  form JSMethod - based on the specified code. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', null, null, true, 800, 600);
	 * var hello = form.newMethod('function aMethod(event){application.output("Hello world!");}');
	 * var removeMethod = form.newMethod('function removeMethod(event){ \
	 *									solutionModel.getForm(event.getFormName()).removeMethod("aMethod"); \
	 *									forms[event.getFormName()].controller.recreateUI();\
	 *									}');
	 * var button1 = form.newButton('Call method!',50,50,120,30,hello);
	 * var button2 = form.newButton('Remove Mehtod!',200,50,120,30,removeMethod);
	 * forms['newForm1'].controller.show();
	 *
	 * @param name the specified name of the method
	 * 
	 * @return true if method was removed successfully , false otherwise
	 */
	public boolean removeMethod(String name);

	/**
	 * The default form view mode. 
	 * 
	 * The view can be changed using a method at runtime. The following views are available: 
	 * - Record view 
	 * - List view 
	 * - Record view (locked) 
	 * - List view (locked) 
	 * - Table View (locked) 
	 * 
	 * NOTE: Only Table View (locked) uses asynchronized related data loading. 
	 * This feature defers all related foundset data loading to the background - enhancing 
	 * the visual display of a related foundset.
	 *
	 * @sample 
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * myForm.view = JSForm.RECORD_VIEW;
	 * forms['newForm1'].controller.show();
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public int getView();

	public void setView(int arg);

	/**
	 * The method that is triggered EVERY TIME the form is displayed; an argument must be passed to the method if this is the first time the form is displayed. 
	 * 
	 * NOTE: onShow can be used to access current foundset dataproviders; onLoad cannot be used because the foundset data is not loaded until after the form is loaded. 
	 * 
	 * NOTE: the onShow event bubbles down, meaning that the onShow event of a form displayed in a tabPanel is fired after the onShow event of the parent.
	 * 
	 * @sample
	 * form.onShow = form.newMethod('function onShow(firstShow, event) { application.output("onShow intercepted on " + event.getFormName() + ". first show? " + firstShow); return false; }');
	 * form.onHide = form.newMethod('function onHide(event) { application.output("onHide blocked on " + event.getFormName()); return false; }');
	 */
	public IBaseSMMethod getOnShow();

	public void setOnShow(IBaseSMMethod method);

	/**
	 * The method that is triggered when a form is loaded/reloaded from the repository; used to alter elements, set globals, hide toolbars, 
	 * etc; onShow method can also be assigned.
	 * NOTE: onShow should be used to access current foundset dataproviders; onLoad cannot be used because the foundset data is not loaded until after the form is loaded. 
	 * Also calls to loadRecords() should be done in the onShow method and not in the onLoad method
	 * If you call loadRecords() in the onShow method, you may want to set the namedFoundSet property of the form to 'empty' to prevent the first default form query.
	 * NOTE: the onLoad event bubbles down, meaning that the onLoad is first fired on the parent then on a tab in a tabpanel (and in tab of that tab panels if you are 3 deep)
	 * 
	 * @sample
	 * form.onLoad = form.newMethod('function onLoad(event) { application.output("onLoad intercepted on " + event.getFormName()); }');
	 * form.onUnLoad = form.newMethod('function onUnLoad(event) { application.output("onUnLoad intercepted on " + event.getFormName()); }');
	 */
	public IBaseSMMethod getOnLoad();

	public void setOnLoad(IBaseSMMethod method);

	/**
	 * The method that is triggered when another form is being activated. 
	 * NOTE: If the onHide method returns false, the form can be prevented from hiding. 
	 * For example, when using onHide with showFormInDialog, the form will not close by clicking the dialog close box (X).
	 *
	 * @sampleas getOnShow()
	 * @see #getOnShow()
	 */
	public IBaseSMMethod getOnHide();

	public void setOnHide(IBaseSMMethod method);

	/**
	 * The method that is triggered each time a record is selected. 
	 * If a form is in List view or Special table view - when the user clicks on it.
	 * In Record view - after the user navigates to another record using the slider or clicks up or down for next/previous record. 
	 * NOTE: Data and Servoy tag values are returned when the onRecordSelection method is executed.
	 * 
	 * @sample
	 * form.onRecordSelection = form.newMethod('function onRecordSelection(event) { application.output("onRecordSelection intercepted on " + event.getFormName()); }');
	 */
	public IBaseSMMethod getOnRecordSelection();

	public void setOnRecordSelection(IBaseSMMethod method);

	/**
	 * Gets a part of the form from the given type (see ISMPart constants). 
	 *
	 * @sample 
	 * form.getPart(ISMPart.HEADER).background = 'red';
	 * form.getPart(ISMPart.LEADING_SUBSUMMARY, 160).background = 'red';
	 *
	 * @param type The type of the part to retrieve.
	 *
	 * @return A IBaseSMPart instance representing the retrieved form part.
	 */
	@ServoyClientSupport(mc = false, sc = true, wc = true)
	public IBaseSMPart getPart(int type);

	/**
	 * Creates a new Header part on the form.
	 * 
	 * @sample
	 * var header = form.newHeaderPart(80);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Header form part.
	 */
	public IBaseSMPart newHeaderPart(int height);

	/**
	 * Creates a new Footer part on the form.
	 *
	 * @sample 
	 * var footer = form.newFooterPart(440);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Footer form part.
	 */
	public IBaseSMPart newFooterPart(int height);

	/**
	 * Returns a IBaseSMBean that has the given name.
	 *
	 * @sample 
	 * var btn = myForm.getBean("mybean");
	 * application.output(mybean.className);
	 *
	 * @param name the specified name of the bean
	 * 
	 * @return a IBaseSMBean object 
	 */
	public IBaseSMBean getBean(String name);

	/**
	 * Removes a IBaseSMBean that has the specified name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.getForm('myform');
	 * form.removeBean('mybean')
	 *  
	 * @param name the specified name of the ISMBean to be removed 
	 * 
	 * @return true if the IBaseSMBean has been removed; false otherwise
	 */
	public boolean removeBean(String name);

}
