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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * @author lvostinar
 *
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLayoutContainer")
public class JSLayoutContainer extends JSBaseContainer implements IJSParent<LayoutContainer>, IJavaScriptType
{
	private LayoutContainer layoutContainer;
	private final IJSParent< ? > parent;
	private boolean isCopy = false;

	public JSLayoutContainer(IJSParent< ? > parent, IApplication application, LayoutContainer layoutContainer)
	{
		super(application);
		this.setLayoutContainer(layoutContainer);
		this.parent = parent;
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
	 * @return String
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
	 * @return String
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
	 * layoutContainer.style = "background-color:'red'";
	 */
	@JSGetter
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
	 * Get/set container height. This is only used for Absolute Layout Container.
	 *
	 * @sample
	 * layoutContainer.height = 300;
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
	 * Get/set container width. This is only used for Absolute Layout Container.
	 *
	 * @sample
	 * layoutContainer.width = 300;
	 */
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

	@Override
	public AbstractContainer getFlattenedContainer()
	{
		return getLayoutContainer();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "JSLayoutContainer[" + getTagType() + ", attributes: " + getLayoutContainer().getAttributes() + "]";
	}
}
