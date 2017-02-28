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

package com.servoy.j2db.solutionmodel;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.Form;

/**
 * Solution model form object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMForm extends IBaseSMForm, ISMHasDesignTimeProperty, ISMHasUUID
{

	//	public static final int TABLE_VIEW = FormController.TABLE_VIEW;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMForm#LIST_VIEW
	 * @see com.servoy.base.solutionmodel.IBaseSMForm#LIST_VIEW
	 */
	public static final int LOCKED_LIST_VIEW = IFormConstants.VIEW_TYPE_LIST_LOCKED;

	/**
	 * @sameas com.servoy.base.solutionmodel.IBaseSMForm#LIST_VIEW
	 * @see com.servoy.base.solutionmodel.IBaseSMForm#LIST_VIEW
	 */
	public static final int LOCKED_RECORD_VIEW = IFormConstants.VIEW_TYPE_RECORD_LOCKED;

	/**
	 * The constants to set or get the encapsulation property of a JSForm. 
	 * They are as follows: JSForm.DEFAULT_ENCAPSULATION, JSForm.PRIVATE_ENCAPSULATION, JSForm.MODULE_PRIVATE_ENCAPSULATION, JSForm.HIDE_DATAPROVIDERS_ENCAPSULATION, JSForm.HIDE_FOUNDSET_ENCAPSULATION, JSForm.HIDE_CONTROLLER_ENCAPSULATION, JSForm.HIDE_ELEMENTS_ENCAPSULATION
	 *
	 * @sample 
	 * var myDefaultForm = solutionModel.newForm('newForm1', myDatasource, myStyleName, false, 800, 600);
	 * myDefaultForm.encapsulation = JSForm.DEFAULT_ENCAPSULATION;
	 * 
	 * var myPrivateForm = solutionModel.newForm('newForm2', myDatasource, myStyleName, false, 800, 600);
	 * myPrivateForm.encapsulation = JSForm.PRIVATE_ENCAPSULATION;
	 * 
	 * var myModulePrivateForm = solutionModel.newForm('newForm3', myDatasource, myStyleName, false, 800, 600);
	 * myModulePrivateForm.encapsulation = JSForm.MODULE_PRIVATE_ENCAPSULATION;
	 * 
	 * var myHideDataprovidersForm = solutionModel.newForm('newForm4', myDatasource, myStyleName, false, 800, 600);
	 * myHideDataprovidersForm.encapsulation = JSForm.HIDE_DATAPROVIDERS_ENCAPSULATION;
	 * 
	 * var myHideFoundsetForm = solutionModel.newForm('newForm5', myDatasource, myStyleName, false, 800, 600);
	 * myHideFoundsetForm.encapsulation = JSForm.HIDE_FOUNDSET_ENCAPSULATION;
	 * 
	 * var myHideControllerForm = solutionModel.newForm('newForm6', myDatasource, myStyleName, false, 800, 600);
	 * myHideControllerForm.encapsulation = JSForm.HIDE_CONTROLLER_ENCAPSULATION;
	 * 
	 * var myHideElementsForm = solutionModel.newForm('newForm7', myDatasource, myStyleName, false, 800, 600);
	 * myHideElementsForm.encapsulation = JSForm.HIDE_ELEMENTS_ENCAPSULATION;
	 * 
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int DEFAULT_ENCAPSULATION = IFormConstants.DEFAULT;

	/**
	 * @sameas DEFAULT_ENCAPSULATION
	 * @see #DEFAULT_ENCAPSULATION
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int PRIVATE_ENCAPSULATION = IFormConstants.HIDE_IN_SCRIPTING_MODULE_SCOPE;

	/**
	 * @sameas DEFAULT_ENCAPSULATION
	 * @see #DEFAULT_ENCAPSULATION
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int MODULE_PRIVATE_ENCAPSULATION = IFormConstants.MODULE_SCOPE;

	/**
	 * @sameas DEFAULT_ENCAPSULATION
	 * @see #DEFAULT_ENCAPSULATION
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int HIDE_DATAPROVIDERS_ENCAPSULATION = IFormConstants.HIDE_DATAPROVIDERS;

	/**
	 * @sameas DEFAULT_ENCAPSULATION
	 * @see #DEFAULT_ENCAPSULATION
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int HIDE_FOUNDSET_ENCAPSULATION = IFormConstants.HIDE_FOUNDSET;

	/**
	 * @sameas DEFAULT_ENCAPSULATION
	 * @see #DEFAULT_ENCAPSULATION
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int HIDE_CONTROLLER_ENCAPSULATION = IFormConstants.HIDE_CONTROLLER;

	/**
	 * @sameas DEFAULT_ENCAPSULATION
	 * @see #DEFAULT_ENCAPSULATION
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public static final int HIDE_ELEMENTS_ENCAPSULATION = IFormConstants.HIDE_ELEMENTS;

	/**
	 * Constant used for form namedFoundset property. The form that uses empty namedFoundset will initially have an empty (cleared) foundset.
	 * 
	 * @sample
	 * // form with empty foundset
	 * var frmEmpty = solutionModel.newForm('products_empty', 'example_data', 'products', null, true, 640, 480);
	 * frmEmpty.newLabel("Empty FoundSet",10,10,200,20);
	 * frmEmpty.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 * frmEmpty.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 * frmEmpty.namedFoundSet = JSForm.EMPTY_FOUNDSET;
	 */
	public static final String EMPTY_FOUNDSET = Form.NAMED_FOUNDSET_EMPTY;

	/**
	 * Constant used for form namedFoundset property. The form that uses a separate namedFoundset will initially have an separate (not shared with other forms) foundset.
	 * 
	 * @sample
	 * // form with separate foundset
	 * var frmSeparate = solutionModel.newForm('products_separate', 'example_data', 'products', null, true, 640, 480);
	 * frmSeparate.newLabel("Separate FoundSet",10,10,200,20);
	 * frmSeparate.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 * frmSeparate.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 * frmSeparate.namedFoundSet = JSForm.SEPARATE_FOUNDSET;
	 * forms['products_separate'].controller.find();
	 * forms['products_separate'].categoryid = '=2';
	 * forms['products_separate'].controller.search();
	 */
	public static final String SEPARATE_FOUNDSET = Form.NAMED_FOUNDSET_SEPARATE;

	/**
	 * @clonedesc com.servoy.j2db.IForm#SELECTION_MODE_DEFAULT
	 * @see com.servoy.j2db.IForm#SELECTION_MODE_DEFAULT
	 * 
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * myForm.selectionMode = JSForm.SELECTION_MODE_DEFAULT;
	 */
	public static final int SELECTION_MODE_DEFAULT = IForm.SELECTION_MODE_DEFAULT;

	/**
	 * @clonedesc com.servoy.j2db.IForm#SELECTION_MODE_SINGLE
	 * @see com.servoy.j2db.IForm#SELECTION_MODE_SINGLE
	 * 
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * myForm.selectionMode = JSForm.SELECTION_MODE_SINGLE;
	 */
	public static final int SELECTION_MODE_SINGLE = IForm.SELECTION_MODE_SINGLE;

	/**
	 * @clonedesc com.servoy.j2db.IForm#SELECTION_MODE_MULTI
	 * @see com.servoy.j2db.IForm#SELECTION_MODE_MULTI
	 * 
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * myForm.selectionMode = JSForm.SELECTION_MODE_MULTI;
	 */
	public static final int SELECTION_MODE_MULTI = IForm.SELECTION_MODE_MULTI;

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
	public ISMVariable newVariable(String name, int type);

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
	public ISMVariable newVariable(String name, int type, String defaultValue);

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
	 * @return a ISMVariable object
	 */
	public ISMVariable getVariable(String name);

	/**
	 * An array consisting of all form variables for this form.
	 * 
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var variables = frm.getVariables();
	 * for (var i in variables)
	 * 	application.output(variables[i].name);
	 * 
	 * @param returnInheritedElements boolean true to also return the elements from the parent form 
	 * @return an array of all variables on this form
	 * 
	 */
	public ISMVariable[] getVariables(boolean returnInheritedElements);

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
	public ISMVariable[] getVariables();

	/**
	 * Creates a new form ISMMethod - based on the specified code. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var method = form.newMethod('function aMethod(event){application.output("Hello world!");}');
	 * var button = myListViewForm.newButton('Show message!',50,50,100,30,method);
	 * forms['newForm1'].controller.show();
	 *
	 * @param code the specified code for the new method
	 * 
	 * @return a new ISMMethod object for this form
	 */
	public ISMMethod newMethod(String code);

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
	 * @return a ISMMethod object (or null if the method with the specified name does not exist)
	 */
	public ISMMethod getMethod(String name);

	/**
	 * Returns all existing form methods for this form.
	 * 
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var methods = frm.getMethods();
	 * for (var m in methods)
	 * 	application.output(methods[m].getName());
	 * 
	 * @param returnInheritedElements boolean true to also return the elements from the parent form 
	 * @return all form methods for the form 
	 */
	public ISMMethod[] getMethods(boolean returnInheritedElements);

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
	public ISMMethod[] getMethods();

	/**
	 * Creates a new ISMField object on the form - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var variable = form.newVariable('myVar', ISMVariable.TEXT);
	 * variable.defaultValue = "'This is a default value (with triple quotes)!'";
	 * var field = form.newField(variable, ISMField.TEXT_FIELD, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();  	
	 *
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 *
	 * @param type the display type of the ISMField object (see the Solution Model -> ISMField node for display types)
	 *
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 *
	 * @param y the vertical "y" position of the ISMField object in pixels
	 *
	 * @param width the width of the ISMField object in pixels
	 *
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object (of the specified display type) 
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newField(Object dataprovider, int type, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of TEXT_FIELD - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600); 
	 * //choose the dataprovider or ISMVariable you want for the Text Field
	 * var x = null;
	 * //global ISMVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',ISMVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form ISMVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',ISMVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var textField = form.newTextField(x,100,100,200,50);
	 * //or a column data provider as the dataprovider
	 * //textField.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a ISMField object with the displayType of TEXT_FIELD
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newTextField(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of TEXT_AREA - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var globalVar = solutionModel.newGlobalVariable('globals', 'myGlobal',ISMVariable.TEXT);
	 * globalVar.defaultValue = "'Type your text in here'";
	 * var textArea = form.newTextArea(globalVar,100,100,300,150);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMTabPanel object in pixels
	 * @param y the vertical "y" position of the ISMTabPanel object in pixels
	 * @param width the width of the ISMTabPanel object in pixels
	 * @param height the height of the ISMTabPanel object in pixels
	 * 
	 * @return a ISMField object with the displayType of TEXT_AREA
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newTextArea(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of COMBOBOX - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newComboBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of COMBOBOX
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newComboBox(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of LISTBOX - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * var list = form.newListBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of LISTBOX
	 */
	public ISMField newListBox(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of MULTISELECT_LISTBOX - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * var calendar = form.newMultiSelectListBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of MULTISELECT_LISTBOX
	 */
	public ISMField newMultiSelectListBox(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of SPINNER - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * var spinner = form.newSpinner(myDataProvider, 10, 460, 100, 20);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of SPINNER
	 */
	public ISMField newSpinner(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of RADIOS (radio buttons) - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample	
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES); 
	 * vlist.customValues = "value1\nvalue2\nvalue3"; 
	 * var radios = form.newRadios('columnDataProvider',100,100,200,200);
	 * radios.valuelist = vlist;
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a ISMField object with the displayType of RADIOS (radio buttons)
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newRadios(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of CHECK (checkbox) - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newCheck(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of CHECK (checkbox)
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newCheck(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of CALENDAR - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newCalendar(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels 
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of CALENDAR
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newCalendar(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of RTF_AREA (enables more than one line of text to be displayed in a field) - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var rtf_area = form.newRtfArea('columnDataProvider',100,100,100,100);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a ISMField object with the displayType of RTF_AREA
	 */
	public ISMField newRtfArea(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of HTML_AREA - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var textProvider = form.newVariable('myVar',ISMVariable.TEXT);
	 * textProvider.defaultValue = "'This is a triple quoted text!'";
	 * var htmlArea = myListViewForm.newHtmlArea(textProvider,100,100,100,100);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a ISMField object on the form with the displayType of HTML_AREA 
	 */
	public ISMField newHtmlArea(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of IMAGE_MEDIA - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var myMediaVar = form.newVariable("media", ISMVariable.MEDIA);
	 * var imageMedia = form.newImageMedia(myMediaVar,100,100,200,200)
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of IMAGE_MEDIA
	 */
	public ISMField newImageMedia(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of TYPE_AHEAD - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "value1\nvalue2\nvalue3";
	 * var typeAhead = form.newTypeAhead(columnTextDataProvider,100,100,300,200);
	 * typeAhead.valuelist = vlist;
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a ISMField object with the displayType of TYPE_AHEAD
	 */
	public ISMField newTypeAhead(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new ISMField object on the form with the displayType of PASSWORD - including the dataprovider/ISMVariable of the ISMField object, the "x" and "y" position of the ISMField object in pixels, as well as the width and height of the ISMField object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var pass = form.newPassword(scopes.globals.aVariable, 100, 100, 70, 30);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param dataprovider the specified dataprovider name/ISMVariable of the ISMField object
	 * @param x the horizontal "x" position of the ISMField object in pixels
	 * @param y the vertical "y" position of the ISMField object in pixels
	 * @param width the width of the ISMField object in pixels
	 * @param height the height of the ISMField object in pixels
	 * 
	 * @return a new ISMField object on the form with the displayType of PASSWORD
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMField newPassword(Object dataprovider, int x, int y, int width, int height);

	/**
	 * Creates a new button on the form with the given text, place, size and ISMMethod as the onAction event triggered action.
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
	 * @return a new ISMButton object
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMButton newButton(String txt, int x, int y, int width, int height, Object action);

	/**
	 * Creates a new ISMLabel object on the form - including the text of the label, the "x" and "y" position of the label object in pixels, the width and height of the label object in pixels.
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
	 * @return a ISMLabel object
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMLabel newLabel(String txt, int x, int y, int width, int height);

	/**
	 * Creates a new ISMLabel object on the form - including the text of the label, the "x" and "y" position of the label object in pixels, the width and height of the label object in pixels and a ISMMethod action such as the method for an onAction event.
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
	 * @param action the event action ISMMethod of the label object
	 * 
	 * @return a ISMLabel object
	 */
	public ISMLabel newLabel(String txt, int x, int y, int width, int height, Object action);

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
	public ISMPortal newPortal(String name, Object relation, int x, int y, int width, int height);

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
	public ISMPortal getPortal(String name);

	/**
	 * Returns all ISMPortal objects of this form (optionally also the ones from the parent form), including the ones without a name.
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
	 * @param returnInheritedElements boolean true to also return the elements from parent form 
	 * @return an array of all ISMPortal objects on this form
	 *
	 */
	public ISMPortal[] getPortals(boolean returnInheritedElements);

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
	public ISMPortal[] getPortals();

	/**
	 * Creates a new ISMTabPanel object on the form - including the name of the ISMTabPanel object, the "x" and "y" position of the ISMTabPanel object in pixels, as well as the width and height of the ISMTabPanel object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('parentForm','db:/server1/parent_table',null,false,640,480); 
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_text', ISMField.TEXT_FIELD,10,10,100,20); 
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * var childTwo = solutionModel.newForm('childTwo','db:/server1/my_table',null,false,400,300);
	 * childTwo.newField('my_table_image', ISMField.IMAGE_MEDIA,10,10,100,100); 
	 * var tabPanel = form.newTabPanel('tabs',10,10,620,460);
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * forms['parentForm'].controller.show();
	 * 
	 * @param name the specified name of the ISMTabPanel object
	 * @param x the horizontal "x" position of the ISMTabPanel object in pixels
	 * @param y the vertical "y" position of the ISMTabPanel object in pixels
	 * @param width the width of the ISMTabPanel object in pixels
	 * @param height the height of the ISMTabPanel object in pixels
	 * 
	 * @return a ISMTabPanel object	
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMTabPanel newTabPanel(String name, int x, int y, int width, int height);

	/**
	 * Returns a ISMTabPanel that has the given name.
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
	 * @return a ISMTabPanel object
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMTabPanel getTabPanel(String name);

	/**
	 * Returns all ISMTabPanels of this form (optionally the ones from the parent form), including the ones without a name.
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
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return an array of all ISMTabPanel objects on this form			
	 *		
	 */
	public ISMTabPanel[] getTabPanels(boolean returnInheritedElements);

	/**
	 * Returns all ISMTabPanels of this form (not including the ones from the parent form), including the ones without a name.
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
	 * @return an array of all ISMTabPanel objects on this form			
	 *		
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMTabPanel[] getTabPanels();

	/**
	 * Creates a new part on the form. The type of the new part (use one of the ISMPart constants)
	 * and its height must be specified.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('myForm', 'db:/example_data/my_table', null, false, 1200, 800);
	 * var header = form.newPart(ISMPart.HEADER, 100);
	 * header.background = 'yellow';
	 * var body = form.newPart(ISMPart.BODY, 700);
	 * body.background = 'green';
	 * var footer = form.newPart(ISMPart.FOOTER, 800);
	 * footer.background = 'orange';
	 *
	 * @param type The type of the new part.
	 * 
	 * @param height The height of the new part
	 * 
	 * @return A ISMPart instance corresponding to the newly created form part.
	 */
	public ISMPart newPart(int type, int height);

	/**
	 * Creates a new Title Header part on the form.
	 * 
	 * @sample
	 * var titleHeader = form.newTitleHeaderPart(40);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Title Header form part.
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMPart newTitleHeaderPart(int height);


	/**
	 * Creates a new Header part on the form.
	 * 
	 * @sample
	 * var header = form.newHeaderPart(80);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Header form part.
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMPart newHeaderPart(int height);

	/**
	 * Creates a new Leading Grand Summary part on the form.
	 * 
	 * @sample
	 * var leadingGrandSummary = form.newLeadingGrandSummaryPart(120);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Leading Grand Summary form part.
	 */
	public ISMPart newLeadingGrandSummaryPart(int height);

	/**
	 * Creates a new Leading Subsummary part on the form.
	 * 
	 * @sample
	 * var leadingSubsummary = form.newLeadingSubSummaryPart(160);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Leading Subsummary form part.
	 */
	public ISMPart newLeadingSubSummaryPart(int height);

	/**
	 * Creates a new Trailing Subsummary part on the form.
	 * 
	 * @sample
	 * var trailingSubsummary = form.newTrailingSubSummaryPart(360);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Trailing Subsummary form part.
	 */
	public ISMPart newTrailingSubSummaryPart(int height);

	/**
	 * Creates a new Trailing Grand Summary part on the form.
	 * 
	 * @sample
	 * var trailingGrandSummary = form.newTrailingGrandSummaryPart(400);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Trailing Grand Summary form part.
	 */
	public ISMPart newTrailingGrandSummaryPart(int height);

	/**
	 * Creates a new Footer part on the form.
	 *
	 * @sample 
	 * var footer = form.newFooterPart(440);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Footer form part.
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMPart newFooterPart(int height);

	/**
	 * Creates a new Title Footer part on the form.
	 * 
	 * @sample
	 * var titleFooter = form.newTitleFooterPart(500);
	 * 
	 * @return A ISMPart instance corresponding to the newly created Title Footer form part.
	 */
	public ISMPart newTitleFooterPart(int height);

	/**
	 * Gets all the parts from the form (optionally also from the parent form), ordered by there height (lowerbound) property, from top == 0 to bottom.
	 *
	 * @sample 
	 * var allParts = form.getParts()
	 * for (var i=0; i<allParts.length; i++) {
	 *	if (allParts[i].getPartType() == ISMPart.BODY)
	 *		application.output('body Y offset: ' + allParts[i].getPartYOffset());
	 * }
	 * 
	 * @param returnInheritedElements boolean true to also return the parts from parent form
	 * @return An array of ISMPart instances corresponding to the parts of the form.
	 */
	public ISMPart[] getParts(boolean returnInheritedElements);

	/**
	 * Gets all the parts from the form (not including the parts of the parent form), ordered by there height (lowerbound) property, from top == 0 to bottom.
	 *
	 * @sample 
	 * var allParts = form.getParts()
	 * for (var i=0; i<allParts.length; i++) {
	 *	if (allParts[i].getPartType() == ISMPart.BODY)
	 *		application.output('body Y offset: ' + allParts[i].getPartYOffset());
	 * }
	 * 
	 * @return An array of ISMPart instances corresponding to the parts of the form.
	 */
	public ISMPart[] getParts();


	/**
	 * Gets a part of the form from the given type (see ISMPart constants). 
	 *
	 * @sample 
	 * form.getPart(ISMPart.HEADER).background = 'red';
	 * form.getPart(ISMPart.LEADING_SUBSUMMARY, 160).background = 'red';
	 *
	 * @param type The type of the part to retrieve.
	 *
	 * @return A ISMPart instance representing the retrieved form part.
	 */
	public ISMPart getPart(int type);

	/**
	 * Gets a part of the form from the given type (see ISMPart constants). 
	 * Use the height if you want to get a specific LEADING_SUBSUMMARY or TRAILING_SUBSUMMARY.
	 *
	 * @sample 
	 * form.getPart(ISMPart.HEADER).background = 'red';
	 * form.getPart(ISMPart.LEADING_SUBSUMMARY, 160).background = 'red';
	 *
	 * @param type The type of the part to retrieve.
	 *
	 * @param height The height of the part to retrieve. Use this parameter when retrieving one of multiple
	 * 	                      Leading/Trailing Subsummary parts.
	 * 
	 * @return A ISMPart instance representing the retrieved form part.
	 */
	@ServoyClientSupport(mc = false, sc = true, wc = true)
	public ISMPart getPart(int type, int height);

	/**
	 * Returns the Y offset of a given part (see ISMPart) of the form. This will include 
	 * all the super forms parts if this form extends a form. 
	 *
	 * @sample
	 * // get the subform
	 * var form = solutionModel.getForm('SubForm');
	 * // get the start offset of the body
	 * var height = form.getPartYOffset(ISMPart.BODY);
	 * // place a new button based on the start offset.
	 * form.newButton('mybutton',50,50+height,80,20,solutionModel.getGlobalMethod('globals', 'test'));
	 *
	 * @param type The type of the part whose Y offset will be returned.
	 *
	 * @return A number holding the Y offset of the specified form part.
	 */
	public int getPartYOffset(int type);

	/**
	 * Returns the Y offset of a given part (see ISMPart) of the form. This will include 
	 * all the super forms parts if this form extends a form. Use the height parameter for 
	 * targetting one of multiple subsummary parts.
	 *
	 * @sample
	 * // get the subform
	 * var form = solutionModel.getForm('SubForm');
	 * // get the start offset of the body
	 * var height = form.getPartYOffset(ISMPart.BODY);
	 * // place a new button based on the start offset.
	 * form.newButton('mybutton',50,50+height,80,20,solutionModel.getGlobalMethod('globals', 'test'));
	 *
	 * @param type The type of the part whose Y offset will be returned.
	 *
	 * @param height The height of the part whose Y offset will be returned. This is used when
	 *                        one of multiple Leading/Trailing Sumsummary parts is retrieved.
	 *                        
	 * @return A number holding the Y offset of the specified form part.
	 */
	public int getPartYOffset(int type, int height);

	/**
	 * Removes a ISMPart of the given type. 
	 *
	 * @sample 
	 * form.removePart(ISMPart.HEADER);
	 * form.removePart(ISMPart.LEADING_SUBSUMMARY, 160);
	 *
	 * @param type The type of the part that should be removed.
	 *                        
	 * @return True if the part is successfully removed, false otherwise.
	 */
	public boolean removePart(int type);

	/**
	 * Removes a ISMPart of the given type. The height parameter is for removing one of multiple subsummary parts.
	 *
	 * @sample 
	 * form.removePart(ISMPart.HEADER);
	 * form.removePart(ISMPart.LEADING_SUBSUMMARY, 160);
	 *
	 * @param type The type of the part that should be removed.
	 *
	 * @param height The height of the part that should be removed. This parameter is for 
	 * 					removing one of multiple Leading/Trailing Subsummary parts.
	 *                        
	 * @return True if the part is successfully removed, false otherwise.
	 */
	public boolean removePart(int type, int height);

	/**
	 * Retrieves the Body part of the form.
	 *
	 * @sample 
	 * form.getBodyPart().background = 'blue';
	 * 
	 * @return A ISMPart instance corresponding to the Body part of the form.
	 */
	public ISMPart getBodyPart();

	/**
	 * Retrieves the Title Header part of the form.
	 * 
	 * @sample
	 * form.getTitleHeaderPart().background = 'red';
	 * 
	 * @return A ISMPart instance corresponding to the Title Header part of the form.
	 */
	public ISMPart getTitleHeaderPart();

	/**
	 * Retrieves the Header part of the form.
	 * 
	 * @sample
	 * form.getHeaderPart().background = 'orange';
	 * 
	 * @return A ISMPart instance corresponding to the Header part of the form.
	 */
	public ISMPart getHeaderPart();

	/**
	 * Retrieves the Leading Grand Summary part of the form.
	 * 
	 * @sample
	 * form.getLeadingGrandSummaryPart().background = 'yellow';
	 * 
	 * @return A ISMPart instance corresponding to the Leading Grand Summary part of the form.
	 */
	public ISMPart getLeadingGrandSummaryPart();

	/**
	 * Gets an array of the Leading Subsummary parts of the form, ordered by their height from top == 0 to bottom.
	 *
	 * @sample 
	 * form.getLeadingSubSummaryParts()[0].background = 'green';
	 * 
	 * @return An array of ISMPart instances corresponding to the Leading Subsummary parts of the form.
	 */
	public ISMPart[] getLeadingSubSummaryParts();

	/**
	 * Gets an array of the Trailing Subsummary parts of the form, ordered by their height from top == 0 to bottom.
	 *
	 * @sample 
	 * form.getTrailingSubSummaryParts()[0].background = 'green';
	 * 
	 * @return An array of ISMPart instances corresponding to the Trailing Subsummary parts of the form.
	 */
	public ISMPart[] getTrailingSubSummaryParts();

	/**
	 * Retrieves the Trailing Grand Summary part of the form.
	 * 
	 * @sample
	 * form.getTrailingGrandSummaryPart().background = 'yellow';
	 * 
	 * @return A ISMPart instance corresponding to the Trailing Grand Summary part of the form.
	 */
	public ISMPart getTrailingGrandSummaryPart();

	/**
	 * Retrieves the Footer part of the form.
	 * 
	 * @sample
	 * form.getFooterPart().background = 'magenta';
	 * 
	 * @return A ISMPart instance corresponding to the Footer part of the form.
	 */
	public ISMPart getFooterPart();

	/**
	 * Retrieves the Title Footer part of the form.
	 * 
	 * @sample
	 * form.getTitleFooterPart().background = 'gray';
	 * 
	 * @return A ISMPart instance corresponding to the Title Footer part of the form.
	 */
	public ISMPart getTitleFooterPart();

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
	 * @return a ISMField object
	 */
	public ISMField getField(String name);

	/**
	 * Returns all ISMField objects of this form, including the ones without a name.
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
	 * @param returnInheritedElements boolean true to also return the elements from the parent form 
	 * @return all ISMField objects of this form
	 *
	 */
	public ISMField[] getFields(boolean returnInheritedElements);

	/**
	 * Returns all ISMField objects of this form, including the ones without a name.
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
	 * @return all ISMField objects of this form
	 *
	 */
	public ISMField[] getFields();

	/**
	 * Returns a ISMButton that has the given name.
	 *
	 * @sample 
	 * var btn = myForm.getButton("hello");
	 * application.output(btn.text);
	 *
	 * @param name the specified name of the button
	 * 
	 * @return a ISMButton object 
	 */
	public ISMButton getButton(String name);

	/**
	 * Returns all ISMButtons of this form, including the ones without a name.
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
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return the list of all ISMButtons on this forms
	 *
	 */
	public ISMButton[] getButtons(boolean returnInheritedElements);

	/**
	 * Returns all ISMButtons of this form, including the ones without a name.
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
	 * @return the list of all ISMButtons on this forms
	 *
	 */
	public ISMButton[] getButtons();

	/**
	 * Creates a new ISMBean object on the form - including the name of the ISMBean object; the classname the ISMBean object is based on, the "x" and "y" position of the ISMBean object in pixels, as well as the width and height of the ISMBean object in pixels.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var bean = form.newBean('bean','com.servoy.extensions.beans.dbtreeview.DBTreeView',200,200,300,300);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param name the specified name of the ISMBean object
	 * @param className the class name of the ISMBean object
	 * @param x the horizontal "x" position of the ISMBean object in pixels
	 * @param y the vertical "y" position of the ISMBean object in pixels
	 * @param width the width of the ISMBean object in pixels
	 * @param height the height of the ISMBean object in pixels
	 * 
	 * @return a ISMBean object 
	 */
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public ISMBean newBean(String name, String className, int x, int y, int width, int height);

	/**
	 * Returns all ISMBeans of this form.
	 *
	 * @sample 
	 * var beans = myForm.getBeans();
	 * for (var b in beans)
	 * {
	 * 	if (beans[b].name != null) 
	 * 		application.output(beans[b].name);
	 * }
	 * 
	 * @param returnInheritedElements boolean true to also return the elements from parent form 
	 * @return the list of all ISMButtons on this forms
	 *
	 */
	public ISMBean[] getBeans(boolean returnInheritedElements);

	/**
	 * Returns all ISMBeans of this form. 
	 *
	 * @sample 
	 * var beans = myForm.getBeans();
	 * for (var b in beans)
	 * {
	 * 	if (beans[b].name != null) 
	 * 		application.output(beans[b].name);
	 * }
	 * 
	 * @return the list of all ISMButtons on this forms
	 *
	 */
	public ISMBean[] getBeans();

	/**
	 * Returns a ISMComponent that has the given name; if found it will be a ISMField, ISMLabel, ISMButton, ISMPortal, ISMBean or ISMTabPanel.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var cmp = frm.getComponent("componentName");
	 * application.output("Component type and name: " + cmp);
	 *
	 * @param name the specified name of the component 
	 * 
	 * @return a ISMComponent object (might be a ISMField, ISMLabel, ISMButton, ISMPortal, ISMBean or ISMTabPanel)
	 */
	public ISMComponent getComponent(String name);

	/**
	 * Returns a array of all the ISMComponents that a form has; they are of type ISMField,ISMLabel,ISMButton,ISMPortal,ISMBean or ISMTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]); 
	 * 
	 * @param returnInheritedElements boolean true to also return the elements from the parent form 
	 * @return an array of all the ISMComponents on the form.
	 */
	public ISMComponent[] getComponents(boolean returnInheritedElements);

	/**
	 * Returns a array of all the ISMComponents that a form has; they are of type ISMField,ISMLabel,ISMButton,ISMPortal,ISMBean or ISMTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]); 
	 * 
	 * @return an array of all the ISMComponents on the form.
	 */
	public ISMComponent[] getComponents();

	/**
	 * Returns a ISMLabel that has the given name.
	 *
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var label = frm.getLabel("myLabel");
	 * application.output(label.text); 
	 *
	 * @param name the specified name of the label
	 * 
	 * @return a ISMLabel object (or null if the label with the specified name does not exist)
	 */
	public ISMLabel getLabel(String name);

	/**
	 * Returns all ISMLabels of this form (optionally including it super forms labels), including the ones without a name.
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
	 * @param returnInheritedElements boolean true to also return the elements from parent form 
	 * @return all ISMLabels on this form
	 *
	 */
	public ISMLabel[] getLabels(boolean returnInheritedElements);

	/**
	 * Returns all ISMLabels of this form (not including its super form), including the ones without a name.
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
	 * @return all ISMLabels on this form
	 *
	 */
	public ISMLabel[] getLabels();

	/**
	 * @sameas com.servoy.j2db.solutionmodel.ISMComponent#getBorderType()
	 * @see com.servoy.j2db.solutionmodel.ISMComponent#getBorderType()
	 */
	public String getBorderType();

	/**
	 * The default page format for the form.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * application.output(form.defaultPageFormat);
	 * form.defaultPageFormat = solutionModel.createPageFormat(612,792,72,72,72,72,SM_ORIENTATION.PORTRAIT,SM_UNITS.PIXELS) 
	 */
	public String getDefaultPageFormat();

	/**
	 * A JSForm instance representing the super form of this form, if this form has a super form. 
	 * 
	 * @sample
	 * var subForm = solutionModel.newForm('childForm',myDatasource,null,true,800,600);
	 * var superForm = solutionModel.newForm('childForm',myDatasource,null,true,800,600);
	 * subForm.extendsForm = superForm;
	 * 
	 */
	public ISMForm getExtendsForm();

	/**
	 * The default sort order only when the form loads.
	 * This is applied each time an internal SQL query is being executed (find, find-all, open form); and is only executed when no other manual sort has been performed on the foundset.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * form.initialSort = "column1 desc, column2 asc, column3 asc";
	 * 
	 */
	public String getInitialSort();

	/**
	 * The navigator (previously named "controller")
	 * that is used to control/navigate to the form. The navigator is shown at
	 * the left or at the right side of the form, depending on the page orientation. 
	 * 
	 * The following options are available: 
	 * -none- - no navigator is assigned. 
	 * DEFAULT - the Servoy default navigator is assigned. 
	 * IGNORE - the navigator last assigned to a previous form. 
	 * Custom - a custom navigator based on a selected form.
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
	 * The percentage value the printed page is enlarged or reduced to; the size of the printed form 
	 * is inversely proportional. For example, if the paperPrintScale is 50, the printed form will be 
	 * enlarged 200%.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * if (form.paperPrintScale < 100)
	 * 	form.paperPrintScale = 100;
	 * 
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPaperPrintScale();

	/**
	 * @see com.servoy.j2db.solutionmodel.ISMField#getScrollbars()
	 *
	 * @sample 
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,1000,600);
	 * form.scrollbars = SM_SCROLLBAR.VERTICAL_SCROLLBAR_NEVER; 
	 * forms['newForm1'].controller.show();
	 */
	public int getScrollbars();

	/**
	 * When set, the form is displayed under the Window menu. 
	 * If it is not set, the form will be 'hidden'. 
	 * NOTE: This is only applicable for Servoy Client. Servoy Developer always shows all forms so that
	 * developers have access to all forms within a solution during development.
	 * 
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var anotherForm= solutionModel.newForm('newForm2', myDatasource, null, true, 800, 600);
	 * //using 'anotherForm' as navigator for aForm
	 * anotherForm.showInMenu = false;
	 * anotherForm.navigator = null;
	 * aForm.navigator = anotherForm;
	 * application.output(aForm.navigator.name);
	 * 
	 */
	public boolean getShowInMenu();

	/**
	 * The Cascading Style Sheet (CSS) class name applied to the form.
	 * 
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * if (aForm.styleClass == null)
	 * 	aForm.styleClass = someStyleClass;
	 * else
	 * 	application.output("The Cascading Style Sheet (CSS) class name applied to this form is " + aForm.styleClass); 
	 */
	public String getStyleClass();

	/**
	 * The name of the Servoy style that is being used on the form.
	 *
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * if (aForm.styleName == null)
	 * 	aForm.styleName = someServoyStyleName;
	 * else
	 * 	application.output("The name of the Servoy style that is being used on the form is " + aForm.styleName); 
	 * 
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String getStyleName();

	/**
	 * The text that displays in the title bar of the form window. 
	 * NOTE: Data tags and Servoy tags can be used as part of the title text.
	 * 
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/a_server/a_table', 'aStyleName', false, 800, 600)
	 * forms['newForm'].controller.show();
	 * if (myForm.titleText == null)
	 * {
	 * 	myForm.titleText = "My new title text should be really cool!"
	 * 	forms['newForm'].controller.recreateUI();
	 * }
	 * else
	 * 	application.output("My text text is already cool");
	 * 
	 */
	public String getTitleText();

	/**
	 * When set, the form is transparent.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,1000,800);
	 * if (form.transparent == false)
	 * {
	 * 	var style = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * 	style.text = style.text + 'field { background-color: blue; }';
	 * 	form.styleName = 'myStyle';
	 * }
	 * var field = form.newField('columnTextDataProvider',ISMField.TEXT_FIELD,100,100,100,50);
	 * forms['myForm'].controller.show(); 
	 */
	public boolean getTransparent();

	/**
	 * Returns the value of the form's selectionMode property.
	 * Selection mode is applied when necessary to the foundset used by the form (through it's multiSelect property), even if the foundset changes.
	 * If two or more forms with non-default and different selectionMode values share the same foundset, the visible one decides.
	 * If two or more non-visible forms with non-default and different selectionMode values share the same foundset, one of them (always the same from a set of forms) decides.
	 * If two or more visible forms with non-default and different selectionMode values share the same foundset, one of them (always the same from a set of forms) decides what the
	 * foundset's selectionMode should be.
	 * 
	 * Can be one of SELECTION_MODE_DEFAULT, SELECTION_MODE_SINGLE or SELECTION_MODE_MULTI.
	 * 
	 * @since 6.1
	 * @sample
	 * var myForm = solutionModel.getForm('my_form_name');
	 * if (myForm.selectionMode == JSForm.SELECTION_MODE_MULTI) myForm.selectionMode = JSForm.SELECTION_MODE_DEFAULT;
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getSelectionMode();

	/**
	 * Property that tells the form to use a named foundset instead of the default foundset.
	 * When JSForm.SEPARATE_FOUNDSET is specified the form will always create a copy of assigned foundset and therefore become separated from other foundsets.
	 * When JSForm.EMPTY_FOUNDSET, the form will have an initially empty foundset.
	 * 
	 * The namedFoundset can be based on a global relation; in this case namedFoundset is the relation's name.
	 * You can also set the namedFoundset to a JSRelation object directly.
	 * It will tell this form to initially load a global relation based foundset.
	 * The global relation's foreign datasource must match the form's datasource.
	 * Do not use relations named "empty" or "separate" to avoid confusions.
	 * 
	 * @sample
	 * // form with separate foundset
	 * var frmSeparate = solutionModel.newForm('products_separate', 'db:/example_data/products', null, true, 640, 480);
	 * frmSeparate.newLabel("Separate FoundSet",10,10,200,20);
	 * frmSeparate.newField('categoryid',ISMField.TEXT_FIELD,10,40,200,20);
	 * frmSeparate.newField('productname',ISMField.TEXT_FIELD,10,70,200,20);
	 * frmSeparate.namedFoundSet = JSForm.SEPARATE_FOUNDSET;
	 * forms['products_separate'].controller.find();
	 * forms['products_separate'].categoryid = '=2';
	 * forms['products_separate'].controller.search();
	 *
	 * // form with empty foundset
	 * var frmEmpty = solutionModel.newForm('products_empty', 'db:/example_data/products', null, true, 640, 480);
	 * frmEmpty.newLabel("Empty FoundSet",10,10,200,20);
	 * frmEmpty.newField('categoryid',ISMField.TEXT_FIELD,10,40,200,20);
	 * frmEmpty.newField('productname',ISMField.TEXT_FIELD,10,70,200,20);
	 * frmEmpty.namedFoundSet = JSForm.EMPTY_FOUNDSET;
	 *
	 * // form with an initial foundset based on a global relation
	 * var frmGlobalRel = solutionModel.newForm("categories_related", solutionModel.getForm("categories"));
	 * frmGlobalRel.namedFoundSet = "g2_to_category_name";
	 *  
	 * // form with an initial foundset based on a global relation
	 * var frmGlobalRel = solutionModel.newForm("categories_related", solutionModel.getForm("categories"));
	 * frmGlobalRel.namedFoundSet = solutionModel.getRelation("g1_to_categories");
	 */
	public String getNamedFoundSet();

	public void setBorderType(String b);

	public void setDefaultPageFormat(String string);

	public void setExtendsForm(Object superForm);

	public void setInitialSort(String arg);

	public void setNavigator(Object navigator);

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPaperPrintScale(int arg);

	public void setScrollbars(int i);

	public void setShowInMenu(boolean arg);

	public void setStyleClass(String arg);

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setStyleName(String arg);

	public void setTitleText(String string);

	public void setTransparent(boolean arg);

	/**
	 * @since 6.1
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setSelectionMode(int arg);

	public void setNamedFoundSet(Object arg);

	/**
	 * The method that overrides the Servoy menu item Select > Delete All. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @sampleas getOnNewRecordCmd()
	 * @see #getOnNewRecordCmd()
	 */
	public ISMMethod getOnDeleteAllRecordsCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Delete Record (or keyboard shortcut). 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnNewRecordCmd()
	 * @see #getOnNewRecordCmd()
	 */
	public ISMMethod getOnDeleteRecordCmd();

	/**
	 * The method that is triggered when (non Design Mode) dragging occurs.
	 * 
	 * @sample
	 * form.onDrag = form.newMethod('function onDrag(event) { application.output("onDrag intercepted from " + event.getSource()); }');
	 * form.onDragEnd = form.newMethod('function onDragEnd(event) { application.output("onDragEnd intercepted from " + event.getSource()); }');
	 * form.onDragOver = form.newMethod('function onDragOver(event) { application.output("onDragOver intercepted from " + event.getSource()); }');
	 * form.onDrop = form.newMethod('function onDrop(event) { application.output("onDrop intercepted from " + event.getSource()); }');
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public ISMMethod getOnDrag();

	/**
	 * The method that is triggered when (non Design Mode) dragging end occurs.
	 * 
	 * @sampleas getOnDrag()
	 * @see #getOnDrag()
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public ISMMethod getOnDragEnd();

	/**
	 * The method that is triggered when (non Design Mode) dragging over a component occurs.
	 * 
	 * @sampleas getOnDrag()
	 * @see #getOnDrag()
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public ISMMethod getOnDragOver();

	/**
	 * The method that is triggered when (non Design Mode) dropping occurs.
	 * 
	 * @sampleas getOnDrag()
	 * @see #getOnDrag()
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public ISMMethod getOnDrop();

	/**
	 * The method that is triggered when focus is gained by a component inside the form.
	 * 
	 * @sample
	 * form.onElementFocusGained = form.newMethod('function onElementFocusGained(event) { application.output("onElementFocusGained intercepted from " + event.getSource()); }');
	 * form.onElementFocusLost = form.newMethod('function onElementFocusLost(event) { application.output("onElementFocusLost intercepted from " + event.getSource()); }');
	 */
	public ISMMethod getOnElementFocusGained();

	/**
	 * The method that gets triggered when focus is lost by a component inside the form.
	 * 
	 * @sampleas getOnElementFocusGained()
	 * @see #getOnElementFocusGained()
	 */
	public ISMMethod getOnElementFocusLost();

	/**
	 * The method that overrides the Servoy menu item Select > Duplicate Record (or keyboard shortcut).
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnNewRecordCmd()
	 * @see #getOnNewRecordCmd()
	 */
	public ISMMethod getOnDuplicateRecordCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Find (or keyboard shortcut) in Data (ready) mode. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sample
	 * form.onFindCmd = form.newMethod('function onFindCmd(event) { application.output("onFindCmd intercepted on " + event.getFormName()); }');
	 * form.onSearchCmd = form.newMethod('function onSearchCmd(event) { application.output("onSearchCmd intercepted on " + event.getFormName()); }');
	 * form.onShowAllRecordsCmd = form.newMethod('function onShowAllRecordsCmd(event) { application.output("onShowAllRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnFindCmd();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMForm#getOnHide()
	 * @see com.servoy.base.solutionmodel.IBaseSMForm#getOnHide()
	 *
	 * @sampleas getOnShow()
	 * @see #getOnShow()
	 */
	public ISMMethod getOnHide();

	/**
	 * The method that overrides the Servoy menu item Select > Invert Records. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnOmitRecordCmd()
	 * @see #getOnOmitRecordCmd()
	 */
	public ISMMethod getOnInvertRecordsCmd();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMForm#getOnLoad()
	 * @see com.servoy.base.solutionmodel.IBaseSMForm#getOnLoad()
	 * 
	 * @sample
	 * form.onLoad = form.newMethod('function onLoad(event) { application.output("onLoad intercepted on " + event.getFormName()); }');
	 * form.onUnLoad = form.newMethod('function onUnLoad(event) { application.output("onUnLoad intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnLoad();

	/**
	 * The method that overrides the Servoy menu item Select > New Record (or keyboard shortcut). 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sample
	 * form.onNewRecordCmd = form.newMethod('function onNewRecordCmd(event) { application.output("onNewRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDuplicateRecordCmd = form.newMethod('function onDuplicateRecordCmd(event) { application.output("onDuplicateRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDeleteRecordCmd = form.newMethod('function onDeleteRecordCmd(event) { application.output("onDeleteRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDeleteAllRecordsCmd = form.newMethod('function onDeleteAllRecordsCmd(event) { application.output("onDeleteAllRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnNewRecordCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Next Record. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnPreviousRecordCmd()
	 * @see #getOnPreviousRecordCmd()
	 */
	public ISMMethod getOnNextRecordCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Omit Record. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sample
	 * form.onOmitRecordCmd = form.newMethod('function onOmitRecordCmd(event) { application.output("onOmitRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onShowOmittedRecordsCmd = form.newMethod('function onShowOmittedRecordsCmd(event) { application.output("onShowOmittedRecordsCmd intercepted on " + event.getFormName()); }');
	 * form.onInvertRecordsCmd = form.newMethod('function onInvertRecordsCmd(event) { application.output("onInvertRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnOmitRecordCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Previous Record. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sample
	 * form.onPreviousRecordCmd = form.newMethod('function onPreviousRecordCmd(event) { application.output("onPreviousRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onNextRecordCmd = form.newMethod('function onNextRecordCmd(event) { application.output("onNextRecordCmd intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnPreviousRecordCmd();

	/**
	 * The method that overrides the Servoy menu item File > Print Preview. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sample
	 * form.onPrintPreviewCmd = form.newMethod('function onPrintPreviewCmd(event) { application.output("onPrintPreviewCmd intercepted on " + event.getFormName()); }');
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public ISMMethod getOnPrintPreviewCmd();

	/**
	 * The method that is triggered when a user clicks into a column on the form.
	 * NOTE: There is a small "e" displayed in the lower left side of the Servoy Client screen in the status area at the bottom of the window when the record is being edited.
	 * 
	 * @sample
	 * form.onRecordEditStart = form.newMethod('function onRecordEditStart(event) { application.output("onRecordEditStart intercepted on " + event.getFormName()); }');
	 * form.onRecordEditStop = form.newMethod('function onRecordEditStop(record, event) { application.output("onRecordEditStop intercepted on " + event.getFormName() + ". record is: " + record); }');
	 * form.onRecordSelection = form.newMethod('function onRecordSelection(event) { application.output("onRecordSelection intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnRecordEditStart();

	/**
	 * The method that is triggered when a record is being saved. 
	 * A record is saved when a user clicks out of it (for example on an empty part of the layout or to another form).
	 * When the method returns false (for example as part of a validation), the user cannot leave the record, for example in
	 * a table view a user cannot move to another record when the callback returns false.
	 * 
	 * @sampleas getOnRecordEditStart()
	 * @see #getOnRecordEditStart()
	 */
	public ISMMethod getOnRecordEditStop();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMForm#getOnRecordSelection()
	 * @see com.servoy.base.solutionmodel.IBaseSMForm#getOnRecordSelection()
	 * 
	 * @sampleas getOnRecordEditStart()
	 * @see #getOnRecordEditStart()
	 */
	public ISMMethod getOnRecordSelection();

	/**
	 * The method that overrides the Servoy menu item Select > Search (or keyboard shortcut) in Find mode. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnFindCmd()
	 * @see #getOnFindCmd()
	 */
	public ISMMethod getOnSearchCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Show All (or keyboard shortcut). 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnFindCmd()
	 * @see #getOnFindCmd()
	 */
	public ISMMethod getOnShowAllRecordsCmd();

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMForm#getOnShow()
	 * @see com.servoy.base.solutionmodel.IBaseSMForm#getOnShow()
	 * 
	 * @sample
	 * form.onShow = form.newMethod('function onShow(firstShow, event) { application.output("onShow intercepted on " + event.getFormName() + ". first show? " + firstShow); return false; }');
	 * form.onHide = form.newMethod('function onHide(event) { application.output("onHide blocked on " + event.getFormName()); return false; }');
	 */
	public ISMMethod getOnShow();

	/**
	 * The method that overrides the Servoy menu item Select > Show Omitted Records. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sampleas getOnOmitRecordCmd()
	 * @see #getOnOmitRecordCmd()
	 */
	public ISMMethod getOnShowOmittedRecordsCmd();

	/**
	 * The method that overrides the Servoy menu item Select > Sort. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @sample
	 * form.onSortCmd = form.newMethod('function onSortCmd(dataProviderID, asc, event) { application.output("onSortCmd intercepted on " + event.getFormName() + ". data provider: " + dataProviderID + ". asc: " + asc); }');
	 */
	public ISMMethod getOnSortCmd();

	/**
	 * The method that is triggered when a form is unloaded from the repository. 
	 * NOTE: Forms can be prevented from being removed from memory by referencing the form object in a global variable or inside an array inside a global variable. Do take care using this technique.
	 * Forms take up memory and if too many forms are in memory and cannot be unloaded, there is a possibility of running out of memory.
	 * 
	 * @sampleas getOnLoad()
	 * @see #getOnLoad()
	 */
	public ISMMethod getOnUnLoad();

	/**
	 * The method that gets triggered when resize occurs.
	 * 
	 * @sample
	 * form.onResize = form.newMethod('function onResize(event) { application.output("onResize intercepted on " + event.getFormName()); }');
	 */
	public ISMMethod getOnResize();

	/**
	 * The method that is executed when the component is rendered.
	 * 
	 * @sample
	 * form.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public ISMMethod getOnRender();

	public void setOnDeleteAllRecordsCmd(ISMMethod method);

	public void setOnDeleteRecordCmd(ISMMethod method);

	public void setOnDrag(ISMMethod method);

	public void setOnDragEnd(ISMMethod method);

	public void setOnDragOver(ISMMethod method);

	public void setOnDrop(ISMMethod method);

	public void setOnDuplicateRecordCmd(ISMMethod method);

	public void setOnFindCmd(ISMMethod method);

	public void setOnInvertRecordsCmd(ISMMethod method);

	public void setOnNewRecordCmd(ISMMethod method);

	public void setOnNextRecordCmd(ISMMethod method);

	public void setOnOmitRecordCmd(ISMMethod method);

	public void setOnPreviousRecordCmd(ISMMethod method);

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnPrintPreviewCmd(ISMMethod method);

	public void setOnSearchCmd(ISMMethod method);

	public void setOnShowAllRecordsCmd(ISMMethod method);

	public void setOnShowOmittedRecordsCmd(ISMMethod method);

	public void setOnSortCmd(ISMMethod method);

	public void setOnRender(ISMMethod method);

	public void setOnElementFocusGained(IBaseSMMethod method);

	public void setOnElementFocusLost(IBaseSMMethod method);

	public void setOnHide(IBaseSMMethod method);

	public void setOnLoad(IBaseSMMethod method);

	public void setOnRecordEditStart(IBaseSMMethod method);

	public void setOnRecordEditStop(IBaseSMMethod method);

	public void setOnRecordSelection(IBaseSMMethod method);

	public void setOnShow(IBaseSMMethod method);

	public void setOnUnLoad(IBaseSMMethod method);

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnResize(IBaseSMMethod method);

	/**
	 * Get or set the encapsulation level for the form.
	 * 
	 * Encapsulation is one of constants JSForm.DEFAULT_ENCAPSULATION, JSForm.PRIVATE_ENCAPSULATION, JSForm.MODULE_PRIVATE_ENCAPSULATION,
	 * JSForm.HIDE_DATAPROVIDERS_ENCAPSULATION, JSForm.HIDE_FOUNDSET_ENCAPSULATION, JSForm.HIDE_CONTROLLER_ENCAPSULATION or JSForm.HIDE_ELEMENTS_ENCAPSULATION
	 *
	 * @sample 
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * myForm.encapsulation = JSForm.HIDE_CONTROLLER_ENCAPSULATION;
	 * @deprecated really only useful in developer - when working with the solution. Not so much at runtime, maybe just
	 * if developer save form solution model api is used...
	 */
	@Deprecated
	public int getEncapsulation();

	@Deprecated
	public void setEncapsulation(int arg);

	/**
	 * The width of the form in pixels.
	 * 
	 * @sample
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * forms['newForm1'].controller.show();
	 * myForm.width = 120;
	 * forms['newForm1'].controller.recreateUI();
	 */
	public int getWidth();

	public void setWidth(int width);

	/**
	 * Returns true if this form is in responsive mode
	 *
	 * @sample
	 * var myForm = solutionModel.getForm('myform');
	 * if (myForm.isResponsive()) {}
	 */
	public boolean isResponsiveLayout();
}
