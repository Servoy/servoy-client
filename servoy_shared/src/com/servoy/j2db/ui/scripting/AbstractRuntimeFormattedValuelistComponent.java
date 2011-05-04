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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;

/**
 * @author lvostinar
 * @since 6.0
 *
 */
public abstract class AbstractRuntimeFormattedValuelistComponent extends AbstractRuntimeValuelistComponent
{
	public AbstractRuntimeFormattedValuelistComponent(IFieldComponent component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(component, jsChangeRecorder, application);
	}

	public void js_setFormat(String format)
	{
		component.setFormat(component.getDataType(), application.getI18NMessageIfPrefixed(format));
		jsChangeRecorder.setChanged();
	}

	public String js_getFormat()
	{
		return component.getFormat();
	}

	public boolean js_isEditable()
	{
		return component.isEditable();
	}

	public void js_setEditable(boolean b)
	{
		component.setEditable(b);
		jsChangeRecorder.setChanged();
	}
}
