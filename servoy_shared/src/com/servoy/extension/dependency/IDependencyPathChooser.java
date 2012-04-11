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
 * If a dependency was resolved in multiple ways, this chooser will pick one of them.<br><br>
 * 
 * For example let's say an extension 'A' version 1 is going to be installed, which depends on 'B' minVersion 1, maxVersion UNBOUNDED. But both 'B 1.2' and 'B 1.5 beta 3' are available and 'B 1.1' is already installed.
 * In this case there are three possibilities to install 'A' version 1:
 * <ol>
 * <li>A 1 (using already installed B 1.1)</li>
 * <li>A 1 + B 1.2</li>
 * <li>A 1 + B 1.5 beta 3</li>
 * </ol>
 * This chooser will be able to choose one of the two (for example choose higher version over the lower one, or choose non-literal version, or choose the already installed version).
 * 
 * @author acostescu
 */
public interface IDependencyPathChooser
{

	/**
	 * Picks one of the valid dependency resolve paths.
	 * @param allResolvedPaths a list containing resolve paths. Each resolve path lists a number of extension version that could be installed/replaced.
	 * @return one of the elements in the list which is closest to what this chooser desires, or null if none close enough is found.
	 */
	ExtensionNode[] pickResolvePath(List<ExtensionNode[]> allResolvedPaths);

}
