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

import java.util.HashMap;
import java.util.Map;

import org.sablo.specification.PropertyDescription;

/**
 * Class responsible for creating {@link IPropertyType} instances and attaching custom behavior to them.<BR>
 * A property type can be for example defined in JSON spec files only or could have 'complex' behavior - behaving differently
 * in different stages (server-side/client-side/...).
 *   
 * @author acostescu
 */
public class CustomPropertyTypeResolver
{

	private static final CustomPropertyTypeResolver INSTANCE = new CustomPropertyTypeResolver();

	public static final CustomPropertyTypeResolver getInstance()
	{
		return INSTANCE;
	}

	private final Map<String, IComplexTypeImpl< ? , ? >> availableComplexTypes = new HashMap<>();
	private final Map<String, CustomPropertyType> cache = new HashMap<>();

	/**
	 * Registers a new complex type with the type system.
	 * @param typeName the type's name as used in .spec files.
	 * @param typeDescription the implementation of this complex custom type.
	 */
	public void registerNewComplexType(String typeName, IComplexTypeImpl< ? , ? > typeDescription)
	{
		availableComplexTypes.put(typeName, typeDescription);
	}

	/**
	 * Checks is a type is already registered.
	 */
	public boolean hasTypeName(String string)
	{
		return availableComplexTypes.containsKey(string);
	}

	/**
	 * This method resolves based on typeName the defined custom types - be it pure JSON defined types or complex types, allowing
	 * complex and custom types to be contributed to the system.
	 * @param typeName the type name as used in .spec files
	 * @return the appropriate property type (handler).
	 */
	// TODO should this name be a more specific ID - to avoid clashes between component packages, or is the typeName enough?
	public CustomPropertyType resolveCustomPropertyType(String typeName)
	{
		CustomPropertyType propertyType = cache.get(typeName);
		if (propertyType == null)
		{
			// currently typeName can resolve to a pure JSON handled all-over-the-place property type or
			// a special type that has JSON defined spec, but also it behaves differently in different stages - see 'components' type
			IComplexTypeImpl< ? , ? > complexTypeImplementation = availableComplexTypes.get(typeName);

			if (complexTypeImplementation != null)
			{
				propertyType = new ComplexCustomPropertyType(typeName, null, complexTypeImplementation); // that null is temporary - it will get populated later by the parser
				propertyType.setDefinition(new PropertyDescription(typeName, propertyType));
			}
			else
			{
				propertyType = new CustomPropertyType(typeName, null); // that null is temporary - it will get populated later by the parser
				propertyType.setDefinition(new PropertyDescription(typeName, propertyType));
			}

			cache.put(typeName, propertyType);
		}
		return propertyType;
	}

}
