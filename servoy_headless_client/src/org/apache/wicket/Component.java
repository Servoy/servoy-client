/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package org.apache.wicket;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.border.Border;

import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.ui.IComponent;

/**
 * @author jcomp
 *
 */
public class Component implements IComponent
{

	private String name;
	private Component parent;
	private final Map<String, Component> components = new HashMap<>();
	private boolean enabled;
	private boolean visible;

	public Component(String name)
	{
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setComponentEnabled(boolean)
	 */
	@Override
	public void setComponentEnabled(boolean enabled)
	{
		this.enabled = enabled;

	}

	@Override
	public boolean isEnabled()
	{
		return enabled;
	}

	@Override
	public void setComponentVisible(boolean visible)
	{
		this.visible = visible;
	}

	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	@Override
	public boolean isVisible()
	{
		return visible;
	}

	public String getMarkupId()
	{
		return getName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setLocation(java.awt.Point)
	 */
	@Override
	public void setLocation(Point location)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getLocation()
	 */
	@Override
	public Point getLocation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setSize(java.awt.Dimension)
	 */
	@Override
	public void setSize(Dimension size)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getSize()
	 */
	@Override
	public Dimension getSize()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setForeground(java.awt.Color)
	 */
	@Override
	public void setForeground(Color foreground)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getForeground()
	 */
	@Override
	public Color getForeground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setBackground(java.awt.Color)
	 */
	@Override
	public void setBackground(Color background)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getBackground()
	 */
	@Override
	public Color getBackground()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setFont(java.awt.Font)
	 */
	@Override
	public void setFont(Font font)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getFont()
	 */
	@Override
	public Font getFont()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setBorder(javax.swing.border.Border)
	 */
	@Override
	public void setBorder(Border border)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getBorder()
	 */
	@Override
	public Border getBorder()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		this.name = name;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setOpaque(boolean)
	 */
	@Override
	public void setOpaque(boolean opaque)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#isOpaque()
	 */
	@Override
	public boolean isOpaque()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setCursor(java.awt.Cursor)
	 */
	@Override
	public void setCursor(Cursor cursor)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#setToolTipText(java.lang.String)
	 */
	@Override
	public void setToolTipText(String tooltip)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	@Override
	public String getToolTipText()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.IComponent#getId()
	 */
	@Override
	public String getId()
	{
		return getName();
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public MainPage findPage()
	{
		if (this instanceof MainPage)
		{
			return (MainPage)this;
		}
		Component c = getParent();
		while (c != null)
		{
			if (c instanceof MainPage)
			{
				return (MainPage)c;
			}
			c = c.getParent();
		}
		return (MainPage)c;
	}

	/**
	 * @return
	 */
	public Component getParent()
	{
		return parent;
	}

	@SuppressWarnings("unchecked")
	public <T> T findParent(Class<T> cls)
	{
		if (parent != null)
		{
			if (cls.isAssignableFrom(parent.getClass()))
			{
				return (T)parent;
			}
			return parent.findParent(cls);
		}
		return null;
	}

	public Component get(String name)
	{
		return components.get(name);
	}

	public void replace(Component component)
	{
		add(component);
	}

	/**
	 * @param component
	 */
	public void add(Component component)
	{
		components.put(component.getName(), component);
		component.parent = this;

	}

	public void remove(Component component)
	{
		components.remove(component.getName());
	}

	public void remove()
	{
		if (parent != null)
		{
			parent.remove(this);
		}
	}

	public Iterator<Component> iterator()
	{
		return components.values().iterator();
	}

	public int size()
	{
		return components.size();
	}

	/**
	 *
	 */
	public void removeAll()
	{
		components.clear();
	}

}
