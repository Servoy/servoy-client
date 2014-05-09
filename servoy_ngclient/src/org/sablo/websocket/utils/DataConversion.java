/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
