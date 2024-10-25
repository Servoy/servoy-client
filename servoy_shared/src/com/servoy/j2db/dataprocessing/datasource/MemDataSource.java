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
 *<pre data-puremarkdown>
	# MemDataSource

	## Overview

	Servoy allows the creation of **In-Memory** tables using HSQL, which behave like regular database tables with similar configurations, including column properties and events. These tables can be populated using a *JSDataSet* and the *createDataSource()* function to dynamically define datasources or foundsets at runtime.

	If the In-Memory table was predefined in Servoy Developer, no need to re-define column and type information during runtime.

	## Creating In-Memory Databases

	There are two ways to create an In-Memory database:

	 - Via the [Solution Explorer](../../../servoy-developer/solution-explorer/README.md), under *Datasources -> In Memory -> Create new data source*. This opens a dialog to specify the datasource name and the [Table Editor](../../../servoy-developer/object-editors/table-editor/README.md) for table structure definition.
	 - At runtime, using the [JSDataSet::createDataSource](../database-manager/jsdataset.md\#createdatasourcename) function to dynamically define datasources.

	## Additional Table Event: onLoad

	In-Memory tables have an extra event called **onLoad**, triggered when a form accesses the In-Memory datasource or when *datasource.mem.name.getFoundSet()* is called. This event allows on-demand population of the datasource, but you must still use *createDataSource()* in the **onLoad** method to populate the table.

	## Commands Summary

	 - _**Create in memory datasource**_ - Opens the [Table Editor](../../../servoy-developer/object-editors/table-editor/README.md).
	 - _**Edit table/view**_ - Edits table structure via the [Table Editor](../../../servoy-developer/object-editors/table-editor/README.md).
	 - _**Delete In Memory Datasource**_ - Deletes the datasource definition.
	 - _**Rename In Memory Datasource**_ - Renames the datasource definition.
	 - _**Search for references**_ - Finds locations within the solution where the datasource is used.

	For more details please refer the [In-memory Databases](../../../../guides/develop/application-design/data-modeling/in-memory-databases.md) from **Data modeling** section of this documentation.
 *</pre>
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
