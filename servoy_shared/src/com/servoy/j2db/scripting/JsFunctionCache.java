/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.DefaultJavaScope.JsMethod;

/**
 * Per-owner cache of the {@link NativeJavaMethod} functions that back the js functions of a scriptable object (foundset, record, dataset, scope).
 * <p>
 * The static (JVM wide) data of a class only holds immutable {@link JsMethod} entries; the (mutable, parent/prototype holding) function
 * instances are created here, once per owner and name, so that they are never shared between owners (and thus never between clients).
 * <p>
 * Owners should keep a field of this type that is <code>null</code> until the first function lookup.
 *
 * @since 2026.12
 */
public final class JsFunctionCache
{
	private final ConcurrentMap<String, NativeJavaMethod> functions = new ConcurrentHashMap<>();

	/**
	 * Gets the function for the given method, creating it on first access, and (re)sets its parent and prototype for the given start scope.
	 */
	public NativeJavaMethod get(JsMethod method, Scriptable start)
	{
		NativeJavaMethod function = getOrCreate(method);
		ScriptRuntime.setFunctionProtoAndParent(function, start);
		return function;
	}

	/**
	 * Same as {@link #get(JsMethod, Scriptable)} but with an explicit context.
	 */
	public NativeJavaMethod get(JsMethod method, Context cx, Scriptable start)
	{
		NativeJavaMethod function = getOrCreate(method);
		ScriptRuntime.setFunctionProtoAndParent(function, cx, start);
		return function;
	}

	private NativeJavaMethod getOrCreate(JsMethod method)
	{
		NativeJavaMethod function = functions.get(method.getName());
		if (function == null)
		{
			function = method.newFunction();
			NativeJavaMethod existing = functions.putIfAbsent(method.getName(), function);
			if (existing != null) function = existing;
		}
		return function;
	}
}
