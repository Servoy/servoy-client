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

import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.server.IUnresolvedUUIDResolver;
import com.servoy.j2db.util.UUID;

/**
 * @author sebster
 *
 */
public class ChainedUnresolvedUUIDResolver implements IUnresolvedUUIDResolver
{
	private List resolvers;

	public ChainedUnresolvedUUIDResolver(List resolvers)
	{
		this.resolvers = resolvers;
	}
	
	public UUID resolve(int elementId, int revision, int contentId) throws RepositoryException
	{
		Iterator iterator = resolvers.iterator();
		while (iterator.hasNext())
		{
			IUnresolvedUUIDResolver resolver = (IUnresolvedUUIDResolver) iterator.next();
			UUID uuid = resolver.resolve(elementId, revision, contentId);
			if (uuid != null && !uuid.equals(IRepository.UNRESOLVED_UUID))
			{
				return uuid;
			}
		}
		return IRepository.UNRESOLVED_UUID;
	}

}
