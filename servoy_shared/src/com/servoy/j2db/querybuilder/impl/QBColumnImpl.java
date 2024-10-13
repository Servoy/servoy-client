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

import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.QueryCustomSelect;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderPart;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * A column from a QBSelect. Used for different conditions.
 *
 * @author rgansevles
 *
 */
public class QBColumnImpl extends QBPart implements QBGenericColumn, QBColumnRagtest<QBGenericColumn>
{
	private final IQuerySelectValue queryColumn;
	protected final boolean negate;

	QBColumnImpl(QBSelect root, QBTableClause queryBuilderTableClause, IQuerySelectValue queryColumn)
	{
		this(root, queryBuilderTableClause, queryColumn, false);
	}

	QBColumnImpl(QBSelect root, QBTableClause parent, IQuerySelectValue queryColumn, boolean negate)
	{
		super(root, parent);
		this.queryColumn = queryColumn;
		this.negate = negate;
	}

	protected QBCondition createCompareCondition(int operator, Object value)
	{
		if (value instanceof IQueryBuilder)
		{
			// condition with subquery
			try
			{
				return createCondition(new SetCondition(operator, new IQuerySelectValue[] { getQuerySelectValue() }, ((IQueryBuilder)value).build(), true));
			}
			catch (RepositoryException e)
			{
				// does not happen
				throw new RuntimeException(e);
			}
		}

		// in case the value is a parameter that will hold a query the query will be used.
		return createCondition(new CompareCondition(operator, this.getQuerySelectValue(), createOperand(value)));
	}

	protected QBCondition createCondition(ISQLCondition queryCondition)
	{
		return new QBCondition(getRoot(), getParent(), negate ? queryCondition.negate() : queryCondition);
	}

	protected IQuerySelectValue getQueryColumn()
	{
		return queryColumn;
	}

	public IQuerySelectValue getQuerySelectValue()
	{
		return queryColumn;
	}

	protected IQuerySelectValue createOperand(Object value)
	{
		IQuerySelectValue querySelectValue = getQuerySelectValue();
		return getRoot().createOperand(value, querySelectValue.getColumnType(), querySelectValue.getFlags());
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#gt(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.gt(0))
	 */
	@Override
	@JSFunction
	public QBCondition gt(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.GT_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#lt(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.lt(99))
	 */
	@Override
	@JSFunction
	public QBCondition lt(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.LT_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#ge(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.ge(2))
	 */
	@Override
	@JSFunction
	public QBCondition ge(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.GTE_OPERATOR, value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderColumn#le(Object)
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.le(2))
	 */
	@Override
	@JSFunction
	public QBCondition le(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.LTE_OPERATOR, value);
	}

	/**
	 * Compare column to a range of 2 values or other columns.
	 * @param value1
	 * @param value2
	 * @sample
	 * query.where.add(query.columns.flag.between(0, 5))
	 */
	@Override
	@JSFunction
	public QBCondition between(Object value1, Object value2)
	{
		return createCondition(
			new CompareCondition(IBaseSQLCondition.BETWEEN_OPERATOR, getQuerySelectValue(), new Object[] { createOperand(value1), createOperand(value2) }));
	}

	/**
	 * Compare column with subquery result.
	 * @param query subquery
	 * @sample
	 * query.where.add(query.columns.flag.isin(query2))
	 */
	@Override
	public QBCondition js_isin(QBPart query)
	{
		return in(query);
	}

	/**
	 * Compare column with custom query result.
	 * @param customQuery custom query
	 * @param args query arguments
	 * @sample
	 * query.where.add(query.columns.ccy.isin("select ccycode from currencies c where c.category = " + query.getTableAlias() + ".currency_category and c.flag = ?", ['T']))
	 */
	@Override
	public QBCondition js_isin(String customQuery, Object[] args)
	{
		return in(customQuery, args);
	}

	@Override
	public QBCondition in(IQueryBuilderPart query)
	{
		return createCompareCondition(IBaseSQLCondition.IN_OPERATOR, query);
	}

	/**
	 * Compare column with values.
	 * @param values array of values
	 * @sample
	 * query.where.add(query.columns.flag.isin([1, 5, 99]))
	 */
	@Override
	public QBCondition js_isin(Object[] values)
	{
		return in(values);
	}

	@Override
	public QBCondition in(Object[] values)
	{
		IQuerySelectValue querySelectValue = getQuerySelectValue();
		return createCondition(new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { querySelectValue },
			new Object[][] { values == null ? new Object[0] : getRoot().createOperands(values, querySelectValue.getColumnType(), querySelectValue.getFlags()) },
			true));
	}

	@Override
	public QBCondition in(String customQuery, Object[] args)
	{
		return createCondition(
			new SetCondition(IBaseSQLCondition.IN_OPERATOR, new IQuerySelectValue[] { getQuerySelectValue() },
				new QueryCustomSelect(customQuery, args == null ? null : getRoot().createOperands(args, null, 0)), true));
	}

	/**
	 * Compare column with null.
	 * @sample
	 * query.where.add(query.columns.flag.isNull)
	 */
	@Override
	@JSReadonlyProperty
	public QBCondition isNull()
	{
		return eq(null);
	}

	/**
	 * Compare column with a value or another column.
	 * Operator: equals
	 * @param value
	 * @sample
	 * query.where.add(query.columns.flag.eq(1))
	 */
	@Override
	@JSFunction
	public QBCondition eq(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, value);
	}

	/**
	 * Compare column with a value or another column.
	 * Operator: like
	 *
	 * @param pattern the string value of the pattern
	 *
	 * @sample
	 * query.where.add(query.columns.companyname.like('Serv%'))
	 *
	 * // case-insensitive compares can be done using the upper (or lower) functions,
	 * // this can be useful when using for example German letters like ß,
	 * query.where.add(query.columns.companyname.upper.like(query.functions.upper('groß%')))
	 */
	@Override
	@JSFunction
	public QBCondition like(Object pattern)
	{
		if (pattern instanceof String)
		{
			// don't try to convert the pattern to the column type
			return createCondition(new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, getQuerySelectValue(), pattern));
		}
		return createCompareCondition(IBaseSQLCondition.LIKE_OPERATOR, pattern);
	}

	/**
	 * Compare column with a value or another column.
	 * Operator: like, with escape character
	 *
	 * @param pattern the string value of the pattern
	 * @param escape the escape char
	 *
	 * @sample
	 * query.where.add(query.columns.companyname.like('X_%', '_'))
	 */
	@Override
	@JSFunction
	public QBCondition like(Object pattern, char escape)
	{
		if (pattern instanceof String)
		{
			// don't try to convert the pattern to the column type
			return createCondition(
				new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, getQuerySelectValue(), new Object[] { pattern, String.valueOf(escape) }));
		}

		return createCondition(
			new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, getQuerySelectValue(), new Object[] { createOperand(pattern), String.valueOf(escape) }));
	}

	/**
	 * Create a negated condition.
	 * @sample
	 * query.where.add(query.columns.flag.not.eq(1))
	 *
	 */
	@Override
	@JSReadonlyProperty
	public QBColumn not()
	{
		return new QBColumnImpl(getRoot(), getParent(), getQuerySelectValue(), !negate);
	}

	/**
	 * Create an ascending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.asc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@Override
	@JSReadonlyProperty
	public QBSort asc()
	{
		return new QBSort(getRoot(), this, true);
	}

	/**
	 * Create an descending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.desc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@Override
	@JSReadonlyProperty
	public QBSort desc()
	{
		return new QBSort(getRoot(), this, false);
	}

	/**
	 * Create an aggregate count expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * 	foundset.loadRecords(query)
	 */
	@Override
	@JSReadonlyProperty
	public QBAggregate count()
	{
		return getRoot().aggregates().count(this);
	}

	/**
	 * Create an aggregate average expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.avg.eq(1))
	 * 	foundset.loadRecords(query)
	 */
	private QBColumn _avg()
	{
		return getRoot().aggregates().avg(this);
	}

	/**
	 * Create an aggregate max expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.max(10))
	 * 	foundset.loadRecords(query)
	 */
	private QBColumn _max()
	{
		return getRoot().aggregates().max(this);
	}

	/**
	 * Create an aggregate min expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.min(10))
	 * 	foundset.loadRecords(query)
	 */
	private QBColumn _min()
	{
		return getRoot().aggregates().min(this);
	}

	/**
	 * Create an aggregate sum expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.sum(10))
	 * 	foundset.loadRecords(query)
	 */
	@Override
	@JSReadonlyProperty
	public QBColumn sum()
	{
		return getRoot().aggregates().sum(this);
	}

	/**
	 * Create upper(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.upper)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction upper()
	{
		return getRoot().functions().upper(this);
	}

	/**
	 * Create abs(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.abs)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction abs()
	{
		return getRoot().functions().abs(this);
	}

	/**
	 * Create sqrt(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.sqrt)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction sqrt()
	{
		return getRoot().functions().sqrt(this);
	}

	/**
	 * Create lower(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.lower)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction lower()
	{
		return getRoot().functions().lower(this);
	}

	/**
	 * Create trim(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.trim)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction trim()
	{
		return getRoot().functions().trim(this);
	}

	/**
	 * Create length(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.len)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction len()
	{
		return length();
	}

	@Override
	public QBFunction length()
	{
		return getRoot().functions().length(this);
	}

	/**
	 * Create bit_length(column) expression
	 * @sample
	 * query.result.add(query.columns.custname.bit_length)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction bit_length()
	{
		return getRoot().functions().bit_length(this);
	}

	/**
	 * Create cast(column, type) expression
	 * @param type string type, see QUERY_COLUMN_TYPES
	 * @sample
	 * query.result.add(query.columns.mycol.cast(QUERY_COLUMN_TYPES.TYPE_INTEGER))
	 */
	@Override
	@JSFunction
	public QBFunction cast(String type)
	{
		return getRoot().functions().cast(this, type);
	}

	/**
	 * Create substring(pos) expression
	 * @param pos
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3))
	 */
	@Override
	@JSFunction
	public QBFunction substring(int pos)
	{
		return getRoot().functions().substring(this, pos);
	}

	/**
	 * Create substring(pos, len) expression
	 * @param pos
	 * @param len
	 * @sample
	 * query.result.add(query.columns.mycol.substring(3, 2))
	 */
	@Override
	@JSFunction
	public QBFunction substring(int pos, int len)
	{
		return getRoot().functions().substring(this, pos, len);
	}

	/**
	 * Create locate(arg) expression
	 * @param arg string to locate
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample'))
	 */
	@Override
	@JSFunction
	public QBFunction locate(Object arg)
	{
		return getRoot().functions().locate(arg, this);
	}

	/**
	 * Create locate(arg, start) expression
	 * @param arg string to locate
	 * @param start start pos
	 * @sample
	 * query.result.add(query.columns.mycol.locate('sample', 5))
	 */
	@Override
	@JSFunction
	public QBFunction locate(Object arg, int start)
	{
		return getRoot().functions().locate(arg, this, start);
	}

	/**
	 * Create nullif(arg) expression
	 * @param arg object to compare
	 * @sample
	 * query.result.add(query.columns.mycol.nullif('none'))
	 */
	@Override
	@JSFunction
	public QBFunction nullif(Object arg)
	{
		return getRoot().functions().nullif(this, arg);
	}

	/**
	 * Create mod(arg) expression
	 * @param arg mod arg
	 * @sample
	 * query.result.add(query.columns.mycol.mod(2))
	 */
	@Override
	@JSFunction
	public QBFunction mod(Object arg)
	{
		return getRoot().functions().mod(this, arg);
	}

	/**
	 * Add up value
	 * @param arg nr to add
	 * @sample
	 * query.result.add(query.columns.mycol.plus(2))
	 */
	@Override
	@JSFunction
	public QBFunction plus(Object arg)
	{
		return getRoot().functions().plus(this, arg);
	}

	/**
	 * Subtract value
	 * @param arg nr to subtract
	 * @sample
	 * query.result.add(query.columns.mycol.minus(2))
	 */
	@Override
	@JSFunction
	public QBFunction minus(Object arg)
	{
		return getRoot().functions().minus(this, arg);
	}

	/**
	 * Multiply with value
	 * @param arg nr to multiply with
	 * @sample
	 * query.result.add(query.columns.mycol.multiply(2))
	 */
	@Override
	@JSFunction
	public QBFunction multiply(Object arg)
	{
		return getRoot().functions().multiply(this, arg);
	}

	/**
	 * Divide by value
	 * @param arg nr to divide by
	 * @sample
	 * query.result.add(query.columns.mycol.divide(2))
	 */
	@Override
	@JSFunction
	public QBFunction divide(Object arg)
	{
		return getRoot().functions().divide(this, arg);
	}

	/**
	 * Concatename with value
	 * @param arg valeu to concatenate with
	 * @sample
	 * query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname))
	 */
	@Override
	@JSFunction
	public QBFunction concat(Object arg)
	{
		return getRoot().functions().concat(this, arg);
	}

	/**
	 * Create floor(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.floor)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction floor()
	{
		return getRoot().functions().floor(this);
	}

	/**
	 * Create round(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.round)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction round()
	{
		return getRoot().functions().round(this);
	}

	/**
	 * Create ceil(column) expression
	 * @sample
	 * query.result.add(query.columns.mycol.ceil)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction ceil()
	{
		return getRoot().functions().ceil(this);
	}

	/**
	 * Extract second from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.second)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction second()
	{
		return getRoot().functions().second(this);
	}

	/**
	 * Extract minute from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.minute)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction minute()
	{
		return getRoot().functions().minute(this);
	}

	/**
	 * Extract hour from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.hour)
	 */
	@Override
	@JSReadonlyProperty
	public QBIntegerColumnBase hour()
	{
		return getRoot().functions().hour(this);
	}

	/**
	 * Extract day from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.day)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction day()
	{
		return getRoot().functions().day(this);
	}

	/**
	 * Extract month from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.month)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction month()
	{
		return getRoot().functions().month(this);
	}

	/**
	 * Extract year from date
	 * @sample
	 * query.result.add(query.columns.mydatecol.year)
	 */
	@Override
	@JSReadonlyProperty
	public QBFunction year()
	{
		return getRoot().functions().year(this);
	}

	@Override
	public QBGenericColumn avg()
	{
		return (QBGenericColumn)_avg();
	}

	@Override
	public QBGenericColumn max()
	{
		return (QBGenericColumn)_max();
	}

	@Override
	public QBGenericColumn min()
	{
		return (QBGenericColumn)_min();
	}

//RAGTEST	@Override
	public BaseColumnType getColumnType()
	{
		return getQuerySelectValue().getColumnType();
	}

	/**
	 * Column type as a string
	 */
	@JSFunction
	public String getTypeAsString()
	{
		BaseColumnType columnType = getColumnType();
		return columnType != null ? Column.getDisplayTypeString(columnType.getSqlType()) : null;
	}

	/**
	 * 	The flags are a bit pattern consisting of 1 or more of the following bits:
	 *  - JSColumn.UUID_COLUMN
	 *  - JSColumn.EXCLUDED_COLUMN
	 *  - JSColumn.TENANT_COLUMN
	 */
	//RAGTEST @Override
	@JSFunction
	public int getFlags()
	{
		return getQuerySelectValue().getFlags();
	}

	@Override
	public String toString()
	{
		IQuerySelectValue querySelectValue = getQuerySelectValue();
		return (negate ? "!" : "") + (querySelectValue == null ? "<NONE>" : querySelectValue.toString());
	}
}
