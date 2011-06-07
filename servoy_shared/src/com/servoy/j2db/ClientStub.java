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
package com.servoy.j2db;


import java.util.Stack;

import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;

/**
 * Remote class for server calls to client
 * 
 * @author jblok
 */
public class ClientStub implements IUserClient
{
	private transient final ClientState client;

	public ClientStub(ClientState c)
	{
		client = c;
	}

	public void alert(final String msg)
	{
		client.invokeLater(new Runnable()
		{
			public void run()
			{
				client.reportInfo(msg); //Messages.getString("servoy.userClient.message.fromServer")
			}
		});
	}

	public boolean isAlive()
	{
		return true;//just rerurn call
	}

	public void shutDown()
	{
		client.invokeLater(new Runnable()
		{
			public void run()
			{
				client.shutDown(true);
			}
		}, true);
	}

	public void closeSolution()
	{
		client.invokeLater(new Runnable()
		{
			public void run()
			{
				client.closeSolution(true, null);
				client.reportInfo(client.getI18NMessage("servoy.client.message.remotesolutionclose")); //$NON-NLS-1$
			}
		}, true);
	}

	public void flushCachedDatabaseData(final String dataSource)
	{
		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				((FoundSetManager)client.getFoundSetManager()).flushCachedDatabaseDataFromRemote(dataSource);
			}
		});
	}


	private final Stack<Object[]> datachanges = new Stack<Object[]>();
	private Runnable datachangesHandler;

	public void notifyDataChange(final String server_name, final String table_name, final IDataSet pks, final int action, final Object[] insertColumnData)
	{
		if (Debug.tracing())
		{
			Debug.trace("Notify Data Change get from the server for dataserver: " + server_name + " table: " + table_name); //$NON-NLS-1$ //$NON-NLS-2$
		}
		synchronized (datachanges)
		{
			datachanges.push(new Object[] { server_name, table_name, pks, new Integer(action), insertColumnData });
			if (datachangesHandler == null)
			{
				datachangesHandler = new Runnable()
				{
					public void run()
					{
						while (datachangesHandler != null)
						{
							final Object[] array;
							synchronized (datachanges)
							{
								if (datachanges.isEmpty())
								{
									datachangesHandler = null; // done
									break;
								}
								array = datachanges.pop();
							}

							client.invokeLater(new Runnable()
							{
								public void run()
								{
									String server_name = (String)array[0];
									String table_name = (String)array[1];
									IDataSet pks = (IDataSet)array[2];
									int action = ((Integer)array[3]).intValue();
									Object[] insertColumnData = (Object[])array[4];

									IDataServer ds = client.getDataServer();
									if (ds instanceof DataServerProxy)
									{
										server_name = ((DataServerProxy)ds).getReverseMappedServerName(server_name);
									}
									((FoundSetManager)client.getFoundSetManager()).notifyDataChange(
										DataSourceUtils.createDBTableDataSource(server_name, table_name), pks, action, insertColumnData);
								}
							});
						}
					}
				};
				client.getScheduledExecutor().execute(datachangesHandler);
			}
		}
	}

	public void activateSolutionMethod(final String globalMethodName, final StartupArguments argumentsScope)
	{
		client.invokeLater(new Runnable()
		{
			public void run()
			{
				client.activateSolutionMethod(globalMethodName, argumentsScope);
			}
		});
	}

	public ClientInfo getClientInfo()
	{
		return client.getClientInfo();
	}
}
