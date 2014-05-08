/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyType;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpec;

import com.servoy.j2db.server.ngclient.WebComponent;
import com.servoy.j2db.server.ngclient.scripting.WebComponentFunction;

/**
 * @author lvostinar
 *
 */
public class RuntimeWebComponent implements Scriptable
{
	private final WebComponent component;
	private Scriptable prototypeScope;
	private final Set<String> specProperties;
	private final Set<String> dataProviderProperties;
	private final Map<String, WebComponentFunction> apiFunctions;

	public RuntimeWebComponent(WebComponent component, WebComponentSpec webComponentSpec)
	{
		this.component = component;
		this.specProperties = webComponentSpec == null ? null : webComponentSpec.getAllPropertiesNames();
		this.apiFunctions = new HashMap<String, WebComponentFunction>();
		this.dataProviderProperties = new HashSet<>();
		if (webComponentSpec != null)
		{
			for (WebComponentApiDefinition def : webComponentSpec.getApis().values())
			{
				apiFunctions.put(def.getName(), new WebComponentFunction(component, def));
			}
			Map<String, PropertyDescription> specs = webComponentSpec.getProperties();
			for (Entry<String, PropertyDescription> e : specs.entrySet())
			{
				if (e.getValue().getType() == PropertyType.dataprovider)
				{
					dataProviderProperties.add(e.getKey());
				}
			}
		}
	}

	@Override
	public String getClassName()
	{
		return null;
	}

	@Override
	public Object get(String name, final Scriptable start)
	{
		if (specProperties != null && specProperties.contains(name))
		{
			return component.getConvertedPropertyWithDefault(name, dataProviderProperties.contains(name), true);
		}
		if (apiFunctions.containsKey(name))
		{
			return apiFunctions.get(name);
		}

		// check if we have a setter/getter for this property
		if (name != null && name.length() > 0)
		{
			String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
			if (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName))
			{
				// call getter
				WebComponentFunction propertyGetter = apiFunctions.get("get" + uName);
				return propertyGetter.call(null, null, null, null);
			}
		}

		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		if (specProperties != null && specProperties.contains(name))
		{
			return true;
		}
		if (apiFunctions.containsKey(name)) return true;

		// check if we have a setter/getter for this property
		if (name != null && name.length() > 0)
		{
			String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
			return (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName));
		}
		return false;
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		if (dataProviderProperties.contains(name))
		{
			// cannot set, not supported
			return;
		}
		if (specProperties != null && specProperties.contains(name))
		{
			component.putProperty(name, value);
		}
		else if (prototypeScope != null)
		{
			if (!apiFunctions.containsKey(name))
			{
				// check if we have a setter for this property
				if (name != null && name.length() > 0)
				{
					String uName = new StringBuffer(name.substring(0, 1).toUpperCase()).append(name.substring(1)).toString();
					if (apiFunctions.containsKey("set" + uName) && apiFunctions.containsKey("get" + uName))
					{
						// call setter
						WebComponentFunction propertySetter = apiFunctions.get("set" + uName);
						propertySetter.call(null, null, null, new Object[] { value });
						return;
					}
				}
				prototypeScope.put(name, start, value);
			}
		}
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
		return prototypeScope;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		this.prototypeScope = prototype;
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
		ArrayList<String> al = new ArrayList<>();
		al.addAll(specProperties);
		al.addAll(apiFunctions.keySet());
		return al.toArray();
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
