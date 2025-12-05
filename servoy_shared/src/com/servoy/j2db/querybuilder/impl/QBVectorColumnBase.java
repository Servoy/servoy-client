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

import com.servoy.j2db.querybuilder.IQueryBuilderColumn;
import com.servoy.j2db.scripting.annotations.JSRealClass;

/**
 * This interface lists functions on vector columns.
 *
 * @author rgansevles
 *
 */
@JSRealClass(QBVectorColumn.class)
public interface QBVectorColumnBase extends IQueryBuilderColumn
{
	/**
	 * Calculate the normalized score for this column using the embedding.
	 *
	 * The normalized score is a number from 0 to 1 where higher means better.
	 * When sorting on score you can sort on 'vector_score(embedding) desc' to get the best matches first.
	 * It is more efficient to sort using the native distance function 'vector_distance(embedding) asc' to get the best matches first.
	 *
	 * The score result has a function min_score for filtering on the score, this is optimized for filtering compared to the standard number comparison functions.
	 *
	 * @param embedding embedding object
	 * @sample
	 * query.result.addquery.joins.books_to_books_embeddings.columns.embedding.vector_score(model.embedding('Magic or Fantasy'))
	 *
	 * @return the QBScoreColumn that can be added to the result.
	 */
	@JSFunction
	public QBScoreColumnBase vector_score(float[] embedding);

	/**
	 * Calculate the database native cosine distance for this column using the embedding.
	 *
	 * The native cosine distance is a positive number where lower means better.
	 * When sorting on score you can sort on 'vector_distance(embedding) asc' to get the best matches first.
	 *
	 * @param embedding embedding object
	 * @sample
	 * query.sort.add(query.joins.books_to_books_embeddings.columns.embedding.vector_distance(model.embedding('Magic or Fantasy')))
	 *
	 * @return the QBNumberColumn that can be used for sorting.
	 */
	@JSFunction
	public QBNumberColumnBase vector_distance(float[] embedding);
}
