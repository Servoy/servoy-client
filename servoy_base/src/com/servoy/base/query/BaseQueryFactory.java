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
 * Create query objects for mobile clients.
 *
 * @author rgansevles
 *
 */
public class BaseQueryFactory implements IQueryFactory<IBaseSQLCondition, IBaseQuerySelectValue>
{
	public static final BaseQueryFactory INSTANCE = new BaseQueryFactory();

	private BaseQueryFactory()
	{
	}

	@Override
	public IBaseSQLCondition or(IBaseSQLCondition cond1, IBaseSQLCondition cond2)
	{
		return BaseOrCondition.or(cond1, cond2);
	}

	@Override
	public IBaseSQLCondition and(IBaseSQLCondition cond1, IBaseSQLCondition cond2)
	{
		return BaseAndCondition.and(cond1, cond2);
	}

	@Override
	public BaseCompareCondition createCompareCondition(int operator, IBaseQuerySelectValue operand1, Object operand2)
	{
		return new BaseCompareCondition(operator, operand1, operand2);
	}

	@Override
	public BaseQueryColumn createQueryColumn(BaseQueryTable table, int id, String name, int sqlType, int length, int scale, int flags)
	{
		return new BaseQueryColumn(table, id, name, new BaseColumnType(sqlType, length, scale), flags, false);
	}
}
