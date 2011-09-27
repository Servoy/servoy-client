/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.server.headlessclient;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.ScriptEngine;

/**
 * Special Scriptengine that has a separate Thread for executing functions.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 *
 */
final class WebScriptEngine extends ScriptEngine implements IFunctionExecutor
{
	private final ScriptExecutor executor = new ScriptExecutor();

	/**
	 * @param app
	 */
	WebScriptEngine(IApplication app)
	{
		super(app);
		Thread thread = new Thread(executor, "ScriptExecutor,clientid:" + app.getClientID()); //$NON-NLS-1$
		thread.setDaemon(true);
		thread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.ScriptEngine#destroy()
	 */
	@Override
	public void destroy()
	{
		executor.destroy();
		super.destroy();
	}

	@Override
	public Object executeFunction(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException)
		throws Exception
	{
		final FunctionEvent event = new FunctionEvent(this, f, scope, thisObject, args, focusEvent, throwException);

		executor.addEvent(event);
		if (event.getException() != null) throw event.getException();
		return event.getReturnValue();
	}

	public Object execute(Function f, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException) throws Exception
	{
		return super.executeFunction(f, scope, thisObject, args, focusEvent, throwException);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.IFunctionExecutor#block(java.lang.Object)
	 */
	public void suspend(Object object)
	{
		executor.suspend(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.IFunctionExecutor#resume(com.servoy.j2db.server.headlessclient.WebRuntimeWindow)
	 */
	public void resume(Object object)
	{
		executor.resume(object);
	}
}