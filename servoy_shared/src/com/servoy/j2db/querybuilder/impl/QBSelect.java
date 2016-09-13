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

import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
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
import com.servoy.j2db.query.IQuerySelectValue;
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
import com.servoy.j2db.util.Settings;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBSelect")
public class QBSelect extends QBTableClause implements IQueryBuilder
{
	private final ITableAndRelationProvider tableProvider;

	private QBResult result;
	private QBSorts sort;
	private QBGroupBy groupBy;
	private QBFunctions functions;
	private QuerySelect query;
	private QBWhereCondition where;
	private QBLogicalCondition having;
	private BaseQueryTable queryTable;

	private QBParameters params;

	private final Scriptable scriptableParent;

	private final IGlobalValueEntry globalScopeProvider;

	private final IDataProviderHandler dataProviderHandler;

	private final boolean conversionLenient;

	QBSelect(ITableAndRelationProvider tableProvider, IGlobalValueEntry globalScopeProvider, IDataProviderHandler dataProviderHandler,
		Scriptable scriptableParent, String dataSource, String alias)
	{
		super(dataSource, alias);
		this.tableProvider = tableProvider;
		this.globalScopeProvider = globalScopeProvider;
		this.dataProviderHandler = dataProviderHandler;
		this.scriptableParent = scriptableParent;
		this.conversionLenient = Boolean.parseBoolean(Settings.getInstance().getProperty("servoy.client.query.convert.lenient", "false"));
	}

	public QBSelect(ITableAndRelationProvider tableProvider, IGlobalValueEntry globalScopeProvider, IDataProviderHandler dataProviderHandler,
		Scriptable scriptableParent, String dataSource, String alias, QuerySelect querySelect)
	{
		this(tableProvider, globalScopeProvider, dataProviderHandler, scriptableParent, dataSource, alias);
		this.query = querySelect;
	}

	@Override
	public QuerySelect build()
	{
		return AbstractBaseQuery.deepClone(getQuery());
	}

	/**
	 * @return the conversionLenient
	 */
	public boolean isConversionLenient()
	{
		return conversionLenient;
	}

	/**
	 * @return the scriptableParent
	 */
	Scriptable getScriptableParent()
	{
		return scriptableParent;
	}

	Table getTable(String dataSource)
	{
		if (dataSource == null)
		{
			throw new RuntimeException("Cannot access table in query without dataSource");
		}
		ITable tbl;
		try
		{
			tbl = tableProvider.getTable(dataSource);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
		if (!(tbl instanceof Table))
		{
			throw new RuntimeException("Cannot resolve datasource '" + dataSource + "'");
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
	 * Get the where-part of the query, used to add conditions.
	 * The conditions added here are AND-ed.
	 * @sample
	 * var query = foundset.getQuery()
	 * query.where.add(query.columns.flag.eq(1))
	 */
	@JSReadonlyProperty
	public QBWhereCondition where() throws RepositoryException
	{
		if (where == null)
		{
			where = new QBWhereCondition(this);
		}
		return where;
	}

	/**
	 * Get the having-part of the query, used to add conditions.
	 * The conditions added here are AND-ed.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
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
				getQuery().setHaving(c = AndCondition.and(c, new AndCondition()));
			}
			having = new QBLogicalCondition(this, this, (AndOrCondition)c);
		}
		return having;
	}

	/**
	 * Clear the having-part of the query.
	 * @sample
	 * var q = foundset.getQuery()
	 * q.where.add(q.columns.x.eq(100))
	 * query.groupBy.clear.root.clearHaving()
	 * foundset.loadRecords(q);
	 */
	@JSFunction
	public QBSelect clearHaving()
	{
		QuerySelect q = getQuery(false);
		if (q != null)
		{
			q.setHaving(null);
		}
		return this;
	}

	/**
	 * Get the result part of the query, used to add result columns or values.
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
	 * Get the sorting part of the query.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
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
	 * Get the group by clause from a query
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
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
	 * Get the named parameters from a query.
	 * @sample
	 * 	var query = datasources.db.example_data.orders.createSelect();
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
	 * Get or create a parameter for the query, this used to parameterize queries.
	 * @sampleas params()
	 *
	 * @param name the name of the parameter
	 */
	@JSFunction
	public QBParameter getParameter(String name) throws RepositoryException
	{
		return params().getParameter(name);
	}

	/**
	 * Create an OR-condition to add conditions to.
	 * @sampleas and()
	 */
	@JSReadonlyProperty
	public QBLogicalCondition or()
	{
		return new QBLogicalCondition(getRoot(), this, new OrCondition());
	}

	/**
	 * Create an AND-condition to add conditions to.
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
	 * Create an negated condition.
	 * @sample
	 * foundset.query.where.add(query.not(query.columns.flag.eq(1)))
	 *
	 * @param cond the logical condition to negate
	 */
	@JSFunction
	public QBCondition not(IQueryBuilderLogicalCondition cond)
	{
		ISQLCondition queryCondition = ((QBLogicalCondition)cond).getQueryCondition();
		return new QBCondition(getRoot(), (QBTableClause)cond.getParent(), queryCondition.negate());
	}

	/**
	 * Create an negated condition.
	 * @sample
	 * foundset.query.where.add(query.not(query.columns.flag.eq(1)))
	 *
	 * @param cond the condition to negate
	 */
	@JSFunction
	public QBCondition not(IQueryBuilderCondition cond)
	{
		return new QBCondition(this, ((QBCondition)cond).getParent(), ((QBCondition)cond).getQueryCondition().negate());
	}

	/**
	 * Get an exists-condition from a subquery
	 * @sample
	 * foundset.query.where.add(query.exists(query2))
	 *
	 * @param query the sub query
	 *
	 */
	public QBCondition js_exists(QBSelect query) throws RepositoryException
	{
		return exists(query);
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

	/**
	 * Get the functions clause from a query, used for functions that are not tied to a column.
	 * @sample
	 * var query = ddatasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.upper.eq(query.functions.upper('servoy'))) //$NON-NLS-1$
	 * foundset.loadRecords(query)
	 */
	@JSReadonlyProperty
	public QBFunctions functions()
	{
		if (functions == null)
		{
			functions = new QBFunctions(this);
		}
		return functions;
	}

	public QuerySelect getQuery()
	{
		return getQuery(true);
	}

	public QuerySelect getQuery(boolean create)
	{
		if (query == null && create)
		{
			query = new QuerySelect(getQueryTable());
		}
		return query;
	}

	@Override
	BaseQueryTable getQueryTable()
	{
		if (queryTable == null)
		{
			if (query != null)
			{
				queryTable = query.getTable();
			}
			else
			{
				queryTable = new QueryTable(getTable().getSQLName(), getTable().getDataSource(), getTable().getCatalog(), getTable().getSchema(), tableAlias);
			}
		}
		return queryTable;
	}

	IQuerySelectValue[] createOperands(Object[] values, BaseColumnType columnType, int flags)
	{
		IQuerySelectValue[] operands = new IQuerySelectValue[values.length];
		for (int i = 0; i < values.length; i++)
		{
			operands[i] = createOperand(values[i], columnType, flags);
		}

		return operands;
	}

	IQuerySelectValue createOperand(Object value, BaseColumnType columnType, int flags)
	{
		if (value instanceof QBColumn)
		{
			return ((QBColumn)value).getQuerySelectValue();
		}

		Object val;
		if (value instanceof QBParameter)
		{
			TablePlaceholderKey key = ((QBParameter)value).getPlaceholderKey();
			Placeholder placeholder = null;
			if (query != null)
			{
				placeholder = query.getPlaceholder(key);
			}
			val = placeholder == null ? new Placeholder(key) : placeholder;
		}
		else if (columnType == null)
		{
			if (value instanceof Date && !(value instanceof Timestamp))
			{
				// make sure a date is a timestamp
				val = new Timestamp(((Date)value).getTime());
			}
			else
			{
				val = value;
			}
		}
		else
		{
			// convert the value (especially UUID) to the type of the column
			val = Column.getAsRightType(columnType.getSqlType(), flags, value, columnType.getLength(), !getRoot().isConversionLenient());
			if (val == null && value != null)
			{
				// safety-fallback, could not convert, let JDBC driver do the conversion, only when servoy.client.query.convert.lenient=true
				val = value;
			}
		}

		return new QueryColumnValue(val, null);
	}

	@Override
	public String toString()
	{
		return "QBSelect(" + getQuery(true).toString() + ')';
	}
}
