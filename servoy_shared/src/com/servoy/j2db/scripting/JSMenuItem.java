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
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.MenuManager;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSMenuItem")
public class JSMenuItem
{
	private final String itemID;
	private String menuText;
	private String styleClass;
	private String iconStyleClass;
	private String tooltipText;
	private boolean enabled = true;
	private final MenuManager menuManager;
	private final List<JSMenuItem> items = new ArrayList<JSMenuItem>();
	private Object[] callbackArguments;

	/**
	 * @param menuManager
	 * @param menuItem
	 */
	public JSMenuItem(MenuManager menuManager, MenuItem menuItem)
	{
		this.itemID = menuItem.getName();
		this.menuText = menuItem.getText();
		this.styleClass = menuItem.getStyleClass();
		this.iconStyleClass = menuItem.getIconStyleClass();
		this.enabled = menuItem.getEnabled();
		this.tooltipText = menuItem.getToolTipText();
		this.menuManager = menuManager;
		Iterator<IPersist> it = menuItem.getAllObjects();
		while (it.hasNext())
		{
			IPersist child = it.next();
			if (child instanceof MenuItem menuItemChild)
			{
				items.add(new JSMenuItem(menuManager, menuItemChild));
			}
		}
	}

	/**
	 * @param menuManager
	 * @param name
	 */
	public JSMenuItem(MenuManager menuManager, String itemID)
	{
		this.itemID = itemID;
		this.menuManager = menuManager;
	}

	/**
	 * The menu name (identifier)
	 *
	 * @return the name (identifier) of the menu item
	 */
	@JSReadonlyProperty
	public String getItemID()
	{
		return itemID;
	}

	@JSSetter
	public void setMenuText(String text)
	{
		this.menuText = text;
		this.menuManager.notifyChanged();
	}

	/**
	 * Set/Get the menu item text
	 *
	 * @sample
	 * menuItem.menuText = 'Item 1';
	 */
	@JSGetter
	public String getMenuText()
	{
		return this.menuText;
	}

	@JSSetter
	public void setStyleClass(String styleclass)
	{
		this.styleClass = styleclass;
		this.menuManager.notifyChanged();
	}

	/**
	 * Set/Get the menu item space separated style classes
	 *
	 * @sample
	 * menu.styleClass = 'myclass';
	 */
	@JSGetter
	public String getStyleClass()
	{
		return this.styleClass;
	}

	@JSSetter
	public void setIconStyleClass(String styleclass)
	{
		this.iconStyleClass = styleclass;
		this.menuManager.notifyChanged();
	}

	/**
	 * Set/Get the menu item space separated icon style classes
	 *
	 * @sample
	 * menu.iconStyleClass = 'fas fa-square';
	 */
	@JSGetter
	public String getIconStyleClass()
	{
		return this.iconStyleClass;
	}

	@JSSetter
	public void setTooltipText(String text)
	{
		this.tooltipText = text;
		this.menuManager.notifyChanged();
	}

	/**
	 * Set/Get the menu item tooltip text
	 *
	 * @sample
	 * menuItem.tooltipText = 'my tooltip';
	 */
	@JSGetter
	public String getTooltipText()
	{
		return this.tooltipText;
	}

	@JSSetter
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
		this.menuManager.notifyChanged();
	}

	/**
	 * Set/Get the menu item enabled state
	 *
	 * @sample
	 * menuItem.enabled = false;
	 */
	@JSGetter
	public boolean getEnabled()
	{
		return this.enabled;
	}

	/**
	 * Returns all the child menus items, either created at design time or at runtime, in the order they will show up in user interface.
	 *
	 * @sample var items = menuItem.getSubMenuItems();
	 */
	@JSFunction
	public JSMenuItem[] getSubMenuItems()
	{
		return items.toArray(new JSMenuItem[0]);
	}

	/**
	 * Gets a child menu item by identifier. Returns null if not found.
	 *
	 * @sample var mnu = menuItem.getSubMenuItem('item1');
	 * @param id the menu item identifier
	 */
	@JSFunction
	public JSMenuItem getSubMenuItem(String id)
	{
		return items.stream().filter(item -> Utils.equalObjects(id, item.getItemID())).findAny().orElse(null);
	}

	/**
	 * Gets a child menu item at index (0 based). Returns null if not found.
	 *
	 * @sample var mnu = menuItem.getSubMenuItemAt(0);
	 * @param index the menu item index among its sibblings
	 */
	@JSFunction
	public JSMenuItem getSubMenuItemAt(int index)
	{
		return index >= 0 && index < items.size() ? items.get(index) : null;
	}

	/**
	 * Adds a new menu item, as last item in the list.
	 *
	 * @sample var item = menuItem.addSubMenuItem('item1');
	 * @param id the menu item identifier
	 */
	@JSFunction
	public JSMenuItem addSubMenuItem(String id)
	{
		return addSubMenuItem(id, items.size());
	}

	/**
	 * Adds a new menu item, at a specific position.
	 *
	 * @sample var mnu = menu.addSubMenuItem('item1',0);
	 * @param id the menu item identifier
	 * @param index the index position in list (0 based)
	 */
	@JSFunction
	public JSMenuItem addSubMenuItem(String id, int index)
	{
		JSMenuItem item = null;
		if (index >= 0 && index <= items.size())
		{
			item = new JSMenuItem(menuManager, id);
			items.add(index, item);
			this.menuManager.notifyChanged();
		}
		return item;
	}

	/**
	 * Removes a menu item with given id, returns true if element was found an removed
	 *
	 * @sample var success = menu.removeSubMenuItem('item1');
	 * @param id the menu item identifier
	 */
	@JSFunction
	public boolean removeSubMenuItem(String id)
	{
		this.menuManager.notifyChanged();
		return items.removeIf(item -> Utils.equalObjects(id, item.getItemID()));
	}

	/**
	 * Removes a menu item from children's list, returns true if element was found an removed
	 *
	 * @sample var success = menu.removeSubMenuItem(item);
	 * @param menuItem the menu item to be removed
	 */
	@JSFunction
	public boolean removeSubMenuItem(JSMenuItem menuItem)
	{
		this.menuManager.notifyChanged();
		return items.remove(menuItem);
	}

	/**
	 * Set/Get the menu item callback arguments (for components that support this)
	 *
	 * @sample
	 * menuItem.callbackArguments = [1];
	 */
	@JSGetter
	public Object[] getCallbackArguments()
	{
		return callbackArguments;
	}

	/**
	 * @param callbackArguments the callbackArguments to set
	 */
	@JSSetter
	public void setCallbackArguments(Object[] callbackArguments)
	{
		this.callbackArguments = callbackArguments;
	}
}
