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
package com.servoy.j2db;

public abstract class ClientVersion
{
	//these fields are private to prevent final class member copy in other classes! 
	private static final int majorVersion = 5;
	private static final int middleVersion = 2;
	private static final int minorVersion = 0;
	private static final int releaseNumber = 990;
	private static final String versionPostfix = "b1"; //$NON-NLS-1$
	private static final String version = majorVersion + "." + middleVersion + "." + minorVersion + (versionPostfix != null ? " " + versionPostfix : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

	public static int getReleaseNumber()
	{
		return releaseNumber;
	}

	public static String getVersion()
	{
		return version;
	}

	public static String getBundleVersion()
	{
		return majorVersion + "." + middleVersion + "." + minorVersion + "." + releaseNumber;
	}

	public static int getMajorVersion()
	{
		return majorVersion;
	}

	public static int getMiddleVersion()
	{
		return middleVersion;
	}

	public static int getMinorVersion()
	{
		return minorVersion;
	}
}
