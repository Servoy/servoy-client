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
package com.servoy.j2db.server.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.servoy.j2db.ClientLogin;
import com.servoy.j2db.Credentials;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * Only entry point to access the application server from a client.
 * 
 * @author rgansevles
 *
 */
public interface IApplicationServer extends Remote
{
	public static final String NAME = "servoy.IApplicationServer"; //$NON-NLS-1$

	public static final String DUMMY_LOGIN = "<dummy-login>"; //$NON-NLS-1$
	public static final String DEBUG_POSTFIX = ".debug"; //$NON-NLS-1$

	public IApplicationServerAccess getApplicationServerAccess(String clientId) throws RemoteException;

	public IClientHost getClientHost() throws RemoteException;

	public SolutionMetaData getSolutionDefinition(String solutionName, int solutionTypeFilter) throws RemoteException, RepositoryException;

	public SolutionMetaData[] getSolutionDefinitions(int solutionTypeFilter) throws RemoteException, RepositoryException;

	public SolutionMetaData[] getLoginSolutionDefinitions(SolutionMetaData solutionMetaData) throws RemoteException, RepositoryException;

	public Solution getLoginSolution(SolutionMetaData mainSolution, SolutionMetaData loginSolution) throws RemoteException, RepositoryException;

	public ClientLogin login(Credentials credentials) throws RemoteException, RepositoryException;

	public Remote getRemoteService(String cid, String rmiLookupName) throws RemoteException;

	/**
	 * Retrieve an client id, if user_uid and password is correct for use in Servoy team provider.
	 * 
	 * @param user_uid
	 * @param passwordMD5Hash (use Utils.calculateMD5Hash(...))
	 * @return the client_id or null if not ok
	 */
	public String getClientID(String user_uid, String passwordMD5Hash) throws RemoteException;

	/**
	 * Get the update sequences of the active solutions.
	 * 
	 * @param rootObjectIds the solution ids
	 * @return the update seqences of the specified solution
	 * @throws RemoteException
	 * @throws RepositoryException
	 */
	public long[] getActiveRootObjectsLastModified(int[] rootObjectIds) throws RemoteException, RepositoryException;

}
