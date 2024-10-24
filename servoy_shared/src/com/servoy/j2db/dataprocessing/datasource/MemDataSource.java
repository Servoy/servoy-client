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
 * <p><b><i>In-Memory Databases Overview</i></b></p>
 *
 * <p>In-Memory databases in Servoy are temporary tables that function like regular database tables but are stored in memory.
 * They can be dynamically created at runtime or during design time.</p>
 *
 * <p><b><i>Creating In-Memory Databases</i></b></p>
 *
 * <p>There are two main ways to create an In-Memory database:</p>
 *
 * <ul>
 *   <li><b>Solution Explorer:</b> Use the context menu option <i>Datasources -> In Memory -> Create new data source</i> to create a new datasource. This opens the Table Editor to define the table structure.</li>
 *   <li><b>At Runtime:</b> Use the <a href="../../../../reference/servoycore/dev-api/database-manager/jsdataset.md#createdatasourcename">JSDataSet::createDataSource</a> function to dynamically create In-Memory tables.</li>
 * </ul>
 *
 * <p><b><i>Usage Example</i></b></p>
 *
 * <pre>
 * var dataset = databaseManager.createEmptyDataSet(0, ['column1', 'column2']);
 * dataset.addRow(['value1', 'value2']);
 * dataset.createDataSource('inmemory_source', [JSColumn.STRING, JSColumn.STRING]);
 * </pre>
 *
 * <p>For more details, refer to the documentation on <a href="../../../../reference/servoycore/dev-api/database-manager/jsdataset.md#createdatasourcename">JSDataSet::createDataSource</a>.</p>
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
