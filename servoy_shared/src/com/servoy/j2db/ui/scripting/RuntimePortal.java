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
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
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

	public String js_getElementType()
	{
		return IScriptBaseMethods.PORTAL;
	}

	public String js_getSortColumns()
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
	public void js_putClientProperty(Object key, Object value)
	{
		super.js_putClientProperty(key, value);
		if (jComponent != null)
		{
			jComponent.putClientProperty(key, value);
		}
	}

	public void js_setScroll(int x, int y)
	{
		if (jComponent != null)
		{
			jComponent.scrollRectToVisible(new Rectangle(x, y, jComponent.getWidth(), jComponent.getHeight()));
		}
	}

	public int js_getScrollX()
	{
		if (jComponent != null)
		{
			return jComponent.getVisibleRect().x;
		}
		return 0;
	}

	public int js_getScrollY()
	{
		if (jComponent != null)
		{
			return jComponent.getVisibleRect().y;
		}
		return 0;
	}

	public int js_getRecordIndex()
	{
		if (foundset != null) return foundset.getSelectedIndex() + 1;
		return 0;
	}

	public void js_deleteRecord()
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

	public void js_setReadOnly(boolean b)
	{
		getComponent().setReadOnly(b);
		getChangesRecorder().setChanged();
	}

	public int js_getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	public int jsFunction_getSelectedIndex()
	{
		return foundset.getSelectedIndex() + 1;
	}

	public int js_getMaxRecordIndex()
	{
		return foundset.getSize();
	}

	@Deprecated
	public void js_setRecordIndex(int i)
	{
		if (i >= 1 && i <= js_getMaxRecordIndex())
		{
			getComponent().setRecordIndex(i - 1);
		}
	}

	public void jsFunction_setSelectedIndex(int i) //Object[] args)
	{
		if (i >= 1 && i <= js_getMaxRecordIndex())
		{
			getComponent().setRecordIndex(i - 1);
		}
	}

	public void js_newRecord(Object[] vargs)
	{
		boolean addOnTop = true;
		if (vargs != null && vargs.length >= 1 && vargs[0] instanceof Boolean)
		{
			addOnTop = ((Boolean)vargs[0]).booleanValue();
		}
		if (foundset != null)
		{
			try
			{
				int i = foundset.newRecord(addOnTop ? 0 : Integer.MAX_VALUE, true);
				getComponent().setRecordIndex(i);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	public void js_duplicateRecord(Object[] vargs)
	{
		boolean addOnTop = true;
		if (vargs != null && vargs.length >= 1 && vargs[0] instanceof Boolean)
		{
			addOnTop = ((Boolean)vargs[0]).booleanValue();
		}
		if (foundset != null)
		{
			try
			{
				int i = foundset.duplicateRecord(foundset.getSelectedIndex(), addOnTop ? 0 : Integer.MAX_VALUE);
				getComponent().setRecordIndex(i);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	public void js_setSize(int x, int y)
	{
		setComponentSize(x, y);
		getChangesRecorder().setSize(getComponent().getSize().width, getComponent().getSize().height, getComponent().getBorder(), new Insets(0, 0, 0, 0), 0);
	}

	public boolean js_isReadOnly()
	{
		return getComponent().isReadOnly();
	}
}
