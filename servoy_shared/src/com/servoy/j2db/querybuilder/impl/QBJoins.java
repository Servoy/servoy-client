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

import static com.servoy.j2db.util.Utils.stream;

import java.util.Map;

import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.query.IJoinConstants;
import com.servoy.j2db.dataprocessing.SQLGenerator;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.DerivedTable;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.TableExpression;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderJoin;
import com.servoy.j2db.querybuilder.IQueryBuilderJoins;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * @author rgansevles
 *
 * @since 6.1
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBJoins extends DefaultJavaScope implements IQueryBuilderJoins
{
	private static Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(QBJoins.class);

	private final QBSelect root;
	private final QBTableClause parent;

	QBJoins(QBSelect root, QBTableClause parent)
	{
		super(root.getScriptableParent(), jsFunctions);
		this.root = root;
		this.parent = parent;

		QuerySelect query = root.getQuery(false);
		if (query != null)
		{
			stream(query.getJoins()) //
				.filter(ISQLTableJoin.class::isInstance).map(ISQLTableJoin.class::cast) //
				.forEach(queryJoin -> allVars.put(queryJoin.getName(),
					new QBJoin(root, parent, queryJoin.getForeignTable().getDataSource(), queryJoin, queryJoin.getName())));
		}
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

	@JSFunction
	public QBJoin[] getJoins()
	{
		return allVars.values().toArray(new QBJoin[allVars.size()]);
	}

	private QBJoin getJoin(String name)
	{
		return (QBJoin)allVars.get(name);
	}


	@Override
	public Object get(String name, Scriptable start)
	{
		Object obj = super.get(name, start);
		if (obj != null && obj != Scriptable.NOT_FOUND)
		{
			return obj;
		}

		if ("parent".equals(name))
		{
			return getParent();
		}
		if ("root".equals(name))
		{
			return getRoot();
		}

		QBJoin join = getOrAddRelation(root.getRelation(name), name, null);
		if (join == null)
		{
			return Scriptable.NOT_FOUND;
		}
		return join;
	}

	@Override
	public QBJoin remove(String key)
	{
		QBJoin removedJoin = (QBJoin)super.remove(key);
		if (removedJoin != null)
		{
			root.getQuery().removeJoin(removedJoin.getJoin());
		}

		return removedJoin;
	}


	@Override
	public void delete(String name)
	{
		QBJoin queryJoin = getJoin(name);
		super.delete(name);
		if (queryJoin != null)
		{
			root.getQuery().removeJoin(queryJoin.getJoin());
		}
	}

	private QBJoin getOrAddRelation(IRelation relation, String relationName, String alias)
	{
		if (relation == null || !parent.getDataSource().equals(relation.getPrimaryDataSource()))
		{
			if (relation == null)
			{
				Debug.log("relation '" + relationName + "' not found");
			}
			else
			{
				Debug.log(
					"relation '" + relationName + "' does not match parent data source: " + parent.getDataSource() + '/' + relation.getPrimaryDataSource());
			}
			return null;
		}

		String name = alias == null ? relationName : alias;
		QBJoin join = getJoin(name);
		if (join == null)
		{
			try
			{
				Table foreignTable = root.getTable(relation.getForeignDataSource());
				if (foreignTable == null)
				{
					Debug.log("foreign table for relation '" + relationName + "' not found");
					return null;
				}
				join = addJoin(SQLGenerator.createJoin(root.getDataProviderHandler(), relation, parent.getQueryTable(),
					new QueryTable(foreignTable.getSQLName(), foreignTable.getDataSource(), foreignTable.getCatalog(), foreignTable.getSchema(), alias), true,
					root.getGlobalScopeProvider()), relation.getForeignDataSource(), name);
			}
			catch (RepositoryException e)
			{
				Debug.error("could not load relation '" + relationName + "'", e);
			}
		}
		return join;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		// ignore
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return "root".equals(name) || "parent".equals(name) || super.has(name, start);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoins#add(String, int)
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
	 *  /** @type {QBJoin<db:/example_data/order_details>} *&#47;
	 * 	var join = query.joins.add('db:/example_data/order_details', QBJoin.INNER_JOIN, 'odetail')
	 * 	join.on.add(join.columns.orderid.eq(query.columns.orderid))
	 *  // to add a join based on a relation, use the relation name
	 *  var join2 = query.joins.add('orders_to_customers', 'cust')
	 * 	query.where.add(join2.columns.customerid.eq(999))
	 * 	foundset.loadRecords(query)
	 *
	 * @param dataSource data source
	 * @param joinType join type, one of QBJoin.LEFT_OUTER_JOIN, QBJoin.INNER_JOIN, QBJoin.RIGHT_OUTER_JOIN, QBJoin.FULL_JOIN
	 */
	@JSFunction
	public QBJoin add(String dataSource, int joinType)
	{
		return add(dataSource, joinType, null);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoins#add(String, String)
	 * @sampleas add(String, int)
	 *
	 * @param dataSourceOrRelation data source
	 * @param alias the alias for joining table
	 */
	@JSFunction
	public QBJoin add(String dataSourceOrRelation, String alias) throws RepositoryException
	{
		IRelation relation = root.getRelation(dataSourceOrRelation);
		if (relation != null)
		{
			return getOrAddRelation(relation, dataSourceOrRelation, alias);
		}
		if (dataSourceOrRelation.indexOf(':') < 0) // all data sources have a colon somewhere
		{
			throw new RepositoryException("Cannot find relation '" + dataSourceOrRelation + "'");
		}
		// a data source
		return add(dataSourceOrRelation, IJoinConstants.LEFT_OUTER_JOIN, alias);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoins#add(String)
	 * @sampleas add(String, int)
	 *
	 * @param dataSource data source
	 */
	@JSFunction
	public QBJoin add(String dataSource)
	{
		return add(dataSource, IJoinConstants.LEFT_OUTER_JOIN, null);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoins#add(IQueryBuilder, int, String)
	 * @sampleas add(QBSelect, int, String)
	 *
	 * @param subqueryBuilder
	 * @param joinType
	 */
	public QBJoin js_add(QBSelect subqueryBuilder, int joinType)
	{
		return add(subqueryBuilder, joinType);
	}

	public QBJoin add(IQueryBuilder subqueryBuilder, int joinType)
	{
		return add(subqueryBuilder, joinType, null);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoins#add(IQueryBuilder, int, String)
	 * @sample
	 * 	var subquery = datasources.db.example_data.products.createSelect();
	 *  subquery.where.add(subquery.columns.supplierid.eq(99));
	 *  subquery.result.add(subquery.columns.categoryid, 'subcat')
	 *  subquery.result.add(subquery.columns.productid, 'subprod')
	 *
	 *  var query = datasources.db.example_data.order_details.createSelect();
	 *  // add a join on a derived table using a subquery
	 *  var join = query.joins.add(subquery, QBJoin.INNER_JOIN, 'subprods');
	 *  join.on.add(query.columns.productid.eq(join.columns['subprod']));
	 *  query.result.add(query.columns.quantity);
	 *  query.result.add(join.columns['subcat']);
	 *
	 * @param subqueryBuilder
	 * @param joinType
	 * @param alias
	 */
	public QBJoin js_add(QBSelect subqueryBuilder, int joinType, String alias)
	{
		return add(subqueryBuilder, joinType, alias);
	}

	public QBJoin add(IQueryBuilder subqueryBuilder, int joinType, String alias)
	{
		if (!DataSourceUtils.isSameServer(getRoot().getDataSource(), subqueryBuilder.getDataSource()))
		{
			throw new RuntimeException(
				"Cannot add derived table join from different server: " + getRoot().getDataSource() + " vs " + subqueryBuilder.getDataSource());
		}

		QuerySelect subquery = ((QBSelect)subqueryBuilder).getQuery();
		QBJoin join = getJoin(alias);
		if (join == null)
		{
			join = addJoin(new QueryJoin(alias, parent.getQueryTable(), new DerivedTable(subquery, alias), new AndCondition(), joinType, true),
				subqueryBuilder.getDataSource(), alias);
		}
		return join;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderJoins#add(String, int, String)
	 * @sampleas add(String, int)
	 *
	 * @param dataSource data source
	 * @param joinType join type, one of {@link IQueryBuilderJoin#LEFT_OUTER_JOIN}, {@link IQueryBuilderJoin#INNER_JOIN}, {@link IQueryBuilderJoin#RIGHT_OUTER_JOIN}, {@link IQueryBuilderJoin#FULL_JOIN}
	 * @param alias the alias for joining table
	 *
	 */
	@JSFunction
	public QBJoin add(String dataSource, int joinType, String alias)
	{
		String name;
		QBJoin join;
		if (alias == null)
		{
			name = UUID.randomUUID().toString();
			join = null;
		}
		else
		{
			name = alias;
			join = getJoin(name);
		}
		if (join == null)
		{
			Table foreignTable = root.getTable(dataSource);
			join = addJoin(new QueryJoin(name, parent.getQueryTable(),
				new TableExpression(
					new QueryTable(foreignTable.getSQLName(), foreignTable.getDataSource(), foreignTable.getCatalog(), foreignTable.getSchema(), alias)),
				new AndCondition(), joinType, true), dataSource, name);
		}
		return join;
	}

	private QBJoin addJoin(ISQLTableJoin queryJoin, String dataSource, String name)
	{
		QBJoin join = new QBJoin(root, parent, dataSource, queryJoin, name);
		root.getQuery().addJoin(queryJoin);
		if (name != null)
		{
			allVars.put(name, join);
		}
		return join;
	}

	/**
	 * @param tableAlias
	 * @return
	 */
	public QBTableClause findQueryBuilderTableClause(String tableAlias)
	{
		QBJoin join = getJoin(tableAlias);
		if (join != null)
		{
			return join;
		}

		// not a direct child, try recursive
		for (QBJoin j : getJoins())
		{
			QBTableClause found = ((QBTableClause)j).findQueryBuilderTableClause(tableAlias);
			if (found != null)
			{
				return found;
			}
		}
		// not found
		return null;
	}

	public ISQLSelect build()
	{
		return getRoot().build();
	}
}
