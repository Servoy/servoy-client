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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.util.Utils;

/**
 * The <code>JSMenuItem</code> scripting wrapper provides functionality for managing menu items in a Servoy application.
 * It supports configuration and interaction through constants, properties, and methods, enabling dynamic customization of menu behavior and appearance.
 *
 * <h2>Functionality</h2>
 * <p>Constants such as <code>ENABLED</code> and <code>VIEWABLE</code> control security flags, determining whether menu items are accessible or visible.
 * Properties like <code>callbackArguments</code>, <code>enabled</code>, <code>iconStyleClass</code>, <code>itemID</code>, <code>menuText</code>, <code>styleClass</code>, and <code>tooltipText</code> allow precise customization of menu item attributes, including visual styling, text, and state.</p>
 *
 * <p>The functionality includes methods to manage menu items dynamically. Developers can add new items using methods like <code>addSubMenuItem</code>, which supports specifying positions, or retrieve existing items through methods such as <code>getSubMenuItem</code> and <code>getSubMenuItemAt</code>.
 * Items can also be removed using the <code>removeSubMenuItem</code> method.</p>
 *
 * <p>Security and visibility features allow control over menu item behavior. The <code>setSecurityFlags</code> method, combined with constants like <code>ENABLED</code> and <code>VIEWABLE</code>, provides flexibility to set whether a menu item is visible and interactive.
 * Additional methods, such as <code>getEnabledWithSecurity</code> and <code>getSubMenuItemsWithSecurity</code>, refine the interaction by considering security constraints.</p>
 *
 * <p>Properties and additional functionality extend to retrieving extra information, such as through the <code>getExtraProperty</code> method, and updating the menu item’s appearance and behavior dynamically.
 * These capabilities make <code>JSMenuItem</code> a versatile tool for creating adaptive and secure menu systems in Servoy applications.</p>
 *
 * <p><a href="../../../servoy-developer/solution-explorer/all-solutions/active-solution/menus/menu-item.md">MenuItem</a> section  of this documentation</p>
 *
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSMenuItem")
public class JSMenuItem extends JSMenu implements IConstantsObject
{
	/**
	 * Constant representing the viewable flag for menu item seurity.
	 *
	 * @sampleas setSecurityFlags(int)
	 */
	public static final int VIEWABLE = MenuItem.VIEWABLE;

	/**
	 * Constant representing the enabled flag for menu item security.
	 *
	 * @sampleas setSecurityFlags(int)
	 */
	public static final int ENABLED = MenuItem.ENABLED;

	private String menuText;
	private String iconStyleClass;
	private String tooltipText;
	private boolean enabled = true;
	private Object[] callbackArguments;
	private final JSMenu parentMenu;
	private Map<String, Map<String, Object>> extraProperties;
	private Map<String, Object> customPropertiesValues;
	private final String[] allowedPermissions;
	private JSONObject permissionsData;
	private int overridenPermissionData = -1;

	/**
	 * @param menuManager
	 * @param name
	 * @param allowedPermissions
	 */
	public JSMenuItem(JSMenu parentMenu, MenuItem menuItem, String[] allowedPermissions)
	{
		super(menuItem.getName(), allowedPermissions);
		this.menuText = menuItem.getText();
		this.styleClass = menuItem.getStyleClass();
		this.iconStyleClass = menuItem.getIconStyleClass();
		this.enabled = menuItem.getEnabled();
		this.tooltipText = menuItem.getToolTipText();
		this.parentMenu = parentMenu;
		this.allowedPermissions = allowedPermissions;
		this.permissionsData = menuItem.getPermissions();
		Iterator<IPersist> it = menuItem.getAllObjects();
		while (it.hasNext())
		{
			IPersist child = it.next();
			if (child instanceof MenuItem menuItemChild)
			{
				items.add(new JSMenuItem(this, menuItemChild, allowedPermissions));
			}
		}
		this.extraProperties = menuItem.getExtraProperties();
		this.customPropertiesValues = new HashMap<String, Object>(menuItem.getCustomPropertiesValues());
	}

	/**
	 * @param menuManager
	 * @param name
	 */
	public JSMenuItem(JSMenu parentMenu, String name, String[] allowedPermissions)
	{
		super(name, allowedPermissions);
		this.parentMenu = parentMenu;
		this.allowedPermissions = allowedPermissions;
	}

	@JSSetter
	public void setMenuText(String text)
	{
		this.menuText = text;
		notifyChanged("menutext", text);
	}

	/**
	 *
	 */
	@Override
	protected void notifyChanged(String property, Object value)
	{
		super.notifyChanged(property, value);
		this.parentMenu.notifyChanged(property, value);
	}

	/**
	 * Set/Get the menu item text
	 *
	 * @sample
	 * menuItem.menuText = 'Item 1';
	 *
	 * @return The text of the menu item.
	 */
	@JSGetter
	public String getMenuText()
	{
		return this.menuText;
	}

	@JSSetter
	public void setIconStyleClass(String styleclass)
	{
		this.iconStyleClass = styleclass;
		notifyChanged("iconstyleclass", styleclass);
	}

	/**
	 * Set/Get the menu item space separated icon style classes
	 *
	 * @sample
	 * menu.iconStyleClass = 'fas fa-square';
	 *
	 * @return The space-separated icon style classes of the menu item.
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
		notifyChanged("tooltiptext", text);
	}

	/**
	 * Set/Get the menu item tooltip text
	 *
	 * @sample
	 * menuItem.tooltipText = 'my tooltip';
	 *
	 * @return The tooltip text of the menu item.
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
		notifyChanged("enabled", enabled);
	}

	/**
	 * Set/Get the menu item enabled state
	 *
	 * @sample
	 * menuItem.enabled = false;
	 *
	 * @return The enabled state of the menu item.
	 */
	@JSGetter
	public boolean getEnabled()
	{
		return this.enabled;
	}

	/**
	 *
	 * @return Returns whether the menu item is enabled with security constraints applied.
	 */
	@JSFunction
	public boolean getEnabledWithSecurity()
	{
		return this.enabled && hasSecurityFlag(MenuItem.ENABLED);
	}

	/**
	 * Override the permission data and design data (for enabled flag) and set if a menu item is viewable(visible) and enabled.
	 *
	 * @param flags either 0 or a combination of JSMenuItem.VIEWABLE and JSMenuItem.ENABLED
	 *
	 * @sample menuItem.setSecurityFlags(JSMenuItem.VIEWABLE|JSMenuItem.ENABLED);
	 */
	@JSFunction
	public void setSecurityFlags(int flags)
	{
		if (flags >= 0)
		{
			this.overridenPermissionData = flags;
		}
	}

	public boolean hasSecurityFlag(int flag)
	{
		if (overridenPermissionData >= 0)
		{
			return (overridenPermissionData & flag) != 0;
		}
		if (this.allowedPermissions != null && this.allowedPermissions.length > 0 && this.permissionsData != null)
		{
			List<String> groupsList = Arrays.asList(this.allowedPermissions);
			for (String key : this.permissionsData.keySet())
			{
				if (groupsList.contains(key))
				{
					int permission = Utils.getAsInteger(this.permissionsData.get(key));
					if ((permission & flag) != 0)
					{
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}


	/**
	 * Set/Get the menu item callback arguments (for components that support this)
	 *
	 * @sample
	 * menuItem.callbackArguments = [1];
	 *
	 * @return The callback arguments of the menu item for supported components.
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

	/**
	 * Returns an extra property value.
	 *
	 * @sample menuItem.getExtraProperty('Sidenav','formName');
	 *
	 * @param {String} categoryName - The category name of the extra property.
	 * @param {String} propertyName - The name of the property to retrieve the value for.
	 *
	 * @return The value of the specified extra property, or null if not found.
	 */
	@JSFunction
	public Object getExtraProperty(String categoryName, String propertyName)
	{
		if (extraProperties != null && extraProperties.containsKey(categoryName))
		{
			return extraProperties.get(categoryName).get(propertyName);
		}
		return null;
	}

	/**
	 * Sets an extra property value (property must be present in the component spec).
	 *
	 * @param {String} categoryName - The category name of the extra property.
	 * @param {String} propertyName - The name of the property to set.
	 * @param {Object} value - The value to assign to the specified property.
	 *
	 * @sample menuItem.setExtraProperty('Sidenav','formName','myform');
	 */
	@JSFunction
	public void setExtraProperty(String categoryName, String propertyName, Object value)
	{
		if (extraProperties == null)
		{
			extraProperties = new HashMap<String, Map<String, Object>>();
		}
		Map<String, Object> propertiesMap = extraProperties.get(categoryName);
		if (propertiesMap == null)
		{
			propertiesMap = new HashMap<String, Object>();
			extraProperties.put(categoryName, propertiesMap);
		}
		propertiesMap.put(propertyName, value);
	}

	/**
	 * Returns custom property value. Custom properties can be defined on each Menu.
	 *
	 * @sample menuItem.getCustomProperty('myproperty');
	 *
	 * @param {String} propertyName - The name of the property to retrieve the value for.
	 *
	 * @return The value of the specified custom property, or null if not found.
	 */
	@JSFunction
	public Object getCustomProperty(String propertyName)
	{
		return customPropertiesValues.get(propertyName);
	}

	/**
	 * Sets a custom property value. Custom properties can be defined on each Menu.
	 *
	 * @param {String} propertyName - The name of the property to set.
	 * @param {Object} value - The value to assign to the specified property.
	 *
	 * @sample menuItem.setCustomProperty('formName','myform');
	 */
	@JSFunction
	public void setCustomProperty(String propertyName, Object value)
	{
		customPropertiesValues.put(propertyName, value);
	}

	/**
	 * @return A map containing all extra properties categorized by their names.
	 */
	public Map<String, Map<String, Object>> getExtraProperties()
	{
		return extraProperties;
	}

	public Map<String, Object> getCustomProperties()
	{
		return this.customPropertiesValues;
	}
}
