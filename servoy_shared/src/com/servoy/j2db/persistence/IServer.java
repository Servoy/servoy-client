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


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Remote interface to a database server
 *
 * @author jblok
 */
public interface IServer extends Remote
{
	public static final String REPOSITORY_SERVER = "repository_server"; //$NON-NLS-1$
	public static final String INMEM_SERVER = "_sv_inmem"; //$NON-NLS-1$
	public static final String SERVOY_UPPERCASE_PREFIX = "SERVOY";


	public ITable getTable(String tableName) throws RepositoryException, RemoteException;

	public ITable getTableBySqlname(String tableSQLName) throws RepositoryException, RemoteException;

	public List<String> getTableAndViewNames(boolean hideTemporary) throws RepositoryException, RemoteException;

	public List<String> getTableNames(boolean hideTempTables) throws RepositoryException, RemoteException;

	public Map<String, ITable> getInitializedTables() throws RepositoryException, RemoteException;

	public List<String> getViewNames(boolean hideTempViews) throws RepositoryException, RemoteException;

	public int getTableType(String tableName) throws RepositoryException, RemoteException;

	public String getName() throws RemoteException;

	public boolean isValid() throws RemoteException;

	public String getDatabaseProductName() throws RepositoryException, RemoteException;

	/**
	 * Get quoted identifier according to the server dialect when needed, return original when no quoting is needed. When both args are used , the qualified
	 * name is returned.
	 *
	 * @param tableSqlName table or column name
	 * @param columnSqlName column name when qualified with quoted table name, otherwise null
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public String getQuotedIdentifier(String tableSqlName, String columnSqlName) throws RepositoryException, RemoteException;

	public String[] getDataModelClonesFrom() throws RemoteException;

	/**
	 * @return
	 */
	public ISequenceProvider getSequenceProvider();
}
