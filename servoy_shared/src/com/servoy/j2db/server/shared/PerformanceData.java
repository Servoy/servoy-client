package com.servoy.j2db.server.shared;


import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;

import com.servoy.j2db.util.Pair;

/**
 * Keeps a list off last 200 most expensive actions (e.g. sql, method calls) to be viewed in a UI.
 *
 * @author jblok
 */
public class PerformanceData
{
	private final static ThreadLocal<PerformanceTiming> top = new ThreadLocal<>();

	/**
	 * Keys are PerformanceTiming unique id's; each PerformanceTiming generates a new id in it's constructor.<br/><br/>
	 *
	 * NOTE: The root PerformanceData can have multiple started timings in this map: 1 for each client. All sub-timing (PerformanceTiming extends PerformanceData)
	 * will only have max one item in this map. As they are an item of a singular stack for that client.
	 */
	private final ConcurrentMap<Integer, PerformanceTiming> startedTimings = new ConcurrentHashMap<>();

	private final ConcurrentLinkedQueue<PerformanceTiming> subTimings = new ConcurrentLinkedQueue<>();

	/**
	 * Started timings are kept to form a stack per client (through PerformanceData/PerformanceTiming startedTimingPerClient hierarchy) as well because for
	 * example an showForm modal dialog could execute other actions and then when modal is closed sub-actions might still happen and they need to point to
	 * the correct parent action.<br/><br/>
	 *
	 * Keys in this map are client UUIDs as strings (so not PerformanceTiming IDs); so there is one child performanceTiming per servoy client executing things (which then can have other timings nested in it).
	 *
	 * NOTE: The root PerformanceData can have multiple started timings in this map: 1 for each client. All sub-timing (PerformanceTiming extends PerformanceData)
	 * will only have max one item in this map. As they are just a call inside a singular call stack for that client.
	 */
	private final ConcurrentMap<String, PerformanceTiming> startedTimingPerClient = new ConcurrentHashMap<>();

	private final Logger log;

	private final IPerformanceRegistry registry;

	private final PerformanceAggregator aggregator;

	private final String id;

	public PerformanceData(IPerformanceRegistry registry, Logger log, String id, PerformanceAggregator aggregator)
	{
		super();
		this.registry = registry;
		this.log = log;
		this.id = id;
		this.aggregator = aggregator;
	}

	public Integer startAction(String action, long start_ms, int type, String clientUUID)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF) return null;
		PerformanceTiming timing = startedTimingPerClient.get(clientUUID);
		if (timing == null)
		{
			timing = new PerformanceTiming(action, type, start_ms, clientUUID, registry, log, id, this.aggregator);
			startedTimingPerClient.put(clientUUID, timing);
			startedTimings.put(timing.getID(), timing);
			PerformanceTiming topTiming = top.get();
			if (topTiming == null) top.set(timing);
			return timing.getID();
		}
		else
		{
			return timing.startAction(action, start_ms, type, clientUUID);
		}
	}

	public void intervalAction(Integer timingId)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF || timingId == null) return;

		PerformanceTiming timing = startedTimings.get(timingId);
		if (timing != null) timing.setIntervalTime();
	}

	public void endAction(Integer timingId, String clientUUID)
	{
		endAction(timingId, 1, clientUUID);
	}

	public void endAction(Integer timingId, int nrecords, String clientUUID)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF || timingId == null) return;
		PerformanceTiming timing = startedTimingPerClient.get(clientUUID);
		if (timing != null)
		{
			// is this the uuid that is on this stack? then this one should be ended.
			// else a child/sub timing should be searched for.
			if (timingId.equals(timing.getID()))
			{
				startedTimingPerClient.remove(clientUUID);
				startedTimings.remove(timingId);
				PerformanceTiming topTiming = top.get();
				if (topTiming == timing) top.remove(); // so the thread local "top" is also the timing that is now ending
				else if ("sql".equals(id)) //$NON-NLS-1$
				{
					// so the thread local "top" is NOT the timing that is now ending; we are ending an SQL statement (this.id is "sql") but the "top" on this
					// thread is not that SQL statement but probably a method call put in "top" thread local by another PerformanceData instance

					// this is bit of a hack to tie the SQL data to the method data based on the id of the Performance Registry
					// find the (possibly nested) method call that is currently running to attach this ending SQL timing to it
					PerformanceTiming methodCallCurrentlyRunning = topTiming;
					PerformanceTiming[] startedActions = methodCallCurrentlyRunning.getStartedActions(); // startedActions can only be max size 1 on PerformanceTiming instances; see javadoc
					while (startedActions.length == 1)
					{
						methodCallCurrentlyRunning = startedActions[0];
						startedActions = methodCallCurrentlyRunning.getStartedActions();
					}
					methodCallCurrentlyRunning.getSubTimings().add(timing);
				}
			}
			else
			{
				timing.endAction(timingId, nrecords, clientUUID);
				timing = null; // this one is not done yet should not be ended below.
			}

		}
		else
		{
			timing = startedTimings.remove(timingId);
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

	public Pair<Integer, Integer> startSubAction(String action, long start_ms, int type, String clientUUID)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF) return null;

		PerformanceTiming lastStartedTiming = startedTimingPerClient.get(clientUUID);
		if (lastStartedTiming == null) return null; // probably a Servoy internal service API call that gets called outside any user method; ignore

		Integer subTimingUUID = lastStartedTiming.startAction(action, start_ms, type, clientUUID); // this call will go to (recursively) last started action on this client's stack
		return new Pair<>(lastStartedTiming.getID(), subTimingUUID);
	}

	public void endSubAction(Pair<Integer, Integer> subActionIDs, String clientUUID)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF) return;
		if (subActionIDs == null) return; // probably a Servoy internal service API call that gets called outside any user method; ignore

		PerformanceTiming timingWithSubAction = startedTimings.get(subActionIDs.getLeft());
		if (timingWithSubAction != null)
		{
			timingWithSubAction.endAction(subActionIDs.getRight(), clientUUID);
		}
	}

	protected static class TimeComparator implements Comparator<PerformanceTimingAggregate>
	{
		public static final TimeComparator INSTANCE = new TimeComparator();

		private TimeComparator()
		{
		}

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

	public Queue<PerformanceTiming> getSubTimings()
	{
		return subTimings;
	}

	public PerformanceAggregator getAggregator()
	{
		return aggregator;
	}
}