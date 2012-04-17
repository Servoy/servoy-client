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

import java.util.List;

/**
 * A dependency path filter will filter out resolved dependency paths that do not match the filter's criteria.<br><br>
 * 
 * For example let's say an extension 'A' version 1 is going to be installed, which depends on 'B' minVersion 1, maxVersion UNBOUNDED. But both 'B 1.2' and 'B 1.5 beta 3' are available and 'B 1.1' is already installed.
 * In this case there are three possibilities to install 'A' version 1:
 * <ol>
 * <li>A 1 (using already installed B 1.1)</li>
 * <li>A 1 + B 1.2</li>
 * <li>A 1 + B 1.5 beta 3</li>
 * </ol>
 * A "final version only" filter would filter out the 3rd path, because it a "beta". 
 * 
 * @author acostescu
 */
public interface IDependencyPathFilter
{

	/**
	 * Filters out of the given list (removes) the resolve paths that do not match the filter's criteria.<br>
	 * (first extension in the extension path should be ignored as it's the one that the user wants to install)
	 * @param allResolvedPaths a list containing resolve paths. Each resolve path lists a number of extension versions that could be installed/replaced. It also contains info about lib conflicts/versions.
	 */
	void filterResolvePaths(List<DependencyPath> allResolvedPaths);

}
