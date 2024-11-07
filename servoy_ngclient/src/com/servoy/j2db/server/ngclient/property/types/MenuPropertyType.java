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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.specification.property.types.ValuesPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class MenuPropertyType extends DefaultPropertyType<MenuTypeSabloValue>
	implements IConvertedPropertyType<MenuTypeSabloValue>, IRhinoToSabloComponent<MenuTypeSabloValue>, ISabloComponentToRhino<MenuTypeSabloValue>,
	IFormElementToSabloComponent<Object, MenuTypeSabloValue>, IFormElementToTemplateJSON<Object, MenuTypeSabloValue>,
	ISupportTemplateValue<Object>, IPropertyWithClientSideConversions<MenuTypeSabloValue>
{
	public static final MenuPropertyType INSTANCE = new MenuPropertyType();
	public static final String TYPE_NAME = "JSMenu";
	public Map<String, Map<String, PropertyDescription>> extraProperties = new HashMap<String, Map<String, PropertyDescription>>();

	private MenuPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		if (json != null && json.has("extraPropertiesCategory") && json.has("extraProperties"))
		{
			String category = json.getString("extraPropertiesCategory");
			Map<String, PropertyDescription> propertiesMap = extraProperties.get(category);
			if (propertiesMap == null)
			{
				propertiesMap = new HashMap<String, PropertyDescription>();
				extraProperties.put(category, propertiesMap);
			}
			JSONObject properties = json.getJSONObject("extraProperties");
			Iterator<String> it = properties.keys();
			while (it.hasNext())
			{
				String key = it.next();
				Object typeInformation = properties.get(key);
				String typeName = typeInformation instanceof String ? (String)typeInformation : ((JSONObject)typeInformation).getString("type");
				Object defaultValue = typeInformation instanceof JSONObject ? ((JSONObject)typeInformation).opt("default") : null;
				boolean hasDefaultValue = typeInformation instanceof JSONObject ? ((JSONObject)typeInformation).has("default") : false;
				IPropertyType< ? > propertyType = TypesRegistry.getType(typeName, false);
				ValuesConfig config = null;
				if (typeInformation instanceof JSONObject && ((JSONObject)typeInformation).has("values"))
				{
					propertyType = ValuesPropertyType.INSTANCE;
					config = new ValuesConfig();
					ArrayList<Object> listdata = new ArrayList<Object>();
					JSONArray array = ((JSONObject)typeInformation).optJSONArray("values");
					if (array != null)
					{
						for (int i = 0; i < array.length(); i++)
						{
							listdata.add(array.get(i));
						}
						config.setValues(listdata.toArray());
						if (defaultValue != null)
						{
							config.addDefault(defaultValue, defaultValue.toString());
						}
					}
				}
				propertiesMap.put(key, new PropertyDescriptionBuilder().withName(key).withType(
					propertyType).withDefaultValue(defaultValue).withHasDefault(hasDefaultValue).withConfig(config).build());
			}
		}
		return json;
	}

	@Override
	public MenuTypeSabloValue fromJSON(Object newJSONValue, MenuTypeSabloValue previousSabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof JSONObject json && previousSabloValue != null)
		{
			previousSabloValue.pushDataProviderValue(json.getString("category"), json.getString("propertyName"), json.getInt("itemIndex"),
				json.get("dataproviderValue"));
		}
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, MenuTypeSabloValue sabloValue, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			sabloValue.toJSON(writer, key, dataConverterContext);
		}
		return writer;
	}

	/**
	 * @return
	 */
	public Map<String, Map<String, PropertyDescription>> getExtraProperties()
	{
		return extraProperties;
	}

	@Override
	public boolean isValueAvailableInRhino(MenuTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return webComponentValue != null;
	}

	@Override
	public Object toRhinoValue(MenuTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext, Scriptable startScriptable)
	{
		if (webComponentValue != null)
		{
			return webComponentValue.getJSMenu();
		}
		return null;
	}

	@Override
	public MenuTypeSabloValue toSabloComponentValue(Object rhinoValue, MenuTypeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext webObjectContext)
	{
		if (rhinoValue instanceof JSMenu menu)
		{
			return new MenuTypeSabloValue(menu, this.extraProperties);
		}
		return null;
	}

	@Override
	public MenuTypeSabloValue toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		if (formElementValue != null)
		{
			Menu menu = dataAdapterList.getApplication().getFlattenedSolution().getMenu(formElementValue.toString());
			if (menu == null)
			{
				menu = dataAdapterList.getApplication().getFlattenedSolution().getMenu(Utils.getAsInteger(formElementValue));
			}
			if (menu != null)
			{
				return new MenuTypeSabloValue(dataAdapterList.getApplication().getMenuManager().getMenu(menu.getName()), this.extraProperties, formElement,
					component, dataAdapterList);
			}
		}
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

		FlattenedSolution fs = formElementContext.getFlattenedSolution();
		if (fs != null)
		{
			Menu menu = null;
			if (formElementValue instanceof Integer)
			{
				menu = fs.getMenu(((Integer)formElementValue).intValue());
			}
			if (formElementValue instanceof String)
			{
				menu = fs.getMenu((String)formElementValue);
			}
			if (menu != null)
			{
				Map<String, Object> newJavaValueForJSON = new HashMap<String, Object>();
				newJavaValueForJSON.put("name", menu.getName());
				newJavaValueForJSON.put("styleclass", menu.getStyleClass());
				addMenuItemsForJSON(newJavaValueForJSON, menu.getAllObjectsAsList());

				JSONUtils.toBrowserJSONFullValue(writer, key, newJavaValueForJSON, null, null);
			}
		}
		return writer;
	}

	private void addMenuItemsForJSON(Map<String, Object> menuMap, List<IPersist> items)
	{
		if (items != null && items.size() > 0)
		{
			List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
			menuMap.put("items", itemsList);
			for (IPersist persist : items)
			{
				if (persist instanceof MenuItem item)
				{
					Map<String, Object> itemMap = new HashMap<String, Object>();
					itemsList.add(itemMap);
					itemMap.put("itemID", item.getName());
					itemMap.put("menuText", item.getText());
					itemMap.put("styleClass", item.getStyleClass());
					itemMap.put("iconStyleClass", item.getIconStyleClass());
					itemMap.put("tooltipText", item.getToolTipText());
					itemMap.put("enabled", item.getEnabled());
					itemMap.put("extraProperties",
						MenuTypeSabloValue.getExtraPropertiesWithDefaultValues(item.getExtraProperties(), this.extraProperties, null, null));
					addMenuItemsForJSON(itemMap, item.getAllObjectsAsList());
				}
			}
		}
	}

	@Override
	public boolean valueInTemplate(Object value, PropertyDescription pd, FormElementContext formElementContext)
	{
		// only use for designer
		if (value == null) return true;
		if (formElementContext.getFormElement().getDesignId() != null) return true;
		return false;
	}


	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);

		w.value(TYPE_NAME);
		return true;
	}

	/**
	 * @param name
	 * @return
	 */
	public String getExtraPropertyCategory(String name)
	{
		for (String category : extraProperties.keySet())
		{
			Map<String, PropertyDescription> properties = extraProperties.get(category);
			if (properties != null && properties.containsKey(name))
			{
				return category;
			}
		}
		return null;
	}

}
