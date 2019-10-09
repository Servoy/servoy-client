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

package com.servoy.j2db.persistence;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;

import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.util.Utils;

/**
 * Utility class for all the CSS positions functions
 *
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public final class CSSPositionUtils
{

	public static void setLocation(ISupportBounds persist, int x, int y)
	{
		if (persist instanceof BaseComponent)
		{
			AbstractContainer container = getParentContainer((BaseComponent)persist);
			setLocationEx((BaseComponent)persist, (BaseComponent)persist, x, y, container.getSize());
		}
		else
		{
			persist.setLocation(new Point(x, y));
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
		if (persist instanceof BaseComponent)
		{
			AbstractContainer container = getParentContainer((BaseComponent)persist);
			setSizeEx((BaseComponent)persist, (BaseComponent)persist, width, height, container.getSize());
		}
		else
		{
			persist.setSize(new Dimension(width, height));
		}
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
			return getLocation(position, container.getSize());
		}
		return persist.getLocation();
	}

	public static Point getLocation(CSSPosition position, Dimension parentSize)
	{
		int top = percentageToPixels(position.top, parentSize.height);
		int left = percentageToPixels(position.left, parentSize.width);
		if (left == -1)
		{
			// not set, we should calculate it then
			int right = percentageToPixels(position.right, parentSize.width);
			int width = percentageToPixels(position.width, parentSize.width);
			if (width >= 0)
			{
				left = parentSize.width - right - width;
			}
		}
		if (top == -1)
		{
			// not set, we should calculate it then
			int height = percentageToPixels(position.height, parentSize.height);
			int bottom = percentageToPixels(position.bottom, parentSize.height);
			if (height >= 0)
			{
				top = parentSize.height - height - bottom;
			}
		}
		return new Point(left, top);
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
			return getSize(position, container.getSize());
		}
		return persist.getSize();
	}

	public static Dimension getSize(CSSPosition position, Dimension parentSize)
	{
		int width = percentageToPixels(position.width, parentSize.width);
		int height = percentageToPixels(position.height, parentSize.height);
		int left = percentageToPixels(position.left, parentSize.width);
		int right = percentageToPixels(position.right, parentSize.width);
		int top = percentageToPixels(position.top, parentSize.height);
		int bottom = percentageToPixels(position.bottom, parentSize.height);
		if (left >= 0 && right >= 0)
		{
			width = parentSize.width - right - left;
		}
		if (top >= 0 && bottom >= 0)
		{
			height = parentSize.height - top - bottom;
		}
		return new Dimension(width, height);
	}

	public static CSSPosition adjustCSSPosition(BaseComponent baseComponent, int x, int y, int width, int height)
	{
		CSSPosition position = baseComponent.getCssPosition();
		CSSPosition adjustedPosition = (position == null) ? new CSSPosition("0", "-1", "-1", "0", "0", "0")
			: new CSSPosition(position.top, position.right, position.bottom, position.left, position.width, position.height);
		AbstractContainer container = getParentContainer(baseComponent);
		Dimension containerSize = container.getSize();
		if (CSSPositionUtils.isSet(position.left) && !CSSPositionUtils.isSet(position.right))
		{
			adjustedPosition.left = pixelsToPercentage(x, containerSize.width, position.left);
			adjustedPosition.width = pixelsToPercentage(width, containerSize.width, position.width);
		}
		else if (!CSSPositionUtils.isSet(position.left) && CSSPositionUtils.isSet(position.right))
		{
			adjustedPosition.right = pixelsToPercentage(containerSize.width - x - width, containerSize.width, position.right);
			adjustedPosition.width = pixelsToPercentage(width, containerSize.width, position.width);
		}
		else
		{
			adjustedPosition.right = pixelsToPercentage(
				percentageToPixels(position.right, containerSize.width) + percentageToPixels(position.left, containerSize.width) - x, containerSize.width,
				position.right);
			adjustedPosition.left = pixelsToPercentage(x, containerSize.width, position.left);
		}

		if (CSSPositionUtils.isSet(position.top) && !CSSPositionUtils.isSet(position.bottom))
		{
			adjustedPosition.top = pixelsToPercentage(y, containerSize.height, position.top);
			adjustedPosition.height = pixelsToPercentage(height, containerSize.height, position.height);
		}
		else if (!CSSPositionUtils.isSet(position.top) && CSSPositionUtils.isSet(position.bottom))
		{
			adjustedPosition.bottom = pixelsToPercentage(containerSize.height - y - height, containerSize.height, position.bottom);
			adjustedPosition.height = pixelsToPercentage(height, containerSize.height, position.height);
		}
		else
		{
			adjustedPosition.bottom = pixelsToPercentage(
				percentageToPixels(position.bottom, containerSize.height) + percentageToPixels(position.top, containerSize.height) - y, containerSize.height,
				position.bottom);
			adjustedPosition.top = pixelsToPercentage(y, containerSize.height, position.top);
		}
		return adjustedPosition;
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
				String[] values = value.substring(value.indexOf("(") + 1, value.lastIndexOf(")")).trim().split(" ");
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
			if (value.endsWith("px")) pixels = Utils.getAsInteger(value.substring(0, value.length() - 2));
			else pixels = Utils.getAsInteger(value);
		}
		return pixels;
	}

	public static int getPixelsValue(String value)
	{
		int valueInteger = -1;
		if (value != null)
		{
			if (value.endsWith("px")) valueInteger = Utils.getAsInteger(value.substring(0, value.length() - 2));
			else valueInteger = Utils.getAsInteger(value, -1);
		}
		return valueInteger;
	}

	public static Point getLocationFromPixels(List<IPersist> persists)
	{
		int x = -1;
		int y = -1;
		if (persists != null)
		{
			for (IPersist persist : persists)
			{
				if (persist instanceof BaseComponent)
				{
					CSSPosition cssPosition = ((BaseComponent)persist).getCssPosition();
					int position = getPixelsValue(cssPosition.left);
					if (position >= 0 && (x == -1 || position < x))
					{
						x = position;
					}
					position = getPixelsValue(cssPosition.top);
					if (position >= 0 && (y == -1 || position < y))
					{
						y = position;
					}
				}
			}
		}
		if (x < 0) x = 0;
		if (y < 0) y = 0;
		return new Point(x, y);
	}

	private static String pixelsToPercentage(int value, int size, String oldValue)
	{
		if (oldValue.startsWith("calc"))
		{
			// must recalculate the expression
			if (oldValue.indexOf("(") > 0 && oldValue.indexOf(")") > 0)
			{
				String[] values = oldValue.substring(oldValue.indexOf("(") + 1, oldValue.lastIndexOf(")")).trim().split(" ");
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

	public static boolean useCSSPosition(Object persist)
	{
		if (persist instanceof BaseComponent && ((BaseComponent)persist).getParent() instanceof Form &&
			((Form)((BaseComponent)persist).getParent()).getUseCssPosition().booleanValue())
		{
			return true;
		}
		if (persist instanceof BaseComponent && CSSPositionUtils.isInAbsoluteLayoutMode((IPersist)persist))
		{
			return true;
		}
		return false;
	}

	public static AbstractContainer getParentContainer(BaseComponent component)
	{
		IPersist currentComponent = component;
		while (currentComponent != null)
		{
			if (currentComponent.getParent() instanceof AbstractContainer) return (AbstractContainer)currentComponent.getParent();
			currentComponent = currentComponent.getParent();
		}
		return null;
	}

	public static void convertToCSSPosition(Form form)
	{
		if (form != null && !form.isResponsiveLayout() &&
			(form.getView() == IFormConstants.VIEW_TYPE_RECORD || form.getView() == IFormConstants.VIEW_TYPE_RECORD_LOCKED))
		{
			form.setUseCssPosition(Boolean.TRUE);
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

	public static boolean isInAbsoluteLayoutMode(IPersist persist)
	{
		IPersist parent = persist.getParent();
		while (parent != null)
		{
			if (parent instanceof LayoutContainer) return isCSSPositionContainer((LayoutContainer)parent);
			if (parent instanceof Form) break;
			parent = parent.getParent();
		}
		return false;
	}

	public static boolean isCSSPositionContainer(WebLayoutSpecification spec)
	{
		return spec != null && spec.isCSSPosition();
	}

	public static boolean isCSSPositionContainer(LayoutContainer container)
	{
		if (container != null)
		{
			WebComponentSpecProvider.getInstance();
			PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				container.getPackageName());
			if (pkg != null)
			{
				return CSSPositionUtils.isCSSPositionContainer(pkg.getSpecification(container.getSpecName()));
			}
		}
		return false;
	}

}
