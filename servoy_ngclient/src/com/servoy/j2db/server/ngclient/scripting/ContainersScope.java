/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.server.ngclient.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2019.12
 */
@SuppressWarnings("nls")
public class ContainersScope implements Scriptable
{
	private final IWebFormController fc;
	private final Map<String, List<String>> namesToLayout = new HashMap<>();

	/**
	 * @param fc
	 */
	public ContainersScope(IWebFormController fc)
	{
		super();
		this.fc = fc;
		fillNames(fc.getForm().getLayoutContainers(), namesToLayout);
	}

	public static Set<String> getAllLayoutNames(Form form)
	{
		Map<String, List<String>> names = new HashMap<>();
		fillNames(form.getLayoutContainers(), names);
		return names.keySet();
	}

	private static void fillNames(Iterator<LayoutContainer> iterator, Map<String, List<String>> namesToLayout)
	{
		while (iterator.hasNext())
		{
			LayoutContainer lc = iterator.next();
			if (lc.getName() != null)
			{
				WebLayoutSpecification spec = null;
				if (lc.getPackageName() != null)
				{
					PackageSpecification<WebLayoutSpecification> pkg = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
						lc.getPackageName());
					if (pkg != null)
					{
						spec = pkg.getSpecification(lc.getSpecName());
					}
				}
				if (spec != null)
				{
					List<String> styleClassValues = new ArrayList<String>();
					String cssClasses = lc.getCssClasses();
					if (cssClasses != null)
					{
						styleClassValues.addAll(Arrays.asList(cssClasses.split(" ")));
					}
					namesToLayout.put(lc.getName(), styleClassValues);
				}
				else
				{
					Debug.warn(
						"Couldn't register form container '" + lc.getName() + "' to the 'containters' because spec '" + lc.getPackageName() + "' not found");
				}
			}
			fillNames(lc.getLayoutContainers(), namesToLayout);
		}
	}

	public IWebFormController getFormControler()
	{
		return fc;
	}

	@Override
	public String getClassName()
	{
		return "LayoutContainers";
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		List<String> cssClasses = namesToLayout.get(name);
		if (cssClasses != null)
		{
			return new ContainerScope(this, name, cssClasses);
		}
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return namesToLayout.containsKey(name);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Scriptable getParentScope()
	{
		return fc.getFormScope();
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
	}

	@Override
	public Object[] getIds()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return "ContainerScope< " + fc + ">";
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

}
