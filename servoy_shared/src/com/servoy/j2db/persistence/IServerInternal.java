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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.util.ITransactionConnection;


/**
 * IServer with internal features (only accessible from application server).
 * @author rgansevles
 *
 */
public interface IServerInternal
{
	void flushTables(List<Table> tabelList) throws RepositoryException;

	Table createNewTable(IValidateName validator, String tableName) throws RepositoryException;

	Table createNewTable(IValidateName validator, String nm, boolean testSQL) throws RepositoryException;

	Table createNewTable(IValidateName validator, Table otherServerTable) throws RepositoryException;

	void reloadTables() throws RepositoryException;

	boolean hasTable(String tableName) throws RepositoryException;

	String[] syncTableObjWithDB(Table t, boolean createMissingServoySequences, boolean createMissingDBSequences, Table templateTable)
		throws RepositoryException, SQLException;

	void syncWithExternalTable(String tableName, Table externalTable) throws RepositoryException;

	void syncColumnSequencesWithDB(Table t) throws RepositoryException;

	String getServerURL();

	ServerConfig getConfig();

	String[] removeTable(Table t) throws SQLException, RepositoryException;

	void removeTable(String tableName) throws SQLException, RepositoryException;

	void testConnection(int i) throws Exception;

	void flagValid();

	void flagInvalid();

	int getState();

	void fireStateChanged(int oldState, int state);

	boolean checkIfTableExistsInDatabase(String adjustedTableName) throws SQLException;

	void duplicateColumnInfo(ColumnInfo sourceColumnInfo, ColumnInfo targetColumnInfo);

	boolean updateColumnInfo(QueryColumn queryColumn) throws RepositoryException;

	void updateAllColumnInfo(Table table) throws RepositoryException;

	void refreshTable(String name) throws RepositoryException;

	void removeTableListener(ITableListener tableListener);

	void addTableListener(ITableListener tableListener);

	Object getNextSequence(String tableName, String columnName) throws RepositoryException;

	boolean supportsSequenceType(int i, Column column) throws Exception;

	String toHTML();

	IServerManagerInternal getServerManager();

	String getName();

	List<String> getViewNames() throws RepositoryException;

	List<String> getTableNames() throws RepositoryException;

	List<String> getTableAndViewNames() throws RepositoryException;

	Table getTable(String tableName) throws RepositoryException;

	IRepository createRepositoryTables() throws RepositoryException;

	IRepository getRepository() throws RepositoryException;

	Table getRepositoryTable(String name) throws RepositoryException;

	Table createNewTable(IValidateName nameValidator, Table selectedTable, String tableName) throws RepositoryException;

	boolean isTableLoaded(String tableName);

	void reloadTableColumnInfo(Table t) throws RepositoryException;

	boolean isValid();

	ITransactionConnection getConnection() throws SQLException;

	ITransactionConnection getUnmanagedConnection() throws SQLException;

	QuerySet getSQLQuerySet(ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve, boolean forceQualifyColumns)
		throws RepositoryException;

	boolean hasMissingDBSequences(Table table) throws SQLException;

	String[] createMissingDBSequences(Table table) throws SQLException, RepositoryException;

	String getDialectClassName();

	DataSource getDataSource() throws Exception;

	String getIndexCreateString(Table t, String indexName, Column[] indexColumns, boolean unique) throws SQLException;

	Connection getRawConnection() throws SQLException;
}
