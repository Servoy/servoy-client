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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.scripting.JSMenuItem;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;

/**
 * @author lvostinar
 *
 */
public class MenuTypeSabloValue implements ISmartPropertyValue, IChangeListener, IModificationListener
{
	private IChangeListener changeMonitor;
	private JSMenu jsMenu;
	private final Map<String, Map<String, PropertyDescription>> extraProperties;
	private final Map<JSMenuItem, Map<String, ISmartPropertyValue>> extraPropertiesSmartValues = new HashMap<JSMenuItem, Map<String, ISmartPropertyValue>>();

	public MenuTypeSabloValue(JSMenu menu, Map<String, Map<String, PropertyDescription>> extraProperties, INGFormElement formElement,
		WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		this.jsMenu = menu;
		this.extraProperties = extraProperties;
		addMenuItemsSabloValues(jsMenu.getMenuItemsWithSecurity(), formElement, component,
			dataAdapterList);
	}

	public MenuTypeSabloValue(JSMenu menu, Map<String, Map<String, PropertyDescription>> extraProperties)
	{
		this.jsMenu = menu;
		this.extraProperties = extraProperties;
	}

	public void toJSON(JSONWriter writer, String key, IBrowserConverterContext dataConverterContext) throws IllegalArgumentException, JSONException
	{
		Map<String, Object> newJavaValueForJSON = new HashMap<String, Object>();
		newJavaValueForJSON.put("name", jsMenu.getName());
		newJavaValueForJSON.put("styleClass", jsMenu.getStyleClass());
		addMenuItemsForJSON(newJavaValueForJSON, jsMenu.getMenuItemsWithSecurity(), jsMenu.getSelectedItem(), dataConverterContext);

		JSONUtils.toBrowserJSONFullValue(writer, key, newJavaValueForJSON, null, dataConverterContext);
	}

	private void addMenuItemsForJSON(Map<String, Object> menuMap, JSMenuItem[] items, JSMenuItem selectedItem, IBrowserConverterContext dataConverterContext)
	{
		if (items != null && items.length > 0)
		{
			List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
			menuMap.put("items", itemsList);
			for (JSMenuItem item : items)
			{
				Map<String, Object> itemMap = new HashMap<String, Object>();
				itemsList.add(itemMap);
				itemMap.put("itemID", item.getName());
				itemMap.put("menuText", item.getMenuText());
				itemMap.put("styleClass", item.getStyleClass());
				itemMap.put("iconStyleClass", item.getIconStyleClass());
				itemMap.put("tooltipText", item.getTooltipText());
				itemMap.put("enabled", item.getEnabledWithSecurity());
				itemMap.put("isSelected", item == selectedItem);
				itemMap.put("callbackArguments", item.getCallbackArguments());
				itemMap.put("extraProperties",
					getExtraPropertiesWithDefaultValues(item.getExtraProperties(), this.extraProperties, this.extraPropertiesSmartValues.get(item),
						dataConverterContext));
				addMenuItemsForJSON(itemMap, item.getMenuItemsWithSecurity(), selectedItem, dataConverterContext);
			}
		}
	}

	private void addMenuItemsSabloValues(JSMenuItem[] items, INGFormElement formElement,
		WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		if (items != null && items.length > 0)
		{
			for (JSMenuItem item : items)
			{
				Map<String, Map<String, Object>> extraPropertiesValues = item.getExtraProperties();
				for (String category : extraProperties.keySet())
				{
					Map<String, PropertyDescription> categoryDefinitions = extraProperties.get(category);
					Map<String, Object> categoryValues = extraPropertiesValues != null ? extraPropertiesValues.get(category) : null;
					if (categoryValues != null)
					{
						for (String propertyName : categoryDefinitions.keySet())
						{
							PropertyDescription definition = categoryDefinitions.get(propertyName);
							if (categoryValues.containsKey(propertyName) && definition.getType() instanceof IFormElementToSabloComponent type)
							{
								Object sabloValue = type.toSabloComponentValue(categoryValues.get(propertyName), definition, formElement, component,
									dataAdapterList);
								if (sabloValue instanceof ISmartPropertyValue smartValue)
								{
									Map<String, ISmartPropertyValue> smartValues = this.extraPropertiesSmartValues.get(item);
									if (smartValues == null)
									{
										smartValues = new HashMap<String, ISmartPropertyValue>();
										this.extraPropertiesSmartValues.put(item, smartValues);
									}
									smartValues.put(propertyName, smartValue);
								}
							}

						}
					}
				}
				addMenuItemsSabloValues(item.getMenuItemsWithSecurity(), formElement, component,
					dataAdapterList);
			}
		}
	}

	public static Map<String, Map<String, Object>> getExtraPropertiesWithDefaultValues(Map<String, Map<String, Object>> extraPropertiesValues,
		Map<String, Map<String, PropertyDescription>> extraProperties, Map<String, ISmartPropertyValue> smartValues,
		IBrowserConverterContext dataConverterContext)
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
			Map<String, PropertyDescription> categoryDefinitions = extraProperties.get(category);
			Map<String, Object> categoryValues = extraPropertiesValues.get(category);
			if (categoryValues == null)
			{
				categoryValues = new HashMap<String, Object>();
			}
			else
			{
				categoryValues = new HashMap<String, Object>(categoryValues);
			}
			extraPropertiesValues.put(category, categoryValues);
			for (String propertyName : categoryDefinitions.keySet())
			{
				PropertyDescription definition = categoryDefinitions.get(propertyName);
				if (!categoryValues.containsKey(propertyName))
				{
					if (definition.hasDefault())
					{
						categoryValues.put(propertyName, definition.getDefaultValue());
					}
				}
				else if (smartValues != null && smartValues.containsKey(propertyName) &&
					definition.getType() instanceof IPropertyConverterForBrowser convertingTypeToUse)
				{
					Object sabloValue = smartValues.get(propertyName);
					StringWriter stringWriter = new StringWriter();
					final JSONWriter writer = new JSONWriter(stringWriter);
					if (convertingTypeToUse instanceof IPropertyWithClientSideConversions)
					{
						JSONUtils.writeConvertedValueWithClientType(writer, null,
							JSONUtils.getClientSideTypeJSONString((IPropertyWithClientSideConversions< ? >)convertingTypeToUse, definition),
							() -> {
								convertingTypeToUse.toJSON(writer, null, sabloValue, definition, dataConverterContext);
								return null;
							});
					}
					else
					{
						writer.object();
						convertingTypeToUse.toJSON(writer, propertyName, sabloValue, definition, dataConverterContext);
						writer.endObject();
					}
					Object newValue = new JSONObject(stringWriter.getBuffer().toString());
					if (!(convertingTypeToUse instanceof IPropertyWithClientSideConversions))
					{
						newValue = ((JSONObject)newValue).get(propertyName);
					}
					categoryValues.put(propertyName, newValue);
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
		for (Map<String, ISmartPropertyValue> propertyValues : this.extraPropertiesSmartValues.values())
		{
			for (ISmartPropertyValue smartValue : propertyValues.values())
			{
				smartValue.attachToBaseObject(changeMonitor, webObjectContext);
			}
		}
	}


	@Override
	public void detach()
	{
		this.jsMenu.removeChangeListener(this);
		this.jsMenu = null;
		this.detachExtraProperties();
	}

	private void detachExtraProperties()
	{
		for (Map<String, ISmartPropertyValue> propertyValues : this.extraPropertiesSmartValues.values())
		{
			for (ISmartPropertyValue propertyValue : propertyValues.values())
			{
				propertyValue.detach();
			}
		}
		this.extraPropertiesSmartValues.clear();
	}

	@Override
	public void valueChanged()
	{
		this.changeMonitor.valueChanged();
	}

	@Override
	public void valueChanged(ModificationEvent e)
	{
		this.valueChanged();
	}

	public Object getJSMenu()
	{
		return jsMenu;
	}

	public void pushDataProviderValue(String category, String propertyName, int itemIndex, Object dataproviderValue)
	{
		JSMenuItem[] items = jsMenu.getMenuItemsWithSecurity();
		if (items != null && items.length > 0 && itemIndex >= 0 && itemIndex < items.length)
		{
			items[itemIndex].setExtraProperty(category, propertyName, dataproviderValue);
		}

	}
}
