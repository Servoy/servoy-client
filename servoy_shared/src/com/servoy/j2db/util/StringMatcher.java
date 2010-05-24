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

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for measuring the degree of similarity between strings.
 * Pairs of strings can be matched, the similarity between them being 
 * returned in different ways. The underlying matching method that is used
 * is the longest common subsequence.
 * 
 * @see http://en.wikipedia.org/wiki/Longest_common_subsequence_problem
 * 
 * @author gerzse
 */
public class StringMatcher
{
	private static Map<String, Map<String, Integer>> cache = new HashMap<String, Map<String, Integer>>();

	private static final int MAX_LEN = 200;
	private static int[][] bestLen = new int[MAX_LEN + 1][MAX_LEN + 1];


	/**
	 * Checks two strings and returns true if the shortest string appears as subsequence
	 * of the longest string.
	 * 
	 * @param a The first string.
	 * @param b The second string.
	 * 
	 * @return true if the shortest string appears as subsequence in the longest string,
	 * 		false otherwise.
	 */
	public static boolean stringMatch(String a, String b)
	{
		if (a == null || b == null || a.length() == 0 || b.length() == 0) return false;
		int minLen = Math.min(a.length(), b.length());
		int matchedLen = longestCommonSubstring(a, b);
		return minLen == matchedLen;
	}

	/**
	 * Returns a proportion between 0 and 1 showing how much two strings match
	 * each other. 1 means that the strings are identical. 0 means that the strings 
	 * don't have even one letter in common.
	 * 
	 * @param a The first string.
	 * @param b The second string.
	 * 
	 * @return A double value between 0 and 1 representing the degree in which the two strings match.
	 */
	public static double stringMatchProportion(String a, String b)
	{
		if (a == null || b == null || a.length() == 0 || b.length() == 0) return 0;
		double matchedLen = longestCommonSubstring(a, b);
		return 0.5 * (matchedLen / a.length() + matchedLen / b.length());
	}

	/**
	 * Returns the length of the longest common subsequence of two strings.
	 * 
	 * @param a The first string.
	 * @param b The second string.
	 * 
	 * @return An int representing the length of the longest common subsequence.
	 */
	public static int stringMatchInLength(String a, String b)
	{
		return longestCommonSubstring(a, b);
	}

	private static int longestCommonSubstring(String a, String b)
	{
		int i, j;
		if (a == null || b == null || a.length() == 0 || b.length() == 0) return 0;

		// If strings too long, just an equality check, sorry.
		if (a.length() > MAX_LEN || b.length() > MAX_LEN) return a.equals(b) ? a.length() : 0;

		int result = 0;

		int cmp = a.compareTo(b);
		String first = cmp < 0 ? a : b;
		String second = cmp < 0 ? b : a;
		Map<String, Integer> matchesOfFirst = null;
		if (cache.containsKey(first))
		{
			matchesOfFirst = cache.get(first);
			if (matchesOfFirst.containsKey(second))
			{
				result = matchesOfFirst.get(second).intValue();
				return result;
			}
		}

		if (a.equals(b))
		{
			result = a.length();
		}
		else
		{
			int n = a.length();
			int m = b.length();
			for (i = 0; i <= n; i++)
				bestLen[i][m] = 0;
			for (j = 0; j < m; j++)
				bestLen[n][j] = 0;
			for (i = n - 1; i >= 0; i--)
			{
				for (j = m - 1; j >= 0; j--)
				{
					if (a.charAt(i) == b.charAt(j)) bestLen[i][j] = bestLen[i + 1][j + 1] + 1;
					else bestLen[i][j] = Math.max(bestLen[i + 1][j], bestLen[i][j + 1]);
				}
			}
			result = bestLen[0][0];
		}

		if (matchesOfFirst == null)
		{
			matchesOfFirst = new HashMap<String, Integer>();
			cache.put(first, matchesOfFirst);
		}
		matchesOfFirst.put(second, new Integer(result));

		return result;
	}
}
