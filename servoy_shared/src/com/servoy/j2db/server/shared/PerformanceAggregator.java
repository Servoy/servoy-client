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

	protected final int maxEntriesToKeep;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public PerformanceAggregator(int maxEntriesToKeep)
	{
		this.maxEntriesToKeep = 5; //maxEntriesToKeep;
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
		this.maxEntriesToKeep = copy.maxEntriesToKeep;
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
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return;

		PerformanceTimingAggregate time;
		lock.readLock().lock();
		try
		{
			time = aggregatesByAction.get(action);
			if (time == null)
			{
				time = new PerformanceTimingAggregate(action, type, getSubActionMaxEntries());
				PerformanceTimingAggregate alreadyThere = aggregatesByAction.putIfAbsent(action, time);
				if (alreadyThere != null) time = alreadyThere;
			}
			// update obj
			time.updateTime(interval_ms, total_ms, nrecords);
			time.updateSubActionTimes(subActionTimings, nrecords);

		}
		finally
		{
			lock.readLock().unlock();
		}
		// do clean
		if (maxEntriesToKeep != IPerformanceRegistry.UNLIMITED_ENTRIES && aggregatesByAction.size() > maxEntriesToKeep)
		{
			lock.writeLock().lock();
			try
			{
				while (aggregatesByAction.size() > maxEntriesToKeep)
				{
					PerformanceTimingAggregate last = aggregatesByAction.values().stream().max(TimeComparator.INSTANCE).get();
					aggregatesByAction.remove(last.getAction());
				}
			}
			finally
			{
				lock.writeLock().unlock();
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
		SortedList<PerformanceTimingAggregate> sortedAggregates = new SortedList<>(TimeComparator.INSTANCE);
		lock.writeLock().lock();
		try
		{
			sortedAggregates.addAll(aggregatesByAction.values());
		}
		finally
		{
			lock.writeLock().unlock();
		}
		return sortedAggregates.toArray(new PerformanceTimingAggregate[sortedAggregates.size()]);
	}

	protected int getSubActionMaxEntries()
	{
		return maxEntriesToKeep == IPerformanceRegistry.UNLIMITED_ENTRIES ? IPerformanceRegistry.UNLIMITED_ENTRIES : Math.max(5, maxEntriesToKeep / 10);
	}

}
