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

import static com.servoy.j2db.util.Utils.iterate;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilderGroupby;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBGroupBy</code> class is utilized to define SQL <code>GROUP BY</code> conditions
 * within a <code>QBSelect</code> query. It allows adding columns, functions, or primary key columns
 * to the <code>GROUP BY</code> clause. This class also provides the capability to clear the existing
 * <code>GROUP BY</code> clause, ensuring flexibility in query construction.</p>
 *
 * <p><b>Key Features</b></p>
 * <p>The <code>parent</code> property links the <code>QBGroupBy</code> instance to its parent table
 * clause, which can be a query or a join clause. The <code>root</code> property provides access to
 * the parent query builder, enabling hierarchical query manipulation.</p>
 *
 * <p>The <code>add(column)</code> and <code>add(function)</code> methods enable adding columns or
 * functions to the <code>GROUP BY</code> clause, supporting aggregation queries. The
 * <code>addPk()</code> method simplifies grouping by adding primary key columns in alphabetical
 * order. Additionally, the <code>clear()</code> method removes all conditions from the
 * <code>GROUP BY</code> clause, allowing for dynamic query adjustments.</p>
 *
 * <p>For further details on query construction and execution, refer to the
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbselect">QBSelect</a> section of the documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBGroupBy extends QBPart implements IQueryBuilderGroupby
{
	QBGroupBy(QBSelect parent)
	{
		super(parent, parent);
	}

	@Override
	@JSReadonlyProperty(debuggerRepresentation = "Query parent part")
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	public QBGroupBy add(String columnName) throws RepositoryException
	{
		return add(getParent().getColumn(columnName));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderGroupby#add(QBColumn)
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.groupBy.add(query.columns.orderid) // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)
	 *
	 * @param column the column to add to the query condition
	 *
	 * @return the updated group-by clause with the specified column added.
	 */
	public QBGroupBy js_add(QBColumn column) throws RepositoryException
	{
		return add(column);
	}

	public QBGroupBy add(QBColumn column) throws RepositoryException
	{
		if (column == null)
		{
			throw new RuntimeException("Cannot add null or undefined column to a group-by clause");
		}
		getParent().getQuery().addGroupBy(((QBColumnImpl)column).getQuerySelectValue());
		return this;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderGroupby#addPk()
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)
	 *
	 * @return the updated group-by clause with the primary key(s) of the table added.
	 */
	@JSFunction
	public QBGroupBy addPk() throws RepositoryException
	{
		ITable table = getParent().getTable();
		if (table != null)
		{
			for (String columnName : iterate(table.getRowIdentColumnNames()))
			{
				add(columnName);
			}
		}
		return this;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderGroupby#clear()
	 * @sample
	 * var q = foundset.getQuery()
	 * q.where.add(q.columns.x.eq(100))
	 * query.groupBy.clear.root.clearHaving()
	 * foundset.loadRecords(q);
	 *
	 * @return the cleared group-by clause.
	 */
	@JSFunction
	public QBGroupBy clear()
	{
		getParent().getQuery().clearGroupBy();
		return this;
	}

	@Override
	public String toString()
	{
		return "QBGroupBy(Helper class for creating group by clauses)";
	}
}
