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

package com.servoy.j2db.dataprocessing;

import java.io.Serializable;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Table filter definition for conditions using queries.
 *
 * @author rgansevles
 *
 */
public class QueryTableFilterdefinition implements Serializable, TableFilterdefinition, IVisitable, IWriteReplace
{
	private QuerySelect querySelect;

	public QueryTableFilterdefinition(QuerySelect querySelect)
	{
		this.querySelect = querySelect;
	}

	/**
	 * @return the querySelect
	 */
	public QuerySelect getQuerySelect()
	{
		return querySelect;
	}

	@Override
	public void acceptVisitor(IVisitor visitor)
	{
		querySelect = AbstractBaseQuery.acceptVisitor(querySelect, visitor);
	}

	@Override
	public boolean affects(ITable table)
	{
		return querySelect.getTable().getDataSource() != null && querySelect.getTable().getDataSource().equals(table.getDataSource());
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((querySelect == null) ? 0 : querySelect.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QueryTableFilterdefinition other = (QueryTableFilterdefinition)obj;
		if (querySelect == null)
		{
			if (other.querySelect != null) return false;
		}
		else if (!querySelect.equals(other.querySelect)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuilder("QueryTableFilterdefinition[") //
			.append(querySelect).append(']') //
			.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(QueryData.DATAPROCESSING_SERIALIZE_DOMAIN, getClass(), querySelect);
	}

	public QueryTableFilterdefinition(ReplacedObject s)
	{
		querySelect = (QuerySelect)s.getObject();
	}
}
