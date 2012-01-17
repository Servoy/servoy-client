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

package com.servoy.j2db.ui.scripting;

import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.ui.IPortalComponent;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.Debug;

/**
 * Scriptable portal component.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimePortal extends AbstractRuntimeBaseComponent<IPortalComponent> implements IScriptPortalComponentMethods
{
	private IFoundSetInternal foundset;
	private JComponent jComponent;

	public RuntimePortal(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	/**
	 * @param jComponent the jComponent to set
	 */
	public void setJComponent(JComponent jComponent)
	{
		this.jComponent = jComponent;
	}

	public void setFoundset(IFoundSetInternal foundset)
	{
		this.foundset = foundset;
	}

	public String getElementType()
	{
		return IRuntimeComponent.PORTAL;
	}

	public String getSortColumns()
	{
		StringBuilder sb = new StringBuilder();
		if (foundset != null)
		{
			for (SortColumn sc : foundset.getSortColumns())
			{
				if (sb.length() > 0) sb.append(", "); //$NON-NLS-1$
				sb.append(sc.toString());
			}
		}
		return sb.toString();
	}

	@Override
	public void putClientProperty(Object key, Object value)
	{
		super.putClientProperty(key, value);
		if (jComponent != null)
		{
			jComponent.putClientProperty(key, value);
		}
	}

	public void setScroll(int x, int y)
	{
		if (jComponent != null)
		{
			jComponent.scrollRectToVisible(new Rectangle(x, y, jComponent.getWidth(), jComponent.getHeight()));
		}
	}

	public int getScrollX()
	{
		if (jComponent != null)
		{
			return jComponent.getVisibleRect().x;
		}
		return 0;
	}

	public int getScrollY()
	{
		if (jComponent != null)
		{
			return jComponent.getVisibleRect().y;
		}
		return 0;
	}

	@Deprecated
	public int js_getRecordIndex()
	{
		if (foundset != null) return foundset.getSelectedIndex() + 1;
		return 0;
	}

	public void deleteRecord()
	{
		if (foundset != null)
		{
			try
			{
				foundset.deleteRecord(foundset.getSelectedIndex());
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	public void setReadOnly(boolean b)
	{
		getComponent().setReadOnly(b);
		getChangesRecorder().setChanged();
	}

	public int getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	// 1-based
	public int jsFunction_getSelectedIndex()
	{
		return getSelectedIndex() + 1;
	}

	// 0-based
	public int getSelectedIndex()
	{
		return foundset.getSelectedIndex();
	}

	public int js_getMaxRecordIndex()
	{
		return foundset.getSize();
	}

	@Deprecated
	public void js_setRecordIndex(int i)
	{
		jsFunction_setSelectedIndex(i);
	}

	// 1-based
	public void jsFunction_setSelectedIndex(int i)
	{
		setSelectedIndex(i - 1);
	}

	// 0-based
	public void setSelectedIndex(int i)
	{
		if (i >= 0 && i < js_getMaxRecordIndex())
		{
			getComponent().setRecordIndex(i);
		}
	}

	public void newRecord()
	{
		newRecord(true);
	}

	public void newRecord(boolean addOnTop)
	{
		if (foundset != null)
		{
			try
			{
				getComponent().setRecordIndex(foundset.newRecord(addOnTop ? 0 : Integer.MAX_VALUE, true));
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	public void duplicateRecord()
	{
		duplicateRecord(true);
	}

	public void duplicateRecord(boolean addOnTop)
	{
		if (foundset != null)
		{
			try
			{
				getComponent().setRecordIndex(foundset.duplicateRecord(foundset.getSelectedIndex(), addOnTop ? 0 : Integer.MAX_VALUE));
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	public void setSize(int x, int y)
	{
		setComponentSize(x, y);
		getChangesRecorder().setSize(getComponent().getSize().width, getComponent().getSize().height, getComponent().getBorder(), new Insets(0, 0, 0, 0), 0);
	}

	public boolean isReadOnly()
	{
		return getComponent().isReadOnly();
	}
}
