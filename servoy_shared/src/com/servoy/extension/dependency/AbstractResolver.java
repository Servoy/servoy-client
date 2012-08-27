/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension.dependency;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.extension.IMessageProvider;
import com.servoy.extension.LibDependencyDeclaration;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.VersionStringUtils;

/**
 * Base class providing common functionality for install/uninstall/replace dependency resolving operations.
 * @author acostescu
 */
public abstract class AbstractResolver implements IMessageProvider
{

	protected final MessageKeeper messages = new MessageKeeper();

	/**
	 * Can be called after a call to {@link #resolveDependencies(String, String)}.
	 * @return a list of any messages that should be presented to the user.
	 */
	public synchronized Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clearMessages();
	}

	protected boolean haveLibConflicts(List<TrackableLibDependencyDeclaration> libs1, List<TrackableLibDependencyDeclaration> libs2)
	{
		int combinedSize = libs1.size() + libs2.size();
		if (combinedSize > 1)
		{
			List<String> availableLibVersions = new ArrayList<String>(combinedSize);
			for (LibDependencyDeclaration sameExistingLib : libs1)
			{
				availableLibVersions.add(sameExistingLib.version);
			}
			for (LibDependencyDeclaration sameExistingLib : libs2)
			{
				availableLibVersions.add(sameExistingLib.version);
			}
			for (LibDependencyDeclaration sameExistingLib : libs1)
			{
				filterOutIncompatibleLibVersions(sameExistingLib, availableLibVersions);
			}
			for (LibDependencyDeclaration sameExistingLib : libs2)
			{
				filterOutIncompatibleLibVersions(sameExistingLib, availableLibVersions);
			}
			return availableLibVersions.size() == 0;
		}
		else
		{
			return false;
		}
	}

	/**
	 * It will remove from the availableLibVersions list the versions that are not compatible with the given lib.
	 */
	protected void filterOutIncompatibleLibVersions(LibDependencyDeclaration lib, List<String> availableLibVersions)
	{
		Iterator<String> it = availableLibVersions.iterator();
		while (it.hasNext())
		{
			String ver = it.next();
			if (!VersionStringUtils.belongsToInterval(ver, lib.minVersion, lib.maxVersion))
			{
				it.remove();
			}
		}
	}

	protected void addToLibsMap(String extensionId, String extensionVersion, LibDependencyDeclaration[] libDependencies,
		Map<String, List<TrackableLibDependencyDeclaration>> libsMap, boolean installed)
	{
		for (LibDependencyDeclaration lib : libDependencies)
		{
			List<TrackableLibDependencyDeclaration> x = libsMap.get(lib.id);
			if (x == null)
			{
				x = new ArrayList<TrackableLibDependencyDeclaration>(1);
				libsMap.put(lib.id, x);
			}
			x.add(new TrackableLibDependencyDeclaration(lib, extensionId, extensionVersion, installed));
		}
	}

	protected void removeFromLibsMap(String extensionId, LibDependencyDeclaration[] libDependencies,
		Map<String, List<TrackableLibDependencyDeclaration>> libsMap)
	{
		for (LibDependencyDeclaration lib : libDependencies)
		{
			List<TrackableLibDependencyDeclaration> x = libsMap.get(lib.id);
			if (x != null)
			{
				Iterator<TrackableLibDependencyDeclaration> it = x.iterator();
				while (it.hasNext())
				{
					if (it.next().declaringExtensionId.equals(extensionId)) it.remove();
				}
				if (x.size() == 0) libsMap.remove(lib.id);
			}
		}
	}

}