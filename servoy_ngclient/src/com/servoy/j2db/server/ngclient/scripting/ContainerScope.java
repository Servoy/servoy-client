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

import java.util.List;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @author jcompagner
 * @since 2019.12
 */
@SuppressWarnings("nls")
public class ContainerScope implements Scriptable
{
	private static final String SERVICE = "layoutcontainer_manager";
	private final ContainersScope parent;
	private final String name;
	private final List<String> cssClasses;

	public ContainerScope(ContainersScope parent, String name, List<String> cssClasses)
	{
		this.parent = parent;
		this.name = name;
		this.cssClasses = cssClasses;
	}

	@Override
	public String getClassName()
	{
		return "Container";
	}

	@Override
	public Object get(String nm, Scriptable start)
	{
		return new Callable()
		{

			@Override
			public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
			{
				if (args.length >= 1 && args[0] != null)
				{
					if ("setCSSStyle".equals(nm) && args.length == 2)
					{
						parent.getFormControler().getFormUI().addContainerCSSStyle(name, (String)args[0], (String)args[1]);
					}
					else if ("removeCSSStyle".equals(nm))
					{
						parent.getFormControler().getFormUI().removeContainerCSSStyle(name, (String)args[0]);
					}
					else
					{
						for (Object object : args)
						{
							String cls = (String)object;
							switch (nm)
							{
								case "addStyleClasses" :
									if (!cssClasses.contains(cls))
									{
										cssClasses.add(cls);
										parent.getFormControler().getFormUI().addContainerStyleClass(name, cls);
									}
									else return Boolean.FALSE;
									break;
								case "removeStyleClasses" :
									if (cssClasses.remove(cls))
									{
										// it did remove it push to client
										parent.getFormControler().getFormUI().removeContainerStyleClass(name, cls);
									}
									else return Boolean.FALSE;
									break;
								case "hasStyleClasses" :
									if (cssClasses.indexOf(cls) == -1) return Boolean.FALSE;
							}
						}
					}
				}
				return Boolean.TRUE;
			}
		};
	}


	@Override
	public Object get(int index, Scriptable start)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public boolean has(String nm, Scriptable start)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String nm, Scriptable start, Object value)
	{
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
	}

	@Override
	public void delete(String nm)
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
		return parent;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
	}

	@Override
	public Object[] getIds()
	{
		return new String[] { "addStyleClass", "removeStyleClass", "hasStyleClass", "setCSSStyle", "removeStyleClasses" };
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return "ContainerScope[" + name + "]";
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

}
