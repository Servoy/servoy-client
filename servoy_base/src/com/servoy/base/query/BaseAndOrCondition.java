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
package com.servoy.base.query;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base condition class for AndCondition and OrCondition.
 *
 * @author rgansevles
 *
 */
public abstract class BaseAndOrCondition<C extends IBaseSQLCondition> implements IBaseSQLCondition
{
	private static final String[] EMPTY_STRINGS = new String[0];

	protected HashMap<String, List<C>> conditions = null; // named conditions

	public BaseAndOrCondition()
	{
	}

	public BaseAndOrCondition(List<C> conditions)
	{
		if (conditions != null && !conditions.isEmpty())
		{
			this.conditions = new HashMap<>();
			this.conditions.put(null, new ArrayList<>(conditions));
		}
	}

	public BaseAndOrCondition(HashMap<String, List<C>> conditions)
	{
		if (conditions != null && !conditions.isEmpty())
		{
			this.conditions = new HashMap<>();
			this.conditions.putAll(conditions);
		}
	}

	public void addCondition(C condition)
	{
		if (condition == null)
		{
			return;
		}
		if (getClass() == condition.getClass())
		{
			Map<String, List<C>> map = ((BaseAndOrCondition)condition).getConditions();
			if (map != null & map.keySet().size() == 1 && map.keySet().iterator().next() == null)
				// an anonymous conditions
				map.values().forEach(list -> list.forEach(BaseAndOrCondition.this::addCondition));
			return;
		}

		conditions = addToConditionMap(conditions, null, condition);
	}

	public List<C> getConditions(String name)
	{
		if (conditions == null)
		{
			return null;
		}
		return conditions.get(name);
	}

	public String[] getConditionNames()
	{
		if (conditions == null)
		{
			return EMPTY_STRINGS;
		}
		return conditions.keySet().toArray(EMPTY_STRINGS);
	}

	static <C extends IBaseSQLCondition> HashMap<String, List<C>> setInConditionMap(HashMap<String, List<C>> map, String name, C c)
	{
		HashMap<String, List<C>> retval = map;
		if (c == null)
		{
			if (retval != null)
			{
				retval.remove(name);
				if (retval.size() == 0)
				{
					return null;
				}
			}
		}
		else
		{
			if (retval == null)
			{
				retval = new HashMap<>();
			}
			List<C> list = retval.get(name);
			if (list == null)
			{
				list = new ArrayList<C>();
				list.add(c);
			}
			retval.put(name, list);

		}
		return retval;
	}

	static <C extends IBaseSQLCondition> HashMap<String, List<C>> addToConditionMap(HashMap<String, List<C>> map, String name, C c)
	{
		HashMap<String, List<C>> retval = map;
		if (c != null)
		{
			List<C> list = null;
			if (retval == null)
			{
				retval = new HashMap<>();
			}
			else
			{
				list = retval.get(name);
			}

			if (list == null)
			{
				list = new ArrayList<C>();
				retval.put(name, list);
			}
			list.add(c);
		}
		return retval;
	}

	public void setCondition(String name, C c)
	{
		conditions = setInConditionMap(conditions, name, c);
	}

	public void addCondition(String name, C c)
	{
		conditions = addToConditionMap(conditions, name, c);
	}

	public void clear()
	{
		conditions = null;
	}

	public Map<String, List<C>> getConditions()
	{
		return conditions;
	}

	public List<C> getAllConditions()
	{
		return conditions == null ? emptyList() : conditions.values().stream().flatMap(List::stream).collect(toList());
	}

	public abstract C negate();


	public abstract String getInfix();

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.conditions == null) ? 0 : this.conditions.hashCode());
		return result;
	}

	// RAGTEST overal equals en tostring
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BaseAndOrCondition other = (BaseAndOrCondition)obj;
		if (this.conditions == null)
		{
			if (other.conditions != null) return false;
		}
		else if (!this.conditions.equals(other.conditions)) return false;
		return true;
	}


	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append('(');
		for (int i = 0; i < conditions.size(); i++)
		{
			if (i > 0)
			{
				sb.append(' ').append(getInfix().toUpperCase()).append(' ');
			}
			sb.append(conditions.get(i).toString());
		}
		return sb.append(')').toString();
	}
}