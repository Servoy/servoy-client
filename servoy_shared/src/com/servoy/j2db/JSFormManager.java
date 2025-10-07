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

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.persistence.Form;
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
		//view types (deprecated)
		if (name.equals("RECORD_VIEW")) return IForm.RECORD_VIEW;
		if (name.equals("LIST_VIEW")) return IForm.LIST_VIEW;
		if (name.equals("LOCKED_RECORD_VIEW")) return IForm.LOCKED_RECORD_VIEW;
		if (name.equals("LOCKED_LIST_VIEW")) return IFormConstants.VIEW_TYPE_LIST_LOCKED; //deprecated
		if (name.equals("LOCKED_TABLE_VIEW")) return IFormConstants.VIEW_TYPE_TABLE_LOCKED; //deprecated

		//encapsulation (all deprecated)
		if (name.equals("DEFAULT_ENCAPSULATION")) return IFormConstants.DEFAULT;
		if (name.equals("PRIVATE_ENCAPSULATION")) return IFormConstants.HIDE_IN_SCRIPTING_MODULE_SCOPE;
		if (name.equals("MODULE_PRIVATE_ENCAPSULATION")) return IFormConstants.MODULE_SCOPE;
		if (name.equals("HIDE_DATAPROVIDERS_ENCAPSULATION")) return IFormConstants.HIDE_DATAPROVIDERS;
		if (name.equals("HIDE_FOUNDSET_ENCAPSULATION")) return IFormConstants.HIDE_FOUNDSET;
		if (name.equals("HIDE_CONTROLLER_ENCAPSULATION")) return IFormConstants.HIDE_CONTROLLER;
		if (name.equals("HIDE_ELEMENTS_ENCAPSULATION")) return IFormConstants.HIDE_ELEMENTS;


		//selection modes
		if (name.equals("SELECTION_MODE_DEFAULT")) return IForm.SELECTION_MODE_DEFAULT;
		if (name.equals("SELECTION_MODE_SINGLE")) return IForm.SELECTION_MODE_SINGLE;
		if (name.equals("SELECTION_MODE_MULTI")) return IForm.SELECTION_MODE_MULTI;

		//foundset
		if (name.equals("EMPTY_FOUNDSET")) return Form.NAMED_FOUNDSET_EMPTY;
		if (name.equals("SEPARATE_FOUNDSET")) return Form.NAMED_FOUNDSET_SEPARATE;


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
