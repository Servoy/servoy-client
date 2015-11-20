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
package com.servoy.j2db.util;

import java.util.AbstractList;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * List interface wrapper on JSONObject.
 *
 * @author rgansevles
 *
 */
public class JSONWrapperList extends AbstractList<Object>
{
	private final JSONArray json;

	public JSONWrapperList(JSONArray json)
	{
		this.json = json;
	}

	protected Object toJava(Object o)
	{
		return ServoyJSONObject.toJava(o);
	}

	@Override
	public Object get(int index)
	{
		return toJava(json.opt(index));
	}

	@Override
	public int size()
	{
		return json.length();
	}

	@Override
	public void add(int index, Object element)
	{
		set(index, element);
	}

	@Override
	public Object set(int index, Object element)
	{
		try
		{
			return json.put(index, element);
		}
		catch (JSONException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString()
	{
		return json.toString();
	}
}
