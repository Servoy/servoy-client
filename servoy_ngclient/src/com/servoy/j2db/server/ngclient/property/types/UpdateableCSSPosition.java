/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.scripting.solutionmodel.ICSSPosition;

public class UpdateableCSSPosition implements ICSSPosition
{

	private final IWebObjectContext comp;
	private final PropertyDescription pd;
	private final CSSPosition pos;

	UpdateableCSSPosition(CSSPosition pos, IWebObjectContext comp, PropertyDescription pd)
	{
		this.pos = pos;
		this.comp = comp;
		this.pd = pd;
	}

	private void flagComponent()
	{
		comp.getUnderlyingWebObject().setProperty(pd.getName(), pos);
		comp.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
	}

	/**
	 * Set left css position (in pixels or percent).
	 *
	 * @param l left position in pixels or percentage
	 *
	 * @sample
	 * comp.cssPosition.l("10").t("10").w("20%").h("30px")
	 *
	 * @return css position
	 */
	@JSFunction
	public ICSSPosition l(String l)
	{
		this.pos.left = l;
		flagComponent();
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
	@JSFunction
	public ICSSPosition r(String r)
	{
		this.pos.right = r;
		flagComponent();
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
	@JSFunction
	public ICSSPosition t(String t)
	{
		this.pos.top = t;
		flagComponent();
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
	@JSFunction
	public ICSSPosition b(String b)
	{
		this.pos.bottom = b;
		flagComponent();
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
	@JSFunction
	public ICSSPosition w(String w)
	{
		this.pos.width = w;
		flagComponent();
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
	@JSFunction
	public ICSSPosition h(String h)
	{
		this.pos.height = h;
		flagComponent();
		return this;
	}

	/**
	 * @param right the right to set
	 */
	@JSSetter
	public void setRight(String right)
	{
		this.pos.right = right;
		flagComponent();
	}

	/**
	 * @return the right
	 */
	@JSGetter
	public String getRight()
	{
		return pos.right;
	}

	@Override
	public String getLeft()
	{
		return pos.left;
	}

	@Override
	public void setLeft(String left)
	{
		this.pos.left = left;
		flagComponent();
	}

	@Override
	public String getTop()
	{
		return pos.top;
	}

	@Override
	public void setTop(String top)
	{
		this.pos.top = top;
		flagComponent();

	}

	@Override
	public String getBottom()
	{
		return pos.bottom;
	}

	@Override
	public void setBottom(String bottom)
	{
		this.pos.bottom = bottom;
		flagComponent();
	}

	@Override
	public String getWidth()
	{
		return pos.width;
	}

	@Override
	public void setWidth(String width)
	{
		this.pos.width = width;
		flagComponent();

	}

	@Override
	public String getHeight()
	{
		return pos.height;
	}

	@Override
	public void setHeight(String height)
	{
		this.pos.height = height;
		flagComponent();
	}

}