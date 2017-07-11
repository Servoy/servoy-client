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
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ExitScriptException;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class GlobalScope extends ScriptVariableScope
{
	private volatile IServiceProvider application;
	private final String scopeName;

	public GlobalScope(Scriptable parent, final String scopeName, IExecutingEnviroment scriptEngine, final IServiceProvider application)
	{
		super(parent, scriptEngine, new ISupportScriptProviders()
		{
			public Iterator< ? extends IScriptProvider> getScriptMethods(boolean sort)
			{
				return application.getFlattenedSolution().getScriptMethods(scopeName, sort);
			}

			public Iterator<ScriptVariable> getScriptVariables(boolean sort)
			{
				return application.getFlattenedSolution().getScriptVariables(scopeName, sort);
			}

			public ScriptMethod getScriptMethod(int methodId)
			{
				return null; // is not used in lazy compilation scope
			}
		});
		this.scopeName = scopeName;
		this.application = application;
	}

	@Override
	public String getScopeName()
	{
		return scopeName;
	}

	boolean initialized = false;

	/**
	 * @return the initialized
	 */
	public boolean isInitialized()
	{
		return initialized;
	}

	public void createVars()
	{
		initialized = false;
		//put all vars in scope
		Iterator<ScriptVariable> it = getScriptLookup().getScriptVariables(false);
		while (it.hasNext())
		{
			put(it.next());
		}
		initialized = true;
	}

	@Override
	public void put(ScriptVariable var, boolean overwriteInitialValue)
	{
		Pair<String, String> scope = ScopesUtils.getVariableScope(var.getDataProviderID());
		if (scope.getLeft() != null && !scope.getLeft().equals(scopeName))
		{
			// this global does not belong here!
			throw new RuntimeException("ScriptVariable was set in global scope '" + scopeName + "'");
		}
		putScriptVariable(scope.getRight(), var, overwriteInitialValue);
	}

	/*
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (application == null)
		{
			if (Debug.tracing())
			{
				Debug.trace("Trying to get a global scope property on an already closed solution", new RuntimeException());
			}
			throw new ExitScriptException("killing current script, client/solution already terminated");
		}

		if ("allrelations".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();
			try
			{
				Iterator<Relation> it = this.application.getFlattenedSolution().getRelations(null, true, true); // returns only global relations
				while (it.hasNext())
				{
					al.add(it.next().getName());
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		if ("allmethods".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();

			try
			{
				Iterator< ? extends IScriptProvider> iterator = getScriptLookup().getScriptMethods(true);
				while (iterator.hasNext())
				{
					al.add(iterator.next().getName());
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		if ("allvariables".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();

			try
			{
				Iterator<ScriptVariable> iterator = getScriptLookup().getScriptVariables(true);
				while (iterator.hasNext())
				{
					al.add(iterator.next().getName());
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			return new NativeJavaArray(this, al.toArray(new String[al.size()]));
		}

		Object o = super.get(name, start);
		if (o == Scriptable.NOT_FOUND)
		{
			try
			{
				o = application.getFoundSetManager().getGlobalRelatedFoundSet(name);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			if (o == null) o = Scriptable.NOT_FOUND;
		}
		return o;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		if (application == null)
		{
			if (Debug.tracing())
			{
				Debug.trace("Trying to get a global scope property on an already closed solution", new RuntimeException());
			}
			throw new ExitScriptException("killing current script, client/solution already terminated");
		}
		return super.has(name, start);
	}

	@Override
	protected String getDataproviderEventName(String name)
	{
		return ScopesUtils.getScopeString(scopeName, name);
	}

	@Override
	public void destroy()
	{
		this.application = null;
		super.destroy();
	}

	@Override
	public String toString()
	{
		return "GlobalScope[name:" + scopeName + ", values:" + allVars + ']';
	}
}
