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

package org.sablo.specification;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author jcompagner
 */
public class WebComponentType
{
	private final Map<String, PropertyDescription> properties = new HashMap<>(7);
	private final String name;

	/**
	 * 
	 */
	public WebComponentType(String name)
	{
		super();
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	protected void addProperty(PropertyDescription propertyDescription)
	{
		properties.put(propertyDescription.getName(), propertyDescription);
	}

	/**
	 * You are not allowed to modify this map!
	 */
	public Map<String, PropertyDescription> getProperties()
	{
		return Collections.unmodifiableMap(properties);
	}

	/**
	 * @param valuelist
	 */
	public Map<String, PropertyDescription> getProperties(PropertyType pt)
	{
		Map<String, PropertyDescription> filtered = new HashMap<>(4);
		for (PropertyDescription pd : properties.values())
		{
			if (pd.getType() == pt)
			{
				filtered.put(pd.getName(), pd);
			}
		}
		return filtered;
	}


	public Set<String> getAllPropertiesNames()
	{
		return new HashSet<String>(properties.keySet());
	}

}