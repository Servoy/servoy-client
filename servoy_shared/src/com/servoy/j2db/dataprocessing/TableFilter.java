/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import static com.servoy.base.util.DataSourceUtilsBase.createDBTableDataSource;
import static com.servoy.j2db.util.Utils.iterate;
import static com.servoy.j2db.util.Utils.toArray;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.dataprocessing.BroadcastFilter.BroadcastFilterOperator;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;


/**
 * Container class for table filters.
 *
 * @author rgansevles
 *
 */
public class TableFilter implements IWriteReplace
{
	private final String name;
	private final String serverName;
	private final String tableName;
	private final String tableSQLName;
	private TableFilterdefinition tableFilterdefinition;
	private final boolean broadcastFilter;

	public TableFilter(String name, String serverName, String tableName, String tableSQLName, String dataprovider, int operator, Object value)
	{
		this(name, serverName, tableName, tableSQLName, new DataproviderTableFilterdefinition(dataprovider, operator, value), false);
	}

	public TableFilter(String name, String serverName, String tableName, String tableSQLName, TableFilterdefinition tableFilterdefinition,
		boolean broadcastFilter)
	{
		this.name = name;
		this.serverName = serverName;
		this.tableName = tableName;
		this.tableSQLName = tableSQLName;
		this.tableFilterdefinition = tableFilterdefinition;
		this.broadcastFilter = broadcastFilter;
	}

	public String getServerName()
	{
		return serverName;
	}

	public String getTableName()
	{
		return tableName;
	}

	public String getDataSource()
	{
		return createDBTableDataSource(serverName, tableName);
	}

	public String getTableSQLName()
	{
		return tableSQLName;
	}

	public String getName()
	{
		return name;
	}

	/**
	 * @return the tableFilterdefinition
	 */
	public TableFilterdefinition getTableFilterdefinition()
	{
		return tableFilterdefinition;
	}

	public void setTableFilterDefinition(TableFilterdefinition tableFilterDefinition)
	{
		this.tableFilterdefinition = tableFilterDefinition;
	}

	/**
	 * @return the broadcastFilter
	 */
	public boolean isBroadcastFilter()
	{
		return broadcastFilter;
	}

	public boolean isContainedIn(Iterable<TableFilter> filters)
	{
		for (TableFilter tf : iterate(filters))
		{
			// do not use filters.contains(this) here, equality on the value (possible an array) would be incorrect
			if (tf != null && /**/Utils.stringSafeEquals(tf.getName(), getName()) && //
				Utils.stringSafeEquals(tf.getServerName(), getServerName()) && //
				Utils.stringSafeEquals(tf.getTableName(), getTableName()) && //
				Utils.stringSafeEquals(tf.getTableSQLName(), getTableSQLName()) && //
				//
				((tableFilterdefinition instanceof DataproviderTableFilterdefinition && //
					tf.getTableFilterdefinition() instanceof DataproviderTableFilterdefinition && //
					//
					Utils.stringSafeEquals(((DataproviderTableFilterdefinition)tf.getTableFilterdefinition()).getDataprovider(),
						((DataproviderTableFilterdefinition)tableFilterdefinition).getDataprovider()) && //
					((DataproviderTableFilterdefinition)tf.getTableFilterdefinition())
						.getOperator() == ((DataproviderTableFilterdefinition)tableFilterdefinition).getOperator() && //
					Utils.equalObjects(((DataproviderTableFilterdefinition)tf.getTableFilterdefinition()).getValue(),
						((DataproviderTableFilterdefinition)tableFilterdefinition).getValue())) //
					|| //
					(tableFilterdefinition instanceof QueryTableFilterdefinition && //
						tf.getTableFilterdefinition() instanceof QueryTableFilterdefinition &&
						((QueryTableFilterdefinition)tableFilterdefinition).getQuerySelect()
							.equals(((QueryTableFilterdefinition)tf.tableFilterdefinition).getQuerySelect()))))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Create a broadcast filter from this table filter.
	 *
	 * Only create it if it is a simple filter that compares values.
	 */
	public BroadcastFilter createBroadcastFilter()
	{
		if (tableFilterdefinition instanceof DataproviderTableFilterdefinition)
		{
			DataproviderTableFilterdefinition dptf = (DataproviderTableFilterdefinition)tableFilterdefinition;
			if (dptf.getOperator() == IBaseSQLCondition.EQUALS_OPERATOR || dptf.getOperator() == IBaseSQLCondition.IN_OPERATOR)
			{
				return new BroadcastFilter(tableName, dptf.getDataprovider(), BroadcastFilterOperator.IN, toArray(dptf.getValue()));
			}
		}

		// Not supported for databroadcast
		return null;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + ((tableFilterdefinition == null) ? 0 : tableFilterdefinition.hashCode());
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
		if (tableFilterdefinition == null)
		{
			if (other.tableFilterdefinition != null) return false;
		}
		else if (!tableFilterdefinition.equals(other.tableFilterdefinition)) return false;
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
			.append(tableFilterdefinition).append(']')//
			.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(QueryData.DATAPROCESSING_SERIALIZE_DOMAIN, getClass(),
			new Object[] { name, serverName, tableName, tableSQLName, tableFilterdefinition, Integer.valueOf(broadcastFilter ? 1 : 0) });
	}

	public TableFilter(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		name = (String)members[i++];
		serverName = (String)members[i++];
		tableName = (String)members[i++];
		tableSQLName = (String)members[i++];
		if (members.length - i >= 3)
		{
			String dataprovider = (String)members[i++];
			int operator = ((Integer)members[i++]).intValue();
			Object value = members[i++];
			tableFilterdefinition = new DataproviderTableFilterdefinition(dataprovider, operator, value);
		}
		else
		{
			tableFilterdefinition = (TableFilterdefinition)members[i++];
		}
		broadcastFilter = members.length - i >= 1 && ((Integer)members[i++]).intValue() == 1;
	}

}
