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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dltk.rhino.dbgp.LazyInitScope;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.ProcedureColumn;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * <p>Execute specific stored procedures directly for any defined data source using the <code>datasources.sp.&lt;servername&gt;.&lt;storedProcedure&gt;()</code> syntax.</p>
 *
 * <h2>Specific Scope for <code>datasources.sp.&lt;servername&gt;.storedProcedure()</code></h2>
 * <p>After enabling stored procedures as outlined in Plot 1, individual procedures can be directly accessed for specific data sources via the syntax
 * <code>datasources.sp.&lt;servername&gt;.&lt;storedProcedure&gt;()</code>. This specific scope allows efficient retrieval and manipulation of data,
 * using Servoy scripting to control execution.</p>
 *
 * <h3>Calling a Stored Procedure</h3>
 * <p>To call a stored procedure:</p>
 * <ol>
 *   <li>Enable procedures for the database server (see <b>Enabling Stored Procedures</b> in Plot 1).</li>
 *   <li>Use code completion to navigate to the desired procedure under <code>datasources.sp.&lt;servername&gt;.&lt;storedProcedureName&gt;()</code>.</li>
 * </ol>
 *
 * <p><b>Example:</b> Calling a specific stored procedure from a defined server</p>
 * <pre>
 * var results = datasources.sp.myserver.calculateInterest(3000, 'USD');
 * </pre>
 *
 * <h3>Handling Result Sets</h3>
 * <p>The result of calling a stored procedure is a <b>JSDataSet</b>, which can be processed further. Some practical ways to work with the dataset include:</p>
 * <ul>
 *   <li><b>Iteration</b>: Use <code>.getValue(row, column)</code> to access each data row returned from the procedure.</li>
 *   <li><b>In-Memory Conversion</b>: If required, use <b>Create In-Memory Table from Procedure</b> to store the returned data as a temporary table,
 *       useful for session-based calculations or cache-like functionality.</li>
 * </ul>
 *
 * <h3>Considerations and Best Practices</h3>
 * <ul>
 *   <li><b>Parameter Handling</b>: Ensure that input parameters match the procedure’s expected data types, especially when working with complex queries.</li>
 *   <li><b>Data Lifecycle</b>: In-memory tables created from stored procedures are session-specific, meaning they clear automatically at the end of each session.</li>
 *   <li><b>Error Monitoring</b>: Stored procedures may generate exceptions if there are issues within the procedure logic. Verify and handle these in the database for smoother client interactions.</li>
 *   <li><b>Performance Optimization</b>: Using stored procedures can reduce network load and offload processing to the database, which can enhance overall performance,
 *       especially for batch updates or large data transformations.</li>
 * </ul>
 *
 * <h3>Remarks</h3>
 * <p>Stored procedures provide powerful tools for offloading operations to the database server, especially useful for data manipulation that benefits
 * from database processing power rather than client-side execution.</p>
 *
 * <p>For details on how to use Stored Procedures with Servoy, refer to the <a href="https://docs.servoy.com/guides/develop/application-design/data-modeling/databases/procedures">Procedures</a> section of this documentation.</p>
 *
 * @author rgansevles
 *
 * @since 8.3
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class SPDataSourceServer extends DefaultJavaScope implements LazyInitScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(SPDataSourceServer.class);
	private volatile IApplication application;

	private final String serverName;

	SPDataSourceServer(IApplication application, String serverName)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
		this.serverName = serverName;
	}

	@Override
	public Object[] getInitializedIds()
	{
		return getRealIds();
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
								IDataSet[] datasets = application.getDataServer().executeProcedure(application.getClientID(), server.getName(),
									application.getFoundSetManager().getTransactionID(server.getName()), proc,
									getAsRightType(proc.getParameters(), unwrap(args)));

								if (datasets == null)
								{
									return null;
								}

								if (datasets.length == 1)
								{
									// single dataset
									return cx.getWrapFactory().wrap(cx, scope, datasets[0], null);
								}

								// return an object with result keys and array-index
								Scriptable obj = cx.newObject(scope);
								// columns sets are sorted
								List<String> keys = new ArrayList<>(proc.getColumns().keySet());
								for (int i = 0; i < datasets.length; i++)
								{
									Object wrapped = cx.getWrapFactory().wrap(cx, scope, datasets[i], null);
									obj.put(i, obj, wrapped);
									if (i < keys.size())
									{
										obj.put(keys.get(i), obj, wrapped);
									}
								}

								return obj;
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
					args[i] = Column.getAsRightType(parameters.get(i).getColumnType(), 0, args[i], true, false);
				}
			}
		}
		return args;
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
