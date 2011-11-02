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
package com.servoy.j2db.dnd;

import java.util.Arrays;

import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.JSBaseEvent;
import com.servoy.j2db.scripting.JSEvent.EventType;

/**
 * JSDNDEvent, used as argument to drag and drop callbacks.
 * 
 * @author gboros
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSDNDEvent extends JSBaseEvent
{
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
	 * Constant returned by JSEvent.getType() in a method that is attached to an onDragEnd event.
	 *
	 * @sample
	 * if (event.getType() == JSEvent.ONDRAGEND) 
	 * {
	 *    // its an on drag end event.
	 *    // return true if the drop has been completed successfully
	 *    return event.isDropSuccess(); 
	 * }
	 */
	public static final String ONDRAGEND = EventType.onDragEnd.toString();


	private int dragResult;
	private Record record;
	private String dataMimeType;

	/**
	 * Sets the result of the drag action.
	 * 
	 * @param dragResult a DRAGNDROP constant, representing the result of the drag action
	 */
	public void setDragResult(int dragResult)
	{
		this.dragResult = dragResult;
	}

	/**
	 * Returns the result of the drag action. 
	 *
	 * @sample
	 * function onDragEnd(event)
	 * {
	 * 		var dragResult = event.getDragResult();
	 * 		if(dragResult == DRAGNDROP.NONE)
	 * 		{
	 * 			// the drag was canceled
	 * 		}
	 * 		else if(dragResult == DRAGNDROP.COPY)
	 * 		{
	 * 			// the drag ended with a copy action
	 * 		}
	 * 		else if(dragResult == DRAGNDROP.MOVE)
	 * 		{
	 * 			// the drag ended with a move action
	 * 		}
	 * @return a DRAGNDROP constant, representing the result of the drag action
	 */
	public int js_getDragResult()
	{
		return this.dragResult;
	}

	/**
	 * Sets the record of the event.
	 * 
	 * @param record of the event
	 */
	public void setRecord(Record record)
	{
		this.record = record;
	}

	/**
	 * Returns the record of the event.
	 *
	 * @sample event.Record();
	 * 
	 * @return Record of the event 
	 */
	public Record js_getRecord()
	{
		return this.record;
	}

	/**
	 * The event data mime type.
	 * 
	 * @sample
	 * // only accept drag if data is a servoy record 
	 * function onDragOver(event)
	 * {
	 *		if(event.dataMimeType.indexOf("application/x-servoy-record-object") == 0) return true;
	 *		else return false;
	 * } 
	 */
	public String js_getDataMimeType()
	{
		return dataMimeType;
	}


	public void js_setDataMimeType(String mimeType)
	{
		dataMimeType = mimeType;
	}

	/**
	 * Returns the event data mime type.
	
	 * @return event data mime type
	 */
	public String getDataMimeType()
	{
		return dataMimeType;
	}

	/**
	 * Sets the event data mime type.
	 * 
	 * @param mimeType event data mime type
	 */
	public void setDataMimeType(String mimeType)
	{
		dataMimeType = mimeType;
	}

	/**
	 * String representation of the drag and drop event.
	 */
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
		return "JSDNDEvent(type = " + type + ", source = " + source + ", formName = " + formName + ", elementName = " + eName + ", timestamp = " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			timestamp +
			",modifiers = " + modifiers + ",x =" + x + ",y = " + y + ",data = " + dataToString + ",dataMimeType = " + dataMimeType + ",dragResult = " + dragResult + ')'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "JSDNDEvent"; //$NON-NLS-1$
	}
}
