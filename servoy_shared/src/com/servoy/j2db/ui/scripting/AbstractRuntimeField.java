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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptFocusMethods;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportSpecialClientProperty;
import com.servoy.j2db.util.IDelegate;

/**
 * Abstract scriptable field.
 * 
 * @author lvostinar
 */
public abstract class AbstractRuntimeField<C extends IFieldComponent> extends AbstractRuntimeBaseComponent<C> implements IScriptRenderMethods,
	IScriptFocusMethods, IScriptReadOnlyMethods
{
	public AbstractRuntimeField(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String js_getDataProviderID()
	{
		return getComponent() instanceof IDisplayData ? ((IDisplayData)getComponent()).getDataProviderID() : null;
	}

	public void js_setToolTipText(String txt)
	{
		getComponent().setToolTipText(txt);
		getChangesRecorder().setChanged();
	}

	public String js_getToolTipText()
	{
		return getComponent().getToolTipText();
	}

	public String[] js_getLabelForElementNames()
	{
		List<ILabel> labels = getComponent().getLabelsFor();
		if (labels != null)
		{
			ArrayList<String> al = new ArrayList<String>(labels.size());
			for (int i = 0; i < labels.size(); i++)
			{
				ILabel label = labels.get(i);
				if (label.getName() != null && !"".equals(label.getName()) && !label.getName().startsWith(ComponentFactory.WEB_ID_PREFIX)) //$NON-NLS-1$
				{
					al.add(label.getName());
				}
			}
			return al.toArray(new String[al.size()]);
		}
		return new String[0];
	}

	public String js_getTitleText()
	{
		return getComponent().getTitleText();
	}

	public int js_getAbsoluteFormLocationY()
	{
		return getComponent().getAbsoluteFormLocationY();
	}

	public void js_requestFocus(Object[] vargs)
	{
		getComponent().requestFocus(vargs);
	}

	public void js_setReadOnly(boolean b)
	{
		getComponent().setReadOnly(b);
		getChangesRecorder().setChanged();
	}

	public boolean js_isReadOnly()
	{
		return getComponent() instanceof IDisplay && ((IDisplay)getComponent()).isReadOnly();
	}

	@Override
	public void js_setVisible(boolean b)
	{
		super.js_setVisible(b);
		if (getComponent().isViewable())
		{
			List<ILabel> labels = getComponent().getLabelsFor();
			if (labels != null)
			{
				for (int i = 0; i < labels.size(); i++)
				{
					ILabel label = labels.get(i);
					IScriptable scriptable = label.getScriptObject();
					if (scriptable instanceof IScriptBaseMethods)
					{
						((IScriptBaseMethods)scriptable).js_setVisible(b);
					}
					else
					{
						label.setComponentVisible(b);
					}
				}
			}
		}
	}

	@Override
	public void js_putClientProperty(Object key, Object value)
	{
		super.js_putClientProperty(key, value);
		if (getComponent() instanceof IDelegate && ((IDelegate)getComponent()).getDelegate() instanceof JComponent)
		{
			((JComponent)((IDelegate)getComponent()).getDelegate()).putClientProperty(key, value);
		}
		if (getComponent() instanceof ISupportSpecialClientProperty)
		{
			((ISupportSpecialClientProperty)getComponent()).setClientProperty(key, value);
		}
	}

	@Override
	public void js_setSize(int x, int y)
	{
		super.js_setSize(x, y);
		getChangesRecorder().setSize(x, y, getComponent().getBorder(), getComponent().getMargin(), 0);
	}

	public void setEditableInFindMode()
	{
		if (!Boolean.TRUE.equals(application.getUIProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
		{
			getComponent().setEditable(true);
		}
	}
}
