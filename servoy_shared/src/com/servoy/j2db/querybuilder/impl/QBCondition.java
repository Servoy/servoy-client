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

package com.servoy.j2db.querybuilder.impl;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;

/**
 * <p>The <code>QBCondition</code> class is a wrapper for conditions used in a <code>QBSelect</code> query.
 * It supports various comparison operations such as equality, ranges, patterns, and inclusion
 * (<code>compare</code>, <code>in</code>, <code>like</code>, <code>between</code>). These conditions
 * help refine query results by enabling precise filtering.</p>
 *
 * <p>The <code>parent</code> property provides access to the query builder's parent table clause,
 * which could be a query or a join clause. The <code>root</code> property allows navigation to the
 * root query builder, facilitating integration with larger query structures.</p>
 *
 * <p>For more information about constructing and executing queries, refer to the
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbselect">QBSelect</a> section of this documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBCondition extends QBPart implements IQueryBuilderCondition
{
	private final ISQLCondition queryCondition;

	QBCondition(QBSelect root, QBTableClause parent, ISQLCondition queryCondition)
	{
		super(root, parent);
		this.queryCondition = queryCondition;
	}

	public ISQLCondition getQueryCondition()
	{
		return queryCondition;
	}
}
