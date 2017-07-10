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


import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.persistence.ScriptVariable;

/**
 * @author jcompagner
 */
public class SolutionScope extends DefaultScope
{
	private ScopesScope ss;
	private boolean destroyed = false;

	public SolutionScope(Scriptable parent)
	{
		super(parent);
	}

	public ScopesScope getScopesScope()
	{
		return ss;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (destroyed)
		{
			throw new RuntimeException("killing current script, client/solution already terminated");
		}
		// legacy globals.x -> scopes.globals.x
		if (ScriptVariable.GLOBAL_SCOPE.equals(name))
		{
			return ss.get(name, start);
		}
		return super.get(name, start);
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		if (destroyed)
		{
			throw new RuntimeException("killing current script, client/solution already terminated");
		}
		return super.has(name, start);
	}

	public void setScopesScope(ScopesScope ss)
	{
		this.ss = ss;
		put(ScriptVariable.SCOPES, this, ss);
		ss.setParentScope(this);
	}

	@Override
	public void destroy()
	{
		this.destroyed = true;
	}
}
