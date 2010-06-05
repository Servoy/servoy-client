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
package com.servoy.j2db.server.headlessclient.dataui;

import javax.swing.Icon;

import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.util.Utils;

public class WebTabHolder
{
	private final WebTabFormLookup panel;
	private final Icon icon;
	private final String tooltip;
	private String text;
	private String tagText;
	private boolean enabled;

	WebTabHolder(String t, IFormLookupPanel panel, Icon icon, String tooltip)
	{
		this.icon = icon;
		this.panel = (WebTabFormLookup)panel;
		this.tooltip = tooltip;
		if (t != null && t.indexOf("%%") != -1) //$NON-NLS-1$
		{
			tagText = t;
		}
		this.text = Utils.stringReplace(TemplateGenerator.getSafeText(t), " ", "&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
		this.enabled = true;
	}

	void setEnabled(boolean b)
	{
		enabled = b;
		if (panel.isReady())
		{
			panel.getWebForm().setComponentEnabled(b);
		}
	}

	boolean isEnabled()
	{
		return enabled;
	}

	WebTabFormLookup getPanel()
	{
		return panel;
	}

	String getText()
	{
		return text;
	}

	void setText(String text)
	{
		this.text = text;
	}

	String getTagText()
	{
		return tagText;
	}
}
