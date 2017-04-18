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

package com.servoy.j2db.server.ngclient;

import java.io.IOException;

import org.sablo.websocket.IWindow;

import com.servoy.j2db.persistence.Form;

/**
 * Represents the client side window object on the server for NGClient.
 *
 * @author rgansevles
 *
 */
public interface INGClientWindow extends IWindow
{

	public interface IFormHTMLAndJSGenerator
	{

		String generateHTMLTemplate();

		String generateJS() throws IOException;

	}

	boolean hasForm(String realName);

	/**
	 * Returns true only if the form was not previously sent to client at all or if the version of the form that was sent to the client is no longer up-to-date (solution model change).
	 */
	boolean hasFormChangedSinceLastSendToClient(Form flattenedForm, String realName);

	void updateForm(Form form, String name, IFormHTMLAndJSGenerator formTemplateGenerator);

	void setFormResolved(String formName, boolean resolved);

	void destroyForm(String name);

	void touchForm(Form flattenedForm, String realInstanceName, boolean async, boolean testForValidForm);

	@Override
	INGClientWebsocketSession getSession();

	/**
	 * @param formName
	 */
	void registerAllowedForm(String formName, INGFormElement element);

	/**
	 * @param relationName
	 * @param element
	 */
	String registerAllowedRelation(String relationName, INGFormElement element);

	/**
	 * @param formName
	 * @param uuidRelationName
	 * @param element
	 * @return
	 */
	String isVisibleAllowed(String formName, String uuidRelationName, INGFormElement element) throws IllegalAccessException;

}
