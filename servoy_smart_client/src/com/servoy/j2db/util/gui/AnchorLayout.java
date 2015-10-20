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
import java.awt.LayoutManager2;
import java.awt.Point;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.servoy.j2db.persistence.IAnchorConstants;

/**
 * Handy Layout Manager keeps anchored sides of an component on the same distance to the border by resizing. (Used in combination with excact positioning,
 * speedsup the development of dialogs with at least 20%)
 * 
 * see main method as example
 * 
 * Author jblok
 */
public class AnchorLayout implements LayoutManager2, IAnchorConstants
{
	private Dimension initalSize;
	private final Hashtable components;
	private boolean enabled = true;

	public AnchorLayout(int width, int height)
	{
		this(new Dimension(width, height));
	}

	public AnchorLayout(Dimension d)
	{
		components = new Hashtable();
		setPreferredSize(d);
	}

	public AnchorLayout()
	{
		this(null);
	}

	public void addLayoutComponent(String s, Component c)
	{
		addLayoutComponent(c, new Integer(DEFAULT));
	}

	public void removeLayoutComponent(Component c)
	{
		components.remove(c);
	}

	public Dimension preferredLayoutSize(Container c)
	{
		return initalSize;
	}

	public Dimension minimumLayoutSize(Container c)
	{
//		return initalSize;
		return new Dimension();
	}

	private Dimension lastSize;

	public void layoutContainer(Container c)
	{
		if (enabled)
		{
			Dimension size = c.getSize();
			if (lastSize != null && lastSize.equals(size)) return;
			Insets insets = c.getInsets();
			lastSize = size;
			int top = 0;
			int bottom = 0;
			int left = 0;
			int right = 0;

			Enumeration e = components.keys();
			while (e.hasMoreElements())
			{
				Component comp = (Component)e.nextElement();

				Object[] constains = (Object[])components.get(comp);
				Point orgLocation = (Point)constains[0];
				Dimension orgSize = (Dimension)constains[1];

				int anchors = ((Integer)constains[2]).intValue();

				Point setLocation = (Point)constains[3];
				Dimension setSize = (Dimension)constains[4];

				if (!comp.getLocation().equals(setLocation))
				{
					Point pNow = comp.getLocation();
					orgLocation.x = orgLocation.x + (pNow.x - setLocation.x);
					orgLocation.y = orgLocation.y + (pNow.y - setLocation.y);
				}
				if (!comp.getSize().equals(setSize))
				{
					Dimension dimNow = comp.getSize();
					orgSize.width = orgSize.width + (dimNow.width - setSize.width);
					orgSize.height = orgSize.height + (dimNow.height - setSize.height);
				}

				top = orgLocation.y;
				left = orgLocation.x;
				right = initalSize.width - (orgLocation.x + orgSize.width);
				bottom = initalSize.height - (orgLocation.y + orgSize.height);

				//savety for bad placed elements(not entrily on the form) to make them resize within the view 
				if (right < 0) right = 0;
				if (bottom < 0) bottom = 0;

				if ((anchors & NORTH) != NORTH)
				{
					top = size.height - bottom - orgSize.height;
				}
				if ((anchors & WEST) != WEST)
				{
					left = size.width - right - orgSize.width;
				}
				if ((anchors & EAST) != EAST)
				{
					right = size.width - left - orgSize.width;
				}
				if ((anchors & SOUTH) != SOUTH)
				{
					bottom = size.height - top - orgSize.height;
				}

				//TODO: don't make the size smaller as the components min. size
				comp.setBounds(left + insets.left, top + insets.top, size.width - (right + left), size.height - (top + bottom));

				constains[3] = comp.getLocation();
				constains[4] = comp.getSize();

			}
		}
	}

	public void addLayoutComponent(Component c, Object o)
	{
		if (c != null)
		{
			Integer anchors;
			if (o != null && o instanceof Integer)
			{
				anchors = (Integer)o;
			}
			else
			{
				anchors = new Integer(DEFAULT);
			}
			if (anchors.intValue() == 0) anchors = new Integer(DEFAULT);
			Object[] constaints = { c.getLocation(), c.getSize(), anchors, c.getLocation(), c.getSize() };
			components.put(c, constaints);
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}

	public float getLayoutAlignmentX(Container c)
	{
		return 0;
	}

	public float getLayoutAlignmentY(Container c)
	{
		return 0;
	}

	public void invalidateLayout(Container c)
	{
	}

	public Dimension maximumLayoutSize(Container c)
	{
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	public void setPreferredSize(Dimension d)
	{
		if (d != null)
		{
			initalSize = d;
		}
		else
		{
			initalSize = new Dimension(640, 480);
		}
	}

	public void setEnabled(boolean b)
	{
		enabled = b;
	}

	public boolean getEnabled()
	{
		return enabled;
	}

	public static void main(String[] args)
	{
		JFrame f = new JFrame();
		f.getContentPane().setLayout(new BorderLayout());
		f.setSize(800, 600);
		f.pack();
		f.setVisible(true);

		JPanel p = new JPanel();
		p.setLayout(new AnchorLayout(300, 200));
		JButton b = new JButton();
		b.setLocation(10, 10);
		b.setSize(40, 40);
		p.add(b, new Integer(ALL));

		f.getContentPane().add(p, BorderLayout.CENTER);
	}

	/**
	 * 
	 */
	public void reset()
	{
		Enumeration e = components.keys();
		while (e.hasMoreElements())
		{
			Component comp = (Component)e.nextElement();

			Object[] constains = (Object[])components.get(comp);

			constains[3] = comp.getLocation();
			constains[4] = comp.getSize();
		}
	}
}
