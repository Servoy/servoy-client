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

import javax.swing.JSplitPane;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportReadOnly;

/**
 * @author lvostinar
 * @since 6.0
 *
 */
public class RuntimeSplitPane extends AbstractRuntimeFormContainer implements IScriptSplitPaneMethods
{
	private final ISplitPane splitPane;

	public RuntimeSplitPane(ISplitPane component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application, JSplitPane enclosingComponent)
	{
		super(component, jsChangeRecorder, application, enclosingComponent);
		this.splitPane = component;
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.SPLITPANE;
	}

	@Override
	public void js_putClientProperty(Object key, Object value)
	{
		super.js_putClientProperty(key, value);
		if (enclosingComponent != null)
		{
			enclosingComponent.putClientProperty(key, value);
		}
	}

	public void js_setReadOnly(boolean b)
	{
		if (enclosingComponent instanceof ISupportReadOnly)
		{
			((ISupportReadOnly)enclosingComponent).setReadOnly(b);
		}
		else
		{
			splitPane.setReadOnly(b);
		}
		jsChangeRecorder.setChanged();
	}

	public int js_getAbsoluteFormLocationY()
	{
		return splitPane.getAbsoluteFormLocationY();
	}

	public boolean js_setLeftForm(Object form, Object relation)
	{
		if (splitPane.setForm(true, form, relation))
		{
			jsChangeRecorder.setChanged();
			return true;
		}
		else return false;
	}

	public boolean js_setLeftForm(Object form)
	{
		return js_setLeftForm(form, null);
	}

	public boolean js_setRightForm(Object form, Object relation)
	{
		if (splitPane.setForm(false, form, relation))
		{
			jsChangeRecorder.setChanged();
			return true;
		}
		else return false;
	}

	public boolean js_setRightForm(Object form)
	{
		return js_setRightForm(form, null);
	}

	public FormScope js_getLeftForm()
	{
		return splitPane.getForm(true);
	}

	public FormScope js_getRightForm()
	{
		return splitPane.getForm(false);
	}

	public void js_setResizeWeight(double resizeWeight)
	{
		splitPane.setResizeWeight(resizeWeight);
	}

	public double js_getDividerLocation()
	{
		return splitPane.getDividerLocation();
	}

	public void js_setDividerLocation(double location)
	{
		splitPane.setRuntimeDividerLocation(location);

	}

	public int js_getDividerSize()
	{
		return splitPane.getDividerSize();
	}

	public void js_setDividerSize(int size)
	{
		splitPane.setDividerSize(size);

	}

	public double js_getResizeWeight()
	{
		return splitPane.getResizeWeight();
	}

	public boolean js_getContinuousLayout()
	{
		return splitPane.getContinuousLayout();
	}

	public void js_setContinuousLayout(boolean b)
	{
		splitPane.setContinuousLayout(b);
	}

	public int js_getRightFormMinSize()
	{
		return splitPane.getFormMinSize(false);
	}

	public void js_setRightFormMinSize(int minSize)
	{
		splitPane.setFormMinSize(false, minSize);

	}

	public int js_getLeftFormMinSize()
	{
		return splitPane.getFormMinSize(true);
	}

	public void js_setLeftFormMinSize(int minSize)
	{
		splitPane.setFormMinSize(true, minSize);

	}
}
