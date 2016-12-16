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
import java.util.Comparator;
import java.util.List;

/**
 * Dataset interface
 *
 * <p>
 * NOTE: do not implement this interface, it can change with new Servoy versions if new functionality is needed.
 * </p>
 *
 * @author jblok
 */
public interface IDataSet extends Serializable, Cloneable
{
	/**
	 * Get the number of rows in this dataset.
	 *
	 * @return int the count
	 */
	public int getRowCount();

	/**
	 * Get a specified row.
	 *
	 * @param row the row to get
	 * @return the row data
	 */
	public Object[] getRow(int row);

	/**
	 * Remove a row from memory (not in db).
	 *
	 * @param index the index, -1 is removeAll
	 */
	public void removeRow(int index);

	/**
	 * Add a row in memory (not in db).
	 *
	 * @param index
	 * @param array
	 */
	public void setRow(int index, Object[] array);

	/**
	 * Add a row in memory (not in db).
	 *
	 * @param array
	 */
	public void addRow(Object[] array);

	/**
	 * return all the rows.
	 *
	 * @return
	 */
	public List<Object[]> getRows();

	/**
	 * Get the number of columns in this dataset.
	 *
	 * @return int the count
	 */
	public int getColumnCount();

	/**
	 * Return the names of the columns, can be null is not requested from server.
	 *
	 * @return the names
	 */
	public String[] getColumnNames();

	/**
	 * Return the types of the columns, can be null is not requested from server.
	 *
	 * @return the types
	 */
	public int[] getColumnTypes();

	/**
	 * Returns true if the query had more results but this set was limited by performQuery (rowsToRetrieve).
	 *
	 * @return boolean
	 */
	public boolean hadMoreRows();

	/**
	 * clears the moreRows boolean
	 */
	public void clearHadMoreRows();

	/**
	 * @param index
	 * @param new_record_value
	 */
	public void addRow(int index, Object[] new_record_value);

	/**
	 * @param column
	 * @param ascending
	 */
	public void sort(int column, boolean ascending);


	/**
	 * @param rowComparator
	 */
	public void sort(Comparator<Object[]> rowComparator);

	/**
	 * adds a new column to the data set
	 *
	 * @param columnIndex the index where the column should be added (index begins with 0)
	 * @param columnName the name of the added column
	 * @return True if the adding was successful and false otherwise; if the method returns false, it means no modifications to the data set were made
	 */
	public boolean addColumn(int columnIndex, String columnName, int columnType);

	/**
	 * removes the column from the specified position (first position is 0)
	 *
	 * @param columnIndex
	 * @return True only if successful; false otherwise
	 */
	public boolean removeColumn(int columnIndex);

	/**
	 * sets the column from the specified position (first position is 0)
	 *
	 * @param columnIndex
	 * @param columnName
	 */
	public void setColumnName(int columnIndex, String columnName);

	/**
	 * @return the data set clone
	 */
	public IDataSet clone();
}
