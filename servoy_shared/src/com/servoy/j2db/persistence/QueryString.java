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
package com.servoy.j2db.persistence;

import java.io.Serializable;

import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Utils;

/**
 * Container for 1 sql string with parameters.
 * 
 * @author rgansevles
 * 
 */
public class QueryString implements Serializable
{
	private String sql;
	private Object[][] parameters;
	private ColumnType[] typeInfo;
	private final boolean performInIsolation;
	private boolean limitApplied;
	private int startRow;
	private int rowsToRetrieve;

	public QueryString(String sql, Object[][] parameters, ColumnType[] typeInfo, boolean performInIsolation)
	{
		this.sql = sql;
		this.parameters = parameters;
		this.typeInfo = typeInfo;
		this.performInIsolation = performInIsolation;
	}

	public QueryString(String sql, Object[][] parameters, boolean performInIsolation)
	{
		this(sql, parameters, null, performInIsolation);
	}

	public QueryString(String sql, boolean performInIsolation)
	{
		this(sql, new Object[][] { null }, null, performInIsolation);
	}

	public Object[][] getParameters()
	{
		return parameters;
	}

	public ColumnType[] getTypeInfo()
	{
		return typeInfo;
	}

	public String getSql()
	{
		return sql;
	}

	public void setSql(String sql)
	{
		this.sql = sql;
	}

	public boolean performInIsolation()
	{
		return performInIsolation;
	}

	public boolean isLimitApplied()
	{
		return limitApplied;
	}

	public int getRowsToRetrieve()
	{
		return rowsToRetrieve;
	}

	public int getStartRow()
	{
		return startRow;
	}

	public void setLimitApplied(boolean b)
	{
		limitApplied = b;
	}

	public void setRowsToRetrieve(int n)
	{
		rowsToRetrieve = n;
	}

	public void setStartRow(int n)
	{
		startRow = n;
	}

	/**
	 * Add parameters and type info
	 * 
	 * @param parms
	 * @param ti
	 * @param position insert position
	 */
	public void addParameters(Object[][] parms, ColumnType[] ti, int position)
	{
		if (parms.length != parameters.length)
		{
			throw new IllegalArgumentException("New parameters not consistent with existing:" + toString()); //$NON-NLS-1$
		}
		if (position < 0 || (parameters.length > 0 && parameters[0] != null && position > parameters[0].length) || parameters.length > 0 &&
			parameters[0] == null && position > 0)
		{
			throw new IllegalArgumentException("Invalid position index " + position + ": " + toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		for (int i = 0; i < parameters.length; i++)
		{
			parameters[i] = Utils.arrayInsert(parameters[i], parms[i], position, parms[i].length);
			// maintain the type info as well (once)
			if (i == 0)
			{
				// use parms[0].length in stead of ti.length here to make sure parameters and typeInfo stay same width (and ti maybe null) 
				typeInfo = Utils.arrayInsert(typeInfo, ti, position, parms[0].length);
			}
		}
	}

	/**
	 * @param parameters
	 * @param typeInfo
	 */
	public void setParametersAndTypeInfo(Object[][] parameters, ColumnType[] typeInfo)
	{
		this.parameters = parameters;
		this.typeInfo = typeInfo;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer(sql);
		if (parameters != null)
		{
			sb.append(' ');
			sb.append(AbstractBaseQuery.toString(parameters));
		}
		if (limitApplied)
		{
			sb.append(" <"); //$NON-NLS-1$
			sb.append(startRow);
			sb.append('^');
			sb.append(rowsToRetrieve);
			sb.append('>');
		}
		if (performInIsolation)
		{
			sb.append(" *"); //$NON-NLS-1$
		}
		return sb.toString();
	}
}
