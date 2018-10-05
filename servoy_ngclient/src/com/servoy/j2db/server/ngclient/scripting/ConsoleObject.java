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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

/**
 * Support for console logging in the serverside logic of web objects.
 * @author gganea@servoy.com
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Console", scriptingName = "console")
@ServoyClientSupport(sc = false, wc = false, ng = true)
public class ConsoleObject
{
	private final IApplication app;

	public ConsoleObject(IApplication app)
	{
		this.app = app;
	}

	/**
	 * Report a message to the console.
	 *
	 * @sample
	 * console.log('some info')
	 *
	 * @param value the info
	 */
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

	/**
	 * Report a warning to the console.
	 *
	 * @sample
	 * console.warn('some warning')
	 *
	 * @param value the warning object
	 */
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

	/**
	 * Report an error to the console.
	 *
	 * @sample
	 * console.error('ERROR')
	 *
	 * @param value the error object
	 */
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
