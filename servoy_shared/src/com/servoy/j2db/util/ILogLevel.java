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
package com.servoy.j2db.util;

/***
 * Logging levels that can be passed as parameter to application.output javascript function
 *
 *  @author lvostinar
 */
public interface ILogLevel
{
	// log4j logging levels

	/**
	 * Logging level.
	 *
	 * @sample application.output('my message',APP_LOG_LEVEL.DEBUG)
	 */
	public static final int DEBUG = 0;

	/**
	 * Logging level.
	 *
	 * @sample application.output('my message',APP_LOG_LEVEL.INFO)
	 */
	public static final int INFO = 1;

	/**
	 * Logging level.
	 *
	 * @sample application.output('my message',APP_LOG_LEVEL.WARNING)
	 */
	public static final int WARNING = 2;

	/**
	 * Logging level.
	 *
	 * @sample application.output('my message',APP_LOG_LEVEL.ERROR)
	 */
	public static final int ERROR = 3;

	/**
	 * Logging level. This level will for the most part be mapped on ERROR, because of java loggign api's
	 *
	 * @sample application.output('my message',APP_LOG_LEVEL.FATAL)
	 */
	public static final int FATAL = 4;
}
