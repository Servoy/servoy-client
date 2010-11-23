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
package com.servoy.j2db.scripting.solutionmodel;

import java.awt.Dimension;
import java.awt.Point;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.Function;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSForm implements IJSParent, IConstantsObject
{
	public static ScriptMethod getScriptMethod(Function function, FlattenedSolution fs)
	{
		String name = (String)function.get("_methodname_", function); //$NON-NLS-1$
		ScriptMethod scriptMethod = fs.getScriptMethod(name);
		return scriptMethod;
	}

	/**
	 * @sameas LIST_VIEW
	 */
	public static final int RECORD_VIEW = IForm.RECORD_VIEW;

	/**
	 * The constants to set or get the view property of a JSForm. 
	 * They are as follows: JSForm.LIST_VIEW, JSForm.LOCKED_LIST_VIEW, JSForm.LOCKED_RECORD_VIEW, JSForm.LOCKED_TABLE_VIEW, JSForm.RECORD_VIEW.
	 *
	 * @sample 
	 * 	var myListViewForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', 'myStyleName', false, 800, 600);
	 * 	myListViewForm.view = JSForm.LIST_VIEW;
	 * 
	 * 	var myLockedListViewForm = solutionModel.newForm('newForm2', 'myServer', 'myTable', 'myStyleName', false, 800, 600);	
	 * 	myLockedListViewForm.view = JSForm.LOCKED_LIST_VIEW;
	 * 	
	 * 	var myLockedRecordViewForm = solutionModel.newForm('newForm3', 'myServer', 'myTable', 'myStyleName', false, 800, 600);
	 * 	myLockedRecordViewForm.view = JSForm.LOCKED_RECORD_VIEW;
	 * 
	 * 	var myLockedTableViewForm = solutionModel.newForm('newForm4', 'myServer', 'myTable', 'myStyleName', false, 800, 600);
	 * 	myLockedTableViewForm.view = JSForm.LOCKED_TABLE_VIEW;
	 * 	
	 * 	var myRecordViewForm = solutionModel.newForm('newForm5', 'myServer', 'myTable', 'myStyleName', false, 800, 600);
	 * 	myRecordViewForm.view = JSForm.RECORD_VIEW;
	 */
	public static final int LIST_VIEW = IForm.LIST_VIEW;

	//	public static final int TABLE_VIEW = FormController.TABLE_VIEW;

	/**
	 * @sameas LIST_VIEW
	 */
	public static final int LOCKED_TABLE_VIEW = FormController.LOCKED_TABLE_VIEW;

	/**
	 * @sameas LIST_VIEW
	 */
	public static final int LOCKED_LIST_VIEW = FormController.LOCKED_LIST_VIEW;

	/**
	 * @sameas LIST_VIEW
	 */
	public static final int LOCKED_RECORD_VIEW = IForm.LOCKED_RECORD_VIEW;

	private Form form;
	private final IApplication application;
	private boolean isCopy;

	public JSForm(IApplication application, Form form, boolean isNew)
	{
		this.application = application;
		this.form = form;
		this.isCopy = isNew;
	}

	public final void checkModification()
	{
		// make copy if needed
		if (!isCopy)
		{
			form = application.getFlattenedSolution().createPersistCopy(form);
			((FormManager)application.getFormManager()).addForm(form, false);

			isCopy = true;
		}
		form.setLastModified(System.currentTimeMillis());

		application.getFlattenedSolution().registerChangedForm(form);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getJSParent()
	 */
	public IJSParent getJSParent()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getBaseComponent()
	 */
	public ISupportChilds getSupportChild()
	{
		return form;
	}

	/**
	 * Creates a new form JSVariable - based on the name of the variable object and the number type, uses the SolutionModel JSVariable constants.
	 *
	 * @sample 
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var variable = form.newFormVariable('myVar', JSVariable.TEXT);
	 * 	variable.defaultValue = "'This is a default value (with tripple quotes)!'";
	 * 	var field = form.newField(variable, JSField.TEXT_FIELD, 100, 100, 200, 200);
	 * 	forms['newForm1'].controller.show();
	 *
	 * @param name the specified name of the variable
	 *
	 * @param type the specified type of the variable (see Solution Model -> JSVariable node constants)
	 * 
	 * @return a JSVariable object
	 */
	public JSVariable js_newFormVariable(String name, int type)
	{
		checkModification();
		try
		{
			ScriptVariable variable = form.createNewScriptVariable(new ScriptNameValidator(application.getFlattenedSolution()), name, type);
			return new JSVariable(application, this, variable, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an existing form variable for the given name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var fvariable = frm.getFormVariable("myVarName");
	 * 	application.output(fvariable.name + " has the default value of " + fvariable.defaultValue);
	 * 
	 * @param name the specified name of the variable
	 * 
	 * @return a JSVariable object
	 */
	public JSVariable js_getFormVariable(String name)
	{
		ScriptVariable variable = null;
		try
		{
			variable = application.getFlattenedSolution().getFlattenedForm(form).getScriptVariable(name);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		if (variable != null)
		{
			return new JSVariable(application, this, variable, false);
		}
		return null;
	}

	/**
	 * An array consisting of all form variables for this form.
	 * 
	 * @sample
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var variables = frm.getFormVariables();
	 * 	for (var i in variables)
	 * 		application.output(variables[i].name);
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return an array of all variables on this form
	 * 
	 */
	public JSVariable[] js_getFormVariables(Object[] args)
	{
		ArrayList<JSVariable> variables = new ArrayList<JSVariable>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<ScriptVariable> scriptVariables = application.getFlattenedSolution().getFlattenedForm(form).getScriptVariables(true);
				while (scriptVariables.hasNext())
				{
					variables.add(new JSVariable(application, this, scriptVariables.next(), false));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<ScriptVariable> scriptVariables = form.getScriptVariables(true);
			while (scriptVariables.hasNext())
			{
				variables.add(new JSVariable(application, this, scriptVariables.next(), false));
			}
		}
		return variables.toArray(new JSVariable[variables.size()]);
	}

	/**
	 * Creates a new form JSMethod - based on the specified code. 
	 *
	 * @sample 
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var method = form.newFormMethod('function aMethod(event){application.output("Hello world!");}');
	 * 	var button = myListViewForm.newButton('Show message!',50,50,100,30,method);
	 *  forms['newForm1'].controller.show();
	 *
	 * @param code the specified code for the new method
	 * 
	 * @return a new JSMethod object for this form
	 */
	public JSMethod js_newFormMethod(String code)
	{
		checkModification();
		String name = JSMethod.parseName(code);

		try
		{
			ScriptMethod method = form.createNewScriptMethod(new ScriptNameValidator(application.getFlattenedSolution()), name);
			method.setDeclaration(code);
			return new JSMethod(application, this, method, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an existing form method for the given name.
	 *
	 * @sample
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var method = frm.getFormMethod("myMethod");
	 * 	application.output(method.code);
	 * 
	 * @param name the specified name of the method
	 * 
	 * @return a JSMethod object (or null if the method with the specified name does not exist)
	 */
	public JSMethod js_getFormMethod(String name)
	{
		ScriptMethod sm = null;
		try
		{
			sm = application.getFlattenedSolution().getFlattenedForm(form).getScriptMethod(name);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		if (sm != null)
		{
			return new JSMethod(application, this, sm, false);
		}
		return null;
	}

	/**
	 * Returns all existing form methods for this form.
	 * 
	 * @sample
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var methods = frm.getFormMethods();
	 * 	for (var m in methods)
	 * 		application.output(methods[m].getName());
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return all form methods for the form 
	 */
	public JSMethod[] js_getFormMethods(Object[] args)
	{
		ArrayList<JSMethod> methods = new ArrayList<JSMethod>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<ScriptMethod> scriptMethods = application.getFlattenedSolution().getFlattenedForm(form).getScriptMethods(true);
				while (scriptMethods.hasNext())
				{
					methods.add(new JSMethod(application, this, scriptMethods.next(), false));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<ScriptMethod> scriptMethods = form.getScriptMethods(true);
			while (scriptMethods.hasNext())
			{
				methods.add(new JSMethod(application, this, scriptMethods.next(), false));
			}
		}
		return methods.toArray(new JSMethod[methods.size()]);
	}


	/**
	 * Creates a new JSField object on the form - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample 
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var variable = form.newFormVariable('myVar', JSVariable.TEXT);
	 * 	variable.defaultValue = "'This is a default value (with tripple quotes)!'";
	 * 	var field = form.newField(variable, JSField.TEXT_FIELD, 100, 100, 200, 200);
	 * 	forms['newForm1'].controller.show();  	
	 *
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param displaytype the display type of the JSField object (see the Solution Model -> JSField node for display types)
	 *
	 * @param x the horizontal "x" position of the JSField object in pixels
	 *
	 * @param y the vertical "y" position of the JSField object in pixels
	 *
	 * @param width the width of the JSField object in pixels
	 *
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a new JSField object (of the specified display type) 
	 */
	public JSField js_newField(Object dataprovider, int type, int x, int y, int width, int height)
	{
		checkModification();
		try
		{
			Field field = form.createNewField(new Point(x, y));
			field.setDisplayType(type);
			field.setSize(new Dimension(width, height));
			if (dataprovider instanceof String)
			{
				field.setDataProviderID((String)dataprovider);
			}
			else if (dataprovider instanceof JSVariable)
			{
				field.setDataProviderID(((JSVariable)dataprovider).getScriptVariable().getDataProviderID());
			}
			return new JSField(this, field, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TEXT_FIELD - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample 
	 *	var form = solutionModel.newForm('newForm1','myServer','myTable',null,true,800,600); 
	 *	//choose the dataprovider or jsvariable you want for the Text Field
	 * 	var x = null;
	 * 	//global jsvariable as the dataprovider 
	 *  //x = solutionModel.newGlobalVariable('myGlobal',JSVariable.TEXT);
	 * 	//x.defaultValue = "'Text from a global variable'";
	 * 	//or a form jsvariable as the dataprovider 
	 * 	//x = form.newFormVariable('myFormVar',JSVariable.TEXT);
	 * 	//x.defaultValue = "'Text from a form variable'";
	 * 	var textField = form.newTextField(x,100,100,200,50);
	 * 	//or a column data provider as the dataprovider
	 * 	//textField.dataProviderID = columnTextDataProvider;
	 * 	forms['newForm1'].controller.show();
	 *
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a JSField object with the displayType of TEXT_FIELD
	 */
	public JSField js_newTextField(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.TEXT_FIELD, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TEXT_AREA - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1','myServer','myTable',null,true,800,600);
	 * 	var globalVar = solutionModel.newGlobalVariable('myGlobal',JSVariable.TEXT);
	 * 	globalVar.defaultValue = "'Type your text in here'";
	 * 	var textArea = form.newTextArea(globalVar,100,100,300,150);
	 * 	forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSTabPanel object in pixels
	 * @param y the vertical "y" position of the JSTabPanel object in pixels
	 * @param width the width of the JSTabPanel object in pixels
	 * @param height the height of the JSTabPanel object in pixels
	 * 
	 * @return a JSField object with the displayType of TEXT_AREA
	 */
	public JSField js_newTextArea(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.TEXT_AREA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of COMBOBOX - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *  
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var calendar = form.newComboBox(myDataProvider, 100, 100, 200, 200);
	 * 	forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a new JSField object on the form with the displayType of COMBOBOX
	 */
	public JSField js_newComboBox(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.COMBOBOX, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of RADIOS (radio buttons) - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample	
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES); 
	 * 	vlist.customValues = "value1\nvalue2\nvalue3"; 
	 * 	var radios = form.newRadios('columnDataProvider',100,100,200,200);
	 * 	radios.valuelist = vlist;
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a JSField object with the displayType of RADIOS (radio buttons)
	 */
	public JSField js_newRadios(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.RADIOS, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of CHECK (checkbox) - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample 
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var calendar = form.newCheck(myDataProvider, 100, 100, 200, 200);
	 * 	forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a new JSField object on the form with the displayType of CHECK (checkbox)
	 */
	public JSField js_newCheck(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.CHECKS, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of CALENDAR - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *  
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var calendar = form.newCalendar(myDataProvider, 100, 100, 200, 200);
	 * 	forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels 
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a new JSField object on the form with the displayType of CALENDAR
	 */
	public JSField js_newCalendar(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.CALENDAR, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of RTF_AREA (enables more than one line of text to be displayed in a field) - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1','myServer','myTable',null,true,800,600);
	 * 	var rtf_area = form.newRtfArea('columnDataProvider',100,100,100,100);
	 *  forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a JSField object with the displayType of RTF_AREA
	 */
	public JSField js_newRtfArea(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.RTF_AREA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of HTML_AREA - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var textProvider = form.newFormVariable('myVar',JSVariable.TEXT);
	 * 	textProvider.defaultValue = "'This is a tripple quotted text!'";
	 * 	var htmlArea = myListViewForm.newHtmlArea(textProvider,100,100,100,100);
	 * 	forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a JSField object on the form with the displayType of HTML_AREA 
	 */
	public JSField js_newHtmlArea(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.HTML_AREA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of IMAGE_MEDIA - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample
	 *  var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 *  var myMediaVar = form.newFormVariable("media", JSVariable.MEDIA);
	 *  var imageMedia = form.newImageMedia(myMediaVar,100,100,200,200)
	 *  forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a new JSField object on the form with the displayType of IMAGE_MEDIA
	 */
	public JSField js_newImageMedia(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.IMAGE_MEDIA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TYPE_AHEAD - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1','myServer','myTable',null,true,800,600);
	 * 	var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES);
	 * 	vlist.customValues = "value1\nvalue2\nvalue3";
	 * 	var typeAhead = form.newTypeAhead(columnTextDataProvider,100,100,300,200);
	 * 	typeAhead.valuelist = vlist;
	 * 	forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a JSField object with the displayType of TYPE_AHEAD
	 */
	public JSField js_newTypeAhead(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.TYPE_AHEAD, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of PASSWORD - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 *  var pass = form.newPassword(globals.aVariable, 100, 100, 70, 30);
	 *  forms['newForm1'].controller.show();
	 * 
	 * @param dataprovidername/jsvariable the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 * 
	 * @return a new JSField object on the form with the displayType of PASSWORD
	 */
	public JSField js_newPassword(Object dataprovider, int x, int y, int width, int height)
	{
		return js_newField(dataprovider, Field.PASSWORD, x, y, width, height);
	}

	/**
	 * Creates a new button on the form with the given text, place, size and JSMethod as the onAction event triggered action.
	 *
	 * @sample 
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var method = solutionModel.newFormMethod('function onAction(event) { application.output("onAction intercepted on " + event.getFormName()); }');
	 * 	var button = solutionModel.newButton('myButton', 10, 10, 100, 30, method);
	 * 	application.output("The new button: " + button.name + " has the following onAction event handling method assigned " + button.onAction.getName());
	 *
	 * @param text the text on the button
	 *
	 * @param x the x coordinate of the button location on the form
	 *
	 * @param y the y coordinate of the button location on the form
	 *
	 * @param width the width of the button
	 * 
	 * @param height the height of the button 
	 *
	 * @param jsmethod the method assigned to handle an onAction event
	 * 
	 * @return a new JSButton object
	 */
	public JSButton js_newButton(String txt, int x, int y, int width, int height, Object action)
	{
		checkModification();
		try
		{
			GraphicalComponent gc = form.createNewGraphicalComponent(new Point(x, y));
			gc.setSize(new Dimension(width, height));
			gc.setText(txt);
			int id = getMethodId(action, gc, application);
			gc.setOnActionMethodID(id);
			return new JSButton(this, gc, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	public JSLabel js_newLabel(String txt, int x, int y, int width, int height)
	{
		return js_newLabel(txt, x, y, width, height, null);
	}

	/**
	 * Creates a new JSLabel object on the form - including the text of the label, the "x" and "y" position of the label object in pixels, the width and height of the label object in pixels and the optional JSMethod action such as the method for an onAction event.
	 *
	 * @sample 
	 * 	var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var label = form.newLabel('The text on the label', 140, 140, 50, 20);
	 * 	forms['newForm1'].controller.show(); 
	 *
	 * @param text the specified text of the label object
	 *
	 * @param x the horizontal "x" position of the label object in pixels
	 *
	 * @param y the vertical "y" position of the label object in pixels
	 *
	 * @param width the width of the label object in pixels
	 *
	 * @param height the height of the label object in pixels
	 *
	 * @param jsmethod optional the event action JSMethod of the label object
	 * 
	 * @return a JSLabel object
	 */
	public JSLabel js_newLabel(String txt, int x, int y, int width, int height, Object action)
	{
		checkModification();
		try
		{
			GraphicalComponent gc = form.createNewGraphicalComponent(new Point(x, y));
			gc.setSize(new Dimension(width, height));
			gc.setText(txt);
			int methodId = getMethodId(action, gc, application);
			if (methodId > 0)
			{
				gc.setOnActionMethodID(methodId);
				gc.setShowClick(false);
			}
			return new JSLabel(this, gc, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param action
	 * @param gc
	 */
	protected static int getMethodId(Object action, GraphicalComponent gc, IApplication application)
	{
		int methodId = -1;
		if (action != null)
		{
			if (action instanceof Function)
			{
				ScriptMethod scriptMethod = getScriptMethod((Function)action, application.getFlattenedSolution());
				if (scriptMethod != null)
				{
					methodId = getMethodId(application, gc, scriptMethod);
				}
			}
			else if (action instanceof JSMethod)
			{
				methodId = getMethodId(application, gc, (JSMethod)action);
			}
			else
			{
				throw new RuntimeException("method argument not a jsmethod"); //$NON-NLS-1$ 
			}
		}
		return methodId;
	}

	/**
	 * Creates a new JSPortal object on the form - including the name of the JSPortal object; the relation the JSPortal object is based on, the "x" and "y" position of the JSPortal object in pixels, as well as the width and height of the JSPortal object in pixels.
	 * 
	 * @sample
	 *  var form = solutionModel.newForm('newForm1', 'server1', 'table1', null, true, 800, 600);
	 * 	var relation = solutionModel.newRelation('parentToChild','server1','table1','server2','table2',JSRelation.INNER_JOIN);
	 * 	relation.newRelationItem('another_parent_table_id', '=', 'another_child_table_parent_id');
	 * 	var portal = form.newPortal('portal',relation,200,200,300,300);
	 * 	portal.newField('someColumn',JSField.TEXT_FIELD,200,200,120);
	 *	forms['newForm1'].controller.show();
	 * 
	 * @param name the specified name of the JSPortal object
	 * @param relation the relation of the JSPortal object
	 * @param x the horizontal "x" position of the JSPortal object in pixels
	 * @param y the vertical "y" position of the JSPortal object in pixels
	 * @param width the width of the JSPortal object in pixels
	 * @param height the height of the JSPortal object in pixels
	 * 
	 * @return a JSPortal object 
	 */
	public JSPortal js_newPortal(String name, Object relation, int x, int y, int width, int height)
	{
		checkModification();
		try
		{
			Portal portal = form.createNewPortal(name, new Point(x, y));
			portal.setSize(new Dimension(width, height));
			String relationName = null;
			if (relation instanceof RelatedFoundSet)
			{
				relationName = ((RelatedFoundSet)relation).getRelationName();
			}
			else if (relation instanceof String)
			{
				relationName = (String)relation;
			}
			else if (relation instanceof JSRelation)
			{
				relationName = ((JSRelation)relation).js_getName();
			}
			portal.setRelationName(relationName);
			return new JSPortal(this, portal, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSPortal that has the given name.
	 *
	 * @sample 
	 *	var frm = solutionModel.getForm("myForm");
	 * 	var portal = frm.getPortal("myPortal");
	 *  portal.initialSort = 'my_table_text desc';
	 *
	 * @param name the specified name of the portal
	 * 
	 * @return a JSPortal object
	 */
	public JSPortal js_getPortal(String name)
	{
		if (name == null) return null;
		Iterator<Portal> portals = null;
		try
		{
			portals = application.getFlattenedSolution().getFlattenedForm(form).getPortals();
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		if (portals != null)
		{
			while (portals.hasNext())
			{
				Portal portal = portals.next();
				if (name.equals(portal.getName()))
				{
					return new JSPortal(this, portal, application, false);
				}
			}
		}
		return null;
	}

	/**
	 * Removes a JSPortal that has the given name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * 	var form = solutionModel.newForm('newFormX','myServer','myTable',null,true,800,600);
	 *	var relation = solutionModel.newRelation('parentToChild','myServer','myTable','myServer','myOtherTable',JSRelation.INNER_JOIN);
	 * 	relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * 	var jsportal = form.newPortal('jsp',relation,100,400,300,300);
	 * 	jsportal.newField('child_table_id',JSField.TEXT_FIELD,200,200,120);
	 * 	var jsmethod = form.newFormMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jsp') == true) application.output('Portal removed ok'); else application.output('Portal could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * 	var removerButton = form.newButton('Click here to remove the portal',450,500,250,50,jsmethod);
	 * 	removerButton.name = 'remover';
	 * 	forms['newFormX'].controller.show();
	 * 
	 * @param name the specified name of the JSPortal to be removed
	 * 
	 * @return true if the JSPortal has successfully been removed; false otherwise
	 */
	public boolean js_removePortal(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<Portal> portals = form.getPortals();
		while (portals.hasNext())
		{
			Portal portal = portals.next();
			if (name.equals(portal.getName()))
			{
				form.removeChild(portal);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all JSPortal objects of this form, including the ones without a name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var portals = frm.getPortals();
	 * 	for (var i in portals)
	 * 	{
	 * 		var p = portals[i];
	 * 		if (p.name != null)
	 * 			application.output(p.name);
	 * 		else
	 * 			application.output("unnamed portal detected");
	 * 	}
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return an array of all JSPortal objects on this form
	 *
	 */
	public JSPortal[] js_getPortals(Object[] args)
	{
		ArrayList<JSPortal> portals = new ArrayList<JSPortal>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<Portal> iterator = application.getFlattenedSolution().getFlattenedForm(form).getPortals();
				while (iterator.hasNext())
				{
					Portal tabPanel = iterator.next();
					portals.add(new JSPortal(this, tabPanel, application, false));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<Portal> iterator = form.getPortals();
			while (iterator.hasNext())
			{
				Portal tabPanel = iterator.next();
				portals.add(new JSPortal(this, tabPanel, application, false));
			}
		}
		return portals.toArray(new JSPortal[portals.size()]);
	}

	/**
	 * Creates a new JSTabPanel object on the form - including the name of the JSTabPanel object, the "x" and "y" position of the JSTabPanel object in pixels, as well as the width and height of the JSTabPanel object in pixels.
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('parentForm','theServer','parent_table',null,false,640,480); 
	 * 	var childOne = solutionModel.newForm('childOne','theServer','child_table',null,false,400,300);
	 * 	childOne.newField('child_table_text', JSField.TEXT_FIELD,10,10,100,20); 
	 * 	var parentToChild = solutionModel.newRelation('parentToChild','theServer','parent_table','theServer','child_table',JSRelation.INNER_JOIN);
	 * 	parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * 	var childTwo = solutionModel.newForm('childTwo','theServer','my_table',null,false,400,300);
	 * 	childTwo.newField('my_table_image', JSField.IMAGE_MEDIA,10,10,100,100); 
	 *  var tabPanel = form.newTabPanel('tabs',10,10,620,460);
	 *  tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 *  tabPanel.newTab('tab2','Child Two',childTwo);
	 *  forms['parentForm'].controller.show();
	 * 
	 * @param name the specified name of the JSTabPanel object
	 * @param x the horizontal "x" position of the JSTabPanel object in pixels
	 * @param y the vertical "y" position of the JSTabPanel object in pixels
	 * @param width the width of the JSTabPanel object in pixels
	 * @param height the height of the JSTabPanel object in pixels
	 * 
	 * @return a JSTabPanel object	
	 */
	public JSTabPanel js_newTabPanel(String name, int x, int y, int width, int height)
	{
		checkModification();
		try
		{
			TabPanel tabPanel = form.createNewTabPanel(name);
			tabPanel.setSize(new Dimension(width, height));
			tabPanel.setLocation(new Point(x, y));
			return new JSTabPanel(this, tabPanel, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSTabPanel that has the given name.
	 *
	 * @sample
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var tabPanel = frm.getTabPanel("myTabPanel"); 
	 * 	var tabs = tabPanel.getTabs(); 
	 * 	for (var i=0; i<tabs.length; i++) 
	 *		application.output("Tab " + i + " has text " + tabs[i].text);	 
	 * 
	 * @param name the specified name of the tabpanel
	 * 
	 * @return a JSTabPanel object
	 */
	public JSTabPanel js_getTabPanel(String name)
	{
		if (name == null) return null;
		try
		{
			Iterator<TabPanel> tabPanels = application.getFlattenedSolution().getFlattenedForm(form).getTabPanels();
			while (tabPanels.hasNext())
			{
				TabPanel tabPanel = tabPanels.next();
				if (name.equals(tabPanel.getName()))
				{
					return new JSTabPanel(this, tabPanel, application, false);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * Removes a JSTabPanel that has the given name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX','myServer','parentTable',null,false,800,600);
	 * var childOne = solutionModel.newForm('childOne','myServer','childTable',null,false,400,300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD,10,10,100,20); 
	 * var parentToChild = solutionModel.newRelation('parentToChild','myServer','parentTable','myServer','childTable',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_id');
	 * var childTwo = solutionModel.newForm('childTwo','myServer','anotherTable',null,false,400,300);
	 * childTwo.newField('columnDataProvider', JSField.TEXT_FIELD,10,10,100,100);
	 * var tabPanel = form.newTabPanel('jst',10,10,620,460);
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * var jsmethod = form.newFormMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jst') == true)\n application.output('TabPanel has been removed ok');\n else\n application.output('TabPanel could not be deleted');\n forms['newFormX'].controller.recreateUI();\n}");
	 * var removerButton = form.newButton('Click here to remove the tab panel',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show(); 
	 *
	 * @param name the specified name of the JSTabPanel to be removed
	 * 
	 * @return true is the JSTabPanel has been successfully removed, false otherwise
	 */
	public boolean js_removeTabPanel(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<TabPanel> tabPanels = form.getTabPanels();
		while (tabPanels.hasNext())
		{
			TabPanel tabPanel = tabPanels.next();
			if (name.equals(tabPanel.getName()))
			{
				form.removeChild(tabPanel);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all JSTabPanels of this form, including the ones without a name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var tabPanels = frm.getTabPanels();
	 * 	for (var i in tabPanels)
	 * 	{
	 *		var tp = tabPanels[i];
	 *		if (tp.name != null)
	 *			application.output("Tab " + tp.name + " has text " + tp.text);
	 *		else
	 *			application.output("Tab with text " + tp.text + " has no name");
	 *	}
	 *
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return an array of all JSTabPanel objects on this form			
	 *		
	 */
	public JSTabPanel[] js_getTabPanels(Object[] args)
	{
		ArrayList<JSTabPanel> tabPanels = new ArrayList<JSTabPanel>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<TabPanel> iterator = application.getFlattenedSolution().getFlattenedForm(form).getTabPanels();
				while (iterator.hasNext())
				{
					TabPanel tabPanel = iterator.next();
					tabPanels.add(new JSTabPanel(this, tabPanel, application, false));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<TabPanel> iterator = form.getTabPanels();
			while (iterator.hasNext())
			{
				TabPanel tabPanel = iterator.next();
				tabPanels.add(new JSTabPanel(this, tabPanel, application, false));
			}
		}
		return tabPanels.toArray(new JSTabPanel[tabPanels.size()]);
	}

	private JSPart[] getParts(int partType)
	{
		ArrayList<JSPart> lst = new ArrayList<JSPart>();
		Iterator<Part> parts = form.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == partType)
			{
				lst.add(new JSPart(this, part, false));
			}
		}
		return lst.toArray(new JSPart[lst.size()]);
	}

	private JSPart getPart(int partType)
	{
		return getPart(partType, -1);
	}

	private JSPart getPart(int partType, int height)
	{
		try
		{
			Iterator<Part> parts = application.getFlattenedSolution().getFlattenedForm(form).getParts();
			while (parts.hasNext())
			{
				Part part = parts.next();
				if (part.getPartType() == partType && (height == -1 || part.getHeight() == height))
				{
					if (!testExtendFormForPart(partType, height))
					{
						return new JSPart(this, part, false);
					}
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * @param partType
	 * @param height
	 */
	private boolean testExtendFormForPart(int partType, int height)
	{
		int extendsFormID = form.getExtendsFormID();
		if (extendsFormID > 0)
		{
			Form superForm = application.getFlattenedSolution().getForm(extendsFormID);
			if (superForm != null)
			{
				if (superForm.getExtendsFormID() != 0)
				{
					try
					{
						superForm = new FlattenedForm(application.getFlattenedSolution(), superForm);
					}
					catch (Exception e)
					{
						throw new RuntimeException("Cant calculate y offset for part, couldn't create flattened form", e); //$NON-NLS-1$
					}
				}
				Iterator<Part> superParts = superForm.getParts();
				while (superParts.hasNext())
				{
					Part superPart = superParts.next();
					// don't return the part if the extends form already has this part.
					if (superPart.getPartType() == partType && (height == -1 || superPart.getHeight() == height))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @param height
	 * @return
	 */
	private JSPart getOrCreatePart(int partType, int height)
	{
		checkModification();
		JSPart part;
		if (partType == Part.LEADING_SUBSUMMARY || partType == Part.TRAILING_SUBSUMMARY)
		{
			part = getPart(partType, height);
		}
		else
		{
			part = getPart(partType);
		}
		if (part == null)
		{
			if (testExtendFormForPart(partType, height))
			{
				throw new RuntimeException("Super form already has this part"); //$NON-NLS-1$
			}
			try
			{

				part = new JSPart(this, form.createNewPart(partType, height), true);

				int testHeight = 0;
				Iterator<Part> parts = form.getParts();
				while (parts.hasNext())
				{
					Part p = parts.next();
					int test = p.getHeight();
					if (test < testHeight)
					{
						throw new RuntimeException(
							"Illegal lowerbound " + height + " for the part " + Part.getDisplayName(partType) + " it must be greater than the previous part lowerbound " + testHeight); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					testHeight = test;
				}
			}
			catch (RepositoryException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			part.js_setHeight(height);
		}
		return part;
	}


	/**
	 * Creates a new part on the form. The type of the new part (use one of the JSPart constants)
	 * and its height must be specified.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('myForm', 'example_data', 'my_table', null, false, 1200, 800);
	 * var header = form.newPart(JSPart.HEADER, 100);
	 * header.background = 'yellow';
	 * var body = form.newPart(JSPart.BODY, 700);
	 * body.background = 'green';
	 * var footer = form.newPart(JSPart.FOOTER, 800);
	 * footer.background = 'orange';
	 *
	 * @param type The type of the new part.
	 * 
	 * @param height The height of the new part
	 * 
	 * @return A JSPart instance corresponding to the newly created form part.
	 */
	public JSPart js_newPart(int type, int height)
	{
		return getOrCreatePart(type, height);
	}


	/**
	 * Creates a new Title Header part on the form.
	 * 
	 * @sample
	 * var titleHeader = form.newTitleHeaderPart(40);
	 * 
	 * @return A JSPart instance corresponding to the newly created Title Header form part.
	 */
	public JSPart js_newTitleHeaderPart(int height)
	{
		return getOrCreatePart(Part.TITLE_HEADER, height);
	}


	/**
	 * Creates a new Header part on the form.
	 * 
	 * @sample
	 * var header = form.newHeaderPart(80);
	 * 
	 * @return A JSPart instance corresponding to the newly created Header form part.
	 */
	public JSPart js_newHeaderPart(int height)
	{
		return getOrCreatePart(Part.HEADER, height);
	}

	/**
	 * Creates a new Leading Grand Summary part on the form.
	 * 
	 * @sample
	 * var leadingGrandSummary = form.newLeadingGrandSummaryPart(120);
	 * 
	 * @return A JSPart instance corresponding to the newly created Leading Grand Summary form part.
	 */
	public JSPart js_newLeadingGrandSummaryPart(int height)
	{
		return getOrCreatePart(Part.LEADING_GRAND_SUMMARY, height);
	}

	/**
	 * Creates a new Leading Subsummary part on the form.
	 * 
	 * @sample
	 * var leadingSubsummary = form.newLeadingSubSummaryPart(160);
	 * 
	 * @return A JSPart instance corresponding to the newly created Leading Subsummary form part.
	 */
	public JSPart js_newLeadingSubSummaryPart(int height)
	{
		return getOrCreatePart(Part.LEADING_SUBSUMMARY, height);
	}


	/**
	 * Creates a new Trailing Subsummary part on the form.
	 * 
	 * @sample
	 * var trailingSubsummary = form.newTrailingSubSummaryPart(360);
	 * 
	 * @return A JSPart instance corresponding to the newly created Trailing Subsummary form part.
	 */
	public JSPart js_newTrailingSubSummaryPart(int height)
	{
		return getOrCreatePart(Part.TRAILING_SUBSUMMARY, height);
	}

	/**
	 * Creates a new Trailing Grand Summary part on the form.
	 * 
	 * @sample
	 * var trailingGrandSummary = form.newTrailingGrandSummaryPart(400);
	 * 
	 * @return A JSPart instance corresponding to the newly created Trailing Grand Summary form part.
	 */
	public JSPart js_newTrailingGrandSummaryPart(int height)
	{
		return getOrCreatePart(Part.TRAILING_GRAND_SUMMARY, height);
	}

	/**
	 * Creates a new Footer part on the form.
	 *
	 * @sample 
	 * var footer = form.newFooterPart(440);
	 * 
	 * @return A JSPart instance corresponding to the newly created Footer form part.
	 */
	public JSPart js_newFooterPart(int height)
	{
		return getOrCreatePart(Part.FOOTER, height);
	}


	/**
	 * Creates a new Title Footer part on the form.
	 * 
	 * @sample
	 * var titleFooter = form.newTitleFooterPart(500);
	 * 
	 * @return A JSPart instance corresponding to the newly created Title Footer form part.
	 */
	public JSPart js_newTitleFooterPart(int height)
	{
		return getOrCreatePart(Part.TITLE_FOOTER, height);
	}

	/**
	 * Gets all the parts from the form, ordered by there height (lowerbound) property, from top == 0 to bottom.
	 *
	 * @sample 
	 * var allParts = form.getParts()
	 * for (var i=0; i<allParts.length; i++) {
	 *	if (allParts[i].getPartType() == JSPart.BODY)
	 *		application.output('body Y offset: ' + allParts[i].getPartYOffset());
	 * }
	 * 
	 * @return An array of JSPart instances corresponding to the parts of the form.
	 */
	public JSPart[] js_getParts()
	{
		ArrayList<JSPart> lst = new ArrayList<JSPart>();
		Iterator<Part> parts = form.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (!testExtendFormForPart(part.getPartType(), part.getHeight())) lst.add(new JSPart(this, part, false));
		}
		return lst.toArray(new JSPart[lst.size()]);
	}

	public JSPart js_getPart(int type)
	{
		return getPart(type);
	}

	/**
	 * Gets a part of the form from the given type (see JSPart constants). 
	 * Use the optional height if you want to get a specific LEADING_SUBSUMMARY or TRAILING_SUBSUMMARY.
	 *
	 * @sample 
	 * form.getPart(JSPart.HEADER).background = 'red';
	 * form.getPart(JSPart.LEADING_SUBSUMMARY, 160).background = 'red';
	 *
	 * @param type The type of the part to retrieve.
	 *
	 * @param height optional The heigh of the part to retrieve. Use this parameter when retrieving one of multiple
	 * 	                      Leading/Trailing Subsummary parts.
	 * 
	 * @return A JSPart instance representing the retrieved form part.
	 */
	public JSPart js_getPart(int type, int height)
	{
		return getPart(type, height);
	}

	public int js_getPartYOffset(int type)
	{
		return js_getPartYOffset(type, -1);
	}

	/**
	 * Returns the Y offset of a given part (see JSPart) of the form. This will include 
	 * all the super forms parts if this form extends a form. Use the optional height for 
	 * targetting one of multiple subsummary parts.
	 *
	 * @sample
	 * // get the subform
	 * var form = solutionModel.getForm('SubForm');
	 * // get the start offset of the body
	 * var height = form.getPartYOffset(JSPart.BODY);
	 * // place a new button based on the start offset.
	 * form.newButton('mybutton',50,50+height,80,20,solutionModel.getGlobalMethod('test'));
	 *
	 * @param type The type of the part whose Y offset will be returned.
	 *
	 * @param height optional The height of the part whose Y offset will be returned. This is used when
	 *                        one of multiple Leading/Trailing Sumsummary parts is retrieved.
	 *                        
	 * @return A number holding the Y offset of the specified form part.
	 */
	public int js_getPartYOffset(int type, int height)
	{
		Form f = form;
		if (form.getExtendsFormID() != 0)
		{
			try
			{
				f = new FlattenedForm(application.getFlattenedSolution(), f);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Cant calculate y offset for part, couldn't create flattened form", e); //$NON-NLS-1$
			}
		}

		Iterator<Part> parts = f.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == type && (height == -1 || part.getHeight() == height))
			{
				return f.getPartStartYPos(part.getID());
			}
		}
		return -1;
	}

	public boolean js_removePart(int type)
	{
		return js_removePart(type, -1);
	}

	/**
	 * Removes a JSPart of the given type. Use the height parameter if you want to remove one of multiple
	 * subsummary parts.
	 *
	 * @sample 
	 * form.removePart(JSPart.HEADER);
	 * form.removePart(JSPart.LEADING_SUBSUMMARY, 160);
	 *
	 * @param type The type of the part that should be removed.
	 *
	 * @param height optional The height of the part that should be removed. Use this parameter when you want
	 *                        to remove one of multiple Leading/Trailing Subsummary parts.
	 *                        
	 * @return True if the part is successfully removed, false otherwise.
	 */
	public boolean js_removePart(int type, int height)
	{
		checkModification();
		Iterator<Part> parts = form.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == type && (height == -1 || part.getHeight() == height))
			{
				form.removeChild(part);
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieves the Body part of the form.
	 *
	 * @sample 
	 * form.getBodyPart().background = 'blue';
	 * 
	 * @return A JSPart instance corresponding to the Body part of the form.
	 */
	public JSPart js_getBodyPart()
	{
		return getPart(Part.BODY);
	}

	/**
	 * Retrieves the Title Header part of the form.
	 * 
	 * @sample
	 * form.getTitleHeaderPart().background = 'red';
	 * 
	 * @return A JSPart instance corresponding to the Title Header part of the form.
	 */
	public JSPart js_getTitleHeaderPart()
	{
		return getPart(Part.TITLE_HEADER);
	}

	/**
	 * Retrieves the Header part of the form.
	 * 
	 * @sample
	 * form.getHeaderPart().background = 'orange';
	 * 
	 * @return A JSPart instance corresponding to the Header part of the form.
	 */
	public JSPart js_getHeaderPart()
	{
		return getPart(Part.HEADER);
	}

	/**
	 * Retrieves the Leading Grand Summary part of the form.
	 * 
	 * @sample
	 * form.getLeadingGrandSummaryPart().background = 'yellow';
	 * 
	 * @return A JSPart instance corresponding to the Leading Grand Summary part of the form.
	 */
	public JSPart js_getLeadingGrandSummaryPart()
	{
		return getPart(Part.LEADING_GRAND_SUMMARY);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getLeadingSubSummaryParts()
	 */
	@Deprecated
	public JSPart js_getLeadingSubSummaryPart()
	{
		return getPart(Part.LEADING_SUBSUMMARY);
	}

	/**
	 * Gets an array of the Leading Subsummary parts of the form, ordered by their height from top == 0 to bottom.
	 *
	 * @sample 
	 * form.getLeadingSubSummaryParts()[0].background = 'green';
	 * 
	 * @return An array of JSPart instances corresponding to the Leading Subsummary parts of the form.
	 */
	public JSPart[] js_getLeadingSubSummaryParts()
	{
		return getParts(Part.LEADING_SUBSUMMARY);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getTrailingSubSummaryParts()
	 */
	@Deprecated
	public JSPart js_getTrailingSubSummaryPart()
	{
		return getPart(Part.TRAILING_SUBSUMMARY);
	}

	/**
	 * Gets an array of the Trailing Subsummary parts of the form, ordered by their height from top == 0 to bottom.
	 *
	 * @sample 
	 * form.getTrailingSubSummaryParts()[0].background = 'green';
	 * 
	 * @return An array of JSPart instances corresponding to the Trailing Subsummary parts of the form.
	 */
	public JSPart[] js_getTrailingSubSummaryParts()
	{
		return getParts(Part.TRAILING_SUBSUMMARY);
	}

	/**
	 * Retrieves the Trailing Grand Summary part of the form.
	 * 
	 * @sample
	 * form.getTrailingGrandSummaryPart().background = 'yellow';
	 * 
	 * @return A JSPart instance corresponding to the Trailing Grand Summary part of the form.
	 */
	public JSPart js_getTrailingGrandSummaryPart()
	{
		return getPart(Part.TRAILING_GRAND_SUMMARY);
	}

	/**
	 * Retrieves the Footer part of the form.
	 * 
	 * @sample
	 * form.getFooterPart().background = 'magenta';
	 * 
	 * @return A JSPart instance corresponding to the Footer part of the form.
	 */
	public JSPart js_getFooterPart()
	{
		return getPart(Part.FOOTER);
	}

	/**
	 * Retrieves the Title Footer part of the form.
	 * 
	 * @sample
	 * form.getTitleFooterPart().background = 'gray';
	 * 
	 * @return A JSPart instance corresponding to the Title Footer part of the form.
	 */
	public JSPart js_getTitleFooterPart()
	{
		return getPart(Part.TITLE_FOOTER);
	}

	/**
	 * The field with the specified name.
	 * 
	 * @sample
	 * 	var form = solutionModel.getForm("myForm");
	 * 	var field = form.getField("myField");
	 * 	application.output(field.dataProviderID);
	 * 
	 * @param name the specified name of the field
	 * 
	 * @return a JSField object
	 */
	public JSField js_getField(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<Field> fields = application.getFlattenedSolution().getFlattenedForm(form).getFields();
			while (fields.hasNext())
			{
				Field field = fields.next();
				if (name.equals(field.getName()))
				{
					return new JSField(this, field, application, false);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * Removes a JSField that has the given name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX','myServer','myTable',null,true,800,600);
	 * var jsfield = form.newField(globals.myGlobalVariable,JSField.TEXT_FIELD,100,300,200,50);
	 * jsfield.name = 'jsf';
	 * var jsmethod = form.newFormMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jsf') == true) application.output('Field has been removed ok'); else application.output('Field could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the field',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSField to remove 
	 * 
	 * @return true is the JSField has been successfully removed; false otherwise
	 */
	public boolean js_removeField(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<Field> fields = form.getFields();
		while (fields.hasNext())
		{
			Field field = fields.next();
			if (name.equals(field.getName()))
			{
				form.removeChild(field);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all JSField objects of this form, including the ones without a name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var fields = frm.getFields();
	 * 	for (var f in fields)
	 * 	{
	 * 		var fname = fields[f].name;
	 * 		if (fname != null)
	 * 			application.output(fname);
	 * 	}
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return all JSField objects of this form
	 *
	 */
	public JSField[] js_getFields(Object[] args)
	{
		ArrayList<JSField> fields = new ArrayList<JSField>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<Field> iterator = application.getFlattenedSolution().getFlattenedForm(form).getFields();
				while (iterator.hasNext())
				{
					Field field = iterator.next();
					fields.add(new JSField(this, field, application, false));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<Field> iterator = form.getFields();
			while (iterator.hasNext())
			{
				Field field = iterator.next();
				fields.add(new JSField(this, field, application, false));
			}
		}
		return fields.toArray(new JSField[fields.size()]);
	}

	/**
	 * Returns a JSButton that has the given name.
	 *
	 * @sample 
	 * var btn = myForm.getButton("hello");
	 * application.output(btn.text);
	 *
	 * @param name the specified name of the button
	 * 
	 * @return a JSButton object 
	 */
	public JSButton js_getButton(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<GraphicalComponent> graphicalComponents = application.getFlattenedSolution().getFlattenedForm(form).getGraphicalComponents();
			while (graphicalComponents.hasNext())
			{
				GraphicalComponent button = graphicalComponents.next();
				if (name.equals(button.getName()) && button.getOnActionMethodID() != 0 && button.getShowClick())
				{
					return new JSButton(this, button, application, false);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * Removes a JSButton that has the specified name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 *  var form = solutionModel.newForm('newFormX','myServer','myTable',null,true,800,600);
	 *  var b1 = form.newButton('This is button1',100,100,200,50,null);
	 *  b1.name = 'b1';
	 *  var jsmethod = form.newFormMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX'); if (form.removeButton('b1') == true) application.output('Button has been removed ok'); else application.output('Button could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 *  var b2 = form.newButton('Click here to remove button1',100,230,200,50,jsmethod);
	 *  b2.name = 'b2';
	 *  forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSButton to be removed 
	 * 
	 * @return true if the JSButton has been removed; false otherwise
	 */
	public boolean js_removeButton(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && button.getOnActionMethodID() != 0 && button.getShowClick())
			{
				form.removeChild(button);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all JSButtons of this form, including the ones without a name.
	 *
	 * @sample 
	 * var buttons = myForm.getButtons();
	 * for (var b in buttons)
	 * {
	 * 		if (buttons[b].name != null) 
	 * 			application.output(buttons[b].name);
	 * 		else
	 * 			application.output(buttons[b].text + " has no name ");
	 * }
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return the list of all JSbuttons on this forms
	 *
	 */
	public JSButton[] js_getButtons(Object[] args)
	{
		ArrayList<JSButton> buttons = new ArrayList<JSButton>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<GraphicalComponent> graphicalComponents = application.getFlattenedSolution().getFlattenedForm(form).getGraphicalComponents();
				while (graphicalComponents.hasNext())
				{
					GraphicalComponent button = graphicalComponents.next();
					if (button.getOnActionMethodID() != 0 && button.getShowClick())
					{
						buttons.add(new JSButton(this, button, application, false));
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
			while (graphicalComponents.hasNext())
			{
				GraphicalComponent button = graphicalComponents.next();
				if (button.getOnActionMethodID() != 0 && button.getShowClick())
				{
					buttons.add(new JSButton(this, button, application, false));
				}
			}
		}
		return buttons.toArray(new JSButton[buttons.size()]);
	}

	/**
	 * Creates a new JSBean object on the form - including the name of the JSBean object; the classname the JSBean object is based on, the "x" and "y" position of the JSBean object in pixels, as well as the width and height of the JSBean object in pixels.
	 * 
	 * @sample
	 *  var form = solutionModel.newForm('newForm1', 'server1', 'table1', null, true, 800, 600);
	 * 	var bean = form.newBean('bean','myclass',200,200,300,300);
	 *	forms['newForm1'].controller.show();
	 * 
	 * @param name the specified name of the JSBean object
	 * @param className the class name of the JSBean object
	 * @param x the horizontal "x" position of the JSBean object in pixels
	 * @param y the vertical "y" position of the JSBean object in pixels
	 * @param width the width of the JSBean object in pixels
	 * @param height the height of the JSBean object in pixels
	 * 
	 * @return a JSBean object 
	 */
	public JSBean js_newBean(String name, String className, int x, int y, int width, int height)
	{
		checkModification();
		try
		{
			Bean bean = form.createNewBean(name, className);
			bean.setSize(new Dimension(width, height));
			bean.setLocation(new Point(x, y));
			return new JSBean(this, bean, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSBean that has the given name.
	 *
	 * @sample 
	 * var btn = myForm.getBean("mybean");
	 * application.output(mybean.className);
	 *
	 * @param name the specified name of the bean
	 * 
	 * @return a JSBean object 
	 */
	public JSBean js_getBean(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<Bean> beans = application.getFlattenedSolution().getFlattenedForm(form).getBeans();
			while (beans.hasNext())
			{
				Bean bean = beans.next();
				if (name.equals(bean.getName()))
				{
					return new JSBean(this, bean, false);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.log(ex);
		}
		return null;
	}

	/**
	 * Removes a JSBean that has the specified name. Returns true if removal was successful, false otherwise. 
	 *
	 * @sample 
	 *  var form = solutionModel.getForm('myform');
	 *  form.removeBean('mybean')
	 *  
	 * @param name the specified name of the JSBean to be removed 
	 * 
	 * @return true if the JSBean has been removed; false otherwise
	 */
	public boolean js_removeBean(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<Bean> beans = form.getBeans();
		while (beans.hasNext())
		{
			Bean bean = beans.next();
			if (name.equals(bean.getName()))
			{
				form.removeChild(bean);
				return true;

			}
		}
		return false;
	}

	/**
	 * Returns all JSBeans of this form.
	 *
	 * @sample 
	 * var beans = myForm.getBeans();
	 * for (var b in beans)
	 * {
	 * 		if (beans[b].name != null) 
	 * 			application.output(beans[b].name);
	 * }
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return the list of all JSbuttons on this forms
	 *
	 */
	public JSBean[] js_getBeans(Object[] args)
	{
		ArrayList<JSBean> beans = new ArrayList<JSBean>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<Bean> iterator = application.getFlattenedSolution().getFlattenedForm(form).getBeans();
				while (iterator.hasNext())
				{
					Bean bean = iterator.next();
					beans.add(new JSBean(this, bean, false));
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<Bean> iterator = form.getBeans();
			while (iterator.hasNext())
			{
				Bean bean = iterator.next();
				beans.add(new JSBean(this, bean, false));
			}
		}
		return beans.toArray(new JSBean[beans.size()]);
	}

	/**
	 * Returns a JSComponent that has the given name; if found it will be a JSField, JSLabel, JSButton, JSPortal, JSBean or JSTabPanel.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var cmp = frm.getComponent("componentName");
	 * 	application.output("Component type and name: " + cmp);
	 *
	 * @param name the specified name of the component 
	 * 
	 * @return a JSComponent object (might be a JSField, JSLabel, JSButton, JSPortal, JSBean or JSTabPanel)
	 */
	public JSComponent< ? > js_getComponent(String name)
	{
		JSComponent< ? > comp = js_getLabel(name);
		if (comp != null) return comp;
		comp = js_getButton(name);
		if (comp != null) return comp;
		comp = js_getField(name);
		if (comp != null) return comp;
		comp = js_getPortal(name);
		if (comp != null) return comp;
		comp = js_getTabPanel(name);
		if (comp != null) return comp;
		comp = js_getBean(name);
		if (comp != null) return comp;
		return null;
	}

	/**
	 * Removes a component (JSLabel, JSButton, JSField, JSPortal, JSBean, JSTabpanel) that has the given name. It is the same as calling "if(!removeLabel(name) && !removeButton(name) ....)".
	 * Returns true if removal was successful, false otherwise.  
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX','myServer','parentTable',null,true,1000,750);
	 * var jsbutton = form.newButton('JSButton to delete',100,100,200,50,null);
	 * jsbutton.name = 'jsb';
	 * var jslabel = form.newLabel('JSLabel to delete',100,200,200,50,null);
	 * jslabel.name = 'jsl';
	 * jslabel.transparent = false;
	 * jslabel.background = 'green';
	 * var jsfield = form.newField('globals.myGlobalVariable',JSField.TEXT_FIELD,100,300,200,50);
	 * jsfield.name = 'jsf';
	 * var relation = solutionModel.newRelation('parentToChild','myServer','parentTable','myServer','childTable',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * var jsportal = form.newPortal('jsp',relation,100,400,300,300);
	 * jsportal.newField('child_table_id',JSField.TEXT_FIELD,200,200,120);
	 * var childOne = solutionModel.newForm('childOne','myServer','childTable',null,false,400,300);
	 * childOne.newField('child_table_id', JSField.TEXT_FIELD,10,10,100,20);
	 * var childTwo = solutionModel.newForm('childTwo','myServer','someOtherTable',null,false,400,300);
	 * childTwo.newField('some_table_id', JSField.TEXT_FIELD,10,10,100,100); 
	 * var jstabpanel = form.newTabPanel('jst',450,30,620,460);
	 * jstabpanel.newTab('tab1','Child One',childOne,relation);
	 * jstabpanel.newTab('tab2','Child Two',childTwo);
	 * var jsmethod = form.newFormMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if ((form.removeComponent('jsb') == true) && (form.removeComponent('jsl') == true) && (form.removeComponent('jsf') == true) && (form.removeComponent('jsp') == true) & (form.removeComponent('jst') == true)) application.output('Components removed ok'); else application.output('Some component(s) could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove form components',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the component to be deleted
	 * 
	 * @return true if component has been successfully deleted; false otherwise
	 */
	public boolean js_removeComponent(String name)
	{
		if (name == null) return false;
		if (js_removeLabel(name)) return true;
		if (js_removeButton(name)) return true;
		if (js_removeField(name)) return true;
		if (js_removePortal(name)) return true;
		if (js_removeTabPanel(name)) return true;
		if (js_removeBean(name)) return true;
		return false;
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean or JSTabPanel.
	 *
	 * @sample
	 * 	var form = solutionModel.getForm("myForm");
	 * 	var components = form.getComponents();
	 * 	for (var i in components)
	 * 		application.output("Component type and name: " + components[i]); 
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return an array of all the JSComponents on the form.
	 */
	public JSComponent< ? >[] js_getComponents(Object[] args)
	{
		ArrayList<JSComponent< ? >> lst = new ArrayList<JSComponent< ? >>();
		lst.addAll(Arrays.asList(js_getLabels(args)));
		lst.addAll(Arrays.asList(js_getButtons(args)));
		lst.addAll(Arrays.asList(js_getFields(args)));
		lst.addAll(Arrays.asList(js_getPortals(args)));
		lst.addAll(Arrays.asList(js_getBeans(args)));
		lst.addAll(Arrays.asList(js_getTabPanels(args)));
		return lst.toArray(new JSComponent[lst.size()]);
	}

	/**
	 * Returns a JSLabel that has the given name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var label = frm.getLabel("myLabel");
	 * 	application.output(label.text); 
	 *
	 * @param name the specified name of the label
	 * 
	 * @return a JSLabel object (or null if the label with the specified name does not exist)
	 */
	public JSLabel js_getLabel(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<GraphicalComponent> graphicalComponents = application.getFlattenedSolution().getFlattenedForm(form).getGraphicalComponents();
			while (graphicalComponents.hasNext())
			{
				GraphicalComponent button = graphicalComponents.next();
				if (name.equals(button.getName()) && !(button.getOnActionMethodID() != 0 && button.getShowClick()))
				{
					return new JSLabel(this, button, application, false);
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * Removes a JSLabel that has the given name. Returns true if removal successful, false otherwise  
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newFormX','myServer','myTable',null,true,1000,750);
	 * var jslabel = form.newLabel('JSLabel to delete',100,200,200,50,null);
	 * jslabel.name = 'jsl';
	 * jslabel.transparent = false;
	 * jslabel.background = 'green';
	 * var jsmethod = form.newFormMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX'); if (form.removeComponent('jsl') == true) application.output('Label has been removed'); else application.output('Label could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the green label',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 * 
	 * @param name the specified name of the JSLabel to be removed
	 * 
	 * @return true if the JSLabel with the given name has successfully been removed; false otherwise
	 */
	public boolean js_removeLabel(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && !(button.getOnActionMethodID() != 0 && button.getShowClick()))
			{
				form.removeChild(button);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all JSLabels of this form, including the ones without a name.
	 *
	 * @sample 
	 * 	var frm = solutionModel.getForm("myForm");
	 * 	var labels = frm.getLabels();
	 * 	for (var i in labels)
	 * 	{
	 * 		var lname = labels[i].name;
	 * 		if (lname != null)
	 * 			application.output(lname);
	 * 	}
	 * 
	 * @param returnInheritedElements optional boolean true to also return the elements from parent form 
	 * @return all JSLabels on this form
	 *
	 */
	public JSLabel[] js_getLabels(Object[] args)
	{
		ArrayList<JSLabel> labels = new ArrayList<JSLabel>();
		if (args.length > 0 && Utils.getAsBoolean(args[0]))
		{
			try
			{
				Iterator<GraphicalComponent> graphicalComponents = application.getFlattenedSolution().getFlattenedForm(form).getGraphicalComponents();
				while (graphicalComponents.hasNext())
				{
					GraphicalComponent button = graphicalComponents.next();
					if (!(button.getOnActionMethodID() != 0 && button.getShowClick()))
					{
						labels.add(new JSLabel(this, button, application, false));
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
			while (graphicalComponents.hasNext())
			{
				GraphicalComponent button = graphicalComponents.next();
				if (!(button.getOnActionMethodID() != 0 && button.getShowClick()))
				{
					labels.add(new JSLabel(this, button, application, false));
				}
			}
		}
		return labels.toArray(new JSLabel[labels.size()]);
	}


	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#js_getBorderType()
	 */
	public String js_getBorderType()
	{
		return form.getBorderType();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getDefaultPageFormat()
	 * 
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * application.output(form.defaultPageFormat);
	 * form.defaultPageFormat = solutionModel.createPageFormat(612,792,72,72,72,72,SM_ORIENTATION.PORTRAIT,SM_UNITS.PIXELS) 
	 */
	public String js_getDefaultPageFormat()
	{
		return form.getDefaultPageFormat();
	}

	/**
	 * A JSForm instance representing the super form of this form, if this form has a super form. 
	 * 
	 * @sample
	 * var subForm = solutionModel.newForm('childForm','myServer','myTable',null,true,800,600);
	 * var superForm = solutionModel.newForm('childForm','myServer','myTable',null,true,800,600);
	 * subForm.extendsForm = superForm;
	 * 
	 */
	public JSForm js_getExtendsForm()
	{
		int extendsFormID = form.getExtendsFormID();
		if (extendsFormID > 0)
		{
			Form superForm = application.getFlattenedSolution().getForm(extendsFormID);
			if (superForm != null)
			{
				return new JSForm(application, superForm, false);
			}
		}
		return null;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getInitialSort()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm','myServer','myTable',null,true,800,600);
	 * form.initialSort = "column1 desc, column2 asc, column3 asc";
	 * 
	 */
	public String js_getInitialSort()
	{
		return form.getInitialSort();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getName()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm','myServer','myTable',null,true,800,600);
	 * var formName = form.name;
	 * application.output(formName);
	 * 
	 */
	public String js_getName()
	{
		return form.getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getNavigatorID()
	 *
	 * @sample 
	 * 	var aForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 *  // you can also use SM_DEFAULTS.INGORE to just reuse the navigator that is already set, or SM_DEFAULTS.DEFAULT to have the default servoy navigator.
	 *  // here we assign an other new form as the navigator.
	 * 	var aNavigator = solutionModel.newForm('navForm', 'myServer', 'myTable', null, false, 800, 600);
	 *  // set the navigators navigator to NONE
	 * 	aNavigator.navigator = SM_DEFAULTS.NONE; // Hide the navigator on the form. 
	 * 	myListViewForm.navigator = aNavigator;
	 * 	application.output(myListViewForm.navigator.name);
	 *  
	 */
	public JSForm js_getNavigator()
	{
		Form f = application.getFlattenedSolution().getForm(form.getNavigatorID());
		if (f != null)
		{
			return new JSForm(application, f, false);
		}
		return null;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getPaperPrintScale()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm','myServer','myTable',null,true,800,600);
	 * if (form.paperPrintScale < 100)
	 * 	form.paperPrintScale = 100;
	 * 
	 */
	public int js_getPaperPrintScale()
	{
		return form.getPaperPrintScale();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getRowBGColorCalculation()
	 * 
	 * @sample
	 * var form = solutionModel.newForm('myForm','myServer','myTable',null,true,800,600);
	 * //assign the global method as a string. Or use a calculation name as the string.
	 * form.rowBGColorCalculation = "globals.calculationDataProvider";
	 */
	public String js_getRowBGColorCalculation()
	{
		return form.getRowBGColorCalculation();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getScrollbars()
	 *
	 * @sample 
	 * var form = solutionModel.newForm('myForm','myServer','myTable',null,true,1000,600);
	 * form.scrollbars = SM_SCROLLBAR.VERTICAL_SCROLLBAR_NEVER; 
	 * forms['newForm1'].controller.show();
	 */
	public int js_getScrollbars()
	{
		return form.getScrollbars();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getServerName()
	 * 
	 * @sample 
	 * var form = solutionModel.newForm('myForm','myServer','myTable',null,true,800,600);
	 * form.serverName = 'anotherServerName';
	 * var theServerName = form.getServerName();
	 * application.output(theServerName);
	 */
	public String js_getServerName()
	{
		return form.getServerName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getShowInMenu()
	 * 
	 * @sample
	 * 	var aForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	var anotherForm= solutionModel.newForm('newForm2', 'myServer', 'myTable', null, true, 800, 600);
	 * 	//using 'anotherForm' as navigator for aForm
	 * 	anotherForm.showInMenu = false;
	 * 	anotherForm.navigator = null;
	 * 	aForm.navigator = anotherForm;
	 * 	application.output(aForm.navigator.name);
	 * 
	 */
	public boolean js_getShowInMenu()
	{
		return form.getShowInMenu();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getStyleClass()
	 * 
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * if (aForm.styleClass == null)
	 * 	aForm.styleClass = someStyleClass;
	 * else
	 * 	application.output("The Cascading Style Sheet (CSS) class name applied to this form is " + aForm.styleClass); 
	 */
	public String js_getStyleClass()
	{
		return form.getStyleClass();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getStyleName()
	 *
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * if (aForm.styleName == null)
	 * 	aForm.styleName = someServoyStyleName;
	 * else
	 * 	application.output("The name of the Servoy style that is being used on the form is " + aForm.styleName); 
	 * 
	 */
	public String js_getStyleName()
	{
		return form.getStyleName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getTableName()
	 * 
	 * @sample
	 * 	var aForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	aForm.tableName = 'anotherTableOfMine'
	 * 	if (forms['newForm1'].controller.find())
	 * 	{
	 * 		columnTextDataProvider = '=aSearchedValue'
	 * 		columnNumberDataProvider = '>10';
	 * 		forms['newForm1'].controller.search()
	 * 	}
	 */
	public String js_getTableName()
	{
		return form.getTableName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getDataSource()
	 * 
	 * @sample
	 * 	var myForm = solutionModel.newForm('newForm', 'aServer', 'aTable', 'aStyleName', false, 800, 600)
	 * 	myForm.dataSource = 'db:/anotherServerName/anotherTableName'
	 * 
	 */
	public String js_getDataSource()
	{
		return form.getDataSource();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getTitleText()
	 * 
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'aServer', 'aTable', 'aStyleName', false, 800, 600)
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
	public String js_getTitleText()
	{
		return form.getTitleText();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getTransparent()
	 * 
	 * @sample
	 * 	var form = solutionModel.newForm('myForm','myServer','myTable',null,true,1000,800);
	 * 	if (form.transparent == false)
	 * 	{
	 * 		var style = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * 		style.text = style.text + 'field { background-color: blue; }';
	 * 		form.styleName = 'myStyle';
	 * 	}
	 * 	var field = form.newField('columnTextDataProvider',JSField.TEXT_FIELD,100,100,100,50);
	 * 	forms['myForm'].controller.show(); 
	 */
	public boolean js_getTransparent()
	{
		return form.getTransparent();
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getNamedFoundSet()
	 */
	@Deprecated
	public boolean js_getUseSeparateFoundSet()
	{
		return form.getUseSeparateFoundSet();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getNamedFoundSet()
	 * 
	 * @sample
	 *	// form with separate foundset
	 *	var frmSeparate = solutionModel.newForm('products_separate', 'example_data', 'products', null, true, 640, 480);
	 *	frmSeparate.newLabel("Separate FoundSet",10,10,200,20);
	 *	frmSeparate.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 *	frmSeparate.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 *	frmSeparate.namedFoundSet = 'separate';
	 *	forms['products_separate'].controller.find();
	 *	forms['products_separate'].categoryid = '=2';
	 *	forms['products_separate'].controller.search();
	 *	// form with empty foundset
	 *	var frmEmpty = solutionModel.newForm('products_empty', 'example_data', 'products', null, true, 640, 480);
	 *	frmEmpty.newLabel("Empty FoundSet",10,10,200,20);
	 *	frmEmpty.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 *	frmEmpty.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 *	frmEmpty.namedFoundSet = 'empty';
	 */
	public String js_getNamedFoundSet()
	{
		return form.getNamedFoundSet();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getView()
	 *
	 * @sample 
	 * var myForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * myForm.view = JSForm.RECORD_VIEW;
	 * forms['newForm1'].controller.show();
	 */
	public int js_getView()
	{
		return form.getView();
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSPart#js_getHeight()
	 */
	@Deprecated
	public int js_getHeight()
	{
		Iterator<Part> parts = form.getParts();
		while (parts.hasNext())
		{
			Part next = parts.next();
			if (next.getPartType() == Part.BODY)
			{
				return next.getHeight();
			}
		}
		return -1;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getWidth()
	 * 
	 * @sample
	 * 	var myForm = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * 	forms['newForm1'].controller.show();
	 * 	myForm.width = 120;
	 *  forms['newForm1'].controller.recreateUI();
	 */
	public int js_getWidth()
	{
		return form.getWidth();
	}

	public void js_setBorderType(String b)
	{
		checkModification();
		form.setBorderType(b);
	}

	public void js_setDefaultPageFormat(String string)
	{
		checkModification();
		form.setDefaultPageFormat(string);
	}

//	public void js_setExtendsForm(JSForm superForm)
//	{
//		js_setExtendsForm((Object)superForm);
//	}

	public void js_setExtendsForm(Object superForm)
	{
		checkModification();
		Form f = null;
		if (superForm instanceof JSForm)
		{
			f = ((JSForm)superForm).getForm();
		}
		else if (superForm instanceof String)
		{
			f = application.getFlattenedSolution().getForm((String)superForm);
		}
		if (f == null)
		{
			form.setExtendsFormID(0);
		}
		else
		{
//			if (!f.getTableName().equals(form.getTableName()) || !f.getServerName().equals(form.getServerName()))
//			{
//				throw new RuntimeException("Cant set an extends form with table: " + f.getTableName() + " on a form with table : " + form.getTableName());
//			}
			form.setExtendsFormID(f.getID());
		}
	}

	public void js_setInitialSort(String arg)
	{
		checkModification();
		form.setInitialSort(arg);
	}

	public void js_setName(String arg)
	{
		checkModification();
		form.setName(arg);
	}

	public void js_setNavigator(Object navigator)
	{
		checkModification();
		int id = 0;
		if (navigator instanceof JSForm)
		{
			id = ((JSForm)navigator).getForm().getID();
		}
		else if (navigator instanceof String)
		{
			Form f = application.getFlattenedSolution().getForm((String)navigator);
			if (f != null)
			{
				id = f.getID();
			}
		}
		else if (navigator instanceof Number)
		{
			id = ((Number)navigator).intValue();
		}
		form.setNavigatorID(id);
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnDeleteAllRecordsCmd()
	 */
	@Deprecated
	public void js_setOnDeleteAllRecordsCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnDeleteAllRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnDeleteAllRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnDeleteAllRecordsCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnDeleteRecordCmd()
	 */
	@Deprecated
	public void js_setOnDeleteRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnDeleteRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnDeleteRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnDeleteRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnDuplicateRecordCmd()
	 */
	@Deprecated
	public void js_setOnDuplicateRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnDuplicateRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnDuplicateRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnDuplicateRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnFindCmd()
	 */
	@Deprecated
	public void js_setOnFindCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnFindCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnFindCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnFindCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnHide()
	 */
	@Deprecated
	public void js_setOnHideMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnHideMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnHideMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnHideMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnInvertRecordsCmd()
	 */
	@Deprecated
	public void js_setOnInvertRecordsCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnInvertRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnInvertRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnInvertRecordsCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnLoad()
	 */
	@Deprecated
	public void js_setOnLoadMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnLoadMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnLoadMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnLoadMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnNewRecordCmd()
	 */
	@Deprecated
	public void js_setOnNewRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnNewRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnNewRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnNewRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnNextRecordCmd()
	 */
	@Deprecated
	public void js_setOnNextRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnNextRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnNextRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnNextRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnOmitRecordCmd()
	 */
	@Deprecated
	public void js_setOnOmitRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnOmitRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnOmitRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnOmitRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnPreviousRecordCmd()
	 */
	@Deprecated
	public void js_setOnPreviousRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnPreviousRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnPreviousRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnPreviousRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnPrintPreviewCmd()
	 */
	@Deprecated
	public void js_setOnPrintPreviewCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnPrintPreviewCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnPrintPreviewCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnPrintPreviewCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnRecordEditStart()
	 */
	@Deprecated
	public void js_setOnRecordEditStartMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnRecordEditStartMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnRecordEditStartMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnRecordEditStartMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnRecordEditStop()
	 */
	@Deprecated
	public void js_setOnRecordEditStopMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnRecordEditStopMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnRecordEditStopMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnRecordEditStopMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnRecordSelection()
	 */
	@Deprecated
	public void js_setOnRecordSelectionMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnRecordSelectionMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnRecordSelectionMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnRecordSelectionMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnSearchCmd()
	 */
	@Deprecated
	public void js_setOnSearchCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnSearchCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnSearchCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnSearchCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnShowAllRecordsCmd()
	 */
	@Deprecated
	public void js_setOnShowAllRecordsCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnShowAllRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnShowAllRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnShowAllRecordsCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnShow()
	 */
	@Deprecated
	public void js_setOnShowMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnShowMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnShowMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnShowMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnShowOmittedRecordsCmd()
	 */
	@Deprecated
	public void js_setOnShowOmittedRecordsCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnShowOmittedRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnShowOmittedRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnShowOmittedRecordsCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnSortCmd()
	 */
	@Deprecated
	public void js_setOnSortCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnSortCmdMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnSortCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnSortCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#js_getOnUnLoad()
	 */
	@Deprecated
	public void js_setOnUnLoadMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSForm.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				form.setOnUnLoadMethodID(scriptMethod.getID());
			}
			else
			{
				form.setOnUnLoadMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			form.setOnUnLoadMethodID(((Number)functionOrInteger).intValue());
		}
	}

	public void js_setPaperPrintScale(int arg)
	{
		checkModification();
		form.setPaperPrintScale(arg);
	}

	public void js_setRowBGColorCalculation(String arg)
	{
		checkModification();
		form.setRowBGColorCalculation(arg);
	}

	public void js_setScrollbars(int i)
	{
		checkModification();
		form.setScrollbars(i);
	}

	public void js_setServerName(String arg)
	{
		checkModification();
		form.setServerName(arg);
	}

	public void js_setShowInMenu(boolean arg)
	{
		checkModification();
		form.setShowInMenu(arg);
		FormManager formManager = (FormManager)application.getFormManager();
		formManager.removeForm(form);
		formManager.addForm(form, false);
	}

	public void js_setStyleClass(String arg)
	{
		checkModification();
		form.setStyleClass(arg);
	}

	public void js_setStyleName(String arg)
	{
		checkModification();
		form.setStyleName(arg);
	}

	public void js_setTableName(String arg)
	{
		checkModification();
		form.setTableName(arg);
	}

	public void js_setDataSource(String arg)
	{
		// check syntax, do not accept invalid URIs
		if (arg != null)
		{
			try
			{
				new URI(arg);
			}
			catch (URISyntaxException e)
			{
				throw new RuntimeException("Invalid dataSource URI: '" + arg + "' :" + e.getMessage()); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		checkModification();
		form.setDataSource(arg);
		// clear the data provider lookups affected by this change
		FlattenedSolution fs = application.getFlattenedSolution();
		fs.flushDataProviderLookups(form);
	}

	public void js_setTitleText(String string)
	{
		checkModification();
		form.setTitleText(string);
	}

	public void js_setTransparent(boolean arg)
	{
		checkModification();
		form.setTransparent(arg);
	}

	@Deprecated
	public void js_setUseSeparateFoundSet(boolean b)
	{
		checkModification();
		form.setUseSeparateFoundSet(b);
	}

	public void js_setNamedFoundSet(String arg)
	{
		checkModification();
		form.setNamedFoundSet(arg);
	}

	public void js_setView(int arg)
	{
		checkModification();
		form.setView(arg);
	}

	@Deprecated
	public void js_setHeight(int height)
	{
		JSPart part = getPart(Part.BODY);
		if (part != null)
		{
			checkModification();
			part.js_setHeight(height);
		}
	}

	public void js_setWidth(int width)
	{
		checkModification();
		form.setWidth(width);
	}

	/**
	 * @return
	 */
	Form getForm()
	{
		return form;
	}

	private static Form getFormParent(AbstractBase baseComponent)
	{
		if (baseComponent instanceof Form) return (Form)baseComponent;
		ISupportChilds parent = baseComponent.getParent();
		while (!(parent instanceof Form) && parent != null)
		{
			parent = parent.getParent();
		}
		return (Form)parent;
	}

	private static JSForm getJSFormParent(IJSParent parent)
	{
		IJSParent form = parent;
		while (!(form instanceof JSForm) && form != null)
		{
			form = form.getJSParent();
		}
		return (JSForm)form;
	}

	protected JSMethod getEventHandler(String methodKey)
	{
		return getEventHandler(application, form, methodKey, this);
	}

	static <T extends AbstractBase> JSMethod getEventHandler(IApplication application, T persist, String methodKey, IJSParent parent)
	{
		int methodid;
		try
		{
			Method getter = persist.getClass().getMethod("get" + Character.toUpperCase(methodKey.charAt(0)) + methodKey.substring(1)); //$NON-NLS-1$
			methodid = ((Integer)getter.invoke(persist)).intValue();
		}
		catch (Exception e)
		{
			// should not happen
			throw new RuntimeException(e);
		}

		if (methodid > 0)
		{
			ScriptMethod scriptMethod = null;
			if (parent instanceof JSForm)
			{
				scriptMethod = ((JSForm)parent).getForm().getScriptMethod(methodid);
				if (scriptMethod == null)
				{
					Form f = ((JSForm)parent).getForm();
					while (f != null && f.getExtendsFormID() != 0 && scriptMethod == null)
					{
						f = application.getFlattenedSolution().getForm(f.getExtendsFormID());
						if (f != null) scriptMethod = f.getScriptMethod(methodid);
					}
				}
			}
			if (scriptMethod == null)
			{
				scriptMethod = application.getFlattenedSolution().getScriptMethod(methodid);
			}
			if (scriptMethod != null)
			{
				JSForm form = scriptMethod.getParent() instanceof Solution ? null : getJSFormParent(parent);
				List<Object> arguments = persist.getInstanceMethodArguments(methodKey);
				if (arguments == null || arguments.size() == 0)
				{
					return new JSMethod(application, form, scriptMethod, false);
				}
				else
				{
					return new JSMethodWithArguments(application, form, scriptMethod, false, arguments.toArray());
				}
			}
		}
		else if (methodid == 0 && BaseComponent.isCommandProperty(methodKey))
		{
			return DEFAULTS.COMMAND_DEFAULT;
		}
		return null;
	}

	static <T extends AbstractBase> void setEventHandler(IApplication application, T persist, String methodKey, JSMethod method)
	{
		try
		{
			Method setter = persist.getClass().getMethod("set" + Character.toUpperCase(methodKey.charAt(0)) + methodKey.substring(1), int.class); //$NON-NLS-1$
			setter.invoke(persist, new Integer(getMethodId(application, persist, method)));
		}
		catch (Exception e)
		{
			// should not happen
			throw new RuntimeException(e);
		}
		persist.putInstanceMethodArguments(methodKey, method instanceof JSMethodWithArguments ? Arrays.asList(((JSMethodWithArguments)method).getArguments())
			: null);
	}

	/**
	 * Set the event handler for the method key, JSMethod may contain arguments.
	 */
	protected void setEventHandler(String methodKey, JSMethod method)
	{
		checkModification();
		setEventHandler(application, form, methodKey, method);
	}


	static int getMethodId(IApplication application, AbstractBase base, JSMethod method)
	{
		if (method == null) return 0;
		if (method == DEFAULTS.COMMAND_DEFAULT) return 0;
		if (method == DEFAULTS.COMMAND_NONE) return -1;
		return getMethodId(application, base, method.getScriptMethod());
	}

	static int getMethodId(IApplication application, AbstractBase base, ScriptMethod method)
	{
		ISupportChilds parent = method.getParent();

		Form f = getFormParent(base);
		// quick check if it is solution or own form..
		if (parent instanceof Solution || parent.getUUID().equals(f.getUUID()))
		{
			return method.getID();
		}
		else
		{
			// it could be a extends form
			while (f != null && f.getExtendsFormID() != 0)
			{
				f = application.getFlattenedSolution().getForm(f.getExtendsFormID());
				if (parent.getUUID().equals(f.getUUID()))
				{
					return method.getID();
				}
			}
			throw new RuntimeException("Method " + method.getName() + " must be a solution method or a forms own method"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDeleteAllRecordsCmdMethodID()
	 *
	 * @sampleas js_getOnNewRecordCmd()
	 */
	public JSMethod js_getOnDeleteAllRecordsCmd()
	{
		return getEventHandler("onDeleteAllRecordsCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDeleteRecordCmdMethodID()
	 * 
	 * @sampleas js_getOnNewRecordCmd()
	 */
	public JSMethod js_getOnDeleteRecordCmd()
	{
		return getEventHandler("onDeleteRecordCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDragMethodID()
	 * 
	 * @sample
	 * form.onDrag = form.newFormMethod('function onDrag(event) { application.output("onDrag intercepted from " + event.getSource()); }');
	 * form.onDragEnd = form.newFormMethod('function onDragEnd(event) { application.output("onDragEnd intercepted from " + event.getSource()); }');
	 * form.onDragOver = form.newFormMethod('function onDragOver(event) { application.output("onDragOver intercepted from " + event.getSource()); }');
	 * form.onDrop = form.newFormMethod('function onDrop(event) { application.output("onDrop intercepted from " + event.getSource()); }');
	 */
	public JSMethod js_getOnDrag()
	{
		return getEventHandler("onDragMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDragEndMethodID()
	 * 
	 * @sampleas js_getOnDrag()
	 */
	public JSMethod js_getOnDragEnd()
	{
		return getEventHandler("onDragEndMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDragOverMethodID()
	 * 
	 * @sampleas js_getOnDrag()
	 */
	public JSMethod js_getOnDragOver()
	{
		return getEventHandler("onDragOverMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDropMethodID()
	 * 
	 * @sampleas js_getOnDrag()
	 */
	public JSMethod js_getOnDrop()
	{
		return getEventHandler("onDropMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnElementFocusGainedMethodID()
	 * 
	 * @sample
	 * form.onElementFocusGained = form.newFormMethod('function onElementFocusGained(event) { application.output("onElementFocusGained intercepted from " + event.getSource()); }');
	 * form.onElementFocusLost = form.newFormMethod('function onElementFocusLost(event) { application.output("onElementFocusLost intercepted from " + event.getSource()); }');
	 */
	public JSMethod js_getOnElementFocusGained()
	{
		return getEventHandler("onElementFocusGainedMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnElementFocusLostMethodID()
	 * 
	 * @sampleas js_getOnElementFocusGained()
	 */
	public JSMethod js_getOnElementFocusLost()
	{
		return getEventHandler("onElementFocusLostMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDuplicateRecordCmdMethodID()
	 * 
	 * @sampleas js_getOnNewRecordCmd()
	 */
	public JSMethod js_getOnDuplicateRecordCmd()
	{
		return getEventHandler("onDuplicateRecordCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnFindCmdMethodID()
	 * 
	 * @sample
	 * form.onFindCmd = form.newFormMethod('function onFindCmd(event) { application.output("onFindCmd intercepted on " + event.getFormName()); }');
	 * form.onSearchCmd = form.newFormMethod('function onSearchCmd(event) { application.output("onSearchCmd intercepted on " + event.getFormName()); }');
	 * form.onShowAllRecordsCmd = form.newFormMethod('function onShowAllRecordsCmd(event) { application.output("onShowAllRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnFindCmd()
	{
		return getEventHandler("onFindCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnHideMethodID()
	 *
	 * @sampleas js_getOnShow()
	 */
	public JSMethod js_getOnHide()
	{
		return getEventHandler("onHideMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnInvertRecordsCmdMethodID()
	 * 
	 * @sampleas js_getOnOmitRecordCmd()
	 */
	public JSMethod js_getOnInvertRecordsCmd()
	{
		return getEventHandler("onInvertRecordsCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnLoadMethodID()
	 * 
	 * @sample
	 * form.onLoad = form.newFormMethod('function onLoad(event) { application.output("onLoad intercepted on " + event.getFormName()); }');
	 * form.onUnLoad = form.newFormMethod('function onUnLoad(event) { application.output("onUnLoad intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnLoad()
	{
		return getEventHandler("onLoadMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnNewRecordCmdMethodID()
	 * 
	 * @sample
	 * form.onNewRecordCmd = form.newFormMethod('function onNewRecordCmd(event) { application.output("onNewRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDuplicateRecordCmd = form.newFormMethod('function onDuplicateRecordCmd(event) { application.output("onDuplicateRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDeleteRecordCmd = form.newFormMethod('function onDeleteRecordCmd(event) { application.output("onDeleteRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDeleteAllRecordsCmd = form.newFormMethod('function onDeleteAllRecordsCmd(event) { application.output("onDeleteAllRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnNewRecordCmd()
	{
		return getEventHandler("onNewRecordCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnNextRecordCmdMethodID()
	 * 
	 * @sampleas js_getOnPreviousRecordCmd()
	 */
	public JSMethod js_getOnNextRecordCmd()
	{
		return getEventHandler("onNextRecordCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnOmitRecordCmdMethodID()
	 * 
	 * @sample
	 * form.onOmitRecordCmd = form.newFormMethod('function onOmitRecordCmd(event) { application.output("onOmitRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onShowOmittedRecordsCmd = form.newFormMethod('function onShowOmittedRecordsCmd(event) { application.output("onShowOmittedRecordsCmd intercepted on " + event.getFormName()); }');
	 * form.onInvertRecordsCmd = form.newFormMethod('function onInvertRecordsCmd(event) { application.output("onInvertRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnOmitRecordCmd()
	{
		return getEventHandler("onOmitRecordCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnPreviousRecordCmdMethodID()
	 * 
	 * @sample
	 * form.onPreviousRecordCmd = form.newFormMethod('function onPreviousRecordCmd(event) { application.output("onPreviousRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onNextRecordCmd = form.newFormMethod('function onNextRecordCmd(event) { application.output("onNextRecordCmd intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnPreviousRecordCmd()
	{
		return getEventHandler("onPreviousRecordCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnPrintPreviewCmdMethodID()
	 * 
	 * @sample
	 * form.onPrintPreviewCmd = form.newFormMethod('function onPrintPreviewCmd(event) { application.output("onPrintPreviewCmd intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnPrintPreviewCmd()
	{
		return getEventHandler("onPrintPreviewCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRecordEditStartMethodID()
	 * 
	 * @sample
	 * form.onRecordEditStart = form.newFormMethod('function onRecordEditStart(event) { application.output("onRecordEditStart intercepted on " + event.getFormName()); }');
	 * form.onRecordEditStop = form.newFormMethod('function onRecordEditStop(record, event) { application.output("onRecordEditStop intercepted on " + event.getFormName() + ". record is: " + record); }');
	 * form.onRecordSelection = form.newFormMethod('function onRecordSelection(event) { application.output("onRecordSelection intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnRecordEditStart()
	{
		return getEventHandler("onRecordEditStartMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRecordEditStopMethodID()
	 * 
	 * @sampleas js_getOnRecordEditStart()
	 */
	public JSMethod js_getOnRecordEditStop()
	{
		return getEventHandler("onRecordEditStopMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRecordSelectionMethodID()
	 * 
	 * @sampleas js_getOnRecordEditStart()
	 */
	public JSMethod js_getOnRecordSelection()
	{
		return getEventHandler("onRecordSelectionMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnSearchCmdMethodID()
	 * 
	 * @sampleas js_getOnFindCmd()
	 */
	public JSMethod js_getOnSearchCmd()
	{
		return getEventHandler("onSearchCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnShowAllRecordsCmdMethodID()
	 * 
	 * @sampleas js_getOnFindCmd()
	 */
	public JSMethod js_getOnShowAllRecordsCmd()
	{
		return getEventHandler("onShowAllRecordsCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnShowMethodID()
	 * 
	 * @sample
	 * form.onShow = form.newFormMethod('function onShow(firstShow, event) { application.output("onShow intercepted on " + event.getFormName() + ". first show? " + firstShow); return false; }');
	 * form.onHide = form.newFormMethod('function onHide(event) { application.output("onHide blocked on " + event.getFormName()); return false; }');
	 */
	public JSMethod js_getOnShow()
	{
		return getEventHandler("onShowMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnShowOmittedRecordsCmdMethodID()
	 * 
	 * @sampleas js_getOnOmitRecordCmd()
	 */
	public JSMethod js_getOnShowOmittedRecordsCmd()
	{
		return getEventHandler("onShowOmittedRecordsCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnSortCmdMethodID()
	 * 
	 * @sample
	 * form.onSortCmd = form.newFormMethod('function onSortCmd(dataProviderID, asc, event) { application.output("onSortCmd intercepted on " + event.getFormName() + ". data provider: " + dataProviderID + ". asc: " + asc); }');
	 */
	public JSMethod js_getOnSortCmd()
	{
		return getEventHandler("onSortCmdMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnUnLoadMethodID()
	 * 
	 * @sampleas js_getOnLoad()
	 */
	public JSMethod js_getOnUnLoad()
	{
		return getEventHandler("onUnLoadMethodID");
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnResizeMethodID()
	 
	 * @sample
	 * form.onResize = form.newFormMethod('function onResize(event) { application.output("onResize intercepted on " + event.getFormName()); }');
	 */
	public JSMethod js_getOnResize()
	{
		return getEventHandler("onResizeMethodID");
	}

	public void js_setOnDeleteAllRecordsCmd(JSMethod method)
	{
		setEventHandler("onDeleteAllRecordsCmdMethodID", method);
	}

	public void js_setOnDeleteRecordCmd(JSMethod method)
	{
		setEventHandler("onDeleteRecordCmdMethodID", method);
	}

	public void js_setOnDrag(JSMethod method)
	{
		setEventHandler("onDragMethodID", method);
	}

	public void js_setOnDragEnd(JSMethod method)
	{
		setEventHandler("onDragEndMethodID", method);
	}

	public void js_setOnDragOver(JSMethod method)
	{
		setEventHandler("onDragOverMethodID", method);
	}

	public void js_setOnDrop(JSMethod method)
	{
		setEventHandler("onDropMethodID", method);
	}

	public void js_setOnElementFocusGained(JSMethod method)
	{
		setEventHandler("onElementFocusGainedMethodID", method);
	}

	public void js_setOnElementFocusLost(JSMethod method)
	{
		setEventHandler("onElementFocusLostMethodID", method);
	}

	public void js_setOnDuplicateRecordCmd(JSMethod method)
	{
		setEventHandler("onDuplicateRecordCmdMethodID", method);
	}

	public void js_setOnFindCmd(JSMethod method)
	{
		setEventHandler("onFindCmdMethodID", method);
	}

	public void js_setOnHide(JSMethod method)
	{
		setEventHandler("onHideMethodID", method);
	}

	public void js_setOnInvertRecordsCmd(JSMethod method)
	{
		setEventHandler("onInvertRecordsCmdMethodID", method);
	}

	public void js_setOnLoad(JSMethod method)
	{
		setEventHandler("onLoadMethodID", method);
	}

	public void js_setOnNewRecordCmd(JSMethod method)
	{
		setEventHandler("onNewRecordCmdMethodID", method);
	}

	public void js_setOnNextRecordCmd(JSMethod method)
	{
		setEventHandler("onNextRecordCmdMethodID", method);
	}

	public void js_setOnOmitRecordCmd(JSMethod method)
	{
		setEventHandler("onOmitRecordCmdMethodID", method);
	}

	public void js_setOnPreviousRecordCmd(JSMethod method)
	{
		setEventHandler("onPreviousRecordCmdMethodID", method);
	}

	public void js_setOnPrintPreviewCmd(JSMethod method)
	{
		setEventHandler("onPrintPreviewCmdMethodID", method);
	}

	public void js_setOnRecordEditStart(JSMethod method)
	{
		setEventHandler("onRecordEditStartMethodID", method);
	}

	public void js_setOnRecordEditStop(JSMethod method)
	{
		setEventHandler("onRecordEditStopMethodID", method);
	}

	public void js_setOnRecordSelection(JSMethod method)
	{
		setEventHandler("onRecordSelectionMethodID", method);
	}

	public void js_setOnSearchCmd(JSMethod method)
	{
		setEventHandler("onSearchCmdMethodID", method);
	}

	public void js_setOnShowAllRecordsCmd(JSMethod method)
	{
		setEventHandler("onShowAllRecordsCmdMethodID", method);
	}

	public void js_setOnShow(JSMethod method)
	{
		setEventHandler("onShowMethodID", method);
	}

	public void js_setOnShowOmittedRecordsCmd(JSMethod method)
	{
		setEventHandler("onShowOmittedRecordsCmdMethodID", method);
	}

	public void js_setOnSortCmd(JSMethod method)
	{
		setEventHandler("onSortCmdMethodID", method);
	}

	public void js_setOnUnLoad(JSMethod method)
	{
		setEventHandler("onUnLoadMethodID", method);
	}

	public void js_setOnResize(JSMethod method)
	{
		setEventHandler("onResizeMethodID", method);
	}

	/**
	 * @return
	 */
	public IApplication getApplication()
	{
		return application;
	}

}
