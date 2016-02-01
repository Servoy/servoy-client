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
import java.util.List;
import java.util.Map;

import com.servoy.j2db.util.UUID;

/**
 * Developer side of repository
 * @author jblok
 */
public interface IDeveloperRepository extends IRepository, IPersistFactory
{
	public ContentSpec getContentSpec() throws RepositoryException;

	public int getElementIdForUUID(UUID id) throws RepositoryException;

	public RootObjectMetaData createRootObjectMetaData(int rootObjectId, UUID rootObjectUuid, String name, int objectTypeId, int activeRelease,
		int latestRelease) throws RepositoryException;

	public IRootObject createRootObject(RootObjectMetaData metaData) throws RepositoryException;

	public void deleteObject(IPersist persist) throws RepositoryException;

	public void undeleteObject(ISupportChilds parent, IPersist persist) throws RepositoryException;

	public Map<String, Object> getPersistAsValueMap(IPersist persist) throws RepositoryException;

	public void updatePersistWithValueMap(IPersist retval, Map<String, Object> propertyValues, boolean overwrite) throws RepositoryException;

	public Object convertArgumentStringToObject(int typeID, String propertyObjectValue) throws RepositoryException;

	public String convertObjectToArgumentString(int typeID, Object propertyObjectValue) throws RepositoryException;

	public IColumnInfoManager getColumnInfoManager();

	public UUID getRepositoryUUID() throws RepositoryException;

	public Map getUserProperties(int user_id) throws RepositoryException; //user 0 is SYSTEM

	public void setUserProperties(int systemUserId, Map props) throws RepositoryException; //user 0 is SYSTEM


	public void setRootObjectActiveRelease(int rootObjectId, int releaseNumber) throws RepositoryException;

	public IRootObject getRootObject(int rootObjectId, int releaseNumber) throws RepositoryException;

	public List getActiveRootObjects(int type) throws RepositoryException;

	public void flushRootObject(int rootID) throws RepositoryException;

	public void removeRootObject(int solutionId) throws RepositoryException;

	public void flushAllCachedData() throws RepositoryException;

	public void updateRootObject(IRootObject rootObject) throws RepositoryException;

	public IServer getServer(String name) throws RemoteException, RepositoryException;
}
