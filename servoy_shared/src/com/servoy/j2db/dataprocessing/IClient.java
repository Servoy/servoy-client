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
package com.servoy.j2db.dataprocessing;


import java.rmi.RemoteException;

import com.servoy.j2db.scripting.StartupArguments;

/**
 * The Client call back interface which are registered on the server.
 *
 * @author jblok
 */
public interface IClient
{
	public void alert(String msg) throws RemoteException;

	public boolean isAlive() throws RemoteException;

	public void shutDown() throws RemoteException;

	public void closeSolution() throws RemoteException;

	/**
	 * called when the databases are modified outside servoy, when server_name and table_name are null all is flushed
	 *
	 * @param server_name
	 * @param table_name
	 * @throws RemoteException
	 */
	public void flushCachedDatabaseData(String dataSource) throws RemoteException;

	//rowData is not null when insert
	public void notifyDataChange(String server_name, String table_name, IDataSet pks, int action, Object[] insertColumnData) throws RemoteException;

	public void activateSolutionMethod(String globalMethodName, StartupArguments argumentsScope) throws RemoteException;


	/**
	 * Get a status line for the client to be displayed on the admin page; only when the client supports this.
	 */
	String getClientStatusLine() throws RemoteException;

	/**
	 * Return the last date and time when a user has physically accessed the application
	 */
	long getLastAccessedTime() throws RemoteException;
}