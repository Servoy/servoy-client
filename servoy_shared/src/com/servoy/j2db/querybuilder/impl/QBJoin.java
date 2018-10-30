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

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.ISQLTableJoin;
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
	public BaseQueryTable getQueryTable()
	{
		return join.getForeignTable();
	}

	/**
	 * Returns the join type, one of {@link IQueryBuilderJoin#LEFT_OUTER_JOIN}, {@link IQueryBuilderJoin#INNER_JOIN}, {@link IQueryBuilderJoin#RIGHT_OUTER_JOIN}, {@link IQueryBuilderJoin#FULL_JOIN}
	 *
	 * @return joinType.
	 */
	@JSFunction
	public int getJoinType()
	{
		return join.getJoinType();
	}

	/**
	 * Get the on clause for the join.
	 * @sample
	 * var query = datasources.db.example_data.person.createSelect();
	 * /** @type {QBJoin<db:/example_data/person>} *&#47;
	 * var join1 = query.joins.add('db:/example_data/person')
	 * join1.on.add(query.columns.parent_person_id.eq(join1.columns.person_id))
	 * /** @type {QBJoin<db:/example_data/person>} *&#47;
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

	@Override
	public String toString()
	{
		return join.toString();
	}
}
