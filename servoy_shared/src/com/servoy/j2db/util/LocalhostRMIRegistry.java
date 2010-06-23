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
package com.servoy.j2db.util;


import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is create to overcome localhost classloading trouble (only for use in developer).
 * When a plugin is loaded in the developer/runtime (via plugin classloader) and does Naming.lookup it cannot find the stub,
 * if that is located in the same jar. (this never happens in the client because of the plugins are loaded in the same classloader (system))
 * 
 * @author jblok
 */
public class LocalhostRMIRegistry
{
	private static Map<String, Remote> serverObjects;

	public static void registerService(String name, Remote obj)
	{
		if (serverObjects == null) serverObjects = new HashMap<String, Remote>(5);
		serverObjects.put(name, obj);
	}

	public static Remote getService(String name)
	{
		if (serverObjects == null)
		{
			return null;
		}
		return serverObjects.get(name);
	}
}
