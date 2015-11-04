/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.query.BaseColumnType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.XMLUtils;

/**
 * Methods for handling table meta data.
 *
 * @author rgansevles
 *
 * @since 6.1
 *
 */
public class MetaDataUtils
{
	/**
	 * Special columns for meta data
	 */
	public static final String METADATA_MODIFICATION_COLUMN = "modification_date";
	public static final String METADATA_DELETION_COLUMN = "deletion_date";

	public static boolean canBeMarkedAsMetaData(ITable table)
	{
		for (IColumn column : table.getRowIdentColumns())
		{
			if (column.getColumnInfo() == null || !column.getColumnInfo().hasFlag(Column.UUID_COLUMN))
			{
				return false;
			}
		}
		IColumn column = table.getColumn(METADATA_MODIFICATION_COLUMN);
		if (column == null || column.getDataProviderType() != IColumnTypes.DATETIME) return false;
		column = table.getColumn(METADATA_DELETION_COLUMN);
		if (column == null || column.getDataProviderType() != IColumnTypes.DATETIME) return false;
		return true;
	}

	/**
	 * Serialize contents of buffered dataset to a string, includes column names and type info
	 * @param dataSet
	 * @return
	 * @throws JSONException
	 */
	public static String serializeTableMetaDataContents(BufferedDataSet dataSet) throws JSONException
	{
		if (dataSet == null)
		{
			return null;
		}

		ServoyJSONObject json = new ServoyJSONObject();

		// columns
		JSONArray jsonColumns = new JSONArray();
		String[] columnNames = dataSet.getColumnNames();
		BaseColumnType[] columnTypes = BufferedDataSetInternal.getColumnTypeInfo(dataSet);
		for (int c = 0; c < columnNames.length; c++)
		{
			JSONObject jsonColumn = new JSONObject();
			jsonColumn.put("name", columnNames[c]);
			jsonColumn.put("type", XMLUtils.serializeColumnType(columnTypes[c]));
			jsonColumns.put(jsonColumn);
		}
		json.put("columns", jsonColumns);

		// rows
		JSONArray jsonRows = new JSONArray();
		for (int r = 0; r < dataSet.getRowCount(); r++)
		{
			Object[] row = dataSet.getRow(r);
			JSONArray rowobj = new JSONArray();
			for (int i = 0; i < row.length && i < columnNames.length; i++)
			{
				Object val;
				if (row[i] == null)
				{
					val = JSONObject.NULL;
				}
				else if (row[i] instanceof byte[])
				{
					val = Utils.encodeBASE64((byte[])row[i]);
				}
				else
				{
					val = row[i];
				}
				rowobj.put(val);
			}
			jsonRows.put(rowobj);
		}
		json.put("rows", jsonRows);

		// toString
		return json.toString(true);
	}

	/**
	 * Deserialize table contents string to of buffered dataset to a string, includes column names and type info
	 *
	 * @param data
	 * @return
	 * @throws JSONException
	 */
	public static BufferedDataSet deserializeTableMetaDataContents(String data) throws JSONException
	{
		if (data == null)
		{
			return null;
		}

		ServoyJSONObject json = new ServoyJSONObject(data, true);
		JSONArray jsonColumns = (JSONArray)json.get("columns");

		String[] columnNames = new String[jsonColumns.length()];
		ColumnType[] columnTypes = new ColumnType[jsonColumns.length()];

		for (int c = 0; c < jsonColumns.length(); c++)
		{
			JSONObject jsonColumn = (JSONObject)jsonColumns.get(c);

			columnNames[c] = jsonColumn.getString("name");
			JSONArray typeArray = new JSONArray(jsonColumn.getString("type"));
			columnTypes[c] = ColumnType.getInstance(typeArray.getInt(0), typeArray.getInt(1), typeArray.getInt(2));
		}

		List<Object[]> rows = new ArrayList<Object[]>();

		JSONArray jsonArray = (JSONArray)json.get("rows");
		for (int r = 0; r < jsonArray.length(); r++)
		{
			JSONArray rowobj = (JSONArray)jsonArray.get(r);
			Object[] row = new Object[columnNames.length];
			for (int i = 0; i < columnNames.length; i++)
			{
				Object val = rowobj.get(i);
				if (val == JSONObject.NULL)
				{
					row[i] = null;
				}
				else if (Column.mapToDefaultType(columnTypes[i].getSqlType()) == IColumnTypes.MEDIA && val instanceof String)
				{
					row[i] = Utils.decodeBASE64((String)val);
				}
				else if (Column.mapToDefaultType(columnTypes[i].getSqlType()) == IColumnTypes.DATETIME && val instanceof String)
				{
					Date parsed = ServoyJSONObject.parseDate((String)val);
					row[i] = parsed == null ? val : parsed; // convert when possible, otherwise leave to driver (fails on mysql)
				}
				else
				{
					row[i] = val;
				}
			}
			rows.add(row);
		}

		return BufferedDataSetInternal.createBufferedDataSet(columnNames, columnTypes, rows, false);
	}

	public static String generateMetaDataFileContents(Table table, int max) throws RemoteException, ServoyException, JSONException, TooManyRowsException
	{
		LinkedHashMap<Column, QueryColumn> qColumns = new LinkedHashMap<Column, QueryColumn>(); // LinkedHashMap to keep order for column names

		QuerySelect query = createTableMetadataQuery(table, qColumns);

		BufferedDataSet dataSet = (BufferedDataSet)ApplicationServerRegistry.get().getDataServer().performQuery(ApplicationServerRegistry.get().getClientId(),
			table.getServerName(), null, query, null, false, 0, max, IDataServer.META_DATA_QUERY, null);
		// not too much data?
		if (dataSet.hadMoreRows())
		{
			throw new TooManyRowsException();
		}

		String[] columnNames = new String[qColumns.size()];
		int i = 0;
		for (Column column : qColumns.keySet())
		{
			columnNames[i++] = column.getName();
		}
		dataSet.setColumnNames(columnNames);

		return serializeTableMetaDataContents(dataSet);
	}


	public static QuerySelect createTableMetadataQuery(Table table, LinkedHashMap<Column, QueryColumn> queryColumns)
	{
		QuerySelect query = new QuerySelect(new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema()));
		LinkedHashMap<Column, QueryColumn> qColumns = queryColumns == null ? new LinkedHashMap<Column, QueryColumn>() : queryColumns; // LinkedHashMap to keep order for column names
		Iterator<IColumn> columns = table.getColumnsSortedByName();
		while (columns.hasNext())
		{
			Column column = (Column)columns.next();
			if (!column.hasFlag(Column.EXCLUDED_COLUMN))
			{
				QueryColumn qColumn = new QueryColumn(query.getTable(), column.getID(), column.getSQLName(), column.getType(), column.getLength());
				query.addColumn(qColumn);
				qColumns.put(column, qColumn);
			}
		}
		for (Column column : table.getRowIdentColumns())
		{
			if (qColumns.containsKey(column))
			{
				query.addSort(new QuerySort(qColumns.get(column), true));
			}
		}
		return query;
	}

	public static int loadMetadataInTable(Table table, String json) throws IOException, ServoyException, JSONException
	{
		// parse dataset
		BufferedDataSet dataSet = MetaDataUtils.deserializeTableMetaDataContents(json);

		// check if all columns exist
		List<String> missingColumns = null;
		for (String colname : dataSet.getColumnNames())
		{
			if (table.getColumn(colname) == null)
			{
				if (missingColumns == null)
				{
					missingColumns = new ArrayList<String>();
				}
				missingColumns.add(colname);
			}
		}
		if (missingColumns != null)
		{
			StringBuilder message = new StringBuilder("Missing columns from meta data for table '").append(table.getName()).append("'").append(
				" in server '").append(table.getServerName()).append("' : ");
			for (String name : missingColumns)
			{
				message.append('\'').append(name).append("' ");
			}
			throw new RepositoryException(message.toString());
		}


		// delete existing data
		ApplicationServerRegistry.get().getDataServer().performUpdates(ApplicationServerRegistry.get().getClientId(),
			new ISQLStatement[] { new SQLStatement(IDataServer.META_DATA_QUERY, table.getServerName(), table.getName(), null, //
				new QueryDelete(new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema()))) // delete entire table
		});
		// insert the data
		ApplicationServerRegistry.get().getDataServer().insertDataSet(ApplicationServerRegistry.get().getClientId(), dataSet, table.getDataSource(),
			table.getServerName(), table.getName(), null, null, null);

		return dataSet.getRowCount();
	}


	public static class TooManyRowsException extends Exception
	{
	}
}
