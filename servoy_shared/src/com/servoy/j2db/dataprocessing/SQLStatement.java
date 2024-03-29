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
import java.util.HashMap;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLUpdate;
import com.servoy.j2db.util.Utils;

/**
 * SQLStatment object which can be serialized
 * @author		jblok
 */
public class SQLStatement implements ITrackingSQLStatement
{
	public static final long serialVersionUID = 4631404274972093942L;

	private int action = NO_ACTION;
	private String server_name;
	private final String table_name;
	private final IDataSet pks;//all pks which should be broadcasted
	private final ISQLUpdate sqlUpdate;
	private final ArrayList<TableFilter> filters;
	private String[] changedColumns;

	private final String transactionID;

	private String[] column_names;
	private Object[][] oldTrackingData;
	private Object[][] newTrackingData;
	private int expectedUpdateCount = -1;

	private String user_uid;
	private HashMap<String, Object> trackingInfo;
	private String clientId;

	private boolean oracleFix;
	private final ISQLSelect requerySelect;
	private int dataType = ISQLStatement.REGULAR_DATA_TYPE;

	/**
	 * create a SQLStatement
	 * @param connection_name
	 * @param tableName
	 * @param pk an array with inside arrays with pkcolumn(s)
	 * @param sql
	 * @param questiondata
	 */
	public SQLStatement(int action, String connection_name, String tableName, IDataSet pks, ISQLUpdate sqlUpdate)
	{
		this(action, connection_name, tableName, pks, null, sqlUpdate, null);
	}

	public SQLStatement(int action, String connection_name, String tableName, IDataSet pks, String tid, ISQLUpdate sqlUpdate, ArrayList<TableFilter> filters)
	{
		this(action, connection_name, tableName, pks, tid, sqlUpdate, filters, null);
	}

	public SQLStatement(int action, String connection_name, String tableName, IDataSet pks, String tid, ISQLUpdate sqlUpdate, ArrayList<TableFilter> filters,
		ISQLSelect requerySelect)
	{
		this.action = action;
		this.server_name = connection_name;
		this.table_name = tableName;
		this.pks = pks;
		this.sqlUpdate = sqlUpdate;
		this.filters = filters;
		this.transactionID = tid;
		this.requerySelect = requerySelect;
	}

	public void setOracleFixTrackingData(boolean oracleFix)
	{
		this.oracleFix = oracleFix;
	}

	public boolean isOracleFixTrackingData()
	{
		return this.oracleFix;
	}

	public void setTrackingData(String[] column_names, Object[][] oldData, Object[][] newData, String user_uid, HashMap<String, Object> trackingInfo,
		String clientId)
	{
		// TODO filter all data that is not changed out of the 3 arrays.
		if (oldData == null && newData == null) return;

		if (oldData == null)
		{
			oldTrackingData = new Object[newData.length][(newData.length > 0 ? (newData[0] != null ? newData[0].length : 0) : 0)];
		}
		else
		{
			oldTrackingData = new Object[oldData.length][(oldData.length > 0 ? (oldData[0] != null ? oldData[0].length : 0) : 0)];
			for (int i = 0; i < oldData.length; i++)
			{
				System.arraycopy(oldData[i], 0, oldTrackingData[i], 0, oldData[i].length);
			}
		}

		if (newData == null)
		{
			newTrackingData = new Object[oldData.length][(oldData.length > 0 ? (oldData[0] != null ? oldData[0].length : 0) : 0)];
		}
		else
		{
			newTrackingData = new Object[newData.length][(newData.length > 0 ? (newData[0] != null ? newData[0].length : 0) : 0)];
			for (int i = 0; i < newData.length; i++)
			{
				System.arraycopy(newData[i], 0, newTrackingData[i], 0, newData[i].length);
			}
		}

		this.column_names = column_names;
		this.user_uid = user_uid;
		this.trackingInfo = trackingInfo;
		this.clientId = clientId;

		//optimize wire transfer
		for (int i = 0; i < oldTrackingData.length; i++)
		{
			for (int j = 0; j < oldTrackingData[i].length; j++)
			{
				if (oldTrackingData[i][j] instanceof ValueFactory.BlobMarkerValue)
				{
					oldTrackingData[i][j] = ((ValueFactory.BlobMarkerValue)oldTrackingData[i][j]).getCachedData();
				}
				if (oldTrackingData[i][j] instanceof ValueFactory.NullValue)
				{
					oldTrackingData[i][j] = null;
				}
				if (oldTrackingData[i][j] instanceof byte[])
				{
					oldTrackingData[i][j] = oldTrackingData[i][j].toString();
				}

				if (newTrackingData[i][j] instanceof ValueFactory.BlobMarkerValue)
				{
					newTrackingData[i][j] = ((ValueFactory.BlobMarkerValue)newTrackingData[i][j]).getCachedData();
				}
				if (newTrackingData[i][j] instanceof ValueFactory.NullValue)
				{
					newTrackingData[i][j] = null;
				}
				// Don't do toString()
				if (newTrackingData[i][j] instanceof byte[])
				{
					newTrackingData[i][j] = newTrackingData[i][j];
				}

				if (Utils.equalObjects(oldTrackingData[i][j], newTrackingData[i][j]))
				{
					oldTrackingData[i][j] = null;
					newTrackingData[i][j] = null;
				}
			}
		}
	}

	public boolean isTracking()
	{
		return (column_names != null && !oracleFix);
	}

	public String getServerName()
	{
		return server_name;
	}

	public void setServerName(String name)
	{
		server_name = name;
	}

	public ISQLUpdate getUpdate()
	{
		return sqlUpdate;
	}

	public ArrayList<TableFilter> getFilters()
	{
		return filters;
	}

	public String[] getChangedColumns()
	{
		return changedColumns;
	}

	public int getAction()
	{
		return action;
	}

	public String getTableName()
	{
		return table_name;
	}

	public String[] getColumnNames()
	{
		return column_names;
	}

	public Object[][] getOldTrackingData()
	{
		return oldTrackingData;
	}

	public Object[][] getNewTrackingData()
	{
		return newTrackingData;
	}

	public String getUserUID()
	{
		return user_uid;
	}

	public HashMap<String, Object> getTrackingInfo()
	{
		return trackingInfo;
	}

	public String getTransactionID()
	{
		return transactionID;
	}

	public String getClientID()
	{
		return clientId;
	}

	/**
	 * Returns the pks.
	 * @return Object[]
	 */
	public IDataSet getPKs()
	{
		return pks;
	}

	public void setIdentityColumn(Column c)
	{
		identityColumn = c;
	}

	private Column identityColumn;

	public Column getIdentityColumn()
	{
		return identityColumn;
	}

	public int getExpectedUpdateCount()
	{
		return expectedUpdateCount;
	}

	/**
	 * Set the update count for checking
	 */
	public void setExpectedUpdateCount(int expectedUpdateCount)
	{
		this.expectedUpdateCount = expectedUpdateCount;
	}

	@Override
	public String toString()
	{
		return "SQLSTATEMENT[server:" + server_name + ", table:" + table_name + ", sql:" + sqlUpdate + ", filters:" + filters + ']'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public void setChangedColumns(String[] changedColumns)
	{
		this.changedColumns = changedColumns;
	}

	public ISQLSelect getRequerySelect()
	{
		return requerySelect;
	}

	public int getDataType()
	{
		return dataType;
	}

	public void setDataType(int dataType)
	{
		this.dataType = dataType;
	}

}
