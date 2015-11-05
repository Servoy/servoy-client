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

package com.servoy.j2db.dataprocessing;

import java.util.List;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Debug;

/**
 * Factory to create data sets using internal api.
 *
 * @author rgansevles
 *
 */
public class BufferedDataSetInternal
{

	public static BufferedDataSet createBufferedDataSet(String[] columnNames, ColumnType[] columnTypes, List<Object[]> rows, boolean hadMore)
	{
		return new BufferedDataSet(columnNames, columnTypes, rows, hadMore);
	}

	public static ColumnType[] getColumnTypeInfo(IDataSet set)
	{
		if (set instanceof BufferedDataSet)
		{
			return ((BufferedDataSet)set).getColumnTypeInfo();
		}
		if (set instanceof DataSetWithIndex)
		{
			return ((DataSetWithIndex)set).getColumnTypeInfo();
		}
		return null;
	}

	/**
	 * @param pks
	 * @param table
	 * @return
	 */
	public static IDataSet convertPksToRightType(IDataSet pks, ITable table)
	{
		if (pks == null || pks.getRowCount() == 0 || table == null)
		{
			return pks;
		}

		BufferedDataSet newPkSet = new BufferedDataSet();
		List<Column> rowIdentColumns = table.getRowIdentColumns();
		for (int r = 0; r < pks.getRowCount(); r++)
		{
			Object[] row = pks.getRow(r);
			if (row != null && row.length == rowIdentColumns.size())
			{
				Object[] newRow = new Object[row.length];
				for (int c = 0; c < row.length; c++)
				{
					newRow[c] = rowIdentColumns.get(c).getAsRightType(row[c]);
				}
				newPkSet.addRow(newRow);
			}
			else
			{
				// invalid, leave untouched
				Debug.error("Could not convert pk datatset for table " + table.getDataSource() + ", pk row size mismatch: " +
					(row == null ? "null" : String.valueOf(row.length)) + "/" + rowIdentColumns.size());
				return pks;
			}
		}

		return newPkSet;
	}


}
