/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;

/**
 * @author jcompagner
 *
 */
public class Deferred
{
	private final IApplication application;
	private final NativePromise promise;

	private BaseFunction resolve;
	private BaseFunction reject;

	public Deferred(IClientPluginAccess access)
	{
		this(((ClientPluginAccessProvider)access).getApplication());
	}

	public Deferred(IApplication application)
	{
		this.application = application;
		BaseFunction fun = new BaseFunction()
		{
			@Override
			public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
			{
				resolve = (BaseFunction)args[0];
				reject = (BaseFunction)args[1];
				return null;
			}
		};
		promise = (NativePromise)ScriptRuntime.newObject(Context.getCurrentContext(), ScriptRuntime.getTopCallScope(Context.getCurrentContext()), "Promise", //$NON-NLS-1$
			new Object[] { fun });
	}

	public void resolve(Object value)
	{
		application.invokeLater(() -> {
			Context cx = Context.enter();
			try
			{
				Object converted = value;
				if (value != null)
				{
					converted = ScriptRuntime.toObject(promise, value);
				}
				resolve.call(cx, promise, promise, new Object[] { converted });
				cx.processMicrotasks();
			}
			finally
			{
				Context.exit();
			}
		});
	}

	public void reject(Object value)
	{
		application.invokeLater(() -> {
			Context cx = Context.enter();
			try
			{
				Object converted = value;
				if (value != null)
				{
					converted = ScriptRuntime.toObject(promise, converted);
				}
				reject.call(cx, promise, promise, new Object[] { value });
				cx.processMicrotasks();
			}
			finally
			{
				Context.exit();
			}
		});

	}

	public NativePromise getPromise()
	{
		return promise;
	}

}
