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

package com.servoy.j2db.server.ngclient.scripting;

import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.IFunctionParameters;
import org.sablo.specification.WebObjectFunctionDefinition;

import com.servoy.j2db.server.ngclient.property.types.NGConversions;

/**
 * Base Javascript function to call a client-side function.
 *
 * @author jcompagner
 *
 */
public abstract class WebBaseFunction implements Function
{
	private Scriptable prototype;
	private Scriptable parent;
	protected final WebObjectFunctionDefinition definition;

	public WebBaseFunction(WebObjectFunctionDefinition definition)
	{
		this.definition = definition;
	}

	protected Object[] convertArguments(Object[] arguments, IWebObjectContext webObjectContext)
	{
		var args = arguments;
		if (args != null && args.length > 0)
		{
			IFunctionParameters parameterTypes = definition.getParameters();

			if (args.length == parameterTypes.getDefinedArgsCount() && parameterTypes.isVarArgs() && args[args.length - 1] instanceof List)
			{
				// this is a varargs method, but last argument is given like an Array.
				// spread this array first as if it was a varargs in the arguments array, appending the param array into the arguments directly
				List< ? > varArgsArray = (List< ? >)args[args.length - 1];
				args = new Object[args.length + varArgsArray.size() - 1];
				System.arraycopy(arguments, 0, args, 0, arguments.length - 1);
				System.arraycopy(varArgsArray.toArray(), 0, args, arguments.length - 1, varArgsArray.size());
			}
			for (int i = 0; i < args.length; i++)
			{
				args[i] = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(args[i], null, parameterTypes.getParameterDefinitionTreatVarArgs(i),
					webObjectContext);
			}
		}
		return args;
	}

	@Override
	public String getClassName()
	{
		return "Function";
	}

	@Override
	public Object get(String name, Scriptable start)
	{
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
		return prototype;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		this.prototype = prototype;
	}

	@Override
	public Scriptable getParentScope()
	{
		return parent;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
		this.parent = parent;
	}

	@Override
	public Object[] getIds()
	{
		return new Object[0];
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

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args)
	{
		return null;
	}
}
