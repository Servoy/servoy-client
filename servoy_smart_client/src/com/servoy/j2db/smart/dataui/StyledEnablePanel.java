/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.UIManager;
import javax.swing.text.html.CSS;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.ImageLoader;

/**
 * Panel with css style support.
 * 
 * @author rgansevles
 *
 */
public class StyledEnablePanel extends EnablePanel
{
	private IApplication application;
	private IStyleRule partRule = null;
	private boolean paintBackgroundOnTopOfFormImage = false;
	private Color bgColor = null;

	public StyledEnablePanel(IApplication application)
	{
		this.application = application;
	}

	public StyledEnablePanel()
	{
		this(null);
	}

	public IApplication getApplication()
	{
		return application;
	}

	public void setApplication(IApplication application)
	{
		this.application = application;
	}


	public void setPaintBackgroundOnTopOfFormImage(boolean paintBackgroundOnTopOfFormImage)
	{
		this.paintBackgroundOnTopOfFormImage = paintBackgroundOnTopOfFormImage;
	}

	public void setCssRule(IStyleRule rule)
	{
		this.partRule = rule;
	}

	public void setBgColor(Color color)
	{
		this.bgColor = color;
	}

	@Override
	public void paint(Graphics g)
	{
		if (paintBackgroundOnTopOfFormImage || (partRule != null && partRule.hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString())))
		{
			// image always on top, set opaque to false
			setOpaque(false);
			Color bg = getBackground();
			if (g != null && bg != null && bg != UIManager.getColor("Panel.background") &&
				((partRule != null && partRule.hasAttribute(CSS.Attribute.BACKGROUND_COLOR.toString())) || bgColor != null))
			{
				if (bg.getAlpha() < 255)
				{
					Graphics g2 = g.create();
					try
					{
						Dimension size = getSize();
						g2.setClip(0, 0, size.width, size.height);
						g2.setColor(bg);
						g2.fillRect(0, 0, size.width, size.height);
					}
					finally
					{
						g2.dispose();
					}
				}
				else
				{
					Color tmp = g.getColor();
					// paint background color first, form is transparent
					g.setColor(bg);
					g.fillRect(0, 0, getWidth(), getHeight());

					g.setColor(tmp);
				}
			}
		}
		ImageLoader.paintImage(g, partRule, application, getSize());
		super.paint(g);
	}
}
