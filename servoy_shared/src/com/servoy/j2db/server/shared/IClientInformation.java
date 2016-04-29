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
package com.servoy.j2db.server.shared;

import java.util.Date;

/**
 * Interface to receive some client information at the server side
 *
 * @author gerze
 */
public interface IClientInformation extends com.servoy.j2db.server.IClientInformation
{
	/**
	 * Gets the clientId of this client.
	 *
	 * @return A String which holds the id of the client.
	 */
	String getClientID();

	/**
	 * Gets the identified of the host computer where this client has connected from.
	 *
	 * @return A String which holds the identifier of the host computer.
	 */
	String getHostIdentifier();

	/**
	 * Gets the name of the host computer where this client has connected from.
	 *
	 * @return A String which holds the name of the host computer.
	 */
	String getHostName();

	/**
	 * Gets the address of the host computer where this client has connected from.
	 *
	 * @return A String which holds the address of the host computer.
	 */
	String getHostAddress();

	/**
	 * Gets the type of the application that was started by this client (smart client, web client, ...).
	 *
	 * @return An int which encodes the application type.
	 */
	int getApplicationType();

	/**
	 * Gets the uid of the user who logged in at this client.
	 *
	 * @return A String holding the uid of the user. If the solution that is running on this client does not require a login, then null is returned.
	 */
	String getUserUID();

	/**
	 * Gets the name of the user who logged in at this client.
	 *
	 * @return A String holding the name of the user. If the solution that is running on the client does not require a login, then null is returned.
	 */
	String getUserName();

	/**
	 * Gets the time and date this client logged in.
	 *
	 * @return A Date when the client logged in. If the solution that is running on the client does not require a login, then null is returned.
	 */
	Date getLoginTime();

	/**
	 * Gets the time and date since this client has been idle.
	 *
	 * @return A Date representing since when the client is idle. If the solution that is running on the client does not require a login, then null is returned.
	 */
	Date getIdleTime();

	/**
	 * Gets the name of the solution that is currently open by the client.
	 *
	 * @return solution name, or null when no solution is open.
	 */
	String getOpenSolutionName();

	/**
	 * Gets the last date and time when a user has physically accessed the application. NGClient only!
	 * @return a date object or null if the client doesn't support this
	 */
	Date getLastAccessedTime();
}
