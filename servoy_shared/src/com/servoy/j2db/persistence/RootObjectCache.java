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
	private final HashMap rootObjectsById;
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
		HashMap rootObjects;

		@Override
		public String toString()
		{
			return RepositoryHelper.getObjectTypeName(rootObjectMetaData.getObjectTypeId()) +
				": " + rootObjectMetaData.toString() + " count: " + rootObjects.size(); //$NON-NLS-1$
		}
	}

	RootObjectCache(AbstractRepository repository, Collection metaDatas)
	{
		this.repository = repository;
		rootObjectsById = new HashMap();
		rootObjectsByName = new HashMap();

		// Initialize the cache.
		Iterator iterator = metaDatas.iterator();
		while (iterator.hasNext())
		{
			add((RootObjectMetaData)iterator.next());
		}
	}


	/**
	 * @param metadata
	 */
	synchronized void add(RootObjectMetaData metaData)
	{
		CacheRecord cacheRecord = new CacheRecord();
		cacheRecord.rootObjectMetaData = metaData;
		cacheRecord.rootObjects = new HashMap();
		rootObjectsById.put(new Integer(metaData.getRootObjectId()), cacheRecord);
		rootObjectsByName.put(new RootObjectKey(metaData.getName(), metaData.getObjectTypeId()), cacheRecord);
	}


	/**
	 * Cache a new root object. THE ROOT OBJECT MUST CONTAIN VALID META DATA AND THE META DATA SHOULD HAVE ALREADY BEEN SAVED!
	 * 
	 * @param rootObject the new root object to add to the cache
	 * @throws RepositoryException if the root object is already cached or does not have release 1
	 */
	private synchronized void cacheNewRootObject(IRootObject rootObject) throws RepositoryException
	{
		Integer key = new Integer(rootObject.getID());
		if (rootObjectsById.containsKey(key))
		{
			throw new RepositoryException("cannot cache new root object with id " + rootObjectsById + " because it already exists");
		}
		CacheRecord cacheRecord = new CacheRecord();
		cacheRecord.rootObjectMetaData = rootObject.getRootObjectMetaData();
		if (rootObject.getReleaseNumber() != 1)
		{
			throw new RepositoryException("cannot cache new root object with release " + rootObject.getReleaseNumber());
		}
		cacheRecord.rootObjects = new HashMap();
		cacheRecord.rootObjects.put(new Integer(1), rootObject);
		rootObjectsById.put(key, cacheRecord);
		rootObjectsByName.put(new RootObjectKey(rootObject.getName(), rootObject.getTypeID()), cacheRecord);
	}

	public synchronized void cacheRootObject(IRootObject rootObject) throws RepositoryException
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

	public synchronized void renameRootObject(int rootObjectId, String name) throws RepositoryException
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

	synchronized IRootObject getRootObject(int rootObjectId, int release) throws RepositoryException
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
		IRootObject rootObject = (IRootObject)cacheRecord.rootObjects.get(key);
		if (rootObject == null)
		{
			rootObject = repository.loadRootObject(cacheRecord.rootObjectMetaData, release);
			cacheRecord.rootObjects.put(key, rootObject);
		}
		return rootObject;
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

	public synchronized int getLatestRelease(int rootObjectId) throws RepositoryException
	{
		return getCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.getLatestRelease();
	}

	public synchronized int getActiveRelease(int rootObjectId) throws RepositoryException
	{
		return getCacheRecordEnsureNotNull(rootObjectId).rootObjectMetaData.getActiveRelease();
	}

	public synchronized RootObjectMetaData getRootObjectMetaData(int rootObjectId) throws RepositoryException
	{
		CacheRecord cacheRecord = getCacheRecord(rootObjectId);
		return cacheRecord != null ? cacheRecord.rootObjectMetaData : null;
	}

	synchronized RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId) throws RepositoryException
	{
		CacheRecord cacheRecord = getCacheRecord(name, objectTypeId);
		return cacheRecord != null ? cacheRecord.rootObjectMetaData : null;
	}

	synchronized RootObjectMetaData[] getRootObjectMetaDatas() throws RepositoryException
	{
		RootObjectMetaData[] metaDatas = new RootObjectMetaData[rootObjectsById.size()];
		Iterator iterator = rootObjectsById.values().iterator();
		int i = 0;
		while (iterator.hasNext())
		{
			metaDatas[i++] = ((CacheRecord)iterator.next()).rootObjectMetaData;
		}
		return metaDatas;
	}

	synchronized RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId) throws RepositoryException
	{
		List list = new SortedList(NameComparator.INSTANCE);
		Iterator iterator = rootObjectsById.values().iterator();
		while (iterator.hasNext())
		{
			RootObjectMetaData metaData = ((CacheRecord)iterator.next()).rootObjectMetaData;
			if (metaData.getObjectTypeId() == objectTypeId)
			{
				list.add(metaData);
			}
		}
		RootObjectMetaData[] filtered = new RootObjectMetaData[list.size()];
		return (RootObjectMetaData[])list.toArray(filtered);
	}

	synchronized void removeRootObject(int rootObjectId) throws RepositoryException
	{
		Integer key = new Integer(rootObjectId);
		flushRootObject(rootObjectId);
		CacheRecord cacheRecord = (CacheRecord)rootObjectsById.get(key);
		if (cacheRecord != null)
		{
			rootObjectsById.remove(key);
			rootObjectsByName.remove(new RootObjectKey(cacheRecord.rootObjectMetaData.getName(), cacheRecord.rootObjectMetaData.getObjectTypeId()));
		}
	}

	synchronized void flushRootObject(int rootObjectId) throws RepositoryException
	{
		Integer key = new Integer(rootObjectId);
		CacheRecord cacheRecord = (CacheRecord)rootObjectsById.get(key);
		if (cacheRecord != null)
		{
			flushRootObjectMap(cacheRecord.rootObjects);
		}
	}

	synchronized void flushRootObjectRelease(int rootObjectId, int release) throws RepositoryException
	{
		CacheRecord cacheRecord = (CacheRecord)rootObjectsById.get(new Integer(rootObjectId));
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
			IRootObject rootObject = (IRootObject)cacheRecord.rootObjects.get(key);
			if (rootObject != null)
			{
				rootObject.getChangeHandler().rootObjectIsFlushed();
				cacheRecord.rootObjects.remove(key);
			}
		}
	}

	synchronized void flush() throws RepositoryException
	{
		Iterator iterator = rootObjectsById.values().iterator();
		while (iterator.hasNext())
		{
			CacheRecord cacheRecord = (CacheRecord)iterator.next();
			flushRootObjectMap(cacheRecord.rootObjects);
		}
	}

	private void flushRootObjectMap(HashMap rootObjectMap) throws RepositoryException
	{
		Iterator iterator = rootObjectMap.values().iterator();
		while (iterator.hasNext())
		{
			IRootObject rootObject = (IRootObject)iterator.next();
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
		return (CacheRecord)rootObjectsById.get(key);

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
