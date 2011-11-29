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
 * Placeholder class, container for named value in a query structure.
 * 
 * @author rgansevles
 * 
 */
public class Placeholder implements IQueryElement
{
	private IPlaceholderKey key;
	private Object value = null;
	boolean set = false;

	/**
	 * @param name
	 */
	public Placeholder(IPlaceholderKey key)
	{
		this.key = key;
	}

	public Object getValue()
	{
		if (!set)
		{
			throw new IllegalStateException("Value " + key + " not set"); //$NON-NLS-1$//$NON-NLS-2$
		}
		return value;
	}

	public void setValue(Object value)
	{
		this.value = value;
		set = true;
	}

	public void clear()
	{
		value = null;
		set = false;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		key = AbstractBaseQuery.acceptVisitor(key, visitor);
	}

	public boolean isSet()
	{
		return set;
	}

	public IPlaceholderKey getKey()
	{
		return key;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
		result = prime * result + (this.set ? 1231 : 1237);
		// only look at value is set is true
		result = prime * result + (((!set) || this.value == null) ? 0 : AbstractBaseQuery.arrayHashcode(this.value));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final Placeholder other = (Placeholder)obj;
		if (this.key == null)
		{
			if (other.key != null) return false;
		}
		else if (!this.key.equals(other.key)) return false;
		if (this.set != other.set) return false;
		if (set) // only look at value is set is true
		{
			return AbstractBaseQuery.arrayEquals(this.value, other.value);
		}
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("${").append(key.toString()); //$NON-NLS-1$
		if (set)
		{
			sb.append('=').append(AbstractBaseQuery.toString(value));
		}
		sb.append('}');
		return sb.toString();
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { key, value, Boolean.valueOf(set) });
	}

	public Placeholder(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		key = (IPlaceholderKey)members[i++];
		value = members[i++];
		set = ((Boolean)members[i++]).booleanValue();
	}

}
