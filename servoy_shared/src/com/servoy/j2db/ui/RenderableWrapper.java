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

import com.servoy.j2db.ui.runtime.HasRuntimeFormat;


/**
 * 
 * Wrapper used for keeping original property values for renderable components
 * 
 * @author gboros
 */
@SuppressWarnings("nls")
public class RenderableWrapper implements IScriptRenderMethodsWithFormat
{
	public static final String PROPERTY_BGCOLOR = "bgcolor";
	public static final String PROPERTY_BORDER = "border";
	public static final String PROPERTY_ENABLED = "enabled";
	public static final String PROPERTY_FGCOLOR = "fgcolor";
	public static final String PROPERTY_FONT = "font";
	public static final String PROPERTY_TOOLTIP = "toolTipText";
	public static final String PROPERTY_TRANSPARENT = "transparant";
	public static final String PROPERTY_VISIBLE = "visible";
	public static final String PROPERTY_FORMAT = "format";

	private final IScriptRenderMethods renderable;
	private final HashMap<String, Object> properties = new HashMap<String, Object>();

	public RenderableWrapper(IScriptRenderMethods renderable)
	{
		this.renderable = renderable;
	}

	public String getFormat()
	{
		if (renderable instanceof HasRuntimeFormat)
		{
			return ((HasRuntimeFormat)renderable).getFormat();
		}
		return null;
	}

	public void setFormat(String format)
	{
		if (renderable instanceof HasRuntimeFormat)
		{
			if (!properties.containsKey(PROPERTY_FORMAT))
			{
				properties.put(PROPERTY_FORMAT, ((HasRuntimeFormat)renderable).getFormat());
			}
			((HasRuntimeFormat)renderable).setFormat(format);
		}
	}

	public String getBgcolor()
	{
		return renderable.getBgcolor();
	}

	public void setBgcolor(String clr)
	{
		if (!properties.containsKey(PROPERTY_BGCOLOR))
		{
			properties.put(PROPERTY_BGCOLOR, renderable.getBgcolor());
		}
		renderable.setBgcolor(clr);
	}

	public String getFgcolor()
	{
		return renderable.getFgcolor();
	}

	public void setFgcolor(String clr)
	{
		if (!properties.containsKey(PROPERTY_FGCOLOR))
		{
			properties.put(PROPERTY_FGCOLOR, renderable.getFgcolor());
		}
		renderable.setFgcolor(clr);
	}

	public boolean isVisible()
	{
		return renderable.isVisible();
	}

	public void setVisible(boolean b)
	{
		if (!properties.containsKey(PROPERTY_VISIBLE))
		{
			properties.put(PROPERTY_VISIBLE, Boolean.valueOf(renderable.isVisible()));
		}
		renderable.setVisible(b);
	}

	public boolean isEnabled()
	{
		return renderable.isEnabled();
	}

	public void setEnabled(boolean b)
	{
		if (!properties.containsKey(PROPERTY_ENABLED))
		{
			properties.put(PROPERTY_ENABLED, Boolean.valueOf(renderable.isEnabled()));
		}
		renderable.setEnabled(b);
	}

	public int getLocationX()
	{
		return renderable.getLocationX();
	}

	public int getLocationY()
	{
		return renderable.getLocationY();
	}

	public int getAbsoluteFormLocationY()
	{
		return renderable.getAbsoluteFormLocationY();
	}

	public int getWidth()
	{
		return renderable.getWidth();
	}

	public int getHeight()
	{
		return renderable.getHeight();
	}

	public String getName()
	{
		return renderable.getName();
	}

	public String getElementType()
	{
		return renderable.getElementType();
	}

	public void putClientProperty(Object key, Object value)
	{
		renderable.putClientProperty(key, value);
	}

	public Object getClientProperty(Object key)
	{
		return renderable.getClientProperty(key);
	}

	public String getBorder()
	{
		return renderable.getBorder();
	}

	public void setBorder(String spec)
	{
		if (!properties.containsKey(PROPERTY_BORDER))
		{
			properties.put(PROPERTY_BORDER, renderable.getBorder());
		}
		renderable.setBorder(spec);
	}

	public String getToolTipText()
	{
		return renderable.getToolTipText();
	}

	public void setToolTipText(String tooltip)
	{
		if (!properties.containsKey(PROPERTY_TOOLTIP))
		{
			properties.put(PROPERTY_TOOLTIP, renderable.getToolTipText());
		}
		renderable.setToolTipText(tooltip);
	}

	public String getFont()
	{
		return renderable.getFont();
	}

	public void setFont(String spec)
	{
		if (!properties.containsKey(PROPERTY_FONT))
		{
			properties.put(PROPERTY_FONT, renderable.getFont());
		}
		renderable.setFont(spec);
	}

	public boolean isTransparent()
	{
		return renderable.isTransparent();
	}

	public void setTransparent(boolean b)
	{
		if (!properties.containsKey(PROPERTY_TRANSPARENT))
		{
			properties.put(PROPERTY_TRANSPARENT, Boolean.valueOf(renderable.isTransparent()));
		}
		renderable.setTransparent(b);
	}

	public String getDataProviderID()
	{
		return renderable.getDataProviderID();
	}

	/**
	 * IMPORTANT: This method should only be called while onRender is being fired. See SVY-2571.
	 */
	void resetProperties()
	{
		Iterator<String> propertiesIte = properties.keySet().iterator();
		String property;
		while (propertiesIte.hasNext())
		{
			property = propertiesIte.next();

			if (PROPERTY_BGCOLOR.equals(property))
			{
				renderable.setBgcolor((String)properties.get(PROPERTY_BGCOLOR));
			}
			else if (PROPERTY_BORDER.equals(property))
			{
				renderable.setBorder((String)properties.get(PROPERTY_BORDER));
			}
			else if (PROPERTY_ENABLED.equals(property))
			{
				renderable.setEnabled(((Boolean)properties.get(PROPERTY_ENABLED)).booleanValue());
			}
			else if (PROPERTY_FGCOLOR.equals(property))
			{
				renderable.setFgcolor((String)properties.get(PROPERTY_FGCOLOR));
			}
			else if (PROPERTY_FONT.equals(property))
			{
				renderable.setFont((String)properties.get(PROPERTY_FONT));
			}
			else if (PROPERTY_TOOLTIP.equals(property))
			{
				renderable.setToolTipText((String)properties.get(PROPERTY_TOOLTIP));
			}
			else if (PROPERTY_TRANSPARENT.equals(property))
			{
				renderable.setTransparent(((Boolean)properties.get(PROPERTY_TRANSPARENT)).booleanValue());
			}
			else if (PROPERTY_VISIBLE.equals(property))
			{
				renderable.setVisible(((Boolean)properties.get(PROPERTY_VISIBLE)).booleanValue());
			}
			else if (PROPERTY_FORMAT.equals(property))
			{
				if (renderable instanceof HasRuntimeFormat)
				{
					((HasRuntimeFormat)renderable).setFormat((String)properties.get(PROPERTY_FORMAT));
				}
			}
		}
		properties.clear();
	}

	public void clearProperty(String property)
	{
		properties.remove(property);
	}

	@Override
	public String toString()
	{
		return renderable.toString();
	}
}
