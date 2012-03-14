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

import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.serialize.ReplacedObject;


/**
 * Container class for table filters.
 * 
 * @author rgansevles
 * 
 */
public class TableFilter implements Serializable
{

	private final String name;
	private final String serverName;
	private final String tableName;
	private final String tableSQLName;
	private final String dataprovider;
	private final int operator;
	private final Object value;


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
		super();
		this.serverName = serverName;
		this.tableName = tableName;
		this.tableSQLName = tableSQLName;
		this.dataprovider = dataprovider;
		this.operator = operator;
		this.value = value;
		this.name = name;
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

	public String getDataprovider()
	{
		return this.dataprovider;
	}

	public int getOperator()
	{
		return this.operator;
	}

	public Object getValue()
	{
		return this.value;
	}

	public String getName()
	{
		return this.name;
	}

	public boolean isContainedIn(Iterable<TableFilter> filters)
	{
		if (filters != null)
		{
			for (TableFilter tf : filters)
			{
				// do not use filters.contains(this) here, equality on the value (possible an array) would be incorrect
				if (tf != null &&
				/**/Utils.stringSafeEquals(tf.getName(), getName()) && //
					Utils.stringSafeEquals(tf.getServerName(), getServerName()) && //
					Utils.stringSafeEquals(tf.getTableName(), getTableName()) && //
					Utils.stringSafeEquals(tf.getTableSQLName(), getTableSQLName()) && //
					Utils.stringSafeEquals(tf.getDataprovider(), getDataprovider()) && //
					tf.getOperator() == getOperator() && //
					Utils.equalObjects(tf.getValue(), getValue()) //
				)
				{
					return true;
				}
			}
		}

		return false;
	}


	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataprovider == null) ? 0 : dataprovider.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + operator;
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		result = prime * result + ((tableSQLName == null) ? 0 : tableSQLName.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final TableFilter other = (TableFilter)obj;
		if (dataprovider == null)
		{
			if (other.dataprovider != null) return false;
		}
		else if (!dataprovider.equals(other.dataprovider)) return false;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (operator != other.operator) return false;
		if (serverName == null)
		{
			if (other.serverName != null) return false;
		}
		else if (!serverName.equals(other.serverName)) return false;
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
		if (value == null)
		{
			if (other.value != null) return false;
		}
		else if (!value.equals(other.value)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("TableFilter{" + (name == null ? "<anonymous>" : name) + "}("); //$NON-NLS-1$
		sb.append(serverName).append(',');
		sb.append(tableName).append(") [");
		sb.append(dataprovider);
		sb.append(RelationItem.getOperatorAsString(operator).toUpperCase());
		sb.append(value).append(']');
		return sb.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		return new ReplacedObject(QueryData.DATAPROCESSING_SERIALIZE_DOMAIN, getClass(),
			new Object[] { name, serverName, tableName, tableSQLName, dataprovider, new Integer(operator), value });
	}

	public TableFilter(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		name = (String)members[i++];
		serverName = (String)members[i++];
		tableName = (String)members[i++];
		tableSQLName = (String)members[i++];
		dataprovider = (String)members[i++];
		operator = ((Integer)members[i++]).intValue();
		value = members[i++];
	}

}
