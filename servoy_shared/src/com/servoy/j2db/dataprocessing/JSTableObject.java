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

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSTableObject extends JSTable
{
	public JSTableObject(ITable table, IServer server)
	{
		super(table, server);
	}

	/**
	 * Creates a new column in this table. The name, type and length of the new column must be specified. For specifying the
	 * type of the column, use the JSColumn constants. The column is not actually created in the database until this
	 * table is synchronized with the database using the JSServer.synchronizeWithDB method. The "allowNull" optional argument specifies if the
	 * column accepts null values (by default it does). The "pkColumn" optional argument specifies if the column is a primary key column (by default it is not).
	 * The method returns a JSColumn instance that corresponds to the newly created column. If any error occurs and the column cannot be created, then the method
	 * returns null.
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server)
	 * {
	 * 	var table = server.createNewTable("users");
	 * 	if (table)
	 * 	{
	 * 		table.createNewColumn("id", JSColumn.INTEGER, 0, false, true);
	 * 		table.createNewColumn("name", JSColumn.TEXT, 100);
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
	public JSColumn js_createNewColumn(Object[] args)
	{
		if (args.length < 3) return null;
		String columnName = args[0].toString();
		int type = Utils.getAsInteger(args[1]);
		int length = Utils.getAsInteger(args[2]);
		boolean allowNull = true;
		boolean pkColumn = false;
		if (args.length > 3) allowNull = Utils.getAsBoolean(args[3]);
		if (args.length > 4) pkColumn = Utils.getAsBoolean(args[4]);
		try
		{
			Column c = ((Table)getTable()).createNewColumn(DummyValidator.INSTANCE, columnName, type, length, allowNull, pkColumn);
			return new JSColumn(c, getServer());
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
