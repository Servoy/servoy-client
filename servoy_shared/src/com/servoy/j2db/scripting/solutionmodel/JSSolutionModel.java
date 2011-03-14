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
import java.awt.Font;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.TableScope;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "SolutionModel", scriptingName = "solutionModel")
public class JSSolutionModel
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSSolutionModel.class, new IReturnedTypesProvider()
		{
			@SuppressWarnings("deprecation")
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { ALIGNMENT.class, ANCHOR.class, BEVELTYPE.class, CURSOR.class, DEFAULTS.class, DISPLAYTYPE.class, FONTSTYLE.class, JOINTYPE.class, MEDIAOPTION.class, PAGEORIENTATION.class, PARTS.class, PRINTSLIDING.class, SCROLLBAR.class, TITLEJUSTIFICATION.class, TITLEPOSITION.class, UNITS.class, VALUELIST.class, VARIABLETYPE.class, VIEW.class, JSForm.class, JSField.class, JSBean.class, JSButton.class, JSCalculation.class, JSComponent.class, JSLabel.class, JSMethod.class, JSPortal.class, JSPart.class, JSRelation.class, JSRelationItem.class, JSStyle.class, JSTabPanel.class, JSTab.class, JSMedia.class, JSValueList.class, JSVariable.class };
			}
		});
	}

	private volatile IApplication application;

	public JSSolutionModel(IApplication application)
	{
		this.application = application;
	}

	/**
	 * Creates a new JSForm Object.
	 * 
	 * NOTE: See the JSForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'myServer', 'myTable', 'myStyleName', false, 800, 600)
	 * //With only a datasource:
	 * //var myForm = solutionModel.newForm('newForm', datasource, 'myStyleName', false, 800, 600)
	 * //now you can add stuff to the form (under JSForm node)
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
	 * @return a new JSForm object
	 */
	public JSForm js_newForm(String name, String serverName, String tableName, String styleName, boolean show_in_menu, int width, int height)
	{
		String dataSource = DataSourceUtils.createDBTableDataSource(serverName, tableName);
		return js_newForm(name, dataSource, styleName, show_in_menu, width, height);
	}

	/**
	 * Creates a new JSForm Object.
	 * 
	 * NOTE: See the JSForm node for more information about form objects that can be added to the new form. 
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'myServer', 'myTable', 'myStyleName', false, 800, 600)
	 * //now you can add stuff to the form (under JSForm node)
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
	 * @return a new JSForm object
	 */
	public JSForm js_newForm(String name, String dataSource, String styleName, boolean show_in_menu, int width, int height)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			Style style = null;
			if (styleName != null)
			{
				style = fs.getStyle(styleName);
			}
			Form form = fs.getSolutionCopy().createNewForm(new ScriptNameValidator(fs), style, name, dataSource, show_in_menu, new Dimension(width, height));
			form.createNewPart(Part.BODY, height);
			((FormManager)application.getFormManager()).addForm(form, false);
			return new JSForm(application, form, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new form with the given JSForm as its super form.
	 * 
	 * @param name The name of the new form
	 * @param superForm the super form that will extended from, see JSform.setExtendsForm();
	 * @return a new JSForm object
	 */
	public JSForm js_newForm(String name, JSForm superForm)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			Form form = fs.getSolutionCopy().createNewForm(new ScriptNameValidator(fs), null, name, null, superForm.js_getShowInMenu(),
				superForm.getForm().getSize());
			form.clearProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
			((FormManager)application.getFormManager()).addForm(form, false);
			form.setExtendsFormID(superForm.getForm().getID());
			return new JSForm(application, form, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the style specified by the given name.
	 * 
	 * @sample
	 * 	var style = solutionModel.getStyle('my_existing_style')
	 * 	style.content = 'combobox { color: #0000ff;font: italic 10pt "Verdana";}'
	 * 
	 * @param name the specified name of the style
	 * 
	 * @return a JSStyle
	 */
	public JSStyle js_getStyle(String name)
	{
		Style style = application.getFlattenedSolution().getStyle(name);
		if (style != null)
		{
			return new JSStyle(application, style, false);
		}
		return null;
	}

	/**
	 * Creates a new style with the given css content string under the given name.
	 * 
	 * NOTE: Will throw an exception if a style with that name already exists.  
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
	 *
	 * @param name the name of the new style
	 * 
	 * @param content the css content of the new style
	 * 
	 * @return a JSStyle object
	 */
	public JSStyle js_newStyle(String name, String content)
	{
		Style style = application.getFlattenedSolution().createStyle(name, content);
		if (style != null)
		{
			return new JSStyle(application, style, true);
		}
		return null;
	}

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
	 * @param jsForm the form to be cloned 
	 * 
	 * @return a JSForm
	 */
	public JSForm js_cloneForm(String newName, JSForm jsForm)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Form clone = fs.clonePersist(jsForm.getForm(), newName, fs.getSolutionCopy());
		((FormManager)application.getFormManager()).addForm(clone, false);
		return new JSForm(application, clone, true);
	}

	/**
	 * Makes an exact copy of the given component (JSComponent/JSField/JSLabel) and gives it a new name.
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
	public <T extends BaseComponent> JSComponent< ? > js_cloneComponent(String newName, JSComponent<T> component)
	{
		return js_cloneComponent(newName, component, null);
	}


	/**
	 * Makes an exact copy of the given component (JSComponent/JSField/JSLabel), gives it a new name and moves it to a new parent form, specified as a parameter.
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
	public <T extends BaseComponent> JSComponent< ? > js_cloneComponent(String newName, JSComponent<T> component, JSForm newParentForm)
	{
		if (component == null || !(component.getBaseComponent(false).getParent() instanceof Form))
		{
			throw new RuntimeException("only components of a form can be cloned");
		}
		JSForm parent = newParentForm;
		if (parent == null)
		{
			parent = (JSForm)component.getJSParent();
		}
		parent.checkModification();
		Form form = parent.getForm();
		FlattenedSolution fs = application.getFlattenedSolution();
		fs.clonePersist(component.getBaseComponent(false), newName, form);
		return parent.js_getComponent(newName);
	}

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
	public boolean js_removeForm(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Form form = fs.getForm(name);
		if (form != null)
		{
			if (((FormManager)application.getFormManager()).removeForm(form))
			{
				fs.deletePersistCopy(form, false);
				return true;
			}
		}
		return false;
	}


	/**
	 * Reverts the specified form to the original (blueprint) version of the form; will result in an exception error if the form is not an original form.
	 * 
	 * NOTE: Make sure you call history.remove first in your Servoy method (script) or call form.controller.recreateUI() before the script ends.
	 *
	 * @sample
	 *  // revert the form to the original solution form, removing any changes done to it through the solution model.
	 *  var revertedForm = solutionModel.revertForm('myForm')
	 *  // add a label on a random place.
	 *  revertedForm.newLabel("MyLabel",Math.random()*100,Math.random()*100,80,20);
	 *  // make sure that the ui is up to date.
	 *  forms.myForm.controller.recreateUI();
	 *
	 * @param name the specified name of the form to revert
	 * 
	 * @return a JSForm object
	 */
	public JSForm js_revertForm(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Form form = fs.getForm(name);
		if (form != null)
		{
			fs.deletePersistCopy(form, true);
			form = fs.getForm(name);
			((FormManager)application.getFormManager()).addForm(form, false);
			application.getFlattenedSolution().registerChangedForm(form);
			return new JSForm(application, form, false);
		}
		return null;
	}

	/**
	 * Gets the specified form object and returns information about the form (see JSForm node).
	 *
	 * @sample
	 * var myForm = solutionModel.getForm('existingFormName');
	 * //get the style of the form (for all other properties see JSForm node)
	 * var styleName = myForm.styleName;
	 *
	 * @param name the specified name of the form
	 * 
	 * @return a JSForm
	 */
	public JSForm js_getForm(String name)
	{
		Form form = ((FormManager)application.getFormManager()).getPossibleForm(name);
		if (form == null)
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			form = fs.getForm(name);
		}

		if (form != null)
		{
			return new JSForm(application, form, false);
		}
		return null;
	}

	/**
	 * Get an array of forms, that are all based on datasource/servername.
	 *
	 * @sample
	 * var forms = solutionModel.getForms(datasource)
	 * for (var i in forms)
	 * 		application.output(forms[i].name)
	 *
	 * @param server the datasource or servername 
	 * 
	 * @return an array of JSForm type elements
	 */
	public JSForm[] js_getForms(String datasource)
	{
		if (datasource == null) throw new IllegalArgumentException("SolutionModel.getForms() param datasource (server/table) is null");
		return getForms(datasource);
	}

	/**
	 * Get an array of forms, that are all based on datasource/servername and tablename.
	 *
	 * @sample
	 * var forms = solutionModel.getForms(datasource,tablename)
	 * for (var i in forms)
	 * 		application.output(forms[i].name)
	 *
	 * @param server the datasource or servername 
	 * 
	 * @param tablename the tablename
	 * 
	 * @return an array of JSForm type elements
	 */
	public JSForm[] js_getForms(String server, String tablename)
	{
		return js_getForms(DataSourceUtils.createDBTableDataSource(server, tablename));
	}

	/**
	 * Get an array of all forms.
	 *
	 * @sample
	 * var forms = solutionModel.getForms()
	 * for (var i in forms)
	 * 		application.output(forms[i].name)
	 *
	 * @return an array of JSForm type elements
	 */
	public JSForm[] js_getForms()
	{
		return getForms(null);
	}

	/**
	 * @param datasource
	 * @return
	 */
	private JSForm[] getForms(String datasource)
	{
		FlattenedSolution fs = application.getFlattenedSolution();

		Iterator<Form> forms = fs.getForms(datasource, true);

		ArrayList<JSForm> list = new ArrayList<JSForm>();
		while (forms.hasNext())
		{
			list.add(new JSForm(application, forms.next(), false));
		}
		return list.toArray(new JSForm[list.size()]);
	}


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
	 * @return a JSMedia element
	 */
	public JSMedia js_getMedia(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Media media = fs.getMedia(name);
		if (media != null)
		{
			return new JSMedia(media, application.getFlattenedSolution(), false);
		}
		return null;
	}

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
	 * @return a JSMedia object
	 *  
	 */
	public JSMedia js_newMedia(String name, byte[] bytes)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			Media media = fs.getSolutionCopy().createNewMedia(new ScriptNameValidator(fs), name);
			media.setPermMediaData(bytes);
			media.setMimeType(ImageLoader.getContentType(bytes));
			return new JSMedia(media, application.getFlattenedSolution(), true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException("error createing new media with name " + name, e);
		}
	}

	/**
	 * Gets the list of all media objects.
	 * 
	 * @sample
	 * 	var mediaList = solutionModel.getMediaList();
	 * 	if (mediaList.length != 0 && mediaList != null) {
	 * 		for (var x in mediaList) {
	 * 			application.output(mediaList[x]);
	 * 		}
	 * 	}
	 * 
	 * 	@return a list with all the media objects.
	 * 	
	 */
	public JSMedia[] js_getMediaList()
	{
		FlattenedSolution fs = application.getFlattenedSolution();

		ArrayList<JSMedia> lst = new ArrayList<JSMedia>();
		Iterator<Media> media = fs.getMedias(true);
		while (media.hasNext())
		{
			lst.add(new JSMedia(media.next(), application.getFlattenedSolution(), false));
		}
		return lst.toArray(new JSMedia[lst.size()]);
	}

	/**
	 * Gets an existing valuelist by the specified name and returns a JSValueList Object that can be assigned to a field.
	 *
	 * @sample
	 * var myValueList = solutionModel.getValueList('myValueListHere')
	 * //now set the valueList property of your field
	 * //myField.valuelist = myValueList
	 *
	 * @param name the specified name of the valuelist
	 * 
	 * @return a JSValueList object
	 */
	public JSValueList js_getValueList(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ValueList valuelist = fs.getValueList(name);
		if (valuelist != null)
		{
			return new JSValueList(valuelist, application, false);
		}
		return null;

	}

	/**
	 * Gets an array of all valuelists for the currently active solution.
	 *
	 * @sample 
	 * 	var valueLists = solutionModel.getValueLists();
	 * 	if (valueLists != null && valueLists.length != 0)
	 * 		for (var i in valueLists)
	 * 			application.output(valueLists[i].name); 
	 * 
	 * @return an array of JSValueList objects
	 */
	public JSValueList[] js_getValueLists()
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ArrayList<JSValueList> valuelists = new ArrayList<JSValueList>();
		Iterator<ValueList> iterator = fs.getValueLists(true);
		while (iterator.hasNext())
		{
			valuelists.add(new JSValueList(iterator.next(), application, false));
		}
		return valuelists.toArray(new JSValueList[valuelists.size()]);

	}


	/**
	 * Creates a new valuelist with the specified name and number type.
	 *
	 * @sample
	 * var vl1 = solutionModel.newValueList("customText",JSValueList.CUSTOM_VALUES);
	 * vl1.customValues = "customvalue1\ncustomvalue2";
	 * var vl2 = solutionModel.newValueList("customid",JSValueList.CUSTOM_VALUES);
	 * vl2.customValues = "customvalue1|1\ncustomvalue2|2";
	 * var form = solutionModel.newForm("customValueListForm",controller.getDataSource(),null,true,300,300);
	 * var combo1 = form.newComboBox("globals.text",10,10,120,20);
	 * combo1.valuelist = vl1;
	 * var combo2 = form.newComboBox("globals.id",10,60,120,20);
	 * combo2.valuelist = vl2;
	 *
	 * @param name the specified name for the valuelist
	 *
	 * @param type the specified number type for the valuelist; may be JSValueList.CUSTOM_VALUES, JSValueList.DATABASE_VALUES, JSValueList.EMPTY_VALUE_ALWAYS, JSValueList.EMPTY_VALUE_NEVER
	 * 
	 * @return a JSValueList object
	 */
	public JSValueList js_newValueList(String name, int type)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			ValueList valuelist = fs.getSolutionCopy().createNewValueList(new ScriptNameValidator(fs), name);
			if (valuelist != null)
			{
				valuelist.setValueListType(type);
				return new JSValueList(valuelist, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
		return null;

	}

	/**
	 * Creates a new global variable with the specified name and number type.
	 * 
	 * NOTE: The global variable number type is based on the value assigned from the SolutionModel-JSVariable node; for example: JSVariable.INTEGER.
	 *
	 * @sample 
	 *	var myGlobalVariable = solutionModel.newGlobalVariable('newGlobalVariable',JSVariable.INTEGER); 
	 *	myGlobalVariable.defaultValue = 12;
	 *
	 * @param name the specified name for the global variable 
	 *
	 * @param type the specified number type for the global variable
	 * 
	 * @return a JSVariable object
	 */
	public JSVariable js_newGlobalVariable(String name, int type)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			ScriptVariable variable = fs.getSolutionCopy().createNewScriptVariable(new ScriptNameValidator(application.getFlattenedSolution()), name, type);
			application.getScriptEngine().getGlobalScope().put(variable);
			return new JSVariable(application, variable, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an existing global variable by the specified name.
	 *
	 * @sample 
	 * 	var globalVariable = solutionModel.getGlobalVariable('globalVariableName');
	 * 	application.output(globalVariable.name + " has the default value of " + globalVariable.defaultValue);
	 * 
	 * @param name the specified name of the global variable
	 * 
	 * @return a JSVariable 
	 */
	public JSVariable js_getGlobalVariable(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ScriptVariable variable = fs.getScriptVariable(name);
		if (variable != null)
		{
			return new JSVariable(application, variable, false);
		}
		return null;
	}

	/**
	 * Gets an array of all global variables.
	 * 
	 * @sample
	 * 	var globalVariables = solutionModel.getGlobalVariables();
	 * 	for (var i in globalVariables)
	 * 		application.output(globalVariables[i].name + " has the default value of " + globalVariables[i].defaultValue);
	 * 
	 * @return an array of JSVariable type elements
	 * 
	 */
	public JSVariable[] js_getGlobalVariables()
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ArrayList<JSVariable> variables = new ArrayList<JSVariable>();
		Iterator<ScriptVariable> scriptVariables = fs.getScriptVariables(true);
		while (scriptVariables.hasNext())
		{
			variables.add(new JSVariable(application, scriptVariables.next(), false));
		}
		return variables.toArray(new JSVariable[variables.size()]);
	}

	/**
	 * Creates a new global method with the specified code.
	 *
	 * @sample 
	 *  var method = solutionModel.newGlobalMethod('function myglobalmethod(){currentcontroller.newRecord()}')
	 *
	 * @param code the specified code for the global method
	 * 
	 * @return a JSMethod object
	 */
	public JSMethod js_newGlobalMethod(String code)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		String name = JSMethod.parseName(code);

		try
		{
			ScriptMethod method = fs.getSolutionCopy().createNewGlobalScriptMethod(new ScriptNameValidator(application.getFlattenedSolution()), name);
			method.setDeclaration(code);
			application.getScriptEngine().getGlobalScope().put(method, method);
			return new JSMethod(application, method, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an existing global method by the specified name.
	 *
	 * @sample 
	 * 	var method = solutionModel.getGlobalMethod("nameOfGlobalMethod"); 
	 * 	if (method != null) application.output(method.code);
	 * 
	 * @param name the name of the specified global method
	 * 
	 * @return a JSMethod
	 */
	public JSMethod js_getGlobalMethod(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ScriptMethod sm = fs.getScriptMethod(name);
		if (sm != null)
		{
			return new JSMethod(application, sm, false);
		}
		return null;
	}

	/**
	 * Get a JSMethod instance with arguments to be assigned to an event.
	 *
	 * @sample 
	 * var str = "John's Bookstore"
	 * var form = solutionModel.getForm('orders')
	 * var button = form.getButton('abutton')
	 * var method = form.getFormMethod('doit') // has 4 arguments: event (fixed), boolean, number and string
	 * // string arguments have to be quoted, they are interpreted before the method is called
	 * var quotedString = "'"+utils.stringReplace(str, "'", "\\'")+"'"
	 * // list all arguments the method has, use nulls for fixed arguments (like event)
	 * button.onAction = solutionModel.wrapMethodWithArguments(method, null, true, 42, quotedString)
	 * 
	 * @param method JSMethod to be assigned to an event
	 * 
	 * @param args positional arguments
	 * 
	 * @return a JSMethod
	 */
	public JSMethod js_wrapMethodWithArguments(JSMethod method, Object... args)
	{
		if (method == null || args == null || args.length == 0)
		{
			return method;
		}
		return new JSMethodWithArguments(method, args);
	}

	/**
	 * The list of all global methods.
	 * 
	 * @sample
	 * 	var methods = solutionModel.getGlobalMethods(); 
	 * 	if (methods != null)
	 * 		for (var x in methods) 
	 * 			application.output(methods[x].getName());
	 * 
	 * @return an array of JSMethod type elements
	 * 
	 */
	public JSMethod[] js_getGlobalMethods()
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ArrayList<JSMethod> methods = new ArrayList<JSMethod>();
		Iterator<ScriptMethod> scriptMethods = fs.getScriptMethods(true);
		while (scriptMethods.hasNext())
		{
			methods.add(new JSMethod(application, scriptMethods.next(), false));
		}
		return methods.toArray(new JSMethod[methods.size()]);
	}

	//newRelation(String name, String primaryServerName, String primaryTableName, String foreignServerName, String foreignTableName,int joinType) <b>
	//newRelation(String name, String primaryDatasource, String foreignDataSource,int joinType)
	/**
	 * Creates a new JSRelation Object with a specified name; includes the primary datasource, optional table name, foreign datasource, optional foreign table name, and the type of join for the new relation.
	 *
	 * @sample 
	 * var rel = solutionModel.newRelation('myRelation','myPrimaryServerName','myPrimaryTableName','myForeignServerName','myForeignTableName',JSRelation.INNER_JOIN);
	 * application.output(rel.getRelationItems()); 
	 *
	 * @param name the specified name of the new relation
	 *
	 * @param primary_server_name|primary_data_source the specified name of the primary server or datasource
	 *
	 * @param primary_table_name optional the specified name of the primary table
	 *
	 * @param foreign_server_name|foreign_data_source the specified name of the foreign server or datasource
	 *
	 * @param foreign_table_name optional the specified name of the foreign table
	 *
	 * @param join_type the type of join for the new relation; JSRelation.INNER_JOIN, JSRelation.LEFT_OUTER_JOIN
	 * 
	 * @return a JSRelation object
	 */
	public JSRelation js_newRelation(Object[] args)
	{
		String name = null;
		String primaryDataSource = null;
		String foreignDataSource = null;
		int joinType;

		FlattenedSolution fs = application.getFlattenedSolution();

		try
		{
			int a = 0;
			// name
			if (a < args.length && args[a] != null)
			{
				name = args[a++].toString();
			}
			// primary server data source or server+table
			if (a < args.length && args[a] != null)
			{
				String primary = args[a++].toString();
				if (primary.indexOf(':') < 0 && a < args.length && args[a] != null)
				{
					// not an uri, server/table combi
					String primaryTableName = args[a++].toString();
					IServer primaryServer = fs.getSolution().getServer(primary);
					if (primaryServer == null) throw new RuntimeException("cant create relation, primary server not found: " + primary);
					if (primaryServer.getTable(primaryTableName) == null) throw new RuntimeException("cant create relation, primary table not found: " +
						primaryTableName);

					primaryDataSource = DataSourceUtils.createDBTableDataSource(primary, primaryTableName);
				}
				else
				{
					// uri
					primaryDataSource = primary;
				}
			}
			// foreign server data source or server+table
			if (a < args.length && args[a] != null)
			{
				String foreign = args[a++].toString();
				if (foreign.indexOf(':') < 0 && a < args.length && args[a] != null)
				{
					// not an uri, server/table combi
					String foreignTableName = args[a++].toString();
					IServer foreignServer = fs.getSolution().getServer(foreign);
					if (foreignServer == null) throw new RuntimeException("cant create relation, foreign server not found: " + foreign);
					if (foreignServer.getTable(foreignTableName) == null) throw new RuntimeException("cant create relation, foreign table not found: " +
						foreignTableName);

					foreignDataSource = DataSourceUtils.createDBTableDataSource(foreign, foreignTableName);
				}
				else
				{
					// uri
					foreignDataSource = foreign;
				}
			}
			if (a < args.length && args[a] != null)
			{
				joinType = Utils.getAsInteger(args[a]);
			}
			else
			{
				return null;
			}
			if (name == null || primaryDataSource == null || foreignDataSource == null)
			{
				return null;
			}

			Relation relation = fs.getSolutionCopy().createNewRelation(new ScriptNameValidator(fs), name, primaryDataSource, foreignDataSource, joinType);
			return new JSRelation(relation, application, true);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an existing relation by the specified name and returns a JSRelation Object.
	 * 
	 * @sample 
	 * 	var relation = solutionModel.getRelation('name');
	 *	application.output("The primary server name is " + relation.primaryServerName);
	 * 	application.output("The primary table name is " + relation.primaryTableName); 
	 * 	application.output("The foreign table name is " + relation.foreignTableName); 
	 * 	application.output("The relation items are " + relation.getRelationItems());
	 * 
	 * @param name the specified name of the relation
	 * 
	 * @return a JSRelation
	 */
	public JSRelation js_getRelation(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Relation relation = fs.getRelation(name);
		if (relation != null)
		{
			return new JSRelation(relation, application, false);
		}
		return null;

	}

	/**
	 * Gets an array of all relations; or an array of all global relations if the specified table is NULL.
	 *
	 * @sample 
	 * 	var relations = solutionModel.getRelations('server_name','table_name');
	 * 	if (relations.length != 0)
	 * 		for (var i in relations)
	 * 			application.output(relations[i].name);
	 *
	 * @param primary_server_name/primary_data_source optional the specified name of the server or datasource for the specified table
	 *
	 * @param primary_table_name optional the specified name of the table
	 * 
	 * @return an array of all relations (all elements in the array are of type JSRelation)
	 */
	public JSRelation[] js_getRelations(Object[] args)
	{
		FlattenedSolution fs = application.getFlattenedSolution();

		try
		{
			String servername = null;
			String tablename = null;
			if (args.length == 2)
			{
				servername = (String)args[0];
				tablename = (String)args[1];
			}
			else if (args.length == 1)
			{
				String[] names = DataSourceUtils.getDBServernameTablename((String)args[0]);
				if (names != null && names.length == 2)
				{
					servername = names[0];
					tablename = names[1];
				}
			}
			Table primaryTable = null;
			if (servername != null && tablename != null)
			{
				IServer primaryServer = fs.getSolution().getServer(servername);
				if (primaryServer == null) throw new RuntimeException("cant create relation, primary server not found: " + servername);
				primaryTable = (Table)primaryServer.getTable(tablename);
				if (primaryTable == null) throw new RuntimeException("cant create relation, primary table not found: " + tablename);
			}

			List<JSRelation> relations = new ArrayList<JSRelation>();
			Iterator<Relation> iterator = fs.getRelations(primaryTable, true, true);
			while (iterator.hasNext())
			{
				Relation relation = iterator.next();
				if ((primaryTable == null && relation.isGlobal()) || (primaryTable != null && !relation.isGlobal()))
				{
					relations.add(new JSRelation(relation, application, false));
				}
			}
			return relations.toArray(new JSRelation[relations.size()]);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets all the calculations for the given datasource.
	 * 
	 * @param datasource The datasource the calculations belong to.
	 */
	public JSCalculation[] js_getCalculations(String datasource)
	{
		try
		{
			ITable table = application.getFoundSetManager().getTable(datasource);
			if (table == null) throw new RuntimeException("No table found for datasource: " + datasource);

			List<JSCalculation> calculations = new ArrayList<JSCalculation>();
			FlattenedSolution fs = application.getFlattenedSolution();
			Iterator<ScriptCalculation> scriptCalculations = fs.getScriptCalculations(table, true);
			while (scriptCalculations.hasNext())
			{
				calculations.add(new JSCalculation(scriptCalculations.next(), false, application));
			}
			return calculations.toArray(new JSCalculation[calculations.size()]);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}


	/**
	 * Get an existing calculation for the given name and datasource.
	 * 
	 * @param name The name of the calculation
	 * @param datasource The datasource the calculation belongs to.
	 */
	public JSCalculation js_getCalculation(String name, String datasource)
	{
		try
		{
			ITable table = application.getFoundSetManager().getTable(datasource);

			ScriptCalculation scriptCalculation = application.getFlattenedSolution().getScriptCalculation(name, table);
			if (scriptCalculation != null)
			{
				return new JSCalculation(scriptCalculation, false, application);
			}
			return null;
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}


	/**
	 * Creates a new calculation for the given code, the type will be the column where it could be build on (if name is a column name),
	 * else it will default to JSVariable.TEXT;
	 * 
	 * @param code The code of the calculation, this must be a full function declaration.
	 * @param datasource The datasource this calculation belongs to. 
	 */
	public JSCalculation js_newCalculation(String code, String datasource)
	{
		return js_newCalculation(code, IColumnTypes.TEXT, datasource);
	}

	/**
	 * Creates a new calculation for the given code and the type, if it builds on a column (name is a column name) then type will be ignored.
	 * 
	 * @param code The code of the calculation, this must be a full function declaration.
	 * @param type The type of the calculation, one of the JSVariable types.
	 * @param datasource The datasource this calculation belongs to. 
	 */
	public JSCalculation js_newCalculation(String code, int type, String datasource)
	{
		try
		{
			ITable table = application.getFoundSetManager().getTable(datasource);
			if (table == null) throw new RuntimeException("No table found for datasource: " + datasource);

			FlattenedSolution fs = application.getFlattenedSolution();
			TableNode tablenode = fs.getSolutionCopyTableNode(table);
			if (tablenode == null) throw new RuntimeException("Couldnt create calculation for datasource: " + datasource);

			String name = JSMethod.parseName(code);
			ScriptCalculation scriptCalculation = tablenode.createNewScriptCalculation(new ScriptNameValidator(fs), name);
			scriptCalculation.setDeclaration(code);
			scriptCalculation.setTypeAndCheck(type);
			TableScope tableScope = (TableScope)application.getScriptEngine().getTableScope(scriptCalculation.getTable());
			if (tableScope != null)
			{
				tableScope.put(scriptCalculation, scriptCalculation);
				((FoundSetManager)application.getFoundSetManager()).flushSQLSheet(datasource);
			}

			return new JSCalculation(scriptCalculation, true, application);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

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
	 * @param orientation optional the specified orientation of the page to be printed; the default is Portrait mode
	 * @param units optional the specified units for the width and height of the page to be printed; the default is pixels
	 */
	public String js_createPageFormat(Object[] vargs)
	{
		double width = vargs.length <= 0 ? 0 : Utils.getAsDouble(vargs[0]);
		double height = vargs.length <= 1 ? 0 : Utils.getAsDouble(vargs[1]);
		double lm = vargs.length <= 2 ? 0 : Utils.getAsDouble(vargs[2]);
		double rm = vargs.length <= 3 ? 0 : Utils.getAsDouble(vargs[3]);
		double tm = vargs.length <= 4 ? 0 : Utils.getAsDouble(vargs[4]);
		double bm = vargs.length <= 5 ? 0 : Utils.getAsDouble(vargs[5]);
		int orientation = vargs.length <= 6 ? PageFormat.PORTRAIT : Utils.getAsInteger(vargs[6]);
		int units = vargs.length <= 7 ? UNITS.PIXELS : Utils.getAsInteger(vargs[7]);
		PageFormat pf = Utils.createPageFormat(width, height, lm, rm, tm, bm, orientation, units);
		return PersistHelper.createPageFormatString(pf);
	}

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
	public String js_createFont(String name, int style, int size)
	{
		Font font = PersistHelper.createFont(name, style, size);
		return PersistHelper.createFontString(font);
	}

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
	public String js_createEmptyBorder(int top_width, int right_width, int bottom_width, int left_width)
	{
		Border border = BorderFactory.createEmptyBorder(top_width, left_width, bottom_width, right_width);
		return ComponentFactoryHelper.createBorderString(border);
	}

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
	public String js_createEtchedBorder(int bevel_type, String highlight_color, String shadow_color)
	{
		Border border = null;
		if (highlight_color != null && shadow_color != null)
		{
			border = BorderFactory.createEtchedBorder(bevel_type, PersistHelper.createColor(highlight_color), PersistHelper.createColor(shadow_color));
		}
		else
		{
			border = BorderFactory.createEtchedBorder(bevel_type);
		}
		return ComponentFactoryHelper.createBorderString(border);
	}

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
	public String js_createBevelBorder(int bevel_type, String highlight_outer_color, String highlight_inner_color, String shadow_outer_color,
		String shadow_inner_color)
	{
		Border border = null;
		if (highlight_outer_color != null && highlight_inner_color != null && shadow_outer_color != null && shadow_inner_color != null)
		{
			border = BorderFactory.createBevelBorder(bevel_type, PersistHelper.createColor(highlight_outer_color),
				PersistHelper.createColor(highlight_inner_color), PersistHelper.createColor(shadow_outer_color), PersistHelper.createColor(shadow_inner_color));

		}
		else
		{
			border = BorderFactory.createBevelBorder(bevel_type);
		}
		return ComponentFactoryHelper.createBorderString(border);
	}

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
	public String js_createLineBorder(int thick, String color)
	{
		Border border = BorderFactory.createLineBorder(PersistHelper.createColor(color), thick);
		return ComponentFactoryHelper.createBorderString(border);
	}

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
	public String js_createTitledBorder(String title_text, String font, String color, int title_justification, int title_position)
	{
		TitledBorder border = BorderFactory.createTitledBorder(title_text);
		border.setTitleJustification(title_justification);
		border.setTitlePosition(title_position);
		if (font != null) border.setTitleFont(PersistHelper.createFont(font));
		if (color != null) border.setTitleColor(PersistHelper.createColor(color));
		return ComponentFactoryHelper.createBorderString(border);
	}

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
	public String js_createMatteBorder(int top_width, int right_width, int bottom_width, int left_width, String color)
	{
		Border border = BorderFactory.createMatteBorder(top_width, left_width, bottom_width, right_width, PersistHelper.createColor(color));
		return ComponentFactoryHelper.createBorderString(border);
	}

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
	public String js_createSpecialMatteBorder(int top_width, int right_width, int bottom_width, int left_width, String top_color, String right_color,
		String bottom_color, String left_color, float rounding_radius, float[] dash_pattern)
	{
		SpecialMatteBorder border = new SpecialMatteBorder(top_width, left_width, bottom_width, right_width, PersistHelper.createColor(top_color),
			PersistHelper.createColor(right_color), PersistHelper.createColor(bottom_color), PersistHelper.createColor(left_color));
		border.setRoundingRadius(rounding_radius);
		if (dash_pattern != null) border.setDashPattern(dash_pattern);
		return ComponentFactoryHelper.createBorderString(border);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "SolutionModel";
	}

	public void destroy()
	{
		application = null;
	}
}
