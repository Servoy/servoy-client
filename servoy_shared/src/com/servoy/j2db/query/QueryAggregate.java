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

import com.servoy.base.query.BaseColumnType;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Aggregates in queries.
 *
 * @author rgansevles
 *
 */
public final class QueryAggregate implements IQuerySelectValue, IQueryElement, IQuerySelectValueSupportsSkip
{
	// aggregate types
	public static final int COUNT = 0;
	public static final int MAX = 1;
	public static final int MIN = 2;
	public static final int AVG = 3;
	public static final int SUM = 4;
	public static final int[] ALL_DEFINED_AGGREGATES = new int[] { COUNT, MAX, MIN, AVG, SUM };

	private final static int SKIP_FLAG = 1 << 16; // used in serialization, should not overlap with aggregate types

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
	private final String alias;
	private final boolean skipInResult;

	public QueryAggregate(int type, IQuerySelectValue aggregee, String name, String alias, boolean skipInResult)
	{
		this.type = type;
		this.aggregee = aggregee;
		this.name = name;
		this.alias = alias;
		this.skipInResult = skipInResult;
	}

	public QueryAggregate(int type, IQuerySelectValue aggregee, String name, String alias)
	{
		this(type, aggregee, name, alias, false);
	}

	public QueryAggregate(int type, IQuerySelectValue aggregee, String name)
	{
		this(type, aggregee, name, null, false);
	}

	@Override
	public IQuerySelectValue asAlias(String newAlias)
	{
		return new QueryAggregate(type, aggregee, name, newAlias);
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
		return alias == null ? name : alias;
	}

	public IQuerySelectValue getAggregee()
	{
		return aggregee;
	}

	public QueryColumn getColumn()
	{
		// for aggregates that do not change the return type return the column of the aggregee
		if (type == COUNT || aggregee == null)
		{
			return null;
		}
		return aggregee.getColumn();
	}

	@Override
	public BaseColumnType getColumnType()
	{
		if (type == COUNT)
		{
			return ColumnType.getColumnType(IColumnTypes.INTEGER);
		}
		return IQuerySelectValue.super.getColumnType();
	}

	@Override
	public int getFlags()
	{
		if (type == COUNT)
		{
			return 0;
		}
		return IQuerySelectValue.super.getFlags();
	}

	public String getAggregateName()
	{
		return AGGREGATE_TYPE_HIBERNATE[type];
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.query.IQuerySelectValueSupportsSkip#skip()
	 */
	@Override
	public boolean skip()
	{
		return skipInResult;
	}

	@Override
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aggregee == null) ? 0 : aggregee.hashCode());
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (skipInResult ? 1231 : 1237);
		result = prime * result + type;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QueryAggregate other = (QueryAggregate)obj;
		if (aggregee == null)
		{
			if (other.aggregee != null) return false;
		}
		else if (!aggregee.equals(other.aggregee)) return false;
		if (alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!alias.equals(other.alias)) return false;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (skipInResult != other.skipInResult) return false;
		if (type != other.type) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuilder(getAggregateName().toUpperCase()).append('(' + aggregee.toString()).append(") ").append(name).append( //$NON-NLS-1$
			alias == null ? "" : (" AS " + alias)).toString();
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { new Integer(type + (skipInResult ? SKIP_FLAG : 0)), aggregee, name, alias });
	}

	public QueryAggregate(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		int typeAndSkip = ((Integer)members[i++]).intValue();
		type = typeAndSkip & ~SKIP_FLAG;
		skipInResult = (typeAndSkip & SKIP_FLAG) != 0;
		aggregee = (IQuerySelectValue)members[i++];
		name = (String)members[i++];
		alias = (i < members.length) ? (String)members[i++] : null;
	}

}
