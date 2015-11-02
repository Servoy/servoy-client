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
package com.servoy.j2db.scripting;


import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.ui.runtime.HasRuntimeReadOnly;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.ui.runtime.IRuntimeGroup;

/**
 * Script object for a group of scriptables, delegates a fixed list of properties to all the enclosed scriptables.
 *
 * @author rgansevles
 *
 * @since 5.0
 */
public class RuntimeGroup implements IRuntimeGroup
{
	private static final Rectangle NO_BOUNDS = new Rectangle(0, 0, 0, 0);

	private final String name;

	private final List<IRuntimeComponent> scriptBaseObjects = new ArrayList<IRuntimeComponent>();

	/**
	 * @param name
	 * @param parent
	 */
	public RuntimeGroup(String name)
	{
		this.name = name;
	}

	public String getElementType()
	{
		return IRuntimeComponent.GROUP;
	}

	public void addScriptBaseMethodsObj(IRuntimeComponent baseMethodsObj)
	{
		scriptBaseObjects.add(baseMethodsObj);
	}

	public String getName()
	{
		return name;
	}

	public boolean isVisible()
	{
		// if 1 element is visible, the group is visible
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			if (obj.isVisible())
			{
				return true;
			}
		}

		return false;
	}

	public void setVisible(boolean b)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setVisible(b);
		}
	}

	public boolean isEnabled()
	{
		// if 1 element is enabled, the group is enabled
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			if (obj.isEnabled())
			{
				return true;
			}
		}

		return false;
	}

	public void setEnabled(boolean b)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setEnabled(b);
		}
	}

	public String getBgcolor()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			String clr = obj.getBgcolor();
			if (clr != null)
			{
				return clr;
			}
		}
		return null;
	}

	public void setBgcolor(String clr)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setBgcolor(clr);
		}
	}

	public String getFgcolor()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			String clr = obj.getFgcolor();
			if (clr != null)
			{
				return clr;
			}
		}
		return null;
	}

	public void setFgcolor(String clr)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setFgcolor(clr);
		}
	}

	public String getBorder()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			String spec = obj.getBorder();
			if (spec != null)
			{
				return spec;
			}
		}
		return null;
	}

	public void setBorder(String spec)
	{

		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setBorder(spec);
		}
	}

	public int getAbsoluteFormLocationY()
	{
		int y = -1;
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			y = Math.min(y == -1 ? Integer.MAX_VALUE : y, obj.getAbsoluteFormLocationY());
		}

		return y;
	}

	public void putClientProperty(Object key, Object value)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.putClientProperty(key, value);
		}
	}

	public Object getClientProperty(Object key)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			Object value = obj.getClientProperty(key);
			if (value != null)
			{
				return value;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.runtime.HasRuntimeDesignTimeProperty#getDesignTimeProperty(java.lang.String)
	 */
	public Object getDesignTimeProperty(String key)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			Object value = obj.getDesignTimeProperty(key);
			if (value != null)
			{
				return value;
			}
		}
		return null;
	}

	public void setToolTipText(String tooltip)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setToolTipText(tooltip);
		}
	}

	public String getToolTipText()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			String tooltip = obj.getToolTipText();
			if (tooltip != null)
			{
				return tooltip;
			}
		}
		return null;
	}

	public void setFont(String spec)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setFont(spec);
		}
	}

	public String getFont()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			String spec = obj.getFont();
			if (spec != null)
			{
				return spec;
			}
		}
		return null;
	}

	public boolean isTransparent()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			if (!obj.isTransparent())
			{
				return false;
			}
		}
		return true;
	}

	public void setTransparent(boolean b)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setTransparent(b);
		}
	}

	public boolean isReadOnly()
	{

		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			if (obj instanceof HasRuntimeReadOnly && !((HasRuntimeReadOnly)obj).isReadOnly())
			{
				return false;
			}
		}
		return true;
	}

	public void setReadOnly(boolean b)
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			if (obj instanceof HasRuntimeReadOnly)
			{
				((HasRuntimeReadOnly)obj).setReadOnly(b);
			}
		}
	}

	/*
	 * location---------------------------------------------------
	 */

	public int getLocationX()
	{
		return getBounds().x;
	}

	public int getLocationY()
	{
		return getBounds().y;
	}

	/*
	 * Move contained objects relative to location change.
	 *
	 * @see com.servoy.j2db.ui.runtime.IRuntimeComponent#setLocation(int, int)
	 */
	public void setLocation(int x, int y)
	{
		Rectangle bounds = getBounds();
		int dx = x - bounds.x;
		int dy = y - bounds.y;

		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			obj.setLocation(obj.getLocationX() + dx, obj.getLocationY() + dy);
		}
	}

	/*
	 * size---------------------------------------------------
	 */

	public int getWidth()
	{
		return getBounds().width;
	}

	public int getHeight()
	{
		return getBounds().height;
	}

	/*
	 * Resize contained objects relative to size change.
	 *
	 * @see com.servoy.j2db.ui.runtime.IRuntimeComponent#setSize(int, int)
	 */
	public void setSize(int width, int height)
	{
		Rectangle bounds = getBounds();
		float scalew = ((float)width) / bounds.width;
		float scaleh = ((float)height) / bounds.height;

		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			int x = obj.getLocationX();
			int y = obj.getLocationY();
			obj.setLocation(bounds.x + (int)Math.floor(scalew * (x - bounds.x)), bounds.y + (int)Math.floor(scaleh * (y - bounds.y)));

			int w = obj.getWidth();
			int h = obj.getHeight();
			obj.setSize((int)Math.floor(scalew * w), (int)Math.floor(scaleh * h));
		}
	}

	protected Rectangle getBounds()
	{
		Rectangle bounds = null;
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			int x = obj.getLocationX();
			int y = obj.getLocationY();
			int width = obj.getWidth();
			int height = obj.getHeight();
			Rectangle rect = new Rectangle(x, y, width, height);
			if (bounds == null)
			{
				bounds = rect;
			}
			else
			{
				bounds = bounds.union(rect);
			}
		}
		return bounds == null ? NO_BOUNDS : bounds;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.runtime.HasRuntimeFormName#getFormName()
	 */
	@Override
	public String getFormName()
	{
		for (IRuntimeComponent obj : scriptBaseObjects)
		{
			String formName = obj.getFormName();
			if (formName != null)
			{
				return formName;
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public void addStyleClassStyle(String styleName)
	{
		//only implemented in ngclient
	}

	@Override
	public void removeStyleClassStyle(String styleName)
	{
		//only implemented in ngclient

	}
}
