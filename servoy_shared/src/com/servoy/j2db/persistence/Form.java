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
package com.servoy.j2db.persistence;


import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.IForm;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.JSONWrapperMap;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A normal Servoy form
 *
 * @author jblok,jcompagner
 */
public class Form extends AbstractContainer implements ITableDisplay, ISupportScrollbars, ISupportScriptProviders, ISupportEncapsulation, ISupportDeprecated
{

	private static final long serialVersionUID = 1L;

	/**
	 * @sameas getNavigatorID()
	 */
	public static final int NAVIGATOR_DEFAULT = IFormConstants.DEFAULT;

	/**
	 * @sameas getNavigatorID()
	 */
	public static final int NAVIGATOR_NONE = IFormConstants.NAVIGATOR_NONE;

	/**
	 * @sameas getNavigatorID()
	 */
	public static final int NAVIGATOR_IGNORE = IFormConstants.NAVIGATOR_IGNORE;

	/**
	 * @clonedesc com.servoy.j2db.solutionmodel.ISMForm#EMPTY_FOUNDSET
	 */
	public static final String NAMED_FOUNDSET_EMPTY = "empty"; //$NON-NLS-1$

	/**
	 * @clonedesc com.servoy.j2db.solutionmodel.ISMForm#SEPARATE_FOUNDSET
	 */
	public static final String NAMED_FOUNDSET_SEPARATE = "separate"; //$NON-NLS-1$

	/**
	 * Constant used for prefixing the namedFoundset property. Prefixes global relations within the namedFoundset property.
	 */
	public static final String NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX = "gr_"; //$NON-NLS-1$

	public static final int NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH = NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX.length();

	public static Comparator<IFormElement> FORM_INDEX_COMPARATOR = new Comparator<IFormElement>()
	{
		public int compare(IFormElement element1, IFormElement element2)
		{
			return element1.getFormIndex() - element2.getFormIndex();
		}
	};

	public transient Form extendsForm;

	/**
	 * Constructor I
	 */
	protected Form(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.FORMS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Property Methods
	 */

	/**
	 * Set the background.
	 *
	 * @param arg the background
	 */
	public void setBackground(java.awt.Color arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND, arg);
	}

	/**
	 * Get the background.
	 *
	 * @return the background
	 */
	public java.awt.Color getBackground()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BACKGROUND);
	}

	/**
	 * Set the server name to use by this form.
	 *
	 * @param arg the server name to use
	 */
	public void setServerName(String arg)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(arg, getTableName()));
	}

	/**
	 * Get the server name used by this form.
	 *
	 */
	public String getServerName()
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(getDataSource());
		return stn == null ? null : stn[0];
	}


	/**
	 * Get the form size.
	 *
	 * @return the size
	 */
	@Override
	public Dimension getSize()
	{
		return checkParts(getParts(), getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE));
	}

	/**
	 * The width of the form in pixels.
	 */
	public int getWidth()
	{
		return getSize().width;
	}

	/**
	 * Set the width.
	 *
	 * @param width
	 */
	public void setWidth(int width)
	{
		setSize(new Dimension(width, getSize().height));
	}

	@Override
	public void clearProperty(String propertyName)
	{
		if ("width".equals(propertyName)) //$NON-NLS-1$
		{
			propertyName = StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName();
		}
		super.clearProperty(propertyName);
	}

	/**
	 * Set show in menu.
	 *
	 * @param arg the flag
	 */
	public void setShowInMenu(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU, arg);
	}

	/**
	 * When set, the form is displayed under the Window menu.
	 * If it is not set, the form will be 'hidden'.
	 * NOTE: This is only applicable for Servoy Client. Servoy Developer always shows all forms so that
	 * developers have access to all forms within a solution during development.
	 */
	public boolean getShowInMenu()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU).booleanValue();
	}

	/**
	 * Set the style name.
	 *
	 * @param arg the name
	 */
	public void setStyleName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLENAME, arg);
	}

	/**
	 * The names of the database server and table that this form is linked to.
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getDataSource()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE);
	}

	/**
	 * Set the data source.
	 *
	 * @param arg the data source uri
	 */
	public void setDataSource(String arg)
	{
		if (arg == null)
		{
			// cannot override form data source from superform with null, always remove the dataSource property when set to null
			clearTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE);
		}
		else
		{
			setTypedProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE, arg);
		}
	}

	/**
	 * The name of the Servoy style that is being used on the form.
	 */
	public String getStyleName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLENAME);
	}

	/**
	 * Set the table name.
	 *
	 * @param arg the table to use
	 */
	public void setTableName(String arg)
	{
		setDataSource(DataSourceUtils.createDBTableDataSource(getServerName(), arg));
	}

	/**
	 * The [name of the table/SQL view].[the name of the database server connection] the form is based on.
	 */
	public String getTableName()
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(getDataSource());
		return stn == null ? null : stn[1];
	}

	private long lastModified = System.currentTimeMillis();

	/**
	 * Set the view.
	 *
	 * @param arg the view
	 */
	public void setView(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_VIEW, arg);
	}

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
	 */
	public int getView()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_VIEW).intValue();
	}

	/**
	 * Set the scale.
	 *
	 * @param arg the scale
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setPaperPrintScale(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PAPERPRINTSCALE, arg);
	}

	/**
	 * The percentage value the printed page is enlarged or reduced to; the size of the printed form
	 * is inversely proportional. For example, if the paperPrintScale is 50, the printed form will be
	 * enlarged 200%.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getPaperPrintScale()
	{
		int paperPrintScale = getTypedProperty(StaticContentSpecLoader.PROPERTY_PAPERPRINTSCALE).intValue();
		if (paperPrintScale == 0)
		{
			return 100;
		}
		return paperPrintScale;
	}

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
	 * @description-mc
	 * The navigator is a form that usually handles navigation in application. It is displayed on left side of the screen. Can also have value SM_DEFAULTS.NONE (no navigator) or SM_DEFAULTS.IGNORE (reuse current form navigator).
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getNavigatorID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAVIGATORID).intValue();
	}

	/**
	 * Set the controller form id.
	 *
	 * @param arg
	 */
	public void setNavigatorID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAVIGATORID, arg);
	}


	/**
	 * The selected parent (extend form) for the form. The default is set to -none-.
	 *
	 * @deprecated replaced by the extends property
	 */
	@Deprecated
	public int getExtendsFormID()
	{
		return getExtendsID();
	}

	/**
	 * Sets the selected parent (extend form) for the form.
	 *
	 * @param arg the selected parent
	 */
	@Deprecated
	public void setExtendsFormID(int arg)
	{
		setExtendsID(arg);
	}

	@Override
	public void setExtendsID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID, arg);
		if (arg != (extendsForm == null ? 0 : extendsForm.getID()) && getRootObject().getChangeHandler() != null)
		{
			// fire event to update parent form reference
			getRootObject().getChangeHandler().fireIPersistChanged(this);
		}
	}

	public Form getExtendsForm()
	{
		return extendsForm;
	}

	public void setExtendsForm(Form form)
	{
		this.extendsForm = form;
	}

	/*
	 * _____________________________________________________________ Methods for Part handling
	 */

	/**
	 * Get all part on this form.
	 *
	 * @return the parts
	 */
	public Iterator<Part> getParts()
	{
		return Utils.asSortedIterator(new TypeIterator<Part>(getAllObjectsAsList(), IRepository.PARTS), partComparator);
	}

	/**
	 * Create a new form part (header,footer and such).
	 *
	 * @param partType the part type
	 * @param heigth the absolute height (from top of Form)
	 * @return the new created part
	 */
	public Part createNewPart(int partType, int heigth) throws RepositoryException
	{
		Part obj = (Part)getSolution().getChangeHandler().createNewObject(this, IRepository.PARTS);
		//set all the required properties

		obj.setPartType(partType);
		obj.setHeight(heigth);

		addChild(obj);
		return obj;
	}

	/**
	 * Check the parts. change for example the form size if summed height from all part is smaller.
	 */
	public static Dimension checkParts(Iterator<Part> parts, Dimension size)
	{
		int totalHeight = 0;
		while (parts.hasNext())
		{
			totalHeight = Math.max(totalHeight, parts.next().getHeight());
		}
		if (size == null)
		{
			return new Dimension(640, totalHeight);
		}
		if (size.height != totalHeight && totalHeight > 0)
		{
			return new Dimension(size.width, totalHeight);
		}
		return size;
	}

	/**
	 * Get a part start position.
	 *
	 * @param partElementId the part element_id
	 * @return the position
	 */
	public int getPartStartYPos(int partElementId)
	{
		int totalHeight = 0;
		//check if parts should be changed
		//change for example the form size if summed height from all part is smaller
		Iterator<Part> it = getParts();
		while (it.hasNext())
		{
			Part p = it.next();
			if (p.getID() == partElementId)
			{
				break;
			}
			totalHeight = p.getHeight();
		}
		//calculate the minimum height for a specific part
		return totalHeight;
	}

	/**
	 * Get a part end position.
	 *
	 * @param partElementId the part element_id
	 * @return the position
	 */
	public int getPartEndYPos(int partElementId)
	{
		int totalHeight = 50000;
		//check if parts should be changed
		//change for example the form size if summed height from all part is smaller
		Iterator<Part> it = getParts();
		while (it.hasNext())
		{
			Part p = it.next();
			if (p.getID() == partElementId)
			{
				totalHeight = p.getHeight();
			}
			int h = p.getHeight();
			if (h > totalHeight)
			{
				return h;
			}
			if (!it.hasNext()) totalHeight = 50000;
		}
		//calculate the minimum height for a specific part
		return totalHeight;
	}

	public Part getPartAt(int y)
	{
		// this should return the same part as for component creation
		Iterator<Part> it = getParts();
		while (it.hasNext())
		{
			Part part = it.next();
			if (part.getHeight() > y)
			{
				return part;
			}
		}
		return null;
	}

	public boolean hasPart(int partType)
	{
		for (Part part : Utils.iterate(new TypeIterator<Part>(getAllObjectsAsList(), IRepository.PARTS)))
		{
			if (part.getPartType() == partType)
			{
				return true;
			}
		}
		return false;
	}

	/*
	 * _____________________________________________________________ Methods for Field handling
	 */

	/**
	 * Get the all the tab seq elements on a form sorted by taborder.
	 *
	 * @return the fields
	 */
	public Iterator<ISupportTabSeq> getTabSeqElementsByTabOrder()
	{
		SortedList<ISupportTabSeq> sl = new SortedList<ISupportTabSeq>(TabSeqComparator.INSTANCE);
		Iterator<Field> fields = getFields();
		while (fields.hasNext())
		{
			sl.add(fields.next());
		}
		Iterator<GraphicalComponent> gcs = getGraphicalComponents();
		while (gcs.hasNext())
		{
			GraphicalComponent gc = gcs.next();
			if (gc.getOnActionMethodID() != 0)
			{
				sl.add(gc);
			}
		}
		Iterator<TabPanel> tbs = getTabPanels();
		while (tbs.hasNext())
		{
			TabPanel tb = tbs.next();
			sl.add(tb);
		}
		Iterator<Bean> beans = getBeans();
		while (beans.hasNext())
		{
			Bean b = beans.next();
			sl.add(b);
		}
		Iterator<Portal> portals = getPortals();
		while (portals.hasNext())
		{
			Portal p = portals.next();
			sl.add(p);
		}
		return sl.iterator();
	}

	/*
	 * _____________________________________________________________ Methods for ScriptVariable handling
	 */
	/**
	 * Get the form variables.
	 *
	 * @param sort the flag (true for sorted by name, false for sorted by line number)
	 * @return the form variables
	 */
	public Iterator<ScriptVariable> getScriptVariables(boolean sort)
	{
		return getScriptVariables(getAllObjectsAsList(), sort);
	}

	/**
	 * Get the form variables.
	 *
	 * @param childs list of child objects
	 * @param sort the flag (true for sorted by name, false for sorted by line number)
	 * @return the form variables
	 */
	public static Iterator<ScriptVariable> getScriptVariables(List<IPersist> childs, boolean sort)
	{
		Iterator<ScriptVariable> vars = new TypeIterator<ScriptVariable>(childs, IRepository.SCRIPTVARIABLES);
		if (sort)
		{
			return Utils.asSortedIterator(vars, NameComparator.INSTANCE);
		}
		return Utils.asSortedIterator(vars, LineNumberComparator.INSTANCE);
	}

	/**
	 * Get a form variable by name.
	 *
	 * @param name the name of the variable to get
	 * @return the form variable
	 */
	public ScriptVariable getScriptVariable(String name)
	{
		if (name != null)
		{
			Iterator<ScriptVariable> it = getScriptVariables(false);
			while (it.hasNext())
			{
				ScriptVariable f = it.next();
				if (name.equals(f.getName()))
				{
					return f;
				}
			}
		}
		return null;
	}

	/**
	 * Create a new form variable.
	 *
	 * @param validator the name validator
	 * @param name the name of the new variable
	 * @param variableType the type of the variable
	 * @return the new form variable
	 * @throws RepositoryException
	 */
	public ScriptVariable createNewScriptVariable(IValidateName validator, String nm, int variableType) throws RepositoryException
	{
		String name = nm == null ? "untitled" : nm; //$NON-NLS-1$

		boolean hit = false;
		for (int element : Column.allDefinedTypes)
		{
			if (variableType == element)
			{
				hit = true;
				break;
			}
		}
		if (!hit)
		{
			throw new RepositoryException("unknow variable type: " + variableType); //$NON-NLS-1$
		}
		//check if name is in use
		validator.checkName(name, 0, new ValidatorSearchContext(this, IRepository.SCRIPTVARIABLES), false);
		ScriptVariable obj = (ScriptVariable)getSolution().getChangeHandler().createNewObject(this, IRepository.SCRIPTVARIABLES);
		//set all the required properties

		obj.setName(name);
		obj.setVariableType(variableType);
		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for ScriptMethod handling
	 */
	/**
	 * Get all script methods.
	 *
	 * @param sort the flag (true for sorted, false for not sorted)
	 * @return all the methods
	 */
	public Iterator<ScriptMethod> getScriptMethods(boolean sort)
	{
		Iterator<ScriptMethod> methods = new TypeIterator<ScriptMethod>(getAllObjectsAsList(), IRepository.METHODS);
		if (sort)
		{
			return Utils.asSortedIterator(methods, NameComparator.INSTANCE);
		}
		return methods;
	}

	/**
	 * Get a script method by id.
	 *
	 * @param id the id of the script method to get
	 * @return the script method
	 */
	public ScriptMethod getScriptMethod(int id)
	{
		return selectById(getScriptMethods(false), id);
	}

	/**
	 * Get a script method by name.
	 *
	 * @param name the name of the script method to get
	 * @return the script method
	 */
	public ScriptMethod getScriptMethod(String name)
	{
		return selectByName(getScriptMethods(false), name);
	}

	/**
	 * Create new script method.
	 *
	 * @param validator the name validator
	 * @param name the name of the method
	 * @return the new script method
	 */
	public ScriptMethod createNewScriptMethod(IValidateName validator, String nm) throws RepositoryException
	{
		String name = nm == null ? "untitled" : nm; //$NON-NLS-1$
		ValidatorSearchContext ft = new ValidatorSearchContext(this, IRepository.METHODS);
		validator.checkName(name, 0, ft, false);
		ScriptMethod obj = (ScriptMethod)getRootObject().getChangeHandler().createNewObject(this, IRepository.METHODS);
		//set all the required properties

		obj.setName(name);
		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	@Override
	public String toString()
	{
		String formName = getName();
		return formName == null ? super.toString() : formName;
	}

	/**
	 * Get all objects sorted by form index
	 *
	 * @return all the form elements
	 */
	public Iterator<IFormElement> getFormElementsSortedByFormIndex()
	{
		return getFormElementsSorted(FORM_INDEX_COMPARATOR);
	}

	/**
	 * Get all objects sorted
	 *
	 * @return all the form elements
	 */
	public Iterator<IFormElement> getFormElementsSorted(Comparator<IFormElement> comparator)
	{
		return new FormTypeIterator(getAllObjectsAsList(), comparator);
	}

	static Comparator<Part> partComparator = new PartComparator();

	public static class PartComparator implements Comparator<Part>
	{

		/*
		 * @see Comparator#compare(Object, Object)
		 */
		public int compare(Part p1, Part p2)
		{
			int diff = (p1.getPartType() - p2.getPartType());
			if (diff == 0)
			{
				return (p1.getHeight() - p2.getHeight());
			}
			return diff;
		}

	}

	public static class FormTypeIterator implements Iterator<IFormElement>
	{
		private List<IFormElement> array;
		private int index = 0;

		public FormTypeIterator(List<IPersist> list, final Comparator<IFormElement> comparator)
		{
			array = new ArrayList<IFormElement>();
			if (list != null)
			{
				for (int i = 0; i < list.size(); i++)
				{
					IPersist p = list.get(i);
					if (p instanceof IFormElement)
					{
						array.add((IFormElement)p);
					}
				}
			}

			IFormElement[] a = array.toArray(new IFormElement[array.size()]);
			Arrays.sort(a, comparator);
			array = Arrays.<IFormElement> asList(a);
		}

		public boolean hasNext()
		{
			return (index < array.size());
		}

		public IFormElement next()
		{
			return array.get(index++);
		}

		public void remove()
		{
			//ignore
		}
	}

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public Dimension getMinMaxUsedFormIndex()
	{
		int min = 10000;
		int max = 0;
		Iterator<IPersist> it5 = getAllObjects();
		while (it5.hasNext())
		{
			IPersist element = it5.next();
			if (element instanceof IFormElement)
			{
				int indx = ((IFormElement)element).getFormIndex();
				if (indx > max) max = indx;
				if (indx < min) min = indx;
			}
		}
		return new Dimension(min, max);
	}

	/**
	 * The method that is triggered when a form is loaded/reloaded from the repository; used to alter elements, set globals, hide toolbars,
	 * etc; onShow method can also be assigned.
	 * NOTE: onShow should be used to access current foundset dataproviders; onLoad cannot be used because the foundset data is not loaded until after the form is loaded.
	 * Also calls to loadRecords() should be done in the onShow method and not in the onLoad method
	 * If you call loadRecords() in the onShow method, you may want to set the namedFoundSet property of the form to 'empty' to prevent the first default form query.
	 * NOTE: the onLoad event bubbles down, meaning that the onLoad is first fired on the parent then on a tab in a tabpanel (and in tab of that tab panels if you are 3 deep)
	 *
	 * @templateprivate
	 * @templatedescription Callback method when form is (re)loaded
	 * @templatename onLoad
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getOnLoadMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID).intValue();
	}

	/**
	 * Set the onLoadMethodID.
	 *
	 * @param arg the onLoadMethodID to set
	 */
	public void setOnLoadMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID, arg);
	}

	/**
	 * The method that is triggered when a form is unloaded from the repository.
	 * NOTE: Forms can be prevented from being removed from memory by referencing the form object in a global variable or inside an array inside a global variable. Do take care using this technique.
	 * Forms take up memory and if too many forms are in memory and cannot be unloaded, there is a possibility of running out of memory.
	 *
	 * @templateprivate
	 * @templatedescription Callback method when form is destroyed
	 * @templatename onUnload
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnUnLoadMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID).intValue();
	}

	/**
	 * Set the onLoadMethodID.
	 *
	 * @param arg the onLoadMethodID to set
	 */
	public void setOnUnLoadMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID, arg);
	}

	/**
	 * The default sort order only when the form loads.
	 * This is applied each time an internal SQL query is being executed (find, find-all, open form); and is only executed when no other manual sort has been performed on the foundset.
	 */
	public String getInitialSort()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT);
	}

	/**
	 * The method that is triggered when another form is being activated.
	 * NOTE: If the onHide method returns false, the form can be prevented from hiding.
	 * For example, when using onHide with showFormInDialog, the form will not close by clicking the dialog close box (X).
	 *
	 * @templatedescription Handle hide window
	 * @templatename onHide
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getOnHideMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID).intValue();
	}

	/**
	 * The method that is triggered when a record is being saved.
	 * A record is saved when a user clicks out of it (for example on an empty part of the layout or to another form).
	 * When the method returns false (for example as part of a validation), the user cannot leave the record, for example in
	 * a table view a user cannot move to another record when the callback returns false.
	 *
	 * @templatedescription Callback method form when editing is stopped, return false if the record fails to validate then the user cannot leave the record.
	 * @templatename onRecordEditStop
	 * @templatetype Boolean
	 * @templateparam JSRecord<${dataSource}> record record being saved
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnRecordEditStopMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID).intValue();
	}

	/**
	 * The method that is triggered each time a record is selected.
	 * If a form is in List view or Special table view - when the user clicks on it.
	 * In Record view - after the user navigates to another record using the slider or clicks up or down for next/previous record.
	 * NOTE: Data and Servoy tag values are returned when the onRecordSelection method is executed.
	 * NOTE: this will also fire if the selection goes to -1 because the foundset is cleared. So foundset.getSelectedRecord() can return null.
	 *
	 * @templatedescription Handle record selected
	 * @templatename onRecordSelection
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getOnRecordSelectionMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID).intValue();
	}

	/**
	 * The method that is triggered EVERY TIME the form is displayed; an argument must be passed to the method if this is the first time the form is displayed.
	 *
	 * NOTE: onShow can be used to access current foundset dataproviders; onLoad cannot be used because the foundset data is not loaded until after the form is loaded.
	 *
	 * NOTE: the onShow event bubbles down, meaning that the onShow event of a form displayed in a tabPanel is fired after the onShow event of the parent.
	 *
	 * @templatedescription Callback method for when form is shown
	 * @templatename onShow
	 * @templateparam Boolean firstShow form is shown first time after load
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getOnShowMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID).intValue();
	}

	/**
	 * Set the defaultSort.
	 *
	 * @param arg The defaultSort to set
	 */
	public void setInitialSort(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT, arg);
	}

	/**
	 * Returns the aliases.
	 *
	 * @return String
	 */
	public String getAliases()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ALIASES);
	}

	/**
	 * Set the aliases.
	 *
	 * @param arg The aliases to set
	 */
	public void setAliases(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ALIASES, arg);
	}

	/**
	 * Set the onHideMethodID.
	 *
	 * @param arg The onHideMethodID to set
	 */
	public void setOnHideMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID, arg);
	}

	/**
	 * Set the onRecordSaveMethodID.
	 *
	 * @param arg The onRecordSaveMethodID to set
	 */
	public void setOnRecordEditStopMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID, arg);
	}

	/**
	 * Set the onRecordShowMethodID.
	 *
	 * @param arg The onRecordShowMethodID to set
	 */
	public void setOnRecordSelectionMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID, arg);
	}

	/**
	 * Set the onShowMethodID.
	 *
	 * @param arg The onShowMethodID to set
	 */
	public void setOnShowMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID, arg);
	}

	/**
	 * The method that is triggered when a user clicks into a column on the form.
	 * NOTE: There is a small "e" displayed in the lower left side of the Servoy Client screen in the status area at the bottom of the window when the record is being edited.
	 *
	 * @templatedescription Callback method form when editing is started
	 * @templatename onRecordEditStart
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnRecordEditStartMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID).intValue();
	}

	/**
	 * Set the onRecordEditStart.
	 *
	 * @param arg The onRecordEditStart to set
	 */
	public void setOnRecordEditStartMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID, arg);
	}

	/**
	 * The method that overrides the Servoy menu item Select > Delete Record (or keyboard shortcut).
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform delete record
	 * @templatename deleteRecord
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.deleteRecord()
	 */
	public int getOnDeleteRecordCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Duplicate Record (or keyboard shortcut).
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform duplicate record
	 * @templatename duplicateRecord
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.duplicateRecord(true)
	 */
	public int getOnDuplicateRecordCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Find (or keyboard shortcut) in Data (ready) mode.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform find
	 * @templatename startFind
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.find()
	 */
	public int getOnFindCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Search (or keyboard shortcut) in Find mode.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform search
	 * @templatename onSearch
	 * @templateparam Boolean clear clear last results
	 * @templateparam Boolean reduce reduce search
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.search(clear, reduce)
	 */
	public int getOnSearchCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID).intValue();
	}


	/**
	 * The method that overrides the Servoy menu item Select > Invert Records.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform invert records
	 * @templatename invertRecords
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.invertRecords()
	 */
	public int getOnInvertRecordsCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > New Record (or keyboard shortcut).
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform create new record
	 * @templatename newRecord
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.newRecord(true)
	 */
	public int getOnNewRecordCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Omit Record.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform omit record
	 * @templatename omitRecord
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.omitRecord()
	 */
	public int getOnOmitRecordCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Show All (or keyboard shortcut).
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform show-all-records
	 * @templatename showAllRecords
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.loadAllRecords()
	 */
	public int getOnShowAllRecordsCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Show Omitted Records.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform show-omitted-records
	 * @templatename showOmittedRecords
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.loadOmittedRecords()
	 */
	public int getOnShowOmittedRecordsCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID).intValue();
	}

	/**
	 * Set the onDeleteRecordCmdMethodID
	 *
	 * @param i
	 */
	public void setOnDeleteRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID, i);
	}

	/**
	 * Set the onDuplicateRecordCmdMethodID
	 *
	 * @param i
	 */
	public void setOnDuplicateRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID, i);
	}


	/**
	 * Set the onSearchCmdMethodID
	 *
	 * @param i
	 */
	public void setOnSearchCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID, i);
	}

	/**
	 * Set the onFindCmdMethodID
	 *
	 * @param i
	 */
	public void setOnFindCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID, i);
	}

	/**
	 * Set the onInvertRecordsCmdMethodID
	 *
	 * @param i
	 */
	public void setOnInvertRecordsCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID, i);
	}

	/**
	 * Set the onNewRecordCmdMethodID
	 *
	 * @param i
	 */
	public void setOnNewRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID, i);
	}

	/**
	 * Set the onOmitRecordCmdMethodID
	 *
	 * @param i
	 */
	public void setOnOmitRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID, i);
	}

	/**
	 * Set the onShowAllRecordsCmdMethodID
	 *
	 * @param i
	 */
	public void setOnShowAllRecordsCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID, i);
	}

	/**
	 * Set the onShowOmittedRecordsCmdMethodID
	 *
	 * @param i
	 */
	public void setOnShowOmittedRecordsCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID, i);
	}

	public int getScrollbars()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLBARS).intValue();
	}

	/**
	 * Set the scrollbars (bitset)
	 *
	 * @param i bitset
	 */
	public void setScrollbars(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SCROLLBARS, i);
	}

	/**
	 * The default page format for the form.
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public String getDefaultPageFormat()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DEFAULTPAGEFORMAT);
	}

	/**
	 * Set the default page format
	 *
	 * @param string the format
	 * @see com.servoy.j2db.util.PersistHelper
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setDefaultPageFormat(String string)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DEFAULTPAGEFORMAT, string);
	}

	/**
	 * The type, color and style of border.
	 * This property is automatically set to "DEFAULT" when a new form is created.
	 */
	public String getBorderType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BORDERTYPE);
	}

	/**
	 * The method that overrides the Servoy menu item Select > Delete All.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform delete all records
	 * @templatename deleteAllRecords
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.deleteAllRecords()
	 */
	public int getOnDeleteAllRecordsCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETEALLRECORDSCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item File > Print Preview.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform print preview
	 * @templatename printPreview
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.showPrintPreview(false, null, 100)
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnPrintPreviewCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID).intValue();
	}

	/**
	 * The method that overrides the Servoy menu item Select > Sort.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Perform sort
	 * @templatename onSort
	 * @templateparam String dataProviderID element data provider
	 * @templateparam Boolean asc sort ascending [true] or descending [false]
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.sort(dataProviderID+(asc?' asc':' desc'), false)
	 */
	public int getOnSortCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID).intValue();
	}

	/**
	 * When checked, the selected form will use its own foundset.
	 * By default, all forms based on the same table share the same foundset.
	 */
	public boolean getUseSeparateFoundSet()
	{
		return NAMED_FOUNDSET_SEPARATE.equals(getNamedFoundSet());
	}

	/**
	 * When checked, the selected form will use an empty foundset.
	 *
	 */
	public boolean getUseEmptyFoundSet()
	{
		return NAMED_FOUNDSET_EMPTY.equals(getNamedFoundSet());
	}

	public String getGlobalRelationNamedFoundset()
	{
		String nfs = getNamedFoundSet();
		if (nfs == null || !nfs.startsWith(NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX))
		{
			return null;
		}
		else
		{
			return nfs.substring(NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH);
		}
	}

	/**
	 * Set the border type
	 *
	 * @param b
	 * @see com.servoy.j2db.dataui.ComponentFactoryHelper
	 */
	public void setBorderType(String b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BORDERTYPE, b);
	}

	/**
	 * Set the onDeleteAllRecordsCmdMethodID
	 *
	 * @param i
	 */
	public void setOnDeleteAllRecordsCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETEALLRECORDSCMDMETHODID, i);
	}

	/**
	 * Get the onPrintPreviewCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnPrintPreviewCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID, i);
	}

	/**
	 * Set the onSortCmdMethodID
	 *
	 * @param i
	 */
	public void setOnSortCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID, i);
	}

	/**
	 * Set the useSeparateFoundSet
	 *
	 * @param b
	 */
	public void setUseSeparateFoundSet(boolean b)
	{
		setNamedFoundSet(b ? NAMED_FOUNDSET_SEPARATE : null);
	}

	/**
	 * The text that displays in the title bar of the form window.
	 * NOTE: Data tags and Servoy tags can be used as part of the title text.
	 */
	public String getTitleText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT);
	}

	/**
	 * Set the form title text.
	 *
	 * @param string
	 */
	public void setTitleText(String string)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TITLETEXT, string);
	}

	/**
	 * The calculation dataprovider used to add background color and highlight selected or alternate rows.
	 * The default is -none-.
	 *
	 * NOTE: This property has been deprecated and is kept visible for legacy purposes. Use CSS Row Styling & onRender event instead.
	 *
	 * @templatedescription Calculate the row background color
	 * @templatename rowBGColorCalculation
	 * @templatetype String
	 * @templateparam Number index row index
	 * @templateparam Boolean selected is the row selected
	 * @templateparam String elementType element type (not supported in webclient)
	 * @templateparam String dataProviderID element data provider (not supported in webclient)
	 * @templateparam String formName form name
	 * @templateparam JSRecord<${dataSource}> record selected record
	 * @templateparam Boolean edited is the record edited
	 * @templateaddtodo
	 * @templatecode
	 * if (selected)
	 *   return '#c4ffff';
	 * else if (index % 2)
	 *   return '#f4ffff';
	 * else
	 *   return '#FFFFFF';
	 */
	public String getRowBGColorCalculation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION);
	}

	/**
	 * Set the rowBGColorCalculation.
	 *
	 * NOTE: This property has been deprecated and is kept visible for legacy purposes. Use CSS Row Styling & onRender event instead.
	 *
	 * @param arg the rowBGColorCalculation
	 */
	public void setRowBGColorCalculation(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION, arg);
	}

	/**
	 * The Cascading Style Sheet (CSS) class name applied to the form.
	 */
	public String getStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS);
	}

	/**
	 * Set the style.
	 *
	 * @param arg the syle
	 */
	public void setStyleClass(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS, arg);
	}

	/**
	 * The method that overrides the Servoy menu item Select > Next Record.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Select next record
	 * @templatename nextRecord
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.setSelectedIndex(controller.getSelectedIndex()+1)
	 */
	public int getOnNextRecordCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID).intValue();
	}

	/**
	 * Set the method that overrides the Servoy menu item Select > Next Record.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @param arg the method
	 */
	public void setOnNextRecordCmdMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID, arg);
	}

	/**
	 * The method that overrides the Servoy menu item Select > Previous Record.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @templatedescription Select previous record
	 * @templatename previousRecord
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * controller.setSelectedIndex(controller.getSelectedIndex()-1)
	 */
	public int getOnPreviousRecordCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID).intValue();
	}

	/**
	 * Set the method that overrides the Servoy menu item Select > Previous Record.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 *
	 * @param arg the method
	 */
	public void setOnPreviousRecordCmdMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID, arg);
	}

	/**
	 * Set the transparent
	 *
	 * @param arg the transparent
	 */
	public void setTransparent(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TRANSPARENT, arg);
	}

	/**
	 * When set, the form is transparent.
	 */
	public boolean getTransparent()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TRANSPARENT).booleanValue();
	}

	/**
	 * Set selectionMode. Selection type is applied when necessary to the foundset used by the form (through it's multiselect property), even if the foundset changes.
	 *
	 * @param arg can be one of {@link IForm#SELECTION_MODE_DEFAULT}, {@link IForm#SELECTION_MODE_SINGLE} and {@link IForm#SELECTION_MODE_MULTI}.
	 */
	public void setSelectionMode(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SELECTIONMODE, arg);
	}

	/**
	 * Returns the value of the selectionMode property.
	 *
	 * @return one of {@link IForm#SELECTION_MODE_DEFAULT}, {@link IForm#SELECTION_MODE_SINGLE} and {@link IForm#SELECTION_MODE_MULTI}.
	 * @see #setSelectionMode(int)
	 */
	public int getSelectionMode()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SELECTIONMODE).intValue();
	}

	/**
	 * Set the namedFoundSet
	 *
	 * @param arg the namedFoundSet
	 */
	public void setNamedFoundSet(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAMEDFOUNDSET, arg);
	}

	/**
	 * Property that tells the form to use a named foundset instead of the default foundset.
	 * When "separate" as special value is specified the form will always create a copy of assigned foundset and therefor become separated from other foundsets.
	 * When "empty" it will initially load an empty foundset.
	 * When a global relation name it will load the a related foundset.
	 */
	public String getNamedFoundSet()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAMEDFOUNDSET);
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging occurs.
	 *
	 * @templateprivate
	 * @templatedescription
	 * Handle start of a drag, it can set the data that should be transfered and should return a constant which dragndrop mode/modes is/are supported
	 *
	 * Should return a DRAGNDROP constant or a combination of 2 constants:
	 * DRAGNDROP.MOVE if only a move can happen,
	 * DRAGNDROP.COPY if only a copy can happen,
	 * DRAGNDROP.MOVE|DRAGNDROP.COPY if a move or copy can happen,
	 * DRAGNDROP.NONE if nothing is supported (drag should not start).
	 * @templatename onDrag
	 * @templatetype Number
	 * @templateparam JSDNDEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return DRAGNDROP.NONE
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDragMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dragging occurs.
	 *
	 * @param arg the method that is triggered
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDragMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID, arg);
	}


	/**
	 * The method that is triggered when (non Design Mode) dragging end occurs.
	 *
	 * @templatedescription
	 * Handle end of a drag
	 *
	 * @templatename onDragEnd
	 * @templateparam JSDNDEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDragEndMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dragging end occurs.
	 *
	 * @param arg the method that is triggered
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDragEndMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID, arg);
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging over a component occurs.
	 *
	 * @templatedescription
	 * Handle a drag over. Determines of a drop is allowed in this location.
	 *
	 * Return true is drop is allowed, otherwise false.
	 * @templatename onDragOver
	 * @templatetype Boolean
	 * @templateparam JSDNDEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 *  if(event.getSource() && event.data){
	 *    return true;
	 *  }
	 *  return false;
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDragOverMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dragging over a component occurs.
	 *
	 * @param arg the method that is triggered
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDragOverMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID, arg);
	}

	/**
	 * The method that is triggered when (non Design Mode) dropping occurs.
	 *
	 * @templatedescription Handle a drop
	 * Return true if drop has been performed successfully, otherwise false.
	 * @templatename onDrop
	 * @templatetype Boolean
	 * @templateparam JSDNDEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return false
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnDropMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dropping occurs.
	 *
	 * @param arg the method that gets triggered
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnDropMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID, arg);
	}

	/**
	 * The method that is triggered when focus is gained by a component inside the form.
	 *
	 * @templatedescription Handle focus gained event of an element on the form. Return false when the focus gained event of the element itself shouldn't be triggered.
	 * @templatename onElementFocusGained
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public int getOnElementFocusGainedMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when focus is gained.
	 *
	 * @param arg the method that gets triggered
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public void setOnElementFocusGainedMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID, arg);
	}

	/**
	 * The method that gets triggered when focus is lost by a component inside the form.
	 *
	 * @templatedescription Handle focus lost event of an element on the form. Return false when the focus lost event of the element itself shouldn't be triggered.
	 * @templatename onElementFocusLost
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public int getOnElementFocusLostMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when focus is lost.
	 *
	 * @param arg the method that gets triggered
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public void setOnElementFocusLostMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID, arg);
	}

	/**
	 * The method that gets triggered when resize occurs.
	 *
	 * @templatedescription Callback method when form is resized
	 * @templatename onResize
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnResizeMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID).intValue();
	}

	/**
	 * Set the method that gets triggered when resize occurs.
	 *
	 * @param arg the method that gets triggered
	 */
	public void setOnResizeMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID, arg);
	}

	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setOnRenderMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID, arg);
	}

	public void setEncapsulation(int arg)
	{
		int newAccess = arg;
		int access = getEncapsulation();
		if ((newAccess & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE &&
			(newAccess & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE)
		{
			if ((access & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE) newAccess = newAccess ^ PersistEncapsulation.MODULE_SCOPE;
			else newAccess = newAccess ^ PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE;
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION, newAccess);

	}

	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getEncapsulation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION).intValue();
	}

	/**
	 * The method that is executed when the component is rendered.
	 *
	 * @templatedescription Called before the form component is rendered
	 * @templatename onRender
	 * @templateparam JSRenderEvent event the render event
	 * @templateaddtodo
	 * @templatecode
	 *
	 * // NOTE: a property set on the renderable, will be kept on the element only during onRender
	 * if (event.isRecordSelected()) {
	 * 	event.getRenderable().fgcolor = '#00ff00';
	 * } else if (event.getRecordIndex() % 2) {
	 * 	event.getRenderable().fgcolor = '#ff0000';
	 * }
	 *
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public int getOnRenderMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID).intValue();
	}

	public long getLastModified()
	{
		if (getExtendsForm() != null) return Math.max(getExtendsForm().getLastModified(), lastModified);
		return lastModified;
	}

	public void setLastModified(long lastModified)
	{
		this.lastModified = lastModified;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractBase#fillClone(com.servoy.j2db.persistence.AbstractBase)
	 */
	@Override
	protected void fillClone(AbstractBase cloned)
	{
		super.fillClone(cloned);

		Dimension size = getSize();
		if (size != null) ((Form)cloned).setSize(size);
	}

	/*
	 * @see com.servoy.j2db.persistence.ISupportDeprecated#getDeprecated()
	 */
	@Override
	public String getDeprecated()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED);
	}

	/*
	 * @see com.servoy.j2db.persistence.ISupportDeprecated#setDeprecated(String)
	 */
	@Override
	public void setDeprecated(String deprecatedInfo)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED, deprecatedInfo);
	}

	/**
	 * Returns true if this form is in responsive layout
	 * Currently its always in responsive layout when it has layout containers
	 * or if it is an form that has no parts and no elements.
	 *
	 * @return true if in responsive layout
	 */
	@SuppressWarnings("nls")
	public boolean isResponsiveLayout()
	{
		Object customProperty = getCustomProperty(new String[] { "layout", "responsive" });
		if (customProperty instanceof Boolean) return ((Boolean)customProperty).booleanValue();
		// backwards, always just return true if it has layout containers.
		return getLayoutContainers().hasNext();
	}

	public void setResponsiveLayout(boolean b)
	{
		putCustomProperty(new String[] { "layout", "responsive" }, Boolean.valueOf(b));
	}

	public Boolean getNgReadOnlyMode()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_NG_READONLY_MODE });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return null;
	}

	public void setNgReadOnlyMode(Boolean readOnly)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_NG_READONLY_MODE }, readOnly);
	}

	public Boolean getReferenceForm()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_REFERENCE_FORM });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return Boolean.FALSE;
	}

	public void setReferenceForm(Boolean referenceForm)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_REFERENCE_FORM }, referenceForm);
	}

	/**
	 * Set the onElementChangeMethodID
	 *
	 * @param arg the onElementChangeMethodID
	 */
	public void setOnElementDataChangeMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID, arg);
	}

	/**
	 * Method that is executed when the data in one of the form's component is successfully changed and
	 * the onDataChange callback from the component does not exist or exists and returned true
	 *
	 * @templatedescription Handle changed data, return false if the value should not be accepted. In NGClient you can return also a (i18n) string, instead of false, which will be shown as a tooltip.
	 * @templatename onElementDataChange
	 * @templatetype Boolean
	 * @templateparam ${dataproviderType} oldValue old value
	 * @templateparam ${dataproviderType} newValue new value
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnElementDataChangeMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID).intValue();
	}
}
