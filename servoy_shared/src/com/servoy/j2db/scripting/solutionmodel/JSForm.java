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
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMVariable;
import com.servoy.base.solutionmodel.mobile.IMobileSMForm;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.solutionmodel.ISMDefaults;
import com.servoy.j2db.solutionmodel.ISMForm;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSForm")
public class JSForm extends JSBaseContainer<Form> implements IJSScriptParent<Form>, IConstantsObject, ISMForm, IMobileSMForm
{
	private Form form;
	private final IApplication application;
	private boolean isCopy;

	public JSForm(IApplication application, Form form, boolean isNew)
	{
		super(application);
		this.application = application;
		this.setForm(form);
		this.isCopy = isNew;
	}

	private Form getForm()
	{
		if (!isCopy)
		{
			// as long as the form is not already a copy, we have to get the real one
			// so that changes to other instances of JSForm that points to the same form are seen in this one.
			Form frm = application.getFlattenedSolution().getForm(form.getName());
			form = frm != null ? frm : form;
		}
		return form;
	}

	private void setForm(Form form)
	{
		this.form = form;
	}

	@Override
	public AbstractContainer getContainer()
	{
		return getForm();
	}

	@Override
	public final void checkModification()
	{
		// make copy if needed
		if (!isCopy)
		{
			setForm(application.getFlattenedSolution().createPersistCopy(getForm()));
			application.getFormManager().addForm(getForm(), false);

			//forms scope still uses the old copy of Script Providers
			Form oldform = getForm();
			List<IFormController> controllers = application.getFormManager().getCachedFormControllers(getForm());
			for (IFormController formController : controllers)
			{
				FormScope formScope = formController.getFormScope();
				formScope.updateProviderswithCopy(oldform, getForm());
			}

			isCopy = true;
		}
		else
		{
			application.getFlattenedSolution().flushFlattendFormCache(getForm(), true);
		}
		getForm().setLastModified(System.currentTimeMillis());

		application.getFlattenedSolution().registerChangedForm(getForm());
	}

	@SuppressWarnings("unchecked")
	public <T extends IScriptProvider> T getScriptCopy(T script)
	{
		checkModification();
		return (T)getForm().getChild(script.getUUID());
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
		return getForm();
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
			ScriptVariable variable = getForm().createNewScriptVariable(new ScriptNameValidator(application.getFlattenedSolution()), name, type);
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
		ScriptVariable variable = getForm().getScriptVariable(name);
		if (variable != null)
		{
			getForm().removeChild(variable);
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
		ScriptVariable variable = application.getFlattenedSolution().getFlattenedForm(getForm()).getScriptVariable(name);
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
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(getForm()) : getForm();
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
			ScriptMethod method = getForm().createNewScriptMethod(new ScriptNameValidator(application.getFlattenedSolution()), name);
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
		ScriptMethod method = getForm().getScriptMethod(name);
		if (method != null)
		{ // first remove from scopes , then remove from model copy - !important
			//removeMethodFromScopes(method);
			getForm().removeChild(method);
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
		ScriptMethod sm = application.getFlattenedSolution().getFlattenedForm(getForm()).getScriptMethod(name);
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
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(getForm()) : getForm();
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

	private JSPart[] getPartsInternal(int partType)
	{
		ArrayList<JSPart> lst = new ArrayList<JSPart>();
		Iterator<Part> parts = getForm().getParts();
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
		Iterator<Part> parts = application.getFlattenedSolution().getFlattenedForm(getForm()).getParts();
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
		Form superForm = application.getFlattenedSolution().getForm(getForm().getExtendsID());
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
				part = JSPart.createPart(this, getForm().createNewPart(partType, height), false);
				int testHeight = 0;
				Iterator<Part> parts = getForm().getParts();
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
		Iterator<Part> parts = getForm().getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if ((header && PersistUtils.isHeaderPart(part.getPartType())) || (!header && PersistUtils.isFooterPart(part.getPartType())))
			{
				getForm().removeChild(part);
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
		Form form2use = returnInheritedElements ? application.getFlattenedSolution().getFlattenedForm(getForm()) : getForm();
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
		Form ff = application.getFlattenedSolution().getFlattenedForm(getForm());

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
		Iterator<Part> parts = getForm().getParts();
		while (parts.hasNext())
		{
			Part part = parts.next();
			if (part.getPartType() == type && (height == -1 || part.getHeight() == height))
			{
				getForm().removeChild(part);
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


	@JSFunction
	@Override
	public JSBean newBean(String name, int y)
	{
		return null; // only in mobile
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

	@JSFunction
	public JSButton newButton(String txt, int y, IBaseSMMethod jsmethod)
	{
		return newButton(txt, 0, y, 10, 10, jsmethod);
	}

	@JSFunction
	public JSLabel newLabel(String txt, int y)
	{
		return newLabel(txt, 0, y, 10, 10, null);
	}

	/**
	 * @sameas com.servoy.j2db.scripting.solutionmodel.JSComponent#getBorderType()
	 */
	@JSGetter
	public String getBorderType()
	{
		return getForm().getBorderType();
	}

	@JSSetter
	public void setBorderType(String b)
	{
		checkModification();
		getForm().setBorderType(b);
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String getDefaultPageFormat()
	{
		return getForm().getDefaultPageFormat();
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setDefaultPageFormat(String string)
	{
		checkModification();
		getForm().setDefaultPageFormat(string);
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
		int extendsFormID = getForm().getExtendsID();
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

			getForm().setExtendsID(AbstractBase.DEFAULT_INT);
		}
		else
		{
//			if (!f.getTableName().equals(form.getTableName()) || !f.getServerName().equals(form.getServerName()))
//			{
//				throw new RuntimeException("Cant set an extends form with table: " + f.getTableName() + " on a form with table : " + form.getTableName());
//			}
			if (f.isResponsiveLayout() != getForm().isResponsiveLayout())
			{
				if (getForm().isResponsiveLayout())
				{
					if (f.getParts().hasNext()) // only if it is not an abstract form
					{
						throw new RuntimeException("Form '" + getForm().getName() + "' is a responsive layout form, it cannot extend form '" + f.getName() +
							"' which is an absolute layout form.");
					}
				}
				else
				{
					throw new RuntimeException("Form '" + getForm().getName() + "' is an absolute layout form, it cannot extend form '" + f.getName() +
						"' which is a responsive layout form.");
				}
			}
			getForm().setExtendsID(f.getID());
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
		return getForm().getInitialSort();
	}

	@JSSetter
	public void setInitialSort(String arg)
	{
		checkModification();
		getForm().setInitialSort(arg);
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
		return getForm().getName();
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
		if (getForm().getNavigatorID() <= 0)
		{
			return Integer.valueOf(getForm().getNavigatorID());
		}
		Form f = application.getFlattenedSolution().getForm(getForm().getNavigatorID());
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

		getForm().setNavigatorID(id);
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPaperPrintScale()
	{
		return getForm().getPaperPrintScale();
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPaperPrintScale(int arg)
	{
		checkModification();
		getForm().setPaperPrintScale(arg);
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String js_getRowBGColorCalculation()
	{
		return getForm().getRowBGColorCalculation();
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
		return getForm().getScrollbars();
	}

	@JSSetter
	public void setScrollbars(int i)
	{
		checkModification();
		getForm().setScrollbars(i);
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
		return getForm().getServerName();
	}

	@JSSetter
	public void setServerName(String arg)
	{
		checkModification();
		getForm().setServerName(arg);
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
		return getForm().getShowInMenu();
	}

	@JSSetter
	public void setShowInMenu(boolean arg)
	{
		checkModification();
		getForm().setShowInMenu(arg);
		application.getFormManager().removeForm(getForm());
		application.getFormManager().addForm(getForm(), false);
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
		return getForm().getStyleClass();
	}

	@JSSetter
	public void setStyleClass(String arg)
	{
		checkModification();
		getForm().setStyleClass(arg);
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String getStyleName()
	{
		return getForm().getStyleName();
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setStyleName(String arg)
	{
		checkModification();
		getForm().setStyleName(arg);
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
		return getForm().getTableName();
	}

	@JSSetter
	public void setTableName(String arg)
	{
		checkModification();
		getForm().setTableName(arg);
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
		return getForm().getDataSource();
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
		getForm().setDataSource(arg);
		// clear the data provider lookups affected by this change
		FlattenedSolution fs = application.getFlattenedSolution();
		fs.flushDataProviderLookups(getForm());
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
		return getForm().getTitleText();
	}

	@JSSetter
	public void setTitleText(String string)
	{
		checkModification();
		getForm().setTitleText(string);
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
		return getForm().getTransparent();
	}

	@JSSetter
	public void setTransparent(boolean arg)
	{
		checkModification();
		getForm().setTransparent(arg);
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getSelectionMode()
	{
		return getForm().getSelectionMode();
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setSelectionMode(int arg)
	{
		checkModification();
		getForm().setSelectionMode(arg);
	}

	/**
	 * @deprecated see getNamedFoundSet()
	 */
	@Deprecated
	public boolean js_getUseSeparateFoundSet()
	{
		return getForm().getUseSeparateFoundSet();
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
		String namedFoundset = getForm().getNamedFoundSet();
		if (namedFoundset != null && namedFoundset.startsWith(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX))
		{
			return namedFoundset.substring(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH);
		}
		if (namedFoundset != null && namedFoundset.startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
		{
			return namedFoundset.substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
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
			getForm().setNamedFoundSet(arg);
		}
		else
		{
			Relation relation = application.getFlattenedSolution().getRelation(arg);
			if (relation != null)
			{
				// see if it is intended as a global relation
				setNamedFoundSetAsGlobalRelation(relation);
			}
			else
			{
				getForm().setNamedFoundSet(Form.NAMED_FOUNDSET_SEPARATE_PREFIX + arg);
			}
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
			getForm().setNamedFoundSet(null);
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
			if (Solution.areDataSourcesCompatible(application.getRepository(), relation.getForeignDataSource(), getForm().getDataSource()))
			{
				getForm().setNamedFoundSet(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX + relation.getName());
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
		return getForm().getView();
	}

	@JSSetter
	public void setView(int arg)
	{
		checkModification();
		getForm().setView(arg);
	}

	/**
	 * @deprecated  As of release 4.1, replaced by {@link JSPart#getHeight()}.
	 */
	@Deprecated
	public int js_getHeight()
	{
		Iterator<Part> parts = getForm().getParts();
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
		return getForm().getWidth();
	}

	@JSSetter
	public void setWidth(int width)
	{
		checkModification();
		getForm().setWidth(width);
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnDeleteAllRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnDeleteAllRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnDeleteAllRecordsCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnDeleteRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnDeleteRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnDeleteRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @deprecated  As of release 4.1, replaced by setOnDuplicateRecordCmd(JSMethod).
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_setOnDuplicateRecordCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnDuplicateRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnDuplicateRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnDuplicateRecordCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnFindCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnFindCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnFindCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnHideMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnHideMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnHideMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnInvertRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnInvertRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnInvertRecordsCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnLoadMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnLoadMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnLoadMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnNewRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnNewRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnNewRecordCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnNextRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnNextRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnNextRecordCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnOmitRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnOmitRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnOmitRecordCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnPreviousRecordCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnPreviousRecordCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnPreviousRecordCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @deprecated As of release 4.1, replaced by setOnPrintPreviewCmd(JSMethod).
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_setOnPrintPreviewCmdMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnPrintPreviewCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnPrintPreviewCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnPrintPreviewCmdMethodID(((Number)functionOrInteger).intValue());
		}
	}

	/**
	 * @deprecated As of release 4.1, replaced by setOnRecordEditStart(JSMethod).
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_setOnRecordEditStartMethod(Object functionOrInteger)
	{
		checkModification();
		if (functionOrInteger instanceof Function)
		{
			Function function = (Function)functionOrInteger;
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnRecordEditStartMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnRecordEditStartMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnRecordEditStartMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnRecordEditStopMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnRecordEditStopMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnRecordEditStopMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnRecordSelectionMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnRecordSelectionMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnRecordSelectionMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnSearchCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnSearchCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnSearchCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnShowAllRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnShowAllRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnShowAllRecordsCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnShowMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnShowMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnShowMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnShowOmittedRecordsCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnShowOmittedRecordsCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnShowOmittedRecordsCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnSortCmdMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnSortCmdMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnSortCmdMethodID(((Number)functionOrInteger).intValue());
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
			ScriptMethod scriptMethod = JSBaseContainer.getScriptMethod(function, application.getFlattenedSolution());
			if (scriptMethod != null)
			{
				getForm().setOnUnLoadMethodID(scriptMethod.getID());
			}
			else
			{
				getForm().setOnUnLoadMethodID(0);
			}
		}
		else if (functionOrInteger instanceof Number)
		{
			getForm().setOnUnLoadMethodID(((Number)functionOrInteger).intValue());
		}
	}

	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void js_setRowBGColorCalculation(String arg)
	{
		checkModification();
		getForm().setRowBGColorCalculation(arg);
	}

	@Deprecated
	public void js_setUseSeparateFoundSet(boolean b)
	{
		checkModification();
		getForm().setUseSeparateFoundSet(b);
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
		return getEventHandler(application, getForm(), methodProperty, this);
	}

	static <T extends AbstractBase> JSMethod getEventHandler(IApplication application, T persist, TypedProperty<Integer> methodProperty, IJSParent< ? > parent)
	{
		return getEventHandler(application, persist, ((Integer)persist.getProperty(methodProperty.getPropertyName())).intValue(), parent,
			methodProperty.getPropertyName());
	}

	public static <T extends AbstractBase> JSMethod getEventHandler(IApplication application, T persist, int methodid, IJSParent< ? > parent,
		String propertyName)
	{
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
				else if (parent instanceof JSForm && ((JSForm)parent).getForm().getDataSource() != null)
				{
					Iterator<ScriptMethod> foundsetMethods = application.getFlattenedSolution().getFoundsetMethods(((JSForm)parent).getForm().getDataSource(),
						false);
					scriptMethod = AbstractBase.selectById(foundsetMethods, methodid);
					if (scriptMethod != null)
					{
						scriptParent = new JSDataSourceNode(application, ((JSForm)parent).getForm().getDataSource());
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
				List<Object> arguments = persist.getFlattenedMethodArguments(propertyName);
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
		else if (methodid == 0 && BaseComponent.isCommandProperty(propertyName))
		{
			return (JSMethod)ISMDefaults.COMMAND_DEFAULT;
		}
		return null;
	}

	public static <T extends AbstractBase> JSMethod getEventHandler(IApplication application, T persist, String uuidOrName, IJSParent< ? > parent,
		String propertyName)
	{
		if (uuidOrName != null)
		{
			IJSScriptParent< ? > scriptParent = null;
			ScriptMethod scriptMethod = application.getFlattenedSolution().getScriptMethod(uuidOrName);
			;
			if (scriptMethod != null)
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
					if (scriptMethod.getParent() != scriptParent.getSupportChild() && scriptParent.getSupportChild() instanceof Form)
					{
						scriptParent = application.getScriptEngine().getSolutionModifier().instantiateForm((Form)scriptParent.getSupportChild(), false);
					}
				}
				List<Object> arguments = persist.getFlattenedMethodArguments(propertyName);
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
		setEventHandler(application, getForm(), methodProperty, method);
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public JSMethod getOnDrag()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID);
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public JSMethod getOnDragOver()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID);
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public JSMethod getOnDrop()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID);
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public JSMethod getOnPrintPreviewCmd()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID);
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public JSMethod getOnRender()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID);
	}

	@JSSetter
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnRender(ISMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, method);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Form#getOnElementDataChangeMethodID()
	 *
	 * @sample
	 * form.onElementDataChange = form.newMethod('function onElementDataChange(oldValue, newValue, event) { application.output("Data changed from " + oldValue + " to " + newValue + " at " + event.getTimestamp()); }');
	 */
	@JSGetter
	public JSMethod getOnElementDataChange()
	{
		return getEventHandler(StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID);
	}

	@JSSetter
	public void setOnElementDataChange(IBaseSMMethod method)
	{
		setEventHandler(StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID, method);
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
		return getForm().getEncapsulation();
	}

	@JSSetter
	public void setEncapsulation(int arg)
	{
		checkModification();
		getForm().setEncapsulation(arg);
	}

	/**
	 * Get or set the ngReadonlyMode for the form. This flag is a performance optimization for tableview/listview to show labels instead of editable fields.
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * myForm.ngReadOnlyMode = true;
	 */
	@JSGetter
	public boolean getNgReadOnlyMode()
	{
		return Utils.getAsBoolean(getForm().getNgReadOnlyMode());
	}

	@JSSetter
	public void setNgReadOnlyMode(boolean arg)
	{
		checkModification();
		getForm().setNgReadOnlyMode(Boolean.valueOf(arg));
	}

	/**
	 * Get or set the positioning (either use anchoring or use css position) for the form.
	 * This is only working for absolute layout forms in NGClient.
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * myForm.useCssPosition = true;
	 */
	@JSGetter
	public boolean getUseCssPosition()
	{
		return Utils.getAsBoolean(getForm().getUseCssPosition());
	}

	@JSSetter
	public void setUseCssPosition(boolean arg)
	{
		checkModification();
		getForm().setUseCssPosition(Boolean.valueOf(arg));
	}

	/**
	 * Returns true if this form is in responsive mode
	 *
	 * @sample
	 * var myForm = solutionModel.getForm('myform');
	 * if (myForm.isResponsive()) {}
	 */
	@JSGetter
	public boolean isResponsiveLayout()
	{
		return getForm().isResponsiveLayout();
	}

	/**
	 * Get a design-time property of a form.
	 *
	 * @param key the property name
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * var prop = frm.getDesignTimeProperty('myprop')
	 */
	@JSFunction
	public Object getDesignTimeProperty(String key)
	{
		return Utils.parseJSExpression(getForm().getCustomDesignTimeProperty(key));
	}

	/**
	 * Set a design-time property of a form.
	 *
	 * @param key the property name
	 * @param value the value to set
	 *
	 * @sample
	 * var frm = solutionModel.getForm('orders')
	 * frm.putDesignTimeProperty('myprop', 'lemon')
	 */
	@JSFunction
	public Object putDesignTimeProperty(String key, Object value)
	{
		checkModification();
		return Utils.parseJSExpression(getForm().putCustomDesignTimeProperty(key, Utils.makeJSExpression(value)));
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
		Map<String, Object> propsMap = getForm().getCustomDesignTimeProperties();
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
	 *
	 * @param key the property name
	 */
	@JSFunction
	public Object removeDesignTimeProperty(String key)
	{
		checkModification();
		return Utils.parseJSExpression(getForm().clearCustomDesignTimeProperty(key));
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
		return getForm().getUUID();
	}

	/**
	 * Creates a new JSField object on the form - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var variable = form.newVariable('myVar', JSVariable.TEXT);
	 * variable.defaultValue = "'This is a default value (with triple quotes)!'";
	 * var field = form.newField(variable, JSField.TEXT_FIELD);
	 * field.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param type the display type of the JSField object (see the Solution Model -> JSField node for display types)
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newField(Object dataprovider, int type)
	{
		return newField(dataprovider, type, 0, 0, 100, 20);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TEXT_FIELD - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * form.useCssPosition = true
	 * var x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * x.defaultValue = "'Text from a global variable'"
	 * var textField = form.newTextField(x);
	 * textField.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a JSField object with the displayType of TEXT_FIELD
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newTextField(Object dataprovider)
	{
		return newField(dataprovider, Field.TEXT_FIELD, 0, 0, 100, 20);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TEXT_AREA - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * form.useCssPosition = true
	 * var globalVar = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * globalVar.defaultValue = "'Type your text in here'";
	 * var textArea = form.newTextArea(globalVar);
	 * textArea.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a JSField object with the displayType of TEXT_AREA
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newTextArea(Object dataprovider)
	{
		return newField(dataprovider, Field.TEXT_AREA, 0, 0, 100, 100);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of COMBOBOX - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var combo = form.newComboBox(myDataProvider);
	 * combo.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of COMBOBOX
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newComboBox(Object dataprovider)
	{
		return newField(dataprovider, Field.COMBOBOX, 0, 0, 100, 20);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of LISTBOX - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * form.useCssPosition = true
	 * var list = form.newListBox(myDataProvider);
	 * list.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of LISTBOX
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newListBox(Object dataprovider)
	{
		return newField(dataprovider, Field.LIST_BOX, 0, 0, 100, 100);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of MULTISELECT_LISTBOX - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * form.useCssPosition = true
	 * var list = form.newMultiSelectListBox(myDataProvider);
	 * list.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of MULTISELECT_LISTBOX
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newMultiSelectListBox(Object dataprovider)
	{
		return newField(dataprovider, Field.MULTISELECT_LISTBOX, 0, 0, 100, 100);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of SPINNER - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'myServer', 'myTable', null, true, 800, 600);
	 * form.useCssPosition = true
	 * var spinner = form.newSpinner(myDataProvider, 10, 460, 100, 20);
	 * spinner.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of SPINNER
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newSpinner(Object dataprovider)
	{
		return newField(dataprovider, Field.SPINNER, 0, 0, 100, 20);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of RADIOS (radio buttons) - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "value1\nvalue2\nvalue3";
	 * var radios = form.newRadios('columnDataProvider');
	 * radios.cssPosition.l("10").t("10").w("20%").h("30px")
	 * radios.valuelist = vlist;
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a JSField object with the displayType of RADIOS (radio buttons)
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newRadios(Object dataprovider)
	{
		return newField(dataprovider, Field.RADIOS, 0, 0, 100, 100);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of CHECK (checkbox) - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var check = form.newCheck(myDataProvider);
	 * check.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of CHECK (checkbox)
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newCheck(Object dataprovider)
	{
		return newField(dataprovider, Field.CHECKS, 0, 0, 100, 100);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of CALENDAR - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var calendar = form.newCalendar(myDataProvider);
	 * calendar.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of CALENDAR
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newCalendar(Object dataprovider)
	{
		return newField(dataprovider, Field.CALENDAR, 0, 0, 100, 20);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of HTML_AREA - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var textProvider = form.newVariable('myVar',JSVariable.TEXT);
	 * textProvider.defaultValue = "'This is a triple quoted text!'";
	 * var htmlArea = myListViewForm.newHtmlArea(textProvider);
	 * htmlArea.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a JSField object on the form with the displayType of HTML_AREA
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newHtmlArea(Object dataprovider)
	{
		return newField(dataprovider, Field.HTML_AREA, 0, 0, 200, 300);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of IMAGE_MEDIA - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var myMediaVar = form.newVariable("media", JSVariable.MEDIA);
	 * var imageMedia = form.newImageMedia(myMediaVar)
	 * imageMedia.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of IMAGE_MEDIA
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newImageMedia(Object dataprovider)
	{
		return newField(dataprovider, Field.IMAGE_MEDIA, 0, 0, 200, 200);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of TYPE_AHEAD - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1',myDatasource,null,true,800,600);
	 * form.useCssPosition = true
	 * var vlist = solutionModel.newValueList('options',JSValueList.CUSTOM_VALUES);
	 * vlist.customValues = "value1\nvalue2\nvalue3";
	 * var typeAhead = form.newTypeAhead(columnTextDataProvider);
	 * typeAhead.cssPosition.l("10").t("10").w("20%").h("30px")
	 * typeAhead.valuelist = vlist;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a JSField object with the displayType of TYPE_AHEAD
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newTypeAhead(Object dataprovider)
	{
		return newField(dataprovider, Field.TYPE_AHEAD, 0, 0, 100, 20);
	}

	/**
	 * Creates a new JSField object on the form with the displayType of PASSWORD - including the dataprovider/JSVariable of the JSField object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var pass = form.newPassword(scopes.globals.aVariable);
	 * pass.cssPosition.l("10").t("10").w("20%").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @return a new JSField object on the form with the displayType of PASSWORD
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSField newPassword(Object dataprovider)
	{
		return newField(dataprovider, Field.PASSWORD, 0, 0, 100, 20);
	}

	/**
	 * Creates a new button on the form with the given text and JSMethod as the onAction event triggered action. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var method = form.newMethod('function onAction(event) { application.output("onAction intercepted on " + event.getFormName()); }');
	 * var button = form.newButton('myButton', method);
	 * button.cssPosition.l("10%").t("10%").w("80px").h("30px")
	 * application.output("The new button: " + button.name + " has the following onAction event handling method assigned " + button.onAction.getName());
	 *
	 * @param txt the text on the button
	 *
	 * @param action the method assigned to handle an onAction event
	 *
	 * @return a new JSButton object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSButton newButton(String txt, Object action)
	{
		return newButton(txt, 0, 0, 100, 20, action);
	}

	/**
	 * Creates a new JSLabel object on the form - including the text of the label. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * form.useCssPosition = true
	 * var label = form.newLabel('The text on the label');
	 * label.cssPosition.l("10%").t("10%").w("80px").h("30px")
	 * forms['newForm1'].controller.show();
	 *
	 * @param txt the specified text of the label object
	 *
	 * @return a JSLabel object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLabel newLabel(String txt)
	{
		return newLabel(txt, 0, 0, 100, 20, null);
	}

	/**
	 * Creates a new JSPortal object on the form - including the name of the JSPortal object and the relation the JSPortal object is based on. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * form.useCssPosition = true
	 * var relation = solutionModel.newRelation('parentToChild','db:/server1/table1','db:/server2/table2',JSRelation.INNER_JOIN);
	 * relation.newRelationItem('another_parent_table_id', '=', 'another_child_table_parent_id');
	 * var portal = form.newPortal('portal',relation);
	 * portal.cssPosition.l("10%").t("10%").b("10%").r("10%")
	 * portal.newField('someColumn',JSField.TEXT_FIELD,200,200,120);
	 * forms['newForm1'].controller.show();
	 *
	 * @param name the specified name of the JSPortal object
	 * @param relation the relation of the JSPortal object
	 *
	 * @return a JSPortal object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSPortal newPortal(String name, Object relation)
	{
		return newPortal(name, relation, 0, 0, 300, 300);
	}

	/**
	 * Creates a new JSTabPanel object on the form - including the name of the JSTabPanel object. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('parentForm','db:/server1/parent_table',null,false,640,480);
	 * form.useCssPosition = true
	 * var childOne = solutionModel.newForm('childOne','db:/server1/child_table',null,false,400,300);
	 * childOne.newField('child_table_text', JSField.TEXT_FIELD,10,10,100,20);
	 * var parentToChild = solutionModel.newRelation('parentToChild','db:/server1/parent_table','db:/server1/child_table',JSRelation.INNER_JOIN);
	 * parentToChild.newRelationItem('parent_table_id','=','child_table_parent_id');
	 * var childTwo = solutionModel.newForm('childTwo','db:/server1/my_table',null,false,400,300);
	 * childTwo.newField('my_table_image', JSField.IMAGE_MEDIA,10,10,100,100);
	 * var tabPanel = form.newTabPanel('tabs');
	 * tabPanel.cssPosition.l("10%").t("10%").b("10%").r("10%")
	 * tabPanel.newTab('tab1','Child One',childOne,parentToChild);
	 * tabPanel.newTab('tab2','Child Two',childTwo);
	 * forms['parentForm'].controller.show();
	 *
	 * @param name the specified name of the JSTabPanel object
	 *
	 * @return a JSTabPanel object
	 */
	@JSFunction
	public JSTabPanel newTabPanel(String name)
	{
		return newTabPanel(name, 0, 0, 100, 100);
	}

	/**
	 * Creates a new JSWebComponent (spec based component) object on a form. You must set location/dimension or css position afterwards
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * form.useCssPosition = true
	 * var webcomponent = form.newWebComponent('mywebcomponent','mypackage-testcomponent');
	 *
	 * @param name the specified name of the JSWebComponent object
	 * @param type the webcomponent name as it appears in the spec
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String name, String type)
	{
		return newWebComponent(name, type, 0, 0, 100, 100);
	}

	public IApplication getApplication()
	{
		return application;
	}

	@Override
	public String toString()
	{
		return "JSForm[name:" + getForm().getName() + ",size:" + getForm().getSize() + ",datasource:" + getForm().getDataSource() + ",style:" +
			getForm().getStyleName() + "]";
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getForm() == null) ? 0 : getForm().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSForm other = (JSForm)obj;
		if (getForm() == null)
		{
			if (other.getForm() != null) return false;
		}
		else if (!getForm().getUUID().equals(other.getForm().getUUID())) return false;
		return true;
	}

	private void addVariableToScopes(ScriptVariable var)
	{
		List<IFormController> controllers = application.getFormManager().getCachedFormControllers(getForm());
		for (IFormController formController : controllers)
		{
			FormScope formScope = formController.getFormScope();
			formScope.put(var, true);
		}
	}

	private void removeVariableFromScopes(ScriptVariable var)
	{
		List<IFormController> controllers = application.getFormManager().getCachedFormControllers(getForm());
		for (IFormController formController : controllers)
		{
			FormScope formScope = formController.getFormScope();
			formScope.updateProviderswithCopy(getForm(), getForm());
			formScope.remove(var);
		}
	}

	private void refreshFromScopes()
	{
		List<IFormController> controllers = application.getFormManager().getCachedFormControllers(getForm());
		for (IFormController formController : controllers)
		{
			FormScope formScope = formController.getFormScope();
			formScope.updateProviderswithCopy(getForm(), getForm());
			formScope.reload();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.solutionmodel.JSBaseContainer#getFlattenedContainer()
	 */
	@Override
	public AbstractContainer getFlattenedContainer()
	{
		return application.getFlattenedSolution().getFlattenedForm(getForm());
	}
}
