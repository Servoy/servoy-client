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
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The JSTableObject is a newly created JSTable, that can be altered - one can add new columns as well to it for example.
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSTableObject")
public class JSTableObject extends JSTable
{
	public JSTableObject(ITable table, IServer server)
	{
		super(table, server);
	}

	/**
	 * @clonedesc js_createNewColumn(String, Number, Number, Boolean, Boolean)
	 * @sampleas js_createNewColumn(String, Number, Number, Boolean, Boolean)
	 *
	 * @param columnName
	 * @param type
	 * @param length
	 * @param allowNull
	 */
	public JSColumnObject js_createNewColumn(String columnName, Number type, Number length, Boolean allowNull)
	{
		return js_createNewColumn(columnName, type, length, allowNull, false);
	}

	/**
	 * @clonedesc js_createNewColumn(String, Number, Number, Boolean, Boolean)
	 * @sampleas js_createNewColumn(String, Number, Number, Boolean, Boolean)
	 *
	 * @param columnName
	 * @param type
	 * @param length
	 */
	public JSColumnObject js_createNewColumn(String columnName, Number type, Number length)
	{
		return js_createNewColumn(columnName, type, length, true, false);
	}

	/**
	 * Creates a new column in this table. The name, type and length of the new column must be specified. For specifying the
	 * type of the column, use the JSColumn constants. The column is not actually created in the database until this
	 * table is synchronized with the database using the JSServer.synchronizeWithDB method.
	 *
	 * The method returns a JSColumn instance that corresponds to the newly created column. If any error occurs and the column cannot be created, then the method
	 * returns null.
	 * @see JSColumnObject
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server)
	 * {
	 * 	var table = server.createNewTable("users");
	 * 	if (table)
	 * 	{
	 * 		var pk = table.createNewColumn("id", JSColumn.MEDIA, 16); // can also use (JSColumn.TEXT, 36) for UUIDs
	 * 		pk.rowIdentifierType = JSColumn.PK_COLUMN;
	 * 		pk.setFlag(JSColumn.UUID_COLUMN, true)
	 * 		pk.sequenceType = JSColumn.UUID_GENERATOR
	 * 		var c = table.createNewColumn("name", JSColumn.TEXT, 100);
	 * 		c.allowNull = false
	 * 		table.createNewColumn("age", JSColumn.INTEGER, 0);
	 * 		table.createNewColumn("last_login", JSColumn.DATETIME, 0);
	 * 		var result = server.synchronizeWithDB(table);
	 * 		if (result) application.output("Table successfully created.");
	 * 		else application.output("Table not created.");
	 * 	}
	 * }
	 *
	 * @param columnName
	 * @param type
	 * @param length
	 * @param allowNull
	 * @param pkColumn
	 */
	public JSColumnObject js_createNewColumn(String columnName, Number type, Number length, Boolean allowNull, Boolean pkColumn)
	{
		int _type = Utils.getAsInteger(type);
		int _length = Utils.getAsInteger(length);
		boolean _allowNull = Utils.getAsBoolean(allowNull);
		boolean _pkColumn = Utils.getAsBoolean(pkColumn);
		try
		{
			Column c = ((Table)getTable()).createNewColumn(DummyValidator.INSTANCE, columnName, _type, _length, _allowNull, _pkColumn);
			return new JSColumnObject(c, getServer(), getTable());
		}
		catch (RepositoryException e)
		{
			Debug.error("Exception while creating new column.", e); //$NON-NLS-1$
			return null;
		}
	}

	/**
	 * Deletes the column with the specified name from this table. The column is not actually deleted from the database until this
	 * table is synchronized with the database using the JSServer.synchronizeWithDB method.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server) {
	 * 	var table = server.getTable("users");
	 * 	if (table) {
	 * 		table.deleteColumn("last_login");
	 * 		server.synchronizeWithDB(table);
	 * 	}
	 * }
	 *
	 * @param columnName
	 */
	public void js_deleteColumn(String columnName)
	{
		((Table)getTable()).removeColumn(columnName);
	}
}
