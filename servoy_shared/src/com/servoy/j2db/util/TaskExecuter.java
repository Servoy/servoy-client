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



class TaskExecuter implements Runnable
{
	private boolean _bStopThread = false;

	private ITaskThreadPoolCallback _callback;

	private Runnable threadConfigurer;

	TaskExecuter(Runnable threadConfigurer, ITaskThreadPoolCallback callback) throws IllegalArgumentException
	{
		super();
		if (callback == null) { throw new IllegalArgumentException("Null IGUIExecutionControllerCallback passed"); //$NON-NLS-1$
		}
		_callback = callback;
		this.threadConfigurer = threadConfigurer;
	}

	TaskExecuter(ITaskThreadPoolCallback callback) throws IllegalArgumentException
	{
		super();
		if (callback == null) { throw new IllegalArgumentException("Null IGUIExecutionControllerCallback passed"); //$NON-NLS-1$
		}
		_callback = callback;
	}

	public void run()
	{
		if (threadConfigurer != null)
		{
			threadConfigurer.run();
		}
		while (!_bStopThread)
		{
			Runnable task = null;
			synchronized (_callback)
			{
				_callback.incrementFreeThreadCount();
				while (!_bStopThread)
				{
					task = _callback.nextTask();
					if (task != null)
					{
						_callback.decrementFreeThreadCount();
						break;
					}
					else
					{
						try
						{
							_callback.wait();
						}
						catch (InterruptedException ignore)
						{
						}
					}
				}
			}
			if (task != null)
			{
				try
				{
					task.run();
				}
				catch (Throwable th)
				{
					_callback.showMessage(th);
				}
			}
		}
	}

	/**
	 * 
	 */
	public void stop()
	{
		_bStopThread = true;
	}
}
