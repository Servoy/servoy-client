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
package com.servoy.j2db.scripting;

import static com.servoy.j2db.scripting.RecordingCallback.wrapCallbacksIfNeeded;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Function delegate that wraps objects returned from function calls when needed.
 * Used to determine dependencies for calculations.
 *
 * @author rgansevles
 *
 */
public class RecordingFunction extends RecordingScriptable implements Function
{
	RecordingFunction(String functionName, Function function)
	{
		super(functionName, function);
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		return wrapIfNeeded(null, null,
			((Function)scriptable).call(cx, scope, (Scriptable)unwrapScriptable(thisObj), wrapCallbacksIfNeeded(args)));
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args)
	{
		return ((Function)scriptable).construct(cx, scope, args);
	}

	static Function wrapFunctionIfNeeded(String functionName, Function function)
	{
		if (function == null || function instanceof RecordingFunction) return function;
		return new RecordingFunction(functionName, function);
	}

}
