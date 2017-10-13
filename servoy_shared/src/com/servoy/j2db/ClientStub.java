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


import java.rmi.RemoteException;
import java.util.Stack;

import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.scripting.StartupArguments;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IGetLastAccessed;
import com.servoy.j2db.util.IGetStatusLine;

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
		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				client.invokeLater(new Runnable()
				{
					public void run()
					{
						client.reportInfo(msg); //Messages.getString("servoy.userClient.message.fromServer")
					}
				});
			}
		});
	}

	public boolean isAlive()
	{
		return true;//just rerurn call
	}

	public void shutDown()
	{
		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				client.invokeLater(new Runnable()
				{
					public void run()
					{
						synchronized (ClientStub.this)
						{
							client.shutDown(true);
						}
					}
				}, true);
			}
		});
	}

	public void closeSolution()
	{
		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				client.invokeLater(new Runnable()
				{
					public void run()
					{
						synchronized (ClientStub.this)
						{
							client.closeSolution(true, null);
							client.reportInfo(client.getI18NMessage("servoy.client.message.remotesolutionclose")); //$NON-NLS-1$
						}
					}
				}, true);
			}
		});
	}

	public void flushCachedDatabaseData(final String dataSource)
	{
		if (client.isShutDown()) return;
		if (Debug.tracing())
		{
			Debug.trace("Flush Cached Data received for datasource: " + dataSource);
		}

		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				Runnable r = new Runnable()
				{
					public void run()
					{
						IDataServer dataServer = client.getDataServer();
						if (dataServer instanceof DataServerProxy)
						{
							String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(dataSource);
							if (dbServernameTablename != null)
							{
								// map from real db server to server names from before switch-server
								for (String srv : ((DataServerProxy)dataServer).getReverseMappedServerNames(dbServernameTablename[0]))
								{
									((FoundSetManager)client.getFoundSetManager()).flushCachedDatabaseDataFromRemote(
										DataSourceUtils.createDBTableDataSource(srv, dbServernameTablename[1]));
								}
								return;
							}
						}

						((FoundSetManager)client.getFoundSetManager()).flushCachedDatabaseDataFromRemote(dataSource);
					}
				};

				if (client.isEventDispatchThread())
				{
					r.run();
				}
				else
				{
					client.invokeLater(r);
				}
			}
		});
	}

	private final Stack<Object[]> datachanges = new Stack<Object[]>();
	private Runnable datachangesHandler;

	public void notifyDataChange(final String server_name, final String table_name, final IDataSet pks, final int sql_action, final Object[] insertColumnData)
	{
		if (client.isShutDown()) return;
		if (Debug.tracing())
		{
			Debug.trace("Notify Data Change get from the server for dataserver: " + server_name + " table: " + table_name + ". Pks: " + //$NON-NLS-1$//$NON-NLS-2$
				(pks != null ? "not null" : "null") + ". Action type: " + sql_action);
		}
		synchronized (datachanges)
		{
			datachanges.push(new Object[] { server_name, table_name, pks, new Integer(sql_action), insertColumnData });
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
									if (client.isShutDown() || !client.isSolutionLoaded()) return;
									String sname = (String)array[0];
									String tname = (String)array[1];
									IDataSet pksDataSet = (IDataSet)array[2];
									int action = ((Integer)array[3]).intValue();
									Object[] insertColumndata = (Object[])array[4];

									IDataServer ds = client.getDataServer();
									if (ds instanceof DataServerProxy)
									{
										// possibly switched from multiple servers to the same destination server.
										for (String srv : ((DataServerProxy)ds).getReverseMappedServerNames(sname))
										{
											((FoundSetManager)client.getFoundSetManager()).notifyDataChange(DataSourceUtils.createDBTableDataSource(srv, tname),
												pksDataSet, action, insertColumndata);
										}
										return;
									}
									// not switched
									((FoundSetManager)client.getFoundSetManager()).notifyDataChange(DataSourceUtils.createDBTableDataSource(sname, tname),
										pksDataSet, action, insertColumndata);
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
		client.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				client.invokeLater(new Runnable()
				{
					public void run()
					{
						client.activateSolutionMethod(globalMethodName, argumentsScope);
					}
				});
			}
		});
	}

	@Override
	public String getClientStatusLine() throws RemoteException
	{
		if (client instanceof IGetStatusLine)
		{
			return ((IGetStatusLine)client).getStatusLine();
		}

		return null;
	}

	public long getLastAccessedTime() throws RemoteException
	{
		if (client instanceof IGetLastAccessed)
		{
			return ((IGetLastAccessed)client).getLastAccessedTime();
		}

		return 0l;
	}


}
