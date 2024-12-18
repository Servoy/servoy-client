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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;

/**
 * The <code>JSLogger</code> class provides a comprehensive API for managing logging operations. It supports constructing log events
 * with various severity levels, such as fatal, error, warn, info, debug, and trace, using the <code>JSLogBuilder</code> instances
 * it creates. Logs can include messages formatted with arguments, enabling flexible and detailed logging.
 *
 * The class offers methods to check if a specific logging level is enabled, allowing efficient logging decisions. It also allows
 * dynamically setting the logger's logging level, overriding the default configuration. This global change persists until the
 * application server is restarted. The current logging level can be retrieved as a string using the <code>level</code> property.
 *
 * @author jdejong
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLogger")
public class JSLogger
{
	private final Logger logger;


	/**
	 * Get the default logger
	 */
	public JSLogger()
	{
		this.logger = LogManager.getLogger(Debug.class);
	}

	/**
	 * Get a specific logger
	 *
	 * @param loggerName
	 */
	public JSLogger(String loggerName)
	{
		this.logger = LogManager.getLogger(loggerName);
	}

	// IS LEVEL ENABLED?
	/**
	 * Check if the current logger's logging level enables logging on the info level.
	 * Return true if the logger's level is set to info, debug or trace.
	 *
	 * @return true if 'info' level is enabled for logging
	 */
	@JSReadonlyProperty
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
	@JSReadonlyProperty
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
	@JSReadonlyProperty
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
	@JSReadonlyProperty
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
	@JSReadonlyProperty
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
	@JSReadonlyProperty
	public boolean isFatalEnabled()
	{
		return logger.isFatalEnabled();
	}

	// BUILDER
	/**
	 * Construct an info log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.info.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder info()
	{
		return new JSLogBuilder(logger.atInfo(), Level.INFO);
	}

	/**
	 * Construct a warn log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder warn()
	{
		return new JSLogBuilder(logger.atWarn(), Level.WARN);
	}

	/**
	 * Construct a debug log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.debug.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder debug()
	{
		return new JSLogBuilder(logger.atDebug(), Level.DEBUG);
	}

	/**
	 * Construct an error log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.error.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder error()
	{
		return new JSLogBuilder(logger.atError(), Level.ERROR);
	}

	/**
	 * Construct a trace log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.trace.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder trace()
	{
		return new JSLogBuilder(logger.atTrace(), Level.TRACE);
	}

	/**
	 * Construct a fatal log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.fatal.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder fatal()
	{
		return new JSLogBuilder(logger.atFatal(), Level.FATAL);
	}

	/**
	 * Construct a log event that will always be logged.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.always.log("some message and {} {}", "some", "arguments");
	 *
	 * @return a LogBuilder
	 */
	@JSReadonlyProperty
	public JSLogBuilder always()
	{
		return new JSLogBuilder(logger.always(), Level.ALL);
	}

	/**
	 * Set the level for this logger.
	 * Be aware that this will override the logging level as configured in log4j.xml,
	 * meaning it affects all JSLogger instances based on that configuration.
	 * This changes the global configuration,
	 * meaning that restarting the client will not reset the logging level to it's default state.
	 * Only restarting the application server will reset the logging level to it's default state.
	 *
	 * @sample
	 * var log = application.getLogger("myLogger");
	 * log.setLevel(log.info);
	 *
	 * @param level the desired logging level for this logger
	 */
	// this argument type might be a little confusing, but we do this so that users can call e.g. log.setLevel(log.info)
	// and log.info happens to return a JSLogBuilder instance.
	@JSFunction
	public void setLevel(JSLogBuilder level)
	{
		Level logLevel = Level.toLevel(level.getLevel().name(), this.logger.getLevel());
		if (logLevel != this.logger.getLevel())
		{
			Configurator.setAllLevels(logger.getName(), logLevel);
		}
	}

	/**
	 * Get the logging level of this logger
	 *
	 * @return the logging level of this logger
	 */
	@JSReadonlyProperty
	public String level()
	{
		return this.logger.getLevel().name();
	}

}
