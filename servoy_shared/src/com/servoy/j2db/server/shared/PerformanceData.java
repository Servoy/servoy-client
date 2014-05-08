package com.servoy.j2db.server.shared;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;

/**
 * Keeps a list off last 200 most expensive action (e.g. sql) operations, to be viewed in a UI
 * 
 * @author jblok
 */
public class PerformanceData
{
	private static final int TOTAL_IN_LIST = 200;
	private final SortedList<PerformanceTimingAggregate> timeObjects = new SortedList<PerformanceTimingAggregate>(new TimeComparator());
	private final Map<String, PerformanceTimingAggregate> action_timeObjects = new HashMap<String, PerformanceTimingAggregate>(TOTAL_IN_LIST);
	private final Map<UUID, PerformanceTiming> startedTimings = new HashMap<UUID, PerformanceTiming>();

	public synchronized void addTiming(String action, long interval_ms, long total_ms, int type)
	{
		PerformanceTimingAggregate time = action_timeObjects.get(action);
		if (time == null)
		{
			time = new PerformanceTimingAggregate(action, type);
			action_timeObjects.put(action, time);
		}
		//remove
		timeObjects.remove(time);

		//update obj
		time.updateTime(interval_ms, total_ms);

		//do sort again
		timeObjects.add(time);

		//do clean
		if (timeObjects.size() > TOTAL_IN_LIST)
		{
			PerformanceTimingAggregate old = timeObjects.remove(TOTAL_IN_LIST);
			action_timeObjects.remove(old.getAction());
		}
	}

	public synchronized void clear()
	{
		timeObjects.clear();
		action_timeObjects.clear();
	}

	public synchronized UUID startAction(String action, long start_ms, int type)
	{
		UUID uuid = UUID.randomUUID();
		PerformanceTiming timing = new PerformanceTiming(action, type, start_ms);
		startedTimings.put(uuid, timing);
		return uuid;
	}

	public synchronized void intervalAction(UUID uuid)
	{
		PerformanceTiming timing = startedTimings.get(uuid);
		if (timing != null) timing.setIntervalTime();
	}

	public synchronized void endAction(UUID uuid)
	{
		PerformanceTiming timing = startedTimings.remove(uuid);
		if (timing != null) addTiming(timing.getAction(), timing.getIntervalTimeMS(), timing.getRunningTimeMS(), timing.getType());
	}

	public synchronized void endAction(String sql)
	{
		if (sql != null)
		{
			Iterator<Entry<UUID, PerformanceTiming>> pfactions = startedTimings.entrySet().iterator();
			while (pfactions.hasNext())
			{
				Entry<UUID, PerformanceTiming> entry = pfactions.next();
				if (sql.equals(entry.getValue().getAction()))
				{
					endAction(entry.getKey());
					return; // don't continue, iterator changed
				}
			}
		}
	}

	private static class TimeComparator implements Comparator<PerformanceTimingAggregate>
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

	public synchronized PerformanceTimingAggregate[] toArray()
	{
		return timeObjects.toArray(new PerformanceTimingAggregate[timeObjects.size()]);
	}

	public synchronized PerformanceTiming[] getStartedActions()
	{
		return startedTimings.values().toArray(new PerformanceTiming[startedTimings.size()]);
	}
}