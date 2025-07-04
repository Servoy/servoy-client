/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBCountAggregate</code> class represents aggregate expressions, such as <code>count</code>,
 * <code>min</code>, and <code>max</code>, within a <code>QBSelect</code> query. It provides a range
 * of methods and properties for constructing and manipulating aggregate expressions, allowing for
 * customization of query results and conditions.</p>
 *
 * <p>Aggregates can be used to calculate summaries, apply mathematical functions, or manipulate
 * data within a query. Properties like <code>count</code>, <code>sum</code>, and <code>avg</code>
 * allow direct creation of aggregate expressions, while others like <code>abs</code> or <code>round</code>
 * perform mathematical operations. Sorting and conditional operations can also be applied using
 * properties such as <code>asc</code>, <code>desc</code>, and <code>isNull</code>.</p>
 *
 * <p>Methods extend the functionality of aggregates by enabling operations like comparison
 * (<code>eq</code>, <code>between</code>, <code>like</code>), mathematical manipulation
 * (<code>plus</code>, <code>minus</code>, <code>mod</code>), and substring extraction
 * (<code>substring</code>). These features provide fine-grained control over how aggregate data
 * is computed and retrieved.</p>
 *
 * <p>The <code>QBCountAggregate</code> class integrates with query builder constructs like
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbcolumn">QBColumn</a>, <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbcondition">QBCondition</a>, and
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbsort">QBSort</a>, enabling complex query logic and data transformations.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, extendsComponent = "QBIntegerColumn")
public interface QBCountAggregate extends IQueryBuilderColumn
{

	/** Add a distinct qualifier to the aggregate
	 * @sample
	 * // count the number of countries that we ship orders to
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.columns.shipcountry.count.distinct);
	 */
	@JSReadonlyProperty(debuggerRepresentation = "Query distinct clause")
	default QBIntegerColumnBase distinct()
	{
		return ((QBAggregateImpl)this).countDistinct();
	}
}
