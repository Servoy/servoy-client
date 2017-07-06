/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.solutionmodel.ISMDefaults;
import com.servoy.j2db.util.Debug;


/**
 * @author lvostinar
 *
 */
public abstract class JSBaseContainer /* implements IJSParent */
{
	private final IApplication application;

	public JSBaseContainer(IApplication application)
	{
		this.application = application;
	}

	public abstract void checkModification();

	public abstract AbstractContainer getContainer();

	public abstract AbstractContainer getFlattenedContainer();

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
			Field field = getContainer().createNewField(new Point(x, y));
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
			return JSField.createField((IJSParent)this, field, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}


	/**
	 * Create a new layout container. The location is used to determine the generated order in html markup.
	 *
	 * @deprecated use newLayoutContainer(position) instead
	 * @sample
	 * var container = form.newLayoutContainer(0,0);
	 * @param x location x
	 * @param y location y
	 * @return the new layout container
	 */
	@Deprecated
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int x, int y)
	{
		return createLayoutContainer(x, y);
	}

	protected JSLayoutContainer createLayoutContainer(int x, int y)
	{
		checkModification();
		try
		{
			AbstractContainer container = getContainer();
			Form form = (Form)container.getAncestor(IRepository.FORMS);
			if (form.isResponsiveLayout())
			{
				LayoutContainer layoutContainer = getContainer().createNewLayoutContainer();
				layoutContainer.setLocation(new Point(x, y));
				return application.getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent)this, layoutContainer);
			}
			else
			{
				//check if form was just created with the solution model and suggest correct method to use
				if (application.getFlattenedSolution().getSolutionCopy().getAllObjectsAsList().contains(form) &&
					!application.getSolution().getAllObjectsAsList().contains(form))
				{
					throw new RuntimeException(
						"Form " + form.getName() + " is not a responsive form, cannot add a layout container on it. Please use solutionModel.newForm('" +
							form.getName() + "',true) to create a responsive form.");
				}

				throw new RuntimeException("Form " + form.getName() +
					" is not responsive, cannot add a layout container on it. Please use a responsive form or create a responsive form with solutionModel.newForm(formName, true);");
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a new layout container. The position is used to determine the generated order in html markup.
	 * This method can only be used in responsive forms.
	 *
	 * If you want to use default values and so on from a layout package (like 12grid) or if you use the solution model
	 * to create a form that is saved back into the workspace (servoyDeveloper.save(form)) then you have to set the
	 * packageName and specName properties. So that it works later on in the designer.
	 *
	 * @sample
	 * var container = form.newLayoutContainer(1);
	 * @param position the position of JSWebComponent object in its parent container
	 * @return the new layout container
	 */
	@JSFunction
	public JSLayoutContainer newLayoutContainer(int position)
	{
		return createLayoutContainer(0, position);
	}

	/**
	 * Returns all JSLayoutContainers objects of this container
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var containers = frm.getLayoutContainers();
	 * for (var c in containers)
	 * {
	 * 		var fname = containers[c].name;
	 * 		application.output(fname);
	 * }
	 *
	 * @return all JSLayoutContainers objects of this container
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer[] getLayoutContainers()
	{
		List<JSLayoutContainer> containers = new ArrayList<JSLayoutContainer>();
		Iterator<LayoutContainer> iterator = getContainer().getLayoutContainers();
		while (iterator.hasNext())
		{
			containers.add(application.getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent)this, iterator.next()));
		}
		return containers.toArray(new JSLayoutContainer[containers.size()]);
	}

	/**
	 * Returns a JSLayoutContainer that has the given name of this container.
	 * Use findLayoutContainer() method to find a JSLayoutContainter through the hierarchy
	 *
	 * @sample
	 * var container = myForm.getLayoutContainer("row1");
	 * application.output(container.name);
	 *
	 * @param name the specified name of the container
	 *
	 * @return a JSLayoutContainer object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer getLayoutContainer(String name)
	{
		if (name == null) return null;

		Iterator<LayoutContainer> containers = getFlattenedContainer().getLayoutContainers();
		while (containers.hasNext())
		{
			LayoutContainer container = containers.next();
			if (name.equals(container.getName()))
			{
				return application.getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent)this, container);
			}
		}
		return null;
	}

	/**
	 * Returns a JSLayoutContainer that has the given name throughout the whole form hierarchy.
	 *
	 * @sample
	 * var container = myForm.findLayoutContainer("row1");
	 * application.output(container.name);
	 *
	 * @param name the specified name of the container
	 *
	 * @return a JSLayoutContainer object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSLayoutContainer findLayoutContainer(final String name)
	{
		if (name == null) return null;

		return (JSLayoutContainer)getFlattenedContainer().acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof LayoutContainer && name.equals(((LayoutContainer)o).getName()))
				{
					JSBaseContainer topContainer = JSBaseContainer.this;
					LayoutContainer lc = (LayoutContainer)o;
					topContainer = getParentContainer(topContainer, lc, application);

					return application.getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent< ? >)topContainer, lc);
				}
				return o instanceof ISupportFormElements ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}


		});
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
			GraphicalComponent gc = getContainer().createNewGraphicalComponent(new Point(x, y));
			gc.setSize(new Dimension(width, height));
			gc.setText(txt);
			if (action instanceof JSMethod)
			{
				JSButton button = new JSButton((IJSParent)this, gc, application, true);
				button.setOnAction((JSMethod)action);
				return button;
			}
			else
			{
				int id = getMethodId(action, gc, application);
				gc.setOnActionMethodID(id);
				return new JSButton((IJSParent)this, gc, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
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
			GraphicalComponent gc = getContainer().createNewGraphicalComponent(new Point(x, y));
			gc.setSize(new Dimension(width, height));
			gc.setText(txt);
			if (action instanceof JSMethod)
			{
				JSLabel label = new JSLabel((IJSParent)this, gc, application, true);
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
				return new JSLabel((IJSParent)this, gc, application, true);
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
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

	static int getMethodId(IApplication application, AbstractBase base, IBaseSMMethod method, TypedProperty<Integer> methodProperty)
	{
		if (method == null && methodProperty != null && BaseComponent.isCommandProperty(methodProperty.getPropertyName())) return -1;
		if (method == null || method == ISMDefaults.COMMAND_DEFAULT) return 0;
		return getMethodId(application, base, ((JSMethod)method).getScriptMethod());
	}

	public static int getMethodId(IApplication application, AbstractBase base, ScriptMethod method)
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

	public static ScriptMethod getScriptMethod(Function function, FlattenedSolution fs)
	{
		return fs.getScriptMethod((String)function.get("_scopename_", function), (String)function.get("_methodname_", function));
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
			Portal portal = getContainer().createNewPortal(name, new Point(x, y));
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
			return new JSPortal((IJSParent)this, portal, application, true);
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
		Iterator<Portal> portals = getFlattenedContainer().getPortals();
		while (portals.hasNext())
		{
			Portal portal = portals.next();
			if (name.equals(portal.getName()))
			{
				return new JSPortal((IJSParent)this, portal, application, false);
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
		Iterator<Portal> portals = getContainer().getPortals();
		while (portals.hasNext())
		{
			Portal portal = portals.next();
			if (name.equals(portal.getName()))
			{
				getContainer().removeChild(portal);
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
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<Portal> iterator = form2use.getPortals();
		while (iterator.hasNext())
		{
			portals.add(new JSPortal((IJSParent)this, iterator.next(), application, false));
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
			TabPanel tabPanel = getContainer().createNewTabPanel(name);
			tabPanel.setSize(new Dimension(width, height));
			tabPanel.setLocation(new Point(x, y));
			return new JSTabPanel((IJSParent)this, tabPanel, application, true);
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
		Iterator<TabPanel> tabPanels = getFlattenedContainer().getTabPanels();
		while (tabPanels.hasNext())
		{
			TabPanel tabPanel = tabPanels.next();
			if (name.equals(tabPanel.getName()))
			{
				return new JSTabPanel((IJSParent)this, tabPanel, application, false);
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
		Iterator<TabPanel> tabPanels = getContainer().getTabPanels();
		while (tabPanels.hasNext())
		{
			TabPanel tabPanel = tabPanels.next();
			if (name.equals(tabPanel.getName()))
			{
				getContainer().removeChild(tabPanel);
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
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<TabPanel> iterator = form2use.getTabPanels();
		while (iterator.hasNext())
		{
			tabPanels.add(new JSTabPanel((IJSParent)this, iterator.next(), application, false));
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	public JSField getField(String name)
	{
		if (name == null) return null;

		Iterator<Field> fields = getFlattenedContainer().getFields();
		while (fields.hasNext())
		{
			Field field = fields.next();
			if (name.equals(field.getName()))
			{
				return JSField.createField((IJSParent)this, field, application, false);
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
	 *
	 * @return true is the JSField has been successfully removed; false otherwise
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public boolean removeField(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<Field> fields = getContainer().getFields();
		while (fields.hasNext())
		{
			Field field = fields.next();
			if (name.equals(field.getName()))
			{
				getContainer().removeChild(field);
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
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<Field> iterator = form2use.getFields();
		while (iterator.hasNext())
		{
			fields.add(JSField.createField((IJSParent)this, iterator.next(), application, false));
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public JSButton getButton(String name)
	{
		if (name == null) return null;

		Iterator<GraphicalComponent> graphicalComponents = getFlattenedContainer().getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && ComponentFactory.isButton(button))
			{
				return new JSButton((IJSParent)this, button, application, false);
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public boolean removeButton(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<GraphicalComponent> graphicalComponents = getContainer().getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (name.equals(button.getName()) && ComponentFactory.isButton(button))
			{
				getContainer().removeChild(button);
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
	 * @return the list of all JSButtons on this forms
	 *
	 */
	@JSFunction
	public JSButton[] getButtons(boolean returnInheritedElements)
	{
		List<JSButton> buttons = new ArrayList<JSButton>();
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<GraphicalComponent> graphicalComponents = form2use.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent button = graphicalComponents.next();
			if (ComponentFactory.isButton(button))
			{
				buttons.add(new JSButton((IJSParent)this, button, application, false));
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
	 * @return the list of all JSButtons on this forms
	 *
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
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
			Bean bean = getContainer().createNewBean(name, className);
			bean.setSize(new Dimension(width, height));
			bean.setLocation(new Point(x, y));
			return new JSBean((IJSParent)this, bean, true);
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public JSBean getBean(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<Bean> beans = getFlattenedContainer().getBeans();
			while (beans.hasNext())
			{
				Bean bean = beans.next();
				if (name.equals(bean.getName()))
				{
					return new JSBean((IJSParent)this, bean, false);
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public boolean removeBean(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<Bean> beans = getContainer().getBeans();
		while (beans.hasNext())
		{
			Bean bean = beans.next();
			if (name.equals(bean.getName()))
			{
				getContainer().removeChild(bean);
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
	 * @return the list of all JSBeans on this forms
	 *
	 */
	@JSFunction
	public JSBean[] getBeans(boolean returnInheritedElements)
	{
		List<JSBean> beans = new ArrayList<JSBean>();
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<Bean> iterator = form2use.getBeans();
		while (iterator.hasNext())
		{
			beans.add(new JSBean((IJSParent)this, iterator.next(), false));
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
	 * @return the list of all JSBeans on this forms
	 *
	 */
	@JSFunction
	public JSBean[] getBeans()
	{
		return getBeans(false);
	}

	/**
	 * Returns a JSComponent that has the given name; if found it will be a JSField, JSLabel, JSButton, JSPortal, JSBean, JSWebComponent or JSTabPanel.
	 *
	 * @sample
	 * var frm = solutionModel.getForm("myForm");
	 * var cmp = frm.getComponent("componentName");
	 * application.output("Component type and name: " + cmp);
	 *
	 * @param name the specified name of the component
	 *
	 * @return a JSComponent object (might be a JSField, JSLabel, JSButton, JSPortal, JSBean, JSWebComponent or JSTabPanel)
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
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
		comp = getWebComponent(name);
		if (comp != null) return comp;
		return null;
	}

	/**
	 * Removes a component (JSLabel, JSButton, JSField, JSPortal, JSBean, JSTabpanel, JSWebComponent) that has the given name. It is the same as calling "if(!removeLabel(name) &amp;&amp; !removeButton(name) ....)".
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
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
		if (removeWebComponent(name)) return true;
		return false;
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean, JSWebComponent or JSTabPanel.
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
		lst.addAll(Arrays.asList(getWebComponents(returnInheritedElements)));
		return lst.toArray(new JSComponent[lst.size()]);
	}

	/**
	 * Returns a array of all the JSComponents that a form has; they are of type JSField,JSLabel,JSButton,JSPortal,JSBean, JSWebComponents or JSTabPanel.
	 *
	 * @sample
	 * var form = solutionModel.getForm("myForm");
	 * var components = form.getComponents();
	 * for (var i in components)
	 * 	application.output("Component type and name: " + components[i]);
	 *
	 * @return an array of all the JSComponents on the form.
	 */
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public JSLabel getLabel(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<GraphicalComponent> graphicalComponents = getFlattenedContainer().getGraphicalComponents();
			while (graphicalComponents.hasNext())
			{
				GraphicalComponent label = graphicalComponents.next();
				if (name.equals(label.getName()) && !ComponentFactory.isButton(label))
				{
					return new JSLabel((IJSParent)this, label, application, false);
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public boolean removeLabel(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<GraphicalComponent> graphicalComponents = getContainer().getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent label = graphicalComponents.next();
			if (name.equals(label.getName()) && !ComponentFactory.isButton(label))
			{
				getContainer().removeChild(label);
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
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<GraphicalComponent> graphicalComponents = form2use.getGraphicalComponents();
		while (graphicalComponents.hasNext())
		{
			GraphicalComponent gc = graphicalComponents.next();
			if (!ComponentFactory.isButton(gc))
			{
				labels.add(new JSLabel((IJSParent)this, gc, application, false));
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
	@ServoyClientSupport(mc = true, ng = true, wc = true, sc = true)
	@JSFunction
	public JSLabel[] getLabels()
	{
		return getLabels(false);
	}

	/**
	 * Creates a new JSWebComponent (spec based component) object on the form.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var bean = form.newWebComponent('bean','mypackage-testcomponent',200,200,300,300);
	 * forms['newForm1'].controller.show();
	 *
	 * @param name the specified name of the JSWebComponent object
	 * @param type the webcomponent name as it appears in the spec
	 * @param x the horizontal "x" position of the JSWebComponent object in pixels
	 * @param y the vertical "y" position of the JSWebComponent object in pixels
	 * @param width the width of the JSWebComponent object in pixels
	 * @param height the height of the JSWebComponent object in pixels
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String name, String type, int x, int y, int width, int height)
	{
		checkModification();
		try
		{
			WebComponent webComponent = getContainer().createNewWebComponent(name, type);
			webComponent.setSize(new Dimension(width, height));
			webComponent.setLocation(new Point(x, y));
			return createWebComponent((IJSParent)this, webComponent, application, true);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new JSWebComponent (spec based component) object on the RESPONSIVE form.
	 *
	 * @sample
	 * var form = solutionModel.newForm('newForm1', 'db:/server1/table1', null, true, 800, 600);
	 * var container = myForm.getLayoutContainer("row1")
	 * var bean = container.newWebComponent('bean','mypackage-testcomponent',1);
	 *
	 * @param name the specified name of the JSWebComponent object
	 * @param type the webcomponent name as it appears in the spec
	 * @param position the position of JSWebComponent object in its parent container
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent newWebComponent(String name, String type, int position)
	{
		checkModification();
		try
		{
			AbstractContainer container = getContainer();
			Form form = (Form)container.getAncestor(IRepository.FORMS);
			if (form.isResponsiveLayout())
			{
				WebComponent webComponent = container.createNewWebComponent(name, type);
				webComponent.setLocation(new Point(0, position));
				return createWebComponent((IJSParent)this, webComponent, application, true);
			}
			else
			{
				throw new RuntimeException("Form " + form.getName() + " is not responsive. Cannot create component without specifying the location and size.");
			}
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSWebComponent that has the given name that is a child of this layout container.
	 * Use findWebComponent() to find a webcomponent through the hierarchy
	 *
	 * @sample
	 * var btn = myForm.getWebComponent("mycomponent");
	 * application.output(mybean.typeName);
	 *
	 * @param name the specified name of the web component
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent getWebComponent(String name)
	{
		if (name == null) return null;

		try
		{
			Iterator<WebComponent> webComponents = getFlattenedContainer().getWebComponents();
			while (webComponents.hasNext())
			{
				WebComponent webComponent = webComponents.next();
				if (name.equals(webComponent.getName()))
				{
					return createWebComponent((IJSParent)this, webComponent, application, false);
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
	 * Returns a JSWebComponent that has the given name through the whole hierarchy of JSLayoutContainers
	 *
	 * @sample
	 * var btn = myForm.findWebComponent("mycomponent");
	 * application.output(mybean.typeName);
	 *
	 * @param name the specified name of the web component
	 *
	 * @return a JSWebComponent object
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent findWebComponent(final String name)
	{
		if (name == null) return null;

		return (JSWebComponent)getFlattenedContainer().acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof WebComponent && name.equals(((WebComponent)o).getName()))
				{
					JSBaseContainer topContainer = JSBaseContainer.this;
					WebComponent wc = (WebComponent)o;
					topContainer = getParentContainer(topContainer, wc, application);
					return createWebComponent((IJSParent< ? >)topContainer, wc, application, false);
				}
				return o instanceof ISupportFormElements ? CONTINUE_TRAVERSAL : CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});
	}

	/**
	 * Removes a JSWebComponent that has the specified name. Returns true if removal was successful, false otherwise.
	 *
	 * @sample
	 * var form = solutionModel.getForm('myform');
	 * form.removeWebComponent('mybean')
	 *
	 * @param name the specified name of the JSWebComponent to be removed
	 *
	 * @return true if the JSWebComponent has been removed; false otherwise
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public boolean removeWebComponent(String name)
	{
		if (name == null) return false;
		checkModification();
		Iterator<WebComponent> webComponents = getContainer().getWebComponents();
		while (webComponents.hasNext())
		{
			WebComponent webComponent = webComponents.next();
			if (name.equals(webComponent.getName()))
			{
				getContainer().removeChild(webComponent);
				return true;

			}
		}
		return false;
	}

	/**
	 * Returns all JSWebComponents of this form.
	 *
	 * @sample
	  * var webComponents = myForm.getWebComponents(false);
	 * for (var i in webComponents)
	 * {
	 * 	if (webComponents[i].name != null)
	 * 		application.output(webComponents[i].name);
	 * }
	 *
	 * @param returnInheritedElements boolean true to also return the elements from parent form
	 * @return the list of all JSWebComponents on this forms
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent[] getWebComponents(boolean returnInheritedElements)
	{
		List<JSWebComponent> webComponents = new ArrayList<JSWebComponent>();
		AbstractContainer form2use = returnInheritedElements ? getFlattenedContainer() : getContainer();
		Iterator<WebComponent> iterator = form2use.getWebComponents();
		while (iterator.hasNext())
		{
			webComponents.add(createWebComponent((IJSParent)this, iterator.next(), application, false));
		}
		return webComponents.toArray(new JSWebComponent[webComponents.size()]);
	}

	/**
	 * Returns all JSWebComponents of this form.
	 *
	 * @sample
	 * var webComponents = myForm.getWebComponents();
	 * for (var i in webComponents)
	 * {
	 * 	if (webComponents[i].name != null)
	 * 		application.output(webComponents[i].name);
	 * }
	 *
	 * @return the list of all JSWebComponent on this forms
	 *
	 */
	@ServoyClientSupport(mc = false, ng = true, wc = false, sc = false)
	@JSFunction
	public JSWebComponent[] getWebComponents()
	{
		return getWebComponents(false);
	}

	public static JSWebComponent createWebComponent(IJSParent< ? > parent, WebComponent baseComponent, IApplication application, boolean isNew)
	{
		if (application.getApplicationType() == IApplication.NG_CLIENT)
		{
			return new JSNGWebComponent(parent, baseComponent, application, isNew);
		}
		else
		{
			return new JSWebComponent(parent, baseComponent, application, isNew);
		}
	}

	/**
	 * @param topContainer
	 * @param lc
	 * @return
	 */
	private static JSBaseContainer getParentContainer(JSBaseContainer topContainer, IPersist lc, IApplication application)
	{
		ArrayList<ISupportChilds> parentHierarchy = new ArrayList<>();
		ISupportChilds parent = lc.getParent();
		while (parent != topContainer.getContainer() && !(parent instanceof Form))
		{
			parentHierarchy.add(parent);
			parent = parent.getParent();
		}
		for (int i = parentHierarchy.size(); --i >= 0;)
		{
			ISupportChilds container = parentHierarchy.get(i);
			if (container instanceof LayoutContainer)
			{
				topContainer = application.getScriptEngine().getSolutionModifier().createLayoutContainer((IJSParent< ? >)topContainer,
					(LayoutContainer)container);
			}
			else
			{
				throw new RuntimeException("unexpected parent: " + container); //$NON-NLS-1$
			}
		}
		return topContainer;
	}
}
