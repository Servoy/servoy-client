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
 * Used to transform a server side set java object (coming from custom server side logic) into a server-side Java Object representing the property value.<br>
 * The Java Object can be used later to sync this property with the browser component as needed (see linked interfaces).<br><br>
 * 
 * For example "MyCustomPoint" to a Java Point object.
 * @author acostescu
 * @see {@link IComplexPropertyValue}
 * @see {@link IDesignJSONToJavaPropertyConverter}
 * @see {@link IJSONToJavaPropertyConverter}
 */
public interface IServerObjToJavaPropertyConverter<CT, T extends IComplexPropertyValue>
{

	/**
	 * Returns true if the property type using this object uses implementation specific server representations of the property and false otherwise.
	 */
	boolean usesServerObjRepresentation();

	/**
	 * Transforms a server side set java object (coming from custom server side logic) into a server-side Java Object representing the property value.<br>
	 * The Java Object can be used later to sync this property with the browser component as needed.<br><br>
	 * 
	 * This method is only called if {@link #usesServerObjRepresentation()} returned true.
	 * 
	 * For example "MyCustomPoint" instance to a Java Point object.
	 * @param jsonValue can be a JSONObject, JSONArray or primitive type.
	 * @return the Java object representing this property's value.
	 */
	T serverObjToJava(Object jsonValue, CT config, T oldJavaObject);

}
