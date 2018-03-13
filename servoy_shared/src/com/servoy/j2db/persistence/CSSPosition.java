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

import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class CSSPosition
{
	public String top;
	public String left;
	public String bottom;
	public String right;
	public String width;
	public String height;

	public CSSPosition(String top, String left, String bottom, String right, String width, String height)
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
			return Utils.equalObjects(this.top, ((CSSPosition)obj).top) && Utils.equalObjects(this.left, ((CSSPosition)obj).left) &&
				Utils.equalObjects(this.bottom, ((CSSPosition)obj).bottom) && Utils.equalObjects(this.right, ((CSSPosition)obj).right) &&
				Utils.equalObjects(this.width, ((CSSPosition)obj).width) && Utils.equalObjects(this.height, ((CSSPosition)obj).height);
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
				position = new CSSPosition("0", "0", "-1", "-1", "0", "0");
			}
			Form form = (Form)((BaseComponent)persist).getParent();
			if (!isSet(position.right))
			{
				position.left = pixelsToPercentage(x, form.getWidth(), position.left);
			}
			else if (!isSet(position.left))
			{
				position.right = pixelsToPercentage(form.getWidth() - x - percentageToPixels(position.width, form.getWidth()), form.getWidth(), position.right);
			}
			else
			{
				position.right = pixelsToPercentage(
					percentageToPixels(position.right, form.getWidth()) + percentageToPixels(position.left, form.getWidth()) - x, form.getWidth(),
					position.right);
				position.left = pixelsToPercentage(x, form.getWidth(), position.left);
			}
			if (!isSet(position.bottom))
			{
				position.top = pixelsToPercentage(y, form.getSize().height, position.top);
			}
			else if (!isSet(position.top))
			{
				position.bottom = pixelsToPercentage(form.getSize().height - y - percentageToPixels(position.height, form.getSize().height),
					form.getSize().height, position.bottom);
			}
			else
			{
				position.bottom = pixelsToPercentage(
					percentageToPixels(position.bottom, form.getSize().height) + percentageToPixels(position.top, form.getSize().height) - y,
					form.getSize().height, position.bottom);
				position.top = pixelsToPercentage(y, form.getSize().height, position.top);
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
				position = new CSSPosition("0", "0", "-1", "-1", "0", "0");
			}
			Form form = (Form)((BaseComponent)persist).getParent();
			if (isSet(position.left) && isSet(position.right))
			{
				position.right = pixelsToPercentage(form.getWidth() - percentageToPixels(position.left, form.getWidth()) - width, form.getWidth(),
					position.right);
			}
			else
			{
				position.width = pixelsToPercentage(width, form.getWidth(), position.width);
			}
			if (isSet(position.top) && isSet(position.bottom))
			{
				position.bottom = pixelsToPercentage(form.getSize().height - percentageToPixels(position.top, form.getSize().height) - height,
					form.getSize().height, position.bottom);
			}
			else
			{
				position.height = pixelsToPercentage(height, form.getSize().height, position.height);
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
				position = new CSSPosition("0", "0", "-1", "-1", "0", "0");
			}
			Form form = (Form)((BaseComponent)persist).getParent();
			int top = percentageToPixels(position.top, form.getSize().height);
			int left = percentageToPixels(position.left, form.getWidth());
			if (left == -1)
			{
				// not set, we should calculate it then
				int right = percentageToPixels(position.right, form.getWidth());
				int width = percentageToPixels(position.width, form.getWidth());
				if (right >= 0 && width >= 0)
				{
					left = form.getWidth() - right - width;
				}
			}
			if (top == -1)
			{
				// not set, we should calculate it then
				int height = percentageToPixels(position.height, form.getSize().height);
				int bottom = percentageToPixels(position.bottom, form.getSize().height);
				if (height >= 0 && bottom >= 0)
				{
					top = form.getSize().height - height - bottom;
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
				position = new CSSPosition("0", "0", "-1", "-1", "0", "0");
			}
			Form form = (Form)((BaseComponent)persist).getParent();
			int width = percentageToPixels(position.width, form.getWidth());
			int height = percentageToPixels(position.height, form.getSize().height);
			int left = percentageToPixels(position.left, form.getWidth());
			int right = percentageToPixels(position.right, form.getWidth());
			int top = percentageToPixels(position.top, form.getSize().height);
			int bottom = percentageToPixels(position.bottom, form.getSize().height);
			if (left >= 0 && right >= 0)
			{
				width = form.getWidth() - right - left;
			}
			if (top >= 0 && bottom >= 0)
			{
				height = form.getSize().height - top - bottom;
			}
			return new Dimension(width, height);
		}
		return persist.getSize();
	}

	public static boolean isSet(String value)
	{
		return !"-1".equals(value);
	}

	private static int percentageToPixels(String value, int size)
	{
		int pixels = 0;
		if (value.endsWith("%"))
		{
			pixels = Utils.getAsInteger(value.substring(0, value.length() - 1)) * size / 100;
		}
		else
		{
			if (value.endsWith("px"))
			{
				value = value.substring(0, value.length() - 2);
			}
			pixels = Utils.getAsInteger(value);
		}
		return pixels;
	}

	private static String pixelsToPercentage(int value, int size, String oldValue)
	{
		if (oldValue.endsWith("%"))
		{
			return String.valueOf(100 * value / size) + "%";
		}
		return String.valueOf(value);
	}
}
