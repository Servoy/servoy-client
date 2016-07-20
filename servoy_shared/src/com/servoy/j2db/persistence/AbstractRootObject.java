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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Reposository tree root object
 *
 * @author sebster
 */
public abstract class AbstractRootObject extends AbstractBase implements IRootObject
{

	private static final long serialVersionUID = 1L;

	private transient IRepository repository;

	// Because this map can be accessed from multiple places at once and changed from multiple places at once,
	// modifying it's contents and iterating over the contents are syncronized (mutual exclusion); otherwise we would end up with concurrent modification exceptions.
	// For example: a solution A has module B, one client opens solution A and another client opens solution B.
	// The initial fix was using ConcurrentHashMap, but there is a bug in Terracotta with ConcurrentHashMap and serialization, so we can't use that for now. See http://jira.terracotta.org/jira/browse/CDV-1377
	private Map<String, IServer> serverProxies; //name -> Server

	private RootObjectMetaData metaData;
	private int releaseNumber;

	private List<IPersist> newObjects;
	private List<IPersist> removedObjects;

	/**
	 * Connection handling, only for developer,therefore transient
	 */
	private transient ChangeHandler changeHandler = null;

	public AbstractRootObject(IRepository repository, RootObjectMetaData metaData)
	{
		super(metaData.getObjectTypeId(), null, metaData.getRootObjectId(), metaData.getRootObjectUuid());
		this.repository = repository;
		this.metaData = metaData;
	}

	void setMetaData(RootObjectMetaData metaData)
	{
		this.metaData = metaData;
	}

	@Override
	public MetaData getMetaData()
	{
		return getRootObjectMetaData();
	}

	public RootObjectMetaData getRootObjectMetaData()
	{
		return metaData;
	}

	public String getName()
	{
		return getRootObjectMetaData().getName();
	}

	public int getReleaseNumber()
	{
		return releaseNumber;
	}

	// for setting after load
	public void setReleaseNumber(int releaseNumber)
	{
		this.releaseNumber = releaseNumber;
	}

	public IRepository getRepository()
	{
		return repository;
	}

	// for setting after load
	public void setRepository(IRepository repository)
	{
		this.repository = repository;
	}

	// for setting after load.
	public void setChangeHandler(ChangeHandler connectionHandler)
	{
		this.changeHandler = connectionHandler;
		connectionHandler.setRootObject(this);
	}

	public ChangeHandler getChangeHandler()
	{
		return changeHandler;
	}

	@Override
	protected void fillClone(AbstractBase cloned)
	{
		// clear changeHandler, the original change handler should not listen to changes in clone
		((AbstractRootObject)cloned).changeHandler = null;
		super.fillClone(cloned);
	}

	@Override
	public IRootObject getRootObject()
	{
		return this;
	}

	/**
	 * This method should not be called by developer only code
	 *
	 * @param name
	 * @throws RepositoryException
	 */
	public IServer getServer(String name) throws RepositoryException
	{
		if (name == null) return null;
		String lowerCaseBame = Utils.toEnglishLocaleLowerCase(name);
		try
		{
			IServer server = null;
			Map<String, IServer> proxies = getServerProxies();

			server = proxies.get(lowerCaseBame);
			if (server == null)
			{
				// fetch a server that is not pre-loaded (could be  referred to in custom query)
				server = getRepository().getServer(lowerCaseBame);
				if (server != null && !(server instanceof IServerInternal))
				{
					//wrap
					server = new ServerProxy(server);
					synchronized (proxies)
					{
						proxies.put(lowerCaseBame, server);
					}
				}
			}
			return server;
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	public Map<String, IServer> getServerProxies()
	{
		if (serverProxies == null) serverProxies = new HashMap<String, IServer>();
		return serverProxies;
	}

	// for setting after load
	public void setServerProxies(Map<String, IServer> serverProxies)
	{
		this.serverProxies = serverProxies;
	}

	@Override
	public int hashCode()
	{
		return getTypeID() ^ getName().hashCode() ^ releaseNumber;
	}

	@Override
	public String toString()
	{
		return getRootObjectMetaData().getName();
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof IRootObject)
		{
			final IRootObject otherRootObject = (IRootObject)other;
			return getTypeID() == otherRootObject.getTypeID() && releaseNumber == otherRootObject.getReleaseNumber() &&
				getName().equals(otherRootObject.getName());
		}
		return false;
	}

	private long lastModifiedTime = System.currentTimeMillis();

	public synchronized void updateLastModifiedTime()
	{
		lastModifiedTime = System.currentTimeMillis();
	}

	// Must be synchronized to avoid read/write conflicts.
	public synchronized long getLastModifiedTime()
	{
		return lastModifiedTime;
	}

	public void registerNewObject(IPersist persist)
	{
		if (removedObjects != null && removedObjects.contains(persist))
		{
			removedObjects.remove(persist);
		}
		else
		{
			persist.flagChanged();
			doGetRegisteredNewObjects().add(persist);
		}
	}

	public boolean unregisterNewObject(IPersist persist)
	{
		if (newObjects != null)
		{
			return newObjects.remove(persist);
		}
		return false;
	}

	public boolean unregisterRemovedObject(IPersist persist)
	{
		if (removedObjects != null)
		{
			return removedObjects.remove(persist);
		}
		return false;
	}

	public void registerRemovedObject(IPersist persist)
	{
		if (newObjects != null && newObjects.contains(persist))
		{
			newObjects.remove(persist);
		}
		else
		{
			doGetRegisteredRemovedObjects().add(persist);
		}
	}

	public boolean isRegisteredNew(IPersist o)
	{
		return newObjects != null && newObjects.contains(o);
	}

	public boolean isRegisteredRemoved(IPersist o)
	{
		return removedObjects != null && removedObjects.contains(o);
	}

	public Iterator<IPersist> getRegisteredNewObjects()
	{
		return Collections.<IPersist> unmodifiableList(doGetRegisteredNewObjects()).iterator();
	}

	public Iterator<IPersist> getRegisteredRemovedObjects()
	{
		return Collections.<IPersist> unmodifiableList(doGetRegisteredRemovedObjects()).iterator();
	}

	protected List<IPersist> doGetRegisteredNewObjects()
	{
		if (newObjects == null) newObjects = new ArrayList<IPersist>();
		return newObjects;
	}

	protected List<IPersist> doGetRegisteredRemovedObjects()
	{
		if (removedObjects == null) removedObjects = new ArrayList<IPersist>();
		return removedObjects;
	}

	public void clearRegisteredRemovedObjects()
	{
		removedObjects = null;
	}

	public void clearRegisteredNewObjects()
	{
		newObjects = null;
	}

	public void clearEditingState(IPersist persist)
	{
		persist.clearChanged();
		UUID persistUuid = persist.getUUID();
		removeFromList(removedObjects, persistUuid);
		removeFromList(newObjects, persistUuid);
	}

	private static void removeFromList(List<IPersist> list, UUID persistUuid)
	{
		if (list == null)
		{
			return;
		}
		Iterator<IPersist> it = list.iterator();
		while (it.hasNext())
		{
			if (persistUuid.equals(it.next().getUUID()))
			{
				it.remove();
			}
		}
	}


}
