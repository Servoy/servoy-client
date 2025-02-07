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
import java.util.stream.Collectors;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.IForm;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A Servoy form.
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

	public static final String NAMED_FOUNDSET_SEPARATE_PREFIX = NAMED_FOUNDSET_SEPARATE + "_"; //$NON-NLS-1$

	public static final int NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH = NAMED_FOUNDSET_SEPARATE_PREFIX.length();

	/**
	 * Constant used for prefixing the namedFoundset property. Prefixes global relations within the namedFoundset property.
	 */
	public static final String NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX = "gr_"; //$NON-NLS-1$

	public static final int NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH = NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX.length();

	public static final String DATASOURCE_NONE = "-none-";

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
		if (stn == null) stn = DataSourceUtils.getMemServernameTablename(getDataSource());
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
		if (getExtendsForm() != null)
		{
			List<Part> parts = new ArrayList<Part>();
			Iterator<Part> it = getParts();
			while (it.hasNext())
			{
				parts.add(it.next());
			}

			Form parentForm = getExtendsForm();
			while (parentForm != null)
			{
				it = parentForm.getParts();
				while (it.hasNext())
				{
					Part parentPart = it.next();
					boolean overriden = false;
					for (Part part : parts)
					{
						if (part.getExtendsID() > 0 && (parentPart.getID() == part.getExtendsID() || parentPart.getExtendsID() == part.getExtendsID()))
						{
							overriden = true;
							break;
						}
					}
					if (!overriden && !parts.contains(parentPart)) parts.add(parentPart);
				}
				parentForm = parentForm.getExtendsForm();
			}
			return checkParts(parts.iterator(), getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE));
		}
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
	 * The height of the form in pixels.
	 */
	public int getHeight()
	{
		return getSize().height;
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

	/**
	 * Set the height.
	 *
	 * @param height
	 */
	public void setHeight(int height)
	{
		if (getParts().hasNext())
		{
			getParts().next().setHeight(height);
		}
		setSize(new Dimension(getSize().width, height));
	}

	/**
	 * If true then the min-with css property will be set for this form so it has a default minimum width
	 *
	 * Can return null so that the default system value should be used.
	 *
	 * @return true if it should use the min-width in the browser
	 */
	public Boolean getUseMinWidth()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_USE_MIN_WIDTH });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return null;
	}

	public void setUseMinWidth(Boolean useMinWidth)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_USE_MIN_WIDTH }, useMinWidth);
	}

	/**
	 * If true then the min-height css property will be set for this form so it has a default minimum height.
	 *
	 * Can return null so that the default system value should be used.
	 *
	 * @return true if it should use the min-height in the browser
	 */
	public Boolean getUseMinHeight()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_USE_MIN_HEIGHT });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return null;
	}

	public void setUseMinHeight(Boolean useMinHeight)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_USE_MIN_HEIGHT }, useMinHeight);
	}

	public int getMinWidth()
	{
		int minWidth = 0;
		for (IPersist persist : getAllObjectsAsList())
		{
			if (persist instanceof IFormElement)
			{
				int persistWidth = CSSPositionUtils.getLocation((IFormElement)persist).x + CSSPositionUtils.getSize((IFormElement)persist).width;
				if (persistWidth > minWidth) minWidth = persistWidth;
			}
		}
		return minWidth;
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public boolean getShowInMenu()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU).booleanValue();
	}

	/**
	 * Set the style name.
	 *
	 * @param arg the name
	 */
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public void setStyleName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLENAME, arg);
	}

	/**
	 * The names of the database server and table that this form is linked to.
	 *
	 * @sample example_data.order_details
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
	@ServoyClientSupport(ng = false, wc = true, sc = true)
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
		if (stn == null) stn = DataSourceUtils.getMemServernameTablename(getDataSource());
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
	 * The default form view mode.<br/><br/>
	 *
	 * The view can be changed using a method at runtime. The following views are available:<br/>
	 * <ul>
	 *   <li>Record view</li>
	 *   <li>List view</li>
	 *   <li>Record view (locked)</li>
	 *   <li>List view (locked)</li>
	 *   <li>Table View (locked)</li>
	 * </ul>
	 *
	 * NOTE: Only Table View (locked) uses asynchronized related data loading.
	 * This feature defers all related foundset data loading to the background - enhancing
	 * the visual display of a related foundset.
	 *
	 * @sample "Record View"
	 * @deprecated starting with Titanium client, List and Table views of a form are no longer implemented. This client has more advanced table / list components available that should be used instead.
	 */
	@Deprecated
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
	 * the left or at the right side of the form, depending on the page orientation.<br/><br/>
	 *
	 * The following options are available:<br/><br/>
	 * -none- - no navigator is assigned.<br/>
	 * DEFAULT - the Servoy default navigator is assigned.<br/>
	 * IGNORE - the navigator last assigned to a previous form.<br/>
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
		if (arg <= 0 && !isResponsiveLayout() && !hasPart(Part.BODY) && !hasPart(0))
		{
			//when extends form property is set to -none-
			//we copy the body part from the parent
			Form parentForm = extendsForm;
			Part body = null;
			outer : while (parentForm != null)
			{
				Iterator<Part> parts = parentForm.getParts();
				while (parts.hasNext())
				{
					Part p = parts.next();
					if (p.getPartType() == Part.BODY)
					{
						body = p;
						break outer;
					}
				}
				parentForm = parentForm.getExtendsForm();
			}
			if (body != null)
			{
				Part clonedBody = (Part)body.clonePersist(this);
				clonedBody.resetUUID();
				clonedBody.setExtendsID(0);
			}
		}
		if ((extendsForm == null ? arg > 0 : extendsForm.getID() != arg) && getRootObject().getChangeHandler() != null)
		{
			// fire event to update parent form reference
			getRootObject().getChangeHandler().fireIPersistChanged(this);
		}
	}

	/**
	 * NOTE: when getting this take into account that currently this is set (based on getExtendsID()) only by ClientState root flattened solution and login
	 * flattened solution + from developer project flattened solutions - at the time the solutions get loaded into flattened, or in case of reload +
	 * due to persist change/create/copy etc in developer. It is a bit weird as this is actually a repo persist model class,
	 * and these Form instances are not stored nor up-to-date unless in what I mentioned earlier, but it was needed for SVY-16572 for example. This might be
	 * improved somehow in the future.
	 */
	public Form getExtendsForm()
	{
		return extendsForm;
	}

	/**
	 * See comment from {@link #getExtendsForm()}
	 */
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
	 * Check the parts. Change for example the form height if summed height from all part is smaller.<br/>
	 * NOTE: Part.getHeight() actually means Part.getY... So every Part.getHeight() will actually return the summed height of all previous parts directly; it's not the actual height of that part only.
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
	 * @param variableType the type of the variable; must be one of {@link Column#allDefinedTypes}
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
				for (IPersist p : list)
				{
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
	 * etc; onShow method can also be assigned.<br/><br/>
	 *
	 * NOTE 1: onShow should be used to access current foundset dataproviders; onLoad cannot be used because the foundset data is not loaded until after the form is loaded.
	 * Also calls to loadRecords() should be done in the onShow method and not in the onLoad method.
	 * If you call loadRecords() in the onShow method, you may want to set the namedFoundSet property of the form to 'empty' to prevent the first default form query.<br/><br/>
	 *
	 * NOTE 2: the onLoad event bubbles down, meaning that the onLoad is first fired on the parent then on a tab in a tabpanel (and in tab of that tab panels if you are 3 deep)
	 *
	 * @sample
	 *
	 * /**
	 *  * Callback method when form is (re)loaded.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"106353C5-73F9-4FEB-8391-6F3B46EB7521"}
	 *  *&#x2f;
	 * function onLoad(event) {
	 *     // TODO Auto-generated method stub
	 *     elements.fc_orderslist.putClientProperty(APP_UI_PROPERTY.LISTFORMCOMPONENT_PAGING_MODE, true);
	 * }
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
	 * The method that is triggered when a form is unloaded from the repository.<br/><br/>
	 *
	 * NOTE: Forms can be prevented from being removed from memory by referencing the form object in a global variable or inside an array inside a global variable. But do take care when using this technique.
	 * Forms take up memory and if too many forms are in memory and cannot be unloaded, there is a possibility of running out of memory.
	 *
	 * @sample
	 * /**
	 *  * Callback method when form is destroyed.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"11CEDAE7-ED62-4444-9C02-5E5D7FAC0EA4"}
	 *  *&#x2f;
	 * function onUnload(event) {
	 *     // Unloads the datasource (only do this when no form is using it anymore)
	 *     databaseManager.removeDataSource(uri);
	 * }
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
	 *
	 * @sample "shipname asc,orderid desc"
	 */
	public String getInitialSort()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_INITIALSORT);
	}

	/**
	 * This method is triggered when the form gets hidden.<br/><br/>
	 *
	 * Return value is DEPRECATED: false return value should no longer be used. In the past, if the onHide method returned false, the form hide could be prevented from happening
	 * in some cases (for example, when using onHide with showFormInDialog, the form will not close by clicking the dialog close box (X)). But that lead to
	 * unexpected situations when the form being hidden had visible nested children it it (tab panels, splits etc.) because only the current form would
	 * decide if hide could be denied, and all other forms, even if they returned false in their on-hide, would not be able to block the hide if this form allowed it.
	 * So those nested forms might think that they are still visible even though they are not.<br/><br/>
	 *
	 * Please use the new onBeforeHide method/handler instead if you want to prevent forms from hiding.
	 *
	 * @sample
	 * /**
	 *  * Handle form's hide.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"2D7F156A-E2D8-4DE8-82C9-A9419AA9EB88"}
	 *  *&#x2f;
	 * function onHide(event) {
	 *     databaseManager.revertEditedRecords();
	 * }
	 *
	 * @templatedescription Handle form's hide.
	 * @templatename onHide
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 *
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getOnHideMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID).intValue();
	}

	/**
	 * This method is triggered when the form wants to hide; this will be called before onHide, and should be used to return if this form can be hidden or not.<br/>
	 * Before the form is really going to hide, this form and all the forms that this form is also showing in its ui hierarchy must allow the hide (return true in onBeforeHide - if present).<br/><br/>
	 *
	 * For example, when using onBeforeHide with showFormInDialog, the form will not close by clicking the dialog close box (X) if the main form in the dialog or any
	 * of the other visible forms in tabpanels/containers are nested in the main are returning false.<br/><br/>
	 *
	 * If the hide operation is allowed for all the forms that are in the affected visible hierarchy, then the onHide handler/method will get called on them as well afterwards.<br/><br/>
	 *
	 * So this handler (on each form) can be used to validate input in the main form and/or any nested visible forms - that are getting ready to hide.
	 *
	 * @sample
	 * /**
	 *  * Check if this form can be hidden, return false if this is not allowed.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;return {Boolean}
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"E37CE276-2B94-47EC-96DA-D631BC344D52"}
	 *  *&#x2f;
	 * function onBeforeHide(event) {
	 *     //reset form variables:
	 *     dialogTitle = '';
	 *     dialogMessage = '';
	 *     dialogDate = '';
	 *     return true;
	 * }
	 *
	 * @templatedescription Check if this form can be hidden, return false if this is not allowed.
	 * @templatename onBeforeHide
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
	public int getOnBeforeHideMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONBEFOREHIDEMETHODID).intValue();
	}

	/**
	 * The method that is triggered when a record is being saved.<br/>
	 * A record is saved when a user clicks out of it (for example on an empty part of the layout or to another form).<br/><br/>
	 *
	 * When this event handler returns false (for example as part of a validation), the user cannot leave the record (change selected record).
	 *
	 * @sample
	 * /**
	 *  * Callback method form when editing is stopped, return false if the record fails to validate then the user cannot leave the record.
	 *  *
	 *  * &#x40;param {JSRecord<db:/example_data/orders>} record record being saved
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;return {Boolean}
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"A63A8831-0D38-4526-ADB5-31B519BE97A9"}
	 *  *&#x2f;
	 * function onRecordEditStop(record, event) {
	 *     var elementName = event.getElementName();
	 *     elements[elementName].removeStyleClass('grayBorder');
	 *     return true;
	 * }
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
	 * The method that is triggered each time a record is selected.<br/><br/>
	 *
	 * NOTE 1: Data and Servoy tag values are returned when the onRecordSelection method is executed.<br/>
	 * NOTE 2: this will also fire if the selection goes to -1 because the foundset is cleared. So foundset.getSelectedRecord() can return null.
	 *
	 * @sample
	 * /**
	 *  * Handle record selected.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"5D614C43-89B3-41D5-941B-0C83CA4D3039"}
	 *  *&#x2f;
	 * function onRecordSelection(event) {
	 *     scopes.globals.selected_user_name = foundset.getSelectedRecord().user_name;
	 * }
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
	 * The method that is triggered each time before a record is selected. Should return true or false to validate the selection change.
	 *
	 * @sample
	 * /**
	 *  * Handle record selected.
	 *  *
	 *  * &#x40;param {Array<JSRecord<db:/example_data/orders>>} old selected records
	 *  * &#x40;param {Array<JSRecord<db:/example_data/orders>>} new selected records
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"5D614C43-89B3-41D5-941B-0C83CA4D3039"}
	 *  *&#x2f;
	 * function onBeforeRecordSelection(event) {
	 *     return true;
	 * }
	 *
	 * @templatedescription Validate record selection before is completed
	 * @templatename onBeforeRecordSelection
	 * @templateparam Array<JSRecord<${dataSource}>> old selection
	 * @templateparam Array<JSRecord<${dataSource}>> new selection
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public int getOnBeforeRecordSelectionMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONBEFORERECORDSELECTIONMETHODID).intValue();
	}

	/**
	 * The method that is triggered EVERY TIME the form is displayed; an true argument will be passed to the method if this is the first time the form is displayed.<br/><br/>
	 *
	 * NOTE 1: onShow can be used to access current foundset dataproviders; onLoad cannot be used because the foundset data is not loaded until after the form is loaded.<br/>
	 * NOTE 2: the onShow event bubbles down, meaning that the onShow event of a form displayed in a tabPanel is fired after the onShow event of the parent.
	 *
	 * @sample
	 * /**
	 *  * Callback method for when form is shown.
	 *  *
	 *  * &#x40;param {Boolean} firstShow form is shown first time after load
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"A8C02494-8CA0-4D89-A332-F308E117D259"}
	 *  *&#x2f;
	 * function onShow(firstShow, event) {
	 *     elements.error.visible = false;
	 * }
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
	 * Set the onHideMethodID.
	 *
	 * @param arg The onHideMethodID to set
	 */
	public void setOnBeforeHideMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONBEFOREHIDEMETHODID, arg);
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
	 * Set the onBeforeRecordSelectionMethodID.
	 *
	 * @param arg The onBeforeRecordSelectionMethodID to set
	 */
	public void setOnBeforeRecordSelectionMethodID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONBEFORERECORDSELECTIONMETHODID, arg);
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
	 * The method that is triggered when a user starts editing a record (for example by clicking into a cell of a table, or editing a field who's data-provider is from that record).
	 *
	 * @sample
	 * /**
	 *  * Callback method form when editing is started.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;return {Boolean}
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"061C702A-CE14-4B9A-9393-7BA3D24988A1"}
	 *  *&#x2f;
	 * function onRecordEditStart(event) {
	 *     // TODO Auto-generated method stub
	 *     var elementName = event.getElementName();
	 *     elements[elementName].addStyleClass(‘grayBorder’);
	 *     return true;
	 * }
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public int getOnShowOmittedRecordsCmdMethodID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID).intValue();
	}

	/**
	 * Set the onDeleteRecordCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnDeleteRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID, i);
	}

	/**
	 * Set the onDuplicateRecordCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnDuplicateRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID, i);
	}


	/**
	 * Set the onSearchCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnSearchCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID, i);
	}

	/**
	 * Set the onFindCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnFindCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID, i);
	}

	/**
	 * Set the onInvertRecordsCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnInvertRecordsCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID, i);
	}

	/**
	 * Set the onNewRecordCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnNewRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID, i);
	}

	/**
	 * Set the onOmitRecordCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnOmitRecordCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID, i);
	}

	/**
	 * Set the onShowAllRecordsCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public void setOnShowAllRecordsCmdMethodID(int i)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID, i);
	}

	/**
	 * Set the onShowOmittedRecordsCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
		String foundsetName = getNamedFoundSet();
		return foundsetName != null && foundsetName.startsWith(NAMED_FOUNDSET_SEPARATE);
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

	public String getSharedFoundsetName()
	{
		String nfs = getNamedFoundSet();
		if (nfs == null || !nfs.startsWith(NAMED_FOUNDSET_SEPARATE_PREFIX))
		{
			return null;
		}
		else
		{
			return nfs.substring(NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
		}
	}

	/**
	 * Set the border type
	 *
	 * @param b
	 * @see com.servoy.j2db.dataui.ComponentFactoryHelper
	 */
	@ServoyClientSupport(ng = false)
	public void setBorderType(String b)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BORDERTYPE, b);
	}

	/**
	 * Set the onDeleteAllRecordsCmdMethodID
	 *
	 * @param i
	 */
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	 * The text that displays in the title bar of the form window.<br/>
	 * NOTE: Data tags and Servoy tags can be used as part of the title text.
	 *
	 * @sample "Order Details"
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
	@ServoyClientSupport(ng = false)
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
	@ServoyClientSupport(ng = false)
	public void setRowBGColorCalculation(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION, arg);
	}

	/**
	 * The Cascading Style Sheet (CSS) class name applied to the form.
	 * @sample "content-panel"
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public String getStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS);
	}

	/**
	 * Set the style.
	 *
	 * @param arg the syle
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	@ServoyClientSupport(ng = false, wc = false, sc = true)
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
	 * @sample "" or "SINGLE" or "MULTI"
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
	 * Property that tells the form to use a named foundset instead of the default foundset.<br/>
	 * When "separate" as special value is specified the form will always create a copy of assigned foundset and therefor become separated from other foundsets.<br/>
	 * When "empty" it will initially load an empty foundset.<br/>
	 * When a global relation name it will load the a related foundset.
	 *
	 * @sample "", or "separate" or "empty"
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
	 * @sample
	 * /**
	 *  * Handle focus gained event of an element on the form. Return false when the focus gained event of the element itself shouldn't be triggered.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;return {Boolean}
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"7E89E3D4-EF0F-4111-8107-D491884E4114"}
	 *  *&#x2f;
	 * function onElementFocusGained(event) {
	 *     var elementName = event.getElementName();
	 *     elements[elementName].addStyleClass('backgroundGreen');
	 *     return true;
	 * }
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
	 * @sample
	 * /**
	 *  * Handle focus lost event of an element on the form. Return false when the focus lost event of the element itself shouldn't be triggered.
	 *  *
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;return {Boolean}
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"9E5C79CE-4A3A-4DD9-AD48-E9EB0A71BD97"}
	 *  *&#x2f;
	 * function onElementFocusLost(event) {
	 *     var elementName = event.getElementName();
	 *     elements[elementName].removeStyleClass('backgroundGreen');
	 *     return true;
	 * }
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
	 * @sample
	 * /**
	 *  * Callback method when form is resized.
	 *  *
	 *  * @param {JSEvent} event the event that triggered the action
	 *  *
	 *  * @properties={typeid:24,uuid:"57CF650F-0481-42DA-A0C4-13AA2001D877"}
	 *  *&#x2f;
	 * function onResize(event) {
	 *     //setting the dividerLocation of a splitpane at the onResize of the form
	 *     var w = controller.getFormWidth();
	 *     elements.split.dividerLocation = w / 2;
	 * }
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

		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size != null) ((Form)cloned).setSize(size);

		((Form)cloned).superPersistCache = null;
	}

	/*
	 * Gets the deprecation info for this element.
	 *
	 * @sample "not used anymore, replaced with 'ordersdetails_new' form"
	 *
	 * @return the deprecation info for this object or null if it is not deprecated
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
	 *
	 * @return true if in responsive layout
	 */
	@SuppressWarnings("nls")
	public boolean isResponsiveLayout()
	{
		Object customProperty = getCustomProperty(new String[] { "layout", "responsive" });
		if (customProperty instanceof Boolean) return ((Boolean)customProperty).booleanValue();
		if (getExtendsForm() != null && getExtendsForm().isResponsiveLayout()) return true;
		// backwards, always just return true if it has layout containers which are not CSSPositionLayoutContainers
		Iterator<LayoutContainer> layoutContainers = getLayoutContainers();
		boolean hasContainers = layoutContainers.hasNext();
		for (LayoutContainer lc : Utils.iterate(layoutContainers))
		{
			if (lc instanceof CSSPositionLayoutContainer) return false;
		}
		return hasContainers;
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

	public Boolean isFormComponent()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_FORM_COMPONENT });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return Boolean.FALSE;
	}

	public void setFormComponent(Boolean formAsComponent)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_FORM_COMPONENT }, formAsComponent);
	}

	public Boolean getUseCssPosition()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_USE_CSS_POSITION });
		if (customProperty instanceof Boolean) return (Boolean)customProperty;
		return Boolean.FALSE;
	}

	public void setUseCssPosition(Boolean cssPosition)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_USE_CSS_POSITION }, cssPosition);
	}

	public boolean getImplicitSecurityNoRights()
	{
		Object customProperty = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_IMPLICIT_SECURITY_NO_RIGHTS });
		if (customProperty instanceof Boolean) return ((Boolean)customProperty).booleanValue();
		return false;
	}

	public void setImplicitSecurityNoRights(boolean implicitSecurity)
	{
		putCustomProperty(new String[] { IContentSpecConstants.PROPERTY_IMPLICIT_SECURITY_NO_RIGHTS }, Boolean.valueOf(implicitSecurity));
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
	 * the onDataChange callback from the component does not exist or exists and returned true.
	 *
	 * @sample
	 * /**
	 *  * Handle changed data, return false if the value should not be accepted. In NGClient you can return also a (i18n) string, instead of false, which will be shown as a tooltip.
	 *  *
	 *  * &#x40;param oldValue old value
	 *  * &#x40;param newValue new value
	 *  * &#x40;param {JSEvent} event the event that triggered the action
	 *  *
	 *  * &#x40;return {Boolean}
	 *  *
	 *  * &#x40;properties={typeid:24,uuid:"06284668-8CBA-4F90-8C31-D304D486DB5C"}
	 *  *&#x2f;
	 * function onElementDataChange(oldValue, newValue, event) {
	 *     var errors = [];
	 *     var validName = new RegExp(/^[A-Za-z0-9-]*$/gm).test(newName);
	 *     if(!validName) {
	 *         errors.push('invalid');
	 *     }
	 *     if(!newName.length) {
	 *         errors.push('empty');
	 *     }
	 *
	 *     return errors.length == 0;
	 * }
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

	@Override
	public ISupportChilds getRealParent()
	{
		return getParent();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractContainer#getWebComponents()
	 */
	@Override
	public Iterator<WebComponent> getWebComponents()
	{
		if (!isResponsiveLayout()) return super.getWebComponents();
		List<WebComponent> result = new ArrayList<>();
		List<IFormElement> elem = getFlattenedObjects(NameComparator.INSTANCE);
		for (IFormElement fe : elem)
		{
			if (fe instanceof WebComponent) result.add((WebComponent)fe);
		}
		return result.iterator();
	}

	/**
	 * Return a description string of the associated layout
	 *
	 * @return "responsive layout" - for the NG responsive forms
	 * @return "css position layout" - for the NG forms using CSS position
	 * @return "absolute layout" - for the forms using anchors
	 * @return "no layout" - for forms with no UI
	 */
	public String getLayoutType()
	{
		if (isResponsiveLayout()) return "responsive layout";
		if (getUseCssPosition().booleanValue()) return "css position layout";
		Iterator<Part> it = getParts();
		if (it.hasNext()) return "absolute layout";
		return "abstract form (no layout)";
	}

	/**
	 * Verify the presence of the associated UI
	 *
	 * @return true if the form is abstract (no UI) otherwise return false
	 */
	public boolean isAbstractForm()
	{
		if (isResponsiveLayout()) return false;
		if (getUseCssPosition().booleanValue() == true) return false;
		Iterator<Part> it = getParts();
		if (it.hasNext()) return false; //abstract form has no parts
		return true;
	}

	private transient IntHashMap<IPersist> superPersistCache = null;

	/**
	 * @param extendsID
	 * @return
	 */
	public IPersist getSuperPersist(int extendsID)
	{
		synchronized (this)
		{
			if (superPersistCache == null)
			{
				IntHashMap<IPersist> cache = new IntHashMap<>();
				acceptVisitor((IPersist persist) -> {
					cache.put(persist.getID(), persist);
					if (persist instanceof ISupportExtendsID && ((ISupportExtendsID)persist).getExtendsID() > 0)
					{
						cache.put(((ISupportExtendsID)persist).getExtendsID(), persist);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				});
				superPersistCache = cache;
			}
		}
		return superPersistCache.get(extendsID);
	}

	@Override
	public void childAdded(IPersist obj)
	{
		super.childAdded(obj);
		setLastModified(System.currentTimeMillis());
		superPersistCache = null;
	}

	@Override
	public void childRemoved(IPersist obj)
	{
		super.childRemoved(obj);
		setLastModified(System.currentTimeMillis());
		superPersistCache = null;
	}

	public boolean containsResponsiveLayout()
	{
		return getLayoutContainers().hasNext() || getObjects(IRepository.CSSPOS_LAYOUTCONTAINERS).hasNext();
	}

	public Iterator<LayoutContainer> getAllLayoutContainers()
	{
		return getAllObjectsAsList().stream().filter(LayoutContainer.class::isInstance).map(LayoutContainer.class::cast).collect(Collectors.toList())
			.iterator();
	}

	/**
	 * Additional information, such as programmer notes about this form's purpose.
	 *
	 * @sample "shows table with order details"
	 */
	@Override
	public String getComment()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_COMMENT);
	}

}
