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
public abstract class AbstractRuntimeButton extends AbstractRuntimeLabel
{
	protected IButton button;

	public AbstractRuntimeButton(IButton button, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(button, jsChangeRecorder, application);
		this.button = button;
	}

	@Override
	public void js_setBorder(String spec)
	{
		super.js_setBorder(spec);
		jsChangeRecorder.setSize(button.getSize().width, button.getSize().height, button.getBorder(), button.getMargin(), 0, true,
			button.getVerticalAlignment());
	}

	@Override
	public void js_putClientProperty(Object key, Object value)
	{
		if (button instanceof JComponent && "contentAreaFilled".equals(key) && value instanceof Boolean)
		{
			((AbstractButton)button).setContentAreaFilled(((Boolean)value).booleanValue());
		}
		else
		{
			super.js_putClientProperty(key, value);
		}

	}

	@Override
	public void js_setSize(int width, int height)
	{
		super.js_setSize(width, height);
		jsChangeRecorder.setSize(width, height, button.getBorder(), button.getMargin(), 0, true, button.getVerticalAlignment());
	}

	public void js_requestFocus(Object[] vargs)
	{
		button.requestFocus(vargs);
	}

	@Override
	public String js_getElementType()
	{
		return IScriptBaseMethods.BUTTON;
	}
}
