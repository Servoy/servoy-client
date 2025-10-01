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
 * The <code>JSDimension</code> object represents a dimension in scripting, defined by its <code>height</code> and
 * <code>width</code> properties. It is used to manage size-related attributes dynamically.
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "dimension")
public class JSDimension
{
	/**
	 * Get/Set width.
	 *
	 * @sample
	 * dimension.width
	 *
	 * @return width
	 */
	@JSGetter
	public int getWidth()
	{
		return 0;
	}

	@JSSetter
	public void setWidth(int width)
	{
	}

	/**
	 * Get/Set height
	 *
	 * @sample
	 * dimension.height
	 *
	 * @return height
	 */
	@JSGetter
	public int getHeight()
	{
		return 0;
	}

	@JSGetter
	public void setHeight(int height)
	{
	}
}
