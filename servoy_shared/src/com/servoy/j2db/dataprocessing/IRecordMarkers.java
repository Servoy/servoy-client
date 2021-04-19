/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.j2db.dataprocessing;

/**
 * @author jcompagner
 * @since 2020.09
 */
public interface IRecordMarkers
{

	/**
	 * Method to report a validation problem for this record.
	 *
	 * @param message The problem message
	 */
	void report(String message);

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param column The column for which this message is generated for
	 */
	void report(String message, String column);

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param dataprovider The column for which this message is generated for
	 * @param level The the log level for this problem
	 */
	void report(String message, String dataprovider, int level);

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param dataprovider The column for which this message is generated for
	 * @param level The the log level for this problem
	 * @param customObject Some user state that can be given to use later on.
	 */
	void report(String message, String dataprovider, int level, Object customObject);

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param dataprovider The column for which this message is generated for
	 * @param level The the log level for this problem
	 * @param customObject Some user state that can be given to use later on.
	 * @param messageKeyParams The params that are used for resolving the message if it was an i18n key.
	 */
	public void report(String message, String dataprovider, int level, Object customObject, Object[] messageKeyParams);

}