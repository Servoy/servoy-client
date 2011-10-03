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

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public class GlobalScope extends ScriptVariableScope
{
	private volatile IServiceProvider application;

	public GlobalScope(Scriptable parent, IExecutingEnviroment scriptEngine, IServiceProvider application)
	{
		super(parent, scriptEngine, application.getFlattenedSolution());
		this.application = application;
	}

	public void createVars()
	{
		//put all vars in scope
		Iterator<ScriptVariable> it = this.application.getFlattenedSolution().getScriptVariables(false);
		while (it.hasNext())
		{
			ScriptVariable var = it.next();
			put(var);
		}
	}

	public void reloadVariablesAndScripts()
	{
		createVars();
		createScriptProviders();
	}

	/*
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (application == null || application.getSolution() == null)
		{
			if (Debug.tracing())
			{
				Debug.trace("Trying to get a global scope property on an already closed solution", new RuntimeException());
			}
			return Scriptable.NOT_FOUND;
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
		else if ("allmethods".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();

			try
			{
				Iterator<ScriptMethod> iterator = application.getFlattenedSolution().getScriptMethods(true);
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
		else if ("allvariables".equals(name)) //$NON-NLS-1$
		{
			List<String> al = new ArrayList<String>();

			try
			{
				Iterator<ScriptVariable> iterator = application.getFlattenedSolution().getScriptVariables(true);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.scripting.ScriptVariableScope#getDataproviderEventName(java.lang.String)
	 */
	@Override
	protected String getDataproviderEventName(String name)
	{
		return ScriptVariable.GLOBAL_DOT_PREFIX + name; // TODO Auto-generated method stub
	}

	@Override
	public void destroy()
	{
		this.application = null;
		super.destroy();
	}
}
