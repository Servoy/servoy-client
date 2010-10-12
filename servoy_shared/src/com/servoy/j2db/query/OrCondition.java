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

import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Query condition consisting of conditions to be OR-ed.
 * @author rgansevles
 *
 */

public final class OrCondition extends AndOrCondition
{
	public OrCondition()
	{
		super();
	}

	public OrCondition(List<ISQLCondition> conditions)
	{
		super(conditions);
	}

	@Override
	public String getInfix()
	{
		return "or"; //$NON-NLS-1$
	}

	@Override
	public ISQLCondition negate()
	{
		// apply De Morgan's laws
		List<ISQLCondition> nconditions = new ArrayList<ISQLCondition>(conditions.size());
		for (int i = 0; i < conditions.size(); i++)
		{
			nconditions.add(conditions.get(i).negate());
		}
		return new AndCondition(nconditions);
	}

	/**
	 * Combine 2 conditions in an OrCondition. 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static ISQLCondition or(ISQLCondition c1, ISQLCondition c2)
	{
		if (c1 == null)
		{
			return c2;
		}
		if (c2 == null)
		{
			return c1;
		}
		OrCondition or = new OrCondition();
		or.addCondition(c1);
		or.addCondition(c2);
		return or;
	}

	///////// serialization ////////////////


	public OrCondition(ReplacedObject s)
	{
		super(s);
	}
}
