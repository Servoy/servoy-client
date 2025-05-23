/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.util.Map;

import org.sablo.specification.IDefaultComponentPropertiesProvider;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.EnabledPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.specification.property.types.VisiblePropertyType;

import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.server.ngclient.property.types.CSSPositionPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ServoyAttributesPropertyType;

/**
 * @author lvostinar
 *
 */
public class DefaultComponentPropertiesProvider implements IDefaultComponentPropertiesProvider
{
	public static DefaultComponentPropertiesProvider instance = new DefaultComponentPropertiesProvider();
	public static final String ENABLED_DATAPROVIDER_NAME = "enabledDataProvider";
	public static final String VISIBLE_DATAPROVIDER_NAME = "visibleDataProvider";

	private DefaultComponentPropertiesProvider()
	{
	}

	@Override
	public void addDefaultComponentProperties(Map<String, PropertyDescription> properties)
	{
		if (!properties.containsKey("location"))
		{
			properties.put("location",
				new PropertyDescriptionBuilder().internal(true).withName("location").withType(TypesRegistry.getType(PointPropertyType.TYPE_NAME)).build());
		}
		if (!properties.containsKey("size"))
		{
			properties.put("size",
				new PropertyDescriptionBuilder().internal(true).withName("size").withType(TypesRegistry.getType(DimensionPropertyType.TYPE_NAME)).build());
		}
		if (!properties.containsKey("anchors"))
		{
			properties.put("anchors",
				new PropertyDescriptionBuilder().internal(true).withName("anchors").withType(IntPropertyType.INSTANCE_NULL_DEFAULT).build());
		}
		if (!properties.containsKey("formIndex"))
		{
			properties.put("formIndex",
				new PropertyDescriptionBuilder().internal(true).withName("formIndex").withType(IntPropertyType.INSTANCE_NULL_DEFAULT).build());
		}
		if (!properties.containsKey(IContentSpecConstants.PROPERTY_CSS_POSITION))
		{
			properties.put(IContentSpecConstants.PROPERTY_CSS_POSITION,
				new PropertyDescriptionBuilder().internal(true).withName(IContentSpecConstants.PROPERTY_CSS_POSITION).withType(
					TypesRegistry.getType(CSSPositionPropertyType.TYPE_NAME)).build());
		}
		if (!properties.containsKey(IContentSpecConstants.PROPERTY_ATTRIBUTES))
		{
			properties.put(IContentSpecConstants.PROPERTY_ATTRIBUTES,
				new PropertyDescriptionBuilder().withName(IContentSpecConstants.PROPERTY_ATTRIBUTES).withType(
					TypesRegistry.getType(ServoyAttributesPropertyType.TYPE_NAME)).build());
		}
		if (properties.values().stream().anyMatch(p -> p.getType() instanceof EnabledPropertyType))
		{
			properties.put(ENABLED_DATAPROVIDER_NAME, new PropertyDescriptionBuilder().internal(true).withName(ENABLED_DATAPROVIDER_NAME).withType(
				TypesRegistry.getType(DataproviderPropertyType.TYPE_NAME)).build());
		}
		if (properties.values().stream().anyMatch(p -> p.getType() instanceof VisiblePropertyType))
		{
			properties.put(VISIBLE_DATAPROVIDER_NAME, new PropertyDescriptionBuilder().internal(true).withName(VISIBLE_DATAPROVIDER_NAME).withType(
				TypesRegistry.getType(DataproviderPropertyType.TYPE_NAME)).build());
		}
	}
}
