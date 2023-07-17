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
import org.apache.logging.log4j.LogBuilder;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

/**
 * Object for constructing log events before logging them. Instances of JSLogBuilder should only be created
 * by calling one of the JSLogger methods that return a JSLogBuilder.
 *
 * @author jdejong
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLogBuilder")
public class JSLogBuilder
{
	private final LogBuilder builder;
	private final Level logLevel; // This is only here so that users can call log.setLevel(log.info), which might be a violation of the principle of single responsibility

	JSLogBuilder(LogBuilder builder, Level logLevel)
	{
		this.builder = builder;
		this.logLevel = logLevel;
	}

	/**
	 * Includes an exception in the log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn.withException(myException).log("some message");
	 *
	 * @param exception The exception to log.
	 * @return the LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder withException(Throwable exception)
	{
		builder.withThrowable(exception);
		return this;
	}

	/**
	 * Logs a message with or without parameters.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn.log("some message {} {} {}", "with", "multiple", "arguments");
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param params parameters to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(Object message, Object... params)
	{
		this.builder.log(message != null ? Utils.getScriptableString(message) : "", params); //$NON-NLS-1$
	}

	/**
	 * Logs an event without adding a message.
	 * This can be useful in combination with withException(e) if no message is required.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn.withException(myException).log();
	 *
	 */
	@JSFunction
	public void log()
	{
		this.builder.log();
	}

	Level getLevel()
	{
		return this.logLevel;
	}
}
