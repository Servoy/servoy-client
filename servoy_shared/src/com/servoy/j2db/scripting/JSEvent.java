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
import java.awt.Point;
import java.util.Arrays;
import java.util.Date;

import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * JSEvent, used as first argument to user-event callbacks.
 * 
 * @author rgansevles
 * 
 * @since 5.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSEvent implements IPrefixedConstantsObject
{
	public enum EventType
	{
		action, focusGained, focusLost, doubleClick, rightClick, onDrag, onDrop, onDragOver, form, dataChange, none
	}

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onAction event. 
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ACTION) 
	 * {
	 *    // its an action event.
	 * }
	 */
	public static final String ACTION = EventType.action.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onFocusGained or the forms onElementFocusGained event.
	 *
	 * @sample 
	 * if (event.getType() == JSEvent.FOCUSGAINED) 
	 * {
	 *    // its a focus gained event.
	 * }
	 */
	public static final String FOCUSGAINED = EventType.focusGained.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onFocusLost or the forms onElementFocusLost event.
	 *
	 * @sample 
	 * if (event.getType() == JSEvent.FOCUSLOST) 
	 * {
	 *    // its a focus lost event.
	 * }
	 */
	public static final String FOCUSLOST = EventType.focusLost.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDoubleClick event.
	 *
	 * @sample 
	 * if (event.getType() == JSEvent.DOUBLECLICK) 
	 * {
	 *    // its a double click event.
	 * }
	 */
	public static final String DOUBLECLICK = EventType.doubleClick.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onRightClick event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.RIGHTCLICK) 
	 * {
	 *    // its a right click event.
	 * }
	 */
	public static final String RIGHTCLICK = EventType.rightClick.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDrag event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ONDRAG) 
	 * {
	 *    // its an ondrag event
	 *    if (event.getElementName() == 'todragelement')
	 *    	return DRAGNDROP.COPY
	 * }
	 */
	public static final String ONDRAG = EventType.onDrag.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDrop event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ONDROP) 
	 * {
	 *    // its a on drop event.
	 *    var element = elements[event.getElementName()];
	 *    // do drop on element
	 *    return true;
	 * }
	 */
	public static final String ONDROP = EventType.onDrop.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDragOver event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ONDRAGOVER) 
	 * {
	 *    // its an on drag over event.
	 *    // return true if it over the right element.
	 *    return event.getElementName() == 'candroponelement';
	 * }
	 */
	public static final String ONDRAGOVER = EventType.onDragOver.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDataChange event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.DATACHANGE) 
	 * {
	 *    // its a data change event
	 * }
	 */
	public static final String DATACHANGE = EventType.dataChange.toString();

	/**
	 * Constant returned by JSEvent.getType() in a method that is attached to a form event (like onShow) or command (like onDeleteRecord)
	 *
	 * @sample
	 * if (event.getType() == JSEvent.FORM) 
	 * {
	 *    // its a form event or command
	 * }
	 */
	public static final String FORM = EventType.form.toString();

	/**
	 * Constant returned by JSEvent.getType() if the event is not used in a known event or command.
	 * @sample
	 * if (event.getType() == JSEvent.NONE) 
	 * {
	 *    // type is not set.
	 * }
	 */
	public static final String NONE = EventType.none.toString();

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

	private String type = NONE;
	private Date timestamp;
	private Object source;
	private String formName;
	private String elementName;
	private int modifiers;
	private Object data;
	private int x;
	private int y;

	public JSEvent()
	{
		timestamp = new Date();
	}

	/**
	 * returns the event type see the JSEvents constants what it can return.
	 * Plugins can create events with there own types.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ACTION) 
	 * {
	 *    // its an action event.
	 * }	
	 * 
	 * @return a String representing the type of this event.
	 */
	public String js_getType()
	{
		return type;
	}

	/**
	 * Returns the time the event occurred.
	 *
	 * @sample event.getTimestamp();
	 * 
	 * @return a Date when this event happened.
	 */
	public Date js_getTimestamp()
	{
		return timestamp;
	}

	/**
	 * returns the source component/element of the event.
	 * If it has a name the getElementName() is the name of this component.
	 *
	 * @sample
	 * var sourceDataProvider = event.getSource().getDataProviderID();
	 * 
	 * @return an Object representing the source of this event.
	 */
	public Object js_getSource()
	{
		return source;
	}

	/**
	 * returns the name of the form the element was placed on.
	 *
	 * @sample
	 * forms[event.getFormName()].myFormMethod();
	 * 
	 * @return a String representing the form name.
	 */
	public String js_getFormName()
	{
		return formName;
	}

	/**
	 * returns the name of the element, can be null if the form was the source of the event. 
	 * 
	 * @sample
	 * if (event.getElementName() == 'myElement')
	 * {
	 *     elements[event.getElementName()].bgcolor = '#ff0000';
	 * }
	 * 
	 * @return a String representing the element name.
	 */
	public String js_getElementName()
	{
		return elementName;
	}

	/**
	 * Returns the modifiers of the event, see JSEvent.MODIFIER_XXXX for the modifiers that can be returned.
	 *
	 * @sample
	 * //test if the SHIFT modifier is used.
	 * if (event.getModifiers() & JSEvent.MODIFIER_SHIFT)
	 * {
	 * 	//do shift action
	 * }
	 * 
	 * @return an int which holds the modifiers as a bitset.
	 */
	public int js_getModifiers()
	{
		return modifiers;
	}

	/**
	 * Returns the x position of the event if applicable.
	 * For example drag'n'drop events will set the x,y positions.
	 * 
	 * @sample
	 * var x = event.getX();
	 * var xPrevious = previousEvent.getX();
	 * var movedXPixels = x -xPrevious;
	 * 
	 * @return an int representing the X position.
	 */
	public int js_getX()
	{
		return x;
	}

	/**
	 * Returns the x position of the event if applicable.
	 * For example drag'n'drop events will set the x,y positions.
	 * 
	 * @sample
	 * var y = event.getY();
	 * var yPrevious = previousEvent.getY();
	 * var movedYPixels = y -yPrevious;
	 * 
	 * @return an int representing the Y position.
	 */
	public int js_getY()
	{
		return y;
	}

	/**
	 * A data object that specific events can set, a user can set data back to the system for events that supports this.
	 *
	 * @sample
	 * // A client design method that handles ondrag
	 * if (event.getType() == JSEvent.ONDRAG)
	 * {
	 *      // the data is the selected elements array
	 *      var elements = event.data;
	 *      // only start a client design drag when there is 1 element
	 *      if (elements.length == 1)
	 *      {
	 *      	return true;
	 *      }
	 * }
	 * 
	 * // code for a data drag method
	 * event.data = "drag me!";
	 * return DRAGNDROP.COPY;
	 * 
	 * // code for a data drop method
	 * var data = event.data;
	 * elemements[event.getElementName()].setText(data);
	 * return true;
	 * 
	 */
	public Object js_getData()
	{
		return data;
	}

	public void js_setData(Object object)
	{
		this.data = object;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public void setType(EventType type)
	{
		this.type = type == null ? null : type.toString();
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}

	public void setSource(Object source)
	{
		this.source = source;
	}

	public void setFormName(String formName)
	{
		this.formName = formName;
	}

	public void setElementName(String elementName)
	{
		if (elementName != null && !elementName.startsWith(ComponentFactory.WEB_ID_PREFIX))
		{
			this.elementName = elementName;
		}
		else
		{
			this.elementName = null;
		}
	}

	public void setModifiers(int modifiers)
	{
		this.modifiers = modifiers;
	}

	public void setLocation(Point point)
	{
		this.x = point.x;
		this.y = point.y;
	}

	public Object getData()
	{
		return data;
	}

	public void setData(Object object)
	{
		this.data = object;
	}

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
		return "JSEvent(type = " + type + ", source = " + source + ", formName = " + formName + ", elementName = " + eName + ", timestamp = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
