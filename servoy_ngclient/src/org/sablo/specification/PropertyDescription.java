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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.util.Utils;


/**
 * Property description as parsed from web component spec file.
 * @author rgansevles
 */
public class PropertyDescription
{
	private final String name;
	private final PropertyType type;
	private final Object config;
	private final boolean array;
	private boolean optional = false; // currently only used in the context of an api function parameter
	private final Object defaultValue;
	private final List<Object> values;

	//case of nested type
	private Map<String, PropertyDescription> properties = null;


	public PropertyDescription(String name, PropertyType type)
	{
		this(name, type, false, null, null);
	}

	public PropertyDescription(String name, PropertyType type, Object config)
	{
		this(name, type, false, config, null);
	}

	public PropertyDescription(String name, PropertyType type, boolean array, Object config, Object defaultValue)
	{
		this(name, type, array, config, defaultValue, null);
	}

	public PropertyDescription(String name, PropertyType type, boolean array, Object config, Object defaultValue, List<Object> values)
	{
		this.name = name;
		this.type = type;
		this.array = array;
		this.config = config;
		this.defaultValue = defaultValue;
		this.values = values;
	}

	/**
	 * @param valuelist
	 */
	public Map<String, PropertyDescription> getProperties(PropertyType pt)
	{
		if (properties != null)
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
		return Collections.EMPTY_MAP;
	}


	public Set<String> getAllPropertiesNames()
	{
		if (properties != null)
		{
			return new HashSet<String>(properties.keySet());
		}
		return Collections.EMPTY_SET;
	}


	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	public PropertyType getType()
	{
		return type;
	}

	public Object getConfig()
	{
		return config;
	}

	public Object getDefaultValue()
	{
		return defaultValue;
	}

	public List<Object> getValues()
	{
		return new ArrayList<Object>(values);
	}

	/**
	 * @return
	 */
	public boolean isArray()
	{
		return array;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PropertyDescription other = (PropertyDescription)obj;
		if (config == null)
		{
			if (other.config != null) return false;
		}
		else if (!config.equals(other.config)) return false;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (type != other.type) return false;
		if (array != other.array) return false;
		if (!Utils.equalObjects(defaultValue, other.defaultValue)) return false;
		return true;
	}

	public boolean isOptional()
	{
		return optional;
	}

	public void setOptional(boolean optional)
	{
		this.optional = optional;
	}


	public void putProperty(String name, PropertyDescription type)
	{
		if (properties == null) properties = new HashMap<>();
		if (type == null) throw new RuntimeException("PropertyDescription type should not be null");
		properties.put(name, type);
	}

	public PropertyDescription getProperty(String name)
	{
		String[] split = name.split("\\.");
		if (split.length > 1)
		{
			PropertyDescription propertyDescription = properties.get(split[0]);
			PropertyDescription typeSpec = (PropertyDescription)propertyDescription.getConfig();
			return typeSpec.getProperty(split[1]);
		}

		if (properties != null)
		{
			return properties.get(name);
		}
		return null;
	}

	public Map<String, PropertyDescription> getProperties()
	{
		if (properties != null) return Collections.unmodifiableMap(properties);
		return Collections.EMPTY_MAP;
	}

	public void putAll(Map<String, PropertyDescription> map)
	{
		properties = new HashMap<>(map);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "PropertyDescription[name:" + name + ",type:" + type + ",array:" + array + ",config:" + config + ",default value:" + defaultValue + "]";
	}
}
