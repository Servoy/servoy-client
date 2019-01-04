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

import java.util.Iterator;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderGroupby;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
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
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	public QBGroupBy add(String columnName) throws RepositoryException
	{
		return add(getParent().getColumn(columnName));
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderGroupby#add(IQueryBuilderColumn)
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.groupBy.add(query.columns.orderid) // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)
	 *
	 * @param column the column to add to the query condition
	 */
	public QBGroupBy js_add(QBColumn column) throws RepositoryException
	{
		return add(column);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderGroupby#add(IQueryBuilderColumn)
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.groupBy.add(query.columns.orderid) // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)
	 *
	 * @param function the function to add to the query
	 */
	public QBGroupBy js_add(QBFunction function) throws RepositoryException
	{
		return add(function);
	}

	public QBGroupBy add(IQueryBuilderColumn column) throws RepositoryException
	{
		if (column == null)
		{
			throw new RuntimeException("Cannot add null or undefined column to a group-by clause");
		}
		getParent().getQuery().addGroupBy(((QBColumn)column).getQuerySelectValue());
		return this;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderGroupby#addPk()
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)
	 */
	@JSFunction
	public QBGroupBy addPk() throws RepositoryException
	{
		Iterator<String> rowIdentColumnNames = getParent().getTable().getRowIdentColumnNames();
		while (rowIdentColumnNames.hasNext())
		{
			add(rowIdentColumnNames.next());
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
	 */
	@JSFunction
	public QBGroupBy clear()
	{
		getParent().getQuery().clearGroupBy();
		return this;
	}
}
