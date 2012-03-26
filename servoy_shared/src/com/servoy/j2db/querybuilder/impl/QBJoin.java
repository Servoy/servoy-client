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
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.querybuilder.IQueryBuilderJoin;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBJoin extends QBTableClause implements IQueryBuilderJoin
{
	private final ISQLTableJoin join;

	private QBLogicalCondition on;

	QBJoin(QBSelect root, QBTableClause parent, String dataSource, ISQLTableJoin join, String alias)
	{
		super(root, parent, dataSource, alias);
		this.join = join;
	}

	@Override
	QueryTable getQueryTable() throws RepositoryException
	{
		return join.getForeignTable();
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoin#on()
	 * @sample
	 * var query = databaseManager.createSelect('db:/example_data/person')
	 * var join1 = query.joins.add('db:/example_data/person')
	 * join1.on.add(query.columns.parent_person_id.eq(join1.columns.person_id))
	 * var join2 = query.joins.add('db:/example_data/person')
	 * join2.on.add(join1.columns.parent_person_id.eq(join2.columns.person_id))
	 * 	
	 * query.where.add(join2.columns.name.eq('john'))
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBLogicalCondition on()
	{
		if (on == null)
		{
			on = new QBLogicalCondition(getRoot(), this, join.getCondition());
		}
		return on;
	}
}
