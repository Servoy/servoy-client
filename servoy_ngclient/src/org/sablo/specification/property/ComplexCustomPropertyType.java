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

/**
 * Represents a property type that is both defined in JSON spec file and has special server-side/client-side handling.
 * 
 * @author acostescu
 */
public class ComplexCustomPropertyType extends CustomPropertyType
{

	protected final IComplexTypeImpl< ? , ? > handlers;

	// just caches
	protected IPropertyConfigurationParser< ? > pcp;
	protected IJSONToJavaPropertyConverter< ? , ? >[] js2j = new IJSONToJavaPropertyConverter< ? , ? >[2];
	protected IDesignJSONToJavaPropertyConverter< ? , ? >[] d2j = new IDesignJSONToJavaPropertyConverter< ? , ? >[2];
	protected IServerObjToJavaPropertyConverter< ? , ? >[] s2j = new IServerObjToJavaPropertyConverter< ? , ? >[2];

	/**
	 * Creates a new property type the is both defined in JSON spec file and has special server-side/client-side handling.
	 * @param typeName see super.
	 * @param definition see super.
	 */
	public ComplexCustomPropertyType(String typeName, PropertyDescription definition, IComplexTypeImpl< ? , ? > handlers)
	{
		super(typeName, definition);
		this.handlers = handlers;
	}

	// TODO does it help to add generics here as well or to drop existing ones?
	@Override
	public IPropertyConfigurationParser< ? > getPropertyConfigurationParser()
	{
		return pcp != null ? pcp : (pcp = handlers.getPropertyConfigurationParser());
	}

	@Override
	public IJSONToJavaPropertyConverter< ? , ? > getJSONToJavaPropertyConverter(boolean isArray)
	{
		int tmp = isArray ? 0 : 1;
		return js2j[tmp] != null ? js2j[tmp] : (js2j[tmp] = handlers.getJSONToJavaPropertyConverter(isArray));
	}

	@Override
	public IDesignJSONToJavaPropertyConverter< ? , ? > getDesignJSONToJavaPropertyConverter(boolean isArray)
	{
		int tmp = isArray ? 0 : 1;
		return d2j[tmp] != null ? d2j[tmp] : (d2j[tmp] = handlers.getDesignJSONToJavaPropertyConverter(isArray));
	}

	@Override
	public IServerObjToJavaPropertyConverter< ? , ? > getServerObjectToJavaPropertyConverter(boolean isArray)
	{
		int tmp = isArray ? 0 : 1;
		return s2j[tmp] != null ? s2j[tmp] : (s2j[tmp] = handlers.getServerObjectToJavaPropertyConverter(isArray));
	}

	@Override
	public String toString()
	{
		return "COMPLEX Type: " + super.toString(); //$NON-NLS-1$
	}

}
