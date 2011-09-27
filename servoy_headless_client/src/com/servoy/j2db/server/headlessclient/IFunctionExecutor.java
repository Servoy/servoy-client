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

/**
 * ScriptEngines that have a separate Thread to execute scripts should implement this interface.
 * 
 * @author jcompagner
 * 
 * @since 6.1
 */
public interface IFunctionExecutor
{

	/**
	 * @param function
	 * @param scope
	 * @param thisObject
	 * @param args
	 * @param focusEvent
	 * @param throwException
	 * @return
	 */
	Object execute(Function function, Scriptable scope, Scriptable thisObject, Object[] args, boolean focusEvent, boolean throwException) throws Exception;

	/**
	 * @param webRuntimeWindow
	 */
	void suspend(Object object);

	/**
	 * @param webRuntimeWindow
	 */
	void resume(Object object);

}
