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
package com.servoy.j2db.util;

import java.awt.Container;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Helper class for printing in (non) GUI env
 * @author jblok
 */
public class RendererParentWrapper
{
	private final Container swingRenderParent;
	private final Set addedComps = new HashSet();

	public RendererParentWrapper(Container a_parent)
	{
		swingRenderParent = a_parent;
	}

	public RendererParentWrapper()
	{
		swingRenderParent = new JLabel();
		swingRenderParent.addNotify();
		swingRenderParent.setVisible(true);
		swingRenderParent.setSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		swingRenderParent.doLayout();
	}

	public void add(JComponent comp)
	{
		if (!addedComps.contains(comp))//add only when not added yet
		{
			addedComps.add(comp);
			swingRenderParent.add(comp);
		}
	}

	public void remove(JComponent comp)
	{
		//nop
	}

	public void removeAll()
	{
		swingRenderParent.removeAll();
		addedComps.clear();
	}

	public void validate()
	{
		swingRenderParent.validate();
	}

	public void invalidate()
	{
		swingRenderParent.invalidate();
	}

	public void destroy()
	{
		removeAll();
	}

	public Container getParent()
	{
		return swingRenderParent;
	}
}
