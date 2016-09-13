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
import com.servoy.j2db.persistence.ColumnInfo;
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
	 * @sampleas com.servoy.j2db.dataprocessing.JSTableObject#js_createNewColumn(String, Number, Number, Boolean, Boolean)
	 */
	@Override
	public boolean js_getAllowNull() // override for jsdoc
	{
		return super.js_getAllowNull();
	}

	/**
	 * Set or clear a flag of a new column.
	 * The flags are a bit pattern consisting of 1 or more of the following bits:
	 *  - JSColumn.UUID_COLUMN;
	 *  - JSColumn.EXCLUDED_COLUMN;
	 *
	 * @sampleas com.servoy.j2db.dataprocessing.JSTableObject#js_createNewColumn(String, Number, Number, Boolean, Boolean)
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
		int colseqtype;
		switch (sequenceType)
		// JSColumn constants differ from ColumnInfo constants
		{
			case JSColumn.SERVOY_SEQUENCE :
				colseqtype = ColumnInfo.SERVOY_SEQUENCE;
				break;
			case JSColumn.DATABASE_SEQUENCE :
				colseqtype = ColumnInfo.DATABASE_SEQUENCE;
				break;
			case JSColumn.DATABASE_IDENTITY :
				colseqtype = ColumnInfo.DATABASE_IDENTITY;
				break;
			case JSColumn.UUID_GENERATOR :
				colseqtype = ColumnInfo.UUID_GENERATOR;
				break;
			default :
				colseqtype = ColumnInfo.NO_SEQUENCE_SELECTED;
		}

		getColumn().setSequenceType(colseqtype);
	}

	/**
	 * Get or set the sequence type of the column.
	 * The sequence type is one of:
	 *  - JSColumn.NONE
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
	 * 		pk.rowIdentifierType = JSColumn.PK_COLUMN;
	 * 		pk.setFlag(JSColumn.UUID_COLUMN, true)
	 * 		pk.sequenceType = JSColumn.UUID_GENERATOR
	 * 		table.createNewColumn("name", JSColumn.TEXT, 100);
	 * 		var result = server.synchronizeWithDB(table);
	 * 		if (result) application.output("Table users successfully created.");
	 * 		else application.output("Table users not created.");
	 * 	}
	 *
	 * 	// groups has database sequence pk
	 * 	table = server.createNewTable("groups");
	 * 	if (table)
	 * 	{
	 * 		pk = table.createNewColumn("id", JSColumn.INTEGER, 0);
	 * 		pk.rowIdentifierType = JSColumn.PK_COLUMN;
	 * 		pk.sequenceType = JSColumn.DATABASE_SEQUENCE
	 * 		pk.setDatabaseSequenceName('mygroupsequence')
	 * 		table.createNewColumn("name", JSColumn.TEXT, 100);
	 * 		result = server.synchronizeWithDB(table);
	 * 		if (result) application.output("Table groups successfully created.");
	 * 		else application.output("Table groups not created.");
	 * 	}
	 * }
	 */
	@Override
	public int js_getSequenceType() // override for jsdoc
	{
		return super.js_getSequenceType();
	}

	public void js_setRowIdentifierType(int type)
	{
		getColumn().setFlag(Column.PK_COLUMN, type == JSColumn.PK_COLUMN);
		getColumn().setFlag(Column.USER_ROWID_COLUMN, type == JSColumn.ROWID_COLUMN);
	}

	/**
	 * Get or set the row identifier type of the column.
	 * The sequence type is one of:
	 *  - JSColumn.PK_COLUMN
	 *  - JSColumn.ROWID_COLUMN
	 *  - JSColumn.NONE
	 *
	 * @sampleas js_getSequenceType()
	 */
	@Override
	public int js_getRowIdentifierType() // override for jsdoc
	{
		return super.js_getRowIdentifierType();
	}

	/**
	 * Set the database sequence name of the column, used for columns with sequence type JSColumn.DATABASE_SEQUENCE.
	 *
	 * @param sequenceName the sequence name
	 *
	 * @sampleas js_getSequenceType()
	 */
	public void js_setDatabaseSequenceName(String sequenceName)
	{
		getColumn().setDatabaseSequenceName(sequenceName);
	}
}
