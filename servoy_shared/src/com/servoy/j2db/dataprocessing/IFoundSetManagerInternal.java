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

import com.servoy.j2db.dataprocessing.FoundSetManager.TableFilterRequest;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.IQueryElement;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.SortOptions;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.WrappedObjectReference;

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

	/**
	 * Insert data to a new or existing data source.
	 *
	 * @param pkNames gives the names of pk columns; if null and insertToDataSource finds a design-time in-mem table definition that does have pks, it will change this reference to the pks it finds.
	 * @return generated values for db identity columns
	 */
	public Object[] insertToDataSource(String name, IDataSet dataSet, ColumnType[] columnTypes, WrappedObjectReference<String[]> pkNames, boolean create,
		boolean skipOnLoad)
		throws ServoyException;

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

	public SQLGenerator getSQLGenerator();

	/**
	 * Gets the type of the column as it is used inside Servoy.<br/><br/>
	 * If the column has a DB column converter defined (in table editor - column settings) it will give the type returned by that converter.<br/><br/>
	 * If the column does not have a column converter it will return either {@link Column#getType()} or {@link IColumn#getDataProviderType()} depending on the value of 'mapToDefaultType' parameter.<br/>
	 *
	 * @param mapToDefaultType if this is false then it returns {@link Column#getType()} and not {@link IColumn#getDataProviderType()} if the column does not have a converter. If this is false then the given {@link IColumn} MUST BE a {@link Column} instance.
	 */
	public int getConvertedTypeForColumn(IColumn column, boolean mapToDefaultType);

	/**
	 * Get the default pk sort columns for the data source.
	 * @since 6.1
	 */
	public List<SortColumn> getDefaultPKSortColumns(String dataSource) throws ServoyException;

	public DataproviderTableFilterdefinition createDataproviderTableFilterdefinition(ITable table, String dataprovider, String operator, Object val)
		throws ServoyException;

	public void setTableFilters(String filterName, String serverName, List<TableFilterRequest> tableFilterRequests, boolean removeOld, boolean fire)
		throws ServoyException;

	public boolean updateTableFilterParam(String serverName, String filterName, ITable table, TableFilterdefinition tableFilterdefinition);

	public ArrayList<TableFilter> getTableFilterParams(String serverName, IQueryElement sql);

	public boolean hasTableFiltersWithJoins(String serverName, IQueryElement sql);

	public Object[][] getTableFilterParams(String serverName, String filterName);

	public boolean removeTableFilterParam(String serverName, String filterName);

	public boolean hasTableFilter(String serverName, String tableName);

	public List<TableFilter> getTableFilters(String serverName, String tableName);

	public List<TableFilter> getTableFilters(String filterName);

	public Collection<String> getInMemDataSourceNames();

	boolean dataSourceExists(String dataSource) throws RepositoryException;

	/**
	 * Get the named foundset.
	 * @since 8.2
	 */
	public IFoundSetInternal getNamedFoundSet(String name, String datasource) throws ServoyException;

	public IFoundSetInternal[] getAllLoadedFoundsets(String datasource, boolean includeViewFoundsets);

	public IFoundSetInternal findFoundset(int id);

	public int getNextFoundSetID();

	void handleUserLoggedin();

	/**
	* @param name
	* @param query
	* @return
	*/
	public ViewFoundSet getViewFoundSet(String name, QBSelect query, boolean register);

	/**
	 * @param foundset
	 * @return
	 */
	public boolean registerViewFoundSet(ViewFoundSet foundset, boolean onlyWeak);

	/**
	 * @param datasource
	 * @return
	 */
	public boolean unregisterViewFoundSet(String datasource);

	public RowManager getRowManager(String dataSource) throws ServoyException;

	/**
	 * @param name
	 * @return
	 */
	public ViewFoundSet getRegisteredViewFoundSet(String name);

	/**
	 * @return
	 */
	public Collection<String> getViewFoundsetDataSourceNames();

	public void removeFoundSet(FoundSet foundset);

	/**
	 * @param record
	 * @param state
	 * @return
	 */
	public JSRecordMarkers validateRecord(IRecordInternal record, Object state);

	public SortOptions getSortOptions(IColumn column);

}
