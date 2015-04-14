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

import com.servoy.base.persistence.constants.IRepositoryConstants;
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
	public static final int TRACKING_VIEWS = 32;

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
	public static final int RELEASES = IRepositoryConstants.RELEASES;
	public static final int BLOBS = IRepositoryConstants.BLOBS;
	public static final int FORMS = IRepositoryConstants.FORMS;
	public static final int FIELDS = IRepositoryConstants.FIELDS;
	public static final int INSETS = IRepositoryConstants.INSETS;
	public static final int PORTALS = IRepositoryConstants.PORTALS;
	public static final int GRAPHICALCOMPONENTS = IRepositoryConstants.GRAPHICALCOMPONENTS; //button,image,label replacement
	public static final int SERVERS = IRepositoryConstants.SERVERS;
	public static final int DATASOURCES = IRepositoryConstants.DATASOURCES;
	public static final int STYLES = IRepositoryConstants.STYLES;
	public static final int TEMPLATES = IRepositoryConstants.TEMPLATES;
	public static final int BEANS = IRepositoryConstants.BEANS;
	public static final int ELEMENTS = IRepositoryConstants.ELEMENTS;
	public static final int ELEMENT_PROPERTIES = IRepositoryConstants.ELEMENT_PROPERTIES;
	public static final int TABS = IRepositoryConstants.TABS;
	public static final int TABPANELS = IRepositoryConstants.TABPANELS;
	public static final int LINES = IRepositoryConstants.LINES;
	public static final int SHAPES = IRepositoryConstants.SHAPES;//obsolete should be removed!!, but that will break repository updates
	public static final int PARTS = IRepositoryConstants.PARTS;
	public static final int RECTSHAPES = IRepositoryConstants.RECTSHAPES;
	public static final int LAYOUTCONTAINERS = IRepositoryConstants.LAYOUTCONTAINERS;
	public static final int WEBCOMPONENTS = IRepositoryConstants.WEBCOMPONENTS;
	public static final int WEBCUSTOMTYPES = IRepositoryConstants.WEBCUSTOMTYPES;

	public static final int RELATIONS = IRepositoryConstants.RELATIONS;
	public static final int RELATION_ITEMS = IRepositoryConstants.RELATION_ITEMS;
	public static final int METHODS = IRepositoryConstants.METHODS;
	public static final int STATEMENTS = IRepositoryConstants.STATEMENTS;

	public static final int INTEGER = IRepositoryConstants.INTEGER;
	public static final int COLOR = IRepositoryConstants.COLOR;
	public static final int POINT = IRepositoryConstants.POINT;
	public static final int STRING = IRepositoryConstants.STRING;
	public static final int DIMENSION = IRepositoryConstants.DIMENSION;
	public static final int FONT = IRepositoryConstants.FONT;
	public static final int BOOLEAN = IRepositoryConstants.BOOLEAN;
	public static final int BORDER = IRepositoryConstants.BORDER;
	public static final int JSON = IRepositoryConstants.JSON;

	public static final int VALUELISTS = IRepositoryConstants.VALUELISTS;
	public static final int SCRIPTVARIABLES = IRepositoryConstants.SCRIPTVARIABLES;
	public static final int SCRIPTCALCULATIONS = IRepositoryConstants.SCRIPTCALCULATIONS;

	public static final int MEDIA = IRepositoryConstants.MEDIA;
	public static final int COLUMNS = IRepositoryConstants.COLUMNS; //SYNC_IDS called before, needed columns type for Ivalidatename searchcontext type
	public static final int TABLENODES = IRepositoryConstants.TABLENODES; // better name whould be datasource node
	public static final int AGGREGATEVARIABLES = IRepositoryConstants.AGGREGATEVARIABLES;

	public static final int USER_PROPERTIES = IRepositoryConstants.USER_PROPERTIES;
	public static final int LOGS = IRepositoryConstants.LOGS;
	public static final int SOLUTIONS = IRepositoryConstants.SOLUTIONS;
	public static final int TABLES = IRepositoryConstants.TABLES;
	public static final int STATS = IRepositoryConstants.STATS;

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
