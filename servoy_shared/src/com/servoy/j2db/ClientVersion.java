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

import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Servoy version info class
 *
 * @author jblok
 */
@SuppressWarnings("nls")
public abstract class ClientVersion
{
	// these fields are private intentionally, to prevent final class member copy in other classes!
	private static final int majorVersion = 2025;
	private static final int middleVersion = 6;
	private static final int minorVersion = 0;
	private static final int releaseNumber = 4081;
	private static final String versionPostfix = "rc2";
	private static final boolean lts = false;
	private static String buildTime = null;

	// make sure you keep this the same format, or make it work with extensions version comparing & xml schema
	private static final String version = majorVersion + "." + middleVersion + "." + minorVersion + (versionPostfix != null ? " " + versionPostfix : "");

	public static int getReleaseNumber()
	{
		return releaseNumber;
	}

	public static String getVersion()
	{
		return version;
	}

	public static String getPureVersion()
	{
		return majorVersion + "." + middleVersion + "." + minorVersion;
	}

	public static String getBundleVersion()
	{
		return majorVersion + "." + middleVersion + "." + minorVersion + "." + releaseNumber + (lts ? "_LTS" : "");
	}

	public static String getBundleVersionWithPostFix()
	{
		if ("".equals(versionPostfix)) return getBundleVersion();
		return majorVersion + "." + middleVersion + "." + minorVersion + "." + releaseNumber + (lts ? "_LTS" : "") + "_" + versionPostfix;
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

	public static String getVersionPostFix()
	{
		return versionPostfix;
	}

	public static boolean isLts()
	{
		return lts;
	}

	/**
	 * The build date if it could get the timestamp from a .svy_timestamp or manifest file.
	 * @return
	 */
	public static String getBuildDate()
	{
		if (buildTime == null)
		{
			long time = getTime();
			if (time <= 0)
			{
				buildTime = "Unknown";
			}
			else
			{
				OffsetDateTime date = OffsetDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
				buildTime = date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm"));
			}
		}
		return buildTime;
	}

	private static long getTime()
	{
		try
		{
			URL resource = ClientVersion.class.getClassLoader().getResource("/.svy_timestamp");
			long lastModified = 0;
			if (resource == null || (lastModified = resource.openConnection().getLastModified()) <= 0)
			{
				resource = ClientVersion.class.getClassLoader().getResource("/META-INF/MANIFEST.MF");
				lastModified = resource.openConnection().getLastModified();
			}
			return lastModified;
		}
		catch (Exception e)
		{
			return -1;
		}
	}
}
