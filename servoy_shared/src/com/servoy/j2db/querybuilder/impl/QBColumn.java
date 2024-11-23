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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * A column from a QBSelect. Used for different conditions.
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBColumn")
public interface QBColumn extends QBColumnRagtest<QBColumn>, QBColumnCompare
{
	/**
	 * Compare column with null.
	 * @sample
	 * query.where.add(query.columns.flag.isNull)
	 */
	@JSReadonlyProperty
	default QBCondition isNull()
	{
		return eq(null);
	}

	/**
	 * Create a negated condition.
	 * @sample
	 * query.where.add(query.columns.flag.not.eq(1))
	 *
	 */
	@JSReadonlyProperty
	QBColumnCompare not();


	/**
	 * Create an ascending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.asc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	QBSort asc();

	/**
	 * Create an descending sort expression
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.desc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	QBSort desc();

	/**
	 * Create an aggregate count expression.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * 	.root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	QBCountAggregate count();

	//
//	/**
//	 * Create sqrt(column) expression
//	 * @sample
//	 * query.result.add(query.columns.custname.sqrt)
//	 */
//	@JSReadonlyProperty
//	public QBFunction sqrt()
//	{
//		return getRoot().functions().sqrt(this);
//	}
//
//	/**
//	 * Create lower(column) expression
//	 * @sample
//	 * query.result.add(query.columns.custname.lower)
//	 */
//	@JSReadonlyProperty
//	public QBFunction lower()
//	{
//		return getRoot().functions().lower(this);
//	}
//
//	/**
//	 * Create trim(column) expression
//	 * @sample
//	 * query.result.add(query.columns.custname.trim)
//	 */
//	@JSReadonlyProperty
//	public QBFunction trim()
//	{
//		return getRoot().functions().trim(this);
//	}
//
//	/**
//	 * Create length(column) expression
//	 * @sample
//	 * query.result.add(query.columns.custname.len)
//	 */
//	@JSReadonlyProperty
//	public QBFunction len()
//	{
//		return length();
//	}
//
//	public QBFunction length()
//	{
//		return getRoot().functions().length(this);
//	}
//
//	/**
//	 * Create bit_length(column) expression
//	 * @sample
//	 * query.result.add(query.columns.custname.bit_length)
//	 */
//	@JSReadonlyProperty
//	public QBFunction bit_length()
//	{
//		return getRoot().functions().bit_length(this);
//	}
//
//	/**
//	 * Create cast(column, type) expression
//	 * @param type string type, see QUERY_COLUMN_TYPES
//	 * @sample
//	 * query.result.add(query.columns.mycol.cast(QUERY_COLUMN_TYPES.TYPE_INTEGER))
//	 */
//	@JSFunction
//	public QBFunction cast(String type)
//	{
//		return getRoot().functions().cast(this, type);
//	}
//
//	/**
//	 * Create substring(pos) expression
//	 * @param pos
//	 * @sample
//	 * query.result.add(query.columns.mycol.substring(3))
//	 */
//	@JSFunction
//	public QBFunction substring(int pos)
//	{
//		return getRoot().functions().substring(this, pos);
//	}
//
//	/**
//	 * Create substring(pos, len) expression
//	 * @param pos
//	 * @param len
//	 * @sample
//	 * query.result.add(query.columns.mycol.substring(3, 2))
//	 */
//	@JSFunction
//	public QBFunction substring(int pos, int len)
//	{
//		return getRoot().functions().substring(this, pos, len);
//	}
//
//	/**
//	 * Create locate(arg) expression
//	 * @param arg string to locate
//	 * @sample
//	 * query.result.add(query.columns.mycol.locate('sample'))
//	 */
//	@JSFunction
//	public QBFunction locate(Object arg)
//	{
//		return getRoot().functions().locate(arg, this);
//	}
//
//	/**
//	 * Create locate(arg, start) expression
//	 * @param arg string to locate
//	 * @param start start pos
//	 * @sample
//	 * query.result.add(query.columns.mycol.locate('sample', 5))
//	 */
//	@JSFunction
//	public QBFunction locate(Object arg, int start)
//	{
//		return getRoot().functions().locate(arg, this, start);
//	}
//

//
//	/**
//	 * Create mod(arg) expression
//	 * @param arg mod arg
//	 * @sample
//	 * query.result.add(query.columns.mycol.mod(2))
//	 */
//	@JSFunction
//	public QBFunction mod(Object arg)
//	{
//		return getRoot().functions().mod(this, arg);
//	}
//
//	/**
//	 * Add up value
//	 * @param arg nr to add
//	 * @sample
//	 * query.result.add(query.columns.mycol.plus(2))
//	 */
//	@JSFunction
//	public QBFunction plus(Object arg)
//	{
//		return getRoot().functions().plus(this, arg);
//	}
//
//	/**
//	 * Subtract value
//	 * @param arg nr to subtract
//	 * @sample
//	 * query.result.add(query.columns.mycol.minus(2))
//	 */
//	@JSFunction
//	public QBFunction minus(Object arg)
//	{
//		return getRoot().functions().minus(this, arg);
//	}
//
//	/**
//	 * Multiply with value
//	 * @param arg nr to multiply with
//	 * @sample
//	 * query.result.add(query.columns.mycol.multiply(2))
//	 */
//	@JSFunction
//	public QBFunction multiply(Object arg)
//	{
//		return getRoot().functions().multiply(this, arg);
//	}
//
//	/**
//	 * Divide by value
//	 * @param arg nr to divide by
//	 * @sample
//	 * query.result.add(query.columns.mycol.divide(2))
//	 */
//	@JSFunction
//	public QBFunction divide(Object arg)
//	{
//		return getRoot().functions().divide(this, arg);
//	}
//
//	/**
//	 * Concatenate with value
//	 * @param arg value to concatenate with
//	 * @sample
//	 * query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname))
//	 */
//	@JSFunction
//	public QBFunction concat(Object arg)
//	{
//		return getRoot().functions().concat(this, arg);
//	}
//
//	/**
//	 * Create floor(column) expression
//	 * @sample
//	 * query.result.add(query.columns.mycol.floor)
//	 */
//	@JSReadonlyProperty
//	public QBFunction floor()
//	{
//		return getRoot().functions().floor(this);
//	}
//
//	/**
//	 * Create round(column) expression
//	 * @sample
//	 * query.result.add(query.columns.mycol.round)
//	 */
//	@JSReadonlyProperty
//	public QBFunction round()
//	{
//		return getRoot().functions().round(this);
//	}
//
//	/**
//	 * Create ceil(column) expression
//	 * @sample
//	 * query.result.add(query.columns.mycol.ceil)
//	 */
//	@JSReadonlyProperty
//	public QBFunction ceil()
//	{
//		return getRoot().functions().ceil(this);
//	}
//
//	/**
//	 * Extract second from date
//	 * @sample
//	 * query.result.add(query.columns.mydatecol.second)
//	 */
//	@JSReadonlyProperty
//	public QBFunction second()
//	{
//		return getRoot().functions().second(this);
//	}
//
//	/**
//	 * Extract minute from date
//	 * @sample
//	 * query.result.add(query.columns.mydatecol.minute)
//	 */
//	@JSReadonlyProperty
//	public QBFunction minute()
//	{
//		return getRoot().functions().minute(this);
//	}
//
//	//@JSIgnore // RAGTEST doc
//	@Override
//	public QBFunction hourragtest()
//	{
//		return getRoot().functions().hour(this);
//	}
//
//
//	/**
//	 * Extract day from date
//	 * @sample
//	 * query.result.add(query.columns.mydatecol.day)
//	 */
//	@JSReadonlyProperty
//	public QBFunction day()
//	{
//		return getRoot().functions().day(this);
//	}
//
//	/**
//	 * Extract month from date
//	 * @sample
//	 * query.result.add(query.columns.mydatecol.month)
//	 */
//	@JSReadonlyProperty
//	public QBFunction month()
//	{
//		return getRoot().functions().month(this);
//	}
//
//	/**
//	 * Extract year from date
//	 * @sample
//	 * query.result.add(query.columns.mydatecol.year)
//	 */
//	@JSReadonlyProperty
//	public QBFunction year()
//	{
//		return getRoot().functions().year(this);
//	}
//
//	@Override
//	public BaseColumnType getColumnType()
//	{
//		return getQuerySelectValue().getColumnType();
//	}
//
//	/**
//	 * Column type as a string
//	 */
//	@JSFunction
//	public String getTypeAsString()
//	{
//		BaseColumnType columnType = getColumnType();
//		return columnType != null ? Column.getDisplayTypeString(columnType.getSqlType()) : null;
//	}
//
//	/**
//	 * 	The flags are a bit pattern consisting of 1 or more of the following bits:
//	 *  - JSColumn.UUID_COLUMN
//	 *  - JSColumn.EXCLUDED_COLUMN
//	 *  - JSColumn.TENANT_COLUMN
//	 */
//	@Override
//	@JSFunction
//	public int getFlags()
//	{
//		return getQuerySelectValue().getFlags();
//	}


}
