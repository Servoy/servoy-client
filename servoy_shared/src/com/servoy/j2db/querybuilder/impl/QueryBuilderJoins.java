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

import org.doomdark.uuid.UUID;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.querybuilder.IQueryBuilderJoin;
import com.servoy.j2db.querybuilder.IQueryBuilderJoins;
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author rgansevles
 *
 */
public class QueryBuilderJoins extends DefaultScope implements IQueryBuilderJoins
{
	private final QueryBuilder root;
	private final QueryBuilderTableClause parent;

	QueryBuilderJoins(QueryBuilder root, QueryBuilderTableClause parent)
	{
		super(root.getScriptableParent());
		this.root = root;
		this.parent = parent;
	}

	@JSFunction
	public QueryBuilderJoin add(String dataSource, int joinType) throws RepositoryException
	{
		return add(dataSource, joinType, null);
	}

	@JSFunction
	public QueryBuilderJoin add(String dataSource, String alias) throws RepositoryException
	{
		return add(dataSource, IQueryBuilderJoin.LEFT_OUTER_JOIN, alias);
	}

	@JSFunction
	public QueryBuilderJoin add(String dataSource) throws RepositoryException
	{
		return add(dataSource, IQueryBuilderJoin.LEFT_OUTER_JOIN, null);
	}

	@JSFunction
	public QueryBuilderJoin add(String dataSource, int joinType, String alias) throws RepositoryException
	{
		Table foreignTable = root.getTable(dataSource);
		QueryJoin queryJoin = new QueryJoin(alias, parent.getQueryTable(), new QueryTable(foreignTable.getSQLName(), foreignTable.getCatalog(),
			foreignTable.getSchema()), new AndCondition(), joinType);
		root.getQuery().addJoin(queryJoin);
		QueryBuilderJoin join = new QueryBuilderJoin(root, parent, dataSource, queryJoin);
		put(alias == null ? new UUID().toString() : alias, getParentScope(), join);
		return join;
	}

	/**
	 * @param tableAlias
	 * @return
	 */
	public QueryBuilderTableClause findQueryBuilderTableClause(String tableAlias)
	{
		Object get = get(tableAlias, getParentScope());
		if (get instanceof QueryBuilderTableClause)
		{
			return (QueryBuilderTableClause)get;
		}
		// not a direct child, try recursive
		for (Object val : getValues())
		{
			if (val instanceof QueryBuilderTableClause)
			{
				QueryBuilderTableClause found = ((QueryBuilderTableClause)val).findQueryBuilderTableClause(tableAlias);
				if (found != null)
				{
					return found;
				}
			}
		}
		// not found
		return null;
	}
}
