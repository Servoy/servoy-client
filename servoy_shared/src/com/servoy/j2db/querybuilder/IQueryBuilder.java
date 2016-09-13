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

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ISQLSelect;


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
	/**
	 * Get the where-part of the query, used to add conditions.
	 * The conditions added here are AND-ed.
	 * <pre>
	 * query.where().add(query.getColumn("flag").eq(new Integer(1)).add(query.getColumn("foo").isNull(); // where flag = 1 and foo is null
	 * </pre>
	 */
	IQueryBuilderLogicalCondition where() throws RepositoryException;

	/**
	 * Get the having-part of the query, used to add conditions.
	 * The conditions added here are AND-ed.
	 * <pre>
	 * // select value from tab group by value having count(value) > 1
	 * query.result().add(query.getColumn("value"))
	 *    .getParent().groupBy().add("value")
	 *    .getParent().having().add("value").count().gt(Integer.valueOf(1)));
	 * </pre>
	 */
	IQueryBuilderLogicalCondition having() throws RepositoryException;

	/**
	 * Clear the having-part of the query.
	 */
	IQueryBuilder clearHaving();

	/**
	 * Get the result part of the query, used to add result columns or values.
	 * <pre>
	 * query.result().add(query.getColumn("id")).add.query.getColumn("note"); // select id, note from tab
	 * </pre>
	 */
	IQueryBuilderResult result();

	/**
	 * Get the sorting part of the query.
	 * <pre>
	 * query.sort().add(query.getColumn("note").desc()).add(query.getColumn("id")); // order by note desc, id asc
	 * </pre>
	 * @see IQueryBuilderColumn#asc()
	 */
	IQueryBuilderSorts sort();

	/**
	 * Get the group by clause from a query
	 * <pre>
	 * // SELECT val2, COUNT(val2) FROM tab GROUP BY val2 ORDER BY COUNT(val2) DESC
	 * query.result().add(query.getColumn(val2.getName())).add(query.getColumn(val2.getName()).count())
	 *   .getParent().groupBy().add(val2.getName())
	 *   .getParent().sort().add(query.getColumn(val2.getName()).count().desc());
	 * </pre>
	 */
	IQueryBuilderGroupby groupBy();

	/**
	 * Get or create a parameter for the query, this used to parameterize queries.
	 * <pre>
	 * query.where().add(query.getColumn("flag").eq(query.getParameter("myvar"));
	 * query.getParameter("myvar").setvalue(new Integer(1));
	 * </pre>
	 *
	 * @param name The name of the parameter
	 */
	IQueryBuilderParameter getParameter(String name) throws RepositoryException;

	/**
	 * Create an OR-condition to add conditions to.
	 * <pre>
	 * // where custid = ? and (order_date is null or order_date > ?)
	 * query.where().add(query.getColumn("custid").eq(new Integer(200)))
	 *     .add(
	 *       query.or()
	 *          .add(query.getColumn("order_date").isNull())
	 *          .add(query.getColumn("order_date").gt(new Date()))
	 *     );
	 * </pre>
	 */
	IQueryBuilderLogicalCondition or();

	/**
	 * Create an AND-condition to add conditions to.
	 * <pre>
	 * // where (flag = ? and order_date is null) or (flag = ? and order_date > ?)
	 *	query.where().add(
	 *	  query.or()
	 *	    .add(
	 *	      query.and()
	 *		    .add(query.getColumn("flag").eq(new Integer(1)))
	 *		    .add(query.getColumn("order_date").isNull())
	 *		 )
	 *	    .add(
	 *	      query.and()
	 *	        .add(query.getColumn("flag").eq(new Integer(2)))
	 *	        .add(query.getColumn("order_date").gt(new Date()))
	 *	     )
	 *	);
	 * </pre>
	 */
	IQueryBuilderLogicalCondition and();

	/**
	 * Create an negated condition.
	 * <pre>
	 * // where not (order_date is null or order_date > ?)
	 *   query.where().add(query.not(
	 *	       query.or()
	 *	          .add(query.getColumn("order_date").isNull())
	 *	          .add(query.getColumn("order_date").gt(new Date()))
	 *	     )
	 *	   );
	 * </pre>
	 */
	IQueryBuilderCondition not(IQueryBuilderCondition cond);

	/**
	 * Get an exists-condition from a subquery
	 * <pre>
	 *  // where exists (select 1 from tab where flag = ?)
	 *  query.where().add(query.exists(subQuery.result().addValue(new Integer(1)).getParent().where().add(subQuery.getColumn("flag").eq("T")).getRoot()));
	 *
	 *  // or simple variant: adds 'select 1' and calls getRoot()
	 *  query.where().add(query.exists(subQuery.where().add(subQuery.getColumn("flag").eq("T"))));
	 * </pre>
	 *
	 * @param query
	 */
	IQueryBuilderCondition exists(IQueryBuilder query) throws RepositoryException;

	/**
	 * Get the functions clause from a query, used for functions that are not tied to a column.
	 * <pre>
	 * // select pk from tab where floor(val / ?) > pk [1999]
	 * IQueryBuilder query = queryFactory.createSelect(table.getDataSource()).result().addPk().getParent();
	 * query.where()
	 *  .add(query.functions().floor(query.getColumn(val.getName()).divide(new Integer(1999))).gt(query.getColumn(id.getName())))
	 *  .getRoot().sort().add(query.getColumn(id.getName()).asc());
	 * </pre>
	 */
	IQueryBuilderFunctions functions();

	/**
	 * Build the query for performing query in the db
	 */
	ISQLSelect build() throws RepositoryException;
}
