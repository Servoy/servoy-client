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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.WrappedException;

import com.servoy.j2db.Messages;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public abstract class DefaultScope implements Scriptable, IDestroyable
{
	private volatile Scriptable parent;
	private volatile Scriptable prototype;

	protected volatile Map<String, Object> allVars; //name -> object
	protected volatile Map<Integer, Object> allIndex; //index -> object

	protected boolean locked = false;

	public DefaultScope(Scriptable parent)
	{
		this(parent, HashMap::new);
	}

	protected DefaultScope(Scriptable parent, Supplier<Map<String, Object>> allVarsSupplier)
	{
		this.parent = parent;
		this.allVars = allVarsSupplier.get();
		this.allIndex = new HashMap<>();
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
	public String getClassName()
	{
		return getClass().getSimpleName();
	}

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

		if (o == null && !has(name, start)) return Scriptable.NOT_FOUND;

		if (o != null && o != Scriptable.NOT_FOUND && !(o instanceof Scriptable || o instanceof Callable))
		{
			Context context = Context.getCurrentContext();
			if (context != null) o = context.getWrapFactory().wrap(context, start, o, o.getClass());
		}

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
		if (name != null && name.equals("length")) return true; //$NON-NLS-1$
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
		if (locked) throw new WrappedException(
			new RuntimeException(Messages.getString("servoy.javascript.error.lockedForIndex", new Object[] { new Integer(index), value }))); //$NON-NLS-1$
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
		if (locked) throw new WrappedException(
			new RuntimeException(Messages.getString("servoy.javascript.error.lockedForDeleteIndex", new Object[] { new Integer(index) }))); //$NON-NLS-1$
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

		for (String string : allVars.keySet())
		{
			array[counter++] = string;
		}
		for (Integer integer : allIndex.keySet())
		{
			array[counter++] = integer;
		}
		return array;
	}

	public Object[] getValues()
	{
		return allVars.values().toArray();
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getDefaultValue(java.lang.Class)
	 */
	public Object getDefaultValue(Class< ? > typeHint)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getClassName());
		sb.append('[');

		Object[] objects = getIds();
		for (Object element : objects)
		{
			sb.append(element);
			sb.append(","); //$NON-NLS-1$
		}
		if (objects.length > 0) sb.setCharAt(sb.length() - 1, ']');
		else sb.append(']');
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
		List<IDestroyable> indexDestroybles = this.allIndex.values().stream().filter(object -> object instanceof IDestroyable)
			.map(object -> (IDestroyable)object).collect(Collectors.toList());

		List<IDestroyable> varDestroyables = this.allVars.values().stream().filter(object -> object instanceof IDestroyable).map(object -> (IDestroyable)object)
			.collect(Collectors.toList());

		this.allIndex.clear();
		this.allVars.clear();
		this.parent = null;
		this.prototype = null;

		destroyChildren(indexDestroybles, varDestroyables);
	}

	/**
	 * @param indexDestroybles
	 * @param varDestroyables
	 */
	protected void destroyChildren(List<IDestroyable> indexDestroybles, List<IDestroyable> varDestroyables)
	{
		indexDestroybles.forEach(destroyable -> destroyable.destroy());
		varDestroyables.forEach(destroyable -> destroyable.destroy());
	}

	/**
	 * This method in {@link DefaultScope} only removes the value for the given key (it does not touch the index access).<br/>
	 * But extending classes should add to it any specific logic. For example if a child class will always keep values referenced via String keys also in the index based access,
	 * it might want to clear that value from the index map too or whatever internal specific member to that class it keeps that value in as well...
	 *
	 * @param key the key of the value to be removed.
	 * @return the value that was removed, if any.
	 */
	public Object remove(String key)
	{
		return allVars.remove(key);
	}

	/**
	 * Removes a value from the index mapping.
	 * Will make sure that the indexes that followed the deleted one will slide left with 1 position.
	 *
	 * @param o the value to be removed
	 * @return the index at which the value was found - in case it was removed or -1 if that value was not found.
	 */
	public int removeIndexByValue(Object o)
	{
		Integer found = null;
		for (Entry<Integer, Object> entry : allIndex.entrySet())
		{
			if (entry.getValue().equals(o))
			{
				Integer key = entry.getKey();
				found = key;
				int oldLength = allIndex.size();
				allIndex.remove(key);
				Integer nextKey = new Integer(key.intValue() + 1);
				while (nextKey.intValue() <= oldLength)
				{
					Object tmp = allIndex.remove(nextKey);
					if (tmp != null) allIndex.put(key, tmp);
					else allIndex.remove(key);

					key = nextKey;
					nextKey = new Integer(key.intValue() + 1);
				}
				break;
			}
		}
		if (found == null)
		{
			found = Utils.mapRemoveByValue(o, allIndex);
		}

		return found != null ? found.intValue() : -1;
	}

	/**
	 * Create a new JavaScript object.
	 *
	 * Equivalent to evaluating "new Object()".
	 *
	 * @param scope
	 *            the scope to search for the constructor and to evaluate
	 *            against
	 * @return the new object
	 */
	public static Scriptable newObject(Scriptable scope)
	{
		NativeObject result = new NativeObject();
		ScriptRuntime.setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.Object);
		return result;
	}

	/**
	 * Create an array with a specified initial length.
	 * <p>
	 *
	 * @param scope
	 *            the scope to create the object in
	 * @param length
	 *            the initial length (JavaScript arrays may have additional
	 *            properties added dynamically).
	 * @return the new array object
	 */
	public static Scriptable newArray(Scriptable scope, int length)
	{
		NativeArray result = new NativeArray(length);
		ScriptRuntime.setBuiltinProtoAndParent(result, scope, TopLevel.Builtins.Array);
		return result;
	}

}
