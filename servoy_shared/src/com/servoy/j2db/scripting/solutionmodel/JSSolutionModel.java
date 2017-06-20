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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.mobile.IMobileSolutionModel;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.solutionmodel.ISMForm;
import com.servoy.j2db.solutionmodel.ISMRelation;
import com.servoy.j2db.solutionmodel.ISMUnits;
import com.servoy.j2db.solutionmodel.ISolutionModel;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.RoundedBorder;
import com.servoy.j2db.util.gui.SpecialMatteBorder;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "SolutionModel", scriptingName = "solutionModel")
public class JSSolutionModel implements ISolutionModel, IMobileSolutionModel
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSSolutionModel.class, new IReturnedTypesProvider()
		{
			@SuppressWarnings("deprecation")
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { ALIGNMENT.class, ANCHOR.class, BEVELTYPE.class, CURSOR.class, DEFAULTS.class, DISPLAYTYPE.class, //
					FONTSTYLE.class, JOINTYPE.class, MEDIAOPTION.class, PAGEORIENTATION.class, PARTS.class, PRINTSLIDING.class, //
					SCROLLBAR.class, TITLEJUSTIFICATION.class, TITLEPOSITION.class, UNITS.class, VALUELIST.class, VARIABLETYPE.class, //
					VIEW.class, JSForm.class, JSDataSourceNode.class, JSBean.class, JSButton.class, JSCalculation.class, //
					JSFieldWithConstants.class, JSLayoutContainer.class, JSText.class, JSTextArea.class, JSCombobox.class, JSRadios.class, JSChecks.class, JSCalendar.class, JSPassword.class, //
					JSList.class, JSInsetList.class, //
					JSComponent.class, JSLabel.class, JSMethod.class, JSPortal.class, JSPartWithConstants.class, JSRelation.class, JSRelationItem.class, //
					JSStyle.class, JSTabPanel.class, JSTab.class, JSMedia.class, JSValueList.class, JSVariable.class, //
					JSHeader.class, JSFooter.class, JSTitle.class, JSWebComponent.class//
				};
			}
		});
	}

	private volatile IApplication application;

	public JSSolutionModel(IApplication application)
	{
		this.application = application;
	}

	protected IApplication getApplication()
	{
		return application;
	}

	/**
	 * Creates a new JSForm Object.
	 *
	 * NOTE: See the JSForm node for more information about form objects that can be added to the new form.
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'my_server', 'my_table', 'myStyleName', false, 800, 600)
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
	@JSFunction
	public JSForm newForm(String name, String serverName, String tableName, String styleName, boolean show_in_menu, int width, int height)
	{
		return newForm(name, DataSourceUtils.createDBTableDataSource(serverName, tableName), styleName, show_in_menu, width, height);
	}

	/**
	 * Creates a new JSForm Object.
	 *
	 * NOTE: See the JSForm node for more information about form objects that can be added to the new form.
	 *
	 * @sample
	 * var myForm = solutionModel.newForm('newForm', 'db:/my_server/my_table', 'myStyleName', false, 800, 600)
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
	@JSFunction
	public JSForm newForm(String name, String dataSource, String styleName, boolean show_in_menu, int width, int height)
	{
		return newForm(name, dataSource, styleName, show_in_menu, width, height, false);
	}

	protected JSForm newForm(String name, String dataSource, String styleName, boolean show_in_menu, int width, int height, boolean isResponsive)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			Style style = null;
			if (styleName != null)
			{
				style = fs.getStyle(styleName);
			}
			Form form = createNewForm(style, name, dataSource, show_in_menu, new Dimension(width, height));
			if (!isResponsive)
			{
				form.createNewPart(Part.BODY, height);
			}
			else
			{
				form.setResponsiveLayout(true);
			}

			if (fs.getSolution().getSolutionType() == SolutionMetaData.MOBILE)
			{
				// mobile solution, make the form mobile
				form.putCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName, Boolean.TRUE);
				form.setStyleName("_servoy_mobile"); // set internal style name
			}

			application.getFormManager().addForm(form, false);
			return instantiateForm(form, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new form with the given JSForm as its super form.
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
	 * @param superForm the super form that will extended from, see JSform.setExtendsForm();
	 * @return a new JSForm object
	 */
	@JSFunction
	public JSForm newForm(String name, ISMForm superForm)
	{
		return newForm(name, superForm, null);
	}

	/**
	 * Creates a new form with the given JSForm as its super form.
	 * Use this function in the case when the super form is a logical form (no parts/UI).
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
	 * @param superForm the super form that will extended from, see JSform.setExtendsForm();
	 * @param isResponsive
	 * @return a new JSForm object
	 */
	@JSFunction
	public JSForm newForm(String name, ISMForm superForm, Boolean isResponsive)
	{
		if (superForm == null)
		{
			throw new IllegalArgumentException("superForm cannot be null");
		}
		if (isResponsive != null && (superForm.isResponsiveLayout() || superForm.getParts().length > 0))
		{
			throw new IllegalArgumentException(
				"Cannot set the layout type of the child form because the superform '" + superForm.getName() + "' is not a logical form.");
		}
		try
		{
			Form form = createNewForm(null, name, null, superForm.getShowInMenu(), ((JSForm)superForm).getSupportChild().getSize());
			if (isResponsive != null) form.setResponsiveLayout(isResponsive.booleanValue());
			form.clearProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
			application.getFormManager().addForm(form, false);
			form.setExtendsID(((JSForm)superForm).getSupportChild().getID());
			return instantiateForm(form, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	@JSFunction
	public IBaseSMForm newForm(String name, String dataSource)
	{
		return newForm(name, dataSource, null, false, 0, 0, false);
	}

	@JSFunction
	public IBaseSMForm newForm(String name)
	{
		return newForm(name, null, null, false, 0, 0, false);
	}

	/**
	 * Create a responsive form:
	 *
	 * @sample
	 * var frm = solutionModel.newForm('test','db:/my_server/my_table', true);
	 * var c = frm.newLayoutContainer(1);
	 *
	 * @param name The name of the new form
	 * @param dataSource the form datasource
	 * @param isResponsive if true will create an responsive form, otherwise an absolute layout form
	 * @return a new JSForm object
	 */
	@JSFunction
	public IBaseSMForm newForm(String name, String dataSource, boolean isResponsive)
	{
		return newForm(name, dataSource, null, false, 0, 0, isResponsive);
	}

	/**
	 * Create a responsive form:
	 *
	 * @sample
	 * var frm = solutionModel.newForm('test', true);
	 * var c = frm.newLayoutContainer(1);
	 *
	 * @param name The name of the new form
	 * @param isResponsive if true will create an responsive form, otherwise an absolute layout form
	 * @return a new JSForm object
	 */
	@JSFunction
	public IBaseSMForm newForm(String name, boolean isResponsive)
	{
		return newForm(name, null, null, false, 0, 0, isResponsive);
	}

	@JSFunction
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public JSList newListForm(String formName, String dataSource, String textDataProviderID)
	{
		return null; // mobile only
	}

	@Override
	@JSFunction
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public JSList getListForm(String name)
	{
		return null; // mobile only
	}

	@Override
	@JSFunction
	@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
	public JSList[] getListForms()
	{
		return null; // mobile only
	}

	protected Form createNewForm(Style style, String name, String dataSource, boolean show_in_menu, Dimension size) throws RepositoryException
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		return fs.getSolutionCopy().createNewForm(new ScriptNameValidator(fs), style, name, dataSource, show_in_menu, size);
	}

	/**
	 * Gets the style specified by the given name.
	 *
	 * @sample
	 * var style = solutionModel.getStyle('my_existing_style')
	 * style.content = 'combobox { color: #0000ff;font: italic 10pt "Verdana";}'
	 *
	 * @param name the specified name of the style
	 *
	 * @return a JSStyle
	 */
	@JSFunction
	public JSStyle getStyle(String name)
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
	 * @return a JSStyle object
	 */
	@JSFunction
	public JSStyle newStyle(String name, String content)
	{

		Style style = application.getFlattenedSolution().createStyle(name, null);
		if (style == null)
		{
			return null;
		}

		JSStyle jsStyle = new JSStyle(application, style, true);
		jsStyle.setText(content);
		return jsStyle;
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
	@JSFunction
	public JSForm cloneForm(String newName, IBaseSMForm jsForm)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Form clone = fs.clonePersist(((JSForm)jsForm).getSupportChild(), newName, fs.getSolutionCopy());
		application.getFormManager().addForm(clone, false);
		return instantiateForm(clone, true);
	}

	protected JSForm instantiateForm(Form form, boolean isNew)
	{
		return new JSForm(application, form, isNew);
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
	@JSFunction
	public JSComponent< ? > cloneComponent(String newName, IBaseSMComponent component)
	{
		return cloneComponent(newName, component, null);
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
	@JSFunction
	public JSComponent< ? > cloneComponent(String newName, IBaseSMComponent component, IBaseSMForm newParentForm)
	{
		if (component == null || !(((JSBase)component).getBaseComponent(false).getParent() instanceof Form))
		{
			throw new RuntimeException("only components of a form can be cloned");
		}
		JSForm parent = (JSForm)newParentForm;
		if (parent == null)
		{
			parent = (JSForm)((JSBase)component).getJSParent();
		}
		parent.checkModification();
		Form form = parent.getSupportChild();
		FlattenedSolution fs = application.getFlattenedSolution();
		fs.clonePersist(((JSBase)component).getBaseComponent(false), newName, form);
		return parent.getComponent(newName);
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
	@JSFunction
	public boolean removeForm(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Form form = fs.getForm(name);
		if (form != null)
		{
			if (application.getFormManager().removeForm(form))
			{
				fs.deletePersistCopy(form, false);
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes the relation specified by name. You cannot remove the relation if it is touched within the application.
	 * So even if you remove all the ui elements using it, like tabs, it still can't be removed, because of underlying created and cached data.
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
	@JSFunction
	public boolean removeRelation(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Relation rel = fs.getRelation(name);
		if (rel != null)
		{
			try
			{
				if (((FoundSetManager)application.getFoundSetManager()).getSQLGenerator().getCachedTableSQLSheet(
					rel.getForeignDataSource()).getRelatedSQLDescription(name) != null)
				{
					return false;
				}
				fs.deletePersistCopy(rel, false);
			}
			catch (ServoyException e)
			{
				Debug.error(e);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * @deprecated Replaced by {@link #removeGlobalMethod(String,String)}
	 */
	@Deprecated
	public boolean js_removeGlobalMethod(String name)
	{
		return removeGlobalMethod(null, name);
	}

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
	@JSFunction
	public boolean removeGlobalMethod(String scopeName, String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ScriptMethod sm = fs.getScriptMethod(scopeName, name);
		if (sm != null)
		{
			fs.deletePersistCopy(sm, false);
			if (application.getFormManager() instanceof FormManager) ((FormManager)application.getFormManager()).fillScriptMenu();
			return true;
		}
		return false;
	}

	/**
	 * @deprecated Replaced by {@link #removeGlobalVariable(String,String)}
	 */
	@Deprecated
	public boolean js_removeGlobalVariable(String name)
	{
		return removeGlobalVariable(null, name);
	}

	/**
	 * Removes the specified global variable.
	 *
	 * @sample
	 * var v1 = solutionModel.newGlobalVariable('globals', 'globalVar1', JSVariable.INTEGER);
	 * var v2 = solutionModel.newGlobalVariable('globals', 'globalVar2', JSVariable.TEXT);
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
	@JSFunction
	public boolean removeGlobalVariable(String scopeName, String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ScriptVariable sv = fs.getScriptVariable(scopeName, name);
		if (sv != null)
		{
			fs.deletePersistCopy(sv, false);
			return true;
		}
		return false;
	}

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
	 * var f = solutionModel.newForm("newForm",databaseManager.getDataSource('example_data', 'orders'),null,false,500,350);
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
	@JSFunction
	public boolean removeMedia(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Media mediaItem = fs.getMedia(name);
		if (mediaItem != null)
		{
			fs.deletePersistCopy(mediaItem, false);
			return true;
		}
		return false;
	}

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
	@JSFunction
	public boolean removeStyle(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Style style = fs.getStyle(name);
		if (style != null)
		{
			fs.removeStyle(name);
			return true;
		}
		return false;
	}

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
	@JSFunction
	public boolean removeValueList(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ValueList valueList = fs.getValueList(name);
		if (valueList != null)
		{
			fs.deletePersistCopy(valueList, false);
			return true;
		}
		return false;
	}

	/**
	 * Removes the calculation specified by name and datasource.
	 *
	 * @sample
	 * var calc1 = solutionModel.newCalculation("function myCalculation1() { return 123; }", JSVariable.INTEGER, "db:/example_data/customers");
	 * var calc2 = solutionModel.newCalculation("function myCalculation2() { return '20'; }", "db:/example_data/customers");
	 *
	 * var c = solutionModel.getCalculation("myCalculation1", "db:/example_data/customers");
	 * application.output("Name: " + c.getName() + ", Stored: " + c.isStored());
	 *
	 * solutionModel.removeCalculation("myCalculation1", "db:/example_data/customers");
	 * c = solutionModel.getCalculation("myCalculation1", "db:/example_data/customers");
	 * if (c != null) {
	 * 	application.output("myCalculation could not be removed.");
	 * }
	 *
	 * var allCalcs = solutionModel.getCalculations("db:/example_data/customers");
	 * for (var i = 0; i < allCalcs.length; i++) {
	 * 	application.output(allCalcs[i]);
	 * }
	 *
	 * @param name the name of the calculation to be removed
	 * @param datasource the datasource the calculation belongs to
	 *
	 * @return true if the removal was successful, false otherwise
	 * @deprecated replaced with solutionModel.getDataSourceNode(dataSource).removeCalculation(name)
	 */
	@Deprecated
	public boolean js_removeCalculation(String name, String datasource)
	{
		return getDataSourceNode(datasource).removeCalculation(name);
	}

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
	 * @return a JSForm object
	 */
	@JSFunction
	public JSForm revertForm(String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		Form form = fs.revertForm(name);
		if (form != null)
		{
			application.getFormManager().addForm(form, false);
			return instantiateForm(form, false);
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
	@JSFunction
	public JSForm getForm(String name)
	{
		if (name == null) return null;
		Form form = application.getFormManager().getPossibleForm(name);

		if (form == null)
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			form = fs.getForm(name);
			if (form == null)
			{
				// search ignoring case
				Iterator<Form> forms = fs.getForms(false);
				String lowerCaseName = Utils.toEnglishLocaleLowerCase(name);
				Form f;
				while (forms.hasNext() && form == null)
				{
					f = forms.next();
					if (Utils.toEnglishLocaleLowerCase(f.getName()).equals(lowerCaseName)) form = f;
				}
			}
		}

		if (form != null)
		{
			return instantiateForm(form, false);
		}
		return null;
	}

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
	 * @return an array of JSForm type elements
	 */
	@JSFunction
	public JSForm[] getForms(String datasource)
	{
		if (datasource == null) throw new IllegalArgumentException("SolutionModel.getForms() param datasource (server/table) is null");
		return getFormsInternal(datasource);
	}

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
	 * @return an array of JSForm type elements
	 */
	@JSFunction
	public JSForm[] getForms(String server, String tablename)
	{
		return getForms(DataSourceUtils.createDBTableDataSource(server, tablename));
	}

	/**
	 * Get an array of all forms.
	 *
	 * @sample
	 * var forms = solutionModel.getForms()
	 * for (var i in forms)
	 * 	application.output(forms[i].name)
	 *
	 * @return an array of JSForm type elements
	 */
	@JSFunction
	public JSForm[] getForms()
	{
		return getFormsInternal(null);
	}

	private JSForm[] getFormsInternal(String datasource)
	{
		FlattenedSolution fs = application.getFlattenedSolution();

		Iterator<Form> forms = fs.getForms(datasource, true);

		ArrayList<JSForm> list = new ArrayList<JSForm>();
		while (forms.hasNext())
		{
			list.add(instantiateForm(forms.next(), false));
		}
		return list.toArray(new JSForm[list.size()]);
	}


	/**
	 * Gets the specified data source node and returns information about the form (see JSDataSourceNode node).
	 * The JSDataSourceNode holds all calculations and foundset methods.
	 *
	 * @sample
	 * var dsnode = solutionModel.getDataSourceNode('db:/example_data/customers');
	 * var c = dsnode.getCalculation("myCalculation");
	 * application.output("Name: " + c.getName() + ", Stored: " + c.isStored());
	 *
	 * @param dataSource table data source
	 *
	 * @return a JSDataSourceNode
	 */
	@JSFunction
	public JSDataSourceNode getDataSourceNode(String dataSource)
	{
		try
		{
			if (application.getFoundSetManager().getTable(dataSource) == null)
			{
				throw new RuntimeException("No table found for datasource: " + dataSource);
			}

			return new JSDataSourceNode(application, dataSource);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
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
	@JSFunction
	public JSMedia getMedia(String name)
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
	@JSFunction
	public JSMedia newMedia(String name, byte[] bytes)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			Media media = fs.getSolutionCopy().createNewMedia(new ScriptNameValidator(fs), name);
			media.setPermMediaData(bytes);
			media.setMimeType(MimeTypes.getContentType(bytes));
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
	@JSFunction
	public JSMedia[] getMediaList()
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
	@JSFunction
	public JSValueList getValueList(String name)
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
	 * var valueLists = solutionModel.getValueLists();
	 * if (valueLists != null && valueLists.length != 0)
	 * 	for (var i in valueLists)
	 * 		application.output(valueLists[i].name);
	 *
	 * @return an array of JSValueList objects
	 */
	@JSFunction
	public JSValueList[] getValueLists()
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
	@JSFunction
	public JSValueList newValueList(String name, int type)
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
	 * @deprecated Replaced by {@link #newGlobalVariable(String,String,int)}
	 */
	@Deprecated
	public JSVariable js_newGlobalVariable(String name, int type)
	{
		return newGlobalVariable(null, name, type);
	}

	/**
	 * Creates a new global variable with the specified name and number type.
	 *
	 * NOTE: The global variable number type is based on the value assigned from the SolutionModel-JSVariable node; for example: JSVariable.INTEGER.
	 *
	 * @sample
	 * var myGlobalVariable = solutionModel.newGlobalVariable('globals', 'newGlobalVariable', JSVariable.INTEGER);
	 * myGlobalVariable.defaultValue = 12;
	 * //myGlobalVariable.defaultValue = "{a:'First letter',b:'Second letter'}" // an js object, type must be media.
	 * //myGlobalVariable.defaultValue = '"some text"'; // Use two pairs of quotes if you want to assign a String as default value.
	 * @param scopeName the scope in which the variable is created
	 * @param name the specified name for the global variable
	 *
	 * @param type the specified number type for the global variable
	 *
	 * @return a JSVariable object
	 */
	@JSFunction
	public JSVariable newGlobalVariable(String scopeName, String name, int type)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			String scope = scopeName == null ? ScriptVariable.GLOBAL_SCOPE : scopeName;
			ScriptVariable variable = fs.getSolutionCopy().createNewScriptVariable(new ScriptNameValidator(application.getFlattenedSolution()), scope, name,
				type);
			application.getScriptEngine().getScopesScope().getGlobalScope(scope).put(variable);
			return new JSVariable(application, variable, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @deprecated Replaced by {@link #getGlobalVariable(String,String)}
	 */
	@Deprecated
	public JSVariable js_getGlobalVariable(String name)
	{
		return getGlobalVariable(null, name);
	}

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
	 * @return a JSVariable
	 */
	@JSFunction
	public JSVariable getGlobalVariable(String scopeName, String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ScriptVariable variable = fs.getScriptVariable(scopeName, name);
		if (variable != null)
		{
			return new JSVariable(application, variable, false);
		}
		return null;
	}

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
	@JSFunction
	public String[] getScopeNames()
	{
		Collection<String> scopeNames = application.getFlattenedSolution().getScopeNames();
		return scopeNames.toArray(new String[scopeNames.size()]);
	}

	/**
	 * Gets an array of all global variables.
	 *
	 * @sample
	 * var globalVariables = solutionModel.getGlobalVariables('globals');
	 * for (var i in globalVariables)
	 * 	application.output(globalVariables[i].name + " has the default value of " + globalVariables[i].defaultValue);
	 *
	 * @return an array of JSVariable type elements
	 *
	 */
	@JSFunction
	public JSVariable[] getGlobalVariables()
	{
		return getGlobalVariables(null);
	}

	/**
	 * @clonedesc getGlobalVariables()
	 * @sampleas getGlobalVariables()
	 * @param scopeName limit to global vars of specified scope name
	 *
	 * @return an array of JSVariable type elements
	 */
	@JSFunction
	public JSVariable[] getGlobalVariables(String scopeName)
	{
		List<JSVariable> variables = new ArrayList<JSVariable>();
		Iterator<ScriptVariable> scriptVariables = application.getFlattenedSolution().getScriptVariables(scopeName, true);
		while (scriptVariables.hasNext())
		{
			variables.add(new JSVariable(application, scriptVariables.next(), false));
		}
		return variables.toArray(new JSVariable[variables.size()]);
	}

	/**
	 * @deprecated Replaced by {@link #newGlobalMethod(String,String)}
	 */
	@Deprecated
	public JSMethod js_newGlobalMethod(String code)
	{
		return newGlobalMethod(null, code);
	}

	/**
	 * Creates a new global method with the specified code in a scope.
	 *
	 * @sample
	 * var method = solutionModel.newGlobalMethod('globals', 'function myglobalmethod(){foundset.newRecord()}')
	 *
	 * @param scopeName the scope in which the method is created
	 * @param code the specified code for the global method
	 *
	 * @return a JSMethod object
	 */
	@JSFunction
	public JSMethod newGlobalMethod(String scopeName, String code)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		String name = JSMethod.parseName(code);

		try
		{
			String scope = scopeName == null ? ScriptVariable.GLOBAL_SCOPE : scopeName;
			ScriptMethod method = fs.getSolutionCopy().createNewGlobalScriptMethod(new ScriptNameValidator(application.getFlattenedSolution()), scope, name);
			method.setDeclaration(code);
			application.getScriptEngine().getScopesScope().getGlobalScope(scope).put(method, method);
			JSMethod jsMethod = new JSMethod(method, application, true);
			return jsMethod;
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @deprecated Replaced by {@link #getGlobalMethod(String,String)}
	 */
	@Deprecated
	public JSMethod js_getGlobalMethod(String name)
	{
		return getGlobalMethod(null, name);
	}

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
	 * @return a JSMethod
	 */
	@JSFunction
	public JSMethod getGlobalMethod(String scopeName, String name)
	{
		FlattenedSolution fs = application.getFlattenedSolution();
		ScriptMethod sm = fs.getScriptMethod(scopeName, name);
		if (sm != null)
		{
			return new JSMethod(sm, application, false);
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
	 * button.onAction = solutionModel.wrapMethodWithArguments(method, [null, true, 42, quotedString])
	 *
	 * @param method JSMethod to be assigned to an event
	 *
	 * @param args positional arguments
	 *
	 * @return a JSMethod
	 */
	@JSFunction
	public JSMethod wrapMethodWithArguments(IBaseSMMethod method, Object... args)
	{
		if (method == null || args == null || args.length == 0)
		{
			return (JSMethod)method;
		}
		return new JSMethodWithArguments((JSMethod)method, args);
	}

	/**
	 * The list of all global methods.
	 *
	 * @sample
	 * var methods = solutionModel.getGlobalMethods('globals');
	 * for (var x in methods)
	 * 	application.output(methods[x].getName());
	 *
	 * @return an array of JSMethod type elements
	 *
	 */
	@JSFunction
	public JSMethod[] getGlobalMethods()
	{
		return getGlobalMethods(null);
	}

	/**
	 * @clonedesc getGlobalMethods()
	 * @sampleas getGlobalMethods()
	 * @param scopeName limit to global methods of specified scope name
	 * @return an array of JSMethod type elements
	 */
	@JSFunction
	public JSMethod[] getGlobalMethods(String scopeName)
	{
		List<JSMethod> methods = new ArrayList<JSMethod>();
		Iterator<ScriptMethod> scriptMethods = application.getFlattenedSolution().getScriptMethods(scopeName, true);
		while (scriptMethods.hasNext())
		{
			methods.add(new JSMethod(scriptMethods.next(), application, false));
		}
		return methods.toArray(new JSMethod[methods.size()]);
	}

	/**
	 * Creates a new JSRelation Object with a specified name; includes the primary server and table name, foreign server and table name, and the type of join for the new relation.
	 *
	 * @deprecated  As of release 6.0, deprecated because of ambigous parameters.
	 *
	 * @sample
	 * var rel = solutionModel.newRelation('myRelation', myPrimaryServerName, myPrimaryTableName, myForeignServerName, myForeignTableName, JSRelation.INNER_JOIN);
	 * application.output(rel.getRelationItems());
	 *
	 * @param name the specified name of the new relation
	 *
	 * @param primaryServerName the specified name of the primary server
	 *
	 * @param primaryTableName the specified name of the primary table
	 *
	 * @param foreignServerName the specified name of the foreign server
	 *
	 * @param foreignTableName the specified name of the foreign table
	 *
	 * @param joinType the type of join for the new relation; JSRelation.INNER_JOIN, JSRelation.LEFT_OUTER_JOIN
	 *
	 * @return a JSRelation object
	 */
	@Deprecated
	public JSRelation js_newRelation(String name, String primaryServerName, String primaryTableName, String foreignServerName, String foreignTableName,
		int joinType)
	{
		return newRelation(name, DataSourceUtils.createDBTableDataSource(primaryServerName, primaryTableName),
			DataSourceUtils.createDBTableDataSource(foreignServerName, foreignTableName), joinType);
	}

	/**
	 * Creates a new JSRelation Object with a specified name; includes the primary server and table name, foreign server and table name, and the type of join for the new relation.
	 *
	 * @deprecated  As of release 6.0, deprecated because of ambigous parameters.
	 *
	 * @sample
	 * var rel = solutionModel.newRelation('myRelation', myPrimaryServerName, myPrimaryTableName, myForeignServerName, myForeignTableName, JSRelation.INNER_JOIN);
	 * application.output(rel.getRelationItems());
	 *
	 * @param name the specified name of the new relation
	 *
	 * @param primaryDataSourceOrServer the specified name of the primary server
	 *
	 * @param primaryTableNameOrForeignServer the specified name of the primary table
	 *
	 * @param foreignDataSourceOrTable the specified name of the foreign server
	 *
	 * @param joinType the type of join for the new relation; JSRelation.INNER_JOIN, JSRelation.LEFT_OUTER_JOIN
	 *
	 * @return a JSRelation object
	 */
	@Deprecated
	public JSRelation js_newRelation(String name, String primaryDataSourceOrServer, String primaryTableNameOrForeignServer, String foreignDataSourceOrTable,
		int joinType)
	{
		if (primaryDataSourceOrServer.indexOf(':') == -1)
		{
			return newRelation(name, DataSourceUtils.createDBTableDataSource(primaryDataSourceOrServer, primaryTableNameOrForeignServer),
				foreignDataSourceOrTable, joinType);
		}
		return newRelation(name, primaryDataSourceOrServer, DataSourceUtils.createDBTableDataSource(primaryTableNameOrForeignServer, foreignDataSourceOrTable),
			joinType);
	}


	/**
	 * Creates a new JSRelation Object with a specified name; includes the primary datasource, foreign datasource and the type of join for the new relation.
	 *
	 * @sample
	 * var rel = solutionModel.newRelation('myRelation', myPrimaryDataSource, myForeignDataSource, JSRelation.INNER_JOIN);
	 * application.output(rel.getRelationItems());
	 *
	 * @param name the specified name of the new relation
	 *
	 * @param primaryDataSource the specified name of the primary datasource
	 *
	 * @param foreignDataSource the specified name of the foreign datasource
	 *
	 * @param joinType the type of join for the new relation; JSRelation.INNER_JOIN, JSRelation.LEFT_OUTER_JOIN
	 *
	 * @return a JSRelation object
	 */
	@JSFunction
	public JSRelation newRelation(String name, String primaryDataSource, String foreignDataSource, int joinType)
	{
		if (name == null || primaryDataSource == null || foreignDataSource == null)
		{
			return null;
		}
		if (joinType != ISMRelation.INNER_JOIN && joinType != ISMRelation.LEFT_OUTER_JOIN)
		{
			throw new IllegalArgumentException("JoinType has to be one of JSRelation.INNER_JOIN, JSRelation.LEFT_OUTER_JOIN: " + joinType);
		}
		try
		{
			if (application.getFoundSetManager().getTable(primaryDataSource) == null)
			{
				throw new RuntimeException("Can't create relation '" + name + "' because primaryDataSource '" + primaryDataSource + "' doesn't exist");
			}
		}
		catch (RepositoryException e1)
		{
			throw new RuntimeException("Can't create relation '" + name + "' because primaryDataSource '" + primaryDataSource + "' doesn't exist");
		}
		try
		{
			if (application.getFoundSetManager().getTable(foreignDataSource) == null)
			{
				throw new RuntimeException("Can't create relation '" + name + "' because foreignDataSource '" + foreignDataSource + "' doesn't exist");
			}
		}
		catch (RepositoryException e1)
		{
			throw new RuntimeException("Can't create relation '" + name + "' because foreignDataSource '" + foreignDataSource + "' doesn't exist");
		}

		FlattenedSolution fs = application.getFlattenedSolution();
		try
		{
			Relation relation = fs.getSolutionCopy().createNewRelation(new ScriptNameValidator(fs), name, primaryDataSource, foreignDataSource, joinType);
			return new JSRelation(relation, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an existing relation by the specified name and returns a JSRelation Object.
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
	 * @return a JSRelation
	 */
	@JSFunction
	public JSRelation getRelation(String name)
	{
		if (name == null) return null;
		FlattenedSolution fs = application.getFlattenedSolution();
		Relation relation = fs.getRelation(name);
		if (relation == null)
		{
			// search ignoring case
			try
			{
				Iterator<Relation> relations = fs.getRelations(false);
				String lowerCaseName = Utils.toEnglishLocaleLowerCase(name);
				Relation r;
				while (relations.hasNext() && relation == null)
				{
					r = relations.next();
					if (Utils.toEnglishLocaleLowerCase(r.getName()).equals(lowerCaseName)) relation = r;
				}
			}
			catch (RepositoryException e)
			{
				// not found then
			}
		}
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
	 * var relations = solutionModel.getRelations('server_name','table_name');
	 * if (relations.length != 0)
	 * 	for (var i in relations)
	 * 		application.output(relations[i].name);
	 *
	 * @param datasource the specified name of the datasource for the specified table
	 *
	 * @return an array of all relations (all elements in the array are of type JSRelation)
	 */

	@JSFunction
	public JSRelation[] getRelations(String datasource)
	{
		String servername = null;
		String tablename = null;
		String[] names = DataSourceUtilsBase.getDBServernameTablename(datasource);
		if (names != null && names.length == 2)
		{
			servername = names[0];
			tablename = names[1];
		}

		return getRelations(servername, tablename);
	}

	/**
	 * @clonedesc getRelations(String)
	 * @sampleas getRelations(String)
	 * @param servername the specified name of the server for the specified table
	 * @param tablename the specified name of the table
	 *
	 * @return an array of all relations (all elements in the array are of type JSRelation)
	 */
	@JSFunction
	public JSRelation[] getRelations(String servername, String tablename)
	{
		FlattenedSolution fs = application.getFlattenedSolution();

		try
		{
			Table primaryTable = null;
			if (servername != null && tablename != null)
			{
				IServer primaryServer = fs.getSolution().getServer(servername);
				if (primaryServer == null) throw new RuntimeException("can't list relations, primary server not found: " + servername);
				primaryTable = (Table)primaryServer.getTable(tablename);
				if (primaryTable == null) throw new RuntimeException("can't list relations, primary table not found: " + tablename);
			}

			List<JSRelation> relations = new ArrayList<JSRelation>();
			Iterator<Relation> iterator = fs.getRelations(primaryTable, true, true);

			while (iterator.hasNext())
			{
				Relation relation = iterator.next();
				if (((primaryTable == null) == relation.isGlobal()) && !relation.isInternal())
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
	 * Gets an array of all relations.
	 *
	 * @sample
	 * var relations = solutionModel.getAllRelations();
	 * if (relations.length != 0)
	 * 	for (var i in relations)
	 * 		application.output(relations[i].name);
	 *
	 * @return an array of all relations (all elements in the array are of type JSRelation)
	 */

	@JSFunction
	public JSRelation[] getAllRelations()
	{
		FlattenedSolution fs = application.getFlattenedSolution();

		try
		{
			List<JSRelation> relations = new ArrayList<JSRelation>();
			Iterator<Relation> iterator = fs.getRelations(true);

			while (iterator.hasNext())
			{
				relations.add(new JSRelation(iterator.next(), application, false));
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
	 *
	 * @deprecated replaced with solutionModel.getDataSourceNode(dataSource).getCalculations()
	 */
	@Deprecated
	public JSCalculation[] js_getCalculations(String datasource)
	{
		return getDataSourceNode(datasource).getCalculations();
	}

	/**
	 * Get an existing calculation for the given name and datasource.
	 *
	 * @param name The name of the calculation
	 * @param datasource The datasource the calculation belongs to.
	 *
	 * @deprecated replaced with solutionModel.getDataSourceNode(dataSource).getCalculation(name)
	 */
	@Deprecated
	public JSCalculation js_getCalculation(String name, String datasource)
	{
		return getDataSourceNode(datasource).getCalculation(name);
	}


	/**
	 * Creates a new calculation for the given code, the type will be the column where it could be build on (if name is a column name),
	 * else it will default to JSVariable.TEXT;
	 *
	 * @param code The code of the calculation, this must be a full function declaration.
	 * @param datasource The datasource this calculation belongs to.
	 *
	 * @deprecated replaced with solutionModel.getDataSourceNode(dataSource).newCalculation(code)
	 */
	@Deprecated
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
	 *
	 * @sample
	 * var calc = solutionModel.newCalculation("function myCalculation() { return 123; }", JSVariable.INTEGER, "db:/example_data/customers");
	 * var calc2 = solutionModel.newCalculation("function myCalculation2() { return '20'; }", "db:/example_data/customers");
	 * var calc3 = solutionModel.newCalculation("function myCalculation3() { return 'Hello World!'; }",	JSVariable.TEXT, "db:/example_data/employees");
	 *
	 * var c = solutionModel.getCalculation("myCalculation","db:/example_data/customers");
	 * application.output("Name: " + c.getName() + ", Stored: " + c.isStored());
	 *
	 * var allCalcs = solutionModel.getCalculations("db:/example_data/customers");
	 * for (var i = 0; i < allCalcs.length; i++) {
	 * 	application.output(allCalcs[i]);
	 * }
	 * @deprecated replaced with solutionModel.getDataSourceNode(dataSource).newCalculation(code, type)
	 */
	@Deprecated
	public JSCalculation js_newCalculation(String code, int type, String datasource)
	{
		return getDataSourceNode(datasource).newCalculation(code, type);
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
	 */

	@JSFunction
	public String createPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin)
	{
		return createPageFormat(width, height, leftmargin, rightmargin, topmargin, bottommargin, PageFormat.PORTRAIT, ISMUnits.PIXELS);
	}

	/**
	 * @clonedesc createPageFormat(double, double, double, double, double, double)
	 * @sampleas createPageFormat(double, double, double, double, double, double)
	 * @param width the specified width of the page to be printed.
	 * @param height the specified height of the page to be printed.
	 * @param leftmargin the specified left margin of the page to be printed.
	 * @param rightmargin the specified right margin of the page to be printed.
	 * @param topmargin the specified top margin of the page to be printed.
	 * @param bottommargin the specified bottom margin of the page to be printed.
	 * @param orientation the specified orientation of the page to be printed; the default is Portrait mode
	 */
	@JSFunction
	public String createPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin, int orientation)
	{
		return createPageFormat(width, height, leftmargin, rightmargin, topmargin, bottommargin, orientation, ISMUnits.PIXELS);
	}

	/**
	 * @clonedesc createPageFormat(double, double, double, double, double, double)
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
	@JSFunction
	public String createPageFormat(double width, double height, double leftmargin, double rightmargin, double topmargin, double bottommargin, int orientation,
		int units)
	{
		PageFormat pf = Utils.createPageFormat(width, height, leftmargin, rightmargin, topmargin, bottommargin, orientation, units);
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
	@JSFunction
	public String createFont(String name, int style, int size)
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
	@JSFunction
	public String createEmptyBorder(int top_width, int right_width, int bottom_width, int left_width)
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
	@JSFunction
	public String createEtchedBorder(int bevel_type, String highlight_color, String shadow_color)
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
	@JSFunction
	public String createBevelBorder(int bevel_type, String highlight_outer_color, String highlight_inner_color, String shadow_outer_color,
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
	@JSFunction
	public String createLineBorder(int thick, String color)
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
	@JSFunction
	public String createTitledBorder(String title_text, String font, String color, int title_justification, int title_position)
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
	@JSFunction
	public String createMatteBorder(int top_width, int right_width, int bottom_width, int left_width, String color)
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
	@JSFunction
	public String createSpecialMatteBorder(int top_width, int right_width, int bottom_width, int left_width, String top_color, String right_color,
		String bottom_color, String left_color, float rounding_radius, float[] dash_pattern)
	{
		SpecialMatteBorder border = new SpecialMatteBorder(top_width, left_width, bottom_width, right_width, PersistHelper.createColor(top_color),
			PersistHelper.createColor(right_color), PersistHelper.createColor(bottom_color), PersistHelper.createColor(left_color));
		border.setRoundingRadius(rounding_radius);
		if (dash_pattern != null) border.setDashPattern(dash_pattern);
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
	@JSFunction
	public String createRoundedBorder(int top_width, int right_width, int bottom_width, int left_width, String top_color, String right_color,
		String bottom_color, String left_color, float[] rounding_radius, String[] border_style)
	{
		RoundedBorder border = new RoundedBorder(top_width, left_width, bottom_width, right_width, PersistHelper.createColor(top_color),
			PersistHelper.createColor(right_color), PersistHelper.createColor(bottom_color), PersistHelper.createColor(left_color));
		border.setRoundingRadius(rounding_radius);
		if (border_style != null) border.setBorderStyles(border_style);
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

	/**
	 * @param parent
	 * @param field
	 * @param application2
	 * @param isNew
	 * @return
	 */
	public JSField createField(IJSParent< ? > parent, Field field, boolean isNew)
	{
		return new JSField(parent, field, application, isNew);
	}

	public JSLayoutContainer createLayoutContainer(IJSParent< ? > parent, LayoutContainer container)
	{
		return new JSLayoutContainer(parent, application, container);
	}

	/**
	 * Retrieves an element by its uuid.
	 * @sample
	 * solutionModel.getObjectByUUID(uuid)
	 * @param uuid element uuid
	 * @return found element
	 */
	@JSFunction
	public Object getObjectByUUID(Object uuid)
	{
		UUID parsedUUID = Utils.getAsUUID(uuid, false);
		if (parsedUUID != null)
		{
			IPersist persist = application.getFlattenedSolution().searchPersist(parsedUUID);
			if (persist instanceof ValueList)
			{
				return new JSValueList((ValueList)persist, application, false);
			}
			if (persist instanceof Form)
			{
				return instantiateForm((Form)persist, false);
			}
			if (persist instanceof Media)
			{
				return new JSMedia((Media)persist, application.getFlattenedSolution(), false);
			}
			if (persist instanceof Relation)
			{
				return new JSRelation((Relation)persist, application, false);
			}
			if (persist instanceof ScriptVariable)
			{
				return new JSVariable(application, (ScriptVariable)persist, false);
			}
			if (persist instanceof ScriptMethod)
			{
				return new JSMethod((ScriptMethod)persist, application, false);
			}
			if (persist instanceof Style)
			{
				return new JSStyle(application, (Style)persist, false);
			}
			if (persist instanceof Part)
			{
				return JSPart.createPart((JSForm)getParentContainer(persist), (Part)persist, false);
			}
			if (persist instanceof LayoutContainer)
			{
				return new JSLayoutContainer(getParentContainer(persist), application, (LayoutContainer)persist);
			}
			if (persist instanceof Field)
			{
				return JSField.createField(getParentContainer(persist), (Field)persist, application, false);
			}
			if (persist instanceof TabPanel)
			{
				return new JSTabPanel(getParentContainer(persist), (TabPanel)persist, application, false);
			}
			if (persist instanceof Portal)
			{
				return new JSPortal(getParentContainer(persist), (Portal)persist, application, false);
			}
			if (persist instanceof Bean)
			{
				return new JSBean(getParentContainer(persist), (Bean)persist, false);
			}
			if (persist instanceof GraphicalComponent)
			{
				if (ComponentFactory.isButton((GraphicalComponent)persist))
				{
					return new JSButton(getParentContainer(persist), (GraphicalComponent)persist, application, false);
				}
				else
				{
					return new JSLabel(getParentContainer(persist), (GraphicalComponent)persist, application, false);
				}
			}
			if (persist instanceof WebComponent)
			{
				return JSBaseContainer.createWebComponent(getParentContainer(persist), (WebComponent)persist, application, false);
			}
		}
		return null;
	}

	private IJSParent< ? > getParentContainer(IPersist persist)
	{
		IJSParent< ? > parent = null;
		List<AbstractContainer> parentHierarchy = new ArrayList<AbstractContainer>();
		IPersist currentParent = persist.getParent();
		while (currentParent != null)
		{
			if (currentParent instanceof AbstractContainer)
			{
				parentHierarchy.add(0, (AbstractContainer)currentParent);
			}
			if (currentParent instanceof Form) break;
			currentParent = currentParent.getParent();
		}
		for (AbstractContainer container : parentHierarchy)
		{
			if (container instanceof LayoutContainer)
			{
				parent = new JSLayoutContainer(parent, application, (LayoutContainer)persist);
			}
			if (container instanceof Form)
			{
				parent = instantiateForm((Form)container, false);
			}
		}
		return parent;
	}
}
