/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.querybuilder;


/**
 * Logical condirion (AND or OR clause) for building Servoy Query Objects.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderLogicalCondition extends IQueryBuilderCondition
{
	/**
	 * Add a condition to the AND or OR condition list.
	 * <pre>
	 * query.where().add(query.getColumn("flag").eq(new Integer(1))).add(query.getColumn("value").isNull());
	 * </pre>
	 */
	IQueryBuilderLogicalCondition add(IQueryBuilderCondition condition);

}
