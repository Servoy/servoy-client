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

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseQueryElement;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.ReplaceVisitor;

/**
 * Common interface for all elements in the query structure.
 *
 * @author rgansevles
 *
 */
public interface IQueryElement extends IBaseQueryElement, ISQLCloneable, IWriteReplace, IVisitable
{
	/**
	 * Replace references to orgTable with newTable.
	 */
	default <T extends IQueryElement> T relinkTable(BaseQueryTable orgTable, BaseQueryTable newTable)
	{
		return (T)AbstractBaseQuery.acceptVisitor(this, new ReplaceVisitor(orgTable, newTable, true));
	}

	/**
	 * Create a clone that can be modified without modifying the original
	 */
	default <T extends IQueryElement> T deepClone()
	{
		return (T)AbstractBaseQuery.deepClone(this);
	}

}
