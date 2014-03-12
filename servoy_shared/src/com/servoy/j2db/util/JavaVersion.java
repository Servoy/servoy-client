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

package com.servoy.j2db.util;

import java.util.StringTokenizer;

/**
 * Utility class for reading java version nicely.
 * 
 * @author acostescu
 */
public class JavaVersion
{

	/**
	 * The version of this (currently running) JVM.
	 */
	public final static JavaVersion CURRENT_JAVA_VERSION = new JavaVersion(System.getProperty("java.version")); //$NON-NLS-1$

	public final int major;
	public final int update;

	/**
	 * @param versionString something similar to 1.7.0_21, 1.6.0_45, 1.6.0_33, 1.8.0, 1.8.0-ea.
	 */
	public JavaVersion(String versionString)
	{
		StringTokenizer t = new StringTokenizer(versionString, "._-"); //$NON-NLS-1$
		int m = 0;
		int u = 0;
		if (t.hasMoreTokens())
		{
			m = Utils.getAsInteger(t.nextToken());
			if (t.hasMoreTokens())
			{
				if (m == 1) m = Utils.getAsInteger(t.nextToken()); // skip the 1, get the 6, 7 or 8
				if (t.hasMoreTokens())
				{
					u = Utils.getAsInteger(t.nextToken());
					if (u == 0 && t.hasMoreTokens()) u = Utils.getAsInteger(t.nextToken()); // skip the 0, get the update
				}
			}
		}
		if (m == 0 && u == 0)
		{
			Debug.warn(new RuntimeException("Cannot correctly parse Java Version string: '" + versionString + "'. Assuming new version.")); //$NON-NLS-1$ //$NON-NLS-2$
			m = 10000; // probably a new unsupported version string... report a big version number
		}

		major = m;
		update = u;
	}

	@Override
	public String toString()
	{
		return "JavaVersion: major - " + major + ", update - " + update; //$NON-NLS-1$//$NON-NLS-2$
	}

}
