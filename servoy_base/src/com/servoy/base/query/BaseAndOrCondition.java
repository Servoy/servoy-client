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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base condition class for AndCondition and OrCondition.
 *
 * @author rgansevles
 *
 */
public abstract class BaseAndOrCondition<C extends IBaseSQLCondition> implements IBaseSQLCondition
{
	private static final String[] EMPTY_STRINGS = new String[0];

	protected HashMap<String, List<C>> conditions = null; // named conditions, null key for anonymous conditions

	public BaseAndOrCondition()
	{
	}

	public BaseAndOrCondition(List<C> conditions)
	{
		if (conditions != null && !conditions.isEmpty())
		{
			HashMap<String, List<C>> map = new HashMap<>();
			map.put(null, conditions);
			this.conditions = validateConditions(map);
		}
	}

	public BaseAndOrCondition(Map<String, List<C>> conditions)
	{
		if (conditions != null && !conditions.isEmpty())
		{
			this.conditions = new HashMap<>(validateConditions(conditions));
		}
	}

	protected static <C, M extends Map<String, List<C>>> M validateConditions(M conditions)
	{
		if (conditions != null)
		{
			if (conditions.isEmpty())
			{
				// when there are no conditions the map should be null
				throw new IllegalArgumentException("Empty conditions map");
			}
			conditions.forEach((name, list) -> {
				if (list == null)
				{
					throw new IllegalArgumentException("Null conditions list for name '" + name + "'");
				}
				if (list.isEmpty())
				{
					// If a named condition is made empty, the key should be removed
					throw new IllegalArgumentException("Empty conditions list for name '" + name + "'");
				}
				if (list.stream().anyMatch(Objects::isNull))
				{
					throw new IllegalArgumentException("Conditions list contains null value for name '" + name + "'");
				}
			});
		}
		return conditions;
	}

	public boolean isEmpty()
	{
		return conditions == null;
	}

	/**
	 * Add anonymous condition
	 */
	public void addCondition(C condition)
	{
		if (condition == null)
		{
			return;
		}
		if (getClass() == condition.getClass())
		{
			Map<String, List<C>> map = ((BaseAndOrCondition)condition).conditions;
			if (map != null & map.keySet().size() == 1 && map.keySet().iterator().next() == null)
				// an anonymous conditions, add to current anonymous condition
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

	private static <C extends IBaseSQLCondition> HashMap<String, List<C>> setInConditionMap(HashMap<String, List<C>> map, String name, C c)
	{
		HashMap<String, List<C>> retval = map;
		if (c == null)
		{
			// remove
			if (retval == null)
			{
				return null;
			}
			retval.remove(name);
			if (retval.isEmpty())
			{
				return null;
			}
		}
		else
		{
			if (retval == null)
			{
				retval = new HashMap<>();
			}
			else
			{
				retval.remove(name);
			}
			List<C> list = new ArrayList<C>();
			list.add(c);
			retval.put(name, list);
		}
		return validateConditions(retval);
	}

	private static <C extends IBaseSQLCondition> HashMap<String, List<C>> addToConditionMap(HashMap<String, List<C>> map, String name, C c)
	{
		if (c == null)
		{
			return map;
		}

		HashMap<String, List<C>> retval = map;
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

		return validateConditions(retval);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.conditions == null) ? 0 : this.conditions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BaseAndOrCondition< ? > other = (BaseAndOrCondition< ? >)obj;
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
		StringBuilder sb = new StringBuilder();
		sb.append('(');

		if (conditions != null)
		{
			sb.append(conditions.entrySet().stream().map(entry -> {
				StringBuilder esb = new StringBuilder();
				if (entry.getKey() != null)
				{
					esb.append(entry.getKey()).append(": ");
				}
				if (entry.getValue().size() > 1)
				{
					esb.append("[");
				}
				esb.append(entry.getValue().stream().map(C::toString).collect(joining(", ")));
				if (entry.getValue().size() > 1)
				{
					esb.append("]");
				}
				return esb.toString();
			}).collect(joining(", ")));
		}

		return sb.append(')').toString();
	}
}