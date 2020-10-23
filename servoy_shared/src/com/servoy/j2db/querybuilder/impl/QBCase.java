/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QuerySearchedCaseExpression;
import com.servoy.j2db.query.QueryWhenClause;
import com.servoy.j2db.querybuilder.IQueryBuilderFunction;
import com.servoy.j2db.util.Pair;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBCase")
public class QBCase extends QBColumn implements IQueryBuilderFunction
{
	private final List<Pair<QBCondition, Object>> whenThen = new ArrayList<>();
	private Object elseValue;

	QBCase(QBSelect root, QBTableClause queryBuilderTableClause)
	{
		super(root, queryBuilderTableClause, null);
	}

	/** RAGTEST doc
	 */
	@JSFunction
	public QBCaseWhen when(QBCondition condition)
	{
		return new QBCaseWhen(this, condition);
	}

	/** RAGTEST doc
	 */
	@JSFunction
	public QBCase elseValue(Object value)
	{
		this.elseValue = value;
		return this;
	}

	QBCase withWhenThen(QBCondition whenCondition, Object thenValue)
	{
		whenThen.add(new Pair<>(whenCondition, thenValue));
		return this;
	}


	@Override
	public IQuerySelectValue getQuerySelectValue()
	{
		List<QueryWhenClause> qWhenThen = whenThen.stream()
			.map(pair -> new QueryWhenClause(pair.getLeft().getQueryCondition(), getRoot().createOperand(pair.getRight(), null, 0)))
			.collect(Collectors.toList());

		IQuerySelectValue qElseValue = getRoot().createOperand(elseValue, null, 0);

		return new QuerySearchedCaseExpression(qWhenThen, qElseValue, null);
	}

//	@Override
//	public String toString()
//	{
//		return (negate ? "!" : "") + functionType.name() + "(<args>)";
//	}
}
