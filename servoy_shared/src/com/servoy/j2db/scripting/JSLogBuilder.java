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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Interface for constructing log events before logging them. Instances of JSLogBuilder should only be created
 * by calling one of the Logger methods that return a JSLogBuilder.
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
	 * Includes a Throwable in the log event.
	 *
	 * @sample
	 * log.atWarn().withTrowable(myException).log("some message");
	 *
	 * @param throwable The Throwable to log.
	 * @return the LogBuilder.
	 */
	@JSFunction
	public JSLogBuilder withThrowable(Throwable throwable)
	{
		this.builder = builder.withThrowable(throwable);
		return this;
	}

	/**
	 * Causes all the data collected to be logged along with the message. Interface default method does nothing.
	 *
	 * @sample
	 * log.atWarn().log(myCharSequence);
	 *
	 * @param message The message to log.
	 */
	@JSFunction
	public void log(CharSequence message)
	{
		this.builder.log(message);
	}

	/**
	 * Causes all the data collected to be logged along with the message. Interface default method does nothing.
	 *
	 * @sample
	 * log.atWarn().log(myString);
	 *
	 * @param message The message to log.
	 */
	@JSFunction
	public void log(String message)
	{
		this.builder.log(message);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @sample
	 * log.atWarn().log("some message {} {} {}", "with", "multiple", "arguments");
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
	 * Causes all the data collected to be logged along with the message and parameters.
	 *
	 * @sample
	 * log.atWarn().log("some message {} {}", mySupplier1, mySupplier2);
	 *
	 * @param message The message.
	 * @param params Parameters to the message.
	 */
	@JSFunction
	public void log(String message, Supplier< ? >... params)
	{
		this.builder.log(message, params);
	}

	/**
	 * Causes all the data collected to be logged along with the message.
	 *
	 * @sample
	 * log.atWarn().log(myMessage);
	 *
	 * @param message The message to log.
	 */
	@JSFunction
	public void log(Message message)
	{
		this.builder.log(message);
	}

	/**
	 * Causes all the data collected to be logged along with the message.
	 *
	 * @sample
	 * log.atWarn().log(myMessageSupplier);
	 *
	 * @param messageSupplier The supplier of the message to log.
	 */
	@JSFunction
	public void log(Supplier<Message> messageSupplier)
	{
		this.builder.log(messageSupplier);
	}

	/**
	 * Causes all the data collected to be logged along with the message.
	 *
	 * @sample
	 * log.atWarn().log(myObject);
	 *
	 * @param message The message to log.
	 */
	@JSFunction
	public void log(Object message)
	{
		this.builder.log(message);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @sample
	 * log.atWarn().log("some message with some {}", "argument");
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0)
	{
		this.builder.log(message, p0);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @sample
	 * log.atWarn().log("some message with {} {}", "some", "argument");
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1)
	{
		this.builder.log(message, p0, p1);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2)
	{
		this.builder.log(message, p0, p1, p2);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3)
	{
		this.builder.log(message, p0, p1, p2, p3);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 * @param p4 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4)
	{
		this.builder.log(message, p0, p1, p2, p3, p4);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 * @param p4 parameter to the message.
	 * @param p5 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5)
	{
		this.builder.log(message, p0, p1, p2, p3, p4, p5);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 * @param p4 parameter to the message.
	 * @param p5 parameter to the message.
	 * @param p6 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6)
	{
		this.builder.log(message, p0, p1, p2, p3, p4, p5, p6);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 * @param p4 parameter to the message.
	 * @param p5 parameter to the message.
	 * @param p6 parameter to the message.
	 * @param p7 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
		Object p7)
	{
		this.builder.log(message, p0, p1, p2, p3, p4, p5, p6, p7);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 * @param p4 parameter to the message.
	 * @param p5 parameter to the message.
	 * @param p6 parameter to the message.
	 * @param p7 parameter to the message.
	 * @param p8 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
		Object p7, Object p8)
	{
		this.builder.log(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
	}

	/**
	 * Logs a message with parameters.
	 *
	 * @param message the message to log; the format depends on the message factory.
	 * @param p0 parameter to the message.
	 * @param p1 parameter to the message.
	 * @param p2 parameter to the message.
	 * @param p3 parameter to the message.
	 * @param p4 parameter to the message.
	 * @param p5 parameter to the message.
	 * @param p6 parameter to the message.
	 * @param p7 parameter to the message.
	 * @param p8 parameter to the message.
	 * @param p9 parameter to the message.
	 *
	 * @see org.apache.logging.log4j.util.Unbox
	 */
	@JSFunction
	public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
		Object p7, Object p8, Object p9)
	{
		this.builder.log(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
