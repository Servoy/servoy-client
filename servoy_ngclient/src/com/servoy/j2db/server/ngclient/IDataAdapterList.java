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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.WebComponent;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;

/**
 * @author jcompagner
 */
public interface IDataAdapterList extends ITagResolver
{

	void pushChanges(WebFormComponent webComponent, String string);

	void pushChanges(WebFormComponent webComponent, String string, String foundsetLinkedRowID);

	void pushChanges(WebFormComponent webComponent, String string, Object newValue, String foundsetLinkedRowID);

	Object executeEvent(WebComponent webComponent, String event, int eventId, Object[] args);

	/**
	 * @param args args to replace in script
	 * @param appendingArgs args to append in script execution
	 */
	Object executeInlineScript(String script, JSONObject args, JSONArray appendingArgs);

	void setRecord(IRecord record, boolean fireChangeEvent);

	void startEdit(WebFormComponent webComponent, String property, String foundsetLinkedRowID);

	void setFindMode(boolean findMode);

	INGApplication getApplication();

	IWebFormController getForm();

	String getDataProviderID(WebFormComponent webComponent, String beanProperty);

	void addVisibleChildForm(IWebFormController form, String relation, boolean shouldUpdateParentFormController);

	void removeVisibleChildForm(IWebFormController form, boolean firstLevel);

	Map<IWebFormController, String> getRelatedForms();

	void addParentRelatedForm(IWebFormController form);

	void removeParentRelatedForm(IWebFormController form);

	List<IWebFormController> getParentRelatedForms();

	IRecordInternal getRecord();

	Object getValueObject(IRecord recordToUse, String dataProviderId);

	void destroy();

	void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables, Set<IWebFormController> childFormsThatWereNotified);

	boolean stopUIEditing(boolean looseFocus);

	void addDataLinkedProperty(IDataLinkedPropertyValue propertyValue, TargetDataLinks dataLinks);

	void removeDataLinkedProperty(IDataLinkedPropertyValue propertyValue);

	void removeFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue);

	void addFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue);

}
