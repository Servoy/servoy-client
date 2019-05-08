/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db;

import java.util.Collection;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.protocol.http.WebSession;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.IDebugHeadlessClient;
import com.servoy.j2db.server.shared.WebCredentials;

/**
 * Handler for debug clients
 * @author rgansevles
 *
 */
public interface IDebugClientHandler
{

	ISessionClient createDebugHeadlessClient(ServletRequest req, String userName, String password, String method, Object[] objects, String preferedSolution)
		throws Exception;

	ISessionClient createDebugAuthenticator(String authenticatorName, String method, Object[] objects) throws Exception;

	IWebClientApplication createDebugWebClient(WebSession webClientSession, HttpServletRequest req, WebCredentials credentials, String method, Object[] objects)
		throws Exception;

	IDebugClient createDebugNGClient(Object wsSession) throws Exception;

	List<IDebugClient> getActiveDebugClients();

	IDebugJ2DBClient getDebugSmartClient();

	IDebugHeadlessClient getDebugHeadlessClient();

	IDebugWebClient getDebugWebClient();

	IDebugNGClient getDebugNGClient();

	IDebugNGDesktopClient getDebugNGDesktopClient();

	<T extends IDebugClient> T getDebugClient(DebugClientType<T> type);

	void flagModelInitialised();

	void setDesignerCallback(IDesignerCallback designerCallback);

	IApplication getDebugReadyClient();

	void reloadDebugSolution(Solution solution);

	void reloadDebugSolutionSecurity();

	void reloadAllStyles();

	void executeMethod(ISupportChilds persist, String scopeName, String methodname);

	void showInDebugClients(Form form);

	public void refreshDebugClientsI18N(boolean recreateForms);

	void refreshDebugClients(Collection<IPersist> changes);

	void refreshDebugClients(ITable table);

	boolean isClientStarted();

	void flushValueList(ValueList valueList);

	void setSolution(Solution solution);
}
