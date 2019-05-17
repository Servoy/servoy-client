/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.query;

import static com.servoy.base.query.IJoinConstants.FULL_JOIN;
import static com.servoy.base.query.IJoinConstants.INNER_JOIN;
import static com.servoy.base.query.IJoinConstants.LEFT_OUTER_JOIN;
import static com.servoy.base.query.IJoinConstants.RIGHT_OUTER_JOIN;

import com.servoy.base.query.BaseQueryTable;

/** Interface for joins in queries.
 * @author rgansevles
 *
 */
public interface ISQLJoin extends IQueryElement
{
	public static final int[] ALL_JOIN_TYPES = { INNER_JOIN, LEFT_OUTER_JOIN, RIGHT_OUTER_JOIN, FULL_JOIN };

	public static final String[] JOIN_TYPES_NAMES = { "inner join", //$NON-NLS-1$
		"left outer join", //$NON-NLS-1$
		"right outer join", //$NON-NLS-1$
		"full join" //$NON-NLS-1$
	};

	public abstract String getName();

	public abstract BaseQueryTable getPrimaryTable();

	/**
	 * The object the join was created for (for example, a table filter)
	 */
	public abstract void setOrigin(Object origin);

	public abstract Object getOrigin();
}
