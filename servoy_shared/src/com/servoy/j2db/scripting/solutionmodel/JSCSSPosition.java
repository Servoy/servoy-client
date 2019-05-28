/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "CSSPosition")
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class JSCSSPosition implements ICSSPosition
{
	private final JSComponent< ? extends BaseComponent> component;

	public JSCSSPosition(JSComponent< ? extends BaseComponent> component)
	{
		super();
		this.component = component;
	}

	/**
	 * Get/Set left css position (in pixels or percent).
	 *
	 * @sample
	 * var left = comp.cssPosition.left;
	 *
	 */
	@Override
	@JSGetter
	public String getLeft()
	{
		CSSPosition position = component.getBaseComponent(false).getCssPosition();
		if (position != null)
		{
			return position.left;
		}
		return null;
	}

	@JSSetter
	public void setLeft(String left)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.left = left;
			component.getBaseComponent(true).setCssPosition(position);
		}
	}

	/**
	 * Get/Set right css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.right
	 *
	 */
	@Override
	@JSGetter
	public String getRight()
	{
		CSSPosition position = component.getBaseComponent(false).getCssPosition();
		if (position != null)
		{
			return position.right;
		}
		return null;
	}

	@JSSetter
	public void setRight(String right)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.right = right;
			component.getBaseComponent(true).setCssPosition(position);
		}
	}

	/**
	 * Get/Set top css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.top
	 *
	 */
	@Override
	@JSGetter
	public String getTop()
	{
		CSSPosition position = component.getBaseComponent(false).getCssPosition();
		if (position != null)
		{
			return position.top;
		}
		return null;
	}

	@JSSetter
	public void setTop(String top)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.top = top;
			component.getBaseComponent(true).setCssPosition(position);
		}
	}


	/**
	 * Get/Set bottom css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.bottom
	 *
	 */
	@Override
	@JSGetter
	public String getBottom()
	{
		CSSPosition position = component.getBaseComponent(false).getCssPosition();
		if (position != null)
		{
			return position.bottom;
		}
		return null;
	}


	@JSSetter
	public void setBottom(String bottom)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.bottom = bottom;
			component.getBaseComponent(true).setCssPosition(position);
		}
	}

	/**
	 * Get/Set width css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.width
	 *
	 * @return width css position
	 */
	@Override
	@JSGetter
	public String getWidth()
	{
		CSSPosition position = component.getBaseComponent(false).getCssPosition();
		if (position != null)
		{
			return position.width;
		}
		return null;
	}

	@JSSetter
	public void setWidth(String width)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.width = width;
			component.getBaseComponent(true).setCssPosition(position);
		}
	}

	/**
	 * Get height css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.h()
	 *
	 * @return height css position
	 */
	@Deprecated
	@JSGetter
	public String getHeigth()
	{
		return getHeight();
	}


	/**
	 * Set height css position (in pixels or percent).
	 *
	 * @param height height position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Deprecated
	@JSGetter
	public void setHeigth(String height)
	{
		setHeight(height);
	}

	/**
	 * Get/Set height css position (in pixels or percent).
	 *
	 * @sample
	 * comp.cssPosition.height
	 *
	 */
	@Override
	@JSGetter
	public String getHeight()
	{
		CSSPosition position = component.getBaseComponent(false).getCssPosition();
		if (position != null)
		{
			return position.height;
		}
		return null;
	}

	@JSGetter
	public void setHeight(String height)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.height = height;
			component.getBaseComponent(true).setCssPosition(position);
		}
	}

	/**
	 * Set left css position (in pixels or percent).
	 *
	 * @param left left position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Override
	@JSFunction
	public ICSSPosition l(String left)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.left = left;
			component.getBaseComponent(true).setCssPosition(position);
		}
		return this;
	}

	/**
	 * Set right css position (in pixels or percent).
	 *
	 * @param right right position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.r("10").b("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Override
	@JSFunction
	public ICSSPosition r(String right)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.right = right;
			component.getBaseComponent(true).setCssPosition(position);
		}
		return this;
	}

	/**
	 * Set top css position (in pixels or percent).
	 *
	 * @param top top position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Override
	@JSFunction
	public ICSSPosition t(String top)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.top = top;
			component.getBaseComponent(true).setCssPosition(position);
		}
		return this;
	}

	/**
	 * Set bottom css position (in pixels or percent).
	 *
	 * @param bottom bottom position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.r("10").b("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Override
	@JSFunction
	public ICSSPosition b(String bottom)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.bottom = bottom;
			component.getBaseComponent(true).setCssPosition(position);
		}
		return this;
	}

	/**
	 * Set width css position (in pixels or percent).
	 *
	 * @param width width position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Override
	@JSFunction
	public ICSSPosition w(String width)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.width = width;
			component.getBaseComponent(true).setCssPosition(position);
		}
		return this;
	}

	/**
	 * Set height css position (in pixels or percent).
	 *
	 * @param height height position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@Override
	@JSFunction
	public ICSSPosition h(String height)
	{
		CSSPosition position = component.getBaseComponent(true).getCssPosition();
		if (position != null)
		{
			position.height = height;
			component.getBaseComponent(true).setCssPosition(position);
		}
		return this;
	}
}
