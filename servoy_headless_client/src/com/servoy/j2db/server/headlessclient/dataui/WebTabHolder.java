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

import java.awt.Color;

import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.util.Utils;

/**
 * A container object that holds a single tab {@link IFormLookupPanel} for a {@link WebTabPanel}
 * 
 * @author jcompagner
 */
public class WebTabHolder
{
	private final WebTabFormLookup panel;
	private final byte[] iconData;
	private MediaResource icon;
	private final String tooltip;
	private String text;
	private String tagText;
	public int mnemonic;
	private boolean enabled;
	private Color foreground = null;

	WebTabHolder(String t, IFormLookupPanel panel, byte[] iconData, String tooltip)
	{
		this.iconData = iconData;
		if (iconData != null)
		{
			icon = new MediaResource(iconData, 0);
		}
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

	int getDisplayedMnemonic()
	{
		return mnemonic;
	}

	void setDisplayedMnemonic(int m)
	{
		this.mnemonic = m;
	}

	String getTagText()
	{
		return tagText;
	}

	public MediaResource getIcon()
	{
		return icon;
	}

	/**
	 * @return the foreground
	 */
	public Color getForeground()
	{
		return foreground;
	}

	/**
	 * @param foreground the foreground to set
	 */
	public void setForeground(Color foreground)
	{
		this.foreground = foreground;
	}

	/**
	 * @return the tooltip
	 */
	public String getTooltip()
	{
		return tooltip;
	}
}
