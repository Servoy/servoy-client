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

package com.servoy.j2db.server.ngclient.design;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * @author gganea@servoy.com
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "console", scriptingName = "console")
public class ConsoleObject
{
	private final ClientState clientState;

	/**
	 * @param client
	 */
	public ConsoleObject(ClientState client)
	{
		this.clientState = client;
	}

	@JSFunction
	public void log(Object value)
	{
		clientState.reportJSInfo(value.toString());
	}

	@JSFunction
	public void warn(Object value)
	{
		clientState.reportJSWarning(value.toString());
	}

	@JSFunction
	public void error(Object value)
	{
		EvaluatorException e = new EvaluatorException(value.toString());
		clientState.reportJSWarning(e.getMessage());
		clientState.reportJSWarning(e.getScriptStackTrace());
	}
}
