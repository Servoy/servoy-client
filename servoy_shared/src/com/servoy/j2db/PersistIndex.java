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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.LineNumberComparator;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.Debug;

/**
 * This Persist Index class is a ItemChangeListener for the persist by itself.
 * But it will not add listeners to the solutions. Subclasses need todo this, because this class is by default immutable.
 *
 * @author jcompagner
 * @since 8.4
 */
public class PersistIndex implements IItemChangeListener<IPersist>, IPersistIndex
{
	protected enum EventType
	{
		CREATED, UPDATED, REMOVED
	}

	protected final ConcurrentMap<String, IPersist> uuidToPersist = new ConcurrentHashMap<>(128);
	// caches per persist class a map of Name->Persist (we assume that is unique!)
	protected final ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> nameToPersist = new ConcurrentHashMap<>();

	protected final ConcurrentMap<String, ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>>> datasourceToPersist = new ConcurrentHashMap<>();

	protected final ConcurrentMap<String, Map<String, IScriptElement>> scopeCacheByName = new ConcurrentHashMap<>();

	protected final CopyOnWriteArrayList<Solution> solutions = new CopyOnWriteArrayList<>();

	protected PersistIndex()
	{
	}

	public PersistIndex(List<Solution> solutions)
	{
		this.solutions.addAll(solutions);
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
		ConcurrentMap<String, IPersist> classToList = initNameCache(persistClass);
		T persist = (T)classToList.get(name);
		return persist;
	}

	protected ConcurrentMap<String, IPersist> initNameCache(Class< ? extends IPersist> persistClass)
	{
		ConcurrentMap<String, IPersist> classToList = nameToPersist.get(persistClass);
		if (classToList == null)
		{
			// make the name cache always lower case hits, we don't allow different casing for most if not all stuff.
			final ConcurrentMap<String, IPersist> tmp = new ConcurrentHashMap<String, IPersist>(128)
			{
				@Override
				public IPersist get(Object key)
				{
					return super.get(key instanceof String ? ((String)key).toLowerCase(Locale.ENGLISH) : key);
				}

				@Override
				public boolean containsKey(Object key)
				{
					return super.containsKey(key instanceof String ? ((String)key).toLowerCase(Locale.ENGLISH) : key);
				}

				@Override
				public IPersist put(String key, IPersist value)
				{
					return super.put(key.toLowerCase(Locale.ENGLISH), value);
				}

				@Override
				public IPersist putIfAbsent(String key, IPersist value)
				{
					return super.putIfAbsent(key.toLowerCase(Locale.ENGLISH), value);
				}

				@Override
				public void putAll(Map< ? extends String, ? extends IPersist> map)
				{
					map.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
				}
			};
			if (ISupportName.class.isAssignableFrom(persistClass))
			{
				visit((persist) -> {
					if (persist.getClass() == persistClass)
					{
						addInNameCache(tmp, persist);
						return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				});
			}
			classToList = tmp;
			nameToPersist.put(persistClass, classToList);
		}
		return classToList;
	}

	protected void addInNameCache(ConcurrentMap<String, IPersist> cache, IPersist persist)
	{
		String name = ((ISupportName)persist).getName();
		if (name != null) cache.put(name, persist);
		else Debug.warn("Trying to create the name cache of a Persist object that we expect to have a name but has none: " + persist); //$NON-NLS-1$
	}

	@SuppressWarnings("unchecked")
	public <T extends IPersist> Iterator<T> getPersistByDatasource(String datasource, Class<T> persistClass)
	{
		return (Iterator<T>)getDatasourceMap(datasource, persistClass).values().iterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IPersist> T getPersistByDatasource(String datasource, Class<T> persistClass, String name)
	{
		return (T)getDatasourceMap(datasource, persistClass).get(name);
	}

	private Map<String, ? extends IPersist> getDatasourceMap(String datasource, Class< ? extends IPersist> persistClass)
	{
		initDatasourceCache(datasource);
		ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> dsMap = datasource != null ? datasourceToPersist.get(datasource) : null;
		ConcurrentMap<String, ? extends IPersist> persistMap = null;
		if (dsMap != null)
		{
			persistMap = dsMap.get(persistClass);
		}
		if (persistMap == null)
		{
			return Collections.emptyMap();
		}
		return persistMap;
	}

	protected void initDatasourceCache(String datasource)
	{
		if ((datasourceToPersist.size() == 0) || (datasource != null && datasourceToPersist.get(datasource) == null))
		{
			visit((persist) -> {
				if (persist instanceof TableNode)
				{
					String tableDs = ((TableNode)persist).getDataSource();
					if (tableDs != null)
					{
						ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> dsMap = datasourceToPersist
							.get(tableDs);
						if (dsMap == null)
						{
							dsMap = new ConcurrentHashMap<>(4);
							dsMap.put(ScriptCalculation.class, new ConcurrentHashMap<String, IPersist>(4));
							dsMap.put(TableNode.class, new ConcurrentHashMap<String, IPersist>(4));
							dsMap.put(AggregateVariable.class, new ConcurrentHashMap<String, IPersist>(4));
							dsMap.put(ScriptMethod.class, new ConcurrentHashMap<String, IPersist>(4));
							datasourceToPersist.put(tableDs, dsMap);
						}
						ConcurrentMap<String, IPersist> tableNodeCache = dsMap.get(TableNode.class);
						Solution solution = (Solution)persist.getAncestor(IRepository.SOLUTIONS);
						tableNodeCache.put(solution.getName(), persist);
						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				}
				else
				{
					ISupportChilds parent = persist.getParent();
					if (persist instanceof ScriptCalculation)
					{
						if (parent instanceof TableNode)
						{
							ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> dsMap = datasourceToPersist
								.get(((TableNode)parent).getDataSource());
							addInDatasourceCache(dsMap.get(ScriptCalculation.class), persist, datasource);
						}
						else
						{
							Debug.error("Something wrong with ScriptCalculation " + ((ScriptCalculation)persist).getName() +
								" should  have table as parent but the parent is: " + parent);
						}
					}
					else if (persist instanceof AggregateVariable)
					{
						if (parent instanceof TableNode)
						{
							ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> dsMap = datasourceToPersist
								.get(((TableNode)parent).getDataSource());
							addInDatasourceCache(dsMap.get(AggregateVariable.class), persist, datasource);
						}
						else
						{
							Debug.error("Something wrong with AggregateVariable " + ((ScriptCalculation)persist).getName() +
								" should  have table as parent but the parent is: " + parent);
						}
					}
					else if (persist instanceof ScriptMethod && parent instanceof TableNode)
					{
						ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> dsMap = datasourceToPersist
							.get(((TableNode)parent).getDataSource());
						addInDatasourceCache(dsMap.get(ScriptMethod.class), persist, datasource);
					}
				}
				return persist instanceof Solution ? IPersistVisitor.CONTINUE_TRAVERSAL : IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			});
			if (datasource != null && datasourceToPersist.get(datasource) == null)
			{
				ConcurrentMap<Class< ? extends IPersist>, ConcurrentMap<String, IPersist>> dsMap = new ConcurrentHashMap<>(4);
				dsMap.put(ScriptCalculation.class, new ConcurrentHashMap<String, IPersist>(4));
				dsMap.put(TableNode.class, new ConcurrentHashMap<String, IPersist>(4));
				dsMap.put(AggregateVariable.class, new ConcurrentHashMap<String, IPersist>(4));
				dsMap.put(ScriptMethod.class, new ConcurrentHashMap<String, IPersist>(4));
				datasourceToPersist.put(datasource, dsMap);
			}
		}
	}

	protected void addInDatasourceCache(ConcurrentMap<String, IPersist> cache, IPersist persist, String datasource)
	{
		String name = ((ISupportName)persist).getName();
		if (name != null)
		{
			cache.put(name, persist);
		}
	}

	@Override
	public IScriptElement getSupportScope(String scopeName, String baseName)
	{
		Map<String, IScriptElement> map = getGlobalScopeCache().get(scopeName);
		return map != null ? map.get(baseName) : null;
	}

	@Override
	public <T extends IScriptElement> Iterator<T> getGlobalScriptObjects(String scopeName, boolean sort, Class<T> cls)
	{
		Stream<IScriptElement> stream = null;
		Map<String, Map<String, IScriptElement>> globalMap = getGlobalScopeCache();
		if (scopeName != null)
		{
			Map<String, IScriptElement> map = globalMap.get(scopeName);
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

	protected Map<String, Map<String, IScriptElement>> getGlobalScopeCache()
	{
		synchronized (scopeCacheByName)
		{
			if (scopeCacheByName.isEmpty())
			{
				visit((persist) -> {
					if (persist instanceof IScriptElement)
					{
						String scopeName = ((IScriptElement)persist).getScopeName();
						if (scopeName == null)
						{
							scopeName = ScriptVariable.GLOBAL_SCOPE;
						}
						Map<String, IScriptElement> scopeMap = scopeCacheByName.get(scopeName);
						if (scopeMap == null)
						{
							Map<String, IScriptElement> prev = scopeCacheByName.putIfAbsent(scopeName, scopeMap = new HashMap<String, IScriptElement>(128));
							if (prev != null) scopeMap = prev;
						}
						addInScopeCache(scopeMap, (IScriptElement)persist);
					}
					return persist instanceof Solution ? IPersistVisitor.CONTINUE_TRAVERSAL : IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				});
			}
		}
		return scopeCacheByName;
	}

	protected void addInScopeCache(Map<String, IScriptElement> cache, IScriptElement persist)
	{
		cache.put(persist.getName(), persist);
	}

	public final PersistIndex createIndex()
	{
		visit((persist) -> {
			putInCache(persist);
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		});
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T extends IPersist> Iterator<T> getIterableFor(Class<T> clz)
	{
		ConcurrentMap<String, IPersist> cacheByName = initNameCache(clz);
		if (cacheByName != null)
		{
			synchronized (cacheByName)
			{
				Collection<IPersist> collection = Collections.unmodifiableCollection(cacheByName.values());
				return (Iterator<T>)collection.iterator();
			}
		}
		return (Iterator<T>)Collections.emptyList().iterator();
	}

	/**
	 * @param persist
	 */
	protected void putInCache(IPersist persist)
	{
		uuidToPersist.put(persist.getUUID().toString(), persist);
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
		nameToPersist.clear();
		datasourceToPersist.clear();
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
		nameToPersist.clear();
		datasourceToPersist.clear();
		scopeCacheByName.clear();
	}

	@Override
	public void itemCreated(IPersist item)
	{
		if (uuidToPersist.isEmpty()) return;
		putInCache(item);
		testNameCache(item, EventType.CREATED);
		testDatasourceCache(item);
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
			cleanScopeCache();
		}
	}

	/**
	 * @param item
	 */
	protected void testNameCache(IPersist item, EventType type)
	{
		if (item instanceof ISupportName)
		{
			ConcurrentMap<String, IPersist> nameCache = nameToPersist.get(item.getClass());
			if (nameCache != null)
			{
				String name = ((ISupportName)item).getName();
				if (name != null)
				{
					switch (type)
					{
						case CREATED :
							nameCache.put(name, item);
							break;
						case REMOVED :
							if (nameCache.remove(name) != item) nameToPersist.remove(item.getClass());
							break;
						case UPDATED :
							if (nameCache.get(name) != item) nameToPersist.remove(item.getClass());
					}
				}
				else if (type != EventType.CREATED)
				{
					// if it is removed or updated and it didn't have a name just remove the class cache for a rebuild
					// could be a name change to null.
					nameToPersist.remove(item.getClass());
				}
			}
		}
	}


	/**
	 * @param item
	 * @param created
	 */
	protected void testDatasourceCache(IPersist item)
	{
		if (item instanceof TableNode || item.getParent() instanceof TableNode)
		{
			// just remove the whole datasource cache
			String ds = item instanceof TableNode ? ((TableNode)item).getDataSource() : ((TableNode)item.getParent()).getDataSource();
			if (ds != null) datasourceToPersist.remove(ds);
		}
	}


	@Override
	public void itemRemoved(IPersist item)
	{
		if (uuidToPersist.isEmpty()) return;
		uuidToPersist.remove(item.getUUID().toString());
		testNameCache(item, EventType.REMOVED);
		testDatasourceCache(item);
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
			cleanScopeCache();
		}
	}

	@Override
	public void itemChanged(IPersist item)
	{
		if (uuidToPersist.isEmpty()) return;
		// just update the persist by the same uuid in the cache, uuid or id should not change.
		uuidToPersist.put(item.getUUID().toString(), item);
		testNameCache(item, EventType.UPDATED);
		testDatasourceCache(item);
		if (item instanceof ISupportScope)
		{
			cleanScopeCache();
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

	protected void cleanScopeCache()
	{
		scopeCacheByName.clear();
	}
}
