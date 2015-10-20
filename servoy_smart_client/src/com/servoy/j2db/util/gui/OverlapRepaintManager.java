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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import com.servoy.j2db.util.WeakHashSet;

/**
 * This repaint manager makes sure that overlapping components inside a container are repainted properly.<BR>
 * 
 * More precisely, it makes sure that, if you have component A on top of component B, and B is repainted then
 * A will be repainted afterwards in order not to be hidden by B's repaint.
 * @author acostescu
 */
public class OverlapRepaintManager extends RepaintManager
{
	private final Set<JComponent> components = new WeakHashSet<JComponent>();

	/**
	 * Creates a new OverlapRepaintManager that uses a default RepaintManager instance to handle unaltered operations.
	 */
	public OverlapRepaintManager()
	{
		super();
	}

	@Override
	public void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h)
	{
		synchronized (this)
		{
			components.add(c);
		}
		super.addDirtyRegion(c, x, y, w, h); // add the dirty region
	}

	private void searchOverlappingRegionsInHierarchy(Container parent, JComponent c, Rectangle repaintedArea)
	{
		if (parent instanceof JComponent) // this means it is not null also
		{
			Rectangle parentRepaintedArea = SwingUtilities.convertRectangle(c, repaintedArea, parent);

			if (parent.getLayout() instanceof AnchorLayout) // can have overlapped components
			{
				Rectangle intersection;
				Component child[] = parent.getComponents();
				for (int i = 0; i < child.length; i++)
				{
					if (child[i] == c) break; // only components that have lower index then the repainted component
					if (!child[i].isVisible()) continue;
					// and that overlap with this component need to be repainted (as they should be shown on top)
					intersection = child[i].getBounds().intersection(parentRepaintedArea);
					if (!intersection.isEmpty())
					{
						super.addDirtyRegion((JComponent)parent, intersection.x, intersection.y, intersection.width, intersection.height);
					}
				}
			}

			// see if parents overlap on some level
			searchOverlappingRegionsInHierarchy(parent.getParent(), (JComponent)parent, parentRepaintedArea);
		}
	}

	@Override
	public void paintDirtyRegions()
	{
		Set<JComponent> tmp;
		synchronized (this)
		{ // swap for thread safety
			tmp = new HashSet<JComponent>(components);
			components.clear();
		}
		for (JComponent component : tmp)
		{
			Rectangle dirtyRegion = super.getDirtyRegion(component);
			searchOverlappingRegionsInHierarchy(component.getParent(), component, dirtyRegion);
		}
		super.paintDirtyRegions();
	}
}