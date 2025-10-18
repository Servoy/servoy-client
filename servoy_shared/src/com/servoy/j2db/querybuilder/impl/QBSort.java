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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.SortingNullprecedence;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.SortOptions;
import com.servoy.j2db.querybuilder.IQueryBuilderSort;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBSort</code> class provides a wrapper for defining SQL sort conditions within the
 * <code>QBSelect</code> framework. It allows for flexible and precise specification of sorting rules,
 * integrating seamlessly with parent and root query references. Main functions include configuring
 * ascending (<code>asc</code>) and descending (<code>desc</code>) sort orders, supporting diverse
 * query components.</p>
 *
 * <p>For additional details on query construction and execution, refer to the
 * <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/qbselect">QBSelect documentation</a>.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBSort extends QBPart implements IQueryBuilderSort, IConstantsObject
{
	private final boolean ascending;
	private final QBColumnImpl queryBuilderColumn;
	private final QBNullPrecedence nullPrecedence;
	private final Boolean ignoreCase;

	public QBSort(QBSelect parent, QBColumnImpl queryBuilderColumn, boolean ascending)
	{
		this(parent, queryBuilderColumn, ascending, null, null);
	}

	public QBSort(QBSelect parent, QBColumnImpl queryBuilderColumn, boolean ascending, QBNullPrecedence nullPrecedence, Boolean ignoreCase)
	{
		super(parent, parent);
		this.queryBuilderColumn = queryBuilderColumn;
		this.ascending = ascending;
		this.nullPrecedence = nullPrecedence;
		this.ignoreCase = ignoreCase;
	}

	@Override
	@JSReadonlyProperty(debuggerRepresentation = "Query parent part")
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	/** RAGTEST doc
	 * Add a custom subquery with alias to the query result.
	 * @sample
	 * // make sure the subquery returns exactly 1 value.
	 * // select (select max from othertab where val = 'test') as mx from tab
	 * query.result.addSubSelect("select max from othertab where val = ?", ["test"], "mx");
	 *
	 * @param customQuery query to add to result
	 * @param args arguments to the query
	 * @param alias result alias
	 *
	 * @return The query result object with the specified custom subquery and alias added.
	 */
	@JSFunction
	public QBSort ignoreCase(boolean newIgnoreCase)
	{
		return new QBSort(getParent(), queryBuilderColumn, ascending, nullPrecedence, Boolean.valueOf(newIgnoreCase));
	}

	/** RAGTEST doc
	 * Add a custom subquery with alias to the query result.
	 * @sample
	 * // make sure the subquery returns exactly 1 value.
	 * // select (select max from othertab where val = 'test') as mx from tab
	 * query.result.addSubSelect("select max from othertab where val = ?", ["test"], "mx");
	 *
	 * @param customQuery query to add to result
	 * @param args arguments to the query
	 * @param alias result alias
	 *
	 * @return The query result object with the specified custom subquery and alias added.
	 */
	@JSFunction
	public QBSort nullPrecedence(QBNullPrecedence newNullPrecedence)
	{
		if (newNullPrecedence == null)
		{
			throw new IllegalArgumentException("nullPrecedence must not be null");
		}

		return new QBSort(getParent(), queryBuilderColumn, ascending, newNullPrecedence, ignoreCase);
	}

	public QuerySort getQueryQuerySort()
	{
		IQuerySelectValue querySelectValue = queryBuilderColumn.getQuerySelectValue();
		SortOptions sortOptions = getParent().getSortOptions(querySelectValue);
		if (ignoreCase != null)
		{
			sortOptions = sortOptions.withIgnoreCase(ignoreCase.booleanValue());
		}
		if (nullPrecedence != null)
		{
			SortingNullprecedence sortingNullprecedence;
			switch (nullPrecedence)
			{
				case nullsFirst :
					sortingNullprecedence = ascending ? SortingNullprecedence.ascNullsFirst : SortingNullprecedence.ascNullsLast;
					break;
				case nullsLast :
					sortingNullprecedence = ascending ? SortingNullprecedence.ascNullsLast : SortingNullprecedence.ascNullsFirst;
					break;
				default : /* databaseDefault */
					sortingNullprecedence = SortingNullprecedence.databaseDefault;
					break;
			}

			sortOptions = sortOptions.withNullprecedence(sortingNullprecedence);
		}

		return new QuerySort(querySelectValue, ascending, sortOptions);
	}
}
