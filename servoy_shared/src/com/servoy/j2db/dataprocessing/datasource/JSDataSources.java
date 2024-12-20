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

import static com.servoy.base.util.DataSourceUtilsBase.isCompleteDBbServerTable;
import static com.servoy.j2db.util.DataSourceUtils.getDBServernameTablename;
import static com.servoy.j2db.util.DataSourceUtils.getInmemDataSourceName;

import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.IDestroyable;

/**
 * <p><code>Datasources</code> in Servoy provide a structured way to interact with various
 * types of data sources, such as database tables, in-memory tables, views, and stored
 * procedures. These <code>datasources</code> are accessible through the <code>datasources</code>
 * object and support dynamic code completion based on the solution’s data model, enabling
 * efficient development.</p>
 *
 * <p><code>Datasources</code> include types like <code>DBDataSource</code>,
 * <code>MemDataSource</code>, <code>ViewDataSource</code>, <code>SPDataSource</code>, and
 * <code>MenuDataSource</code>, each catering to a specific use case. For example,
 * <code>db</code> is used for server/table-based data sources, <code>mem</code> for in-memory
 * tables, and <code>view</code> for view foundset data sources. Stored procedures are managed
 * under the <code>sp</code> property, with server-side configuration enabling their use. The
 * <code>menu</code> property handles menu-related datasources.</p>
 *
 * <p>For details related to datasources, refer to the specific sections in the Servoy documentation:</p>
 * <p><a href="./dbdatasourceserver.md">DBDataSourceServer</a></p>
 * <p><a href="../../../../guides/develop/application-design/data-modeling/in-memory-databases.md#create-in-memory-datasource">
 * Create In Memory DataSource</a></p>
 * <p><a href="../../../../guides/develop/application-design/data-modeling/view-datasource.md">
 * View Foundset Datasource</a></p>
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
				return new Class< ? >[] { SPDataSource.class, SPDataSourceServer.class, DBDataSource.class, MemDataSource.class, JSDataSource.class, JSConnectionDefinition.class, DBDataSourceServer.class, ViewDataSource.class, MenuDataSource.class };
			}
		});
	}
	private volatile IApplication application;

	public JSDataSources(IApplication application)
	{
		this.application = application;
	}

	private DBDataSource db;
	private MemDataSource mem;
	private ViewDataSource view;
	private SPDataSource sp;
	private MenuDataSource menuDataSource;

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

	/**
	 * Scope property for view foundset data sources.
	 *
	 * @sample
	 * datasources.view['myds']
	 */
	@JSReadonlyProperty
	public ViewDataSource view()
	{
		if (view == null)
		{
			view = new ViewDataSource(application);
		}
		return view;
	}

	/**
	 * Scope property for stored procedures.
	 * This will list the stored procedures of server that have this property enabled (see server editor).
	 *
	 * @sample
	 * datasources.sp.servername.mystoredproc();
	 */
	@JSReadonlyProperty
	public SPDataSource sp()
	{
		if (sp == null)
		{
			sp = new SPDataSource(application);
		}
		return sp;
	}

	/**
	 * Scope property for view foundset data sources.
	 *
	 * @sample
	 * datasources.view['myds']
	 */
	@JSReadonlyProperty
	public MenuDataSource menu()
	{
		if (menuDataSource == null)
		{
			menuDataSource = new MenuDataSource(application);
		}
		return menuDataSource;
	}

	/**
	 * Scope getter for a datasource node based on a JSFoundset/JSRecord/ViewFoundset/ViewRecord
	 *
	 * @sample
	 * datasources.get(recordOrFoundset)
	 *
	 * @param datasource
	 *
	 * @return a JSDataSource based on parameter
	 */
	@JSFunction
	public JSDataSource get(Object recordOrFoundset)
	{
		var unwrapped = recordOrFoundset instanceof Wrapper w ? w.unwrap() : recordOrFoundset;
		String datasource = null;
		if (unwrapped instanceof IFoundSet fs)
		{
			datasource = fs.getDataSource();
		}
		else if (unwrapped instanceof IRecord r && r.getParentFoundSet() != null)
		{
			datasource = r.getParentFoundSet().getDataSource();
		}
		return get(datasource);
	}

	/**
	 * Scope getter for a datasource node based on datasource string.
	 *
	 * @sample
	 * datasources.get(datasource)
	 *
	 * @param datasource
	 *
	 * @return a JSDataSource based on parameter
	 */
	@JSFunction
	public JSDataSource get(String datasource)
	{
		String[] stn = getDBServernameTablename(datasource);
		if (isCompleteDBbServerTable(stn))
		{
			return new JSDataSource(application, datasource);
		}
		if (getInmemDataSourceName(datasource) != null)
		{
			return new JSDataSource(application, datasource);
		}
		//TODO view
		return null;
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
		if (view != null)
		{
			view.destroy();
			view = null;
		}
		if (sp != null)
		{
			sp.destroy();
			sp = null;
		}
		application = null;
	}
}