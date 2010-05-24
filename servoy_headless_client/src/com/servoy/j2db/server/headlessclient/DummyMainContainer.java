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
package com.servoy.j2db.server.headlessclient;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.Iterator;

import javax.swing.border.Border;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.FormManager.History;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.ui.IComponent;

public class DummyMainContainer implements IMainContainer
{
	private FormController f;

	public void showSolutionLoading(boolean b)
	{
	}

	public void showBlankPanel()
	{
	}

	public void show(String name)
	{
	}

	public void flushCachedItems()
	{
	}

	public FormController getNavigator()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getContainerName()
	 */
	public String getContainerName()
	{
		return null;
	}

	public FormController setNavigator(FormController c)
	{
		return null;
	}

	public void add(IComponent c, String name)
	{
	}

	public void remove(IComponent c)
	{
	}

	public void removeAll()
	{
		//never used
	}

	public String getName()
	{
		return null;
	}

	public void setName(String name)
	{
	}

	public void setBackground(Color background)
	{
	}

	public Color getBackground()
	{
		return null;
	}

	public void setBorder(Border border)
	{
	}

	public Border getBorder()
	{
		return null;
	}

	public void setComponentVisible(boolean visible)
	{
	}

	public String getId()
	{
		return null;
	}

	public boolean isVisible()
	{
		return false;
	}

	public void setCursor(Cursor cursor)
	{
	}

	public void setFont(Font font)
	{
	}

	public Font getFont()
	{
		return null;
	}

	public void setForeground(Color foreground)
	{
	}

	public Color getForeground()
	{
		return null;
	}

	public void setLocation(Point location)
	{
	}

	public Point getLocation()
	{
		return null;
	}

	public void setOpaque(boolean opaque)
	{
	}

	public boolean isOpaque()
	{
		return false;
	}

	public void setToolTipText(String tooltip)
	{
	}

	public void setSize(Dimension size)
	{
	}

	public Dimension getSize()
	{
		return null;
	}

	public boolean isEnabled()
	{
		return false;
	}

	public void setComponentEnabled(boolean enabled)
	{
	}

	public Iterator<IComponent> getComponentIterator()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getCurrentForm()
	 */
	public Form getCurrentForm()
	{
		return f != null ? f.getForm() : null;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#setCurrentForm(com.servoy.j2db.persistence.Form)
	 */
	public void setFormController(FormController f)
	{
		this.f = f;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getForm()
	 */
	public FormController getController()
	{
		return f;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getHistory()
	 */
	private History history;

	public History getHistory()
	{
		if (history == null)
		{
			history = new FormManager.History(f.getApplication(), this);
		}
		return history;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#setTitle(java.lang.String)
	 */
	public void setTitle(String titleText)
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#getToolTipText()
	 */
	public String getToolTipText()
	{
		//ignore
		return null;
	}

	public void requestFocus()
	{
	}
}
