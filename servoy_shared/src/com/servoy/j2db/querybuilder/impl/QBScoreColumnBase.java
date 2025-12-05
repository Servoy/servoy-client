/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import com.servoy.j2db.scripting.annotations.JSRealClass;

/**
 *
 * This interface lists functions on vector score columns.
 *
 * @author rgansevles
 *
 */
@JSRealClass(QBScoreColumn.class)
public interface QBScoreColumnBase extends QBNumberColumnBase
{
	/**
	 * Compare normalized score column with a value or another column.
	 * The normalized score is a number from 0 to 1 where higher means better.
	 *
	 * This function is equivalent to 'vector_score(embedding).ge(score)', but optimized for the specific database implementation.
	 *
	 * Operator: min_score
	 * @param value
	 * @sample
	 * query.where.add(query.joins.books_to_books_embeddings.columns.embedding.vector_score(model.embedding('Magic or Fantasy')).min_score(0.7))
	 *
	 *  @return a QBCondition representing the "minimal score" comparison.
	 */

	@JSFunction
	QBCondition min_score(Object normalizedScore);
}
