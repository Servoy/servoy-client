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

package com.servoy.j2db.debug;

/**
 * @author jcompagner
 * @since 6.0
 */
public class DataCallProfileData
{

	private final String name;
	private final String datasource;
	private final String transaction_id;
	private final long startTime;
	private final long endTime;
	private final String query;
	private final String argumentString;
	private final int count;

	/**
	 * @param string
	 * @param server_name
	 * @param transaction_id
	 * @param startTime
	 * @param endTime
	 * @param query
	 * @param argumentString
	 */
	public DataCallProfileData(String name, String datasource, String transaction_id, long startTime, long endTime, String query, String argumentString,
		int count)
	{
		this.name = name;
		this.datasource = datasource;
		this.transaction_id = transaction_id;
		this.startTime = startTime;
		this.endTime = endTime;
		this.query = query;
		this.argumentString = argumentString;
		this.count = count;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the datasource
	 */
	public String getDatasource()
	{
		return datasource;
	}

	/**
	 * @return the transaction_id
	 */
	public String getTransactionId()
	{
		return transaction_id;
	}

	public long getTime()
	{
		return endTime - startTime;
	}

	/**
	 * @return the query
	 */
	public String getQuery()
	{
		return query;
	}

	/**
	 * @return the argumentString
	 */
	public String getArgumentString()
	{
		return argumentString;
	}

	/**
	 * @return the count
	 */
	public int getCount()
	{
		return count;
	}

	public void toXML(StringBuilder sb)
	{
		sb.append("<sqlprofiledata ");
		sb.append("name=\"");
		sb.append(name);
		sb.append("\" datasource=\"");
		sb.append(datasource);
		sb.append("\" time=\"");
		sb.append(getTime());
		sb.append("\" query=\"");
		sb.append(query);
		sb.append("\" argumentString=\"");
		sb.append(argumentString);
		if (transaction_id != null)
		{
			sb.append("\" transactionid=\"");
			sb.append(transaction_id);
		}
		sb.append("\"/>");
	}
}
