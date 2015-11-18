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

import com.servoy.j2db.persistence.AbstractTable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * @author gganea
 *
 */
public class MemTable extends AbstractTable implements ITable
{
	private final String name;

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
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getDataSource()
	 */
	@Override
	public String getDataSource()
	{
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
		return "mem";
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
		return true;
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
	 * @see com.servoy.j2db.persistence.AbstractTable#validateNewColumn(com.servoy.j2db.persistence.IValidateName, java.lang.String)
	 */
	@Override
	protected void validateNewColumn(IValidateName validator, String colname) throws RepositoryException
	{
		if (columns.containsKey(colname))
		{
			throw new RepositoryException("A column on table " + getName() + "/server " + getServerName() + " with name " + colname + " already exists");
		}
	}
}
