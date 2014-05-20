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

import java.util.ArrayList;
import java.util.Iterator;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderResult;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBResult extends QBPart implements IQueryBuilderResult
{
	/**
	 * @param queryBuilder
	 */
	QBResult(QBSelect parent)
	{
		super(parent, parent);
	}

	@Override
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	/**
	 * Add the tables' primary pk columns in alphabetical order to the query result.
	 * @sample
	 * query.result.addPk()
	 */
	@JSFunction
	public QBResult addPk() throws RepositoryException
	{
		Iterator<String> rowIdentColumnNames = getParent().getTable().getRowIdentColumnNames();
		while (rowIdentColumnNames.hasNext())
		{
			add(rowIdentColumnNames.next());
		}
		return this;
	}

	/**
	 * Clear the columns in the query result.
	 * @sample
	 * query.result.clear()
	 */
	@JSFunction
	public QBResult clear()
	{
		getParent().getQuery().setColumns(null);
		return this;
	}

	/**
	 * Add a column by name to the query result.
	 * @sample
	 * query.result.add("custname")
	 */
	public QBResult add(String columnName) throws RepositoryException
	{
		return add(getParent().getColumn(columnName));
	}

	/**
	 * Add a column with alias by name to the query result.
	 * @sample
	 * query.result.add("custname", "customer_name")
	 */
	public QBResult add(String columnName, String alias) throws RepositoryException
	{
		return add(getParent().getColumn(columnName), alias);
	}

	/**
	 * Add a column to the query result.
	 * @sample
	 * query.result.add(query.columns.custname)
	 *
	 * @param column column to add to result
	 */
	public QBResult js_add(QBColumn column) throws RepositoryException
	{
		return add(column);
	}

	/**
	 * Add a column with alias to the query result.
	 * @sample
	 * query.result.add(query.columns.custname, 'customer_name')
	 *
	 * @param column column to add to result
	 * @param alias column alias
	 */
	public QBResult js_add(QBColumn column, String alias) throws RepositoryException
	{
		return add(column, alias);
	}

	/**
	 * Add an aggregate to the query result.
	 * @sample
	 * query.result.add(query.columns.label_text.max)
	 *
	 * @param aggregate the aggregate to add to result
	 */
	public QBResult js_add(QBAggregate aggregate) throws RepositoryException
	{
		return add(aggregate);
	}

	/**
	 * Add an aggregate with alias to the query result.
	 * @sample
	 * query.result.add(query.columns.item_count.max, 'maximum_items')
	 *
	 * @param aggregate the aggregate to add to result
	 * @param alias aggregate alias
	 */
	public QBResult js_add(QBAggregate aggregate, String alias) throws RepositoryException
	{
		return add(aggregate, alias);
	}

	/**
	 * Add a function result to the query result.
	 * @sample
	 * query.result.add(query.columns.custname.upper())
	 *
	 * @param func the function to add to the result
	 */
	public QBResult js_add(QBFunction func) throws RepositoryException
	{
		return add(func);
	}

	/**
	 * Add a function with alias result to the query result.
	 * @sample
	 * query.result.add(query.columns.custname.upper(), 'customer_name')
	 *
	 * @param func the function to add to the result
	 * @param alias function alias
	 */
	public QBResult js_add(QBFunction func, String alias) throws RepositoryException
	{
		return add(func, alias);
	}

	public QBResult add(IQueryBuilderColumn column) throws RepositoryException
	{
		return add(column, null);
	}

	public QBResult add(IQueryBuilderColumn column, String alias) throws RepositoryException
	{
		IQuerySelectValue querySelectValue = ((QBColumn)column).getQuerySelectValue();
		getParent().getQuery().addColumn(alias == null ? querySelectValue : querySelectValue.asAlias(alias));
		return this;
	}

	/**
	 * returns an array with all the columns that will be in the select of this query.
	 * can return empty array. Then the system will auto append the pk when this query is used.
	 * @sample
	 * var columns = query.result.getColumns();
	 *
	 * @return An array of QBColumn thats in the select of this query.
	 */
	@JSFunction
	public QBColumn[] getColumns()
	{
		ArrayList<IQuerySelectValue> columns = getParent().getQuery().getColumns();
		QBColumn[] result = new QBColumn[columns == null ? 0 : columns.size()];
		for (int i = 0; i < result.length; i++)
		{
			IQuerySelectValue selectValue = columns.get(i);
			if (selectValue instanceof QueryColumn)
			{
				result[i] = new QBColumn(getRoot(), getParent(), selectValue);
			}
			else if (selectValue instanceof QueryAggregate)
			{
				result[i] = new QBAggregate(getRoot(), getParent(), selectValue, ((QueryAggregate)selectValue).getType());
			}
			else if (selectValue instanceof QueryFunction)
			{
				result[i] = new QBFunction(getRoot(), getParent(), ((QueryFunction)selectValue).getFunction(), ((QueryFunction)selectValue).getArgs());
			}
		}
		return result;
	}

	/**
	 * Add a value to the query result.
	 * @sample
	 * query.result.addValue(100)
	 *
	 * @param value value add to result
	 */
	@JSFunction
	public QBResult addValue(Object value) throws RepositoryException
	{
		return addValue(value, null);
	}

	/**
	 * Add a value with an alias to the query result.
	 * @sample
	 * query.result.addValue(100, 'myvalue')
	 *
	 * @param value value add to result
	 * @param alias value alias
	 */
	@JSFunction
	public QBResult addValue(Object value, String alias) throws RepositoryException
	{
		getParent().getQuery().addColumn(new QueryColumnValue(value, alias, value instanceof Integer));
		return this;
	}

	public void js_setDistinct(boolean distinct) throws RepositoryException
	{
		setDistinct(distinct);
	}

	/**
	 * Get/set the distinct flag for the query.
	 * @sample
	 * query.result.distinct = true
	 */
	public boolean js_isDistinct() throws RepositoryException
	{
		return isDistinct();
	}

	public boolean isDistinct() throws RepositoryException
	{
		return getParent().getQuery().isDistinct();
	}

	public QBResult setDistinct(boolean distinct) throws RepositoryException
	{
		getParent().getQuery().setDistinct(distinct);
		return this;
	}
}
