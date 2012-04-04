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
	private final IQuerySelectValue queryColumn;
	private final boolean negate;

	QBColumn(QBSelect root, QBTableClause queryBuilderTableClause, IQuerySelectValue queryColumn)
	{
		this(root, queryBuilderTableClause, queryColumn, false);
	}

	QBColumn(QBSelect root, QBTableClause parent, IQuerySelectValue queryColumn, boolean negate)
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
	 * @param value
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
	 * @param value
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
	 * @param value
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
	 * @param value
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
	 * @param value1
	 * @param value2
	 * @sample
	 * query.where.add(query.columns.flag.between(0, 5))
	 */
	@JSFunction
	public QBCondition between(Object value1, Object value2)
	{
		return createCondition(new CompareCondition(ISQLCondition.BETWEEN_OPERATOR, getQuerySelectValue(),
			new Object[] { getRoot().createOperand(value1), getRoot().createOperand(value2) }));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#in(IQueryBuilderPart)
	 * @param query subquery
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
	 * @param values array of values
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
	 * query.where.add(query.columns.flag.isNull)
	 */
	@JSReadonlyProperty
	public QBCondition isNull()
	{
		return eq(null);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#eq(Object)
	 * @param value
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
	 * @param value
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
	 * @param value string value
	 * @param ecape escape char
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
		return new QBColumn(getRoot(), getParent(), getQuerySelectValue(), !negate);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#asc()
	 * @sample
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
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
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
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
	 *  /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate count()
	{
		return new QBAggregate(getRoot(), getParent(), getQuerySelectValue(), QueryAggregate.COUNT);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#avg()
	 * @sample
	 *  /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.avg.eq(1))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate avg()
	{
		return new QBAggregate(getRoot(), getParent(), getQuerySelectValue(), QueryAggregate.AVG);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#max()
	 * @sample
	 *  /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.max(10))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate max()
	{
		return new QBAggregate(getRoot(), getParent(), getQuerySelectValue(), QueryAggregate.MAX);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#min()
	 * @sample
	 *  /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.min(10))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate min()
	{
		return new QBAggregate(getRoot(), getParent(), getQuerySelectValue(), QueryAggregate.MIN);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#sum()
	 * @sample
	 *  /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.sum(10))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBAggregate sum()
	{
		return new QBAggregate(getRoot(), getParent(), getQuerySelectValue(), QueryAggregate.SUM);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#upper()
	 * @sample
	 * query.result.add(query.columns.custname.upper)
	 */
	@JSReadonlyProperty
	public QBFunction upper()
	{
		return getRoot().functions().upper(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#abs()
	 * @sample
	 * query.result.add(query.columns.custname.abs)
	 */
	@JSReadonlyProperty
	public QBFunction abs()
	{
		return getRoot().functions().abs(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#sqrt()
	 * @sample
	 * query.result.add(query.columns.custname.sqrt)
	 */
	@JSReadonlyProperty
	public QBFunction sqrt()
	{
		return getRoot().functions().sqrt(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#lower()
	 * @sample
	 * query.result.add(query.columns.custname.lower)
	 */
	@JSReadonlyProperty
	public QBFunction lower()
	{
		return getRoot().functions().lower(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#trim()
	 * @sample
	 * query.result.add(query.columns.custname.trim)
	 */
	@JSReadonlyProperty
	public QBFunction trim()
	{
		return getRoot().functions().trim(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#length()
	 * @sample
	 * query.result.add(query.columns.custname.len)
	 */
	@JSReadonlyProperty
	public QBFunction len()
	{
		return length();
	}

	public QBFunction length()
	{
		return getRoot().functions().length(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#bit_length()
	 * @sample
	 * query.result.add(query.columns.custname.bit_length)
	 */
	@JSReadonlyProperty
	public QBFunction bit_length()
	{
		return getRoot().functions().bit_length(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#cast(String)
	 * @param type string type, see QUERY_COLUMN_TYPES
	 * @sample
	 * query.result.add(query.columns.mycol.cast(QUERY_COLUMN_TYPES.TYPE_INTEGER))
	 */
	@JSFunction
	public QBFunction cast(String type)
	{
		return getRoot().functions().cast(this, type);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#substring(int)
	 * @param pos
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3))
	 */
	@JSFunction
	public QBFunction substring(int pos)
	{
		return getRoot().functions().substring(this, pos);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#substring(int, int)
	 * @param pos
	 * @param len
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3, 2))
	 */
	@JSFunction
	public QBFunction substring(int pos, int len)
	{
		return getRoot().functions().substring(this, pos, len);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#locate(Object)
	 * @param arg string to locate
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample'))
	 */
	@JSFunction
	public QBFunction locate(Object arg)
	{
		return getRoot().functions().locate(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#locate(Object, int)
	 * @param arg string to locate
	 * @param start start pos
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample', 5))
	 */
	@JSFunction
	public QBFunction locate(Object arg, int start)
	{
		return getRoot().functions().locate(this, arg, start);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#nullif(Object)
	 * @param arg object to compare
	 * @sample
	 * query.result.add(query.columns.mycol.nullif('none'))
	 */
	@JSFunction
	public QBFunction nullif(Object arg)
	{
		return getRoot().functions().nullif(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#mod(Object)
	 * @param arg mod arg
	 * @sample
	 * query.result.add(query.columns.mycol.mod(2))
	 */
	@JSFunction
	public QBFunction mod(Object arg)
	{
		return getRoot().functions().mod(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#plus(Object)
	 * @param arg nr to add
	 * @sample
	 * query.result.add(query.columns.mycol.plus(2))
	 */
	@JSFunction
	public QBFunction plus(Object arg)
	{
		return getRoot().functions().plus(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#minus(Object)
	 * @param arg nr to subtract
	 * @sample
	 * query.result.add(query.columns.mycol.minus(2))
	 */
	@JSFunction
	public QBFunction minus(Object arg)
	{
		return getRoot().functions().minus(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#multiply(Object)
	 * @param arg nr to multiply with
	 * @sample
	 * query.result.add(query.columns.mycol.multiply(2))
	 */
	@JSFunction
	public QBFunction multiply(Object arg)
	{
		return getRoot().functions().multiply(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#divide(Object)
	 * @param arg nr to divide by
	 * @sample
	 * query.result.add(query.columns.mycol.divide(2))
	 */
	@JSFunction
	public QBFunction divide(Object arg)
	{
		return getRoot().functions().divide(this, arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#floor()
	 * @sample
	 * query.result.add(query.columns.mycol.floor)
	 */
	@JSReadonlyProperty
	public QBFunction floor()
	{
		return getRoot().functions().floor(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#round()
	 * @sample
	 * query.result.add(query.columns.mycol.round)
	 */
	@JSReadonlyProperty
	public QBFunction round()
	{
		return getRoot().functions().round(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#ceil()
	 * @sample
	 * query.result.add(query.columns.mycol.ceil)
	 */
	@JSReadonlyProperty
	public QBFunction ceil()
	{
		return getRoot().functions().ceil(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#second()
	 * @sample
	 * query.result.add(query.columns.mydatecol.second)
	 */
	@JSReadonlyProperty
	public QBFunction second()
	{
		return getRoot().functions().second(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#minute()
	 * @sample
	 * query.result.add(query.columns.mydatecol.minute)
	 */
	@JSReadonlyProperty
	public QBFunction minute()
	{
		return getRoot().functions().minute(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#hour()
	 * @sample
	 * query.result.add(query.columns.mydatecol.hour)
	 */
	@JSReadonlyProperty
	public QBFunction hour()
	{
		return getRoot().functions().hour(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#day()
	 * @sample
	 * query.result.add(query.columns.mydatecol.day)
	 */
	@JSReadonlyProperty
	public QBFunction day()
	{
		return getRoot().functions().day(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#month()
	 * @sample
	 * query.result.add(query.columns.mydatecol.month)
	 */
	@JSReadonlyProperty
	public QBFunction month()
	{
		return getRoot().functions().month(this);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#year()
	 * @sample
	 * query.result.add(query.columns.mydatecol.year)
	 */
	@JSReadonlyProperty
	public QBFunction year()
	{
		return getRoot().functions().year(this);
	}

}
