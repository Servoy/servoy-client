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

package com.servoy.j2db.query;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IQueryFactory;

/**
 * Create queries and conditions for regular clients.
 *
 * @author rgansevles
 *
 */
public class QueryFactory implements IQueryFactory<ISQLCondition, IQuerySelectValue>
{
	public static final QueryFactory INSTANCE = new QueryFactory();

	private QueryFactory()
	{
	}

	@Override
	public ISQLCondition or(ISQLCondition cond1, ISQLCondition cond2)
	{
		return OrCondition.or(cond1, cond2);
	}

	@Override
	public ISQLCondition and(ISQLCondition cond1, ISQLCondition cond2)
	{
		return AndCondition.and(cond1, cond2);
	}

	@Override
	public CompareCondition createCompareCondition(int operator, IQuerySelectValue operand1, Object operand2)
	{
		return new CompareCondition(operator, operand1, operand2);
	}

	@Override
	public QueryColumn createQueryColumn(BaseQueryTable table, int id, String name, int sqlType, int length, int scale, int flags)
	{
		return new QueryColumn(table, id, name, sqlType, length, scale, flags);
	}

}
