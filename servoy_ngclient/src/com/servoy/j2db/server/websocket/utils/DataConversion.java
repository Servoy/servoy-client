/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.websocket.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jcompagner
 *
 */
public class DataConversion
{
	private final Map<String, Object> conversions = new HashMap<>();

	private final List<String> path = new ArrayList<>(3);

	public DataConversion()
	{
	}

	public void pushNode(String name)
	{
		path.add(name);
	}

	public void popNode()
	{
		path.remove(path.size() - 1);
	}

	/**
	 * @param string
	 */
	@SuppressWarnings("unchecked")
	public void convert(String converterType)
	{
		Map<String, Object> map = conversions;
		for (int i = 0; i < path.size() - 1; i++)
		{
			Map<String, Object> nextMap = (Map<String, Object>)map.get(path.get(i));
			if (nextMap == null)
			{
				map.put(path.get(i), nextMap = new HashMap<>());
			}
			map = nextMap;
		}

		map.put(path.get(path.size() - 1), converterType);
	}

	/**
	 * @return
	 */
	public Map<String, Object> getConversions()
	{
		return conversions;
	}
}
