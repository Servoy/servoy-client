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
package com.servoy.j2db.persistence;

import java.util.regex.Pattern;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.util.UUID;


/**
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.RELATION_ITEMS)
@ServoyClientSupport(mc = true, wc = true, sc = true)
public class RelationItem extends AbstractBase implements ISupportContentEquals, IPersistCloneable, ICloneable
{

	private static final long serialVersionUID = 1L;

	public static final int[] RELATION_OPERATORS = new int[] {
		// standard set
		ISQLCondition.EQUALS_OPERATOR, ISQLCondition.GT_OPERATOR, ISQLCondition.LT_OPERATOR, ISQLCondition.GTE_OPERATOR, ISQLCondition.LTE_OPERATOR, ISQLCondition.NOT_OPERATOR, ISQLCondition.IN_OPERATOR, ISQLCondition.LIKE_OPERATOR, ISQLCondition.NOT_LIKE_OPERATOR,
		// case insensitive
		ISQLCondition.EQUALS_OPERATOR | ISQLCondition.CASEINSENTITIVE_MODIFIER, ISQLCondition.NOT_OPERATOR |
			ISQLCondition.CASEINSENTITIVE_MODIFIER, ISQLCondition.LIKE_OPERATOR |
				ISQLCondition.CASEINSENTITIVE_MODIFIER, ISQLCondition.NOT_LIKE_OPERATOR | ISQLCondition.CASEINSENTITIVE_MODIFIER,
		// or null
		ISQLCondition.EQUALS_OPERATOR | ISQLCondition.ORNULL_MODIFIER, ISQLCondition.GT_OPERATOR | ISQLCondition.ORNULL_MODIFIER, ISQLCondition.LT_OPERATOR |
			ISQLCondition.ORNULL_MODIFIER, ISQLCondition.GTE_OPERATOR | ISQLCondition.ORNULL_MODIFIER, ISQLCondition.LTE_OPERATOR |
				ISQLCondition.ORNULL_MODIFIER, ISQLCondition.NOT_OPERATOR | ISQLCondition.ORNULL_MODIFIER, ISQLCondition.IN_OPERATOR |
					ISQLCondition.ORNULL_MODIFIER, ISQLCondition.LIKE_OPERATOR |
						ISQLCondition.ORNULL_MODIFIER, ISQLCondition.NOT_LIKE_OPERATOR | ISQLCondition.ORNULL_MODIFIER,
		// case insensitive or null
		ISQLCondition.EQUALS_OPERATOR | ISQLCondition.CASEINSENTITIVE_MODIFIER | ISQLCondition.ORNULL_MODIFIER, ISQLCondition.NOT_OPERATOR |
			ISQLCondition.CASEINSENTITIVE_MODIFIER | ISQLCondition.ORNULL_MODIFIER,
		//
	};

	/**
	 * Constructor I
	 */
	RelationItem(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.RELATION_ITEMS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.AbstractBase#clearChanged()
	 */
	@Override
	public void clearChanged()
	{
		if (isChanged && getParent() instanceof Relation)
		{
			((Relation)getParent()).flushCashedItems();
		}
		super.clearChanged();
	}

	/**
	 * Set the tableName1
	 *
	 * @param arg the tableName1
	 */
	public void setPrimaryDataProviderID(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_PRIMARYDATAPROVIDERID, arg);
	}

	/**
	 * The name of the column from the source table
	 * that this relation item is based on.
	 */
	public String getPrimaryDataProviderID()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_PRIMARYDATAPROVIDERID);
	}

	/**
	 * Set the foreignTableName
	 *
	 * @param arg the foreignTableName
	 */
	public void setForeignColumnName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_FOREIGNCOLUMNNAME, arg);
	}

	/**
	 * The name of the column from the destination table
	 * that this relation item is based on.
	 */
	public String getForeignColumnName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_FOREIGNCOLUMNNAME);
	}

	public static String getOperatorAsString(int op)
	{
		if ((op & IBaseSQLCondition.OPERATOR_MASK) == op)
		{
			// no modifiers
			return IBaseSQLCondition.OPERATOR_STRINGS[op];
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < IBaseSQLCondition.ALL_MODIFIERS.length; i++)
		{
			if ((op & IBaseSQLCondition.ALL_MODIFIERS[i]) != 0)
			{
				sb.append(IBaseSQLCondition.MODIFIER_STRINGS[i]);
			}
		}
		sb.append(IBaseSQLCondition.OPERATOR_STRINGS[op & IBaseSQLCondition.OPERATOR_MASK]);
		return sb.toString();
	}


	/**
	 * Swap the operator, leave the modifier in place
	 *
	 * @param op
	 */
	public static int swapOperator(int op)
	{
		int swapped;
		switch (op & IBaseSQLCondition.OPERATOR_MASK)
		{
			case IBaseSQLCondition.GT_OPERATOR :
				swapped = IBaseSQLCondition.LT_OPERATOR;
				break;

			case IBaseSQLCondition.LT_OPERATOR :
				swapped = IBaseSQLCondition.GT_OPERATOR;
				break;

			case IBaseSQLCondition.GTE_OPERATOR :
				swapped = IBaseSQLCondition.LTE_OPERATOR;
				break;

			case IBaseSQLCondition.LTE_OPERATOR :
				swapped = IBaseSQLCondition.GTE_OPERATOR;
				break;

			case IBaseSQLCondition.NOT_OPERATOR :
				swapped = IBaseSQLCondition.NOT_OPERATOR;
				break;

			case IBaseSQLCondition.EQUALS_OPERATOR :
				swapped = IBaseSQLCondition.EQUALS_OPERATOR;
				break;

			//we have made like an exception since global like column doesn't make any sense.
			case IBaseSQLCondition.LIKE_OPERATOR :
				swapped = IBaseSQLCondition.LIKE_OPERATOR;
				break;

			case IBaseSQLCondition.NOT_LIKE_OPERATOR :
				swapped = IBaseSQLCondition.NOT_LIKE_OPERATOR;
				break;

			default :
				return -1;
		}
		return swapped | (op & ~IBaseSQLCondition.OPERATOR_MASK);
	}

	public static boolean checkIfValidOperator(String op)
	{
		return getValidOperator(op, RELATION_OPERATORS, null) != -1;
	}

	private static int parseOperatorString(String operatorString)
	{
		if (operatorString == null)
		{
			return -1;
		}
		String opString = operatorString;
		int mod = 0;
		boolean foundModifiers = true;
		while (foundModifiers)
		{
			foundModifiers = false;
			for (int m = 0; m < IBaseSQLCondition.ALL_MODIFIERS.length; m++)
			{
				if (opString.startsWith(IBaseSQLCondition.MODIFIER_STRINGS[m]))
				{
					foundModifiers = true;
					opString = opString.substring(IBaseSQLCondition.MODIFIER_STRINGS[m].length());
					mod |= IBaseSQLCondition.ALL_MODIFIERS[m];
					break;
				}
			}
		}

		for (int i = 0; i < IBaseSQLCondition.OPERATOR_STRINGS.length; i++)
		{
			Pattern pattern = Pattern.compile(IBaseSQLCondition.OPERATOR_STRINGS[i].replace(" ", "\\s+"), Pattern.CASE_INSENSITIVE);
			if (pattern.matcher(opString).matches())
			{
				return IBaseSQLCondition.ALL_DEFINED_OPERATORS[i] | mod;
			}
		}

		// not found
		return -1;
	}

	/**
	 * Parse the operator string '[<modifier>]<operator>'
	 *
	 * @param str
	 * @param operators includes modifiers when modifiers is null
	 * @param modifiers
	 */
	public static int getValidOperator(String str, int[] operators, int[] modifiers)
	{
		int operator = parseOperatorString(str);
		if (operator == -1)
		{
			return -1;
		}

		// check against valid operators and modifiers
		if (modifiers == null)
		{
			// operators contains values including modifiers
			for (int element : operators)
			{
				if (element == operator)
				{
					return operator;
				}
			}
			return -1;
		}

		// operators and modifiers are separate
		int mod = operator & ~IBaseSQLCondition.OPERATOR_MASK;
		for (int x : IBaseSQLCondition.ALL_MODIFIERS)
		{
			mod &= ~x;
		}
		if (mod != 0)
		{
			// illegal modifiers left
			return -1;
		}

		int op = operator & IBaseSQLCondition.OPERATOR_MASK;
		for (int x : operators)
		{
			if (x == op)
			{
				return operator;
			}
		}

		// not allowed
		return -1;
	}

	/**
	 * The operator that defines the relationship between the primary dataprovider
	 * and the foreign column.
	 */
	public int getOperator()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_OPERATOR).intValue();
	}

	/**
	 * Sets the operator.
	 *
	 * @param operator The operator to set
	 */
	public void setOperator(int operator)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_OPERATOR, operator);
	}

	public boolean contentEquals(Object obj)
	{
		if (obj instanceof RelationItem)
		{
			RelationItem other = (RelationItem)obj;
			return (getPrimaryDataProviderID().equals(other.getPrimaryDataProviderID()) && getOperator() == other.getOperator() &&
				getForeignColumnName() == other.getForeignColumnName());
		}
		return false;
	}

}