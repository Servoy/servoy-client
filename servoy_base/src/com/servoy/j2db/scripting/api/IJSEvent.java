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
package com.servoy.j2db.scripting.api;

import java.awt.Event;
import java.util.Date;

import com.servoy.j2db.scripting.annotations.ServoyMobile;

/**
 * 
 * @author jcompagner
 * @since 7.0
 */
@ServoyMobile
public interface IJSEvent
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

	/**
	 * returns the event type see the JSEvents constants what it can return.
	 * Plugins can create events with there own types.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ACTION) 
	 * {
	 * 	// its an action event.
	 * }	
	 * 
	 * @return a String representing the type of this event.
	 */
	public String getType();

	/**
	 * Returns the time the event occurred.
	 *
	 * @sample event.getTimestamp();
	 * 
	 * @return a Date when this event happened.
	 */
	public Date getTimestamp();

	/**
	 * returns the source component/element of the event.
	 * If it has a name the getElementName() is the name of this component.
	 *
	 * @sample
	 * // cast to runtime text field (change to anoter kind of type if you know the type)
	 * /** @type {RuntimeTextField} *&#47;
	 * var source = event.getSource();
	 * var sourceDataProvider = source.getDataProviderID();
	 * 
	 * @return an Object representing the source of this event.
	 */
	public Object getSource();

	/**
	 * returns the name of the form the element was placed on.
	 *
	 * @sample
	 * forms[event.getFormName()].myFormMethod();
	 * 
	 * @return a String representing the form name.
	 */
	public String getFormName();

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
	public String getElementName();

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
	public int getModifiers();

	/**
	 * Returns the x position of the event, relative to the component that fired it, if applicable.
	 * For example drag'n'drop events will set the x,y positions.
	 * 
	 * @sample
	 * var x = event.getX();
	 * var xPrevious = previousEvent.getX();
	 * var movedXPixels = x -xPrevious;
	 * 
	 * @return an int representing the X position.
	 */
	public int getX();

	/**
	 * Returns the y position of the event, relative to the component that fired it, if applicable.
	 * For example drag'n'drop events will set the x,y positions.
	 * 
	 * @sample
	 * var y = event.getY();
	 * var yPrevious = previousEvent.getY();
	 * var movedYPixels = y -yPrevious;
	 * 
	 * @return an int representing the Y position.
	 */
	public int getY();

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
	public Object getData();

	public void setData(Object object);

}
