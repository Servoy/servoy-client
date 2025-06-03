/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.scripting.JSMenuItem;
import com.servoy.j2db.server.ngclient.WebFormComponent;

/**
 * @author lvostinar
 *
 */
public class MenuItemPropertyType extends DefaultPropertyType<JSMenuItem> implements IPropertyConverterForBrowser<JSMenuItem>, IClassPropertyType<JSMenuItem>
{
	public static final MenuItemPropertyType INSTANCE = new MenuItemPropertyType();
	public static final String TYPE_NAME = "JSMenuItem";

	private MenuItemPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public JSMenuItem fromJSON(Object newJSONValue, JSMenuItem previousSabloValue, PropertyDescription pd, IBrowserConverterContext converterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		JSMenuItem menuItem = null;
		if (newJSONValue instanceof JSONObject jsonObject && converterContext.getWebObject() instanceof WebFormComponent webFormComponent)
		{
			JSMenu jsmenu = webFormComponent.getDataConverterContext().getApplication().getMenuManager().getMenu(jsonObject.optString("menuid"));
			if (jsmenu != null)
			{
				menuItem = jsmenu.findMenuItem(jsonObject.optString("id"));
			}
		}
		else if (newJSONValue instanceof JSMenuItem)
		{
			menuItem = (JSMenuItem)newJSONValue;
		}
		return menuItem;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, JSMenuItem sabloValue, PropertyDescription pd, IBrowserConverterContext converterContext)
		throws JSONException
	{
		if (sabloValue != null)
		{
			JSONUtils.addKeyIfPresent(writer, key);
			writer.object();
			writer.key("svyType").value("JSMenuItem");
			writer.key("id").value(sabloValue.getName());
			writer.endObject();
		}
		return writer;
	}

	@Override
	public Class<JSMenuItem> getTypeClass()
	{
		return JSMenuItem.class;
	}
}
