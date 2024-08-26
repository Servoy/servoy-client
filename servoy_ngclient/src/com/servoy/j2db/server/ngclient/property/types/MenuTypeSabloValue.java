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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.scripting.JSMenuItem;

/**
 * @author lvostinar
 *
 */
public class MenuTypeSabloValue implements ISmartPropertyValue, IChangeListener
{
	private IChangeListener changeMonitor;
	private JSMenu jsMenu;
	private final Map<String, Map<String, Object>> extraProperties;

	public MenuTypeSabloValue(JSMenu menu, Map<String, Map<String, Object>> extraProperties)
	{
		this.jsMenu = menu;
		this.extraProperties = extraProperties;
	}

	public void toJSON(JSONWriter writer, String key, IBrowserConverterContext dataConverterContext) throws IllegalArgumentException, JSONException
	{
		Map<String, Object> newJavaValueForJSON = new HashMap<String, Object>();
		newJavaValueForJSON.put("name", jsMenu.getName());
		newJavaValueForJSON.put("styleclass", jsMenu.getStyleClass());
		addMenuItemsForJSON(newJavaValueForJSON, jsMenu.getMenuItems(), jsMenu.getSelectedItem());

		JSONUtils.toBrowserJSONFullValue(writer, key, newJavaValueForJSON, null, dataConverterContext);
	}

	private void addMenuItemsForJSON(Map<String, Object> menuMap, JSMenuItem[] items, JSMenuItem selectedItem)
	{
		if (items != null && items.length > 0)
		{
			List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
			menuMap.put("items", itemsList);
			for (JSMenuItem item : items)
			{
				Map<String, Object> itemMap = new HashMap<String, Object>();
				itemsList.add(itemMap);
				itemMap.put("itemID", item.getItemID());
				itemMap.put("menuText", item.getMenuText());
				itemMap.put("styleClass", item.getStyleClass());
				itemMap.put("iconStyleClass", item.getIconStyleClass());
				itemMap.put("tooltipText", item.getTooltipText());
				itemMap.put("enabled", item.getEnabled());
				itemMap.put("isSelected", item == selectedItem);
				itemMap.put("callbackArguments", item.getCallbackArguments());
				itemMap.put("extraProperties", getExtraPropertiesWithDefaultValues(item.getExtraProperties(), this.extraProperties));
				addMenuItemsForJSON(itemMap, item.getSubMenuItems(), selectedItem);
			}
		}
	}

	public static Map<String, Map<String, Object>> getExtraPropertiesWithDefaultValues(Map<String, Map<String, Object>> extraPropertiesValues,
		Map<String, Map<String, Object>> extraProperties)
	{
		if (extraProperties == null) return extraPropertiesValues;

		if (extraPropertiesValues != null)
		{
			extraPropertiesValues = new HashMap<String, Map<String, Object>>(extraPropertiesValues);
		}
		else
		{
			extraPropertiesValues = new HashMap<String, Map<String, Object>>();
		}
		for (String category : extraProperties.keySet())
		{
			Map<String, Object> categoryDefinitions = extraProperties.get(category);
			Map<String, Object> categoryValues = extraPropertiesValues.get(category);
			if (categoryValues == null)
			{
				categoryValues = new HashMap<String, Object>();
				extraPropertiesValues.put(category, categoryValues);
			}
			for (String propertyName : categoryDefinitions.keySet())
			{
				if (!categoryValues.containsKey(propertyName))
				{
					Object definition = categoryDefinitions.get(propertyName);
					if (definition instanceof JSONObject jsonDefition && jsonDefition.has("default"))
					{
						categoryValues.put(propertyName, jsonDefition.get("default"));
					}
				}

			}
		}
		return extraPropertiesValues;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectContext)
	{
		this.changeMonitor = changeMonitor;
		this.jsMenu.addChangeListener(this);
	}


	@Override
	public void detach()
	{
		this.jsMenu.removeChangeListener(this);
		this.jsMenu = null;
	}

	@Override
	public void valueChanged()
	{
		this.changeMonitor.valueChanged();
	}

	public Object getJSMenu()
	{
		return jsMenu;
	}
}
