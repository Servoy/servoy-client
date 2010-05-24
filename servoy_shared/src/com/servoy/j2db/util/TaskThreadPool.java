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
import java.util.Iterator;
import java.util.List;

public class TaskThreadPool implements ITaskExecuter
{

	private final int _iMaxThreads;

	// Count of available or free threads.
	private int _iFree;

	private final List _tasks = new ArrayList();
	private final List taskExecuters = new ArrayList();

	private final MyCallback _callback = new MyCallback();

	String name = "TaskExecuter"; //$NON-NLS-1$

	private Runnable threadConfigurer;


	/**
	 * Ctor specifying the max number of threads.
	 * 
	 * @param maxThreads Maximum number of threads.
	 * 
	 * @throws IllegalArgumentException If maxThreads < 0
	 */
	public TaskThreadPool(Runnable threadConfigurer, int maxThreads) throws IllegalArgumentException
	{
		this(maxThreads, "TaskExecuter"); //$NON-NLS-1$
		this.threadConfigurer = threadConfigurer;
	}

	public TaskThreadPool(int maxThreads) throws IllegalArgumentException
	{
		this(maxThreads, "TaskExecuter"); //$NON-NLS-1$
	}

	public TaskThreadPool(int maxThreads, String thread_names) throws IllegalArgumentException
	{
		super();
		name = thread_names;
		if (name == null) name = "TaskExecuter"; //$NON-NLS-1$
		if (maxThreads <= 0)
		{
			throw new IllegalArgumentException("Negative/Zero maxThreads passed"); //$NON-NLS-1$
		}
		_iMaxThreads = maxThreads;
	}

	public void execute(Runnable task)
	{
		addTask(task);
	}

	/**
	 * Add a task to be executed by the next available thread in this thread pool. If there is no free thread available but the maximum number of threads hasn't
	 * been reached then create a new thread.
	 */
	public void addTask(Runnable task) throws IllegalArgumentException
	{
		if (task == null)
		{
			throw new IllegalArgumentException("Null Runnable passed"); //$NON-NLS-1$
		}
		synchronized (_callback)
		{
			_tasks.add(task);
			// Should there me a Max Number of threads?
			if (_iFree == 0)
			{
				if (taskExecuters.size() < _iMaxThreads)
				{
					final TaskExecuter taskExecuter = new TaskExecuter(threadConfigurer, _callback);
					taskExecuters.add(taskExecuter);
					Thread th = new Thread(taskExecuter, name + "[" + (taskExecuters.size() - 1) + "]"); //$NON-NLS-1$ //$NON-NLS-2$
//					th.setPriority(Thread.MIN_PRIORITY);
					th.setDaemon(true);
					th.start();
				}
			}
			else
			{
				_callback.notify();
			}
		}
	}

	public void stop()
	{
		synchronized (_callback)
		{
			Iterator it = taskExecuters.iterator();
			while (it.hasNext())
			{
				TaskExecuter te = (TaskExecuter)it.next();
				te.stop();
			}
			_callback.notifyAll();
		}
	}

	private final class MyCallback implements ITaskThreadPoolCallback
	{
		public synchronized void incrementFreeThreadCount()
		{
			++_iFree;
		}

		public synchronized void decrementFreeThreadCount()
		{
			--_iFree;
		}

		public synchronized Runnable nextTask()
		{
			if (_tasks.size() > 0)
			{
				return (Runnable)_tasks.remove(0);
			}
			return null;
		}

		public void showMessage(Throwable th)
		{
			Debug.error(th);
		}
	}
}
