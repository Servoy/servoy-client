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

import javax.swing.AbstractButton;
import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IButton;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;

/**
 * Abstract scriptable button.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeButton<C extends IButton> extends AbstractRuntimeLabel<C>
{

	public AbstractRuntimeButton(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	@Override
	public void js_setBorder(String spec)
	{
		super.js_setBorder(spec);
		getChangesRecorder().setSize(getComponent().getSize().width, getComponent().getSize().height, getComponent().getBorder(), getComponent().getMargin(),
			0, true, getComponent().getVerticalAlignment());
	}

	@Override
	public void js_putClientProperty(Object key, Object value)
	{
		if (getComponent() instanceof JComponent && "contentAreaFilled".equals(key) && value instanceof Boolean)
		{
			((AbstractButton)getComponent()).setContentAreaFilled(((Boolean)value).booleanValue());
		}
		else
		{
			super.js_putClientProperty(key, value);
		}

	}

	public void js_setSize(int width, int height)
	{
		setComponentSize(width, height);
		getChangesRecorder().setSize(width, height, getComponent().getBorder(), getComponent().getMargin(), 0, true, getComponent().getVerticalAlignment());
	}

	public void js_requestFocus(Object[] vargs)
	{
		getComponent().requestFocus(vargs);
	}

	@Override
	public String js_getElementType()
	{
		return IScriptBaseMethods.BUTTON;
	}
}
