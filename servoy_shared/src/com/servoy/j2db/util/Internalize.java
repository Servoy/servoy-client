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

import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Class to make serializable objects smaller, by reusing same immutable objects, also will help reduce memory in many client (=server) environment.
 * 
 * @author jblok
 */
public final class Internalize
{
	private static final Internalize instance = new Internalize();

	static volatile int maxsize = 50000;
	private final ConcurrentHashMap<Object, ObjectHolder> internedMap = new ConcurrentHashMap<Object, ObjectHolder>(5000);
	private final AtomicBoolean makingroom = new AtomicBoolean(false);

	private Internalize()
	{
	}

	public static void setMaxSize(int size)
	{
		maxsize = size;
		if (size <= 0)
		{
			instance.clear();
		}
	}

	public static Object intern(Object obj)
	{
		if (obj == null) return obj;
		if (obj instanceof Boolean)
		{
			return (((Boolean)obj).booleanValue() ? Boolean.TRUE : Boolean.FALSE);
		}
		if (maxsize <= 0) return obj;
		if (obj instanceof String || obj instanceof Number)//ONLY handle immutable objects
		{
			return instance.add(obj);
		}
		else if (obj instanceof Object[])
		{
			Object[] array = (Object[])obj;
			for (int i = 0; i < array.length; i++)
			{
				array[i] = intern(array[i]);
			}
			return array;
		}
		return obj;
	}

	/**
	 * 
	 */
	private void clear()
	{
		internedMap.clear();
	}


	private Object add(Object obj)
	{
		ObjectHolder retValue = internedMap.get(obj);
		if (retValue == null)
		{
			if (internedMap.size() >= maxsize)
			{
				// clear less used.
				makeRoom();
			}
			internedMap.put(obj, new ObjectHolder(obj));
			return obj;
		}
		else
		{
			// if making room then don't increment.
			if (!makingroom.get()) retValue.increment();
			return retValue.object;
		}
	}

	private void makeRoom()
	{
		if (makingroom.compareAndSet(false, true))
		{
			try
			{
				TreeSet<ObjectHolder> set = new TreeSet<ObjectHolder>(internedMap.values());
				int counter = 0;
				// clean 1/5 of max objects.
				int toRemove = maxsize / 5;
				for (ObjectHolder objectHolder : set)
				{
					internedMap.remove(objectHolder.object);
					if (++counter == toRemove) break;
				}
			}
			finally
			{
				makingroom.set(false);
			}
		}
	}

	private final static class ObjectHolder implements Comparable<ObjectHolder>
	{
		private final Object object;
		private volatile int counter = 0;

		/**
		 * @param obj
		 */
		public ObjectHolder(Object obj)
		{
			object = obj;
		}

		public final void increment()
		{
			counter++;
			if (counter < 0) counter = 1000;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public final int compareTo(ObjectHolder o)
		{
			int compare = counter - o.counter;
			// if compare is 0 then test for hash, for the same hash 1 of them will fall out.
			// but that will be picked up the next time, the chance is not that great.
			return compare == 0 ? object.hashCode() - o.object.hashCode() : compare;
		}
	}
}
