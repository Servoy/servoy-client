/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.j2db.scripting;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSBounds")
public class JSBounds
{
	private int x;
	private int y;
	private int width;
	private int height;


	public JSBounds(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/**
	 * Get/Set x coordinate.
	 *
	 * @sample
	 * bounds.x
	 *
	 * @return x coordinate
	 */
	@JSGetter
	public int getX()
	{
		return this.x;
	}

	@JSSetter
	public void setX(int x)
	{
		this.x = x;
	}

	/**
	 * Get/Set y coordinate
	 *
	 * @sample
	 * bounds.y
	 *
	 * @return y
	 */
	@JSGetter
	public int getY()
	{
		return this.y;
	}

	@JSGetter
	public void setY(int y)
	{
		this.y = y;
	}

	/**
	 * Get/Set width.
	 *
	 * @sample
	 * bounds.width
	 *
	 * @return width
	 */
	@JSGetter
	public int getWidth()
	{
		return this.width;
	}

	@JSSetter
	public void setWidth(int width)
	{
		this.width = width;
	}

	/**
	 * Get/Set height
	 *
	 * @sample
	 * bounds.height
	 *
	 * @return height
	 */
	@JSGetter
	public int getHeight()
	{
		return this.height;
	}

	@JSGetter
	public void setHeight(int height)
	{
		this.height = height;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "JSBounds(x = " + x + ", y = " + y + ", width = " + width + ", height = " + height + ')'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
