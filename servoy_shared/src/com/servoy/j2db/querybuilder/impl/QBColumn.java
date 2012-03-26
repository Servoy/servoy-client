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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderPart;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBColumn extends QBPart implements IQueryBuilderColumn
{
	private final QueryColumn queryColumn;
	private final boolean negate;

	QBColumn(QBSelect root, QBTableClause queryBuilderTableClause, QueryColumn queryColumn)
	{
		this(root, queryBuilderTableClause, queryColumn, false);
	}

	QBColumn(QBSelect root, QBTableClause parent, QueryColumn queryColumn, boolean negate)
	{
		super(root, parent);
		this.queryColumn = queryColumn;
		this.negate = negate;
	}

	protected QBCondition createCompareCondition(int operator, Object value)
	{
		return createCondition(new CompareCondition(operator, this.getQuerySelectValue(), getRoot().createOperand(value)));
	}

	protected QBCondition createCondition(ISQLCondition queryCondition)
	{
		return new QBCondition(getRoot(), getParent(), negate ? queryCondition.negate() : queryCondition);
	}

	public IQuerySelectValue getQuerySelectValue()
	{
		return queryColumn;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#gt(Object)
	 * @sample
	 * query.where.add(query.columns.flag.gt(0))
	 */
	@JSFunction
	public QBCondition gt(Object value)
	{
		return createCompareCondition(ISQLCondition.GT_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#lt(Object)
	 * @sample
	 * query.where.add(query.columns.flag.lt(99))
	 */
	@JSFunction
	public QBCondition lt(Object value)
	{
		return createCompareCondition(ISQLCondition.LT_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#ge(Object)
	 * @sample
	 * query.where.add(query.columns.flag.ge(2))
	 */
	@JSFunction
	public QBCondition ge(Object value)
	{
		return createCompareCondition(ISQLCondition.GTE_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#le(Object)
	 * @sample
	 * query.where.add(query.columns.flag.le(2))
	 */
	@JSFunction
	public QBCondition le(Object value)
	{
		return createCompareCondition(ISQLCondition.LTE_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#between(Object, Object)
	 * @sample
	 * query.where.add(query.columns.flag.between(0, 5))
	 */
	@JSFunction
	public QBCondition between(Object value1, Object value2)
	{
		return createCompareCondition(ISQLCondition.BETWEEN_OPERATOR, new Object[] { value1, value2 });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#in(IQueryBuilderPart)
	 * @sample
	 * query.where.add(query.columns.flag.in(query2))
	 */
	public QBCondition js_isin(QBPart query) throws RepositoryException
	{
		return in(query);
	}

	public QBCondition in(IQueryBuilderPart query) throws RepositoryException
	{
		return createCondition(new SetCondition(ISQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { getQuerySelectValue() }, query.getRoot().build(), true));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#in(Object[])
	 * @sample
	 * query.where.add(query.columns.flag.in([1, 5, 99]))
	 */
	public QBCondition js_isin(Object[] values)
	{
		return in(values);
	}

	public QBCondition in(Object[] values)
	{
		return createCondition(new SetCondition(ISQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { getQuerySelectValue() },
			new Object[][] { values == null ? new Object[0] : values }, true));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#isNull()
	 * @sample
	 * query.where.add(query.columns.flag.isNull())
	 */
	@JSReadonlyProperty
	public QBCondition isNull()
	{
		return eq(null);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#eq(Object)
	 * @sample
	 * query.where.add(query.columns.flag.eq(1))
	 */
	@JSFunction
	public QBCondition eq(Object value)
	{
		return createCompareCondition(ISQLCondition.EQUALS_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#like(String)
	 * @sample
	 * query.where.add(query.columns.companyname.like('Serv%'))
	 */
	@JSFunction
	public QBCondition like(String pattern)
	{
		return createCompareCondition(ISQLCondition.LIKE_OPERATOR, pattern);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#like(String, char)
	 * @sample
	 * query.where.add(query.columns.companyname.like('X_%', '_'))
	 */
	@JSFunction
	public QBCondition like(String pattern, char escape)
	{
		return createCompareCondition(ISQLCondition.LIKE_OPERATOR, new Object[] { pattern, String.valueOf(escape) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#not()
	 * @sample
	 * query.where.add(query.columns.flag.not.eq(1))
	 */
	@JSReadonlyProperty
	public QBColumn not()
	{
		return new QBColumn(getRoot(), getParent(), queryColumn, !negate);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#asc()
	 * @sample
	 * var query = databaseManager.createSelect('db:/example_data/orders')
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.asc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBSort asc()
	{
		return new QBSort(getRoot(), this, true);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#desc()
	 * @sample
	 * var query = databaseManager.createSelect('db:/example_data/orders')
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.desc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBSort desc()
	{
		return new QBSort(getRoot(), this, false);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#count()
	 * @sample
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate count()
	{
		return new QBAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.COUNT);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#avg()
	 * @sample
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.avg.eq(1))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate avg()
	{
		return new QBAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.AVG);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#max()
	 * @sample
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.max(10))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate max()
	{
		return new QBAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.MAX);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#min()
	 * @sample
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.min(10))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate min()
	{
		return new QBAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.MIN);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#sum()
	 * @sample
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.sum(10))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate sum()
	{
		return new QBAggregate(getRoot(), getParent(), queryColumn, QueryAggregate.SUM);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#upper()
	 * @sample
	 * query.result.add(query.columns.custname.upper())
	 */
	@JSReadonlyProperty
	public QBFunction upper()
	{
		return new QBFunction(getRoot(), getParent(), queryColumn, QueryFunction.UPPER);
	}

	// TODO: add more functions
}
