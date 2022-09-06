/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.j2db.scripting;

import java.lang.reflect.Array;
import java.util.ArrayList;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Wrapper;

/**
 * @author jcompagner
 * @since 2022.03
 */
public class ServoyNativeArray extends NativeArray
{

	private final Class< ? > componentType;

	/**
	 * @param array
	 */
	public ServoyNativeArray(Object[] array, Class< ? > componentType)
	{
		super(array);
		this.componentType = componentType;
	}


	@Override
	public Object unwrap()
	{
		Object[] ids = getIds();

		for (Object id : ids)
		{
			if (id instanceof String || (id instanceof Number && ((Number)id).intValue() < 0))
			{
				return this;
			}
		}

		ArrayList<Object> al = new ArrayList<Object>(ids.length);
		for (Object id : ids)
		{
			if (id instanceof Number)
			{
				int index = ((Number)id).intValue();
				Object o = get(index, this);
				if (o != NOT_FOUND)
				{
					o = o instanceof Wrapper ? ((Wrapper)o).unwrap() : o;
					while (al.size() <= index)
						al.add(null);
					al.set(index, o);
				}
			}
		}
		Object[] array = al.toArray((Object[])Array.newInstance(componentType, al.size()));
		return array;
	}
}
