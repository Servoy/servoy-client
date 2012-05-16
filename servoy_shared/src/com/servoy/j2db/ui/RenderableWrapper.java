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
 *
 */
public class RenderableWrapper implements IScriptRenderMethodsWithFormat
{
	private static enum PROPERTY
	{
		BGCOLOR, BORDER, ENABLED, FGCOLOR, FONT, TOOLTIP, TRANSPARENT, VISIBLE, FORMAT
	}

	private final IScriptRenderMethods renderable;
	private final HashMap<PROPERTY, Object> properties = new HashMap<PROPERTY, Object>();

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
			if (!properties.containsKey(PROPERTY.FORMAT))
			{
				properties.put(PROPERTY.FORMAT, ((HasRuntimeFormat)renderable).getFormat());
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
		if (!properties.containsKey(PROPERTY.BGCOLOR))
		{
			properties.put(PROPERTY.BGCOLOR, renderable.getBgcolor());
		}
		renderable.setBgcolor(clr);
	}

	public String getFgcolor()
	{
		return renderable.getFgcolor();
	}

	public void setFgcolor(String clr)
	{
		if (!properties.containsKey(PROPERTY.FGCOLOR))
		{
			properties.put(PROPERTY.FGCOLOR, renderable.getFgcolor());
		}
		renderable.setFgcolor(clr);
	}

	public boolean isVisible()
	{
		return renderable.isVisible();
	}

	public void setVisible(boolean b)
	{
		if (!properties.containsKey(PROPERTY.VISIBLE))
		{
			properties.put(PROPERTY.VISIBLE, Boolean.valueOf(renderable.isVisible()));
		}
		renderable.setVisible(b);
	}

	public boolean isEnabled()
	{
		return renderable.isEnabled();
	}

	public void setEnabled(boolean b)
	{
		if (!properties.containsKey(PROPERTY.ENABLED))
		{
			properties.put(PROPERTY.ENABLED, Boolean.valueOf(renderable.isEnabled()));
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
		if (!properties.containsKey(PROPERTY.BORDER))
		{
			properties.put(PROPERTY.BORDER, renderable.getBorder());
		}
		renderable.setBorder(spec);
	}

	public String getToolTipText()
	{
		return renderable.getToolTipText();
	}

	public void setToolTipText(String tooltip)
	{
		if (!properties.containsKey(PROPERTY.TOOLTIP))
		{
			properties.put(PROPERTY.TOOLTIP, renderable.getToolTipText());
		}
		renderable.setToolTipText(tooltip);
	}

	public String getFont()
	{
		return renderable.getFont();
	}

	public void setFont(String spec)
	{
		if (!properties.containsKey(PROPERTY.FONT))
		{
			properties.put(PROPERTY.FONT, renderable.getFont());
		}
		renderable.setFont(spec);
	}

	public boolean isTransparent()
	{
		return renderable.isTransparent();
	}

	public void setTransparent(boolean b)
	{
		if (!properties.containsKey(PROPERTY.TRANSPARENT))
		{
			properties.put(PROPERTY.TRANSPARENT, Boolean.valueOf(renderable.isTransparent()));
		}
		renderable.setTransparent(b);
	}

	public String getDataProviderID()
	{
		return renderable.getDataProviderID();
	}

	public void resetProperties()
	{
		Iterator<PROPERTY> propertiesIte = properties.keySet().iterator();
		while (propertiesIte.hasNext())
		{
			switch (propertiesIte.next())
			{
				case BGCOLOR :
					renderable.setBgcolor((String)properties.get(PROPERTY.BGCOLOR));
					break;
				case BORDER :
					renderable.setBorder((String)properties.get(PROPERTY.BORDER));
					break;
				case ENABLED :
					renderable.setEnabled(((Boolean)properties.get(PROPERTY.ENABLED)).booleanValue());
					break;
				case FGCOLOR :
					renderable.setFgcolor((String)properties.get(PROPERTY.FGCOLOR));
					break;
				case FONT :
					renderable.setFont((String)properties.get(PROPERTY.FONT));
					break;
				case TOOLTIP :
					renderable.setToolTipText((String)properties.get(PROPERTY.TOOLTIP));
					break;
				case TRANSPARENT :
					renderable.setTransparent(((Boolean)properties.get(PROPERTY.TRANSPARENT)).booleanValue());
					break;
				case VISIBLE :
					renderable.setVisible(((Boolean)properties.get(PROPERTY.VISIBLE)).booleanValue());
					break;
				case FORMAT :
					if (renderable instanceof HasRuntimeFormat)
					{
						((HasRuntimeFormat)renderable).setFormat((String)properties.get(PROPERTY.FORMAT));
					}
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
