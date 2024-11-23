/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
w
 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.j2db.querybuilder.impl;

import static com.servoy.j2db.persistence.IColumnTypes.TEXT;
import static com.servoy.j2db.util.Utils.getCallerMethodName;
import static java.util.Arrays.asList;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.querybuilder.IQueryBuilderFunctions;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;

/**
 * Utility class for all available SQL functions.
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
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#upper(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.upper.eq(query.functions.upper('Sample')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBTextColumnBase upper(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.upper, new IQuerySelectValue[] { createOperand(value, TEXT) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#abs(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynum.abs.eq(query.functions.abs(myval)))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase abs(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.abs, new IQuerySelectValue[] { createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#sqrt(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynum.sqrt.eq(query.functions.sqrt(myval)))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase sqrt(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.sqrt, new IQuerySelectValue[] { createOperand(value) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#lower(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.lower.eq(query.functions.lower('Sample')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBTextColumnBase lower(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.lower, new IQuerySelectValue[] { createOperand(value, TEXT) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#trim(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.trim.eq(query.functions.trim('Sample')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBTextColumnBase trim(Object value)
	{
		// ansi standard trim()
		return trim("BOTH", " ", "FROM", value);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#trim(String, String, String, Object)
	 * @param leading_trailing_both 'leading', 'trailing' or 'both'
	 * @param characters characters to remove
	 * @param fromKeyword 'from'
	 * @param value value to trim
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * // show shipname but remove trailing space
	 * query.result.add(query.functions.trim('trailing', ' ', 'from', query.columns.shipname));
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBTextColumnBase trim(String leading_trailing_both, String characters, String fromKeyword, Object value)
	{
		// some dbs (HXTT-DBF) do not like parameters for the characters, when it is 1 character it is safe for inline.
		IQuerySelectValue charactersValue = characters.length() == 1
			? new QueryColumnValue('\'' + characters + '\'', null, true)
			: new QueryColumnValue(characters, null, false);
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.trim, new IQuerySelectValue[] { //
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
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#length(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.len.eq(query.functions.len('Sample')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBIntegerColumnBase len(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.length, new IQuerySelectValue[] { createOperand(value) });
	}


	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#bit_length(Object)
	 * @param value
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.bit_length.eq(query.functions.bit_length('Sample')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBIntegerColumnBase bit_length(Object value)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.bit_length, new IQuerySelectValue[] { createOperand(value) });
	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#cast(Object, String)
//	 * @param value object to cast
//	 * @param type type see QUERY_COLUMN_TYPES
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.result.add(query.functions.cast("22",QUERY_COLUMN_TYPES.TYPE_INTEGER)).add(query.columns.amt_discount.cast(QUERY_COLUMN_TYPES.TYPE_STRING));
//	 * application.output(databaseManager.getDataSetByQuery(query,1).getAsHTML())
//	 */
//	@JSFunction
//	public QBFunction cast(Object value, String type)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.cast,
//			new IQuerySelectValue[] { createOperand(value), new QueryColumnValue(type, null, true) });
//	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#substring(Object, int)
	 * @param arg column name
	 * @param pos position
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.substring(3).eq(query.functions.substring('Sample', 3)))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBTextColumnBase substring(Object arg, int pos)
	{
		return substring(arg, pos, Integer.MAX_VALUE - pos);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#substring(Object, int, int)
	 * @param arg column name
	 * @param pos position
	 * @param len length
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.substring(3, 2).eq(query.functions.substring('Sample', 3, 2)))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBTextColumnBase substring(Object arg, int pos, int len)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.substring,
			new IQuerySelectValue[] { createOperand(arg, TEXT), createOperand(Integer.valueOf(pos)), createOperand(Integer.valueOf(len)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#locate(Object, Object)
	 * @param string1 string to locate
	 * @param string2 string to search in
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.locate('amp').eq(query.functions.locate('amp', 'Sample')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object string1, Object string2)
	{
		return locate(string1, string2, 1);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#locate(Object, Object, int)
	 * @param string1 string to locate
	 * @param string2 string to search in
	 * @param start start pos
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.locate('amp', 1).eq(query.functions.locate('amp', 'Sample', 1)))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBIntegerColumnBase locate(Object string1, Object string2, int start)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.locate,
			new IQuerySelectValue[] { createOperand(string1, TEXT), createOperand(string2, TEXT), createOperand(Integer.valueOf(start)) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#nullif(Object, Object)
	 * @param arg1
	 * @param arg1
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.nullif('none').eq(query.functions.nullif('Sample', 'none')))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBColumn nullif(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.nullif, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#mod(Object, Object)
	 * @param dividend
	 * @param divisor
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.mod(2).eq(query.functions.mod(myvar, 2))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase mod(Object dividend, Object divisor)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.mod, new IQuerySelectValue[] { createOperand(dividend), createOperand(divisor) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#plus(Object, Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.plus(2).eq(query.functions.plus(myvar, 2))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase plus(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.plus, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#minus(Object, Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.minus(2).eq(query.functions.minus(myvar, 2))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase minus(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.minus, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#multiply(Object, Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.multiply(2).eq(query.functions.multiply(myvar, 2))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase multiply(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.multiply, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#divide(Object, Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.divide(2).eq(query.functions.divide(myvar, 2))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBNumberColumnBase divide(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.divide, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#concat(Object, Object)
	 * @param arg1
	 * @param arg2
	 * @sample
	 * 	var query = datasources.db.udm.contacts.createSelect();
	 * 	query.result.add(query.columns.name_first.concat(' ').concat(query.columns.name_last))
	 * 	var ds = databaseManager.getDataSetByQuery(query, -1)
	 *
	 */
	@JSFunction
	public QBTextColumnBase concat(Object arg1, Object arg2)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.concat, new IQuerySelectValue[] { createOperand(arg1), createOperand(arg2) });
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#floor(Object)
	 * @param arg number object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mynumcol.floor.eq(query.functions.floor(myvar))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBIntegerColumnBase floor(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.floor, new IQuerySelectValue[] { createOperand(arg) });
	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#round(Object)
//	 * @param arg number object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mynumcol.round.eq(query.functions.round(myvar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction round(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.round, new IQuerySelectValue[] { createOperand(arg) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#round(Object)
//	 * @param arg number object
//	 * @param decimals The number of decimal places to round number to, default 0
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mynumcol.round.eq(query.functions.round(myvar, 1))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	@Override
//	public QBFunction round(Object arg, int decimals)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.round,
//			new IQuerySelectValue[] { createOperand(arg), createOperand(Integer.valueOf(decimals)) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#ceil(Object)
//	 * @param arg number object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mynumcol.ceil.eq(query.functions.ceil(myvar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction ceil(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.ceil, new IQuerySelectValue[] { createOperand(arg) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#custom(String, Object...)
//	 * @param name custom function name
//	 * @param args function arguments
//	 * @sample
//	 *  // select myadd(freight, 500) from orders
//	 * 	var query = datasources.db.example_data.orders.createSelect();
//	 * 	query.result.add(query.functions.custom('myadd', query.columns.freight, 500));
//	 * 	var dataset = databaseManager.getDataSetByQuery(query, 100);
//	 */
//	@JSFunction
//	public QBFunction custom(String name, Object... args)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.custom,
//			Utils.arrayAdd(getRoot().createOperands(args == null ? new Object[] { null } : args, null, 0), new QueryColumnValue(name, null, true), false));
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#second(Object)
//	 * @param arg date object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mydatecol.second.eq(query.functions.second(mydatevar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction second(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.second, new IQuerySelectValue[] { createOperand(arg) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#minute(Object)
//	 * @param arg date object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mydatecol.minute.eq(query.functions.minute(mydatevar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction minute(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.minute, new IQuerySelectValue[] { createOperand(arg) });
//	}

	// RAGTEST check ook argument??
	// public QBIntegerColumn hour(QBDatetimeColumn arg)


	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#hour(Object)
	 * @param arg date object
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.mydatecol.hour.eq(query.functions.hour(mydatevar))
	 * foundset.loadRecords(query);
	 */
	@JSFunction
	public QBIntegerColumnBase hourragtest(Object arg)
	{
		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.hour, new IQuerySelectValue[] { createOperand(arg) });
	}

//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#day(Object)
//	 * @param arg date object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mydatecol.day.eq(query.functions.day(mydatevar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction day(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.day, new IQuerySelectValue[] { createOperand(arg) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#month(Object)
//	 * @param arg date object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mydatecol.month.eq(query.functions.month(mydatevar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction month(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.month, new IQuerySelectValue[] { createOperand(arg) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#year(Object)
//	 * @param arg date object
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mydatecol.year.eq(query.functions.year(mydatevar))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction year(Object arg)
//	{
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.year, new IQuerySelectValue[] { createOperand(arg) });
//	}
//
//	/**
//	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderFunctions#coalesce(Object...)
//	 * @param args arguments to coalesce
//	 * @sample
//	 * var query = datasources.db.example_data.orders.createSelect();
//	 * query.where.add(query.columns.mycol.coalesce('defval').eq(query.functions.coalesce(myvar, 'defval'))
//	 * foundset.loadRecords(query);
//	 */
//	@JSFunction
//	public QBFunction coalesce(Object... args)
//	{
//		List<IQuerySelectValue> list = new ArrayList<IQuerySelectValue>(args.length);
//		for (Object arg : args)
//		{
//			list.add(createOperand(arg));
//		}
//		return new QBFunctionImpl(getRoot(), getParent(), QueryFunctionType.coalesce, list.toArray(new IQuerySelectValue[list.size()]));
//	}

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
