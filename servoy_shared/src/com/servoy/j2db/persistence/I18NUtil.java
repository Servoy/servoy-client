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
package com.servoy.j2db.persistence;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.ISQLActionTypes;
import com.servoy.j2db.dataprocessing.ISQLStatement;
import com.servoy.j2db.dataprocessing.SQLStatement;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryInsert;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.util.Utils;

/**
 * Utility class for loading/saving i18n texts in the repository
 *
 * @author gboros
 */
public class I18NUtil
{

	static public class MessageEntry
	{
		String language;
		String key;
		String value;

		public MessageEntry(String language, String key, String value)
		{
			this.language = language == null ? "" : language;
			this.key = key == null ? "" : key;
			this.value = value == null ? "" : value;
		}

		public String getLanguageKey()
		{
			return language + '.' + key;
		}

		public String getLanguage()
		{
			return language;
		}

		public String getKey()
		{
			return key;
		}

		public String getValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return getValue();
		}
	}

	public static void writeMessagesToRepository(String i18NServerName, String i18NTableName, IRepository repository, IDataServer dataServer, String clientID,
		TreeMap<String, MessageEntry> messages, boolean noUpdates, boolean noRemoves, String filterName, String[] filterValue, FoundSetManager fm)
			throws Exception
	{
		writeMessagesToRepository(i18NServerName, i18NTableName, repository, dataServer, clientID, messages, noUpdates, noRemoves, null, filterName,
			filterValue, fm);
	}

	public static void writeMessagesToRepository(String i18NServerName, String i18NTableName, IRepository repository, IDataServer dataServer, String clientID,
		TreeMap<String, MessageEntry> messages, boolean noUpdates, boolean noRemoves, TreeMap<String, MessageEntry> remoteMessages, String filterName,
		String[] filterValue, IFoundSetManagerInternal fm) throws Exception
	{
		// get remote messages snapshot
		if (remoteMessages == null)
			remoteMessages = loadSortedMessagesFromRepository(repository, dataServer, clientID, i18NServerName, i18NTableName, filterName, filterValue, fm);

		if (remoteMessages != null)
		{
			IServer i18NServer = repository.getServer(i18NServerName);
			Table i18NTable = null;
			if (i18NServer != null)
			{
				i18NTable = (Table)i18NServer.getTable(i18NTableName);
			}
			if (i18NTable != null)
			{
				Column pkColumn = i18NTable.getRowIdentColumns().get(0); // runtime exception when no ident columns

				QueryTable messagesTable = new QueryTable(i18NTable.getSQLName(), i18NTable.getDataSource(), i18NTable.getCatalog(), i18NTable.getSchema());
				QueryColumn pkCol = new QueryColumn(messagesTable, pkColumn.getID(), pkColumn.getSQLName(), pkColumn.getType(), pkColumn.getLength(),
					pkColumn.getScale(), pkColumn.getFlags());
				QueryColumn msgLang = new QueryColumn(messagesTable, -1, "message_language", Types.VARCHAR, 5, 0, 0);
				QueryColumn msgKey = new QueryColumn(messagesTable, -1, "message_key", Types.VARCHAR, 150, 0, 0);
				QueryColumn msgVal = new QueryColumn(messagesTable, -1, "message_value", Types.VARCHAR, 2000, 0, 0);

				ArrayList<SQLStatement> updateStatements = new ArrayList<SQLStatement>();
				// go thorough messages, update exiting, add news to remote

				// in case we need to insert a record, we must know if it is database managed or servoy managed
				boolean logIdIsServoyManaged = false;
				ColumnInfo ci = pkColumn.getColumnInfo();
				if (ci != null)
				{
					int autoEnterType = ci.getAutoEnterType();
					int autoEnterSubType = ci.getAutoEnterSubType();
					logIdIsServoyManaged = (autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER) && (autoEnterSubType != ColumnInfo.NO_SEQUENCE_SELECTED) &&
						(autoEnterSubType != ColumnInfo.DATABASE_IDENTITY);
				}

				Iterator<Map.Entry<String, MessageEntry>> messagesIte = messages.entrySet().iterator();
				Map.Entry<String, MessageEntry> messageEntry;
				while (messagesIte.hasNext())
				{
					messageEntry = messagesIte.next();
					String key = messageEntry.getKey();
					String value = messageEntry.getValue().getValue();
					String lang = messageEntry.getValue().getLanguage();
					if (lang.equals("")) lang = null;
					String messageKey = messageEntry.getValue().getKey();

					if (!remoteMessages.containsKey(key)) // insert
					{
						QueryInsert insert = new QueryInsert(messagesTable);
						QueryColumn[] insertColumns = null;
						Object[] insertColumnValues = null;
						if (logIdIsServoyManaged)
						{
							Object messageId = dataServer.getNextSequence(i18NServerName, i18NTableName, pkColumn.getName(), -1);
							if (lang == null)
							{
								insertColumns = new QueryColumn[] { pkCol, msgKey, msgVal };
								insertColumnValues = new Object[] { messageId, messageKey, value };
							}
							else
							{
								insertColumns = new QueryColumn[] { pkCol, msgKey, msgLang, msgVal };
								insertColumnValues = new Object[] { messageId, messageKey, lang, value };
							}
						}
						else
						{
							if (lang == null)
							{
								insertColumns = new QueryColumn[] { msgKey, msgVal };
								insertColumnValues = new Object[] { messageKey, value };
							}
							else
							{
								insertColumns = new QueryColumn[] { msgKey, msgLang, msgVal };
								insertColumnValues = new Object[] { messageKey, lang, value };
							}
						}

						Column filterColumn = (Column)i18NTable.getColumn(filterName);
						if (filterColumn != null && filterValue != null && filterValue.length > 0)
						{
							insertColumns = Utils.arrayAdd(insertColumns, new QueryColumn(messagesTable, filterColumn.getID(), filterColumn.getSQLName(),
								filterColumn.getType(), filterColumn.getLength(), filterColumn.getScale(), filterColumn.getFlags()), true);
							insertColumnValues = Utils.arrayAdd(insertColumnValues, filterValue[0], true);
						}

						insert.setColumnValues(insertColumns, insertColumnValues);

						updateStatements.add(new SQLStatement(ISQLActionTypes.INSERT_ACTION, i18NServerName, i18NTableName, null, null, insert, null));
					}
					else if (!remoteMessages.get(key).getValue().equals(value) && !noUpdates) // update
					{
						QueryUpdate update = new QueryUpdate(messagesTable);
						update.addValue(msgVal, value);
						update.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgKey, messageKey));
						update.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgLang, lang));

						if (filterName != null)
						{
							Column filterColumn = (Column)i18NTable.getColumn(filterName);
							if (filterColumn != null && filterValue != null && filterValue.length > 0)
							{
								QueryColumn columnFilter = new QueryColumn(messagesTable, filterColumn.getID(), filterColumn.getSQLName(),
									filterColumn.getType(), filterColumn.getLength(), filterColumn.getScale(), filterColumn.getFlags());
								CompareCondition cc = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, columnFilter,
									new QueryColumnValue(filterValue[0], null));
								update.addCondition("FILTER", cc); //$NON-NLS-1$
							}
						}

						updateStatements.add(new SQLStatement(ISQLActionTypes.UPDATE_ACTION, i18NServerName, i18NTableName, null, null, update,
							fm != null ? fm.getTableFilterParams(i18NServerName, update) : null));
					}
				}

				if (!noRemoves)
				{
					// go thorough remote, remove if not existing locally
					Iterator<Map.Entry<String, MessageEntry>> remoteMessagesIte = remoteMessages.entrySet().iterator();
					Map.Entry<String, MessageEntry> remoteMessageEntry;
					while (remoteMessagesIte.hasNext())
					{
						remoteMessageEntry = remoteMessagesIte.next();
						String key = remoteMessageEntry.getKey();
						if (!messages.containsKey(key)) // delete
						{
							String lang = remoteMessageEntry.getValue().getLanguage();
							if (lang.equals("")) lang = null;
							String messageKey = remoteMessageEntry.getValue().getKey();

							QueryDelete delete = new QueryDelete(messagesTable);
							delete.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgKey, messageKey));
							delete.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgLang, lang));

							if (filterName != null)
							{
								Column filterColumn = (Column)i18NTable.getColumn(filterName);
								if (filterColumn != null && filterValue != null && filterValue.length > 0)
								{
									QueryColumn columnFilter = new QueryColumn(messagesTable, filterColumn.getID(), filterColumn.getSQLName(),
										filterColumn.getType(), filterColumn.getLength(), filterColumn.getScale(), filterColumn.getFlags());
									CompareCondition cc = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, columnFilter,
										new QueryColumnValue(filterValue[0], null));
									delete.addCondition(cc);
								}
							}

							updateStatements.add(new SQLStatement(ISQLActionTypes.DELETE_ACTION, i18NServerName, i18NTableName, null, null, delete,
								fm != null ? fm.getTableFilterParams(i18NServerName, delete) : null));

						}
					}
				}
				for (SQLStatement st : updateStatements)
				{
					st.setDataType(ISQLStatement.I18N_DATA_TYPE);
				}
				dataServer.performUpdates(clientID, updateStatements.toArray(new ISQLStatement[updateStatements.size()]));

			}
		}
	}

	public static TreeMap<String, MessageEntry> loadSortedMessagesFromRepository(IRepository repository, IDataServer dataServer, String clientID,
		String i18NServerName, String i18NTableName, String filterName, String[] filterValue, IFoundSetManagerInternal fm) throws Exception
	{
		TreeMap<String, MessageEntry> sortedMessages = new TreeMap<String, MessageEntry>();

		IServer i18NServer = repository.getServer(i18NServerName);
		if (i18NServer != null)
		{
			Table i18NTable = (Table)i18NServer.getTable(i18NTableName);
			if (i18NTable != null)
			{
				QueryTable messagesTable = new QueryTable(i18NTable.getSQLName(), i18NTable.getDataSource(), i18NTable.getCatalog(), i18NTable.getSchema());
				QuerySelect sql = new QuerySelect(messagesTable);

				QueryColumn msgLang = new QueryColumn(messagesTable, -1, "message_language", Types.VARCHAR, 5, 0, 0);
				QueryColumn msgKey = new QueryColumn(messagesTable, -1, "message_key", Types.VARCHAR, 150, 0, 0);
				QueryColumn msgVal = new QueryColumn(messagesTable, -1, "message_value", Types.VARCHAR, 2000, 0, 0);

				sql.addColumn(msgLang);
				sql.addColumn(msgKey);
				sql.addColumn(msgVal);

				if (filterName != null)
				{
					Column filterColumn = (Column)i18NTable.getColumn(filterName);
					if (filterColumn != null && filterValue != null && filterValue.length > 0)
					{
						QueryColumn columnFilter = new QueryColumn(messagesTable, filterColumn.getID(), filterColumn.getSQLName(), filterColumn.getType(),
							filterColumn.getLength(), filterColumn.getScale(), filterColumn.getFlags());
						CompareCondition cc = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, columnFilter, new QueryColumnValue(filterValue[0], null));
						sql.addCondition("FILTER", cc); //$NON-NLS-1$
					}
				}

				sql.addSort(new QuerySort(msgLang, true));
				sql.addSort(new QuerySort(msgKey, true));

				IDataSet set = dataServer.performQuery(clientID, i18NServerName, null, sql, fm != null ? fm.getTableFilterParams(i18NServerName, sql) : null,
					false, 0, Integer.MAX_VALUE, IDataServer.MESSAGES_QUERY);
				int rowCount = set.getRowCount();
				if (rowCount > 0)
				{
					for (int i = 0; i < rowCount; i++)
					{
						Object[] row = set.getRow(i);
						MessageEntry messageEntry = new MessageEntry((String)row[0], (String)row[1], (String)row[2]);
						sortedMessages.put(messageEntry.getLanguageKey(), messageEntry);
					}
				}
			}
		}

		return sortedMessages;
	}

}
