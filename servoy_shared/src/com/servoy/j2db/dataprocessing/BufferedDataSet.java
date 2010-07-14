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


import java.io.Reader;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Internalize;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * ContentList like result, can be serialized between server and client
 * 
 * @author jblok
 */
public class BufferedDataSet implements IDataSet
{
	public static final long serialVersionUID = -6878367385657220897L;
	private List<Object[]> rows; //which contains RowData with column data
	private boolean hadMore;
	private String[] columnNames;
	private int[] columnTypes;

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
		this.columnTypes = columnTypes;
		this.rows = rows;
		hadMore = false;
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


	/**
	 * Get all data in mem, to make it possible to get the Row Count, column Count, etc. Column definition
	 * 
	 * NUMBER(P <= 9) Integer
	 * 
	 * NUMBER(P <= 18) Long
	 * 
	 * NUMBER(P >= 19) BigDecimal
	 * 
	 * NUMBER(P <=16, S > 0) Double
	 * 
	 * NUMBER(P >= 17, S > 0) BigDecimal
	 */
	public BufferedDataSet(ResultSet rs, int startRow, int maxResults, boolean createMetaInfo, boolean skipBlobs, boolean unique, TimeZone clientTimeZone)
		throws SQLException
	{
		if (rs == null)
		{
			rows = new SafeArrayList<Object[]>(0);
			return;
		}
		rows = new SafeArrayList<Object[]>(50);

		ResultSetMetaData metaData = rs.getMetaData();

		int numberOfColumns = metaData.getColumnCount();
		columnTypes = new int[numberOfColumns];
		boolean[] blobSkips = new boolean[numberOfColumns];
		if (createMetaInfo)
		{
			columnNames = new String[numberOfColumns];
		}
		for (int column = 1; column <= numberOfColumns; column++)
		{
			columnTypes[column - 1] = metaData.getColumnType(column);
			blobSkips[column - 1] = false;
			if (createMetaInfo || skipBlobs)
			{
				String name = metaData.getColumnLabel(column);
				if (createMetaInfo)
				{
					columnNames[column - 1] = name;
				}
				if (name != null && name.toUpperCase().indexOf(IDataServer.BLOB_MARKER_COLUMN_ALIAS) != -1)
				{
					columnTypes[column - 1] = Types.BLOB;
					blobSkips[column - 1] = skipBlobs;
				}
			}
		}

		Set<RowData> uniqueRows = null;
		if (unique)
		{
			uniqueRows = new HashSet<RowData>(50);
		}

		// Get all rows.
		int count = 0;
		while (true)
		{
			hadMore = rs.next();
			if (!hadMore)
			{
				break;
			}

			// when unique flag is set, we have to check more records to see if there are more (new) results
			if (!unique)
			{
				if (maxResults >= 0 && count >= maxResults)
				{
					break;
				}

				// if unique flag is set we may have to skip the row without incrementing count
				if (count < startRow)
				{
					count++;
					continue;
				}
			}

			Object[] newRow = new Object[numberOfColumns];
			for (int i = 1; i <= numberOfColumns; i++)
			{
				try
				{
					switch (columnTypes[i - 1])
					{
						case Types.TINYINT :
						case Types.SMALLINT :
						case Types.INTEGER :
							newRow[i - 1] = Integer.valueOf(rs.getInt(i));
							break;
						case Types.NUMERIC :
						case Types.DECIMAL :
							int precision = metaData.getPrecision(i);
							int scale = metaData.getScale(i);
							if (scale == 0 && precision == 0)
							{
								// We don't know what is returned now. Just get it as a double
								newRow[i - 1] = new Double(rs.getDouble(i));
								break;
							}
							else if (scale == 0)
							{
								if (precision <= 9)
								{
									newRow[i - 1] = Integer.valueOf(rs.getInt(i));
									break;
								}
								else if (precision <= 18)
								{
									newRow[i - 1] = Long.valueOf(rs.getLong(i));
									break;
								}
								else
								// if (precision >= 19)
								{
									newRow[i - 1] = rs.getBigDecimal(i);
									break;
								}
							}
							else
							{
								if (precision <= 16)
								{
									newRow[i - 1] = new Double(rs.getDouble(i));
									break;
								}
								else
								// if (precision >= 17)
								{
									newRow[i - 1] = rs.getBigDecimal(i);
									break;
								}
							}
						case Types.FLOAT :
						case Types.DOUBLE :
							newRow[i - 1] = new Double(rs.getDouble(i));
							break;
						case Types.REAL :
							newRow[i - 1] = new Double(rs.getFloat(i));
							//cant do getDouble on mssql driver
							break;
						case Types.BIGINT :
							newRow[i - 1] = Long.valueOf(rs.getLong(i));
							break;
						case Types.DATE :
							java.util.Date d1 = rs.getTimestamp(i);
							//wrap again some drivers return not serial subclasses
							if (d1 != null) newRow[i - 1] = new Date(convertToClientTimeZone(d1.getTime(), clientTimeZone));
							break;
						case Types.TIME :
							java.util.Date d2 = rs.getTimestamp(i);
							//wrap again some drivers return not serial subclasses
							if (d2 != null) newRow[i - 1] = new Time(convertToClientTimeZone(d2.getTime(), clientTimeZone));
							break;
						case Types.TIMESTAMP :
						case 11 ://date?? fix for 'odbc-bridge' and 'inet driver'
							java.util.Date d3 = rs.getTimestamp(i);

							//wrap again some drivers return not serial subclasses
							if (d3 != null) newRow[i - 1] = new Timestamp(convertToClientTimeZone(d3.getTime(), clientTimeZone));
							break;

						case Types.BIT :
						case 16 : //is java 1.4 BOOLEAN type
							newRow[i - 1] = Integer.valueOf((Utils.getAsBoolean(rs.getString(i)) ? 1 : 0));
							break;
						case Types.CHAR :
						case Types.VARCHAR :
						case Types.LONGVARCHAR :
						case -8 ://nchar fix for 'odbc-bridge' and 'inet driver'
						case -9 ://nvarchar fix for 'odbc-bridge' and 'inet driver'
						case -10 ://ntext fix for 'odbc-bridge' and 'inet driver'
						case -11 ://UID text fix M$ driver -sql server
							newRow[i - 1] = rs.getString(i);
							break;
						case Types.CLOB :
							Clob clob = rs.getClob(i);
							if (clob != null)
							{
								StringBuffer sb = new StringBuffer();
								char[] chars = new char[2048];
								Reader reader = clob.getCharacterStream();
								int read = reader.read(chars);
								while (read != -1)
								{
									sb.append(chars, 0, read);
									read = reader.read(chars);
								}
								newRow[i - 1] = sb.toString();
							}
							break;
						case Types.VARBINARY :
						case Types.BINARY :
						case Types.LONGVARBINARY :
						case Types.BLOB :
							if (blobSkips[i - 1])
							{
								//add marker
								newRow[i - 1] = ValueFactory.createBlobMarkerValue();
							}
							else
							{
								newRow[i - 1] = rs.getBytes(i);
							}
							break;
						case Types.OTHER :
						case Types.NULL :
						default :
							newRow[i - 1] = rs.getString(i);
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}

				if (!(newRow[i - 1] instanceof ValueFactory.BlobMarkerValue) && rs.wasNull()) //wasNull
				{
					//cannot be called when value is not retrieved (which is in caseof blobmarker)
					newRow[i - 1] = null;//remove last object
				}
			}

			newRow = (Object[])Internalize.intern(newRow);

			if (uniqueRows != null && !uniqueRows.add(new RowData(newRow)))
			{
				// skip this duplicate row without incrementing count
				continue;
			}

			if (maxResults >= 0 && count >= maxResults)
			{
				// we have enough records, hadMore is now set because we have seen a new and different record
				break;
			}

			if (count++ >= startRow)
			{
				rows.add(newRow);
			}
		}

		/*
		 * if(rs.next()) { String callstack = ""; if (Debug.TRACE) { // Get the page where it was called from // it starts with _jsp. java.io.StringWriter sw =
		 * new java.io.StringWriter(); new Exception().printStackTrace(new java.io.PrintWriter(sw)); callstack = sw.toString(); Debug.trace("There is more data
		 * in the resultset than asked for..."+callstack); } }
		 */
	}

	/**
	 * @param date
	 * @param clientTimeZone
	 * @return
	 */
	public static long convertToClientTimeZone(long date, TimeZone clientTimeZone)
	{
		if (clientTimeZone == null) return date;

		TimeZone defaultTimeZone = TimeZone.getDefault();

		if (clientTimeZone.equals(defaultTimeZone)) return date;

		GregorianCalendar server = new GregorianCalendar(defaultTimeZone);
		GregorianCalendar client = new GregorianCalendar(clientTimeZone);

		server.setTimeInMillis(date);

		client.set(Calendar.YEAR, server.get(Calendar.YEAR));
		client.set(Calendar.MONTH, server.get(Calendar.MONTH));
		client.set(Calendar.DAY_OF_MONTH, server.get(Calendar.DAY_OF_MONTH));
		client.set(Calendar.HOUR_OF_DAY, server.get(Calendar.HOUR_OF_DAY));
		client.set(Calendar.MINUTE, server.get(Calendar.MINUTE));
		client.set(Calendar.SECOND, server.get(Calendar.SECOND));
		client.set(Calendar.MILLISECOND, server.get(Calendar.MILLISECOND));

		return client.getTimeInMillis();
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
		return columnTypes.clone();
	}

	/*
	 * Setter for json deserialisation
	 */
	public void setColumnTypes(int[] columnTypes)
	{
		this.columnTypes = columnTypes;
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
		Object[][] array = this.rows.toArray(new Object[this.rows.size()][]);//strange construct due to type interference 
		Arrays.sort(array, new ArrayComparator(column, ascending));
		this.rows = new SafeArrayList<Object[]>(Arrays.asList(array));
	}

	public boolean addColumn(int columnIndex, String columnName, int columnType)
	{
		int size = getColumnCount();
		if (columnIndex == -1) columnIndex = size;
		if (columnIndex < 0 || columnIndex > size || Utils.stringIsEmpty(columnName))
		{
			return false;
		}
		String[] oldColumns = getColumnNames();
		String[] newColumns = new String[size + 1];
		int[] oldColumnTypes = columnTypes;
		int[] newColumnTypes = oldColumnTypes == null && size > 0 ? null : new int[size + 1];
		for (int i = 0; i < columnIndex; i++)
		{
			newColumns[i] = oldColumns[i];
			if (newColumnTypes != null) newColumnTypes[i] = oldColumnTypes[i];
		}
		newColumns[columnIndex] = columnName;
		if (newColumnTypes != null) newColumnTypes[columnIndex] = columnType;
		for (int i = columnIndex + 1; i < size + 1; i++)
		{
			newColumns[i] = oldColumns[i - 1];
			if (newColumnTypes != null) newColumnTypes[i] = oldColumnTypes[i - 1];
		}
		columnNames = newColumns;
		columnTypes = newColumnTypes;
		updateRowsWhenColumnAdded(columnIndex);
		return true;
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
		int[] oldColumnTypes = columnTypes;
		int[] newColumnTypes = oldColumnTypes == null || size == 1 ? null : new int[size - 1];
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
		StringBuffer sb = new StringBuffer();
		sb.append("BufferedDataSet "); //$NON-NLS-1$
		if (columnNames != null && columnNames.length > 0)
		{
			sb.append('{');
			sb.append("Columnnames"); //$NON-NLS-1$
			sb.append(Arrays.toString(columnNames));
			sb.append("} "); //$NON-NLS-1$
		}
		int rowCount = getRowCount() > 100 ? 100 : getRowCount();
		for (int i = 0; i < rowCount; i++)
		{
			sb.append("\nrow_"); //$NON-NLS-1$
			sb.append(i + 1);
			sb.append(Arrays.toString(getRow(i)));
			sb.append(' ');
		}
		return sb.toString();
	}

	/**
	 * Container class for row data. Used for uniqueness check.
	 * 
	 * @author rgansevles
	 * 
	 */
	private static class RowData
	{
		Object[] data;

		public RowData(Object[] data)
		{
			this.data = data;
		}

		private static int hashCode(Object[] array)
		{
			final int PRIME = 31;
			if (array == null) return 0;
			int result = 1;
			for (Object element : array)
			{
				result = PRIME * result + (element == null ? 0 : element.hashCode());
			}
			return result;
		}

		@Override
		public int hashCode()
		{
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + RowData.hashCode(this.data);
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			final RowData other = (RowData)obj;
			if (!Arrays.equals(this.data, other.data)) return false;
			return true;
		}
	}

	private static class ArrayComparator implements Comparator<Object>
	{
		private final int column;
		private final boolean acending;

		ArrayComparator(int col, boolean acending)
		{
			this.column = col;
			this.acending = acending;
		}

		public int compare(Object o1, Object o2)
		{
			int retval = compareAsc(o1, o2);
			if (!acending)
			{
				retval = retval * -1;
			}
			return retval;
		}

		public int compareAsc(Object o1, Object o2)
		{
			Object value1 = ((Object[])o1)[column];
			Object value2 = ((Object[])o2)[column];
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