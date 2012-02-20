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

import java.util.Iterator;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.querybuilder.IQueryBuilderResult;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBResult extends QBPart implements IQueryBuilderResult
{
	/**
	 * @param queryBuilder
	 */
	QBResult(QBSelect parent)
	{
		super(parent, parent);
	}

	@Override
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	@JSFunction
	public QBResult addPk() throws RepositoryException
	{
		Iterator<String> rowIdentColumnNames = getParent().getTable().getRowIdentColumnNames();
		while (rowIdentColumnNames.hasNext())
		{
			add(rowIdentColumnNames.next());
		}
		return this;
	}

	@JSFunction
	public QBResult add(String columnName) throws RepositoryException
	{
		return add(getParent().getColumn(columnName));
	}

	public QBResult js_add(QBColumn column) throws RepositoryException
	{
		return add(column);
	}

	public QBResult add(IQueryBuilderColumn column) throws RepositoryException
	{
		getParent().getQuery().addColumn(((QBColumn)column).getQuerySelectValue());
		return this;
	}

	public QBResult js_add(QBAggregate aggregate) throws RepositoryException
	{
		return add(aggregate);
	}

	public QBResult js_add(QBFunction func) throws RepositoryException
	{
		return add(func);
	}

	@JSFunction
	public QBResult addValue(Object value) throws RepositoryException
	{
		getParent().getQuery().addColumn(new QueryColumnValue(value, null, value instanceof Integer));
		return this;
	}

	public void js_setDistinct(boolean distinct) throws RepositoryException
	{
		setDistinct(distinct);
	}

	public boolean js_isDistinct() throws RepositoryException
	{
		return isDistinct();
	}

	public boolean isDistinct() throws RepositoryException
	{
		return getParent().getQuery().isDistinct();
	}

	public QBResult setDistinct(boolean distinct) throws RepositoryException
	{
		getParent().getQuery().setDistinct(distinct);
		return this;
	}
}
