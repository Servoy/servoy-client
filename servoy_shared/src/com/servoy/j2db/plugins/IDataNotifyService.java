/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.j2db.plugins;

import java.rmi.RemoteException;

import com.servoy.j2db.dataprocessing.IDataSet;

/**
 * @author jcompagner
 * @since 8.2
 *
 */
public interface IDataNotifyService
{
	/**
	 * Registers a databroadcast listener that gets all databroadcast over all the database servers
	 */
	public void registerDataNotifyListener(IDataNotifyListener listener);

	/**
	 * Deregisters a databroadcast listener that gets all databroadcast over all the database servers
	 */
	public void deregisterDataNotifyListener(IDataNotifyListener listener);

	/**
	 * Returns a list of datasources that are current in use (over all clients)
	 */
	public String[] getUsedDataSources();

	/**
	 * Tell the system to do a full flush on the given datasource
	 *
	 * @param dataSource
	 * @throws RemoteException
	 */
	public void flushCachedDatabaseData(String dataSource);

	/**
	 * Tell the system to trigger a data notification on the given table for the given pk set.
	 *
	 * @param server_name
	 * @param table_name
	 * @param pks
	 * @param action
	 * @param insertColumnData
	 * @throws RemoteException
	 */
	public void notifyDataChange(String server_name, String table_name, IDataSet pks, int action, Object[] insertColumnData) throws RemoteException;

}
