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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.servoy.j2db.util.LinkedListWithAccessToNodes;
import com.servoy.j2db.util.LinkedListWithAccessToNodes.Node;

/**
 * @author jcompagner
 */
public interface IFireCollectable
{

	/**
	 * Make sure that the Set<String> are never null! They should always contain at least 1 String (dataprovider).
	 * See {@link FireCollector}.
	 */
	default void completeFire(Map<IRecord, Set<String>> entries)
	{
		int start = Integer.MAX_VALUE;
		int end = -1;
		Set<String> dataproviders = null;
		for (Entry<IRecord, Set<String>> entry : entries.entrySet())
		{
			int index = getRecordIndex(entry.getKey());
			if (index != -1 && start > index)
			{
				start = index;
			}
			if (end < index)
			{
				end = index;
			}
			if (dataproviders == null) dataproviders = entry.getValue();
			else dataproviders.addAll(entry.getValue());
		}
		if (start != Integer.MAX_VALUE && end != -1)
		{
			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE, dataproviders);
		}
	}

	// this is a duplicate of the one in IFoundset; it's needed by the "default" method above
	public int getRecordIndex(IRecord record);

	void fireFoundSetEvent(int firstRow, int lastRow, int changeType, Set<String> dataproviders);

}
