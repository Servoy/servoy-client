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
package com.servoy.j2db.persistence;


import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Identifies a SQL server table obj.
 *
 * @author jblok
 */
public interface ITable
{
	public static final int UNKNOWN = -1;
	public static final int TABLE = 0;
	public static final int VIEW = 1;
	public static final int ALIAS = 2; // not supported yet, only for completeness

	public String getName();

	public String getCatalog();

	public String getSchema();

	public int getTableType();

	public int getColumnType(String name);

	public String getSQLName();

	public String getDataSource();

	public String getServerName();

	public String[] getColumnNames();

	public String[] getDataProviderIDs();

	public Iterator<String> getRowIdentColumnNames();

	/**
	 * @return
	 */
	public boolean getExistInDB();

	/**
	 * @param b
	 */
	public void setExistInDB(boolean b);

	/**
	 *
	 */
	public void acquireWriteLock();

	/**
	 * @param columnListener
	 */
	public void removeIColumnListener(IItemChangeListener<IColumn> columnListener);

	/**
	 * @param columnName
	 * @return
	 */
	public Column getColumn(String columnName);

	/**
	 * @param column
	 */
	public void removeColumn(Column column);

	/**
	 * @return
	 */
	public boolean isMarkedAsHiddenInDeveloper();

	/**
	 * @return
	 */
	public boolean isMarkedAsMetaData();

	/**
	 * @return
	 */
	public Collection<Column> getColumns();

	/**
	 * @param columnListener
	 */
	public void addIColumnListener(IItemChangeListener<IColumn> columnListener);

	/**
	 * @return
	 */
	public int getRowIdentColumnsCount();

	/**
	 * @param b
	 */
	public void setHiddenInDeveloperBecauseNoPk(boolean b);

	/**
	 * @return
	 */
	public boolean isHiddenInDeveloperBecauseNoPk();

	/**
	 * @return
	 */
	public int getColumnCount();


	/**
	 * @return
	 */
	public List<Column> getRowIdentColumns();

	/**
	 * @return
	 */
	public Iterator<Column> getColumnsSortedByName();

	/**
	 * @param selection
	 */
	public void setMarkedAsMetaData(boolean selection);

	/**
	 * @param selection
	 */
	public void setMarkedAsHiddenInDeveloperInternal(boolean selection);

	/**
	 *
	 */
	public void releaseWriteLock();

	/**
	 *
	 */
	public void updateDataproviderIDsIfNeeded();

	/**
	 * @return
	 */
	public int getPKColumnTypeRowIdentCount();

	/**
	 * @param changedColumns
	 */
	public void fireIColumnsChanged(Collection<IColumn> changedColumns);

	/**
	 *
	 */
	public void acquireReadLock();

	/**
	 *
	 */
	public void releaseReadLock();

	/**
	 * @param columnName
	 */
	public void removeColumn(String columnName);

	/**
	 * @param validator
	 * @param name
	 * @param sqlType
	 * @param length
	 * @param scale
	 * @return
	 */
	public Column createNewColumn(IValidateName validator, String name, int sqlType, int length, int scale) throws RepositoryException;

	/**
	 * @param validator
	 * @param string
	 * @param integer
	 * @param i
	 * @param j
	 * @param b
	 * @param c
	 * @return
	 * @throws RepositoryException
	 */
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull, boolean pkColumn)
		throws RepositoryException;

	/**
	 * @param validator
	 * @param string
	 * @param varchar
	 * @param i
	 * @param j
	 * @param b
	 */
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull) throws RepositoryException;

	/**
	 * @param nameValidator
	 * @param colname
	 * @param text
	 * @param i
	 * @return
	 */
	public Column createNewColumn(IValidateName nameValidator, String colname, int type, int length) throws RepositoryException;

	/**
	 * @param oldDataProviderID
	 */
	public void columnDataProviderIDChanged(String oldDataProviderID);

	/**
	 * @param column
	 */
	public void fireIColumnChanged(IColumn column);

	/**
	 * @param validator
	 * @param oldSQLName
	 * @param newName
	 */
	public void columnNameChange(IValidateName validator, String oldSQLName, String newName) throws RepositoryException;

	/**
	 * @param column
	 */
	public void addRowIdentColumn(Column column);

	/**
	 * @param column
	 */
	public void removeRowIdentColumn(Column column);

	public void setInitialized(boolean initialized);

	public boolean isInitialized();

	/**
	 * @param columnSqlname
	 * @return
	 */
	public IColumn getColumnBySqlname(String columnSqlname);
}
