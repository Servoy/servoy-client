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


import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.util.Debug;

/**
 * Scriptable column object for use in scripting 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSColumn implements IReturnedTypesProvider, IConstantsObject, IColumnTypes
{
	/**
	 * Constant used when setting or getting the type of columns.
	 * 
	 * @sampleas js_getType()
	 */
	public static final int DATETIME = IColumnTypes.DATETIME;

	/**
	 * @sameas DATETIME
	 */
	public static final int TEXT = IColumnTypes.TEXT;

	/**
	 * @sameas DATETIME
	 */
	public static final int NUMBER = IColumnTypes.NUMBER;

	/**
	 * @sameas DATETIME
	 */
	public static final int INTEGER = IColumnTypes.INTEGER;

	/**
	 * @sameas DATETIME
	 */
	public static final int MEDIA = IColumnTypes.MEDIA;

	private Column column;
	private IServer server;

	public JSColumn()
	{
	} //only for use JS engine

	public JSColumn(Column column, IServer server)
	{
		this.column = column;
		this.server = server;
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "JSColumn"; //$NON-NLS-1$
	}

	/**
	 * Get the data provider id (name) for this column.
	 *
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * var dataProviderId = column.getDataProviderID()
	 * 
	 * @return String dataprovider id.
	 */
	public String js_getDataProviderID()
	{
		return column.getDataProviderID();
	}

	/**
	 * Get the name of the column as known by the database.
	 *
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * var sqlName = column.getSQLName()
	 * 
	 * @return String sql name
	 */
	public String js_getSQLName()
	{
		return column.getSQLName();
	}

	/**
	 * Get the qualified name (including table name) of the column as known by the database.
	 * The name is quoted, if necessary, as defined by the actual database used.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * var qualifiedSqlName = column.getQualifiedName()
	 * 
	 * @return String qualified column name.
	 */
	public String js_getQualifiedName()
	{
		try
		{
			return server.getQuotedIdentifier(column.getTable().getSQLName(), column.getSQLName());
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Returns a quoted version of the column name, if necessary, as defined by the actual database used.
	 *
	 * @sample
	 * //use with the raw SQL plugin:
	 * //if the table name contains characters that are illegal in sql, the table name will be quoted
	 * var jsTable = databaseManager.getTable('udm', 'campaigns')
	 * var quotedTableName = jsTable.getQuotedSQLName()
	 * var jsColumn = jsTable.getColumn('active')
	 * var quotedColumnName = jsColumn.getQuotedSQLName()
	 * plugins.rawSQL.executeSQL('udm',  quotedTableName,  'select * from ' + quotedTableName + ' where ' + quotedColumnName + ' = ?', [1])
	 * 
	 * @return column name, quoted if needed. 
	 */
	public String js_getQuotedSQLName()
	{
		try
		{
			return server.getQuotedIdentifier(null, column.getSQLName());
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Get the JDBC type of the column.
	 * The type reported by the JDBC driver will be mapped to one of:
	 *  - JSColumn.DATETIME
	 *  - JSColumn.TEXT
	 *  - JSColumn.NUMBER
	 *  - JSColumn.INTEGER
	 *  - JSColumn.MEDIA
	 *
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * switch (column.getType())
	 * {
	 * case JSColumn.TEXT:
	 *	// handle text column
	 * break;
	 * 
	 * case JSColumn.NUMBER:
	 * case JSColumn.INTEGER:
	 *	// handle numerical column
	 * break;
	 * }
	 * 
	 * @return int sql type.
	 */
	public int js_getType()
	{
		return Column.mapToDefaultType(column.getType());
	}

	/**
	 * Get the name JDBC type of the column.
	 * The same mapping as defined in JSColumn.getType() is applied.
	 * 
	 * @see com.servoy.j2db.dataprocessing.JSColumn#js_getType()
	 *
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * var typeName = column.getTypeAsString()
	 * 
	 * @return String sql name.
	 */
	public String js_getTypeAsString()
	{
		return column.getTypeAsString();
	}

	/**
	 * Get the allow-null flag of the column.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * if (!column.getAllowNull())
	 * {
	 * 	 // column cannot be null
	 * }
	 * 
	 * @return boolean allow-null flag.
	 */
	public boolean js_getAllowNull()
	{
		return column.getAllowNull();
	}

	/**
	 * Get the length of the column as reported by the JDBC driver.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customername')
	 * if (column.getLength() < 10)
	 * {
	 * 	 // handle short column
	 * }
	 * 
	 * @return int column length.
	 */
	public int js_getLength()
	{
		return column.getLength();
	}

	/**
	 * Get the scale of the column as reported by the JDBC driver.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customername')
	 * var scale = column.getScale()
	 * 
	 * @return int column scale.
	 */
	public int js_getScale()
	{
		return column.getScale();
	}

	/**
	 * Is this column one of the row identifiers for its table.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('orderid')
	 * if (column.isRowIdentifier())
	 * {
	 * 	 // handle pk column
	 * }
	 * 
	 * @return boolean true if is row identifier else false.
	 */
	public boolean js_isRowIdentifier()
	{
		return (column.getRowIdentType() != Column.NORMAL_COLUMN);
	}

	/**
	 * Is this column marked as UUID column.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('orderid')
	 * if (column.isUUID())
	 * {
	 * 	 // handle UUID column
	 * }
	 * 
	 * @return boolean true if is marked as UUID columns else false.
	 */
	public boolean js_isUUID()
	{
		return column.hasFlag(Column.UUID_COLUMN);
	}

	/**
	 * Get the foreign type of the column.
	 * The foreign type can be defined design time as a foreign key reference to another table.
	 * 
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_mergeRecords(Object[])
	 *
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customerid')
	 * var foreignType = column.getForeignType()
	 * if (foreignType != null)
	 * {
	 * 	var fkTable = databaseManager.getTable('example_data', foreignType)
	 * }	
	 * 
	 * @return String foreign type.
	 */
	public String js_getForeignType()
	{
		ColumnInfo columnInfo = column.getColumnInfo();
		return columnInfo != null ? columnInfo.getForeignType() : null;
	}

	/**
	 * Get the title property of the column.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customername')
	 * var title = column.getTitle()
	 * 
	 * @return String column title.
	 */
	public String js_getTitle()
	{
		return column.getTitle();
	}

	/**
	 * Get the description property of the column.
	 * 
	 * @sample
	 * var table = databaseManager.getTable('example_data', 'orders')
	 * var column = table.getColumn('customername')
	 * var desc = column.getDescription()
	 * 
	 * @return String column description.
	 */
	public String js_getDescription()
	{
		ColumnInfo columnInfo = column.getColumnInfo();
		return columnInfo != null ? columnInfo.getDescription() : null;
	}

	@Override
	public String toString()
	{
		return "JSColumn[" + column.getName() + ']'; //$NON-NLS-1$
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}
}
