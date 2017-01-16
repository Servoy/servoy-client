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
package com.servoy.j2db.property;


import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import com.servoy.j2db.persistence.ISupportHTMLToolTipText;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.Pair;

/**
 * class is used in all popups
 *
 * @author jblok
 */
public class PropertyListCellRenderer extends DefaultListCellRenderer
{
	private Font boldFont;
	private Font normalFont;
	private final boolean showToolTips;

	public PropertyListCellRenderer(boolean showToolTips)
	{
		this.showToolTips = showToolTips;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		int align = LEFT;
		Font f = normalFont;
		String newvalue = ""; //$NON-NLS-1$

		if (value != null) newvalue = value.toString();

		if (value instanceof ScriptMethod)
		{
			newvalue = ((ScriptMethod)value).getDisplayName();
		}
		if (value instanceof Pair && ((Pair)value).getLeft() != null)
		{
			newvalue = ((Pair)value).getLeft().toString();
		}

		if (newvalue != null && newvalue.startsWith("*")) //$NON-NLS-1$
		{
			newvalue = newvalue.substring(1);
			f = boldFont;
			align = CENTER;
		}
		else
		{
			f = normalFont;
			align = LEFT;
		}

		Component c = super.getListCellRendererComponent(list, newvalue, index, isSelected, cellHasFocus);
		if (showToolTips)
		{
			if (value instanceof ISupportHTMLToolTipText)
			{
				String html = ((ISupportHTMLToolTipText)value).toHTML();
				if (!html.startsWith("<html>")) html = "<html>" + html + "</html>";
				((JComponent)c).setToolTipText(html);
			}
			else
			{
				((JComponent)c).setToolTipText(null);
			}
		}

		if (normalFont == null)
		{
			normalFont = c.getFont();
			boldFont = normalFont.deriveFont(Font.BOLD);
			f = normalFont;
		}

		c.setFont(f);
		if (c instanceof JLabel)
		{
			((JLabel)c).setHorizontalAlignment(align);
		}
		return c;
	}
}
