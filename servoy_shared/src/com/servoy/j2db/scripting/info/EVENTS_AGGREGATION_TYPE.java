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
package com.servoy.j2db.scripting.info;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IConstantsObject;

/**
 * <p>Constants are defined to represent different aggregation types for return value of eventsManager.fireEventListeners. The constants include:</p>
 * <ul>
 *   <li><b>RETURN_VALUE_BOOLEAN</b>: fireEventListeners returns a logical and between all the listeners return values.</li>
 *   <li><b>RETURN_VALUE_ARRAY</b>: fireEventListeners returns an array with all the listeners return values.</li>
 * </ul>
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public enum EVENTS_AGGREGATION_TYPE implements IConstantsObject
{
	/**
	 * Constant for eventsManager fireEventListeners.
	 *
	 * @sample
	 * var myboolean = eventsManager.fireEventListeners('customEvent',null,null,EVENTS_AGGREGATION_TYPE.RETURN_VALUE_BOOLEAN);
	 *
	 */
	RETURN_VALUE_BOOLEAN,


	/**
	 * Constant for eventsManager fireEventListeners.
	 *
	 * @sample
	 * var myarray = eventsManager.fireEventListeners('customEvent',null,null,EVENTS_AGGREGATION_TYPE.RETURN_VALUE_ARRAY);
	 */
	RETURN_VALUE_ARRAY,
}
