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
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;

/**
 * A column from a QBSelect. Used for different conditions.
 *
 * @author rgansevles
 *
 */
public class QBColumnImpl extends QBPart
	implements IQueryBuilderColumn, QBIntegerColumnBase, QBDatetimeColumnBase, QBNumberColumnBase, QBMediaColumnBase,
	QBTextColumnBase, QBColumnCompare, QBColumn, QBColumnNumberRagtest<QBColumn>
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


	/////////////////////////////////////////////////////////
	////////////// QBColumnCompare methods //////////////////
	/////////////////////////////////////////////////////////

	public QBCondition gt(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.GT_OPERATOR, value);
	}

	public QBCondition lt(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.LT_OPERATOR, value);
	}

	public QBCondition ge(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.GTE_OPERATOR, value);
	}

	public QBCondition le(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.LTE_OPERATOR, value);
	}

	public QBCondition between(Object value1, Object value2)
	{
		return createCondition(
			new CompareCondition(IBaseSQLCondition.BETWEEN_OPERATOR, getQuerySelectValue(), new Object[] { createOperand(value1), createOperand(value2) }));
	}

	public QBCondition isin(QBPart query)
	{
		return createCompareCondition(IBaseSQLCondition.IN_OPERATOR, query);
	}

	public QBCondition isin(String customQuery, Object[] args)
	{
		return createCondition(
			new SetCondition(IBaseSQLCondition.IN_OPERATOR, new IQuerySelectValue[] { getQuerySelectValue() },
				new QueryCustomSelect(customQuery, args == null ? null : getRoot().createOperands(args, null, 0)), true));
	}

	public QBCondition isin(Object[] values)
	{
		IQuerySelectValue querySelectValue = getQuerySelectValue();
		return createCondition(new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { querySelectValue },
			new Object[][] { values == null ? new Object[0] : getRoot().createOperands(values, querySelectValue.getColumnType(), querySelectValue.getFlags()) },
			true));
	}

	public QBCondition eq(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, value);
	}

	public QBCondition like(Object pattern)
	{
		if (pattern instanceof String)
		{
			// don't try to convert the pattern to the column type
			return createCondition(new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, getQuerySelectValue(), pattern));
		}
		return createCompareCondition(IBaseSQLCondition.LIKE_OPERATOR, pattern);
	}

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


	/////////////////////////////////////////////////////////
	////////////// General QBColumn methods /////////////////
	/////////////////////////////////////////////////////////

	public QBColumn not()
	{
		return new QBColumnImpl(getRoot(), getParent(), getQuerySelectValue(), !negate);
	}

	public QBSort asc()
	{
		return new QBSort(getRoot(), this, true);
	}

	public QBSort desc()
	{
		return new QBSort(getRoot(), this, false);
	}

	public QBCountAggregate count()
	{
		return getRoot().aggregates().count(this);
	}

	/////////////////////////////////////////////////////////
	////////////// QBColumnRagtest methods /////////////////
	/////////////////////////////////////////////////////////

	public QBColumn avg()
	{
		return getRoot().aggregates().avg(this);
	}

	public QBColumn max()
	{
		return getRoot().aggregates().max(this);
	}

	public QBColumn min()
	{
		return getRoot().aggregates().min(this);
	}

	public QBColumn sum()
	{
		return getRoot().aggregates().sum(this);
	}

	public QBColumn nullif(Object arg)
	{
		return getRoot().functions().nullif(this, arg);
	}

	/////////////////////////////////////////////////////////
	////////////// QBTextColumnBase methods /////////////////
	/////////////////////////////////////////////////////////

	public QBTextColumnBase upper()
	{
		return getRoot().functions().upper(this);
	}

	public QBTextColumnBase lower()
	{
		return getRoot().functions().lower(this);
	}

	public QBTextColumnBase trim()
	{
		return getRoot().functions().trim(this);
	}

	public QBIntegerColumnBase len()
	{
		return getRoot().functions().len(this);
	}

	public QBTextColumnBase substring(int pos)
	{
		return getRoot().functions().substring(this, pos);
	}

	public QBTextColumnBase substring(int pos, int len)
	{
		return getRoot().functions().substring(this, pos, len);
	}

	public QBIntegerColumnBase bit_length()
	{
		return getRoot().functions().bit_length(this);
	}

	public QBIntegerColumnBase locate(Object arg)
	{
		return getRoot().functions().locate(arg, this);
	}

	public QBIntegerColumnBase locate(Object arg, int start)
	{
		return getRoot().functions().locate(arg, this, start);
	}

	public QBTextColumnBase concat(Object arg)
	{
		return getRoot().functions().concat(this, arg);
	}

	/////////////////////////////////////////////////////////
	////////////// QBColumnNumberRagtest methods /////////////////
	/////////////////////////////////////////////////////////

	public QBColumn abs()
	{
		return (QBColumn)getRoot().functions().abs(this);
	}

	public QBNumberColumnBase sqrt()
	{
		return getRoot().functions().sqrt(this);
	}

	public QBColumn mod(Object arg)
	{
		return (QBColumn)getRoot().functions().mod(this, arg);
	}

	public QBNumberColumnBase plus(Object arg)
	{
		return getRoot().functions().plus(this, arg);
	}

	public QBNumberColumnBase minus(Object arg)
	{
		return getRoot().functions().minus(this, arg);
	}

	public QBNumberColumnBase multiply(Object arg)
	{
		return getRoot().functions().multiply(this, arg);
	}

	public QBNumberColumnBase divide(Object arg)
	{
		return getRoot().functions().divide(this, arg);
	}


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
	/////////////////////////////////////////////////////////
	////////////// QBNumberColumnBase methods ///////////////
	/////////////////////////////////////////////////////////

	public QBIntegerColumnBase floor()
	{
		return getRoot().functions().floor(this);
	}

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
	//@JSIgnore // RAGTEST doc
	@Override
	public QBIntegerColumnBase hourragtest()
	{
		return getRoot().functions().hourragtest(this);
	}


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

	@Override
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
	@Override
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
