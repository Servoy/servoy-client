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
package com.servoy.j2db.dataprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * @author jcompagner
 * 
 */
public class QueryData implements Serializable, IVisitable
{
	private String server_name;
	private final String transaction_id;
	private final ISQLSelect sqlSelect;
	private final ArrayList filters;
	private final boolean distinctInMemory;
	private final int startRow;
	private final int rowsToRetrieve;
	private final int type;

	public static String DATAPROCESSING_SERIALIZE_DOMAIN = "D"; //$NON-NLS-1$

	static
	{
		Map classMapping = new HashMap();

		classMapping.put("com.servoy.j2db.dataprocessing.QueryData", new Short((short)1)); //$NON-NLS-1$
		classMapping.put("com.servoy.j2db.dataprocessing.TableFilter", new Short((short)2)); //$NON-NLS-1$

		ReplacedObject.installClassMapping(DATAPROCESSING_SERIALIZE_DOMAIN, classMapping);
	}

	public static void initialize()
	{
		// this method triggers the static code above.
	}


	/**
	 * @param clientID
	 * @param serverName
	 * @param transactionID
	 * @param sqlSelect
	 * @param b
	 * @param i
	 * @param j
	 * @param relationQuery
	 */
	public QueryData(String server_name, String transaction_id, ISQLSelect sqlSelect, ArrayList filters, boolean distinctInMemory, int startRow,
		int rowsToRetrieve, int type)
	{
		this.server_name = server_name;
		this.transaction_id = transaction_id;
		this.sqlSelect = sqlSelect;
		this.filters = filters;
		this.distinctInMemory = distinctInMemory;
		this.startRow = startRow;
		this.rowsToRetrieve = rowsToRetrieve;
		this.type = type;
	}

	/**
	 * @return the server_name
	 */
	public String getServerName()
	{
		return this.server_name;
	}

	/**
	 * set the server_name
	 */
	public void setServerName(String server_name)
	{
		this.server_name = server_name;
	}

	/**
	 * @return the transaction_id
	 */
	public String getTransactionId()
	{
		return this.transaction_id;
	}

	/**
	 * @return the sqlSelect
	 */
	public ISQLSelect getSqlSelect()
	{
		return this.sqlSelect;
	}

	/**
	 * @return the filters
	 */
	public ArrayList getFilters()
	{
		return this.filters;
	}

	/**
	 * @return the distinctInMemory
	 */
	public boolean isDistinctInMemory()
	{
		return this.distinctInMemory;
	}

	/**
	 * @return the startRow
	 */
	public int getStartRow()
	{
		return this.startRow;
	}

	/**
	 * @return the rowsToRetrieve
	 */
	public int getRowsToRetrieve()
	{
		return this.rowsToRetrieve;
	}

	/**
	 * @return the type
	 */
	public int getType()
	{
		return this.type;
	}

	/**
	 * {@link IVisitable}
	 */
	public void acceptVisitor(IVisitor visitor)
	{
		sqlSelect.acceptVisitor(visitor);
		AbstractBaseQuery.acceptVisitor(filters, visitor);
	}


	///////// serialization ////////////////

	public Object writeReplace()
	{
		return new ReplacedObject(DATAPROCESSING_SERIALIZE_DOMAIN, getClass(),
			new Object[] { server_name, transaction_id, sqlSelect, filters, new int[] { distinctInMemory ? 1 : 0, startRow, rowsToRetrieve, type } });
	}

	public QueryData(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		server_name = (String)members[i++];
		transaction_id = (String)members[i++];
		sqlSelect = (ISQLSelect)members[i++];
		filters = (ArrayList)members[i++];

		int[] numbers = (int[])members[i++];
		distinctInMemory = numbers[0] == 1;
		startRow = numbers[1];
		rowsToRetrieve = numbers[2];
		type = numbers[3];
	}

}
