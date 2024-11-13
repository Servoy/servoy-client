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
package com.servoy.j2db.scripting.info;

import com.servoy.base.persistence.constants.IBaseApplicationTypes;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * Application Types Overview
 *
 * Servoy applications can run in different environments, each tailored to specific use cases:
 *
 * <ul>
 *   <li><b><a href="[SmartClientLink]">Smart Client</a></b>: A desktop-like environment with rich functionality.</li>
 *   <li><b><a href="[HeadlessClientLink]">Headless Client</a></b>: A backend-only environment for automation and services.</li>
 *   <li><b><a href="[WebClientLink]">Web Client</a></b>: A browser-based environment, no installation needed.</li>
 *   <li><b><a href="[RuntimeClientLink]">Runtime Client</a></b>: A lightweight, standalone execution environment.</li>
 *   <li><b><a href="[MobileClientLink]">Mobile Client</a></b>: Optimized for mobile devices and touch input.</li>
 *   <li><b><a href="[NGClientLink]">NG Client</a></b>: A modern web environment with responsive UI/UX.</li>
 * </ul>
 *
 * These application types allow developers to design specific solutions that fully leverage the features of each runtime environment.
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class APPLICATION_TYPES implements IPrefixedConstantsObject, IBaseApplicationTypes
{
	@Override
	public String toString()
	{
		return "Names of the application types";
	}
}
