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
package com.servoy.j2db.dataprocessing;


import java.util.EventObject;

import javax.swing.event.TableModelEvent;

/**
 * Row event object
 * @author jblok
 */
class RowEvent extends EventObject
{
	public static final int INSERT = TableModelEvent.INSERT; // 1
	public static final int UPDATE = TableModelEvent.UPDATE; // 0
	public static final int DELETE = TableModelEvent.DELETE; // -1
	public static final int PK_UPDATED = 2;

	private final Row row;
	private final int type;
	private final Object data;
	private final boolean isAggregateChange;
	private final String pkHashKey;

	RowEvent(Object source, Row row, int type, Object data)
	{
		this(source, row, row.getPKHashKey(), type, data, false);
	}

	RowEvent(Object source, Row row, String pkHashKey, int type, Object data, boolean isAggregateChange)
	{
		super(source);
		this.row = row;
		this.pkHashKey = pkHashKey;
		this.type = type;
		this.data = data;
		this.isAggregateChange = isAggregateChange;
	}

	/**
	 * Returns the type.
	 *
	 * @return int
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Returns the row.
	 *
	 * @return Row
	 */
	public Row getRow()
	{
		return row;
	}

	/**
	 * @return the pkHashKey
	 */
	public String getPkHashKey()
	{
		if (pkHashKey == null && row != null) return row.getPKHashKey();
		return pkHashKey;
	}

	/**
	 * Get old pk hash, only used with PK_UPDATED events
	 * @return
	 */
	public String getOldPkHash()
	{
		return type == PK_UPDATED && data instanceof String ? (String)data : null;
	}

	/**
	 * Which columns have been updated, only used with UPDATE events
	 */
	public Object[] getChangedColumnNames()
	{
		return (type == UPDATE || type == PK_UPDATED) && data instanceof Object[] ? (Object[])data : null;
	}

	public boolean isAggregateChange()
	{
		return isAggregateChange;
	}
}
