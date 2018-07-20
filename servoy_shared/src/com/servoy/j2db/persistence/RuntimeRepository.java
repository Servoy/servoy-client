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

package com.servoy.j2db.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.servoy.j2db.util.UUID;

/**
 * @author Jan Blok
 *
 */
public class RuntimeRepository implements IRemoteRepository
{
	private final IServerManagerInternal serverManager;
	private final Map<String, Solution> solutionAndmodules;
	private final String solutionName;

	public RuntimeRepository(IServerManagerInternal serverManager, Map<String, Solution> solutionAndmodules, String solutionName)
	{
		this.serverManager = serverManager;
		this.solutionAndmodules = solutionAndmodules;
		this.solutionName = solutionName;
	}

	public IRootObject getActiveRootObject(int id)
	{
		for (Solution m : solutionAndmodules.values())
		{
			if (m.getID() == id)
			{
				return m;
			}
		}
		return null;
	}

	public IRootObject getActiveRootObject(String name, int type)
	{
		if (type == IRepository.SOLUTIONS)
		{
			if (name == null) name = solutionName;
			return solutionAndmodules.get(name);
		}
		if (type == IRepository.STYLES)
		{
			for (Solution m : solutionAndmodules.values())
			{
				Map<String, Style> styles = m.getSerializableRuntimeProperty(Solution.PRE_LOADED_STYLES);
				if (styles != null && styles.containsKey(name))
				{
					return styles.get(name);
				}
			}
		}
//		if (type ==IRepository.TEMPLATES) // client does not load templates
		return null;
	}

	public long[] getActiveRootObjectsLastModified(int[] rootObjectIds)
	{
		return new long[] { 1 };
	}

	public ConcurrentMap<String, IServer> getServerProxies(RootObjectMetaData[] metas)
	{
		ConcurrentMap<String, IServer> sp = new ConcurrentHashMap<String, IServer>();
		String[] names = getServerNames(false);
		for (String name : names)
		{
			sp.put(name, getServer(name));
		}
		return sp;
	}

	public String[] getServerNames(boolean sort)
	{
		return serverManager.getServerNames(true, true, sort, false);
	}

	public IServer getServer(String name)
	{
		return serverManager.getServer(name);
	}

	public String[] getDuplicateServerNames(String name)
	{
		return getServer(name) == null ? new String[0] : new String[] { name };
	}

	public RootObjectMetaData getRootObjectMetaData(UUID uuid)
	{
		return null;
	}

	public RootObjectMetaData getRootObjectMetaData(int rootObjectId)
	{
		return null;
	}

	public RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId)
	{
		if (objectTypeId == IRepository.SOLUTIONS)
		{
			Solution m = solutionAndmodules.get(name);
			if (m != null)
			{
				return m.getSolutionMetaData();
			}
		}
		return null;
	}

	public RootObjectMetaData[] getRootObjectMetaDatas()
	{
		return getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
	}

	public RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId)
	{
		List<RootObjectMetaData> retVal = new ArrayList<RootObjectMetaData>();
		for (Solution m : solutionAndmodules.values())
		{
			retVal.add(m.getSolutionMetaData());
		}
		return retVal.toArray(new RootObjectMetaData[solutionAndmodules.size()]);
	}

	public byte[] getMediaBlob(int blob_id)
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.persistence.IRepository#getModuleMetaDatas(int)
	 */
	public List<RootObjectReference> getActiveSolutionModuleMetaDatas(int solutionId)
	{
		// Note: referencedModules includes main solution
		List<RootObjectReference> retVal = new ArrayList<RootObjectReference>();
		for (Solution m : solutionAndmodules.values())
		{
			retVal.add(new RootObjectReference(m.getName(), m.getUUID(), m.getSolutionMetaData(), 1));
		}
		return retVal;
	}
}