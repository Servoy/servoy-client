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
	@JSReadonlyProperty
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
	 */
	@JSFunction
	@Override
	public QBAggregate count()
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
	 */
	@JSFunction
	@Override
	public QBAggregate count(Object aggregee)
	{
		IQuerySelectValue operand = createOperand(aggregee, aggregee instanceof Number ? IColumnTypes.INTEGER : 0);
		if (operand instanceof QueryColumnValue && (aggregee instanceof Number || ASTERIX.equals(aggregee)))
		{
			operand = ((QueryColumnValue)operand).withFixedvalue(true);
		}

		return new QBAggregate(getRoot(), getParent(), operand, QueryAggregate.COUNT, QueryAggregate.ALL);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderAggregates#avg(Object)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.aggregates.avg(query.columns.amount)).add(query.columns.countryCode)
	 * 	query.groupBy.add(query.columns.countryCode)
	 *  var ds = databaseManager.getDataSetByQuery(query, 100);
	 */
	@JSFunction
	@Override
	public QBAggregate avg(Object aggregee)
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
	 */
	@JSFunction
	@Override
	public QBAggregate max(Object aggregee)
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
	 */
	@JSFunction
	@Override
	public QBAggregate min(Object aggregee)
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
	 */
	@JSFunction
	@Override
	public QBAggregate sum(Object aggregee)
	{
		return createAggregate(aggregee, QueryAggregate.SUM);
	}

	protected QBAggregate createAggregate(Object aggregee, int aggregateType)
	{
		return new QBAggregate(getRoot(), getParent(), getRoot().createOperand(aggregee, null, 0), aggregateType, QueryAggregate.ALL);
	}

	protected IQuerySelectValue createOperand(Object value, int type)
	{
		return getRoot().createOperand(value, getColumnType(type), 0);
	}
}
