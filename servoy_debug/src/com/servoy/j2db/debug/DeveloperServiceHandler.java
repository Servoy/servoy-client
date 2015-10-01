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

package com.servoy.j2db.debug;

import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.Debug;

/**
 * A service which makes it possible for the client to interact with the developer.
 * @author emera
 */
public class DeveloperServiceHandler implements IServerService
{

	private final DebugNGClient client;

	public DeveloperServiceHandler(DebugNGClient debugNGClient)
	{
		this.client = debugNGClient;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if ("openFormInDesigner".equals(methodName))
		{
			String formName = args.optString("formname");
			Form form = formName != null ? client.getFormManager().getPossibleForm(formName) : null;
			if (form != null)
			{
				client.getDesignerCallback().showFormInDesigner(form);
			}
			else
			{
				Debug.error("Form " + formName + " was not found");
			}
		}
		return null;
	}

}
