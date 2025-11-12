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
import java.util.stream.Collectors;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.util.Debug;

/**
 * @author emera
 */
public class JSFormManager extends DefaultScope implements IJSFormManager
{
	private final IApplication application;

	public JSFormManager(IApplication application, Scriptable scope)
	{
		super(scope);
		this.application = application;
	}


	@Override
	public String getClassName()
	{
		return null;
	}

	@SuppressWarnings("nls")
	@Override
	public Object get(String name, Scriptable start)
	{
		switch (name)
		{
			// view types (deprecated)
			case "RECORD_VIEW" :
				return IForm.RECORD_VIEW;
			case "LIST_VIEW" :
				return IForm.LIST_VIEW;
			case "LOCKED_RECORD_VIEW" :
				return IForm.LOCKED_RECORD_VIEW;
			case "LOCKED_LIST_VIEW" :
				return IFormConstants.VIEW_TYPE_LIST_LOCKED; // deprecated
			case "LOCKED_TABLE_VIEW" :
				return IFormConstants.VIEW_TYPE_TABLE_LOCKED; // deprecated

			// encapsulation (all deprecated)
			case "DEFAULT_ENCAPSULATION" :
				return IFormConstants.DEFAULT;
			case "PRIVATE_ENCAPSULATION" :
				return IFormConstants.HIDE_IN_SCRIPTING_MODULE_SCOPE;
			case "MODULE_PRIVATE_ENCAPSULATION" :
				return IFormConstants.MODULE_SCOPE;
			case "HIDE_DATAPROVIDERS_ENCAPSULATION" :
				return IFormConstants.HIDE_DATAPROVIDERS;
			case "HIDE_FOUNDSET_ENCAPSULATION" :
				return IFormConstants.HIDE_FOUNDSET;
			case "HIDE_CONTROLLER_ENCAPSULATION" :
				return IFormConstants.HIDE_CONTROLLER;
			case "HIDE_ELEMENTS_ENCAPSULATION" :
				return IFormConstants.HIDE_ELEMENTS;

			// selection modes
			case "SELECTION_MODE_DEFAULT" :
				return IForm.SELECTION_MODE_DEFAULT;
			case "SELECTION_MODE_SINGLE" :
				return IForm.SELECTION_MODE_SINGLE;
			case "SELECTION_MODE_MULTI" :
				return IForm.SELECTION_MODE_MULTI;

			// foundset constants
			case "EMPTY_FOUNDSET" :
				return Form.NAMED_FOUNDSET_EMPTY;
			case "SEPARATE_FOUNDSET" :
				return Form.NAMED_FOUNDSET_SEPARATE;

			case "NAMES" :
				return getNames(start);
			case "INSTANCES" :
				return getInstances(start);

			default :
				return getJSForm(name);
		}
	}

	private Object getInstances(Scriptable start)
	{
		final List<String> forms = new ArrayList<>();
		application.getFlattenedSolution().getForms(true)
			.forEachRemaining(form -> forms.add(form.getName()));
		Scriptable topLevel = ScriptableObject.getTopLevelScope(start);
		ScriptableObject instancesScope = new NativeObject()
		{
			@Override
			public Object get(String name, Scriptable start)
			{
				if (has(name, start)) return super.get(name, start);

				if (forms.contains(name))
				{
					JSForm jsForm = getJSForm(name);
					put(name, this, jsForm);
					return jsForm;
				}

				return super.get(name, start);
			}

			@Override
			public Object[] getIds()
			{
				return forms.toArray(new Object[0]);
			}

			@Override
			public String toString()
			{
				return forms.stream().collect(Collectors.joining(", ", "{", "}"));
			}
		};
		instancesScope.setParentScope(topLevel);
		return instancesScope;
	}

	private JSForm getJSForm(String formName)
	{
		Form formInstance = application.getFlattenedSolution().getForm(formName);
		JSForm jsForm = null;
		if (formInstance != null)
		{
			jsForm = new JSForm(application, formInstance, false);
		}
		return jsForm;
	}

	private Object getNames(Scriptable start)
	{
		Scriptable topLevel = ScriptableObject.getTopLevelScope(start);
		ScriptableObject namesScope = new NativeObject();
		namesScope.setParentScope(topLevel);

		application.getFlattenedSolution().getForms(true)
			.forEachRemaining(form -> namesScope.put(form.getName(), namesScope, form.getName()));

		return namesScope;
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
		List<String> ids = new ArrayList<>();
		try
		{
			application.getFlattenedSolution().getForms(true).forEachRemaining(form -> ids.add(form.getName()));
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return ids.toArray();
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
