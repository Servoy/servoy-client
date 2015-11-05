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

import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.server.shared.IClientManagerInternal;
import com.servoy.j2db.util.ServoyException;


/**
 * IDataServer with extensions only valid in the application server.
 */
public interface IDataServerInternal
{
	public static final int MAX_ROWS_TO_RETRIEVE = 49999999;

	void doInserts(String client_id, String transaction_id, ITable table, IDataSet resultWithColumnNames, ArrayList<TableFilter> filters)
		throws ServoyException;

	void doI18NUpdates(String client_id, String transaction_id, Table table, BufferedDataSet resultWithColumnNames, boolean onlyInserts) throws ServoyException;

	IServerManagerInternal getServerManager();

	Object getNextRepositorySequence(String tableName, String columnName, int columnInfoID) throws RepositoryException;

	String startRepositoryTransaction(String client_id) throws RepositoryException;

	boolean endRepositoryTransactions(String client_id, String[] transaction_ids, boolean commit) throws RepositoryException;

	Object[] performRepositoryUpdates(String client_id, ISQLStatement[] statements) throws ServoyException;

	IDataSet performRepositoryQuery(String client_id, String transaction_id, ISQLSelect sqlSelect, ArrayList<TableFilter> filters, boolean distinctInMemory,
		int startRow, int rowsToRetrieve, boolean createMetaInfo, int type, ITrackingSQLStatement trackingInfo) throws ServoyException;

	IClientManagerInternal getClientManager();
}
