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

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.dataprocessing.SQLGenerator;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.querybuilder.IQueryBuilderJoin;
import com.servoy.j2db.querybuilder.IQueryBuilderJoins;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBJoins extends DefaultScope implements IQueryBuilderJoins
{
	private final QBSelect root;
	private final QBTableClause parent;

	private final Map<String, QBJoin> joins = new HashMap<String, QBJoin>();

	QBJoins(QBSelect root, QBTableClause parent)
	{
		super(root.getScriptableParent());
		this.root = root;
		this.parent = parent;
	}

	@JSReadonlyProperty
	public QBTableClause getParent()
	{
		return parent;
	}

	@JSReadonlyProperty
	public QBSelect getRoot()
	{
		return root;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		QBJoin join = getOrAddRelation(root.getRelation(name), name, null);
		if (join == null)
		{
			return Scriptable.NOT_FOUND;
		}
		return join;
	}

	private QBJoin getOrAddRelation(IRelation relation, String relationName, String alias)
	{
		if (relation == null || !parent.getDataSource().equals(relation.getPrimaryDataSource()))
		{
			if (relation == null)
			{
				Debug.log("relation '" + relationName + "' not found");
			}
			else
			{
				Debug.log("relation '" + relationName + "' does not match parent data source: " + parent.getDataSource() + '/' +
					relation.getPrimaryDataSource());
			}
			return null;
		}

		String name = alias == null ? relationName : alias;
		QBJoin join = joins.get(name);
		if (join == null)
		{
			try
			{
				Table foreignTable = root.getTable(relation.getForeignDataSource());
				if (foreignTable == null)
				{
					Debug.log("foreign table for relation '" + relationName + "' not found");
					return null;
				}

				join = addJoin(SQLGenerator.createJoin(root.getDataProviderHandler(), relation, parent.getQueryTable(),
					new QueryTable(foreignTable.getSQLName(), foreignTable.getCatalog(), foreignTable.getSchema()), root.getGlobalScopeProvider()),
					relation.getForeignDataSource(), name);
			}
			catch (RepositoryException e)
			{
				Debug.error("could not load relation '" + relationName + "'", e);
			}
		}
		return join;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		// ignore
	}

	@JSFunction
	public QBJoin add(String dataSource, int joinType) throws RepositoryException
	{
		return add(dataSource, joinType, null);
	}

	@JSFunction
	public QBJoin add(String dataSourceOrRelation, String alias) throws RepositoryException
	{
		IRelation relation = root.getRelation(dataSourceOrRelation);
		if (relation != null)
		{
			return getOrAddRelation(relation, dataSourceOrRelation, alias);
		}
		if (dataSourceOrRelation.indexOf(':') < 0) // all data sources have a colon somewhere
		{
			throw new RepositoryException("Cannot find relation '" + dataSourceOrRelation + "'");
		}
		// a data source
		return add(dataSourceOrRelation, IQueryBuilderJoin.LEFT_OUTER_JOIN, alias);
	}

	@JSFunction
	public QBJoin add(String dataSource) throws RepositoryException
	{
		return add(dataSource, IQueryBuilderJoin.LEFT_OUTER_JOIN, null);
	}

	@JSFunction
	public QBJoin add(String dataSource, int joinType, String alias) throws RepositoryException
	{
		String name;
		QBJoin join;
		if (alias == null)
		{
			name = new UUID().toString();
			join = null;
		}
		else
		{
			name = alias;
			join = joins.get(name);
		}
		if (join == null)
		{
			Table foreignTable = root.getTable(dataSource);
			join = addJoin(
				new QueryJoin(name, parent.getQueryTable(), new QueryTable(foreignTable.getSQLName(), foreignTable.getCatalog(), foreignTable.getSchema()),
					new AndCondition(), joinType), dataSource, name);
		}
		return join;
	}

	private QBJoin addJoin(ISQLTableJoin queryJoin, String dataSource, String name) throws RepositoryException
	{
		QBJoin join = new QBJoin(root, parent, dataSource, queryJoin, name);
		root.getQuery().addJoin(queryJoin);
		joins.put(name, join);
		return join;
	}

	/**
	 * @param tableAlias
	 * @return
	 */
	public QBTableClause findQueryBuilderTableClause(String tableAlias)
	{
		QBJoin join = joins.get(tableAlias);
		if (join != null)
		{
			return join;
		}

		// not a direct child, try recursive
		for (QBJoin j : joins.values())
		{
			QBTableClause found = ((QBTableClause)j).findQueryBuilderTableClause(tableAlias);
			if (found != null)
			{
				return found;
			}
		}
		// not found
		return null;
	}
}
