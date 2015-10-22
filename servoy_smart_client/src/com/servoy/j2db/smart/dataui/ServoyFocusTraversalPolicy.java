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
package com.servoy.j2db.smart.dataui;

import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.util.List;

import com.servoy.j2db.util.IFocusCycleRoot;

/**
 * Focus traversal policy that takes into account the defined tab sequence.
 */
public class ServoyFocusTraversalPolicy extends DefaultFocusTraversalPolicy
{
	public static final DefaultFocusTraversalPolicy defaultPolicy = new DefaultFocusTraversalPolicy();
	public static final ServoyFocusTraversalPolicy datarenderPolicy = new ServoyFocusTraversalPolicy();

	@Override
	public Component getComponentAfter(Container focusCycleRoot, Component aComponent)
	{
		Component next = getNeighborComponent(focusCycleRoot, aComponent, false);
		if (next != null) return next;
		else return super.getComponentAfter(focusCycleRoot, aComponent);
	}

	@Override
	public Component getComponentBefore(Container focusCycleRoot, Component aComponent)
	{
		Component prev = getNeighborComponent(focusCycleRoot, aComponent, true);
		if (prev != null) return prev;
		else return super.getComponentBefore(focusCycleRoot, aComponent);
	}

	private Component getNeighborComponent(Container focusCycleRoot, Component aComponent, boolean moveBackwards)
	{
		if (focusCycleRoot instanceof IFocusCycleRoot)
		{
			if (!((IFocusCycleRoot)focusCycleRoot).isTraversalPolicyEnabled()) return null;

			List<Component> componentSet = ((IFocusCycleRoot<Component>)focusCycleRoot).getTabSeqComponents();
			if (componentSet != null)
			{
				Object[] componentList = componentSet.toArray();

				// editable combobox; the editor (aComponent) is a text field that is not in the component list
				if (aComponent.getParent() instanceof DataComboBox) aComponent = aComponent.getParent();

				Component neighbor = null;
				int index = -1;
				for (int i = 0; i < componentList.length; i++)
				{
					Component entry = (Component)componentList[i];
					if (entry.equals(aComponent))
					{
						index = i;
						break;
					}
				}
				int startIndex = index;
				while (index != -1)
				{
					if (moveBackwards) index--;
					else index++;
					if (index < 0) index = componentList.length - 1;
					if (index >= componentList.length) index = 0;
					Component comp = ((Component)componentList[index]);
					if (comp.isEnabled() && comp.isVisible())
					{
						neighbor = comp;
						break;
					}
					if (startIndex == index) break;
				}
				if ((neighbor != null) && (neighbor instanceof IFocusCycleRoot) && getImplicitDownCycleTraversal())
				{
					neighbor = traverseDownAsMuchAsPossible(neighbor, moveBackwards);
				}
				return neighbor;
			}
		}
		return null;
	}

	@Override
	public Component getDefaultComponent(Container focusCycleRoot)
	{
		if (focusCycleRoot instanceof IFocusCycleRoot)
		{
			if (!((IFocusCycleRoot)focusCycleRoot).isTraversalPolicyEnabled()) return null;

			return traverseDownAsMuchAsPossible(focusCycleRoot, false);
		}
		return super.getDefaultComponent(focusCycleRoot);
	}

	@Override
	public Component getFirstComponent(Container focusCycleRoot)
	{
		if (focusCycleRoot instanceof IFocusCycleRoot)
		{
			if (!((IFocusCycleRoot)focusCycleRoot).isTraversalPolicyEnabled()) return null;

			return traverseDownAsMuchAsPossible(focusCycleRoot, false);
		}
		return super.getFirstComponent(focusCycleRoot);
	}

	@Override
	public Component getLastComponent(Container focusCycleRoot)
	{
		if (focusCycleRoot instanceof IFocusCycleRoot)
		{
			if (!((IFocusCycleRoot)focusCycleRoot).isTraversalPolicyEnabled()) return null;

			return traverseDownAsMuchAsPossible(focusCycleRoot, true);
		}
		return super.getLastComponent(focusCycleRoot);
	}

	private Component traverseDownAsMuchAsPossible(Component c, boolean moveBackwards)
	{
		if (c instanceof IFocusCycleRoot)
		{
			IFocusCycleRoot root = (IFocusCycleRoot)c;
			if (root.isTraversalPolicyEnabled())
			{
				Component firstChild;
				if (moveBackwards) firstChild = (Component)root.getLastFocusableField();
				else firstChild = (Component)root.getFirstFocusableField();
				if (firstChild != null)
				{
					return traverseDownAsMuchAsPossible(firstChild, moveBackwards);
				}
			}
		}
		else
		{
			if (c != null && (!c.isEnabled() || !c.isVisible()))
			{
				return getNeighborComponent(c.getFocusCycleRootAncestor(), c, moveBackwards);
			}
		}
		return c;
	}
}
