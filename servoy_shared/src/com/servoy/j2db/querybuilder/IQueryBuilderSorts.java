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

import com.servoy.j2db.persistence.RepositoryException;

/**
 * The sorts (ordering) section of Servoy Query Objects.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderSorts extends IQueryBuilderPart
{
	/**
	 * Add the tables' primary pk columns in alphabetical order to the query sort.
	 */
	IQueryBuilderSorts addPk() throws RepositoryException;

	/**
	 * Ad a sorting on a column to the query sort.
	 * @see IQueryBuilderColumn#asc()
	 * @see IQueryBuilderColumn#desc()
	 */
	IQueryBuilderSorts add(IQueryBuilderSort columnSort) throws RepositoryException;

	/**
	 * Ad an ascending sorting on a column to the query sort.
	 */
	IQueryBuilderSorts add(IQueryBuilderColumn columnSortAsc) throws RepositoryException;
}
