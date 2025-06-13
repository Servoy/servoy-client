/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import static com.servoy.j2db.query.ColumnType.getColumnType;
import static com.servoy.j2db.query.QueryAggregate.ASTERIX;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.querybuilder.IQueryBuilderAggregates;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBAggregates</code> class provides a collection of aggregate functions that can be
 * utilized within a <code>QBSelect</code> query. These functions enable the creation of expressions
 * for common operations such as averages, counts, maximums, minimums, and sums. They are designed
 * to work within query results or as part of grouping and filtering logic.</p>
 *
 * <p>Key methods include <code>avg</code>, <code>count</code>, <code>max</code>, <code>min</code>,
 * and <code>sum</code>, which allow you to compute aggregate values for specific columns or
 * expressions. Additionally, the <code>parent</code> and <code>root</code> properties give access
 * to the parent query or table clause, facilitating complex query structures and subqueries.</p>
 *
 * <p>For more information about constructing and executing queries, refer to
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbselect">QBSelect</a> section of the documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBAggregates extends QBPart implements IQueryBuilderAggregates
{
	QBAggregates(QBSelect parent)
	{
		super(parent, parent);
	}

	@Override
	@JSReadonlyProperty(debuggerRepresentation = "Query parent part")
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#count()
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.count().add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 *
	 *  @return A QBCountAggregate object representing the count operation.
	 */
	@JSFunction
	@Override
	public QBCountAggregate count()
	{
		return count(ASTERIX);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#count(Object)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.count(query.columns.amount)).add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 *
	 *  @param aggregee The column, expression, or value to count. Can also be a special value like "*" for counting all rows.
	 *
	 *  @return A QBCountAggregate object representing the count operation with the specified aggregee.
	 */
	@JSFunction
	@Override
	public QBCountAggregate count(Object aggregee)
	{
		IQuerySelectValue operand = createOperand(aggregee, aggregee instanceof Number ? IColumnTypes.INTEGER : 0);
		if (operand instanceof QueryColumnValue queryColumnValue && (aggregee instanceof Number || ASTERIX.equals(aggregee)))
		{
			operand = queryColumnValue.withFixedvalue(true);
		}

		return new QBAggregateImpl(getRoot(), getParent(), operand, QueryAggregate.COUNT, QueryAggregate.ALL);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#avg(Object)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.avg(query.columns.amount)).add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 *
	 *  @param aggregee The column or expression to calculate the average for.
	 *
	 *  @return A QBColumn object representing the average operation for the specified aggregee.
	 */
	@JSFunction
	@Override
	public QBGenericColumnBase avg(Object aggregee)
	{
		return createAggregate(aggregee, QueryAggregate.AVG);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#max(Object)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.max(query.columns.amount)).add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 *
	 *  @param aggregee The column or expression to calculate the maximum value for. This can be a specific column or a computed expression.
	 *
	 *  @return A QBAggregate object representing the maximum value operation for the specified aggregee.
	 */
	@JSFunction
	@Override
	public QBGenericColumnBase max(Object aggregee)
	{
		return createAggregate(aggregee, QueryAggregate.MAX);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#min(Object)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.min(query.columns.amount)).add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 *
	 * @param aggregee The column or expression to calculate the minimum value for. This can be a specific column or a computed expression.
	 *
	 * @return A QBColumn object representing the minimum value operation for the specified aggregee.
	 */
	@JSFunction
	@Override
	public QBGenericColumnBase min(Object aggregee)
	{
		return createAggregate(aggregee, QueryAggregate.MIN);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#sum(Object)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.sum(query.columns.amount)).add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 *
	 * @param aggregee The column or expression to calculate the sum for. This can be a specific column or a computed expression.
	 *
	 * @return A QBColumn object representing the sum operation for the specified aggregee.
	 */
	@JSFunction
	@Override
	public QBGenericColumnBase sum(Object aggregee)
	{
		return createAggregate(aggregee, QueryAggregate.SUM);
	}

	protected QBGenericColumnBase createAggregate(Object aggregee, int aggregateType)
	{
		return new QBAggregateImpl(getRoot(), getParent(), getRoot().createOperand(aggregee, null, 0), aggregateType, QueryAggregate.ALL);
	}

	protected IQuerySelectValue createOperand(Object value, int type)
	{
		return getRoot().createOperand(value, getColumnType(type), 0);
	}

	@Override
	public String toString()
	{
		return "QBAggregates(Helper class for creating aggregates)";
	}
}
