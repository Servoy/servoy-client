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

import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;


/**
 * Container class for table filters.
 *
 * @author rgansevles
 *
 */
public class TableFilter implements Serializable, IWriteReplace
{

	private final String name;
	private final String serverName; // RAGTEST datasource
	private final String tableName;
	private final String tableSQLName;
	private final TableFilterCondition tableFilterCondition;

	/**
	 * @param serverName
	 * @param tableName
	 * @param tableSQLName
	 * @param dataprovider
	 * @param operator
	 * @param value
	 * @param name
	 */
	public TableFilter(String name, String serverName, String tableName, String tableSQLName, String dataprovider, int operator, Object value)
	{
		// RAGTEST nog aangeroepen?
		this(name, serverName, tableName, tableSQLName, new DataproviderTableFilterCondition(dataprovider, operator, value));
	}

	public TableFilter(String name, String serverName, String tableName, String tableSQLName, TableFilterCondition tableFilterCondition)
	{
		this.name = name;
		this.serverName = serverName;
		this.tableName = tableName;
		this.tableSQLName = tableSQLName;
		this.tableFilterCondition = tableFilterCondition;
	}

	public String getServerName()
	{
		return this.serverName;
	}

	public String getTableName()
	{
		return this.tableName;
	}

	public String getTableSQLName()
	{
		return this.tableSQLName;
	}

	public String getName()
	{
		return this.name;
	}

	/**
	 * @return the tableFilterCondition
	 */
	public TableFilterCondition getTableFilterCondition()
	{
		return tableFilterCondition;
	}

	public boolean isContainedIn(Iterable<TableFilter> filters)
	{
		if (filters != null)
		{
//			for (TableFilter tf : filters)
//			{
//				// do not use filters.contains(this) here, equality on the value (possible an array) would be incorrect
//				if (tf != null && /**/Utils.stringSafeEquals(tf.getName(), getName()) && //
//					Utils.stringSafeEquals(tf.getServerName(), getServerName()) && //
//					Utils.stringSafeEquals(tf.getTableName(), getTableName()) && //
//					Utils.stringSafeEquals(tf.getTableSQLName(), getTableSQLName())
//&& //
//			RAGTEST		Utils.stringSafeEquals(tf.getDataprovider(), getDataprovider()) && //
//					tf.getOperator() == getOperator() && //
//					Utils.equalObjects(tf.getValue(), getValue()) //
//				)
//				{
//					return true;
//				}
//			}
		}

		return false;
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + ((tableFilterCondition == null) ? 0 : tableFilterCondition.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		result = prime * result + ((tableSQLName == null) ? 0 : tableSQLName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TableFilter other = (TableFilter)obj;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (serverName == null)
		{
			if (other.serverName != null) return false;
		}
		else if (!serverName.equals(other.serverName)) return false;
		if (tableFilterCondition == null)
		{
			if (other.tableFilterCondition != null) return false;
		}
		else if (!tableFilterCondition.equals(other.tableFilterCondition)) return false;
		if (tableName == null)
		{
			if (other.tableName != null) return false;
		}
		else if (!tableName.equals(other.tableName)) return false;
		if (tableSQLName == null)
		{
			if (other.tableSQLName != null) return false;
		}
		else if (!tableSQLName.equals(other.tableSQLName)) return false;
		return true;
	}


	@Override
	public String toString()
	{
		return new StringBuilder("TableFilter{" + (name == null ? "<anonymous>" : name) + "}(")//
			.append(serverName).append(',')//
			.append(tableName == null ? "<ALL>" : tableName).append(") [")//
			//	RAGTEST		.append(dataprovider)//
			//			.append(RelationItem.getOperatorAsString(operator).toUpperCase())//
			//			.append(value).append(']')//
			.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(QueryData.DATAPROCESSING_SERIALIZE_DOMAIN, getClass(),
			new Object[] { name, serverName, tableName, tableSQLName, tableFilterCondition });
	}

	public TableFilter(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		name = (String)members[i++];
		serverName = (String)members[i++];
		tableName = (String)members[i++];
		tableSQLName = (String)members[i++];
		tableFilterCondition = (TableFilterCondition)members[i++];
	}

}
