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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * Default implementation of the {@link IDataSet} interface.
 *
 * ContentList like result, can be serialized between server and client
 *
 * @author jblok
 */
public class BufferedDataSet implements ISerializableDataSet
{
	public static final long serialVersionUID = -6878367385657220897L;
	private List<Object[]> rows; //which contains RowData with column data
	private boolean hadMore;
	private String[] columnNames;
	private ColumnType[] columnTypes;

	public BufferedDataSet()
	{
		rows = new SafeArrayList<Object[]>(0);
	}

	/**
	 * Create a non db bind dataset
	 *
	 * @param columnNames the array with all the column names
	 * @param rows a list with Object arrays containing column data in same length/order as columnames array
	 */
	public BufferedDataSet(String[] columnNames, List<Object[]> rows)
	{
		this(columnNames, null, rows);
	}

	public BufferedDataSet(String[] columnNames, int[] columnTypes)
	{
		this(columnNames, columnTypes, new SafeArrayList<Object[]>(0));
	}

	public BufferedDataSet(String[] columnNames, int[] columnTypes, List<Object[]> rows)
	{
		this.columnNames = columnNames;
		this.rows = rows;
		hadMore = false;
		setColumnTypes(columnTypes);
	}

	/* package scope so it does not end up in javadoc */
	BufferedDataSet(String[] columnNames, ColumnType[] columnTypes, List<Object[]> rows, boolean hadMore)
	{
		this.columnNames = columnNames;
		this.columnTypes = columnTypes;
		this.rows = rows;
		this.hadMore = hadMore;
	}

	protected BufferedDataSet(IDataSet set)//make copy
	{
		this(set, null);
	}

	protected BufferedDataSet(IDataSet set, int[] columns)//make copy, with only indicated columns
	{
		if (set != null)
		{
			int rowCount = set.getRowCount();
			rows = new SafeArrayList<Object[]>(rowCount + 5);
			for (int r = 0; r < rowCount; r++)
			{
				Object[] array = set.getRow(r);
				Object[] data = null;
				if (array != null)
				{
					if (columns == null)
					{
						data = new Object[array.length];
						for (int c = 0; c < array.length; c++)
						{
							data[c] = array[c];
						}
					}
					else if (columns.length > 0)
					{
						data = new Object[columns.length];
						for (int c = 0; c < columns.length; c++)
						{
							data[c] = array[columns[c]];
						}
					}
				}
				if (data != null) rows.add(data);
			}

			hadMore = set.hadMoreRows();
		}
		else
		{
			rows = new SafeArrayList<Object[]>(0);
			hadMore = false;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public IDataSet clone()
	{
		BufferedDataSet set = new BufferedDataSet();
		set.columnNames = columnNames != null ? columnNames.clone() : null;
		set.columnTypes = columnTypes != null ? columnTypes.clone() : null;
		set.rows = rows != null ? new SafeArrayList<Object[]>(new ArrayList<Object[]>(rows)) : null;
		set.hadMore = hadMore;
		return set;
	}

	/**
	 * Get the number of rows in this dataset
	 */
	public int getRowCount()
	{
		if (rows == null)
		{
			return 0;
		}
		else
		{
			return rows.size();
		}
	}

	/**
	 * Get a specified row
	 *
	 * @param row the row to get
	 * @return the row data
	 */
	public Object[] getRow(int row)
	{
		return rows.get(row);
	}

	/*
	 * private int numberOfColumns = 0; public int getColumnCount() { return numberOfColumns; }
	 */
	public void removeRow(int index)
	{
		if (index == -1)
		{
			rows.clear();
		}
		else
		{
			rows.remove(index);
		}
	}

	public void setRow(int index, Object[] array)
	{
		rows.set(index, array);
	}

	public void addRow(Object[] array)
	{
		rows.add(array);
	}

	public void addRow(int index, Object[] array)
	{
		rows.add(index, array);
	}

	public int getColumnCount()
	{
		if (columnNames != null)
		{
			return columnNames.length;
		}

		//fallback to first row
		if (getRowCount() > 0)
		{
			return getRow(0).length;
		}

		return 0;
	}

	public boolean isColumnUnique(int column)
	{
		Set<Object> set = new HashSet<Object>();
		for (int i = 0; i < getRowCount(); i++)
		{
			Object obj = getRow(i)[column];
			if (obj != null && !set.add(obj))
			{
				return false;
			}
		}
		return true;
	}

	public boolean hadMoreRows()
	{
		return hadMore;
	}

	public void clearHadMoreRows()
	{
		hadMore = false;
	}

	public String[] getColumnNames()
	{
		if (columnNames == null)
		{
			int count = getColumnCount();
			columnNames = new String[count];
			for (int i = 0; i < columnNames.length; i++)
			{
				columnNames[i] = "column" + i; //$NON-NLS-1$
			}
		}
		return columnNames;
	}

	/*
	 * Setter for json deserialisation
	 */
	public void setColumnNames(String[] columnNames)
	{
		this.columnNames = columnNames;
	}

	public int[] getColumnTypes()
	{
		if (columnTypes == null)
		{
			return null;
		}
		int[] tps = new int[columnTypes.length];
		for (int i = 0; i < columnTypes.length; i++)
		{
			tps[i] = columnTypes[i].getSqlType();
		}
		return tps;
	}


	/**
	 * @return the column types
	 */
	/* package scope so it does not end up in javadoc */
	ColumnType[] getColumnTypeInfo()
	{
		return columnTypes == null ? null : columnTypes.clone();
	}

	/*
	 * Setter for json deserialisation
	 */
	public void setColumnTypes(int[] intTypes)
	{
		this.columnTypes = ColumnType.getColumnTypes(intTypes);
	}

	/*
	 * Getter for json serialisation
	 */
	public List<Object[]> getRows()
	{
		return new ArrayList<Object[]>(rows);
	}

	/*
	 * Setter for json deserialisation
	 */
	public void setRows(List<Object[]> rows)
	{
		this.rows = new SafeArrayList<Object[]>(rows);
	}

	public void sort(int column, boolean ascending)
	{
		sort(new ArrayComparator(column, ascending));
	}

	public void sort(Comparator<Object[]> rowComparator)
	{
		Object[][] array = this.rows.toArray(new Object[this.rows.size()][]);//strange construct due to type interference
		Arrays.sort(array, rowComparator);
		this.rows = new SafeArrayList<Object[]>(Arrays.asList(array));
	}

	public boolean addColumn(int columnIndex, String columnName, int columnType)
	{
		int size = getColumnCount();
		int index = (columnIndex == -1) ? size : columnIndex;
		if (index < 0 || index > size || Utils.stringIsEmpty(columnName))
		{
			return false;
		}

		String[] newColumns = Utils.arrayInsert(getColumnNames(), new String[] { columnName }, index, 1);
		columnNames = newColumns;

		if (size == 0 || columnTypes != null)
		{
			ColumnType[] newColumnTypes = Utils.arrayInsert(columnTypes, new ColumnType[] { ColumnType.getInstance(columnType, Integer.MAX_VALUE, 0) }, index,
				1);
			columnTypes = newColumnTypes;
		}

		updateRowsWhenColumnAdded(index);
		return true;
	}

	public void setColumnName(int columnIndex, String columnName)
	{
		if (columnIndex >= 0 && columnIndex < getColumnCount() && !Utils.stringIsEmpty(columnName))
		{
			getColumnNames()[columnIndex] = columnName;
		}
	}


	public boolean removeColumn(int columnIndex)
	{
		int size = getColumnCount();
		if (columnIndex < 0 || columnIndex >= size)
		{
			return false;
		}
		String[] oldColumns = getColumnNames();
		String[] newColumns = new String[size - 1];
		ColumnType[] oldColumnTypes = columnTypes;
		ColumnType[] newColumnTypes = oldColumnTypes == null || size == 1 ? null : new ColumnType[size - 1];
		for (int i = 0; i < columnIndex; i++)
		{
			newColumns[i] = oldColumns[i];
			if (newColumnTypes != null) newColumnTypes[i] = oldColumnTypes[i];
		}
		for (int i = columnIndex; i < size - 1; i++)
		{
			newColumns[i] = oldColumns[i + 1];
			if (newColumnTypes != null) newColumnTypes[i] = oldColumnTypes[i + 1];
		}
		columnNames = newColumns;
		columnTypes = newColumnTypes;
		updateRowsWhenColumnRemoved(columnIndex);
		return true;
	}

	private void updateRowsWhenColumnAdded(int columnIndex)
	{
		int size = getColumnCount();
		if (columnIndex > size || columnIndex < 0) return;
		int x = 0;
		for (Object[] row : rows)
		{
			Object[] currentRow = new Object[size];
			for (int i = 0; i < columnIndex; i++)
			{
				currentRow[i] = row[i];
			}
			currentRow[columnIndex] = null;
			for (int i = columnIndex + 1; i < size; i++)
			{
				currentRow[i] = row[i - 1];
			}
			rows.set(x++, currentRow);
		}
	}

	private void updateRowsWhenColumnRemoved(int columnIndex)
	{
		int size = getColumnCount();
		if (columnIndex >= size + 1 || columnIndex < 0) return; //shouldn't happen
		int x = 0;
		for (Object[] row : rows)
		{
			Object[] currentRow = new Object[size];
			for (int i = 0; i < columnIndex; i++)
			{
				currentRow[i] = row[i];
			}
			for (int i = columnIndex + 1; i < size + 1; i++)
			{
				currentRow[i - 1] = row[i];
			}
			rows.set(x++, currentRow);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("BufferedDataSet "); //$NON-NLS-1$
		if (columnNames != null && columnNames.length > 0)
		{
			sb.append('{');
			sb.append("Columnnames "); //$NON-NLS-1$
			sb.append(Arrays.toString(columnNames));
			sb.append("} "); //$NON-NLS-1$
		}
		int rowCount = getRowCount() > 100 ? 100 : getRowCount();
		for (int i = 0; i < rowCount; i++)
		{
			sb.append("\nrow_"); //$NON-NLS-1$
			sb.append(i + 1);
			sb.append('=');
			sb.append(Arrays.toString(getRow(i)));
			sb.append(' ');
		}
		return sb.toString();
	}

	private static class ArrayComparator implements Comparator<Object[]>
	{
		private final int column;
		private final boolean acending;

		ArrayComparator(int col, boolean acending)
		{
			this.column = col;
			this.acending = acending;
		}

		public int compare(Object[] o1, Object[] o2)
		{
			int retval = compareAsc(o1, o2);
			if (!acending)
			{
				retval = retval * -1;
			}
			return retval;
		}

		public int compareAsc(Object[] o1, Object[] o2)
		{
			Object value1 = o1[column];
			Object value2 = o2[column];
			if (value1 == null && value2 == null) return 0;
			if (value1 == null) return 1;
			if (value2 == null) return -1;

			if (value1 instanceof String)
			{
				return ((String)value1).compareToIgnoreCase((String)value2);
			}
			else if ((value1 instanceof Number) && (value2 instanceof Number))
			{
				Double d1 = new Double(((Number)value1).doubleValue());
				Double d2 = new Double(((Number)value2).doubleValue());
				return d1.compareTo(d2);
			}
			else if (value1 instanceof Comparable)
			{
				return ((Comparable<Object>)value1).compareTo(value2);
			}
			return 0;
		}
	}
}