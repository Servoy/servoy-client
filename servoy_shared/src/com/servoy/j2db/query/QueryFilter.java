/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.util.List;

import com.servoy.base.query.BaseAbstractBaseQuery;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Filter with joins and a condition.
 *
 * In case of filters on dataproviders, the joins and pkColumns are null.
 *
 * @author rgansevles
 *
 */
public class QueryFilter implements IVisitable, IWriteReplace, ISQLCloneable
{
	private List<ISQLJoin> joins;
	private List<QueryColumn> pkColumns;
	private ISQLCondition condition;

	/**
	 * @param condition
	 */
	public QueryFilter(ISQLCondition condition)
	{
		this(null, null, condition);
	}

	/**
	 * @param joins
	 * @param pkColumns
	 * @param condition
	 */
	public QueryFilter(List<ISQLJoin> joins, List<QueryColumn> pkColumns, ISQLCondition condition)
	{
		this.joins = joins;
		this.pkColumns = pkColumns;
		this.condition = condition;
	}

	/**
	 * @return the joins
	 */
	public List<ISQLJoin> getJoins()
	{
		return joins;
	}

	/**
	 * @return the pkColumns
	 */
	public List<QueryColumn> getPkColumns()
	{
		return pkColumns;
	}

	/**
	 * @return the condition
	 */
	public ISQLCondition getCondition()
	{
		return condition;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	@Override
	public void acceptVisitor(IVisitor visitor)
	{
		joins = AbstractBaseQuery.acceptVisitor(joins, visitor);
		pkColumns = AbstractBaseQuery.acceptVisitor(pkColumns, visitor);
		condition = AbstractBaseQuery.acceptVisitor(condition, visitor);
	}

	@Override
	public String toString()
	{
		return new StringBuilder("QueryFilter [joins=").append(BaseAbstractBaseQuery.toString(joins))
			.append(", pkColumns=").append(BaseAbstractBaseQuery.toString(pkColumns))
			.append(", condition=").append(BaseAbstractBaseQuery.toString(condition))
			.append(']').toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { joins, pkColumns, condition });
	}

	public QueryFilter(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		joins = (List<ISQLJoin>)members[i++];
		pkColumns = (List<QueryColumn>)members[i++];
		condition = (ISQLCondition)members[i++];
	}
}
