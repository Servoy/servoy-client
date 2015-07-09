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


import java.util.Stack;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.DelegateModificationSubject;
import com.servoy.j2db.dataprocessing.IModificationSubject;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Scope that holds the global scopes.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public class ScopesScope extends DefaultScope
{
	private volatile IServiceProvider application;
	private final IExecutingEnviroment scriptEngine;
	private final DelegateModificationSubject delegateModificationSubject = new DelegateModificationSubject();

	public ScopesScope(Scriptable parent, IExecutingEnviroment scriptEngine, IServiceProvider application)
	{
		super(parent);
		this.scriptEngine = scriptEngine;
		this.application = application;
		setLocked(true);
	}

	public IModificationSubject getModificationSubject()
	{
		return delegateModificationSubject;
	}

	public void createGlobalsScope()
	{
		getGlobalScope(ScriptVariable.GLOBAL_SCOPE);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		Object object = super.get(name, start);
		if (object == Scriptable.NOT_FOUND && application.getFlattenedSolution().getScopeNames().contains(name))
		{
			object = getGlobalScope(name);
		}
		else if (object instanceof GlobalScope && !((GlobalScope)object).isInitialized())
		{
			application.reportJSError("Scope '" + name +
				"' was accessed while not fully created yet, check for scope variables recursively referring to each other, scope stack:" +
				globalScopeCreateStack.toString(), null);
		}
		return object;
	}

	private final Stack<String> globalScopeCreateStack = new Stack<String>();

	/**
	 * Get or create global scope.
	 * @param sc
	 * @return
	 */
	public GlobalScope getGlobalScope(String sc)
	{
		String scopeName = sc == null ? ScriptVariable.GLOBAL_SCOPE : sc;
		GlobalScope gs;
		Object gsObj = allVars.get(scopeName);
		if (gsObj instanceof GlobalScope)
		{
			gs = (GlobalScope)gsObj;
		}
		else
		{
			gs = new GlobalScope(getParentScope(), scopeName, scriptEngine, application)
			{
				@Override
				protected void putScriptVariable(String name, ScriptVariable var, boolean overwriteInitialValue)
				{
					globalScopeCreateStack.push("var:" + name);
					try
					{
						super.putScriptVariable(name, var, overwriteInitialValue);
					}
					finally
					{
						globalScopeCreateStack.pop();
					}
				}
			};
			allVars.put(scopeName, gs);
			globalScopeCreateStack.push(scopeName);
			try
			{
				gs.createVars();
			}
			finally
			{
				globalScopeCreateStack.pop();
			}
			gs.getModificationSubject().addModificationListener(delegateModificationSubject);
		}
		return gs;
	}

	public void reloadVariablesAndScripts()
	{
		for (Object var : allVars.values().toArray())
		{
			if (var instanceof GlobalScope)
			{
				((GlobalScope)var).createScriptProviders(false);
				globalScopeCreateStack.push(((GlobalScope)var).getScopeName());
				try
				{
					((GlobalScope)var).createVars();
				}
				finally
				{
					globalScopeCreateStack.pop();
				}
			}
		}
	}

	/**
	 * @param dataProviderID
	 * @return
	 */
	public Object get(String sc, String dataProviderID)
	{
		String scopeName;
		String baseName;
		if (sc == null)
		{
			Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
			scopeName = scope.getLeft();
			baseName = scope.getRight();
		}
		else
		{
			scopeName = sc;
			baseName = ScopesUtils.getVariableScope(dataProviderID).getRight();
		}

		GlobalScope gs = getGlobalScope(scopeName);
		if (gs == null)
		{
			return null;
		}
		return gs.get(baseName);
	}

	public Object executeGlobalFunction(String scopeName, String methodName, Object[] args, boolean focusEvent, boolean throwException) throws Exception
	{
		GlobalScope gs = getGlobalScope(scopeName);
		if (gs == null)
		{
			return null;
		}

		Object function = gs.get(methodName);
		if (function instanceof Function)
		{
			return scriptEngine.executeFunction((Function)function, gs, gs, args, focusEvent, throwException);
		}
		return null;
	}

	@Override
	public Object[] getIds()
	{
		// just return the names of the global scopes
		return allVars.keySet().toArray();
	}

	@Override
	public void destroy()
	{
		application = null;
		for (Object gs : allVars.values())
		{
			if (gs instanceof GlobalScope)
			{
				((GlobalScope)gs).getModificationSubject().removeModificationListener(delegateModificationSubject);
				((GlobalScope)gs).destroy();
			}
		}
		super.destroy();
	}

	@Override
	public String toString()
	{
		return "ScopesScope[" + allVars.keySet() + ']';
	}
}
