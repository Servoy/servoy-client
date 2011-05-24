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

package com.servoy.j2db.ui;

import java.awt.Insets;
import java.util.Properties;

import javax.swing.border.Border;

/**
 * @author lvostinar
 *
 */
public class DummyChangesRecorder implements IStylePropertyChangesRecorder
{
	public static final DummyChangesRecorder INSTANCE = new DummyChangesRecorder();

	/**
	 * 
	 */
	private DummyChangesRecorder()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#getChanges()
	 */
	public Properties getChanges()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#isChanged()
	 */
	public boolean isChanged()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#setChanges(java.util.Properties)
	 */
	public void setChanges(Properties changes)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#setRendered()
	 */
	public void setRendered()
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#setChanged()
	 */
	public void setChanged()
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#isValueChanged()
	 */
	public boolean isValueChanged()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChanges#setValueChanged()
	 */
	public void setValueChanged()
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setBgcolor(java.lang.String)
	 */
	public void setBgcolor(String bgcolor)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setFgcolor(java.lang.String)
	 */
	public void setFgcolor(String clr)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setBorder(java.lang.String)
	 */
	public void setBorder(String border)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setTransparent(boolean)
	 */
	public void setTransparent(boolean transparent)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setLocation(int, int)
	 */
	public void setLocation(int x, int y)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setFont(java.lang.String)
	 */
	public void setFont(String spec)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setVisible(boolean)
	 */
	public void setVisible(boolean visible)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setSize(int, int, javax.swing.border.Border, java.awt.Insets, int)
	 */
	public void setSize(int width, int height, Border border, Insets margin, int fontSize)
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.IStylePropertyChangesRecorder#setSize(int, int, javax.swing.border.Border, java.awt.Insets, int, boolean, int)
	 */
	public void setSize(int width, int height, Border border, Insets margin, int fontSize, boolean isButton, int valign)
	{

	}

}
