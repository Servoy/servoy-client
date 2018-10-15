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

import java.awt.Point;
import java.util.Date;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.api.IJSEvent;
import com.servoy.j2db.component.ComponentFactory;

/**
 * JSBaseEvent base class for js event objects
 *
 * @author gboros
 *
 * @since 6.1
 */
public class JSBaseEvent implements IConstantsObject, IJSEvent
{
	protected String type = IJSEvent.NONE;
	protected String name;
	protected Date timestamp;
	protected Object source;
	protected String formName;
	protected String elementName;
	protected int modifiers;
	protected Object data;
	protected int x;
	protected int y;
	protected Point absoluteLocation;

	public JSBaseEvent()
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
	 * 	// its an action event.
	 * }
	 *
	 * @return a String representing the type of this event.
	 */
	@JSFunction
	public String getType()
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
	@JSFunction
	public Date getTimestamp()
	{
		return timestamp;
	}

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
	@JSFunction
	public Object getSource()
	{
		if (source instanceof IScriptableProvider) return ((IScriptableProvider)source).getScriptObject();
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
	@JSFunction
	public String getFormName()
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
	@JSFunction
	public String getElementName()
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
	@JSFunction
	public int getModifiers()
	{
		return modifiers;
	}

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
	@JSFunction
	public int getX()
	{
		return x;
	}

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
	@JSFunction
	public int getY()
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
	 * elements[event.getElementName()].setText(data);
	 * return true;
	 *
	 */
	@JSGetter
	public Object getData()
	{
		return data;
	}

	@JSSetter
	public void setData(Object object)
	{
		this.data = object;
	}

	/**
	 * Returns the name of the event which was triggered
	 *
	 * @sample
	 * var name = event.getName();
	 *
	 * @return name of event as string
	 */
	@JSFunction
	public String getName()
	{
		if (name != null)
		{
			return name;
		}
		return getType();
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setType(JSEvent.EventType type)
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

	/**
	 * @return the absoluteLocation
	 */
	public Point getAbsoluteLocation()
	{
		return absoluteLocation;
	}

	/**
	 * @param absoluteLocation the absoluteLocation to set
	 */
	public void setAbsoluteLocation(Point absoluteLocation)
	{
		this.absoluteLocation = absoluteLocation;
	}

}
