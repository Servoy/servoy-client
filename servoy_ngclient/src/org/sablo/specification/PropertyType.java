/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sablo.specification;

import org.sablo.specification.property.IDesignJSONToJavaPropertyConverter;
import org.sablo.specification.property.IJSONToJavaPropertyConverter;
import org.sablo.specification.property.IPropertyConfigurationParser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IServerObjToJavaPropertyConverter;


/**
 * Base class for property types in web component spec files.
 * 
 * @author rgansevles
 * @author acostescu
 */
@SuppressWarnings("nls")
public class PropertyType implements IPropertyType
{

	private final String typeName;
	private final Default defaultEnumValue;

	public PropertyType(String typeName)
	{
		this(typeName, Default.customDoNotTreatThisInSwitches);
	}

	public PropertyType(String typeName, Default defaultEnumValue)
	{
		this.typeName = typeName;
		this.defaultEnumValue = defaultEnumValue;
	}

	@Override
	public Default getDefaultEnumValue()
	{
		return defaultEnumValue;
	}

	@Override
	public String getName()
	{
		return typeName;
	}

	@Override
	public String toString()
	{
		return typeName + " - Property type";
	}

	@Override
	public PropertyDescription getCustomJSONTypeDefinition()
	{
		return null;
	}

	@Override
	public IPropertyConfigurationParser<?> getPropertyConfigurationParser()
	{
		return null;
	}

	@Override
	public IJSONToJavaPropertyConverter<?, ?> getJSONToJavaPropertyConverter(boolean isArray)
	{
		return null;
	}

	@Override
	public IDesignJSONToJavaPropertyConverter<?, ?> getDesignJSONToJavaPropertyConverter(boolean isArray)
	{
		return null;
	}

	@Override
	public IServerObjToJavaPropertyConverter<?, ?> getServerObjectToJavaPropertyConverter(boolean isArray)
	{
		return null;
	}

}
