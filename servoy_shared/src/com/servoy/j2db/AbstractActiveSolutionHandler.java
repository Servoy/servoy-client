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

import java.io.IOException;
import java.rmi.RemoteException;

import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.LocalhostRMIRegistry;

/**
 * Base active solution handler.
 * @author rob
 *
 */
public abstract class AbstractActiveSolutionHandler implements IActiveSolutionHandler
{
	public IApplicationServer getApplicationServer()
	{
		return (IApplicationServer)LocalhostRMIRegistry.getService(IApplicationServer.NAME);
	}

	abstract public IRepository getRepository();

	public boolean haveRepositoryAccess()
	{
		return getRepository() != null;
	}

	public Solution[] loadActiveSolutions(RootObjectMetaData[] solutionDefs) throws RepositoryException, RemoteException
	{
		Solution[] retval = new Solution[solutionDefs.length];
		for (int i = 0; i < solutionDefs.length; i++)
		{
			retval[i] = loadSolution(solutionDefs[i]);
		}
		return retval;
	}

	protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
	{
		return (Solution)getRepository().getActiveRootObject(solutionDef.getRootObjectId());
	}

	public Solution[] loadLoginSolutionAndModules(SolutionMetaData mainSolutionDef) throws RepositoryException, RemoteException
	{
		SolutionMetaData[] loginSolutionDefinitions = getApplicationServer().getLoginSolutionDefinitions(mainSolutionDef);
		if (loginSolutionDefinitions == null)
		{
			throw new RepositoryException("Could not load login solution");
		}
		Solution[] solutions = new Solution[loginSolutionDefinitions.length];
		for (int i = 0; i < loginSolutionDefinitions.length; i++)
		{
			solutions[i] = loadLoginSolution(mainSolutionDef, loginSolutionDefinitions[i]);
		}

		return solutions;
	}

	protected Solution loadLoginSolution(SolutionMetaData mainSolutionDef, SolutionMetaData loginSolutionDef) throws RemoteException, RepositoryException
	{
		Solution loginSolution = getApplicationServer().getLoginSolution(mainSolutionDef, loginSolutionDef);
		if (loginSolution != null && loginSolution.getRepository() == null)
		{
			loginSolution.setRepository(getRepository()); // transient
		}
		return loginSolution;
	}

	public void saveActiveSolution(Solution solution) throws IOException
	{
		// ignore
	}

}
