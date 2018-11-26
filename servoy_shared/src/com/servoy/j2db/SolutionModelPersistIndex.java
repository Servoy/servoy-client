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

package com.servoy.j2db;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.FilteredIterator;
import com.servoy.j2db.util.IFilter;

/**
 * @author jcompagner
 * @since 8.4
 *
 */
public class SolutionModelPersistIndex extends PersistIndex implements ISolutionModelPersistIndex
{
	private final ConcurrentMap<IPersist, Boolean> removedPersist = new ConcurrentHashMap<>();
	private final IPersistIndex index;

	public SolutionModelPersistIndex(IPersistIndex index)
	{
		this.index = index;
	}

	@Override
	public <T extends IPersist> T getPersistByUUID(String uuid, Class<T> persistClass)
	{
		T persist = null;
		if (testIndex())
		{
			persist = super.getPersistByUUID(uuid, persistClass);
		}
		if (persist == null)
		{
			persist = index.getPersistByUUID(uuid, persistClass);
		}
		if (persist != null && isRemoved(persist)) return null;
		return persist;
	}

	@Override
	public IPersist getPersistByUUID(String uuid)
	{
		IPersist persist = null;
		if (testIndex())
		{
			persist = super.getPersistByUUID(uuid);
		}
		if (persist == null)
		{
			persist = index.getPersistByUUID(uuid);
		}
		if (persist != null && isRemoved(persist)) return null;
		return persist;
	}

	@Override
	public <T extends IPersist> T getPersistByName(String name, Class<T> persistClass)
	{
		T persist = null;
		if (testIndex())
		{
			persist = super.getPersistByName(name, persistClass);
		}
		if (persist == null)
		{
			persist = index.getPersistByName(name, persistClass);
		}
		if (persist != null && isRemoved(persist)) return null;
		return persist;
	}

	@Override
	public <T extends IPersist> T getPersistByID(int id, Class<T> persistClass)
	{
		T persist = null;
		if (testIndex())
		{
			persist = super.getPersistByID(id, persistClass);
		}
		if (persist == null)
		{
			persist = index.getPersistByID(id, persistClass);
		}
		if (persist != null && isRemoved(persist)) return null;
		return persist;
	}

	@Override
	public <T extends IPersist> Iterator<T> getIterableFor(Class<T> persistClass)
	{
		Iterator<T> iterator = index.getIterableFor(persistClass);
		if (testIndex())
		{
			Set<T> set = new HashSet<>();
			super.getIterableFor(persistClass).forEachRemaining((persist) -> set.add(persist));
			if (set.size() > 0)
			{
				// must merge
				iterator.forEachRemaining((persist) -> {
					if (!set.contains(persist)) set.add(persist);
				});
				iterator = set.iterator();
			}
		}

		if (!removedPersist.isEmpty())
		{
			iterator = new FilteredIterator<T>(iterator, new IFilter<T>()
			{
				@Override
				public boolean match(Object o)
				{
					return !removedPersist.containsKey(o);
				}
			});
		}
		return iterator;
	}

	@Override
	public ISupportScope getSupportScope(String scopeName, String baseName)
	{
		ISupportScope supportScope = null;
		if (testIndex())
		{
			supportScope = super.getSupportScope(scopeName, baseName);
		}
		if (supportScope == null)
		{
			supportScope = index.getSupportScope(scopeName, baseName);
		}
		return supportScope;
	}

	private boolean testIndex()
	{
		if (uuidToPersist.isEmpty() && !solutions.isEmpty())
		{
			createIndex();
		}
		return !uuidToPersist.isEmpty();
	}


	@Override
	public void setSolutionModelSolution(Solution solution)
	{
		solutions.add(solution);
		solution.getChangeHandler().addIPersistListener(this);
	}

	private void flushIndex()
	{
		uuidToPersist.clear();
		idToPersist.clear();
		nameToPersist.clear();
		scopeCacheByName.clear();
	}

	public boolean isRemoved(IPersist persist)
	{
		return removedPersist.containsKey(persist);
	}

	public void addRemoved(IPersist persist)
	{
		removedPersist.put(persist, Boolean.TRUE);
	}

	public void removeRemoved(IPersist persist)
	{
		removedPersist.remove(persist);
	}

	public Set<IPersist> getRemoved()
	{
		return Collections.unmodifiableSet(removedPersist.keySet());
	}


	@Override
	public void itemCreated(IPersist item)
	{
		flushIndex();
	}

	@Override
	public void itemRemoved(IPersist item)
	{
		flushIndex();
	}
}
