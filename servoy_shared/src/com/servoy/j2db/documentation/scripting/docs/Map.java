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
package com.servoy.j2db.documentation.scripting.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Map", scriptingName = "Map")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Map
{
	/**
	 * The size accessor property returns the number of elements in a Map object.
	 *
	 * @sample map.size;
	 *
	 */
	public Number js_getSize()
	{
		return null;
	}

	public void js_setSize(Number length)
	{
	}

	/**
	 * The set() method adds or updates an entry in a Map object with a specified key and a value.
	 *
	 * @sample map.set(keyObject, value);
	 *
	 * @param key
	 * @param value
	 *
	 */
	public Map js_set(Object key, Object value)
	{
		return null;
	}

	/**
	 * The get() method returns a specified element from a Map object. If the value that is associated to the provided key is an object, then you will get a reference to that object and any change made to that object will effectively modify it inside the Map object.
	 *
	 * @sample var value = map.get(key);
	 *
	 * @param key
	 */
	public Object js_get(Object key)
	{
		return null;
	}

	/**
	 * The clear() method removes all elements from a Map object.
	 *
	 * @sample map.clear();
	 *
	 */
	public void js_clear()
	{
	}

	/**
	 * The delete() method removes the specified element from a Map object by key.
	 *
	 * @sample var success = map.delete(key);
	 *
	 * @param key
	 */
	public boolean js_delete(Object key)
	{
		return true;
	}

	/**
	 * The entries() method returns a new iterator object that contains the [key, value] pairs for each element in the Map object in insertion order. In this particular case, this iterator object is also an iterable, so the for-of loop can be used..
	 *
	 * @sample for(var entry of map.entries()) {}
	 *
	 * @return the iterator that can be used in for of loops
	 */
	public Iterator js_entries()
	{
		return null;
	}

	/**
	 * returns a decimal code of the char in the string.
	 *
	 * @sample map.forEach(function(keyValuePair) {});
	 *
	 * @param callback
	 * @param thisArgument
	 *
	 */
	public void js_forEach(Function calback, Object thisArgument)
	{
		return;
	}

	/**
	 * returns a string that appends the parameter string to the string.
	 *
	 * @sample var containsKey = map.has(key);
	 *
	 * @param key
	 *
	 */
	public boolean js_has(Object key)
	{
		return true;
	}

	/**
	 * The keys() method returns a new iterator object that contains the keys for each element in the Map object in insertion order. In this particular case, this iterator object is also an iterable, so a for...of loop can be used.
	 *
	 *  @sample var values = map.keys();
	 *
	 * @return the iterator that can be used in for of loops
	 */
	public Iterator js_keys()
	{
		return null;
	}

	/**
	 * The values() method returns a new iterator object that contains the values for each element in the Map object in insertion order.
	 *
	 *@sample var values = map.values();
	 *
	 * @return the iterator that can be used in for of loops
	 */
	public Iterator js_values()
	{
		return null;
	}
}
