/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

/**
 * <pre data-puremarkdown>
`MenuDataSource` provides runtime access to all defined menu foundsets, allowing menus to function as datasources with `datasources.menu` in scripting. This enables menu structures to be used as data records, supporting components like `DBTreeView` and custom FormComponents for enhanced menu interactions.

For more on setting up and using menu datasources, see [MenuFoundSet](../database-manager/menufoundset.md).
 * </pre>
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class MenuDataSource extends DefaultJavaScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(MenuDataSource.class);
	private volatile IApplication application;

	MenuDataSource(IApplication application)
	{
		super(application.getScriptEngine().getSolutionScope(), jsFunctions);
		this.application = application;
	}

	@Override
	protected boolean fill()
	{
		application.getFlattenedSolution().getMenus(true).forEachRemaining(menu -> {
			put(menu.getName(), this, new JSMenuDataSource(application, DataSourceUtils.createMenuDataSource(menu.getName())));
		});

		return true;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object val = super.get(name, start);
		if (val == null || val == Scriptable.NOT_FOUND)
		{
			// maybe added later; initialize in view table
			fill();
			val = super.get(name, start);
		}

		return val;
	}

	@Override
	public void destroy()
	{
		application = null;
		super.destroy();
	}
}
