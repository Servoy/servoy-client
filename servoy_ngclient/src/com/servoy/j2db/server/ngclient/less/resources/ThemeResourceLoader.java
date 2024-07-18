/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.server.ngclient.less.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 2019.03
 */
@SuppressWarnings("nls")
public class ThemeResourceLoader
{
	public static final String CUSTOM_PROPERTIES_LESS = "custom_servoy_theme_properties.less";
	public static final String PROPERTIES_LESS = "servoy_theme_properties.less";
	public static final String THEME_LESS = "servoy_theme.less";
	public static final String VARIANTS_JSON = "variants.json";
	public static final String[] VERSIONS = new String[] { "latest", "2023.3.0", "2022.3.0", "2021.3.0", "2020.6.0", "2019.12.0", "2019.6.0", "2019.3.0", "8.4.0" };

	private static SortedMap<Version, String> themePropertyResource = new TreeMap<>();
	private static SortedMap<Version, String> themeResource = new TreeMap<>();
	static
	{
		themeResource.put(new Version("8.4.0"), "servoy_theme_8.4.0.less");
		themeResource.put(new Version("2019.3.0"), "servoy_theme_2019.3.0.less");
		themeResource.put(new Version("2019.6.0"), "servoy_theme_2019.6.0.less");
		themeResource.put(new Version("2019.12.0"), "servoy_theme_2019.12.0.less");
		themeResource.put(new Version("2020.6.0"), "servoy_theme_2020.6.0.less");
		themeResource.put(new Version("2021.3.0_ng2"), "servoy_theme_2021.3.0_ng2.less");
		themeResource.put(new Version("2022.3.0"), "servoy_theme_2022.3.0.less");
		themeResource.put(new Version("2022.3.0_ng2"), "servoy_theme_2022.3.0_ng2.less");
		themeResource.put(new Version("2023.3.0"), "servoy_theme_2023.3.0.less");
		themeResource.put(new Version("2023.3.0_ng2"), "servoy_theme_2023.3.0_ng2.less");

		themePropertyResource.put(new Version("8.4.0"), "servoy_theme_properties_8.4.0.less");
		themePropertyResource.put(new Version("2019.3.0"), "servoy_theme_properties_2019.3.0.less");
		themePropertyResource.put(new Version("2019.6.0"), "servoy_theme_properties_2019.6.0.less");
		themePropertyResource.put(new Version("2019.12.0"), "servoy_theme_properties_2019.12.0.less");
		themePropertyResource.put(new Version("2020.6.0"), "servoy_theme_properties_2020.6.0.less");
		themePropertyResource.put(new Version("2021.3.0_ng2"), "servoy_theme_properties_2021.3.0_ng2.less");
		themePropertyResource.put(new Version("2022.3.0"), "servoy_theme_properties_2022.3.0.less");
		themePropertyResource.put(new Version("2022.3.0_ng2"), "servoy_theme_properties_2022.3.0_ng2.less");
		themePropertyResource.put(new Version("2023.3.0"), "servoy_theme_properties_2023.3.0.less");
		themePropertyResource.put(new Version("2023.3.0_ng2"), "servoy_theme_properties_2023.3.0_ng2.less");
	}

	public static byte[] getDefaultSolutionLess()
	{
		return load("default_solution.less", ClientVersion.getPureVersion()).getBytes(Charset.forName("UTF-8"));
	}

	public static byte[] getVariantsFile()
	{
		return load("variants.json", ClientVersion.getPureVersion()).getBytes(Charset.forName("UTF-8"));
	}

	public static byte[] getCustomProperties()
	{
		return load("custom_servoy_theme_properties.less", getLatestNG2Version()).getBytes(Charset.forName("UTF-8"));
	}

	public static String getLatestNG2Version()
	{
		return getLatestVersion() + "_ng2";
	}

	public static String getLatestVersion()
	{
		return Arrays.stream(VERSIONS).filter(version -> !version.contains("_ng2") && !version.contains("latest")).findFirst().orElse(null);
	}

	public static String getLatestThemeProperties()
	{
		return load(themePropertyResource.get(themePropertyResource.lastKey()), ClientVersion.getPureVersion());
	}

	public static String getLatestTheme()
	{
		return load(themeResource.get(themeResource.lastKey()), ClientVersion.getPureVersion());
	}


	public static String getThemeProperties(String maxVersion)
	{
		String resource = getResource(maxVersion, themePropertyResource);

		return load(resource, maxVersion);
	}

	public static String getTheme(String maxVersion)
	{
		String resource = getResource(maxVersion, themeResource);

		return load(resource, maxVersion);
	}

	/**
	 * @param maxVersion
	 * @return
	 */
	private static String getResource(String maxVersion, SortedMap<Version, String> versionToResource)
	{
		Version version = new Version(maxVersion);
		if ("latest_ng2".equals(maxVersion))
		{
			version = new Version(getLatestNG2Version());
		}
		// first get the exact version
		String resource = versionToResource.get(version);
		if (resource == null)
		{
			Version last = null;
			for (Entry<Version, String> next : versionToResource.entrySet())
			{
				if (next.getKey().compareTo(version) < 0)
				{
					last = next.getKey();
				}
				else
				{
					if (last == null) last = next.getKey();
					break;
				}
			}
			resource = versionToResource.get(last);
		}
		return resource;
	}

	private static String load(String path, String versionString)
	{
		try (InputStream is = ThemeResourceLoader.class.getResource(path).openStream())
		{
			Charset charset = Charset.forName("UTF-8");
			String data = Utils.getTXTFileContent(is, charset);
			data = data.replace("%%servoy_version%%", versionString);
			return data;
		}
		catch (IOException e)
		{
			return "// can't load " + path + ", exception : " + e.getLocalizedMessage();
		}
	}

	private static class Version implements Comparable<Version>
	{
		private final int major;
		private final int minor;
		private final int micro;
		private final boolean ng2;

		public Version(String versionString)
		{
			String[] parts = versionString.split("\\.");
			ng2 = versionString.contains("_ng2");
			if (parts.length > 2)
			{
				major = Utils.getAsInteger(parts[0], false);
				minor = Utils.getAsInteger(parts[1], false);
				micro = Utils.getAsInteger(parts[2], false);
			}
			else if (parts.length > 1)
			{
				major = Utils.getAsInteger(parts[0], false);
				minor = Utils.getAsInteger(parts[1], false);
				micro = 0;
			}
			else if (parts.length > 0)
			{
				major = Utils.getAsInteger(parts[0], false);
				minor = 0;
				micro = 0;
			}
			else
			{
				major = 0;
				minor = 0;
				micro = 0;
			}
		}

		@Override
		public int compareTo(Version o)
		{
			if (ng2 != o.ng2) return ng2 ? -1 : 1;
			if (major > o.major) return 1;
			if (major < o.major) return -1;
			if (minor > o.minor) return 1;
			if (minor < o.minor) return -1;
			return micro - o.micro;
		}

		@Override
		public String toString()
		{
			return major + "." + minor + "." + micro + (ng2 ? "_ng2" : "");
		}
	}
}
