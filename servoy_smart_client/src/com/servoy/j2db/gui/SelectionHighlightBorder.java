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
package com.servoy.j2db.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.geom.Line2D;

import javax.swing.border.AbstractBorder;

/**
 * Lightweight version of SpecialMatteBorder, used for drawing 1px dotted border
 * around selected items in lists or tables.
 */
public class SelectionHighlightBorder extends AbstractBorder
{

	private static final long serialVersionUID = -953369700524433734L;

	private final Color color;
	private final int thickness = 1;

	public SelectionHighlightBorder()
	{
		this(Color.BLACK);
	}

	public SelectionHighlightBorder(Color color)
	{
		this.color = color;
	}

	@Override
	public Insets getBorderInsets(Component c)
	{
		return new Insets(thickness, thickness, thickness, thickness);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.top = insets.right = insets.bottom = thickness;
		return insets;
	}

	@Override
	public boolean isBorderOpaque()
	{
		return true;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		Stroke oldStroke = ((Graphics2D)g).getStroke();
		g.translate(x, y);

		g.setColor(color);
		((Graphics2D)g).setStroke(new BasicStroke(thickness, 0, 0, 1f, new float[] { 1, 1 }, 0f));

		((Graphics2D)g).draw(new Line2D.Float(0f, 0f, width - 1, 0f));
		((Graphics2D)g).draw(new Line2D.Float(width - 1, 0f, width - 1, height - 1));
		((Graphics2D)g).draw(new Line2D.Float(0f, height - 1, width - 1, height - 1));
		((Graphics2D)g).draw(new Line2D.Float(0f, 0f, 0f, height - 1));

		g.translate(-x, -y);
		g.setColor(oldColor);
		((Graphics2D)g).setStroke(oldStroke);
	}

}
