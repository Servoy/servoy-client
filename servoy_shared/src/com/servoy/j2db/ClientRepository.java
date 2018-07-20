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
package com.servoy.j2db;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.servoy.j2db.persistence.AbstractRootObject;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.UUID;

/**
 * Delegate repository responsible for replacing the repository in serialized root objects with a local repository.
 *
 * @author rgansevles
 *
 */
public class ClientRepository implements IRepository, IDelegate<IRepository>
{
	private IRepository repository;
	// lots of unneeded RMI calls are made if not for this cache - and that meant lots and lots of lag
	private final Map<String, String[]> duplicateServerNamesCache = new ConcurrentHashMap<String, String[]>();

	public ClientRepository(IRepository repository /* may be null and filled when repository access is there */)
	{
		this.repository = repository;
	}

	public IRepository getDelegate()
	{
		return repository;
	}

	public void setDelegate(IRepository repository)
	{
		this.repository = repository;
	}

	protected <T> T updateRepository(T t)
	{
		if (t instanceof AbstractRootObject && ((AbstractRootObject)t).getRepository() == null)
		{
			((AbstractRootObject)t).setRepository(this);
		}
		return t;
	}

	public String[] getServerNames(boolean sort) throws RemoteException, RepositoryException
	{
		if (repository == null) return new String[0];
		return repository.getServerNames(sort);
	}

	public IServer getServer(String name) throws RemoteException, RepositoryException
	{
		if (repository == null) return null;
		return repository.getServer(name);
	}

	public String[] getDuplicateServerNames(String name) throws RemoteException, RepositoryException
	{
		String[] result = null;
		if (repository != null)
		{
			if (duplicateServerNamesCache.containsKey(name))
			{
				result = duplicateServerNamesCache.get(name);
			}
			else
			{
				result = repository.getDuplicateServerNames(name);
				duplicateServerNamesCache.put(name, result);
			}
		}
		return result;
	}

	public ConcurrentMap<String, IServer> getServerProxies(RootObjectMetaData[] metas) throws RemoteException, RepositoryException
	{
		if (repository == null) return new ConcurrentHashMap<String, IServer>();
		return repository.getServerProxies(metas);
	}

	public RootObjectMetaData getRootObjectMetaData(int rootObjectId) throws RemoteException, RepositoryException
	{
		if (repository == null) return null;
		return repository.getRootObjectMetaData(rootObjectId);
	}

	public RootObjectMetaData getRootObjectMetaData(UUID uuid) throws RemoteException, RepositoryException
	{
		if (repository == null) return null;
		return repository.getRootObjectMetaData(uuid);
	}

	public RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId) throws RemoteException, RepositoryException
	{
		if (repository == null) return null;
		return repository.getRootObjectMetaData(name, objectTypeId);
	}

	public RootObjectMetaData[] getRootObjectMetaDatas() throws RemoteException, RepositoryException
	{
		if (repository == null) return new RootObjectMetaData[0];
		return repository.getRootObjectMetaDatas();
	}

	public RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId) throws RemoteException, RepositoryException
	{
		if (repository == null) return new RootObjectMetaData[0];
		return repository.getRootObjectMetaDatasForType(objectTypeId);
	}

	public IRootObject getActiveRootObject(int id) throws RemoteException, RepositoryException
	{
		if (repository == null) return null;
		return updateRepository(repository.getActiveRootObject(id));
	}

	public IRootObject getActiveRootObject(String name, int objectTypeId) throws RemoteException, RepositoryException
	{
		if (repository == null) return null;
		return updateRepository(repository.getActiveRootObject(name, objectTypeId));
	}

	public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException
	{
		if (repository == null) return new long[0];
		return repository.getActiveRootObjectsLastModified(rootObjectIds);
	}

	public byte[] getMediaBlob(int blob_id) throws RemoteException, RepositoryException
	{
		if (repository == null) return new byte[0];
		return repository.getMediaBlob(blob_id);
	}

	public List<RootObjectReference> getActiveSolutionModuleMetaDatas(int solutionId) throws RemoteException, RepositoryException
	{
		if (repository == null) return Collections.<RootObjectReference> emptyList();
		return repository.getActiveSolutionModuleMetaDatas(solutionId);
	}

}
