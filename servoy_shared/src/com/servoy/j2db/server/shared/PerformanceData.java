package com.servoy.j2db.server.shared;


import java.util.Comparator;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * Keeps a list off last 200 most expensive actions (e.g. sql, method calls) to be viewed in a UI.
 *
 * @author jblok
 */
public class PerformanceData
{
	private final static ThreadLocal<PerformanceTiming> top = new ThreadLocal<>();

	private final ConcurrentMap<UUID, PerformanceTiming> startedTimings = new ConcurrentHashMap<>();

	private final ConcurrentLinkedQueue<PerformanceTiming> subTimings = new ConcurrentLinkedQueue<>();

	// stack because for example an showForm modal dialog could execute other actions and then when modal
	// is closed sub-actions might still happen and they need to point to the correct parent action
	private final ConcurrentMap<String, Stack<PerformanceTiming>> startedTimingUUIDsStack = new ConcurrentHashMap<>();

	private final Logger log;

	private final int maxEntriesToKeep;

	private final PerformanceAggregator aggregator;

	private final String id;

	public PerformanceData(int maxEntriesToKeep, Logger log, String id, PerformanceAggregator aggregator)
	{
		super();
		this.maxEntriesToKeep = maxEntriesToKeep;
		this.log = log;
		this.id = id;
		this.aggregator = aggregator;
	}

	public UUID startAction(String action, long start_ms, int type, String clientUUID)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return null;
		Stack<PerformanceTiming> stack = startedTimingUUIDsStack.get(clientUUID);
		if (stack == null)
		{
			stack = new Stack<>();
			startedTimingUUIDsStack.put(clientUUID, stack);
			PerformanceTiming timing = new PerformanceTiming(action, type, start_ms, clientUUID, maxEntriesToKeep, log, id, this.aggregator);
			startedTimings.put(timing.getUuid(), timing);
			stack.push(timing);
			PerformanceTiming topTiming = top.get();
			if (topTiming == null) top.set(timing);
			return timing.getUuid();
		}
		else
		{
			PerformanceTiming timing = stack.peek();
			return timing.startAction(action, start_ms, type, clientUUID);
		}
	}

	public void intervalAction(UUID uuid)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF || uuid == null) return;

		PerformanceTiming timing = startedTimings.get(uuid);
		if (timing != null) timing.setIntervalTime();
	}

	public void endAction(UUID uuid, String clientUUID)
	{
		endAction(uuid, 1, clientUUID);
	}

	public void endAction(UUID uuid, int nrecords, String clientUUID)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF || uuid == null) return;
		PerformanceTiming timing;
		Stack<PerformanceTiming> stack = startedTimingUUIDsStack.get(clientUUID);
		if (stack != null)
		{
			timing = stack.peek();
			// is this the uuid that is on this stack. then this one should be ended.
			// else a child/sub timing should be searched for.
			if (uuid.equals(timing.getUuid()))
			{
				stack.pop();
				if (stack.size() == 0)
				{
					startedTimingUUIDsStack.remove(clientUUID);
					startedTimings.remove(uuid);
					PerformanceTiming topTiming = top.get();
					if (topTiming == timing) top.remove();
					else if ("sql".equals(id)) // bit of a hack to tie the sql data to the method data based on the id of the Performance Registry
					{
						PerformanceTiming current = topTiming;
						PerformanceTiming[] startedActions = current.getStartedActions();
						while (startedActions.length == 1)
						{
							current = startedActions[0]; // should always be null for the same client (a stack)
							startedActions = current.getStartedActions();
						}
						current.getSubTimings().add(timing);
					}
				}
			}
			else
			{
				timing.endAction(uuid, nrecords, clientUUID);
				timing = null; // this one is not done yet should not be ended below.
			}

		}
		else
		{
			timing = startedTimings.remove(uuid);
		}
		if (timing != null)
		{
			if (log != null && log.isInfoEnabled())
			{
				log.info(timing.getClientUUID() + '|' + timing.getAction() + '|' + timing.getRunningTimeMS() + '|' + timing.getIntervalTimeMS());
			}
			timing.setEndTime();
			addTiming(timing.getAction(), timing.getIntervalTimeMS(), timing.getRunningTimeMS(), timing.getType(), timing.getSubTimings(), nrecords);
			subTimings.add(timing);
		}
		return;
	}

	public void addTiming(String action, long interval_ms, long total_ms, int type,
		Queue<PerformanceTiming> subActionTimings, int nrecords)
	{
		this.aggregator.addTiming(action, interval_ms, total_ms, type, subActionTimings, nrecords);
	}

	// currently we can have/need only one layer of nesting/sub-actions (sub-actions cannot be accessed right now by the outside world to continue nesting furter)
	public Pair<UUID, UUID> startSubAction(String action, long start_ms, int type, String clientUUID)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return null;

		Stack<PerformanceTiming> stack = startedTimingUUIDsStack.get(clientUUID);
		if (stack == null || stack.isEmpty()) return null; // probably a Servoy internal service API call that gets called outside any user method; ignore

		PerformanceTiming lastStartedTiming = stack.peek();

		UUID subTimingUUID = null;
		if (lastStartedTiming != null)
		{
			subTimingUUID = lastStartedTiming.startAction(action, start_ms, type, clientUUID);
		}
		return new Pair<>(lastStartedTiming.getUuid(), subTimingUUID);
	}

	public void endSubAction(Pair<UUID, UUID> subActionUUIDs, String clientUUID)
	{
		if (maxEntriesToKeep == IPerformanceRegistry.OFF) return;
		if (subActionUUIDs == null) return; // probably a Servoy internal service API call that gets called outside any user method; ignore

		PerformanceTiming timingWithSubAction = startedTimings.get(subActionUUIDs.getLeft());
		if (timingWithSubAction != null)
		{
			timingWithSubAction.endAction(subActionUUIDs.getRight(), clientUUID);
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

	public PerformanceTiming[] getStartedActions()
	{
		return startedTimings.values().toArray(new PerformanceTiming[startedTimings.size()]);
	}

	/**
	 * @return the subTimings
	 */
	public Queue<PerformanceTiming> getSubTimings()
	{
		return subTimings;
	}

	/**
	 * @return the aggregator
	 */
	public PerformanceAggregator getAggregator()
	{
		return aggregator;
	}
}