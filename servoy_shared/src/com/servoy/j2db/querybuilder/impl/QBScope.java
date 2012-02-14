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

package com.servoy.j2db.querybuilder.impl;

import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author rgansevles
 * 
 * @since 6.1
 *
 */
public class QBScope extends DefaultScope
{
	private final Map<String, NativeJavaMethod> jsFunctions;

	QBScope(Scriptable scriptParent, Map<String, NativeJavaMethod> jsFunctions)
	{
		super(scriptParent);
		this.jsFunctions = jsFunctions;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		NativeJavaMethod jm = jsFunctions.get(name);
		if (jm != null)
		{
			return jm;
		}

		return super.get(name, start);
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return jsFunctions.containsKey(name) || super.has(name, start);
	}
}
