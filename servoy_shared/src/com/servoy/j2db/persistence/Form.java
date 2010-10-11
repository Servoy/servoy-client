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
import java.awt.Point;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.JSONWrapperMap;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;

/**
 * A normal Servoy form
 * 
 * @author jblok,jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class Form extends AbstractBase implements ISupportFormElements, ITableDisplay, ISupportUpdateableName, ISupportScrollbars, IPersistCloneable,
	ISupportSize, ISupportScriptProviders, ICloneable
{
	/**
	 * @sameas getNavigatorID()
	 */
	public static final int NAVIGATOR_DEFAULT = 0;

	/**
	 * @sameas getNavigatorID() 
	 */
	public static final int NAVIGATOR_NONE = -1;

	/**
	 * @sameas getNavigatorID()
	 */
	public static final int NAVIGATOR_IGNORE = -2;

	/**
	 * 	Flag which indicates if this form uses a separate foundSet.
	 */
	public static final String SEPARATE_FLAG = "separate"; //$NON-NLS-1$

	/**
	 * 	Flag which indicates if this form's foundset will load the default data.
	 */
	public static final String EMPTY_FLAG = "empty"; //$NON-NLS-1$

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */

	private String titleText = null;
	private String dataSource = null; // url db:/serverName/tableName

	/**
	 * Constant which indicates the name of this form.
	 */
	private String formName = null;
	private java.awt.Dimension size = null;
	private boolean showInMenu = false;
	private String styleName;
	private java.awt.Color background = null;
	private int view;

	private int paperPrintScale;
	private int navigatorID;//zero is default slider
	private int extendsFormID;//zero is none
	private String rowBGColorCalculation;
	private int onLoadMethodID;
	private int onUnLoadMethodID;
	private int onShowMethodID;
	private int onHideMethodID;
	private int onRecordEditStartMethodID;
	private int onRecordSelectionMethodID;
	private int onRecordEditStopMethodID;
	private String initialSort;
	private String aliases;

	private int onNewRecordCmdMethodID;
	private int onDuplicateRecordCmdMethodID;
	private int onDeleteRecordCmdMethodID;
	private int onFindCmdMethodID;
	private int onSearchCmdMethodID;
	private int onShowAllRecordsCmdMethodID;
	private int onOmitRecordCmdMethodID;
	private int onShowOmittedRecordsCmdMethodID;
	private int onInvertRecordsCmdMethodID;

	private int onSortCmdMethodID;
	private int onDeleteAllRecordsCmdMethodID;
	private int onPrintPreviewCmdMethodID;
	private int onNextRecordCmdMethodID;
	private int onPreviousRecordCmdMethodID;


	private int onDragMethodID;
	private int onDragOverMethodID;
	private int onDragEndMethodID;
	private int onDropMethodID;
	private int onElementFocusGainedMethodID;
	private int onElementFocusLostMethodID;
	private int onResizeMethodID;
	private int onRenderMethodID;

	private int scrollbars;
	private String defaultPageFormat; //orientation;width;height;ImageableX;ImageableY,ImageableWidth;ImageableHeight
	private String borderType = null;
	private String styleClass;

	private String namedFoundSet;
	private int access;

	/**
	 * Constructor I
	 */
	Form(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.FORMS, parent, element_id, uuid);
	}

	// only for use in FlattenedForm
	protected void setCustomPropertiesMap(JSONWrapperMap map)
	{
		if (map == null)
		{
			customProperties = null;
			jsonCustomProperties = null;
		}
		else
		{
			jsonCustomProperties = map;
			customProperties = jsonCustomProperties.toString();
		}
	}

	// only for use in FlattenedForm
	protected JSONWrapperMap getCustomPropertiesMap()
	{
		if (customProperties == null) return null;
		if (jsonCustomProperties == null)
		{
			jsonCustomProperties = new JSONWrapperMap(customProperties);
		}
		return jsonCustomProperties;
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
		checkForChange(background, arg);
		background = arg;
	}

	/**
	 * Get the background.
	 * 
	 * @return the background
	 */
	public java.awt.Color getBackground()
	{
		return background;
	}

	/**
	 * Set the server name to use by this form.
	 * 
	 * @param arg the server name to use
	 */
	public void setServerName(String arg)
	{
		String uri = DataSourceUtils.createDBTableDataSource(arg, getTableName());
		checkForChange(dataSource, uri);
		dataSource = uri == null ? null : (uri.intern());
		table = null;//clear any cached table
	}

	/**
	 * Get the server name used by this form.
	 * 
	 */
	public String getServerName()
	{
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		return stn == null ? null : stn[0];
	}

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. This method shouldn't be called from outside the persistance package!!
	 * 
	 * @param arg the form name
	 * @exclude
	 */
	public void setName(String arg)
	{
		if (formName != null) throw new UnsupportedOperationException("Can't set name 2x, use updateName"); //$NON-NLS-1$
		formName = arg;
	}

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 * 
	 * @exclude
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(this, IRepository.FORMS), false);
		checkForNameChange(formName, arg);
		formName = arg;
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * The name of the form.
	 */
	public String getName()
	{
		return formName;
	}

	/**
	 * Set the form size.
	 * 
	 * @param arg the size
	 */
	public void setSize(Dimension arg)
	{
		checkForChange(size, arg);
		size = arg;
	}

	/**
	 * Get the form size.
	 * 
	 * @return the size
	 */
	public Dimension getSize()
	{
		if (size == null)
		{
			size = new Dimension(380, 200);
			return new Dimension(380, 200);
		}
		else
		{
			checkParts();
			return new Dimension(size);
		}
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
		checkForChange(getSize().width, width);
		size.width = width;
	}

	/**
	 * Set show in menu.
	 * 
	 * @param arg the flag
	 */
	public void setShowInMenu(boolean arg)
	{
		checkForChange(showInMenu, arg);
		showInMenu = arg;
	}

	/**
	 * When set, the form is displayed under the Window menu. 
	 * If it is not set, the form will be 'hidden'. 
	 * NOTE: This is only applicable for Servoy Client. Servoy Developer always shows all forms so that
	 * developers have access to all forms within a solution during development.
	 */
	public boolean getShowInMenu()
	{
		return showInMenu;
	}

	/**
	 * Set the style name.
	 * 
	 * @param arg the name
	 */
	public void setStyleName(String arg)
	{
		checkForChange(styleName, arg);
		styleName = arg;
	}

	/**
	 * The names of the database server and table that this form is linked to.
	 */
	public String getDataSource()
	{
		return dataSource;
	}

	/**
	 * Set the data source.
	 * 
	 * @param arg the data source uri
	 */
	public void setDataSource(String arg)
	{
		checkForChange(dataSource, arg);
		dataSource = arg == null ? null : (arg.intern());
		table = null;
	}

	/**
	 * The name of the Servoy style that is being used on the form.
	 */
	public String getStyleName()
	{
		return styleName;
	}

	/**
	 * Clears the form table.
	 */
	public void clearTable()
	{
		table = null;
	}

	/**
	 * Set the table name.
	 * 
	 * @param arg the table to use
	 */
	public void setTableName(String arg)
	{
		String uri = DataSourceUtils.createDBTableDataSource(getServerName(), arg);
		checkForChange(dataSource, uri);
		dataSource = uri == null ? null : (uri.intern());
		table = null;//clear any cached table
	}

	/**
	 * The [name of the table/SQL view].[the name of the database server connection] the form is based on.
	 */
	public String getTableName()
	{
		String[] stn = DataSourceUtils.getDBServernameTablename(dataSource);
		return stn == null ? null : stn[1];
	}

	private transient Table table;//local cache
	private boolean transparent;
	private long lastModified = System.currentTimeMillis();

	/**
	 * The table associated with this form.
	 * 
	 * @exclude
	 */
	public Table getTable() throws RepositoryException
	{
		if (table == null && dataSource != null)
		{
			try
			{
				IServer s = getSolution().getServer(getServerName());
				if (s != null)
				{
					table = (Table)s.getTable(getTableName());
				}
			}
			catch (RemoteException ex)
			{
				Debug.error(ex);
			}
		}
		return table;
	}

	/**
	 * Set the view.
	 * 
	 * @param arg the view
	 */
	public void setView(int arg)
	{
		checkForChange(view, arg);
		view = arg;
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
		return view;
	}

	/**
	 * Set the scale.
	 * 
	 * @param arg the scale
	 */
	public void setPaperPrintScale(int arg)
	{
		checkForChange(paperPrintScale, arg);
		paperPrintScale = arg;
	}

	/**
	 * The percentage value the printed page is enlarged or reduced to; the size of the printed form 
	 * is inversely proportional. For example, if the paperPrintScale is 50, the printed form will be 
	 * enlarged 200%.
	 */
	public int getPaperPrintScale()
	{
		if (paperPrintScale == 0)
		{
			return 100;
		}
		else
		{
			return paperPrintScale;
		}
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
	 */
	public int getNavigatorID()
	{
		return navigatorID;
	}

	/**
	 * Set the controller form id.
	 * 
	 * @param arg
	 */
	public void setNavigatorID(int arg)
	{
		checkForChange(navigatorID, arg);
		navigatorID = arg;
	}


	/**
	 * The selected parent (extend form) for the form. The default is set to -none-.
	 */
	public int getExtendsFormID()
	{
		return extendsFormID;
	}

	/**
	 * Sets the selected parent (extend form) for the form.
	 * 
	 * @param arg the selected parent
	 */
	public void setExtendsFormID(int arg)
	{
		checkForChange(extendsFormID, arg);
		extendsFormID = arg;
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
		return new SortedTypeIterator<Part>(getAllObjectsAsList(), IRepository.PARTS, partComparator);
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
	public void checkParts()
	{
		int totalHeight = 0;
		//check if parts should be changed
		Iterator<Part> it = getParts();
		while (it.hasNext())
		{
			Part p = it.next();
			int h = p.getHeight();
			if (h > totalHeight)
			{
				totalHeight = h;
			}
		}
		if (size == null)
		{
			setSize(new Dimension(640, totalHeight));
		}
		else if (size.height != totalHeight && totalHeight > 0)
		{
			setSize(new Dimension(size.width, totalHeight));
		}
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

	/*
	 * _____________________________________________________________ Methods for Field handling
	 */

	/**
	 * Get the all the fields on a form.
	 * 
	 * @return the fields
	 */
	public Iterator<Field> getFields()
	{
		return getObjects(IRepository.FIELDS);
	}

	/**
	 * Get the all the fields on a form sorted by taborder.
	 * 
	 * @return the fields
	 */
	public Iterator<ISupportTabSeq> getFieldsByTabOrder()
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

	/**
	 * Create a new field.
	 * 
	 * @param location the location
	 * @return the field
	 */
	public Field createNewField(Point location) throws RepositoryException
	{
		Field obj = (Field)getSolution().getChangeHandler().createNewObject(this, IRepository.FIELDS);

		//set all the required properties
		obj.setLocation(location);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for Label handling
	 */
	/**
	 * Get all the graphicalComponents from this form.
	 * 
	 * @return graphicalComponents
	 */
	public Iterator<GraphicalComponent> getGraphicalComponents()
	{
		return getObjects(IRepository.GRAPHICALCOMPONENTS);
	}

	/**
	 * Create new graphicalComponents.
	 * 
	 * @param location
	 * @return the graphicalComponent
	 */
	public GraphicalComponent createNewGraphicalComponent(Point location) throws RepositoryException
	{
		GraphicalComponent obj = (GraphicalComponent)getRootObject().getChangeHandler().createNewObject(this, IRepository.GRAPHICALCOMPONENTS);
		//set all the required properties

		obj.setLocation(location);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for Shape handling
	 */

	/**
	 * Get all the shapes.
	 * 
	 * @return the shapes
	 */
	public Iterator<Shape> getShapes()
	{
		return getObjects(IRepository.SHAPES);
	}

	/**
	 * Create a new shape.
	 * 
	 * @param location
	 * @return the shape
	 */
	public Shape createNewShape(Point location) throws RepositoryException
	{
		Shape obj = (Shape)getRootObject().getChangeHandler().createNewObject(this, IRepository.SHAPES);
		//set all the required properties

		obj.setLocation(location);
		obj.setLineSize(1);
		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for ScriptVariable handling
	 */
	/**
	 * Get the form variables.
	 * 
	 * @param sort the flag (true for sorted, false for not sorted)	 * 
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
	 * @param sort the flag (true for sorted, false for not sorted)
	 * @return the form variables
	 */
	public static Iterator<ScriptVariable> getScriptVariables(List<IPersist> childs, boolean sort)
	{
		if (sort)
		{
			return new SortedTypeIterator<ScriptVariable>(childs, IRepository.SCRIPTVARIABLES, NameComparator.INSTANCE);
		}
		else
		{
			return new TypeIterator<ScriptVariable>(childs, IRepository.SCRIPTVARIABLES);
		}
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
	public ScriptVariable createNewScriptVariable(IValidateName validator, String name, int variableType) throws RepositoryException
	{
		if (name == null) name = "untitled"; //$NON-NLS-1$

		boolean hit = false;
		int[] types = Column.allDefinedTypes;
		for (int element : types)
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
	 * _____________________________________________________________ Methods for Portal handling
	 */
	/**
	 * Get all the portals from this form.
	 * 
	 * @return the portals
	 */
	public Iterator<Portal> getPortals()
	{
		return getObjects(IRepository.PORTALS);
	}

	/**
	 * Create a new portal.
	 * 
	 * @param name the name of the new portal
	 * @param location the location of the new portal
	 * @return the new portal
	 */
	public Portal createNewPortal(String name, Point location) throws RepositoryException
	{
		if (name == null) name = "untitled";

		Portal obj = (Portal)getRootObject().getChangeHandler().createNewObject(this, IRepository.PORTALS);
		//set all the required properties

		obj.setLocation(location);
		obj.setName(name);
//		obj.setRelationID(relation.getID());

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for Bean handling
	 */
	/**
	 * Get all the beans for this form.
	 * 
	 * @return all the beans
	 */
	public Iterator<Bean> getBeans()
	{
		return getObjects(IRepository.BEANS);
	}

	/**
	 * Create a new bean.
	 * 
	 * @param name the name of the bean
	 * @param className the class name 
	 * @return the new bean
	 */
	public Bean createNewBean(String name, String className) throws RepositoryException
	{
		if (name == null) name = "untitled"; //$NON-NLS-1$

		Bean obj = (Bean)getRootObject().getChangeHandler().createNewObject(this, IRepository.BEANS);
		//set all the required properties

		obj.setName(name);
		obj.setBeanClassName(className);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for TabPanel handling
	 */
	/**
	 * Get all the form tab panels.
	 * 
	 * @return all the tab panels
	 */
	public Iterator<TabPanel> getTabPanels()
	{
		return getObjects(IRepository.TABPANELS);
	}

	/**
	 * Create a new tab panel.
	 * 
	 * @param name
	 * @return the new tab panel
	 */
	public TabPanel createNewTabPanel(String name) throws RepositoryException
	{
		TabPanel obj = (TabPanel)getRootObject().getChangeHandler().createNewObject(this, IRepository.TABPANELS);
		//set all the required properties

		obj.setName(name);

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods for Rectangle handling
	 */
	/**
	 * @deprecated
	 */
	@Deprecated
	public RectShape createNewRectangle(Point location) throws RepositoryException
	{
		RectShape obj = (RectShape)getRootObject().getChangeHandler().createNewObject(this, IRepository.RECTSHAPES);
		//set all the required properties

		obj.setLocation(location);
		obj.setLineSize(1);
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
		if (!sort)
		{
			return getObjects(IRepository.METHODS);
		}
		else
		{
			return new SortedTypeIterator<ScriptMethod>(getAllObjectsAsList(), IRepository.METHODS, NameComparator.INSTANCE);
		}
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
	public ScriptMethod createNewScriptMethod(IValidateName validator, String name) throws RepositoryException
	{
		if (name == null) name = "untitled"; //$NON-NLS-1$
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

	public Solution getSolution()
	{
		return (Solution)getRootObject();
	}

	@Override
	public String toString()
	{
		return (formName == null ? super.toString() : formName);
	}

	/**
	 * Get all objects sorted by form index
	 * 
	 * @return all the form elements
	 */
	public Iterator<IPersist> getAllObjectsSortedByFormIndex()
	{
		return new FormTypeIterator(getAllObjectsAsList(), new Comparator<IPersist>()
		{
			public int compare(IPersist persist1, IPersist persist2)
			{
				if (persist1 instanceof IFormElement && persist2 instanceof IFormElement)
				{
					return ((IFormElement)persist1).getFormIndex() - ((IFormElement)persist2).getFormIndex();
				}
				return 0;
			}
		});
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

	protected static class FormTypeIterator implements Iterator<IPersist>
	{
		private List<IPersist> array;
		private int index = 0;

		FormTypeIterator(List<IPersist> list, final Comparator<IPersist> comparator)
		{
			array = new ArrayList<IPersist>();
			if (list != null)
			{
				int index = 0;
				for (int i = 0; i < list.size(); i++)
				{
					IPersist p = list.get(i);
					if (p instanceof IFormElement)
					{
						array.add(index, p);
						index++;
					}
					else
					{
						array.add(p);
					}
				}
			}

			IPersist[] a = array.toArray(new IPersist[array.size()]);
			Arrays.sort(a, comparator);
			array = Arrays.<IPersist> asList(a);
		}

		public boolean hasNext()
		{
			return (index < array.size());
		}

		public IPersist next()
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
	 * @templatedescription Callback method when form is (re)loaded
	 * @templatename onLoad
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnLoadMethodID()
	{
		return onLoadMethodID;
	}

	/**
	 * Set the onLoadMethodID.
	 * 
	 * @param arg the onLoadMethodID to set
	 */
	public void setOnLoadMethodID(int arg)
	{
		checkForChange(onLoadMethodID, arg);
		onLoadMethodID = arg;
	}

	/**
	 * The method that is triggered when a form is unloaded from the repository. 
	 * NOTE: Forms can be prevented from being removed from memory by referencing the form object in a global variable or inside an array inside a global variable. Do take care using this technique.
	 * Forms take up memory and if too many forms are in memory and cannot be unloaded, there is a possibility of running out of memory.
	 * 
	 * @templatedescription Callback method when form is destroyed
	 * @templatename onUnload
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnUnLoadMethodID()
	{
		return onUnLoadMethodID;
	}

	/**
	 * Set the onLoadMethodID.
	 * 
	 * @param arg the onLoadMethodID to set
	 */
	public void setOnUnLoadMethodID(int arg)
	{
		checkForChange(onUnLoadMethodID, arg);
		onUnLoadMethodID = arg;
	}

	/**
	 * The default sort order only when the form loads.
	 * This is applied each time an internal SQL query is being executed (find, find-all, open form); and is only executed when no other manual sort has been performed on the foundset.
	 */
	public String getInitialSort()
	{
		return initialSort;
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
	public int getOnHideMethodID()
	{
		return onHideMethodID;
	}

	/**
	 * The method that is triggered when a record is being saved. 
	 * A record is saved when a user clicks out of it (for example on an empty part of the layout or to another form); can return false (for example as part of a validation) where a condition must be changed to return true. 
	 * NOTE: The name of this property has been changed from onRecordSave.
	 * 
	 * @templatedescription Callback method form when editing is stopped
	 * @templatename onRecordEditStop
	 * @templatetype Boolean
	 * @templateparam JSRecord record record being saved
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnRecordEditStopMethodID()
	{
		return onRecordEditStopMethodID;
	}

	/**
	 * The method that is triggered each time a record is selected. 
	 * If a form is in List view or Special table view - when the user clicks on it.
	 * In Record view - after the user navigates to another record using the slider or clicks up or down for next/previous record. 
	 * NOTE: Data and Servoy tag values are returned when the onRecordSelection method is executed.
	 * 
	 * @templatedescription Handle record selected
	 * @templatename onRecordSelection
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnRecordSelectionMethodID()
	{
		return onRecordSelectionMethodID;
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
	public int getOnShowMethodID()
	{
		return onShowMethodID;
	}

	/**
	 * Set the defaultSort.
	 * 
	 * @param arg The defaultSort to set
	 */
	public void setInitialSort(String arg)
	{
		checkForChange(initialSort, arg);
		initialSort = arg;
	}

	/**
	 * Returns the aliases.
	 * 
	 * @return String
	 */
	public String getAliases()
	{
		return aliases;
	}

	/**
	 * Set the aliases.
	 * 
	 * @param arg The aliases to set
	 */
	public void setAliases(String arg)
	{
		checkForChange(aliases, arg);
		aliases = arg;
	}

	/**
	 * Set the onHideMethodID.
	 * 
	 * @param arg The onHideMethodID to set
	 */
	public void setOnHideMethodID(int arg)
	{
		checkForChange(onHideMethodID, arg);
		onHideMethodID = arg;
	}

	/**
	 * Set the onRecordSaveMethodID.
	 * 
	 * @param arg The onRecordSaveMethodID to set
	 */
	public void setOnRecordEditStopMethodID(int arg)
	{
		checkForChange(onRecordEditStopMethodID, arg);
		onRecordEditStopMethodID = arg;
	}

	/**
	 * Set the onRecordShowMethodID.
	 * 
	 * @param arg The onRecordShowMethodID to set
	 */
	public void setOnRecordSelectionMethodID(int arg)
	{
		checkForChange(onRecordSelectionMethodID, arg);
		onRecordSelectionMethodID = arg;
	}

	/**
	 * Set the onShowMethodID.
	 * 
	 * @param arg The onShowMethodID to set
	 */
	public void setOnShowMethodID(int arg)
	{
		checkForChange(onShowMethodID, arg);
		onShowMethodID = arg;
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
	 */
	public int getOnRecordEditStartMethodID()
	{
		return onRecordEditStartMethodID;
	}

	/**
	 * Set the onRecordEditStart.
	 * 
	 * @param arg The onRecordEditStart to set
	 */
	public void setOnRecordEditStartMethodID(int arg)
	{
		checkForChange(onRecordEditStartMethodID, arg);
		onRecordEditStartMethodID = arg;
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
		return onDeleteRecordCmdMethodID;
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
		return onDuplicateRecordCmdMethodID;
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
		return onFindCmdMethodID;
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
		return onSearchCmdMethodID;
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
		return onInvertRecordsCmdMethodID;
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
		return onNewRecordCmdMethodID;
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
		return onOmitRecordCmdMethodID;
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
		return onShowAllRecordsCmdMethodID;
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
		return onShowOmittedRecordsCmdMethodID;
	}

	/**
	 * Set the onDeleteRecordCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnDeleteRecordCmdMethodID(int i)
	{
		checkForChange(onDeleteRecordCmdMethodID, i);
		onDeleteRecordCmdMethodID = i;
	}

	/**
	 * Set the onDuplicateRecordCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnDuplicateRecordCmdMethodID(int i)
	{
		checkForChange(onDuplicateRecordCmdMethodID, i);
		onDuplicateRecordCmdMethodID = i;
	}


	/**
	 * Set the onSearchCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnSearchCmdMethodID(int i)
	{
		checkForChange(onSearchCmdMethodID, i);
		onSearchCmdMethodID = i;
	}

	/**
	 * Set the onFindCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnFindCmdMethodID(int i)
	{
		checkForChange(onFindCmdMethodID, i);
		onFindCmdMethodID = i;
	}

	/**
	 * Set the onInvertRecordsCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnInvertRecordsCmdMethodID(int i)
	{
		checkForChange(onInvertRecordsCmdMethodID, i);
		onInvertRecordsCmdMethodID = i;
	}

	/**
	 * Set the onNewRecordCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnNewRecordCmdMethodID(int i)
	{
		checkForChange(onNewRecordCmdMethodID, i);
		onNewRecordCmdMethodID = i;
	}

	/**
	 * Set the onOmitRecordCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnOmitRecordCmdMethodID(int i)
	{
		checkForChange(onOmitRecordCmdMethodID, i);
		onOmitRecordCmdMethodID = i;
	}

	/**
	 * Set the onShowAllRecordsCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnShowAllRecordsCmdMethodID(int i)
	{
		checkForChange(onShowAllRecordsCmdMethodID, i);
		onShowAllRecordsCmdMethodID = i;
	}

	/**
	 * Set the onShowOmittedRecordsCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnShowOmittedRecordsCmdMethodID(int i)
	{
		checkForChange(onShowOmittedRecordsCmdMethodID, i);
		onShowOmittedRecordsCmdMethodID = i;
	}

	public int getScrollbars()
	{
		return scrollbars;
	}

	/**
	 * Set the scrollbars (bitset)
	 * 
	 * @param i bitset
	 */
	public void setScrollbars(int i)
	{
		checkForChange(scrollbars, i);
		scrollbars = i;
	}

	/**
	 * The default page format for the form. 
	 */
	public String getDefaultPageFormat()
	{
		return defaultPageFormat;
	}

	/**
	 * Set the default page format
	 * 
	 * @param string the format
	 * @see com.servoy.j2db.util.PersistHelper
	 */
	public void setDefaultPageFormat(String string)
	{
		checkForChange(defaultPageFormat, string);
		defaultPageFormat = string;
	}

	/**
	 * The type, color and style of border. 
	 * This property is automatically set to "DEFAULT" when a new form is created.
	 */
	public String getBorderType()
	{
		return borderType;
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
		return onDeleteAllRecordsCmdMethodID;
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
	public int getOnPrintPreviewCmdMethodID()
	{
		return onPrintPreviewCmdMethodID;
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
		return onSortCmdMethodID;
	}

	/**
	 * When checked, the selected form will use its own foundset. 
	 * By default, all forms based on the same table share the same foundset.
	 */
	public boolean getUseSeparateFoundSet()
	{
		return (SEPARATE_FLAG.equals(namedFoundSet));
	}

	/**
	 * When checked, the selected form will use an empty foundset. 
	 * 
	 */
	public boolean getUseEmptyFoundSet()
	{
		return (EMPTY_FLAG.equals(namedFoundSet));
	}

	/**
	 * Set the border type
	 * 
	 * @param b
	 * @see com.servoy.j2db.dataui.ComponentFactoryHelper
	 */
	public void setBorderType(String b)
	{
		checkForChange(borderType, b);
		borderType = b;
	}

	/**
	 * Set the onDeleteAllRecordsCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnDeleteAllRecordsCmdMethodID(int i)
	{
		checkForChange(onDeleteAllRecordsCmdMethodID, i);
		onDeleteAllRecordsCmdMethodID = i;
	}

	/**
	 * Get the onPrintPreviewCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnPrintPreviewCmdMethodID(int i)
	{
		checkForChange(onPrintPreviewCmdMethodID, i);
		onPrintPreviewCmdMethodID = i;
	}

	/**
	 * Set the onSortCmdMethodID
	 * 
	 * @param i
	 */
	public void setOnSortCmdMethodID(int i)
	{
		checkForChange(onSortCmdMethodID, i);
		onSortCmdMethodID = i;
	}

	/**
	 * Set the useSeparateFoundSet
	 * 
	 * @param b
	 */
	public void setUseSeparateFoundSet(boolean b)
	{
		setNamedFoundSet(b ? SEPARATE_FLAG : null);
	}

	/**
	 * The text that displays in the title bar of the form window. 
	 * NOTE: Data tags and Servoy tags can be used as part of the title text. 
	 */
	public String getTitleText()
	{
		return titleText;
	}

	/**
	 * Set the form title text.
	 * 
	 * @param string
	 */
	public void setTitleText(String string)
	{
		checkForChange(titleText, string);
		titleText = string;
	}

	/**
	 * The calculation dataprovider used to add background color and highlight selected or alternate rows. 
	 * The default is -none-. 
	 * 
	 * @templatedescription Calculate the row background color
	 * @templatename rowBGColorCalculation
	 * @templatetype Color
	 * @templateparam Number index row index
	 * @templateparam Boolean selected is the row selected
	 * @templateparam String elementType element type (not supported in webclient)
	 * @templateparam String dataProviderID element data provider (not supported in webclient)
	 * @templateparam String formName form name
	 * @templateparam JSRecord record selected record
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
		return rowBGColorCalculation;
	}

	/**
	 * Set the rowBGColorCalculation
	 * 
	 * @param arg the rowBGColorCalculation
	 */
	public void setRowBGColorCalculation(String arg)
	{
		checkForChange(rowBGColorCalculation, arg);
		rowBGColorCalculation = arg;
	}

	/**
	 * The Cascading Style Sheet (CSS) class name applied to the form. 
	 */
	public String getStyleClass()
	{
		return styleClass;
	}

	/**
	 * Set the style.
	 * 
	 * @param arg the syle
	 */
	public void setStyleClass(String arg)
	{
		checkForChange(styleClass, arg);
		styleClass = arg;
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
		return onNextRecordCmdMethodID;
	}

	/**
	 * Set the method that overrides the Servoy menu item Select > Next Record.
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @param arg the method
	 */
	public void setOnNextRecordCmdMethodID(int arg)
	{
		checkForChange(onNextRecordCmdMethodID, arg);
		onNextRecordCmdMethodID = arg;
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
		return onPreviousRecordCmdMethodID;
	}

	/**
	 * Set the method that overrides the Servoy menu item Select > Previous Record. 
	 * This property is automatically set to "DEFAULT" (no override) when the form is created.
	 * 
	 * @param arg the method
	 */
	public void setOnPreviousRecordCmdMethodID(int arg)
	{
		checkForChange(onPreviousRecordCmdMethodID, arg);
		onPreviousRecordCmdMethodID = arg;
	}

	/**
	 * Set the transparent
	 * 
	 * @param arg the transparent
	 */
	public void setTransparent(boolean arg)
	{
		checkForChange(transparent, arg);
		transparent = arg;
	}

	/**
	 * When set, the form is transparent.
	 */
	public boolean getTransparent()
	{
		return transparent;
	}

	/**
	 * Set the namedFoundSet
	 * 
	 * @param arg the namedFoundSet
	 */
	public void setNamedFoundSet(String arg)
	{
		checkForChange(namedFoundSet, arg);
		namedFoundSet = arg;
	}

	/**
	 * Property that tells the form to use a named foundset instead of the default foundset.
	 * When "separate" as special value is specified the form will always create a copy of assigned foundset and therefor become separated from other foundsets
	 */
	public String getNamedFoundSet()
	{
		return namedFoundSet;
	}

	/**
	 * The method that is triggered when (non Design Mode) dragging occurs.
	 * 
	 * @templatedescription 
	 * Handle start of a drag, it can set the data that should be transfered and should return a constant which dragndrop mode/modes is/are supported
	 * 
	 * Should return a DRAGNDROP constant or a combination of 2 constants:
	 * DRAGNDROP.MOVE if only a move can happen,
	 * DRAGNDROP.COPY if only a copy can happen,
	 * DRAGNDROP.MOVE|DRAGNDROP.COPY if a move or copy can happen,
	 * DRAGNDROP.NONE if nothing is supported (drag should start).
	 * @templatename onDrag
	 * @templatetype Number
	 * @templateparam JSDNDEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return DRAGNDROP.NONE
	 */
	public int getOnDragMethodID()
	{
		return onDragMethodID;
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dragging occurs. 
	 * 
	 * @param arg the method that is triggered
	 */
	public void setOnDragMethodID(int arg)
	{
		checkForChange(onDragMethodID, arg);
		onDragMethodID = arg;
	}


	/**
	 * The method that is triggered when (non Design Mode) dragging end occurs.
	 * 
	 * @templatedescription 
	 * Handle end of a drag
	 * 
	 * @templatename onDragEnd
	 * @templatetype Number
	 * @templateparam JSDNDEvent event the event that triggered the action
	 */
	public int getOnDragEndMethodID()
	{
		return onDragEndMethodID;
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dragging end occurs. 
	 * 
	 * @param arg the method that is triggered
	 */
	public void setOnDragEndMethodID(int arg)
	{
		checkForChange(onDragEndMethodID, arg);
		onDragEndMethodID = arg;
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
	public int getOnDragOverMethodID()
	{
		return onDragOverMethodID;
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dragging over a component occurs.
	 * 
	 * @param arg the method that is triggered
	 */
	public void setOnDragOverMethodID(int arg)
	{
		checkForChange(onDragOverMethodID, arg);
		onDragOverMethodID = arg;
	}

	/**
	 * The method that is triggered when (non Design Mode) dropping occurs.
	 * 
	 * @templatedescription Handle a drop
	 * @templatename onDrop
	 * @templatetype Boolean
	 * @templateparam JSDNDEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return false
	 */
	public int getOnDropMethodID()
	{
		return onDropMethodID;
	}

	/**
	 * Set the method that gets triggered when (non Design Mode) dropping occurs.
	 * 
	 * @param arg the method that gets triggered
	 */
	public void setOnDropMethodID(int arg)
	{
		checkForChange(onDropMethodID, arg);
		onDropMethodID = arg;
	}

	/**
	 * The method that is triggered when focus is gained by a component inside the form.
	 * 
	 * @templatedescription Handle focus element gaining focus
	 * @templatename onElementFocusGained
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnElementFocusGainedMethodID()
	{
		return onElementFocusGainedMethodID;
	}

	/**
	 * Set the method that gets triggered when focus is gained.
	 * 
	 * @param arg the method that gets triggered
	 */
	public void setOnElementFocusGainedMethodID(int arg)
	{
		checkForChange(onElementFocusGainedMethodID, arg);
		onElementFocusGainedMethodID = arg;
	}

	/**
	 * The method that gets triggered when focus is lost by a component inside the form.
	 * 
	 * @templatedescription Handle focus element loosing focus
	 * @templatename onElementFocusLost
	 * @templatetype Boolean
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 * @templatecode
	 * return true
	 */
	public int getOnElementFocusLostMethodID()
	{
		return onElementFocusLostMethodID;
	}

	/**
	 * Set the method that gets triggered when focus is lost.
	 * 
	 * @param arg the method that gets triggered
	 */
	public void setOnElementFocusLostMethodID(int arg)
	{
		checkForChange(onElementFocusLostMethodID, arg);
		onElementFocusLostMethodID = arg;
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
		return onResizeMethodID;
	}

	/**
	 * Set the method that gets triggered when resize occurs.
	 * 
	 * @param arg the method that gets triggered
	 */
	public void setOnResizeMethodID(int arg)
	{
		checkForChange(onResizeMethodID, arg);
		onResizeMethodID = arg;
	}

	public void setOnRenderMethodID(int arg)
	{
		checkForChange(onRenderMethodID, arg);
		onRenderMethodID = arg;
	}

	public void setAccess(int arg)
	{
		checkForChange(access, arg);
		access = arg;
	}

	public int getAccess()
	{
		return access;
	}

	/**
	 * The method that is executed when the component is rendered.
	 */
	public int getOnRenderMethodID()
	{
		return onRenderMethodID;
	}

	public long getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(long lastModified)
	{
		this.lastModified = lastModified;
	}

	@Override
	public IPersist clonePersist()
	{
		Form formClone = (Form)super.clonePersist();
		if (size != null) formClone.setSize(new Dimension(size));
		return formClone;
	}
}
