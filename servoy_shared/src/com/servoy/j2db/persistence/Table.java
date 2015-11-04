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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.util.AliasKeyMap;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;

/**
 * A database table
 *
 * @author jblok
 */
public class Table implements ITable, Serializable, ISupportUpdateableName
{
	public static final long serialVersionUID = -5229736429539207132L;

	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final String serverName;
	private boolean existInDB;
	private String plainSQLName;
	private final int tableType;

	private transient volatile boolean initialized = false;
	private volatile boolean hiddenInDeveloper = false;
	private volatile boolean hiddenBecauseNoPk = false;

	/**
	 * Flag to mark this table as meta data, meta data will be included in export files
	 */
	private volatile boolean isMetaData = false;

	private final AliasKeyMap<String, String, Column> columns = new AliasKeyMap<String, String, Column>(new LinkedHashMap<String, Column>());
	private final List<Column> keyColumns = new ArrayList<Column>();

	// listeners
	protected transient List<IColumnListener> tableListeners;

	// retrieved from driver
	private String catalog;
	private String schema;

	private String dataSource;

	private List<SortColumn>[] indexes;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	/**
	 * Constructor
	 *
	 * @param theSQLName as returned by the db driver (can be camelcasing)
	 * @param qualifiedName can be null if same as plainSQLName
	 */
	public Table(String serverName, String theSQLName, boolean existInDB, int tableType, String catalog, String schema)
	{
		this.serverName = serverName;
		this.existInDB = existInDB;
		this.plainSQLName = theSQLName;
		this.tableType = tableType;
		this.catalog = catalog;
		this.schema = schema;
	}


	public void acquireReadLock()
	{
		rwLock.readLock().lock();
	}

	public void releaseReadLock()
	{
		rwLock.readLock().unlock();
	}

	public void acquireWriteLock()
	{
		rwLock.writeLock().lock();
	}

	public void releaseWriteLock()
	{
		rwLock.writeLock().unlock();
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Table)
		{
			Table other = (Table)o;
			if (other.serverName.equals(serverName) && other.plainSQLName.equalsIgnoreCase(plainSQLName))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return (serverName.hashCode() / 2) + (plainSQLName.hashCode() / 2);
	}

/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */

	private transient String name = null;//temp var

	/**
	 * Get the identifiying name of this table
	 */
	public String getName()
	{
		if (name == null)
		{
			name = Ident.generateNormalizedNonReservedOSName(plainSQLName);
		}
		return name;
	}

	/**
	 * String representation
	 */
	@Override
	public String toString()
	{
		return plainSQLName;
	}

	/**
	 * String SQL representation name (as retrieved from db)
	 */
	public String getSQLName()
	{
		return plainSQLName;
	}

	/**
	 * @return the tableType
	 */
	public int getTableType()
	{
		return this.tableType;
	}

	public void setInitialized(boolean initialized)
	{
		this.initialized = initialized;
	}

	public boolean isInitialized()
	{
		return initialized;
	}

	/**
	 * NOTE: use {@link IServerInternal#isTableMarkedAsHiddenInDeveloper(String)} if you do not want to load (init) table columns from DB by getting the Table object.<br><br>
	 *
	 * Only used in developer. Some tables should be hidden from use if specified.<BR>
	 * Solutions might want to deprecate certain tables or just not use the whole data model.
	 */
	public boolean isMarkedAsHiddenInDeveloper()
	{
		return hiddenInDeveloper;
	}

	/**
	 * Use {@link IServerInternal#setTableMarkedAsHiddenInDeveloper(Table, boolean)} instead.
	 * This should not be called directly, but be managed by the server object.
	 */
	public void setMarkedAsHiddenInDeveloperInternal(boolean hidden)
	{
		hiddenInDeveloper = hidden;
	}

	public boolean isHiddenInDeveloperBecauseNoPk()
	{
		return hiddenBecauseNoPk;
	}

	public void setHiddenInDeveloperBecauseNoPk(boolean b)
	{
		hiddenBecauseNoPk = b;
	}

	/**
	 * Flag to mark this table as meta data.
	 */
	public boolean isMarkedAsMetaData()
	{
		return isMetaData;
	}

	/**
	 * Flag to mark this table as meta data.
	 */
	public void setMarkedAsMetaData(boolean isMetaData)
	{
		this.isMetaData = isMetaData;
	}

	public String getCatalog()
	{
		return catalog;
	}

	public String getSchema()
	{
		return schema;
	}

	/**
	 * @param table_cat
	 * @param table_schem
	 */
	public void updateSqlnameCatalogAndSchema(String table_dbname, String table_cat, String table_schem)
	{
		this.plainSQLName = table_dbname;
		this.catalog = table_cat;
		this.schema = table_schem;
	}

	/**
	 * @param indexes
	 */
	public void setIndexes(List<SortColumn>[] indexes)
	{
		this.indexes = indexes;
	}

	/**
	 * @return the indexes
	 */
	public List<SortColumn>[] getIndexes()
	{
		return indexes;
	}

	/**
	 * @see com.servoy.j2db.persistence.ITable#getColumnType(java.lang.String)
	 */
	public int getColumnType(String cname)
	{
		IColumn column = getColumn(cname);
		if (column != null)
		{
			return Column.mapToDefaultType(((Column)column).getType());
		}
		return 0;
	}

	public void setDataSource(String dataSource)
	{
		this.dataSource = dataSource;
	}

	public String getDataSource()
	{
		if (dataSource == null)
		{
			// when dataSource has not been set default to a db-uri
			dataSource = DataSourceUtils.createDBTableDataSource(serverName, getName());
		}
		return dataSource;
	}

	public String getServerName()
	{
		return serverName;
	}

	void addRowIdentColumn(Column c)
	{
		if (!keyColumns.contains(c)) keyColumns.add(c);
		Collections.sort(keyColumns, NameComparator.INSTANCE);
	}

	void removeRowIdentColumn(Column c)
	{
		keyColumns.remove(c);
	}

	/**
	 * Get all key Columns
	 */
	public List<Column> getRowIdentColumns()
	{
		return keyColumns;
	}

	public int getRowIdentColumnsCount()
	{
		return keyColumns.size();
	}

	public int getPKColumnTypeRowIdentCount()
	{
		int retval = 0;
		Iterator<Column> it = keyColumns.iterator();
		while (it.hasNext())
		{
			Column c = it.next();
			if (c.getRowIdentType() == Column.PK_COLUMN)
			{
				retval++;
			}
		}
		return retval;
	}

	public void addColumn(Column c)
	{
		columns.put(c.getName(), c);
	}

	public Column getColumn(String colname)
	{
		return getColumn(colname, true);
	}

	public Column getColumn(String colname, boolean ignoreCase)
	{
		if (colname == null) return null;
		return columns.get(ignoreCase ? Utils.toEnglishLocaleLowerCase(colname) : colname);
	}

	public Collection<Column> getColumns()
	{
		return columns.values();
	}

	public Iterator<IColumn> getColumnsSortedByName()
	{
		SortedList<IColumn> newList = new SortedList<IColumn>(ColumnComparator.INSTANCE, getColumns());
		return newList.iterator();
	}

	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull) throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, type, length, scale);
		c.setAllowNull(allowNull);
		return c;
	}

	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale, boolean allowNull, boolean pkColumn)
		throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, type, length, scale, allowNull);
		c.setDatabasePK(pkColumn);
		return c;
	}

	public Column createNewColumn(IValidateName validator, String colname, int type, int length, boolean allowNull) throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, type, length, 0);
		c.setAllowNull(allowNull);
		return c;
	}

	public Column createNewColumn(IValidateName validator, String colname, int type, int length, boolean allowNull, boolean pkColumn) throws RepositoryException
	{
		Column c = createNewColumn(validator, colname, type, length, 0, allowNull);
		c.setDatabasePK(pkColumn);
		return c;
	}

	public IColumn createNewIColumn(IValidateName validator, String colname, int type, int length) throws RepositoryException
	{
		return createNewColumn(validator, colname, type, length);
	}

	public Column createNewColumn(IValidateName validator, String colname, int type, int length) throws RepositoryException
	{
		return createNewColumn(validator, colname, type, length, 0);
	}

	public Column createNewColumn(IValidateName validator, String colname, int type, int length, int scale) throws RepositoryException
	{
		if (columns.containsKey(Utils.toEnglishLocaleLowerCase(colname)))
		{
			throw new RepositoryException("A column on table " + getName() + "/server " + getServerName() + " with name " + colname + " already exists"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		validator.checkName(colname, 0, new ValidatorSearchContext(this, IRepository.COLUMNS), true);
		Column c = new Column(this, colname, type, length, scale, false);
		addColumn(c);
		fireIColumnCreated(c);
		return c;
	}

	//while creating a new table, name changes are possible
	void columnNameChange(IValidateName validator, String oldName, String newName) throws RepositoryException
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

	/**
	 * Called when the dataProviderID of a column might have changed - the special double-keyed map we use should get updated.
	 * For example after an import into the repository, an already initialized table might use new aliases for columns. Or after changing the active solution in developer.
	 */
	public void updateDataproviderIDsIfNeeded()
	{
		for (Entry<String, Column> entry : columns.entrySet())
		{
			// just in case column info dataprovider ID changed
			columns.put(entry.getKey(), entry.getValue());
		}
	}

	public void columnDataProviderIDChanged(String oldDataProviderID)
	{
		columns.updateAlias(oldDataProviderID);
	}

	private transient List<Column> deleteColumns;

	public void removeColumn(IColumn c)
	{

		if (existInDB && ((Column)c).getExistInDB())
		{
			if (deleteColumns == null) deleteColumns = new ArrayList<Column>();
			deleteColumns.add((Column)c);
		}
		keyColumns.remove(c);//just to make sure
		columns.remove(c.getName());
		fireIColumnRemoved(c);
	}

	public void removeColumn(String colname)
	{
		Column c = getColumn(colname);
		if (c != null) removeColumn(c);
	}

	public void removeAllNotInDBCreatedColumns()
	{
		if (existInDB)
		{
			List<Column> notNeededColumns = new ArrayList<Column>();
			Iterator<Column> it = columns.values().iterator();
			while (it.hasNext())
			{
				Column c = it.next();
				if (!c.getExistInDB())
				{
					notNeededColumns.add(c);
				}
			}

			Iterator<Column> it2 = notNeededColumns.iterator();
			while (it2.hasNext())
			{
				Column c = it2.next();
				keyColumns.remove(c);//just to make sure
				columns.remove(c.getName());
			}
		}
	}

	public boolean hasDeleteColumns()
	{
		return getDeleteColumns().hasNext();
	}

	public Iterator<Column> getDeleteColumns()
	{
		if (deleteColumns == null) deleteColumns = new ArrayList<Column>();
		return deleteColumns.iterator();
	}

	public void setExistInDB(boolean b)
	{
		existInDB = b;
	}

	public boolean getExistInDB()
	{
		return existInDB;
	}

	public int getColumnCount()
	{
		return columns.size();
	}

	public void addIColumnListener(IColumnListener listener)
	{
		if (listener != null)
		{
			if (tableListeners == null) tableListeners = new ArrayList<IColumnListener>();
			if (!tableListeners.contains(listener)) tableListeners.add(listener);
		}
	}

	public void removeIColumnListener(IColumnListener listener)
	{
		if (listener != null)
		{
			if (tableListeners == null) tableListeners = new ArrayList<IColumnListener>();
			tableListeners.remove(listener);
		}
	}

	protected void fireIColumnCreated(IColumn column)
	{
		if (tableListeners == null) return;
		for (IColumnListener columnListener : tableListeners)
		{
			columnListener.iColumnCreated(column);
		}
	}

	// How to call these ones?? deletes/changes are not through solution.
	protected void fireIColumnRemoved(IColumn column)
	{
		if (tableListeners == null) return;
		for (IColumnListener columnListener : tableListeners)
		{
			columnListener.iColumnRemoved(column);
		}
	}

	public void fireIColumnChanged(IColumn column)
	{
		fireIColumnsChanged(Collections.singletonList(column));
	}

	public void fireIColumnsChanged(Collection<IColumn> cols)
	{
		if (tableListeners == null || cols == null || cols.size() == 0) return;
		for (IColumnListener columnListener : tableListeners)
		{
			columnListener.iColumnsChanged(cols);
		}
	}

	/**
	 * @see com.servoy.j2db.persistence.ITable#getColumnNames()
	 */
	public String[] getColumnNames()
	{
		return columns.keySet().toArray(new String[columns.keySet().size()]);
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

	public int getColumnInfoID(String columnName)
	{
		Column c = getColumn(columnName);
		if (c != null)
		{
			ColumnInfo ci = c.getColumnInfo();
			if (ci != null)
			{
				return ci.getID();
			}
		}
		return -1;
	}

	public void updateName(IValidateName validator, String newname) throws RepositoryException
	{
		throw new UnsupportedOperationException("table name can't be updated"); //$NON-NLS-1$
	}

	public static String getTableTypeAsString(int tableType)
	{
		switch (tableType)
		{
			case TABLE :
				return "TABLE"; //$NON-NLS-1$
			case VIEW :
				return "VIEW"; //$NON-NLS-1$
			case ALIAS :
				return "ALIAS"; //$NON-NLS-1$
		}
		return "<unknown>"; //$NON-NLS-1$
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getIColumns()
	 */
	@Override
	public Collection<IColumn> getIColumns()
	{
		return new ArrayList<IColumn>(columns.values());
	}


}
