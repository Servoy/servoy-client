/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import org.mozilla.javascript.annotations.JSFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;

/**
 * This class is a wrapper for the SLF4J logger.
 * It provides an API for logging with arguments, e.g. logger.info("my message and my {}", "argument");.
 * Available logging levels are: info, warn, debug, error and trace.
 *
 * @author jdejong
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "logger")
public class JSLogger
{
	private final Logger logger;


	public JSLogger()
	{
		this.logger = LoggerFactory.getLogger(Debug.class);
	}

	public JSLogger(String loggerName)
	{
		this.logger = LoggerFactory.getLogger(loggerName);
	}

	// INFO
	@JSFunction
	public void info(String message)
	{
		logger.info(message);
	}

	@JSFunction
	public void info(String message, Object arg1)
	{
		logger.info(message, arg1);
	}

	@JSFunction
	public void info(String message, Object arg1, Object arg2)
	{
		logger.info(message, arg1, arg2);
	}

	@JSFunction
	public void info(String message, Object... args)
	{
		logger.info(message, args);
	}

	@JSFunction
	public void info(String message, Throwable arg1)
	{
		logger.info(message, arg1);
	}

	// WARN
	@JSFunction
	public void warn(String message)
	{
		logger.warn(message);
	}

	@JSFunction
	public void warn(String message, Object arg1)
	{
		logger.warn(message, arg1);
	}

	@JSFunction
	public void warn(String message, Object arg1, Object arg2)
	{
		logger.warn(message, arg1, arg2);
	}

	@JSFunction
	public void warn(String message, Object... args)
	{
		logger.warn(message, args);
	}

	@JSFunction
	public void warn(String message, Throwable arg1)
	{
		logger.warn(message, arg1);
	}

	// ERROR
	@JSFunction
	public void error(String message)
	{
		logger.error(message);
	}

	@JSFunction
	public void error(String message, Object arg1)
	{
		logger.error(message, arg1);
	}

	@JSFunction
	public void error(String message, Object arg1, Object arg2)
	{
		logger.error(message, arg1, arg2);
	}

	@JSFunction
	public void error(String message, Object... args)
	{
		logger.error(message, args);
	}

	@JSFunction
	public void error(String message, Throwable arg1)
	{
		logger.error(message, arg1);
	}

	// DEBUG
	@JSFunction
	public void debug(String message)
	{
		logger.debug(message);
	}

	@JSFunction
	public void debug(String message, Object arg1)
	{
		logger.debug(message, arg1);
	}

	@JSFunction
	public void debug(String message, Object arg1, Object arg2)
	{
		logger.debug(message, arg1, arg2);
	}

	@JSFunction
	public void debug(String message, Object... args)
	{
		logger.debug(message, args);
	}

	@JSFunction
	public void debug(String message, Throwable arg1)
	{
		logger.debug(message, arg1);
	}

	//TRACE
	@JSFunction
	public void trace(String message)
	{
		logger.trace(message);
	}

	@JSFunction
	public void trace(String message, Object arg1)
	{
		logger.trace(message, arg1);
	}

	@JSFunction
	public void trace(String message, Object arg1, Object arg2)
	{
		logger.trace(message, arg1, arg2);
	}

	@JSFunction
	public void trace(String message, Object... args)
	{
		logger.trace(message, args);
	}

	@JSFunction
	public void trace(String message, Throwable arg1)
	{
		logger.trace(message, arg1);
	}

	// IS LEVEL ENABLED?
	@JSFunction
	public boolean isInfoEnabled()
	{
		return logger.isInfoEnabled();
	}

	@JSFunction
	public boolean isWarnEnabled()
	{
		return logger.isWarnEnabled();
	}

	@JSFunction
	public boolean isErrorEnabled()
	{
		return logger.isErrorEnabled();
	}

	@JSFunction
	public boolean isDebugEnabled()
	{
		return logger.isDebugEnabled();
	}

	@JSFunction
	public boolean isTraceEnabled()
	{
		return logger.isTraceEnabled();
	}
}
