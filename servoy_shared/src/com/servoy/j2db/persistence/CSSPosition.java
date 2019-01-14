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
import java.io.Serializable;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class CSSPosition implements Serializable
{
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

	public static void setLocation(ISupportBounds persist, int x, int y)
	{
		if (persist instanceof BaseComponent)
		{
			AbstractContainer container = getParentContainer((BaseComponent)persist);
			setLocationEx((BaseComponent)persist, (BaseComponent)persist, x, y, container.getSize());
		}
	}

	public static void setLocationEx(BaseComponent baseComponent, AbstractBase persist, int x, int y, Dimension containerSize)
	{
		if (useCSSPosition(baseComponent))
		{
			CSSPosition position;
			if (persist instanceof BaseComponent)
			{
				position = ((BaseComponent)persist).getCssPosition();
			}
			else
			{
				position = (CSSPosition)persist.getProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName());
			}
			if (position == null)
			{
				position = new CSSPosition("0", "-1", "-1", "0", "0", "0");
			}
			if (!isSet(position.right))
			{
				position.left = pixelsToPercentage(x, containerSize.width, position.left);
			}
			else if (!isSet(position.left))
			{
				position.right = pixelsToPercentage(containerSize.width - x - percentageToPixels(position.width, containerSize.width), containerSize.width,
					position.right);
			}
			else
			{
				position.right = pixelsToPercentage(
					percentageToPixels(position.right, containerSize.width) + percentageToPixels(position.left, containerSize.width) - x, containerSize.width,
					position.right);
				position.left = pixelsToPercentage(x, containerSize.width, position.left);
			}
			if (!isSet(position.bottom))
			{
				position.top = pixelsToPercentage(y, containerSize.height, position.top);
			}
			else if (!isSet(position.top))
			{
				position.bottom = pixelsToPercentage(containerSize.height - y - percentageToPixels(position.height, containerSize.height), containerSize.height,
					position.bottom);
			}
			else
			{
				position.bottom = pixelsToPercentage(
					percentageToPixels(position.bottom, containerSize.height) + percentageToPixels(position.top, containerSize.height) - y,
					containerSize.height, position.bottom);
				position.top = pixelsToPercentage(y, containerSize.height, position.top);
			}
			if (persist instanceof BaseComponent)
			{
				((BaseComponent)persist).setCssPosition(position);
			}
			else
			{
				persist.setProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName(), position);
			}
		}
		else
		{
			Point p = new Point(x, y);
			if (persist instanceof ISupportBounds)
			{
				((ISupportBounds)persist).setLocation(p);
			}
			else
			{
				persist.setProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), p);
			}
		}
	}

	public static void setSize(ISupportBounds persist, int width, int height)
	{
		AbstractContainer container = getParentContainer((BaseComponent)persist);
		setSizeEx((BaseComponent)persist, (BaseComponent)persist, width, height, container.getSize());
	}

	public static void setSizeEx(BaseComponent baseComponent, AbstractBase persist, int width, int height, Dimension containerSize)
	{
		if (useCSSPosition(baseComponent))
		{
			CSSPosition position;
			if (persist instanceof BaseComponent)
			{
				position = ((BaseComponent)persist).getCssPosition();
			}
			else
			{
				position = (CSSPosition)persist.getProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName());
			}
			if (position == null)
			{
				position = new CSSPosition("0", "-1", "-1", "0", "0", "0");
			}

			if (isSet(position.left) && isSet(position.right))
			{
				position.right = pixelsToPercentage(containerSize.width - percentageToPixels(position.left, containerSize.width) - width, containerSize.width,
					position.right);
			}
			else
			{
				position.width = pixelsToPercentage(width, containerSize.width, position.width);
			}
			if (isSet(position.top) && isSet(position.bottom))
			{
				position.bottom = pixelsToPercentage(containerSize.height - percentageToPixels(position.top, containerSize.height) - height,
					containerSize.height, position.bottom);
			}
			else
			{
				position.height = pixelsToPercentage(height, containerSize.height, position.height);
			}
			if (persist instanceof BaseComponent)
			{
				((BaseComponent)persist).setCssPosition(position);
			}
			else
			{
				persist.setProperty(StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName(), position);
			}
		}
		else
		{
			Dimension d = new Dimension(width, height);
			if (persist instanceof ISupportBounds)
			{
				((ISupportBounds)persist).setSize(d);
			}
			else
			{
				persist.setProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), d);
			}
		}
	}

	public static Point getLocation(ISupportBounds persist)
	{
		if (useCSSPosition(persist))
		{
			CSSPosition position = ((BaseComponent)persist).getCssPosition();
			if (position == null)
			{
				position = new CSSPosition("0", "-1", "-1", "0", "0", "0");
			}
			AbstractContainer container = getParentContainer((BaseComponent)persist);
			int top = percentageToPixels(position.top, container.getSize().height);
			int left = percentageToPixels(position.left, container.getSize().width);
			if (left == -1)
			{
				// not set, we should calculate it then
				int right = percentageToPixels(position.right, container.getSize().width);
				int width = percentageToPixels(position.width, container.getSize().width);
				if (right >= 0 && width >= 0)
				{
					left = container.getSize().width - right - width;
				}
			}
			if (top == -1)
			{
				// not set, we should calculate it then
				int height = percentageToPixels(position.height, container.getSize().height);
				int bottom = percentageToPixels(position.bottom, container.getSize().height);
				if (height >= 0 && bottom >= 0)
				{
					top = container.getSize().height - height - bottom;
				}
			}
			return new Point(left, top);
		}
		return persist.getLocation();
	}

	public static Dimension getSize(ISupportSize persist)
	{
		if (useCSSPosition(persist))
		{
			CSSPosition position = ((BaseComponent)persist).getCssPosition();
			if (position == null)
			{
				position = new CSSPosition("0", "-1", "-1", "0", "0", "0");
			}
			AbstractContainer container = getParentContainer((BaseComponent)persist);
			int width = percentageToPixels(position.width, container.getSize().width);
			int height = percentageToPixels(position.height, container.getSize().height);
			int left = percentageToPixels(position.left, container.getSize().width);
			int right = percentageToPixels(position.right, container.getSize().width);
			int top = percentageToPixels(position.top, container.getSize().height);
			int bottom = percentageToPixels(position.bottom, container.getSize().height);
			if (left >= 0 && right >= 0)
			{
				width = container.getSize().width - right - left;
			}
			if (top >= 0 && bottom >= 0)
			{
				height = container.getSize().height - top - bottom;
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
		if (value.startsWith("calc"))
		{
			if (value.indexOf("(") > 0 && value.indexOf(")") > 0)
			{
				value = value.substring(value.indexOf("(") + 1, value.lastIndexOf(")"));
				value = value.trim();
				String[] values = value.split(" ");
				if (values.length == 3)
				{
					int value1 = valueToPizels(values[0], size);
					int value2 = valueToPizels(values[2], size);
					// only simple calc, x + y or x - y
					boolean subtract = "-".equals(values[1]);
					pixels = subtract ? (value1 - value2) : (value1 + value2);
				}
			}
		}
		else
		{
			pixels = valueToPizels(value, size);
		}
		return pixels;
	}

	private static int valueToPizels(String value, int size)
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
		if (oldValue.startsWith("calc"))
		{
			// must recalculate the expression
			if (oldValue.indexOf("(") > 0 && oldValue.indexOf(")") > 0)
			{
				oldValue = oldValue.substring(oldValue.indexOf("(") + 1, oldValue.lastIndexOf(")"));
				oldValue = oldValue.trim();
				String[] values = oldValue.split(" ");
				if (values.length == 3)
				{
					// only simple calc, x + y or x - y
					boolean substract = "-".equals(values[1]);
					int value1 = valueToPizels(values[0], size);
					int newvalue = 0;
					if (substract)
					{
						newvalue = value1 - value;
					}
					else
					{
						newvalue = value - value1;
					}
					if (newvalue < 0)
					{
						newvalue = -newvalue;
						substract = !substract;
					}
					String adjustedValue = newvalue + "px";
					if (values[2].endsWith("%"))
					{
						adjustedValue = (newvalue * size / 100) + "%";
					}
					return "calc(" + values[0] + " " + (substract ? "-" : "+") + " " + adjustedValue + ")";
				}
			}
			return oldValue;
		}
		else if (oldValue.endsWith("%"))
		{
			return String.valueOf(100 * value / size) + "%";
		}
		return String.valueOf(value);
	}

	private static boolean useCSSPosition(Object persist)
	{
		if (persist instanceof BaseComponent && ((BaseComponent)persist).getParent() instanceof Form &&
			((Form)((BaseComponent)persist).getParent()).getUseCssPosition())
		{
			return true;
		}
		if (persist instanceof BaseComponent && PersistHelper.isInAbsoluteLayoutMode((IPersist)persist))
		{
			return true;
		}
		return false;
	}

	private static AbstractContainer getParentContainer(BaseComponent component)
	{
		IPersist currentComponent = component;
		while (currentComponent != null)
		{
			if (currentComponent.getParent() instanceof AbstractContainer) return (AbstractContainer)currentComponent.getParent();
			currentComponent = component.getParent();
		}
		return null;
	}

	public static void convertToCSSPosition(Form form)
	{
		if (form != null && !form.isResponsiveLayout() &&
			(form.getView() == IFormConstants.VIEW_TYPE_RECORD || form.getView() == IFormConstants.VIEW_TYPE_RECORD_LOCKED))
		{
			form.setUseCssPosition(true);
			form.acceptVisitor(new IPersistVisitor()
			{
				@Override
				public Object visit(IPersist o)
				{
					if (useCSSPosition(o))
					{
						BaseComponent element = (BaseComponent)o;
						int anchors = element.getAnchors();
						CSSPosition startPosition = new CSSPosition(((anchors & IAnchorConstants.NORTH) != 0) ? "0" : "-1",
							((anchors & IAnchorConstants.EAST) != 0) ? "0" : "-1", ((anchors & IAnchorConstants.SOUTH) != 0) ? "0" : "-1",
							((anchors & IAnchorConstants.WEST) != 0) ? "0" : "-1", String.valueOf(element.getSize().width),
							String.valueOf(element.getSize().height));
						element.setCssPosition(startPosition);
						setLocation(element, element.getLocation().x, element.getLocation().y);
						setSize(element, element.getSize().width, element.getSize().height);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
		}
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
