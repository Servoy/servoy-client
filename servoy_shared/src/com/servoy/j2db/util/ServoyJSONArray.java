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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

public class ServoyJSONArray extends JSONArray implements Serializable
{
	public ServoyJSONArray()
	{
		super();
	}

	public ServoyJSONArray(Collection< ? > collection)
	{
		super(collection);
	}

	public ServoyJSONArray(JSONTokener x) throws JSONException
	{
		super(x);
	}

	public ServoyJSONArray(Object array) throws JSONException
	{
		super(array);
	}

	public ServoyJSONArray(String source) throws JSONException
	{
		super(source);
	}

	@Override
	public String toString()
	{
		try
		{
			if (length() == 0) return "[]"; //$NON-NLS-1$
			return "[\n" + join(",\n") + "\n]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeObject(ServoyJSONObject.toSerializable(this));
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		ServoyJSONObject.fromSerializable(this, in.readObject());
	}

	// TODO should/can we make this non-static methods for ServoyJSONArrays only?
	/**
	 * As {@link JSONArray} doesn't have nice API for add/remove of elements this utility method will do that by creating a modified copy of the initial array that includes the inserted element.
	 */
	public static ServoyJSONArray insertAtIndexInJSONArray(JSONArray previousValue, int index, Object value)
	{
		ServoyJSONArray newValue = new ServoyJSONArray();
		try
		{
			for (int i = previousValue.length() - 1; i >= index; i--)
			{
				newValue.put(i + 1, previousValue.opt(i));
			}
			newValue.put(index, value);
			for (int i = index - 1; i >= 0; i--)
			{
				newValue.put(i, previousValue.opt(i));
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return newValue;
	}

	/**
	 * As {@link JSONArray} doesn't have nice API for add/remove of elements this utility method will do that by creating a modified copy of the initial array with the given index removed.
	 */
	public static ServoyJSONArray removeIndexFromJSONArray(JSONArray previousValue, int index)
	{
		ServoyJSONArray newValue = new ServoyJSONArray();
		try
		{
			for (int i = previousValue.length() - 1; i > index; i--)
			{
				newValue.put(i - 1, previousValue.opt(i));
			}
			for (int i = index - 1; i >= 0; i--)
			{
				newValue.put(i, previousValue.opt(i));
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return newValue;
	}

}
