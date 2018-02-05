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

import java.awt.Dimension;
import java.awt.Point;

/**
 * @author lvostinar
 *
 */
public class CSSPosition
{
	public int top;
	public int left;
	public int bottom;
	public int right;
	public int width;
	public int height;

	public CSSPosition(int top, int left, int bottom, int right, int width, int height)
	{
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof CSSPosition)
		{
			return this.top == ((CSSPosition)obj).top && this.left == ((CSSPosition)obj).left && this.bottom == ((CSSPosition)obj).bottom &&
				this.right == ((CSSPosition)obj).right && this.width == ((CSSPosition)obj).width && this.height == ((CSSPosition)obj).height;
		}
		return false;
	}

	public static void setLocation(ISupportBounds persist, int x, int y)
	{
		if (persist instanceof BaseComponent && ((BaseComponent)persist).getParent() instanceof Form &&
			((Form)((BaseComponent)persist).getParent()).getUseCssPosition())
		{
			CSSPosition position = ((BaseComponent)persist).getCssPosition();
			if (position == null)
			{
				position = new CSSPosition(0, 0, -1, -1, 0, 0);
			}
			Form form = (Form)((BaseComponent)persist).getParent();
			if (position.right == -1)
			{
				position.left = x;
			}
			else if (position.left == -1)
			{
				position.right = form.getWidth() - x - position.width;
			}
			else
			{
				position.right = position.right + position.left - x;
				position.left = x;
			}
			if (position.bottom == -1)
			{
				position.top = y;
			}
			else if (position.top == -1)
			{
				position.bottom = form.getSize().height - y - position.height;
			}
			else
			{
				position.bottom = position.bottom + position.top - y;
				position.top = y;
			}
			((BaseComponent)persist).setCssPosition(position);
		}
		else
		{
			persist.setLocation(new Point(x, y));
		}
	}

	public static void setSize(ISupportBounds persist, int width, int height)
	{
		if (persist instanceof BaseComponent && ((BaseComponent)persist).getParent() instanceof Form &&
			((Form)((BaseComponent)persist).getParent()).getUseCssPosition())
		{
			CSSPosition position = ((BaseComponent)persist).getCssPosition();
			if (position == null)
			{
				position = new CSSPosition(0, 0, -1, -1, 0, 0);
			}
			Form form = (Form)((BaseComponent)persist).getParent();
			if (position.left >= 0 && position.right >= 0)
			{
				position.right = form.getWidth() - position.left - width;
			}
			else
			{
				position.width = width;
			}
			if (position.top >= 0 && position.bottom >= 0)
			{
				position.bottom = form.getSize().height - position.top - height;
			}
			else
			{
				position.height = height;
			}
			((BaseComponent)persist).setCssPosition(position);
		}
		else
		{
			persist.setSize(new Dimension(width, height));
		}
	}

	public static Point getLocation(ISupportBounds persist)
	{
		if (persist instanceof BaseComponent && ((BaseComponent)persist).getParent() instanceof Form &&
			((Form)((BaseComponent)persist).getParent()).getUseCssPosition())
		{
			CSSPosition position = ((BaseComponent)persist).getCssPosition();
			if (position == null)
			{
				position = new CSSPosition(0, 0, -1, -1, 0, 0);
			}
			int top = position.top;
			int left = position.left;
			Form form = (Form)((BaseComponent)persist).getParent();
			if (left == -1)
			{
				// not set, we should calculate it then
				if (position.right >= 0 && position.width >= 0)
				{
					left = form.getWidth() - position.right - position.width;
				}
			}
			if (top == -1)
			{
				// not set, we should calculate it then
				if (position.height >= 0 && position.bottom >= 0)
				{
					top = form.getSize().height - position.height - position.bottom;
				}
			}
			return new Point(left, top);
		}
		return persist.getLocation();
	}

	public static Dimension getSize(ISupportSize persist)
	{
		if (persist instanceof BaseComponent && ((BaseComponent)persist).getParent() instanceof Form &&
			((Form)((BaseComponent)persist).getParent()).getUseCssPosition())
		{
			CSSPosition position = ((BaseComponent)persist).getCssPosition();
			if (position == null)
			{
				position = new CSSPosition(0, 0, -1, -1, 0, 0);
			}
			int width = position.width;
			int height = position.height;
			Form form = (Form)((BaseComponent)persist).getParent();
			if (position.left >= 0 && position.right >= 0)
			{
				width = form.getWidth() - position.right - position.left;
			}
			if (position.top >= 0 && position.bottom >= 0)
			{
				height = form.getSize().height - position.top - position.bottom;
			}
			return new Dimension(width, height);
		}
		return persist.getSize();
	}
}
