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

import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.util.keyword.Ident.generateNormalizedNonReservedOSName;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IGlobalValueEntry;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableAndRelationProvider;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.AndOrCondition;
import com.servoy.j2db.query.ExistsCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * <pre data-puremarkdown>
`QBSelect` is a wrapper for building SQL `SELECT` queries in Servoy, providing a flexible API to add conditions, sorting, grouping, joins, and parameters to SQL-based queries. Through structured access to clauses like `where`, `groupBy`, and `joins`, `QBSelect` supports complex query construction and parameterized queries.

For detailed query building, see [Query Builder](../../../../guides/develop/programming-guide/working-with-data/searching/query-builder.md) in the Servoy documentation.

 * </pre>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBSelect")
public class QBSelect extends QBTableClause implements IQueryBuilder
{
	private static final char QUOTE = '\'';

	private final ITableAndRelationProvider tableProvider;

	private QBResult result;
	private QBSorts sort;
	private QBGroupBy groupBy;
	private QBFunctions functions;
	private QBAggregates aggregates;
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
		// do not clone immutables because QueryTable is marked as immutable and a new instance of QueryTable is seen as a
		// different table when this query is used in another query (like with subcondition)
		return deepClone(getQuery(), false);
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

	/*
	 * Get the table, returns null when the datasource does not refer to a physical table or the table cannot be found
	 */
	ITable getTable(String dataSource)
	{
		if (dataSource == null)
		{
			throw new RuntimeException("Cannot access table in query without dataSource");
		}

		try
		{
			// Return null when not found
			return tableProvider.getTable(dataSource);
		}
		catch (RepositoryException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String[] getColumnNames() throws RepositoryException
	{
		String[] columnNames = super.getColumnNames();
		if (columnNames.length > 0)
		{
			return columnNames;
		}
		// Column names cannot be retrieved from the datasource, fall back to the columns in the query
		return getQuery().getColumnNames();
	}

	@Override
	protected QBColumn createColumn(String name) throws RepositoryException
	{
		if (getTable() != null)
		{
			return super.createColumn(name);
		}

		// Column names cannot be retrieved from the datasource, fall back to the columns in the query
		for (IQuerySelectValue qcol : query.getColumns())
		{
			if (name.equals(qcol.getAliasOrName()) || name.equals(generateNormalizedNonReservedOSName(qcol.getColumnName())))
			{
				return new QBColumnImpl(getRoot(), this, qcol);
			}
		}

		throw new RepositoryException("Cannot find query column '" + name + "' in data source '" + getDataSource() + "'");
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

	/** Create an inlined value. An inlined value is a value that will appear literally in the resulting sql.
	 *  For example
	 *  <pre>
	 *  	query.where.add(query.columns.custid.eq(query.inline(200)))
	 *  </pre>
	 *  results in sql
	 *  <pre>
	 *  	where custid = 200
	 *  </pre>
	 *  And
	 *  <pre>
	 *  	query.where.add(query.columns.custid.eq(200))
	 *  </pre>
	 *  results in sql
	 *  <pre>
	 *  	where custid = ?
	 *  </pre> with prepared statement value 200.
	 *  <p>
	 *  Inlined values can be used in situations where prepared statement expressions give sql problems, for example in some group-by clauses.
	 *  <p>
	 *  Note that using the same query with different inlined values effectively disables prepared statement caching for the query and may have a negative performance impact.
	 *  <p>
	 *  In case of a string will the value be validated, values that contain a single quote will not be inlined.
	 *
	 * @sample
	 * 	var query = datasources.db.example_data.order_details.createSelect();
	 * 	var mult = query.columns.unitprice.multiply(query.inline(100, query.columns.unitprice));
	 * 	query.result.add(mult);
	 * 	query.result.add(query.columns.discount.max);
	 * 	query.groupBy.add(mult);
	 *
	 * @param number value to inline
	 */
	@JSFunction
	public Object inline(Number number)
	{
		return inline(number, null);
	}

	/**
	 * Create an inlined value converted to the type of the column.
	 *
	 * @sampleas inline(Number)
	 *
	 * @param number value to inline
	 * @param columnForType convert value to type of the column
	 */
	@JSFunction
	public Object inline(Number number, IQueryBuilderColumn columnForType)
	{
		return number == null ? null : new QueryColumnValue(
			columnForType == null ? number : getAsRightType(number, columnForType.getColumnType(), columnForType.getFlags()), null, true);
	}

	/**
	 * Create an inlined (quoted) value.
	 *
	 * @sampleas inline(Number)
	 *
	 * @param string value to inline
	 */
	@JSFunction
	public Object inline(String string)
	{
		if (string == null)
		{
			return null;
		}
		if (validateInlineString(string))
		{
			return new QueryColumnValue(new StringBuilder().append(QUOTE).append(string).append(QUOTE).toString(), null, true);
		}
		// not valid, make it a parameterized query
		return new QueryColumnValue(string, null, false);
	}

	/**
	 * Validate whether the string is safe to be put in a query, protecting from sql injection.
	 */
	private static boolean validateInlineString(String string)
	{
		// if it does not contain a quote or escape we are safe
		return string == null || (string.indexOf(QUOTE) == -1 && string.indexOf('\\') == -1);
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
	 *	        .add(query.columns.order_date.gt(new Date()))
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

	/**
	 * Get the functions clause from a query, used for functions that are not tied to a column.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.where.add(query.columns.shipname.upper.eq(query.functions.upper('servoy')))
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

	/**
	 * Get the aggregates clause from a query, used for aggregates that are not tied to a column.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.result.add(query.aggregates.count().add(query.columns.countryCode)
	 * query.groupBy.add(query.columns.countryCode)
	 * var ds = databaseManager.getDataSetByQuery(query, 100);
	 */
	@JSReadonlyProperty
	public QBAggregates aggregates()
	{
		if (aggregates == null)
		{
			aggregates = new QBAggregates(this);
		}
		return aggregates;
	}

	/**
	 * Specifies a comment of the query.
	 * @sample
	 * var query = datasources.db.example_data.orders.createSelect();
	 * query.comment = 'Query comment'
	 */
	@JSGetter
	public String getComment()
	{
		if (query != null)
		{
			return query.getComment();
		}
		return null;
	}

	@JSSetter
	public void setComment(String comment)
	{
		getQuery().setComment(comment);
	}

	/** Create an case searched expression.
	 * @sample
	 * 	var query = datasources.db.example_data.order_details.createSelect();
	 *
	 * // case expressions can be added to the result of the query
	 * 	query.result.add(query.case.when(query.columns.quantity.ge(1000)).then('BIG').else('small'));
	 *
	 *  // they can also be used in conditions
	 * 	query.where.add(query.case
	 * 		.when(query.columns.discount.gt(10)).then(50)
	 * 		.when(query.columns.quantity.le(20)).then(70)
	 * 		.else(100)
	 * 	.multiply(query.columns.unitprice).lt(10000));
	 */
	@JSReadonlyProperty(property = "case")
	public QBCase js_case()
	{
		return qcase();
	}

	public QBCase qcase()
	{
		return new QBCase(getRoot(), this);
	}

	/**
	 * Returns the internal SQL of the QBSelect.
	 * Table filters are on by default.
	 *
	 * @sample var sql = query.getSQL(true)
	 *
	 * @return String representing the sql of the Query Builder.
	 */
	@JSFunction
	public String getSQL() throws ServoyException
	{
		return getSQL(true);
	}

	/**
	 * @clonedesc getSQL()
	 *
	 * @sampleas getSQL()
	 *
	 * @param includeFilters include the table filters [default true].
	 *
	 * @return String representing the sql of the Query Builder.
	 */
	@JSFunction
	public String getSQL(boolean includeFilters) throws ServoyException
	{
		QuerySet querySet = tableProvider.getQuerySet(getQuery(), includeFilters);
		return querySet.getSelect().getSql();
	}

	/**
	 * Returns the parameters for the internal SQL of the QBSelect.
	 * Table filters are on by default.
	 *
	 * @sample var parameters = query.getSQLParameters(true)
	 *
	 * @return An Array with the sql parameter values.
	 */
	@JSFunction
	public Object[] getSQLParameters() throws ServoyException
	{
		return getSQLParameters(true);
	}

	/**
	 * @clonedesc getSQLParameters()
	 *
	 * @sampleas getSQLParameters()
	 *
	 * @param includeFilters include the table filters [default true].
	 *
	 * @return An Array with the sql parameter values.
	 */
	@JSFunction
	public Object[] getSQLParameters(boolean includeFilters) throws ServoyException
	{
		QuerySet querySet = tableProvider.getQuerySet(getQuery(), includeFilters);
		// TODO parameters from updates and cleanups
		Object[][] qsParams = querySet.getSelect().getParameters();
		if (qsParams != null && qsParams.length > 0)
		{
			return qsParams[0];
		}

		return null;
	}

	/**
	 * Returns a foundset object for a specified pk base query. Same as databaseManager.getFoundSet(QBSelect).
	 *
	 * @sample
	 * var qb = datasources.db.example_data.orders.createSelect();
	 * qb.result.addPk();
	 * qb.where.add(qb.columns.product_id.eq(1))
	 * var fs = qb.getFoundSet();
	 *
	 * @return A new JSFoundset with the query as its base query.
	 */
	@JSFunction
	public FoundSet getFoundSet()
	{
		try
		{
			return (FoundSet)tableProvider.getFoundSet(this);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Can't get new foundset for: " + query, e); //$NON-NLS-1$
		}
	}

	/**
	 * Performs a sql query with a query builder object. Same as databaseManager.getDataSetByQuery.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * Using this variation of getDataSet any Tablefilter on the involved tables will be taken into account.
	 *
	 * @sample
	 * // use the query from foundset and add a condition
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var q = foundset.getQuery()
	 * q.where.add(q.joins.orders_to_order_details.columns.discount.eq(2))
	 * var maxReturnedRows = 10;//useful to limit number of rows
	 * var ds = q.getDataSet( maxReturnedRows);
	 *
	 * // query: select PK from example.book_nodes where parent = 111 and(note_date is null or note_date > now)
	 * var query = datasources.db.example_data.book_nodes.createSelect().result.addPk().root
	 * query.where.add(query.columns.parent_id.eq(111))
	 * 	.add(query.or
	 * 	.add(query.columns.note_date.isNull)
	 * 	.add(query.columns.note_date.gt(new Date())))
	 * query.getDataSet(max_returned_rows)
	 *
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	@JSFunction
	public JSDataSet getDataSet(Number max_returned_rows) throws ServoyException
	{
		return getDataSet(max_returned_rows, Boolean.TRUE);
	}

	/**
	 * Performs a sql query with a query builder object. Same as databaseManager.getDataSetByQuery.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * @sample
	 * // use the query from a foundset and add a condition
	 * /** @type {QBSelect<db:/example_data/orders>} *&#47;
	 * var q = foundset.getQuery()
	 * q.where.add(q.joins.orders_to_order_details.columns.discount.eq(2))
	 * var maxReturnedRows = 10;//useful to limit number of rows
	 * var ds = q.getDataSet(true, maxReturnedRows);
	 *
	 * // query: select PK from example.book_nodes where parent = 111 and(note_date is null or note_date > now)
	 * var query = datasources.db.example_data.book_nodes.createSelect().result.addPk().root
	 * query.where.add(query.columns.parent_id.eq(111))
	 * 	.add(query.or
	 * 	.add(query.columns.note_date.isNull)
	 * 	.add(query.columns.note_date.gt(new Date())))
	 * query.getDataSet(true, max_returned_rows)
	 *
	 * @param max_returned_rows The maximum number of rows returned by the query.
	 * @param useTableFilters use table filters (default true).
	 *
	 * @return The JSDataSet containing the results of the query.
	 */
	@JSFunction
	public JSDataSet getDataSet(Number max_returned_rows, Boolean useTableFilters) throws ServoyException
	{
		int _max_returned_rows = Utils.getAsInteger(max_returned_rows);

		String serverName = DataSourceUtils.getDataSourceServerName(this.getDataSource());

		if (serverName == null)
			throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { this.getDataSource() }));
		QuerySelect select = this.build();

		if (!QBSelect.validateQueryArguments(select, tableProvider.getApplication()))
		{
			return new JSDataSet(tableProvider.getApplication());
		}

		try
		{
			return new JSDataSet(tableProvider.getApplication(), tableProvider.getDataSetByQuery(this,
				!Boolean.FALSE.equals(useTableFilters), _max_returned_rows));
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static boolean validateQueryArguments(ISQLQuery select, IApplication application)
	{
		if (select != null)
		{
			if (select instanceof QuerySelect && ((QuerySelect)select).getColumns() == null)
			{
				application.reportJSError("Custom query: " + select + " not executed because no columns are specified to be selected", null);
				return false;
			}

			final List<Placeholder> placeHolders = new ArrayList<Placeholder>();
			AbstractBaseQuery.acceptVisitor(select, new IVisitor()
			{
				public Object visit(Object o)
				{
					if (o instanceof Placeholder)
					{
						placeHolders.add((Placeholder)o);
					}
					return o;
				}
			});

			for (Placeholder placeholder : placeHolders)
			{
				if (!placeholder.isSet())
				{
					application.reportJSError("Custom query: " + select + //$NON-NLS-1$
						" not executed because not all arguments have been set: " + placeholder.getKey(), null); //$NON-NLS-1$
					return false;
				}
				Object value = placeholder.getValue();
				if (value instanceof DbIdentValue && ((DbIdentValue)value).getPkValue() == null)
				{
					application.reportJSError("Custom query: " + select + //$NON-NLS-1$
						" not executed because the arguments have a database ident value that is null, from a not yet saved record", null); //$NON-NLS-1$
					return false;
				}

				if (value instanceof java.util.Date && !(value instanceof Timestamp) && !(value instanceof Time))
				{
					placeholder.setValue(new Timestamp(((java.util.Date)value).getTime()));
				}
			}
		}

		return true;
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
				ITable table = getTable();
				if (table == null)
				{
					throw new RuntimeException("Cannot find table for datasource '" + getDataSource() + "'");
				}
				queryTable = table.queryTable(tableAlias);
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
		if (value instanceof QBColumnImpl qbColumn)
		{
			return qbColumn.getQuerySelectValue();
		}

		Object val = value;
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
			if (value instanceof Date && !(value instanceof Timestamp) && !(value instanceof Time))
			{
				// make sure a date is a timestamp
				val = new Timestamp(((Date)value).getTime());
			}
		}
		else if (!(value instanceof IQuerySelectValue))
		{
			// convert the value (especially UUID) to the type of the column
			val = getAsRightType(value, columnType, flags);
		}

		if (val instanceof IQuerySelectValue)
		{
			return (IQuerySelectValue)val;
		}

		return new QueryColumnValue(val, null);
	}

	private Object getAsRightType(Object value, BaseColumnType columnType, int flags)
	{
		Object val = Column.getAsRightType(columnType, flags, value, !isConversionLenient(), false);
		if (val == null && value != null)
		{
			// safety-fallback, could not convert, let JDBC driver do the conversion, only when servoy.client.query.convert.lenient=true
			return value;
		}
		return val;
	}

	@Override
	public String toString()
	{
		return "QBSelect(" + getQuery(true).toString() + ')';
	}
}
