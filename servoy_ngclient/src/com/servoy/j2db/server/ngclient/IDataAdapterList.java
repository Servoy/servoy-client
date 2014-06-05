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
import org.sablo.WebComponent;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.dataprocessing.IRecord;

/**
 * @author jcompagner
 */
public interface IDataAdapterList extends IDataConverter, ITagResolver
{
	/**
	 * @param webComponent
	 * @param string
	 */
	void pushChanges(WebFormComponent webComponent, String string);

	/**
	 * @param webComponent
	 * @param string
	 * @param newValue
	 */
	void pushChanges(WebFormComponent webComponent, String string, Object newValue);

	/**
	 * @param webComponent
	 * @param string
	 * @param args
	 */
	Object executeEvent(WebComponent webComponent, String event, int eventId, Object[] args);

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
	void startEdit(WebFormComponent webComponent, String property);

	/**
	 * @param findMode
	 */
	void setFindMode(boolean findMode);

	INGApplication getApplication();

	IWebFormController getForm();

	void addRelatedForm(IWebFormController form, String relation);

	void removeRelatedForm(IWebFormController form);
}
