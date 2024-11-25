/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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
import com.servoy.j2db.query.QuerySearchedCaseExpression;
import com.servoy.j2db.querybuilder.IQueryBuilderSearchCaseExpression;

/**
 * <p>The <code>QBSearchedCaseExpression</code> class provides functionality for constructing SQL
 * case expressions with dynamic branching logic. It supports the definition of complex conditional
 * logic directly within queries, enabling nuanced data manipulation and evaluation.</p>
 *
 * <p>This class allows for a wide range of operations on columns, including mathematical functions
 * (e.g., <code>abs</code>, <code>ceil</code>, <code>sqrt</code>), string manipulations
 * (e.g., <code>upper</code>, <code>trim</code>, <code>concat</code>), and date extractions
 * (e.g., <code>year</code>, <code>month</code>, <code>day</code>). Additionally, it facilitates
 * comparisons, aggregate operations (e.g., <code>sum</code>, <code>avg</code>), and casting of
 * column types. These features make it a versatile tool for building expressive and dynamic queries.</p>
 *
 * <p>For further details, refer to the
 * <a href="./qbselect.md">QBSelect documentation</a>.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBSearchedCaseExpression")
public class QBSearchedCaseExpression extends QBColumn implements IQueryBuilderSearchCaseExpression
{
	QBSearchedCaseExpression(QBSelect root, QBTableClause queryBuilderTableClause, QuerySearchedCaseExpression querySearchedCaseExpression)
	{
		super(root, queryBuilderTableClause, querySearchedCaseExpression);
	}
}
