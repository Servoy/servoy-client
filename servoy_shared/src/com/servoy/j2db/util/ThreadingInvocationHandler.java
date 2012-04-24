/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.servoy.j2db.persistence.RepositoryException;

/**
 * Invocation handler that runs all methods in a thread, so thread locals of the calling thread do not conflict with the thread locals used in the call.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public class ThreadingInvocationHandler implements InvocationHandler
{
	private final Object obj;

	private ThreadingInvocationHandler(Object obj)
	{
		this.obj = obj;
	}

	@SuppressWarnings("unchecked")
	public static <T> T createThreadingInvocationHandler(T obj)
	{
		if (obj == null)
		{
			return null;
		}
		return (T)Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(), new ThreadingInvocationHandler(obj));
	}

	/*
	 * Invoke methods in a separate thread.
	 * 
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
	{
		final Object[] result = { null };
		final Throwable[] throwable = { null };

		Runnable methodRunner = new Runnable()
		{
			public void run()
			{
				try
				{
					result[0] = method.invoke(obj, args);
				}
				catch (InvocationTargetException e)
				{
					throwable[0] = e.getCause();
				}
				catch (Throwable t)
				{
					throwable[0] = t;
				}
			}
		};

		Thread thread = new Thread(methodRunner, "ThreadingInvocationHandler"); //$NON-NLS-1$
		thread.start();
		try
		{
			thread.join();
		}
		catch (InterruptedException e)
		{
			Debug.error(e);
			throw new RepositoryException(e.getMessage());
		}
		if (throwable[0] != null)
		{
			throw throwable[0];
		}
		return result[0];
	}
}
