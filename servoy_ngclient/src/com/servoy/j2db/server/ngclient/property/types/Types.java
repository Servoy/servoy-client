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

package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IAdjustablePropertyType;
import org.sablo.specification.property.types.IPropertyTypeFactory;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetInitialPageSizePropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetTreePropertyType;

/**
 * @author Johan
 *
 */
public class Types
{
	private static final Types typesInstance = new Types();
	private boolean registered = false;

	public static Types getTypesInstance()
	{
		return typesInstance;
	}

	public synchronized void registerTypes()
	{
		if (registered) return;
		registered = true;
		TypesRegistry.addType(JSUploadPropertyType.INSTANCE);
		TypesRegistry.addType(CSSPositionPropertyType.INSTANCE);
		TypesRegistry.addType(BorderPropertyType.INSTANCE);
		TypesRegistry.addType(DatasetPropertyType.INSTANCE);
		TypesRegistry.addType(RelationPropertyType.INSTANCE);
		TypesRegistry.addType(MediaPropertyType.INSTANCE);
		TypesRegistry.addType(LabelForPropertyType.INSTANCE);
		TypesRegistry.addType(FormPropertyType.INSTANCE);
		TypesRegistry.addType(FormComponentPropertyType.INSTANCE);
		TypesRegistry.addType(RuntimeComponentPropertyType.INSTANCE);
		TypesRegistry.addType(FormatPropertyType.INSTANCE);
		TypesRegistry.addType(FormScopePropertyType.INSTANCE);
		TypesRegistry.addType(MediaOptionsPropertyType.INSTANCE);
		TypesRegistry.addType(ClientFunctionPropertyType.CLIENT_FUNCTION_INSTANCE);
		TypesRegistry.addType(
			new FoundsetLinkedPropertyType<String, DataproviderTypeSabloValue>(DataproviderPropertyType.INSTANCE.getName(), DataproviderPropertyType.INSTANCE));
		TypesRegistry.addType(
			new FoundsetLinkedPropertyType<String, BasicTagStringTypeSabloValue>(TagStringPropertyType.INSTANCE.getName(), TagStringPropertyType.INSTANCE));
		TypesRegistry.addType(new FoundsetLinkedPropertyType<String, BasicTagStringTypeSabloValue>(TitleStringPropertyType.NG_INSTANCE.getName(),
			TitleStringPropertyType.NG_INSTANCE));
		TypesRegistry.addType(
			new FoundsetLinkedPropertyType<Object, ValueListTypeSabloValue>(ValueListPropertyType.INSTANCE.getName(), ValueListPropertyType.INSTANCE));
		TypesRegistry.addType(ServoyFunctionPropertyType.SERVOY_INSTANCE);
		TypesRegistry.addType(ServoyStringPropertyType.INSTANCE);
		TypesRegistry.addType(ModifiablePropertyType.INSTANCE);
		TypesRegistry.addType(ByteArrayResourcePropertyType.INSTANCE);
		TypesRegistry.addType(MediaDataproviderPropertyType.INSTANCE);
		TypesRegistry.addType(HTMLStringPropertyType.INSTANCE);
		TypesRegistry.addType(FindModePropertyType.INSTANCE);
		TypesRegistry.addType(ReadonlyPropertyType.INSTANCE);

		TypesRegistry.addType(NGObjectPropertyType.NG_INSTANCE);
		TypesRegistry.addType(ServoyAttributesPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGEnabledPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGVisiblePropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGColorPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGDatePropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGUUIDPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGDimensionPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGFontPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGInsetsPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGPointPropertyType.NG_INSTANCE);
		TypesRegistry.addType(NGTabSeqPropertyType.NG_INSTANCE);
		TypesRegistry.addType(RecordPropertyType.INSTANCE);
		TypesRegistry.addType(FunctionRefType.INSTANCE);
		TypesRegistry.addType(VariantPropertyType.INSTANCE);
		TypesRegistry.addType(NGStyleClassPropertyType.NG_INSTANCE);

		// TODO allow bean developer through a sort of plug point to contribute these kind of types themselves
		TypesRegistry.addType(FoundsetPropertyType.INSTANCE);
		TypesRegistry.addType(FoundsetInitialPageSizePropertyType.INSTANCE);
		TypesRegistry.addType(FoundsetTreePropertyType.INSTANCE);
		TypesRegistry.addType(ComponentPropertyType.INSTANCE);

		TypesRegistry.addTypeFactory(CustomJSONArrayType.TYPE_NAME, new IPropertyTypeFactory<PropertyDescription, Object>()
		{

			@Override
			public IAdjustablePropertyType<Object> createType(PropertyDescription params)
			{
				return new NGCustomJSONArrayType(params);
			}
		});
		TypesRegistry.addTypeFactory(CustomJSONObjectType.TYPE_NAME, new IPropertyTypeFactory<String, Object>()
		{

			@Override
			public IAdjustablePropertyType<Object> createType(String typeName)
			{
				return new NGCustomJSONObjectType(typeName, null);
			}
		});

		TypesRegistry.addType(JSEventType.INSTANCE);
		TypesRegistry.addType(JSDNDEventType.INSTANCE);
		TypesRegistry.addType(JSNativeJavaObjectType.INSTANCE);
		TypesRegistry.addType(FoundsetReferencePropertyType.INSTANCE);
		TypesRegistry.addType(FoundsetReferencePropertyTypeOld.INSTANCE);
		TypesRegistry.addType(RowReferencePropertyType.INSTANCE);
		TypesRegistry.addType(MapPropertyType.INSTANCE);
		TypesRegistry.addType(JSONPropertyType.INSTANCE);

		TypesRegistry.addType(DbIdentPropertyType.INSTANCE);
		TypesRegistry.addType(DynamicClientFunctionPropertyType.INSTANCE);
		TypesRegistry.addType(ValuelistConfigPropertyType.INSTANCE);
		TypesRegistry.addType(MenuPropertyType.INSTANCE);
		TypesRegistry.addType(MenuItemPropertyType.INSTANCE);
	}
}
