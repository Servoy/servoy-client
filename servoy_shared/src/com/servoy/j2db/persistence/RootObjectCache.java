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

import com.servoy.j2db.util.SortedList;

/**
 * @author sebster
 */
public class RootObjectCache
{
	private final AbstractRepository repository;
	private final HashMap<Integer, CacheRecord> rootObjectsById;
	private final HashMap rootObjectsByName;

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

	RootObjectCache(AbstractRepository repository, Collection metaDatas)
	{
		this.repository = repository;
		rootObjectsById = new HashMap<Integer, CacheRecord>();
		rootObjectsByName = new HashMap();

		// Initialize the cache.
		Iterator iterator = metaDatas.iterator();
		while (iterator.hasNext())
		{
			add((RootObjectMetaData)iterator.next(), false);
		}
	}

	void add(RootObjectMetaData metaData, boolean allowUpdate)
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}


	public void cacheRootObject(IRootObject rootObject) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	public void renameRootObject(int rootObjectId, String name) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
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
		return null;
	}

	public IRootObject getRootObject(String name, int objectTypeId, int release) throws RepositoryException
	{
		RootObjectMetaData rod = getRootObjectMetaData(name, objectTypeId);
		if (rod != null)
		{
			return getRootObject(rod.getRootObjectId(), release);
		}
		return null;
	}

	public boolean isRootObjectCached(String name, int objectTypeId, int release) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	IRootObject getRootObject(int rootObjectId, int release) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
		try
		{
			CacheRecord cacheRecord = getCacheRecord(rootObjectId);
			if (cacheRecord == null)
			{
				return null;
			}
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
			if (rootObject == null)
			{
				rootObject = repository.loadRootObject(cacheRecord.rootObjectMetaData, release);
				cacheRecord.rootObjects.put(key, rootObject);
			}
			return rootObject;
		}
		finally
		{
			AbstractRepository.REPOSITORY_LOCK.unlock();
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
		AbstractRepository.REPOSITORY_LOCK.lock();
		try
		{
			return getCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.getLatestRelease();
		}
		finally
		{
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	public int getActiveRelease(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
		try
		{
			return getCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.getActiveRelease();
		}
		finally
		{
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	public RootObjectMetaData getRootObjectMetaData(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
		try
		{
			CacheRecord cacheRecord = getCacheRecord(rootObjectId);
			return cacheRecord != null ? cacheRecord.rootObjectMetaData : null;
		}
		finally
		{
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
		try
		{
			CacheRecord cacheRecord = getCacheRecord(name, objectTypeId);
			return cacheRecord != null ? cacheRecord.rootObjectMetaData : null;
		}
		finally
		{
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	RootObjectMetaData[] getRootObjectMetaDatas() throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	void removeRootObject(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	void flushRootObject(int rootObjectId) throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	void flushRootObjectRelease(int rootObjectId, int release)
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
		}
	}

	void flush() throws RepositoryException
	{
		AbstractRepository.REPOSITORY_LOCK.lock();
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
			AbstractRepository.REPOSITORY_LOCK.unlock();
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
