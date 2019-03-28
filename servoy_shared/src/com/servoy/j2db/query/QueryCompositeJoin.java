/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import java.util.stream.Stream;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Multiple joins added together.
 *
 * @author rgansevles
 *
 */
public final class QueryCompositeJoin implements ISQLTableJoin
{
	private String name;
	private List<ISQLJoin> joins = new ArrayList<ISQLJoin>();

	private transient Object origin; // origin, transient, only used in the client


	public QueryCompositeJoin(String name)
	{
		this.name = name;
	}

	public QueryCompositeJoin(String name, List< ? extends ISQLJoin> joins)
	{
		this(name);
		if (joins != null)
		{
			this.joins.addAll(joins);
		}
	}

	public String getName()
	{
		return name;
	}

	public List<ISQLJoin> getJoins()
	{
		return joins;
	}

	public BaseQueryTable getPrimaryTable()
	{
		if (joins.size() > 0)
		{
			return joins.get(0).getPrimaryTable();
		}
		return null;
	}

	@Override
	public ITableReference getForeignTableReference()
	{
		if (joins.size() > 0)
		{
			ISQLJoin join = joins.get(joins.size() - 1);
			if (join instanceof ISQLTableJoin)
			{
				return ((ISQLTableJoin)join).getForeignTableReference();
			}
		}
		return null;
	}

	public AndCondition getCondition()
	{
		// get the last condition
		if (joins.size() > 0)
		{
			ISQLJoin join = joins.get(joins.size() - 1);
			if (join instanceof ISQLTableJoin)
			{
				return ((ISQLTableJoin)join).getCondition();
			}
		}
		return null;
	}

	public boolean hasInnerJoin()
	{
		for (int i = 0; i < joins.size(); i++)
		{
			ISQLJoin join = joins.get(i);
			if (!(join instanceof ISQLTableJoin) || ((ISQLTableJoin)join).hasInnerJoin())
			{
				// we don't know for custom joins, so return true to be on the safe side
				return true;
			}
		}
		return false;
	}

	/**
	 * Invert the direction of this join.
	 */
	public void invert(String newName)
	{
		List<ISQLJoin> invertedJoins = new ArrayList<ISQLJoin>();
		for (int i = joins.size() - 1; i >= 0; i--)
		{
			ISQLJoin join = joins.get(i);
			if (join instanceof ISQLTableJoin)
			{
				((ISQLTableJoin)join).invert(newName);
			}
			invertedJoins.add(join);
		}
		joins = invertedJoins;
		this.name = newName;
	}

	@Override
	public int getJoinType()
	{
		return tableJoins().findAny() //
			.map(ISQLTableJoin::getJoinType) //
			.orElse(Integer.valueOf(ISQLJoin.INNER_JOIN)) //
			.intValue();
	}

	public void setJoinType(int joinType)
	{
		tableJoins().forEach(join -> join.setJoinType(joinType));
	}

	@Override
	public boolean isPermanent()
	{
		return tableJoins().anyMatch(ISQLTableJoin::isPermanent);
	}


	@Override
	public void setPermanent(boolean permanent)
	{
		tableJoins().forEach(join -> join.setPermanent(permanent));
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}


	public void acceptVisitor(IVisitor visitor)
	{
		joins = AbstractBaseQuery.acceptVisitor(joins, visitor);
	}

	public static List<ISQLJoin> flatten(List<ISQLJoin> joins)
	{
		// flatten the list of joins, add all QueryCompositeJoin joins to the main list
		return flatten(joins, 0);
	}

	private Stream<ISQLTableJoin> tableJoins()
	{
		return joins.stream() //
			.filter(ISQLTableJoin.class::isInstance) //
			.map(ISQLTableJoin.class::cast);
	}

	public void setOrigin(Object origin)
	{
		this.origin = origin;
	}

	public Object getOrigin()
	{
		return origin;
	}

	private static List<ISQLJoin> flatten(List<ISQLJoin> joins, int start)
	{
		if (joins == null || (start > 0 && joins.size() <= start))
		{
			return null;
		}

		for (int i = start; i < joins.size(); i++)
		{
			ISQLJoin join = joins.get(i);
			if (join instanceof QueryCompositeJoin)
			{
				// only start copying stuff when there is actually something to flatten
				if (start == 0 && joins.size() == 1)
				{
					// usual case: just 1 composite join
					return flatten(((QueryCompositeJoin)join).getJoins());
				}
				List<ISQLJoin> flattened = new ArrayList<ISQLJoin>();
				for (int j = start; j < i - 1; j++)
				{
					flattened.add(joins.get(j));
				}
				List<ISQLJoin> f = flatten(((QueryCompositeJoin)join).getJoins());
				if (f != null)
				{
					flattened.addAll(f);
				}
				f = flatten(joins, i + 1);
				if (f != null)
				{
					flattened.addAll(f);
				}
				return flattened;
			}
		}

		if (start == 0)
		{
			return joins; // there was nothing to flatten
		}
		return joins.subList(start, joins.size());
	}

	@Override
	public String toString()
	{
		return new StringBuilder("Joins(").append(name).append(')').append(joins.toString()).toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { name, joins });
	}

	public QueryCompositeJoin(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		name = (String)members[i++];
		joins = (List<ISQLJoin>)members[i++];
	}
}
