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

import com.servoy.j2db.util.Immutable;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;


/**
 * Query Condition class for boolean values.
 * 
 * @author rgansevles
 * 
 */
public final class BooleanCondition implements ISQLCondition, Immutable
{
	public static final BooleanCondition TRUE_CONDITION = new BooleanCondition(true);
	public static final BooleanCondition FALSE_CONDITION = new BooleanCondition(false);

	private final boolean value;

	private BooleanCondition(boolean value)
	{
		this.value = value;
	}

	public boolean getValue()
	{
		return value;
	}

	public static BooleanCondition valueOf(boolean b)
	{
		return b ? TRUE_CONDITION : FALSE_CONDITION;
	}

	public ISQLCondition negate()
	{
		return valueOf(!value);
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (this.value ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final BooleanCondition other = (BooleanCondition)obj;
		if (this.value != other.value) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return value ? "TRUE" : "FALSE"; //$NON-NLS-1$//$NON-NLS-2$
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), Boolean.valueOf(value));
	}

	public BooleanCondition(ReplacedObject s)
	{
		this.value = ((Boolean)s.getObject()).booleanValue();
	}

}
