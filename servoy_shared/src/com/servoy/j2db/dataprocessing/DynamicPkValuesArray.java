/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import java.util.List;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.query.IDynamicValue;

/**
 * Holder for PK values in data set to be used in a Query condition.
 * The data set can be updated with newly added values.
 * 
 * @author rgansevles
 * 
 * @since 6.1.1
 *
 */
public class DynamicPkValuesArray implements IDynamicValue, Cloneable
{
	private final List<Column> pkColumns;
	private final IDataSet pks;

	/**
	 * @param pks
	 * @param columnTypeInfo
	 */
	public DynamicPkValuesArray(List<Column> pkColumns, IDataSet pks)
	{
		this.pkColumns = pkColumns;
		this.pks = pks;
	}

	public Object getValue()
	{
		// get the actual values for SetCondition
		return SQLGenerator.createPKValuesArray(pkColumns, pks);
	}

	public IDataSet getPKs()
	{
		return pks;
	}

	public List<Column> getPkColumns()
	{
		return pkColumns;
	}

	@Override
	public DynamicPkValuesArray clone()
	{
		// Used by DeepCloneVisitor
		// pkColumns never changes, pks are updated so clone
		return new DynamicPkValuesArray(pkColumns, pks.clone());
	}
}
