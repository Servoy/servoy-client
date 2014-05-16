/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package org.sablo.specification.property;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyType;

/**
 * Property types that are defined in JSON spec files.
 * @author acostescu
 */
public class CustomPropertyType extends PropertyType
{

	private PropertyDescription definition;

	/**
	 * Creates a new property types that is defined in JSON spec files.
	 * @param typeName the name of this type as used in spec files.
	 * @param definition the parsed JSON definition of this type. If null, it must be set later via {@link #setDefinition(PropertyDescription)}.
	 */
	public CustomPropertyType(String typeName, PropertyDescription definition)
	{
		super(typeName);
		setDefinition(definition);
	}

	void setDefinition(PropertyDescription definition)
	{
		this.definition = definition;
	}

	/**
	 * Returns the parsed JSON definition of this type.
	 * @return the parsed JSON definition of this type.
	 */
	@Override
	public PropertyDescription getCustomJSONTypeDefinition()
	{
		return definition;
	}

	@Override
	public String toString()
	{
		return super.toString() + "\nDefinition JSON:\n" + (definition == null ? "null" : definition.toStringWholeTree()); //$NON-NLS-1$//$NON-NLS-2$
	}

}
