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

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

	private final ConcurrentMap<String, PerformanceTimingAggregate> aggregatesByAction;

	protected final IPerformanceRegistry registry;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public PerformanceAggregator(IPerformanceRegistry registry)
	{
		this.registry = registry;
		int maxEntriesToKeep = registry.getMaxNumberOfEntriesPerContext();
		if (maxEntriesToKeep == IPerformanceRegistry.OFF)
		{
			aggregatesByAction = null;
		}
		else
		{
			aggregatesByAction = new ConcurrentHashMap<String, PerformanceTimingAggregate>(
				maxEntriesToKeep > 0 ? maxEntriesToKeep : DEFAULT_MAX_ENTRIES_TO_KEEP_IN_PRODUCTION);
		}
	}

	/**
	 * Creates a clone/copy.
	 */
	public PerformanceAggregator(PerformanceAggregator copy)
	{
		this.registry = copy.registry;
		int maxEntriesToKeep = this.registry.getMaxNumberOfEntriesPerContext();
		if (copy.aggregatesByAction != null)
		{
			aggregatesByAction = new ConcurrentHashMap<String, PerformanceTimingAggregate>(maxEntriesToKeep);
			for (PerformanceTimingAggregate a : copy.aggregatesByAction.values())
			{
				PerformanceTimingAggregate copyOfA = new PerformanceTimingAggregate(a);
				aggregatesByAction.put(a.getAction(), copyOfA);
			}
		}
		else if (maxEntriesToKeep == IPerformanceRegistry.OFF)
		{
			aggregatesByAction = null;
		}
		else
		{
			aggregatesByAction = new ConcurrentHashMap<String, PerformanceTimingAggregate>(
				maxEntriesToKeep > 0 ? maxEntriesToKeep : DEFAULT_MAX_ENTRIES_TO_KEEP_IN_PRODUCTION);
		}
	}

	/**
	 * Please use {@link #startAction(String, long, int)} / {@link #endAction(UUID)} / {@link #intervalAction(UUID)} whenever possible instead.
	 */
	public void addTiming(String action, long interval_ms, long total_ms, int type,
		Queue<PerformanceTiming> subActionTimings, int nrecords)
	{
		if (this.registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF ||
			this.registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.LOGGING_ONLY) return;

		PerformanceTimingAggregate time;
		lock.readLock().lock();
		try
		{
			time = aggregatesByAction.get(action);
			if (time == null)
			{
				time = new PerformanceTimingAggregate(action, type, registry);
				PerformanceTimingAggregate alreadyThere = aggregatesByAction.putIfAbsent(action, time);
				if (alreadyThere != null) time = alreadyThere;
			}
			// update obj
			time.updateTime(interval_ms, total_ms, nrecords);
			time.updateSubActionTimes(subActionTimings, nrecords); // update the sub-action times on sub-aggregate(s) as well
		}
		finally
		{
			lock.readLock().unlock();
		}
		// do clean
		int maxEntriesToKeep = this.registry.getMaxNumberOfEntriesPerContext();
		if (maxEntriesToKeep != IPerformanceRegistry.UNLIMITED_ENTRIES && aggregatesByAction.size() > (maxEntriesToKeep * 1.1)) // 10% more is allowed
		{
			SortedList<PerformanceTimingAggregate> sorted = getSortedList();
			int size = sorted.size();
			int maxToRemoveForThisClient = size - maxEntriesToKeep;
			int counter = 0;
			while (aggregatesByAction.size() > maxEntriesToKeep && counter++ < maxToRemoveForThisClient)
			{
				PerformanceTimingAggregate last = sorted.get(size - counter);
				aggregatesByAction.remove(last.getAction());
			}
		}
		return;
	}

	public void clear()
	{
		lock.writeLock().lock();
		try
		{
			aggregatesByAction.clear();
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}

	public PerformanceTimingAggregate[] toArray()
	{
		SortedList<PerformanceTimingAggregate> sortedAggregates = getSortedList();
		return sortedAggregates.toArray(new PerformanceTimingAggregate[sortedAggregates.size()]);
	}

	/**
	 * @return
	 */
	private SortedList<PerformanceTimingAggregate> getSortedList()
	{
		SortedList<PerformanceTimingAggregate> sortedAggregates = new SortedList<>(TimeComparator.INSTANCE, aggregatesByAction.values().size());
		lock.writeLock().lock();
		try
		{
			sortedAggregates.addAll(aggregatesByAction.values());
		}
		finally
		{
			lock.writeLock().unlock();
		}
		return sortedAggregates;
	}
}
