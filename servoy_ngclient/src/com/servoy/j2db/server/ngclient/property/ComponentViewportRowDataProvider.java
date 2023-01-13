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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithClientSideType;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.util.Pair;

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
	protected void populateRowData(IRecordInternal record, Set<String> propertyNames, JSONWriter w, String generatedRowId, ViewportClientSideTypes types)
		throws JSONException
	{
		w.object();
		dal.setRecordQuietly(record);

		List<Pair<String/* propertyName */, JSONString/* dynamicClientSideType */>> dynamicClientSideTypesForProperties = new ArrayList<>();
		if (propertyNames != null)
		{
			for (String propertyName : propertyNames)
			{
				// cell update
				populateCellData(propertyName, w, dynamicClientSideTypesForProperties);
			}
		}
		else
		{
			// full row
			componentTypeSabloValue.getRecordBasedProperties().forEach(propertyName -> populateCellData(propertyName, w, dynamicClientSideTypesForProperties));
		}
		types.registerClientSideType((dynamicClientSideTypesForProperties.size() == 0) ? null : dynamicClientSideTypesForProperties);

		w.endObject();
	}

	private void populateCellData(String propertyName, JSONWriter w,
		List<Pair<String/* propertyName */, JSONString/* dynamicClientSideType */>> dynamicClientSideTypesForProperties) throws JSONException
	{
		PropertyDescription t = component.getSpecification().getProperty(propertyName);
		Object val = component.getRawPropertyValue(propertyName);
		if (t != null && val != null)
		{
			// write values and only write client side types that are dynamic (client side already knows the client side types that are static for this component sent via ClientSideTypesState)
			IJSONStringWithClientSideType jsonValueRepresentationForWrappedValue = JSONUtils.FullValueToJSONConverter.INSTANCE.getConvertedValueWithClientType(
				val, t,
				new BrowserConverterContext(component, t.getPushToServer()), true);

			if (jsonValueRepresentationForWrappedValue != null)
			{
				w.key(propertyName);
				w.value(jsonValueRepresentationForWrappedValue);
				if (jsonValueRepresentationForWrappedValue.getClientSideType() != null)
				{
					dynamicClientSideTypesForProperties.add(new Pair<>(propertyName,
						jsonValueRepresentationForWrappedValue.getClientSideType()));
				}
			}
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
