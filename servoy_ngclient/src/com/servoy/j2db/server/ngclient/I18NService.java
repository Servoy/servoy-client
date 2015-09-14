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

import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.j2db.IApplication;

/**
 * @author jcompagner
 *
 */
public class I18NService implements IServerService
{
	public static final String NAME = "i18nService"; //$NON-NLS-1$

	private final IApplication application;

	public I18NService(IApplication application)
	{
		this.application = application;

	}

	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if ("getI18NMessages".equals(methodName))
		{
			JSONObject values = new JSONObject();
			for (int i = 0; i < args.length(); i++)
			{
				String key = args.getString(Integer.toString(i));
				values.put(key, application.getI18NMessage(key));

			}
			return values;
		}
		return null;
	}
}
