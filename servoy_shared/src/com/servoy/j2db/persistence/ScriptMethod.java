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
package com.servoy.j2db.persistence;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;


/**
 * <p>The <code>ScriptMethod</code> class represents a script method in the Servoy environment, offering functionality for defining,
 * managing, and inspecting script methods. It provides various properties for checking the visibility of the method, such as whether
 * it should appear in the menu with the <code>getShowInMenu()</code> method.</p>
 *
 * <p>
 * Additionally, the class allows for setting and retrieving details about the method, including its <code>name</code>, <code>scopeName</code>,
 * and <code>declaration</code>, which can be managed through the <code>setName()</code>, <code>setScopeName()</code>,
 * and <code>setDeclaration()</code> methods. </p>
 *
 * <p>The class also includes features for determining the accessibility and type of the method. For example, it can check if the method is a
 * <code>constructor</code> using the <code>isConstructor()</code> method. It provides flexibility for generating the method signature with
 * <code>getScriptMethodSignature()</code>, which constructs a string representation of the method’s parameters, types, and return type.</p>
 *
 * <p>Overall, the <code>ScriptMethod</code> class is allowing to define and managing methods in the Servoy environment, with a focus on
 * method visibility, signatures, and associated metadata like scope and constructor status.</p>
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "Method", typeCode = IRepository.METHODS)
@ServoyClientSupport(mc = true, wc = true, sc = true)
@SuppressWarnings("nls")
public class ScriptMethod extends AbstractScriptProvider implements IPersistCloneable, ICloneable
{

	private static final long serialVersionUID = 1L;

	private transient Boolean isDeeplink;
	private transient Boolean isPrivate;
	private transient Boolean isProtected;
	private transient Boolean isConstructor;

	private Boolean isPublic;

	/**
	 * Constructor I
	 */
	ScriptMethod(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.METHODS, parent, element_id, uuid);
	}

	public void setShowInMenu(boolean arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU, arg);
	}

	/**
	 * Flag that tells if the method appears or not in the "Methods" menu of Servoy Client.
	 */
	public boolean getShowInMenu()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_SHOWINMENU).booleanValue();
	}


	public String getPrefixedName()
	{
		if (getParent() instanceof Solution)
		{
			return ScopesUtils.getScopeString(this);
		}
		return getName();
	}

	// declaration, name, scopeName overrides - needed for docs mc annotation
	@Override
	public String getDeclaration()
	{
		return super.getDeclaration();
	}

	@Override
	public void setDeclaration(String declaration)
	{
		super.setDeclaration(declaration);
		isDeeplink = null;
		isPrivate = null;
		isProtected = null;
		isPublic = null;
		isConstructor = null;
	}

	@Override
	public String getName()
	{
		return super.getName();
	}

	@Override
	public void setName(String arg)
	{
		super.setName(arg);
	}

	@Override
	public String getScopeName()
	{
		return super.getScopeName();
	}

	@Override
	public void setScopeName(String scopeName)
	{
		super.setScopeName(scopeName);
	}

	public boolean isDeeplink()
	{
		Boolean deepLink = isDeeplink;
		if (deepLink == null)
		{
			String declaration = getDeclaration();
			if (declaration == null)
			{
				deepLink = Boolean.FALSE;
			}
			else
			{
				int index = declaration.indexOf("*/");
				deepLink = Boolean.valueOf(index != -1 && declaration.lastIndexOf("@deeplink", index) != -1);
			}
			isDeeplink = deepLink;
		}
		return deepLink.booleanValue();
	}

	/**
	 * @return
	 */
	public boolean isPrivate()
	{
		Boolean priv = isPrivate;
		if (priv == null)
		{
			String declaration = getDeclaration();
			if (declaration == null)
			{
				priv = Boolean.FALSE;
			}
			else
			{
				int index = declaration.indexOf("*/");
				priv = Boolean.valueOf(index != -1 && declaration.lastIndexOf("@private", index) != -1);
			}
			isPrivate = priv;
		}
		return priv.booleanValue();
	}

	public boolean isProtected()
	{
		if (isPrivate()) return false;
		Boolean prot = isProtected;
		if (prot == null)
		{
			String declaration = getDeclaration();
			if (declaration == null)
			{
				prot = Boolean.FALSE;
			}
			else
			{
				int index = declaration.indexOf("*/");
				prot = Boolean.valueOf(index != -1 && declaration.lastIndexOf("@protected", index) != -1);
			}
			isProtected = prot;
		}
		return prot.booleanValue();
	}

	public boolean isPublic()
	{
		Boolean publ = isPublic;
		if (publ == null)
		{
			String declaration = getDeclaration();
			if (declaration == null)
			{
				publ = Boolean.FALSE;
			}
			else
			{
				int index = declaration.indexOf("*/");
				publ = Boolean.valueOf(index != -1 && declaration.lastIndexOf("@public", index) != -1);
			}
			isPublic = publ;
		}
		return publ.booleanValue();
	}

	public boolean isConstructor()
	{
		Boolean constructor = isConstructor;
		if (constructor == null)
		{
			String declaration = getDeclaration();
			if (declaration == null)
			{
				constructor = Boolean.FALSE;
			}
			else
			{
				int index = declaration.indexOf("*/");
				constructor = Boolean.valueOf(index != -1 && declaration.lastIndexOf("@constructor", index) != -1);
			}
			isConstructor = constructor;
		}
		return constructor.booleanValue();
	}


	@Override
	public String toString()
	{
		return "ScriptMethod[name:" + (getParent() instanceof Solution ? ScopesUtils.getScopeString(this) : getName()) + ", inmenu:" + getShowInMenu() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public String getScriptMethodSignature(String methodName, boolean showParam, boolean showParamType, boolean showReturnType, boolean showReturnTypeAtEnd)
	{
		MethodArgument[] args = getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);

		StringBuilder methodSignatureBuilder = new StringBuilder();
		methodSignatureBuilder.append(methodName != null ? methodName : getName()).append('(');
		for (int i = 0; i < args.length; i++)
		{
			if ((showParam || showParamType) && args[i].isOptional()) methodSignatureBuilder.append('[');
			if (showParam)
			{
				methodSignatureBuilder.append(args[i].getName());
				if (showParamType) methodSignatureBuilder.append(':');
			}
			if (showParamType) methodSignatureBuilder.append(args[i].getType());
			if ((showParam || showParamType) && args[i].isOptional()) methodSignatureBuilder.append(']');
			if (i < args.length - 1) methodSignatureBuilder.append(", ");
		}
		methodSignatureBuilder.append(')');

		if (showReturnType)
		{
			MethodArgument returnTypeArgument = getRuntimeProperty(IScriptProvider.METHOD_RETURN_TYPE);
			String returnType = "void";
			if (returnTypeArgument != null)
			{
				if ("*".equals(returnTypeArgument.getType().getName())) returnType = "Any";
				else returnType = returnTypeArgument.getType().getName();
			}
			if (showReturnTypeAtEnd)
			{
				methodSignatureBuilder.append(" - ").append(returnType);
			}
			else
			{
				methodSignatureBuilder.insert(0, ' ').insert(0, returnType);
			}
		}

		return methodSignatureBuilder.toString();
	}

}
