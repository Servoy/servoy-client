/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.JSEvent;

/**
 * Exception sent when validation has failed.
 * 
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class ValidationFailedException extends ApplicationException
{
	private Object oldValue;
	private Object newValue;
	private JSEvent event;

	public ValidationFailedException(int errorCode, Object oldValue, Object newValue, JSEvent event)
	{
		super(errorCode);
		initValues(oldValue, newValue, event);
	}

	public ValidationFailedException(int errorCode, Exception ex, Object oldValue, Object newValue, JSEvent event)
	{
		this(errorCode, oldValue, newValue, event);
		initCause(ex);
	}

	public ValidationFailedException(int errorCode, Object[] values, Object oldValue, Object newValue, JSEvent event)
	{
		super(errorCode, values);
		initValues(oldValue, newValue, event);
	}

	private void initValues(Object oldValue, Object newValue, JSEvent event)
	{
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.event = event;
	}

	/**
	 * Old value for the element.
	 * 
	 * @sampleas com.servoy.j2db.util.ServoyException#js_getErrorCode()
	 */
	public Object js_getOldValue()
	{
		return oldValue;
	}

	/**
	 * New value for the element(that causes failed validation).
	 * 
	 * @sampleas com.servoy.j2db.util.ServoyException#js_getErrorCode()
	 */
	public Object js_getNewValue()
	{
		return newValue;
	}

	/**
	 * Validation failed event(contains user interface trigger). 
	 * 
	 * @sampleas com.servoy.j2db.util.ServoyException#js_getErrorCode()
	 */
	public JSEvent js_getEvent()
	{
		return event;
	}

}
