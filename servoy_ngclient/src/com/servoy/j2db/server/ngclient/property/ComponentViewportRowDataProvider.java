/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.util.List;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.util.Utils;

/**
 * This class provides viewport data for a component's record - related properties. It is used when combining component properties with foundset properties.
 * These properties are sent as a viewport data array inside the component property - to browser; so these types of properties are not send only once as part of the
 * model for all records.
 *
 * @author acostescu
 */
public class ComponentViewportRowDataProvider extends ViewportRowDataProvider
{

	protected final FoundsetDataAdapterList dal;
	protected final List<String> recordBasedProperties;
	protected final ComponentTypeSabloValue componentTypeSabloValue;
	protected final WebFormComponent component;

	public ComponentViewportRowDataProvider(FoundsetDataAdapterList dal, WebFormComponent component, List<String> recordBasedProperties,
		ComponentTypeSabloValue componentTypeSabloValue)
	{
		this.dal = dal;
		this.recordBasedProperties = recordBasedProperties;
		this.componentTypeSabloValue = componentTypeSabloValue;
		this.component = component;
	}

	@Override
	protected void populateRowData(IRecordInternal record, String columnName, JSONWriter w, DataConversion clientConversionInfo, String generatedRowId)
		throws JSONException
	{
		w.object();
		dal.setRecordQuietly(record);

		String columnPropertyName = getPropertyName(columnName);

		if (columnPropertyName != null)
		{
			// cell update
			populateCellData(columnPropertyName, w, clientConversionInfo);
		}
		else
		{
			// full row
			for (String propertyName : recordBasedProperties)
			{
				populateCellData(propertyName, w, clientConversionInfo);
			}
		}
		w.endObject();
	}

	@Override
	protected boolean containsColumn(String columnName)
	{
		if (columnName == null) return true;

		return getPropertyName(columnName) != null;
	}

	private String getPropertyName(String columnName)
	{
		if (columnName != null)
		{
			if (recordBasedProperties.contains(columnName))
			{
				return columnName;
			}
			else
			{
				// this is probably a dpid
				for (String propertyName : recordBasedProperties)
				{
					Object dpValue = component.getProperty(propertyName);
					if (dpValue instanceof DataproviderTypeSabloValue &&
						Utils.equalObjects(((DataproviderTypeSabloValue)dpValue).getDataProviderID(), columnName))
					{
						return propertyName;
					}
				}
			}
		}
		return null;
	}

	private void populateCellData(String propertyName, JSONWriter w, DataConversion clientConversionInfo) throws JSONException
	{
		PropertyDescription t = component.getSpecification().getProperty(propertyName);
		Object val = component.getRawPropertyValue(propertyName);
		if (t != null && val != null)
		{
			clientConversionInfo.pushNode(propertyName);
			FullValueToJSONConverter.INSTANCE.toJSONValue(w, propertyName, val, t, clientConversionInfo,
				new BrowserConverterContext(component, t.getPushToServer()));
			clientConversionInfo.popNode();
		}
	}

	@Override
	protected boolean shouldGenerateRowIds()
	{
		return false;
	}

	@Override
	protected boolean isReady()
	{
		return true;
	}

}
