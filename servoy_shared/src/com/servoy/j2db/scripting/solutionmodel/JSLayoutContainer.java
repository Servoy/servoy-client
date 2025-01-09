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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.FlattenedLayoutContainer;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.PersistHelper;

/**
 * <p>The <b>JSLayoutContainer</b> is a JavaScript object designed to facilitate the management and manipulation
 * of layout containers in the Solution Model. It supports both hierarchical navigation and dynamic creation
 * of containers, offering extensive control over layout structures. This object is particularly useful for
 * building responsive designs and managing nested layouts efficiently.</p>
 *
 * <p>The container's properties allow users to define dimensions, positions, and appearance attributes.
 * For example, height, <code>x</code>, and <code>y</code> coordinates control spatial placement, while
 * <code>cssClasses</code> and <code>tagType</code> customize the HTML output. Identifiers such as
 * <code>elementId</code>, <code>name</code>, <code>packageName</code>, and <code>specName</code> ensure clear
 * referencing and seamless integration with layout specifications.</p>
 *
 * <p>The methods provided enable comprehensive interaction with components and containers. Users can add,
 * retrieve, or remove components through functions like <code>newWebComponent(type)</code> or
 * <code>getComponent(name)</code>. Attribute management is also supported, allowing customization with
 * methods like <code>putAttribute(key, value)</code>. Additionally, hierarchical navigation is made simple
 * with functions such as <code>findLayoutContainer(name)</code> and <code>getLayoutContainers()</code>, which
 * aid in accessing or organizing nested structures dynamically.</p>
 *
 * <p>These features collectively make the <b>JSLayoutContainer</b> a robust tool for developing adaptable
 * and scalable form layouts.</p>
 *
 * @author lvostinar
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLayoutContainer")
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class JSLayoutContainer extends JSBaseContainer<LayoutContainer> implements IJavaScriptType
{
	private LayoutContainer layoutContainer;
	private final IJSParent< ? > parent;
	private boolean isCopy = false;
	private final IApplication application;

	public JSLayoutContainer(IJSParent< ? > parent, IApplication application, LayoutContainer layoutContainer)
	{
		super(application);
		this.setLayoutContainer(layoutContainer);
		this.parent = parent;
		this.application = application;
	}

	private LayoutContainer getLayoutContainer()
	{
		if (!isCopy)
		{
			// as long as the layoutcontainer is not already a copy, we have to get the real one
			// so that changes to other instances of JSLayoutContainer that points to the same container are seen in this one.
			LayoutContainer lc = (LayoutContainer)parent.getSupportChild().getChild(layoutContainer.getUUID());
			layoutContainer = lc != null ? lc : layoutContainer;
		}
		return layoutContainer;
	}

	private void setLayoutContainer(LayoutContainer layoutContainer)
	{
		this.layoutContainer = layoutContainer;
	}

	public LayoutContainer getSupportChild()
	{
		return getLayoutContainer();
	}

	public IJSParent< ? > getJSParent()
	{
		return parent;
	}

	@Override
	public void checkModification()
	{
		parent.checkModification();
		LayoutContainer tempPersist = layoutContainer;
		if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			setLayoutContainer((LayoutContainer)parent.getSupportChild().getChild(getLayoutContainer().getUUID()));
			isCopy = true;
		}
		layoutContainer = (LayoutContainer)JSBase.getOverridePersistIfNeeded(tempPersist, layoutContainer, getJSParent());
	}

	@Override
	public AbstractContainer getContainer()
	{
		return getLayoutContainer();
	}


	/**
	 * set the layout spec name
	 *
	 * @param name
	 */
	@JSSetter
	public void setSpecName(String name)
	{
		checkModification();
		getLayoutContainer().setSpecName(name);
	}

	/**
	 * returns the layouts spec name
	 *
	 * @return The specification name of the layout container.
	 */
	@JSGetter
	public String getSpecName()
	{
		return getLayoutContainer().getSpecName();
	}

	/**
	 * set the layout package name
	 *
	 * @param name
	 */
	@JSSetter
	public void setPackageName(String name)
	{
		checkModification();
		getLayoutContainer().setPackageName(name);
	}

	/**
	 * returns the layouts package name
	 *
	 * @return The package name associated with the layout container.
	 */
	@JSGetter
	public String getPackageName()
	{
		return getLayoutContainer().getPackageName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getTagType()
	 *
	 * @sample
	 * layoutContainer.tagType = 'span';
	 *
	 * @return The tag type (e.g., 'div', 'span') of the layout container.
	 */
	@JSGetter
	public String getTagType()
	{
		return getLayoutContainer().getTagType();
	}

	@JSSetter
	public void setTagType(String tagType)
	{
		checkModification();
		getLayoutContainer().setTagType(tagType);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getElementId()
	 *
	 * @sample
	 * layoutContainer.elementId = 'rowCol';
	 *
	 * @return The unique element ID of the layout container.
	 */
	@JSGetter
	public String getElementId()
	{
		return getLayoutContainer().getElementId();
	}

	@JSSetter
	public void setElementId(String elementId)
	{
		checkModification();
		getLayoutContainer().setElementId(elementId);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getCssClasses()
	 *
	 * @sample
	 * layoutContainer.cssClasses = 'myContainer';
	 *
	 * @return A space-separated string of CSS classes applied to the layout container.
	 */
	@JSGetter
	public String getCssClasses()
	{
		return getLayoutContainer().getCssClasses();
	}

	@JSSetter
	public void setCssClasses(String cssClasses)
	{
		checkModification();
		getLayoutContainer().setCssClasses(cssClasses);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getAttribute()
	 *
	 * @param name the attributes name
	 *
	 * @sample
	 * layoutContainer.getAttribute('class');
	 *
	 * @return The value of the specified attribute, or null if the attribute is not set.
	 */
	@JSFunction
	public String getAttribute(String name)
	{
		return getLayoutContainer().getAttribute(name);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#putAttribute()
	 *
	 * @sample
	 * layoutContainer.putAttribute('class','container fluid');
	 *
	 * @param key
	 * @param value
	 */
	@JSFunction
	public void putAttribute(String name, String value)
	{
		checkModification();
		getLayoutContainer().putAttribute(name, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getStyle()
	 *
	 * @sample
	 * layoutContainer.style = "background-color:red";
	 */
	@JSGetter
	@Deprecated
	public String getStyle()
	{
		return getLayoutContainer().getStyle();
	}

	@JSSetter
	public void setStyle(String style)
	{
		checkModification();
		getLayoutContainer().setStyle(style);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.BaseComponent#getName()
	 *
	 * @sample
	 * layoutContainer.name = 'col1';
	 *
	 * @return The name of the layout container.
	 */
	@JSGetter
	public String getName()
	{
		return getLayoutContainer().getName();
	}

	@JSSetter
	public void setName(String arg)
	{
		getLayoutContainer().setName(arg);
	}

	/**
	 * Get/set x location. Location is used for ordering in html output.
	 *
	 * @sample
	 * layoutContainer.x = 100;;
	 *
	 * @return The X-coordinate of the layout container's position.
	 */
	@JSGetter
	public int getX()
	{
		return getLayoutContainer().getLocation().x;
	}

	@JSSetter
	public void setX(int x)
	{
		checkModification();
		getLayoutContainer().setLocation(new Point(x, getLayoutContainer().getLocation().y));
	}

	/**
	 * Get/set Y location. Location is used for ordering in html output.
	 *
	 * @sample
	 * layoutContainer.y = 100;;
	 *
	 * @return The Y-coordinate of the layout container's position.
	 */
	@JSGetter
	public int getY()
	{
		return getLayoutContainer().getLocation().y;
	}

	@JSSetter
	public void setY(int y)
	{
		checkModification();
		getLayoutContainer().setLocation(new Point(getLayoutContainer().getLocation().x, y));
	}

	/**
	 * Get/set container height. This is only used for CSS Position Container.
	 *
	 * @sample
	 * layoutContainer.height = 300;
	 *
	 * @return The height of the layout container.
	 */
	@JSGetter
	public int getHeight()
	{
		return getLayoutContainer().getSize().height;
	}

	@JSSetter
	public void setHeight(int height)
	{
		checkModification();
		getLayoutContainer().setSize(new Dimension(getLayoutContainer().getSize().width, height));
	}

	/**
	 * Get/set container width. This is only used for CSS Position Container.
	 *
	 * @deprecated
	 * @sample
	 * layoutContainer.width = 300;
	 *
	 * @return The width of the layout container.
	 */
	@Deprecated
	@JSGetter
	public int getWidth()
	{
		return getLayoutContainer().getSize().width;
	}

	@JSSetter
	public void setWidth(int width)
	{
		checkModification();
		getLayoutContainer().setSize(new Dimension(width, getLayoutContainer().getSize().height));
	}

	/**
	 * Remove a layout container (with all its children) from hierarchy.
	 *
	 * @sample
	 * layoutContainer.remove();
	 */
	@JSFunction
	public void remove()
	{
		checkModification();
		parent.getSupportChild().removeChild(getLayoutContainer());
	}

	@Override
	public AbstractContainer getFlattenedContainer()
	{
		LayoutContainer lc = getLayoutContainer();
		return (FlattenedLayoutContainer)PersistHelper.getFlattenedPersist(application.getFlattenedSolution(),
			(Form)lc.getAncestor(IRepository.FORMS),
			lc);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "JSLayoutContainer[" + getTagType() + ", attributes: " + getLayoutContainer().getMergedAttributes() + "]";
	}
}
