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

import java.util.concurrent.atomic.AtomicReference;

import com.servoy.j2db.util.Debug;

/**
 * This class holds a reference to the single application server instance in this JVM and provides the registry (services).
 * @author rgansevles, jblok
 */
public final class ApplicationServerRegistry
{
	private static final AtomicReference<IApplicationServerSingleton> as_instanceRef = new AtomicReference<IApplicationServerSingleton>();
	private static final AtomicReference<IServiceRegistry> reg_instanceRef = new AtomicReference<IServiceRegistry>();

	//cannot be created
	private ApplicationServerRegistry()
	{
	}

	public static IApplicationServerSingleton get()
	{
		return as_instanceRef.get();
	}

	/** 
	 * Set instance, will not overwrite existing instance
	 * @param instance
	 * @return true if successful
	 */
	public static boolean setApplicationServerSingleton(IApplicationServerSingleton instance)
	{
		return as_instanceRef.compareAndSet(null, instance) || as_instanceRef.compareAndSet(instance, instance);
	}

	/** 
	 * Set instance, will not overwrite existing instance
	 * @param instance
	 * @return true if successful
	 */
	public static boolean setServiceRegistry(IServiceRegistry instance)
	{
		return reg_instanceRef.compareAndSet(null, instance) || reg_instanceRef.compareAndSet(instance, instance);
	}

	public static <S> S getService(Class<S> reference)
	{
		IServiceRegistry reg = reg_instanceRef.get();
		if (reg != null) reg.getService(reference);
		return null;
	}

	public static IServiceRegistry getServiceRegistry()
	{
		return reg_instanceRef.get();
	}

	public static void clear()
	{
		as_instanceRef.set(null);
		reg_instanceRef.set(null);
	}

	public static boolean waitForApplicationServerStarted()
	{
		IApplicationServerSingleton instance = get();
		if (instance == null)
		{
			return false;
		}
		if (instance.isStarting())
		{
			synchronized (instance.getClass())
			{
				if (instance.isStarting())
				{
					try
					{
						instance.getClass().wait();
					}
					catch (InterruptedException e)
					{
						Debug.error(e);
					}
				}
			}
		}
		return !instance.isStarting();
	}
}
