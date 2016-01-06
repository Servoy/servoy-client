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

import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
public abstract class AbstractScriptProvider extends AbstractBase
	implements IScriptProvider, ISupportUpdateableName, ISupportContentEquals, ISupportDeprecatedAnnotation
{

	private static final long serialVersionUID = 1L;

	private transient String methodCode;
	private transient Boolean isDeprecated;

	/**
	 * @param type
	 * @param parent
	 * @param elementId
	 * @param uuid
	 */
	public AbstractScriptProvider(int type, ISupportChilds parent, int elementId, UUID uuid)
	{
		super(type, parent, elementId, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */
	@Deprecated
	public void setSource(String arg)
	{
		setMethodCode(arg);
	}

	@Deprecated
	public String getSource()
	{
		return getMethodCode();
	}

	//the repository element id can differ!
	public boolean contentEquals(Object obj)
	{
		if (getClass() != obj.getClass()) return false;
		if (obj instanceof AbstractScriptProvider && getName() != null)
		{
			AbstractScriptProvider other = (AbstractScriptProvider)obj;
			if (getName().equals(other.getName()))
			{
				if (getDeclaration() != null)
				{
					return getDeclaration().equals(other.getDeclaration());
				}
				return other.getDeclaration() == null;
			}
		}
		return false;
	}

	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		if (methodCode != null) setMethodCode(methodCode);
	}

	/**
	 * updates the name
	 *
	 * @param arg the name
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(getParent(), type), false);
		String declaration = getTypedProperty(StaticContentSpecLoader.PROPERTY_DECLARATION);
		if (declaration != null)
		{
			declaration = declaration.replaceFirst("(.*function\\s+)" + getName() + "(\\s*\\(.*\\)\\s*\\{.*)", "$1" + arg + "$2"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			setTypedProperty(StaticContentSpecLoader.PROPERTY_DECLARATION, declaration);
		}
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		if (methodCode != null) setMethodCode(methodCode);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMMethod#getName()
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the declaration
	 *
	 * @param arg the declaration
	 */
	@Deprecated
	public void setMethodCode(String arg)
	{
		String name = getName();
		if (name == null)
		{
			// keep deprecated methodCode until name is set
			methodCode = arg;
		}
		else
		{
			setDeclaration(MethodTemplate.getTemplate(getClass(), null).getMethodDeclaration(name, arg, null));
		}
	}

	/**
	 * Get the declaration
	 *
	 * @return the declaration
	 */
	@Deprecated
	public String getMethodCode()
	{
		String declaration = getTypedProperty(StaticContentSpecLoader.PROPERTY_DECLARATION);
		if (declaration == null)
		{
			return methodCode;
		}

		int functionIndex = declaration.indexOf("function "); //$NON-NLS-1$
		if (functionIndex == -1) return declaration;
		int startBracketIndex = declaration.indexOf('{', functionIndex) + 1;
		// remove the extra \n we put in when serializing
		if (declaration.charAt(startBracketIndex) == '\n')
		{
			startBracketIndex++;
		}
		int endBracketIndex = declaration.lastIndexOf('}');
		// remove the extra \n we put in when serializing
		if (endBracketIndex > startBracketIndex && declaration.charAt(endBracketIndex - 1) == '\n')
		{
			endBracketIndex--;
		}
		return declaration.substring(startBracketIndex, endBracketIndex);
	}

	public String getDisplayName()//can be camelcasing
	{
		return getName();
	}

	public String getDataProviderID()//get the id
	{
		return getName();
	}

	public int getLineNumberOffset()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_LINENUMBEROFFSET).intValue();
	}

	public void setLineNumberOffset(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_LINENUMBEROFFSET, arg);
	}

	/**
	 * @clonedesc com.servoy.base.solutionmodel.IBaseSMMethod#getCode()
	 */
	public String getDeclaration()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DECLARATION);
	}

	/**
	 * Sets the full source code of this method (including doc and function declaration)
	 * @param declaration
	 */
	public void setDeclaration(String declaration)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DECLARATION, declaration);
		methodCode = null;
		isDeprecated = null;
	}

	/**
	 * The name of the scope.
	 */
	public String getScopeName()
	{
		String value = getTypedProperty(StaticContentSpecLoader.PROPERTY_SCOPENAME);
		if (value == null && getParent() instanceof Solution)
		{
			// scope name is a bit strange, has 2 default values: null for non direct solution children (handled through ContentSpec) and "globals" for direct solution children...
			// it needs to be interpreted as "globals" if not available for solution children (for importing solutions from older versions before scopes were introduced - otherwise a "null.js" gets created)
			value = ScriptVariable.GLOBAL_SCOPE;
		}
		return value;
	}

	public void setScopeName(String scopeName)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_SCOPENAME, scopeName);
	}

	public boolean isDeprecated()
	{
		Boolean deprecated = isDeprecated;
		if (deprecated == null)
		{
			String declaration = getDeclaration();
			if (declaration == null)
			{
				deprecated = Boolean.FALSE;
			}
			else
			{
				int index = declaration.indexOf("*/");
				deprecated = Boolean.valueOf(index != -1 && declaration.lastIndexOf("@deprecated", index) != -1);
			}
			isDeprecated = deprecated;
		}
		return deprecated.booleanValue();
	}
}
