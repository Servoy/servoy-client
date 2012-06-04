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

package com.servoy.j2db.debug;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;

import org.apache.wicket.util.io.ByteArrayOutputStream;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.AbstractRemoteInvocationHandler;
import com.servoy.j2db.util.Debug;

/**
 * Proxy invocationHandler that serializes/deserializes arguments to method calls and the result.
 * This can be used to mimic rmi access from debug SC in developer.
 * 
 * @author rgansevles
 * 
 * @since 6.0
 *
 */
public class SerializingRemoteInvocationHandler extends AbstractRemoteInvocationHandler
{
	private final IApplication application;

	private SerializingRemoteInvocationHandler(IApplication application, Remote remote)
	{
		super(remote);
		this.application = application;
	}

	public static <T extends Remote> T createSerializingSerializingInvocationHandler(IApplication application, T obj)
	{
		if (obj == null)
		{
			return null;
		}
		return createSerializingSerializingInvocationHandler(application, obj, getRemoteInterfaces(obj.getClass()));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Remote> T createSerializingSerializingInvocationHandler(IApplication application, T obj, Class< ? >[] interfaces)
	{
		if (obj == null)
		{
			return null;
		}
		return (T)Proxy.newProxyInstance(obj.getClass().getClassLoader(), interfaces, new SerializingRemoteInvocationHandler(application, obj));
	}

	/**
	 * Write the object to an object stream and read it back.
	 * @param <T>
	 * @param o
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public <T> T serializeAndDeserialize(T o) throws IOException, ClassNotFoundException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(baos);
		os.writeObject(o);
		os.close();

		ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))
		{
			@Override
			protected Class< ? > resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
			{
				String name = desc.getName();
				try
				{
					return Class.forName(name, false, application.getBeanManager().getClassLoader());
				}
				catch (ClassNotFoundException e)
				{
					Debug.error("Could not load class using beanmanager classloader, trying default", e);
					return super.resolveClass(desc);
				}
			}
		};
		@SuppressWarnings("unchecked")
		T o2 = (T)is.readObject();
		is.close();

		return o2;
	}

	/*
	 * Serialize/deserialize the arguments and the return object.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		Object[] serializedArgs = serializeAndDeserialize(args);
		Object res = invokeMethod(method, serializedArgs);
		return serializeAndDeserialize(res);
	}
}
