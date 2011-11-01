/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.j2db.util;

import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * Utility methods for handling global scopes.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public class ScopesUtils
{
	/**
	 * Get the scope of a variable.
	 * <br> globals.x -> (globals, x)
	 * <br> scopes.s.x -> (s, x)
	 * <br> x -> (null, x)
	 */
	public static Pair<String, String> getVariableScope(String id)
	{
		if (id == null) return null;

		String scopeName = null;
		String dpName = id;
		if (id.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
		{
			scopeName = ScriptVariable.GLOBAL_SCOPE;
			dpName = id.substring(ScriptVariable.GLOBALS_DOT_PREFIX.length());
		}
		else if (id.startsWith(ScriptVariable.SCOPES_DOT_PREFIX))
		{
			int dot = id.indexOf('.', ScriptVariable.SCOPES_DOT_PREFIX.length() + 1);
			if (dot >= 0)
			{
				scopeName = id.substring(ScriptVariable.SCOPES_DOT_PREFIX.length(), dot);
				dpName = id.substring(ScriptVariable.SCOPES_DOT_PREFIX.length() + scopeName.length() + 1);
			}
		}

		return new Pair<String, String>(scopeName, dpName);
	}

	/**
	 * Check if the id is scoped.
	 * <br> globals.x -> true
	 * <br> scopes.s.x -> true
	 * <br> x -> false
	 */
	public static boolean isVariableScope(String id)
	{
		return id != null && (id.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX) || id.startsWith(ScriptVariable.SCOPES_DOT_PREFIX));
	}

	/**
	 * Get the string representation for a variable or method plus scope:
	 * <br>(null, x) -> x
	 * <br>(scopename, x) -> scopes.scopename.x
	 */
	public static String getScopeString(Pair<String, String> variableScope)
	{
		if (variableScope == null)
		{
			return null;
		}
		return getScopeString(variableScope.getLeft(), variableScope.getRight());
	}

	/**
	 * Get the string representation for a variable plus scope:
	 * <br>(null, x) -> x
	 * <br>(scopename, x) -> scopes.scopename.x
	 */
	public static String getScopeString(String scopeName, String methodName)
	{
		if (scopeName == null)
		{
			return methodName;
		}
		return new StringBuilder(ScriptVariable.SCOPES_DOT_PREFIX.length() + scopeName.length() + 1 + methodName.length()).append(
			ScriptVariable.SCOPES_DOT_PREFIX).append(scopeName).append('.').append(methodName).toString();
	}

	/**
	 * Get the string representation for a variable/method plus scope:
	 * <br>(null, x) -> x
	 * <br>(scopename, x) -> scopes.scopename.x
	 * 
	 */
	public static String getScopeString(ISupportScope supportScope)
	{
		if (supportScope == null)
		{
			return null;
		}
		return getScopeString(supportScope.getScopeName(), supportScope.getName());
	}
}