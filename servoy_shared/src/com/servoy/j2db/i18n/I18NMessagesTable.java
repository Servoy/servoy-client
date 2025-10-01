/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.i18n;

import java.sql.SQLException;
import java.sql.Types;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Debug;

/**
 * Utility class for creating an I18NB-messages table.
 *
 * @author rgansevles
 *
 */
public class I18NMessagesTable
{
	public static ITable createMessagesTable(IServerInternal server, String tablename, int primaryKeySequenceType) throws RepositoryException, SQLException
	{
		// Create the table in the repository.
		IValidateName validator = DummyValidator.INSTANCE;
		ITable table = server.createNewTable(validator, tablename);
		if (primaryKeySequenceType == ColumnInfo.UUID_GENERATOR)
		{
			Column column = table.createNewColumn(validator, "message_id", ColumnType.getInstance(IColumnTypes.TEXT, 36, 0));
			column.setDatabasePK(true);
			column.setSequenceType(primaryKeySequenceType);
			column.setFlag(IBaseColumn.UUID_COLUMN, true);
		}
		else
		{
			Column column = table.createNewColumn(validator, "message_id", ColumnType.getInstance(Types.INTEGER, 0, 0), false, true);
			column.setSequenceType(primaryKeySequenceType);
		}
		table.createNewColumn(validator, "message_key", ColumnType.getInstance(Types.VARCHAR, 150, 0), false);
		Column messageLanguage = table.createNewColumn(validator, "message_language", ColumnType.getInstance(Types.VARCHAR, 150, 0), true);
		table.createNewColumn(validator, "message_value", ColumnType.getInstance(Types.VARCHAR, 2000, 0), true);
		server.syncTableObjWithDB(table, false, false);

		try
		{
			server.createIndex(table, table.getName() + "_m_l", new Column[] { messageLanguage }, false);
		}
		catch (Exception e)
		{
			Debug.error("Failed to create an index on the messages table", e);
		}
		return table;
	}
}
