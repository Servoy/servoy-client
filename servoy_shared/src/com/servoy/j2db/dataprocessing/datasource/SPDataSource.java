/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;

/**
 *
 * <p>Access and execute stored procedures dynamically at runtime from any enabled data source using the <code>datasources.sp</code> object.</p>
 *
 * <h2>Runtime Access to Stored Procedures in <code>datasources.sp</code></h2>
 * <p>The <code>datasources.sp</code> object provides runtime access to all stored procedures enabled across supported data sources.
 * This access allows dynamic execution of procedures directly within Servoy’s scripting environment, enabling developers to interact
 * with the database more flexibly without needing SQL statements.</p>
 *
 * <h3>Enabling Stored Procedures</h3>
 * <p>To make stored procedures accessible in your solution:</p>
 * <ol>
 *   <li>Open the configuration settings for your database server.</li>
 *   <li>Under Advanced Server Settings, check the <b>Enable Procedures</b> option to allow stored procedures to appear.</li>
 *   <li><b>Restart</b> Servoy Developer to apply the changes.</li>
 * </ol>
 * <p>After enabling, stored procedures appear under the Procedures node in the solution explorer.
 * This setup allows the procedures to be accessed directly in code via the <code>datasources.sp</code> object.</p>
 *
 * <p><b>Example:</b> Accessing stored procedures under a server</p>
 * <pre>
 * var dataset = datasources.sp.myserver.mystoredprocedure(param1, param2);
 * </pre>
 *
 * <h3>Accessing Stored Procedures</h3>
 * <p>The <code>datasources.sp</code> structure organizes stored procedures by server. The syntax for accessing a procedure follows this format:</p>
 * <ul>
 *   <li><code>datasources.sp.&lt;servername&gt;.&lt;storedProcedureName&gt;()</code></li>
 * </ul>
 * <p>This syntax is available with code completion, making it easy to view and select from available stored procedures for each server.</p>
 *
 * <h3>Use Case Examples:</h3>
 * <ul>
 *   <li><b>Data Retrieval</b>: Retrieve filtered datasets or calculated results from stored procedures, improving performance by handling data operations within the database.</li>
 *   <li><b>Batch Processing</b>: Use stored procedures for complex, multi-step operations that are better handled by the database itself.</li>
 * </ul>
 *
 * <h3>Remarks</h3>
 * <ul>
 *   <li><b>Execution Context</b>: The logic within stored procedures is managed within the database. Any required changes to procedure logic should be handled in the database environment.</li>
 *   <li><b>In-Memory Data Source Support</b>: You can create an in-memory data source (temp table) directly from a stored procedure’s dataset, which is useful for handling temporary data during a session.</li>
 *   <li><b>Error Handling</b>: Procedures may throw database-side exceptions; handling these at the database layer will prevent disruption in the Servoy client.</li>
 * </ul>
 *
 * <p>For details on how to use Stored Procedures with Servoy, refer to the <a href="../../../guides/develop/application-design/data-modeling/databases/procedures.md">Procedures</a> section of this documentation.</p>
 *
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class SPDataSource extends DefaultJavaScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(SPDataSource.class);
	private volatile IApplication application;

	SPDataSource(IApplication application)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
	}

	@Override
	protected boolean fill()
	{
		// server names
		try
		{
			for (String serverName : application.getRepository().getServerNames(false))
			{
				put(serverName, this, new SPDataSourceServer(application, serverName));
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}

		return true;
	}

	@Override
	public void destroy()
	{
		application = null;
		super.destroy();
	}
}
