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
package com.servoy.j2db.util.serialize;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author rgansevles
 *
 */
public class ReplacedObject implements Serializable
{
	private static Map<String, Map<Class< ? extends IWriteReplace>, Short>> classMapping = new HashMap<String, Map<Class< ? extends IWriteReplace>, Short>>();
	private static Map<String, Map<Short, Class< ? extends IWriteReplace>>> typeMapping = new HashMap<String, Map<Short, Class< ? extends IWriteReplace>>>();

	private final short t; // type
	private final Object o; // object
	private final String d; // domain;

	public ReplacedObject(String domain, Class< ? > cls, Object o)
	{
		this.d = domain;
		this.t = getType(domain, cls);
		this.o = o;
	}

	public static void installClassMapping(String domain, Map<Class< ? extends IWriteReplace>, Short> map)
	{
		classMapping.put(domain, map);
	}

	public static Collection<Class< ? extends IWriteReplace>> getDomainClasses(String domain)
	{
		Map<Class< ? extends IWriteReplace>, Short> domainMapping = classMapping.get(domain);
		if (domainMapping == null)
		{
			throw new IllegalStateException("Classmapping for domain " + domain + " not installed"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return domainMapping.keySet();
	}

	private static short getType(String domain, Class< ? > cls)
	{
		Map<Class< ? extends IWriteReplace>, Short> domainMapping = classMapping.get(domain);
		if (domainMapping == null)
		{
			throw new IllegalStateException("Classmapping for domain " + domain + " not installed"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Short type = domainMapping.get(cls);
		if (type == null)
		{
			throw new IllegalArgumentException("No class mapping defined for serialization for class " + cls.getName() + " in domain " + domain); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return type.shortValue();
	}


	private static Class< ? extends IWriteReplace> getClass(String domain, short type)
	{
		Map<Short, Class< ? extends IWriteReplace>> domainTypeMapping = typeMapping.get(domain);
		if (domainTypeMapping == null)
		{
			domainTypeMapping = new HashMap<Short, Class< ? extends IWriteReplace>>();

			Map<Class< ? extends IWriteReplace>, Short> domainMapping = classMapping.get(domain);
			if (domainMapping == null)
			{
				throw new IllegalStateException("Classmapping for domain " + domain + " not installed"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			for (Entry<Class< ? extends IWriteReplace>, Short> entry : domainMapping.entrySet())
			{
				domainTypeMapping.put(entry.getValue(), entry.getKey());
			}
			typeMapping.put(domain, domainTypeMapping);
		}

		Class< ? extends IWriteReplace> cls = domainTypeMapping.get(Short.valueOf(type));
		if (cls == null)
		{
			throw new IllegalStateException("Class name not found for type " + type + " in domain " + domain); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return cls;
	}

	protected Object readResolve()
	{
		try
		{
			Class< ? extends IWriteReplace> cls = getClass(d, t);
			Constructor< ? extends IWriteReplace> constructor = cls.getConstructor(new Class[] { getClass() });
			return constructor.newInstance(new Object[] { this });
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error resolving replaced object in domain " + d + " of type " + t + ':' + o, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}


	/**
	 * @return
	 */
	public Object getObject()
	{
		return o;
	}


	/** Utility method to convert array type and copy elements.
	 * @param array
	 * @param componentClass
	 * @return
	 */
	public static Object[] convertArray(Object[] array, Class< ? > componentClass)
	{
		if (array == null)
		{
			return null;
		}
		Object[] res = (Object[])java.lang.reflect.Array.newInstance(componentClass, array.length);
		System.arraycopy(array, 0, res, 0, array.length);
		return res;
	}
}
