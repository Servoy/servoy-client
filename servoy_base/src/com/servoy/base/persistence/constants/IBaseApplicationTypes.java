/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.persistence.constants;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * @author lvostinar
 *
 */
@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseApplicationTypes
{
	/**
	 * Constant for application type smart_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.SMART_CLIENT)
	 * {
	 * 	//we are in smart_client
	 * }
	 *
	 * @deprecated smart client is removed
	 */
	@Deprecated
	public static final int SMART_CLIENT = 2; //smart, rich

	/**
	 * Constant for application type headless_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.HEADLESS_CLIENT)
	 * {
	 * 	//we are in headless_client
	 * }
	 */
	public static final int HEADLESS_CLIENT = 4;

	/**
	 * Constant for application type web_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.WEB_CLIENT)
	 * {
	 * 	//we are in web_client
	 * }
	 * @deprecated web client is removed
	 */
	@Deprecated
	public static final int WEB_CLIENT = 5;

	/**
	 * Constant for application type runtime_client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.RUNTIME_CLIENT)
	 * {
	 * 	//we are in runtime_client
	 * }
	 *
	 * @deprecated runtime client is removed
	 */
	@Deprecated
	public static final int RUNTIME_CLIENT = 6;

	/**
	 * Constant for application type mobile client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.MOBILE_CLIENT)
	 * {
	 * 	//we are in mobile client
	 * }
	 */
	public static final int MOBILE_CLIENT = 8;

	/**
	 * Constant for application type ng client.
	 *
	 * @sample
	 * if (application.getApplicationType() == APPLICATION_TYPES.NG_CLIENT)
	 * {
	 * 	//we are in mobile client
	 * }
	 */
	public static final int NG_CLIENT = 9;
}
