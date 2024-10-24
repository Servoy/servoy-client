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
 * <p><b><i>In-Memory Databases</i></b></p>
 *
 * <p><b><i>Overview</i></b></p>
 * <p>Servoy allows the creation of <b>In-Memory</b> tables using HSQL, which behave like regular database tables with similar configurations, including column properties and events.
 * These tables can be populated using a <i>JSDataSet</i> and the <i>createDataSource()</i> function to dynamically define datasources or foundsets at runtime.</p>
 *
 * <p>If the In-Memory table was predefined in Servoy Developer, no need to re-define column and type information during runtime.</p>
 *
 * <p><b><i>Creating In-Memory Databases</i></b></p>
 * <p>There are two ways to create an In-Memory database:</p>
 * <ul>
 *   <li>Via the <a href="../../../servoy-developer/solution-explorer/README.md">Solution Explorer</a>, under <i>Datasources -> In Memory -> Create new data source</i>. This opens a dialog to specify the datasource name and the <a href="../../../../reference/servoy-developer/object-editors/table-editor/README.md">Table Editor</a> for table structure definition.</li>
 *   <li>At runtime, using the <a href="../database-manager/jsdataset.md#createdatasourcename">JSDataSet::createDataSource</a> function to dynamically define datasources.</li>
 * </ul>
 *
 * <p><b><i>Additional Table Event: onLoad</i></b></p>
 * <p>In-Memory tables have an extra event called <b>onLoad</b>, triggered when a form accesses the In-Memory datasource or when <a href="../../../../reference/servoycore/dev-api/database-manager/jsdataset.md#getFoundSet">datasource.mem.name.getFoundSet()</a> is called. This event allows on-demand population of the datasource, but you must still use <i>createDataSource()</i> in the <b>onLoad</b> method to populate the table.</p>
 *
 * <p><b><i>Commands Summary</i></b></p>
 * <ul>
 *   <li><a href="memdatasource.md#create-in-memory-datasource">Create in memory datasource</a> - Opens the <a href="../../../servoy-developer/object-editors/table-editor/">Table Editor</a>.</li>
 *   <li><a href="memdatasource.md#edit-table-view">Edit table/view</a> - Edits table structure via the <a href="../../../servoy-developer/object-editors/table-editor/">Table Editor</a>.</li>
 *   <li><a href="memdatasource.md#delete-in-memory-datasource">Delete In Memory Datasource</a> - Deletes the datasource definition.</li>
 *   <li><a href="memdatasource.md#rename-in-memory-datasource">Rename In Memory Datasource</a> - Renames the datasource definition.</li>
 *   <li><a href="memdatasource.md#search-for-references">Search for references</a> - Finds locations within the solution where the datasource is used.</li>
 * </ul>
 *
 * For more details please refer the <a href="../../../../guides/develop/application-design/data-modeling/in-memory-databases.md">In-memory Databases</a> from <b>Data modeling</b> section of this documentation.
 *
 * Runtime access to all defined in memory datasources. In scripting: <pre>datasources.mem</pre>
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
