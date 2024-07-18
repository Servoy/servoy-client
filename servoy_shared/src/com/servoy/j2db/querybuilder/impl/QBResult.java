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

import static com.servoy.j2db.util.Utils.iterate;
import static java.util.Arrays.stream;

import java.util.ArrayList;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryCustomSelect;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.query.QuerySearchedCaseExpression;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilder;
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
		ITable table = getParent().getTable();
		if (table != null)
		{
			for (String columnName : iterate(table.getRowIdentColumnNames()))
			{
				add(columnName);
			}
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
	public QBResult js_add(QBColumn column)
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
	public QBResult js_add(QBColumn column, String alias)
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
	public QBResult js_add(QBAggregate aggregate)
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
	public QBResult js_add(QBAggregate aggregate, String alias)
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
	public QBResult js_add(QBFunction func)
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
	public QBResult js_add(QBFunction func, String alias)
	{
		return add(func, alias);
	}

	/**
	 * Add a case searched expression to the query result.
	 *
	 * @param qcase The searched case expression.
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBSelect#js_case()
	 */
	public QBResult js_add(QBSearchedCaseExpression qcase)
	{
		return add(qcase);
	}

	/**
	 * Add a case searched expression with alias to the query result.
	 *
	 * @param qcase The searched case expression.
	 * @param alias function alias
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBSelect#js_case()
	 */
	public QBResult js_add(QBSearchedCaseExpression qcase, String alias)
	{
		return add(qcase, alias);
	}

	/**
	 * Add all columns from a query or a join to the query result.
	 * @sample
	 * query.result.add(query.columns)
	 * query.result.add(query.joins.orders_to_orderdetail.columns)
	 *
	 * @param columns columns to add to result
	 */
	public QBResult js_add(QBColumns columns)
	{
		stream(columns.getValues())
			.filter(IQueryBuilderColumn.class::isInstance)
			.map(IQueryBuilderColumn.class::cast)
			.forEach(this::add);
		return this;
	}

	public QBResult add(IQueryBuilderColumn column)
	{
		return add(column, null);
	}

	public QBResult add(IQueryBuilderColumn column, String alias)
	{
		if (column == null)
		{
			throw new RuntimeException("Cannot add null or undefined column to a query");
		}
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
		return columns == null ? new QBColumn[0] : columns.stream().map(selectValue -> {

			if (selectValue instanceof QueryAggregate)
			{
				QueryAggregate queryAggregate = (QueryAggregate)selectValue;
				return new QBAggregate(getRoot(), getParent(), selectValue, queryAggregate.getType(), queryAggregate.getQuantifier());
			}
			if (selectValue instanceof QueryFunction)
			{
				QueryFunctionType function = ((QueryFunction)selectValue).getFunction();
				return new QBFunction(getRoot(), getParent(), function, ((QueryFunction)selectValue).getArgs());
			}
			if (selectValue instanceof QuerySearchedCaseExpression)
			{
				return new QBSearchedCaseExpression(getRoot(), getParent(), ((QuerySearchedCaseExpression)selectValue));
			}
			return new QBColumn(getRoot(), getParent(), selectValue);

		}).toArray(QBColumn[]::new);
	}

	/**
	 * Add a value to the query result.
	 * @sample
	 * query.result.addValue(100)
	 *
	 * @param value value add to result
	 */
	@JSFunction
	public QBResult addValue(Object value)
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
	public QBResult addValue(Object value, String alias)
	{
		getParent().getQuery().addColumn(
			value instanceof IQuerySelectValue ? ((IQuerySelectValue)value).asAlias(alias) : new QueryColumnValue(value, alias, value instanceof Integer));
		return this;
	}

	/**
	 * Add a custom subquery to the query result.
	 * @sample
	 * // make sure the subquery returns exactly 1 value.
	 * // select (select max from othertab where val = 'test') from tab
	 * query.result.addSubSelect("select max(field) from othertab where val = ?", ["test"]);
	 *
	 * @param customQuery query to add to result
	 * @param args arguments to the query
	 */
	@JSFunction
	public QBResult addSubSelect(String customQuery, Object[] args)
	{
		return doAddSubSelect(new QueryCustomSelect(customQuery, args == null ? null : getRoot().createOperands(args, null, 0)), null);
	}

	/**
	 * Add a custom subquery with alias to the query result.
	 * @sample
	 * // make sure the subquery returns exactly 1 value.
	 * // select (select max from othertab where val = 'test') as mx from tab
	 * query.result.addSubSelect("select max from othertab where val = ?", ["test"], "mx");
	 *
	 * @param customQuery query to add to result
	 * @param args arguments to the query
	 * @param alias result alias
	 */
	@JSFunction
	public QBResult addSubSelect(String customQuery, Object[] args, String alias)
	{
		return doAddSubSelect(new QueryCustomSelect(customQuery, args == null ? null : getRoot().createOperands(args, null, 0)), alias);
	}

	public QBResult addSubSelect(IQueryBuilder query, String alias) throws RepositoryException
	{
		return doAddSubSelect(query.build(), alias);
	}

	public QBResult addSubSelect(IQueryBuilder query) throws RepositoryException
	{
		return doAddSubSelect(query.build(), null);
	}

	/**
	 * Add a query with alias to the query result.
	 * @sample
	 * // make sure the query returns exactly 1 value.
	 * query.result.addSubSelect(subquery, "mx");
	 *
	 * @param query query to add to result
	 * @param alias result alias
	 */
	public QBResult js_addSubSelect(QBSelect query, String alias) throws RepositoryException
	{
		return addSubSelect(query, alias);
	}

	/**
	 * Add a query to the query result.
	 * @sample
	 * // make sure the query returns exactly 1 value.
	 * query.result.addSubSelect(subquery);
	 *
	 * @param query query to add to result
	 */
	public QBResult js_addSubSelect(QBSelect query) throws RepositoryException
	{
		return addSubSelect(query);
	}

	protected QBResult doAddSubSelect(ISQLSelect select, String alias)
	{
		getParent().getQuery().addColumn(new QueryColumnValue(select, alias, false));
		return this;
	}


	/**
	 * Remove a column by name from the query result.
	 * @sample
	 * query.result.remove("custname")
	 *
	 * @param name name or alias of column to remove from the result
	 */
	@JSFunction
	public QBResult remove(String name) throws RepositoryException
	{
		QuerySelect query = getParent().getQuery();
		query.removeColumn(query.getColumn(name));
		return this;
	}

	/**
	 * remove a column from the query result.
	 * @sample
	 * query.result.remove(query.columns.custname)
	 *
	 * @param column column to remove from the result
	 */
	public QBResult js_remove(QBColumn column)
	{
		return remove(column);
	}

	public QBResult remove(IQueryBuilderColumn column)
	{
		if (column != null)
		{
			getParent().getQuery().removeColumn(((QBColumn)column).getQuerySelectValue());
		}
		return this;
	}

	public void js_setDistinct(boolean distinct)
	{
		setDistinct(distinct);
	}

	/**
	 * Get/set the distinct flag for the query.
	 * @sample
	 * query.result.distinct = true
	 */
	public boolean js_isDistinct()
	{
		return isDistinct();
	}

	public boolean isDistinct()
	{
		return getParent().getQuery().isDistinct();
	}

	public QBResult setDistinct(boolean distinct)
	{
		getParent().getQuery().setDistinct(distinct);
		return this;
	}
}
