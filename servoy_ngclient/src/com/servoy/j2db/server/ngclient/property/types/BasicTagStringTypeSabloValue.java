/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import org.json.JSONWriter;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.util.Utils;

/**
 * A tag string property value that represents/wraps a simple string.
 * It is not aware of any tags, but subclasses can add support for that.
 *
 * @author acostescu
 */
public class BasicTagStringTypeSabloValue
{

	private String designValue;
	private final DataAdapterList dataAdapterList;

	public BasicTagStringTypeSabloValue(String designValue, DataAdapterList dataAdapterList)
	{
		this.designValue = designValue;
		this.dataAdapterList = dataAdapterList;
	}

	public String getDesignValue()
	{
		return designValue;
	}

	public String getTagReplacedValue()
	{
		return designValue;
	}

	protected void setDesignValue(String newDesignValue)
	{
		designValue = newDesignValue;
	}

	protected DataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		String v = getTagReplacedValue();
		writer.value(v != null ? v : "");
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof BasicTagStringTypeSabloValue)
		{
			return Utils.equalObjects(((BasicTagStringTypeSabloValue)obj).designValue, designValue);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return designValue != null ? designValue.hashCode() : super.hashCode();
	}

}