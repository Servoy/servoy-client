/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Case-when  when-element in a query structure.
 *
 * @author rgansevles
 *
 */
public class QueryWhenClause implements IVisitable, IWriteReplace
{
	private ISQLCondition condition;
	private IQuerySelectValue result;

	public QueryWhenClause(ISQLCondition condition, IQuerySelectValue result)
	{
		this.condition = condition;
		this.result = result;
	}

	public ISQLCondition getCondition()
	{
		return condition;
	}

	public IQuerySelectValue getResult()
	{
		return result;
	}

	public void acceptVisitor(IVisitor visitor)
	{
		condition = AbstractBaseQuery.acceptVisitor(condition, visitor);
		result = AbstractBaseQuery.acceptVisitor(result, visitor);
	}

	@Override
	public String toString()
	{
		return new StringBuilder("WHEN ")
			.append(condition == null ? "<NULL>" : condition.toString())
			.append(" THEN ")
			.append(result == null ? "<NULL>" : result.toString())
			.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QueryWhenClause other = (QueryWhenClause)obj;
		if (condition == null)
		{
			if (other.condition != null) return false;
		}
		else if (!condition.equals(other.condition)) return false;
		if (result == null)
		{
			if (other.result != null) return false;
		}
		else if (!result.equals(other.result)) return false;
		return true;
	}

///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { condition, result });
	}

	public QueryWhenClause(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		condition = (ISQLCondition)members[0];
		result = (IQuerySelectValue)members[1];
	}

}
