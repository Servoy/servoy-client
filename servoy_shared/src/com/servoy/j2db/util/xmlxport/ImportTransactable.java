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

package com.servoy.j2db.util.xmlxport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITransactable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Debug;

public class ImportTransactable implements ITransactable
{
	Map<IServerInternal, List<Table>> tablesByServer = new HashMap<IServerInternal, List<Table>>();

	public void addTable(IServerInternal server, Table table)
	{
		List<Table> tableList = tablesByServer.get(server);
		if (tableList == null)
		{
			tableList = new ArrayList<Table>();
			tablesByServer.put(server, tableList);
		}
		tableList.add(table);
	}

	public void processPostCommit()
	{
		// Do nothing.
	}

	public void processPostRollBack()
	{
		Iterator<Map.Entry<IServerInternal, List<Table>>> iterator = tablesByServer.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<IServerInternal, List<Table>> entry = iterator.next();
			IServerInternal server = entry.getKey();
			List<Table> tableList = entry.getValue();
			try
			{
				server.flushTables(tableList);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	public Map<IServerInternal, List<Table>> getTablesByServer()
	{
		return tablesByServer;
	}
}