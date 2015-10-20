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
package com.servoy.j2db.util.wizard;


import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * <class description>
 * 
 * @author jblok
 */
public class HighlightJButton extends JButton
{
	//.....................................................................

	// The constructors
	public HighlightJButton(Icon icon, Color color)
	{
		super(icon);
		initButton(color);
	}

	public HighlightJButton(String text, Color color)
	{
		super(text);
		initButton(color);
	}

	public HighlightJButton(String text, Icon icon, Color color)
	{
		super(text, icon);
		initButton(color);
	}

	public HighlightJButton(Action a, Color color)
	{
		super(a);
		initButton(color);
	}

	//.....................................................................
	// The methods 

	// Does the extra initialisations
	protected void initButton(Color highlightColor)
	{
		this.highlightColor = highlightColor;
		defaultColor = getBackground();

		addMouseListener(new MouseHandler());
	}

	// Overriden to ensure that the button won't stay highlighted if it had the mouse over
	@Override
	public void setEnabled(boolean b)
	{
		reset();
		super.setEnabled(b);
	}

	// Forces the button to unhighlight
	public void reset()
	{
		setBackground(defaultColor);
	}

	//.....................................................................
	// The fields

	private Color highlightColor; // The highlighted color
	private Color defaultColor; // The default color

	//.....................................................................

	// The mouse handler which makes the highlighting
	private class MouseHandler extends MouseAdapter
	{
		// The mouse passes over the button
		@Override
		public void mouseEntered(MouseEvent e)
		{
			if (isEnabled()) setHighlight(true);
		}

		// The mouse passes out of the button
		@Override
		public void mouseExited(MouseEvent e)
		{
			if (isEnabled()) setHighlight(false);
		}
	}

	public void setHighlight(boolean b)
	{
		if (isEnabled())
		{
			if (b)
			{
				setBackground(highlightColor);
			}
			else
			{
				setBackground(defaultColor);
			}
		}
	}
}
