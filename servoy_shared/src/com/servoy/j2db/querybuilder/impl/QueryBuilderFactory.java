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

import com.servoy.j2db.persistence.ITableProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;

/**
 * @author rgansevles
 *
 */
public class QueryBuilderFactory implements IQueryBuilderFactory
{
	private Scriptable scriptableParent;
	private final ITableProvider tableProvider;

	public QueryBuilderFactory(ITableProvider tableProvider)
	{
		this.tableProvider = tableProvider;
	}

	/**
	 * @param scriptableParent the scriptableParent to set
	 */
	public void setScriptableParent(Scriptable scriptableParent)
	{
		this.scriptableParent = scriptableParent;
	}

	public QueryBuilder createSelect(String dataSource, String alias) throws RepositoryException
	{
		QueryBuilder queryBuilder = new QueryBuilder(tableProvider, dataSource, alias);
		queryBuilder.setScriptableParent(scriptableParent);
		return queryBuilder;
	}

	public QueryBuilder createSelect(String dataSource) throws RepositoryException
	{
		return createSelect(dataSource, null);
	}
}
