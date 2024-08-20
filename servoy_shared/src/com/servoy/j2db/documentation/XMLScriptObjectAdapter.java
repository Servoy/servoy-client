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

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ITypedScriptObject;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Text;

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

	@Override
	public IObjectDocumentation getObjectDocumentation()
	{
		return objDoc;
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

	public String[] getParameterNames(String methodName, Class< ? >[] argTypes)
	{
		IParameter[] parameters = getParameters(methodName, argTypes);
		String[] paramNames = null;
		if (parameters != null)
		{
			paramNames = new String[parameters.length];
			for (int i = 0; i < parameters.length; i++)
			{
				paramNames[i] = parameters[i].getName();
			}
		}
		return paramNames;
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
				String description = argDoc.getDescription();
				boolean optional = argDoc.isOptional();
				boolean varargs = false;
				if (argTypes != null && params.length == argTypes.length && i == params.length - 1)
				{
//					varargs = argTypes[i].isArray() && !argTypes[i].getComponentType().isPrimitive();
					varargs = fdoc.isVarargs();
				}
				params[i++] = new ScriptParameter(name, null, argDoc.getType(), description, optional, varargs);
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

	public String getJSTranslatedSignature(String methodName, Class< ? >[] argTypes)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
		if (fdoc != null)
		{
			return fdoc.getFullJSTranslatedSignature(false, true);
		}
		return null;
	}

	public String getSample(String methodName)
	{
		return getSample(methodName, ClientSupport.Default);
	}

	public String getSample(String methodName, ClientSupport csp)
	{
		return getSample(methodName, null, csp);
	}

	public String getSample(String methodName, Class< ? >[] argTypes)
	{
		return getSample(methodName, argTypes, ClientSupport.Default);
	}

	public String getSample(final String methodName, Class< ? >[] argTypes, ClientSupport csp)
	{
		IFunctionDocumentation fdoc = (argTypes == null ? objDoc.getFunction(methodName) : objDoc.getFunction(methodName, argTypes));
		if (fdoc != null)
		{
			if (!fdoc.isDocumented() && original != null)
			{
				return original.getSample(methodName);
			}
			return fdoc.getSample(csp);
		}
		if (original != null) return original.getSample(methodName);
		return null;
	}

	public String getToolTip(String methodName)
	{
		return getToolTip(methodName, null, ClientSupport.Default);
	}

	public String getToolTip(String methodName, Class< ? >[] argTypes)
	{
		return getToolTip(methodName, argTypes, ClientSupport.Default);
	}

	public String getToolTip(String methodName, ClientSupport csp)
	{
		return getToolTip(methodName, null, csp);
	}

	public String getToolTip(String methodName, Class< ? >[] argTypes, ClientSupport csp)
	{
		IFunctionDocumentation fdoc = (argTypes != null ? objDoc.getFunction(methodName, argTypes) : objDoc.getFunction(methodName));
		if (fdoc != null)
		{
			if (!fdoc.isDocumented() && original != null) return original.getToolTip(methodName);
			return (fdoc.getDescription(csp) == null && csp == ClientSupport.mc ? fdoc.getDescription(ClientSupport.Default) : fdoc.getDescription(csp));
		}
		if (original != null) return original.getToolTip(methodName);
		return null;
	}

	/**
	 * Combines the description with sample, parameters/parameter descriptions and return value/return value description.<br/>
	 * CAN return null!
	 */
	public String getExtendedTooltip(String methodName, Class< ? >[] argTypes, ClientSupport csp, ITagResolver resolver)
	{
		Class< ? > returnType = getReturnedType(methodName, argTypes);
		String returnDescription = getReturnDescription(methodName, argTypes);
		IParameter[] parameters = getParameters(methodName, argTypes);
		String extendedTooltip = getToolTip(methodName, argTypes, csp);
		String sample = getSample(methodName, argTypes, csp);
		boolean moreDetailsWereAdded = false;

		extendedTooltip = HtmlUtils.applyDescriptionMagic(extendedTooltip);

		if (sample != null)
		{
			if (!moreDetailsWereAdded) extendedTooltip += "<br/>";
			moreDetailsWereAdded = true;
			extendedTooltip += "<br/><pre><i>" + Text.processTags(HtmlUtils.escapeMarkup(sample).toString(), resolver).toString() + "</i></pre>";
		}
		if (parameters != null)
		{
			if (!moreDetailsWereAdded) extendedTooltip += "<br/>";
			moreDetailsWereAdded = true;
			extendedTooltip += "<br/>";
			String description;
			for (IParameter parameter : parameters)
			{
				description = parameter.getDescription();
				extendedTooltip = extendedTooltip + "<br/> <b>@param</b> {" + parameter.getType() + "} " + parameter.getName() + " " +
					(description != null ? HtmlUtils.applyDescriptionMagic(description) : "");
			}
		}
		if (returnType != null)
		{
			IFunctionDocumentation fdoc = objDoc.getFunction(methodName, argTypes);
			if (fdoc == null || fdoc.getType() == IFunctionDocumentation.TYPE_FUNCTION)
			{
				if (!moreDetailsWereAdded) extendedTooltip += "<br/>";
				moreDetailsWereAdded = true;
				extendedTooltip = extendedTooltip + "<br/> <b>@return</b> {" + getReturnTypeString(returnType) + "} ";
				if (returnDescription != null) extendedTooltip += HtmlUtils.applyDescriptionMagic(returnDescription);
			}
		}

		return extendedTooltip;
	}

	public static String getReturnTypeString(Class< ? > returnType)
	{
		if (returnType == null) return "*unknown*";
		StringBuilder sb = new StringBuilder();
		while (returnType.isArray())
		{
			sb.append("[]");
			returnType = returnType.getComponentType();
		}
		sb.insert(0, DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(returnType));
		return sb.toString();
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

	public boolean isSpecial(String propertyName)
	{
		IFunctionDocumentation fdoc = objDoc.getFunction(propertyName, (Class< ? >[])null);
		if (fdoc != null) return fdoc.isSpecial();
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
