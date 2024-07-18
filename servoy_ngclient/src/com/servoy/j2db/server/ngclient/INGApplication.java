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

import java.util.Map;

import org.sablo.IChangeListener;
import org.sablo.specification.WebObjectApiFunctionDefinition;

import com.servoy.j2db.INGClientApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet.MediaInfo;
import com.servoy.j2db.util.Pair;

/**
 * Client with websocket-client specific features.
 *
 * @author rgansevles
 */
public interface INGApplication extends INGClientApplication
{
	INGClientWebsocketSession getWebsocketSession();

	IChangeListener getChangeListener();

	INGFormManager getFormManager();

	NGRuntimeWindowManager getRuntimeWindowManager();

	void changesWillBeSend();

	Pair<Long, Long> onStartSubAction(String serviceName, String functionName, WebObjectApiFunctionDefinition apiFunction, Object[] arguments);

	void onStopSubAction(Pair<Long, Long> perfId);

	void updateLastAccessed();

	void recreateForm(Form form, String name);

	void flushRecreatedForm(Form form, String formName);

	String registerClientFunction(String code);

	Map<String, String> getClientFunctions();

	/**
	 * @param dynamicID
	 * @return
	 */
	MediaInfo getMedia(String dynamicID);

	/**
	 * @param sabloValue
	 * @return
	 */
	MediaInfo createMediaInfo(byte[] sabloValue);

	void shutDown(boolean force);
}
