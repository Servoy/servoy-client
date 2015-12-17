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

import java.util.Collection;

public interface ITableListener extends IItemChangeListener<ITable>
{
	public static final int ENABLED = 0x01;
	public static final int VALID = 0x02;

	/**
	 * This happens when the server "discovers" tables. This can happen when user creates tables, when server initially loads set of existing tables, when a
	 * server reload is triggered and so on.<BR>
	 * So whenever a table is added to the server's table list. So be careful, it does not only happen when user creates a table.
	 *
	 * @param server server that generated this event.
	 * @param tableNames the names of the added tables.
	 */
	void tablesAdded(IServerInternal server, String tableNames[]);

	/**
	 * This happens when the server no longer contains a set of tables. This can happen when user deletes tables, when a server reload is triggered and so
	 * on.<BR>
	 * So whenever a table is removed from the server's table list. So be careful, it does not only happen when user deletes a table.
	 *
	 * @param server the server that generated the event.
	 * @param tables the tables that disappeared.
	 * @param deleted if this happened due to the user really deleting a table.
	 */
	void tablesRemoved(IServerInternal server, ITable[] tables, boolean deleted);

	/**
	 * This happens when the hiddenInDeveloper flag of a Table changes value.
	 * @param server the server that contains that table.
	 * @param table the table that has been hidden/unhidden.
	 */
	void hiddenTableChanged(IServerInternal server, Table table);

	/**
	 * @param oldValue bit-mask showing the old state. For example (oldValue & ENABLED) == ENABLED means enabled = true.
	 * @param newValue bit-mask showing the new state. For example (oldValue & VALID) == VALID means valid = true.
	 */
	void serverStateChanged(IServerInternal server, int oldState, int newState);

	void tableInitialized(Table t);


	public static abstract class TableListener implements ITableListener
	{
		public void itemCreated(ITable item)
		{
		}

		public void itemRemoved(ITable item)
		{
		}

		public void itemChanged(ITable item)
		{
		}

		public void itemChanged(Collection<ITable> items)
		{
		}

		public void tablesAdded(IServerInternal server, String[] tableNames)
		{
		}

		public void tablesRemoved(IServerInternal server, Table[] tables, boolean deleted)
		{
		}

		public void hiddenTableChanged(IServerInternal server, Table table)
		{
		}

		public void serverStateChanged(IServerInternal server, int oldState, int newState)
		{
		}

		public void tableInitialized(Table t)
		{
		}
	}
}