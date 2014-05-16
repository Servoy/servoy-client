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
 * Used to parse a JSON value (comming from client-side) representing the property and to update or create a new server-side Java Object based on it.<br>
 * The Java Object can be used later to sync this property with the browser component as needed in a similar fashion (see linked interfaces).<br><br>
 * 
 * For example "{ x: 10, y: 10 }" to a Java Point object.
 * @author acostescu
 * @see {@link IComplexPropertyValue}
 * @see {@link IDesignJSONToJavaPropertyConverter}
 * @see {@link IServerObjToJavaPropertyConverter}
 */
public interface IJSONToJavaPropertyConverter<CT, T extends IComplexPropertyValue>
{
	// TODO if we reach the conclusion that oldJavaObject can never be null for a complex type when an update comes from browser
	// we can delete this interface and move the method to IComplexPropertyObject

	/**
	 * Parses a JSON value (comming from client-side) representing the property and uses it to update or create a new server-side Java Object.<br>
	 * The Java Object can be used later to sync this property with the browser component as needed in a similar fashion.<br><br>
	 * 
	 * For example "{ x: 10, y: 10 }" to a Java Point object.
	 * @param jsonValue can be a JSONObject, JSONArray or primitive type.
	 * @return the new or updated Java object representing this property.
	 */
	T jsonToJava(Object jsonValue, T oldJavaObject, CT config);

}
