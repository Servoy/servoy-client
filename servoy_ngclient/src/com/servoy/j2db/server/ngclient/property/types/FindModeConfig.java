/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashMap;
import java.util.Set;

/**
 * @author gganea
 */
public class FindModeConfig
{
	private final HashMap<String, Object> forEntries;

	public FindModeConfig(HashMap<String, Object> forEntities)
	{
		this.forEntries = forEntities;
	}

	/**
	 * @return Returns the names of the properties that are affected by toggling findmode.
	 */
	public Set<String> configPropertiesNames()
	{
		return forEntries.keySet();

	}

	/**
	 * Tells what is the boolean value that will automatically be set on a property from {@link #configPropertiesNames()} when find mode changes to true.
	 * When find mode goes back to false, the previous value of that property will be restored.<br/><br/>
	 *
	 * TODO the returned value should always be a boolean from JSON.
	 */
	public Object getConfiguredPropertyValueOf(String propertyName)
	{
		return forEntries.get(propertyName);
	}
}
