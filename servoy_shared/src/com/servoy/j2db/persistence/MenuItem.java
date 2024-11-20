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

package com.servoy.j2db.persistence;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * <p>The <code>MenuItem</code> is a reusable, design-time entity that integrates into <code>Menu</code> structures.
 * It represents an individual menu item used in building menus. Its parent can be either a <code>Menu</code> or another
 * <code>MenuItem</code>, enabling nested menu structures and flexible menu composition.</p>
 *
 * It is a reusable, design-time entity that integrates seamlessly into `Menu` structures
 *
 * <h2>Key features</h2>
 * <p>The <code>MenuItem</code> includes several important properties that integrate
 * seamlessly within the <code>Menu</code> structure. The <code>enabled</code>
 * property determines whether the menu item is interactive. The
 * <code>iconStyleClass</code> and <code>styleClass</code> properties allow the
 * application of specific styling to the item's icon and general appearance,
 * complementing the <code>Menu</code>'s overall design. The <code>name</code>
 * property uniquely identifies the menu item, while the <code>permissions</code>
 * property ensures security and visibility control by restricting access based on
 * user roles. The <code>text</code> property displays the menu item content and
 * supports HTML and localized text for dynamic customization. Additionally, the
 * <code>toolTipText</code> property provides rich, formatted hover text, enhancing
 * user experience and maintaining consistency with the parent <code>Menu</code>.</p>
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.MENU_ITEMS)
@ServoyClientSupport(mc = false, wc = false, sc = false, ng = true)
public class MenuItem extends AbstractBase implements ISupportUpdateableName, ICloneable, ISupportChilds
{
	public static final int VIEWABLE = 1;
	public static final int ENABLED = 2;

	/**
	 * Constructor I
	 */
	MenuItem(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.MENU_ITEMS, parent, element_id, uuid);
	}

	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		// do we care about duplicates here ?
		validator.checkName(arg, getID(), new ValidatorSearchContext(IRepository.MENU_ITEMS), false);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * Set the name
	 *
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * The name/identifier of the menu item.
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the toolTipText
	 *
	 * @param arg the toolTipText
	 */
	public void setToolTipText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT, arg);
	}

	/**
	 * The text displayed when hovering over the component with a mouse cursor.
	 *
	 * NOTE:
	 * HTML should be used for multi-line tooltips; you can also use any
	 * valid HTML tags to format tooltip text. For example:
	 * <html>This includes<b>bolded text</b> and
	 * <font color='blue'>BLUE</font> text as well.</html>
	 */
	public String getToolTipText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT);
	}

	/**
	 * Set the text
	 *
	 * @param arg the text
	 */
	public void setText(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT, arg);
	}

	/**
	 * The text displayed in menu item. Can contain html, i18n text.
	 *
	 */
	public String getText()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TEXT);
	}

	public String getStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS);
	}

	/**
	 * The css classes added in browser.
	 */
	public void setStyleClass(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_STYLECLASS, arg);
	}

	public void setEnabled(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENABLED, arg);
	}

	/**
	 * Enabled property of the menu item.
	 */
	public boolean getEnabled()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ENABLED).booleanValue();
	}

	public String getIconStyleClass()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ICONSTYLECLASS);
	}

	/**
	 * The css classes for displaying an icon (at the left of the text) in the menu item.
	 */
	public void setIconStyleClass(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ICONSTYLECLASS, arg);
	}

	public void setPermissions(JSONObject permissions)
	{
		this.setTypedProperty(StaticContentSpecLoader.PROPERTY_PERMISSIONS, permissions);
	}

	/**
	 * The permissions defined for menu item. Similar to form element security. You can configure if a menu item is visible/enabled based on permission.
	 */
	public JSONObject getPermissions()
	{
		return this.getTypedProperty(StaticContentSpecLoader.PROPERTY_PERMISSIONS);
	}

	public void putExtraProperty(String category, String propertyName, Object value)
	{
		this.putCustomProperty(new String[] { "extraProperties", category, propertyName }, value);
	}

	public Object getExtraProperty(String category, String propertyName)
	{
		return this.getCustomProperty(new String[] { "extraProperties", category, propertyName });
	}

	public Map<String, Map<String, Object>> getExtraProperties()
	{
		Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>)getCustomProperty(new String[] { "extraProperties" });
		if (map != null)
		{
			return new HashMap<String, Map<String, Object>>(map);
		}
		return null;
	}

	public MenuItem createNewMenuItem(IValidateName validator, String menuItemName) throws RepositoryException
	{
		String name = menuItemName == null ? "untitled" : menuItemName; //$NON-NLS-1$

		//check if name is in use
		validator.checkName(name, 0, new ValidatorSearchContext(IRepository.MENU_ITEMS), false);

		MenuItem obj = (MenuItem)getRootObject().getChangeHandler().createNewObject(this, IRepository.MENU_ITEMS);
		//set all the required properties

		obj.setName(name);

		addChild(obj);
		return obj;
	}

	@Override
	public String toString()
	{
		String name = getName();
		return name == null ? super.toString() : name;
	}
}
