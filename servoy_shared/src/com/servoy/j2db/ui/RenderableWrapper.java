/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.ui;

import java.util.HashMap;
import java.util.Iterator;

/**
 * 
 * Wrapper used for keeping original property values for renderable components
 * 
 * @author gboros
 *
 */
public class RenderableWrapper implements IScriptRenderMethods
{
	private static enum PROPERTY
	{
		BGCOLOR, BORDER, ENABLED, FGCOLOR, FONT, TOOLTIP, TRANSPARENT, VISIBLE
	}

	private final IScriptRenderMethods renderable;
	private final HashMap<PROPERTY, Object> properties = new HashMap<PROPERTY, Object>();

	public RenderableWrapper(IScriptRenderMethods renderable)
	{
		this.renderable = renderable;
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getBgcolor()
	 */
	public String js_getBgcolor()
	{
		return renderable.js_getBgcolor();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setBgcolor(java.lang.String)
	 */
	public void js_setBgcolor(String clr)
	{
		if (!properties.containsKey(PROPERTY.BGCOLOR))
		{
			properties.put(PROPERTY.BGCOLOR, renderable.js_getBgcolor());
		}
		renderable.js_setBgcolor(clr);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getFgcolor()
	 */
	public String js_getFgcolor()
	{
		return renderable.js_getFgcolor();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setFgcolor(java.lang.String)
	 */
	public void js_setFgcolor(String clr)
	{
		if (!properties.containsKey(PROPERTY.FGCOLOR))
		{
			properties.put(PROPERTY.FGCOLOR, renderable.js_getFgcolor());
		}
		renderable.js_setFgcolor(clr);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_isVisible()
	 */
	public boolean js_isVisible()
	{
		return renderable.js_isVisible();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setVisible(boolean)
	 */
	public void js_setVisible(boolean b)
	{
		if (!properties.containsKey(PROPERTY.VISIBLE))
		{
			properties.put(PROPERTY.VISIBLE, Boolean.valueOf(renderable.js_isVisible()));
		}
		renderable.js_setVisible(b);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_isEnabled()
	 */
	public boolean js_isEnabled()
	{
		return renderable.js_isEnabled();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setEnabled(boolean)
	 */
	public void js_setEnabled(boolean b)
	{
		if (!properties.containsKey(PROPERTY.ENABLED))
		{
			properties.put(PROPERTY.ENABLED, Boolean.valueOf(renderable.js_isEnabled()));
		}
		renderable.js_setEnabled(b);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getLocationX()
	 */
	public int js_getLocationX()
	{
		return renderable.js_getLocationX();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getLocationY()
	 */
	public int js_getLocationY()
	{
		return renderable.js_getLocationY();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getAbsoluteFormLocationY()
	 */
	public int js_getAbsoluteFormLocationY()
	{
		return renderable.js_getAbsoluteFormLocationY();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getWidth()
	 */
	public int js_getWidth()
	{
		return renderable.js_getWidth();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getHeight()
	 */
	public int js_getHeight()
	{
		return renderable.js_getHeight();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getName()
	 */
	public String js_getName()
	{
		return renderable.js_getName();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return renderable.js_getElementType();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_putClientProperty(java.lang.Object, java.lang.Object)
	 */
	public void js_putClientProperty(Object key, Object value)
	{
		renderable.js_putClientProperty(key, value);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getClientProperty(java.lang.Object)
	 */
	public Object js_getClientProperty(Object key)
	{
		return renderable.js_getClientProperty(key);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getBorder()
	 */
	public String js_getBorder()
	{
		return renderable.js_getBorder();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setBorder(java.lang.String)
	 */
	public void js_setBorder(String spec)
	{
		if (!properties.containsKey(PROPERTY.BORDER))
		{
			properties.put(PROPERTY.BORDER, renderable.js_getBorder());
		}
		renderable.js_setBorder(spec);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getToolTipText()
	 */
	public String js_getToolTipText()
	{
		return renderable.js_getToolTipText();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setToolTipText(java.lang.String)
	 */
	public void js_setToolTipText(String tooltip)
	{
		if (!properties.containsKey(PROPERTY.TOOLTIP))
		{
			properties.put(PROPERTY.TOOLTIP, renderable.js_getToolTipText());
		}
		renderable.js_setToolTipText(tooltip);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getFont()
	 */
	public String js_getFont()
	{
		return renderable.js_getFont();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setFont(java.lang.String)
	 */
	public void js_setFont(String spec)
	{
		if (!properties.containsKey(PROPERTY.FONT))
		{
			properties.put(PROPERTY.FONT, renderable.js_getFont());
		}
		renderable.js_setFont(spec);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_isTransparent()
	 */
	public boolean js_isTransparent()
	{
		return renderable.js_isTransparent();
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_setTransparent(boolean)
	 */
	public void js_setTransparent(boolean b)
	{
		if (!properties.containsKey(PROPERTY.TRANSPARENT))
		{
			properties.put(PROPERTY.TRANSPARENT, Boolean.valueOf(renderable.js_isTransparent()));
		}
		renderable.js_setTransparent(b);
	}

	/*
	 * @see com.servoy.j2db.ui.IScriptRenderMethods#js_getDataProviderID()
	 */
	public String js_getDataProviderID()
	{
		return renderable.js_getDataProviderID();
	}

	public void resetProperties()
	{
		Iterator<PROPERTY> propertiesIte = properties.keySet().iterator();
		while (propertiesIte.hasNext())
		{
			switch (propertiesIte.next())
			{
				case BGCOLOR :
					renderable.js_setBgcolor((String)properties.get(PROPERTY.BGCOLOR));
					break;
				case BORDER :
					renderable.js_setBorder((String)properties.get(PROPERTY.BORDER));
					break;
				case ENABLED :
					renderable.js_setEnabled(((Boolean)properties.get(PROPERTY.ENABLED)).booleanValue());
					break;
				case FGCOLOR :
					renderable.js_setFgcolor((String)properties.get(PROPERTY.FGCOLOR));
					break;
				case FONT :
					renderable.js_setFont((String)properties.get(PROPERTY.FONT));
					break;
				case TOOLTIP :
					renderable.js_setToolTipText((String)properties.get(PROPERTY.TOOLTIP));
					break;
				case TRANSPARENT :
					renderable.js_setTransparent(((Boolean)properties.get(PROPERTY.TRANSPARENT)).booleanValue());
					break;
				case VISIBLE :
					renderable.js_setVisible(((Boolean)properties.get(PROPERTY.VISIBLE)).booleanValue());
					break;
			}
		}
		properties.clear();
	}

	@Override
	public String toString()
	{
		return renderable.toString();
	}
}
