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

import java.util.List;

/**
 * @author jcompagner
 *
 */
public class RowUpdateInfo
{

	private final SQLStatement statement;
	private final List dbPKReturnValues;
	private final List aggregatesToRemove;
	private final Row row;
	private Record record;

	/**
	 * @param row 
	 * @param src 
	 * @param statement
	 * @param mustRequeryRow
	 * @param dbPKReturnValues
	 * @param aggregatesToRemove
	 */
	public RowUpdateInfo(Row row, SQLStatement statement, List dbPKReturnValues, List aggregatesToRemove)
	{
		this.row = row;
		this.statement = statement;
		this.dbPKReturnValues = dbPKReturnValues;
		this.aggregatesToRemove = aggregatesToRemove;
	}

	/**
	 * @return
	 */
	public ISQLStatement getISQLStatement()
	{
		return statement;
	}

	/**
	 * @return
	 */
	public Row getRow()
	{
		return row;
	}

	/**
	 * @return Returns the aggregatesToRemove.
	 */
	public List getAggregatesToRemove()
	{
		return this.aggregatesToRemove;
	}

	/**
	 * @return
	 */
	public List getPkReturnValues()
	{
		return dbPKReturnValues;
	}

	/**
	 * @return
	 */
	public Record getRecord()
	{
		return record;
	}

	public void setRecord(Record record)
	{
		this.record = record;
	}

	/**
	 * @return
	 */
	public FoundSet getFoundSet()
	{
		return (FoundSet)record.getParentFoundSet();
	}

	@Override
	public String toString()
	{
		return "RowUpdateInfo for row [[" + row + "]],  Pkreturnvalues: " + dbPKReturnValues + ", aggregates: " + aggregatesToRemove;
	}
}
