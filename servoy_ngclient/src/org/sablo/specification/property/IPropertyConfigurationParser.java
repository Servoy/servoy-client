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

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

/**
 * Parses the configuration of a property when used in a component.<br>
 * For example "myComponentProperty: { type: 'myCustomType', myCustomTypeConfig1: true, myCustomTypeConfig2: [2, 4 ,6] }"
 * @author acostescu
 */
public interface IPropertyConfigurationParser<CT>
{

	/**
	 * Parses the JSON property configuration object into something that is easily usable later on by the property type implementation.<BR>
	 * Example of JSON: "myComponentProperty: { type: 'myCustomType', myCustomTypeConfig1: true, myCustomTypeConfig2: [2, 4 ,6] }"
	 * @return a custom property type controlled object that it is able to use later on; it will be stored in PropertyDescription and
	 * can be accessed via {@link PropertyDescription#getConfig()}
	 */
	public CT parseProperyConfiguration(JSONObject configObject);

}
