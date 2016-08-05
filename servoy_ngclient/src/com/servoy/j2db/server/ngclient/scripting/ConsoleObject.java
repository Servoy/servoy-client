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

package com.servoy.j2db.server.ngclient.scripting;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;

/**
 * @author gganea@servoy.com
 *
 */
public class ConsoleObject
{
	private final IApplication app;

	public ConsoleObject(IApplication app)
	{
		this.app = app;
	}

	@JSFunction
	public void log(Object value)
	{
		app.reportJSInfo(value != null ? value.toString() : "null");
	}

	@JSFunction
	public void warn(Object value)
	{
		app.reportJSWarning(value != null ? value.toString() : "null");
	}

	@JSFunction
	public void error(Object value)
	{
		EvaluatorException e = new EvaluatorException(value != null ? value.toString() : "null");
		app.reportJSWarning(e.getMessage());
		app.reportJSWarning(e.getScriptStackTrace());
	}

}
