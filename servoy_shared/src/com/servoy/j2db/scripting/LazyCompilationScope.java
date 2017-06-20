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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.dltk.rhino.dbgp.LazyInitScope;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.Debugger;
import org.mozilla.javascript.debug.IDebuggerWithWatchPoints;

import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 */
public abstract class LazyCompilationScope extends DefaultScope implements LazyInitScope
{
	protected volatile IExecutingEnviroment scriptEngine;
	private final Map<Integer, String> idVars; //id -> name (not in the same so it will not be in runtime env)
	private volatile Scriptable functionParent;//default this
	private volatile ISupportScriptProviders scriptLookup;

	public LazyCompilationScope(Scriptable parent, IExecutingEnviroment scriptEngine, ISupportScriptProviders scriptLookup)
	{
		super(parent);
		this.scriptLookup = scriptLookup;
		this.scriptEngine = scriptEngine;
		idVars = new HashMap<Integer, String>();
		functionParent = this;
		createScriptProviders(true);
	}

	// final because called from constructor
	protected final void createScriptProviders(boolean overwriteInitialValue)
	{
		Iterator< ? extends IScriptProvider> it = scriptLookup.getScriptMethods(false);
		while (it.hasNext())
		{
			IScriptProvider sm = it.next();
			put(sm, sm, overwriteInitialValue);
		}
	}

	public ISupportScriptProviders getScriptLookup()
	{
		return scriptLookup;
	}

	public void setFunctionParentScriptable(Scriptable functionParent)
	{
		this.functionParent = functionParent;
	}

	public Scriptable getFunctionParentScriptable()
	{
		return functionParent;
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return idVars.containsKey(Integer.valueOf(index));
	}

	public void put(IScriptProvider sm, Object function)
	{
		put(sm, function, true);
	}

	/**
	 * @return true if the value was changed/put and false otherwise (depending on the value of 'overwriteInitialValue' and the presence of sm)
	 */
	public boolean put(IScriptProvider sm, Object function, boolean overwriteInitialValue)
	{
		if (!overwriteInitialValue && allVars.containsKey(sm.getDataProviderID()))
		{
			return false;
		}

		remove(sm.getName());

		allVars.put(sm.getDataProviderID(), function);
		idVars.put(new Integer(sm.getID()), sm.getDataProviderID());

		return true;
	}

	public Object remove(IScriptProvider sm)
	{
		String sName = idVars.remove(new Integer(sm.getID()));
		if (sName != null)
		{
			Object o = allVars.remove(sName);
			// Script method may be either  a interpreted rhino Function  or a (yet to be interpreted ScriptMethod)
			return (o instanceof Function || o instanceof ScriptMethod) ? o : null;
		}
		return null;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object o = getImpl(name, start);
		if (o != Scriptable.NOT_FOUND)
		{
			Context currentContext = Context.getCurrentContext();
			if (currentContext != null)
			{
				Debugger debugger = currentContext.getDebugger();
				if (debugger instanceof IDebuggerWithWatchPoints)
				{
					IDebuggerWithWatchPoints wp = (IDebuggerWithWatchPoints)debugger;
					wp.access(name, this);
				}
			}
		}
		return o;
	}

	protected final Object getImpl(String name, Scriptable start)
	{
		IScriptProvider sp = getScriptProvider(name);
		if (sp != null)
		{
			try
			{
				Scriptable compileScope = functionParent;
				Scriptable functionSuper = getFunctionSuper(sp);
				if (functionSuper != null)
				{
					compileScope = new DefaultScope(functionParent)
					{
						@Override
						public String getClassName()
						{
							return "RunScope"; //$NON-NLS-1$
						}
					};
					compileScope.setPrototype(functionParent);
					// _formname_ is used in a lot of plugins (getParent of a function object, see FunctionDef) to get the form name
					compileScope.put("_formname_", compileScope, functionParent.get("_formname_", functionParent)); //$NON-NLS-1$ //$NON-NLS-2$
					compileScope.put("_super", compileScope, functionSuper); //$NON-NLS-1$
				}
				Function function = scriptEngine.compileFunction(sp, compileScope);
				put(name, start, function);//replace to prevent more compiles
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return super.get(name, start);
	}

	private IScriptProvider getScriptProvider(String name)
	{
		Object o = allVars.get(name);
		if (o instanceof IScriptProvider) return (IScriptProvider)o;
		return null;
	}

	protected Scriptable getFunctionSuper(IScriptProvider sp)
	{
		return null;
	}

	/**
	 * @see org.eclipse.dltk.rhino.dbgp.LazyInitScope#getInitializedIds()
	 */
	public Object[] getInitializedIds()
	{
		List<Object> array = new ArrayList<Object>(allVars.size() + allIndex.size() + 2);
		array.add("allnames"); //$NON-NLS-1$
		array.add("length"); //$NON-NLS-1$

		for (Object element : allVars.keySet())
		{
			String name = (String)element;
			Object o = super.get(name, this);
			if (!(o instanceof IScriptProvider))
			{
				array.add(name);
			}
		}
//		for (Iterator iter = allIndex.keySet().iterator(); iter.hasNext();)
//		{
//			array.add(iter.next());
//		}
		return array.toArray();
	}

	public String getFunctionName(Integer id)
	{
		return idVars.get(id);
	}

	public Function getFunctionByName(String name)
	{
		if (name == null) return null;
		Object o = getImpl(name, this);
		return (o instanceof Function ? (Function)o : null);
	}

	public void reload()
	{
		Iterator<Object> it = allVars.values().iterator();
		while (it.hasNext())
		{
			Object object = it.next();
			if (object instanceof IScriptProvider || object instanceof Function)
			{
				it.remove();
			}
		}

		idVars.clear();
		createScriptProviders(true);
	}

	public abstract String getScopeName();

	@Override
	public String toString()
	{
		return "LazyCompilationScope[parent:" + (getParentScope() == this ? "this" : getParentScope()) + ", scriptLookup:" + scriptLookup + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public Object remove(String name)
	{
		Utils.mapRemoveByValue(name, idVars);
		return super.remove(name);
	}

	@Override
	public void destroy()
	{
		this.scriptEngine = null;
		this.idVars.clear();
		this.functionParent = null;
		this.scriptLookup = null;
		super.destroy();
	}

}
