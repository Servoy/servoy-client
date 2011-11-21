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


import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.DelegateModificationSubject;
import com.servoy.j2db.dataprocessing.IModificationSubject;
import com.servoy.j2db.persistence.IRootObject;
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

	public void createScopes()
	{
		removeModificationListeners();
		allVars.clear();
		for (Pair<String, IRootObject> scope : application.getFlattenedSolution().getScopes())
		{
			createGlobalScope(scope.getLeft());
		}
	}

	public void createScriptProviders()
	{
		for (Object var : allVars.values())
		{
			if (var instanceof GlobalScope)
			{
				((GlobalScope)var).createScriptProviders();
			}
		}
	}

	public GlobalScope getOrCreateGlobalScope(String sc)
	{
		String scopeName = sc == null ? ScriptVariable.GLOBAL_SCOPE : sc;
		GlobalScope gs = getGlobalScope(scopeName);
		if (gs == null)
		{
			gs = createGlobalScope(scopeName);
		}
		return gs;
	}

	protected GlobalScope createGlobalScope(String scopeName)
	{
		GlobalScope gs = new GlobalScope(this, scopeName, scriptEngine, application);
		allVars.put(scopeName, gs);
		gs.createVars();
		gs.getModificationSubject().addModificationListener(delegateModificationSubject);
		return gs;
	}

	public GlobalScope getGlobalScope(String scopeName)
	{
		return (GlobalScope)allVars.get(scopeName == null ? ScriptVariable.GLOBAL_SCOPE : scopeName);
	}

	public void reloadVariablesAndScripts()
	{
		createScriptProviders();
		createScopes();
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

	/**
	 * @param id
	 * @return
	 */
	public GlobalScope getGlobalScopeForFunction(Integer id)
	{
		for (Object gs : allVars.values())
		{
			if (gs instanceof GlobalScope && ((GlobalScope)gs).getFunctionName(id) != null)
			{
				return (GlobalScope)gs;
			}
		}
		return null;
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

	protected void removeModificationListeners()
	{
		for (Object gs : allVars.values())
		{
			if (gs instanceof GlobalScope)
			{
				((GlobalScope)gs).getModificationSubject().removeModificationListener(delegateModificationSubject);
			}
		}
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
		removeModificationListeners();
		super.destroy();
	}

}
