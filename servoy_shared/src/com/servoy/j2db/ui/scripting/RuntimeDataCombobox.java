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

import java.awt.Dimension;

import javax.swing.JComponent;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;

/**
 * @author lvostinar
 * @since 6.0
 */
public class RuntimeDataCombobox extends AbstractRuntimeFormattedValuelistComponent implements IScriptDataComboboxMethods
{
	public RuntimeDataCombobox(IFieldComponent component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(component, jsChangeRecorder, application);
	}

	public String js_getElementType()
	{
		return IScriptBaseMethods.COMBOBOX;
	}

	@Override
	public void js_setSize(int x, int y)
	{
		if (component instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)component).setCachedSize(new Dimension(x, y));
		}
		component.setSize(new Dimension(x, y));
		if (component instanceof JComponent)
		{
			((JComponent)component).validate();
		}
		jsChangeRecorder.setSize(x, y, null, null, 0);
	}
}
