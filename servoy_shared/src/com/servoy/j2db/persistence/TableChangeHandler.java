/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class TableChangeHandler extends ItemChangeHandler<IServerInternal, ITable>
{
	private static TableChangeHandler tableChangeHandler = new TableChangeHandler();

	private TableChangeHandler()
	{
	}

	public static TableChangeHandler getInstance()
	{
		return tableChangeHandler;
	}

	public void fireTablesRemoved(IServerInternal server, ITable removedTables[], boolean deleted)
	{
		if (removedTables != null && removedTables.length > 0)
		{
			List<IItemChangeListener<ITable>> itemListeners = listeners.get(server);
			if (itemListeners != null)
			{
				for (IItemChangeListener<ITable> listener : itemListeners)
				{
					try
					{
						((ITableListener)listener).tablesRemoved(server, removedTables, deleted);

					}
					catch (Exception ex)
					{
						Debug.error(ex);//an exception should never interupt the process
					}
				}
			}
		}
	}

	public void fireStateChanged(IServerInternal server, int oldState, int newState)
	{
		List<IItemChangeListener<ITable>> itemListeners = listeners.get(server);
		if (itemListeners != null)
		{
			for (IItemChangeListener<ITable> listener : itemListeners)
			{
				try
				{
					((ITableListener)listener).serverStateChanged(server, oldState, newState);

				}
				catch (Exception ex)
				{
					Debug.error(ex);//an exception should never interupt the process
				}
			}
		}
	}

	public void fireTablesAdded(IServerInternal server, String tableNames[])
	{
		if (tableNames != null && tableNames.length > 0)
		{
			List<IItemChangeListener<ITable>> itemListeners = listeners.get(server);
			if (itemListeners != null)
			{
				List<IItemChangeListener<ITable>> itemListenersCopy = new CopyOnWriteArrayList<IItemChangeListener<ITable>>(itemListeners);
				for (IItemChangeListener<ITable> listener : itemListenersCopy)
				{
					try
					{
						((ITableListener)listener).tablesAdded(server, tableNames);

					}
					catch (Exception ex)
					{
						Debug.error(ex);//an exception should never interupt the process
					}
				}
			}
		}
	}

	public void fireHiddenChanged(IServerInternal server, Table changedTable)
	{
		List<IItemChangeListener<ITable>> itemListeners = listeners.get(server);
		if (itemListeners != null)
		{
			for (IItemChangeListener<ITable> listener : itemListeners)
			{
				try
				{
					((ITableListener)listener).hiddenTableChanged(server, changedTable);

				}
				catch (Exception ex)
				{
					Debug.error(ex);//an exception should never interupt the process
				}
			}
		}
	}

	public void fireTableInitialized(IServerInternal server, Table t)
	{
		if (t != null)
		{
			List<IItemChangeListener<ITable>> itemListeners = listeners.get(server);
			if (itemListeners != null)
			{
				for (IItemChangeListener<ITable> listener : itemListeners)
				{
					try
					{
						((ITableListener)listener).tableInitialized(t);

					}
					catch (Exception ex)
					{
						Debug.error(ex);//an exception should never interupt the process
					}
				}
			}
		}
	}
}
