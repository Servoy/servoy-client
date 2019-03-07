/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
 *  * In scripting: <pre>datasources.view</pre>
 *  
 * @author emera
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class ViewDataSource extends DefaultJavaScope
{
	private static Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(ViewDataSource.class);
	private final IApplication application;

	ViewDataSource(IApplication application)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
	}

	@Override
	protected boolean fill()
	{
		for (String name : application.getFoundSetManager().getViewFoundsetDataSourceNames())
		{
			put(name, this, new JSViewDataSource(application, DataSourceUtils.createViewDataSource(name)));
		}

		return true;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		String dsname = DataSourceUtils.getViewDataSourceName(name);
		if (dsname == null)
		{
			// correct argument myds
			dsname = name;
		}
		else
		{
			// incorrect argument view:myds
			Debug.warn("Accessing view foundset datasource scriptable using datasource uri (" + name + "), please use datasource name (" + dsname + ")");
		}

		Object val = super.get(dsname, start);
		if (val == null || val == Scriptable.NOT_FOUND)
		{
			// maybe added later; initialize in view table
			try
			{
				application.getFoundSetManager().getTable(DataSourceUtils.createViewDataSource(dsname));
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
