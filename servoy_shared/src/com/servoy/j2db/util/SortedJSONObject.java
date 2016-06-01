/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An JSONObject that keeps it's keys sorted according to the given comparator.
 * It is useful when modifying JSON text so that the order of keys is clear. If no comparator is provided it will use the natural ordering of elements.
 *
 * @author acostescu
 */
public class SortedJSONObject extends JSONObject
{

	protected Comparator<String> keysComparator;
	protected TreeSet<String> cachedSorteKeySet;

	public SortedJSONObject(SortedJSONTokener x) throws JSONException
	{
		super(x);
	}

	public SortedJSONObject(String sourceJSON) throws JSONException
	{
		this(new SortedJSONTokener(sourceJSON));
	}

	// --------------------------------------------------------
	// Add cached sorting to the keySet
	// --------------------------------------------------------
	/**
	 * When toString is used, output will have the keys sorted using the given comparator.
	 * Also iterating over keys via {@link #keys()} or {@link #keySet()} will give them in the same sorted order.
	 */
	public void setKeysComparator(Comparator<String> keysComparator)
	{
		this.keysComparator = keysComparator;
		resetKeysetSortCache();
	}

	public Comparator<String> getKeysComparator()
	{
		return keysComparator;
	}

	/**
	 * Same as super except that iterating over the keys is done in the order specified by {@link #setKeysComparator(Comparator)} comparator.
	 */
	@Override
	public Set<String> keySet()
	{
		if (cachedSorteKeySet == null)
		{
			cachedSorteKeySet = new TreeSet<>(keysComparator);
			cachedSorteKeySet.addAll(super.keySet());
		}
		return cachedSorteKeySet;
	}

	/**
	 * Keys are only sorted again when needed; operations such as put/remove need to call this method to clear the cached keys when keys change.
	 */
	protected void resetKeysetSortCache()
	{
		cachedSorteKeySet = null;
	}
	// --------------------------------------------------------
	// End of sorting code
	// --------------------------------------------------------

	// --------------------------------------------------------
	// Clear key cache when a key is put/removed
	// --------------------------------------------------------
	@Override
	public JSONObject put(String key, boolean value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject put(String key, Collection< ? > value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject put(String key, double value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject put(String key, int value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject put(String key, long value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject put(String key, Map< ? , ? > value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject put(String key, Object value) throws JSONException
	{
		super.put(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject putOnce(String key, Object value) throws JSONException
	{
		super.putOnce(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public JSONObject putOpt(String key, Object value) throws JSONException
	{
		super.putOpt(key, value);
		resetKeysetSortCache();
		return this;
	}

	@Override
	public Object remove(String key)
	{
		Object r = super.remove(key);
		resetKeysetSortCache();
		return r;
	}
	// --------------------------------------------------------
	// End of code that intercepts key changes
	// --------------------------------------------------------

}
