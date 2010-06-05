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

import com.servoy.j2db.FormController;

/**
 * @author jcompagner
 * 
 */
public class SelectedRecordScope implements Scriptable
{
	private final Scriptable prototype;

	private final FormController fc;

	public SelectedRecordScope(FormController fc, Scriptable prototype)
	{
		this.fc = fc;
		this.prototype = prototype;

	}

	private Scriptable getSelectedRecord()
	{
		int selectedIndex = fc.getFormModel().getSelectedIndex();
		Scriptable record = (Scriptable)fc.getFormModel().getRecord(selectedIndex);
		if (record == null)
		{
			record = fc.getFormModel().getPrototypeState();
		}
		return record;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getParentScope()
	 */
	public Scriptable getParentScope()
	{
		return null;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#setParentScope(org.mozilla.javascript.Scriptable)
	 */
	public void setParentScope(Scriptable parent)
	{
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#getPrototype()
	 */
	public Scriptable getPrototype()
	{
		return prototype;
	}

	/**
	 * @see org.mozilla.javascript.Scriptable#setPrototype(org.mozilla.javascript.Scriptable)
	 */
	public void setPrototype(Scriptable prototype)
	{
	}

	public void delete(int index)
	{
		getSelectedRecord().delete(index);
	}

	public void delete(String name)
	{
		getSelectedRecord().delete(name);
	}

	public Object get(int index, Scriptable start)
	{
		return getSelectedRecord().get(index, start);
	}

	public Object get(String name, Scriptable start)
	{
		return getSelectedRecord().get(name, start);
	}

	public String getClassName()
	{
		return getSelectedRecord().getClassName();
	}

	public Object getDefaultValue(Class hint)
	{
		return getSelectedRecord().getDefaultValue(hint);
	}

	public Object[] getIds()
	{
		return getSelectedRecord().getIds();
	}

	public boolean has(int index, Scriptable start)
	{
		return getSelectedRecord().has(index, start);
	}

	public boolean has(String name, Scriptable start)
	{
		return getSelectedRecord().has(name, start);
	}

	public boolean hasInstance(Scriptable instance)
	{
		return getSelectedRecord().hasInstance(instance);
	}

	public void put(int index, Scriptable start, Object value)
	{
		getSelectedRecord().put(index, start, value);
	}

	public void put(String name, Scriptable start, Object value)
	{
		getSelectedRecord().put(name, start, value);
	}

}
