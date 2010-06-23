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

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.concurrent.Executor;

/**
 * Helper class to wrap some swing related methods
 * @author jblok
 */
public class SwingHelper
{
	private final Executor threadpool;
	private static SwingHelper memyselfandi;
	private static Object wailtLock = new Object();
	private static final int sleepIntervals = 500;

	private SwingHelper(Executor a_threadpool)
	{
		threadpool = a_threadpool;
	}

	public static SwingHelper getSwingHelper(Executor a_threadpool)
	{
		if (memyselfandi == null)
		{
			memyselfandi = new SwingHelper(a_threadpool);
		}
		return memyselfandi;
	}

	public Object paintingWait(IUIBlocker blocker, String reason, int delayBeforeBlockUI_ms, BackgroundRunner backgroundRunner) throws Exception
	{
		threadpool.execute(backgroundRunner);

		synchronized (wailtLock)
		{
			wailtLock.wait(delayBeforeBlockUI_ms);
		}

		long startTime = System.currentTimeMillis();
		boolean didBlock = false;
		try
		{
			while (!backgroundRunner.isFinished())
			{
				if (!didBlock && (System.currentTimeMillis() - startTime) > delayBeforeBlockUI_ms)
				{
					didBlock = true;
					blocker.blockGUI(reason);
				}
				int time_spend_ms = dispatchEvents(sleepIntervals);
				if (!backgroundRunner.isFinished() && (sleepIntervals - time_spend_ms) > 0)
				{
					Thread.sleep((sleepIntervals - time_spend_ms));
				}
			}
		}
		finally
		{
			if (didBlock) blocker.releaseGUI();
		}

		return backgroundRunner.getReturnValue();
	}

	public static abstract class BackgroundRunner implements Runnable
	{
		private Exception ex;
		private Object retval;
		private boolean isFinished;

		boolean isFinished()
		{
			return isFinished;
		}

		Object getReturnValue() throws Exception
		{
			if (ex != null) throw ex;
			return retval;
		}

		public final void run()
		{
			try
			{
				retval = doWork();
			}
			catch (Exception e)
			{
				ex = e;
			}
			finally
			{
				isFinished = true;
				synchronized (wailtLock)
				{
					wailtLock.notify();
				}
			}
		}

		public abstract Object doWork() throws Exception;
	}

	public static int dispatchEvents(int ms)//returns the actual time spend in ms
	{
//		dispatchEventsUntilNow();
//		if (true) return;

		if (java.awt.EventQueue.isDispatchThread() && !isDispatching)
		{
			try
			{
				isDispatching = true;

//				int handled = 0;
				java.awt.EventQueue eventQ = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
				long start_time = System.currentTimeMillis();
				long time = start_time + ms;
				while (System.currentTimeMillis() < time)
				{
					if (eventQ.peekEvent() == null)
					{
						break; //no more event ,leave
					}
					try
					{
						AWTEvent event = eventQ.getNextEvent();
						if (event instanceof ActiveEvent)
						{
							((ActiveEvent)event).dispatch();
						}
						else
						{
							Object source = event.getSource();
							if (source instanceof Component)
							{
								Component comp = (Component)source;
								comp.dispatchEvent(event);
							}
						}
//						handled++;
					}
					catch (InterruptedException e)
					{
						//hmm
					}
				}
				return (int)(start_time - System.currentTimeMillis());
			}
			finally
			{
				isDispatching = false;
			}
		}
		return 0;
	}

	private static boolean isDispatching = false;

	private static void dispatchEventsUntilNow()
	{
		if (java.awt.EventQueue.isDispatchThread() && !isDispatching)
		{
			isDispatching = true;
			try
			{
				int handled = 0;
				java.awt.EventQueue eventQ = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
				class StopEvent extends AWTEvent
				{
					StopEvent()
					{
						super(new Object(), 0);
					}
				}
				eventQ.postEvent(new StopEvent());
				while (true)
				{
					try
					{
						AWTEvent event = eventQ.getNextEvent();
						if (event instanceof StopEvent)
						{
							break;
						}
						if (event instanceof ActiveEvent)
						{
							((ActiveEvent)event).dispatch();
						}
						else
						{
							Object source = event.getSource();
							if (source instanceof Component)
							{
								Component comp = (Component)source;
								comp.dispatchEvent(event);
							}
						}
						handled++;
					}
					catch (InterruptedException e)
					{
						//hmm
					}
				}
//				Debug.trace("####dispatchEventsUntilNow handeld "+(handled)+" events");
			}
			finally
			{
				isDispatching = false;
			}
		}
	}

	public static void centerFrame(Window frame)
	{
		Dimension frameSize = frame.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2 - 10);
	}
}
