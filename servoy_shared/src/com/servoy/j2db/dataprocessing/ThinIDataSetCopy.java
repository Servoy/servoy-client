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

import com.servoy.j2db.util.SafeArrayList;

public class ThinIDataSetCopy
{

	private final List<Object[]> rows;
	private final String[] columnNames;
	private final int[] columnTypes;

	public ThinIDataSetCopy(IDataSet dataSet)
	{
		rows = new SafeArrayList<Object[]>();
		for (int i = 0; i < dataSet.getRowCount(); i++)
		{
			rows.add(dataSet.getRow(i));
		}

		if (dataSet.getColumnNames() != null)
		{
			columnNames = new String[dataSet.getColumnNames().length];
			System.arraycopy(dataSet.getColumnNames(), 0, columnNames, 0, columnNames.length);
		}
		else
		{
			columnNames = null;
		}

		if (dataSet.getColumnTypes() != null)
		{
			columnTypes = new int[dataSet.getColumnTypes().length];
			System.arraycopy(dataSet.getColumnTypes(), 0, columnTypes, 0, columnTypes.length);
		}
		else
		{
			columnTypes = null;
		}
	}

	public List<Object[]> getRows()
	{
		return rows;
	}

	public String[] getColumnNames()
	{
		return columnNames;
	}

	public int[] getColumnTypes()
	{
		return columnTypes;
	}

	public BufferedDataSet toBufferedDataSet()
	{
		return new BufferedDataSet(columnNames, columnTypes, rows);
	}

}
