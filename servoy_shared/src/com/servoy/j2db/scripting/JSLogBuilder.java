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
import org.mozilla.javascript.NativeError;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The <code>JSLogBuilder</code> class facilitates constructing and logging events with support for exceptions and
 * parameterized messages. It integrates with the logging framework and is instantiated through <code>JSLogger</code> methods.
 * The class supports adding exceptions to log events, including <code>Throwable</code> and JavaScript's <code>NativeError</code>.
 * Developers can log messages with or without additional parameters, using flexible formatting.
 *
 * When handling <code>NativeError</code> objects, it extracts properties like name, message, and stack trace for inclusion
 * in the log. Additionally, it provides functionality to log events without a message, making it suitable for error-only
 * logging scenarios. The logging level is preserved as part of the instance for potential configuration or usage.
 *
 * @author jdejong
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSLogBuilder")
public class JSLogBuilder
{
	private final LogBuilder builder;
	private final Level logLevel; // This is only here so that users can call log.setLevel(log.info), which might be a violation of the principle of single responsibility

	private NativeError nativeError = null;

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
	public JSLogBuilder withException(Object exception)
	{
		if (exception instanceof Throwable t)
		{
			builder.withThrowable(t);
		}
		else if (exception instanceof NativeError ne)
		{
			nativeError = ne;
		}
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
	@SuppressWarnings("nls")
	@JSFunction
	public void log(Object message, Object... params)
	{
		String msg = message != null ? Utils.getScriptableString(message) : "";
		if (nativeError != null)
		{
			Object name = nativeError.get("name", nativeError);
			if (name == null || name instanceof UniqueTag) name = "";
			else name += ": ";
			Object errorMsg = nativeError.get("message", nativeError);
			if (errorMsg == null || errorMsg instanceof UniqueTag) errorMsg = "";
			else errorMsg = "\t" + errorMsg + "\n";
			msg += "\n" + name + errorMsg + nativeError.getStackDelegated();
		}
		else if (logLevel.isMoreSpecificThan(Level.WARN))
		{
			msg += Debug.getScriptStackTrace(msg, null);
		}
		this.builder.log(msg, params);
		nativeError = null;
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
	@SuppressWarnings("nls")
	@JSFunction
	public void log()
	{
		if (nativeError != null)
		{
			Object name = nativeError.get("name", nativeError);
			if (name == null || name instanceof UniqueTag) name = "";
			else name += ": ";
			Object msg = nativeError.get("message", nativeError);
			if (msg == null || msg instanceof UniqueTag) msg = "<error>";
			this.builder.log(name.toString() + msg + "\n" + nativeError.getStackDelegated());
		}
		else this.builder.log();
		nativeError = null;
	}

	Level getLevel()
	{
		return this.logLevel;
	}
}
