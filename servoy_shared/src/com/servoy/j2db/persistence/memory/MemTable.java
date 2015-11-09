/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.persistence.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnComparator;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnListener;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea
 *
 */
public class MemTable implements ITable
{


	private final String name;
	private final Map<String, Column> columns = new HashMap<String, Column>();
	//TODO rowIdentColumns* api should be refactored out because it is copied from Table
	private final List<Column> keyColumns = new ArrayList<Column>();

	// listeners
	protected transient List<IColumnListener> tableListeners;

	public MemTable(String name)
	{
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getCatalog()
	 */
	@Override
	public String getCatalog()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getSchema()
	 */
	@Override
	public String getSchema()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getTableType()
	 */
	@Override
	public int getTableType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumnType(java.lang.String)
	 */
	@Override
	public int getColumnType(String name)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getSQLName()
	 */
	@Override
	public String getSQLName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getDataSource()
	 */
	@Override
	public String getDataSource()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getServerName()
	 */
	@Override
	public String getServerName()
	{
		return MemServer.SERVER_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumnNames()
	 */
	@Override
	public String[] getColumnNames()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getDataProviderIDs()
	 */
	@Override
	public String[] getDataProviderIDs()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getRowIdentColumnNames()
	 */
	@Override
	public Iterator<String> getRowIdentColumnNames()
	{
		return new Iterator<String>()
		{
			Iterator<Column> intern = keyColumns.iterator();

			public void remove()
			{
				throw new UnsupportedOperationException("Can't remove row ident columns"); //$NON-NLS-1$
			}

			public String next()
			{
				return intern.next().getName();
			}

			public boolean hasNext()
			{
				return intern.hasNext();
			}
		};
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getExistInDB()
	 */
	@Override
	public boolean getExistInDB()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setExistInDB(boolean)
	 */
	@Override
	public void setExistInDB(boolean b)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#acquireWriteLock()
	 */
	@Override
	public void acquireWriteLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#removeIColumnListener(com.servoy.j2db.persistence.IColumnListener)
	 */
	@Override
	public void removeIColumnListener(IColumnListener columnListener)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumn(java.lang.String)
	 */
	@Override
	public Column getColumn(String columnName)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#removeColumn(com.servoy.j2db.persistence.Column)
	 */
	@Override
	public void removeColumn(Column column)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#isMarkedAsHiddenInDeveloper()
	 */
	@Override
	public boolean isMarkedAsHiddenInDeveloper()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#isMarkedAsMetaData()
	 */
	@Override
	public boolean isMarkedAsMetaData()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumns()
	 */
	@Override
	public Collection<Column> getColumns()
	{
		return columns.values();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#addIColumnListener(com.servoy.j2db.persistence.IColumnListener)
	 */
	@Override
	public void addIColumnListener(IColumnListener listener)
	{
		if (listener != null)
		{
			if (tableListeners == null) tableListeners = new ArrayList<IColumnListener>();
			if (!tableListeners.contains(listener)) tableListeners.add(listener);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getRowIdentColumnsCount()
	 */
	@Override
	public int getRowIdentColumnsCount()
	{
		return keyColumns.size();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setHiddenInDeveloperBecauseNoPk(boolean)
	 */
	@Override
	public void setHiddenInDeveloperBecauseNoPk(boolean b)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#isHiddenInDeveloperBecauseNoPk()
	 */
	@Override
	public boolean isHiddenInDeveloperBecauseNoPk()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumnCount()
	 */
	@Override
	public int getColumnCount()
	{
		return columns.size();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getRowIdentColumns()
	 */
	@Override
	public List<Column> getRowIdentColumns()
	{
		return keyColumns;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumnsSortedByName()
	 */
	@Override
	public Iterator<Column> getColumnsSortedByName()
	{
		SortedList<Column> newList = new SortedList<Column>(ColumnComparator.INSTANCE, columns.values());
		return newList.iterator();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setMarkedAsMetaData(boolean)
	 */
	@Override
	public void setMarkedAsMetaData(boolean selection)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setMarkedAsHiddenInDeveloperInternal(boolean)
	 */
	@Override
	public void setMarkedAsHiddenInDeveloperInternal(boolean selection)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#releaseWriteLock()
	 */
	@Override
	public void releaseWriteLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#updateDataproviderIDsIfNeeded()
	 */
	@Override
	public void updateDataproviderIDsIfNeeded()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getPKColumnTypeRowIdentCount()
	 */
	@Override
	public int getPKColumnTypeRowIdentCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#fireIColumnsChanged(java.util.Collection)
	 */
	@Override
	public void fireIColumnsChanged(Collection<IColumn> cols)
	{
		if (tableListeners == null || cols == null || cols.size() == 0) return;
		for (IColumnListener columnListener : tableListeners)
		{
			columnListener.iColumnsChanged(cols);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#acquireReadLock()
	 */
	@Override
	public void acquireReadLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#releaseReadLock()
	 */
	@Override
	public void releaseReadLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#removeColumn(java.lang.String)
	 */
	@Override
	public void removeColumn(String columnName)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * //TODO createNewColumn* API should be refactored out as it is copied from Table (non-Javadoc)
	 *
	 *
	 * @see com.servoy.j2db.persistence.ITable#createNewColumn(com.servoy.j2db.persistence.IValidateName, java.lang.String, int, int, int)
	 */
	@Override
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale) throws RepositoryException
	{
		validator.checkName(colname, 0, new ValidatorSearchContext(this, IRepository.COLUMNS), true);

		if (columns.containsKey(colname)) return columns.get(colname);

		Column c = new Column(this, colname, type, length, scale, false);
		columns.put(colname, c);
		fireIColumnCreated(c);
		return c;
	}

	/**
	 * //TODO refactor tableListeners out of MemTable and Table as they do the same thing
	 * @param c
	 */
	private void fireIColumnCreated(Column c)
	{
		if (tableListeners == null) return;
		for (IColumnListener columnListener : tableListeners)
		{
			columnListener.iColumnCreated(c);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#createNewColumn(com.servoy.j2db.persistence.IValidateName, java.lang.String, int, int, int, boolean, boolean)
	 */
	@Override
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull, boolean pkColumn)
		throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, type, length, scale, allowNull);
		c.setDatabasePK(pkColumn);
		return c;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#createNewColumn(com.servoy.j2db.persistence.IValidateName, java.lang.String, int, int, int, boolean)
	 */
	@Override
	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull) throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, type, length, scale);
		c.setAllowNull(allowNull);
		return c;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#createNewColumn(com.servoy.j2db.persistence.IValidateName, java.lang.String, int, int)
	 */
	@Override
	public Column createNewColumn(IValidateName nameValidator, String colname, int type, int length) throws RepositoryException
	{
		return createNewColumn(nameValidator, colname, type, length, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#columnDataProviderIDChanged(java.lang.String)
	 */
	@Override
	public void columnDataProviderIDChanged(String oldDataProviderID)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#fireIColumnChanged(com.servoy.j2db.persistence.IColumn)
	 */
	@Override
	public void fireIColumnChanged(IColumn column)
	{
		fireIColumnsChanged(Collections.singletonList(column));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#columnNameChange(com.servoy.j2db.persistence.IValidateName, java.lang.String, java.lang.String)
	 */
	@Override
	public void columnNameChange(IValidateName validator, String oldName, String newName) throws RepositoryException
	{
		if (oldName != null && newName != null && !oldName.equals(newName))
		{
			if (columns.containsKey(Utils.toEnglishLocaleLowerCase(newName)))
			{
				throw new RepositoryException("A column on table " + getName() + " with name/dataProviderID " + newName + " already exists"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			validator.checkName(newName, -1, new ValidatorSearchContext(this, IRepository.COLUMNS), true);
			Column c = columns.get(Utils.toEnglishLocaleLowerCase(oldName));
			if (c != null)
			{
				c.setSQLName(newName);
				columns.remove(Utils.toEnglishLocaleLowerCase(oldName));
				columns.put(Utils.toEnglishLocaleLowerCase(newName), c);
				fireIColumnChanged(c);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#addRowIdentColumn(com.servoy.j2db.persistence.Column)
	 */
	@Override
	public void addRowIdentColumn(Column column)
	{
		keyColumns.add(column);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#removeRowIdentColumn(com.servoy.j2db.persistence.Column)
	 */
	@Override
	public void removeRowIdentColumn(Column column)
	{
		keyColumns.remove(column);
	}

}
