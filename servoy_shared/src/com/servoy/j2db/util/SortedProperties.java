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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class SortedProperties extends Properties
{
	@Override
	public void store(Writer writer, String comments) throws IOException
	{
		Properties sorted = getSortedProperties();
		sorted.store(writer, comments);
	}

	@Override
	public void store(OutputStream out, String comments) throws IOException
	{
		Properties sorted = getSortedProperties();
		sorted.store(out, comments);
	}

	/**
	 * @return
	 */
	private Properties getSortedProperties()
	{
		Properties sortedProps = new Properties()
		{
			@Override
			public Set<Map.Entry<Object, Object>> entrySet()
			{
				/*
				 * Using comparator to avoid the following exception on jdk >=9: java.lang.ClassCastException:
				 * java.base/java.util.concurrent.ConcurrentHashMap$MapEntry cannot be cast to java.base/java.lang.Comparable
				 */
				Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<Map.Entry<Object, Object>>(new Comparator<Map.Entry<Object, Object>>()
				{
					@Override
					public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2)
					{
						return o1.getKey().toString().compareTo(o2.getKey().toString());
					}
				});
				sortedSet.addAll(super.entrySet());
				return sortedSet;
			}

			@Override
			public Set<Object> keySet()
			{
				return new TreeSet<Object>(super.keySet());
			}

			@Override
			public synchronized Enumeration<Object> keys()
			{
				return Collections.enumeration(new TreeSet<Object>(super.keySet()));
			}

		};
		sortedProps.putAll(this);
		return sortedProps;
	}

	public synchronized String toFileString()
	{
		try
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			store(out, null);
			return out.toString();
		}
		catch (Exception e)
		{
			Debug.error(e);
			return null;
		}
	}
}
