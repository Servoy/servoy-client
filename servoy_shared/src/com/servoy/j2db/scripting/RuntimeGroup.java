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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;
import com.servoy.j2db.ui.IScriptTransparentMethods;

/**
 * Script object for a group of scriptables, delegates a fixed list of properties to all the enclosed scriptables.
 * 
 * @author rgansevles
 * 
 * @since 5.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class RuntimeGroup implements IScriptTransparentMethods, IScriptReadOnlyMethods
{
	private static final Rectangle NO_BOUNDS = new Rectangle(0, 0, 0, 0);

	private final String name;

	private final List<IScriptBaseMethods> scriptBaseObjects = new ArrayList<IScriptBaseMethods>();

	/**
	 * @param name 
	 * @param parent
	 */
	public RuntimeGroup(String name)
	{
		this.name = name;
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.GROUP;
	}

	public void addScriptBaseMethodsObj(IScriptBaseMethods baseMethodsObj)
	{
		scriptBaseObjects.add(baseMethodsObj);
	}

	public String js_getName()
	{
		return name;
	}

	public boolean js_isVisible()
	{
		// if 1 element is visible, the group is visible
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj.js_isVisible())
			{
				return true;
			}
		}

		return false;
	}

	public void js_setVisible(boolean b)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_setVisible(b);
		}
	}

	public boolean js_isEnabled()
	{
		// if 1 element is enabled, the group is enabled
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj.js_isEnabled())
			{
				return true;
			}
		}

		return false;
	}

	public void js_setEnabled(boolean b)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_setEnabled(b);
		}
	}

	public String js_getBgcolor()
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			String clr = obj.js_getBgcolor();
			if (clr != null)
			{
				return clr;
			}
		}
		return null;
	}

	public void js_setBgcolor(String clr)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_setBgcolor(clr);
		}
	}

	public String js_getFgcolor()
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			String clr = obj.js_getFgcolor();
			if (clr != null)
			{
				return clr;
			}
		}
		return null;
	}

	public void js_setFgcolor(String clr)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_setFgcolor(clr);
		}
	}

	public String js_getBorder()
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			String spec = obj.js_getBorder();
			if (spec != null)
			{
				return spec;
			}
		}
		return null;
	}

	public void js_setBorder(String spec)
	{

		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_setBorder(spec);
		}
	}

	public int js_getAbsoluteFormLocationY()
	{
		int y = -1;
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			y = Math.min(y == -1 ? Integer.MAX_VALUE : y, obj.js_getAbsoluteFormLocationY());
		}

		return y;
	}

	public void js_putClientProperty(Object key, Object value)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_putClientProperty(key, value);
		}
	}

	public Object js_getClientProperty(Object key)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			Object value = obj.js_getClientProperty(key);
			if (value != null)
			{
				return value;
			}
		}
		return null;
	}

	public void js_setToolTipText(String tooltip)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptTransparentMethods)
			{
				((IScriptTransparentMethods)obj).js_setToolTipText(tooltip);
			}
		}
	}

	public String js_getToolTipText()
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptTransparentMethods)
			{
				String tooltip = ((IScriptTransparentMethods)obj).js_getToolTipText();
				if (tooltip != null)
				{
					return tooltip;
				}
			}
		}
		return null;
	}

	public void js_setFont(String spec)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptTransparentMethods)
			{
				((IScriptTransparentMethods)obj).js_setFont(spec);
			}
		}
	}

	public String js_getFont()
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptTransparentMethods)
			{
				String spec = ((IScriptTransparentMethods)obj).js_getFont();
				if (spec != null)
				{
					return spec;
				}
			}
		}
		return null;
	}

	public boolean js_isTransparent()
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptTransparentMethods && !((IScriptTransparentMethods)obj).js_isTransparent())
			{
				return false;
			}
		}
		return true;
	}

	public void js_setTransparent(boolean b)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptTransparentMethods)
			{
				((IScriptTransparentMethods)obj).js_setTransparent(b);
			}
		}
	}

	public boolean js_isReadOnly()
	{

		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptReadOnlyMethods && !((IScriptReadOnlyMethods)obj).js_isReadOnly())
			{
				return false;
			}
		}
		return true;
	}

	public void js_setReadOnly(boolean b)
	{
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			if (obj instanceof IScriptReadOnlyMethods)
			{
				((IScriptReadOnlyMethods)obj).js_setReadOnly(b);
			}
		}
	}

	/*
	 * location---------------------------------------------------
	 */

	public int js_getLocationX()
	{
		return getBounds().x;
	}

	public int js_getLocationY()
	{
		return getBounds().y;
	}

	/*
	 * Move contained objects relative to location change.
	 * 
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_setLocation(int, int)
	 */
	public void js_setLocation(int x, int y)
	{
		Rectangle bounds = getBounds();
		int dx = x - bounds.x;
		int dy = y - bounds.y;

		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			obj.js_setLocation(obj.js_getLocationX() + dx, obj.js_getLocationY() + dy);
		}
	}

	/*
	 * size---------------------------------------------------
	 */

	public int js_getWidth()
	{
		return getBounds().width;
	}

	public int js_getHeight()
	{
		return getBounds().height;
	}

	/*
	 * Resize contained objects relative to size change.
	 * 
	 * @see com.servoy.j2db.ui.IScriptBaseMethods#js_setSize(int, int)
	 */
	public void js_setSize(int width, int height)
	{
		Rectangle bounds = getBounds();
		float scalew = ((float)width) / bounds.width;
		float scaleh = ((float)height) / bounds.height;

		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			int x = obj.js_getLocationX();
			int y = obj.js_getLocationY();
			obj.js_setLocation(bounds.x + (int)Math.floor(scalew * (x - bounds.x)), bounds.y + (int)Math.floor(scaleh * (y - bounds.y)));

			int w = obj.js_getWidth();
			int h = obj.js_getHeight();
			obj.js_setSize((int)Math.floor(scalew * w), (int)Math.floor(scaleh * h));
		}
	}

	protected Rectangle getBounds()
	{
		Rectangle bounds = null;
		for (IScriptBaseMethods obj : scriptBaseObjects)
		{
			int x = obj.js_getLocationX();
			int y = obj.js_getLocationY();
			int width = obj.js_getWidth();
			int height = obj.js_getHeight();
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
}
