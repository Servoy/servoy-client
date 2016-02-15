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

import java.util.Map;

import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.util.UUID;

/**
 * Timing of actions like queries/methods in the server.
 * It can contain sub-actions so it extends PerformanceData.
 *
 * @author jblok
 */
public class PerformanceTiming extends PerformanceData
{

	private final UUID uuid;
	private final String action;
	private long start_ms;
	private final int type;
	private long interval_ms;
	private final String clientUUID;

	public PerformanceTiming(String action, int type, long start_ms, String clientUUID)
	{
		super(TOTAL_IN_SUBLIST);

		this.uuid = UUID.randomUUID();
		this.action = action;
		this.type = type;
		this.start_ms = start_ms;
		this.clientUUID = clientUUID;
	}


	@Override
	public synchronized void addTiming(String subAction, long intervalMsSubAction, long totalMsSubAction, int typeOfSubAction,
		Map<String, PerformanceTimingAggregate> subSubActionTimings)
	{
		if (typeOfSubAction == IDataServer.METHOD_CALL_WAITING_FOR_USER_INPUT)
		{
			// the subaction was waiting for user input; so discard it's running time from this action's calculation as it's not useful
			// (we could also keep a separate longs and substract them from these values during display if we need to show total time of an action including waiting for user stuff in the future)
			start_ms += totalMsSubAction;
		}

		super.addTiming(subAction, intervalMsSubAction, totalMsSubAction, typeOfSubAction, subSubActionTimings);
	}

	public UUID getUuid()
	{
		return uuid;
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
		}
		return "Unknown"; //$NON-NLS-1$
	}

	public long getStartTimeMS()
	{
		return start_ms;
	}

	public String getClientUUID()
	{
		return clientUUID;
	}

	public long getRunningTimeMS()
	{
		return System.currentTimeMillis() - start_ms;
	}

	public long getIntervalTimeMS()
	{
		return (interval_ms == 0 ? System.currentTimeMillis() : interval_ms) - start_ms;
	}

	public void setIntervalTime()
	{
		interval_ms = System.currentTimeMillis();
	}

}
