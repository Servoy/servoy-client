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

import java.sql.Timestamp;
import java.util.Date;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableAndRelationProvider;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.ExistsCondition;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBSelect extends QBTableClause implements IQueryBuilder
{
	private static final String CONDITION_WHERE = "SQ:WHERE";

	private final ITableAndRelationProvider tableProvider;

	private QBResult result;
	private QBSorts sort;
	private QBGroupBy groupBy;
	protected QuerySelect query;
	private QBLogicalCondition where;
	private QBLogicalCondition having;
	private QueryTable queryTable;

	private QBParameters params;

	private Scriptable scriptableParent;

	private final IGlobalValueEntry globalScopeProvider;

	private final IDataProviderHandler dataProviderHandler;

	QBSelect(ITableAndRelationProvider tableProvider, IGlobalValueEntry globalScopeProvider, IDataProviderHandler dataProviderHandler, String dataSource,
		String alias)
	{
		super(dataSource, alias);
		this.tableProvider = tableProvider;
		this.globalScopeProvider = globalScopeProvider;
		this.dataProviderHandler = dataProviderHandler;
	}

	/**
	 * @param querySelect
	 */
	public QBSelect(ITableAndRelationProvider tableProvider, IGlobalValueEntry globalScopeProvider, IDataProviderHandler dataProviderHandler,
		String dataSource, QuerySelect querySelect)
	{
		super(dataSource, null);
		this.tableProvider = tableProvider;
		this.globalScopeProvider = globalScopeProvider;
		this.dataProviderHandler = dataProviderHandler;
		this.query = querySelect;
	}

	public QuerySelect build() throws RepositoryException
	{
		return AbstractBaseQuery.deepClone(getQuery());
	}

	/**
	 * @param scriptableParent
	 */
	public void setScriptableParent(Scriptable scriptableParent)
	{
		this.scriptableParent = scriptableParent;
	}

	/**
	 * @return the scriptableParent
	 */
	Scriptable getScriptableParent()
	{
		return scriptableParent;
	}

	Table getTable(String dataSource) throws RepositoryException
	{
		if (dataSource == null)
		{
			throw new RepositoryException("Cannot access table in query without dataSource");
		}
		ITable tbl = tableProvider.getTable(dataSource);
		if (!(tbl instanceof Table))
		{
			throw new RepositoryException("Cannot resolve datasource '" + dataSource + "'");
		}
		return (Table)tbl;
	}

	IRelation getRelation(String name)
	{
		return tableProvider.getRelation(name);
	}

	IGlobalValueEntry getGlobalScopeProvider()
	{
		return globalScopeProvider;
	}

	IDataProviderHandler getDataProviderHandler()
	{
		return dataProviderHandler;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#where()
	 * @sample
	 * var query = foundset.getQuery()
	 * query.where.add(query.columns.flag.eq(1))
	 */
	@JSReadonlyProperty
	public QBLogicalCondition where() throws RepositoryException
	{
		if (where == null)
		{
			AndCondition c = getQuery().getCondition(QBSelect.CONDITION_WHERE);
			if (c == null)
			{
				getQuery().setCondition(QBSelect.CONDITION_WHERE, c = new AndCondition());
			}
			where = new QBLogicalCondition(this, this, c);
		}
		return where;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#having()
	 * @sample
	 * var query = databaseManager.createSelect('db:/example_data/orders')
	 * query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)	 
	 */
	@JSReadonlyProperty
	public QBLogicalCondition having() throws RepositoryException
	{
		if (having == null)
		{
			ISQLCondition c = getQuery().getHaving();
			if (!(c instanceof AndOrCondition))
			{
				getQuery().setHaving(null, c = AndCondition.and(c, new AndCondition()));
			}
			where = new QBLogicalCondition(this, this, (AndOrCondition)c);
		}
		return where;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#result()
	 * @sample
	 * query.result.add(query.columns.company_id).add(query.columns.customerid)
	 */
	@JSReadonlyProperty
	public QBResult result()
	{
		if (result == null)
		{
			result = new QBResult(this);
		}
		return result;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#sort()
	 * @sample
	 * var query = databaseManager.createSelect('db:/example_data/orders')
	 * query.sort
	 * .add(query.joins.orders_to_order_details.columns.quantity.desc)
	 * .add(query.columns.companyid)
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBSorts sort()
	{
		if (sort == null)
		{
			sort = new QBSorts(this);
		}
		return sort;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#groupBy()
	 * @sample
	 * var query = databaseManager.createSelect('db:/example_data/orders')
	 * query.groupBy.addPk() // have to group by on pk when using having-conditions in (foundset) pk queries
	 * .root.having.add(query.joins.orders_to_order_details.columns.quantity.count.eq(0))
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBGroupBy groupBy()
	{
		if (groupBy == null)
		{
			groupBy = new QBGroupBy(this);
		}
		return groupBy;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#params()
	 * @sample
	 * 	var query = databaseManager.createSelect('db:/example_data/orders')
	 * 	query.where.add(query.columns.contact_id.eq(query.getParameter('mycontactid')))
	 * 	
	 * 	// load orders where contact_id = 100
	 * 	query.params['mycontactid'] = 100
	 * 	foundset.loadRecords(query)
	 * 	
	 * 	// load orders where contact_id = 200
	 * 	query.params['mycontactid'] = 200
	 * 	foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBParameters params()
	{
		if (params == null)
		{
			params = new QBParameters(getScriptableParent(), this);
		}
		return params;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#getParameter(String)
	 * @sampleas params()
	 */
	@JSFunction
	public QBParameter getParameter(String name) throws RepositoryException
	{
		return params().getParameter(name);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#or()
	 * @sampleas and()
	 */
	@JSReadonlyProperty
	public QBLogicalCondition or()
	{
		return new QBLogicalCondition(getRoot(), this, new OrCondition());
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#and()
	 * @sample
	 * query.where.add(
	 *	  query.or
	 *	    .add(
	 *	      query.and
	 *		    .add(query.columns.flag.eq(1))
	 *	    .add(query.columns.order_date.isNull)
	 *		 )
	 *	    .add(
	 *	      query.and
	 *	        .add(query.columns.flag.eq(2))
	 *	        .add(query.column.order_date.gt(new Date()))
	 *	     )
	 *	);	
	 */
	@JSReadonlyProperty
	public QBLogicalCondition and()
	{
		return new QBLogicalCondition(getRoot(), this, new AndCondition());
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#not(IQueryBuilderLogicalCondition)
	 * @sample
	 * foundset.query.where.add(query.not(query.columns.flag.eq(1)))
	 */
	@JSFunction
	public QBCondition not(IQueryBuilderLogicalCondition cond)
	{
		return new QBCondition(getRoot(), (QBTableClause)cond.getParent(), ((QBLogicalCondition)cond).getQueryCondition().negate());
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#not(IQueryBuilderCondition)
	 * @sample
	 * foundset.query.where.add(query.not(query.columns.flag.eq(1)))
	 */
	@JSFunction
	public QBCondition not(IQueryBuilderCondition cond)
	{
		return new QBCondition(this, ((QBCondition)cond).getParent(), ((QBCondition)cond).getQueryCondition().negate());
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilder#exists(IQueryBuilder)
	 * @sample
	 * foundset.query.where.add(query.exists(query2))
	 */
	public QBCondition js_exists(QBSelect q) throws RepositoryException
	{
		return exists(q);
	}

	public QBCondition exists(IQueryBuilder q) throws RepositoryException
	{
		ISQLSelect select = ((QBSelect)q).build();
		if (select instanceof QuerySelect && ((QuerySelect)select).getColumns() == null)
		{
			// no columns, add 'select 1'
			((QuerySelect)select).addColumn(new QueryColumnValue(Integer.valueOf(1), null, true));
		}
		return new QBCondition(this, this, new ExistsCondition(select, true));
	}

	public QuerySelect getQuery() throws RepositoryException
	{
		if (query == null)
		{
			query = new QuerySelect(getQueryTable());
		}
		return query;
	}

	@Override
	QueryTable getQueryTable() throws RepositoryException
	{
		if (queryTable == null)
		{
			if (query != null)
			{
				queryTable = query.getTable();
			}
			else
			{
				queryTable = new QueryTable(getTable().getSQLName(), getTable().getCatalog(), getTable().getSchema());
			}
		}
		return queryTable;
	}

	Object createOperand(Object value)
	{
		if (value instanceof QBColumn)
		{
			return ((QBColumn)value).getQuerySelectValue();
		}
		if (value instanceof QBParameter)
		{
			TablePlaceholderKey key = ((QBParameter)value).getPlaceholderKey();
			Placeholder placeholder = null;
			if (query != null)
			{
				placeholder = query.getPlaceholder(key);
			}
			if (placeholder == null)
			{
				placeholder = new Placeholder(key);
			}
			return placeholder;
		}
		if (value instanceof Date && !(value instanceof Timestamp))
		{
			return new Timestamp(((Date)value).getTime());
		}
		return value;
	}
}
