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

import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;

/**
 * The <code>DBDataSource</code> class provides a utility API to access and manage all currently
 * available datasources within the Servoy application, including those from all valid servers and tables.
 * Accessed in scripting through <code>datasources.db</code>, this API facilitates interaction with
 * database servers, tables, and their associated records and schemas.
 *
 * For further reference, see:
  <ul>
 * 	<li><a href="https://docs.servoy.com/reference/servoycore/dev-api/datasources/dbdatasourceserver">DBDataSourceServer</a></li>
 * 	<li><a href="https://docs.servoy.com/reference/servoycore/dev-api/datasources">Datasources</a></li>
 * </ul>
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class DBDataSource extends DefaultJavaScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(DBDataSource.class);
	private volatile IApplication application;

	DBDataSource(IApplication application)
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
				put(serverName, this, new DBDataSourceServer(application, serverName));
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
