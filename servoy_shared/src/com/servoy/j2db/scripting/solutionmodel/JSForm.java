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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMVariable;
import com.servoy.base.solutionmodel.mobile.IMobileSMForm;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.solutionmodel.ISMDefaults;
import com.servoy.j2db.solutionmodel.ISMForm;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSForm")
public class JSForm implements IJSScriptParent<Form>, IConstantsObject, ISMForm, IMobileSMForm
{
	public static ScriptMethod getScriptMethod(Function function, FlattenedSolution fs)
	{
		return fs.getScriptMethod((String)function.get("_scopename_", function), (String)function.get("_methodname_", function));
	}

	protected Form form;
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

			//forms scope still uses the old copy of Script Providers
			Form oldform = form;
			List<FormController> controllers = ((FormManager)application.getFormManager()).getCachedFormControllers(form);
			for (FormController formController : controllers)
			{
				FormScope formScope = formController.getFormScope();
				formScope.updateProviderswithCopy(oldform, form);
			}

			isCopy = true;
		}
		form.setLastModified(System.currentTimeMillis());

		application.getFlattenedSolution().registerChangedForm(form);
	}

	@SuppressWarnings("unchecked")
	public <T extends IScriptProvider> T getScriptCopy(T script)
	{
		checkModification();
		return (T)form.getChild(script.getUUID());
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getJSParent()
	 */
	public IJSParent< ? > getJSParent()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.scripting.solutionmodel.IJSParent#getBaseComponent()
	 */
	public Form getSupportChild()
	{
		return form;
	}

	/**
	 * Creates a new form JSVariable - based on the name of the variable object and the number type, uses the SolutionModel JSVariable constants.
	 *
	 * @sampleas newVariable(String,int,String)
	 *
	 * @param name the specified name of the variable
	 *
	 * @param type the specified type of the variable (see Solution Model -> JSVariable node constants)
	 *
	 * @return a JSVariable object
	 */
	@JSFunction
	public JSVariable newVariable(String name, int type)
	{
		return newVariable(name, type, null);
	}

	/**
	 * Creates a new form JSVariable - based on the name of the variable object , the  type  and it's default value , uses the SolutionModel JSVariable constants.
	 *
	 * This method does not require the form to be destroyed and recreated. Use this method if you want to change the form's model without destroying the runtime form</b>
	 *
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
	@JSFunction
	public JSVariable newVariable(String name, int type, String defaultValue)
	{
		checkModification();
		try
		{
			ScriptVariable variable = form.createNewScriptVariable(new ScriptNameValidator(application.getFlattenedSolution()), name, type);
			variable.setDefaultValue(defaultValue);
			addVariableToScopes(variable);
			return new JSVariable(application, this, variable, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

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
	@JSFunction
	public boolean removeVariable(String name)
	{
		if (name == null) return false;
		checkModification();
		ScriptVariable variable = form.getScriptVariable(name);
		if (variable != null)
		{
			form.removeChild(variable);
			removeVariableFromScopes(variable);
			return true;
		}
		return false;
	}

	/**
	 * @deprecated replaced by newVariable(String, int)
	 */
	@Deprecated
	public JSVariable js_newFormVariable(String name, int type)
	{
		return newVariable(name, type);
	}

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
	 * @return a JSVariable object
	 */
	@JSFunction
	public JSVariable getVariable(String name)
	{
		ScriptVariable variable = application.getFlattenedSolution().getFlattenedForm(form).getScriptVariable(name);
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
	 * var frm = solutionModel.getForm("myForm");
	 * var variables = frm.getVariables();
	 * for (var i in variables)
	 * 	application.output(variables[i].name);
	 *
	 * @param returnInheritedElements boolean true to also return the elements from the parent form
	 * @return an array of all variables on this form
	 *
	 */
	@JSFunction
	public JSVariable[] getVariables(boolean returnInheritedElements)
	{
		List<JSVariable> variables = new ArrayList<JSVariable>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<ScriptVariable> scriptVariables = form2use.getScriptVariables(true);
		while (scriptVariables.hasNext())
		{
			variables.add(new JSVariable(application, this, scriptVariables.next(), false));
		}
		return variables.toArray(new JSVariable[variables.size()]);
	}

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
	@JSFunction
	public JSVariable[] getVariables()
	{
		return getVariables(false);
	}

	/**
	 * @param name the specified name of the variable
	 *
	 * @deprecated replaced by getVariable(String)
	 */
	@Deprecated
	public JSVariable js_getFormVariable(String name)
	{
		return getVariable(name);
	}

	/**
	 * @param returnInheritedElements boolean true to also return the elements from the parent form
	 *
	 * @deprecated replaced by getVariables(boolean)
	 */
	@Deprecated
	public JSVariable[] js_getFormVariables(boolean returnInheritedElements)
	{
		return getVariables(returnInheritedElements);
	}

	/**
	 * @deprecated replaced by getVariables()
	 */
	@Deprecated
	public JSVariable[] js_getFormVariables()
	{
		return getVariables();
	}

	/**
	 * Creates a new form JSMethod - based on the specified code.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var method = form.newMethod('function aMethod(event){application.output("Hello world!");}');
	 * var button = myListViewForm.newButton('Show message!',50,50,100,30,method);
	 * forms['newForm1'].controller.show();
	 *
	 * @param code the specified code for the new method
	 *
	 * @return a new JSMethod object for this form
	 */
	@JSFunction
	public JSMethod newMethod(String code)
	{
		checkModification();
		String name = JSMethod.parseName(code);

		try
		{
			ScriptMethod method = form.createNewScriptMethod(new ScriptNameValidator(application.getFlattenedSolution()), name);
			method.setDeclaration(code);
			refreshFromScopes();//addMethodToScopes(method);
			return new JSMethod(this, method, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

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
	@JSFunction
	public boolean removeMethod(String name)
	{
		if (name == null) return false;
		checkModification();
		ScriptMethod method = form.getScriptMethod(name);
		if (method != null)
		{ // first remove from scopes , then remove from model copy - !important
			//removeMethodFromScopes(method);
			form.removeChild(method);
			refreshFromScopes();
			return true;
		}
		return false;
	}

	/**
	 * @deprecated replaced by newMethod(String)
	 */
	@Deprecated
	public JSMethod js_newFormMethod(String code)
	{
		return newMethod(code);
	}

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
	 * @return a JSMethod object (or null if the method with the specified name does not exist)
	 */
	@JSFunction
	public JSMethod getMethod(String name)
	{
		ScriptMethod sm = application.getFlattenedSolution().getFlattenedForm(form).getScriptMethod(name);
		if (sm != null)
		{
			return new JSMethod(this, sm, application, false);
		}
		return null;
	}

	/**
	 * @param name the specified name of the method
	 *
	 * @deprecated replaced by getMethod(String)
	 */
	@Deprecated
	public JSMethod js_getFormMethod(String name)
	{
		return getMethod(name);
	}

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
	@JSFunction
	public JSMethod[] getMethods(boolean returnInheritedElements)
	{
		List<JSMethod> methods = new ArrayList<JSMethod>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<ScriptMethod> scriptMethods = form2use.getScriptMethods(true);
		while (scriptMethods.hasNext())
		{
			methods.add(new JSMethod(this, scriptMethods.next(), application, false));
		}
		return methods.toArray(new JSMethod[methods.size()]);
	}

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
	@JSFunction
	public JSMethod[] getMethods()
	{
		return getMethods(false);
	}

	/**
	 * @param returnInheritedElements boolean true to also return the elements from the parent form
	 *
	 * @deprecated replaced by getMethods(boolean)
	 */
	@Deprecated
	public JSMethod[] js_getFormMethods(boolean returnInheritedElements)
	{
		return getMethods(returnInheritedElements);
	}

	/**
	 * @deprecated replaced by getMethods()
	 */
	@Deprecated
	public JSMethod[] js_getFormMethods()
	{
		return getMethods();
	}

	/**
	 * Creates a new JSField object on the form - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var variable = form.newVariable('myVar', JSVariable.TEXT);
	 * variable.defaultValue = "'This is a default value (with triple quotes)!'";
	 * var field = form.newField(variable, JSField.TEXT_FIELD, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param type the display type of the JSField object (see the Solution Model -> JSField node for display types)
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
	@JSFunction
	public JSField newField(Object dataprovider, int type, int x, int y, int width, int height)
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
			return JSField.createField(this, field, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	@JSFunction
	public JSField newField(IBaseSMVariable dataprovider, int type, int y)
	{
		return newField(dataprovider, type, 0, y, 10, 10);
	}

	@JSFunction
	public JSField newField(String dataprovider, int type, int y)
	{
		return newField(dataprovider, type, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TEXT_FIELD - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * //choose the dataprovider or jsvariable you want for the Text Field
	 * var x = null;
	 * //global jsvariable as the dataprovider
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form jsvariable as the dataprovider
	 * //x = form.newVariable('myFormVar',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var textField = form.newTextField(x,100,100,200,50);
	 * //or a column data provider as the dataprovider
	 * //textField.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a JSField object with the displayType of TEXT_FIELD
	 */
	@JSFunction
	public JSField newTextField(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.TEXT_FIELD, x, y, width, height);
	}

	@JSFunction
	public JSText newTextField(IBaseSMVariable dataprovider, int y)
	{
		return (JSText)newField(dataprovider, Field.TEXT_FIELD, 0, y, 10, 10);
	}

	@JSFunction
	public JSText newTextField(String dataprovider, int y)
	{
		return (JSText)newField(dataprovider, Field.TEXT_FIELD, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TEXT_AREA - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var globalVar = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * globalVar.defaultValue = "'Type your text in here'";
	 * var textArea = form.newTextArea(globalVar,100,100,300,150);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSTabPanel object in pixels
	 * @param y the vertical "y" position of the JSTabPanel object in pixels
	 * @param width the width of the JSTabPanel object in pixels
	 * @param height the height of the JSTabPanel object in pixels
	 *
	 * @return a JSField object with the displayType of TEXT_AREA
	 */
	@JSFunction
	public JSField newTextArea(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.TEXT_AREA, x, y, width, height);
	}

	@JSFunction
	public JSTextArea newTextArea(IBaseSMVariable dataprovider, int y)
	{
		return (JSTextArea)newField(dataprovider, Field.TEXT_AREA, 0, y, 10, 10);
	}

	@JSFunction
	public JSTextArea newTextArea(String dataprovider, int y)
	{
		return (JSTextArea)newField(dataprovider, Field.TEXT_AREA, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of COMBOBOX - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newComboBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of COMBOBOX
	 */
	@JSFunction
	public JSField newComboBox(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.COMBOBOX, x, y, width, height);
	}

	@JSFunction
	public JSCombobox newCombobox(IBaseSMVariable dataprovider, int y)
	{
		return (JSCombobox)newField(dataprovider, Field.COMBOBOX, 0, y, 10, 10);
	}

	@JSFunction
	public JSCombobox newCombobox(String dataprovider, int y)
	{
		return (JSCombobox)newField(dataprovider, Field.COMBOBOX, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of LISTBOX - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * var list = form.newListBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of LISTBOX
	 */
	@JSFunction
	public JSField newListBox(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.LIST_BOX, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of MULTISELECT_LISTBOX - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * var calendar = form.newMultiSelectListBox(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of MULTISELECT_LISTBOX
	 */
	@JSFunction
	public JSField newMultiSelectListBox(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.MULTISELECT_LISTBOX, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of SPINNER - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * var spinner = form.newSpinner(myDataProvider, 10, 460, 100, 20);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of SPINNER
	 */
	@JSFunction
	public JSField newSpinner(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.SPINNER, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of RADIOS (radio buttons) - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "value1\nvalue2\nvalue3";
	 * var radios = form.newRadios('columnDataProvider',100,100,200,200);
	 * radios.valuelist = vlist;
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a JSField object with the displayType of RADIOS (radio buttons)
	 */
	@JSFunction
	public JSField newRadios(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.RADIOS, x, y, width, height);
	}

	@JSFunction
	public JSRadios newRadios(IBaseSMVariable dataprovider, int y)
	{
		return (JSRadios)newField(dataprovider, Field.RADIOS, 0, y, 10, 10);
	}

	@JSFunction
	public JSRadios newRadios(String dataprovider, int y)
	{
		return (JSRadios)newField(dataprovider, Field.RADIOS, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of CHECK (checkbox) - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newCheck(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of CHECK (checkbox)
	 */
	@JSFunction
	public JSField newCheck(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.CHECKS, x, y, width, height);
	}

	@JSFunction
	public JSChecks newCheck(IBaseSMVariable dataprovider, int y)
	{
		return (JSChecks)newField(dataprovider, Field.CHECKS, 0, y, 10, 10);
	}

	@JSFunction
	public JSChecks newCheck(String dataprovider, int y)
	{
		return (JSChecks)newField(dataprovider, Field.CHECKS, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of CALENDAR - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var calendar = form.newCalendar(myDataProvider, 100, 100, 200, 200);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of CALENDAR
	 */
	@JSFunction
	public JSField newCalendar(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.CALENDAR, x, y, width, height);
	}

	@JSFunction
	public JSCalendar newCalendar(IBaseSMVariable dataprovider, int y)
	{
		return (JSCalendar)newField(dataprovider, Field.CALENDAR, 0, y, 10, 10);
	}

	@JSFunction
	public JSCalendar newCalendar(String dataprovider, int y)
	{
		return (JSCalendar)newField(dataprovider, Field.CALENDAR, 0, y, 10, 10);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of RTF_AREA (enables more than one line of text to be displayed in a field) - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var rtf_area = form.newRtfArea('columnDataProvider',100,100,100,100);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a JSField object with the displayType of RTF_AREA
	 */
	@JSFunction
	public JSField newRtfArea(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.RTF_AREA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of HTML_AREA - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var textProvider = form.newVariable('myVar',JSVariable.TEXT);
	 * textProvider.defaultValue = "'This is a triple quoted text!'";
	 * var htmlArea = myListViewForm.newHtmlArea(textProvider,100,100,100,100);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a JSField object on the form with the displayType of HTML_AREA
	 */
	@JSFunction
	public JSField newHtmlArea(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.HTML_AREA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of IMAGE_MEDIA - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var myMediaVar = form.newVariable("media", JSVariable.MEDIA);
	 * var imageMedia = form.newImageMedia(myMediaVar,100,100,200,200)
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of IMAGE_MEDIA
	 */
	@JSFunction
	public JSField newImageMedia(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.IMAGE_MEDIA, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TYPE_AHEAD - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "value1\nvalue2\nvalue3";
	 * var typeAhead = form.newTypeAhead(columnTextDataProvider,100,100,300,200);
	 * typeAhead.valuelist = vlist;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a JSField object with the displayType of TYPE_AHEAD
	 */
	@JSFunction
	public JSField newTypeAhead(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.TYPE_AHEAD, x, y, width, height);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of PASSWORD - including the dataprovider/JSVariable of the JSField object, the "x" and "y" position of the JSField object in pixels, as well as the width and height of the JSField object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var pass = form.newPassword(scopes.globals.aVariable, 100, 100, 70, 30);
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param x the horizontal "x" position of the JSfield object in pixels
	 * @param y the vertical "y" position of the JSField object in pixels
	 * @param width the width of the JSField object in pixels
	 * @param height the height of the JSField object in pixels
	 *
	 * @return a new JSField object on the form with the displayType of PASSWORD
	 */
	@JSFunction
	public JSField newPassword(Object dataprovider, int x, int y, int width, int height)
	{
		return newField(dataprovider, Field.PASSWORD, x, y, width, height);
	}

	@JSFunction
	public JSPassword newPassword(IBaseSMVariable dataprovider, int y)
	{
		return (JSPassword)newField(dataprovider, Field.PASSWORD, 0, y, 10, 10);
	}

	@JSFunction
	public JSPassword newPassword(String dataprovider, int y)
	{
		return (JSPassword)newField(dataprovider, Field.PASSWORD, 0, y, 10, 10);
	}

	/**
	 * Creates a new button on the form with the given text, place, size and JSMethod as the onAction event triggered action.
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
	 * @return a new JSButton object
	 */
	@JSFunction
	public JSButton newButton(String txt, int x, int y, int width, int height, Object action)
	{
		checkModification();
		try
		{
			GraphicalComponent gc = form.createNewGraphicalComponent(new Point(x, y));
			gc.setSize(new Dimension(width, height));
			gc.setText(txt);
			if (action instanceof JSMethod)
			{
				JSButton button = new JSButton(this, gc, application, true);
				button.setOnAction((JSMethod)action);
				return button;
			}
			else
			{
				int id = getMethodId(action, gc, application);
				gc.setOnActionMethodID(id);
				return new JSButton(this, gc, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	@JSFunction
	public JSButton newButton(String txt, int y, IBaseSMMethod jsmethod)
	{
		return newButton(txt, 0, y, 10, 10, jsmethod);
	}

	/**
	 * Creates a new JSLabel object on the form - including the text of the label, the "x" and "y" position of the label object in pixels, the width and height of the label object in pixels.
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
	 * @return a JSLabel object
	 */
	@JSFunction
	public JSLabel newLabel(String txt, int x, int y, int width, int height)
	{
		return newLabel(txt, x, y, width, height, null);
	}

	/**
	 * Creates a new JSLabel object on the form - including the text of the label, the "x" and "y" position of the label object in pixels, the width and height of the label object in pixels and a JSMethod action such as the method for an onAction event.
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
	 * @param action the event action JSMethod of the label object
	 *
	 * @return a JSLabel object
	 */
	@JSFunction
	public JSLabel newLabel(String txt, int x, int y, int width, int height, Object action)
	{
		checkModification();
		try
		{
			GraphicalComponent gc = form.createNewGraphicalComponent(new Point(x, y));
			gc.setSize(new Dimension(width, height));
			gc.setText(txt);
			if (action instanceof JSMethod)
			{
				JSLabel label = new JSLabel(this, gc, application, true);
				label.setOnAction((JSMethod)action);
				return label;
			}
			else
			{
				int methodId = getMethodId(action, gc, application);
				if (methodId > 0)
				{
					gc.setOnActionMethodID(methodId);
					gc.setShowClick(false);
				}
				return new JSLabel(this, gc, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	@JSFunction
	public JSLabel newLabel(String txt, int y)
	{
		return newLabel(txt, 0, y, 10, 10, null);
	}

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
				methodId = getMethodId(application, gc, (JSMethod)action, null);
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
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/table1','db:/server2/table2',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('another_parent_table_id', '=', 'another_child_table_parent_id');
	 * var portal = form.newPortal('portal',relation,200,200,300,300);
	 * portal.newField('someColumn',JSField.TEXT_FIELD,200,200,120);
	 * forms['newForm1'].controller.show();
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
	@JSFunction
	public JSPortal newPortal(String name, Object relation, int x, int y, int width, int height)
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
				relationName = ((JSRelation)relation).getName();
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
	 * var frm = solutionModel.getForm("myForm");
	 * var portal = frm.getPortal("myPortal");
	 * portal.initialSort = 'my_table_text desc';
	 *
	 * @param name the specified name of the portal
	 *
	 * @return a JSPortal object
	 */
	@JSFunction
	public JSPortal getPortal(String name)
	{
		if (name == null) return null;
		Iterator<Portal> portals = application.getFlattenedSolution().getFlattenedForm(form).getPortals();
		while (portals.hasNext())
		{
			Portal portal = portals.next();
			if (name.equals(portal.getName()))
			{
				return new JSPortal(this, portal, application, false);
			}
		}
		return null;
	}

	/**
	 * Removes a JSPortal that has the given name. Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,800,600);
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/myTable','db:/server1/myOtherTable',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * var jsportal = form.newPortal('jsp',relation,100,400,300,300);
	 * jsportal.newField('child_table_id',JSField.TEXT_FIELD,200,200,120);
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jsp') == true) application.output('Portal removed ok'); else application.output('Portal could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the portal',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSPortal to be removed
	 *
	 * @return true if the JSPortal has successfully been removed; false otherwise
	 */
	@JSFunction
	public boolean removePortal(String name)
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
	 * Returns all JSPortal objects of this form (optionally also the ones from the parent form), including the ones without a name.
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
	 * @return an array of all JSPortal objects on this form
	 *
	 */
	@JSFunction
	public JSPortal[] getPortals(boolean returnInheritedElements)
	{
		List<JSPortal> portals = new ArrayList<JSPortal>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<Portal> iterator = form2use.getPortals();
		while (iterator.hasNext())
		{
			portals.add(new JSPortal(this, iterator.next(), application, false));
		}
		return portals.toArray(new JSPortal[portals.size()]);
	}

	/**
	 * Returns all JSPortal objects of this form (not including the ones from the parent form), including the ones without a name.
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
	 * @return an array of all JSPortal objects on this form
	 *
	 */
	@JSFunction
	public JSPortal[] getPortals()
	{
		return getPortals(false);
	}

	/**
	 * Creates a new JSTabPanel object on the form - including the name of the JSTabPanel object, the "x" and "y" position of the JSTabPanel object in pixels, as well as the width and height of the JSTabPanel object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('parentForm','db:/server1/parent_table',null,false,640,480);
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD,10,10,100,20);
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * var childTwo = solutionModel.newForm('childTwo','db:/server1/my_table',null,false,400,300);
	 * childTwo.newField('my_table_image', JSField.IMAGE_MEDIA,10,10,100,100);
	 * var tabPanel = form.newTabPanel('tabs',10,10,620,460);
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * forms['parentForm'].controller.show();
	 *
	 * @param name the specified name of the JSTabPanel object
	 * @param x the horizontal "x" position of the JSTabPanel object in pixels
	 * @param y the vertical "y" position of the JSTabPanel object in pixels
	 * @param width the width of the JSTabPanel object in pixels
	 * @param height the height of the JSTabPanel object in pixels
	 *
	 * @return a JSTabPanel object
	 */
	@JSFunction
	public JSTabPanel newTabPanel(String name, int x, int y, int width, int height)
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
	 * var frm = solutionModel.getForm("myForm");
	 * var tabPanel = frm.getTabPanel("myTabPanel");
	 * var tabs = tabPanel.getTabs();
	 * for (var i=0; i<tabs.length; i++)
	 *	application.output("Tab " + i + " has text " + tabs[i].text);
	 *
	 * @param name the specified name of the tabpanel
	 *
	 * @return a JSTabPanel object
	 */
	@JSFunction
	public JSTabPanel getTabPanel(String name)
	{
		if (name == null) return null;
		Iterator<TabPanel> tabPanels = application.getFlattenedSolution().getFlattenedForm(form).getTabPanels();
		while (tabPanels.hasNext())
		{
			TabPanel tabPanel = tabPanels.next();
			if (name.equals(tabPanel.getName()))
			{
				return new JSTabPanel(this, tabPanel, application, false);
			}
		}
		return null;
	}

	/**
	 * Removes a JSTabPanel that has the given name. Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX','db:/server1/parent_table',null,false,800,600);
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD,10,10,100,20);
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_id');
	 * var childTwo = solutionModel.newForm('childTwo','db:/server1/another_table',null,false,400,300);
	 * childTwo.newField('columnDataProvider', JSField.TEXT_FIELD,10,10,100,100);
	 * var tabPanel = form.newTabPanel('jst',10,10,620,460);
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jst') == true)\n application.output('TabPanel has been removed ok');\n else\n application.output('TabPanel could not be deleted');\n forms['newFormX'].controller.recreateUI();\n}");
	 * var removerButton = form.newButton('Click here to remove the tab panel',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSTabPanel to be removed
	 *
	 * @return true is the JSTabPanel has been successfully removed, false otherwise
	 */
	@JSFunction
	public boolean removeTabPanel(String name)
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
	 * Returns all JSTabPanels of this form (optionally the ones from the parent form), including the ones without a name.
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
	 * @return an array of all JSTabPanel objects on this form
	 *
	 */
	@JSFunction
	public JSTabPanel[] getTabPanels(boolean returnInheritedElements)
	{
		List<JSTabPanel> tabPanels = new ArrayList<JSTabPanel>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<TabPanel> iterator = form2use.getTabPanels();
		while (iterator.hasNext())
		{
			tabPanels.add(new JSTabPanel(this, iterator.next(), application, false));
		}
		return tabPanels.toArray(new JSTabPanel[tabPanels.size()]);
	}

	/**
	 * Returns all JSTabPanels of this form (not including the ones from the parent form), including the ones without a name.
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
	 * @return an array of all JSTabPanel objects on this form
	 *
	 */
	@JSFunction
	public JSTabPanel[] getTabPanels()
	{
		return getTabPanels(false);
	}

	private JSPart[] getPartsInternal(int partType)
	{
		ArrayList<JSPart> lst = new ArrayList<JSPart>();
		Iterator<Part> parts = form.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == partType)
			{
				lst.add(JSPart.createPart(this, part, false));
			}
		}
		return lst.toArray(new JSPart[lst.size()]);
	}

	private JSPart getPartInternal(int partType)
	{
		return getPartInternal(partType, -1);
	}

	private JSPart getPartInternal(int partType, int height)
	{
		Iterator<Part> parts = application.getFlattenedSolution().getFlattenedForm(form).getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == partType && (height == -1 || part.getHeight() == height))
			{
				return JSPart.createPart(this, part, false);
			}
		}
		return null;
	}

	private boolean testExtendFormForPart(int partType, int height)
	{
		Form superForm = application.getFlattenedSolution().getForm(form.getExtendsID());
		if (superForm != null)
		{
			Iterator<Part> superParts = application.getFlattenedSolution().getFlattenedForm(superForm).getParts();
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
		return false;
	}

	protected JSPart getOrCreatePart(int partType, int height)
	{
		checkModification();
		JSPart part;
		if (partType == Part.LEADING_SUBSUMMARY || partType == Part.TRAILING_SUBSUMMARY)
		{
			part = getPartInternal(partType, height);
		}
		else
		{
			part = getPartInternal(partType);
		}
		if (part == null)
		{
			if (testExtendFormForPart(partType, height))
			{
				throw new RuntimeException("Super form already has this part"); //$NON-NLS-1$
			}
			try
			{
				part = JSPart.createPart(this, form.createNewPart(partType, height), false);
				int testHeight = 0;
				Iterator<Part> parts = form.getParts();
				while (parts.hasNext())
				{
					Part p = parts.next();
					int test = p.getHeight();
					if (test < testHeight)
					{
						throw new RuntimeException("Illegal lowerbound " + height + " for the part " + Part.getDisplayName(partType) + //$NON-NLS-1$//$NON-NLS-2$
							" it must be greater than the previous part lowerbound " + testHeight); //$NON-NLS-1$
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
			part.setHeight(height);
		}
		return part;
	}


	/**
	 * Creates a new part on the form. The type of the new part (use one of the JSPart constants)
	 * and its height must be specified.
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm', 'db:/example_data/my_table', null, false, 1200, 800);
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
	@JSFunction
	public JSPart newPart(int type, int height)
	{
		return getOrCreatePart(type, height);
	}


	/**
	 * Creates a new Title Header part on the form.
	 *
	 * @sample
	 * var titleHeader = form.newTitleHeaderPart(40);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Title Header form part.
	 */
	@JSFunction
	public JSPart newTitleHeaderPart(int height)
	{
		return getOrCreatePart(Part.TITLE_HEADER, height);
	}

	/**
	 * Creates a new Header part on the form.
	 *
	 * @sample
	 * var header = form.newHeaderPart(80);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Header form part.
	 */
	@JSFunction
	public JSPart newHeaderPart(int height)
	{
		return getOrCreatePart(Part.HEADER, height);
	}

	@JSFunction
	public JSHeader newHeader()
	{
		return null; // mobile only
	}

	@JSFunction
	public JSHeader getHeader()
	{
		return null; // mobile only
	}

	@JSFunction
	public JSInsetList newInsetList(int yLocation, String relationName, String headerText, String textDataProviderID)
	{
		return null; // mobile only
	}

	@Override
	@JSFunction
	public JSInsetList getInsetList(String name)
	{
		return null; // mobile only
	}

	@Override
	@JSFunction
	public JSInsetList[] getInsetLists()
	{
		return null; // mobile only
	}

	@Override
	@JSFunction
	public boolean removeInsetList(String name)
	{
		return false; // mobile only
	}

	@Override
	public void setComponentOrder(IBaseSMComponent[] components)
	{
		// mobile only
	}

	@JSFunction
	public boolean removeHeader()
	{
		return removePart(true);
	}

	private boolean removePart(boolean header)
	{
		checkModification();
		Iterator<Part> parts = form.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if ((header && PersistUtils.isHeaderPart(part.getPartType())) || (!header && PersistUtils.isFooterPart(part.getPartType())))
			{
				form.removeChild(part);
				return true;
			}
		}
		return false;
	}


	/**
	 * Creates a new Leading Grand Summary part on the form.
	 *
	 * @sample
	 * var leadingGrandSummary = form.newLeadingGrandSummaryPart(120);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Leading Grand Summary form part.
	 */
	@JSFunction
	public JSPart newLeadingGrandSummaryPart(int height)
	{
		return getOrCreatePart(Part.LEADING_GRAND_SUMMARY, height);
	}

	/**
	 * Creates a new Leading Subsummary part on the form.
	 *
	 * @sample
	 * var leadingSubsummary = form.newLeadingSubSummaryPart(160);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Leading Subsummary form part.
	 */
	@JSFunction
	public JSPart newLeadingSubSummaryPart(int height)
	{
		return getOrCreatePart(Part.LEADING_SUBSUMMARY, height);
	}


	/**
	 * Creates a new Trailing Subsummary part on the form.
	 *
	 * @sample
	 * var trailingSubsummary = form.newTrailingSubSummaryPart(360);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Trailing Subsummary form part.
	 */
	@JSFunction
	public JSPart newTrailingSubSummaryPart(int height)
	{
		return getOrCreatePart(Part.TRAILING_SUBSUMMARY, height);
	}

	/**
	 * Creates a new Trailing Grand Summary part on the form.
	 *
	 * @sample
	 * var trailingGrandSummary = form.newTrailingGrandSummaryPart(400);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Trailing Grand Summary form part.
	 */
	@JSFunction
	public JSPart newTrailingGrandSummaryPart(int height)
	{
		return getOrCreatePart(Part.TRAILING_GRAND_SUMMARY, height);
	}

	/**
	 * Creates a new Footer part on the form.
	 *
	 * @sample
	 * var footer = form.newFooterPart(440);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSFooter instance corresponding to the newly created Footer form part.
	 */
	@JSFunction
	public JSPart newFooterPart(int height)
	{
		return getOrCreatePart(Part.FOOTER, height);
	}

	@JSFunction
	public JSFooter newFooter()
	{
		return null; // mobile only
	}

	@JSFunction
	public JSFooter getFooter()
	{
		return null; // mobile only
	}

	@JSFunction
	public boolean removeFooter()
	{
		return removePart(false);
	}

	/**
	 * Creates a new Title Footer part on the form.
	 *
	 * @sample
	 * var titleFooter = form.newTitleFooterPart(500);
	 *
	 * @param height The height of the new part
	 *
	 * @return A JSPart instance corresponding to the newly created Title Footer form part.
	 */
	@JSFunction
	public JSPart newTitleFooterPart(int height)
	{
		return getOrCreatePart(Part.TITLE_FOOTER, height);
	}

	/**
	 * Gets all the parts from the form (optionally also from the parent form), ordered by there height (lowerbound) property, from top == 0 to bottom.
	 *
	 * @sample
	 * var allParts = form.getParts()
	 * for (var i=0; i<allParts.length; i++) {
	 *	if (allParts[i].getPartType() == JSPart.BODY)
	 *		application.output('body Y offset: ' + allParts[i].getPartYOffset());
	 * }
	 *
	 * @param returnInheritedElements boolean true to also return the parts from parent form
	 * @return An array of JSPart instances corresponding to the parts of the form.
	 */
	@JSFunction
	public JSPart[] getParts(boolean returnInheritedElements)
	{
		List<JSPart> lst = new ArrayList<JSPart>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<Part> parts = form2use.getParts();
		while (parts.hasNext())
		{
			lst.add(JSPart.createPart(this, parts.next(), false));
		}
		return lst.toArray(new JSPart[lst.size()]);
	}

	/**
	 * Gets all the parts from the form (not including the parts of the parent form), ordered by there height (lowerbound) property, from top == 0 to bottom.
	 *
	 * @sampleas com.servoy.j2db.solutionmodel.ISMPart#getPartYOffset()
	 *
	 * @return An array of JSPart instances corresponding to the parts of the form.
	 */
	@JSFunction
	public JSPart[] getParts()
	{
		return getParts(false);
	}


	/**
	 * Gets a part of the form from the given type (see JSPart constants).
	 *
	 * @sample
	 * form.getPart(JSPart.HEADER).background = 'red';
	 * form.getPart(JSPart.LEADING_SUBSUMMARY, 160).background = 'red';
	 *
	 * @param type The type of the part to retrieve.
	 *
	 * @return A JSPart instance representing the retrieved form part.
	 */
	@JSFunction
	public JSPart getPart(int type)
	{
		return getPartInternal(type);
	}

	/**
	 * Gets a part of the form from the given type (see JSPart constants).
	 * Use the height if you want to get a specific LEADING_SUBSUMMARY or TRAILING_SUBSUMMARY.
	 *
	 * @sample
	 * form.getPart(JSPart.HEADER).background = 'red';
	 * form.getPart(JSPart.LEADING_SUBSUMMARY, 160).background = 'red';
	 *
	 * @param type The type of the part to retrieve.
	 *
	 * @param height The height of the part to retrieve. Use this parameter when retrieving one of multiple
	 * 	                      Leading/Trailing Subsummary parts.
	 *
	 * @return A JSPart instance representing the retrieved form part.
	 */
	@JSFunction
	public JSPart getPart(int type, int height)
	{
		return getPartInternal(type, height);
	}

	/**
	 * Returns the Y offset of a given part (see JSPart) of the form. This will include
	 * all the super forms parts if this form extends a form.
	 *
	 * @sample
	 * // get the subform
	 * var form = solutionModel.getForm('SubForm');
	 * // get the start offset of the body
	 * var height = form.getPartYOffset(JSPart.BODY);
	 * // place a new button based on the start offset.
	 * form.newButton('mybutton',50,50+height,80,20,solutionModel.getGlobalMethod('globals', 'test'));
	 *
	 * @param type The type of the part whose Y offset will be returned.
	 *
	 * @return A number holding the Y offset of the specified form part.
	 */
	@JSFunction
	public int getPartYOffset(int type)
	{
		return getPartYOffset(type, -1);
	}

	/**
	 * Returns the Y offset of a given part (see JSPart) of the form. This will include
	 * all the super forms parts if this form extends a form. Use the height parameter for
	 * targetting one of multiple subsummary parts.
	 *
	 * @sample
	 * // get the subform
	 * var form = solutionModel.getForm('SubForm');
	 * // get the start offset of the body
	 * var height = form.getPartYOffset(JSPart.BODY);
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
	@JSFunction
	public int getPartYOffset(int type, int height)
	{
		Form ff = application.getFlattenedSolution().getFlattenedForm(form);

		Iterator<Part> parts = ff.getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == type && (height == -1 || part.getHeight() == height))
			{
				return ff.getPartStartYPos(part.getID());
			}
		}
		return -1;
	}

	/**
	 * Removes a JSPart of the given type.
	 *
	 * @sample
	 * form.removePart(JSPart.HEADER);
	 * form.removePart(JSPart.LEADING_SUBSUMMARY, 160);
	 *
	 * @param type The type of the part that should be removed.
	 *
	 * @return True if the part is successfully removed, false otherwise.
	 */
	@JSFunction
	public boolean removePart(int type)
	{
		return removePart(type, -1);
	}

	/**
	 * Removes a JSPart of the given type. The height parameter is for removing one of multiple subsummary parts.
	 *
	 * @sample
	 * form.removePart(JSPart.HEADER);
	 * form.removePart(JSPart.LEADING_SUBSUMMARY, 160);
	 *
	 * @param type The type of the part that should be removed.
	 *
	 * @param height The height of the part that should be removed. This parameter is for
	 * 					removing one of multiple Leading/Trailing Subsummary parts.
	 *
	 * @return True if the part is successfully removed, false otherwise.
	 */
	@JSFunction
	public boolean removePart(int type, int height)
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
	@JSFunction
	public JSPart getBodyPart()
	{
		return getPartInternal(Part.BODY);
	}

	/**
	 * Retrieves the Title Header part of the form.
	 *
	 * @sample
	 * form.getTitleHeaderPart().background = 'red';
	 *
	 * @return A JSPart instance corresponding to the Title Header part of the form.
	 */
	@JSFunction
	public JSPart getTitleHeaderPart()
	{
		return getPartInternal(Part.TITLE_HEADER);
	}

	/**
	 * Retrieves the Header part of the form.
	 *
	 * @sample
	 * form.getHeaderPart().background = 'orange';
	 *
	 * @return A JSPart instance corresponding to the Header part of the form.
	 */
	@JSFunction
	public JSPart getHeaderPart()
	{
		return getPartInternal(Part.HEADER);
	}

	/**
	 * Retrieves the Leading Grand Summary part of the form.
	 *
	 * @sample
	 * form.getLeadingGrandSummaryPart().background = 'yellow';
	 *
	 * @return A JSPart instance corresponding to the Leading Grand Summary part of the form.
	 */
	@JSFunction
	public JSPart getLeadingGrandSummaryPart()
	{
		return getPartInternal(Part.LEADING_GRAND_SUMMARY);
	}

	/**
	 * @deprecated see getLeadingSubSummaryParts()
	 */
	@Deprecated
	public JSPart js_getLeadingSubSummaryPart()
	{
		return getPartInternal(Part.LEADING_SUBSUMMARY);
	}

	/**
	 * Gets an array of the Leading Subsummary parts of the form, ordered by their height from top == 0 to bottom.
	 *
	 * @sample
	 * form.getLeadingSubSummaryParts()[0].background = 'green';
	 *
	 * @return An array of JSPart instances corresponding to the Leading Subsummary parts of the form.
	 */
	@JSFunction
	public JSPart[] getLeadingSubSummaryParts()
	{
		return getPartsInternal(Part.LEADING_SUBSUMMARY);
	}

	/**
	 * @deprecated getTrailingSubSummaryParts()
	 */
	@Deprecated
	public JSPart js_getTrailingSubSummaryPart()
	{
		return getPartInternal(Part.TRAILING_SUBSUMMARY);
	}

	/**
	 * Gets an array of the Trailing Subsummary parts of the form, ordered by their height from top == 0 to bottom.
	 *
	 * @sample
	 * form.getTrailingSubSummaryParts()[0].background = 'green';
	 *
	 * @return An array of JSPart instances corresponding to the Trailing Subsummary parts of the form.
	 */
	@JSFunction
	public JSPart[] getTrailingSubSummaryParts()
	{
		return getPartsInternal(Part.TRAILING_SUBSUMMARY);
	}

	/**
	 * Retrieves the Trailing Grand Summary part of the form.
	 *
	 * @sample
	 * form.getTrailingGrandSummaryPart().background = 'yellow';
	 *
	 * @return A JSPart instance corresponding to the Trailing Grand Summary part of the form.
	 */
	@JSFunction
	public JSPart getTrailingGrandSummaryPart()
	{
		return getPartInternal(Part.TRAILING_GRAND_SUMMARY);
	}

	/**
	 * Retrieves the Footer part of the form.
	 *
	 * @sample
	 * form.getFooterPart().background = 'magenta';
	 *
	 * @return A JSPart instance corresponding to the Footer part of the form.
	 */
	@JSFunction
	public JSPart getFooterPart()
	{
		return getPartInternal(Part.FOOTER);
	}

	/**
	 * Retrieves the Title Footer part of the form.
	 *
	 * @sample
	 * form.getTitleFooterPart().background = 'gray';
	 *
	 * @return A JSPart instance corresponding to the Title Footer part of the form.
	 */
	@JSFunction
	public JSPart getTitleFooterPart()
	{
		return getPartInternal(Part.TITLE_FOOTER);
	}

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
	 * @return a JSField object
	 */
	@JSFunction
	public JSField getField(String name)
	{
		if (name == null) return null;

		Iterator<Field> fields = application.getFlattenedSolution().getFlattenedForm(form).getFields();
		while (fields.hasNext())
		{
			Field field = fields.next();
			if (name.equals(field.getName()))
			{
				return JSField.createField(this, field, application, false);
			}
		}
		return null;
	}

	/**
	 * Removes a JSField that has the given name. Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,800,600);
	 * var jsfield = form.newField(scopes.globals.myGlobalVariable,JSField.TEXT_FIELD,100,300,200,50);
	 * jsfield.name = 'jsf';
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if (form.removeComponent('jsf') == true) application.output('Field has been removed ok'); else application.output('Field could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the field',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSField to remove
	 *
	 * @return true is the JSField has been successfully removed; false otherwise
	 */
	@JSFunction
	public boolean removeField(String name)
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
	 * @return all JSField objects of this form
	 *
	 */
	@JSFunction
	public JSField[] getFields(boolean returnInheritedElements)
	{
		List<JSField> fields = new ArrayList<JSField>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<Field> iterator = form2use.getFields();
		while (iterator.hasNext())
		{
			fields.add(JSField.createField(this, iterator.next(), application, false));
		}
		return fields.toArray(new JSField[fields.size()]);
	}

	/**
	 * Returns all JSField objects of this form, including the ones without a name.
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
	 * @return all JSField objects of this form
	 *
	 */
	@JSFunction
	public JSField[] getFields()
	{
		return getFields(false);
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
	@JSFunction
	public JSButton getButton(String name)
	{
		if (name == null) return null;

		Iterator<GraphicalComponent> graphicalComponents = application.getFlattenedSolution().getFlattenedForm(form).getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && ComponentFactory.isButton(button))
			{
				return new JSButton(this, button, application, false);
			}
		}
		return null;
	}

	/**
	 * Removes a JSButton that has the specified name. Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,800,600);
	 * var b1 = form.newButton('This is button1',100,100,200,50,null);
	 * b1.name = 'b1';
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX'); if (form.removeButton('b1') == true) application.output('Button has been removed ok'); else application.output('Button could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var b2 = form.newButton('Click here to remove button1',100,230,200,50,jsmethod);
	 * b2.name = 'b2';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSButton to be removed
	 *
	 * @return true if the JSButton has been removed; false otherwise
	 */
	@JSFunction
	public boolean removeButton(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && ComponentFactory.isButton(button))
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
	 * 	if (buttons[b].name != null)
	 * 		application.output(buttons[b].name);
	 * 	else
	 * 		application.output(buttons[b].text + " has no name ");
	 * }
	 *
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return the list of all JSbuttons on this forms
	 *
	 */
	@JSFunction
	public JSButton[] getButtons(boolean returnInheritedElements)
	{
		List<JSButton> buttons = new ArrayList<JSButton>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<GraphicalComponent> graphicalComponents = form2use.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (ComponentFactory.isButton(button))
			{
				buttons.add(new JSButton(this, button, application, false));
			}
		}
		return buttons.toArray(new JSButton[buttons.size()]);
	}

	/**
	 * Returns all JSButtons of this form, including the ones without a name.
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
	 * @return the list of all JSbuttons on this forms
	 *
	 */
	@JSFunction
	public JSButton[] getButtons()
	{
		return getButtons(false);
	}

	/**
	 * Creates a new JSBean object on the form - including the name of the JSBean object; the classname the JSBean object is based on, the "x" and "y" position of the JSBean object in pixels, as well as the width and height of the JSBean object in pixels.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var bean = form.newBean('bean','com.servoy.extensions.beans.dbtreeview.DBTreeView',200,200,300,300);
	 * forms['newForm1'].controller.show();
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
	@JSFunction
	public JSBean newBean(String name, String className, int x, int y, int width, int height)
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

	@JSFunction
	@Override
	public JSBean newBean(String name, int y)
	{
		return null; // only in mobile
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
	@JSFunction
	@Override
	public JSBean getBean(String name)
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
	 * var form = solutionModel.getForm('myform');
	 * form.removeBean('mybean')
	 *
	 * @param name the specified name of the JSBean to be removed
	 *
	 * @return true if the JSBean has been removed; false otherwise
	 */
	@JSFunction
	@Override
	public boolean removeBean(String name)
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
	 * 	if (beans[b].name != null)
	 * 		application.output(beans[b].name);
	 * }
	 *
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return the list of all JSbuttons on this forms
	 *
	 */
	@JSFunction
	@Override
	public JSBean[] getBeans(boolean returnInheritedElements)
	{
		List<JSBean> beans = new ArrayList<JSBean>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<Bean> iterator = form2use.getBeans();
		while (iterator.hasNext())
		{
			beans.add(new JSBean(this, iterator.next(), false));
		}
		return beans.toArray(new JSBean[beans.size()]);
	}

	/**
	 * Returns all JSBeans of this form.
	 *
	 * @sample
	 * var beans = myForm.getBeans();
	 * for (var b in beans)
	 * {
	 * 	if (beans[b].name != null)
	 * 		application.output(beans[b].name);
	 * }
	 *
	 * @return the list of all JSbuttons on this forms
	 *
	 */
	@JSFunction
	@Override
	public JSBean[] getBeans()
	{
		return getBeans(false);
	}

	/**
	 * Returns a JSComponent that has the given name; if found it will be a JSField, JSLabel, JSButton, JSPortal, JSBean or JSTabPanel.
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var cmp = frm.getComponent("componentName");
	 * application.output("Component type and name: " + cmp);
	 *
	 * @param name the specified name of the component
	 *
	 * @return a JSComponent object (might be a JSField, JSLabel, JSButton, JSPortal, JSBean or JSTabPanel)
	 */
	@JSFunction
	public JSComponent< ? > getComponent(String name)
	{
		JSComponent< ? > comp = getLabel(name);
		if (comp != null) return comp;
		comp = getButton(name);
		if (comp != null) return comp;
		comp = getField(name);
		if (comp != null) return comp;
		comp = getPortal(name);
		if (comp != null) return comp;
		comp = getTabPanel(name);
		if (comp != null) return comp;
		comp = getBean(name);
		if (comp != null) return comp;
		return null;
	}

	/**
	 * Removes a component (JSLabel, JSButton, JSField, JSPortal, JSBean, JSTabpanel) that has the given name. It is the same as calling "if(!removeLabel(name) &amp;&amp; !removeButton(name) ....)".
	 * Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newFormX','db:/server1/parent_table',null,true,1000,750);
	 * var jsbutton = form.newButton('JSButton to delete',100,100,200,50,null);
	 * jsbutton.name = 'jsb';
	 * var jslabel = form.newLabel('JSLabel to delete',100,200,200,50,null);
	 * jslabel.name = 'jsl';
	 * jslabel.transparent = false;
	 * jslabel.background = 'green';
	 * var jsfield = form.newField('scopes.globals.myGlobalVariable',JSField.TEXT_FIELD,100,300,200,50);
	 * jsfield.name = 'jsf';
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('parent_table_id', '=', 'child_table_id');
	 * var jsportal = form.newPortal('jsp',relation,100,400,300,300);
	 * jsportal.newField('child_table_id',JSField.TEXT_FIELD,200,200,120);
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_id', JSField.TEXT_FIELD,10,10,100,20);
	 * var childTwo = solutionModel.newForm('childTwo','server1','other_table',null,false,400,300);
	 * childTwo.newField('some_table_id', JSField.TEXT_FIELD,10,10,100,100);
	 * var jstabpanel = form.newTabPanel('jst',450,30,620,460);
	 * jstabpanel.newTab('tab1','Child One',childOne,relation);
	 * jstabpanel.newTab('tab2','Child Two',childTwo);
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX');\n if ((form.removeComponent('jsb') == true) && (form.removeComponent('jsl') == true) && (form.removeComponent('jsf') == true) && (form.removeComponent('jsp') == true) & (form.removeComponent('jst') == true)) application.output('Components removed ok'); else application.output('Some component(s) could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove form components',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the component to be deleted
	 *
	 * @return true if component has been successfully deleted; false otherwise
	 */
	@JSFunction
	public boolean removeComponent(String name)
	{
		if (name == null) return false;
		if (removeLabel(name)) return true;
		if (removeButton(name)) return true;
		if (removeField(name)) return true;
		if (removePortal(name)) return true;
		if (removeTabPanel(name)) return true;
		if (removeBean(name)) return true;
		return false;
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean or JSTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]);
	 *
	 * @param returnInheritedElements boolean true to also return the elements from the parent form
	 * @return an array of all the JSComponents on the form.
	 */
	@JSFunction
	public JSComponent< ? >[] getComponents(boolean returnInheritedElements)
	{
		List<JSComponent< ? >> lst = new ArrayList<JSComponent< ? >>();
		lst.addAll(Arrays.asList(getLabels(returnInheritedElements)));
		lst.addAll(Arrays.asList(getButtons(returnInheritedElements)));
		lst.addAll(Arrays.asList(getFields(returnInheritedElements)));
		lst.addAll(Arrays.asList(getPortals(returnInheritedElements)));
		lst.addAll(Arrays.asList(getBeans(returnInheritedElements)));
		lst.addAll(Arrays.asList(getTabPanels(returnInheritedElements)));
		return lst.toArray(new JSComponent[lst.size()]);
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean or JSTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]);
	 *
	 * @return an array of all the JSComponents on the form.
	 */
	@JSFunction
	public JSComponent< ? >[] getComponents()
	{
		return getComponents(false);
	}

	/**
	 * Returns a JSLabel that has the given name.
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var label = frm.getLabel("myLabel");
	 * application.output(label.text);
	 *
	 * @param name the specified name of the label
	 *
	 * @return a JSLabel object (or null if the label with the specified name does not exist)
	 */
	@JSFunction
	public JSLabel getLabel(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<GraphicalComponent> graphicalComponents = application.getFlattenedSolution().getFlattenedForm(form).getGraphicalComponents();
			while (graphicalComponents.hasNext())
			{
				GraphicalComponent label = graphicalComponents.next();
				if (name.equals(label.getName()) && !ComponentFactory.isButton(label))
				{
					return new JSLabel(this, label, application, false);
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
	 * var form = solutionModel.newForm('newFormX',myDatasource,null,true,1000,750);
	 * var jslabel = form.newLabel('JSLabel to delete',100,200,200,50,null);
	 * jslabel.name = 'jsl';
	 * jslabel.transparent = false;
	 * jslabel.background = 'green';
	 * var jsmethod = form.newMethod("function removeMe(event) { var form = solutionModel.getForm('newFormX'); if (form.removeComponent('jsl') == true) application.output('Label has been removed'); else application.output('Label could not be deleted'); forms['newFormX'].controller.recreateUI();}");
	 * var removerButton = form.newButton('Click here to remove the green label',450,500,250,50,jsmethod);
	 * removerButton.name = 'remover';
	 * forms['newFormX'].controller.show();
	 *
	 * @param name the specified name of the JSLabel to be removed
	 *
	 * @return true if the JSLabel with the given name has successfully been removed; false otherwise
	 */
	@JSFunction
	public boolean removeLabel(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<GraphicalComponent> graphicalComponents = form.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent label = graphicalComponents.next();
			if (name.equals(label.getName()) && !ComponentFactory.isButton(label))
			{
				form.removeChild(label);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all JSLabels of this form (optionally including it super forms labels), including the ones without a name.
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
	 * @return all JSLabels on this form
	 *
	 */
	@JSFunction
	public JSLabel[] getLabels(boolean returnInheritedElements)
	{
		List<JSLabel> labels = new ArrayList<JSLabel>();
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(form) : form;
		Iterator<GraphicalComponent> graphicalComponents = form2use.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent gc = graphicalComponents.next();
			if (!ComponentFactory.isButton(gc))
			{
				labels.add(new JSLabel(this, gc, application, false));
			}
		}
		return labels.toArray(new JSLabel[labels.size()]);
	}

	/**
	 * Returns all JSLabels of this form (not including its super form), including the ones without a name.
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
	 * @return all JSLabels on this form
	 *
	 */
	@JSFunction
	public JSLabel[] getLabels()
	{
		return getLabels(false);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#getBorderType()
	 */
	@JSGetter
	public String getBorderType()
	{
		return form.getBorderType();
	}

	@JSSetter
	public void setBorderType(String b)
	{
		checkModification();
		form.setBorderType(b);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getDefaultPageFormat()
	 *
	 * @sample
	 * var form = solutionModel.getForm("someForm");
	 * application.output(form.defaultPageFormat);
	 * form.defaultPageFormat = solutionModel.createPageFormat(612,792,72,72,72,72,SM_ORIENTATION.PORTRAIT,SM_UNITS.PIXELS)
	 */
	@JSGetter
	public String getDefaultPageFormat()
	{
		return form.getDefaultPageFormat();
	}

	@JSSetter
	public void setDefaultPageFormat(String string)
	{
		checkModification();
		form.setDefaultPageFormat(string);
	}

	/**
	 * A JSForm instance representing the super form of this form, if this form has a super form.
	 *
	 * @sample
	 * var subForm = solutionModel.newForm('childForm',myDatasource,null,true,800,600);
	 * var superForm = solutionModel.newForm('childForm',myDatasource,null,true,800,600);
	 * subForm.extendsForm = superForm;
	 *
	 */
	@JSGetter
	public JSForm getExtendsForm()
	{
		int extendsFormID = form.getExtendsID();
		if (extendsFormID > 0)
		{
			Form superForm = application.getFlattenedSolution().getForm(extendsFormID);
			if (superForm != null)
			{
				return application.getScriptEngine().getSolutionModifier().instantiateForm(superForm, false);
			}
		}
		return null;
	}

	@JSSetter
	public void setExtendsForm(Object superForm)
	{
		checkModification();
		Form f = null;
		if (superForm instanceof JSForm)
		{
			f = ((JSForm)superForm).getSupportChild();
		}
		else if (superForm instanceof String)
		{
			f = application.getFlattenedSolution().getForm((String)superForm);
		}

		if (f == null)
		{
			if (superForm != null)
			{
				// this kind of argument is not supported or wrong
				throw new RuntimeException("extendsForm must receive either null, a JSForm object or a valid form name");
			}

			form.setExtendsID(AbstractBase.DEFAULT_INT);
		}
		else
		{
//			if (!f.getTableName().equals(form.getTableName()) || !f.getServerName().equals(form.getServerName()))
//			{
//				throw new RuntimeException("Cant set an extends form with table: " + f.getTableName() + " on a form with table : " + form.getTableName());
//			}
			form.setExtendsID(f.getID());
		}
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getInitialSort()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * form.initialSort = "column1 desc, column2 asc, column3 asc";
	 *
	 */
	@JSGetter
	public String getInitialSort()
	{
		return form.getInitialSort();
	}

	@JSSetter
	public void setInitialSort(String arg)
	{
		checkModification();
		form.setInitialSort(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getName()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * var formName = form.name;
	 * application.output(formName);
	 *
	 */
	@JSReadonlyProperty
	public String getName()
	{
		return form.getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getNavigatorID()
	 *
	 * @description-mc
	 * The navigator is a form that usually handles navigation in application. It is displayed on left side of the screen. Can also have value NONE (no navigator) or IGNORE (reuse current form navigator).
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
	@JSGetter
	public Object getNavigator()
	{
		if (form.getNavigatorID() <= 0)
		{
			return Integer.valueOf(form.getNavigatorID());
		}
		Form f = application.getFlattenedSolution().getForm(form.getNavigatorID());
		if (f != null)
		{
			return application.getScriptEngine().getSolutionModifier().instantiateForm(f, false);
		}
		return null;
	}

	@JSSetter
	public void setNavigator(Object navigator)
	{
		checkModification();
		int id = 0;
		if (navigator instanceof JSForm)
		{
			id = ((JSForm)navigator).getSupportChild().getID();
		}
		else if (navigator instanceof String)
		{
			Form f = application.getFlattenedSolution().getForm((String)navigator);
			if (f != null)
			{
				id = f.getID();
			}
			else
			{
				throw new RuntimeException("cannot find form with name '" + (String)navigator + "'");
			}
		}
		else if (navigator instanceof Number)
		{
			id = ((Number)navigator).intValue();
		}
		else if (navigator != null)
		{
			throw new RuntimeException("cannot get navigator form from given object '" + navigator.toString() + "'");
		}

		form.setNavigatorID(id);
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getPaperPrintScale()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * if (form.paperPrintScale < 100)
	 * 	form.paperPrintScale = 100;
	 *
	 */
	@JSGetter
	public int getPaperPrintScale()
	{
		return form.getPaperPrintScale();
	}

	@JSSetter
	public void setPaperPrintScale(int arg)
	{
		checkModification();
		form.setPaperPrintScale(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getRowBGColorCalculation()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * //assign the global method as a string. Or use a calculation name as the string.
	 * form.rowBGColorCalculation = "scopes.globals.calculationDataProvider";
	 *
	 * @deprecated onRender event replaces rowBGColorCalculation functionality
	 * @see com.servoy.j2db.scripting.solutionmodel.JSForm#getOnRender()
	 */
	@Deprecated
	public String js_getRowBGColorCalculation()
	{
		return form.getRowBGColorCalculation();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getScrollbars()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,1000,600);
	 * form.scrollbars = SM_SCROLLBAR.VERTICAL_SCROLLBAR_NEVER;
	 * forms['newForm1'].controller.show();
	 */
	@JSGetter
	public int getScrollbars()
	{
		return form.getScrollbars();
	}

	@JSSetter
	public void setScrollbars(int i)
	{
		checkModification();
		form.setScrollbars(i);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getServerName()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,800,600);
	 * form.serverName = 'anotherServerName';
	 * var theServerName = form.serverName;
	 * application.output(theServerName);
	 */
	@JSGetter
	public String getServerName()
	{
		return form.getServerName();
	}

	@JSSetter
	public void setServerName(String arg)
	{
		checkModification();
		form.setServerName(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getShowInMenu()
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
	@JSGetter
	public boolean getShowInMenu()
	{
		return form.getShowInMenu();
	}

	@JSSetter
	public void setShowInMenu(boolean arg)
	{
		checkModification();
		form.setShowInMenu(arg);
		FormManager formManager = (FormManager)application.getFormManager();
		formManager.removeForm(form);
		formManager.addForm(form, false);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getStyleClass()
	 *
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * if (aForm.styleClass == null)
	 * 	aForm.styleClass = someStyleClass;
	 * else
	 * 	application.output("The Cascading Style Sheet (CSS) class name applied to this form is " + aForm.styleClass);
	 */
	@JSGetter
	public String getStyleClass()
	{
		return form.getStyleClass();
	}

	@JSSetter
	public void setStyleClass(String arg)
	{
		checkModification();
		form.setStyleClass(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getStyleName()
	 *
	 * @sample
	 * var aForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * if (aForm.styleName == null)
	 * 	aForm.styleName = someServoyStyleName;
	 * else
	 * 	application.output("The name of the Servoy style that is being used on the form is " + aForm.styleName);
	 *
	 */
	@JSGetter
	public String getStyleName()
	{
		return form.getStyleName();
	}

	@JSSetter
	public void setStyleName(String arg)
	{
		checkModification();
		form.setStyleName(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getTableName()
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
	@JSGetter
	public String getTableName()
	{
		return form.getTableName();
	}

	@JSSetter
	public void setTableName(String arg)
	{
		checkModification();
		form.setTableName(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getDataSource()
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/a_server/a_table', 'aStyleName', false, 800, 600)
	 * myForm.dataSource = 'db:/anotherServerName/anotherTableName'
	 *
	 */
	@JSGetter
	public String getDataSource()
	{
		return form.getDataSource();
	}

	@JSSetter
	public void setDataSource(String arg)
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

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getTitleText()
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
	@JSGetter
	public String getTitleText()
	{
		return form.getTitleText();
	}

	@JSSetter
	public void setTitleText(String string)
	{
		checkModification();
		form.setTitleText(string);
	}


	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getTransparent()
	 *
	 * @sample
	 * var form = solutionModel.newForm('myForm',myDatasource,null,true,1000,800);
	 * if (form.transparent == false)
	 * {
	 * 	var style = solutionModel.newStyle('myStyle','form { background-color: yellow; }');
	 * 	style.text = style.text + 'field { background-color: blue; }';
	 * 	form.styleName = 'myStyle';
	 * }
	 * var field = form.newField('columnTextDataProvider',JSField.TEXT_FIELD,100,100,100,50);
	 * forms['myForm'].controller.show();
	 */
	@JSGetter
	public boolean getTransparent()
	{
		return form.getTransparent();
	}

	@JSSetter
	public void setTransparent(boolean arg)
	{
		checkModification();
		form.setTransparent(arg);
	}

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
	@JSGetter
	public int getSelectionMode()
	{
		return form.getSelectionMode();
	}

	@JSSetter
	public void setSelectionMode(int arg)
	{
		checkModification();
		form.setSelectionMode(arg);
	}

	/**
	 * @deprecated see getNamedFoundSet()
	 */
	@Deprecated
	public boolean js_getUseSeparateFoundSet()
	{
		return form.getUseSeparateFoundSet();
	}

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
	 * frmSeparate.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 * frmSeparate.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
	 * frmSeparate.namedFoundSet = JSForm.SEPARATE_FOUNDSET;
	 * forms['products_separate'].controller.find();
	 * forms['products_separate'].categoryid = '=2';
	 * forms['products_separate'].controller.search();
	 *
	 * // form with empty foundset
	 * var frmEmpty = solutionModel.newForm('products_empty', 'db:/example_data/products', null, true, 640, 480);
	 * frmEmpty.newLabel("Empty FoundSet",10,10,200,20);
	 * frmEmpty.newField('categoryid',JSField.TEXT_FIELD,10,40,200,20);
	 * frmEmpty.newField('productname',JSField.TEXT_FIELD,10,70,200,20);
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
	@JSGetter
	public String getNamedFoundSet()
	{
		String namedFoundset = form.getNamedFoundSet();
		if (namedFoundset != null && namedFoundset.startsWith(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX))
		{
			return namedFoundset.substring(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH);
		}
		return namedFoundset;
	}

	@JSSetter
	public void setNamedFoundSet(Object arg)
	{
		if (arg == null || arg instanceof String)
		{
			setNamedFoundSet((String)arg);
		}
		else if (arg instanceof JSRelation)
		{
			setNamedFoundSet((JSRelation)arg);
		}
		else
		{
			throw new RuntimeException("object type is incompatible with namedFoundset property");
		}
	}

	private void setNamedFoundSet(String arg)
	{
		checkModification();
		if (arg == null || Form.NAMED_FOUNDSET_EMPTY.equals(arg) || Form.NAMED_FOUNDSET_SEPARATE.equals(arg))
		{
			form.setNamedFoundSet(arg);
		}
		else
		{
			// see if it is intended as a global relation
			setNamedFoundSetAsGlobalRelation(application.getFlattenedSolution().getRelation(arg));
		}
	}

	private void setNamedFoundSet(JSRelation globalRelation)
	{
		checkModification();
		if (globalRelation != null)
		{
			setNamedFoundSetAsGlobalRelation(globalRelation.getSupportChild());
		}
		else
		{
			form.setNamedFoundSet(null);
		}
	}

	private void setNamedFoundSetAsGlobalRelation(Relation relation)
	{
		// check to see if the relation is compatible with the datasource (must be a global relation on the appropriate table)
		if (relation == null || !relation.isGlobal())
		{
			throw new RuntimeException("relation not found or invalid; namedFoundset only supports global relations");
		}
		else
		{
			if (Solution.areDataSourcesCompatible(application.getRepository(), relation.getForeignDataSource(), form.getDataSource()))
			{
				form.setNamedFoundSet(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX + relation.getName());
			}
			else
			{
				throw new RuntimeException("(namedFoundset) relation '" + relation.getName() + "' is incompatible with form dataSource");
			}
		}
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getView()
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * myForm.view = JSForm.RECORD_VIEW;
	 * forms['newForm1'].controller.show();
	 */
	@JSGetter
	public int getView()
	{
		return form.getView();
	}

	@JSSetter
	public void setView(int arg)
	{
		checkModification();
		form.setView(arg);
	}

	/**
	 * @deprecated  As of release 4.1, replaced by {@link JSPart#getHeight()}.
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
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * forms['newForm1'].controller.show();
	 * myForm.width = 120;
	 * forms['newForm1'].controller.recreateUI();
	 */
	@JSGetter
	public int getWidth()
	{
		return form.getWidth();
	}

	@JSSetter
	public void setWidth(int width)
	{
		checkModification();
		form.setWidth(width);
	}

	/**
	 * @deprecated  As of release 4.1, replaced by setOnDeleteAllRecordsCmd(JSMethod).
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
	 * @deprecated  As of release 4.1, replaced by setOnDeleteRecordCmd(JSMethod).
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
	 * @deprecated  As of release 4.1, replaced by setOnDuplicateRecordCmd(JSMethod).
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
	 * @deprecated  As of release 4.1, replaced by setOnFindCmd(JSMethod).
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
	 * @deprecated  As of release 4.1, replaced by setOnHideCmd(JSMethod).
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
	 * @deprecated  As of release 4.1, replaced by setOnInvertRecordsCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnLoad(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnNewRecordCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnNextRecordCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnOmitRecordCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnPreviousRecordCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnPrintPreviewCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnRecordEditStart(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnRecordEditStop(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnRecordSelection(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnSearchCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnShowAllRecordsCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnShow(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnShowOmittedRecordsCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnSortCmd(JSMethod).
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
	 * @deprecated As of release 4.1, replaced by setOnUnLoad(JSMethod).
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

	@Deprecated
	public void js_setRowBGColorCalculation(String arg)
	{
		checkModification();
		form.setRowBGColorCalculation(arg);
	}

	@Deprecated
	public void js_setUseSeparateFoundSet(boolean b)
	{
		checkModification();
		form.setUseSeparateFoundSet(b);
	}

	@Deprecated
	public void js_setHeight(int height)
	{
		JSPart part = getPartInternal(Part.BODY);
		if (part != null)
		{
			checkModification();
			part.setHeight(height);
		}
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

	private static JSForm getJSFormParent(IJSParent< ? > parent)
	{
		IJSParent< ? > form = parent;
		while (!(form instanceof JSForm) && form != null)
		{
			form = form.getJSParent();
		}
		return (JSForm)form;
	}

	protected JSMethod getEventHandler(TypedProperty<Integer> methodProperty)
	{
		return getEventHandler(application, form, methodProperty, this);
	}

	static <T extends AbstractBase> JSMethod getEventHandler(IApplication application, T persist, TypedProperty<Integer> methodProperty, IJSParent< ? > parent)
	{
		int methodid = ((Integer)persist.getProperty(methodProperty.getPropertyName())).intValue();
		if (methodid > 0)
		{
			IJSScriptParent< ? > scriptParent = null;
			ScriptMethod scriptMethod = null;
			if (parent instanceof JSForm)
			{
				// form method
				scriptMethod = ((JSForm)parent).getSupportChild().getScriptMethod(methodid);
				if (scriptMethod == null)
				{
					Form f = ((JSForm)parent).getSupportChild();
					while (f != null && f.getExtendsID() > 0 && scriptMethod == null)
					{
						f = application.getFlattenedSolution().getForm(f.getExtendsID());
						if (f != null) scriptMethod = f.getScriptMethod(methodid);
					}
					if (scriptMethod != null)
					{
						scriptParent = application.getScriptEngine().getSolutionModifier().instantiateForm(f, false);
					}
				}
			}

			if (scriptMethod == null)
			{
				// foundset method
				if (parent instanceof JSDataSourceNode)
				{
					scriptMethod = ((JSDataSourceNode)parent).getSupportChild().getFoundsetMethod(methodid);
				}
				else if (parent instanceof JSForm && ((JSForm)parent).form.getDataSource() != null)
				{
					Iterator<ScriptMethod> foundsetMethods = application.getFlattenedSolution().getFoundsetMethods(((JSForm)parent).form.getDataSource(),
						false);
					scriptMethod = AbstractBase.selectById(foundsetMethods, methodid);
					if (scriptMethod != null)
					{
						scriptParent = new JSDataSourceNode(application, ((JSForm)parent).form.getDataSource());
					}
				}
			}

			if (scriptMethod == null)
			{
				// global method
				scriptMethod = application.getFlattenedSolution().getScriptMethod(methodid);
			}

			if (scriptMethod != null)
			{
				if (scriptParent == null)
				{
					if (scriptMethod.getParent() instanceof TableNode && parent instanceof JSDataSourceNode)
					{
						scriptParent = (JSDataSourceNode)parent;
					}
					else if (scriptMethod.getParent() instanceof Solution)
					{
						// global
						scriptParent = null;
					}
					else
					{
						// form method
						scriptParent = getJSFormParent(parent);
					}
				}
				List<Object> arguments = persist.getFlattenedMethodArguments(methodProperty.getPropertyName());
				if (arguments == null || arguments.size() == 0)
				{
					return new JSMethod(scriptParent, scriptMethod, application, false);
				}
				else
				{
					return new JSMethodWithArguments(application, scriptParent, scriptMethod, false, arguments.toArray());
				}
			}
		}
		else if (methodid == 0 && BaseComponent.isCommandProperty(methodProperty.getPropertyName()))
		{
			return (JSMethod)ISMDefaults.COMMAND_DEFAULT;
		}
		return null;
	}

	static <T extends AbstractBase> void setEventHandler(IApplication application, T persist, TypedProperty<Integer> methodProperty, IBaseSMMethod method)
	{
		persist.setProperty(methodProperty.getPropertyName(), new Integer(getMethodId(application, persist, method, methodProperty)));
		persist.putMethodParameters(methodProperty.getPropertyName(),
			//method instanceof JSMethodWithArguments ? Arrays.asList(((JSMethodWithArguments)method).getParameters()) : null,
			new ArrayList(), method instanceof JSMethodWithArguments ? Arrays.asList(((JSMethodWithArguments)method).getArguments()) : null);
	}

	/**
	 * Set the event handler for the method key, JSMethod may contain arguments.
	 */
	protected void setEventHandler(TypedProperty<Integer> methodProperty, IBaseSMMethod method)
	{
		checkModification();
		setEventHandler(application, form, methodProperty, method);
	}

	static int getMethodId(IApplication application, AbstractBase base, IBaseSMMethod method, TypedProperty<Integer> methodProperty)
	{
		if (method == null && methodProperty != null && BaseComponent.isCommandProperty(methodProperty.getPropertyName())) return -1;
		if (method == null || method == ISMDefaults.COMMAND_DEFAULT) return 0;
		return getMethodId(application, base, ((JSMethod)method).getScriptMethod());
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

		// it could be a extends form
		while (f != null && f.getExtendsID() > 0)
		{
			f = application.getFlattenedSolution().getForm(f.getExtendsID());
			if (f != null && parent.getUUID().equals(f.getUUID()))
			{
				return method.getID();
			}
		}

		// or a foundset method
		Iterator<ScriptMethod> foundsetMethods = application.getFlattenedSolution().getFoundsetMethods(f.getDataSource(), false);
		while (foundsetMethods.hasNext())
		{
			if (foundsetMethods.next().getID() == method.getID())
			{
				return method.getID();
			}
		}

		throw new RuntimeException("Method " + method.getName() + " must be a solution method, foundset method or a forms own method"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDeleteAllRecordsCmdMethodID()
	 *
	 * @sampleas getOnNewRecordCmd()
	 */
	@JSGetter
	public JSMethod getOnDeleteAllRecordsCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDELETEALLRECORDSCMDMETHODID);
	}

	@JSSetter
	public void setOnDeleteAllRecordsCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDELETEALLRECORDSCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDeleteRecordCmdMethodID()
	 *
	 * @sampleas getOnNewRecordCmd()
	 */
	@JSGetter
	public JSMethod getOnDeleteRecordCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID);
	}

	@JSSetter
	public void setOnDeleteRecordCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDragMethodID()
	 *
	 * @sample
	 * form.onDrag = form.newMethod('function onDrag(event) { application.output("onDrag intercepted from " + event.getSource()); }');
	 * form.onDragEnd = form.newMethod('function onDragEnd(event) { application.output("onDragEnd intercepted from " + event.getSource()); }');
	 * form.onDragOver = form.newMethod('function onDragOver(event) { application.output("onDragOver intercepted from " + event.getSource()); }');
	 * form.onDrop = form.newMethod('function onDrop(event) { application.output("onDrop intercepted from " + event.getSource()); }');
	 */
	@JSGetter
	public JSMethod getOnDrag()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID);
	}

	@JSSetter
	public void setOnDrag(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDragEndMethodID()
	 *
	 * @sampleas getOnDrag()
	 */
	@JSGetter
	public JSMethod getOnDragEnd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID);
	}

	@JSSetter
	public void setOnDragEnd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDragOverMethodID()
	 *
	 * @sampleas getOnDrag()
	 */
	@JSGetter
	public JSMethod getOnDragOver()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID);
	}

	@JSSetter
	public void setOnDragOver(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDropMethodID()
	 *
	 * @sampleas getOnDrag()
	 */
	@JSGetter
	public JSMethod getOnDrop()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID);
	}

	@JSSetter
	public void setOnDrop(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnElementFocusGainedMethodID()
	 *
	 * @sample
	 * form.onElementFocusGained = form.newMethod('function onElementFocusGained(event) { application.output("onElementFocusGained intercepted from " + event.getSource()); }');
	 * form.onElementFocusLost = form.newMethod('function onElementFocusLost(event) { application.output("onElementFocusLost intercepted from " + event.getSource()); }');
	 */
	@JSGetter
	public JSMethod getOnElementFocusGained()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID);
	}

	@JSSetter
	public void setOnElementFocusGained(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnElementFocusLostMethodID()
	 *
	 * @sampleas getOnElementFocusGained()
	 */
	@JSGetter
	public JSMethod getOnElementFocusLost()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID);
	}

	@JSSetter
	public void setOnElementFocusLost(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnDuplicateRecordCmdMethodID()
	 *
	 * @sampleas getOnNewRecordCmd()
	 */
	@JSGetter
	public JSMethod getOnDuplicateRecordCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID);
	}

	@JSSetter
	public void setOnDuplicateRecordCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnFindCmdMethodID()
	 *
	 * @sample
	 * form.onFindCmd = form.newMethod('function onFindCmd(event) { application.output("onFindCmd intercepted on " + event.getFormName()); }');
	 * form.onSearchCmd = form.newMethod('function onSearchCmd(event) { application.output("onSearchCmd intercepted on " + event.getFormName()); }');
	 * form.onShowAllRecordsCmd = form.newMethod('function onShowAllRecordsCmd(event) { application.output("onShowAllRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnFindCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID);
	}

	@JSSetter
	public void setOnFindCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnHideMethodID()
	 *
	 * @sampleas getOnShow()
	 */
	@JSGetter
	public JSMethod getOnHide()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID);
	}

	@JSSetter
	public void setOnHide(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnInvertRecordsCmdMethodID()
	 *
	 * @sampleas getOnOmitRecordCmd()
	 */
	@JSGetter
	public JSMethod getOnInvertRecordsCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID);
	}

	@JSSetter
	public void setOnInvertRecordsCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnLoadMethodID()
	 *
	 * @sample
	 * form.onLoad = form.newMethod('function onLoad(event) { application.output("onLoad intercepted on " + event.getFormName()); }');
	 * form.onUnLoad = form.newMethod('function onUnLoad(event) { application.output("onUnLoad intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnLoad()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID);
	}

	@JSSetter
	public void setOnLoad(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnNewRecordCmdMethodID()
	 *
	 * @sample
	 * form.onNewRecordCmd = form.newMethod('function onNewRecordCmd(event) { application.output("onNewRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDuplicateRecordCmd = form.newMethod('function onDuplicateRecordCmd(event) { application.output("onDuplicateRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDeleteRecordCmd = form.newMethod('function onDeleteRecordCmd(event) { application.output("onDeleteRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onDeleteAllRecordsCmd = form.newMethod('function onDeleteAllRecordsCmd(event) { application.output("onDeleteAllRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnNewRecordCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID);
	}

	@JSSetter
	public void setOnNewRecordCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnNextRecordCmdMethodID()
	 *
	 * @sampleas getOnPreviousRecordCmd()
	 */
	@JSGetter
	public JSMethod getOnNextRecordCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID);
	}

	@JSSetter
	public void setOnNextRecordCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnOmitRecordCmdMethodID()
	 *
	 * @sample
	 * form.onOmitRecordCmd = form.newMethod('function onOmitRecordCmd(event) { application.output("onOmitRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onShowOmittedRecordsCmd = form.newMethod('function onShowOmittedRecordsCmd(event) { application.output("onShowOmittedRecordsCmd intercepted on " + event.getFormName()); }');
	 * form.onInvertRecordsCmd = form.newMethod('function onInvertRecordsCmd(event) { application.output("onInvertRecordsCmd intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnOmitRecordCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID);
	}

	@JSSetter
	public void setOnOmitRecordCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnPreviousRecordCmdMethodID()
	 *
	 * @sample
	 * form.onPreviousRecordCmd = form.newMethod('function onPreviousRecordCmd(event) { application.output("onPreviousRecordCmd intercepted on " + event.getFormName()); }');
	 * form.onNextRecordCmd = form.newMethod('function onNextRecordCmd(event) { application.output("onNextRecordCmd intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnPreviousRecordCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID);
	}

	@JSSetter
	public void setOnPreviousRecordCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnPrintPreviewCmdMethodID()
	 *
	 * @sample
	 * form.onPrintPreviewCmd = form.newMethod('function onPrintPreviewCmd(event) { application.output("onPrintPreviewCmd intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnPrintPreviewCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID);
	}

	@JSSetter
	public void setOnPrintPreviewCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRecordEditStartMethodID()
	 *
	 * @sample
	 * form.onRecordEditStart = form.newMethod('function onRecordEditStart(event) { application.output("onRecordEditStart intercepted on " + event.getFormName()); }');
	 * form.onRecordEditStop = form.newMethod('function onRecordEditStop(record, event) { application.output("onRecordEditStop intercepted on " + event.getFormName() + ". record is: " + record); }');
	 * form.onRecordSelection = form.newMethod('function onRecordSelection(event) { application.output("onRecordSelection intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnRecordEditStart()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID);
	}

	@JSSetter
	public void setOnRecordEditStart(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRecordEditStopMethodID()
	 *
	 * @sampleas getOnRecordEditStart()
	 */
	@JSGetter
	public JSMethod getOnRecordEditStop()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID);
	}

	@JSSetter
	public void setOnRecordEditStop(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRecordSelectionMethodID()
	 *
	 * @sampleas getOnRecordEditStart()
	 */
	@JSGetter
	public JSMethod getOnRecordSelection()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID);
	}

	@JSSetter
	public void setOnRecordSelection(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnSearchCmdMethodID()
	 *
	 * @sampleas getOnFindCmd()
	 */
	@JSGetter
	public JSMethod getOnSearchCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID);
	}

	@JSSetter
	public void setOnSearchCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnShowAllRecordsCmdMethodID()
	 *
	 * @sampleas getOnFindCmd()
	 */
	@JSGetter
	public JSMethod getOnShowAllRecordsCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID);
	}

	@JSSetter
	public void setOnShowAllRecordsCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnShowMethodID()
	 *
	 * @sample
	 * form.onShow = form.newMethod('function onShow(firstShow, event) { application.output("onShow intercepted on " + event.getFormName() + ". first show? " + firstShow); return false; }');
	 * form.onHide = form.newMethod('function onHide(event) { application.output("onHide blocked on " + event.getFormName()); return false; }');
	 */
	@JSGetter
	public JSMethod getOnShow()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID);
	}

	@JSSetter
	public void setOnShow(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnShowOmittedRecordsCmdMethodID()
	 *
	 * @sampleas getOnOmitRecordCmd()
	 */
	@JSGetter
	public JSMethod getOnShowOmittedRecordsCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID);
	}

	@JSSetter
	public void setOnShowOmittedRecordsCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnSortCmdMethodID()
	 *
	 * @sample
	 * form.onSortCmd = form.newMethod('function onSortCmd(dataProviderID, asc, event) { application.output("onSortCmd intercepted on " + event.getFormName() + ". data provider: " + dataProviderID + ". asc: " + asc); }');
	 */
	@JSGetter
	public JSMethod getOnSortCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID);
	}

	@JSSetter
	public void setOnSortCmd(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnUnLoadMethodID()
	 *
	 * @sampleas getOnLoad()
	 */
	@JSGetter
	public JSMethod getOnUnLoad()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID);
	}

	@JSSetter
	public void setOnUnLoad(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnResizeMethodID()
	 *
	 * @sample
	 * form.onResize = form.newMethod('function onResize(event) { application.output("onResize intercepted on " + event.getFormName()); }');
	 */
	@JSGetter
	public JSMethod getOnResize()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID);
	}

	@JSSetter
	public void setOnResize(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnRenderMethodID()
	 *
	 * @sample
	 * form.onRender = form.newMethod('function onRender(event) { event.getElement().bgcolor = \'#00ff00\' }');
	 */
	@JSGetter
	public JSMethod getOnRender()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID);
	}

	@JSSetter
	public void setOnRender(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, method);
	}

	/**
	 * Get or set the encapsulation level for the form.
	 *
	 * Encapsulation is one of constants JSForm.DEFAULT_ENCAPSULATION, JSForm.PRIVATE_ENCAPSULATION, JSForm.MODULE_PRIVATE_ENCAPSULATION,
	 * JSForm.HIDE_DATAPROVIDERS_ENCAPSULATION, JSForm.HIDE_FOUNDSET_ENCAPSULATION, JSForm.HIDE_CONTROLLER_ENCAPSULATION or JSForm.HIDE_ELEMENTS_ENCAPSULATION
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * myForm.encapsulation = JSForm.HIDE_CONTROLLER_ENCAPSULATION;
	 */
	@JSGetter
	public int getEncapsulation()
	{
		return form.getEncapsulation();
	}

	@JSSetter
	public void setEncapsulation(int arg)
	{
		checkModification();
		form.setEncapsulation(arg);
	}

	/** Get a design-time property of a form.
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var prop = frm.getDesignTimeProperty('myprop')
	 */
	@JSFunction
	public Object getDesignTimeProperty(String key)
	{
		return Utils.parseJSExpression(form.getCustomDesignTimeProperty(key));
	}

	/** Set a design-time property of a form.
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * frm.putDesignTimeProperty('myprop', 'lemon')
	 */
	@JSFunction
	public Object putDesignTimeProperty(String key, Object value)
	{
		checkModification();
		return Utils.parseJSExpression(form.putCustomDesignTimeProperty(key, Utils.makeJSExpression(value)));
	}

	/** Get the design-time properties of a form.
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var propNames = frm.getDesignTimePropertyNames()
	 */
	@JSFunction
	public String[] getDesignTimePropertyNames()
	{
		Map<String, Object> propsMap = form.getCustomDesignTimeProperties();
		String[] designTimePropertyNames = new String[0];
		if (propsMap != null)
		{
			designTimePropertyNames = propsMap.keySet().toArray(new String[propsMap.size()]);
		}
		return designTimePropertyNames;
	}

	/** Clear a design-time property of a form.
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * frm.removeDesignTimeProperty('myprop')
	 */
	@JSFunction
	public Object removeDesignTimeProperty(String key)
	{
		return putDesignTimeProperty(key, null);
	}

	/**
	 * Returns the UUID of this form.
	 *
	 * @sample
	 * var form_UUID = myForm.getUUID();
	 * application.output(form_UUID.toString());
	 */
	@JSFunction
	public UUID getUUID()
	{
		return form.getUUID();
	}

	public IApplication getApplication()
	{
		return application;
	}

	@Override
	public String toString()
	{
		return "JSForm[name:" + form.getName() + ",size:" + form.getSize() + ",datasource:" + form.getDataSource() + ",style:" + form.getStyleName() + "]";
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((form == null) ? 0 : form.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSForm other = (JSForm)obj;
		if (form == null)
		{
			if (other.form != null) return false;
		}
		else if (!form.getUUID().equals(other.form.getUUID())) return false;
		return true;
	}

	private void addVariableToScopes(ScriptVariable var)
	{
		List<FormController> controllers = ((FormManager)application.getFormManager()).getCachedFormControllers(form);
		for (FormController formController : controllers)
		{
			FormScope formScope = formController.getFormScope();
			formScope.put(var, true);
		}
	}

	private void removeVariableFromScopes(ScriptVariable var)
	{
		List<FormController> controllers = ((FormManager)application.getFormManager()).getCachedFormControllers(form);
		for (FormController formController : controllers)
		{
			FormScope formScope = formController.getFormScope();
			formScope.updateProviderswithCopy(form, form);
			formScope.remove(var);
		}
	}

	private void refreshFromScopes()
	{
		List<FormController> controllers = ((FormManager)application.getFormManager()).getCachedFormControllers(form);
		for (FormController formController : controllers)
		{
			FormScope formScope = formController.getFormScope();
			formScope.updateProviderswithCopy(form, form);
			formScope.reload();
		}
	}
}
