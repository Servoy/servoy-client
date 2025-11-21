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

import static com.servoy.j2db.query.QueryFunction.QueryFunctionType._denormalize_vector_score;
import static com.servoy.j2db.util.Utils.arrayAdd;

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

/**
 * A column from a QBSelect. Base class that contains all actual implementations.
 *
 * @author rgansevles
 *
 */
public class QBColumnImpl extends QBPart
	implements QBGenericColumnBase, QBIntegerColumnBase, QBDatetimeColumnBase, QBNumberColumnBase, QBMediaColumnBase,
	QBTextColumnBase, QBScoreColumnBase, QBColumnComparable<QBColumnComparable<QBColumn>>, QBTextColumnComparableFunctions, QBArrayColumnBase,
	QBVectorColumnBase, QBColumn,
	QBNumberColumnFunctions<QBColumn>
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
	////////////// QBColumnComparable methods //////////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBColumnComparable not()
	{
		return new QBColumnImpl(getRoot(), getParent(), getQuerySelectValue(), !negate);
	}

	@Override
	public QBCondition gt(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.GT_OPERATOR, value);
	}

	@Override
	public QBCondition lt(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.LT_OPERATOR, value);
	}

	@Override
	public QBCondition ge(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.GTE_OPERATOR, value);
	}

	@Override
	public QBCondition le(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.LTE_OPERATOR, value);
	}

	@Override
	public QBCondition between(Object value1, Object value2)
	{
		return createCondition(
			new CompareCondition(IBaseSQLCondition.BETWEEN_OPERATOR, getQuerySelectValue(), new Object[] { createOperand(value1), createOperand(value2) }));
	}

	@Override
	public QBCondition isin(QBPart query)
	{
		return createCompareCondition(IBaseSQLCondition.IN_OPERATOR, query);
	}

	@Override
	public QBCondition isin(String customQuery, Object[] args)
	{
		return createCondition(
			new SetCondition(IBaseSQLCondition.IN_OPERATOR, new IQuerySelectValue[] { getQuerySelectValue() },
				new QueryCustomSelect(customQuery, args == null ? null : getRoot().createOperands(args, null, 0)), true));
	}

	@Override
	public QBCondition isin(Object[] values)
	{
		IQuerySelectValue querySelectValue = getQuerySelectValue();
		return createCondition(new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { querySelectValue },
			new Object[][] { values == null ? new Object[0] : getRoot().createOperands(values, querySelectValue.getColumnType(), querySelectValue.getFlags()) },
			true));
	}

	@Override
	public QBCondition eq(Object value)
	{
		return createCompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, value);
	}

	/////////////////////////////////////////////////////////
	//////////// QBTextColumnComparable methods ////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBCondition like(Object pattern)
	{
		if (pattern instanceof String)
		{
			// don't try to convert the pattern to the column type
			return createCondition(new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, getQuerySelectValue(), pattern));
		}
		return createCompareCondition(IBaseSQLCondition.LIKE_OPERATOR, pattern);
	}

	@Override
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

	@Override
	public QBSort asc()
	{
		return new QBSort(getRoot(), this, true);
	}

	@Override
	public QBSort desc()
	{
		return new QBSort(getRoot(), this, false);
	}

	@Override
	public QBCountAggregate count()
	{
		return getRoot().aggregates().count(this);
	}

	@Override
	public QBTextColumnBase concat(Object arg)
	{
		return getRoot().functions().concat(this, arg);
	}

	/////////////////////////////////////////////////////////
	//////////// QBNumberColumnFunctions methods ////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBColumn avg()
	{
		return (QBColumn)getRoot().aggregates().avg(this);
	}

	@Override
	public QBColumn max()
	{
		return (QBColumn)getRoot().aggregates().max(this);
	}

	@Override
	public QBColumn min()
	{
		return (QBColumn)getRoot().aggregates().min(this);
	}

	@Override
	public QBColumn sum()
	{
		return (QBColumn)getRoot().aggregates().sum(this);
	}

	@Override
	public QBColumn nullif(Object arg)
	{
		return (QBColumn)getRoot().functions().nullif(this, arg);
	}

	@Override
	public QBColumn coalesce(Object... args)
	{
		return (QBColumn)getRoot().functions().coalesce(arrayAdd(args, this, false));
	}

	/////////////////////////////////////////////////////////
	////////////// QBTextColumnBase methods /////////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBTextColumnBase upper()
	{
		return getRoot().functions().upper(this);
	}

	@Override
	public QBTextColumnBase lower()
	{
		return getRoot().functions().lower(this);
	}

	@Override
	public QBTextColumnBase trim()
	{
		return getRoot().functions().trim(this);
	}

	@Override
	public QBIntegerColumnBase len()
	{
		return getRoot().functions().len(this);
	}

	@Override
	public QBTextColumnBase substring(int pos)
	{
		return getRoot().functions().substring(this, pos);
	}

	@Override
	public QBTextColumnBase substring(int pos, int len)
	{
		return getRoot().functions().substring(this, pos, len);
	}

	@Override
	public QBIntegerColumnBase bit_length()
	{
		return getRoot().functions().bit_length(this);
	}

	@Override
	public QBIntegerColumnBase locate(Object arg)
	{
		return getRoot().functions().locate(arg, this);
	}

	@Override
	public QBIntegerColumnBase locate(Object arg, int start)
	{
		return getRoot().functions().locate(arg, this, start);
	}

	/////////////////////////////////////////////////////////
	////////// QBNumberColumnFunctions methods //////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBColumn abs()
	{
		return (QBColumn)getRoot().functions().abs(this);
	}

	@Override
	public QBNumberColumnBase sqrt()
	{
		return getRoot().functions().sqrt(this);
	}

	@Override
	public QBColumn mod(Object arg)
	{
		return (QBColumn)getRoot().functions().mod(this, arg);
	}

	@Override
	public QBNumberColumnBase plus(Object arg)
	{
		return getRoot().functions().plus(this, arg);
	}

	@Override
	public QBNumberColumnBase minus(Object arg)
	{
		return getRoot().functions().minus(this, arg);
	}

	@Override
	public QBNumberColumnBase multiply(Object arg)
	{
		return getRoot().functions().multiply(this, arg);
	}

	@Override
	public QBNumberColumnBase divide(Object arg)
	{
		return getRoot().functions().divide(this, arg);
	}

	@Override
	public QBGenericColumnBase cast(String type)
	{
		return getRoot().functions().cast(this, type);
	}

	@Override
	public int getFlags()
	{
		return getQuerySelectValue().getFlags();
	}

	@Override
	public String getTypeAsString()
	{
		BaseColumnType columnType = getColumnType();
		return columnType != null ? Column.getDisplayTypeString(columnType.getSqlType()) : null;
	}

	/////////////////////////////////////////////////////////
	////////////// QBNumberColumnBase methods ///////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBIntegerColumnBase floor()
	{
		return getRoot().functions().floor(this);
	}

	@Override
	public QBIntegerColumnBase round()
	{
		return getRoot().functions().round(this);
	}

	@Override
	public QBIntegerColumnBase ceil()
	{
		return getRoot().functions().ceil(this);
	}

	/////////////////////////////////////////////////////////
	////////////// QBDatetimeColumnBase methods ///////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBIntegerColumnBase hour()
	{
		return getRoot().functions().hour(this);
	}

	@Override
	public QBIntegerColumnBase second()
	{
		return getRoot().functions().second(this);
	}

	@Override
	public QBIntegerColumnBase minute()
	{
		return getRoot().functions().minute(this);
	}

	@Override
	public QBIntegerColumnBase day()
	{
		return getRoot().functions().day(this);
	}

	@Override
	public QBIntegerColumnBase month()
	{
		return getRoot().functions().month(this);
	}

	@Override
	public QBIntegerColumnBase year()
	{
		return getRoot().functions().year(this);
	}

	/////////////////////////////////////////////////////////
	////////////// QBArrayColumnBase methods ///////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBIntegerColumnBase cardinality()
	{
		return getRoot().functions().cardinality(this);
	}

	/////////////////////////////////////////////////////////
	////////////// QBVectorColumnBase methods ///////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBScoreColumnBase vector_score(float[] embedding)
	{
		return getRoot().functions().vector_score(this, embedding);
	}

	/////////////////////////////////////////////////////////
	////////////// QBScoreColumnBase methods ///////////////
	/////////////////////////////////////////////////////////

	@Override
	public QBCondition min_score(Object normalizedScore)
	{
		// compare using the denormalized score so native indexes can be used
		var denormalizedScore = new QBFunctionImpl(getRoot(), getParent(), _denormalize_vector_score, new IQuerySelectValue[] { createOperand(this) });
		var denormalizedMinScore = new QBFunctionImpl(getRoot(), getParent(), _denormalize_vector_score,
			new IQuerySelectValue[] { createOperand(normalizedScore) });

		return denormalizedScore.le(denormalizedMinScore);
	}

	/////////////////////////////////////////////////////////
	//////////////////// General methods ////////////////////
	/////////////////////////////////////////////////////////

	public BaseColumnType getColumnType()
	{
		return getQuerySelectValue().getColumnType();
	}

	@Override
	public String toString()
	{
		IQuerySelectValue querySelectValue = getQuerySelectValue();
		return (negate ? "!" : "") + (querySelectValue == null ? "<NONE>" : querySelectValue.toString());
	}
}
