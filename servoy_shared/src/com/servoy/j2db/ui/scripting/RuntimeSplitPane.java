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

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;

/**
 * Scriptable split pane.
 * 
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeSplitPane extends AbstractRuntimeTabPaneAlike implements IScriptSplitPaneMethods
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


	public boolean js_setLeftForm(Object form, Object relation)
	{
		return ((ISplitPane)getComponent()).setForm(true, form, relation);
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
		return ((ISplitPane)getComponent()).setForm(false, form, relation);
	}

	public boolean js_setRightForm(Object form)
	{
		return js_setRightForm(form, null);
	}

	public FormScope js_getLeftForm()
	{
		return ((ISplitPane)getComponent()).getForm(true);
	}

	public IForm getLeftForm()
	{
		FormScope left = js_getLeftForm();
		return left == null ? null : (FormController)left.getFormController();
	}

	public FormScope js_getRightForm()
	{
		return ((ISplitPane)getComponent()).getForm(false);
	}

	public IForm getRightForm()
	{
		FormScope right = js_getRightForm();
		return right == null ? null : (FormController)right.getFormController();
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
		((ISplitPane)getComponent()).setResizeWeight(resizeWeight);
		getChangesRecorder().setChanged();
	}

	public double getDividerLocation()
	{
		return ((ISplitPane)getComponent()).getDividerLocation();
	}

	public void setDividerLocation(double location)
	{
		((ISplitPane)getComponent()).setRuntimeDividerLocation(location);
	}

	public int getDividerSize()
	{
		return ((ISplitPane)getComponent()).getDividerSize();
	}

	public void setDividerSize(int size)
	{
		((ISplitPane)getComponent()).setDividerSize(size);
		getChangesRecorder().setChanged();
	}

	public double getResizeWeight()
	{
		return ((ISplitPane)getComponent()).getResizeWeight();
	}

	public boolean getContinuousLayout()
	{
		return ((ISplitPane)getComponent()).getContinuousLayout();
	}

	public void setContinuousLayout(boolean b)
	{
		((ISplitPane)getComponent()).setContinuousLayout(b);
		getChangesRecorder().setChanged();
	}

	public int getRightFormMinSize()
	{
		return ((ISplitPane)getComponent()).getFormMinSize(false);
	}

	public void setRightFormMinSize(int minSize)
	{
		((ISplitPane)getComponent()).setFormMinSize(false, minSize);
		getChangesRecorder().setChanged();
	}

	public int getLeftFormMinSize()
	{
		return ((ISplitPane)getComponent()).getFormMinSize(true);
	}

	public void setLeftFormMinSize(int minSize)
	{
		((ISplitPane)getComponent()).setFormMinSize(true, minSize);
		getChangesRecorder().setChanged();
	}
}
