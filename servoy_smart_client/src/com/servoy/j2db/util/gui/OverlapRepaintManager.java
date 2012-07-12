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

import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.RenderEventExecutor;

/**
 * This repaint manager makes sure that overlapping components inside a container are repainted properly.<BR>
 * 
 * More precisely, it makes sure that, if you have component A on top of component B, and B is repainted then
 * A will be repainted afterwards in order not to be hidden by B's repaint.
 * @author acostescu
 */
public class OverlapRepaintManager extends RepaintManager
{
	private final Set<JComponent> components = new HashSet<JComponent>();

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
		// must see if somewhere in the component hierarchy, on some level that uses AnchorLayout,
		// this area paints over an overlapped component that should be on top of the current repainting one...
		int dX = x, dY = y, dW = w, dH = h;
		if (SwingUtilities.isEventDispatchThread())
		{
			// if its marked as dirty because of a change in fireOnRender that was run from paint
			// ignore this as the changes are already painted - if not ignored, we will have
			// a cycle calling of repaint -> paintComponent -> fireOnRender -> repaint 
			ISupportOnRenderCallback componentOnRenderParent = getOnRenderParent(c);
			if (componentOnRenderParent != null)
			{
				RenderEventExecutor ree = componentOnRenderParent.getRenderEventExecutor();
				if (ree != null && ree.hasRenderCallback())
				{
					if (ree.isOnRenderRunningOnComponentPaint()) return;
					else
					{
						// make the entire component dirty so changes made in onRender are applied
						dX = 0;
						dY = 0;
						dW = c.getWidth();
						dH = c.getHeight();
					}
				}
			}
		}
		synchronized (this)
		{
			components.add(c);
		}
		super.addDirtyRegion(c, dX, dY, dW, dH); // add the dirty region
	}

	private ISupportOnRenderCallback getOnRenderParent(Component c)
	{
		if (c == null) return null;
		if (c instanceof ISupportOnRenderCallback) return (ISupportOnRenderCallback)c;
		else return getOnRenderParent(c.getParent());
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