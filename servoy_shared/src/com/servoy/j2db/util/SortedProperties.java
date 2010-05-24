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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class SortedProperties extends Properties
{
	@Override
	public synchronized Enumeration keys()//return a sorted list
	{
		ArrayList newList = new ArrayList();
		Enumeration enumeration = super.keys();
		while (enumeration.hasMoreElements())
		{
			String element = (String)enumeration.nextElement();
			newList.add(element);
		}

		final Object[] array = newList.toArray();

		Arrays.sort(array);

		return new Enumeration()
		{
			int index = 0;

			public boolean hasMoreElements()
			{
				return (index < array.length);
			}

			public Object nextElement()
			{
				Object obj = array[index];
				index++;
				return obj;
			}
		};
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
