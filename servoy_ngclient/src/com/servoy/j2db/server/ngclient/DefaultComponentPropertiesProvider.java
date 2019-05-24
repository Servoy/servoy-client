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

import java.util.HashMap;
import java.util.Map;

import org.sablo.specification.IDefaultComponentPropertiesProvider;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.server.ngclient.property.types.CSSPositionPropertyType;

/**
 * @author lvostinar
 *
 */
public class DefaultComponentPropertiesProvider implements IDefaultComponentPropertiesProvider
{
	public static DefaultComponentPropertiesProvider instance = new DefaultComponentPropertiesProvider();

	private DefaultComponentPropertiesProvider()
	{
	}

	@Override
	public Map<String, PropertyDescription> getDefaultComponentProperties()
	{
		Map<String, PropertyDescription> properties = new HashMap<String, PropertyDescription>();
		properties.put("location", new PropertyDescriptionBuilder().withName("location").withType(TypesRegistry.getType(PointPropertyType.TYPE_NAME)).build());
		properties.put("size", new PropertyDescriptionBuilder().withName("size").withType(TypesRegistry.getType(DimensionPropertyType.TYPE_NAME)).build());
		properties.put("anchors", new PropertyDescriptionBuilder().withName("anchors").withType(TypesRegistry.getType(IntPropertyType.TYPE_NAME)).build());
		properties.put("formIndex", new PropertyDescriptionBuilder().withName("formIndex").withType(TypesRegistry.getType(IntPropertyType.TYPE_NAME)).build());
		properties.put(IContentSpecConstants.PROPERTY_CSS_POSITION,
			new PropertyDescriptionBuilder().withName(IContentSpecConstants.PROPERTY_CSS_POSITION).withType(
				TypesRegistry.getType(CSSPositionPropertyType.TYPE_NAME)).build());
		return properties;
	}
}
