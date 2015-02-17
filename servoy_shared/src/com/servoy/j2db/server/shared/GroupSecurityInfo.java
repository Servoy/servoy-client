/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.shared;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.UUID;

public class GroupSecurityInfo implements ISupportName, PCloneable<GroupSecurityInfo>
{
	public String groupName;
	public Map<String, List<SecurityInfo>> tableSecurity = new ConcurrentHashMap<String, List<SecurityInfo>>();//servername.tablename -> SecurityInfos
	public Map<UUID, List<SecurityInfo>> formSecurity = new ConcurrentHashMap<UUID, List<SecurityInfo>>();//form_uuid -> SecurityInfos

	/**
	 * Creates a new instance for the group with the given name.<BR>
	 * tableSecurity and formSecurity will be != null and empty.
	 *
	 * @param groupName the group name.
	 */
	public GroupSecurityInfo(String groupName)
	{
		this.groupName = groupName;
	}

	public String getName()
	{
		return groupName;
	}

	@Override
	public GroupSecurityInfo clone()
	{
		GroupSecurityInfo gsi = new GroupSecurityInfo(groupName);
		for (Entry<String, List<SecurityInfo>> element : tableSecurity.entrySet())
		{
			List<SecurityInfo> valCopy = null;
			if (element.getValue() != null)
			{
				valCopy = new SortedList<SecurityInfo>(element.getValue().size());
				valCopy.addAll(element.getValue());
			}
			gsi.tableSecurity.put(element.getKey(), valCopy);
		}
		for (Entry<UUID, List<SecurityInfo>> element : formSecurity.entrySet())
		{
			List<SecurityInfo> valCopy = null;
			if (element.getValue() != null)
			{
				valCopy = new SortedList<SecurityInfo>(element.getValue().size());
				valCopy.addAll(element.getValue());
			}
			gsi.formSecurity.put(element.getKey(), valCopy);
		}
		return gsi;
	}

}
