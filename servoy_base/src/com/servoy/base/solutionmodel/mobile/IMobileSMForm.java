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

package com.servoy.base.solutionmodel.mobile;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.scripting.solutionhelper.IBaseSHInsetList;
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMVariable;


/**
 * Solution model form object for mobile clients.
 * 
 * @author rgansevles
 * @author acostescu
 *
 * @since 7.0
 */
@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
public interface IMobileSMForm extends IBaseSMForm
{

	/**
	 * Creates a new JSField object on the form .
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource, null, true, 800, 600);
	 * var variable = form.newVariable('myVar', JSVariable.TEXT);
	 * variable.defaultValue = "'This is a default value (with triple quotes)!'";
	 * var field = form.newField(variable, JSField.TEXT_FIELD, 1);
	 * forms['newForm1'].controller.show();  	
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 *
	 * @param type the display type of the JSField object (see the Solution Model -> JSField node for display types)
	 *
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 *
	 */
	public IBaseSMField newField(IBaseSMVariable dataprovider, int type, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newField(IBaseSMVariable,int,int)
	 */
	public IBaseSMField newField(String dataprovider, int type, int y);

	/**
	 * Creates a new JSText field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the Text Field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var textField = form.newTextField(x,1);
	 * //or a column data provider as the dataprovider
	 * //textField.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSText field
	 */
	public IMobileSMText newTextField(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newTextField(IBaseSMVariable,int)
	 */
	public IMobileSMText newTextField(String dataprovider, int y);

	/**
	 * Creates a new JSTextArea field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var field = form.newTextArea(x,1);
	 * //or a column data provider as the dataprovider
	 * //field.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSTextArea field
	 */
	public IMobileSMTextArea newTextArea(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newTextArea(IBaseSMVariable,int)
	 */
	public IMobileSMTextArea newTextArea(String dataprovider, int y);

	/**
	 * Creates a new JSCombobox field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var field = form.newCombobox(x,1);
	 * //or a column data provider as the dataprovider
	 * //field.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSCombobox field
	 */
	public IMobileSMCombobox newCombobox(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newCombobox(IBaseSMVariable,int)
	 */
	public IMobileSMCombobox newCombobox(String dataprovider, int y);

	/**
	 * Creates a new JSRadios field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a global variable'";
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',JSVariable.TEXT);
	 * //x.defaultValue = "'Text from a form variable'";
	 * var field = form.newRadios(x,1);
	 * //or a column data provider as the dataprovider
	 * //field.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSRadios field
	 */
	public IMobileSMRadios newRadios(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newRadios(IBaseSMVariable,int)
	 */
	public IMobileSMRadios newRadios(String dataprovider, int y);

	/**
	 * Creates a new JSChecks field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.INTEGER);
	 * //x.defaultValue = "'1'";
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',JSVariable.INTEGER);
	 * //x.defaultValue = "'1'";
	 * var field = form.newCheck(x,1);
	 * //or a column data provider as the dataprovider
	 * //field.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSChecks field
	 */
	public IMobileSMChecks newCheck(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newCheck(IBaseSMVariable,int)
	 */
	public IMobileSMChecks newCheck(String dataprovider, int y);

	/**
	 * Creates a new JSPassword field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal',JSVariable.TEXT);
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar',JSVariable.TEXT);
	 * var field = form.newPassword(x,1);
	 * //or a column data provider as the dataprovider
	 * //field.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSPassword field
	 */
	public IMobileSMPassword newPassword(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newPassword(IBaseSMVariable,int)
	 */
	public IMobileSMPassword newPassword(String dataprovider, int y);

	/**
	 * Creates a new JSCalendar field on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1',myDatasource); 
	 * //choose the dataprovider or JSVariable you want for the field
	 * var x = null;
	 * //global JSVariable as the dataprovider 
	 * //x = solutionModel.newGlobalVariable('globals', 'myGlobal', JSVariable.DATETIME);
	 * //or a form JSVariable as the dataprovider 
	 * //x = form.newVariable('myFormVar', JSVariable.DATETIME);
	 * var field = form.newCalendar(x,1);
	 * //or a column data provider as the dataprovider
	 * //field.dataProviderID = columnTextDataProvider;
	 * forms['newForm1'].controller.show();
	 *
	 * @param dataprovider the specified dataprovider name/JSVariable of the JSField object
	 * @param y the vertical "y" position of the JSField object, defines the order of elements on the form
	 * 
	 * @return a new JSCalendar field
	 */
	public IMobileSMCalendar newCalendar(IBaseSMVariable dataprovider, int y);

	/**
	 * @sameas com.servoy.base.solutionmodel.mobile.IMobileSMForm#newCalendar(IBaseSMVariable,int)
	 */
	public IMobileSMCalendar newCalendar(String dataprovider, int y);

	/**
	 * Creates a new button on the form with the given text.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var method = form.newMethod('function onAction(event) { application.output("onAction intercepted on " + event.getFormName()); }');
	 * var button = form.newButton('myButton', 1, method);
	 * application.output("The new button: " + button.name + " has the following onAction event handling method assigned " + button.onAction.getName());
	 *
	 * @param txt the text on the button
	 *
	 * @param y the y coordinate of the button location on the form, defines the order of elements on the form
	 *
	 * @param jsmethod the method assigned to handle an onAction event
	 * 
	 * @return a new JSButton object
	 */
	public IBaseSMButton newButton(String txt, int y, IBaseSMMethod jsmethod);

	/**
	 * Creates a new JSLabel object on the form.
	 *
	 * @sample 
	 * var form = solutionModel.newForm('newForm1', myDatasource);
	 * var label = form.newLabel('The text on the label', 1);
	 * forms['newForm1'].controller.show(); 
	 *
	 * @param txt the specified text of the label object
	 *
	 * @param y the vertical "y" position of the label, defines the order of elements on the form
	 *
	 * @return a JSLabel object
	 */
	public IBaseSMLabel newLabel(String txt, int y);

	/**
	 * Creates a new Header part on the form.
	 * 
	 * @sample
	 * var header = form.newHeader();
	 * 
	 * @return A JSHeader instance corresponding to the newly created Header form part.
	 */
	public IMobileSMHeader newHeader();

	/**
	 * Get the Header part on the form if it exists.
	 * 
	 * @sample
	 * var header = form.getHeader();
	 * 
	 * @return A JSHeader or null when not found.
	 */
	public IMobileSMHeader getHeader();

	/**
	 * Removes a JSHeader if it exists.
	 *
	 * @sample 
	 * var form = solutionModel.getForm('myform');
	 * form.removeHeader()
	 * 
	 * @return true if the JSHeader has successfully been removed; false otherwise
	 */
	public boolean removeHeader();

	/**
	 * Creates a new Footer part on the form.
	 *
	 * @sample 
	 * var footer = form.newFooter();
	 * 
	 * @return A JSFooter instance corresponding to the newly created Footer form part.
	 */
	public IMobileSMFooter newFooter();

	/**
	 * Get the Footer part on the form if it exists.
	 * 
	 * @sample
	 * var footer = form.getFooter();
	 * 
	 * @return A JSFooter or null when not found.
	 */
	public IMobileSMFooter getFooter();

	/**
	 * Removes a JSFooter if it exists.
	 *
	 * @sample 
	 * var form = solutionModel.getForm('myform');
	 * form.removeFooter()
	 * 
	 * @return true if the JSFooter has successfully been removed; false otherwise
	 */
	public boolean removeFooter();

	/**
	 * Creates a new JSBean object on the form.
	 * 
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1');
	 * var bean = form.newBean('bean', 1);
	 * forms['newForm1'].controller.show();
	 * 
	 * @param name the specified name of the JSBean object
	 * @param y the vertical "y" position of the JSBean object, defines the order of elements on the form
	 * 
	 * @return a JSBean object 
	 */
	public IMobileSMBean newBean(String name, int y);

	/**
	 * Creates a new inset list mobile component in the given form. The inset list will be populated based on the given datasource and relation.
	 * 
	 * @param yLocation the vertical location of the inset list in the form's components list.
	 * @param relationName the relation used to show data, just like it would happen in a related tab-panel.
	 * @param headerText can be null; it's a convenience argument for setting the title (header text) for the inset list.
	 * @param textDataProviderID can be null; it's a convenience argument for setting the dataprovider that will be used to populate the main text area of the list's items.
	 * @return the newly created inset list.
	 * 
	 * @sample
	 * var f = solutionModel.newForm("created_by_sm_1","db:/udm/contacts");
	 * // create an inset list
	 * var insetList = f.newInsetList(8,"accountmanager_to_companies","Companies","company_name");
	 * insetList.subtextDataProviderID = "company_description";
	 * insetList.onAction = f.newMethod("function buttonPressed() { plugins.dialogs.showWarningDialog('Title', 'inset list clicked','OK'); }");
	 */
	public IBaseSHInsetList newInsetList(int yLocation, String relationName, String headerText, String textDataProviderID);

	/**
	 * Returns an existing inset list.
	 * 
	 * @param name the inset list's name.
	 * 
	 * @return the existing inset list, or null if it does not exist.
	 * 
	 * @sample
	 * var form = solutionModel.getForm("myform");
	 * var insetList = form.getInsetList('mylist1');
	 */
	public IBaseSHInsetList getInsetList(String name);

	/**
	 * Gets all insets lists on the form.
	 * 
	 * @sample
	 * var form = solutionModel.getForm('test');
	 * var insetLists = form.getInsetLists();
	 */
	public IBaseSHInsetList[] getInsetLists();

	/**
	 * Removes inset list from the form.
	 * 
	 * @param name Inset List name.
	 * 
	 * @sample
	 * var form = solutionModel.getForm('test');
	 * form.removeInsetList('myinsetlist');
	 */
	public boolean removeInsetList(String name);

	/**
	 * Sets the vertical order of components inside body (ordered by Y coordinate) or horizontal order of components inside footer (ordered by X coordinate).
	 * 
	 * @param components The components to order.
	 * 
	 * @sample
	 * var form = solutionModel.getForm('myForm');
	 * form.setComponentOrder([f.getField('field1'),f.getField('field2'),f.getButton('mybutton')]);
	 */
	public void setComponentOrder(IBaseSMComponent[] components);

}
