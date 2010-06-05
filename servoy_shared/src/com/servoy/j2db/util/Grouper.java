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



import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Grouper
{

	public static Object group(Set objectSet, IGroupInfo groupInfo)
	{
		// Create the root sorted map.
		Map root = new HashMap();

		// Get the number of fields to group by, and the comparator
		// to use to sort the remaining elements.
		int fieldCount = groupInfo.getFieldCount();
		Comparator objectComparator = groupInfo.getObjectComparator();

		// Iterate over all the objects in the object set, grouping
		// each element appropriately.
		Iterator iterator = objectSet.iterator();
		while (iterator.hasNext())
		{
			// Get the next object.
			Object object = iterator.next();

			// Current group is the root, current field is null.
			Map group = root;
			Object field = null;

			// Find the correct child node to which to add the object.
			// At each stage, construct the new group if it does not exist.
			for (int i = 0; i < fieldCount; i++)
			{
				// Get the child for the current group, and create it if it does not yet exist.
				Map child = (Map) group.get(field);
				if (child == null)
				{
					child = new TreeMap(groupInfo.getFieldComparator(i));
					group.put(field, child);
				}

				// Continue from the child.
				group = child;

				// Get the field value for this group.
				field = groupInfo.getFieldValue(object, i);
			}

			// Get the set that these values should be added to, and create it if it does not yet exist.
			Set set = (Set) group.get(field);
			if (set == null)
			{
				set = new TreeSet(objectComparator);
				group.put(field, set);
			}

			// Add the object to the correct set.
			set.add(object);
		}
		
		return root.get(null);
	}
	
	public static String format(Object groupResult, IGroupFormatter groupFormatter)
	{
		return format(groupResult, groupFormatter, 0);
	}
	
	private static String format(Object groupResult, IGroupFormatter groupFormatter, int depth)
	{
		StringBuffer result = new StringBuffer();
		if (groupResult instanceof Set)
		{
			result.append(groupFormatter.startObjectList());
			Iterator iterator = ((Set) groupResult).iterator();
			while (iterator.hasNext())
			{
				Object object = iterator.next();
				result.append(groupFormatter.formatObject(object));
				if (iterator.hasNext())
				{
					result.append(groupFormatter.objectSeperator());
				}
			}
			result.append(groupFormatter.endObjectList());
		}
		else if (groupResult instanceof Map)
		{
			result.append(groupFormatter.startGroup(depth));
			Iterator iterator = ((Map) groupResult).entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry entry = (Map.Entry) iterator.next();
				result.append(groupFormatter.formatField(depth, entry.getKey()));
				result.append(format(entry.getValue(), groupFormatter, depth + 1));
				if (iterator.hasNext())
				{
					result.append(groupFormatter.groupSeperator(depth));
				}
			}
			result.append(groupFormatter.endGroup(depth));
		}
		return result.toString();
	}

}
