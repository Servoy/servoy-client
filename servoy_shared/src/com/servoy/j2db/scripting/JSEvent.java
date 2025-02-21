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
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * The <code>JSEvent</code> object serves as the primary argument for user-event callbacks, encapsulating key details about
 * application-triggered events. It provides information such as event type, source element or form, position, and any
 * associated data, enabling developers to handle interactions dynamically and efficiently. Constants like <code>ACTION</code>,
 * <code>DATACHANGE</code>, and <code>DOUBLECLICK</code> help identify specific event types, while methods such as
 * <code>getType()</code>, <code>getSource()</code>, and <code>getElementName()</code> give precise context for each event.
 *
 * <p>
 * In addition to identifying event origins and types, the object supports positional data with methods like <code>getX()</code>
 * and <code>getY()</code>, and tracks the timing of occurrences using <code>getTimestamp()</code>. It also features a
 * <code>data</code> property to carry event-specific payloads, enhancing customization options. This makes <code>JSEvent</code>
 * a flexible and powerful tool for implementing responsive, user-driven functionality in applications.
 * </p>
 *
 * @author rgansevles
 *
 * @since 5.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSEvent")
public class JSEvent extends JSBaseEvent
{
	private boolean propagationStopped = false;

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
		return "JSEvent(type = " + type + ", source = " + ((source instanceof Wrapper) ? ((Wrapper)source).unwrap() : source) + ", formName = " + formName + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			", elementName = " + eName + ", timestamp = " + //$NON-NLS-1$ //$NON-NLS-2$
			timestamp + ",modifiers = " + modifiers + ",x =" + x + ",y = " + y + ",data = " + dataToString + ')'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "JSEvent"; //$NON-NLS-1$
	}

	/**
	 * stopPropagation is used in case of multiple event listeners (added via application.addEventListener). When application.fireEventListeners is called you can use this api to stop executing further listeners.
	 *
	 * @sample event.stopPropagation()
	 *
	 */
	@JSFunction
	@ServoyClientSupport(ng = true, wc = true, sc = true, mc = false)
	public void stopPropagation()
	{
		propagationStopped = true;
	}

	public boolean isPropagationStopped()
	{
		return propagationStopped;
	}
}
