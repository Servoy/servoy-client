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
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;


/**
 * In-Memory Databases Overview
 *
 * In addition to using tables stored on real database servers, Servoy allows you to create **In-Memory** tables using HSQL.
 * These tables are configured like regular database tables and can be populated using `JSDataSet` and the `createDataSource()` method.
 *
 * @summary Servoy's In-Memory tables can be dynamically created at runtime or during design time, offering flexibility in managing temporary data.
 */

/**
 * Creating In-Memory Databases
 *
 * There are two main ways to create In-Memory databases:
 *
 * 1. **Through Solution Explorer**:
 *    Use the Solution Explorer's context menu (`Datasources -> In Memory -> Create new data source`) to create an In-Memory datasource, which opens the Table Editor for column definitions.
 *
 * 2. **At Runtime with JSDataSet::createDataSource()**:
 *    This function allows dynamic datasource creation and does not require redefinition of columns if done during design time.
 *
 * @see [JSDataSet::createDataSource](../../../../reference/servoycore/dev-api/database-manager/jsdataset.md#createdatasourcename)
 * @see [Table Editor](../../../../reference/servoy-developer/object-editors/table-editor/README.md)
 */

/**
 * Additional Table Event: onLoad
 *
 * In-Memory tables have an additional event called **onLoad**, triggered when a form accesses the datasource or when the method `datasource.mem.name.getFoundSet()` is called.
 * This event enables you to populate the table on demand.
 *
 * @example
 * function onLoad(event) {
 *    var dataset = databaseManager.createEmptyDataSet(0, ['column1', 'column2']);
 *    dataset.addRow(['value1', 'value2']);
 *    dataset.createDataSource('inmemory_source', [JSColumn.STRING, JSColumn.STRING]);
 * }
 *
 * @see [JSDataSet::createDataSource](../../../../reference/servoycore/dev-api/database-manager/jsdataset.md#createdatasourcename)
 */

/**
 * Commands Summary
 *
 * A set of commands is available for managing In-Memory datasources via the context menu:
 *
 * - **Create in memory datasource**: Opens the Table Editor to define a new In-Memory table.
 * - **Edit table/view**: Allows editing the In-Memory table structure.
 * - **Delete/Rename In Memory Datasource**: Deletes or renames the In-Memory datasource definition.
 * - **Search for references**: Lists all occurrences of the In-Memory datasource in the solution.
 *
 * @see [Commands details](in-memory-databases.md#commands-details)
 *
 * runtime access to all defined in memory datasources. In scripting: <pre>datasources.mem</pre>
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class MemDataSource extends DefaultJavaScope
{
	private static Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(MemDataSource.class);
	private final IApplication application;

	MemDataSource(IApplication application)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
	}

	@Override
	protected boolean fill()
	{
		for (String name : application.getFoundSetManager().getInMemDataSourceNames())
		{
			put(name, this, new JSDataSource(application, DataSourceUtils.createInmemDataSource(name)));
		}

		return true;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		// Allow mem:mydataas well because we inadvertently used the datasource as key before release 7.4.4 (SVY-8064)
		String dsname = DataSourceUtils.getInmemDataSourceName(name);
		if (dsname == null)
		{
			// correct argument myds
			dsname = name;
		}
		else
		{
			// incorrect argument mem:myds
			Debug.warn("Accessing in-memory datasource scriptable using datasource uri (" + name + "), please use datasource name (" + dsname + ")");
		}

		Object val = super.get(dsname, start);
		if (val == null || val == Scriptable.NOT_FOUND)
		{
			// maybe added later; initialize in mem table
			try
			{
				application.checkAuthorized();
				application.getFoundSetManager().getTable(DataSourceUtils.createInmemDataSource(dsname));
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
			fill();
			val = super.get(dsname, start);
		}

		return val;
	}

}
