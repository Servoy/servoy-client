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
import org.sablo.specification.PropertyDescription;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.scripting.JSMenuItem;
import com.servoy.j2db.scripting.ScriptObjectRegistry;

/**
 * <p><code>Menus</code> in Servoy provide a scripting object for creating and managing
 * menus (<code>JSMenu</code>, <code>JSMenuItem</code>) in applications. These objects facilitate
 * dynamic or design-time menu configuration and interaction.</p>
 *
 * <p>The <code>menus</code> object supports creating new menus using <code>createMenu(name)</code>,
 * where the <code>name</code> must be a unique identifier. Existing menus can be retrieved
 * using <code>getMenu(name)</code>, returning <code>null</code> if the specified menu does
 * not exist. Additionally, all menus, whether created at runtime or design time, can be
 * retrieved using <code>getMenus()</code>, which returns an array of available menus.</p>
 *
 * <p>Menus enable developers to customize user navigation and application functionality
 * dynamically or statically, supporting enhanced application experiences.</p>
 *
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Menus", scriptingName = "menus")
public class MenuManager implements IMenuManager, IReturnedTypesProvider
{

	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(MenuManager.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return getAllReturnedTypesInternal();
			}
		});
	}

	@Override
	public Class< ? >[] getAllReturnedTypes()
	{
		return getAllReturnedTypesInternal();
	}

	private static Class< ? >[] getAllReturnedTypesInternal()
	{
		return new Class< ? >[] { JSMenu.class, JSMenuItem.class };
	}

	private final Map<String, JSMenu> menus = new HashMap<String, JSMenu>();
	private final ClientState application;
	private boolean initialized = false;
	private String[] allowedPermissions;
	private static Map<String, Map<String, PropertyDescription>> extraProperties = new HashMap<String, Map<String, PropertyDescription>>();

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
	 *
	 * @return the newly created menu with the specified name.
	 */
	@JSFunction
	public JSMenu createMenu(String name)
	{
		this.initMenus();
		JSMenu menu = new JSMenu(name, allowedPermissions);
		menus.put(name, menu);
		return menu;
	}

	/**
	 * Returns all the menus, either created at design time or at runtime.
	 *
	 * @sample var menus = menus.getMenus();
	 *
	 * @return an array of all menus, created either at design time or runtime.
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
	 *
	 * @return the menu with the specified name, or null if not found.
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
				menus.put(menu.getName(), new JSMenu(menu, allowedPermissions));
			}
		}
	}

	public void flushMenus()
	{
		menus.clear();
		this.initialized = false;
	}

	/**
	 * @param allowedPermissions
	 */
	public void setCurrentPermissions(String[] allowedPermissions)
	{
		flushMenus();
		this.allowedPermissions = allowedPermissions;
	}

	public static void setExtraProperties(Map<String, Map<String, PropertyDescription>> extraProperties)
	{
		MenuManager.extraProperties = extraProperties;
	}

	public static Map<String, Map<String, PropertyDescription>> getExtraProperties()
	{
		return MenuManager.extraProperties;
	}
}
