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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.servoy.base.query.BaseColumnType;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Case-when element in a query structure.
 *
 * @author rgansevles
 *
 */
public final class QuerySearchedCaseExpression implements IQuerySelectValue
{
	private List<QueryWhenClause> whenClauses;

	private IQuerySelectValue otherwiseResult;

	private final String alias;

	public QuerySearchedCaseExpression(List<QueryWhenClause> whenClauses, IQuerySelectValue otherwiseResult, String alias)
	{
		this.whenClauses = whenClauses == null ? new ArrayList<>() : whenClauses;
		this.otherwiseResult = otherwiseResult;
		this.alias = alias;
	}

	@Override
	public IQuerySelectValue asAlias(String newAlias)
	{
		return new QuerySearchedCaseExpression(whenClauses, otherwiseResult, newAlias);
	}

	public String getAlias()
	{
		return alias;
	}

	public List<QueryWhenClause> getWhenClauses()
	{
		return whenClauses;
	}

	public IQuerySelectValue getOtherwiseResult()
	{
		return otherwiseResult;
	}

	@Override
	public BaseColumnType getColumnType()
	{
		// Use the column type of the results
		return Stream.concat(whenClauses.stream().map(QueryWhenClause::getResult), Stream.of(otherwiseResult))
			.filter(Objects::nonNull)
			.map(IQuerySelectValue::getColumnType)
			.reduce(IQuerySelectValue::determineCompatibleColumnType)
			.orElse(null);
	}

	@Override
	public int getFlags()
	{
		// Combine the flags of the results
		return Stream.concat(whenClauses.stream().map(QueryWhenClause::getResult), Stream.of(otherwiseResult))
			.filter(Objects::nonNull)
			.mapToInt(IQuerySelectValue::getFlags)
			.reduce((left, right) -> left | right)
			.orElse(0);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((otherwiseResult == null) ? 0 : otherwiseResult.hashCode());
		result = prime * result + ((whenClauses == null) ? 0 : whenClauses.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QuerySearchedCaseExpression other = (QuerySearchedCaseExpression)obj;
		if (alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!alias.equals(other.alias)) return false;
		if (otherwiseResult == null)
		{
			if (other.otherwiseResult != null) return false;
		}
		else if (!otherwiseResult.equals(other.otherwiseResult)) return false;
		if (whenClauses == null)
		{
			if (other.whenClauses != null) return false;
		}
		else if (!whenClauses.equals(other.whenClauses)) return false;
		return true;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		whenClauses = AbstractBaseQuery.acceptVisitor(whenClauses, visitor);
		otherwiseResult = AbstractBaseQuery.acceptVisitor(otherwiseResult, visitor);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("CASE");

		whenClauses.stream().map(QueryWhenClause::toString).forEach(clause -> sb.append(' ').append(clause));

		return sb.append(" ELSE ").append(otherwiseResult == null ? "<NULL>" : otherwiseResult.toString())
			.append(alias == null ? "" : (" AS " + alias)).toString();
	}


	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { whenClauses, otherwiseResult, alias });
	}

	public QuerySearchedCaseExpression(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		whenClauses = (List<QueryWhenClause>)members[i++];
		otherwiseResult = (IQuerySelectValue)members[i++];
		alias = (String)members[i++];
	}

}
