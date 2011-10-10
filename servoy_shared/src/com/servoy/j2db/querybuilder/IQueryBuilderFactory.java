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
 * Factory for creating Servoy Query Objects builders.
 * <pre>
 * IQueryBuilder query = pluginAccess.getDatabaseManager().getQueryFactory().createSelect(dataSource);
 * </pre>
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface IQueryBuilderFactory
{
	/**
	 * Create a query builder for a data source.
	 */
	IQueryBuilder createSelect(String dataSource) throws RepositoryException;

	/**
	 * Create a query builder for a data source.
	 * <p>The main table will have specified alias.
	 */
	IQueryBuilder createSelect(String dataSource, String alias) throws RepositoryException;
}
