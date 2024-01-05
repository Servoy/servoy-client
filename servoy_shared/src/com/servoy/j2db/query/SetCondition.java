/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.query;

import com.servoy.base.query.BaseSetCondition;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.query.AbstractBaseQuery.PlaceHolderSetter;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Condition class in query structure that compares an array of keys with a matrix of values.
 * <p>
 * The keys, operators and the matrix must have the same width.
 * <p>
 * The comparison is done by AND-ing all key value comparisons against matrix values for each row, per row the logic is OR-ed. For example
 *
 * <pre>
 *          [ pk1, pk2 ]  ( =, &gt; )  [ 1  , 2  ]
 *                                  [ 10 , 20 ]
 *                                  [ 55 , 42 ]
 * </pre>
 *
 * Means
 *
 * <pre>
 *      pk1 = 1  AND pk2 &gt; 2
 * OR   pk1 = 10 AND pk2 &gt; 40
 * OR   pk1 = 55 AND pk2 &gt; 42
 * </pre>
 *
 * When the boolean andCondition is set to false (by the negate method), AND and OR are used the other way around.
 *
 * @author rgansevles
 *
 */
public class SetCondition extends BaseSetCondition<IQuerySelectValue> implements ISQLCondition
{
	public SetCondition(int operators[], IQuerySelectValue[] keys, Object values, boolean andCondition)
	{
		super(operators, keys, values, andCondition);
	}

	/**
	 * Constructor for all the same operators.
	 *
	 * @param operator
	 * @param keys
	 * @param values
	 * @param andCondition
	 */
	public SetCondition(int operator, IQuerySelectValue[] keys, Object values, boolean andCondition)
	{
		super(operator, keys, values, andCondition);
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}


	@Override
	public ISQLCondition negate()
	{
		return (ISQLCondition)super.negate();
	}

	@Override
	protected SetCondition withOperators(int[] ops)
	{
		return new SetCondition(ops, keys, values, andCondition);
	}

	public void acceptVisitor(IVisitor visitor)
	{
		keys = AbstractBaseQuery.acceptVisitor(keys, visitor);
		values = AbstractBaseQuery.acceptVisitor(values, visitor);
		if (values instanceof Placeholder && visitor instanceof PlaceHolderSetter)
		{
			PlaceHolderSetter phs = (PlaceHolderSetter)visitor;
			Placeholder ph = (Placeholder)values;
			if (ph.getKey().equals(phs.getKey()))
			{
				ph.setValue(validateValues(keys, phs.getValue()));
			}
		}
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { Integer.valueOf(2) /* version */, operators, ReplacedObject.convertArray(keys, Object.class), values, Boolean
				.valueOf(andCondition) });
		// Version 1: new Object[] { operators, ReplacedObject.convertArray(keys, Object.class), values, Boolean.valueOf(andCondition) }
	}

	public SetCondition(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		int version;
		if (members[0] instanceof Integer)
		{
			// versioned
			version = ((Integer)members[i++]).intValue();
		}
		else
		{
			// unversioned, first version
			version = 1;
		}

		operators = (int[])members[i++];
		keys = (IQuerySelectValue[])ReplacedObject.convertArray((Object[])members[i++], IQuerySelectValue.class);
		values = members[i++];
		andCondition = ((Boolean)members[i++]).booleanValue();


		if (version == 1 && operators.length == 1 && (operators[0] == IBaseSQLCondition.EQUALS_OPERATOR || operators[0] == IBaseSQLCondition.NOT_OPERATOR))
		{
			boolean isQuery = false;
			if (values instanceof ISQLSelect)
			{
				isQuery = true;
			}
			else if (values instanceof Placeholder)
			{
				isQuery = ((Placeholder)values).isSet() && ((Placeholder)values).getValue() instanceof ISQLSelect;
			}


			// Before release 8.0 a stored SetCondition was using EQUALS_OPERATOR for subselects, this has been changed to IN_OPERATOR, see SVY-8091.
			if (isQuery)
			{
				operators[0] = operators[0] == IBaseSQLCondition.EQUALS_OPERATOR ? IBaseSQLCondition.IN_OPERATOR : IBaseSQLCondition.NOT_IN_OPERATOR;
			}
		}
	}

}
