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


import java.util.HashMap;
import java.util.List;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.ServoyException;

/**
 * Internal interface to extend the foundset manager
 * 
 * @author jblok
 */
public interface IFoundSetManagerInternal extends IFoundSetManager, IDatabaseManager
{
	public IFoundSetInternal getGlobalRelatedFoundSet(String relationName) throws ServoyException;

	public IFoundSetInternal getSharedFoundSet(String dataSource, List<SortColumn> defaultSortColumns) throws ServoyException;

	/**
	 * Get a separate foundset for some Form.
	 * 
	 * @param table
	 * @return IFoundSet
	 * @throws ServoyException
	 */
	public IFoundSetInternal getSeparateFoundSet(IFoundSetListener listener, List<SortColumn> defaultSortColumns) throws ServoyException;

	/**
	 * Get a new uninitialized foundset for some data source not being attached to any Form.
	 * 
	 * @param dataSource
	 * @param defaultSortColumns may be null
	 * @return IFoundSet
	 * @throws ServoyException
	 */
	public IFoundSetInternal getNewFoundSet(String dataSource, List<SortColumn> defaultSortColumns) throws ServoyException;

	/**
	 * Get a new uninitialized foundset for some table not being attached to any Form.
	 * 
	 * @param table
	 * @param defaultSortColumns may be null
	 * @return IFoundSet
	 * @throws ServoyException
	 */
	public IFoundSetInternal getNewFoundSet(ITable table, List<SortColumn> defaultSortColumns) throws ServoyException;

	/**
	 * Get sortColumns for table and sortoptions.
	 * 
	 * @param t the table
	 * @param sortOptions the options
	 * @return List
	 */
	public List<SortColumn> getSortColumns(ITable t, String sortOptions);

	/**
	 * Get the total count in the foundset
	 * 
	 * @param fs
	 * @return the count
	 */
	public int getFoundSetCount(IFoundSetInternal fs);

	public EditRecordList getEditRecordList();

	public IServiceProvider getApplication();

	public String createDataSourceFromDataSet(String name, IDataSet dataSet, int[] intTypes) throws ServoyException;

	public boolean removeDataSource(String uri) throws RepositoryException;

	public boolean commitTransaction(boolean saveFirst);

	public void rollbackTransaction(boolean rollbackEdited, boolean queryForNewData);

	public IGlobalValueEntry getGlobalScopeProvider();

	/**
	 * add tracking info used for logging  
	 */
	public void addTrackingInfo(String columnName, Object value);


	/**
	 * get all tracking info as a map, used for logging
	 * 
	 * @return map of tracking info  
	 */
	public HashMap<String, Object> getTrackingInfo();
}
