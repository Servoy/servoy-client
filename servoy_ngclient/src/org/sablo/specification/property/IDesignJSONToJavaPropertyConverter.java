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


/**
 * Used to parse a JSON value (comming from design-time) representing the property into a server-side Java Object representing the property value.<br>
 * The Java Object can be used later to sync this property with the browser component as needed (see linked interfaces).<br><br>
 * 
 * For example "{ x: 10, y: 10 }" to a Java Point object.
 * @author acostescu
 * @see {@link IComplexPropertyValue}
 * @see {@link IJSONToJavaPropertyConverter}
 * @see {@link IServerObjToJavaPropertyConverter}
 */
public interface IDesignJSONToJavaPropertyConverter<CT, T extends IComplexPropertyValue>
{

	/**
	 * Parses a JSON value (comming from design-time) representing the property into a server-side Java Object representing the property value.<br>
	 * The Java Object can be used later to sync this property with the browser component as needed.<br><br>
	 * 
	 * For example "{ x: 10, y: 10 }" to a Java Point object.
	 * @param jsonValue can be a JSONObject, JSONArray or primitive type.
	 * @param config the configuration of this property as defined in the .spec file. For example { type: 'valuelist', for: 'dataProviderID'}
	 * @return the Java object representing this property's value.
	 */
	T designJSONToJava(Object jsonValue, CT config);

}
