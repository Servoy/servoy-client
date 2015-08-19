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

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.websocket.utils.DataConversion;

/**
 * Types that implement this interface do send some useful contents in template JSON (either through {@link NGConversions.IFormElementToTemplateJSON},
 * or just the design value is useful large json) but at runtime - when the application/record/... becomes available the want to update that value without
 * sending it again completely.<br/><br/>
 *
 * For example a "component" (child component) type sends in the template any child component properties that want to be in template, and at runtime (when initial
 * data is requested) they should only send initial runtime property changes, not the whole bunch of child component properties that were aready sent/cached through
 * the template. The same thing that a form currently does.
 *
 * @author acostescu
 */
public interface ITemplateValueUpdaterType<T> extends IPropertyConverterForBrowser<T>
{

	/**
	 * Writes changes that are to be interpreted as an update to the already-sent-through-template property value.<br/>
	 * It will get called as part of the initial data push after runtime components become available on client.<br/><br/>
	 *
	 * You can look at it as a diff between the template content and the current state of the property at runtime.
	 *
	 * @param writer the JSON writer to write JSON converted data to.
	 * @param key if this value will be part of a JSONObject then key is non-null and you MUST do writer.key(...) before adding the converted value. This
	 * is useful for cases when you don't want the value written at all in resulting JSON in which case you don't write neither key or value. If
	 * key is null and you want to write the converted value write only the converted value to the writer, ignore the key.
	 * @param object the property value to convert to JSON.
	 * @param propertyDescription the description of the property
	 * @param clientConversion can be use to mark needed client/browser side conversion types.
	 * @return the writer for cascaded usage.
	 * @throws JSONException if a JSON exception happens.
	 */
	JSONWriter initialToJSON(JSONWriter writer, String key, T sabloValue, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException;

}
