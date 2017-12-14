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


import com.servoy.base.persistence.IBaseColumn;
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

	/**
	 * Constant for column information indicating unset values.
	 *
	 * @sampleas js_getSequenceType()
	 */
	public static final int NONE = 0;
	/**
	 * Constant used when setting or getting the row identifier type of columns.
	 * This value identifies columns that are defined as primary key in the database.
	 *
	 * @sampleas js_getRowIdentifierType()
	 */
	public static final int PK_COLUMN = Column.PK_COLUMN;

	/**
	 * Constant used when setting or getting the row identifier type of columns.
	 * This value identifies columns that are defined as primary key by the developer (but not in the database).
	 *
	 * @sampleas js_getRowIdentifierType()
	 */
	public static final int ROWID_COLUMN = Column.USER_ROWID_COLUMN;

	/**
	 * Constant used when setting or getting the flags of columns.
	 * This flag identifies columns whose values are treated as UUID.
	 *
	 * @sampleas js_hasFlag(int)
	 */
	public static final int UUID_COLUMN = Column.UUID_COLUMN;

	/**
	 * Constant used when setting or getting the flags of columns.
	 * This flag identifies columns that are skipped in the sql.
	 *
	 * @sampleas js_hasFlag(int)
	 */
	public static final int EXCLUDED_COLUMN = Column.EXCLUDED_COLUMN;

	/**
	 * Constant used when setting or getting the flags of columns.
	 * This flag identifies columns that are marked as a tenant column.
	 *
	 * @sampleas js_hasFlag(int)
	 */
	public static final int TENANT_COLUMN = Column.TENANT_COLUMN;

	/**
	 * Constant used when setting or getting the sequence type of columns.
	 *
	 * @sampleas js_getSequenceType()
	 */
	public static final int SERVOY_SEQUENCE = ColumnInfo.SERVOY_SEQUENCE + 1; // added 1 to prevent clash with NONE
	/**
	 * @sameas SERVOY_SEQUENCE
	 */
	public static final int DATABASE_SEQUENCE = ColumnInfo.DATABASE_SEQUENCE + 1;
	/**
	 * @sameas SERVOY_SEQUENCE
	 */
	public static final int DATABASE_IDENTITY = ColumnInfo.DATABASE_IDENTITY + 1;
	/**
	 * @sameas SERVOY_SEQUENCE
	 */
	public static final int UUID_GENERATOR = ColumnInfo.UUID_GENERATOR + 1;


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
	 * @return the column
	 */
	public Column getColumn()
	{
		return column;
	}

	/**
	 * @return the server
	 */
	public IServer getServer()
	{
		return server;
	}

	/**
	 * @see com.servoy.j2db.scripting.IPrefixedConstantsObject#getPrefix()
	 */
	public String getPrefix()
	{
		return "JSColumn"; //$NON-NLS-1$
	}

	/**
	 * Get the data provider id for this column (which is the same as name if not explicitly defined otherwise).
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
		return column.getDataProviderType();
	}

	/**
	 * Get the raw JDBC type of the column, which allows to check database specific types, like sting/byte column type variations.
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
	 * var column = table.getColumn('customerid')
	 * var sqlType = column.getSQLType();
	 *
	 * @return int sql type.
	 * @see java.sql.Types
	 */
	public int js_getSQLType()
	{
		return column.getType();
	}

	/**
	 * Get the name JDBC type of the column.
	 * The same mapping as defined in JSColumn.getType() is applied.
	 *
	 * @see com.servoy.j2db.dataprocessing.JSColumn#js_getType()
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
	 * var column = table.getColumn('customerid')
	 * var typeName = column.getTypeAsString()
	 *
	 * @return String sql name.
	 */
	public String js_getTypeAsString()
	{
		return column.getTypeAsString();
	}

	/** Check a flag of the column.
	 * The flags are a bit pattern consisting of 1 or more of the following bits:
	 *  - JSColumn.UUID_COLUMN
	 *  - JSColumn.EXCLUDED_COLUMN
	 *  - JSColumn.TENANT_COLUMN
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
	 * var column = table.getColumn('customerid')
	 * if (column.hasFlag(JSColumn.UUID_COLUMN))
	 * {
	 * 	// handle uuid column
	 * }
	 *
	 * @param flag
	 *
	 * @return boolean whether flag is set.
	 */
	public boolean js_hasFlag(int flag)
	{
		return column.hasFlag(flag);
	}

	/**
	 * Get the sequence type of the column.
	 * The sequence type is one of:
	 *  - JSColumn.NONE
	 *  - JSColumn.SERVOY_SEQUENCE
	 *  - JSColumn.DATABASE_SEQUENCE
	 *  - JSColumn.DATABASE_IDENTITY
	 *  - JSColumn.UUID_GENERATOR;
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
	 * var column = table.getColumn('customerid')
	 * switch (column.getSequenceType())
	 * {
	 * case JSColumn.NONE:
	 *	// handle column with no sequence
	 * break;
	 *
	 * case JSColumn.UUID_GENERATOR:
	 *	// handle uuid generated column
	 * break;
	 * }
	 *
	 * @return int sequence type.
	 */
	public int js_getSequenceType()
	{
		switch (getColumn().getSequenceType())
		// JSColumn constants differ from ColumnInfo constants
		{
			case ColumnInfo.SERVOY_SEQUENCE :
				return SERVOY_SEQUENCE;
			case ColumnInfo.DATABASE_SEQUENCE :
				return DATABASE_SEQUENCE;
			case ColumnInfo.DATABASE_IDENTITY :
				return DATABASE_IDENTITY;
			case ColumnInfo.UUID_GENERATOR :
				return UUID_GENERATOR;
			default :
				return NONE;
		}
	}

	/**
	 * Get the allow-null flag of the column.
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * Use column.getRowIdentifierType() != JSColumn.NONE instead.
	 *
	 * @deprecated As of release 6.0, replaced by {@link #getRowIdentifierType()}.
	 */
	@Deprecated
	public boolean js_isRowIdentifier()
	{
		return js_getRowIdentifierType() != NONE;
	}

	/**
	 * Get the row identifier type of the column.
	 * The sequence type is one of:
	 *  - JSColumn.PK_COLUMN
	 *  - JSColumn.ROWID_COLUMN
	 *  - JSColumn.NONE
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
	 * var column = table.getColumn('customerid')
	 * switch (column.getRowIdentifierType())
	 * {
	 * case JSColumn.NONE:
	 *	// handle normal column
	 * break;
	 *
	 * case JSColumn.PK_COLUMN:
	 *	// handle database pk column
	 * break;
	 *
	 * case JSColumn.ROWID_COLUMN:
	 *	// handle developer defined pk column
	 * break;
	 * }
	 *
	 * @return int row identifier type.
	 */
	public int js_getRowIdentifierType()
	{
		return column.hasFlag(IBaseColumn.PK_COLUMN) ? PK_COLUMN //
			: column.hasFlag(IBaseColumn.USER_ROWID_COLUMN) ? ROWID_COLUMN //
				: NONE;
	}

	/**
	 * Use column.hasFlag(JSColumn.UUID_COLUMN) instead.
	 *
	 * @deprecated As of release 6.0, replaced by {@link #hasFlag()}.
	 *
	 */
	@Deprecated
	public boolean js_isUUID()
	{
		return column.hasFlag(IBaseColumn.UUID_COLUMN);
	}

	/**
	 * Get the default format of the column.
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
	 * var column = table.getColumn('customerid')
	 * var format = column.getDefaultFormat()
	 *
	 * @return String column default format.
	 */
	public String js_getDefaultFormat()
	{
		ColumnInfo columnInfo = column.getColumnInfo();
		return columnInfo != null ? columnInfo.getDefaultFormat() : null;
	}

	/**
	 * Get the foreign type of the column.
	 * The foreign type can be defined design time as a foreign key reference to another table.
	 *
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_mergeRecords(Object[])
	 *
	 * @sample
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
	 * var table = databaseManager.getTable('db:/example_data/orders')
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
