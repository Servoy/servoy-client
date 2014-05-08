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

package org.sablo.specification;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author jcompagner
 *
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