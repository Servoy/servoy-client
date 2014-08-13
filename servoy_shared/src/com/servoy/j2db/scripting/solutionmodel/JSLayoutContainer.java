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
	private final IApplication application;
	protected LayoutContainer layoutContainer;
	private final IJSParent< ? > parent;
	private boolean isCopy = false;

	public JSLayoutContainer(IJSParent< ? > parent, IApplication application, LayoutContainer layoutContainer)
	{
		super(application);
		this.application = application;
		this.layoutContainer = layoutContainer;
		this.parent = parent;
	}

	public LayoutContainer getSupportChild()
	{
		return layoutContainer;
	}

	public IJSParent< ? > getJSParent()
	{
		return parent;
	}

	@Override
	public void checkModification()
	{
		parent.checkModification();
		if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			layoutContainer = (LayoutContainer)parent.getSupportChild().getChild(layoutContainer.getUUID());
			isCopy = true;
		}
	}

	@Override
	public AbstractContainer getContainer()
	{
		return layoutContainer;
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
		return layoutContainer.getTagType();
	}

	@JSSetter
	public void setTagType(String tagType)
	{
		checkModification();
		layoutContainer.setTagType(tagType);
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
		return layoutContainer.getElementId();
	}

	@JSSetter
	public void setElementId(String elementId)
	{
		checkModification();
		layoutContainer.setElementId(elementId);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.LayoutContainer#getCSSClasses()
	 * 
	 * @sample
	 * layoutContainer.cssClasses = 'myContainer';
	 */
	@JSGetter
	public String getCSSClasses()
	{
		return layoutContainer.getCSSClasses();
	}

	@JSSetter
	public void setCSSClasses(String cssClasses)
	{
		checkModification();
		layoutContainer.setCSSClasses(cssClasses);
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
		return layoutContainer.getStyle();
	}

	@JSSetter
	public void setStyle(String style)
	{
		checkModification();
		layoutContainer.setStyle(style);
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
		return layoutContainer.getName();
	}

	@JSSetter
	public void setName(String arg)
	{
		layoutContainer.setName(arg);
	}
}
