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

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

public class ServoyJSONArray extends JSONArray
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


}
