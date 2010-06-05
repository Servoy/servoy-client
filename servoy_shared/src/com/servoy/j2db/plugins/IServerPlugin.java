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
package com.servoy.j2db.plugins;


import java.util.Map;

/**
 * @author Jan Blok
 */
public interface IServerPlugin extends IPlugin
{
	/**
	 * Called on application startup after application started.
	 */
	public void initialize(IServerAccess app) throws PluginException;

	/**
	 * Provide the properties that can be used to configure this plugin.
	 * Key must be the property name (prefix with plugin name to
	 * prevent collisions), the value should be some description/information about this property
	 * 
	 * @return the properties
	 */
	public Map<String, String> getRequiredPropertyNames();
}
