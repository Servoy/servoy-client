/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.UUID;

public class TestRepository extends AbstractRepository
{
	private final IntHashMap<UUID> intToUUID = new IntHashMap<>();
	private final HashMap<UUID, Integer> uuidToInt = new HashMap<>();
	private int elementIdCounter = 1;
	private RootObjectMetaData createdMetaData;
	private IRootObject rootObject;

	/**
	 * @param serverManager
	 */
	protected TestRepository()
	{
		super(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.AbstractRepository#createRootObjectMetaData(int, com.servoy.j2db.util.UUID, java.lang.String, int, int, int)
	 */
	@Override
	public RootObjectMetaData createRootObjectMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease,
		int latestRelease)
	{
		createdMetaData = super.createRootObjectMetaData(rootObjectId, rootObjectUuid, name, objectTypeId, activeRelease, latestRelease);
		return createdMetaData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.AbstractRepository#createRootObject(com.servoy.j2db.persistence.RootObjectMetaData)
	 */
	@Override
	public IRootObject createRootObject(RootObjectMetaData metaData) throws RepositoryException
	{
		rootObject = super.createRootObject(metaData);
		return rootObject;
	}

	@Override
	public IColumnInfoManager getColumnInfoManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserProperties(int systemUserId, Map props) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setRootObjectActiveRelease(int rootObjectId, int releaseNumber) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getMediaBlob(int blob_id) throws RemoteException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNewElementID(UUID new_uuid) throws RepositoryException
	{
		int id = elementIdCounter++;
		intToUUID.put(id, new_uuid);
		uuidToInt.put(new_uuid, Integer.valueOf(id));
		return id;
	}

	@Override
	public Properties getUserProperties(int user_id) throws RepositoryException
	{
		return null;
	}

	@Override
	protected Collection<RootObjectMetaData> loadRootObjectMetaDatas() throws Exception
	{
		return Arrays.asList(createdMetaData);
	}

	@Override
	protected IRootObject loadRootObject(RootObjectMetaData romd, int releaseNumber) throws RepositoryException
	{
		return rootObject;
	}

	@Override
	public IRootObject createNewRootObject(String name, int objectTypeId, int newElementID, UUID uuid) throws RepositoryException
	{
		return createRootObject(createRootObjectMetaData(newElementID, uuid, name, objectTypeId, 1, 1));
	}

	@Override
	public void restoreObjectToCurrentRelease(IPersist persist) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRootObject(IRootObject rootObject) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}


	@Override
	public int resolveIdForElementUuid(UUID id) throws RepositoryException
	{
		Integer integer = uuidToInt.get(id);
		if (integer != null) return integer.intValue();
		return 0;
	}

	@Override
	protected ContentSpec loadContentSpec() throws RepositoryException
	{
		return StaticContentSpecLoader.getContentSpec();
	}

	@Override
	public UUID resolveUUIDForElementId(int id) throws RepositoryException
	{
		return intToUUID.get(id);
	}

}