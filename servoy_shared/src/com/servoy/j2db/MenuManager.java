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

package com.servoy.j2db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.scripting.JSMenu;

/**
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Menus", scriptingName = "menus")
public class MenuManager implements IMenuManager
{
	private final Map<String, JSMenu> menus = new HashMap<String, JSMenu>();
	private final ClientState application;
	private boolean initialized = false;
	private String[] groups;

	/**
	 * @param clientState
	 */
	public MenuManager(ClientState clientState)
	{
		this.application = clientState;
		// lazy load the menus, too early now
	}

	/**
	 * Create a new, empty menu. Identifier must be unique among existing menus.
	 *
	 * @sample var mnu = menus.createMenu('mymenu');
	 * @param name the menu name (identifier)
	 */
	@JSFunction
	public JSMenu createMenu(String name)
	{
		this.initMenus();
		JSMenu menu = new JSMenu(name, groups);
		menus.put(name, menu);
		return menu;
	}

	/**
	 * Returns all the menus, either created at design time or at runtime.
	 *
	 * @sample var menus = menus.getMenus();
	 */
	@JSFunction
	public JSMenu[] getMenus()
	{
		this.initMenus();
		return menus.values().toArray(new JSMenu[0]);
	}

	/**
	 * Gets a menu by name. Returns null if not found.
	 *
	 * @sample var mnu = menus.getMenu('mymenu');
	 * @param name the menu name (identifier)
	 */
	@JSFunction
	public JSMenu getMenu(String name)
	{
		this.initMenus();
		return menus.get(name);
	}

	private void initMenus()
	{
		if (!initialized)
		{
			initialized = true;
			Iterator<Menu> it = application.getFlattenedSolution().getMenus(false);
			while (it.hasNext())
			{
				Menu menu = it.next();
				menus.put(menu.getName(), new JSMenu(menu, groups));
			}
		}
	}

	public void flushMenus()
	{
		menus.clear();
		this.initialized = false;
	}

	/**
	 * @param groups
	 */
	public void setCurrentGroups(String[] groups)
	{
		flushMenus();
		this.groups = groups;
	}
}
