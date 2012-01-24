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
 * Used by clients to access the repository.
 * 
 * @author jblok
 */
public interface IRepository
{
	/**
	 * Max length for root object names.
	 */
	public static final int MAX_ROOT_OBJECT_NAME_LENGTH = 100;

	/**
	 * Element id for unresolved elements.
	 */
	public static final int UNRESOLVED_ELEMENT = -100;
	public static final UUID UNRESOLVED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"); //$NON-NLS-1$
	public static final String REPOSITORY_UUID_PROPERTY_NAME = "repository_uuid"; //$NON-NLS-1$

	/**
	 * Final security types
	 */
	public static final int VIEWABLE = 1;
	public static final int ACCESSIBLE = 2;
	/**
	 * Default access rights for a form/form element.
	 */
	public static final int IMPLICIT_FORM_ACCESS = VIEWABLE | ACCESSIBLE;

	public static final int READ = 1;
	public static final int INSERT = 2;
	public static final int UPDATE = 4;
	public static final int DELETE = 8;
	public static final int TRACKING = 16;
	/**
	 * Default access rights for a table.
	 */
	public static final int IMPLICIT_TABLE_ACCESS = READ | INSERT | UPDATE | DELETE; // NO TRACKING for implicit

	public static final int SYSTEM_USER_ID = 0;
	public static final String ADMIN_GROUP = "Administrators"; //$NON-NLS-1$
	public static final String TEAM_GROUP = "TeamUsers"; //$NON-NLS-1$

	/**
	 * Final repository object_types
	 */
	public static final int RELEASES = 1;
	public static final int BLOBS = 2;
	public static final int FORMS = 3;
	public static final int FIELDS = 4;
	public static final int INSETS = 5;
	public static final int PORTALS = 6;
	public static final int GRAPHICALCOMPONENTS = 7; //button,image,label replacement
	public static final int SERVERS = 8;
	public static final int DATASOURCES = 9;
	public static final int STYLES = 10;
	public static final int TEMPLATES = 11;
	public static final int BEANS = 12;
	public static final int ELEMENTS = 13;
	public static final int ELEMENT_PROPERTIES = 14;
	public static final int TABS = 15;
	public static final int TABPANELS = 16;
	public static final int LINES = 17;
	public static final int SHAPES = 18;//obsolete should be removed!!, but that will break repository updates
	public static final int PARTS = 19;
	public static final int RECTSHAPES = 21;

	public static final int RELATIONS = 22;
	public static final int RELATION_ITEMS = 23;
	public static final int METHODS = 24;
	public static final int STATEMENTS = 25;

	public static final int INTEGER = 26;
	public static final int COLOR = 27;
	public static final int POINT = 28;
	public static final int STRING = 29;
	public static final int DIMENSION = 30;
	public static final int FONT = 31;
	public static final int BOOLEAN = 32;
	public static final int BORDER = 33;

	public static final int VALUELISTS = 34;
	public static final int SCRIPTVARIABLES = 35;
	public static final int SCRIPTCALCULATIONS = 36;

	public static final int MEDIA = 37;
	public static final int COLUMNS = 38; //SYNC_IDS called before, needed columns type for Ivalidatename searchcontext type 
	public static final int TABLENODES = 39; // better name whould be datasource node
	public static final int AGGREGATEVARIABLES = 40;

	public static final int USER_PROPERTIES = 41;
	public static final int LOGS = 42;
	public static final int SOLUTIONS = 43;
	public static final int TABLES = 44;
	public static final int STATS = 45;

	/**
	 * Get all the defined server interfaces.
	 * 
	 * @return String[] all the server names
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public String[] getServerNames(boolean sort) throws RemoteException, RepositoryException;

	/**
	 * Get the named server interface.
	 * <p>
	 * <b>NOTE: NEVER call this method from client code, always use solution.getServer(name), databaseManager.switchServer() is based on this!<b>
	 * 
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public IServer getServer(String name) throws RemoteException, RepositoryException;

	/**
	 * Get the names of the server that are valid for the name.
	 * 
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public String[] getDuplicateServerNames(String name) throws RemoteException, RepositoryException;

	/**
	 * Get the server proxies for offline loading
	 * 
	 * @param solutionId the solution id
	 * @param release the solution release
	 * @param alreadyKnownServerNames list of names which are already known and dont have to be returned (can be null)
	 * @return the server proxies
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public Map<String, IServer> getServerProxies(RootObjectMetaData[] metas) throws RemoteException, RepositoryException;

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 * 
	 * @exclude
	 */
	public RootObjectMetaData getRootObjectMetaData(int rootObjectId) throws RemoteException, RepositoryException;

	public RootObjectMetaData getRootObjectMetaData(UUID uuid) throws RemoteException, RepositoryException;

	public RootObjectMetaData getRootObjectMetaData(String name, int objectTypeId) throws RemoteException, RepositoryException;

	public RootObjectMetaData[] getRootObjectMetaDatas() throws RemoteException, RepositoryException;

	public RootObjectMetaData[] getRootObjectMetaDatasForType(int objectTypeId) throws RemoteException, RepositoryException;

	/**
	 * Get a root object.
	 * 
	 * @param id the id
	 * @return Root object
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public IRootObject getActiveRootObject(int id) throws RemoteException, RepositoryException;

	public IRootObject getActiveRootObject(String name, int objectTypeId) throws RemoteException, RepositoryException;

	/**
	 * Get the update sequences of the active solutions.
	 * 
	 * @param rootObjectIds the solution ids
	 * @return the update seqences of the specified solution
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException;

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL. Should only be called from Media Object!
	 * 
	 * @exclude
	 */
	public byte[] getMediaBlob(int blob_id) throws RemoteException, RepositoryException;

	public List<RootObjectReference> getActiveSolutionModuleMetaDatas(int solutionId) throws RemoteException, RepositoryException;

}
