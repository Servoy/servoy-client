/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
 * Key class for named value with some obvject in a query structure.
 * 
 * @author rgansevles
 * 
 */

public class ObjectPlaceholderKey<T> implements IPlaceholderKey
{
	protected String name;
	private T object;

	/**
	 * @param name
	 */
	public ObjectPlaceholderKey(T Object, String name)
	{
		this.name = name;
		object = Object;
	}

	public String getName()
	{
		return name;
	}

	public T getObject()
	{
		return object;
	}

	public void setObject(T object)
	{
		this.object = object;
	}


	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		object = (AbstractBaseQuery.acceptVisitor(object, visitor));
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ObjectPlaceholderKey other = (ObjectPlaceholderKey)obj;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return name;
	}

	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), name);
	}

	public ObjectPlaceholderKey(ReplacedObject s)
	{
		name = (String)s.getObject();
	}

}
