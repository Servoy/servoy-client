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


/**
 * Timing of actions like queries in the server.
 *
 * @author jblok
 */
public class PerformanceTimingAggregate
{
	private final String action;
	private long min_ms;
	private long max_ms;
	private long s2; // used for running calculation of standard deviation
	private int count;
	private final int type;
	private long xtotal_ms;
	private long total_interval_ms;

	public PerformanceTimingAggregate(String action, int type)
	{
		this.action = action;
		this.type = type;
	}

	public PerformanceTimingAggregate(PerformanceTimingAggregate copy)
	{
		this.action = copy.getAction();
		this.type = copy.getType();
		this.min_ms = copy.getMinTimeMS();
		this.max_ms = copy.getMaxTimeMS();
		this.s2 = copy.getS2();
		this.count = copy.getCount();
		this.xtotal_ms = copy.getTotalTimeMS();
		this.total_interval_ms = copy.getTotalIntervalTimeMS();
	}

	public void updateTime(long interval_ms, long running_ms)
	{
		total_interval_ms += interval_ms;
		xtotal_ms += running_ms;
		min_ms = count == 0 ? running_ms : Math.min(min_ms, running_ms);
		max_ms = count == 0 ? running_ms : Math.max(max_ms, running_ms);
		s2 += (running_ms * running_ms);
		count++;
	}

	public void updateTime(long total_interval_ms, long running_ms, long min_ms, long max_ms, long s2, int count)
	{
		this.total_interval_ms += total_interval_ms;
		this.xtotal_ms += running_ms;
		this.min_ms = Math.min(this.min_ms, min_ms);
		this.max_ms = Math.max(this.max_ms, max_ms);
		this.s2 += s2;
		this.count += count;
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
		return PerformanceTiming.getTypeString(type);
	}

	public long getAverageIntervalTimeMS()
	{
		if (count == 0) return total_interval_ms;
		return (total_interval_ms / count);
	}

	public long getAverageTimeMS()
	{
		if (count == 0) return xtotal_ms;
		return (xtotal_ms / count);
	}

	public long getTotalIntervalTimeMS()
	{
		return total_interval_ms;
	}

	public long getTotalTimeMS()
	{
		return xtotal_ms;
	}

	public long getMinTimeMS()
	{
		return min_ms;
	}

	public long getMaxTimeMS()
	{
		return max_ms;
	}

	public double getStandardDeviation()
	{
		if (count <= 1) return 0;

		// see http://en.wikipedia.org/wiki/Standard_deviation for calculating standard deviation
		// see http://easycalculation.com/statistics/standard-deviation.php for calculating stdev

		// Population Standard deviation
//		return Math.sqrt((count * s2) - (total_ms * total_ms)) / count;

		// Standard deviation
		return Math.sqrt((((double)((count * s2) - (xtotal_ms * xtotal_ms)))) / (count * (count - 1)));
	}

	public int getCount()
	{
		return count;
	}

	public long getS2()
	{
		return s2;
	}
}
