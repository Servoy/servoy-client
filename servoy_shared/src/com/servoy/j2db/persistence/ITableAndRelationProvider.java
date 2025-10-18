/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.SortOptions;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.util.ServoyException;

/**
 * Table and relation lookup.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ITableAndRelationProvider
{
	/**
	 * Get a table object interface for a datasource
	 *
	 * @param dataSource the datasource
	 * @return the table interface
	 */
	ITable getTable(String dataSource) throws RepositoryException;

	/**
	 * Get relation by name for current solution
	 * @param relationName
	 * @return
	 */
	IRelation getRelation(String relationName);

	/**
	 * Find the data source of the table with given sql name in same server as serverDataSource
	 * @param serverDataSource
	 * @param tableSQLName
	 */
	String resolveDataSource(String serverDataSource, String tableSQLName);

	/** RAGTEST doc
	 * Find the data source of the table with given sql name in same server as serverDataSource
	 * @param serverDataSource
	 * @param tableSQLName
	 */
	IColumn resolveColumn(QueryColumn qColumn);

	/**
	 * Get the QuerySet for a QuerySelect select.
	 *
	 * @param select the QuerySelect
	 * @param includeFilters include table filters option
	 */
	QuerySet getQuerySet(QuerySelect select, boolean includeFilters) throws RepositoryException;

	IApplication getApplication();

	/**
	 * Get a new foundset for the query.
	 * @since 6.1
	 */
	IFoundSet getFoundSet(IQueryBuilder query) throws ServoyException;

	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * @param query IQueryBuilder query.
	 * @param useTableFilters use table filters (default true).
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The IDataSet containing the results of the query.
	 *
	 * @since 8.0
	 */
	IDataSet getDataSetByQuery(IQueryBuilder query, boolean useTableFilters, int max_returned_rows) throws ServoyException;

	/**
	 * RAGTEST doc
	 * @param dataSource
	 * @param columnSqlName
	 * @return
	 */
	SortOptions getSortOptions(IColumn column);

}
