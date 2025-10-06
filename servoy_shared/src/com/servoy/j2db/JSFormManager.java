/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.util.Debug;

/**
 * @author emera
 */
public class JSFormManager implements Scriptable, IJSFormManager
{
	private final ClientState application;

	public JSFormManager(ClientState application)
	{
		super();
		this.application = application;
	}


	@Override
	public String getClassName()
	{
		return null;
	}


	@Override
	public Object get(String name, Scriptable start)
	{
		try
		{
			IForm frm = application.getFormManager().getForm(name);
			if (frm instanceof BasicFormController fc)
			{
				return new JSForm(fc);
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}


	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}


	@Override
	public boolean has(String name, Scriptable start)
	{
		return get(name, start) != null;
	}


	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}


	@Override
	public void put(String name, Scriptable start, Object value)
	{
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
	}

	@Override
	public void delete(String name)
	{
	}

	@Override
	public void delete(int index)
	{
	}

	@Override
	public Scriptable getPrototype()
	{
		return null;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
	}

	@Override
	public Scriptable getParentScope()
	{
		return null;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
	}

	@Override
	public Object[] getIds()
	{
		List<String> controllers = new ArrayList<>();
		try
		{
			application.getFlattenedSolution().getForms(true).forEachRemaining(form -> controllers.add(form.getName()));
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return controllers.toArray();
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return null;
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}
}
