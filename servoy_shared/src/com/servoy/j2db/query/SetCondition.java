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

import java.util.Arrays;

import com.servoy.base.query.BaseAbstractBaseQuery;
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
		int[] negop = new int[operators.length];
		for (int i = 0; i < operators.length; i++)
		{
			negop[i] = OPERATOR_NEGATED[operators[i] & IBaseSQLCondition.OPERATOR_MASK] | (operators[i] & ~IBaseSQLCondition.OPERATOR_MASK);
		}
		return new SetCondition(negop, keys, values, !andCondition);
	}


	public void acceptVisitor(IVisitor visitor)
	{
		keys = AbstractBaseQuery.acceptVisitor(keys, visitor);
		values = AbstractBaseQuery.acceptVisitor(values, visitor);
		if (visitor instanceof PlaceHolderSetter)
		{
			PlaceHolderSetter phs = (PlaceHolderSetter)visitor;
			Placeholder ph = null;
			Object phValue = null;
			if (values instanceof Placeholder)
			{
				ph = (Placeholder)values;
				phValue = phs.getValue();
			}
			else if (values instanceof Object[] && ((Object[])values).length == 1)
			{
				// When a placeholder is used with the QueryBuilder and the parameter value is set with an array we have to get the array and turn [ val1, ,,,, valn ] into [[ val1, ,,,, valn ]].
				// this happens with code:
				//    query.where.add(query.columns.mycolumn.isin(query.getParameter('myparam')));
				//    query.params['myparam'] = ['x', 'y', 'z']
				Object values0 = ((Object[])values)[0];
				if (values0 instanceof Object[] && ((Object[])values0).length == 1 && ((Object[])values0)[0] instanceof QueryColumnValue)
				{
					QueryColumnValue colValue = (QueryColumnValue)((Object[])values0)[0];
					if (colValue.getValue() instanceof Placeholder && phs.getValue() instanceof Object[])
					{
						ph = (Placeholder)colValue.getValue();
						phValue = new Object[][] { (Object[])phs.getValue() }; // 1 key, values in array of dimension 1
					}
				}
			}
			if (ph != null && ph.getKey().equals(phs.getKey()))
			{
				ph.setValue(validateValues(keys, phValue));
			}
		}
	}


	private static int hashCode(int[] array)
	{
		final int PRIME = 31;
		if (array == null) return 0;
		int result = 1;
		for (int element : array)
		{
			result = PRIME * result + element;
		}
		return result;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (this.andCondition ? 1231 : 1237);
		result = PRIME * result + BaseAbstractBaseQuery.hashCode(this.keys);
		result = PRIME * result + SetCondition.hashCode(this.operators);
		result = PRIME * result + ((this.values == null) ? 0 : BaseAbstractBaseQuery.arrayHashcode(this.values));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final SetCondition other = (SetCondition)obj;
		if (this.andCondition != other.andCondition) return false;
		if (!Arrays.equals(this.keys, other.keys)) return false;
		if (!Arrays.equals(this.operators, other.operators)) return false;
		return BaseAbstractBaseQuery.arrayEquals(this.values, other.values);
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();

		sb.append('(');
		for (int k = 0; k < keys.length; k++)
		{
			if (k > 0)
			{
				sb.append('|');
			}
			sb.append(keys[k].toString());
		}
		sb.append(')');
		if (keys.length > 1)
		{
			sb.append(andCondition ? "AND" : "OR"); //$NON-NLS-1$//$NON-NLS-2$
		}
		for (int o = 0; o < operators.length; o++)
		{
			if (o > 0)
			{
				sb.append('|');
			}
			sb.append(IBaseSQLCondition.OPERATOR_STRINGS[operators[o] & IBaseSQLCondition.OPERATOR_MASK].toUpperCase());
			int modifiers = (operators[0] & ~IBaseSQLCondition.OPERATOR_MASK);
			if (modifiers != 0)
			{
				sb.append('(');
				// modifiers
				boolean added = false;
				for (int m = 0; m < IBaseSQLCondition.ALL_MODIFIERS.length; m++)
				{
					if ((m & IBaseSQLCondition.ALL_MODIFIERS[m]) != 0)
					{
						if (added)
						{
							sb.append(',');
						}
						sb.append(IBaseSQLCondition.MODIFIER_STRINGS[m]);
						added = true;
					}
				}
				sb.append(')');
			}
		}
		sb.append('(');
		if (values instanceof Object[][])
		{
			Object[][] vals = (Object[][])values;
			for (int k = 0; k < vals.length; k++)
			{
				if (k > 0)
				{
					sb.append('|');
				}
				sb.append(BaseAbstractBaseQuery.toString(vals[k]));
			}
		}
		else
		{
			sb.append(BaseAbstractBaseQuery.toString(values));
		}
		sb.append(')');

		return sb.toString();
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { Integer.valueOf(2) /* version */, operators, ReplacedObject.convertArray(keys, Object.class), values, Boolean.valueOf(andCondition) });
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
