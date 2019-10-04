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

package com.servoy.j2db.persistence;

import java.io.Serializable;

import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class CSSPosition implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String top;
	public String left;
	public String bottom;
	public String right;
	public String width;
	public String height;

	public CSSPosition(String top, String right, String bottom, String left, String width, String height)
	{
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.left = left;
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof CSSPosition)
		{
			return Utils.equalObjects(this.top, ((CSSPosition)obj).top) && Utils.equalObjects(this.left, ((CSSPosition)obj).left) &&
				Utils.equalObjects(this.bottom, ((CSSPosition)obj).bottom) && Utils.equalObjects(this.right, ((CSSPosition)obj).right) &&
				Utils.equalObjects(this.width, ((CSSPosition)obj).width) && Utils.equalObjects(this.height, ((CSSPosition)obj).height);
		}
		return false;
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("{");
		buffer.append("top:");
		buffer.append(this.top);
		buffer.append(",");
		buffer.append("right:");
		buffer.append(this.right);
		buffer.append(",");
		buffer.append("bottom:");
		buffer.append(this.bottom);
		buffer.append(",");
		buffer.append("left:");
		buffer.append(this.left);
		buffer.append(",");
		buffer.append("width:");
		buffer.append(this.width);
		buffer.append(",");
		buffer.append("height:");
		buffer.append(this.height);
		buffer.append("}");
		return buffer.toString();
	}
}
