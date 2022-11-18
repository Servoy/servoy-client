/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import static com.servoy.j2db.scripting.RecordingFunction.wrapFunctionIfNeeded;
import static com.servoy.j2db.scripting.RecordingScriptable.wrapScriptableIfNeeded;
import static com.servoy.j2db.util.Utils.arrayMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolScriptable;
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
abstract class AbstractRecordingScriptable implements Scriptable, IDelegate<Scriptable>, SymbolScriptable
{
	protected final Scriptable scriptable;
	protected final String scriptableName;

	protected AbstractRecordingScriptable(String scriptableName, Scriptable scriptable)
	{
		this.scriptableName = scriptableName;
		this.scriptable = scriptable;
	}

	public Scriptable getDelegate()
	{
		return scriptable;
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
			UsedDataProviderTracker tracker = UsedDataProviderTracker.peek();
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

	static Object[] wrapIfNeeded(Object[] array)
	{
		return arrayMap(array, o -> wrapIfNeeded(null, null, o));
	}

	static Object wrapIfNeeded(String scriptableName, String name, Object o)
	{
		if (o instanceof Scriptable && o != Scriptable.NOT_FOUND
		// eval is a special case, cannot be called directly
			&& !(o instanceof IdFunctionObject && "eval".equals(((IdFunctionObject)o).getFunctionName())) && !"Object".equals(name)) //$NON-NLS-1$ //$NON-NLS-2$
		{
			if (o instanceof Function)
			{
				if ((IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER.equals(scriptableName) || IExecutingEnviroment.TOPLEVEL_UTILS.equals(scriptableName)) &&
					"hasRecords".equals(name)) //$NON-NLS-1$
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
									UsedDataProviderTracker tracker = UsedDataProviderTracker.peek();
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

				return wrapFunctionIfNeeded(name, (Function)o);
			}

			return wrapScriptableIfNeeded(name, (Scriptable)o);
		}
		return o;
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
		// do not wrap toplevel scope. Note that scriptengine puts anything that needs to be tracked in solution scope.
		return parentScope == null ? null : parentScope.getParentScope() == null ? parentScope : wrapScriptableIfNeeded(null, parentScope);
	}

	public Scriptable getPrototype()
	{
		Scriptable prototype = scriptable.getPrototype();
		return prototype == null ? scriptable instanceof ImporterTopLevel ? scriptable : null : wrapScriptableIfNeeded(null, prototype);
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

	@Override
	public Object get(Symbol key, Scriptable start)
	{
		return ensureSymbolScriptable().get(key, getStart(start));
	}

	@Override
	public boolean has(Symbol key, Scriptable start)
	{
		return ensureSymbolScriptable().has(key, getStart(start));
	}

	@Override
	public void put(Symbol key, Scriptable start, Object value)
	{
		ensureSymbolScriptable().put(key, getStart(start), value);
	}

	@Override
	public void delete(Symbol key)
	{
		ensureSymbolScriptable().delete(key);
	}

	protected SymbolScriptable ensureSymbolScriptable()
	{
		if (!(scriptable instanceof SymbolScriptable))
			throw ScriptRuntime.typeErrorById(
				"msg.object.not.symbolscriptable", ScriptRuntime.typeof(scriptable));
		return (SymbolScriptable)scriptable;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!super.equals(obj))
		{
			return scriptable.equals(obj);
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		return scriptable.hashCode();
	}

}
