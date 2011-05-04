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

import java.awt.Rectangle;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptMediaInputFieldMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;

/**
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeMediaField extends AbstractRuntimeField implements IScriptMediaInputFieldMethods
{
	private final JComponent jComponent;

	public RuntimeMediaField(IFieldComponent component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application, JComponent jComponent)
	{
		super(component, jsChangeRecorder, application);
		this.jComponent = jComponent;
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.IMAGE_MEDIA;
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

	public boolean js_isEditable()
	{
		return component.isEditable();
	}
}
