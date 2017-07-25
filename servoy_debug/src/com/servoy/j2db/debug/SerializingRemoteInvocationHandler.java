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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
public class SerializingRemoteInvocationHandler<T extends Remote> extends AbstractRemoteInvocationHandler<T>
{
	private final IApplication application;

	private SerializingRemoteInvocationHandler(IApplication application, T remote)
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
		return (T)Proxy.newProxyInstance(obj.getClass().getClassLoader(), interfaces, new SerializingRemoteInvocationHandler<T>(application, obj));
	}

	/**
	 * Write the object to an object stream and read it back.
	 * @param <O>
	 * @param o
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public <O> O serializeAndDeserialize(O o) throws IOException, ClassNotFoundException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Map<ReplacedObject, Object> map = new HashMap<>();
		ObjectOutputStream os = new ReplaceObjectOutputStream(baos, map);
		os.writeObject(o);
		os.close();

		ObjectInputStream is = new ResolveObjectInputStream(new ByteArrayInputStream(baos.toByteArray()), map);
		@SuppressWarnings("unchecked")
		O o2 = (O)is.readObject();
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

	/**
	 * @author jcompagner
	 *
	 */
	private final class ResolveObjectInputStream extends ObjectInputStream
	{
		private final Map<ReplacedObject, Object> map;

		/**
		 * @param in
		 */
		private ResolveObjectInputStream(InputStream in, Map<ReplacedObject, Object> map) throws IOException
		{
			super(in);
			this.map = map;
			enableResolveObject(true);
		}

		@Override
		protected Class< ? > resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
		{
			String name = desc.getName();
			if (name.endsWith("SerializingRemoteInvocationHandler$ReplacedObject"))
			{
				return super.resolveClass(desc);
			}
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

		@Override
		protected Object resolveObject(Object obj) throws IOException
		{
			Object o = map.get(obj);
			if (o != null) return o;
			return super.resolveObject(obj);
		}
	}

	private static final class ReplaceObjectOutputStream extends ObjectOutputStream
	{
		private final Map<ReplacedObject, Object> replacedCache;

		/**
		 * @param out
		 */
		private ReplaceObjectOutputStream(OutputStream out, Map<ReplacedObject, Object> replacedCache) throws IOException
		{
			super(out);
			this.replacedCache = replacedCache;
			enableReplaceObject(true);
		}

		@Override
		protected Object replaceObject(Object obj) throws IOException
		{
			if (obj instanceof Remote)
			{
				ReplacedObject ro = new ReplacedObject();
				replacedCache.put(ro, obj);
				return ro;
			}
			return super.replaceObject(obj);
		}
	}

	private static final class ReplacedObject implements Serializable
	{
		private final UUID uuid = UUID.randomUUID();

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof ReplacedObject)
			{
				return uuid.equals(((ReplacedObject)obj).uuid);
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return uuid.hashCode();
		}
	}

}
