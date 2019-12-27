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
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.querybuilder.IQueryBuilderAggregate;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBAggregate")
public class QBAggregate extends QBColumn implements IQueryBuilderAggregate
{
	private final int aggregateType;
	private final int aggregateQuantifier;

	QBAggregate(QBSelect root, QBTableClause queryBuilderTableClause, IQuerySelectValue queryColumn, int aggregateType, int aggregateQuantifier)
	{
		super(root, queryBuilderTableClause, queryColumn);
		this.aggregateType = aggregateType;
		this.aggregateQuantifier = aggregateQuantifier;
	}

	@Override
	public IQuerySelectValue getQuerySelectValue()
	{
		return new QueryAggregate(aggregateType, aggregateQuantifier, getQueryColumn(), null, null, false);
	}

	/** Add a distinct qualifier to the aggregate
	 * @sample
	 * // count the number of countries that we ship orders to
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.columns.shipcountry.count.distinct);
	 */
	@JSReadonlyProperty
	public QBAggregate distinct()
	{
		return new QBAggregate(getRoot(), getParent(), getQueryColumn(), aggregateType, QueryAggregate.DISTINCT);
	}

}
