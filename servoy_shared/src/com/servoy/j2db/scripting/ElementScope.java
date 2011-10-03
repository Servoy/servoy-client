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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.component.ComponentFactory;


/**
 * JavaScript Scope to hold form elements
 * 
 * @author jcompagner
 */
public class ElementScope extends DefaultScope
{
	/**
	 */
	public ElementScope(Scriptable parent)
	{
		super(parent);
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if ("allnames".equals(name))
		{
			Object[] array = getNamesArray();
			return new NativeJavaArray(this, array);
		}
		return super.get(name, start);
	}

	/**
	 * @return
	 */
	private Object[] getNamesArray()
	{
		List list = new ArrayList(allVars.keySet());
		Iterator it = list.iterator();
		while (it.hasNext())
		{
			String key = (String)it.next();
			if (key.startsWith(ComponentFactory.WEB_ID_PREFIX))
			{
				it.remove();
			}
		}

		Object[] array = list.toArray(new String[list.size()]);
		Arrays.sort(array);
		return array;
	}

	/**
	 * @see com.servoy.j2db.scripting.DefaultScope#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		// just return the names for a element scope
		return getNamesArray();
	}
}
