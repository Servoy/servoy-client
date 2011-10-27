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

package com.servoy.j2db.scripting;

import java.util.HashMap;

import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

/**
 * @author lvostinar
 *
 */
public class ServoyNativeJavaObject extends NativeJavaObject implements IScriptableAddition
{
	protected HashMap<String, Object> additionalVars;

	public ServoyNativeJavaObject(Scriptable scope, Object javaObject, JavaMembers members)
	{
		super(scope, javaObject, members);
		additionalVars = new HashMap<String, Object>();
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object superValue = super.get(name, start);
		if ((superValue == null || superValue == Scriptable.NOT_FOUND) && additionalVars.containsKey(name))
		{
			return additionalVars.get(name);
		}
		return superValue;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return super.has(name, start) || additionalVars.containsKey(name);
	}

	public void addVar(String name, Object value)
	{
		additionalVars.put(name, value);
	}

}
