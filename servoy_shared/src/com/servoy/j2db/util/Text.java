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


import java.util.StringTokenizer;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;


/**
 * This class will resolve the %%i18n.keys%% by default, even without tag resolver
 * 
 * @author jblok
 */
public class Text
{
	public static String TAGCHAR = "%"; //$NON-NLS-1$

	public static String processTags(String s, ITagResolver resolver)
	{
		if (s == null)
		{
			return null;
		}

		StringBuffer retval = new StringBuffer();
		StringTokenizer tk = new StringTokenizer(s, TAGCHAR, true);
		boolean changed = false;
		while (tk.hasMoreTokens())
		{
			String token1 = getNextToken(tk);
			if (token1 != null && token1.equals(TAGCHAR))
			{
				String token2 = getNextToken(tk);
				if (token2 != null && token2.equals(TAGCHAR))
				{
					changed = true;
					String macro = getNextToken(tk);
					if (TAGCHAR.equals(macro))
					{
						do
						{
							retval.append(token1);
							token1 = token2;
							token2 = macro;
							macro = getNextToken(tk);
						}
						while (TAGCHAR.equals(macro));
					}
					String percent1 = getNextToken(tk);
					String percent2 = getNextToken(tk);
					if (macro != null && TAGCHAR.equals(percent1) && TAGCHAR.equals(percent2))
					{
						String string = null;
						String trimmed = macro.trim();
						if (trimmed.startsWith("i18n:")) //$NON-NLS-1$
						{
							IServiceProvider provider = J2DBGlobals.getServiceProvider();
							if (provider != null)
							{
								string = provider.getI18NMessageIfPrefixed(trimmed);
							}
							else
							{
								Debug.error("Error converting the i18n message '" + trimmed + "' of the tag text: '" + s + "' because the service provider wasn't set!"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
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

	private static String getNextToken(StringTokenizer tk)
	{
		if (tk.hasMoreTokens())
		{
			return tk.nextToken();
		}
		else
		{
			return null;
		}
	}
}
