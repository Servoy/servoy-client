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
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportReadOnly;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;

/**
 * Scriptable split pane.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeSplitPane extends AbstractRuntimeFormContainer<ISplitPane, JSplitPane> implements IScriptSplitPaneMethods
{
	public RuntimeSplitPane(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String getElementType()
	{
		return IRuntimeComponent.SPLITPANE;
	}

	@Override
	public void putClientProperty(Object key, Object value)
	{
		super.putClientProperty(key, value);
		if (enclosingComponent != null)
		{
			enclosingComponent.putClientProperty(key, value);
		}
	}

	public void setReadOnly(boolean b)
	{
		if (enclosingComponent instanceof ISupportReadOnly)
		{
			((ISupportReadOnly)enclosingComponent).setReadOnly(b);
		}
		else
		{
			getComponent().setReadOnly(b);
		}
		getChangesRecorder().setChanged();
	}

	public boolean isReadOnly()
	{
		if (enclosingComponent instanceof ISupportReadOnly)
		{
			return ((ISupportReadOnly)enclosingComponent).isReadOnly();
		}
		return getComponent().isReadOnly();
	}


	public int getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	public boolean js_setLeftForm(Object form, Object relation)
	{
		if (getComponent().setForm(true, form, relation))
		{
			getChangesRecorder().setChanged();
			return true;
		}
		return false;
	}

	public boolean js_setLeftForm(Object form)
	{
		return js_setLeftForm(form, null);
	}

	public boolean setLeftForm(String formName)
	{
		return js_setLeftForm(formName);
	}

	public boolean setLeftForm(String formName, String relationName)
	{
		return js_setLeftForm(formName, relationName);
	}

	public boolean setLeftForm(String formName, IFoundSet relatedFoundSet)
	{
		return js_setLeftForm(formName, relatedFoundSet);
	}

	public boolean js_setRightForm(Object form, Object relation)
	{
		if (getComponent().setForm(false, form, relation))
		{
			getChangesRecorder().setChanged();
			return true;
		}
		return false;
	}

	public boolean js_setRightForm(Object form)
	{
		return js_setRightForm(form, null);
	}

	public FormScope js_getLeftForm()
	{
		return getComponent().getForm(true);
	}

	public IForm getLeftForm()
	{
		FormScope left = js_getLeftForm();
		return left == null ? null : left.getFormController();
	}

	public FormScope js_getRightForm()
	{
		return getComponent().getForm(false);
	}

	public IForm getRightForm()
	{
		FormScope right = js_getRightForm();
		return right == null ? null : right.getFormController();
	}

	public boolean setRightForm(String formName)
	{
		return js_setRightForm(formName);
	}

	public boolean setRightForm(String formName, String relationName)
	{
		return js_setRightForm(formName, relationName);
	}

	public boolean setRightForm(String formName, IFoundSet relatedFoundSet)
	{
		return js_setRightForm(formName, relatedFoundSet);
	}

	public void setResizeWeight(double resizeWeight)
	{
		getComponent().setResizeWeight(resizeWeight);
		getChangesRecorder().setChanged();
	}

	public double getDividerLocation()
	{
		return getComponent().getDividerLocation();
	}

	public void setDividerLocation(double location)
	{
		getComponent().setRuntimeDividerLocation(location);
	}

	public int getDividerSize()
	{
		return getComponent().getDividerSize();
	}

	public void setDividerSize(int size)
	{
		getComponent().setDividerSize(size);
		getChangesRecorder().setChanged();
	}

	public double getResizeWeight()
	{
		return getComponent().getResizeWeight();
	}

	public boolean getContinuousLayout()
	{
		return getComponent().getContinuousLayout();
	}

	public void setContinuousLayout(boolean b)
	{
		getComponent().setContinuousLayout(b);
		getChangesRecorder().setChanged();
	}

	public int getRightFormMinSize()
	{
		return getComponent().getFormMinSize(false);
	}

	public void setRightFormMinSize(int minSize)
	{
		getComponent().setFormMinSize(false, minSize);
		getChangesRecorder().setChanged();
	}

	public int getLeftFormMinSize()
	{
		return getComponent().getFormMinSize(true);
	}

	public void setLeftFormMinSize(int minSize)
	{
		getComponent().setFormMinSize(true, minSize);
		getChangesRecorder().setChanged();
	}
}
