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
package com.servoy.j2db.dataui;

/**
 * Options for {@link PropertyEditorHint} hints.
 * 
 * @author rob
 *
 * @since 5.0
 */
public enum PropertyEditorOption
{
	/**
	 * Boolean option to include none in the available values. default: Boolean.TRUE
	 */
	includeNone,
	/**
	 * Boolean option to include form local data. (e.g. form methods, form variables) in the available values, default: Boolean.TRUE
	 */
	includeForm,
	/**
	 * Boolean option to include global data. (e.g. global methods, global variables) in the available values, default: Boolean.TRUE
	 */
	includeGlobal,
	/**
	 * Boolean option to include columns in the available values. default: Boolean.TRUE
	 */
	includeColumns,
	/**
	 * Boolean option to include calculations in the available values. default: Boolean.TRUE
	 */
	includeCalculations,
	/**
	 * Boolean option to include related calculations in the available values. default: Boolean.TRUE
	 */
	includeRelatedCalculations,
	/**
	 * Boolean option to include aggregates in the available values. default: Boolean.TRUE
	 */
	includeAggregates,
	/**
	 * Boolean option to include related aggregates in the available values. default: Boolean.TRUE
	 */
	includeRelatedAggregates,
	/**
	 * Boolean option to include related dataproviders in the available values. default: Boolean.TRUE
	 */
	includeRelations,
	/**
	 * Boolean option to include nested relations or nested related dataproviders. (e.g. a_to_b.b_to_c.c_field) in the available values, default: Boolean.TRUE
	 */
	includeNestedRelations,
	/**
	 * String setting for lookupName in {@link PropertyEditorClass#styleclass} property selection.
	 */
	styleLookupName
}