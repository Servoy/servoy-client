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
 * Filters out dependency paths that have downgrades or downgrades/upgrades in them. 
 * @author acostescu
 */
public class DisallowVersionReplacementFilter implements IDependencyPathFilter
{

	protected final boolean onlyDisallowDowngrades;

	/**
	 * Create a new version replacement filter.
	 * @param onlyDisallowDowngrades if true, then upgrades are allowed by the filter but downgrades are not; if false both upgrades and downgrades are disallowed
	 */
	public DisallowVersionReplacementFilter(boolean onlyDisallowDowngrades)
	{
		this.onlyDisallowDowngrades = onlyDisallowDowngrades;
	}

	public void filterResolvePaths(List<DependencyPath> allResolvedPaths)
	{
		Iterator<DependencyPath> it = allResolvedPaths.iterator();
		while (it.hasNext())
		{
			ExtensionNode[] resolvePath = it.next().extensionPath;
			for (int i = resolvePath.length - 1; i > 0; i--) // ignore first node
			{
				if ((!onlyDisallowDowngrades && resolvePath[i].resolveType == ExtensionNode.UPGRADE_RESOLVE) ||
					resolvePath[i].resolveType == ExtensionNode.DOWNGRADE_RESOLVE)
				{
					it.remove();
					break;
				}
			}
		}
	}

}
