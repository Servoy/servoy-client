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
import java.util.Collections;
import java.util.List;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author emera
 */
public class JSComponentManager extends DefaultScope implements IJSComponentManager
{
	public JSComponentManager(Scriptable scope)
	{
		super(scope);
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (isPackage(name))
		{
			return new PackageScriptable(name, start);
		}

		return Scriptable.NOT_FOUND;
	}

	private boolean isPackage(String name)
	{
		if (name == null) return false;
		return WebComponentSpecProvider.getSpecProviderState().getPackageNames().contains(name);
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return isPackage(name);
	}

	@Override
	public Object[] getIds()
	{
		SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
		return componentsSpecProviderState.getPackageNames().toArray(new String[0]);
	}

	private class PackageScriptable extends DefaultScope
	{
		private final String packageName;

		public PackageScriptable(String packageName, Scriptable parent)
		{
			super(parent);
			this.packageName = packageName;
		}

		@Override
		public Object get(String name, Scriptable start)
		{
			if (name == null) return Scriptable.NOT_FOUND;

			String componentName = packageName + '-' + name;
			if (isComponent(componentName))
			{
				return new ComponentScriptable(componentName, start);
			}
			return Scriptable.NOT_FOUND;
		}

		private boolean isComponent(String name)
		{
			return WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(name.replace('_', '-')) != null;
		}

		@Override
		public boolean has(String name, Scriptable start)
		{
			return isComponent(packageName + '-' + name);
		}

		@Override
		public Object[] getIds()
		{
			SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
			PackageSpecification<WebObjectSpecification> pkg = componentsSpecProviderState.getWebObjectSpecifications().get(packageName);
			return pkg.getSpecifications().keySet().stream()
				.map(specName -> specName.substring(specName.indexOf('-') + 1))
				.toArray(String[]::new);
		}
	}

	private class ComponentScriptable extends NativeObject
	{
		final String componentName;
		final List<String> members;

		ComponentScriptable(String componentName, Scriptable start)
		{
			this.componentName = componentName;
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentName);

			if (spec != null)
			{
				members = new ArrayList<>();
				members.addAll(spec.getAllPropertiesNames());
				members.addAll(spec.getApiFunctions().keySet());
			}
			else members = Collections.emptyList();

			setParentScope(ScriptableObject.getTopLevelScope(start));
		}

		@Override
		public Object get(String name, Scriptable start)
		{
			if (members.contains(name))
			{
				put(name, this, name);
				return name;
			}
			return Scriptable.NOT_FOUND;
		}

		@Override
		public Object[] getIds()
		{
			return members.toArray(new Object[0]);
		}

		@Override
		public Object getDefaultValue(Class< ? > typeHint)
		{
			return componentName;
		}
	}
}
