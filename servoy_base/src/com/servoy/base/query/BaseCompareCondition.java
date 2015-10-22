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

/**
 * Compare-condition for mobile and regular clients.
 * 
 * @author rgansevles
 *
 */

public class BaseCompareCondition extends BaseSetCondition<IBaseQuerySelectValue>
{

	public BaseCompareCondition(int operator, IBaseQuerySelectValue operand1, Object operand2)
	{
		super(new int[] { operator }, new IBaseQuerySelectValue[] { operand1 }, new Object[][] { new Object[] { operand2 } }, true);
	}

	public int getOperator()
	{
		return getOperators()[0];
	}

	public IBaseQuerySelectValue getOperand1()
	{
		return getKeys()[0];
	}

	public Object getOperand2()
	{
		return ((Object[][])getValues())[0][0];
	}

	@Override
	public IBaseSQLCondition negate()
	{
		int op = getOperator();
		return new BaseCompareCondition(OPERATOR_NEGATED[op & IBaseSQLCondition.OPERATOR_MASK] | (op & ~IBaseSQLCondition.OPERATOR_MASK), getOperand1(),
			getOperand2());
	}
}
