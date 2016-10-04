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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.IQueryElement;
import com.servoy.j2db.query.QuerySelect;
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

	public IFoundSetInternal getSharedFoundSet(String dataSource) throws ServoyException;

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
	 * @param pkSelect set pk query, may be null to generate the default pk select
	 * @param defaultSortColumns may be null
	 * @return IFoundSet
	 * @throws ServoyException
	 */
	public IFoundSetInternal getNewFoundSet(String dataSource, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException;

	/**
	 * Get a new uninitialized foundset for some table not being attached to any Form.
	 *
	 * @param table
	 * @param defaultSortColumns may be null
	 * @return IFoundSet
	 * @throws ServoyException
	 */
	public IFoundSetInternal getNewFoundSet(ITable table, QuerySelect pkSelect, List<SortColumn> defaultSortColumns) throws ServoyException;

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

	public IApplication getApplication();

	public String createDataSourceFromDataSet(String name, IDataSet dataSet, int[] intTypes, String[] pkNames) throws ServoyException;

	public boolean removeDataSource(String uri) throws RepositoryException;

	public void rollbackTransaction(boolean rollbackEdited, boolean queryForNewData, boolean revertSavedRecords);

	public IGlobalValueEntry getScopesScopeProvider();

	public GlobalTransaction getGlobalTransaction();

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

	/**
	 * @param dataSource
	 * @param persist
	 */
	public void reloadFoundsetMethod(String dataSource, IScriptProvider scriptMethod);


	public IColumnValidatorManager getColumnValidatorManager();

	public IConverterManager<IUIConverter> getUIConverterManager();

	public IConverterManager<IColumnConverter> getColumnConverterManager();

	/**
	 * Get the default pk sort columns for the data source.
	 * @since 6.1
	 */
	public List<SortColumn> getDefaultPKSortColumns(String dataSource) throws ServoyException;

	public TableFilter createTableFilter(String name, String serverName, ITable table, String dataprovider, int operator, Object value) throws ServoyException;

	public boolean addTableFilterParam(String filterName, String serverName, ITable table, String dataprovider, String operator, Object value)
		throws ServoyException;

	public ArrayList<TableFilter> getTableFilterParams(String serverName, IQueryElement sql);

	public Object[][] getTableFilterParams(String serverName, String filterName);

	public boolean removeTableFilterParam(String serverName, String filterName);

	public boolean hasTableFilter(String serverName, String tableName);

	public Collection<String> getInMemDataSourceNames();

	boolean dataSourceExists(String dataSource) throws RepositoryException;
}
