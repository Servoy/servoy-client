/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util.gui;



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * BorderLayout that lays out North and South between East and West (normal BorderLayout puts North and South surrounding East and West).
 */
public class VerticalBorderLayout extends BorderLayout
{

	/**
	 * Get the component that corresponds to the given constraint location
	 * 
	 * @param key The desired absolute position, either NORTH, SOUTH, EAST, or WEST.
	 * @param ltr Is the component line direction left-to-right?
	 */
	private Component getChild(String key, boolean ltr)
	{
		Component result = null;

		if (key == NORTH)
		{
			result = (getLayoutComponent(PAGE_START) != null) ? getLayoutComponent(PAGE_START) : getLayoutComponent(NORTH);
		}
		else if (key == SOUTH)
		{
			result = (getLayoutComponent(PAGE_END) != null) ? getLayoutComponent(PAGE_END) : getLayoutComponent(SOUTH);
		}
		else if (key == WEST)
		{
			result = ltr ? getLayoutComponent(LINE_START) : getLayoutComponent(LINE_END);
			if (result == null)
			{
				result = getLayoutComponent(WEST);
			}
		}
		else if (key == EAST)
		{
			result = ltr ? getLayoutComponent(LINE_END) : getLayoutComponent(LINE_START);
			if (result == null)
			{
				result = getLayoutComponent(EAST);
			}
		}
		else if (key == CENTER)
		{
			result = getLayoutComponent(CENTER);
		}
		if (result != null && !result.isVisible())
		{
			result = null;
		}
		return result;
	}

	/**
	 * Determines the minimum size of the <code>target</code> container using this layout manager.
	 * <p>
	 * This method is called when a container calls its <code>getMinimumSize</code> method. Most applications do not call this method directly.
	 * 
	 * @param target the container in which to do the layout.
	 * @return the minimum dimensions needed to lay out the subcomponents of the specified container.
	 * @see java.awt.Container
	 * @see java.awt.BorderLayout#preferredLayoutSize
	 * @see java.awt.Container#getMinimumSize()
	 */
	@Override
	public Dimension minimumLayoutSize(Container target)
	{
		synchronized (target.getTreeLock())
		{
			Dimension dim = new Dimension(0, 0);

			boolean ltr = target.getComponentOrientation().isLeftToRight();
			Component c = null;

			if ((c = getChild(NORTH, ltr)) != null)
			{
				Dimension d = c.getMinimumSize();
				dim.height += d.height + getHgap();
				dim.width = Math.max(d.width, dim.width);
			}
			if ((c = getChild(SOUTH, ltr)) != null)
			{
				Dimension d = c.getMinimumSize();
				dim.height += d.height + getHgap();
				dim.width = Math.max(d.width, dim.width);
			}
			if ((c = getChild(CENTER, ltr)) != null)
			{
				Dimension d = c.getMinimumSize();
				dim.height += d.height;
				dim.width = Math.max(d.width, dim.width);
			}
			if ((c = getChild(EAST, ltr)) != null)
			{
				Dimension d = c.getMinimumSize();
				dim.height = Math.max(d.height, dim.height);
				dim.width += d.width + getVgap();
			}
			if ((c = getChild(WEST, ltr)) != null)
			{
				Dimension d = c.getMinimumSize();
				dim.height = Math.max(d.height, dim.height);
				dim.width += d.width + getVgap();
			}

			Insets insets = target.getInsets();
			dim.height += insets.left + insets.right;
			dim.width += insets.top + insets.bottom;

			return dim;
		}
	}

	/**
	 * Determines the preferred size of the <code>target</code> container using this layout manager, based on the components in the container.
	 * <p>
	 * Most applications do not call this method directly. This method is called when a container calls its <code>getPreferredSize</code> method.
	 * 
	 * @param target the container in which to do the layout.
	 * @return the preferred dimensions to lay out the subcomponents of the specified container.
	 * @see java.awt.Container
	 * @see java.awt.BorderLayout#minimumLayoutSize
	 * @see java.awt.Container#getPreferredSize()
	 */
	@Override
	public Dimension preferredLayoutSize(Container target)
	{
		synchronized (target.getTreeLock())
		{
			Dimension dim = new Dimension(0, 0);

			boolean ltr = target.getComponentOrientation().isLeftToRight();
			Component c = null;

			if ((c = getChild(NORTH, ltr)) != null)
			{
				Dimension d = c.getPreferredSize();
				dim.height += d.height + getHgap();
				dim.width = Math.max(d.width, dim.width);
			}
			if ((c = getChild(SOUTH, ltr)) != null)
			{
				Dimension d = c.getPreferredSize();
				dim.height += d.height + getHgap();
				dim.width = Math.max(d.width, dim.width);
			}
			if ((c = getChild(CENTER, ltr)) != null)
			{
				Dimension d = c.getPreferredSize();
				dim.height += d.height;
				dim.width = Math.max(d.width, dim.width);
			}
			if ((c = getChild(EAST, ltr)) != null)
			{
				Dimension d = c.getPreferredSize();
				dim.height = Math.max(d.height, dim.height);
				dim.width += d.width + getVgap();
			}
			if ((c = getChild(WEST, ltr)) != null)
			{
				Dimension d = c.getPreferredSize();
				dim.height = Math.max(d.height, dim.height);
				dim.width += d.width + getVgap();
			}

			Insets insets = target.getInsets();
			dim.height += insets.left + insets.right;
			dim.width += insets.top + insets.bottom;

			return dim;
		}
	}

	/**
	 * Returns the maximum dimensions for this layout given the components in the specified target container.
	 * 
	 * @param target the component which needs to be laid out
	 * @see Container
	 * @see #minimumLayoutSize
	 * @see #preferredLayoutSize
	 */
	@Override
	public Dimension maximumLayoutSize(Container target)
	{
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Lays out the container argument using this border layout.
	 * <p>
	 * This method actually reshapes the components in the specified container in order to satisfy the constraints of this <code>BorderLayout</code> object.
	 * The <code>NORTH</code> and <code>SOUTH</code> components, if any, are placed at the top and bottom of the container, respectively. The
	 * <code>WEST</code> and <code>EAST</code> components are then placed on the left and right, respectively. Finally, the <code>CENTER</code> object is
	 * placed in any remaining space in the middle.
	 * <p>
	 * Most applications do not call this method directly. This method is called when a container calls its <code>doLayout</code> method.
	 * 
	 * @param target the container in which to do the layout.
	 * @see java.awt.Container
	 * @see java.awt.Container#doLayout()
	 */
	@Override
	public void layoutContainer(Container target)
	{
		synchronized (target.getTreeLock())
		{
			Insets insets = target.getInsets();
			int top = insets.top;
			int bottom = target.getHeight() - insets.bottom;
			int left = insets.left;
			int right = target.getWidth() - insets.right;

			boolean ltr = target.getComponentOrientation().isLeftToRight();
			Component c = null;

			if ((c = getChild(EAST, ltr)) != null)
			{
				c.setSize(c.getWidth(), bottom - top);
				Dimension d = c.getPreferredSize();
				c.setBounds(right - d.width, top, d.width, bottom - top);
				right -= d.width + getHgap();
			}
			if ((c = getChild(WEST, ltr)) != null)
			{
				c.setSize(c.getWidth(), bottom - top);
				Dimension d = c.getPreferredSize();
				c.setBounds(left, top, d.width, bottom - top);
				left += d.width + getHgap();
			}
			if ((c = getChild(NORTH, ltr)) != null)
			{
				c.setSize(right - left, c.getHeight());
				Dimension d = c.getPreferredSize();
				c.setBounds(left, top, right - left, d.height);
				top += d.height + getVgap();
			}
			if ((c = getChild(SOUTH, ltr)) != null)
			{
				c.setSize(right - left, c.getHeight());
				Dimension d = c.getPreferredSize();
				c.setBounds(left, bottom - d.height, right - left, d.height);
				bottom -= d.height + getVgap();
			}
			if ((c = getChild(CENTER, ltr)) != null)
			{
				c.setBounds(left, top, right - left, bottom - top);
			}
		}
	}

}