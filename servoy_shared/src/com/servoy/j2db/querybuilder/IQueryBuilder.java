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


/**
 * Interface for building Servoy Query Objects.
 *
 * <p>Simple example:
 * <pre>
 * 		// select order_date from ordes where custid = 100
 * 		IQueryBuilder query = pluginAccess.getDatabaseManager().getQueryFactory().createSelect(dataSource);
 * 		query.result().add("order_date").getParent().where().add(query.getColumn("custid").eq(new Integer(99)));
 * 	</pre>
 * 
 * <p>Example with join:
 * <pre>
 * 		// select pk from main
 * 		// join detail on detail.fk = main.mainid
 * 		// where detail.value <= 88
 * 		// and detail.value between 7 and 88
 * 		IQueryBuilder query = pluginAccess.getDatabaseManager().getQueryFactory().createPKSelect(mainDataSource);
 * 		query.joins().add(detailDataSource, IQueryBuilderJoin.LEFT_OUTER_JOIN, "detail")
 * 		   .on()
 * 		      .add(query.getColumn("detail", "fk").eq(query.getColumn("mainid")))
 * 		   .getRoot().where()
 * 		      .add(query.getColumn("detail", "value").le(new Integer(88)))
 * 		      .add(query.getColumn("detail", "value").between(new Integer(7), new Integer(88)));
 * 	</pre>
 * 
 * <p>Example with subquery
 * <pre>
 * 		// select pk
 * 		// from table
 * 		// where custid = 200
 * 		// and (order_date is null or order_date > now)
 * 		// and action_code in (
 * 		// 	select action_code from table where valid != 1
 * 		// )
 * 		IQueryBuilder subQuery = pluginAccess.getDatabaseManager().getQueryFactory().createSelect(table.getDataSource());
 * 		IQueryBuilder query = pluginAccess.getDatabaseManager().getQueryFactory().createPKSelect(table.getDataSource());
 * 		query.where().add(query.getColumn("custid").eq(new Integer(200)))
 * 		   .add(
 * 		     query.or()
 * 		       .add(query.getColumn("order_date").isNull())
 * 		       .add(query.getColumn("order_date").gt(new Date()))
 * 		   )
 * 		   .add(query.getColumn("action_code").in(
 * 		     subQuery.result().add(subQuery.getColumn("action_code")).getParent()
 * 		     .where().add(subQuery.getColumn("valid").not().eq(new Integer(1)))
 * 		   .getRoot()
 * 		   )
 * 		   );
 * 	</pre>
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface IQueryBuilder extends IQueryBuilderTableClause
{
	IQueryBuilderLogicalCondition where();

	IQueryBuilderResult result();

	IQueryBuilderLogicalCondition or();

	IQueryBuilderCondition not(IQueryBuilderCondition cond);
}
