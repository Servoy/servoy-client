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

package com.servoy.j2db.persistence;

import static com.servoy.base.persistence.IBaseColumn.TENANT_COLUMN;
import static com.servoy.j2db.util.Utils.toEnglishLocaleLowerCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.AliasKeyMap;
import com.servoy.j2db.util.SortedList;

/**
 * Base class for database tables and in memory tables.
 * @author emera
 */
public abstract class AbstractTable implements ITable, Serializable
{
	public static final long serialVersionUID = -1L;

	protected final List<Column> keyColumns = new ArrayList<Column>();
	protected final AliasKeyMap<String, String, Column> columns = new AliasKeyMap<String, String, Column>(new LinkedHashMap<String, Column>());
	private transient volatile boolean initialized = false;

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

	public int getRowIdentColumnsCount()
	{
		return keyColumns.size();
	}

	/**
	 * Get all key Columns
	 */
	public List<Column> getRowIdentColumns()
	{
		return keyColumns;
	}

	public void addRowIdentColumn(Column c)
	{
		if (!keyColumns.contains(c)) keyColumns.add(c);
		Collections.sort(keyColumns, NameComparator.INSTANCE);
	}

	public void removeRowIdentColumn(Column c)
	{
		keyColumns.remove(c);
	}

	protected void fireIColumnCreated(IColumn column)
	{
		ColumnChangeHandler.getInstance().fireItemCreated(this, column);
	}

	public void columnDataProviderIDChanged(String oldDataProviderID)
	{
		columns.updateAlias(oldDataProviderID);
	}

	public void fireIColumnChanged(IColumn column)
	{
		fireIColumnsChanged(Collections.singletonList(column));
	}

	public void fireIColumnsChanged(Collection<IColumn> cols)
	{
		ColumnChangeHandler.getInstance().fireItemChanged(this, cols);
	}

	@Override
	public List<Column> getTenantColumns()
	{
		return getColumns().stream() //
			.filter(column -> column.hasFlag(TENANT_COLUMN)) //
			.collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#addIColumnListener(com.servoy.j2db.persistence.IColumnListener)
	 */
	@Override
	public void addIColumnListener(IItemChangeListener<IColumn> listener)
	{
		ColumnChangeHandler.getInstance().add(this, listener);
	}

	public void removeIColumnListener(IItemChangeListener<IColumn> listener)
	{
		ColumnChangeHandler.getInstance().remove(this, listener);
	}

	public void addColumn(Column c)
	{
		columns.put(c.getName(), c);
	}

	public Collection<Column> getColumns()
	{
		return columns.values();
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
	 * @see com.servoy.j2db.persistence.ITable#getColumnsSortedByName()
	 */
	@Override
	public Iterator<Column> getColumnsSortedByName()
	{
		SortedList<Column> newList = new SortedList<Column>(ColumnComparator.getColumnsNameComparator(), getColumns());
		return newList.iterator();
	}

	/**
	 * @see com.servoy.j2db.persistence.ITable#getColumnNames()
	 */
	public String[] getColumnNames()
	{
		return columns.keySet().toArray(new String[columns.keySet().size()]);
	}

	public Column getColumn(String colname, boolean ignoreCase)
	{
		if (colname == null) return null;
		Column column = columns.get(colname);
		if (column == null && ignoreCase)
		{
			column = columns.get(toEnglishLocaleLowerCase(colname));
		}
		return column;
	}

	public Column getColumn(String colname)
	{
		return getColumn(colname, true);
	}

	public IColumn getColumnBySqlname(String columnSqlname)
	{
		if (columnSqlname == null) return null;

		for (Column column : columns.values())
		{
			if (columnSqlname.equals(column.getSQLName()))
			{
				return column;
			}
		}

		return null;
	}

	//while creating a new table, name changes are possible
	public void columnNameChange(IValidateName validator, String oldName, String newName) throws RepositoryException
	{
		if (oldName != null && newName != null && !oldName.equals(newName))
		{
			if (columns.containsKey(toEnglishLocaleLowerCase(newName)))
			{
				throw new RepositoryException("A column on table " + getName() + " with name/dataProviderID " + newName + " already exists"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			validator.checkName(newName, null, new ValidatorSearchContext(this, IRepository.COLUMNS), true);
			Column c = columns.get(toEnglishLocaleLowerCase(oldName));
			if (c != null)
			{
				c.setSQLName(newName);
				columns.remove(toEnglishLocaleLowerCase(oldName));
				columns.put(toEnglishLocaleLowerCase(newName), c);
				fireIColumnChanged(c);
			}
		}
	}

	protected abstract void validateNewColumn(IValidateName validator, String colname) throws RepositoryException;

	@Override
	public Column createNewColumn(IValidateName validator, String colname, ColumnType columnType) throws RepositoryException
	{
		validateNewColumn(validator, colname);
		Column c = new Column(this, colname, columnType, false);
		addColumn(c);
		fireIColumnCreated(c);
		return c;
	}

	@Override
	public Column createNewColumn(IValidateName validator, String colname, ColumnType columnType, boolean allowNull, boolean pkColumn)
		throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, columnType, allowNull);
		c.setDatabasePK(pkColumn);
		return c;
	}

	@Override
	public Column createNewColumn(IValidateName validator, String colname, ColumnType columnType, boolean allowNull) throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, columnType);
		c.setAllowNull(allowNull);
		return c;
	}

	@Override
	public void setInitialized(boolean initialized)
	{
		this.initialized = initialized;
	}

	@Override
	public boolean isInitialized()
	{
		return initialized;
	}

	@Override
	public String[] getDataProviderIDs()
	{
		String[] dataProviderIDs = new String[columns.size()];
		int i = 0;
		for (IColumn column : columns.values())
		{
			dataProviderIDs[i++] = column.getDataProviderID();
		}

		return dataProviderIDs;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumnsSortedByPrefs()
	 */
	@Override
	public Iterator<Column> getColumnsSortedByIndex(List<String> indexedNamesList)
	{
		SortedList<Column> newList = new SortedList<Column>(ColumnComparator.getColumnsIndexComparator(indexedNamesList), getColumns());
		return newList.iterator();
	}
}
