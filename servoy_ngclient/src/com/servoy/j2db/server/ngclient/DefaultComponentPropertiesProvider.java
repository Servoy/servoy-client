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

import org.json.JSONObject;
import org.sablo.specification.IDefaultComponentPropertiesProvider;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.EnabledPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
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
				new PropertyDescriptionBuilder().withName("location").withType(TypesRegistry.getType(PointPropertyType.TYPE_NAME)).build());
		}
		if (!properties.containsKey("size"))
		{
			properties.put("size",
				new PropertyDescriptionBuilder().withName("size").withType(TypesRegistry.getType(DimensionPropertyType.TYPE_NAME)).build());
		}
		if (!properties.containsKey("anchors"))
		{
			properties.put("anchors",
				new PropertyDescriptionBuilder().withName("anchors").withType(IntPropertyType.INSTANCE_NULL_DEFAULT).build());
		}
		if (!properties.containsKey("formIndex"))
		{
			properties.put("formIndex",
				new PropertyDescriptionBuilder().withName("formIndex").withType(IntPropertyType.INSTANCE_NULL_DEFAULT)
					.withTags(new JSONObject().put(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
						"The Z index of this component. If two components overlap, then the component with higher Z index is displayed above the component with lower Z index."))
					.build());

		}
		if (!properties.containsKey(IContentSpecConstants.PROPERTY_CSS_POSITION))
		{
			properties.put(IContentSpecConstants.PROPERTY_CSS_POSITION,
				new PropertyDescriptionBuilder().withName(IContentSpecConstants.PROPERTY_CSS_POSITION).withType(
					TypesRegistry.getType(CSSPositionPropertyType.TYPE_NAME))
					.withTags(new JSONObject().put(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
						"CSS position is a replacement for anchoring system making it more intuitive to place a component.\r\n" +
							"CSS position should be set on form, an absolute position form can either work with anchoring or with css position.\r\n" +
							"This is only working in NGClient."))
					.build());
		}
		if (!properties.containsKey(IContentSpecConstants.PROPERTY_ATTRIBUTES))
		{
			properties.put(IContentSpecConstants.PROPERTY_ATTRIBUTES,
				new PropertyDescriptionBuilder().withName(IContentSpecConstants.PROPERTY_ATTRIBUTES).withType(
					TypesRegistry.getType(ServoyAttributesPropertyType.TYPE_NAME))
					.withTags(new JSONObject().put(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
						"Array of attributes of a component that will be rendered in HTML."))
					.build());

		}
		if (!properties.containsKey(IContentSpecConstants.PROPERTY_COMMENT))
		{
			properties.put(IContentSpecConstants.PROPERTY_COMMENT,
				new PropertyDescriptionBuilder().withName(IContentSpecConstants.PROPERTY_COMMENT).withType(
					TypesRegistry.getType(StringPropertyType.TYPE_NAME))
					.withTags(new JSONObject().put(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
						"Additional design time information, such as programmer notes about this model object's purpose.")
						.put(WebFormComponent.TAG_SCOPE, "design"))
					.build());

		}
		if (properties.values().stream().anyMatch(p -> p.getType() instanceof EnabledPropertyType))
		{
			properties.put(ENABLED_DATAPROVIDER_NAME, new PropertyDescriptionBuilder().withName(ENABLED_DATAPROVIDER_NAME).withType(
				TypesRegistry.getType(DataproviderPropertyType.TYPE_NAME))
				.withTags(new JSONObject().put(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
					"Component enabled state can be controlled through enabled property(boolean) and an enabled dataprovider (optional) that should evaluate to true/false. If enabled dataprovider is set then the component enabled state will be a logical and between the two values. Disabled components prevent any user interaction."))
				.build());
		}
		if (properties.values().stream().anyMatch(p -> p.getType() instanceof VisiblePropertyType))
		{
			properties.put(VISIBLE_DATAPROVIDER_NAME, new PropertyDescriptionBuilder().withName(VISIBLE_DATAPROVIDER_NAME).withType(
				TypesRegistry.getType(DataproviderPropertyType.TYPE_NAME))
				.withTags(new JSONObject().put(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
					"Component visibility can be controlled through visible property(boolean) and a visible dataprovider (optional) that should evaluate to true/false. If visible dataprovider is set then the component visibility will be a logical and between the two values."))
				.build());
		}
	}
}
