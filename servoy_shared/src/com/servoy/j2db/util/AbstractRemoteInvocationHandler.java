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
import java.rmi.Remote;
import java.util.HashSet;
import java.util.Set;

/** Base invocation handler on remote interfaces.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public abstract class AbstractRemoteInvocationHandler<T extends Remote> implements InvocationHandler, IDelegate<T>
{
	private final T remote;

	protected AbstractRemoteInvocationHandler(T remote)
	{
		this.remote = remote;
	}

	public T getDelegate()
	{
		return remote;
	}

	/*
	 * Get the remote interfaces for this class and its superclasses
	 */
	protected static Class[] getRemoteInterfaces(Class< ? > objectClass)
	{
		Set<Class< ? extends Remote>> remoteInterfaces = new HashSet<Class< ? extends Remote>>();

		for (Class< ? > cls = objectClass; cls != Object.class && cls != null; cls = cls.getSuperclass())
		{
			for (Class< ? > intf : cls.getInterfaces())
			{
				if (Remote.class.isAssignableFrom(intf))
				{
					remoteInterfaces.add((Class< ? extends Remote>)intf);
				}
			}
		}

		return remoteInterfaces.toArray(new Class[remoteInterfaces.size()]);
	}

	/**
	 * @param method
	 * @param args
	 * @throws Throwable 
	 */
	protected Object invokeMethod(Method method, Object[] args) throws Throwable
	{
		try
		{
			return method.invoke(remote, args);
		}
		catch (InvocationTargetException e)
		{
			throw e.getCause();
		}
	}
}
