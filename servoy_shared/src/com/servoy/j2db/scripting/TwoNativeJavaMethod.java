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


import java.awt.Component;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * @author jcompagner
 * 
 */
public class TwoNativeJavaMethod implements Function
{
	protected Function function;
	protected Scriptable secondObject;
	protected Component listView;

	/**
	 * @param javaObject2
	 */
	public TwoNativeJavaMethod(NativeJavaObject javaObject, Function function, Component listView)
	{
		this.secondObject = javaObject;
		this.function = function;
		this.listView = listView;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Function#call(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, org.mozilla.javascript.Scriptable,
	 * java.lang.Object[])
	 */
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) throws JavaScriptException
	{
		Object object = null;
		try
		{
			object = function.call(cx, scope, thisObj, args);
			// dont call repaint for just getters..
			if (listView != null && ((args != null && args.length > 0) || (object == null || object == Undefined.instance))) listView.repaint();
		}
		finally
		{
			function.call(cx, scope, secondObject, args);
		}
		return object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Function#construct(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) throws JavaScriptException
	{
		return function.construct(cx, scope, args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
	public String getClassName()
	{
		return function.getClassName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public Object get(String name, Scriptable start)
	{
		return function.get(name, start);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#get(int, org.mozilla.javascript.Scriptable)
	 */
	public Object get(int index, Scriptable start)
	{
		return function.get(index, start);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#has(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(String name, Scriptable start)
	{
		return function.has(name, start);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#has(int, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(int index, Scriptable start)
	{
		return function.has(index, start);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(String name, Scriptable start, Object value)
	{
		function.put(name, start, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#put(int, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(int index, Scriptable start, Object value)
	{
		function.put(index, start, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#delete(java.lang.String)
	 */
	public void delete(String name)
	{
		function.delete(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#delete(int)
	 */
	public void delete(int index)
	{
		function.delete(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#getPrototype()
	 */
	public Scriptable getPrototype()
	{
		return function.getPrototype();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#setPrototype(org.mozilla.javascript.Scriptable)
	 */
	public void setPrototype(Scriptable prototype)
	{
		function.setPrototype(prototype);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#getParentScope()
	 */
	public Scriptable getParentScope()
	{
		return function.getParentScope();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#setParentScope(org.mozilla.javascript.Scriptable)
	 */
	public void setParentScope(Scriptable parent)
	{
		function.setParentScope(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#getIds()
	 */
	public Object[] getIds()
	{
		return function.getIds();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#getDefaultValue(java.lang.Class)
	 */
	public Object getDefaultValue(Class hint)
	{
		return function.getDefaultValue(hint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.Scriptable#hasInstance(org.mozilla.javascript.Scriptable)
	 */
	public boolean hasInstance(Scriptable instance)
	{
		return function.hasInstance(instance);
	}

}
