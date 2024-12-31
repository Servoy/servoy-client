/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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
import com.servoy.j2db.querybuilder.IQueryBuilderCaseWhen;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.scripting.IJavaScriptType;

/**
 * <p>The <code>QBCaseWhen</code> class is a utility for defining <code>WHEN</code> conditions within
 * an SQL <code>CASE</code> expression in a <code>QBSelect</code> query. It specifies the conditions
 * that, when met, determine the result of the <code>CASE</code> expression.</p>
 *
 * <p>The <code>then</code> method assigns the value to return when the specified <code>WHEN</code>
 * condition is satisfied. This enables the construction of dynamic and complex conditional logic
 * directly within SQL queries.</p>
 *
 * <p>For more information about constructing and executing queries, refer to the
 * <a href="./qbselect.md">QBSelect</a> section of this documentation.</p>
 *
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "QBCaseWhen")
public class QBCaseWhen implements IQueryBuilderCaseWhen, IJavaScriptType
{
	private final QBCase parent;
	private final IQueryBuilderCondition whenCondition;

	QBCaseWhen(QBCase parent, IQueryBuilderCondition whenCondition)
	{
		this.parent = parent;
		this.whenCondition = whenCondition;
	}

	/**
	 * Set the return value to use when the condition of the searched case expression is met.
	 *
	 * @param value The value.
	 *
	 * @sampleas com.servoy.j2db.querybuilder.impl.QBSelect#js_case()
	 *
	 * @return the parent `QBCase` object with the associated condition and value applied.
	 */
	@JSFunction
	public QBCase then(Object value)
	{
		return parent.withWhenThen(whenCondition, value);
	}
}
