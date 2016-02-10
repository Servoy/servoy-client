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

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderTableClause;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.ServoyException;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public abstract class QBTableClause extends QBPart implements IQueryBuilderTableClause
{
	private final String dataSource;
	protected String tableAlias;

	private QBJoins joins;

	private Table table;
	private final Map<String, QBColumn> columns = new HashMap<String, QBColumn>();
	private QBColumns builderColumns;

	QBTableClause(String dataSource, String tableAlias)
	{
		super();
		this.tableAlias = tableAlias;
		this.dataSource = dataSource;
	}

	QBTableClause(QBSelect root, QBTableClause parent, String dataSource, String tableAlias)
	{
		super(root, parent);
		this.dataSource = dataSource;
		this.tableAlias = tableAlias;
	}

	/**
	 * Returns the datasource for this.
	 *
	 * @return the dataSource
	 */
	@JSFunction
	public String getDataSource()
	{
		return dataSource;
	}

	/**
	 *  Returns the table alias for this.
	 *
	 * @return the tableAlias
	 */
	@JSFunction
	public String getTableAlias()
	{
		if (tableAlias == null)
		{
			// use the alias as was set on the table.
			// Freeze the value so that when the table is (de)serialized this value does not change
			tableAlias = getQueryTable().getAliasFrozen();
		}

		return tableAlias;
	}

	abstract BaseQueryTable getQueryTable();


	/**
	 * Get all the columns of the datasource that can be used for this query (select or where clause)
	 * @sample
	 * var query = foundset.getQuery();
	 * query.result.add(query.columns.name, "name");
	 * query.where.add(query.columns.orderdate.isNull)
	 */
	@JSReadonlyProperty
	public QBColumns columns() throws ServoyException
	{
		if (builderColumns == null)
		{
			builderColumns = new QBColumns(getRoot().getScriptableParent());
			for (String columnName : getTable().getDataProviderIDs())
			{
				builderColumns.put(columnName, getRoot().getScriptableParent(), getColumn(columnName));
			}
			builderColumns.setLocked(true);
		}
		return builderColumns;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderTableClause#joins()
	 * @sample
	 * foundset.getQuery().joins
	 */
	@JSReadonlyProperty
	public QBJoins joins()
	{
		if (joins == null)
		{
			joins = new QBJoins(getRoot(), this);
		}
		return joins;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderTableClause#getColumn(String)
	 * @sample
	 * foundset.getQuery().getColumn('orderid')
	 *
	 * @param name the name of column to get
	 */
	@JSFunction
	public QBColumn getColumn(String name) throws RepositoryException
	{
		QBColumn builderColumn = columns.get(name);
		if (builderColumn == null)
		{
			Column col = getTable().getColumn(name);
			if (col == null)
			{
				throw new RepositoryException("Cannot find column '" + name + "' in data source '" + dataSource + "'");
			}
			columns.put(name,
				builderColumn = new QBColumn(getRoot(), this, new QueryColumn(getQueryTable(), col.getID(), col.getSQLName(), col.getType(), col.getLength(),
					col.getScale(), col.getFlags(), false)));
		}
		return builderColumn;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderTableClause#getColumn(String, String)
	 * @sample
	 * foundset.getQuery().getColumn('orderid', 'opk')
	 *
	 * @param columnTableAlias the alias for the table
	 * @param name the name of column to get
	 */
	@JSFunction
	public QBColumn getColumn(String columnTableAlias, String name) throws RepositoryException
	{
		if (columnTableAlias == null)
		{
			throw new IllegalArgumentException("null tableAlias for getColumn");
		}
		QBTableClause queryBuilderTableClause = getRoot().findQueryBuilderTableClause(columnTableAlias);
		if (queryBuilderTableClause == null)
		{
			throw new RepositoryException("Cannot find table(alias) '" + columnTableAlias + "'");
		}
		return queryBuilderTableClause.getColumn(name);
	}

	Table getTable()
	{
		if (table == null)
		{
			table = getRoot().getTable(dataSource);
		}
		return table;
	}

	QBTableClause findQueryBuilderTableClause(String columnTableAlias)
	{
		if (tableAlias != null && tableAlias.equals(columnTableAlias))
		{
			return this;
		}
		if (joins != null)
		{
			return joins.findQueryBuilderTableClause(columnTableAlias);
		}
		return null;
	}
}
