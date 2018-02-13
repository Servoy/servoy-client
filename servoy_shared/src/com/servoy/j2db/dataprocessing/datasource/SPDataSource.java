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

import java.rmi.RemoteException;
import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class SPDataSource extends DefaultJavaScope
{
	private static Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(SPDataSource.class);
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

	@Override
	public void destroy()
	{
		application = null;
		super.destroy();
	}
}
