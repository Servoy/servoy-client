/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.query;

import java.util.Arrays;

/**
 * Set-condition for mobile and regular clients.
 * 
 * @author rgansevles
 *
 */
public class BaseSetCondition<K extends IBaseQuerySelectValue> implements IBaseSQLCondition
{
	protected int[] operators; // use '=' for in-conditions
	protected K[] keys;
	protected Object values; // maybe placeholder or subquery
	protected boolean andCondition; // true for AND between key/value pairs and OR between records

	public BaseSetCondition(int operators[], K[] keys, Object values, boolean andCondition)
	{
		validateOperators(operators);
		this.values = validateValues(keys, values);
		this.operators = operators;
		this.keys = keys;
		this.andCondition = andCondition;
	}

	protected BaseSetCondition()
	{
		// for deserialization of super class
	}

	private static int[] makeOperatorsArray(int op, int n)
	{
		int[] operators = new int[n];
		for (int i = 0; i < n; i++)
		{
			operators[i] = op;
		}
		return operators;
	}

	/**
	 * Constructor for all the same operators.
	 * 
	 * @param operator
	 * @param keys
	 * @param values
	 * @param andCondition
	 */
	public BaseSetCondition(int operator, K[] keys, Object values, boolean andCondition)
	{
		this(makeOperatorsArray(operator, keys.length), keys, values, andCondition);
	}

	/**
	 * Validate values and convert if neeed.
	 * 
	 * @param keys
	 * @param values
	 */
	protected static <K extends IBaseQuerySelectValue> Object validateValues(K[] keys, Object values)
	{
		Object vals = values;

		if (keys == null || keys.length == 0)
		{
			throw new IllegalArgumentException("Empty key set in set condition"); //$NON-NLS-1$
		}

		if (vals instanceof IQueryValues)
		{
			// placeholder or sub-query: ok
			return vals;
		}

		// convenience: array of objects as wide as keys, convert to array of value arrays
		if (vals instanceof Object[] && !(vals instanceof Object[][]) && ((Object[])vals).length == keys.length)
		{
			Object[][] converted = new Object[((Object[])vals).length][];
			for (int i = 0; i < converted.length; i++)
			{
				converted[i] = new Object[] { ((Object[])vals)[i] };
			}
			vals = converted;
		}


		if (vals == null || !(vals instanceof Object[][]) || ((Object[][])vals).length != keys.length)
		{
			throw new IllegalArgumentException("Value list does not match key list in set condition"); //$NON-NLS-1$
		}

		// ok
		return vals;
	}

	private static void validateOperators(int[] operators)
	{
		if (operators == null || operators.length == 0)
		{
			throw new IllegalArgumentException("operators missing");
		}
		for (int op : operators)
		{
			boolean ok = false;
			int maskedOp = op & OPERATOR_MASK;
			for (int defined : IBaseSQLCondition.ALL_DEFINED_OPERATORS)
			{
				if (maskedOp == defined)
				{
					ok = true;
					break;
				}
			}
			if (!ok)
			{
				throw new IllegalArgumentException("Unknown operator " + op);
			}
			int maskedMod = op & ~OPERATOR_MASK;
			for (int defined : IBaseSQLCondition.ALL_MODIFIERS)
			{
				maskedMod = maskedMod & ~defined;
			}
			if (maskedMod != 0)
			{
				throw new IllegalArgumentException("Unknown operation modifier " + op);
			}
		}
	}

	public K[] getKeys()
	{
		return keys;
	}

	public Object getValues()
	{
		return values;
	}

	public boolean isAndCondition()
	{
		return andCondition;
	}

	public int[] getOperators()
	{
		return operators;
	}

	public IBaseSQLCondition negate()
	{
		int[] negop = new int[operators.length];
		for (int i = 0; i < operators.length; i++)
		{
			negop[i] = OPERATOR_NEGATED[operators[i] & IBaseSQLCondition.OPERATOR_MASK] | (operators[i] & ~IBaseSQLCondition.OPERATOR_MASK);
		}
		return new BaseSetCondition(negop, keys, values, !andCondition);
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
		result = PRIME * result + BaseSetCondition.hashCode(this.operators);
		result = PRIME * result + ((this.values == null) ? 0 : BaseAbstractBaseQuery.arrayHashcode(this.values));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BaseSetCondition other = (BaseSetCondition)obj;
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
}
