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

import java.util.Arrays;

import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Base class for query elements that are based on a user defined string and arguments.
 * 
 * @author rgansevles
 * 
 */
public class QueryCustomElement implements IQueryElement
{
	protected final String sql;
	protected final Object[] args;

	public QueryCustomElement(String sql, Object[] args)
	{
		this.sql = sql;
		this.args = args;
	}

	public QueryCustomElement(String sql)
	{
		this.sql = sql;
		this.args = null;
	}

	public String getSql()
	{
		return sql;
	}

	public Object[] getArgs()
	{
		return args;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
	}

	public QueryTable getTable()
	{
		return null;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + AbstractBaseQuery.hashCode(this.args);
		result = PRIME * result + ((this.sql == null) ? 0 : this.sql.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryCustomElement other = (QueryCustomElement)obj;
		if (!Arrays.equals(this.args, other.args)) return false;
		if (this.sql == null)
		{
			if (other.sql != null) return false;
		}
		else if (!this.sql.equals(other.sql)) return false;
		return true;
	}


	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("CUSTOM ELEMENT "); //$NON-NLS-1$
		sb.append(sql);
		if (args != null)
		{
			sb.append(" ("); //$NON-NLS-1$
			sb.append(AbstractBaseQuery.toString(args));
			sb.append(')');
		}
		return sb.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { sql, args });
	}

	public QueryCustomElement(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		this.sql = (String)members[i++];
		this.args = (Object[])members[i++];
	}

}
