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
 * <p>Constants are defined to represent different application types, each assigned a numeric value. These constants enable applications to identify and handle specific client environments dynamically. The constants include:</p>
 * <ul>
 *   <li><b>HEADLESS_CLIENT</b>: Represents a headless client.</li>
 *   <li><b>MOBILE_CLIENT</b>: Represents a mobile client.</li>
 *   <li><b>NG_CLIENT</b>: Represents an NG client.</li>
 *   <li><b>RUNTIME_CLIENT</b>: Represents a runtime client.</li>
 *   <li><b>SMART_CLIENT</b>: Represents a smart client.</li>
 *   <li><b>WEB_CLIENT</b>: Represents a web client.</li>
 * </ul>
 *
 * <h2>Key Points</h2>
 * <ul>
 *   <li><b>Type</b>: All constants are of type <code>Number</code>.</li>
 *   <li><b>Usage</b>: These constants can be used in conditional logic to execute code tailored for the respective application type.</li>
 * </ul>
 *
 * <p>These constants provide a structured approach to managing diverse application environments, ensuring that applications can adapt their behavior based on the client type dynamically and efficiently.</p>
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
