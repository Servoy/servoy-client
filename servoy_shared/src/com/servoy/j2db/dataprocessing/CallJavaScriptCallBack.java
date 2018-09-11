/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.util.Debug;

class CallJavaScriptCallBack implements IRecordCallback
{
	private final Function callback;
	private final IExecutingEnviroment scriptEngine;
	private final Scriptable thisObject;

	public CallJavaScriptCallBack(Function callback, IExecutingEnviroment scriptEngine, Scriptable thisObject)
	{
		this.callback = callback;
		this.scriptEngine = scriptEngine;
		this.thisObject = thisObject;
	}

	@Override
	public Object handleRecord(IRecord record, int recordIndex, IFoundSet foundset)
	{
		Scriptable callbackScope = callback.getParentScope();
		try
		{
			return scriptEngine.executeFunction(callback, callbackScope, (Scriptable)(thisObject == null ? foundset : thisObject),
				new Object[] { record, Integer.valueOf(recordIndex + 1), foundset }, false, true);
		}
		catch (Exception ex)
		{
			Debug.error("Error executing callback: ", ex);
			if (ex instanceof RuntimeException)
			{
				throw (RuntimeException)ex;
			}
		}
		return null;
	}
}