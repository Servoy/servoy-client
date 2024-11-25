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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.query.SortOptions;
import com.servoy.j2db.querybuilder.IQueryBuilderSort;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * <p>The <code>QBSort</code> class provides a wrapper for defining SQL sort conditions within the
 * <code>QBSelect</code> framework. It allows for flexible and precise specification of sorting rules,
 * integrating seamlessly with parent and root query references. Main functions include configuring
 * ascending (<code>asc</code>) and descending (<code>desc</code>) sort orders, supporting diverse
 * query components.</p>
 *
 * <p>For additional details on query construction and execution, refer to the
 * <a href="./qbselect.md">QBSelect documentation</a>.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBSort")
public class QBSort extends QBPart implements IQueryBuilderSort
{
	private final boolean ascending;
	private final QBColumn queryBuilderColumn;

	public QBSort(QBSelect parent, QBColumn queryBuilderColumn, boolean ascending)
	{
		super(parent, parent);
		this.queryBuilderColumn = queryBuilderColumn;
		this.ascending = ascending;
	}

	@Override
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	public QuerySort getQueryQuerySort()
	{
		return new QuerySort(queryBuilderColumn.getQuerySelectValue(), ascending, SortOptions.NONE);
	}
}
