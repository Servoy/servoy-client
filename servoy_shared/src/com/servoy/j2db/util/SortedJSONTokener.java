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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A JSONTokener that produces child {@link SortedJSONObject} instances instead of simple {@link JSONObject} instances.
 *
 * @author acostescu
 */
public class SortedJSONTokener extends JSONTokener
{

	public SortedJSONTokener(String s)
	{
		super(s);
	}

	@Override
	public Object nextValue() throws JSONException
	{
		char c = this.nextClean();
		back();

		if (c == '{') return createSpecificJSONObject();

		return super.nextValue();
	}

	/**
	 * Should create the correct type of child {@link SortedJSONObject}s for this tokener.
	 * @return an instance of a class that extends or is SortedJSONObject.
	 */
	protected SortedJSONObject createSpecificJSONObject()
	{
		return new SortedJSONObject(this);
	}

}
