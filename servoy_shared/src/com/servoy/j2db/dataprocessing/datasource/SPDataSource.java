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
 * <pre data-puremarkdown>
## Overview
Access and execute stored procedures dynamically at runtime from any enabled data source using the `datasources.sp` object.

## Runtime Access to Stored Procedures in `datasources.sp`

The `datasources.sp` object provides runtime access to all stored procedures enabled across supported data sources. This access allows dynamic execution of procedures directly within Servoy’s scripting environment, enabling developers to interact with the database more flexibly without needing SQL statements.

### Enabling Stored Procedures
To make stored procedures accessible in your solution:
1. Open the configuration settings for your database server.
2. Under Advanced Server Settings, check the **Enable Procedures** option to allow stored procedures to appear.
3. **Restart** Servoy Developer to apply the changes.

After enabling, stored procedures appear under the Procedures node in the solution explorer. This setup allows the procedures to be accessed directly in code via the `datasources.sp` object.

Example: Accessing stored procedures under a server
`var dataset = datasources.sp.myserver.mystoredprocedure(param1, param2);`

### Accessing Stored Procedures
The `datasources.sp` structure organizes stored procedures by server. The syntax for accessing a procedure follows this format:
- **`datasources.sp.<servername>.<storedProcedureName>()`**
This syntax is available with code completion, making it easy to view and select from available stored procedures for each server.

**Use Case Examples:**
- **Data Retrieval**: Retrieve filtered datasets or calculated results from stored procedures, improving performance by handling data operations within the database.
- **Batch Processing**: Use stored procedures for complex, multi-step operations that are better handled by the database itself.

### Remarks
- **Execution Context**: The logic within stored procedures is managed within the database. Any required changes to procedure logic should be handled in the database environment.
- **In-Memory Data Source Support**: You can create an in-memory data source (temp table) directly from a stored procedure’s dataset, which is useful for handling temporary data during a session.
- **Error Handling**: Procedures may throw database-side exceptions; handling these at the database layer will prevent disruption in the Servoy client.

For details on how to use Stored Procedures with Servoy, refer to the [Procedures](../../../guides/develop/application-design/data-modeling/databases/procedures.md) section of this documentation

 * </pre>
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
