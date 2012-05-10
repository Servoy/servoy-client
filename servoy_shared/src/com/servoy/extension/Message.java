/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension;

/**
 * A text message containing either a info, a warning or an error.
 * 
 * @author acostescu
 */
public class Message
{
	public final static int INFO = 1;
	public final static int WARNING = 2;
	public final static int ERROR = 3;

	public final int severity;
	public final String message;

	public Message(String message, int severity)
	{
		this.message = message;
		this.severity = severity;
	}

	@Override
	@SuppressWarnings("nls")
	public String toString()
	{
		String severityS;
		switch (severity)
		{
			case INFO :
				severityS = "INFO";
				break;
			case WARNING :
				severityS = "INFO";
				break;
			case ERROR :
				severityS = "INFO";
				break;
			default :
				severityS = "-unknown-";
		}
		return "(" + severityS + ", " + message + ")";
	}

}
