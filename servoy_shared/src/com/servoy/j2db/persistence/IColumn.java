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

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.j2db.query.ColumnType;


/**
 * Tagging interface on dataprovider for use in ColumnWrapper with isAggregate test function
 *
 * @author jblok
 */
public interface IColumn extends IBaseColumn, IDataProvider, IColumnTypes
{
	public String getName();

	public boolean isAggregate();

	public Table getTable() throws RepositoryException;

	public String getTypeAsString();

	/**
	 * @return
	 */
	public ColumnInfo getColumnInfo();

	/**
	 * @return
	 */
	public boolean getExistInDB();

	/**
	 * @return
	 */
	public boolean getAllowNull();

	/**
	 * @return
	 */
	public ColumnType getConfiguredColumnType();

	/**
	 *
	 */
	public void removeColumnInfo();

	/**
	 * @return
	 */
	public int getSequenceType();

	/**
	 * @param allowNull
	 */
	public void setAllowNull(boolean allowNull);

	/**
	 * @param pkColumn
	 */
	public void setDatabasePK(boolean pkColumn);

	/**
	 * @param sequenceType
	 */
	public void setSequenceType(int sequenceType);

//	/**
//	 *
//	 */
//	public void removeColumnInfo();
}
