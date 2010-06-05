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

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

class OvertypeCaret extends DefaultCaret
{
	/*
	 * The overtype caret will simply be a horizontal line one pixel high (once we determine where to paint it)
	 */
	@Override
	public void paint(Graphics g)
	{
		if (isVisible())
		{
			try
			{
				JTextComponent component = getComponent();
				String text = component.getDocument().getText(getDot(), 1);
				if (text.trim().length() == 0 && !" ".equals(text))
				{
					super.paint(g);
					return;
				}
				Rectangle r = component.modelToView(getDot());
				g.setColor(component.getCaretColor());
				int width = g.getFontMetrics().charWidth(text.charAt(0));
				int y = r.y + r.height - 2;
				g.drawLine(r.x, y, r.x + width - 1, y);
			}
			catch (BadLocationException e)
			{
			}
		}
	}

	/*
	 * Damage must be overridden whenever the paint method is overridden (The damaged area is the area the caret is painted in. We must consider the area for
	 * the default caret and this caret)
	 */
	@Override
	protected synchronized void damage(Rectangle r)
	{
		if (r != null)
		{
			try
			{
				JTextComponent component = getComponent();
				String text = component.getDocument().getText(getDot(), 1);
				if (text.trim().length() == 0 && !" ".equals(text))
				{
					super.damage(r);
					return;
				}
				x = r.x;
				y = r.y;
				width = component.getFontMetrics(component.getFont()).charWidth('w');
				height = r.height;
				repaint();
			}
			catch (BadLocationException e)
			{
			}
		}
	}
}