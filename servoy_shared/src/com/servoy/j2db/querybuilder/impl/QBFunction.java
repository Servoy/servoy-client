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
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryFunction;
import com.servoy.j2db.query.QueryFunction.QueryFunctionType;
import com.servoy.j2db.querybuilder.IQueryBuilderFunction;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBFunction")
public class QBFunction extends QBColumn implements IQueryBuilderFunction
{
	private final QueryFunctionType functionType;
	private final IQuerySelectValue[] functionArgs;

	QBFunction(QBSelect root, QBTableClause queryBuilderTableClause, QueryFunctionType functionType, IQuerySelectValue[] functionArgs)
	{
		super(root, queryBuilderTableClause, null);
		this.functionType = functionType;
		this.functionArgs = functionArgs;
	}

	@Override
	public IQuerySelectValue getQuerySelectValue()
	{
		return new QueryFunction(functionType, functionArgs, null);
	}

	@Override
	public String toString()
	{
		return (negate ? "!" : "") + functionType.name() + "(<args>)";
	}
}
