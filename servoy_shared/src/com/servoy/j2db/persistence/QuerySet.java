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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.servoy.j2db.query.AbstractBaseQuery;

/**
 * Container for all parts that may be needed for running a query, all parts are optional.
 * <ul>
 * <li> list of updates, may be create temp table, insert into, etc
 * <li> select
 * <li> list of cleanups, for dropping temp tables
 * </ul>
 * 
 * @author rob
 * 
 */
public class QuerySet implements Serializable
{
	private List updates = null;
	private QueryString select = null;
	private List cleanups = null;


	public void setSelect(QueryString select)
	{
		if (this.select != null && select != null)
		{
			throw new IllegalArgumentException("Multiple select queries in query set"); //$NON-NLS-1$
		}
		this.select = select;
	}

	public void addUpdate(QueryString update)
	{
		if (updates == null)
		{
			updates = new ArrayList();
		}
		updates.add(update);
	}

	public void addCleanup(QueryString cleanup)
	{
		if (cleanups == null)
		{
			cleanups = new ArrayList();
		}
		cleanups.add(cleanup);
	}

	public QueryString[] getCleanups()
	{
		return cleanups == null ? null : (QueryString[])cleanups.toArray(new QueryString[cleanups.size()]);
	}

	public QueryString getSelect()
	{
		return select;
	}

	public QueryString[] getUpdates()
	{
		return updates == null ? null : (QueryString[])updates.toArray(new QueryString[updates.size()]);
	}

	/**
	 * Join the queries from another set with this one.
	 * 
	 * @param set
	 */
	public void addQuerySet(QuerySet set)
	{
		if (set == null)
		{
			return;
		}
		setSelect(set.getSelect());

		QueryString[] newUpdates = set.getUpdates();
		if (newUpdates != null)
		{
			if (updates == null)
			{
				updates = new ArrayList();
			}
			updates.addAll(Arrays.asList(newUpdates));
		}

		QueryString[] newCleanups = set.getCleanups();
		if (newCleanups != null)
		{
			if (cleanups == null)
			{
				cleanups = new ArrayList();
			}
			cleanups.addAll(Arrays.asList(newCleanups));
		}
	}

	/**
	 * 
	 */
	public void clearSelect()
	{
		select = null;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("QuerySet { updates = "); //$NON-NLS-1$
		sb.append(AbstractBaseQuery.toString(updates));
		sb.append(", select = "); //$NON-NLS-1$
		sb.append(AbstractBaseQuery.toString(select));
		sb.append(", cleanups = "); //$NON-NLS-1$
		sb.append(AbstractBaseQuery.toString(cleanups));
		sb.append(" }"); //$NON-NLS-1$
		return sb.toString();
	}
}
