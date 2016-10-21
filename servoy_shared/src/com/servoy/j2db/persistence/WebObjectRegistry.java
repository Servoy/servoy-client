/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.persistence;

import com.servoy.j2db.util.WeakHashSet;

/**
 * This class is meant to keep track of all instances of WebObjectImpl so that (basically only in developer) when a .spec file changes
 * all the caches that might have been invalidated by the change can be cleared.
 *
 * @author acostescu
 */
public class WebObjectRegistry
{

	private static WeakHashSet<WebObjectImpl> trackedWebObjects = null;

	public static void startTracking()
	{
		trackedWebObjects = new WeakHashSet<>(64);
	}

	public static void stopTracking()
	{
		if (trackedWebObjects != null) trackedWebObjects.clear();
		trackedWebObjects = null;
	}

	public static void registerWebObject(WebObjectImpl webObject)
	{
		if (trackedWebObjects != null)
		{
			trackedWebObjects.add(webObject);
		} // else we are not tracking anything; probably running in client
	}

	public static void clearWebObjectCaches()
	{
		if (trackedWebObjects != null)
		{
			for (WebObjectImpl webObject : trackedWebObjects)
			{
				webObject.reload();
			}
		} // else we are not tracking anything; probably running in client; it's weird that this even got called
	}

}
