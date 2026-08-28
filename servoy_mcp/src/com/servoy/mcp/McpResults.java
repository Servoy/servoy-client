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

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Renders whatever a scope function returned as the text content of a tool result.
 *
 * <p>A tool is free to return whatever it likes; nothing is converted or validated on the way out.
 * If an agent cannot make sense of what comes back, that is between the tool author and the agent.
 * The only thing done here is presentation.</p>
 *
 * @author Servoy
 */
public class McpResults
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private McpResults()
	{
	}

	public static String describe(Object returned)
	{
		if (returned == null) return ""; //$NON-NLS-1$
		if (returned instanceof CharSequence) return returned.toString();

		if (returned instanceof Number)
		{
			// the scripting engine hands back a Double for every number, so a count would come out
			// as "91.0". Whole values are rendered without the decimal, which is what a reader -
			// human or agent - expects to see.
			double value = ((Number)returned).doubleValue();
			if (!Double.isNaN(value) && !Double.isInfinite(value) && value == Math.rint(value) && Math.abs(value) < 1e15)
			{
				return String.valueOf((long)value);
			}
			return String.valueOf(value);
		}

		if (returned instanceof Boolean) return returned.toString();

		if (returned instanceof Map || returned instanceof Collection || returned instanceof Object[])
		{
			try
			{
				return MAPPER.writeValueAsString(returned);
			}
			catch (Exception e)
			{
				// falling through to toString is better than failing a call that already succeeded
				return String.valueOf(returned);
			}
		}

		return String.valueOf(returned);
	}
}
