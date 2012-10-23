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

import java.awt.Event;
import java.util.Arrays;

import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.api.IJSEvent;

/**
 * JSEvent, used as first argument to user-event callbacks.
 * 
 * @author rgansevles
 * 
 * @since 5.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSEvent extends JSBaseEvent implements IJSEvent
{
	public enum EventType
	{
		action, focusGained, focusLost, doubleClick, rightClick, onDrag, onDrop, onDragOver, onDragEnd, form, dataChange, none
	}

	/**
	 * Constant returned by JSEvent.getType() if the event is not used in a known event or command.
	 * @sample
	 * if (event.getType() == JSEvent.NONE) 
	 * {
	 * 	// type is not set.
	 * }
	 */
	public static final String NONE = EventType.none.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onAction event. 
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ACTION) 
	 * {
	 * 	// its an action event.
	 * }
	 */
	public static final String ACTION = EventType.action.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onFocusGained or the forms onElementFocusGained event.
	 *
	 * @sample 
	 * if (event.getType() == JSEvent.FOCUSGAINED) 
	 * {
	 * 	// its a focus gained event.
	 * }
	 */
	public static final String FOCUSGAINED = EventType.focusGained.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onFocusLost or the forms onElementFocusLost event.
	 *
	 * @sample 
	 * if (event.getType() == JSEvent.FOCUSLOST) 
	 * {
	 * 	// its a focus lost event.
	 * }
	 */
	public static final String FOCUSLOST = EventType.focusLost.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDoubleClick event.
	 *
	 * @sample 
	 * if (event.getType() == JSEvent.DOUBLECLICK) 
	 * {
	 * 	// its a double click event.
	 * }
	 */
	public static final String DOUBLECLICK = EventType.doubleClick.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onRightClick event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.RIGHTCLICK) 
	 * {
	 * 	// its a right click event.
	 * }
	 */
	public static final String RIGHTCLICK = EventType.rightClick.toString();


	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDataChange event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.DATACHANGE) 
	 * {
	 * 	// its a data change event
	 * }
	 */
	public static final String DATACHANGE = EventType.dataChange.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to a form event (like onShow) or command (like onDeleteRecord)
	 *
	 * @sample
	 * if (event.getType() == JSEvent.FORM) 
	 * {
	 * 	// its a form event or command
	 * }
	 */
	public static final String FORM = EventType.form.toString();

	/**
	 * Constant for the SHIFT modifier that can be returned by JSEvent.getModifiers();
	 * 
	 * @sampleas js_getModifiers()
	 * 
	 * @see com.servoy.j2db.scripting.JSEvent#js_getModifiers()
	 */
	public static final int MODIFIER_SHIFT = Event.SHIFT_MASK;

	/**
	 * Constant for the CTRL modifier that can be returned by JSEvent.getModifiers();
	 * 
	 * @sampleas js_getModifiers()
	 * 
	 * @see com.servoy.j2db.scripting.JSEvent#js_getModifiers()
	 */
	public static final int MODIFIER_CTRL = Event.CTRL_MASK;

	/**
	 * Constant for the META modifier that can be returned by JSEvent.getModifiers();
	 * 
	 * @sampleas js_getModifiers()
	 * 
	 * @see com.servoy.j2db.scripting.JSEvent#js_getModifiers()
	 */
	public static final int MODIFIER_META = Event.META_MASK;

	/**
	 * Constant for the ALT modifier that can be returned by JSEvent.getModifiers();
	 * 
	 * @sampleas js_getModifiers()
	 * 
	 * @see com.servoy.j2db.scripting.JSEvent#js_getModifiers()
	 */
	public static final int MODIFIER_ALT = Event.ALT_MASK;

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
