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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple {@link ThreadPoolExecutor} executor that works around the fact that java 5 can't grow or shrink core threads.
 * Only java 6 will be able to do that.
 * 
 * This is a default {@link ThreadPoolExecutor} that has its own {@link ThreadFactory} and has its own {@link BlockingQueue} and {@link RejectedExecutionHandler} instances
 * for handling the growing from corePoolSize (default running threads) to the maximumPoolSize.
 * 
 * If you also need scheduling see {@link ServoyScheduledExecutor}
 * 
 * @author jcompagner
 * @since 6.0
 * 
 */
public class ServoyThreadPoolExecutor extends ThreadPoolExecutor
{

	/**
	 * constructs a thread pool with 1 core thread up to maximumPoolSize that time out after 10 seconds if idle
	 * 
	 * @param maximumPoolSize The maximum pool size.
	 * @param name The thread prefix name part.
	 * @param daemonThreads Boolean indicating that the threads should be daemon or not.
	 */
	public ServoyThreadPoolExecutor(int maximumPoolSize, String name, boolean daemonThreads)
	{
		this(1, maximumPoolSize, 10, TimeUnit.SECONDS, new RejectingLinkedBlockingQueue(), new DefaultThreadFactory(name, daemonThreads),
			new ReentryRejectedExecutionHandler());
	}


	public ServoyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, String threadName, boolean daemonThreads)
	{
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, new RejectingLinkedBlockingQueue(), new DefaultThreadFactory(threadName, daemonThreads),
			new ReentryRejectedExecutionHandler());
	}

	private ServoyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
		ThreadFactory threadFactory, RejectedExecutionHandler handler)
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
		((RejectingLinkedBlockingQueue)workQueue).setExecutor(this);
	}


	private static final class ReentryRejectedExecutionHandler implements RejectedExecutionHandler
	{
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
		{
			if (!executor.isShutdown() && !executor.isTerminated() && !executor.isTerminating())
			{
				executor.execute(r);
			}
		}
	}

	private static class RejectingLinkedBlockingQueue extends LinkedBlockingQueue<Runnable>
	{
		private ThreadPoolExecutor executor;

		public void setExecutor(ThreadPoolExecutor executor)
		{
			this.executor = executor;
		}

		@Override
		public boolean offer(Runnable e)
		{
			if (executor != null && executor.getPoolSize() < executor.getMaximumPoolSize()) return false;
			return super.offer(e);
		}
	}

	private static class DefaultThreadFactory implements ThreadFactory
	{
		static final AtomicInteger poolNumber = new AtomicInteger(1);
		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final String namePrefix;
		final boolean daemonThreads;

		@SuppressWarnings("nls")
		DefaultThreadFactory(String name, boolean daemonThreads)
		{
			this.daemonThreads = daemonThreads;
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = (name == null ? "servoy-pool-" : name) + poolNumber.getAndIncrement() + "-thread-";
		}

		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			t.setDaemon(daemonThreads);
			if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}
}
