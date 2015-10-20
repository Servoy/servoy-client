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
package com.servoy.j2db.gui.editlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;

import javax.swing.JList;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import com.servoy.j2db.gui.SelectionHighlightBorder;

public class NavigableCellRenderer implements ListCellRenderer
{
	private static Border selectionBorder;

	static
	{
		selectionBorder = UIManager.getBorder("List.focusCellHighlightBorder"); //$NON-NLS-1$
		if (selectionBorder == null)
		{
			Color lfColor = UIManager.getColor("Tree.selectionBorderColor"); //$NON-NLS-1$
			if (lfColor == null) lfColor = Color.BLACK;
			selectionBorder = new SelectionHighlightBorder(lfColor);
		}
	}

	private static final long serialVersionUID = 7555943552731677877L;

	private final ListCellRenderer innerRenderer;

	public NavigableCellRenderer(ListCellRenderer innerRenderer)
	{
		this.innerRenderer = innerRenderer;
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		Component innerComponent = innerRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (innerComponent instanceof JToggleButton)
		{
			JToggleButton btn = (JToggleButton)innerComponent;
			decorateButton(btn, isSelected && list.hasFocus());
		}
		return innerComponent;
	}

	public static void decorateButton(JToggleButton btn, boolean isSelected)
	{
		btn.setFocusPainted(false);
		btn.setBorderPainted(true);
		Border cloned = cloneBorder(btn.getBorder(), isSelected, btn);
		cloned = compactBorder(cloned, btn);
		btn.setBorder(cloned);
	}

	private static Border compactBorder(Border source, Component c)
	{
		if (source instanceof CompoundBorder)
		{
			CompoundBorder cb = (CompoundBorder)source;
			Border inside = compactBorder(cb.getInsideBorder(), c);
			Border outside = compactBorder(cb.getOutsideBorder(), c);
			if ((inside instanceof EmptyBorder) && (outside instanceof EmptyBorder))
			{
				Insets ins = inside.getBorderInsets(c);
				Insets insOutside = outside.getBorderInsets(c);
				ins.top += insOutside.top;
				ins.left += insOutside.left;
				ins.bottom += insOutside.bottom;
				ins.right += insOutside.right;
				return new EmptyBorder(ins);
			}
			else
			{
				return new CompoundBorder(outside, inside);
			}
		}
		else
		{
			return source;
		}
	}

	private static Border cloneBorder(Border source, boolean isSelected, Component c)
	{
		if (isSelected)
		{
			Insets originalBorderInsets = source.getBorderInsets(c);
			Insets selectionBorderInsets = selectionBorder.getBorderInsets(c);
			originalBorderInsets.top -= selectionBorderInsets.top;
			originalBorderInsets.left -= selectionBorderInsets.left;
			originalBorderInsets.bottom -= selectionBorderInsets.bottom;
			originalBorderInsets.right -= selectionBorderInsets.right;
			if (originalBorderInsets.top < 0) originalBorderInsets.top = 0;
			if (originalBorderInsets.left < 0) originalBorderInsets.left = 0;
			if (originalBorderInsets.bottom < 0) originalBorderInsets.bottom = 0;
			if (originalBorderInsets.right < 0) originalBorderInsets.right = 0;
			if (originalBorderInsets.top > 0 || originalBorderInsets.left > 0 || originalBorderInsets.right > 0 || originalBorderInsets.bottom > 0)
			{
				EmptyBorder theFiller = new EmptyBorder(originalBorderInsets);
				return new CompoundBorder(selectionBorder, theFiller);
			}
			else
			{
				return selectionBorder;
			}
		}
		else
		{
			Border selectedBorder = cloneBorder(source, true, c);
			// selected border may be bigger, so use it so content fits nicely and we don't get 'movement' effect
			return new EmptyBorder(selectedBorder.getBorderInsets(c));
		}
	}

}
