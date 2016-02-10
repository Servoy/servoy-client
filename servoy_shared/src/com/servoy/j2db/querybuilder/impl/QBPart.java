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
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.IQueryElement;
import com.servoy.j2db.querybuilder.IQueryBuilderPart;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;


/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBPart")
public abstract class QBPart implements IQueryBuilderPart, IJavaScriptType
{
	private final QBSelect root;
	private final QBTableClause parent;

	QBPart()
	{
		this.root = (QBSelect)this;
		this.parent = (QBTableClause)this;
	}

	QBPart(QBSelect root, QBTableClause parent)
	{
		this.root = root;
		this.parent = parent;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderPart#getParent()
	 * @sample
	 * 	var query = datasources.db.example_data.person.createSelect();
	 * 	query.where.add(query.joins.person_to_parent.joins.person_to_parent.columns.name.eq('john'))
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBTableClause getParent()
	{
		return parent;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderPart#getRoot()
	 * @sample
	 * 	var subquery = datasources.db.example_data.order_details.createSelect();
	 *
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 * 	query.where.add(query
	 * 		.or
	 * 			.add(query.columns.order_id.not.isin([1, 2, 3]))
	 *
	 * 			.add(query.exists(
	 * 					subquery.where.add(subquery.columns.orderid.eq(query.columns.order_id)).root
	 * 			))
	 * 		)
	 *
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBSelect getRoot()
	{
		return root;
	}

	public IQueryElement build() throws RepositoryException
	{
		return getRoot().build();
	}

}
