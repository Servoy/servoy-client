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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An JSONObject that keeps it's keys stable according to how they initially appear in the string.<br/>
 * It is useful when modifying JSON text so that the order of keys in it remains stable.<br/><br/>
 *
 * New keys can be inserted at any position in the stable key set using {@link #setKeyPositionBefore(String, String)} or {@link #setKeyPositionAfter(String, String)}.
 *
 * @author acostescu
 */
public class StableKeysJSONObject extends SortedJSONObject
{

	protected Map<String, Integer> keyIndexes; // for each key it keeps a 'compare' index that can be used to keep each key at it's desired position withing the keyset iterator.
	protected int currentIdx; // keeps track of initially added key indexes (only used during constructor execution)

	public StableKeysJSONObject(SortedJSONTokener x) throws JSONException
	{
		super(x);
		if (keyIndexes == null) keyIndexes = new HashMap<>();
		currentIdx = -1;
		setKeysComparator(new StableKeyComparator());
	}

	public StableKeysJSONObject(String source) throws JSONException
	{
		this(new StableKeysJSONTokener(source));
	}

	@Override
	public JSONObject putOnce(String key, Object value) throws JSONException
	{
		if (keyIndexes == null)
		{
			keyIndexes = new HashMap<>();
			currentIdx = 0;
		}

		if (currentIdx >= 0) keyIndexes.put(key, Integer.valueOf(currentIdx++)); // we are still in the constructor; initial values are still being populated
		return super.putOnce(key, value);
	}

	public void setKeyPositionBefore(String keyToPosition, String relativeToKey)
	{
		if (keyIndexes.size() == 0) keyIndexes.put(keyToPosition, Integer.valueOf(0));

		Integer idxOfRelativeKey = keyIndexes.get(relativeToKey);
		if (idxOfRelativeKey != null)
		{
			insertKeyAtPosition(keyToPosition, idxOfRelativeKey.intValue());
		}
		resetKeysetSortCache();
	}

	public void setKeyPositionAfter(String keyToPosition, String relativeToKey)
	{
		if (keyIndexes.size() == 0) keyIndexes.put(keyToPosition, Integer.valueOf(0));

		Integer idxOfRelativeKey = keyIndexes.get(relativeToKey);
		if (idxOfRelativeKey != null)
		{
			insertKeyAtPosition(keyToPosition, idxOfRelativeKey.intValue() + 1);
		}
		resetKeysetSortCache();
	}

	protected void insertKeyAtPosition(String keyToPosition, int idxAtInsert)
	{
		keyIndexes.remove(keyToPosition); // just in case it's there already with old value

		for (Entry<String, Integer> e : keyIndexes.entrySet())
		{
			// shift affected indexes by 1
			if (e.getValue().intValue() >= idxAtInsert) e.setValue(Integer.valueOf(e.getValue().intValue() + 1));
		}

		keyIndexes.put(keyToPosition, Integer.valueOf(idxAtInsert));
	}

	@Override
	public Object remove(String key)
	{
		keyIndexes.remove(key);
		return super.remove(key);
	}

	/**
	 * The keys that we know where they belong are sorted accordingly; all other (unknown) keys are added to the back sorted alphabetically ignoring case.
	 */
	protected class StableKeyComparator implements Comparator<String>
	{

		@Override
		public int compare(String o1, String o2)
		{
			Integer idxOf1 = keyIndexes.get(o1);
			Integer idxOf2 = keyIndexes.get(o2);

			if (idxOf1 != null && idxOf2 != null)
			{
				return idxOf1.intValue() - idxOf2.intValue();
			}
			else if (idxOf1 != null) //&& idxOf2 == null
			{
				return -1;
			}
			else if (idxOf2 != null) // && idxOf1 == null
			{
				return 1;
			}

			return StringComparator.INSTANCE.compare(o1, o2);
		}

	}

}