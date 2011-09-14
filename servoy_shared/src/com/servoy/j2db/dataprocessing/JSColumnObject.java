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

package com.servoy.j2db.dataprocessing;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServer;

/**
 * Scriptable column object used when new columns are created.
 * The column can be modified, changes are applied in the server.synchronizeWithDB() call.
 * 
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSColumnObject extends JSColumn
{
	public JSColumnObject()
	{
	} // only for use JS engine

	public JSColumnObject(Column column, IServer server)
	{
		super(column, server);
	}

	public void js_setAllowNull(boolean allowNull)
	{
		getColumn().setAllowNull(allowNull);
	}

	/**
	 * Get or set the allow-null flag of a new column.
	 * Note that when a column is added to an existing table, allowNull will always be set.
	 * For a primary key column, the allowNull flag will be always off, for other columns the flag is set by default.
	 * 
	 * @sampleas com.servoy.j2db.dataprocessing.JSTableObject#js_createNewColumn(Object[])
	 * 
	 * @param allowNull
	 */
	@Override
	public boolean js_getAllowNull() // override for jsdoc 
	{
		return super.js_getAllowNull();
	}

	/**
	 * Set or clear a flag of a new column.
	 * The flags are a bit pattern consisting of 1 or more of the following bits:
	 *  - JSColumn.PK_COLUMN;
	 *  - JSColumn.USER_ROWID_COLUMN;
	 *  - JSColumn.UUID_COLUMN;
	 *  - JSColumn.EXCLUDED_COLUMN;
	 *  
	 * @sampleas com.servoy.j2db.dataprocessing.JSTableObject#js_createNewColumn(Object[])
	 * 
	 * @param flag the flag to set
	 * @param set true for set flag, false for clear flag
	 */
	public void js_setFlag(int flag, boolean set)
	{
		getColumn().setFlag(flag, set);
	}

	public void js_setSequenceType(int sequenceType)
	{
		getColumn().setSequenceType(sequenceType);
	}

	/**
	 * Set the sequence type of the column.
	 * The sequence type is one of:
	 *  - JSColumn.NO_SEQUENCE_SELECTED
	 *  - JSColumn.SERVOY_SEQUENCE
	 *  - JSColumn.DATABASE_SEQUENCE
	 *  - JSColumn.DATABASE_IDENTITY
	 *  - JSColumn.UUID_GENERATOR;
	 *
	 * @sample
	 * var server = plugins.maintenance.getServer("example_data");
	 * if (server)
	 * {
	 *	// users has uuid pk
	 * 	var table = server.createNewTable("users");
	 * 	if (table)
	 * 	{
	 * 		var pk = table.createNewColumn("id", JSColumn.MEDIA, 16); // can also use <JSColumn.TEXT, 36> for UUIDs)
	 * 		pk.setFlag(JSColumn.PK_COLUMN, true);
	 * 		pk.setFlag(JSColumn.UUID_COLUMN, true)
	 * 		pk.sequenceType = JSColumn.UUID_GENERATOR
	 * 		table.createNewColumn("name", JSColumn.TEXT, 100);
	 * 		var result = server.synchronizeWithDB(table);
	 * 		if (result) application.output("Table users successfully created.");
	 * 		else application.output("Table users not created.");
	 * 	}
	 * 
	 *  // groups has database sequence pk
	 * 	table = server.createNewTable("groups");
	 * 	if (table)
	 * 	{
	 * 		pk = table.createNewColumn("id", JSColumn.INTEGER, 0);
	 * 		pk.setFlag(JSColumn.PK_COLUMN, true);
	 * 		pk.sequenceType = JSColumn.DATABASE_SEQUENCE
	 * 		pk.setDatabaseSequenceName('mygroupsequence')
	 * 		table.createNewColumn("name", JSColumn.TEXT, 100);
	 * 		result = server.synchronizeWithDB(table);
	 * 		if (result) application.output("Table groups successfully created.");
	 * 		else application.output("Table groups not created.");
	 * 	}
	 * }
	 * 
	 * @param sequenceType
	 */
	@Override
	public int js_getSequenceType() // override for jsdoc 
	{
		return super.js_getSequenceType();
	}

	/**
	 * Set the database sequence name of the column, used for columns with sequence type JSColumn.DATABASE_SEQUENCE.
	 *
	 * @sampleas js_getSequenceType()
	 * 
	 * @param sequenceName
	 */
	public void js_setDatabaseSequenceName(String sequenceName)
	{
		getColumn().setDatabaseSequenceName(sequenceName);
	}
}
