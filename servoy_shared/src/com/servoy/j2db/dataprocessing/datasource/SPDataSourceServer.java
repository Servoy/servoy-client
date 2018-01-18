/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.dataprocessing.datasource;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.ProcedureColumn;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * Scope for datasources.db.myserver.
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class SPDataSourceServer extends DefaultJavaScope
{
	private static Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(SPDataSourceServer.class);
	private volatile IApplication application;

	private final String serverName;

	SPDataSourceServer(IApplication application, String serverName)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
		this.serverName = serverName;
	}

	@Override
	protected boolean fill()
	{
		// table and view names
		try
		{
			final IServer server = application.getRepository().getServer(serverName);
			if (server != null)
			{
				// TODO change to getProcedures()
				for (final Procedure proc : server.getProcedures())
				{
					put(proc.getName(), this, new Callable()
					{
						@Override
						public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
						{
							try
							{
								return application.getDataServer().executeProcedure(application.getClientID(), server.getName(),
									application.getFoundSetManager().getTransactionID(server.getName()), proc,
									getAsRightType(proc.getParameters(), unwrap(args)));
							}
							catch (RemoteException | ServoyException e)
							{
								Debug.error(e);
								throw new RuntimeException("error calling procedure '" + proc.getName() + "'", e);
							}
						}
					});
				}
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}

		return true;
	}

	private static Object[] unwrap(Object[] args)
	{
		if (args == null)
		{
			return null;
		}
		Object[] unwrapped = new Object[args.length];
		for (int i = 0; i < args.length; i++)
		{
			unwrapped[i] = (args[i] instanceof Wrapper) ? ((Wrapper)args[i]).unwrap() : args[i];
		}
		return unwrapped;
	}

	private static Object[] getAsRightType(List<ProcedureColumn> parameters, Object[] args)
	{
		if (args != null)
		{
			for (int i = 0; i < args.length; i++)
			{
				if (i <= parameters.size() - 1)
				{
					args[i] = Column.getAsRightType(parameters.get(i).getColumnType().getSqlType(), 0, args[i], 0, true, false);
				}
			}
		}
		return args;
	}

	/**
	 * Get the server name.
	 *
	 * @sample
	 * datasources.db.example_data.getServerName() // returns 'example_data'
	 *
	 * @return String server name
	 */
	@JSFunction
	public String getServerName()
	{
		return serverName;
	}

	/**
	 * Get the server where this server is a data model clone from.
	 *
	 * @sample
	 * datasources.db.example_data99.getDataModelCloneFrom().getServerName()
	 *
	 * @return DBDataSourceServer server
	 */
	@JSFunction
	public SPDataSourceServer getDataModelCloneFrom()
	{
		try
		{
			IServerInternal server = (IServerInternal)application.getRepository().getServer(serverName);
			if (server != null)
			{
				return new SPDataSourceServer(application, server.getConfig().getDataModelCloneFrom());
			}
		}
		catch (RemoteException e)
		{
			Debug.error(e);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public void destroy()
	{
		application = null;
		super.destroy();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + '(' + serverName + ')';
	}
}
