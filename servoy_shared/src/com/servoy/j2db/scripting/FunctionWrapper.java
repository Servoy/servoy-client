/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.server.shared.PerformanceData;

/**
 * @author jcomp
 *
 */
public class FunctionWrapper implements Function
{
	private final Function function;
	private final PerformanceData performanceData;
	private final String clientID;
	private final String name;

	public FunctionWrapper(Function function, String name, PerformanceData performanceData, String clientID)
	{
		this.function = function;
		this.performanceData = performanceData;
		this.name = name;
		this.clientID = clientID;
	}

	/**
	 * In almost all cases - that just deal with any Function impl. - you should not need to call this method.
	 * It returns the wrapped method.
	 */
	public Function getWrappedFunction()
	{
		return function;
	}

	private Scriptable fixStart(Scriptable start)
	{
		if (start == this)
		{
			return function;
		}
		return start;
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		Integer pfId = performanceData.startAction(name, System.currentTimeMillis(), IDataServer.METHOD_CALL, clientID);
		try
		{
			return function.call(cx, fixStart(scope), fixStart(thisObj), args);
		}
		finally
		{
			if (pfId != null)
			{
				performanceData.endAction(pfId, clientID);
			}
		}
	}

	public String getClassName()
	{
		return function.getClassName();
	}

	public Scriptable construct(Context cx, Scriptable scope, Object[] args)
	{
		return function.construct(cx, fixStart(scope), args);
	}

	public Object get(String pName, Scriptable start)
	{
		return function.get(pName, fixStart(start));
	}

	public Object get(int index, Scriptable start)
	{
		return function.get(index, fixStart(start));
	}

	public boolean has(String pName, Scriptable start)
	{
		return function.has(pName, fixStart(start));
	}

	public boolean has(int index, Scriptable start)
	{
		return function.has(index, fixStart(start));
	}

	public void put(String pName, Scriptable start, Object value)
	{
		function.put(pName, fixStart(start), value);
	}

	public void put(int index, Scriptable start, Object value)
	{
		function.put(index, fixStart(start), value);
	}

	public void delete(String pName)
	{
		function.delete(pName);
	}

	public void delete(int index)
	{
		function.delete(index);
	}

	public Scriptable getPrototype()
	{
		return function.getPrototype();
	}

	public void setPrototype(Scriptable prototype)
	{
		function.setPrototype(prototype);
	}

	public Scriptable getParentScope()
	{
		return function.getParentScope();
	}

	public void setParentScope(Scriptable parent)
	{
		function.setParentScope(parent);
	}

	public Object[] getIds()
	{
		return function.getIds();
	}

	public Object getDefaultValue(Class< ? > hint)
	{
		return function.getDefaultValue(hint);
	}

	public boolean hasInstance(Scriptable instance)
	{
		return function.hasInstance(fixStart(instance));
	}

}
