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
import java.util.Map;

import org.sablo.specification.PropertyDescription;

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
	protected final WebFormComponent component;
	protected final List<String> recordBasedProperties;
	protected final ComponentTypeSabloValue componentTypeSabloValue;

	public ComponentViewportRowDataProvider(FoundsetDataAdapterList dal, WebFormComponent component, List<String> recordBasedProperties,
		ComponentTypeSabloValue componentTypeSabloValue)
	{
		this.dal = dal;
		this.component = component;
		this.recordBasedProperties = recordBasedProperties;
		this.componentTypeSabloValue = componentTypeSabloValue;
	}

	@Override
	protected void populateRowData(IRecordInternal record, String columnName, Map<String, Object> data, PropertyDescription dataTypes)
	{
		dal.setRecordQuietly(record);

		if (columnName != null)
		{
			// cell update
			populateCellData(data, dataTypes, columnName);
		}
		else
		{
			// full row
			for (String propertyName : recordBasedProperties)
			{
				populateCellData(data, dataTypes, propertyName);
			}
		}
	}

	private void populateCellData(Map<String, Object> data, PropertyDescription dataTypes, String propertyName)
	{
		PropertyDescription t = component.getSpecification().getProperty(propertyName);
		Object val = component.getRawProperties().get(propertyName);
		if (t != null && val != null)
		{
			dataTypes.putProperty(propertyName, t);
			data.put(propertyName, val);
		}
	}

}
