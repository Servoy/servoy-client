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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class APPLICATION_TYPES implements IPrefixedConstantsObject
{
	/**
	 * Constant for application type smart_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.SMART_CLIENT)
	 * {
	 * 	//we are in smart_client
	 * }
	 */
	public static final int SMART_CLIENT = IApplication.CLIENT; //smart, rich

	/**
	 * Constant for application type headless_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.HEADLESS_CLIENT)
	 * {
	 * 	//we are in headless_client
	 * }
	 */
	public static final int HEADLESS_CLIENT = IApplication.HEADLESS_CLIENT;

	/**
	 * Constant for application type web_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.WEB_CLIENT)
	 * {
	 * 	//we are in web_client
	 * }
	 */
	public static final int WEB_CLIENT = IApplication.WEB_CLIENT;

	/**
	 * Constant for application type runtime_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.RUNTIME_CLIENT)
	 * {
	 * 	//we are in runtime_client
	 * }
	 */
	public static final int RUNTIME_CLIENT = IApplication.RUNTIME;

	public String getPrefix()
	{
		return "APPLICATION_TYPES";
	}

	@Override
	public String toString()
	{
		return "Names of the application types";
	}
}
