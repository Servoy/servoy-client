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

package com.servoy.j2db.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Utils;

/**
 * <code>JSMenu</code> is a wrapper for scripting menu objects, providing properties and methods to define and manage menus in a user interface.
 *
 * <p>The <b>name</b> property serves as the identifier for a menu, allowing for easy reference during scripting.
 * The <b>styleClass</b> property enables the assignment of space-separated CSS style classes to customize the menu's appearance.</p>
 *
 * <p>JSMenu offers a range of methods to interact with menu items. The <code>addMenuItem(id)</code> and <code>addMenuItem(id, index)</code> methods allow adding menu items either at the end of the list or at a specific position.
 * Existing menu items can be retrieved using methods like <code>findMenuItem(id)</code>, which searches for items by identifier, including nested ones, and <code>getMenuItem(id)</code> or <code>getMenuItemAt(index)</code>, which retrieve menu items based on identifier or index respectively.
 * The <code>getMenuItems()</code> method returns a list of all menu items in the order they appear in the interface.</p>
 *
 * <p>Additional functionality includes removing menu items using <code>removeMenuItem(menuItem)</code> or <code>removeMenuItem(id)</code>, both of which return a Boolean indicating success.
 * The <code>selectMenuItem(menuItem)</code> method allows selecting a specific menu item in the user interface.
 * For enhanced security considerations, the <code>getMenuItemsWithSecurity()</code> method provides items with security metadata, and the <code>getSelectedItem()</code> method retrieves the currently selected item.</p>
 *
 * <p>For further information, refer to the
 * <a href="https://docs.servoy.com/reference/servoy-developer/menu">menu</a> section of this documentation.</p>
 *
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSMenu")
public class JSMenu
{
	private final String name;
	private JSMenuItem selectedItem;

	private final List<IModificationListener> changeListeners = new ArrayList<IModificationListener>();
	private final String[] allowedPermissions;
	private final Map<String, Object> customPropertiesDefinitions;

	protected String styleClass;
	protected final List<JSMenuItem> items = new ArrayList<JSMenuItem>();

	/**
	 * @param menuManager
	 * @param menu
	 * @param groups
	 */
	public JSMenu(Menu menu, String[] allowedPermissions)
	{
		this.name = menu.getName();
		this.styleClass = menu.getStyleClass();
		this.allowedPermissions = allowedPermissions;
		this.customPropertiesDefinitions = menu.getCustomPropertiesDefinition();
		Iterator<IPersist> it = menu.getAllObjects();
		while (it.hasNext())
		{
			IPersist child = it.next();
			if (child instanceof MenuItem menuItem)
			{
				items.add(new JSMenuItem(this, menuItem, allowedPermissions));
			}
		}
	}

	/**
	 * @param menuManager
	 * @param name
	 */
	public JSMenu(String name, String[] allowedPermissions)
	{
		this.name = name;
		this.allowedPermissions = allowedPermissions;
		this.customPropertiesDefinitions = new HashMap<String, Object>();
	}

	/**
	 * The menu name (identifier)
	 *
	 * @return the name (identifier) of the menu
	 */
	@JSReadonlyProperty
	public String getName()
	{
		return name;
	}

	@JSSetter
	public void setStyleClass(String styleclass)
	{
		this.styleClass = styleclass;
		this.notifyChanged("styleclass", styleclass);
	}

	/**
	 * Set/Get the menu space separated styleclases
	 *
	 * @sample
	 * menu.styleClass = 'myclass';
	 *
	 * @return The space-separated style classes applied to the menu.
	 */
	@JSGetter
	public String getStyleClass()
	{
		return this.styleClass;
	}

	/**
	 * Returns all the menus items, either created at design time or at runtime, in the order they will show up in user interface.
	 *
	 * @sample var items = menu.getMenuItems();
	 *
	 * @return An array of all menu items in this menu, in the order they appear in the user interface.
	 */
	@JSFunction
	public JSMenuItem[] getMenuItems()
	{
		return items.toArray(new JSMenuItem[0]);
	}

	/**
	 * @return An array of menu items that have the security flag set to "viewable."
	 */
	@JSFunction
	public JSMenuItem[] getMenuItemsWithSecurity()
	{
		return items.stream().filter(item -> item.hasSecurityFlag(MenuItem.VIEWABLE)).collect(Collectors.toList()).toArray(new JSMenuItem[0]);
	}

	/**
	 * Gets a menu item by identifier. Returns null if not found.
	 *
	 * @sample var mnu = menu.getMenuItem('item1');
	 * @param id the menu item identifier
	 *
	 * @return The menu item with the specified identifier, or null if not found.
	 */
	@JSFunction
	public JSMenuItem getMenuItem(String id)
	{
		return items.stream().filter(item -> Utils.equalObjects(id, item.getName())).findAny().orElse(null);
	}

	/**
	 * Gets a menu item by identifier. Also searches for nested elements. Returns null if not found.
	 *
	 * @sample var mnu = menu.findMenuItem('item1');
	 * @param id the menu item identifier
	 *
	 * @return The menu item with the specified identifier, including nested items, or null if not found.
	 */
	@JSFunction
	public JSMenuItem findMenuItem(String id)
	{
		for (JSMenuItem item : items)
		{
			if (Utils.equalObjects(id, item.getName()))
			{
				return item;
			}
			JSMenuItem subItem = item.findMenuItem(id);
			if (subItem != null)
			{
				return subItem;
			}
		}
		return null;
	}

	/**
	 * Gets a menu item by index (0 based). Returns null if not found.
	 *
	 * @sample var mnu = menu.getMenuItemAt(0);
	 * @param index the menu item index among its sibblings
	 *
	 * @return The menu item at the specified index, or null if the index is out of bounds.
	 */
	@JSFunction
	public JSMenuItem getMenuItemAt(int index)
	{
		return index >= 0 && index < items.size() ? items.get(index) : null;
	}

	/**
	 * Adds a new menu item, as last item in the list.
	 *
	 * @sample var item = menu.addMenuItem('item1');
	 * @param id the menu item identifier
	 *
	 * @return The newly added menu item.
	 */
	@JSFunction
	public JSMenuItem addMenuItem(String id)
	{
		return addMenuItem(id, items.size());
	}

	/**
	 * Adds a new menu item, at a specific position.
	 *
	 * @sample var mnu = menu.addMenuItem('item1',0);
	 * @param id the menu item identifier
	 * @param index the index position in list (0 based)
	 *
	 * @return The newly added menu item at the specified index.
	 */
	@JSFunction
	public JSMenuItem addMenuItem(String id, int index)
	{
		JSMenuItem item = null;
		if (index >= 0 && index <= items.size())
		{
			item = new JSMenuItem(this, id, this.allowedPermissions);
			items.add(index, item);
			this.notifyChanged(String.valueOf(FoundSetEvent.CHANGE_INSERT), Integer.valueOf(index));
		}
		return item;
	}

	/**
	 * Removes a menu item with given id, returns true if element was found an removed
	 *
	 * @sample var success = menu.removeMenuItem('item1');
	 * @param id the menu item identifier
	 *
	 * @return True if a menu item with the specified identifier was removed; false otherwise.
	 */
	@JSFunction
	public boolean removeMenuItem(String id)
	{
		return removeMenuItem(findMenuItem(id));
	}

	/**
	 * Removes a menu item from children's list, returns true if element was found an removed
	 *
	 * @sample var success = menu.removeMenuItem(item);
	 * @param menuItem the menu item to be removed
	 *
	 * @return True if the specified menu item was removed; false otherwise.
	 */
	@JSFunction
	public boolean removeMenuItem(JSMenuItem menuItem)
	{
		int index = items.indexOf(menuItem);
		boolean removed = items.remove(menuItem);
		if (removed)
		{
			this.notifyChanged(String.valueOf(FoundSetEvent.CHANGE_DELETE), Integer.valueOf(index));
		}
		return removed;
	}

	/**
	 * Selects a menu item in user interface
	 *
	 * @sample menu.selectMenuItem(item);
	 * @param menuItem the menu item to be selected
	 */
	@JSFunction
	public void selectMenuItem(JSMenuItem menuItem)
	{
		this.selectedItem = menuItem;
		this.notifyChanged(null, null);
	}

	/**
	 * Selects a menu item in user interface (by menu id). Returns whether the item was found and selected.
	 *
	 * @sample menu.selectMenuItemId('item1');
	 * @param menuItemId the menu item id to be selected
	 *
	 * @return True if the specified menu item was selected; false otherwise.
	 */
	@JSFunction
	public boolean selectMenuItemById(String menuItemId)
	{
		this.selectedItem = findMenuItem(menuItemId);
		this.notifyChanged(null, null);
		return this.selectedItem != null;
	}

	public void updateSelectedMenuItem(JSMenuItem menuItem)
	{
		this.selectedItem = menuItem;
	}

	/**
	 * @return the selectedItem
	 */
	@JSFunction
	public JSMenuItem getSelectedItem()
	{
		return selectedItem;
	}

	protected void notifyChanged(String property, Object value)
	{
		changeListeners.forEach(listener -> listener.valueChanged(new ModificationEvent(property, value, this)));
	}

	public void addChangeListener(IModificationListener listener)
	{
		changeListeners.add(listener);
	}

	public boolean removeChangeListener(IModificationListener listener)
	{
		return changeListeners.remove(listener);
	}

	public Map<String, Object> getCustomPropertiesDefinition()
	{
		return customPropertiesDefinitions;
	}
}
