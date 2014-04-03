/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import org.json.JSONObject;

import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.server.ngclient.component.WebComponentApiDefinition;

/**
 * @author jcompagner
 *
 */
public interface IDataAdapterList extends IDataConverter
{
	/**
	 * @param webComponent
	 * @param string
	 */
	void pushChanges(WebComponent webComponent, String string);

	/**
	 * @param webComponent
	 * @param string
	 * @param newValue
	 */
	void pushChanges(WebComponent webComponent, String string, Object newValue);

	/**
	 * @param webComponent
	 * @param string
	 * @param args
	 */
	Object execute(WebComponent webComponent, String event, int eventId, Object[] args);

	/**
	 * @param apiDefinition
	 * @param elementName
	 * @param args
	 * @return
	 */
	Object executeApi(WebComponentApiDefinition apiDefinition, String elementName, Object[] args);

	/**
	 * @param script
	 * @param args
	 * @return
	 */
	Object executeInlineScript(String script, JSONObject args);

	/**
	 * @param record
	 * @return 
	 */
	void setRecord(IRecord record, boolean fireChangeEvent);

	/**
	 * 
	 * @param webComponent
	 * @param property
	 */
	void startEdit(WebComponent webComponent, String property);

	/**
	 * @param findMode
	 */
	void setFindMode(boolean findMode);

}
