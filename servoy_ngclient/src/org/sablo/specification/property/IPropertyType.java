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
 * This class represents property types - which can have normal usage as declared in their type definition JSON,
 * can be default types or can have special handling (in case of complex properties, keeping different structures of data at design time/server side/client side).
 * 
 * @author acostescu
 */
public interface IPropertyType extends IComplexTypeImpl
{

	/**
	 * The type name as used in .spec files.
	 * @return the type name as used in .spec files.
	 */
	String getName();

	/**
	 * Can be null; if it's not null then this type was defined as a custom type in JSON. It could also have special handling attached to it,
	 * depending on what other method return.
	 * @return the corresponding (JSON based) representation of this type's definition.
	 */
	PropertyDescription getCustomJSONTypeDefinition();

	// TODO I think this method and following enum can be removed if we manage to move all default Types to special
	// implementations of this interface and avoid switch statements. 
	/**
	 * Gets the default type enum value if present.
	 * @return for defaults that use switch statements, have an enum value returned here. Otherwise return null.
	 */
	Default getDefaultEnumValue();

	// declare default types for switch statements:
	public enum Default
	{
		// @formatter:off
		color,
		string,
		tagstring,
		point,
		dimension,
		insets,
		font,
		border,
		bool("boolean"),
		scrollbars,
		bytenumber("byte"),
		doublenumber("double"),
		floatnumber("float"),
		intnumber("int"),
		longnumber("long"),
		shortnumber("short"),
		values,
		dataprovider,
		valuelist,
		function,
		form,
		formscope,
		format,
		relation,
		tabseq,
		media,
		mediaoptions,
		styleclass,
		object,
		bean,
		componentDef,
		customDoNotTreatThisInSwitches,
		date; // can be used in api calls
		// @formatter:on

		private final String alias;
		private IPropertyType type = null;

		private Default(String alias)
		{
			this.alias = alias;
		}

		private Default()
		{
			this(null);
		}

		public String getAlias()
		{
			return alias != null ? alias : name();
		}

		public IPropertyType getType()
		{
			if (type == null)
			{
				type = new PropertyType(getAlias(), this);
			}
			return type;
		}

	}

}
