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

package org.sablo.websocket.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class records the conversion being done in a tree structure, 
 * by pushing and popping the current path, and calling convert for a specific node.
 * It will build up a conversions map then for the leaf nodes where convert is being called on.
 * This map contains for all tree nodes again a map and the leave nodes are holding the value that is given with the convert call (mostly the type of conversion)
 *  
 * @author jcompagner
 * @since 8.0
 */
public class DataConversion
{
	private final Map<String, Object> conversions = new HashMap<>();

	private final List<String> path = new ArrayList<>(3);

	public DataConversion()
	{
	}

	/**
	 * Push a node name to the current path
	 * 
	 * @param name
	 */
	public void pushNode(String name)
	{
		path.add(name);
	}

	/**
	 * Pops the current path
	 */
	public void popNode()
	{
		path.remove(path.size() - 1);
	}

	/**
	 * Recors the given converterType of the current tree path.
	 * 
	 * @param converterType
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
	 * @return a Map of the registered conversion which is a Map of nodeName->Map for parent nodes and the leaf node is nodeName->converterType 
	 */
	public Map<String, Object> getConversions()
	{
		return conversions;
	}
}
