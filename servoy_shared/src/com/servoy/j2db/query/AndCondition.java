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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Query condition consisting of conditions to be AND-ed.
 *
 * @author rgansevles
 *
 */
public final class AndCondition extends AndOrCondition
{

	public AndCondition()
	{
		super();
	}

	public AndCondition(String name, ISQLCondition condition)
	{
		this(singletonMap(name, asList(condition)));
	}

	public AndCondition(List<ISQLCondition> conditions)
	{
		super(conditions);
	}

	public AndCondition(Map<String, List<ISQLCondition>> conditions)
	{
		super(conditions);
	}

	@Override
	public String getInfix()
	{
		return "and"; //$NON-NLS-1$
	}

	@Override
	public ISQLCondition negate()
	{
		if (conditions == null)
		{
			return new OrCondition();
		}

		// apply De Morgan's laws
		HashMap<String, List<ISQLCondition>> nconditions = new HashMap<>(conditions.size());
		for (Entry<String, List<ISQLCondition>> entry : conditions.entrySet())
		{
			nconditions.put(entry.getKey(), entry.getValue().stream().map(ISQLCondition::negate).collect(toList()));
		}
		return new OrCondition(nconditions);
	}

	/**
	 * Combine 2 conditions in an AndCondition.
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static ISQLCondition and(ISQLCondition c1, ISQLCondition c2)
	{
		if (c1 == null)
		{
			return c2;
		}
		if (c2 == null)
		{
			return c1;
		}
		AndCondition and = new AndCondition();
		and.addCondition(c1);
		and.addCondition(c2);
		return and;
	}

	public static AndCondition and(AndCondition addTo, ISQLCondition c)
	{
		if (c == null)
		{
			return addTo;
		}
		AndCondition and;
		if (addTo != null)
		{
			and = addTo;
		}
		else
		{
			and = new AndCondition();
		}
		and.addCondition(c);
		return and;
	}

	public static ISQLCondition and(List<ISQLCondition> conditions)
	{
		if (conditions == null || conditions.isEmpty())
		{
			return null;
		}
		if (conditions.size() == 1)
		{
			return conditions.get(0);
		}
		return new AndCondition(conditions);
	}

	///////// serialization ////////////////


	public AndCondition(ReplacedObject so)
	{
		super(so);
	}

}
