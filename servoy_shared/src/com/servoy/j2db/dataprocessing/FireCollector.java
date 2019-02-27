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
package com.servoy.j2db.dataprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jcompagner
 *
 */
public class FireCollector implements AutoCloseable
{
	private static final ThreadLocal<FireCollector> current = new ThreadLocal<FireCollector>();

	/**
	 * Creates a new FireCollector or returns the current one of the current thread already has one.
	 * Make sure that when calling this method, the {@link #done()} method needs to be called. So that should be in a try/finally block
	 *
	 * @return {@link FireCollector}
	 */
	public static FireCollector getFireCollector()
	{
		FireCollector fireCollector = current.get();
		if (fireCollector == null)
		{
			fireCollector = new FireCollector();
			current.set(fireCollector);
		}
		fireCollector.depth++;
		return fireCollector;
	}

	private final Map<IFireCollectable, Map<IRecord, List<String>>> map = new HashMap<IFireCollectable, Map<IRecord, List<String>>>();
	private int depth = 0;

	private FireCollector()
	{
	}

	public void done()
	{
		if (depth == 1)
		{
			try
			{
				while (map.size() > 0)
				{
					ArrayList<Map.Entry<IFireCollectable, Map<IRecord, List<String>>>> copy = new ArrayList<Map.Entry<IFireCollectable, Map<IRecord, List<String>>>>(
						map.entrySet());
					map.clear();
					for (Map.Entry<IFireCollectable, Map<IRecord, List<String>>> entry : copy)
					{
						entry.getKey().completeFire(entry.getValue());
					}
				}
			}
			finally
			{
				current.remove();
			}
		}
		depth--;
	}

	/**
	 * @param parentFoundSet
	 * @param record
	 */
	public void put(IFireCollectable rowCollectable, IRecord collected, String dataproviderID)
	{
		Map<IRecord, List<String>> lst = map.get(rowCollectable);
		if (lst == null)
		{
			lst = new HashMap<IRecord, List<String>>();
			map.put(rowCollectable, lst);
		}
		List<String> dataproviders = lst.get(collected);
		if (dataproviders == null)
		{
			dataproviders = new ArrayList<String>();
			lst.put(collected, dataproviders);
		}
		if (!dataproviders.contains(dataproviderID))
		{
			dataproviders.add(dataproviderID);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close()
	{
		done();
	}

}
