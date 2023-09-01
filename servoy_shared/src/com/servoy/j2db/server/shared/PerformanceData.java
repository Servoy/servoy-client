package com.servoy.j2db.server.shared;


import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;

import com.servoy.j2db.util.Pair;

/**
 * Root entries in {@link IPerformanceRegistry}. So the performance registry will have 1 instance of this class per "context". (context in perf. reg. is either the solution name
 * - in case of performance registry for method calls, or the DB server name - in case of performance registry for sql).<br/><br/>
 *
 * This class is extended by {@link PerformanceTiming} as well, so all sub-actions (PerformanceTiming) in each client's stack do extend from this class as well.
 *
 * @author jblok
 */
public class PerformanceData
{

	/**
	 * Static, so 1 instance per thread/client for the whole PerformanceData class (no matter how many instances of it there are).<br/>
	 * That means that this instance is shared between all PerformanceData instances (no matter of the context they are registered for -
	 * the solution name or DB server name) in all IPerformanceRegistry instances (on the same thread/client of course).<br/><br/>
	 *
	 * So there is only 1 instance of this for a client - shared between the PerformanceData instance for solutions (which is 1 for method calls in the active
	 * solution of the client, at least until client changes solution) and all DB server sql PerformanceData instances (for that client).
	 */
	private final static ThreadLocal<PerformanceTiming> sharedTopTimingOfClientTL = new ThreadLocal<>();

	/**
	 * Keys are PerformanceTiming unique id's; each PerformanceTiming generates a new id in it's constructor.<br/><br/>
	 *
	 * NOTE: The root PerformanceData can have multiple started timings in this map: 1 for each client. All sub-timing (PerformanceTiming extends PerformanceData)
	 * will only have max one item in this map. As they are an item of a singular stack for that client.
	 */
	private final ConcurrentMap<Long, PerformanceTiming> startedTimings = new ConcurrentHashMap<>();

	/**
	 * Non-static, so 1 instance per thread and per instance of PerformanceData/PerformanceTiming (so multiple instances in the end for each thread/client).
	 */
	private final ThreadLocal<PerformanceTiming> startedTimingPerClientForThisInstanceTL = new ThreadLocal<>();

	private final Logger log;

	private final IPerformanceRegistry registry;

	private final PerformanceAggregator aggregator;

	private final String contextId;

	public PerformanceData(IPerformanceRegistry registry, Logger log, String contextId, PerformanceAggregator aggregator)
	{
		super();
		this.registry = registry;
		this.log = log;
		this.contextId = contextId;
		this.aggregator = aggregator;
	}

	public Long startAction(String action, long start_ms, int type, String clientUUID, String customObject)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF) return null;
		PerformanceTiming startedTimingPerClientForThisInstance = startedTimingPerClientForThisInstanceTL.get();
		if (startedTimingPerClientForThisInstance == null)
		{
			Long parentId = getID();
			PerformanceTiming sharedTopTimingOfClient = sharedTopTimingOfClientTL.get();
			if (parentId == null && sharedTopTimingOfClient != null && "sql".equals(contextId)) //$NON-NLS-1$
			{
				PerformanceTiming methodCallCurrentlyRunning = sharedTopTimingOfClient;
				PerformanceTiming[] startedActions = methodCallCurrentlyRunning.getStartedActions(); // startedActions can only be max size 1 on PerformanceTiming instances; see javadoc
				while (startedActions.length == 1)
				{
					methodCallCurrentlyRunning = startedActions[0];
					startedActions = methodCallCurrentlyRunning.getStartedActions();
				}
				parentId = methodCallCurrentlyRunning.getID();
			}
			startedTimingPerClientForThisInstance = new PerformanceTiming(action, type, parentId, customObject, start_ms, clientUUID, registry, log, contextId,
				this.aggregator);
			startedTimingPerClientForThisInstanceTL.set(startedTimingPerClientForThisInstance);
			startedTimings.put(startedTimingPerClientForThisInstance.getID(), startedTimingPerClientForThisInstance);
			if (sharedTopTimingOfClient == null) sharedTopTimingOfClientTL.set(startedTimingPerClientForThisInstance);
			return startedTimingPerClientForThisInstance.getID();
		}
		else
		{
			return startedTimingPerClientForThisInstance.startAction(action, start_ms, type, clientUUID, customObject);
		}
	}

	/**
	 * return the ID of this performace data, the default one is just null
	 */
	protected Long getID()
	{
		return null;
	}

	public void intervalAction(Long timingId)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF || timingId == null) return;

		PerformanceTiming timing = startedTimings.get(timingId);
		if (timing != null) timing.setIntervalTime();
	}

	public void endAction(Long timingId, String clientUUID)
	{
		endAction(timingId, 1, clientUUID);
	}

	public void endAction(Long timingId, int nrecords, String clientUUID)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF || timingId == null) return;
		PerformanceTiming startedTimingPerClientForThisInstance = startedTimingPerClientForThisInstanceTL.get();
		PerformanceTiming timingThatEnded = startedTimingPerClientForThisInstance;
		if (startedTimingPerClientForThisInstance != null)
		{
			// is this the uuid that is on this stack? then this one should be ended.
			// else a child/sub timing should be searched for.
			if (timingId.equals(startedTimingPerClientForThisInstance.getID()))
			{
				startedTimingPerClientForThisInstanceTL.remove();
				startedTimings.remove(timingId);
				PerformanceTiming sharedTopTimingOfClient = sharedTopTimingOfClientTL.get();
				if (sharedTopTimingOfClient == startedTimingPerClientForThisInstance) sharedTopTimingOfClientTL.remove(); // so the static thread local is also the timing that is now ending
				else if ("sql".equals(contextId)) //$NON-NLS-1$
				{
					// so the thread local "sharedTopTimingOfClientTL" is NOT the timing that is now ending;
					// we are ending an SQL statement (this.id is "sql") but the "sharedTopTimingOfClientTL" on this
					// thread is not that SQL statement but probably a method call put in "sharedTopTimingOfClientTL" thread local by another PerformanceData instance

					// this is bit of a hack to tie the SQL data of current timing that now ends to the correct method data based on the id of the Performance Registry;
					// find the most deeply nested method call - that is currently running to attach this ending SQL timing to it
					PerformanceTiming methodCallCurrentlyRunning = sharedTopTimingOfClient;
					PerformanceTiming[] startedActions = methodCallCurrentlyRunning.getStartedActions(); // startedActions can only be max size 1 on PerformanceTiming instances; see javadoc
					while (startedActions.length == 1)
					{
						methodCallCurrentlyRunning = startedActions[0];
						startedActions = methodCallCurrentlyRunning.getStartedActions();
					}
					methodCallCurrentlyRunning.getSubTimings().add(startedTimingPerClientForThisInstance);
				}
			}
			else
			{
				// ok so child startedTimingPerClientForThisInstance is not the action that is being ended; forward end action to child in the action 'stack'
				startedTimingPerClientForThisInstance.endAction(timingId, nrecords, clientUUID);
				timingThatEnded = null; // this one is not done yet should not be ended below.
			}
		}
		else
		{
			timingThatEnded = startedTimings.remove(timingId);
		}
		if (timingThatEnded != null)
		{
			if (log != null && log.isInfoEnabled())
			{
				log.info(
					timingThatEnded.getID() + "|" + timingThatEnded.getParentID() + '|' + timingThatEnded.getClientUUID() + '|' + timingThatEnded.getAction() +
						'|' +
						timingThatEnded.getRunningTimeMS() + '|' + timingThatEnded.getIntervalTimeMS() + '|' + timingThatEnded.getTypeString() + '|' +
						timingThatEnded.getCustomObject());
			}
			timingThatEnded.setEndTime();
			addTiming(timingThatEnded, timingThatEnded.getIntervalTimeMS(),
				timingThatEnded.getRunningTimeMS(), nrecords);
			// make sure when this is ended the sub timings are all cleared so they are not all kept in memory (until the last one is done, this can take a while with long running tasks)
			timingThatEnded.getSubTimings().clear();
		}
		return;
	}

	public void addTiming(PerformanceTiming timing, long interval_ms, long total_ms, int nrecords)
	{
		this.aggregator.addTiming(timing.getAction(), interval_ms, total_ms, timing.getType(), timing.getSubTimings(), nrecords);
	}

	public Pair<Long, Long> startSubAction(String action, long start_ms, int type, String clientUUID, String customObject)
	{
		if (registry.getMaxNumberOfEntriesPerContext() == IPerformanceRegistry.OFF) return null;

		PerformanceTiming lastStartedTiming = startedTimingPerClientForThisInstanceTL.get();
		if (lastStartedTiming == null) return null; // probably a Servoy internal service API call that gets called outside any user method; ignore

		Long subTimingUUID = lastStartedTiming.startAction(action, start_ms, type, clientUUID, customObject); // this call will go to (recursively) last started action on this client's stack
		return new Pair<>(lastStartedTiming.getID(), subTimingUUID);
	}

	public void endSubAction(Pair<Long, Long> subActionIDs, String clientUUID)
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

	public PerformanceAggregator getAggregator()
	{
		return aggregator;
	}
}