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

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.WebComponent;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.server.ngclient.IChangeListener;

/**
 * The representation of a property value for a complex type property - server side.
 * It can be used to generate a JSON value (to be sent client-side) representing the property when there is a change in the server-side Java Object of this property.<br>
 * The Java Object can get updates from the browser component as needed in a similar fashion (see linked interfaces).<br><br>
 * 
 * For example a Java Point object could be represented as "{ x: 10, y: 10 }".
 * @author acostescu
 * @see {@link IJSONToJavaPropertyConverter}
 * @see {@link IDesignJSONToJavaPropertyConverter}
 * @see {@link IServerObjToJavaPropertyConverter}
 */
public interface IComplexPropertyValue
{

	public static final Object NOT_AVAILABLE = new Object();

	/**
	 * Method that will get called when this property value is attached to a component.<br>
	 * NOTE: other methods of this interface might get called prior to init - for initial values to be sent to browser in form templates.
	 * 
	 * @param changeMonitor an object that can be used to notify the system that something in this property has changed.
	 * @param component the component to which the complex property belongs.
	 * @param propertyName the name of the property that this value was assigned to. (can be nested with '.' if the value is inside a custom JSON property leaf)
	 */
	void init(IChangeListener changeMonitor, WebComponent component, String propertyName);

	/**
	 * Transforms this property value object into a JSON to be sent to the client<br>
	 * 
	 * For example a Java Point object could be represented as "{ x: 10, y: 10 }".
	 * @return the new or updated Java object representing this property.
	 */
	JSONWriter toJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException;

	/**
	 * Write changes of this property value object into a JSON to be sent to the client. It can be a custom JSON that will be interpreted by
	 * custom client side code.
	 * 
	 * For example a Java Point object change only on x axis could be represented as "{ x: 10 }".
	 * @return the new or updated Java object representing this property.
	 */
	JSONWriter changesToJSON(JSONWriter destinationJSON, DataConversion conversionMarkers) throws JSONException;

	/**
	 * Writes the design JSON that would be transformed into this property object.<br>
	 * This method must be able to run even if the change monitor 
	 * @param writer
	 * @return
	 */
	JSONWriter toDesignJSON(JSONWriter writer) throws JSONException;

	/**
	 * Transforms this property value representation into an implementation specific server side object.
	 * @return either the implementation specific object or {@link #NOT_AVAILABLE}, if this property doesn't provide such an object.
	 */
	Object toServerObj();

}
