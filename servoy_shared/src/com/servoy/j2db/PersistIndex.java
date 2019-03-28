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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.LineNumberComparator;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.IntHashMap;

/**
 * This Persist Index class is a ItemChangeListener for the persist by itself.
 * But it will not add listeners to the solutions. Subclasses need todo this, because this class is by default immutable.
 *
 * @author jcompagner
 * @since 8.4
 */
public class PersistIndex implements IItemChangeListener<IPersist>, IPersistIndex
{
	protected final ConcurrentMap<String, IPersist> uuidToPersist = new ConcurrentHashMap<>();
	protected final ConcurrentMap<Class< ? extends IPersist>, IntHashMap<IPersist>> idToPersist = new ConcurrentHashMap<>();
	// caches per persist class a map of Name->Persist (we assume that is unique!)
	protected final ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, ? extends IPersist>> nameToPersist = new ConcurrentHashMap<>();

	protected final ConcurrentMap<String, Map<String, ISupportScope>> scopeCacheByName = new ConcurrentHashMap<>();

	protected final CopyOnWriteArrayList<Solution> solutions = new CopyOnWriteArrayList<>();

	protected PersistIndex()
	{
	}

	public PersistIndex(List<Solution> solutions)
	{
		this.solutions.addAll(solutions);
		createIndex();
	}

	@Override
	public IPersist getPersistByUUID(String uuid)
	{
		return uuidToPersist.get(uuid);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IPersist> T getPersistByUUID(String uuid, Class<T> clz)
	{
		IPersist persist = uuidToPersist.get(uuid);
		if (persist != null && clz.isInstance(persist)) return (T)persist;
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IPersist> T getPersistByName(String name, Class<T> persistClass)
	{
		ConcurrentMap<String, ? extends IPersist> classToList = nameToPersist.get(persistClass);
		if (classToList == null)
		{
			final ConcurrentMap<String, T> tmp = new ConcurrentHashMap<>();
			if (ISupportName.class.isAssignableFrom(persistClass))
			{
				visit((persist) -> {
					if (persist.getClass() == persistClass)
					{
						tmp.put(((ISupportName)persist).getName(), (T)persist);
						return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				});
			}
			classToList = tmp;
			nameToPersist.put(persistClass, classToList);
		}
		T persist = (T)classToList.get(name);
		return persist;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IPersist> T getPersistByID(int id, Class<T> clz)
	{
		IntHashMap<IPersist> cacheById = idToPersist.get(clz);
		if (cacheById != null)
		{
			synchronized (cacheById)
			{
				return (T)cacheById.get(id);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends IPersist> Iterator<T> getIterableFor(Class<T> clz)
	{
		IntHashMap<IPersist> cacheById = idToPersist.get(clz);
		if (cacheById != null)
		{
			Collection<IPersist> collection = Collections.unmodifiableCollection(cacheById.values());
			return (Iterator<T>)collection.iterator();
		}
		return (Iterator<T>)Collections.emptyList().iterator();
	}

	@Override
	public ISupportScope getSupportScope(String scopeName, String baseName)
	{
		Map<String, ISupportScope> map = getGlobalScopeCache().get(scopeName);
		return map != null ? map.get(baseName) : null;
	}

	@Override
	public <T extends ISupportScope> Iterator<T> getGlobalScriptObjects(String scopeName, boolean sort, Class<T> cls)
	{
		Stream<ISupportScope> stream = null;
		Map<String, Map<String, ISupportScope>> globalMap = getGlobalScopeCache();
		if (scopeName != null)
		{
			Map<String, ISupportScope> map = globalMap.get(scopeName);
			if (map != null)
			{
				stream = map.values().stream();
			}
		}
		else
		{
			stream = globalMap.values().stream().flatMap(map -> map.values().stream());
		}
		if (stream != null)
		{
			Stream<T> filtered = stream.filter(persist -> cls.isAssignableFrom(persist.getClass())).map(persist -> (T)persist);
			if (sort) filtered = filtered.sorted(NameComparator.INSTANCE);
			else if (cls == ScriptVariable.class) filtered = filtered.sorted(LineNumberComparator.INSTANCE); // special case always sort variables on line number
			return filtered.collect(Collectors.toList()).iterator();
		}

		List<T> empty = Collections.emptyList();
		return empty.iterator();
	}

	private Map<String, Map<String, ISupportScope>> getGlobalScopeCache()
	{
		if (scopeCacheByName.isEmpty())
		{
			visit((persist) -> {
				if (persist instanceof ISupportScope)
				{
					String scopeName = ((ISupportScope)persist).getScopeName();
					if (scopeName == null)
					{
						scopeName = ScriptVariable.GLOBAL_SCOPE;
					}
					Map<String, ISupportScope> scopeMap = scopeCacheByName.get(scopeName);
					if (scopeMap == null)
					{
						Map<String, ISupportScope> prev = scopeCacheByName.putIfAbsent(scopeName, scopeMap = new HashMap<String, ISupportScope>());
						if (prev != null) scopeMap = prev;
					}
					scopeMap.put(((ISupportScope)persist).getName(), (ISupportScope)persist);

				}
				return persist instanceof Solution ? IPersistVisitor.CONTINUE_TRAVERSAL : IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			});
		}
		return scopeCacheByName;
	}

	protected final void createIndex()
	{
		visit((persist) -> {
			uuidToPersist.put(persist.getUUID().toString(), persist);
			IntHashMap<IPersist> cacheById = idToPersist.get(persist.getClass());
			if (cacheById == null)
			{
				cacheById = new IntHashMap<>();
				IntHashMap<IPersist> currentValue = idToPersist.putIfAbsent(persist.getClass(), cacheById);
				if (currentValue != null) cacheById = currentValue;
			}
			synchronized (cacheById)
			{
				cacheById.put(persist.getID(), persist);
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		});
	}

	protected final void visit(IPersistVisitor visitor)
	{
		for (Solution solution : solutions)
		{
			solution.acceptVisitor(visitor);
		}
	}

	@Override
	public void reload()
	{
		uuidToPersist.clear();
		idToPersist.clear();
		nameToPersist.clear();
		scopeCacheByName.clear();
		createIndex();
	}

	public void destroy()
	{
		Solution[] sols = solutions.toArray(new Solution[solutions.size()]);
		solutions.clear();
		for (Solution solution : sols)
		{
			solution.getChangeHandler().removeIPersistListener(this);
		}
		uuidToPersist.clear();
		idToPersist.clear();
		nameToPersist.clear();
		scopeCacheByName.clear();
	}

	@Override
	public void itemCreated(IPersist item)
	{
		if (uuidToPersist.isEmpty()) return;
		uuidToPersist.put(item.getUUID().toString(), item);
		IntHashMap<IPersist> cacheById = idToPersist.get(item.getClass());
		if (cacheById == null)
		{
			cacheById = new IntHashMap<>();
			IntHashMap<IPersist> currentValue = idToPersist.putIfAbsent(item.getClass(), cacheById);
			if (currentValue != null) cacheById = currentValue;
		}
		synchronized (cacheById)
		{
			cacheById.put(item.getID(), item);
		}
		nameToPersist.remove(item.getClass());
		if (item instanceof ISupportChilds)
		{
			// If a form (or any isupport childs is added, also add all children); this is in mirror with remove
			Iterator<IPersist> allObjects = ((ISupportChilds)item).getAllObjects();
			while (allObjects.hasNext())
			{
				itemCreated(allObjects.next());
			}
		}
		if (item instanceof ISupportScope)
		{
			scopeCacheByName.clear();
		}
	}

	@Override
	public void itemRemoved(IPersist item)
	{
		if (uuidToPersist.isEmpty()) return;
		uuidToPersist.remove(item.getUUID().toString());
		IntHashMap<IPersist> idCache = idToPersist.get(item.getClass());
		if (idCache != null)
		{
			synchronized (idCache)
			{
				idCache.remove(item.getID());
			}
		}
		nameToPersist.remove(item.getClass());
		if (item instanceof ISupportChilds)
		{
			// If a form (or any isupport childs is removed, also remove all children)
			Iterator<IPersist> allObjects = ((ISupportChilds)item).getAllObjects();
			while (allObjects.hasNext())
			{
				itemRemoved(allObjects.next());
			}
		}
		if (item instanceof ISupportScope)
		{
			scopeCacheByName.clear();
		}
	}

	@Override
	public void itemChanged(IPersist item)
	{
		if (uuidToPersist.isEmpty()) return;
		// just update the persist by the same uuid in the cache, uuid or id should not change.
		uuidToPersist.put(item.getUUID().toString(), item);
		IntHashMap<IPersist> cacheById = idToPersist.get(item.getClass());
		// changed item should be in the cache..
		synchronized (cacheById)
		{
			cacheById.put(item.getID(), item);
		}
		nameToPersist.remove(item.getClass());
		if (item instanceof ISupportScope)
		{
			scopeCacheByName.clear();
		}
	}

	@Override
	public void itemChanged(Collection<IPersist> items)
	{
		for (IPersist persist : items)
		{
			itemChanged(persist);
		}
	}
}
