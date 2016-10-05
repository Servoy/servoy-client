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

/**
 * A web object that might not be not aware of it's PropertyDescription. For legacy purposes only (Bean usage for web components).
 * !!! Keep this interface free of any sablo dependencies, or remove Bean legacy as web component support !!!
 *
 * @author acostescu
 */
public interface IBasicWebObject extends ISupportChilds
{

	void setName(String arg);

	String getName();

	void setTypeName(String arg);

	String getTypeName();

	JSONObject getFlattenedJson();

	void setProperty(String propertyName, Object val);

	void clearProperty(String propertyName);

	Object getProperty(String propertyName);

	IBasicWebComponent getParentComponent();

	void updateJSON();

	JSONObject getJson();

	void clearChanged();

	Object getOwnProperty(String propertyName);
}
