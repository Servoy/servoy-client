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


import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.border.BevelBorder;

/**
 * @author jblok
 * 
 */
public class TabLikeBorder extends BevelBorder
{
	public TabLikeBorder()
	{
		super(BevelBorder.RAISED);
	}

	@Override
	protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		int h = height;
		int w = width;

		if (h > 20) h = 20;

		g.translate(x, y);

		g.setColor(getHighlightOuterColor(c));
		g.drawLine(0, 0, 0, h - 2);
		g.drawLine(1, 0, w - 2, 0);

		g.setColor(getHighlightInnerColor(c));
		g.drawLine(1, 1, 1, h - 2);
		g.drawLine(2, 1, w - 3, 1);

		g.setColor(getShadowOuterColor(c));
//        g.drawLine(0, h-1, w-1, h-1);
		g.drawLine(w - 1, 0, w - 1, h - 2);

		g.setColor(getShadowInnerColor(c));
//        g.drawLine(1, h-2, w-2, h-2);
		g.drawLine(w - 2, 1, w - 2, h - 2);

		g.translate(-x, -y);
		g.setColor(oldColor);

	}
}
