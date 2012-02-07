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

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Scriptable for recording access to delegate scriptable. Used to determine dependencies for calculations.
 * 
 * @author rgansevles
 * 
 */
public class RecordingScriptable implements Scriptable, IDelegate<Scriptable>, Wrapper
{
	private final static ThreadLocal<List<UsedDataProviderTracker>> recordedThreadLocal = new ThreadLocal<List<UsedDataProviderTracker>>();

	protected final Scriptable scriptable;
	protected final String scriptableName;

	public RecordingScriptable(String scriptableName, Scriptable scriptable)
	{
		this.scriptableName = scriptableName;
		this.scriptable = scriptable;
	}

	public void pushRecordingTracker(UsedDataProviderTracker usedDataProviderTracker)
	{
		List<UsedDataProviderTracker> stack = recordedThreadLocal.get();
		if (stack == null)
		{
			stack = new ArrayList<UsedDataProviderTracker>();
			recordedThreadLocal.set(stack);
		}
		stack.add(usedDataProviderTracker);
	}

	public UsedDataProviderTracker popRecordingTracker()
	{
		List<UsedDataProviderTracker> stack = recordedThreadLocal.get();
		if (stack == null || stack.size() <= 0)
		{
			// should never happen
			throw new IllegalStateException("Cannot pop calculation recording tracker"); //$NON-NLS-1$
		}
		return stack.remove(stack.size() - 1);
	}

	public UsedDataProviderTracker peekRecordingTracker()
	{
		List<UsedDataProviderTracker> stack = recordedThreadLocal.get();
		if (stack != null && stack.size() > 0)
		{
			return stack.get(stack.size() - 1);
		}
		return null;
	}

	public Scriptable getDelegate()
	{
		return scriptable;
	}

	public Object unwrap()
	{
		return scriptable instanceof Wrapper ? ((Wrapper)scriptable).unwrap() : scriptable;
	}

	public void delete(int index)
	{
		scriptable.delete(index);
	}

	public void delete(String name)
	{
		scriptable.delete(name);
	}

	public Object get(int index, Scriptable start)
	{
		return scriptable.get(index, getStart(start));
	}

	public Object get(String name, Scriptable start)
	{
		Object o = scriptable.get(name, getStart(start));
		if (o != Scriptable.NOT_FOUND)
		{
			UsedDataProviderTracker tracker = peekRecordingTracker();
			if (tracker != null)
			{
				if (scriptable instanceof GlobalScope)
				{
					tracker.usedGlobal(ScopesUtils.getScopeString(((GlobalScope)scriptable).getScopeName(), name));
				}
				else
				{
					tracker.usedName(scriptable, name);
				}

				return wrapIfNeeded(scriptableName, name, o);
			}
		}
		return o;
	}

	static Object wrapIfNeeded(String scriptableName, String name, Object o)
	{
		if (o instanceof Scriptable && o != Scriptable.NOT_FOUND
		// eval is a special case, cannot be called directly
			&& !(o instanceof IdFunctionObject && "eval".equals(((IdFunctionObject)o).getFunctionName())) && !"Object".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			if (o instanceof Function)
			{
				if (("databaseManager".equals(scriptableName) || "utils".equals(scriptableName)) && "hasRecords".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{
					// special case, databaseManager.hasRecords(record, relationName) checks existence of related foundsets 
					return new RecordingFunction(name, (Function)o)
					{
						@Override
						public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
						{
							if (args != null && args.length > 1 && args[1] instanceof String)
							{
								Object arg0 = args[0];
								if (arg0 instanceof Wrapper)
								{
									arg0 = ((Wrapper)arg0).unwrap();
								}
								if (arg0 instanceof IRecordInternal)
								{
									UsedDataProviderTracker tracker = peekRecordingTracker();
									if (tracker != null)
									{
										tracker.usedFromRecord((IRecordInternal)arg0, (String)args[1]);
									}
								}
							}
							return super.call(cx, scope, thisObj, args);
						}
					};
				}
				return new RecordingFunction(name, (Function)o);
			}

			return new RecordingScriptable(name, (Scriptable)o);
		}
		return o;
	}

	public static Object unwrapScriptable(Object obj)
	{
		if (obj instanceof IDelegate< ? >)
		{
			Object delegate = ((IDelegate< ? >)obj).getDelegate();
			if (delegate instanceof Scriptable)
			{
				return delegate;
			}
		}
		return obj;
	}

	public static Object[] unwrapScriptable(Object[] array)
	{
		if (array == null)
		{
			return null;
		}
		Object[] retval = new Object[array.length];
		for (int i = 0; i < array.length; i++)
		{
			retval[i] = unwrapScriptable(array[i]);
		}
		return retval;
	}

	public String getClassName()
	{
		return scriptable.getClassName();
	}

	public Object getDefaultValue(Class< ? > hint)
	{
		return scriptable.getDefaultValue(hint);
	}

	public Object[] getIds()
	{
		return scriptable.getIds();
	}

	public Scriptable getParentScope()
	{
		Scriptable parentScope = scriptable.getParentScope();
		return parentScope == null ? null : new RecordingScriptable(null, parentScope);
	}

	public Scriptable getPrototype()
	{
		Scriptable prototype = scriptable.getPrototype();
		return prototype == null ? scriptable instanceof ImporterTopLevel ? scriptable : null : new RecordingScriptable(null, prototype);
	}

	/**
	 * When start is yourself use scriptable as start for put/get
	 */
	protected Scriptable getStart(Scriptable start)
	{
		if (start == this)
		{
			return scriptable;
		}
		return start;
	}

	public boolean has(int index, Scriptable start)
	{
		return scriptable.has(index, getStart(start));
	}

	public boolean has(String name, Scriptable start)
	{
		return scriptable.has(name, getStart(start));
	}

	public boolean hasInstance(Scriptable instance)
	{
		return scriptable.hasInstance(instance);
	}

	public void put(int index, Scriptable start, Object value)
	{
		scriptable.put(index, getStart(start), value);
	}

	public void put(String name, Scriptable start, Object value)
	{
		scriptable.put(name, getStart(start), value);
	}

	public void setParentScope(Scriptable parent)
	{
		scriptable.setParentScope(parent);
	}

	public void setPrototype(Scriptable prototype)
	{
		scriptable.setPrototype(prototype);
	}
}
