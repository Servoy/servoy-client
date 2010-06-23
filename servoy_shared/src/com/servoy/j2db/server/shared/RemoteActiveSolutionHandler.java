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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.LocalActiveSolutionHandler;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Load solutions from local client cache if the active update seq matches
 * 
 * @author jblok
 */
public class RemoteActiveSolutionHandler extends LocalActiveSolutionHandler
{
	private final Map<Integer, Long> loadedActiveSolutionUpdateSequences = new HashMap<Integer, Long>(); //solution_id -> asus

	public RemoteActiveSolutionHandler(IServiceProvider sp)
	{
		super(sp);
	}

	@Override
	public Solution[] loadActiveSolutions(RootObjectMetaData[] solutionDefs) throws RepositoryException, RemoteException
	{
		int[] sol_ids = new int[solutionDefs.length];
		for (int i = 0; i < sol_ids.length; i++)
		{
			sol_ids[i] = solutionDefs[i].getRootObjectId();
		}
		long asus[] = getApplicationServer().getActiveRootObjectsLastModified(sol_ids);
		Map<String, IServer> sps = getRepository().getServerProxies(solutionDefs);
		Solution[] retval = new Solution[solutionDefs.length];
		for (int i = 0; i < solutionDefs.length; i++)
		{
			Solution s = loadCachedSolution(solutionDefs[i], asus[i], sps);
			if (s == null)
			{
				//do full load
				s = loadSolution(solutionDefs[i]);
			}
			if (s != null)
			{
				if (s.getRepository() == null)
				{
					s.setRepository(getRepository()); // transient
				}
				loadedActiveSolutionUpdateSequences.put(new Integer(s.getSolutionID()), new Long(asus[i]));
			}

			retval[i] = s;
		}
		return retval;
	}

	@Override
	public IApplicationServer getApplicationServer()
	{
		return getServiceProvider().getApplicationServer();
	}

	@Override
	public Solution[] loadLoginSolutionAndModules(SolutionMetaData mainSolutionDef) throws RepositoryException, RemoteException
	{
		SolutionMetaData[] loginSolutionDefinitions = getApplicationServer().getLoginSolutionDefinitions(mainSolutionDef);
		if (loginSolutionDefinitions == null)
		{
			throw new RepositoryException("Could not load login solution");
		}
		int[] sol_ids = new int[loginSolutionDefinitions.length];
		for (int i = 0; i < sol_ids.length; i++)
		{
			sol_ids[i] = loginSolutionDefinitions[i].getRootObjectId();
		}
		long asus[] = getApplicationServer().getActiveRootObjectsLastModified(sol_ids);

		Solution[] solutions = new Solution[loginSolutionDefinitions.length];
		for (int i = 0; i < loginSolutionDefinitions.length; i++)
		{
			Solution s = loadCachedSolution(loginSolutionDefinitions[i], asus[i], null);
			if (s == null)
			{
				//do full load
				s = getApplicationServer().getLoginSolution(mainSolutionDef, loginSolutionDefinitions[i]);
			}
			if (s != null)
			{
				if (s.getRepository() == null)
				{
					s.setRepository(getRepository()); // transient
				}
				loadedActiveSolutionUpdateSequences.put(new Integer(s.getSolutionID()), new Long(asus[i]));
			}

			solutions[i] = s;
		}

		return solutions;
	}


	private Solution loadCachedSolution(RootObjectMetaData solutionDef, long lastModified, Map<String, IServer> serverProxies)
	{

		int solID = solutionDef.getRootObjectId();
		Solution s = null;
		//try disk load
		File file = null;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		GZIPInputStream zip = null;
		ObjectInputStream ois = null;
		try
		{
			URL url = getServiceProvider().getServerURL();
			String name = (url.getHost() + '_' + url.getPort()) + '_' + solutionDef.getName();
			file = new File(System.getProperty("user.home"), J2DBGlobals.CLIENT_LOCAL_DIR + name + ".solution"); //$NON-NLS-1$ //$NON-NLS-2$
			if (file.exists())
			{
				fis = new FileInputStream(file);
				bis = new BufferedInputStream(fis);
				zip = new GZIPInputStream(bis);
				ois = new ObjectInputStream(zip);
				long stored_asus = ois.readLong();
				if (stored_asus == lastModified)
				{
					Solution fileSolution = (Solution)ois.readObject();
					if (fileSolution.getSolutionID() == solID)//check if same
					{
						s = fileSolution;
						s.setServerProxies(serverProxies);
						Utils.closeInputStream(ois);
					}
					else
					{
						Utils.closeInputStream(ois);
						file.delete();
					}
				}
				else
				{
					Utils.closeInputStream(ois);
					file.delete();
				}
			}
		}
		catch (Exception e)//can fail when classes are changed and do not longer match
		{
			s = null;
			Debug.trace(e);//no need to show to user
			Utils.closeInputStream(ois);
			Utils.closeInputStream(fis);
			try
			{
				if (file != null) file.delete();//if error happens just delete the file
			}
			catch (Exception e1)
			{
				Debug.error(e1);
			}
		}

		return s;
	}

	@Override
	public void saveActiveSolution(Solution solution) throws IOException
	{
		URL url = getServiceProvider().getServerURL();
		String name = (url.getHost() + '_' + url.getPort()) + '_' + solution.getName();

		File hiddendir = new File(System.getProperty("user.home"), J2DBGlobals.CLIENT_LOCAL_DIR); //$NON-NLS-1$
		if (!hiddendir.exists()) hiddendir.mkdirs();

		Long asus = loadedActiveSolutionUpdateSequences.get(new Integer(solution.getSolutionID()));
		File file = new File(hiddendir, name + ".solution"); //$NON-NLS-1$
		if (!file.exists() && asus != null && asus.longValue() >= 0)
		{
			solution.setServerProxies(null);//clear
			solution.setRepository(null);//clear

			FileOutputStream fis = new FileOutputStream(file);
			BufferedOutputStream bis = new BufferedOutputStream(fis);
			GZIPOutputStream zip = new GZIPOutputStream(bis);
			ObjectOutputStream ois = new ObjectOutputStream(zip);
			ois.writeLong(asus.longValue());
			ois.writeObject(solution);
			ois.close();
		}
	}


}
