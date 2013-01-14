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
package com.servoy.j2db.dataprocessing;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.ScopesScope;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;

/**
 * Helper class to handle dataprovider with the "scopes." prefix 
 * 
 * @author rgansevles
 */
class ScopesScopeProvider implements IGlobalValueEntry
{
	private final ScopesScope scopesScope;

	public ScopesScopeProvider(ScopesScope scopesScope)
	{
		this.scopesScope = scopesScope;
	}

	public boolean containsDataProvider(String dataProviderID)
	{
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			GlobalScope globalScope = scopesScope.getGlobalScope(scope.getLeft());
			if (globalScope != null)
			{
				return globalScope.has(scope.getRight(), scopesScope);
			}
		}
		return false;
	}

	public Object getDataProviderValue(String dataProviderID)
	{
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			GlobalScope globalScope = scopesScope.getGlobalScope(scope.getLeft());
			if (globalScope != null)
			{
				Scriptable scriptable = globalScope;
				Object value = null;
				String[] datapath = scope.getRight().split("\\."); //$NON-NLS-1$
				for (String provider : datapath)
				{
					value = scriptable.get(provider, scriptable);
					if (value instanceof Scriptable)
					{
						scriptable = (Scriptable)value;
					}
					else break;
				}
				return value;
			}
		}

		return null;
	}

	public Object setDataProviderValue(String dataProviderID, Object value)
	{
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			GlobalScope globalScope = scopesScope.getGlobalScope(scope.getLeft());
			if (globalScope != null)
			{
				return globalScope.put(scope.getRight(), value);
			}
		}

		return null;
	}
}