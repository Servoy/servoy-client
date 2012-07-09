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

import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Aggregates in queries.
 * 
 * @author rgansevles
 * 
 */
public final class QueryAggregate implements IQuerySelectValue
{
	// aggregate types
	public static final int COUNT = 0;
	public static final int MAX = 1;
	public static final int MIN = 2;
	public static final int AVG = 3;
	public static final int SUM = 4;
	public static final int[] ALL_DEFINED_AGGREGATES = new int[] { COUNT, MAX, MIN, AVG, SUM };

	public static final String[] AGGREGATE_TYPE_HIBERNATE = new String[] { "count", //$NON-NLS-1$
	"max", //$NON-NLS-1$
	"min", //$NON-NLS-1$
	"avg", //$NON-NLS-1$
	"sum" //$NON-NLS-1$
	};

	public static final String ASTERIX = "*";

	private final int type;
	private IQuerySelectValue aggregee;
	private final String name;

	public QueryAggregate(int type, IQuerySelectValue aggregee, String name)
	{
		this.type = type;
		this.aggregee = aggregee;
		this.name = name;
	}

	public int getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public String getAlias()
	{
		return name;
	}

	public IQuerySelectValue getAggregee()
	{
		return aggregee;
	}

	public QueryColumn getColumn()
	{
		if (aggregee == null)
		{
			return null;
		}
		return aggregee.getColumn();
	}

	public String getAggregateName()
	{
		return AGGREGATE_TYPE_HIBERNATE[type];
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		aggregee = AbstractBaseQuery.acceptVisitor(aggregee, visitor);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((this.aggregee == null) ? 0 : this.aggregee.hashCode());
		result = PRIME * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = PRIME * result + this.type;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryAggregate other = (QueryAggregate)obj;
		if (this.aggregee == null)
		{
			if (other.aggregee != null) return false;
		}
		else if (!this.aggregee.equals(other.aggregee)) return false;
		if (this.name == null)
		{
			if (other.name != null) return false;
		}
		else if (!this.name.equals(other.name)) return false;
		if (this.type != other.type) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuffer(getAggregateName().toUpperCase()).append('(' + aggregee.toString()).append(") ").append(name).toString(); //$NON-NLS-1$
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { new Integer(type), aggregee, name });
	}

	public QueryAggregate(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		type = ((Integer)members[i++]).intValue();
		aggregee = (IQuerySelectValue)members[i++];
		name = (String)members[i++];
	}


}
