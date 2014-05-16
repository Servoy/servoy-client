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

package com.servoy.j2db.server.ngclient.properties;

import org.sablo.specification.property.IComplexTypeImpl;
import org.sablo.specification.property.IDesignJSONToJavaPropertyConverter;
import org.sablo.specification.property.IJSONToJavaPropertyConverter;
import org.sablo.specification.property.IPropertyConfigurationParser;
import org.sablo.specification.property.IServerObjToJavaPropertyConverter;

import com.servoy.j2db.util.Debug;

/**
 * Implementation for the complex custom type "foundset".
 * 
 * @author acostescu
 */
public class FoundsetTypeImpl implements IComplexTypeImpl<Object, FoundsetTypeValue>
{

	@Override
	public IPropertyConfigurationParser<Object> getPropertyConfigurationParser()
	{
		return null;
	}

	@Override
	public IJSONToJavaPropertyConverter<Object, FoundsetTypeValue> getJSONToJavaPropertyConverter(boolean isArray)
	{
		return new IJSONToJavaPropertyConverter<Object, FoundsetTypeValue>()
		{

			@Override
			public FoundsetTypeValue jsonToJava(Object jsonValue, FoundsetTypeValue oldJavaObject, Object config)
			{
				if (oldJavaObject == null) Debug.error("Somehow oldValue was null when getting browser updates (for foundset type property)...");
				else
				{
					oldJavaObject.browserUpdatesReceived(jsonValue);
				}
				return oldJavaObject;
			}

		};
	}

	@Override
	public IDesignJSONToJavaPropertyConverter<Object, FoundsetTypeValue> getDesignJSONToJavaPropertyConverter(boolean isArray)
	{
		return new IDesignJSONToJavaPropertyConverter<Object, FoundsetTypeValue>()
		{

			@Override
			public FoundsetTypeValue designJSONToJava(Object jsonValue, Object config)
			{
				return new FoundsetTypeValue(jsonValue, config);
			}

		};
	}

	@Override
	public IServerObjToJavaPropertyConverter<Object, FoundsetTypeValue> getServerObjectToJavaPropertyConverter(boolean isArray)
	{
		// TODO implement more here if we want this type of properties accessible in scripting
		return new IServerObjToJavaPropertyConverter<Object, FoundsetTypeValue>()
		{

			@Override
			public boolean usesServerObjRepresentation()
			{
				return false;
			}

			@Override
			public FoundsetTypeValue serverObjToJava(Object jsonValue, Object config, FoundsetTypeValue oldValue)
			{
				return oldValue;
			}

		};
	}

}
