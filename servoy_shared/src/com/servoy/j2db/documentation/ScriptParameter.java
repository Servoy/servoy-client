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

public class ScriptParameter implements IParameter
{
	private final String name;

	// java-script type name; see getter comments
	private final String type;
	private final Class< ? > realType;
	private final String description;
	private final boolean optional;
	private final boolean vararg;

	/**
	 * @param typePrefix TODO this should be removed when the prefix mechanism will be implemented to be generally available.
	 */
	public ScriptParameter(String name, String typePrefix, Class< ? > realType, boolean optional, boolean vararg)
	{
		this(name, typePrefix, realType, null, optional, vararg);
	}

	/**
	 * @param typePrefix TODO this should be removed when the prefix mechanism will be implemented to be generally available.
	 */
	public ScriptParameter(String name, String typePrefix, Class< ? > realType, String description, boolean optional, boolean vararg)
	{
		this.name = name;
		this.realType = realType;
		this.description = description;
		this.optional = optional;
		this.vararg = vararg;

		String translatedType = DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(realType);
		if (translatedType != null)
		{
			if (typePrefix != null)
			{
				type = typePrefix + translatedType;
			}
			else
			{
				type = translatedType;
			}
		}
		else
		{
			type = null;
		}
	}

	public String getName()
	{
		return name;
	}

	public String getType()
	{
		return type;
	}

	public boolean isOptional()
	{
		return optional;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.core.doc.IParameter#isVarArgs()
	 */
	public boolean isVarArgs()
	{
		return vararg;
	}

	public String getDescription()
	{
		return description;
	}

	public Class< ? > getRealType()
	{
		return realType;
	}

}
