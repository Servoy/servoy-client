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


import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.scripting.api.IJSDatabaseManager;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.dataprocessing.datasource.JSDataSources;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.QueryString;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryCustomSelect;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.querybuilder.impl.QBAggregate;
import com.servoy.j2db.querybuilder.impl.QBColumn;
import com.servoy.j2db.querybuilder.impl.QBColumns;
import com.servoy.j2db.querybuilder.impl.QBCondition;
import com.servoy.j2db.querybuilder.impl.QBFunction;
import com.servoy.j2db.querybuilder.impl.QBFunctions;
import com.servoy.j2db.querybuilder.impl.QBGroupBy;
import com.servoy.j2db.querybuilder.impl.QBJoin;
import com.servoy.j2db.querybuilder.impl.QBJoins;
import com.servoy.j2db.querybuilder.impl.QBLogicalCondition;
import com.servoy.j2db.querybuilder.impl.QBParameter;
import com.servoy.j2db.querybuilder.impl.QBParameters;
import com.servoy.j2db.querybuilder.impl.QBPart;
import com.servoy.j2db.querybuilder.impl.QBResult;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.querybuilder.impl.QBSort;
import com.servoy.j2db.querybuilder.impl.QBSorts;
import com.servoy.j2db.querybuilder.impl.QBTableClause;
import com.servoy.j2db.querybuilder.impl.QBWhereCondition;
import com.servoy.j2db.querybuilder.impl.QUERY_COLUMN_TYPES;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.ScriptVariableScope;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.scripting.info.COLUMNTYPE;
import com.servoy.j2db.scripting.info.SQL_ACTION_TYPES;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Scriptable database manager object
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Database Manager", scriptingName = "databaseManager")
public class JSDatabaseManager implements IJSDatabaseManager
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSDatabaseManager.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { COLUMNTYPE.class, SQL_ACTION_TYPES.class, JSColumn.class, JSDataSet.class, JSDataSources.class, JSFoundSetUpdater.class, Record.class, FoundSet.class, JSTable.class, //
					QBSelect.class, QBAggregate.class, QBColumn.class, QBColumns.class, QBCondition.class, //
					QBFunction.class, QBGroupBy.class, QBJoin.class, QBJoins.class, QBLogicalCondition.class, QBWhereCondition.class, QBResult.class, //
					QBSort.class, QBSorts.class, QBTableClause.class, QBPart.class, QBParameter.class, QBParameters.class, QBFunctions.class, QUERY_COLUMN_TYPES.class };
			}
		});
	}

	private volatile IApplication application;

	public JSDatabaseManager(IApplication application)
	{
		this.application = application;
	}

	private void checkAuthorized() throws ServoyException
	{
		if (!application.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
	}

	/*
	 * _____________________________________________________________ locking methods
	 */

	/**
	 * Request lock(s) for a foundset, can be a normal or related foundset.
	 * The record_index can be -1 to lock all rows, 0 to lock the current row, or a specific row of > 0
	 * Optionally name the lock(s) so that it can be referenced it in releaseAllLocks()
	 *
	 * returns true if the lock could be acquired.
	 *
	 * @sample
	 * //locks the complete foundset
	 * databaseManager.acquireLock(foundset,-1);
	 *
	 * //locks the current row
	 * databaseManager.acquireLock(foundset,0);
	 *
	 * //locks all related orders for the current Customer
	 * var success = databaseManager.acquireLock(Cust_to_Orders,-1);
	 * if(!success)
	 * {
	 * 	plugins.dialogs.showWarningDialog('Alert','Failed to get a lock','OK');
	 * }
	 *
	 * @param foundset The JSFoundset to get the lock for
	 * @param recordIndex The record index which should be locked.
	 *
	 * @return true if the lock could be acquired.
	 */
	public boolean js_acquireLock(IFoundSetInternal foundset, Number recordIndex) throws ServoyException
	{
		return js_acquireLock(foundset, recordIndex, null);
	}

	/**
	 * @clonedesc js_acquireLock(IFoundSetInternal,Number)
	 *
	 * @sampleas js_acquireLock(IFoundSetInternal,Number)
	 *
	 * @param foundset The JSFoundset to get the lock for
	 * @param recordIndex The record index which should be locked.
	 * @param lockName The name of the lock.
	 *
	 * @return true if the lock could be acquired.
	 */
	public boolean js_acquireLock(IFoundSetInternal foundset, Number recordIndex, String lockName) throws ServoyException
	{
		int _recordIndex = Utils.getAsInteger(recordIndex);
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).acquireLock(foundset, _recordIndex - 1, lockName);
	}

	/**
	 * Adds a filter to all the foundsets based on a table.
	 * Note: if null is provided as the tablename the filter will be applied on all tables with the dataprovider name.
	 * A dataprovider can have multiple filters defined, they will all be applied.
	 * returns true if the tablefilter could be applied.
	 *
	 * @sample
	 * // Best way to call this in a global solution startup method, but filters may be added/removed at any time.
	 * // Note that multiple filters can be added to the same dataprovider, they will all be applied.
	 *
	 * // filter on messages table where messagesid>10, the filter has a name so it can be removed using databaseManager.removeTableFilterParam()
	 * var success = databaseManager.addTableFilterParam('admin', 'messages', 'messagesid', '>', 10, 'higNumberedMessagesRule')
	 *
	 * // all tables that have the companyid column should be filtered
	 * var success = databaseManager.addTableFilterParam('crm', null, 'companyidid', '=', currentcompanyid)
	 *
	 * // some filters with in-conditions
	 * var success = databaseManager.addTableFilterParam('crm', 'products', 'productcode', 'in', [120, 144, 200])
	 * var success = databaseManager.addTableFilterParam('crm', 'orders', 'countrycode', 'in', 'select country code from countries where region = "Europe"')
	 *
	 * // you can use modifiers in the operator as well, filter on companies where companyname is null or equals-ignore-case 'servoy'
	 * var success = databaseManager.addTableFilterParam('crm', 'companies', 'companyname', '#^||=', 'servoy')
	 *
	 * // the value may be null, this will result in 'column is null' sql condition.
	 * var success = databaseManager.addTableFilterParam('crm', 'companies', 'verified', '=', null)
	 *
	 * //if you want to add a filter for a column (created by you) in the i18n table
	 * databaseManager.addTableFilterParam('database', 'your_i18n_table', 'message_variant', 'in', [1, 2])
	 *
	 *
	 * @param serverName The name of the database server connection for the specified table name.
	 * @param tableName The name of the specified table.
	 * @param dataprovider A specified dataprovider column name.
	 * @param operator One of "=, <, >, >=, <=, !=, LIKE, or IN" optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null).
	 * @param value The specified filter value.
	 *
	 * @return true if the tablefilter could be applied.
	 */
	public boolean js_addTableFilterParam(String serverName, String tableName, String dataprovider, String operator, Object value) throws ServoyException
	{
		return addTableFilterParam5args(serverName, tableName, dataprovider, operator, value);
	}


	/**
	 * @clonedesc js_addTableFilterParam(String,String,String,String,Object)
	 *
	 * @sampleas js_addTableFilterParam(String,String,String,String,Object)
	 *
	 * @param datasource The datasource
	 * @param dataprovider A specified dataprovider column name.
	 * @param operator One of "=, <, >, >=, <=, !=, LIKE, or IN" optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null).
	 * @param value The specified filter value.
	 *
	 * @return true if the tablefilter could be applied.
	 */
	public boolean js_addTableFilterParam(String datasource, String dataprovider, String operator, Object value) throws ServoyException
	{
		String[] ds = DataSourceUtilsBase.getDBServernameTablename(datasource);
		if (ds == null) throw new RuntimeException("Datasource is invalid:  " + datasource); //$NON-NLS-1$
		return addTableFilterParamInternal(ds[0], ds[1], dataprovider, operator, value, null);
	}

	/**
	 * @clonedesc js_addTableFilterParam(String,String,String,String,Object)
	 *
	 * @sampleas js_addTableFilterParam(String,String,String,String,Object)
	 *
	 * @param datasource The datasource
	 * @param dataprovider A specified dataprovider column name.
	 * @param operator One of "=, <, >, >=, <=, !=, LIKE, or IN" optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null).
	 * @param value The specified filter value.
	 * @param filterName The specified name of the database table filter.
	 *
	 * @return true if the tablefilter could be applied.
	 */
	public boolean js_addTableFilterParam(String datasource, String dataprovider, String operator, Object value, String filterName) throws ServoyException
	{
		return addTableFilterParam5args(datasource, dataprovider, operator, value, filterName);
	}

	/*
	 * js_addTableFilterParam(String datasource, String dataprovider, String operator, Object value, String filterName) and js_addTableFilterParam(String
	 * serverName, String tableName, String dataprovider, String operator, Object value) have ambigious signatures when value is null or a String. Select method
	 * based on actual arguments.
	 */
	private boolean addTableFilterParam5args(String datasourceOrServerName, String dataproviderOrTablename, String operatorOrDataprovider,
		Object valueOrOperator, Object filterNameOrValue) throws ServoyException
	{
		String[] ds = DataSourceUtilsBase.getDBServernameTablename(datasourceOrServerName);
		if (ds == null)
		{
			// datasourceOrServerName=serverName, dataproviderOrTablename=tableName, operatorOrDataprovider=dataprovider, valueOrOperator=operator, filterNameOrValue=value
			if (!(valueOrOperator instanceof String))
			{
				throw new RuntimeException("Operator is invalid:  " + valueOrOperator); //$NON-NLS-1$
			}
			return addTableFilterParamInternal(datasourceOrServerName, dataproviderOrTablename, operatorOrDataprovider, (String)valueOrOperator,
				filterNameOrValue, null);
		}

		// datasourceOrServerName=datasource, dataproviderOrTablename=dataprovider, operatorOrDataprovider=operator, valueOrOperator=value, filterNameOrValue=filter
		if (filterNameOrValue != null && !(filterNameOrValue instanceof String))
		{
			throw new RuntimeException("FilterName is invalid:  " + filterNameOrValue); //$NON-NLS-1$
		}
		return addTableFilterParamInternal(ds[0], ds[1], dataproviderOrTablename, operatorOrDataprovider, valueOrOperator, (String)filterNameOrValue);
	}

	/**
	 * @clonedesc js_addTableFilterParam(String,String,String,String,Object)
	 *
	 * @sampleas js_addTableFilterParam(String,String,String,String,Object)
	 *
	 * @param serverName The name of the database server connection for the specified table name.
	 * @param tableName The name of the specified table.
	 * @param dataprovider A specified dataprovider column name.
	 * @param operator One of "=, <, >, >=, <=, !=, LIKE, or IN" optionally augmented with modifiers "#" (ignore case) or "^||" (or-is-null).
	 * @param value The specified filter value.
	 * @param filterName The specified name of the database table filter.
	 *
	 * @return true if the tablefilter could be applied.
	 */
	public boolean js_addTableFilterParam(String serverName, String tableName, String dataprovider, String operator, Object value, String filterName)
		throws ServoyException
	{
		return addTableFilterParamInternal(serverName, tableName, dataprovider, operator, value, filterName);
	}

	private boolean addTableFilterParamInternal(String serverName, String tableName, String dataprovider, String operator, Object value, String filterName)
		throws ServoyException
	{
		checkAuthorized();
		try
		{
			if (value instanceof Wrapper)
			{
				value = ((Wrapper)value).unwrap();
			}
			IServer server = application.getSolution().getServer(serverName);
			if (server == null)
			{
				application.reportJSError("Table filter not applied to unknown server '" + serverName + "', tableName = '" + tableName + "', dataprovider = '" +
					dataprovider + "', operator = '" + operator + "', value = '" + value + "', filterName = '" + filterName + "'", null);
				return false;
			}
			ITable table = null;
			if (tableName != null)
			{
				table = server.getTable(tableName);
				if (table == null)
				{
					application.reportJSError("Table filter not applied to unknown table: serverName = '" + serverName + "', tableName = '" + tableName +
						"', dataprovider = '" + dataprovider + "', operator = '" + operator + "', value = '" + value + "', filterName = '" + filterName + "'",
						null);
					return false;
				}
			}
			// else table remains null: apply to all tables with that column
			return (((FoundSetManager)application.getFoundSetManager()).addTableFilterParam(filterName, serverName, table, dataprovider, operator, value));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return false;
	}

	/**
	 * Removes a previously defined table filter.
	 *
	 * @sample var success = databaseManager.removeTableFilterParam('admin', 'higNumberedMessagesRule')
	 *
	 * @param serverName The name of the database server connection.
	 * @param filterName The name of the filter that should be removed.
	 *
	 * @return true if the filter could be removed.
	 */
	public boolean js_removeTableFilterParam(String serverName, String filterName)
	{
		if (serverName == null || filterName == null) return false;

		try
		{
			return (((FoundSetManager)application.getFoundSetManager()).removeTableFilterParam(serverName, filterName));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return false;
	}

	/**
	 * Returns a two dimensional array object containing the table filter information currently applied to the servers tables.
	 * The "columns" of a row from this array are: tablename,dataprovider,operator,value,tablefilername
	 *
	 * @sample
	 * var params = databaseManager.getTableFilterParams(databaseManager.getDataSourceServerName(controller.getDataSource()))
	 * for (var i = 0; params != null && i < params.length; i++)
	 * {
	 * 	application.output('Table filter on table ' + params[i][0]+ ': '+ params[i][1]+ ' '+params[i][2]+ ' '+params[i][3] +(params[i][4] == null ? ' [no name]' : ' ['+params[i][4]+']'))
	 * }
	 *
	 * @param serverName The name of the database server connection.
	 * @param filterName The filter name for which to get the array.
	 *
	 * @return Two dimensional array.
	 */
	public Object[][] js_getTableFilterParams(String serverName, String filterName)
	{
		if (serverName != null)
		{
			try
			{
				return (((FoundSetManager)application.getFoundSetManager()).getTableFilterParams(serverName, filterName));
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		return null;
	}

	/**
	 * @clonedesc js_getTableFilterParams(String,String)
	 *
	 * @sampleas js_getTableFilterParams(String,String)
	 *
	 * @param serverName The name of the database server connection.
	 *
	 * @return Two dimensional array.
	 */
	public Object[][] js_getTableFilterParams(String serverName)
	{
		return js_getTableFilterParams(serverName, null);
	}

	/**
	 * Creates a foundset that combines all the records of the specified one-to-many relation seen from the given parent/primary foundset.
	 * The created foundset will not contain records that have not been saved in the database, because the records in the foundset will be the
	 * result of a select query to the database.
	 *
	 * @sample
	 * // Convert in the order form a orders foundset into a orderdetails foundset,
	 * // that has all the orderdetails from all the orders in the foundset.
	 * var convertedFoundSet = databaseManager.convertFoundSet(foundset,order_to_orderdetails);
	 * // or var convertedFoundSet = databaseManager.convertFoundSet(foundset,"order_to_orderdetails");
	 * forms.orderdetails.controller.showRecords(convertedFoundSet);
	 *
	 * @param foundset The JSFoundset to convert.
	 * @param related can be a one-to-many relation object or the name of a one-to-many relation
	 *
	 * @return The converted JSFoundset.
	 */
	public FoundSet js_convertFoundSet(FoundSet foundset, FoundSet related) throws ServoyException
	{
		return convertFoundSet(foundset, related);
	}

	/**
	 * @clonedesc js_convertFoundSet(FoundSet, FoundSet)
	 * @sampleas js_convertFoundSet(FoundSet, FoundSet)
	 *
	 * @param foundset The JSFoundset to convert.
	 * @param related the name of a one-to-many relation
	 *
	 * @return The converted JSFoundset.
	 */
	public FoundSet js_convertFoundSet(FoundSet foundset, String related) throws ServoyException
	{
		return convertFoundSet(foundset, related);
	}

	public FoundSet convertFoundSet(Object foundset, Object related) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet && ((FoundSet)foundset).getTable() != null)
		{
			FoundSet fs_old = (FoundSet)foundset;
			try
			{
				String relationName;
				if (related instanceof RelatedFoundSet)
				{
					relationName = ((RelatedFoundSet)related).getRelationName();
				}
				else if (related instanceof String)
				{
					relationName = (String)related;
				}
				else
				{
					Debug.warn("convertFoundSet: invalid argument " + related); //$NON-NLS-1$
					return null;
				}

				Relation relation = application.getFlattenedSolution().getRelation(relationName);
				if (relation == null || relation.isMultiServer() || fs_old.getTable() == null ||
					!fs_old.getTable().equals(application.getFlattenedSolution().getTable(relation.getPrimaryDataSource())))
				{
					Debug.warn("convertFoundSet: cannot use relation " + relationName); //$NON-NLS-1$
					return null;
				}

				ITable ft = application.getFlattenedSolution().getTable(relation.getForeignDataSource());
				FoundSet fs_new = (FoundSet)application.getFoundSetManager().getNewFoundSet(ft, null,
					application.getFoundSetManager().getDefaultPKSortColumns(ft.getDataSource()));

				QuerySelect sql = fs_old.getPksAndRecords().getQuerySelectForModification();
				SQLSheet sheet_new = fs_old.getSQLSheet().getRelatedSheet(relation, ((FoundSetManager)application.getFoundSetManager()).getSQLGenerator());
				if (sheet_new != null)
				{
					BaseQueryTable oldTable = sql.getTable();
					ISQLTableJoin join = (ISQLTableJoin)sql.getJoin(oldTable, relation.getName());
					if (join == null)
					{
						join = SQLGenerator.createJoin(application.getFlattenedSolution(), relation, oldTable,
							new QueryTable(ft.getSQLName(), ft.getDataSource(), ft.getCatalog(), ft.getSchema()), fs_old);
						sql.addJoin(join);
					}

					BaseQueryTable mainTable = join.getForeignTable();

					// invert the join
					sql.setTable(mainTable);
					join.invert("INVERTED." + join.getName()); //$NON-NLS-1$

					// set the columns to be the PKs from the related table
					ArrayList<IQuerySelectValue> pkColumns = new ArrayList<IQuerySelectValue>();
					Iterator<Column> pks = sheet_new.getTable().getRowIdentColumns().iterator();
					while (pks.hasNext())
					{
						Column column = pks.next();
						pkColumns.add(new QueryColumn(mainTable, column.getID(), column.getSQLName(), column.getType(), column.getLength(), column.getScale(),
							column.getFlags()));
					}
					sql.setColumns(pkColumns);

					// sorting will be on the original columns, when distinct is set, this will conflict with the related pk columns
					sql.setDistinct(false);

					fs_new.setSQLSelect(sql);
					return fs_new;
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	/**
	 * Converts the argument to a JSDataSet, possible use in controller.loadRecords(dataset).
	 * The optional array of dataprovider names is used (only) to add the specified dataprovider names as columns to the dataset.
	 *
	 * @sample
	 * // converts a foundset pks to a dataset
	 * var dataset = databaseManager.convertToDataSet(foundset);
	 * // converts a foundset to a dataset
	 * //var dataset = databaseManager.convertToDataSet(foundset,['product_id','product_name']);
	 * // converts an object array to a dataset
	 * //var dataset = databaseManager.convertToDataSet(files,['name','path']);
	 * // converts an array to a dataset
	 * //var dataset = databaseManager.convertToDataSet(new Array(1,2,3,4,5,6));
	 * // converts an string list to a dataset
	 * //var dataset = databaseManager.convertToDataSet('4,5,6');
	 *
	 * @param foundset The foundset to be converted.
	 *
	 * @return JSDataSet with the data.
	 */
	public JSDataSet js_convertToDataSet(IFoundSetInternal foundset) throws RepositoryException
	{
		return js_convertToDataSet(foundset, null);
	}

	/**
	 * @clonedesc js_convertToDataSet(IFoundSetInternal)
	 *
	 * @sampleas js_convertToDataSet(IFoundSetInternal)
	 *
	 * @param foundset The foundset to be converted.
	 * @param dataproviderNames Array with column names.
	 *
	 * @return JSDataSet with the data.
	 */
	public JSDataSet js_convertToDataSet(IFoundSetInternal foundset, String[] dataproviderNames) throws RepositoryException
	{
		if (foundset == null)
		{
			return null;
		}
		String[] dpnames = { "id" }; //$NON-NLS-1$
		ColumnType[] dptypes = { ColumnType.getInstance(IColumnTypes.INTEGER, Integer.MAX_VALUE, 0) };

		List<Object[]> lst = new ArrayList<Object[]>();

		FoundSet fs = (FoundSet)foundset;
		if (fs.getTable() != null)
		{
			if (dataproviderNames != null)
			{
				dpnames = dataproviderNames;
			}
			else
			{
				dpnames = fs.getSQLSheet().getPKColumnDataProvidersAsArray();
			}

			FoundSetManager fsm = (FoundSetManager)application.getFoundSetManager();
			boolean getInOneQuery = !fs.isInFindMode() && (fs.hadMoreRows() || fs.getSize() > fsm.pkChunkSize) && !fsm.getEditRecordList().hasEditedRecords(fs);

			dptypes = new ColumnType[dpnames.length];
			Table table = fs.getSQLSheet().getTable();
			Map<String, Column> columnMap = new HashMap<String, Column>();
			for (int i = 0; i < dpnames.length; i++)
			{
				IDataProvider dp = application.getFlattenedSolution().getDataProviderForTable(table, dpnames[i]);
				dptypes[i] = dp == null ? ColumnType.getInstance(0, 0, 0)
					: ColumnType.getInstance(dp instanceof Column ? ((Column)dp).getType() : dp.getDataProviderType(), dp.getLength(),
						dp instanceof Column ? ((Column)dp).getScale() : 0);
				if (getInOneQuery)
				{
					// only columns and data we can get from the foundset (calculations only when stored)
					if (dp instanceof Column)
					{
						columnMap.put(dpnames[i], (Column)dp);
						// Blobs require special resultset handling
						getInOneQuery = !SQLGenerator.isBlobColumn((Column)dp);
					}
					else
					{
						// aggregates, globals
						getInOneQuery = fs.containsDataProvider(dpnames[i]);
					}
				}
			}

			if (getInOneQuery && columnMap.size() > 0)
			{
				// large foundset, query the columns in 1 go
				QuerySelect sqlSelect = AbstractBaseQuery.deepClone(fs.getSqlSelect());
				ArrayList<IQuerySelectValue> cols = new ArrayList<IQuerySelectValue>(columnMap.size());
				ArrayList<String> distinctColumns = new ArrayList<String>(columnMap.size());
				for (String dpname : dpnames)
				{
					Column column = columnMap.get(dpname);
					if (column != null && !distinctColumns.contains(dpname))
					{
						distinctColumns.add(dpname);
						cols.add(new QueryColumn(sqlSelect.getTable(), column.getID(), column.getSQLName(), column.getType(), column.getLength(),
							column.getScale(), column.getFlags()));
					}
				}

				boolean hasJoins = sqlSelect.getJoins() != null;
				if (hasJoins)
				{
					// add pk columns so distinct-in-memory can be used
					List<Column> rowIdentColumns = ((Table)fs.getTable()).getRowIdentColumns();
					for (Column column : rowIdentColumns)
					{
						if (!columnMap.containsKey(column.getDataProviderID()))
						{
							cols.add(new QueryColumn(sqlSelect.getTable(), column.getID(), column.getSQLName(), column.getType(), column.getLength(),
								column.getScale(), column.getFlags()));
						}
					}
				}

				sqlSelect.setColumns(cols);
				try
				{
					SQLSheet sheet = fs.getSQLSheet();
					IConverterManager<IColumnConverter> columnConverterManager = ((FoundSetManager)fs.getFoundSetManager()).getColumnConverterManager();
					SQLStatement trackingInfo = null;
					if (fsm.getEditRecordList().hasAccess(sheet.getTable(), IRepository.TRACKING_VIEWS))
					{
						trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, sheet.getServerName(), sheet.getTable().getName(), null, null);
						trackingInfo.setTrackingData(sqlSelect.getColumnNames(), new Object[][] { }, new Object[][] { }, fsm.getApplication().getUserUID(),
							fsm.getTrackingInfo(), fsm.getApplication().getClientID());
					}
					IDataSet dataSet = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), fsm.getTransactionID(sheet),
						sqlSelect, fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), hasJoins, 0, -1, IDataServer.FOUNDSET_LOAD_QUERY, trackingInfo);

					lst = new ArrayList<Object[]>(dataSet.getRowCount());
					for (int i = 0; i < dataSet.getRowCount(); i++)
					{
						Object[] row = new Object[dpnames.length];
						Object[] dataseRow = dataSet.getRow(i); // may contain more data: pk columns for distinct-in-memory
						for (int j = 0; j < dpnames.length; j++)
						{
							Column column = columnMap.get(dpnames[j]);
							if (column == null)
							{
								// fs.containsDataProvider returned true for this dpname
								row[j] = fs.getDataProviderValue(dpnames[j]);
							}
							else
							{
								row[j] = sheet.convertValueToObject(dataseRow[distinctColumns.indexOf(dpnames[j])], sheet.getColumnIndex(dpnames[j]),
									columnConverterManager);
							}
						}
						lst.add(row);
					}
				}
				catch (RepositoryException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					Debug.error(e);
					throw new RepositoryException(e.getMessage());
				}
			}
			else
			{
				// loop over the records
				for (int i = 0; i < fs.getSize(); i++)
				{
					IRecordInternal record = fs.getRecord(i);
					Object[] pk = new Object[dpnames.length];
					for (int j = 0; j < dpnames.length; j++)
					{
						pk[j] = record.getValue(dpnames[j]);
					}
					lst.add(pk);
				}
			}
		}
		return new JSDataSet(application, BufferedDataSetInternal.createBufferedDataSet(dpnames, dptypes, lst, false));
	}

	/**
	 * @clonedesc js_convertToDataSet(IFoundSetInternal)
	 *
	 * @sampleas js_convertToDataSet(IFoundSetInternal)
	 *
	 * @param ids Concatenated values to be put into dataset.
	 *
	 * @return JSDataSet with the data.
	 */
	public JSDataSet js_convertToDataSet(String ids)
	{
		if (ids == null)
		{
			return null;
		}
		String[] dpnames = { "id" }; //$NON-NLS-1$
		ColumnType[] dptypes = { ColumnType.getInstance(IColumnTypes.INTEGER, Integer.MAX_VALUE, 0) };

		List<Object[]> lst = new ArrayList<Object[]>();
		StringTokenizer st = new StringTokenizer(ids, ",;\n\r\t "); //$NON-NLS-1$
		while (st.hasMoreElements())
		{
			Object o = st.nextElement();
			if (o instanceof Double && ((Double)o).doubleValue() == ((Double)o).intValue())
			{
				o = new Integer(((Double)o).intValue());
			}
			lst.add(new Object[] { o });
		}
		return new JSDataSet(application, BufferedDataSetInternal.createBufferedDataSet(dpnames, dptypes, lst, false));
	}

	/**
	 * @clonedesc js_convertToDataSet(IFoundSetInternal)
	 *
	 * @sampleas js_convertToDataSet(IFoundSetInternal)
	 *
	 * @param values The values array.
	 * @param dataproviderNames The property names array.

	 * @return JSDataSet with the data.
	 */
	public JSDataSet js_convertToDataSet(Object[] values, String[] dataproviderNames)
	{
		if (values == null)
		{
			return null;
		}
		String[] dpnames = { "id" }; //$NON-NLS-1$
		ColumnType[] dptypes = { ColumnType.getInstance(IColumnTypes.INTEGER, Integer.MAX_VALUE, 0) };

		List<Object[]> lst = new ArrayList<Object[]>();

		Object[] array = values;
		if (dataproviderNames != null)
		{
			dpnames = dataproviderNames;
		}

		Map<String, Method> getters = new HashMap<String, Method>();

		for (Object o : array)
		{
			if (o instanceof Number || o instanceof String || o instanceof UUID || o instanceof Date)
			{
				if (o instanceof Double && ((Double)o).doubleValue() == ((Double)o).intValue())
				{
					o = new Integer(((Double)o).intValue());
				}
				lst.add(new Object[] { o });
			}
			else if (o instanceof Scriptable)
			{
				List<Object> row = new ArrayList<Object>();
				for (String dpname : dpnames)
				{
					if (((Scriptable)o).has(dpname, (Scriptable)o)) row.add(ScriptVariableScope.unwrap(((Scriptable)o).get(dpname, (Scriptable)o)));
				}
				if (dpnames.length != row.size() || dpnames.length == 0)
				{
					// for backward compatibility
					lst.add(new Object[] { o });
				}
				else
				{
					lst.add(row.toArray());
				}
			}
			else if (o != null)
			{
				//try reflection
				List<Object> row = new ArrayList<Object>();
				for (String dpname : dpnames)
				{
					Method m = getMethod(o, dpname, getters);
					if (m != null)
					{
						try
						{
							row.add(m.invoke(o, (Object[])null));
						}
						catch (Exception e)
						{
							Debug.error(e);
						}
					}
				}
				if (dpnames.length != row.size() || dpnames.length == 0)
				{
					// for backward compatibility
					lst.add(new Object[] { o });
				}
				else
				{
					lst.add(row.toArray());
				}
			}
		}

		return new JSDataSet(application, BufferedDataSetInternal.createBufferedDataSet(dpnames, dptypes, lst, false));
	}

	/**
	 * @clonedesc js_convertToDataSet(IFoundSetInternal)
	 *
	 * @sampleas js_convertToDataSet(IFoundSetInternal)
	 *
	 * @param values The values array.

	 * @return JSDataSet with the data.
	 */
	public JSDataSet js_convertToDataSet(Object[] values)
	{
		return js_convertToDataSet(values, null);
	}

	private Method getMethod(Object o, String pname, Map<String, Method> getters)
	{
		Method retval = getters.get(pname);
		if (retval == null && !getters.containsKey(pname))
		{
			Method[] methods = o.getClass().getMethods();
			for (Method m : methods)
			{
				String name = m.getName();
				if (m.getParameterTypes().length == 0 && name.startsWith("get") && name.substring(3).equalsIgnoreCase(pname)) //$NON-NLS-1$
				{
					retval = m;
					break;
				}
			}
			getters.put(pname, retval);
		}
		return retval;
	}

	/**
	 * Returns an empty dataset object.
	 *
	 * @sample
	 * // gets an empty dataset with a specifed row and column count
	 * var dataset = databaseManager.createEmptyDataSet(10,10)
	 * // gets an empty dataset with a specifed row count and column array
	 * var dataset2 = databaseManager.createEmptyDataSet(10,new Array ('a','b','c','d'))
	 *
	 * @param rowCount The number of rows in the DataSet object.
	 * @param columnCount Number of columns.
	 *
	 * @return An empty JSDataSet with the initial sizes.
	 */
	public JSDataSet js_createEmptyDataSet(int rowCount, int columnCount)
	{
		return new JSDataSet(application, rowCount, new String[columnCount]);
	}

	/**
	 * @clonedesc js_createEmptyDataSet(int,int)
	 *
	 * @sampleas js_createEmptyDataSet(int,int)
	 *
	 * @param rowCount
	 * @param columnNames
	 *
	 * @return An empty JSDataSet with the initial sizes.
	 */
	public JSDataSet js_createEmptyDataSet(int rowCount, String[] columnNames)
	{
		return new JSDataSet(application, rowCount, columnNames);
	}

	/**
	 * @clonedesc js_createEmptyDataSet(int,int)
	 *
	 * @sampleas js_createEmptyDataSet(int,int)
	 *
	 * @return An empty JSDataSet with the initial sizes.
	 */
	public JSDataSet js_createEmptyDataSet()
	{
		return new JSDataSet(application);
	}

	private boolean validateQueryArguments(Object[] arguments, String sql_query)
	{
		if (arguments != null)
		{
			for (int i = 0; i < arguments.length; i++)
			{
				if (arguments[i] instanceof java.util.Date)
				{
					arguments[i] = new Timestamp(((java.util.Date)arguments[i]).getTime());
				}
				else if (arguments[i] instanceof DbIdentValue && ((DbIdentValue)arguments[i]).getPkValue() == null)
				{
					application.reportJSError("Custom query: " + sql_query + //$NON-NLS-1$
						" not executed because the arguments have a database ident value that is null, from a not yet saved record", null); //$NON-NLS-1$
					return false;
				}
				else if (arguments[i] == null)
				{
					arguments[i] = ValueFactory.createNullValue(Types.OTHER);
				}
			}
		}

		return true;
	}

	private boolean validateQueryArguments(ISQLQuery select)
	{
		if (select != null)
		{
			if (select instanceof QuerySelect && ((QuerySelect)select).getColumns() == null)
			{
				application.reportJSError("Custom query: " + select + " not executed because no columns are specified to be selected", null);
				return false;
			}

			final List<Placeholder> placeHolders = new ArrayList<Placeholder>();
			AbstractBaseQuery.acceptVisitor(select, new IVisitor()
			{
				public Object visit(Object o)
				{
					if (o instanceof Placeholder)
					{
						placeHolders.add((Placeholder)o);
					}
					return o;
				}
			});

			for (Placeholder placeholder : placeHolders)
			{
				if (!placeholder.isSet())
				{
					application.reportJSError("Custom query: " + select + //$NON-NLS-1$
						" not executed because not all arguments have been set: " + placeholder.getKey(), null); //$NON-NLS-1$
					return false;
				}
				if (placeholder.getValue() instanceof DbIdentValue && ((DbIdentValue)placeholder.getValue()).getPkValue() == null)
				{
					application.reportJSError("Custom query: " + select + //$NON-NLS-1$
						" not executed because the arguments have a database ident value that is null, from a not yet saved record", null); //$NON-NLS-1$
					return false;
				}

				if (placeholder.getValue() instanceof java.util.Date)
				{
					placeholder.setValue(new Timestamp(((java.util.Date)placeholder.getValue()).getTime()));
				}
			}
		}

		return true;
	}

	/**
	 * @clonedesc js_createDataSourceByQuery(String, String, String, Object[], int, int[])
	 * @sampleas js_createDataSourceByQuery(String, String, String, Object[], int, int[])
	 *
	 * @param name data source name
	 * @param server_name The name of the server where the query should be executed.
	 * @param sql_query The custom sql.
	 * @param arguments Specified arguments or null if there are no arguments.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, String server_name, String sql_query, Object[] arguments, int max_returned_rows)
		throws ServoyException
	{
		return js_createDataSourceByQuery(name, server_name, sql_query, arguments, max_returned_rows, null);
	}

	/**
	 * Performs a sql query on the specified server, saves the the result in a datasource.
	 * Will throw an exception if anything went wrong when executing the query.
	 * Column types in the datasource are inferred from the query result or can be explicitly specified.
	 *
	 * Using this variation of createDataSourceByQuery any Tablefilter on the involved tables will be disregarded.
	 *
	 * A datasource can be reused if the data has the same signature (column names and types).
	 * A new createDataSourceByQuery() call will clear the datasource contents from a previous call and insert the current data.
	 *
	 * @sample
	 * var query = 'select address, city, country  from customers';
	 * var uri = databaseManager.createDataSourceByQuery('mydata', 'example_data', query, null, 999);
	 * //var uri = databaseManager.createDataSourceByQuery('mydata', 'example_data', query, null, 999, [JSColumn.TEXT, JSColumn.TEXT, JSColumn.TEXT]);
	 *
	 * // the uri can be used to create a form using solution model
	 * var myForm = solutionModel.newForm('newForm', uri, 'myStyleName', false, 800, 600)
	 * myForm.newTextField('city', 140, 20, 140,20)
	 *
	 * // the uri can be used to acces a foundset directly
	 * var fs = databaseManager.getFoundSet(uri)
	 * fs.loadAllRecords();
	 *
	 * @param name data source name
	 * @param server_name The name of the server where the query should be executed.
	 * @param sql_query The custom sql.
	 * @param arguments Specified arguments or null if there are no arguments.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 * @param types The column types
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, String server_name, String sql_query, Object[] arguments, int max_returned_rows, int[] types)
		throws ServoyException
	{
		return js_createDataSourceByQuery(name, server_name, sql_query, arguments, max_returned_rows, types, null);
	}

	/**
	 * Performs a sql query on the specified server, saves the the result in a datasource.
	 * Will throw an exception if anything went wrong when executing the query.
	 * Column types in the datasource are inferred from the query result or can be explicitly specified.
	 *
	 * Using this variation of createDataSourceByQuery any Tablefilter on the involved tables will be disregarded.
	 *
	 * A datasource can be reused if the data has the same signature (column names and types).
	 * A new createDataSourceByQuery() call will clear the datasource contents from a previous call and insert the current data.
	 *
	 * @sample
	 * var query = 'select customer_id, address, city, country  from customers';
	 * var uri = databaseManager.createDataSourceByQuery('mydata', 'example_data', query, null, 999);
	 * //var uri = databaseManager.createDataSourceByQuery('mydata', 'example_data', query, null, 999, [JSColumn.TEXT, JSColumn.TEXT, JSColumn.TEXT], ['customer_id']);
	 *
	 * // the uri can be used to create a form using solution model
	 * var myForm = solutionModel.newForm('newForm', uri, 'myStyleName', false, 800, 600)
	 * myForm.newTextField('city', 140, 20, 140,20)
	 *
	 * // the uri can be used to acces a foundset directly
	 * var fs = databaseManager.getFoundSet(uri)
	 * fs.loadAllRecords();
	 *
	 * @param name data source name
	 * @param server_name The name of the server where the query should be executed.
	 * @param sql_query The custom sql.
	 * @param arguments Specified arguments or null if there are no arguments.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 * @param types The column types
	 * @param pkNames array of pk names, when null a hidden pk-column will be added
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, String server_name, String sql_query, Object[] arguments, int max_returned_rows, int[] types,
		String[] pkNames) throws ServoyException
	{
		checkAuthorized();
		if (server_name == null) throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { "<null>" })); //$NON-NLS-1$

		if (!checkQueryForSelect(sql_query))
		{
			throw new RuntimeException(new DataException(ServoyException.BAD_SQL_SYNTAX, new SQLException(), sql_query));
		}
		if (name == null || !checkQueryForSelect(sql_query) || !validateQueryArguments(arguments, sql_query))
		{
			return null;
		}

		try
		{
			return ((FoundSetManager)application.getFoundSetManager()).createDataSourceFromQuery(name, server_name, new QueryCustomSelect(sql_query, arguments),
				false, max_returned_rows, types, pkNames);
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @clonedesc js_createDataSourceByQuery(String, QBSelect, Boolean, Number, int[], String[])
	 * @sampleas js_createDataSourceByQuery(String, QBSelect, Boolean, Number, int[], String[])
	 *
	 * @param name data source name
	 * @param query The query builder to be executed.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, QBSelect query, Number max_returned_rows) throws ServoyException
	{
		return js_createDataSourceByQuery(name, query, Boolean.TRUE, max_returned_rows, null, null);
	}

	/**
	 * Performs a query and saves the result in a datasource.
	 * Will throw an exception if anything went wrong when executing the query.
	 * Column types in the datasource are inferred from the query result or can be explicitly specified.
	 *
	 * Using this variation of createDataSourceByQuery any Tablefilter on the involved tables will be taken into account.
	 *
	 * A datasource can be reused if the data has the same signature (column names and types).
	 * A new createDataSourceByQuery() call will clear the datasource contents from a previous call and insert the current data.
	 *
	 * @sample
	 * // select customer data for order 1234
	 * var q = datasources.db.example_data.customers.createSelect();
	 * q.result.add(q.columns.address).add(q.columns.city).add(q.columns.country);
	 * q.where.add(q.joins.customers_to_orders.columns.orderid.eq(1234));
	 * var uri = databaseManager.createDataSourceByQuery('mydata', q, 999);
	 * //var uri = databaseManager.createDataSourceByQuery('mydata', q, 999, [JSColumn.TEXT, JSColumn.TEXT, JSColumn.TEXT]);
	 *
	 * // the uri can be used to create a form using solution model
	 * var myForm = solutionModel.newForm('newForm', uri, 'myStyleName', false, 800, 600);
	 * myForm.newTextField('city', 140, 20, 140,20);
	 *
	 * // the uri can be used to acces a foundset directly
	 * var fs = databaseManager.getFoundSet(uri);
	 * fs.loadAllRecords();
	 *
	 * @param name Data source name
	 * @param query The query builder to be executed.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 * @param types The column types
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, QBSelect query, Number max_returned_rows, int[] types) throws ServoyException
	{
		return js_createDataSourceByQuery(name, query, Boolean.TRUE, max_returned_rows, types, null);
	}

	/**
	 * Performs a query and saves the result in a datasource.
	 * Will throw an exception if anything went wrong when executing the query.
	 * Column types in the datasource are inferred from the query result or can be explicitly specified.
	 *
	 * Using this variation of createDataSourceByQuery any Tablefilter on the involved tables will be taken into account.
	 *
	 * A datasource can be reused if the data has the same signature (column names and types).
	 * A new createDataSourceByQuery() call will clear the datasource contents from a previous call and insert the current data.
	 *
	 * @sample
	 * // select customer data for order 1234
	 * var q = datasources.db.example_data.customers.createSelect();
	 * q.result.add(q.columns.customer_id).add(q.columns.city).add(q.columns.country);
	 * q.where.add(q.joins.customers_to_orders.columns.orderid.eq(1234));
	 * var uri = databaseManager.createDataSourceByQuery('mydata', q, 999, null, ['customer_id']);
	 * //var uri = databaseManager.createDataSourceByQuery('mydata', q, 999, [JSColumn.TEXT, JSColumn.TEXT, JSColumn.TEXT], ['customer_id']);
	 *
	 * // the uri can be used to create a form using solution model
	 * var myForm = solutionModel.newForm('newForm', uri, 'myStyleName', false, 800, 600);
	 * myForm.newTextField('city', 140, 20, 140,20);
	 *
	 * // the uri can be used to acces a foundset directly
	 * var fs = databaseManager.getFoundSet(uri);
	 * fs.loadAllRecords();
	 *
	 * @param name Data source name
	 * @param query The query builder to be executed.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 * @param types The column types
	 * @param pkNames array of pk names, when null a hidden pk-column will be added
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, QBSelect query, Number max_returned_rows, int[] types, String[] pkNames) throws ServoyException
	{
		return js_createDataSourceByQuery(name, query, Boolean.TRUE, max_returned_rows, types, pkNames);
	}

	/**
	 * Performs a query and saves the result in a datasource.
	 * Will throw an exception if anything went wrong when executing the query.
	 * Column types in the datasource are inferred from the query result or can be explicitly specified.
	 *
	 * A datasource can be reused if the data has the same signature (column names and types).
	 * A new createDataSourceByQuery() call will clear the datasource contents from a previous call and insert the current data.
	 *
	 * @sample
	 * // select customer data for order 1234
	 * var q = datasources.db.example_data.customers.createSelect()
	 * q.result.add(q.columns.customer_id).add(q.columns.city).add(q.columns.country);
	 * q.where.add(q.joins.customers_to_orders.columns.orderid.eq(1234));
	 * var uri = databaseManager.createDataSourceByQuery('mydata', q, true, 999, null, ['customer_id']);
	 * //var uri = databaseManager.createDataSourceByQuery('mydata', q, true, 999, [JSColumn.TEXT, JSColumn.TEXT, JSColumn.TEXT], ['customer_id']);
	 *
	 * // the uri can be used to create a form using solution model
	 * var myForm = solutionModel.newForm('newForm', uri, 'myStyleName', false, 800, 600);
	 * myForm.newTextField('city', 140, 20, 140,20);
	 *
	 * // the uri can be used to acces a foundset directly
	 * var fs = databaseManager.getFoundSet(uri);
	 * fs.loadAllRecords();
	 *
	 * @param name Data source name
	 * @param query The query builder to be executed.
	 * @param useTableFilters use table filters (default true).
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 * @param types The column types, when null the types are inferred from the query.
	 * @param pkNames array of pk names, when null a hidden pk-column will be added
	 *
	 * @return datasource containing the results of the query or null if the parameters are wrong.
	 */
	public String js_createDataSourceByQuery(String name, QBSelect query, Boolean useTableFilters, Number max_returned_rows, int[] types, String[] pkNames)
		throws ServoyException
	{
		int _max_returned_rows = Utils.getAsInteger(max_returned_rows);
		checkAuthorized();

		String serverName = DataSourceUtils.getDataSourceServerName(query.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { query.getDataSource() }));

		QuerySelect select = query.build();
		if (!validateQueryArguments(select))
		{
			return null;
		}

		try
		{
			return ((FoundSetManager)application.getFoundSetManager()).createDataSourceFromQuery(name, serverName, select,
				!Boolean.FALSE.equals(useTableFilters), _max_returned_rows, types, pkNames);
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Performs a sql query on the specified server, returns the result in a dataset.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * Using this variation of getDataSetByQuery any Tablefilter on the involved tables will be disregarded.
	 *
	 * @sample
	 * //finds duplicate records in a specified foundset
	 * var vQuery =" SELECT companiesid from companies where company_name IN (SELECT company_name from companies group bycompany_name having count(company_name)>1 )";
	 * var vDataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), vQuery, null, 1000);
	 * controller.loadRecords(vDataset);
	 *
	 * var maxReturnedRows = 10;//useful to limit number of rows
	 * var query = 'select c1,c2,c3 from test_table where start_date = ?';//do not use '.' or special chars in names or aliases if you want to access data by name
	 * var args = new Array();
	 * args[0] = order_date //or  new Date()
	 * var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), query, args, maxReturnedRows);
	 *
	 * // place in label:
	 * // elements.myLabel.text = '<html>'+dataset.getAsHTML()+'</html>';
	 *
	 * //example to calc a strange total
	 * global_total = 0;
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	dataset.rowIndex = i;
	 * 	global_total = global_total + dataset.c1 + dataset.getValue(i,3);
	 * }
	 * //example to assign to dataprovider
	 * //employee_salary = dataset.getValue(row,column)
	 *
	 * @param server_name The name of the server where the query should be executed.
	 * @param sql_query The custom sql.
	 * @param arguments Specified arguments or null if there are no arguments.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	public JSDataSet js_getDataSetByQuery(String server_name, String sql_query, Object[] arguments, Number max_returned_rows) throws ServoyException
	{
		int _max_returned_rows = Utils.getAsInteger(max_returned_rows);
		checkAuthorized();
		if (server_name == null) throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { "<null>" })); //$NON-NLS-1$

		if (!checkQueryForSelect(sql_query))
		{
			throw new RuntimeException(new DataException(ServoyException.BAD_SQL_SYNTAX, new SQLException(), sql_query));
		}
		if (!validateQueryArguments(arguments, sql_query))
		{
			return new JSDataSet(application);
		}

		try
		{
			return new JSDataSet(application, ((FoundSetManager)application.getFoundSetManager()).getDataSetByQuery(server_name,
				new QueryCustomSelect(sql_query, arguments), false, _max_returned_rows));
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	private boolean checkQueryForSelect(String sql)
	{
		if (sql == null) return false;

		String lowerCaseSql = sql.trim().toLowerCase();
		int declareIndex = lowerCaseSql.indexOf("declare"); //$NON-NLS-1$
		int withIndex = lowerCaseSql.indexOf("with"); //$NON-NLS-1$
		int selectIndex = lowerCaseSql.indexOf("select"); //$NON-NLS-1$
		int callIndex = lowerCaseSql.indexOf("call"); //$NON-NLS-1$
		return ((declareIndex != -1 && declareIndex < 4) || (selectIndex != -1 && selectIndex < 4) || (callIndex != -1 && callIndex < 4) ||
			(withIndex != -1 && withIndex < 4));
	}


	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * Using this variation of getDataSetByQuery any Tablefilter on the involved tables will be taken into account.
	 *
	 * @sample
	 * // use the query froma foundset and add a condition
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var q = foundset.getQuery()
	 * q.where.add(q.joins.orders_to_order_details.columns.discount.eq(2))
	 * var maxReturnedRows = 10;//useful to limit number of rows
	 * var ds = databaseManager.getDataSetByQuery(q, maxReturnedRows);
	 *
	 * // query: select PK from example.book_nodes where parent = 111 and(note_date is null or note_date > now)
	 * var query = datasources.db.example_data.book_nodes.createSelect().result.addPk().root
	 * query.where.add(query.columns.parent_id.eq(111))
	 * 	.add(query.or
	 * 	.add(query.columns.note_date.isNull)
	 * 	.add(query.columns.note_date.gt(new Date())))
	 * databaseManager.getDataSetByQuery(q, max_returned_rows)
	 *
	 * @param query QBSelect query.
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	public JSDataSet js_getDataSetByQuery(QBSelect query, Number max_returned_rows) throws ServoyException
	{
		return js_getDataSetByQuery(query, Boolean.TRUE, max_returned_rows);
	}

	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * @sample
	 * // use the query froma foundset and add a condition
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var q = foundset.getQuery()
	 * q.where.add(q.joins.orders_to_order_details.columns.discount.eq(2))
	 * var maxReturnedRows = 10;//useful to limit number of rows
	 * var ds = databaseManager.getDataSetByQuery(q, true, maxReturnedRows);
	 *
	 * // query: select PK from example.book_nodes where parent = 111 and(note_date is null or note_date > now)
	 * var query = datasources.db.example_data.book_nodes.createSelect().result.addPk().root
	 * query.where.add(query.columns.parent_id.eq(111))
	 * 	.add(query.or
	 * 	.add(query.columns.note_date.isNull)
	 * 	.add(query.columns.note_date.gt(new Date())))
	 * databaseManager.getDataSetByQuery(q, true, max_returned_rows)
	 *
	 * @param query QBSelect query.
	 * @param useTableFilters use table filters (default true).
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	public JSDataSet js_getDataSetByQuery(QBSelect query, Boolean useTableFilters, Number max_returned_rows) throws ServoyException
	{
		int _max_returned_rows = Utils.getAsInteger(max_returned_rows);
		checkAuthorized();

		String serverName = DataSourceUtils.getDataSourceServerName(query.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { query.getDataSource() }));
		QuerySelect select = query.build();

		if (!validateQueryArguments(select))
		{
			return new JSDataSet(application);
		}

		try
		{
			return new JSDataSet(application, ((FoundSetManager)application.getFoundSetManager()).getDataSetByQuery(serverName, select,
				!Boolean.FALSE.equals(useTableFilters), _max_returned_rows));
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @deprecated As of release 3.5, replaced by {@link plugins.rawSQL#executeStoredProcedure(String, String, Object[], int[], int)}.
	 */
	@Deprecated
	public Object js_executeStoredProcedure(String serverName, String procedureDeclaration, Object[] args, int[] inOutType, int maxNumberOfRowsToRetrieve)
		throws ServoyException
	{
		checkAuthorized();
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "rawSQL"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptable so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod("js_executeStoredProcedure", //$NON-NLS-1$
						new Class[] { String.class, String.class, Object[].class, int[].class, int.class });
					return m.invoke(so, new Object[] { serverName, procedureDeclaration, args, inOutType, new Integer(maxNumberOfRowsToRetrieve) });
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError("Writing to file failed", //$NON-NLS-1$
			"For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code"); //$NON-NLS-1$
		return null;
	}

	/**
	 * @deprecated No longer supported.
	 */
	@Deprecated
	public String js_getLastDatabaseMessage()//for last unspecified db warning and errors
	{
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the total number of records in a foundset.
	 *
	 * NOTE: This can be an expensive operation (time-wise) if your resultset is large.
	 *
	 * @sample
	 * //return the total number of records in a foundset.
	 * databaseManager.getFoundSetCount(foundset);
	 *
	 * @param foundset The JSFoundset to get the count for.
	 *
	 * @return the foundset count
	 */
	@JSSignature(arguments = { FoundSet.class })
	public int js_getFoundSetCount(Object foundset) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof IFoundSetInternal)
		{
			return application.getFoundSetManager().getFoundSetCount((IFoundSetInternal)foundset);
		}
		return 0;
	}

	/**
	 * Can be used to recalculate a specified record or all rows in the specified foundset.
	 * May be necessary when data is changed from outside of servoy, or when there is data changed inside servoy
	 * but records with calculations depending on that data where not loaded so not updated and you need to update
	 * the stored calculation values because you are depending on that with queries or aggregates.
	 *
	 * @sample
	 * // recalculate one record from a foundset.
	 * databaseManager.recalculate(foundset.getRecord(1));
	 * // recalculate all records from the foundset.
	 * // please use with care, this can be expensive!
	 * //databaseManager.recalculate(foundset);
	 *
	 * @param foundsetOrRecord JSFoundset or JSRecord to recalculate.
	 */
	public void js_recalculate(Object foundsetOrRecord) throws ServoyException
	{
		checkAuthorized();
		if (foundsetOrRecord instanceof IRecordInternal)
		{
			IRecordInternal record = (IRecordInternal)foundsetOrRecord;
			SQLSheet sheet = record.getParentFoundSet().getSQLSheet();
			recalculateRecord(record, sheet.getStoredCalculationNames());
			((FoundSet)record.getParentFoundSet()).fireFoundSetChanged();
		}
		else if (foundsetOrRecord instanceof FoundSet)
		{
			FoundSet fs = (FoundSet)foundsetOrRecord;
			SQLSheet sheet = fs.getSQLSheet();
			List<String> calculationNames = sheet.getStoredCalculationNames();
			for (int i = 0; i < fs.getSize(); i++)
			{
				recalculateRecord(fs.getRecord(i), calculationNames);
			}
			fs.fireFoundSetChanged();
		}
	}

	private void recalculateRecord(IRecordInternal record, List<String> calcnames)
	{
		record.startEditing();
		record.getRawData().getRowManager().flagAllRowCalcsForRecalculation(record.getPKHashKey());
		//recalc all stored calcs (required due to use of plugin methods in calc)
		for (String calc : calcnames)
		{
			record.getValue(calc);
		}
		try
		{
			record.stopEditing();
		}
		catch (Exception e)
		{
			Debug.error("error when recalculation record: " + record, e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a JSFoundsetUpdater object that can be used to update all or a specific number of rows in the specified foundset.
	 *
	 * @sampleas com.servoy.j2db.dataprocessing.JSFoundSetUpdater#js_performUpdate()
	 *
	 * @param foundset The foundset to update.
	 *
	 * @return The JSFoundsetUpdater for the specified JSFoundset.
	 */
	@JSSignature(arguments = { FoundSet.class })
	public JSFoundSetUpdater js_getFoundSetUpdater(Object foundset) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet)
		{
			return new JSFoundSetUpdater(application, (FoundSet)foundset);
		}
		return null;
	}

	/**
	 * Returns an array of records that fail after a save.
	 *
	 * @sample
	 * var array = databaseManager.getFailedRecords()
	 * for( var i = 0 ; i < array.length ; i++ )
	 * {
	 * 	var record = array[i];
	 * 	application.output(record.exception);
	 * 	if (record.exception.getErrorCode() == ServoyException.RECORD_VALIDATION_FAILED)
	 * 	{
	 * 		// exception thrown in pre-insert/update/delete event method
	 * 		var thrown = record.exception.getValue()
	 * 		application.output("Record validation failed: "+thrown)
	 * 	}
	 * 	// find out the table of the record (similar to getEditedRecords)
	 * 	var jstable = databaseManager.getTable(record);
	 * 	var tableSQLName = jstable.getSQLName();
	 * 	application.output('Table:'+tableSQLName+' in server:'+jstable.getServerName()+' failed to save.')
	 * }
	 *
	 * @return Array of failed JSRecords
	 */
	public IRecordInternal[] js_getFailedRecords()
	{
		return application.getFoundSetManager().getEditRecordList().getFailedRecords();
	}

	/**
	 * Returns an array of records that fail after a save.
	 *
	 * @sample
	 * var array = databaseManager.getFailedRecords(foundset)
	 * for( var i = 0 ; i < array.length ; i++ )
	 * {
	 * 	var record = array[i];
	 * 	application.output(record.exception);
	 * 	if (record.exception.getErrorCode() == ServoyException.RECORD_VALIDATION_FAILED)
	 * 	{
	 * 		// exception thrown in pre-insert/update/delete event method
	 * 		var thrown = record.exception.getValue()
	 * 		application.output("Record validation failed: "+thrown)
	 * 	}
	 * 	// find out the table of the record (similar to getEditedRecords)
	 * 	var jstable = databaseManager.getTable(record);
	 * 	var tableSQLName = jstable.getSQLName();
	 * 	application.output('Table:'+tableSQLName+' in server:'+jstable.getServerName()+' failed to save.')
	 * }
	 *
	 * @param foundset return failed records in the foundset only.
	 *
	 * @return Array of failed JSRecords
	 */
	public IRecordInternal[] js_getFailedRecords(IFoundSetInternal foundset)
	{
		return application.getFoundSetManager().getEditRecordList().getFailedRecords(foundset);
	}

	/**
	 * Returns an array of edited records with outstanding (unsaved) data.
	 *
	 * This is different form JSRecord.isEditing() because this call actually checks if there are changes between the current
	 * record data and the stored data in the database. If there are no changes then the record is removed from the edited records
	 * list (so after this call JSRecord.isEditing() can return false when it returned true just before this call)
	 *
	 * NOTE: To return a dataset of outstanding (unsaved) edited data for each record, see JSRecord.getChangedData();
	 * NOTE2: The fields focus may be lost in user interface in order to determine the edits.
	 *
	 * @sample
	 * //This method can be used to loop through all outstanding changes,
	 * //the application.output line contains all the changed data, their tablename and primary key
	 * var editr = databaseManager.getEditedRecords()
	 * for (x=0;x<editr.length;x++)
	 * {
	 * 	var ds = editr[x].getChangedData();
	 * 	var jstable = databaseManager.getTable(editr[x]);
	 * 	var tableSQLName = jstable.getSQLName();
	 * 	var pkrec = jstable.getRowIdentifierColumnNames().join(',');
	 * 	var pkvals = new Array();
	 * 	for (var j = 0; j < jstable.getRowIdentifierColumnNames().length; j++)
	 * 	{
	 * 		pkvals[j] = editr[x][jstable.getRowIdentifierColumnNames()[j]];
	 * 	}
	 * 	application.output('Table: '+tableSQLName +', PKs: '+ pkvals.join(',') +' ('+pkrec +')');
	 * 	// Get a dataset with outstanding changes on a record
	 * 	for( var i = 1 ; i <= ds.getMaxRowIndex() ; i++ )
	 * 	{
	 * 		application.output('Column: '+ ds.getValue(i,1) +', oldValue: '+ ds.getValue(i,2) +', newValue: '+ ds.getValue(i,3));
	 * 	}
	 * }
	 * //in most cases you will want to set autoSave back on now
	 * databaseManager.setAutoSave(true);
	 *
	 * @return Array of outstanding/unsaved JSRecords.
	 */
	public IRecordInternal[] js_getEditedRecords()
	{
		return application.getFoundSetManager().getEditRecordList().getEditedRecords();
	}

	/**
	 * Returns an array of edited records with outstanding (unsaved) data.
	 *
	 * NOTE: To return a dataset of outstanding (unsaved) edited data for each record, see JSRecord.getChangedData();
	 * NOTE2: The fields focus may be lost in user interface in order to determine the edits.
	 *
	 * @sample
	 * //This method can be used to loop through all outstanding changes in a foundset,
	 * //the application.output line contains all the changed data, their tablename and primary key
	 * var editr = databaseManager.getEditedRecords(foundset)
	 * for (x=0;x<editr.length;x++)
	 * {
	 * 	var ds = editr[x].getChangedData();
	 * 	var jstable = databaseManager.getTable(editr[x]);
	 * 	var tableSQLName = jstable.getSQLName();
	 * 	var pkrec = jstable.getRowIdentifierColumnNames().join(',');
	 * 	var pkvals = new Array();
	 * 	for (var j = 0; j < jstable.getRowIdentifierColumnNames().length; j++)
	 * 	{
	 * 		pkvals[j] = editr[x][jstable.getRowIdentifierColumnNames()[j]];
	 * 	}
	 * 	application.output('Table: '+tableSQLName +', PKs: '+ pkvals.join(',') +' ('+pkrec +')');
	 * 	// Get a dataset with outstanding changes on a record
	 * 	for( var i = 1 ; i <= ds.getMaxRowIndex() ; i++ )
	 * 	{
	 * 		application.output('Column: '+ ds.getValue(i,1) +', oldValue: '+ ds.getValue(i,2) +', newValue: '+ ds.getValue(i,3));
	 * 	}
	 * }
	 * databaseManager.saveData(foundset);//save all records from foundset
	 *
	 * @param foundset return edited records in the foundset only.
	 *
	 * @return Array of outstanding/unsaved JSRecords.
	 */

	public IRecordInternal[] js_getEditedRecords(IFoundSetInternal foundset)
	{
		return application.getFoundSetManager().getEditRecordList().getEditedRecords(foundset, true);
	}

	/**
	 * Returns a dataset with outstanding (not saved) changed data on a record
	 *
	 * NOTE: To return an array of records with oustanding changed data, see the function databaseManager.getEditedRecords().
	 *
	 * @deprecated As of release 5.0, replaced by {@link Record#getChangedData()}.
	 *
	 * @sample
	 * var dataset = databaseManager.getChangedRecordData(record)
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	application.output(dataset.getValue(i,1) +' '+ dataset.getValue(i,2) +' '+ dataset.getValue(i,3));
	 * }
	 *
	 * @param r The specified record.
	 *
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getEditedRecords()
	 * @see com.servoy.j2db.dataprocessing.Record#js_getChangedData()
	 */
	@Deprecated
	public JSDataSet js_getChangedRecordData(Object r) throws ServoyException
	{
		checkAuthorized();
		if (r instanceof IRecordInternal)
		{
			IRecordInternal rec = ((IRecordInternal)r);
			if (rec.getParentFoundSet() != null && rec.getRawData() != null)
			{
				String[] cnames = rec.getParentFoundSet().getSQLSheet().getColumnNames();
				Object[] oldd = rec.getRawData().getRawOldColumnData();
				List<Object[]> rows = new ArrayList<Object[]>();
				if (oldd != null || !rec.getRawData().existInDB())
				{
					Object[] newd = rec.getRawData().getRawColumnData();
					for (int i = 0; i < cnames.length; i++)
					{
						Object oldv = (oldd == null ? null : oldd[i]);
						if (!Utils.equalObjects(oldv, newd[i])) rows.add(new Object[] { cnames[i], oldv, newd[i] });
					}
				}
				return new JSDataSet(application, new BufferedDataSet(new String[] { "col_name", "old_value", "new_value" }, rows)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		return null;
	}

	/**
	 * Returns the internal SQL which defines the specified (related)foundset.
	 * Optionally, the foundset and table filter params can be excluded in the sql (includeFilters=false).
	 * Make sure to set the applicable filters when the sql is used in a loadRecords() call.
	 * When the founset is in find mode, the find conditions are included in the resulting query.
	 *
	 * @sample var sql = databaseManager.getSQL(foundset)
	 *
	 * @param foundsetOrQBSelect The JSFoundset or QBSelect to get the sql for.
	 * @param includeFilters include the foundset and table filters.
	 *
	 * @return String representing the sql of the JSFoundset.
	 */
	public String js_getSQL(Object foundsetOrQBSelect, boolean includeFilters) throws ServoyException
	{
		checkAuthorized();
		if (foundsetOrQBSelect instanceof FoundSet && ((FoundSet)foundsetOrQBSelect).getTable() != null)
		{
			try
			{
				QuerySet querySet = getQuerySet(((FoundSet)foundsetOrQBSelect).getCurrentStateQuery(true, false), includeFilters);
				StringBuilder sql = new StringBuilder();
				QueryString[] prepares = querySet.getPrepares();
				for (int i = 0; prepares != null && i < prepares.length; i++)
				{
					// TODO parameters from updates and cleanups
					// sql.append(updates[i].getSql());
					// sql.append("\n"); //$NON-NLS-1$
				}
				sql.append(querySet.getSelect().getSql());
				QueryString[] cleanups = querySet.getCleanups();
				for (int i = 0; cleanups != null && i < cleanups.length; i++)
				{
					// TODO parameters from updates and cleanups
					//sql.append("\n"); //$NON-NLS-1$
					//sql.append(cleanups[i].getSql());
				}
				return sql.toString();
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (foundsetOrQBSelect instanceof QBSelect)
		{
			try
			{
				QuerySelect select = ((QBSelect)foundsetOrQBSelect).build();
				if (select.getColumns() == null)
				{
					// no columns, add pk
					// note that QBSelect.build() already returns a clone
					ITable table = application.getFoundSetManager().getTable(select.getTable().getDataSource());
					Iterator<Column> pkIt = ((Table)table).getRowIdentColumns().iterator();
					if (!pkIt.hasNext())
					{
						throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { table.getName() });
					}

					while (pkIt.hasNext())
					{
						Column c = pkIt.next();
						select.addColumn(new QueryColumn(select.getTable(), c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale(), c.getFlags()));
					}
				}
				QuerySet querySet = getQuerySet(select, includeFilters);
				return querySet.getSelect().getSql();
			}
			catch (RemoteException e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	/**
	 * Returns the internal SQL which defines the specified (related)foundset.
	 * Table filters are on by default.
	 * Make sure to set the applicable filters when the sql is used in a loadRecords() call.
	 *
	 * @sample var sql = databaseManager.getSQL(foundset)
	 *
	 * @param foundsetOrQBSelect The JSFoundset or QBSelect to get the sql for.
	 *
	 * @return String representing the sql of the JSFoundset.
	 */
	public String js_getSQL(Object foundsetOrQBSelect) throws ServoyException
	{
		checkAuthorized();
		return js_getSQL(foundsetOrQBSelect, true);
	}

	/**
	 * Returns the internal SQL parameters, as an array, that are used to define the specified (related)foundset.
	 * When the founset is in find mode, the arguments for the find conditions are included in the result.
	 *
	 * @sample var sqlParameterArray = databaseManager.getSQLParameters(foundset,false)
	 *
	 * @param foundsetOrQBSelect The JSFoundset or QBSelect to get the sql parameters for.
	 * @param includeFilters include the parameters for the filters.
	 *
	 * @return An Array with the sql parameter values.
	 */
	public Object[] js_getSQLParameters(Object foundsetOrQBSelect, boolean includeFilters) throws ServoyException
	{
		checkAuthorized();
		if (foundsetOrQBSelect instanceof FoundSet && ((FoundSet)foundsetOrQBSelect).getTable() != null)
		{
			try
			{
				// TODO parameters from updates and cleanups
				QuerySet querySet = getQuerySet(((FoundSet)foundsetOrQBSelect).getCurrentStateQuery(true, false), includeFilters);
				Object[][] qsParams = querySet.getSelect().getParameters();
				if (qsParams == null || qsParams.length == 0)
				{
					return null;
				}
				return qsParams[0];
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (foundsetOrQBSelect instanceof QBSelect)
		{
			try
			{
				QuerySelect select = ((QBSelect)foundsetOrQBSelect).build();
				if (select.getColumns() == null)
				{
					// no columns, add pk
					// note that QBSelect.build() already returns a clone
					ITable table = application.getFoundSetManager().getTable(select.getTable().getDataSource());
					Iterator<Column> pkIt = ((Table)table).getRowIdentColumns().iterator();
					if (!pkIt.hasNext())
					{
						throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { table.getName() });
					}

					while (pkIt.hasNext())
					{
						Column c = pkIt.next();
						select.addColumn(new QueryColumn(select.getTable(), c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale(), c.getFlags()));
					}
				}
				QuerySet querySet = getQuerySet(select, includeFilters);
				Object[][] qsParams = querySet.getSelect().getParameters();
				if (qsParams == null || qsParams.length == 0)
				{
					return null;
				}
				return qsParams[0];
			}
			catch (RemoteException e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	/**
	 * Returns the internal SQL parameters, as an array, that are used to define the specified (related)foundset.
	 * Parameters for the filters are included.
	 *
	 * @sample var sqlParameterArray = databaseManager.getSQLParameters(foundset,false)
	 *
	 * @param foundsetOrQBSelect The JSFoundset or QBSelect to get the sql parameters for.
	 *
	 * @return An Array with the sql parameter values.
	 */
	public Object[] js_getSQLParameters(Object foundsetOrQBSelect) throws ServoyException
	{
		checkAuthorized();
		return js_getSQLParameters(foundsetOrQBSelect, true);
	}

	private QuerySet getQuerySet(QuerySelect sqlSelect, boolean includeFilters) throws RemoteException, ServoyException
	{
		String serverName = DataSourceUtils.getDataSourceServerName(sqlSelect.getTable().getDataSource());
		ArrayList<TableFilter> tableFilterParams;
		if (includeFilters)
		{
			tableFilterParams = ((FoundSetManager)application.getFoundSetManager()).getTableFilterParams(serverName, sqlSelect);
		}
		else
		{
			// get the sql without any filters
			sqlSelect = AbstractBaseQuery.deepClone(sqlSelect);
			sqlSelect.clearCondition(SQLGenerator.CONDITION_FILTER);
			tableFilterParams = null;
		}
		return application.getDataServer().getSQLQuerySet(serverName, sqlSelect, tableFilterParams, 0, -1, true);
	}

	/**
	 * Flushes the client data cache and requeries the data for a record (based on the record index) in a foundset or all records in the foundset.
	 * Used where a program external to Servoy has modified the database record.
	 * Record index of -1 will refresh all records in the foundset and 0 the selected record.
	 *
	 * @sample
	 * //refresh the second record from the foundset.
	 * databaseManager.refreshRecordFromDatabase(foundset,2)
	 * //flushes all records in the related foundset  (-1 is or can be an expensive operation)
	 * databaseManager.refreshRecordFromDatabase(order_to_orderdetails,-1);
	 *
	 * @param foundset The JSFoundset to refresh
	 * @param index The index of the JSRecord that must be refreshed (or -1 for all).
	 *
	 * @return true if the refresh was done.
	 */
	public boolean js_refreshRecordFromDatabase(Object foundset, int index) throws ServoyException
	{
		checkAuthorized();
		int idx = index;
		if (foundset instanceof IFoundSetInternal && ((IFoundSetInternal)foundset).getTable() != null)
		{
			if (idx == -1)//refresh all
			{
				// TODO should be checked if the foundset is completely loaded and only has X records?
				// So we only flush those records not the complete table?
				((FoundSetManager)application.getFoundSetManager()).flushCachedDatabaseData(
					application.getFoundSetManager().getDataSource(((IFoundSetInternal)foundset).getTable()));
				return true;
			}
			else
			{
				if (idx == 0)
				{
					idx = ((IFoundSetInternal)foundset).getSelectedIndex() + 1;
				}
				IRecordInternal rec = ((IFoundSetInternal)foundset).getRecord(idx - 1);//row because used by javascript 1 based
				if (rec != null)
				{
					Row r = rec.getRawData();
					if (r != null)
					{
						try
						{
							r.rollbackFromDB();
							r.setLastException(null);
							return true;
						}
						catch (Exception e)
						{
							Debug.error(e);
							return false;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * This method is deprecated, use databaseManager.convertToDataSet(foundset, pkNames) instead.
	 *
	 * @sample
	 * var dataSet = databaseManager.convertToDataSet(foundset,['order_id']);
	 *
	 * @deprecated As of release 6.0, replaced by {@link #convertToDataSet(Object[])}.
	 *
	 * @param foundset The foundset
	 * @param dataprovider The dataprovider for the values of the array.
	 *
	 * @return An Array with the column values.
	 */
	@Deprecated
	public Object[] js_getFoundSetDataProviderAsArray(Object foundset, String dataprovider) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet && ((FoundSet)foundset).getSQLSheet().getTable() != null)
		{
			FoundSet fs = (FoundSet)foundset;
			FoundSetManager fsm = (FoundSetManager)application.getFoundSetManager();
			SQLSheet sheet = fs.getSQLSheet();
			Column column = sheet.getTable().getColumn(dataprovider);
			if (column != null)
			{
				IDataSet dataSet = null;
				if ((fs.hadMoreRows() || fs.getSize() > fsm.pkChunkSize) && !fsm.getEditRecordList().hasEditedRecords(fs))
				{
					// large foundset, query the column in 1 go
					QuerySelect sqlSelect = AbstractBaseQuery.deepClone(fs.getSqlSelect());
					ArrayList<IQuerySelectValue> cols = new ArrayList<IQuerySelectValue>(1);
					cols.add(new QueryColumn(sqlSelect.getTable(), column.getID(), column.getSQLName(), column.getType(), column.getLength(), column.getScale(),
						column.getFlags()));
					sqlSelect.setColumns(cols);
					SQLStatement trackingInfo = null;
					if (fsm.getEditRecordList().hasAccess(sheet.getTable(), IRepository.TRACKING_VIEWS))
					{
						trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, sheet.getServerName(), sheet.getTable().getName(), null, null);
						trackingInfo.setTrackingData(new String[] { column.getSQLName() }, new Object[][] { }, new Object[][] { },
							fsm.getApplication().getUserUID(), fsm.getTrackingInfo(), fsm.getApplication().getClientID());
					}
					try
					{
						dataSet = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), fsm.getTransactionID(sheet),
							sqlSelect, fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), false, 0, -1, IDataServer.FOUNDSET_LOAD_QUERY, trackingInfo);
					}
					catch (RemoteException e)
					{
						Debug.error(e);
						return new Object[0];
					}
					catch (ServoyException e)
					{
						Debug.error(e);
						return new Object[0];
					}
				}
				else
				{
					// small foundset or there are edited records
					List<Column> pks = fs.getSQLSheet().getTable().getRowIdentColumns();
					if (pks.size() == 1 && pks.get(0).equals(column)) //if is pk optimize
					{
						PksAndRecordsHolder pksAndRecordsCopy;
						PKDataSet pkds;
						boolean queryForMore;
						int rowCount;
						synchronized (fs.getPksAndRecords())
						{
							pksAndRecordsCopy = fs.getPksAndRecords().shallowCopy();
							pkds = pksAndRecordsCopy.getPks();
							queryForMore = pkds == null || pkds.hadMoreRows();
							rowCount = pkds == null ? 0 : pkds.getRowCount();
						}
						if (queryForMore)
						{
							fs.queryForMorePKs(pksAndRecordsCopy, rowCount, -1, true);
						}
						dataSet = pkds;
					}
				}

				if (dataSet != null)
				{
					Object[] retval = new Object[dataSet.getRowCount()];
					for (int i = 0; i < retval.length; i++)
					{
						Object[] dataSetRow = dataSet.getRow(i);
						if (dataSetRow == null)
						{
							Debug.warn("js_getFoundSetDataProviderAsArray - null row at index: " + i + " when getting dataprovider: " + dataprovider + //$NON-NLS-1$//$NON-NLS-2$
								" from foundset: " + foundset); //$NON-NLS-1$
							retval[i] = null;
						}
						else
						{
							Object value = dataSetRow[0];
							if (column.hasFlag(Column.UUID_COLUMN))
							{
								// this is a UUID column, first convert to UUID (could be string or byte array (media)) - so we can get/use it as a valid uuid string
								value = Utils.getAsUUID(value, false);
							}
							retval[i] = value;
						}
					}
					return retval;
				}
			}
			// cannot het the data via a dataset, use the records (could be slow)
			List<Object> lst = new ArrayList<Object>();
			for (int i = 0; i < fs.getSize(); i++)
			{
				IRecordInternal r = fs.getRecord(i);
				Object value = r.getValue(dataprovider);
				if (value instanceof Date)
				{
					value = new Date(((Date)value).getTime());
				}
				lst.add(value);
			}
			return lst.toArray();
		}
		return new Object[0];
	}

	/**
	 * Returns the server name from the datasource, or null if not a database datasource.
	 *
	 * @sample var servername = databaseManager.getDataSourceServerName(datasource);
	 *
	 * @param dataSource The datasource string to get the server name from.
	 *
	 * @return The servername of the datasource.
	 */
	@JSFunction
	public String getDataSourceServerName(String dataSource)
	{
		String[] retval = DataSourceUtilsBase.getDBServernameTablename(dataSource);
		if (retval == null) return null;
		return retval[0];
	}

	/**
	 * Returns the table name from the datasource, or null if not a database datasource.
	 *
	 * @description-mc
	 * Returns the table name from the datasource, or null if the specified argument is not a database datasource.
	 *
	 * @sample var tablename = databaseManager.getDataSourceTableName(datasource);
	 *
	 * @sample-mc
	 * var theTableName = databaseManager.getDataSourceTableName(datasource);
	 *
	 * @param dataSource The datasource string to get the tablename from.
	 *
	 * @return The tablename of the datasource.
	 */
	@JSFunction
	public String getDataSourceTableName(String dataSource)
	{
		String[] retval = DataSourceUtilsBase.getDBServernameTablename(dataSource);
		if (retval == null) return null;
		return retval[1];
	}

	/**
	 * Returns the datasource corresponding to the given server/table.
	 *
	 * @sample var datasource = databaseManager.getDataSource('example_data', 'categories');
	 *
	 * @param serverName The name of the table's server.
	 * @param tableName The table's name.
	 *
	 * @return The datasource of the given table/server.
	 */
	@JSFunction
	public String getDataSource(String serverName, String tableName)
	{
		return DataSourceUtils.createDBTableDataSource(serverName, tableName);
	}

	/**
	 * Check wether a data source exists. This function can be used for any type of data source (db-based, in-memory).
	 *
	 * @sample
	 * if (!databaseManager.dataSourceExists(dataSource))
	 * {
	 *    // does not exist
	 * }
	 *
	 * @param datasource
	 *
	 * @return boolean exists
	 */
	public boolean js_dataSourceExists(String dataSource) throws ServoyException
	{
		checkAuthorized();
		try
		{
			return application.getFoundSetManager().getTable(dataSource) != null;
		}
		catch (RepositoryException e)
		{
			Debug.debug(e); // server not found
			return false;
		}
	}

	/**
	 * Returns the JSTable object from which more info can be obtained (like columns).
	 * The parameter can be a JSFoundset,JSRecord,datasource string or server/tablename combination.
	 *
	 * @sample
	 * var jstable = databaseManager.getTable(controller.getDataSource());
	 * //var jstable = databaseManager.getTable(foundset);
	 * //var jstable = databaseManager.getTable(record);
	 * //var jstable = databaseManager.getTable(datasource);
	 * var tableSQLName = jstable.getSQLName();
	 * var columnNamesArray = jstable.getColumnNames();
	 * var firstColumnName = columnNamesArray[0];
	 * var jscolumn = jstable.getColumn(firstColumnName);
	 * var columnLength = jscolumn.getLength();
	 * var columnType = jscolumn.getTypeAsString();
	 * var columnSQLName = jscolumn.getSQLName();
	 * var isPrimaryKey = jscolumn.isRowIdentifier();
	 *
	 * @param foundset The foundset where the JSTable can be get from.
	 *
	 * @return the JSTable get from the input.
	 */
	public JSTable js_getTable(IFoundSetInternal foundset) throws ServoyException
	{
		String serverName = null;
		String tableName = null;
		if (foundset != null && foundset.getTable() != null)
		{
			serverName = foundset.getTable().getServerName();
			tableName = foundset.getTable().getName();
		}
		return js_getTable(serverName, tableName);
	}

	/**
	 * @clonedesc js_getTable(IFoundSetInternal)
	 *
	 * @sampleas js_getTable(IFoundSetInternal)
	 *
	 * @param record The record where the table can be get from.
	 *
	 * @return the JSTable get from the input.
	 */

	public JSTable js_getTable(IRecordInternal record) throws ServoyException
	{
		String serverName = null;
		String tableName = null;
		if (record != null)
		{
			IFoundSetInternal fs = record.getParentFoundSet();
			if (fs != null && fs.getTable() != null)
			{
				serverName = fs.getTable().getServerName();
				tableName = fs.getTable().getName();
			}
		}
		return js_getTable(serverName, tableName);
	}

	/**
	 * @clonedesc js_getTable(IFoundSetInternal)
	 *
	 * @sampleas js_getTable(IFoundSetInternal)
	 *
	 * @param dataSource The datasource where the table can be get from.
	 *
	 * @return the JSTable get from the input.
	 */
	public JSTable js_getTable(String dataSource) throws ServoyException
	{
		String serverName = null;
		String tableName = null;
		if (dataSource != null)
		{
			String[] server_table = DataSourceUtilsBase.getDBServernameTablename(dataSource);
			if (server_table != null)
			{
				serverName = server_table[0];
				tableName = server_table[1];
			}
		}
		return js_getTable(serverName, tableName);
	}

	/**
	 * @clonedesc js_getTable(IFoundSetInternal)
	 *
	 * @sampleas js_getTable(IFoundSetInternal)
	 *
	 * @param serverName Server name.
	 * @param tableName Table name.
	 *
	 * @return the JSTable get from the input.
	 */
	public JSTable js_getTable(String serverName, String tableName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			if (serverName != null)
			{
				IServer server = application.getSolution().getServer(serverName);
				if (server != null && tableName != null)
				{
					ITable t = server.getTable(tableName);
					if (t != null)
					{
						return new JSTable(t, server);
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	//strongly recommended to use a transaction
	//currently does not support compound pks
	/**
	 * Merge records from the same foundset, updates entire datamodel (via foreign type on columns) with destination
	 * record pk, deletes source record. Do use a transaction!
	 *
	 * This function is very handy in situations where duplicate data exists. It allows you to merge the two records
	 * and move all related records in one go. Say the source_record is "Ikea" and the combined_destination_record is "IKEA", the
	 * "Ikea" record is deleted and all records related to it (think of contacts and orders, for instance) will be related
	 * to the "IKEA" record.
	 *
	 * The function takes an optional array of column names. If provided, the data in the named columns will be copied
	 * from source_record to combined_destination_record.
	 *
	 * Note that it is essential for both records to originate from the same foundset, as shown in the sample code.
	 *
	 * @sample databaseManager.mergeRecords(foundset.getRecord(1),foundset.getRecord(2));
	 *
	 * @param sourceRecord The source JSRecord to copy from.
	 * @param combinedDestinationRecord The target/destination JSRecord to copy into.
	 * @param columnNames The column names array that should be copied.
	 *
	 * @return true if the records could me merged.
	 */
	public boolean js_mergeRecords(IRecordInternal sourceRecord, IRecordInternal combinedDestinationRecord, String[] columnNames) throws ServoyException
	{
		checkAuthorized();
		if (sourceRecord != null && combinedDestinationRecord != null)
		{
			FoundSetManager fsm = (FoundSetManager)application.getFoundSetManager();
			try
			{
				if (sourceRecord.getParentFoundSet() != combinedDestinationRecord.getParentFoundSet())
				{
					return false;
				}

				Table mainTable = (Table)combinedDestinationRecord.getParentFoundSet().getTable();
				String mainTableForeignType = mainTable.getName();
				String transaction_id = fsm.getTransactionID(mainTable.getServerName());

				Object sourceRecordPK = null;
				Object combinedDestinationRecordPK = null;

				Column pkc = null;
				Iterator<Column> pk_it = mainTable.getRowIdentColumns().iterator();
				if (pk_it.hasNext())
				{
					pkc = pk_it.next();
					sourceRecordPK = sourceRecord.getValue(pkc.getDataProviderID());
					if (sourceRecordPK == null) sourceRecordPK = ValueFactory.createNullValue(pkc.getType());
					combinedDestinationRecordPK = combinedDestinationRecord.getValue(pkc.getDataProviderID());
					if (combinedDestinationRecordPK == null) combinedDestinationRecordPK = ValueFactory.createNullValue(pkc.getType());
					if (pk_it.hasNext()) return false;//multipk not supported
				}

				List<SQLStatement> updates = new ArrayList<SQLStatement>();

				IServer server = application.getSolution().getServer(mainTable.getServerName());
				if (server != null)
				{
					Iterator<String> it = server.getTableNames(false).iterator();
					while (it.hasNext())
					{
						String tableName = it.next();
						Table table = (Table)server.getTable(tableName);
						if (table.getRowIdentColumnsCount() > 1) continue;//not supported

						Iterator<Column> it2 = table.getColumns().iterator();
						while (it2.hasNext())
						{
							Column c = it2.next();
							if (c.getColumnInfo() != null)
							{
								if (mainTableForeignType.equalsIgnoreCase(c.getColumnInfo().getForeignType()))
								{
									//update table set foreigntypecolumn = combinedDestinationRecordPK where foreigntypecolumn = sourceRecordPK

									QueryTable qTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
									QueryUpdate qUpdate = new QueryUpdate(qTable);

									QueryColumn qc = new QueryColumn(qTable, c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale(), c.getFlags());
									qUpdate.addValue(qc, combinedDestinationRecordPK);

									ISQLCondition condition = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, qc, sourceRecordPK);
									qUpdate.setCondition(condition);

									IDataSet pks = new BufferedDataSet();
									pks.addRow(new Object[] { ValueFactory.createTableFlushValue() });//unknown number of records changed

									SQLStatement statement = new SQLStatement(ISQLActionTypes.UPDATE_ACTION, table.getServerName(), table.getName(), pks,
										transaction_id, qUpdate, fsm.getTableFilterParams(table.getServerName(), qUpdate));

									updates.add(statement);
								}
							}
						}
					}
				}

				IDataSet pks = new BufferedDataSet();
				pks.addRow(new Object[] { sourceRecordPK });
				QueryTable qTable = new QueryTable(mainTable.getSQLName(), mainTable.getDataSource(), mainTable.getCatalog(), mainTable.getSchema());
				QueryDelete qDelete = new QueryDelete(qTable);
				QueryColumn qc = new QueryColumn(qTable, pkc.getID(), pkc.getSQLName(), pkc.getType(), pkc.getLength(), pkc.getScale(), pkc.getFlags());
				ISQLCondition condition = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, qc, sourceRecordPK);
				qDelete.setCondition(condition);
				SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, mainTable.getServerName(), mainTable.getName(), pks, transaction_id,
					qDelete, fsm.getTableFilterParams(mainTable.getServerName(), qDelete));
				statement.setExpectedUpdateCount(1); // check that the row is really deleted
				updates.add(statement);

				IFoundSetInternal sfs = sourceRecord.getParentFoundSet();
				if (combinedDestinationRecord.startEditing())
				{
					if (columnNames != null)
					{
						for (String element : columnNames)
						{
							if (element == null) continue;
							if (sfs.getSQLSheet().getColumnIndex(element) >= 0)
							{
								combinedDestinationRecord.setValue(element, sourceRecord.getValue(element));
							}
						}
					}
					fsm.getEditRecordList().stopEditing(true, combinedDestinationRecord);
				}
				else
				{
					return false;
				}

				Object[] results = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), updates.toArray(new ISQLStatement[updates.size()]));
				for (int i = 0; results != null && i < results.length; i++)
				{
					if (results[i] instanceof ServoyException)
					{
						throw (ServoyException)results[i];
					}
				}
				//sfs.deleteRecord(sfs.getRecordIndex(sourceRecord), true); not needed, will be flushed from memory in finally
				return true;
			}
			catch (Exception ex)
			{
				application.handleException(application.getI18NMessage("servoy.foundsetupdater.updateFailed"), //$NON-NLS-1$
					new ApplicationException(ServoyException.SAVE_FAILED, ex));
			}
			finally
			{
				fsm.flushCachedDatabaseData(null);
			}
		}
		return false;
	}

	/**
	 * @clonedesc js_mergeRecords(IRecordInternal,IRecordInternal,String[])
	 *
	 * @sampleas js_mergeRecords(IRecordInternal,IRecordInternal,String[])
	 *
	 * @param sourceRecord The source JSRecord to copy from.
	 * @param combinedDestinationRecord The target/destination JSRecord to copy into.
	 *
	 * @return true if the records could me merged.
	 */
	public boolean js_mergeRecords(IRecordInternal sourceRecord, IRecordInternal combinedDestinationRecord) throws ServoyException
	{
		return js_mergeRecords(sourceRecord, combinedDestinationRecord, null);
	}

	/**
	 * Returns the total number of records(rows) in a table.
	 *
	 * NOTE: This can be an expensive operation (time-wise) if your resultset is large
	 *
	 * @sample
	 * //return the total number of rows in a table.
	 * var count = databaseManager.getTableCount(foundset);
	 *
	 * @param dataSource Data where a server table can be get from. Can be a foundset, a datasource name or a JSTable.
	 *
	 * @return the total table count.
	 */
	public int js_getTableCount(Object dataSource) throws ServoyException
	{
		checkAuthorized();
		ITable table = null;
		if (dataSource instanceof IFoundSetInternal)
		{
			IFoundSetInternal foundset = (IFoundSetInternal)dataSource;
			table = foundset.getTable();
		}
		if (dataSource instanceof String)
		{
			JSTable jstable = js_getTable(dataSource.toString());
			if (jstable != null)
			{
				table = jstable.getTable();
			}
		}
		else if (dataSource instanceof JSTable)
		{
			table = ((JSTable)dataSource).getTable();
		}
		return ((FoundSetManager)application.getFoundSetManager()).getTableCount(table);
	}

	/**
	 * Switches a named server to another named server with the same datamodel (recommended to be used in an onOpen method for a solution).
	 * return true if successful.
	 * Note that this only works if source and destination server are of the same database type.
	 *
	 * @sample
	 * //dynamically changes a server for the entire solution, destination database server must contain the same tables/columns!
	 * //will fail if there is a lock, transaction , if repository_server is used or if destination server is invalid
	 * //in the solution keep using the sourceName every where to reference the server!
	 * var success = databaseManager.switchServer('crm', 'crm1')
	 *
	 * @param sourceName The name of the source database server connection
	 * @param destinationName The name of the destination database server connection.
	 *
	 * @return true if the switch could be done.
	 */
	public boolean js_switchServer(String sourceName, String destinationName) throws ServoyException
	{
		checkAuthorized();
		if (IServer.REPOSITORY_SERVER.equals(sourceName)) return false;
		if (IServer.REPOSITORY_SERVER.equals(destinationName)) return false;
		if (((FoundSetManager)application.getFoundSetManager()).hasTransaction()) return false;
		if (((FoundSetManager)application.getFoundSetManager()).hasLocks(null)) return false;
		IServer server = null;
		try
		{
			server = application.getSolution().getServer(destinationName);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		try
		{
			if (server == null || !server.isValid()) return false;
		}
		catch (RemoteException e)
		{
			Debug.error(e);
			return false;
		}

		DataServerProxy pds = application.getDataServerProxy();
		if (pds == null)
		{
			// no dataserver access yet?
			return false;
		}

		pds.switchServer(sourceName, destinationName);
		((FoundSetManager)application.getFoundSetManager()).flushCachedDatabaseData(null);//flush all
		((FoundSetManager)application.getFoundSetManager()).registerClientTables(sourceName); // register existing used tables to server
		return true;
	}


	/**
	 * Saves all outstanding (unsaved) data and exits the current record.
	 * Optionally, by specifying a record or foundset, can save a single record or all records from foundset instead of all the data.
	 *
	 * NOTE: The fields focus may be lost in user interface in order to determine the edits.
	 * 		 SaveData called from table events (like afterRecordInsert) is only partially supported depeding on how first saveData (that triggers the event) is called.
	 * 		 If first saveData is called with no arguments, all saveData from table events are returning immediatelly with true value and records will be saved as part of first save.
	 *       If first saveData is called with record(s) as arguments, saveData from table event will try to save record(s) from arguments that are different than those in first call.
	 *       SaveData with no arguments inside table events will always return true without saving anything.
	 *
	 * @sample
	 * databaseManager.saveData();
	 * //databaseManager.saveData(foundset.getRecord(1));//save specific record
	 * //databaseManager.saveData(foundset);//save all records from foundset
	 *
	 * // when creating many records in a loop do a batch save on an interval as every 10 records (to save on memory and roundtrips)
	 * // for (var recordIndex = 1; recordIndex <= 5000; recordIndex++)
	 * // {
	 * //		foundset.newRecord();
	 * //		someColumn = recordIndex;
	 * //		anotherColumn = "Index is: " + recordIndex;
	 * //		if (recordIndex % 10 == 0) databaseManager.saveData();
	 * // }
	 *
	 * @return true if the save was done without an error.
	 */
	@JSFunction
	public boolean saveData() throws ServoyException
	{
		checkAuthorized();
		EditRecordList editRecordList = application.getFoundSetManager().getEditRecordList();
		IRecordInternal[] failedRecords = editRecordList.getFailedRecords();
		for (IRecordInternal record : failedRecords)
		{
			editRecordList.startEditing(record, false);
		}
		return editRecordList.stopEditing(true) == ISaveConstants.STOPPED;
	}

	/**
	 * @clonedesc saveData()
	 *
	 * @sampleas saveData()
	 *
	 * @param foundset The JSFoundset to save.

	 * @return true if the save was done without an error.
	 */
	@JSFunction
	public boolean saveData(IJSFoundSet foundset) throws ServoyException
	{
		checkAuthorized();
		if (foundset != null)
		{
			EditRecordList editRecordList = application.getFoundSetManager().getEditRecordList();
			IRecordInternal[] failedRecords = editRecordList.getFailedRecords((IFoundSetInternal)foundset);
			for (IRecordInternal record : failedRecords)
			{
				editRecordList.startEditing(record, false);
			}
			IRecord[] editedRecords = editRecordList.getEditedRecords((IFoundSetInternal)foundset);
			return editRecordList.stopEditing(true, Arrays.asList(editedRecords)) == ISaveConstants.STOPPED;
		}
		return saveData();
	}

	/**
	 * @clonedesc saveData()
	 *
	 * @sampleas saveData()
	 *
	 * @param record The JSRecord to save.

	 * @return true if the save was done without an error.
	 */
	@JSFunction
	public boolean saveData(IJSRecord record) throws ServoyException
	{
		checkAuthorized();
		if (record != null)
		{
			EditRecordList editRecordList = application.getFoundSetManager().getEditRecordList();
			IRecordInternal[] failedRecords = editRecordList.getFailedRecords();
			if (Arrays.asList(failedRecords).contains(record))
			{
				editRecordList.startEditing((IRecordInternal)record, false);
			}
			return editRecordList.stopEditing(true, (IRecord)record) == ISaveConstants.STOPPED;
		}
		return saveData();
	}

	/**
	 * Returns a foundset object for a specified datasource or server and tablename.
	 *
	 * @sampleas getFoundSet(String)
	 *
	 * @param serverName The servername to get a JSFoundset for.
	 * @param tableName The tablename for that server
	 *
	 * @return A new JSFoundset for that datasource.
	 */
	public IJSFoundSet js_getFoundSet(String serverName, String tableName) throws ServoyException
	{
		return getFoundSet(DataSourceUtils.createDBTableDataSource(serverName, tableName));
	}

	/**
	 * Returns a foundset object for a specified datasource or server and tablename.
	 * Alternative method: datasources.db.server_name.table_name.getFoundSet() or datasources.mem['ds'].getFoundSet()
	 *
	 * @sample
	 * // type the foundset returned from the call with JSDoc, fill in the right server/tablename
	 * /** @type {JSFoundset<db:/servername/tablename>} *&#47;
	 * var fs = databaseManager.getFoundSet(controller.getDataSource())
	 * // same as datasources.db.example_data.orders.getFoundSet() or datasources.mem['myds'].getFoundSet()
	 * var ridx = fs.newRecord()
	 * var record = fs.getRecord(ridx)
	 * record.emp_name = 'John'
	 * databaseManager.saveData()
	 *
	 * @param dataSource The datasource to get a JSFoundset for.
	 *
	 * @return A new JSFoundset for that datasource.
	 */
	@JSFunction
	public IJSFoundSet getFoundSet(String dataSource) throws ServoyException
	{
		checkAuthorized();
		try
		{
			return (FoundSet)application.getFoundSetManager().getFoundSet(dataSource);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Can't get new foundset for: " + dataSource, e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a foundset object for a specified pk query.
	 *
	 * @sampleas getFoundSet(String)
	 *
	 * @param query The query to get the JSFoundset for.
	 *
	 * @return A new JSFoundset for that query.
	 */
	public FoundSet js_getFoundSet(QBSelect query) throws ServoyException
	{
		checkAuthorized();
		try
		{
			return (FoundSet)application.getFoundSetManager().getFoundSet(query);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Can't get new foundset for: " + query, e); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the next sequence for a column which has a sequence defined in its column dataprovider properties.
	 *
	 * NOTE: For more infomation on configuring the sequence for a column, see the section Auto enter options for a column from the Dataproviders chapter in the Servoy Developer User's Guide.
	 *
	 * @sample
	 * var seqDataSource = forms.seq_table.controller.getDataSource();
	 * var nextValue = databaseManager.getNextSequence(seqDataSource, 'seq_table_value');
	 * application.output(nextValue);
	 *
	 * nextValue = databaseManager.getNextSequence(databaseManager.getDataSourceServerName(seqDataSource), databaseManager.getDataSourceTableName(seqDataSource), 'seq_table_value')
	 * application.output(nextValue);
	 *
	 * @param dataSource The datasource that points to the table which has the column with the sequence,
	 * 								or the name of the server where the table can be found. If the name of the server
	 * 								is specified, then a second optional parameter specifying the name of the table
	 * 								must be used. If the datasource is specified, then the name of the table is not needed
	 * 								as the second argument.
	 * @param columnName The name of the column that has a sequence defined in its properties.
	 *
	 * @return The next sequence for the column, null if there was no sequence for that column
	 *         or if there is no column with the given name.
	 */
	public Object js_getNextSequence(String dataSource, String columnName) throws ServoyException
	{
		checkAuthorized();
		String serverName = getDataSourceServerName(dataSource);
		if (serverName != null)
		{
			String tableName = getDataSourceTableName(dataSource);
			if (tableName != null) return js_getNextSequence(serverName, tableName, columnName);
		}
		return null;
	}

	/**
	 *
	 * @param serverName The datasource that points to the table which has the column with the sequence,
	 * 								or the name of the server where the table can be found. If the name of the server
	 * 								is specified, then a second optional parameter specifying the name of the table
	 * 								must be used. If the datasource is specified, then the name of the table is not needed
	 * 								as the second argument.
	 * @param tableName The name of the table that has the column with the sequence. Use this parameter
	 * 							only if you specified the name of the server as the first parameter.
	 * @param columnName The name of the column that has a sequence defined in its properties.
	 *
	 * @return The next sequence for the column, null if there was no sequence for that column
	 *         or if there is no column with the given name.
	 * @deprecated Use getNextSequence(datasource,column)
	 */
	@Deprecated
	public Object js_getNextSequence(String serverName, String tableName, String columnName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			IServer server = application.getRepository().getServer(serverName);
			if (server == null) return null;

			Table table = (Table)server.getTable(tableName);
			if (table == null) return null;

			int columnInfoID = table.getColumnInfoID(columnName);
			if (columnInfoID == -1) return null;

			return application.getDataServer().getNextSequence(serverName, tableName, columnName, columnInfoID);
		}
		catch (Exception e)
		{
			Debug.error(e);
			return null;
		}
	}

	/**
	 * Returns an array with all the server names used in the solution.
	 *
	 * NOTE: For more detail on named server connections, see the chapter on Database Connections, beginning with the Introduction to database connections in the Servoy Developer User's Guide.
	 *
	 * @sample var array = databaseManager.getServerNames()
	 *
	 * @return An Array of servernames.
	 */
	public String[] js_getServerNames() throws ServoyException
	{
		checkAuthorized();
		//we use flattensolution to be sure we also take the combined server proxies from modules (which are combined in flatten solution)
		Map<String, IServer> sp = application.getFlattenedSolution().getSolution().getServerProxies();
		if (sp != null)
		{
			synchronized (sp)
			{
				return sp.keySet().toArray(new String[sp.size()]);
			}
		}
		return new String[0];
	}

	/**
	 * Retrieves a list with names of all database servers that have property DataModelCloneFrom equal to the server name parameter.
	 *
	 * @sample
	 * 	var serverNames = databaseManager.getDataModelClonesFrom('myServerName');
	 *
	 * @param serverName
	 */
	@JSFunction
	public String[] getDataModelClonesFrom(String serverName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			IServer server = application.getSolution().getServer(serverName);
			if (server != null)
			{
				return server.getDataModelClonesFrom();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Returns the database product name as supplied by the driver for a server.
	 *
	 * NOTE: For more detail on named server connections, see the chapter on Database Connections, beginning with the Introduction to database connections in the Servoy Developer User's Guide.
	 *
	 * @sample var databaseProductName = databaseManager.getDatabaseProductName(servername)
	 *
	 * @param serverName The specified name of the database server connection.
	 *
	 * @return A database product name.
	 */
	public String js_getDatabaseProductName(String serverName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			IServer s = application.getSolution().getServer(serverName);
			if (s != null)
			{
				return s.getDatabaseProductName();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Returns an array of all table names for a specified server.
	 *
	 * @sample
	 * //return all the table names as array
	 * var tableNamesArray = databaseManager.getTableNames('user_data');
	 * var firstTableName = tableNamesArray[0];
	 *
	 * @param serverName The server name to get the table names from.
	 *
	 * @return An Array with the tables names of that server.
	 */
	public String[] js_getTableNames(String serverName) throws ServoyException
	{
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).getTableNames(serverName);
	}

	/**
	 * Returns an array of all view names for a specified server.
	 *
	 * @sample
	 * //return all the view names as array
	 * var viewNamesArray = databaseManager.getViewNames('user_data');
	 * var firstViewName = viewNamesArray[0];
	 *
	 * @param serverName The server name to get the view names from.
	 *
	 * @return An Array with the view names of that server.
	 */
	public String[] js_getViewNames(String serverName) throws ServoyException
	{
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).getViewNames(serverName);
	}

	/**
	 * Returns true if the current client has any or the specified lock(s) acquired.
	 *
	 * @sample var hasLocks = databaseManager.hasLocks('mylock')
	 *
	 * @param lockName The lock name to check.
	 *
	 * @return true if the current client has locks or the lock.
	 */
	public boolean js_hasLocks(String lockName) throws ServoyException
	{
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).hasLocks(lockName);
	}

	/**
	 * @clonedesc js_hasLocks(String)
	 *
	 * @sampleas js_hasLocks(String)
	 *
	 * @return true if the current client has locks or the lock.
	 */

	public boolean js_hasLocks() throws ServoyException
	{
		return js_hasLocks(null);
	}

	/**
	 * Release all current locks the client has (optionally limited to named locks).
	 * return true if the locks are released.
	 *
	 * @sample databaseManager.releaseAllLocks('mylock')
	 *
	 * @return true if all locks or the lock is released.
	 */
	public boolean js_releaseAllLocks() throws ServoyException
	{
		checkAuthorized();
		return js_releaseAllLocks(null);
	}

	/**
	 * @clonedesc js_releaseAllLocks()
	 *
	 * @sampleas js_releaseAllLocks()
	 *
	 * @param lockName The lock name to release.
	 *
	 * @return  true if all locks or the lock is released.
	 */
	public boolean js_releaseAllLocks(String lockName) throws ServoyException
	{
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).releaseAllLocks(lockName);
	}

	/*
	 * _____________________________________________________________ transaction methods
	 */

	/**
	 * Returns true if a transaction is committed; rollback if commit fails.
	 *
	 * @param saveFirst save edited records to the database first (default true)
	 * @param revertSavedRecords if a commit fails and a rollback is done, the when given false the records are not reverted to the database state (and are in edited records again)
	 *
	 * @sampleas js_startTransaction()
	 *
	 * @return if the transaction could be committed.
	 */
	public boolean js_commitTransaction(boolean saveFirst, boolean revertSavedRecords) throws ServoyException
	{
		checkAuthorized();
		IFoundSetManagerInternal fsm = application.getFoundSetManager();
		return fsm.commitTransaction(saveFirst, revertSavedRecords);
	}

	/**
	 * Returns true if a transaction is committed; rollback if commit fails.
	 *
	 * @param saveFirst save edited records to the database first (default true)
	 *
	 * @sampleas js_startTransaction()
	 *
	 * @return if the transaction could be committed.
	 */
	public boolean js_commitTransaction(boolean saveFirst) throws ServoyException
	{
		checkAuthorized();
		IFoundSetManagerInternal fsm = application.getFoundSetManager();
		return fsm.commitTransaction(saveFirst, true);
	}

	/**
	 * Returns true if a transaction is committed; rollback if commit fails.
	 * Saves all edited records and commits the data.
	 *
	 * @sampleas js_startTransaction()
	 *
	 * @return if the transaction could be committed.
	 */
	public boolean js_commitTransaction() throws ServoyException
	{
		return js_commitTransaction(true);
	}

	/**
	 * Rollback a transaction started by databaseManager.startTransaction().
	 * Note that when autosave is false, revertEditedRecords() will not handle deleted records, while rollbackTransaction() does.
	 * Also, saved records within the transactions are restored to the database values, so user input is lost, to controll this see rollbackTransaction(boolean,boolean)
	 *
	 * @param rollbackEdited call rollbackEditedRecords() before rolling back the transaction
	 *
	 * @sampleas js_startTransaction()
	 */
	public void js_rollbackTransaction(boolean rollbackEdited) throws ServoyException
	{
		checkAuthorized();
		IFoundSetManagerInternal fsm = application.getFoundSetManager();
		fsm.rollbackTransaction(rollbackEdited, true, true);
	}

	/**
	 * Rollback a transaction started by databaseManager.startTransaction().
	 * Note that when autosave is false, revertEditedRecords() will not handle deleted records, while rollbackTransaction() does.
	 *
	 * @param rollbackEdited call rollbackEditedRecords() before rolling back the transaction
	 * @param revertSavedRecords if false then all records in the transaction do keep the user input and are back in the edited records list.
	 * Note that if the pks of such a record are no longer used by it's foundset (find/search or load by query or ...) it will just be rolled-back as
	 * it can't be put in editing records list.
	 *
	 * @sampleas js_startTransaction()
	 */
	public void js_rollbackTransaction(boolean rollbackEdited, boolean revertSavedRecords) throws ServoyException
	{
		checkAuthorized();
		IFoundSetManagerInternal fsm = application.getFoundSetManager();
		fsm.rollbackTransaction(rollbackEdited, true, revertSavedRecords);
	}

	/**
	 * Rollback a transaction started by databaseManager.startTransaction().
	 * Note that when autosave is false, revertEditedRecords() will not handle deleted records, while rollbackTransaction() does.
	 * Also, rollbackEditedRecords() is called before rolling back the transaction see rollbackTransaction(boolean) to controll that behavior
	 * and saved records within the transactions are restored to the database values, so user input is lost, to control this see rollbackTransaction(boolean,boolean)
	 *
	 * @sampleas js_startTransaction()
	 */
	public void js_rollbackTransaction() throws ServoyException
	{
		js_rollbackTransaction(true);
	}

	/**
	 * Start a database transaction.
	 * If you want to avoid round trips to the server or avoid the posibility of blocking other clients
	 * because of your pending changes, you can use databaseManager.setAutoSave(false/true) and databaseManager.rollbackEditedRecords().
	 *
	 * startTransaction, commit/rollbackTransacton() does support rollbacking of record deletes which autoSave = false doesnt support.
	 *
	 * @sample
	 * // starts a database transaction
	 * databaseManager.startTransaction()
	 * //Now let users input data
	 *
	 * //when data has been entered do a commit or rollback if the data entry is canceld or the the commit did fail.
	 * if (cancel || !databaseManager.commitTransaction())
	 * {
	 * 	databaseManager.rollbackTransaction();
	 * }
	 */
	public void js_startTransaction() throws ServoyException
	{
		checkAuthorized();
		application.getFoundSetManager().startTransaction();
	}

	/**
	 * Enable/disable the default null validator for non null columns, makes it possible todo the checks later on when saving, when for example autosave is disabled.
	 *
	 * @sample
	 * databaseManager.nullColumnValidatorEnabled = false;//disable
	 *
	 * //test if enabled
	 * if(databaseManager.nullColumnValidatorEnabled) application.output('null validation enabled')
	 */
	public boolean js_getNullColumnValidatorEnabled()
	{
		return ((FoundSetManager)application.getFoundSetManager()).isNullColumnValidatorEnabled();
	}

	public void js_setNullColumnValidatorEnabled(boolean enable)
	{
		((FoundSetManager)application.getFoundSetManager()).setNullColumnValidatorEnabled(enable);
	}

	/**
	 * Set autosave, if false then no saves will happen by the ui (not including deletes!).
	 * Until you call databaseManager.saveData() or setAutoSave(true)
	 *
	 * If you also want to be able to rollback deletes then you have to use databaseManager.startTransaction().
	 * Because even if autosave is false deletes of records will be done.
	 *
	 * @sample
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 *
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * databaseManager.setAutoSave(true)
	 *
	 * @param autoSave Boolean to enable or disable autosave.
	 *
	 * @return false if the current edited record could not be saved.
	 */
	@JSFunction
	public boolean setAutoSave(boolean autoSave)
	{
		if (Debug.tracing())
		{
			if (autoSave) Debug.trace("Auto save enable"); //$NON-NLS-1$
			else Debug.trace("Auto save disabled"); //$NON-NLS-1$
		}
		return ((FoundSetManager)application.getFoundSetManager()).getEditRecordList().setAutoSave(autoSave);
	}

	/**
	 * Returns true or false if autosave is enabled or disabled.
	 *
	 * @sample
	 * //Set autosave, if false then no saves will happen by the ui (not including deletes!). Until you call saveData or setAutoSave(true)
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 *
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * databaseManager.setAutoSave(true)
	 *
	 * @return true if autosave if enabled.
	 */
	@JSFunction
	public boolean getAutoSave()
	{
		return application.getFoundSetManager().getEditRecordList().getAutoSave();
	}

	/**
	 * Turnoff the initial form foundset record loading, set this in the solution open method.
	 * Simular to calling foundset.clear() in the form's onload event.
	 *
	 * NOTE: When the foundset record loading is turned off, controller.find or controller.loadAllRecords must be called to display the records
	 *
	 * @sample
	 * //this has to be called in the solution open method
	 * databaseManager.setCreateEmptyFormFoundsets()
	 */
	public void js_setCreateEmptyFormFoundsets()
	{
		((FoundSetManager)application.getFoundSetManager()).createEmptyFormFoundsets();
	}

	/**
	 * Rolls back in memory edited records that are outstanding (not saved).
	 * Can specify a record or foundset as parameter to rollback.
	 * Best used in combination with the function databaseManager.setAutoSave()
	 * This does not include deletes, they do not honor the autosafe false flag so they cant be rollbacked by this call.
	 *
	 * @deprecated  As of release 6.1, renamed to {@link #revertEditedRecords()}.
	 *
	 * @sample
	 * //Set autosave, if false then no saves will happen by the ui (not including deletes!). Until you call saveData or setAutoSave(true)
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 *
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * //databaseManager.rollbackEditedRecords(foundset); // rollback all records from foundset
	 * //databaseManager.rollbackEditedRecords(foundset.getSelectedRecord()); // rollback only one record
	 * databaseManager.setAutoSave(true)
	 */
	@Deprecated
	public void js_rollbackEditedRecords() throws ServoyException
	{
		js_revertEditedRecords();
	}

	/**
	 * @clonedesc js_rollbackEditedRecords()
	 *
	 * @deprecated  As of release 6.1, renamed to {@link #revertEditedRecords()}.
	 *
	 * @sampleas js_rollbackEditedRecords()
	 *
	 * @param foundset A JSFoundset to rollback.
	 */
	@Deprecated
	public void js_rollbackEditedRecords(IFoundSetInternal foundset) throws ServoyException
	{
		js_revertEditedRecords(foundset);
	}

	/**
	 * @clonedesc js_rollbackEditedRecords()
	 *
	 * @deprecated  As of release 6.1, renamed to {@link #revertEditedRecords()}.
	 *
	 * @sampleas js_rollbackEditedRecords()
	 *
	 * @param record A JSRecord to rollback.
	 */
	@Deprecated
	public void js_rollbackEditedRecords(IRecordInternal record) throws ServoyException
	{
		js_revertEditedRecords(record);
	}

	/**
	 * Reverts outstanding (not saved) in memory changes from edited records.
	 * Can specify a record or foundset as parameter to rollback.
	 * Best used in combination with the function databaseManager.setAutoSave()
	 * This does not include deletes, they do not honor the autosafe false flag so they cant be rollbacked by this call.
	 *
	 *
	 * @sample
	 * //Set autosave, if false then no saves will happen by the ui (not including deletes!). Until you call saveData or setAutoSave(true)
	 * //reverts in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 *
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.revertEditedRecords()
	 * //databaseManager.revertEditedRecords(foundset); // rollback all records from foundset
	 * //databaseManager.revertEditedRecords(foundset.getSelectedRecord()); // rollback only one record
	 * databaseManager.setAutoSave(true)
	 */
	public void js_revertEditedRecords() throws ServoyException
	{
		checkAuthorized();
		application.getFoundSetManager().getEditRecordList().rollbackRecords();
	}

	/**
	 * @clonedesc js_revertEditedRecords()
	 *
	 * @sampleas js_revertEditedRecords()
	 *
	 * @param foundset A JSFoundset to revert.
	 */
	public void js_revertEditedRecords(IFoundSetInternal foundset) throws ServoyException
	{
		checkAuthorized();
		if (foundset != null)
		{
			List<IRecordInternal> records = new ArrayList<IRecordInternal>();
			records.addAll(Arrays.asList(application.getFoundSetManager().getEditRecordList().getEditedRecords(foundset)));
			records.addAll(Arrays.asList(application.getFoundSetManager().getEditRecordList().getFailedRecords(foundset)));
			if (records.size() > 0) application.getFoundSetManager().getEditRecordList().rollbackRecords(records);
		}
	}

	/**
	 * @clonedesc js_revertEditedRecords()
	 *
	 * @sampleas js_revertEditedRecords()
	 *
	 * @param record A JSRecord to rollback.
	 *
	 * @deprecated see JSRecord#revertChanges()
	 */
	@Deprecated
	public void js_revertEditedRecords(IRecordInternal record) throws ServoyException
	{
		checkAuthorized();
		if (record != null)
		{
			List<IRecordInternal> records = new ArrayList<IRecordInternal>();
			records.add(record);
			application.getFoundSetManager().getEditRecordList().rollbackRecords(records);
		}
	}

	/**
	 * Returns true if there is an transaction active for this client.
	 *
	 * @sample var hasTransaction = databaseManager.hasTransaction()
	 *
	 * @return true if the client has a transaction.
	 */
	public boolean js_hasTransaction()
	{
		return application.getFoundSetManager().hasTransaction();
	}

	/*
	 * _____________________________________________________________ helper methods methods
	 */

	/**
	 * Returns true if the (related)foundset exists and has records.
	 *
	 * @sample
	 * if (%%elementName%%.hasRecords(orders_to_orderitems))
	 * {
	 * 	//do work on relatedFoundSet
	 * }
	 * //if (%%elementName%%.hasRecords(foundset.getSelectedRecord(),'orders_to_orderitems.orderitems_to_products'))
	 * //{
	 * //	//do work on deeper relatedFoundSet
	 * //}
	 *
	 * @param foundset A JSFoundset to test.
	 *
	 * @return true if the foundset/relation has records.
	 */
	@JSFunction
	public boolean hasRecords(IJSFoundSet foundset)
	{
		if (foundset != null)
		{
			return foundset.getSize() > 0;
		}
		return false;
	}

	/**
	 * @clonedesc hasRecords(IJSFoundSet)
	 *
	 * @sampleas hasRecords(IJSFoundSet)
	 *
	 * @param record A JSRecord to test.
	 * @param relationString The relation name.
	 *
	 * @return true if the foundset/relation has records.
	 */
	@JSFunction
	public boolean hasRecords(IJSRecord record, String relationString)
	{
		return JSDatabaseManager.hasRecords((IRecordInternal)record, relationString);
	}

	public static boolean hasRecords(IRecordInternal record, String relationString)
	{
		if (record != null)
		{
			boolean retval = false;
			String relatedFoundSets = relationString;
			StringTokenizer tk = new StringTokenizer(relatedFoundSets, "."); //$NON-NLS-1$
			while (tk.hasMoreTokens())
			{
				String relationName = tk.nextToken();
				IFoundSetInternal rfs = record.getRelatedFoundSet(relationName);
				if (rfs != null && rfs.getSize() > 0)
				{
					retval = true;
					record = rfs.getRecord(0);
				}
				else
				{
					retval = false;
					break;
				}
			}
			return retval;
		}
		return false;
	}

	/**
	 * Returns true if the specified foundset, on a specific index or in any of its records, or the specified record has changes.
	 *
	 * NOTE: The fields focus may be lost in user interface in order to determine the edits.
	 *
	 * @sample
	 * if (databaseManager.hasRecordChanges(foundset,2))
	 * {
	 * 	//do save or something else
	 * }
	 *
	 * @param foundset The JSFoundset to test if it has changes.
	 * @param index The record index in the foundset to test (not specified means has the foundset any changed records)
	 *
	 * @return true if there are changes in the JSFoundset or JSRecord.
	 */
	public boolean js_hasRecordChanges(IFoundSetInternal foundset, Number index)
	{
		if (foundset == null) return false;
		int _index = Utils.getAsInteger(index);
		IRecordInternal rec = null;
		if (_index > 0)
		{
			rec = foundset.getRecord(_index - 1);
		}
		else
		{
			EditRecordList el = application.getFoundSetManager().getEditRecordList();
			el.removeUnChangedRecords(true, false);
			// first the quick way of testing the foundset itself.
			if (el.hasEditedRecords(foundset))
			{
				return true;
			}
			// if not found then look if other foundsets had record(s) that are changed that also are in this foundset.
//				String ds = foundset.getDataSource();
//				IRecordInternal[] editedRecords = el.getEditedRecords();
//				for (IRecordInternal editedRecord : editedRecords)
//				{
//					IRecordInternal record = editedRecord;
//					if (record.getRawData() != null && !record.existInDataSource())
//					{
//						if (record.getParentFoundSet().getDataSource().equals(ds))
//						{
//							if (foundset.getRecord(record.getPK()) != null)
//							{
//								return true;
//							}
//						}
//					}
//				}
		}
		return js_hasRecordChanges(rec);
	}

	/**
	 * @clonedesc js_hasRecordChanges(IFoundSetInternal,Number)
	 *
	 * @sampleas js_hasRecordChanges(IFoundSetInternal,Number)
	 *
	 * @param foundset The JSFoundset to test if it has changes.
	 *
	 * @return true if there are changes in the JSFoundset or JSRecord.
	 */
	public boolean js_hasRecordChanges(IFoundSetInternal foundset)
	{
		return js_hasRecordChanges(foundset, Integer.valueOf(-1));
	}

	/**
	 * @clonedesc js_hasRecordChanges(IFoundSetInternal,Number)
	 *
	 * @sampleas js_hasRecordChanges(IFoundSetInternal,Number)
	 *
	 * @param record The JSRecord to test if it has changes.
	 *
	 * @return true if there are changes in the JSFoundset or JSRecord.
	 *
	 * @deprecated use JSRecord#hasChangedData() instead
	 */
	@Deprecated
	public boolean js_hasRecordChanges(IRecordInternal record)
	{
		if (record != null && record.getRawData() != null)
		{
			return record.getRawData().isChanged();
		}
		return false;
	}

	/**
	 * Returns true if the argument (foundSet / record) has at least one row that was not yet saved in the database.
	 *
	 * @sample
	 * var fs = databaseManager.getFoundSet(databaseManager.getDataSourceServerName(controller.getDataSource()),'employees');
	 * databaseManager.startTransaction();
	 * var ridx = fs.newRecord();
	 * var record = fs.getRecord(ridx);
	 * record.emp_name = 'John';
	 * if (databaseManager.hasNewRecords(fs)) {
	 * 	application.output("new records");
	 * } else {
	 * 	application.output("no new records");
	 * }
	 * databaseManager.saveData();
	 * databaseManager.commitTransaction();
	 *
	 * @param foundset The JSFoundset to test.
	 * @param index The record index in the foundset to test (not specified means has the foundset any new records)
	 *
	 * @return true if the JSFoundset has new records or JSRecord is a new record.
	 */
	public boolean js_hasNewRecords(IFoundSetInternal foundset, Number index)
	{
		if (foundset == null) return false;
		int _index = Utils.getAsInteger(index);
		IRecordInternal rec = null;
		if (_index > 0)
		{
			rec = foundset.getRecord(_index - 1);
		}
		else
		{
			EditRecordList el = application.getFoundSetManager().getEditRecordList();
			// fist test quickly for this foundset only.
			IRecordInternal[] editedRecords = el.getEditedRecords(foundset, true);
			for (IRecordInternal editedRecord : editedRecords)
			{
				IRecordInternal record = editedRecord;
				if (record.getRawData() != null && !record.existInDataSource())
				{
					return true;
				}
			}
			// if not found then look if other foundsets had record(s) that are new that also are in this foundset.
			String ds = foundset.getDataSource();
			editedRecords = el.getEditedRecords();
			for (IRecordInternal editedRecord : editedRecords)
			{
				IRecordInternal record = editedRecord;
				if (record.getRawData() != null && !record.existInDataSource())
				{
					if (record.getParentFoundSet().getDataSource().equals(ds))
					{
						if (foundset.getRecord(record.getPK()) != null)
						{
							return true;
						}
					}
				}
			}

		}

		return js_hasNewRecords(rec);
	}

	/**
	 * @clonedesc js_hasNewRecords(IFoundSetInternal,Number)
	 *
	 * @sampleas js_hasNewRecords(IFoundSetInternal,Number)
	 *
	 * @param foundset The JSFoundset to test.
	 *
	 * @return true if the JSFoundset has new records or JSRecord is a new record.
	 */
	public boolean js_hasNewRecords(IFoundSetInternal foundset)
	{
		return js_hasNewRecords(foundset, Integer.valueOf(-1));
	}

	/**
	 * @clonedesc js_hasNewRecords(IFoundSetInternal,Number)
	 *
	 * @sampleas js_hasNewRecords(IFoundSetInternal,Number)
	 *
	 * @param record The JSRecord to test.
	 *
	 * @return true if the JSFoundset has new records or JSRecord is a new record.
	 *
	 * @deprecated use JSRecord#isNew() instead
	 */
	@Deprecated
	public boolean js_hasNewRecords(IRecordInternal record)
	{
		if (record != null && record.getRawData() != null)
		{
			return !record.existInDataSource();
		}
		return false;
	}

	/**
	 * Copies all matching non empty columns (if overwrite boolean is given all columns except pk/ident, if array then all columns except pk and array names).
	 * returns true if no error did happen.
	 *
	 * NOTE: This function could be used to store a copy of records in an archive table. Use the getRecord() function to get the record as an object.
	 *
	 * @deprecated  As of release 6.0, replaced by {@link #copyMatchingFields(Object[])}
	 *
	 * @sample
	 * for( var i = 1 ; i <= foundset.getSize() ; i++ )
	 * {
	 * 	var srcRecord = foundset.getRecord(i);
	 * 	var destRecord = otherfoundset.getRecord(i);
	 * 	if (srcRecord == null || destRecord == null) break;
	 * 	databaseManager.copyMatchingColumns(srcRecord,destRecord,true)
	 * }
	 * //saves any outstanding changes to the dest foundset
	 * databaseManager.saveData();
	 *
	 * @param src The source record or object to be copied.
	 * @param dest_record The destination record to copy to.
	 * @param overwrite/array_of_names_not_overwritten optional true (default false) if everything can be overwritten or an array of names that shouldnt be overwritten.
	 *
	 * @return true if no errors happend.
	 */
	@Deprecated
	public boolean js_copyMatchingColumns(Object[] values) throws ServoyException
	{
		Object src = values[0];
		Object dest = values[1];
		List<Object> al = new ArrayList<Object>();
		boolean overwrite = false;
		if (values.length > 2)
		{
			if (values[2] instanceof Boolean)
			{
				overwrite = ((Boolean)values[2]).booleanValue();
			}
			else if (values[2].getClass().isArray())
			{
				al = Arrays.asList((Object[])values[2]);
			}
		}
		return copyMatchingFields(src, (IRecordInternal)dest, overwrite, al.toArray());
	}

	public boolean copyMatchingFields(Object src, IRecordInternal dest, boolean overwrite, Object[] names) throws ServoyException
	{
		checkAuthorized();
		List<Object> al = new ArrayList<Object>();
		if (names != null)
		{
			al = Arrays.asList(names);
		}
		try
		{
			SQLSheet destSheet = dest.getParentFoundSet().getSQLSheet();
			Table dest_table = destSheet.getTable();
			boolean wasEditing = dest.isEditing();
			Map<String, Method> getters = new HashMap<String, Method>();

			if (dest.startEditing())
			{
				Iterator<Column> it = dest_table.getColumns().iterator();
				while (it.hasNext())
				{
					Column c = it.next();
					ColumnInfo ci = c.getColumnInfo();
					if (ci != null && ci.isExcluded())
					{
						continue;
					}

					if (al.contains(c.getDataProviderID()))
					{
						// skip, also if value in dest_rec is null
						continue;
					}

					Object dval = dest.getValue(c.getDataProviderID());
					if (dval == null || (!dest_table.getRowIdentColumns().contains(c) && (overwrite || (al.size() > 0 && !al.contains(c.getDataProviderID())))))
					{
						if (src instanceof IRecordInternal)
						{
							IRecordInternal src_rec = (IRecordInternal)src;
							int index = src_rec.getParentFoundSet().getSQLSheet().getColumnIndex(c.getDataProviderID());
							if (index != -1)
							{
								Object sval = src_rec.getValue(c.getDataProviderID());
								try
								{
									int type = c.getType();
									if (ci.getConverterName() != null && ci.getConverterName().trim().length() != 0)
									{
										IColumnConverter columnConverter = ((FoundSetManager)application.getFoundSetManager()).getColumnConverterManager().getConverter(
											ci.getConverterName());
										if (columnConverter instanceof ITypedColumnConverter)
										{
											try
											{
												int convType = ((ITypedColumnConverter)columnConverter).getToObjectType(
													ComponentFactory.<String> parseJSonProperties(ci.getConverterProperties()));
												if (convType != Integer.MAX_VALUE)
												{
													type = Column.mapToDefaultType(convType);
												}
											}
											catch (IOException e)
											{
												Debug.error("Exception loading properties for converter " + columnConverter.getName() + ", properties: " +
													ci.getConverterProperties(), e);
											}
										}
									}
									dest.setValue(c.getDataProviderID(), Column.getAsRightType(type, c.getFlags(), sval, c.getLength(), true));
								}
								catch (Exception e)
								{
									application.reportJSError("Could not copy matching field to " + dest_table.getName() + "." + c.getDataProviderID() +
										". The value: '" + sval + "' does not match the type of the destination.", e);
								}
							}
						}
						else if (src instanceof NativeObject)
						{
							NativeObject no = ((NativeObject)src);
							if (no.has(c.getDataProviderID(), no))
							{
								Object raw_val = no.get(c.getDataProviderID(), no);
								Object val = c.getAsRightType(raw_val);
								dest.setValue(c.getDataProviderID(), val);
							}
						}
						else if (src != null)
						{
							Method m = getMethod(src, c.getDataProviderID(), getters);
							if (m != null)
							{
								Object raw_val = m.invoke(src, (Object[])null);
								Object val = c.getAsRightType(raw_val);
								dest.setValue(c.getDataProviderID(), val);
							}
						}
					}
				}

				if (!wasEditing)
				{
					dest.stopEditing();
				}
				return true;
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	/**
	 * Copies all matching non empty columns (if overwrite boolean is given all columns except pk/ident, if array then all columns except pk and array names).
	 * The matching requires the properties and getter functions of the source to match those of the destination; for the getter functions,
	 * the 'get' will be removed and the remaining name will be converted to lowercase before attempting to match.
	 * Returns true if no error occurred.
	 *
	 * NOTE: This function could be used to store a copy of records in an archive table. Use the getRecord() function to get the record as an object.
	 * Before trying this example, please make sure that the foundsets have some records loaded:
	 * @sample
	 * otherfoundset.loadAllRecords();
	 * for( var i = 1 ; i <= foundset.getSize() ; i++ )
	 * {
	 * 	var srcRecord = foundset.getRecord(i);
	 * 	var destRecord = otherfoundset.getRecord(i);
	 * 	if (srcRecord == null || destRecord == null) break;
	 * 	databaseManager.copyMatchingFields(srcRecord,destRecord,true)
	 * }
	 * //saves any outstanding changes to the dest foundset
	 * databaseManager.saveData();
	 *
	 * //copying from a MailMessage JavaScript object
	 * //var _msg = plugins.mail.receiveMail(login, password, true, 0, null, properties);
	 * //if (_msg != null)
	 * //{
	 * //	controller.newRecord();
	 * //	var srcObject = _msg[0];
	 * //	var destRecord = foundset.getSelectedRecord();
	 * //	databaseManager.copyMatchingFields(srcObject, destRecord, true);
	 * //	databaseManager.saveData();
	 * //}
	 *
	 * @param source The source record or (java/javascript)object to be copied.
	 * @param destination The destination record to copy to.
	 *
	 * @return true if no errors happened.
	 */
	public boolean js_copyMatchingFields(Object source, IRecordInternal destination) throws ServoyException
	{
		return js_copyMatchingFields(source, destination, Boolean.FALSE);
	}

	/**
	 * @clonedesc js_copyMatchingFields(Object,IRecordInternal)
	 *
	 * @sampleas js_copyMatchingFields(Object,IRecordInternal)
	 *
	 * @param source The source record or (java/javascript)object to be copied.
	 * @param destination The destination record to copy to.
	 * @param overwrite Boolean values to overwrite all values. If overwrite is false/not provided, then the non empty values are not overwritten in the destination record.
	 * @return true if no errors happened.
	 */
	public boolean js_copyMatchingFields(Object source, IRecordInternal destination, Boolean overwrite) throws ServoyException
	{
		boolean _overwrite = Utils.getAsBoolean(overwrite);
		return copyMatchingFields(source, destination, _overwrite, null);
	}

	/**
	 * @clonedesc js_copyMatchingFields(Object,IRecordInternal)
	 *
	 * @sampleas js_copyMatchingFields(Object,IRecordInternal)
	 *
	 * @param source The source record or (java/javascript)object to be copied.
	 * @param destination The destination record to copy to.
	 * @param names The property names that shouldn't be overriden.

	 * @return true if no errors happened.
	 */
	public boolean js_copyMatchingFields(Object source, IRecordInternal destination, String[] names) throws ServoyException
	{
		return copyMatchingFields(source, destination, false, names);
	}

	/**
	 * Add tracking info used in the log table.
	 * When tracking is enabled and a new row is inserted in the log table,
	 * if it has a column named 'columnName', its value will be set with 'value'
	 *
	 * @sample databaseManager.addTrackingInfo('log_column_name', 'trackingInfo')
	 *
	 * @param columnName The name of the column in the log table, used for tracking info
	 * @param value The value to be set when inserting a new row in the log table, for the 'columnName' column
	 */
	public void js_addTrackingInfo(String columnName, Object value)
	{
		application.getFoundSetManager().addTrackingInfo(columnName, value);
	}


	/**
	 * @deprecated  As of release 5.0, replaced by {@link JSDataSet#createDataSource(String, Object)}
	 */
	@Deprecated
	public String js_createDataSource(Object[] args) throws ServoyException
	{
		checkAuthorized();
		if (args.length >= 3)
		{
			String name = String.valueOf(args[0]);
			if (args[1] instanceof IDataSet && args[2] instanceof Object[])
			{

				int[] intTypes = new int[((Object[])args[2]).length];
				for (int i = 0; i < ((Object[])args[2]).length; i++)
				{
					intTypes[i] = Utils.getAsInteger(((Object[])(args[2]))[i]);
				}
				try
				{
					return application.getFoundSetManager().createDataSourceFromDataSet(name, (IDataSet)args[1], intTypes, null);
				}
				catch (ServoyException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		return null;
	}

	/**
	 * Free resources allocated for a previously created data source
	 *
	 * @deprecated Deprecated as of release 5.0, not needed anymore.
	 *
	 * @sample databaseManager.removeDataSource(uri);
	 *
	 * @param uri
	 */
	@Deprecated
	public boolean js_removeDataSource(String uri)
	{
		try
		{
			return application.getFoundSetManager().removeDataSource(uri);
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		return false;
	}

	/**
	 * Create a QueryBuilder object for a datasource.
	 * @sample
	 *
	 * /** @type {QBSelect<db:/example_data/book_nodes>} *&#47;
	 * var q = databaseManager.createSelect('db:/example_data/book_nodes');
	 * q.result.addPk()
	 * q.where.add(q.columns.label_text.not.isin(null))
	 * datasources.db.example_data.book_nodes.getFoundSet().loadRecords(q)
	 *
	 * @param dataSource The data source to build a query for.
	 *
	 * @return query builder
	 */
	public QBSelect js_createSelect(String dataSource) throws ServoyException
	{
		return (QBSelect)application.getFoundSetManager().getQueryFactory().createSelect(dataSource);
	}

	/**
	 * Create a QueryBuilder object for a datasource with given table alias.
	 * The alias can be used inside custom queries to bind to the outer table.
	 * @sample
	 *
	 * /** @type {QBSelect<db:/example_data/book_nodes>} *&#47;
	 * var q = databaseManager.createSelect('db:/example_data/book_nodes', 'b');
	 * q.result.addPk()
	 * q.where.add(q.columns.label_text.isin('select comment_text from book_text t where t.note_text = ? and t.node_id = b.node_id', ['test']))
	 * datasources.db.example_data.book_nodes.getFoundSet().loadRecords(q)
	 *
	 * @param dataSource The data source to build a query for.
	 * @param tableAlias The alias for the main table.
	 *
	 * @return query builder
	 */
	public QBSelect js_createSelect(String dataSource, String tableAlias) throws ServoyException
	{
		return (QBSelect)application.getFoundSetManager().getQueryFactory().createSelect(dataSource, tableAlias);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "DatabaseManager"; //$NON-NLS-1$
	}

	public void destroy()
	{
		application = null;
	}

}
