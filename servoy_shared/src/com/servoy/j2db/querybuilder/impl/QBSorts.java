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

import static com.servoy.j2db.util.Utils.iterate;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderSort;
import com.servoy.j2db.querybuilder.IQueryBuilderSorts;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBSorts</code> class is a versatile utility for managing sorting conditions in
 * <code>QBSelect</code> queries. It enables developers to define ascending or descending sort orders
 * for columns and functions, offering granular control over query result organization. This class
 * integrates with other query builder components, allowing for dynamic updates to sorting criteria
 * during query construction.</p>
 *
 * <p>With support for adding primary key columns automatically in alphabetical order,
 * <code>QBSorts</code> streamlines the sorting process in complex queries. Additionally, it includes
 * features for clearing sorting conditions, ensuring flexibility and adaptability. Key methods
 * include <code>add(columnSortAsc)</code> for adding sort orders, <code>addPk()</code> for incorporating
 * primary key columns, and <code>clear()</code> for resetting the sort configuration.</p>
 *
 * <p>For more details, see the <a href="./qbselect.md">QBSelect documentation</a>.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBSorts extends QBPart implements IQueryBuilderSorts
{
	/**
	 * @param queryBuilder
	 */
	QBSorts(QBSelect parent)
	{
		super(parent, parent);
	}

	@Override
	@JSReadonlyProperty
	public final QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderSorts#addPk()
	 * @sample
	 *
	 * query.sort.addPk()
	 */
	@JSFunction
	public QBSorts addPk() throws RepositoryException
	{
		ITable table = getParent().getTable();
		if (table != null)
		{
			for (String columnName : iterate(table.getRowIdentColumnNames()))
			{
				add(getParent().getColumn(columnName).asc());
			}
		}
		return this;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderSorts#clear()
	 * @sample
	 *
	 * query.sort.clear()
	 */
	@JSFunction
	public QBSorts clear()
	{
		getParent().getQuery().clearSorts();
		return this;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderSorts#add(IQueryBuilderSort)
	 * @sample
	 * query.sort.add(query.columns.orderid.desc)
	 *
	 * @param sort the sort to add
	 */
	public QBSorts js_add(QBSort sort) throws RepositoryException
	{
		return add(sort);
	}

	public QBSorts add(IQueryBuilderSort sort) throws RepositoryException
	{
		getParent().getQuery().addSort(((QBSort)sort).getQueryQuerySort());
		return this;
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderSorts#add(IQueryBuilderColumn)
	 * @sample
	 * query.sort.add(query.columns.orderid)
	 *
	 * @param columnSortAsc column to sort by
	 */
	public QBSorts js_add(QBColumn columnSortAsc) throws RepositoryException
	{
		return add(columnSortAsc);
	}

	/**
	 * @clonedesc com.servoy.j2db.querybuilder.IQueryBuilderSorts#add(IQueryBuilderColumn)
	 * @sample
	 * query.sort.add(query.columns.orderid)
	 *
	 * @param functionSortAsc function to add
	 */
	public QBSorts js_add(QBFunction functionSortAsc) throws RepositoryException
	{
		return add(functionSortAsc);
	}

	public QBSorts add(IQueryBuilderColumn columnSortAsc) throws RepositoryException
	{
		return add(columnSortAsc.asc());
	}

	@Override
	public String toString()
	{
		return "QBSorts(Helper class for creating functions)";
	}
}
