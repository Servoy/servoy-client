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
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilderTableClause;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.ServoyException;

/**
 * The <code>QBTableClause</code> class is a fundamental component for building queries within the Servoy environment.
 * It manages properties related to the table, such as `dataSource` and `tableAlias`, and provides functionality
 * to retrieve or create columns for the query.
 *
 * <p>The class offers methods for managing the columns associated with a data source, such as <code>getColumn()</code>
 * to retrieve specific columns by name, and <code>columns()</code> to get all available columns.</p>
 *
 * <p>It allows for complex query construction by supporting joins, accessible via the <code>joins()</code> method,
 * which handles relationships between tables. The <code>getTable()</code> method provides access to the underlying
 * table associated with the data source, while <code>getColumnNames()</code> retrieves all the column names in the table.</p>
 *
 * <p>Additionally, the `QBTableClause` class provides a way to find other `QBTableClause` objects through table aliases,
 * using the <code>findQueryBuilderTableClause()</code> method.</p>
 *
 * <p>The class also facilitates the dynamic creation of columns when needed, and it ensures that the correct
 * <code>QBColumn</code> objects are available for query building. By supporting query table retrieval, column management,
 * and joins, <code>QBTableClause</code> is integral to structuring and executing complex database queries
 * in the Servoy environment.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public abstract class QBTableClause extends QBPart implements IQueryBuilderTableClause
{
	private final String dataSource;
	protected String tableAlias;

	private QBJoins joins;

	private ITable table;
	private final Map<String, QBColumn> columns = new HashMap<>();

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
			for (String columnName : getColumnNames())
			{
				builderColumns.put(columnName, getRoot().getScriptableParent(), getColumn(columnName));
			}
			builderColumns.setLocked(true);
		}
		return builderColumns;
	}

	protected String[] getColumnNames() throws RepositoryException
	{
		ITable tbl = getTable();
		return tbl == null ? new String[0] : tbl.getDataProviderIDs();
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
	 *
	 *  @return the QBColumn representing the specified column name.
	 */
	@JSFunction
	public QBColumn getColumn(String name) throws RepositoryException
	{
		QBColumn builderColumn = columns.get(name);
		if (builderColumn == null)
		{
			columns.put(name, builderColumn = createColumn(name));
		}
		return builderColumn;
	}

	protected QBColumn createColumn(String name) throws RepositoryException
	{
		ITable tbl = getTable();
		Column col = tbl == null ? null : tbl.getColumn(name);
		if (col == null)
		{
			throw new RepositoryException("Cannot find column '" + name + "' in data source '" + dataSource + "'");
		}
		return new QBColumnImpl(getRoot(), this, col.queryColumn(getQueryTable()));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderTableClause#getColumn(String, String)
	 * @sample
	 * foundset.getQuery().getColumn('orderid', 'opk')
	 *
	 * @param columnTableAlias the alias for the table
	 * @param name the name of column to get
	 *
	 *  @return the QBColumn representing the specified column from the table with the given alias.
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

	/*
	 * Get the table, returns null when the datasource does not refer to a physical table or the table cannot be found
	 */
	protected ITable getTable()
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
