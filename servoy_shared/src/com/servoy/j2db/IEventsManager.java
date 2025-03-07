/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db;

import org.mozilla.javascript.Function;

import com.servoy.j2db.scripting.info.EVENTS_AGGREGATION_TYPE;
import com.servoy.j2db.scripting.info.EventType;

/**
 * @author lvostinar
 *
 */
public interface IEventsManager
{

	/**
	 * @param callback
	 * @param eventType
	 * @param context
	 */
	void addListener(EventType eventType, Function callback, String context);

	/**
	 * @param eventType
	 * @param context
	 * @param callback
	 */
	void removeListener(EventType eventType, Function callback, String context);

	/**
	 * @param eventType
	 * @param context
	 * @return
	 */
	boolean hasListeners(EventType eventType, String context);

	/**
	 * @param eventType
	 * @param context
	 * @param callbackArguments
	 * @param returnValueAggregationType
	 * @return
	 */
	Object fireListeners(EventType eventType, String context, Object[] callbackArguments, EVENTS_AGGREGATION_TYPE returnValueAggregationType);

}
