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

package com.servoy.j2db.server.shared;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.servoy.j2db.dataprocessing.IDataServer;

/**
 * Timing of sub-actions like queries/methods in the server. It is not used as a root entry in the performance registry (which are just PerformanceData instances),
 * just for sub-actions of those. It can contain sub-actions of it's own - building 1 stack per client, so it extends PerformanceData.
 *
 * @author jblok
 */
public class PerformanceTiming extends PerformanceData
{
	private final static AtomicLong ID_GEN = new AtomicLong();
	private final Long id;
	private final Long parentId;
	private final String action;
	private final String customObject;
	private final int type;
	private final String clientUUID;
	private final AtomicLong start_ms = new AtomicLong(0);
	private final AtomicLong end_ms = new AtomicLong(0);
	private final AtomicLong interval_ms = new AtomicLong(0);

	private final ConcurrentLinkedQueue<PerformanceTiming> subTimings = new ConcurrentLinkedQueue<>();

	public PerformanceTiming(String action, int type, Long parentId, String customObject, long start_ms, String clientUUID, IPerformanceRegistry registry,
		Logger log,
		String contextId, PerformanceAggregator aggregator)
	{
		super(registry, log, contextId, aggregator);

		this.id = Long.valueOf(ID_GEN.getAndIncrement());
		this.parentId = parentId;
		this.action = action;
		this.type = type;
		this.customObject = customObject;
		this.start_ms.set(start_ms);
		this.clientUUID = clientUUID;
	}


	@Override
	public void addTiming(PerformanceTiming timing, long intervalMsSubAction, long totalMsSubAction, int nrecords)
	{
		long totalMsSubAction2 = totalMsSubAction;
		long intervalMsSubAction2 = intervalMsSubAction;
		if (timing.getType() == IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT)
		{
			// the subaction was waiting for user input; so discard it's running time from this action's calculation as it's not useful
			// (we could also keep a separate longs and substract them from these values during display if we need to show total time of an action including waiting for user stuff in the future)
			start_ms.addAndGet(totalMsSubAction);
			totalMsSubAction2 = 0;
			intervalMsSubAction2 = 0;
		}

		super.addTiming(timing, intervalMsSubAction2, totalMsSubAction2, nrecords);
		// if in log only we don't seem to need to subtimings that is later on used in the aggregator only which doesn't do anything in log only
		if (this.registry.getMaxNumberOfEntriesPerContext() != IPerformanceRegistry.LOGGING_ONLY)
			subTimings.add(timing);
	}

	@Override
	public Long getID()
	{
		return id;
	}

	public String getAction()
	{
		return action;
	}

	public int getType()
	{
		return type;
	}

	public String getTypeString()
	{
		return getTypeString(type);
	}

	public static String getTypeString(int type)
	{
		switch (type)
		{
			case IDataServer.CUSTOM_QUERY :
				return "Custom"; //$NON-NLS-1$
			case IDataServer.FIND_BROWSER_QUERY :
				return "Find"; //$NON-NLS-1$
			case IDataServer.RELATION_QUERY :
				return "Relation"; //$NON-NLS-1$
			case IDataServer.REFRESH_ROLLBACK_QUERY :
				return "Refresh/Rollback"; //$NON-NLS-1$
			case IDataServer.UPDATE_QUERY :
				return "Update"; //$NON-NLS-1$
			case IDataServer.INSERT_QUERY :
				return "Insert"; //$NON-NLS-1$
			case IDataServer.DELETE_QUERY :
				return "Delete"; //$NON-NLS-1$
			case IDataServer.RAW_QUERY :
				return "Raw SQL"; //$NON-NLS-1$
			case IDataServer.AGGREGATE_QUERY :
				return "Aggregate SQL"; //$NON-NLS-1$
			case IDataServer.REPOSITORY_QUERY :
				return "Repository SQL"; //$NON-NLS-1$
			case IDataServer.FOUNDSET_LOAD_QUERY :
				return "Load foundset"; //$NON-NLS-1$
			case IDataServer.LOCKS_QUERY :
				return "Acquire locks"; //$NON-NLS-1$
			case IDataServer.MESSAGES_QUERY :
				return "Load messages"; //$NON-NLS-1$
			case IDataServer.VALUELIST_QUERY :
				return "Load valueList"; //$NON-NLS-1$
			case IDataServer.PRINT_QUERY :
				return "Printing"; //$NON-NLS-1$
			case IDataServer.USERMANAGEMENT_QUERY :
				return "User management"; //$NON-NLS-1$
			case IDataServer.META_DATA_QUERY :
				return "Meta data"; //$NON-NLS-1$
			case IDataServer.METHOD_CALL :
				return "Function"; //$NON-NLS-1$
			case IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT :
				return "Function_userinput"; //$NON-NLS-1$
		}
		return "Unknown"; //$NON-NLS-1$
	}

	public long getStartTimeMS()
	{
		return start_ms.get();
	}

	public String getClientUUID()
	{
		return clientUUID;
	}

	public long getRunningTimeMS()
	{
		long end = end_ms.get();
		if (end == 0) end = System.currentTimeMillis();
		return end - start_ms.get();
	}

	public long getIntervalTimeMS()
	{
		return (interval_ms.get() == 0 ? System.currentTimeMillis() : interval_ms.get()) - start_ms.get();
	}

	public void setIntervalTime()
	{
		interval_ms.set(System.currentTimeMillis());
	}

	public void setEndTime()
	{
		end_ms.set(System.currentTimeMillis());
	}

	public Queue<PerformanceTiming> getSubTimings()
	{
		return subTimings;
	}

	public String getCustomObject()
	{
		return customObject;
	}

	public Long getParentID()
	{
		return parentId;
	}
}
