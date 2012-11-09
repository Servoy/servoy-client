/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;


/**
 * This class filters out, in Developer only, things which should not be allowed, as (currently) improper usage
 * of import hooks (for instance, pre-import hooks need special attention as their objects cannot be used 
 * inside the main solution) 
 * 
 * @author acostache
 *
 */
public class DeveloperFlattenedSolution extends FlattenedSolution
{
	private final boolean filterImportHooks;

	public DeveloperFlattenedSolution(boolean filterImportHooks)
	{
		super();
		this.filterImportHooks = filterImportHooks;
	}

	private Solution[] filterModules(String mainSolution, Solution[] modulez)
	{
		List<Solution> filteredModules = new ArrayList<Solution>();
		for (Solution module : modulez)
		{
			if (!SolutionMetaData.isImportHook(module.getSolutionMetaData())) filteredModules.add(module);
			else if (mainSolution.equals(module.getName())) filteredModules.add(module); //if it's the main solution we add it to its list of modules
		}
		return filteredModules.toArray(new Solution[filteredModules.size()]);
	}

	@Override
	protected void setSolutionAndModules(String mainSolutionName, Solution[] mods) throws RemoteException
	{
		super.setSolutionAndModules(mainSolutionName, (filterImportHooks ? filterModules(mainSolutionName, mods) : mods));
	}
}
