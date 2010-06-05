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
public abstract class AbstractScriptProvider extends AbstractBase implements IScriptProvider, ISupportUpdateableName, ISupportContentEquals
{
	private String name = null;
	private int lineNumberOffset;
	private String declaration;
	private String methodCode;

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
		if (name != null) throw new UnsupportedOperationException("Can't set name 2x, use updateName"); //$NON-NLS-1$
		name = arg;
	}

	/**
	 * updates the name
	 * 
	 * @param arg the name
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		validator.checkName(arg, getID(), new ValidatorSearchContext(getParent(), IRepository.METHODS), false);
		checkForNameChange(name, arg);
		if (declaration != null)
		{
			declaration = declaration.replaceFirst("(.*function\\s+)" + name + "(\\s*\\(.*\\)\\s*\\{.*)", "$1" + arg + "$2"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		name = arg;
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * The name of the method.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the declaration
	 * 
	 * @param arg the declaration
	 */
	@Deprecated
	public void setMethodCode(String arg)
	{
		this.methodCode = arg;
	}

	/**
	 * Get the declaration
	 * 
	 * @return the declaration
	 */
	@Deprecated
	public String getMethodCode()
	{
		if (methodCode != null) return methodCode;
		if (declaration != null)
		{
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
		return null;
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
		return lineNumberOffset;
	}

	public void setLineNumberOffset(int arg)
	{
		checkForChange(lineNumberOffset, arg);
		lineNumberOffset = arg;
	}

	/**
	 * The full source code of this method (including doc and function declaration).
	 */
	public String getDeclaration()
	{
		if (methodCode != null && declaration == null)
		{
			declaration = MethodTemplate.getTemplate(getClass(), null).getMethodDeclaration(name, methodCode);
			methodCode = null;
		}
		return declaration;
	}

	/**
	 * Sets the full source code of this method (including doc and function declaration)
	 * @param declaration
	 */
	public void setDeclaration(String declaration)
	{
		checkForChange(this.declaration, declaration);
		this.declaration = declaration;
	}

}
