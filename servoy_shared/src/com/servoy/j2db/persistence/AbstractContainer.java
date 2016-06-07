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

package com.servoy.j2db.persistence;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public abstract class AbstractContainer extends AbstractBase
	implements ISupportFormElements, ISupportExtendsID, ISupportUpdateableName, IPersistCloneable, ICloneable
{

	private static final long serialVersionUID = 1L;

	protected AbstractContainer(int type, ISupportChilds parent, int element_id, UUID uuid)
	{
		super(type, parent, element_id, uuid);
	}

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. This method shouldn't be called from outside the persistance package!!
	 *
	 * @param arg the form name
	 * @exclude
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		if (validateName(arg))
		{
			validator.checkName(arg, getID(), new ValidatorSearchContext(getAncestor(IRepository.FORMS), getTypeID()), false);
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	protected boolean validateName(String newName)
	{
		return true;
	}

	/**
	 * The name of the form.
	 */
	@ServoyClientSupport(mc = true, wc = true, sc = true)
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the container size.
	 *
	 * @param arg the size
	 */
	public void setSize(Dimension arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE, arg);
	}

	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null)
		{
			return new java.awt.Dimension(20, 20);
		}
		return size;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportExtendsID#getExtendsID()
	 */
	@Override
	public int getExtendsID()
	{
		if (getTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID) != null)
			return getTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID).intValue();
		else return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ISupportExtendsID#setExtendsID(int)
	 */
	@Override
	public void setExtendsID(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_EXTENDSID, arg);

	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return PersistHelper.getFlattenedPropertiesMap(this);
	}

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

	public FormReference createNewFormReference(Point location) throws RepositoryException
	{
		FormReference obj = (FormReference)getSolution().getChangeHandler().createNewObject(this, IRepository.FORMREFERENCE);

		//set all the required properties
		obj.setLocation(location);

		addChild(obj);
		return obj;
	}

	/**
	 * Get the all the child layout containers.
	 *
	 * @return the layout containers
	 */
	public Iterator<LayoutContainer> getLayoutContainers()
	{
		return getObjects(IRepository.LAYOUTCONTAINERS);
	}

	/**
	 * Get the all the child form references.
	 *
	 * @return the form references elements
	 */
	public Iterator<FormReference> getFormReferences()
	{
		return getObjects(IRepository.FORMREFERENCE);
	}

	/**
	 * Create a new layout container.
	 *
	 * @return the field
	 */
	public LayoutContainer createNewLayoutContainer() throws RepositoryException
	{
		LayoutContainer obj = (LayoutContainer)getSolution().getChangeHandler().createNewObject(this, IRepository.LAYOUTCONTAINERS);

		addChild(obj);
		return obj;
	}

	public Solution getSolution()
	{
		return (Solution)getRootObject();
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
		Portal obj = (Portal)getRootObject().getChangeHandler().createNewObject(this, IRepository.PORTALS);
		//set all the required properties

		obj.setLocation(location);
		obj.setName(name == null ? "untitled" : name); //$NON-NLS-1$

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
		Bean obj = (Bean)getRootObject().getChangeHandler().createNewObject(this, IRepository.BEANS);
		//set all the required properties

		obj.setName(name == null ? "untitled" : name); //$NON-NLS-1$
		obj.setBeanClassName(className);

		addChild(obj);
		return obj;
	}

	/**
	 * Create a new web component.
	 *
	 * @param name the name of the bean
	 * @param className the class name
	 * @return the new bean
	 */
	public WebComponent createNewWebComponent(String name, String type) throws RepositoryException
	{
		WebComponent obj = (WebComponent)getRootObject().getChangeHandler().createNewObject(this, IRepository.WEBCOMPONENTS);
		//set all the required properties

		if (name != null) obj.setName(name);
		obj.setTypeName(type);

		addChild(obj);
		return obj;
	}

	public Iterator<WebComponent> getWebComponents()
	{
		return getObjects(IRepository.WEBCOMPONENTS);
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

	public List<IPersist> getHierarchyChildren()
	{
		return getAllObjectsAsList();
	}

	/**
	 * Flatten this containers containment hierarchy into a list and return it sorted if the given comparator is not null.
	 * @param comparator
	 * @return
	 */
	public List<IFormElement> getFlattenedObjects(Comparator< ? super IFormElement> comparator)
	{
		List<IFormElement> flattenedPersists = new ArrayList<IFormElement>();
		List<IPersist> children = getHierarchyChildren();
		for (IPersist persist : children)
		{
			if (persist instanceof AbstractContainer)
			{
				List<IFormElement> flattenedObjects = ((AbstractContainer)persist).getFlattenedObjects(comparator);
				for (IFormElement element : flattenedObjects)
				{
					if (!flattenedPersists.contains(element)) flattenedPersists.add(element);
				}
			}
			else if (persist instanceof IFormElement)
			{
				flattenedPersists.add((IFormElement)persist);
			}
		}
		IFormElement[] array = flattenedPersists.toArray(new IFormElement[flattenedPersists.size()]);
		if (comparator != null)
		{
			Arrays.sort(array, comparator);
		}
		return new ArrayList<IFormElement>(Arrays.<IFormElement> asList(array));
	}

	public List<IPersist> getFlattenedFormElementsAndLayoutContainers()
	{
		List<IPersist> flattenedPersists = new ArrayList<IPersist>();
		List<IPersist> children = getHierarchyChildren();
		for (IPersist persist : children)
		{
			if (persist instanceof AbstractContainer)
			{
				if (persist instanceof LayoutContainer) flattenedPersists.add(persist);
				flattenedPersists.addAll(((AbstractContainer)persist).getFlattenedFormElementsAndLayoutContainers());
			}
			else if (persist instanceof IFormElement)
			{
				flattenedPersists.add(persist);
			}
		}

		return flattenedPersists;
	}

	/**
	 * Search this containers containment hierarchy recursively for the given uuid.
	 * @param searchFor
	 * @return
	 */

	public IPersist findChild(UUID searchFor)
	{
		return findChild(searchFor, false);
	}

	public IPersist findChild(UUID searchFor, boolean searchInFormReferences)
	{
		List<IPersist> children = getHierarchyChildren();
		for (IPersist iPersist : children)
		{
			if (iPersist.getUUID().equals(searchFor)) return iPersist;
			if (iPersist instanceof LayoutContainer || (searchInFormReferences && iPersist instanceof FormReference))
			{
				IPersist result = ((AbstractContainer)iPersist).findChild(searchFor, searchInFormReferences);
				if (result != null) return result;
			}
		}
		return null;
	}

	public ArrayList<IPersist> getSortedChildren()
	{
		ArrayList<IPersist> children = new ArrayList<IPersist>();
		Iterator<IPersist> it = getAllObjects();
		while (it.hasNext())
		{
			IPersist p = it.next();
			if (p instanceof ISupportBounds)
			{
				children.add(p);
			}
		}
		IPersist[] sortedChildArray = children.toArray(new IPersist[0]);
		Arrays.sort(sortedChildArray, PositionComparator.XY_PERSIST_COMPARATOR);
		children = new ArrayList<IPersist>(Arrays.asList(sortedChildArray));
		return children;
	}

	public List<FormReference> getFormReferencesWithoutNesting()
	{
		final List<FormReference> formReferences = new ArrayList<>();
		this.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof FormReference && o != AbstractContainer.this)
				{
					formReferences.add((FormReference)o);
					return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		Collections.sort(formReferences, NameComparator.INSTANCE);
		return formReferences;
	}
}
