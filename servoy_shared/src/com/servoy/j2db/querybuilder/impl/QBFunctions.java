/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import static com.servoy.j2db.persistence.IColumnTypes.TEXT;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.abs;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.bit_length;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.cardinality;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.cast;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.ceil;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.coalesce;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.concat;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.custom;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.day;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.divide;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.floor;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.hour;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.length;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.locate;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.lower;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.minus;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.minute;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.mod;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.month;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.multiply;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.nullif;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.plus;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.round;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.second;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.sqrt;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.substring;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.trim;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.upper;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.vector_distance;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.vector_score;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.year;
import static com.servoy.j2db.util.Utils.getCallerMethodName;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.querybuilder.IQueryBuilderFunctions;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * <p>The <code>QBFunctions</code> class provides a comprehensive set of SQL functions designed to
 * enhance query building in <code>QBSelect</code>. It enables the creation of mathematical, string,
 * and date-based expressions, along with conditional and custom logic.</p>
 *
 * <p>This class allows users to perform operations like calculating absolute values, rounding, and
 * extracting substrings. It also includes advanced capabilities such as casting data types,
 * concatenating strings, trimming whitespace, and formatting dates. With access to aggregation
 * methods and support for custom function definitions, the class is versatile in handling diverse
 * SQL requirements.</p>
 *
 * <p>For additional guidance on query construction and execution, refer to the
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbselect">QBSelect</a> section of the documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBFunctions extends QBPart implements IQueryBuilderFunctions
{
	QBFunctions(QBSelect parent)
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
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#upper()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.upper.eq(query.functions.upper('Sample')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the value converted to uppercase.
	 */
	@JSFunction
	public QBTextColumnBase upper(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), upper, new IQuerySelectValue[] { createOperand(value, TEXT) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#abs()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynum.abs.eq(query.functions.abs(myval)))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the absolute value of the input.
	 */
	@JSFunction
	public QBNumberColumnBase abs(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), abs, new IQuerySelectValue[] { createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#sqrt()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynum.sqrt.eq(query.functions.sqrt(myval)))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the square root of the input.
	 */
	@JSFunction
	public QBNumberColumnBase sqrt(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), sqrt, new IQuerySelectValue[] { createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#lower()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.lower.eq(query.functions.lower('Sample')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the value converted to lowercase.
	 */
	@JSFunction
	public QBTextColumnBase lower(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), lower, new IQuerySelectValue[] { createOperand(value, TEXT) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#trim()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.trim.eq(query.functions.trim('Sample')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the value with leading and trailing spaces removed.
	 */
	@JSFunction
	public QBTextColumnBase trim(Object value)
	{
		// ansi standard trim()
		return trim("BOTH", " ", "FROM", value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#trim()
	 * @param leading_trailing_both 'leading', 'trailing' or 'both'
	 * @param characters characters to remove
	 * @param fromKeyword 'from'
	 * @param value value to trim
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * // show shipname but remove trailing space
	 * query.result.add(query.functions.trim('trailing', ' ', 'from', query.columns.shipname));
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the value with specified characters trimmed from a specified position.
	 */
	@JSFunction
	public QBTextColumnBase trim(String leading_trailing_both, String characters, String fromKeyword, Object value)
	{
		// some dbs (HXTT-DBF) do not like parameters for the characters, when it is 1 character it is safe for inline.
		IQuerySelectValue charactersValue = characters.length() == 1
			? new QueryColumnValue('\'' + characters + '\'', null, true)
			: new QueryColumnValue(characters, null, false);
		return new QBFunctionImpl(getRoot(), getParent(), trim, new IQuerySelectValue[] { //
			new QueryColumnValue(leading_trailing_both, null, validateKeyword(leading_trailing_both, "leading", "trailing", "both")), // keyword
			charactersValue, // characters
			new QueryColumnValue(fromKeyword, null, validateKeyword(fromKeyword, "from")), // keyword
			createOperand(value, TEXT)
		});
	}

	protected IQuerySelectValue createOperand(Object value)
	{
		warnIgnoredNegatedColumn(value);
		return getRoot().createOperand(value, null, 0);
	}

	protected IQuerySelectValue createOperand(Object value, int type)
	{
		warnIgnoredNegatedColumn(value);
		return getRoot().createOperand(value, ColumnType.getColumnType(type), 0);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#len()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.len.eq(query.functions.len('Sample')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the length of the value.
	 */
	@JSFunction
	public QBIntegerColumnBase len(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), length, new IQuerySelectValue[] { createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#bit_length()
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.bit_length.eq(query.functions.bit_length('Sample')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the bit length of the value.
	 */
	@JSFunction
	public QBIntegerColumnBase bit_length(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), bit_length, new IQuerySelectValue[] { createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBColumn#cast(String)
	 * @param value object to cast
	 * @param type type see QUERY_COLUMN_TYPES
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.functions.cast("22",QUERY_COLUMN_TYPES.TYPE_INTEGER)).add(query.columns.amt_discount.cast(QUERY_COLUMN_TYPES.TYPE_STRING));
	 * application.output(databaseManager.getDataSetByQuery(query,1).getAsHTML())
	 *
	 * @return A query builder column representing the value cast to the specified type.
	 */
	@JSFunction
	public QBGenericColumnBase cast(Object value, String type)
	{
		return new QBFunctionImpl(getRoot(), getParent(), cast, new IQuerySelectValue[] { createOperand(value), new QueryColumnValue(type, null, true) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#substring(int)
	 * @param arg column name
	 * @param pos position
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.substring(3).eq(query.functions.substring('Sample', 3)))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing a substring starting at the specified position.
	 */
	@JSFunction
	public QBTextColumnBase substring(Object arg, int pos)
	{
		return substring(arg, pos, Integer.MAX_VALUE - pos);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#substring(int, int)
	 * @param arg column name
	 * @param pos position
	 * @param len length
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.substring(3, 2).eq(query.functions.substring('Sample', 3, 2)))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing a substring of specified length starting at the specified position.
	 */
	@JSFunction
	public QBTextColumnBase substring(Object arg, int pos, int len)
	{
		return new QBFunctionImpl(getRoot(), getParent(), substring,
			new IQuerySelectValue[] { createOperand(arg, TEXT), createOperand(Integer.valueOf(pos)), createOperand(Integer.valueOf(len)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#locate(Object)
	 * @param string1 string to locate
	 * @param string2 string to search in
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.locate('amp').eq(query.functions.locate('amp', 'Sample')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the position of the first occurrence of one string in another.
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object string1, Object string2)
	{
		return locate(string1, string2, 1);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBTextColumnBase#locate(Object, int)
	 * @param string1 string to locate
	 * @param string2 string to search in
	 * @param start start pos
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.locate('amp', 1).eq(query.functions.locate('amp', 'Sample', 1)))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the position of the first occurrence of one string in another, starting from a given position.
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object string1, Object string2, int start)
	{
		return new QBFunctionImpl(getRoot(), getParent(), locate,
			new IQuerySelectValue[] { createOperand(string1, TEXT), createOperand(string2, TEXT), createOperand(Integer.valueOf(start)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBColumnBaseFunctions#nullif(Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.nullif('none').eq(query.functions.nullif('Sample', 'none')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column that returns null if the two arguments are equal.
	 */
	@JSFunction
	public QBGenericColumnBase nullif(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), nullif, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#mod(Object)
	 * @param dividend
	 * @param divisor
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.mod(2).eq(query.functions.mod(myvar, 2))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the remainder of the division of two numbers.
	 */
	@JSFunction
	public QBIntegerColumnBase mod(Object dividend, Object divisor)
	{
		return new QBFunctionImpl(getRoot(), getParent(), mod, new IQuerySelectValue[] { createOperand(dividend), createOperand(divisor) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#plus(Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.plus(2).eq(query.functions.plus(myvar, 2))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the sum of two arguments.
	 */
	@JSFunction
	public QBNumberColumnBase plus(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), plus, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#minus(Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.minus(2).eq(query.functions.minus(myvar, 2))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the difference between two arguments.
	 */
	@JSFunction
	public QBNumberColumnBase minus(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), minus, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#multiply(Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.multiply(2).eq(query.functions.multiply(myvar, 2))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the product of two arguments.
	 */
	@JSFunction
	public QBNumberColumnBase multiply(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), multiply, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnFunctions#divide(Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.divide(2).eq(query.functions.divide(myvar, 2))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the division of two arguments.
	 */
	@JSFunction
	public QBNumberColumnBase divide(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), divide, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBColumn#concat(Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * 	var query = datasources.db.udm.contacts.createSelect();
	 * 	query.result.add(query.columns.name_first.concat(' ').concat(query.columns.name_last))
	 * 	var ds = databaseManager.getDataSetByQuery(query, -1)
	 *
	 * @return A query builder column representing the concatenation of two arguments.
	 */
	@JSFunction
	public QBTextColumnBase concat(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), concat, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnBase#floor()
	 * @param arg number object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.floor.eq(query.functions.floor(myvar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the largest integer less than or equal to the input.
	 */
	@JSFunction
	public QBIntegerColumnBase floor(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), floor, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnBase#round()
	 * @param arg number object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.round.eq(query.functions.round(myvar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the input rounded to the nearest integer.
	 */
	@JSFunction
	public QBIntegerColumnBase round(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), round, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnBase#round()
	 * @param arg number object
	 * @param decimals The number of decimal places to round number to, default 0
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.round.eq(query.functions.round(myvar, 1))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the input rounded to the specified number of decimal places.
	 */
	@JSFunction
	public QBNumberColumnBase round(Object arg, int decimals)
	{
		return new QBFunctionImpl(getRoot(), getParent(), round, new IQuerySelectValue[] { createOperand(arg), createOperand(Integer.valueOf(decimals)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBNumberColumnBase#ceil()
	 * @param arg number object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.ceil.eq(query.functions.ceil(myvar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the smallest integer greater than or equal to the input.
	 */
	@JSFunction
	public QBIntegerColumnBase ceil(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), ceil, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * Call a custom defined function.
	 * @param name custom function name
	 * @param args function arguments
	 * @sample
	 *  // select myadd(freight, 500) from orders
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.result.add(query.functions.custom('myadd', query.columns.freight, 500));
	 * 	var dataset = databaseManager.getDataSetByQuery(query, 100);
	 *
	 * @return A query builder column representing a custom function with the given name and arguments.
	 */
	@JSFunction
	public QBGenericColumnBase custom(String name, Object... args)
	{
		return new QBFunctionImpl(getRoot(), getParent(), custom,
			Utils.arrayAdd(getRoot().createOperands(args == null ? new Object[] { null } : args, null, 0), new QueryColumnValue(name, null, true), false));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBDatetimeColumnBase#second()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.second.eq(query.functions.second(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the second component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase second(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), second, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBDatetimeColumnBase#minute()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.minute.eq(query.functions.minute(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the minute component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase minute(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), minute, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBDatetimeColumnBase#hour()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.hour.eq(query.functions.hour(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the hour component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase hour(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), hour, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBDatetimeColumnBase#day()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.day.eq(query.functions.day(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the day component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase day(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), day, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBDatetimeColumnBase#month()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.month.eq(query.functions.month(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the month component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase month(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), month, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBDatetimeColumnBase#year()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.year.eq(query.functions.year(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the year component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase year(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), year, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBArrayColumnBase#cardinality()
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.year.eq(query.functions.year(mydatevar))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the year component of a date/time value.
	 */
	@JSFunction
	public QBIntegerColumnBase cardinality(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), cardinality, new IQuerySelectValue[] { createOperand(arg) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBVectorColumnBase#vector_score(String)
	 * @param arg vector column
	 * @param embedding embedding object
	 * @sample
	 * var query = datasources.db.example_data.books.createSelect();
	 * query.where.add(query.joins.books_to_books_embeddings.columns.embedding.vector_score(model.embedding('Magic or Fantasy')).min_score(0.7))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the normalized vector score of the vector column compared with the embedding.
	 */
	@JSFunction
	public QBScoreColumnBase vector_score(Object arg, float[] embedding)
	{
		return new QBFunctionImpl(getRoot(), getParent(), vector_score,
			new IQuerySelectValue[] { createOperand(arg), new QueryColumnValue(embedding, null) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBVectorColumnBase#vector_distance(String)
	 * @param arg vector column
	 * @param embedding embedding object
	 * @sample
	 * var query = datasources.db.example_data.books.createSelect();
	 * query.sort.add(query.joins.books_to_books_embeddings.columns.embedding.vector_distance(model.embedding('Magic or Fantasy')))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column representing the database native cosine distance of the vector column compared with the embedding.
	 */
	@JSFunction
	public QBScoreColumnBase vector_distance(Object arg, float[] embedding)
	{
		IQuerySelectValue firstArg = createOperand(arg);
		return new QBFunctionImpl(getRoot(), getParent(), vector_distance,
			new IQuerySelectValue[] { firstArg, new QueryColumnValue(embedding, null, false, firstArg.getNativeTypename()) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.impl.QBColumnBaseFunctions#coalesce(Object[])
	 * @param args arguments to coalesce
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mycol.coalesce('defval').eq(query.functions.coalesce(myvar, 'defval'))
	 * foundset.loadRecords(query);
	 *
	 * @return A query builder column that returns the first non-null argument.
	 */
	@JSFunction
	public QBGenericColumnBase coalesce(Object... args)
	{
		IQuerySelectValue[] functionArgs = stream(args).map(this::createOperand).toArray(IQuerySelectValue[]::new);
		return new QBFunctionImpl(getRoot(), getParent(), coalesce, functionArgs);
	}

	/**
	 * @return true if the value matches one of the allowed strings.
	 */
	private static boolean validateKeyword(String value, String... allowed)
	{
		return value != null && asList(allowed).contains(value.trim().toLowerCase());
	}

	private static void warnIgnoredNegatedColumn(Object value)
	{
		if (value instanceof QBColumnImpl qbColumn && qbColumn.negate)
		{
			Debug.warn("Function " + getCallerMethodName(2) + "() called on negated column (" + value + ") , negation will be ignored");
		}
	}

	@Override
	public String toString()
	{
		return "QBFunctions(Helper class for creating functions)";
	}

}
