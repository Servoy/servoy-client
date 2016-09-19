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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.IDestroyable;

/**
 * In scripting: <pre>datasources</pre>
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Datasources", scriptingName = "datasources")
public class JSDataSources implements IDestroyable
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSDataSources.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { DBDataSource.class, MemDataSource.class, JSDataSource.class, DBDataSourceServer.class };
			}
		});
	}
	private volatile IApplication application;

	public JSDataSources(IApplication application)
	{
		this.application = application;
	}

	private DBDataSource db;

	/**
	 * Scope property for server/table based data sources.
	 *
	 * @sample
	 * datasources.db.example_data.orders
	 */
	@JSReadonlyProperty
	public DBDataSource db()
	{
		if (db == null)
		{
			db = new DBDataSource(application);
		}
		return db;
	}

	private MemDataSource mem;

	/**
	 * Scope property for in-memory data sources.
	 *
	 * @sample
	 * datasources.mem['myds']
	 */
	@JSReadonlyProperty
	public MemDataSource mem()
	{
		if (mem == null)
		{
			mem = new MemDataSource(application);
		}
		return mem;
	}

	public void destroy()
	{
		if (db != null)
		{
			db.destroy();
			db = null;
		}
		if (mem != null)
		{
			mem.destroy();
			mem = null;
		}
		application = null;
	}
}