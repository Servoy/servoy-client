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

import com.servoy.j2db.querybuilder.IQueryBuilderPart;
import com.servoy.j2db.querybuilder.IQueryBuilderTableClause;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;


/**
 * @author rgansevles
 *
 */
public abstract class AbstractQueryBuilderPart<P extends IQueryBuilderTableClause> implements IQueryBuilderPart
{
	private final QueryBuilder root;
	private final P parent;

	@SuppressWarnings("unchecked")
	AbstractQueryBuilderPart()
	{
		this.root = (QueryBuilder)this;
		this.parent = (P)this;
	}

	AbstractQueryBuilderPart(QueryBuilder root, P parent)
	{
		this.root = root;
		this.parent = parent;
	}

	@JSReadonlyProperty
	final public P getParent()
	{
		return parent;
	}

	@JSReadonlyProperty
	final public QueryBuilder getRoot()
	{
		return root;
	}
}
