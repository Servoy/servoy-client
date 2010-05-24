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
package com.servoy.j2db.util;

import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class SlidingWindowAppender extends AppenderSkeleton
{
	private int windowSize = 1000;

	private boolean locationInfo = false;

	protected LinkedList eventWindow;

	protected Object eventWindowLock = new Object();

	public SlidingWindowAppender()
	{
		eventWindow = new LinkedList();
	}

	@Override
	public void append(LoggingEvent event)
	{
		if (!checkEntryConditions())
		{
			return;
		}
		event.getThreadName();
		event.getNDC();
		if (locationInfo)
		{
			event.getLocationInformation();
		}
		synchronized (eventWindowLock)
		{
			eventWindow.add(event);
			if (eventWindow.size() > windowSize)
			{
				eventWindow.removeFirst();
			}
		}
	}

	protected boolean checkEntryConditions()
	{
		if (this.layout == null)
		{
			errorHandler.error("No layout set for appender named [" + name + "].");
			return false;
		}
		return true;
	}

	@Override
	synchronized public void close()
	{
		this.closed = true;
	}

	@Override
	public boolean requiresLayout()
	{
		return true;
	}

	public String formatBuffer(Filter filter, boolean ascending)
	{
		synchronized (eventWindowLock)
		{
			StringBuffer buffer = new StringBuffer();
			String header = layout.getHeader();
			if (header != null) buffer.append(header);
			ListIterator iterator = eventWindow.listIterator(ascending ? 0 : eventWindow.size());
			while (ascending ? iterator.hasNext() : iterator.hasPrevious())
			{
				LoggingEvent event = (LoggingEvent)(ascending ? iterator.next() : iterator.previous());
				int accept = Filter.NEUTRAL;
				Filter currentFilter = filter;
				while (currentFilter != null && accept == Filter.NEUTRAL)
				{
					accept = currentFilter.decide(event);
					currentFilter = currentFilter.next;
				}
				if (accept != Filter.DENY)
				{
					buffer.append(layout.format(event));
				}
			}
			String footer = layout.getFooter();
			if (footer != null) buffer.append(footer);
			return buffer.toString();
		}
	}

	public void clearWindow()
	{
		synchronized (eventWindowLock)
		{
			eventWindow = new LinkedList();
		}
	}

	public void setWindowSize(int windowSize)
	{
		synchronized (eventWindowLock)
		{
			while (windowSize < eventWindow.size())
			{
				eventWindow.removeFirst();
			}
			this.windowSize = windowSize;
		}
	}

	public int getWindowSize()
	{
		return windowSize;
	}

	public void setLocationInfo(boolean locationInfo)
	{
		this.locationInfo = locationInfo;
	}

	public boolean getLocationInfo()
	{
		return locationInfo;
	}

}
