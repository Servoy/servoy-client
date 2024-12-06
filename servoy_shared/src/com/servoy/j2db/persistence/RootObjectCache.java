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


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.PersistIndexCache;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;

/**
 * @author sebster
 */
public class RootObjectCache
{

	private static final Logger log = LoggerFactory.getLogger(RootObjectCache.class.getCanonicalName());

	private final AbstractRepository repository;
	private final HashMap<Integer, CacheRecord> rootObjectsById;
	private final HashMap rootObjectsByName;
	private final Map<Pair<String, Integer>, Long> loadingBlocker = new HashMap<>(); // map of pair<rootObjectThatIsBeingLoaded, rootObjectType> -> id_of_thread_doing_the_actual_loading; the pair needs to contain information that makes a root object unique enough to not have getRootObject() called twice recursively (for example solution with name "A" while it loads might want to get style with name "A" used by a form and hang the thread if we don't identify rootObjects being loaded based on object types as well in the key of this map); (we ignore here root object versions - as I don't think they can mingle while loading root objects)

	class RootObjectKey
	{
		String name;
		int objectTypeId;

		public RootObjectKey(String name, int objectTypeId)
		{
			this.name = name;
			this.objectTypeId = objectTypeId;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof RootObjectKey)
			{
				final RootObjectKey otherKey = (RootObjectKey)other;
				return name.equals(otherKey.name) && objectTypeId == otherKey.objectTypeId;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			return name.hashCode() * 29 + objectTypeId;
		}
	}

	class CacheRecord
	{
		RootObjectMetaData rootObjectMetaData;
		HashMap<Integer, IRootObject> rootObjects;

		@Override
		public String toString()
		{
			return RepositoryHelper.getObjectTypeName(rootObjectMetaData.getObjectTypeId()) + ": " + rootObjectMetaData.toString() + " count: " + //$NON-NLS-1$
				rootObjects.size();
		}
	}

	RootObjectCache(AbstractRepository repository, Collection<RootObjectMetaData> metaDatas)
	{
		this.repository = repository;
		rootObjectsById = new HashMap<Integer, CacheRecord>();
		rootObjectsByName = new HashMap<>();

		// Initialize the cache.
		Iterator<RootObjectMetaData> iterator = metaDatas.iterator();
		while (iterator.hasNext())
		{
			add(iterator.next(), false);
		}
	}

	void add(RootObjectMetaData metaData, boolean allowUpdate)
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			Integer intId = new Integer(metaData.getRootObjectId());

			CacheRecord cacheRecord = allowUpdate ? rootObjectsById.get(intId) : null;
			if (cacheRecord == null)
			{
				cacheRecord = new CacheRecord();
				cacheRecord.rootObjects = new HashMap<Integer, IRootObject>();
				rootObjectsById.put(intId, cacheRecord);
				rootObjectsByName.put(new RootObjectKey(metaData.getName(), metaData.getObjectTypeId()), cacheRecord);
			}
			else
			{
				for (IRootObject ro : cacheRecord.rootObjects.values())
				{
					((AbstractRootObject)ro).setMetaData(metaData);
				}
			}

			cacheRecord.rootObjectMetaData = metaData;
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	public void cacheRootObject(IRootObject rootObject) throws RepositoryException
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			CacheRecord cacheRecord = getCacheRecordEnsureNotNull(rootObject.getID());
			Integer release = new Integer(rootObject.getReleaseNumber());
			if (cacheRecord.rootObjects.get(release) != null)
			{
				// Root object is already cached, this is a bug.
				throw new RepositoryException(
					"duplicate cache of " + RepositoryHelper.getObjectTypeName(rootObject.getTypeID()) + " with name '" + rootObject.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			cacheRecord.rootObjects.put(release, rootObject);
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	public void renameRootObject(int rootObjectId, String name) throws RepositoryException
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			RootObjectMetaData metaData = getRootObjectMetaData(rootObjectId);
			RootObjectKey newKey = new RootObjectKey(name, metaData.getObjectTypeId());
			if (rootObjectsByName.containsKey(newKey))
			{
				throw new RepositoryException("root object with name='" + name + "' and type=" + metaData.getObjectTypeId() + " already exists");
			}
			RootObjectKey oldKey = new RootObjectKey(metaData.getName(), metaData.getObjectTypeId());
			Object cacheRecord = rootObjectsByName.remove(oldKey);
			metaData.setName(name);
			rootObjectsByName.put(newKey, cacheRecord);
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	public IRootObject getActiveRootObject(int rootObjectId) throws RepositoryException
	{
		return getRootObject(rootObjectId, 0);
	}

	public IRootObject getActiveRootObject(String name, int objectTypeId) throws RepositoryException
	{
		RootObjectMetaData rod = getRootObjectMetaData(name, objectTypeId);
		if (rod != null)
		{
			return getActiveRootObject(rod.getRootObjectId());
		}
		Debug.warn("Active root object '" + name + "' , type " + objectTypeId + " not found");
		return null;
	}

	public IRootObject getRootObject(String name, int objectTypeId, int release) throws RepositoryException
	{
		RootObjectMetaData rod = getRootObjectMetaData(name, objectTypeId);
		if (rod != null)
		{
			return getRootObject(rod.getRootObjectId(), release);
		}
		Debug.warn("Active root object '" + name + "' , type " + objectTypeId + ", release " + release + " not found");
		return null;
	}

	public boolean isRootObjectCached(String name, int objectTypeId, int release) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			RootObjectMetaData rod = getRootObjectMetaData(name, objectTypeId);
			if (rod != null)
			{
				CacheRecord cacheRecord = getCacheRecord(rod.getRootObjectId());
				if (cacheRecord != null)
				{
					if (release == 0)
					{
						release = cacheRecord.rootObjectMetaData.getActiveRelease();
					}
					else if (release == -1)
					{
						release = cacheRecord.rootObjectMetaData.getLatestRelease();
					}
					Integer key = new Integer(release);
					IRootObject rootObject = cacheRecord.rootObjects.get(key);
					return rootObject != null;
				}
			}
			return false;
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	IRootObject getRootObject(final int rootObjectId, final int release) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			CacheRecord cacheRecord = getCacheRecord(rootObjectId);
			if (cacheRecord == null)
			{
				return null;
			}
			int realRelease = release;
			if (realRelease == 0)
			{
				realRelease = cacheRecord.rootObjectMetaData.getActiveRelease();
			}
			else if (realRelease == -1)
			{
				realRelease = cacheRecord.rootObjectMetaData.getLatestRelease();
			}
			Integer key = new Integer(realRelease);
			IRootObject rootObject = cacheRecord.rootObjects.get(key);
			if (rootObject == null)
			{
				// don't fully lock on loading, only lock for this root object of this type
				AbstractRepository.unlock();
				if (log.isDebugEnabled())
					log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
						"] rootObj not in cache: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						cacheRecord.rootObjectMetaData.getObjectTypeId());
				try
				{
					// block loading for the same root object.
					Long idOfThreadThatWasLoadingIt;
					Pair<String, Integer> semiUniqueRootObjKey = new Pair<>(cacheRecord.rootObjectMetaData.getName(),
						Integer.valueOf(cacheRecord.rootObjectMetaData.getObjectTypeId()));
					synchronized (loadingBlocker)
					{
						idOfThreadThatWasLoadingIt = loadingBlocker.putIfAbsent(semiUniqueRootObjKey,
							Long.valueOf(Thread.currentThread().getId())); // this has to be inside the synchronized (loadingBlocker) block to avoid calling .wait() below if the notifyAll() gets called by another thread before we call .wait() here but after the putIfAbsent - the sync block prevents that
						if (idOfThreadThatWasLoadingIt != null)
						{
							if (idOfThreadThatWasLoadingIt.longValue() == Thread.currentThread().getId()) // this can cause a hang where 1 thread is waiting for itself to complete a rootObj load; should never happen
								throw new RuntimeException("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
									"] unexpected recursive getRootObject(...) call where the same thread is trying to load same (or similar) rootObj twice: " + //$NON-NLS-1$
									cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + cacheRecord.rootObjectMetaData.getObjectTypeId()); //$NON-NLS-1$ //$NON-NLS-2$

							try
							{
								if (log.isDebugEnabled()) log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
									"] waiting for other thread to finish loading: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
									cacheRecord.rootObjectMetaData.getObjectTypeId());
								loadingBlocker.wait(); // we don't have to do this in a while (to avoid spurious thread wakes - see javadoc or wakes due to other rootObjs/versions finishing load) because we call recursively getRootObject(...) below which is kind a a while loop - it rechecks wait conditions
							}
							catch (InterruptedException e)
							{
							}
						}
					}

					if (idOfThreadThatWasLoadingIt != null)
					{
						if (log.isDebugEnabled()) log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
							"] rechecking rootObj after wait: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							cacheRecord.rootObjectMetaData.getObjectTypeId());
						return getRootObject(rootObjectId, release); // we waited for the rootObj with this name (and some version) to load above; now check again if it's there and get it (or wait again if another thread loads already some version of this rootObj)
					}
					else
					{
						try
						{
							rootObject = cacheRecord.rootObjects.get(key); // check again to see if another thread already loaded this rootObj/version; avoid loading it multiple times (it can happen theoretically that two threads get to the line after AbstractRepository.unlock(); above trying to load the same rootObj; then one of the threads could load it and finish loading it before the second thread resumes execution; but two threads will never be at this line for the same rootObj at the same time due to the loadingBlocker locking per rootObj; so a simple check here should be safe I think)
							if (rootObject == null)
							{
								if (log.isDebugEnabled()) log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
									"] loading solution: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
									cacheRecord.rootObjectMetaData.getObjectTypeId());

								// do the actual load
								rootObject = repository.loadRootObject(cacheRecord.rootObjectMetaData, realRelease);
								if (rootObject != null)
								{
									AbstractRepository.lock(); // lock as we are modifying the cache
									try
									{
										if (log.isDebugEnabled()) log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
											"] adding sol to cache: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
											cacheRecord.rootObjectMetaData.getObjectTypeId());
										cacheRecord.rootObjects.put(key, rootObject);
									}
									finally
									{
										AbstractRepository.unlock();
									}
								}
							}
							else
							{
								if (log.isDebugEnabled()) log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
									"] solution was already loaded by another thread: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + //$NON-NLS-1$//$NON-NLS-2$
									" / " + cacheRecord.rootObjectMetaData.getObjectTypeId()); //$NON-NLS-1$
							}
						}
						catch (RepositoryException e)
						{
							Debug.error("Can't load root object of " + cacheRecord, e);
							throw e;
						}
						finally
						{
							synchronized (loadingBlocker)
							{
								if (log.isDebugEnabled()) log.debug("[RootObjectCache][getRootObject][" + Thread.currentThread().getId() + //$NON-NLS-1$
									"] clearing loading sol. flag and notifying: " + cacheRecord.rootObjectMetaData.getName() + " / " + realRelease + " / " + //$NON-NLS-1$//$NON-NLS-2$
									cacheRecord.rootObjectMetaData.getObjectTypeId());
								loadingBlocker.remove(semiUniqueRootObjKey);
								loadingBlocker.notifyAll();
							}
						}
					}
				}
				finally
				{
					AbstractRepository.lock();
				}
			}
			return rootObject;
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

//	synchronized void setActiveRelease(int rootObjectId, int activeRelease) throws RepositoryException
//	{
//		loadCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.setActiveRelease(activeRelease);
//	}
//
//	synchronized void setLatestRelease(int rootObjectId, int latestRelease) throws RepositoryException
//	{
//		loadCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.setLatestRelease(latestRelease);
//	}

	public int getLatestRelease(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			return getCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.getLatestRelease();
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	public int getActiveRelease(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			return getCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.getActiveRelease();
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	public RootObjectMetaData getRootObjectMetaData(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			CacheRecord cacheRecord = getCacheRecord(rootObjectId);
			return cacheRecord != null ? cacheRecord.rootObjectMetaData : null;
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			CacheRecord cacheRecord = getCacheRecord(name, objectTypeId);
			return cacheRecord != null ? cacheRecord.rootObjectMetaData : null;
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	RootObjectMetaData[] getRootObjectMetaDatas() throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			RootObjectMetaData[] metaDatas = new RootObjectMetaData[rootObjectsById.size()];
			Iterator<CacheRecord> iterator = rootObjectsById.values().iterator();
			int i = 0;
			while (iterator.hasNext())
			{
				metaDatas[i++] = iterator.next().rootObjectMetaData;
			}
			return metaDatas;
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId) throws RepositoryException
	{
		AbstractRepository.lock();
		try
		{
			List list = new SortedList(NameComparator.INSTANCE);
			Iterator<CacheRecord> iterator = rootObjectsById.values().iterator();
			while (iterator.hasNext())
			{
				RootObjectMetaData metaData = iterator.next().rootObjectMetaData;
				if (metaData.getObjectTypeId() == objectTypeId)
				{
					list.add(metaData);
				}
			}
			RootObjectMetaData[] filtered = new RootObjectMetaData[list.size()];
			return (RootObjectMetaData[])list.toArray(filtered);
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	void removeRootObject(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			Integer key = new Integer(rootObjectId);
			flushRootObject(rootObjectId);
			CacheRecord cacheRecord = rootObjectsById.get(key);
			if (cacheRecord != null)
			{
				rootObjectsById.remove(key);
				rootObjectsByName.remove(new RootObjectKey(cacheRecord.rootObjectMetaData.getName(), cacheRecord.rootObjectMetaData.getObjectTypeId()));
			}
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	void flushRootObject(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			Integer key = new Integer(rootObjectId);
			CacheRecord cacheRecord = rootObjectsById.get(key);
			if (cacheRecord != null)
			{
				flushRootObjectMap(cacheRecord.rootObjects);
			}
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	void flushRootObjectRelease(int rootObjectId, int release)
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			CacheRecord cacheRecord = rootObjectsById.get(new Integer(rootObjectId));
			if (cacheRecord != null)
			{
				if (release == 0)
				{
					release = cacheRecord.rootObjectMetaData.getActiveRelease();
				}
				else if (release == -1)
				{
					release = cacheRecord.rootObjectMetaData.getLatestRelease();
				}
				Integer key = new Integer(release);
				IRootObject rootObject = cacheRecord.rootObjects.get(key);
				if (rootObject != null)
				{
					rootObject.getChangeHandler().rootObjectIsFlushed();
					cacheRecord.rootObjects.remove(key);
				}
			}
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	void flush() throws RepositoryException
	{
		AbstractRepository.lock();
		PersistIndexCache.flush();
		try
		{
			Iterator<CacheRecord> iterator = rootObjectsById.values().iterator();
			while (iterator.hasNext())
			{
				CacheRecord cacheRecord = iterator.next();
				flushRootObjectMap(cacheRecord.rootObjects);
			}
		}
		finally
		{
			AbstractRepository.unlock();
		}
	}

	private void flushRootObjectMap(HashMap<Integer, IRootObject> rootObjectMap) throws RepositoryException
	{
		Iterator<IRootObject> iterator = rootObjectMap.values().iterator();
		while (iterator.hasNext())
		{
			IRootObject rootObject = iterator.next();
			ChangeHandler ch = rootObject.getChangeHandler();
			if (ch != null)
			{
				ch.rootObjectIsFlushed();
			}
			iterator.remove();
		}
	}

	private CacheRecord getCacheRecord(int rootObjectId) throws RepositoryException
	{
		Integer key = new Integer(rootObjectId);
		return rootObjectsById.get(key);

	}

	private CacheRecord getCacheRecordEnsureNotNull(int rootObjectId) throws RepositoryException
	{
		CacheRecord cacheRecord = getCacheRecord(rootObjectId);
		if (cacheRecord == null)
		{
			throw new RepositoryException("root object with id " + rootObjectId + " does not exist in the repository");
		}
		return cacheRecord;
	}

	private CacheRecord getCacheRecord(String name, int objectTypeId) throws RepositoryException
	{
		if (name == null) return null;
		RootObjectKey key = new RootObjectKey(name, objectTypeId);
		return (CacheRecord)rootObjectsByName.get(key);
	}
}
