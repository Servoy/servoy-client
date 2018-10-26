/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.server.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.server.shared.PerformanceData.TimeComparator;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;

/**
 * A class that can aggregate the results of multiple actions/operations.
 *
 * @author acostescu
 */
public class PerformanceAggregator
{

	public static final int DEFAULT_MAX_ENTRIES_TO_KEEP_IN_PRODUCTION = 500;

	private SortedList<PerformanceTimingAggregate> sortedAggregates;
	private Map<String, PerformanceTimingAggregate> aggregatesByAction;

	protected final int maxEntriesToKeep;

	public PerformanceAggregator(int maxEntriesToKeep)
	{
		this.maxEntriesToKeep = maxEntriesToKeep;
	}

	/**
	 * Creates a clone/copy.
	 */
	public PerformanceAggregator(PerformanceAggregator copy)
	{
		this.maxEntriesToKeep = copy.maxEntriesToKeep;
		if (copy.sortedAggregates != null)
		{
			sortedAggregates = new SortedList<PerformanceTimingAggregate>(new TimeComparator());
			aggregatesByAction = new HashMap<String, PerformanceTimingAggregate>(maxEntriesToKeep);
			for (PerformanceTimingAggregate a : copy.sortedAggregates)
			{
				PerformanceTimingAggregate copyOfA = new PerformanceTimingAggregate(a);
				sortedAggregates.add(copyOfA);
				aggregatesByAction.put(a.getAction(), copyOfA);
			}
		}
	}

	/**
	 * Please use {@link #startAction(String, long, int)} / {@link #endAction(UUID)} / {@link #intervalAction(UUID)} whenever possible instead.
	 */
	public synchronized void addTiming(String action, long interval_ms, long total_ms, int type, Map<String, PerformanceTimingAggregate> subActionTimings)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return;

		if (aggregatesByAction == null) aggregatesByAction = new HashMap<String, PerformanceTimingAggregate>(
			maxEntriesToKeep > 0 ? maxEntriesToKeep : DEFAULT_MAX_ENTRIES_TO_KEEP_IN_PRODUCTION);
		if (sortedAggregates == null) sortedAggregates = new SortedList<PerformanceTimingAggregate>(new TimeComparator());

		PerformanceTimingAggregate time = aggregatesByAction.get(action);
		if (time == null)
		{
			time = new PerformanceTimingAggregate(action, type, getSubActionMaxEntries());
			aggregatesByAction.put(action, time);
		}
		// remove it because it will need to be sorted again after update
		sortedAggregates.remove(time);

		// update obj
		time.updateTime(interval_ms, total_ms);
		time.updateSubActionTimes(subActionTimings);

		// do sort again
		sortedAggregates.add(time);

		// do clean
		if (maxEntriesToKeep != IPerformanceRegistry.UNLIMITED_ENTRIES && sortedAggregates.size() > maxEntriesToKeep)
		{
			PerformanceTimingAggregate old = sortedAggregates.remove(maxEntriesToKeep);
			aggregatesByAction.remove(old.getAction());
		}
	}

	public synchronized void clear()
	{
		if (sortedAggregates != null) sortedAggregates.clear();
		if (aggregatesByAction != null) aggregatesByAction.clear();
	}

	public synchronized PerformanceTimingAggregate[] toArray()
	{
		return (sortedAggregates != null ? sortedAggregates.toArray(new PerformanceTimingAggregate[sortedAggregates.size()])
			: new PerformanceTimingAggregate[0]);
	}

	public synchronized Map<String, PerformanceTimingAggregate> toMap()
	{
		return (aggregatesByAction != null ? Collections.unmodifiableMap(aggregatesByAction) : new HashMap<String, PerformanceTimingAggregate>());
	}

	protected int getSubActionMaxEntries()
	{
		return maxEntriesToKeep == IPerformanceRegistry.UNLIMITED_ENTRIES ? IPerformanceRegistry.UNLIMITED_ENTRIES : Math.max(5, maxEntriesToKeep / 10);
	}

}
