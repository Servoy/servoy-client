/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.persistence;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

/**
 * An {@link IBasicWebObject} that is always a child/property of a parent component or WebCustomType. It is aware of it's location in parent (property name and maybe index as well in case of array)
 *
 * @author acostescu
 */
public interface IChildWebObject extends IBasicWebObject
{

	public final static String UUID_KEY = "svyUUID";

	/**
	 * In case this persist is nested inside a WebComponent or a WebCustomType, the json key is the key in the parent persist that has this value.
	 */
	String getJsonKey();

	void setJsonKey(String newJsonKey);

	/**
	 * In case this persist is nested inside a WebComponent or a WebCustomType via an array property, the index is the index of this persist inside the parent's array property specified by {@link #getJsonKey()}.
	 */
	int getIndex();

	/**
	 * @see #getIndex()
	 */
	public void setIndex(int i);

	/**
	 * Some WebObjects hold in their (full) .frm JSON representation more then their json properties. In this case {@link #getFullJsonInFrmFile()} differs from {@link #getJson()}.
	 * For example a for a custom object {@link #getFullJsonInFrmFile()} is identical to {@link #getJson()} because it's just a map of key-value pairs in the .frm.
	 * But for {@link ChildWebComponent} persists, their .frm has all their json properties as a sub-property but also has the type of the component. In this case {@link #getJson()} refers
	 * to the component's properties, while {@link #getFullJsonInFrmFile()} refers to what the content is directly in the .frm file.
	 */
	JSONObject getFullJsonInFrmFile();

	PropertyDescription getPropertyDescription();

	@Override
	IBasicWebObject getParent();

	void resetUUID();

}
