/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;

/**
 * Table filter definition for conditions on dataproviders.
 *
 * @author rgansevles
 *
 */
public class DataproviderTableFilterdefinition implements Serializable, TableFilterdefinition, IWriteReplace
{
	private final String dataprovider;
	private final int operator;
	private final Object value;

	public DataproviderTableFilterdefinition(String dataprovider, int operator, Object value)
	{
		this.dataprovider = dataprovider;
		this.operator = operator;
		this.value = value;
	}

	/**
	 * @return the dataprovider
	 */
	public String getDataprovider()
	{
		return dataprovider;
	}

	/**
	 * @return the operator
	 */
	public int getOperator()
	{
		return operator;
	}

	/**
	 * @return the value
	 */
	public Object getValue()
	{
		return value;
	}

	@Override
	public boolean affects(ITable table)
	{
		return table.getColumn(dataprovider) != null;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataprovider == null) ? 0 : dataprovider.hashCode());
		result = prime * result + operator;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		DataproviderTableFilterdefinition other = (DataproviderTableFilterdefinition)obj;
		if (dataprovider == null)
		{
			if (other.dataprovider != null) return false;
		}
		else if (!dataprovider.equals(other.dataprovider)) return false;
		if (operator != other.operator) return false;
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
		return new StringBuilder("DataproviderTableFilterdefinition[") //
			.append(dataprovider) //
			.append(RelationItem.getOperatorAsString(operator).toUpperCase()) //
			.append(value).append(']') //
			.toString();
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(QueryData.DATAPROCESSING_SERIALIZE_DOMAIN, getClass(), new Object[] { dataprovider, Integer.valueOf(operator), value });
	}

	public DataproviderTableFilterdefinition(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		dataprovider = (String)members[i++];
		operator = ((Integer)members[i++]).intValue();
		value = members[i++];
	}
}
