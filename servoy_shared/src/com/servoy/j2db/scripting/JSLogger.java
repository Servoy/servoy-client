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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;

/**
 * This class is a wrapper for the Log4j logger.
 * It provides an API for logging with arguments, e.g. log.info("my message and my {}", "argument");.
 * This class can also be used to obtain JSLogBuilder instances.
 * Available logging levels are (in order): fatal, error, warn, info, debug and trace.
 *
 * @author jdejong
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLogger")
public class JSLogger
{
	private final Logger logger;


	public JSLogger()
	{
		this.logger = LogManager.getLogger(Debug.class);
	}

	public JSLogger(String loggerName)
	{
		this.logger = LogManager.getLogger(loggerName);
	}

	// INFO
	/**
	 * Log a message without arguments on the 'info' level,
	 * so it only writes if the logger's level is set to trace, debug or info.
	 *
	 * @param message the message to write to the log
	 */
	@JSFunction
	public void info(String message)
	{
		logger.info(message);
	}

	/**
	 * Log a message with an argument on the 'info' level,
	 * so it only writes if the logger's level is set to trace, debug or info.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 */
	@JSFunction
	public void info(String message, Object arg1)
	{
		logger.info(message, arg1);
	}

	/**
	 * Log a message with two arguments on the 'info' level,
	 * so it only writes if the logger's level is set to trace, debug or info.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 * @param arg2
	 */
	@JSFunction
	public void info(String message, Object arg1, Object arg2)
	{
		logger.info(message, arg1, arg2);
	}

	/**
	 * Log a message with multiple arguments on the 'info' level,
	 * so it only writes if the logger's level is set to trace, debug or info.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param args
	 */
	@JSFunction
	public void info(String message, Object... args)
	{
		logger.info(message, args);
	}

	/**
	 * Log a message with a Throwable argument on the 'info' level,
	 * so it only writes if the logger's level is set to trace, debug or info.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1 a Throwable
	 */
	@JSFunction
	public void info(String message, Throwable arg1)
	{
		logger.info(message, arg1);
	}

	// WARN
	/**
	 * Log a message without arguments on the 'warn' level,
	 * so it only writes if the logger's level is set to trace, debug, info or warn.
	 *
	 * @param message the message to write to the log
	 */
	@JSFunction
	public void warn(String message)
	{
		logger.warn(message);
	}

	/**
	 * Log a message with an argument on the 'warn' level,
	 * so it only writes if the logger's level is set to trace, debug, info or warn.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 */
	@JSFunction
	public void warn(String message, Object arg1)
	{
		logger.warn(message, arg1);
	}

	/**
	 * Log a message with two arguments on the 'warn' level,
	 * so it only writes if the logger's level is set to trace, debug, info or warn.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 * @param arg2
	 */
	@JSFunction
	public void warn(String message, Object arg1, Object arg2)
	{
		logger.warn(message, arg1, arg2);
	}

	/**
	 * Log a message with multiple arguments on the 'warn' level,
	 * so it only writes if the logger's level is set to trace, debug, info or warn.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param args
	 */
	@JSFunction
	public void warn(String message, Object... args)
	{
		logger.warn(message, args);
	}

	/**
	 * Log a message with a Throwable argument on the 'warn' level,
	 * so it only writes if the logger's level is set to trace, debug, info or warn.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1 a Throwable
	 */
	@JSFunction
	public void warn(String message, Throwable arg1)
	{
		logger.warn(message, arg1);
	}

	// ERROR
	/**
	 * Log a message without arguments on the 'error' level,
	 * so it only writes if the logger's level is set to trace, debug, info, warn or error.
	 *
	 * @param message the message to write to the log
	 */
	@JSFunction
	public void error(String message)
	{
		logger.error(message);
	}

	/**
	 * Log a message with an argument on the 'error' level,
	 * so it only writes if the logger's level is set to trace, debug, info, warn or error.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 */
	@JSFunction
	public void error(String message, Object arg1)
	{
		logger.error(message, arg1);
	}

	/**
	 * Log a message with two arguments on the 'error' level,
	 * so it only writes if the logger's level is set to trace, debug, info, warn or error.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 * @param arg2
	 */
	@JSFunction
	public void error(String message, Object arg1, Object arg2)
	{
		logger.error(message, arg1, arg2);
	}

	/**
	 * Log a message with multiple arguments on the 'error' level,
	 * so it only writes if the logger's level is set to trace, debug, info, warn or error.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param args
	 */
	@JSFunction
	public void error(String message, Object... args)
	{
		logger.error(message, args);
	}

	/**
	 * Log a message with a Throwable argument on the 'error' level,
	 * so it only writes if the logger's level is set to trace, debug, info, warn or error.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1 a Throwable
	 */
	@JSFunction
	public void error(String message, Throwable arg1)
	{
		logger.error(message, arg1);
	}

	// DEBUG
	/**
	 * Log a message without arguments on the 'debug' level,
	 * so it only writes if the logger's level is set to trace or debug.
	 *
	 * @param message the message to write to the log
	 */
	@JSFunction
	public void debug(String message)
	{
		logger.debug(message);
	}

	/**
	 * Log a message with an argument on the 'debug' level,
	 * so it only writes if the logger's level is set to trace or debug.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 */
	@JSFunction
	public void debug(String message, Object arg1)
	{
		logger.debug(message, arg1);
	}

	/**
	 * Log a message with two arguments on the 'debug' level,
	 * so it only writes if the logger's level is set to trace or debug.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 * @param arg2
	 */
	@JSFunction
	public void debug(String message, Object arg1, Object arg2)
	{
		logger.debug(message, arg1, arg2);
	}

	/**
	 * Log a message with multiple arguments on the 'debug' level,
	 * so it only writes if the logger's level is set to trace or debug.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param args
	 */
	@JSFunction
	public void debug(String message, Object... args)
	{
		logger.debug(message, args);
	}

	/**
	 * Log a message with a Throwable argument on the 'debug' level,
	 * so it only writes if the logger's level is set to trace or debug.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1 a Throwable
	 */
	@JSFunction
	public void debug(String message, Throwable arg1)
	{
		logger.debug(message, arg1);
	}

	//TRACE
	/**
	 * Log a message without arguments on the 'trace' level,
	 * so it only writes if the logger's level is set to trace.
	 *
	 * @param message the message to write to the log
	 */
	@JSFunction
	public void trace(String message)
	{
		logger.trace(message);
	}

	/**
	 * Log a message with an argument on the 'trace' level,
	 * so it only writes if the logger's level is set to trace.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 */
	@JSFunction
	public void trace(String message, Object arg1)
	{
		logger.trace(message, arg1);
	}

	/**
	 * Log a message with two arguments on the 'trace' level,
	 * so it only writes if the logger's level is set to trace.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 * @param arg2
	 */
	@JSFunction
	public void trace(String message, Object arg1, Object arg2)
	{
		logger.trace(message, arg1, arg2);
	}

	/**
	 * Log a message with multiple arguments on the 'trace' level,
	 * so it only writes if the logger's level is set to trace.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param args
	 */
	@JSFunction
	public void trace(String message, Object... args)
	{
		logger.trace(message, args);
	}

	/**
	 * Log a message with a Throwable argument on the 'trace' level,
	 * so it only writes if the logger's level is set to trace.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1 a Throwable
	 */
	@JSFunction
	public void trace(String message, Throwable arg1)
	{
		logger.trace(message, arg1);
	}

	// FATAL
	/**
	 * Log a message without arguments on the 'fatal' level,
	 * so since fatal is the most severe logging level, it always writes to the log.
	 *
	 * @param message the message to write to the log
	 */
	@JSFunction
	public void fatal(String message)
	{
		logger.fatal(message);
	}

	/**
	 * Log a message with an argument on the 'fatal' level,
	 * so since fatal is the most severe logging level, it always writes to the log.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 */
	@JSFunction
	public void fatal(String message, Object arg1)
	{
		logger.fatal(message, arg1);
	}

	/**
	 * Log a message with two arguments on the 'fatal' level,
	 * so since fatal is the most severe logging level, it always writes to the log.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1
	 * @param arg2
	 */
	@JSFunction
	public void fatal(String message, Object arg1, Object arg2)
	{
		logger.fatal(message, arg1, arg2);
	}

	/**
	 * Log a message with multiple arguments on the 'fatal' level,
	 * so since fatal is the most severe logging level, it always writes to the log.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param args
	 */
	@JSFunction
	public void fatal(String message, Object... args)
	{
		logger.fatal(message, args);
	}

	/**
	 * Log a message with a Throwable argument on the 'fatal' level,
	 * so since fatal is the most severe logging level, it always writes to the log.
	 * Use {} as placeholder for an argument.
	 *
	 * @param message the message to write to the log
	 * @param arg1 a Throwable
	 */
	@JSFunction
	public void fatal(String message, Throwable arg1)
	{
		logger.fatal(message, arg1);
	}


	// IS LEVEL ENABLED?
	/**
	 * Check if the current logger's logging level enables logging on the info level.
	 * Return true if the logger's level is set to info, debug or trace.
	 *
	 * @return true if 'info' level is enabled for logging
	 */
	@JSFunction
	public boolean isInfoEnabled()
	{
		return logger.isInfoEnabled();
	}

	/**
	 * Check if the current logger's logging level enables logging on the warn level.
	 * Return true if the logger's level is set to warn, info, debug or trace.
	 *
	 * @return true if 'warn' level is enabled for logging
	 */
	@JSFunction
	public boolean isWarnEnabled()
	{
		return logger.isWarnEnabled();
	}

	/**
	 * Check if the current logger's logging level enables logging on the error level.
	 * Return true if the logger's level is set to error, warn, info, debug or trace.
	 *
	 * @return true if 'error' level is enabled for logging
	 */
	@JSFunction
	public boolean isErrorEnabled()
	{
		return logger.isErrorEnabled();
	}

	/**
	 * Check if the current logger's logging level enables logging on the debug level.
	 * Return true if the logger's level is set to debug or trace.
	 *
	 * @return true if 'debug' level is enabled for logging
	 */
	@JSFunction
	public boolean isDebugEnabled()
	{
		return logger.isDebugEnabled();
	}

	/**
	 * Check if the current logger's logging level enables logging on the trace level.
	 * Return true if the logger's level is set to trace.
	 *
	 * @return true if 'trace' level is enabled for logging
	 */
	@JSFunction
	public boolean isTraceEnabled()
	{
		return logger.isTraceEnabled();
	}

	/**
	 * Check if the current logger's logging level enables logging on the fatal level.
	 * Return true if the logger's level is set to fatal, error, warn, info, debug or trace.
	 *
	 * @return true if 'fatal' level is enabled for logging
	 */
	@JSFunction
	public boolean isFatalEnabled()
	{
		return logger.isFatalEnabled();
	}

	// BUILDER
	/**
	 * Construct an info log event.
	 *
	 * @sample
	 * log.atInfo().log("some message and {} ", "some", "arguments");
	 *
	 * @return a LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder atInfo()
	{
		return new JSLogBuilder(logger.atInfo());
	}

	/**
	 * Construct a warn log event.
	 *
	 * @sample
	 * log.atWarn().log("some message and {} ", "some", "arguments");
	 *
	 * @return a LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder atWarn()
	{
		return new JSLogBuilder(logger.atWarn());
	}

	/**
	 * Construct a debug log event.
	 *
	 * @sample
	 * log.atDebug().log("some message and {} ", "some", "arguments");
	 *
	 * @return a LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder atDebug()
	{
		return new JSLogBuilder(logger.atDebug());
	}

	/**
	 * Construct an error log event.
	 *
	 * @sample
	 * log.atError().log("some message and {} ", "some", "arguments");
	 *
	 * @return a LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder atError()
	{
		return new JSLogBuilder(logger.atError());
	}

	/**
	 * Construct a trace log event.
	 *
	 * @sample
	 * log.atTrace().log("some message and {} ", "some", "arguments");
	 *
	 * @return a LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder atTrace()
	{
		return new JSLogBuilder(logger.atTrace());
	}

	/**
	 * Construct a log event that will always be logged.
	 *
	 * @sample
	 * log.always().log("some message and {} ", "some", "arguments");
	 *
	 * @return a LogBuilder.
	 */
	public JSLogBuilder always()
	{
		return new JSLogBuilder(logger.always());
	}

}
