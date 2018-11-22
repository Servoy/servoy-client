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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.IntHashMap;

/**
 * @author jcompagner
 * @since 8.4
 */
public class PersistIndex implements IItemChangeListener<IPersist>
{
	private final ConcurrentMap<String, IPersist> uuidToPersist = new ConcurrentHashMap<>();
	private final IntHashMap<IPersist> idToPersist = new IntHashMap<>();
	// caches per persist class a map of Name->Persist (we assume that is unique!)
	private final ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, ? extends IPersist>> nameToPersist = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, Map<String, ISupportScope>> scopeCacheByName = new ConcurrentHashMap<>();

	private final CopyOnWriteArrayList<Solution> solutions = new CopyOnWriteArrayList<>();

	private final ConcurrentMap<IPersist, Boolean> removedPersist = new ConcurrentHashMap<>();

	private CopySolutionPersistListener copyPersistListener;

	/**
	 * @param uuid
	 * @return the perist found i the index.
	 */
	public IPersist getPersistByUUID(String uuid)
	{
		if (uuidToPersist.isEmpty()) createIndex();
		return uuidToPersist.get(uuid);
	}

	/**
	 * @param uuid
	 * @return the perist found i the index.
	 */
	@SuppressWarnings("unchecked")
	public <T extends IPersist> T getPersistByUUID(String uuid, Class<T> clz)
	{
		if (uuidToPersist.isEmpty()) createIndex();
		IPersist persist = uuidToPersist.get(uuid);
		if (persist != null && clz.isInstance(persist)) return (T)persist;
		return null;
	}

	/**
	 * This cache assumes that for the given persist class the persist that are in the cache have unique names.
	 * So Forms/ValueList/Relations. But this won't work for Fields or WebComponents because those are only unique by there container Form.
	 * @param name
	 *  @return the perist found having that name
	 */
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
					if (persist.getClass() == persistClass && !isRemoved(persist))
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

	/**
	 * @param id
	 * @return the perist found i the index.
	 */
	@SuppressWarnings("unchecked")
	public <T extends IPersist> T getPersistByID(int id, Class<T> clz)
	{
		if (uuidToPersist.isEmpty()) createIndex();
		synchronized (idToPersist)
		{
			IPersist persist = idToPersist.get(id);
			if (persist != null && clz.isInstance(persist)) return (T)persist;
		}
		return null;
	}


	/**
	 * @param scopeName
	 * @param baseName
	 * @return
	 */
	public ISupportScope getSupportScope(String scopeName, String baseName)
	{
		Map<String, ISupportScope> map = getGlobalScopeCache().get(scopeName);
		return map != null ? map.get(baseName) : null;
	}

	private Map<String, Map<String, ISupportScope>> getGlobalScopeCache()
	{
		if (scopeCacheByName.isEmpty())
		{
			visit((persist) -> {
				if (persist instanceof ISupportScope && !isRemoved(persist))
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

	protected void load(FlattenedSolution fs)
	{
		Solution solution = fs.getSolution();
		if (solution != null)
		{

			solution.getChangeHandler().addIPersistListener(this);
			solutions.add(solution);

			Solution[] modules = fs.getModules();
			if (modules != null)
			{
				for (Solution module : modules)
				{
					module.getChangeHandler().addIPersistListener(this);
					solutions.add(module);
				}
			}
		}
	}

	private void createIndex()
	{
		visit((persist) -> {
			if (!isRemoved(persist))
			{
				uuidToPersist.put(persist.getUUID().toString(), persist);
				idToPersist.put(persist.getID(), persist);
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		});
	}

	protected void visit(IPersistVisitor visitor)
	{
		for (Solution solution : solutions)
		{
			solution.acceptVisitor(visitor);
		}
	}

	protected void flush()
	{
		Solution[] sols = solutions.toArray(new Solution[solutions.size()]);
		solutions.clear();
		for (Solution solution : sols)
		{
			solution.getChangeHandler().removeIPersistListener(this);
			if (copyPersistListener != null) solution.getChangeHandler().removeIPersistListener(copyPersistListener);
		}
		flushIndex();
		removedPersist.clear();

		copyPersistListener = null;
	}

	private void flushIndex()
	{
		uuidToPersist.clear();
		idToPersist.clear();
		nameToPersist.clear();
		scopeCacheByName.clear();
	}

	boolean isLoaded()
	{
		return solutions.size() > 0;
	}

	boolean isRemoved(IPersist persist)
	{
		return removedPersist.containsKey(persist);
	}

	void addRemoved(IPersist persist)
	{
		uuidToPersist.remove(persist.getUUID().toString());
		synchronized (idToPersist)
		{
			idToPersist.remove(persist.getID());
		}
		nameToPersist.remove(persist.getClass());
		removedPersist.put(persist, Boolean.TRUE);
	}

	void removeRemoved(IPersist persist)
	{
		uuidToPersist.put(persist.getUUID().toString(), persist);
		synchronized (idToPersist)
		{
			idToPersist.put(persist.getID(), persist);
		}
		nameToPersist.remove(persist.getClass());
		removedPersist.remove(persist);
	}

	Set<IPersist> getRemoved()
	{
		return Collections.unmodifiableSet(removedPersist.keySet());
	}

	@Override
	public void itemCreated(IPersist item)
	{
		uuidToPersist.put(item.getUUID().toString(), item);
		synchronized (idToPersist)
		{
			idToPersist.put(item.getID(), item);
		}
		nameToPersist.remove(item.getClass());
		if (item instanceof ISupportScope)
		{
			scopeCacheByName.clear();
		}
	}

	@Override
	public void itemRemoved(IPersist item)
	{
		uuidToPersist.remove(item.getUUID().toString());
		synchronized (idToPersist)
		{
			idToPersist.remove(item.getID());
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
		// just update the persist by the same uuid in the cache, uuid or id should not change.
		uuidToPersist.put(item.getUUID().toString(), item);
		synchronized (idToPersist)
		{
			idToPersist.put(item.getID(), item);
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

	/**
	 * @param solution
	 */
	void setCopySolution(Solution solution)
	{
		solutions.add(solution);
		// should always be null... should only be called once
		if (copyPersistListener == null) copyPersistListener = new CopySolutionPersistListener();
		solution.getChangeHandler().addIPersistListener(copyPersistListener);
	}

	private class CopySolutionPersistListener implements IItemChangeListener<IPersist>
	{

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

		@Override
		public void itemChanged(IPersist item)
		{
			PersistIndex.this.itemChanged(item);
		}

		@Override
		public void itemChanged(Collection<IPersist> items)
		{
			PersistIndex.this.itemChanged(items);
		}
	}
}
