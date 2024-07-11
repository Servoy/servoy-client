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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.ui.IRect;
import com.servoy.j2db.ui.scripting.RuntimeRectangle;

/**
 * Represents a rectangle in the browser, (a div with a 1 pixel border)
 *
 * @author jcompagner
 */
public class WebRect extends WebBaseLabel implements IRect
{

	private final int shapeType;
	private int lineWidth;

	/**
	 * @param id
	 */
	public WebRect(String id, IApplication application, RuntimeRectangle scriptable, int type)
	{
		super(application, scriptable, id);
		this.shapeType = type;
		if (type != RectShape.BORDER_PANEL)
		{
			setBackground(Color.white);
			setForeground(Color.black);
		}
	}

	/**
	 * @see com.servoy.j2db.ui.IRect#getLineWidth()
	 */
	public int getLineWidth()
	{
		return lineWidth;
	}

	/**
	 * @see com.servoy.j2db.ui.IRect#getRadius()
	 */
	public int getRadius()
	{
		return 0;
	}

	/**
	 * @see com.servoy.j2db.ui.IRect#setLineWidth(int)
	 */
	public void setLineWidth(int lineWidth)
	{
		this.lineWidth = lineWidth;
	}

	/**
	 * @see com.servoy.j2db.ui.IRect#setRadius(int)
	 */
	public void setRadius(int radius)
	{
	}

	@Override
	public int getFontSize()
	{
		return 0;
	}
}
