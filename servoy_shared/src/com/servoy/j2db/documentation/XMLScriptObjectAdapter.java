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
package com.servoy.j2db.documentation;

import java.util.HashSet;
import java.util.Set;

import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ITypedScriptObject;
import com.servoy.j2db.util.Debug;

@SuppressWarnings({ "nls", "deprecation" })
public class XMLScriptObjectAdapter implements ITypedScriptObject
{
	private final IObjectDocumentation objDoc;
	private final IScriptObject original;
	private Class< ? >[] returnedTypes = null;
	private boolean returnedTypesComputed = false;

	public XMLScriptObjectAdapter(IObjectDocumentation objDoc, IScriptObject original)
	{
		this.objDoc = objDoc;
		this.original = original;
	}

	public IScriptObject getScriptObject()
	{
		return original;
	}

	/**
	 * @param allReturnedTypes
	 */
	public void setReturnTypes(Class< ? >[] allReturnedTypes)
	{
		returnedTypes = allReturnedTypes;
		returnedTypesComputed = true;
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		if (!returnedTypesComputed)
		{
			if (objDoc.getReturnedTypes().size() > 0)
			{
				Set<Class< ? >> returnedTypesSet = new HashSet<Class< ? >>();
				for (String className : objDoc.getReturnedTypes())
				{
					try
					{
						returnedTypesSet.add(Class.forName(className));
					}
					catch (ClassNotFoundException e)
					{
						Debug.error("Cannot load returned class '" + className + "' from documented object '" + objDoc.getQualifiedName() + "'.");
					}
				}
				returnedTypes = new Class< ? >[returnedTypesSet.size()];
				returnedTypesSet.toArray(returnedTypes);
			}
			returnedTypesComputed = true;
		}
		return returnedTypes;
	}

	public String[] getParameterNames(String methodName)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName);
		if (fdoc != null)
		{
			String[] argNames = new String[fdoc.getArguments().size()];
			int i = 0;
			for (IParameterDocumentation argDoc : fdoc.getArguments().values())
			{
				String name = argDoc.isOptional() ? "[" + argDoc.getName() + "]" : argDoc.getName();
				argNames[i++] = name;
			}
			return argNames;
		}
		if (original != null) return original.getParameterNames(methodName);
		return null;
	}

	public IParameter[] getParameters(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			IParameter[] params = new IParameter[fdoc.getArguments().size()];
			int i = 0;
			for (IParameterDocumentation argDoc : fdoc.getArguments().values())
			{
				String name = argDoc.getName();
				String type = argDoc.getType() != null ? argDoc.getType().getSimpleName() : "Object";
				String description = argDoc.getDescription();
				boolean optional = argDoc.isOptional();
				boolean varargs = false;
				if (argTypes != null && params.length == argTypes.length && i == params.length - 1)
				{
//					varargs = argTypes[i].isArray() && !argTypes[i].getComponentType().isPrimitive();
					varargs = fdoc.isVarargs();
				}
				params[i++] = new ScriptParameter(name, type, argDoc.getType(), description, optional, varargs);
			}
			return params;
		}
		return null;
	}

	public String getSignature(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getFullSignature(false, true);
		}
		return null;
	}

	public String getSample(String methodName)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName);
		if (fdoc != null)
		{
			if (!fdoc.isDocumented() && original != null) return original.getSample(methodName);
			return fdoc.getSample();
		}
		if (original != null) return original.getSample(methodName);
		return null;
	}

	public String getSample(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			if (!fdoc.isDocumented() && original != null) return original.getSample(methodName);
			return fdoc.getSample();
		}
		if (original != null) return original.getSample(methodName);
		return null;
	}

	public String getToolTip(String methodName)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName);
		if (fdoc != null)
		{
			if (!fdoc.isDocumented() && original != null) return original.getToolTip(methodName);
			return fdoc.getDescription();
		}
		if (original != null) return original.getToolTip(methodName);
		return null;
	}

	public String getToolTip(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			if (!fdoc.isDocumented() && original != null) return original.getToolTip(methodName);
			return fdoc.getDescription();
		}
		if (original != null) return original.getToolTip(methodName);
		return null;
	}

	public Class< ? > getReturnedType(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getReturnedType();
		}
		return null;
	}

	public String getReturnDescription(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getReturnDescription();
		}
		return null;
	}

	public String getSince(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getSince();
		}
		return null;
	}

	public String getUntil(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getUntil();
		}
		return null;
	}

	public boolean isDeprecated(String methodName)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName);
		if (fdoc != null) return fdoc.isDeprecated();
		if (original != null) return original.isDeprecated(methodName);
		return false;
	}

	public boolean isDeprecated(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null) return fdoc.isDeprecated();
		if (original != null) return original.isDeprecated(methodName);
		return false;
	}

	public String getDeprecatedText(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getDeprecatedText();
		}
		return null;
	}
}
