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


import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

import com.servoy.j2db.Messages;

/**
 * @author jcompagner
 */
public abstract class DefaultScope implements Scriptable
{
	private volatile Scriptable parent;
	private volatile Scriptable prototype;

	protected volatile HashMap<String, Object> allVars; //name -> object
	protected volatile HashMap<Integer, Object> allIndex; //index -> object

	protected boolean locked = false;

	public DefaultScope(Scriptable parent)
	{
		this.parent = parent;
		allVars = new HashMap<String, Object>();
		allIndex = new HashMap<Integer, Object>();
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
	public abstract String getClassName();

	/**
	 * @see org.mozilla.javascript.Scriptable#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public Object get(String name, Scriptable start)
	{
		if ("length".equals(name)) //$NON-NLS-1$
		{
			return new Integer(allIndex.size());
		}
		else if ("allnames".equals(name)) //$NON-NLS-1$
		{
			Context.enter();
			try
			{
				Object[] array = allVars.keySet().toArray(new String[allVars.size()]);
				Arrays.sort(array);
				return new NativeJavaArray(this, array);
			}
			finally
			{
				Context.exit();
			}
		}

		Object o = allVars.get(name);
		if (o instanceof Date)
		{
			o = new Date(((Date)o).getTime());//make copy so changes are seen (date is mutable and whould bypass equals)
		}
		else if (o != null && o.getClass().isArray() && !o.getClass().getComponentType().isPrimitive())
		{
			o = new NativeJavaArray(this, o);
		}
		if (o == null && !has(name, start)) return Scriptable.NOT_FOUND;
		return o;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#get(int, org.mozilla.javascript.Scriptable)
	 */
	public Object get(int index, Scriptable start)
	{
		Object o = allIndex.get(new Integer(index));
		if (o == null && !has(index, start)) return Scriptable.NOT_FOUND;
		return o;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#has(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(String name, Scriptable start)
	{
		if (name.equals("length")) return true; //$NON-NLS-1$
		return allVars.containsKey(name);
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#has(int, org.mozilla.javascript.Scriptable)
	 */
	public boolean has(int index, Scriptable start)
	{
		return allIndex.containsKey(new Integer(index));
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(String name, Scriptable start, Object value)
	{
		if (locked) throw new WrappedException(new RuntimeException(Messages.getString("servoy.javascript.error.lockedForName", new Object[] { name, value }))); //$NON-NLS-1$
		allVars.put(name, value);
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#put(int, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	public void put(int index, Scriptable start, Object value)
	{
		if (locked) throw new WrappedException(new RuntimeException(Messages.getString(
			"servoy.javascript.error.lockedForIndex", new Object[] { new Integer(index), value }))); //$NON-NLS-1$
		allIndex.put(new Integer(index), value);
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#delete(java.lang.String)
	 */
	public void delete(String name)
	{
		if (locked) throw new WrappedException(new RuntimeException(Messages.getString("servoy.javascript.error.lockedForDeleteName", new Object[] { name }))); //$NON-NLS-1$
		allVars.remove(name);
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#delete(int)
	 */
	public void delete(int index)
	{
		if (locked) throw new WrappedException(new RuntimeException(Messages.getString(
			"servoy.javascript.error.lockedForDeleteIndex", new Object[] { new Integer(index) }))); //$NON-NLS-1$
		allIndex.remove(new Integer(index));
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getPrototype()
	 */
	public Scriptable getPrototype()
	{
		return prototype;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#setPrototype(org.mozilla.javascript.Scriptable)
	 */
	public void setPrototype(Scriptable prototype)
	{
		this.prototype = prototype;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getParentScope()
	 */
	public Scriptable getParentScope()
	{
		return parent;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#setParentScope(org.mozilla.javascript.Scriptable)
	 */
	public void setParentScope(Scriptable parent)
	{
		this.parent = parent;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getIds()
	 */
	public Object[] getIds()
	{
		Object[] array = new Object[allVars.size() + allIndex.size() + 2];
		int counter = 0;
		array[counter++] = "allnames"; //$NON-NLS-1$
		array[counter++] = "length"; //$NON-NLS-1$

		for (Iterator<String> iter = allVars.keySet().iterator(); iter.hasNext();)
		{
			array[counter++] = iter.next();
		}
		for (Iterator<Integer> iter = allIndex.keySet().iterator(); iter.hasNext();)
		{
			array[counter++] = iter.next();
		}
		return array;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getDefaultValue(java.lang.Class)
	 */
	public Object getDefaultValue(Class typeHint)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getClassName());
		sb.append("["); //$NON-NLS-1$

		Object[] objects = getIds();
		for (Object element : objects)
		{
			sb.append(element);
			sb.append(","); //$NON-NLS-1$
		}
		if (objects.length > 0) sb.setCharAt(sb.length() - 1, ']');
		else sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#hasInstance(org.mozilla.javascript.Scriptable)
	 */
	public boolean hasInstance(Scriptable instance)
	{
		Scriptable proto = instance.getPrototype();

		while (proto != null)
		{
			if (proto.equals(this)) return true;
			proto = proto.getPrototype();
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean isLocked()
	{
		return locked;
	}

	/**
	 * @param b
	 */
	public void setLocked(boolean b)
	{
		locked = b;
	}

	public void destroy()
	{
		this.allIndex.clear();
		this.allVars.clear();
		this.parent = null;
		this.prototype = null;
	}

}
