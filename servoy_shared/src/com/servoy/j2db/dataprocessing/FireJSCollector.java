/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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
 * @author lvostinar
 *
 */
public class FireJSCollector
{
	private static final ThreadLocal<FireJSCollector> current = new ThreadLocal<FireJSCollector>();

	public static FireJSCollector createOrGetJSFireCollector()
	{
		FireJSCollector fireCollector = current.get();
		if (fireCollector == null)
		{
			fireCollector = new FireJSCollector();
			current.set(fireCollector);
		}
		fireCollector.depth++;
		return fireCollector;
	}

	public static FireJSCollector getJSFireCollector()
	{
		return current.get();
	}

	private final Map<Record, List<ModificationEvent>> map = new HashMap<Record, List<ModificationEvent>>();
	private int depth = 0;

	private FireJSCollector()
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
					ArrayList<Map.Entry<Record, List<ModificationEvent>>> copy = new ArrayList<Map.Entry<Record, List<ModificationEvent>>>(map.entrySet());
					map.clear();
					for (Map.Entry<Record, List<ModificationEvent>> entry : copy)
					{
						for (ModificationEvent me : entry.getValue())
						{
							entry.getKey().completeFire(me);
						}
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

	public void put(Record record, ModificationEvent me)
	{
		List<ModificationEvent> lst = map.get(record);
		if (lst == null)
		{
			lst = new ArrayList<ModificationEvent>();
			map.put(record, lst);
		}
		lst.add(me);
	}
}
