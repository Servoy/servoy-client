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

import org.apache.logging.log4j.LogBuilder;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

/**
 * Interface for constructing log events before logging them. Instances of JSLogBuilder should only be created
 * by calling one of the JSLogger methods that return a JSLogBuilder.
 *
 * @author jdejong
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLogBuilder")
public class JSLogBuilder
{
	LogBuilder builder;

	JSLogBuilder(LogBuilder builder)
	{
		this.builder = builder;
	}

	/**
	 * Includes an exception in the log event.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn().withException(myException).log("some message");
	 *
	 * @param throwable The exception to log.
	 * @return the LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder withException(Throwable exception)
	{
		this.builder = builder.withThrowable(exception);
		return this;
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn().log("some message {} {} {}", "with", "multiple", "arguments");
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param params parameters to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object... params)
	{
		this.builder.log(message, params);
	}

	/**
	 * Causes all the data collected to be logged along with the message.
	 *
	 * @sample
	 * var log = application.getLogger();
	 * log.warn().log("some message or object");
	 *
	 * @param message The message to log.
	 */
	@JSFunction
	public void log(Object message)
	{
		this.builder.log(Utils.getScriptableString(message));
	}

	/**
	 * Causes all the data collected to be logged. Default implementation does nothing.
	 */
	@JSFunction
	public void log()
	{
		this.builder.log();
	}
}
