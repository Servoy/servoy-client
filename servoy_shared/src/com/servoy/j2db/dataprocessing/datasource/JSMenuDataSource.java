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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.MenuFoundSet;
import com.servoy.j2db.dataprocessing.MenuItemRecord;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.ServoyException;

/**
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSMenuDataSource implements IJavaScriptType, IDestroyable
{
	private volatile IApplication application;
	private final String datasource;

	public JSMenuDataSource(IApplication application, String datasource)
	{
		this.application = application;
		this.datasource = datasource;
	}

	/**
	 * Get the datasource string.
	 *
	 * @sample
	 * datasources.menu.mymenu.getDataSource() // returns 'menu:mymenu'
	 *
	 * @return String datasource
	 */
	@JSFunction
	public String getDataSource()
	{
		return datasource;
	}

	/**
	 * Get relation name that allows to load child menu items
	 *
	 * @sample
	 * datasources.menu.mymenu.getParentToChildrenRelationName()
	 *
	 * @return String relationName
	 */
	@JSFunction
	public String getParentToChildrenRelationName()
	{
		return MenuItemRecord.MENUITEM_RELATION_NAME;
	}

	/**
	 * Returns the MenuFoundSet for this menu datasource. The menu foundset is always a shared foundset which is fully loaded will all menu items.
	 * The foundset will be completely created when is first requested.
	 *
	 * @sample
	 * var fs = datasources.menu.mymenu.getFoundSet()
	 *
	 * @return A new MenuFoundSet  for the datasource.
	 */
	@JSFunction
	public MenuFoundSet getFoundSet() throws ServoyException
	{
		IFoundSet foundSet = application.getFoundSetManager().getFoundSet(datasource);
		if (foundSet instanceof MenuFoundSet) return (MenuFoundSet)foundSet;
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.util.IDestroyable#destroy()
	 */
	@Override
	public void destroy()
	{
		application = null;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + '(' + datasource + ')';
	}
}