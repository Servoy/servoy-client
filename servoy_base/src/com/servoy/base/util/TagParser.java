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
package com.servoy.base.util;

import java.util.ArrayList;


/**
 * This class will resolve the %%i18n.keys%% by default, even without tag resolver
 *
 * @author jblok
 */
public class TagParser
{
	public static final String TAGCHAR = "%"; //$NON-NLS-1$

	public static String processTags(String s, ITagResolver resolver, I18NProvider i18nProvider)
	{
		if (s == null)
		{
			return null;
		}
		if (s.startsWith("i18n:") && i18nProvider != null)
		{
			return i18nProvider.getI18NMessage(s.substring(5));
		}

		StringBuilder retval = new StringBuilder();
		String[] splitResult = split(s, TAGCHAR.charAt(0));
		int[] splitIdx = { 0 };
		boolean changed = false;
		while (splitIdx[0] < splitResult.length)
		{
			String token1 = getNextToken(splitResult, splitIdx);
			if (token1 != null && token1.equals(TAGCHAR))
			{
				String token2 = getNextToken(splitResult, splitIdx);
				if (token2 != null && token2.equals(TAGCHAR))
				{
					changed = true;
					String macro = getNextToken(splitResult, splitIdx);
					if (TAGCHAR.equals(macro))
					{
						do
						{
							retval.append(token1);
							token1 = token2;
							token2 = macro;
							macro = getNextToken(splitResult, splitIdx);
						}
						while (TAGCHAR.equals(macro));
					}
					String percent1 = getNextToken(splitResult, splitIdx);
					String percent2 = getNextToken(splitResult, splitIdx);
					if (macro != null && TAGCHAR.equals(percent1) && TAGCHAR.equals(percent2))
					{
						String string = null;
						String trimmed = macro.trim();
						if (trimmed.startsWith("i18n:")) //$NON-NLS-1$
						{
							if (i18nProvider != null)
							{
								string = i18nProvider.getI18NMessageIfPrefixed(trimmed);
							}
							else
							{
								string = trimmed;
							}
						}
						else if (resolver != null)
						{
							string = resolver.getStringValue(trimmed);
						}

						if (string != null)
						{
							retval.append(string);
						}
					}
					else
					{
						retval.append(token1);
						retval.append(token2);
						if (macro != null) retval.append(macro);
						if (percent1 != null) retval.append(percent1);
						if (percent2 != null) retval.append(percent2);
					}
				}
				else
				{
					retval.append(token1);
					if (token2 != null) retval.append(token2);
				}
			}
			else
			{
				retval.append(token1);
			}
		}
		return changed ? retval.toString() : s;
	}

	public static boolean staticStringUsesDataproviderAsTag(String staticString, String dataProvider)
	{
		if (staticString == null) return false;
		return staticString.contains(TAGCHAR + dataProvider + TAGCHAR);
	}

	public static String[] split(String s, char tag)
	{
		ArrayList<String> result = new ArrayList<String>();
		char currentChar;
		int startIdx = -1;

		for (int i = 0; i < s.length(); i++)
		{
			currentChar = s.charAt(i);
			if (currentChar == tag)
			{
				if (startIdx + 1 < i)
				{
					result.add(s.substring(startIdx + 1, i));
				}
				result.add(Character.toString(currentChar));
				startIdx = i;
			}
		}

		if (startIdx + 1 < s.length()) result.add(s.substring(startIdx + 1));

		return result.toArray(new String[result.size()]);
	}

	private static String getNextToken(String[] splitResult, int[] splitIdx)
	{
		if (splitIdx[0] < splitResult.length)
		{
			String nextToken = splitResult[splitIdx[0]];
			splitIdx[0]++;
			return nextToken;
		}
		else
		{
			return null;
		}
	}
}
