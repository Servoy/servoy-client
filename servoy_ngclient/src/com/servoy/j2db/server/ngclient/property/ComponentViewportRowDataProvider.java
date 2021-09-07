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

import java.util.Set;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.WebFormComponent;

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
	protected final ComponentTypeSabloValue componentTypeSabloValue;
	protected final WebFormComponent component;

	public ComponentViewportRowDataProvider(FoundsetDataAdapterList dal, WebFormComponent component,
		ComponentTypeSabloValue componentTypeSabloValue)
	{
		this.dal = dal;
		this.componentTypeSabloValue = componentTypeSabloValue;
		this.component = component;
	}

	@Override
	protected void populateRowData(IRecordInternal record, Set<String> propertyNames, JSONWriter w, DataConversion clientConversionInfo, String generatedRowId)
		throws JSONException
	{
		w.object();
		dal.setRecordQuietly(record);

		if (propertyNames != null)
		{
			for (String propertyName : propertyNames)
			{
				// cell update
				populateCellData(propertyName, w, clientConversionInfo);
			}
		}
		else
		{
			// full row
			componentTypeSabloValue.getRecordBasedProperties().forEach(propertyName -> populateCellData(propertyName, w, clientConversionInfo));
		}
		w.endObject();
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

	@Override
	protected FoundsetDataAdapterList getDataAdapterList()
	{
		return dal;
	}
}
