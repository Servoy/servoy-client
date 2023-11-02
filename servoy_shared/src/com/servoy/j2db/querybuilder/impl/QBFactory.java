/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.ITableAndRelationProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;

/**
 * @author rgansevles
 *
 */
public class QBFactory implements IQueryBuilderFactory
{
	private final Scriptable scriptableParent;
	private final ITableAndRelationProvider tableProvider;
	private final IGlobalValueEntry globalScopeProvider;
	private final IDataProviderHandler dataProviderHandler;

	public QBFactory(ITableAndRelationProvider tableProvider, IGlobalValueEntry globalScopeProvider, IDataProviderHandler dataProviderHandler,
		Scriptable scriptableParent)
	{
		this.tableProvider = tableProvider;
		this.globalScopeProvider = globalScopeProvider;
		this.dataProviderHandler = dataProviderHandler;
		this.scriptableParent = scriptableParent;
	}

	public QBSelect createSelect(String dataSource, String alias) throws RepositoryException
	{
		return new QBSelect(tableProvider, globalScopeProvider, dataProviderHandler, scriptableParent, dataSource, alias);
	}

	public QBSelect createSelect(String dataSource) throws RepositoryException
	{
		return createSelect(dataSource, null);
	}

	public QBSelect createSelect(String dataSource, String alias, QuerySelect querySelect)
	{
		return new QBSelect(tableProvider, globalScopeProvider, dataProviderHandler, scriptableParent, dataSource, alias, querySelect);
	}

	/**
	 * Resolve the datasource for the table with given sql name, look in server of serverDataSource.
	 *
	 * @param serverDataSource
	 * @param tableSQLName
	 */
	public String resolveDataSource(String serverDataSource, String tableSQLName)
	{
		String dataSource = tableProvider.resolveDataSource(serverDataSource, tableSQLName);
		if (dataSource == null)
		{
			throw new RuntimeException("Could not resolve table sqlname = '" + tableSQLName + "' in server of datasource '" + serverDataSource + "'");
		}
		return dataSource;
	}
}
