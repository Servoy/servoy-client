/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 * @since 8.4
 */
public class PersistIndexCache
{
	private static final Logger LOG = LoggerFactory.getLogger("com.servoy.PersistIndexCache");

	private static final ConcurrentMap<UUID, IPersistIndex> persistIndexCache = new ConcurrentHashMap<>();

	public static IPersistIndex getPersistIndex(Solution solution, Solution[] modules)
	{
		boolean isCloned = solution.getRuntimeProperty(AbstractBase.Cloned) != null && solution.getRuntimeProperty(AbstractBase.Cloned).booleanValue();
		IPersistIndex index = isCloned ? null : persistIndexCache.get(solution.getUUID());
		if (index == null)
		{
			List<Solution> solutions = new ArrayList<>();
			solutions.add(solution);
			if (modules != null)
			{
				for (Solution mod : modules)
				{
					solutions.add(mod);
				}
			}
			index = new PersistIndex(solutions);

			if (!isCloned)
			{
				IPersistIndex alreadyCreated = persistIndexCache.putIfAbsent(solution.getUUID(), index);
				if (alreadyCreated != null) index = alreadyCreated;
				LOG.debug("Persist Index Cache entry created for " + solution + " with modules " + Arrays.asList(modules));
			}
		}
		return index;
	}

	public static void flush()
	{
		LOG.debug("Persist Index Cache cleared");
		persistIndexCache.clear();
	}

	public static void reload()
	{
		persistIndexCache.values().stream().forEach(index -> index.reload());
	}

	/**
	 * @param solution
	 * @return
	 */
	public static IPersistIndex getCachedIndex(Solution solution)
	{
		if (solution != null) return persistIndexCache.get(solution.getUUID());
		return null;
	}

}
