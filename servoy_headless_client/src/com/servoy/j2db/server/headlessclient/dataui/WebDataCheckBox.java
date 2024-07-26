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
package com.servoy.j2db.server.headlessclient.dataui;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.ui.scripting.RuntimeCheckbox;
import com.servoy.j2db.util.Utils;

/**
 * Represents a checkbox field in the webbrowser.
 *
 * @author jcompagner
 */
public class WebDataCheckBox extends WebBaseSelectBox implements IResolveObject
{

	public WebDataCheckBox(IApplication application, RuntimeCheckbox scriptable, String id, String text, IValueList list)
	{
		this(application, scriptable, id, text);
		onValue = list;
	}


	public WebDataCheckBox(IApplication application, RuntimeCheckbox scriptable, String id, String text)
	{
		super(application, scriptable, id, text);
	}

	public final RuntimeCheckbox getScriptObject()
	{
		return (RuntimeCheckbox)scriptable;
	}

	/*
	 * _____________________________________________________________ Methods for model object resolve
	 */
	public Object resolveDisplayValue(Object realVal)
	{
		if (onValue != null && onValue.getSize() >= 1)
		{
			Object real = onValue.getRealElementAt(0);
			if (real == null)
			{
				return Boolean.valueOf(realVal == null);
			}
			return Boolean.valueOf(Utils.equalObjects(real, realVal)); // not just direct equals cause for example it could happen that one is Long(1) and the other Integer(1) and it would be false
		}
		if (realVal instanceof Boolean) return realVal;
		if (realVal instanceof Number)
		{
			return Boolean.valueOf(((Number)realVal).intValue() >= 1);
		}
		return Boolean.valueOf(realVal != null && "1".equals(realVal.toString()));
	}

	public Object resolveRealValue(Object displayVal)
	{
		if (onValue != null && onValue.getSize() >= 1)
		{
			return (Utils.getAsBoolean(displayVal) ? onValue.getRealElementAt(0) : null);
		}
		else
		{
//	TODO this seems not possible in web and we don't have the previousRealValue
//				// if value == null and still nothing selected return null (no data change)
//				if (previousRealValue == null && !Utils.getAsBoolean(displayVal))
//				{
//					return null;
//				}
			return Integer.valueOf((Utils.getAsBoolean(displayVal) ? 1 : 0));
		}
	}

	public void setValueObject(Object value)
	{
		((ChangesRecorder)getStylePropertyChanges()).testChanged(this, value);
		if (getStylePropertyChanges().isChanged())
		{
			// this component is going to update it's contents, without the user changing the
			// components contents; so remove invalid state if necessary
			setValueValid(true, null);
		}
	}


	@Override
	public String toString()
	{
		return getScriptObject().toString("value:" + getValueObject()); //$NON-NLS-1$
	}


}
