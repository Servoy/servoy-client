/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.persistence;

import com.servoy.j2db.util.ILogLevel;


/**
 * The <code>InfoChannel</code> interface allows a program to send info messages to a user. Fatal errors should be handled using exceptions, but for all
 * non-fatal errors information can be sent to the user with this interface.
 *
 */
public interface InfoChannel extends ILogLevel
{
	/**
	 * Send an informational message. This message should contain information that could be interesting for a user to see. The <code>priority</code> parameter
	 * should indicate the relative importance of the message.
	 *
	 * @param message the message to send
	 */
	public void info(String message, int priority);

	public void clientInfo(String message, int priority);

	public void displayWarningMessage(String title, String message, boolean scrollableDialog);
}
