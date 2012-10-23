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
package com.servoy.j2db.scripting;

import java.util.Arrays;

import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * JSEvent, used as first argument to user-event callbacks.
 * 
 * @author rgansevles
 * 
 * @since 5.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSEvent extends JSBaseEvent
{
	@Override
	public String toString()
	{
		Object dataToString = data;
		if (dataToString == this) dataToString = "this"; //$NON-NLS-1$
		if (data != null && data.getClass().isArray() && !data.getClass().getComponentType().isPrimitive())
		{
			dataToString = Arrays.toString((Object[])data);
		}
		String eName = elementName;
		if (eName == null && source != null)
		{
			eName = "<no name>"; //$NON-NLS-1$
		}
		return "JSEvent(type = " + type + ", source = " + ((source instanceof Wrapper) ? ((Wrapper)source).unwrap() : source) + ", formName = " + formName + ", elementName = " + eName + ", timestamp = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			timestamp + ",modifiers = " + modifiers + ",x =" + x + ",y = " + y + ",data = " + dataToString + ')'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "JSEvent"; //$NON-NLS-1$
	}
}
