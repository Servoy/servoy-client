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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author rob
 *
 */
public class ReplacedObject implements Serializable
{
	private static Map classMapping = new HashMap();
	private static Map typeMapping = new HashMap();
	
	private short t; // type
	private Object o; // object
	private String d; // domain;
	
	public ReplacedObject(String domain, Class cls, Object o)
	{
		this.d = domain;
		this.t = getType(domain, cls);
		this.o = o;
	}
	
	
	
	public static void installClassMapping(String domain, Map map)
	{
		classMapping.put(domain, map);
	}

	private static short getType(String domain, Class cls)
	{
		Map domainMapping = (Map) classMapping.get(domain);
		if (domainMapping == null)
		{
			throw new IllegalStateException("Classmapping for domain "+domain+" not installed"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		String className = cls.getName();
		Number type = (Number)domainMapping.get(className);
		if (type == null)
		{
			throw new IllegalArgumentException("No class mapping defined for serialization for class "+className+" in domain "+domain); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		return type.shortValue();
	}
	
	
	private static Class getClass(String domain, short type) throws ClassNotFoundException
	{
		Map domainTypeMapping = (Map) typeMapping.get(domain);
		if (domainTypeMapping == null)
		{
			domainTypeMapping = new HashMap();
			
			Map domainMapping = (Map) classMapping.get(domain);
			if (domainMapping == null)
			{
				throw new IllegalStateException("Classmapping for domain "+domain+" not installed"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			Iterator it = domainMapping.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry entry = (Entry) it.next();
				domainTypeMapping.put(entry.getValue(), entry.getKey());
			}
			typeMapping.put(domain, domainTypeMapping);
		}
		
		String className = (String) domainTypeMapping.get(new Short(type));
		if (className == null)
		{
			throw new IllegalStateException("Class name not found for type "+type+" in domain "+domain); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return Class.forName(className);
	}
	
	
	protected Object readResolve()
	{
		try
		{
			Class cls = getClass(d, t);
			Constructor constructor = cls.getConstructor(new Class[] { getClass()});
			return constructor.newInstance(new Object[] { this} );
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error resolving replaced object in domain "+d+" of type "+t+':'+o, e); //$NON-NLS-1$ //$NON-NLS-2$
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
	public static Object[] convertArray(Object[] array, Class componentClass)
	{
		if (array == null)
		{
			return null;
		}
		Object[] res = (Object[])java.lang.reflect.Array.newInstance(
				componentClass, array.length);
		System.arraycopy(array, 0, res, 0, array.length);
		return res;
	}
}
