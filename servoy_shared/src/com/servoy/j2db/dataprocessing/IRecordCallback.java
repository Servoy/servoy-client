/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.mozilla.javascript.Scriptable;

/**
 * Interface used to handle a record during a foundset parse.
 *
 * @author lvostinar
 * @since 7.4
 */

public interface IRecordCallback
{
	/**
	 * Method that will be called for each record in the foundset.
	 *
	 * @param record Record to process.
	 * @param recordIndex The index of the record in foundset.
	 * @param foundset The foundset that is traversed.
	 *
	 * @return null to continue traversal, anything else to stop it
	 */
	public Object handleRecord(IRecord record, int recordIndex, Scriptable foundset);
}
