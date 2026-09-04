/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
package com.servoy.mcp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

/**
 * Holds on to what one logger said, for the tests where the log line is the promise.
 *
 * <p>Most of what the server logs is a side effect: the test asserts the outcome and does not care
 * how it was announced. Two things are not like that. A tool that cannot be published is dropped
 * and logged, and nothing else marks it - there is no error in the Developer, by design - so
 * whoever wrote that tool learns about it from this line or not at all. That makes the line part of
 * the contract, and a contract is worth asserting.</p>
 *
 * <p>Everything else is silenced by {@code log4j2-test.xml}. This puts one named logger back on
 * record, into a list rather than onto the console, and takes it off again when the block ends.</p>
 *
 * <pre>
 * try (McpLogCapture log = McpLogCapture.of("servoy.mcp"))
 * {
 *     ... provoke it ...
 *     assertTrue(log.contains("not publishing a tool"));
 * }
 * </pre>
 *
 * @author Servoy
 */
final class McpLogCapture implements AutoCloseable
{
	private final List<String> lines = Collections.synchronizedList(new ArrayList<String>());

	private final LoggerContext context;

	private final String loggerName;

	private McpLogCapture(String loggerName)
	{
		this.loggerName = loggerName;
		this.context = (LoggerContext)LogManager.getContext(false);

		AbstractAppender appender = new AbstractAppender("McpLogCapture", (Filter)null, //$NON-NLS-1$
			(Layout< ? extends Serializable>)null, true, Property.EMPTY_ARRAY)
		{
			@Override
			public void append(LogEvent event)
			{
				lines.add(event.getLevel() + " " + event.getMessage().getFormattedMessage()); //$NON-NLS-1$
			}
		};
		appender.start();

		Configuration configuration = context.getConfiguration();

		// a logger of its own rather than an appender on the root one: additive false keeps these
		// events away from whatever the root is doing, and removing it again is a single call
		LoggerConfig capturing = new LoggerConfig(loggerName, Level.ALL, false);
		capturing.addAppender(appender, Level.ALL, null);

		configuration.addLogger(loggerName, capturing);
		context.updateLoggers();
	}

	static McpLogCapture of(String loggerName)
	{
		return new McpLogCapture(loggerName);
	}

	/** Whether any line said this. */
	boolean contains(String text)
	{
		for (String line : lines)
		{
			if (line.contains(text)) return true;
		}

		return false;
	}

	/** Everything that was said, for a failure message worth reading. */
	List<String> lines()
	{
		return new ArrayList<String>(lines);
	}

	@Override
	public void close()
	{
		context.getConfiguration().removeLogger(loggerName);
		context.updateLoggers();
	}
}
