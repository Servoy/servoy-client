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
import com.servoy.j2db.util.UUID;

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

	private Scriptable fixStart(Scriptable start)
	{
		if (start == this)
		{
			return function;
		}
		return start;
	}

	/**
	 * @param cx
	 * @param scope
	 * @param thisObj
	 * @param args
	 * @return
	 * @see org.mozilla.javascript.Function#call(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		UUID pfUuid = performanceData != null ? performanceData.startAction(name, System.currentTimeMillis(), IDataServer.METHOD_CALL, clientID) : null;
		try
		{
			return function.call(cx, fixStart(scope), fixStart(thisObj), args);
		}
		finally
		{
			if (pfUuid != null)
			{
				performanceData.endAction(pfUuid, clientID);
			}
		}
	}

	/**
	 * @return
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
	public String getClassName()
	{
		return function.getClassName();
	}

	/**
	 * @param cx
	 * @param scope
	 * @param args
	 * @return
	 * @see org.mozilla.javascript.Function#construct(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	public Scriptable construct(Context cx, Scriptable scope, Object[] args)
	{
		return function.construct(cx, fixStart(scope), args);
	}

	/**
	 * @param name
	 * @param start
	 * @return
	 * @see org.mozilla.javascript.Scriptable#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public Object get(String name, Scriptable start)
	{
		return function.get(name, fixStart(start));
	}

	/**
	 * @param index
	 * @param start
	 * @return
	 * @see org.mozilla.javascript.Scriptable#get(int, org.mozilla.javascript.Scriptable)
	 */
	public Object get(int index, Scriptable start)
	{
		return function.get(index, fixStart(start));
	}

	/**
	 * @param name
	 * @param start
	 * @return
	 * @see org.mozilla.javascript.Scriptable#has(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(String name, Scriptable start)
	{
		return function.has(name, fixStart(start));
	}

	/**
	 * @param index
	 * @param start
	 * @return
	 * @see org.mozilla.javascript.Scriptable#has(int, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(int index, Scriptable start)
	{
		return function.has(index, fixStart(start));
	}

	/**
	 * @param name
	 * @param start
	 * @param value
	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(String name, Scriptable start, Object value)
	{
		function.put(name, fixStart(start), value);
	}

	/**
	 * @param index
	 * @param start
	 * @param value
	 * @see org.mozilla.javascript.Scriptable#put(int, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(int index, Scriptable start, Object value)
	{
		function.put(index, fixStart(start), value);
	}

	/**
	 * @param name
	 * @see org.mozilla.javascript.Scriptable#delete(java.lang.String)
	 */
	public void delete(String name)
	{
		function.delete(name);
	}

	/**
	 * @param index
	 * @see org.mozilla.javascript.Scriptable#delete(int)
	 */
	public void delete(int index)
	{
		function.delete(index);
	}

	/**
	 * @return
	 * @see org.mozilla.javascript.Scriptable#getPrototype()
	 */
	public Scriptable getPrototype()
	{
		return function.getPrototype();
	}

	/**
	 * @param prototype
	 * @see org.mozilla.javascript.Scriptable#setPrototype(org.mozilla.javascript.Scriptable)
	 */
	public void setPrototype(Scriptable prototype)
	{
		function.setPrototype(prototype);
	}

	/**
	 * @return
	 * @see org.mozilla.javascript.Scriptable#getParentScope()
	 */
	public Scriptable getParentScope()
	{
		return function.getParentScope();
	}

	/**
	 * @param parent
	 * @see org.mozilla.javascript.Scriptable#setParentScope(org.mozilla.javascript.Scriptable)
	 */
	public void setParentScope(Scriptable parent)
	{
		function.setParentScope(parent);
	}

	/**
	 * @return
	 * @see org.mozilla.javascript.Scriptable#getIds()
	 */
	public Object[] getIds()
	{
		return function.getIds();
	}

	/**
	 * @param hint
	 * @return
	 * @see org.mozilla.javascript.Scriptable#getDefaultValue(java.lang.Class)
	 */
	public Object getDefaultValue(Class< ? > hint)
	{
		return function.getDefaultValue(hint);
	}

	/**
	 * @param instance
	 * @return
	 * @see org.mozilla.javascript.Scriptable#hasInstance(org.mozilla.javascript.Scriptable)
	 */
	public boolean hasInstance(Scriptable instance)
	{
		return function.hasInstance(fixStart(instance));
	}


}
