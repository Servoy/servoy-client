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

import com.servoy.j2db.IEventDelegator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.plugins.IClientPluginAccess;

public interface IHeadlessClient extends IEventDelegator
{
	/**
	 * Check if the client still valid for usage.
	 */
	public boolean isValid();

	/**
	 * Gets the clientId of this client.
	 *
	 * @return A String which holds the id of the client.
	 */
	public String getClientID();

	/**
	 * Get a dataprovider value.
	 *
	 * @param contextName This is the form name or null if the method is a global method.
	 * @param dataprovider the dataprovider name as seen in Servoy
	 * @return the value for the dataprovider
	 */
	public Object getDataProviderValue(String contextName, String dataprovider);

	/**
	 * Set a dataprovider value.
	 *
	 * @param contextName This is the form name or null if the method is a global method.
	 * @param dataprovider the dataprovider name as seen in Servoy
	 * @param value to set
	 * @return the old value or null if no change
	 */
	public Object setDataProviderValue(String contextName, String dataprovider, Object value);

	/**
	 * Get the plugin access, for more functionality.
	 * @return the plugin access
	 */
	public IClientPluginAccess getPluginAccess();

	/**
	 * Shut down this client
	 * @param force to enforce shutdown
	 */
	public void shutDown(boolean force);

	/**
	 * Load solution into the client
	 *
	 * @param solutionName of the solution to load
	 * @throws RepositoryException
	 */
	public void loadSolution(String solutionName) throws RepositoryException;

	/**
	 * Close currently opened solution
	 *
	 * @param force to enforce close
	 * @return whether the closing was successful
	 */
	public boolean closeSolution(boolean force);
}
