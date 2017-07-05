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

import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * Abstract repository to be sub-classed, contains some generic functions
 * @author jblok
 */
public abstract class AbstractRepository extends AbstractPersistFactory implements IDeveloperRepository
{
	public static final int repository_version = 49;

	/**
	 * This repository is used by client
	 */
	protected final String lock_id = UUID.randomUUID().toString();

	protected RepositoryHelper repositoryHelper;

	private UUID repository_uuid = null;
	private final Map<String, String> mimeTypeMap;
	protected final IServerManager serverManager;

	protected AbstractRepository(IServerManager serverManager)
	{
		this.serverManager = serverManager;
		mimeTypeMap = new HashMap<String, String>();
		mimeTypeMap.put("gif", "image/gif"); //$NON-NLS-1$//$NON-NLS-2$
		mimeTypeMap.put("png", "image/png"); //$NON-NLS-1$//$NON-NLS-2$
		mimeTypeMap.put("jpg", "image/jpg"); //$NON-NLS-1$ //$NON-NLS-2$


		repositoryHelper = new RepositoryHelper(this);
	}

	public String getContentType(String filename)
	{
		if (filename == null) return null;

		int i = filename.lastIndexOf('.');
		if (i < 0) return null;

		return mimeTypeMap.get(filename.substring(i + 1).toLowerCase());
	}


	public void registerMimeTypes(Map<String, String> mimeTypes)
	{
		mimeTypeMap.putAll(mimeTypes);
	}

	public synchronized UUID getRepositoryUUID() throws RepositoryException
	{
		if (repository_uuid == null)
		{
			Properties props = getUserProperties(SYSTEM_USER_ID);
			String rep_uuid = props.getProperty(REPOSITORY_UUID_PROPERTY_NAME);
			if (rep_uuid != null)
			{
				repository_uuid = UUID.fromString(rep_uuid);
			}
			else
			{
				repository_uuid = UUID.randomUUID();
				props.put(REPOSITORY_UUID_PROPERTY_NAME, repository_uuid.toString());
				setUserProperties(SYSTEM_USER_ID, props);
			}
		}
		return repository_uuid;
	}

	public abstract Properties getUserProperties(int user_id) throws RepositoryException; //user 0 is SYSTEM

	public Map<String, Method> getGettersViaIntrospection(Object obj) throws IntrospectionException
	{
		return RepositoryHelper.getGettersViaIntrospection(obj);
	}

	public IPersist createNewPersistInSolution(Solution solution, UUID parentUUID, int typeID, int id, UUID uuid, Map<String, Object> values)
		throws RepositoryException
	{
		IPersist parent = searchPersist(solution, parentUUID);
		if (parent instanceof ISupportChilds)
		{
			IPersist persist = null;
			// first try to find the persist/child, if it is not already loaded. (tablenodes will do that)
			Iterator<IPersist> it = ((ISupportChilds)parent).getAllObjects();
			while (it.hasNext())
			{
				IPersist child = it.next();
				if (child.getUUID().equals(uuid))
				{
					persist = child;
					break;
				}
			}
			if (persist == null)
			{
				persist = solution.getChangeHandler().createNewObject((ISupportChilds)parent, typeID, id, uuid);
				((ISupportChilds)parent).addChild(persist);
			}
			updatePersistWithValueMap(persist, values, false);

			return persist;
		}
		return null;
	}

	/**
	 * Search the persist in the parent tree.
	 * <p>
	 * optimized using the assumption that the persist is in a tree of similar structure to the tree of parent.
	 *
	 * @param parent
	 * @param persist
	 * @throws RepositoryException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IPersist> T searchPersist(ISupportChilds parent, T persist)
	{
		if (persist == null || parent == null) return null;
		UUID parentUuid = parent.getUUID();
		if (parentUuid.equals(persist.getUUID())) return (T)parent;

		// find the path to the parent
		List<UUID> path = new ArrayList<UUID>();
		IPersist p = persist;
		while (p != null)
		{
			path.add(p.getUUID());
			if (p.getUUID().equals(parentUuid))
			{
				p = null;
			}
			else
			{
				p = p.getParent();
			}
		}

		if (path.size() > 0 && parentUuid.equals(path.get(path.size() - 1)))
		{
			// find persist via same path
			IPersist node = parent;
			for (int i = path.size() - 2; node != null && i >= 0; i--)
			{
				node = ((ISupportChilds)node).getChild(path.get(i));
			}
			if (node != null)
			{
				return (T)node;
			}
		}

		// not found via the path, check if the persist's (grand)parent is the parent, in that case the persist was deleted
		p = persist;
		while (p != null)
		{
			if (p == parent)
			{
				// persist was deleted, no need to do a complete search
				return null;
			}
			p = p.getParent();
		}

		// not found via the path and in different parent, fall back on complete search on uuid
		return (T)searchPersist(parent, persist.getUUID(), persist.getParent());
	}

	public static IPersist searchPersist(ISupportChilds node, final UUID uuid)
	{
		return searchPersist(node, uuid, null);
	}

	public static IPersist searchPersist(ISupportChilds node, final UUID uuid, final ISupportChilds searchParent)
	{
		if (node == null || uuid == null) return null;
		if (node.getUUID().equals(uuid)) return node;

		return (IPersist)node.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o instanceof ISupportChilds)
				{
					if (searchParent == null || searchParent.equals(o))
					{
						IPersist child = ((ISupportChilds)o).getChild(uuid);
						if (child != null)
						{
							return child;
						}
					}
					return CONTINUE_TRAVERSAL;
				}
				return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
			}
		});
	}

	/**
	 * Create a copy of a solution object without its children.
	 *
	 * @param solution
	 * @throws RepositoryException
	 */
	public Solution createSolutionCopy(Solution solution) throws RepositoryException
	{
		Solution solutionCopy = (Solution)createObject(null, solution.getTypeID(), solution.getID(), solution.getUUID());
		solutionCopy.setChangeHandler(new ChangeHandler(this));

		Map<String, Object> values = getPersistAsValueMap(solution);
		updatePersistWithValueMap(solutionCopy, values, false);
		return solutionCopy;
	}

	/**
	 * Copy a persist from another solution to the solution.
	 *
	 * @param persist
	 * @param dontSearch
	 * @param solution
	 * @throws RepositoryException
	 */
	public IPersist copyPersistIntoSolution(IPersist persist, ISupportChilds parent, boolean search) throws RepositoryException
	{
		IPersist destPersist = null;
		if (parent.getUUID().equals(persist.getUUID()))
		{
			destPersist = parent;
		}
		else if (search)
		{
			destPersist = searchPersist(parent, persist);
		}
		if (destPersist == null)
		{
			// object was added
			IPersist destParent = searchPersist(parent, persist.getParent());
			if (!(destParent instanceof ISupportChilds))
			{
				// this should never happen, don't know how to fix this one
				throw new RepositoryException("Could not update editing solution, please refresh the solution"); //$NON-NLS-1$
			}
			destPersist = createObject((ISupportChilds)destParent, persist.getTypeID(), persist.getID(), persist.getUUID());
			((ISupportChilds)destParent).addChild(destPersist);
		}

		if (persist instanceof AbstractScriptProvider)
		{
			((AbstractScriptProvider)destPersist).setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS,
				((AbstractScriptProvider)persist).getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS));
			((AbstractScriptProvider)destPersist).setRuntimeProperty(IScriptProvider.COMMENT,
				((AbstractScriptProvider)persist).getRuntimeProperty(IScriptProvider.COMMENT));
			((AbstractScriptProvider)destPersist).setSerializableRuntimeProperty(IScriptProvider.TYPE,
				((AbstractScriptProvider)persist).getSerializableRuntimeProperty(IScriptProvider.TYPE));
		}

		Map<String, Object> values = ((AbstractRepository)persist.getRootObject().getRepository()).getPersistAsValueMap(persist);
		updatePersistWithValueMap(destPersist, values, true);
		if (destPersist.isChanged())
		{
			destPersist.getRootObject().getChangeHandler().fireIPersistChanged(destPersist);
		}
		return destPersist;
	}


	/*
	 * ___________________________RootObjectCache methods
	 */

	private RootObjectCache rootObjectCache = null;
	private static Object ROOT_OBJECT_SYNC = new Object();

	public boolean isRootObjectCacheInitialized()
	{
		return rootObjectCache != null;
	}

	protected RootObjectCache getRootObjectCache() throws RepositoryException
	{
		try
		{
			synchronized (ROOT_OBJECT_SYNC)
			{
				if (rootObjectCache == null)
				{
					rootObjectCache = new RootObjectCache(this, loadRootObjectMetaDatas());
				}
			}
			return rootObjectCache;
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	public void flush()
	{
		// will force a reload of RootObjectMetaDatas as well (not just caches for existing RootObjectMetaDatas)
		synchronized (ROOT_OBJECT_SYNC)
		{
			rootObjectCache = null;
		}
	}

	public void flushAllCachedData() throws RepositoryException
	{
		getRootObjectCache().flush();
	}

	public void flushRootObject(int rootObjectId) throws RepositoryException
	{
		getRootObjectCache().flushRootObject(rootObjectId);
	}

	public void flushRootObjectRelease(int rootObjectId, int release) throws RepositoryException
	{
		getRootObjectCache().flushRootObjectRelease(rootObjectId, release);
	}

	public void removeRootObject(int rootObjectId) throws RepositoryException
	{
		// Delete solution from cache...
		getRootObjectCache().removeRootObject(rootObjectId);
	}

	public List<IRootObject> getActiveRootObjects(int objectTypeId) throws RepositoryException
	{
		try
		{
			RootObjectMetaData[] metadatas = getRootObjectMetaDatasForType(objectTypeId);
			List<IRootObject> rootObjects = new ArrayList<IRootObject>(metadatas.length);
			for (RootObjectMetaData element : metadatas)
			{
				rootObjects.add(getRootObject(element.getRootObjectId(), element.getActiveRelease()));
			}
			return rootObjects;
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	public SolutionMetaData[] getSolutionMetaDatas() throws RepositoryException
	{
		RootObjectMetaData[] metas = getRootObjectCache().getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
		SolutionMetaData[] result = new SolutionMetaData[metas.length];
		System.arraycopy(metas, 0, result, 0, metas.length);
		return result;
	}


	public RootObjectMetaData[] getRootObjectMetaDatas() throws RepositoryException
	{
		return getRootObjectCache().getRootObjectMetaDatas();
	}

	public RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId) throws RepositoryException
	{
		return getRootObjectCache().getRootObjectMetaDatasForType(objectTypeId);
	}

	public RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId) throws RepositoryException
	{
		return getRootObjectCache().getRootObjectMetaData(name, objectTypeId);
	}

	public RootObjectMetaData getRootObjectMetaData(UUID uuid) throws RepositoryException
	{
		return getRootObjectCache().getRootObjectMetaData(getElementIdForUUID(uuid));
	}

	public RootObjectMetaData getRootObjectMetaData(int rootObjectId) throws RepositoryException
	{
		return getRootObjectCache().getRootObjectMetaData(rootObjectId);
	}

	public IRootObject getRootObject(int rootObjectId, int release) throws RepositoryException
	{
		return getRootObjectCache().getRootObject(rootObjectId, release);
	}

	public IRootObject getRootObject(String name, int objectTypeId, int release) throws RepositoryException
	{
		return getRootObjectCache().getRootObject(name, objectTypeId, release);
	}

	public IRootObject getActiveRootObject(int rootObjectId) throws RepositoryException
	{
		return getRootObjectCache().getActiveRootObject(rootObjectId);
	}

	public IRootObject getActiveRootObject(String name, int objectTypeId) throws RepositoryException
	{
		return getRootObjectCache().getActiveRootObject(name, objectTypeId);
	}

	public IRootObject getLatestRootObject(int rootObjectId) throws RepositoryException
	{
		RootObjectMetaData metadata = getRootObjectCache().getRootObjectMetaData(rootObjectId);
		if (metadata == null)
		{
			try
			{
				Iterator<RootObjectMetaData> iterator = loadRootObjectMetaDatas().iterator();
				while (iterator.hasNext())
				{
					metadata = iterator.next();
					if (metadata.getRootObjectId() == rootObjectId)
					{
						getRootObjectCache().add(metadata, false);
						return getRootObject(rootObjectId, metadata.getLatestRelease());
					}
				}
			}
			catch (Exception e)
			{
				Debug.error("Error loading new MetaData for rootobjectid: " + rootObjectId, e); //$NON-NLS-1$
			}
			return null;
		}
		return getRootObject(rootObjectId, metadata.getLatestRelease());
	}

	protected abstract Collection<RootObjectMetaData> loadRootObjectMetaDatas() throws Exception;

	//Should only be called by rootobjectcache
	protected abstract IRootObject loadRootObject(RootObjectMetaData romd, int releaseNumber) throws RepositoryException;

	/*
	 * ___________________________Helper methods
	 */

	public Map<String, Object> getPersistAsValueMap(IPersist persist) throws RepositoryException
	{
		return ((AbstractBase)persist).getPropertiesMap();
	}

	public void updatePersistWithValueMap(IPersist persist, Map<String, Object> propertyValues, boolean overwrite) throws RepositoryException
	{
		((AbstractBase)persist).copyPropertiesMap(propertyValues, overwrite);
	}

	/**
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#initClone(com.servoy.j2db.persistence.IPersist, com.servoy.j2db.persistence.IPersist)
	 */
	@Override
	public void initClone(IPersist clone, IPersist objToClone, boolean flattenOverrides) throws RepositoryException
	{
		RepositoryHelper.initClone(clone, objToClone, flattenOverrides);
	}

	/**
	 * @throws RepositoryException
	 * @see com.servoy.j2db.persistence.AbstractPersistFactory#createRootObject(int)
	 */
	@Override
	protected IPersist createRootObject(int elementId) throws RepositoryException
	{
		RootObjectMetaData rootObjectMetaData = getRootObjectMetaData(elementId);
		return createRootObject(rootObjectMetaData);

	}

	public IRootObject createRootObject(RootObjectMetaData metaData) throws RepositoryException
	{
		AbstractRootObject rootObject = null;
		switch (metaData.getObjectTypeId())
		{
			case IRepository.SOLUTIONS :
				SolutionMetaData smd = (SolutionMetaData)metaData;
				rootObject = new Solution(this, smd);
				break;
			case IRepository.STYLES :
				rootObject = new Style(this, metaData);
				break;
			case IRepository.TEMPLATES :
				rootObject = new Template(this, metaData);
				break;
			default :
				throw new RepositoryException("Invalid root object type!"); //$NON-NLS-1$
		}

		// Set the root object properties.
		rootObject.setReleaseNumber(1);
		rootObject.setRevisionNumber(1);
		rootObject.setRepository(this);

		return rootObject;
	}

	/**
	 * Create a new root object
	 */
	public IRootObject createNewRootObject(String name, int objectTypeId) throws RepositoryException
	{
		UUID uuid = UUID.randomUUID();
		IRootObject rootObject = createNewRootObject(name, objectTypeId, getNewElementID(uuid), uuid);
		// Put the root object in the cache.
		return rootObject;
	}

	/**
	 * Remove persist from parent and register it and all children (recursive) as removed.
	 *
	 * @throws RepositoryException
	 */
	public void deleteObject(IPersist persist) throws RepositoryException
	{
		persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o.getRootObject() instanceof AbstractRootObject)
				{
					((AbstractRootObject)o.getRootObject()).registerRemovedObject(o);
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		persist.getParent().removeChild(persist);
	}

	/**
	 * Re-add previously removed persist to parent and unregister it and all its children (recursive) as new.
	 *
	 * @throws RepositoryException
	 */
	public void undeleteObject(ISupportChilds parent, IPersist persist) throws RepositoryException
	{
		persist.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o.getRootObject() instanceof AbstractRootObject)
				{
					((AbstractRootObject)o.getRootObject()).registerNewObject(o);
					o.flagChanged();
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		parent.addChild(persist);
	}

	public abstract IRootObject createNewRootObject(String name, int objectTypeId, int newElementID, UUID uuid) throws RepositoryException;

	public abstract void restoreObjectToCurrentRelease(IPersist persist) throws RepositoryException;

	/*
	 * _____________________________________________________________ Server methods, note servers are not in version control!
	 */

	/**
	 * Load the server names which are defined in this repository<br>
	 * Will also return the name of the server which created this repository
	 *
	 * @return the defined server names
	 */
	public String[] getServerNames(boolean sort) throws RepositoryException
	{
		return serverManager.getServerNames(true, false, sort, false);
	}

	/**
	 * Returns only valid server interfaces.
	 * <p>
	 * <b>NOTE: NEVER call this method from client code, always use solution.getServer(name), databaseManager.switchServer() is based on this!<b>
	 *
	 * @return the server
	 */
	public IServer getServer(String name)
	{
		return serverManager.getServer(name, true, false);
	}

	public String[] getDuplicateServerNames(String name)
	{
		return serverManager.getDuplicateServerNames(name, true, false);
	}

	public IServer[] getValidDataModelCloneServers(String name)
	{
		if (name == null) return null;
		IServer[] clones = serverManager.getDataModelCloneServers(name);
		List<IServer> validClones = new ArrayList<IServer>();
		if (clones != null && clones.length > 0)
		{
			for (IServer clone : clones)
			{
				try
				{
					if (serverManager.getServer(clone.getName(), true, true) != null)
					{
						// valid server
						validClones.add(clone);
					}
				}
				catch (Exception ex)
				{
					Debug.error(ex);
				}
			}
		}
		return validClones.toArray(new IServer[0]);

	}

	public Map<String, IServer> getServerProxies(RootObjectMetaData[] metas) throws RepositoryException
	{
		Map<String, IServer> retval = new HashMap<String, IServer>();
		for (RootObjectMetaData element : metas)
		{
			Solution s = (Solution)getRootObject(element.getRootObjectId(), element.getActiveRelease());
			Map<String, IServer> sps = s.getServerProxies();
			synchronized (sps)
			{
				retval.putAll(sps);
			}
		}
		return retval;
	}

	public List<RootObjectReference> getActiveSolutionModuleMetaDatas(int solutionId) throws RepositoryException
	{
		return repositoryHelper.getActiveSolutionModuleMetaDatas(solutionId);
	}

	public RootObjectMetaData createRootObjectMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease,
		int latestRelease)
	{
		switch (objectTypeId)
		{
			case IRepository.SOLUTIONS :
				return new SolutionMetaData(rootObjectId, rootObjectUuid, name, objectTypeId, activeRelease, latestRelease);
			default :
				return new RootObjectMetaData(rootObjectId, rootObjectUuid, name, objectTypeId, activeRelease, latestRelease);
		}
	}

	public RootObjectMetaData createNewRootObjectMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease,
		int latestRelease) throws RepositoryException
	{
		RootObjectMetaData romd = createRootObjectMetaData(rootObjectId, rootObjectUuid, name, objectTypeId, activeRelease, latestRelease);
		getRootObjectCache().add(romd, false);
		return romd;
	}

	/**
	 * Adds the given meta data object to the root object meta data cache.
	 *
	 * @param rootObjectMetaData the meta data to be added.
	 */
	public void addRootObjectMetaData(RootObjectMetaData rootObjectMetaData) throws RepositoryException
	{
		getRootObjectCache().add(rootObjectMetaData, true);
	}

	// use this with caution, will not load all rootobjects
	public void cacheRootObject(IRootObject rootObject) throws RepositoryException
	{
		try
		{
			synchronized (ROOT_OBJECT_SYNC)
			{
				if (rootObjectCache == null)
				{
					rootObjectCache = new RootObjectCache(this, new ArrayList());
				}
			}
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
		addRootObjectMetaData(rootObject.getRootObjectMetaData());
		getRootObjectCache().cacheRootObject(rootObject);
	}

	public abstract void updateRootObject(IRootObject rootObject) throws RepositoryException;
}
