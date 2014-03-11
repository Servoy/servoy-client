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
	 * @param versionString something similar to 1.7.0_21, 1.6.0_45 or 1.6.0_33.
	 */
	public JavaVersion(String versionString)
	{
		StringTokenizer t = new StringTokenizer(versionString, "._-"); //$NON-NLS-1$
		t.nextToken(); // skip the 1
		major = Utils.getAsInteger(t.nextToken()); // get the 7 or 8
		if (t.hasMoreTokens())
		{
			t.nextToken(); // skip the 0
			if (t.hasMoreTokens()) update = Utils.getAsInteger(t.nextToken()); // get the update
			else update = 0;
		}
		else
		{
			update = 0;
		}
	}
}
