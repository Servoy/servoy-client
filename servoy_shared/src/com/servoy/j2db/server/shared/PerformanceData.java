package com.servoy.j2db.server.shared;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * Keeps a list off last 200 most expensive actions (e.g. sql, method calls) to be viewed in a UI.
 *
 * @author jblok
 */
public class PerformanceData extends PerformanceAggregator
{
	private final Map<UUID, PerformanceTiming> startedTimings = new HashMap<UUID, PerformanceTiming>();

	// stack because for example an showForm modal dialog could execute other actions and then when modal
	// is closed sub-actions might still happen and they need to point to the correct parent action
	private final Stack<UUID> startedTimingUUIDsStack = new Stack<>();

	public PerformanceData(int maxEntriesToKeep)
	{
		super(maxEntriesToKeep);
	}

	public synchronized UUID startAction(String action, long start_ms, int type, String clientUUID)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return null;

		PerformanceTiming timing = new PerformanceTiming(action, type, start_ms, clientUUID, maxEntriesToKeep);
		startedTimingUUIDsStack.push(timing.getUuid());
		startedTimings.put(timing.getUuid(), timing);
		return timing.getUuid();
	}

	public synchronized UUID startAction(String action, long start_ms, int type)
	{
		return startAction(action, start_ms, type, null);
	}

	public synchronized void intervalAction(UUID uuid)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF || uuid == null) return;

		PerformanceTiming timing = startedTimings.get(uuid);
		if (timing != null) timing.setIntervalTime();
	}

	public synchronized void endAction(UUID uuid)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF || uuid == null) return;

		PerformanceTiming timing = startedTimings.remove(uuid);
		if (timing != null) addTiming(timing.getAction(), timing.getIntervalTimeMS(), timing.getRunningTimeMS(), timing.getType(), timing.toMap());
		startedTimingUUIDsStack.pop();
	}

	// currently we can have/need only one layer of nesting/sub-actions (sub-actions cannot be accessed right now by the outside world to continue nesting furter)
	public synchronized Pair<UUID, UUID> startSubAction(String action, long start_ms, int type, String clientUUID)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return null;

		if (startedTimingUUIDsStack.isEmpty()) return null; // probably a Servoy internal service API call that gets called outside any user method; ignore
		UUID lastStartedTimingUUID = startedTimingUUIDsStack.peek();

		PerformanceTiming lastStartedTiming = startedTimings.get(lastStartedTimingUUID);
		UUID subTimingUUID = null;
		if (lastStartedTiming != null)
		{
			subTimingUUID = lastStartedTiming.startAction(action, start_ms, type, clientUUID);
		}
		return new Pair<>(lastStartedTimingUUID, subTimingUUID);
	}

	public synchronized void endSubAction(Pair<UUID, UUID> subActionUUIDs)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return;
		if (subActionUUIDs == null) return; // probably a Servoy internal service API call that gets called outside any user method; ignore

		PerformanceTiming timingWithSubAction = startedTimings.get(subActionUUIDs.getLeft());
		if (timingWithSubAction != null)
		{
			timingWithSubAction.endAction(subActionUUIDs.getRight());
		}
	}

	protected static class TimeComparator implements Comparator<PerformanceTimingAggregate>
	{
		public int compare(PerformanceTimingAggregate o1, PerformanceTimingAggregate o2)
		{
			long t1 = o1.getTotalIntervalTimeMS();
			long t2 = o2.getTotalIntervalTimeMS();
			if (t1 == t2)
			{
				return o1.getAction().compareTo(o2.getAction());
			}
			return (int)(t2 - t1);
		}
	}

	public synchronized PerformanceTiming[] getStartedActions()
	{
		return startedTimings.values().toArray(new PerformanceTiming[startedTimings.size()]);
	}
}