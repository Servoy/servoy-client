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

import com.servoy.j2db.util.IVisitor;
import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Condition for 'where exists (sql)'.
 * 
 * @author rob
 * 
 */
public class ExistsCondition implements ISQLCondition
{
	private ISQLSelect sqlSelect;
	private final boolean exists;

	public ExistsCondition(ISQLSelect sqlSelect, boolean exists)
	{
		this.sqlSelect = sqlSelect;
		this.exists = exists;
	}

	public ISQLSelect getSqlSelect()
	{
		return sqlSelect;
	}

	public boolean getExists()
	{
		return exists;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		sqlSelect = AbstractBaseQuery.acceptVisitor(sqlSelect, visitor);
	}

	public ISQLCondition negate()
	{
		return new ExistsCondition(sqlSelect, !exists);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (exists ? 1231 : 1237);
		result = prime * result + ((sqlSelect == null) ? 0 : sqlSelect.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ExistsCondition other = (ExistsCondition)obj;
		if (exists != other.exists) return false;
		if (sqlSelect == null)
		{
			if (other.sqlSelect != null) return false;
		}
		else if (!sqlSelect.equals(other.sqlSelect)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuffer().append(exists ? "" : "NOT ").append("EXISTS (").append(sqlSelect.toString()).append(')').toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { sqlSelect, Boolean.valueOf(exists) });
	}

	public ExistsCondition(ReplacedObject s)
	{
		Object[] array = (Object[])s.getObject();
		sqlSelect = (ISQLSelect)array[0];
		exists = ((Boolean)array[1]).booleanValue();
	}
}