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
 * <li> list of prepares, may be create temp table, insert into, etc
 * <li> select/update
 * <li> list of cleanups, for dropping temp tables
 * </ul>
 * 
 * @author rgansevles
 * 
 */
public class QuerySet implements Serializable
{
	private List<QueryString> prepares = null;
	private QueryString select = null;
	private QueryString update = null;
	private List<QueryString> cleanups = null;

	public void setSelect(QueryString select)
	{
		setSelectOrUpdate(select, true);
	}

	public void setUpdate(QueryString update)
	{
		setSelectOrUpdate(update, false);
	}

	private void setSelectOrUpdate(QueryString selectOrUpdate, boolean isSelect)
	{
		if ((this.select != null || this.update != null) && selectOrUpdate != null)
		{
			throw new IllegalArgumentException("Multiple select/update queries in query set"); //$NON-NLS-1$
		}
		if (isSelect)
		{
			this.select = selectOrUpdate;
		}
		else
		{
			this.update = selectOrUpdate;
		}
	}

	public QueryString getSelect()
	{
		return select;
	}

	public QueryString getUpdate()
	{
		return update;
	}

	public void addPrepare(QueryString prepare)
	{
		if (prepares == null)
		{
			prepares = new ArrayList<QueryString>();
		}
		prepares.add(prepare);
	}

	public void addCleanup(QueryString cleanup)
	{
		if (cleanups == null)
		{
			cleanups = new ArrayList<QueryString>();
		}
		cleanups.add(cleanup);
	}

	public QueryString[] getCleanups()
	{
		return cleanups == null ? null : (QueryString[])cleanups.toArray(new QueryString[cleanups.size()]);
	}

	public QueryString[] getPrepares()
	{
		return prepares == null ? null : (QueryString[])prepares.toArray(new QueryString[prepares.size()]);
	}

	/**
	 * Join the queries from another set with this one.
	 * The set parameter's update will be added as a prepare statement.
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

		QueryString[] newPrepares = set.getPrepares();
		if (newPrepares != null)
		{
			if (prepares == null)
			{
				prepares = new ArrayList<QueryString>();
			}
			prepares.addAll(Arrays.asList(newPrepares));
		}
		if (set.getUpdate() != null)
		{
			addPrepare(set.getUpdate());
		}

		QueryString[] newCleanups = set.getCleanups();
		if (newCleanups != null)
		{
			if (cleanups == null)
			{
				cleanups = new ArrayList<QueryString>();
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
		sb.append("QuerySet { prepares = "); //$NON-NLS-1$
		sb.append(AbstractBaseQuery.toString(prepares));
		if (select != null)
		{
			sb.append(", select = "); //$NON-NLS-1$
			sb.append(AbstractBaseQuery.toString(select));
		}
		if (update != null)
		{
			sb.append(", update = "); //$NON-NLS-1$
			sb.append(AbstractBaseQuery.toString(update));
		}
		sb.append(", cleanups = "); //$NON-NLS-1$
		sb.append(AbstractBaseQuery.toString(cleanups));
		sb.append(" }"); //$NON-NLS-1$
		return sb.toString();
	}
}
