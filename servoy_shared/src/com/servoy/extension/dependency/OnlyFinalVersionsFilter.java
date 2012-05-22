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

package com.servoy.extension.dependency;

import java.util.Iterator;
import java.util.List;

/**
 * Filters out dependency paths that have versions containing letters in them.
 * @author acostescu
 */
public class OnlyFinalVersionsFilter implements IDependencyPathFilter
{

	public void filterResolvePaths(List<DependencyPath> allResolvedPaths)
	{
		Iterator<DependencyPath> it = allResolvedPaths.iterator();
		while (it.hasNext())
		{
			ExtensionNode[] resolvePath = it.next().extensionPath;
			for (int i = resolvePath.length - 1; i > 0; i--) // ignore first node
			{
				if (containsLetters(resolvePath[i].version))
				{
					it.remove();
					break;
				}
			}
		}
	}

	protected boolean containsLetters(String version)
	{
		boolean containsLetters = false;
		for (int i = version.length() - 1; !containsLetters && (i >= 0); i--)
		{
			if (Character.isLetter(version.charAt(i)))
			{
				containsLetters = true;
			}
		}
		return containsLetters;
	}

	public String getFilterMessage()
	{
		return "Alpha/beta/intermediate (non-final) versions are not allowed by advanced dependency resolve options."; //$NON-NLS-1$
	}

}