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

import java.util.ArrayList;
import java.util.List;

/**
 * And-condition for mobile and regular clients.
 * 
 * @author rgansevles
 *
 */
public class BaseAndCondition extends BaseAndOrCondition<IBaseSQLCondition>
{
	public BaseAndCondition()
	{
	}

	public BaseAndCondition(List<IBaseSQLCondition> conditions)
	{
		super(conditions);
	}

	@Override
	public String getInfix()
	{
		return "and"; //$NON-NLS-1$
	}

	@Override
	public IBaseSQLCondition negate()
	{
		// apply De Morgan's laws
		List<IBaseSQLCondition> nconditions = new ArrayList<IBaseSQLCondition>(conditions.size());
		for (int i = 0; i < conditions.size(); i++)
		{
			nconditions.add(conditions.get(i).negate());
		}
		return new BaseOrCondition(nconditions);
	}

	/**
	 * Combine 2 conditions in an AndCondition. 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static IBaseSQLCondition and(IBaseSQLCondition c1, IBaseSQLCondition c2)
	{
		if (c1 == null)
		{
			return c2;
		}
		if (c2 == null)
		{
			return c1;
		}
		BaseAndCondition and = new BaseAndCondition();
		and.addCondition(c1);
		and.addCondition(c2);
		return and;
	}
}
