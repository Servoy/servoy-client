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
package com.servoy.j2db.util.toolbar;


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;


/**
 * ToolbarLayout is a LayoutManager2 that should be used on a toolbar panel to allow placement of components in absolute positions.
 * 
 * This is the place where components are setted it's bounds by actual toolbar configuration.
 * 
 * @author Libor Kramolis
 */
public class ToolbarLayout implements LayoutManager2, java.io.Serializable
{
	/** Toolbar horizontal gap. */
	public static final int HGAP = 1;
	/** Toolbar vertical gap. */
	public static final int VGAP = 1;

	static final long serialVersionUID = 7489472539255790677L;

	/** ToolbarConfiguration cached for getting preferred toolbar configuration width. */
	ToolbarPanel toolbarConfig;
	/** Map of components. */
	HashMap componentMap;

	/**
	 * Creates a new ToolbarLayout.
	 */
	public ToolbarLayout(ToolbarPanel conf)
	{
		toolbarConfig = conf;
		componentMap = new HashMap();
	}

	/**
	 * Adds the specified component with the specified name to the layout. Everytime throws IllegalArgumentException.
	 * 
	 * @param name the component name
	 * @param comp the component to be added
	 */
	public void addLayoutComponent(String name, Component comp)
	{
		throw new IllegalArgumentException();
	}

	/**
	 * Adds the specified component to the layout, using the specified constraint object.
	 * 
	 * @param comp the component to be added
	 * @param constraints the where/how the component is added to the layout.
	 * @exception <code>ClassCastException</code> if the argument is not a <code>ToolbarConstraints</code>.
	 */
	public void addLayoutComponent(Component comp, Object constr)
	{
		if (!(constr instanceof ToolbarConstraints)) throw new IllegalArgumentException("EXC_wrongConstraints"); //$NON-NLS-1$

		componentMap.put(comp, constr);
		ToolbarConstraints tc = (ToolbarConstraints)constr;
		tc.setPreferredSize(comp.getPreferredSize());
		comp.setVisible(tc.isVisible());
	}

	/**
	 * Removes the specified component from this layout.
	 * 
	 * @param comp the component to be removed
	 */
	public void removeLayoutComponent(Component comp)
	{
		componentMap.remove(comp);
	}

	/**
	 * Calculates the preferred dimension for the specified panel given the components in the specified parent container.
	 * 
	 * @param parent the component to be laid out
	 * 
	 * @see #minimumLayoutSize
	 */
	public Dimension preferredLayoutSize(Container parent)
	{
		Insets insets = parent.getInsets();
		Dimension prefSize = new Dimension(insets.left + toolbarConfig.getPrefWidth() + insets.right, insets.top + toolbarConfig.getPrefHeight() +
			insets.bottom);
		return prefSize;
	}

	/**
	 * Calculates the minimum dimension for the specified panel given the components in the specified parent container.
	 * 
	 * @param parent the component to be laid out
	 * @see #preferredLayoutSize
	 */
	public Dimension minimumLayoutSize(Container parent)
	{
		return preferredLayoutSize(parent);
	}

	/**
	 * Returns the maximum size of this component.
	 * 
	 * @see java.awt.Component#getMinimumSize()
	 * @see java.awt.Component#getPreferredSize()
	 * @see LayoutManager
	 */
	public Dimension maximumLayoutSize(Container parent)
	{
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	/**
	 * Returns the alignment along the x axis. This specifies how the component would like to be aligned relative to other components. The value should be a
	 * number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned the furthest away from the origin, 0.5 is centered, etc.
	 */
	public float getLayoutAlignmentX(Container parent)
	{
		return 0;
	}

	/**
	 * Returns the alignment along the y axis. This specifies how the component would like to be aligned relative to other components. The value should be a
	 * number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned the furthest away from the origin, 0.5 is centered, etc.
	 */
	public float getLayoutAlignmentY(Container parent)
	{
		return 0;
	}

	/**
	 * Invalidates the layout, indicating that if the layout manager has cached information it should be discarded.
	 */
	public void invalidateLayout(Container parent)
	{
	}

	/**
	 * Lays out the container in the specified panel.
	 * 
	 * @param parent the component which needs to be laid out
	 */
	public void layoutContainer(Container parent)
	{
		synchronized (parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			boolean ltr = parent.getComponentOrientation().isLeftToRight();
			if (!ltr)
			{
				insets.right += insets.left;
				insets.left = insets.right - insets.left;
				insets.right -= insets.left;
			}
			int maxPosition = parent.getWidth() - (insets.left + insets.right) - HGAP;

			Iterator it;
			Component comp;
			ToolbarConstraints constr;
			Rectangle bounds;

			/*
			 * It is very important to update preferred sizes for each component because when any component change it's size it can affect to other components'
			 * size and position.
			 */
			it = componentMap.keySet().iterator();
			while (it.hasNext())
			{
				comp = (Component)it.next();
				constr = (ToolbarConstraints)componentMap.get(comp);
				constr.updatePreferredSize(comp.getPreferredSize());
			}

			/* Setting components' bounds. */
			it = componentMap.keySet().iterator();
			while (it.hasNext())
			{
				comp = (Component)it.next();
				constr = (ToolbarConstraints)componentMap.get(comp);

				/* ToolbarConstraints has component bounds prepared. */
				bounds = constr.getBounds();

				if ((bounds.x < maxPosition) && /* If component starts on visible position ... */
				(bounds.x + bounds.width > maxPosition))
				{ /* ... but with width it is over visible area ... */
					bounds.width = maxPosition - bounds.x; /* ... so width is cropped to max possible. */
				}
				else
				{
					if (bounds.x > maxPosition + HGAP)
					{ /* If component is over visible position ... */
						constr.setPosition(maxPosition + HGAP); /* ... new position is setted to min invisible. */
						constr.updatePosition();
					}
				}

				if (!ltr)
				{
					bounds.x = maxPosition - bounds.x - bounds.width;
				}
				comp.setBounds(bounds);
			}
		}
	}
}
