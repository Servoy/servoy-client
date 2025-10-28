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
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

/**
 * @author emera
 */
public class JSComponentManager implements Scriptable, IJSComponentManager
{
	private final ClientState application;

	public JSComponentManager(ClientState application)
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
		if (isComponent(name))
		{
			String componentName = name.replace('_', '-');
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentName);
			if (spec == null) return Scriptable.NOT_FOUND;

			List<String> list = new ArrayList<>();
			list.addAll(spec.getAllPropertiesNames());
			list.addAll(spec.getApiFunctions().keySet());

			Scriptable componentObj = new NativeObject()
			{
				@Override
				public Object get(String name_, Scriptable start_)
				{
					if (has(name_, start_)) return super.get(name_, start_);

					if (list.contains(name_))
					{
						put(name_, this, name_);
						return name_;
					}
					return Scriptable.NOT_FOUND;
				}

				@Override
				public Object[] getIds()
				{
					return list.toArray(new Object[0]);
				}

				@Override
				public String toString()
				{
					return list.stream().collect(Collectors.joining(", ", "{", "}"));
				}
			};
			Scriptable topLevel = ScriptableObject.getTopLevelScope(start);
			componentObj.setParentScope(topLevel);
			return componentObj;
		}
		return Scriptable.NOT_FOUND;
	}

	private boolean isComponent(String name)
	{
		if (name == null) return false;
		return WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(name.replace('_', '-')) != null;
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
		SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
		return componentsSpecProviderState.getWebObjectSpecifications()
			.values()
			.stream()
			.flatMap(componentsPackage -> componentsPackage.getSpecifications().keySet().stream())
			.toArray(String[]::new);
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
