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
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.util.Utils;

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
		if (app instanceof IDebugClient)
		{
			((IDebugClient)app).reportToDebugger(getValueAsString(value), false);
		}
		else
		{
			app.reportJSInfo(getValueAsString(value));
		}
	}

	@JSFunction
	public void warn(Object value)
	{
		if (app instanceof IDebugClient)
		{
			((IDebugClient)app).reportToDebugger(getValueAsString(value), true);
		}
		else
		{
			app.reportJSWarning(getValueAsString(value));
		}
	}

	@JSFunction
	public void error(Object value)
	{
		if (app instanceof IDebugClient)
		{
			((IDebugClient)app).reportToDebugger(getValueAsString(value), true);
		}
		else
		{
			EvaluatorException e = new EvaluatorException(getValueAsString(value));
			app.reportJSWarning(e.getMessage());
			app.reportJSWarning(e.getScriptStackTrace());
		}
	}

	private String getValueAsString(Object value)
	{
		return value != null ? (value instanceof Scriptable ? Utils.getScriptableString((Scriptable)value) : value.toString()) : "null";
	}
}
