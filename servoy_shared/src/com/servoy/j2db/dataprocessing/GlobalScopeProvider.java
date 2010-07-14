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

import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.GlobalScope;

/**
 * Helper class to handle dataprovider with the "globals." prefix
 * 
 * @author rgansevles
 */
class GlobalScopeProvider implements IGlobalValueEntry
{
	private final GlobalScope globalScope;

	public GlobalScopeProvider(GlobalScope globalScope)
	{
		this.globalScope = globalScope;
	}

	public boolean containsDataProvider(String dataProviderID)
	{
		if (dataProviderID.length() > ScriptVariable.GLOBAL_DOT_PREFIX.length() && dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String global = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
			return globalScope.has(global, globalScope);
		}
		return false;
	}

	public Object getDataProviderValue(String dataProviderID)
	{
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String global = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
			if (globalScope.has(global, globalScope))
			{
				return globalScope.get(global);
			}
		}
		return null;
	}

	public Object setDataProviderValue(String dataProviderID, Object value)
	{
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			String global = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
			if (globalScope.has(global, globalScope))
			{
				return globalScope.put(global, value);
			}
		}
		return null;
	}
}