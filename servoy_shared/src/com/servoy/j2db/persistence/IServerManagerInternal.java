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

import java.io.File;
import java.sql.Driver;

import com.servoy.j2db.server.shared.IServiceRegistry;
import com.servoy.j2db.util.visitor.IVisitable;


/**
 * IServerManager interface extended with internal methods.
 */
public interface IServerManagerInternal extends IServerManager
{
	void init(IServiceRegistry serviceRegistry, boolean appServerStartup) throws Exception;

	IServer deleteServer(ServerConfig oldServerConfig);

	IServer createServer(ServerConfig newServerConfig);

	String validateServerConfig(String oldServerName, ServerConfig serverConfig);

	void addServerConfigListener(IServerConfigListener serverConfigSyncer);

	void removeServerConfigListener(IServerConfigListener logServerListener);

	ISequenceProvider getGlobalSequenceProvider();

	void reloadServersTables() throws RepositoryException;

	File getDriversDir();

	/**
	 * Tries to load driver from drivers dir or system
	 * @param driverClassName the name
	 * @param url the jdbc url
	 * @return the driver instance
	 * @throws Exception
	 */
	Driver loadDriver(String driverClassName, String url) throws Exception;

	void loadInstalledDrivers();

	IServer getServer(String string);

	IServer getLogServer();

	String getLogServerName();

	void testServerConfigConnection(ServerConfig serverConfig, int i) throws Exception;

	void setLogServerName(String currentServerName);

	void saveServerConfig(String oldServerName, ServerConfig serverConfig) throws RepositoryException;

	/**
	 * Get list of drivers found in drivers dir
	 * @return the known drivers
	 */
	String[] getKnownDriverClassNames();

	/**
	 * Get list of drivers found in drivers dir or used (system loaded)
	 * @return the all the drivers
	 */
	String[] getAllDriverClassNames();

	ServerConfig[] getServerConfigs();

	boolean logTableExists();

	boolean clientStatsTableExists();

	void removeServerListener(IServerListener serverListener);

	void addServerListener(IServerListener serverListener);

	ISequenceProvider getSequenceProvider(String name);

	IColumnInfoManager[] getColumnInfoManagers(String name);

	ServerConfig getServerConfig(String name);

	IServer getRepositoryServer() throws RepositoryException;

	void addGlobalColumnInfoProvider(IColumnInfoProvider cip);

	void removeGlobalColumnInfoProvider(IColumnInfoProvider cip);

	void setGlobalColumnInfoProviders(IServerInternal server, IDeveloperRepository rep, String clientId);

	void setGlobalSequenceProvider(ISequenceProvider sm);

	public boolean isServerDataModelCloneCycling(IServerInternal server);

	void updateColumnInfo(IVisitable sqlSelect, String server_name) throws RepositoryException;

	ClassLoader getClassLoader();

	void setServerEnableListener(IServerEnableListener iServerEnableListener);

	void close();
}
