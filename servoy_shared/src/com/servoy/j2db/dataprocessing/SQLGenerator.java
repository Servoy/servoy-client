/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.dataprocessing;


import static com.servoy.base.persistence.IBaseColumn.IDENT_COLUMNS;
import static com.servoy.base.persistence.IBaseColumn.UUID_COLUMN;
import static com.servoy.base.query.IQueryConstants.INNER_JOIN;
import static com.servoy.j2db.dataprocessing.SortColumn.ASCENDING;
import static com.servoy.j2db.persistence.Column.mapToDefaultType;
import static com.servoy.j2db.persistence.IColumnTypes.MEDIA;
import static com.servoy.j2db.query.AbstractBaseQuery.acceptVisitor;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.query.AndCondition.and;
import static com.servoy.j2db.query.OrCondition.or;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.cast;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.castfrom;
import static com.servoy.j2db.query.QueryFunction.QueryFunctionType.upper;
import static com.servoy.j2db.util.Utils.iterate;
import static com.servoy.j2db.util.Utils.stream;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.Array;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.Wrapper;

import com.servoy.base.dataprocessing.BaseSQLGenerator;
import com.servoy.base.dataprocessing.ITypeConverter;
import com.servoy.base.dataprocessing.IValueConverter;
import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryColumn;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.query.IQueryConstants;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.dataprocessing.FindState.RelatedFindState;
import com.servoy.j2db.dataprocessing.SQLSheet.ConverterInfo;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderHandler;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.SortingNullprecedence;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.AndCondition;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.ExistsCondition;
import com.servoy.j2db.query.IPlaceholderKey;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.IQuerySort;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.ObjectPlaceholderKey;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryCustomSelect;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryFactory;
import com.servoy.j2db.query.QueryFilter;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.query.QueryInsert;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.query.SortOptions;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * This class is used to generate the (in repository stored?) SQL(Prepared)Statements and to generate te sql for the user find
 *
 * @author jblok
 */
public class SQLGenerator
{
	public static final String STRING_EMPTY = ""; //$NON-NLS-1$

	public static final String PLACEHOLDER_PRIMARY_KEY = "PK"; //$NON-NLS-1$
	public static final String PLACEHOLDER_RELATION_KEY = "RK"; //$NON-NLS-1$
	public static final String PLACEHOLDER_INSERT_KEY = "INSERT"; //$NON-NLS-1$
	public static final String PLACEHOLDER_FOUNDSET_PKS = "FOUNDSET_PKS"; //$NON-NLS-1$

	public static final String SERVOY_CONDITION_PREFIX = "SV:"; //$NON-NLS-1$
	public static final String CONDITION_FILTER = SERVOY_CONDITION_PREFIX + 'F';
	public static final String CONDITION_OMIT = SERVOY_CONDITION_PREFIX + 'O';
	public static final String CONDITION_DELETED = SERVOY_CONDITION_PREFIX + 'D';
	public static final String CONDITION_RELATION = SERVOY_CONDITION_PREFIX + 'R';
	public static final String CONDITION_SEARCH = SERVOY_CONDITION_PREFIX + 'S';
	public static final String CONDITION_CLEAR = SERVOY_CONDITION_PREFIX + 'C';

	public static final String SQL_QUERY_VALIDATION_MESSAGE = "A query must start with 'SELECT', optionally preceded by 'WITH' or 'DECLARE', and must contain 'FROM'";

/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final IApplication application;
	private final Map<String, SQLSheet> cachedDataSourceSQLSheets = new HashMap<String, SQLSheet>(64); // dataSource -> sqlSheet
	private final boolean relatedNullSearchAddPkConditionSystemSetting;
	private final boolean enforcePkInSort;
	private final boolean setRelationNameComment;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public SQLGenerator(IApplication app, boolean setRelationNameComment)
	{
		application = app;
		relatedNullSearchAddPkConditionSystemSetting = Utils.getAsBoolean(
			application.getSettings().getProperty("servoy.client.relatedNullSearchAddPkCondition", "true"));
		// sort should always contain the pk, so that when sorting values are not unique the sorting result is stable
		enforcePkInSort = Utils.getAsBoolean(application.getSettings().getProperty("servoy.foundset.sort.enforcepk", "true")); //$NON-NLS-1$//$NON-NLS-2$
		this.setRelationNameComment = setRelationNameComment;
	}

	private boolean relatedNullSearchAddPkCondition()
	{
		Object relatedNullSearchAddPkConditionSolutionSetting = application.getClientProperty(IApplication.RELATED_NULL_SEARCH_ADD_PK_CONDITION);
		if (relatedNullSearchAddPkConditionSolutionSetting != null)
		{
			return Utils.getAsBoolean(relatedNullSearchAddPkConditionSolutionSetting);
		}
		return relatedNullSearchAddPkConditionSystemSetting;
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */

	// SQL pk(s) select for foundset
	// Note: removeUnusedJoins must be false when the resulting query is changed afterwards (like adding columns)
	QuerySelect getPKSelectSqlSelect(IGlobalValueEntry provider, Table table, QuerySelect oldSQLQuery, List<IRecordInternal> findStates, boolean reduce,
		IDataSet omitPKs, List<SortColumn> orderByFields, boolean removeUnusedJoins) throws ServoyException
	{
		if (table == null)
		{
			throw new RepositoryException(ServoyException.InternalCodes.TABLE_NOT_FOUND);
		}

		QuerySelect retval;
		if (oldSQLQuery != null)
		{
			retval = deepClone(oldSQLQuery);
			retval.setGroupBy(null);
			if (orderByFields != null) retval.clearSorts(); // will be generated based on foundset sorting
			// remove all servoy conditions, except filter, clear, search and relation
			for (String conditionName : retval.getConditionNames())
			{
				if (conditionName != null && conditionName.startsWith(SERVOY_CONDITION_PREFIX) &&
					!(CONDITION_FILTER.equals(conditionName) || CONDITION_CLEAR.equals(conditionName) || CONDITION_SEARCH.equals(conditionName) ||
						CONDITION_RELATION.equals(conditionName)))
				{
					retval.setCondition(conditionName, null);
				}
			}
		}
		else
		{
			retval = new QuerySelect(table.queryTable());
		}

		// Example:-select pk1,pk2 from tablename1 where ((fieldname1 like '%abcd%') or ((fieldname2 like '%xyz%')) (retrieve max 200 rows)

		ArrayList<IQuerySelectValue> pkQueryColumns = new ArrayList<IQuerySelectValue>(3);
		ArrayList<Column> pkColumns = new ArrayList<Column>(3);
		// getPrimaryKeys from table
		Iterator<Column> pks = table.getRowIdentColumns().iterator();

		// make select
		if (!pks.hasNext())
		{
			throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { table.getName() });
		}
		while (pks.hasNext())
		{
			Column column = pks.next();
			pkColumns.add(column);
			pkQueryColumns.add(column.queryColumn(retval.getTable()));
		}
		retval.setColumns(pkQueryColumns);

		if (omitPKs != null && omitPKs.getRowCount() != 0)
		{
			// omit is rebuild each time
			retval.setCondition(CONDITION_OMIT,
				createSetConditionFromPKs(IBaseSQLCondition.NOT_OPERATOR, pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]), pkColumns, omitPKs));
		}
		else if (oldSQLQuery != null)
		{
			retval.setCondition(CONDITION_OMIT, oldSQLQuery.getConditionClone(CONDITION_OMIT));
		}

		if (findStates != null && findStates.size() != 0) //new
		{
			ISQLCondition moreWhere = null;
			for (IRecordInternal obj : findStates)
			{
				if (obj instanceof FindState)
				{
					moreWhere = or(moreWhere, createConditionFromFindState((FindState)obj, retval, provider, pkQueryColumns));
				}
			}

			if (moreWhere != null)
			{
				// if this query is in a clear state move/set that condition as the SEARCH condition from now on.
				// because we want to reduce or append to that search below.
				ISQLCondition clearCondition = retval.getCondition(CONDITION_CLEAR);
				if (clearCondition != null)
				{
					retval.clearCondition(CONDITION_CLEAR);
					retval.addCondition(CONDITION_SEARCH, clearCondition);
				}

				if (reduce)
				{
					retval.addCondition(CONDITION_SEARCH, moreWhere);
				}
				else
				{
					retval.addConditionOr(CONDITION_SEARCH, moreWhere);
				}

				if (retval.getJoins() != null)
				{
					// check if the search condition has an or-condition
					final boolean[] hasOr = { false };
					acceptVisitor(retval.getCondition(CONDITION_SEARCH), new IVisitor()
					{
						public Object visit(Object o)
						{
							if (o instanceof OrCondition && ((OrCondition)o).getAllConditions().size() > 1)
							{
								hasOr[0] = true;
								return new VisitorResult(o, false);
							}
							return o;
						}
					});

					if (hasOr[0])
					{
						// override join type to left outer join, a related OR-search should not make the result set smaller
						for (ISQLJoin join : retval.getJoins())
						{
							if (join instanceof QueryJoin && ((QueryJoin)join).getJoinType() == IQueryConstants.INNER_JOIN)
							{
								((QueryJoin)join).setJoinType(IQueryConstants.LEFT_OUTER_JOIN);
							}
						}
					}
				}
			}
		}

		// make orderby
		if (orderByFields != null || retval.getSorts() == null)
		{
			List<SortColumn> orderBy = orderByFields == null ? new ArrayList<SortColumn>(3) : orderByFields;
			if (orderBy.size() == 0)
			{
				for (Column pkColumn : pkColumns)
				{
					orderBy.add(new SortColumn(pkColumn));
				}
			}

			addSorts(retval, retval.getTable(), provider, table, orderBy, true, false /* joins added for sorting are not permanent */);
		} // else use ordering defined in query

		if (removeUnusedJoins)
		{
			// remove unneeded joins, some may have been added because of a previous sort and are no longer needed.
			retval.removeUnusedJoins(false, true);
		}

		// do not remove sort or groupby test, will cause invalid queries
		// this one causes error and can not be fixed,
		// if (joinswherepart.length() != 0 && !sortIsRelated && groupbyKeyword == STRING_EMPTY && table.getPrimaryKeyCount() == 1)
		// sql select distinct(s_contacts.contactsid) from s_contacts,s_companies where s_contacts.company_id = s_companies.company_id order by s_contacts.surname  ERROR:  For SELECT DISTINCT, ORDER BY expressions must appear in target list

		// retval may have set distinct and plainPKSelect flag based on previous sort columns, make sure to reset first
		retval.setDistinct(false);
		retval.setPlainPKSelect(false);
		if (retval.getJoins() != null && retval.getColumns().size() == 1 && isDistinctAllowed(retval.getColumns(), retval.getSorts()))//if joined pks comes back multiple times
		{
			retval.setDistinct(true);
		}
		else if (retval.getJoins() == null && retval.getColumns().size() == pkColumns.size())//plain pk select
		{
			retval.setPlainPKSelect(true);
		}
		return retval;
	}

	public void addSorts(QuerySelect sqlSelect, BaseQueryTable selectTable, IGlobalValueEntry provider, ITable table, List<SortColumn> orderByFields,
		boolean includeRelated, boolean permanentJoins) throws RepositoryException
	{
		List<Column> unusedRowidentColumns = new ArrayList<Column>(table.getRowIdentColumns());
		for (int i = 0; orderByFields != null && i < orderByFields.size(); i++)
		{
			SortColumn sc = orderByFields.get(i);
			IColumn column = sc.getColumn(); // can be column or aggregate
			if (column.getDataProviderType() == MEDIA && (column.getFlags() & (IDENT_COLUMNS | UUID_COLUMN)) == 0)
			{
				continue; // skip cannot sort blob columns
			}
			SortOptions sortOptions = application.getFoundSetManager().getSortOptions(sc.getColumn());

			Relation[] relations = sc.getRelations();
			// compare on server objects, relation.foreignServerName may be different in case of duplicates
			boolean doRelatedJoin = (includeRelated && relations != null);
			if (doRelatedJoin)
			{
				FlattenedSolution fs = application.getFlattenedSolution();
				for (Relation relation : relations)
				{
					if (relation.isMultiServer() && !fs.getTable(relation.getForeignDataSource()).getServerName().equals(table.getServerName()))
					{
						doRelatedJoin = false;
						break;
					}
				}
			}
			if (doRelatedJoin)
			// related sort, cannot join across multiple servers
			{
				BaseQueryTable primaryQtable = selectTable;
				BaseQueryTable foreignQtable = null;
				for (Relation relation : relations)
				{
					// join must be re-created as it is possible to have globals involved;
					// first remove, then create it
					ISQLTableJoin join = (ISQLTableJoin)sqlSelect.getJoin(primaryQtable, relation.getName());
					if (join != null) sqlSelect.removeJoin(join);

					if (join == null)
					{
						ITable foreignTable = application.getFlattenedSolution().getTable(relation.getForeignDataSource());
						foreignQtable = foreignTable.queryTable();
					}
					else
					{
						foreignQtable = join.getForeignTable();
					}

					sqlSelect.addJoin(createJoin(application.getFlattenedSolution(), relation, primaryQtable, foreignQtable, permanentJoins, provider));
					primaryQtable = foreignQtable;
				}
				IQuerySelectValue queryColumn;
				if (column instanceof Column)
				{
					queryColumn = ((Column)column).queryColumn(foreignQtable);
					unusedRowidentColumns.remove(column);
				}
				else if (column instanceof AggregateVariable)
				{
					AggregateVariable aggregate = (AggregateVariable)column;
					queryColumn = new QueryAggregate(aggregate.getType(), aggregate.getAggregateQuantifier(),
						new QueryColumn(foreignQtable, -1, aggregate.getColumnNameToAggregate(),
							aggregate.getDataProviderType(), aggregate.getLength(), 0, null, aggregate.getFlags()),
						aggregate.getName(), null, false);

					// there has to be a group-by clause for all selected fields
					List<IQuerySelectValue> columns = sqlSelect.getColumns();
					for (IQuerySelectValue selectVal : columns)
					{
						List<IQuerySelectValue> groupBy = sqlSelect.getGroupBy();
						if (selectVal instanceof QueryColumn && (groupBy == null || !groupBy.contains(selectVal)))
						{
							sqlSelect.addGroupBy(selectVal);
						}
					}

					// if the aggregate has not been selected yet, add it and skip it in the result
					QueryAggregate skippedAggregate = new QueryAggregate(aggregate.getType(), aggregate.getAggregateQuantifier(),
						new QueryColumn(foreignQtable, -1, aggregate.getColumnNameToAggregate(), aggregate.getDataProviderType(), aggregate.getLength(), 0,
							null, aggregate.getFlags()),
						aggregate.getName(), null, true);
					if (!columns.contains(skippedAggregate))
					{
						sqlSelect.addColumn(skippedAggregate);
					}
				}
				else
				{
					Debug.log("Skipping sort on unexpected related column type " + column.getClass()); //$NON-NLS-1$
					continue;
				}
				sqlSelect.addSort(new QuerySort(queryColumn, sc.getSortOrder() == ASCENDING, sortOptions));
			}
			else
			{
				// make sure an invalid sort is not possible
				if (column instanceof Column && column.getTable().getName().equals(table.getName()))
				{
					sqlSelect.addSort(new QuerySort(((Column)column).queryColumn(selectTable), sc.getSortOrder() == ASCENDING, sortOptions));
					unusedRowidentColumns.remove(column);
				}
				else
				{
					Debug.log("Skipping sort on unrelated column " + column.getName() + '.' + column.getTable().getName() + " for table " + table.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		// Make sure pk is part of the sort, in case of non-unique sort columns, the sorted result may not be the same in each fetch
		if (enforcePkInSort)
		{
			for (Column column : unusedRowidentColumns)
			{
				SortOptions sortOptions = application.getFoundSetManager().getSortOptions(column);
				sqlSelect.addSort(new QuerySort(column.queryColumn(selectTable), true, sortOptions));
			}
		}
	}

	/**
	 * Join clause for this relation.
	 */
	public ISQLTableJoin createJoin(IDataProviderHandler flattenedSolution, IRelation relation, BaseQueryTable primaryTable, BaseQueryTable foreignTable,
		boolean permanentJoin, final IGlobalValueEntry provider) throws RepositoryException
	{
		return createJoin(flattenedSolution, relation, primaryTable, foreignTable, permanentJoin, provider, setRelationNameComment, relation.getName());
	}

	public static ISQLTableJoin createJoin(IDataProviderHandler flattenedSolution, IRelation relation, BaseQueryTable primaryTable, BaseQueryTable foreignTable,
		boolean permanentJoin, final IGlobalValueEntry provider, boolean setRelationNameComment, String alias) throws RepositoryException
	{
		if (relation instanceof AbstractBase)
		{
			ISQLTableJoin queryJoin = ((AbstractBase)relation).getRuntimeProperty(Relation.RELATION_JOIN);
			if (queryJoin != null)
			{
				// a query join was defined for this relation, just relink the tables for the first and last in the joins
				queryJoin = deepClone(queryJoin);
				queryJoin = AbstractBaseQuery.relinkTable(queryJoin.getPrimaryTable(), primaryTable, queryJoin);
				queryJoin = AbstractBaseQuery.relinkTable(queryJoin.getForeignTable(), foreignTable, queryJoin);

				// update the placeholders for globals
				queryJoin.acceptVisitor(new IVisitor()
				{
					public Object visit(Object o)
					{
						if (o instanceof Placeholder && ((Placeholder)o).getKey() instanceof ObjectPlaceholderKey)
						{
							Object value = provider.getDataProviderValue(((ObjectPlaceholderKey<int[]>)((Placeholder)o).getKey()).getName());
							int[] args = ((ObjectPlaceholderKey<int[]>)((Placeholder)o).getKey()).getObject();
							int dataProviderType = args[0];
							int flags = args[1];
							if (value == null)
							{
								return ValueFactory.createNullValue(dataProviderType);
							}
							return Column.getAsRightType(dataProviderType, flags, value, Integer.MAX_VALUE, false, false);
						}
						return o;
					}
				});

				return queryJoin;
			}
		}

		// build a join from the relation items
		IDataProvider[] primary = relation.getPrimaryDataProviders(flattenedSolution);
		Column[] foreign = relation.getForeignColumns(flattenedSolution);
		int[] operators = relation.getOperators();

		AndCondition joinCondition = new AndCondition();

		for (int x = 0; x < primary.length; x++)
		{
			Column primaryColumn = null;

			//check if stored script calc or table column
			if (primary[x] instanceof ScriptCalculation)
			{
				ScriptCalculation sc = ((ScriptCalculation)primary[x]);
				primaryColumn = sc.getTable().getColumn(sc.getName()); // null when not stored
			}
			else if (primary[x] instanceof Column)
			{
				primaryColumn = (Column)primary[x];
			}

			QueryColumn foreignColumn = foreign[x].queryColumn(foreignTable);
			Object value;
			if (primaryColumn == null)
			{
				if (primary[x] instanceof LiteralDataprovider)
				{
					value = ((LiteralDataprovider)primary[x]).getValue();
					value = foreign[x].getAsRightType(value);
				}
				else
				{
					value = provider.getDataProviderValue(primary[x].getDataProviderID());
					if (value == null)
					{
						value = ValueFactory.createNullValue(primary[x].getDataProviderType());
					}
					else if (value instanceof Placeholder)
					{
						if (((Placeholder)value).getKey() instanceof ObjectPlaceholderKey< ? >)
						{
							((ObjectPlaceholderKey)((Placeholder)value).getKey()).setObject(
								new int[] { primary[x].getDataProviderType(), primary[x].getFlags() });
						}
					}
					else
					{
						value = Column.getAsRightType(primary[x].getDataProviderType(), primary[x].getFlags(), value, Integer.MAX_VALUE, false, false);
					}
				}
			}
			else
			// table type, can be stored calc
			{
				value = primaryColumn.queryColumn(primaryTable);
			}

			// all operators are swappable because only relation operators in RelationItem.RELATION_OPERATORS can be defined.
			// NOTE: elements in joinCondition MUST be CompareConditions (expected in QueryGenerator and SQLGenerator.createConditionFromFindState)
			joinCondition.addCondition(new CompareCondition(RelationItem.swapOperator(operators[x]), foreignColumn, value));
		}
		if (joinCondition.isEmpty())
		{
			throw new RepositoryException("Missing join condition in relation " + relation.getName()); //$NON-NLS-1$
		}
		QueryJoin queryJoin = new QueryJoin(relation.getName(), primaryTable, foreignTable, joinCondition, relation.getJoinType(), permanentJoin, alias);
		if (setRelationNameComment)
		{
			String comment;
			if (alias != null && !alias.equals(relation.getName()))
			{
				comment = "join " + alias + " (" + relation.getName() + ")";
			}
			else
			{
				comment = "relation " + relation.getName();
			}
			queryJoin.setComment(comment);
		}
		return queryJoin;
	}

	/**
	 * Create a join between 2 tables with the same datasource.
	 *
	 * The join is a inner join on the pk columns.
	 */
	public static ISQLTableJoin createSelfJoin(ITable table, BaseQueryTable queryTable1, BaseQueryTable queryTable2)
	{
		ISQLCondition joinCondition = and(table.getRowIdentColumns().stream()
			.map(pkColumn -> new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, pkColumn.queryColumn(queryTable1), pkColumn.queryColumn(queryTable2)))
			.collect(toList()));
		return new QueryJoin(null, queryTable1, queryTable2, joinCondition, INNER_JOIN, false, null);
	}

	static Object[][] createPKValuesArray(List<Column> pkColumns, Collection<Object[]> pks)
	{
		Object[][] pkValues = new Object[pkColumns.size()][];

		for (int k = 0; k < pkColumns.size(); k++)
		{
			pkValues[k] = new Object[pks.size()];
		}

		int r = 0;
		for (Object[] row : pks)
		{
			for (int k = 0; k < row.length; k++)
			{
				Column c = pkColumns.get(k);
				pkValues[k][r] = c.getAsRightType(row[k]);
			}
			r++;
		}
		return pkValues;
	}

	/**
	 * inverse from createPKValuesArray
	 * @param pkColumns
	 * @param pks
	 * @return
	 */
	static BufferedDataSet createPKValuesDataSet(List<Column> pkColumns, Object[][] pkValues)
	{
		List<Object[]> rows = new ArrayList<>();

		if (pkValues != null && pkValues.length > 0 && pkValues[0] != null)
		{
			for (int r = 0; r < pkValues[0].length; r++)
			{
				Object[] pk = new Object[pkColumns.size()];
				for (int k = 0; k < pkColumns.size(); k++)
				{
					pk[k] = pkValues[k][r];
				}
				rows.add(pk);
			}
		}

		return new BufferedDataSet(null, rows);
	}

	static SetCondition createSetConditionFromPKs(int operator, QueryColumn[] pkQuerycolumns, List<Column> pkColumns, IDataSet pks)
	{
		if (pkQuerycolumns.length != pkColumns.size())
		{
			throw new RuntimeException("Inconsistent pk list"); //$NON-NLS-1$
		}
		if (pkQuerycolumns.length != pks.getColumnCount())
		{
			throw new RuntimeException("Inconsistent pk values"); //$NON-NLS-1$
		}

		if (pks.getRowCount() == 0)
		{
			return null;
		}

		return new SetCondition(operator, pkQuerycolumns, createPKValuesArray(pkColumns, pks.getRows()),
			(operator & IBaseSQLCondition.OPERATOR_MASK) == IBaseSQLCondition.EQUALS_OPERATOR);
	}


	/**
	 * Distinct is allowed if order by clause is a subset of the selected columns.
	 */
	public static boolean isDistinctAllowed(List<IQuerySelectValue> columns, List<IQuerySort> orderByFields)
	{
		return stream(orderByFields).allMatch(sort -> {
			if (sort instanceof QuerySort)
			{
				QuerySort qSort = (QuerySort)sort;
				IQuerySelectValue column = qSort.getColumn();
				if (!columns.contains(column))
				{
					return false;
				}
				// with ignoreCase the sql for the sort column is not the same as how the column appears in the select list
				if (Column.mustApplyCaseinsensitiveModifier(qSort, column))
				{
					return false;
				}
				// with ascNullsFirst or ascNullsLast the sql for the sort column may not be the same as how the column appears in the select list
				if (qSort.nullprecedence() != SortingNullprecedence.databaseDefault)
				{
					return false;
				}
				return true;
			}
			return false;
		});
	}

	public static String getFindToolTip(IServiceProvider application)
	{
		List<Object[]> data = new ArrayList<Object[]>();
		data.add(new Object[] { "c1||c2", application.getI18NMessage("servoy.client.findModeHelp.orGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { application.getI18NMessage("servoy.client.findModeHelp.formatDateCol1"), application.getI18NMessage( //$NON-NLS-1$
			"servoy.client.findModeHelp.formatDateCol2") }); //$NON-NLS-1$
		data.add(new Object[] { "!c", application.getI18NMessage("servoy.client.findModeHelp.notGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "#c", application.getI18NMessage("servoy.client.findModeHelp.modifiedCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "^", application.getI18NMessage("servoy.client.findModeHelp.nullGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "^=", application.getI18NMessage("servoy.client.findModeHelp.nullTextCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "&lt;x", application.getI18NMessage("servoy.client.findModeHelp.ltGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "&gt;x", application.getI18NMessage("servoy.client.findModeHelp.gtGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "&lt;=x", application.getI18NMessage("servoy.client.findModeHelp.lteGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "&gt;=x", application.getI18NMessage("servoy.client.findModeHelp.gteGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "x...y", application.getI18NMessage("servoy.client.findModeHelp.betweenGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "x", application.getI18NMessage("servoy.client.findModeHelp.equalsGeneralCondition") }); //$NON-NLS-1$ //$NON-NLS-2$

		data.add(new Object[] { "<b>" + application.getI18NMessage("servoy.client.findModeHelp.numberFields") + "</b>", "&nbsp;" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		data.add(new Object[] { "=x", application.getI18NMessage("servoy.client.findModeHelp.equalsNumberCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "^=", application.getI18NMessage("servoy.client.findModeHelp.nullZeroCondition") }); //$NON-NLS-1$ //$NON-NLS-2$

		data.add(new Object[] { "<b>" + application.getI18NMessage("servoy.client.findModeHelp.dateFields") + "</b>", "&nbsp;" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		data.add(new Object[] { "#c", application.getI18NMessage("servoy.client.findModeHelp.equalsDateCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "now", application.getI18NMessage("servoy.client.findModeHelp.nowDateCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "//", application.getI18NMessage("servoy.client.findModeHelp.todayCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "today", application.getI18NMessage("servoy.client.findModeHelp.todayCondition") }); //$NON-NLS-1$ //$NON-NLS-2$

		data.add(new Object[] { "<b>" + application.getI18NMessage("servoy.client.findModeHelp.textFieldsCol1") + "</b>", "&nbsp;" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		data.add(new Object[] { "#c", application.getI18NMessage("servoy.client.findModeHelp.caseInsensitiveCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "= x", application.getI18NMessage("servoy.client.findModeHelp.equalsSpaceXCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "^=", application.getI18NMessage("servoy.client.findModeHelp.nullTextCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "%x%", application.getI18NMessage("servoy.client.findModeHelp.containsTextCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "%x_y%", application.getI18NMessage("servoy.client.findModeHelp.containsXCharYCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "\\%", application.getI18NMessage("servoy.client.findModeHelp.containsPercentCondition") }); //$NON-NLS-1$ //$NON-NLS-2$
		data.add(new Object[] { "\\_", application.getI18NMessage("servoy.client.findModeHelp.containsUnderscoreCondition") }); //$NON-NLS-1$ //$NON-NLS-2$

		BufferedDataSet set = new BufferedDataSet(
			new String[] { application.getI18NMessage("servoy.client.findModeHelp.generalCol1"), application.getI18NMessage( //$NON-NLS-1$
				"servoy.client.findModeHelp.generalCol2") }, //$NON-NLS-1$
			data);
		JSDataSet ds = new JSDataSet(application, set);
		return "<html><body>" + ds.js_getAsHTML(Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE) + "</body></html>"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private ISQLCondition createConditionFromFindState(FindState s, QuerySelect sqlSelect, IGlobalValueEntry provider, List<IQuerySelectValue> pkQueryColumns)
		throws RepositoryException
	{
		ISQLCondition and = null;

		List<RelatedFindState> relatedFindStates = s.createFindStateJoins(sqlSelect, Collections.<IRelation> emptyList(), sqlSelect.getTable(), provider);
		for (int i = 0; relatedFindStates != null && i < relatedFindStates.size(); i++)
		{
			RelatedFindState rfs = relatedFindStates.get(i);
			FindState state = rfs.getFindState();
			BaseQueryTable columnTable = rfs.getPrimaryTable();

			SQLSheet sheet = state.getParentFoundSet().getSQLSheet();
			Table table = sheet.getTable();

			for (Entry<String, Object> elem : state.getColumnData().entrySet())
			{
				final String dataProviderID = elem.getKey();
				Object raw = elem.getValue();
				if (raw == null) continue;

				int dataProviderType = -1;
				ConverterInfo columnConverterInfo = null;
				IColumnConverter columnConverter = null;
				IQuerySelectValue qCol = null;
				IColumn c = table.getColumn(dataProviderID);
				if (c != null)
				{
					dataProviderType = c.getDataProviderType();
					columnConverterInfo = sheet.getColumnConverterInfo(dataProviderID);
					if (columnConverterInfo != null)
					{
						columnConverter = application.getFoundSetManager().getColumnConverterManager().getConverter(columnConverterInfo.converterName);
						if (columnConverter instanceof ITypedColumnConverter)
						{
							int convType = ((ITypedColumnConverter)columnConverter).getToObjectType(columnConverterInfo.props);
							if (convType != Integer.MAX_VALUE)
							{
								dataProviderType = mapToDefaultType(convType);
							}
						}
					}

					// a column
					qCol = ((Column)c).queryColumn(columnTable);
				}
				else
				{
					// not a column, check for aggregates
					Iterator<AggregateVariable> aggregateVariables = application.getFlattenedSolution().getAggregateVariables(sheet.getTable(), false);
					while (c == null && aggregateVariables.hasNext())
					{
						AggregateVariable agg = aggregateVariables.next();
						if (dataProviderID.equals(agg.getDataProviderID()))
						{
							// found aggregate
							c = agg;
						}
					}

					if (c != null)
					{
						dataProviderType = c.getDataProviderType();
						Map<String, QuerySelect> aggregates = sheet.getAggregates();
						if (aggregates != null)
						{
							QuerySelect aggregateSelect = aggregates.get(dataProviderID);
							if (aggregateSelect != null)
							{
								qCol = ((List<IQuerySelectValue>)AbstractBaseQuery.relinkTable(aggregateSelect.getTable(), columnTable,
									aggregateSelect.getColumnsClone())).get(0);
							}
						}
					}
				}

				if (qCol == null)
				{
					// not a column and not an aggregate
					Debug.log("Ignoring search on unknown/unsupported data provider '" + dataProviderID + "'"); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}

				ParsedFormat format = state.getFormat(dataProviderID);
				String formatString = null;
				if (format != null)
				{
					formatString = format.getEditFormat();
					if (formatString == null)
					{
						formatString = format.getDisplayFormat();
					}
				}
				if (Utils.stringIsEmpty(formatString))
				{
					formatString = TagResolver.getDefaultFormatForType(application, dataProviderType);
				}

				ISQLCondition or = null;
				if (raw.getClass().isArray())
				{
					int length = Array.getLength(raw);
					Object[] elements = new Object[length];
					for (int e = 0; e < length; e++)
					{
						Object obj = Array.get(raw, e);
						if (obj instanceof Wrapper)
						{
							obj = ((Wrapper)obj).unwrap();
						}
						// Have to use getAsRightType twice here, once to parse using format (getAsType(dataProviderType, formatString))
						// and once to convert for query (getAsType(c.getDataProviderType(), null))
						Object converted = convertFromObject(application, columnConverter, columnConverterInfo, dataProviderID, c.getDataProviderType(),
							Column.getAsRightType(dataProviderType, c.getFlags(), obj, formatString, c.getLength(), null, false, false), false);
						elements[e] = Column.getAsRightType(c.getDataProviderType(), c.getFlags(), converted, null, c.getLength(), null, false, false);
					}
					// where qCol in (e1, e2, ..., en)
					or = new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, new IQuerySelectValue[] { qCol }, new Object[][] { elements }, true);
				}
				else
				{
					final IColumnConverter fColumnConverter = columnConverter;
					final ConverterInfo fColumnConverterInfo = columnConverterInfo;
					final int fDataProviderType = c.getDataProviderType();
					or = (ISQLCondition)BaseSQLGenerator.parseFindExpression(QueryFactory.INSTANCE, raw, qCol, columnTable, dataProviderType, formatString, c,
						rfs.getRelations().size() > 0 && relatedNullSearchAddPkCondition(), new IValueConverter()
						{
							@Override
							public Object convertFromObject(Object value)
							{
								return SQLGenerator.convertFromObject(application, fColumnConverter, fColumnConverterInfo, dataProviderID, fDataProviderType,
									value, false);
							}
						}, new ITypeConverter()
						{

							@Override
							public Object getAsRightType(int type, int flags, Object obj, int l, boolean throwOnFail)
							{
								return Column.getAsRightType(type, flags, obj, l, throwOnFail, false);
							}

							@Override
							public Object getAsRightType(int type, int flags, Object obj, String format, int l, boolean throwOnFail)
							{
								return Column.getAsRightType(type, flags, obj, format, l, null, throwOnFail, false);
							}
						}, table.getRowIdentColumns().get(0), Debug.LOGGER);
				}

				if (or != null)
				{
					ISQLCondition condition;
					if (c instanceof AggregateVariable)
					{
						condition = createExistsCondition(application.getFlattenedSolution(), sqlSelect, or, rfs.getRelations(), columnTable, provider,
							pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]), setRelationNameComment);
					}
					else
					{
						condition = or;
					}

					and = AndCondition.and(and, condition);
				}
			}
		}

		return and;
	}

	public static Object convertFromObject(IServiceProvider application, IColumnConverter columnConverter, ConverterInfo columnConverterInfo,
		String dataProviderID, int columnType, Object obj, boolean throwOnFail)
	{
		if (columnConverter != null)
		{
			try
			{
				return columnConverter.convertFromObject(columnConverterInfo.props, columnType, obj);
			}
			catch (Exception e)
			{
				IllegalArgumentException illarg = new IllegalArgumentException(
					"Could not convert value '" + obj + "' for dataprovider '" + dataProviderID + "'", e); //$NON-NLS-3$
				if (throwOnFail)
				{
					throw illarg;
				}

				// don't throw, just log and return original object
				application.reportJSError(illarg.getMessage(), illarg);
			}
		}
		return obj;
	}

	public static ISQLCondition createExistsCondition(IDataProviderHandler flattenedSolution, QuerySelect sqlSelect, ISQLCondition condition,
		List<IRelation> relations, BaseQueryTable columnTable, IGlobalValueEntry provider, BaseQueryColumn[] pkQueryColumns, boolean setRelationNameComment)
		throws RepositoryException
	{
		// search on aggregate, change to exists-condition:
		// exists (select 1 from innermain join related1 ... join relatedn where innermain.pk = main.pk having aggregate(relatedn))
		if (relations.size() == 0)
		{
			// searching for aggregate in main table, does no make sense.. ignore in search
			return null;
		}

		QuerySelect existsSelect = new QuerySelect(new QueryTable(sqlSelect.getTable().getName(), sqlSelect.getTable().getDataSource(),
			sqlSelect.getTable().getCatalogName(), sqlSelect.getTable().getSchemaName()));
		existsSelect.addColumn(new QueryColumnValue(Integer.valueOf(1), null, true));

		// innermain.pk = main.pk
		QueryColumn[] innerPkColumns = new QueryColumn[pkQueryColumns.length];
		for (int p = 0; p < pkQueryColumns.length; p++)
		{
			BaseQueryColumn pk = pkQueryColumns[p];
			innerPkColumns[p] = new QueryColumn(existsSelect.getTable(), pk.getId(), pk.getName(), pk.getColumnType().getSqlType(),
				pk.getColumnType().getLength(), pk.getColumnType().getScale(), pk.getNativeTypename(), pk.getFlags(), pk.isIdentity());

			// group by on the inner pk, some dbs (hxtt dbf) require that
			existsSelect.addGroupBy(innerPkColumns[p]);
		}
		existsSelect.addCondition("AGGREGATE-SEARCH", new SetCondition(new int[] { IBaseSQLCondition.EQUALS_OPERATOR }, innerPkColumns, //$NON-NLS-1$
			pkQueryColumns, true));

		// add the joins
		BaseQueryTable prevTable = existsSelect.getTable();
		for (IRelation relation : relations)
		{
			ITable foreignTable = flattenedSolution.getTable(relation.getForeignDataSource());
			QueryTable foreignQtable = foreignTable.queryTable();
			existsSelect.addJoin(createJoin(flattenedSolution, relation, prevTable, foreignQtable, true, provider, setRelationNameComment, relation.getName()));

			prevTable = foreignQtable;
		}
		existsSelect.addHaving(AbstractBaseQuery.relinkTable(columnTable, prevTable, condition));

		return new ExistsCondition(existsSelect, true);
	}

	static SetCondition createDynamicPKSetConditionForFoundset(FoundSet foundSet, BaseQueryTable queryTable, IDataSet pks)
	{
		Table table = (Table)foundSet.getTable();

		List<Column> rowIdentColumns = table.getRowIdentColumns();
		QueryColumn[] pkQueryColumns = new QueryColumn[rowIdentColumns.size()];

		// getPrimaryKeys from table
		for (int i = 0; i < rowIdentColumns.size(); i++)
		{
			Column column = rowIdentColumns.get(i);
			pkQueryColumns[i] = column.queryColumn(queryTable);
		}

		// Dynamic PK condition, the special placeholder will be updated when the foundset pk set changes
		Placeholder placeHolder = new Placeholder(new TablePlaceholderKey(queryTable, SQLGenerator.PLACEHOLDER_FOUNDSET_PKS));
		placeHolder.setValue(new DynamicPkValuesArray(rowIdentColumns, pks.getRows()));
		return new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, pkQueryColumns, placeHolder, true);
	}

	static boolean isDynamicPKSetCondition(ISQLCondition condition)
	{
		if (condition instanceof SetCondition)
		{
			Object values = ((SetCondition)condition).getValues();
			if (!(values instanceof Placeholder))
			{
				return false;
			}
			IPlaceholderKey key = ((Placeholder)values).getKey();
			if (!(key instanceof TablePlaceholderKey))
			{
				return false;
			}
			if (!SQLGenerator.PLACEHOLDER_FOUNDSET_PKS.equals(((TablePlaceholderKey)key).getName()))
			{
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * check if the query is will never return any rows, in that case just return an empty dataset.
	 */
	public static IDataSet getEmptyDataSetForDummyQuery(ISQLSelect sqlSelect)
	{
		if (sqlSelect instanceof QuerySelect && ((QuerySelect)sqlSelect).getWhere() != null)
		{
			// go over all where conditions to check if this query would result in possible values or not.
			for (IBaseSQLCondition condition : iterate(((QuerySelect)sqlSelect).getWhere().getAllConditions()))
			{
				boolean skipQuery = false;
				if (condition instanceof SetCondition && ((SetCondition)condition).isAndCondition())
				{
					// check for EQUALS_OPERATOR
					int ncols = ((SetCondition)condition).getKeys().length;
					int[] operators = ((SetCondition)condition).getOperators();
					boolean eqop = true;
					for (int i = 0; i < ncols; i++)
					{
						if (operators[i] != IBaseSQLCondition.EQUALS_OPERATOR)
						{
							eqop = false;
						}
					}

					if (eqop)
					{
						Object value = ((SetCondition)condition).getValues();
						if (value instanceof Placeholder)
						{
							Object phval = ((Placeholder)value).getValue();
							skipQuery = phval instanceof DynamicPkValuesArray && ((DynamicPkValuesArray)phval).isEmpty(); // cleared foundset
						}
						else if (value instanceof Object[][])
						{
							skipQuery = ((Object[][])value).length == 0 || ((Object[][])value)[0].length == 0;
						}
					}
				}
				// else more complex query, run the query

				if (skipQuery)
				{
					// no need to query, dummy condition (where 1=2) here
					List<IQuerySelectValue> columns = ((QuerySelect)sqlSelect).getColumns();
					String[] columnNames = new String[columns.size()];
					ColumnType[] columnTypes = new ColumnType[columns.size()];
					for (int i = 0; i < columns.size(); i++)
					{
						IQuerySelectValue col = columns.get(i);
						columnNames[i] = col.getAliasOrName();
						BaseColumnType columnType = col.getColumnType();
						columnTypes[i] = columnType == null ? ColumnType.getInstance(Types.OTHER, 0, 0)
							: ColumnType.getInstance(columnType.getSqlType(), columnType.getLength(), columnType.getScale());
					}

					return BufferedDataSetInternal.createBufferedDataSet(columnNames, columnTypes, new SafeArrayList<Object[]>(0), false);
				}
			}
		}

		// query needs to be run
		return null;
	}

	private void createAggregates(SQLSheet sheet, QueryTable queryTable)
	{
		Table table = sheet.getTable();
		Iterator<AggregateVariable> it = application.getFlattenedSolution().getAggregateVariables(table, false);
		while (it.hasNext())
		{
			AggregateVariable aggregate = it.next();
			QuerySelect sql = new QuerySelect(queryTable);
			sql.addColumn(new QueryAggregate(aggregate.getType(), aggregate.getAggregateQuantifier(),
				new QueryColumn(queryTable, -1, aggregate.getColumnNameToAggregate(), aggregate.getDataProviderType(), aggregate.getLength(), 0, null,
					aggregate.getFlags()),
				aggregate.getName(), null, false));
			sheet.addAggregate(aggregate.getDataProviderID(), aggregate.getDataProviderIDToAggregate(), sql);
		}
	}

	//return all sql as sqlsheet with related sheets
	public synchronized SQLSheet getCachedTableSQLSheet(String dataSource) throws ServoyException
	{
		SQLSheet sheet = cachedDataSourceSQLSheets.get(dataSource);
		if (sheet == null)
		{
			long t1 = System.currentTimeMillis();
			sheet = createTableSQL(dataSource, true);
			if (Debug.tracing())
			{
				long t2 = System.currentTimeMillis();
				Debug.trace("Creating the sql sheet took ms: " + (t2 - t1) + " for table: " + dataSource); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return sheet;
	}

	//explicitly ask for new, any related is still be a cached one!! (only used by subsumaryFS)
	public synchronized SQLSheet getNewTableSQLSheet(String dataSource) throws ServoyException
	{
		return createTableSQL(dataSource, false);
	}


	/**
	 * @param uri
	 */
	public synchronized void removeCache(String dataSource)
	{
		cachedDataSourceSQLSheets.remove(dataSource);
	}

	private SQLSheet createNoTableSQL(boolean cache)
	{
		SQLSheet sheet = new SQLSheet(application, null, null);
		if (cache) cachedDataSourceSQLSheets.put(null, sheet);//never remove this line, due to recursive behaviour, register a state when immediately!
		return sheet;
	}

	private SQLSheet createTableSQL(String dataSource, boolean cache) throws ServoyException
	{
		if (dataSource == null)
		{
			return createNoTableSQL(cache);
		}
		Table table = (Table)application.getFoundSetManager().getTable(dataSource);
		if (table == null)
		{
			throw new RepositoryException("Cannot create sql: table not found for data source '" + dataSource + '\''); //$NON-NLS-1$
		}
		SQLSheet retval = new SQLSheet(application, table.getServerName(), table);

		if (cache) cachedDataSourceSQLSheets.put(dataSource, retval);//never remove this line, due to recursive behaviour, register a state when immediately!

		QueryTable queryTable = table.queryTable();

		QuerySelect select = new QuerySelect(queryTable);
		QueryDelete delete = new QueryDelete(queryTable);
		QueryInsert insert = new QueryInsert(queryTable);
		QueryUpdate update = new QueryUpdate(queryTable);

		List<Column> columns = new ArrayList<Column>();
		for (Column c : table.getColumns())
		{
			ColumnInfo ci = c.getColumnInfo();
			if (ci != null && ci.isExcluded())
			{
				continue;
			}
			columns.add(c);
		}

		List<String> requiredDataProviderIDs = new ArrayList<String>();
		Iterator<Column> pks = table.getRowIdentColumns().iterator();
		if (!pks.hasNext())
		{
			throw new RepositoryException(ServoyException.InternalCodes.PRIMARY_KEY_NOT_FOUND, new Object[] { table.getName() });
		}
		List<QueryColumn> pkQueryColumns = new ArrayList<QueryColumn>();
		while (pks.hasNext())
		{
			Column column = pks.next();
			if (!columns.contains(column)) columns.add(column);
			requiredDataProviderIDs.add(column.getDataProviderID());
			pkQueryColumns.add(column.queryColumn(queryTable));
		}

		select.setColumns(makeQueryColumns(columns, queryTable, insert));
		SetCondition pkSelect = new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, pkQueryColumns.toArray(new QueryColumn[pkQueryColumns.size()]),
			new Placeholder(new TablePlaceholderKey(queryTable, PLACEHOLDER_PRIMARY_KEY)), true);

		select.setCondition(CONDITION_SEARCH, pkSelect);
		delete.setCondition(deepClone(pkSelect));
		update.setCondition(deepClone(pkSelect));

		// fill dataprovider map
		List<String> dataProviderIDsDilivery = new ArrayList<String>();
		for (Column col : columns)
		{
			dataProviderIDsDilivery.add(col.getDataProviderID());
		}

		retval.addSelect(select, dataProviderIDsDilivery, requiredDataProviderIDs, null);
		retval.addDelete(delete, requiredDataProviderIDs);
		retval.addInsert(insert, dataProviderIDsDilivery);
		retval.addUpdate(update, dataProviderIDsDilivery, requiredDataProviderIDs);

		//related stuff
		createAggregates(retval, queryTable);

		return retval;
	}


	/**
	 * Create place holder name for PR (Relation Key)
	 */
	static TablePlaceholderKey createRelationKeyPlaceholderKey(BaseQueryTable foreignTable, String relationName)
	{
		return new TablePlaceholderKey(foreignTable, createRelationKeyPlaceholderName(relationName));
	}

	private static String createRelationKeyPlaceholderName(String relationName)
	{
		return PLACEHOLDER_RELATION_KEY + ':' + relationName;
	}

	synchronized void makeRelatedSQL(SQLSheet relatedSheet, Relation r)
	{
		if (relatedSheet.getRelatedSQLDescription(r.getName()) != null) return;

		try
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			if (!Relation.isValid(r, fs) || r.isParentRef())
			{
				return;
			}
			ITable foreignTable = fs.getTable(r.getForeignDataSource());
			if (foreignTable == null)
			{
				return;
			}

			// add primary keys if missing
			QueryTable foreignQTable = foreignTable.queryTable();
			QuerySelect relatedSelect = new QuerySelect(foreignQTable);

			List<String> parentRequiredDataProviderIDs = new ArrayList<>();
			Column[] relcols = r.getForeignColumns(fs);
			for (Column column : relcols)
			{
				parentRequiredDataProviderIDs.add(column.getDataProviderID());
			}

			relatedSelect.setCondition(CONDITION_RELATION, createRelatedCondition(application, r, foreignQTable));

			Collection<Column> rcolumns = foreignTable.getColumns();
			relatedSelect.setColumns(makeQueryColumns(rcolumns, foreignQTable, null));

			// fill dataprovider map
			List<String> dataProviderIDsDilivery = new ArrayList<String>();
			for (Column col : rcolumns)
			{
				dataProviderIDsDilivery.add(col.getDataProviderID());
			}

			relatedSheet.addRelatedSelect(r.getName(), relatedSelect, dataProviderIDsDilivery, parentRequiredDataProviderIDs, null);

			createAggregates(relatedSheet, foreignQTable);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
	}

	public static ISQLCondition createRelatedCondition(IServiceProvider app, Relation relation, QueryTable foreignTable) throws RepositoryException
	{
		IDataProvider[] primary = relation.getPrimaryDataProviders(app.getFlattenedSolution());
		Column[] foreign = relation.getForeignColumns(app.getFlattenedSolution());
		int[] operators = relation.getOperators();

		IQuerySelectValue[] keys = new IQuerySelectValue[primary.length];
		int swapped[] = new int[primary.length];

		for (int x = 0; x < primary.length; x++)
		{
			// need all keys as columns on the left side......
			int operator = RelationItem.swapOperator(operators[x]);
			if (operator == -1)
			{
				throw new RepositoryException("Cannot swap relation operator for relation " + relation.getName());
			}
			// column = ? construct
			IQuerySelectValue key = foreign[x].queryColumn(foreignTable);

			if ((key.getFlags() & UUID_COLUMN) == 0)
			{
				// When we have a text and non-text column we can cast the non-text column to string
				int primaryType = primary[x].getDataProviderType();
				int foreignType = mapToDefaultType(key.getColumnType());
				if (foreignType == IColumnTypes.TEXT && primaryType != IColumnTypes.TEXT && primaryType != 0)
				{
					// key is text, value is non-text, cast the value to text when we supply it
					operator |= IBaseSQLCondition.CAST_TO_MODIFIER;
				}
				else if (primaryType == IColumnTypes.TEXT && foreignType != IColumnTypes.TEXT)
				{
					// value is text, key is non-text, cast the key to text
					key = new QueryFunction(cast,
						new IQuerySelectValue[] { key, new QueryColumnValue(IQueryConstants.TYPE_STRING, null, true) }, null);
					// Cast if needed
					operator |= IBaseSQLCondition.CAST_TO_MODIFIER;
				}
			}

			keys[x] = key;
			swapped[x] = operator;
		}
		return new SetCondition(swapped, keys, new Placeholder(createRelationKeyPlaceholderKey(foreignTable, relation.getName())), true);
	}

	/**
	 * Find the relation placeholder in the query.
	 *
	 * <p>Use this method instead of
	 * <pre>querySelect.getPlaceholder(SQLGenerator.createRelationKeyPlaceholderKey(querySelect.getTable(), relationName));</pre>
	 * For better performance.
	 */
	public static Placeholder getRelationPlaceholder(QuerySelect querySelect, String relationName)
	{
		List<ISQLCondition> relationCondition = querySelect.getConditions(SQLGenerator.CONDITION_RELATION);
		if (relationCondition != null)
		{
			ISQLCondition firstCondition = relationCondition.iterator().next();
			if (firstCondition instanceof SetCondition)
			{
				SetCondition setCondition = (SetCondition)firstCondition;
				if (setCondition.getValues() instanceof Placeholder)
				{
					Placeholder placeholder = (Placeholder)setCondition.getValues();
					if (createRelationKeyPlaceholderName(relationName).equals(placeholder.getKey().getName()))
					{
						return placeholder;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Create a condition if the filter is applicable to the table.
	 *
	 * @param qTable
	 * @param table
	 * @param filter
	 * @return
	 */
	public static QueryFilter createTableFiltercondition(BaseQueryTable qTable, ITable table, TableFilter filter)
	{
		if (filter.getTableFilterdefinition() == null)
		{
			return null;
		}

		if (!table.getSQLName().equals(qTable.getName()))
		{
			// not for this table
			return null;
		}

		if (filter.getTableFilterdefinition() instanceof QueryTableFilterdefinition)
		{
			return createTableFiltercondition(qTable, table, ((QueryTableFilterdefinition)filter.getTableFilterdefinition()).getQuerySelect());
		}

		if (filter.getTableFilterdefinition() instanceof DataproviderTableFilterdefinition)
		{
			return createTableFiltercondition(qTable, table, (DataproviderTableFilterdefinition)filter.getTableFilterdefinition());
		}

		throw new IllegalStateException("Unknown table filter definition type: " + filter.getTableFilterdefinition().getClass().getName());

	}

	private static QueryFilter createTableFiltercondition(BaseQueryTable qTable, ITable table, QuerySelect filterQuery)
	{
		QuerySelect filterQueryClone = deepClone(filterQuery);
		filterQueryClone.relinkTable(filterQueryClone.getTable(), qTable);

		List<QueryColumn> pkColumns = table.getRowIdentColumns().stream()
			.map(column -> column.queryColumn(qTable))
			.collect(toList());

		return new QueryFilter(filterQueryClone.getJoins(), pkColumns, filterQueryClone.getWhere());
	}

	public static QueryFilter createTableFiltercondition(BaseQueryTable qTable, ITable table, DataproviderTableFilterdefinition filterdefinition)
	{
		Column c = table.getColumn(filterdefinition.getDataprovider());
		if (c == null)
		{
			Debug.error("Could not apply filter " + filterdefinition + " on table " + table + " : column not found:" + filterdefinition.getDataprovider()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return null;
		}
		int op = filterdefinition.getOperator();
		int maskedOp = op & IBaseSQLCondition.OPERATOR_MASK;
		Object value = filterdefinition.getValue();

		QueryColumn qColumn = c.queryColumn(qTable);
		ISQLCondition filterWhere;
		if (maskedOp == IBaseSQLCondition.EQUALS_OPERATOR || maskedOp == IBaseSQLCondition.NOT_OPERATOR || maskedOp == IBaseSQLCondition.IN_OPERATOR ||
			maskedOp == IBaseSQLCondition.NOT_IN_OPERATOR || maskedOp == IBaseSQLCondition.GT_OPERATOR || maskedOp == IBaseSQLCondition.LT_OPERATOR ||
			maskedOp == IBaseSQLCondition.GTE_OPERATOR || maskedOp == IBaseSQLCondition.LTE_OPERATOR)
		{
			Object inValues;
			boolean andCondition = true;
			if (value instanceof List< ? >)
			{
				inValues = new Object[][] { ((List< ? >)value).toArray() };
				andCondition = maskedOp != IBaseSQLCondition.NOT_OPERATOR && maskedOp != IBaseSQLCondition.NOT_IN_OPERATOR;
			}
			else if (value != null && value.getClass().isArray())
			{
				inValues = new Object[][] { (Object[])value };
				andCondition = maskedOp != IBaseSQLCondition.NOT_OPERATOR && maskedOp != IBaseSQLCondition.NOT_IN_OPERATOR;
			}
			else
			{
				if (value != null && isSelectQuery(value.toString()))
				{
					if ((op & IBaseSQLCondition.IS_SQL_MODIFIER) == 0)
					{
						Debug.warn(
							"Filter is created using a custom query without using the sql-modifier, this will be removed in a future version of servoy, please use operator '" +
								RelationItem.getOperatorAsString(op | IBaseSQLCondition.IS_SQL_MODIFIER) + "'");
					}
					// add as subquery
					inValues = new QueryCustomSelect(value.toString(), null);
				}
				else
				{
					if (value != null && (op & IBaseSQLCondition.IS_SQL_MODIFIER) != 0)
					{
						Debug.warn("Filter has the sql-modifier, but the value is not valid sql for filters: " + SQL_QUERY_VALIDATION_MESSAGE + ":" + value);
					}
					inValues = new Object[][] { new Object[] { value } };
				}
			}
			// replace values with column type value
			if (inValues instanceof Object[][])
			{
				Object[][] array = (Object[][])inValues;
				for (int i = 0; i < array.length; i++)
				{
					for (int j = 0; array[i] != null && j < array[i].length; j++)
					{
						Object v = c.getAsRightType(array[i][j]);
						if (v == null) v = ValueFactory.createNullValue(c.getType());
						array[i][j] = v;
					}
				}
			}
			filterWhere = new SetCondition(op, new IQuerySelectValue[] { qColumn }, inValues, andCondition);
		}
		else if (maskedOp == IBaseSQLCondition.BETWEEN_OPERATOR || maskedOp == IBaseSQLCondition.NOT_BETWEEN_OPERATOR)
		{
			Object op1 = null;
			Object op2 = null;
			if (value instanceof List< ? > && ((List< ? >)value).size() > 1)
			{
				op1 = ((List< ? >)value).get(0);
				op2 = ((List< ? >)value).get(1);
			}
			else if (value != null && value.getClass().isArray() && ((Object[])value).length > 1)
			{
				op1 = ((Object[])value)[0];
				op2 = ((Object[])value)[1];
			}

			op1 = c.getAsRightType(op1);
			if (op1 == null) op1 = ValueFactory.createNullValue(c.getType());
			op2 = c.getAsRightType(op2);
			if (op2 == null) op2 = ValueFactory.createNullValue(c.getType());
			filterWhere = new CompareCondition(op, qColumn, new Object[] { op1, op2 });
		}
		else
		{
			Object operand;
			if (maskedOp == IBaseSQLCondition.LIKE_OPERATOR || maskedOp == IBaseSQLCondition.NOT_LIKE_OPERATOR)
			{
				operand = value;
			}
			else
			{
				operand = c.getAsRightType(value);
				if (operand == null) operand = ValueFactory.createNullValue(c.getType());
			}
			filterWhere = new CompareCondition(op, qColumn, operand);
		}

		return new QueryFilter(filterWhere);
	}

	/**
	 * A select query must start with 'select' and must contains 'from'. This may be prefixed with starting word 'with' or 'declare'
	 *
	 * @see SQLGenerator#SQL_QUERY_VALIDATION_MESSAGE
	 */
	public static boolean isSelectQuery(String value)
	{
		return value != null && value.matches("(?si)\\s*(\\b(with|declare)\\b.*)?\\bselect\\b.*\\bfrom\\b.*");
	}

	public static QuerySelect createUpdateLockSelect(QuerySelect tableSelectQuery, Collection<Object[]> pks, boolean lockInDb)
	{
		if (pks.isEmpty())
		{
			return null;
		}

		QuerySelect lockSelect = deepClone(tableSelectQuery);
		if (lockInDb)
		{
			lockSelect.setLockMode(ISQLSelect.LOCK_MODE_LOCK_NOWAIT);
		}

		List<Object[]> pklist = new ArrayList<>(pks);
		int pkSize = pklist.get(0).length;

		// values is an array as wide as the columns, each element consists of the values for that column
		Object[][] values = new Object[pkSize][];
		for (int k = 0; k < pkSize; k++)
		{
			values[k] = new Object[pklist.size()];
			for (int r = 0; r < pklist.size(); r++)
			{
				values[k][r] = pklist.get(r)[k];
			}
		}

		lockSelect.setPlaceholderValueChecked(new TablePlaceholderKey(lockSelect.getTable(), PLACEHOLDER_PRIMARY_KEY), values);
		return lockSelect;
	}

	// RAGTEST alleen in EditRecordList gebruikt
	public static Object[][] convertPKValuesForQueryCompare(Object[][] pkValues, int ncolumns)
	{
		// values is an array as wide as the columns, each element consists of the values for that column
		Object[][] values = new Object[ncolumns][];
		for (int k = 0; k < ncolumns; k++)
		{
			values[k] = new Object[pkValues.length];
			for (int r = 0; r < pkValues.length; r++)
			{
				values[k][r] = pkValues[r][k];
			}
		}
		return values;
	}

	private static ArrayList<IQuerySelectValue> makeQueryColumns(Collection<Column> columns, QueryTable queryTable, QueryInsert insert)
	{
		ArrayList<IQuerySelectValue> queryColumns = new ArrayList<>();
		List<QueryColumn> insertColumns = new ArrayList<>();

		for (Column column : columns)
		{
			ColumnInfo ci = column.getColumnInfo();
			if (ci != null && ci.isExcluded())
			{
				continue;
			}
			QueryColumn queryColumn = column.queryColumn(queryTable);

			if (isBlobColumn(column))
			{
				String alias = column.getDataProviderID()
					.substring(0,
						Math.min(Column.MAX_SQL_OBJECT_NAME_LENGTH - (IDataServer.BLOB_MARKER_COLUMN_ALIAS.length() + 1),
							column.getDataProviderID().length())) +
					'_' + IDataServer.BLOB_MARKER_COLUMN_ALIAS;
				// make sure the alias is unique (2 media columns starting with the same name may clash here)
				char c = 'a';
				for (int i = 0; i < queryColumns.size(); i++)
				{
					IQuerySelectValue sv = queryColumns.get(i);
					if (alias.equals(sv.getAlias()))
					{
						// alias not unique, replace first char to make it unique
						alias = (c++) + alias.substring(1);
						// search again
						i = 0;
					}
				}
				queryColumns.add(new QueryColumnValue(Integer.valueOf(1), alias, true));
			}
			else
			{
				queryColumns.add(queryColumn);
			}

			if (insert != null && (ci == null || !ci.isDBManaged()))
			{
				insertColumns.add(queryColumn);
			}
		}

		if (insert != null)
		{
			insert.setColumnValues(insertColumns.toArray(new QueryColumn[insertColumns.size()]),
				new Placeholder(new TablePlaceholderKey(queryTable, PLACEHOLDER_INSERT_KEY)));
		}
		return queryColumns;
	}

	/**
	 * @param column
	 * @return
	 */
	static boolean isBlobColumn(Column column)
	{
		return mapToDefaultType(column.getType()) == MEDIA && !column.hasFlag(UUID_COLUMN | IDENT_COLUMNS);
	}

	/**
	 * Create the sql for a single aggregate on a column.
	 *
	 * @param aggregee
	 * @return
	 */

	public static QuerySelect createAggregateSelect(int aggregateType, ITable table, Object aggregee)
	{
		Column column = null;
		if (aggregee instanceof Column)
		{
			column = (Column)aggregee;
		}

		QuerySelect select = new QuerySelect(table.queryTable());
		select.addColumn(new QueryAggregate(aggregateType,
			(column == null) ? (IQuerySelectValue)new QueryColumnValue(aggregee, "n", aggregee instanceof Integer || QueryAggregate.ASTERIX.equals(aggregee)) //$NON-NLS-1$
				: column.queryColumn(select.getTable()),
			"maxval")); //$NON-NLS-1$
		return select;
	}

	public static QuerySelect createAggregateSelect(QuerySelect sqlSelect, Collection<QuerySelect> aggregates, List<Column> pkColumns)
	{
		QuerySelect selectClone = deepClone(sqlSelect);
		selectClone.clearSorts();
		selectClone.setDistinct(false);
		selectClone.setColumns(null);
		selectClone.removeUnusedJoins(true, true);

		QuerySelect aggregateSqlSelect;

		if (selectClone.getJoins() == null)
		{
			// simple case, no joins
			// Select count(pk) from main where <condition>
			aggregateSqlSelect = selectClone;
		}
		else
		{
			// we have joins, change to an exists-query to make the aggregates correct, otherwise duplicate records
			// in the main table cause incorrect aggregate values
			// Select count(pk) from main main1 where exists (select 1 from main main2 join detail on detail.FK = main2.FK where main1.PK = main2.PK and <condition>)

			QuerySelect innerSelect = selectClone;

			ArrayList<IQuerySelectValue> innerColumns = new ArrayList<IQuerySelectValue>();
			innerColumns.add(new QueryColumnValue(Integer.valueOf(1), null, true));
			innerSelect.setColumns(innerColumns);

			BaseQueryTable innerTable = innerSelect.getTable();
			QueryTable outerTable = new QueryTable(innerTable.getName(), innerTable.getDataSource(), innerTable.getCatalogName(), innerTable.getSchemaName());

			aggregateSqlSelect = new QuerySelect(outerTable);
			for (Column column : pkColumns)
			{
				innerSelect.addCondition("EXISTS", //$NON-NLS-1$
					new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, column.queryColumn(innerTable), column.queryColumn(outerTable)));
			}
			aggregateSqlSelect.addCondition("EXISTS", new ExistsCondition(innerSelect, true)); //$NON-NLS-1$
		}

		ArrayList<IQuerySelectValue> columns = new ArrayList<IQuerySelectValue>();
		for (QuerySelect aggregate : aggregates)
		{
			columns.addAll(AbstractBaseQuery.relinkTable(aggregate.getTable(), aggregateSqlSelect.getTable(), aggregate.getColumnsClone()));
		}
		aggregateSqlSelect.setColumns(columns);

		return aggregateSqlSelect;
	}

	/**
	 * Create a condition for comparing a column like a value.
	 *
	 * @param selectValue
	 * @param column
	 * @param value
	 * @return
	 */
	public static CompareCondition createLikeCompareCondition(IQuerySelectValue selectValue, int dataProviderType, String value)
	{
		IQuerySelectValue likeSelectValue;
		if (mapToDefaultType(dataProviderType) == IColumnTypes.TEXT)
		{
			likeSelectValue = new QueryFunction(upper, selectValue, null);
		}
		else
		{
			likeSelectValue = new QueryFunction(castfrom,
				new IQuerySelectValue[] { selectValue, new QueryColumnValue(IQueryConstants.TYPE_INTEGER, null,
					true), new QueryColumnValue(IQueryConstants.TYPE_STRING, null, true) },
				null);
		}
		return new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, likeSelectValue, value);
	}


}
