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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ScheduledExecutorService} that has 2 thread pools, 1 for the normal execute and 1 for scheduled execution.
 * This is because the {@link ScheduledThreadPoolExecutor} is a fixed size thread pool. And will not grow.
 * This class does have options to let the normal calls to {@link ExecutorService#execute(Runnable)} grow the thread pool if all 
 * the threads are busy to a maximum of the executorMaximumPoolSize. When using the {@link ScheduledExecutorService#schedule(Callable, long, TimeUnit)} 
 * or other schedule calls it will map on the fixed size thread pool specified by the scheduledExecutorSize parameter.
 * 
 * @author jcompagner
 */
public class ServoyScheduledExecutor extends ThreadPoolExecutor implements ScheduledExecutorService
{
	private final Runnable NOTHING = new Runnable()
	{
		public void run()
		{
			try
			{
				// sleep for a while so that if there are more then one of these 
				// runnables inserted it doesn't get handled by 1 worker.
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
		}
	};
	private volatile ScheduledThreadPoolExecutor scheduledService;
	private final int scheduledExecutorSize;
	private final int executorSize;

	/**
	 * @param executorSize The core size normal executor
	 * @param executorMaximumPoolSize The maximum pool size that the normal executor can grow to.
	 * @param scheduledExecutorSize The (fixed) core size of the scheduled executor
	 */
	public ServoyScheduledExecutor(int executorSize, int executorMaximumPoolSize, int scheduledExecutorSize)
	{
		super(executorSize, executorMaximumPoolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		this.executorSize = executorSize;
		this.scheduledExecutorSize = scheduledExecutorSize;
	}

	/**
	 * @param scheduledExecutorSize
	 */
	private synchronized ScheduledExecutorService getScheduledExecutorService()
	{
		if (scheduledService == null)
		{
			scheduledService = new ScheduledThreadPoolExecutor(scheduledExecutorSize, getThreadFactory(), getRejectedExecutionHandler())
			{
				@Override
				protected void beforeExecute(Thread t, Runnable r)
				{
					ServoyScheduledExecutor.this.beforeExecute(t, r);
				}

				@Override
				protected void afterExecute(Runnable r, Throwable t)
				{
					ServoyScheduledExecutor.this.afterExecute(r, t);
				}
			};
		}
		return scheduledService;
	}

	// a thread local that will be set on threads of this scheduler, to check recusion in execute
	private static final ThreadLocal<Boolean> schedulerThreadsCheck = new ThreadLocal<Boolean>();

	@Override
	protected void beforeExecute(Thread t, Runnable r)
	{
		schedulerThreadsCheck.set(Boolean.TRUE);
		super.beforeExecute(t, r);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t)
	{
		schedulerThreadsCheck.remove();
		super.afterExecute(r, t);
	}

	/**
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture< ? > schedule(Runnable command, long delay, TimeUnit unit)
	{
		return getScheduledExecutorService().schedule(command, delay, unit);
	}

	/**
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
	 */
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
	{
		return getScheduledExecutorService().schedule(callable, delay, unit);
	}

	/**
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture< ? > scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
	{
		return getScheduledExecutorService().scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	/**
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture< ? > scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
	{
		return getScheduledExecutorService().scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
	 */
	@Override
	public void execute(Runnable command)
	{
		// hack around the fact that the java 5 Executor will not grow and shrink like we want it to.
		// java 6 has support for it to also be able to time out core pool threads.
		// make it also always one bigger if the caller is a scheduler thread it self and the active count => current core size.
		synchronized (schedulerThreadsCheck)
		{
			int activeCount = super.getActiveCount();
			int coreSize = super.getCorePoolSize();
			if (activeCount >= coreSize && (super.getMaximumPoolSize() > coreSize || schedulerThreadsCheck.get() != null))
			{
				setCorePoolSize(coreSize + 1);
			}
			else if (coreSize > executorSize && (activeCount + 1) < coreSize)
			{
				int newCoreSize = Math.max(executorSize, activeCount + 1);
				setCorePoolSize(newCoreSize);
				while (newCoreSize++ < coreSize)
				{
					super.execute(NOTHING);
				}
			}
		}
		super.execute(command);
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	@Override
	public boolean isTerminated()
	{
		return (scheduledService == null || scheduledService.isTerminated()) && super.isTerminated();
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	@Override
	public void shutdown()
	{
		try
		{
			if (scheduledService != null) scheduledService.shutdown();
		}
		finally
		{
			super.shutdown();
		}
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	@Override
	public List<Runnable> shutdownNow()
	{
		ArrayList<Runnable> lst = new ArrayList<Runnable>();
		try
		{
			if (scheduledService != null) lst.addAll(scheduledService.shutdownNow());
		}
		finally
		{
			lst.addAll(super.shutdownNow());
		}
		return lst;
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#remove(java.lang.Runnable)
	 */
	@Override
	public boolean remove(Runnable task)
	{
		if (!super.remove(task))
		{
			if (scheduledService != null) return scheduledService.remove(task);
		}
		return false;
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#awaitTermination(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
	{
		long time = System.currentTimeMillis();
		boolean b = true;
		if (scheduledService != null) b = scheduledService.awaitTermination(timeout, unit);
		if (b)
		{
			long elapsed = System.currentTimeMillis() - time;
			elapsed = unit.convert(elapsed, TimeUnit.MILLISECONDS);
			b = super.awaitTermination((timeout - elapsed), unit);
		}
		return b;
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#setThreadFactory(java.util.concurrent.ThreadFactory)
	 */
	@Override
	public void setThreadFactory(ThreadFactory threadFactory)
	{
		super.setThreadFactory(threadFactory);

		if (scheduledService != null) scheduledService.setThreadFactory(threadFactory);
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#setRejectedExecutionHandler(java.util.concurrent.RejectedExecutionHandler)
	 */
	@Override
	public void setRejectedExecutionHandler(RejectedExecutionHandler handler)
	{
		super.setRejectedExecutionHandler(handler);

		if (scheduledService != null) scheduledService.setRejectedExecutionHandler(handler);
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	@Override
	public int getActiveCount()
	{
		return (scheduledService == null ? 0 : scheduledService.getActiveCount()) + super.getActiveCount();
	}

	/**
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	@Override
	public long getCompletedTaskCount()
	{
		return (scheduledService == null ? 0 : scheduledService.getCompletedTaskCount()) + super.getCompletedTaskCount();
	}

}
