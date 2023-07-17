/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import static com.servoy.j2db.scripting.RecordingScriptable.unwrapScriptable;
import static com.servoy.j2db.util.Utils.arrayMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

/**
 * Function delegate that wraps function parameters to function calls when needed.
 * Used to determine dependencies for calculations.
 *
 * Note that this class cannot implement {@link Wrapper} (as opposed to {@link RecordingFunction}), the parameters would otherwise be unwrapped before calling the function.
 *
 * @author rgansevles
 *
 */
public class RecordingCallback extends AbstractRecordingScriptable implements Function // Not Wrapper!
{
	public RecordingCallback(Function function)
	{
		super(null, function);
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		return ((Function)scriptable).call(cx, scope, (Scriptable)unwrapScriptable(thisObj), wrapIfNeeded(args));
	}

	static Object[] wrapCallbacksIfNeeded(Object[] args)
	{
		return arrayMap(args, RecordingCallback::wrapCallbackIfNeeded);
	}

	static Object wrapCallbackIfNeeded(Object o)
	{
		if (o instanceof Function && !(o instanceof RecordingCallback))
		{
			return new RecordingCallback((Function)o);
		}

		return o;
	}


	public Scriptable construct(Context cx, Scriptable scope, Object[] args)
	{
		return ((Function)scriptable).construct(cx, scope, args);
	}

}
