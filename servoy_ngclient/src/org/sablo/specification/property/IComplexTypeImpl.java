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

package org.sablo.specification.property;

import org.sablo.specification.PropertyDescription;


/**
 * Implementors of this interface are able to provide complex property type behavior (special client-side/server-side handling) for JSON custom property types.
 * 
 * @author acostescu
 */
public interface IComplexTypeImpl<CT, T extends IComplexPropertyValue>
{

	/**
	 * Parser used to parse the JSON property configuration object into something that is easily usable later on (through {@link PropertyDescription#getConfig()}) by the property type implementation.<BR>
	 * Example of JSON: "myComponentProperty: { type: 'myCustomType', myCustomTypeConfig1: true, myCustomTypeConfig2: [2, 4 ,6] }"<BR><BR>
	 * 
	 * If this is null but the property declaration contains configuration information, {@link PropertyDescription#getConfig()} will contain the actual JSON object. 
	 */
	IPropertyConfigurationParser<CT> getPropertyConfigurationParser();

	// TODO ac document this
	IJSONToJavaPropertyConverter<CT, T> getJSONToJavaPropertyConverter(boolean isArray);

	// TODO ac document this
	IDesignJSONToJavaPropertyConverter<CT, T> getDesignJSONToJavaPropertyConverter(boolean isArray);

	// TODO ac document this
	IServerObjToJavaPropertyConverter<CT, T> getServerObjectToJavaPropertyConverter(boolean isArray);

}