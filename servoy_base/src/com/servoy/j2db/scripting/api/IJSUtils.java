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

package com.servoy.j2db.scripting.api;

import java.util.Date;

import com.servoy.j2db.scripting.annotations.ServoyMobile;

/**
 * @author jcompagner
 *
 */
@ServoyMobile
public interface IJSUtils
{
	public String dateFormat(Date date, String format);

	/**
	 * Returns true if the (related)foundset exists and has records.
	 * Another use is, to pass a record and qualified relations string to test multiple relations/foundset at once  
	 *
	 * @sample
	 * //test the orders_to_orderitems foundset 
	 * if (%%elementName%%.hasRecords(orders_to_orderitems))
	 * {
	 * 	//do work on relatedFoundSet
	 * }
	 * //test the orders_to_orderitems.orderitems_to_products foundset to be reached from the current record 
	 * //if (%%elementName%%.hasRecords(foundset.getSelectedRecord(),'orders_to_orderitems.orderitems_to_products'))
	 * //{
	 * //	//do work on deeper relatedFoundSet
	 * //}
	 *
	 * @param foundset the foundset to be tested

	 * @return true if exists 
	 */

	public boolean hasRecords(IJSFoundSet foundset);

	/**
	 * @clonedesc js_hasRecords(IFoundSetInternal)
	 * 
	 * @sampleas js_hasRecords(IFoundSetInternal)
	 * 
	 * @param record A JSRecord to test.
	 * @param relationString The relation name.
	 *
	 * @return true if the foundset/relation has records.
	 */
	public boolean hasRecords(IJSRecord record, String relationString);
}
