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

package com.servoy.j2db.server.ngclient.property;


/**
 * Property description as parsed from web component spec file.
 * 
 * @author rgansevles
 *
 */
public class PropertyDescription
{
	private final String name;
	private final PropertyType type;
	private final Object config;
	private final boolean array;
	private boolean optional = false; // currently only used in the context of an api function parameter

	public PropertyDescription(String name, PropertyType type)
	{
		this(name, type, false, null);
	}

	public PropertyDescription(String name, PropertyType type, Object config)
	{
		this(name, type, false, config);
	}

	public PropertyDescription(String name, PropertyType type, boolean array, Object config)
	{
		this.name = name;
		this.type = type;
		this.array = array;
		this.config = config;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "PropertyDescription[name:" + name + ",type:" + type + ",array:" + array + ",config:" + config + "]";
	}
}
