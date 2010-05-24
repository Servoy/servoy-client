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
import java.util.List;

import org.mozilla.javascript.Scriptable;

/**
 * Script object for a group of scriptables, delegates a fixed list of properties to all the enclosed scriptables.
 * 
 * @author rob
 * 
 * @since 5.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class GroupScriptObject
{
	private final List<Scriptable> scriptables = new ArrayList<Scriptable>();
	private final Scriptable parent;

	/**
	 * @param parent
	 */
	public GroupScriptObject(Scriptable parent)
	{
		this.parent = parent;
	}

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_getElementType()
	 */
	public String js_getElementType()
	{
		return "GROUP"; //$NON-NLS-1$
	}

	public void addScriptable(Scriptable scriptable)
	{
		scriptables.add(scriptable);
	}

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_isVisible()
	 */
	public boolean js_getVisible()
	{
		return Boolean.TRUE.equals(get("visible")); //$NON-NLS-1$
	}

	public void js_setVisible(boolean b)
	{
		put("visible", Boolean.valueOf(b)); //$NON-NLS-1$
	}

	/**
	 * @sameas com.servoy.j2db.ui.IScriptBaseMethods#js_isEnabled()
	 */
	public boolean js_getEnabled()
	{
		return Boolean.TRUE.equals(get("enabled")); //$NON-NLS-1$
	}

	public void js_setEnabled(boolean b)
	{
		put("enabled", Boolean.valueOf(b)); //$NON-NLS-1$
	}

	protected Object get(String name)
	{
		if (scriptables.size() > 0)
		{
			return scriptables.get(0).get(name, parent);
		}
		return Scriptable.NOT_FOUND;
	}


	protected void put(String name, Object value)
	{
		for (Scriptable scriptable : scriptables)
		{
			scriptable.put(name, parent, value);
		}
	}

}
