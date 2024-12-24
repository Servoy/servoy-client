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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * The Set object lets you store unique values of any type, whether primitive values or object references.
 *
 * For more information see: <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set">Set (MDN)</a>.
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Set", scriptingName = "Set")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Set
{
	/**
	 * The size accessor property returns the number of elements in a Set object.
	 *
	 * @sample set.size;
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
	 * The set() method adds or updates an entry in a Set object with a specified key and a value.
	 *
	 * @sample set.add(value);
	 *
	 * @param value
	 *
	 * @return the Set itself
	 *
	 */
	public Set js_add(Object value)
	{
		return null;
	}

	/**
	 * The clear() method removes all elements from a Set object.
	 *
	 * @sample set.clear();
	 *
	 */
	public void js_clear()
	{
	}

	/**
	 * The delete() method removes the specified element from a Set object by key.
	 *
	 * @sample var success = set.delete(key);
	 *
	 * @param value
	 *
	 * @return {boolean} True if the specified element was successfully removed from the Set; false otherwise.
	 */
	@JSFunction
	public boolean delete(Object value)
	{
		return true;
	}

	/**
	 * The entries() method returns a new iterator object that contains the [key, value] pairs for each element in the Set object in insertion order. In this particular case, this iterator object is also an iterable, so the for-of loop can be used..
	 *
	 * @sample for(var entry of set.entries()) {}
	 *
	 * @return the iterator that can be used in for of loops
	 */
	public Iterator js_entries()
	{
		return null;
	}

	/**
	 * The forEach() method executes a provided function once for each value in the Set object, in insertion order.
	 *
	 * @sample set.forEach(function(keyValuePair) {});
	 *
	 * @param callback
	 * @param thisArgument
	 *
	 */
	public void js_forEach(Function callback, Object thisArgument)
	{
		return;
	}

	/**
	 * The has() method returns a boolean indicating whether an element with the specified value exists in a Set object or not.
	 *
	 * @sample var containsKey = set.has(key);
	 *
	 * @param key
	 *
	 * @return {boolean} True if the Set contains an element with the specified key; false otherwise.
	 *
	 */
	public boolean js_has(Object key)
	{
		return true;
	}

	/**
	 * The keys() method returns a new iterator object that contains the keys for each element in the Set object in insertion order. In this particular case, this iterator object is also an iterable, so a for...of loop can be used.
	 *
	 *  @sample var values = set.keys();
	 *
	 * @return the iterator that can be used in for of loops
	 */
	public Iterator js_keys()
	{
		return null;
	}

	/**
	 * The values() method returns a new iterator object that contains the values for each element in the Set object in insertion order.
	 *
	 *@sample var values = set.values();
	 *
	 * @return the iterator that can be used in for of loops
	 */
	public Iterator js_values()
	{
		return null;
	}
}
