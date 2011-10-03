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

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.querybuilder.IQueryBuilderTableClause;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.ServoyException;

/**
 * @author rgansevles
 *
 */
public abstract class QueryBuilderTableClause implements IQueryBuilderTableClause
{
	private final String dataSource;
	private final QueryBuilder root;
	private final QueryBuilderTableClause parent;

	private QueryBuilderJoins joins;


	private Table table;
	private final Map<String, QueryBuilderColumn> columns = new HashMap<String, QueryBuilderColumn>();
	private QueryBuilderColumns builderColumns;

	QueryBuilderTableClause(String dataSource)
	{
		this.root = (QueryBuilder)this;
		this.parent = this;
		this.dataSource = dataSource;
	}

	QueryBuilderTableClause(QueryBuilder root, QueryBuilderTableClause parent, String dataSource)
	{
		this.root = root;
		this.parent = parent;
		this.dataSource = dataSource;
	}

	abstract QueryTable getQueryTable() throws RepositoryException;

	@JSReadonlyProperty
	public QueryBuilder getRoot()
	{
		return root;
	}

	@JSReadonlyProperty
	public QueryBuilderTableClause getParent()
	{
		return parent;
	}

	@JSReadonlyProperty
	public QueryBuilderColumns columns() throws ServoyException
	{
		if (builderColumns == null)
		{
			builderColumns = new QueryBuilderColumns(getRoot().getScriptableParent());
			for (String columnName : getTable().getColumnNames())
			{
				builderColumns.put(columnName, getRoot().getScriptableParent(), getColumn(columnName));
			}
			builderColumns.setLocked(true);
		}
		return builderColumns;
	}


	@JSReadonlyProperty
	public QueryBuilderJoins joins()
	{
		if (joins == null)
		{
			joins = new QueryBuilderJoins(getRoot(), this);
		}
		return joins;
	}

	@JSFunction
	public QueryBuilderColumn getColumn(String name) throws RepositoryException
	{
		QueryBuilderColumn builderColumn = columns.get(name);
		if (builderColumn == null)
		{
			Column col = getTable().getColumn(name);
			if (col == null)
			{
				throw new RepositoryException("Cannot find column '" + name + "' in data source '" + dataSource + "'");
			}
			columns.put(
				name,
				builderColumn = new QueryBuilderColumn(getRoot(), this, new QueryColumn(getQueryTable(), col.getID(), col.getSQLName(), col.getType(),
					col.getLength(), col.getScale(), false)));
		}
		return builderColumn;
	}

	@JSFunction
	public QueryBuilderColumn getColumn(String tableAlias, String name) throws RepositoryException
	{
		QueryBuilderTableClause queryBuilderTableClause = getRoot().findQueryBuilderTableClause(tableAlias);
		if (queryBuilderTableClause == null)
		{
			throw new RepositoryException("Cannot find table(alias) '" + tableAlias + "'");
		}
		return queryBuilderTableClause.getColumn(name);
	}

	Table getTable() throws RepositoryException
	{
		if (table == null)
		{
			table = getRoot().getTable(dataSource);
		}
		return table;
	}

	QueryBuilderTableClause findQueryBuilderTableClause(String tableAlias)
	{
		if (tableAlias == null)
		{
			return this;
		}
		if (joins != null)
		{
			return joins.findQueryBuilderTableClause(tableAlias);
		}
		return null;
	}
}
