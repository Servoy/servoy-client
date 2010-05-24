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
public class FireCollector
{
	private final Map<IFireCollectable, List<Object>> map = new HashMap<IFireCollectable, List<Object>>();

	/**
	 * 
	 */
	public void done()
	{
		for (Map.Entry<IFireCollectable, List<Object>> entry : map.entrySet())
		{
			entry.getKey().completeFire(entry.getValue());
		}
	}

	/**
	 * @param parentFoundSet
	 * @param record
	 */
	public void put(IFireCollectable rowCollectable, Object collected)
	{
		List<Object> lst = map.get(rowCollectable);
		if (lst == null)
		{
			lst = new ArrayList<Object>();
			map.put(rowCollectable, lst);
		}
		lst.add(collected);
	}

}
