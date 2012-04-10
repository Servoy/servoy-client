/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.servoy.j2db.ClientVersion;

/**
 * This class is able to parse and compare extension and lib version strings as defined by the Servoy extension schema.<br><br>
 * The extension version is a string that must begin with a number, optionally followed by one or more constructs like (&quot;.&quot; followed by a number)
 * or (optionally space followed by a word made out of only a-zA-Z chars followed by optionally space followed by a number).<br><br>
 * When letters are used in version strings, the letter version will be considered below the non-letter one (except for "i" which stands for intermediate
 * and is considered to be above the non letter one).<br><br>
 * For example "15.2.53 bata 1" &lt; "15.2.53 beta 1" &lt; "15.2.53 beta 5" &lt; "15.2.53" &lt; "15.2.53 i 1" &lt; "15.2.53.1".<br><br>
 *
 * Currently it's regEx is \d+((\.|\s?[A-Za-z]+\s?)\d+)*
 * @author acostescu
 */
@SuppressWarnings("nls")
public class VersionStringUtils
{

	public static final String UNBOUNDED = null;
	public static final String INTERMEDIATE = "i";

	private static final String EXCLUSIVE_POSTFIX = "*";
	private static final Pattern PATTERN = Pattern.compile("\\d+((\\.|\\s?[A-Za-z]+\\s?)\\d+)*");
	private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
	private static final Pattern LETTER_GROUP_PATTERN = Pattern.compile("(\\s?[A-Za-z]+\\s?)");
	private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

	/**
	 * Returns the version of Servoy that is now running.
	 * @return the version of Servoy that is now running.
	 */
	public static String getCurrentServoyVersion()
	{
		return ClientVersion.getVersion();
	}

	/**
	 * Verifies that a String conforms to supported version format.
	 * @return true if it's a valid version string, false if it is not.
	 */
	public static boolean checkVersionPattern(String version)
	{
		return PATTERN.matcher(version).matches();
	}

	public static void assertValidVersion(String version)
	{
		if (!checkVersionPattern(version)) throw new IllegalArgumentException("Version '" + version + "' is not a valid version string.");
	}

	public static void assertValidMinMaxVersion(String minMaxVersion)
	{
		if (UNBOUNDED != minMaxVersion)
		{
			String versionString = minMaxVersion;
			if (isExclusive(minMaxVersion))
			{
				versionString = minMaxVersion.substring(0, minMaxVersion.length() - EXCLUSIVE_POSTFIX.length());
			}
			assertValidVersion(versionString);
		}
	}

	/**
	 * Create a exclusive version string (specifies open interval boundary).<br><br>
	 * NOTE: Version string MUST already be valid as checked by {@link #checkVersionPattern(String)}.
	 * @param vesionString the version String.
	 * @return the given versionString marked as exclusive.
	 */
	public static String createExclusiveVersionString(String minMaxVersion)
	{
		if (minMaxVersion.endsWith(EXCLUSIVE_POSTFIX)) return minMaxVersion;
		return minMaxVersion + EXCLUSIVE_POSTFIX;
	}

	public static boolean isExclusive(String minMaxVersion)
	{
		if (minMaxVersion == UNBOUNDED) return false;
		return minMaxVersion.endsWith(EXCLUSIVE_POSTFIX);
	}

	/**
	 * Checks to see if the given version is part of the given version interval.<br><br>
	 * NOTE: Version strings MUST already be valid as checked by {@link #checkVersionPattern(String)} (if not exclusive).
	 * @param versionToCheck the version to check.
	 * @param minVersion minimum version. Can be an exclusive version. Can be {@link #UNBOUNDED}.
	 * @param maxVersion maximum version. Can be an exclusive version. Can be {@link #UNBOUNDED}.
	 * @return true if the version to check is within the given bounds.
	 */
	public static boolean belongsToInterval(String versionToCheck, String minVersion, String maxVersion)
	{
		int minCompareResult;
		int maxCompareResult;
		if (minVersion == UNBOUNDED) minCompareResult = 1;
		else
		{
			minCompareResult = compareVersions(versionToCheck, minVersion);
			if (minCompareResult == 0 && isExclusive(minVersion)) minCompareResult = -1;
		}
		if (maxVersion == UNBOUNDED) maxCompareResult = -1;
		else
		{
			maxCompareResult = compareVersions(versionToCheck, maxVersion);
			if (maxCompareResult == 0 && isExclusive(maxVersion)) maxCompareResult = 1;
		}

		return (minCompareResult >= 0) && (maxCompareResult <= 0);
	}

	/**
	 * Checks if two string versions are equal.<br><br>
	 * NOTE: Version strings MUST already be valid as checked by {@link #checkVersionPattern(String)}.
	 * @return true if they are the same.
	 */
	public static boolean sameVersion(String ver1, String ver2)
	{
		return compareVersions(ver1, ver2) == 0;
	}

	/**
	 * Compares two version strings.<br><br>
	 * NOTE: Version strings MUST already be valid as checked by {@link #checkVersionPattern(String)}. If an exclusive version is given it is
	 * treated as an non-exclusive one when comparing.
	 * @return < 0 if ver1 < ver2, == 0 if the two versions are the same, > 0 if ver1 > ver2.
	 */
	public static int compareVersions(String ver1, String ver2)
	{
		Matcher ver1Matcher = NUMBER_PATTERN.matcher(ver1);
		Matcher ver2Matcher = NUMBER_PATTERN.matcher(ver2);

		String compareBlock1;
		String compareBlock2;
		int result = 0;
		do
		{
			compareBlock1 = getNextBlock(ver1Matcher);
			compareBlock2 = getNextBlock(ver2Matcher);
			result = compareBlocks(compareBlock1 == null ? "0" : compareBlock1, compareBlock2 == null ? "0" : compareBlock2);
		}
		while (result == 0 && compareBlock1 != null && compareBlock2 != null);

		return result;
	}

	private static String getNextBlock(Matcher matcher)
	{
		String block = null;

		if (matcher.lookingAt())
		{
			block = matcher.group(0);
			matcher.region(matcher.end(), matcher.regionEnd());
		}
		else
		{
			matcher.usePattern(LETTER_GROUP_PATTERN);
			if (matcher.lookingAt())
			{
				block = matcher.group(0).trim();
				matcher.region(matcher.end(), matcher.regionEnd());
			}
		}

		if (block != null)
		{
			matcher.usePattern(DOT_PATTERN);
			if (matcher.lookingAt())
			{
				matcher.region(matcher.end(), matcher.regionEnd());
			}
		}
		matcher.usePattern(NUMBER_PATTERN);

		return block;
	}

	private static int compareBlocks(String compareBlock1, String compareBlock2)
	{
		Integer i1 = null, i2 = null;
		try
		{
			i1 = Integer.valueOf(Integer.parseInt(compareBlock1));
		}
		catch (NumberFormatException e)
		{
		}
		try
		{
			i2 = Integer.valueOf(Integer.parseInt(compareBlock2));
		}
		catch (NumberFormatException e)
		{
		}

		if (i1 == null && i2 != null)
		{
			return INTERMEDIATE.equalsIgnoreCase(compareBlock1) ? 1 : -1;
		}
		else if (i1 != null && i2 == null)
		{
			return INTERMEDIATE.equals(compareBlock2) ? -1 : 1;
		}
		else if (i1 == null) // && i2 == null)
		{
			// both are strings
			if (INTERMEDIATE.equalsIgnoreCase(compareBlock1) && !INTERMEDIATE.equalsIgnoreCase(compareBlock2)) return 1;
			if (!INTERMEDIATE.equalsIgnoreCase(compareBlock1) && INTERMEDIATE.equalsIgnoreCase(compareBlock2)) return -1;
			return compareBlock1.compareToIgnoreCase(compareBlock2);
		}
		else
		{
			// both are ints
			return i1.intValue() - i2.intValue();
		}
	}

}
